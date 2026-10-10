package top.egon.enjoyshop.goods.infrastructure.ad;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import top.egon.cola.component.common.mybatis.extension.EgonColaRepository;
import top.egon.cola.component.common.mybatis.model.EgonModel;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.ParameterizedType;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/** Offline contract for the advertisement schema and generated persistence boundary. */
class AdPersistenceTest {

    private static final String MIGRATION_PATH =
            "db/egon-mp/V20261010_001__initialize_advertisement_cdc.sql";
    private static final String MANIFEST_PATH = "db/egon-mp/repository-manifest.json";
    private static final List<String> PRIOR_VERSIONS = List.of(
            "20260913_001", "20260923_001", "20260929_001", "20261009_001");
    private static final List<String> PRIOR_SHA256 = List.of(
            "e4936cb2a7943d50cd4b7d7b49252d9712513a2f27ade70f125d2bf8f335ea31",
            "1904922e16dc9d2c2d56afb8a9cafb3fff2e4291cf20774f47a64a2ab5e44a17",
            "6db577031d1adb40dc83ca5c187345c3ad0cb94d6e446a877163e5755280bd83",
            "db319455730c9d20b96401bbd7cdf0f488b23f8fa4efa15ccc4e1985e167c24c");
    private static final List<String> AD_COLUMNS = List.of(
            "id", "tenant_id", "create_user_id", "create_time", "update_user_id", "update_time",
            "deleted_at", "version", "name", "position", "image_url", "target_url", "status",
            "start_time", "end_time", "sort_order");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void singleNewMigration() throws Exception {
        byte[] migrationBytes = resourceBytes(MIGRATION_PATH);
        JsonNode scripts = objectMapper.readTree(resourceBytes(MANIFEST_PATH)).path("scripts");

        assertThat(scripts.isArray()).isTrue();
        assertThat(scripts.size()).isEqualTo(5);
        JsonNode latest = scripts.get(4);
        assertThat(latest.path("version").asText()).isEqualTo("20261010_001");
        assertThat(latest.path("path").asText()).isEqualTo(MIGRATION_PATH);
        assertThat(latest.path("sha256").asText()).isEqualTo(sha256(migrationBytes));

        String migration = new String(migrationBytes, StandardCharsets.UTF_8);
        assertThat(migration).contains("CREATE TABLE ad (")
                .contains("ALTER TABLE public.ad REPLICA IDENTITY FULL;")
                .contains("ALTER TABLE public.spu REPLICA IDENTITY FULL;")
                .contains("ALTER TABLE public.sku REPLICA IDENTITY FULL;");
    }

    @Test
    void inheritedFields() throws Exception {
        Class<?> adPo = Class.forName("top.egon.enjoyshop.goods.infrastructure.ad.po.AdPO");
        Class<?> adDao = Class.forName("top.egon.enjoyshop.goods.infrastructure.ad.dao.AdDAO");
        Class<?> adRepository = Class.forName("top.egon.enjoyshop.goods.infrastructure.ad.repo.AdRepository");
        Class<?> adBo = Class.forName("top.egon.enjoyshop.goods.domain.ad.AdBO");
        Class<?> adDomainQuery = Class.forName("top.egon.enjoyshop.goods.domain.ad.AdDomainQuery");

        assertThat(adPo.getSuperclass()).isEqualTo(EgonModel.class);
        Set<String> declaredPoFields = new TreeSet<>();
        for (var field : adPo.getDeclaredFields()) {
            if (!field.isSynthetic()) {
                declaredPoFields.add(field.getName());
            }
        }
        assertThat(declaredPoFields).containsExactlyInAnyOrder(
                "name", "position", "imageUrl", "targetUrl", "status", "startTime", "endTime", "sortOrder");
        assertThat(adPo.getDeclaredField("startTime").getType()).isEqualTo(Instant.class);
        assertThat(adPo.getDeclaredField("endTime").getType()).isEqualTo(Instant.class);
        assertThat(adPo.getDeclaredField("status").getType().getName())
                .isEqualTo("top.egon.enjoyshop.goods.domain.ad.AdStatusEnum");
        assertThat(adPo.getDeclaredField("status").getAnnotation(com.baomidou.mybatisplus.annotation.TableField.class)
                .value()).isEqualTo("status");

        assertThat(adRepository.getGenericSuperclass()).isInstanceOf(ParameterizedType.class);
        ParameterizedType repositoryType = (ParameterizedType) adRepository.getGenericSuperclass();
        assertThat(repositoryType.getRawType()).isEqualTo(EgonColaRepository.class);
        assertThat(repositoryType.getActualTypeArguments()).containsExactly(adDao, adPo);

        assertThat(adBo.getDeclaredField("startAt").getType()).isEqualTo(Instant.class);
        assertThat(adBo.getDeclaredField("endAt").getType()).isEqualTo(Instant.class);
        for (var field : adBo.getDeclaredFields()) {
            assertThat(field.getType().getName())
                    .as("Domain fields must not depend on persistence types")
                    .doesNotStartWith("top.egon.enjoyshop.goods.infrastructure.")
                    .doesNotStartWith("com.baomidou.mybatisplus.");
        }
        assertThat(adDomainQuery.getName()).startsWith("top.egon.enjoyshop.goods.domain.ad.");
    }

