package top.egon.enjoyshop.goods.domain.goods;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import top.egon.cola.component.common.core.pojo.PageSlice;

public interface BrandDomainService {

    BrandBO create(@Valid BrandBO value);

    BrandBO update(@Valid BrandBO value);

    void delete(@NotNull @Positive Long id, @NotNull @Min(0) Long expectedVersion);

    BrandBO detail(@NotNull @Positive Long id);

    PageSlice<BrandBO> query(@Valid BrandDomainQuery query);

    /**
     * Whether an active row already uses this name inside the current tenant.
     *
     * @param name      candidate name, never blank
     * @param excludeId row to ignore, nullable for the create path
     */
    boolean existsActiveByName(@NotBlank @Size(max = 120) String name, @Positive Long excludeId);

    /** Catalog ordered page: sequence descending with the identifier as the tie breaker. */
    List<BrandBO> listPage(@Valid @NotNull BrandDomainQuery query, @Min(0) Integer seqMin, @Min(0) Integer seqMax);

    /** Total rows behind {@link #listPage} so the page envelope can carry {@code total}. */
    long countPage(@Valid @NotNull BrandDomainQuery query, @Min(0) Integer seqMin, @Min(0) Integer seqMax);
}
