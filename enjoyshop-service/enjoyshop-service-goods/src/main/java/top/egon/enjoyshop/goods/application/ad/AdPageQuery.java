package top.egon.enjoyshop.goods.application.ad;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import top.egon.enjoyshop.goods.common.validation.AdValidationGroup;
import top.egon.enjoyshop.goods.common.validation.ValidAdPosition;
import top.egon.enjoyshop.goods.domain.ad.AdStatusEnum;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class AdPageQuery {

    @ValidAdPosition(groups = AdValidationGroup.Page.class)
    private String position;

    private AdStatusEnum status;

    @NotNull(groups = AdValidationGroup.Page.class)
    @Min(value = 1, groups = AdValidationGroup.Page.class)
    @Max(value = 1_000_000, groups = AdValidationGroup.Page.class)
    @Builder.Default
    private Integer pageNo = 1;

    @NotNull(groups = AdValidationGroup.Page.class)
    @Min(value = 1, groups = AdValidationGroup.Page.class)
    @Max(value = 100, groups = AdValidationGroup.Page.class)
    @Builder.Default
    private Integer pageSize = 20;

    @JsonAnySetter
    public void rejectUnknown(String name, Object value) {
        throw new IllegalArgumentException("Unknown advertisement page field: " + name);
    }
}
