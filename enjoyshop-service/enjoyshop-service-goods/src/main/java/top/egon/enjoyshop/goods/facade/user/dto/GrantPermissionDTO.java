package top.egon.enjoyshop.goods.facade.user.dto;

import jakarta.validation.constraints.NotBlank;
import top.egon.enjoyshop.goods.facade.validation.NativeRpcValidationGroup;

import top.egon.cola.component.common.core.pojo.BasePojo;

public record GrantPermissionDTO(
        @NotBlank(groups = NativeRpcValidationGroup.class) String roleCode,
        @NotBlank(groups = NativeRpcValidationGroup.class) String permissionCode,
        @NotBlank(groups = NativeRpcValidationGroup.class) String operatorId,
        @NotBlank(groups = NativeRpcValidationGroup.class) String requestId) implements BasePojo {
}
