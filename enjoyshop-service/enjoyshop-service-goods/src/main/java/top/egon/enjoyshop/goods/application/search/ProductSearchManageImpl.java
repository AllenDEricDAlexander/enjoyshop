package top.egon.enjoyshop.goods.application.search;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.enjoyshop.goods.common.validation.ProductSearchValidationGroup;
import top.egon.enjoyshop.goods.domain.search.ProductSearchDomainService;

@Service("productSearchManage")
@Validated(ProductSearchValidationGroup.PublicSearch.class)
@Slf4j
@RequiredArgsConstructor
public class ProductSearchManageImpl implements ProductSearchManage {
    @Qualifier("productSearchDomainService") private final ProductSearchDomainService domain;
    @Qualifier("productSearchQueryConverterImpl") private final ProductSearchQueryConverter queries;
    @Qualifier("productSearchResultConverterImpl") private final ProductSearchResultConverter results;
    @Qualifier("egonColaValidationUtils") private final ValidationUtils validation;

    @Override
    public ProductSearchPageResult search(ProductSearchQuery query) {
        validation.validate(query, ProductSearchValidationGroup.PublicSearch.class);
        var criteria = queries.toTarget(query);
        validation.validate(criteria, ProductSearchValidationGroup.InternalSearch.class);
        var result = results.toTarget(domain.search(criteria));
        log.debug("completed product search page={} resultCount={}", result.getPageNo(), result.getRows().size());
        return result;
    }
}
