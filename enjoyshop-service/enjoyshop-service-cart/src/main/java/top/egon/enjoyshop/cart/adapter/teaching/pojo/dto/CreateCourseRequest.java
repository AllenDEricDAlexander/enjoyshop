package top.egon.enjoyshop.cart.adapter.teaching.pojo.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateCourseRequest(@NotBlank String code, @NotBlank String name) {
}
