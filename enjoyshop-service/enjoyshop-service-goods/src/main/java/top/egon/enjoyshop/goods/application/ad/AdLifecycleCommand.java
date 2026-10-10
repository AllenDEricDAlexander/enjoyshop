package top.egon.enjoyshop.goods.application.ad;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import top.egon.enjoyshop.goods.common.json.ProductIdDeserializer;

@Data
@NoArgsConstructor
@AllArgsConstructor(onConstructor_ = @com.fasterxml.jackson.annotation.JsonCreator(mode = com.fasterxml.jackson.annotation.JsonCreator.Mode.DISABLED))
@Accessors(chain = true)
@Builder
public class AdLifecycleCommand {

    @NotNull
    @Positive
    @JsonDeserialize(using = ProductIdDeserializer.LongIntegerValueDeserializer.class)
    private Long id;

    @NotNull
    @Min(0)
    @JsonDeserialize(using = ProductIdDeserializer.LongIntegerValueDeserializer.class)
    private Long version;

    @JsonAnySetter
    public void rejectUnknown(String name, Object value) {
        throw new IllegalArgumentException("Unknown advertisement lifecycle field: " + name);
    }
}
