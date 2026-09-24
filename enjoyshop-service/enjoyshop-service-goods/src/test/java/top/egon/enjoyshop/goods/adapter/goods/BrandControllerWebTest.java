package top.egon.enjoyshop.goods.adapter.goods;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.cola.component.common.core.pojo.PageResultRecord;
import top.egon.enjoyshop.goods.adapter.filter.TenantContextFilter;
import top.egon.enjoyshop.goods.adapter.goods.pojo.convertor.BrandAdapterConverterImpl;
import top.egon.enjoyshop.goods.adapter.filter.TraceIdFilter;
import top.egon.enjoyshop.goods.adapter.handler.BaseExceptionHandler;
import top.egon.enjoyshop.goods.application.goods.BrandManage;
import top.egon.enjoyshop.goods.application.goods.BrandPageQuery;
import top.egon.enjoyshop.goods.application.goods.BrandResult;
import top.egon.enjoyshop.goods.common.enums.GoodsErrorStatus;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The five {@code API-001}-{@code API-005} contract shapes (Spec §9.2, {@code TEST-002}/003/006/007/
 * 009/010/016). The mocked {@link BrandManage} fixes the adapter boundary: what is asserted here is
 * the HTTP envelope, status code and OpenAPI-visible parameter names, not persistence behavior.
 */
@WebMvcTest(controllers = BrandController.class)
@ContextConfiguration(classes = {
        top.egon.enjoyshop.goods.start.config.NativeHttpSecurityConfiguration.class,
        BrandController.class,
        BrandAdapterConverterImpl.class,
        BaseExceptionHandler.class,
        TraceIdFilter.class,
        BrandControllerWebTest.ManageMockConfiguration.class
})
class BrandControllerWebTest {

    /**
     * The collaborator is registered under its production bean name so the controller's
     * {@code @Qualifier("brandManage")} injection point resolves exactly as it does at runtime, and
     * {@link #resetCollaborator()} restores the per-test isolation that a cached slice context
     * would otherwise lose.
     */
    @TestConfiguration(proxyBeanMethods = false)
    static class ManageMockConfiguration {

        @Bean("brandManage")
        BrandManage brandManage() {
            return Mockito.mock(BrandManage.class);
        }
    }

    private static final String CREATED_ID = "1948768293456580097";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BrandManage brandManage;

    @BeforeEach
    void resetCollaborator() {
        reset(brandManage);
    }

    private static BrandResult aBrand(long idVersion) {
        return new BrandResult().setId(CREATED_ID).setName("Apple").setImage(null).setLetter("A")
                .setSeq(100).setVersion(idVersion);
    }

