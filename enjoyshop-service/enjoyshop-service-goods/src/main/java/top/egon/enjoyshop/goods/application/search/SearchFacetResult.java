package top.egon.enjoyshop.goods.application.search;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
public class SearchFacetResult {
    @NotNull @Pattern(regexp = "^(brand|category|spec)$") private String kind;
    @NotNull private String name;
    @io.swagger.v3.oas.annotations.media.Schema(requiredMode = io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED) private boolean truncated;
    @Builder.Default @NotNull @Size(max = 100) private List<SearchFacetBucketResult> buckets = List.of();
}
