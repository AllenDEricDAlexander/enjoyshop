package top.egon.enjoyshop.goods.common.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** Closed ordering strategies; the future search adapter consumes numeric SKU tie breakers. */
@Getter
@AllArgsConstructor
public enum ProductSearchSortEnum {
    RELEVANCE("relevance") {
        @Override public List<SortClauseBO> clauses(boolean keywordPresent) {
            return keywordPresent ? List.of(new SortClauseBO("_score", false), new SortClauseBO("skuId", true))
                    : List.of(new SortClauseBO("skuId", false));
        }
    },
    PRICE_ASC("priceAsc") {
        @Override public List<SortClauseBO> clauses(boolean keywordPresent) { return ordered("price", true); }
    },
    PRICE_DESC("priceDesc") {
        @Override public List<SortClauseBO> clauses(boolean keywordPresent) { return ordered("price", false); }
    },
    SALES_DESC("salesDesc") {
        @Override public List<SortClauseBO> clauses(boolean keywordPresent) { return ordered("saleNum", false); }
    },
    CREATED_DESC("createdDesc") {
        @Override public List<SortClauseBO> clauses(boolean keywordPresent) { return ordered("skuCreatedAt", false); }
    };

    @JsonValue private final String code;
    public abstract List<SortClauseBO> clauses(boolean keywordPresent);

    public static ProductSearchSortEnum fromCode(String code) {
        return Arrays.stream(values()).filter(value -> value.code.equals(code)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported search ordering"));
    }

    private static List<SortClauseBO> ordered(String field, boolean ascending) {
        return List.of(new SortClauseBO(field, ascending), new SortClauseBO("skuId", true));
    }

    /** Immutable value semantics, with fields restricted to the strategies declared above. */
    public record SortClauseBO(String field, boolean ascending) { }
}
