# 购物车实现计划

| Field | Value |
| --- | --- |
| Document | `2026-10-13-14-52-shopping-cart-implementation.md` |
| Template Version | `5` |
| Status | `Review` |
| Created | `2026-10-13 14:52 CST` |
| Updated | `2026-10-13 15:10 CST` |
| Owner | mario |
| Repository | enjoyshop |
| Scope | `enjoyshop-service-cart` 的守兵与鉴身接入、样例清理、`cart_item` 与两条购物车 REST，以及 `enjoyshop-web` 的 `#/cart` |
| Source Requirement | 用户确认购物车规格并要求写实现计划，且明确说“注意权限接入”。购物车必须是独立模块，不能放进 goods |
| Baseline Revision | `e3ebe39` on main；工作区另有未提交的 `enjoyshop-service/pom.xml` 模块登记，以及未跟踪的 `enjoyshop-service/enjoyshop-service-cart`。本计划不改 goods 源码，不改平台源码 |
| Implements Spec | [独立购物车模块](../spec/2026-10-13-14-29-shopping-cart.md) |
| Spec Status | `Review` |
| Spec Revision | `Updated 2026-10-13 14:39 CST` |
| Effective Specs | [独立购物车模块](../spec/2026-10-13-14-29-shopping-cart.md) |
| Depends On Plans | `None` |
| Supersedes | `None` |
| Superseded By | `None` |
| Related Plans | [目录权限与 Edge 登录实现计划](2026-10-13-11-47-catalog-permission-and-edge-login.md) |

## 1. Summary

本计划实现 [独立购物车模块](../spec/2026-10-13-14-29-shopping-cart.md)。规格状态仍是 `Review`；用户已确认可以据此写计划，因此本计划状态是 `Review`，不是 `Ready`。

实现顺序把权限放在购物车业务之前：先在 cart POM 增加已经批准的守兵与鉴身 `5.4.1`，写出 `cartAuthorizationComponent` 和四个环境里的 `enjoyshop-cart` 配置；再安装 `cartSecurityFilterChain`，让守兵跑在鉴身前面。然后删除 Light 样例，再用 `scripts/egon-codegen.sh` 从新建的 `cart_item` 脚本生成目录类型。其后才写四个数量策略、商品详情客户端、Manage 编排、控制器和网页。

权限不新做登录，也不使用 `@PreAuthorize`、本地 JWT、请求头用户名或固定用户名 `itcast`。读权限是 `enjoyshop:cart:read`，写权限是 `enjoyshop:cart:write`，资源是 `enjoyshop.cart`，系统码是 `enjoyshop-cart`。所有者只取鉴身快照里的 USER 主体。缺 Bearer 由过滤器和入口点返回两字段 JSON 401。权限拒绝是 `610501` 的 `ResultRecord` 403。鉴身不可用是 `610502` 的 503。

共 9 个 Step，每步一个路径受限的提交。本任务只写计划，不写业务代码，不启动服务，不连接数据库，不执行生成器。

## 2. Target Spec and Effective Design

### 2.1 Primary target

- Path: [独立购物车模块](../spec/2026-10-13-14-29-shopping-cart.md)
- Status: `Review`
- Revision: `Updated 2026-10-13 14:39 CST`，基线 `e3ebe39`
- Approval evidence: 用户在本会话明确说规格已 confirmed，并要求 `/egon-coding-writing-plan`，同时强调权限接入。规格文件里的 Status 仍是 `Review`，所以计划不能标 `Ready`

### 2.2 Effective Spec set

| Role | Spec/link | Status/revision | Effective sections | Why included |
| --- | --- | --- | --- | --- |
| Primary | [独立购物车模块](../spec/2026-10-13-14-29-shopping-cart.md) | `Review`，`Updated 2026-10-13 14:39 CST` | 全文，尤其是 §4、§6 至 §16 | 唯一要被 Step 覆盖的需求集 |
| Dependency, cited only | [目录权限与 Edge 登录](../spec/2026-10-13-11-06-catalog-permission-and-edge-login.md) | `Accepted` | §5、§7、§9、§12 | 购物车规格已把守兵、鉴身、权限码和 Edge 回跳收成自己的 `REQ-002`、`REQ-007`、`REQ-009`。该规格不放入文首 Effective Specs，否则它自己的 goods 需求会被要求在本计划的 Step 里再实现一遍。目录权限计划同样只把自己列入文首 Effective Specs |

### 2.3 Superseded or excluded content

不实现匿名车、Cookie 车、登录合并、Redis 账本、`tb_order_item`、勾选、收藏、下架清理、固定优惠文案、结算页和 `getOrderInfo`。不新增 goods 接口，不修改 goods 源码，不修改 `/Users/mario/SelfProject/Egon-COLA/egon-cola-xingyuan/`。不新增 Event、外盒、Spring Statemachine、Feign、Eureka、Hystrix、Ribbon、jjwt、Thymeleaf 或本地 JWT。

## 3. Effective Requirements and Acceptance

| Requirement | Source Spec section | Effective statement | Observable acceptance | Implementation impact |
| --- | --- | --- | --- | --- |
| `REQ-001` | §4 | 购物车位于 `enjoyshop-service-cart`，不位于 goods | 控制器、表和 DAO 只在 cart 模块 | 不改 goods；生成器和控制器都在 cart |
| `REQ-002` | §4、§9.2 | 未登录不能使用购物车 | 无 Bearer 返回 401，且不写库 | `cartSecurityFilterChain` 与两字段入口点 |
| `REQ-003` | §4、§11 | PostgreSQL 是唯一权威存储 | 只读写 `cart_item`；业务不注入 RedisTemplate 或 RabbitTemplate | DDL、生成器和扫描测试 |
| `REQ-004` | §4、§9.1 | 只有增量命令和只读列表 | `POST /api/v1/cart/items` 与 `GET /api/v1/cart`；没有 GET 修改 | 改写生成出的 CRUD 控制器 |
| `REQ-005` | §4、§7.3 | 新行冻结单价和单重，旧行不重新取价 | 商品改价后旧行 `unitPrice` 不变 | `RecomputeStoredPriceStrategy` |
| `REQ-006` | §4、§10.6 | 结果数量不大于 0 时软删除；再加入是新行并重新取价 | `deleted_at` 非空；新行使用当时详情价 | `SoftDeleteStrategy` 与 `InsertFromSnapshotStrategy` |
| `REQ-007` | §4、§15 | 读写权限分离，所有者只来自身份 | GET 要 `enjoyshop:cart:read`，POST 要 `enjoyshop:cart:write` | `cartAuthorizationComponent` 在仓库访问之前调用 |
| `REQ-008` | §4、§7.3 | 新行价格只来自服务端商品详情 | 请求体无价格；goods 失败不插入；只转发 Authorization | `GoodsProductSnapshotClient` |
| `REQ-009` | §4、§12 | 现有网页提供购物车，并把绝对数量换成差额 | `#/cart` 可列表和加减；差额为 0 不请求；登录后回到允许的哈希 | `enjoyshop-web` |
| `REQ-010` | §4、§10 | 金额和重量使用整数，溢出即拒绝 | `Math.multiplyExact` 或 `Math.addExact` 溢出返回 `610506` | 重算策略 |
| `REQ-011` | §4、§11 | 同一有效 SKU 并发修改只保留一行 | 部分唯一索引；第二次冲突返回 `610507` | SQL 索引与 Manage 重试 |
| `REQ-020` | §4、§8.2 | 业务接受前删除未使用的 Light 样例 | 样例类型和样例脚本不再被业务引用 | Step 3 的删除与替换 |

## 4. Implementation Strategy and Dependency Order

### 4.1 Ordered strategy

权限先于样例清理，样例清理先于第一条购物车业务。这和目录权限计划把授权组件放在样例删除之前的顺序一致，也满足用户“注意权限接入”。

1. Step 1 增加两个已批准 starter，写出错误码和 `cartAuthorizationComponent`，并让四个 YAML 具备与 goods 同形的 `jianshen` / `shoubing` 键。此时还留着 `user-base-url`，因为那是 `EXAMPLE-013`，属于 Step 3。
2. Step 2 把现有 `permitAll` 链改成 `cartSecurityFilterChain`。守兵在鉴身之前。`register-filter: true` 时启动抛 `IllegalStateException`。401 体只有 `code` 和 `message`。
3. Step 3 删除 §8.2 的样例，改写会因此无法编译的架构测试，并去掉样例外部地址。
4. Step 4 由代理编写 `cart_item` 脚本，再用生成器 `plan` / `apply` 写出目录类型。不手写第二套 PO、DAO、Repository、领域服务、Manage 或控制器。
5. Step 5 到 Step 7 在生成类型上补策略、事务外 goods 调用、权限前置和两条公开 REST。
6. Step 8 只改现有网页。
7. Step 9 做清理扫描、配置键对比、部分唯一索引的 PostgreSQL 执行测试，以及时间列测试。

生成器 `renderModel` 对表 `cart_item` 的类型名是 `CartItemPO`、`CartItemDAO`、`CartItemRepository`、`CartItemBO`、`CartItemDomainService`、`CartItemManage`、`CartItemController`，转换器是 `CartItemPersistenceConverter`。规格 §8.2 里的 `CartController`、`CartManage` 和 `CartItemConverter` 若与 v2 plan 路径不同，以 v2 plan 为准，禁止再写一份同职责类型。公开 URL 和 `operationId` 仍按规格改到生成出的控制器上。

### 4.2 Test-first strategy

| Behavior | RED | Why it fails first | GREEN |
| --- | --- | --- | --- |
| 授权组件 | `CartAuthorizationComponentTest` | 类还不存在；随后断言 403、503 和忽略用户名 | `CartAuthorizationComponent` |
| 过滤器 401 | `CartAuthenticationBoundaryTest` | 当前链是 `anyRequest().permitAll()`，购物车 URL 不会是 401 | `cartSecurityFilterChain` |
| 样例消失 | 改写后的架构测试 | 删除前测试仍引用 `UserController` | 删除样例并改断言 |
| 四种数量 | `CartItemAdjustmentStrategyTest` | 四个策略类还不存在 | 策略与注册表 |
| 权限在写库之前 | `CartItemManageImplTest` | Manage 还没有在仓库调用前执行 `requirePermission` | `CartItemManageImpl` |
| 两条 REST | `CartHttpContractTest` | 生成出的控制器仍是普通 CRUD 路径 | 改写控制器和异常处理器 |
| 网页差额 | `test/cart.test.mjs` | `cart.js` 和回跳键还不存在 | 页面与 `api.js` |

生成器产物没有先写失败测试再手写类体。它们的证明是 v2 plan 的 ADD 清单和 `apply` 退出码 0。随后的行为测试再改这些类。

### 4.3 Sequential and parallel boundaries

| Step | Depends on | May run in parallel with | Must not overlap with | Reason |
| --- | --- | --- | --- | --- |
| Step 1 | None | None | Step 2 到 Step 9 | POM 和授权组件是后面过滤器的编译前提 |
| Step 2 | Step 1 | None | Step 3 | 安全配置在样例删除前先接上守兵和鉴身 |
| Step 3 | Step 2 | None | Step 4 | 样例 DAO 删除后才能把 MapperScan 指到 cart |
| Step 4 | Step 3 | None | Step 5 | 策略要调用生成出的领域服务 |
| Step 5 | Step 4 | None | Step 6 | Manage 选择策略 |
| Step 6 | Step 1 与 Step 5 | None | Step 7 | 控制器调用已经鉴权的 Manage |
| Step 7 | Step 2 与 Step 6 | None | Step 8 | 网页依赖稳定的 URL 和错误码 |
| Step 8 | Step 7 | None | Step 9 | 扫描测试要看到最终网页和后端 |
| Step 9 | Step 3、Step 4、Step 8 | None | None | 收口扫描、索引和时间列 |

没有两个 Step 可以同时改同一个文件。

### 4.4 Commit boundaries

每个 Step 一个语义提交，只包含该 Step 的 Commit paths。不提交 `.egon/`。不把 goods、平台仓库、目录权限规格或目录权限计划放进购物车提交。`enjoyshop-service/pom.xml` 里已有的模块登记保持不动，不在这些提交里重复修改。

### 4.5 Spec Simplicity and Implementation-necessity Audit

| Spec element | Spec necessity verdict/section | Current repository evidence | Direct/reuse alternative | Interaction/implementation cost | Plan decision |
| --- | --- | --- | --- | --- | --- |
| 独立 cart 模块 | Keep，§7.0 | 模块已由 light 5.4.1 生成 | 放进 goods | 多一个进程 | Implement |
| `cart_item` | Add，§11 | 模块没有该表 | Redis 哈希或订单行 | 一张表和两个部分索引 | Implement |
| `API-001` 与 `API-002` | Add，§9.1 | 没有购物车 URL | GET 修改或三个 CRUD 动词 | 两个契约 | Implement，并删掉生成器多出来的 CRUD 动词 |
| 商品详情客户端 | Add，§7.0 | `GET /api/v1/products/{spuId}` 已存在 | 浏览器提交价格 | 仅新行一次 HTTP | Implement |
| 四个策略 | Add，§13 | 商品模块把状态规则放到独立组件 | Manage 里四段分支 | 四个 Bean | Implement |
| 守兵加鉴身 | Keep，§7.2 与 §15 | goods 的 `productSecurityFilterChain` 与 `ProductAuthorizationComponent` | 新认证服务或 `@PreAuthorize` | 两个已批准 starter | Implement |
| `#/cart` | Add，§12 | `app.js` 没有 cart 路由 | 新 Thymeleaf 站点 | 一个哈希页 | Implement |
| 生成器 CRUD 命令 | 规格 §8.2 允许路径以 v2 plan 为准 | `GenerationScopeValidator` 会生成 Create、Update、Delete 命令 | 再手写一套同名类 | 公开面必须收成一个差额命令 | 生成后改写，不另建第二套目录类型 |

没有让调用方先取价格再原样提交的接口。价格、所有者和租户都由服务端决定。

### 4.6 Change-unit Dependency Matrix

| Change unit | Requirements | Proof/RED point | Compile/runtime prerequisites | Produces | Consumers/unblocks | Owning Step |
| --- | --- | --- | --- | --- | --- | --- |
| 授权组件与系统码 | `REQ-007` | `CartAuthorizationComponentTest` | 两个 starter | `cartAuthorizationComponent` | 过滤器与 Manage | Step 1 |
| 过滤器链 | `REQ-002` | `CartAuthenticationBoundaryTest` | Step 1 的 Bean | `cartSecurityFilterChain` | 控制器 401 | Step 2 |
| 样例清理 | `REQ-020` | 改写后的架构测试 | Step 2 已提交 | 样例文件消失 | 生成器输入不再含用户表 | Step 3 |
| `cart_item` 与目录类型 | `REQ-001` `REQ-003` | v2 plan ADD 清单 | Step 3 的 SQL 替换 | PO、DAO、Repository、领域服务、Manage、控制器 | 策略和编排 | Step 4 |
| 四种数量策略 | `REQ-005` `REQ-006` `REQ-010` | `CartItemAdjustmentStrategyTest` | 生成出的领域服务 | 四个策略与注册表 | Manage | Step 5 |
| goods 客户端与编排 | `REQ-004` `REQ-007` `REQ-008` `REQ-011` | `CartItemManageImplTest` | 授权组件和策略 | `cartManage.adjust` | 控制器 | Step 6 |
| 公开 REST 与 OpenAPI | `REQ-002` `REQ-004` | `CartHttpContractTest` | Manage 和过滤器 | `listCart`、`adjustCartItem`、`bearerAuth` | 网页 | Step 7 |
| 网页差额与回跳 | `REQ-009` | `test/cart.test.mjs` | 两个 URL | `#/cart` 与 `enjoyshop.returnHash` | 收口扫描 | Step 8 |
| 扫描、索引和时间 | `REQ-003` `REQ-011` `REQ-020` | `TEST-005` `TEST-011` `TEST-020` | 前面各步 | 部分唯一索引被 PostgreSQL 执行 | 无 | Step 9 |

### 4.7 Java, Spring, and Egon-COLA Implementation Standards

| Concern | Current repository evidence | Effective Spec decision | Planned implementation consequence | Owning Steps/checks |
| --- | --- | --- | --- | --- |
| Architecture profile | cart `pom.xml` 父 POM `egon-cola-archetypes-parent:5.4.1`，包 `top.egon.enjoyshop.cart` | §6.1 保持 Light | 不新建传统三层，不改父 POM | Step 3 至 Step 7；`MC-ARCH-001` |
| Reuse/capability | goods POM 已有守兵、鉴身 `5.4.1`、`RestClient` 所在的 web starter、MP starter、id starter | §6.1 只补两个 starter | cart POM 增加同版本两个依赖；商品调用用 `RestClient` | Step 1 与 Step 6；`MC-REUSE-001` `MC-DEP-001` |
| Naming/model/validation/conversion | goods 的 `ProductResult`、`@Valid`、`BaseConverter` | §10 | 生成类型保留后缀；差额命令用 class 和分组校验；转换器沿用生成类并确认 `BaseConverter` | Step 4 至 Step 7；`MC-NAME-001` `MC-VALID-001` `MC-MODEL-001` `MC-CONVERT-001` |
| Bean/logging/util/JSON/time/config | `ProductAuthorizationComponent` 的 `@Component("productAuthorizationComponent")` 与 `@Qualifier("authorizationService")` | §6.2 | `@Slf4j`、显式 Bean 名、`@Qualifier`；四个 YAML 键一致；时间按 §4.9 | Step 1、Step 2、Step 9；`MC-LOG-001` `MC-BEAN-001` `MC-JSON-001` `MC-TIME-001` `MC-CONFIG-001` |
| Business variation/pattern | 规格 §13 四种调整结果 | 策略加注册表 | `CartItemAdjustmentKind` 映射四个 Bean | Step 5；`MC-PATTERN-001` |

#### Capability reuse ledger

| Need | Candidates inspected | Exact evidence | Fit/gap | Decision | Added dependency/custom code | Owning Step/check |
| --- | --- | --- | --- | --- | --- | --- |
| 验签与权限决策 | Spring Security、goods 的两个天权 starter | goods `pom.xml` 第 113 到 120 行，版本 `5.4.1` | cart POM 还没有这两项 | 已批准，按 goods 坐标加入 | `egon-cola-tianquan-shoubing-starter` 与 `egon-cola-tianquan-jianshen-starter` `5.4.1` | Step 1；`MC-DEP-001` |
| 商品快照 | Feign、JDK HTTP、Spring `RestClient` | cart 已有 `spring-boot-starter-web` | 不需要 Feign | 复用 `RestClient` | 无新依赖 | Step 6；`MC-REUSE-001` |
| 发号 | `egon-cola-component-common-id-starter` | 生成 POM 已有 | 足够 | 复用 | 无 | Step 4；`MC-REUSE-001` |
| Redis 账本 | 生成 POM 的 Redis starter 与 `RedisConfig` | `RedisConfig` 只在缓存组件开启时提供 Redisson | 用户禁止把 Redis 当账本 | 不注入 RedisTemplate，不新增客户端 | 无 | Step 9；`MC-REUSE-001` |
| 登录 | 御衡 Edge 与 `connection.js` | 登录地址已是 `/oauth2/authorization/{registrationId}` | 足够 | 不新增登录依赖 | 无 | Step 8；`MC-REUSE-001` |
| 目录脚手架 | 手写 PO、MyBatis-Plus Generator、`egon-codegen.sh` | 生成器 README：`configVersion` 必须是 1，`backend-crud` 覆盖到控制器 | 手写会和生成器冲突 | 使用已验证生成器 | 不在业务 POM 加 FreeMarker | Step 4；`MC-DEP-001` |
| PostgreSQL 驱动 | `org.postgresql:postgresql` | cart `pom.xml` 已声明 | 部分索引测试可以加载该驱动 | 复用，不引入 Testcontainers | 无 | Step 9；`MC-DEP-001` |

### 4.8 User-mandated Java Rule Implementation Matrix

| Literal rule | Spec source | Repository evidence | Exact files and order | Pseudocode obligations | Validation gate | Steps | Status/blocker |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Rule 1 | §6.2 与 §10.1 | goods 的 `ProductResult`、`SkuDAO` | 生成类型和手写命令、策略、结果 | 类名以 PO、BO、DAO、Query、Command、Result、Repository 或明确组件后缀结尾 | `CartSourceContractTest` 扫描后缀 | Step 4、Step 5、Step 6 | PASS |
| Rule 2 | §6.2 与 §9.2 | goods 控制器 `@Valid` | `AdjustCartItemCommand`、控制器、领域入口 | `@NotBlank`、`@Pattern`、`@NotNull`、`@Min`、`@Max`、自定义非 0 约束；无电话号码 | `CartHttpContractTest` 拒绝 0 | Step 6、Step 7 | PASS |
| Rule 3 | §6.2 与 §10.3.1 | goods 使用 class 与 Lombok | PO、命令、结果、两个转换器 | class 使用规定 Lombok；转换器实现 `BaseConverter` 或 `BaseForwardConverter`；不把新实体写成 record | 编译与 `TEST-013` | Step 4、Step 6 | PASS |
| Rule 4 | §6.2 与 §7.3 | `ProductAuthorizationComponent` | 授权组件、策略、客户端、Manage、控制器 | `@Slf4j`、显式 Bean 名、`@RequiredArgsConstructor`、依赖上的 `@Qualifier` | 授权测试与上下文加载 | Step 1、Step 5、Step 6 | PASS |
| Rule 5 | §6.2 | JDK `Math` | 重算策略 | 只用 `Math.multiplyExact` 与 `Math.addExact` | 溢出测试 | Step 5 | PASS |
| Rule 6 | §6.2 与 §9.2 | `GoodsErrorStatus.getStatus` 返回 `name()` | `CartErrorStatus` | 状态文本用枚举名，不用 ordinal；JSON 走 Jackson | OpenAPI 与错误体测试 | Step 1、Step 7 | PASS |
| Rule 7 | §6.2 与 `EXAMPLE-013` | cart 四个 `application*.yml` | 四个 YAML | 新增键四个文件都有，值可以不同 | `CartProfileKeyParityTest` | Step 1、Step 3、Step 9 | PASS |
| Rule 9 | §6.2 与 §13 | 四种数量结果的外部调用不同 | 四个策略和注册表 | 禁止在 Manage 里写四套硬编码分支 | `CartItemAdjustmentStrategyTest` | Step 5 | PASS |
| Rule 10 | §6.3 | EgonModel 的 `create_time` 与 `deleted_at` | `CartItemPO` 与仓库写入 | `OffsetDateTime` 与 `LocalDateTime`；禁止 `java.util.Date` | `TEST-011` | Step 4、Step 9 | PASS |
| Rule 11 | §6.1 | Light 包树与 MP starter | 每个 Step 的仓库文件 | 保持 Light；有效行用 `deleted_at IS NULL`；命令和查询分离；没有 Event | 架构测试 | 每个 Step | PASS |

### 4.9 Time semantics and conversion contract

Time contract: Required

| Time ID | Fields/symbols | Semantics | Java/runtime type | Storage policy | Wire policy | Business zone source | Display zone source | Boundary / DST rules | Precision / Clock | Compatibility | Verification |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `TIME-001` | `CartItemPO.createTime`、`CartItemPO.updateTime`、列 `create_time`、`update_time` | Occurred instant | OffsetDateTime | UTC instant | Internal | Not applicable: 这两列只审计写入时刻，购物车不按日历计算 | Not applicable: `#/cart` 不渲染这两列 | Not applicable: 没有日历日期或夏令时业务规则 | PostgreSQL `timestamptz(6)`；`Clock.systemUTC()` 截断到微秒 | Not applicable: 新列，没有历史购物车行 | Step 9；`TEST-011` |
| `TIME-002` | `CartItemPO.deletedAt`、列 `deleted_at` | Occurred instant | LocalDateTime | Legacy UTC | Internal | Not applicable: 墓碑时刻不参与业务日历 | Not applicable: 有效行列表不返回该列，页面不渲染 | Not applicable: 空值表示有效行，没有夏令时换日规则 | `timestamp(6)` without time zone；把 `Clock.systemUTC()` 转成 UTC 墙钟并截断到微秒 | goods 的 EgonModel 已把该列当作 UTC 墙钟；购物车按同一映射转换 | Step 9；`TEST-011` |

## 5. Change File Tree

```text
MODIFY  enjoyshop-service/enjoyshop-service-cart/pom.xml
MODIFY  enjoyshop-service/enjoyshop-service-cart/src/main/resources/application.yml
MODIFY  enjoyshop-service/enjoyshop-service-cart/src/main/resources/application-dev.yml
MODIFY  enjoyshop-service/enjoyshop-service-cart/src/main/resources/application-prod.yml
MODIFY  enjoyshop-service/enjoyshop-service-cart/src/main/resources/application-test.yml
CREATE  CartErrorStatus / cartAuthorizationComponent / CartAuthenticationBoundaryTest
MODIFY  NativeHttpSecurityConfiguration
DELETE  规格 §8.2 EXAMPLE-001 到 EXAMPLE-005、EXAMPLE-011、EXAMPLE-012 的样例树
MODIFY  EXAMPLE-006 到 EXAMPLE-010、EXAMPLE-013 的替换文件
CREATE  V20261013_001__initialize_cart_schema.sql
MODIFY  repository-manifest.json
CREATE  codegen/cart-item.json
GENERATED  backend-crud 对 cart_item 的 22 个目录文件
CREATE  四个调整策略、GoodsProductSnapshotClient、AdjustCartItemCommand、CartResult
MODIFY  生成出的 Manage、领域服务、Repository XML、控制器、OpenApiConfig
CREATE  enjoyshop-web/src/pages/cart.js 与 test/cart.test.mjs
MODIFY  enjoyshop-web/src/app.js、api.js、connection.js、products.js、search.js
MODIFY  docs/egon/codegen/ddl-consumption-log.md
```

精确路径在第 7 章每个 File 标题里只出现一次。样例删除使用目录路径时，目录成员就是规格 §8.2 对应 `EXAMPLE-*` 代码块里的文件，不使用通配符扩大到 `RedisConfig` 或 `StudentManagementApplication`。

| Operation | Path | Current evidence/symbol | Final symbols/state | Responsibility | Step | Requirements | Validation owner |
| --- | --- | --- | --- | --- | --- | --- | --- |
| MODIFY | `enjoyshop-service/enjoyshop-service-cart/pom.xml` | 无天权 starter；有 `LightFacadeConverterImpl` 登记 | 两个 `5.4.1` starter；去掉该登记 | 依赖与样例 RPC | Step 1 与 Step 3 | `REQ-007` `REQ-020` | 组件测试与 `TEST-020` |
| CREATE | `.../common/enums/CartErrorStatus.java` | 不存在 | `610501` 到 `610507` 与 `610599` | 业务错误码 | Step 1 | `REQ-007` | 授权测试 |
| CREATE | `.../application/cart/CartAuthorizationComponent.java` | 不存在 | `cartAuthorizationComponent` | 读写真源快照 | Step 1 | `REQ-007` | `CartAuthorizationComponentTest` |
| MODIFY | 四个 `application*.yml` | 只有 `shoubing`，端口回退 `8080`，有样例 base-url | 补 `jianshen`，系统码 `enjoyshop-cart`，端口回退 `8081`；Step 3 再删样例地址 | 配置一致 | Step 1 与 Step 3 | `REQ-007` `REQ-020` | `TEST-016` `TEST-020` |
| MODIFY | `NativeHttpSecurityConfiguration.java` | `applicationSecurityFilterChain` 为 `permitAll` | 增加 `cartSecurityFilterChain` 与 `cartCorsConfigurationSource` | 401 入口 | Step 2 | `REQ-002` | `CartAuthenticationBoundaryTest` |
| DELETE | 规格 §8.2 样例目录和样例外文件 | `UserController`、`CourseController` 仍在 | 这些路径不存在 | 样例清理 | Step 3 | `REQ-020` | `TEST-020` |
| CREATE | `db/egon-mp/V20261013_001__initialize_cart_schema.sql` | 不存在 | `cart_item` 与两个部分索引 | 权威表 | Step 4 | `REQ-003` `REQ-011` | `TEST-005` |
| GENERATED | `CartItemPO` 到 `CartItemController` 共 22 个文件 | 不存在 | v2 plan 的 ADD 路径 | 初始目录类型 | Step 4 | `REQ-001` `REQ-003` | `apply` 退出码 0 |
| CREATE | 四个策略与注册表 | 不存在 | `cartItemAdjustmentStrategyRegistry` | 数量结果 | Step 5 | `REQ-005` `REQ-006` `REQ-010` | 策略测试 |
| CREATE | `GoodsProductSnapshotClient.java` | 不存在 | `goodsProductSnapshotClient` | 转发 Authorization | Step 6 | `REQ-008` | Manage 测试 |
| MODIFY | `CartItemManageImpl.java` | 生成后只有普通 CRUD | `cartManage` 在仓库前鉴权 | 编排 | Step 6 | `REQ-004` `REQ-007` `REQ-011` | `CartItemManageImplTest` |
| MODIFY | `CartItemController.java` | 生成后是 CRUD | `listCart` 与 `adjustCartItem` | 公开 REST | Step 7 | `REQ-004` | `CartHttpContractTest` |
| CREATE | `enjoyshop-web/src/pages/cart.js` | 不存在 | `renderCart` | 购物车页 | Step 8 | `REQ-009` | `TEST-014` |
| MODIFY | `enjoyshop-web/src/api.js` | 401 只改哈希 | 额外写入允许的 `enjoyshop.returnHash` | 回跳 | Step 8 | `REQ-009` | `TEST-015` |

## 6. Prerequisites, Constraints, and Plan Clarifications

### 6.1 Repository and worktree baseline

- 仓库指令：计划不改生产代码。执行阶段才按 Step 提交。
- 分支 `main`，提交 `e3ebe39`，领先 origin，未推送。
- 不要改这些已有脏文件：`docs/egon/spec/2026-10-13-10-15-platform-user-authentication-integration.md`、`docs/egon/plan/2026-10-13-11-47-catalog-permission-and-edge-login.md`、`docs/egon/spec/2026-10-13-11-06-catalog-permission-and-edge-login.md`。
- 允许在购物车规格里只补 `Related Plans`。
- `.gitignore` 忽略 `.egon/`。生成器状态不得提交。
- 不连接 PostgreSQL，不启动 cart、goods、Edge 或浏览器。

### 6.2 Build, test, and environment prerequisites

| Concern | Exact command/source | Required state | Validation boundary |
| --- | --- | --- | --- |
| Cart 测试 | 仓库根目录 `mvn -pl enjoyshop-service/enjoyshop-service-cart -am -Dtest=测试类 -Dsurefire.failIfNoSpecifiedTests=false test` | JDK 21 与现有 Maven | 模块测试，不启动对外服务 |
| 网页测试 | `enjoyshop-web` 目录 `npm test`，脚本是 `node --test test/*.test.mjs` | 现有 Node | 不打开浏览器 |
| 生成器 | `/Users/mario/SelfProject/Egon-COLA/scripts/egon-codegen.sh` | 调用前环境变量 `EGON_CODEGEN_CLASSPATH` 已指向本地 classpath | 缺失时退出码 7 且输出 `BLOCKED_TOOLING`；不得下载依赖 |
| PostgreSQL 索引测试 | 仅 Step 9，环境变量 `CART_TEST_JDBC_URL` 指向已经存在的库 | cart POM 已有 `org.postgresql:postgresql` | 本计划不启动数据库，不新增 Testcontainers |

### 6.3 Immutable constraints and approved decisions

- 父 POM 保持 `top.egon:egon-cola-archetypes-parent:5.4.1`，空 `relativePath`。
- 类名 `StudentManagementApplication` 保持不变。
- 成功体使用既有 `ResultRecord`，成功码 `10000`。过滤器失败体不是 `ResultRecord`。
- 不在平台发放 `enjoyshop:cart:read` 或 `enjoyshop:cart:write`。测试使用 goods 同款合成令牌。
- 购物车不检查 `enjoyshop:product:read`。goods 对详情返回 401 或 403 时，购物车返回 `610505` 并且不写库。
- `V20260913_001` 只有在从未写入 `ddl_history` 时才能按 `EXAMPLE-006` 替换。cart 模块目前没有已应用记录。一旦应用，禁止再改该文件。
- 数据库事务不包裹 goods HTTP。goods 最多调用一次，写入事务最多两次。隔离级别 `READ COMMITTED`。

### 6.4 Plan Clarifications

| ID | Small implementation inference | Repository evidence | Why semantics are unchanged | Impact if wrong |
| --- | --- | --- | --- | --- |
| `PLAN-CLAR-001` | 生成器配置 JSON 的 `configVersion` 是 `1`。v2 指的是 `plan` 写出的计划文件，不是把 JSON 写成 `2` | 生成器 README 与 `CodegenConfigBO` | 公开契约仍由规格决定 | 写成 `2` 会在校验时失败 |
| `PLAN-CLAR-002` | 目录类型使用生成器名字：`CartItemController`、`CartItemManage`、`CartItemPersistenceConverter`。Bean 名改成规格要求的 `cartManage`、`cartItemConverter` 只发生在这些类上，不另建同职责类 | `GenerationScopeValidator.renderModel` 对 `cart_item` 的 `stem + "BO"` 与 `stem + "Controller"` | 规格 §8.2 已写明路径冲突时以 v2 plan 为准 | 若再手写 `CartController`，会有两套入口 |
| `PLAN-CLAR-003` | Step 3 增加空的 `infrastructure.cart.dao` 包说明，供 MapperScan 在 DAO 生成前编译。生成器随后只新增 `CartItemDAO.java` | 当前 `@MapperScan` 指向即将删除的 user 与 teaching DAO | 扫描范围仍只有 cart DAO | 空包不会变成业务类型 |
| `PLAN-CLAR-004` | `ddl_history` 仍只留在替换后的 `V20260913_001`。新购物车脚本不第二遍创建该表 | `TEST-021` 点名初始化脚本必须含 `ddl_history` | 新脚本只建 `cart_item` | 重复建表会让首启失败 |
| `PLAN-CLAR-005` | 部分唯一索引测试使用已有 PostgreSQL 驱动和 `CART_TEST_JDBC_URL`。不新增 Testcontainers | cart `pom.xml` 已有 `org.postgresql:postgresql`；goods 的 `PersistenceTestSupport` 用 H2，不能证明部分索引 | 断言仍是真实执行 `CREATE UNIQUE INDEX ... WHERE deleted_at IS NULL` 后的冲突 | 没有该 URL 时 Step 9 失败，不能改成跳过 |
| `PLAN-CLAR-006` | goods 快照的 HTTP 类型是 `GoodsProductSnapshotClientImpl`，Bean 名仍是 `goodsProductSnapshotClient`；接口留在 Step 5，供插入策略在事务外调用 | 生成器不产生 goods 客户端；策略测试不能等待 HTTP 实现 | 公开 URL、错误码和冻结价格不变 | 若把 HTTP 调用放进 `@Transactional` 方法，会把商品请求包进数据库事务 |

