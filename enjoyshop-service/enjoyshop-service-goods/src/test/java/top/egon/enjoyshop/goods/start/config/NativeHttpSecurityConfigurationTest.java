package top.egon.enjoyshop.goods.start.config;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import top.egon.cola.component.common.core.pojo.PageResultRecord;
import top.egon.enjoyshop.goods.application.goods.BrandManage;
import top.egon.enjoyshop.goods.start.StudentManagementApplication;

import static org.hamcrest.Matchers.containsString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest(classes = StudentManagementApplication.class)
class NativeHttpSecurityConfigurationTest extends top.egon.enjoyshop.goods.support.PersistenceTestSupport {

    private static final String ALLOWED_ORIGIN = "http://localhost:9100";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationContext applicationContext;

    @MockitoBean(name = "brandManage")
    private BrandManage brandManage;

    @BeforeEach
    void resetBrandManage() {
        reset(brandManage);
        when(brandManage.listBrands(any(), isNull(), isNull()))
                .thenReturn(PageResultRecord.success(List.of(), 0L, 1, 500));
    }

    @Test
    void allowed_origin_preflight_accepts_upload_headers_and_method() throws Exception {
        mockMvc.perform(options("/api/v1/files")
                        .header("Origin", ALLOWED_ORIGIN)
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Authorization,Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", ALLOWED_ORIGIN))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("POST")))
                .andExpect(header().string("Access-Control-Allow-Headers", containsString("Authorization")));
    }

    @Test
    void rejected_origin_has_no_allow_origin_header() throws Exception {
        mockMvc.perform(options("/api/v1/files")
                        .header("Origin", "https://denied.example.test")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Authorization,Content-Type"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void anonymous_upload_is_401_but_an_authenticated_session_reaches_mvc() throws Exception {
        mockMvc.perform(post("/api/v1/files"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHORIZATION_HEADER_INVALID"))
                .andExpect(jsonPath("$.message").value("Tianquan-Shoubing authentication failed"))
                .andExpect(jsonPath("$.success").doesNotExist());

        mockMvc.perform(multipart("/api/v1/files").session(authenticatedSession()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(610106));
    }

    @Test
    void authenticated_brand_get_exposes_location_for_allowed_origin() throws Exception {
        mockMvc.perform(get("/api/v1/brands").header("Origin", ALLOWED_ORIGIN))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHORIZATION_HEADER_INVALID"))
                .andExpect(header().string("Access-Control-Allow-Origin", ALLOWED_ORIGIN))
                .andExpect(header().string("Access-Control-Expose-Headers", containsString("Location")));

        mockMvc.perform(get("/api/v1/brands")
                        .header("Origin", ALLOWED_ORIGIN)
                        .header("Authorization", "Bearer product-read"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", ALLOWED_ORIGIN))
                .andExpect(header().string("Access-Control-Expose-Headers", containsString("Location")));

        mockMvc.perform(get("/api/v1/brands").header("Authorization", "Bearer product-read"))
                .andExpect(status().isOk());
    }

    @Test
    void spring_manages_the_s3_client_without_connecting_to_the_endpoint() {
        assertThat(applicationContext.containsBean("goodsS3Client")).isTrue();
    }

    private static MockHttpSession authenticatedSession() {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated("test-user", "unused", List.of()));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        return session;
    }
}
