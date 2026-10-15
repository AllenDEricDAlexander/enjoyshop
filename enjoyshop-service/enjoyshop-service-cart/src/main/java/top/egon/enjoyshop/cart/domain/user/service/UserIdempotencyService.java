package top.egon.enjoyshop.cart.domain.user.service;

import jakarta.validation.constraints.NotBlank;

/** Request-claim capability owned by the user domain. */
public interface UserIdempotencyService {
    boolean claim(@NotBlank String key);
}
