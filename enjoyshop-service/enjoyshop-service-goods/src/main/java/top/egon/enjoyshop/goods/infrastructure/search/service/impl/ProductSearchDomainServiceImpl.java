package top.egon.enjoyshop.goods.infrastructure.search.service.impl;

import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.enjoyshop.goods.common.enums.GoodsErrorStatus;
import top.egon.enjoyshop.goods.common.validation.ProductSearchValidationGroup;
import top.egon.enjoyshop.goods.domain.search.ProductSearchDomainService;
import top.egon.enjoyshop.goods.domain.search.ProductSearchRepository;
import top.egon.enjoyshop.goods.domain.search.SearchCriteriaBO;
import top.egon.enjoyshop.goods.domain.search.ProductSearchPageBO;
import top.egon.enjoyshop.goods.common.config.ProductSearchProperties;

/** Validates the read boundary without pretending an undeployed search engine exists. */
@Service("productSearchDomainService")
@Validated(ProductSearchValidationGroup.InternalSearch.class)
@Slf4j
@RequiredArgsConstructor
public class ProductSearchDomainServiceImpl implements ProductSearchDomainService {
    @Qualifier("elasticProductSearchRepository") private final ObjectProvider<ProductSearchRepository> repositories;
    @Qualifier("productSearchProperties") private final ProductSearchProperties properties;
    @Qualifier("egonColaValidationUtils") private final ValidationUtils validation;

    @Override
    public ProductSearchPageBO search(SearchCriteriaBO criteria) {
        validation.validate(criteria, ProductSearchValidationGroup.InternalSearch.class);
        if (!criteria.getTenantId().toString().equals(MDC.get("tenantId"))
                || !criteria.getTenantId().toString().equals(MDC.get("productSearchTenantId"))) {
            throw new CommonException(GoodsErrorStatus.SEARCH_SITE_NOT_CONFIGURED);
        }
        ProductSearchRepository repository = repositories.getIfAvailable();
        if (!properties.isEnabled() || repository == null) {
            log.debug("product search is unavailable enabled={} adapterPresent={}", properties.isEnabled(), repository != null);
            throw new CommonException(GoodsErrorStatus.SEARCH_UNAVAILABLE);
        }
        ProductSearchPageBO page = repository.search(criteria);
        if (page == null || !validation.isValid(page)
                || !Objects.equals(page.getPageNo(), criteria.getPageNo())
                || !Objects.equals(page.getPageSize(), criteria.getPageSize())
                || page.getItems().size() > criteria.getPageSize()
                || page.getItems().size() != Math.min((long) criteria.getPageSize(),
                        Math.max(0L, page.getTotal() - criteria.getOffset()))
                || (page.getTotal() == 0 && !page.getFacets().isEmpty())
                || (!page.isFacetsTruncated() && page.getFacets().stream().anyMatch(facet -> facet.isTruncated()))) {
            log.warn("product search adapter returned an invalid page");
            throw new CommonException(GoodsErrorStatus.SEARCH_UNAVAILABLE);
        }
        return page;
    }
}
