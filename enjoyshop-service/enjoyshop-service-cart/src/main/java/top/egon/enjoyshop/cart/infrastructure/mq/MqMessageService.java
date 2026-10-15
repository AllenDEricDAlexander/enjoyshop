package top.egon.enjoyshop.cart.infrastructure.mq;

import jakarta.validation.constraints.NotNull;

/** Single outbound message boundary; the broker profile is decided by the bound implementation. */
public interface MqMessageService {
    void publish(@NotNull MqRouteEnum route, @NotNull Object payload);
}
