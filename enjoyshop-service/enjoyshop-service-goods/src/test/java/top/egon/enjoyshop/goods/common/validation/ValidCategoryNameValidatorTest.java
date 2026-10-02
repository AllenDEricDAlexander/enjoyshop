package top.egon.enjoyshop.goods.common.validation;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ValidCategoryNameValidatorTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void optionalCategoryNameAllowsNull() {
        assertThat(validator.validate(new OptionalCategoryNameDTO(null))).isEmpty();
    }

    @Test
    void requiredCategoryNameRejectsNull() {
        assertThat(validator.validate(new RequiredCategoryNameDTO(null))).isNotEmpty();
    }

    @Test
    void presentCategoryNameRejectsBlankInput() {
        assertThat(validator.validate(new OptionalCategoryNameDTO("   "))).isNotEmpty();
    }

    @Test
    void trimmedCategoryNameLengthIsLimitedToOneHundredTwentyCharacters() {
        assertThat(validator.validate(new OptionalCategoryNameDTO("  " + "x".repeat(120) + "  "))).isEmpty();
        assertThat(validator.validate(new OptionalCategoryNameDTO("x".repeat(121)))).isNotEmpty();
    }

    @Test
    void internalWhitespaceIsPreservedAndAccepted() {
        assertThat(validator.validate(new OptionalCategoryNameDTO("mobile phone"))).isEmpty();
    }

    private record OptionalCategoryNameDTO(
            @ValidCategoryName(required = false) String categoryName) {
    }

    private record RequiredCategoryNameDTO(
            @ValidCategoryName(required = true) String categoryName) {
    }
}
