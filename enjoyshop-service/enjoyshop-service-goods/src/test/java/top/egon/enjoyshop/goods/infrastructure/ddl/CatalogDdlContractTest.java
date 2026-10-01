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
import org.junit.jupiter.api.Test;

class CatalogDdlContractTest {

    private static final Path DDL_PATH = Path.of(
            "src/main/resources/db/egon-mp/V20260929_001__initialize_catalog_lookup_schema.sql");
    private static final Path MANIFEST_PATH = Path.of(
            "src/main/resources/db/egon-mp/repository-manifest.json");
    private static final Pattern CREATE_TABLE = Pattern.compile(
            "CREATE\\s+TABLE\\s+([a-z][a-z0-9_]*)\\s*\\((.*?)\\)\\s*;",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    @Test
    void shouldDefineTheCatalogTablesAndEgonModelColumnsInOneManagedVersion() throws Exception {
        assertThat(DDL_PATH).exists();
        String ddl = Files.readString(DDL_PATH, StandardCharsets.UTF_8);
        Map<String, String> tableBodies = CREATE_TABLE.matcher(ddl).results()
                .collect(java.util.stream.Collectors.toMap(
                        match -> match.group(1).toLowerCase(Locale.ROOT),
                        match -> normalize(match.group(2))));

        assertThat(tableBodies.keySet())
                .containsExactlyInAnyOrder("category", "template", "spec", "para", "category_brand");
        for (Map.Entry<String, String> table : tableBodies.entrySet()) {
            assertThat(table.getValue())
                    .as("EgonModel columns for %s", table.getKey())
                    .contains(
                            "ID BIGINT PRIMARY KEY",
                            "TENANT_ID BIGINT NOT NULL",
                            "CREATE_USER_ID VARCHAR(128) NOT NULL DEFAULT 'MIGRATION'",
                            "CREATE_TIME TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP",
                            "UPDATE_USER_ID VARCHAR(128) NOT NULL DEFAULT 'MIGRATION'",
                            "UPDATE_TIME TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP",
                            "DELETED_AT TIMESTAMP(6) WITHOUT TIME ZONE",
                            "VERSION BIGINT NOT NULL DEFAULT 0 CHECK (VERSION >= 0)");
        }

        assertThat(tableBodies.get("category"))
                .contains("NAME VARCHAR(120) NOT NULL", "GOODS_NUM INTEGER NOT NULL DEFAULT 0 CHECK (GOODS_NUM >= 0)",
                        "IS_SHOW CHAR(1) NOT NULL DEFAULT '1' CHECK (IS_SHOW IN ('0', '1'))",
                        "IS_MENU CHAR(1) NOT NULL DEFAULT '0' CHECK (IS_MENU IN ('0', '1'))",
                        "SEQ INTEGER NOT NULL DEFAULT 0 CHECK (SEQ >= 0)",
                        "PARENT_ID BIGINT", "TEMPLATE_ID BIGINT")
                .doesNotContain("PARENT_ID BIGINT NOT NULL", "TEMPLATE_ID BIGINT NOT NULL");
        assertThat(tableBodies.get("template"))
                .contains("NAME VARCHAR(120) NOT NULL", "SPEC_NUM INTEGER NOT NULL DEFAULT 0 CHECK (SPEC_NUM >= 0)",
                        "PARA_NUM INTEGER NOT NULL DEFAULT 0 CHECK (PARA_NUM >= 0)");
        assertThat(tableBodies.get("spec"))
                .contains("NAME VARCHAR(120) NOT NULL", "OPTIONS TEXT NOT NULL",
                        "SEQ INTEGER NOT NULL DEFAULT 0", "TEMPLATE_ID BIGINT NOT NULL");
        assertThat(tableBodies.get("para"))
                .contains("NAME VARCHAR(120) NOT NULL", "OPTIONS TEXT NOT NULL",
                        "SEQ INTEGER NOT NULL DEFAULT 0", "TEMPLATE_ID BIGINT NOT NULL");
        assertThat(tableBodies.get("category_brand"))
                .contains("CATEGORY_ID BIGINT NOT NULL", "BRAND_ID BIGINT NOT NULL");
    }

    @Test
    void shouldKeepCatalogForeignKeysNonCascadingAndProtectActiveLinkUniqueness() throws Exception {
        assertThat(DDL_PATH).exists();
        String ddl = normalize(Files.readString(DDL_PATH, StandardCharsets.UTF_8));

        assertThat(ddl)
                .contains("CONSTRAINT FK_CATEGORY_PARENT FOREIGN KEY (PARENT_ID) REFERENCES CATEGORY (ID) ON DELETE RESTRICT",
                        "CONSTRAINT FK_CATEGORY_TEMPLATE FOREIGN KEY (TEMPLATE_ID) REFERENCES TEMPLATE (ID) ON DELETE RESTRICT",
                        "CONSTRAINT FK_SPEC_TEMPLATE FOREIGN KEY (TEMPLATE_ID) REFERENCES TEMPLATE (ID) ON DELETE RESTRICT",
                        "CONSTRAINT FK_PARA_TEMPLATE FOREIGN KEY (TEMPLATE_ID) REFERENCES TEMPLATE (ID) ON DELETE RESTRICT",
                        "CONSTRAINT FK_CATEGORY_BRAND_CATEGORY FOREIGN KEY (CATEGORY_ID) REFERENCES CATEGORY (ID) ON DELETE RESTRICT",
                        "CONSTRAINT FK_CATEGORY_BRAND_BRAND FOREIGN KEY (BRAND_ID) REFERENCES BRAND (ID) ON DELETE RESTRICT",
                        "CREATE INDEX IDX_CATEGORY_TENANT_NAME_ACTIVE ON CATEGORY (TENANT_ID, NAME, ID) WHERE DELETED_AT IS NULL",
                        "CREATE INDEX IDX_SPEC_TENANT_TEMPLATE_SEQ_ID_ACTIVE ON SPEC (TENANT_ID, TEMPLATE_ID, SEQ, ID) WHERE DELETED_AT IS NULL",
                        "CREATE UNIQUE INDEX UK_CATEGORY_BRAND_LIFECYCLE ON CATEGORY_BRAND (TENANT_ID, CATEGORY_ID, BRAND_ID, DELETED_AT)",
                        "CREATE UNIQUE INDEX UK_CATEGORY_BRAND_ACTIVE ON CATEGORY_BRAND (TENANT_ID, CATEGORY_ID, BRAND_ID) WHERE DELETED_AT IS NULL")
                .doesNotContain("ON DELETE CASCADE", "ALTER TABLE BRAND", "DROP TABLE BRAND",
                        "INSERT INTO", "UPDATE BRAND", "DELETE FROM BRAND");
    }

    @Test
    void shouldAppendOnlyTheNextManifestVersionWithTheExactSqlChecksum() throws Exception {
        assertThat(DDL_PATH).exists();
        assertThat(MANIFEST_PATH).exists();
        byte[] ddlBytes = Files.readAllBytes(DDL_PATH);
        JsonNode manifest = new ObjectMapper().readTree(MANIFEST_PATH.toFile());
        List<JsonNode> scripts = StreamSupport.stream(manifest.path("scripts").spliterator(), false).toList();

        assertThat(manifest.path("family").asText()).isEqualTo("light");
        assertThat(scripts).hasSize(3);
        assertManifestEntry(scripts.get(0), "20260913_001",
                "db/egon-mp/V20260913_001__initialize_repository_schema.sql",
                "e4936cb2a7943d50cd4b7d7b49252d9712513a2f27ade70f125d2bf8f335ea31");
        assertManifestEntry(scripts.get(1), "20260923_001",
                "db/egon-mp/V20260923_001__initialize_goods_schema.sql",
                "1904922e16dc9d2c2d56afb8a9cafb3fff2e4291cf20774f47a64a2ab5e44a17");
        assertManifestEntry(scripts.get(2), "20260929_001",
                "db/egon-mp/V20260929_001__initialize_catalog_lookup_schema.sql",
                sha256(ddlBytes));
    }

    private static void assertManifestEntry(JsonNode entry, String version, String path, String sha256) {
        assertThat(entry.path("version").asText()).isEqualTo(version);
        assertThat(entry.path("path").asText()).isEqualTo(path);
        assertThat(entry.path("sha256").asText()).isEqualTo(sha256);
    }

    private static String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private static String normalize(String value) {
        return value.replaceAll("\\s+", " ").trim().toUpperCase(Locale.ROOT);
    }
}
