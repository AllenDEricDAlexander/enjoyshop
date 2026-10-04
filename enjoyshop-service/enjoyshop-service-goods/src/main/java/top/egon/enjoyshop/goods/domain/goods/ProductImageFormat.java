package top.egon.enjoyshop.goods.domain.goods;

import lombok.AllArgsConstructor;
import lombok.Getter;
import top.egon.cola.component.common.core.enums.EgonEnum;

@Getter
@AllArgsConstructor
public enum ProductImageFormat implements EgonEnum {

    JPEG(1, "JPEG image", "image/jpeg", "jpg"),
    PNG(2, "PNG image", "image/png", "png");

    private final int code;

    private final String message;

    private final String contentType;

    private final String extension;
}
