package top.egon.enjoyshop.goods.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Shared cross-field metadata keeps Common independent of Application and Domain carriers. */
@Component("productSearchValidator")
@Slf4j
public class ProductSearchValidator implements ConstraintValidator<ValidProductSearch, ProductSearchValidator.SearchInput> {
    public interface SearchInput {
        Integer getMinPrice();
        Integer getMaxPrice();
        Integer getPageNo();
        Integer getPageSize();
    }

    @Override
    public boolean isValid(SearchInput value, ConstraintValidatorContext context) {
        if (value == null) { return true; }
        if (value.getMinPrice() != null && value.getMaxPrice() != null
                && value.getMinPrice() > value.getMaxPrice()) {
            return reject(context, "minPrice", "Minimum price must not exceed maximum price");
        }
        if (value.getPageNo() != null && value.getPageSize() != null
                && value.getPageNo() > 0 && value.getPageSize() > 0
                && (long) value.getPageNo() * value.getPageSize() > 10_000) {
            return reject(context, "pageNo", "Search pages must remain within the 10000 row window");
        }
        return true;
    }

    private static boolean reject(ConstraintValidatorContext context, String field, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addPropertyNode(field).addConstraintViolation();
        return false;
    }
}
