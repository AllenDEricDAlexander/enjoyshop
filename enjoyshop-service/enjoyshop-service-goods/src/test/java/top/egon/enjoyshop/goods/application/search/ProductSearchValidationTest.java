package top.egon.enjoyshop.goods.application.search;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.slf4j.MDC;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.enjoyshop.goods.common.enums.ProductSearchSortEnum;
import top.egon.enjoyshop.goods.common.validation.ProductSearchValidationGroup;
import top.egon.enjoyshop.goods.domain.search.SearchCriteriaBO;
import top.egon.enjoyshop.goods.domain.search.ProductSearchItemBO;
import top.egon.enjoyshop.goods.domain.search.ProductSearchPageBO;
import top.egon.enjoyshop.goods.common.config.ProductSearchProperties;

class ProductSearchValidationTest {
    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private final ProductSearchQueryConverter queries = Mappers.getMapper(ProductSearchQueryConverter.class);
    private final ProductSearchResultConverter results = Mappers.getMapper(ProductSearchResultConverter.class);

    @AfterAll static void closeValidator() { FACTORY.close(); }
    @AfterEach void clearScope() { MDC.clear(); }

    @Test void defaultsAndInclusivePricesAreBoundedWithoutTruncatingInput() {
        var query = ProductSearchQuery.builder().minPrice(0).maxPrice(Integer.MAX_VALUE).build();
        assertThat(FACTORY.getValidator().validate(query, ProductSearchValidationGroup.PublicSearch.class)).isEmpty();
        assertThat(query.getPageNo()).isEqualTo(1);
        assertThat(query.getPageSize()).isEqualTo(20);
        assertThat(query.getSort()).isEqualTo(ProductSearchSortEnum.RELEVANCE);
        assertThat(query.getSpecs()).isEmpty();
        query.setKeywords("null");
        assertThat(FACTORY.getValidator().validate(query)).isNotEmpty();
        query.setKeywords("");
        query.setMinPrice(2).setMaxPrice(1);
        assertThat(FACTORY.getValidator().validate(query)).anyMatch(v -> v.getPropertyPath().toString().equals("minPrice"));
        query.setMinPrice(-1).setMaxPrice(null);
        assertThat(FACTORY.getValidator().validate(query)).isNotEmpty();
    }

    @Test void pageWindowUsesLongArithmeticAndRejectsInvalidBoundaries() {
        var query = ProductSearchQuery.builder().pageNo(100).pageSize(100).build();
        assertThat(FACTORY.getValidator().validate(query)).isEmpty();
        for (int page : List.of(101, 10001, Integer.MAX_VALUE, 0, -1)) {
            query.setPageNo(page);
            assertThat(FACTORY.getValidator().validate(query)).isNotEmpty();
        }
        for (int size : List.of(0, 101, Integer.MAX_VALUE, -1)) {
            query.setPageNo(1).setPageSize(size);
            assertThat(FACTORY.getValidator().validate(query)).isNotEmpty();
        }
    }

    @Test void specificationBoundsAndLeafConstraintsApplyToBothCarriers() {
        var query = ProductSearchQuery.builder().specs(Map.of("", List.of("red"))).build();
        assertThat(FACTORY.getValidator().validate(query)).isNotEmpty();
        for (var specs : List.of(Map.of("color", List.<String>of()), Map.of("color", List.of(" ")),
                Map.of("color", List.of("x".repeat(121))), Map.of("x".repeat(121), List.of("red")))) {
            query.setSpecs(specs);
            assertThat(FACTORY.getValidator().validate(query)).isNotEmpty();
        }
        var oversized = new HashMap<String, List<String>>();
        for (int i = 0; i < 21; i++) { oversized.put("spec" + i, List.of("v")); }
        query.setSpecs(oversized);
        assertThat(FACTORY.getValidator().validate(query)).isNotEmpty();
        var internal = SearchCriteriaBO.builder().tenantId(41L).visible(false).build();
        assertThat(FACTORY.getValidator().validate(internal, ProductSearchValidationGroup.InternalSearch.class)).isNotEmpty();
        internal.setVisible(true).setTenantId(null);
        assertThat(FACTORY.getValidator().validate(internal, ProductSearchValidationGroup.InternalSearch.class)).isNotEmpty();
    }

