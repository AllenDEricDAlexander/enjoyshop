package top.egon.enjoyshop.cart.start.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class NativeHttpCompatibilityTest {

    @Test
    void cartRoutesStayOnTheCartSecurityChain() throws Exception {
        String securitySource = Files.readString(Path.of(
                "src/main/java/top/egon/enjoyshop/cart/start/config/NativeHttpSecurityConfiguration.java"));
        assertThat(securitySource).contains("cartSecurityFilterChain");
        assertThat(securitySource).contains("/api/v1/cart");
        assertThat(securitySource).doesNotContain("/api/users");
    }
}
