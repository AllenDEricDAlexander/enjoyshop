package top.egon.enjoyshop.goods.infrastructure.sync;

import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;
import top.egon.enjoyshop.goods.domain.sync.AdCacheInvalidationEvent;
import top.egon.enjoyshop.goods.domain.sync.AdInvalidationBO;

/** Maps validated domain invalidation facts to the flattened v1 wire payload. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR,
        builder = @Builder(disableBuilder = true))
public interface AdInvalidationEventConverter extends BaseForwardConverter<AdInvalidationBO, AdCacheInvalidationEvent> {

    @Override
    @Mapping(target = "schemaVersion", constant = "1")
    @Mapping(target = "sourceAlias", source = "cursor.sourceAlias")
    @Mapping(target = "sourceEpoch", source = "cursor.sourceEpoch")
    @Mapping(target = "sourceLsn", expression = "java(source.getCursor().getSourceLsnText())")
    @Mapping(target = "sourceTxId", source = "cursor.sourceTxId")
    @Mapping(target = "sourceRecordOrder", source = "cursor.sourceRecordOrder")
    @Mapping(target = "occurredAt", expression = "java(source.getCursor().getOccurredAt())")
    @Mapping(target = "sourceTable", source = "cursor.table")
    @Mapping(target = "operation", source = "cursor.operation")
    AdCacheInvalidationEvent toTarget(AdInvalidationBO source);
}