## 7. Ordered File-by-file Implementation Steps

> 每一步都可以单独验证。生成器文件的伪代码是配置和命令，不是类体。

### Step 1 — 接入守兵与鉴身依赖，并让授权组件按快照拒绝

- Requirements: `REQ-007`
- Dependencies: `None`
- Baseline state: cart POM 没有天权 starter；`application.yml` 只有 `shoubing`，没有 `jianshen`；不存在 `CartAuthorizationComponent`。goods 已用同版本 starter 和 `ProductAuthorizationComponent`。
- Observable outcome: 合成身份缺少读或写权限、不是 USER、系统码不是 `enjoyshop-cart` 或 MDC 不一致时，组件抛出 `610501`；决策为空或 `INDETERMINATE` 时抛出 `610502`。组件方法不接收用户名参数。
- End state: 两个 starter、错误码、授权组件和四个环境的权限键已经存在。样例 `user-base-url` 仍在。过滤器链仍是下一步才改。
- Test-first gate: `Required — CartAuthorizationComponentTest 在组件类出现前不能编译；POM 必须先于测试，因为测试要引用鉴身的 AuthorizationService`
- Manual Checks: `MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-NAME-001, MC-VALID-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-CONFIG-001, MC-SCOPE-001, MC-TEST-001`
- Literal Rules: `Rule 1, Rule 2, Rule 4, Rule 6, Rule 7, Rule 11`
- Ordered files:

#### File 1 — `MODIFY enjoyshop-service/enjoyshop-service-cart/pom.xml`

- Purpose: 让后续测试能够编译鉴身与守兵类型。
- Symbols: `egon-cola-tianquan-shoubing-starter`, `egon-cola-tianquan-jianshen-starter`
- Repository evidence: goods `pom.xml` 第 113 到 120 行已经声明这两个构件，版本都是 `5.4.1`。cart POM 目前没有它们，也没有 jjwt。
- Dependencies and consumers: 父 BOM `5.4.1`；下一步的过滤器和本步的授权组件。
- Why now: 测试和组件都要 import 平台类型。这是已批准依赖，不是新的认证栈。
- Contract/signature changes: 只增加两个依赖。不改父 POM，不增加 Feign、jjwt、Eureka 或第二个 Redis 客户端。
- Input/output and state mapping: 无运行时状态。版本固定 `5.4.1`，groupId `top.egon`。
- Error and edge behavior: 若仓库解析不到这两个构件，停止本步，不改用本地 JWT。
- Standards impact: `MC-DEP-001`, `MC-REUSE-001`, `MC-SCOPE-001`, `MC-TEST-001`
- Literal rule enforcement: Rule 11 保持 Light 模块边界，不把依赖加到 goods。Rule 5 不新增工具库。
- Implementation pseudocode:

```xml
<dependency>
  <groupId>top.egon</groupId>
  <artifactId>egon-cola-tianquan-shoubing-starter</artifactId>
  <version>5.4.1</version>
</dependency>
<dependency>
  <groupId>top.egon</groupId>
  <artifactId>egon-cola-tianquan-jianshen-starter</artifactId>
  <version>5.4.1</version>
</dependency>
```

- Verification contribution: `mvn -pl enjoyshop-service/enjoyshop-service-cart -am -DskipTests dependency:tree` 能看到这两个 `5.4.1` 构件，且看不到新的 jjwt。
- After this file: 模块可以编译平台安全类型。授权组件尚未存在，所以行为测试仍是红的。

#### File 2 — `CREATE enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/application/cart/CartAuthorizationComponentTest.java`

- Purpose: 在生产类之前固定 403、503 和“所有者不来自请求头”。
- Symbols: `CartAuthorizationComponentTest.deniesWhenDecisionIsNotAllow`, `deniesWhenPrincipalIsNotUser`, `unavailableWhenDecisionIsNull`, `ignoresAnyCallerSuppliedUsername`
- Repository evidence: `ProductAuthorizationComponentTest` 用合成 `Rbac3ContextAuthentication` 和 mock `AuthorizationService`。
- Dependencies and consumers: 生产类 `cartAuthorizationComponent`；`@Qualifier("authorizationService")`。
- Why now: 这是 `REQ-007` 的失败测试。
- Contract/signature changes: 断言 `requirePermission("enjoyshop:cart:read")` 与 `requirePermission("enjoyshop:cart:write")`。
- Input/output and state mapping: 快照 `systemCode=enjoyshop-cart`、`principalType=USER`、租户与 subject 一致，MDC `tenantId` 与 `userId` 相同。决策 `ALLOW` 且版本一致时不抛。方法没有用户名参数，测试里另造一个 `"itcast"` 字符串也不能改变主体。
- Error and edge behavior: 匿名、系统码错误、租户不是正整数、主体空白、快照不一致、MDC 不一致、决策不是 `ALLOW`，都得到 `CommonException` 码 `610501`。决策 `null` 或 `INDETERMINATE` 得到 `610502`。资源参数必须是 `enjoyshop.cart`。
- Standards impact: `MC-BEAN-001`, `MC-VALID-001`, `MC-TEST-001`, `MC-SCOPE-001`
- Literal rule enforcement: Rule 4 要求测试构造的 Bean 名是 `cartAuthorizationComponent`。Rule 2 覆盖非法权限码模式。Rule 11 测试放在 Light 的 application 包。
- Implementation pseudocode:

```java
@Test
void deniesWhenSystemCodeIsNotCart() {
    Rbac3ContextAuthentication authentication = userAuthentication("enjoyshop-goods", "user-1");
    SecurityContextHolder.getContext().setAuthentication(authentication);
    CommonException error = assertThrows(CommonException.class,
            () -> component.requirePermission("enjoyshop:cart:read"));
    assertThat(error.getCode()).isEqualTo(610501);
    verify(authorization, never()).requirePermission(any());
}
```

- Verification contribution: 在 File 4 之前，该测试因缺少 `CartAuthorizationComponent` 而不能编译。
- After this file: 失败原因是缺少生产类，不是夹具或数据库。

#### File 3 — `CREATE enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/common/enums/CartErrorStatus.java`

- Purpose: 给出购物车业务码，供授权组件和以后的异常处理器使用。
- Symbols: `CART_FORBIDDEN`, `CART_AUTHORIZATION_UNAVAILABLE`, `CART_REQUEST_INVALID`, `CART_SKU_NOT_SELLABLE`, `CART_GOODS_UNAVAILABLE`, `CART_QUANTITY_OVERFLOW`, `CART_VERSION_CONFLICT`, `CART_INTERNAL_ERROR`
- Repository evidence: `GoodsErrorStatus` 实现 `ErrorStatus`，`getStatus()` 返回 `name()`。商品码止于 `610499`。
- Dependencies and consumers: `CartAuthorizationComponent` 与 Step 7 的 `BaseExceptionHandler`。
- Why now: 组件要抛稳定码，不能临时写裸数字而不命名。
- Contract/signature changes: `610501` 消息 `cart permission is denied`；`610502` 消息 `cart authorization is unavailable`；`610503` 消息 `cart request parameters are invalid`；`610504` `CART_SKU_NOT_SELLABLE`；`610505` `CART_GOODS_UNAVAILABLE`；`610506` `CART_QUANTITY_OVERFLOW`；`610507` `CART_VERSION_CONFLICT`；`610599` `CART_INTERNAL_ERROR`。
- Input/output and state mapping: 枚举名是对外 `status`，整数是 `code`。不用 ordinal。
- Error and edge behavior: 不复用商品码。未列出的数字不在本枚举里发明。
- Standards impact: `MC-JSON-001`, `MC-NAME-001`, `MC-SCOPE-001`, `MC-TEST-001`
- Literal rule enforcement: Rule 1 类名以状态枚举的业务含义结尾并实现既有 `ErrorStatus`。Rule 6 的 `getStatus` 返回 `name()`。Rule 11 放在 `common.enums`。
- Implementation pseudocode:

```java
@Getter
@AllArgsConstructor
public enum CartErrorStatus implements ErrorStatus {
    CART_FORBIDDEN(610501, "cart permission is denied"),
    CART_AUTHORIZATION_UNAVAILABLE(610502, "cart authorization is unavailable");
    private final int code;
    private final String message;
    @Override public String getStatus() { return name(); }
}
```

- Verification contribution: 授权测试断言异常码 `610501` 与 `610502` 等于这两个枚举的 `code`。
- After this file: 错误码可编译。组件仍未接上，测试仍红。

#### File 4 — `CREATE enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/application/cart/CartAuthorizationComponent.java`

- Purpose: 按 goods 的授权组件检查身份，但使用购物车系统码、资源和错误码。
- Symbols: `@Component("cartAuthorizationComponent")`, `requirePermission`
- Repository evidence: `ProductAuthorizationComponent` 第 20 到 68 行。它检查 `Rbac3ContextAuthentication`、租户、`PrincipalType.USER`、快照、MDC，再调用 `PermissionRequest.api`。
- Dependencies and consumers: `@Qualifier("authorizationService") AuthorizationService`。Step 6 的 Manage 是第一个业务调用者。
- Why now: File 2 的红灯已经固定行为。
- Contract/signature changes: `public void requirePermission(@NotBlank @Pattern(regexp = "enjoyshop:cart:(read|write)") String permissionCode)`。
- Input/output and state mapping: 允许时无返回值。主体是 `identity.subject()`，必须等于 `snapshot.identitySub()`。不读取 `HttpServletRequest`，不接受 header 用户名。
- Error and edge behavior: 未认证或匿名抛 `CommonException(401000, "UNAUTHORIZED", "Verified user authentication is required")`，这与商品组件一致，且只会发生在过滤器已经放过的调用。其余身份不匹配抛 `610501`。决策空或 `INDETERMINATE` 抛 `610502`。`ALLOW` 还要匹配租户、`rbac3UserId`、权限码、`authVersion` 和 `policyVersion`。
- Standards impact: `MC-BEAN-001`, `MC-LOG-001`, `MC-VALID-001`, `MC-SCOPE-001`, `MC-TEST-001`
- Literal rule enforcement: Rule 4 使用 `@Slf4j`、`@Validated`、`@RequiredArgsConstructor`、`@Qualifier("authorizationService")` 和 Bean 名 `cartAuthorizationComponent`。Rule 2 用 `@Pattern` 限制权限码。Rule 11 类留在 application 层，不访问 DAO。
- Implementation pseudocode:

```java
@Component("cartAuthorizationComponent")
@Validated @Slf4j @RequiredArgsConstructor
public class CartAuthorizationComponent {
    @Qualifier("authorizationService")
    private final AuthorizationService authorization;
    public void requirePermission(@NotBlank @Pattern(regexp = "enjoyshop:cart:(read|write)") String permissionCode) {
        Rbac3ContextAuthentication bound = currentUserOrThrow();
        if (!"enjoyshop-cart".equals(bound.context().snapshot().systemCode())) throw forbidden();
        Decision decision = authorization.requirePermission(PermissionRequest.api(permissionCode, "enjoyshop.cart")).decision();
        if (decision == null || decision == Decision.INDETERMINATE) throw unavailable();
        if (decision != Decision.ALLOW) throw forbidden();
    }
}
```

- Verification contribution: `CartAuthorizationComponentTest` 转为绿。伪代码里的决策比较必须补上商品组件里的租户、主体、MDC、围栏和版本相等检查，不能只比较枚举名。
- After this file: 单元测试通过。HTTP 401 还没有，因为过滤器链未改。

#### File 5 — `MODIFY enjoyshop-service/enjoyshop-service-cart/src/main/resources/application.yml`

- Purpose: 让默认环境具备鉴身系统码、购物车端口、goods 超时和 CORS 键。
- Symbols: `egon.cola.platform.tianquan.jianshen.system-code`, `app.goods.base-url`, `server.port`
- Repository evidence: 该文件第 57 行端口回退 `8080`；第 321 行只有 `shoubing`，没有 `jianshen`。goods 的 `application.yml` 第 425 到 454 行有完整 `jianshen` 与 `shoubing`。
- Dependencies and consumers: Step 2 读取 `register-filter` 与 `app.web.cors.allowed-origins`。
- Why now: 组件测试不依赖 YAML，但进程下一跳需要键已经存在。
- Contract/signature changes: `server.port` 与 `${server.port:8080}` 的回退改为 `8081`。新增 `jianshen.enabled=true`、`register-filter=false`、`system-code=enjoyshop-cart`，以及 goods 同形的 `runtime` 与 `authorization` 子键。`shoubing.register-filter` 保持 `false`。新增 `app.goods.base-url`、`connect-timeout=2s`、`read-timeout=2s` 和 `app.web.cors.allowed-origins`。
- Input/output and state mapping: 地址默认 `http://127.0.0.1:8080` 指 goods，不是购物车自己。Redis 地址只给鉴身运行时，不给购物车账本。
- Error and edge behavior: 不删除 `user-base-url`。两个 `register-filter` 都不得写成 `true`。
- Standards impact: `MC-CONFIG-001`, `MC-SCOPE-001`, `MC-TEST-001`
- Literal rule enforcement: Rule 7 要求另外三个环境文件拥有同一组键。Rule 11 配置仍属于 cart 模块。
- Implementation pseudocode:

```yaml
server:
  port: ${SERVER_PORT:8081}
app:
  goods:
    base-url: ${GOODS_BASE_URL:http://127.0.0.1:8080}
    connect-timeout: ${GOODS_CONNECT_TIMEOUT:2s}
    read-timeout: ${GOODS_READ_TIMEOUT:2s}
  web:
    cors:
      allowed-origins: ${CART_CORS_ALLOWED_ORIGINS:}
egon.cola.platform.tianquan.jianshen:
  enabled: true
  register-filter: false
  system-code: enjoyshop-cart
```

- Verification contribution: 本步结束时四个文件都能解析出 `enjoyshop-cart` 和 `register-filter: false`。
- After this file: 默认 YAML 已有鉴身树。样例 `user-base-url` 仍保留到 Step 3。

#### File 6 — `MODIFY enjoyshop-service/enjoyshop-service-cart/src/main/resources/application-dev.yml`

- Purpose: 让 dev 的键集合与默认文件一致。
- Symbols: 与 File 5 相同的键
- Repository evidence: `application-dev.yml` 有 `user-base-url` 和 `shoubing`，端口回退仍是 `8080`。
- Dependencies and consumers: Spring profile `dev`。
- Why now: Rule 7 不允许只改一个环境。
- Contract/signature changes: 新增键与 File 5 相同。值仍可用环境变量覆盖。
- Input/output and state mapping: `system-code` 的值必须是 `enjoyshop-cart`，不能改成 goods。
- Error and edge behavior: 不删除样例 base-url。
- Standards impact: `MC-CONFIG-001`, `MC-SCOPE-001`, `MC-TEST-001`
- Literal rule enforcement: Rule 7 键集合与 `application.yml` 对齐。Rule 11 不引入新的配置文件种类。
- Implementation pseudocode:

```yaml
server:
  port: ${SERVER_PORT:8081}
app.goods.base-url: ${GOODS_BASE_URL:http://127.0.0.1:8080}
app.goods.connect-timeout: ${GOODS_CONNECT_TIMEOUT:2s}
app.goods.read-timeout: ${GOODS_READ_TIMEOUT:2s}
app.web.cors.allowed-origins: ${CART_CORS_ALLOWED_ORIGINS:}
egon.cola.platform.tianquan.jianshen.system-code: enjoyshop-cart
egon.cola.platform.tianquan.jianshen.register-filter: false
egon.cola.platform.tianquan.shoubing.register-filter: false
```

- Verification contribution: dev 文件包含 `system-code: enjoyshop-cart`。
- After this file: dev 与默认文件的新增键一致。

#### File 7 — `MODIFY enjoyshop-service/enjoyshop-service-cart/src/main/resources/application-prod.yml`

- Purpose: 让 prod 的键集合与默认文件一致。
- Symbols: 与 File 5 相同的键
- Repository evidence: `application-prod.yml` 同样有样例 `user-base-url` 和仅有的 `shoubing`。
- Dependencies and consumers: Spring profile `prod`。
- Why now: 生产环境不能缺少系统码。
- Contract/signature changes: 键与 File 5 相同。生产地址通过环境变量注入，键本身不能省略。
- Input/output and state mapping: 超时默认仍是 2 秒。
- Error and edge behavior: 不把 `register-filter` 设为 `true`。
- Standards impact: `MC-CONFIG-001`, `MC-SCOPE-001`, `MC-TEST-001`
- Literal rule enforcement: Rule 7 保持四文件键一致。Rule 11 配置只服务 cart 进程。
- Implementation pseudocode:

```yaml
server.port: ${SERVER_PORT:8081}
app.goods.base-url: ${GOODS_BASE_URL:http://127.0.0.1:8080}
app.goods.connect-timeout: ${GOODS_CONNECT_TIMEOUT:2s}
app.goods.read-timeout: ${GOODS_READ_TIMEOUT:2s}
app.web.cors.allowed-origins: ${CART_CORS_ALLOWED_ORIGINS:}
egon.cola.platform.tianquan.jianshen.system-code: enjoyshop-cart
egon.cola.platform.tianquan.jianshen.register-filter: false
```

- Verification contribution: prod 文件的 `system-code` 是 `enjoyshop-cart`。
- After this file: prod 键已补齐。

#### File 8 — `MODIFY enjoyshop-service/enjoyshop-service-cart/src/main/resources/application-test.yml`

- Purpose: 让 test 的键集合与默认文件一致。
- Symbols: 与 File 5 相同的键
- Repository evidence: `application-test.yml` 的 `external-http` 目前只有 `enabled`，没有 `user-base-url`，也没有 `jianshen`。
- Dependencies and consumers: 测试 profile。Step 9 的键对比会读它。
- Why now: 测试环境缺键会让 Rule 7 失败。
- Contract/signature changes: 补上与 File 5 相同的键。不要为了暂时对齐而把 `user-base-url` 加回来。
- Input/output and state mapping: goods 地址默认仍指向 `127.0.0.1:8080`。
- Error and edge behavior: `register-filter` 为 `false`。
- Standards impact: `MC-CONFIG-001`, `MC-SCOPE-001`, `MC-TEST-001`
- Literal rule enforcement: Rule 7 四文件新增键一致。Rule 11 不新增第五份配置。
- Implementation pseudocode:

```yaml
server.port: ${SERVER_PORT:8081}
app.goods.base-url: ${GOODS_BASE_URL:http://127.0.0.1:8080}
app.goods.connect-timeout: ${GOODS_CONNECT_TIMEOUT:2s}
app.goods.read-timeout: ${GOODS_READ_TIMEOUT:2s}
app.web.cors.allowed-origins: ${CART_CORS_ALLOWED_ORIGINS:}
egon.cola.platform.tianquan.jianshen.system-code: enjoyshop-cart
egon.cola.platform.tianquan.jianshen.register-filter: false
egon.cola.platform.tianquan.shoubing.register-filter: false
```

- Verification contribution: 四个 YAML 都能找到 `enjoyshop-cart`、两处 `register-filter: false`、`app.goods.base-url` 和 CORS 键。
- After this file: 权限配置在四个环境对齐。样例地址的删除留在 Step 3。

