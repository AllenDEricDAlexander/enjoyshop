package top.egon.enjoyshop.goods.adapter.goods;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.common.core.pojo.PageResultRecord;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpBearerAuthenticationFilter;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpJwtVerifier;
import top.egon.enjoyshop.goods.application.goods.BrandManage;
import top.egon.enjoyshop.goods.application.goods.BrandResult;
import top.egon.enjoyshop.goods.application.goods.FileManage;
import top.egon.enjoyshop.goods.application.goods.FileUploadCommand;
import top.egon.enjoyshop.goods.common.enums.GoodsErrorStatus;
import top.egon.enjoyshop.goods.start.StudentManagementApplication;

import javax.imageio.ImageIO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest(classes = StudentManagementApplication.class)
@Import(FileControllerWebTest.ShoubingFilterFixture.class)
class FileControllerWebTest extends top.egon.enjoyshop.goods.support.PersistenceTestSupport {

    private static final String PUBLIC_URL = "https://files.example.test/product-media/opaque-key.png";
    private static final String USER_TOKEN = "verified-test-user-token";

    @TestConfiguration(proxyBeanMethods = false)
    static class ShoubingFilterFixture {

        @Bean("idpBearerAuthenticationFilter")
        IdpBearerAuthenticationFilter fileUploadShoubingFilterFixture(
                @Qualifier("jacksonObjectMapper") ObjectMapper objectMapper) {
            IdpJwtVerifier verifier = new IdpJwtVerifier(token -> {
                if (!USER_TOKEN.equals(token)) {
                    throw new BadJwtException("invalid test bearer");
                }
                return userJwt(token);
            }, "platform", Clock.fixed(
                    Instant.parse("2026-10-05T00:00:00Z"), ZoneOffset.UTC));
            return new IdpBearerAuthenticationFilter(verifier, objectMapper);
        }

        @Bean
        FilterRegistrationBean<IdpBearerAuthenticationFilter> disabledServletRegistration(
                @Qualifier("idpBearerAuthenticationFilter") IdpBearerAuthenticationFilter filter) {
            FilterRegistrationBean<IdpBearerAuthenticationFilter> registration =
                    new FilterRegistrationBean<>(filter);
            registration.setEnabled(false);
            return registration;
        }

        private static Jwt userJwt(String token) {
            Instant now = Instant.parse("2026-10-05T00:00:00Z");
            return Jwt.withTokenValue(token)
                    .header("alg", "RS256")
                    .header("kid", "test-key")
                    .header("typ", "at+jwt")
                    .issuer("https://identity.example.test")
                    .subject("test-user")
                    .audience(List.of("platform"))
                    .issuedAt(now)
                    .notBefore(now)
                    .expiresAt(now.plusSeconds(300))
                    .claim("principal_type", "USER")
                    .claim("tid", "test-tenant")
                    .claim("jti", "test-token-id")
                    .claim("acr", "PASSWORD")
                    .claim("auth_time", now)
                    .build();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FileManage fileManage;

    @MockitoBean(name = "brandManage")
    private BrandManage brandManage;

    @BeforeEach
    void resetCollaborator() {
        reset(fileManage);
        reset(brandManage);
        when(brandManage.listBrands(any(), isNull(), isNull()))
                .thenReturn(PageResultRecord.success(List.<BrandResult>of(), 0L, 1, 500));
    }

    @Test
    void missing_bearer_is_rejected_with_the_shoubing_error_shape() throws Exception {
        mockMvc.perform(multipart("/api/v1/files").file(imagePart()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHORIZATION_HEADER_INVALID"))
                .andExpect(jsonPath("$.message").value("Tianquan-Shoubing authentication failed"))
                .andExpect(jsonPath("$.success").doesNotExist());

        verifyNoInteractions(fileManage);
    }

    @Test
    void invalid_bearer_is_rejected_by_the_shoubing_filter() throws Exception {
        mockMvc.perform(multipart("/api/v1/files")
                        .file(imagePart())
                        .header("Authorization", "Bearer invalid-test-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("JWT_INVALID"))
                .andExpect(jsonPath("$.message").value("Tianquan-Shoubing authentication failed"))
                .andExpect(jsonPath("$.success").doesNotExist());

        verifyNoInteractions(fileManage);
    }

    @Test
    void public_brand_route_does_not_use_the_upload_bearer_filter() throws Exception {
        mockMvc.perform(get("/api/v1/brands").header("Authorization", "Bearer invalid-test-token"))
                .andExpect(status().isOk());
    }

    @Test
    void authenticated_upload_returns_201_location_and_url_envelope() throws Exception {
        byte[] image = imageBytes("png");
        when(fileManage.upload(any())).thenReturn(PUBLIC_URL);

        mockMvc.perform(multipart("/api/v1/files")
                        .file(new MockMultipartFile("file", "caller-name.jpg", "text/plain", image))
                        .header("Authorization", "Bearer " + USER_TOKEN))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", PUBLIC_URL))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(10000))
                .andExpect(jsonPath("$.data").value(PUBLIC_URL));

        ArgumentCaptor<FileUploadCommand> captor = ArgumentCaptor.forClass(FileUploadCommand.class);
        verify(fileManage).upload(captor.capture());
        assertThat(captor.getValue().getContent()).containsExactly(image);
    }

    @Test
    void missing_file_is_a_file_request_error_before_manage() throws Exception {
        mockMvc.perform(multipart("/api/v1/files").header("Authorization", "Bearer " + USER_TOKEN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(610106))
                .andExpect(jsonPath("$.status").value("FILE_REQUEST_INVALID"));

        verifyNoInteractions(fileManage);
    }

    @Test
    void over_ten_mib_is_413_before_manage() throws Exception {
        MockMultipartFile oversized = new MockMultipartFile("file", "large.png", "image/png",
                new byte[10 * 1024 * 1024 + 1]);

        mockMvc.perform(multipart("/api/v1/files").file(oversized)
                        .header("Authorization", "Bearer " + USER_TOKEN))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code").value(610107))
                .andExpect(jsonPath("$.status").value("FILE_TOO_LARGE"));

        verifyNoInteractions(fileManage);
    }

    @Test
    void storage_errors_remain_goods_result_records() throws Exception {
        when(fileManage.upload(any())).thenThrow(new CommonException(GoodsErrorStatus.FILE_STORAGE_UNAVAILABLE));

        mockMvc.perform(multipart("/api/v1/files").file(imagePart())
                        .header("Authorization", "Bearer " + USER_TOKEN))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value(610108))
                .andExpect(jsonPath("$.data").doesNotExist());

        reset(fileManage);
        when(fileManage.upload(any())).thenThrow(new CommonException(GoodsErrorStatus.FILE_STORAGE_OUTCOME_UNKNOWN));

        mockMvc.perform(multipart("/api/v1/files").file(imagePart())
                        .header("Authorization", "Bearer " + USER_TOKEN))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value(610109))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    private static MockMultipartFile imagePart() throws IOException {
        return new MockMultipartFile("file", "image.png", "image/png", imageBytes("png"));
    }

    private static byte[] imageBytes(String format) throws IOException {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        assertThat(ImageIO.write(image, format, output)).isTrue();
        return output.toByteArray();
    }

}
