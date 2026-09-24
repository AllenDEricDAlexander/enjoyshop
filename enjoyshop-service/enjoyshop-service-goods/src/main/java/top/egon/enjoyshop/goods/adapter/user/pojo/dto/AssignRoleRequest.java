package top.egon.enjoyshop.goods.adapter.user.pojo.dto;

import jakarta.validation.constraints.NotBlank;

public record AssignRoleRequest(@NotBlank String roleCode) {
}
