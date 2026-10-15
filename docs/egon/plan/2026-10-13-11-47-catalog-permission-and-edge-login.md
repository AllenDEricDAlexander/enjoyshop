# 目录权限与 Edge 登录实现计划

| Field | Value |
| --- | --- |
| Document | `2026-10-13-11-47-catalog-permission-and-edge-login.md` |
| Template Version | `4` |
| Status | `Review` |
| Created | `2026-10-13 11:47 CST` |
| Updated | `2026-10-13 11:47 CST` |
| Owner | mario |
| Repository | enjoyshop |
| Scope | goods 的品牌、规格、上传授权，样例纵向删除与架构合同改写，以及网页 Edge 登录回跳 |
| Source Requirement | 用户接受规格并要求继续写实现计划；样例删除已选定整段删除并改写架构合同 |
| Baseline Revision | `main` / `17cfa79` |
| Implements Spec | [目录权限与 Edge 登录](../spec/2026-10-13-11-06-catalog-permission-and-edge-login.md) |
| Spec Status | `Accepted` |
| Spec Revision | `Updated 2026-10-13 11:41 CST`，基线 `main` / `17cfa79` |
| Effective Specs | [目录权限与 Edge 登录](../spec/2026-10-13-11-06-catalog-permission-and-edge-login.md) |
| Depends On Plans | None |
| Supersedes | None |
| Superseded By | None |
| Related Plans | [认证边界测试](2026-10-13-10-30-platform-authentication-boundary-test.md) |

## 1. Summary

本计划实现已接受规格：品牌读写、规格查询和上传在守兵验签之后按鉴身权限码失败关闭；用户与教学样例纵向删除，架构合同不再要求这些样例存在；网页继续使用御衡 Edge，401 回到 `#/connection`。不新增依赖、DDL、YAML 开关或登录端点。计划状态是 Review，用户接受前不写生产代码。

最终证据是六步各自的聚焦测试，加上 goods 模块测试和 `enjoyshop-web` 的 `npm test`。这些测试不启动真实守兵。

## 2. Target Spec and Effective Design

唯一主规格是 [目录权限与 Edge 登录](../spec/2026-10-13-11-06-catalog-permission-and-edge-login.md)。用户在本轮说“接受，继续写实现计划”，状态因此记为 Accepted。随后用户选定整段删除样例并改写架构合同，该决定已写入同一规格的 DEC-007，并于 2026-10-13 11:41 CST 通过严格校验。

它修订认证边界规格里尚未执行的品牌、规格、教学和上传例外，修订品牌骨架里未执行的权限文字，并修订文件规格“上传只验守兵身份”的条款。首页网关规格仍规定浏览器登录属于 Edge。商品、广告、公开搜索和公开广告位的权限码不在本计划里改写。前一份认证边界计划已经完成，只作为关系记录，不提供新的需求编号。

工作区里 `docs/egon/spec/2026-10-13-10-15-platform-user-authentication-integration.md` 已有未提交修改。实施提交不得把它或本计划以外的脏文件带进去。

## 3. Effective Requirements and Acceptance

| ID | 规格位置 | 验收 | 实现影响 |
| --- | --- | --- | --- |
| REQ-001 | §4、§9 API-001/002 | 品牌读权限；无令牌 401；拒绝 610111；不可用 610112 | 品牌链、`brandAuthorizationComponent`、两个读方法 |
| REQ-002 | §4、§9 API-003/004/005 | 品牌写权限在领域调用之前 | 三个写方法，成功体不变 |
| REQ-003 | §4、§9 API-006 | 规格读权限；610113/610114；不查库 | 规格链和 `specManage` |
| REQ-004 | §4、§9 API-007 | 上传同时有守兵 USER 与 `enjoyshop:file:write` | 上传链补鉴身过滤器；拒绝时不调用对象存储 |
| REQ-005 | §4、§8、DEC-007 | 样例 HTTP/GraphQL/RPC 消失，架构合同改写 | 删除样例纵向和只被它们使用的文件 |
| REQ-006 | §4、§12 | 空 registration ID 不跳转；401 进入 `#/connection` | `connection.js` 已有空 ID 失败；`api.js` 补回跳 |
| REQ-007 | §4、§7、§15 | 三条路径忽略 `X-Tenant-Id` 与 `X-Actor-Id` | `TenantContextFilter` 与网页停止发送身份头 |
| REQ-008 | §4、§7 | 商品、广告、公开搜索、公开广告位不变 | 过滤链断言保持旧匹配器和旧权限码 |

## 4. Implementation Strategy and Dependency Order

授权组件没有调用方也能编译，所以先写组件测试和组件，再让 Manage 调用它们，然后才安装过滤链。OpenAPI 只描述已经决定的认证，放在链之后。样例删除会拆掉大量类型，必须等品牌授权编译稳定后再做，并且同一提交里改写架构测试，否则中途不能编译。网页放在最后，因为它只消费已经存在的 401 和已删除的路由。

六步串行。每步一个提交。没有并行写范围。不运行代码生成器，不新增 Maven 依赖，不写 SQL。

### 4.5 Spec Simplicity and Implementation-necessity Audit

| Spec element | Spec necessity verdict/section | Current repository evidence | Plan action |
| --- | --- | --- | --- |
| 三个授权组件 | Add，§7.0 | 商品和广告已经是同形组件，没有 `@PreAuthorize` | Step 1 复用 `AuthorizationService` |
| 品牌链与规格链 | Add，§7.0 | 两条路径落在默认 permitAll | Step 3 复制商品链 |
| 上传链补鉴身过滤器 | Add，§7.0 | 上传链只有守兵过滤器 | Step 3 按商品链安装两个过滤器 |
| 样例纵向与样例 Rabbit 配置 | Remove，DEC-007 | `RabbitMqConfig` 只声明用户和课程导入队列；CDC 用 `CdcRabbitConfiguration` | Step 5 删除并改写哈希，不改 CDC |
| 本地登录页 | Remove，§7.0 | Edge 地址已在 `connection.js` | Step 6 不新增表单或令牌存储 |
| 新表或新依赖 | Not applicable，§11 | 权限在鉴身快照 | 不实施 |

### 4.6 Change-unit Dependency Matrix

| Change unit | Requirements | Proof/RED point | 前置 | Step |
| --- | --- | --- | --- | --- |
| 错误码与三个授权组件 | REQ-001 至 REQ-004 | 类不存在时测试无法加载 | 无 | 1 |
| Manage 在校验和领域之前调用组件 | REQ-001 至 REQ-004、REQ-007 | 现有 Manage 测试看不到 `requirePermission` | Step 1 | 2 |
| 过滤链与租户头跳过 | REQ-001、REQ-003、REQ-004、REQ-007、REQ-008 | 边界测试仍断言上传链没有鉴身过滤器 | Step 2 | 3 |
| OpenAPI bearerAuth | REQ-001 至 REQ-004 | 上传合同仍断言 `shoubingBearer` | Step 3 | 4 |
| 样例删除与架构合同 | REQ-005 | 架构测试仍要求样例类型存在 | Step 4 | 5 |
| 网页登录回跳与样例页删除 | REQ-005、REQ-006、REQ-007 | 网页测试仍请求 `/api/users` 并发送身份头 | Step 5 | 6 |

### 4.7 Java, Spring, and Egon-COLA Implementation Standards

剖面是现有 Egon-COLA Light：`adapter`、`application`、`domain`、`infrastructure`、`start`。证据是 `StudentManagementApplication` 与 `LightPersistenceArchitectureTest`。不新增层，不把样例包改成另一套结构。

| Concern | Current repository evidence | Effective Spec decision | Plan enforcement |
| --- | --- | --- | --- |
| 架构剖面 | Light 包已存在 | 新类只进 `application.goods` 与既有 start/adapter 文件 | 不新建包 |
| 复用 | `ProductAuthorizationComponent`、两个过滤器 Bean、`AuthorizationService` | 不新增安全库 | 构造器注入已有 Bean |
| 依赖 | POM 已有守兵和鉴身 Starter 5.4.1 | 不新增依赖 | Step 5 只删一条 MapStruct 包名 |
| 校验 | 品牌、规格、上传的 Manage 接口使用方法参数 `@Valid` | 权限调用位于现有参数校验和领域调用之前 | PLAN-CLAR-001 |
| 配置 | 四份 profile 的守兵键由 `ProfileKeyParityTest` 锁定 | 只删除样例 HTTP 基址 | Step 5 |
| 测试 | 离线合同测试与 Mockito Manage 测试 | 每个行为先 RED | 六步都写明 RED 原因 |

| Need | Candidates inspected | Exact evidence | Fit/gap | Decision |
| --- | --- | --- | --- | --- |
| 权限判断 | Spring Security `@PreAuthorize`；现有 `AuthorizationService` | 商品组件已调用 `PermissionRequest.api` | `@PreAuthorize` 与现有形状不符 | 复用 `AuthorizationService`，不新增依赖 |
| 验签过滤器 | `IdpBearerAuthenticationFilter`、`Rbac3BearerAuthenticationFilter` | `NativeHttpSecurityConfiguration` 已注入这两个 Bean | 无缺口 | 品牌、规格、上传链复用 |
| 参数校验 | 接口 `@Valid`；`ValidationUtils` Bean `egonColaValidationUtils` | `ProductManageImpl` 在权限之后调用 `validation.validate` | 接口 `@Valid` 会在方法体之前执行 | Manage 改为权限后调用 `ValidationUtils` |
| 浏览器登录 | Edge `/oauth2/authorization/{registrationId}` | `connection.js` 已跳转 | 无缺口 | 不新增登录实现 |
| 样例消息 | `RabbitMqConfig` 与 `CdcRabbitConfiguration` | 后者不引用 `MqRouteEnum` | 样例队列不是 CDC | 删除样例拓扑，CDC 文件不改 |

### 4.8 User-mandated Java Rule Implementation Matrix

| Literal rule | Spec source | Repository evidence | Exact files and order | Pseudocode obligations | Validation gate | Steps | Status/blocker |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Rule 1 | §6.2 Rule 1，不新增 POJO | 现有 Command、Query、VO 已按后缀命名 | 不新增载体；Step 2 只改 Manage | 不把权限结果做成新的 DTO | 编译与现有 Web 测试仍映射旧 VO | 1、2 | PASS |
| Rule 2 | §7.3 权限位于现有校验之前 | Manage 接口目前有 `@Valid` | `BrandManage`、`SpecManage`、`FileManage` 及其实现 | 先 `requirePermission`，再 `validation.validate` | Manage 测试证明拒绝时领域未被调用，空上传仍是校验失败 | 2 | PASS |
| Rule 3 | §10 不新增载体 | 无新 class 或 record | 无新文件 | 不引入 record，不绕过现有 MapStruct | 无新转换器测试 | 1 | PASS |
| Rule 4 | §6.2 Rule 4 | `ProductAuthorizationComponent` 的注解 | 三个新组件 | `@Slf4j`、具名 `@Component`、`@RequiredArgsConstructor`、`@Qualifier("authorizationService")` | 组件测试用构造器创建，不读令牌日志 | 1、2 | PASS |
| Rule 5 | §6.1 不新增工具库 | POM 现有依赖 | 无新工具类 | 只使用 JDK 与平台 API | POM 依赖集合不变 | 1、5 | PASS |
| Rule 6 | §9 错误体 | `GoodsErrorStatus implements ErrorStatus` | 枚举末尾六个新常量 | 用枚举构造 `CommonException`，不用 ordinal | 断言 code 与 status 名 | 1 | PASS |
| Rule 7 | DEC-007 | 基址只出现在三份 profile | 三份 YAML | 删除两枚样例 URL；不改守兵默认值 | `ProfileKeyParityTest` | 5 | PASS |
| Rule 9 | §13 不新增策略家族 | 商品是一次显式调用 | 三个同形组件 | 差异只有资源名、正则和错误码 | 边界测试读取 `@Pattern` | 1、3 | PASS |
| Rule 10 | §6.2 Rule 10 | 本轮无新时间字段 | 无 | 不使用 `java.util` 日期 | 无新时间映射 | 1 | PASS |
| Rule 11 | §6.1 Light | 现有分层与样例包 | 新类进入 `application.goods`；样例包删除 | 不新建层，不保留样例来满足计数 | 架构测试与文件计数 | 1 至 6 | PASS |

## 5. Change File Tree

生产代码只新增三个授权组件。品牌、规格、上传的 Manage、过滤链、错误码、三个控制器和 OpenAPI 测试被修改。样例包按目录删除。CDC、商品、广告、搜索、分片 SQL 和 `spring.rabbitmq` 不在树中。

```text
CREATE 三个 AuthorizationComponent 与三个组件测试
MODIFY GoodsErrorStatus、三个 Manage 接口和实现、三个 Manage 测试
MODIFY NativeHttpSecurityConfiguration、TenantContextFilter、PlatformAuthenticationBoundaryContractTest
MODIFY BrandController、SpecController、FileController 与三份 OpenAPI 合同测试
DELETE 样例 main/test 包、infrastructure/mq、样例配置与只服务样例的测试
MODIFY 架构合同、启动类 MapperScan、NativeRpcConfiguration、pom、三份 YAML、两份原生兼容测试
MODIFY enjoyshop-web 的 app.js、api.js、connection.js、两份测试
DELETE enjoyshop-web/src/pages/users.js 与 teaching.js
```

完整路径和所属步骤列在第 7 章每个 File 块。第 7 章的 Commit paths 与这些路径一致。

## 6. Prerequisites, Constraints, and Plan Clarifications

| 条件 | 要求 |
| --- | --- |
| 工作目录 | 仓库根 `/Users/mario/SelfProject/enjoyshop`；网页测试在 `enjoyshop-web` |
| 构建 | `mvn -pl enjoyshop-service/enjoyshop-service-goods -am -Dtest=选择器 -Dsurefire.failIfNoSpecifiedTests=false test` |
| 网页 | `npm test`，脚本是 `node --test test/*.test.mjs` |
| 不可变 | 不改平台源码，不 push，不改 `CdcRabbitConfiguration`，不改商品和广告权限码，不改已应用 SQL，不把 `TIANQUAN_SHOUBING_ENABLED` 默认改成 true |
| 脏文件 | 实施提交只包含该步 Commit paths。前一份认证规格的未提交 diff 排除在外 |
| 生成器 | 不使用。没有 CREATE 脚手架，也没有 ALTER |

| ID | 澄清 | 证据 | 若错了 |
| --- | --- | --- | --- |
| PLAN-CLAR-001 | Manage 接口上的 `@Valid` 由 Spring 在方法体之前执行。为满足规格顺序，三个接口去掉方法参数 `@Valid`，实现在 `requirePermission` 之后调用已有 `egonColaValidationUtils`。控制器上的 `@Valid` 保留 | `ProductManageImpl` 已是这个顺序；`ValidationUtils.validate` 抛 `ConstraintViolationException` | 若必须保留方法参数 `@Valid`，权限就会落在校验之后，需要回到规格 |
| PLAN-CLAR-002 | `DomainServiceImpl.java` 磁盘上是 11 个，合同测试写成 10。删除 5 个样例实现后保留 6 个，断言改为 6。PO、DAO、Repository 从 14 改为 6 | 递归计数与 `javaFileCount` 相同 | 若漏删一个样例实现，Step 5 的计数测试会失败并回到删除清单 |
| PLAN-CLAR-003 | `app.integrations.rabbitmq.enabled` 与 `exchange` 保留，因为 `SAMPLE_RABBITMQ_STATE` 仍读取 enabled。删除的是队列 Bean，不是这个开关 | `NativeReadwriteContractTest` 第 59 到 64 行 | 若实施时删掉该键，必须同时改这份断言，不能只改一份 profile |

没有未决设计分叉。registration ID 继续由部署填写，仓库默认空字符串。

## 7. Ordered File-by-file Implementation Steps
### Step 1 — 追加错误码并添加三个授权组件

- Requirements: REQ-001, REQ-002, REQ-003, REQ-004
- Dependencies: 无。AuthorizationService 与 ProductAuthorizationComponent 已存在。
- Baseline state: GoodsErrorStatus 止于 SEARCH_INTERNAL_ERROR。三个授权组件不存在。
- Observable outcome: ALLOW 调用对应资源上的权限码；DENY、非 USER 和 MDC 不一致返回禁止码；空决策或 INDETERMINATE 返回不可用码。
- End state: 三个组件可构造，尚未被 Manage 或过滤链调用。
- Test-first gate: Required。测试先引用不存在的类，RED 原因是 ClassNotFoundException；枚举和组件补上后 GREEN。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001
- Literal Rules: Rule 1, Rule 3, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11
- Ordered files:

#### File 1 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/application/goods/BrandAuthorizationComponentTest.java`

- Purpose: 先锁定 brand 权限的允许、拒绝、不可用和非 USER。
- Symbols: BrandAuthorizationComponentTest.decisionsFailClosed
- Repository evidence: 同包已有 ProductAuthorizationComponentTest，用反射构造组件并 mock AuthorizationService。
- Dependencies and consumers: 无生产调用方。生产类在本步后面的文件创建。
- Why now: RED 必须先于组件存在。
- Contract/signature changes: 只新增测试，不改生产签名。
- Input/output and state mapping: ALLOW 无异常；DENY 与非 USER 为 610111；INDETERMINATE 和 null 为 610112。
- Error and edge behavior: 没有 Authentication、主体是 SERVICE、MDC tenantId 与令牌不一致都失败关闭，不读取 X-Tenant-Id。
- Standards impact: MC-TEST-001, MC-SCOPE-001
- Literal rule enforcement: Rule 11 测试放在与生产类相同的包，不新建测试层。
- Implementation pseudocode:

```java
class BrandAuthorizationComponentTest {
  @Test void decisionsFailClosed() throws Exception {
    var type = Class.forName("top.egon.enjoyshop.goods.application.goods.BrandAuthorizationComponent");
    AuthorizationService decisions = mock(AuthorizationService.class);
    Object gate = type.getConstructor(AuthorizationService.class).newInstance(decisions);
    Method method = type.getMethod("requirePermission", String.class);
    assertThat(method.getParameters()[0].getAnnotation(Pattern.class).regexp()).isEqualTo("enjoyshop:brand:(read|write)");
    assertThatThrownBy(() -> method.invoke(gate, "enjoyshop:brand:read")).hasRootCauseInstanceOf(CommonException.class);
    installUser("1", "editor");
    PermissionRequest request = PermissionRequest.api("enjoyshop:brand:read", "enjoyshop.brand");
    when(decisions.requirePermission(request)).thenReturn(allow("enjoyshop:brand:read"));
    method.invoke(gate, "enjoyshop:brand:read");
    verify(decisions).requirePermission(request);
    when(decisions.requirePermission(request)).thenReturn(deny("enjoyshop:brand:read"));
    assertCode(method, gate, "enjoyshop:brand:read", 610111);
    when(decisions.requirePermission(request)).thenReturn(indeterminate("enjoyshop:brand:read"));
    assertCode(method, gate, "enjoyshop:brand:read", 610112);
    when(decisions.requirePermission(request)).thenReturn(null);
    assertCode(method, gate, "enjoyshop:brand:read", 610112);
    installPrincipal(PrincipalType.SERVICE);
    assertCode(method, gate, "enjoyshop:brand:read", 610111);
    installUser("1", "editor");
    MDC.put("tenantId", "2");
    assertCode(method, gate, "enjoyshop:brand:read", 610111);
  }
}
```

- Verification contribution: 本步验证命令的第一个失败点。
- After this file: 测试存在且因缺少生产类失败。
#### File 2 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/application/goods/SpecAuthorizationComponentTest.java`

- Purpose: 先锁定 spec 权限的允许、拒绝、不可用和非 USER。
- Symbols: SpecAuthorizationComponentTest.decisionsFailClosed
- Repository evidence: 同包已有 ProductAuthorizationComponentTest，用反射构造组件并 mock AuthorizationService。
- Dependencies and consumers: 无生产调用方。生产类在本步后面的文件创建。
- Why now: RED 必须先于组件存在。
- Contract/signature changes: 只新增测试，不改生产签名。
- Input/output and state mapping: ALLOW 无异常；DENY 与非 USER 为 610113；INDETERMINATE 和 null 为 610114。
- Error and edge behavior: 没有 Authentication、主体是 SERVICE、MDC tenantId 与令牌不一致都失败关闭，不读取 X-Tenant-Id。
- Standards impact: MC-TEST-001, MC-SCOPE-001
- Literal rule enforcement: Rule 11 测试放在与生产类相同的包，不新建测试层。
- Implementation pseudocode:

