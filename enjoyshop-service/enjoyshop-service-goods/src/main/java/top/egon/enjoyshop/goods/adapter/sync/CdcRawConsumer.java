package top.egon.enjoyshop.goods.adapter.sync;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rabbitmq.client.Channel;
import jakarta.validation.ConstraintViolationException;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.enjoyshop.goods.domain.sync.CdcChangeStrategy;
import top.egon.enjoyshop.goods.domain.sync.CdcOperationEnum;
import top.egon.enjoyshop.goods.domain.sync.CdcRecordBO;
import top.egon.enjoyshop.goods.domain.sync.CdcSourceCursorBO;
import top.egon.enjoyshop.goods.infrastructure.config.CdcSourceProperties;
import top.egon.enjoyshop.goods.infrastructure.sync.CdcEventPublisher;

/** Strictly normalizes committed PostgreSQL messages before publishing and acknowledging them. */
@Component("cdcRawConsumer")
@ConditionalOnProperty(name = "app.cdc.enabled", havingValue = "true")
@Validated
@Slf4j
@RequiredArgsConstructor
public class CdcRawConsumer {

    private static final String RAW_EXCHANGE = "enjoyshop.cdc.raw.v1";
    private static final String RAW_ROUTING_KEY = "master-data";
    private static final BigInteger MAX_UNSIGNED_LSN = new BigInteger("18446744073709551615");

    @Qualifier("jacksonObjectMapper")
    private final ObjectMapper objectMapper;

    @Qualifier("app.cdc-top.egon.enjoyshop.goods.infrastructure.config.CdcSourceProperties")
    private final CdcSourceProperties sourceProperties;

    @Qualifier("cdcChangeStrategies")
    private final Map<String, CdcChangeStrategy> strategies;

    @Qualifier("cdcEventPublisher")
    private final CdcEventPublisher publisher;

    @Qualifier("egonColaValidationUtils")
    private final ValidationUtils validation;

