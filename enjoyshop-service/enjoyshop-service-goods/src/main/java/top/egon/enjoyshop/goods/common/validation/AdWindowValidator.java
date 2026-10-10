package top.egon.enjoyshop.goods.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component("adWindowValidator")
@Slf4j
public class AdWindowValidator implements ConstraintValidator<ValidAdWindow, AdWindowValidator.AdWindowInput> {

    public interface AdWindowInput {
        Instant getStartAt();

        Instant getEndAt();
    }

    @Override
    public boolean isValid(AdWindowInput value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        Instant startAt = value.getStartAt();
        Instant endAt = value.getEndAt();
        boolean valid = true;
        if (startAt != null && startAt.getNano() % 1_000 != 0) {
            valid &= reject(context, "startAt", "must have at most microsecond precision");
        }
        if (endAt != null && endAt.getNano() % 1_000 != 0) {
            valid &= reject(context, "endAt", "must have at most microsecond precision");
        }
        if (startAt != null && endAt != null && !endAt.isAfter(startAt)) {
            valid &= reject(context, "endAt", "must be later than startAt");
        }
        return valid;
    }

    private static boolean reject(ConstraintValidatorContext context, String property, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(property)
                .addConstraintViolation();
        return false;
    }
}
