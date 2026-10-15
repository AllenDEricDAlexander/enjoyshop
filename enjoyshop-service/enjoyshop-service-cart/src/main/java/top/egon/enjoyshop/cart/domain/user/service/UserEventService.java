package top.egon.enjoyshop.cart.domain.user.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import top.egon.enjoyshop.cart.domain.user.vos.UserEvent;

/** User domain-event publication capability. */
public interface UserEventService {
    void publish(@Valid @NotNull UserEvent event);
}