    @RabbitListener(queues = "enjoyshop.cdc.normalize.v1", containerFactory = "cdcRabbitListenerContainerFactory")
    public void onMessage(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            validateRawTransport(message);
            JsonNode envelope = objectMapper.readTree(message.getBody());
            if (isTrustedControlMessage(envelope)) {
                log.debug("ignored trusted Debezium transaction control status={}", envelope.path("status").textValue());
                channel.basicAck(deliveryTag, false);
                return;
            }
            CdcRecordBO record = toRecord(envelope);
            CdcSourceCursorBO cursor = record.getCursor();
            CdcChangeStrategy strategy = strategies.get(cursor.getTable());
            if (strategy == null || !strategy.supports(cursor.getSourceAlias(), cursor.getSchema(), cursor.getTable())) {
                throw new IllegalArgumentException("CDC source table has no registered strategy");
            }
            List<? extends CdcChangeStrategy.SyncEvent> events = strategy.normalize(record);
            publisher.publishConfirmed(events, strategy.routingKey());
            channel.basicAck(deliveryTag, false);
        } catch (JsonProcessingException | IllegalArgumentException | ConstraintViolationException poison) {
            log.warn("CDC raw message rejected reason=invalid-source-or-schema routingKey={}",
                    message.getMessageProperties().getReceivedRoutingKey());
            channel.basicReject(deliveryTag, false);
        }
    }

    private void validateRawTransport(Message message) {
        if (message == null || message.getBody() == null || message.getBody().length == 0
                || message.getBody().length > sourceProperties.getMaxPayloadBytes()) {
            throw new IllegalArgumentException("CDC raw payload is empty or exceeds its configured limit");
        }
        MessageProperties properties = message.getMessageProperties();
        if (!RAW_EXCHANGE.equals(properties.getReceivedExchange())
                || !RAW_ROUTING_KEY.equals(properties.getReceivedRoutingKey())
                || properties.getContentType() == null
                || !properties.getContentType().toLowerCase(java.util.Locale.ROOT).startsWith("application/json")) {
            throw new IllegalArgumentException("CDC raw transport metadata is not registered");
        }
    }

    private boolean isTrustedControlMessage(JsonNode envelope) {
        if (envelope == null || !envelope.isObject() || !envelope.has("status")) {
            return false;
        }
        String status = textual(envelope, "status");
        if (!"BEGIN".equals(status) && !"END".equals(status)) {
            throw new IllegalArgumentException("CDC control status is unknown");
        }
        SetSupport.requireAllowedFields(envelope,
                "status", "id", "event_count", "data_collections", "source");
        textual(envelope, "id");
        if (envelope.has("source")) {
            sourceRegistration(envelope.path("source"));
        } else if (!sourceProperties.getSourceRegistry().containsKey("master_data")) {
            throw new IllegalArgumentException("CDC control source is not registered");
        }
        if ("BEGIN".equals(status)) {
            if (envelope.has("event_count") || envelope.has("data_collections")) {
                throw new IllegalArgumentException("CDC BEGIN control shape is invalid");
            }
            return true;
        }
        nonNegativeInteger(envelope, "event_count");
        if (envelope.has("data_collections")) {
            validateDataCollections(envelope.path("data_collections"));
        }
        return true;
    }

    private void validateDataCollections(JsonNode collections) {
        if (!collections.isArray()) {
            throw new IllegalArgumentException("CDC transaction data_collections must be an array");
        }
        CdcSourceProperties.SourceRegistration source = sourceProperties.getSourceRegistry().get("master_data");
        for (JsonNode collection : collections) {
            if (!collection.isObject()) {
                throw new IllegalArgumentException("CDC transaction collection entry must be an object");
            }
            SetSupport.requireAllowedFields(collection, "data_collection", "event_count");
            String qualifiedName = textual(collection, "data_collection");
            String expectedPrefix = source.getDatabase() + "." + source.getSchema() + ".";
            if (!qualifiedName.startsWith(expectedPrefix)
                    || !source.getTables().contains(qualifiedName.substring(expectedPrefix.length()))) {
                throw new IllegalArgumentException("CDC transaction collection is not registered");
            }
            nonNegativeInteger(collection, "event_count");
        }
    }

    private CdcRecordBO toRecord(JsonNode envelope) {
        if (envelope == null || !envelope.isObject()) {
            throw new IllegalArgumentException("CDC row envelope must be an object");
        }
        SetSupport.requireAllowedFields(envelope, "before", "after", "source", "op", "transaction");
        if (!envelope.has("before") || !envelope.has("after")) {
            throw new IllegalArgumentException("CDC row envelope is missing before or after");
        }
        JsonNode sourceNode = requiredObject(envelope, "source");
        CdcSourceProperties.SourceRegistration source = sourceRegistration(sourceNode);
        CdcOperationEnum operation = CdcOperationEnum.fromCode(textual(envelope, "op"));
        JsonNode before = envelope.get("before");
        JsonNode after = envelope.get("after");
        requireOperationSides(operation, before, after);
        String table = textual(sourceNode, "table");
        if (!source.getTables().contains(table)) {
            throw new IllegalArgumentException("CDC source table is not registered");
        }

        Long beforeId = rowLong(before, "id");
        Long beforeTenant = rowLong(before, "tenant_id");
        Long beforeSpu = "sku".equals(table) ? rowLong(before, "spu_id") : null;
        String beforePosition = "ad".equals(table) ? rowText(before, "position") : null;
        Long afterId = rowLong(after, "id");
        Long afterTenant = rowLong(after, "tenant_id");
        Long afterSpu = "sku".equals(table) ? rowLong(after, "spu_id") : null;
        String afterPosition = "ad".equals(table) ? rowText(after, "position") : null;

        validateSnapshotFlag(sourceNode, operation);

        BigInteger lsn = optionalUnsignedInteger(sourceNode, "lsn");
        if (lsn != null && lsn.compareTo(MAX_UNSIGNED_LSN) > 0) {
            throw new IllegalArgumentException("CDC PostgreSQL LSN exceeds unsigned 64-bit range");
        }
        if (operation != CdcOperationEnum.SNAPSHOT && lsn == null) {
            throw new IllegalArgumentException("CDC live row is missing its PostgreSQL LSN");
        }
        String sourceTxId = decimalText(sourceNode, "txId", operation == CdcOperationEnum.SNAPSHOT);
        String recordOrder = transactionOrder(envelope.get("transaction"));
        if (operation != CdcOperationEnum.SNAPSHOT && sourceTxId == null) {
            throw new IllegalArgumentException("CDC live row is missing its source transaction id");
        }
        Long sourceTsMs = requiredLong(sourceNode, "ts_ms", true);
        CdcSourceCursorBO cursor = CdcSourceCursorBO.builder()
                .sourceAlias("master_data")
                .sourceEpoch(source.getSourceEpoch())
                .schema(source.getSchema())
                .table(table)
                .operation(operation)
                .rowPk(afterId != null ? afterId : beforeId)
                .sourceLsn(lsn)
                .sourceTxId(sourceTxId)
                .sourceRecordOrder(recordOrder)
                .sourceTsMs(sourceTsMs)
                .build();
        CdcRecordBO record = CdcRecordBO.builder()
                .cursor(cursor)
                .factFingerprint(factFingerprint(before, after))
                .beforeRowId(beforeId)
                .beforeTenantId(beforeTenant)
                .beforeSpuId(beforeSpu)
                .beforePosition(beforePosition)
                .afterRowId(afterId)
                .afterTenantId(afterTenant)
                .afterSpuId(afterSpu)
                .afterPosition(afterPosition)
                .build();
        validation.validate(record);
        return record;
    }

    private CdcSourceProperties.SourceRegistration sourceRegistration(JsonNode sourceNode) {
        if (sourceNode == null || !sourceNode.isObject()) {
            throw new IllegalArgumentException("CDC source metadata must be an object");
        }
        String sourceName = textual(sourceNode, "name");
        for (CdcSourceProperties.SourceRegistration source : sourceProperties.getSourceRegistry().values()) {
            if (source.getSourceName().equals(sourceName)) {
                if (!source.getDatabase().equals(textual(sourceNode, "db"))
                        || !source.getSchema().equals(textual(sourceNode, "schema"))) {
                    throw new IllegalArgumentException("CDC source name does not match its registered database");
                }
                return source;
            }
        }
        throw new IllegalArgumentException("CDC source name is not registered");
    }

    private String transactionOrder(JsonNode transaction) {
        if (transaction == null || transaction.isNull()) {
            return null;
        }
        if (!transaction.isObject()) {
            throw new IllegalArgumentException("CDC transaction metadata must be an object or null");
        }
        SetSupport.requireAllowedFields(transaction, "id", "total_order", "data_collection_order");
        textual(transaction, "id");
        BigInteger order = requiredInteger(transaction, "total_order", false);
        if (order.signum() <= 0) {
            throw new IllegalArgumentException("CDC transaction record order must be positive");
        }
        if (transaction.has("data_collection_order")) {
            nonNegativeInteger(transaction, "data_collection_order");
        }
        return order.toString();
    }

    private String factFingerprint(JsonNode before, JsonNode after) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            updateFact(digest, (byte) 'B', before);
            updateFact(digest, (byte) 'A', after);
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException | JsonProcessingException impossible) {
            throw new IllegalStateException("CDC source row fingerprint could not be computed", impossible);
        }
    }

    private void updateFact(MessageDigest digest, byte side, JsonNode row) throws JsonProcessingException {
        digest.update(side);
        if (row == null || row.isNull()) {
            digest.update((byte) 0);
            return;
        }
        byte[] canonical = objectMapper.writeValueAsBytes(canonicalize(row));
        digest.update((byte) 1);
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(canonical.length).array());
        digest.update(canonical);
    }

    private JsonNode canonicalize(JsonNode value) {
        if (value.isObject()) {
            ObjectNode sorted = objectMapper.createObjectNode();
            List<String> names = new ArrayList<>();
            value.fieldNames().forEachRemaining(names::add);
            names.sort(Comparator.naturalOrder());
            for (String name : names) {
                sorted.set(name, canonicalize(value.get(name)));
            }
            return sorted;
        }
        if (value.isArray()) {
            ArrayNode values = objectMapper.createArrayNode();
            value.forEach(item -> values.add(canonicalize(item)));
            return values;
        }
        return value.deepCopy();
    }

    private static void requireOperationSides(CdcOperationEnum operation, JsonNode before, JsonNode after) {
        boolean beforeRequired = operation == CdcOperationEnum.UPDATE || operation == CdcOperationEnum.DELETE;
        boolean afterRequired = operation == CdcOperationEnum.UPDATE || operation == CdcOperationEnum.CREATE
                || operation == CdcOperationEnum.SNAPSHOT;
        if (beforeRequired != (before != null && !before.isNull())
                || afterRequired != (after != null && !after.isNull())) {
            throw new IllegalArgumentException("CDC before/after values do not match the row operation");
        }
        if ((before != null && !before.isNull() && !before.isObject())
                || (after != null && !after.isNull() && !after.isObject())) {
            throw new IllegalArgumentException("CDC before/after row values must be objects or null");
        }
    }

    private static JsonNode requiredObject(JsonNode parent, String field) {
        JsonNode node = parent.get(field);
        if (node == null || !node.isObject()) {
            throw new IllegalArgumentException("CDC required object field is missing: " + field);
        }
        return node;
    }

    private static String textual(JsonNode parent, String field) {
        JsonNode value = parent == null ? null : parent.get(field);
        if (value == null || !value.isTextual() || value.textValue().isBlank()) {
            throw new IllegalArgumentException("CDC required text field is missing: " + field);
        }
        return value.textValue();
    }

    private static Long rowLong(JsonNode row, String field) {
        if (row == null || row.isNull()) {
            return null;
        }
        return requiredLong(row, field, false);
    }

    private static String rowText(JsonNode row, String field) {
        if (row == null || row.isNull()) {
            return null;
        }
        return textual(row, field);
    }

    private static void validateSnapshotFlag(JsonNode source, CdcOperationEnum operation) {
        JsonNode snapshot = source.get("snapshot");
        if (snapshot == null || snapshot.isNull()) {
            return;
        }
        boolean snapshotRecord = operation == CdcOperationEnum.SNAPSHOT;
        if (snapshot.isBoolean()) {
            if (snapshot.booleanValue() != snapshotRecord) {
                throw new IllegalArgumentException("CDC source snapshot flag does not match the row operation");
            }
            return;
        }
        if (snapshotRecord && snapshot.isTextual() && "last".equals(snapshot.textValue())) {
            return;
        }
        throw new IllegalArgumentException("CDC source snapshot flag has an unsupported shape");
    }

    private static Long requiredLong(JsonNode parent, String field, boolean allowZero) {
        BigInteger value = requiredInteger(parent, field, false);
        if (value.signum() < 0 || (!allowZero && value.signum() == 0)) {
            throw new IllegalArgumentException("CDC numeric field is outside its valid range: " + field);
        }
        try {
            return value.longValueExact();
        } catch (ArithmeticException failure) {
            throw new IllegalArgumentException("CDC numeric field exceeds signed 64-bit range: " + field, failure);
        }
    }

    private static BigInteger optionalUnsignedInteger(JsonNode parent, String field) {
        JsonNode value = parent.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.isIntegralNumber()) {
            throw new IllegalArgumentException("CDC LSN must be an exact integer");
        }
        BigInteger number = value.bigIntegerValue();
        if (number.signum() < 0) {
            throw new IllegalArgumentException("CDC LSN must be nonnegative");
        }
        return number;
    }

    private static String decimalText(JsonNode parent, String field, boolean nullable) {
        JsonNode value = parent.get(field);
        if (value == null || value.isNull()) {
            if (nullable) {
                return null;
            }
            throw new IllegalArgumentException("CDC transaction id is missing");
        }
        if (!value.isIntegralNumber()) {
            throw new IllegalArgumentException("CDC transaction id must be an exact integer");
        }
        BigInteger number = value.bigIntegerValue();
        if (number.signum() < 0 || number.toString().length() > 20) {
            throw new IllegalArgumentException("CDC transaction id is outside its valid range");
        }
        return number.toString();
    }

    private static BigInteger requiredInteger(JsonNode parent, String field, boolean nullable) {
        JsonNode value = parent == null ? null : parent.get(field);
        if (value == null || value.isNull()) {
            if (nullable) {
                return null;
            }
            throw new IllegalArgumentException("CDC required integer field is missing: " + field);
        }
        if (!value.isIntegralNumber()) {
            throw new IllegalArgumentException("CDC field must be an exact integer: " + field);
        }
        return value.bigIntegerValue();
    }

    private static void nonNegativeInteger(JsonNode parent, String field) {
        if (requiredInteger(parent, field, false).signum() < 0) {
            throw new IllegalArgumentException("CDC control counter must be nonnegative: " + field);
        }
    }

    private static final class SetSupport {

        private SetSupport() {
        }

        private static void requireAllowedFields(JsonNode object, String... allowed) {
            java.util.Set<String> fields = java.util.Set.of(allowed);
            object.fieldNames().forEachRemaining(field -> {
                if (!fields.contains(field)) {
                    throw new IllegalArgumentException("CDC message contains an unsupported field");
                }
            });
        }
    }
}
