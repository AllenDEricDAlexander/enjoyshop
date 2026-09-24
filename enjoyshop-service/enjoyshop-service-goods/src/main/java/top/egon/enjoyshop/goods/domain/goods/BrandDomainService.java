package top.egon.enjoyshop.goods.domain.goods;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.pojo.PageSlice;

@Validated
public interface BrandDomainService {

    BrandBO create(@Valid BrandBO value);

    BrandBO update(@Valid BrandBO value);

    void delete(@NotNull @Positive Long id, @NotNull @Min(0) Long expectedVersion);

    BrandBO detail(@NotNull @Positive Long id);

    PageSlice<BrandBO> query(@Valid BrandDomainQuery query);
}
