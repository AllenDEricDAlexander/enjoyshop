package top.egon.enjoyshop.goods.application.goods;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.cola.component.common.core.pojo.PageResultRecord;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.enjoyshop.goods.common.enums.GoodsErrorStatus;
import top.egon.enjoyshop.goods.application.goods.converter.BrandResultConverter;
import top.egon.enjoyshop.goods.application.goods.converter.CreateBrandCommandConverter;
import top.egon.enjoyshop.goods.application.goods.converter.DeleteBrandCommandConverter;
import top.egon.enjoyshop.goods.application.goods.converter.UpdateBrandCommandConverter;
import top.egon.enjoyshop.goods.domain.goods.BrandBO;
import top.egon.enjoyshop.goods.domain.goods.BrandDomainQuery;
import top.egon.enjoyshop.goods.domain.goods.BrandDomainService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Application-layer gate for the operator guard and the name pre-check (TEST-011 / TEST-016).
 *
 * <p>The domain service is a mock, so "rejected before the persistence layer" is observable as
 * zero interactions instead of a database round trip.</p>
 */
@ExtendWith(MockitoExtension.class)
class BrandManageImplTest {

    private static final String OPERATOR_KEY = "userId";
    private static final String OPERATOR = "10001";
    private static final long BRAND_ID = 90001L;
    private static final String BRAND_NAME = "Apple";
    private static final Integer SEQ_MIN = 10;
    private static final Integer SEQ_MAX = 20;

    @Mock
    private BrandDomainService domainService;

    @Mock
    private CreateBrandCommandConverter createConverter;

    @Mock
    private UpdateBrandCommandConverter updateConverter;

    @Mock
    private DeleteBrandCommandConverter deleteConverter;

    @Mock
    private BrandResultConverter resultConverter;

    @Mock
    private BrandAuthorizationComponent authorization;

    @Mock
    private ValidationUtils validation;

    @InjectMocks
    private BrandManageImpl manage;

    @BeforeEach
    void clearOperatorContext() {
        MDC.remove(OPERATOR_KEY);
    }

    @AfterEach
    void releaseOperatorContext() {
        MDC.clear();
    }

    @Test
    void write_without_operator_is_rejected_with_610105_and_zero_downstream() {
        MDC.remove(OPERATOR_KEY);

        assertThatThrownBy(() -> manage.create(createCommand()))
                .isInstanceOfSatisfying(CommonException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo(610105);
                    assertThat(exception.getStatus()).isEqualTo("BRAND_OPERATOR_REQUIRED");
                });
        assertThatThrownBy(() -> manage.update(updateCommand()))
                .isInstanceOfSatisfying(CommonException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(610105));
        assertThatThrownBy(() -> manage.delete(deleteCommand()))
                .isInstanceOfSatisfying(CommonException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(610105));

