package top.egon.enjoyshop.cart.start.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import top.egon.cola.platform.tianquan.jianshen.contract.authorization.SystemAuthorizationSnapshot;
import top.egon.cola.platform.tianquan.jianshen.starter.cache.AuthorizationSnapshotCache;
import top.egon.cola.platform.tianquan.jianshen.starter.cache.SingleFlightSnapshotLoader;
import top.egon.cola.platform.tianquan.jianshen.starter.client.Rbac3AuthorizationClient;
import top.egon.cola.platform.tianquan.jianshen.starter.security.Rbac3BearerAuthenticationFilter;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpBearerAuthenticationFilter;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpJwtVerifier;
import top.egon.enjoyshop.cart.start.StudentManagementApplication;
import top.egon.enjoyshop.cart.support.PersistenceTestSupport;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(CartAuthenticationBoundaryTest.BoundaryFilterFixture.class)
@SpringBootTest(classes = StudentManagementApplication.class, properties = {
        "app.web.cors.allowed-origins=http://localhost:9100",
        "egon.cola.platform.tianquan.jianshen.runtime.redis-enabled=false",
        "egon.cola.platform.tianquan.jianshen.authorization.service-token.enabled=false"
})
class CartAuthenticationBoundaryTest extends PersistenceTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FilterChainProxy chain;

    @Test
    void missingBearerIsTwoFieldUnauthorized() throws Exception {
        assertTwoFieldUnauthorized(get("/api/v1/cart"));
        assertTwoFieldUnauthorized(post("/api/v1/cart/items").contentType("application/json").content("{}"));
        assertThat(chain.getFilters("/api/v1/cart").stream().map(filter -> filter.getClass().getSimpleName()).toList())
                .containsSubsequence("IdpBearerAuthenticationFilter", "Rbac3BearerAuthenticationFilter", "AnonymousAuthenticationFilter");
        assertThat(chain.getFilters("/api/v1/cart/items").stream().map(filter -> filter.getClass().getSimpleName()).toList())
                .containsSubsequence("IdpBearerAuthenticationFilter", "Rbac3BearerAuthenticationFilter", "AnonymousAuthenticationFilter");
    }

    @Test
    void registerFilterTrueFailsStartup() throws Exception {
        NativeHttpSecurityConfiguration config = new NativeHttpSecurityConfiguration();
        assertThatThrownBy(() -> config.cartSecurityFilterChain(null, null, null, null, null, true, false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cart SecurityFilterChain");
        assertThatThrownBy(() -> config.cartSecurityFilterChain(null, null, null, null, null, false, true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cart SecurityFilterChain");
    }

    private void assertTwoFieldUnauthorized(org.springframework.test.web.servlet.RequestBuilder request) throws Exception {
        mockMvc.perform(request)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHORIZATION_HEADER_INVALID"))
                .andExpect(jsonPath("$.message").value("Tianquan-Shoubing authentication failed"))
                .andExpect(jsonPath("$.success").doesNotExist());
    }

    /** Test profile keeps Shoubing disabled, so the chain needs the same synthetic filters goods uses. */
    @TestConfiguration(proxyBeanMethods = false)
    static class BoundaryFilterFixture {

        @Bean("idpBearerAuthenticationFilter")
        IdpBearerAuthenticationFilter idp(@Qualifier("jacksonObjectMapper") ObjectMapper mapper) {
            IdpJwtVerifier verifier = new IdpJwtVerifier(token -> {
                throw new BadJwtException("Unknown synthetic token");
            }, "platform", Clock.systemUTC());
            return new IdpBearerAuthenticationFilter(verifier, mapper);
        }

        @Bean("cartBoundaryDisabledIdpRegistration")
        FilterRegistrationBean<IdpBearerAuthenticationFilter> disabledIdp(
                @Qualifier("idpBearerAuthenticationFilter") IdpBearerAuthenticationFilter filter) {
            FilterRegistrationBean<IdpBearerAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
            registration.setEnabled(false);
            return registration;
        }

        @Bean("rbac3BearerAuthenticationFilter")
        Rbac3BearerAuthenticationFilter rbac(@Qualifier("jacksonObjectMapper") ObjectMapper mapper) {
            AuthorizationSnapshotCache cache = new AuthorizationSnapshotCache(new AuthorizationSnapshotCache.SnapshotStore() {
                @Override
                public Optional<SystemAuthorizationSnapshot> get(AuthorizationSnapshotCache.Key key) {
                    return Optional.empty();
                }

                @Override
                public void put(AuthorizationSnapshotCache.Key key, SystemAuthorizationSnapshot snapshot, Duration ttl) {
                }

                @Override
                public void invalidate(AuthorizationSnapshotCache.Key key) {
                }

                @Override
                public void invalidateUser(String systemCode, String tenantId, String identitySub) {
                }

                @Override
                public void invalidateTenant(String systemCode, String tenantId) {
                }
            }, Clock.systemUTC(), Duration.ZERO);
            Rbac3AuthorizationClient client = (system, identity) -> {
                throw new Rbac3AuthorizationClient.AuthorizationUnavailableException("unused");
            };
            return new Rbac3BearerAuthenticationFilter(
                    new SingleFlightSnapshotLoader(cache, client, "enjoyshop-cart", Duration.ofSeconds(1), Clock.systemUTC()),
                    mapper);
        }

        @Bean("cartBoundaryDisabledRbacRegistration")
        FilterRegistrationBean<Rbac3BearerAuthenticationFilter> disabledRbac(
                @Qualifier("rbac3BearerAuthenticationFilter") Rbac3BearerAuthenticationFilter filter) {
            FilterRegistrationBean<Rbac3BearerAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
            registration.setEnabled(false);
            return registration;
        }
    }
}
