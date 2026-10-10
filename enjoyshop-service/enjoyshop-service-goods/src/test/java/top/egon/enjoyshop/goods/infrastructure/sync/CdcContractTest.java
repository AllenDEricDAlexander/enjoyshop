package top.egon.enjoyshop.goods.infrastructure.sync;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.enjoyshop.goods.domain.sync.AdCacheInvalidationEvent;
import top.egon.enjoyshop.goods.domain.sync.CdcChangeStrategy;
import top.egon.enjoyshop.goods.domain.sync.CdcOperationEnum;
import top.egon.enjoyshop.goods.domain.sync.CdcRecordBO;
import top.egon.enjoyshop.goods.domain.sync.CdcSourceCursorBO;
import top.egon.enjoyshop.goods.domain.sync.ProductRefreshBO;
import top.egon.enjoyshop.goods.domain.sync.ProductRefreshRequestedEvent;
import top.egon.enjoyshop.goods.infrastructure.config.CdcSourceProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Contracts for committed-row normalization and the future current-state refresh protocol. */
class CdcContractTest {

    private static final long TENANT_ID = 41L;
    private static final Instant OCCURRED_AT = Instant.parse("2026-10-10T01:00:00Z");

    @Test
    void normalizesAdOldNew() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            ValidationUtils validation = new ValidationUtils(factory.getValidator());
            AdCdcChangeStrategy strategy = adStrategy(validation);
            assertThat(strategy.supports("master_data", "public", "ad")).isTrue();
            assertThat(strategy.supports("master_data", "public", "spu")).isFalse();
            assertThat(strategy.routingKey()).isEqualTo("ad.invalidate.v1");
            CdcRecordBO moved = record("ad", CdcOperationEnum.UPDATE, 9001L)
                    .setBeforeRowId(9001L).setBeforeTenantId(TENANT_ID).setBeforePosition("web_index_mobile")
                    .setAfterRowId(9001L).setAfterTenantId(TENANT_ID).setAfterPosition("web_index_lb");

            List<? extends CdcChangeStrategy.SyncEvent> events = strategy.normalize(moved);

