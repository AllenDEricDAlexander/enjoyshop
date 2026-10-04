package top.egon.enjoyshop.goods.adapter.goods.pojo.convertor;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;
import top.egon.enjoyshop.goods.adapter.goods.pojo.dto.SpecCategoryQueryDTO;
import top.egon.enjoyshop.goods.adapter.goods.pojo.vo.SpecOptionVO;
import top.egon.enjoyshop.goods.application.goods.SpecListQuery;
import top.egon.enjoyshop.goods.application.goods.SpecResult;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface SpecAdapterConverter extends BaseForwardConverter<SpecResult, SpecOptionVO> {

    @Mapping(target = "categoryName", source = "categoryName", qualifiedByName = "trimCategoryName")
    SpecListQuery toListQuery(SpecCategoryQueryDTO source);

    @Override
    @Mapping(target = "name", source = "name")
    @Mapping(target = "options", source = "options")
    SpecOptionVO toTarget(SpecResult source);

    List<SpecOptionVO> toVOList(List<SpecResult> results);

    @Named("trimCategoryName")
    static String trimCategoryName(String value) {
        return value == null ? null : value.trim();
    }
}
