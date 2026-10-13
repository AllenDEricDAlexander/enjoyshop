package top.egon.enjoyshop.cart.application.cart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.validation.Validation;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.MDC;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
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
import top.egon.enjoyshop.cart.common.enums.CartErrorStatus;

class CartAuthorizationComponentTest {

    private final AuthorizationService authorization = mock(AuthorizationService.class);

    private final CartAuthorizationComponent component = new CartAuthorizationComponent(authorization);

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    @Test
    void deniesWhenDecisionIsNotAllow() {
        installUser("enjoyshop-cart", "user-1", "1");
        PermissionRequest request = PermissionRequest.api("enjoyshop:cart:read", "enjoyshop.cart");
        when(authorization.requirePermission(request)).thenReturn(decision("enjoyshop:cart:read", Decision.DENY, "rbac-user", 1, 1));
        assertCode(() -> component.requirePermission("enjoyshop:cart:read"), CartErrorStatus.CART_FORBIDDEN.getCode());
        verify(authorization).requirePermission(request);

        when(authorization.requirePermission(request)).thenReturn(decision("enjoyshop:cart:read", Decision.ALLOW, "rbac-user", 9, 1));
        assertCode(() -> component.requirePermission("enjoyshop:cart:read"), CartErrorStatus.CART_FORBIDDEN.getCode());
        when(authorization.requirePermission(request)).thenReturn(decision("enjoyshop:cart:write", Decision.ALLOW, "rbac-user", 1, 1));
        assertCode(() -> component.requirePermission("enjoyshop:cart:read"), CartErrorStatus.CART_FORBIDDEN.getCode());
    }

    @Test
    void deniesWhenPrincipalIsNotUser() {
        IdentityPrincipal service = mock(IdentityPrincipal.class);
        when(service.principalType()).thenReturn(PrincipalType.SERVICE);
        when(service.subject()).thenReturn("user-1");
        when(service.tenantId()).thenReturn("1");
        install(service, snapshot("enjoyshop-cart", "user-1", "1"), "1", "user-1");
        assertCode(() -> component.requirePermission("enjoyshop:cart:write"), CartErrorStatus.CART_FORBIDDEN.getCode());
        verify(authorization, never()).requirePermission(Mockito.any());
    }

    @Test
    void unavailableWhenDecisionIsNull() {
        installUser("enjoyshop-cart", "user-1", "1");
        PermissionRequest request = PermissionRequest.api("enjoyshop:cart:read", "enjoyshop.cart");
        when(authorization.requirePermission(request)).thenReturn(null);
        assertCode(() -> component.requirePermission("enjoyshop:cart:read"), CartErrorStatus.CART_AUTHORIZATION_UNAVAILABLE.getCode());
        when(authorization.requirePermission(request)).thenReturn(decision("enjoyshop:cart:read", Decision.INDETERMINATE, "rbac-user", 1, 1));
        assertCode(() -> component.requirePermission("enjoyshop:cart:read"), CartErrorStatus.CART_AUTHORIZATION_UNAVAILABLE.getCode());
    }

    @Test
    void ignoresAnyCallerSuppliedUsername() throws Exception {
        String ignoredCallerName = "itcast";
        assertThat(CartAuthorizationComponent.class.getMethod("requirePermission", String.class).getParameterCount()).isOne();
        installUser("enjoyshop-cart", "user-1", "1");
        PermissionRequest request = PermissionRequest.api("enjoyshop:cart:write", "enjoyshop.cart");
        when(authorization.requirePermission(request)).thenReturn(decision("enjoyshop:cart:write", Decision.ALLOW, "rbac-user", 1, 1));
        component.requirePermission("enjoyshop:cart:write");
        when(authorization.requirePermission(request)).thenReturn(decision("enjoyshop:cart:write", Decision.ALLOW, ignoredCallerName, 1, 1));
        assertCode(() -> component.requirePermission("enjoyshop:cart:write"), CartErrorStatus.CART_FORBIDDEN.getCode());
        assertThat(ignoredCallerName).isEqualTo("itcast");
    }

