package top.egon.enjoyshop.goods.domain.goods;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import top.egon.enjoyshop.goods.common.validation.GoodsCatalogQueryValidationGroup;
import top.egon.enjoyshop.goods.common.validation.ValidCategoryName;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class SpecDomainQuery {

    @ValidCategoryName(required = true, groups = GoodsCatalogQueryValidationGroup.class)
    private String categoryName;
}
