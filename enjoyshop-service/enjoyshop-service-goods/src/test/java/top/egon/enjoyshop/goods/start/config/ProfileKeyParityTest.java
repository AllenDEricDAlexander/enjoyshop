package top.egon.enjoyshop.goods.start.config;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code TEST-013} executed statically: the governance key families must be present in every
 * environment file with the same "off unless a variable says otherwise" default, and the native
 * read/write rule resource must be selected consistently by every profile.
 *
 * <p>Nothing here starts a Spring context, connects to a database or reads an instance — the files
 * are parsed as data. That is deliberate: database routing and connection health are verified in
 * their own offline/runtime gates, while profile key drift is a textual property that can be
 * caught without any environment.</p>
 *
 * <p>The parity criterion is the narrowed one recorded by {@code PLAN-CLAR-026}/{@code RISK-P6}:
 * strict equality is asserted for the keys this test owns, not for the whole document, because the
 * archetype baseline keeps 110 base-only and 4 test-only sample keys by design and moving them would
 * be an unauthorised ~330-line change touching values that are supposed to differ per environment.
 * </p>
 */
class ProfileKeyParityTest {

    private static final List<String> PROFILES = List.of(
            "application.yml", "application-dev.yml", "application-test.yml", "application-prod.yml");

    private static final String SHARDING_IMPORT = "egon-mybatis-plus-sharding.yml";

    /** The exact configuration paths owned by CORS, storage, upload and catalog routing. */
    private static final List<String> GOVERNANCE_KEYS = List.of(
            "egon.cola.component.tianshu.enabled",
            "egon.cola.component.tianshu.redis.enabled",
            "egon.cola.component.tianshu.registry.enabled",
            "egon.cola.component.tianshu.registry.http.enabled",
            "egon.cola.platform.tianquan.shoubing.enabled",
            "egon.cola.platform.tianquan.shoubing.register-filter",
            "egon.cola.platform.tianquan.shoubing.issuer",
            "egon.cola.platform.tianquan.shoubing.jwk-set-uri",
            "egon.cola.platform.tianquan.shoubing.resource-server-id",
            "egon.cola.platform.tianquan.shoubing.resource-uri",
            "egon.cola.platform.tianquan.shoubing.service-client.app-id",
            "egon.cola.platform.tianquan.shoubing.service-client.registration-id",
            "egon.cola.component.yuheng.openapi.enabled",
            "app.web.cors.allowed-origins",
            "app.object-storage.endpoint",
            "app.object-storage.region",
            "app.object-storage.bucket",
            "app.object-storage.path-style-access",
            "app.object-storage.public-url-base",
            "app.object-storage.access-key",
            "app.object-storage.secret-key",
            "app.object-storage.api-call-attempt-timeout",
            "app.object-storage.api-call-timeout",
            "spring.servlet.multipart.max-file-size",
            "spring.servlet.multipart.max-request-size",
            "egon.cola.component.mybatis-plus.sharding.config-style",
            "egon.cola.component.mybatis-plus.sharding.native-rules-resource",
            "egon.cola.component.mybatis-plus.sharding.mode",
            "egon.cola.component.mybatis-plus.sharding.transaction-default-type",
            "egon.cola.component.mybatis-plus.sharding.data-sources");

    private static final List<String> GOVERNANCE_SWITCHES = List.of(
            "egon.cola.component.tianshu.enabled",
            "egon.cola.component.tianshu.redis.enabled",
            "egon.cola.component.tianshu.registry.enabled",
            "egon.cola.component.tianshu.registry.http.enabled",
            "egon.cola.platform.tianquan.shoubing.enabled",
            "egon.cola.platform.tianquan.shoubing.register-filter",
            "egon.cola.component.yuheng.openapi.enabled");

    private static final Pattern PLACEHOLDER = Pattern.compile("^\\$\\{[^{}]*?:([^{}]*?)\\}$");

    private static final Set<String> ALL_PROFILES = Set.copyOf(PROFILES);

