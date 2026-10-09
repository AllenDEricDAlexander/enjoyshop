package top.egon.enjoyshop.goods.application.goods;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import top.egon.enjoyshop.goods.common.json.ProductIdDeserializer;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor(onConstructor_ = @com.fasterxml.jackson.annotation.JsonCreator(mode = com.fasterxml.jackson.annotation.JsonCreator.Mode.DISABLED))
@Accessors(chain = true)
@Builder
@Schema(additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public class SpuSaveCommand {

    @NotBlank
    @Size(max = 64)
    private String sn;

    @NotBlank
    @Size(max = 200)
    private String name;

    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, defaultValue = "")
    @NotNull
    @Size(max = 300)
    @Builder.Default
    private String caption = "";

    @NotNull
    @Positive
    @Schema(type = "string", pattern = "[0-9]+")
    @JsonDeserialize(using = ProductIdDeserializer.class)
    private Long brandId;

    @NotNull
    @Positive
    @Schema(type = "string", pattern = "[0-9]+")
    @JsonDeserialize(using = ProductIdDeserializer.class)
    private Long category1Id;

    @NotNull
    @Positive
    @Schema(type = "string", pattern = "[0-9]+")
    @JsonDeserialize(using = ProductIdDeserializer.class)
    private Long category2Id;

    @NotNull
    @Positive
    @Schema(type = "string", pattern = "[0-9]+")
    @JsonDeserialize(using = ProductIdDeserializer.class)
    private Long category3Id;

    @NotNull
    @Positive
    @Schema(type = "string", pattern = "[0-9]+")
    @JsonDeserialize(using = ProductIdDeserializer.class)
    private Long templateId;

    @NotNull
    @PositiveOrZero
    @Schema(type = "string", pattern = "[0-9]+", requiredMode = Schema.RequiredMode.NOT_REQUIRED, defaultValue = "0")
    @Builder.Default
    @JsonDeserialize(using = ProductIdDeserializer.class)
    private Long freightId = 0L;

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

    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, defaultValue = "[]")
    @NotNull
    @Size(max = 20)
    @Builder.Default
    private List<@NotBlank @Size(max = 100) String> saleService = List.of();

    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, defaultValue = "")
    @NotNull
    @Size(max = 20000)
    @Builder.Default
    private String introduction = "";

    @NotNull
    @JsonDeserialize(using = ProductIdDeserializer.BooleanValueDeserializer.class)
    private Boolean isEnableSpec;

    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, defaultValue = "{}")
    @NotNull
    @Size(max = 20)
    @Builder.Default
    private Map<@NotBlank @Size(max = 120) String, @NotNull @Size(min = 1, max = 100) List<@NotBlank @Size(max = 120) String>> specItems = Map.of();

    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, defaultValue = "{}")
    @NotNull
    @Size(max = 100)
    @Builder.Default
    private Map<@NotBlank @Size(max = 120) String, @NotBlank @Size(max = 1000) String> paraItems = Map.of();

    @JsonAnySetter
    public void rejectUnknown(String name, Object value) {
        throw new IllegalArgumentException("Unknown product field: " + name);
    }
}
