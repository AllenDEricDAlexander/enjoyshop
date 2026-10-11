package top.egon.enjoyshop.goods.adapter.goods;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import top.egon.enjoyshop.goods.common.config.ProductSearchProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import top.egon.enjoyshop.goods.start.StudentManagementApplication;
import top.egon.enjoyshop.goods.support.PersistenceTestSupport;

/** Offline HTTP contract: there is deliberately no production search repository in this phase. */
@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest(classes = StudentManagementApplication.class)
@TestPropertySource(properties = {"app.search.enabled=true", "app.search.public-sites.store.example.test=41"})
class ProductSearchControllerWebTest extends PersistenceTestSupport {
    @Autowired private MockMvc mvc;
    @Autowired @Qualifier("productSearchProperties") private ProductSearchProperties properties;

    @Test
    void malformedScalarAndSpecificationInputsAreRejectedBeforeTheAbsentEngine() throws Exception {
        for (var entry : Map.of("tenantId", "999", "sort", "price", "brandId", "01", "categoryId", "9223372036854775808",
                "price", "10-20", "minPrice", "1.5", "pageSize", "101", "keywords", "x".repeat(101)).entrySet()) {
            mvc.perform(get("/api/v1/product-search").param(entry.getKey(), entry.getValue())
                            .with(request -> { request.setServerName("store.example.test"); return request; }))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("code").value(610401));
        }
        mvc.perform(get("/api/v1/product-search").param("pageNo", "1", "2")
                        .with(request -> { request.setServerName("store.example.test"); return request; }))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("code").value(610401));
        for (var entry : Map.of("spec.", "red", "spec.color", " ").entrySet()) {
            mvc.perform(get("/api/v1/product-search").param(entry.getKey(), entry.getValue())
                            .with(request -> { request.setServerName("store.example.test"); return request; }))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("code").value(610401));
        }
        mvc.perform(get("/api/v1/product-search").param("minPrice", "200").param("maxPrice", "100")
                        .with(request -> { request.setServerName("store.example.test"); return request; }))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("data.fieldErrors[0].field").value("minPrice"));
        mvc.perform(get("/api/v1/product-search").param("pageNo", "101").param("pageSize", "100")
                        .with(request -> { request.setServerName("store.example.test"); return request; }))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("data.fieldErrors[0].field").value("pageNo"));
    }

    @Test
    void hostAndForwardedHeaderCannotChooseAnUnregisteredTenant() throws Exception {
        mvc.perform(get("/api/v1/product-search").header("X-Tenant-Id", "41")
                        .header("X-Forwarded-Host", "store.example.test")
                        .with(request -> { request.setServerName("unknown.example.test"); return request; }))
                .andExpect(status().isNotFound()).andExpect(jsonPath("code").value(610402));
        mvc.perform(post("/api/v1/product-search").with(request -> { request.setServerName("store.example.test"); return request; }))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/product-search/private").with(request -> { request.setServerName("store.example.test"); return request; }))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/products"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void disabledSearchAndEmptyOptionalScalarsFailClosed() throws Exception {
        properties.setEnabled(false);
        mvc.perform(get("/api/v1/product-search"))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("code").value(610403));
        properties.setEnabled(true);
        mvc.perform(get("/api/v1/product-search").param("pageNo", "")
                        .with(request -> { request.setServerName("store.example.test"); return request; }))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("code").value(610401));
    }

    @Test
    void servletDecodesLiteralPlusExactlyOnce() throws Exception {
        mvc.perform(get(URI.create("/api/v1/product-search?spec.memory=8%2B128G"))
                        .with(request -> { request.setServerName("store.example.test"); return request; }))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("code").value(610403));
    }

    @Test
    void missingEngineReturnsUnavailableInsteadOfAnEmptySuccessfulSearch() throws Exception {
        mvc.perform(get("/api/v1/product-search").param("keywords", "手机")
                        .with(request -> { request.setServerName("store.example.test"); return request; }))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("code").value(610403))
                .andExpect(jsonPath("success").value(false));
    }
}
