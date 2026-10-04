package top.egon.enjoyshop.goods.application.goods;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.egon.enjoyshop.goods.application.goods.converter.SpecManageConverter;
import top.egon.enjoyshop.goods.application.goods.converter.SpecResultConverter;
import top.egon.enjoyshop.goods.domain.goods.SpecBO;
import top.egon.enjoyshop.goods.domain.goods.SpecDomainQuery;
import top.egon.enjoyshop.goods.domain.goods.SpecDomainService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpecManageImplTest {

    @Mock
    private SpecDomainService domainService;

    private SpecManageImpl manage;

    @BeforeEach
    void setUp() {
        manage = new SpecManageImpl(domainService,
                Mappers.getMapper(SpecManageConverter.class),
                Mappers.getMapper(SpecResultConverter.class));
    }

    @Test
    void trims_category_name_and_projects_ordered_options() {
        SpecListQuery query = new SpecListQuery().setCategoryName("  phones  ");
        SpecBO row = new SpecBO().setId(501L).setName("Color").setOptions(List.of("Black", "White")).setSeq(10);
        when(domainService.listByCategoryName(any())).thenReturn(List.of(row));

        List<SpecResult> results = manage.listSpecsByCategoryName(query);

        assertThat(results).containsExactly(new SpecResult("Color", List.of("Black", "White")));
        ArgumentCaptor<SpecDomainQuery> captor = ArgumentCaptor.forClass(SpecDomainQuery.class);
        verify(domainService).listByCategoryName(captor.capture());
        assertThat(captor.getValue().getCategoryName()).isEqualTo("phones");
    }

    @Test
    void no_matching_specs_returns_an_empty_list() {
        when(domainService.listByCategoryName(any())).thenReturn(List.of());

        assertThat(manage.listSpecsByCategoryName(new SpecListQuery().setCategoryName("phones"))).isEmpty();
    }
}
