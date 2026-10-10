package top.egon.enjoyshop.goods.infrastructure.ad.converter;

import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseConverter;
import top.egon.enjoyshop.goods.domain.ad.AdBO;
import top.egon.enjoyshop.goods.infrastructure.ad.po.AdPO;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR,
        builder = @Builder(disableBuilder = true))
public interface AdPersistenceConverter extends BaseConverter<AdPO, AdBO> {

    @Override
    @Mapping(target = "startAt", source = "startTime")
    @Mapping(target = "endAt", source = "endTime")
    AdBO toTarget(AdPO source);

    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createUserId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateUserId", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "startTime", source = "startAt")
    @Mapping(target = "endTime", source = "endAt")
    AdPO toSource(AdBO target);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createUserId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateUserId", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "startTime", source = "startAt")
    @Mapping(target = "endTime", source = "endAt")
    void mergeForUpdate(AdBO source, @MappingTarget AdPO target);
}
