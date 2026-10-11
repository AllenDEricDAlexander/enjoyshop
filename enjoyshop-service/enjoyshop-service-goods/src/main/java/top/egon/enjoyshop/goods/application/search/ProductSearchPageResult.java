package top.egon.enjoyshop.goods.application.search;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;
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
public class ProductSearchPageResult {
    @Builder.Default @NotNull @Size(max = 100) private List<ProductSearchItemResult> rows = List.of();
    @NotNull @PositiveOrZero private Long total;
    @NotNull @PositiveOrZero private Long totalPages;
    @NotNull @Min(1) @Max(10000) private Integer pageNo;
    @NotNull @Min(1) @Max(100) private Integer pageSize;
    @NotNull @Min(1) @Max(10000) private Integer maxAccessiblePageNo;
    @Builder.Default @NotNull @Size(max = 22) private List<SearchFacetResult> facets = List.of();
    @io.swagger.v3.oas.annotations.media.Schema(requiredMode = io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED) private boolean facetsTruncated;
}
