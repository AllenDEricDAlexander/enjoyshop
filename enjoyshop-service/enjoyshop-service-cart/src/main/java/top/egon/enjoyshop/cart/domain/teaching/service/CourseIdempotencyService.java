package top.egon.enjoyshop.cart.domain.teaching.service;

import jakarta.validation.constraints.NotBlank;

/** Request-claim capability owned by the teaching domain. */
public interface CourseIdempotencyService {
    boolean claim(@NotBlank String key);
}
