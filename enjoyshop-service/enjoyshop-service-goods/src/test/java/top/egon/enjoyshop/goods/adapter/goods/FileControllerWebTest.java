package top.egon.enjoyshop.goods.adapter.goods;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.enjoyshop.goods.application.goods.FileManage;
import top.egon.enjoyshop.goods.application.goods.FileUploadCommand;
import top.egon.enjoyshop.goods.common.enums.GoodsErrorStatus;
import top.egon.enjoyshop.goods.start.StudentManagementApplication;

import javax.imageio.ImageIO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest(classes = StudentManagementApplication.class)
class FileControllerWebTest extends top.egon.enjoyshop.goods.support.PersistenceTestSupport {

    private static final String PUBLIC_URL = "https://files.example.test/product-media/opaque-key.png";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FileManage fileManage;

    @BeforeEach
    void resetCollaborator() {
        reset(fileManage);
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
    void authenticated_upload_returns_201_location_and_url_envelope() throws Exception {
        byte[] image = imageBytes("png");
        when(fileManage.upload(any())).thenReturn(PUBLIC_URL);

        mockMvc.perform(multipart("/api/v1/files")
                        .file(new MockMultipartFile("file", "caller-name.jpg", "text/plain", image))
                        .session(authenticatedSession()))
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
        mockMvc.perform(multipart("/api/v1/files").session(authenticatedSession()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(610106))
                .andExpect(jsonPath("$.status").value("FILE_REQUEST_INVALID"));

        verifyNoInteractions(fileManage);
    }

    @Test
    void over_ten_mib_is_413_before_manage() throws Exception {
        MockMultipartFile oversized = new MockMultipartFile("file", "large.png", "image/png",
                new byte[10 * 1024 * 1024 + 1]);

        mockMvc.perform(multipart("/api/v1/files").file(oversized).session(authenticatedSession()))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code").value(610107))
                .andExpect(jsonPath("$.status").value("FILE_TOO_LARGE"));

        verifyNoInteractions(fileManage);
    }

    @Test
    void storage_errors_remain_goods_result_records() throws Exception {
        when(fileManage.upload(any())).thenThrow(new CommonException(GoodsErrorStatus.FILE_STORAGE_UNAVAILABLE));

        mockMvc.perform(multipart("/api/v1/files").file(imagePart()).session(authenticatedSession()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value(610108))
                .andExpect(jsonPath("$.data").doesNotExist());

        reset(fileManage);
        when(fileManage.upload(any())).thenThrow(new CommonException(GoodsErrorStatus.FILE_STORAGE_OUTCOME_UNKNOWN));

        mockMvc.perform(multipart("/api/v1/files").file(imagePart()).session(authenticatedSession()))
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

    private static MockHttpSession authenticatedSession() {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated("test-user", "unused", List.of()));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        return session;
    }
}
