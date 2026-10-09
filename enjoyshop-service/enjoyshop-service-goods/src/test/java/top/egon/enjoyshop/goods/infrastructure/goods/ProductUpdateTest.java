package top.egon.enjoyshop.goods.infrastructure.goods;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.validation.Validation;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.enjoyshop.goods.domain.goods.CatalogReferenceBO;
import top.egon.enjoyshop.goods.domain.goods.ProductBO;
import top.egon.enjoyshop.goods.domain.goods.ProductDomainService;
import top.egon.enjoyshop.goods.domain.goods.SkuBO;
import top.egon.enjoyshop.goods.domain.goods.SpuBO;
import top.egon.enjoyshop.goods.infrastructure.goods.po.SkuPO;
import top.egon.enjoyshop.goods.infrastructure.goods.po.SpuPO;
import top.egon.enjoyshop.goods.infrastructure.goods.repo.CategoryBrandRepository;
import top.egon.enjoyshop.goods.infrastructure.goods.repo.SkuRepository;
import top.egon.enjoyshop.goods.infrastructure.goods.repo.SpuRepository;

class ProductUpdateTest {

    @Test
    void updateKeepsSkuIdentityCreationAndCountersWhileResettingReview() throws Exception {
        try (AnnotationConfigApplicationContext context = fixture()) {
            SpuRepository spu = context.getBean(SpuRepository.class);
            SkuRepository sku = context.getBean(SkuRepository.class);
            SpuPO current = SpuPO.builder().id(100L).tenantId(1L).sn("SPU-A").name("旧手机")
                    .version(3L).status("1").isMarketable(false).createTime(Instant.parse("2026-10-09T01:00:00Z"))
                    .saleNum(8).commentNum(2).build();
            SkuPO existing = SkuPO.builder().id(101L).tenantId(1L).spuId(100L).sn("SKU-A").name("旧手机")
                    .version(2L).price(1).num(0).alertNum(0).weight(0).status("1")
                    .createTime(current.getCreateTime()).saleNum(5).commentNum(1).build();
            when(spu.lockByLifecycle(any())).thenReturn(current);
            when(sku.selectBySpuLifecycle(100L, false)).thenReturn(List.of(existing));
            when(spu.updateById(any())).thenAnswer(call -> { ((SpuPO) call.getArgument(0)).setVersion(4L); return true; });
            when(sku.updateById(any())).thenReturn(true);
            ProductBO input = validProduct().setExpectedVersion(3L);
            input.getSpu().setId(100L);
            input.getSkuList().getFirst().setId(101L).setPrice(100);
            ProductBO result = context.getBean(ProductDomainService.class).update(input);
            assertThat(result.getSpu().getVersion()).isEqualTo(4L);
            assertThat(result.getSpu().getStatus()).isEqualTo("0");
            assertThat(result.getSkuList().getFirst().getId()).isEqualTo(101L);
            assertThat(result.getSkuList().getFirst().getCreateTime()).isEqualTo(existing.getCreateTime());
            assertThat(result.getSkuList().getFirst().getSaleNum()).isEqualTo(5);
            assertThat(result.getSkuList().getFirst().getName()).isEqualTo("新手机");
        } finally {
            MDC.clear();
        }
    }

    @Test
    void aSkuFromAnotherProductAndAnOldParentVersionCannotBeWritten() throws Exception {
        try (AnnotationConfigApplicationContext context = fixture()) {
            SpuRepository spu = context.getBean(SpuRepository.class);
            SkuRepository sku = context.getBean(SkuRepository.class);
            when(spu.lockByLifecycle(any())).thenReturn(SpuPO.builder().id(100L).tenantId(1L)
                    .sn("SPU-A").version(3L).status("0").isMarketable(false).build());
            when(sku.selectBySpuLifecycle(100L, false)).thenReturn(List.of());
            ProductBO value = validProduct().setExpectedVersion(3L);
            value.getSpu().setId(100L);
            value.getSkuList().getFirst().setId(999L);
            assertThatThrownBy(() -> context.getBean(ProductDomainService.class).update(value))
                    .isInstanceOf(CommonException.class);
            value.setExpectedVersion(2L);
            assertThatThrownBy(() -> context.getBean(ProductDomainService.class).update(value))
                    .isInstanceOf(CommonException.class);
        } finally {
            MDC.clear();
        }
    }

