package top.egon.enjoyshop.goods.application.ad;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;
import top.egon.enjoyshop.goods.domain.ad.AdBO;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR,
        builder = @org.mapstruct.Builder(disableBuilder = true))
public interface AdResultConverter extends BaseForwardConverter<AdBO, AdResult> {

    @Override
    AdResult toTarget(AdBO source);
}
