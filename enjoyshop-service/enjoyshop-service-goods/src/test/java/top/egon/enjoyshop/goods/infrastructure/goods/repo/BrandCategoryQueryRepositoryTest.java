package top.egon.enjoyshop.goods.infrastructure.goods.repo;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.enjoyshop.goods.domain.goods.BrandDomainQuery;
import top.egon.enjoyshop.goods.infrastructure.goods.po.BrandPO;
import top.egon.enjoyshop.goods.start.StudentManagementApplication;

@ActiveProfiles("test")
@SpringBootTest(classes = StudentManagementApplication.class)
class BrandCategoryQueryRepositoryTest extends top.egon.enjoyshop.goods.support.PersistenceTestSupport {

    @Autowired
    @Qualifier("brandRepository")
    private BrandRepository repository;

    private JdbcTemplate jdbc;

    @BeforeEach
    void createCatalogFixture() {
        jdbc = new JdbcTemplate(testDataSource);
        jdbc.execute("""
                CREATE TABLE brand (
                    id BIGINT PRIMARY KEY,
                    tenant_id BIGINT NOT NULL,
                    create_user_id VARCHAR(128) NOT NULL,
                    create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL,
                    update_user_id VARCHAR(128) NOT NULL,
                    update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL,
                    deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
                    version BIGINT NOT NULL,
                    name VARCHAR(120) NOT NULL,
                    image VARCHAR(500),
                    letter CHAR(1) NOT NULL,
                    seq INTEGER NOT NULL
                )
                """);
        jdbc.execute("CREATE TABLE category (id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL, name VARCHAR(120) NOT NULL, deleted_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE category_brand (id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL, category_id BIGINT NOT NULL, brand_id BIGINT NOT NULL, deleted_at TIMESTAMP)");

        insertBrand(1, 41, "Apple", 100, null);
        insertBrand(2, 41, "Samsung", 100, null);
        insertBrand(3, 41, "Dell", 70, null);
        insertBrand(4, 41, "Nokia", 60, null);
        insertBrand(5, 41, "Removed", 50, LocalDateTime.parse("2026-01-01T00:00:00"));
        insertBrand(6, 42, "Other tenant", 110, null);

        jdbc.update("INSERT INTO category (id, tenant_id, name) VALUES (101, 41, 'phones')");
        jdbc.update("INSERT INTO category (id, tenant_id, name) VALUES (102, 41, 'phones')");
        jdbc.update("INSERT INTO category (id, tenant_id, name, deleted_at) VALUES (103, 41, 'phones', TIMESTAMP '2026-01-01 00:00:00')");
        jdbc.update("INSERT INTO category (id, tenant_id, name) VALUES (104, 42, 'phones')");
        jdbc.update("INSERT INTO category (id, tenant_id, name) VALUES (105, 41, 'tablets')");

        insertLink(201, 41, 101, 1, null);
        insertLink(202, 41, 102, 1, null);
        insertLink(203, 41, 102, 2, null);
        insertLink(204, 41, 103, 3, null);
        insertLink(205, 41, 101, 4, LocalDateTime.parse("2026-01-01T00:00:00"));
        insertLink(206, 41, 101, 5, null);
        insertLink(207, 42, 104, 6, null);
        insertLink(208, 41, 105, 3, null);
    }

    @Test
    void categoryFilterUnionsSameNameCategoriesAndCountsDistinctActiveTenantBrands() {
        try (var tenant = MDC.putCloseable("tenantId", "41")) {
            BrandDomainQuery query = query("phones", 1, 10);

            var page = repository.selectBrandPage(query, null, null);
            long count = repository.countBrandPage(query, null, null);

            assertThat(page).extracting(BrandPO::getId).containsExactly(2L, 1L);
            assertThat(count).isEqualTo(2L);
        }
    }

    @Test
    void categoryFilterCombinesWithExistingNameAndSequenceFilters() {
        try (var tenant = MDC.putCloseable("tenantId", "41")) {
            BrandDomainQuery query = query("phones", 1, 10).setName("Apple");

            var page = repository.selectBrandPage(query, 90, 101);
            long count = repository.countBrandPage(query, 90, 101);

            assertThat(page).extracting(BrandPO::getId).containsExactly(1L);
            assertThat(count).isEqualTo(1L);
        }
    }

    @Test
    void unmatchedCategoryReturnsAnEmptyPageAndZeroCount() {
        try (var tenant = MDC.putCloseable("tenantId", "41")) {
            BrandDomainQuery query = query("not-a-category", 1, 10);

            assertThat(repository.selectBrandPage(query, null, null)).isEmpty();
            assertThat(repository.countBrandPage(query, null, null)).isZero();
        }
    }

    @Test
    void absentCategoryNameRetainsTheExistingTenantScopedActiveBrandCollection() {
        try (var tenant = MDC.putCloseable("tenantId", "41")) {
            BrandDomainQuery query = query(null, 1, 10);

            assertThat(repository.selectBrandPage(query, null, null))
                    .extracting(BrandPO::getId)
                    .containsExactly(2L, 1L, 3L, 4L);
            assertThat(repository.countBrandPage(query, null, null)).isEqualTo(4L);
        }
    }

    private void insertBrand(long id, long tenantId, String name, int seq, LocalDateTime deletedAt) {
        jdbc.update("""
                        INSERT INTO brand (id, tenant_id, create_user_id, create_time, update_user_id, update_time,
                                           deleted_at, version, name, letter, seq)
                        VALUES (?, ?, 'test', CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, ?, 0, ?, 'A', ?)
                        """,
                id, tenantId, deletedAt, name, seq);
    }

    private void insertLink(long id, long tenantId, long categoryId, long brandId, LocalDateTime deletedAt) {
        jdbc.update("INSERT INTO category_brand (id, tenant_id, category_id, brand_id, deleted_at) VALUES (?, ?, ?, ?, ?)",
                id, tenantId, categoryId, brandId, deletedAt);
    }

    private static BrandDomainQuery query(String categoryName, int pageNo, int pageSize) {
        return BrandDomainQuery.builder()
                .categoryName(categoryName)
                .page(new PageQuery(pageNo, pageSize))
                .build();
    }
}
