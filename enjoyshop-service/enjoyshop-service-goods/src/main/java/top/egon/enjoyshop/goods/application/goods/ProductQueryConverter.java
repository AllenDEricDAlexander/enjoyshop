package top.egon.enjoyshop.goods.application.goods;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;
import top.egon.enjoyshop.goods.domain.goods.ProductBO;
import top.egon.enjoyshop.goods.domain.goods.ProductStateBO;
import top.egon.enjoyshop.goods.domain.goods.SpuBO;

@Mapper(componentModel = "spring", uses = {ProductSpuResultConverter.class, ProductSkuResultConverter.class},
        unmappedTargetPolicy = ReportingPolicy.ERROR, builder = @org.mapstruct.Builder(disableBuilder = true))
public interface ProductQueryConverter extends BaseForwardConverter<ProductBO, ProductResult> {
    @Override
    ProductResult toTarget(ProductBO source);


    @Mapping(target = "isMarketable", source = "marketable")
    @Mapping(target = "isDelete", source = "deleted")
    ProductStateResult toState(ProductStateBO source);
}
