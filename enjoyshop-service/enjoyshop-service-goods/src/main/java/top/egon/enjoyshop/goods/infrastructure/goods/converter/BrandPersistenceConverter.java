package top.egon.enjoyshop.goods.infrastructure.goods.service.impl;

import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import top.egon.enjoyshop.goods.infrastructure.goods.po.BrandPO;
import top.egon.enjoyshop.goods.domain.goods.BrandBO;
import top.egon.cola.component.common.core.converter.BaseConverter;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR, builder = @Builder(disableBuilder = true))
public interface BrandPersistenceConverter extends BaseConverter<BrandPO, BrandBO> {

    @Override
    BrandBO toTarget(BrandPO source);

    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createUserId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateUserId", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Override
    BrandPO toSource(BrandBO target);
}
