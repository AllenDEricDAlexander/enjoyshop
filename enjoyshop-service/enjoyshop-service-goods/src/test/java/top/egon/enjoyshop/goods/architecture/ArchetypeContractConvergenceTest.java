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
 * Generated-project contract for the Light archetype layering and common component rules.
 *
 * <p>Every assertion is a source-shape check so the contract is observable before the Spring
 * context exists; the runtime behaviour behind each rule is covered by the family fixtures.</p>
 */
class ArchetypeContractConvergenceTest {

    private static final String ROOT = "top.egon.enjoyshop.goods";

    private static final Path MAIN = Path.of("src/main/java");
    private static final Path TEST = Path.of("src/test/java");

    @Test
    void domainOnlyExposesServicesAndKeepsPortsOutOfDomain() throws IOException {
        List<Path> sources = sources(MAIN);
        String all = join(sources);

        for (String forbidden : List.of(
                ROOT + ".domain.teaching.gateway", ROOT + ".domain.user.gateway",
                ROOT + ".domain.teaching.event", ROOT + ".domain.user.event",
                ROOT + ".domain.teaching.client", ROOT + ".domain.user.client")) {
            assertFalse(all.contains(forbidden), "domain port package must be gone: " + forbidden);
        }
        assertFalse(all.contains("QueryGateway"), "gateway capability must be a domain Service");
        String withoutCdcPublisher = all.replace("CdcEventPublisher", "").replace("cdcEventPublisher", "");
        assertFalse(withoutCdcPublisher.contains("EventPublisher"),
                "publisher capability must be a domain Service");
        String withoutPlatformCache = all.replace("EgonColaCachePort", "").replace("egonColaCachePort", "");
        assertFalse(withoutPlatformCache.contains("CachePort"),
                "data cache must not be a domain port");

        assertFalse(Files.exists(modulePath("domain/teaching")));
        assertFalse(Files.exists(modulePath("domain/user")));
        assertFalse(Files.exists(modulePath("domain/teaching/gateway")));
        assertFalse(Files.exists(modulePath("domain/user/gateway")));
        assertFalse(Files.exists(modulePath("domain/teaching/client")));
        assertFalse(Files.exists(modulePath("domain/user/client")));
        assertFalse(all.contains(ROOT + ".domain.teaching.service.TeachingQueryService"));
        assertFalse(all.contains(ROOT + ".domain.user.service.UserQueryService"));
    }

    @Test
    void externalClientsLiveInInfrastructureClientAndImpl() throws IOException {
        List<Path> sources = sources(MAIN);
        String all = join(sources);

        assertFalse(Files.exists(modulePath("infrastructure/teaching")));
        assertFalse(Files.exists(modulePath("infrastructure/user")));
        assertFalse(all.contains("LocalTeachingQueryService"), "client implementation must use the ClientImpl suffix");
        assertFalse(all.contains("RestUserQueryService"));
    }

    @Test
    void applicationAndAdapterCarriersUseThePojoDirectories() throws IOException {
        for (String path : List.of(
                "application/teaching",
                "application/user",
                "adapter/teaching",
                "adapter/user",
                "adapter/pojo/convertor/LightFacadeConverter.java")) {
            assertFalse(Files.exists(modulePath(path)), "sample carrier must be gone: " + path);
        }
        assertTrue(Files.exists(modulePath("facade/validation/NativeRpcValidationGroup.java")));
        assertFalse(Files.exists(modulePath("application/teaching/assemblers")));
        assertFalse(Files.exists(modulePath("application/user/assemblers")));
        String all = join(sources(MAIN));
        assertFalse(all.contains("Assembler"), "duplicate snapshot assembly must be deleted");
        assertFalse(all.contains(ROOT + ".application.teaching.command"));
        assertFalse(all.contains(ROOT + ".adapter.teaching.dto."));
    }

    @Test
    void technicalRepositoryStaysIndependentOfDaoPoAndConverter() throws IOException {
        assertFalse(Files.exists(Path.of("src/main/resources/mybatis/mapper/teaching")));
        assertFalse(Files.exists(Path.of("src/main/resources/mybatis/mapper/user")));
        String all = join(sources(MAIN));
        assertFalse(all.contains(ROOT + ".infrastructure.teaching.repo.dao"));
        assertFalse(all.contains(ROOT + ".infrastructure.user.repo.po"));

        String scan = read(modulePath("start/StudentManagementApplication.java"));
        assertTrue(scan.contains(ROOT + ".infrastructure.goods.dao"));
        assertTrue(scan.contains(ROOT + ".infrastructure.ad.dao"));
        assertFalse(scan.contains(ROOT + ".infrastructure.teaching.dao"));
        assertFalse(scan.contains(ROOT + ".infrastructure.user.dao"));
        assertFalse(scan.contains("repo.dao"));
    }

