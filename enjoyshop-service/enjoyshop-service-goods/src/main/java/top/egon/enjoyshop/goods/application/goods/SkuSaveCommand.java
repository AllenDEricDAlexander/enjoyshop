package top.egon.enjoyshop.goods.application.goods;

import com.fasterxml.jackson.annotation.JsonIgnore;
import top.egon.enjoyshop.goods.common.validation.ProductCompositionValidator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import top.egon.enjoyshop.goods.common.json.ProductIdDeserializer;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import top.egon.enjoyshop.goods.common.validation.ProductValidationGroup;

@Data
@NoArgsConstructor
@AllArgsConstructor(onConstructor_ = @com.fasterxml.jackson.annotation.JsonCreator(mode = com.fasterxml.jackson.annotation.JsonCreator.Mode.DISABLED))
@Accessors(chain = true)
@Builder
@Schema(additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public class SkuSaveCommand implements ProductCompositionValidator.ProductSkuInput {

    @Null(groups = ProductValidationGroup.Create.class)
    @Positive(groups = ProductValidationGroup.Update.class)
    @Schema(type = "string", pattern = "[0-9]+")
    @JsonDeserialize(using = ProductIdDeserializer.class)
    private Long id;

    @NotBlank
    @Size(max = 64)
    private String sn;

    @NotNull
    @Min(0)
    @JsonDeserialize(using = ProductIdDeserializer.IntegerValueDeserializer.class)
    private Integer num;

    @NotNull
    @Min(0)
    @JsonDeserialize(using = ProductIdDeserializer.IntegerValueDeserializer.class)
    private Integer alertNum;

    @NotNull
    @Min(1)
    @JsonDeserialize(using = ProductIdDeserializer.IntegerValueDeserializer.class)
    private Integer price;

    @NotNull
    @Min(0)
    @JsonDeserialize(using = ProductIdDeserializer.IntegerValueDeserializer.class)
    private Integer weight;

    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, defaultValue = "{}")
    @NotNull
    @Size(max = 20)
    @Builder.Default
    private Map<@NotBlank @Size(max = 120) String, @NotBlank @Size(max = 120) String> spec = Map.of();

    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, defaultValue = "")
    @NotNull
    @Size(max = 500)
    @Pattern(regexp = "^(?:|https://\\S+)$")
    @Builder.Default
    private String image = "";

    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, defaultValue = "[]")
    @NotNull
    @Size(max = 10)
    @Builder.Default
    private List<@NotBlank @Size(max = 500) @Pattern(regexp = "^https://\\S+$") String> images = List.of();

    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, defaultValue = "1")
    @NotNull
    @Pattern(regexp = "^(?:1|2)$")
    @Builder.Default
    private String status = "1";

    @JsonAnySetter
    public void rejectUnknown(String name, Object value) {
        throw new IllegalArgumentException("Unknown product field: " + name);
    }
    @Override
    @JsonIgnore
    public Long compositionId() {
        return id;
    }

    @Override
    @JsonIgnore
    public String compositionBarcode() {
        return sn;
    }

    @Override
    @JsonIgnore
    public Map<String, String> compositionSpecification() {
        return spec;
    }
}
