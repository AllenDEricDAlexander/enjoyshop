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
import top.egon.enjoyshop.goods.domain.sync.AdCacheInvalidationEvent;
import top.egon.enjoyshop.goods.domain.sync.AdInvalidationBO;
import top.egon.enjoyshop.goods.domain.sync.CdcChangeStrategy;
import top.egon.enjoyshop.goods.domain.sync.CdcOperationEnum;
import top.egon.enjoyshop.goods.domain.sync.CdcRecordBO;
import top.egon.enjoyshop.goods.domain.sync.CdcSourceCursorBO;

/** Converts ad before/after positions into tenant-scoped idempotent eviction requests. */
@Component("adCdcChangeStrategy")
@Validated
@Slf4j
@RequiredArgsConstructor
public class AdCdcChangeStrategy implements CdcChangeStrategy {

    private static final String ROUTING_KEY = "ad.invalidate.v1";

    @Qualifier("cdcEventIdentityComponent")
    private final CdcEventIdentityComponent identity;

    @Qualifier("adInvalidationEventConverterImpl")
    private final AdInvalidationEventConverter converter;

    @Qualifier("egonColaValidationUtils")
    private final ValidationUtils validation;

    @Override
    public boolean supports(String sourceAlias, String schema, String table) {
        return "master_data".equals(sourceAlias) && "public".equals(schema) && "ad".equals(table);
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
            throw new IllegalArgumentException("CDC source is not registered for advertisement invalidation");
        }
        Map<Long, TreeSet<String>> targets = new TreeMap<>();
        if (cursor.getOperation() != CdcOperationEnum.CREATE
                && cursor.getOperation() != CdcOperationEnum.SNAPSHOT) {
            addTarget(targets, record.getBeforeTenantId(), record.getBeforePosition());
        }
        if (cursor.getOperation() != CdcOperationEnum.DELETE) {
            addTarget(targets, record.getAfterTenantId(), record.getAfterPosition());
        }
        List<SyncEvent> events = new ArrayList<>(targets.size());
        for (Map.Entry<Long, TreeSet<String>> target : targets.entrySet()) {
            AdInvalidationBO invalidation = AdInvalidationBO.builder()
                    .eventId(identity.eventId(record, target.getKey(), ROUTING_KEY))
                    .tenantId(target.getKey())
                    .cursor(cursor)
                    .positions(List.copyOf(target.getValue()))
                    .build();
            validation.validate(invalidation);
            AdCacheInvalidationEvent event = converter.toTarget(invalidation);
            validation.validate(event);
            events.add(event);
        }
        log.debug("normalized advertisement CDC source={} operation={} targetCount={}",
                cursor.getSourceAlias(), cursor.getOperation().getCode(), events.size());
        return List.copyOf(events);
    }

    private static void addTarget(Map<Long, TreeSet<String>> targets, Long tenantId, String position) {
        if (tenantId == null || position == null) {
            throw new IllegalArgumentException("Advertisement CDC identity is incomplete");
        }
        targets.computeIfAbsent(tenantId, ignored -> new TreeSet<>()).add(position);
        if (targets.size() > 2) {
            throw new IllegalArgumentException("Advertisement CDC target count exceeds two tenants");
        }
    }
}
