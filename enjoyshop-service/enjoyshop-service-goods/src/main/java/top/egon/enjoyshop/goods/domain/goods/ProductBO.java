package top.egon.enjoyshop.goods.domain.goods;

import com.fasterxml.jackson.annotation.JsonIgnore;
import top.egon.enjoyshop.goods.common.validation.ProductCompositionValidator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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
@AllArgsConstructor
@Accessors(chain = true)
@Builder
@ValidProductComposition(groups = {ProductValidationGroup.Create.class, ProductValidationGroup.Update.class})
public class ProductBO implements ProductCompositionValidator.ProductCompositionInput {

    @NotNull
    @Valid
    private SpuBO spu;

    @NotNull
    @Size(min = 1, max = 100)
    private List<@NotNull @Valid SkuBO> skuList;

    @NotNull(groups = ProductValidationGroup.Update.class)
    @Min(0)
    private Long expectedVersion;
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
