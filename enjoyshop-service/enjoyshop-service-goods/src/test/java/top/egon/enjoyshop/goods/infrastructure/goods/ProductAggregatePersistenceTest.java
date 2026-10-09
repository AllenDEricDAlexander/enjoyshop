package top.egon.enjoyshop.goods.infrastructure.goods;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import top.egon.enjoyshop.goods.domain.goods.ProductBO;
import top.egon.enjoyshop.goods.domain.goods.ProductDomainService;
import top.egon.enjoyshop.goods.domain.goods.ProductDomainQuery;
import top.egon.enjoyshop.goods.domain.goods.SkuBO;
import top.egon.enjoyshop.goods.domain.goods.SpuBO;
import top.egon.enjoyshop.goods.start.StudentManagementApplication;
import top.egon.enjoyshop.goods.support.PersistenceTestSupport;

@ActiveProfiles("test")
@SpringBootTest(classes = StudentManagementApplication.class, properties = {
        "app.integrations.rabbitmq.enabled=false", "app.integrations.redis.enabled=false",
        "app.integrations.external-http.enabled=false", "egon.cola.component.rpc.enabled=false",
        "egon.cola.component.tianshu.enabled=false"})
class ProductAggregatePersistenceTest extends PersistenceTestSupport {
    @Autowired
    @Qualifier("productDomainService")
    private ProductDomainService service;

    private JdbcTemplate jdbc;

    @BeforeEach
    void catalog() throws Exception {
        jdbc = new JdbcTemplate(testDataSource);
        MDC.put("tenantId", "1");
        MDC.put("userId", "editor");
        for (String table : List.of("spu", "sku")) {
            jdbc.execute("ALTER TABLE " + table + " ADD active_sn VARCHAR GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN sn ELSE NULL END)");
            jdbc.execute("CREATE UNIQUE INDEX test_" + table + "_active ON " + table + "(tenant_id,active_sn)");
        }
        for (String resource : List.of("V20260923_001__initialize_goods_schema.sql",
                "V20260929_001__initialize_catalog_lookup_schema.sql")) {
            String ddl = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/resources/db/egon-mp", resource));
            java.util.regex.Matcher tables = java.util.regex.Pattern.compile("CREATE TABLE [a-z_]+ \\(.*?\\)\\s*;",
                    java.util.regex.Pattern.DOTALL).matcher(ddl);
            while (tables.find()) {
                jdbc.execute(tables.group());
            }
        }
        jdbc.update("INSERT INTO brand(id,tenant_id,name,image,letter,seq) VALUES(1,1,'品牌','','B',0)");
        jdbc.update("INSERT INTO template(id,tenant_id,name) VALUES(5,1,'模板')");
        jdbc.update("INSERT INTO category(id,tenant_id,name,parent_id,template_id) VALUES(2,1,'一级',NULL,NULL)");
        jdbc.update("INSERT INTO category(id,tenant_id,name,parent_id,template_id) VALUES(3,1,'二级',2,NULL)");
        jdbc.update("INSERT INTO category(id,tenant_id,name,parent_id,template_id) VALUES(4,1,'手机',3,5)");
    }

    @Test
    void createAndEditUseOneAggregateWithStableSkuIdentity() {
        ProductBO created = service.create(product("SPU-A", "SKU-A"));
        assertThat(created.getSpu().getId()).isPositive();
        assertThat(created.getSpu().getVersion()).isZero();
        assertThat(created.getSpu().getStatus()).isEqualTo("0");
        assertThat(created.getSpu().getIsMarketable()).isFalse();
        assertThat(created.getSkuList().getFirst().getName()).isEqualTo("手机");
        ProductBO update = product("SPU-A", "SKU-A").setExpectedVersion(0L);
        update.getSpu().setId(created.getSpu().getId()).setName("新手机");
        update.getSkuList().getFirst().setId(created.getSkuList().getFirst().getId()).setPrice(200);
        ProductBO changed = service.update(update);
        assertThat(changed.getSpu().getVersion()).isEqualTo(1L);
        assertThat(changed.getSkuList().getFirst().getId()).isEqualTo(created.getSkuList().getFirst().getId());
        assertThat(changed.getSkuList().getFirst().getCreateTime()).isEqualTo(created.getSkuList().getFirst().getCreateTime());
        ProductBO detail = service.detail(ProductDomainQuery.builder().id(created.getSpu().getId()).build());
        assertThat(detail.getSkuList().getFirst().getPrice()).isEqualTo(200);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM category_brand", Integer.class)).isEqualTo(1);
    }

