package top.egon.enjoyshop.goods.adapter.goods;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import top.egon.enjoyshop.goods.start.StudentManagementApplication;
import top.egon.enjoyshop.goods.support.PersistenceTestSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Reads the ads OpenAPI group with MockMvc and isolated H2; no PostgreSQL, Redis, identity provider or broker starts. */
@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest(classes = StudentManagementApplication.class)
class AdOpenApiContractTest extends PersistenceTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void adsGroupDocumentsExactlySixOperationsAndKeepsPublicSchemaPrivate() throws Exception {
        JsonNode document = new ObjectMapper().readTree(mockMvc.perform(get("/v3/api-docs/ads"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray());

        Map<String, String> operations = Map.of(
                "get /api/v1/ads", "listAdvertisements",
                "post /api/v1/ads", "createAdvertisement",
                "get /api/v1/ads/{adId}", "getAdvertisement",
                "put /api/v1/ads/{adId}", "replaceAdvertisement",
                "delete /api/v1/ads/{adId}", "deleteAdvertisement",
                "get /api/v1/ad-slots/{position}/ads", "listPublishedAdvertisements");
        assertThat(document.path("paths").size()).isEqualTo(3);
        for (Map.Entry<String, String> entry : operations.entrySet()) {
            String[] key = entry.getKey().split(" ", 2);
            JsonNode operation = document.path("paths").path(key[1]).path(key[0]);
            assertThat(operation.path("operationId").asText()).isEqualTo(entry.getValue());
            assertThat(operation.path("responses").has(key[0].equals("post") ? "201" : "200")).isTrue();
        }
        assertThat(document.at("/paths/~1api~1v1~1ads/post/responses/201/headers/Location").isMissingNode()).isFalse();
        assertThat(document.at("/paths/~1api~1v1~1ads/post/requestBody/content/application~1json/schema/$ref").asText())
                .isEqualTo("#/components/schemas/AdCreateInput");
        assertThat(document.at("/paths/~1api~1v1~1ads~1{adId}/put/requestBody/content/application~1json/schema/$ref").asText())
                .isEqualTo("#/components/schemas/AdUpdateInput");
        JsonNode createRequired = document.at("/components/schemas/AdCreateInput/required");
        assertThat(createRequired.toString()).doesNotContain("version");
        assertThat(document.at("/components/schemas/AdCreateInput/properties/version/readOnly").asBoolean()).isTrue();
        assertThat(document.at("/components/schemas/AdCreateInput/properties/version/type").toString())
                .contains("integer", "null");
        assertThat(document.at("/components/schemas/AdUpdateInput/required").toString()).contains("version");
        assertThat(document.at("/paths/~1api~1v1~1ad-slots~1{position}~1ads/get/security").isArray()).isTrue();
        assertThat(document.at("/paths/~1api~1v1~1ad-slots~1{position}~1ads/get/security").isEmpty()).isTrue();
        assertThat(document.at("/paths/~1api~1v1~1ads/get/security").toString()).contains("bearerAuth");
        assertThat(document.at("/components/securitySchemes/bearerAuth/scheme").asText()).isEqualTo("bearer");
        assertThat(document.at("/components/schemas/AdResult/properties/id/type").asText()).isEqualTo("string");
        assertThat(document.at("/components/schemas/AdPublicResult/properties/id/type").asText()).isEqualTo("string");
        JsonNode publicProperties = document.at("/components/schemas/AdPublicResult/properties");
        assertThat(propertyNames(publicProperties)).containsExactlyInAnyOrder("id", "name", "imageUrl", "targetUrl");
        assertThat(publicProperties.has("tenantId")).isFalse();
        assertThat(publicProperties.has("version")).isFalse();
        assertThat(publicProperties.has("startAt")).isFalse();
        assertThat(document.path("paths").has("/api/v1/products")).isFalse();
        assertThat(document.path("paths").has("/api/v1/brands")).isFalse();
        assertThat(document.path("paths").has("/api/v1/files")).isFalse();

        mockMvc.perform(get("/api/v1/ads"))
                .andExpect(status().isUnauthorized())
                .andExpect(computed -> assertThat(new ObjectMapper().readTree(
                        computed.getResponse().getContentAsByteArray()).path("code").asText())
                        .isEqualTo("AUTHORIZATION_HEADER_INVALID"));
        mockMvc.perform(get("/api/v1/ads").header("Authorization", "Bearer product-read"))
                .andExpect(status().isForbidden())
                .andExpect(computed -> assertThat(new ObjectMapper().readTree(
                        computed.getResponse().getContentAsByteArray()).path("code").asInt()).isEqualTo(610305));
        mockMvc.perform(get("/api/v1/ad-slots/web_index_lb/ads")
                        .header("X-Tenant-Id", "41")
                        .header("X-Forwarded-Host", "store.example.test")
                        .with(request -> {
                            request.setServerName("unregistered.example.test");
                            return request;
                        }))
                .andExpect(status().isNotFound())
                .andExpect(computed -> assertThat(new ObjectMapper().readTree(
                        computed.getResponse().getContentAsByteArray()).path("code").asInt()).isEqualTo(610306));
        mockMvc.perform(post("/api/v1/ad-slots/web_index_lb/ads")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(computed -> assertThat(new ObjectMapper().readTree(
                        computed.getResponse().getContentAsByteArray()).path("code").asText())
                        .isEqualTo("AUTHORIZATION_HEADER_INVALID"));
    }

    private static Set<String> propertyNames(JsonNode properties) {
        java.util.Set<String> names = new java.util.HashSet<>();
        properties.fieldNames().forEachRemaining(names::add);
        return names;
    }
}
