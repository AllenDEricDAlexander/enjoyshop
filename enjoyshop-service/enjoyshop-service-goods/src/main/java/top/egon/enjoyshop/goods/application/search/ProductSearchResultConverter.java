package top.egon.enjoyshop.goods.application.search;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.mapstruct.Mapper;
import org.mapstruct.AnnotateWith;
import org.springframework.stereotype.Component;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;
import top.egon.enjoyshop.goods.domain.search.ProductSearchItemBO;
import top.egon.enjoyshop.goods.domain.search.ProductSearchPageBO;
import top.egon.enjoyshop.goods.domain.search.SearchFacetBO;
import top.egon.enjoyshop.goods.domain.search.SearchFacetBucketBO;

@AnnotateWith(value = Component.class, elements = @AnnotateWith.Element(strings = "productSearchResultConverterImpl"))
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR,
        builder = @org.mapstruct.Builder(disableBuilder = true))
public interface ProductSearchResultConverter extends BaseForwardConverter<ProductSearchPageBO, ProductSearchPageResult> {
    Pattern MARK_TAG = Pattern.compile("</?mark>");

    @Override
    @Mapping(target = "rows", source = "items")
    ProductSearchPageResult toTarget(ProductSearchPageBO source);

    @Mapping(target = "nameHighlight", source = "nameHighlight", qualifiedByName = "safeHighlight")
    ProductSearchItemResult toItem(ProductSearchItemBO source);
    SearchFacetResult toFacet(SearchFacetBO source);
    SearchFacetBucketResult toBucket(SearchFacetBucketBO source);

    /** Raw names stay plain text; only balanced fixed mark tags may reach the separate HTML field. */
    @Named("safeHighlight")
    default String safeHighlight(String fragment) {
        if (fragment == null || fragment.isEmpty()) { return null; }
        String text = MARK_TAG.matcher(fragment).replaceAll("");
        if (text.contains("<") || text.contains(">")) { return null; }
        Matcher tags = MARK_TAG.matcher(fragment);
        int depth = 0;
        while (tags.find()) {
            depth += "<mark>".equals(tags.group()) ? 1 : -1;
            if (depth < 0 || depth > 1) { return null; }
        }
        return depth == 0 ? fragment : null;
    }
}
