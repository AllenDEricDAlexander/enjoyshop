package top.egon.enjoyshop.goods.adapter.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.common.core.pojo.ResultRecord;
import top.egon.enjoyshop.goods.adapter.goods.pojo.vo.FieldErrorsVO;
import top.egon.enjoyshop.goods.adapter.goods.pojo.vo.FieldViolationVO;
import top.egon.enjoyshop.goods.common.enums.GoodsErrorStatus;

import java.util.List;

/**
 * The single error boundary for the goods routes: every failure becomes a
 * {@link ResultRecord} envelope plus a safe HTTP status (Spec §9.2 error tables, §7.3.4).
 *
 * <p>The advice is package-scoped on purpose. The archetype's sample controllers keep their own
 * status semantics, and the light archetype's original {@code GlobalExceptionHandler} was removed by
 * {@code DEC-115}, so this class is the only advice on the goods routes and cannot double-wrap or
 * lose a mapping to a competing one.</p>
 *
 * <p>Nothing from a throwable reaches the client except the stable {@link GoodsErrorStatus} code,
 * status and message; causes, SQL fragments and exception class names stay in the server log.</p>
 */
@Slf4j
@RestControllerAdvice(name = "baseExceptionHandler", basePackages = "top.egon.enjoyshop.goods.adapter.goods")
public class BaseExceptionHandler {

