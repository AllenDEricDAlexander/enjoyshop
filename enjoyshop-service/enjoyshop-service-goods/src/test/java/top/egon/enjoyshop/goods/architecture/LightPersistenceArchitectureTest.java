package architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Generated-project architecture contract for the Light MyBatis-Plus profile.
 */
class LightPersistenceArchitectureTest {

    @Test
    void generatedProjectUsesCommonPersistenceOwnership() throws IOException {
        Path sourceRoot = Path.of("src/main/java");
        List<Path> javaFiles;
        try (Stream<Path> paths = Files.walk(sourceRoot)) {
            javaFiles = paths.filter(path -> path.toString().endsWith(".java")).toList();
        }
        String source = javaFiles.stream()
                .map(path -> read(path))
                .reduce("", String::concat);

        assertFalse(source.contains("jakarta.persistence"));
        assertFalse(source.contains("JpaRepository"));
        assertFalse(source.contains("EntityManager"));
        assertFalse(source.contains("UuidV7"));
        assertFalse(source.contains("repo.jpa"));
        assertFalse(source.contains("repo.impl"));
        assertFalse(source.contains("repo.mapper"));
        assertTrue(source.contains("extends EgonModel<"));
        assertTrue(source.contains("extends EgonColaMapper<"));
        assertFalse(source.contains("EgonColaIService"));
        assertFalse(source.contains("EgonColaServiceImpl"));
        assertTrue(source.contains("extends EgonColaRepository<"));
        assertEquals(7, count(javaFiles, "Repository.java"));
        for (Path file : javaFiles) {
            if (file.toString().contains("service/impl") && file.toString().endsWith("DomainServiceImpl.java")) {
                assertFalse(read(file).contains("repo.dao"), "Business Service must compose repositories: " + file);
                assertFalse(read(file).contains("extends EgonCola"));
            }
        }

        assertEquals(6, count(javaFiles, "PO.java"));
        assertEquals(6, count(javaFiles, "DAO.java"));
        assertEquals(6, count(javaFiles, "DomainServiceImpl.java"));
        for (String prefix : List.of("Spu", "Sku", "CategoryBrand", "Ad")) {
            assertTrue(javaFiles.stream().anyMatch(file -> file.getFileName().toString().equals(prefix + "PO.java")));
            assertTrue(javaFiles.stream().anyMatch(file -> file.getFileName().toString().equals(prefix + "DAO.java")));
            assertTrue(javaFiles.stream().anyMatch(file -> file.getFileName().toString().equals(prefix + "Repository.java")));
        }
        Path searchPort = Path.of("src/main/java/top/egon/enjoyshop/goods/domain/search/ProductSearchRepository.java");
        assertFalse(read(searchPort).contains("org.springframework"));
        assertFalse(read(searchPort).contains("com.baomidou.mybatisplus"));
        assertFalse(read(searchPort).contains("co.elastic.clients"));
        assertFalse(source.contains("class ShardingDataSourceBootstrapper"));
        assertFalse(source.contains("YamlShardingSphereDataSourceFactory"));
        String pom = Files.readString(Path.of("pom.xml"), StandardCharsets.UTF_8);
        assertTrue(pom.contains("egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter"));
        assertFalse(pom.contains("shardingsphere-jdbc"));
        assertFalse(Files.exists(Path.of("src/main/resources/datasource/sharding.yml")));
        assertFalse(Files.exists(Path.of("src/main/resources/sharding/shardingsphere-sharding.yml")));
        assertTrue(Files.exists(Path.of("src/main/resources/egon-mybatis-plus-sharding.yml")));
        assertFalse(Files.exists(Path.of("src/main/java/top/egon/enjoyshop/goods/application/ad/CreateAdCommand.java")));
        assertFalse(Files.exists(Path.of("src/main/java/top/egon/enjoyshop/goods/application/ad/UpdateAdCommand.java")));
        assertFalse(Files.exists(Path.of("src/main/java/top/egon/enjoyshop/goods/application/ad/DeleteAdCommand.java")));
        assertFalse(Files.exists(Path.of("src/main/java/top/egon/enjoyshop/goods/application/ad/AdDetailQuery.java")));
        Path adDomainService = Path.of("src/main/java/top/egon/enjoyshop/goods/domain/ad/AdDomainService.java");
        assertFalse(read(adDomainService).contains("org.springframework"));
        Path adDomain = Path.of("src/main/java/top/egon/enjoyshop/goods/domain/ad/AdBO.java");
        assertFalse(read(adDomain).contains("com.baomidou.mybatisplus"));
        Path adDomainServiceImpl = Path.of("src/main/java/top/egon/enjoyshop/goods/infrastructure/ad/service/impl/AdDomainServiceImpl.java");
        assertFalse(read(adDomainServiceImpl).contains("AdDAO"));
        assertFalse(read(adDomainServiceImpl).contains("getBaseMapper"));
    }

