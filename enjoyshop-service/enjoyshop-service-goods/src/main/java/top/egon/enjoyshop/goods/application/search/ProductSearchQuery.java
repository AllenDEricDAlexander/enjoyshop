package top.egon.enjoyshop.goods.application.search;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
import top.egon.enjoyshop.goods.common.enums.ProductSearchSortEnum;
import top.egon.enjoyshop.goods.common.validation.ValidProductSearch;
import top.egon.enjoyshop.goods.common.validation.ProductSearchValidator.SearchInput;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
@ValidProductSearch
public class ProductSearchQuery implements SearchInput {
    @NotNull @Size(max = 100) @Pattern(regexp = "(?s)^(?!null$).*$") @Builder.Default
    private String keywords = "";
    @Positive private Long categoryId;
    @Positive private Long brandId;
    @Min(0) private Integer minPrice;
    @Min(0) private Integer maxPrice;
    @NotNull @Min(1) @Max(10000) @Builder.Default private Integer pageNo = 1;
    @NotNull @Min(1) @Max(100) @Builder.Default private Integer pageSize = 20;
    @NotNull @Builder.Default private ProductSearchSortEnum sort = ProductSearchSortEnum.RELEVANCE;
    @NotNull @Size(max = 20) @Builder.Default
    private Map<@NotBlank @Size(max = 120) String,
            @NotNull @Size(min = 1, max = 20) List<@NotBlank @Size(max = 120) @Pattern(regexp = "(?s)^(?!null$).*$") String>> specs = Map.of();
}
