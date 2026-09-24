package top.egon.enjoyshop.goods.domain.teaching.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import top.egon.enjoyshop.goods.domain.teaching.vos.TeachingEvent;

/** Teaching domain-event publication capability. */
public interface TeachingEventService {
    void publish(@Valid @NotNull TeachingEvent event);
}
