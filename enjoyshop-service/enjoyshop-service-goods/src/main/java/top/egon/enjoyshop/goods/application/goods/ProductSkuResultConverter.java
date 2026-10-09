package top.egon.enjoyshop.goods.application.goods;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;
import top.egon.enjoyshop.goods.domain.goods.SkuBO;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR,
        builder = @org.mapstruct.Builder(disableBuilder = true))
public interface ProductSkuResultConverter extends BaseForwardConverter<SkuBO, SkuResult> {
    @Override
    SkuResult toTarget(SkuBO source);

}
