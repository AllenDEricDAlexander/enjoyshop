package top.egon.enjoyshop.goods.domain.goods;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import lombok.extern.slf4j.Slf4j;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.common.core.validation.BaseValidator;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.enjoyshop.goods.common.enums.GoodsErrorStatus;

@Slf4j
public final class ProductImageValidator extends BaseValidator {

    private static final int MAX_IMAGE_BYTES = 10 * 1024 * 1024;

    private ProductImageValidator() {
    }

    public static ProductImageFormat detectFormat(byte[] content) {
        return new ProductImageValidator().detectFormatFromValidatedContent(content);
    }

    @Override
    protected ValidationUtils getValidationUtils() {
        return JakartaValidation.UTILS;
    }

    private ProductImageFormat detectFormatFromValidatedContent(byte[] content) {
        try {
            getValidationUtils().validateValue(ProductImageBO.class, "content", content);
        } catch (ConstraintViolationException exception) {
            if (content != null && content.length > MAX_IMAGE_BYTES) {
                throw new CommonException(GoodsErrorStatus.FILE_TOO_LARGE);
            }
            throw new CommonException(GoodsErrorStatus.FILE_REQUEST_INVALID);
        }

        try (ImageInputStream input = new MemoryCacheImageInputStream(new ByteArrayInputStream(content))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new CommonException(GoodsErrorStatus.FILE_REQUEST_INVALID);
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                return switch (reader.getFormatName().toUpperCase(Locale.ROOT)) {
                    case "JPEG", "JPG" -> ProductImageFormat.JPEG;
                    case "PNG" -> ProductImageFormat.PNG;
                    default -> throw new CommonException(GoodsErrorStatus.FILE_REQUEST_INVALID);
                };
            } finally {
                reader.dispose();
            }
        } catch (IOException | IllegalArgumentException exception) {
            throw new CommonException(GoodsErrorStatus.FILE_REQUEST_INVALID);
        }
    }

    private static final class JakartaValidation {
        private static final ValidationUtils UTILS =
                new ValidationUtils(Validation.buildDefaultValidatorFactory().getValidator());
    }
}
