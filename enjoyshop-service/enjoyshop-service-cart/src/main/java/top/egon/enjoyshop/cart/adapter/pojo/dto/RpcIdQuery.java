package top.egon.enjoyshop.cart.adapter.pojo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Validates a scalar identifier after decoding its Protobuf presence. */
public record RpcIdQuery(@NotNull @Positive Long id) {
}
