package top.egon.enjoyshop.goods.application.search;

import jakarta.validation.constraints.NotNull;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import io.swagger.v3.oas.annotations.media.Schema;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
@JsonInclude(JsonInclude.Include.ALWAYS)
public class ProductSearchItemResult {
    @Schema(type = "string", pattern = "[1-9][0-9]{0,18}") @NotNull private String skuId;
    @Schema(type = "string", pattern = "[1-9][0-9]{0,18}") @NotNull private String spuId;
    @NotNull private String name;
    @Schema(description = "HTML-encoded highlight containing only mark tags; null when unavailable", types = {"string", "null"}, requiredMode = Schema.RequiredMode.REQUIRED)
    private String nameHighlight;
    @Schema(description = "Price in integer cents", minimum = "1", format = "int32") @NotNull private Integer price;
    @NotNull private String image;
    @Schema(type = "string") @NotNull private String brandId;
    @NotNull private String brandName;
    @Schema(type = "string") @NotNull private String categoryId;
    @NotNull private String categoryName;
    @Builder.Default @NotNull private Map<String, String> specs = Map.of();
}
