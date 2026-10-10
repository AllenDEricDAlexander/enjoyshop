package top.egon.enjoyshop.goods.infrastructure.sync;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.enjoyshop.goods.domain.sync.CdcChangeStrategy;
import top.egon.enjoyshop.goods.infrastructure.config.CdcSourceProperties;

/** Publishes semantic CDC events only after every required broker confirm succeeds. */
@Component("cdcEventPublisher")
@ConditionalOnProperty(name = "app.cdc.enabled", havingValue = "true")
@Validated
@Slf4j
@RequiredArgsConstructor
public class CdcEventPublisher {

    private static final String SYNC_EXCHANGE = "enjoyshop.sync.v1";
    private static final String AD_ROUTE = "ad.invalidate.v1";
    private static final String PRODUCT_ROUTE = "product.refresh.v1";

    @Qualifier("cdcRabbitTemplate")
    private final RabbitTemplate rabbitTemplate;

    @Qualifier("jacksonObjectMapper")
    private final ObjectMapper objectMapper;

    @Qualifier("app.cdc-top.egon.enjoyshop.goods.infrastructure.config.CdcSourceProperties")
    private final CdcSourceProperties sourceProperties;

    @Qualifier("egonColaValidationUtils")
    private final ValidationUtils validation;

    public void publishConfirmed(List<? extends CdcChangeStrategy.SyncEvent> events, String routingKey) {
        if (events == null || events.isEmpty() || events.size() > 2 || routingKey == null
                || !Set.of(AD_ROUTE, PRODUCT_ROUTE).contains(routingKey)) {
            throw new IllegalArgumentException("CDC semantic event route is invalid");
        }
        List<Message> messages = new ArrayList<>(events.size());
        List<CorrelationData> correlations = new ArrayList<>(events.size());
        Set<String> eventIds = new HashSet<>();
        Set<Long> tenants = new HashSet<>();
        for (CdcChangeStrategy.SyncEvent event : events) {
            validateRouteAndSource(event, routingKey);
            if (!eventIds.add(event.getEventId()) || !tenants.add(event.getTenantId())) {
                throw new IllegalArgumentException("CDC semantic event targets must be unique per delivery");
            }
            messages.add(message(event));
            correlations.add(new CorrelationData(event.getEventId() + ":" + UUID.randomUUID()));
        }

        for (int index = 0; index < messages.size(); index++) {
            rabbitTemplate.send(SYNC_EXCHANGE, routingKey, messages.get(index), correlations.get(index));
        }
        awaitAllConfirms(events, correlations);
    }

    private void validateRouteAndSource(CdcChangeStrategy.SyncEvent event, String routingKey) {
        validation.validate(event);
        String expectedRoute = "ad".equals(event.getSourceTable()) ? AD_ROUTE : PRODUCT_ROUTE;
        if (!expectedRoute.equals(routingKey)) {
            throw new IllegalArgumentException("CDC semantic route does not match the event source table");
        }
        CdcSourceProperties.SourceRegistration source = sourceProperties.getSourceRegistry()
                .get(event.getSourceAlias());
        if (source == null || !source.getSourceEpoch().equals(event.getSourceEpoch())
                || !source.getTables().contains(event.getSourceTable())) {
            throw new IllegalArgumentException("CDC semantic event source is not registered");
        }
    }

    private Message message(CdcChangeStrategy.SyncEvent event) {
        byte[] body;
        try {
            body = objectMapper.writeValueAsBytes(event);
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("CDC event serialization failed", failure);
        }
        MessageProperties properties = new MessageProperties();
        properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        properties.setContentEncoding(StandardCharsets.UTF_8.name());
        properties.setMessageId(event.getEventId());
        properties.setHeader("schemaVersion", event.getSchemaVersion());
        properties.setHeader("tenantId", Long.toString(event.getTenantId()));
        properties.setHeader("sourceAlias", event.getSourceAlias());
        properties.setHeader("sourceEpoch", event.getSourceEpoch());
        properties.setHeader("sourceTable", event.getSourceTable());
        properties.setHeader("route", "ad".equals(event.getSourceTable()) ? AD_ROUTE : PRODUCT_ROUTE);
        return new Message(body, properties);
    }

    private void awaitAllConfirms(List<? extends CdcChangeStrategy.SyncEvent> events,
            List<CorrelationData> correlations) {
        try {
            var completed = correlations.stream().map(CorrelationData::getFuture)
                    .toArray(java.util.concurrent.CompletableFuture[]::new);
            java.util.concurrent.CompletableFuture.allOf(completed)
                    .get(sourceProperties.getConfirmTimeout().toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw new AmqpException("CDC publisher confirm wait was interrupted", failure);
        } catch (ExecutionException | CompletionException | TimeoutException failure) {
            throw new AmqpException("CDC semantic publish did not receive all confirms", failure);
        }
        for (int index = 0; index < correlations.size(); index++) {
            CorrelationData correlation = correlations.get(index);
            CorrelationData.Confirm confirm = correlation.getFuture().join();
            if (!confirm.isAck() || correlation.getReturned() != null) {
                log.warn("CDC semantic publish was not accepted eventId={} ack={} returned={}",
                        events.get(index).getEventId(), confirm.isAck(), correlation.getReturned() != null);
                throw new AmqpException("CDC semantic publish was nacked or returned");
            }
        }
    }
}
