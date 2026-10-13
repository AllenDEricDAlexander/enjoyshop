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
    CATALOG_REQUEST_INVALID(610110, "request parameters are invalid"),
    PRODUCT_REQUEST_INVALID(610201, "product request parameters are invalid"),
    PRODUCT_NOT_FOUND(610202, "product does not exist or is not visible"),
    PRODUCT_VERSION_CONFLICT(610203, "product version changed"),
    PRODUCT_STATE_CONFLICT(610204, "product state or catalog does not allow this operation"),
    PRODUCT_BUSINESS_KEY_CONFLICT(610205, "product business key is occupied"),
    PRODUCT_FORBIDDEN(610206, "product permission is denied"),
    PRODUCT_ID_UNAVAILABLE(610207, "product identifier generation is unavailable"),
    PRODUCT_AUTHORIZATION_UNAVAILABLE(610208, "product authorization is unavailable"),
    PRODUCT_INTERNAL_ERROR(610299, "product internal error"),
    AD_REQUEST_INVALID(610301, "advertisement request is invalid"),
    AD_NOT_FOUND(610302, "advertisement does not exist or is not visible"),
    AD_VERSION_CONFLICT(610303, "advertisement was updated by another operation"),
    AD_DEPENDENCY_UNAVAILABLE(610304, "advertisement dependency is unavailable"),
    AD_FORBIDDEN(610305, "advertisement permission is denied"),
    AD_SITE_NOT_CONFIGURED(610306, "advertisement site is not configured"),
    AD_DATA_LIMIT(610307, "advertisement data exceeds the configured limit"),
    AD_AUTHORIZATION_UNAVAILABLE(610308, "advertisement authorization is unavailable"),
    AD_ID_UNAVAILABLE(610309, "advertisement identifier generation is unavailable"),
    AD_DATA_INVALID(610310, "advertisement data is invalid"),
    AD_INTERNAL_ERROR(610399, "advertisement internal error"),
    SEARCH_REQUEST_INVALID(610401, "search request parameters are invalid"),
    SEARCH_SITE_NOT_CONFIGURED(610402, "search site is not configured"),
    SEARCH_UNAVAILABLE(610403, "product search is unavailable"),
    SEARCH_INTERNAL_ERROR(610499, "product search internal error"),
    BRAND_FORBIDDEN(610111, "Brand permission is denied"),
    BRAND_AUTHORIZATION_UNAVAILABLE(610112, "Brand authorization is unavailable"),
    SPEC_FORBIDDEN(610113, "Spec permission is denied"),
    SPEC_AUTHORIZATION_UNAVAILABLE(610114, "Spec authorization is unavailable"),
    FILE_FORBIDDEN(610115, "File permission is denied"),
    FILE_AUTHORIZATION_UNAVAILABLE(610116, "File authorization is unavailable");

    private final int code;

    private final String message;

    @Override
    public String getStatus() {
        return name();
    }
}
