package top.egon.enjoyshop.goods.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Pure composition checks; catalog ownership and persisted state are checked in the transaction. */
@Component("productCompositionValidator")
@Slf4j
public class ProductCompositionValidator implements ConstraintValidator<ValidProductComposition, ProductCompositionValidator.ProductCompositionInput> {

    /** Shared constraint contract keeps Common independent of either Application or Domain carriers. */
    public interface ProductCompositionInput {
        Boolean specificationsEnabled();
        Map<String, java.util.List<String>> advertisedSpecifications();
        java.util.List<? extends ProductSkuInput> compositionSkus();
    }

    public interface ProductSkuInput {
        Long compositionId();
        String compositionBarcode();
        Map<String, String> compositionSpecification();
    }

    @Override
    public boolean isValid(ProductCompositionInput command, ConstraintValidatorContext context) {
        if (command == null || command.compositionSkus() == null
                || command.specificationsEnabled() == null || command.advertisedSpecifications() == null) {
            return true;
        }
        Map<String, java.util.List<String>> advertised = command.advertisedSpecifications();
        if (!command.specificationsEnabled()
                && (command.compositionSkus().size() != 1 || !advertised.isEmpty())) {
            return reject(context, "Disabled specifications require exactly one SKU and an empty specification set", null);
        }
        if (command.specificationsEnabled() && advertised.isEmpty()) {
            return reject(context, "Enabled specifications require advertised values", null);
        }
        Set<Long> ids = new HashSet<>();
        Set<String> barcodes = new HashSet<>();
        Set<Map<String, String>> combinations = new HashSet<>();
        for (int index = 0; index < command.compositionSkus().size(); index++) {
            ProductSkuInput sku = command.compositionSkus().get(index);
            if (sku == null || sku.compositionBarcode() == null || sku.compositionSpecification() == null) {
                continue;
            }
            if ((sku.compositionId() != null && !ids.add(sku.compositionId())) || !barcodes.add(sku.compositionBarcode().trim())) {
                return reject(context, "SKU identifiers and barcodes must be unique within the product", index);
            }
            if (!command.specificationsEnabled() && !sku.compositionSpecification().isEmpty()) {
                return reject(context, "A specification-disabled SKU must have an empty specification", index);
            }
            if (!sku.compositionSpecification().keySet().equals(advertised.keySet())) {
                return reject(context, "SKU specification keys must match the advertised keys", index);
            }
            for (Map.Entry<String, String> entry : sku.compositionSpecification().entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null
                        || advertised.get(entry.getKey()) == null
                        || !advertised.get(entry.getKey()).contains(entry.getValue())) {
                    return reject(context, "SKU specification values must belong to the advertised options", index);
                }
            }
            if (!combinations.add(new TreeMap<>(sku.compositionSpecification()))) {
                return reject(context, "SKU specification combinations must be unique", index);
            }
        }
        return true;
    }

    private static boolean reject(ConstraintValidatorContext context, String message, Integer index) {
        context.disableDefaultConstraintViolation();
        if (index == null) {
            context.buildConstraintViolationWithTemplate(message).addPropertyNode("skuList").addConstraintViolation();
        } else {
            context.buildConstraintViolationWithTemplate(message).addPropertyNode("skuList")
                    .addPropertyNode("spec").inIterable().atIndex(index).addConstraintViolation();
        }
        return false;
    }
}