- Validation working directory: 仓库根目录 `/Users/mario/SelfProject/enjoyshop`
- Verification command: `mvn -pl enjoyshop-service/enjoyshop-service-cart -am -Dtest=CartAuthorizationComponentTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: 退出码 0。`deniesWhenSystemCodeIsNotCart`、`deniesWhenPrincipalIsNotUser`、`unavailableWhenDecisionIsNull` 和 `ignoresAnyCallerSuppliedUsername` 通过。
- Failure returns to: File 4 的快照比较，或 File 1 的依赖坐标。
- Completion criteria: 测试证明所有者来自快照而不是请求头；四个 YAML 含 `system-code: enjoyshop-cart`；POM 只有这两个新增 starter。
- Rollback: 回退本步提交。样例文件和其他脏文档不动。
- Commit paths: `enjoyshop-service/enjoyshop-service-cart/pom.xml`, `enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/application/cart/CartAuthorizationComponentTest.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/common/enums/CartErrorStatus.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/application/cart/CartAuthorizationComponent.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/resources/application.yml`, `enjoyshop-service/enjoyshop-service-cart/src/main/resources/application-dev.yml`, `enjoyshop-service/enjoyshop-service-cart/src/main/resources/application-prod.yml`, `enjoyshop-service/enjoyshop-service-cart/src/main/resources/application-test.yml`
- Commit: `feat(cart): check cart permissions from the Jianshen snapshot`

### Step 2 — 安装购物车过滤器链并返回两字段 401

- Requirements: `REQ-002`
- Dependencies: `Step 1`
- Baseline state: `NativeHttpSecurityConfiguration` 只有 `applicationSecurityFilterChain`，并且 `anyRequest().permitAll()`。没有 CORS Bean。
- Observable outcome: `GET /api/v1/cart` 和 `POST /api/v1/cart/items` 在没有 Authorization 时返回 401，JSON 只有 `code=AUTHORIZATION_HEADER_INVALID` 和 `message=Tianquan-Shoubing authentication failed`，没有 `success`。`register-filter: true` 时上下文创建抛出 `IllegalStateException`。
- End state: 守兵过滤器位于鉴身过滤器之前。403 的 HTTP 映射要等控制器和异常处理器，留在 Step 7。
- Test-first gate: `Required — 当前 permitAll 链不会对购物车 URL 返回 401，测试先失败`
- Manual Checks: `MC-ARCH-001, MC-BEAN-001, MC-CONFIG-001, MC-JSON-001, MC-SCOPE-001, MC-TEST-001`
- Literal Rules: `Rule 4, Rule 6, Rule 7, Rule 11`
- Ordered files:

#### File 1 — `CREATE enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/start/config/CartAuthenticationBoundaryTest.java`

- Purpose: 固定缺 Bearer 的入口点，以及禁止 starter 自行注册过滤器。
- Symbols: `CartAuthenticationBoundaryTest.missingBearerIsTwoFieldUnauthorized`, `registerFilterTrueFailsStartup`
- Repository evidence: goods `NativeHttpSecurityConfiguration` 的入口点写 `Map.of("code", "AUTHORIZATION_HEADER_INVALID", "message", "Tianquan-Shoubing authentication failed")`，并且不在入口点设置 `Cache-Control`。
- Dependencies and consumers: `cartSecurityFilterChain`、`idpBearerAuthenticationFilter`、`rbac3BearerAuthenticationFilter`。
- Why now: 生产链还是 `permitAll`，这是正确的红灯。
- Contract/signature changes: 两个购物车 URL 都要 401。响应体不能出现 `success` 或令牌原文。
- Input/output and state mapping: 请求不带 Authorization。期望 HTTP 401 和两个 JSON 字段。
- Error and edge behavior: 另有一个用例把 `egon.cola.platform.tianquan.shoubing.register-filter` 设为 `true`，断言 `IllegalStateException` 消息包含 `cart SecurityFilterChain`。本步不断言 403，因为还没有控制器。
- Standards impact: `MC-JSON-001`, `MC-BEAN-001`, `MC-TEST-001`, `MC-SCOPE-001`
- Literal rule enforcement: Rule 6 断言 Jackson 写出的字段名是 `code` 与 `message`。Rule 11 测试放在现有 `start.config` 包。
- Implementation pseudocode:

```java
@Test
void missingBearerIsTwoFieldUnauthorized() throws Exception {
    mockMvc.perform(get("/api/v1/cart"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTHORIZATION_HEADER_INVALID"))
        .andExpect(jsonPath("$.message").value("Tianquan-Shoubing authentication failed"))
        .andExpect(jsonPath("$.success").doesNotExist());
}
```

- Verification contribution: 在 File 2 之前，状态不是 401。
- After this file: 测试因安全链尚未匹配购物车路径而失败。

#### File 2 — `MODIFY enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/start/config/NativeHttpSecurityConfiguration.java`

- Purpose: 用购物车专用链替换“所有请求匿名放行”对购物车 URL 的效果。
- Symbols: `cartSecurityFilterChain`, `cartCorsConfigurationSource`, `applicationSecurityFilterChain`
- Repository evidence: 当前类第 15 到 23 行只有 `permitAll`。goods 的 `productSecurityFilterChain` 使用 `securityMatcher`、`addFilterBefore(permissions, AnonymousAuthenticationFilter.class)` 和 `addFilterBefore(identity, Rbac3BearerAuthenticationFilter.class)`。
- Dependencies and consumers: Step 1 的两个 starter 与 `register-filter` 配置。Step 7 的控制器走这条链。
- Why now: File 1 要求 401。
- Contract/signature changes: Bean `cartSecurityFilterChain` 匹配 `/api/v1/cart` 与 `/api/v1/cart/**`，`anyRequest().authenticated()`。保留 `applicationSecurityFilterChain` 只放行非购物车路径，避免执行器被误伤。CORS Bean 名 `cartCorsConfigurationSource`，来源键 `app.web.cors.allowed-origins`，方法含 GET、POST、PUT、DELETE、OPTIONS，允许 Authorization 与 Content-Type，暴露 Location，`allowCredentials` 为 false。
- Input/output and state mapping: 入口点只写两个字段。过滤器自身的 `Cache-Control: no-store` 由平台 `IdpBearerAuthenticationFilter` 设置，入口点不另加。
- Error and edge behavior: 任一 `register-filter` 为 true 时抛 `IllegalStateException("Cart authentication filters must only run inside the cart SecurityFilterChain")`。不使用 `@PreAuthorize`。不解析 JWT。
- Standards impact: `MC-BEAN-001`, `MC-JSON-001`, `MC-CONFIG-001`, `MC-SCOPE-001`, `MC-TEST-001`
- Literal rule enforcement: Rule 4 给过滤器参数和 CORS Bean 加 `@Qualifier`。Rule 7 读取的配置键已在四个 YAML 中存在。Rule 11 安全配置留在 `start.config`。
- Implementation pseudocode:

```java
@Bean("cartSecurityFilterChain")
@Order(Ordered.LOWEST_PRECEDENCE - 1)
SecurityFilterChain cartSecurityFilterChain(HttpSecurity http,
        @Qualifier("cartCorsConfigurationSource") CorsConfigurationSource cors,
        @Qualifier("jacksonObjectMapper") ObjectMapper mapper,
        @Qualifier("idpBearerAuthenticationFilter") IdpBearerAuthenticationFilter identity,
        @Qualifier("rbac3BearerAuthenticationFilter") Rbac3BearerAuthenticationFilter permissions,
        @Value("${egon.cola.platform.tianquan.shoubing.register-filter:false}") boolean idpRegistration,
        @Value("${egon.cola.platform.tianquan.jianshen.register-filter:false}") boolean rbacRegistration) throws Exception {
    if (idpRegistration || rbacRegistration) {
        throw new IllegalStateException("Cart authentication filters must only run inside the cart SecurityFilterChain");
    }
    http.securityMatcher("/api/v1/cart", "/api/v1/cart/**");
    http.addFilterBefore(permissions, AnonymousAuthenticationFilter.class);
    http.addFilterBefore(identity, Rbac3BearerAuthenticationFilter.class);
    return http.authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated()).build();
}
```

- Verification contribution: `missingBearerIsTwoFieldUnauthorized` 变为绿。伪代码必须同时调用与 goods 相同的 `configureCommonSecurity` 入口点，不能只加过滤器而不写 401 体。
- After this file: 缺 Bearer 的购物车 URL 返回两字段 401。样例控制器仍可被后置的 `permitAll` 链看到，直到 Step 3 删除它们。

- Validation working directory: 仓库根目录 `/Users/mario/SelfProject/enjoyshop`
- Verification command: `mvn -pl enjoyshop-service/enjoyshop-service-cart -am -Dtest=CartAuthenticationBoundaryTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: 退出码 0。缺 Bearer 为 401 且没有 `success`；`register-filter=true` 抛出 `IllegalStateException`。
- Failure returns to: File 2 的 matcher、过滤器顺序或入口点 JSON。
- Completion criteria: 守兵在鉴身之前；401 体只有 `code` 和 `message`；没有新增认证依赖。
- Rollback: 回退本步提交，恢复 `permitAll` 单链。
- Commit paths: `enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/start/config/CartAuthenticationBoundaryTest.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/start/config/NativeHttpSecurityConfiguration.java`
- Commit: `feat(cart): authenticate cart routes with Shoubing before Jianshen`

### Step 3 — 在购物车业务之前删除 Light 样例

- Requirements: `REQ-020` `REQ-001`
- Dependencies: `Step 2`
- Baseline state: cart 模块仍含 `UserController`、`CourseController`、样例 SQL、GraphQL、proto 和 `user-base-url`。`cartSecurityFilterChain` 已经匹配购物车 URL。
- Observable outcome: 规格 `EXAMPLE-001` 到 `EXAMPLE-009` 以及 `EXAMPLE-011` 到 `EXAMPLE-013` 点名的样例文件不存在或已被替换；`RedisConfig`、`StudentManagementApplication` 和 `NativeHttpSecurityConfiguration` 仍在。
- End state: 模块不再引用用户或课程类型。`MapperScan` 只指向 `infrastructure.cart.dao`。购物车表和控制器尚未生成。
- Test-first gate: `Required — 先改架构测试，使它们在样例仍存在时失败；删除样例后转为通过`
- Manual Checks: `MC-ARCH-001, MC-SCOPE-001, MC-TEST-001, MC-CONFIG-001, MC-REUSE-001`
- Literal Rules: `Rule 7, Rule 11`
- Ordered files:

#### File 1 — `MODIFY enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/architecture/ArchetypeContractConvergenceTest.java`

- Purpose: 让架构测试不再要求用户或课程类型。
- Symbols: ArchetypeContractConvergenceTest
- Repository evidence: 规格 EXAMPLE-008。该测试目前读取样例类。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
assertThat(source).doesNotContain("UserController");
assertThat(source).doesNotContain("CourseController");
assertThat(Files.exists(Path.of("src/main/java/top/egon/enjoyshop/cart/infrastructure/config/RedisConfig.java"))).isTrue();
assertThat(Files.exists(Path.of("src/main/java/top/egon/enjoyshop/cart/start/config/NativeHttpSecurityConfiguration.java"))).isTrue();
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 2 — `MODIFY enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/architecture/LightPersistenceArchitectureTest.java`

- Purpose: 去掉对样例 Repository 的存在断言。
- Symbols: LightPersistenceArchitectureTest
- Repository evidence: EXAMPLE-008 点名该测试引用 CourseRepository。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
assertThat(mainJava).doesNotContain("interface CourseRepository");
assertThat(mainJava).doesNotContain("interface UserRepository");
assertThat(Path.of("src/main/java/top/egon/enjoyshop/cart/infrastructure/config/RedisConfig.java")).exists();
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 3 — `MODIFY enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/infrastructure/migration/FlywayMigrationConventionTest.java`

- Purpose: 不再要求样例分片脚本存在。
- Symbols: FlywayMigrationConventionTest
- Repository evidence: EXAMPLE-005 写明该测试点名分片样例文件。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
assertThat(Path.of("src/main/resources/db/migration/sharding")).doesNotExist();
assertThat(Files.readString(Path.of("src/main/resources/db/egon-mp/V20260913_001__initialize_repository_schema.sql"))).contains("ddl_history");
assertThat(Files.readString(Path.of("src/main/resources/db/egon-mp/V20260913_001__initialize_repository_schema.sql"))).doesNotContain("CREATE TABLE users");
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 4 — `MODIFY enjoyshop-service/enjoyshop-service-cart/src/test/resources/mybatis/h2-schema.sql`

- Purpose: 删掉 H2 里的用户和课程表。
- Symbols: h2-schema.sql
- Repository evidence: EXAMPLE-008 说 H2 脚本含用户表。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
-- cart boundary only
-- no CREATE TABLE users
-- no CREATE TABLE courses
-- cart_item is added by the PostgreSQL script in Step 4, not by this H2 file
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 5 — `MODIFY enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/application/transaction/LocalTransactionBoundaryTest.java`

- Purpose: 事务边界不再指向样例 Manage。
- Symbols: LocalTransactionBoundaryTest
- Repository evidence: EXAMPLE-008。购物车 Manage 要到 Step 4 才生成，本步只断言样例 Manage 已消失。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
assertThat(sourceTree).doesNotContain("class UserManageImpl");
assertThat(sourceTree).doesNotContain("class CourseManageImpl");
assertThat(Path.of("src/main/java/top/egon/enjoyshop/cart/application/cart/CartAuthorizationComponent.java")).exists();
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 6 — `MODIFY enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/start/config/NativeHttpCompatibilityTest.java`

- Purpose: HTTP 兼容断言改为购物车路径仍由安全配置保护。
- Symbols: NativeHttpCompatibilityTest
- Repository evidence: EXAMPLE-008 目前断言 `/api/users`。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
assertThat(securitySource).contains("cartSecurityFilterChain");
assertThat(securitySource).contains("/api/v1/cart");
assertThat(securitySource).doesNotContain("/api/users");
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 7 — `MODIFY enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/infrastructure/config/datasource/RepositoryPersistenceContractTest.java`

- Purpose: 持久化契约不再引用样例表。
- Symbols: RepositoryPersistenceContractTest
- Repository evidence: EXAMPLE-008。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
assertThat(sql).doesNotContain("CREATE TABLE users");
assertThat(sql).doesNotContain("CREATE TABLE courses");
assertThat(sql).contains("ddl_history");
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 8 — `DELETE enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/adapter/user`

- Purpose: 删除 EXAMPLE-001 在该目录下的全部样例文件。
- Symbols: UserController, PermissionController, RoleController
- Repository evidence: 规格 §8.2 EXAMPLE-001 列出该目录中的每一个文件。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
Delete every file named by EXAMPLE-001 under enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/adapter/user.
Do not delete RedisConfig, StudentManagementApplication, NativeHttpSecurityConfiguration, or CartAuthorizationComponent.
Leave no import of UserController anywhere in src/main/java.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 9 — `DELETE enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/adapter/teaching`

- Purpose: 删除 EXAMPLE-002 在该目录下的全部样例文件。
- Symbols: CourseController, SchoolClassController
- Repository evidence: 规格 §8.2 EXAMPLE-002 列出该目录中的每一个文件。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
Delete every file named by EXAMPLE-002 under enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/adapter/teaching.
Do not delete RedisConfig, StudentManagementApplication, NativeHttpSecurityConfiguration, or CartAuthorizationComponent.
Leave no import of CourseController anywhere in src/main/java.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 10 — `DELETE enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/application/user`

- Purpose: 删除 EXAMPLE-001 在该目录下的全部样例文件。
- Symbols: UserManage, PermissionManage, RoleManage
- Repository evidence: 规格 §8.2 EXAMPLE-001 列出该目录中的每一个文件。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
Delete every file named by EXAMPLE-001 under enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/application/user.
Do not delete RedisConfig, StudentManagementApplication, NativeHttpSecurityConfiguration, or CartAuthorizationComponent.
Leave no import of UserManage anywhere in src/main/java.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 11 — `DELETE enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/application/teaching`

- Purpose: 删除 EXAMPLE-002 在该目录下的全部样例文件。
- Symbols: CourseManage, SchoolClassManage
- Repository evidence: 规格 §8.2 EXAMPLE-002 列出该目录中的每一个文件。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
Delete every file named by EXAMPLE-002 under enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/application/teaching.
Do not delete RedisConfig, StudentManagementApplication, NativeHttpSecurityConfiguration, or CartAuthorizationComponent.
Leave no import of CourseManage anywhere in src/main/java.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 12 — `DELETE enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/domain/user`

- Purpose: 删除 EXAMPLE-001 在该目录下的全部样例文件。
- Symbols: UserAggregate, UserDomainService
- Repository evidence: 规格 §8.2 EXAMPLE-001 列出该目录中的每一个文件。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
Delete every file named by EXAMPLE-001 under enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/domain/user.
Do not delete RedisConfig, StudentManagementApplication, NativeHttpSecurityConfiguration, or CartAuthorizationComponent.
Leave no import of UserAggregate anywhere in src/main/java.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 13 — `DELETE enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/domain/teaching`

- Purpose: 删除 EXAMPLE-002 在该目录下的全部样例文件。
- Symbols: CourseAggregate, SchoolClassAggregate
- Repository evidence: 规格 §8.2 EXAMPLE-002 列出该目录中的每一个文件。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
Delete every file named by EXAMPLE-002 under enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/domain/teaching.
Do not delete RedisConfig, StudentManagementApplication, NativeHttpSecurityConfiguration, or CartAuthorizationComponent.
Leave no import of CourseAggregate anywhere in src/main/java.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 14 — `DELETE enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/infrastructure/user`

- Purpose: 删除 EXAMPLE-001 在该目录下的全部样例文件。
- Symbols: UserDAO, UserPO, UserRepository
- Repository evidence: 规格 §8.2 EXAMPLE-001 列出该目录中的每一个文件。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
Delete every file named by EXAMPLE-001 under enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/infrastructure/user.
Do not delete RedisConfig, StudentManagementApplication, NativeHttpSecurityConfiguration, or CartAuthorizationComponent.
Leave no import of UserDAO anywhere in src/main/java.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 15 — `DELETE enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/infrastructure/teaching`

- Purpose: 删除 EXAMPLE-002 在该目录下的全部样例文件。
- Symbols: CourseDAO, CoursePO, CourseRepository
- Repository evidence: 规格 §8.2 EXAMPLE-002 列出该目录中的每一个文件。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
Delete every file named by EXAMPLE-002 under enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/infrastructure/teaching.
Do not delete RedisConfig, StudentManagementApplication, NativeHttpSecurityConfiguration, or CartAuthorizationComponent.
Leave no import of CourseDAO anywhere in src/main/java.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 16 — `DELETE enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/adapter/user`

- Purpose: 删除 EXAMPLE-001 在该目录下的全部样例文件。
- Symbols: UserControllerTest
- Repository evidence: 规格 §8.2 EXAMPLE-001 列出该目录中的每一个文件。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
Delete every file named by EXAMPLE-001 under enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/adapter/user.
Do not delete RedisConfig, StudentManagementApplication, NativeHttpSecurityConfiguration, or CartAuthorizationComponent.
Leave no import of UserControllerTest anywhere in src/main/java.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 17 — `DELETE enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/adapter/teaching`

- Purpose: 删除 EXAMPLE-002 在该目录下的全部样例文件。
- Symbols: CourseControllerTest
- Repository evidence: 规格 §8.2 EXAMPLE-002 列出该目录中的每一个文件。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
Delete every file named by EXAMPLE-002 under enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/adapter/teaching.
Do not delete RedisConfig, StudentManagementApplication, NativeHttpSecurityConfiguration, or CartAuthorizationComponent.
Leave no import of CourseControllerTest anywhere in src/main/java.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 18 — `DELETE enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/application/user`

- Purpose: 删除 EXAMPLE-001 在该目录下的全部样例文件。
- Symbols: UserManageTest
- Repository evidence: 规格 §8.2 EXAMPLE-001 列出该目录中的每一个文件。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
Delete every file named by EXAMPLE-001 under enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/application/user.
Do not delete RedisConfig, StudentManagementApplication, NativeHttpSecurityConfiguration, or CartAuthorizationComponent.
Leave no import of UserManageTest anywhere in src/main/java.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 19 — `DELETE enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/application/teaching`

- Purpose: 删除 EXAMPLE-002 在该目录下的全部样例文件。
- Symbols: CourseManageTest
- Repository evidence: 规格 §8.2 EXAMPLE-002 列出该目录中的每一个文件。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
Delete every file named by EXAMPLE-002 under enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/application/teaching.
Do not delete RedisConfig, StudentManagementApplication, NativeHttpSecurityConfiguration, or CartAuthorizationComponent.
Leave no import of CourseManageTest anywhere in src/main/java.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 20 — `DELETE enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/domain/user`

- Purpose: 删除 EXAMPLE-001 在该目录下的全部样例文件。
- Symbols: UserAggregateTest
- Repository evidence: 规格 §8.2 EXAMPLE-001 列出该目录中的每一个文件。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
Delete every file named by EXAMPLE-001 under enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/domain/user.
Do not delete RedisConfig, StudentManagementApplication, NativeHttpSecurityConfiguration, or CartAuthorizationComponent.
Leave no import of UserAggregateTest anywhere in src/main/java.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 21 — `DELETE enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/domain/teaching`

- Purpose: 删除 EXAMPLE-002 在该目录下的全部样例文件。
- Symbols: SchoolClassAggregateTest
- Repository evidence: 规格 §8.2 EXAMPLE-002 列出该目录中的每一个文件。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
Delete every file named by EXAMPLE-002 under enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/domain/teaching.
Do not delete RedisConfig, StudentManagementApplication, NativeHttpSecurityConfiguration, or CartAuthorizationComponent.
Leave no import of SchoolClassAggregateTest anywhere in src/main/java.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 22 — `DELETE enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/infrastructure/user`

- Purpose: 删除 EXAMPLE-001 在该目录下的全部样例文件。
- Symbols: UserInfrastructureValidatorTest
- Repository evidence: 规格 §8.2 EXAMPLE-001 列出该目录中的每一个文件。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
Delete every file named by EXAMPLE-001 under enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/infrastructure/user.
Do not delete RedisConfig, StudentManagementApplication, NativeHttpSecurityConfiguration, or CartAuthorizationComponent.
Leave no import of UserInfrastructureValidatorTest anywhere in src/main/java.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 23 — `DELETE enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/infrastructure/teaching`

- Purpose: 删除 EXAMPLE-002 在该目录下的全部样例文件。
- Symbols: TeachingInfrastructureValidatorTest
- Repository evidence: 规格 §8.2 EXAMPLE-002 列出该目录中的每一个文件。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
Delete every file named by EXAMPLE-002 under enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/infrastructure/teaching.
Do not delete RedisConfig, StudentManagementApplication, NativeHttpSecurityConfiguration, or CartAuthorizationComponent.
Leave no import of TeachingInfrastructureValidatorTest anywhere in src/main/java.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 24 — `DELETE enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/facade/user`

- Purpose: 删除 EXAMPLE-001 在该目录下的全部样例文件。
- Symbols: UserFacade
- Repository evidence: 规格 §8.2 EXAMPLE-001 列出该目录中的每一个文件。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
Delete every file named by EXAMPLE-001 under enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/facade/user.
Do not delete RedisConfig, StudentManagementApplication, NativeHttpSecurityConfiguration, or CartAuthorizationComponent.
Leave no import of UserFacade anywhere in src/main/java.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 25 — `DELETE enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/facade/teaching`

- Purpose: 删除 EXAMPLE-002 在该目录下的全部样例文件。
- Symbols: CourseFacade
- Repository evidence: 规格 §8.2 EXAMPLE-002 列出该目录中的每一个文件。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
Delete every file named by EXAMPLE-002 under enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/facade/teaching.
Do not delete RedisConfig, StudentManagementApplication, NativeHttpSecurityConfiguration, or CartAuthorizationComponent.
Leave no import of CourseFacade anywhere in src/main/java.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 26 — `DELETE enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/infrastructure/mq`

- Purpose: 删除 EXAMPLE-011 在该目录下的全部样例文件。
- Symbols: MqRouteEnum, RabbitMqMessageServiceImpl
- Repository evidence: 规格 §8.2 EXAMPLE-011 列出该目录中的每一个文件。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
Delete every file named by EXAMPLE-011 under enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/infrastructure/mq.
Do not delete RedisConfig, StudentManagementApplication, NativeHttpSecurityConfiguration, or CartAuthorizationComponent.
Leave no import of MqRouteEnum anywhere in src/main/java.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 27 — `DELETE enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/infrastructure/mq`

- Purpose: 删除 EXAMPLE-011 在该目录下的全部样例文件。
- Symbols: RabbitMqMessageServiceImplTest
- Repository evidence: 规格 §8.2 EXAMPLE-011 列出该目录中的每一个文件。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
Delete every file named by EXAMPLE-011 under enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/infrastructure/mq.
Do not delete RedisConfig, StudentManagementApplication, NativeHttpSecurityConfiguration, or CartAuthorizationComponent.
Leave no import of RabbitMqMessageServiceImplTest anywhere in src/main/java.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 28 — `DELETE enjoyshop-service/enjoyshop-service-cart/src/main/resources/mybatis/mapper/user`

- Purpose: 删除 EXAMPLE-003 在该目录下的全部样例文件。
- Symbols: UserDAO.xml
- Repository evidence: 规格 §8.2 EXAMPLE-003 列出该目录中的每一个文件。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
Delete every file named by EXAMPLE-003 under enjoyshop-service/enjoyshop-service-cart/src/main/resources/mybatis/mapper/user.
Do not delete RedisConfig, StudentManagementApplication, NativeHttpSecurityConfiguration, or CartAuthorizationComponent.
Leave no import of UserDAO.xml anywhere in src/main/java.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 29 — `DELETE enjoyshop-service/enjoyshop-service-cart/src/main/resources/mybatis/mapper/teaching`

- Purpose: 删除 EXAMPLE-003 在该目录下的全部样例文件。
- Symbols: CourseDAO.xml
- Repository evidence: 规格 §8.2 EXAMPLE-003 列出该目录中的每一个文件。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
Delete every file named by EXAMPLE-003 under enjoyshop-service/enjoyshop-service-cart/src/main/resources/mybatis/mapper/teaching.
Do not delete RedisConfig, StudentManagementApplication, NativeHttpSecurityConfiguration, or CartAuthorizationComponent.
Leave no import of CourseDAO.xml anywhere in src/main/java.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 30 — `DELETE enjoyshop-service/enjoyshop-service-cart/src/main/resources/db/migration/sharding`

- Purpose: 删除 EXAMPLE-005 在该目录下的全部样例文件。
- Symbols: master-data and shard sample SQL
- Repository evidence: 规格 §8.2 EXAMPLE-005 列出该目录中的每一个文件。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
Delete every file named by EXAMPLE-005 under enjoyshop-service/enjoyshop-service-cart/src/main/resources/db/migration/sharding.
Do not delete RedisConfig, StudentManagementApplication, NativeHttpSecurityConfiguration, or CartAuthorizationComponent.
Leave no import of master-data and shard sample SQL anywhere in src/main/java.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 31 — `DELETE enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/common/exception/UserDomainException.java`

- Purpose: 删除 EXAMPLE-001 的样例文件 UserDomainException。
- Symbols: UserDomainException
- Repository evidence: 规格 §8.2 EXAMPLE-001 单独列出该路径。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
Delete enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/common/exception/UserDomainException.java.
Do not delete ConfigDecryptException, RedisConfig, or CartErrorStatus.
Confirm no remaining source file imports UserDomainException.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 32 — `DELETE enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/common/exception/UserFacadeException.java`

- Purpose: 删除 EXAMPLE-001 的样例文件 UserFacadeException。
- Symbols: UserFacadeException
- Repository evidence: 规格 §8.2 EXAMPLE-001 单独列出该路径。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
Delete enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/common/exception/UserFacadeException.java.
Do not delete ConfigDecryptException, RedisConfig, or CartErrorStatus.
Confirm no remaining source file imports UserFacadeException.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 33 — `DELETE enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/common/exception/UserUseCaseException.java`

- Purpose: 删除 EXAMPLE-001 的样例文件 UserUseCaseException。
- Symbols: UserUseCaseException
- Repository evidence: 规格 §8.2 EXAMPLE-001 单独列出该路径。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
Delete enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/common/exception/UserUseCaseException.java.
Do not delete ConfigDecryptException, RedisConfig, or CartErrorStatus.
Confirm no remaining source file imports UserUseCaseException.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 34 — `DELETE enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/common/exception/TeachingDomainException.java`

- Purpose: 删除 EXAMPLE-002 的样例文件 TeachingDomainException。
- Symbols: TeachingDomainException
- Repository evidence: 规格 §8.2 EXAMPLE-002 单独列出该路径。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
Delete enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/common/exception/TeachingDomainException.java.
Do not delete ConfigDecryptException, RedisConfig, or CartErrorStatus.
Confirm no remaining source file imports TeachingDomainException.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 35 — `DELETE enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/common/exception/TeachingFacadeException.java`

- Purpose: 删除 EXAMPLE-002 的样例文件 TeachingFacadeException。
- Symbols: TeachingFacadeException
- Repository evidence: 规格 §8.2 EXAMPLE-002 单独列出该路径。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
Delete enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/common/exception/TeachingFacadeException.java.
Do not delete ConfigDecryptException, RedisConfig, or CartErrorStatus.
Confirm no remaining source file imports TeachingFacadeException.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 36 — `DELETE enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/common/exception/TeachingUseCaseException.java`

- Purpose: 删除 EXAMPLE-002 的样例文件 TeachingUseCaseException。
- Symbols: TeachingUseCaseException
- Repository evidence: 规格 §8.2 EXAMPLE-002 单独列出该路径。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
Delete enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/common/exception/TeachingUseCaseException.java.
Do not delete ConfigDecryptException, RedisConfig, or CartErrorStatus.
Confirm no remaining source file imports TeachingUseCaseException.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 37 — `DELETE enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/common/exception/BaseBusinessException.java`

- Purpose: 删除 EXAMPLE-012 的样例文件 BaseBusinessException。
- Symbols: BaseBusinessException
- Repository evidence: 规格 §8.2 EXAMPLE-012 单独列出该路径。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
Delete enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/common/exception/BaseBusinessException.java.
Do not delete ConfigDecryptException, RedisConfig, or CartErrorStatus.
Confirm no remaining source file imports BaseBusinessException.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 38 — `DELETE enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/common/enums/DeletedStatus.java`

- Purpose: 删除 EXAMPLE-011 的样例文件 DeletedStatus。
- Symbols: DeletedStatus
- Repository evidence: 规格 §8.2 EXAMPLE-011 单独列出该路径。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
Delete enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/common/enums/DeletedStatus.java.
Do not delete ConfigDecryptException, RedisConfig, or CartErrorStatus.
Confirm no remaining source file imports DeletedStatus.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 39 — `DELETE enjoyshop-service/enjoyshop-service-cart/src/main/resources/graphql/user.graphqls`

- Purpose: 删除 EXAMPLE-003 的样例文件 user.graphqls。
- Symbols: user.graphqls
- Repository evidence: 规格 §8.2 EXAMPLE-003 单独列出该路径。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
Delete enjoyshop-service/enjoyshop-service-cart/src/main/resources/graphql/user.graphqls.
Do not delete ConfigDecryptException, RedisConfig, or CartErrorStatus.
Confirm no remaining source file imports user.graphqls.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 40 — `DELETE enjoyshop-service/enjoyshop-service-cart/src/main/resources/graphql/teaching.graphqls`

- Purpose: 删除 EXAMPLE-003 的样例文件 teaching.graphqls。
- Symbols: teaching.graphqls
- Repository evidence: 规格 §8.2 EXAMPLE-003 单独列出该路径。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
Delete enjoyshop-service/enjoyshop-service-cart/src/main/resources/graphql/teaching.graphqls.
Do not delete ConfigDecryptException, RedisConfig, or CartErrorStatus.
Confirm no remaining source file imports teaching.graphqls.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 41 — `DELETE enjoyshop-service/enjoyshop-service-cart/src/main/proto/teaching_user_facade.proto`

- Purpose: 删除 EXAMPLE-003 的样例文件 teaching_user_facade.proto。
- Symbols: teaching_user_facade.proto
- Repository evidence: 规格 §8.2 EXAMPLE-003 单独列出该路径。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
Delete enjoyshop-service/enjoyshop-service-cart/src/main/proto/teaching_user_facade.proto.
Do not delete ConfigDecryptException, RedisConfig, or CartErrorStatus.
Confirm no remaining source file imports teaching_user_facade.proto.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 42 — `DELETE enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/adapter/handler/ApiResponse.java`

- Purpose: 删除 EXAMPLE-004 的样例文件 ApiResponse。
- Symbols: ApiResponse
- Repository evidence: 规格 §8.2 EXAMPLE-004 单独列出该路径。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
Delete enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/adapter/handler/ApiResponse.java.
Do not delete ConfigDecryptException, RedisConfig, or CartErrorStatus.
Confirm no remaining source file imports ApiResponse.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 43 — `DELETE enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/adapter/handler/GlobalExceptionHandler.java`

- Purpose: 删除 EXAMPLE-004 的样例文件 GlobalExceptionHandler。
- Symbols: GlobalExceptionHandler
- Repository evidence: 规格 §8.2 EXAMPLE-004 单独列出该路径。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
Delete enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/adapter/handler/GlobalExceptionHandler.java.
Do not delete ConfigDecryptException, RedisConfig, or CartErrorStatus.
Confirm no remaining source file imports GlobalExceptionHandler.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 44 — `DELETE enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/adapter/handler/GraphQlExceptionResolver.java`

- Purpose: 删除 EXAMPLE-004 的样例文件 GraphQlExceptionResolver。
- Symbols: GraphQlExceptionResolver
- Repository evidence: 规格 §8.2 EXAMPLE-004 单独列出该路径。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
Delete enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/adapter/handler/GraphQlExceptionResolver.java.
Do not delete ConfigDecryptException, RedisConfig, or CartErrorStatus.
Confirm no remaining source file imports GraphQlExceptionResolver.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 45 — `DELETE enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/adapter/handler/ResponseWrapperHandler.java`

- Purpose: 删除 EXAMPLE-004 的样例文件 ResponseWrapperHandler。
- Symbols: ResponseWrapperHandler
- Repository evidence: 规格 §8.2 EXAMPLE-004 单独列出该路径。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
Delete enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/adapter/handler/ResponseWrapperHandler.java.
Do not delete ConfigDecryptException, RedisConfig, or CartErrorStatus.
Confirm no remaining source file imports ResponseWrapperHandler.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 46 — `DELETE enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/adapter/handler/RabbitConsumerErrorHandler.java`

- Purpose: 删除 EXAMPLE-011 的样例文件 RabbitConsumerErrorHandler。
- Symbols: RabbitConsumerErrorHandler
- Repository evidence: 规格 §8.2 EXAMPLE-011 单独列出该路径。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
Delete enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/adapter/handler/RabbitConsumerErrorHandler.java.
Do not delete ConfigDecryptException, RedisConfig, or CartErrorStatus.
Confirm no remaining source file imports RabbitConsumerErrorHandler.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 47 — `DELETE enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/adapter/handler/GlobalExceptionHandlerTest.java`

- Purpose: 删除 EXAMPLE-004 的样例文件 GlobalExceptionHandlerTest。
- Symbols: GlobalExceptionHandlerTest
- Repository evidence: 规格 §8.2 EXAMPLE-004 单独列出该路径。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
Delete enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/adapter/handler/GlobalExceptionHandlerTest.java.
Do not delete ConfigDecryptException, RedisConfig, or CartErrorStatus.
Confirm no remaining source file imports GlobalExceptionHandlerTest.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 48 — `DELETE enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/adapter/handler/GraphQlExceptionResolverTest.java`

- Purpose: 删除 EXAMPLE-004 的样例文件 GraphQlExceptionResolverTest。
- Symbols: GraphQlExceptionResolverTest
- Repository evidence: 规格 §8.2 EXAMPLE-004 单独列出该路径。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
Delete enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/adapter/handler/GraphQlExceptionResolverTest.java.
Do not delete ConfigDecryptException, RedisConfig, or CartErrorStatus.
Confirm no remaining source file imports GraphQlExceptionResolverTest.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 49 — `DELETE enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/adapter/handler/ResponseWrapperHandlerTest.java`

- Purpose: 删除 EXAMPLE-004 的样例文件 ResponseWrapperHandlerTest。
- Symbols: ResponseWrapperHandlerTest
- Repository evidence: 规格 §8.2 EXAMPLE-004 单独列出该路径。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
Delete enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/adapter/handler/ResponseWrapperHandlerTest.java.
Do not delete ConfigDecryptException, RedisConfig, or CartErrorStatus.
Confirm no remaining source file imports ResponseWrapperHandlerTest.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 50 — `DELETE enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/adapter/handler/RabbitConsumerErrorHandlerTest.java`

- Purpose: 删除 EXAMPLE-011 的样例文件 RabbitConsumerErrorHandlerTest。
- Symbols: RabbitConsumerErrorHandlerTest
- Repository evidence: 规格 §8.2 EXAMPLE-011 单独列出该路径。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
Delete enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/adapter/handler/RabbitConsumerErrorHandlerTest.java.
Do not delete ConfigDecryptException, RedisConfig, or CartErrorStatus.
Confirm no remaining source file imports RabbitConsumerErrorHandlerTest.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 51 — `DELETE enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/infrastructure/config/RabbitMqConfig.java`

- Purpose: 删除 EXAMPLE-011 的样例文件 RabbitMqConfig。
- Symbols: RabbitMqConfig
- Repository evidence: 规格 §8.2 EXAMPLE-011 单独列出该路径。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
Delete enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/infrastructure/config/RabbitMqConfig.java.
Do not delete ConfigDecryptException, RedisConfig, or CartErrorStatus.
Confirm no remaining source file imports RabbitMqConfig.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 52 — `DELETE enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/infrastructure/config/LocalAdapterConfiguration.java`

- Purpose: 删除 EXAMPLE-011 的样例文件 LocalAdapterConfiguration。
- Symbols: LocalAdapterConfiguration
- Repository evidence: 规格 §8.2 EXAMPLE-011 单独列出该路径。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
Delete enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/infrastructure/config/LocalAdapterConfiguration.java.
Do not delete ConfigDecryptException, RedisConfig, or CartErrorStatus.
Confirm no remaining source file imports LocalAdapterConfiguration.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 53 — `DELETE enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/infrastructure/config/ExternalClientConfig.java`

- Purpose: 删除 EXAMPLE-011 的样例文件 ExternalClientConfig。
- Symbols: ExternalClientConfig
- Repository evidence: 规格 §8.2 EXAMPLE-011 单独列出该路径。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
Delete enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/infrastructure/config/ExternalClientConfig.java.
Do not delete ConfigDecryptException, RedisConfig, or CartErrorStatus.
Confirm no remaining source file imports ExternalClientConfig.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 54 — `DELETE enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/infrastructure/config/TransactionCompletionExecutor.java`

- Purpose: 删除 EXAMPLE-011 的样例文件 TransactionCompletionExecutor。
- Symbols: TransactionCompletionExecutor
- Repository evidence: 规格 §8.2 EXAMPLE-011 单独列出该路径。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
Delete enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/infrastructure/config/TransactionCompletionExecutor.java.
Do not delete ConfigDecryptException, RedisConfig, or CartErrorStatus.
Confirm no remaining source file imports TransactionCompletionExecutor.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 55 — `DELETE enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/infrastructure/config/RabbitMqConfigTest.java`

- Purpose: 删除 EXAMPLE-011 的样例文件 RabbitMqConfigTest。
- Symbols: RabbitMqConfigTest
- Repository evidence: 规格 §8.2 EXAMPLE-011 单独列出该路径。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
Delete enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/infrastructure/config/RabbitMqConfigTest.java.
Do not delete ConfigDecryptException, RedisConfig, or CartErrorStatus.
Confirm no remaining source file imports RabbitMqConfigTest.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 56 — `DELETE enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/infrastructure/config/TransactionCompletionExecutorTest.java`

- Purpose: 删除 EXAMPLE-011 的样例文件 TransactionCompletionExecutorTest。
- Symbols: TransactionCompletionExecutorTest
- Repository evidence: 规格 §8.2 EXAMPLE-011 单独列出该路径。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
Delete enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/infrastructure/config/TransactionCompletionExecutorTest.java.
Do not delete ConfigDecryptException, RedisConfig, or CartErrorStatus.
Confirm no remaining source file imports TransactionCompletionExecutorTest.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 57 — `DELETE enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/support/RecordingMqMessageService.java`

- Purpose: 删除 EXAMPLE-011 的样例文件 RecordingMqMessageService。
- Symbols: RecordingMqMessageService
- Repository evidence: 规格 §8.2 EXAMPLE-011 单独列出该路径。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
Delete enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/support/RecordingMqMessageService.java.
Do not delete ConfigDecryptException, RedisConfig, or CartErrorStatus.
Confirm no remaining source file imports RecordingMqMessageService.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 58 — `DELETE enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/adapter/pojo/convertor/LightFacadeConverter.java`

- Purpose: 删除 EXAMPLE-012 的样例文件 LightFacadeConverter。
- Symbols: LightFacadeConverter
- Repository evidence: 规格 §8.2 EXAMPLE-012 单独列出该路径。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
Delete enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/adapter/pojo/convertor/LightFacadeConverter.java.
Do not delete ConfigDecryptException, RedisConfig, or CartErrorStatus.
Confirm no remaining source file imports LightFacadeConverter.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 59 — `DELETE enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/adapter/pojo/convertor/package-info.java`

- Purpose: 删除 EXAMPLE-012 的样例文件 convertor package-info。
- Symbols: convertor package-info
- Repository evidence: 规格 §8.2 EXAMPLE-012 单独列出该路径。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
Delete enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/adapter/pojo/convertor/package-info.java.
Do not delete ConfigDecryptException, RedisConfig, or CartErrorStatus.
Confirm no remaining source file imports convertor package-info.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 60 — `DELETE enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/adapter/pojo/dto/RpcIdQuery.java`

- Purpose: 删除 EXAMPLE-012 的样例文件 RpcIdQuery。
- Symbols: RpcIdQuery
- Repository evidence: 规格 §8.2 EXAMPLE-012 单独列出该路径。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
Delete enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/adapter/pojo/dto/RpcIdQuery.java.
Do not delete ConfigDecryptException, RedisConfig, or CartErrorStatus.
Confirm no remaining source file imports RpcIdQuery.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 61 — `DELETE enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/adapter/pojo/dto/package-info.java`

- Purpose: 删除 EXAMPLE-012 的样例文件 dto package-info。
- Symbols: dto package-info
- Repository evidence: 规格 §8.2 EXAMPLE-012 单独列出该路径。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
Delete enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/adapter/pojo/dto/package-info.java.
Do not delete ConfigDecryptException, RedisConfig, or CartErrorStatus.
Confirm no remaining source file imports dto package-info.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 62 — `DELETE enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/facade/validation/NativeRpcValidationGroup.java`

- Purpose: 删除 EXAMPLE-012 的样例文件 NativeRpcValidationGroup。
- Symbols: NativeRpcValidationGroup
- Repository evidence: 规格 §8.2 EXAMPLE-012 单独列出该路径。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
Delete enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/facade/validation/NativeRpcValidationGroup.java.
Do not delete ConfigDecryptException, RedisConfig, or CartErrorStatus.
Confirm no remaining source file imports NativeRpcValidationGroup.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 63 — `DELETE enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/facade/validation/package-info.java`

- Purpose: 删除 EXAMPLE-012 的样例文件 validation package-info。
- Symbols: validation package-info
- Repository evidence: 规格 §8.2 EXAMPLE-012 单独列出该路径。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
Delete enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/facade/validation/package-info.java.
Do not delete ConfigDecryptException, RedisConfig, or CartErrorStatus.
Confirm no remaining source file imports validation package-info.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 64 — `DELETE enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/facade/package-info.java`

- Purpose: 删除 EXAMPLE-012 的样例文件 facade package-info。
- Symbols: facade package-info
- Repository evidence: 规格 §8.2 EXAMPLE-012 单独列出该路径。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
Delete enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/facade/package-info.java.
Do not delete ConfigDecryptException, RedisConfig, or CartErrorStatus.
Confirm no remaining source file imports facade package-info.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 65 — `MODIFY enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/start/config/NativeRpcConfiguration.java`

- Purpose: 去掉样例 RPC 转换器，保留校验 Bean。
- Symbols: nativeRpcValidation, nativeRpcValidationExceptionMapper
- Repository evidence: EXAMPLE-007。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
Remove the lightFacadeConverter bean and teaching or user RPC providers.
Keep nativeRpcValidation and nativeRpcValidationExceptionMapper.
Do not restore teaching_user_facade.proto.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 66 — `MODIFY enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/start/config/NativeRpcConfigurationTest.java`

- Purpose: RPC 测试不再引用 proto。
- Symbols: NativeRpcConfigurationTest
- Repository evidence: EXAMPLE-007。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
Assert nativeRpcValidation exists.
Assert lightFacadeConverter does not exist.
Do not load teaching_user_facade.proto.
Stop when a deleted sample type is still imported, and do not recreate that sample to force compilation.
Keep RedisConfig, StudentManagementApplication, NativeHttpSecurityConfiguration, CartAuthorizationComponent, and CartErrorStatus.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 67 — `MODIFY enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/adapter/NativeRpcProviderTest.java`

- Purpose: 提供者测试不再引用样例 facade。
- Symbols: NativeRpcProviderTest
- Repository evidence: EXAMPLE-007。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
Remove assertions that UserFacade or CourseFacade beans exist.
Keep the test compiling against nativeRpcValidation only.
Do not recreate a sample provider.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 68 — `MODIFY enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/facade/NativeRpcContractTest.java`

- Purpose: 契约测试不再解析样例 proto。
- Symbols: NativeRpcContractTest
- Repository evidence: EXAMPLE-007。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
Stop reading teaching_user_facade.proto.
Assert the cart module has no facade.user or facade.teaching type.
Keep validation group checks only if the group file still exists; otherwise delete those assertions.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 69 — `MODIFY enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/facade/NativeRpcMappingTest.java`

- Purpose: 映射测试不再依赖 LightFacadeConverter。
- Symbols: NativeRpcMappingTest
- Repository evidence: EXAMPLE-007。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
Remove LightFacadeConverter mapping cases.
Do not add a cart RPC facade.
Leave the test green by asserting no sample converter remains.
Stop when a deleted sample type is still imported, and do not recreate that sample to force compilation.
Keep RedisConfig, StudentManagementApplication, NativeHttpSecurityConfiguration, CartAuthorizationComponent, and CartErrorStatus.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 70 — `MODIFY enjoyshop-service/enjoyshop-service-cart/pom.xml`

- Purpose: 去掉 LightFacadeConverterImpl 登记；没有 proto 时跳过 protobuf 生成。
- Symbols: facadeImplementationPackage
- Repository evidence: cart pom.xml 第 308 行登记了 LightFacadeConverterImpl。EXAMPLE-007。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```xml
Delete the facadeImplementationPackage line that names LightFacadeConverterImpl.
If the inherited protobuf plugin fails with no proto, skip that plugin goal in this module POM only.
Do not remove the two Tianquan 5.4.1 dependencies added in Step 1.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 71 — `MODIFY enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/start/StudentManagementApplication.java`

- Purpose: 排除 GraphQL 自动配置，并把 MapperScan 改到 cart DAO 包。
- Symbols: StudentManagementApplication
- Repository evidence: 当前 MapperScan 指向 infrastructure.user.dao 与 infrastructure.teaching.dao。goods 启动类排除 GraphQlAutoConfiguration 与 GraphQlWebMvcAutoConfiguration。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```java
@SpringBootApplication(scanBasePackages = "top.egon.enjoyshop.cart",
    exclude = {GraphQlAutoConfiguration.class, GraphQlWebMvcAutoConfiguration.class})
@MapperScan(basePackages = "top.egon.enjoyshop.cart.infrastructure.cart.dao")
Keep the class name StudentManagementApplication.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 72 — `CREATE enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/infrastructure/cart/dao/package-info.java`

- Purpose: 在 DAO 生成前提供 MapperScan 能解析的空包。
- Symbols: package-info
- Repository evidence: PLAN-CLAR-003。生成器下一步会在同包写入 CartItemDAO.java。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```java
package top.egon.enjoyshop.cart.infrastructure.cart.dao;
/** Cart DAO package reserved for the generator. */
Do not put a handwritten CartItemDAO in this file.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 73 — `MODIFY enjoyshop-service/enjoyshop-service-cart/src/main/resources/db/egon-mp/V20260913_001__initialize_repository_schema.sql`

- Purpose: 只保留 ddl_history，去掉样例业务表。
- Symbols: ddl_history
- Repository evidence: EXAMPLE-006。该脚本尚未在 cart 模块应用。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
Keep the existing ddl_history definition at the end of the script.
Remove CREATE TABLE users, roles, permissions, user_roles, role_permissions, courses, school_classes, and class_course_schedules.
An unknown egon_migration.role still fails.
Do not add cart_item to this already-versioned file.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 74 — `MODIFY enjoyshop-service/enjoyshop-service-cart/src/main/resources/db/egon-mp/repository-manifest.json`

- Purpose: 把初始化脚本的 SHA-256 更新为替换后的字节。
- Symbols: repository-manifest.json
- Repository evidence: EXAMPLE-006 要求 manifest 哈希与文件一起更新。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
Compute SHA-256 of the replaced V20260913_001 file bytes.
Replace only that script entry's sha256.
Do not change the version token or reorder applied history.
Do not add the cart script until Step 4.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 75 — `MODIFY enjoyshop-service/enjoyshop-service-cart/src/main/resources/application.yml`

- Purpose: 去掉样例用户和教学服务地址，保留 Step 1 的权限键。
- Symbols: external-http.enabled
- Repository evidence: EXAMPLE-013。test 文件本来就没有 user-base-url，另外三个文件有。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
In application.yml, delete app.integrations.external-http.user-base-url and teaching-base-url.
Leave external-http.enabled.
Keep app.goods.base-url, timeouts, CORS, and enjoyshop-cart system-code from Step 1.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 76 — `MODIFY enjoyshop-service/enjoyshop-service-cart/src/main/resources/application-dev.yml`

- Purpose: 去掉样例用户和教学服务地址，保留 Step 1 的权限键。
- Symbols: external-http.enabled
- Repository evidence: EXAMPLE-013。test 文件本来就没有 user-base-url，另外三个文件有。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
In application-dev.yml, delete app.integrations.external-http.user-base-url and teaching-base-url.
Leave external-http.enabled.
Keep app.goods.base-url, timeouts, CORS, and enjoyshop-cart system-code from Step 1.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 77 — `MODIFY enjoyshop-service/enjoyshop-service-cart/src/main/resources/application-prod.yml`

- Purpose: 去掉样例用户和教学服务地址，保留 Step 1 的权限键。
- Symbols: external-http.enabled
- Repository evidence: EXAMPLE-013。test 文件本来就没有 user-base-url，另外三个文件有。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
In application-prod.yml, delete app.integrations.external-http.user-base-url and teaching-base-url.
Leave external-http.enabled.
Keep app.goods.base-url, timeouts, CORS, and enjoyshop-cart system-code from Step 1.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 78 — `MODIFY enjoyshop-service/enjoyshop-service-cart/src/main/resources/application-test.yml`

- Purpose: 去掉样例用户和教学服务地址，保留 Step 1 的权限键。
- Symbols: external-http.enabled
- Repository evidence: EXAMPLE-013。test 文件本来就没有 user-base-url，另外三个文件有。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
In application-test.yml, delete app.integrations.external-http.user-base-url and teaching-base-url.
Leave external-http.enabled.
Keep app.goods.base-url, timeouts, CORS, and enjoyshop-cart system-code from Step 1.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 79 — `MODIFY enjoyshop-service/enjoyshop-service-cart/README.md`

- Purpose: 把学生样例说明改成购物车模块说明。
- Symbols: README.md
- Repository evidence: EXAMPLE-009。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
Replace the student-management title with EnjoyShop Cart.
State that cart data lives in PostgreSQL and that login uses the existing Edge flow.
Do not document a Redis cart hash or a Thymeleaf page.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 80 — `MODIFY enjoyshop-service/enjoyshop-service-cart/README.zh-CN.md`

- Purpose: 中文说明与英文说明一致。
- Symbols: README.zh-CN.md
- Repository evidence: EXAMPLE-009。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
把标题改为 EnjoyShop Cart。
写明权威库是 PostgreSQL，登录沿用御衡 Edge。
不要写 Redis 购物车账本。
Stop when a deleted sample type is still imported, and do not recreate that sample to force compilation.
Keep RedisConfig, StudentManagementApplication, NativeHttpSecurityConfiguration, CartAuthorizationComponent, and CartErrorStatus.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 81 — `MODIFY enjoyshop-service/enjoyshop-service-cart/deploy/container/README.md`

- Purpose: 容器说明不再提到教学 proto。
- Symbols: deploy/container/README.md
- Repository evidence: EXAMPLE-009 写明容器说明现在写着教学 proto。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```text
Remove teaching_user_facade.proto from the container notes.
Name the process enjoyshop-service-cart and the default port 8081.
Do not add a new base image or a new published port contract.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。

#### File 82 — `MODIFY enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/start/config/OpenApiConfig.java`

- Purpose: 把 OpenAPI 标题从学生样例改为购物车。安全方案留到 Step 7。
- Symbols: OpenApiConfig
- Repository evidence: 当前 Bean 名是 studentManagementOpenApi，标题是 Student Management。
- Dependencies and consumers: Step 2 的安全配置必须保留。样例类型没有购物车消费者。
- Why now: 样例必须在生成 cart_item 之前消失，否则初始化脚本仍会建用户表。
- Contract/signature changes: 删除或替换样例符号，不改变 cartSecurityFilterChain 的 401 契约。
- Input/output and state mapping: 这些文件没有购物车行。删除后没有数据库写入。
- Error and edge behavior: 若编译仍引用被删类型，回到引用处删除 import，不要把样例类加回来。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 保持 Light 包，只删除样例纵向。Rule 7 的四个 YAML 在本步去掉样例键后仍然键集合一致。
- Implementation pseudocode:

```java
Change the Info title to EnjoyShop Cart.
Keep the bean method compiling.
bearerAuth and the bean name cartOpenApi are applied in Step 7 together with the contract test.
Do not invent a second OpenAPI bean in this step.
```

- Verification contribution: 本步末尾的架构测试与 rg 扫描。
- After this file: 该路径不再提供样例符号，基础设施文件仍在。


- Validation working directory: 仓库根目录 `/Users/mario/SelfProject/enjoyshop`
- Verification command: `mvn -pl enjoyshop-service/enjoyshop-service-cart -am -Dtest=ArchetypeContractConvergenceTest,LightPersistenceArchitectureTest,FlywayMigrationConventionTest,NativeHttpCompatibilityTest,CartAuthenticationBoundaryTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: 退出码 0。源码树找不到 `class UserController`、`class CourseController`、`CREATE TABLE users` 或 `user-base-url`。`RedisConfig` 与 `cartSecurityFilterChain` 仍在。
- Failure returns to: 仍引用被删类型的测试或启动类。
- Completion criteria: `REQ-020` 的样例路径已删除或替换，且 Step 1 的权限键还在。
- Rollback: 回退本步提交。不要用恢复课程表的方式修编译。
- Commit paths: `enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/architecture/ArchetypeContractConvergenceTest.java`, `enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/architecture/LightPersistenceArchitectureTest.java`, `enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/infrastructure/migration/FlywayMigrationConventionTest.java`, `enjoyshop-service/enjoyshop-service-cart/src/test/resources/mybatis/h2-schema.sql`, `enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/application/transaction/LocalTransactionBoundaryTest.java`, `enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/start/config/NativeHttpCompatibilityTest.java`, `enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/infrastructure/config/datasource/RepositoryPersistenceContractTest.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/adapter/user`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/adapter/teaching`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/application/user`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/application/teaching`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/domain/user`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/domain/teaching`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/infrastructure/user`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/infrastructure/teaching`, `enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/adapter/user`, `enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/adapter/teaching`, `enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/application/user`, `enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/application/teaching`, `enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/domain/user`, `enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/domain/teaching`, `enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/infrastructure/user`, `enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/infrastructure/teaching`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/facade/user`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/facade/teaching`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/infrastructure/mq`, `enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/infrastructure/mq`, `enjoyshop-service/enjoyshop-service-cart/src/main/resources/mybatis/mapper/user`, `enjoyshop-service/enjoyshop-service-cart/src/main/resources/mybatis/mapper/teaching`, `enjoyshop-service/enjoyshop-service-cart/src/main/resources/db/migration/sharding`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/common/exception/UserDomainException.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/common/exception/UserFacadeException.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/common/exception/UserUseCaseException.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/common/exception/TeachingDomainException.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/common/exception/TeachingFacadeException.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/common/exception/TeachingUseCaseException.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/common/exception/BaseBusinessException.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/common/enums/DeletedStatus.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/resources/graphql/user.graphqls`, `enjoyshop-service/enjoyshop-service-cart/src/main/resources/graphql/teaching.graphqls`, `enjoyshop-service/enjoyshop-service-cart/src/main/proto/teaching_user_facade.proto`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/adapter/handler/ApiResponse.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/adapter/handler/GlobalExceptionHandler.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/adapter/handler/GraphQlExceptionResolver.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/adapter/handler/ResponseWrapperHandler.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/adapter/handler/RabbitConsumerErrorHandler.java`, `enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/adapter/handler/GlobalExceptionHandlerTest.java`, `enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/adapter/handler/GraphQlExceptionResolverTest.java`, `enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/adapter/handler/ResponseWrapperHandlerTest.java`, `enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/adapter/handler/RabbitConsumerErrorHandlerTest.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/infrastructure/config/RabbitMqConfig.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/infrastructure/config/LocalAdapterConfiguration.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/infrastructure/config/ExternalClientConfig.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/infrastructure/config/TransactionCompletionExecutor.java`, `enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/infrastructure/config/RabbitMqConfigTest.java`, `enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/infrastructure/config/TransactionCompletionExecutorTest.java`, `enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/support/RecordingMqMessageService.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/adapter/pojo/convertor/LightFacadeConverter.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/adapter/pojo/convertor/package-info.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/adapter/pojo/dto/RpcIdQuery.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/adapter/pojo/dto/package-info.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/facade/validation/NativeRpcValidationGroup.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/facade/validation/package-info.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/facade/package-info.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/start/config/NativeRpcConfiguration.java`, `enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/start/config/NativeRpcConfigurationTest.java`, `enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/adapter/NativeRpcProviderTest.java`, `enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/facade/NativeRpcContractTest.java`, `enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/facade/NativeRpcMappingTest.java`, `enjoyshop-service/enjoyshop-service-cart/pom.xml`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/start/StudentManagementApplication.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/infrastructure/cart/dao/package-info.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/resources/db/egon-mp/V20260913_001__initialize_repository_schema.sql`, `enjoyshop-service/enjoyshop-service-cart/src/main/resources/db/egon-mp/repository-manifest.json`, `enjoyshop-service/enjoyshop-service-cart/src/main/resources/application.yml`, `enjoyshop-service/enjoyshop-service-cart/src/main/resources/application-dev.yml`, `enjoyshop-service/enjoyshop-service-cart/src/main/resources/application-prod.yml`, `enjoyshop-service/enjoyshop-service-cart/src/main/resources/application-test.yml`, `enjoyshop-service/enjoyshop-service-cart/README.md`, `enjoyshop-service/enjoyshop-service-cart/README.zh-CN.md`, `enjoyshop-service/enjoyshop-service-cart/deploy/container/README.md`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/start/config/OpenApiConfig.java`
- Commit: `refactor(cart): remove unused light archetype samples`

### Step 4 — 编写 cart_item 脚本并生成目录类型

- Requirements: `REQ-001` `REQ-003` `REQ-011`
- Dependencies: `Step 3`
- Baseline state: 样例表脚本已去掉业务表。`infrastructure.cart.dao` 只有包说明。没有 `cart_item`。
- Observable outcome: v2 plan 对 `cart_item` 的 `backend-crud` 产物是 ADD，`apply` 退出码 0。PO 不重复声明 EgonModel 列。独立 `CREATE INDEX` 留在 SQL 中，生成器忽略它们。
- End state: 22 个目录文件已生成。公开 REST 仍可能是生成器的 CRUD 路径，Step 7 才收成两条购物车 URL。不提交 `.egon/`。
- Test-first gate: `Not applicable — 目录文件禁止先手写类体再让测试编译。证明是生成器 plan 的 ADD 清单，而不是一个预先失败的 Java 测试`
- Manual Checks: `MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-NAME-001, MC-MODEL-001, MC-CONVERT-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001`
- Literal Rules: `Rule 1, Rule 3, Rule 10, Rule 11`
- Ordered files:

#### File 1 — `CREATE enjoyshop-service/enjoyshop-service-cart/src/main/resources/db/egon-mp/V20261013_001__initialize_cart_schema.sql`

- Purpose: 由代理编写 cart_item 的 CREATE TABLE 和两个部分索引。
- Symbols: cart_item, uk_cart_item_active, idx_cart_item_owner
- Repository evidence: goods 的 V20261009_001 把 CREATE TABLE 放在 MASTER_DATA 的 DO 块里，并把部分唯一索引放在同一脚本。
- Dependencies and consumers: Step 3 已去掉样例 CREATE TABLE users。生成器只消费 cart_item。
- Why now: 目录类型必须由生成器先落盘，后面的策略才能修改领域服务。
- Contract/signature changes: PO 只含业务列。Mapper XML 仍映射继承列。events.enabled 是 false。
- Input/output and state mapping: CREATE INDEX 不参与建模。软删除唯一性由 Step 9 用 PostgreSQL 执行证明。
- Error and edge behavior: plan 出现 CONFLICT 或 BLOCKED_TOOLING 时停止，不手写替代类。
- Standards impact: MC-ARCH-001, MC-MODEL-001, MC-CONVERT-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 1 保留生成后缀。Rule 3 使用生成器的 Lombok 与 BaseConverter。Rule 10 时间列使用 java.time。Rule 11 包保持 Light。
- Implementation pseudocode:

```sql
DO $egon$
BEGIN
  IF current_setting('egon_migration.role') = 'MASTER_DATA' THEN
    CREATE TABLE cart_item (
      id BIGINT PRIMARY KEY,
      tenant_id BIGINT NOT NULL,
      create_user_id VARCHAR(128) NOT NULL,
      create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL,
      update_user_id VARCHAR(128) NOT NULL,
      update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL,
      deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
      version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
      owner_user_id VARCHAR(128) NOT NULL,
      spu_id BIGINT NOT NULL,
      sku_id BIGINT NOT NULL,
      category1_id BIGINT NOT NULL,
      category2_id BIGINT NOT NULL,
      category3_id BIGINT NOT NULL,
      name VARCHAR(3000) NOT NULL,
      image VARCHAR(500) NOT NULL DEFAULT '',
      unit_price INTEGER NOT NULL CHECK (unit_price >= 1),
      unit_weight INTEGER NOT NULL CHECK (unit_weight >= 0),
      quantity INTEGER NOT NULL CHECK (quantity >= 1),
      line_amount INTEGER NOT NULL CHECK (line_amount >= 1),
      line_weight INTEGER NOT NULL CHECK (line_weight >= 0),
      pay_amount INTEGER NOT NULL CHECK (pay_amount = line_amount)
    );
    CREATE UNIQUE INDEX uk_cart_item_active ON cart_item (tenant_id, owner_user_id, sku_id) WHERE deleted_at IS NULL;
    CREATE INDEX idx_cart_item_owner ON cart_item (tenant_id, owner_user_id, id) WHERE deleted_at IS NULL;
  ELSIF current_setting('egon_migration.role') IS DISTINCT FROM 'MASTER_DATA' THEN
    RAISE EXCEPTION 'unsupported egon_migration.role';
  END IF;
END
$egon$;
```

- Verification contribution: v2 plan 的文件清单包含该路径，或在路径不同时包含同一 artifact。
- After this file: 该文件由生成器拥有。后续业务修改会使再次 apply 产生 CONFLICT，因此不再对它重跑生成器。

#### File 2 — `MODIFY enjoyshop-service/enjoyshop-service-cart/src/main/resources/db/egon-mp/repository-manifest.json`

- Purpose: 登记新脚本的版本、路径和 SHA-256。
- Symbols: repository-manifest.json
- Repository evidence: goods 的 manifest 按版本追加脚本，不改已有条目的字节。
- Dependencies and consumers: Step 3 已去掉样例 CREATE TABLE users。生成器只消费 cart_item。
- Why now: 目录类型必须由生成器先落盘，后面的策略才能修改领域服务。
- Contract/signature changes: PO 只含业务列。Mapper XML 仍映射继承列。events.enabled 是 false。
- Input/output and state mapping: CREATE INDEX 不参与建模。软删除唯一性由 Step 9 用 PostgreSQL 执行证明。
- Error and edge behavior: plan 出现 CONFLICT 或 BLOCKED_TOOLING 时停止，不手写替代类。
- Standards impact: MC-ARCH-001, MC-MODEL-001, MC-CONVERT-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 1 保留生成后缀。Rule 3 使用生成器的 Lombok 与 BaseConverter。Rule 10 时间列使用 java.time。Rule 11 包保持 Light。
- Implementation pseudocode:

```text
Append one scripts entry for V20261013_001.
sha256 is the digest of the new file bytes.
Do not edit the V20260913_001 digest again.
Do not connect to PostgreSQL.
```

- Verification contribution: v2 plan 的文件清单包含该路径，或在路径不同时包含同一 artifact。
- After this file: 该文件由生成器拥有。后续业务修改会使再次 apply 产生 CONFLICT，因此不再对它重跑生成器。

#### File 3 — `CREATE enjoyshop-service/enjoyshop-service-cart/codegen/cart-item.json`

- Purpose: 给出生成器输入。configVersion 必须是 1。
- Symbols: cart-item.json
- Repository evidence: 生成器 README：未知键会被 FAIL_ON_UNKNOWN_PROPERTIES 拒绝。backend-crud 展开到控制器。
- Dependencies and consumers: Step 3 已去掉样例 CREATE TABLE users。生成器只消费 cart_item。
- Why now: 目录类型必须由生成器先落盘，后面的策略才能修改领域服务。
- Contract/signature changes: PO 只含业务列。Mapper XML 仍映射继承列。events.enabled 是 false。
- Input/output and state mapping: CREATE INDEX 不参与建模。软删除唯一性由 Step 9 用 PostgreSQL 执行证明。
- Error and edge behavior: plan 出现 CONFLICT 或 BLOCKED_TOOLING 时停止，不手写替代类。
- Standards impact: MC-ARCH-001, MC-MODEL-001, MC-CONVERT-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 1 保留生成后缀。Rule 3 使用生成器的 Lombok 与 BaseConverter。Rule 10 时间列使用 java.time。Rule 11 包保持 Light。
- Implementation pseudocode:

```json
{
  "configVersion": 1,
  "projectType": "light",
  "basePackage": "top.egon.enjoyshop.cart",
  "domain": "cart",
  "outputRoot": "enjoyshop-service/enjoyshop-service-cart",
  "input": {"mode": "schema", "schemaFiles": ["enjoyshop-service/enjoyshop-service-cart/src/main/resources/db/egon-mp/V20261013_001__initialize_cart_schema.sql"]},
  "logicalTables": ["cart_item"],
  "artifacts": ["backend-crud"],
  "events": {"enabled": false},
  "apiContract": {"basePath": "/api/v1/cart", "existingErrorMapper": "top.egon.enjoyshop.cart.common.enums.CartErrorStatus"}
}
```

- Verification contribution: v2 plan 的文件清单包含该路径，或在路径不同时包含同一 artifact。
- After this file: 该文件由生成器拥有。后续业务修改会使再次 apply 产生 CONFLICT，因此不再对它重跑生成器。

#### File 4 — `GENERATED enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/infrastructure/cart/po/CartItemPO.java`

- Purpose: 由 backend-crud 生成该目录文件。
- Symbols: CartItemPO.java
- Repository evidence: GenerationScopeValidator 对表 cart_item 使用 stem CartItem，领域模型后缀 BO。
- Dependencies and consumers: Step 3 已去掉样例 CREATE TABLE users。生成器只消费 cart_item。
- Why now: 目录类型必须由生成器先落盘，后面的策略才能修改领域服务。
- Contract/signature changes: PO 只含业务列。Mapper XML 仍映射继承列。events.enabled 是 false。
- Input/output and state mapping: CREATE INDEX 不参与建模。软删除唯一性由 Step 9 用 PostgreSQL 执行证明。
- Error and edge behavior: plan 出现 CONFLICT 或 BLOCKED_TOOLING 时停止，不手写替代类。
- Standards impact: MC-ARCH-001, MC-MODEL-001, MC-CONVERT-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 1 保留生成后缀。Rule 3 使用生成器的 Lombok 与 BaseConverter。Rule 10 时间列使用 java.time。Rule 11 包保持 Light。
- Implementation pseudocode:

```text
Do not type this file by hand.
Run /Users/mario/SelfProject/Egon-COLA/scripts/egon-codegen.sh plan --config enjoyshop-service/enjoyshop-service-cart/codegen/cart-item.json.
Require EGON_CODEGEN_CLASSPATH. A missing classpath prints BLOCKED_TOOLING and exits 7. Do not download dependencies.
Read the v2 plan JSON. This path must be an ADD with no CONFLICT.
Then run apply --plan the printed plan id --config the same json.
If the v2 plan uses a different path for the same artifact, keep the plan path and do not create a second class.
```

- Verification contribution: v2 plan 的文件清单包含该路径，或在路径不同时包含同一 artifact。
- After this file: 该文件由生成器拥有。后续业务修改会使再次 apply 产生 CONFLICT，因此不再对它重跑生成器。

#### File 5 — `GENERATED enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/infrastructure/cart/dao/CartItemDAO.java`

- Purpose: 由 backend-crud 生成该目录文件。
- Symbols: CartItemDAO.java
- Repository evidence: GenerationScopeValidator 对表 cart_item 使用 stem CartItem，领域模型后缀 BO。
- Dependencies and consumers: Step 3 已去掉样例 CREATE TABLE users。生成器只消费 cart_item。
- Why now: 目录类型必须由生成器先落盘，后面的策略才能修改领域服务。
- Contract/signature changes: PO 只含业务列。Mapper XML 仍映射继承列。events.enabled 是 false。
- Input/output and state mapping: CREATE INDEX 不参与建模。软删除唯一性由 Step 9 用 PostgreSQL 执行证明。
- Error and edge behavior: plan 出现 CONFLICT 或 BLOCKED_TOOLING 时停止，不手写替代类。
- Standards impact: MC-ARCH-001, MC-MODEL-001, MC-CONVERT-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 1 保留生成后缀。Rule 3 使用生成器的 Lombok 与 BaseConverter。Rule 10 时间列使用 java.time。Rule 11 包保持 Light。
- Implementation pseudocode:

```text
Do not type this file by hand.
Run /Users/mario/SelfProject/Egon-COLA/scripts/egon-codegen.sh plan --config enjoyshop-service/enjoyshop-service-cart/codegen/cart-item.json.
Require EGON_CODEGEN_CLASSPATH. A missing classpath prints BLOCKED_TOOLING and exits 7. Do not download dependencies.
Read the v2 plan JSON. This path must be an ADD with no CONFLICT.
Then run apply --plan the printed plan id --config the same json.
If the v2 plan uses a different path for the same artifact, keep the plan path and do not create a second class.
```

- Verification contribution: v2 plan 的文件清单包含该路径，或在路径不同时包含同一 artifact。
- After this file: 该文件由生成器拥有。后续业务修改会使再次 apply 产生 CONFLICT，因此不再对它重跑生成器。

#### File 6 — `GENERATED enjoyshop-service/enjoyshop-service-cart/src/main/resources/mybatis/mapper/cart/CartItemDAO.xml`

- Purpose: 由 backend-crud 生成该目录文件。
- Symbols: CartItemDAO.xml
- Repository evidence: GenerationScopeValidator 对表 cart_item 使用 stem CartItem，领域模型后缀 BO。
- Dependencies and consumers: Step 3 已去掉样例 CREATE TABLE users。生成器只消费 cart_item。
- Why now: 目录类型必须由生成器先落盘，后面的策略才能修改领域服务。
- Contract/signature changes: PO 只含业务列。Mapper XML 仍映射继承列。events.enabled 是 false。
- Input/output and state mapping: CREATE INDEX 不参与建模。软删除唯一性由 Step 9 用 PostgreSQL 执行证明。
- Error and edge behavior: plan 出现 CONFLICT 或 BLOCKED_TOOLING 时停止，不手写替代类。
- Standards impact: MC-ARCH-001, MC-MODEL-001, MC-CONVERT-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 1 保留生成后缀。Rule 3 使用生成器的 Lombok 与 BaseConverter。Rule 10 时间列使用 java.time。Rule 11 包保持 Light。
- Implementation pseudocode:

```text
Do not type this file by hand.
Run /Users/mario/SelfProject/Egon-COLA/scripts/egon-codegen.sh plan --config enjoyshop-service/enjoyshop-service-cart/codegen/cart-item.json.
Require EGON_CODEGEN_CLASSPATH. A missing classpath prints BLOCKED_TOOLING and exits 7. Do not download dependencies.
Read the v2 plan JSON. This path must be an ADD with no CONFLICT.
Then run apply --plan the printed plan id --config the same json.
If the v2 plan uses a different path for the same artifact, keep the plan path and do not create a second class.
```

- Verification contribution: v2 plan 的文件清单包含该路径，或在路径不同时包含同一 artifact。
- After this file: 该文件由生成器拥有。后续业务修改会使再次 apply 产生 CONFLICT，因此不再对它重跑生成器。

#### File 7 — `GENERATED enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/infrastructure/cart/repo/CartItemRepository.java`

- Purpose: 由 backend-crud 生成该目录文件。
- Symbols: CartItemRepository.java
- Repository evidence: GenerationScopeValidator 对表 cart_item 使用 stem CartItem，领域模型后缀 BO。
- Dependencies and consumers: Step 3 已去掉样例 CREATE TABLE users。生成器只消费 cart_item。
- Why now: 目录类型必须由生成器先落盘，后面的策略才能修改领域服务。
- Contract/signature changes: PO 只含业务列。Mapper XML 仍映射继承列。events.enabled 是 false。
- Input/output and state mapping: CREATE INDEX 不参与建模。软删除唯一性由 Step 9 用 PostgreSQL 执行证明。
- Error and edge behavior: plan 出现 CONFLICT 或 BLOCKED_TOOLING 时停止，不手写替代类。
- Standards impact: MC-ARCH-001, MC-MODEL-001, MC-CONVERT-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 1 保留生成后缀。Rule 3 使用生成器的 Lombok 与 BaseConverter。Rule 10 时间列使用 java.time。Rule 11 包保持 Light。
- Implementation pseudocode:

```text
Do not type this file by hand.
Run /Users/mario/SelfProject/Egon-COLA/scripts/egon-codegen.sh plan --config enjoyshop-service/enjoyshop-service-cart/codegen/cart-item.json.
Require EGON_CODEGEN_CLASSPATH. A missing classpath prints BLOCKED_TOOLING and exits 7. Do not download dependencies.
Read the v2 plan JSON. This path must be an ADD with no CONFLICT.
Then run apply --plan the printed plan id --config the same json.
If the v2 plan uses a different path for the same artifact, keep the plan path and do not create a second class.
```

- Verification contribution: v2 plan 的文件清单包含该路径，或在路径不同时包含同一 artifact。
- After this file: 该文件由生成器拥有。后续业务修改会使再次 apply 产生 CONFLICT，因此不再对它重跑生成器。

#### File 8 — `GENERATED enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/domain/cart/CartItemBO.java`

- Purpose: 由 backend-crud 生成该目录文件。
- Symbols: CartItemBO.java
- Repository evidence: GenerationScopeValidator 对表 cart_item 使用 stem CartItem，领域模型后缀 BO。
- Dependencies and consumers: Step 3 已去掉样例 CREATE TABLE users。生成器只消费 cart_item。
- Why now: 目录类型必须由生成器先落盘，后面的策略才能修改领域服务。
- Contract/signature changes: PO 只含业务列。Mapper XML 仍映射继承列。events.enabled 是 false。
- Input/output and state mapping: CREATE INDEX 不参与建模。软删除唯一性由 Step 9 用 PostgreSQL 执行证明。
- Error and edge behavior: plan 出现 CONFLICT 或 BLOCKED_TOOLING 时停止，不手写替代类。
- Standards impact: MC-ARCH-001, MC-MODEL-001, MC-CONVERT-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 1 保留生成后缀。Rule 3 使用生成器的 Lombok 与 BaseConverter。Rule 10 时间列使用 java.time。Rule 11 包保持 Light。
- Implementation pseudocode:

```text
Do not type this file by hand.
Run /Users/mario/SelfProject/Egon-COLA/scripts/egon-codegen.sh plan --config enjoyshop-service/enjoyshop-service-cart/codegen/cart-item.json.
Require EGON_CODEGEN_CLASSPATH. A missing classpath prints BLOCKED_TOOLING and exits 7. Do not download dependencies.
Read the v2 plan JSON. This path must be an ADD with no CONFLICT.
Then run apply --plan the printed plan id --config the same json.
If the v2 plan uses a different path for the same artifact, keep the plan path and do not create a second class.
```

- Verification contribution: v2 plan 的文件清单包含该路径，或在路径不同时包含同一 artifact。
- After this file: 该文件由生成器拥有。后续业务修改会使再次 apply 产生 CONFLICT，因此不再对它重跑生成器。

#### File 9 — `GENERATED enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/domain/cart/CartItemDomainQuery.java`

- Purpose: 由 backend-crud 生成该目录文件。
- Symbols: CartItemDomainQuery.java
- Repository evidence: GenerationScopeValidator 对表 cart_item 使用 stem CartItem，领域模型后缀 BO。
- Dependencies and consumers: Step 3 已去掉样例 CREATE TABLE users。生成器只消费 cart_item。
- Why now: 目录类型必须由生成器先落盘，后面的策略才能修改领域服务。
- Contract/signature changes: PO 只含业务列。Mapper XML 仍映射继承列。events.enabled 是 false。
- Input/output and state mapping: CREATE INDEX 不参与建模。软删除唯一性由 Step 9 用 PostgreSQL 执行证明。
- Error and edge behavior: plan 出现 CONFLICT 或 BLOCKED_TOOLING 时停止，不手写替代类。
- Standards impact: MC-ARCH-001, MC-MODEL-001, MC-CONVERT-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 1 保留生成后缀。Rule 3 使用生成器的 Lombok 与 BaseConverter。Rule 10 时间列使用 java.time。Rule 11 包保持 Light。
- Implementation pseudocode:

```text
Do not type this file by hand.
Run /Users/mario/SelfProject/Egon-COLA/scripts/egon-codegen.sh plan --config enjoyshop-service/enjoyshop-service-cart/codegen/cart-item.json.
Require EGON_CODEGEN_CLASSPATH. A missing classpath prints BLOCKED_TOOLING and exits 7. Do not download dependencies.
Read the v2 plan JSON. This path must be an ADD with no CONFLICT.
Then run apply --plan the printed plan id --config the same json.
If the v2 plan uses a different path for the same artifact, keep the plan path and do not create a second class.
```

- Verification contribution: v2 plan 的文件清单包含该路径，或在路径不同时包含同一 artifact。
- After this file: 该文件由生成器拥有。后续业务修改会使再次 apply 产生 CONFLICT，因此不再对它重跑生成器。

#### File 10 — `GENERATED enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/domain/cart/CartItemDomainService.java`

- Purpose: 由 backend-crud 生成该目录文件。
- Symbols: CartItemDomainService.java
- Repository evidence: GenerationScopeValidator 对表 cart_item 使用 stem CartItem，领域模型后缀 BO。
- Dependencies and consumers: Step 3 已去掉样例 CREATE TABLE users。生成器只消费 cart_item。
- Why now: 目录类型必须由生成器先落盘，后面的策略才能修改领域服务。
- Contract/signature changes: PO 只含业务列。Mapper XML 仍映射继承列。events.enabled 是 false。
- Input/output and state mapping: CREATE INDEX 不参与建模。软删除唯一性由 Step 9 用 PostgreSQL 执行证明。
- Error and edge behavior: plan 出现 CONFLICT 或 BLOCKED_TOOLING 时停止，不手写替代类。
- Standards impact: MC-ARCH-001, MC-MODEL-001, MC-CONVERT-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 1 保留生成后缀。Rule 3 使用生成器的 Lombok 与 BaseConverter。Rule 10 时间列使用 java.time。Rule 11 包保持 Light。
- Implementation pseudocode:

```text
Do not type this file by hand.
Run /Users/mario/SelfProject/Egon-COLA/scripts/egon-codegen.sh plan --config enjoyshop-service/enjoyshop-service-cart/codegen/cart-item.json.
Require EGON_CODEGEN_CLASSPATH. A missing classpath prints BLOCKED_TOOLING and exits 7. Do not download dependencies.
Read the v2 plan JSON. This path must be an ADD with no CONFLICT.
Then run apply --plan the printed plan id --config the same json.
If the v2 plan uses a different path for the same artifact, keep the plan path and do not create a second class.
```

- Verification contribution: v2 plan 的文件清单包含该路径，或在路径不同时包含同一 artifact。
- After this file: 该文件由生成器拥有。后续业务修改会使再次 apply 产生 CONFLICT，因此不再对它重跑生成器。

#### File 11 — `GENERATED enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/infrastructure/cart/service/impl/CartItemDomainServiceImpl.java`

- Purpose: 由 backend-crud 生成该目录文件。
- Symbols: CartItemDomainServiceImpl.java
- Repository evidence: GenerationScopeValidator 对表 cart_item 使用 stem CartItem，领域模型后缀 BO。
- Dependencies and consumers: Step 3 已去掉样例 CREATE TABLE users。生成器只消费 cart_item。
- Why now: 目录类型必须由生成器先落盘，后面的策略才能修改领域服务。
- Contract/signature changes: PO 只含业务列。Mapper XML 仍映射继承列。events.enabled 是 false。
- Input/output and state mapping: CREATE INDEX 不参与建模。软删除唯一性由 Step 9 用 PostgreSQL 执行证明。
- Error and edge behavior: plan 出现 CONFLICT 或 BLOCKED_TOOLING 时停止，不手写替代类。
- Standards impact: MC-ARCH-001, MC-MODEL-001, MC-CONVERT-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 1 保留生成后缀。Rule 3 使用生成器的 Lombok 与 BaseConverter。Rule 10 时间列使用 java.time。Rule 11 包保持 Light。
- Implementation pseudocode:

```text
Do not type this file by hand.
Run /Users/mario/SelfProject/Egon-COLA/scripts/egon-codegen.sh plan --config enjoyshop-service/enjoyshop-service-cart/codegen/cart-item.json.
Require EGON_CODEGEN_CLASSPATH. A missing classpath prints BLOCKED_TOOLING and exits 7. Do not download dependencies.
Read the v2 plan JSON. This path must be an ADD with no CONFLICT.
Then run apply --plan the printed plan id --config the same json.
If the v2 plan uses a different path for the same artifact, keep the plan path and do not create a second class.
```

- Verification contribution: v2 plan 的文件清单包含该路径，或在路径不同时包含同一 artifact。
- After this file: 该文件由生成器拥有。后续业务修改会使再次 apply 产生 CONFLICT，因此不再对它重跑生成器。

#### File 12 — `GENERATED enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/application/cart/CreateCartItemCommand.java`

- Purpose: 由 backend-crud 生成该目录文件。
- Symbols: CreateCartItemCommand.java
- Repository evidence: GenerationScopeValidator 对表 cart_item 使用 stem CartItem，领域模型后缀 BO。
- Dependencies and consumers: Step 3 已去掉样例 CREATE TABLE users。生成器只消费 cart_item。
- Why now: 目录类型必须由生成器先落盘，后面的策略才能修改领域服务。
- Contract/signature changes: PO 只含业务列。Mapper XML 仍映射继承列。events.enabled 是 false。
- Input/output and state mapping: CREATE INDEX 不参与建模。软删除唯一性由 Step 9 用 PostgreSQL 执行证明。
- Error and edge behavior: plan 出现 CONFLICT 或 BLOCKED_TOOLING 时停止，不手写替代类。
- Standards impact: MC-ARCH-001, MC-MODEL-001, MC-CONVERT-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 1 保留生成后缀。Rule 3 使用生成器的 Lombok 与 BaseConverter。Rule 10 时间列使用 java.time。Rule 11 包保持 Light。
- Implementation pseudocode:

```text
Do not type this file by hand.
Run /Users/mario/SelfProject/Egon-COLA/scripts/egon-codegen.sh plan --config enjoyshop-service/enjoyshop-service-cart/codegen/cart-item.json.
Require EGON_CODEGEN_CLASSPATH. A missing classpath prints BLOCKED_TOOLING and exits 7. Do not download dependencies.
Read the v2 plan JSON. This path must be an ADD with no CONFLICT.
Then run apply --plan the printed plan id --config the same json.
If the v2 plan uses a different path for the same artifact, keep the plan path and do not create a second class.
```

- Verification contribution: v2 plan 的文件清单包含该路径，或在路径不同时包含同一 artifact。
- After this file: 该文件由生成器拥有。后续业务修改会使再次 apply 产生 CONFLICT，因此不再对它重跑生成器。

#### File 13 — `GENERATED enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/application/cart/UpdateCartItemCommand.java`

- Purpose: 由 backend-crud 生成该目录文件。
- Symbols: UpdateCartItemCommand.java
- Repository evidence: GenerationScopeValidator 对表 cart_item 使用 stem CartItem，领域模型后缀 BO。
- Dependencies and consumers: Step 3 已去掉样例 CREATE TABLE users。生成器只消费 cart_item。
- Why now: 目录类型必须由生成器先落盘，后面的策略才能修改领域服务。
- Contract/signature changes: PO 只含业务列。Mapper XML 仍映射继承列。events.enabled 是 false。
- Input/output and state mapping: CREATE INDEX 不参与建模。软删除唯一性由 Step 9 用 PostgreSQL 执行证明。
- Error and edge behavior: plan 出现 CONFLICT 或 BLOCKED_TOOLING 时停止，不手写替代类。
- Standards impact: MC-ARCH-001, MC-MODEL-001, MC-CONVERT-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 1 保留生成后缀。Rule 3 使用生成器的 Lombok 与 BaseConverter。Rule 10 时间列使用 java.time。Rule 11 包保持 Light。
- Implementation pseudocode:

```text
Do not type this file by hand.
Run /Users/mario/SelfProject/Egon-COLA/scripts/egon-codegen.sh plan --config enjoyshop-service/enjoyshop-service-cart/codegen/cart-item.json.
Require EGON_CODEGEN_CLASSPATH. A missing classpath prints BLOCKED_TOOLING and exits 7. Do not download dependencies.
Read the v2 plan JSON. This path must be an ADD with no CONFLICT.
Then run apply --plan the printed plan id --config the same json.
If the v2 plan uses a different path for the same artifact, keep the plan path and do not create a second class.
```

- Verification contribution: v2 plan 的文件清单包含该路径，或在路径不同时包含同一 artifact。
- After this file: 该文件由生成器拥有。后续业务修改会使再次 apply 产生 CONFLICT，因此不再对它重跑生成器。

#### File 14 — `GENERATED enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/application/cart/DeleteCartItemCommand.java`

- Purpose: 由 backend-crud 生成该目录文件。
- Symbols: DeleteCartItemCommand.java
- Repository evidence: GenerationScopeValidator 对表 cart_item 使用 stem CartItem，领域模型后缀 BO。
- Dependencies and consumers: Step 3 已去掉样例 CREATE TABLE users。生成器只消费 cart_item。
- Why now: 目录类型必须由生成器先落盘，后面的策略才能修改领域服务。
- Contract/signature changes: PO 只含业务列。Mapper XML 仍映射继承列。events.enabled 是 false。
- Input/output and state mapping: CREATE INDEX 不参与建模。软删除唯一性由 Step 9 用 PostgreSQL 执行证明。
- Error and edge behavior: plan 出现 CONFLICT 或 BLOCKED_TOOLING 时停止，不手写替代类。
- Standards impact: MC-ARCH-001, MC-MODEL-001, MC-CONVERT-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 1 保留生成后缀。Rule 3 使用生成器的 Lombok 与 BaseConverter。Rule 10 时间列使用 java.time。Rule 11 包保持 Light。
- Implementation pseudocode:

```text
Do not type this file by hand.
Run /Users/mario/SelfProject/Egon-COLA/scripts/egon-codegen.sh plan --config enjoyshop-service/enjoyshop-service-cart/codegen/cart-item.json.
Require EGON_CODEGEN_CLASSPATH. A missing classpath prints BLOCKED_TOOLING and exits 7. Do not download dependencies.
Read the v2 plan JSON. This path must be an ADD with no CONFLICT.
Then run apply --plan the printed plan id --config the same json.
If the v2 plan uses a different path for the same artifact, keep the plan path and do not create a second class.
```

- Verification contribution: v2 plan 的文件清单包含该路径，或在路径不同时包含同一 artifact。
- After this file: 该文件由生成器拥有。后续业务修改会使再次 apply 产生 CONFLICT，因此不再对它重跑生成器。

#### File 15 — `GENERATED enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/application/cart/CartItemDetailQuery.java`

- Purpose: 由 backend-crud 生成该目录文件。
- Symbols: CartItemDetailQuery.java
- Repository evidence: GenerationScopeValidator 对表 cart_item 使用 stem CartItem，领域模型后缀 BO。
- Dependencies and consumers: Step 3 已去掉样例 CREATE TABLE users。生成器只消费 cart_item。
- Why now: 目录类型必须由生成器先落盘，后面的策略才能修改领域服务。
- Contract/signature changes: PO 只含业务列。Mapper XML 仍映射继承列。events.enabled 是 false。
- Input/output and state mapping: CREATE INDEX 不参与建模。软删除唯一性由 Step 9 用 PostgreSQL 执行证明。
- Error and edge behavior: plan 出现 CONFLICT 或 BLOCKED_TOOLING 时停止，不手写替代类。
- Standards impact: MC-ARCH-001, MC-MODEL-001, MC-CONVERT-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 1 保留生成后缀。Rule 3 使用生成器的 Lombok 与 BaseConverter。Rule 10 时间列使用 java.time。Rule 11 包保持 Light。
- Implementation pseudocode:

```text
Do not type this file by hand.
Run /Users/mario/SelfProject/Egon-COLA/scripts/egon-codegen.sh plan --config enjoyshop-service/enjoyshop-service-cart/codegen/cart-item.json.
Require EGON_CODEGEN_CLASSPATH. A missing classpath prints BLOCKED_TOOLING and exits 7. Do not download dependencies.
Read the v2 plan JSON. This path must be an ADD with no CONFLICT.
Then run apply --plan the printed plan id --config the same json.
If the v2 plan uses a different path for the same artifact, keep the plan path and do not create a second class.
```

- Verification contribution: v2 plan 的文件清单包含该路径，或在路径不同时包含同一 artifact。
- After this file: 该文件由生成器拥有。后续业务修改会使再次 apply 产生 CONFLICT，因此不再对它重跑生成器。

#### File 16 — `GENERATED enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/application/cart/CartItemPageQuery.java`

- Purpose: 由 backend-crud 生成该目录文件。
- Symbols: CartItemPageQuery.java
- Repository evidence: GenerationScopeValidator 对表 cart_item 使用 stem CartItem，领域模型后缀 BO。
- Dependencies and consumers: Step 3 已去掉样例 CREATE TABLE users。生成器只消费 cart_item。
- Why now: 目录类型必须由生成器先落盘，后面的策略才能修改领域服务。
- Contract/signature changes: PO 只含业务列。Mapper XML 仍映射继承列。events.enabled 是 false。
- Input/output and state mapping: CREATE INDEX 不参与建模。软删除唯一性由 Step 9 用 PostgreSQL 执行证明。
- Error and edge behavior: plan 出现 CONFLICT 或 BLOCKED_TOOLING 时停止，不手写替代类。
- Standards impact: MC-ARCH-001, MC-MODEL-001, MC-CONVERT-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 1 保留生成后缀。Rule 3 使用生成器的 Lombok 与 BaseConverter。Rule 10 时间列使用 java.time。Rule 11 包保持 Light。
- Implementation pseudocode:

```text
Do not type this file by hand.
Run /Users/mario/SelfProject/Egon-COLA/scripts/egon-codegen.sh plan --config enjoyshop-service/enjoyshop-service-cart/codegen/cart-item.json.
Require EGON_CODEGEN_CLASSPATH. A missing classpath prints BLOCKED_TOOLING and exits 7. Do not download dependencies.
Read the v2 plan JSON. This path must be an ADD with no CONFLICT.
Then run apply --plan the printed plan id --config the same json.
If the v2 plan uses a different path for the same artifact, keep the plan path and do not create a second class.
```

- Verification contribution: v2 plan 的文件清单包含该路径，或在路径不同时包含同一 artifact。
- After this file: 该文件由生成器拥有。后续业务修改会使再次 apply 产生 CONFLICT，因此不再对它重跑生成器。

#### File 17 — `GENERATED enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/application/cart/CartItemResult.java`

- Purpose: 由 backend-crud 生成该目录文件。
- Symbols: CartItemResult.java
- Repository evidence: GenerationScopeValidator 对表 cart_item 使用 stem CartItem，领域模型后缀 BO。
- Dependencies and consumers: Step 3 已去掉样例 CREATE TABLE users。生成器只消费 cart_item。
- Why now: 目录类型必须由生成器先落盘，后面的策略才能修改领域服务。
- Contract/signature changes: PO 只含业务列。Mapper XML 仍映射继承列。events.enabled 是 false。
- Input/output and state mapping: CREATE INDEX 不参与建模。软删除唯一性由 Step 9 用 PostgreSQL 执行证明。
- Error and edge behavior: plan 出现 CONFLICT 或 BLOCKED_TOOLING 时停止，不手写替代类。
- Standards impact: MC-ARCH-001, MC-MODEL-001, MC-CONVERT-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 1 保留生成后缀。Rule 3 使用生成器的 Lombok 与 BaseConverter。Rule 10 时间列使用 java.time。Rule 11 包保持 Light。
- Implementation pseudocode:

```text
Do not type this file by hand.
Run /Users/mario/SelfProject/Egon-COLA/scripts/egon-codegen.sh plan --config enjoyshop-service/enjoyshop-service-cart/codegen/cart-item.json.
Require EGON_CODEGEN_CLASSPATH. A missing classpath prints BLOCKED_TOOLING and exits 7. Do not download dependencies.
Read the v2 plan JSON. This path must be an ADD with no CONFLICT.
Then run apply --plan the printed plan id --config the same json.
If the v2 plan uses a different path for the same artifact, keep the plan path and do not create a second class.
```

- Verification contribution: v2 plan 的文件清单包含该路径，或在路径不同时包含同一 artifact。
- After this file: 该文件由生成器拥有。后续业务修改会使再次 apply 产生 CONFLICT，因此不再对它重跑生成器。

#### File 18 — `GENERATED enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/infrastructure/cart/converter/CartItemPersistenceConverter.java`

- Purpose: 由 backend-crud 生成该目录文件。
- Symbols: CartItemPersistenceConverter.java
- Repository evidence: GenerationScopeValidator 对表 cart_item 使用 stem CartItem，领域模型后缀 BO。
- Dependencies and consumers: Step 3 已去掉样例 CREATE TABLE users。生成器只消费 cart_item。
- Why now: 目录类型必须由生成器先落盘，后面的策略才能修改领域服务。
- Contract/signature changes: PO 只含业务列。Mapper XML 仍映射继承列。events.enabled 是 false。
- Input/output and state mapping: CREATE INDEX 不参与建模。软删除唯一性由 Step 9 用 PostgreSQL 执行证明。
- Error and edge behavior: plan 出现 CONFLICT 或 BLOCKED_TOOLING 时停止，不手写替代类。
- Standards impact: MC-ARCH-001, MC-MODEL-001, MC-CONVERT-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 1 保留生成后缀。Rule 3 使用生成器的 Lombok 与 BaseConverter。Rule 10 时间列使用 java.time。Rule 11 包保持 Light。
- Implementation pseudocode:

```text
Do not type this file by hand.
Run /Users/mario/SelfProject/Egon-COLA/scripts/egon-codegen.sh plan --config enjoyshop-service/enjoyshop-service-cart/codegen/cart-item.json.
Require EGON_CODEGEN_CLASSPATH. A missing classpath prints BLOCKED_TOOLING and exits 7. Do not download dependencies.
Read the v2 plan JSON. This path must be an ADD with no CONFLICT.
Then run apply --plan the printed plan id --config the same json.
If the v2 plan uses a different path for the same artifact, keep the plan path and do not create a second class.
```

- Verification contribution: v2 plan 的文件清单包含该路径，或在路径不同时包含同一 artifact。
- After this file: 该文件由生成器拥有。后续业务修改会使再次 apply 产生 CONFLICT，因此不再对它重跑生成器。

#### File 19 — `GENERATED enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/application/cart/converter/CreateCartItemCommandConverter.java`

- Purpose: 由 backend-crud 生成该目录文件。
- Symbols: CreateCartItemCommandConverter.java
- Repository evidence: GenerationScopeValidator 对表 cart_item 使用 stem CartItem，领域模型后缀 BO。
- Dependencies and consumers: Step 3 已去掉样例 CREATE TABLE users。生成器只消费 cart_item。
- Why now: 目录类型必须由生成器先落盘，后面的策略才能修改领域服务。
- Contract/signature changes: PO 只含业务列。Mapper XML 仍映射继承列。events.enabled 是 false。
- Input/output and state mapping: CREATE INDEX 不参与建模。软删除唯一性由 Step 9 用 PostgreSQL 执行证明。
- Error and edge behavior: plan 出现 CONFLICT 或 BLOCKED_TOOLING 时停止，不手写替代类。
- Standards impact: MC-ARCH-001, MC-MODEL-001, MC-CONVERT-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 1 保留生成后缀。Rule 3 使用生成器的 Lombok 与 BaseConverter。Rule 10 时间列使用 java.time。Rule 11 包保持 Light。
- Implementation pseudocode:

```text
Do not type this file by hand.
Run /Users/mario/SelfProject/Egon-COLA/scripts/egon-codegen.sh plan --config enjoyshop-service/enjoyshop-service-cart/codegen/cart-item.json.
Require EGON_CODEGEN_CLASSPATH. A missing classpath prints BLOCKED_TOOLING and exits 7. Do not download dependencies.
Read the v2 plan JSON. This path must be an ADD with no CONFLICT.
Then run apply --plan the printed plan id --config the same json.
If the v2 plan uses a different path for the same artifact, keep the plan path and do not create a second class.
```

- Verification contribution: v2 plan 的文件清单包含该路径，或在路径不同时包含同一 artifact。
- After this file: 该文件由生成器拥有。后续业务修改会使再次 apply 产生 CONFLICT，因此不再对它重跑生成器。

#### File 20 — `GENERATED enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/application/cart/converter/UpdateCartItemCommandConverter.java`

- Purpose: 由 backend-crud 生成该目录文件。
- Symbols: UpdateCartItemCommandConverter.java
- Repository evidence: GenerationScopeValidator 对表 cart_item 使用 stem CartItem，领域模型后缀 BO。
- Dependencies and consumers: Step 3 已去掉样例 CREATE TABLE users。生成器只消费 cart_item。
- Why now: 目录类型必须由生成器先落盘，后面的策略才能修改领域服务。
- Contract/signature changes: PO 只含业务列。Mapper XML 仍映射继承列。events.enabled 是 false。
- Input/output and state mapping: CREATE INDEX 不参与建模。软删除唯一性由 Step 9 用 PostgreSQL 执行证明。
- Error and edge behavior: plan 出现 CONFLICT 或 BLOCKED_TOOLING 时停止，不手写替代类。
- Standards impact: MC-ARCH-001, MC-MODEL-001, MC-CONVERT-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 1 保留生成后缀。Rule 3 使用生成器的 Lombok 与 BaseConverter。Rule 10 时间列使用 java.time。Rule 11 包保持 Light。
- Implementation pseudocode:

```text
Do not type this file by hand.
Run /Users/mario/SelfProject/Egon-COLA/scripts/egon-codegen.sh plan --config enjoyshop-service/enjoyshop-service-cart/codegen/cart-item.json.
Require EGON_CODEGEN_CLASSPATH. A missing classpath prints BLOCKED_TOOLING and exits 7. Do not download dependencies.
Read the v2 plan JSON. This path must be an ADD with no CONFLICT.
Then run apply --plan the printed plan id --config the same json.
If the v2 plan uses a different path for the same artifact, keep the plan path and do not create a second class.
```

- Verification contribution: v2 plan 的文件清单包含该路径，或在路径不同时包含同一 artifact。
- After this file: 该文件由生成器拥有。后续业务修改会使再次 apply 产生 CONFLICT，因此不再对它重跑生成器。

#### File 21 — `GENERATED enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/application/cart/converter/DeleteCartItemCommandConverter.java`

- Purpose: 由 backend-crud 生成该目录文件。
- Symbols: DeleteCartItemCommandConverter.java
- Repository evidence: GenerationScopeValidator 对表 cart_item 使用 stem CartItem，领域模型后缀 BO。
- Dependencies and consumers: Step 3 已去掉样例 CREATE TABLE users。生成器只消费 cart_item。
- Why now: 目录类型必须由生成器先落盘，后面的策略才能修改领域服务。
- Contract/signature changes: PO 只含业务列。Mapper XML 仍映射继承列。events.enabled 是 false。
- Input/output and state mapping: CREATE INDEX 不参与建模。软删除唯一性由 Step 9 用 PostgreSQL 执行证明。
- Error and edge behavior: plan 出现 CONFLICT 或 BLOCKED_TOOLING 时停止，不手写替代类。
- Standards impact: MC-ARCH-001, MC-MODEL-001, MC-CONVERT-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 1 保留生成后缀。Rule 3 使用生成器的 Lombok 与 BaseConverter。Rule 10 时间列使用 java.time。Rule 11 包保持 Light。
- Implementation pseudocode:

```text
Do not type this file by hand.
Run /Users/mario/SelfProject/Egon-COLA/scripts/egon-codegen.sh plan --config enjoyshop-service/enjoyshop-service-cart/codegen/cart-item.json.
Require EGON_CODEGEN_CLASSPATH. A missing classpath prints BLOCKED_TOOLING and exits 7. Do not download dependencies.
Read the v2 plan JSON. This path must be an ADD with no CONFLICT.
Then run apply --plan the printed plan id --config the same json.
If the v2 plan uses a different path for the same artifact, keep the plan path and do not create a second class.
```

- Verification contribution: v2 plan 的文件清单包含该路径，或在路径不同时包含同一 artifact。
- After this file: 该文件由生成器拥有。后续业务修改会使再次 apply 产生 CONFLICT，因此不再对它重跑生成器。

#### File 22 — `GENERATED enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/application/cart/converter/CartItemResultConverter.java`

- Purpose: 由 backend-crud 生成该目录文件。
- Symbols: CartItemResultConverter.java
- Repository evidence: GenerationScopeValidator 对表 cart_item 使用 stem CartItem，领域模型后缀 BO。
- Dependencies and consumers: Step 3 已去掉样例 CREATE TABLE users。生成器只消费 cart_item。
- Why now: 目录类型必须由生成器先落盘，后面的策略才能修改领域服务。
- Contract/signature changes: PO 只含业务列。Mapper XML 仍映射继承列。events.enabled 是 false。
- Input/output and state mapping: CREATE INDEX 不参与建模。软删除唯一性由 Step 9 用 PostgreSQL 执行证明。
- Error and edge behavior: plan 出现 CONFLICT 或 BLOCKED_TOOLING 时停止，不手写替代类。
- Standards impact: MC-ARCH-001, MC-MODEL-001, MC-CONVERT-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 1 保留生成后缀。Rule 3 使用生成器的 Lombok 与 BaseConverter。Rule 10 时间列使用 java.time。Rule 11 包保持 Light。
- Implementation pseudocode:

```text
Do not type this file by hand.
Run /Users/mario/SelfProject/Egon-COLA/scripts/egon-codegen.sh plan --config enjoyshop-service/enjoyshop-service-cart/codegen/cart-item.json.
Require EGON_CODEGEN_CLASSPATH. A missing classpath prints BLOCKED_TOOLING and exits 7. Do not download dependencies.
Read the v2 plan JSON. This path must be an ADD with no CONFLICT.
Then run apply --plan the printed plan id --config the same json.
If the v2 plan uses a different path for the same artifact, keep the plan path and do not create a second class.
```

- Verification contribution: v2 plan 的文件清单包含该路径，或在路径不同时包含同一 artifact。
- After this file: 该文件由生成器拥有。后续业务修改会使再次 apply 产生 CONFLICT，因此不再对它重跑生成器。

#### File 23 — `GENERATED enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/application/cart/CartItemManage.java`

- Purpose: 由 backend-crud 生成该目录文件。
- Symbols: CartItemManage.java
- Repository evidence: GenerationScopeValidator 对表 cart_item 使用 stem CartItem，领域模型后缀 BO。
- Dependencies and consumers: Step 3 已去掉样例 CREATE TABLE users。生成器只消费 cart_item。
- Why now: 目录类型必须由生成器先落盘，后面的策略才能修改领域服务。
- Contract/signature changes: PO 只含业务列。Mapper XML 仍映射继承列。events.enabled 是 false。
- Input/output and state mapping: CREATE INDEX 不参与建模。软删除唯一性由 Step 9 用 PostgreSQL 执行证明。
- Error and edge behavior: plan 出现 CONFLICT 或 BLOCKED_TOOLING 时停止，不手写替代类。
- Standards impact: MC-ARCH-001, MC-MODEL-001, MC-CONVERT-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 1 保留生成后缀。Rule 3 使用生成器的 Lombok 与 BaseConverter。Rule 10 时间列使用 java.time。Rule 11 包保持 Light。
- Implementation pseudocode:

```text
Do not type this file by hand.
Run /Users/mario/SelfProject/Egon-COLA/scripts/egon-codegen.sh plan --config enjoyshop-service/enjoyshop-service-cart/codegen/cart-item.json.
Require EGON_CODEGEN_CLASSPATH. A missing classpath prints BLOCKED_TOOLING and exits 7. Do not download dependencies.
Read the v2 plan JSON. This path must be an ADD with no CONFLICT.
Then run apply --plan the printed plan id --config the same json.
If the v2 plan uses a different path for the same artifact, keep the plan path and do not create a second class.
```

- Verification contribution: v2 plan 的文件清单包含该路径，或在路径不同时包含同一 artifact。
- After this file: 该文件由生成器拥有。后续业务修改会使再次 apply 产生 CONFLICT，因此不再对它重跑生成器。

#### File 24 — `GENERATED enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/application/cart/CartItemManageImpl.java`

- Purpose: 由 backend-crud 生成该目录文件。
- Symbols: CartItemManageImpl.java
- Repository evidence: GenerationScopeValidator 对表 cart_item 使用 stem CartItem，领域模型后缀 BO。
- Dependencies and consumers: Step 3 已去掉样例 CREATE TABLE users。生成器只消费 cart_item。
- Why now: 目录类型必须由生成器先落盘，后面的策略才能修改领域服务。
- Contract/signature changes: PO 只含业务列。Mapper XML 仍映射继承列。events.enabled 是 false。
- Input/output and state mapping: CREATE INDEX 不参与建模。软删除唯一性由 Step 9 用 PostgreSQL 执行证明。
- Error and edge behavior: plan 出现 CONFLICT 或 BLOCKED_TOOLING 时停止，不手写替代类。
- Standards impact: MC-ARCH-001, MC-MODEL-001, MC-CONVERT-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 1 保留生成后缀。Rule 3 使用生成器的 Lombok 与 BaseConverter。Rule 10 时间列使用 java.time。Rule 11 包保持 Light。
- Implementation pseudocode:

```text
Do not type this file by hand.
Run /Users/mario/SelfProject/Egon-COLA/scripts/egon-codegen.sh plan --config enjoyshop-service/enjoyshop-service-cart/codegen/cart-item.json.
Require EGON_CODEGEN_CLASSPATH. A missing classpath prints BLOCKED_TOOLING and exits 7. Do not download dependencies.
Read the v2 plan JSON. This path must be an ADD with no CONFLICT.
Then run apply --plan the printed plan id --config the same json.
If the v2 plan uses a different path for the same artifact, keep the plan path and do not create a second class.
```

- Verification contribution: v2 plan 的文件清单包含该路径，或在路径不同时包含同一 artifact。
- After this file: 该文件由生成器拥有。后续业务修改会使再次 apply 产生 CONFLICT，因此不再对它重跑生成器。

#### File 25 — `GENERATED enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/adapter/cart/CartItemController.java`

- Purpose: 由 backend-crud 生成该目录文件。
- Symbols: CartItemController.java
- Repository evidence: GenerationScopeValidator 对表 cart_item 使用 stem CartItem，领域模型后缀 BO。
- Dependencies and consumers: Step 3 已去掉样例 CREATE TABLE users。生成器只消费 cart_item。
- Why now: 目录类型必须由生成器先落盘，后面的策略才能修改领域服务。
- Contract/signature changes: PO 只含业务列。Mapper XML 仍映射继承列。events.enabled 是 false。
- Input/output and state mapping: CREATE INDEX 不参与建模。软删除唯一性由 Step 9 用 PostgreSQL 执行证明。
- Error and edge behavior: plan 出现 CONFLICT 或 BLOCKED_TOOLING 时停止，不手写替代类。
- Standards impact: MC-ARCH-001, MC-MODEL-001, MC-CONVERT-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 1 保留生成后缀。Rule 3 使用生成器的 Lombok 与 BaseConverter。Rule 10 时间列使用 java.time。Rule 11 包保持 Light。
- Implementation pseudocode:

```text
Do not type this file by hand.
Run /Users/mario/SelfProject/Egon-COLA/scripts/egon-codegen.sh plan --config enjoyshop-service/enjoyshop-service-cart/codegen/cart-item.json.
Require EGON_CODEGEN_CLASSPATH. A missing classpath prints BLOCKED_TOOLING and exits 7. Do not download dependencies.
Read the v2 plan JSON. This path must be an ADD with no CONFLICT.
Then run apply --plan the printed plan id --config the same json.
If the v2 plan uses a different path for the same artifact, keep the plan path and do not create a second class.
```

- Verification contribution: v2 plan 的文件清单包含该路径，或在路径不同时包含同一 artifact。
- After this file: 该文件由生成器拥有。后续业务修改会使再次 apply 产生 CONFLICT，因此不再对它重跑生成器。

#### File 26 — `MODIFY docs/egon/codegen/ddl-consumption-log.md`

- Purpose: 在 apply 成功后追加一条 classpath SQL 消费记录。
- Symbols: ddl-consumption-log.md
- Repository evidence: 该日志已存在，只能追加。它不是 ddl_history，也不表示执行了 PostgreSQL。
- Dependencies and consumers: Step 3 已去掉样例 CREATE TABLE users。生成器只消费 cart_item。
- Why now: 目录类型必须由生成器先落盘，后面的策略才能修改领域服务。
- Contract/signature changes: PO 只含业务列。Mapper XML 仍映射继承列。events.enabled 是 false。
- Input/output and state mapping: CREATE INDEX 不参与建模。软删除唯一性由 Step 9 用 PostgreSQL 执行证明。
- Error and edge behavior: plan 出现 CONFLICT 或 BLOCKED_TOOLING 时停止，不手写替代类。
- Standards impact: MC-ARCH-001, MC-MODEL-001, MC-CONVERT-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 1 保留生成后缀。Rule 3 使用生成器的 Lombok 与 BaseConverter。Rule 10 时间列使用 java.time。Rule 11 包保持 Light。
- Implementation pseudocode:

```text
Append one entry with Asia/Shanghai time, profile light, output root enjoyshop-service/enjoyshop-service-cart, and throughVersion 20261013_001.
Include the repository-relative path, SHA-256, and full SQL of V20261013_001.
Do not rewrite older entries and do not connect to a database.
```

- Verification contribution: v2 plan 的文件清单包含该路径，或在路径不同时包含同一 artifact。
- After this file: 该文件由生成器拥有。后续业务修改会使再次 apply 产生 CONFLICT，因此不再对它重跑生成器。


- Validation working directory: 仓库根目录 `/Users/mario/SelfProject/enjoyshop`
- Verification command: `EGON_CODEGEN_CLASSPATH="$EGON_CODEGEN_CLASSPATH" /Users/mario/SelfProject/Egon-COLA/scripts/egon-codegen.sh plan --config enjoyshop-service/enjoyshop-service-cart/codegen/cart-item.json`，审阅 JSON 后再 `apply --plan <plan-id> --config enjoyshop-service/enjoyshop-service-cart/codegen/cart-item.json`，然后 `mvn -pl enjoyshop-service/enjoyshop-service-cart -am -DskipTests compile`
- Expected result: plan 无 CONFLICT；apply 退出码 0；编译退出码 0。缺少 classpath 时退出码 7，本步停止。
- Failure returns to: SQL 的 EgonModel 列，或配置 JSON 的未知键。
- Completion criteria: `CartItemPO` 不重复声明 `tenantId` 与 `deletedAt`；日志已追加；`.egon/` 未进入提交。
- Rollback: 删除本步生成的源文件和 SQL 登记，不恢复样例表。已写坏的 v2 plan 用 `recover`，不手改 `.egon/codegen/state.json`。
- Commit paths: `enjoyshop-service/enjoyshop-service-cart/src/main/resources/db/egon-mp/V20261013_001__initialize_cart_schema.sql`, `enjoyshop-service/enjoyshop-service-cart/src/main/resources/db/egon-mp/repository-manifest.json`, `enjoyshop-service/enjoyshop-service-cart/codegen/cart-item.json`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/infrastructure/cart/po/CartItemPO.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/infrastructure/cart/dao/CartItemDAO.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/resources/mybatis/mapper/cart/CartItemDAO.xml`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/infrastructure/cart/repo/CartItemRepository.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/domain/cart/CartItemBO.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/domain/cart/CartItemDomainQuery.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/domain/cart/CartItemDomainService.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/infrastructure/cart/service/impl/CartItemDomainServiceImpl.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/application/cart/CreateCartItemCommand.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/application/cart/UpdateCartItemCommand.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/application/cart/DeleteCartItemCommand.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/application/cart/CartItemDetailQuery.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/application/cart/CartItemPageQuery.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/application/cart/CartItemResult.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/infrastructure/cart/converter/CartItemPersistenceConverter.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/application/cart/converter/CreateCartItemCommandConverter.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/application/cart/converter/UpdateCartItemCommandConverter.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/application/cart/converter/DeleteCartItemCommandConverter.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/application/cart/converter/CartItemResultConverter.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/application/cart/CartItemManage.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/application/cart/CartItemManageImpl.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/adapter/cart/CartItemController.java`, `docs/egon/codegen/ddl-consumption-log.md`
- Commit: `feat(cart): generate cart_item persistence from the managed schema`


### Step 5 — 用四个策略表达数量结果

- Requirements: `REQ-005` `REQ-006` `REQ-010`
- Dependencies: `Step 4`
- Baseline state: `CartItemBO`、`CartItemDomainService` 和 `CartItemRepository` 已由生成器落盘，但还没有差额策略。Manage 仍是生成出的普通 CRUD。
- Observable outcome: 首次加入冻结详情价；已有行在商品改价后仍用原单价重算金额和重量；结果数量不大于 0 时只写 `deleted_at`，数量保持删除前的正数；无有效行的负增量不调用 goods，也不插入。乘法溢出变成 `610506`。
- End state: 注册表按 `CartItemAdjustmentKind` 找到四个 Bean。重算、软删除和无行删除策略的字段里没有 `GoodsProductSnapshotClient`。goods 的 HTTP 实现还没写，只有接口。
- Test-first gate: `Required — CartItemAdjustmentStrategyTest 在四个策略类出现前不能编译`
- Manual Checks: `MC-PATTERN-001, MC-NAME-001, MC-MODEL-001, MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-VALID-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001`
- Literal Rules: `Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 9, Rule 10, Rule 11`
- Ordered files:

#### File 1 — `CREATE enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/application/cart/CartItemAdjustmentStrategyTest.java`

- Purpose: 在策略类出现之前固定冻结价、软删除、无行删除和溢出。
- Symbols: freezesPriceOnInsert, recomputesFromStoredPriceWhenCatalogPriceChanges, softDeleteKeepsPositiveQuantity, absentRemovalDoesNotCallGoods, overflowMapsTo610506
- Repository evidence: 规格 §14.1 与 TEST-002、TEST-004。goods 的领域测试用假协作对象，不启动 PostgreSQL。
- Dependencies and consumers: 下一步才有的四个策略、`GoodsProductSnapshotClient` 假对象和 `CartItemDomainService` 假对象。
- Why now: 这是红灯。生产策略还不存在。
- Contract/signature changes: 断言只有插入策略调用一次 `load`。重算策略在快照单价变化后仍把行金额写成旧单价乘新数量。
- Input/output and state mapping: 输入是已有 `CartItemBO` 或空，以及有符号差额。输出是更新后的 BO，或空表示不写。
- Error and edge behavior: 无行且差额为负时，假的 domain 没有 `insertActive` 调用。`Math.multiplyExact` 溢出被断言为 `CommonException` 码 `610506`。软删除后的 `quantity` 仍大于 0。
- Standards impact: MC-PATTERN-001, MC-TEST-001, MC-UTIL-001, MC-SCOPE-001
- Literal rule enforcement: Rule 5 断言只用精确整数运算。Rule 9 断言四个类分别存在，而不是一个含四段价格计算的方法。Rule 11 测试放在 application.cart。
- Implementation pseudocode:

```java
@Test
void recomputesFromStoredPriceWhenCatalogPriceChanges() {
    CartItemBO stored = new CartItemBO().setQuantity(2).setUnitPrice(100).setUnitWeight(5);
    GoodsProductSnapshotResult changed = snapshot().setUnitPrice(999);
    CartItemBO updated = recompute.applyInsideTransaction(stored, 1, changed);
    assertThat(updated.getUnitPrice()).isEqualTo(100);
    assertThat(updated.getQuantity()).isEqualTo(3);
    assertThat(updated.getLineAmount()).isEqualTo(300);
    assertThat(updated.getLineWeight()).isEqualTo(15);
    verify(goods, never()).load(anyLong(), anyLong(), any());
}
@Test
void overflowMapsTo610506() {
    CartItemBO stored = new CartItemBO().setQuantity(2).setUnitPrice(Integer.MAX_VALUE).setUnitWeight(1);
    CommonException error = assertThrows(CommonException.class,
            () -> recompute.applyInsideTransaction(stored, 1, null));
    assertThat(error.getCode()).isEqualTo(610506);
}
```

- Verification contribution: 本步末尾的聚焦测试。
- After this file: 该文件符合本步契约，后续文件才能编译。

#### File 2 — `CREATE enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/infrastructure/cart/GoodsProductSnapshotResult.java`

- Purpose: 给插入策略一个进程内快照，而不是把 goods 的 Java 类型引进来。
- Symbols: GoodsProductSnapshotResult
- Repository evidence: 商品模块的 `SpuResult` 与 `SkuResult` 已有 status、isMarketable、isDelete、price、weight、image。购物车不能依赖 goods 的类。
- Dependencies and consumers: Step 6 的 HTTP 实现填充它。插入策略只读它。
- Why now: 策略测试要能手造快照，不能等 HTTP 客户端。
- Contract/signature changes: class，字段覆盖可售判断和冻结列。没有用户名，没有请求价格。
- Input/output and state mapping: 空图片用空字符串表示，不使用 null 作为入库值。价格是分，重量是克。
- Error and edge behavior: 缺字段时插入策略视为不可售，不在这个类里发 HTTP。
- Standards impact: MC-MODEL-001, MC-NAME-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 1 类名以 Result 结尾。Rule 3 使用规定的 Lombok class 注解，不用 record。Rule 10 这个类没有时间字段。
- Implementation pseudocode:

```java
@Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain = true) @Builder
public class GoodsProductSnapshotResult {
    private Long spuId;
    private Long skuId;
    private String name;
    private String skuImage;
    private String spuImage;
    private Integer unitPrice;
    private Integer unitWeight;
    private Long category1Id;
    private Long category2Id;
    private Long category3Id;
    private String spuStatus;
    private Boolean marketable;
    private Boolean deleted;
    private String skuStatus;
}
```

- Verification contribution: 本步末尾的聚焦测试。
- After this file: 该文件符合本步契约，后续文件才能编译。

#### File 3 — `CREATE enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/infrastructure/cart/GoodsProductSnapshotClient.java`

- Purpose: 把 goods 调用留在插入策略的事务外方法上，本步只定接口。
- Symbols: GoodsProductSnapshotClient.load
- Repository evidence: 规格 §13 的适配器。cart 模块还没有这个类型。
- Dependencies and consumers: 只有 `InsertFromSnapshotStrategy` 注入它。Step 6 提供 `@Component("goodsProductSnapshotClient")` 的实现。
- Why now: 策略要先能编译，HTTP 细节仍属于下一步。
- Contract/signature changes: `GoodsProductSnapshotResult load(long spuId, long skuId, String authorizationHeader)`。
- Input/output and state mapping: 授权头只用于转发给 goods。返回值不含购物车所有者。
- Error and edge behavior: 接口本身不捕获超时。实现在 Step 6 把连接失败、超时、401、403 和 5xx 变成 `610505`。
- Standards impact: MC-ARCH-001, MC-REUSE-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 接口放在 infrastructure.cart，不放进 goods。Rule 4 的 Bean 名由实现类声明。
- Implementation pseudocode:

```java
public interface GoodsProductSnapshotClient {
    GoodsProductSnapshotResult load(long spuId, long skuId, String authorizationHeader);
}
// The authorizationHeader is forwarded unchanged. It is not parsed into a cart owner.
```

- Verification contribution: 本步末尾的聚焦测试。
- After this file: 该文件符合本步契约，后续文件才能编译。

#### File 4 — `CREATE enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/application/cart/CartItemAdjustmentKind.java`

- Purpose: 用枚举分类四种结果，避免 Manage 复制四套价格规则。
- Symbols: CartItemAdjustmentKind.classify
- Repository evidence: 规格 §13.1 的四种分类名。
- Dependencies and consumers: 注册表和 Step 6 的 Manage 都调用 `classify`。
- Why now: 策略实现之前需要稳定的分类。
- Contract/signature changes: `INSERT_FROM_SNAPSHOT`、`ABSENT_REMOVAL`、`RECOMPUTE_STORED`、`SOFT_DELETE`。
- Input/output and state mapping: 无行且差额为正是插入；无行且为负是缺席删除；有行且相加后不大于 0 是软删除；否则重算。
- Error and edge behavior: `Math.addExact` 溢出时抛 `610506`，不回绕。不读取 goods。
- Standards impact: MC-PATTERN-001, MC-JSON-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 6 枚举不把 ordinal 当业务码。Rule 9 分类只选择策略，不计算金额。Rule 5 使用 `Math.addExact`。
- Implementation pseudocode:

```java
public enum CartItemAdjustmentKind {
    INSERT_FROM_SNAPSHOT, ABSENT_REMOVAL, RECOMPUTE_STORED, SOFT_DELETE;
    public static CartItemAdjustmentKind classify(CartItemBO existing, int num) {
        if (existing == null) return num > 0 ? INSERT_FROM_SNAPSHOT : ABSENT_REMOVAL;
        try {
            int next = Math.addExact(existing.getQuantity(), num);
            return next <= 0 ? SOFT_DELETE : RECOMPUTE_STORED;
        } catch (ArithmeticException ex) {
            throw new CommonException(610506, "CART_QUANTITY_OVERFLOW", "cart quantity overflow");
        }
    }
}
```

- Verification contribution: 本步末尾的聚焦测试。
- After this file: 该文件符合本步契约，后续文件才能编译。

#### File 5 — `CREATE enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/application/cart/CartItemAdjustmentStrategy.java`

- Purpose: 把事务外取价和事务内落库分成两个方法。
- Symbols: loadOutsideTransaction, applyInsideTransaction
- Repository evidence: 规格要求 goods 不在数据库事务内，同时 TEST-002 要求策略自己决定是否调用 goods。
- Dependencies and consumers: 四个实现和注册表。
- Why now: 接口必须先于实现。
- Contract/signature changes: 取价方法可以返回 null。应用方法返回要写入的 BO；返回 null 表示不写。
- Input/output and state mapping: 现有行、差额和可选快照是输入。策略不解析 Authorization，不读取用户名。
- Error and edge behavior: 不可售、溢出和缺快照都抛 `CommonException`，不把数量写成 0。
- Standards impact: MC-PATTERN-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 9 这是策略接口，不是 Manage 里的分支。Rule 2 的命令校验留在 Step 6，策略只拒绝算不出来的数量。
- Implementation pseudocode:

```java
public interface CartItemAdjustmentStrategy {
    GoodsProductSnapshotResult loadOutsideTransaction(long spuId, long skuId, String authorizationHeader);
    CartItemBO applyInsideTransaction(CartItemBO locked, int num, GoodsProductSnapshotResult snapshot);
}
```

- Verification contribution: 本步末尾的聚焦测试。
- After this file: 该文件符合本步契约，后续文件才能编译。

#### File 6 — `CREATE enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/application/cart/InsertFromSnapshotStrategy.java`

- Purpose: 只有新行才取价，并在事务内冻结单价、单重和图片。
- Symbols: @Component("insertFromSnapshotStrategy")
- Repository evidence: 规格 ASM-004 与 ASM-006。生成器没有这个类。
- Dependencies and consumers: `@Qualifier("goodsProductSnapshotClient")` 与 `@Qualifier("cartItemDomainService")`。
- Why now: 测试已经要求首次加入调用 goods 并冻结价格。
- Contract/signature changes: 可售才调用 `insertActive`。图片先用 SKU，空白再用 SPU，仍空白则空字符串。
- Input/output and state mapping: 单价、行金额、应付和重量用整数。`payAmount` 等于 `lineAmount`。类目和名称来自快照，不来自请求。
- Error and edge behavior: HTTP 不是 200、SPU status 不是 `1`、不可上架、已删除、SKU status 不是 `1`、单价小于 1 或重量小于 0 时抛 `610504` 并且不调用 `insertActive`。名称超过 3000 或图片超过 500 同样拒绝。
- Standards impact: MC-PATTERN-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 4 使用 `@Slf4j`、`@RequiredArgsConstructor` 和两个 `@Qualifier`。Rule 5 使用 `Math.multiplyExact`。Rule 9 本类只处理插入。
- Implementation pseudocode:

```java
@Component("insertFromSnapshotStrategy")
@Slf4j @RequiredArgsConstructor
public class InsertFromSnapshotStrategy implements CartItemAdjustmentStrategy {
    @Qualifier("goodsProductSnapshotClient") private final GoodsProductSnapshotClient goods;
    @Qualifier("cartItemDomainService") private final CartItemDomainService domain;
    public GoodsProductSnapshotResult loadOutsideTransaction(long spuId, long skuId, String authorizationHeader) {
        return goods.load(spuId, skuId, authorizationHeader);
    }
    public CartItemBO applyInsideTransaction(CartItemBO locked, int num, GoodsProductSnapshotResult snapshot) {
        if (!sellable(snapshot)) throw new CommonException(610504, "CART_SKU_NOT_SELLABLE", "cart sku is not sellable");
        int amount = Math.multiplyExact(snapshot.getUnitPrice(), num);
        CartItemBO created = new CartItemBO().setQuantity(num).setUnitPrice(snapshot.getUnitPrice())
                .setUnitWeight(snapshot.getUnitWeight()).setLineAmount(amount).setPayAmount(amount)
                .setLineWeight(Math.multiplyExact(snapshot.getUnitWeight(), num));
        domain.insertActive(created);
        return created;
    }
}
```

- Verification contribution: 本步末尾的聚焦测试。
- After this file: 该文件符合本步契约，后续文件才能编译。

#### File 7 — `CREATE enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/application/cart/AbsentRemovalStrategy.java`

- Purpose: 无有效行时的负增量保持空车，并且不访问 goods。
- Symbols: @Component("absentRemovalStrategy")
- Repository evidence: 规格 §7.3：无行且负增量返回 200 且不写。
- Dependencies and consumers: 不注入 goods 客户端，也不注入 domain。
- Why now: 插入策略之后补上幂等删除。
- Contract/signature changes: 两个方法都不调用外部系统。`applyInsideTransaction` 返回 null。
- Input/output and state mapping: 没有新的 PO，没有 `deleted_at` 写入。
- Error and edge behavior: 即使调用方传入快照也丢弃，不插入。
- Standards impact: MC-PATTERN-001, MC-BEAN-001, MC-LOG-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 4 有 Bean 名和 `@Slf4j`，没有需要 `@Qualifier` 的依赖。Rule 9 不在这里重算价格。
- Implementation pseudocode:

```java
@Component("absentRemovalStrategy")
@Slf4j
public class AbsentRemovalStrategy implements CartItemAdjustmentStrategy {
    public GoodsProductSnapshotResult loadOutsideTransaction(long spuId, long skuId, String authorizationHeader) {
        return null;
    }
    public CartItemBO applyInsideTransaction(CartItemBO locked, int num, GoodsProductSnapshotResult snapshot) {
        return null;
    }
}
```

- Verification contribution: 本步末尾的聚焦测试。
- After this file: 该文件符合本步契约，后续文件才能编译。

#### File 8 — `CREATE enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/application/cart/RecomputeStoredPriceStrategy.java`

- Purpose: 已有行按冻结单价和单重重算，不刷新商品现价。
- Symbols: @Component("recomputeStoredPriceStrategy")
- Repository evidence: 规格 REQ-005。教程按车内价格计算，并补上重量没更新的缺陷。
- Dependencies and consumers: `@Qualifier("cartItemDomainService")`。禁止注入 goods 客户端。
- Why now: 测试要求旧单价不变。
- Contract/signature changes: 新数量是 `addExact(old, num)`。行金额和行重量用 `multiplyExact`。应付等于行金额。
- Input/output and state mapping: 单价和单重沿用 locked 上的值。快照参数被忽略。
- Error and edge behavior: 溢出抛 `610506`，不调用 `updateActive`。结果数量小于 1 不是本策略的路径。
- Standards impact: MC-PATTERN-001, MC-BEAN-001, MC-LOG-001, MC-UTIL-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 4 注入 domain 时使用 `@Qualifier`。Rule 5 只用 JDK `Math`。Rule 9 本类看不到 goods。
- Implementation pseudocode:

```java
@Component("recomputeStoredPriceStrategy")
@Slf4j @RequiredArgsConstructor
public class RecomputeStoredPriceStrategy implements CartItemAdjustmentStrategy {
    @Qualifier("cartItemDomainService") private final CartItemDomainService domain;
    public GoodsProductSnapshotResult loadOutsideTransaction(long spuId, long skuId, String authorizationHeader) {
        return null;
    }
    public CartItemBO applyInsideTransaction(CartItemBO locked, int num, GoodsProductSnapshotResult snapshot) {
        int quantity = Math.addExact(locked.getQuantity(), num);
        int amount = Math.multiplyExact(locked.getUnitPrice(), quantity);
        locked.setQuantity(quantity).setLineAmount(amount).setPayAmount(amount)
                .setLineWeight(Math.multiplyExact(locked.getUnitWeight(), quantity));
        domain.updateActive(locked);
        return locked;
    }
}
```

- Verification contribution: 本步末尾的聚焦测试。
- After this file: 该文件符合本步契约，后续文件才能编译。

#### File 9 — `CREATE enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/application/cart/SoftDeleteStrategy.java`

- Purpose: 结果数量不大于 0 时写墓碑，保留原来的正数量。
- Symbols: @Component("softDeleteStrategy")
- Repository evidence: 规格 §10.6。EgonModel 的 `deleted_at` 是 timestamp without time zone。
- Dependencies and consumers: `@Qualifier("cartItemDomainService")`。不注入 goods。
- Why now: 测试要求软删除不把数量写成 0，也不复活旧行。
- Contract/signature changes: 用 `Clock.systemUTC()` 得到 UTC 墙钟，截断到微秒，写入 `LocalDateTime`。
- Input/output and state mapping: `quantity` 保持锁定行上的正数。不更新单价。
- Error and edge behavior: 本策略不清除 `deleted_at`。再次购买由以后的插入策略新建一行。
- Standards impact: MC-PATTERN-001, MC-BEAN-001, MC-LOG-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 10 使用 `java.time.LocalDateTime` 与 `Clock.systemUTC()`，不用 `java.util.Date`。Rule 4 给 domain 加 `@Qualifier`。
- Implementation pseudocode:

```java
@Component("softDeleteStrategy")
@Slf4j @RequiredArgsConstructor
public class SoftDeleteStrategy implements CartItemAdjustmentStrategy {
    @Qualifier("cartItemDomainService") private final CartItemDomainService domain;
    public CartItemBO applyInsideTransaction(CartItemBO locked, int num, GoodsProductSnapshotResult snapshot) {
        LocalDateTime deletedAt = LocalDateTime.ofInstant(Clock.systemUTC().instant(), ZoneOffset.UTC)
                .truncatedTo(ChronoUnit.MICROS);
        locked.setDeletedAt(deletedAt);
        domain.softDelete(locked.getId(), deletedAt, locked.getUpdateUserId(), OffsetDateTime.now(Clock.systemUTC()));
        return locked;
    }
}
```

- Verification contribution: 本步末尾的聚焦测试。
- After this file: 该文件符合本步契约，后续文件才能编译。

#### File 10 — `CREATE enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/application/cart/CartItemAdjustmentStrategyRegistry.java`

- Purpose: 按枚举取策略。缺少实现时失败，而不是退回一段默认分支。
- Symbols: @Component("cartItemAdjustmentStrategyRegistry")
- Repository evidence: 规格 §13.1 的注册表。
- Dependencies and consumers: 四个策略 Bean。Step 6 的 Manage 只依赖注册表。
- Why now: 四个实现都已有 Bean 名。
- Contract/signature changes: `require(CartItemAdjustmentKind)` 返回对应策略。
- Input/output and state mapping: 构造器把四个策略放进 `EnumMap`。
- Error and edge behavior: 映射缺失抛 `610599`。不允许按字符串类名反射创建。
- Standards impact: MC-PATTERN-001, MC-BEAN-001, MC-LOG-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 4 四个构造参数都有 `@Qualifier`。Rule 9 注册表只做查找。Rule 11 不引入 Event。
- Implementation pseudocode:

```java
@Component("cartItemAdjustmentStrategyRegistry")
@Slf4j @RequiredArgsConstructor
public class CartItemAdjustmentStrategyRegistry {
    @Qualifier("insertFromSnapshotStrategy") private final CartItemAdjustmentStrategy insert;
    @Qualifier("absentRemovalStrategy") private final CartItemAdjustmentStrategy absent;
    @Qualifier("recomputeStoredPriceStrategy") private final CartItemAdjustmentStrategy recompute;
    @Qualifier("softDeleteStrategy") private final CartItemAdjustmentStrategy softDelete;
    public CartItemAdjustmentStrategy require(CartItemAdjustmentKind kind) {
        CartItemAdjustmentStrategy strategy = Map.of(CartItemAdjustmentKind.INSERT_FROM_SNAPSHOT, insert,
                CartItemAdjustmentKind.ABSENT_REMOVAL, absent,
                CartItemAdjustmentKind.RECOMPUTE_STORED, recompute,
                CartItemAdjustmentKind.SOFT_DELETE, softDelete).get(kind);
        if (strategy == null) throw new CommonException(610599, "CART_INTERNAL_ERROR", "cart internal error");
        return strategy;
    }
}
```

- Verification contribution: 本步末尾的聚焦测试。
- After this file: 该文件符合本步契约，后续文件才能编译。

#### File 11 — `MODIFY enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/domain/cart/CartItemDomainService.java`

- Purpose: 让策略能列出、锁定、更新和软删除有效行。
- Symbols: listActive, lockActive, insertActive, updateActive, softDelete
- Repository evidence: 生成器已有 CRUD 方法。规格 §11.2 的访问路径还没有这些名字。
- Dependencies and consumers: 四个策略和 Step 6 的列表编排。
- Why now: 策略方法要调用这些签名才能编译。
- Contract/signature changes: 有效行条件是租户、所有者和 `deleted_at IS NULL`。锁定再加 `sku_id`。
- Input/output and state mapping: 软删除只接收墓碑时间和审计列，不接收新的数量。
- Error and edge behavior: 找不到锁定行时返回空，不抛商品错误。
- Standards impact: MC-ARCH-001, MC-NAME-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 1 保持 DomainService 后缀。Rule 11 领域接口不依赖 HTTP 或 goods。
- Implementation pseudocode:

```java
List<CartItemBO> listActive(long tenantId, String ownerUserId);
Optional<CartItemBO> lockActive(long tenantId, String ownerUserId, long skuId);
void insertActive(CartItemBO row);
void updateActive(CartItemBO row);
void softDelete(long id, LocalDateTime deletedAt, String updateUserId, OffsetDateTime updateTime);
```

- Verification contribution: 本步末尾的聚焦测试。
- After this file: 该文件符合本步契约，后续文件才能编译。

#### File 12 — `MODIFY enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/infrastructure/cart/service/impl/CartItemDomainServiceImpl.java`

- Purpose: 把新的领域方法委托给仓库，并在插入时发号。
- Symbols: CartItemDomainServiceImpl
- Repository evidence: goods 的 `AdDomainServiceImpl` 使用已有的 `SnowflakeIdGenerator.nextLongId()`。
- Dependencies and consumers: `@Qualifier("cartItemRepository")` 与 `@Qualifier("cartItemConverter")`。
- Why now: 接口已经增加方法，实现必须同时补上。
- Contract/signature changes: 插入时用 `SnowflakeIdGenerator.nextLongId()` 填 `id`，不新建发号器。
- Input/output and state mapping: `createTime` 与 `updateTime` 是 `OffsetDateTime`，来自 `Clock.systemUTC()` 并截断到微秒。
- Error and edge behavior: 软删除调用仓库的墓碑更新，不调用会把数量改成 0 的删除。
- Standards impact: MC-BEAN-001, MC-LOG-001, MC-TIME-001, MC-REUSE-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 4 保持生成类上的 `@Slf4j`、构造注入和 `@Qualifier`。Rule 10 不使用 `java.util.Date`。Rule 11 不在这里发 Event。
- Implementation pseudocode:

```java
public void softDelete(long id, LocalDateTime deletedAt, String updateUserId, OffsetDateTime updateTime) {
    repository.markDeleted(id, deletedAt, updateUserId, updateTime);
}
public void insertActive(CartItemBO row) {
    row.setId(SnowflakeIdGenerator.nextLongId());
    row.setCreateTime(OffsetDateTime.now(Clock.systemUTC()).truncatedTo(ChronoUnit.MICROS));
    row.setUpdateTime(row.getCreateTime());
    repository.insert(converter.toSource(row));
}
```

- Verification contribution: 本步末尾的聚焦测试。
- After this file: 该文件符合本步契约，后续文件才能编译。

#### File 13 — `MODIFY enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/infrastructure/cart/converter/CartItemPersistenceConverter.java`

- Purpose: 把生成器的转换器 Bean 改成规格要求的名字，并确认它实现 BaseConverter。
- Symbols: @Component("cartItemConverter")
- Repository evidence: 生成器类名是 CartItemPersistenceConverter。规格 §10.4 要求 Bean 名 cartItemConverter，并实现 BaseConverter。
- Dependencies and consumers: 领域服务使用 `@Qualifier("cartItemConverter")`。不要再写一个 CartItemConverter 类。
- Why now: 领域实现马上要注入这个 Bean 名。
- Contract/signature changes: 双向转换 PO 与 BO，包含继承列。不转换请求体。
- Input/output and state mapping: 标识在 PO 中仍是 Long。请求里的字符串标识不经过这个转换器。
- Error and edge behavior: 若生成器没有实现 BaseConverter，在这个类上补齐，而不是新建第二个转换器。
- Standards impact: MC-CONVERT-001, MC-BEAN-001, MC-MODEL-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 3 使用 BaseConverter，不用 BeanUtils.copyProperties。Rule 4 Bean 名是 cartItemConverter。
- Implementation pseudocode:

```java
@Component("cartItemConverter")
public class CartItemPersistenceConverter implements BaseConverter<CartItemPO, CartItemBO> {
    public CartItemPO toSource(CartItemBO target) { /* 映射业务列和继承列 */ return po; }
    public CartItemBO toTarget(CartItemPO source) { /* 不丢 deletedAt 与 createTime */ return bo; }
}
```

- Verification contribution: 本步末尾的聚焦测试。
- After this file: 该文件符合本步契约，后续文件才能编译。

#### File 14 — `MODIFY enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/infrastructure/cart/repo/CartItemRepository.java`

- Purpose: 封装有效行查询、行锁和墓碑更新。
- Symbols: listActive, lockActive, markDeleted
- Repository evidence: 生成的仓库目前只有通用 CRUD。规格要求 `FOR UPDATE` 与 `ORDER BY id`。
- Dependencies and consumers: 领域服务是唯一调用者。控制器不能注入仓库。
- Why now: 领域实现已经调用这些方法。
- Contract/signature changes: `lockActive` 生成 `SELECT ... FOR UPDATE`。列表使用 `idx_cart_item_owner` 的列顺序。
- Input/output and state mapping: 所有查询都带 `deleted_at IS NULL`，除了按主键写墓碑。
- Error and edge behavior: 锁等待由数据库处理。本类不重试唯一约束。
- Standards impact: MC-ARCH-001, MC-REUSE-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 继续使用 Egon COLA MP，不手写第二套 JDBC 仓库。Rule 1 类名保持 Repository。
- Implementation pseudocode:

```java
public Optional<CartItemBO> lockActive(long tenantId, String ownerUserId, long skuId) {
    CartItemPO row = dao.lockActive(tenantId, ownerUserId, skuId);
    return Optional.ofNullable(row).map(converter::toTarget);
}
public void markDeleted(long id, LocalDateTime deletedAt, String updateUserId, OffsetDateTime updateTime) {
    int updated = dao.markDeleted(id, deletedAt, updateUserId, updateTime);
    if (updated != 1) throw new CommonException(610599, "CART_INTERNAL_ERROR", "cart internal error");
}
```

- Verification contribution: 本步末尾的聚焦测试。
- After this file: 该文件符合本步契约，后续文件才能编译。

#### File 15 — `MODIFY enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/infrastructure/cart/dao/CartItemDAO.java`

- Purpose: 为行锁和墓碑增加 MyBatis 方法。
- Symbols: lockActive, listActive, markDeleted
- Repository evidence: 生成的 DAO 继承 MP BaseMapper。部分索引查询需要手写 SQL。
- Dependencies and consumers: 仓库和 `CartItemDAO.xml`。
- Why now: 仓库方法需要 DAO 签名。
- Contract/signature changes: 方法参数是租户、所有者、SKU 和墓碑时间。不接收价格。
- Input/output and state mapping: PO 仍不重复声明 `tenantId` 与 `deletedAt`。
- Error and edge behavior: XML 缺失时启动失败。不要改成内存 Map。
- Standards impact: MC-NAME-001, MC-REUSE-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 1 保持 DAO 后缀。Rule 11 使用现有 MP starter。
- Implementation pseudocode:

```java
@Select("SELECT ...")
CartItemPO lockActive(@Param("tenantId") long tenantId, @Param("ownerUserId") String ownerUserId, @Param("skuId") long skuId);
List<CartItemPO> listActive(@Param("tenantId") long tenantId, @Param("ownerUserId") String ownerUserId);
int markDeleted(@Param("id") long id, @Param("deletedAt") LocalDateTime deletedAt,
        @Param("updateUserId") String updateUserId, @Param("updateTime") OffsetDateTime updateTime);
```

- Verification contribution: 本步末尾的聚焦测试。
- After this file: 该文件符合本步契约，后续文件才能编译。

#### File 16 — `MODIFY enjoyshop-service/enjoyshop-service-cart/src/main/resources/mybatis/mapper/cart/CartItemDAO.xml`

- Purpose: 写出带行锁和有效行过滤的 SQL。
- Symbols: lockActive, listActive, markDeleted
- Repository evidence: 生成器会忽略独立 `CREATE INDEX`，所以锁定语义必须在 SQL 里写 `FOR UPDATE`。
- Dependencies and consumers: DAO 方法。Step 9 的 PostgreSQL 测试不依赖这份 XML 来证明部分索引。
- Why now: DAO 签名已经引用这些 statement。
- Contract/signature changes: 列表 `ORDER BY id ASC`。锁定语句包含 `AND deleted_at IS NULL FOR UPDATE`。
- Input/output and state mapping: 墓碑更新只 SET `deleted_at`、`update_user_id`、`update_time` 和 `version`。`quantity` 不出现在 SET 中。
- Error and edge behavior: 更新行数不是 1 时由仓库抛 `610599`。不要在 XML 里吞掉唯一冲突。
- Standards impact: MC-REUSE-001, MC-TIME-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 10 SQL 使用 `timestamp` 参数，不使用 `java.util.Date`。Rule 11 有效行谓词是 `deleted_at IS NULL`。
- Implementation pseudocode:

```xml
<select id="lockActive" resultType="top.egon.enjoyshop.cart.infrastructure.cart.po.CartItemPO">
  SELECT id, tenant_id, owner_user_id, sku_id, quantity, unit_price, unit_weight, line_amount, pay_amount, line_weight, version
  FROM cart_item
  WHERE tenant_id = #{tenantId} AND owner_user_id = #{ownerUserId} AND sku_id = #{skuId} AND deleted_at IS NULL
  FOR UPDATE
</select>
<update id="markDeleted">
  UPDATE cart_item
  SET deleted_at = #{deletedAt}, update_user_id = #{updateUserId}, update_time = #{updateTime}, version = version + 1
  WHERE id = #{id} AND deleted_at IS NULL
</update>
```

- Verification contribution: 本步末尾的聚焦测试。
- After this file: 该文件符合本步契约，后续文件才能编译。


- Validation working directory: 仓库根目录 `/Users/mario/SelfProject/enjoyshop`
- Verification command: `mvn -pl enjoyshop-service/enjoyshop-service-cart -am -Dtest=CartItemAdjustmentStrategyTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: 退出码 0。五个测试通过。重算策略的类里没有 `GoodsProductSnapshotClient` 字段。
- Failure returns to: 策略的价格来源，或 `classify` 把有行负增量错分成缺席删除。
- Completion criteria: 旧行不重新取价；软删除不把数量写成 0；溢出是 `610506`；无行负增量不调用 `load`。
- Rollback: 回退本步提交。不恢复样例表，也不重跑生成器。
- Commit paths: `enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/application/cart/CartItemAdjustmentStrategyTest.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/infrastructure/cart/GoodsProductSnapshotResult.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/infrastructure/cart/GoodsProductSnapshotClient.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/application/cart/CartItemAdjustmentKind.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/application/cart/CartItemAdjustmentStrategy.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/application/cart/InsertFromSnapshotStrategy.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/application/cart/AbsentRemovalStrategy.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/application/cart/RecomputeStoredPriceStrategy.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/application/cart/SoftDeleteStrategy.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/application/cart/CartItemAdjustmentStrategyRegistry.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/domain/cart/CartItemDomainService.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/infrastructure/cart/service/impl/CartItemDomainServiceImpl.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/infrastructure/cart/converter/CartItemPersistenceConverter.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/infrastructure/cart/repo/CartItemRepository.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/infrastructure/cart/dao/CartItemDAO.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/resources/mybatis/mapper/cart/CartItemDAO.xml`
- Commit: `feat(cart): classify quantity changes with four strategies`


### Step 6 — 在写库之前鉴权，并在事务外读取商品

- Requirements: `REQ-004` `REQ-007` `REQ-008` `REQ-011`
- Dependencies: `Step 1` 与 `Step 5`
- Baseline state: 授权组件和四个策略已经存在。Manage 仍是生成出的 CRUD，还没有 goods 实现。
- Observable outcome: `adjust` 的第一个协作调用是 `requirePermission("enjoyshop:cart:write")`，`list` 的第一个调用是 read。所有者是快照 subject。goods 最多调用一次且不在事务内；写入事务最多两次；第二次唯一冲突返回 `610507`。
- End state: Bean `cartManage` 能完成列表和差额。公开 HTTP 仍要等 Step 7 删掉生成的 CRUD 映射。
- Test-first gate: `Required — CartItemManageImplTest 在 cartManage 改写前会因缺少 adjust 方法或调用顺序失败`
- Manual Checks: `MC-ARCH-001, MC-REUSE-001, MC-VALID-001, MC-BEAN-001, MC-LOG-001, MC-PATTERN-001, MC-UTIL-001, MC-SCOPE-001, MC-TEST-001`
- Literal Rules: `Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 9, Rule 10, Rule 11`
- Ordered files:

#### File 1 — `CREATE enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/application/cart/CartItemManageImplTest.java`

- Purpose: 固定写权限发生在任何仓库访问之前，并且 goods 在事务外。
- Symbols: requirePermissionWriteBeforeAnyRepository, readPermissionBeforeList, ignoresUsernameHeader, goods403DoesNotInsert, secondUniqueViolationIs610507, unsellableDoesNotInsert
- Repository evidence: 规格 §7.2 的调用顺序和 TEST-003、TEST-005、TEST-006。goods 的 Manage 测试用 mock 证明拒绝时不进入领域。
- Dependencies and consumers: Step 1 的 `cartAuthorizationComponent`、Step 5 的注册表，以及本步的 `cartItemWriteComponent`。
- Why now: Manage 仍是生成出的 CRUD，这是正确的红灯。
- Contract/signature changes: adjust 的第一个协作调用必须是 `requirePermission("enjoyshop:cart:write")`。list 的第一个调用是 read。
- Input/output and state mapping: 所有者断言为鉴身 subject `user-1`。测试传入的 `itcast` 不能出现在 `listActive` 或 `lockActive` 参数里。
- Error and edge behavior: 授权抛错时 domain 与 goods 都是零次调用。goods 抛 `610505` 时 `writer.apply` 为零次。第二次唯一冲突抛 `610507`。`CartItemManageImpl` 上没有 `@Transactional`。
- Standards impact: MC-TEST-001, MC-BEAN-001, MC-SCOPE-001, MC-PATTERN-001
- Literal rule enforcement: Rule 4 测试要求 Bean 名 `cartManage`。Rule 9 断言 Manage 源码不包含 `multiplyExact`。Rule 11 测试不启动数据库。
- Implementation pseudocode:

```java
@Test
void requirePermissionWriteBeforeAnyRepository() {
    doThrow(new CommonException(610501, "CART_FORBIDDEN", "cart permission is denied"))
            .when(authorization).requirePermission("enjoyshop:cart:write");
    assertThatThrownBy(() -> manage.adjust(command("100", "200", 1), "Bearer caller"))
            .isInstanceOf(CommonException.class);
    InOrder order = inOrder(authorization, domain, goods, writer);
    order.verify(authorization).requirePermission("enjoyshop:cart:write");
    verifyNoInteractions(domain, goods, writer);
}
@Test
void ignoresUsernameHeader() {
    manage.adjust(command("100", "200", 1), "Bearer caller");
    verify(domain).listActive(1L, "user-1");
    verify(domain, never()).listActive(anyLong(), eq("itcast"));
}
```

- Verification contribution: 本步末尾的聚焦测试。
- After this file: 该文件符合本步契约。

#### File 2 — `CREATE enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/infrastructure/cart/GoodsProductSnapshotClientImpl.java`

- Purpose: 用 RestClient 读取商品详情，并只转发调用方的 Authorization。
- Symbols: @Component("goodsProductSnapshotClient")
- Repository evidence: cart 已有 `spring-boot-starter-web`，因此有 `RestClient`。规格禁止 Feign。
- Dependencies and consumers: 实现 Step 5 的 `GoodsProductSnapshotClient`。插入策略是唯一注入点。
- Why now: Manage 测试要一个能按状态码抛错的实现；单测可以用假对象，本类用契约固定映射。
- Contract/signature changes: `GET {app.goods.base-url}/api/v1/products/{spuId}`。连接超时和读取超时各取配置，默认 2 秒。
- Input/output and state mapping: 请求头只有 Authorization。响应里取出目标 SKU 的 price、weight、image、status，以及 SPU 的 status、isMarketable、isDelete、image 和三个类目。
- Error and edge behavior: 404 抛 `610504`。超时、连接失败、401、403 和 5xx 抛 `610505` 并且不返回快照。日志只记 spuId、HTTP 状态和耗时，不记 Authorization，也不记 introduction。
- Standards impact: MC-REUSE-001, MC-DEP-001, MC-BEAN-001, MC-LOG-001, MC-CONFIG-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 4 使用 `@Slf4j`、`@RequiredArgsConstructor` 和配置值注入。Rule 11 不新增 HTTP 依赖。Rule 5 不在客户端做金额运算。
- Implementation pseudocode:

```java
@Component("goodsProductSnapshotClient")
@Slf4j @RequiredArgsConstructor
public class GoodsProductSnapshotClientImpl implements GoodsProductSnapshotClient {
    private final RestClient client;
    public GoodsProductSnapshotResult load(long spuId, long skuId, String authorizationHeader) {
        long started = System.nanoTime();
        try {
            ResponseEntity<JsonNode> response = client.get().uri("/api/v1/products/{spuId}", spuId)
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeader).retrieve().toEntity(JsonNode.class);
            log.info("goods snapshot spuId={} status={} elapsedMs={}", spuId, response.getStatusCode().value(), elapsed(started));
            return toSnapshot(response.getBody(), skuId);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) throw skuNotSellable();
            throw goodsUnavailable();
        } catch (ResourceAccessException ex) {
            throw goodsUnavailable();
        }
    }
}
```

- Verification contribution: 本步末尾的聚焦测试。
- After this file: 该文件符合本步契约。

#### File 3 — `CREATE enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/common/exception/CartSnapshotRequiredException.java`

- Purpose: 让写入事务在缺少快照时回滚，并把 goods 留在事务外。
- Symbols: CartSnapshotRequiredException
- Repository evidence: 规格 §7.3：锁定后如果行消失且还没有快照，回滚后再调用一次 goods。
- Dependencies and consumers: 只有 `CartItemWriteComponent` 抛出，只有 `CartItemManageImpl` 捕获。
- Why now: 它不是给网页的错误体。网页看到的是重试后的成功、`610504`、`610505` 或 `610599`。
- Contract/signature changes: 无字段的运行时异常。不实现 ErrorStatus。
- Input/output and state mapping: 事务因该异常回滚。没有部分更新。
- Error and edge behavior: Manage 最多捕获一次。第二次仍缺快照时改抛 `610599`。
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 这是应用内部信号，不是 Event，也不写外盒。Rule 1 它不是数据载体。
- Implementation pseudocode:

```java
public class CartSnapshotRequiredException extends RuntimeException {
    public CartSnapshotRequiredException() {
        super("cart insert needs a goods snapshot outside the write transaction");
    }
}
```

- Verification contribution: 本步末尾的聚焦测试。
- After this file: 该文件符合本步契约。

#### File 4 — `CREATE enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/application/cart/AdjustCartItemCommand.java`

- Purpose: 收一个差额命令，不收价格、用户名或租户。
- Symbols: AdjustCartItemCommand.spuId, skuId, num
- Repository evidence: 规格 §10.3 与 ASM-001。生成器的 Create、Update、Delete 命令形状不同，不能拿来当公开命令。
- Dependencies and consumers: 控制器和 `cartManage.adjust`。`@Qualifier("egonColaValidationUtils")` 在 Manage 里校验它。
- Why now: Manage 要先有可校验的命令，测试才能编译。
- Contract/signature changes: 三个字段。`spuId` 与 `skuId` 匹配 `[1-9][0-9]{0,18}`。`num` 是 -9999 到 9999 且非 0。
- Input/output and state mapping: class，使用 `@Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain = true) @Builder`。
- Error and edge behavior: 0、空白标识、越界和未知 JSON 键都不是合法命令。电话号码不出现。
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 1 类名以 Command 结尾。Rule 2 使用 `@NotBlank`、`@Pattern`、`@NotNull`、`@Min`、`@Max` 和自定义约束。Rule 3 不用 record。
- Implementation pseudocode:

```java
@Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain = true) @Builder
public class AdjustCartItemCommand {
    @NotBlank @Pattern(regexp = "[1-9][0-9]{0,18}") private String spuId;
    @NotBlank @Pattern(regexp = "[1-9][0-9]{0,18}") private String skuId;
    @NotNull @Min(-9999) @Max(9999) @CartQuantityDelta private Integer num;
}
```

- Verification contribution: 本步末尾的聚焦测试。
- After this file: 该文件符合本步契约。

#### File 5 — `CREATE enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/common/validation/CartQuantityDelta.java`

- Purpose: 把差额 0 定义成字段约束，而不是控制器里的裸 if。
- Symbols: CartQuantityDelta
- Repository evidence: 规格要求 `num == 0` 返回 400，消息稳定。
- Dependencies and consumers: `CartQuantityDeltaValidator` 与 `AdjustCartItemCommand.num`。
- Why now: 命令注解要引用这个约束。
- Contract/signature changes: `@Constraint(validatedBy = CartQuantityDeltaValidator.class)`，`message` 默认 `must be a non-zero signed delta`。
- Input/output and state mapping: 可标在 Integer 字段上。
- Error and edge behavior: null 交给 `@NotNull`，本约束对 null 视为通过，避免两条重复消息。
- Standards impact: MC-VALID-001, MC-NAME-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 2 使用 ConstraintValidator 扩展，不把全部校验塞进 ValidationUtils。Rule 11 约束放在 common.validation。
- Implementation pseudocode:

```java
@Documented
@Constraint(validatedBy = CartQuantityDeltaValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CartQuantityDelta {
    String message() default "must be a non-zero signed delta";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
```

- Verification contribution: 本步末尾的聚焦测试。
- After this file: 该文件符合本步契约。

#### File 6 — `CREATE enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/common/validation/CartQuantityDeltaValidator.java`

- Purpose: 拒绝 0，同时保留正负差额。
- Symbols: CartQuantityDeltaValidator.isValid
- Repository evidence: Jakarta `ConstraintValidator` 是现成扩展点。
- Dependencies and consumers: 由 `@CartQuantityDelta` 调用。
- Why now: 注解已经指向这个类。
- Contract/signature changes: `isValid` 在 value 为 null 或非 0 时返回 true。
- Input/output and state mapping: 0 返回 false，不改写字段。
- Error and edge behavior: 不把越界也塞进这里，越界仍由 `@Min` 与 `@Max` 负责。
- Standards impact: MC-VALID-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 2 这是自定义 ConstraintValidator。Rule 5 不引入新的校验库。
- Implementation pseudocode:

```java
public class CartQuantityDeltaValidator implements ConstraintValidator<CartQuantityDelta, Integer> {
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        return value == null || value.intValue() != 0;
    }
}
```

- Verification contribution: 本步末尾的聚焦测试。
- After this file: 该文件符合本步契约。

#### File 7 — `CREATE enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/application/cart/ListCartQuery.java`

- Purpose: 给列表一个查询对象，并且不让它携带身份。
- Symbols: ListCartQuery
- Repository evidence: 规格 §10.1。生成器的 `CartItemPageQuery` 会带来分页，而 ASM-005 要求整车。
- Dependencies and consumers: `cartManage.list`。
- Why now: 控制器下一步要把空查询交给 Manage。
- Contract/signature changes: 没有业务筛选字段，没有 page，没有用户名。
- Input/output and state mapping: class 加规定 Lombok。身份在 Manage 内读取。
- Error and edge behavior: 调用方即使反射填了字段，Manage 也不读。
- Standards impact: MC-NAME-001, MC-MODEL-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 1 类名以 Query 结尾。Rule 3 使用 class。Rule 11 查询和命令分开，没有 Event。
- Implementation pseudocode:

```java
@Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain = true) @Builder
public class ListCartQuery {
    // Identity is intentionally absent. cartManage reads the Jianshen snapshot instead.
}
```

- Verification contribution: 本步末尾的聚焦测试。
- After this file: 该文件符合本步契约。

#### File 8 — `CREATE enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/application/cart/CartResult.java`

- Purpose: 返回有效行、总件数和总金额。
- Symbols: CartResult.items, totalNum, totalPrice
- Repository evidence: 规格 API-001 的 data 形状。生成器没有合计类型。
- Dependencies and consumers: Manage 组装，控制器包进 `ResultRecord`。
- Why now: 列表和调整共用这一个成功体。
- Contract/signature changes: `items`、`totalNum`、`totalPrice`。金额单位是分。
- Input/output and state mapping: 空车是空列表和两个 0，不是 null。
- Error and edge behavior: 合计溢出不返回半截对象，由 Manage 抛 `610506`。
- Standards impact: MC-NAME-001, MC-MODEL-001, MC-JSON-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 1 类名以 Result 结尾。Rule 3 使用规定 Lombok class。Rule 6 JSON 由 Jackson 按字段名输出。
- Implementation pseudocode:

```java
@Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain = true) @Builder
public class CartResult {
    private List<CartItemResult> items = new ArrayList<>();
    private Integer totalNum;
    private Integer totalPrice;
}
```

- Verification contribution: 本步末尾的聚焦测试。
- After this file: 该文件符合本步契约。

#### File 9 — `MODIFY enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/application/cart/CartItemResult.java`

- Purpose: 把生成结果收成页面需要的列，标识改成十进制字符串。
- Symbols: CartItemResult
- Repository evidence: 生成器会按表列给出 Long 标识和继承列。规格 §9.2 的行对象不返回 `deletedAt`、租户和版本。
- Dependencies and consumers: `CartItemResultConverter` 与 `CartResult`。
- Why now: 合计类型已经引用它。
- Contract/signature changes: 保留 name、image、unitPrice、unitWeight、quantity、lineAmount、payAmount、lineWeight。`spuId` 与 `skuId` 改为 String。
- Input/output and state mapping: 不增加价格输入字段。
- Error and edge behavior: 软删除行根本不会进入这个对象。
- Standards impact: MC-NAME-001, MC-MODEL-001, MC-JSON-001, MC-CONVERT-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 1 保持 Result 后缀。Rule 3 保持 class 与 Lombok。Rule 6 不使用 ordinal。
- Implementation pseudocode:

```java
@Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain = true) @Builder
public class CartItemResult {
    private String spuId;
    private String skuId;
    private String name;
    private String image;
    private Integer unitPrice;
    private Integer unitWeight;
    private Integer quantity;
    private Integer lineAmount;
    private Integer payAmount;
    private Integer lineWeight;
}
```

- Verification contribution: 本步末尾的聚焦测试。
- After this file: 该文件符合本步契约。

#### File 10 — `MODIFY enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/application/cart/converter/CartItemResultConverter.java`

- Purpose: 用不可逆转换把 BO 投影成页面行。
- Symbols: @Component("cartItemResultConverter")
- Repository evidence: 规格 §10.4 要求 `BaseForwardConverter<CartItemBO, CartItemResult>`，Bean 名 `cartItemResultConverter`。
- Dependencies and consumers: Manage 列表组装。不要另写 `CartItemConverter` 来做这件事。
- Why now: 结果字段已经改成字符串标识。
- Contract/signature changes: `toTarget` 用 `Long.toString` 写 `spuId` 和 `skuId`。没有 `toSource`。
- Input/output and state mapping: 不复制 `deletedAt`。图片保持已冻结的字符串。
- Error and edge behavior: 标识为 null 时抛 `610599`，因为有效行不应缺标识。
- Standards impact: MC-CONVERT-001, MC-BEAN-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 3 实现 `BaseForwardConverter`，不用 `BeanUtils.copyProperties`。Rule 4 Bean 名固定。
- Implementation pseudocode:

```java
@Component("cartItemResultConverter")
public class CartItemResultConverter implements BaseForwardConverter<CartItemBO, CartItemResult> {
    public CartItemResult toTarget(CartItemBO source) {
        return new CartItemResult()
                .setSpuId(Long.toString(source.getSpuId()))
                .setSkuId(Long.toString(source.getSkuId()))
                .setUnitPrice(source.getUnitPrice())
                .setQuantity(source.getQuantity())
                .setLineAmount(source.getLineAmount())
                .setPayAmount(source.getPayAmount())
                .setLineWeight(source.getLineWeight());
    }
}
```

- Verification contribution: 本步末尾的聚焦测试。
- After this file: 该文件符合本步契约。

#### File 11 — `CREATE enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/application/cart/CartItemWriteComponent.java`

- Purpose: 把唯一的写事务放在独立 Bean 上，避免自调用跳过事务。
- Symbols: @Component("cartItemWriteComponent")
- Repository evidence: Spring 的 `@Transactional` 只有经过另一个 Bean 才生效。goods 调用必须留在这个 Bean 之外。
- Dependencies and consumers: `@Qualifier("cartItemDomainService")` 与 `@Qualifier("cartItemAdjustmentStrategyRegistry")`。
- Why now: Manage 需要一个会回滚的写入边界。
- Contract/signature changes: 方法 `apply` 使用 `@Transactional(isolation = Isolation.READ_COMMITTED)`。
- Input/output and state mapping: 先 `lockActive`，再 `classify`。已有行时丢弃快照。缺行且需要快照时抛 `CartSnapshotRequiredException`。
- Error and edge behavior: 本方法不调用 goods，不捕获唯一冲突。唯一冲突留给 Manage 在事务外重试。
- Standards impact: MC-ARCH-001, MC-BEAN-001, MC-LOG-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 4 使用 `@Slf4j`、构造注入和 `@Qualifier`。Rule 9 只调用注册表，不写四套价格。Rule 11 隔离级别是 READ COMMITTED。
- Implementation pseudocode:

```java
@Component("cartItemWriteComponent")
@Slf4j @RequiredArgsConstructor
public class CartItemWriteComponent {
    @Qualifier("cartItemDomainService") private final CartItemDomainService domain;
    @Qualifier("cartItemAdjustmentStrategyRegistry") private final CartItemAdjustmentStrategyRegistry registry;
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CartItemBO apply(long tenantId, String ownerUserId, long skuId, int num, GoodsProductSnapshotResult snapshot) {
        CartItemBO locked = domain.lockActive(tenantId, ownerUserId, skuId).orElse(null);
        CartItemAdjustmentKind kind = CartItemAdjustmentKind.classify(locked, num);
        if (kind == CartItemAdjustmentKind.INSERT_FROM_SNAPSHOT && snapshot == null) throw new CartSnapshotRequiredException();
        if (kind != CartItemAdjustmentKind.INSERT_FROM_SNAPSHOT) snapshot = null;
        return registry.require(kind).applyInsideTransaction(locked, num, snapshot);
    }
}
```

- Verification contribution: 本步末尾的聚焦测试。
- After this file: 该文件符合本步契约。

#### File 12 — `MODIFY enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/application/cart/CartItemManage.java`

- Purpose: 把生成器的 CRUD 接口收成列表和差额两个方法。
- Symbols: CartItemManage.list, adjust
- Repository evidence: 生成接口会暴露 create、update、delete、page。规格只有 API-001 和 API-002。
- Dependencies and consumers: `CartItemManageImpl` 与下一步控制器。
- Why now: 实现类要按新接口编译。
- Contract/signature changes: `CartResult list(ListCartQuery query)` 与 `CartResult adjust(AdjustCartItemCommand command, String authorizationHeader)`。
- Input/output and state mapping: 授权头不是所有者。查询对象不提供租户。
- Error and edge behavior: 不保留公开的 delete 方法，避免数量被直接写成 0。
- Standards impact: MC-ARCH-001, MC-NAME-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 1 保持 Manage 后缀。Rule 11 接口留在 application，不访问 DAO。
- Implementation pseudocode:

```java
public interface CartItemManage {
    CartResult list(ListCartQuery query);
    CartResult adjust(AdjustCartItemCommand command, String authorizationHeader);
}
```

- Verification contribution: 本步末尾的聚焦测试。
- After this file: 该文件符合本步契约。

#### File 13 — `MODIFY enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/application/cart/CartItemManageImpl.java`

- Purpose: 在仓库访问之前检查权限，并把 goods 限制为一次、写入事务限制为两次。
- Symbols: @Service("cartManage")
- Repository evidence: 规格 §7.2 与 §7.3。生成实现只有 CRUD。
- Dependencies and consumers: `cartAuthorizationComponent`、`egonColaValidationUtils`、`cartItemDomainService`、`cartItemAdjustmentStrategyRegistry`、`cartItemWriteComponent`、`cartItemResultConverter`。
- Why now: 这是本步的绿灯。测试已经规定调用顺序。
- Contract/signature changes: list 先 `requirePermission("enjoyshop:cart:read")`。adjust 先 `requirePermission("enjoyshop:cart:write")`，再 `validation.validate(command)`。
- Input/output and state mapping: 租户和 subject 只取 `Rbac3ContextAuthentication` 的快照。预读用 `listActive`，不用 `lockActive`。无行且差额为负时直接返回当前列表。无行且为正时才 `loadOutsideTransaction`。
- Error and edge behavior: 唯一约束名是 `uk_cart_item_active` 时回滚后重试一次，第二次抛 `610507`。其他完整性错误抛 `610599`。goods 失败不进入 `writer.apply`。合计使用 `Math.addExact`，溢出 `610506`。本类不加 `@Transactional`。
- Standards impact: MC-BEAN-001, MC-LOG-001, MC-VALID-001, MC-PATTERN-001, MC-UTIL-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 2 调用 `ValidationUtils.validate`。Rule 4 每个依赖都有 `@Qualifier`，类上有 `@Slf4j` 和 `@Validated`。Rule 5 合计只用 `Math.addExact`。Rule 9 不在本类写价格分支。
- Implementation pseudocode:

```java
@Service("cartManage")
@Validated @Slf4j @RequiredArgsConstructor
public class CartItemManageImpl implements CartItemManage {
    @Qualifier("cartAuthorizationComponent") private final CartAuthorizationComponent authorization;
    @Qualifier("egonColaValidationUtils") private final ValidationUtils validation;
    @Qualifier("cartItemDomainService") private final CartItemDomainService domain;
    @Qualifier("cartItemAdjustmentStrategyRegistry") private final CartItemAdjustmentStrategyRegistry registry;
    @Qualifier("cartItemWriteComponent") private final CartItemWriteComponent writer;
    @Qualifier("cartItemResultConverter") private final CartItemResultConverter results;
    public CartResult adjust(AdjustCartItemCommand command, String authorizationHeader) {
        authorization.requirePermission("enjoyshop:cart:write");
        validation.validate(command);
        Identity identity = currentSnapshot();
        CartItemBO preview = find(identity, command.getSkuId());
        CartItemAdjustmentKind kind = CartItemAdjustmentKind.classify(preview, command.getNum());
        GoodsProductSnapshotResult snapshot = null;
        int goodsCalls = 0;
        if (kind == CartItemAdjustmentKind.INSERT_FROM_SNAPSHOT) {
            snapshot = registry.require(kind).loadOutsideTransaction(Long.parseLong(command.getSpuId()), Long.parseLong(command.getSkuId()), authorizationHeader);
            goodsCalls = 1;
        }
        if (kind == CartItemAdjustmentKind.ABSENT_REMOVAL) return listOwned(identity);
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                writer.apply(identity.tenantId(), identity.subject(), Long.parseLong(command.getSkuId()), command.getNum(), snapshot);
                return listOwned(identity);
            } catch (CartSnapshotRequiredException ex) {
                if (goodsCalls >= 1 || attempt == 2) throw internal();
                snapshot = registry.require(CartItemAdjustmentKind.INSERT_FROM_SNAPSHOT).loadOutsideTransaction(Long.parseLong(command.getSpuId()), Long.parseLong(command.getSkuId()), authorizationHeader);
                goodsCalls++;
            } catch (DataIntegrityViolationException ex) {
                if (!isActiveSkuConflict(ex) || attempt == 2) throw conflictOrInternal(ex);
            }
        }
        throw conflict();
    }
}
```

- Verification contribution: 本步末尾的聚焦测试。
- After this file: 该文件符合本步契约。


- Validation working directory: 仓库根目录 `/Users/mario/SelfProject/enjoyshop`
- Verification command: `mvn -pl enjoyshop-service/enjoyshop-service-cart -am -Dtest=CartItemManageImplTest,CartItemAdjustmentStrategyTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: 退出码 0。写权限拒绝时 domain、goods 和 writer 都没有交互。`itcast` 不是所有者。
- Failure returns to: `CartItemManageImpl` 的权限顺序，或 `CartItemWriteComponent` 把 goods 调用放进了事务。
- Completion criteria: 读和写权限分离；新行才调用 goods；无行负增量不写；第二次 `uk_cart_item_active` 冲突是 `610507`。
- Rollback: 回退本步提交。不重跑生成器。
- Commit paths: `enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/application/cart/CartItemManageImplTest.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/infrastructure/cart/GoodsProductSnapshotClientImpl.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/common/exception/CartSnapshotRequiredException.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/application/cart/AdjustCartItemCommand.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/common/validation/CartQuantityDelta.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/common/validation/CartQuantityDeltaValidator.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/application/cart/ListCartQuery.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/application/cart/CartResult.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/application/cart/CartItemResult.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/application/cart/converter/CartItemResultConverter.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/application/cart/CartItemWriteComponent.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/application/cart/CartItemManage.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/application/cart/CartItemManageImpl.java`
- Commit: `feat(cart): adjust lines after a snapshot permission check`

