package top.egon.enjoyshop.goods.application.goods;

import com.fasterxml.jackson.annotation.JsonIgnore;
import top.egon.enjoyshop.goods.common.validation.ProductCompositionValidator;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import top.egon.enjoyshop.goods.common.validation.ProductValidationGroup;
import top.egon.enjoyshop.goods.common.validation.ValidProductComposition;

@Data
@NoArgsConstructor
@AllArgsConstructor(onConstructor_ = @com.fasterxml.jackson.annotation.JsonCreator(mode = com.fasterxml.jackson.annotation.JsonCreator.Mode.DISABLED))
@Accessors(chain = true)
@Builder
@ValidProductComposition(groups = {ProductValidationGroup.Create.class, ProductValidationGroup.Update.class})
public class ProductSaveCommand implements ProductCompositionValidator.ProductCompositionInput {

    @NotNull
    @Valid
    private SpuSaveCommand spu;

    @NotNull
    @Size(min = 1, max = 100)
    private List<@NotNull @Valid SkuSaveCommand> skuList;

    @Null(groups = ProductValidationGroup.Create.class)
    @NotNull(groups = ProductValidationGroup.Update.class)
    @Min(0)
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = top.egon.enjoyshop.goods.common.json.ProductIdDeserializer.LongIntegerValueDeserializer.class)
    @io.swagger.v3.oas.annotations.media.Schema(requiredMode = io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED, description = "仅更新时必须提交当前整数版本；创建时不得提交非null版本")
    private Long version;

    @JsonAnySetter
    public void rejectUnknown(String name, Object value) {
        throw new IllegalArgumentException("Unknown product field: " + name);
    }
    @Override
    @JsonIgnore
    public Boolean specificationsEnabled() {
        return spu == null ? null : spu.getIsEnableSpec();
    }

    @Override
    @JsonIgnore
    public Map<String, List<String>> advertisedSpecifications() {
        return spu == null ? null : spu.getSpecItems();
    }

    @Override
    @JsonIgnore
    public List<? extends ProductCompositionValidator.ProductSkuInput> compositionSkus() {
        return skuList;
    }
}
