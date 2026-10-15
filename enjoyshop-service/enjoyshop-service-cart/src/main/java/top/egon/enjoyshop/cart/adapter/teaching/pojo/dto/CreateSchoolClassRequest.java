package top.egon.enjoyshop.cart.adapter.teaching.pojo.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateSchoolClassRequest(@NotBlank String name, @NotBlank String semester) {
}
