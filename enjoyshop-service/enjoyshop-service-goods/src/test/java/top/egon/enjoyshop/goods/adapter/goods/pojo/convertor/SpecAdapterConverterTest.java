package top.egon.enjoyshop.goods.adapter.goods.pojo.convertor;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import top.egon.enjoyshop.goods.adapter.goods.pojo.dto.SpecCategoryQueryDTO;
import top.egon.enjoyshop.goods.adapter.goods.pojo.vo.SpecOptionVO;
import top.egon.enjoyshop.goods.application.goods.SpecListQuery;
import top.egon.enjoyshop.goods.application.goods.SpecResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpecAdapterConverterTest {

    private final SpecAdapterConverter converter = Mappers.getMapper(SpecAdapterConverter.class);

    @Test
    void category_query_trims_only_outer_whitespace() {
        SpecCategoryQueryDTO source = new SpecCategoryQueryDTO().setCategoryName("  mobile phone  ");

        SpecListQuery query = converter.toListQuery(source);

        assertThat(query.getCategoryName()).isEqualTo("mobile phone");
    }

    @Test
    void result_projection_copies_options_into_immutable_json_value() {
        List<String> options = new ArrayList<>(List.of("Black", "White"));

        SpecOptionVO result = converter.toTarget(new SpecResult("Color", options));
        options.add("Blue");

        assertThat(result.name()).isEqualTo("Color");
        assertThat(result.options()).containsExactly("Black", "White");
        assertThatThrownBy(() -> result.options().add("Blue")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void null_options_become_empty_array_and_empty_result_list_stays_empty() {
        assertThat(converter.toTarget(new SpecResult("Color", null)).options()).isEmpty();
        assertThat(converter.toVOList(List.of())).isEmpty();
    }
}
