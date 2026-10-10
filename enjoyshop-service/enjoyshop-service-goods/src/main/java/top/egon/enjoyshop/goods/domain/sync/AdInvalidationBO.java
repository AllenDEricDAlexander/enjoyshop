package top.egon.enjoyshop.goods.domain.sync;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
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

/** One tenant's bounded union of old and new advertisement positions. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class AdInvalidationBO {

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
    private List<@NotBlank @Pattern(regexp = "^[a-z][a-z0-9_]{0,63}$") String> positions;

    public AdInvalidationBO setPositions(List<String> positions) {
        this.positions = positions == null ? null : List.copyOf(positions);
        return this;
    }

    @AssertTrue(message = "advertisement positions must be sorted and unique")
    public boolean isPositionsCanonical() {
        return positions == null || positions.stream().distinct().count() == positions.size()
                && positions.equals(positions.stream().sorted().toList());
    }

    public static class AdInvalidationBOBuilder {
        public AdInvalidationBOBuilder positions(List<String> positions) {
            this.positions = positions == null ? null : List.copyOf(positions);
            return this;
        }
    }
}
