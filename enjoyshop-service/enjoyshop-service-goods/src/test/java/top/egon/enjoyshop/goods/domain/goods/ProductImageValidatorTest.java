package top.egon.enjoyshop.goods.domain.goods;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.core.exception.CommonException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductImageValidatorTest {

    private static final int MAX_IMAGE_BYTES = 10 * 1024 * 1024;

    @Test
    void accepts_jpeg_and_png_from_actual_bytes() throws IOException {
        assertThat(ProductImageValidator.detectFormat(imageBytes("jpeg"))).isEqualTo(ProductImageFormat.JPEG);
        assertThat(ProductImageValidator.detectFormat(imageBytes("png"))).isEqualTo(ProductImageFormat.PNG);
    }

    @Test
    void rejects_null_empty_unknown_and_truncated_images_as_invalid_file() throws IOException {
        byte[] png = imageBytes("png");

        assertRejected(null, 610106);
        assertRejected(new byte[0], 610106);
        assertRejected(new byte[]{1, 2, 3, 4, 5, 6, 7, 8}, 610106);
        assertRejected(Arrays.copyOf(png, 4), 610106);
    }

    @Test
    void rejects_content_over_ten_mib_as_too_large() {
        assertRejected(new byte[MAX_IMAGE_BYTES + 1], 610107);
    }

    @Test
    void product_image_value_clones_constructor_builder_setter_and_accessor_bytes() throws IOException {
        byte[] source = imageBytes("png");
        byte firstByte = source[0];
        ProductImageBO image = ProductImageBO.builder().content(source).build();

        source[0] = 0;
        assertThat(image.getContent()[0]).isEqualTo(firstByte);

        byte[] accessorBytes = image.getContent();
        accessorBytes[0] = 0;
        assertThat(image.getContent()[0]).isEqualTo(firstByte);

        byte[] replacement = imageBytes("jpeg");
        byte replacementFirstByte = replacement[0];
        image.setContent(replacement);
        replacement[0] = 0;
        assertThat(image.getContent()[0]).isEqualTo(replacementFirstByte);
    }

    private static void assertRejected(byte[] content, int expectedCode) {
        assertThatThrownBy(() -> ProductImageValidator.detectFormat(content))
                .isInstanceOfSatisfying(CommonException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(expectedCode));
    }

    private static byte[] imageBytes(String format) throws IOException {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        assertThat(ImageIO.write(image, format, output)).isTrue();
        return output.toByteArray();
    }
}
