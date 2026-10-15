package top.egon.enjoyshop.cart.domain.user.service;

import top.egon.enjoyshop.cart.domain.user.aggregates.RolePermissionAggregate;
import top.egon.enjoyshop.cart.domain.user.aggregates.UserAggregate;
import top.egon.enjoyshop.cart.domain.user.entities.Role;
import top.egon.enjoyshop.cart.domain.user.vos.RoleCode;

import java.util.Optional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Persistence-owning role domain service contract. */
public interface RoleDomainService {
    Optional<Role> findByCode(@Valid @NotNull RoleCode roleCode);

    Role save(@Valid @NotNull Role role);

    void savePermissions(@Valid @NotNull RolePermissionAggregate aggregate);

    UserAggregate assignRole(@Valid @NotNull UserAggregate user, @Valid @NotNull Role role);
}
