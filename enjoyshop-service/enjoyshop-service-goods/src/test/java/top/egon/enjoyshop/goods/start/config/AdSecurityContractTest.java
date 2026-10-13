package top.egon.enjoyshop.goods.start.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.yaml.snakeyaml.Yaml;
import top.egon.enjoyshop.goods.adapter.filter.AdPublicTenantFilter;
import top.egon.enjoyshop.goods.adapter.filter.TenantContextFilter;
import top.egon.enjoyshop.goods.common.enums.GoodsErrorStatus;
import top.egon.enjoyshop.goods.infrastructure.config.AdProperties;

import static org.assertj.core.api.Assertions.assertThat;

/** Offline tenant-scope and security-chain contract; no application or external service starts. */
class AdSecurityContractTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void registeredServerNameSetsTenantAndIgnoresCallerTenantHeaders() throws Exception {
        AdPublicTenantFilter filter = filter(true);
        MockHttpServletRequest request = request("GET", "store.example.test");
        request.addHeader("X-Tenant-Id", "999");
        request.addHeader("X-Actor-Id", "attacker");
        request.addHeader("X-Forwarded-Host", "attacker.example.test");
        MDC.put("tenantId", "outer-tenant");
        MDC.put("userId", "outer-user");
        AtomicReference<String> tenantSeenByController = new AtomicReference<>();
        AtomicReference<String> userSeenByController = new AtomicReference<>();
        AtomicBoolean invoked = new AtomicBoolean();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            invoked.set(true);
            tenantSeenByController.set(MDC.get("tenantId"));
            userSeenByController.set(MDC.get("userId"));
        });

        assertThat(invoked).isTrue();
        assertThat(tenantSeenByController.get()).isEqualTo("41");
        assertThat(userSeenByController.get()).isEqualTo("outer-user");
        assertThat(MDC.get("tenantId")).isEqualTo("outer-tenant");
        assertThat(MDC.get("userId")).isEqualTo("outer-user");
    }

    @Test
    void unregisteredHostAndDisabledPublicReadsFailBeforeTheChain() throws Exception {
        MockHttpServletResponse unknownHostResponse = new MockHttpServletResponse();
        AtomicBoolean unknownHostReachedChain = new AtomicBoolean();
        filter(true).doFilter(request("GET", "attacker.example.test"), unknownHostResponse,
                (servletRequest, servletResponse) -> unknownHostReachedChain.set(true));
        assertThat(unknownHostReachedChain).isFalse();
        assertThat(unknownHostResponse.getStatus()).isEqualTo(404);
        assertThat(new ObjectMapper().readTree(unknownHostResponse.getContentAsByteArray()).path("code").asInt())
                .isEqualTo(GoodsErrorStatus.AD_SITE_NOT_CONFIGURED.getCode());

        MockHttpServletResponse disabledResponse = new MockHttpServletResponse();
        AtomicBoolean disabledReachedChain = new AtomicBoolean();
        filter(false).doFilter(request("GET", "store.example.test"), disabledResponse,
                (servletRequest, servletResponse) -> disabledReachedChain.set(true));
        assertThat(disabledReachedChain).isFalse();
        assertThat(disabledResponse.getStatus()).isEqualTo(404);
    }

    @Test
    void legacyTenantFilterSkipsNewRoutesWithoutChangingExistingPaths() {
        ExposedTenantContextFilter filter = new ExposedTenantContextFilter();
        assertThat(filter.skips("GET", "/api/v1/products/1")).isTrue();
        assertThat(filter.skips("GET", "/api/v1/ads")).isTrue();
        assertThat(filter.skips("DELETE", "/api/v1/ads/1")).isTrue();
        assertThat(filter.skips("GET", "/api/v1/ad-slots/web_index_lb/ads")).isTrue();
        assertThat(filter.skips("GET", "/api/v1/brands")).isTrue();
        assertThat(filter.skips("GET", "/api/v1/specs/category/phones")).isTrue();
        assertThat(filter.skips("POST", "/api/v1/files")).isTrue();
        assertThat(filter.skips("GET", "/api/v1/legacy-unscoped")).isFalse();
    }

    @Test
    void publicFilterOnlyProcessesTheExactGetRouteAndSecurityChainsAreDedicated() throws Exception {
        ExposedAdPublicTenantFilter filter = new ExposedAdPublicTenantFilter(
                properties(true), new ObjectMapper());
        assertThat(filter.skips("GET", "/api/v1/ad-slots/web_index_lb/ads")).isFalse();
        assertThat(filter.skips("POST", "/api/v1/ad-slots/web_index_lb/ads")).isTrue();
        assertThat(filter.skips("GET", "/api/v1/ad-slots/web_index_lb/ads/extra")).isTrue();

        var methods = java.util.Arrays.stream(NativeHttpSecurityConfiguration.class.getDeclaredMethods())
                .map(java.lang.reflect.Method::getName)
                .toList();
        assertThat(methods).contains("adSecurityFilterChain", "publicAdSecurityFilterChain");
    }

    @Test
    @SuppressWarnings("unchecked")
    void adSiteConfigurationIsConsistentAndProductionRemainsDisabled() throws Exception {
        Set<String> expectedKeys = Set.of("public-enabled", "public-sites", "allowed-positions",
                "candidate-limit", "public-limit", "load-timeout");
        for (String profile : List.of("application.yml", "application-dev.yml", "application-test.yml", "application-prod.yml")) {
            Map<String, Object> root = new Yaml().load(Files.readString(Path.of("src/main/resources", profile)));
            Map<String, Object> app = (Map<String, Object>) root.get("app");
            Map<String, Object> ad = (Map<String, Object>) app.get("ad");
            assertThat(ad.keySet()).containsExactlyInAnyOrderElementsOf(expectedKeys);
            assertThat(ad.get("candidate-limit")).isEqualTo(200);
            assertThat(ad.get("public-limit")).isEqualTo(20);
            assertThat(ad.get("load-timeout")).isEqualTo("PT2S");
            assertThat(ad.get("allowed-positions")).isEqualTo(List.of("web_index_lb"));
            if ("application-test.yml".equals(profile)) {
                assertThat(ad.get("public-enabled")).isEqualTo(true);
                assertThat((Map<String, Object>) ad.get("public-sites")).containsEntry("store.example.test", 41);
            } else {
                assertThat(ad.get("public-enabled")).isEqualTo(false);
                assertThat((Map<String, Object>) ad.get("public-sites")).isEmpty();
            }
        }
    }

    private static AdPublicTenantFilter filter(boolean enabled) {
        return new AdPublicTenantFilter(properties(enabled), new ObjectMapper());
    }

    private static AdProperties properties(boolean enabled) {
        return AdProperties.builder()
                .publicEnabled(enabled)
                .publicSites(Map.of("store.example.test", 41L))
                .allowedPositions(List.of("web_index_lb"))
                .build();
    }

    private static MockHttpServletRequest request(String method, String host) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, "/api/v1/ad-slots/web_index_lb/ads");
        request.setServerName(host);
        return request;
    }

    private static final class ExposedTenantContextFilter extends TenantContextFilter {
        boolean skips(String method, String path) {
            MockHttpServletRequest request = new MockHttpServletRequest(method, path);
            return shouldNotFilter(request);
        }
    }

    private static final class ExposedAdPublicTenantFilter extends AdPublicTenantFilter {
        private ExposedAdPublicTenantFilter(AdProperties properties, ObjectMapper mapper) {
            super(properties, mapper);
        }

        boolean skips(String method, String path) {
            MockHttpServletRequest request = new MockHttpServletRequest(method, path);
            return shouldNotFilter(request);
        }
    }
}
