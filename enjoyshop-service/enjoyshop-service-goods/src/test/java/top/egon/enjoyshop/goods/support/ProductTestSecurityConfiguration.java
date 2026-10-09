package top.egon.enjoyshop.goods.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import top.egon.cola.platform.tianquan.shoubing.starter.security.*;
import top.egon.cola.platform.tianquan.jianshen.contract.authorization.*;
import top.egon.cola.platform.tianquan.jianshen.starter.authorization.*;
import top.egon.cola.platform.tianquan.jianshen.starter.cache.*;
import top.egon.cola.platform.tianquan.jianshen.starter.client.Rbac3AuthorizationClient;
import top.egon.cola.platform.tianquan.jianshen.starter.security.*;

/** Explicit synthetic identity/snapshot fixtures; never included in production. */
@TestConfiguration(value = "productTestSecurityConfiguration", proxyBeanMethods = false)
public class ProductTestSecurityConfiguration {
    public static final Instant NOW = Instant.parse("2026-10-09T00:00:00Z");
    private static final Set<String> TOKENS = Set.of("product-read", "product-write", "product-audit", "product-purge", "product-all", "product-deny", "product-unavailable", "product-other-tenant", "product-service", "product-cache-unavailable", "product-timeout", "product-resource-missing");

    @Bean("idpBearerAuthenticationFilter")
    @ConditionalOnMissingBean(name = "idpBearerAuthenticationFilter")
    IdpBearerAuthenticationFilter idp(@Qualifier("jacksonObjectMapper") ObjectMapper mapper) {
        IdpJwtVerifier verifier = new IdpJwtVerifier(token -> {
            if (!TOKENS.contains(token)) { throw new BadJwtException("Unknown synthetic token"); }
            return Jwt.withTokenValue(token).header("alg", "RS256").header("kid", "test").header("typ", "at+jwt")
                    .issuer("https://identity.example.test").subject(token).audience(List.of("platform"))
                    .issuedAt(NOW).notBefore(NOW).expiresAt(NOW.plusSeconds(300))
                    .claim("principal_type", token.equals("product-service") ? "SERVICE" : "USER").claim("tid", token.equals("product-other-tenant") ? "2" : "1")
                    .claim("jti", "synthetic-jti").claim("acr", "PASSWORD").claim("auth_time", NOW).build();
        }, "platform", Clock.fixed(NOW, ZoneOffset.UTC));
        return new IdpBearerAuthenticationFilter(verifier, mapper);
    }

    @Bean("productTestDisabledIdpRegistration")
    FilterRegistrationBean<IdpBearerAuthenticationFilter> disabledIdp(@Qualifier("idpBearerAuthenticationFilter") IdpBearerAuthenticationFilter filter) {
        FilterRegistrationBean<IdpBearerAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false); return registration;
    }

    @Bean("authorizationSnapshotCache")
    AuthorizationSnapshotCache snapshots() {
        return new AuthorizationSnapshotCache(new AuthorizationSnapshotCache.SnapshotStore() {
            private final Map<AuthorizationSnapshotCache.Key, SystemAuthorizationSnapshot> values = new ConcurrentHashMap<>();
            public Optional<SystemAuthorizationSnapshot> get(AuthorizationSnapshotCache.Key key) {
                if (key.identitySub().equals("product-cache-unavailable")) { throw new IllegalStateException("Synthetic cache read failure"); }
                return Optional.ofNullable(values.get(key));
            }
            public void put(AuthorizationSnapshotCache.Key key, SystemAuthorizationSnapshot value, Duration ttl) { values.put(key, value); }
            public void invalidate(AuthorizationSnapshotCache.Key key) { values.remove(key); }
            public void invalidateUser(String tenant, String user, String system) { values.clear(); }
            public void invalidateTenant(String tenant, String system) { values.clear(); }
        }, Clock.fixed(NOW, ZoneOffset.UTC), Duration.ZERO);
    }

    @Bean("rbac3BearerAuthenticationFilter")
    Rbac3BearerAuthenticationFilter rbac(@Qualifier("jacksonObjectMapper") ObjectMapper mapper,
            @Qualifier("authorizationSnapshotCache") AuthorizationSnapshotCache cache) {
        Rbac3AuthorizationClient client = (system, identity) -> {
            if (identity.subject().equals("product-unavailable")) { throw new Rbac3AuthorizationClient.AuthorizationUnavailableException("Synthetic snapshot unavailable"); }
            if (identity.subject().equals("product-timeout")) { throw new Rbac3AuthorizationClient.AuthorizationUnavailableException("Synthetic HTTP fetch timeout"); }
            String suffix = identity.subject().replace("product-", "");
            Set<String> grants = suffix.equals("all") || suffix.equals("other-tenant") || suffix.equals("resource-missing")
                    ? Set.of("enjoyshop:product:read", "enjoyshop:product:write", "enjoyshop:product:audit", "enjoyshop:product:purge")
                    : Set.of("enjoyshop:product:" + suffix);
            return new SystemAuthorizationSnapshot(identity.tenantId(), identity.subject(), "synthetic-user", system, 1, 1,
                    List.of(), List.of(), null, grants, suffix.equals("resource-missing") ? Set.of() : Set.of("enjoyshop.product"), Map.of(), Map.of(), "synthetic", NOW, NOW.plusSeconds(1));
        };
        return new Rbac3BearerAuthenticationFilter(new SingleFlightSnapshotLoader(cache, client, "enjoyshop-goods", Duration.ofSeconds(1), Clock.fixed(NOW, ZoneOffset.UTC)), mapper);
    }

    @Bean("productTestDisabledRbacRegistration")
    FilterRegistrationBean<Rbac3BearerAuthenticationFilter> disabledRbac(@Qualifier("rbac3BearerAuthenticationFilter") Rbac3BearerAuthenticationFilter filter) {
        FilterRegistrationBean<Rbac3BearerAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false); return registration;
    }

    @Bean("authorizationService")
    AuthorizationService authorization() {
        return new DefaultAuthorizationService(() -> {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (!(auth instanceof Rbac3ContextAuthentication context)) { throw new IllegalStateException("Synthetic verified context required"); }
            return context.context();
        }, request -> null, request -> null, Clock.fixed(NOW, ZoneOffset.UTC));
    }
}
