package top.egon.enjoyshop.goods.application.goods;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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
public class DeleteBrandCommand {

    @NotNull
    @Positive
    private Long id;

    @NotNull
    @Min(0)
    private Long expectedVersion;
}
