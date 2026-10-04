package top.egon.enjoyshop.goods.adapter.goods;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import top.egon.enjoyshop.goods.application.goods.FileManage;
import top.egon.enjoyshop.goods.start.StudentManagementApplication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest(classes = StudentManagementApplication.class)
class FileOpenApiContractTest extends top.egon.enjoyshop.goods.support.PersistenceTestSupport {

    private static final String PATH = "/api/v1/files";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FileManage fileManage;

    @Test
    void upload_openapi_describes_multipart_security_and_error_contract() throws Exception {
        byte[] body = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();
        JsonNode document = objectMapper.readTree(new String(body, StandardCharsets.UTF_8));
        JsonNode operation = document.path("paths").path(PATH).path("post");

        assertThat(operation.isMissingNode()).as("API-008 must be documented").isFalse();
        assertThat(operation.path("operationId").asText()).isEqualTo("uploadFile");
        assertThat(operation.at("/requestBody/required").asBoolean()).isTrue();
        JsonNode bodySchema = operation.at("/requestBody/content/multipart~1form-data/schema");
        if (bodySchema.has("$ref")) {
            String schemaName = bodySchema.path("$ref").asText().replace("#/components/schemas/", "");
            bodySchema = document.path("components").path("schemas").path(schemaName);
        }
        assertThat(bodySchema.path("required").toString()).contains("file");
        assertThat(bodySchema.at("/properties/file/type").asText()).isEqualTo("string");
        assertThat(bodySchema.at("/properties/file/format").asText()).isEqualTo("binary");
        assertThat(operation.path("security").get(0).has("shoubingBearer")).isTrue();
        assertThat(document.at("/components/securitySchemes/shoubingBearer/type").asText()).isEqualTo("http");
        assertThat(document.at("/components/securitySchemes/shoubingBearer/scheme").asText()).isEqualTo("bearer");

        JsonNode responses = operation.path("responses");
        assertThat(responseCodes(responses)).containsExactlyInAnyOrder("201", "400", "401", "403", "413", "503");
        assertThat(responses.at("/201/headers").has("Location")).isTrue();
        assertThat(responses.at("/401/content/application~1json/schema/type").asText()).isEqualTo("object");
        assertThat(responses.at("/401/content/application~1json/schema/required"))
                .extracting(JsonNode::toString)
                .asString()
                .contains("code", "message");
        assertThat(responses.at("/403/content/application~1json/schema/type").asText()).isEqualTo("object");
    }

    private static List<String> responseCodes(JsonNode responses) {
        List<String> codes = new ArrayList<>();
        responses.fieldNames().forEachRemaining(codes::add);
        return codes;
    }
}
