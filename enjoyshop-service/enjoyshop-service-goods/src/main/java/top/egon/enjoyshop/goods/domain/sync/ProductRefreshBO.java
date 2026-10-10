package top.egon.enjoyshop.goods.domain.sync;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/** One tenant's bounded union of affected SPU roots to refresh from current state. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class ProductRefreshBO {

    @NotBlank
    @Pattern(regexp = "^[a-f0-9]{64}$")
    private String eventId;

    @NotNull
    @Positive
    private Long tenantId;

    @Valid
    @NotNull
    private CdcSourceCursorBO cursor;

    @NotEmpty
    @Size(max = 2)
    private List<@NotNull @Positive Long> spuIds;

    public ProductRefreshBO setSpuIds(List<Long> spuIds) {
        this.spuIds = spuIds == null ? null : List.copyOf(spuIds);
        return this;
    }

    @jakarta.validation.constraints.AssertTrue(message = "SPU roots must be sorted and unique")
    public boolean isSpuIdsCanonical() {
        return spuIds == null || spuIds.stream().distinct().count() == spuIds.size()
                && spuIds.equals(spuIds.stream().sorted().toList());
    }

    public static class ProductRefreshBOBuilder {
        public ProductRefreshBOBuilder spuIds(List<Long> spuIds) {
            this.spuIds = spuIds == null ? null : List.copyOf(spuIds);
            return this;
        }
    }
}
