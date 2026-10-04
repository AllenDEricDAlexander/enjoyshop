package top.egon.enjoyshop.goods.domain.goods;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.groups.ConvertGroup;
import jakarta.validation.groups.Default;
import java.util.List;
import top.egon.enjoyshop.goods.common.validation.GoodsCatalogQueryValidationGroup;

public interface SpecDomainService {

    List<SpecBO> listByCategoryName(
            @Valid @NotNull @ConvertGroup(from = Default.class, to = GoodsCatalogQueryValidationGroup.class)
                    SpecDomainQuery query);
}
