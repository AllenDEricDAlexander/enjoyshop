package top.egon.enjoyshop.goods.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ProductCompositionValidator.class)
public @interface ValidProductComposition {
    String message() default "Product SKU composition is invalid";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
