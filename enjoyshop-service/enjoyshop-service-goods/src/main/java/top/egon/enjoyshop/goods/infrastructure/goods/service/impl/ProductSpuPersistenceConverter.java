package top.egon.enjoyshop.goods.infrastructure.goods.service.impl;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseConverter;
import top.egon.enjoyshop.goods.domain.goods.SpuBO;
import top.egon.enjoyshop.goods.infrastructure.goods.po.SpuPO;

@Mapper(componentModel = "spring", uses = ProductJsonFieldConverter.class, unmappedTargetPolicy = ReportingPolicy.ERROR,
        builder = @org.mapstruct.Builder(disableBuilder = true))
public interface ProductSpuPersistenceConverter extends BaseConverter<SpuPO, SpuBO> {
    @Override
    @Mapping(target = "images", source = "images", qualifiedByName = "decodeProductStrings")
    @Mapping(target = "saleService", source = "saleService", qualifiedByName = "decodeProductStrings")
    @Mapping(target = "specItems", source = "specItems", qualifiedByName = "decodeProductSpecItems")
    @Mapping(target = "paraItems", source = "paraItems", qualifiedByName = "decodeProductMap")
    SpuBO toTarget(SpuPO source);

    @Override
    @Mapping(target = "images", source = "images", qualifiedByName = "encodeProductField")
    @Mapping(target = "saleService", source = "saleService", qualifiedByName = "encodeProductField")
    @Mapping(target = "specItems", source = "specItems", qualifiedByName = "encodeProductField")
    @Mapping(target = "paraItems", source = "paraItems", qualifiedByName = "encodeProductField")
    SpuPO toSource(SpuBO target);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createUserId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateUserId", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "sn", ignore = true)
    @Mapping(target = "saleNum", ignore = true)
    @Mapping(target = "commentNum", ignore = true)
    @Mapping(target = "images", source = "images", qualifiedByName = "encodeProductField")
    @Mapping(target = "saleService", source = "saleService", qualifiedByName = "encodeProductField")
    @Mapping(target = "specItems", source = "specItems", qualifiedByName = "encodeProductField")
    @Mapping(target = "paraItems", source = "paraItems", qualifiedByName = "encodeProductField")
    void mergeForUpdate(SpuBO source, @MappingTarget SpuPO target);
}
