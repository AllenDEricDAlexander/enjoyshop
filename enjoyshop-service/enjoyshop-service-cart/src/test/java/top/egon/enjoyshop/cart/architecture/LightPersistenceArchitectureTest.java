package architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Generated-project architecture contract for the Light MyBatis-Plus profile.
 */
class LightPersistenceArchitectureTest {

    @Test
    void generatedProjectUsesCommonPersistenceOwnership() throws IOException {
        String mainJava = readTree(Path.of("src/main/java"));
        assertThat(mainJava).doesNotContain("interface CourseRepository");
        assertThat(mainJava).doesNotContain("interface UserRepository");
        assertThat(mainJava).doesNotContain("jakarta.persistence");
        assertThat(mainJava).doesNotContain("JpaRepository");
        assertThat(mainJava).doesNotContain("EntityManager");
        assertThat(Path.of("src/main/java/top/egon/enjoyshop/cart/infrastructure/config/RedisConfig.java")).exists();
        assertThat(Path.of("src/main/resources/egon-mybatis-plus-sharding.yml")).exists();
        String pom = Files.readString(Path.of("pom.xml"));
        assertThat(pom).contains("egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter");
        assertThat(pom).doesNotContain("shardingsphere-jdbc");
    }

    @Test
    void generatedShardingConfigurationUsesTenantRouting() throws IOException {
        String sharding = Files.readString(Path.of("src/main/resources/egon-mybatis-plus-sharding.yml"));
        assertThat(sharding).contains("mybatis-plus:");
        assertThat(sharding).contains("STANDARD_TENANT_ID");
        assertThat(sharding).contains("tenant_id");
        assertThat(sharding).contains("type: SINGLE");
        assertThat(sharding).contains("transaction-default-type: LOCAL");
        assertThat(sharding).doesNotContain("UuidV7BucketShardingAlgorithm");
    }

    private static String readTree(Path root) throws IOException {
        try (Stream<Path> paths = Files.walk(root)) {
            return paths.filter(path -> path.toString().endsWith(".java"))
                    .map(LightPersistenceArchitectureTest::read)
                    .reduce("", String::concat);
        }
    }

    private static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException("cannot read " + path, exception);
        }
    }
}
