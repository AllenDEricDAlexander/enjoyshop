package top.egon.enjoyshop.cart.adapter.user.pojo.dto;

import jakarta.validation.constraints.NotBlank;

public record GrantPermissionRequest(@NotBlank String permissionCode) {
}
