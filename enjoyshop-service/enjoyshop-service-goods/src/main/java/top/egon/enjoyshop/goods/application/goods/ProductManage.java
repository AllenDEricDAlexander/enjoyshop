package top.egon.enjoyshop.goods.application.goods;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.pojo.PageResultRecord;
import top.egon.enjoyshop.goods.common.validation.ProductValidationGroup;

@Validated
public interface ProductManage {
    @Validated(ProductValidationGroup.Create.class)
    ProductResult create(@Valid @NotNull ProductSaveCommand command);
    @Validated(ProductValidationGroup.Update.class)
    ProductResult update(@NotNull @Positive Long spuId, @Valid @NotNull ProductSaveCommand command);
    @Validated(ProductValidationGroup.Detail.class)
    ProductResult detail(@Valid @NotNull ProductIdQuery query);
    @Validated(ProductValidationGroup.Page.class)
    PageResultRecord<ProductSummaryResult> page(@Valid @NotNull ProductPageQuery query);
    @Validated(ProductValidationGroup.StateCall.class)
    ProductStateResult audit(@Valid @NotNull ProductLifecycleCommand command);
    @Validated(ProductValidationGroup.StateCall.class)
    ProductStateResult pull(@Valid @NotNull ProductLifecycleCommand command);
    @Validated(ProductValidationGroup.StateCall.class)
    ProductStateResult put(@Valid @NotNull ProductLifecycleCommand command);
    @Validated(ProductValidationGroup.StateCall.class)
    boolean delete(@Valid @NotNull ProductLifecycleCommand command);
    @Validated(ProductValidationGroup.StateCall.class)
    ProductStateResult restore(@Valid @NotNull ProductLifecycleCommand command);
    @Validated(ProductValidationGroup.StateCall.class)
    boolean purge(@Valid @NotNull ProductLifecycleCommand command);
}