    @Test
    void cachedReadsAndWritesStayOnTheSpringProxyOfTheSameRegionKey() throws IOException {
        for (Path path : sources(MAIN)) {
            String text = read(path);
            assertFalse(text.contains("this.findCachedById("), "cache entry must not be self-invoked: " + path);
            assertFalse(text.contains("this.updateCachedById("));
        }
        assertFalse(Files.exists(modulePath("infrastructure/teaching/cache")));
        assertFalse(Files.exists(modulePath("infrastructure/user/cache")));
    }

    @Test
    void idempotencyClaimIsADomainServiceThatValidatorsDoNotTouch() throws IOException {
        for (Path path : sources(MAIN)) {
            String name = path.getFileName().toString();
            if (!name.endsWith("Validator.java")) {
                continue;
            }
            String text = read(path);
            assertFalse(text.contains("RedisTemplate"), "validator must not write the idempotency store: " + name);
            assertFalse(text.contains("Idempotency"));
        }
    }

    @Test
    void manageDependsOnlyOnDomainServices() throws IOException {
        for (Path path : sources(MAIN)) {
            if (!path.getFileName().toString().endsWith("ManageImpl.java")) {
                continue;
            }
            String text = read(path);
            for (String forbidden : List.of("RabbitTemplate", "StringRedisTemplate", "RedisTemplate",
                    "EventPublisher", "CachePort", "QueryGateway", "MqMessageService", "Repository", "DAO")) {
                assertFalse(text.contains(forbidden),
                        "Manage must not reach infrastructure: " + forbidden + " in " + path);
            }
        }
    }

    @Test
    void mqTopologyAndRoutingComeFromOneEnumInsteadOfTypePrefixGuessing() throws IOException {
        String all = join(sources(MAIN));
        assertFalse(all.contains(".startsWith(\"class.\")"), "routing must not guess from the event type");
        assertFalse(all.contains(".startsWith(\"course.\")"));
        assertFalse(all.contains(".startsWith(\"authorization.\")"));

        assertFalse(Files.exists(modulePath("infrastructure/mq")));
        assertFalse(Files.exists(modulePath("infrastructure/mq/MqRouteEnum.java")));
        assertFalse(Files.exists(modulePath("infrastructure/config/RabbitMqConfig.java")));
    }

    @Test
    void customExceptionsAreCommonScopedAndCarryTheStringStatusCode() throws IOException {
        for (String path : List.of(
                "common/exception/BaseBusinessException.java",
                "common/exception/ConfigDecryptException.java")) {
            assertTrue(Files.exists(modulePath(path)), "exception must sit in common: " + path);
        }
        for (String path : List.of(
                "common/exception/TeachingDomainException.java",
                "common/exception/UserDomainException.java",
                "common/exception/TeachingUseCaseException.java",
                "common/exception/UserUseCaseException.java",
                "common/exception/TeachingFacadeException.java",
                "common/exception/UserFacadeException.java")) {
            assertFalse(Files.exists(modulePath(path)), "sample exception must be gone: " + path);
        }
        assertFalse(Files.exists(modulePath("common/exceptions")));
        assertFalse(Files.exists(modulePath("domain/teaching/exceptions")));
        assertFalse(Files.exists(modulePath("facade/teaching/exceptions")));
        assertFalse(Files.exists(modulePath("start/config/encryption/ConfigDecryptException.java")));

        for (Path path : sources(MAIN)) {
            String text = read(path);
            assertFalse(text.contains("extends RuntimeException"),
                    "custom exceptions must inherit the common体系: " + path);
            if (path.startsWith(modulePath("common/exception")) && text.contains("class ") && !text.contains("interface ")) {
                assertTrue(text.contains("CommonException") || text.contains("BusinessException")
                                || text.contains("BaseBusinessException"),
                        "common exception must re-root on the common体系: " + path);
            }
        }
        String base = read(modulePath("common/exception/BaseBusinessException.java"));
        assertTrue(base.contains("extends BusinessException"), "business failure must re-root on common");
        assertTrue(base.contains("String code"), "the stable wire code stays a String");
        assertTrue(read(modulePath("common/exception/ConfigDecryptException.java"))
                .contains("extends CommonException"), "technical failure re-roots on CommonException");
    }

