package top.egon.enjoyshop.goods.adapter.goods;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import top.egon.enjoyshop.goods.application.goods.SpecManage;
import top.egon.enjoyshop.goods.start.StudentManagementApplication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest(classes = StudentManagementApplication.class)
class SpecOpenApiContractTest extends top.egon.enjoyshop.goods.support.PersistenceTestSupport {

    private static final String PATH = "/api/v1/specs/category/{categoryName}";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SpecManage specManage;

    @Test
    void api_007_is_published_with_required_category_path_and_option_array() throws Exception {
        byte[] body = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();
        JsonNode document = OBJECT_MAPPER.readTree(body);
        JsonNode operation = document.path("paths").path(PATH).path("get");

        assertThat(operation.isMissingNode()).isFalse();
        assertThat(operation.path("operationId").asText()).isEqualTo("listSpecsByCategoryName");
        assertThat(operation.path("security").get(0).has("bearerAuth")).isTrue();
        assertThat(operation.path("responses").has("401")).isTrue();
        assertThat(operation.path("responses").has("403")).isTrue();
        assertThat(operation.path("responses").has("503")).isTrue();
        assertThat(document.at("/components/securitySchemes/bearerAuth/scheme").asText()).isEqualTo("bearer");
        JsonNode categoryName = null;
        for (JsonNode parameter : operation.path("parameters")) {
            if ("categoryName".equals(parameter.path("name").asText())) {
                categoryName = parameter;
                break;
            }
        }
        assertThat(categoryName).isNotNull();
        assertThat(categoryName.path("in").asText()).isEqualTo("path");
        assertThat(categoryName.path("required").asBoolean()).isTrue();
        assertThat(categoryName.at("/schema/type").asText()).isEqualTo("string");
        assertThat(categoryName.at("/schema/maxLength").asInt()).isEqualTo(120);

        var responseMediaTypes = operation.path("responses").path("200").path("content").fields();
        assertThat(responseMediaTypes.hasNext()).isTrue();
        JsonNode responseSchema = responseMediaTypes.next().getValue().path("schema");
        String resultSchemaName = schemaName(responseSchema.path("$ref").asText());
        JsonNode resultData = document.path("components").path("schemas").path(resultSchemaName)
                .path("properties").path("data");
        assertThat(resultData.path("type").asText()).isEqualTo("array");
        String optionSchemaName = schemaName(resultData.path("items").path("$ref").asText());
        JsonNode options = document.path("components").path("schemas").path(optionSchemaName)
                .path("properties").path("options");
        assertThat(options.path("type").asText()).isEqualTo("array");
        assertThat(options.path("items").path("type").asText()).isEqualTo("string");
    }

    private static String schemaName(String reference) {
        return reference.substring(reference.lastIndexOf('/') + 1);
    }
}
