package top.egon.enjoyshop.goods.application.goods;

import org.mapstruct.Mapper;
import org.mapstruct.AfterMapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;
import top.egon.enjoyshop.goods.domain.goods.ProductBO;
import top.egon.enjoyshop.goods.domain.goods.ProductDomainQuery;

@Mapper(componentModel = "spring", uses = {ProductSpuSaveConverter.class, ProductSkuSaveConverter.class},
        unmappedTargetPolicy = ReportingPolicy.ERROR, builder = @org.mapstruct.Builder(disableBuilder = true))
public interface ProductApplicationConverter extends BaseForwardConverter<ProductSaveCommand, ProductBO> {
    @Override
    @Mapping(target = "expectedVersion", source = "version")
    ProductBO toTarget(ProductSaveCommand source);

    @Mapping(target = "expectedVersion", source = "source.version")
    ProductBO toUpdateTarget(ProductSaveCommand source, Long spuId);

    @AfterMapping
    default void bindUpdateIdentity(ProductSaveCommand source, Long spuId, @MappingTarget ProductBO target) {
        target.getSpu().setId(spuId);
    }

    @Mapping(target = "id", source = "spuId")
    @Mapping(target = "page", ignore = true)
    @Mapping(target = "name", ignore = true)
    @Mapping(target = "sn", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "isMarketable", ignore = true)
    ProductDomainQuery toDomainQuery(ProductIdQuery source);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sn", expression = "java(source.getSn() == null ? null : source.getSn().trim())")
    @Mapping(target = "page", expression = "java(new top.egon.cola.component.common.core.pojo.PageQuery(source.getPageNo(), source.getPageSize()))")
    ProductDomainQuery toDomainQuery(ProductPageQuery source);
}