    @Test void converterRequiresDedicatedScopeAndCopiesSpecificationsImmutably() {
        var values = new ArrayList<>(List.of("red", "blue", "red", "8+128G"));
        var input = new HashMap<String, List<String>>(); input.put("color", values);
        var query = ProductSearchQuery.builder().keywords("  手机  ").specs(input).build();
        MDC.put("tenantId", "999");
        assertThatThrownBy(() -> queries.toTarget(query)).isInstanceOf(CommonException.class);
        MDC.put(ProductSearchQueryConverter.SEARCH_TENANT_MDC_KEY, "41");
        assertThatThrownBy(() -> queries.toTarget(query)).isInstanceOf(CommonException.class);
        MDC.put("tenantId", "41");
        var criteria = queries.toTarget(query);
        assertThat(criteria.getTenantId()).isEqualTo(41);
        assertThat(criteria.getVisible()).isTrue();
        assertThat(criteria.getKeywords()).isEqualTo("手机");
        assertThat(criteria.getSpecs().get("color")).containsExactly("8+128G", "blue", "red");
        input.clear(); values.clear();
        assertThat(criteria.getSpecs()).containsKey("color");
        assertThatThrownBy(() -> criteria.getSpecs().put("x", List.of("y"))).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> criteria.getSpecs().get("color").add("x")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test void orderingStrategiesHaveExplicitFieldsAndNumericSkuTieBreakers() {
        for (var entry : Map.of(ProductSearchSortEnum.PRICE_ASC, "price", ProductSearchSortEnum.PRICE_DESC, "price",
                ProductSearchSortEnum.SALES_DESC, "saleNum", ProductSearchSortEnum.CREATED_DESC, "skuCreatedAt").entrySet()) {
            var clauses = entry.getKey().clauses(true);
            assertThat(clauses.getFirst().field()).isEqualTo(entry.getValue());
            assertThat(clauses.getLast()).isEqualTo(new ProductSearchSortEnum.SortClauseBO("skuId", true));
        }
        assertThat(ProductSearchSortEnum.PRICE_ASC.clauses(true).getFirst().ascending()).isTrue();
        assertThat(ProductSearchSortEnum.PRICE_DESC.clauses(true).getFirst().ascending()).isFalse();
        assertThat(ProductSearchSortEnum.RELEVANCE.clauses(true).getFirst().field()).isEqualTo("_score");
        assertThat(ProductSearchSortEnum.RELEVANCE.clauses(false)).containsExactly(new ProductSearchSortEnum.SortClauseBO("skuId", false));
        assertThatThrownBy(() -> ProductSearchSortEnum.fromCode("price")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ProductSearchSortEnum.fromCode("PRICEASC")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void projectionPreservesLargeIdsAndRawNamesButRejectsUnsafeHighlightMarkup() throws Exception {
        var item = ProductSearchItemBO.builder().skuId(9007199254740993L).spuId(Long.MAX_VALUE).name("<script>x</script>")
                .price(1).image("").brandId(10L).brandName("brand").categoryId(20L).categoryName("category").build();
        for (String unsafe : List.of("<img src=x onerror=alert(1)>", "<mark onclick=x>手机</mark>", "<mark>x", "</mark>x", "<mark><mark>x</mark></mark>")) {
            item.setNameHighlight(unsafe);
            assertThat(results.toItem(item).getNameHighlight()).isNull();
        }
        item.setNameHighlight("&lt;script&gt;<mark>手机</mark>");
        var view = results.toItem(item);
        assertThat(view.getName()).isEqualTo("<script>x</script>");
        assertThat(view.getNameHighlight()).isEqualTo("&lt;script&gt;<mark>手机</mark>");
        assertThat(view.getSkuId()).isEqualTo("9007199254740993");
        assertThat(view.getSpuId()).isEqualTo(Long.toString(Long.MAX_VALUE));
        assertThat(view.getPrice()).isEqualTo(1);
        item.setNameHighlight(null);
        var json = new ObjectMapper().readTree(new ObjectMapper().writeValueAsBytes(results.toItem(item)));
        assertThat(json.has("nameHighlight")).isTrue();
        assertThat(json.get("nameHighlight").isNull()).isTrue();
        assertThat(json.has("tenantId")).isFalse();
        assertThat(json.path("specs").size()).isZero();
        assertThat(new ObjectMapper().writeValueAsString(ProductSearchSortEnum.PRICE_ASC)).isEqualTo("\"priceAsc\"");
    }

    @Test void pageMetadataDoesNotOverflowAndEmptyCollectionsStayPresent() {
        var page = ProductSearchPageBO.builder().total(Long.MAX_VALUE).pageNo(1).pageSize(20).build();
        assertThat(page.getTotalPages()).isEqualTo(Long.MAX_VALUE / 20 + 1);
        assertThat(page.getMaxAccessiblePageNo()).isEqualTo(500);
        var result = results.toTarget(page);
        assertThat(result.getRows()).isEmpty(); assertThat(result.getFacets()).isEmpty();
        page.setTotal(0L);
        assertThat(results.toTarget(page).getTotalPages()).isZero();
    }

    @Test void propertiesFailClosedAndDoNotProvisionUnknownEngineKeys() {
        var properties = ProductSearchProperties.builder().build();
        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getPublicSites()).isEmpty();
        assertThat(FACTORY.getValidator().validate(properties)).isEmpty();
        properties.setEnabled(true);
        assertThat(FACTORY.getValidator().validate(properties)).isNotEmpty();
        properties.setPublicSites(Map.of("store.example.test", 41L));
        assertThat(FACTORY.getValidator().validate(properties)).isEmpty();
        properties.setQueryTimeout(Duration.ofSeconds(3));
        assertThat(FACTORY.getValidator().validate(properties)).isNotEmpty();
    }
}
