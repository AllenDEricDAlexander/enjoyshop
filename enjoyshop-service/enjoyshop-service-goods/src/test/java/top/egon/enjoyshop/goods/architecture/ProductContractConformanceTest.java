package top.egon.enjoyshop.goods.architecture;

import static org.assertj.core.api.Assertions.*;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import jakarta.validation.Valid;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.validation.annotation.Validated;
import top.egon.enjoyshop.goods.application.goods.*;
import top.egon.enjoyshop.goods.domain.goods.ProductDomainService;

class ProductContractConformanceTest {
    private static final Path SOURCE = Path.of("src/main/java/top/egon/enjoyshop/goods");

    @Test void everyApplicationAndDomainHandoffRetainsValidation() {
        for (Class<?> type : List.of(ProductManage.class, ProductDomainService.class)) {
            assertThat(type.getDeclaredMethods()).hasSize(10);
            if (type == ProductManage.class) { assertThat(type.getAnnotation(Validated.class)).isNotNull(); }
            else { assertThat(top.egon.enjoyshop.goods.infrastructure.goods.service.impl.ProductDomainServiceImpl.class.getAnnotation(Validated.class)).isNotNull(); }
            for (var method : type.getDeclaredMethods()) {
                for (var parameter : method.getParameters()) {
                    if (!parameter.getType().equals(Long.class)) { assertThat(parameter.getAnnotation(Valid.class)).isNotNull(); }
                    assertThat(parameter.getAnnotation(jakarta.validation.constraints.NotNull.class)).isNotNull();
                }
            }
        }
        for (Class<?> type : List.of(SpuSaveCommand.class, SkuSaveCommand.class)) {
            var names = java.util.Arrays.stream(type.getDeclaredFields()).map(java.lang.reflect.Field::getName).toList();
            assertThat(names).doesNotContain("tenantId", "createUserId", "createTime", "updateUserId", "updateTime", "deletedAt", "version", "saleNum", "commentNum");
        }
        assertThat(java.util.Arrays.stream(SpuSaveCommand.class.getDeclaredFields()).map(java.lang.reflect.Field::getName).toList())
                .doesNotContain("id", "status", "isMarketable");
    }

    @Test void concreteDependenciesKeepQualifiedLombokConstructorInjection() throws Exception {
        for (String name : List.of("application.goods.ProductManageImpl", "application.goods.ProductAuthorizationComponent",
                "infrastructure.goods.service.impl.ProductDomainServiceImpl", "infrastructure.goods.service.impl.ProductJsonFieldConverter")) {
            Class<?> type = Class.forName("top.egon.enjoyshop.goods." + name);
            var fields = java.util.Arrays.stream(type.getDeclaredFields()).filter(field -> !Modifier.isStatic(field.getModifiers())).toList();
            var parameters = type.getDeclaredConstructors()[0].getParameters();
            assertThat(parameters).hasSize(fields.size());
            for (int i = 0; i < fields.size(); i++) {
                assertThat(Modifier.isFinal(fields.get(i).getModifiers())).isTrue();
                assertThat(parameters[i].getAnnotation(Qualifier.class).value()).isEqualTo(fields.get(i).getAnnotation(Qualifier.class).value());
            }
        }
    }

    @Test void domainAndApplicationRemainIndependentOfPersistenceAndSecurityTransport() throws Exception {
        try (var files = Files.walk(SOURCE.resolve("domain/goods"))) {
            for (Path file : files.filter(path -> path.getFileName().toString().startsWith("Product")).toList()) {
                assertThat(Files.readString(file)).doesNotContain("org.springframework.security", "java.sql", ".infrastructure.", "java.util.Date");
            }
        }
        String manage = Files.readString(SOURCE.resolve("application/goods/ProductManageImpl.java"));
        assertThat(manage).doesNotContain(".infrastructure.", "Repository", "getBaseMapper");
        assertThat(Files.readString(SOURCE.resolve("infrastructure/goods/service/impl/ProductDomainServiceImpl.java")))
                .doesNotContain(".dao.", "getBaseMapper", "SqlSession", "DataSource");
        for (Path base : List.of(SOURCE.resolve("application/goods"), SOURCE.resolve("infrastructure/goods/service/impl"))) {
            try (var files = Files.list(base)) {
                for (Path file : files.filter(path -> path.getFileName().toString().startsWith("Product") && path.toString().endsWith("Converter.java")).toList()) {
                    assertThat(Files.readString(file)).containsAnyOf("BaseConverter<", "BaseForwardConverter<")
                            .doesNotContain("BeanUtils", "BeanCopier", "Fastjson", "Hutool");
                }
            }
        }
    }
}
