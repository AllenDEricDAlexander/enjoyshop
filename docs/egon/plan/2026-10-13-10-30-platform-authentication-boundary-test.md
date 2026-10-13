# 平台认证边界契约测试实现计划

| Field | Value |
| --- | --- |
| Document | `2026-10-13-10-30-platform-authentication-boundary-test.md` |
| Template Version | `4` |
| Status | `Completed` |
| Created | `2026-10-13 10:30 CST` |
| Updated | `2026-10-13 10:43 CST` |
| Owner | mario |
| Repository | enjoyshop |
| Scope | enjoyshop-service-goods 的平台认证边界契约测试；不修改生产代码、配置、表或页面 |
| Source Requirement | 用户确认 `2026-10-13-10-15-platform-user-authentication-integration.md` 后要求继续写实现计划 |
| Baseline Revision | `main` / `507d417`；实现提交 `b101577`；收尾只记录这两份设计文档，不触碰 enjoyshop-web 与其他未跟踪文档 |
| Implements Spec | [用户认证与 Egon-COLA 权限体系整合](../spec/2026-10-13-10-15-platform-user-authentication-integration.md) |
| Spec Status | `Implemented` |
| Spec Revision | 规格 Updated `2026-10-13 10:43 CST`；实现提交 `b101577`；规划基线 `507d417` |
| Effective Specs | [用户认证与 Egon-COLA 权限体系整合](../spec/2026-10-13-10-15-platform-user-authentication-integration.md) |
| Depends On Plans | `None` |
| Supersedes | `None` |
| Superseded By | `None` |
| Related Plans | `None` |

## 1. Summary

本计划实现认证规格里唯一的代码变更：新增 `PlatformAuthenticationBoundaryContractTest`。测试锁定现有守兵/鉴身过滤链、遗留匿名控制器、权限码和“禁止第二套认证栈”，不新增生产类、依赖、配置或表。

只有 1 个实现步骤。完成证据是在仓库根目录执行规格给出的 Maven 命令，且该测试与列出的既有回归测试通过。本计划不启动守兵、鉴身、Redis 或浏览器登录。

本计划已执行完毕。Step 1 的提交是 `b101577`。主规格收尾为 Implemented，因为规格自己定义的测试都已通过；这不把离线契约测试写成真实登录验收。

## 2. Target Spec and Effective Design

### 2.1 Primary target

- Path: [用户认证与 Egon-COLA 权限体系整合](../spec/2026-10-13-10-15-platform-user-authentication-integration.md)
- Status: `Implemented`
- Revision: Updated `2026-10-13 10:43 CST`，规划基线 `507d417`，实现提交 `b101577`
- Approval evidence: 用户先确认规格并要求写计划，随后确认本计划并授权执行。规划当时不改规格状态；执行完成后再把规格收尾为 Implemented。

### 2.2 Effective Spec set

| Role | Spec/link | Status/revision | Effective sections | Why included |
| --- | --- | --- | --- | --- |
| Primary | [认证整合规格](../spec/2026-10-13-10-15-platform-user-authentication-integration.md) | Implemented，`2026-10-13 10:43 CST` | 全文，尤其 §3.2、§4、§7、§8.3、§14、§16 | 唯一要实施的需求集是 REQ-001 至 REQ-010 |
| Dependency | [首页网关架构](../spec/2026-10-09-16-39-homepage-kubernetes-gateway-architecture.md) | Accepted | §7 | 约束：不新建网关，不把 Edge 登录写进本测试的启动范围 |
| Dependency | [文件目录与上传](../spec/2026-09-28-21-50-distributed-file-catalog-lookups.md) | Accepted | §9 | 约束：上传链保持只有守兵过滤器 |
| Dependency | [广告同步](../spec/2026-10-10-14-48-postgresql-cdc-advertisement-synchronization.md) | Accepted | §7、§9 | 约束：广告管理与公开广告位分链保持不变 |

依赖规格的需求已经由既有代码满足。本计划不重新实现它们，所以它们不进入文首 Effective Specs 的需求提取范围。测试只断言当前代码没有偏离这些约束。

### 2.3 Superseded or excluded content

None。没有被取代的认证计划。畅购示例里的认证服务、JKS、Cookie `uid`、Thymeleaf 和社交登录按主规格非目标排除，不出现在文件树中。

## 3. Effective Requirements and Acceptance

