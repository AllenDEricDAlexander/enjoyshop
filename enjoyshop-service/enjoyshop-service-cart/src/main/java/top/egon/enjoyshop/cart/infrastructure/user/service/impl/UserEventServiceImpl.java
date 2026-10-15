package top.egon.enjoyshop.cart.infrastructure.user.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import top.egon.enjoyshop.cart.domain.user.service.UserEventService;
import top.egon.enjoyshop.cart.domain.user.vos.UserEvent;
import top.egon.enjoyshop.cart.infrastructure.mq.MqMessageService;
import top.egon.enjoyshop.cart.infrastructure.mq.MqRouteEnum;

/** Resolves the declared route for a user event and hands the message to the single MQ boundary. */
@Validated
@Service("userEventService")
@RequiredArgsConstructor
@Slf4j
public class UserEventServiceImpl implements UserEventService {
    @Qualifier("mqMessageService")
    private final MqMessageService mqMessageService;

    @Override
    public void publish(UserEvent event) {
        mqMessageService.publish(MqRouteEnum.resolveProducer(event.type()), event);
    }
}
