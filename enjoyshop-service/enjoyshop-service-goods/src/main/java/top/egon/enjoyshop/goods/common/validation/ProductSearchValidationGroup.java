package top.egon.enjoyshop.goods.common.validation;

import jakarta.validation.groups.Default;

/** Search boundaries share leaf constraints and keep trusted scope validation internal. */
public final class ProductSearchValidationGroup {
    private ProductSearchValidationGroup() { }
    public interface PublicSearch extends Default { }
    public interface InternalSearch extends Default { }
}
