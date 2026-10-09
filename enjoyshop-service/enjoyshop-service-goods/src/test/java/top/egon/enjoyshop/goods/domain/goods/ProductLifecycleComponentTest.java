package top.egon.enjoyshop.goods.domain.goods;

import static org.assertj.core.api.Assertions.*;
import java.lang.reflect.InvocationTargetException;
import org.junit.jupiter.api.Test;

class ProductLifecycleComponentTest {
    @Test
    void transitionMatrix() throws Exception {
        Object rules = Class.forName("top.egon.enjoyshop.goods.domain.goods.ProductLifecycleComponent").getConstructor().newInstance();
        var evaluate = rules.getClass().getMethod("evaluate", ProductActionEnum.class, ProductStateBO.class);
        ProductStateBO active = ProductStateBO.builder().id(1L).version(3L).status("0").marketable(false)
                .deleted(false).hasActiveSku(true).referencesValid(true).build();
        ProductStateBO approved = (ProductStateBO) evaluate.invoke(rules, ProductActionEnum.AUDIT, active);
        assertThat(approved.getStatus()).isEqualTo("1");
        assertThat(approved.getMarketable()).isTrue();
        ProductStateBO down = (ProductStateBO) evaluate.invoke(rules, ProductActionEnum.PULL, approved);
        assertThat(down.getStatus()).isEqualTo("1");
        assertThat(down.getMarketable()).isFalse();
        assertThatThrownBy(() -> evaluate.invoke(rules, ProductActionEnum.DELETE, approved))
                .isInstanceOf(InvocationTargetException.class);
        assertThatThrownBy(() -> evaluate.invoke(rules, ProductActionEnum.PUT, active))
                .isInstanceOf(InvocationTargetException.class);
        ProductStateBO deleted = (ProductStateBO) evaluate.invoke(rules, ProductActionEnum.DELETE, down);
        assertThat(deleted.getDeleted()).isTrue();
        assertThat(deleted.getStatus()).isEqualTo("0");
        ProductStateBO restored = (ProductStateBO) evaluate.invoke(rules, ProductActionEnum.RESTORE, deleted);
        assertThat(restored.getDeleted()).isFalse();
        assertThat(restored.getMarketable()).isFalse();
        assertThatThrownBy(() -> evaluate.invoke(rules, ProductActionEnum.AUDIT, active.setHasActiveSku(false)))
                .isInstanceOf(InvocationTargetException.class);
    }
}