| Requirement | Source Spec section | Effective statement | Observable acceptance | Implementation impact |
| --- | --- | --- | --- | --- |
| REQ-001 | 主规格 §4 | 身份认证只由守兵完成 | goods 无登录、注销、授权码或令牌端点 | 测试扫描源码与 POM，不新增端点 |
| REQ-002 | 主规格 §4 | 一次守兵登录访问已授权 goods 资源 | 不产生 goods 会话或按接口再登录 | 测试断言 formLogin 与 logout 保持关闭 |
| REQ-003 | 主规格 §4 | 商品和广告按鉴身权限码授权 | 权限码正则与 `PermissionRequest.api` 资源名不变 | 反射读取现有组件，不改组件 |
| REQ-004 | 主规格 §4 | 资源服务不信任客户端自报身份 | 管理链先守兵、后鉴身、再匿名过滤器 | 读取过滤链方法源码顺序 |
| REQ-005 | 主规格 §4 | 失败关闭 | 组件仍把非 ALLOW 与不可用映射到 610206、610208、610305、610308 | 既有组件测试回归；新测试确认异常类型仍被引用 |
| REQ-006 | 主规格 §4 | SERVICE 不能冒充 USER | `PrincipalType.USER` 判断仍在 | 反射或源码断言该枚举引用存在 |
| REQ-007 | 主规格 §4、§8.3 | 禁止畅购认证栈 | 无示例类型、JKS、Cookie `uid`、Thymeleaf、授权服务器 | 源码与 POM 断言 |
| REQ-008 | 主规格 §4 | 不实现社交登录 | 无微信或 QQ 授权码交换类型 | 源码禁止对应类名与回调路径 |
| REQ-009 | 主规格 §4、§8.3 | 既有路由分界不变 | 六条链与 11 个控制器名单一致 | 源码扫描 |
| REQ-010 | 主规格 §4、§14 | 契约测试锁定边界 | 未归类 `@RestController` 或第二套栈使构建失败 | 新增这一个测试 |

## 4. Implementation Strategy and Dependency Order

### 4.1 Ordered strategy

生产过滤链和授权组件已经存在，并且规格禁止修改它们。因此没有迁移、代码生成、配置或前端步骤。唯一文件是测试，放在与 `NativeHttpSecurityConfiguration` 相同的包，以便调用包可见的 `@Bean` 方法。测试不启动 `StudentManagementApplication`，避免 `ProductSecurityContractTest` 那种持久化上下文去连接外部资源。

### 4.2 Test-first strategy

Test-first gate 对本步骤不适用。规格 §8 与 §14 规定本轮没有生产代码变更，被断言的行为已经在当前源码中。先写一个故意失败的测试不能产生随后的最小生产修改，除非违反规格去改过滤链。测试写完后应直接 GREEN。若 GREEN 失败，说明断言与当前源码不一致，回到测试文件修正断言或停止并修订规格，不能为了让测试通过而修改生产认证行为。

### 4.3 Sequential and parallel boundaries

| Step | Depends on | May run in parallel with | Must not overlap with | Reason |
| --- | --- | --- | --- | --- |
| Step 1 | None | None | `NativeHttpSecurityConfiguration.java`、两个授权组件、`pom.xml`、四个 `application*.yml` | 这些文件只读；写入范围只有新测试 |

### 4.4 Commit boundaries

Step 1 产生一个只包含新测试文件的提交。规格的 `Related Plans` 回写属于计划编写，不属于该实现提交。

### 4.5 Spec Simplicity and Implementation-necessity Audit

| Spec element | Spec necessity verdict/section | Current repository evidence | Direct/reuse alternative | Interaction/implementation cost | Plan decision |
| --- | --- | --- | --- | --- | --- |
| 认证微服务、登录 API、JKS、Cookie `uid` | Remove，主规格 §7.0 | goods POM 只有守兵与鉴身 Starter 5.4.1 | 现有过滤链 | 会增加第二套令牌状态 | Already exists as rejected；不实施 |
| 微信或 QQ 适配器 | Remove，主规格 §7.0 与 DEC-003 | 平台与 goods 均无该类型 | 不实现 | 新外部密钥与绑定表 | Return to Spec 若要做；本计划不实施 |
| 本地 RolePermissionAggregate 作为生产 PEP | Remove，主规格 §7.0 | `domain.user` 仍是教学样例 | 鉴身快照 | 两套角色 | Already exists as teaching only；不改 |
| 六条 SecurityFilterChain 与两个授权组件 | Keep，主规格 §7.0、§8.3 | `NativeHttpSecurityConfiguration` 与两个组件 | 无更小替代 | 无新调用 | Already exists |
| `PlatformAuthenticationBoundaryContractTest` | Add，主规格 §7.0、§14 | 当前没有这个类；`AdSecurityContractTest` 已有离线源码合同风格 | 只靠评审 | 仅测试，无运行时状态 | Implement |

没有 fetch-then-forward 接口。规格没有要求新的 API、表、缓存或页面。