### Step 7 — 公开两条购物车 REST，并把拒绝写成 ResultRecord

- Requirements: `REQ-002` `REQ-004` `REQ-007`
- Dependencies: `Step 2` 与 `Step 6`
- Baseline state: 过滤器和 Manage 已可用。生成控制器仍可能暴露 CRUD。OpenAPI Bean 名仍可能是 `studentManagementOpenApi`。
- Observable outcome: `GET /api/v1/cart` 与 `POST /api/v1/cart/items` 是仅有的购物车业务 URL。差额 0 和任何查询串返回 400 `610503`。无权限返回 403 `610501`。成功响应带 `Cache-Control: no-store`。缺 Bearer 仍是两字段 401。
- End state: operationId 为 `listCart` 与 `adjustCartItem`。文档含 `bearerAuth`，请求体没有价格。
- Test-first gate: `Required — CartHttpContractTest 在控制器改写前不能同时满足 400、403 和 operationId`
- Manual Checks: `MC-VALID-001, MC-JSON-001, MC-BEAN-001, MC-LOG-001, MC-SCOPE-001, MC-TEST-001`
- Literal Rules: `Rule 2, Rule 4, Rule 6, Rule 11`
- Ordered files:

#### File 1 — `CREATE enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/adapter/cart/CartHttpContractTest.java`

