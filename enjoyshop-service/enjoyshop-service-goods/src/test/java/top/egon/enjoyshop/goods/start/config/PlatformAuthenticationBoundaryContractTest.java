package top.egon.enjoyshop.goods.start.config;

import jakarta.validation.constraints.Pattern;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import top.egon.enjoyshop.goods.application.ad.AdAuthorizationComponent;
import top.egon.enjoyshop.goods.application.goods.ProductAuthorizationComponent;
import top.egon.enjoyshop.goods.common.enums.GoodsErrorStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Offline platform-authentication boundary. Does not start the application, Redis, or Shoubing. */
class PlatformAuthenticationBoundaryContractTest {

    private static final java.util.regex.Pattern REST_CONTROLLER =
            java.util.regex.Pattern.compile("(?m)^\\s*@RestController(?!Advice)\\b");

    @Test
    void filterChainsMatchTheSpecRoutes() throws Exception {
        String security = Files.readString(Path.of(
                "src/main/java/top/egon/enjoyshop/goods/start/config/NativeHttpSecurityConfiguration.java"));
        assertThat(security).contains(
                ".formLogin(form -> form.disable())",
                ".httpBasic(basic -> basic.disable())",
                ".logout(logout -> logout.disable())");

        String products = methodBody(security, "productSecurityFilterChain");
        assertThat(products).contains("securityMatcher(\"/api/v1/products\", \"/api/v1/products/**\")");
        assertThat(products).contains(".anyRequest().authenticated()");
        assertThat(filterOrder(products)).isEqualTo("identity-before-rbac-before-anonymous");

        String ads = methodBody(security, "adSecurityFilterChain");
        assertThat(ads).contains("securityMatcher(\"/api/v1/ads\", \"/api/v1/ads/**\")");
        assertThat(ads).contains(".anyRequest().authenticated()");
        assertThat(filterOrder(ads)).isEqualTo("identity-before-rbac-before-anonymous");

        String upload = methodBody(security, "fileUploadSecurityFilterChain");
        assertThat(security).contains("FILE_UPLOAD_PATH = \"/api/v1/files\"");
        assertThat(upload).contains("AntPathRequestMatcher(FILE_UPLOAD_PATH, HttpMethod.POST.name())");
        assertThat(upload).contains("IdpBearerAuthenticationFilter");
        assertThat(upload).doesNotContain("Rbac3BearerAuthenticationFilter");

        String publicAds = methodBody(security, "publicAdSecurityFilterChain");
        assertThat(publicAds).contains("securityMatcher(\"/api/v1/ad-slots/**\")");
        assertThat(publicAds).contains("if (publicEnabled)");
        assertThat(publicAds).contains("requestMatchers(HttpMethod.GET, \"/api/v1/ad-slots/*/ads\").permitAll()");
        assertThat(publicAds).contains("anyRequest().denyAll()");

        String search = methodBody(security, "productSearchSecurityFilterChain");
        assertThat(search).contains("securityMatcher(\"/api/v1/product-search\", \"/api/v1/product-search/**\")");
        assertThat(search).contains("requestMatchers(HttpMethod.GET, \"/api/v1/product-search\").permitAll()");
        assertThat(search).contains("anyRequest().denyAll()");

        assertThat(methodBody(security, "applicationSecurityFilterChain")).contains(".anyRequest().permitAll()");
        assertThat(security).contains("@Order(Ordered.LOWEST_PRECEDENCE)");
        assertThat(security).contains("SecurityFilterChain applicationSecurityFilterChain");

        assertThat(controllerSimpleNames("src/main/java")).containsExactlyInAnyOrder(
                "ProductController", "AdController", "FileController", "ProductSearchController",
                "BrandController", "SpecController", "UserController", "RoleController",
                "PermissionController", "CourseController", "SchoolClassController");
    }

