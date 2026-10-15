package top.egon.enjoyshop.cart.domain.teaching.entities;

import top.egon.enjoyshop.cart.domain.teaching.enums.CourseStatus;
import top.egon.enjoyshop.cart.domain.teaching.vos.CourseCode;

import java.util.Objects;

public record Course(Long id, CourseCode code, String name, CourseStatus status) {
    public Course {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("id must be positive");
        }
        Objects.requireNonNull(code, "code must not be null");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        Objects.requireNonNull(status, "status must not be null");
    }

}
