package top.egon.enjoyshop.goods.domain.goods;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import top.egon.enjoyshop.goods.common.validation.GoodsCatalogQueryValidationGroup;
import top.egon.enjoyshop.goods.common.validation.ValidCategoryName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import top.egon.cola.component.common.core.pojo.PageQuery;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class BrandDomainQuery {

    private String name;

    private String letter;

    @ValidCategoryName(required = false, groups = GoodsCatalogQueryValidationGroup.class)
    private String categoryName;

    @Valid
    @NotNull
    @Builder.Default
    private PageQuery page = PageQuery.defaultPage();
}