### 4.6 Change-unit Dependency Matrix

| Change unit | Requirements | Proof/RED point | Compile/runtime prerequisites | Produces | Consumers/unblocks | Owning Step |
| --- | --- | --- | --- | --- | --- | --- |
| 认证边界契约测试 | REQ-001、REQ-002、REQ-003、REQ-004、REQ-005、REQ-006、REQ-007、REQ-008、REQ-009、REQ-010 | 无 RED；当前源码即期望行为 | JUnit 5、AssertJ、现有生产类只读可见 | `PlatformAuthenticationBoundaryContractTest` | 后续认证改动必须先改规格 | Step 1 |

### 4.7 Java, Spring, and Egon-COLA Implementation Standards

| Concern | Current repository evidence | Effective Spec decision | Planned implementation consequence | Owning Steps/checks |
| --- | --- | --- | --- | --- |
| Architecture profile | goods Light 包 `start.config` | 主规格 §6.1：保持 Light | 测试放在现有测试包，不新增模块 | Step 1；MC-ARCH-001 |
| Reuse/capability | 守兵与鉴身 Starter 已在 POM | 主规格 §6.1：不新增依赖 | 测试只反射和读取现有类型 | Step 1；MC-REUSE-001、MC-DEP-001 |
| Naming/model/validation/conversion | 无新生产类型 | 主规格 §6.2 Rule 1–3 为 N/A | 不新增 POJO、分组或 Converter | Step 1；MC-NAME-001、MC-VALID-001、MC-MODEL-001、MC-CONVERT-001 |
| Bean/logging/util/JSON/time/config | 测试类不是 Bean；`AdSecurityContractTest` 无 `@Slf4j` | 主规格 §6.2 | 测试不使用业务 Bean 注解，不改四份配置 | Step 1；MC-LOG-001、MC-BEAN-001、MC-UTIL-001、MC-JSON-001、MC-TIME-001、MC-CONFIG-001 |
| Business variation/pattern | 登录方式不属于 goods | 主规格 §13：禁止 goods 内登录策略 | 测试拒绝畅购登录类型，不新增策略类 | Step 1；MC-PATTERN-001 |

#### Capability reuse ledger

| Need | Candidates inspected | Exact evidence | Fit/gap | Decision | Added dependency/custom code | Owning Step/check |
| --- | --- | --- | --- | --- | --- | --- |
| 过滤链合同 | `AdSecurityContractTest` 读源码；`ProductSecurityContractTest` 启动全应用 | 前者不启动应用；后者继承 `PersistenceTestSupport` | 全应用会超出“不连接 Redis 或守兵” | 复用离线读取加同包反射，不复用全应用测试 | None | Step 1；MC-REUSE-001 |
| 权限码合同 | `ProductAuthorizationComponentTest` | 已覆盖 610206 与 610208 | 足够作为回归，不必在新测试里重新模拟决策 | 新测试只锁定正则、资源名和 USER 类型 | None | Step 1；MC-TEST-001 |
| 依赖禁止 | goods `pom.xml` 直接依赖 | 已有两个 Starter，无 Thymeleaf | 无缺口 | 读取 POM 文本，不新增坐标 | None | Step 1；MC-DEP-001 |

### 4.8 User-mandated Java Rule Implementation Matrix

| Literal rule | Spec source | Repository evidence | Exact files and order | Pseudocode obligations | Validation gate | Steps | Status/blocker |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Rule 1 | 主规格 §6.2 Rule 1 | 无新生产类型 | 只有测试类 | 不创建 DAO、PO、BO、DTO | 测试源码无新生产类型声明 | Step 1 | N/A |
| Rule 2 | 主规格 §6.2 Rule 2 | 现有 `requirePermission` 参数已有 `@Pattern` | 测试反射该注解 | 不断言新分组 | 反射到现有正则 | Step 1 | N/A |
| Rule 3 | 主规格 §6.2 Rule 3 | 无新载体 | 无模型文件 | 不写 Lombok 或 MapStruct | 无新类 | Step 1 | N/A |
| Rule 4 | 主规格 §6.2 Rule 4 | 邻近离线测试没有 `@Slf4j` | 测试类 | 不把测试标成 `@Service` | 源码审查与编译 | Step 1 | N/A |
| Rule 5 | 主规格 §6.2 Rule 5 | JUnit 与 AssertJ 已在测试依赖 | 测试文件 | 只用 JDK、JUnit、AssertJ | POM 与 import 断言 | Step 1 | PASS |
| Rule 6 | 主规格 §6.2 Rule 6 | 无新 JSON | 无 | 不新增枚举编码 | 不改序列化 | Step 1 | N/A |
| Rule 7 | 主规格 §6.2 Rule 7 | 四份配置已由 `ProfileKeyParityTest` 锁定 | 不修改 yml | 新测试不写配置 | 既有 parity 测试仍在回归命令外，本步不改键 | Step 1 | N/A |
| Rule 9 | 主规格 §6.2 Rule 9、§13 | 复杂登录分支被禁止放在 goods | 测试文件 | 断言不存在登录策略类型 | 源码类名扫描 | Step 1 | PASS |
| Rule 10 | 主规格 §6.2 Rule 10 | 无新时间字段 | 无 | 不导入 `java.util.Date` | 测试不新增时间类型 | Step 1 | N/A |
| Rule 11 | 主规格 §6.1、§6.2 Rule 11 | Light 的 `start.config` 测试包已存在 | 测试文件放在该包 | 不新增模块或 `biz` 包 | `LightPersistenceArchitectureTest` 回归 | Step 1 | PASS |

