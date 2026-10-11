package top.egon.enjoyshop.goods.domain.search;

import jakarta.validation.Valid;
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
public class ProductSearchPageBO {
    @NotNull @Size(max = 100) @Valid @Builder.Default private List<@NotNull ProductSearchItemBO> items = List.of();
    @NotNull @PositiveOrZero private Long total;
    @NotNull @Min(1) @Max(10000) private Integer pageNo;
    @NotNull @Min(1) @Max(100) private Integer pageSize;
    @NotNull @Size(max = 22) @Valid @Builder.Default private List<@NotNull SearchFacetBO> facets = List.of();
    private boolean facetsTruncated;

    public long getTotalPages() {
        return total == null || pageSize == null || pageSize <= 0 ? 0 : total / pageSize + (total % pageSize == 0 ? 0 : 1);
    }

    public int getMaxAccessiblePageNo() { return pageSize == null || pageSize <= 0 ? 0 : 10000 / pageSize; }
}
