package top.egon.enjoyshop.goods.adapter.goods;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import top.egon.cola.component.common.core.pojo.PageResultRecord;
import top.egon.enjoyshop.goods.adapter.filter.TraceIdFilter;
import top.egon.enjoyshop.goods.adapter.handler.BaseExceptionHandler;
import top.egon.enjoyshop.goods.application.ad.AdLifecycleCommand;
import top.egon.enjoyshop.goods.application.ad.AdManage;
import top.egon.enjoyshop.goods.application.ad.AdPageQuery;
import top.egon.enjoyshop.goods.application.ad.AdPublicResult;
import top.egon.enjoyshop.goods.application.ad.AdReadQuery;
import top.egon.enjoyshop.goods.application.ad.AdResult;
import top.egon.enjoyshop.goods.application.ad.AdSaveCommand;
import top.egon.enjoyshop.goods.common.validation.AdValidationGroup;
import top.egon.enjoyshop.goods.domain.ad.AdStatusEnum;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Isolated HTTP contract tests; the fake Manage has no persistence or broker dependencies. */
@WebMvcTest(controllers = AdController.class)
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = {
        AdController.class,
        BaseExceptionHandler.class,
        TraceIdFilter.class,
        AdControllerTest.AdControllerTestConfiguration.class
})
class AdControllerTest {

    @TestConfiguration(proxyBeanMethods = false)
    static class AdControllerTestConfiguration {

        @Bean
        AdManageState adManageState() {
            return new AdManageState();
        }

        @Bean("adManage")
        AdManage adManage(AdManageState state) {
            return new TestAdManage(state);
        }

        @Bean("adAllowedPositions")
        List<String> adAllowedPositions() {
            return List.of("web_index_lb");
        }

        @Bean("adImagePublicBase")
        URI adImagePublicBase() {
            return URI.create("https://images.example.test/products/");
        }
    }