    @Test
    void deniesWhenSystemCodeIsNotCart() {
        installUser("enjoyshop-goods", "user-1", "1");
        assertCode(() -> component.requirePermission("enjoyshop:cart:read"), CartErrorStatus.CART_FORBIDDEN.getCode());
        verify(authorization, never()).requirePermission(Mockito.any());
    }

    @Test
    void allowsMatchingReadAndWriteDecisions() {
        installUser("enjoyshop-cart", "user-1", "1");
        for (String action : List.of("read", "write")) {
            String code = "enjoyshop:cart:" + action;
            PermissionRequest request = PermissionRequest.api(code, "enjoyshop.cart");
            when(authorization.requirePermission(request)).thenReturn(decision(code, Decision.ALLOW, "rbac-user", 1, 1));
            component.requirePermission(code);
            verify(authorization).requirePermission(request);
        }
    }

    @Test
    void rejectsAnonymousAndMissingBearerBeforePermissionLookup() {
        assertCode(() -> component.requirePermission("enjoyshop:cart:read"), 401000);
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "cart-test", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
        assertCode(() -> component.requirePermission("enjoyshop:cart:read"), 401000);
        verify(authorization, never()).requirePermission(Mockito.any());
    }

    @Test
    void unavailableWhenAuthenticationIsNotRbac3() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("itcast", "n/a", AuthorityUtils.createAuthorityList("ROLE_USER")));
        assertCode(() -> component.requirePermission("enjoyshop:cart:read"), CartErrorStatus.CART_AUTHORIZATION_UNAVAILABLE.getCode());
        verify(authorization, never()).requirePermission(Mockito.any());
    }

    @Test
    void deniesWhenTenantSubjectSnapshotOrMdcDoesNotMatch() {
        installUser("enjoyshop-cart", "user-1", "0");
        assertCode(() -> component.requirePermission("enjoyshop:cart:read"), CartErrorStatus.CART_FORBIDDEN.getCode());
        installUser("enjoyshop-cart", "user-1", "abc");
        assertCode(() -> component.requirePermission("enjoyshop:cart:read"), CartErrorStatus.CART_FORBIDDEN.getCode());

        installContext(openContext(identity("user-1", "1"), snapshot("enjoyshop-cart", "other-user", "1"), false), "1", "user-1");
        assertCode(() -> component.requirePermission("enjoyshop:cart:read"), CartErrorStatus.CART_FORBIDDEN.getCode());

        IdentityPrincipal blankSubject = mock(IdentityPrincipal.class);
        when(blankSubject.principalType()).thenReturn(PrincipalType.USER);
        when(blankSubject.subject()).thenReturn(" ");
        when(blankSubject.tenantId()).thenReturn("1");
        installContext(openContext(blankSubject, snapshot("enjoyshop-cart", "user-1", "1"), false), "1", "user-1");
        assertCode(() -> component.requirePermission("enjoyshop:cart:read"), CartErrorStatus.CART_FORBIDDEN.getCode());

        installUser("enjoyshop-cart", "user-1", "1");
        MDC.put("tenantId", "2");
        assertCode(() -> component.requirePermission("enjoyshop:cart:read"), CartErrorStatus.CART_FORBIDDEN.getCode());
        MDC.put("tenantId", "1");
        MDC.put("userId", "itcast");
        assertCode(() -> component.requirePermission("enjoyshop:cart:read"), CartErrorStatus.CART_FORBIDDEN.getCode());

        installBound(identity("user-1", "1"), snapshot("enjoyshop-cart", "user-1", "1"), true);
        assertCode(() -> component.requirePermission("enjoyshop:cart:read"), CartErrorStatus.CART_FORBIDDEN.getCode());
        verify(authorization, never()).requirePermission(Mockito.any());
    }

    @Test
    void rejectsPermissionCodeOutsideCartActions() throws Exception {
        var validator = Validation.buildDefaultValidatorFactory().getValidator();
        var method = CartAuthorizationComponent.class.getMethod("requirePermission", String.class);
        for (String illegal : List.of("", "enjoyshop:product:read", "enjoyshop:cart:admin", "itcast")) {
            assertThat(validator.forExecutables().validateParameters(component, method, new Object[] {illegal})).isNotEmpty();
        }
        assertThat(validator.forExecutables().validateParameters(component, method, new Object[] {"enjoyshop:cart:read"})).isEmpty();
        assertThat(validator.forExecutables().validateParameters(component, method, new Object[] {"enjoyshop:cart:write"})).isEmpty();
    }

    private static void assertCode(Runnable action, int code) {
        assertThatThrownBy(action::run).isInstanceOf(CommonException.class).extracting(error -> ((CommonException) error).getCode()).isEqualTo(code);
    }

    private static void installUser(String systemCode, String subject, String tenant) {
        install(identity(subject, tenant), snapshot(systemCode, subject, tenant), tenant, subject);
    }

    private static void install(
            IdentityPrincipal identity,
            SystemAuthorizationSnapshot snapshot,
            String mdcTenant,
            String mdcUser) {
        SecurityContextHolder.getContext().setAuthentication(new Rbac3AuthenticationToken(new Rbac3UserDetails(identity, snapshot)));
        MDC.put("tenantId", mdcTenant);
        MDC.put("userId", mdcUser);
    }

    private static void installBound(IdentityPrincipal identity, SystemAuthorizationSnapshot snapshot, boolean fenced) {
        installContext(new AuthorizationService.RuntimeAuthorizationContext(identity, snapshot, fenced), identity.tenantId(), identity.subject());
    }

    private static void installContext(
            AuthorizationService.RuntimeAuthorizationContext context,
            String mdcTenant,
            String mdcUser) {
        Rbac3ContextAuthentication authentication = mock(
                Rbac3ContextAuthentication.class,
                Mockito.withSettings().extraInterfaces(Authentication.class));
        Authentication springAuthentication = (Authentication) authentication;
        when(springAuthentication.isAuthenticated()).thenReturn(true);
        when(authentication.context()).thenReturn(context);
        SecurityContextHolder.getContext().setAuthentication(springAuthentication);
        MDC.put("tenantId", mdcTenant);
        MDC.put("userId", mdcUser);
    }

    private static AuthorizationService.RuntimeAuthorizationContext openContext(
            IdentityPrincipal identity,
            SystemAuthorizationSnapshot snapshot,
            boolean fenced) {
        AuthorizationService.RuntimeAuthorizationContext context = mock(AuthorizationService.RuntimeAuthorizationContext.class);
        when(context.identity()).thenReturn(identity);
        when(context.snapshot()).thenReturn(snapshot);
        when(context.fenced()).thenReturn(fenced);
        return context;
    }

    private static IdentityPrincipal identity(String subject, String tenant) {
        Instant now = Instant.now();
        return new IdentityPrincipal(subject, tenant, "test-jti", Set.of("platform"), now, now.plusSeconds(300), AuthenticationContext.password());
    }

    private static SystemAuthorizationSnapshot snapshot(String systemCode, String subject, String tenant) {
        Instant now = Instant.now();
        return new SystemAuthorizationSnapshot(
                tenant,
                subject,
                "rbac-user",
                systemCode,
                1,
                1,
                List.of(),
                List.of(),
                null,
                Set.of(),
                Set.of("enjoyshop.cart"),
                Map.of(),
                Map.of(),
                "test",
                now,
                now.plusSeconds(1));
    }

    private static AuthorizationDecision decision(String code, Decision outcome, String subjectId, long authVersion, long policyVersion) {
        return new AuthorizationDecision(outcome, "TEST", "1", subjectId, code, authVersion, policyVersion, List.of(), Instant.now());
    }
}
