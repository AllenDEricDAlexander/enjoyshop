package top.egon.enjoyshop.goods.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("adPositionValidator")
@Slf4j
@RequiredArgsConstructor
public class AdPositionValidator implements ConstraintValidator<ValidAdPosition, String> {

    @Qualifier("adAllowedPositions")
    private final List<String> allowedPositions;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || value.matches("^[a-z][a-z0-9_]{0,63}$") && allowedPositions.contains(value);
    }
}
