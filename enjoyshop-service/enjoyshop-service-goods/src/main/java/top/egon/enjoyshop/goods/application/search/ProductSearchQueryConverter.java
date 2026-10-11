package top.egon.enjoyshop.goods.application.search;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.AnnotateWith;
import org.springframework.stereotype.Component;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import org.slf4j.MDC;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.enjoyshop.goods.common.enums.GoodsErrorStatus;
import top.egon.enjoyshop.goods.domain.search.SearchCriteriaBO;

@AnnotateWith(value = Component.class, elements = @AnnotateWith.Element(strings = "productSearchQueryConverterImpl"))
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR,
        builder = @org.mapstruct.Builder(disableBuilder = true))
public interface ProductSearchQueryConverter extends BaseForwardConverter<ProductSearchQuery, SearchCriteriaBO> {
    /** Set only by the search site's scope filter, never by the legacy tenant-header filter. */
    String SEARCH_TENANT_MDC_KEY = "productSearchTenantId";

    @Override
    default SearchCriteriaBO toTarget(ProductSearchQuery source) {
        String scope = MDC.get(SEARCH_TENANT_MDC_KEY);
        if (scope == null || !scope.matches("[1-9][0-9]{0,18}") || !scope.equals(MDC.get("tenantId"))) {
            throw new CommonException(GoodsErrorStatus.SEARCH_SITE_NOT_CONFIGURED);
        }
        return toTarget(source, Long.parseLong(scope));
    }

    @Mapping(target = "sortClauses", ignore = true)
    @Mapping(target = "tenantId", expression = "java(tenantId)")
    @Mapping(target = "visible", constant = "true")
    @Mapping(target = "keywords", expression = "java(source.getKeywords().strip())")
    @Mapping(target = "specs", source = "specs", qualifiedByName = "canonicalSpecifications")
    SearchCriteriaBO toTarget(ProductSearchQuery source, @Context Long tenantId);

    @Named("canonicalSpecifications")
    default Map<String, List<String>> canonicalSpecifications(Map<String, List<String>> values) {
        TreeMap<String, List<String>> result = new TreeMap<>();
        values.forEach((key, choices) -> result.put(key, choices.stream().distinct().sorted().toList()));
        return Collections.unmodifiableMap(result);
    }
}
