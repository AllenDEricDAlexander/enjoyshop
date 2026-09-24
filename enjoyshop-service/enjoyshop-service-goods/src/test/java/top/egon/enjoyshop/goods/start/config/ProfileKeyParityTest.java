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
 * environment file with the same "off unless a variable says otherwise" default, and the brand
 * routing entry must exist in every {@code tables} map the binder can reach.
 *
 * <p>Nothing here starts a Spring context, connects to a database or reads an instance — the files
 * are parsed as data. That is deliberate: the runtime half of {@code REQ-004} (the {@code @Pattern}
 * that keeps the driver and URL PostgreSQL-only) is exercised at deployment, while key drift is a
 * purely textual property that must be catchable without any environment.</p>
 *
 * <p>The parity criterion is the narrowed one recorded by {@code PLAN-CLAR-026}/{@code RISK-P6}:
 * strict equality is asserted for the keys this change owns, not for the whole document, because the
 * archetype baseline keeps 110 base-only and 4 test-only sample keys by design and moving them would
 * be an unauthorised ~330-line change touching values that are supposed to differ per environment.
 * </p>
 */
class ProfileKeyParityTest {

    private static final List<String> PROFILES = List.of(
            "application.yml", "application-dev.yml", "application-test.yml", "application-prod.yml");

    private static final String SHARDING_IMPORT = "egon-mybatis-plus-sharding.yml";

    /** The six governance switches, plus the brand routing key, owned by REQ-004. */
    private static final List<String> GOVERNANCE_KEYS = List.of(
            "egon.cola.component.tianshu.enabled",
            "egon.cola.component.tianshu.redis.enabled",
            "egon.cola.component.tianshu.registry.enabled",
            "egon.cola.component.tianshu.registry.http.enabled",
            "egon.cola.platform.tianquan.shoubing.enabled",
            "egon.cola.component.yuheng.openapi.enabled",
            "egon.cola.component.mybatis-plus.sharding.tables.brand");

    private static final List<String> GOVERNANCE_SWITCHES = GOVERNANCE_KEYS.subList(0, 6);

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
    void brand_is_registered_in_every_tables_map() {
        for (String profile : PROFILES) {
            Map<String, Object> root = load(profile);
            assertThat(flatten(root))
                    .as("brand routing entry in %s", profile)
                    .contains("egon.cola.component.mybatis-plus.sharding.tables.brand");
            assertThat(effectiveDefault(valueAt(root,
                    "egon.cola.component.mybatis-plus.sharding.tables.brand.type")))
                    .as("brand is SINGLE master data (DEC-114) in %s", profile)
                    .isEqualTo("SINGLE");
            assertThat(effectiveDefault(valueAt(root,
                    "egon.cola.component.mybatis-plus.sharding.tables.brand.data-source")))
                    .as("brand lands on master_data in %s", profile)
                    .isEqualTo("master_data");
        }
        // The base file imports this one, so its tables map is a sixth place brand must exist.
        assertThat(flatten(load(SHARDING_IMPORT)))
                .contains("egon.cola.component.mybatis-plus.sharding.tables.brand");
    }
}