- Purpose: 固定两条公开 URL、差额 0、查询串、403 体和 no-store。
- Symbols: rejectsZeroDelta, rejectsAnyQueryString, forbiddenIsResultRecord, missingBearerStaysTwoFields, openApiHasBearerAuth
- Repository evidence: 规格 TEST-001、TEST-003 与 TEST-012。goods 的 Web 测试用 MockMvc 和合成身份。
- Dependencies and consumers: Step 2 的过滤器链、Step 6 的 Manage，以及本步的控制器和异常处理器。
- Why now: 生成控制器仍是 CRUD，所以 `POST /api/v1/cart/items` 还不会按差额拒绝 0。
- Contract/signature changes: 0 返回 400，`code` 是 610503，`data.fieldErrors` 含 `num`。任何查询串也是 400。
- Input/output and state mapping: 成功响应带 `Cache-Control: no-store` 和 `ResultRecord` 成功码 10000。403 的 `status` 是 `CART_FORBIDDEN`。
- Error and edge behavior: 无 Bearer 仍是两字段 401，不能被异常处理器改成带 `success` 的体。文档请求体没有 price，operationId 是 `listCart` 与 `adjustCartItem`。
- Standards impact: MC-TEST-001, MC-JSON-001, MC-VALID-001, MC-SCOPE-001
- Literal rule enforcement: Rule 2 覆盖 0 和越界。Rule 6 断言状态名不是 ordinal。Rule 11 测试放在 adapter.cart。
- Implementation pseudocode:

```java
@Test
void rejectsZeroDelta() throws Exception {
    mockMvc.perform(post("/api/v1/cart/items").header(HttpHeaders.AUTHORIZATION, bearer())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"spuId\":\"100\",\"skuId\":\"200\",\"num\":0}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(610503))
        .andExpect(jsonPath("$.status").value("CART_REQUEST_INVALID"))
        .andExpect(jsonPath("$.data.fieldErrors[0].field").value("num"));
}
@Test
void missingBearerStaysTwoFields() throws Exception {
    mockMvc.perform(get("/api/v1/cart"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.success").doesNotExist());
}
```

- Verification contribution: 本步末尾的聚焦测试。
- After this file: 该文件符合本步契约。

#### File 2 — `CREATE enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/application/cart/CartFieldErrorResult.java`

- Purpose: 给 400 一个字段错误元素。
- Symbols: CartFieldErrorResult.field, message
- Repository evidence: goods 的 `FieldViolationVO` 在 goods 包内，cart 不能依赖它。规格要求 `field` 与 `message`。
- Dependencies and consumers: `CartFieldErrorsResult` 与异常处理器。
- Why now: 合同测试要看到 `data.fieldErrors[0].field`。
- Contract/signature changes: 两个字符串字段。不回显堆栈。
- Input/output and state mapping: class 加规定 Lombok。
- Error and edge behavior: message 使用约束消息，不拼接 SQL。
- Standards impact: MC-NAME-001, MC-MODEL-001, MC-JSON-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 1 类名以 Result 结尾。Rule 3 使用 class，不用 record。Rule 6 字段名由 Jackson 输出。
- Implementation pseudocode:

