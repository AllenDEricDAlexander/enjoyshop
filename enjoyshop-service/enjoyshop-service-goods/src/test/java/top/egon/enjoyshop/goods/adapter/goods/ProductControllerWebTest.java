package top.egon.enjoyshop.goods.adapter.goods;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import top.egon.enjoyshop.goods.start.StudentManagementApplication;
import top.egon.enjoyshop.goods.support.PersistenceTestSupport;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest(classes = StudentManagementApplication.class)
class ProductControllerWebTest extends PersistenceTestSupport {
    @Autowired private MockMvc mvc;
    @Autowired @Qualifier("jacksonObjectMapper") private ObjectMapper mapper;
    private JdbcTemplate jdbc;

    @BeforeEach void catalog() throws Exception {
        jdbc = new JdbcTemplate(testDataSource);
        for (String resource : List.of("V20260923_001__initialize_goods_schema.sql", "V20260929_001__initialize_catalog_lookup_schema.sql")) {
            String ddl = Files.readString(Path.of("src/main/resources/db/egon-mp", resource));
            var tables = java.util.regex.Pattern.compile("CREATE TABLE [a-z_]+ \\(.*?\\)\\s*;", java.util.regex.Pattern.DOTALL).matcher(ddl);
            while (tables.find()) { jdbc.execute(tables.group()); }
        }
        jdbc.update("INSERT INTO brand(id,tenant_id,name,image,letter,seq) VALUES(1,1,'品牌','','B',0)");
        jdbc.update("INSERT INTO template(id,tenant_id,name) VALUES(5,1,'模板')");
        jdbc.update("INSERT INTO category(id,tenant_id,name,parent_id,template_id) VALUES(2,1,'一级',NULL,NULL)");
        jdbc.update("INSERT INTO category(id,tenant_id,name,parent_id,template_id) VALUES(3,1,'二级',2,NULL)");
        jdbc.update("INSERT INTO category(id,tenant_id,name,parent_id,template_id) VALUES(4,1,'手机',3,5)");
    }

    private MockHttpServletRequestBuilder token(MockHttpServletRequestBuilder request, String action) {
        return request.header("Authorization", "Bearer product-" + action);
    }

    private ObjectNode body() throws Exception {
        return (ObjectNode) mapper.readTree("""
                {"spu":{"sn":"P-WEB","name":"手机","brandId":"1","category1Id":"2","category2Id":"3","category3Id":"4","templateId":"5","isEnableSpec":false},
                 "skuList":[{"sn":"S-WEB","price":100,"num":5,"alertNum":1,"weight":10}]}
                """);
    }

    private ObjectNode create() throws Exception {
        var response = mvc.perform(token(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content(body().toString()), "all"))
                .andExpect(status().isCreated()).andExpect(header().exists("Location"))
                .andExpect(jsonPath("data.spu.id").isString()).andExpect(jsonPath("data.skuList[0].id").isString())
                .andExpect(jsonPath("data.spu.status").value("0")).andExpect(jsonPath("data.spu.isMarketable").value(false))
                .andReturn().getResponse();
        return (ObjectNode) mapper.readTree(response.getContentAsByteArray()).path("data");
    }

