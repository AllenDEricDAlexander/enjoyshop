package top.egon.enjoyshop.goods.domain.goods;

import jakarta.validation.constraints.AssertTrue;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.enjoyshop.goods.common.validation.ProductValidationGroup;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class ProductDomainQuery {

    @NotNull(groups = ProductValidationGroup.Detail.class)
    @Positive
    private Long id;

    @NotNull
    @Builder.Default
    private Boolean deleted = false;

    @NotNull(groups = ProductValidationGroup.Page.class)
    private PageQuery page;

    @Size(min = 1, max = 200)
    private String name;

    @Size(min = 1, max = 64)
    private String sn;

    @Pattern(regexp = "^(?:0|1)$")
    private String status;

    private Boolean isMarketable;
    @JsonIgnore
    @AssertTrue(groups = ProductValidationGroup.Page.class, message = "product pages are limited to 100 rows")
    public boolean isPageBounded() {
        return page == null || (page.pageNo() >= 1 && page.pageSize() >= 1 && page.pageSize() <= 100);
    }
}