```java
@Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain = true) @Builder
public class CartFieldErrorResult {
    private String field;
    private String message;
}
```

- Verification contribution: 本步末尾的聚焦测试。
- After this file: 该文件符合本步契约。

#### File 3 — `CREATE enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/application/cart/CartFieldErrorsResult.java`

- Purpose: 让 400 的 data 只包含 fieldErrors。
- Symbols: CartFieldErrorsResult.fieldErrors
- Repository evidence: 规格 §9.2 的校验失败示例。
- Dependencies and consumers: 异常处理器作为 `ResultRecord` 的 data。
- Why now: 元素类型已经存在。
- Contract/signature changes: 一个列表属性，JSON 名 `fieldErrors`。
- Input/output and state mapping: 空列表不用于成功响应。成功 data 仍是 `CartResult`。
- Error and edge behavior: 查询串错误没有字段时，data 为 null，不伪造字段名。
- Standards impact: MC-NAME-001, MC-MODEL-001, MC-JSON-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 1 与 Rule 3 同上。Rule 11 这个类不出基础设施。
- Implementation pseudocode:

```java
@Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain = true) @Builder
public class CartFieldErrorsResult {
    private List<CartFieldErrorResult> fieldErrors;
}
```

- Verification contribution: 本步末尾的聚焦测试。
- After this file: 该文件符合本步契约。

#### File 4 — `CREATE enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/adapter/handler/BaseExceptionHandler.java`

- Purpose: 把购物车业务码映射成规定的 HTTP 状态，并保持过滤器 401 不被改写。
- Symbols: BaseExceptionHandler
- Repository evidence: goods 的 `BaseExceptionHandler` 使用 `ResultRecord.failure`。cart 在 Step 3 已删除样例 `GlobalExceptionHandler`，目前没有购物车处理器。
- Dependencies and consumers: 控制器抛出的 `CommonException`、`MethodArgumentNotValidException` 和 `ConstraintViolationException`。
- Why now: 合同测试要求 610501 的 HTTP 状态是 403，体是 ResultRecord。
- Contract/signature changes: 610501 为 403，610502 与 610505 为 503，610503 为 400，610504、610506、610507 为 409，610599 为 500。
- Input/output and state mapping: 校验失败的 data 是 `CartFieldErrorsResult`。其他业务失败的 data 为 null。`status` 用枚举名。
- Error and edge behavior: 不处理已经由过滤器写完的响应。不把 401 入口点包成 ResultRecord。日志不记录 Authorization。
- Standards impact: MC-JSON-001, MC-LOG-001, MC-BEAN-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 4 类上 `@Slf4j`。Rule 6 `getStatus()` 已返回枚举名，处理器不再写 ordinal。Rule 11 处理器放在 adapter.handler。
- Implementation pseudocode:

```java
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class BaseExceptionHandler {
    @ExceptionHandler(CommonException.class)
    public ResponseEntity<ResultRecord<Void>> handle(CommonException error) {
        HttpStatus status = switch (error.getCode()) {
            case 610501 -> HttpStatus.FORBIDDEN;
            case 610502, 610505 -> HttpStatus.SERVICE_UNAVAILABLE;
            case 610503 -> HttpStatus.BAD_REQUEST;
            case 610504, 610506, 610507 -> HttpStatus.CONFLICT;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        return ResponseEntity.status(status).body(ResultRecord.failure(error));
    }
}
```

- Verification contribution: 本步末尾的聚焦测试。
- After this file: 该文件符合本步契约。

#### File 5 — `MODIFY enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/adapter/cart/CartItemController.java`

- Purpose: 只留下列表和差额两个操作，并给成功响应加上 no-store。
- Symbols: listCart, adjustCartItem
- Repository evidence: 生成控制器会注册 CRUD 动词。规格的 operationId 是 `listCart` 与 `adjustCartItem`。
- Dependencies and consumers: `@Qualifier("cartManage")`。过滤器已经保证无 Bearer 到不了这里。
- Why now: 合同测试正在请求这两个 URL。
- Contract/signature changes: `GET /api/v1/cart` 与 `POST /api/v1/cart/items`。成功用 `ResponseEntity` 设置 `Cache-Control: no-store`。
- Input/output and state mapping: POST 先拒绝查询串和非 `spuId`、`skuId`、`num` 的键，再交给 `@Valid` 命令。价格键直接 400。
- Error and edge behavior: 查询串返回 610503 且不调用 Manage。生成的 page、create、update、delete 映射删除。
- Standards impact: MC-VALID-001, MC-BEAN-001, MC-LOG-001, MC-JSON-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 2 参数使用 `@Valid`。Rule 4 控制器有 `@Slf4j` 和 `@Qualifier("cartManage")`。Rule 6 成功体走 `ResultRecord.success`。
- Implementation pseudocode:

```java
@GetMapping
@Operation(operationId = "listCart")
@SecurityRequirement(name = "bearerAuth")
public ResponseEntity<ResultRecord<CartResult>> listCart(HttpServletRequest request) {
    rejectQueryString(request);
    return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .body(ResultRecord.success(manage.list(new ListCartQuery())));
}
@PostMapping("/items")
@Operation(operationId = "adjustCartItem")
public ResponseEntity<ResultRecord<CartResult>> adjustCartItem(@Valid @RequestBody AdjustCartItemCommand command, HttpServletRequest request) {
    rejectQueryString(request);
    return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .body(ResultRecord.success(manage.adjust(command, request.getHeader(HttpHeaders.AUTHORIZATION))));
}
```

- Verification contribution: 本步末尾的聚焦测试。
- After this file: 该文件符合本步契约。

#### File 6 — `MODIFY enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/start/config/OpenApiConfig.java`

- Purpose: 把文档 Bean 改名为 cartOpenApi，并声明 bearerAuth。
- Symbols: @Bean("cartOpenApi")
- Repository evidence: Step 3 只改了标题。goods 的 `OpenApiConfig` 用 HTTP bearer、`bearerFormat("JWT")` 和名字 `bearerAuth`，Bean 仍叫 `studentManagementOpenApi`。
- Dependencies and consumers: TEST-012 读取 `/v3/api-docs`。控制器上的 `@SecurityRequirement` 引用这个名字。
- Why now: 合同测试要在文档里找到 bearerAuth 和两个 operationId。
- Contract/signature changes: 删除 `studentManagementOpenApi` 这个 Bean 名。标题保持 EnjoyShop Cart。
- Input/output and state mapping: 安全方案是 HTTP bearer JWT，不嵌入令牌样例。
- Error and edge behavior: 不把购物车文档写成允许匿名。不恢复学生样例描述。
- Standards impact: MC-JSON-001, MC-BEAN-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 4 Bean 名改为 `cartOpenApi`。Rule 6 安全方案不是业务枚举 ordinal。Rule 11 配置类留在 start.config。
- Implementation pseudocode:

```java
@Bean("cartOpenApi")
OpenAPI cartOpenApi() {
    Components components = new Components().addSecuritySchemes("bearerAuth",
            new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT"));
    return new OpenAPI().components(components).info(new Info().title("EnjoyShop Cart").version("1.0.0"));
}
```

- Verification contribution: 本步末尾的聚焦测试。
- After this file: 该文件符合本步契约。


- Validation working directory: 仓库根目录 `/Users/mario/SelfProject/enjoyshop`
- Verification command: `mvn -pl enjoyshop-service/enjoyshop-service-cart -am -Dtest=CartHttpContractTest,CartAuthenticationBoundaryTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: 退出码 0。`/v3/api-docs` 含 `listCart`、`adjustCartItem` 和 `bearerAuth`，请求体属性没有 price。
- Failure returns to: 控制器的查询串判断，或异常处理器的 HTTP 映射。
- Completion criteria: 生成 CRUD 动词不再公开；403 是 ResultRecord；401 不是 ResultRecord。
- Rollback: 回退本步提交。过滤器链保持 Step 2 的行为。
- Commit paths: `enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/adapter/cart/CartHttpContractTest.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/application/cart/CartFieldErrorResult.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/application/cart/CartFieldErrorsResult.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/adapter/handler/BaseExceptionHandler.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/adapter/cart/CartItemController.java`, `enjoyshop-service/enjoyshop-service-cart/src/main/java/top/egon/enjoyshop/cart/start/config/OpenApiConfig.java`
- Commit: `feat(cart): publish list and delta routes`

### Step 8 — 在现有网页用差额显示购物车

- Requirements: `REQ-009`
- Dependencies: `Step 7`
- Baseline state: `app.js` 没有 `#/cart`。`api.js` 在 BFF 401 时只改哈希。搜索卡片已经有 `spuId` 和 `skuId`，但没有加入按钮。
- Observable outcome: `#/cart` 能按差额提交。差额为 0 不发请求。购物车 401 把允许的原哈希写入 `sessionStorage` 键 `enjoyshop.returnHash`，登录后回到该哈希。空 registration 仍不跳转。
- End state: 商品页和搜索页以 `num` 为 1 调用同一条 POST，不提交价格。非购物车 401 不写回跳键。
- Test-first gate: `Required — test/cart.test.mjs 在三个导出出现前失败`
- Manual Checks: `MC-SCOPE-001, MC-TEST-001, MC-VALID-001`
- Literal Rules: `Rule 11`
- Ordered files:

#### File 1 — `CREATE enjoyshop-web/test/cart.test.mjs`

- Purpose: 在页面函数出现之前固定差额、0 不请求和回跳白名单。
- Symbols: cartDelta, cartAdjustBody, consumeAllowedReturnHash
- Repository evidence: 规格 TEST-014 与 TEST-015。`package.json` 的 test 脚本是 `node --test test/*.test.mjs`。现有 `api.test.mjs` 已断言品牌 401 只改哈希。
- Dependencies and consumers: `cart.js`、`api.js` 与 `connection.js` 的导出。
- Why now: 这些导出还不存在，所以本文件先失败。
- Contract/signature changes: 输入 3、当前 1 得到差额 2。相等时 `cartDelta` 为 0，并且 `cartAdjustBody` 不被调用。加号体是 `num: 1` 且没有 price。
- Input/output and state mapping: 允许的哈希只有 `#/cart`、`#/products`、`#/search`。其他 401 不写 `enjoyshop.returnHash`。
- Error and edge behavior: 空 registration 仍抛出原文 `请先配置 BFF 模式和真实登录配置名称`，并且不产生 URL。不使用 localStorage。
- Standards impact: MC-TEST-001, MC-SCOPE-001, MC-VALID-001
- Literal rule enforcement: Rule 11 测试只覆盖现有网页，不新建站点。Rule 2 的服务端校验不在这里重复实现成第二套数量上限。
- Implementation pseudocode:

