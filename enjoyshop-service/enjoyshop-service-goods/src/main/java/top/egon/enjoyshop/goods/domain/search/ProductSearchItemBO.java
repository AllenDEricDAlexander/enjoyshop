package top.egon.enjoyshop.goods.domain.search;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
@JsonInclude(JsonInclude.Include.ALWAYS)
public class ProductSearchItemBO {
    @NotNull @Positive private Long skuId;
    @NotNull @Positive private Long spuId;
    @NotNull @Size(max = 3000) private String name;
    private String nameHighlight;
    @NotNull @Min(1) private Integer price;
    @NotNull @Size(max = 500) private String image;
    @NotNull @Positive private Long brandId;
    @NotNull @Size(max = 120) private String brandName;
    @NotNull @Positive private Long categoryId;
    @NotNull @Size(max = 120) private String categoryName;
    @NotNull @Builder.Default private Map<@NotBlank @Size(max = 120) String,
            @NotBlank @Size(max = 120) String> specs = Map.of();
}
