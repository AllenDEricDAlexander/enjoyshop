package top.egon.enjoyshop.goods.domain.search;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Read-only search boundary; implementations must filter hits and facets by the mandatory tenant/visible specification. */
public interface ProductSearchRepository {
    ProductSearchPageBO search(@Valid @NotNull SearchCriteriaBO criteria);
}