```javascript
test('typed quantity becomes a signed delta and zero does not post', () => {
  assert.equal(cartDelta(3, 1), 2);
  assert.equal(cartDelta(1, 1), 0);
  assert.equal(cartAdjustBody('100', '200', 1).price, undefined);
  assert.deepEqual(cartAdjustBody('100', '200', 1), { spuId: '100', skuId: '200', num: 1 });
});
test('only an allowed cart hash is stored on cart 401', async () => {
  const storage = new MemoryStorage();
  const locationHash = { hash: '#/cart' };
  const client = createApi({ mode: 'bff', locationHash, storage, fetcher: async () => response({ message: '未登录' }, 401) });
  await assert.rejects(client.request('/api/v1/cart'));
  assert.equal(storage.get('enjoyshop.returnHash'), '#/cart');
  assert.equal(locationHash.hash, '#/connection');
});
```

- Verification contribution: enjoyshop-web 的 `npm test`。
- After this file: 测试因缺少导出而失败。

#### File 2 — `CREATE enjoyshop-web/src/pages/cart.js`

- Purpose: 提供购物车页和纯函数差额。
- Symbols: renderCart, cartDelta, cartAdjustBody
- Repository evidence: `app.js` 目前没有 cart 路由。`ui.js` 已有 `pageHeader`、`empty` 和 `button`。
- Dependencies and consumers: `api('/api/v1/cart')` 与 `api('/api/v1/cart/items')`。`app.js` 下一步注册 `renderCart`。
- Why now: 测试已经引用这些函数。
- Contract/signature changes: GET 渲染行。加号提交 1，减号提交 -1。输入框提交 `cartDelta(typed, current)`，结果为 0 时不调用 api。
- Input/output and state mapping: 元显示为 `amount / 100` 的两位小数。提交体只有 `spuId`、`skuId`、`num`。
- Error and edge behavior: 加载文案是“正在读取购物车”，空车是“购物车是空的”。提交期间禁用触发按钮。不渲染勾选、收藏、优惠或结算。403 显示“当前身份没有此操作权限”。
- Standards impact: MC-VALID-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 10 页面不解析审计时间。Rule 11 页面不保存令牌，也不提交价格。
- Implementation pseudocode:

```javascript
export function cartDelta(typed, current) {
  if (!Number.isInteger(typed) || !Number.isInteger(current)) throw new Error('数量必须是整数');
  return typed - current;
}
export function cartAdjustBody(spuId, skuId, num) {
  return { spuId: String(spuId), skuId: String(skuId), num };
}
export function renderCart(root) {
  root.innerHTML = pageHeader('购物车', '数量按差额提交，价格由服务端冻结。') + '<p role="status">正在读取购物车</p>';
}
```

- Verification contribution: 本步末尾的聚焦测试。
- After this file: 该文件符合本步契约。

#### File 3 — `MODIFY enjoyshop-web/src/app.js`

- Purpose: 注册 `#/cart`，并在启动时消费允许的回跳。
- Symbols: renderCart, consumeAllowedReturnHash
- Repository evidence: 商品中心分组在第 10 行，路由表在第 13 行。没有 cart。
- Dependencies and consumers: `pages/cart.js` 与 `pages/connection.js`。
- Why now: 页面函数已经存在，但用户还进不去。
- Contract/signature changes: 导航增加 `['cart', '购物车', '▣']`。路由表增加 `cart: renderCart`。
- Input/output and state mapping: 首次 render 前调用 `consumeAllowedReturnHash(location, sessionStorage)`。
- Error and edge behavior: 不允许的存储值被删除，不改变当前哈希。不读取 localStorage。
- Standards impact: MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 仍是现有哈希页面，不引入新的前端框架。
- Implementation pseudocode:

```javascript
import { renderCart } from './pages/cart.js';
import { consumeAllowedReturnHash } from './pages/connection.js';
consumeAllowedReturnHash(location, sessionStorage);
const groups = [
  ['商品中心', [['products', '商品管理', '▦'], ['cart', '购物车', '▣'], ['recycle', '商品回收站', '↺']]],
];
const routes = { cart: renderCart, products: renderProducts };
```

- Verification contribution: 本步末尾的聚焦测试。
- After this file: 该文件符合本步契约。

#### File 4 — `MODIFY enjoyshop-web/src/api.js`

- Purpose: 只在购物车 401 时写下允许的回跳哈希。
- Symbols: createApi storage
- Repository evidence: 第 86 到 88 行在 BFF 401 时把哈希改成 `#/connection`，不写 sessionStorage。
- Dependencies and consumers: `createApi` 的现有调用方。品牌 401 测试必须仍然只改哈希。
- Why now: 购物车测试已经要求 cart 路径写入键。
- Contract/signature changes: 新增可选 `storage`，默认 `globalThis.sessionStorage`。
- Input/output and state mapping: 仅当 path 以 `/api/v1/cart` 开头且当前哈希属于三个允许值时 `setItem('enjoyshop.returnHash', hash)`。然后仍把哈希改为 `#/connection`。
- Error and edge behavior: 品牌、搜索和其他 401 不写这个键。不写 localStorage，不保存 Bearer。
- Standards impact: MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 不新增登录端点。现有 `/oauth2/authorization/{registrationId}` 保持不变。
- Implementation pseudocode:

```javascript
const allowed = new Set(['#/cart', '#/products', '#/search']);
if (response.status === 401 && mode === 'bff' && locationHash && locationHash.hash !== '#/connection') {
  if (String(path).startsWith('/api/v1/cart') && allowed.has(locationHash.hash) && storage) {
    storage.setItem('enjoyshop.returnHash', locationHash.hash);
  }
  locationHash.hash = '#/connection';
}
```

- Verification contribution: 本步末尾的聚焦测试。
- After this file: 该文件符合本步契约。

#### File 5 — `MODIFY enjoyshop-web/src/pages/connection.js`

- Purpose: 登录成功回到允许的原哈希；空 registration 仍然不跳。
- Symbols: consumeAllowedReturnHash, loginUrl
- Repository evidence: 第 48 到 50 行在 registration 为空时抛出固定中文，并 `location.assign` 到 `/oauth2/authorization/`。`contracts.test.mjs` 锁定了这句错误。
- Dependencies and consumers: `app.js` 在启动时调用消费函数。登录按钮改为调用 `loginUrl`。
- Why now: 回跳键已经由 api.js 写入。
- Contract/signature changes: `loginUrl` 在 mode 不是 bff 或 registration 空白时抛出原文 `请先配置 BFF 模式和真实登录配置名称`。
- Input/output and state mapping: 消费函数只接受三个哈希，读完即 `removeItem`。
- Error and edge behavior: 非法存储值不导航。Bearer 模式仍然不能平台登录。
- Standards impact: MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 不把令牌写入 sessionStorage。键名只代表回跳页面。
- Implementation pseudocode:

```javascript
export function loginUrl(connection) {
  if (connection.mode !== 'bff' || !String(connection.registration || '').trim()) {
    throw new Error('请先配置 BFF 模式和真实登录配置名称');
  }
  return `${connection.base}/oauth2/authorization/${encodeURIComponent(connection.registration.trim())}`;
}
export function consumeAllowedReturnHash(locationHash, storage) {
  const value = storage.getItem('enjoyshop.returnHash');
  storage.removeItem('enjoyshop.returnHash');
  if (value === '#/cart' || value === '#/products' || value === '#/search') locationHash.hash = value;
  return locationHash.hash;
}
```

- Verification contribution: 本步末尾的聚焦测试。
- After this file: 该文件符合本步契约。

#### File 6 — `MODIFY enjoyshop-web/src/pages/products.js`

- Purpose: 在已经有 SPU 与 SKU 标识的行上加入购物车。
- Symbols: add-cart
- Repository evidence: 商品编辑器的 SKU 行已经有 `sku.id`，外层有 SPU id。搜索结果类型同样有两个标识。
- Dependencies and consumers: `cartAdjustBody` 与 `api('/api/v1/cart/items')`。
- Why now: 购物车页已经能处理返回的列表，商品页仍缺入口。
- Contract/signature changes: 只给已保存的 SKU 显示按钮。请求体是 `cartAdjustBody(spuId, skuId, 1)`。
- Input/output and state mapping: 不提交 price、weight 或 name。按钮在请求返回前禁用。
- Error and edge behavior: 没有 sku id 的新行不发请求。401 由 api.js 记住 `#/products`。
- Standards impact: MC-SCOPE-001, MC-TEST-001, MC-VALID-001
- Literal rule enforcement: Rule 11 不新增商品接口。Rule 2 数量固定为 1，不再做一套页面价格校验。
- Implementation pseudocode:

```javascript
if (action === 'add-cart') {
  const skuId = control.dataset.skuId;
  if (!skuId) throw new Error('请先保存 SKU，再加入购物车');
  control.disabled = true;
  await api('/api/v1/cart/items', { method: 'POST', body: cartAdjustBody(spuId, skuId, 1) });
  notify('已加入购物车');
}
```

- Verification contribution: 本步末尾的聚焦测试。
- After this file: 该文件符合本步契约。

#### File 7 — `MODIFY enjoyshop-web/src/pages/search.js`

- Purpose: 在搜索卡片上用现成的 spuId 和 skuId 加入购物车。
- Symbols: searchResults add-cart
- Repository evidence: `ProductSearchItemResult` 已返回 `spuId` 与 `skuId`。卡片目前只显示 skuId。
- Dependencies and consumers: 同一个 `cartAdjustBody`。
- Why now: 商品页入口还不够覆盖搜索用例 UC-003。
- Contract/signature changes: 每个 `page.rows` 增加按钮，提交 `num` 为 1。
- Input/output and state mapping: 不把展示用的元价格写进请求。
- Error and edge behavior: 请求未返回时禁用该按钮。失败留在搜索页。
- Standards impact: MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 不新做搜索接口。页面不保存令牌。
- Implementation pseudocode:

```javascript
const body = cartAdjustBody(item.spuId, item.skuId, 1);
button.addEventListener('click', async () => {
  button.disabled = true;
  try { await api('/api/v1/cart/items', { method: 'POST', body }); notify('已加入购物车'); }
  finally { button.disabled = false; }
});
```

- Verification contribution: 本步末尾的聚焦测试。
- After this file: 该文件符合本步契约。


- Validation working directory: `enjoyshop-web`
- Verification command: `npm test`
- Expected result: 退出码 0。差额测试、回跳白名单，以及原有的空 registration 测试都通过。
- Failure returns to: `cartDelta`、`api.js` 的路径判断，或 `loginUrl` 的错误原文。
- Completion criteria: 页面不保存访问令牌；差额 0 不请求；只允许三个回跳哈希。
- Rollback: 回退本步提交。后端 URL 保持不变。
- Commit paths: `enjoyshop-web/test/cart.test.mjs`, `enjoyshop-web/src/pages/cart.js`, `enjoyshop-web/src/app.js`, `enjoyshop-web/src/api.js`, `enjoyshop-web/src/pages/connection.js`, `enjoyshop-web/src/pages/products.js`, `enjoyshop-web/src/pages/search.js`
- Commit: `feat(web): open the cart with a signed quantity delta`

### Step 9 — 证明样例已删除、配置键一致、部分索引生效

- Requirements: `REQ-001` `REQ-003` `REQ-011` `REQ-020`
- Dependencies: `Step 3`、`Step 4` 与 `Step 8`
- Baseline state: 业务代码和网页已经按前面的步骤落地。还没有把样例扫描、四文件键对比和 PostgreSQL 部分索引放进同一个收口测试。
- Observable outcome: 样例路径不存在；权限组件和 `RedisConfig` 仍在；四个 YAML 键集合一致；PostgreSQL 拒绝两条有效的同一 SKU，并允许一条墓碑加一条有效行。
- End state: `TIME-001` 与 `TIME-002` 有执行证据。没有新增 `scripts/work` 脚本。
- Test-first gate: `Not applicable — 本步只锁定前面步骤已经完成的树和脚本。失败时回到对应前序 Step，不在本步放宽断言，也不改生产代码来让扫描通过`
- Manual Checks: `MC-ARCH-001, MC-CONFIG-001, MC-TIME-001, MC-REUSE-001, MC-SCOPE-001, MC-TEST-001`
- Literal Rules: `Rule 7, Rule 10, Rule 11`
- Ordered files:

#### File 1 — `CREATE enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/architecture/CartSourceContractTest.java`

- Purpose: 收口确认样例已删除、权限组件仍在，且业务源码没有 Redis 账本。
- Symbols: CartSourceContractTest
- Repository evidence: 规格 TEST-013、TEST-020、TEST-021 与 EXAMPLE-010。`RedisConfig` 必须留下。
- Dependencies and consumers: Step 3 的删除结果和 Step 1 的 `CartAuthorizationComponent`。
- Why now: 前面的步骤已经改完树。本测试把它们锁住。
- Contract/signature changes: 断言 `UserController`、`CourseController`、`MqRouteEnum`、`LightFacadeConverter` 的文件不存在，源码不引用它们。
- Input/output and state mapping: `StudentManagementApplication`、`NativeHttpSecurityConfiguration`、`RedisConfig` 和 `CartAuthorizationComponent` 存在。业务源码没有 `RedisTemplate`、`RabbitTemplate`、`Feign`、`@PreAuthorize` 或 jjwt。
- Error and edge behavior: 初始化 SQL 含 `ddl_history`，不含 `CREATE TABLE users`。manifest 的 SHA-256 等于文件字节。四个 YAML 不含 `user-base-url`。
- Standards impact: MC-ARCH-001, MC-REUSE-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 11 确认仍是 Light，且没有 Event 或外盒依赖。Rule 4 确认授权组件 Bean 名仍是 `cartAuthorizationComponent`。
- Implementation pseudocode:

```java
@Test
void permissionComponentRemainsAndSamplesStayGone() throws Exception {
    assertThat(Path.of("src/main/java/top/egon/enjoyshop/cart/application/cart/CartAuthorizationComponent.java")).exists();
    assertThat(Path.of("src/main/java/top/egon/enjoyshop/cart/infrastructure/config/RedisConfig.java")).exists();
    assertThat(Path.of("src/main/java/top/egon/enjoyshop/cart/adapter/user")).doesNotExist();
    String tree = Files.walk(Path.of("src/main/java")).map(Path::toString).reduce("", String::concat);
    assertThat(Files.readString(Path.of("src/main/java/top/egon/enjoyshop/cart/application/cart/CartAuthorizationComponent.java")))
            .contains("cartAuthorizationComponent").contains("enjoyshop:cart:read");
    assertThat(tree).doesNotContain("RedisTemplate").doesNotContain("@PreAuthorize");
}
```

- Verification contribution: 本步末尾的聚焦测试。
- After this file: 该文件符合本步契约。

#### File 2 — `CREATE enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/architecture/CartProfileKeyParityTest.java`

- Purpose: 证明四个环境的购物车键集合一致。
- Symbols: CartProfileKeyParityTest
- Repository evidence: 规格 TEST-016 与 Rule 7。四个 `application*.yml` 都在 Step 1 和 Step 3 改过。
- Dependencies and consumers: YAML 解析使用已有测试依赖。不新增 snakeyaml 坐标，使用 Spring 已传递的解析器。
- Why now: 键缺失只会在四个文件对比时暴露。
- Contract/signature changes: 比较键路径，不要求值相同。
- Input/output and state mapping: 必须同时存在 `server.port`、`app.goods.base-url`、`app.goods.connect-timeout`、`app.goods.read-timeout`、`app.web.cors.allowed-origins`、`egon.cola.platform.tianquan.jianshen.system-code`、两处 `register-filter`。
- Error and edge behavior: 任一文件多出或缺少这些键即失败。`system-code` 的值在四个文件都是 `enjoyshop-cart`。`register-filter` 的值都是 false。
- Standards impact: MC-CONFIG-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 7 这是四个环境文件的键对比。Rule 11 不新增第五份配置。
- Implementation pseudocode:

```java
@Test
void goodsTimeoutsAndPermissionKeysMatchAcrossProfiles() {
    List<Map<String, Object>> docs = List.of(yaml("application.yml"), yaml("application-dev.yml"),
            yaml("application-prod.yml"), yaml("application-test.yml"));
    assertThat(docs).allSatisfy(doc -> assertThat(text(doc, "egon.cola.platform.tianquan.jianshen.system-code")).isEqualTo("enjoyshop-cart"));
    assertThat(keys(docs.get(0))).contains("app.goods.base-url", "app.goods.connect-timeout", "app.goods.read-timeout", "app.web.cors.allowed-origins");
    assertThat(docs.stream().map(this::keys).distinct()).hasSize(1);
}
```

- Verification contribution: 本步末尾的聚焦测试。
- After this file: 该文件符合本步契约。

#### File 3 — `CREATE enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/infrastructure/migration/CartSchemaExecutionTest.java`

- Purpose: 在真实 PostgreSQL 上执行部分唯一索引，并核对两种时间列。
- Symbols: CartSchemaExecutionTest
- Repository evidence: 规格 TEST-005 与 TEST-011。H2 不能证明 `WHERE deleted_at IS NULL`。cart POM 已有 `org.postgresql:postgresql`。
- Dependencies and consumers: 环境变量 `CART_TEST_JDBC_URL`。不使用 Testcontainers，不调用应用的 ddl_history 执行器。
- Why now: 这是部分索引的执行证明。没有 URL 时测试失败，不能跳过。
- Contract/signature changes: 在事务里创建临时 schema 和 `cart_item`，执行脚本中的 `CREATE UNIQUE INDEX ... WHERE deleted_at IS NULL`，然后回滚。
- Input/output and state mapping: 两条 `deleted_at` 为空且租户、所有者、SKU 相同的插入必须冲突。一条已删除再加一条有效插入必须成功。
- Error and edge behavior: `create_time` 的类型是 `timestamp with time zone`。`deleted_at` 的类型是 `timestamp without time zone`。源码扫描确认没有 `java.util.Date`。
- Standards impact: MC-TIME-001, MC-DEP-001, MC-SCOPE-001, MC-TEST-001
- Literal rule enforcement: Rule 10 断言 Java 源码没有 `java.util.Date`，列类型分别对应 `OffsetDateTime` 与 `LocalDateTime`。Rule 11 唯一性包含 `deleted_at IS NULL`。
- Implementation pseudocode:

```java
@Test
void partialUniqueIndexRejectsTwoActiveRowsAndAllowsATombstone() throws Exception {
    String url = System.getenv("CART_TEST_JDBC_URL");
    if (url == null || url.isBlank()) fail("CART_TEST_JDBC_URL is required");
    try (Connection connection = DriverManager.getConnection(url)) {
        connection.setAutoCommit(false);
        connection.createStatement().execute("CREATE SCHEMA IF NOT EXISTS cart_probe");
        connection.createStatement().execute(cartTableSql("cart_probe.cart_item"));
        insert(connection, null);
        assertThatThrownBy(() -> insert(connection, null)).isInstanceOf(SQLException.class);
        insert(connection, LocalDateTime.of(2026, 10, 13, 6, 52, 1));
        connection.rollback();
    }
}
```

- Verification contribution: 本步末尾的聚焦测试。
- After this file: 该文件符合本步契约。


- Validation working directory: 仓库根目录 `/Users/mario/SelfProject/enjoyshop`
- Verification command: `mvn -pl enjoyshop-service/enjoyshop-service-cart -am -Dtest=CartSourceContractTest,CartProfileKeyParityTest,CartSchemaExecutionTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: 退出码 0。`CART_TEST_JDBC_URL` 缺失时命令失败，不能改成跳过。
- Failure returns to: Step 3 的残留样例、Step 1 的缺失键，或 Step 4 的索引定义。
- Completion criteria: 部分唯一索引由 PostgreSQL 执行；时间列类型与 §4.9 一致；购物车业务不注入 RedisTemplate。
- Rollback: 回退本步提交。不删除已经证明的表来让测试变绿。
- Commit paths: `enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/architecture/CartSourceContractTest.java`, `enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/architecture/CartProfileKeyParityTest.java`, `enjoyshop-service/enjoyshop-service-cart/src/test/java/top/egon/enjoyshop/cart/infrastructure/migration/CartSchemaExecutionTest.java`
- Commit: `test(cart): prove permissions, partial uniqueness, and sample removal`


## 8. Test, Validation, and Quality Gates

| Gate | Working directory | Command | Expected | 失败时回到 |
| --- | --- | --- | --- | --- |
| Step 1 授权 | 仓库根 | `mvn -pl enjoyshop-service/enjoyshop-service-cart -am -Dtest=CartAuthorizationComponentTest -Dsurefire.failIfNoSpecifiedTests=false test` | 退出码 0；系统码、非 USER、空决策和 `itcast` 四类断言通过 | `CartAuthorizationComponent` |
| Step 2 401 | 仓库根 | `mvn -pl enjoyshop-service/enjoyshop-service-cart -am -Dtest=CartAuthenticationBoundaryTest -Dsurefire.failIfNoSpecifiedTests=false test` | 缺 Bearer 为两字段 401；`register-filter=true` 启动失败 | `cartSecurityFilterChain` |
| Step 3 样例 | 仓库根 | `mvn -pl enjoyshop-service/enjoyshop-service-cart -am -Dtest=ArchetypeContractConvergenceTest,LightPersistenceArchitectureTest,FlywayMigrationConventionTest,NativeHttpCompatibilityTest,CartAuthenticationBoundaryTest -Dsurefire.failIfNoSpecifiedTests=false test` | 退出码 0；源码不再含 `class UserController` | 仍引用样例的测试 |
| Step 4 生成 | 仓库根 | `egon-codegen.sh plan` 然后 `apply`，再 `mvn -pl enjoyshop-service/enjoyshop-service-cart -am -DskipTests compile` | plan 无 CONFLICT；apply 与编译退出码 0。缺 classpath 时退出码 7 并停止 | SQL 或配置 JSON |
| Step 5 策略 | 仓库根 | `mvn -pl enjoyshop-service/enjoyshop-service-cart -am -Dtest=CartItemAdjustmentStrategyTest -Dsurefire.failIfNoSpecifiedTests=false test` | 退出码 0；旧单价不变；溢出为 610506 | 四个策略 |
| Step 6 编排 | 仓库根 | `mvn -pl enjoyshop-service/enjoyshop-service-cart -am -Dtest=CartItemManageImplTest -Dsurefire.failIfNoSpecifiedTests=false test` | 退出码 0；写权限先于仓库；goods 失败不写 | `CartItemManageImpl` |
| Step 7 契约 | 仓库根 | `mvn -pl enjoyshop-service/enjoyshop-service-cart -am -Dtest=CartHttpContractTest,CartAuthenticationBoundaryTest -Dsurefire.failIfNoSpecifiedTests=false test` | 退出码 0；0 为 400；403 是 ResultRecord；401 仍无 success | 控制器或异常处理器 |
| Step 8 网页 | `enjoyshop-web` | `npm test` | 退出码 0；差额、回跳和空 registration 原有断言都通过 | `cart.js` 或 `api.js` |
| Step 9 索引与键 | 仓库根 | `mvn -pl enjoyshop-service/enjoyshop-service-cart -am -Dtest=CartSourceContractTest,CartProfileKeyParityTest,CartSchemaExecutionTest -Dsurefire.failIfNoSpecifiedTests=false test` | 退出码 0。没有 `CART_TEST_JDBC_URL` 时 `CartSchemaExecutionTest` 失败 | 对应的前序 Step |
| 模块回归 | 仓库根 | `mvn -pl enjoyshop-service/enjoyshop-service-cart -am test` | 退出码 0。不启动对外服务、浏览器或真实守兵 | 第一个失败的测试 |
| 静态差异 | 仓库根 | `git diff --check` | 无空白错误 | 当前步 |

没有现有命令覆盖不到的门禁，因此不在 `scripts/work/` 增加临时脚本。网页门禁是 `npm test`，不启动浏览器。PostgreSQL 证明使用已有驱动和 `CART_TEST_JDBC_URL`，不新增 Testcontainers。

## 9. Migration, Compatibility, Rollout, and Rollback

新增脚本是 `V20261013_001__initialize_cart_schema.sql`。它只在 `egon_migration.role=MASTER_DATA` 时创建 `cart_item` 和两个部分索引。`V20260913_001` 在从未应用时按 EXAMPLE-006 替换为只含 `ddl_history`，并更新 manifest 哈希。两个脚本一旦写入 `ddl_history` 都不可再改。

`docs/egon/codegen/ddl-consumption-log.md` 只追加生成器消费记录，不代表 PostgreSQL 已经执行。部署前准备空的购物车库，再启动 cart。goods 不需要同时迁移。默认端口回退是 8081，避免和 goods 的 8080 冲突。

兼容变化是新增的：未登录访问购物车得到两字段 401；已登录但没有 `enjoyshop:cart:read` 或 `enjoyshop:cart:write` 得到 `610501`。平台授权发放不在本计划内。goods 的公开接口和网页原有登录地址不变。非购物车接口的 401 仍直接进入 `#/connection`，不写回跳键。

回滚按九个提交逆序恢复。未对外的表可以随进程停止后删除。已经提交的购物车行不能靠恢复样例脚本找回。不要回滚 `enjoyshop-service/pom.xml` 里已经存在的模块登记，除非该登记是在执行阶段之外单独提交的。

## 10. Requirement-to-Step Traceability Matrix

| Requirement | Steps | 验收证据 |
| --- | --- | --- |
| REQ-001 | Step 3, Step 4, Step 9 | 控制器、表和 DAO 只在 cart 模块；源码扫描找不到 goods 中的购物车类型 |
| REQ-002 | Step 2, Step 7 | 缺 Bearer 为两字段 401，且异常处理器不把它改成 ResultRecord |
| REQ-003 | Step 4, Step 5, Step 9 | 只读写 `cart_item`；业务源码没有 RedisTemplate 或 RabbitTemplate |
| REQ-004 | Step 6, Step 7 | 只有 `GET /api/v1/cart` 与 `POST /api/v1/cart/items`；查询串和差额 0 都是 400 |
| REQ-005 | Step 5, Step 6 | 已有行重算使用冻结单价；并发插入后的失败方也不刷新价格 |
| REQ-006 | Step 5, Step 6 | 软删除保留正数量；无行负增量不写；再次加入是新行并重新取价 |
| REQ-007 | Step 1, Step 6, Step 7 | 读权限与写权限分开；403 是 `610501`；所有者不是 `itcast` 或请求头 |
| REQ-008 | Step 6 | 新行价格只来自 goods 详情；失败、不可售都不插入；只转发 Authorization |
| REQ-009 | Step 8 | `#/cart` 用差额提交；差额 0 不请求；401 只为允许哈希写入 `enjoyshop.returnHash` |
| REQ-010 | Step 5, Step 6 | `Math.multiplyExact` 或 `Math.addExact` 溢出返回 `610506` |
| REQ-011 | Step 4, Step 6, Step 9 | 部分唯一索引被 PostgreSQL 执行；第二次冲突返回 `610507` |
| REQ-020 | Step 3, Step 9 | EXAMPLE 清单中的样例文件不存在或已替换；`RedisConfig` 与权限组件仍在 |

## 11. Risks, Blockers, and User Decisions

| ID | 风险 | 影响 | 处理 | 负责人 | 状态 |
| --- | --- | --- | --- | --- | --- |
| RISK-001 | 平台尚未授予 `enjoyshop:cart:read` 与 `enjoyshop:cart:write` | 真实登录后会收到 610501 | 本计划只实现校验和合成令牌，不在平台放行 | 平台管理员 | Tracked |
| RISK-002 | 生成器 v2 路径与本计划文件名不一致 | 若再手写一套类会有两个入口 | 以 v2 plan 的路径为准，不新增同职责类型 | 实施者 | Tracked |
| RISK-003 | 调用方缺少 `enjoyshop:product:read` | 新行取价得到 610505 | 不在购物车放宽 goods 的权限 | 调用方 | Tracked |
| RISK-004 | `CART_TEST_JDBC_URL` 未设置 | Step 9 不能证明部分索引 | 测试失败，禁止改成跳过或改用 H2 | 实施者 | Tracked |
| PLAN-CLAR-001 | 生成器 JSON 版本写成 2 | plan 校验失败 | JSON 保持 `configVersion` 1 | 实施者 | Recorded |
| PLAN-CLAR-002 | 目录类名与规格草稿名不同 | 公开 URL 仍按规格改到生成出的控制器 | 不另建 `CartController` | 实施者 | Recorded |
| PLAN-CLAR-003 | MapperScan 早于 DAO 类 | 空包说明只为编译 | 生成器随后写入 `CartItemDAO` | 实施者 | Recorded |
| PLAN-CLAR-004 | `ddl_history` 被建两次 | 首启失败 | 新脚本不创建该表 | 实施者 | Recorded |
| PLAN-CLAR-005 | 用 H2 代替部分索引 | 唯一性证明无效 | 必须使用 PostgreSQL 驱动 | 实施者 | Recorded |
| PLAN-CLAR-006 | goods 客户端拆成接口和实现 | 只影响类型名，Bean 名仍是 `goodsProductSnapshotClient` | 插入策略依赖接口，HTTP 放在实现类 | 实施者 | Recorded |

没有需要用户再选择的设计分叉。权限沿用守兵和鉴身，不新建认证栈。本计划不开始实施。

## 12. Review and Acceptance

### 12.1 Original-request fidelity

用户要求购物车做成独立模块，并在写计划时强调权限接入。Step 1 与 Step 2 先安装已批准的守兵和鉴身；Step 6 在任何仓库写操作之前调用 `requirePermission`；Step 7 把拒绝映射为 `610501` 的 403。没有匿名车，没有 Redis 账本，也没有把购物车放进 goods。

### 12.2 Spec consistency

公开 URL、错误码、冻结价格、软删除、部分唯一索引、网页差额和样例清理都来自购物车规格。目录权限规格只作为已收口行为的先例，没有把它的 goods 需求再实现一遍。生成器路径与规格草稿名冲突时，按规格 §8.2 采用 v2 plan，不另建第二套类型。

### 12.3 Repository executability

依赖坐标、过滤器顺序、`RestClient`、`ValidationUtils`、`SnowflakeIdGenerator` 和网页哈希都对照了当前仓库。执行命令使用现有的 Maven 与 `npm test`。生成器命令使用已存在的 `scripts/egon-codegen.sh`。

### 12.4 Coverage and release safety

每个行为步骤都先有失败测试，生成器步骤用 ADD 清单代替手写类体。提交路径限定在该步文件。不提交 `.egon/`，不改平台源码，不改已有的目录权限文档。九步逆序可回滚。没有未决的 `scripts/work` 脚本。

### 12.5 Blocking Manual Check

| Check ID | Applicability | Status | Evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- |
| MC-ARCH-001 | Applicable | PASS | §4.7 选定现有 Light 剖面，父 POM 保持 5.4.1 | 新类进入 cart 的 adapter、application、domain、infrastructure，不新建传统三层 | 无额外动作 |
| MC-REUSE-001 | Applicable | PASS | §4.7 复用守兵、鉴身、RestClient、MP 与发号器 | 不新建认证服务，不用 Feign 或 Redis 账本 | 无额外动作 |
| MC-DEP-001 | Applicable | PASS | Step 1 只增加 goods 已使用的两个 5.4.1 starter | 不增加 jjwt、Eureka、Testcontainers 或第二个 Redis 客户端 | 无额外动作 |
| MC-NAME-001 | Applicable | PASS | §4.8 Rule 1 与 Step 4 到 Step 7 的类型名 | 数据载体使用 PO、BO、DAO、Query、Command、Result、Repository 后缀 | 无额外动作 |
| MC-VALID-001 | Applicable | PASS | `AdjustCartItemCommand` 的注解与 Manage 的 `ValidationUtils.validate` | 控制器和 Manage 都校验差额；0 返回 610503 | 无额外动作 |
| MC-MODEL-001 | Applicable | PASS | §4.8 Rule 3；新载体是 class | 只有既有 `ResultRecord` 保持 record，新命令和结果不用 record | 无额外动作 |
| MC-CONVERT-001 | Applicable | PASS | `cartItemConverter` 与 `cartItemResultConverter` | 分别实现 `BaseConverter` 与 `BaseForwardConverter`，不使用 BeanUtils 复制 | 无额外动作 |
| MC-LOG-001 | Applicable | PASS | 授权组件、策略、客户端、Manage 和处理器都要求 `@Slf4j` | 日志不记录 Authorization 或商品介绍正文 | 无额外动作 |
| MC-BEAN-001 | Applicable | PASS | `cartAuthorizationComponent`、`cartManage`、`cartItemWriteComponent` 与四个策略 | 业务 Bean 显式命名，依赖使用 `@Qualifier` 和构造注入 | 无额外动作 |
| MC-UTIL-001 | Applicable | PASS | Step 5 只用 JDK `Math.multiplyExact` 与 `Math.addExact` | 没有新建工具类，也没有白名单外的计算库 | 无额外动作 |
| MC-JSON-001 | Applicable | PASS | `CartErrorStatus.getStatus` 返回 `name()`，成功体是 `ResultRecord` | 过滤器 401 保持两字段，不用 ordinal 当错误码 | 无额外动作 |
| MC-TIME-001 | Applicable | PASS | §4.9 的 TIME-001 与 TIME-002，由 Step 9 的 `CartSchemaExecutionTest` 证明 | 创建时间是 UTC `OffsetDateTime`，墓碑是 UTC 墙钟 `LocalDateTime`，没有 `java.util.Date` | 无额外动作 |
| MC-CONFIG-001 | Applicable | PASS | Step 1 增加键，Step 3 删除样例地址，Step 9 对比四个文件 | 键集合一致，系统码都是 `enjoyshop-cart`，`register-filter` 都是 false | 无额外动作 |
| MC-PATTERN-001 | Applicable | PASS | §13 与 Step 5 的四个策略加注册表 | Manage 不写四套价格分支，重算策略看不到 goods 客户端 | 无额外动作 |
| MC-SCOPE-001 | Applicable | PASS | 每个 Step 的提交路径；goods 与平台源码不在范围内 | 不改目录权限规格，不把购物车放进 goods | 无额外动作 |
| MC-TEST-001 | Applicable | PASS | 第 8 章九个门禁都有命令和预期 | 权限、401、策略、事务边界、契约、网页和部分索引都有断言 | 无额外动作 |
| MC-BLOCKER-001 | Applicable | PASS | §11 的事项都已有处理，没有新的设计分叉 | 剩余事项是平台授权、生成器路径差异和测试库 URL，都不改变已关闭的模块与登录决定 | 无额外动作 |

### 12.6 Final verdict

PASS — Ready for user review