    @Test
    void generatedShardingConfigurationUsesTenantRouting() throws IOException {
        String sharding = read(Path.of("src/main/resources/egon-mybatis-plus-sharding.yml"));
        assertTrue(sharding.contains("mybatis-plus:"));
        assertTrue(sharding.contains("sharding:"));
        assertTrue(sharding.contains("config-style: NATIVE"));
        assertTrue(sharding.contains("native-rules-resource: classpath:egon-ss-readwrite.yml"));
        assertTrue(sharding.contains("mode: ${APP_DATASOURCE_MODE:SHARDING_READWRITE}"));
        assertTrue(sharding.contains("transaction-default-type: LOCAL"));
        assertFalse(sharding.contains("config-style: STRATEGY"));
        assertFalse(sharding.contains("STANDARD_TENANT_ID"));
        assertFalse(sharding.contains("UuidV7BucketShardingAlgorithm"));

        String nativeRules = read(Path.of("src/main/resources/egon-ss-readwrite.yml"));
        assertTrue(nativeRules.contains("!READWRITE_SPLITTING"));
        assertTrue(nativeRules.contains("dataSourceGroups:"));
        assertTrue(nativeRules.contains("master_data_primary"));
        assertTrue(nativeRules.contains("master_data_replica_0"));
        assertTrue(nativeRules.contains("shard_0_primary"));
        assertTrue(nativeRules.contains("shard_0_replica_0"));
        assertTrue(nativeRules.contains("shard_1_primary"));
        assertTrue(nativeRules.contains("shard_1_replica_0"));
        assertTrue(nativeRules.contains("transactionalReadQueryStrategy: PRIMARY"));
        assertTrue(nativeRules.contains("!SINGLE"));
        assertTrue(nativeRules.contains("master_data.public.brand"));
        assertTrue(nativeRules.contains("master_data.public.ad"));
        assertTrue(nativeRules.contains("!SHARDING"));
        assertTrue(nativeRules.contains("school_classes_0"));
        assertTrue(nativeRules.contains("school_classes_1"));
        assertTrue(nativeRules.contains("class_course_schedules_0"));
        assertTrue(nativeRules.contains("class_course_schedules_1"));
        assertTrue(nativeRules.contains("bindingTables: [\"school_classes,class_course_schedules\"]"));
        assertFalse(nativeRules.contains("master_data.public.school_classes_"));
        assertFalse(nativeRules.contains("_t0"));
        assertFalse(nativeRules.contains("_t1"));
        assertFalse(nativeRules.contains("_t2"));
        assertFalse(sharding.contains("datasource/sharding.yml"));
    }

    private static long count(List<Path> paths, String suffix) {
        return paths.stream().filter(path -> path.getFileName().toString().endsWith(suffix)).count();
    }

    private static String read(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("cannot read generated contract path " + path, exception);
        }
    }
}
