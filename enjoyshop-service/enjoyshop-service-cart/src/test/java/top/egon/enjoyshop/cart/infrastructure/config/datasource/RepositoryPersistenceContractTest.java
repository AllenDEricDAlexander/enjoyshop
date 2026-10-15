package top.egon.enjoyshop.cart.infrastructure.config.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class RepositoryPersistenceContractTest {

    @Test
    void repositoryScriptDropsSampleTablesAndKeepsDdlHistory() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/egon-mp/V20260913_001__initialize_repository_schema.sql"));
        assertThat(sql).doesNotContain("CREATE TABLE users");
        assertThat(sql).doesNotContain("CREATE TABLE courses");
        assertThat(sql).contains("ddl_history");
    }
}
