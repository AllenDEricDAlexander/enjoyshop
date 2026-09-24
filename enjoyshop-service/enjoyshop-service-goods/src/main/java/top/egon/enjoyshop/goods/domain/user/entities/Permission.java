package top.egon.enjoyshop.goods.domain.user.entities;

import top.egon.enjoyshop.goods.domain.user.enums.PermissionStatus;
import top.egon.enjoyshop.goods.domain.user.vos.PermissionCode;

import java.util.Objects;

public record Permission(PermissionCode code, String name, PermissionStatus status) {
    public Permission {
        Objects.requireNonNull(code, "code must not be null");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        Objects.requireNonNull(status, "status must not be null");
    }
}
