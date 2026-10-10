package top.egon.enjoyshop.goods.common.validation;

import jakarta.validation.groups.Default;

/** Validation scenarios shared by advertisement management boundaries. */
public final class AdValidationGroup {

    private AdValidationGroup() {
    }

    public interface Write extends Default {
    }

    public interface Create extends Write {
    }

    public interface Update extends Write {
    }

    public interface Delete extends Default {
    }

    public interface Detail extends Default {
    }

    public interface Page extends Default {
    }

    public interface PublicRead extends Write {
    }
}
