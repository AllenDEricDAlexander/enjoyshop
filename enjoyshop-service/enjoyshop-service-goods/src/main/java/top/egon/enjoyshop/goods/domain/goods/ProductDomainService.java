package top.egon.enjoyshop.goods.domain.goods;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import top.egon.cola.component.common.core.pojo.PageResultRecord;
import top.egon.enjoyshop.goods.common.validation.ProductValidationGroup;

public interface ProductDomainService {

    ProductBO create(@Valid @NotNull ProductBO value);

    ProductBO update(@Valid @NotNull ProductBO value);

    ProductBO detail(@Valid @NotNull ProductDomainQuery query);

    PageResultRecord<SpuBO> page(@Valid @NotNull ProductDomainQuery query);

    ProductStateBO audit(@NotNull @Positive Long spuId, @NotNull @Min(0) Long expectedVersion);
    ProductStateBO pull(@NotNull @Positive Long spuId, @NotNull @Min(0) Long expectedVersion);
    ProductStateBO put(@NotNull @Positive Long spuId, @NotNull @Min(0) Long expectedVersion);
    boolean delete(@NotNull @Positive Long spuId, @NotNull @Min(0) Long expectedVersion);
    ProductStateBO restore(@NotNull @Positive Long spuId, @NotNull @Min(0) Long expectedVersion);
    boolean purge(@NotNull @Positive Long spuId, @NotNull @Min(0) Long expectedVersion);
}
