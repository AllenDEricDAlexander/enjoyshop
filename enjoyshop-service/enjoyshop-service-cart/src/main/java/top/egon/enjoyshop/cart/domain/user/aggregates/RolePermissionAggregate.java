package top.egon.enjoyshop.cart.domain.user.aggregates;

import top.egon.enjoyshop.cart.domain.user.entities.Permission;
import top.egon.enjoyshop.cart.domain.user.entities.Role;
import top.egon.enjoyshop.cart.domain.user.validators.UserDomainValidator;
import top.egon.enjoyshop.cart.domain.user.vos.PermissionCode;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public final class RolePermissionAggregate {
    private final Role role;
    private final Set<PermissionCode> permissions = new LinkedHashSet<>();

    public RolePermissionAggregate(Role role) {
        this.role = Objects.requireNonNull(role);
    }

    public Role role() {
        return role;
    }

    public Set<PermissionCode> permissions() {
        return Set.copyOf(permissions);
    }

    public void grant(Permission permission) {
        UserDomainValidator.requireAssignable(role, permission);
        permissions.add(permission.code());
    }
}
