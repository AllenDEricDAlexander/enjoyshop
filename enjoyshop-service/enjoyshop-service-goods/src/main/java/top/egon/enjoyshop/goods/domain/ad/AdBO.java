package top.egon.enjoyshop.goods.domain.ad;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import top.egon.enjoyshop.goods.common.validation.AdValidationGroup;
import top.egon.enjoyshop.goods.common.validation.AdWindowValidator;
import top.egon.enjoyshop.goods.common.validation.ValidAdPosition;
import top.egon.enjoyshop.goods.common.validation.ValidAdUrl;
import top.egon.enjoyshop.goods.common.validation.ValidAdWindow;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
@ValidAdWindow(groups = {AdValidationGroup.Create.class, AdValidationGroup.Update.class})
public class AdBO implements AdWindowValidator.AdWindowInput {

    @Positive(groups = {AdValidationGroup.Update.class, AdValidationGroup.Delete.class, AdValidationGroup.Detail.class})
    @Null(groups = AdValidationGroup.Create.class)
    @NotNull(groups = {AdValidationGroup.Update.class, AdValidationGroup.Delete.class})
    private Long id;

    @Min(value = 0, groups = {AdValidationGroup.Update.class, AdValidationGroup.Delete.class})
    @Null(groups = AdValidationGroup.Create.class)
    @NotNull(groups = {AdValidationGroup.Update.class, AdValidationGroup.Delete.class})
    private Long version;

    @Positive
    private Long tenantId;

    private String createUserId;

    private Instant createTime;

    private String updateUserId;

    private Instant updateTime;

    private LocalDateTime deletedAt;

    @NotBlank(groups = AdValidationGroup.Write.class)
    @Size(max = 120, groups = AdValidationGroup.Write.class)
    private String name;

    @NotBlank(groups = AdValidationGroup.Write.class)
    @Size(max = 64, groups = AdValidationGroup.Write.class)
    @ValidAdPosition(groups = AdValidationGroup.Write.class)
    private String position;

    @NotBlank(groups = AdValidationGroup.Write.class)
    @Size(max = 512, groups = AdValidationGroup.Write.class)
    @ValidAdUrl(image = true, groups = AdValidationGroup.Write.class)
    private String imageUrl;

    @NotBlank(groups = AdValidationGroup.Write.class)
    @Size(max = 1024, groups = AdValidationGroup.Write.class)
    @ValidAdUrl(groups = AdValidationGroup.Write.class)
    private String targetUrl;

    @NotNull(groups = AdValidationGroup.Write.class)
    private AdStatusEnum status;

    @NotNull(groups = AdValidationGroup.Write.class)
    private Instant startAt;

    @NotNull(groups = AdValidationGroup.Write.class)
    private Instant endAt;

    @NotNull(groups = AdValidationGroup.Write.class)
    @Min(value = 0, groups = AdValidationGroup.Write.class)
    @jakarta.validation.constraints.Max(value = 1_000_000, groups = AdValidationGroup.Write.class)
    private Integer sortOrder;

    @Override
    public Instant getStartAt() {
        return startAt;
    }

    @Override
    public Instant getEndAt() {
        return endAt;
    }
}
