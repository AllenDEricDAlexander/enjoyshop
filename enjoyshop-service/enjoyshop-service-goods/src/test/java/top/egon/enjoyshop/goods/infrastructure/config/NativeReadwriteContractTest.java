package top.egon.enjoyshop.goods.infrastructure.config;

import org.apache.shardingsphere.driver.yaml.YamlJDBCConfiguration;
import org.apache.shardingsphere.infra.util.yaml.YamlEngine;
import org.apache.shardingsphere.readwritesplitting.yaml.config.YamlReadwriteSplittingRuleConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.common.cache.autoconfigure.EgonColaCacheProperties;
import top.egon.cola.component.common.mybatis.routing.EgonColaPhysicalTargetBO;
import top.egon.cola.component.common.mybatis.routing.EgonColaRoutingProfileBO;
import top.egon.cola.component.common.mybatis.sharding.EgonColaShardingProperties;
import top.egon.cola.component.common.mybatis.sharding.EgonColaShardingProperties.ConfigStyleEnum;
import top.egon.cola.component.common.mybatis.sharding.EgonColaShardingProperties.DataSourceRoleEnum;
import top.egon.cola.component.common.mybatis.sharding.EgonColaShardingProperties.ModeEnum;
import top.egon.cola.component.common.mybatis.sharding.EgonColaShardingProperties.PhysicalDataSourceProperties;
import top.egon.cola.component.common.mybatis.sharding.bootstrap.EgonColaShardingTopologyValidator;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/** Offline contract for the native PostgreSQL read/write configuration and SQL layout. */
class NativeReadwriteContractTest {

    private static final String SHARDING_PREFIX = "egon.cola.component.mybatis-plus.sharding";
    private static final String NATIVE_RESOURCE = "classpath:egon-ss-readwrite.yml";
    private static final List<String> PROFILES = List.of(
            "application.yml", "application-dev.yml", "application-test.yml", "application-prod.yml");
    private static final Map<String, Boolean> SAMPLE_RABBITMQ_STATE = Map.of(
            "application.yml", false,
            "application-dev.yml", true,
            "application-test.yml", false,
            "application-prod.yml", true);
    private static final List<String> LOGICAL_GROUPS = List.of("master_data", "shard_0", "shard_1");
    private static final Set<String> MASTER_DATA_TABLES = Set.of(
            "spu", "sku", "users", "roles", "permissions", "user_roles", "role_permissions",
            "courses", "brand", "category", "template", "spec", "para", "category_brand", "ad");
    private static final Map<String, String> EXPECTED_PHYSICAL_ROLES = Map.of(
            "master_data_primary", "master_data/PRIMARY",
            "master_data_replica_0", "master_data/REPLICA",
            "shard_0_primary", "shard_0/PRIMARY",
            "shard_0_replica_0", "shard_0/REPLICA",
            "shard_1_primary", "shard_1/PRIMARY",
            "shard_1_replica_0", "shard_1/REPLICA");

    @Test
    void readWriteGroupsBindSixPhysicalSources() {
        for (String profile : PROFILES) {
            EgonColaShardingProperties properties = bindProfile(profile);
            assertThat(properties.getMode()).as("mode in %s", profile).isEqualTo(ModeEnum.SHARDING_READWRITE);
            assertThat(properties.getConfigStyle()).as("config style in %s", profile).isEqualTo(ConfigStyleEnum.NATIVE);
            assertThat(properties.getNativeRulesResource()).as("native rules in %s", profile).isEqualTo(NATIVE_RESOURCE);
            assertThat(properties.getTransactionDefaultType()).as("transaction type in %s", profile).isEqualTo("LOCAL");
            assertThat(properties.getTables()).as("legacy STRATEGY table declarations in %s", profile).isEmpty();
            assertThat(roleBindings(properties.getDataSources())).as("physical roles in %s", profile)
                    .isEqualTo(EXPECTED_PHYSICAL_ROLES);
        }
    }

