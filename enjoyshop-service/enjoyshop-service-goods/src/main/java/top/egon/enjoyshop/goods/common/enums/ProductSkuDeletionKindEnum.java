package top.egon.enjoyshop.goods.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ProductSkuDeletionKindEnum {
    PRODUCT("P"), EDIT("E");

    @EnumValue
    @JsonValue
    private final String code;

    @JsonCreator
    public static ProductSkuDeletionKindEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ProductSkuDeletionKindEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown SKU deletion kind");
    }
}