    @Test
    void registerFilterTrueRejectsProtectedChains() {
        NativeHttpSecurityConfiguration config = new NativeHttpSecurityConfiguration();
        assertThatThrownBy(() -> config.productSecurityFilterChain(null, null, null, null, null, true, false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must only run inside")
                .hasMessageContaining("product SecurityFilterChain");
        assertThatThrownBy(() -> config.adSecurityFilterChain(null, null, null, null, null, false, true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must only run inside")
                .hasMessageContaining("ad SecurityFilterChain");
        assertThatThrownBy(() -> config.fileUploadSecurityFilterChain(null, null, null, null, true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must be installed in the file-upload SecurityFilterChain");
    }

    @Test
    void authorizationComponentsKeepUserPermissionContracts() throws Exception {
        assertPattern(ProductAuthorizationComponent.class, "enjoyshop:product:(read|write|audit|purge)");
        assertPattern(AdAuthorizationComponent.class, "enjoyshop:ad:(read|write)");
        assertThat(Files.readString(sourceOf(ProductAuthorizationComponent.class))).contains(
                "PrincipalType.USER", "enjoyshop.product", "610206", "610208");
        assertThat(Files.readString(sourceOf(AdAuthorizationComponent.class))).contains(
                "PrincipalType.USER", "enjoyshop.ad", "GoodsErrorStatus.AD_FORBIDDEN",
                "GoodsErrorStatus.AD_AUTHORIZATION_UNAVAILABLE");
        assertThat(GoodsErrorStatus.AD_FORBIDDEN.getCode()).isEqualTo(610305);
        assertThat(GoodsErrorStatus.AD_AUTHORIZATION_UNAVAILABLE.getCode()).isEqualTo(610308);
    }

    @Test
    void changgouAuthStackIsAbsent() throws Exception {
        String tree = allMainJava("src/main/java");
        assertThat(tree).doesNotContain(
                "class LoginRedirectController",
                "class URLFilter",
                "class CreateJwtTest",
                "class AuthService",
                "changgou.jks",
                "getFirst(\"uid\")");
        assertThat(tree.toLowerCase(Locale.ROOT)).doesNotContain("weixin", "wechat", "qq.com/oauth");
        assertThat(Files.readString(Path.of("pom.xml"))).doesNotContain(
                "spring-boot-starter-thymeleaf",
                "spring-security-oauth2-authorization-server");
    }

    private static void assertPattern(Class<?> type, String regexp) throws Exception {
        Pattern pattern = type.getMethod("requirePermission", String.class)
                .getParameters()[0]
                .getAnnotation(Pattern.class);
        assertThat(pattern).isNotNull();
        assertThat(pattern.regexp()).isEqualTo(regexp);
    }

    private static String filterOrder(String methodBody) {
        int permissions = methodBody.indexOf(
                "addFilterBefore(permissions, AnonymousAuthenticationFilter.class)");
        int identity = methodBody.indexOf(
                "addFilterBefore(identity, Rbac3BearerAuthenticationFilter.class)");
        if (permissions >= 0 && identity > permissions) {
            return "identity-before-rbac-before-anonymous";
        }
        return "unexpected";
    }

    private static String methodBody(String source, String methodName) {
        int signature = source.indexOf("SecurityFilterChain " + methodName + "(");
        int throwsClause = source.indexOf("throws Exception", signature);
        if (signature < 0 || throwsClause < 0) {
            throw new IllegalArgumentException("missing " + methodName);
        }
        int open = source.indexOf('{', throwsClause);
        int depth = 0;
        for (int index = open; index < source.length(); index++) {
            char current = source.charAt(index);
            if (current == '{') {
                depth++;
            } else if (current == '}') {
                depth--;
                if (depth == 0) {
                    return source.substring(open, index + 1);
                }
            }
        }
        throw new IllegalArgumentException("unclosed " + methodName);
    }

    private static List<String> controllerSimpleNames(String root) throws IOException {
        List<String> names = new ArrayList<>();
        try (var paths = Files.walk(Path.of(root))) {
            for (Path path : paths.filter(candidate -> candidate.toString().endsWith(".java")).toList()) {
                if (REST_CONTROLLER.matcher(Files.readString(path)).find()) {
                    String fileName = path.getFileName().toString();
                    names.add(fileName.substring(0, fileName.length() - ".java".length()));
                }
            }
        }
        return names;
    }

    private static Path sourceOf(Class<?> type) {
        return Path.of("src/main/java", type.getName().replace('.', '/') + ".java");
    }

    private static String allMainJava(String root) throws IOException {
        StringBuilder tree = new StringBuilder();
        try (var paths = Files.walk(Path.of(root))) {
            for (Path path : paths.filter(candidate -> candidate.toString().endsWith(".java")).toList()) {
                tree.append(Files.readString(path)).append('\n');
            }
        }
        return tree.toString();
    }
}
