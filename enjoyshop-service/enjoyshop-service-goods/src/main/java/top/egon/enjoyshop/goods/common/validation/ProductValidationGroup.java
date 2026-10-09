package top.egon.enjoyshop.goods.common.validation;

import jakarta.validation.groups.Default;

/** Product boundary groups; shared carriers keep operation-specific constraints. */
public final class ProductValidationGroup {
    private ProductValidationGroup() {
    }

    public interface Create extends Default { }
    public interface Update extends Default { }
    public interface HttpStateInput extends Default { }
    public interface StateCall extends Default { }
    public interface Detail extends Default { }
    public interface Page extends Default { }
}
