package top.egon.enjoyshop.goods.adapter.goods;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import top.egon.enjoyshop.goods.start.StudentManagementApplication;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code TEST-014}: the generated OAS 3 document is the contract the frontend and any SDK are built
 * from, so its stable anchors — the five {@code operationId}s, the required-ness of the path and
 * query parameters, and the fact that only the three write contracts declare 401 — are asserted here
 * instead of being eyeballed.
 *
 * <p>Following {@code DEC-111} (option A) nothing is published to Yuheng and no live instance is
 * consulted: springdoc builds the document inside the application context and it is read over
 * MockMvc, which is a unit-level route with no network. The context reuses
 * {@code PersistenceTestSupport}'s embedded H2 fixture with the DDL runner switched off, exactly as
 * the other {@code @SpringBootTest} classes in this module do, so no PostgreSQL instance is touched.</p>
 *
 * <p>The document is fetched once and held statically: {@code PersistenceTestSupport} marks the
 * context dirty after every method, so three fetches would rebuild the same document three times.</p>
 */
@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest(classes = StudentManagementApplication.class)
class BrandOpenApiContractTest extends top.egon.enjoyshop.goods.support.PersistenceTestSupport {

    private static final String COLLECTION = "/api/v1/brands";
    private static final String ITEM = "/api/v1/brands/{brandId}";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static JsonNode document;

    @Autowired
    private MockMvc mockMvc;

    private JsonNode paths() throws Exception {
        if (document == null) {
            byte[] body = mockMvc.perform(get("/v3/api-docs"))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsByteArray();
            document = OBJECT_MAPPER.readTree(new String(body, StandardCharsets.UTF_8)).path("paths");
        }
        return document;
    }

    @Test
    void five_operation_ids_match_spec() throws Exception {
        JsonNode paths = paths();
        assertThat(operation(paths, COLLECTION, "get").path("operationId").asText()).isEqualTo("listBrands");
        assertThat(operation(paths, ITEM, "get").path("operationId").asText()).isEqualTo("getBrand");
        assertThat(operation(paths, COLLECTION, "post").path("operationId").asText()).isEqualTo("createBrand");
        assertThat(operation(paths, ITEM, "put").path("operationId").asText()).isEqualTo("updateBrand");
        assertThat(operation(paths, ITEM, "delete").path("operationId").asText()).isEqualTo("deleteBrand");

        assertThat(texts(operation(paths, COLLECTION, "post").path("tags")))
                .as("every brand operation carries the class-level tag")
                .containsExactly("brand");
    }

    @Test
    void required_parameters_match_contract() throws Exception {
        JsonNode paths = paths();
        for (String verb : List.of("get", "put", "delete")) {
            JsonNode brandId = parameter(operation(paths, ITEM, verb), "brandId");
            assertThat(brandId.path("in").asText()).as("brandId is a path parameter for %s", verb).isEqualTo("path");
            assertThat(brandId.path("required").asBoolean()).as("brandId is required for %s", verb).isTrue();
            assertThat(brandId.at("/schema/type").asText()).as("brandId type for %s", verb).isEqualTo("integer");
            assertThat(brandId.at("/schema/format").asText()).as("brandId format for %s", verb).isEqualTo("int64");
        }

        JsonNode version = parameter(operation(paths, ITEM, "delete"), "version");
        assertThat(version.path("in").asText()).isEqualTo("query");
        assertThat(version.path("required").asBoolean()).as("version is required on delete").isTrue();
        assertThat(version.at("/schema/format").asText()).isEqualTo("int64");

        // API-001 is fully optional: the absence of both page parameters is what selects full-catalog mode.
        JsonNode listParameters = operation(paths, COLLECTION, "get").path("parameters");
        assertThat(names(listParameters))
                .containsExactlyInAnyOrder(
                        "name", "letter", "seqMin", "seqMax", "pageNo", "pageSize", "categoryName");
        for (JsonNode param : listParameters) {
            assertThat(param.path("required").asBoolean())
                    .as("list parameter %s stays optional", param.path("name").asText())
                    .isFalse();
        }
        JsonNode categoryName = parameter(operation(paths, COLLECTION, "get"), "categoryName");
        assertThat(categoryName.path("in").asText()).isEqualTo("query");
        assertThat(categoryName.at("/schema/type").asText()).isEqualTo("string");
        assertThat(categoryName.at("/schema/maxLength").asInt()).isEqualTo(120);
    }

    @Test
    void write_operations_declare_401_read_do_not() throws Exception {
        JsonNode paths = paths();
        assertThat(responseCodes(operation(paths, COLLECTION, "post")))
                .as("API-003 declares 401 but has no 404 to declare")
                .containsExactlyInAnyOrder("201", "400", "401", "409", "500");
        assertThat(responseCodes(operation(paths, ITEM, "put")))
                .as("API-004")
                .containsExactlyInAnyOrder("200", "400", "401", "404", "409", "500");
        assertThat(responseCodes(operation(paths, ITEM, "delete")))
                .as("API-005")
                .containsExactlyInAnyOrder("200", "400", "401", "404", "409", "500");
        assertThat(responseCodes(operation(paths, COLLECTION, "get")))
                .as("API-001 is read-only: no 401, no 404")
                .containsExactlyInAnyOrder("200", "400", "500");
        assertThat(responseCodes(operation(paths, ITEM, "get")))
                .as("API-002 is read-only: no 401")
                .containsExactlyInAnyOrder("200", "400", "404", "500");

        assertThat(operation(paths, COLLECTION, "post").at("/responses/201/headers").has("Location"))
                .as("only the created-contract advertises Location")
                .isTrue();
        assertThat(operation(paths, ITEM, "put").at("/responses").has("201"))
                .as("update answers 200, never 201")
                .isFalse();
    }

    private static JsonNode operation(JsonNode paths, String path, String verb) {
        // path() and not at(): a path key such as "/api/v1/brands" is one field name, and at() would
        // read it as a nested JSON pointer and always come back missing.
        JsonNode operation = paths.path(path).path(verb);
        assertThat(operation.isMissingNode())
                .as("%s %s must be documented", verb.toUpperCase(), path)
                .isFalse();
        return operation;
    }

    private static JsonNode parameter(JsonNode operation, String name) {
        JsonNode match = null;
        for (JsonNode param : operation.path("parameters")) {
            if (name.equals(param.path("name").asText())) {
                match = param;
            }
        }
        assertThat(match).as("parameter %s must be documented", name).isNotNull();
        return match;
    }

    /**
     * Names of the six API-001 filters. They cannot go through {@link #texts}: a {@code parameters}
     * entry is an object node, and {@code JsonNode#asText()} on an object is the empty string, which
     * is exactly what made this assertion report six nameless parameters the first time.
     */
    private static List<String> names(JsonNode parameters) {
        return StreamSupport
                .stream(Spliterators.spliteratorUnknownSize(parameters.elements(), 0), false)
                .map(param -> param.path("name").asText())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /** Text items of a JSON array, e.g. an operation's {@code tags}. */
    private static List<String> texts(JsonNode array) {
        return StreamSupport
                .stream(Spliterators.spliteratorUnknownSize(array.elements(), 0), false)
                .map(node -> node.asText())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private static List<String> responseCodes(JsonNode operation) {
        JsonNode responses = operation.path("responses");
        assertThat(responses.isObject()).as("responses must be an object").isTrue();
        List<String> codes = new ArrayList<>();
        responses.fieldNames().forEachRemaining(codes::add);
        return codes;
    }
}
