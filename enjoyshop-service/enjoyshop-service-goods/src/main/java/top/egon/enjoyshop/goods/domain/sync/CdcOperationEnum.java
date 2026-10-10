package top.egon.enjoyshop.goods.domain.sync;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** PostgreSQL row operations that can produce a synchronization request. */
@Getter
@AllArgsConstructor
public enum CdcOperationEnum {
    CREATE("c"),
    UPDATE("u"),
    DELETE("d"),
    SNAPSHOT("r");

    private final String code;

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static CdcOperationEnum fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("CDC operation is required");
        }
        for (CdcOperationEnum operation : values()) {
            if (operation.code.equals(code)) {
                return operation;
            }
        }
        throw new IllegalArgumentException("Unknown CDC row operation");
    }
}
