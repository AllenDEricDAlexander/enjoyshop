package top.egon.enjoyshop.goods.adapter.handler;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.dao.DuplicateKeyException;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.enjoyshop.goods.adapter.filter.TraceIdFilter;
import top.egon.enjoyshop.goods.adapter.goods.BrandController;
import top.egon.enjoyshop.goods.adapter.goods.pojo.convertor.BrandAdapterConverterImpl;
import top.egon.enjoyshop.goods.application.goods.BrandManage;
import top.egon.enjoyshop.goods.common.enums.GoodsErrorStatus;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.isOneOf;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exception-to-envelope mapping gate (Spec §9.2 error tables, §7.3.4, {@code REQ-019}).
 *
 * <p>The slice registers {@link BaseExceptionHandler} explicitly, mirroring how every other adapter
 * slice test in this module wires its own bean set; the mocked {@link BrandManage} is the only way
 * to make the controlled throw each failure branch.</p>
 */
@WebMvcTest(controllers = BrandController.class)
@ContextConfiguration(classes = {
        top.egon.enjoyshop.goods.start.config.NativeHttpSecurityConfiguration.class,
        BrandController.class,
        BrandAdapterConverterImpl.class,
        BaseExceptionHandler.class,
        TraceIdFilter.class
})
class BaseExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * {@link BrandManage} is injected by the controller as {@code @Qualifier("brandManage")}, so the
     * mock must be registered under that exact bean name; a by-type mock would get a generated name
     * and the qualifier would resolve to nothing.
     */
    @MockitoBean(name = "brandManage")
    private BrandManage brandManage;

    @Test
    void common_exception_maps_to_envelope_with_http_status() throws Exception {
        when(brandManage.detail(any())).thenThrow(new CommonException(GoodsErrorStatus.BRAND_VERSION_CONFLICT));

        mockMvc.perform(get("/api/v1/brands/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(610104))
                .andExpect(jsonPath("$.status").value("BRAND_VERSION_CONFLICT"))
                .andExpect(jsonPath("$.message").value("brand was updated by another operation"))
                .andExpect(jsonPath("$.data").doesNotExist())
                // traceId comes from MDC: proves the envelope is built inside the TraceIdFilter chain
                .andExpect(jsonPath("$.traceId").isString())
                .andExpect(jsonPath("$.timestamp").isNumber());
    }

    @Test
    void validation_failure_carries_field_errors() throws Exception {
        mockMvc.perform(post("/api/v1/brands")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"  \",\"letter\":\"AB\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(610101))
                .andExpect(jsonPath("$.status").value("BRAND_REQUEST_INVALID"))
                .andExpect(jsonPath("$.data.fieldErrors").isArray())
                .andExpect(jsonPath("$.data.fieldErrors[0].field").exists())
                .andExpect(jsonPath("$.data.fieldErrors[*].constraint",
                        everyItem(isOneOf("NotNull", "NotBlank", "Size", "Pattern", "Min", "Max",
                                "Positive", "PositiveOrZero", "typeMismatch"))))
                .andExpect(jsonPath("$.data.fieldErrors[0].message").exists());
    }

    @Test
    void duplicate_key_maps_to_409_610103() throws Exception {
        when(brandManage.detail(any())).thenThrow(new DuplicateKeyException("duplicate key value"));

        mockMvc.perform(get("/api/v1/brands/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(610103))
                .andExpect(jsonPath("$.status").value("BRAND_NAME_DUPLICATED"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void unexpected_exception_is_sanitized_to_500() throws Exception {
        when(brandManage.detail(any()))
                .thenThrow(new IllegalStateException("jdbc:postgresql://internal-db:5432/primary"));

        mockMvc.perform(get("/api/v1/brands/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(610199))
                .andExpect(jsonPath("$.status").value("BRAND_INTERNAL_ERROR"))
                .andExpect(content().string(not(containsString("jdbc:postgresql"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))))
                .andExpect(content().string(not(containsString("internal-db"))))
                .andExpect(content().string(not(containsString("java.lang"))));
    }
}
