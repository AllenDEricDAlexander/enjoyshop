package top.egon.enjoyshop.goods.infrastructure.goods;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ProductQuerySqlShapeTest {

    @Test
    void queriesAndWritesHaveExplicitTenantLifecycleAndVersionGuards() throws Exception {
        Path root = Path.of("src/main/resources/mybatis/mapper/goods");
        String spu = Files.readString(root.resolve("SpuDAO.xml"));
        String sku = Files.readString(root.resolve("SkuDAO.xml"));
        String association = Files.readString(root.resolve("CategoryBrandDAO.xml"));
        assertThat(spu).contains("selectByLifecycle", "lockByLifecycle", "selectPage", "countPage",
                "selectCatalogReferences", "restoreVersionedById", "purgeDeletedById",
                "tenant_id", "GREATEST", "ORDER BY id DESC", "#{offset}");
        assertThat(sku).contains("selectBySpuLifecycle", "selectAllBySpuForPurge", "restoreVersionedById",
                "purgeAllBySpu", "deletion_kind = 'P'", "deletion_kind = NULL", "#{et.deletionKind}",
                "tenant_id", "GREATEST", "#{MP_OPTLOCK_VERSION_ORIGINAL}");
        assertThat(association).contains("selectActiveByEndpoints", "#{tenantId}", "#{categoryId}", "#{brandId}");
    }
}
