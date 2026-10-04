package top.egon.enjoyshop.goods.infrastructure.goods.repo;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import top.egon.enjoyshop.goods.domain.goods.SpecBO;
import top.egon.enjoyshop.goods.domain.goods.SpecDomainQuery;
import top.egon.enjoyshop.goods.infrastructure.goods.po.SpecPO;
import top.egon.enjoyshop.goods.infrastructure.goods.service.impl.SpecPersistenceConverter;
import top.egon.enjoyshop.goods.start.StudentManagementApplication;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(classes = StudentManagementApplication.class)
class SpecQueryRepositoryTest extends top.egon.enjoyshop.goods.support.PersistenceTestSupport {

    @Autowired
    @Qualifier("specRepository")
    private SpecRepository repository;

    private final SpecPersistenceConverter converter = Mappers.getMapper(SpecPersistenceConverter.class);

    private JdbcTemplate jdbc;

    @BeforeEach
    void createCatalogFixture() {
        jdbc = new JdbcTemplate(testDataSource);
        jdbc.execute("CREATE TABLE category (id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL, name VARCHAR(120) NOT NULL, template_id BIGINT NOT NULL, deleted_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE template (id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL, deleted_at TIMESTAMP)");
        jdbc.execute("""
                CREATE TABLE spec (
                    id BIGINT PRIMARY KEY,
                    tenant_id BIGINT NOT NULL,
                    create_user_id VARCHAR(128) NOT NULL,
                    create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL,
                    update_user_id VARCHAR(128) NOT NULL,
                    update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL,
                    deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
                    version BIGINT NOT NULL,
                    name VARCHAR(120) NOT NULL,
                    options TEXT NOT NULL,
                    seq INTEGER NOT NULL,
                    template_id BIGINT NOT NULL
                )
                """);

        insertTemplate(301, 41, null);
        insertTemplate(302, 41, null);
        insertTemplate(303, 42, null);
        insertTemplate(304, 41, LocalDateTime.parse("2026-01-01T00:00:00"));

        insertCategory(101, 41, "phones", 301, null);
        insertCategory(102, 41, "phones", 301, null);
        insertCategory(103, 41, "phones", 302, LocalDateTime.parse("2026-01-01T00:00:00"));
        insertCategory(104, 42, "phones", 303, null);
        insertCategory(105, 41, "phone-cases", 301, null);
        insertCategory(106, 41, "phones", 304, null);

        insertSpec(401, 41, 301, "Capacity", "64,128", 10, null);
        insertSpec(402, 41, 301, "Color", "Black,White", 10, null);
        insertSpec(403, 41, 301, "Size", "S,M,L", 20, null);
        insertSpec(404, 41, 302, "Deleted category", "unused", 1, null);
        insertSpec(405, 42, 303, "Other tenant", "unused", 1, null);
        insertSpec(406, 41, 304, "Deleted template", "unused", 1, null);
        insertSpec(407, 41, 301, "Deleted spec", "unused", 1,
                LocalDateTime.parse("2026-01-01T00:00:00"));
    }

    @Test
    void same_name_categories_union_active_rows_and_deduplicate_shared_templates() {
        try (var tenant = MDC.putCloseable("tenantId", "41")) {
            var rows = repository.listByCategoryName(query("phones"));

            assertThat(rows).extracting(SpecPO::getId).containsExactly(401L, 402L, 403L);
            assertThat(rows).extracting(SpecPO::getOptions).containsExactly("64,128", "Black,White", "S,M,L");
            assertThat(converter.toTargetList(rows))
                    .extracting(SpecBO::getOptions)
                    .containsExactly(List.of("64", "128"), List.of("Black", "White"), List.of("S", "M", "L"));
        }
    }

    @Test
    void options_projection_preserves_direct_split_order_and_empty_tokens() {
        assertThat(SpecPersistenceConverter.splitOptions(null)).isEmpty();
        assertThat(SpecPersistenceConverter.splitOptions("")).isEmpty();
        assertThat(SpecPersistenceConverter.splitOptions("A,,B,"))
                .containsExactly("A", "", "B");
        assertThat(SpecPersistenceConverter.splitOptions(" A , B "))
                .containsExactly(" A ", " B ");
    }

    @Test
    void category_match_is_exact_and_no_match_returns_empty_rows() {
        try (var tenant = MDC.putCloseable("tenantId", "41")) {
            assertThat(repository.listByCategoryName(query("Phones"))).isEmpty();
            assertThat(repository.listByCategoryName(query("phone"))).isEmpty();
        }
    }

    private void insertTemplate(long id, long tenantId, LocalDateTime deletedAt) {
        jdbc.update("INSERT INTO template (id, tenant_id, deleted_at) VALUES (?, ?, ?)", id, tenantId, deletedAt);
    }

    private void insertCategory(long id, long tenantId, String name, long templateId, LocalDateTime deletedAt) {
        jdbc.update("INSERT INTO category (id, tenant_id, name, template_id, deleted_at) VALUES (?, ?, ?, ?, ?)",
                id, tenantId, name, templateId, deletedAt);
    }

    private void insertSpec(long id, long tenantId, long templateId, String name, String options, int seq,
                            LocalDateTime deletedAt) {
        jdbc.update("""
                        INSERT INTO spec (id, tenant_id, create_user_id, create_time, update_user_id, update_time,
                                          deleted_at, version, name, options, seq, template_id)
                        VALUES (?, ?, 'test', CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, ?, 0, ?, ?, ?, ?)
                        """,
                id, tenantId, deletedAt, name, options, seq, templateId);
    }

    private static SpecDomainQuery query(String categoryName) {
        return new SpecDomainQuery().setCategoryName(categoryName);
    }
}
