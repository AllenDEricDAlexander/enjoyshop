package top.egon.enjoyshop.goods.application.goods.converter;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;
import top.egon.enjoyshop.goods.application.goods.SpecResult;
import top.egon.enjoyshop.goods.domain.goods.SpecBO;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface SpecResultConverter extends BaseForwardConverter<SpecBO, SpecResult> {

    @Override
    @Mapping(target = "name", source = "name")
    @Mapping(target = "options", source = "options")
    SpecResult toTarget(SpecBO source);

    List<SpecResult> toTargetList(List<SpecBO> source);
}
