package top.egon.enjoyshop.cart.application.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class LocalTransactionBoundaryTest {

    @Test
    void sampleManageTypesAreGoneBeforeCartManageExists() throws IOException {
        String sourceTree = readTree(Path.of("src/main/java"));
        assertThat(sourceTree).doesNotContain("class UserManageImpl");
        assertThat(sourceTree).doesNotContain("class CourseManageImpl");
        assertThat(Path.of(
                "src/main/java/top/egon/enjoyshop/cart/application/cart/CartAuthorizationComponent.java")).exists();
    }

    private static String readTree(Path root) throws IOException {
        try (Stream<Path> paths = Files.walk(root)) {
            return paths.filter(path -> path.toString().endsWith(".java"))
                    .map(LocalTransactionBoundaryTest::read)
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