    @Test
    void adRegionHasBoundedIndependentTtlsInEveryProfile() {
        for (String profile : PROFILES) {
            EgonColaCacheProperties properties = bindCacheProfile(profile);
            EgonColaCacheProperties.RegionTtl region = properties.getRegions().get("AdCandidates");
            assertThat(region).as("AdCandidates region in %s", profile).isNotNull();
            assertThat(region.getL1Expire()).isEqualTo(Duration.ofSeconds(10));
            assertThat(region.getL1Jitter()).isEqualTo(Duration.ofSeconds(2));
            assertThat(region.getL2Expire()).isEqualTo(Duration.ofSeconds(30));
            assertThat(region.getL2Jitter()).isEqualTo(Duration.ofSeconds(5));
            assertThat(region.getNullExpire()).isNull();
        }
    }

    @Test
    void cdcDisabledByDefaultWithTypedConfirmBudgetsInEveryProfile() {
        for (String profile : PROFILES) {
            StandardEnvironment environment = profileEnvironment(profile);
            CdcSourceProperties cdc = Binder.get(environment)
                    .bind("app.cdc", Bindable.of(CdcSourceProperties.class))
                    .orElseThrow(() -> new IllegalStateException("CDC properties did not bind for " + profile));
            RabbitProperties rabbit = Binder.get(environment)
                    .bind("spring.rabbitmq", Bindable.of(RabbitProperties.class))
                    .orElseThrow(() -> new IllegalStateException("Rabbit properties did not bind for " + profile));
            assertThat(cdc.isEnabled()).as("CDC must be disabled in %s", profile).isFalse();
            assertThat(cdc.getMaxPayloadBytes()).isEqualTo(1_048_576);
            assertThat(cdc.getConfirmTimeout()).isEqualTo(Duration.ofSeconds(5));
            assertThat(cdc.getSourceRegistry()).isEmpty();
            assertThat(rabbit.getPublisherConfirmType())
                    .as("publisher confirms in %s", profile)
                    .isEqualTo(org.springframework.amqp.rabbit.connection.CachingConnectionFactory.ConfirmType.CORRELATED);
            assertThat(rabbit.isPublisherReturns()).as("publisher returns in %s", profile).isTrue();
            assertThat(environment.getProperty("app.integrations.rabbitmq.enabled", Boolean.class))
                    .as("existing sample Rabbit integration in %s", profile)
                    .isEqualTo(SAMPLE_RABBITMQ_STATE.get(profile));
            try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
                new ValidationUtils(factory.getValidator()).validate(cdc);
            }
        }
    }

    @Test
    void originalSampleRabbitConfigurationRemainsUnchangedAndNoNewBrokerSdkIsRequired() throws Exception {
        assertThat(sha256(Path.of("src/main/java/top/egon/enjoyshop/goods/infrastructure/config/RabbitMqConfig.java")))
                .isEqualTo("5d8f721d5a5a79718034e5be009c1ea615d2a8de7dbef47da8934e1dfada5ab5");
        assertThat(sha256(Path.of("src/main/java/top/egon/enjoyshop/goods/infrastructure/mq/MqRouteEnum.java")))
                .isEqualTo("bdbb81af71feb66808b439666cafdf445ea7d9714c5c219f3947746277e99258");
        String pom = Files.readString(Path.of("pom.xml"), StandardCharsets.UTF_8).toLowerCase(java.util.Locale.ROOT);
        assertThat(pom).doesNotContain("debezium", "kafka-clients", "elasticsearch", "transactional-outbox-starter");
    }

    @Test
    void allApprovedFilesPresent() throws IOException {
        Path root = projectRoot();
        Path module = root.resolve("enjoyshop-service/enjoyshop-service-goods");
        List<Path> required = List.of(
                module.resolve("src/main/java/top/egon/enjoyshop/goods/domain/ad/AdBO.java"),
                module.resolve("src/main/java/top/egon/enjoyshop/goods/domain/ad/AdDomainQuery.java"),
                module.resolve("src/main/java/top/egon/enjoyshop/goods/domain/sync/CdcRecordBO.java"),
                module.resolve("src/main/java/top/egon/enjoyshop/goods/domain/sync/CdcSourceCursorBO.java"),
                module.resolve("src/main/java/top/egon/enjoyshop/goods/domain/sync/AdCacheInvalidationEvent.java"),
                module.resolve("src/main/java/top/egon/enjoyshop/goods/domain/sync/ProductRefreshRequestedEvent.java"),
                module.resolve("src/main/java/top/egon/enjoyshop/goods/infrastructure/config/CdcRabbitConfiguration.java"),
                module.resolve("src/main/java/top/egon/enjoyshop/goods/infrastructure/sync/CdcEventPublisher.java"),
                module.resolve("src/main/java/top/egon/enjoyshop/goods/adapter/sync/CdcRawConsumer.java"),
                module.resolve("src/main/java/top/egon/enjoyshop/goods/adapter/sync/AdCacheInvalidationConsumer.java"),
                root.resolve("deployment/cdc/debezium/application.properties"),
                root.resolve("deployment/cdc/postgres/replication-contract.md"),
                root.resolve("deployment/cdc/rabbit/topology-contract.md"),
                root.resolve("deployment/cdc/acceptance.md"),
                root.resolve("deployment/cdc/README.md"),
                root.resolve("deployment/cdc/check.sh"),
                module.resolve("codegen/prepare-ad.sh"));
        assertThat(required).allSatisfy(path -> assertThat(Files.isRegularFile(path))
                .as("approved implementation artifact %s", path).isTrue());
        Path infrastructure = module.resolve("src/main/java/top/egon/enjoyshop/goods/infrastructure");
        assertThat(javaFileCount(infrastructure, "PO.java")).isEqualTo(14);
        assertThat(javaFileCount(infrastructure, "DAO.java")).isEqualTo(14);
        assertThat(javaFileCount(infrastructure, "Repository.java")).isEqualTo(14);
        assertThat(javaFileCount(infrastructure, "DomainServiceImpl.java")).isEqualTo(10);
    }

    @Test
    void qualifiedConstructorBeans() throws ReflectiveOperationException {
        for (String name : List.of(
                "top.egon.enjoyshop.goods.application.ad.AdManageImpl",
                "top.egon.enjoyshop.goods.infrastructure.ad.service.impl.AdDomainServiceImpl",
                "top.egon.enjoyshop.goods.infrastructure.ad.repo.AdRepository",
                "top.egon.enjoyshop.goods.infrastructure.sync.CdcEventPublisher",
                "top.egon.enjoyshop.goods.infrastructure.sync.AdCdcChangeStrategy",
                "top.egon.enjoyshop.goods.infrastructure.sync.ProductCdcChangeStrategy",
                "top.egon.enjoyshop.goods.adapter.sync.CdcRawConsumer",
                "top.egon.enjoyshop.goods.adapter.sync.AdCacheInvalidationConsumer")) {
            assertQualifiedConstructor(Class.forName(name));
        }
    }

    @Test
    void noForbiddenImports() throws IOException, ReflectiveOperationException {
        Path goods = projectRoot().resolve("enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods");
        List<Path> sourceFiles = new ArrayList<>();
        for (String file : List.of("domain/ad/AdBO.java", "domain/ad/AdDomainQuery.java",
                "domain/ad/AdDomainService.java", "domain/ad/AdEligibilitySpecification.java")) {
            sourceFiles.add(goods.resolve(file));
        }
        for (String directory : List.of("domain/sync")) {
            try (var files = Files.newDirectoryStream(goods.resolve(directory), "*.java")) {
                files.forEach(sourceFiles::add);
            }
        }
        List<String> forbidden = List.of("org.springframework.", "org.apache.ibatis.",
                "com.baomidou.mybatisplus.", "org.springframework.security", "org.springframework.amqp.",
                "com.rabbitmq.", "infrastructure.", "JsonNode");
        for (Path source : sourceFiles) {
            String text = Files.readString(source);
            for (String marker : forbidden) {
                assertThat(text).as("forbidden dependency or API %s in %s", marker, source)
                        .doesNotContain(marker);
            }
        }
        // AdStatusEnum retains the existing MP code mapping required by AdPO.status.
        assertThat(top.egon.enjoyshop.goods.domain.ad.AdStatusEnum.class.getDeclaredField("code")
                .isAnnotationPresent(com.baomidou.mybatisplus.annotation.EnumValue.class)).isTrue();
        String modulePom = Files.readString(projectRoot().resolve("enjoyshop-service/enjoyshop-service-goods/pom.xml"));
        assertThat(modulePom.toLowerCase(java.util.Locale.ROOT))
                .doesNotContain("debezium", "kafka-clients", "elasticsearch", "transactional-outbox-starter");
    }

    @Test
    void noTemporaryTypes() {
        Path root = projectRoot();
        Path java = root.resolve("enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods");
        assertThat(Files.exists(root.resolve("scripts/work/2026-10-10-16-13-postgresql-cdc-advertisement-implementation-verify.sh")))
                .isFalse();
        assertThat(Files.exists(root.resolve("scripts/work/2026-10-10-16-13-postgresql-cdc-advertisement-implementation-prepare-ad.sh")))
                .isFalse();
        assertThat(Files.exists(java.resolve("adapter/ad/AdController.java"))).isFalse();
        assertThat(Files.exists(java.resolve("infrastructure/ad/AdPersistenceConverter.java"))).isFalse();
        assertThat(Files.exists(java.resolve("infrastructure/ad/catalog/AdCatalogSource.java"))).isFalse();
    }

    @Test
    void nativeProfilesAreParseableWithoutConnections() throws IOException {
        ClassPathResource resource = new ClassPathResource("egon-ss-readwrite.yml");
        assertThat(resource.exists()).as("native read/write rule resource must exist").isTrue();
        byte[] nativeYaml;
        try (var input = resource.getInputStream()) {
            nativeYaml = input.readAllBytes();
        }

        YamlJDBCConfiguration yaml = YamlEngine.unmarshal(
                new String(nativeYaml, StandardCharsets.UTF_8), YamlJDBCConfiguration.class);
        assertThat(yaml.getDataSources()).as("the Common starter owns all physical pools").isEmpty();
        assertThat(yaml.getTransaction().getDefaultType()).isEqualTo("LOCAL");
        assertThat(yaml.getProps())
                .containsEntry("sql-show", false)
                .containsEntry("check-table-metadata-enabled", true);

        List<YamlReadwriteSplittingRuleConfiguration> readWriteRules = yaml.getRules().stream()
                .filter(YamlReadwriteSplittingRuleConfiguration.class::isInstance)
                .map(YamlReadwriteSplittingRuleConfiguration.class::cast)
                .toList();
        assertThat(readWriteRules).hasSize(1);
        var groups = readWriteRules.getFirst().getDataSourceGroups();
        assertThat(groups.keySet()).containsExactlyInAnyOrderElementsOf(LOGICAL_GROUPS);
        for (String group : LOGICAL_GROUPS) {
            var definition = groups.get(group);
            assertThat(definition.getWriteDataSourceName()).isEqualTo(group + "_primary");
            assertThat(definition.getReadDataSourceNames()).containsExactly(group + "_replica_0");
            assertThat(definition.getTransactionalReadQueryStrategy()).isEqualTo("PRIMARY");
            assertThat(definition.getLoadBalancerName()).isEqualTo("round_robin");
        }
        assertThat(readWriteRules.getFirst().getLoadBalancers()).containsKey("round_robin");

        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            EgonColaShardingTopologyValidator topologyValidator = new EgonColaShardingTopologyValidator(
                    new ValidationUtils(factory.getValidator()));
            var topology = topologyValidator.validate(routingProperties(), nativeYaml);
            assertThat(topology.profiles().keySet()).containsExactlyInAnyOrderElementsOf(expectedLogicalTables());
            assertThat(topology.schemas().keySet()).containsExactlyInAnyOrderElementsOf(LOGICAL_GROUPS);
            assertNodes(topology.profiles().get("school_classes"), Set.of(
                    "shard_0.public.school_classes_0", "shard_0.public.school_classes_1",
                    "shard_1.public.school_classes_0", "shard_1.public.school_classes_1"));
            assertNodes(topology.profiles().get("class_course_schedules"), Set.of(
                    "shard_0.public.class_course_schedules_0", "shard_0.public.class_course_schedules_1",
                    "shard_1.public.class_course_schedules_0", "shard_1.public.class_course_schedules_1"));
        }
    }

    private static EgonColaShardingProperties bindProfile(String profile) {
        StandardEnvironment environment = new StandardEnvironment();
        try {
            for (var source : new YamlPropertySourceLoader().load(
                    profile, new ClassPathResource(profile))) {
                environment.getPropertySources().addFirst(source);
            }
        } catch (IOException failure) {
            throw new IllegalStateException("cannot load sharding profile " + profile, failure);
        }
        Map<String, Object> placeholders = new LinkedHashMap<>();
        for (String group : LOGICAL_GROUPS) {
            String envName = group.toUpperCase().replace('_', '-').replace('-', '_');
            placeholders.put("LIGHT_SHARDING_" + envName + "_URL",
                    "jdbc:postgresql://example.invalid:5432/" + group + "_contract");
            placeholders.put("LIGHT_SHARDING_" + envName + "_REPLICA_0_URL",
                    "jdbc:postgresql://example.invalid:5432/" + group + "_replica_contract");
            placeholders.put("LIGHT_SHARDING_" + envName + "_REPLICA_0_USERNAME", "replica-contract");
            placeholders.put("LIGHT_SHARDING_" + envName + "_REPLICA_0_PASSWORD", "");
        }
        placeholders.put("LIGHT_SHARDING_DRIVER_CLASS_NAME", "org.postgresql.Driver");
        placeholders.put("LIGHT_SHARDING_USERNAME", "contract");
        placeholders.put("LIGHT_SHARDING_PASSWORD", "");
        environment.getPropertySources().addFirst(new MapPropertySource("native-readwrite-contract", placeholders));
        return Binder.get(environment)
                .bind(SHARDING_PREFIX, Bindable.of(EgonColaShardingProperties.class))
                .orElseThrow(() -> new IllegalStateException("sharding properties did not bind for " + profile));
    }

    private static EgonColaCacheProperties bindCacheProfile(String profile) {
        StandardEnvironment environment = new StandardEnvironment();
        try {
            for (var source : new YamlPropertySourceLoader().load(profile, new ClassPathResource(profile))) {
                environment.getPropertySources().addFirst(source);
            }
        } catch (IOException failure) {
            throw new IllegalStateException("cannot load cache profile " + profile, failure);
        }
        environment.getPropertySources().addFirst(new MapPropertySource("cache-profile-support",
                Map.of("spring.application.name", "cache-contract")));
        return Binder.get(environment)
                .bind("egon.cola.component.cache", Bindable.of(EgonColaCacheProperties.class))
                .orElseThrow(() -> new IllegalStateException("cache properties did not bind for " + profile));
    }

    private static StandardEnvironment profileEnvironment(String profile) {
        StandardEnvironment environment = new StandardEnvironment();
        try {
            for (var source : new YamlPropertySourceLoader().load(profile, new ClassPathResource(profile))) {
                environment.getPropertySources().addFirst(source);
            }
        } catch (IOException failure) {
            throw new IllegalStateException("cannot load profile " + profile, failure);
        }
        return environment;
    }

    private static String sha256(Path path) throws IOException, NoSuchAlgorithmException {
        return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(Files.readAllBytes(path)));
    }

    private static long javaFileCount(Path root, String suffix) throws IOException {
        try (var paths = Files.walk(root)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(suffix))
                    .count();
        }
    }

    private static Path projectRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        while (current != null) {
            if (Files.isDirectory(current.resolve("deployment"))
                    && Files.isDirectory(current.resolve("enjoyshop-service"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("repository root could not be located");
    }

    private static void assertQualifiedConstructor(Class<?> type) {
        List<Field> dependencies = Arrays.stream(type.getDeclaredFields())
                .filter(field -> Modifier.isFinal(field.getModifiers()) && !Modifier.isStatic(field.getModifiers()))
                .toList();
        assertThat(dependencies).as("final constructor dependencies for %s", type.getName()).isNotEmpty();
        for (Field dependency : dependencies) {
            assertThat(dependency.isAnnotationPresent(Qualifier.class))
                    .as("field qualifier %s.%s", type.getSimpleName(), dependency.getName()).isTrue();
            assertThat(dependency.isAnnotationPresent(Autowired.class))
                    .as("field injection %s.%s", type.getSimpleName(), dependency.getName()).isFalse();
        }
        Constructor<?> constructor = Arrays.stream(type.getDeclaredConstructors())
                .filter(candidate -> candidate.getParameterCount() == dependencies.size())
                .findFirst()
                .orElseThrow(() -> new AssertionError("missing required constructor for " + type.getName()));
        assertThat(Arrays.stream(constructor.getParameters())
                .allMatch(parameter -> parameter.isAnnotationPresent(Qualifier.class)))
                .as("constructor parameter qualifiers for %s", type.getName()).isTrue();
    }

    private static EgonColaShardingProperties routingProperties() {
        List<PhysicalDataSourceProperties> sources = new ArrayList<>();
        for (String logicalGroup : LOGICAL_GROUPS) {
            sources.add(physicalSource(logicalGroup + "_primary", logicalGroup, DataSourceRoleEnum.PRIMARY));
            sources.add(physicalSource(logicalGroup + "_replica_0", logicalGroup, DataSourceRoleEnum.REPLICA));
        }
        return EgonColaShardingProperties.builder()
                .enabled(true)
                .mode(ModeEnum.SHARDING_READWRITE)
                .configStyle(ConfigStyleEnum.NATIVE)
                .transactionDefaultType("LOCAL")
                .nativeRulesResource(NATIVE_RESOURCE)
                .dataSources(sources)
                .tables(Map.of())
                .build();
    }

    private static PhysicalDataSourceProperties physicalSource(
            String name, String logicalName, DataSourceRoleEnum role) {
        return new PhysicalDataSourceProperties(name, logicalName, role, "org.postgresql.Driver",
                "jdbc:postgresql://example.invalid:5432/" + logicalName + "_contract", "contract", "");
    }

    private static Map<String, String> roleBindings(List<PhysicalDataSourceProperties> sources) {
        return sources.stream().collect(Collectors.toMap(
                PhysicalDataSourceProperties::name,
                source -> source.logicalName() + "/" + source.role().name()));
    }

    private static Set<String> expectedLogicalTables() {
        Set<String> tables = new TreeSet<>(MASTER_DATA_TABLES);
        tables.add("school_classes");
        tables.add("class_course_schedules");
        return tables;
    }

    private static void assertNodes(EgonColaRoutingProfileBO profile, Set<String> expected) {
        assertThat(profile).isNotNull();
        Set<String> actual = profile.actualNodes().values().stream()
                .flatMap(Collection::stream)
                .map(node -> node.group() + "." + node.schema() + "." + node.table())
                .collect(Collectors.toSet());
        assertThat(actual).isEqualTo(expected);
    }
}
