package top.egon.enjoyshop.cart.facade.teaching.dto;

import jakarta.validation.constraints.NotBlank;
import top.egon.enjoyshop.cart.facade.validation.NativeRpcValidationGroup;

import top.egon.cola.component.common.core.pojo.BasePojo;

public record CreateCourseDTO(
        @NotBlank(groups = NativeRpcValidationGroup.class) String code,
        @NotBlank(groups = NativeRpcValidationGroup.class) String name,
        @NotBlank(groups = NativeRpcValidationGroup.class) String operatorId,
        @NotBlank(groups = NativeRpcValidationGroup.class) String requestId) implements BasePojo {
}