## 5. Change File Tree

```text
CREATE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/start/config/PlatformAuthenticationBoundaryContractTest.java
```

| Operation | Path | Current evidence/symbol | Final symbols/state | Responsibility | Step | Requirements | Validation owner |
| --- | --- | --- | --- | --- | --- | --- | --- |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/start/config/PlatformAuthenticationBoundaryContractTest.java` | 该路径不存在；同包已有 `AdSecurityContractTest` | 类 `PlatformAuthenticationBoundaryContractTest` 与下列四个测试方法 | 锁定认证边界 | Step 1 | REQ-001、REQ-002、REQ-003、REQ-004、REQ-005、REQ-006、REQ-007、REQ-008、REQ-009、REQ-010 | Maven Surefire 选择器 |

四个方法名固定为 `filterChainsMatchTheSpecRoutes`、`registerFilterTrueRejectsProtectedChains`、`authorizationComponentsKeepUserPermissionContracts`、`changgouAuthStackIsAbsent`。

## 6. Prerequisites, Constraints, and Plan Clarifications

### 6.1 Repository and worktree baseline

- 分支 `main`，提交 `507d417`。
- 不得修改 `enjoyshop-web/**`、广告 CDC 文档、搜索文档，以及 goods 的生产源码和 `pom.xml`。
- 实现提交只包含第 5 章那个测试文件。
- 不使用 `git add .`。

### 6.2 Build, test, and environment prerequisites

| Concern | Exact command/source | Required state | Validation boundary |
| --- | --- | --- | --- |
| Build tool | 仓库根目录 `mvn -pl enjoyshop-service/enjoyshop-service-goods -am` | 本地 Maven 可解析父工程与 Starter 5.4.1 | 模块测试，不要求外部守兵 |
| 路径基准 | `AdSecurityContractTest` 使用 `Path.of("src/main/resources", profile)` | Surefire 工作目录是 goods 模块 | 新测试同样使用模块相对路径 |
| 既有回归 | `ProductAuthorizationComponentTest`、`AdManageTest`、`AdSecurityContractTest`、`LightPersistenceArchitectureTest` | 当前基线可编译 | 确认新测试没有破坏架构约束 |

### 6.3 Immutable constraints and approved decisions

- 不修改 `NativeHttpSecurityConfiguration`、`ProductAuthorizationComponent`、`AdAuthorizationComponent`、POM 和四份 `application*.yml`。
- 不新增依赖、SQL、前端页面或 `scripts/work` 脚本。现有 Maven 命令覆盖门禁。
- 品牌、规格、教学控制器保持在默认 `permitAll` 的遗留名单。
- 上传链不得被断言成同时安装鉴身过滤器。

### 6.4 Plan Clarifications

| ID | Small implementation inference | Repository evidence | Why semantics are unchanged | Impact if wrong |
| --- | --- | --- | --- | --- |
| PLAN-CLAR-001 | 用模块相对源码读取加同包反射，而不用 `@SpringBootTest` | `ProductSecurityContractTest` 会启动持久化；规格 §14 禁止连接 Redis 或守兵 | 断言的仍是同一过滤链源码和同一 `@Bean` 方法 | 若必须证明运行时 `FilterChainProxy` 顺序，应追加已有 `ProductSecurityContractTest`，而不是改生产代码 |
| PLAN-CLAR-002 | `@RestController` 扫描使用 `(?<!Advice)` 否定回溯，排除 `@RestControllerAdvice` | `BaseExceptionHandler` 的注解文本包含 `@RestController` 这个前缀 | 遗留名单仍是规格 §8.3 的七个控制器加四个生产控制器 | 若漏掉否定回溯，会把异常处理器算成新的匿名接口 |

## 7. Ordered File-by-file Implementation Steps

### Step 1 — 锁定 goods 平台认证边界

- Requirements: REQ-001、REQ-002、REQ-003、REQ-004、REQ-005、REQ-006、REQ-007、REQ-008、REQ-009、REQ-010
- Dependencies: None
- Baseline state: `507d417` 上六条过滤链、两个授权组件和教学/品牌控制器均已存在；目标测试文件不存在
- Observable outcome: Surefire 能发现 `PlatformAuthenticationBoundaryContractTest`，并对当前源码全部断言通过
- End state: 只有新测试文件被添加；生产认证行为与规格 §8.3 一致
- Test-first gate: Not applicable — 规格禁止生产修改，当前源码已经是被锁定的行为，不存在先失败再补生产代码的缺口
- Manual Checks: MC-ARCH-001、MC-REUSE-001、MC-DEP-001、MC-UTIL-001、MC-PATTERN-001、MC-SCOPE-001、MC-TEST-001
- Literal Rules: Rule 5、Rule 9、Rule 11
- Ordered files:

#### File 1 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/start/config/PlatformAuthenticationBoundaryContractTest.java`

- Purpose: 在不启动应用的前提下锁定规格 §8.3 与 §14 的认证边界。
- Symbols: `PlatformAuthenticationBoundaryContractTest`；方法 `filterChainsMatchTheSpecRoutes`、`registerFilterTrueRejectsProtectedChains`、`authorizationComponentsKeepUserPermissionContracts`、`changgouAuthStackIsAbsent`
- Repository evidence: 同包 `AdSecurityContractTest` 用 `Path.of("src/main/resources")` 和反射读取 `NativeHttpSecurityConfiguration`；`productSecurityFilterChain` 是包可见方法，且在使用 `HttpSecurity` 之前抛出 `IllegalStateException`
- Dependencies and consumers: 只读生产类 `NativeHttpSecurityConfiguration`、`ProductAuthorizationComponent`、`AdAuthorizationComponent`、goods `pom.xml` 与 `src/main/java`；没有生产代码调用这个测试
- Why now: 这是本步骤的唯一交付物
- Contract/signature changes: 无生产签名变化。测试观察到的生产合同是现有方法 `requirePermission(String)`、六个 `@Bean` 过滤链方法和 POM 直接依赖
- Input/output and state mapping: 输入是模块相对源码、POM 文本和包可见方法的布尔参数；输出只是断言。无数据库、缓存、令牌或 MDC 写入
- Error and edge behavior: `register-filter=true` 必须抛出包含 “must only run inside” 或 “must be installed in the file-upload” 的 `IllegalStateException`；控制器数量不是 11 时失败；上传链源码不得出现 `Rbac3BearerAuthenticationFilter`；管理链源码必须先 `addFilterBefore(permissions, AnonymousAuthenticationFilter.class)` 再 `addFilterBefore(identity, Rbac3BearerAuthenticationFilter.class)`
- Standards impact: MC-ARCH-001 测试留在 Light 的 `start.config`；MC-REUSE-001 与 MC-DEP-001 不新增依赖；MC-UTIL-001 只用 JDK、JUnit、AssertJ；MC-PATTERN-001 不新增策略类；MC-SCOPE-001 不改生产文件；MC-TEST-001 由 Surefire 执行
- Literal rule enforcement: Rule 5 不允许 Apache Commons 或 Guava 之外的新工具，本文件只用 `java.nio.file` 与测试库；Rule 9 用类名扫描禁止 goods 内登录策略，而不是写 `if (wechat)`；Rule 11 文件放在现有 Light 测试包，不创建新模块
- Implementation pseudocode:

```java
class PlatformAuthenticationBoundaryContractTest {
    @Test void filterChainsMatchTheSpecRoutes() throws Exception {
        String security = Files.readString(Path.of("src/main/java/top/egon/enjoyshop/goods/start/config/NativeHttpSecurityConfiguration.java"));
        assertThat(security).contains(".formLogin(form -> form.disable())", ".httpBasic(basic -> basic.disable())", ".logout(logout -> logout.disable())");
        assertThat(methodBody(security, "productSecurityFilterChain")).contains("securityMatcher(\"/api/v1/products\", \"/api/v1/products/**\")");
        assertThat(filterOrder(methodBody(security, "productSecurityFilterChain"))).isEqualTo("identity-before-rbac-before-anonymous");
        assertThat(filterOrder(methodBody(security, "adSecurityFilterChain"))).isEqualTo("identity-before-rbac-before-anonymous");
        assertThat(methodBody(security, "fileUploadSecurityFilterChain")).contains("POST").contains("/api/v1/files").doesNotContain("Rbac3BearerAuthenticationFilter");
        assertThat(methodBody(security, "publicAdSecurityFilterChain")).contains("/api/v1/ad-slots/**");
        assertThat(methodBody(security, "productSearchSecurityFilterChain")).contains("GET").contains("/api/v1/product-search").contains("denyAll");
        assertThat(controllerSimpleNames("src/main/java")).containsExactlyInAnyOrder(
                "ProductController", "AdController", "FileController", "ProductSearchController",
                "BrandController", "SpecController", "UserController", "RoleController",
                "PermissionController", "CourseController", "SchoolClassController");
    }

    @Test void registerFilterTrueRejectsProtectedChains() {
        NativeHttpSecurityConfiguration config = new NativeHttpSecurityConfiguration();
        assertThatThrownBy(() -> config.productSecurityFilterChain(mockHttp(), null, null, null, null, true, false))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("product SecurityFilterChain");
        assertThatThrownBy(() -> config.adSecurityFilterChain(mockHttp(), null, null, null, null, false, true))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("ad SecurityFilterChain");
        assertThatThrownBy(() -> config.fileUploadSecurityFilterChain(mockHttp(), null, null, emptyProvider(), true))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("file-upload SecurityFilterChain");
    }

    @Test void authorizationComponentsKeepUserPermissionContracts() throws Exception {
        assertPattern(ProductAuthorizationComponent.class, "enjoyshop:product:(read|write|audit|purge)");
        assertPattern(AdAuthorizationComponent.class, "enjoyshop:ad:(read|write)");
        assertThat(Files.readString(sourceOf(ProductAuthorizationComponent.class))).contains("PrincipalType.USER", "enjoyshop.product", "610206", "610208");
        assertThat(Files.readString(sourceOf(AdAuthorizationComponent.class))).contains("PrincipalType.USER", "enjoyshop.ad", "AD_FORBIDDEN", "AD_AUTHORIZATION_UNAVAILABLE");
    }

    @Test void changgouAuthStackIsAbsent() throws Exception {
        String tree = allMainJava("src/main/java");
        assertThat(tree).doesNotContain("class LoginRedirectController", "class URLFilter", "class CreateJwtTest", "class AuthService", "changgou.jks", "getFirst(\"uid\")");
        assertThat(tree).doesNotContain("weixin", "wechat", "qq.com/oauth");
        assertThat(Files.readString(Path.of("pom.xml"))).doesNotContain("spring-boot-starter-thymeleaf", "spring-security-oauth2-authorization-server");
    }
}
```

- Verification contribution: 四个方法分别对应 TEST-001、注册开关、TEST-002/TEST-003 的合同冻结，以及 TEST-004
- After this file: 测试文件可被 Surefire 编译并直接通过；生产源码字节不变

- Validation working directory: 仓库根目录 `/Users/mario/SelfProject/enjoyshop`
- Verification command: `mvn -pl enjoyshop-service/enjoyshop-service-goods -am -Dtest=PlatformAuthenticationBoundaryContractTest,ProductAuthorizationComponentTest,AdManageTest,AdSecurityContractTest,LightPersistenceArchitectureTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: Maven 退出码 0；`PlatformAuthenticationBoundaryContractTest` 四个方法通过；列出的既有测试通过
- Failure returns to: File 1。若失败原因是生产源码与规格 §8.3 不一致，停止实现并回到规格，不修改生产文件
- Completion criteria: 只有该测试文件变更，命令退出码 0，且没有新增依赖、配置或生产类
- Rollback: 删除这个测试文件即可；没有数据或配置需要回滚
- Commit paths: `enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/start/config/PlatformAuthenticationBoundaryContractTest.java`
- Commit: `test(goods): lock platform authentication boundary`

## 8. Test, Validation, and Quality Gates

| Gate/order | Working directory | Command or method | Scope | Expected result | Failure returns to | Requirements/runtime boundary |
| --- | --- | --- | --- | --- | --- | --- |
| RED for Step 1 | 不适用 | 不先制造失败测试 | 无生产缺口 | 规格禁止先改生产代码再让测试变绿 | File 1 的测试设计 | REQ-010；不连接守兵 |
| GREEN for Step 1 | 仓库根目录 | `mvn -pl enjoyshop-service/enjoyshop-service-goods -am -Dtest=PlatformAuthenticationBoundaryContractTest,ProductAuthorizationComponentTest,AdManageTest,AdSecurityContractTest,LightPersistenceArchitectureTest -Dsurefire.failIfNoSpecifiedTests=false test` | 新测试加四个既有测试类 | 退出码 0 | File 1，或规格 §8.3 若生产源码不匹配 | REQ-001 至 REQ-010；模块，无外部服务 |
| 架构回归 | 同上 | 同一命令中的 `LightPersistenceArchitectureTest` | Light 包边界 | 通过，证明没有新模块 | Step 1 | Rule 11 |
| 运行时登录 | 不执行 | 规格 §3.2 排除真实登录 | 无 | 计划不声称浏览器或守兵已验证 | 不适用 | 用户控制的环境以后单独验收 |

不创建 `scripts/work` 脚本。现有 Maven Surefire 覆盖门禁，最后也没有需要提升或删除的临时脚本。

## 9. Migration, Compatibility, Rollout, and Rollback

N/A。主规格 §11 与 §16 规定没有 DDL、配置键、双写或部署顺序变化。回滚点是删除 Step 1 的测试文件。品牌、上传、公开读和教学接口的运行时行为不变。

## 10. Requirement-to-Step Traceability Matrix

| Requirement | Effective Spec section | Steps | Files | Tests/gates | Completion evidence |
| --- | --- | --- | --- | --- | --- |
| REQ-001 | 主规格 §4、§8 | Step 1 | `PlatformAuthenticationBoundaryContractTest.java` | `changgouAuthStackIsAbsent` | 无登录类型与令牌端点源码 |
| REQ-002 | 主规格 §4、§7 | Step 1 | 同上 | `filterChainsMatchTheSpecRoutes` | formLogin 与 logout 保持关闭 |
| REQ-003 | 主规格 §4、§8.3 | Step 1 | 同上 | `authorizationComponentsKeepUserPermissionContracts` | 商品与广告权限正则仍在 |
| REQ-004 | 主规格 §4、§7.3 | Step 1 | 同上 | `filterChainsMatchTheSpecRoutes` | 过滤器源码顺序符合规格 |
| REQ-005 | 主规格 §4、§14 | Step 1 | 同上，加既有组件测试 | `ProductAuthorizationComponentTest`、`AdManageTest` | 610206、610208、610305、610308 回归 |
| REQ-006 | 主规格 §4 | Step 1 | 同上 | `authorizationComponentsKeepUserPermissionContracts` | `PrincipalType.USER` 仍被要求 |
| REQ-007 | 主规格 §4、§14 TEST-004 | Step 1 | 同上 | `changgouAuthStackIsAbsent` | POM 无 Thymeleaf 与授权服务器 |
| REQ-008 | 主规格 §4、DEC-003 | Step 1 | 同上 | `changgouAuthStackIsAbsent` | 无微信或 QQ OAuth 源码 |
| REQ-009 | 主规格 §4、§8.3 | Step 1 | 同上 | `filterChainsMatchTheSpecRoutes` | 11 个控制器与六条链一致 |
| REQ-010 | 主规格 §4、§14 | Step 1 | 同上 | 整个 GREEN 命令 | 未归类控制器会使精确名单断言失败 |

## 11. Risks, Blockers, and User Decisions

| ID | Risk or decision | Impacted Steps/files | Evidence | Owner | Status/action |
| --- | --- | --- | --- | --- | --- |
| RISK-001 | 规格曾停留在 Review，计划当时不能标 Ready | 整个计划 | 用户随后确认执行；收尾时规格改为 Implemented | mario | Closed：见 §12.7，交付提交 b101577 |
| RISK-002 | 离线源码断言看不到 Spring 最终过滤器顺序 | Step 1 | `ProductSecurityContractTest` 已用 `FilterChainProxy` 覆盖运行时顺序，但会启动持久化 | 实现者 | Closed：按 PLAN-CLAR-001 不把全应用测试放进本步骤 |
| RISK-003 | 品牌与规格写接口仍匿名 | 无文件 | 主规格 RISK-001，本计划明确不改 | mario | Closed：残留风险保留在规格，不在本计划解决 |
| BLOCK-001 | 无未决实现选择 | 无 | §5.4 与 §6.4 没有需要用户回答的问题 | mario | Closed：可以按 Step 1 实现 |

## 12. Review and Acceptance

### 12.1 Original requirement fidelity

REQ-001 至 REQ-010 都由 Step 1 的同一个测试覆盖。社交登录、新微服务、登录页和畅购认证栈保持为不做。用户确认规格后，本计划没有扩大范围。

### 12.2 Spec consistency

文件树与规格 §8.2 一致，只有一个 CREATE。没有把 Remove 项写成实现。两条澄清只影响测试手法，不改变认证语义。没有 fetch-then-forward。

### 12.3 Repository executability

目标测试路径当前不存在。生产方法、权限码、错误码和同包可见性已在 `507d417` 核对。验证命令与规格 §14 一致，工作目录是仓库根，测试内部路径按 Surefire 的模块目录解释。

### 12.4 Test and release completeness

没有 RED 阶段，原因已在 §4.2 说明。GREEN 命令包含架构测试。没有迁移、配置发布或临时 shell 脚本。回滚是删除一个文件。

### 12.5 Blocking Manual Check

| Check ID | Applicability | Status | Evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- |
| MC-ARCH-001 | Applicable | PASS | §4.7；测试包位于现有 Light `start.config` | 不新增模块或传统三层 | None |
| MC-REUSE-001 | Applicable | PASS | §4.7 复用表；离线测试风格来自 `AdSecurityContractTest` | 没有自定义认证框架 | None |
| MC-DEP-001 | Not applicable | N/A | 测试断言 POM 不得新增 Thymeleaf 或授权服务器 | 没有待批准依赖 | None |
| MC-NAME-001 | Not applicable | N/A | 无新生产类型 | 测试类不是 POJO 载体 | None |
| MC-VALID-001 | Not applicable | N/A | 不新增层间对象；只反射现有 `@Pattern` | 无新校验分组 | None |
| MC-MODEL-001 | Not applicable | N/A | 无新 class 载体或 record | 无 Lombok 决策 | None |
| MC-CONVERT-001 | Not applicable | N/A | 无对象转换 | 不绕过 BaseConverter，因为没有转换 | None |
| MC-LOG-001 | Not applicable | N/A | 测试不是 Spring 业务类；邻近离线测试也无 `@Slf4j` | 不伪造业务日志 Bean | None |
| MC-BEAN-001 | Not applicable | N/A | 不新增需注入的 Bean | 无 Qualifier 决策 | None |
| MC-UTIL-001 | Applicable | PASS | 伪代码只用 `java.nio.file`、JUnit、AssertJ | 符合 Rule 5 的 JDK 与既有测试库 | None |
| MC-JSON-001 | Not applicable | N/A | 无新 JSON 合同 | 不改 Jackson | None |
| MC-TIME-001 | Not applicable | N/A | 无新时间字段 | 不引入 `java.util.Date` | None |
| MC-CONFIG-001 | Not applicable | N/A | 不修改四份 profile | 键结构不变 | None |
| MC-PATTERN-001 | Applicable | PASS | §13 与 `changgouAuthStackIsAbsent` | goods 不实现登录策略分支 | None |
| MC-SCOPE-001 | Applicable | PASS | 第 5 章只有一个测试文件 | 生产、web 和文档实现范围未被扩大 | None |
| MC-TEST-001 | Applicable | PASS | §8 的 Maven 命令与四个断言方法 | 边界和既有回归都有退出码标准 | None |
| MC-BLOCKER-001 | Applicable | PASS | §11 没有 Open 项 | 可以按 Review 计划等待用户接受后实现 | None |

### 12.6 Final verdict

PASS — Ready for user review

本节保留计划设计审查时的结论。当前实际执行收尾记录见 §12.7，Header 为 Completed。该状态只覆盖这一个契约测试步骤，不表示守兵、鉴身、Redis 或浏览器登录已经运行。

### 12.7 执行完工与状态收尾（2026-10-13 10:43 CST）

用户确认本计划并要求执行。唯一实现步骤已经验证并单独提交，没有把规格、计划或其他未跟踪文件放进该提交。

| Step | 状态 | 交付提交 | 文件/证据 |
| --- | --- | --- | --- |
| 1 锁定 goods 平台认证边界 | Completed | `b1015772564e170d9287fa949ac1f3122f0b244b` | 只新增 `PlatformAuthenticationBoundaryContractTest`；仓库根目录 Maven 命令于 2026-10-13 10:39 CST 退出码 0，22 个测试通过 |

测试先按源码修正了三处断言手法：方法体从 `throws Exception` 之后截取，`@Pattern` 读取参数注解，上传路径锁定类常量 `FILE_UPLOAD_PATH`。生产过滤链、授权组件、POM 和配置没有修改。没有新建 `scripts/work` 脚本，也没有推送或发布。

## Java / CQE contract review

- 无新的 POJO、枚举或值对象。
- 不修改 MP Starter、分片或 DDL。
- 不新增校验注解；测试只读取现有 `@Pattern`。
- 无新的业务唯一键或 `deleted_at`。
- 无新的 Command、Query、Event、Outbox 或状态机。
- Light 结构保持不变，教学 `domain.user` 不被生产测试当成权限权威。

## Backend generation and dependency evidence

不调用 `scripts/egon-codegen.sh`。没有 CREATE 目录表，也没有新的 Light 模块。goods POM 中守兵与鉴身 Starter `5.4.1` 保持原样，本计划不申请新依赖。
