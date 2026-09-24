package top.egon.enjoyshop.goods.application.goods.converter;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import top.egon.enjoyshop.goods.application.goods.UpdateBrandCommand;
import top.egon.enjoyshop.goods.domain.goods.BrandBO;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface UpdateBrandCommandConverter extends BaseForwardConverter<UpdateBrandCommand, BrandBO> {

    @Override
    @Mapping(target = "version", source = "expectedVersion")
    BrandBO toTarget(UpdateBrandCommand source);
}
