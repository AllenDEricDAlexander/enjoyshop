package top.egon.enjoyshop.goods.infrastructure.sync;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.enjoyshop.goods.domain.sync.CdcChangeStrategy;
import top.egon.enjoyshop.goods.domain.sync.CdcOperationEnum;
import top.egon.enjoyshop.goods.domain.sync.CdcRecordBO;
import top.egon.enjoyshop.goods.domain.sync.CdcSourceCursorBO;
import top.egon.enjoyshop.goods.domain.sync.ProductRefreshBO;
import top.egon.enjoyshop.goods.domain.sync.ProductRefreshRequestedEvent;

/** Converts SPU and SKU changes into tenant-scoped requests to reread current product state. */
@Component("productCdcChangeStrategy")
@Validated
@Slf4j
@RequiredArgsConstructor
public class ProductCdcChangeStrategy implements CdcChangeStrategy {

    private static final String ROUTING_KEY = "product.refresh.v1";

    @Qualifier("cdcEventIdentityComponent")
    private final CdcEventIdentityComponent identity;

    @Qualifier("productRefreshEventConverterImpl")
    private final ProductRefreshEventConverter converter;

    @Qualifier("egonColaValidationUtils")
    private final ValidationUtils validation;

    @Override
    public boolean supports(String sourceAlias, String schema, String table) {
        return "master_data".equals(sourceAlias) && "public".equals(schema)
                && ("spu".equals(table) || "sku".equals(table));
    }

    @Override
    public String routingKey() {
        return ROUTING_KEY;
    }

    @Override
    public List<? extends SyncEvent> normalize(@jakarta.validation.Valid CdcRecordBO record) {
        validation.validate(record);
        CdcSourceCursorBO cursor = record.getCursor();
        if (!supports(cursor.getSourceAlias(), cursor.getSchema(), cursor.getTable())) {
            throw new IllegalArgumentException("CDC source is not registered for product refresh");
        }
        Map<Long, TreeSet<Long>> targets = new TreeMap<>();
        if (cursor.getOperation() != CdcOperationEnum.CREATE
                && cursor.getOperation() != CdcOperationEnum.SNAPSHOT) {
            addTarget(targets, record.getBeforeTenantId(), rootId(cursor.getTable(), record.getBeforeRowId(),
                    record.getBeforeSpuId()));
        }
        if (cursor.getOperation() != CdcOperationEnum.DELETE) {
            addTarget(targets, record.getAfterTenantId(), rootId(cursor.getTable(), record.getAfterRowId(),
                    record.getAfterSpuId()));
        }
        List<SyncEvent> events = new ArrayList<>(targets.size());
        for (Map.Entry<Long, TreeSet<Long>> target : targets.entrySet()) {
            ProductRefreshBO refresh = ProductRefreshBO.builder()
                    .eventId(identity.eventId(record, target.getKey(), ROUTING_KEY))
                    .tenantId(target.getKey())
                    .cursor(cursor)
                    .spuIds(List.copyOf(target.getValue()))
                    .build();
            validation.validate(refresh);
            ProductRefreshRequestedEvent event = converter.toTarget(refresh);
            validation.validate(event);
            events.add(event);
        }
        log.debug("normalized product CDC source={} operation={} targetCount={}",
                cursor.getSourceAlias(), cursor.getOperation().getCode(), events.size());
        return List.copyOf(events);
    }

    private static Long rootId(String table, Long rowId, Long spuId) {
        return switch (table) {
            case "spu" -> rowId;
            case "sku" -> spuId;
            default -> throw new IllegalArgumentException("Unsupported product CDC table");
        };
    }

    private static void addTarget(Map<Long, TreeSet<Long>> targets, Long tenantId, Long spuId) {
        if (tenantId == null || spuId == null || spuId <= 0) {
            throw new IllegalArgumentException("Product CDC identity is incomplete");
        }
        targets.computeIfAbsent(tenantId, ignored -> new TreeSet<>()).add(spuId);
        if (targets.size() > 2) {
            throw new IllegalArgumentException("Product CDC target count exceeds two tenants");
        }
    }
}
