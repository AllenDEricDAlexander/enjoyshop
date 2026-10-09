package top.egon.enjoyshop.goods.infrastructure.goods.service.impl;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseConverter;
import top.egon.enjoyshop.goods.domain.goods.SkuBO;
import top.egon.enjoyshop.goods.infrastructure.goods.po.SkuPO;
import top.egon.enjoyshop.goods.common.enums.ProductSkuDeletionKindEnum;

@Mapper(componentModel = "spring", imports = ProductSkuDeletionKindEnum.class, uses = ProductJsonFieldConverter.class, unmappedTargetPolicy = ReportingPolicy.ERROR,
        builder = @org.mapstruct.Builder(disableBuilder = true))
public interface ProductSkuPersistenceConverter extends BaseConverter<SkuPO, SkuBO> {
    @Override
    @Mapping(target = "images", source = "images", qualifiedByName = "decodeProductStrings")
    @Mapping(target = "spec", source = "spec", qualifiedByName = "decodeProductMap")
    @Mapping(target = "deletionKind", expression = "java(ProductSkuDeletionKindEnum.fromCode(source.getDeletionKind()))")
    SkuBO toTarget(SkuPO source);

    @Override
    @Mapping(target = "images", source = "images", qualifiedByName = "encodeProductField")
    @Mapping(target = "spec", source = "spec", qualifiedByName = "encodeProductField")
    @Mapping(target = "deletionKind", expression = "java(target.getDeletionKind() == null ? null : target.getDeletionKind().getCode())")
    SkuPO toSource(SkuBO target);

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
    @Mapping(target = "spuId", ignore = true)
    @Mapping(target = "deletionKind", ignore = true)
    @Mapping(target = "images", source = "images", qualifiedByName = "encodeProductField")
    @Mapping(target = "spec", source = "spec", qualifiedByName = "encodeProductField")
    void mergeForUpdate(SkuBO source, @MappingTarget SkuPO target);
}
