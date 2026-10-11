package top.egon.enjoyshop.goods.domain.search;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
public class SearchFacetBO {
    @NotNull @Pattern(regexp = "^(brand|category|spec)$") private String kind;
    @NotBlank private String name;
    private boolean truncated;
    @NotNull @Size(max = 100) @Valid @Builder.Default private List<@NotNull SearchFacetBucketBO> buckets = List.of();
}