    @Test void tenRoutesCompleteLifecycleAndPreserveStableIds() throws Exception {
        ObjectNode created = create(); String id = created.path("spu").path("id").asText();
        String sku = created.path("skuList").get(0).path("id").asText(); String item = "/api/v1/products/" + id;
        mvc.perform(token(get("/api/v1/products"), "read")).andExpect(status().isOk()).andExpect(jsonPath("records[0].id").value(id));
        mvc.perform(token(get(item), "read")).andExpect(status().isOk()).andExpect(jsonPath("data.spu.id").value(id));
        ObjectNode changed = body(); changed.put("version", 0); ((ObjectNode) changed.path("skuList").get(0)).put("id", sku);
        mvc.perform(token(put(item).contentType(MediaType.APPLICATION_JSON).content(changed.toString()), "write"))
                .andExpect(status().isOk()).andExpect(jsonPath("data.spu.version").value(1)).andExpect(jsonPath("data.skuList[0].id").value(sku));
        state(item + "/approval", 1, "audit", 2, true);
        mvc.perform(token(delete(item).param("version", "2"), "write")).andExpect(status().isConflict()).andExpect(jsonPath("code").value(610204));
        state(item + "/withdrawal", 2, "write", 3, false);
        state(item + "/publication", 3, "write", 4, true);
        state(item + "/withdrawal", 4, "write", 5, false);
        mvc.perform(token(delete(item).param("version", "5"), "write")).andExpect(status().isOk()).andExpect(jsonPath("data").value(true));
        mvc.perform(token(get(item), "read")).andExpect(status().isNotFound()).andExpect(jsonPath("code").value(610202));
        mvc.perform(token(get(item).param("deleted", "true"), "read")).andExpect(status().isOk()).andExpect(jsonPath("data.skuList[0].id").value(sku));
        state(item + "/restoration", 6, "write", 7, false);
        mvc.perform(token(delete(item).param("version", "7"), "write")).andExpect(status().isOk());
        mvc.perform(token(delete(item + "/purge").param("version", "8"), "purge")).andExpect(status().isOk()).andExpect(jsonPath("data").value(true));
        mvc.perform(token(delete(item + "/purge").param("version", "8"), "purge")).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM spu", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sku", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM category_brand", Integer.class)).isEqualTo(1);
    }

    private void state(String path, int version, String permission, int next, boolean marketable) throws Exception {
        mvc.perform(token(put(path).contentType(MediaType.APPLICATION_JSON).content("{\"version\":" + version + "}"), permission))
                .andExpect(status().isOk()).andExpect(jsonPath("data.version").value(next)).andExpect(jsonPath("data.isMarketable").value(marketable));
    }

    @Test void permissionsAndTrustedTenantCannotBeSpoofed() throws Exception {
        String item = "/api/v1/products/" + create().path("spu").path("id").asText();
        mvc.perform(token(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content(body().toString()), "read"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("code").value(610206));
        mvc.perform(token(put(item + "/approval").contentType(MediaType.APPLICATION_JSON).content("{\"version\":0}"), "write"))
                .andExpect(status().isForbidden());
        mvc.perform(token(delete(item + "/purge").param("version", "0"), "write")).andExpect(status().isForbidden());
        mvc.perform(token(get(item).header("X-Tenant-Id", "2").header("X-Actor-Id", "attacker"), "all")).andExpect(status().isOk());
        mvc.perform(token(get(item), "other-tenant")).andExpect(status().isNotFound());
        mvc.perform(token(get(item), "resource-missing")).andExpect(status().isForbidden()).andExpect(jsonPath("code").value(610206));
        assertThat(jdbc.queryForObject("SELECT create_user_id FROM spu", String.class)).isEqualTo("product-all");
        assertThat(org.slf4j.MDC.get("tenantId")).isNull();
    }

    @Test void strictTokensNestedFieldPathsAndPaginationHaveProductErrors() throws Exception {
        ObjectNode invalid = body(); ((ObjectNode) invalid.path("skuList").get(0)).put("price", 0);
        mvc.perform(token(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content(invalid.toString()), "all"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("code").value(610201))
                .andExpect(jsonPath("data.fieldErrors[0].field").value("skuList[0].price"));
        invalid = body(); ((ObjectNode) invalid.path("skuList").get(0)).put("price", "100");
        mvc.perform(token(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content(invalid.toString()), "all"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("code").value(610201));
        mvc.perform(token(get("/api/v1/products/001"), "read")).andExpect(status().isBadRequest());
        mvc.perform(token(get("/api/v1/products").param("pageSize", "101"), "read")).andExpect(status().isBadRequest());
        mvc.perform(token(get("/api/v1/products").param("deleted", "yes"), "read")).andExpect(status().isBadRequest());
        mvc.perform(token(get("/api/v1/products").param("pageSize", "1", "2"), "read")).andExpect(status().isBadRequest());
    }
}
