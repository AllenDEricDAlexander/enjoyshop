package top.egon.enjoyshop.goods.application.goods;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import top.egon.cola.component.common.core.converter.BaseConverter;
import top.egon.cola.component.common.core.converter.BaseForwardConverter;
import top.egon.enjoyshop.goods.domain.goods.ProductBO;
import top.egon.enjoyshop.goods.domain.goods.SpuBO;
import top.egon.enjoyshop.goods.infrastructure.goods.po.SpuPO;

class ProductConverterTest {

    @Test
    @SuppressWarnings("unchecked")
    void fieldMappingsPreserveAuditAndIsolateWireAndInputFields() throws Exception {
        try (AnnotationConfigApplicationContext context = converters()) {
            BaseConverter<SpuPO, SpuBO> persistence = (BaseConverter<SpuPO, SpuBO>) context.getBean("productSpuPersistenceConverterImpl");
            SpuPO row = SpuPO.builder().id(9007199254740993L).tenantId(1L).sn("SPU-A").name("手机")
                    .version(3L).createUserId("author").createTime(Instant.parse("2026-10-09T01:00:00Z"))
                    .updateUserId("editor").updateTime(Instant.parse("2026-10-09T02:00:00Z"))
                    .deletedAt(LocalDateTime.parse("2026-10-09T03:00:00.000001"))
                    .images("[\"https://media.example.com/p.png\"]").saleService("[\"七天包退\"]")
                    .specItems("{\"颜色\":[\"红\",\"蓝\"]}").paraItems("{\"年份\":\"2026\"}").build();
            SpuBO business = persistence.toTarget(row);
            assertThat(business.getImages()).containsExactly("https://media.example.com/p.png");
            assertThat(business.getSpecItems()).containsEntry("颜色", List.of("红", "蓝"));
            SpuPO copy = persistence.toSource(business);
            assertThat(copy.getCreateTime()).isEqualTo(row.getCreateTime());
            assertThat(copy.getDeletedAt()).isEqualTo(row.getDeletedAt());
            assertThat(copy.getTenantId()).isEqualTo(1L);
            BaseForwardConverter<ProductBO, ProductResult> projection = (BaseForwardConverter<ProductBO, ProductResult>) context.getBean("productQueryConverterImpl");
            ProductResult result = projection.toTarget(ProductBO.builder().spu(business).skuList(List.of()).build());
            assertThat(result.getSpu().getIsDelete()).isTrue();
            ObjectMapper mapper = context.getBean("jacksonObjectMapper", ObjectMapper.class);
            String json = mapper.writeValueAsString(result);
            assertThat(json).contains("\"id\":\"9007199254740993\"").doesNotContain("tenantId", "createUserId", "updateUserId", "deletionKind");
            business.setName("edited");
            assertThat(row.getName()).isEqualTo("手机");
        }
    }

    private static AnnotationConfigApplicationContext converters() throws Exception {
        List<Class<?>> classes = new ArrayList<>();
        for (String name : List.of(
                "infrastructure.goods.service.impl.ProductJsonFieldConverter",
                "infrastructure.goods.service.impl.ProductSpuPersistenceConverterImpl",
                "infrastructure.goods.service.impl.ProductSkuPersistenceConverterImpl",
                "application.goods.ProductSpuSaveConverterImpl", "application.goods.ProductSkuSaveConverterImpl",
                "application.goods.ProductSpuResultConverterImpl", "application.goods.ProductSkuResultConverterImpl",
                "application.goods.ProductApplicationConverterImpl", "application.goods.ProductQueryConverterImpl")) {
            try {
                classes.add(Class.forName("top.egon.enjoyshop.goods." + name));
            } catch (ClassNotFoundException missingImplementation) {
                // Missing production capability is asserted before constructing the test context.
            }
        }
        assertThat(classes).as("all necessary product single-table and aggregate conversions").hasSize(9);
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.registerBean("jacksonObjectMapper", ObjectMapper.class, () -> new ObjectMapper().findAndRegisterModules());
        context.register(classes.toArray(Class<?>[]::new));
        context.refresh();
        return context;
    }
}
