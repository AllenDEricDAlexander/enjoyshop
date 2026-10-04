package top.egon.enjoyshop.goods.infrastructure.goods.service.impl;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.enjoyshop.goods.domain.goods.ObjectStoragePort;
import top.egon.enjoyshop.goods.domain.goods.ProductImageBO;
import top.egon.enjoyshop.goods.domain.goods.ProductImageFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileDomainServiceImplTest {

    private static final String STABLE_URL = "https://images.example.test/product-media/opaque-key.jpg";

    @Mock
    private ObjectStoragePort objectStoragePort;

    @InjectMocks
    private FileDomainServiceImpl fileDomainService;

    @Test
    void invalid_image_fails_before_the_storage_port() {
        ProductImageBO image = ProductImageBO.builder().content(new byte[]{1, 2, 3, 4}).build();

        assertThatThrownBy(() -> fileDomainService.upload(image))
                .isInstanceOfSatisfying(CommonException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(610106));

        verifyNoInteractions(objectStoragePort);
    }

    @Test
    void image_over_ten_mib_uses_the_too_large_error_before_the_storage_port() {
        ProductImageBO image = ProductImageBO.builder().content(new byte[10 * 1024 * 1024 + 1]).build();

        assertThatThrownBy(() -> fileDomainService.upload(image))
                .isInstanceOfSatisfying(CommonException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(610107));

        verifyNoInteractions(objectStoragePort);
    }

    @Test
    void valid_jpeg_is_detected_and_delegated_once_to_the_domain_port() throws IOException {
        byte[] bytes = jpegBytes();
        when(objectStoragePort.putObject(any(byte[].class), eq(ProductImageFormat.JPEG))).thenReturn(STABLE_URL);

        assertThat(fileDomainService.upload(ProductImageBO.builder().content(bytes).build())).isEqualTo(STABLE_URL);

        ArgumentCaptor<byte[]> content = ArgumentCaptor.forClass(byte[].class);
        verify(objectStoragePort).putObject(content.capture(), eq(ProductImageFormat.JPEG));
        assertThat(content.getValue()).containsExactly(bytes);
    }

    private static byte[] jpegBytes() throws IOException {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        assertThat(ImageIO.write(image, "jpeg", output)).isTrue();
        return output.toByteArray();
    }
}
