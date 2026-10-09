package top.egon.enjoyshop.goods.domain.goods;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Predicate;
import lombok.extern.slf4j.Slf4j;
import top.egon.cola.component.common.core.exception.CommonException;

/** Immutable Specification registry shared by all lifecycle operations. */
@Slf4j
public class ProductLifecycleComponent {
    private static final Predicate<ProductStateBO> ACTIVE = value -> !Boolean.TRUE.equals(value.getDeleted());
    private static final Predicate<ProductStateBO> DOWN = value -> !Boolean.TRUE.equals(value.getMarketable());
    private static final Predicate<ProductStateBO> APPROVED = value -> "1".equals(value.getStatus());
    private static final Predicate<ProductStateBO> PUBLISHABLE = value -> Boolean.TRUE.equals(value.getHasActiveSku())
            && Boolean.TRUE.equals(value.getReferencesValid());
    private static final Map<ProductActionEnum, ProductTransitionBO> RULES = rules();

    private static Map<ProductActionEnum, ProductTransitionBO> rules() {
        EnumMap<ProductActionEnum, ProductTransitionBO> rules = new EnumMap<>(ProductActionEnum.class);
        rules.put(ProductActionEnum.CREATE, new ProductTransitionBO(value -> true, "0", false, false));
        rules.put(ProductActionEnum.EDIT, new ProductTransitionBO(ACTIVE.and(DOWN), "0", false, false));
        rules.put(ProductActionEnum.AUDIT, new ProductTransitionBO(ACTIVE.and(PUBLISHABLE), "1", true, false));
        rules.put(ProductActionEnum.PULL, new ProductTransitionBO(ACTIVE, null, false, false));
        rules.put(ProductActionEnum.PUT, new ProductTransitionBO(ACTIVE.and(APPROVED).and(PUBLISHABLE), "1", true, false));
        rules.put(ProductActionEnum.DELETE, new ProductTransitionBO(DOWN, "0", false, true));
        rules.put(ProductActionEnum.RESTORE, new ProductTransitionBO(ACTIVE.negate(), "0", false, false));
        rules.put(ProductActionEnum.PURGE, new ProductTransitionBO(ACTIVE.negate(), "0", false, true));
        if (rules.size() != ProductActionEnum.values().length) {
            throw new IllegalStateException("Missing product lifecycle rule");
        }
        return Map.copyOf(rules);
    }

    public ProductStateBO evaluate(ProductActionEnum action, ProductStateBO current) {
        ProductTransitionBO rule = RULES.get(action);
        if (rule == null || current == null || !rule.guard().test(current)) {
            throw new CommonException(610204, "PRODUCT_STATE_CONFLICT", "Product state does not allow this operation");
        }
        return ProductStateBO.builder().id(current.getId()).version(current.getVersion())
                .status(rule.status() == null ? current.getStatus() : rule.status())
                .marketable(rule.marketable()).deleted(rule.deleted())
                .hasActiveSku(current.getHasActiveSku()).referencesValid(current.getReferencesValid()).build();
    }

    private record ProductTransitionBO(Predicate<ProductStateBO> guard, String status, boolean marketable, boolean deleted) { }
}
