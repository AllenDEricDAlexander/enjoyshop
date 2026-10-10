package top.egon.enjoyshop.goods.application.ad;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import top.egon.enjoyshop.goods.common.validation.AdValidationGroup;
import top.egon.enjoyshop.goods.common.validation.ValidAdPosition;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class AdReadQuery {

    @NotBlank(groups = AdValidationGroup.PublicRead.class)
    @ValidAdPosition(groups = AdValidationGroup.PublicRead.class)
    private String position;

    @NotNull(groups = AdValidationGroup.PublicRead.class)
    @Min(value = 1, groups = AdValidationGroup.PublicRead.class)
    @Max(value = 20, groups = AdValidationGroup.PublicRead.class)
    @Builder.Default
    private Integer limit = 20;

    @JsonAnySetter
    public void rejectUnknown(String name, Object value) {
        throw new IllegalArgumentException("Unknown advertisement query field: " + name);
    }
}
