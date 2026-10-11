package top.egon.enjoyshop.goods.infrastructure.search;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.enjoyshop.goods.adapter.filter.ProductSearchTenantFilter;
import top.egon.enjoyshop.goods.adapter.goods.ProductSearchController;
import top.egon.enjoyshop.goods.adapter.handler.BaseExceptionHandler;
import top.egon.enjoyshop.goods.application.search.ProductSearchManage;
import top.egon.enjoyshop.goods.application.search.ProductSearchManageImpl;
import top.egon.enjoyshop.goods.application.search.ProductSearchQuery;
import top.egon.enjoyshop.goods.application.search.ProductSearchQueryConverter;
import top.egon.enjoyshop.goods.application.search.ProductSearchResultConverter;
import top.egon.enjoyshop.goods.domain.search.ProductSearchDomainService;
import top.egon.enjoyshop.goods.domain.search.ProductSearchRepository;
import top.egon.enjoyshop.goods.domain.search.SearchCriteriaBO;
import top.egon.enjoyshop.goods.domain.search.ProductSearchItemBO;
import top.egon.enjoyshop.goods.domain.search.ProductSearchPageBO;
import top.egon.enjoyshop.goods.domain.search.SearchFacetBO;
import top.egon.enjoyshop.goods.domain.search.SearchFacetBucketBO;
import top.egon.enjoyshop.goods.common.config.ProductSearchProperties;
import top.egon.enjoyshop.goods.infrastructure.search.service.impl.ProductSearchDomainServiceImpl;

/** Test-only engine port validates orchestration; no ES/PG/MQ/runtime process is started. */
class ProductSearchBoundaryTest {
    private AnnotationConfigApplicationContext context;
    private TestSearchRepository repository;
    private MockMvc mvc;

    @BeforeEach void boundary() {
        context = new AnnotationConfigApplicationContext(BoundaryConfiguration.class);
        repository = (TestSearchRepository) context.getBean("elasticProductSearchRepository");
        mvc = MockMvcBuilders.standaloneSetup(context.getBean(ProductSearchController.class))
                .setControllerAdvice(new BaseExceptionHandler()).addFilters(context.getBean(ProductSearchTenantFilter.class)).build();
    }
    @AfterEach void cleanup() { context.close(); MDC.clear(); }

    @Test void trustedHostOverridesHeadersAndFullResultKeepsOnlyPublicFields() throws Exception {
        repository.response = page();
        MDC.put("tenantId", "88"); MDC.put(ProductSearchQueryConverter.SEARCH_TENANT_MDC_KEY, "77");
        mvc.perform(get("/api/v1/product-search").header("X-Tenant-Id", "999").header("X-Forwarded-Host", "evil.test")
                        .param("keywords", " 手机 ").param("spec.color", "red", "blue", "red").param("sort", "priceAsc")
                        .with(request -> { request.setServerName("STORE.EXAMPLE.TEST."); return request; }))
                .andExpect(status().isOk()).andExpect(jsonPath("data.rows[0].skuId").value("9007199254740993"))
                .andExpect(jsonPath("data.rows[0].price").value(199900))
                .andExpect(jsonPath("data.rows[0].nameHighlight").value("<mark>手机</mark>"))
                .andExpect(jsonPath("data.rows[0].tenantId").doesNotExist())
                .andExpect(jsonPath("data.totalPages").value(1)).andExpect(jsonPath("data.maxAccessiblePageNo").value(500))
                .andExpect(jsonPath("data.facets[0].truncated").value(true)).andExpect(jsonPath("data.facetsTruncated").value(true))
                .andExpect(header().string("Cache-Control", "no-store"));
        assertThat(repository.seen.getTenantId()).isEqualTo(41L);
        assertThat(repository.seen.getVisible()).isTrue();
        assertThat(repository.seen.getKeywords()).isEqualTo("手机");
        assertThat(repository.seen.getSpecs().get("color")).containsExactly("blue", "red");
        assertThat(MDC.get("tenantId")).isEqualTo("88");
        assertThat(MDC.get(ProductSearchQueryConverter.SEARCH_TENANT_MDC_KEY)).isEqualTo("77");
    }

    @Test void literalPlusAndPercentSequencesAreNotDecodedTwice() throws Exception {
        repository.response = page();
        mvc.perform(get("/api/v1/product-search").param("spec.memory", "8+128G", "%2B")
                        .with(request -> { request.setServerName("store.example.test"); return request; }))
                .andExpect(status().isOk());
        assertThat(repository.seen.getSpecs().get("memory")).containsExactly("%2B", "8+128G");
    }