    @Test
    void handwrittenEnumsValidatorsConvertersAndCarriersImplementTheCommonContracts() throws IOException {
        for (Path path : sources(MAIN)) {
            String text = read(path);
            String name = path.getFileName().toString();
            if (text.contains("\npublic enum ") || text.contains("\nenum ")) {
                boolean storedScalarEnum = name.equals("ProductSkuDeletionKindEnum.java")
                        && text.contains("@EnumValue") && text.contains("@JsonValue") && text.contains("@JsonCreator");
                boolean internalActions = name.equals("ProductActionEnum.java")
                        && text.contains("CREATE, EDIT, AUDIT, PULL, PUT, DELETE, RESTORE, PURGE");
                boolean searchSort = name.equals("ProductSearchSortEnum.java") && text.contains("@JsonValue");
                boolean adStatus = name.equals("AdStatusEnum.java")
                        && text.contains("@EnumValue") && text.contains("@JsonValue") && text.contains("@JsonCreator");
                boolean cdcOperation = name.equals("CdcOperationEnum.java")
                        && text.contains("@JsonValue") && text.contains("@JsonCreator");
                assertTrue(text.contains("implements EgonEnum") || text.contains("implements ErrorStatus")
                                || text.contains("extends ") || storedScalarEnum || internalActions
                                || searchSort || adStatus || cdcOperation,
                        "handwritten enum must implement EgonEnum: " + name);
            }
            if (name.endsWith("Validator.java")) {
                if (text.contains("implements ConstraintValidator<")) {
                    assertTrue(text.contains("boolean isValid("), "native constraint must implement evaluation: " + name);
                } else {
                    assertTrue(text.contains("extends BaseValidator"), "validator contract: " + name);
                    assertTrue(text.contains("getValidationUtils"), "validator must expose the common facade: " + name);
                }
            }
            if (name.endsWith("Convertor.java") || name.endsWith("Converter.java")) {
                assertTrue(text.contains("BaseConverter<") || text.contains("BaseForwardConverter<"),
                        "semantic mapping must use the common converter: " + name);
                boolean jsonFieldCodec = name.equals("ProductJsonFieldConverter.java")
                        && text.contains("BaseForwardConverter<String, JsonNode>")
                        && text.contains("@Qualifier(\"jacksonObjectMapper\")") && text.contains("@Named");
                assertTrue(text.contains("@Mapper") || jsonFieldCodec, "semantic mapping must be generated; JSON field codec uses Jackson: " + name);
            }
        }
        String deleted = read(modulePath("common/enums/DeletedStatus.java"));
        assertTrue(deleted.contains("NOT_DELETED(0"));
        assertTrue(deleted.contains("DELETED(1"));
        assertFalse(join(sources(MAIN)).contains(".ordinal()"), "ordinal must never act as the code");
        assertFalse(Files.exists(modulePath("facade/teaching")));
        assertFalse(Files.exists(modulePath("facade/user")));
    }

    @Test
    void nativeFacadeHasOneProtobufChainAndFacadeAssertsDelegateToCommonValidation() throws IOException {
        List<Path> sources = sources(MAIN);
        String all = join(sources);
        assertFalse(all.contains(ROOT + ".facade.rpc.CourseRpcService"));
        for (String removed : List.of("CourseRpcProvider", "SchoolClassRpcProvider",
                "UserRpcProvider", "PermissionRpcProvider")) {
            assertFalse(all.contains(removed), "the wrapper provider folds into the facade impl: " + removed);
        }
        assertFalse(Files.exists(modulePath("adapter/teaching/rpc")));
        assertFalse(Files.exists(modulePath("adapter/user/rpc")));
        assertFalse(Files.exists(modulePath("facade/rpc")));
        assertFalse(Files.exists(modulePath("adapter/validation")),
                "the native group belongs to the published facade contract");
        for (Path path : sources) {
            if (path.startsWith(modulePath("facade"))) {
                assertFalse(read(path).contains(ROOT + ".adapter."),
                        "facade must stay a self-contained contract layer: " + path);
            }
        }

        assertEquals(0, count(all, "@EgonRpcService"), "sample protobuf facades are gone");
        assertEquals(0, count(all, "@EgonRpcProvider"), "sample providers are gone");
        assertFalse(Files.exists(Path.of("src/main/proto/teaching_user_facade.proto")));
    }