    private static final String AD_ID = "900000000000000001";
    private static final Instant START = Instant.parse("2026-10-10T00:00:00Z");
    private static final Instant END = Instant.parse("2026-11-01T00:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdManageState manage;

    @BeforeEach
    void resetManage() {
        manage.reset();
    }

    @Test
    void listDefaultsAndForwardsThePageQuery() throws Exception {
        manage.pageResult = PageResultRecord.success(List.of(adResult()), 1, 1, 20);

        mockMvc.perform(get("/api/v1/ads"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].id").value(AD_ID))
                .andExpect(jsonPath("$.page.pageNo").value(1))
                .andExpect(jsonPath("$.page.pageSize").value(20))
                .andExpect(jsonPath("$.page.pages").value(1));

        assertThat(manage.lastPage.getPageNo()).isEqualTo(1);
        assertThat(manage.lastPage.getPageSize()).isEqualTo(20);
    }

    @Test
    void listParsesCodeFiltersAndRejectsUnknownOrRepeatedQueryParameters() throws Exception {
        manage.pageResult = PageResultRecord.success(List.of(), 0, 2, 5);

        mockMvc.perform(get("/api/v1/ads").param("pageNo", "2").param("pageSize", "5")
                        .param("position", "web_index_lb").param("status", "1"))
                .andExpect(status().isOk());
        assertThat(manage.lastPage.getPosition()).isEqualTo("web_index_lb");
        assertThat(manage.lastPage.getStatus()).isEqualTo(AdStatusEnum.ENABLED);
        assertThat(manage.lastPage.getPageNo()).isEqualTo(2);
        assertThat(manage.lastPage.getPageSize()).isEqualTo(5);

        mockMvc.perform(get("/api/v1/ads").param("tenantId", "41"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(610301));
        mockMvc.perform(get("/api/v1/ads").param("pageNo", "1", "2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(610301));
        mockMvc.perform(get("/api/v1/ads").param("status", "2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(610301));
    }

    @Test
    void detailRequiresCanonicalDecimalIdAndReturnsEnvelope() throws Exception {
        manage.detailResult = adResult();

        mockMvc.perform(get("/api/v1/ads/" + AD_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(AD_ID))
                .andExpect(jsonPath("$.data.version").value(3));
        assertThat(manage.lastDetailId).isEqualTo(900000000000000001L);

        mockMvc.perform(get("/api/v1/ads/001"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(610301));
        mockMvc.perform(get("/api/v1/ads/9223372036854775808"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(610301));
    }

    @Test
    void createTrimsNameAndReturns201WithLocation() throws Exception {
        manage.createResult = adResult();

        mockMvc.perform(post("/api/v1/ads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeBody(null, "  Spring sale  ")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/ads/" + AD_ID))
                .andExpect(jsonPath("$.data.id").value(AD_ID));

        assertThat(manage.lastCreate.getName()).isEqualTo("Spring sale");
        assertThat(manage.lastCreate.getVersion()).isNull();
    }

    @Test
    void createRejectsUnknownFieldsAndInvalidWindows() throws Exception {
        mockMvc.perform(post("/api/v1/ads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeBody(null, "Spring sale").replace("\"sortOrder\":1", "\"sortOrder\":1,\"tenantId\":41")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(610301));
        mockMvc.perform(post("/api/v1/ads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeBody(null, "Spring sale").replace(END.toString(), START.toString())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(610301));
        mockMvc.perform(post("/api/v1/ads")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("{}"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value(610301));
    }

    @Test
    void replaceRequiresIntegerVersionAndIsAFullReplacement() throws Exception {
        manage.replaceResult = adResult();

        mockMvc.perform(put("/api/v1/ads/" + AD_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeBody(2, "Spring sale")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(3));
        assertThat(manage.lastReplaceId).isEqualTo(900000000000000001L);
        assertThat(manage.lastReplace.getVersion()).isEqualTo(2L);

        mockMvc.perform(put("/api/v1/ads/" + AD_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeBody(null, "Spring sale")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(610301));
    }

    @Test
    void deleteReturnsBooleanEnvelopeAndRejectsDuplicateVersion() throws Exception {
        manage.deleteResult = true;

        mockMvc.perform(delete("/api/v1/ads/" + AD_ID).param("version", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
        assertThat(manage.lastDelete.getId()).isEqualTo(900000000000000001L);
        assertThat(manage.lastDelete.getVersion()).isEqualTo(2L);

        mockMvc.perform(delete("/api/v1/ads/" + AD_ID).param("version", "2", "3"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(610301));
    }

    @Test
    void publicReadReturnsOnlyPublicProjectionAndNoStore() throws Exception {
        manage.publishedResults = List.of(new AdPublicResult()
                .setId(AD_ID).setName("Spring sale")
                .setImageUrl("https://images.example.test/products/ad.png")
                .setTargetUrl("/events/spring"));

        mockMvc.perform(get("/api/v1/ad-slots/web_index_lb/ads").param("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.data[0].id").value(AD_ID))
                .andExpect(jsonPath("$.data[0].tenantId").doesNotExist())
                .andExpect(jsonPath("$.data[0].version").doesNotExist())
                .andExpect(jsonPath("$.data[0].startAt").doesNotExist());

        mockMvc.perform(get("/api/v1/ad-slots/web_index_lb/ads").param("tenantId", "41"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(610301));

        assertThat(manage.lastPublished.getPosition()).isEqualTo("web_index_lb");
        assertThat(manage.lastPublished.getLimit()).isEqualTo(1);
    }

    private static String writeBody(Integer version, String name) {
        String versionField = version == null ? "" : "\"version\":" + version + ",";
        return "{" + versionField
                + "\"name\":\"" + name + "\","
                + "\"position\":\"web_index_lb\","
                + "\"imageUrl\":\"https://images.example.test/products/ad.png\","
                + "\"targetUrl\":\"/events/spring\","
                + "\"status\":\"1\","
                + "\"startAt\":\"" + START + "\","
                + "\"endAt\":\"" + END + "\","
                + "\"sortOrder\":1} ";
    }

    private static AdResult adResult() {
        return new AdResult().setId(AD_ID).setName("Spring sale").setPosition("web_index_lb")
                .setImageUrl("https://images.example.test/products/ad.png").setTargetUrl("/events/spring")
                .setStatus(AdStatusEnum.ENABLED).setStartAt(START).setEndAt(END).setSortOrder(1).setVersion(3L);
    }

    static class AdManageState {
        AdResult createResult;
        AdResult replaceResult;
        AdResult detailResult;
        PageResultRecord<AdResult> pageResult;
        List<AdPublicResult> publishedResults;
        boolean deleteResult;
        AdSaveCommand lastCreate;
        AdSaveCommand lastReplace;
        Long lastReplaceId;
        Long lastDetailId;
        AdLifecycleCommand lastDelete;
        AdPageQuery lastPage;
        AdReadQuery lastPublished;

        void reset() {
            createResult = null;
            replaceResult = null;
            detailResult = null;
            pageResult = null;
            publishedResults = List.of();
            deleteResult = false;
            lastCreate = null;
            lastReplace = null;
            lastReplaceId = null;
            lastDetailId = null;
            lastDelete = null;
            lastPage = null;
            lastPublished = null;
        }
    }

    static class TestAdManage implements AdManage {
        private final AdManageState state;

        TestAdManage(AdManageState state) {
            this.state = state;
        }

        @Override
        public AdResult create(AdSaveCommand command) {
            state.lastCreate = command;
            return state.createResult;
        }

        @Override
        public AdResult replace(Long adId, AdSaveCommand command) {
            state.lastReplaceId = adId;
            state.lastReplace = command;
            return state.replaceResult;
        }

        @Override
        public PageResultRecord<AdResult> page(AdPageQuery query) {
            state.lastPage = query;
            return state.pageResult;
        }

        @Override
        public AdResult detail(Long adId) {
            state.lastDetailId = adId;
            return state.detailResult;
        }

        @Override
        public boolean delete(AdLifecycleCommand command) {
            state.lastDelete = command;
            return state.deleteResult;
        }

        @Override
        public List<AdPublicResult> published(AdReadQuery query) {
            state.lastPublished = query;
            return state.publishedResults;
        }
    }
}