```java
class SpecAuthorizationComponentTest {
  @Test void decisionsFailClosed() throws Exception {
    var type = Class.forName("top.egon.enjoyshop.goods.application.goods.SpecAuthorizationComponent");
    AuthorizationService decisions = mock(AuthorizationService.class);
    Object gate = type.getConstructor(AuthorizationService.class).newInstance(decisions);
    Method method = type.getMethod("requirePermission", String.class);
    assertThat(method.getParameters()[0].getAnnotation(Pattern.class).regexp()).isEqualTo("enjoyshop:spec:read");
    assertThatThrownBy(() -> method.invoke(gate, "enjoyshop:spec:read")).hasRootCauseInstanceOf(CommonException.class);
    installUser("1", "editor");
    PermissionRequest request = PermissionRequest.api("enjoyshop:spec:read", "enjoyshop.spec");
    when(decisions.requirePermission(request)).thenReturn(allow("enjoyshop:spec:read"));
    method.invoke(gate, "enjoyshop:spec:read");
    verify(decisions).requirePermission(request);
    when(decisions.requirePermission(request)).thenReturn(deny("enjoyshop:spec:read"));
    assertCode(method, gate, "enjoyshop:spec:read", 610113);
    when(decisions.requirePermission(request)).thenReturn(indeterminate("enjoyshop:spec:read"));
    assertCode(method, gate, "enjoyshop:spec:read", 610114);
    when(decisions.requirePermission(request)).thenReturn(null);
    assertCode(method, gate, "enjoyshop:spec:read", 610114);
    installPrincipal(PrincipalType.SERVICE);
    assertCode(method, gate, "enjoyshop:spec:read", 610113);
    installUser("1", "editor");
    MDC.put("tenantId", "2");
    assertCode(method, gate, "enjoyshop:spec:read", 610113);
  }
}
```

- Verification contribution: 本步验证命令的第一个失败点。
- After this file: 测试存在且因缺少生产类失败。
#### File 3 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/application/goods/FileAuthorizationComponentTest.java`

- Purpose: 先锁定 file 权限的允许、拒绝、不可用和非 USER。
- Symbols: FileAuthorizationComponentTest.decisionsFailClosed
- Repository evidence: 同包已有 ProductAuthorizationComponentTest，用反射构造组件并 mock AuthorizationService。
- Dependencies and consumers: 无生产调用方。生产类在本步后面的文件创建。
- Why now: RED 必须先于组件存在。
- Contract/signature changes: 只新增测试，不改生产签名。
- Input/output and state mapping: ALLOW 无异常；DENY 与非 USER 为 610115；INDETERMINATE 和 null 为 610116。
- Error and edge behavior: 没有 Authentication、主体是 SERVICE、MDC tenantId 与令牌不一致都失败关闭，不读取 X-Tenant-Id。
- Standards impact: MC-TEST-001, MC-SCOPE-001
- Literal rule enforcement: Rule 11 测试放在与生产类相同的包，不新建测试层。
- Implementation pseudocode:

```java
class FileAuthorizationComponentTest {
  @Test void decisionsFailClosed() throws Exception {
    var type = Class.forName("top.egon.enjoyshop.goods.application.goods.FileAuthorizationComponent");
    AuthorizationService decisions = mock(AuthorizationService.class);
    Object gate = type.getConstructor(AuthorizationService.class).newInstance(decisions);
    Method method = type.getMethod("requirePermission", String.class);
    assertThat(method.getParameters()[0].getAnnotation(Pattern.class).regexp()).isEqualTo("enjoyshop:file:write");
    assertThatThrownBy(() -> method.invoke(gate, "enjoyshop:file:write")).hasRootCauseInstanceOf(CommonException.class);
    installUser("1", "editor");
    PermissionRequest request = PermissionRequest.api("enjoyshop:file:write", "enjoyshop.file");
    when(decisions.requirePermission(request)).thenReturn(allow("enjoyshop:file:write"));
    method.invoke(gate, "enjoyshop:file:write");
    verify(decisions).requirePermission(request);
    when(decisions.requirePermission(request)).thenReturn(deny("enjoyshop:file:write"));
    assertCode(method, gate, "enjoyshop:file:write", 610115);
    when(decisions.requirePermission(request)).thenReturn(indeterminate("enjoyshop:file:write"));
    assertCode(method, gate, "enjoyshop:file:write", 610116);
    when(decisions.requirePermission(request)).thenReturn(null);
    assertCode(method, gate, "enjoyshop:file:write", 610116);
    installPrincipal(PrincipalType.SERVICE);
    assertCode(method, gate, "enjoyshop:file:write", 610115);
    installUser("1", "editor");
    MDC.put("tenantId", "2");
    assertCode(method, gate, "enjoyshop:file:write", 610115);
  }
}
```

- Verification contribution: 本步验证命令的第一个失败点。
- After this file: 测试存在且因缺少生产类失败。
#### File 4 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/enums/GoodsErrorStatus.java`

- Purpose: 按编号追加六个权限错误码，不改已有常量。
- Symbols: BRAND_FORBIDDEN, BRAND_AUTHORIZATION_UNAVAILABLE, SPEC_FORBIDDEN, SPEC_AUTHORIZATION_UNAVAILABLE, FILE_FORBIDDEN, FILE_AUTHORIZATION_UNAVAILABLE
- Repository evidence: 枚举实现 ErrorStatus，getStatus 返回 name()，现有码止于 SEARCH_INTERNAL_ERROR。
- Dependencies and consumers: 三个新组件和后续 Manage 测试读取这些常量。
- Why now: 组件要引用稳定错误码，所以枚举先于组件。
- Contract/signature changes: 只在枚举常量区末尾追加，不重排 610101 至 610499。
- Input/output and state mapping: 610111 至 610116 的 code、status 名和 message 与规格 JSON 一致。
- Error and edge behavior: 未知决策不能复用商品 610206 或广告 610305。
- Standards impact: MC-JSON-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 6 新常量实现 ErrorStatus，code 是显式整数而不是 ordinal。
- Implementation pseudocode:

```java
BRAND_FORBIDDEN(610111, "Brand permission is denied"),
BRAND_AUTHORIZATION_UNAVAILABLE(610112, "Brand authorization is unavailable"),
SPEC_FORBIDDEN(610113, "Spec permission is denied"),
SPEC_AUTHORIZATION_UNAVAILABLE(610114, "Spec authorization is unavailable"),
FILE_FORBIDDEN(610115, "File permission is denied"),
FILE_AUTHORIZATION_UNAVAILABLE(610116, "File authorization is unavailable");
```

- Verification contribution: 组件测试断言 getCode 与 name。
- After this file: 六个常量存在，旧常量的编号不变。
#### File 5 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/BrandAuthorizationComponent.java`

- Purpose: 按商品组件的形状校验 enjoyshop.brand。
- Symbols: brandAuthorizationComponent
- Repository evidence: ProductAuthorizationComponent 已比对 Rbac3ContextAuthentication、PrincipalType.USER、租户、主体、systemCode 和 fenced。
- Dependencies and consumers: Step 2 的 Manage 是唯一调用方。本步没有过滤链。
- Why now: 测试已经引用这个类名和构造器。
- Contract/signature changes: requirePermission(String) 的 @Pattern 为 enjoyshop:brand:(read|write)。
- Input/output and state mapping: 资源名固定为 enjoyshop.brand。日志不包含令牌。
- Error and edge behavior: 缺身份沿用商品组件的 401000；业务拒绝是 610111；鉴身不可用是 610112。不回退到请求头。
- Standards impact: MC-ARCH-001, MC-REUSE-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 4 使用 @Slf4j、@Component 具名和 @RequiredArgsConstructor，并把 AuthorizationService 标成 @Qualifier("authorizationService")；Rule 6 用 GoodsErrorStatus 构造 CommonException，不用 ordinal；Rule 11 类放在已有 application.goods。
- Implementation pseudocode:

```java
@Component("brandAuthorizationComponent")
@Validated
@Slf4j
@RequiredArgsConstructor
public class BrandAuthorizationComponent {
  @Qualifier("authorizationService")
  private final AuthorizationService authorization;
  public void requirePermission(@NotBlank @Pattern(regexp = "enjoyshop:brand:(read|write)") String permissionCode) {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken) {
      throw new CommonException(401000, "UNAUTHORIZED", "Verified user authentication is required");
    }
    if (!(authentication instanceof Rbac3ContextAuthentication bound) || bound.context() == null
        || bound.context().identity() == null || bound.context().snapshot() == null) {
      throw new CommonException(GoodsErrorStatus.BRAND_AUTHORIZATION_UNAVAILABLE);
    }
    var identity = bound.context().identity();
    var snapshot = bound.context().snapshot();
    if (identity.principalType() != PrincipalType.USER
        || !Objects.equals(MDC.get("tenantId"), identity.tenantId())
        || !Objects.equals(MDC.get("userId"), identity.subject())
        || !Objects.equals(identity.tenantId(), snapshot.tenantId())
        || !Objects.equals(identity.subject(), snapshot.identitySub())
        || !"enjoyshop-goods".equals(snapshot.systemCode())
        || bound.context().fenced()) {
      throw new CommonException(GoodsErrorStatus.BRAND_FORBIDDEN);
    }
    var decision = authorization.requirePermission(PermissionRequest.api(permissionCode, "enjoyshop.brand"));
    if (decision == null || decision.decision() == Decision.INDETERMINATE) {
      throw new CommonException(GoodsErrorStatus.BRAND_AUTHORIZATION_UNAVAILABLE);
    }
    if (decision.decision() != Decision.ALLOW
        || !Objects.equals(decision.permissionCode(), permissionCode)
        || !Objects.equals(decision.tenantId(), identity.tenantId())
        || decision.authVersion() != snapshot.authVersion()
        || decision.policyVersion() != snapshot.policyVersion()) {
      throw new CommonException(GoodsErrorStatus.BRAND_FORBIDDEN);
    }
    log.info("brand permission allowed");
  }
}
```

- Verification contribution: 同名测试从 RED 变为 GREEN。
- After this file: brandAuthorizationComponent 可被后续 Manage 注入。
#### File 6 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/SpecAuthorizationComponent.java`

- Purpose: 按商品组件的形状校验 enjoyshop.spec。
- Symbols: specAuthorizationComponent
- Repository evidence: ProductAuthorizationComponent 已比对 Rbac3ContextAuthentication、PrincipalType.USER、租户、主体、systemCode 和 fenced。
- Dependencies and consumers: Step 2 的 Manage 是唯一调用方。本步没有过滤链。
- Why now: 测试已经引用这个类名和构造器。
- Contract/signature changes: requirePermission(String) 的 @Pattern 为 enjoyshop:spec:read。
- Input/output and state mapping: 资源名固定为 enjoyshop.spec。日志不包含令牌。
- Error and edge behavior: 缺身份沿用商品组件的 401000；业务拒绝是 610113；鉴身不可用是 610114。不回退到请求头。
- Standards impact: MC-ARCH-001, MC-REUSE-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 4 使用 @Slf4j、@Component 具名和 @RequiredArgsConstructor，并把 AuthorizationService 标成 @Qualifier("authorizationService")；Rule 6 用 GoodsErrorStatus 构造 CommonException，不用 ordinal；Rule 11 类放在已有 application.goods。
- Implementation pseudocode:

```java
@Component("specAuthorizationComponent")
@Validated
@Slf4j
@RequiredArgsConstructor
public class SpecAuthorizationComponent {
  @Qualifier("authorizationService")
  private final AuthorizationService authorization;
  public void requirePermission(@NotBlank @Pattern(regexp = "enjoyshop:spec:read") String permissionCode) {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken) {
      throw new CommonException(401000, "UNAUTHORIZED", "Verified user authentication is required");
    }
    if (!(authentication instanceof Rbac3ContextAuthentication bound) || bound.context() == null
        || bound.context().identity() == null || bound.context().snapshot() == null) {
      throw new CommonException(GoodsErrorStatus.SPEC_AUTHORIZATION_UNAVAILABLE);
    }
    var identity = bound.context().identity();
    var snapshot = bound.context().snapshot();
    if (identity.principalType() != PrincipalType.USER
        || !Objects.equals(MDC.get("tenantId"), identity.tenantId())
        || !Objects.equals(MDC.get("userId"), identity.subject())
        || !Objects.equals(identity.tenantId(), snapshot.tenantId())
        || !Objects.equals(identity.subject(), snapshot.identitySub())
        || !"enjoyshop-goods".equals(snapshot.systemCode())
        || bound.context().fenced()) {
      throw new CommonException(GoodsErrorStatus.SPEC_FORBIDDEN);
    }
    var decision = authorization.requirePermission(PermissionRequest.api(permissionCode, "enjoyshop.spec"));
    if (decision == null || decision.decision() == Decision.INDETERMINATE) {
      throw new CommonException(GoodsErrorStatus.SPEC_AUTHORIZATION_UNAVAILABLE);
    }
    if (decision.decision() != Decision.ALLOW
        || !Objects.equals(decision.permissionCode(), permissionCode)
        || !Objects.equals(decision.tenantId(), identity.tenantId())
        || decision.authVersion() != snapshot.authVersion()
        || decision.policyVersion() != snapshot.policyVersion()) {
      throw new CommonException(GoodsErrorStatus.SPEC_FORBIDDEN);
    }
    log.info("spec permission allowed");
  }
}
```

- Verification contribution: 同名测试从 RED 变为 GREEN。
- After this file: specAuthorizationComponent 可被后续 Manage 注入。
#### File 7 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/FileAuthorizationComponent.java`

- Purpose: 按商品组件的形状校验 enjoyshop.file。
- Symbols: fileAuthorizationComponent
- Repository evidence: ProductAuthorizationComponent 已比对 Rbac3ContextAuthentication、PrincipalType.USER、租户、主体、systemCode 和 fenced。
- Dependencies and consumers: Step 2 的 Manage 是唯一调用方。本步没有过滤链。
- Why now: 测试已经引用这个类名和构造器。
- Contract/signature changes: requirePermission(String) 的 @Pattern 为 enjoyshop:file:write。
- Input/output and state mapping: 资源名固定为 enjoyshop.file。日志不包含令牌。
- Error and edge behavior: 缺身份沿用商品组件的 401000；业务拒绝是 610115；鉴身不可用是 610116。不回退到请求头。
- Standards impact: MC-ARCH-001, MC-REUSE-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 4 使用 @Slf4j、@Component 具名和 @RequiredArgsConstructor，并把 AuthorizationService 标成 @Qualifier("authorizationService")；Rule 6 用 GoodsErrorStatus 构造 CommonException，不用 ordinal；Rule 11 类放在已有 application.goods。
- Implementation pseudocode:

```java
@Component("fileAuthorizationComponent")
@Validated
@Slf4j
@RequiredArgsConstructor
public class FileAuthorizationComponent {
  @Qualifier("authorizationService")
  private final AuthorizationService authorization;
  public void requirePermission(@NotBlank @Pattern(regexp = "enjoyshop:file:write") String permissionCode) {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken) {
      throw new CommonException(401000, "UNAUTHORIZED", "Verified user authentication is required");
    }
    if (!(authentication instanceof Rbac3ContextAuthentication bound) || bound.context() == null
        || bound.context().identity() == null || bound.context().snapshot() == null) {
      throw new CommonException(GoodsErrorStatus.FILE_AUTHORIZATION_UNAVAILABLE);
    }
    var identity = bound.context().identity();
    var snapshot = bound.context().snapshot();
    if (identity.principalType() != PrincipalType.USER
        || !Objects.equals(MDC.get("tenantId"), identity.tenantId())
        || !Objects.equals(MDC.get("userId"), identity.subject())
        || !Objects.equals(identity.tenantId(), snapshot.tenantId())
        || !Objects.equals(identity.subject(), snapshot.identitySub())
        || !"enjoyshop-goods".equals(snapshot.systemCode())
        || bound.context().fenced()) {
      throw new CommonException(GoodsErrorStatus.FILE_FORBIDDEN);
    }
    var decision = authorization.requirePermission(PermissionRequest.api(permissionCode, "enjoyshop.file"));
    if (decision == null || decision.decision() == Decision.INDETERMINATE) {
      throw new CommonException(GoodsErrorStatus.FILE_AUTHORIZATION_UNAVAILABLE);
    }
    if (decision.decision() != Decision.ALLOW
        || !Objects.equals(decision.permissionCode(), permissionCode)
        || !Objects.equals(decision.tenantId(), identity.tenantId())
        || decision.authVersion() != snapshot.authVersion()
        || decision.policyVersion() != snapshot.policyVersion()) {
      throw new CommonException(GoodsErrorStatus.FILE_FORBIDDEN);
    }
    log.info("file permission allowed");
  }
}
```

- Verification contribution: 同名测试从 RED 变为 GREEN。
- After this file: fileAuthorizationComponent 可被后续 Manage 注入。

- Validation working directory: /Users/mario/SelfProject/enjoyshop
- Verification command: `mvn -pl enjoyshop-service/enjoyshop-service-goods -am -Dtest=BrandAuthorizationComponentTest,SpecAuthorizationComponentTest,FileAuthorizationComponentTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: 首次只有测试时失败；本步结束时进程退出码 0，三个测试通过。
- Failure returns to: BrandAuthorizationComponentTest、SpecAuthorizationComponentTest、FileAuthorizationComponentTest
- Completion criteria: 六个新错误码和三个组件的允许、拒绝、不可用、非 USER、MDC 不一致都被断言。
- Rollback: 删除本步三个测试、三个组件，并撤销 GoodsErrorStatus 的六行追加。
- Commit paths: enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/application/goods/BrandAuthorizationComponentTest.java, enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/application/goods/SpecAuthorizationComponentTest.java, enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/application/goods/FileAuthorizationComponentTest.java, enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/enums/GoodsErrorStatus.java, enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/BrandAuthorizationComponent.java, enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/SpecAuthorizationComponent.java, enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/FileAuthorizationComponent.java
- Commit: `feat(goods): add brand spec and file permission gates`

### Step 2 — 在校验和领域调用之前接入授权

- Requirements: REQ-001, REQ-002, REQ-003, REQ-004, REQ-007
- Dependencies: Step 1 的三个组件和错误码。
- Baseline state: BrandManageImpl 只在写方法里读取 MDC userId。SpecManageImpl 与 FileManageImpl 直接进入领域。三个接口的方法参数带 @Valid。
- Observable outcome: 权限被拒绝时领域服务和对象存储端口都没有被调用；允许之后才校验命令并进入原业务。写品牌时 userId 仍来自 MDC，而不是 X-Actor-Id。
- End state: 七个受保护操作都先调用对应组件。控制器 URL 和成功体不变。
- Test-first gate: Required。先改三份 Manage 测试，RED 原因是生产代码还没有调用 requirePermission，verify 失败。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-BEAN-001, MC-VALID-001, MC-LOG-001, MC-SCOPE-001, MC-TEST-001
- Literal Rules: Rule 2, Rule 4, Rule 11
- Ordered files:

#### File 1 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/application/goods/BrandManageImplTest.java`

- Purpose: 先证明 Brand 拒绝发生在领域调用之前。
- Symbols: BrandManageImplTest.denialDoesNotCallDomain, allowValidatesThenCallsDomain
- Repository evidence: 现有 BrandManageImplTest 直接构造或注入 Manage，并 mock 领域服务。File 测试目前用 MethodValidationInterceptor 覆盖空文件。
- Dependencies and consumers: 只被本步生产改动满足。
- Why now: RED 要在生产调用出现之前写好。
- Contract/signature changes: 新增授权 mock。品牌写方法在授权放行后仍断言 610105。
- Input/output and state mapping: 拒绝映射为对应 GoodsErrorStatus；允许后的成功体仍由原测试锁定。
- Error and edge behavior: 空上传在授权放行后仍抛 ConstraintViolationException，且对象存储 mock 无交互。授权拒绝时即使命令为空也不调用领域。
- Standards impact: MC-TEST-001, MC-SCOPE-001
- Literal rule enforcement: Rule 11 测试放在与生产类相同的包，不新建测试层。
- Implementation pseudocode:

```java
@Mock BrandAuthorizationComponent authorization;
@Mock ValidationUtils validation;
@Test void denialDoesNotCallDomain() {
  doThrow(new CommonException(GoodsErrorStatus.BRAND_FORBIDDEN)).when(authorization)
      .requirePermission("enjoyshop:brand:read");
  assertThatThrownBy(() -> manage.listBrands(sample()))
      .isInstanceOf(CommonException.class);
  verifyNoInteractions(domainService);
}
@Test void allowValidatesThenCallsDomain() {
  manage.listBrands(validSample());
  InOrder order = inOrder(authorization, validation, domainService);
  order.verify(authorization).requirePermission("enjoyshop:brand:read");
  order.verify(validation).validate(any());
  order.verify(domainService);
}

@Test void writeWithoutOperatorRemains610105AfterPermissionAllows() {
  MDC.remove("userId");
  assertThatThrownBy(() -> manage.create(createCommand()))
      .isInstanceOfSatisfying(CommonException.class, error ->
          assertThat(error.getCode()).isEqualTo(610105));
  verifyNoInteractions(domainService);
}
```

- Verification contribution: 本步命令在生产改动前失败，改动后通过。
- After this file: 测试编译还依赖下一步的构造器参数，因此本文件先提交到工作区，同一步内立刻改生产类。
#### File 2 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/application/goods/SpecManageImplTest.java`

- Purpose: 先证明 Spec 拒绝发生在领域调用之前。
- Symbols: SpecManageImplTest.denialDoesNotCallDomain, allowValidatesThenCallsDomain
- Repository evidence: 现有 SpecManageImplTest 直接构造或注入 Manage，并 mock 领域服务。File 测试目前用 MethodValidationInterceptor 覆盖空文件。
- Dependencies and consumers: 只被本步生产改动满足。
- Why now: RED 要在生产调用出现之前写好。
- Contract/signature changes: 新增授权 mock。品牌写方法在授权放行后仍断言 610105。
- Input/output and state mapping: 拒绝映射为对应 GoodsErrorStatus；允许后的成功体仍由原测试锁定。
- Error and edge behavior: 空上传在授权放行后仍抛 ConstraintViolationException，且对象存储 mock 无交互。授权拒绝时即使命令为空也不调用领域。
- Standards impact: MC-TEST-001, MC-SCOPE-001
- Literal rule enforcement: Rule 11 测试放在与生产类相同的包，不新建测试层。
- Implementation pseudocode:

```java
@Mock SpecAuthorizationComponent authorization;
@Mock ValidationUtils validation;
@Test void denialDoesNotCallDomain() {
  doThrow(new CommonException(GoodsErrorStatus.SPEC_FORBIDDEN)).when(authorization)
      .requirePermission("enjoyshop:spec:read");
  assertThatThrownBy(() -> manage.listSpecsByCategoryName(sample()))
      .isInstanceOf(CommonException.class);
  verifyNoInteractions(domainService);
}
@Test void allowValidatesThenCallsDomain() {
  manage.listSpecsByCategoryName(validSample());
  InOrder order = inOrder(authorization, validation, domainService);
  order.verify(authorization).requirePermission("enjoyshop:spec:read");
  order.verify(validation).validate(any());
  order.verify(domainService);
}
```

- Verification contribution: 本步命令在生产改动前失败，改动后通过。
- After this file: 测试编译还依赖下一步的构造器参数，因此本文件先提交到工作区，同一步内立刻改生产类。
#### File 3 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/application/goods/FileManageImplTest.java`

- Purpose: 先证明 File 拒绝发生在领域调用之前。
- Symbols: FileManageImplTest.denialDoesNotCallDomain, allowValidatesThenCallsDomain
- Repository evidence: 现有 FileManageImplTest 直接构造或注入 Manage，并 mock 领域服务。File 测试目前用 MethodValidationInterceptor 覆盖空文件。
- Dependencies and consumers: 只被本步生产改动满足。
- Why now: RED 要在生产调用出现之前写好。
- Contract/signature changes: 新增授权 mock。品牌写方法在授权放行后仍断言 610105。
- Input/output and state mapping: 拒绝映射为对应 GoodsErrorStatus；允许后的成功体仍由原测试锁定。
- Error and edge behavior: 空上传在授权放行后仍抛 ConstraintViolationException，且对象存储 mock 无交互。授权拒绝时即使命令为空也不调用领域。
- Standards impact: MC-TEST-001, MC-SCOPE-001
- Literal rule enforcement: Rule 11 测试放在与生产类相同的包，不新建测试层。
- Implementation pseudocode:

```java
@Mock FileAuthorizationComponent authorization;
@Mock ValidationUtils validation;
@Test void denialDoesNotCallDomain() {
  doThrow(new CommonException(GoodsErrorStatus.FILE_FORBIDDEN)).when(authorization)
      .requirePermission("enjoyshop:file:write");
  assertThatThrownBy(() -> manage.upload(sample()))
      .isInstanceOf(CommonException.class);
  verifyNoInteractions(fileDomainService);
}
@Test void allowValidatesThenCallsDomain() {
  manage.upload(validSample());
  InOrder order = inOrder(authorization, validation, fileDomainService);
  order.verify(authorization).requirePermission("enjoyshop:file:write");
  order.verify(validation).validate(any());
  order.verify(fileDomainService);
}
```

- Verification contribution: 本步命令在生产改动前失败，改动后通过。
- After this file: 测试编译还依赖下一步的构造器参数，因此本文件先提交到工作区，同一步内立刻改生产类。
#### File 4 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/BrandManage.java`

- Purpose: 去掉会在方法体之前执行的参数 @Valid，使权限能够先运行。
- Symbols: create, update, delete, detail, page, listBrands
- Repository evidence: BrandManage 目前是 @Validated，方法参数带 @Valid。
- Dependencies and consumers: BrandController 调用该接口。控制器自己的 @Valid 不在这个文件。
- Why now: 实现类的第一行权限调用必须先于校验。
- Contract/signature changes: 删除方法参数上的 @Valid 与只服务于参数校验的 @NotNull。类上的 @Validated 保留。
- Input/output and state mapping: 方法签名和返回类型不变。
- Error and edge behavior: null 命令改由实现里的 ValidationUtils 拒绝，不再依赖方法参数约束。
- Standards impact: MC-VALID-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 2 层间校验改到实现里的 ValidationUtils，分组保持默认组，不新增分组。
- Implementation pseudocode:

```java
public interface BrandManage {
  // 删除 create, update, delete, detail, page, listBrands 参数上的 @Valid。
  // 控制器 DTO 的 @Valid 不动。
}
```

- Verification contribution: Manage 测试的顺序断言。
- After this file: 接口不再在进入方法前触发方法校验。
#### File 5 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/SpecManage.java`

- Purpose: 去掉会在方法体之前执行的参数 @Valid，使权限能够先运行。
- Symbols: listSpecsByCategoryName
- Repository evidence: SpecManage 目前是 @Validated，方法参数带 @Valid。
- Dependencies and consumers: SpecController 调用该接口。控制器自己的 @Valid 不在这个文件。
- Why now: 实现类的第一行权限调用必须先于校验。
- Contract/signature changes: 删除方法参数上的 @Valid 与只服务于参数校验的 @NotNull。类上的 @Validated 保留。
- Input/output and state mapping: 方法签名和返回类型不变。
- Error and edge behavior: null 命令改由实现里的 ValidationUtils 拒绝，不再依赖方法参数约束。
- Standards impact: MC-VALID-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 2 层间校验改到实现里的 ValidationUtils，分组保持默认组，不新增分组。
- Implementation pseudocode:

```java
@Validated
public interface SpecManage {
  List<SpecResult> listSpecsByCategoryName(SpecListQuery query);
  // 参数不再带 @Valid。SpecController 仍校验路径上的分类名。
  // 实现先 requirePermission("enjoyshop:spec:read")，再 validation.validate(query)。
}
```

- Verification contribution: Manage 测试的顺序断言。
- After this file: 接口不再在进入方法前触发方法校验。
#### File 6 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/FileManage.java`

- Purpose: 去掉会在方法体之前执行的参数 @Valid，使权限能够先运行。
- Symbols: upload
- Repository evidence: FileManage 目前是 @Validated，方法参数带 @Valid。
- Dependencies and consumers: FileController 调用该接口。控制器自己的 @Valid 不在这个文件。
- Why now: 实现类的第一行权限调用必须先于校验。
- Contract/signature changes: 删除方法参数上的 @Valid 与只服务于参数校验的 @NotNull。类上的 @Validated 保留。
- Input/output and state mapping: 方法签名和返回类型不变。
- Error and edge behavior: null 命令改由实现里的 ValidationUtils 拒绝，不再依赖方法参数约束。
- Standards impact: MC-VALID-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 2 层间校验改到实现里的 ValidationUtils，分组保持默认组，不新增分组。
- Implementation pseudocode:

```java
@Validated
public interface FileManage {
  String upload(FileUploadCommand command);
  // 删除参数 @Valid 与 @NotNull，避免方法拦截器先于权限执行。
  // FileController 的 multipart @Valid 保留。空内容由 ValidationUtils 拒绝。
}
```

- Verification contribution: Manage 测试的顺序断言。
- After this file: 接口不再在进入方法前触发方法校验。
#### File 7 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/BrandManageImpl.java`

- Purpose: 品牌读写在校验、重名检查和领域调用之前要求权限。
- Symbols: brandManage, requireOperator
- Repository evidence: 类名 brandManage。写方法已有 requireOperator，读方法目前没有。userId 读取 MDC。
- Dependencies and consumers: BrandController 通过 Spring 代理调用。BrandManageImplTest 构造它。
- Why now: 接口已经不再提前校验。
- Contract/signature changes: 构造器增加 brandAuthorizationComponent 与 egonColaValidationUtils。五个业务方法都先要权限；page 使用 read。
- Input/output and state mapping: create、update、delete 使用 write。detail、page、listBrands 使用 read。requireOperator 仍只看 MDC userId。
- Error and edge behavior: 授权异常直接抛出，不转换成 610105。MDC 空白只在 write 权限已经允许后才变成 610105。
- Standards impact: MC-BEAN-001, MC-VALID-001, MC-LOG-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 2 先权限再 validation.validate；Rule 4 两个新字段都有 @Qualifier；Rule 11 仍在 application.goods。
- Implementation pseudocode:

```java
@Qualifier("brandAuthorizationComponent")
private final BrandAuthorizationComponent authorization;
@Qualifier("egonColaValidationUtils")
private final ValidationUtils validation;
public PageResultRecord<BrandResult> listBrands(BrandPageQuery query, Integer seqMin, Integer seqMax) {
  authorization.requirePermission("enjoyshop:brand:read");
  validation.validate(query);
  // 原 listPage 与 countPage
}
public BrandResult detail(BrandDetailQuery query) {
  authorization.requirePermission("enjoyshop:brand:read");
  validation.validate(query);
  return resultConverter.toTarget(domainService.detail(query.getId()));
}
public BrandResult create(CreateBrandCommand command) {
  authorization.requirePermission("enjoyshop:brand:write");
  validation.validate(command);
  requireOperator();
  requireNameAvailable(command.getName(), null);
  return resultConverter.toTarget(domainService.create(createConverter.toTarget(command)));
}

public BrandResult update(UpdateBrandCommand command) {
  authorization.requirePermission("enjoyshop:brand:write");
  validation.validate(command);
  requireOperator();
  requireNameAvailable(command.getName(), command.getId());
  return resultConverter.toTarget(domainService.update(updateConverter.toTarget(command)));
}
public void delete(DeleteBrandCommand command) {
  authorization.requirePermission("enjoyshop:brand:write");
  validation.validate(command);
  requireOperator();
  domainService.delete(command.getId(), command.getVersion());
}
public PageSlice<BrandResult> page(BrandPageQuery query) {
  authorization.requirePermission("enjoyshop:brand:read");
  validation.validate(query);
  // 保留原 query 映射
}
```

- Verification contribution: BrandManageImplTest 的顺序和 610105。
- After this file: 未允许的请求不会调用 BrandDomainService。
#### File 8 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/SpecManageImpl.java`

- Purpose: 规格查询先要 read 权限，再校验和查询。
- Symbols: specManage.listSpecsByCategoryName
- Repository evidence: 现有方法直接 manageConverter.toTarget 后调用 specDomainService。
- Dependencies and consumers: SpecController 是调用方。
- Why now: 品牌实现已经示范同一顺序。
- Contract/signature changes: 构造器增加 specAuthorizationComponent 与 egonColaValidationUtils。
- Input/output and state mapping: 权限码固定 enjoyshop:spec:read。分类名不合法仍由原校验产生 610110，但发生在权限允许之后。
- Error and edge behavior: 拒绝时 listByCategoryName 不被调用。
- Standards impact: MC-BEAN-001, MC-VALID-001, MC-LOG-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 2 与 Rule 4 同品牌实现；Rule 11 不新增包。
- Implementation pseudocode:

```java
authorization.requirePermission("enjoyshop:spec:read");
validation.validate(query);
return resultConverter.toTargetList(domainService.listByCategoryName(manageConverter.toTarget(query)));
```

- Verification contribution: SpecManageImplTest 的 verifyNoInteractions。
- After this file: 无权限不查规格。
#### File 9 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/FileManageImpl.java`

- Purpose: 上传先要 file:write，再校验字节并调用领域。
- Symbols: fileManage.upload
- Repository evidence: 现有 upload 先转换再 fileDomainService.upload。空文件测试依赖方法 @Valid。
- Dependencies and consumers: FileController 调用它。对象存储端口在领域实现内部。
- Why now: 规格实现已完成同一顺序。
- Contract/signature changes: 构造器增加 fileAuthorizationComponent 与 egonColaValidationUtils。
- Input/output and state mapping: 成功返回值仍是领域给出的 URL 字符串。日志语句保持不打印文件内容。
- Error and edge behavior: 拒绝时 converter 和 fileDomainService 都没有交互。放行后的空内容仍是 ConstraintViolationException。
- Standards impact: MC-BEAN-001, MC-VALID-001, MC-LOG-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 2 用 ValidationUtils 替代方法参数 @Valid；Rule 4 字段具名注入；Rule 11 保持原类。
- Implementation pseudocode:

```java
public String upload(FileUploadCommand command) {
  authorization.requirePermission("enjoyshop:file:write");
  validation.validate(command);
  ProductImageBO image = converter.toTarget(command);
  String url = fileDomainService.upload(image);
  log.info("product image upload completed");
  return url;
}
```

- Verification contribution: FileManageImplTest 覆盖拒绝和空内容。
- After this file: 无上传权限不会写入对象。

- Validation working directory: /Users/mario/SelfProject/enjoyshop
- Verification command: `mvn -pl enjoyshop-service/enjoyshop-service-goods -am -Dtest=BrandManageImplTest,SpecManageImplTest,FileManageImplTest,BrandAuthorizationComponentTest,SpecAuthorizationComponentTest,FileAuthorizationComponentTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: 测试先改、生产未改时失败；本步结束退出码 0。
- Failure returns to: 三份 Manage 测试里的 denialDoesNotCallDomain
- Completion criteria: 七个操作的拒绝路径都不调用领域；品牌写操作在权限允许后仍要求 MDC userId。
- Rollback: 恢复三个接口的 @Valid，并移除三个实现里的授权字段。
- Commit paths: enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/application/goods/BrandManageImplTest.java, enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/application/goods/SpecManageImplTest.java, enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/application/goods/FileManageImplTest.java, enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/BrandManage.java, enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/SpecManage.java, enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/FileManage.java, enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/BrandManageImpl.java, enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/SpecManageImpl.java, enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/FileManageImpl.java
- Commit: `feat(goods): check catalog permissions before domain calls`

### Step 3 — 安装品牌链、规格链，并给上传链补上鉴身

- Requirements: REQ-001, REQ-003, REQ-004, REQ-007, REQ-008
- Dependencies: Step 2 已经把权限检查放进 Manage。过滤链负责在那之前完成验签和快照。
- Baseline state: 品牌和规格走默认 permitAll。上传链只有守兵过滤器，边界测试断言它不含 Rbac3。TenantContextFilter 还不跳过这三条路径。
- Observable outcome: 品牌链和规格链的过滤器顺序与商品链相同；上传链同时安装两个过滤器；register-filter 为 true 时三条链都在使用 HttpSecurity 之前失败；请求头不能写入这三条路径的 MDC。
- End state: 匿名请求在进入控制器之前是 401。商品、广告、公开搜索和公开广告位的匹配器不变。
- Test-first gate: Required。先改边界测试。RED 原因是源码还没有 brandSecurityFilterChain，上传方法体仍被断言为不含 Rbac3，而新断言要求含有它。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-CONFIG-001, MC-SCOPE-001, MC-TEST-001
- Literal Rules: Rule 4, Rule 7, Rule 9, Rule 11
- Ordered files:

#### File 1 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/start/config/PlatformAuthenticationBoundaryContractTest.java`

