package top.egon.enjoyshop.goods.application.ad;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import top.egon.enjoyshop.goods.common.json.ProductIdDeserializer;
import top.egon.enjoyshop.goods.common.validation.AdValidationGroup;
import top.egon.enjoyshop.goods.common.validation.AdWindowValidator;
import top.egon.enjoyshop.goods.common.validation.ValidAdPosition;
import top.egon.enjoyshop.goods.common.validation.ValidAdUrl;
import top.egon.enjoyshop.goods.common.validation.ValidAdWindow;
import top.egon.enjoyshop.goods.domain.ad.AdStatusEnum;

@Data
@NoArgsConstructor
@AllArgsConstructor(onConstructor_ = @com.fasterxml.jackson.annotation.JsonCreator(mode = com.fasterxml.jackson.annotation.JsonCreator.Mode.DISABLED))
@Accessors(chain = true)
@Builder
@ValidAdWindow(groups = {AdValidationGroup.Create.class, AdValidationGroup.Update.class})
public class AdSaveCommand implements AdWindowValidator.AdWindowInput {

    @Null(groups = AdValidationGroup.Create.class)
    @NotNull(groups = AdValidationGroup.Update.class)
    @Min(value = 0, groups = AdValidationGroup.Update.class)
    @JsonDeserialize(using = ProductIdDeserializer.LongIntegerValueDeserializer.class)
    private Long version;

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
    @Max(value = 1_000_000, groups = AdValidationGroup.Write.class)
    private Integer sortOrder;

    @JsonAnySetter
    public void rejectUnknown(String name, Object value) {
        throw new IllegalArgumentException("Unknown advertisement field: " + name);
    }

    @JsonSetter("name")
    public AdSaveCommand setName(String name) {
        this.name = name == null ? null : name.trim();
        return this;
    }

    @Override
    public Instant getStartAt() {
        return startAt;
    }

    @Override
    public Instant getEndAt() {
        return endAt;
    }
}