        verifyNoInteractions(domainService);
    }

    @Test
    void denialDoesNotCallDomain() {
        lenient().doThrow(new CommonException(GoodsErrorStatus.BRAND_FORBIDDEN))
                .when(authorization).requirePermission("enjoyshop:brand:read");

        assertThatThrownBy(() -> manage.listBrands(pageQuery(BRAND_NAME, 1, 10), SEQ_MIN, SEQ_MAX))
                .isInstanceOf(CommonException.class);

        verifyNoInteractions(domainService);
    }

    @Test
    void allowValidatesThenCallsDomain() {
        BrandBO row = brand();
        when(domainService.listPage(any(BrandDomainQuery.class), eq(SEQ_MIN), eq(SEQ_MAX)))
                .thenReturn(List.of(row));
        when(domainService.countPage(any(BrandDomainQuery.class), eq(SEQ_MIN), eq(SEQ_MAX))).thenReturn(1L);
        when(resultConverter.toTargetList(List.of(row))).thenReturn(List.of(brandResult()));

        manage.listBrands(pageQuery(BRAND_NAME, 1, 10), SEQ_MIN, SEQ_MAX);

        InOrder order = inOrder(authorization, validation, domainService);
        order.verify(authorization).requirePermission("enjoyshop:brand:read");
        order.verify(validation).validate(any(BrandPageQuery.class));
        order.verify(domainService).listPage(any(BrandDomainQuery.class), eq(SEQ_MIN), eq(SEQ_MAX));
    }

    @Test
    void writeWithoutOperatorRemains610105AfterPermissionAllows() {
        MDC.remove(OPERATOR_KEY);

        assertThatThrownBy(() -> manage.create(createCommand()))
                .isInstanceOfSatisfying(CommonException.class, error ->
                        assertThat(error.getCode()).isEqualTo(610105));

        verifyNoInteractions(domainService);
    }

    @Test
    void create_with_duplicate_name_is_rejected_with_610103_before_write() {
        MDC.put(OPERATOR_KEY, OPERATOR);
        when(domainService.existsActiveByName(BRAND_NAME, null)).thenReturn(true);

        assertThatThrownBy(() -> manage.create(createCommand()))
                .isInstanceOfSatisfying(CommonException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo(610103);
                    assertThat(exception.getStatus()).isEqualTo("BRAND_NAME_DUPLICATED");
                });

        verify(domainService, never()).create(any());
    }

    @Test
    void update_duplicate_check_excludes_self_row() {
        MDC.put(OPERATOR_KEY, OPERATOR);
        when(updateConverter.toTarget(any(UpdateBrandCommand.class))).thenReturn(brand());

        manage.update(updateCommand());

        ArgumentCaptor<Long> excludeId = ArgumentCaptor.forClass(Long.class);
        verify(domainService).existsActiveByName(eq(BRAND_NAME), excludeId.capture());
        assertThat(excludeId.getValue()).isEqualTo(BRAND_ID);
    }

    @Test
    void list_brands_returns_total_and_page_meta() {
        MDC.put(OPERATOR_KEY, OPERATOR);
        BrandBO row = brand();
        when(domainService.listPage(any(BrandDomainQuery.class), eq(SEQ_MIN), eq(SEQ_MAX)))
                .thenReturn(List.of(row));
        when(domainService.countPage(any(BrandDomainQuery.class), eq(SEQ_MIN), eq(SEQ_MAX))).thenReturn(37L);
        when(resultConverter.toTargetList(List.of(row))).thenReturn(List.of(brandResult()));

        PageResultRecord<BrandResult> page = manage.listBrands(pageQuery(BRAND_NAME, 1, 10), SEQ_MIN, SEQ_MAX);

        assertThat(page.success()).isTrue();
        assertThat(page.records()).containsExactly(brandResult());
        assertThat(page.page().total()).isEqualTo(37L);
        assertThat(page.page().pageNo()).isEqualTo(1);
        assertThat(page.page().pageSize()).isEqualTo(10);
    }

    @Test
    void list_brands_passes_category_to_the_same_domain_query_for_list_and_count() {
        MDC.put(OPERATOR_KEY, OPERATOR);
        BrandBO row = brand();
        when(domainService.listPage(any(BrandDomainQuery.class), eq(SEQ_MIN), eq(SEQ_MAX)))
                .thenReturn(List.of(row));
        when(domainService.countPage(any(BrandDomainQuery.class), eq(SEQ_MIN), eq(SEQ_MAX))).thenReturn(1L);
        when(resultConverter.toTargetList(List.of(row))).thenReturn(List.of(brandResult()));

        BrandPageQuery query = pageQuery(BRAND_NAME, 1, 10).setCategoryName("phones");
        manage.listBrands(query, SEQ_MIN, SEQ_MAX);

        ArgumentCaptor<BrandDomainQuery> listQuery = ArgumentCaptor.forClass(BrandDomainQuery.class);
        ArgumentCaptor<BrandDomainQuery> countQuery = ArgumentCaptor.forClass(BrandDomainQuery.class);
        verify(domainService).listPage(listQuery.capture(), eq(SEQ_MIN), eq(SEQ_MAX));
        verify(domainService).countPage(countQuery.capture(), eq(SEQ_MIN), eq(SEQ_MAX));
        assertThat(listQuery.getValue().getCategoryName()).isEqualTo("phones");
        assertThat(countQuery.getValue().getCategoryName()).isEqualTo("phones");
        assertThat(listQuery.getValue().getPage()).isEqualTo(query.getPage());
    }

    private static BrandBO brand() {
        return BrandBO.builder().id(BRAND_ID).version(0L).name(BRAND_NAME)
                .image("https://cdn/apple.png").letter("A").seq(10).build();
    }

    private static BrandResult brandResult() {
        return BrandResult.builder().id(Long.toString(BRAND_ID)).version(0L).name(BRAND_NAME)
                .image("https://cdn/apple.png").letter("A").seq(10).build();
    }

    private static BrandPageQuery pageQuery(String name, int pageNo, int pageSize) {
        return BrandPageQuery.builder().name(name).letter("A")
                .page(new PageQuery(pageNo, pageSize)).build();
    }

    private static CreateBrandCommand createCommand() {
        return new CreateBrandCommand().setName(BRAND_NAME).setImage("https://cdn/apple.png")
                .setLetter("A").setSeq(10);
    }

    private static UpdateBrandCommand updateCommand() {
        return new UpdateBrandCommand().setId(BRAND_ID).setExpectedVersion(0L).setName(BRAND_NAME)
                .setImage("https://cdn/apple.png").setLetter("A").setSeq(10);
    }

    private static DeleteBrandCommand deleteCommand() {
        return new DeleteBrandCommand().setId(BRAND_ID).setExpectedVersion(0L);
    }
}
