package top.egon.enjoyshop.goods.application.goods.converter;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import top.egon.enjoyshop.goods.domain.goods.BrandBO;
import top.egon.enjoyshop.goods.application.goods.BrandResult;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface BrandResultConverter extends BaseForwardConverter<BrandBO, BrandResult> {

    @Override
    @Mapping(target = "id", expression = "java(source.getId() == null ? null : Long.toString(source.getId()))")
    BrandResult toTarget(BrandBO source);
}