    @Test
    void daoMonitoringAspectWatchesTheDaoLayerAndInfrastructureLogsStayOnFailureBoundaries() throws IOException {
        assertTrue(Files.exists(modulePath("infrastructure/aop/DaoMonitorAspect.java")));
        assertFalse(Files.exists(modulePath("infrastructure/aop/RepositoryMonitorAspect.java")));
        String aspect = read(modulePath("infrastructure/aop/DaoMonitorAspect.java"));
        assertTrue(aspect.contains("infrastructure.dao"));
        assertTrue(aspect.contains("*DAO"));
        assertFalse(aspect.contains("..repo.."), "service/cache/converter must not be counted as DAO");
        String logAspect = read(modulePath("infrastructure/aop/InfrastructureLogAspect.java"));
        assertTrue(logAspect.contains("client.impl"));
        assertTrue(logAspect.contains("mq"));
        assertFalse(logAspect.contains("converter"));
    }

    @Test
    void obsoleteInfrastructurePackagesAndDuplicateFacadesAreGone() throws IOException {
        for (String path : List.of(
                "domain/teaching/client", "domain/user/client",
                "infrastructure/teaching/cache", "infrastructure/user/cache",
                "infrastructure/teaching/mq", "infrastructure/user/mq",
                "adapter/teaching/rpc", "adapter/user/rpc",
                "facade/rpc", "common/exceptions",
                "application/teaching/assemblers", "application/user/assemblers",
                "application/teaching/command", "application/teaching/query", "application/teaching/result",
                "application/user/command", "application/user/query", "application/user/result",
                "adapter/teaching/dto", "adapter/teaching/vo", "adapter/teaching/convertor",
                "adapter/user/dto", "adapter/user/vo", "adapter/user/convertor")) {
            assertFalse(Files.exists(modulePath(path)), "obsolete package must not survive: " + path);
        }
        String all = join(sources(MAIN));
        assertFalse(all.contains("InMemoryCourseCacheService"));
        assertFalse(all.contains("InMemoryUserCacheService"));
        assertFalse(Files.exists(modulePath("infrastructure/config/LocalAdapterConfiguration.java")));
        assertFalse(Files.exists(modulePath("infrastructure/config/ExternalClientConfig.java")));
    }

    @Test
    void testSurfaceKeepsTheLocalFakeAndCoversTheMovedContracts() throws IOException {
        // This fixture is the verifier, so its own forbidden-path literals must not count as usage.
        List<Path> fixtures = sources(TEST).stream()
                .filter(path -> !path.getFileName().equals(Path.of("ArchetypeContractConvergenceTest.java")))
                .toList();
        String all = join(fixtures);
        assertFalse(all.contains("CourseCachePort"));
        assertFalse(all.contains("TeachingEventPublisher"));
        assertFalse(all.contains("TeachingQueryGateway"));
        assertFalse(all.contains("RepositoryMonitorAspect"));
        assertFalse(all.contains("facade.rpc.LightFacadeConverter"));
        assertFalse(all.contains("RecordingMqMessageService"));
        assertFalse(all.contains("LocalMqMessageService"));
    }

    private static Path modulePath(String relative) {
        return MAIN.resolve(ROOT.replace('.', '/')).resolve(relative);
    }

    private static List<Path> sources(Path root) throws IOException {
        try (Stream<Path> paths = Files.walk(root)) {
            return paths.filter(path -> path.toString().endsWith(".java")).sorted().toList();
        }
    }

    private static String join(List<Path> paths) {
        StringBuilder builder = new StringBuilder();
        for (Path path : paths) {
            builder.append(read(path));
        }
        return builder.toString();
    }

    private static String read(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("cannot read contract path " + path, exception);
        }
    }

    private static long countMatches(String text, String regex) {
        return java.util.regex.Pattern.compile(regex).matcher(text).results().count();
    }

    private static int count(String text, String needle) {
        int total = 0;
        for (int index = text.indexOf(needle); index >= 0; index = text.indexOf(needle, index + 1)) {
            total++;
        }
        return total;
    }
}
