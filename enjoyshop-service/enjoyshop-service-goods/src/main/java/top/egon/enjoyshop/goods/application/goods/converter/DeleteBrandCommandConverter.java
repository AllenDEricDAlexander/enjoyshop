package top.egon.enjoyshop.goods.application.goods.converter;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import top.egon.enjoyshop.goods.application.goods.DeleteBrandCommand;
import top.egon.enjoyshop.goods.domain.goods.BrandBO;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface DeleteBrandCommandConverter extends BaseForwardConverter<DeleteBrandCommand, BrandBO> {

    @Override
    @Mapping(target = "version", source = "expectedVersion")
    @Mapping(target = "name", ignore = true)
    @Mapping(target = "image", ignore = true)
    @Mapping(target = "letter", ignore = true)
    @Mapping(target = "seq", ignore = true)
    BrandBO toTarget(DeleteBrandCommand source);
}