    @Test
    void aSkuInsertFailureRollsBackParentAndNewCatalogAssociation() {
        jdbc.execute("ALTER TABLE sku ADD CONSTRAINT forced_child_failure CHECK (sn <> 'SKU-B')");
        assertThatThrownBy(() -> service.create(product("SPU-B", "SKU-B")))
                .isInstanceOf(RuntimeException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM spu", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sku", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM category_brand", Integer.class)).isZero();
    }

    @Test
    void lifecycleKeepsEditHistoryDeletedAndPurgesAllHistory() {
        ProductBO created = service.create(product("SPU-C", "SKU-C"));
        Long id = created.getSpu().getId();
        Long originalSku = created.getSkuList().getFirst().getId();
        ProductBO replacement = product("SPU-C", "SKU-D").setExpectedVersion(0L);
        replacement.getSpu().setId(id);
        service.update(replacement);
        assertThat(service.audit(id, 1L).getMarketable()).isTrue();
        assertThatThrownBy(() -> service.delete(id, 2L)).isInstanceOf(RuntimeException.class);
        service.pull(id, 2L);
        assertThat(service.delete(id, 3L)).isTrue();
        ProductBO recycled = service.detail(ProductDomainQuery.builder().id(id).deleted(true).build());
        assertThat(recycled.getSkuList()).hasSize(1);
        assertThat(recycled.getSkuList().getFirst().getId()).isNotEqualTo(originalSku);
        service.restore(id, 4L);
        assertThat(service.detail(ProductDomainQuery.builder().id(id).build()).getSkuList()).hasSize(1);
        assertThat(jdbc.queryForObject("SELECT deletion_kind FROM sku WHERE id=?", String.class, originalSku)).isEqualTo("E");
        service.delete(id, 5L);
        assertThat(service.purge(id, 6L)).isTrue();
        assertThat(service.purge(id, 6L)).isTrue();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sku WHERE spu_id=?", Integer.class, id)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM spu", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM category_brand", Integer.class)).isEqualTo(1);
    }

    @Test
    void restoreCollisionRollsBackParentAndLeavesAllChildrenRecycled() {
        ProductBO created = service.create(product("SPU-R", "SKU-R"));
        Long id = created.getSpu().getId();
        service.delete(id, 0L);
        service.create(product("SPU-OTHER", "SKU-R"));
        assertThatThrownBy(() -> service.restore(id, 1L)).isInstanceOf(RuntimeException.class);
        assertThat(service.detail(ProductDomainQuery.builder().id(id).deleted(true).build()).getSkuList()).hasSize(1);
        assertThat(jdbc.queryForObject("SELECT version FROM spu WHERE id=?", Long.class, id)).isEqualTo(1L);
        assertThat(jdbc.queryForObject("SELECT deleted_at FROM spu WHERE id=?", java.time.LocalDateTime.class, id)).isNotNull();
    }

    @Test
    void auditAndEditWithOneVersionCannotBothCommit() throws Exception {
        ProductBO created = service.create(product("SPU-RACE", "SKU-RACE"));
        Long id = created.getSpu().getId();
        ProductBO edit = product("SPU-RACE", "SKU-RACE").setExpectedVersion(0L);
        edit.getSpu().setId(id).setName("编辑胜出");
        edit.getSkuList().getFirst().setId(created.getSkuList().getFirst().getId());
        var ready = new java.util.concurrent.CountDownLatch(2);
        var start = new java.util.concurrent.CountDownLatch(1);
        try (var workers = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<Boolean> audit = () -> race(ready, start, () -> service.audit(id, 0L));
            java.util.concurrent.Callable<Boolean> update = () -> race(ready, start, () -> service.update(edit));
            var first = workers.submit(audit); var second = workers.submit(update);
            assertThat(ready.await(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            start.countDown();
            int successes = (first.get(10, java.util.concurrent.TimeUnit.SECONDS) ? 1 : 0)
                    + (second.get(10, java.util.concurrent.TimeUnit.SECONDS) ? 1 : 0);
            assertThat(successes).isEqualTo(1);
        }
        ProductBO actual = service.detail(ProductDomainQuery.builder().id(id).build());
        assertThat(actual.getSpu().getVersion()).isEqualTo(1L);
        if (actual.getSpu().getIsMarketable()) {
            assertThat(actual.getSkuList().getFirst().getVersion()).isZero();
            assertThat(actual.getSpu().getName()).isEqualTo("手机");
        } else {
            assertThat(actual.getSkuList().getFirst().getVersion()).isEqualTo(1L);
            assertThat(actual.getSkuList().getFirst().getName()).isEqualTo("编辑胜出");
        }
    }

    private static boolean race(java.util.concurrent.CountDownLatch ready, java.util.concurrent.CountDownLatch start,
            java.util.concurrent.Callable<?> operation) throws Exception {
        MDC.put("tenantId", "1"); MDC.put("userId", "race-editor");
        try {
            ready.countDown();
            if (!start.await(5, java.util.concurrent.TimeUnit.SECONDS)) { throw new AssertionError("race did not start"); }
            operation.call(); return true;
        } catch (top.egon.cola.component.common.core.exception.CommonException rejected) {
            assertThat(rejected.getCode()).isEqualTo(610203); return false;
        } finally { MDC.clear(); }
    }

    @Test
    void currentVersionNoOpDoesNotWriteAndStaleVersionStillFails() {
        Long id = service.create(product("SPU-NOOP", "SKU-NOOP")).getSpu().getId();
        service.audit(id, 0L);
        assertThat(service.audit(id, 1L).getVersion()).isEqualTo(1L);
        assertThatThrownBy(() -> service.audit(id, 0L)).isInstanceOf(top.egon.cola.component.common.core.exception.CommonException.class);
        service.pull(id, 1L);
        assertThat(service.pull(id, 2L).getVersion()).isEqualTo(2L);
        service.put(id, 2L);
        assertThat(service.put(id, 3L).getVersion()).isEqualTo(3L);
    }

    @Test
    void aParentPurgeFailureRollsBackAlreadyDeletedChildren() {
        Long id = service.create(product("SPU-FAIL", "SKU-FAIL")).getSpu().getId();
        service.delete(id, 0L);
        jdbc.execute("CREATE TABLE purge_blocker(parent_id BIGINT REFERENCES spu(id))");
        jdbc.update("INSERT INTO purge_blocker VALUES(?)", id);
        assertThatThrownBy(() -> service.purge(id, 1L)).isInstanceOf(RuntimeException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sku WHERE spu_id=?", Integer.class, id)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT version FROM spu WHERE id=?", Long.class, id)).isEqualTo(1L);
    }

    private static ProductBO product(String sn, String barcode) {
        return ProductBO.builder().spu(SpuBO.builder().sn(sn).name("手机").brandId(1L)
                        .category1Id(2L).category2Id(3L).category3Id(4L).templateId(5L).isEnableSpec(false).build())
                .skuList(List.of(SkuBO.builder().sn(barcode).price(100).num(5).alertNum(1).weight(10).build())).build();
    }
}
