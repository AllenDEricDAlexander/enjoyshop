package top.egon.enjoyshop.goods.adapter.user.pojo.dto;

import jakarta.validation.constraints.NotBlank;

public record GrantPermissionRequest(@NotBlank String permissionCode) {
}