    private static AnnotationConfigApplicationContext fixture() throws Exception {
        Class<?> implementation = null;
        try {
            implementation = Class.forName("top.egon.enjoyshop.goods.infrastructure.goods.service.impl.ProductDomainServiceImpl");
        } catch (ClassNotFoundException missingCapability) {
            // The assertion is the RED point for the missing domain implementation.
        }
        assertThat(implementation).as("product aggregate domain implementation").isNotNull();
        EgonColaTenantIdProvider.initialize("tenantId");
        MDC.put("tenantId", "1");
        MDC.put("userId", "editor");
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.registerBean("jacksonObjectMapper", com.fasterxml.jackson.databind.ObjectMapper.class,
                () -> new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules());
        context.registerBean("egonColaValidationUtils", ValidationUtils.class,
                () -> new ValidationUtils(Validation.buildDefaultValidatorFactory().getValidator()));
        SpuRepository spu = mock(SpuRepository.class);
        when(spu.selectByLifecycle(any())).thenAnswer(call -> spu.lockByLifecycle(call.getArgument(0)));
        SkuRepository sku = mock(SkuRepository.class);
        CategoryBrandRepository association = mock(CategoryBrandRepository.class);
        when(spu.loadCatalogReferences(any())).thenReturn(CatalogReferenceBO.builder().brandId(1L)
                .brandName("品牌").categoryName("手机").category1Id(2L).category2Id(3L).category3Id(4L).templateId(5L).build());
        when(association.selectActiveByEndpoints(4L, 1L)).thenReturn(
                top.egon.enjoyshop.goods.infrastructure.goods.po.CategoryBrandPO.builder().id(10L).build());
        context.registerBean("spuDAO", top.egon.enjoyshop.goods.infrastructure.goods.dao.SpuDAO.class,
                () -> mock(top.egon.enjoyshop.goods.infrastructure.goods.dao.SpuDAO.class));
        context.registerBean("skuDAO", top.egon.enjoyshop.goods.infrastructure.goods.dao.SkuDAO.class,
                () -> mock(top.egon.enjoyshop.goods.infrastructure.goods.dao.SkuDAO.class));
        context.registerBean("categoryBrandDAO", top.egon.enjoyshop.goods.infrastructure.goods.dao.CategoryBrandDAO.class,
                () -> mock(top.egon.enjoyshop.goods.infrastructure.goods.dao.CategoryBrandDAO.class));
        context.registerBean("spuRepository", SpuRepository.class, () -> spu);
        context.registerBean("skuRepository", SkuRepository.class, () -> sku);
        context.registerBean("categoryBrandRepository", CategoryBrandRepository.class, () -> association);
        for (String name : List.of("ProductJsonFieldConverter", "ProductSpuPersistenceConverterImpl", "ProductSkuPersistenceConverterImpl")) {
            context.register(Class.forName("top.egon.enjoyshop.goods.infrastructure.goods.service.impl." + name));
        }
        context.register(top.egon.enjoyshop.goods.domain.goods.ProductLifecycleComponent.class);
        context.register(implementation);
        context.refresh();
        return context;
    }

    private static ProductBO validProduct() {
        return ProductBO.builder().spu(SpuBO.builder().sn("SPU-A").name("新手机").brandId(1L)
                .category1Id(2L).category2Id(3L).category3Id(4L).templateId(5L).isEnableSpec(false).build())
                .skuList(List.of(SkuBO.builder().sn("SKU-A").num(0).alertNum(0).weight(0).price(1).build())).build();
    }
}