    @Test
    void create_returns_201_with_location_and_envelope() throws Exception {
        when(brandManage.create(any())).thenReturn(aBrand(0L));

        mockMvc.perform(post("/api/v1/brands").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Apple\",\"letter\":\"a\",\"seq\":100}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/brands/" + CREATED_ID))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(10000))
                .andExpect(jsonPath("$.data.id").value(1948768293456580097L))
                .andExpect(jsonPath("$.data.version").value(0))
                .andExpect(jsonPath("$.data.letter").value("A"));
    }

    @Test
    void list_without_page_params_uses_full_mode_500() throws Exception {
        when(brandManage.listBrands(any(), isNull(), isNull()))
                .thenReturn(PageResultRecord.success(List.of(aBrand(0L)), 1L, 1, 500));

        mockMvc.perform(get("/api/v1/brands"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.pageSize").value(500))
                .andExpect(jsonPath("$.page.pageNo").value(1))
                .andExpect(jsonPath("$.page.total").value(1))
                .andExpect(jsonPath("$.records").isArray())
                .andExpect(jsonPath("$.records[0].id").value(1948768293456580097L));

        ArgumentCaptor<BrandPageQuery> captor = ArgumentCaptor.forClass(BrandPageQuery.class);
        verify(brandManage).listBrands(captor.capture(), isNull(), isNull());
        assertThat(captor.getValue().getPage()).isEqualTo(new PageQuery(PageQuery.DEFAULT_PAGE_NO, 500));
    }

    @Test
    void out_of_range_page_returns_empty_records() throws Exception {
        when(brandManage.listBrands(any(), isNull(), isNull()))
                .thenReturn(PageResultRecord.success(List.of(), 3L, 999, 10));

        mockMvc.perform(get("/api/v1/brands").param("pageNo", "999").param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records").isArray())
                .andExpect(jsonPath("$.records").isEmpty())
                .andExpect(jsonPath("$.page.total").value(3));
    }

    @Test
    void update_echoes_the_new_version() throws Exception {
        when(brandManage.update(any())).thenReturn(aBrand(4L).setName("Apple Inc"));

        mockMvc.perform(put("/api/v1/brands/" + CREATED_ID).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Apple Inc\",\"letter\":\"A\",\"seq\":120,\"version\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.version").value(4))
                .andExpect(jsonPath("$.data.name").value("Apple Inc"));

        ArgumentCaptor<top.egon.enjoyshop.goods.application.goods.UpdateBrandCommand> captor =
                ArgumentCaptor.forClass(top.egon.enjoyshop.goods.application.goods.UpdateBrandCommand.class);
        verify(brandManage).update(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(Long.valueOf(CREATED_ID));
        assertThat(captor.getValue().getExpectedVersion()).isEqualTo(3L);
    }

    @Test
    void delete_returns_true_and_repeat_maps_404() throws Exception {
        mockMvc.perform(delete("/api/v1/brands/" + CREATED_ID).param("version", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(true));

        doThrow(new CommonException(GoodsErrorStatus.BRAND_NOT_FOUND)).when(brandManage).delete(any());
        mockMvc.perform(delete("/api/v1/brands/" + CREATED_ID).param("version", "0"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(610102))
                .andExpect(jsonPath("$.status").value("BRAND_NOT_FOUND"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void non_positive_brand_id_is_400() throws Exception {
        mockMvc.perform(get("/api/v1/brands/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(610101))
                .andExpect(jsonPath("$.status").value("BRAND_REQUEST_INVALID"));
    }

    @Test
    void non_numeric_brand_id_is_400() throws Exception {
        mockMvc.perform(get("/api/v1/brands/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(610101))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void missing_version_parameter_is_400() throws Exception {
        mockMvc.perform(delete("/api/v1/brands/" + CREATED_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(610101));
    }

    @Test
    void inverted_seq_range_is_400() throws Exception {
        mockMvc.perform(get("/api/v1/brands").param("seqMin", "90").param("seqMax", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(610101))
                .andExpect(jsonPath("$.status").value("BRAND_REQUEST_INVALID"));
    }

    @Test
    void invalid_letter_is_400_with_field_errors() throws Exception {
        mockMvc.perform(post("/api/v1/brands").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Apple\",\"letter\":\"AB\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(610101))
                .andExpect(jsonPath("$.data.fieldErrors").isArray())
                .andExpect(jsonPath("$.data.fieldErrors[0].field").value("letter"))
                .andExpect(jsonPath("$.data.fieldErrors[0].constraint").value("Pattern"));
    }

    @Test
    void write_without_actor_is_401_via_advice() throws Exception {
        when(brandManage.create(any()))
                .thenThrow(new CommonException(GoodsErrorStatus.BRAND_OPERATOR_REQUIRED));

        mockMvc.perform(post("/api/v1/brands").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Apple\",\"letter\":\"a\",\"seq\":100}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(610105))
                .andExpect(jsonPath("$.status").value("BRAND_OPERATOR_REQUIRED"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.traceId").exists());

        mockMvc.perform(get("/api/v1/brands/" + CREATED_ID))
                .andExpect(status().isOk());
    }

    @Test
    void version_conflict_is_409() throws Exception {
        when(brandManage.update(any())).thenThrow(new CommonException(GoodsErrorStatus.BRAND_VERSION_CONFLICT));

        mockMvc.perform(put("/api/v1/brands/" + CREATED_ID).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Apple\",\"letter\":\"A\",\"seq\":1,\"version\":3}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(610104));
    }

    /**
     * {@code TenantContextFilter} is asserted against a mock servlet request rather than through the
     * slice, because what has to be observed is the MDC state <em>inside</em> the chain and the state
     * after it — a MockMvc round trip cannot see either without a probe endpoint.
     */
    @Test
    void mdc_carries_trimmed_tenant_and_operator_while_the_chain_runs() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", " 42 ");
        request.addHeader("X-Actor-Id", " alice ");

        Map<String, String> seen = new LinkedHashMap<>();
        new TenantContextFilter().doFilter(request, new MockHttpServletResponse(),
                (req, res) -> seen.put("mdc", MDC.get("tenantId") + "|" + MDC.get("userId")));

        assertThat(seen.get("mdc")).isEqualTo("42|alice");
        assertThat(MDC.get("tenantId")).isNull();
        assertThat(MDC.get("userId")).isNull();
    }

    @Test
    void mdc_falls_back_to_default_tenant_and_empty_operator() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", "   ");

        AtomicReference<String> seen = new AtomicReference<>();
        new TenantContextFilter().doFilter(request, new MockHttpServletResponse(),
                (req, res) -> seen.set(MDC.get("tenantId") + "|" + MDC.get("userId")));

        assertThat(seen.get()).isEqualTo("1|");
    }

    @Test
    void mdc_is_cleared_even_when_the_chain_fails() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Actor-Id", "alice");
        IllegalStateException failure = new IllegalStateException("downstream");

        assertThatIllegalStateException().isThrownBy(() -> new TenantContextFilter()
                .doFilter(request, new MockHttpServletResponse(), (req, res) -> {
                    throw failure;
                })).isSameAs(failure);

        assertThat(MDC.get("tenantId")).isNull();
        assertThat(MDC.get("userId")).isNull();
    }
}
