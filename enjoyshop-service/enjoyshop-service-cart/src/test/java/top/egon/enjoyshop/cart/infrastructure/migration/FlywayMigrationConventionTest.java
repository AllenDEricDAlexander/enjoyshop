package top.egon.enjoyshop.cart.infrastructure.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FlywayMigrationConventionTest {

    @Test
    void sampleShardingMigrationsAreGoneAndRepositoryScriptKeepsDdlHistory() throws Exception {
        assertThat(Path.of("src/main/resources/db/migration/sharding")).doesNotExist();
        String sql = Files.readString(Path.of(
                "src/main/resources/db/egon-mp/V20260913_001__initialize_repository_schema.sql"));
        assertThat(sql).contains("ddl_history");
        assertThat(sql).doesNotContain("CREATE TABLE users");
    }
}