    private static Map<String, Object> load(String resource) {
        try (InputStream in = ProfileKeyParityTest.class.getClassLoader().getResourceAsStream(resource)) {
            assertThat(in).as("classpath resource %s", resource).isNotNull();
            Map<String, Object> merged = new LinkedHashMap<>();
            for (Object document : new Yaml().loadAll(in)) {
                if (document instanceof Map<?, ?> map) {
                    map.forEach((key, value) -> merged.put(String.valueOf(key), value));
                }
            }
            return merged;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Set<String> flatten(Map<String, Object> root) {
        Set<String> paths = new LinkedHashSet<>();
        walk("", root, paths);
        return paths;
    }

    @SuppressWarnings("unchecked")
    private static void walk(String prefix, Map<String, Object> node, Set<String> sink) {
        node.forEach((key, value) -> {
            String path = prefix.isEmpty() ? key : prefix + "." + key;
            if (value instanceof Map<?, ?> child) {
                sink.add(path);
                walk(path, (Map<String, Object>) child, sink);
            } else if (value instanceof List<?> list) {
                sink.add(path);
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i) instanceof Map<?, ?> item) {
                        walk(path + "[" + i + "]", (Map<String, Object>) item, sink);
                    }
                }
            } else {
                sink.add(path);
            }
        });
    }

    private static Object valueAt(Map<String, Object> root, String dottedPath) {
        Object node = root;
        for (String segment : dottedPath.split("\\.")) {
            if (!(node instanceof Map<?, ?> map) || !map.containsKey(segment)) {
                return null;
            }
            node = map.get(segment);
        }
        return node;
    }

    /** What the key resolves to when its environment variable is absent. */
    private static String effectiveDefault(Object raw) {
        if (raw == null) {
            return "<absent>";
        }
        if (raw instanceof Boolean flag) {
            return String.valueOf(flag);
        }
        Matcher matcher = PLACEHOLDER.matcher(String.valueOf(raw).trim());
        return matcher.matches() ? matcher.group(1) : String.valueOf(raw).trim();
    }

    @Test
    void governance_key_families_have_strict_parity_across_the_four_profiles() {
        Map<String, Set<String>> keysByProfile = new LinkedHashMap<>();
        PROFILES.forEach(profile -> keysByProfile.put(profile, flatten(load(profile))));

        for (String key : GOVERNANCE_KEYS) {
            Set<String> present = new LinkedHashSet<>();
            keysByProfile.forEach((profile, keys) -> {
                if (keys.contains(key)) {
                    present.add(profile);
                }
            });
            // Criterion 2 of PLAN-CLAR-026: this change must not be the origin of a profile-only key.
            assertThat(present).as("key %s must exist in all four profiles", key).isEqualTo(ALL_PROFILES);
        }
    }

    @Test
    void governance_switches_default_to_false_in_every_profile() {
        for (String profile : PROFILES) {
            Map<String, Object> root = load(profile);
            for (String key : GOVERNANCE_SWITCHES) {
                assertThat(effectiveDefault(valueAt(root, key)))
                        .as("%s must stay off unless %s is set explicitly (profile %s)", key, key, profile)
                        .isEqualTo("false");
            }
        }
    }

    @Test
    void catalogs_use_native_rule_resource_in_every_profile() {
        for (String profile : PROFILES) {
            Map<String, Object> root = load(profile);
            String prefix = "egon.cola.component.mybatis-plus.sharding.";
            assertThat(effectiveDefault(valueAt(root, prefix + "config-style")))
                    .as("NATIVE rules are selected in %s", profile).isEqualTo("NATIVE");
            assertThat(effectiveDefault(valueAt(root, prefix + "native-rules-resource")))
                    .as("native rule source is selected in %s", profile)
                    .isEqualTo("classpath:egon-ss-readwrite.yml");
            assertThat(effectiveDefault(valueAt(root, prefix + "mode")))
                    .as("read/write mode is selected in %s", profile).isEqualTo("SHARDING_READWRITE");
            assertThat(effectiveDefault(valueAt(root, prefix + "transaction-default-type")))
                    .as("LOCAL transaction mode stays selected in %s", profile).isEqualTo("LOCAL");
            assertThat(flatten(root)).as("legacy STRATEGY table entries in %s", profile)
                    .noneMatch(key -> key.startsWith(prefix + "tables."));
        }
        assertThat(effectiveDefault(valueAt(load(SHARDING_IMPORT),
                "egon.cola.component.mybatis-plus.sharding.native-rules-resource")))
                .as("the imported base sharding configuration uses the same NATIVE rule source")
                .isEqualTo("classpath:egon-ss-readwrite.yml");
    }

    @Test
    void native_sharding_core_keys_have_strict_parity() {
        String prefix = "egon.cola.component.mybatis-plus.sharding.";
        Set<String> expected = new LinkedHashSet<>(flatten(load("application.yml")));
        expected.removeIf(key -> !key.startsWith(prefix));

        for (String profile : PROFILES) {
            Set<String> actual = new LinkedHashSet<>(flatten(load(profile)));
            actual.removeIf(key -> !key.startsWith(prefix));
            assertThat(actual).as("native sharding key tree in %s", profile)
                    .containsExactlyInAnyOrderElementsOf(expected);
        }
    }

    @Test
    void productAuthorizationProfilesHaveIdenticalKeysAndBoundedCache() {
        String prefix = "egon.cola.platform.tianquan.jianshen";
        Set<String> expected = flatten(Map.of("jianshen", valueAt(load("application.yml"), prefix)));
        for (String profile : PROFILES) {
            Map<String, Object> root = load(profile);
            assertThat(flatten(Map.of("jianshen", valueAt(root, prefix)))).isEqualTo(expected);
            assertThat(valueAt(root, prefix + ".register-filter")).isEqualTo(false);
            assertThat(valueAt(root, prefix + ".system-code")).isEqualTo("enjoyshop-goods");
            assertThat(valueAt(root, prefix + ".authorization.cache-ttl")).isEqualTo("PT1S");
            assertThat(valueAt(root, prefix + ".authorization.near-cache-ttl")).isEqualTo("PT0S");
            assertThat(valueAt(root, prefix + ".authorization.maximum-jitter")).isEqualTo("PT0S");
            assertThat(valueAt(root, prefix + ".authorization.fetch-timeout")).isEqualTo("PT1S");
        }
    }
    @Test
    void jianshenProfilesBindToTheActualStarterProperties() throws Exception {
        String prefix = "egon.cola.platform.tianquan.jianshen";
        for (String profile : PROFILES) {
            assertThat(valueAt(load(profile), prefix + ".runtime.database")).isNotNull();
            assertThat(valueAt(load(profile), prefix + ".runtime.password-file")).isNotNull();
            var environment = new org.springframework.core.env.StandardEnvironment();
            for (var source : new org.springframework.boot.env.YamlPropertySourceLoader().load(profile,
                    new org.springframework.core.io.ClassPathResource(profile))) {
                environment.getPropertySources().addFirst(source);
            }
            environment.getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource("binding-fixture",
                    Map.of(prefix + ".runtime.database", "6", prefix + ".runtime.password-file", "/not-read/example",
                            prefix + ".authorization.service-token.scopes", "rbac3.snapshot.read")));
            var properties = org.springframework.boot.context.properties.bind.Binder.get(environment).bind(prefix,
                    org.springframework.boot.context.properties.bind.Bindable.of(top.egon.cola.platform.tianquan.jianshen.starter.autoconfigure.Rbac3StarterProperties.class)).get();
            assertThat(properties.getRuntime().getDatabase()).isEqualTo(6);
            assertThat(properties.getRuntime().getPasswordFile()).isEqualTo("/not-read/example");
            assertThat(properties.getAuthorization().getServiceToken().getScopes()).isEqualTo("rbac3.snapshot.read");
            assertThat(properties.getAuthorization().getCacheTtl()).isEqualTo(java.time.Duration.ofSeconds(1));
            assertThat(properties.isRegisterFilter()).isFalse();
        }
    }

}
