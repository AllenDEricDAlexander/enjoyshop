package top.egon.enjoyshop.goods.application.goods;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;
import top.egon.enjoyshop.goods.domain.goods.SpuBO;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR,
        builder = @org.mapstruct.Builder(disableBuilder = true))
public interface ProductSpuSaveConverter extends BaseForwardConverter<SpuSaveCommand, SpuBO> {
    @Mapping(target = "name", source = "name", qualifiedByName = "trimSpuText")
    @Mapping(target = "sn", source = "sn", qualifiedByName = "trimSpuText")
    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createUserId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateUserId", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "saleNum", ignore = true)
    @Mapping(target = "commentNum", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "isMarketable", ignore = true)
    SpuBO toTarget(SpuSaveCommand source);
    @org.mapstruct.Named("trimSpuText")
    default String trimSpuText(String value) { return value == null ? null : value.trim(); }

}
