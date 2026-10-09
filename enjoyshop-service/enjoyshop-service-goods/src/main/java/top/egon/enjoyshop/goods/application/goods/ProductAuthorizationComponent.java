package top.egon.enjoyshop.goods.application.goods;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.core.context.SecurityContextHolder;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.platform.tianquan.jianshen.contract.authorization.Decision;
import top.egon.cola.platform.tianquan.jianshen.contract.authorization.PermissionRequest;
import top.egon.cola.platform.tianquan.jianshen.starter.authorization.AuthorizationService;
import top.egon.cola.platform.tianquan.jianshen.starter.security.Rbac3ContextAuthentication;
import top.egon.cola.platform.tianquan.shoubing.contract.PrincipalType;

@Component("productAuthorizationComponent")
@Validated
@Slf4j
@RequiredArgsConstructor
public class ProductAuthorizationComponent {
    @Qualifier("authorizationService")
    private final AuthorizationService authorization;

    public void requirePermission(@NotBlank @Pattern(regexp = "enjoyshop:product:(read|write|audit|purge)") String permissionCode) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken) {
            throw new CommonException(401000, "UNAUTHORIZED", "Verified user authentication is required");
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
                    || Long.parseLong(identity.tenantId()) <= 0) { throw forbidden(); }
        } catch (NumberFormatException invalidTenant) { throw forbidden(); }
        if (identity.principalType() != PrincipalType.USER || identity.subject() == null || identity.subject().isBlank()
                || !Objects.equals(identity.tenantId(), snapshot.tenantId())
                || !Objects.equals(identity.subject(), snapshot.identitySub())
                || !"enjoyshop-goods".equals(snapshot.systemCode()) || context.fenced()
                || !Objects.equals(MDC.get("tenantId"), identity.tenantId())
                || !Objects.equals(MDC.get("userId"), identity.subject())) {
            throw forbidden();
        }
        var decision = authorization.requirePermission(PermissionRequest.api(permissionCode, "enjoyshop.product"));
        if (decision == null || decision.decision() == Decision.INDETERMINATE) { throw unavailable(); }
        if (decision.decision() != Decision.ALLOW || !Objects.equals(decision.tenantId(), identity.tenantId())
                || !Objects.equals(decision.subjectId(), snapshot.rbac3UserId())
                || !Objects.equals(decision.permissionCode(), permissionCode)
                || decision.authVersion() != snapshot.authVersion() || decision.policyVersion() != snapshot.policyVersion()) {
            throw forbidden();
        }
    }

    private static CommonException forbidden() {
        return new CommonException(610206, "PRODUCT_FORBIDDEN", "Product permission is denied");
    }
    private static CommonException unavailable() {
        return new CommonException(610208, "PRODUCT_AUTHORIZATION_UNAVAILABLE", "Product authorization is unavailable");
    }
}
