package top.egon.enjoyshop.goods.adapter.goods;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import top.egon.enjoyshop.goods.start.StudentManagementApplication;
import top.egon.enjoyshop.goods.support.PersistenceTestSupport;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest(classes = StudentManagementApplication.class)
class ProductSearchOpenApiContractTest extends PersistenceTestSupport {
    @Autowired private MockMvc mvc;

    @Test void generatedDocumentDefinesOneSafeQueryWithoutChangingManagementSecurity() throws Exception {
        var document = new ObjectMapper().readTree(mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray());
        var path = document.at("/paths/~1api~1v1~1product-search");
        assertThat(path.size()).isEqualTo(1);
        var operation = path.path("get");
        assertThat(operation.path("operationId").asText()).isEqualTo("searchProductSkus");
        assertThat(operation.path("requestBody").isMissingNode()).isTrue();
        assertThat(operation.path("security").isArray()).isTrue();
        assertThat(operation.path("security")).isEmpty();
        for (String status : new String[]{"200", "400", "404", "500", "503"}) {
            assertThat(operation.path("responses").has(status)).isTrue();
        }
        assertThat(operation.at("/responses/400/content/application~1json/schema/$ref").asText())
                .isEqualTo("#/components/schemas/SearchRequestError");
        assertThat(document.at("/components/schemas/SearchRequestError").toString()).contains("$ref");
        assertThat(document.at("/components/schemas/FieldErrorsVO/properties/fieldErrors").isMissingNode()).isFalse();
        assertThat(operation.at("/responses/503/content/application~1json/schema/$ref").asText())
                .isEqualTo("#/components/schemas/SearchBusinessError");
        assertThat(document.at("/components/schemas/ProductSearchItemResult/required").toString()).contains("skuId", "price", "name");
        assertThat(operation.path("parameters").toString()).contains("keywords", "categoryId", "brandId", "minPrice", "maxPrice", "pageNo", "pageSize", "sort", "priceAsc");
        assertThat(operation.path("description").asText()).contains("spec.<name>", "503", "10000");
        assertThat(document.at("/components/schemas/ProductSearchItemResult/properties/skuId/type").asText()).isEqualTo("string");
        assertThat(document.at("/components/schemas/ProductSearchItemResult/properties/price/type").asText()).isEqualTo("integer");
        assertThat(document.at("/components/schemas/ProductSearchItemResult/properties/tenantId").isMissingNode()).isTrue();
        assertThat(document.at("/paths/~1api~1v1~1products/get/security").toString()).contains("bearerAuth");
        assertThat(document.at("/paths/~1api~1v1~1products/get/operationId").asText()).isEqualTo("listProducts");
        assertThat(document.at("/paths/~1api~1v1~1ad-slots~1{position}~1ads/get/security").isArray()).isTrue();
    }
}
