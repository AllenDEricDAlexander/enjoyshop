package top.egon.enjoyshop.goods.application.goods.converter;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;
import top.egon.enjoyshop.goods.application.goods.SpecListQuery;
import top.egon.enjoyshop.goods.domain.goods.SpecDomainQuery;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface SpecManageConverter extends BaseForwardConverter<SpecListQuery, SpecDomainQuery> {

    @Override
    @Mapping(target = "categoryName", source = "categoryName", qualifiedByName = "trimCategoryName")
    SpecDomainQuery toTarget(SpecListQuery source);

    @Named("trimCategoryName")
    static String trimCategoryName(String value) {
        return value == null ? null : value.trim();
    }
}
