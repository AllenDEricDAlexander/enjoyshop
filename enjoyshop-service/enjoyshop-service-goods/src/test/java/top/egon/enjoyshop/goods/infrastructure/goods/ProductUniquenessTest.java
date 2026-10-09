package top.egon.enjoyshop.goods.infrastructure.goods;

import static org.assertj.core.api.Assertions.assertThat;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** SQL contract evidence; PostgreSQL execution/concurrency remains a deployment acceptance gate. */
class ProductUniquenessTest {
    @Test
    void activeAndHistoricalBusinessKeysAreTenantScoped() throws Exception {
        String ddl = Files.readString(Path.of("src/main/resources/db/egon-mp/V20261009_001__initialize_product_schema.sql"));
        for (String table : new String[]{"spu", "sku"}) {
            assertThat(ddl).contains("ON " + table + " (tenant_id, sn) WHERE deleted_at IS NULL")
                    .contains("ON " + table + " (tenant_id, sn, deleted_at)");
            String mapper = Files.readString(Path.of("src/main/resources/mybatis/mapper/goods/" + (table.equals("spu") ? "Spu" : "Sku") + "DAO.xml"));
            assertThat(mapper).contains("GREATEST", "MAX(history.deleted_at)", "INTERVAL '0.000001' SECOND", "history.tenant_id = #{et.tenantId}");
        }
    }
}
