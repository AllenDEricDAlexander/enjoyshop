package top.egon.enjoyshop.goods.adapter.sync;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import jakarta.validation.ConstraintViolationException;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.enjoyshop.goods.domain.sync.AdCacheInvalidationEvent;
import top.egon.enjoyshop.goods.domain.sync.CdcOperationEnum;
import top.egon.enjoyshop.goods.infrastructure.ad.repo.AdRepository;
import top.egon.enjoyshop.goods.infrastructure.config.CdcSourceProperties;

/** Validates a trusted tenant-scoped request, attempts cache invalidation, then acknowledges it. */
@Component("adCacheInvalidationConsumer")
@ConditionalOnProperty(name = "app.cdc.enabled", havingValue = "true")
@Validated
@Slf4j
@RequiredArgsConstructor
public class AdCacheInvalidationConsumer {

    private static final String SYNC_EXCHANGE = "enjoyshop.sync.v1";
    private static final String ROUTING_KEY = "ad.invalidate.v1";

    @Qualifier("jacksonObjectMapper")
    private final ObjectMapper objectMapper;

    @Qualifier("app.cdc-top.egon.enjoyshop.goods.infrastructure.config.CdcSourceProperties")
    private final CdcSourceProperties sourceProperties;

    @Qualifier("adRepository")
    private final AdRepository adRepository;

    @Qualifier("egonColaValidationUtils")
    private final ValidationUtils validation;

    @RabbitListener(queues = "enjoyshop.ad.invalidate.v1", containerFactory = "cdcRabbitListenerContainerFactory")
    public void onMessage(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        AdCacheInvalidationEvent event;
        try {
            validateTransport(message);
            JsonNode json = objectMapper.readTree(message.getBody());
            validateStrictWireTypes(json);
            event = objectMapper.treeToValue(json, AdCacheInvalidationEvent.class);
            validation.validate(event);
            validateRegisteredSource(event, message.getMessageProperties());
        } catch (JsonProcessingException | IllegalArgumentException | ConstraintViolationException poison) {
            log.warn("advertisement CDC message rejected reason=invalid-event routingKey={}",
                    message.getMessageProperties().getReceivedRoutingKey());
            channel.basicReject(deliveryTag, false);
            return;
        }

        Map<String, String> previousMdc = MDC.getCopyOfContextMap();
        try {
            MDC.put("tenantId", Long.toString(event.getTenantId()));
            adRepository.evictCandidates(event.getPositions());
        } finally {
            if (previousMdc == null) {
                MDC.clear();
            } else {
                MDC.setContextMap(previousMdc);
            }
        }
        channel.basicAck(deliveryTag, false);
    }

    private void validateTransport(Message message) {
        if (message == null || message.getBody() == null || message.getBody().length == 0
                || message.getBody().length > sourceProperties.getMaxPayloadBytes()) {
            throw new IllegalArgumentException("advertisement CDC payload is empty or too large");
        }
        MessageProperties properties = message.getMessageProperties();
        if (!SYNC_EXCHANGE.equals(properties.getReceivedExchange())
                || !ROUTING_KEY.equals(properties.getReceivedRoutingKey())
                || properties.getContentType() == null
                || !properties.getContentType().toLowerCase(java.util.Locale.ROOT).startsWith("application/json")) {
            throw new IllegalArgumentException("advertisement CDC transport route is invalid");
        }
    }

    private void validateStrictWireTypes(JsonNode json) {
        if (json == null || !json.isObject()) {
            throw new IllegalArgumentException("advertisement CDC event must be a JSON object");
        }
        requireAllowedFields(json, "eventId", "schemaVersion", "tenantId", "sourceAlias", "sourceEpoch",
                "sourceLsn", "sourceTxId", "sourceRecordOrder", "occurredAt", "sourceTable", "operation",
                "positions");
        if (!json.path("schemaVersion").isInt() || json.path("schemaVersion").intValue() != 1) {
            throw new IllegalArgumentException("advertisement CDC schemaVersion must be integer 1");
        }
        String tenantText = requiredText(json, "tenantId");
        if (!tenantText.matches("^[1-9][0-9]*$")) {
            throw new IllegalArgumentException("advertisement CDC tenantId must be a positive decimal string");
        }
        try {
            long tenant = Long.parseLong(tenantText);
            if (tenant <= 0 || !Long.toString(tenant).equals(tenantText)) {
                throw new IllegalArgumentException("advertisement CDC tenantId is not canonical");
            }
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException("advertisement CDC tenantId exceeds signed 64-bit range", failure);
        }
        requiredText(json, "eventId");
        requiredText(json, "sourceAlias");
        requiredText(json, "sourceEpoch");
        requiredText(json, "occurredAt");
        if (!"ad".equals(requiredText(json, "sourceTable"))) {
            throw new IllegalArgumentException("advertisement CDC source table is invalid");
        }
        CdcOperationEnum operation = CdcOperationEnum.fromCode(requiredText(json, "operation"));
        requireNullableText(json, "sourceLsn");
        requireNullableText(json, "sourceTxId");
        requireNullableText(json, "sourceRecordOrder");
        if (operation != CdcOperationEnum.SNAPSHOT
                && (json.path("sourceLsn").isNull() || json.path("sourceTxId").isNull())) {
            throw new IllegalArgumentException("live advertisement CDC events require source cursor values");
        }
        JsonNode positions = json.path("positions");
        if (!positions.isArray() || positions.isEmpty() || positions.size() > 2) {
            throw new IllegalArgumentException("advertisement CDC positions must contain one or two values");
        }
        for (JsonNode position : positions) {
            if (!position.isTextual()) {
                throw new IllegalArgumentException("advertisement CDC positions must be strings");
            }
        }
    }

    private void validateRegisteredSource(AdCacheInvalidationEvent event, MessageProperties properties) {
        CdcSourceProperties.SourceRegistration source = sourceProperties.getSourceRegistry()
                .get(event.getSourceAlias());
        if (source == null || !source.getSourceEpoch().equals(event.getSourceEpoch())
                || !source.getTables().contains("ad")) {
            throw new IllegalArgumentException("advertisement CDC source or epoch is not registered");
        }
        Map<String, Object> headers = properties.getHeaders();
        if (!event.getEventId().equals(properties.getMessageId())
                || !Integer.valueOf(1).equals(headers.get("schemaVersion"))
                || !Long.toString(event.getTenantId()).equals(headers.get("tenantId"))
                || !event.getSourceAlias().equals(headers.get("sourceAlias"))
                || !event.getSourceEpoch().equals(headers.get("sourceEpoch"))
                || !"ad".equals(headers.get("sourceTable"))
                || !ROUTING_KEY.equals(headers.get("route"))) {
            throw new IllegalArgumentException("advertisement CDC headers do not match the event body");
        }
    }

    private static String requiredText(JsonNode json, String field) {
        JsonNode value = json.get(field);
        if (value == null || !value.isTextual() || value.textValue().isBlank()) {
            throw new IllegalArgumentException("advertisement CDC required string field is invalid: " + field);
        }
        return value.textValue();
    }

    private static void requireNullableText(JsonNode json, String field) {
        JsonNode value = json.get(field);
        if (value == null || (!value.isNull() && !value.isTextual())) {
            throw new IllegalArgumentException("advertisement CDC nullable field must be a string or null: " + field);
        }
    }

    private static void requireAllowedFields(JsonNode json, String... allowed) {
        Set<String> fields = Set.of(allowed);
        json.fieldNames().forEachRemaining(field -> {
            if (!fields.contains(field)) {
                throw new IllegalArgumentException("advertisement CDC event contains an unsupported field");
            }
        });
    }
}