    @Test
    void noOldSqlDrift() throws Exception {
        JsonNode scripts = objectMapper.readTree(resourceBytes(MANIFEST_PATH)).path("scripts");
        assertThat(scripts.size()).isGreaterThanOrEqualTo(PRIOR_VERSIONS.size());
        for (int index = 0; index < PRIOR_VERSIONS.size(); index++) {
            JsonNode entry = scripts.get(index);
            String version = PRIOR_VERSIONS.get(index);
            String path = entry.path("path").asText();
            assertThat(entry.path("version").asText()).isEqualTo(version);
            assertThat(path).endsWith("V" + version + "__" + migrationSuffix(version));
            assertThat(entry.path("sha256").asText()).isEqualTo(PRIOR_SHA256.get(index));
            assertThat(sha256(resourceBytes(path))).isEqualTo(PRIOR_SHA256.get(index));
        }
    }

    @Test
    void tenantPredicates() throws Exception {
        String mapper = new String(resourceBytes("mybatis/mapper/ad/AdDAO.xml"), StandardCharsets.UTF_8);
        assertThat(mapper).contains(
                "<select id=\"selectByLifecycle\"",
                "<select id=\"lockByLifecycle\"",
                "<select id=\"selectPage\"",
                "<select id=\"countPage\"",
                "<select id=\"selectCandidates\"",
                "AND tenant_id = #{tenantId}",
                "AND deleted_at IS NULL");
        assertThat(mapper).contains("FOR UPDATE", "ORDER BY sort_order ASC, id ASC", "LIMIT #{limit}");
    }

    @Test
    void concurrentVersionContract() throws Exception {
        String mapper = new String(resourceBytes("mybatis/mapper/ad/AdDAO.xml"), StandardCharsets.UTF_8);
        assertThat(mapper).contains(
                "AND tenant_id = #{et.tenantId}",
                "AND deleted_at IS NULL\n          AND version = #{MP_OPTLOCK_VERSION_ORIGINAL}",
                "version = version + 1",
                "CURRENT_TIMESTAMP AT TIME ZONE 'UTC'");
    }

    @Test
    void terminalDeleteContract() throws Exception {
        String mapper = new String(resourceBytes("mybatis/mapper/ad/AdDAO.xml"), StandardCharsets.UTF_8);
        assertThat(mapper).contains(
                "<update id=\"deleteVersionedById\">",
                "UPDATE ad",
                "deleted_at = (CURRENT_TIMESTAMP AT TIME ZONE 'UTC')",
                "AND deleted_at IS NULL");
        assertThat(mapper).doesNotContain("DELETE FROM ad");
    }

