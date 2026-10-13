package top.egon.enjoyshop.goods.application.goods;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.validation.constraints.Pattern;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.platform.tianquan.jianshen.contract.authorization.AuthorizationDecision;
import top.egon.cola.platform.tianquan.jianshen.contract.authorization.Decision;
import top.egon.cola.platform.tianquan.jianshen.contract.authorization.PermissionRequest;
import top.egon.cola.platform.tianquan.jianshen.contract.authorization.SystemAuthorizationSnapshot;
import top.egon.cola.platform.tianquan.jianshen.starter.authorization.AuthorizationService;
import top.egon.cola.platform.tianquan.jianshen.starter.security.Rbac3AuthenticationToken;
import top.egon.cola.platform.tianquan.jianshen.starter.security.Rbac3ContextAuthentication;
import top.egon.cola.platform.tianquan.jianshen.starter.security.Rbac3UserDetails;
import top.egon.cola.platform.tianquan.shoubing.contract.AuthenticationContext;
import top.egon.cola.platform.tianquan.shoubing.contract.IdentityPrincipal;
import top.egon.cola.platform.tianquan.shoubing.contract.PrincipalType;

class BrandAuthorizationComponentTest {

    private static final String CODE = "enjoyshop:brand:read";

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    @Test
    void decisionsFailClosed() throws Exception {
        var type = Class.forName("top.egon.enjoyshop.goods.application.goods.BrandAuthorizationComponent");
        AuthorizationService decisions = mock(AuthorizationService.class);
        Object gate = type.getConstructor(AuthorizationService.class).newInstance(decisions);
        Method method = type.getMethod("requirePermission", String.class);
        assertThat(method.getParameters()[0].getAnnotation(Pattern.class).regexp())
                .isEqualTo("enjoyshop:brand:(read|write)");
        assertThatThrownBy(() -> method.invoke(gate, CODE))
                .extracting(Throwable::getCause)
                .isInstanceOfSatisfying(CommonException.class, failure -> {
                    assertThat(failure.getCode()).isEqualTo(401000);
                    assertThat(failure.getStatus()).isEqualTo("UNAUTHORIZED");
                    assertThat(failure.getMessage()).isEqualTo("Verified user authentication is required");
                });
        installUser("1", "editor");
        PermissionRequest request = PermissionRequest.api(CODE, "enjoyshop.brand");
        when(decisions.requirePermission(request)).thenReturn(allow(CODE));
        method.invoke(gate, CODE);
        verify(decisions).requirePermission(request);
        when(decisions.requirePermission(request)).thenReturn(deny(CODE));
        assertCode(method, gate, CODE, 610111, "BRAND_FORBIDDEN");
        when(decisions.requirePermission(request)).thenReturn(indeterminate(CODE));
        assertCode(method, gate, CODE, 610112, "BRAND_AUTHORIZATION_UNAVAILABLE");
        when(decisions.requirePermission(request)).thenReturn(null);
        assertCode(method, gate, CODE, 610112, "BRAND_AUTHORIZATION_UNAVAILABLE");
        installPrincipal(PrincipalType.SERVICE);
        assertCode(method, gate, CODE, 610111, "BRAND_FORBIDDEN");
        installUser("1", "editor");
        MDC.put("tenantId", "2");
        assertCode(method, gate, CODE, 610111, "BRAND_FORBIDDEN");
    }

    private static void assertCode(Method method, Object target, String permission, int code, String status) {
        try {
            method.invoke(target, permission);
            fail("permission gate should reject");
        } catch (InvocationTargetException failure) {
            assertThat(failure.getCause()).isInstanceOf(CommonException.class);
            CommonException rejected = (CommonException) failure.getCause();
            assertThat(rejected.getCode()).isEqualTo(code);
            assertThat(rejected.getStatus()).isEqualTo(status);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError(failure);
        }
    }

    private static AuthorizationDecision allow(String code) {
        return decision(code, Decision.ALLOW);
    }

    private static AuthorizationDecision deny(String code) {
        return decision(code, Decision.DENY);
    }

    private static AuthorizationDecision indeterminate(String code) {
        return decision(code, Decision.INDETERMINATE);
    }

    private static AuthorizationDecision decision(String code, Decision outcome) {
        return new AuthorizationDecision(outcome, "TEST", "1", "rbac-user", code, 1, 1, List.of(), Instant.now());
    }

    private static void installUser(String tenant, String subject) {
        Instant now = Instant.now();
        IdentityPrincipal identity = new IdentityPrincipal(
                subject, tenant, "test-jti", Set.of("platform"), now, now.plusSeconds(300), AuthenticationContext.password());
        SystemAuthorizationSnapshot snapshot = new SystemAuthorizationSnapshot(
                tenant, subject, "rbac-user", "enjoyshop-goods", 1, 1,
                List.of(), List.of(), null, Set.of(), Set.of("enjoyshop.brand"), Map.of(), Map.of(), "test", now, now.plusSeconds(1));
        SecurityContextHolder.getContext().setAuthentication(
                new Rbac3AuthenticationToken(new Rbac3UserDetails(identity, snapshot)));
        MDC.put("tenantId", tenant);
        MDC.put("userId", subject);
    }

    private static void installPrincipal(PrincipalType principalType) {
        IdentityPrincipal identity = mock(IdentityPrincipal.class);
        when(identity.principalType()).thenReturn(principalType);
        when(identity.tenantId()).thenReturn("1");
        when(identity.subject()).thenReturn("editor");
        Instant now = Instant.now();
        SystemAuthorizationSnapshot snapshot = new SystemAuthorizationSnapshot(
                "1", "editor", "rbac-user", "enjoyshop-goods", 1, 1,
                List.of(), List.of(), null, Set.of(), Set.of("enjoyshop.brand"), Map.of(), Map.of(), "test", now, now.plusSeconds(1));
        AuthorizationService.RuntimeAuthorizationContext context = mock(AuthorizationService.RuntimeAuthorizationContext.class);
        when(context.identity()).thenReturn(identity);
        when(context.snapshot()).thenReturn(snapshot);
        when(context.fenced()).thenReturn(false);
        SecurityContextHolder.getContext().setAuthentication(new TypedAuthentication(context));
        MDC.put("tenantId", "1");
        MDC.put("userId", "editor");
    }

    private static final class TypedAuthentication extends AbstractAuthenticationToken implements Rbac3ContextAuthentication {

        private final AuthorizationService.RuntimeAuthorizationContext context;

        private TypedAuthentication(AuthorizationService.RuntimeAuthorizationContext context) {
            super(List.of());
            this.context = context;
            setAuthenticated(true);
        }

        @Override
        public AuthorizationService.RuntimeAuthorizationContext context() {
            return context;
        }

        @Override
        public Object getCredentials() {
            return "";
        }

        @Override
        public Object getPrincipal() {
            return "service";
        }
    }
}
