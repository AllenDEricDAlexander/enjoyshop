package top.egon.enjoyshop.cart.application.cart;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.platform.tianquan.jianshen.contract.authorization.Decision;
import top.egon.cola.platform.tianquan.jianshen.contract.authorization.PermissionRequest;
import top.egon.cola.platform.tianquan.jianshen.starter.authorization.AuthorizationService;
import top.egon.cola.platform.tianquan.jianshen.starter.security.Rbac3ContextAuthentication;
import top.egon.cola.platform.tianquan.shoubing.contract.PrincipalType;
import top.egon.enjoyshop.cart.common.enums.CartErrorStatus;

@Component("cartAuthorizationComponent")
@Validated
@Slf4j
@RequiredArgsConstructor
public class CartAuthorizationComponent {

    private static final String SYSTEM_CODE = "enjoyshop-cart";

    private static final String RESOURCE_CODE = "enjoyshop.cart";

    @Qualifier("authorizationService")
    private final AuthorizationService authorization;

    public void requirePermission(@NotBlank @Pattern(regexp = "enjoyshop:cart:(read|write)") String permissionCode) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            throw unauthenticated();
        }
        if (!(authentication instanceof Rbac3ContextAuthentication bound) || bound.context() == null
                || bound.context().identity() == null || bound.context().snapshot() == null) {
            throw unavailable();
        }
        var context = bound.context();
        var identity = context.identity();
        var snapshot = context.snapshot();
        try {
            if (identity.tenantId() == null || !identity.tenantId().matches("[1-9][0-9]{0,18}")
                    || Long.parseLong(identity.tenantId()) <= 0) {
                throw forbidden();
            }
        } catch (NumberFormatException invalidTenant) {
            throw forbidden();
        }
        if (identity.principalType() != PrincipalType.USER || identity.subject() == null || identity.subject().isBlank()
                || !Objects.equals(identity.tenantId(), snapshot.tenantId())
                || !Objects.equals(identity.subject(), snapshot.identitySub())
                || !SYSTEM_CODE.equals(snapshot.systemCode()) || context.fenced()
                || !Objects.equals(MDC.get("tenantId"), identity.tenantId())
                || !Objects.equals(MDC.get("userId"), identity.subject())) {
            throw forbidden();
        }
        var decision = authorization.requirePermission(PermissionRequest.api(permissionCode, RESOURCE_CODE));
        if (decision == null || decision.decision() == Decision.INDETERMINATE) {
            throw unavailable();
        }
        if (decision.decision() != Decision.ALLOW || !Objects.equals(decision.tenantId(), identity.tenantId())
                || !Objects.equals(decision.subjectId(), snapshot.rbac3UserId())
                || !Objects.equals(decision.permissionCode(), permissionCode)
                || decision.authVersion() != snapshot.authVersion() || decision.policyVersion() != snapshot.policyVersion()) {
            throw forbidden();
        }
    }

    private static CommonException unauthenticated() {
        log.warn("cart authentication rejected, code={}", 401000);
        return new CommonException(401000, "UNAUTHORIZED", "Verified user authentication is required");
    }

    private static CommonException forbidden() {
        log.warn("cart authorization rejected, code={}", CartErrorStatus.CART_FORBIDDEN.getCode());
        return new CommonException(CartErrorStatus.CART_FORBIDDEN);
    }

    private static CommonException unavailable() {
        log.warn("cart authorization unavailable, code={}", CartErrorStatus.CART_AUTHORIZATION_UNAVAILABLE.getCode());
        return new CommonException(CartErrorStatus.CART_AUTHORIZATION_UNAVAILABLE);
    }
}