            assertThat(events).hasSize(1);
            AdCacheInvalidationEvent event = (AdCacheInvalidationEvent) events.getFirst();
            assertThat(event.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(event.getPositions()).containsExactly("web_index_lb", "web_index_mobile");
            assertThat(event.getSourceLsn()).isEqualTo("0/016B6A80");
            assertThat(event.getOccurredAt()).isEqualTo(OCCURRED_AT);

            CdcRecordBO tenantMove = record("ad", CdcOperationEnum.UPDATE, 9001L)
                    .setBeforeRowId(9001L).setBeforeTenantId(41L).setBeforePosition("web_index_lb")
                    .setAfterRowId(9001L).setAfterTenantId(42L).setAfterPosition("web_index_lb");
            List<? extends CdcChangeStrategy.SyncEvent> split = strategy.normalize(tenantMove);
            assertThat(split).extracting(CdcChangeStrategy.SyncEvent::getTenantId).containsExactly(41L, 42L);
            assertThat(split).allSatisfy(value -> assertThat(((AdCacheInvalidationEvent) value).getPositions())
                    .containsExactly("web_index_lb"));

            CdcRecordBO deleted = record("ad", CdcOperationEnum.DELETE, 9001L)
                    .setBeforeRowId(9001L).setBeforeTenantId(TENANT_ID).setBeforePosition("web_index_mobile");
            AdCacheInvalidationEvent deletion = (AdCacheInvalidationEvent) strategy.normalize(deleted).getFirst();
            assertThat(deletion.getPositions()).containsExactly("web_index_mobile");

            CdcRecordBO created = record("ad", CdcOperationEnum.CREATE, 9002L)
                    .setAfterRowId(9002L).setAfterTenantId(TENANT_ID).setAfterPosition("web_index_lb");
            assertThat(((AdCacheInvalidationEvent) strategy.normalize(created).getFirst()).getOperation())
                    .isEqualTo(CdcOperationEnum.CREATE);
            CdcRecordBO snapshot = record("ad", CdcOperationEnum.SNAPSHOT, 9003L)
                    .setAfterRowId(9003L).setAfterTenantId(TENANT_ID).setAfterPosition("web_index_lb");
            snapshot.getCursor().setSourceLsn(null).setSourceTxId(null).setSourceRecordOrder(null);
            AdCacheInvalidationEvent snapshotEvent = (AdCacheInvalidationEvent) strategy.normalize(snapshot).getFirst();
            assertThat(snapshotEvent.getOperation()).isEqualTo(CdcOperationEnum.SNAPSHOT);
            assertThat(snapshotEvent.getSourceLsn()).isNull();
        }
    }

    @Test
    void normalizesSkuRoots() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            ValidationUtils validation = new ValidationUtils(factory.getValidator());
            ProductCdcChangeStrategy strategy = productStrategy(validation);
            assertThat(strategy.supports("master_data", "public", "spu")).isTrue();
            assertThat(strategy.supports("master_data", "public", "sku")).isTrue();
            assertThat(strategy.supports("master_data", "public", "ad")).isFalse();
            assertThat(strategy.routingKey()).isEqualTo("product.refresh.v1");
            CdcRecordBO movedSku = record("sku", CdcOperationEnum.UPDATE, 7001L)
                    .setBeforeRowId(7001L).setBeforeTenantId(TENANT_ID).setBeforeSpuId(101L)
                    .setAfterRowId(7001L).setAfterTenantId(TENANT_ID).setAfterSpuId(102L);

            List<? extends CdcChangeStrategy.SyncEvent> events = strategy.normalize(movedSku);

            assertThat(events).hasSize(1);
            ProductRefreshRequestedEvent event = (ProductRefreshRequestedEvent) events.getFirst();
            assertThat(event.getSpuIds()).containsExactly(101L, 102L);
            assertThat(event.getOperation()).isEqualTo(CdcOperationEnum.UPDATE);
            assertThat(event.getSourceTable()).isEqualTo("sku");

            CdcRecordBO spu = record("spu", CdcOperationEnum.DELETE, 101L)
                    .setBeforeRowId(101L).setBeforeTenantId(TENANT_ID);
            ProductRefreshRequestedEvent deletedSpu = (ProductRefreshRequestedEvent) strategy.normalize(spu).getFirst();
            assertThat(deletedSpu.getSpuIds()).containsExactly(101L);
            assertThat(deletedSpu.getOperation()).isEqualTo(CdcOperationEnum.DELETE);

            CdcRecordBO tenantMove = record("sku", CdcOperationEnum.UPDATE, 7001L)
                    .setBeforeRowId(7001L).setBeforeTenantId(41L).setBeforeSpuId(101L)
                    .setAfterRowId(7001L).setAfterTenantId(42L).setAfterSpuId(102L);
            assertThat(strategy.normalize(tenantMove))
                    .extracting(CdcChangeStrategy.SyncEvent::getTenantId)
                    .containsExactly(41L, 42L);
        }
    }

    @Test
    void stableEventIdentity() {
        ObjectMapper mapper = objectMapper();
        CdcEventIdentityComponent identity = new CdcEventIdentityComponent(mapper);
        CdcRecordBO source = record("ad", CdcOperationEnum.UPDATE, 9001L)
                .setBeforeRowId(9001L).setBeforeTenantId(41L).setBeforePosition("web_index_lb")
                .setAfterRowId(9001L).setAfterTenantId(41L).setAfterPosition("web_index_mobile");

        String eventId = identity.eventId(source, 41L, "ad.invalidate.v1");

        assertThat(eventId).hasSize(64).matches("[a-f0-9]{64}");
        assertThat(identity.eventId(source, 41L, "ad.invalidate.v1")).isEqualTo(eventId);
        assertThat(identity.eventId(source, 42L, "ad.invalidate.v1")).isNotEqualTo(eventId);
        assertThat(identity.eventId(source, 41L, "product.refresh.v1")).isNotEqualTo(eventId);
        assertThat(identity.eventId(source.setFactFingerprint("b".repeat(64)), 41L, "ad.invalidate.v1"))
                .isNotEqualTo(eventId);
        CdcRecordBO snapshot = record("ad", CdcOperationEnum.SNAPSHOT, 9001L)
                .setAfterRowId(9001L).setAfterTenantId(41L).setAfterPosition("web_index_lb");
        snapshot.getCursor().setSourceLsn(null).setSourceTxId(null).setSourceRecordOrder(null);
        CdcRecordBO zeroLsnSnapshot = record("ad", CdcOperationEnum.SNAPSHOT, 9001L)
                .setAfterRowId(9001L).setAfterTenantId(41L).setAfterPosition("web_index_lb");
        zeroLsnSnapshot.getCursor().setSourceTxId(null).setSourceRecordOrder(null);
        assertThat(identity.eventId(snapshot, 41L, "ad.invalidate.v1"))
                .isNotEqualTo(identity.eventId(zeroLsnSnapshot, 41L, "ad.invalidate.v1"));
        assertThat(CdcSourceCursorBO.builder().sourceLsn(BigInteger.valueOf(23_816_832L)).build()
                .getSourceLsnText()).isEqualTo("0/016B6A80");
        assertThat(CdcSourceCursorBO.builder().sourceLsn(new BigInteger("18446744073709551615")).build()
                .getSourceLsnText()).isEqualTo("FFFFFFFF/FFFFFFFF");
    }

    @Test
    void rejectsPoison() {
        assertThatThrownBy(() -> CdcOperationEnum.fromCode("t"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CdcOperationEnum.fromCode("?"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(CdcOperationEnum.values()).extracting(CdcOperationEnum::getCode)
                .containsExactly("c", "u", "d", "r");

        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            ValidationUtils validation = new ValidationUtils(factory.getValidator());
            validation.validate(new CdcSourceProperties());
            CdcRecordBO updateWithoutBefore = record("ad", CdcOperationEnum.UPDATE, 9001L)
                    .setAfterRowId(9001L).setAfterTenantId(TENANT_ID).setAfterPosition("web_index_lb");
            assertThatThrownBy(() -> validation.validate(updateWithoutBefore))
                    .isInstanceOf(ConstraintViolationException.class);
            assertThatThrownBy(() -> validation.validate(record("ad", CdcOperationEnum.CREATE, 9001L)
                    .setAfterRowId(9002L).setAfterTenantId(TENANT_ID).setAfterPosition("web_index_lb")))
                    .isInstanceOf(ConstraintViolationException.class);
            assertThatThrownBy(() -> validation.validate(record("spu", CdcOperationEnum.CREATE, 101L)
                    .setAfterRowId(101L).setAfterTenantId(TENANT_ID).setAfterSpuId(102L)))
                    .isInstanceOf(ConstraintViolationException.class);
            assertThatThrownBy(() -> validation.validate(CdcSourceCursorBO.builder()
                    .sourceAlias("master_data").sourceEpoch("e1").schema("public").table("ad")
                    .operation(CdcOperationEnum.CREATE).rowPk(1L)
                    .sourceLsn(new BigInteger("18446744073709551616")).sourceTsMs(0L).build()))
                    .isInstanceOf(ConstraintViolationException.class);
            validation.validate(CdcSourceCursorBO.builder()
                    .sourceAlias("master_data").sourceEpoch("e1").schema("public").table("ad")
                    .operation(CdcOperationEnum.CREATE).rowPk(1L).sourceLsn(BigInteger.ZERO)
                    .sourceTxId("17").sourceTsMs(0L).build());

            CdcSourceProperties properties = new CdcSourceProperties().setEnabled(true).setSourceRegistry(Map.of());
            assertThatThrownBy(() -> validation.validate(properties))
                    .isInstanceOf(ConstraintViolationException.class);
            CdcSourceProperties.SourceRegistration registration = new CdcSourceProperties.SourceRegistration(
                    "enjoyshop_cdc_master_e1", "e1", "enjoyshop_cdc_master_e1", "goods_master", "public",
                    Set.of("ad", "spu", "sku"));
            validation.validate(properties.setSourceRegistry(Map.of("master_data", registration)));
            assertThatThrownBy(() -> productStrategy(validation).normalize(
                    record("teaching", CdcOperationEnum.CREATE, 1L)
                            .setAfterRowId(1L).setAfterTenantId(TENANT_ID)))
                    .isInstanceOf(ConstraintViolationException.class);
            CdcRecordBO unregisteredSource = record("ad", CdcOperationEnum.CREATE, 1L)
                    .setAfterRowId(1L).setAfterTenantId(TENANT_ID).setAfterPosition("web_index_lb");
            unregisteredSource.getCursor().setSourceAlias("other");
            assertThatThrownBy(() -> adStrategy(validation).normalize(unregisteredSource))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void refreshProtocolIsCurrentState() throws Exception {
        ObjectMapper mapper = objectMapper();
        CdcEventIdentityComponent identity = new CdcEventIdentityComponent(mapper);
        CdcRecordBO source = record("sku", CdcOperationEnum.UPDATE, 7001L)
                .setBeforeRowId(7001L).setBeforeTenantId(TENANT_ID).setBeforeSpuId(101L)
                .setAfterRowId(7001L).setAfterTenantId(TENANT_ID).setAfterSpuId(102L);
        List<Long> mutableRoots = new ArrayList<>(List.of(101L, 102L));
        ProductRefreshBO refresh = ProductRefreshBO.builder()
                .eventId(identity.eventId(source, TENANT_ID, "product.refresh.v1"))
                .tenantId(TENANT_ID)
                .cursor(source.getCursor())
                .spuIds(mutableRoots)
                .build();
        mutableRoots.clear();
        ProductRefreshRequestedEvent event = new ProductRefreshEventConverterImpl().toTarget(refresh);
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            new ValidationUtils(factory.getValidator()).validate(event);
        }

        JsonNode json = mapper.readTree(mapper.writeValueAsBytes(event));
        assertThat(json.path("schemaVersion").intValue()).isEqualTo(1);
        assertThat(json.path("tenantId").textValue()).isEqualTo("41");
        assertThat(json.path("spuIds").isArray()).isTrue();
        assertThat(json.path("spuIds").get(0).textValue()).isEqualTo("101");
        assertThat(json.path("spuIds").get(1).textValue()).isEqualTo("102");
        assertThat(json.path("operation").textValue()).isEqualTo("u");
        assertThat(json.path("sourceLsn").textValue()).isEqualTo("0/016B6A80");
        assertThat(json.path("sourceEpoch").textValue()).isEqualTo("e1");
        assertThat(json.path("sourceTxId").textValue()).isEqualTo("17");
        assertThat(json.path("sourceRecordOrder").textValue()).isEqualTo("1");
        assertThat(json.path("occurredAt").textValue()).isEqualTo("2026-10-10T01:00:00Z");
        assertThat(json.has("before")).isFalse();
        assertThat(json.has("after")).isFalse();
        assertThat(json.has("status")).isFalse();
        assertThat(json.has("spuIdsCanonical")).isFalse();
        assertThatThrownBy(() -> event.getSpuIds().add(103L)).isInstanceOf(UnsupportedOperationException.class);

        JsonNode withUnknown = mapper.readTree(mapper.writeValueAsBytes(event));
        ((ObjectNode) withUnknown).put("marketable", true);
        assertThatThrownBy(() -> mapper.treeToValue(withUnknown, ProductRefreshRequestedEvent.class))
                .hasRootCauseInstanceOf(IllegalArgumentException.class);

        String fingerprint = identity.contentFingerprint(event);
        ProductRefreshRequestedEvent conflictingPayload = new ProductRefreshEventConverterImpl().toTarget(
                ProductRefreshBO.builder().eventId(refresh.getEventId()).tenantId(TENANT_ID)
                        .cursor(source.getCursor()).spuIds(List.of(101L)).build());
        assertThat(conflictingPayload.getEventId()).isEqualTo(event.getEventId());
        assertThat(identity.contentFingerprint(conflictingPayload)).isNotEqualTo(fingerprint);
        assertThat(refresh.getSpuIds()).containsExactly(101L, 102L);
    }

    private static AdCdcChangeStrategy adStrategy(ValidationUtils validation) {
        return new AdCdcChangeStrategy(new CdcEventIdentityComponent(objectMapper()),
                new AdInvalidationEventConverterImpl(), validation);
    }

    private static ProductCdcChangeStrategy productStrategy(ValidationUtils validation) {
        return new ProductCdcChangeStrategy(new CdcEventIdentityComponent(objectMapper()),
                new ProductRefreshEventConverterImpl(), validation);
    }

    private static CdcRecordBO record(String table, CdcOperationEnum operation, Long rowPk) {
        return CdcRecordBO.builder()
                .cursor(CdcSourceCursorBO.builder()
                        .sourceAlias("master_data")
                        .sourceEpoch("e1")
                        .schema("public")
                        .table(table)
                        .operation(operation)
                        .rowPk(rowPk)
                        .sourceLsn(BigInteger.valueOf(23_816_832L))
                        .sourceTxId("17")
                        .sourceRecordOrder("1")
                        .sourceTsMs(OCCURRED_AT.toEpochMilli())
                        .build())
                .factFingerprint("a".repeat(64))
                .build();
    }

    private static ObjectMapper objectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
