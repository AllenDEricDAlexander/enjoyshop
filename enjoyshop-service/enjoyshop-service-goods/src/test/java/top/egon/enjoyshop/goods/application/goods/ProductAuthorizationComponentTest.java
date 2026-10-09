package top.egon.enjoyshop.goods.application.goods;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.security.core.context.SecurityContextHolder;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.platform.tianquan.jianshen.contract.authorization.*;
import top.egon.cola.platform.tianquan.jianshen.starter.authorization.AuthorizationService;
import top.egon.cola.platform.tianquan.jianshen.starter.security.*;
import top.egon.cola.platform.tianquan.shoubing.contract.*;

class ProductAuthorizationComponentTest {
    @AfterEach void clear() { SecurityContextHolder.clearContext(); MDC.clear(); }

    @Test
    void fourPermissionsUseResourceBoundDecisionsAndFailClosed() throws Exception {
        AuthorizationService decisions = mock(AuthorizationService.class);
        Object gate = Class.forName("top.egon.enjoyshop.goods.application.goods.ProductAuthorizationComponent")
                .getConstructor(AuthorizationService.class).newInstance(decisions);
        var method = gate.getClass().getMethod("requirePermission", String.class);
        assertThatThrownBy(() -> method.invoke(gate, "enjoyshop:product:read"))
                .hasRootCauseInstanceOf(CommonException.class);
        install("1");
        for (String action : List.of("read", "write", "audit", "purge")) {
            String code = "enjoyshop:product:" + action;
            PermissionRequest request = PermissionRequest.api(code, "enjoyshop.product");
            when(decisions.requirePermission(request)).thenReturn(decision(code, Decision.ALLOW));
            method.invoke(gate, code);
            verify(decisions).requirePermission(request);
            when(decisions.requirePermission(request)).thenReturn(decision(code, Decision.DENY));
            assertCode(method, gate, code, 610206);
            when(decisions.requirePermission(request)).thenReturn(decision(code, Decision.INDETERMINATE));
            assertCode(method, gate, code, 610208);
            when(decisions.requirePermission(request)).thenReturn(null);
            assertCode(method, gate, code, 610208);
        }
        MDC.put("tenantId", "2");
        assertCode(method, gate, "enjoyshop:product:read", 610206);
    }

    @Test
    void expiredCachedSnapshotCannotBeUsedAsStaleAuthorization() {
        install("1");
        var context = ((Rbac3ContextAuthentication) SecurityContextHolder.getContext().getAuthentication()).context();
        var clock = mock(java.time.Clock.class);
        Instant now = context.snapshot().generatedAt();
        when(clock.instant()).thenReturn(now);
        var store = mock(top.egon.cola.platform.tianquan.jianshen.starter.cache.AuthorizationSnapshotCache.SnapshotStore.class);
        var key = new top.egon.cola.platform.tianquan.jianshen.starter.cache.AuthorizationSnapshotCache.Key("enjoyshop-goods", "1", "editor");
        when(store.get(key)).thenReturn(java.util.Optional.of(context.snapshot()));
        var cache = new top.egon.cola.platform.tianquan.jianshen.starter.cache.AuthorizationSnapshotCache(store, clock, java.time.Duration.ZERO);
        assertThat(cache.get(key)).isPresent();
        when(clock.instant()).thenReturn(now.plusSeconds(2));
        assertThat(cache.get(key)).isEmpty();
    }

    private static void assertCode(java.lang.reflect.Method method, Object target, String permission, int code) {
        try {
            method.invoke(target, permission);
            fail("permission gate should reject");
        } catch (InvocationTargetException failure) {
            assertThat(failure.getCause()).isInstanceOf(CommonException.class);
            assertThat(((CommonException) failure.getCause()).getCode()).isEqualTo(code);
        } catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
    }

    private static AuthorizationDecision decision(String code, Decision decision) {
        return new AuthorizationDecision(decision, "TEST", "1", "rbac-user", code, 1, 1, List.of(), Instant.now());
    }

    private static void install(String tenant) {
        Instant now = Instant.now();
        IdentityPrincipal identity = new IdentityPrincipal("editor", tenant, "test-jti", Set.of("platform"), now, now.plusSeconds(300), AuthenticationContext.password());
        SystemAuthorizationSnapshot snapshot = new SystemAuthorizationSnapshot(tenant, "editor", "rbac-user", "enjoyshop-goods", 1, 1,
                List.of(), List.of(), null, Set.of(), Set.of("enjoyshop.product"), Map.of(), Map.of(), "test", now, now.plusSeconds(1));
        SecurityContextHolder.getContext().setAuthentication(new Rbac3AuthenticationToken(new Rbac3UserDetails(identity, snapshot)));
        MDC.put("tenantId", tenant); MDC.put("userId", "editor");
    }
}