    @Test void emptyPageIsDifferentFromUnavailableAndRetainsCountMetadata() throws Exception {
        repository.response = ProductSearchPageBO.builder().total(0L).pageNo(1).pageSize(20).build();
        mvc.perform(get("/api/v1/product-search").with(request -> { request.setServerName("store.example.test"); return request; }))
                .andExpect(status().isOk()).andExpect(jsonPath("data.rows").isEmpty()).andExpect(jsonPath("data.facets").isEmpty())
                .andExpect(jsonPath("data.total").value(0)).andExpect(jsonPath("data.totalPages").value(0));
    }

    @Test void invalidPortOutputAndDependencyFailuresAreNotSuccessfulSearches() throws Exception {
        repository.response = page().setPageNo(2);
        mvc.perform(get("/api/v1/product-search").with(request -> { request.setServerName("store.example.test"); return request; }))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("code").value(610403));
        repository.response = page(); repository.response.getItems().getFirst().setPrice(null);
        mvc.perform(get("/api/v1/product-search").with(request -> { request.setServerName("store.example.test"); return request; }))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("code").value(610403));
        repository.response = page().setItems(List.of());
        mvc.perform(get("/api/v1/product-search").with(request -> { request.setServerName("store.example.test"); return request; }))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("code").value(610403));
        repository.response = null;
        mvc.perform(get("/api/v1/product-search").with(request -> { request.setServerName("store.example.test"); return request; }))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("success").value(false));
        assertThat(MDC.get("tenantId")).isNull();
    }

    @Test void directSpringBoundariesValidateCriteriaAndTrustedTenant() {
        var domain = context.getBean(ProductSearchDomainService.class);
        var manage = context.getBean(ProductSearchManage.class);
        assertThatThrownBy(() -> manage.search(ProductSearchQuery.builder().pageSize(101).build()))
                .isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> domain.search(SearchCriteriaBO.builder().tenantId(41L).visible(false).build()))
                .isInstanceOf(ConstraintViolationException.class);
        MDC.put("tenantId", "42"); MDC.put(ProductSearchQueryConverter.SEARCH_TENANT_MDC_KEY, "42");
        assertThatThrownBy(() -> domain.search(SearchCriteriaBO.builder().tenantId(41L).build()))
                .isInstanceOf(CommonException.class).extracting(e -> ((CommonException) e).getCode()).isEqualTo(610402);
        assertThat(repository.seen).isNull();
    }

    private static ProductSearchPageBO page() {
        return ProductSearchPageBO.builder().total(1L).pageNo(1).pageSize(20)
                .items(List.of(ProductSearchItemBO.builder().skuId(9007199254740993L).spuId(8L).name("手机")
                        .nameHighlight("<mark>手机</mark>").price(199900).image("").brandId(10L).brandName("brand")
                        .categoryId(20L).categoryName("category").specs(Map.of("color", "red")).build()))
                .facets(List.of(SearchFacetBO.builder().kind("brand").name("品牌").truncated(true)
                        .buckets(List.of(SearchFacetBucketBO.builder().key("10").label("brand").count(1L).build())).build()))
                .facetsTruncated(true).build();
    }

    static class TestSearchRepository implements ProductSearchRepository {
        SearchCriteriaBO seen;
        ProductSearchPageBO response;
        @Override public ProductSearchPageBO search(SearchCriteriaBO criteria) { seen = criteria; return response; }
    }

    @Configuration(value = "productSearchBoundaryTestConfiguration", proxyBeanMethods = false)
    @Import({ProductSearchManageImpl.class, ProductSearchDomainServiceImpl.class, ProductSearchController.class, ProductSearchTenantFilter.class})
    static class BoundaryConfiguration {
        @Bean("searchTestValidator") LocalValidatorFactoryBean validator() { return new LocalValidatorFactoryBean(); }
        @Bean("searchTestMethodValidation") static MethodValidationPostProcessor methodValidation() { return new MethodValidationPostProcessor(); }
        @Bean("egonColaValidationUtils") ValidationUtils validation(@Qualifier("searchTestValidator") jakarta.validation.Validator validator) {
            return new ValidationUtils(validator);
        }
        @Bean("productSearchProperties") ProductSearchProperties properties() {
            return ProductSearchProperties.builder().enabled(true).publicSites(Map.of("store.example.test", 41L)).build();
        }
        @Bean("jacksonObjectMapper") ObjectMapper mapper() { return new ObjectMapper(); }
        @Bean("elasticProductSearchRepository") ProductSearchRepository repository() { return new TestSearchRepository(); }
        @Bean("productSearchQueryConverterImpl") ProductSearchQueryConverter queries() { return Mappers.getMapper(ProductSearchQueryConverter.class); }
        @Bean("productSearchResultConverterImpl") ProductSearchResultConverter results() { return Mappers.getMapper(ProductSearchResultConverter.class); }
    }
}
