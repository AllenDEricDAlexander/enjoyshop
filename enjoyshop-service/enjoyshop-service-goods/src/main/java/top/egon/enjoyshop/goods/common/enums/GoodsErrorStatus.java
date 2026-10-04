package top.egon.enjoyshop.goods.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import top.egon.cola.component.common.core.enums.ErrorStatus;

/**
 * Stable goods business codes; codes are appended only and never renumbered.
 */
@Getter
@AllArgsConstructor
public enum GoodsErrorStatus implements ErrorStatus {

    BRAND_REQUEST_INVALID(610101, "request parameters are invalid"),
    BRAND_NOT_FOUND(610102, "brand does not exist or is not visible to current tenant"),
    BRAND_NAME_DUPLICATED(610103, "brand name already exists in current tenant"),
    BRAND_VERSION_CONFLICT(610104, "brand was updated by another operation"),
    BRAND_OPERATOR_REQUIRED(610105, "identifiable operator is required for this operation"),
    BRAND_INTERNAL_ERROR(610199, "brand internal error"),
    FILE_REQUEST_INVALID(610106, "file request is invalid"),
    FILE_TOO_LARGE(610107, "file exceeds the upload size limit"),
    FILE_STORAGE_UNAVAILABLE(610108, "file storage is unavailable"),
    FILE_STORAGE_OUTCOME_UNKNOWN(610109, "file storage outcome is unknown"),
    CATALOG_REQUEST_INVALID(610110, "request parameters are invalid");

    private final int code;

    private final String message;

    @Override
    public String getStatus() {
        return name();
    }
}
