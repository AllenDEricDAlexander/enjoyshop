package top.egon.enjoyshop.goods.application.goods;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;
import top.egon.enjoyshop.goods.domain.goods.SkuBO;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR,
        builder = @org.mapstruct.Builder(disableBuilder = true))
public interface ProductSkuSaveConverter extends BaseForwardConverter<SkuSaveCommand, SkuBO> {
    @Mapping(target = "sn", source = "sn", qualifiedByName = "trimSkuText")
    @Override
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createUserId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateUserId", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "saleNum", ignore = true)
    @Mapping(target = "commentNum", ignore = true)
    @Mapping(target = "spuId", ignore = true)
    @Mapping(target = "categoryId", ignore = true)
    @Mapping(target = "categoryName", ignore = true)
    @Mapping(target = "brandName", ignore = true)
    @Mapping(target = "name", ignore = true)
    @Mapping(target = "deletionKind", ignore = true)
    SkuBO toTarget(SkuSaveCommand source);
    @org.mapstruct.Named("trimSkuText")
    default String trimSkuText(String value) { return value == null ? null : value.trim(); }

}
