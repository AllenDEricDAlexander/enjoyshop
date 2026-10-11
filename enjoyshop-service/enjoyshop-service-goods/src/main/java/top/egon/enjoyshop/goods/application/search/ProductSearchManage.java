package top.egon.enjoyshop.goods.application.search;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface ProductSearchManage {
    ProductSearchPageResult search(@Valid @NotNull ProductSearchQuery query);
}
