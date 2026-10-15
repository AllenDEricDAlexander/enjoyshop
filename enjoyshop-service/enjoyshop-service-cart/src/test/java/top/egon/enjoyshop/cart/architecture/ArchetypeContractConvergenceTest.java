package top.egon.enjoyshop.cart.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Cart boundary replaces the light user and course archetype contract. */
class ArchetypeContractConvergenceTest {

    @Test
    void sampleControllersAreGoneAndCartInfrastructureRemains() throws IOException {
        String source = readTree(Path.of("src/main/java"));
        assertThat(source).doesNotContain("UserController");
        assertThat(source).doesNotContain("CourseController");
        assertThat(Files.exists(Path.of(
                "src/main/java/top/egon/enjoyshop/cart/infrastructure/config/RedisConfig.java"))).isTrue();
        assertThat(Files.exists(Path.of(
                "src/main/java/top/egon/enjoyshop/cart/start/config/NativeHttpSecurityConfiguration.java"))).isTrue();
        assertThat(Files.readString(Path.of(
                "src/main/java/top/egon/enjoyshop/cart/start/StudentManagementApplication.java")))
                .contains("top.egon.enjoyshop.cart.infrastructure.cart.dao");
    }

    private static String readTree(Path root) throws IOException {
        try (Stream<Path> paths = Files.walk(root)) {
            return paths.filter(path -> path.toString().endsWith(".java"))
                    .map(ArchetypeContractConvergenceTest::read)
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