- Purpose: 把新的链顺序、上传链鉴身过滤器和样例控制器名单写成会失败的合同。
- Symbols: filterChainsMatchTheSpecRoutes, registerFilterTrueRejectsProtectedChains
- Repository evidence: 测试目前要求上传链不含 Rbac3BearerAuthenticationFilter，控制器名单含五个样例控制器。
- Dependencies and consumers: 只读 NativeHttpSecurityConfiguration 源码，不启动守兵。
- Why now: 生产链还是旧的，所以新断言先红。
- Contract/signature changes: fileUploadSecurityFilterChain 的调用改为七个参数，以便同时传入两个注册开关。
- Input/output and state mapping: 品牌匹配 /api/v1/brands 与 /**，顺序是 LOWEST_PRECEDENCE-6。规格是 -7。上传异常文本仍含 file-upload SecurityFilterChain。
- Error and edge behavior: 商品、广告、公开广告和搜索的原断言保留。控制器名单删除 UserController、RoleController、PermissionController、CourseController、SchoolClassController。样例控制器要到 Step 5 才从磁盘消失，因此本步只锁定过滤链，控制器名单的删除断言放到 Step 5。
- Standards impact: MC-TEST-001, MC-SCOPE-001
- Literal rule enforcement: Rule 11 测试放在与生产类相同的包，不新建测试层。
- Implementation pseudocode:

```java
String brands = methodBody(security, "brandSecurityFilterChain");
assertThat(brands).contains("securityMatcher(\"/api/v1/brands\", \"/api/v1/brands/**\")");
assertThat(filterOrder(brands)).isEqualTo("identity-before-rbac-before-anonymous");
assertThat(security).contains("@Order(Ordered.LOWEST_PRECEDENCE - 6)");
String specs = methodBody(security, "specSecurityFilterChain");
assertThat(specs).contains("securityMatcher(\"/api/v1/specs\", \"/api/v1/specs/**\")");
assertThat(security).contains("@Order(Ordered.LOWEST_PRECEDENCE - 7)");
String upload = methodBody(security, "fileUploadSecurityFilterChain");
assertThat(upload).contains("Rbac3BearerAuthenticationFilter");
assertThat(upload).contains("IdpBearerAuthenticationFilter");
assertThat(upload.indexOf("if (idpRegistration || rbacRegistration)")).isLessThan(upload.indexOf("http."));
assertThatThrownBy(() -> config.fileUploadSecurityFilterChain(null, null, null, null, null, true, false))
    .isInstanceOf(IllegalStateException.class)
    .hasMessageContaining("file-upload SecurityFilterChain");
String tenant = Files.readString(Path.of("src/main/java/top/egon/enjoyshop/goods/adapter/filter/TenantContextFilter.java"));
assertThat(tenant).contains("/api/v1/brands", "/api/v1/specs", "/api/v1/files");
```

- Verification contribution: 本步验证命令的 RED 与 GREEN。
- After this file: 测试期望新链，生产源码尚未包含它们。
#### File 2 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/start/config/NativeHttpSecurityConfiguration.java`

- Purpose: 增加两条链，并让上传链同时安装守兵和鉴身。
- Symbols: brandSecurityFilterChain, specSecurityFilterChain, fileUploadSecurityFilterChain
- Repository evidence: productSecurityFilterChain 在使用 http 之前检查两个 register-filter。FILE_UPLOAD_PATH 已在方法外。
- Dependencies and consumers: 边界测试读源码。运行时 Spring Security 选择链。
- Why now: 测试已经要求这些方法体。
- Contract/signature changes: 上传方法改为接收两个过滤器实例和两个 boolean，不再用 ObjectProvider 跳过缺失的守兵过滤器。
- Input/output and state mapping: 品牌和规格 anyRequest().authenticated()。上传仍只匹配 POST /api/v1/files。401 响应体沿用 configureCommonSecurity。
- Error and edge behavior: 任一 register-filter 为 true 时抛 IllegalStateException，且不调用 http.securityMatcher。公开链和默认链不改。
- Standards impact: MC-REUSE-001, MC-CONFIG-001, MC-SCOPE-001, MC-TEST-001, MC-ARCH-001
- Literal rule enforcement: Rule 4 过滤器参数使用已有 @Qualifier；Rule 7 不新增 YAML 键，默认 register-filter 仍是 false；Rule 9 不新增认证策略类；Rule 11 仍在 start.config。
- Implementation pseudocode:

```java
@Bean("brandSecurityFilterChain")
@Order(Ordered.LOWEST_PRECEDENCE - 6)
SecurityFilterChain brandSecurityFilterChain(HttpSecurity http, CorsConfigurationSource cors, ObjectMapper mapper,
    IdpBearerAuthenticationFilter identity, Rbac3BearerAuthenticationFilter permissions,
    @Value("${egon.cola.platform.tianquan.shoubing.register-filter:false}") boolean idpRegistration,
    @Value("${egon.cola.platform.tianquan.jianshen.register-filter:false}") boolean rbacRegistration) throws Exception {
  if (idpRegistration || rbacRegistration) {
    throw new IllegalStateException("Brand authentication filters must only run inside the brand SecurityFilterChain");
  }
  http.securityMatcher("/api/v1/brands", "/api/v1/brands/**");
  configureCommonSecurity(http, cors, mapper);
  http.addFilterBefore(permissions, AnonymousAuthenticationFilter.class)
      .addFilterBefore(identity, Rbac3BearerAuthenticationFilter.class);
  return http.authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated()).build();
}
// specSecurityFilterChain 使用 LOWEST_PRECEDENCE - 7 和 /api/v1/specs。
// fileUploadSecurityFilterChain 保留 FILE_UPLOAD_PATH 与 POST，
// 在 http 使用前用同一 if 抛出含 "file-upload SecurityFilterChain" 的 IllegalStateException，
// 然后按商品链安装 permissions 与 identity。
```

- Verification contribution: 边界测试的链顺序和异常文本。
- After this file: 三条业务链都会验签并装入鉴身快照。默认链仍 permitAll，但这三条路径不再落到它。
#### File 3 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/filter/TenantContextFilter.java`

- Purpose: 品牌、规格和上传不再接受租户或操作人请求头。
- Symbols: shouldNotFilter
- Repository evidence: 方法已跳过 product-search、products、ads、ad-slots。缺省租户是 1。
- Dependencies and consumers: 边界测试读取源码。品牌写方法读取的 userId 必须来自平台过滤器写下的 MDC。
- Why now: 链已经会写入身份 MDC，请求头过滤器必须先让路。
- Contract/signature changes: 只增加三个路径及其斜杠前缀。
- Input/output and state mapping: 跳过时不写 tenantId 和 userId，也不使用默认租户 1。
- Error and edge behavior: 其他仍由该过滤器处理的旧路径保持原样。公开搜索继续用自己的站点租户逻辑。
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-ARCH-001
- Literal rule enforcement: Rule 11 只改现有过滤器，不新增身份来源。
- Implementation pseudocode:

```java
return path.equals("/api/v1/product-search") || path.startsWith("/api/v1/product-search/")
    || path.equals("/api/v1/products") || path.startsWith("/api/v1/products/")
    || path.equals("/api/v1/ads") || path.startsWith("/api/v1/ads/")
    || path.equals("/api/v1/ad-slots") || path.startsWith("/api/v1/ad-slots/")
    || path.equals("/api/v1/brands") || path.startsWith("/api/v1/brands/")
    || path.equals("/api/v1/specs") || path.startsWith("/api/v1/specs/")
    || path.equals("/api/v1/files") || path.startsWith("/api/v1/files/");
```

- Verification contribution: 边界测试断言三个路径字面量。
- After this file: X-Tenant-Id 与 X-Actor-Id 不能再为这三条路径建立操作人。

- Validation working directory: /Users/mario/SelfProject/enjoyshop
- Verification command: `mvn -pl enjoyshop-service/enjoyshop-service-goods -am -Dtest=PlatformAuthenticationBoundaryContractTest,ProductAuthorizationComponentTest,AdManageTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: 边界测试先红后绿；商品和广告授权测试仍通过。退出码 0。
- Failure returns to: PlatformAuthenticationBoundaryContractTest.filterChainsMatchTheSpecRoutes
- Completion criteria: 新链顺序、上传链两个过滤器、register-filter 失败和租户跳过都被源码合同锁住。商品与广告断言没有改弱。
- Rollback: 恢复上传链的旧签名，删除品牌链和规格链，并恢复 TenantContextFilter 的原跳过名单。
- Commit paths: enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/start/config/PlatformAuthenticationBoundaryContractTest.java, enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/start/config/NativeHttpSecurityConfiguration.java, enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/filter/TenantContextFilter.java
- Commit: `feat(goods): authenticate brand spec and upload chains`

### Step 4 — 三个控制器改用 bearerAuth 文档

- Requirements: REQ-001, REQ-002, REQ-003, REQ-004
- Dependencies: Step 3 的运行时认证已经存在。本步只改 springdoc 注解和合同测试。
- Baseline state: 三个控制器已有 @Operation 和 operationId。FileController 使用本地方案 shoubingBearer。OpenApiConfig 与 ProductOpenApiConfiguration 已声明 bearerAuth。
- Observable outcome: 七个 operationId 不变，OAS 安全要求是 bearerAuth，并写明 401、403、503。
- End state: 上传合同不再要求 shoubingBearer。商品和广告的 OpenAPI 文件不改。
- Test-first gate: Required。三份合同测试先断言 bearerAuth 与 401/403/503，RED 原因是上传操作仍指向 shoubingBearer，品牌和规格操作还没有 security 要求。
- Manual Checks: MC-SCOPE-001, MC-TEST-001, MC-JSON-001
- Literal Rules: Rule 6, Rule 11
- Ordered files:

#### File 1 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/goods/BrandOpenApiContractTest.java`

- Purpose: 先要求 BrandController 的文档使用 bearerAuth。
- Symbols: BrandOpenApiContractTest
- Repository evidence: 测试已锁定 operationId。上传测试第 59 行断言 shoubingBearer。
- Dependencies and consumers: 读取 springdoc 生成的 JSON，不访问外网。
- Why now: 控制器注解尚未改。
- Contract/signature changes: 不断言新的 operationId，只追加 security 和响应码。
- Input/output and state mapping: security 数组含 bearerAuth。responses 含 401、403、503。
- Error and edge behavior: 上传测试删除对 shoubingBearer 的存在性断言，避免两套方案名。
- Standards impact: MC-TEST-001, MC-SCOPE-001
- Literal rule enforcement: Rule 6 文档描述现有 JSON 错误，不新增编码；Rule 11 测试留在 adapter.goods。
- Implementation pseudocode:

```java
for (String operationId : List.of("listBrands", "getBrand", "createBrand", "updateBrand", "deleteBrand")) {
  JsonNode operation = findByOperationId(document, operationId);
  assertThat(operation.path("security").get(0).has("bearerAuth")).isTrue();
  assertThat(operation.path("responses").has("401")).isTrue();
  assertThat(operation.path("responses").has("403")).isTrue();
  assertThat(operation.path("responses").has("503")).isTrue();
}
assertThat(document.at("/components/securitySchemes/bearerAuth/scheme").asText()).isEqualTo("bearer");
```

- Verification contribution: 本步命令在注解改动前失败。
- After this file: 合同失败指向缺少 bearerAuth。
#### File 2 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/goods/SpecOpenApiContractTest.java`

- Purpose: 先要求 SpecController 的文档使用 bearerAuth。
- Symbols: SpecOpenApiContractTest
- Repository evidence: 测试已锁定 operationId。上传测试第 59 行断言 shoubingBearer。
- Dependencies and consumers: 读取 springdoc 生成的 JSON，不访问外网。
- Why now: 控制器注解尚未改。
- Contract/signature changes: 不断言新的 operationId，只追加 security 和响应码。
- Input/output and state mapping: security 数组含 bearerAuth。responses 含 401、403、503。
- Error and edge behavior: 上传测试删除对 shoubingBearer 的存在性断言，避免两套方案名。
- Standards impact: MC-TEST-001, MC-SCOPE-001
- Literal rule enforcement: Rule 6 文档描述现有 JSON 错误，不新增编码；Rule 11 测试留在 adapter.goods。
- Implementation pseudocode:

```java
for (String operationId : List.of("listSpecsByCategoryName")) {
  JsonNode operation = findByOperationId(document, operationId);
  assertThat(operation.path("security").get(0).has("bearerAuth")).isTrue();
  assertThat(operation.path("responses").has("401")).isTrue();
  assertThat(operation.path("responses").has("403")).isTrue();
  assertThat(operation.path("responses").has("503")).isTrue();
}
assertThat(document.at("/components/securitySchemes/bearerAuth/scheme").asText()).isEqualTo("bearer");
```

- Verification contribution: 本步命令在注解改动前失败。
- After this file: 合同失败指向缺少 bearerAuth。
#### File 3 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/goods/FileOpenApiContractTest.java`

- Purpose: 先要求 FileController 的文档使用 bearerAuth。
- Symbols: FileOpenApiContractTest
- Repository evidence: 测试已锁定 operationId。上传测试第 59 行断言 shoubingBearer。
- Dependencies and consumers: 读取 springdoc 生成的 JSON，不访问外网。
- Why now: 控制器注解尚未改。
- Contract/signature changes: 不断言新的 operationId，只追加 security 和响应码。
- Input/output and state mapping: security 数组含 bearerAuth。responses 含 401、403、503。
- Error and edge behavior: 上传测试删除对 shoubingBearer 的存在性断言，避免两套方案名。
- Standards impact: MC-TEST-001, MC-SCOPE-001
- Literal rule enforcement: Rule 6 文档描述现有 JSON 错误，不新增编码；Rule 11 测试留在 adapter.goods。
- Implementation pseudocode:

```java
for (String operationId : List.of("uploadFile")) {
  JsonNode operation = findByOperationId(document, operationId);
  assertThat(operation.path("security").get(0).has("bearerAuth")).isTrue();
  assertThat(operation.path("responses").has("401")).isTrue();
  assertThat(operation.path("responses").has("403")).isTrue();
  assertThat(operation.path("responses").has("503")).isTrue();
}
assertThat(document.at("/components/securitySchemes/bearerAuth/scheme").asText()).isEqualTo("bearer");
```

- Verification contribution: 本步命令在注解改动前失败。
- After this file: 合同失败指向缺少 bearerAuth。
#### File 4 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/goods/BrandController.java`

- Purpose: 给 BrandController 补上规格要求的安全文档。
- Symbols: listBrands, getBrand, createBrand, updateBrand, deleteBrand
- Repository evidence: 方法已有 @Operation。全局 bearerAuth 已在 OpenApiConfig。
- Dependencies and consumers: springdoc 与对应合同测试。运行时认证不读这些注解。
- Why now: 测试已经要求这些字段。
- Contract/signature changes: 五个方法都加 @SecurityRequirement(name = \"bearerAuth\") 和 401、403、503 的 @ApiResponse。
- Input/output and state mapping: operationId、URL、成功 schema 不变。
- Error and edge behavior: 不在注解示例里放真实令牌。
- Standards impact: MC-SCOPE-001, MC-JSON-001, MC-TEST-001
- Literal rule enforcement: Rule 6 不改变错误 JSON 的 Jackson 形状；Rule 11 不移动控制器。
- Implementation pseudocode:

```java
@Operation(operationId = "listBrands", summary = "保持原摘要",
    security = @SecurityRequirement(name = "bearerAuth"),
    responses = {
      @ApiResponse(responseCode = "401", description = "Tianquan-Shoubing authentication failed"),
      @ApiResponse(responseCode = "403", description = "permission denied"),
      @ApiResponse(responseCode = "503", description = "authorization unavailable")
    })
```

- Verification contribution: 对应 OpenAPI 合同变绿。
- After this file: BrandController 的 OAS 与运行时 Bearer 使用同一个方案名。
#### File 5 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/goods/SpecController.java`

- Purpose: 给 SpecController 补上规格要求的安全文档。
- Symbols: listSpecsByCategoryName
- Repository evidence: 方法已有 @Operation。全局 bearerAuth 已在 OpenApiConfig。
- Dependencies and consumers: springdoc 与对应合同测试。运行时认证不读这些注解。
- Why now: 测试已经要求这些字段。
- Contract/signature changes: 只改 listSpecsByCategoryName 的文档注解。
- Input/output and state mapping: operationId、URL、成功 schema 不变。
- Error and edge behavior: 不在注解示例里放真实令牌。
- Standards impact: MC-SCOPE-001, MC-JSON-001, MC-TEST-001
- Literal rule enforcement: Rule 6 不改变错误 JSON 的 Jackson 形状；Rule 11 不移动控制器。
- Implementation pseudocode:

```java
@Operation(operationId = "listSpecsByCategoryName", summary = "保持原摘要",
    security = @SecurityRequirement(name = "bearerAuth"),
    responses = {
      @ApiResponse(responseCode = "401", description = "Tianquan-Shoubing authentication failed"),
      @ApiResponse(responseCode = "403", description = "permission denied"),
      @ApiResponse(responseCode = "503", description = "authorization unavailable")
    })
```

- Verification contribution: 对应 OpenAPI 合同变绿。
- After this file: SpecController 的 OAS 与运行时 Bearer 使用同一个方案名。
#### File 6 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/goods/FileController.java`

- Purpose: 给 FileController 补上规格要求的安全文档。
- Symbols: uploadFile
- Repository evidence: 方法已有 @Operation。全局 bearerAuth 已在 OpenApiConfig。
- Dependencies and consumers: springdoc 与对应合同测试。运行时认证不读这些注解。
- Why now: 测试已经要求这些字段。
- Contract/signature changes: 删除类上的 @SecurityScheme(name = \"shoubingBearer\")，方法改为 bearerAuth。
- Input/output and state mapping: operationId、URL、成功 schema 不变。
- Error and edge behavior: 不在注解示例里放真实令牌。
- Standards impact: MC-SCOPE-001, MC-JSON-001, MC-TEST-001
- Literal rule enforcement: Rule 6 不改变错误 JSON 的 Jackson 形状；Rule 11 不移动控制器。
- Implementation pseudocode:

```java
@Operation(operationId = "uploadFile", summary = "保持原摘要",
    security = @SecurityRequirement(name = "bearerAuth"),
    responses = {
      @ApiResponse(responseCode = "401", description = "Tianquan-Shoubing authentication failed"),
      @ApiResponse(responseCode = "403", description = "permission denied"),
      @ApiResponse(responseCode = "503", description = "authorization unavailable")
    })
```

- Verification contribution: 对应 OpenAPI 合同变绿。
- After this file: FileController 的 OAS 与运行时 Bearer 使用同一个方案名。

- Validation working directory: /Users/mario/SelfProject/enjoyshop
- Verification command: `mvn -pl enjoyshop-service/enjoyshop-service-goods -am -Dtest=BrandOpenApiContractTest,SpecOpenApiContractTest,FileOpenApiContractTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: 三份合同先红后绿，退出码 0。商品和广告 OpenAPI 测试不在本步改动范围内，留到最终回归。
- Failure returns to: FileOpenApiContractTest 对 shoubingBearer 的旧断言已被新断言替换；若 bearerAuth 缺失则回到 FileController
- Completion criteria: 七个 operationId 都要求 bearerAuth，上传方案名不再是 shoubingBearer。
- Rollback: 恢复三个控制器的原注解和三份合同里的原安全断言。
- Commit paths: enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/goods/BrandOpenApiContractTest.java, enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/goods/SpecOpenApiContractTest.java, enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/goods/FileOpenApiContractTest.java, enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/goods/BrandController.java, enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/goods/SpecController.java, enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/goods/FileController.java
- Commit: `docs(goods): publish bearer auth on catalog operations`

### Step 5 — 删除样例纵向并改写架构合同

- Requirements: REQ-005
- Dependencies: Step 4 已完成，品牌、规格和上传授权不再依赖样例包。
- Baseline state: 用户和教学包、样例 MQ、样例 RPC 和架构测试的正向断言都还在。CDC 使用另一套 Rabbit 配置。
- Observable outcome: 样例 HTTP、GraphQL 和 RPC 不再注册；架构测试不再要求样例类型存在；品牌、商品、广告、搜索和 CDC 仍在。
- End state: src/main 中不再引用 domain.user、domain.teaching、adapter.user、adapter.teaching 等样例包。InfrastructureLogAspect 里原有的 mq 切点字符串可以留下。
- Test-first gate: Required。先改两份合同测试，RED 原因是样例文件仍然存在而新断言要求它们不存在，且文件计数还不是 6。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-CONFIG-001, MC-SCOPE-001, MC-TEST-001
- Literal Rules: Rule 5, Rule 7, Rule 11
- Ordered files:

#### File 1 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/architecture/ArchetypeContractConvergenceTest.java`

- Purpose: 先把架构合同改成样例类型必须不存在，使当前树变红。
- Symbols: domainOnlyExposesServicesAndKeepsPortsOutOfDomain, mqTopologyAndRoutingComeFromOneEnumInsteadOfTypePrefixGuessing, nativeFacadeHasOneProtobufChainAndFacadeAssertsDelegateToCommonValidation
- Repository evidence: 该测试目前要求 TeachingQueryService、UserQueryService、样例客户端、样例 POJO、CourseRepository、MqRouteEnum 路由键、四个 @EgonRpcService 和 teaching_user_facade.proto。
- Dependencies and consumers: 只读源码。品牌、商品、广告、搜索的负向约束和 Manage 不得引用 DAO 的循环保留。
- Why now: 样例文件还在，所以“必须不存在”会失败。
- Contract/signature changes: 删除要求样例文件存在的断言。DaoMonitorAspect 与 InfrastructureLogAspect 的现有切点文本断言保留。
- Input/output and state mapping: ManageImpl 仍不得包含 RabbitTemplate、StringRedisTemplate、RedisTemplate、Repository、DAO、MqMessageService。
- Error and edge behavior: Cdc 的 teaching 非法表名断言不在这个文件，不要改掉。protobuf 文件被断言为不存在，@EgonRpcService 计数改为 0。
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-ARCH-001
- Literal rule enforcement: Rule 11 删除样例纵向，不新建层，也不把样例类型留在磁盘上满足旧哈希或旧计数。
- Implementation pseudocode:

```text
assertFalse(Files.exists(modulePath("domain/teaching")));
assertFalse(Files.exists(modulePath("domain/user")));
assertFalse(Files.exists(modulePath("infrastructure/mq/MqRouteEnum.java")));
assertFalse(Files.exists(modulePath("infrastructure/config/RabbitMqConfig.java")));
assertFalse(Files.exists(Path.of("src/main/proto/teaching_user_facade.proto")));
assertEquals(0, count(join(sources(MAIN)), "@EgonRpcService"));
for (Path path : sources(MAIN)) {
  if (!path.getFileName().toString().endsWith("ManageImpl.java")) continue;
  String text = read(path);
  for (String forbidden : List.of("RabbitTemplate", "StringRedisTemplate", "RedisTemplate", "Repository", "DAO", "MqMessageService")) {
    assertFalse(text.contains(forbidden), path.toString());
  }
}
```

- Verification contribution: 本步开始时 RED，删除完成后 GREEN。
- After this file: 合同不再保护样例文件。
#### File 2 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/config/NativeReadwriteContractTest.java`

- Purpose: 去掉两个样例文件的 SHA-256，并把基础设施计数改成删除后的 6。
- Symbols: originalSampleRabbitConfigurationRemainsUnchangedAndNoNewBrokerSdkIsRequired, allApprovedFilesPresent
- Repository evidence: 当前哈希锁定 RabbitMqConfig 与 MqRouteEnum。PO、DAO、Repository 期望 14，DomainServiceImpl 期望 10，磁盘上 DomainServiceImpl 实际是 11。
- Dependencies and consumers: CDC 文件清单和 MASTER_DATA_TABLES 里的 users、roles、courses 仍保留，因为本轮没有 DDL。
- Why now: 新计数在文件删除前会失败。
- Contract/signature changes: 删除两个 sha256 断言。保留 POM 不得含 debezium、kafka-clients、elasticsearch、transactional-outbox-starter。SAMPLE_RABBITMQ_STATE 保留。
- Input/output and state mapping: 删除后 PO、DAO、Repository、DomainServiceImpl 都是 6。
- Error and edge behavior: 不改 CdcRabbitConfiguration 的存在性断言，也不从逻辑表名单删除样例表。
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-CONFIG-001
- Literal rule enforcement: Rule 7 不改四份 profile 的 rabbitmq.enabled；Rule 11 计数只统计保留的品牌、商品、规格、文件、广告和搜索。
- Implementation pseudocode:

```text
assertThat(pom).doesNotContain("debezium", "kafka-clients", "elasticsearch", "transactional-outbox-starter");
assertThat(Files.exists(Path.of("src/main/java/top/egon/enjoyshop/goods/infrastructure/config/RabbitMqConfig.java"))).isFalse();
assertThat(Files.exists(Path.of("src/main/java/top/egon/enjoyshop/goods/infrastructure/mq/MqRouteEnum.java"))).isFalse();
assertThat(javaFileCount(infrastructure, "PO.java")).isEqualTo(6);
assertThat(javaFileCount(infrastructure, "DAO.java")).isEqualTo(6);
assertThat(javaFileCount(infrastructure, "Repository.java")).isEqualTo(6);
assertThat(javaFileCount(infrastructure, "DomainServiceImpl.java")).isEqualTo(6);
```

- Verification contribution: 删除完成前计数失败。
- After this file: 哈希不再阻止删除样例 Rabbit 配置。
#### File 3 — `DELETE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/user`

- Purpose: 删除main源码中的样例包 adapter/user。
- Symbols: adapter/user
- Repository evidence: 目录存在，且只被样例控制器、门面或样例消息使用。
- Dependencies and consumers: Step 5 后面的启动类和合同测试不再引用它。
- Why now: 架构合同已经要求它不存在。
- Contract/signature changes: 整个目录删除，不保留 package-info 来满足旧测试。
- Input/output and state mapping: 没有运行时替身。品牌、商品、广告、搜索目录不在这条路径里。
- Error and edge behavior: 若目录里意外出现非样例类型，停止并回到规格，不把该类型改成匿名接口。
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-ARCH-001
- Literal rule enforcement: Rule 11 删除样例纵向，不新建层，也不把样例类型留在磁盘上满足旧哈希或旧计数。
- Implementation pseudocode:

```text
remove_tree enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/user
remain brand, product, ad, search, sync, file
package adapter/user has no remaining Java file
```

- Verification contribution: 删除后的架构测试与编译。
- After this file: enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/user 不存在。
#### File 4 — `DELETE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/teaching`

- Purpose: 删除main源码中的样例包 adapter/teaching。
- Symbols: adapter/teaching
- Repository evidence: 目录存在，且只被样例控制器、门面或样例消息使用。
- Dependencies and consumers: Step 5 后面的启动类和合同测试不再引用它。
- Why now: 架构合同已经要求它不存在。
- Contract/signature changes: 整个目录删除，不保留 package-info 来满足旧测试。
- Input/output and state mapping: 没有运行时替身。品牌、商品、广告、搜索目录不在这条路径里。
- Error and edge behavior: 若目录里意外出现非样例类型，停止并回到规格，不把该类型改成匿名接口。
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-ARCH-001
- Literal rule enforcement: Rule 11 删除样例纵向，不新建层，也不把样例类型留在磁盘上满足旧哈希或旧计数。
- Implementation pseudocode:

```text
remove_tree enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/teaching
remain brand, product, ad, search, sync, file
package adapter/teaching has no remaining Java file
```

- Verification contribution: 删除后的架构测试与编译。
- After this file: enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/teaching 不存在。
#### File 5 — `DELETE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/user`

- Purpose: 删除main源码中的样例包 application/user。
- Symbols: application/user
- Repository evidence: 目录存在，且只被样例控制器、门面或样例消息使用。
- Dependencies and consumers: Step 5 后面的启动类和合同测试不再引用它。
- Why now: 架构合同已经要求它不存在。
- Contract/signature changes: 整个目录删除，不保留 package-info 来满足旧测试。
- Input/output and state mapping: 没有运行时替身。品牌、商品、广告、搜索目录不在这条路径里。
- Error and edge behavior: 若目录里意外出现非样例类型，停止并回到规格，不把该类型改成匿名接口。
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-ARCH-001
- Literal rule enforcement: Rule 11 删除样例纵向，不新建层，也不把样例类型留在磁盘上满足旧哈希或旧计数。
- Implementation pseudocode:

```text
remove_tree enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/user
remain brand, product, ad, search, sync, file
package application/user has no remaining Java file
```

- Verification contribution: 删除后的架构测试与编译。
- After this file: enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/user 不存在。
#### File 6 — `DELETE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/teaching`

- Purpose: 删除main源码中的样例包 application/teaching。
- Symbols: application/teaching
- Repository evidence: 目录存在，且只被样例控制器、门面或样例消息使用。
- Dependencies and consumers: Step 5 后面的启动类和合同测试不再引用它。
- Why now: 架构合同已经要求它不存在。
- Contract/signature changes: 整个目录删除，不保留 package-info 来满足旧测试。
- Input/output and state mapping: 没有运行时替身。品牌、商品、广告、搜索目录不在这条路径里。
- Error and edge behavior: 若目录里意外出现非样例类型，停止并回到规格，不把该类型改成匿名接口。
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-ARCH-001
- Literal rule enforcement: Rule 11 删除样例纵向，不新建层，也不把样例类型留在磁盘上满足旧哈希或旧计数。
- Implementation pseudocode:

```text
remove_tree enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/teaching
remain brand, product, ad, search, sync, file
package application/teaching has no remaining Java file
```

- Verification contribution: 删除后的架构测试与编译。
- After this file: enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/teaching 不存在。
#### File 7 — `DELETE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/user`

- Purpose: 删除main源码中的样例包 domain/user。
- Symbols: domain/user
- Repository evidence: 目录存在，且只被样例控制器、门面或样例消息使用。
- Dependencies and consumers: Step 5 后面的启动类和合同测试不再引用它。
- Why now: 架构合同已经要求它不存在。
- Contract/signature changes: 整个目录删除，不保留 package-info 来满足旧测试。
- Input/output and state mapping: 没有运行时替身。品牌、商品、广告、搜索目录不在这条路径里。
- Error and edge behavior: 若目录里意外出现非样例类型，停止并回到规格，不把该类型改成匿名接口。
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-ARCH-001
- Literal rule enforcement: Rule 11 删除样例纵向，不新建层，也不把样例类型留在磁盘上满足旧哈希或旧计数。
- Implementation pseudocode:

```text
remove_tree enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/user
remain brand, product, ad, search, sync, file
package domain/user has no remaining Java file
```

- Verification contribution: 删除后的架构测试与编译。
- After this file: enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/user 不存在。
#### File 8 — `DELETE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/teaching`

- Purpose: 删除main源码中的样例包 domain/teaching。
- Symbols: domain/teaching
- Repository evidence: 目录存在，且只被样例控制器、门面或样例消息使用。
- Dependencies and consumers: Step 5 后面的启动类和合同测试不再引用它。
- Why now: 架构合同已经要求它不存在。
- Contract/signature changes: 整个目录删除，不保留 package-info 来满足旧测试。
- Input/output and state mapping: 没有运行时替身。品牌、商品、广告、搜索目录不在这条路径里。
- Error and edge behavior: 若目录里意外出现非样例类型，停止并回到规格，不把该类型改成匿名接口。
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-ARCH-001
- Literal rule enforcement: Rule 11 删除样例纵向，不新建层，也不把样例类型留在磁盘上满足旧哈希或旧计数。
- Implementation pseudocode:

```text
remove_tree enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/teaching
remain brand, product, ad, search, sync, file
package domain/teaching has no remaining Java file
```

- Verification contribution: 删除后的架构测试与编译。
- After this file: enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/teaching 不存在。
#### File 9 — `DELETE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/user`

- Purpose: 删除main源码中的样例包 infrastructure/user。
- Symbols: infrastructure/user
- Repository evidence: 目录存在，且只被样例控制器、门面或样例消息使用。
- Dependencies and consumers: Step 5 后面的启动类和合同测试不再引用它。
- Why now: 架构合同已经要求它不存在。
- Contract/signature changes: 整个目录删除，不保留 package-info 来满足旧测试。
- Input/output and state mapping: 没有运行时替身。品牌、商品、广告、搜索目录不在这条路径里。
- Error and edge behavior: 若目录里意外出现非样例类型，停止并回到规格，不把该类型改成匿名接口。
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-ARCH-001
- Literal rule enforcement: Rule 11 删除样例纵向，不新建层，也不把样例类型留在磁盘上满足旧哈希或旧计数。
- Implementation pseudocode:

```text
remove_tree enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/user
remain brand, product, ad, search, sync, file
package infrastructure/user has no remaining Java file
```

- Verification contribution: 删除后的架构测试与编译。
- After this file: enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/user 不存在。
#### File 10 — `DELETE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/teaching`

- Purpose: 删除main源码中的样例包 infrastructure/teaching。
- Symbols: infrastructure/teaching
- Repository evidence: 目录存在，且只被样例控制器、门面或样例消息使用。
- Dependencies and consumers: Step 5 后面的启动类和合同测试不再引用它。
- Why now: 架构合同已经要求它不存在。
- Contract/signature changes: 整个目录删除，不保留 package-info 来满足旧测试。
- Input/output and state mapping: 没有运行时替身。品牌、商品、广告、搜索目录不在这条路径里。
- Error and edge behavior: 若目录里意外出现非样例类型，停止并回到规格，不把该类型改成匿名接口。
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-ARCH-001
- Literal rule enforcement: Rule 11 删除样例纵向，不新建层，也不把样例类型留在磁盘上满足旧哈希或旧计数。
- Implementation pseudocode:

```text
remove_tree enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/teaching
remain brand, product, ad, search, sync, file
package infrastructure/teaching has no remaining Java file
```

- Verification contribution: 删除后的架构测试与编译。
- After this file: enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/teaching 不存在。
#### File 11 — `DELETE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/user`

- Purpose: 删除test源码中的样例包 adapter/user。
- Symbols: adapter/user
- Repository evidence: 目录存在，且只被样例控制器、门面或样例消息使用。
- Dependencies and consumers: Step 5 后面的启动类和合同测试不再引用它。
- Why now: 架构合同已经要求它不存在。
- Contract/signature changes: 整个目录删除，不保留 package-info 来满足旧测试。
- Input/output and state mapping: 没有运行时替身。品牌、商品、广告、搜索目录不在这条路径里。
- Error and edge behavior: 若目录里意外出现非样例类型，停止并回到规格，不把该类型改成匿名接口。
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-ARCH-001
- Literal rule enforcement: Rule 11 删除样例纵向，不新建层，也不把样例类型留在磁盘上满足旧哈希或旧计数。
- Implementation pseudocode:

```text
remove_tree enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/user
remain brand, product, ad, search, sync, file
package adapter/user has no remaining Java file
```

- Verification contribution: 删除后的架构测试与编译。
- After this file: enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/user 不存在。
#### File 12 — `DELETE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/teaching`

- Purpose: 删除test源码中的样例包 adapter/teaching。
- Symbols: adapter/teaching
- Repository evidence: 目录存在，且只被样例控制器、门面或样例消息使用。
- Dependencies and consumers: Step 5 后面的启动类和合同测试不再引用它。
- Why now: 架构合同已经要求它不存在。
- Contract/signature changes: 整个目录删除，不保留 package-info 来满足旧测试。
- Input/output and state mapping: 没有运行时替身。品牌、商品、广告、搜索目录不在这条路径里。
- Error and edge behavior: 若目录里意外出现非样例类型，停止并回到规格，不把该类型改成匿名接口。
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-ARCH-001
- Literal rule enforcement: Rule 11 删除样例纵向，不新建层，也不把样例类型留在磁盘上满足旧哈希或旧计数。
- Implementation pseudocode:

```text
remove_tree enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/teaching
remain brand, product, ad, search, sync, file
package adapter/teaching has no remaining Java file
```

- Verification contribution: 删除后的架构测试与编译。
- After this file: enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/teaching 不存在。
#### File 13 — `DELETE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/application/user`

- Purpose: 删除test源码中的样例包 application/user。
- Symbols: application/user
- Repository evidence: 目录存在，且只被样例控制器、门面或样例消息使用。
- Dependencies and consumers: Step 5 后面的启动类和合同测试不再引用它。
- Why now: 架构合同已经要求它不存在。
- Contract/signature changes: 整个目录删除，不保留 package-info 来满足旧测试。
- Input/output and state mapping: 没有运行时替身。品牌、商品、广告、搜索目录不在这条路径里。
- Error and edge behavior: 若目录里意外出现非样例类型，停止并回到规格，不把该类型改成匿名接口。
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-ARCH-001
- Literal rule enforcement: Rule 11 删除样例纵向，不新建层，也不把样例类型留在磁盘上满足旧哈希或旧计数。
- Implementation pseudocode:

```text
remove_tree enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/application/user
remain brand, product, ad, search, sync, file
package application/user has no remaining Java file
```

- Verification contribution: 删除后的架构测试与编译。
- After this file: enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/application/user 不存在。
#### File 14 — `DELETE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/application/teaching`

- Purpose: 删除test源码中的样例包 application/teaching。
- Symbols: application/teaching
- Repository evidence: 目录存在，且只被样例控制器、门面或样例消息使用。
- Dependencies and consumers: Step 5 后面的启动类和合同测试不再引用它。
- Why now: 架构合同已经要求它不存在。
- Contract/signature changes: 整个目录删除，不保留 package-info 来满足旧测试。
- Input/output and state mapping: 没有运行时替身。品牌、商品、广告、搜索目录不在这条路径里。
- Error and edge behavior: 若目录里意外出现非样例类型，停止并回到规格，不把该类型改成匿名接口。
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-ARCH-001
- Literal rule enforcement: Rule 11 删除样例纵向，不新建层，也不把样例类型留在磁盘上满足旧哈希或旧计数。
- Implementation pseudocode:

```text
remove_tree enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/application/teaching
remain brand, product, ad, search, sync, file
package application/teaching has no remaining Java file
```

- Verification contribution: 删除后的架构测试与编译。
- After this file: enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/application/teaching 不存在。
#### File 15 — `DELETE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/domain/user`

- Purpose: 删除test源码中的样例包 domain/user。
- Symbols: domain/user
- Repository evidence: 目录存在，且只被样例控制器、门面或样例消息使用。
- Dependencies and consumers: Step 5 后面的启动类和合同测试不再引用它。
- Why now: 架构合同已经要求它不存在。
- Contract/signature changes: 整个目录删除，不保留 package-info 来满足旧测试。
- Input/output and state mapping: 没有运行时替身。品牌、商品、广告、搜索目录不在这条路径里。
- Error and edge behavior: 若目录里意外出现非样例类型，停止并回到规格，不把该类型改成匿名接口。
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-ARCH-001
- Literal rule enforcement: Rule 11 删除样例纵向，不新建层，也不把样例类型留在磁盘上满足旧哈希或旧计数。
- Implementation pseudocode:

```text
remove_tree enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/domain/user
remain brand, product, ad, search, sync, file
package domain/user has no remaining Java file
```

- Verification contribution: 删除后的架构测试与编译。
- After this file: enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/domain/user 不存在。
#### File 16 — `DELETE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/domain/teaching`

- Purpose: 删除test源码中的样例包 domain/teaching。
- Symbols: domain/teaching
- Repository evidence: 目录存在，且只被样例控制器、门面或样例消息使用。
- Dependencies and consumers: Step 5 后面的启动类和合同测试不再引用它。
- Why now: 架构合同已经要求它不存在。
- Contract/signature changes: 整个目录删除，不保留 package-info 来满足旧测试。
- Input/output and state mapping: 没有运行时替身。品牌、商品、广告、搜索目录不在这条路径里。
- Error and edge behavior: 若目录里意外出现非样例类型，停止并回到规格，不把该类型改成匿名接口。
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-ARCH-001
- Literal rule enforcement: Rule 11 删除样例纵向，不新建层，也不把样例类型留在磁盘上满足旧哈希或旧计数。
- Implementation pseudocode:

```text
remove_tree enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/domain/teaching
remain brand, product, ad, search, sync, file
package domain/teaching has no remaining Java file
```

- Verification contribution: 删除后的架构测试与编译。
- After this file: enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/domain/teaching 不存在。
#### File 17 — `DELETE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/user`

- Purpose: 删除test源码中的样例包 infrastructure/user。
- Symbols: infrastructure/user
- Repository evidence: 目录存在，且只被样例控制器、门面或样例消息使用。
- Dependencies and consumers: Step 5 后面的启动类和合同测试不再引用它。
- Why now: 架构合同已经要求它不存在。
- Contract/signature changes: 整个目录删除，不保留 package-info 来满足旧测试。
- Input/output and state mapping: 没有运行时替身。品牌、商品、广告、搜索目录不在这条路径里。
- Error and edge behavior: 若目录里意外出现非样例类型，停止并回到规格，不把该类型改成匿名接口。
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-ARCH-001
- Literal rule enforcement: Rule 11 删除样例纵向，不新建层，也不把样例类型留在磁盘上满足旧哈希或旧计数。
- Implementation pseudocode:

```text
remove_tree enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/user
remain brand, product, ad, search, sync, file
package infrastructure/user has no remaining Java file
```

- Verification contribution: 删除后的架构测试与编译。
- After this file: enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/user 不存在。
#### File 18 — `DELETE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/teaching`

- Purpose: 删除test源码中的样例包 infrastructure/teaching。
- Symbols: infrastructure/teaching
- Repository evidence: 目录存在，且只被样例控制器、门面或样例消息使用。
- Dependencies and consumers: Step 5 后面的启动类和合同测试不再引用它。
- Why now: 架构合同已经要求它不存在。
- Contract/signature changes: 整个目录删除，不保留 package-info 来满足旧测试。
- Input/output and state mapping: 没有运行时替身。品牌、商品、广告、搜索目录不在这条路径里。
- Error and edge behavior: 若目录里意外出现非样例类型，停止并回到规格，不把该类型改成匿名接口。
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-ARCH-001
- Literal rule enforcement: Rule 11 删除样例纵向，不新建层，也不把样例类型留在磁盘上满足旧哈希或旧计数。
- Implementation pseudocode:

```text
remove_tree enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/teaching
remain brand, product, ad, search, sync, file
package infrastructure/teaching has no remaining Java file
```

- Verification contribution: 删除后的架构测试与编译。
- After this file: enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/teaching 不存在。
#### File 19 — `DELETE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/facade/user`

- Purpose: 删除main源码中的样例包 facade/user。
- Symbols: facade/user
- Repository evidence: 目录存在，且只被样例控制器、门面或样例消息使用。
- Dependencies and consumers: Step 5 后面的启动类和合同测试不再引用它。
- Why now: 架构合同已经要求它不存在。
- Contract/signature changes: 整个目录删除，不保留 package-info 来满足旧测试。
- Input/output and state mapping: 没有运行时替身。品牌、商品、广告、搜索目录不在这条路径里。
- Error and edge behavior: 若目录里意外出现非样例类型，停止并回到规格，不把该类型改成匿名接口。
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-ARCH-001
- Literal rule enforcement: Rule 11 删除样例纵向，不新建层，也不把样例类型留在磁盘上满足旧哈希或旧计数。
- Implementation pseudocode:

```text
remove_tree enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/facade/user
remain brand, product, ad, search, sync, file
package facade/user has no remaining Java file
```

- Verification contribution: 删除后的架构测试与编译。
- After this file: enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/facade/user 不存在。
#### File 20 — `DELETE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/facade/teaching`

- Purpose: 删除main源码中的样例包 facade/teaching。
- Symbols: facade/teaching
- Repository evidence: 目录存在，且只被样例控制器、门面或样例消息使用。
- Dependencies and consumers: Step 5 后面的启动类和合同测试不再引用它。
- Why now: 架构合同已经要求它不存在。
- Contract/signature changes: 整个目录删除，不保留 package-info 来满足旧测试。
- Input/output and state mapping: 没有运行时替身。品牌、商品、广告、搜索目录不在这条路径里。
- Error and edge behavior: 若目录里意外出现非样例类型，停止并回到规格，不把该类型改成匿名接口。
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-ARCH-001
- Literal rule enforcement: Rule 11 删除样例纵向，不新建层，也不把样例类型留在磁盘上满足旧哈希或旧计数。
- Implementation pseudocode:

```text
remove_tree enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/facade/teaching
remain brand, product, ad, search, sync, file
package facade/teaching has no remaining Java file
```

- Verification contribution: 删除后的架构测试与编译。
- After this file: enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/facade/teaching 不存在。
#### File 21 — `DELETE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/mq`

- Purpose: 删除main源码中的样例包 infrastructure/mq。
- Symbols: infrastructure/mq
- Repository evidence: 目录存在，且只被样例控制器、门面或样例消息使用。
- Dependencies and consumers: Step 5 后面的启动类和合同测试不再引用它。
- Why now: 架构合同已经要求它不存在。
- Contract/signature changes: 整个目录删除，不保留 package-info 来满足旧测试。
- Input/output and state mapping: 没有运行时替身。品牌、商品、广告、搜索目录不在这条路径里。
- Error and edge behavior: 若目录里意外出现非样例类型，停止并回到规格，不把该类型改成匿名接口。
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-ARCH-001
- Literal rule enforcement: Rule 11 删除样例纵向，不新建层，也不把样例类型留在磁盘上满足旧哈希或旧计数。
- Implementation pseudocode:

```text
remove_tree enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/mq
remain brand, product, ad, search, sync, file
package infrastructure/mq has no remaining Java file
```

- Verification contribution: 删除后的架构测试与编译。
- After this file: enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/mq 不存在。
#### File 22 — `DELETE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/mq`

- Purpose: 删除test源码中的样例包 infrastructure/mq。
- Symbols: infrastructure/mq
- Repository evidence: 目录存在，且只被样例控制器、门面或样例消息使用。
- Dependencies and consumers: Step 5 后面的启动类和合同测试不再引用它。
- Why now: 架构合同已经要求它不存在。
- Contract/signature changes: 整个目录删除，不保留 package-info 来满足旧测试。
- Input/output and state mapping: 没有运行时替身。品牌、商品、广告、搜索目录不在这条路径里。
- Error and edge behavior: 若目录里意外出现非样例类型，停止并回到规格，不把该类型改成匿名接口。
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-ARCH-001
- Literal rule enforcement: Rule 11 删除样例纵向，不新建层，也不把样例类型留在磁盘上满足旧哈希或旧计数。
- Implementation pseudocode:

```text
remove_tree enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/mq
remain brand, product, ad, search, sync, file
package infrastructure/mq has no remaining Java file
```

- Verification contribution: 删除后的架构测试与编译。
- After this file: enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/mq 不存在。
#### File 23 — `DELETE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/exception/UserUseCaseException.java`

- Purpose: 删除只服务样例的 UserUseCaseException。
- Symbols: UserUseCaseException
- Repository evidence: 引用搜索显示它只被用户或教学样例、样例 MQ 或样例 RPC 使用。
- Dependencies and consumers: 保留 BaseBusinessException、ConfigDecryptException、CdcRabbitConfiguration、PersistenceTestSupport。
- Why now: 所在包的样例调用方已经列入删除。
- Contract/signature changes: 单文件或单目录删除，不改同目录里的非样例文件。
- Input/output and state mapping: 没有新的匿名替换接口。
- Error and edge behavior: RabbitMqConfig 删除后 CDC 仍由 CdcRabbitConfiguration 声明自己的交换机和队列。
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-ARCH-001
- Literal rule enforcement: Rule 11 删除样例纵向，不新建层，也不把样例类型留在磁盘上满足旧哈希或旧计数。
- Implementation pseudocode:

```text
delete enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/exception/UserUseCaseException.java
keep CdcRabbitConfiguration and BaseBusinessException
no sample replacement route
```

- Verification contribution: 主源码不再引用该符号。
- After this file: UserUseCaseException 已从树中消失。
#### File 24 — `DELETE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/exception/TeachingUseCaseException.java`

- Purpose: 删除只服务样例的 TeachingUseCaseException。
- Symbols: TeachingUseCaseException
- Repository evidence: 引用搜索显示它只被用户或教学样例、样例 MQ 或样例 RPC 使用。
- Dependencies and consumers: 保留 BaseBusinessException、ConfigDecryptException、CdcRabbitConfiguration、PersistenceTestSupport。
- Why now: 所在包的样例调用方已经列入删除。
- Contract/signature changes: 单文件或单目录删除，不改同目录里的非样例文件。
- Input/output and state mapping: 没有新的匿名替换接口。
- Error and edge behavior: RabbitMqConfig 删除后 CDC 仍由 CdcRabbitConfiguration 声明自己的交换机和队列。
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-ARCH-001
- Literal rule enforcement: Rule 11 删除样例纵向，不新建层，也不把样例类型留在磁盘上满足旧哈希或旧计数。
- Implementation pseudocode:

```text
delete enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/exception/TeachingUseCaseException.java
keep CdcRabbitConfiguration and BaseBusinessException
no sample replacement route
```

- Verification contribution: 主源码不再引用该符号。
- After this file: TeachingUseCaseException 已从树中消失。
#### File 25 — `DELETE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/exception/UserDomainException.java`

- Purpose: 删除只服务样例的 UserDomainException。
- Symbols: UserDomainException
- Repository evidence: 引用搜索显示它只被用户或教学样例、样例 MQ 或样例 RPC 使用。
- Dependencies and consumers: 保留 BaseBusinessException、ConfigDecryptException、CdcRabbitConfiguration、PersistenceTestSupport。
- Why now: 所在包的样例调用方已经列入删除。
- Contract/signature changes: 单文件或单目录删除，不改同目录里的非样例文件。
- Input/output and state mapping: 没有新的匿名替换接口。
- Error and edge behavior: RabbitMqConfig 删除后 CDC 仍由 CdcRabbitConfiguration 声明自己的交换机和队列。
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-ARCH-001
- Literal rule enforcement: Rule 11 删除样例纵向，不新建层，也不把样例类型留在磁盘上满足旧哈希或旧计数。
- Implementation pseudocode:

```text
delete enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/exception/UserDomainException.java
keep CdcRabbitConfiguration and BaseBusinessException
no sample replacement route
```

- Verification contribution: 主源码不再引用该符号。
- After this file: UserDomainException 已从树中消失。
#### File 26 — `DELETE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/exception/TeachingDomainException.java`

- Purpose: 删除只服务样例的 TeachingDomainException。
- Symbols: TeachingDomainException
- Repository evidence: 引用搜索显示它只被用户或教学样例、样例 MQ 或样例 RPC 使用。
- Dependencies and consumers: 保留 BaseBusinessException、ConfigDecryptException、CdcRabbitConfiguration、PersistenceTestSupport。
- Why now: 所在包的样例调用方已经列入删除。
- Contract/signature changes: 单文件或单目录删除，不改同目录里的非样例文件。
- Input/output and state mapping: 没有新的匿名替换接口。
- Error and edge behavior: RabbitMqConfig 删除后 CDC 仍由 CdcRabbitConfiguration 声明自己的交换机和队列。
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-ARCH-001
- Literal rule enforcement: Rule 11 删除样例纵向，不新建层，也不把样例类型留在磁盘上满足旧哈希或旧计数。
- Implementation pseudocode:

```text
delete enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/exception/TeachingDomainException.java
keep CdcRabbitConfiguration and BaseBusinessException
no sample replacement route
```

- Verification contribution: 主源码不再引用该符号。
- After this file: TeachingDomainException 已从树中消失。
#### File 27 — `DELETE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/exception/UserFacadeException.java`

- Purpose: 删除只服务样例的 UserFacadeException。
- Symbols: UserFacadeException
- Repository evidence: 引用搜索显示它只被用户或教学样例、样例 MQ 或样例 RPC 使用。
- Dependencies and consumers: 保留 BaseBusinessException、ConfigDecryptException、CdcRabbitConfiguration、PersistenceTestSupport。
- Why now: 所在包的样例调用方已经列入删除。
- Contract/signature changes: 单文件或单目录删除，不改同目录里的非样例文件。
- Input/output and state mapping: 没有新的匿名替换接口。
- Error and edge behavior: RabbitMqConfig 删除后 CDC 仍由 CdcRabbitConfiguration 声明自己的交换机和队列。
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-ARCH-001
- Literal rule enforcement: Rule 11 删除样例纵向，不新建层，也不把样例类型留在磁盘上满足旧哈希或旧计数。
- Implementation pseudocode:

```text
delete enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/exception/UserFacadeException.java
keep CdcRabbitConfiguration and BaseBusinessException
no sample replacement route
```

- Verification contribution: 主源码不再引用该符号。
- After this file: UserFacadeException 已从树中消失。
#### File 28 — `DELETE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/exception/TeachingFacadeException.java`

- Purpose: 删除只服务样例的 TeachingFacadeException。
- Symbols: TeachingFacadeException
- Repository evidence: 引用搜索显示它只被用户或教学样例、样例 MQ 或样例 RPC 使用。
- Dependencies and consumers: 保留 BaseBusinessException、ConfigDecryptException、CdcRabbitConfiguration、PersistenceTestSupport。
- Why now: 所在包的样例调用方已经列入删除。
- Contract/signature changes: 单文件或单目录删除，不改同目录里的非样例文件。
- Input/output and state mapping: 没有新的匿名替换接口。
- Error and edge behavior: RabbitMqConfig 删除后 CDC 仍由 CdcRabbitConfiguration 声明自己的交换机和队列。
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-ARCH-001
- Literal rule enforcement: Rule 11 删除样例纵向，不新建层，也不把样例类型留在磁盘上满足旧哈希或旧计数。
- Implementation pseudocode:

```text
delete enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/exception/TeachingFacadeException.java
keep CdcRabbitConfiguration and BaseBusinessException
no sample replacement route
```

- Verification contribution: 主源码不再引用该符号。
- After this file: TeachingFacadeException 已从树中消失。
#### File 29 — `DELETE enjoyshop-service/enjoyshop-service-goods/src/main/resources/graphql/user.graphqls`

- Purpose: 删除只服务样例的 user.graphqls。
- Symbols: user.graphqls
- Repository evidence: 引用搜索显示它只被用户或教学样例、样例 MQ 或样例 RPC 使用。
- Dependencies and consumers: 保留 BaseBusinessException、ConfigDecryptException、CdcRabbitConfiguration、PersistenceTestSupport。
- Why now: 所在包的样例调用方已经列入删除。
- Contract/signature changes: 单文件或单目录删除，不改同目录里的非样例文件。
- Input/output and state mapping: 没有新的匿名替换接口。
- Error and edge behavior: RabbitMqConfig 删除后 CDC 仍由 CdcRabbitConfiguration 声明自己的交换机和队列。
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-ARCH-001
- Literal rule enforcement: Rule 11 删除样例纵向，不新建层，也不把样例类型留在磁盘上满足旧哈希或旧计数。
- Implementation pseudocode:

```text
delete enjoyshop-service/enjoyshop-service-goods/src/main/resources/graphql/user.graphqls
keep CdcRabbitConfiguration and BaseBusinessException
no sample replacement route
```

- Verification contribution: 主源码不再引用该符号。
- After this file: user.graphqls 已从树中消失。
#### File 30 — `DELETE enjoyshop-service/enjoyshop-service-goods/src/main/resources/graphql/teaching.graphqls`

- Purpose: 删除只服务样例的 teaching.graphqls。
- Symbols: teaching.graphqls
- Repository evidence: 引用搜索显示它只被用户或教学样例、样例 MQ 或样例 RPC 使用。
- Dependencies and consumers: 保留 BaseBusinessException、ConfigDecryptException、CdcRabbitConfiguration、PersistenceTestSupport。
- Why now: 所在包的样例调用方已经列入删除。
- Contract/signature changes: 单文件或单目录删除，不改同目录里的非样例文件。
- Input/output and state mapping: 没有新的匿名替换接口。
- Error and edge behavior: RabbitMqConfig 删除后 CDC 仍由 CdcRabbitConfiguration 声明自己的交换机和队列。
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-ARCH-001
- Literal rule enforcement: Rule 11 删除样例纵向，不新建层，也不把样例类型留在磁盘上满足旧哈希或旧计数。
- Implementation pseudocode:

```text
delete enjoyshop-service/enjoyshop-service-goods/src/main/resources/graphql/teaching.graphqls
keep CdcRabbitConfiguration and BaseBusinessException
no sample replacement route
```

- Verification contribution: 主源码不再引用该符号。
- After this file: teaching.graphqls 已从树中消失。
#### File 31 — `DELETE enjoyshop-service/enjoyshop-service-goods/src/main/proto/teaching_user_facade.proto`

- Purpose: 删除只服务样例的 teaching_user_facade.proto。
- Symbols: teaching_user_facade.proto
- Repository evidence: 引用搜索显示它只被用户或教学样例、样例 MQ 或样例 RPC 使用。
- Dependencies and consumers: 保留 BaseBusinessException、ConfigDecryptException、CdcRabbitConfiguration、PersistenceTestSupport。
- Why now: 所在包的样例调用方已经列入删除。
- Contract/signature changes: 单文件或单目录删除，不改同目录里的非样例文件。
- Input/output and state mapping: 没有新的匿名替换接口。
- Error and edge behavior: RabbitMqConfig 删除后 CDC 仍由 CdcRabbitConfiguration 声明自己的交换机和队列。
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-ARCH-001
- Literal rule enforcement: Rule 11 删除样例纵向，不新建层，也不把样例类型留在磁盘上满足旧哈希或旧计数。
- Implementation pseudocode:

```text
delete enjoyshop-service/enjoyshop-service-goods/src/main/proto/teaching_user_facade.proto
keep CdcRabbitConfiguration and BaseBusinessException
no sample replacement route
```

- Verification contribution: 主源码不再引用该符号。
- After this file: teaching_user_facade.proto 已从树中消失。
#### File 32 — `DELETE enjoyshop-service/enjoyshop-service-goods/src/main/resources/mybatis/mapper/user`

- Purpose: 删除只服务样例的 mapper/user。
- Symbols: mapper/user
- Repository evidence: 引用搜索显示它只被用户或教学样例、样例 MQ 或样例 RPC 使用。
- Dependencies and consumers: 保留 BaseBusinessException、ConfigDecryptException、CdcRabbitConfiguration、PersistenceTestSupport。
- Why now: 所在包的样例调用方已经列入删除。
- Contract/signature changes: 单文件或单目录删除，不改同目录里的非样例文件。
- Input/output and state mapping: 没有新的匿名替换接口。
- Error and edge behavior: RabbitMqConfig 删除后 CDC 仍由 CdcRabbitConfiguration 声明自己的交换机和队列。
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-ARCH-001
- Literal rule enforcement: Rule 11 删除样例纵向，不新建层，也不把样例类型留在磁盘上满足旧哈希或旧计数。
- Implementation pseudocode:

```text
delete enjoyshop-service/enjoyshop-service-goods/src/main/resources/mybatis/mapper/user
keep CdcRabbitConfiguration and BaseBusinessException
no sample replacement route
```

- Verification contribution: 主源码不再引用该符号。
- After this file: mapper/user 已从树中消失。
#### File 33 — `DELETE enjoyshop-service/enjoyshop-service-goods/src/main/resources/mybatis/mapper/teaching`

- Purpose: 删除只服务样例的 mapper/teaching。
- Symbols: mapper/teaching
- Repository evidence: 引用搜索显示它只被用户或教学样例、样例 MQ 或样例 RPC 使用。
- Dependencies and consumers: 保留 BaseBusinessException、ConfigDecryptException、CdcRabbitConfiguration、PersistenceTestSupport。
- Why now: 所在包的样例调用方已经列入删除。
- Contract/signature changes: 单文件或单目录删除，不改同目录里的非样例文件。
- Input/output and state mapping: 没有新的匿名替换接口。
- Error and edge behavior: RabbitMqConfig 删除后 CDC 仍由 CdcRabbitConfiguration 声明自己的交换机和队列。
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-ARCH-001
- Literal rule enforcement: Rule 11 删除样例纵向，不新建层，也不把样例类型留在磁盘上满足旧哈希或旧计数。
- Implementation pseudocode:

```text
delete enjoyshop-service/enjoyshop-service-goods/src/main/resources/mybatis/mapper/teaching
keep CdcRabbitConfiguration and BaseBusinessException
no sample replacement route
```

- Verification contribution: 主源码不再引用该符号。
- After this file: mapper/teaching 已从树中消失。
#### File 34 — `DELETE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/pojo/convertor/LightFacadeConverter.java`

- Purpose: 删除只服务样例的 LightFacadeConverter。
- Symbols: LightFacadeConverter
- Repository evidence: 引用搜索显示它只被用户或教学样例、样例 MQ 或样例 RPC 使用。
- Dependencies and consumers: 保留 BaseBusinessException、ConfigDecryptException、CdcRabbitConfiguration、PersistenceTestSupport。
- Why now: 所在包的样例调用方已经列入删除。
- Contract/signature changes: 单文件或单目录删除，不改同目录里的非样例文件。
- Input/output and state mapping: 没有新的匿名替换接口。
- Error and edge behavior: RabbitMqConfig 删除后 CDC 仍由 CdcRabbitConfiguration 声明自己的交换机和队列。
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-ARCH-001
- Literal rule enforcement: Rule 11 删除样例纵向，不新建层，也不把样例类型留在磁盘上满足旧哈希或旧计数。
- Implementation pseudocode:

```text
delete enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/pojo/convertor/LightFacadeConverter.java
keep CdcRabbitConfiguration and BaseBusinessException
no sample replacement route
```

- Verification contribution: 主源码不再引用该符号。
- After this file: LightFacadeConverter 已从树中消失。
#### File 35 — `DELETE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/config/ExternalClientConfig.java`

- Purpose: 删除只服务样例的 ExternalClientConfig。
- Symbols: ExternalClientConfig
- Repository evidence: 引用搜索显示它只被用户或教学样例、样例 MQ 或样例 RPC 使用。
- Dependencies and consumers: 保留 BaseBusinessException、ConfigDecryptException、CdcRabbitConfiguration、PersistenceTestSupport。
- Why now: 所在包的样例调用方已经列入删除。
- Contract/signature changes: 单文件或单目录删除，不改同目录里的非样例文件。
- Input/output and state mapping: 没有新的匿名替换接口。
- Error and edge behavior: RabbitMqConfig 删除后 CDC 仍由 CdcRabbitConfiguration 声明自己的交换机和队列。
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-ARCH-001
- Literal rule enforcement: Rule 11 删除样例纵向，不新建层，也不把样例类型留在磁盘上满足旧哈希或旧计数。
- Implementation pseudocode:

```text
delete enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/config/ExternalClientConfig.java
keep CdcRabbitConfiguration and BaseBusinessException
no sample replacement route
```

- Verification contribution: 主源码不再引用该符号。
- After this file: ExternalClientConfig 已从树中消失。
#### File 36 — `DELETE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/config/LocalAdapterConfiguration.java`

- Purpose: 删除只服务样例的 LocalAdapterConfiguration。
- Symbols: LocalAdapterConfiguration
- Repository evidence: 引用搜索显示它只被用户或教学样例、样例 MQ 或样例 RPC 使用。
- Dependencies and consumers: 保留 BaseBusinessException、ConfigDecryptException、CdcRabbitConfiguration、PersistenceTestSupport。
- Why now: 所在包的样例调用方已经列入删除。
- Contract/signature changes: 单文件或单目录删除，不改同目录里的非样例文件。
- Input/output and state mapping: 没有新的匿名替换接口。
- Error and edge behavior: RabbitMqConfig 删除后 CDC 仍由 CdcRabbitConfiguration 声明自己的交换机和队列。
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-ARCH-001
- Literal rule enforcement: Rule 11 删除样例纵向，不新建层，也不把样例类型留在磁盘上满足旧哈希或旧计数。
- Implementation pseudocode:

```text
delete enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/config/LocalAdapterConfiguration.java
keep CdcRabbitConfiguration and BaseBusinessException
no sample replacement route
```

- Verification contribution: 主源码不再引用该符号。
- After this file: LocalAdapterConfiguration 已从树中消失。
#### File 37 — `DELETE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/config/RabbitMqConfig.java`

- Purpose: 删除只服务样例的 RabbitMqConfig。
- Symbols: RabbitMqConfig
- Repository evidence: 引用搜索显示它只被用户或教学样例、样例 MQ 或样例 RPC 使用。
- Dependencies and consumers: 保留 BaseBusinessException、ConfigDecryptException、CdcRabbitConfiguration、PersistenceTestSupport。
- Why now: 所在包的样例调用方已经列入删除。
- Contract/signature changes: 单文件或单目录删除，不改同目录里的非样例文件。
- Input/output and state mapping: 没有新的匿名替换接口。
- Error and edge behavior: RabbitMqConfig 删除后 CDC 仍由 CdcRabbitConfiguration 声明自己的交换机和队列。
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-ARCH-001
- Literal rule enforcement: Rule 11 删除样例纵向，不新建层，也不把样例类型留在磁盘上满足旧哈希或旧计数。
- Implementation pseudocode:

```text
delete enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/config/RabbitMqConfig.java
keep CdcRabbitConfiguration and BaseBusinessException
no sample replacement route
```

- Verification contribution: 主源码不再引用该符号。
- After this file: RabbitMqConfig 已从树中消失。
#### File 38 — `DELETE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/config/TransactionCompletionExecutor.java`

- Purpose: 删除只服务样例的 TransactionCompletionExecutor。
- Symbols: TransactionCompletionExecutor
- Repository evidence: 引用搜索显示它只被用户或教学样例、样例 MQ 或样例 RPC 使用。
- Dependencies and consumers: 保留 BaseBusinessException、ConfigDecryptException、CdcRabbitConfiguration、PersistenceTestSupport。
- Why now: 所在包的样例调用方已经列入删除。
- Contract/signature changes: 单文件或单目录删除，不改同目录里的非样例文件。
- Input/output and state mapping: 没有新的匿名替换接口。
- Error and edge behavior: RabbitMqConfig 删除后 CDC 仍由 CdcRabbitConfiguration 声明自己的交换机和队列。
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-ARCH-001
- Literal rule enforcement: Rule 11 删除样例纵向，不新建层，也不把样例类型留在磁盘上满足旧哈希或旧计数。
- Implementation pseudocode:

```text
delete enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/config/TransactionCompletionExecutor.java
keep CdcRabbitConfiguration and BaseBusinessException
no sample replacement route
```

- Verification contribution: 主源码不再引用该符号。
- After this file: TransactionCompletionExecutor 已从树中消失。
#### File 39 — `DELETE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/config/RabbitMqConfigTest.java`

- Purpose: 删除只服务样例的 RabbitMqConfigTest。
- Symbols: RabbitMqConfigTest
- Repository evidence: 引用搜索显示它只被用户或教学样例、样例 MQ 或样例 RPC 使用。
- Dependencies and consumers: 保留 BaseBusinessException、ConfigDecryptException、CdcRabbitConfiguration、PersistenceTestSupport。
- Why now: 所在包的样例调用方已经列入删除。
- Contract/signature changes: 单文件或单目录删除，不改同目录里的非样例文件。
- Input/output and state mapping: 没有新的匿名替换接口。
- Error and edge behavior: RabbitMqConfig 删除后 CDC 仍由 CdcRabbitConfiguration 声明自己的交换机和队列。
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-ARCH-001
- Literal rule enforcement: Rule 11 删除样例纵向，不新建层，也不把样例类型留在磁盘上满足旧哈希或旧计数。
- Implementation pseudocode:

```text
delete enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/config/RabbitMqConfigTest.java
keep CdcRabbitConfiguration and BaseBusinessException
no sample replacement route
```

- Verification contribution: 主源码不再引用该符号。
- After this file: RabbitMqConfigTest 已从树中消失。
#### File 40 — `DELETE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/config/TransactionCompletionExecutorTest.java`

- Purpose: 删除只服务样例的 TransactionCompletionExecutorTest。
- Symbols: TransactionCompletionExecutorTest
- Repository evidence: 引用搜索显示它只被用户或教学样例、样例 MQ 或样例 RPC 使用。
- Dependencies and consumers: 保留 BaseBusinessException、ConfigDecryptException、CdcRabbitConfiguration、PersistenceTestSupport。
- Why now: 所在包的样例调用方已经列入删除。
- Contract/signature changes: 单文件或单目录删除，不改同目录里的非样例文件。
- Input/output and state mapping: 没有新的匿名替换接口。
- Error and edge behavior: RabbitMqConfig 删除后 CDC 仍由 CdcRabbitConfiguration 声明自己的交换机和队列。
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-ARCH-001
- Literal rule enforcement: Rule 11 删除样例纵向，不新建层，也不把样例类型留在磁盘上满足旧哈希或旧计数。
- Implementation pseudocode:

```text
delete enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/config/TransactionCompletionExecutorTest.java
keep CdcRabbitConfiguration and BaseBusinessException
no sample replacement route
```

- Verification contribution: 主源码不再引用该符号。
- After this file: TransactionCompletionExecutorTest 已从树中消失。
#### File 41 — `DELETE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/support/RecordingMqMessageService.java`

- Purpose: 删除只服务样例的 RecordingMqMessageService。
- Symbols: RecordingMqMessageService
- Repository evidence: 引用搜索显示它只被用户或教学样例、样例 MQ 或样例 RPC 使用。
- Dependencies and consumers: 保留 BaseBusinessException、ConfigDecryptException、CdcRabbitConfiguration、PersistenceTestSupport。
- Why now: 所在包的样例调用方已经列入删除。
- Contract/signature changes: 单文件或单目录删除，不改同目录里的非样例文件。
- Input/output and state mapping: 没有新的匿名替换接口。
- Error and edge behavior: RabbitMqConfig 删除后 CDC 仍由 CdcRabbitConfiguration 声明自己的交换机和队列。
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-ARCH-001
- Literal rule enforcement: Rule 11 删除样例纵向，不新建层，也不把样例类型留在磁盘上满足旧哈希或旧计数。
- Implementation pseudocode:

```text
delete enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/support/RecordingMqMessageService.java
keep CdcRabbitConfiguration and BaseBusinessException
no sample replacement route
```

- Verification contribution: 主源码不再引用该符号。
- After this file: RecordingMqMessageService 已从树中消失。
#### File 42 — `DELETE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/application/transaction/LocalTransactionBoundaryTest.java`

- Purpose: 删除只服务样例的 LocalTransactionBoundaryTest。
- Symbols: LocalTransactionBoundaryTest
- Repository evidence: 引用搜索显示它只被用户或教学样例、样例 MQ 或样例 RPC 使用。
- Dependencies and consumers: 保留 BaseBusinessException、ConfigDecryptException、CdcRabbitConfiguration、PersistenceTestSupport。
- Why now: 所在包的样例调用方已经列入删除。
- Contract/signature changes: 单文件或单目录删除，不改同目录里的非样例文件。
- Input/output and state mapping: 没有新的匿名替换接口。
- Error and edge behavior: RabbitMqConfig 删除后 CDC 仍由 CdcRabbitConfiguration 声明自己的交换机和队列。
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-ARCH-001
- Literal rule enforcement: Rule 11 删除样例纵向，不新建层，也不把样例类型留在磁盘上满足旧哈希或旧计数。
- Implementation pseudocode:

```text
delete enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/application/transaction/LocalTransactionBoundaryTest.java
keep CdcRabbitConfiguration and BaseBusinessException
no sample replacement route
```

- Verification contribution: 主源码不再引用该符号。
- After this file: LocalTransactionBoundaryTest 已从树中消失。
#### File 43 — `DELETE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/facade/NativeRpcMappingTest.java`

- Purpose: 删除只服务样例的 NativeRpcMappingTest。
- Symbols: NativeRpcMappingTest
- Repository evidence: 引用搜索显示它只被用户或教学样例、样例 MQ 或样例 RPC 使用。
- Dependencies and consumers: 保留 BaseBusinessException、ConfigDecryptException、CdcRabbitConfiguration、PersistenceTestSupport。
- Why now: 所在包的样例调用方已经列入删除。
- Contract/signature changes: 单文件或单目录删除，不改同目录里的非样例文件。
- Input/output and state mapping: 没有新的匿名替换接口。
- Error and edge behavior: RabbitMqConfig 删除后 CDC 仍由 CdcRabbitConfiguration 声明自己的交换机和队列。
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-ARCH-001
- Literal rule enforcement: Rule 11 删除样例纵向，不新建层，也不把样例类型留在磁盘上满足旧哈希或旧计数。
- Implementation pseudocode:

```text
delete enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/facade/NativeRpcMappingTest.java
keep CdcRabbitConfiguration and BaseBusinessException
no sample replacement route
```

- Verification contribution: 主源码不再引用该符号。
- After this file: NativeRpcMappingTest 已从树中消失。
#### File 44 — `DELETE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/NativeRpcProviderTest.java`

- Purpose: 删除只服务样例的 NativeRpcProviderTest。
- Symbols: NativeRpcProviderTest
- Repository evidence: 引用搜索显示它只被用户或教学样例、样例 MQ 或样例 RPC 使用。
- Dependencies and consumers: 保留 BaseBusinessException、ConfigDecryptException、CdcRabbitConfiguration、PersistenceTestSupport。
- Why now: 所在包的样例调用方已经列入删除。
- Contract/signature changes: 单文件或单目录删除，不改同目录里的非样例文件。
- Input/output and state mapping: 没有新的匿名替换接口。
- Error and edge behavior: RabbitMqConfig 删除后 CDC 仍由 CdcRabbitConfiguration 声明自己的交换机和队列。
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-ARCH-001
- Literal rule enforcement: Rule 11 删除样例纵向，不新建层，也不把样例类型留在磁盘上满足旧哈希或旧计数。
- Implementation pseudocode:

```text
delete enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/NativeRpcProviderTest.java
keep CdcRabbitConfiguration and BaseBusinessException
no sample replacement route
```

- Verification contribution: 主源码不再引用该符号。
- After this file: NativeRpcProviderTest 已从树中消失。
#### File 45 — `DELETE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/facade/NativeRpcContractTest.java`

- Purpose: 删除只服务样例的 NativeRpcContractTest。
- Symbols: NativeRpcContractTest
- Repository evidence: 引用搜索显示它只被用户或教学样例、样例 MQ 或样例 RPC 使用。
- Dependencies and consumers: 保留 BaseBusinessException、ConfigDecryptException、CdcRabbitConfiguration、PersistenceTestSupport。
- Why now: 所在包的样例调用方已经列入删除。
- Contract/signature changes: 单文件或单目录删除，不改同目录里的非样例文件。
- Input/output and state mapping: 没有新的匿名替换接口。
- Error and edge behavior: RabbitMqConfig 删除后 CDC 仍由 CdcRabbitConfiguration 声明自己的交换机和队列。
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-ARCH-001
- Literal rule enforcement: Rule 11 删除样例纵向，不新建层，也不把样例类型留在磁盘上满足旧哈希或旧计数。
- Implementation pseudocode:

```text
delete enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/facade/NativeRpcContractTest.java
keep CdcRabbitConfiguration and BaseBusinessException
no sample replacement route
```

- Verification contribution: 主源码不再引用该符号。
- After this file: NativeRpcContractTest 已从树中消失。
#### File 46 — `DELETE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/config/datasource/RepositoryPersistenceContractTest.java`

- Purpose: 删除只服务样例的 RepositoryPersistenceContractTest。
- Symbols: RepositoryPersistenceContractTest
- Repository evidence: 引用搜索显示它只被用户或教学样例、样例 MQ 或样例 RPC 使用。
- Dependencies and consumers: 保留 BaseBusinessException、ConfigDecryptException、CdcRabbitConfiguration、PersistenceTestSupport。
- Why now: 所在包的样例调用方已经列入删除。
- Contract/signature changes: 单文件或单目录删除，不改同目录里的非样例文件。
- Input/output and state mapping: 没有新的匿名替换接口。
- Error and edge behavior: RabbitMqConfig 删除后 CDC 仍由 CdcRabbitConfiguration 声明自己的交换机和队列。
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-ARCH-001
- Literal rule enforcement: Rule 11 删除样例纵向，不新建层，也不把样例类型留在磁盘上满足旧哈希或旧计数。
- Implementation pseudocode:

```text
delete enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/config/datasource/RepositoryPersistenceContractTest.java
keep CdcRabbitConfiguration and BaseBusinessException
no sample replacement route
```

- Verification contribution: 主源码不再引用该符号。
- After this file: RepositoryPersistenceContractTest 已从树中消失。
#### File 47 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/start/StudentManagementApplication.java`

- Purpose: MapperScan 去掉两个样例 DAO 包。
- Symbols: StudentManagementApplication
- Repository evidence: 扫描名单含 infrastructure.user.dao、infrastructure.teaching.dao、infrastructure.goods.dao、infrastructure.ad.dao。
- Dependencies and consumers: MyBatis 启动时扫描。类名保持不变。
- Why now: 样例 DAO 目录已经删除，扫描它们会使启动失败。
- Contract/signature changes: 只删除两行样例包名。
- Input/output and state mapping: goods 与 ad 的 DAO 扫描保留。
- Error and edge behavior: 不重命名 StudentManagementApplication。
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-ARCH-001
- Literal rule enforcement: Rule 11 启动类仍在 start 包，只减少扫描范围。
- Implementation pseudocode:

```java
@MapperScan(basePackages = {
    "top.egon.enjoyshop.goods.infrastructure.goods.dao",
    "top.egon.enjoyshop.goods.infrastructure.ad.dao"
})
```

- Verification contribution: 应用上下文测试不再寻找样例 Mapper。
- After this file: 扫描名单只剩商品和广告。
#### File 48 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/start/config/NativeRpcConfiguration.java`

- Purpose: 删除样例 LightFacadeConverter Bean，保留校验 Bean。
- Symbols: nativeRpcValidation, nativeRpcValidationExceptionMapper
- Repository evidence: lightFacadeConverter 是文件里唯一的样例 Bean。另外两个 Bean 把 ValidationUtils 暴露给 RPC。
- Dependencies and consumers: NativeRpcConfigurationTest 还断言 lightFacadeConverter，必须在同一步改掉。
- Why now: 转换器接口已经删除。
- Contract/signature changes: 删除 lightFacadeConverter 方法与 MapStruct import。
- Input/output and state mapping: nativeRpcValidation 仍返回注入的 egonColaValidationUtils。
- Error and edge behavior: 没有新的 RPC 服务。
- Standards impact: MC-REUSE-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 4 剩余 Bean 保持原有名称和 @Qualifier；Rule 11 不新增 RPC 层。
- Implementation pseudocode:

```java
@Bean("nativeRpcValidation")
@Primary
ValidationUtils nativeRpcValidation(@Qualifier("egonColaValidationUtils") ValidationUtils validation) {
  return validation;
}
@Bean("nativeRpcValidationExceptionMapper")
RpcProviderExceptionMapper nativeRpcValidationExceptionMapper() {
  return failure -> failure instanceof ConstraintViolationException || failure instanceof DateTimeParseException
      ? Optional.of(Status.INVALID_ARGUMENT.withDescription("Invalid RPC request").asRuntimeException())
      : Optional.empty();
}
```

- Verification contribution: NativeRpcConfigurationTest 不再取 lightFacadeConverter。
- After this file: 配置类只保留通用校验接线。
#### File 49 — `MODIFY enjoyshop-service/enjoyshop-service-goods/pom.xml`

- Purpose: 去掉只指向已删除转换器的 facadeImplementationPackage。
- Symbols: facadeImplementationPackage
- Repository evidence: 第 329 行是 LightFacadeConverterImpl。上一行 ..adapter.. 通配保留。
- Dependencies and consumers: MapStruct 编译期扫描。
- Why now: 接口已删除，留下实现包名会让生成失败。
- Contract/signature changes: 只删那一个子元素。
- Input/output and state mapping: 不新增依赖，不删除 starter。
- Error and edge behavior: POM 仍不得出现 debezium、kafka-clients、elasticsearch、transactional-outbox-starter。
- Standards impact: MC-DEP-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 5 不引入新的工具库；Rule 11 不改变模块结构。
- Implementation pseudocode:

```xml
<facadeImplementationPackages>
  <facadeImplementationPackage>..adapter..</facadeImplementationPackage>
</facadeImplementationPackages>
```

- Verification contribution: NativeReadwriteContractTest 的 POM 禁止项。
- After this file: MapStruct 不再寻找 LightFacadeConverterImpl。
#### File 50 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/resources/application.yml`

- Purpose: 删除样例用户和教学服务的 HTTP 基址。
- Symbols: app.integrations.external-http.user-base-url, app.integrations.external-http.teaching-base-url
- Repository evidence: 这两个键只被已删除的 ExternalClientConfig 读取。application-test.yml 本来就没有它们。
- Dependencies and consumers: ProfileKeyParityTest 不把这两个键列入 GOVERNANCE_KEYS，但规则 7 要求删除时三份一起删。
- Why now: 配置类已经删除，留下键没有消费者。
- Contract/signature changes: 不新增键，不改 shoubing.enabled 的默认 false，不改 external-http.enabled，不改 app.integrations.rabbitmq.enabled。
- Input/output and state mapping: application、dev、prod 都不再含这两键。test profile 不补上它们。
- Error and edge behavior: spring.rabbitmq 与 app.cdc 段不动。
- Standards impact: MC-CONFIG-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 7 三份有该键的 profile 同步删除，值的差异不通过新增键修复。
- Implementation pseudocode:

```yaml
# in application.yml under app.integrations.external-http keep enabled and delete only:
# user-base-url
# teaching-base-url
```

- Verification contribution: ProfileKeyParityTest 仍通过。
- After this file: application.yml 不再声明样例基址。
#### File 51 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-dev.yml`

- Purpose: 删除样例用户和教学服务的 HTTP 基址。
- Symbols: app.integrations.external-http.user-base-url, app.integrations.external-http.teaching-base-url
- Repository evidence: 这两个键只被已删除的 ExternalClientConfig 读取。application-test.yml 本来就没有它们。
- Dependencies and consumers: ProfileKeyParityTest 不把这两个键列入 GOVERNANCE_KEYS，但规则 7 要求删除时三份一起删。
- Why now: 配置类已经删除，留下键没有消费者。
- Contract/signature changes: 不新增键，不改 shoubing.enabled 的默认 false，不改 external-http.enabled，不改 app.integrations.rabbitmq.enabled。
- Input/output and state mapping: application、dev、prod 都不再含这两键。test profile 不补上它们。
- Error and edge behavior: spring.rabbitmq 与 app.cdc 段不动。
- Standards impact: MC-CONFIG-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 7 三份有该键的 profile 同步删除，值的差异不通过新增键修复。
- Implementation pseudocode:

```yaml
# in application-dev.yml under app.integrations.external-http keep enabled and delete only:
# user-base-url
# teaching-base-url
```

- Verification contribution: ProfileKeyParityTest 仍通过。
- After this file: application-dev.yml 不再声明样例基址。
#### File 52 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-prod.yml`

- Purpose: 删除样例用户和教学服务的 HTTP 基址。
- Symbols: app.integrations.external-http.user-base-url, app.integrations.external-http.teaching-base-url
- Repository evidence: 这两个键只被已删除的 ExternalClientConfig 读取。application-test.yml 本来就没有它们。
- Dependencies and consumers: ProfileKeyParityTest 不把这两个键列入 GOVERNANCE_KEYS，但规则 7 要求删除时三份一起删。
- Why now: 配置类已经删除，留下键没有消费者。
- Contract/signature changes: 不新增键，不改 shoubing.enabled 的默认 false，不改 external-http.enabled，不改 app.integrations.rabbitmq.enabled。
- Input/output and state mapping: application、dev、prod 都不再含这两键。test profile 不补上它们。
- Error and edge behavior: spring.rabbitmq 与 app.cdc 段不动。
- Standards impact: MC-CONFIG-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 7 三份有该键的 profile 同步删除，值的差异不通过新增键修复。
- Implementation pseudocode:

```yaml
# in application-prod.yml under app.integrations.external-http keep enabled and delete only:
# user-base-url
# teaching-base-url
```

- Verification contribution: ProfileKeyParityTest 仍通过。
- After this file: application-prod.yml 不再声明样例基址。
#### File 53 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/start/config/NativeHttpCompatibilityTest.java`

- Purpose: 去掉对已删除 /api/users 和 /api/courses 的匿名探测。
- Symbols: business_post_keeps_public_access_request_context_and_json_contract
- Repository evidence: 测试用 SpringBootTest 向 /api/users POST，并带 X-Operator-Id，还断言 OpenAPI 含 /api/courses。
- Dependencies and consumers: 公开商品搜索仍是匿名 GET。品牌 POST 已不再匿名。
- Why now: 样例控制器删除后这些路径不会注册。
- Contract/signature changes: 公开访问断言改到 GET /api/v1/product-search。删除用户和课程的 paths 断言。
- Input/output and state mapping: 不再发送 X-Operator-Id 或 X-Actor-Id。
- Error and edge behavior: 不把品牌或上传改回 permitAll 来保住旧断言。
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-ARCH-001
- Literal rule enforcement: Rule 11 测试仍在 start.config，不恢复样例控制器。
- Implementation pseudocode:

```java
mockMvc.perform(get("/api/v1/product-search"))
    .andExpect(status().isOk());
mockMvc.perform(get("/v3/api-docs"))
    .andExpect(jsonPath("$.paths['/api/users']").doesNotExist())
    .andExpect(jsonPath("$.paths['/api/courses']").doesNotExist())
    .andExpect(jsonPath("$.paths['/api/v1/brands']").exists());
```

- Verification contribution: 兼容测试不再请求已删除路由。
- After this file: 公开搜索断言取代样例 POST。
#### File 54 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/start/config/NativeRpcConfigurationTest.java`

- Purpose: 不再要求 lightFacadeConverter Bean。
- Symbols: named_rpc_validation_reuses_the_existing_component_validator
- Repository evidence: 测试在上下文中取 lightFacadeConverter。校验 Bean 断言仍有价值。
- Dependencies and consumers: NativeRpcConfiguration 已删除该 Bean。
- Why now: 同一步必须让测试和配置一致。
- Contract/signature changes: 删除 getBean("lightFacadeConverter")。保留 nativeRpcValidation 断言。
- Input/output and state mapping: 四个 profile 的原生键断言保持。
- Error and edge behavior: 不恢复转换器来让旧断言通过。
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-ARCH-001
- Literal rule enforcement: Rule 4 只断言仍然存在的具名 Bean；Rule 11 不恢复 facade 样例。
- Implementation pseudocode:

```java
assertThat(context.getBean("nativeRpcValidation")).isInstanceOf(ValidationUtils.class);
assertThat(context.getBean("nativeRpcValidationExceptionMapper")).isInstanceOf(RpcProviderExceptionMapper.class);
assertThatThrownBy(() -> context.getBean("lightFacadeConverter")).isInstanceOf(NoSuchBeanDefinitionException.class);
```

- Verification contribution: 配置测试可编译。
- After this file: 上下文没有 LightFacadeConverter。
#### File 55 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/start/config/PlatformAuthenticationBoundaryContractTest.java`

- Purpose: 控制器名单去掉五个样例控制器。
- Symbols: filterChainsMatchTheSpecRoutes
- Repository evidence: Step 3 保留了含样例控制器的名单，避免那一步在样例尚未删除时失败。
- Dependencies and consumers: 样例控制器目录已经删除。
- Why now: 这是同一提交里的合同收尾。
- Contract/signature changes: containsExactlyInAnyOrder 只保留 ProductController、AdController、FileController、ProductSearchController、BrandController、SpecController。
- Input/output and state mapping: 不新增控制器。
- Error and edge behavior: 若磁盘上仍有第五个样例 @RestController，测试失败并回到删除目录。
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-ARCH-001
- Literal rule enforcement: Rule 11 删除样例纵向，不新建层，也不把样例类型留在磁盘上满足旧哈希或旧计数。
- Implementation pseudocode:

```java
assertThat(controllerSimpleNames("src/main/java")).containsExactlyInAnyOrder(
    "ProductController", "AdController", "FileController", "ProductSearchController",
    "BrandController", "SpecController");
```

- Verification contribution: 边界测试的控制器计数。
- After this file: 样例控制器不再被合同要求存在。

- Validation working directory: /Users/mario/SelfProject/enjoyshop
- Verification command: `mvn -pl enjoyshop-service/enjoyshop-service-goods -am -Dtest=ArchetypeContractConvergenceTest,NativeReadwriteContractTest,PlatformAuthenticationBoundaryContractTest,NativeRpcConfigurationTest,ProfileKeyParityTest,CdcContractTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: 合同改写后、目录删除前失败；本步全部文件完成后退出码 0。CdcContractTest 不要求修改。
- Failure returns to: ArchetypeContractConvergenceTest 与 NativeReadwriteContractTest 的新断言
- Completion criteria: 样例包和点名文件不存在；PO、DAO、Repository、DomainServiceImpl 计数为 6；CDC 配置文件字节未改；没有新的匿名样例路由。
- Rollback: 用本提交的父版本恢复样例树和两份合同。不需要数据库回滚。
- Commit paths: enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/architecture/ArchetypeContractConvergenceTest.java, enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/config/NativeReadwriteContractTest.java, enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/user, enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/teaching, enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/user, enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/teaching, enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/user, enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/teaching, enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/user, enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/teaching, enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/user, enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/teaching, enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/application/user, enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/application/teaching, enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/domain/user, enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/domain/teaching, enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/user, enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/teaching, enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/facade/user, enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/facade/teaching, enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/mq, enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/mq, enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/exception/UserUseCaseException.java, enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/exception/TeachingUseCaseException.java, enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/exception/UserDomainException.java, enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/exception/TeachingDomainException.java, enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/exception/UserFacadeException.java, enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/exception/TeachingFacadeException.java, enjoyshop-service/enjoyshop-service-goods/src/main/resources/graphql/user.graphqls, enjoyshop-service/enjoyshop-service-goods/src/main/resources/graphql/teaching.graphqls, enjoyshop-service/enjoyshop-service-goods/src/main/proto/teaching_user_facade.proto, enjoyshop-service/enjoyshop-service-goods/src/main/resources/mybatis/mapper/user, enjoyshop-service/enjoyshop-service-goods/src/main/resources/mybatis/mapper/teaching, enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/pojo/convertor/LightFacadeConverter.java, enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/config/ExternalClientConfig.java, enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/config/LocalAdapterConfiguration.java, enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/config/RabbitMqConfig.java, enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/config/TransactionCompletionExecutor.java, enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/config/RabbitMqConfigTest.java, enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/config/TransactionCompletionExecutorTest.java, enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/support/RecordingMqMessageService.java, enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/application/transaction/LocalTransactionBoundaryTest.java, enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/facade/NativeRpcMappingTest.java, enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/NativeRpcProviderTest.java, enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/facade/NativeRpcContractTest.java, enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/config/datasource/RepositoryPersistenceContractTest.java, enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/start/StudentManagementApplication.java, enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/start/config/NativeRpcConfiguration.java, enjoyshop-service/enjoyshop-service-goods/pom.xml, enjoyshop-service/enjoyshop-service-goods/src/main/resources/application.yml, enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-dev.yml, enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-prod.yml, enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/start/config/NativeHttpCompatibilityTest.java, enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/start/config/NativeRpcConfigurationTest.java, enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/start/config/PlatformAuthenticationBoundaryContractTest.java
- Commit: `refactor(goods): remove sample user and teaching vertical`

### Step 6 — 网页回到 Edge 登录并删除样例页面

- Requirements: REQ-005, REQ-006, REQ-007
- Dependencies: Step 5 已删除 `/api/users` 等样例接口。网页不再导入那些页面。
- Baseline state: `connection.js` 在 registration ID 为空时已经抛出“请先配置 BFF 模式和真实登录配置名称”并且不跳转。`api.js` 仍给品牌、规格和用户路径附加身份头。`app.js` 仍挂组织与教学。401 不改哈希。
- Observable outcome: 空 registration ID 不导航；BFF 业务 401 把哈希设为 `#/connection`；请求不再发送 `X-Tenant-Id`、`X-Actor-Id`、`X-Operator-Id`。旧样例哈希走现有未找到状态。
- End state: `config.js` 仍是 `apiBase` 空、`authMode` 为 `bff`、`loginRegistrationId` 空、`adPosition` 为 `web_index_lb`。浏览器不保存访问令牌。
- Test-first gate: Required。先改两份网页测试。RED 原因是实现仍会给 `/api/v1/brands` 发送身份头，401 也不改 `location.hash`。
- Manual Checks: MC-SCOPE-001, MC-TEST-001
- Literal Rules: Rule 11
- Ordered files:

#### File 1 — `MODIFY enjoyshop-web/test/api.test.mjs`

- Purpose: 先锁定 401 回跳，并停止用 `/api/users` 代表业务请求。
- Symbols: api.test.mjs
- Repository evidence: 第 26 行请求 `/api/users/12`，第 64 行只断言 401 的错误文案，不检查哈希。
- Dependencies and consumers: 导入 `createApi`。没有生产页面依赖测试。
- Why now: 实现还没改，新断言会失败。
- Contract/signature changes: 业务路径改为仍存在的 `/api/v1/brands`。401 用例在 BFF 模式且当前哈希不是 `#/connection` 时期望哈希被设置。
- Input/output and state mapping: 不写入 localStorage 或 sessionStorage。Bearer 模式仍可带 Authorization。
- Error and edge behavior: 已经位于 `#/connection` 时，401 不再次赋值。空 registration 的导航断言放在下一份测试。
- Standards impact: MC-TEST-001, MC-SCOPE-001
- Literal rule enforcement: Rule 11 测试仍是无构建步骤的 Node 脚本，不引入前端框架。
- Implementation pseudocode:

```javascript
const hash = { value: '#/products' };
const client = createApi({ mode: 'bff', fetcher: async (url) => {
  if (String(url).endsWith('/api/v1/brands')) return jsonResponse(401, { code: 'AUTHORIZATION_HEADER_INVALID', message: 'Tianquan-Shoubing authentication failed' });
  return jsonResponse(200, { headerName: 'X-CSRF', token: 'csrf' });
}, locationHash: hash });
await assert.rejects(client.request('/api/v1/brands'));
assert.equal(hash.value, '#/connection');
```

- Verification contribution: 本步 `npm test` 的 RED。
- After this file: 测试期望回跳，`api.js` 尚未实现。

#### File 2 — `MODIFY enjoyshop-web/test/contracts.test.mjs`

- Purpose: 去掉对已删除页面和身份头的依赖。
- Symbols: contracts.test.mjs
- Repository evidence: 文件导入 `scheduleBody` 与 `permissionTree`，并向 `/api/users` POST，断言 `X-Tenant-Id` 与 `X-Operator-Id`。
- Dependencies and consumers: 只验证 `createApi` 与剩余页面导出。
- Why now: `users.js` 与 `teaching.js` 还在，所以导入仍能成功；新断言要求品牌请求没有身份头，当前实现会失败。
- Contract/signature changes: 删除两个样例导入。POST 名单不再包含 `/api/users`。
- Input/output and state mapping: `/api/v1/brands`、`/api/v1/products`、`/api/v1/ads` 的写请求都不带三个身份头。CSRF 仍先请求 `/bff/csrf`。
- Error and edge behavior: 空 registration ID 调用登录动作时抛出原中文错误，并且 `location.assign` 不被调用。
- Standards impact: MC-TEST-001, MC-SCOPE-001
- Literal rule enforcement: Rule 11 不新增页面框架。
- Implementation pseudocode:

```javascript
const calls = [];
const client = createApi({ mode: 'bearer', token: 'dev-token', legacyTenantId: '2', legacyActorId: '3', fetcher: async (url, init) => {
  calls.push({ url, headers: init.headers });
  return jsonResponse(200, { success: true, data: {} });
}});
await client.request('/api/v1/brands', { method: 'POST', body: {} });
assert.equal(calls.at(-1).headers['X-Tenant-Id'], undefined);
assert.equal(calls.at(-1).headers['X-Actor-Id'], undefined);
assert.equal(calls.at(-1).headers['X-Operator-Id'], undefined);
assert.equal(calls.at(-1).headers.Authorization, 'Bearer dev-token');
```

- Verification contribution: 身份头回归被锁住。
- After this file: 合同测试不再导入样例页面。

#### File 3 — `MODIFY enjoyshop-web/src/api.js`

- Purpose: BFF 业务 401 回到登录页，并停止发送身份头。
- Symbols: createApi, request
- Repository evidence: 第 71 到 77 行对品牌、规格、用户路径写入三个身份头。401 只进入 ApiError。
- Dependencies and consumers: `connection.js` 与各业务页调用 `api`。`config.js` 不改。
- Why now: 测试已经要求新行为。
- Contract/signature changes: 删除 legacy 头分支。401 且 mode 为 bff、哈希不是 `#/connection` 时设置哈希。
- Input/output and state mapping: Bearer 模式保留 Authorization。令牌仍只在内存变量 `bearer`。
- Error and edge behavior: `/bff/csrf` 自己的 401 也回到登录页。写请求仍先取 CSRF。网络失败不改哈希。
- Standards impact: MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持静态页面和哈希路由，不引入新的前端层。
- Implementation pseudocode:

```javascript
export function createApi({ base = '', mode = 'bff', token = '', fetcher = globalThis.fetch, locationHash = globalThis.location } = {}) {
  async function request(path, { method = 'GET', query, body } = {}) {
    const headers = { Accept: 'application/json' };
    if (mode === 'bearer' && bearer) headers.Authorization = `Bearer ${bearer}`;
    if (mode === 'bff' && method !== 'GET') {
      const csrf = await request('/bff/csrf');
      headers[csrf.headerName] = csrf.token;
    }
    const response = await fetcher(`${base}${path}${queryString(query)}`, { method, headers, credentials: mode === 'bff' ? 'include' : 'omit' });
    if (response.status === 401 && mode === 'bff' && locationHash.hash !== '#/connection') locationHash.hash = '#/connection';
    if (!response.ok) throw new ApiError(message, response.status, result);
    return result;
  }
}
```

- Verification contribution: api.test.mjs 的哈希断言。
- After this file: 业务 401 会离开当前哈希；身份头不再发送。

#### File 4 — `MODIFY enjoyshop-web/src/app.js`

- Purpose: 删除组织与教学导航和四个样例路由。
- Symbols: renderUsers, renderPermissions, renderCourses, renderClasses
- Repository evidence: 第 7 到 8 行导入样例页面，第 14 行是“组织与教学”，第 20 行注册四个哈希。
- Dependencies and consumers: 哈希路由是 `location.hash.replace(/^#\\//,'').split('?')[0]`。未知哈希已有未找到状态。
- Why now: 页面文件即将删除，导入必须先去掉。
- Contract/signature changes: 删除四个路由键和整组导航。
- Input/output and state mapping: `#/users`、`#/permissions`、`#/courses`、`#/classes` 落入现有未找到分支。
- Error and edge behavior: 品牌、规格、上传、商品、广告、搜索路由保留。
- Standards impact: MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 不引入路由框架。
- Implementation pseudocode:

```javascript
const groups = [
  ['商品与内容', [['products', '商品'], ['brands', '品牌与规格'], ['ads', '广告'], ['search', '搜索']]],
];
const routes = { overview: renderOverview, connection: renderConnection, products: renderProducts, brands: renderBrands, ads: renderAds, search: renderSearch };
```

- Verification contribution: 页面测试不再解析已删除的导出名。
- After this file: 导航没有组织与教学。

#### File 5 — `MODIFY enjoyshop-web/src/pages/connection.js`

- Purpose: 概览去掉样例入口，连接表单去掉旧租户和操作人字段。
- Symbols: renderOverview, renderConnection
- Repository evidence: 第 10 到 11 行链到 users 和 classes。第 25 到 26 行是 legacyTenantId 与 legacyActorId。
- Dependencies and consumers: 登录按钮已调用 `location.assign`，空 ID 已抛错。
- Why now: 与 api.js 的身份头删除保持同一页面语义。
- Contract/signature changes: 不新增登录表单，不改 `/oauth2/authorization/` 路径。
- Input/output and state mapping: 表单仍提交 base、mode、registration、token。token 仍不落本地存储。
- Error and edge behavior: mode 不是 bff 或 registration 空白时抛原错误并且不调用 assign。
- Standards impact: MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 页面仍是普通函数，不新增框架组件。
- Implementation pseudocode:

```javascript
const entries = [
  ['products', '商品管理'],
  ['brands', '品牌与规格'],
  ['ads', '广告运营'],
  ['search', '商城搜索'],
];
if (connection.mode !== 'bff' || !connection.registration.trim()) {
  throw new Error('请先配置 BFF 模式和真实登录配置名称');
}
location.assign(`${connection.base}/oauth2/authorization/${segment(connection.registration)}`);
```

- Verification contribution: 空 registration 的合同断言。
- After this file: 概览和连接页不再指向样例能力。

#### File 6 — `DELETE enjoyshop-web/src/pages/users.js`

- Purpose: 删除用户和权限样例页。
- Symbols: renderUsers, renderPermissions, permissionTree
- Repository evidence: app.js 与 contracts.test.mjs 是仅有的导入方，上两个文件已去掉导入。
- Dependencies and consumers: 无剩余导入。
- Why now: 导入已经断开。
- Contract/signature changes: 文件删除。
- Input/output and state mapping: 没有替换页面。
- Error and edge behavior: 旧哈希由 app.js 的未找到状态处理。
- Standards impact: MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 不把样例页改造成新的权限管理页。
- Implementation pseudocode:

```text
delete enjoyshop-web/src/pages/users.js
rg "pages/users.js|renderUsers|permissionTree" enjoyshop-web/src enjoyshop-web/test
expect no remaining import of the deleted user sample page
```

- Verification contribution: npm test 能解析全部导入。
- After this file: 用户样例页不存在。

#### File 7 — `DELETE enjoyshop-web/src/pages/teaching.js`

- Purpose: 删除课程和班级样例页。
- Symbols: renderCourses, renderClasses, scheduleBody
- Repository evidence: 只被已修改的 app.js 和 contracts.test.mjs 导入。
- Dependencies and consumers: 无剩余导入。
- Why now: 与 users.js 同一导航组。
- Contract/signature changes: 文件删除。
- Input/output and state mapping: 没有替换页面。
- Error and edge behavior: `#/courses` 与 `#/classes` 显示未找到。
- Standards impact: MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 不保留教学样例页。
- Implementation pseudocode:

```text
delete enjoyshop-web/src/pages/teaching.js
rg "pages/teaching.js|renderCourses|scheduleBody" enjoyshop-web/src enjoyshop-web/test
expect no remaining import of the deleted teaching sample page
```

- Verification contribution: npm test 不再解析 teaching.js。
- After this file: 教学样例页不存在。

- Validation working directory: /Users/mario/SelfProject/enjoyshop/enjoyshop-web
- Verification command: `npm test`
- Expected result: 两份测试先改时失败；七个文件都完成后退出码 0。
- Failure returns to: enjoyshop-web/test/api.test.mjs 的 401 哈希断言
- Completion criteria: 空 registration 不导航，401 进入 `#/connection`，三个身份头消失，样例页面和导航消失。
- Rollback: 恢复 app.js、api.js、connection.js、两份测试和两个页面文件。
- Commit paths: enjoyshop-web/test/api.test.mjs, enjoyshop-web/test/contracts.test.mjs, enjoyshop-web/src/api.js, enjoyshop-web/src/app.js, enjoyshop-web/src/pages/connection.js, enjoyshop-web/src/pages/users.js, enjoyshop-web/src/pages/teaching.js
- Commit: `feat(web): return catalog sessions to edge login`

## 8. Test, Validation, and Quality Gates

| Gate | Working directory | Command | Expected | 失败时回到 |
| --- | --- | --- | --- | --- |
| Step 1 RED/GREEN | 仓库根 | `mvn -pl enjoyshop-service/enjoyshop-service-goods -am -Dtest=BrandAuthorizationComponentTest,SpecAuthorizationComponentTest,FileAuthorizationComponentTest -Dsurefire.failIfNoSpecifiedTests=false test` | 先类加载失败，后退出码 0 | 三个组件测试 |
| Step 2 RED/GREEN | 仓库根 | `mvn -pl enjoyshop-service/enjoyshop-service-goods -am -Dtest=BrandManageImplTest,SpecManageImplTest,FileManageImplTest -Dsurefire.failIfNoSpecifiedTests=false test` | 先 verify 失败，后退出码 0 | denialDoesNotCallDomain |
| Step 3 RED/GREEN | 仓库根 | `mvn -pl enjoyshop-service/enjoyshop-service-goods -am -Dtest=PlatformAuthenticationBoundaryContractTest -Dsurefire.failIfNoSpecifiedTests=false test` | 先缺方法，后退出码 0 | 边界测试 |
| Step 4 RED/GREEN | 仓库根 | `mvn -pl enjoyshop-service/enjoyshop-service-goods -am -Dtest=BrandOpenApiContractTest,SpecOpenApiContractTest,FileOpenApiContractTest -Dsurefire.failIfNoSpecifiedTests=false test` | 先缺 bearerAuth，后退出码 0 | 上传合同 |
| Step 5 RED/GREEN | 仓库根 | `mvn -pl enjoyshop-service/enjoyshop-service-goods -am -Dtest=ArchetypeContractConvergenceTest,NativeReadwriteContractTest,CdcContractTest,ProfileKeyParityTest -Dsurefire.failIfNoSpecifiedTests=false test` | 先计数或存在性失败，后退出码 0 | 两份架构合同 |
| Step 6 RED/GREEN | enjoyshop-web | `npm test` | 先哈希或身份头失败，后退出码 0 | api.test.mjs |
| 模块回归 | 仓库根 | `mvn -pl enjoyshop-service/enjoyshop-service-goods -am test` | 退出码 0。不启动真实守兵、Redis 或对象存储 | 第一个失败的测试类 |
| 静态差异 | 仓库根 | `git diff --check` | 无空白错误 | 当前步 |

没有现有命令覆盖不到的门禁，因此不新增 `scripts/work` 临时脚本。

## 9. Migration, Compatibility, Rollout, and Rollback

没有数据库迁移，没有新的 SQL 版本，也没有 SHA-256 清单条目。样例表留在已应用的分片合同里。

兼容变化是规格要求的：匿名品牌和规格请求变为 401；只有守兵身份、没有 `enjoyshop:file:write` 的上传被拒绝；样例 URL 不再注册。商品、广告、公开搜索和公开广告位的客户端不变。

上线前由平台管理员授予 `enjoyshop:brand:read`、`enjoyshop:brand:write`、`enjoyshop:spec:read`、`enjoyshop:file:write`。goods 不内置放行。部署时把真实 Edge registration ID 写入网页配置，仓库默认值保持空字符串。守兵 `register-filter` 默认仍是 false。

回滚按六个提交逆序恢复。不需要数据库回滚。平台里已经授予的权限码可以留下。

## 10. Requirement-to-Step Traceability Matrix

| Requirement | Steps | 验收证据 |
| --- | --- | --- |
| REQ-001 | 1, 2, 3, 4 | 品牌读权限、610111、610112、品牌链 |
| REQ-002 | 1, 2, 4 | 三个写方法在领域前检查 write |
| REQ-003 | 1, 2, 3, 4 | 规格链与 610113、610114 |
| REQ-004 | 1, 2, 3, 4 | 上传链两个过滤器，拒绝时不写对象 |
| REQ-005 | 5, 6 | 样例类型和页面不存在，架构合同已改 |
| REQ-006 | 6 | 空 registration 不跳转，401 进入 `#/connection` |
| REQ-007 | 2, 3, 6 | 过滤器跳过三条路径，网页不发送身份头 |
| REQ-008 | 3 | 商品、广告、公开搜索、公开广告位断言保持 |

## 11. Risks, Blockers, and User Decisions

| ID | 风险 | 影响 | 处理 | 负责人 | 状态 |
| --- | --- | --- | --- | --- | --- |
| RISK-001 | 平台尚未授予四个新权限码 | 已登录运营会收到拒绝 | 上线前由平台授予；goods 不内置允许 | 平台管理员 | Tracked |
| RISK-002 | 架构测试依赖样例包 | 构建失败 | DEC-007 已改写合同 | mario | Closed |
| RISK-003 | 部署忘记填写 registration ID | 登录按钮不能跳转 | 页面提示现有中文错误；仓库不写假 ID | 部署负责人 | Tracked |
| PLAN-CLAR-001 | Manage 的 @Valid 顺序 | 只影响校验与权限的先后 | 按商品实现改成 ValidationUtils | 实施者 | Recorded |
| PLAN-CLAR-002 | DomainServiceImpl 计数基线不一致 | 只影响合同期望值 | 删除后断言 6 | 实施者 | Recorded |
| PLAN-CLAR-003 | 样例 Rabbit 开关仍留在 YAML | 没有队列 Bean | 保留 enabled 与 exchange，不改 CDC | 实施者 | Recorded |

没有需要用户再选择的设计分叉。本计划不开始实施。

## 12. Review and Acceptance

### 12.1 Original-request fidelity

用户要求补权限、接入网页登录，并接受规格。样例接口按后续选择整段删除，而不是加一套教学权限。公开读和商品、广告授权没有被扩大。

### 12.2 Spec consistency

权限码、资源名、错误码、过滤链顺序和 DEC-007 的删除清单都来自已接受规格。计划没有新增 URL、表或登录端点。

### 12.3 Repository executability

文件路径、Bean 名、测试类和 Maven 命令都对照当前 `main` / `17cfa79` 工作区。CDC 与样例 Rabbit 配置的分离已经读过源码。

### 12.4 Coverage and release safety

每个行为步都先写会失败的测试。提交路径限定在该步文件。没有 DDL 和依赖变更。

### 12.5 Blocking Manual Check

| Check ID | Applicability | Status | Evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- |
| MC-ARCH-001 | Applicable | PASS | §4.7 选定现有 Light 剖面 | 新类进入 application.goods，样例删除不新建层 | 无额外动作 |
| MC-REUSE-001 | Applicable | PASS | §4.7 复用 AuthorizationService 和两个过滤器 | 没有新的登录或权限库 | 无额外动作 |
| MC-DEP-001 | Applicable | PASS | Step 5 只删除一个 MapStruct 包名 | 不新增 Maven 依赖 | 无额外动作 |
| MC-NAME-001 | Not applicable | N/A | 规格 §10 不新增 POJO | 组件沿用 AuthorizationComponent 后缀 | 无额外动作 |
| MC-VALID-001 | Applicable | PASS | PLAN-CLAR-001 与 Step 2 | 权限之后调用已有 ValidationUtils | 无额外动作 |
| MC-MODEL-001 | Not applicable | N/A | 无新 class 载体或 record | 不选择新的 Lombok 构造注解 | 无额外动作 |
| MC-CONVERT-001 | Not applicable | N/A | 无新转换器 | 现有 MapStruct 转换器保持 | 无额外动作 |
| MC-LOG-001 | Applicable | PASS | 三个组件使用 @Slf4j | 日志不记录令牌 | 无额外动作 |
| MC-BEAN-001 | Applicable | PASS | 具名 Component、构造器和 Qualifier | 与商品组件相同 | 无额外动作 |
| MC-UTIL-001 | Not applicable | N/A | 无新工具类 | 不引入白名单外的库 | 无额外动作 |
| MC-JSON-001 | Applicable | PASS | GoodsErrorStatus 与原成功 JSON | 不用 ordinal 做错误码 | 无额外动作 |
| MC-TIME-001 | Not applicable | N/A | 无新时间字段 | 不引入 java.util 日期 | 无额外动作 |
| MC-CONFIG-001 | Applicable | PASS | DEC-007 与 Step 5 的三份 YAML | 只删除样例基址，守兵默认 false 保持 | 无额外动作 |
| MC-PATTERN-001 | Applicable | PASS | §13 与三个同形组件 | 没有登录策略分支 | 无额外动作 |
| MC-SCOPE-001 | Applicable | PASS | REQ-008 与 CDC 排除 | 不改商品、广告、公开读和 CDC | 无额外动作 |
| MC-TEST-001 | Applicable | PASS | 六步都有 RED 命令 | 新拒绝、链和样例删除都有断言 | 无额外动作 |
| MC-BLOCKER-001 | Applicable | PASS | §11 没有未决设计选择 | 剩余事项是上线授予权限和部署 ID | 无额外动作 |

### 12.6 Final verdict

PASS — Ready for user review
