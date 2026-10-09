package top.egon.enjoyshop.goods.application.goods;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import top.egon.enjoyshop.goods.common.validation.ProductValidationGroup;

@Data
@NoArgsConstructor
@AllArgsConstructor(onConstructor_ = @com.fasterxml.jackson.annotation.JsonCreator(mode = com.fasterxml.jackson.annotation.JsonCreator.Mode.DISABLED))
@Accessors(chain = true)
@Builder
public class ProductLifecycleCommand {

    @Null(groups = ProductValidationGroup.HttpStateInput.class)
    @NotNull(groups = ProductValidationGroup.StateCall.class)
    @Positive
    @io.swagger.v3.oas.annotations.media.Schema(hidden = true)
    private Long spuId;

    @NotNull
    @Min(0)
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = top.egon.enjoyshop.goods.common.json.ProductIdDeserializer.LongIntegerValueDeserializer.class)
    private Long version;

    @JsonAnySetter
    public void rejectUnknown(String name, Object value) {
        throw new IllegalArgumentException("Unknown product field: " + name);
    }
}
