package top.egon.enjoyshop.goods.adapter.goods;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
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
class ProductOpenApiContractTest extends PersistenceTestSupport {
    @Autowired private MockMvc mvc;
    @Test void realDocumentDefinesTenOperationsBearerAndMaterialResponseShapes() throws Exception {
        var document = new ObjectMapper().readTree(mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray());
        Map<String,String> operations = Map.of("get", "listProducts", "post", "createProduct",
                "get /{spuId}", "getProduct", "put /{spuId}", "updateProduct", "delete /{spuId}", "deleteProduct",
                "put /{spuId}/approval", "approveProduct", "put /{spuId}/withdrawal", "withdrawProduct",
                "put /{spuId}/publication", "publishProduct", "put /{spuId}/restoration", "restoreProduct", "delete /{spuId}/purge", "purgeProduct");
        for (var entry : operations.entrySet()) {
            String[] key = entry.getKey().split(" ", 2);
            var operation = document.path("paths").path("/api/v1/products" + (key.length == 2 ? key[1] : "")).path(key[0]);
            assertThat(operation.path("operationId").asText()).isEqualTo(entry.getValue());
            assertThat(operation.path("security").toString()).contains("bearerAuth");
            for (String code : new String[]{"400", "401", "403", "500", "503"}) { assertThat(operation.path("responses").has(code)).isTrue(); }
        }
        assertThat(document.at("/components/securitySchemes/bearerAuth/scheme").asText()).isEqualTo("bearer");
        assertThat(document.at("/components/schemas/SpuResult/properties/id/type").asText()).isEqualTo("string");
        assertThat(document.at("/components/schemas/SkuResult/properties/id/type").asText()).isEqualTo("string");
        assertThat(document.at("/paths/~1api~1v1~1products/post/responses/201/headers/Location").isMissingNode()).isFalse();
    }
}
