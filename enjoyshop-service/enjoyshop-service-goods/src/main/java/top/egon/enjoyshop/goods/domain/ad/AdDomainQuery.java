package top.egon.enjoyshop.goods.domain.ad;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.enjoyshop.goods.common.validation.AdValidationGroup;
import top.egon.enjoyshop.goods.common.validation.ValidAdPosition;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class AdDomainQuery {

    @NotNull(groups = AdValidationGroup.Detail.class)
    @Positive(groups = AdValidationGroup.Detail.class)
    private Long id;

    @NotNull(groups = {AdValidationGroup.Detail.class, AdValidationGroup.Page.class, AdValidationGroup.PublicRead.class})
    @Positive(groups = {AdValidationGroup.Detail.class, AdValidationGroup.Page.class, AdValidationGroup.PublicRead.class})
    private Long tenantId;

    @ValidAdPosition(groups = {AdValidationGroup.Page.class, AdValidationGroup.PublicRead.class})
    private String position;

    private AdStatusEnum status;

    @Valid
    @NotNull(groups = AdValidationGroup.Page.class)
    @Builder.Default
    private PageQuery page = new PageQuery(1, 20);

    @NotNull(groups = AdValidationGroup.PublicRead.class)
    @Min(value = 1, groups = AdValidationGroup.PublicRead.class)
    @Max(value = 20, groups = AdValidationGroup.PublicRead.class)
    private Integer limit;

    @AssertTrue(groups = AdValidationGroup.Page.class, message = "advertisement pages are limited to 100 rows")
    public boolean isPageBounded() {
        return page == null || (page.pageNo() >= 1 && page.pageSize() >= 1 && page.pageSize() <= 100);
    }
}
