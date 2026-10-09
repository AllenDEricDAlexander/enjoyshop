package top.egon.enjoyshop.goods.application.goods;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor(onConstructor_ = @com.fasterxml.jackson.annotation.JsonCreator(mode = com.fasterxml.jackson.annotation.JsonCreator.Mode.DISABLED))
@Accessors(chain = true)
@Builder
public class ProductIdQuery {

    @NotNull
    @Positive
    @Schema(type = "string", pattern = "[0-9]+")
    private Long spuId;

    @NotNull
    @Builder.Default
    private Boolean deleted = false;
}
