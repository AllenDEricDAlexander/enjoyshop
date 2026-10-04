package top.egon.enjoyshop.goods.domain.goods;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;
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
public class SpecBO {

    @NotNull
    private Long id;

    @NotBlank
    @Size(max = 120)
    private String name;

    @NotNull
    private List<String> options;

    @NotNull
    @PositiveOrZero
    private Integer seq;
}
