package top.egon.enjoyshop.goods.infrastructure.ddl;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationGroups;
import top.egon.enjoyshop.goods.infrastructure.goods.po.SkuPO;

/** Source-only contract; PostgreSQL execution and concurrency require separate acceptance. */
class ProductDdlContractTest {

    private static final Path DIRECTORY = Path.of("src/main/resources/db/egon-mp");
    private static final String PRODUCT_SCRIPT = "V20261009_001__initialize_product_schema.sql";
    private static final Pattern CREATE_TABLE = Pattern.compile(
            "CREATE\\s+TABLE\\s+([a-z][a-z0-9_]*)\\s*\\((.*?)\\)\\s*;",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Map<String, String> HISTORICAL_CHECKSUMS = Map.of(
            "V20260913_001__initialize_repository_schema.sql",
            "e4936cb2a7943d50cd4b7d7b49252d9712513a2f27ade70f125d2bf8f335ea31",
            "V20260923_001__initialize_goods_schema.sql",
            "1904922e16dc9d2c2d56afb8a9cafb3fff2e4291cf20774f47a64a2ab5e44a17",
            "V20260929_001__initialize_catalog_lookup_schema.sql",
            "6db577031d1adb40dc83ca5c187345c3ad0cb94d6e446a877163e5755280bd83");

    @Test
    void shouldDefineOnlyProductTablesWithInheritedLifecycleColumns() throws Exception {
        Path script = DIRECTORY.resolve(PRODUCT_SCRIPT);
        assertThat(script).exists();
        String ddl = Files.readString(script, StandardCharsets.UTF_8);
        Map<String, String> tables = CREATE_TABLE.matcher(ddl).results()
                .collect(java.util.stream.Collectors.toMap(
                        match -> match.group(1), match -> normalize(match.group(2))));
        assertThat(tables.keySet()).containsExactlyInAnyOrder("spu", "sku");
        for (String body : tables.values()) {
            assertThat(body).contains("ID BIGINT PRIMARY KEY", "TENANT_ID BIGINT NOT NULL",
                    "CREATE_USER_ID VARCHAR(128) NOT NULL", "UPDATE_USER_ID VARCHAR(128) NOT NULL",
                    "CREATE_TIME TIMESTAMP(6) WITH TIME ZONE NOT NULL",
                    "UPDATE_TIME TIMESTAMP(6) WITH TIME ZONE NOT NULL",
                    "DELETED_AT TIMESTAMP(6) WITHOUT TIME ZONE", "VERSION BIGINT NOT NULL DEFAULT 0");
        }
        assertThat(tables.get("spu")).contains("SN VARCHAR(64) NOT NULL", "NAME VARCHAR(200) NOT NULL",
                "BRAND_ID BIGINT NOT NULL", "CATEGORY1_ID BIGINT NOT NULL", "CATEGORY2_ID BIGINT NOT NULL",
                "CATEGORY3_ID BIGINT NOT NULL", "TEMPLATE_ID BIGINT NOT NULL", "FREIGHT_ID BIGINT NOT NULL DEFAULT 0",
                "IMAGES TEXT NOT NULL", "SALE_SERVICE TEXT NOT NULL", "SPEC_ITEMS TEXT NOT NULL",
                "PARA_ITEMS TEXT NOT NULL", "IS_MARKETABLE BOOLEAN NOT NULL DEFAULT FALSE",
                "IS_ENABLE_SPEC BOOLEAN NOT NULL").doesNotContain("IS_DELETE");
        assertThat(tables.get("sku")).contains("SPU_ID BIGINT NOT NULL", "SN VARCHAR(64) NOT NULL",
                "PRICE INTEGER NOT NULL", "NUM INTEGER NOT NULL", "ALERT_NUM INTEGER NOT NULL",
                "WEIGHT INTEGER NOT NULL", "SPEC TEXT NOT NULL", "DELETION_KIND CHAR(1)");
    }

    @Test
    void shouldProtectActiveKeysAndProductLifecycleWithoutCascades() throws Exception {
        Path script = DIRECTORY.resolve(PRODUCT_SCRIPT);
        assertThat(script).exists();
        String ddl = normalize(Files.readString(script, StandardCharsets.UTF_8));
        assertThat(ddl).contains("CURRENT_SETTING('EGON_MIGRATION.ROLE')", "'MASTER_DATA'", "'SHARD'",
                "RAISE EXCEPTION", "FOREIGN KEY (SPU_ID) REFERENCES SPU (ID) ON DELETE RESTRICT",
                "FOREIGN KEY (BRAND_ID) REFERENCES BRAND (ID) ON DELETE RESTRICT",
                "FOREIGN KEY (TEMPLATE_ID) REFERENCES TEMPLATE (ID) ON DELETE RESTRICT",
                "CREATE UNIQUE INDEX UK_SPU_LIFECYCLE ON SPU (TENANT_ID, SN, DELETED_AT)",
                "CREATE UNIQUE INDEX UK_SPU_ACTIVE ON SPU (TENANT_ID, SN) WHERE DELETED_AT IS NULL",
                "CREATE UNIQUE INDEX UK_SKU_LIFECYCLE ON SKU (TENANT_ID, SN, DELETED_AT)",
                "CREATE UNIQUE INDEX UK_SKU_ACTIVE ON SKU (TENANT_ID, SN) WHERE DELETED_AT IS NULL",
                "CREATE INDEX IDX_SPU_ACTIVE_PAGE ON SPU (TENANT_ID, ID DESC) WHERE DELETED_AT IS NULL",
                "CREATE INDEX IDX_SPU_DELETED_PAGE ON SPU (TENANT_ID, ID DESC) WHERE DELETED_AT IS NOT NULL",
                "CREATE INDEX IDX_SKU_PARENT ON SKU (TENANT_ID, SPU_ID, ID)",
                "DELETION_KIND IN ('P', 'E')", "DELETION_KIND IS NULL", "DELETED_AT IS NOT NULL",
                "STATUS IN ('0', '1')", "STATUS IN ('1', '2')", "PRICE >= 1")
                .doesNotContain("ON DELETE CASCADE", "CREATE TABLE CATEGORY_BRAND", "DROP TABLE",
                        "ALTER TABLE", "INSERT INTO");
    }

    @Test
    void shouldAppendOneVersionAndPreserveAllHistoricalSqlBytes() throws Exception {
        Path script = DIRECTORY.resolve(PRODUCT_SCRIPT);
        assertThat(script).exists();
        JsonNode manifest = new ObjectMapper().readTree(DIRECTORY.resolve("repository-manifest.json").toFile());
        List<JsonNode> entries = StreamSupport.stream(manifest.path("scripts").spliterator(), false).toList();
        assertThat(entries).hasSize(5);
        for (int index = 0; index < 3; index++) {
            JsonNode entry = entries.get(index);
            String fileName = Path.of(entry.path("path").asText()).getFileName().toString();
            String expected = HISTORICAL_CHECKSUMS.get(fileName);
            assertThat(expected).as("historical manifest identity").isNotNull();
            assertThat(entry.path("sha256").asText()).isEqualTo(expected);
            assertThat(sha256(DIRECTORY.resolve(fileName))).isEqualTo(expected);
        }
        JsonNode product = entries.get(3);
        assertThat(product.path("version").asText()).isEqualTo("20261009_001");
        assertThat(product.path("path").asText()).isEqualTo("db/egon-mp/" + PRODUCT_SCRIPT);
        assertThat(product.path("sha256").asText()).isEqualTo(sha256(script));
        JsonNode advertisement = entries.get(4);
        String advertisementScript = "V20261010_001__initialize_advertisement_cdc.sql";
        assertThat(advertisement.path("version").asText()).isEqualTo("20261010_001");
        assertThat(advertisement.path("path").asText()).isEqualTo("db/egon-mp/" + advertisementScript);
        assertThat(advertisement.path("sha256").asText()).isEqualTo(sha256(DIRECTORY.resolve(advertisementScript)));
    }

    @Test
    void shouldRejectNegativeSkuValuesWithDatabaseShapeConstraints() {
        SkuPO value = SkuPO.builder().spuId(1L).sn("SKU-1").name("手机")
                .price(-1).num(-1).alertNum(0).weight(0).categoryId(2L)
                .categoryName("手机").brandName("品牌").build();
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validate(value, EgonColaModelValidationGroups.Insert.class))
                    .extracting(violation -> violation.getPropertyPath().toString())
                    .contains("price", "num");
            value.setPrice(1).setNum(0).setStatus("3");
            assertThat(factory.getValidator().validate(value, EgonColaModelValidationGroups.Insert.class))
                    .extracting(violation -> violation.getPropertyPath().toString())
                    .contains("status");
        }
    }

    private static String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim().toUpperCase(Locale.ROOT);
    }

    private static String sha256(Path path) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
    }
}
