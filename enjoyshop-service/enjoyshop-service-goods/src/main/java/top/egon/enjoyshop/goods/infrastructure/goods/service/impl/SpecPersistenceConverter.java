package top.egon.enjoyshop.goods.infrastructure.goods.service.impl;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;
import top.egon.enjoyshop.goods.domain.goods.SpecBO;
import top.egon.enjoyshop.goods.infrastructure.goods.po.SpecPO;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface SpecPersistenceConverter extends BaseForwardConverter<SpecPO, SpecBO> {

    @Override
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "options", source = "options", qualifiedByName = "splitOptions")
    @Mapping(target = "seq", source = "seq")
    SpecBO toTarget(SpecPO source);

    List<SpecBO> toTargetList(List<SpecPO> source);

    @Named("splitOptions")
    static List<String> splitOptions(String value) {
        return value == null || value.isEmpty() ? List.of() : List.of(value.split(","));
    }
}
