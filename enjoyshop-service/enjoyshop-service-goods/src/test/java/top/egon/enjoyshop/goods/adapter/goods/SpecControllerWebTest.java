package top.egon.enjoyshop.goods.adapter.goods;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import top.egon.enjoyshop.goods.application.goods.SpecListQuery;
import top.egon.enjoyshop.goods.application.goods.SpecManage;
import top.egon.enjoyshop.goods.application.goods.SpecResult;
import top.egon.enjoyshop.goods.start.StudentManagementApplication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest(classes = StudentManagementApplication.class)
class SpecControllerWebTest extends top.egon.enjoyshop.goods.support.PersistenceTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SpecManage specManage;

    @BeforeEach
    void resetCollaborator() {
        reset(specManage);
    }

    @Test
    void category_lookup_returns_ordered_option_arrays() throws Exception {
        when(specManage.listSpecsByCategoryName(any()))
                .thenReturn(List.of(new SpecResult("Size", List.of("S", "M", "L"))));

        mockMvc.perform(get("/api/v1/specs/category/phones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(10000))
                .andExpect(jsonPath("$.data[0].name").value("Size"))
                .andExpect(jsonPath("$.data[0].options").isArray())
                .andExpect(jsonPath("$.data[0].options[0]").value("S"))
                .andExpect(jsonPath("$.data[0].options[2]").value("L"));

        ArgumentCaptor<SpecListQuery> captor = ArgumentCaptor.forClass(SpecListQuery.class);
        verify(specManage).listSpecsByCategoryName(captor.capture());
        assertThat(captor.getValue().getCategoryName()).isEqualTo("phones");
    }

    @Test
    void category_without_specs_returns_empty_array() throws Exception {
        when(specManage.listSpecsByCategoryName(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/specs/category/phones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void blank_category_name_is_rejected_before_manage() throws Exception {
        mockMvc.perform(get("/api/v1/specs/category/{categoryName}", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(610110))
                .andExpect(jsonPath("$.status").value("CATALOG_REQUEST_INVALID"));

        verifyNoInteractions(specManage);
    }

    @Test
    void overlong_category_name_is_rejected_before_manage() throws Exception {
        mockMvc.perform(get("/api/v1/specs/category/" + "x".repeat(121)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(610110))
                .andExpect(jsonPath("$.status").value("CATALOG_REQUEST_INVALID"));

        verifyNoInteractions(specManage);
    }
}
