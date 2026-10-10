package top.egon.enjoyshop.goods.domain.ad;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AdStatusEnum {
    DISABLED("0"),
    ENABLED("1");

    @EnumValue
    @JsonValue
    private final String code;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static AdStatusEnum fromValue(JsonNode value) {
        if (value == null || !value.isTextual()) {
            throw new IllegalArgumentException("Advertisement status must be a string code");
        }
        return switch (value.textValue()) {
            case "0" -> DISABLED;
            case "1" -> ENABLED;
            default -> throw new IllegalArgumentException("Unknown advertisement status");
        };
    }
}
