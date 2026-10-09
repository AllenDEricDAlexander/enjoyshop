package top.egon.enjoyshop.goods.application.goods;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;
import top.egon.enjoyshop.goods.domain.goods.SpuBO;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR,
        builder = @org.mapstruct.Builder(disableBuilder = true))
public interface ProductSpuResultConverter extends BaseForwardConverter<SpuBO, SpuResult> {
    @Override
    @Mapping(target = "isDelete", expression = "java(source.getDeletedAt() != null)")
    SpuResult toTarget(SpuBO source);

    @Mapping(target = "isDelete", expression = "java(source.getDeletedAt() != null)")
    ProductSummaryResult toSummary(SpuBO source);
}