    @Test
    void noUnscopedCandidates() throws Exception {
        String migration = new String(resourceBytes(MIGRATION_PATH), StandardCharsets.UTF_8);
        int masterDataGuard = migration.indexOf("IF current_setting('egon_migration.role') = 'MASTER_DATA' THEN");
        int createTable = migration.indexOf("CREATE TABLE ad (");
        int shardGuard = migration.indexOf("ELSIF current_setting('egon_migration.role') = 'SHARD' THEN");
        int unknownRole = migration.indexOf("RAISE EXCEPTION 'Unknown managed DDL role';");
        String adTable = tableDefinition(migration);

        assertThat(masterDataGuard).isGreaterThanOrEqualTo(0);
        assertThat(createTable).isGreaterThan(masterDataGuard);
        assertThat(shardGuard).isGreaterThan(createTable);
        assertThat(unknownRole).isGreaterThan(shardGuard);
        assertThat(Pattern.compile("(?m)^\\s{12}([a-z][a-z0-9_]*)\\s+")
                        .matcher(adTable).results().map(result -> result.group(1)).toList())
                .containsExactlyElementsOf(AD_COLUMNS);
        assertThat(adTable).contains(
                "tenant_id BIGINT NOT NULL",
                "CONSTRAINT ck_ad_tenant CHECK (tenant_id > 0)",
                "CONSTRAINT ck_ad_name CHECK (char_length(name) BETWEEN 1 AND 120 AND btrim(name) <> '')",
                "CONSTRAINT ck_ad_position CHECK (position ~ '^[a-z][a-z0-9_]{0,63}$')",
                "status CHAR(1) NOT NULL DEFAULT '0'",
                "CONSTRAINT ck_ad_status CHECK (status IN ('0', '1'))",
                "CONSTRAINT ck_ad_window CHECK (end_time > start_time)",
                "CONSTRAINT ck_ad_sort_order CHECK (sort_order BETWEEN 0 AND 1000000)");
        assertThat(adTable).doesNotContain("REFERENCES", "UNIQUE");
        assertThat(countOccurrences(migration, "CREATE INDEX idx_ad_")).isEqualTo(2);
        assertThat(migration).doesNotContain("CREATE UNIQUE INDEX");
        assertThat(countOccurrences(migration, "CREATE INDEX ")).isEqualTo(2);
        assertThat(migration).contains(
                "CREATE INDEX idx_ad_active_slot ON ad (tenant_id, position, sort_order, id) WHERE deleted_at IS NULL AND status = '1';",
                "CREATE INDEX idx_ad_admin_tenant_id ON ad (tenant_id, id DESC) WHERE deleted_at IS NULL;");
        assertThat(countOccurrences(migration, "REPLICA IDENTITY FULL")).isEqualTo(3);
        assertThat(migration).contains("-- SHARD has no advertisement tables in this release.");
    }

    private static byte[] resourceBytes(String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);
        assertThat(resource.exists()).as("classpath resource %s", path).isTrue();
        try (InputStream input = resource.getInputStream()) {
            return input.readAllBytes();
        }
    }

    private static String sha256(byte[] value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
    }

    private static String migrationSuffix(String version) {
        return switch (version) {
            case "20260913_001" -> "initialize_repository_schema.sql";
            case "20260923_001" -> "initialize_goods_schema.sql";
            case "20260929_001" -> "initialize_catalog_lookup_schema.sql";
            case "20261009_001" -> "initialize_product_schema.sql";
            default -> throw new IllegalArgumentException("unexpected historical version " + version);
        };
    }

    private static String tableDefinition(String migration) {
        int start = migration.indexOf("CREATE TABLE ad (");
        int end = migration.indexOf("\n        );", start);
        assertThat(start).isGreaterThanOrEqualTo(0);
        assertThat(end).isGreaterThan(start);
        return migration.substring(start, end + "\n        );".length());
    }

    private static long countOccurrences(String value, String needle) {
        long count = 0;
        int next = 0;
        while ((next = value.indexOf(needle, next)) >= 0) {
            count++;
            next += needle.length();
        }
        return count;
    }
}
