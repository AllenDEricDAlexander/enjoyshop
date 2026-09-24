package top.egon.enjoyshop.goods.adapter.handler;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.common.core.pojo.ResultRecord;
import top.egon.enjoyshop.goods.adapter.goods.pojo.vo.FieldErrorsVO;
import top.egon.enjoyshop.goods.adapter.goods.pojo.vo.FieldViolationVO;
import top.egon.enjoyshop.goods.common.enums.GoodsErrorStatus;

import java.util.List;

/**
 * The single error boundary for the brand routes: every failure becomes a
 * {@link ResultRecord} envelope plus a safe HTTP status (Spec §9.2 error tables, §7.3.4).
 *
 * <p>The advice is package-scoped on purpose. The archetype's sample controllers keep their own
 * status semantics, and the light archetype's original {@code GlobalExceptionHandler} was removed by
 * {@code DEC-115}, so this class is the only advice on the brand chain and cannot double-wrap or
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
    public ResponseEntity<ResultRecord<Void>> handleCommonException(CommonException exception) {
        HttpStatusCode status = httpStatus(exception.getCode());
        if (HttpStatus.INTERNAL_SERVER_ERROR.equals(status)) {
            log.error("brand failure with an unmapped business code code={} status={}",
                    exception.getCode(), exception.getStatus(), exception);
        } else {
            log.warn("brand business rejection code={} status={} http={}",
                    exception.getCode(), exception.getStatus(), status.value());
        }
        return ResponseEntity.status(status).body(ResultRecord.failure(exception));
    }

    /**
     * Bean Validation on a request body or a model attribute: the offending fields go into
     * {@code data.fieldErrors} so the envelope keys themselves never change.
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ResultRecord<FieldErrorsVO>> handleBindFailure(BindException exception) {
        List<FieldViolationVO> violations = exception.getBindingResult().getFieldErrors().stream()
                .map(BaseExceptionHandler::toViolation)
                .toList();
        log.warn("brand request rejected with {} field violation(s)", violations.size());
        return fieldErrorsResponse(violations);
    }

    /**
     * Method-level constraint failures raised through {@code @Validated} on a handler parameter,
     * including the cross-field range check the controller raises itself.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ResultRecord<FieldErrorsVO>> handleConstraintViolation(
            ConstraintViolationException exception) {
        List<ConstraintViolation<?>> violations = exception.getConstraintViolations() == null
                ? List.of()
                : List.copyOf(exception.getConstraintViolations());
        log.warn("brand request rejected with {} constraint violation(s)", violations.size());
        return fieldErrorsResponse(violations.stream().map(BaseExceptionHandler::toViolation).toList());
    }

    /**
     * Structural request defects: a missing query parameter, an identifier that is not a number, an
     * unreadable body or an argument the handler rejects. No detail of the cause is echoed.
     */
    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class, IllegalArgumentException.class})
    public ResponseEntity<ResultRecord<Void>> handleBadRequest(Exception exception) {
        log.warn("brand request is not processable: {}", exception.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ResultRecord.failure(GoodsErrorStatus.BRAND_REQUEST_INVALID));
    }

    /**
     * The unique-index race backstop: the pre-check in the application layer answers the common case
     * with 610103, and a concurrent insert that slips past it lands here with the same payload.
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ResultRecord<Void>> handleUniqueConflict(DuplicateKeyException exception) {
        log.warn("brand unique conflict on concurrent create");
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ResultRecord.failure(GoodsErrorStatus.BRAND_NAME_DUPLICATED));
    }

    /**
     * Anything unmapped is a server-side defect: log the throwable with its stack, answer only the
     * 610199 envelope.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResultRecord<Void>> handleUnexpectedException(Exception exception) {
        log.error("unexpected brand failure", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResultRecord.failure(GoodsErrorStatus.BRAND_INTERNAL_ERROR));
    }

    /**
     * The one code-to-status table of {@code REQ-019}: 610101 bad request, 610102 not found,
     * 610103/610104 conflict, 610105 unauthorized, everything else internal error.
     */
    private static HttpStatusCode httpStatus(int code) {
        return switch (code) {
            case 610101 -> HttpStatus.BAD_REQUEST;
            case 610102 -> HttpStatus.NOT_FOUND;
            case 610103, 610104 -> HttpStatus.CONFLICT;
            case 610105 -> HttpStatus.UNAUTHORIZED;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    private static ResponseEntity<ResultRecord<FieldErrorsVO>> fieldErrorsResponse(List<FieldViolationVO> violations) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ResultRecord.result(
                GoodsErrorStatus.BRAND_REQUEST_INVALID, false, new FieldErrorsVO(violations)));
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
}