    /**
     * A {@link CommonException} already carries the business code, so the envelope is built from it
     * directly and only the HTTP status is derived by the code table.
     */
    @ExceptionHandler(CommonException.class)
    public ResponseEntity<ResultRecord<Void>> handleCommonException(CommonException exception, HttpServletRequest request) {
        if (searchPath(request)) {
            GoodsErrorStatus error = java.util.Arrays.stream(GoodsErrorStatus.values())
                    .filter(value -> value.getCode() == exception.getCode() && value.name().startsWith("SEARCH_"))
                    .findFirst().orElse(GoodsErrorStatus.SEARCH_INTERNAL_ERROR);
            return ResponseEntity.status(httpStatus(error.getCode())).body(ResultRecord.failure(error));
        }
        if (!productPath(request)) {
            if (!adPath(request)) { return handleCommonException(exception); }
            if (exception.getCode() == 401000) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ResultRecord.failure(exception));
            }
            GoodsErrorStatus error = mapBusinessStatus(exception.getCode());
            return adError(error, httpStatus(error.getCode()));
        }
        if (idFailure(exception)) { return productError(GoodsErrorStatus.PRODUCT_ID_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE); }
        if (exception.getCode() == 401000) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ResultRecord.failure(exception));
        }
        GoodsErrorStatus error = java.util.Arrays.stream(GoodsErrorStatus.values())
                .filter(value -> value.getCode() == exception.getCode() && value.name().startsWith("PRODUCT_"))
                .findFirst().orElse(GoodsErrorStatus.PRODUCT_INTERNAL_ERROR);
        return productError(error, httpStatus(error.getCode()));
    }

    /** Kept for existing programmatic callers with the original goods semantics. */
    public ResponseEntity<ResultRecord<Void>> handleCommonException(CommonException exception) {
        HttpStatusCode status = httpStatus(exception.getCode());
        if (HttpStatus.INTERNAL_SERVER_ERROR.equals(status)) {
            log.error("goods failure with an unmapped business code code={} status={}",
                    exception.getCode(), exception.getStatus(), exception);
        } else {
            log.warn("goods business rejection code={} status={} http={}",
                    exception.getCode(), exception.getStatus(), status.value());
        }
        return ResponseEntity.status(status).body(ResultRecord.failure(exception));
    }

    /**
     * Bean Validation on a request body or a model attribute: the offending fields go into
     * {@code data.fieldErrors} so the envelope keys themselves never change.
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ResultRecord<FieldErrorsVO>> handleBindFailure(
            BindException exception, HttpServletRequest request) {
        List<FieldViolationVO> violations = exception.getBindingResult().getFieldErrors().stream()
                .map(BaseExceptionHandler::toViolation)
                .toList();
        log.warn("goods request rejected with {} field violation(s)", violations.size());
        return fieldErrorsResponse(violations, requestErrorStatus(request));
    }

    /**
     * Method-level constraint failures raised through {@code @Validated} on a handler parameter,
     * including the cross-field range check the controller raises itself.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ResultRecord<FieldErrorsVO>> handleConstraintViolation(
            ConstraintViolationException exception, HttpServletRequest request) {
        List<ConstraintViolation<?>> violations = exception.getConstraintViolations() == null
                ? List.of()
                : List.copyOf(exception.getConstraintViolations());
        log.warn("goods request rejected with {} constraint violation(s)", violations.size());
        return fieldErrorsResponse(violations.stream().map(value -> productPath(request) || searchPath(request) ? toProductViolation(value) : toViolation(value)).toList(),
                requestErrorStatus(request));
    }

    /**
     * Structural request defects: a missing query parameter, an identifier that is not a number, an
     * unreadable body or an argument the handler rejects. No detail of the cause is echoed.
     */
    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class, IllegalArgumentException.class})
    public ResponseEntity<ResultRecord<Void>> handleBadRequest(Exception exception, HttpServletRequest request) {
        log.warn("goods request is not processable: {}", exception.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ResultRecord.failure(requestErrorStatus(request)));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ResultRecord<Void>> handleUnsupportedMedia(
            HttpMediaTypeNotSupportedException exception, HttpServletRequest request) {
        if (adPath(request)) {
            return adError(GoodsErrorStatus.AD_REQUEST_INVALID, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        }
        return handleUnexpectedException(exception, request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ResultRecord<Void>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException exception) {
        log.warn("file upload rejected because the request exceeds the configured size limit");
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ResultRecord.failure(GoodsErrorStatus.FILE_TOO_LARGE));
    }

    /**
     * The unique-index race backstop: the pre-check in the application layer answers the common case
     * with 610103, and a concurrent insert that slips past it lands here with the same payload.
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ResultRecord<Void>> handleUniqueConflict(DuplicateKeyException exception, HttpServletRequest request) {
        if (productPath(request)) { return productError(GoodsErrorStatus.PRODUCT_BUSINESS_KEY_CONFLICT, HttpStatus.CONFLICT); }
        if (adPath(request)) { return adError(GoodsErrorStatus.AD_VERSION_CONFLICT, HttpStatus.CONFLICT); }
        log.warn("goods unique conflict on concurrent create");
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ResultRecord.failure(GoodsErrorStatus.BRAND_NAME_DUPLICATED));
    }

    /**
     * Anything unmapped is a server-side defect: log the throwable with its stack, answer only the
     * 610199 envelope.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResultRecord<Void>> handleUnexpectedException(Exception exception, HttpServletRequest request) {
        if (searchPath(request)) {
            log.error("unexpected product search failure", exception);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResultRecord.failure(GoodsErrorStatus.SEARCH_INTERNAL_ERROR));
        }
        if (productPath(request)) {
            if (idFailure(exception)) { return productError(GoodsErrorStatus.PRODUCT_ID_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE); }
            if (exception instanceof org.springframework.security.access.AccessDeniedException
                    || exception instanceof top.egon.cola.platform.tianquan.jianshen.starter.authorization.DefaultAuthorizationService.AuthorizationDeniedException
                    || exception instanceof top.egon.cola.platform.tianquan.jianshen.starter.client.Rbac3AuthorizationClient.AuthorizationDeniedException) {
                return productError(GoodsErrorStatus.PRODUCT_FORBIDDEN, HttpStatus.FORBIDDEN);
            }
            if (exception instanceof top.egon.cola.platform.tianquan.jianshen.starter.authorization.AuthorizationService.RuntimeUnavailableException
                    || exception instanceof top.egon.cola.platform.tianquan.jianshen.starter.client.Rbac3AuthorizationClient.AuthorizationUnavailableException) {
                return productError(GoodsErrorStatus.PRODUCT_AUTHORIZATION_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE);
            }
            log.error("unexpected product failure", exception);
            return productError(GoodsErrorStatus.PRODUCT_INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        if (adPath(request)) {
            if (idFailure(exception)) { return adError(GoodsErrorStatus.AD_ID_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE); }
            if (exception instanceof org.springframework.security.access.AccessDeniedException
                    || exception instanceof top.egon.cola.platform.tianquan.jianshen.starter.authorization.DefaultAuthorizationService.AuthorizationDeniedException
                    || exception instanceof top.egon.cola.platform.tianquan.jianshen.starter.client.Rbac3AuthorizationClient.AuthorizationDeniedException) {
                return adError(GoodsErrorStatus.AD_FORBIDDEN, HttpStatus.FORBIDDEN);
            }
            if (exception instanceof top.egon.cola.platform.tianquan.jianshen.starter.authorization.AuthorizationService.RuntimeUnavailableException
                    || exception instanceof top.egon.cola.platform.tianquan.jianshen.starter.client.Rbac3AuthorizationClient.AuthorizationUnavailableException) {
                return adError(GoodsErrorStatus.AD_AUTHORIZATION_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE);
            }
            if (exception instanceof DataAccessException) {
                return adError(GoodsErrorStatus.AD_DEPENDENCY_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE);
            }
            log.error("unexpected advertisement failure", exception);
            return adError(GoodsErrorStatus.AD_INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        log.error("unexpected goods failure", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResultRecord.failure(GoodsErrorStatus.BRAND_INTERNAL_ERROR));
    }

    /**
     * The one code-to-status table of {@code REQ-019}: 610101 bad request, 610102 not found,
     * 610103/610104 conflict, 610105 unauthorized, everything else internal error.
     */
    private static HttpStatusCode httpStatus(int code) {
        return switch (code) {
            case 610101, 610106, 610110, 610201, 610401 -> HttpStatus.BAD_REQUEST;
            case 610102, 610202, 610302, 610306, 610402 -> HttpStatus.NOT_FOUND;
            case 610103, 610104, 610203, 610204, 610205, 610303 -> HttpStatus.CONFLICT;
            case 610105 -> HttpStatus.UNAUTHORIZED;
            case 610206, 610305 -> HttpStatus.FORBIDDEN;
            case 610107 -> HttpStatus.PAYLOAD_TOO_LARGE;
            case 610108, 610109, 610207, 610208, 610304, 610307, 610308, 610309, 610310, 610403 -> HttpStatus.SERVICE_UNAVAILABLE;
            case 610301 -> HttpStatus.BAD_REQUEST;
            case 610399, 610499 -> HttpStatus.INTERNAL_SERVER_ERROR;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    private static ResponseEntity<ResultRecord<FieldErrorsVO>> fieldErrorsResponse(
            List<FieldViolationVO> violations, GoodsErrorStatus status) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ResultRecord.result(
                status, false, new FieldErrorsVO(violations)));
    }

    private static GoodsErrorStatus requestErrorStatus(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String path = contextPath.isEmpty() ? requestUri : requestUri.substring(contextPath.length());
        if (searchPath(request)) { return GoodsErrorStatus.SEARCH_REQUEST_INVALID; }
        if (productPath(request)) { return GoodsErrorStatus.PRODUCT_REQUEST_INVALID; }
        if (adPath(request)) { return GoodsErrorStatus.AD_REQUEST_INVALID; }
        if (path.equals("/api/v1/specs") || path.startsWith("/api/v1/specs/")) {
            return GoodsErrorStatus.CATALOG_REQUEST_INVALID;
        }
        if (path.equals("/api/v1/files") || path.startsWith("/api/v1/files/")) {
            return GoodsErrorStatus.FILE_REQUEST_INVALID;
        }
        return GoodsErrorStatus.BRAND_REQUEST_INVALID;
    }

    private static FieldViolationVO toViolation(FieldError error) {
        return new FieldViolationVO(error.getField(), constraintName(error), error.getDefaultMessage());
    }

    /**
     * {@code ObjectError#getCodes()} is ordered from the most qualified key down to the raw error
     * code, and for a Bean Validation failure that raw code is the constraint annotation's simple
     * name ({@code NotBlank}, {@code Pattern}, {@code Size}, ...). Spring exposes no
     * {@code getConstraint()} accessor on {@link FieldError}, so this is the documented way to name
     * the violated constraint; a type-conversion failure yields {@code typeMismatch} instead.
     */
    private static String constraintName(FieldError error) {
        String[] codes = error.getCodes();
        if (codes == null || codes.length == 0) {
            return error.getCode();
        }
        return codes[codes.length - 1];
    }

    private static FieldViolationVO toViolation(ConstraintViolation<?> violation) {
        // A parameter-level violation is reported as "<method>.<parameter>" and a nested one as
        // "<method>.<object>.<property>"; only the leaf is a client-facing field name.
        String propertyPath = violation.getPropertyPath().toString();
        String leaf = StringUtils.substringAfterLast(propertyPath, ".");
        return new FieldViolationVO(StringUtils.isBlank(leaf) ? propertyPath : leaf,
                violation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName(),
                violation.getMessage());
    }
    private static boolean searchPath(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return path.equals("/api/v1/product-search") || path.startsWith("/api/v1/product-search/");
    }

    private static boolean productPath(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return path.equals("/api/v1/products") || path.startsWith("/api/v1/products/");
    }

    private static boolean adPath(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return path.equals("/api/v1/ads") || path.startsWith("/api/v1/ads/")
                || path.equals("/api/v1/ad-slots") || path.startsWith("/api/v1/ad-slots/");
    }

    private static GoodsErrorStatus mapBusinessStatus(int code) {
        return java.util.Arrays.stream(GoodsErrorStatus.values())
                .filter(value -> value.getCode() == code && value.name().startsWith("AD_"))
                .findFirst()
                .orElse(GoodsErrorStatus.AD_INTERNAL_ERROR);
    }

    private static ResponseEntity<ResultRecord<Void>> adError(GoodsErrorStatus error, HttpStatusCode status) {
        return ResponseEntity.status(status).body(ResultRecord.failure(error));
    }

    private static ResponseEntity<ResultRecord<Void>> productError(GoodsErrorStatus error, HttpStatusCode status) {
        return ResponseEntity.status(status).body(ResultRecord.failure(error));
    }

    private static boolean idFailure(Throwable failure) {
        for (int depth = 0; failure != null && depth < 16; depth++, failure = failure.getCause()) {
            if (failure instanceof top.egon.cola.component.common.id.exception.ClockMovedBackwardException
                    || failure instanceof top.egon.cola.component.common.id.exception.IdGenerationInterruptedException
                    || failure instanceof top.egon.cola.component.common.id.exception.SnowflakeTimestampOutOfRangeException) { return true; }
        }
        return false;
    }

    private static FieldViolationVO toProductViolation(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        // Native method validation prefixes method/parameter; keep the entire nested field suffix.
        String[] prefix = path.split("\\.", 3);
        if (prefix.length == 3) { path = prefix[2]; }
        else if (prefix.length == 2) { path = prefix[1]; }
        return new FieldViolationVO(path,
                violation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName(), violation.getMessage());
    }

}
