# enjoyshop 骨架与品牌 CRUD 实施计划

| Field | Value |
| --- | --- |
| Document | `2026-09-24-12-42-scaffold-brand-crud-implementation.md` |
| Template Version | `4` |
| Status | `Ready` |
| Created | `2026-09-24 12:42 CST` |
| Updated | `2026-09-24 15:20 CST` |
| Owner | `mario` |
| Repository | `enjoyshop` |
| Scope | `仓库根 Maven 骨架（外层 reactor 与二级聚合）、enjoyshop-service-goods 单模块生成工程（light）、删除自带全局信封设施、品牌垂直切片（adapter/application/domain/infrastructure 包）、brand 受管 DDL、统一异常处理、新增 MDC 租户过滤器、verifier 计数基线、天枢与 OpenAPI 多环境配置键` |
| Source Requirement | `用户 2026-09-23 迭代说明：项目结构（gateway、service、service_api、transaction_fescar、web）与 8 条需求（父 pom、五个二级模块、tianshu 注册中心、enjoyshop_common、enjoyshop_service_goods_api、enjoyshop_service_goods、品牌增删改查 8 种行为、公共异常处理 BaseExceptionHandler）；§5.4 十四项决策已于 2026-09-23 全部选定推荐项` |
| Baseline Revision | `02cda6b (main)；工作树仅含未跟踪的 docs/（本 Spec 与本 Plan）与 .DS_Store，无任何 POM 或源码` |
| Implements Spec | [enjoyshop 工程骨架与商品品牌 CRUD 设计](../spec/2026-09-23-17-16-enjoyshop-scaffold-brand-crud.md) |
| Spec Status | `Accepted` |
| Spec Revision | `Updated 2026-09-24 15:20 CST（DEC-101 由 web 改选 light 并新增 DEC-115 至 DEC-118；批准证据：用户 2026-09-24 会话明确表示 spec 审核通过，并在 BLOCK-001 处置时确认改选）` |
| Effective Specs | [enjoyshop 工程骨架与商品品牌 CRUD 设计](../spec/2026-09-23-17-16-enjoyshop-scaffold-brand-crud.md) |
| Depends On Plans | `None` |
| Supersedes | `None` |
| Superseded By | `None` |
| Related Plans | `None` |

## 1. Summary

本 Plan 实现唯一有效 Spec《enjoyshop 工程骨架与商品品牌 CRUD 设计》（已 Accepted）：由 `egon-coding-create-new-module` 从 `egon-cola-archetype-light:5.4.1` 生成 `enjoyshop-service-goods` **单模块**工程（`DEC-101` 选项 B），删除 light 自带的全局信封设施（`DEC-115`），建立外层 reactor 与两个二级聚合 POM（gateway、fescar、service-api、common 四个候选工程按 `DEC-103` 至 `DEC-106` 全部不建）；写入 `brand` 受管 DDL 与 Manifest 条目；经 `scripts/egon-codegen.sh`（**light** profile，backend-crud）产出 22 个 GENERATED 目录产物；再补齐目录产物不承载的自定义查询、操作者守卫、统一异常处理 `BaseExceptionHandler`、adapter 契约载体与 `BrandController` 五契约（`ResultRecord`/`PageResultRecord` 信封 + OpenAPI 注解），新增写 MDC 的 `TenantContextFilter`（`DEC-116`），更新自带 verifier 的计数基线（`DEC-117`），并把天枢/OpenAPI 配置键在四份 profile 中补齐为"键集合一致、默认关闭"。共 11 个 Step、每 Step 一个语义提交；完成证据为生成工程 `./mvnw test` 通过（含自带架构 verifier 与本 Plan 新增的 TEST-001 至 TEST-016 对应用例）以及 `mvn -N validate`、`dependency:tree` 零自写版本、代码生成 `plan` 无 CONFLICT。

实现依赖方向严格取生成形态（单模块内的包依赖）：`start → adapter/application/infrastructure`，`adapter → application/facade`，`application → domain`，`domain → common`，`infrastructure → domain/common`；`brand` 表唯一写路径在 `infrastructure` 的 `BrandRepository`。

`BLOCK-001` 已于 2026-09-24 关闭：web archetype 的四个 `evaluationFacade*` 是**生成物硬组成部分**（infrastructure 真实编译期依赖 加 8 个样例文件 加两个 verifier 断言，Spec `EVD-026`），"暂不使用外部 facade"在该形态下不可实现，而 create-new-module 技能禁止默认指向样例契约。用户据此把 `DEC-101` 改选 light（无对端 facade 属性，Spec `EVD-027`），其四项连带后果由 `DEC-115` 至 `DEC-118` 承担；codegen 的 light profile 与 web **共用同一个 `javaPackage()` 映射**（Spec `EVD-028`），因此 22 个产物的 Java 包名一字不改，取消的只是 Maven 模块目录层。本 Plan 状态由 `Blocked` 转 `Ready`。

## 2. Target Spec and Effective Design

### 2.1 Primary target

- Path: [docs/egon/spec/2026-09-23-17-16-enjoyshop-scaffold-brand-crud.md](../spec/2026-09-23-17-16-enjoyshop-scaffold-brand-crud.md)
- Status: `Accepted`（2026-09-24 由用户批准；Header 已同步）
- Revision: `Updated 2026-09-24 08:09 CST`；Spec 撰写基线 `02cda6b`，与本 Plan 基线一致
- Approval evidence: 用户 2026-09-24 会话原话"当前spec审核通过，可以开始写plan了"；此前 2026-09-23 用户已对 §5.4 全部 14 项重大决策答复"按照推荐方案 继续"

### 2.2 Effective Spec set

| Role | Spec/link | Status/revision | Effective sections | Why included |
| --- | --- | --- | --- | --- |
| Primary | [2026-09-23-17-16-enjoyshop-scaffold-brand-crud.md](../spec/2026-09-23-17-16-enjoyshop-scaffold-brand-crud.md) | `Accepted`，`Updated 2026-09-24 08:09 CST` | 全部章节（§1 至 §20 及两个尾部审查节） | 唯一治理本文档；其 `Amends`/`Supersedes`/`Depends On`/`Related Specs` 均为 `None`，无关系展开 |

### 2.3 Superseded or excluded content

`None`（Spec 无修订关系）。范围排除项沿 Spec 原文执行：`enjoyshop_gateway`、`enjoyshop-transaction-fescar`、`enjoyshop-service-api/`、`enjoyshop-common/` 四个工程不创建（`DEC-103` 至 `DEC-106` 选 A）；`-facade` 模块本迭代不新增 RPC 操作（`DEC-105`、`DEC-110`）；历史模型 `EVD-024` 只登记移植义务不在本期实施（§2.5、`RISK-011`、`RISK-012`）；天枢真实注册与玉衡文档发布不做，只交付默认关闭的完整配置键（`DEC-107`、`DEC-111` 选 A）。

## 3. Effective Requirements and Acceptance

| Requirement | Source Spec section | Effective statement | Observable acceptance | Implementation impact |
| --- | --- | --- | --- | --- |
| `REQ-001` | §4/§8.2/§16 | 仓库根 `packaging=pom` 外层 reactor，modules 只列工程根，不作生成工程 parent | `mvn -N validate` 通过；生成 root parent 仍为 `top.egon:egon-cola-archetypes-parent:5.4.1` | Step 1 外层 POM |
| `REQ-002` | §4/§8.2/§17 | 二级模块按连字符目录落位，未建模块各有已批准决策 | 目录树与 §8.2 一致；`DEC-102` 至 `DEC-106` 均已选 A | Step 1 两个二级聚合 POM |
| `REQ-003` | §4/§6.1/§8.2 | 商品工程由 create-new-module 从唯一非 open archetype 生成 | light 单模块齐全（八个包目录）；自带 verifier 测试通过（计数基线按 `DEC-117` 更新） | Step 1 生成 + 删除自带信封类 + 更新 verifier；Step 4 生成目录产物；Step 11 verifier 回归 |
| `REQ-004` | §4/§7.1/§15 | 天枢配置键四份 profile 键集合一致，默认 `enabled: false` | 键集合比对通过；缺天枢实例可启动 | Step 10 五份 YAML + 键集合测试 |
| `REQ-005` | §4/§7.0 | 不建 `enjoyshop_common`，共享走各工程 `-common` 层 + Components BOM | POM 无空模块；`GoodsErrorStatus` 位于 `-common` | Step 1/Step 3 |
| `REQ-006` | §4/§6.1/§9.0 | `-facade` 为发布载体但不新增操作；工程依赖全部来自继承管理 | `dependency:tree` 无自写 `<version>` | Step 1 声明形状；Step 11 核查 |
| `REQ-007` | §4/§9.2.3/§10/§11 | `POST /api/v1/brands` 一次事务创建品牌，201+Location，重复名 409 | 成功契约字段齐备；`version` 为 0 | Step 2/4/5/6/7/9 |
| `REQ-008` | §4/§9.2.4/§10.3 | `PUT /api/v1/brands/{brandId}` 整替换并以 `version` 防静默覆盖 | 冲突 409 610104；成功后审计列与版本递增 | Step 4/6/7/9 |
| `REQ-009` | §4/§9.2.5/§10.6 | `DELETE` 软删除写 `deleted_at`，重复删除 404，同名可重建 | 首次 200 true；重复 404 610102 | Step 4/6/9 |
| `REQ-010` | §4/§9.2.2/§15 | `GET /brands/{brandId}` 单条查询；不存在与跨租户同载荷 | 两种成因响应逐字段一致 | Step 5/6/8/9 |
| `REQ-011` | §4/§9.2.1 | 无过滤参数返回全部未软删除品牌，顺序稳定 | 空结果 `records` 为 `[]` 非 null | Step 5/6/9 |
| `REQ-012` | §4/§9.2.1 | 同一集合查询支持 name 前缀、letter 精确、seq 闭区间 | SQL 形态与 §11 访问模式一致；转义为字面量 | Step 5/7/9 |
| `REQ-013` | §4/§9.2.1 | 分页归一：pageNo 从 1 起、pageSize 默认 10 上限 500 | 与 `PageQuery`/`Pagination` 一致；越界页空集合 | Step 6/7/9 |
| `REQ-014` | §4/§9.2.1/§7.3.3 | 分页与条件可并用，排序 `seq DESC, id DESC` | 相邻页不重复不遗漏同一稳定行 | Step 5/6/9 |
| `REQ-015` | §4/§10.2/§11.2.1 | `brand` 表含四业务列 + 八继承列，主键 BIGINT 雪花 | DDL 与 §11 逐列一致（继承列类型按 `PLAN-CLAR-001` 修正）；PO 不重复继承列 | Step 2 DDL；Step 4 生成 PO |
| `REQ-016` | §4/§11.2.1/§16 | 一次变更只新增下一份脚本与 Manifest 条目，不改已应用脚本 | `sha256` 与字节一致；旧条目未变 | Step 2 |
| `REQ-017` | §4/§11.2.1/§7.3.4 | 业务唯一键 `(tenant_id, name)` 限有效行（`WHERE deleted_at IS NULL` 部分索引） | 同租户重复 409；软删后释放名称；跨租户可同名 | Step 2/5/6 |
| `REQ-018` | §4/§9.2 参数表/§10.3.1 | 每层边界 Spring Boot Validation；`ValidationUtils` 仅通用手工校验 | 负例逐条 400 610101 含 `fieldErrors` | Step 5/6/7/8/9 |
| `REQ-019` | §4/§9.2 映射表/§15 | `BaseExceptionHandler`（`@RestControllerAdvice`）把 §9 每个错误行映射到信封与安全 HTTP 状态 | 每类异常有稳定码与脱敏载荷，不泄露堆栈/SQL | Step 3/8（含 `401` 610105 写守卫） |
| `REQ-020` | §4/§9.0 | 写为 Command、读为 Query，本迭代零 Event | 无事件表、无 outbox/MQ 依赖 | Step 4（`events.enabled=false`） |
| `REQ-021` | §4/§6.1/§7.0 | 不新增未批准依赖/插件/生成器；FreeMarker 仅属生成工具 | 除 `DEC-*` 批准项外 POM 零新增 | Step 1/4/11（`TEST-015`） |
| `REQ-022` | §4/§14 | 切片有可执行单元与集成（切片）测试设计 | §14 每个 `TEST-*` 有目标与断言；`mvnw test` 为验收命令 | Step 3/5/6/7/8/9/10/11 |

Spec 的 8 项用户行为到 5 个 HTTP 原子契约的映射（`DEC-112` 选 A）与本 Plan 的 Step 对应：查询全部+条件+分页+分页条件 → `API-001`（Step 9 `listBrands`）；按 ID 查询单条 → `API-002`（Step 9 `getBrand`）；增加 → `API-003`（Step 9 `createBrand`）；修改 → `API-004`（Step 9 `updateBrand`）；删除 → `API-005`（Step 9 `deleteBrand`）。

## 4. Implementation Strategy and Dependency Order

### 4.1 Ordered strategy

顺序由真实依赖推导，而非层列表：**(1)** 工程必须先存在才能写入任何文件（Step 1 生成 + reactor）；**(2)** 受管 DDL 与 Manifest 是代码生成器的输入（backend-code-generation 顺序第 1 步），必须在 `plan/apply` 之前落盘（Step 2）；**(3)** 生成的 `BrandDomainServiceImpl` 以 FQN 引用工程自带的错误边界类型（`GenerationScopeValidator` 把 `apiContract.existingErrorMapper` 直接插入模板），故 apply 前必须存在 `GoodsErrorStatus` 与 `GoodsErrorMapper`（Step 3）；**(4)** `plan` 无 CONFLICT 才能 `apply` 产出 22 个 GENERATED 文件并追加 classpath SQL journal（Step 4）；**(5)** 自定义命名查询（`listBrandPage`/`countBrandPage`/`countActiveByName`）扩展生成的 DAO/XML/Repository（Step 5）；**(6)** 应用层端口与编排（存在性查重、操作者守卫、含 total 的 `listBrands`）依赖 Step 5 的 Repository 方法，同时把 `version` 补进 `BrandResult` 以维持 `API-004`/`API-005` 的乐观锁契约（Step 6）；**(7)** adapter 契约载体与转换器独立可测（Step 7）；**(8)** 统一异常处理先行于控制器切片测试（Step 8）；**(9)** `BrandController` 定制为 §9 五契约并修正过滤器操作者缺省（Step 9）；**(10)** 配置键与分片表注册在全部代码就位后一次收口（Rule 7 同树变更，Step 10）；**(11)** 全量回归、OpenAPI 结构断言与依赖核查收尾（Step 11）。平台契约（`EgonModel`、`EgonColaRepository`、`ResultRecord`、`PageQuery`）全部来自已发布制品，无需先行步骤。

### 4.2 Test-first strategy

| 行为 | RED 测试 | 最小 GREEN | 允许的重构/接线 |
| --- | --- | --- | --- |
| 自定义查询 SQL 形态与 LIKE 转义（`TEST-008`） | `BrandQuerySqlShapeTest`：断言 XML 含 `ORDER BY seq DESC, id DESC`、`ESCAPE '\'`、`deleted_at IS NULL`、`id !=`，且 `likePrefix` 转义 `%`/`_`/`\` — 语句与方法尚不存在 | `BrandDAO`/`BrandDAO.xml`/`BrandRepository`（Step 5） | 参数级 `@Validated` 注解补齐 |
| 操作者守卫与名称前置查重（`TEST-011`/`TEST-016` 的应用层） | `BrandManageImplTest`：空白 MDC `userId` 下 `create/update/delete` 须抛 610105 且零交互；查重命中须 610103 且不触达 create — 生成实现无守卫无查重 | `BrandManageImpl` 定制 + `BrandDomainService`/impl 端口（Step 6） | `listBrands` 装配 |
| adapter 归一与投影（`TEST-001`/`TEST-003` 的转换基座） | `BrandAdapterConverterTest`：letter 小写输入出大写、名称 trim+空白压缩、seq 缺省 0、Result 的 String id 出 VO 的 Long — 转换器不存在 | records + `BrandAdapterConverter`（Step 7） | `@Schema` 描述补齐 |
| 统一异常信封（`TEST-002`/`TEST-005`/`TEST-010`/`TEST-016` 的映射层） | `BaseExceptionHandlerTest`：`CommonException(610104)` 得 409 信封、字段校验得 400 `fieldErrors`、未知异常 500 脱敏 — advice 不存在时 MockMvc 得 Boot 默认错误体 | `BaseExceptionHandler`（Step 8） | `DuplicateKeyException` 分支补齐 |
| §9 五契约外形（`TEST-002`/`003`/`006`/`007`/`009`/`010`） | `BrandControllerWebTest`：201+Location、信封、全量模式 `pageSize=500` 回显、越界空页、404 同载荷 — 生成版 controller 返回裸类型与 `PageSlice`，全部断言失败 | `BrandController` 定制（Step 9） | OpenAPI 注解、`@Validated`、`@ApiResponses` |
| 配置键奇偶与默认关闭（`TEST-013`） | `ProfileKeyParityTest`：四份 profile 键集合相等、五处 `*_ENABLED` 缺省为 false — brand 表键与默认值尚未写入 | 五份 YAML 修改（Step 10） | 无 |
| OpenAPI 文档结构（`TEST-014`） | `BrandOpenApiContractTest`：五个 `operationId`、参数必填性、错误码声明 — 控制器注解在 Step 9 才补齐，先行运行失败 | 注解已在 Step 9 就位（Step 11 断言通过） | 无 |

Step 1（骨架）、Step 2（纯 DDL/Manifest）、Step 3（编译前置契约类型）、Step 4（生成器操作）为证据-backed `Not applicable`，理由在各 Step 内陈述。

### 4.3 Sequential and parallel boundaries

| Step | Depends on | May run in parallel with | Must not overlap with | Reason |
| --- | --- | --- | --- | --- |
| Step 1 | None | None | 全部后续 Step | 工程与 reactor 是一切文件的载体 |
| Step 2 | Step 1 | None | Step 1 写入的文件 | DDL 位于生成工程 resources 内 |
| Step 3 | Step 1 | Step 2（不同路径，但统一串行执行） | Step 2 | common 层类型与 DDL 无引用关系 |
| Step 4 | Step 2、Step 3 | None | 全部 | 生成器消费 Manifest 与错误边界 FQN |
| Step 5 | Step 4 | None | 生成的 DAO/XML/Repository | 修改生成文件需先有生成基线 |
| Step 6 | Step 5 | None | Step 5 文件 | 端口/编排调用自定义 Repository 方法并补 `BrandResult.version` |
| Step 7 | Step 6（`BrandResult.version` 存在后 VO 映射才完整） | None | Step 6 文件 | converter 消费 `BrandResult` |
| Step 8 | Step 3（错误码）、Step 7（`FieldViolationVO`/`FieldErrorsVO`） | None | Step 7 文件 | advice 组装错误载体 |
| Step 9 | Step 6、7、8 | None | 上述全部 | 控制器编排全链 |
| Step 10 | Step 4（brand 表已在 DDL 中） | None | 无重叠 | 仅 YAML 与 starter 测试 |
| Step 11 | Step 1 至 Step 10 | None | 全部 | 全量回归是最后闸门 |

无并行 Step；所有写作用域互不重叠，同一文件（如 `BrandDAO.xml`）只归一个 Step 拥有。

### 4.4 Commit boundaries

每个 Step 恰好一个语义提交，路径严格限位（§7 各 Step 的 Commit paths）。例外说明：Step 6 在一次提交内同时定制 `BrandResult`/`BrandResultConverter` 与编排层，因为拆开会产生无法编译的中间提交（接口新增方法后实现类必须同步实现）；Step 9 同理（接口签名、五个方法与过滤器缺省值共同构成 §9 契约外形）。文档（Spec 与本 Plan）随 Step 1 一并入库。`.egon/` 与 `.DS_Store` 永不入库。

### 4.5 Spec Simplicity and Implementation-necessity Audit

| Spec element | Spec necessity verdict/section | Current repository evidence | Direct/reuse alternative | Interaction/implementation cost | Plan decision |
| --- | --- | --- | --- | --- | --- |
| 外层 reactor + 2 个二级聚合 POM | `REQ-001`/`REQ-002`，§7.0 Add | 仓库无任何 POM（`git ls-files` 证据）；multi-project-parent 默认纯聚合 | 平铺仓库根 | 仅构建期文件 | Implement |
| `enjoyshop-service-goods` 单模块工程 | `REQ-003`，`DEC-101` 选 B | `egon-cola-archetype-light:5.4.1` 已在本地仓库（`/Users/mario/maven/repository`），`requiredProperties` 只含 `gitignore` | web 七模块（拒绝：`EVD-026` 的对端 Evaluation facade 是硬组成部分，`BLOCK-001` 不可实现） | 一个进程 + 一份配置 | Implement（生成，light） |
| `brand` 表 + 2 索引 + 1 CHECK | `REQ-015`/`REQ-016`/`REQ-017`，§11 | 绿地无表可复用；受管 Runner 与 Manifest 先例完整 | 无 | 一份脚本一次发布 | Implement |
| 22 个 GENERATED 目录产物 | §8.2 GEN 标注；backend-code-generation 目录契约 | catalog `backend-crud` 固定覆盖 | 手写（禁止） | 生成器写，模型只审 diff | Implement（生成） |
| 自定义 `listBrandPage`/`countBrandPage`/`countActiveByName` | §11.2.1 访问模式 + §9.2.1 排序/转义/total | 生成 `selectByQuery` 只有等值过滤 + `ORDER BY id ASC`、无 count，无法表达 §9 参数表 | 复用生成的 `selectByQuery`（拒绝：排序与谓词不符，`PageResultRecord` 需 total） | 三个命名语句 + 两个归一函数 | Implement |
| `BrandResult` 增补 `version` | §9.2.4/§9.2.5 要求 version 回传；`BrandVO.version` 必填 | `policyFields` 跳过全部基列（含 `version`），生成 Result 无法携带 | 无替代：乐观锁契约功能必需 | 修改 2 个生成文件（一次 CONFLICT 代价，§8.3 已声明） | Implement |
| `BrandVO` 审计时间列（`createTime`/`updateTime`） | `ASM-005` 列出，§9.2 载荷示例展示 | 生成链路（domain model/persistence converter/result）按目录契约排除全部基列，时间值在应用层不可达；archetype 先例 `GradeDetailVO` 仅业务字段 | 侵入 domain model + persistence converter 两个生成文件（为纯展示字段最大化 CONFLICT 面） | 无消费者行为依赖时间字段（§9.2.4 的过期判断可用 `version`） | 降级为不在本迭代 VO 暴露，登记 `PLAN-CLAR-011` 与 §11 `RISK-P2`，恢复路径写入 §9 |
| `BrandAdapterConverter` | §10.4 | 生成链只有 PO↔Brand 与 Brand→Result；Request→Command 与 Result→VO 无生成物 | 手写 setter（禁止） | 一个 MapStruct 接口 | Implement |
| `BaseExceptionHandler` + 字段错误载体 | `REQ-019`，`DEC-109` 选 A | light 自带 `GlobalExceptionHandler`（无 `basePackages` 限定的 `@RestControllerAdvice`，已占用三类校验异常，以 `ApiResponse` 加固定 400 返回）与 `ResponseWrapperHandler`（会把 `ResultRecord` 二次包装）；common-core 有 `CommonException(ErrorStatus)` 与 `ResultRecord.failure` | 复用自带 advice（拒绝：载荷不同形，且会与 `BaseExceptionHandler` 争抢同一批异常，选择不确定） | 一个 advice + 两个 record；删除三个自带类 | Implement（`DEC-115`／`PLAN-CLAR-013`：Step 1 删除三个自带类，Step 8 新建 advice，不加 `basePackages` 限定即已是链路唯一） |
| `TenantContextFilter` | §7.3.1 步骤 1、§7.0 | light 自带 `RequestContextFilter`（`X-Operator-Id` 加 ThreadLocal，**不写 MDC**） | 复用自带过滤器（拒绝：`EVD-012` 的 MP-SDJ 只从 MDC `tenantId` 取值，不写即每条 SQL 抛 `TENANT_CONTEXT_MISSING`） | 一个 filter Bean 与 MDC 生命周期 | Add（`DEC-116`／`PLAN-CLAR-014`：新增独立 MDC 过滤器，自带 ThreadLocal 过滤器共存） |
| 天枢/OpenAPI 配置键 | `REQ-004`，`DEC-107`/`DEC-111` 选 A | 生成工程四份 profile 键齐全，dev/test/prod 的 `TIANSHU_*`/`TIANQUAN_SHOUBING_ENABLED` 缺省 `true` 需翻转为 false；sharding `tables` 需补 `brand` | 无 | 五份 YAML + 一个键集合测试 | Implement |
| 缓存/Event/网关/fescar/common 工程/前置查询接口 | §7.0 Remove、`DEC-003`/`DEC-103`/`DEC-104`/`DEC-105`/`DEC-106` | 仓库无任何实现物，Spec 已逐条拒绝 | — | 零 | 不实施 |

审计结论：无 fetch-then-forward 接口（`tenant_id` 由 MDC 派生、`seq`/`letter` 由调用方真实持有）、无投机模式或缓存层、无未经 Spec 的元素；唯一的必要性张力是 VO 审计时间列，已按证据降级并登记，不构成静默改设计。

### 4.6 Change-unit Dependency Matrix

| Change unit | Requirements | Proof/RED point | Compile/runtime prerequisites | Produces | Consumers/unblocks | Owning Step |
| --- | --- | --- | --- | --- | --- | --- |
| 工程骨架与 reactor | `REQ-001` `REQ-002` `REQ-003` `REQ-005` `REQ-006` | `mvn -N validate` + 生成工程 `./mvnw -q validate` | archetype-light:5.4.1 本地已安装（requiredProperties 只含 `gitignore`） | 单模块工程 + 3 个聚合 POM | 全部 Step | Step 1 |
| brand Schema | `REQ-015` `REQ-016` `REQ-017` | `shasum` 与 Manifest 一致 + 静态逐列比对 | 生成工程的 infrastructure resources | `V20260923_001` + Manifest 条目 | Step 4 生成器输入 | Step 2 |
| 错误边界契约 | `REQ-019` | `GoodsErrorStatusTest` 码值断言 | common 模块 + common-core 依赖 | `GoodsErrorStatus`/`GoodsErrorMapper` | Step 4 生成引用；Step 8 advice | Step 3 |
| 生成目录产物 | `REQ-003` `REQ-015` `REQ-020` `REQ-021` | `plan` JSON 22 ADD 无 CONFLICT；`apply` 退出码 0；`./mvnw -q compile` | Step 2 + Step 3 + `EGON_CODEGEN_CLASSPATH` | 22 个 GENERATED 文件 + journal + `.gitignore` | Step 5/6/7/9 | Step 4 |
| 自定义查询 | `REQ-010` `REQ-011` `REQ-012` `REQ-017` `REQ-018` | `BrandQuerySqlShapeTest` RED→GREEN | 生成 DAO/XML/Repository | 3 个命名语句 + `likePrefix` | Step 6 | Step 5 |
| 用例编排与守卫 | `REQ-007` `REQ-008` `REQ-009` `REQ-011` `REQ-014` `REQ-018` | `BrandManageImplTest` RED→GREEN | Step 5 + 生成的 manage/域链 | 守卫、查重、`listBrands`、`BrandResult.version` | Step 7/9 | Step 6 |
| adapter 载体与转换 | `REQ-007` `REQ-008` `REQ-012` `REQ-013` `REQ-018` | `BrandAdapterConverterTest` RED→GREEN | Step 6 的 `BrandResult.version` | 5+1 个 record + 转换器 | Step 8/9 | Step 7 |
| 统一异常处理 | `REQ-019` `REQ-010` | `BaseExceptionHandlerTest` RED→GREEN | Step 3 + Step 7 载体 | `BaseExceptionHandler` | Step 9/11 | Step 8 |
| §9 五契约 | `REQ-007` 至 `REQ-014`、`REQ-019` | `BrandControllerWebTest` RED→GREEN | Step 6/7/8 | 定制后 `BrandController` + 过滤器修正 | Step 10/11 | Step 9 |
| 配置键收口 | `REQ-004` | `ProfileKeyParityTest` RED→GREEN | Step 4（brand 表已定义） | 五份 YAML 修改 + 奇偶测试 | Step 11 | Step 10 |
| 全量回归收口 | `REQ-021` `REQ-022` | `BrandOpenApiContractTest` + 全模块 `./mvnw test` + `dependency:tree` | 全部 Step | 最终验证产物 | 交付评审 | Step 11 |

### 4.7 Java, Spring, and Egon-COLA Implementation Standards

| Concern | Current repository evidence | Effective Spec decision | Planned implementation consequence | Owning Steps/checks |
| --- | --- | --- | --- | --- |
| Architecture profile | 仓库绿地；唯一允许形态为 `egon-cola-archetype-light` 单模块（`EVD-003`、`EVD-020` verifier、`EVD-027`） | `DEC-101` 选 B；禁止 `biz.*` 混用（Rule 11） | 全部文件落位于生成形态的 `common/facade/domain/application/infrastructure/adapter/start` **包**；自定义文件放对应包既有风格（`adapter.filter`／`adapter.handler`）；`adapter/handler/` 因 `DEC-115` 清空后由 Step 8 独占 | Step 1/4；`MC-ARCH-001` |
| Reuse/capability | `ResultRecord`/`PageResultRecord`/`PageQuery`/`PageSlice`/`CommonException(ErrorStatus)`/`ErrorStatus`/`ValidationUtils` 均在 common-core（源码已核对）；`EgonModel`/`EgonColaMapper`/`EgonColaRepository` 在 MP-SDJ starter | `DEC-004`/`DEC-005`/`DEC-109` 全部复用 | 信封、分页归一、乐观锁、软删除、雪花 ID、异常载体全部复用，零新增依赖 | Step 3/8；`MC-REUSE-001`/`MC-DEP-001` |
| Naming/model/validation/conversion | 生成命名规则实测（`BrandPO`/`BrandDAO`/`BrandRepository`/`BrandPersistenceConverter`/`BrandManageImpl` 等）；生成 CQE 载体为 Lombok class（command.ftl/query.ftl/result.ftl），adapter 手写载体按 Spec 为 record | Spec §10.1/§10.3.1 + `PLAN-CLAR-002`/`PLAN-CLAR-003` | 生成物类名/Bean 名以生成器为准；adapter 侧 `CreateBrandRequest`/`UpdateBrandRequest`/`BrandListRequest`/`BrandVO`/`FieldViolationVO`/`FieldErrorsVO` 为 record + Jakarta 约束 + `@Schema`；每个层间交接逐条校验（Controller `@Valid`、Manage 接口 `@Validated`、Repository 参数级约束） | Step 5/6/7；`MC-NAME-001`/`MC-MODEL-001`/`MC-VALID-001`/`MC-CONVERT-001` |
| Bean/logging/util/JSON/time/config | 生成 Bean：`brandController`/`brandManage`/`brandDomainService`/`brandRepository`/`brandPersistenceConverterImpl`；`lombok.config` 开启 `copyableAnnotations += Qualifier`（`EVD-020`）；工具仅 JDK + commons-lang3（样例先例） | Spec §6.2 Rule 4/5/6/7/10；`PLAN-CLAR-002`（Bean 名为 `brandManage` 而非 Spec 字面 `brandManageImpl`） | 自定义 Bean 显式命名（`baseExceptionHandler`），注入 `@RequiredArgsConstructor` + `@Qualifier`（`lombok.config` 已复制）；`@Slf4j` 于全部业务类；JSON 走 Boot Jackson（int 码、`name()` 状态、非 null 才输出）；时间仅 `EgonModel` 既有 `Instant`/`LocalDateTime`；五份 YAML 键集合一致 | Step 3/6/8/9/10；`MC-LOG-001`/`MC-BEAN-001`/`MC-UTIL-001`/`MC-JSON-001`/`MC-TIME-001`/`MC-CONFIG-001` |
| Business variation/pattern | 品牌逻辑单表、无状态机、无类型分发（Spec §13 判 Simple） | Spec §13.1/§13.2：直接实现 + Bean Validation，不造仪式模式 | 守卫/查重为顺序直写；唯一"模式"是框架强制的 `EgonColaRepository` 模板扩展 | Step 6；`MC-PATTERN-001` |

#### Capability reuse ledger

| Need | Candidates inspected | Exact evidence | Fit/gap | Decision | Added dependency/custom code | Owning Step/check |
| --- | --- | --- | --- | --- | --- | --- |
| HTTP 信封与分页 | common-core `ResultRecord`/`PageResultRecord`/`PageMetaRecord`/`PageQuery`/`PageSlice` | `ResultRecord.success/failure`、`PageResultRecord.success(records,total,pageNo,pageSize)`、`PageQuery` 紧凑构造归一（源码已读） | 完全满足 §9 信封 | 复用，零新增 | None | Step 9；`MC-REUSE-001` |
| 业务异常载体 | common-core `CommonException(ErrorStatus)`、`ResultRecord.failure(Throwable)` | 构造器与工厂方法已核对 | 满足 401/404/409/500 全部分支 | 复用（不新建异常类型） | None | Step 3/8；`MC-REUSE-001` |
| 持久化模板与拦截器 | MP-SDJ starter `EgonModel`（八继承列 + `@TableLogic` + `@Version`）、`EgonColaMapper`、`EgonColaRepository`、租户/审计填充 | 源码与 archetype 先例（`SchoolClassRepository`） | 完全覆盖租户、审计、乐观锁、软删 | 复用 | None | Step 4/5；`MC-REUSE-001` |
| 生成工具 | `scripts/egon-codegen.sh`（Egon-COLA 仓库）+ FreeMarker 2.3.35 | codegen README + `codegen-web.json` 样例 + `egon-cola-component-code-generator:5.4.1` 本地已装 | 满足 backend-crud 全部产物 | 使用已验证生成器 | 无（FreeMarker 为预批准工具依赖，仅生成器内部） | Step 4；`MC-DEP-001` |
| 对象转换 | MapStruct（生成 POM 既有 mapstruct-plus starter）+ common-core `BaseConverter`/`BaseForwardConverter` | 生成 converter 模板即该组合；`GradeAdapterConverter` 先例 | 满足；注意单接口不可继承两份同一泛型接口（`PLAN-CLAR-004`） | 复用 | None | Step 7；`MC-CONVERT-001` |
| 通用校验 | spring-boot-starter-validation + common-core `ValidationUtils` | adapter/application POM 既有；`ValidationUtils.requireNotNull` API 已核对 | 覆盖；跨字段区间复核直接 if + `CommonException`（`ValidationUtils` 仅通用场景） | 复用 | None | Step 7/9；`MC-VALID-001` |
| 字符串工具 | JDK `String` + commons-lang3 `StringUtils.normalizeSpace` | adapter POM 传递依赖 commons-lang3（Boot web 栈默认传递）；Spec Rule 5 白名单 | 满足名称归一 | 复用 | None | Step 7；`MC-UTIL-001` |
| 文档与强约束 | `yuheng-starter-openapi-webmvc`（starter POM 既有）+ `swagger-annotations-jakarta`（common-core 传递）+ 平台 `EgonOperationCustomizer` | `EVD-022`/`EVD-023`；`@Operation(operationId=...)` 属性已核对 | 满足 `DEC-111` 选 A（注解级 + 单元断言） | 复用，不新增 UI 依赖 | None | Step 9/11；`MC-REUSE-001`/`MC-DEP-001` |

### 4.8 User-mandated Java Rule Implementation Matrix

| Literal rule | Spec source | Repository evidence | Exact files and order | Pseudocode obligations | Validation gate | Steps | Status/blocker |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Rule 1 | Spec §6.2 Rule 1、§10.1 | 生成命名规则 + §10.1 16 对象清单 | Step 3 枚举；Step 4 生成 22 类；Step 6 端口/编排；Step 7 六 record + 转换器；Step 8 advice | 全部类型以语义后缀结尾（PO/DAO/VO/Request/Query/Command/Result/Repository/Manage/Service/Converter/Filter/Handler/Exception/Error/Mapper）；无 `Data`/`Info`/`Param`/`Bean` | `TEST-001` 静态命名清单复核（Step 11 执行） | 3/4/6/7/8 | PASS |
| Rule 2 | Spec §6.2 Rule 2、§9.2 参数表、`MC-VALID-001` | `@Valid @RequestBody`（GradeController 先例）；`@Validated` 接口（生成 manage/domain 模板） | Step 5 Repository 参数级；Step 6 Manage 接口 `@Valid`；Step 7 Request record 约束；Step 9 Controller `@Valid` + `@Validated` 类注解 | 五个交接逐条列出：外部输入→Controller（`@Valid` record）、Controller→Manage（`@Valid` command/query + 类级 `@Validated`）、Manage→Domain（`@Validated` 端口）、Manage/Domain→Repository（参数级约束）、区间跨字段 if+`CommonException`；`ValidationUtils` 不承载业务校验；无电话语义故无 libphonenumber | `TEST-003`/`TEST-008` 负例切片与单测 | 5/6/7/9 | PASS |
| Rule 3 | Spec §6.2 Rule 3、§10.3.1/§10.4 | 生成载体为 class + 完整 Lombok 组合（模板）；adapter 先例 record（`PageQuery` 同构）；`BaseForwardConverter` 泛型契约 | Step 4 生成 PO/域模型/CQE（class 组合，含 PO 的 `@EqualsAndHashCode(callSuper=true)` + `@SuperBuilder`）；Step 6 `BrandResult` 增补；Step 7 六 record（豁免）+ `BrandAdapterConverter extends BaseForwardConverter` | 生成文件保持模板注解不手改结构；adapter record 为不可变载体；转换仅 MapStruct + `BaseForwardConverter`，禁 BeanUtils/反射/JSON 往返 | `TEST-001`/`TEST-012` 构造与映射断言 | 4/6/7 | PASS |
| Rule 4 | Spec §6.2 Rule 4 | 生成模板 `@Service("brandManage")`/`@Service("brandDomainService")`/`@Repository("brandRepository")` + `@Slf4j` + `@RequiredArgsConstructor` + `@Qualifier`；`lombok.config` copyable | Step 3 两个类型（枚举无需 log，mapper 为 final 工具类）；Step 6 编排层；Step 8 advice（`@RestControllerAdvice(name="baseExceptionHandler")`）；Step 9 控制器 | 每个业务类 `@Slf4j`；Spring 单例显式 Bean 名；final 字段 + `@RequiredArgsConstructor` + 逐字段 `@Qualifier`；`lombok.config` 已复制 Qualifier 无需修改 | `BrandManageImplTest` 装配 + 上下文切片启动 | 3/6/8/9 | PASS |
| Rule 5 | Spec §6.2 Rule 5、§20.5 `MC-UTIL-001` | 全仓工具仅 JDK/Commons/Guava 白名单 | Step 5 `likePrefix`（JDK `String.replace`）；Step 7 `StringUtils.normalizeSpace`（commons-lang3） | 不自研 `*Utils`，不引入白名单外工具；Tika 不涉及 | `TEST-015` 的 import 检索（Step 11） | 5/7 | PASS |
| Rule 6 | Spec §6.2 Rule 6、`EVD-023` | Boot Jackson 全局配置（生成 application.yml `jackson.*`）；`ResultRecord` 自带 Jackson 注解 | Step 7 VO/Request record（默认 Jackson，无 ordinal）；Step 8 信封输出 | `GoodsErrorStatus` 以 `int code` + `name()` 输出、不进 JSON 序列化歧义；无持久化枚举列故 `@EnumValue` 不适用、VO 无枚举故 `@JsonValue` 不适用；无 Gson/Fastjson | `TEST-007`/`TEST-009` 序列化断言 | 7/8 | PASS |
| Rule 7 | Spec §6.2 Rule 7、`REQ-004` | 生成工程 base + dev/test/prod + `egon-mybatis-plus-sharding.yml` 五份文件共同承载 sharding/tianshu 键 | Step 10 五份 YAML 同树修改 + `ProfileKeyParityTest` | 键集合完全一致；值可不同（enabled 缺省翻转、brand 表注册到每个 `tables` 映射） | `TEST-013` 键集合比对 + 绑定 | 10 | PASS |
| Rule 9 | Spec §6.2 Rule 9、§13 | 品牌逻辑 Simple（单表、无状态机、无分发） | Step 6 编排直写 | 无 Strategy/State/Factory/Specification/Observer；变点仅守卫与查重顺序逻辑 | `BrandManageImplTest` 行为断言 | 6 | PASS |
| Rule 10 | Spec §6.2 Rule 10、`EVD-010`/`EVD-011` | `EgonModel`：`Instant` 审计列、`LocalDateTime deletedAt`；DDL 先例 `TIMESTAMP(6) WITH/WITHOUT TIME ZONE` | Step 2 DDL 时间列类型；Step 4 生成 PO 不重复时间列 | 不新增任何 `java.util.Date`/`Calendar`；本迭代自定义代码无时间字段（`PLAN-CLAR-011`：VO 不暴露审计时间） | `ArchetypeContractConvergenceTest` 静态禁止 + Step 11 回归 | 2/4 | PASS |
| Rule 11 | Spec §6.1/§6.2/§8 | 形态唯一：生成 light 单模块；持久化走 MP-SDJ starter；DDL 受管；唯一键含 `deleted_at`；CQE 无 Event | 全部 11 个 Step 的每个文件 | 无混合结构；`BrandRepository` 是唯一持久化入口；事件零产物 | 生成 verifier（`LightPersistenceArchitectureTest`，计数基线按 `DEC-117` 更新）+ Step 11 全量回归 | 全部 | PASS |

## 5. Change File Tree

```text
enjoyshop/
├── pom.xml                                                   CREATE 外层 reactor（packaging=pom，无 parent，modules 只列二级目录）
├── docs/egon/spec/2026-09-23-17-16-enjoyshop-scaffold-brand-crud.md   已存在（Status 已改 Accepted，随 Step 1 入库）
├── docs/egon/plan/2026-09-24-12-42-scaffold-brand-crud-implementation.md   已存在（本文件，随 Step 1 入库）
├── docs/egon/codegen/ddl-consumption-log.md                  CREATE classpath SQL 消费日志（Step 4，apply 成功后追加）
├── .gitignore                                                MODIFY 追加 `.egon/` 与 `.DS_Store`（Step 4）
├── enjoyshop-service/
│   ├── pom.xml                                               CREATE 二级聚合（modules 只列 enjoyshop-service-goods）
│   └── enjoyshop-service-goods/                              CREATE 由 egon-cola-archetype-light:5.4.1 生成（Step 1，单模块）
│       ├── pom.xml                                           GENERATED-ROOT 继承 top.egon:egon-cola-archetypes-parent:5.4.1
│       ├── codegen/brand-codegen.json                        CREATE 生成器配置（Step 4）
│       ├── .egon/codegen/                                    生成器状态目录（不提交）
│       ├── src/main/java/top/egon/enjoyshop/goods/common/
│       │   ├── enums/GoodsErrorStatus.java                   CREATE（Step 3）
│       │   └── error/GoodsErrorMapper.java                   CREATE（Step 3）
│       ├── src/test/java/top/egon/enjoyshop/goods/common/enums/
│       │   └── GoodsErrorStatusTest.java                     CREATE（Step 3）
│       ├── src/main/java/top/egon/enjoyshop/goods/domain/goods/
│       │   ├── Brand.java                                    GENERATED（Step 4）
│       │   ├── BrandDomainQuery.java                         GENERATED（Step 4）
│       │   └── BrandDomainService.java                       GENERATED（Step 4）→ MODIFY（Step 6 增补端口方法）
│       ├── src/main/java/top/egon/enjoyshop/goods/application/goods/
│       │   ├── CreateBrandCommand.java                       GENERATED（Step 4）
│       │   ├── UpdateBrandCommand.java                       GENERATED（Step 4）
│       │   ├── DeleteBrandCommand.java                       GENERATED（Step 4）
│       │   ├── BrandDetailQuery.java                         GENERATED（Step 4）
│       │   ├── BrandPageQuery.java                           GENERATED（Step 4）
│       │   ├── BrandResult.java                              GENERATED（Step 4）→ MODIFY（Step 6 增补 version）
│       │   ├── BrandManage.java                              GENERATED（Step 4）→ MODIFY（Step 6 增补 listBrands）
│       │   ├── BrandManageImpl.java                          GENERATED（Step 4）→ MODIFY（Step 6 守卫/查重/listBrands）
│       │   └── converter/
│       │       ├── CreateBrandCommandConverter.java          GENERATED（Step 4）
│       │       ├── UpdateBrandCommandConverter.java          GENERATED（Step 4）
│       │       ├── DeleteBrandCommandConverter.java          GENERATED（Step 4）
│       │       └── BrandResultConverter.java                 GENERATED（Step 4）→ MODIFY（Step 6 version 映射）
│       ├── src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/
│       │   ├── po/BrandPO.java                               GENERATED（Step 4）
│       │   ├── dao/BrandDAO.java                             GENERATED（Step 4）→ MODIFY（Step 5 增补四个方法）
│       │   ├── repo/BrandRepository.java                     GENERATED（Step 4）→ MODIFY（Step 5 增补查询与归一）
│       │   ├── converter/BrandPersistenceConverter.java      GENERATED（Step 4）
│       │   └── service/impl/BrandDomainServiceImpl.java      GENERATED（Step 4）→ MODIFY（Step 6 实现端口方法）
│       ├── src/main/resources/
│       │   ├── mybatis/mapper/goods/BrandDAO.xml             GENERATED（Step 4）→ MODIFY（Step 5 三个自定义语句）
│       │   └── db/egon-mp/
│       │       ├── V20260913_001__initialize_repository_schema.sql   生成工程自带（历史脚本，不可改）
│       │       ├── V20260923_001__initialize_goods_schema.sql        CREATE（Step 2）
│       │       └── repository-manifest.json                          GENERATED-ROOT 自带 → MODIFY（Step 2 追加条目）
│       ├── src/test/java/top/egon/enjoyshop/goods/infrastructure/goods/repo/
│       │   └── BrandQuerySqlShapeTest.java                   CREATE（Step 5）
│       ├── src/test/java/top/egon/enjoyshop/goods/application/goods/
│       │   └── BrandManageImplTest.java                      CREATE（Step 6）
│       ├── src/main/java/top/egon/enjoyshop/goods/adapter/
│       │   ├── goods/BrandController.java                    GENERATED（Step 4）→ MODIFY（Step 9 §9 五契约定制）
│       │   ├── goods/pojo/dto/
│       │   │   ├── CreateBrandRequest.java                   CREATE（Step 7）
│       │   │   ├── UpdateBrandRequest.java                   CREATE（Step 7）
│       │   │   └── BrandListRequest.java                     CREATE（Step 7）
│       │   ├── goods/pojo/vo/
│       │   │   ├── BrandVO.java                              CREATE（Step 7）
│       │   │   ├── FieldViolationVO.java                     CREATE（Step 7）
│       │   │   └── FieldErrorsVO.java                        CREATE（Step 7）
│       │   ├── goods/pojo/convertor/BrandAdapterConverter.java   CREATE（Step 7）
│       │   ├── filter/TenantContextFilter.java               CREATE（Step 9，写 MDC tenantId/userId，`DEC-116`）
│       │   └── handler/BaseExceptionHandler.java             CREATE（Step 8）
│       ├── src/main/java/top/egon/enjoyshop/goods/adapter/handler/
│       │   ├── ApiResponse.java                              GENERATED-ROOT 自带 → DELETE（Step 1，`DEC-115`）
│       │   ├── GlobalExceptionHandler.java                   GENERATED-ROOT 自带 → DELETE（Step 1，`DEC-115`）
│       │   └── ResponseWrapperHandler.java                   GENERATED-ROOT 自带 → DELETE（Step 1，`DEC-115`）
│       ├── src/test/java/top/egon/enjoyshop/goods/adapter/
│       │   ├── goods/pojo/convertor/BrandAdapterConverterTest.java   CREATE（Step 7）
│       │   ├── goods/BrandControllerWebTest.java             CREATE（Step 9）
│       │   ├── goods/BrandOpenApiContractTest.java           CREATE（Step 11）
│       │   └── handler/BaseExceptionHandlerTest.java         CREATE（Step 8）
│       ├── src/test/java/top/egon/enjoyshop/goods/adapter/handler/
│       │   └── ResponseWrapperHandlerTest.java               GENERATED-ROOT 自带 → DELETE（Step 1，`DEC-115`）
│       ├── src/test/java/architecture/LightPersistenceArchitectureTest.java   GENERATED-ROOT 自带 → MODIFY（Step 1 四个计数期望值 8→9/9/6，`DEC-117`）
│       └── src/main/resources/
│           ├── egon-mybatis-plus-sharding.yml                GENERATED-ROOT 自带 → MODIFY（Step 10 brand 表注册）
│           ├── application.yml                               GENERATED-ROOT 自带 → MODIFY（Step 10 brand 表注册；tianshu/yuheng 键保持缺省关闭）
│           ├── application-dev.yml                           GENERATED-ROOT 自带 → MODIFY（Step 10 enabled 缺省翻转 + brand 表注册）
│           ├── application-test.yml                          GENERATED-ROOT 自带 → MODIFY（Step 10 同上）
│           └── application-prod.yml                          GENERATED-ROOT 自带 → MODIFY（Step 10 同上）
│       ├── src/test/java/top/egon/enjoyshop/goods/start/config/
│       │   └── ProfileKeyParityTest.java                     CREATE（Step 10）
└── enjoyshop-web/
    └── pom.xml                                               CREATE 二级聚合（无工程内容，`DEC-102`/`DEC-110`）
```

不创建（有已批准决策）：`enjoyshop-gateway/`、`enjoyshop-service-api/`、`enjoyshop-common/`、`enjoyshop-transaction-fescar/`（`DEC-103` 至 `DEC-106`）；`docs/egon/spec` 下无新增 Spec。生成工程自带的样例域（teaching／user）保持原样，含其 GraphQL／MQ／Redis／幂等／proto 能力（`DEC-118` 记 Context-only，create-new-module 契约：样例替换属后续 Spec）；唯三例外是 Step 1 显式删除的 `adapter/handler/{ApiResponse,GlobalExceptionHandler,ResponseWrapperHandler}.java` 及其单测（`DEC-115`）。light 样例域与自带 `RequestContextFilter`／`TraceIdFilter` 均保持原样（`DEC-116` 只新增 `TenantContextFilter`，不改自带过滤器）。

## 6. Prerequisites, Constraints, and Plan Clarifications

### 6.1 Repository and worktree baseline

- 无 `AGENTS.md`（全仓检索 0 命中）；规范源为本 Plan 引用的四个 skill references 与用户逐字 Java 规则。
- 基线 `02cda6b (main)`；未跟踪路径仅 `docs/`（Spec、本 Plan）与 `.DS_Store`。执行时若出现其他脏文件，保持路径限位提交，不将其纳入任何 Step 的 Commit paths；`.DS_Store` 永不入库。
- 本 Plan 自身与 Spec 在 Step 1 首个提交中入库（docs 路径随骨架提交）。
- 生成器状态目录 `enjoyshop-service/enjoyshop-service-goods/.egon/` 是代码生成器唯一所有权记录，必须保留在磁盘但不提交；Step 4 将其加入 `.gitignore`。删除它会使后续再生成把同名文件判为 CONFLICT。
- 生成工程自带的 `db/migration/sharding/`（V/B 历史 Flyway 形态文件）为归档，任何 Step 不得触碰；`db/egon-mp/V20260913_001__initialize_repository_schema.sql` 及其 Manifest 旧条目不可改（`REQ-016`）。
- 生成工程自带的样例域代码（teaching／user，含其 GraphQL、MQ、Redis、幂等与 proto）本迭代不移除、不修改（`DEC-118` 记 Context-only）；自带 `RequestContextFilter`／`TraceIdFilter` 同样不动（`DEC-116` 只新增 `TenantContextFilter`）。唯三例外：Step 1 File 1b 删除 `adapter/handler/` 下三个自带类与其单测（`DEC-115`），Step 1 File 1c 更新 verifier 的四个计数期望值（`DEC-117`，语义断言不动）；样例 verifier 保持通过。

### 6.2 Build, test, and environment prerequisites

| Concern | Exact command/source | Required state | Validation boundary |
| --- | --- | --- | --- |
| 本仓构建 | `mvn -N validate`（enjoyshop 根，系统 Maven 3.9.x；本仓无 wrapper） | 退出码 0 | 静态/模块 |
| 生成命令 | Egon-COLA 仓库根 `/Users/mario/SelfProject/Egon-COLA` 下 `./mvnw -B -ntp archetype:generate ...` | `egon-cola-archetype-light/5.4.1` 已在 `/Users/mario/maven/repository`（已核实） | 静态/模块 |
| 生成器类路径 | Egon-COLA 根：`./mvnw -B -ntp -pl egon-cola-components/egon-cola-component-code-generator -am install -DskipTests`，随后 `dependency:build-classpath -DincludeScope=runtime -Dmdep.outputFile=/tmp/egon-codegen.cp`，再 `export EGON_CODEGEN_CLASSPATH="egon-cola-components/egon-cola-component-code-generator/target/classes:$(cat /tmp/egon-codegen.cp)"` | 生成器模块已 install（5.4.1 本地已核实）；类路径每个条目存在，否则 `BLOCKED_TOOLING`（退出码 7） | 静态 |
| 生成工程构建/测试 | `cd enjoyshop-service/enjoyshop-service-goods && ./mvnw ...`（生成工程自带 wrapper） | 父 POM `egon-cola-archetypes-parent:5.4.1` 与 Components BOM 可从本地仓库解析 | 模块 |
| Java 运行时 | Java 21（archetype `java.version`） | `JAVA_HOME` 指向 21 | 静态 |
| 数据库/天枢/玉衡 | 无（`DEC-107`/`DEC-111` 选 A；禁止连库、禁容器、禁启动应用） | 不需要 | 不适用 |

### 6.3 Immutable constraints and approved decisions

- 已应用脚本与校验和不可改：`V20260913_001__initialize_repository_schema.sql` 与 Manifest 既有条目（`REQ-016`）；失败时新增下一脚本（无 down DDL）。
- 不连接 PostgreSQL、不执行 DDL、不启动应用、不跑容器（Spec §3.2 用户长期约束）；`EXPLAIN`、`ddl_history`、真实索引行为属部署后验证（§9）。
- `DEC-109` 选 A：全部响应为 `ResultRecord`/`PageResultRecord` 信封并保留 `BaseExceptionHandler` 名；改信封必须新 Spec。
- `DEC-005`：ID 由 `@TableId(type = ASSIGN_ID)` 雪花生成，业务代码不手写 ID。
- `ASM-006` 错误码区段 `6101xx`（610101/610102/610103/610104/610105/610199）不可漂移；`code` 为 `int`，`status` 为枚举 `name()`。
- FreeMarker 仅属生成器工具依赖（2026-09-22 用户批准），不得进入生成工程 POM；除此之外零新增依赖（`REQ-021`）。
- 代码生成 `plan` 出现 `CONFLICT`、`CHECKSUM_DRIFT` 或 `BLOCKED_TOOLING` 即停止该 Step，禁止手写补丁绕过（Spec §4.1、backend-code-generation）。

### 6.4 Plan Clarifications

| ID | Small implementation inference | Repository evidence | Why semantics are unchanged | Impact if wrong |
| --- | --- | --- | --- | --- |
| `PLAN-CLAR-001` | `brand` 继承列类型按 `EgonModel` 实型修正：`create_user_id`/`update_user_id` 为 `VARCHAR(128) NOT NULL DEFAULT 'migration'`，`version` 为 `BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0)`；Spec §11.2.1 表中的 `BIGINT`（审计用户）/`INTEGER`（version）不予采用 | `EgonModel.java`：`createUserId`/`updateUserId` 为 `String`、`version` 为 `Long`；生成器 `GenerationScopeValidator.requireBase` 硬性要求 `varchar` 非空 / `bigint` 非空（`BASE_FIELD_TYPE` 诊断会拒绝 Spec 字面值，`plan` 直接失败）；light/web 两份 `V20260913_001` 先例同型 | Spec 自身规范即"继承列类型由继承列决定"（§11.2.1 时间列注记）且 §6.2 Rule 10 要求与 `EgonModel` 逐列一致；本修正恰是执行该规范，四业务列与索引设计逐字保留 | 若按 Spec 字面写 DDL，Step 4 `plan` 返回 `BASE_FIELD_TYPE` 诊断，无法生成 |
| `PLAN-CLAR-002` | 22 个 GENERATED 文件的最终路径/类型名/Bean 名以生成器实测规则为准：`BrandPersistenceConverter`（非 Spec 的 `BrandPOConverter`）、`BrandPageQuery`/`BrandDetailQuery`/`BrandDomainQuery`（Spec §10.1 拒绝清单只约束手写类，不约束目录产物）、域模型与域查询/服务平铺于 `top.egon.enjoyshop.goods.domain.goods`、域实现位于 `infrastructure.goods.service.impl`、CQE 平铺于 `application.goods`、控制器位于 `adapter.goods`；Bean 名 `brandManage`（非 Spec 字面 `brandManageImpl`） | `ProjectLayoutStrategy.java` 的 `javaPackage`/`moduleKey` 与 `GenerationScopeValidator` 第 77-133 行命名推导；`@Service("[=manageBean]")` 模板即 `brandManage` | Spec §8.2 已把这些文件标为 GEN 并声明"由生成器产出、不由模型手抄"；HTTP 契约、字段、行为均不变 | 生成后路径与本 Plan §5 树逐一对得上即无影响；以 `plan` JSON 文件清单为准 |
| `PLAN-CLAR-003` | 生成的 Command/Query/Result/域模型为 Lombok class（`@Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain=true) @Builder`，PO 另有 `@SuperBuilder`+`@EqualsAndHashCode(callSuper=true)`），而非 Spec §10.3.1 设想的 record；adapter 手写的 Request/VO/载体仍为 record | `command.ftl`/`query.ftl`/`result.ftl`/`domain-model.ftl`/`po.ftl` 模板原文 | Rule 3 允许 record 仅用于不可变值对象，class + 完整注解组合是规范基线；归一化（trim/大写/缺省 0）由 Spec §10.4 指定的映射所有者 `BrandAdapterConverter`（`@Named` 限定方法）承担，位置平移、语义不变 | 无外部可观察差异；`TEST-001` 按实际表示断言 |
| `PLAN-CLAR-004` | Spec §9.2 的 downstream 符号名映射到生成链：`BrandManage#create/update/delete/detail/page` 承载 createBrand/updateBrand/deleteBrand/getBrand 语义；列表用新增端口方法 `BrandDomainService#listPage/countPage` + `BrandManage#listBrands`（含 total）；单接口不可继承两份 `BaseForwardConverter<S,T>` 泛型实例（Java 规则），`BrandAdapterConverter` 以 `BrandResult→BrandVO` 为主继承方向、其余映射为同接口 MapStruct 方法 | 生成 `manage.java.ftl`/`domain-service.java.ftl` 方法签名；backend-code-generation"Domain Service 实现只经 Repository、不触 DAO"；Java 泛型继承限制 | 对外 HTTP 契约（operationId、载荷、状态码）逐字不变；内部方法名是实现细节 | 无 |
| `PLAN-CLAR-005` | ~~不新建 `TenantContextFilter`，改 web 自带 `OrganizationAuthContextFilter` 一行~~ → **被 `PLAN-CLAR-014` 取代**：light 形态下不存在该类，且自带 `RequestContextFilter` 不写 MDC，故改为新增 `TenantContextFilter`（`DEC-116`） | Spec `EVD-027`（light 自带过滤器用 ThreadLocal）取代原 `EVD-013`（web 自带过滤器写 MDC） | §7.3.1 步骤 1 的可观察行为与 `TEST-016` 的 401 断言不变 | 保留本行以记录口径变更，不作为实施依据 |
| `PLAN-CLAR-006` | 新增工程自带错误边界 `top.egon.enjoyshop.goods.common.error.GoodsErrorMapper`（静态 `missing(Long)`/`zeroRows(String,Long,Long)` 返回 `CommonException(GoodsErrorStatus)`）：`create→610103`、`update→610104`、`delete→610102`、缺行→`610102`；生成 `BrandDomainServiceImpl` 以 `[errorMapper].zeroRows/missing` 直接引用它 | `GenerationScopeValidator.java:141` 把 `apiContract.existingErrorMapper` 插入模板；`domain-impl.ftl` 的 `throw [errorMapper].zeroRows(...)`；backend-code-generation 要求 apply 前"工程已提供全部被引用类型" | Spec §7.3.4/§9.2 的状态与码映射逐条落位（缺行 404 先于版本冲突 409 的判定顺序由生成实现的 `getById` 前置读保证）；不新建异常类型，复用 `CommonException(ErrorStatus)`（Spec 字面的 `IllegalStateException` 改为携带码的 `CommonException`，可观察的 HTTP 行为与 `TEST-016` 断言完全一致） | 若该类型缺失，apply 后生成代码无法编译 |
| `PLAN-CLAR-007` | `BaseExceptionHandler` 以 `@RestControllerAdvice(name="baseExceptionHandler")` 注册，**不加** `basePackages` 限定——因为 light 自带的 `GlobalExceptionHandler` 已在 Step 1 File 1b 删除（`DEC-115`），`BaseExceptionHandler` 是品牌链路唯一 advice，限定反而会漏掉跨包异常 | 原 web 口径是限定品牌包以避开自带 `OrganizationGlobalExceptionHandler`；light 下的冲突源已删除（Spec `EVD-027`） | §9 的五类错误映射逐条落位，`TEST-002`／`TEST-016` 的信封与状态码断言不再受第三方 advice 干扰 | 若保留 `basePackages`，`adapter` 包以外的异常将退回 Boot 默认错误体 |
| `PLAN-CLAR-008` | `brand` 脚本为生成工程 Manifest（family `web`，已含 `20260913_001`）之后的下一版本：文件名沿用 Spec 的 `V20260923_001__initialize_goods_schema.sql`，追加条目不改旧条目；脚本内 `SHARD` 角色分支为 `NULL`（brand 为 `SINGLE` 主数据表，分片目标不承载），沿用样例的 `egon_migration.role` 分支形状 | 生成工程自带 `db/egon-mp/repository-manifest.json`（family web + 1 条目）与样例脚本角色分支 | Spec"一次变更 = 一份新脚本 + Manifest 追加"原样成立；版本排序 `20260913_001 < 20260923_001` | 若无 SHARD no-op 分支且 Runner 以 SHARD 角色执行该脚本，目标库迁移报 `Unknown managed DDL role` |
| `PLAN-CLAR-009` | 操作者守卫与名称前置查重落位：守卫在 `BrandManageImpl` 三个写方法开头（MDC `userId` 空白 → `CommonException(BRAND_OPERATOR_REQUIRED)`）；查重经新增域端口 `BrandDomainService#existsActiveByName` → `BrandRepository#countActiveByName`（tenant 谓词由 MP 拦截器注入、`deleted_at IS NULL` 在自定义 SQL 内），create 排除 `null`、update 排除自身 id | 生成链依赖方向为 `application → domain → infrastructure`（application POM 不依赖 infrastructure，GradeManageImpl 同构）；Spec §7.3.4 的"Manage/Domain 前置守卫"与 `TEST-011`/`TEST-016` 断言（401/409 + 零持久层交互）全部保持 | 可观察行为与 Spec 逐条一致；仅"Manage 直调 Repository"的字面路径改为端口转发（目录契约禁止应用层触达 Repository） | 无 |
| `PLAN-CLAR-010` | `BrandManage#listBrands` 返回 `PageResultRecord<BrandResult>` 仅作为 records+total 的应用层载体，控制器取出 records 映射 VO 后按 §7.3.1 步骤 9 重建信封（信封构造仍在 controller） | `PageResultRecord.success(records,total,pageNo,pageSize)` 工厂已核对；tianshu Admin 有同形先例 | 零新增载体类；HTTP 载荷与 §9.2.1 逐字段一致 | 无 |
| `PLAN-CLAR-011` | `BrandVO` 载荷为 `id`、`name`、`image`、`letter`、`seq`、`version`，不包含 `createTime`/`updateTime`；`BrandResult` 仅增补 `version`（乐观锁契约功能必需），审计时间不在本迭代暴露 | 生成器 `policyFields` 显式跳过全部 `BASE_COLUMNS`（含审计与 `version`），时间值在 domain/application 层不可达；`version` 在域模型上存在可直通；archetype 先例 `GradeDetailVO` 仅业务字段；生成 application.yml `default-property-inclusion: non_null` | Spec §9.2 载荷中两个展示字段缺席：无任何消费行为依赖时间（§9.2.4 过期判断用 `version` 足够）；乐观锁契约（`version` 必填回传）完整保留；这是对 `ASM-005` 的证据驱动收窄，已在 §11 登记 `RISK-P2`，恢复路径见 §9 | 若前端强依赖时间字段，需后续 Spec 修订（侵入域模型与持久化转换器两个生成文件），属可见契约变更需用户确认 |
| `PLAN-CLAR-012` | 生成器配置落地为工程内持久文件 `enjoyshop-service/enjoyshop-service-goods/codegen/brand-codegen.json` 并入库（未来再生成的输入）；`input.mode=manifest` 直读 Manifest（含 sha256 校验），不复制 SQL 副本 | codegen README 配置契约（`configVersion=1`、未知键拒绝、`manifest` 模式）+ `codegen-web.json` 样例 | 配置即 backend-code-generation 要求的"模型只写 SQL、Manifest、生成器配置"第三件；无语义影响 | 若不入库，未来 SQL 变更需重建配置，易漂移 |
| `PLAN-CLAR-013` | `DEC-101` 改选 light 后，Step 1 除生成工程外还负责**删除**三个自带信封类与**更新**自带 verifier 的四个计数期望值；删除清单为 `adapter/handler/{ApiResponse,GlobalExceptionHandler,ResponseWrapperHandler}.java` 与 `adapter/handler/ResponseWrapperHandlerTest.java` | Spec `EVD-027`（`ResponseWrapperHandler` 的 `@ControllerAdvice(basePackages=\"${package}.adapter\")` 加 `ResponseBodyAdvice` 会二次包装 `ResultRecord`；`GlobalExceptionHandler` 无 `basePackages` 限定并占用三类校验异常）与 `EVD-020`（`LightPersistenceArchitectureTest.java:47,55,56,57` 的硬编码计数） | 品牌的五契约、信封字段、错误码与 HTTP 状态逐字不变；变的只是样例域端点的错误外形（Spec `RISK-014` 已登记该代价）与 verifier 的数字基线（语义断言一字不动） | 若不删除，`BaseExceptionHandler` 与自带 advice 的选择不确定，`TEST-002`／`TEST-016` 的信封断言不可靠；若不改计数，Step 11 全量测试必失败 |
| `PLAN-CLAR-014` | `TenantContextFilter` 由「修改 web 自带 `OrganizationAuthContextFilter` 一行」改为**新增**独立过滤器（`DEC-116`）；自带 `RequestContextFilter`（`X-Operator-Id` 加 ThreadLocal）与 `TraceIdFilter`（MDC `traceId`）保持原样 | Spec `EVD-027`：light 的 `RequestContextFilter` 用 `RequestContextHolder`（ThreadLocal）而**不写 MDC**，而 `EVD-012` 的 MP-SDJ 租户拦截器只从 MDC `tenantId` 取值 | §7.3.1 步骤 1 的可观察行为（`X-Tenant-Id` 缺省写 `1`、操作者缺省写空白、finally 清理）与 `TEST-016` 的 401 断言完全一致；只是实现落点从「改自带」变为「新增」 | 若沿用自带过滤器而不写 MDC，每条 SQL 落库都 `TENANT_CONTEXT_MISSING` |
| `PLAN-CLAR-015` | codegen 配置的 `projectType` 由 `web` 改为 `light`，并**删除** `modulePaths` 块；22 个产物的 Java 包名与文件名一字不改 | Spec `EVD-028`：`CodegenProfileEnum.LIGHT.allowsController()` 为 true；`LightLayout` 与 `WebLayout` 的 `allows` 集合相同且共用同一个 `javaPackage(config, artifact)` 映射，唯一差别是 `LightLayout.relativePath` 用空前缀 | Step 4 的产物清单、Step 5 至 Step 9 的包路径、Step 10 的测试位置全部沿用原 Plan 文本，只需去掉 Maven 模块目录层 | 若误留 `modulePaths`，`LightLayout` 不读它（无副作用），但配置文件会误导后续维护者 |
| `PLAN-CLAR-016` | Step 10 的 `ProfileKeyParityTest` 从 `enjoyshop-service-goods-starter/src/test/...` 移到单模块的 `src/test/java/top/egon/enjoyshop/goods/start/config/`；各 Step 的验证命令去掉全部 `-pl` 模块选择器，改为整工程加 `-Dtest=测试类名` | light 是单模块工程，没有 `enjoyshop-service-goods-starter` 这个 Maven 模块；其自带配置类位于 `top.egon.enjoyshop.goods.start.config`（`EVD-027` 的 pom 与 `OpenApiConfig` 等） | 测试目标、四份 profile 与 sharding yml 的键集合断言完全不变 | 若保留 `-pl`，Maven 直接报 `Could not find the selected project in the reactor` |

## 7. Ordered File-by-file Implementation Steps

### Step 1 — 生成 enjoyshop-service-goods 单模块工程、删除自带全局信封设施并建立外层与二级聚合 POM

- Requirements: `REQ-001`, `REQ-002`, `REQ-003`, `REQ-005`, `REQ-006`
- Dependencies: `None`（`BLOCK-001` 已于 2026-09-24 关闭）
- Baseline state: 仓库仅 README、.gitignore、.agents 软链与未跟踪的 docs/；无任何 POM；`egon-cola-archetype-light:5.4.1` 与其 parent/BOM 已在本地仓库 `/Users/mario/maven/repository`（已核实，其 `archetype-metadata.xml` 的 requiredProperties 只有 `gitignore`，无对端 facade）。
- Observable outcome: `enjoyshop-service/enjoyshop-service-goods` 单模块工程存在且 parent 链保持 `top.egon:egon-cola-archetypes-parent:5.4.1`；`adapter/handler/` 下只剩 `BaseExceptionHandler.java`（本 Step 尚未创建，此时为空目录亦可），全仓 `grep -rn "ResponseWrapperHandler\|GlobalExceptionHandler\|ApiResponse"` 在 `src/` 下零命中；`LightPersistenceArchitectureTest` 的四个计数期望值为 9/9/9/6；`mvn -N validate`（根）与生成工程 `./mvnw -q validate` 均退出码 0。
- End state: 三个聚合 POM + 生成工程样例内容入库（已剔除三个自带信封类与一个自带单测，verifier 计数基线已更新；`db/egon-mp` 既有脚本与样例域保持原样）；文档（Spec + 本 Plan）随本提交入库；`gateway`/`fescar`/`service-api`/`common` 四个目录确认不存在。
- Test-first gate: `Not applicable` — 纯工程骨架与构建清单，无行为可测；验收即两条 validate 命令与目录形状（`REQ-001` 的验收原文）。
- Manual Checks: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-SCOPE-001`, `MC-TEST-001`
- Literal Rules: `Rule 11`
- Ordered files:

#### File 1 — `CREATE enjoyshop-service/enjoyshop-service-goods`

- Purpose: 用 create-new-module 契约从唯一非 open archetype 生成商品工程骨架。
- Symbols: 生成 root GAV `top.egon.enjoyshop:enjoyshop-service-goods:1.0.0-SNAPSHOT`，基础包 `top.egon.enjoyshop.goods`；**单模块**，COLA 分层以 `src/main/java/top/egon/enjoyshop/goods/{common,facade,domain,application,infrastructure,adapter,start}` 包承载（`DEC-101` 选项 B）。
- Repository evidence: `archetype-selection.md` 的选择表（`light` = `expectedTopology=root`，`Light and agent have no peer-facade properties`）与命令形状；本地仓库已含 `top/egon/egon-cola-archetype-light/5.4.1`（解包核对：`requiredProperties` 只含 `gitignore`）。
- Dependencies and consumers: 后续全部 Step 的文件载体；外层 reactor 将聚合此 root。
- Why now: 一切代码与 DDL 必须先有宿主工程；生成是本迭代唯一合法的骨架来源（`REQ-003`）。
- Contract/signature changes: 新建工程 root POM 的 `<parent>` 为 `top.egon:egon-cola-archetypes-parent:5.4.1` + 空 `<relativePath/>`；**唯一**的生成物改动是 `DEC-115` 的三个类删除与 `DEC-117` 的四个期望数字，两者都在 §5 树与 Commit paths 中显式列出。
- Input/output and state mapping: 输入为 6 个 `-D` 属性（无对端 facade）；输出为 `<outputDirectory>/<artifactId>` 目录树；MDC/无状态。
- Error and edge behavior: archetype 无法解析即停止并展示 Maven 错误（禁止本地 install、禁止 `generate_archetypes.sh`）。删除三个自带类后必须全仓检索确认无残留引用（尤其 `ArchetypeContractConvergenceTest` 是否引用 `ApiResponse`）——若仍有引用则本 Step 阻塞，不得改测试绕过。
- Standards impact: `MC-ARCH-001`, `MC-DEP-001`, `MC-SCOPE-001` — 形态唯一、零依赖新增、四个候选工程不建。
- Literal rule enforcement: `Rule 11` — 骨架只能由 archetype 生成，禁止复制 `source-projects` 或手写模块树。
- Implementation pseudocode:

```bash
cd /Users/mario/SelfProject/Egon-COLA
./mvnw -B -ntp archetype:generate \
  -DarchetypeGroupId=top.egon \
  -DarchetypeArtifactId=egon-cola-archetype-light \
  -DarchetypeVersion=5.4.1 \
  -DgroupId=top.egon.enjoyshop \
  -DartifactId=enjoyshop-service-goods \
  -Dversion=1.0.0-SNAPSHOT \
  -Dpackage=top.egon.enjoyshop.goods \
  -DoutputDirectory=/Users/mario/SelfProject/enjoyshop/enjoyshop-service \
  -DinteractiveMode=false
# 生成后核对（create-new-module 验收项）：
ls enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/   # common facade domain application infrastructure adapter start 八个包目录
grep -A2 "<parent>" enjoyshop-service/enjoyshop-service-goods/pom.xml   # parent=egon-cola-archetypes-parent:5.4.1，relativePath 空
```

#### File 1b — `DELETE` light 自带的全局信封设施（`DEC-115`）

- Purpose: 移除会对品牌链路二次包装与争抢异常处理的三个自带类，使 `ResultRecord` 信封与 `BaseExceptionHandler` 成为品牌链路上唯一的信封与唯一的 advice。
- Symbols: 删除 `src/main/java/top/egon/enjoyshop/goods/adapter/handler/{ApiResponse,GlobalExceptionHandler,ResponseWrapperHandler}.java` 与 `src/test/java/top/egon/enjoyshop/goods/adapter/handler/ResponseWrapperHandlerTest.java`。
- Repository evidence: Spec `EVD-027`——`ResponseWrapperHandler` 是 `@ControllerAdvice(basePackages="${package}.adapter")` 加 `ResponseBodyAdvice`，`beforeBodyWrite` 把非 `ApiResponse` 返回值一律包成 `ApiResponse.success(body)`；`GlobalExceptionHandler` 是无 `basePackages` 限定的 `@RestControllerAdvice`，已占用 `MethodArgumentNotValidException`／`ValidationException`／`IllegalArgumentException`。
- Dependencies and consumers: Step 8 的 `BaseExceptionHandler`、Step 9 的 `BrandController`；样例域 teaching／user 端点将回退到 Spring 默认错误处理（Spec `RISK-014` 已登记该代价）。
- Why now: 必须在任何品牌代码落盘前完成，否则 Step 8/9 的信封断言会被自带 advice 污染。
- Contract/signature changes: 品牌 HTTP 契约不变（`ResultRecord`／`PageResultRecord` 加 6101xx 业务码）；变化的只是样例域端点的错误响应外形。
- Input/output and state mapping: 纯删除，无新增符号。
- Error and edge behavior: 删除后全仓 `grep -rn "ApiResponse\|ResponseWrapperHandler\|GlobalExceptionHandler" src/` 必须零命中；若 `ArchetypeContractConvergenceTest` 或其他自带测试引用了它们，本 Step 阻塞并按 Spec `DEC-115` 报告，不得弱化自带断言。
- Standards impact: `MC-ARCH-001`, `MC-SCOPE-001` — 删除范围严格限于这四个文件，样例域其余代码不动。
- Literal rule enforcement: `Rule 11` — 不混用形态；`Rule 4` — 删除后品牌 advice 由 Step 8 以显式 Bean 名注册。
- Implementation pseudocode:

```bash
cd /Users/mario/SelfProject/enjoyshop/enjoyshop-service/enjoyshop-service-goods
rm src/main/java/top/egon/enjoyshop/goods/adapter/handler/ApiResponse.java \
   src/main/java/top/egon/enjoyshop/goods/adapter/handler/GlobalExceptionHandler.java \
   src/main/java/top/egon/enjoyshop/goods/adapter/handler/ResponseWrapperHandler.java \
   src/test/java/top/egon/enjoyshop/goods/adapter/handler/ResponseWrapperHandlerTest.java
grep -rn "ApiResponse\|ResponseWrapperHandler\|GlobalExceptionHandler" src/ ; test $? -eq 1   # 期望零命中
```

- Verification contribution: `DEC-115` 的可观察验收（品牌链路信封唯一）。
- After this file: `adapter/handler/` 为空目录，等待 Step 8 写入 `BaseExceptionHandler.java`。

#### File 1c — `MODIFY src/test/java/architecture/LightPersistenceArchitectureTest.java`（`DEC-117`）

- Purpose: 把自带 verifier 对样例域文件数的硬编码期望值更新为加一后的基线，使 Step 4 生成 `brand` 产物后 verifier 仍可通过。
- Symbols: 四个 `assertEquals`：`Repository.java` 8→9、`PO.java` 8→9、`DAO.java` 8→9、`DomainServiceImpl.java` 5→6。**不改**任何语义断言（`extends EgonModel<`、`extends EgonColaMapper<`、`extends EgonColaRepository<`、`repo.dao`／`repo.impl`／`repo.mapper` 禁止、pom 依赖断言等）。
- Repository evidence: Spec `EVD-020` 与解包核对——`LightPersistenceArchitectureTest.java:47,55,56,57` 为 `assertEquals(8, count(javaFiles, "Repository.java"))`、`assertEquals(8, ... "PO.java")`、`assertEquals(8, ... "DAO.java")`、`assertEquals(5, ... "DomainServiceImpl.java")`。
- Dependencies and consumers: Step 4 的 22 个产物（各贡献 1 个 PO／DAO／Repository／DomainServiceImpl）、Step 11 的全量 `./mvnw test`。
- Why now: 必须在 Step 4 之前改好；否则 Step 4 之后 verifier 立即失败，而 Plan 禁止手改生成器产物或弱化断言绕过。
- Contract/signature changes: 仅测试期望值，无生产契约变化。
- Input/output and state mapping: 数字字面量替换，四个 `assertEquals` 的第一个参数。
- Error and edge behavior: 误改语义断言即失去 verifier 保护；Step 1 提交前用 `git diff` 逐行复核只改四个数字。
- Standards impact: `MC-TEST-001` — 全量测试闸门保持强度。
- Literal rule enforcement: `Rule 11` — 持久化归属断言（`EgonColaRepository`／禁止 `repo.dao`）一字不动。
- Implementation pseudocode:

```bash
cd /Users/mario/SelfProject/enjoyshop/enjoyshop-service/enjoyshop-service-goods
# 仅替换四个 assertEquals 的第一个数字，其余文本不动
git diff -- src/test/java/architecture/LightPersistenceArchitectureTest.java   # 期望恰好 4 行变更
```

- Verification contribution: `DEC-117` 的可观察验收；Step 11 全量测试能作为硬闸门。
- After this file: verifier 基线就位，Step 4 生成后不因计数漂移失败。

- Verification contribution: `REQ-003` 的"生成 root 为 light 单模块且含八层包目录"验收；`BLOCK-001` 已关闭，生成命令不含任何对端 facade 参数。
- After this file: 生成工程完整存在；样例域与自带测试保持原样；尚无任何本迭代业务文件。

#### File 2 — `CREATE pom.xml`

- Purpose: 仓库根外层 reactor，聚合二级目录。
- Symbols: GAV `top.egon.enjoyshop:enjoyshop:1.0.0-SNAPSHOT`，`packaging=pom`，modules = [`enjoyshop-service`, `enjoyshop-web`]。
- Repository evidence: multi-project-parent "Default: a pure outer aggregator"（无 `<parent>`、只列工程根）；Spec `REQ-001`。
- Dependencies and consumers: Maven 命令行；两个二级聚合 POM。
- Why now: reactor 只能在子目录存在后列出它们。
- Contract/signature changes: 新文件；无 `<parent>`、无 `<dependencyManagement>`、无插件配置（`REQ-001` 禁区）。
- Input/output and state mapping: 纯构建期；`-N` 校验只读本文件。
- Error and edge behavior: 模块路径不存在时 validate 报错；不添加任何业务依赖。
- Standards impact: `MC-ARCH-001`, `MC-SCOPE-001` — reactor 不充当 parent、不携带平台源码模块。
- Literal rule enforcement: `Rule 11` — 外层只聚合，不改生成工程 parent 链。
- Implementation pseudocode:

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>top.egon.enjoyshop</groupId>
    <artifactId>enjoyshop</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>
    <modules>
        <module>enjoyshop-service</module>
        <module>enjoyshop-web</module>
    </modules>
</project>
```

- Verification contribution: `mvn -N validate` 通过即 `REQ-001` 验收。
- After this file: 根 reactor 可校验；未列任何内部模块。

#### File 3 — `CREATE enjoyshop-service/pom.xml`

- Purpose: service 二级目录聚合。
- Symbols: GAV `top.egon.enjoyshop:enjoyshop-service:1.0.0-SNAPSHOT`，`packaging=pom`，modules = [`enjoyshop-service-goods`]。
- Repository evidence: Spec §8.2 二级聚合职责；`REQ-002`。
- Dependencies and consumers: 根 reactor；生成工程 root。
- Why now: 分组目录与生成工程在 Step 1 同时就位。
- Contract/signature changes: 新文件；只列工程根，不重复列内部模块。
- Input/output and state mapping: 纯构建期聚合。
- Error and edge behavior: 无 parent 继承、无依赖声明。
- Standards impact: `MC-ARCH-001`, `MC-SCOPE-001` — 不装不存在的兄弟工程（gateway/fescar 不建）。
- Literal rule enforcement: `Rule 11` — 聚合不改变生成工程的直接 Egon parent。
- Implementation pseudocode:

```xml
<project ...>
    <modelVersion>4.0.0</modelVersion>
    <groupId>top.egon.enjoyshop</groupId>
    <artifactId>enjoyshop-service</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>
    <modules>
        <module>enjoyshop-service-goods</module>
    </modules>
</project>
```

- Verification contribution: 全 reactor `mvn validate` 时该 POM 被解析。
- After this file: service 二级目录聚合完成。

#### File 4 — `CREATE enjoyshop-web/pom.xml`

- Purpose: web 二级目录聚合占位（本迭代无工程内容）。
- Symbols: GAV `top.egon.enjoyshop:enjoyshop-web:1.0.0-SNAPSHOT`，`packaging=pom`，无 modules 元素（本迭代无工程内容）。
- Repository evidence: Spec §8.2 "仅二级聚合 POM"；`DEC-102`/`DEC-110` 选 A。
- Dependencies and consumers: 根 reactor。
- Why now: 用户条目 2 要求目录形状一次到位；后续聚合工程由后续 Spec 生成。
- Contract/signature changes: 新文件；空聚合（`REQ-005` 禁空模块——目录聚合不是模块，无制品产出）。
- Input/output and state mapping: 纯构建期。
- Error and edge behavior: 空 modules 合法；不加任何占位子工程。
- Standards impact: `MC-ARCH-001`, `MC-SCOPE-001` — 不为凑形状创建空业务工程。
- Literal rule enforcement: `Rule 11` — 占位聚合不引入第三种结构。
- Implementation pseudocode:

```xml
<project ...>
    <groupId>top.egon.enjoyshop</groupId>
    <artifactId>enjoyshop-web</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>
    <!-- enjoyshop-web 的聚合工程由后续 Spec 生成；本迭代无工程内容 -->
</project>
```

- Verification contribution: 根 reactor validate 覆盖。
- After this file: 目录形状与 §8.2 一致。

- Validation working directory: `/Users/mario/SelfProject/enjoyshop`
- Verification command: `mvn -N validate && cd enjoyshop-service/enjoyshop-service-goods && ./mvnw -q validate && ls src/main/java/top/egon/enjoyshop/goods/ && ! grep -rq "ApiResponse\|ResponseWrapperHandler\|GlobalExceptionHandler" src/ && grep -c "assertEquals(9\|assertEquals(6" src/test/java/architecture/LightPersistenceArchitectureTest.java`
- Expected result: 前两条命令退出码 0；八个包目录（`common facade domain application infrastructure adapter start`）存在；`grep -rq` 无命中（`!` 使其成功）；`grep -c` 输出 `4`；生成 root parent 为 `egon-cola-archetypes-parent:5.4.1`；`ls enjoyshop-gateway enjoyshop-common enjoyshop-service-api enjoyshop-transaction-fescar` 均报不存在。
- Failure returns to: File 1（生成失败／parent 链被改）、File 1b（删除后仍有残留引用）或 File 1c（误改语义断言）。
- Completion criteria: `REQ-001`/`REQ-002`/`REQ-003` 的验收命令与目录核对全部成立；`DEC-115` 的四个文件确已删除且无残留引用；`DEC-117` 的四个期望值确为 9/9/9/6 且语义断言未动；文档两份随提交入库。
- Rollback: `git rm -r enjoyshop/ pom.xml`（本提交为纯新增，可整体 revert）。
- Commit paths: `pom.xml`, `enjoyshop-service/pom.xml`, `enjoyshop-web/pom.xml`, `enjoyshop-service/enjoyshop-service-goods/`, `docs/egon/spec/2026-09-23-17-16-enjoyshop-scaffold-brand-crud.md`, `docs/egon/plan/2026-09-24-12-42-scaffold-brand-crud-implementation.md`
- Commit: `feat(scaffold): 生成 enjoyshop-service-goods 单模块工程并建立外层与二级聚合 POM`

### Step 2 — 新增 brand 受管 DDL 脚本与 Manifest 条目

- Requirements: `REQ-015`, `REQ-016`, `REQ-017`
- Dependencies: `Step 1`
- Baseline state: 生成工程自带 `db/egon-mp/V20260913_001__initialize_repository_schema.sql` 与 `repository-manifest.json`（family `web`，1 条目，sha256 `39b04ed6…`）；两者不可修改。
- Observable outcome: 新脚本与 Manifest 追加条目存在，`shasum -a 256` 输出与 Manifest 记录逐字节一致；旧条目与旧脚本未变（`git diff` 仅新增行）。
- End state: 受管 DDL 历史含两个版本；brand 表具备 §11.2.1 全部列/索引/CHECK（继承列类型按 `PLAN-CLAR-001`）。
- Test-first gate: `Not applicable` — 数据定义无行为测试可执行；禁止连接 PostgreSQL（Spec §3.2），验证为校验和与静态逐列比对（`TEST-012` 的静态半部）。
- Manual Checks: `MC-ARCH-001`, `MC-MODEL-001`, `MC-TIME-001`, `MC-SCOPE-001`, `MC-TEST-001`
- Literal Rules: `Rule 10`, `Rule 11`
- Ordered files:

#### File 1 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/resources/db/egon-mp/V20260923_001__initialize_goods_schema.sql`

- Purpose: 一次数据库变更的唯一新版本：brand 表 + 唯一索引 + 排序索引 + CHECK。
- Symbols: 表 `brand`；索引 `uk_brand_tenant_name_active`（部分唯一）、`idx_brand_tenant_seq_id`；约束 `ck_brand_seq`；角色分支 `MASTER_DATA`/`SHARD`。
- Repository evidence: 生成工程自带 `V20260913_001` 的角色分支形状、列顺序、`CHECK (version >= 0)` 与部分唯一索引写法（light/web 双先例）；`EgonModel.java` 字段类型。
- Dependencies and consumers: 被 `repository-manifest.json` 记录、被生成器 `manifest` 输入模式消费、被 `EgonColaPostgreDdlRunner` 在未来启动时执行（本迭代不执行）。
- Why now: backend-code-generation 顺序第 1 步——SQL 与 Manifest 必须先于 `plan/apply` 存在。
- Contract/signature changes: 新增表结构与索引；无 Java 契约。
- Input/output and state mapping: `id/tenant_id/name/image/letter/seq` + 八继承列；`seq` 缺省 0、`image` 可空 NULL；`deleted_at NULL` 即有效行（部分唯一索引谓词）；时间列 `TIMESTAMP(6) WITH TIME ZONE`、`deleted_at` `WITHOUT TIME ZONE`（Rule 10，与 `EgonModel` 的 `Instant`/`LocalDateTime` 逐列同型）。
- Error and edge behavior: 未知角色 `RAISE EXCEPTION`（样例同形）；SHARD 目标 `NULL` 直通（brand 为 SINGLE，`PLAN-CLAR-008`）；空表建索引无长锁，不需要 `CONCURRENTLY`；不写回滚脚本（失败走下一修正脚本）。
- Standards impact: `MC-MODEL-001`, `MC-TIME-001`, `MC-SCOPE-001` — 列集合与 `EgonModel`/§11 逐列一致；不发明第二方言。
- Literal rule enforcement: `Rule 10` — 全部时间列 `TIMESTAMP(6)` 家族且时区语义与 `EgonModel` 逐列对应，无 `java.util` 时间概念；`Rule 11` — DDL 由受管 Runner 体系承载，业务唯一键组合 `tenant_id + name + deleted_at IS NULL`。
- Implementation pseudocode:

```sql
-- enjoyshop goods：brand 品牌字典（SINGLE，master_data 承载）；仅新增，不改 20260913_001
DO $egon$
BEGIN
    IF current_setting('egon_migration.role') = 'MASTER_DATA' THEN
        CREATE TABLE brand (
            id BIGINT PRIMARY KEY,
            tenant_id BIGINT NOT NULL,
            name VARCHAR(120) NOT NULL,
            image VARCHAR(500) NULL,
            letter CHAR(1) NOT NULL,
            seq INTEGER NOT NULL DEFAULT 0,
            create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
            version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
            CONSTRAINT ck_brand_seq CHECK (seq >= 0)
        );
        CREATE UNIQUE INDEX uk_brand_tenant_name_active ON brand (tenant_id, name) WHERE deleted_at IS NULL;
        CREATE INDEX idx_brand_tenant_seq_id ON brand (tenant_id, seq DESC, id DESC);
    ELSIF current_setting('egon_migration.role') = 'SHARD' THEN
        NULL;  -- brand 为 SINGLE 主数据表，分片目标不承载该表
    ELSE
        RAISE EXCEPTION 'Unknown managed DDL role';
    END IF;
END
$egon$;
```

- Verification contribution: `TEST-012` 静态半部（逐列比对 §11.2.1 与本文件）；执行时校验和写入 Manifest。
- After this file: brand Schema 就绪且不可再改（后续变更走新版本）。

#### File 2 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/resources/db/egon-mp/repository-manifest.json`

- Purpose: 追加本版本的 `{version, path, sha256}` 条目。
- Symbols: `scripts` 数组新增一个对象；`family` 保持 `web`。
- Repository evidence: 生成工程自带 Manifest 形状（`{"family":"web","scripts":[{...}]}`）。
- Dependencies and consumers: 生成器 `manifest` 输入模式会校验既有条目 sha256 前缀不变；未来 Runner 消费。
- Why now: Manifest 与脚本必须同提交落盘，生成器才能以 `manifest` 模式解析。
- Contract/signature changes: 仅追加；既有条目的 `version/path/sha256` 三键逐字节不变。
- Input/output and state mapping: `version=20260923_001`、`path=db/egon-mp/V20260923_001__initialize_goods_schema.sql`、`sha256=<File 1 字节摘要>`。
- Error and edge behavior: 摘要计算须在文件定稿后执行；若后续改动 SQL 必须同步重算（否则生成器 `CHECKSUM_DRIFT`）。
- Standards impact: `MC-SCOPE-001`, `MC-TEST-001` — 单一变更单一条目；校验和可复核。
- Literal rule enforcement: `Rule 11` — 分布式受管 DDL：一变更一版本一校验和，历史不可改。
- Implementation pseudocode:

```bash
cd enjoyshop-service/enjoyshop-service-goods/src/main/resources
shasum -a 256 db/egon-mp/V20260923_001__initialize_goods_schema.sql
# 将输出的 64 位十六进制写入 manifest：
# {"version":"20260923_001","path":"db/egon-mp/V20260923_001__initialize_goods_schema.sql","sha256":"<摘要>"}
git diff --stat -- db/egon-mp/repository-manifest.json   # 仅 1 行新增，0 行修改
```

- Verification contribution: `REQ-016` 验收（sha256 一致、旧条目未变）。
- After this file: 生成器可直接以 manifest 模式消费。

- Validation working directory: `/Users/mario/SelfProject/enjoyshop/enjoyshop-service/enjoyshop-service-goods`
- Verification command: `shasum -a 256 src/main/resources/db/egon-mp/V20260923_001__initialize_goods_schema.sql && python3 -c "import json,pathlib,hashlib;m=json.load(open('src/main/resources/db/egon-mp/repository-manifest.json'));e=[s for s in m['scripts'] if s['version']=='20260923_001'][0];assert e['sha256']==hashlib.sha256(pathlib.Path('src/main/resources/'+e['path']).read_bytes()).hexdigest();assert len(m['scripts'])==2"`
- Expected result: 校验和一致断言通过、脚本共 2 条、退出码 0；`git diff` 显示 Manifest 仅追加。
- Failure returns to: File 1（SQL 定稿后再改）或 File 2（摘要/路径笔误）。
- Completion criteria: 脚本与 Manifest 字节一致且旧历史未动；SQL 文本与 §11.2.1 逐列核对通过（含 `PLAN-CLAR-001` 的继承列类型）。
- Rollback: revert 本提交的两个文件（表未在任何库执行，无数据影响）。
- Commit paths: `enjoyshop-service/enjoyshop-service-goods/src/main/resources/db/egon-mp/V20260923_001__initialize_goods_schema.sql`, `enjoyshop-service/enjoyshop-service-goods/src/main/resources/db/egon-mp/repository-manifest.json`
- Commit: `feat(goods): 新增 brand 受管 DDL 脚本与 Manifest 条目`

### Step 3 — 落地公共错误边界（GoodsErrorStatus 与 GoodsErrorMapper）

- Requirements: `REQ-019`
- Dependencies: `Step 1`
- Baseline state: 生成工程 `-common` 模块只有 archetype 样例代码；`egon-cola-component-common-core` 已是 common 模块依赖（`ErrorStatus`/`CommonException` 可直接使用）。
- Observable outcome: 两个类型编译通过，枚举码值测试通过；FQN `top.egon.enjoyshop.goods.common.error.GoodsErrorMapper` 可被生成器配置引用。
- End state: 错误码区段 `6101xx`（`ASM-006`）与生成域实现所需的 `missing/zeroRows` 边界就绪。
- Test-first gate: `Not applicable` — 两类型是生成链路的编译前置契约（backend-code-generation 要求 apply 前"工程已提供全部被引用类型"），不是行为变更；随同提交的 `GoodsErrorStatusTest` 是码值稳定性断言（`TEST-005`/`TEST-016` 的断言基座），先写会因类型不存在而无法编译，无独立 RED 价值。
- Manual Checks: `MC-NAME-001`, `MC-REUSE-001`, `MC-JSON-001`, `MC-BEAN-001`, `MC-LOG-001`, `MC-SCOPE-001`, `MC-TEST-001`
- Literal Rules: `Rule 1`, `Rule 4`, `Rule 6`, `Rule 11`
- Ordered files:

#### File 1 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/enums/GoodsErrorStatus.java`

- Purpose: 品牌稳定业务码枚举（实现 common-core `ErrorStatus`）。
- Symbols: 枚举常量 `BRAND_REQUEST_INVALID(610101)`、`BRAND_NOT_FOUND(610102)`、`BRAND_NAME_DUPLICATED(610103)`、`BRAND_VERSION_CONFLICT(610104)`、`BRAND_OPERATOR_REQUIRED(610105)`、`BRAND_INTERNAL_ERROR(610199)`；方法 `getCode()`/`getMessage()`/`getStatus()=name()`。
- Repository evidence: common-core `ErrorStatus extends EgonEnum`（`int getCode()`、`String getMessage()`、`String getStatus()`）；Spec `ASM-006` 区段与英文 message 原文。
- Dependencies and consumers: `CommonException` 构造入参；`BaseExceptionHandler` 映射；`GoodsErrorMapper`；生成 `BrandDomainServiceImpl` 的异常路径。
- Why now: Step 4 的生成配置以 FQN 引用依赖该枚举的 `GoodsErrorMapper`；Step 8 advice 需要码常量。
- Contract/signature changes: 新类型；`code` 为 `int`、`status` 取 `name()`，无 ordinal 业务码。
- Input/output and state mapping: 纯常量；`message` 为英文稳定说明，不拼接用户输入。
- Error and edge behavior: 无状态、无线程安全 Issue（枚举）；新增码值只能追加不得改值（调用方按 `code` 分支）。
- Standards impact: `MC-NAME-001`, `MC-REUSE-001`, `MC-JSON-001` — 枚举后缀 `ErrorStatus` 复用平台契约；int 码 + `name()` 状态，无 `@JsonValue` 需求（不直接序列化枚举）。
- Literal rule enforcement: `Rule 1` — 语义后缀 `ErrorStatus`；`Rule 6` — 业务码为显式 int 字段，禁止 ordinal；`Rule 4` — 非 Spring Bean，无需 Bean 名/注入；`Rule 11` — 位于 `-common` 层（`DEC-106`）。
- Implementation pseudocode:

```java
package top.egon.enjoyshop.goods.common.enums;

@Getter
@AllArgsConstructor
public enum GoodsErrorStatus implements ErrorStatus {
    BRAND_REQUEST_INVALID(610101, "request parameters are invalid"),
    BRAND_NOT_FOUND(610102, "brand does not exist or is not visible to current tenant"),
    BRAND_NAME_DUPLICATED(610103, "brand name already exists in current tenant"),
    BRAND_VERSION_CONFLICT(610104, "brand was updated by another operation"),
    BRAND_OPERATOR_REQUIRED(610105, "identifiable operator is required for this operation"),
    BRAND_INTERNAL_ERROR(610199, "brand internal error");

    private final int code;
    private final String message;

    @Override
    public String getStatus() {
        return name();
    }
}
```

- Verification contribution: `GoodsErrorStatusTest` 断言六个码值与 `name()`（`ASM-006` 稳定性）。
- After this file: 错误码契约可被编译期引用。

#### File 2 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/error/GoodsErrorMapper.java`

- Purpose: 生成 `BrandDomainServiceImpl` 引用的错误边界（`missing`/`zeroRows` → 携带码的 `CommonException`）。
- Symbols: `static CommonException missing(Long id)`；`static CommonException zeroRows(String operation, Long id, Long version)`；私有构造器。
- Repository evidence: `domain-impl.ftl` 以 `throw [errorMapper].zeroRows(...)`/`[errorMapper].missing(...)` 静态调用；`CommonException(ErrorStatus)` 构造器已核对；映射语义即 Spec §7.3.4/§9.2 各映射表。
- Dependencies and consumers: 生成域实现（Step 4 产出）；间接服务 `BaseExceptionHandler`。
- Why now: 生成配置必须给出已存在的 FQN，否则 apply 后无法编译（backend-code-generation 前置确认项）。
- Contract/signature changes: 新类型；方法签名与生成模板调用形状逐参一致。
- Input/output and state mapping: `missing → 610102`；`zeroRows("create") → 610103`、`("update") → 610104`、`("delete") → 610102`、其余 → `610199`；不携带堆栈外泄风险（异常消息为枚举稳定文案）。
- Error and edge behavior: 未知 operation 走 `BRAND_INTERNAL_ERROR` 兜底；缺行判定（404）由生成实现的前置 `getById` 保证先于版本冲突（409）。
- Standards impact: `MC-NAME-001`, `MC-REUSE-001`, `MC-LOG-001` — 复用 `CommonException` 不新建异常类型；无日志点（纯映射）。
- Literal rule enforcement: `Rule 1` — 行为类后缀 `Mapper`（错误映射语义）；`Rule 11` — 位于 `-common`，被 `infrastructure` 经 domain 传递可见。
- Implementation pseudocode:

```java
package top.egon.enjoyshop.goods.common.error;

public final class GoodsErrorMapper {
    private GoodsErrorMapper() {
    }

    public static CommonException missing(Long id) {
        return new CommonException(GoodsErrorStatus.BRAND_NOT_FOUND);
    }

    public static CommonException zeroRows(String operation, Long id, Long version) {
        return switch (operation) {
            case "create" -> new CommonException(GoodsErrorStatus.BRAND_NAME_DUPLICATED);
            case "update" -> new CommonException(GoodsErrorStatus.BRAND_VERSION_CONFLICT);
            case "delete" -> new CommonException(GoodsErrorStatus.BRAND_NOT_FOUND);
            default -> new CommonException(GoodsErrorStatus.BRAND_INTERNAL_ERROR);
        };
    }
}
```

- Verification contribution: 使 Step 4 生成代码可编译；`TEST-005`/`TEST-006`/`TEST-010` 的码值来源。
- After this file: apply 前置类型齐备。

#### File 3 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/common/enums/GoodsErrorStatusTest.java`

- Purpose: 锁定 `ASM-006` 码值与 `status=name()` 契约。
- Symbols: 测试方法 `brand_error_codes_match_asm006`、`status_is_enum_name`。
- Repository evidence: common 模块测试依赖 `junit-jupiter`（POM 已核对）； archetype 测试命名风格。
- Dependencies and consumers: 仅测 File 1。
- Why now: 契约类型与其稳定性断言同提交，防后续漂移。
- Contract/signature changes: 无生产契约变化。
- Input/output and state mapping: 断言六个常量 code/message/status。
- Error and edge behavior: 断言失败即阻塞提交。
- Standards impact: `MC-TEST-001`, `MC-SCOPE-001` — 契约有静态断言。
- Literal rule enforcement: `Rule 11` — 测试位于所选形态的 common 模块测试树。
- Implementation pseudocode:

```java
@Test
void brand_error_codes_match_asm006() {
    assertThat(GoodsErrorStatus.BRAND_REQUEST_INVALID.getCode()).isEqualTo(610101);
    assertThat(GoodsErrorStatus.BRAND_NOT_FOUND.getCode()).isEqualTo(610102);
    assertThat(GoodsErrorStatus.BRAND_NAME_DUPLICATED.getCode()).isEqualTo(610103);
    assertThat(GoodsErrorStatus.BRAND_VERSION_CONFLICT.getCode()).isEqualTo(610104);
    assertThat(GoodsErrorStatus.BRAND_OPERATOR_REQUIRED.getCode()).isEqualTo(610105);
    assertThat(GoodsErrorStatus.BRAND_INTERNAL_ERROR.getCode()).isEqualTo(610199);
}

@Test
void status_is_enum_name() {
    assertThat(GoodsErrorStatus.BRAND_NAME_DUPLICATED.getStatus()).isEqualTo("BRAND_NAME_DUPLICATED");
}
```

- Verification contribution: `TEST-005`/`TEST-016` 的码值基座。
- After this file: common 层契约冻结。

- Validation working directory: `/Users/mario/SelfProject/enjoyshop/enjoyshop-service/enjoyshop-service-goods`
- Verification command: `./mvnw -q test`
- Expected result: `GoodsErrorStatusTest` 2 个用例通过，退出码 0。
- Failure returns to: File 1（码值与 `ASM-006` 不符）。
- Completion criteria: 两个类型 + 测试编译通过；FQN 与 Step 4 配置一致。
- Rollback: revert 本提交三文件（无外部消费者）。
- Commit paths: `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/enums/GoodsErrorStatus.java`, `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/error/GoodsErrorMapper.java`, `enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/common/enums/GoodsErrorStatusTest.java`
- Commit: `feat(goods): 新增品牌错误码枚举与生成链错误边界`

### Step 4 — 配置并执行代码生成器（plan → apply → journal）

- Requirements: `REQ-003`, `REQ-015`, `REQ-020`, `REQ-021`
- Dependencies: `Step 2`, `Step 3`
- Baseline state: DDL/Manifest 就绪（Step 2）、错误边界 FQN 就绪（Step 3）、`EGON_CODEGEN_CLASSPATH` 按 §6.2 构建并可解析 Java 21；生成工程内无 `brand` 相关文件。
- Observable outcome: `plan` JSON 列出 22 个 `ADD`、0 `CONFLICT`；`apply` 退出码 0；`./mvnw -q compile` 通过；journal 追加一条记录。
- End state: 22 个 GENERATED 文件由生成器产出（po/dao/mapper-xml/repo/domain-model/domain-query/domain-service/domain-impl/command×3/query×2/result/converter×5/manage/manage-impl/controller）；生成器状态目录 `.egon/` 出现且被忽略。
- Test-first gate: `Not applicable` — 生成器操作以机器摘要与冲突清单为客观证据（backend-code-generation 的 Plan 阶段要求），不适用行为 RED；生成结果的编译即验证。
- Manual Checks: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-NAME-001`, `MC-MODEL-001`, `MC-CONVERT-001`, `MC-BEAN-001`, `MC-LOG-001`, `MC-TIME-001`, `MC-SCOPE-001`, `MC-TEST-001`
- Literal Rules: `Rule 1`, `Rule 3`, `Rule 4`, `Rule 6`, `Rule 10`, `Rule 11`
- Ordered files:

#### File 1 — `CREATE enjoyshop-service/enjoyshop-service-goods/codegen/brand-codegen.json`

- Purpose: 生成器输入配置（backend-code-generation 允许模型书写的第三件东西）。
- Symbols: `configVersion=1`、`projectType=web`、`basePackage=top.egon.enjoyshop.goods`、`domain=goods`、15 个 artifacts、`logicalTables=["brand"]`、`fieldPolicies`、`apiContract`。
- Repository evidence: `codegen-web.json` 样例 + `CodegenConfigBO` 字段（未知键一律拒绝）+ `ProjectLayoutStrategy` 路径规则 + `policyFields` 跳过基列。
- Dependencies and consumers: `scripts/egon-codegen.sh plan/apply --config` 消费；`.egon/codegen/` 状态以配置指纹绑定。
- Why now: 配置指向 Step 2 的 Manifest 与 Step 3 的错误边界，两者已就绪。
- Contract/signature changes: 新文件；`existingErrorMapper` 必须逐字等于 Step 3 FQN。
- Input/output and state mapping: `filter=["name","letter"]` 使 `BrandPageQuery`/`BrandDomainQuery` 携带这两个字段（`seqMin/seqMax` 非 DDL 列，作为自定义语句独立参数，`PLAN-CLAR-004`）；`result=["name","image","letter","seq"]`；`events.enabled=false`（`REQ-020` 零事件）。
- Error and edge behavior: 未知键、缺 `logicalTables`、表名不匹配、既有脚本 sha256 漂移分别返回配置错误/`CHECKSUM_DRIFT`（退出码 2/3/8），任一即停止。
- Standards impact: `MC-DEP-001`, `MC-SCOPE-001` — 不扩展 artifact 范围（`persistence-crud` 与全链二选一，本配置取 Spec §8 的全链）。
- Literal rule enforcement: `Rule 11` — 生成范围与目录产物归属由配置显式声明，模型不手写目录产物。
- Implementation pseudocode:

```json
{
  "configVersion": 1,
  "projectType": "light",
  "basePackage": "top.egon.enjoyshop.goods",
  "domain": "goods",
  "outputRoot": "enjoyshop-service/enjoyshop-service-goods",
  "input": {
    "mode": "manifest",
    "resourceRoot": "enjoyshop-service/enjoyshop-service-goods/src/main/resources",
    "manifest": "db/egon-mp/repository-manifest.json"
  },
  "logicalTables": ["brand"],
  "artifacts": ["po", "dao", "mapper-xml", "repo", "domain-model", "domain-query", "command", "query",
    "result", "converter", "domain-service", "domain-impl", "manage", "manage-impl", "controller"],
  "existingTypeMappings": {},
  "fieldPolicies": {
    "create": ["name", "image", "letter", "seq"],
    "update": ["name", "image", "letter", "seq"],
    "result": ["name", "image", "letter", "seq"],
    "filter": ["name", "letter"],
    "sort": ["id"]
  },
  "apiContract": {
    "existingErrorMapper": "top.egon.enjoyshop.goods.common.error.GoodsErrorMapper",
    "basePath": "/api/v1/brands"
  },
  "events": { "enabled": false }
}
```

- Verification contribution: `plan` 摘要与文件清单的可复现输入（`PLAN-CLAR-012`）。
- After this file: 生成操作可执行且可重放。

#### File 2 — `GENERATED enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/po/BrandPO.java`

- Purpose: infrastructure 持久化五件套由生成器产出：`BrandPO`、`BrandDAO`、`BrandRepository`、`BrandPersistenceConverter`、`BrandDomainServiceImpl`。
- Symbols: `BrandPO extends EgonModel<BrandPO>`（仅 `name/image/letter/seq` 四业务列，`@SuperBuilder`+`@EqualsAndHashCode(callSuper=true)`+`@TableName("brand")`）；`BrandDAO extends EgonColaMapper<BrandPO>`（含 `selectByQuery`）；`BrandRepository extends EgonColaRepository<BrandDAO,BrandPO>`（`@Repository("brandRepository")`）；`BrandPersistenceConverter extends BaseConverter<BrandPO,Brand>`（`@Mapper(componentModel="spring", unmappedTargetPolicy=ERROR)`，Bean `brandPersistenceConverterImpl`）；`BrandDomainServiceImpl`（`@Service("brandDomainService")`，create/update/delete/detail/query，异常经 `GoodsErrorMapper`）。
- Repository evidence: `po.ftl`/`dao.ftl`/`repository.ftl`/`converter.ftl`/`domain-impl.ftl` 模板原文；`SchoolClassPO`/`SchoolClassRepository` 先例同构。
- Dependencies and consumers: 依赖 Step 3 错误边界与 MP-SDJ starter；被 Step 5/6 的自定义代码扩展。
- Why now: 目录产物只能由生成器写（SQL 变更后的刷新路径），且必须先于自定义扩展存在。
- Contract/signature changes: 新增五个类；`selectByQuery(query, limit, offset)` 由 `filter` 策略生成（等值谓词 + `ORDER BY id ASC`，本迭代不用于品牌列表——自定义查询在 Step 5）。
- Input/output and state mapping: 继承列不重复声明（`id/tenant_id/审计/deleted_at/version` 全部来自 `EgonModel`）；XML 映射继承列（下一文件）。
- Error and edge behavior: `apply` 遇 `CONFLICT`（磁盘文件与生成基线不一致）整体停止、不写任何文件（退出码 4）；磁盘与计划漂移返回退出码 6。
- Standards impact: `MC-NAME-001`, `MC-MODEL-001`, `MC-CONVERT-001`, `MC-BEAN-001`, `MC-LOG-001`, `MC-TIME-001` — 后缀/注解组合/Bean 名/时间类型全部由模板保证并人工复核 diff。
- Literal rule enforcement: `Rule 1`/`Rule 3` — 类名与 Lombok 组合按模板；`Rule 4` — Bean 名与 `@Qualifier` 注入由模板；`Rule 10` — 继承列时间类型不重写；`Rule 11` — Repository 为唯一持久化入口、域实现不触 DAO。
- Implementation pseudocode:

```bash
cd /Users/mario/SelfProject/enjoyshop
scripts_root=/Users/mario/SelfProject/Egon-COLA
$scripts_root/scripts/egon-codegen.sh templates                     # 探测：模板清单 + JSON 末行，缺类路径即 BLOCKED_TOOLING
$scripts_root/scripts/egon-codegen.sh plan --config enjoyshop-service/enjoyshop-service-goods/codegen/brand-codegen.json
# 读取末行 JSON：files 应为 22 条 ADD、conflicts 为空、destructive 为空
$scripts_root/scripts/egon-codegen.sh apply --plan <plan-id> --config enjoyshop-service/enjoyshop-service-goods/codegen/brand-codegen.json
cd enjoyshop-service/enjoyshop-service-goods && ./mvnw -q compile   # 生成物可编译（GoodsErrorMapper 引用成立）
```

- Verification contribution: `plan` JSON 22 ADD/0 CONFLICT + compile 通过；`TEST-012` 的 `extends EgonModel<` 静态断言由自带 verifier 承担。
- After this file: 五个 infrastructure 类就绪，未被人工修改。

#### File 3 — `GENERATED enjoyshop-service/enjoyshop-service-goods/src/main/resources/mybatis/mapper/goods/BrandDAO.xml`

- Purpose: 命名语句与 resultMap（含继承列映射）由生成器产出。
- Symbols: `BrandPOResultMap`（八继承列 + 四业务列）、`selectActiveById`、`selectActiveByIds`、`deleteVersionedById`（UTC 逻辑删除 + 乐观锁）、`selectByQuery`；`<sql id="columns">` 显式列清单（禁 `SELECT *`）。
- Repository evidence: `mapper.xml.ftl` 原文；`GradeDAO.xml` 先例同形。
- Dependencies and consumers: `BrandDAO` 接口的语句体；`mybatis/mapper/**/*.xml` 被生成 application.yml 的 `mapper-locations` 加载。
- Why now: XML 是目录产物，必须与 DAO 同批生成。
- Contract/signature changes: 新增语句集；`deleteVersionedById` 的 `WHERE ... AND version = #{MP_OPTLOCK_VERSION_ORIGINAL}` 保证软删除的版本条件。
- Input/output and state mapping: `deleted_at = (CURRENT_TIMESTAMP AT TIME ZONE 'UTC')` 为软删时间表达式（Rule 10，UTC 语义）。
- Error and edge behavior: 手工改动本文件将使未来 `plan` 报 `CONFLICT`（Step 5 的自定义扩展即接受该代价，§8.3 已声明）。
- Standards impact: `MC-TIME-001`, `MC-SCOPE-001` — 时间表达式与列清单由模板固化。
- Literal rule enforcement: `Rule 10` — UTC 逻辑删除表达式；`Rule 11` — 显式列、租户谓词交由拦截器、软删谓词在 SQL 内。
- Implementation pseudocode:

```bash
# 与 File 2 同一 plan/apply 产出；核对点：
grep -c "selectByQuery\|selectActiveById\|selectActiveByIds\|deleteVersionedById" \
  enjoyshop-service/enjoyshop-service-goods/src/main/resources/mybatis/mapper/goods/BrandDAO.xml
grep -n "CURRENT_TIMESTAMP AT TIME ZONE 'UTC'" 同上   # 软删时间表达式存在
```

- Verification contribution: Step 5 的 RED 测试将以本文件为基线断言新增语句。
- After this file: 生成语句齐备；自定义语句在 Step 5 追加。

#### File 4 — `GENERATED enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/Brand.java`

- Purpose: domain 模块三件套：`Brand`（域模型：`id`、`version` + 四业务字段）、`BrandDomainQuery`（`name`/`letter` + `PageQuery page`）、`BrandDomainService`（create/update/delete/detail/query 端口）。
- Symbols: 如上；`BrandDomainQuery.getPage()/setPage(PageQuery)` 由 `filter` 策略 + 模板固定 `page` 字段构成。
- Repository evidence: `domain-model.ftl`/`domain-query.ftl`/`domain-service.ftl`；`PageQuery.defaultPage()` 缺省值。
- Dependencies and consumers: 被 application/infrastructure 消费；Step 6 将增补端口方法。
- Why now: 域端口是 Step 6 编排的编译前提。
- Contract/signature changes: 新增三类型；域模型不含审计时间（`PLAN-CLAR-011`）。
- Input/output and state mapping: `version` 直通（乐观锁身份在域层可用）。
- Error and edge behavior: 无（纯载体/端口）。
- Standards impact: `MC-NAME-001`, `MC-MODEL-001` — class + Lombok 组合；命名 `Brand` 为领域模型（Spec §10.1 BO 角色）。
- Literal rule enforcement: `Rule 1`/`Rule 3` — 命名与 class 组合按模板；`Rule 11` — 域层不依赖 Spring Web/MyBatis 类型（模板仅引 common-core）。
- Implementation pseudocode:

```bash
DOMAIN_BASE=enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods
ls "$DOMAIN_BASE"
# 期望：Brand.java  BrandDomainQuery.java  BrandDomainService.java
grep -n "private Long version" "$DOMAIN_BASE/Brand.java"                # 乐观锁身份在域层可直通
grep -n "PageQuery page" "$DOMAIN_BASE/BrandDomainQuery.java"           # 分页归一载体（defaultPage() 缺省）
grep -n "PageSlice<Brand> query" "$DOMAIN_BASE/BrandDomainService.java" # 生成端口（本迭代列表改走 Step 6 新增方法）
```

- Verification contribution: Step 6 端口增补的落点。
- After this file: 域三件套就绪。

#### File 5 — `GENERATED enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/CreateBrandCommand.java`

- Purpose: application CQE 八件套：三个 Command（`CreateBrandCommand` 四业务字段带 `@Size/@NotNull`；`UpdateBrandCommand` 加 `id`+`expectedVersion`；`DeleteBrandCommand` 仅 `id`+`expectedVersion`）、`BrandDetailQuery`、`BrandPageQuery`（filter 字段 + `PageQuery page`）、`BrandResult`（`String id` + result 字段）、`BrandManage`/`BrandManageImpl`（`@Service("brandManage")`，`@Transactional` 写、`@Transactional(readOnly=true)` 读）。
- Symbols: 如上；`PageSlice` 仅出现在生成的 `page()` 链路（本迭代不使用，保持生成原样）。
- Repository evidence: `command.ftl`/`query.ftl`/`result.ftl`/`manage.ftl`/`manage-impl.ftl`；`GradeManageImpl` 先例注解组合。
- Dependencies and consumers: 被 Step 7 adapter 转换器与 Step 9 控制器消费；`BrandResult` 将在 Step 6 增补 `version`。
- Why now: 编排层是守卫与查重的宿主，必须先存在。
- Contract/signature changes: 新增八类型；无 Event 载体（`events.enabled=false`）。
- Input/output and state mapping: `expectedVersion` 即 HTTP `version` 的应用层名（生成 update 转换器 `@Mapping(target="version", source="expectedVersion")`）。
- Error and edge behavior: 生成的 `create/update/delete` 无守卫无查重（Step 6 定制）；`detail` 缺行抛 `GoodsErrorMapper.missing`（404）。
- Standards impact: `MC-NAME-001`, `MC-MODEL-001`, `MC-VALID-001`, `MC-BEAN-001`, `MC-LOG-001` — 命令级 `@Valid`（manage 接口）+ `@Service("brandManage")` + `@Slf4j` 由模板保证。
- Literal rule enforcement: `Rule 1`/`Rule 2` — CQE 后缀与层间 `@Valid`；`Rule 3` — class 组合载体；`Rule 4` — Bean 名/`@Qualifier`/`@Slf4j`；`Rule 11` — 编排不触持久化类型。
- Implementation pseudocode:

```bash
ls enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/
# 期望：BrandDetailQuery  BrandManage  BrandManageImpl  BrandPageQuery  BrandResult
#       CreateBrandCommand  DeleteBrandCommand  UpdateBrandCommand  converter/
```

- Verification contribution: Step 6 的 RED 测试宿主。
- After this file: 应用层八件套就绪（含 4 个转换器于下一文件）。

#### File 6 — `GENERATED enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/converter/BrandResultConverter.java`

- Purpose: application 转换四件套：`CreateBrandCommandConverter`、`UpdateBrandCommandConverter`（`expectedVersion→version`）、`DeleteBrandCommandConverter`、`BrandResultConverter`（域→Result，`id` 由 `Long.toString` 投影），均为 `@Mapper(componentModel="spring")` + `BaseForwardConverter` 单继承方向。
- Symbols: 如上；Bean 名为类型名首字母小写 + `Impl`（如 `brandResultConverterImpl`）。
- Repository evidence: `converter.ftl` 四分支原文；`unmappedTargetPolicy=ERROR` 保证字段漂移编译期暴露。
- Dependencies and consumers: 被生成 `BrandManageImpl` 与 Step 7 adapter 转换器（读 `BrandResult`）消费。
- Why now: Result 载体的 String id 投影是 §9 载荷 `id` 的上游事实（adapter 再转回 Long，Spec `RISK-005` 口径不变：JSON 输出为整数）。
- Contract/signature changes: 新增四接口。
- Input/output and state mapping: `BrandResult.id` 为 String（生成契约）；`version` 此时尚无字段（Step 6 增补）。
- Error and edge behavior: 修改任何生成转换器都将触发未来 `CONFLICT`；Step 6 的增补接受该代价。
- Standards impact: `MC-CONVERT-001`, `MC-NAME-001` — 全部经 `BaseForwardConverter`，无手写 setter/BeanUtils。
- Literal rule enforcement: `Rule 3` — MapStruct + `BaseForwardConverter` 契约；`Rule 1` — Converter 后缀。
- Implementation pseudocode:

```bash
CONV_BASE=enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/converter
ls "$CONV_BASE"
# 期望：BrandResultConverter  CreateBrandCommandConverter  DeleteBrandCommandConverter  UpdateBrandCommandConverter
grep -n "expectedVersion" "$CONV_BASE/UpdateBrandCommandConverter.java"   # @Mapping(target="version", source="expectedVersion")
grep -n "BaseForwardConverter" "$CONV_BASE/BrandResultConverter.java"     # 不可逆投影基类
grep -n "Long.toString" "$CONV_BASE/BrandResultConverter.java"            # id 的 String 投影（Step 6 增补 version 映射的落点）
```

- Verification contribution: 转换链编译与 Step 6 的 version 增补落点。
- After this file: 22 个 GENERATED 文件全部就绪。

#### File 7 — `GENERATED enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/goods/BrandController.java`

- Purpose: 生成的基线控制器（create/detail/page/update/delete 五路由，裸 `BrandResult`/`PageSlice` 返回、无信封与 OpenAPI 注解）。
- Symbols: `@RestController("brandController")`、`@RequestMapping("/api/v1/brands")`、`parseId` 静态方法。
- Repository evidence: `controller.ftl` 原文（web profile 含 controller）。
- Dependencies and consumers: `brandManage` Bean；Step 9 将整体定制为 §9 契约（Spec §8.2 "GEN 加 CREATE 方法体"）。
- Why now: 定制必须以生成基线为起点（可审计 diff）。
- Contract/signature changes: 本 Step 不改其内容；Step 9 的修改是声明的定制（非生成器回写）。
- Input/output and state mapping: 与 §9 不符的部分（信封、201、注解）全部由 Step 9 替换。
- Error and edge behavior: 生成版 `parseId` 抛 `IllegalArgumentException`（Step 8 的 advice 将其映射 400，定制后由 `MethodArgumentTypeMismatch` 路径接管）。
- Standards impact: `MC-BEAN-001`, `MC-NAME-001` — Bean 名 `brandController` 与 Spec §9.2.1 一致。
- Literal rule enforcement: `Rule 4` — Bean 名与构造注入由模板；`Rule 11` — 控制器位于 adapter，不触 DAO。
- Implementation pseudocode:

```bash
CTRL=enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/goods/BrandController.java
grep -n "class BrandController\|@RestController\|@RequestMapping" "$CTRL"
grep -n "PageSlice" "$CTRL"   # 生成版以 PageSlice 返回且无信封——Step 9 将替换为 ResultRecord/PageResultRecord 契约
grep -n "parseId" "$CTRL"     # 生成版 String 路径参数解析——Step 9 改为 Long 路径变量 + @Positive
```

- Verification contribution: Step 9 RED 测试的"生成版外形不符"断言对象。
- After this file: 生成基线完整；全工程 `./mvnw -q compile` 通过。

#### File 8 — `CREATE docs/egon/codegen/ddl-consumption-log.md`

- Purpose: 追加 classpath SQL 消费日志（backend-code-generation 对 apply 成功后的强制要求）。
- Symbols: 一条记录：Asia/Shanghai 时间、profile=`web`、输出根、`throughVersion: 20260923_001`、脚本版本/路径/sha256/全文。
- Repository evidence: backend-code-generation "Classpath SQL journal" 节（追加-only；缺失则从头部创建）。
- Dependencies and consumers: 审计链；后续 SQL 变更继续追加。
- Why now: apply 成功是追加的唯一触发点。
- Contract/signature changes: 新文件（本仓库首条）。
- Input/output and state mapping: 不代表 DDL 已在任何数据库执行；不连接数据库填写。
- Error and edge behavior: 若 apply 失败则本文件不创建。
- Standards impact: `MC-SCOPE-001`, `MC-TEST-001` — 生成动作可审计。
- Literal rule enforcement: `Rule 11` — 受管 DDL 治理链的记录义务。
- Implementation pseudocode:

```markdown
## 2026-09-24 12:xx Asia/Shanghai — profile web → enjoyshop-service/enjoyshop-service-goods
- throughVersion: 20260923_001
- script V20260923_001 / db/egon-mp/V20260923_001__initialize_goods_schema.sql / sha256=<Step 2 摘要>
- <V20260923_001 的完整 SQL 文本粘贴于此>
```

- Verification contribution: journal 条目与 Manifest sha256 一致。
- After this file: 本轮生成闭环。

#### File 9 — `MODIFY .gitignore`

- Purpose: 忽略生成器状态目录与系统噪声。
- Symbols: 追加 `.egon/`、`.DS_Store`。
- Repository evidence: 现有 `.gitignore` 仅 `.idea/`、`.agents/`；`.egon/` 是生成器所有权记录（README）。
- Dependencies and consumers: git。
- Why now: apply 首次产生 `.egon/`，必须与生成同提交忽略。
- Contract/signature changes: 追加两行。
- Input/output and state mapping: 无。
- Error and edge behavior: 不忽略 `docs/`（Spec/Plan/journal 必须入库）。
- Standards impact: `MC-SCOPE-001` — 仓库卫生属变更范围。
- Literal rule enforcement: `Rule 11` — 生成器状态不入库但保留磁盘（删除将破坏所有权记录）。
- Implementation pseudocode:

```text
# 既有内容逐字保留：
.idea/
.agents/
# 追加以下两行：
# .egon/ = 代码生成器所有权状态（plans/state/journal/lock），保留在磁盘、永不提交
.egon/
# 系统噪声
.DS_Store
```

- Verification contribution: `git status` 不再显示 `.egon/`。
- After this file: Step 4 提交干净。

- Validation working directory: `/Users/mario/SelfProject/enjoyshop`
- Verification command: `python3 /Users/mario/SelfProject/Egon-COLA/scripts/egon-codegen.sh check --config enjoyshop-service/enjoyshop-service-goods/codegen/brand-codegen.json; cd enjoyshop-service/enjoyshop-service-goods && ./mvnw -q compile`
- Expected result: `check` 退出码 0（磁盘与生成基线一致）；compile 退出码 0；`git status` 干净（除 `.egon/` 已忽略）。
- Failure returns to: File 1（配置键错误/`CHECKSUM_DRIFT`）→ Step 2/3（输入未就绪）；`CONFLICT` → 检查是否手写了目录产物并停止。
- Completion criteria: 22 文件与 §5 树逐一对应；journal 已追加；零手工目录产物。
- Rollback: revert 配置/journal/`.gitignore` + 删除 `.egon/` 与 22 个生成文件（生成可重放，无外部影响）。
- Commit paths: `enjoyshop-service/enjoyshop-service-goods/codegen/brand-codegen.json`, `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/po/BrandPO.java`, `enjoyshop-service/enjoyshop-service-goods/src/main/resources/mybatis/mapper/goods/BrandDAO.xml`, `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/Brand.java`, `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/CreateBrandCommand.java`, `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/converter/BrandResultConverter.java`, `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/goods/BrandController.java`, `docs/egon/codegen/ddl-consumption-log.md`, `.gitignore`, `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/`, `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/`, `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/`, `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/goods/`
- Commit: `feat(goods): 生成 brand 后端目录产物并记录 SQL 消费日志`

### Step 5 — 扩展自定义持久化查询（DAO 方法、XML 语句、Repository 门面）

- Requirements: `REQ-010`, `REQ-011`, `REQ-012`, `REQ-017`, `REQ-018`
- Dependencies: `Step 4`
- Baseline state: 生成的 `BrandDAO` 仅有 `selectByQuery`（等值过滤 + `ORDER BY id ASC`，无 count、无 LIKE、无区间、无业务排序），无法表达 §9.2.1 参数表与 `REQ-014` 排序键。
- Observable outcome: `BrandQuerySqlShapeTest` 从 RED 转 GREEN：XML 含 `listBrandPage`/`countBrandPage`/`countActiveByName` 语句且形态断言通过；`likePrefix` 对 `%`/`_`/`\` 的转义断言通过。
- End state: Repository 暴露 `selectActiveById`/`countActiveByName`/`selectBrandPage`/`countBrandPage` 四个带参数级约束的门面方法；生成的 `selectByQuery` 链保持原样（未被调用，目录契约保留）。
- Test-first gate: `Required` — RED 原因：三个命名语句与 `likePrefix` 尚不存在，XML 文本断言与转义断言失败（缺行为而非坏夹具）。
- Manual Checks: `MC-VALID-001`, `MC-NAME-001`, `MC-ARCH-001`, `MC-SCOPE-001`, `MC-TEST-001`
- Literal Rules: `Rule 1`, `Rule 2`, `Rule 11`
- Ordered files:

#### File 1 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/goods/repo/BrandQuerySqlShapeTest.java`

- Purpose: 以纯文本/纯函数断言锁定 §11.2.1 访问模式与 `TEST-008` 的转义契约（无数据库）。
- Symbols: `listBrandPage_orders_by_seq_desc_id_desc`、`listBrandPage_filters_are_parameterized_and_active_only`、`count_active_by_name_excludes_self`、`like_prefix_escapes_wildcards`。
- Repository evidence: infrastructure 模块测试依赖 `spring-boot-starter-test`（POM 已核对）；`TEST-008` "MyBatis 语句解析（无库）" 的静态形态路线。
- Dependencies and consumers: 读 `BrandDAO.xml` 与 `BrandRepository` 源码资源；不启动 Spring。
- Why now: 这是本 Step 的 RED 行为闸门（语句缺失 → 断言失败）。
- Contract/signature changes: 仅测试。
- Input/output and state mapping: 以 UTF-8 读 XML 文本 `contains` 断言；`likePrefix("50% off") → "50\% off%"`、`likePrefix(null) → null`。
- Error and edge behavior: 断言覆盖 `deleted_at IS NULL`、`ESCAPE '\'`、`ORDER BY seq DESC, id DESC`、`LIMIT #{limit} OFFSET #{offset}`、`AND id != #{excludeId}`。
- Standards impact: `MC-TEST-001`, `MC-SCOPE-001` — SQL 形态可静态复核，不连库。
- Literal rule enforcement: `Rule 2` — 负例与转义契约先于实现；`Rule 11` — 测试位于所选形态 infrastructure 测试树。
- Implementation pseudocode:

```java
@Test
void listBrandPage_orders_by_seq_desc_id_desc() throws Exception {
    String xml = Files.readString(Path.of(
        "src/main/resources/mybatis/mapper/goods/BrandDAO.xml"));
    assertThat(xml).contains("ORDER BY seq DESC, id DESC");
    assertThat(xml).contains("LIMIT #{limit} OFFSET #{offset}");
    assertThat(xml).contains("ESCAPE '\\'");
    assertThat(xml).contains("deleted_at IS NULL");
}

@Test
void like_prefix_escapes_wildcards() {
    assertThat(BrandRepository.likePrefix("50% off")).isEqualTo("50\\% off%");
    assertThat(BrandRepository.likePrefix("a_b\\c")).isEqualTo("a\\_b\\\\c%");
    assertThat(BrandRepository.likePrefix("   ")).isNull();
}
```

- Verification contribution: `TEST-008` 的执行载体。
- After this file: RED 成立（语句与方法不存在 → 编译期缺 `likePrefix`、运行期缺语句断言失败，两种失败均为声明的缺行为）。

#### File 2 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/dao/BrandDAO.java`

- Purpose: 增补四个命名查询方法。
- Symbols: `selectActiveById(@Param("id") Long id)`、`countActiveByName(@Param("name") String name, @Param("excludeId") Long excludeId)`、`listBrandPage(name, letter, seqMin, seqMax, limit, offset)`、`countBrandPage(name, letter, seqMin, seqMax)`（全部 `List<BrandPO>/long` 返回、`@Param` 显式）。
- Repository evidence: 生成的 `BrandDAO extends EgonColaMapper<BrandPO>` + `GradeDAO` 命名方法先例。
- Dependencies and consumers: XML 语句体（File 3）；`BrandRepository`（File 4）。
- Why now: 接口先于 XML 语句被 Repository 引用是编译顺序；RED 测试已锁定契约。
- Contract/signature changes: 追加方法，不改生成方法。
- Input/output and state mapping: `excludeId=null` 表示不排除（create 查重）；`seqMin/seqMax` 可空闭区间；`letter` 等值（adapter 已大写归一）。
- Error and edge behavior: 全部谓词在 XML 内判空拼接；租户谓词由 MP 拦截器注入（不在此手写）。
- Standards impact: `MC-NAME-001`, `MC-ARCH-001` — DAO 后缀（访问组件）与 adapter→application→domain→infrastructure 方向不变。
- Literal rule enforcement: `Rule 1` — DAO 命名；`Rule 11` — Mapper 语句显式、禁 `SELECT *`（继承 `columns` 片段）。
- Implementation pseudocode:

```java
List<BrandPO> listBrandPage(@Param("name") String namePrefixEscaped, @Param("letter") String letter,
        @Param("seqMin") Integer seqMin, @Param("seqMax") Integer seqMax,
        @Param("limit") int limit, @Param("offset") long offset);

long countBrandPage(@Param("name") String namePrefixEscaped, @Param("letter") String letter,
        @Param("seqMin") Integer seqMin, @Param("seqMax") Integer seqMax);

long countActiveByName(@Param("name") String name, @Param("excludeId") Long excludeId);

BrandPO selectActiveById(@Param("id") Long id);
```

- Verification contribution: `BrandQuerySqlShapeTest` 的语句 id 与参数名可解析。
- After this file: 接口与 XML 断言对齐（XML 未改前仍 RED）。

#### File 3 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/resources/mybatis/mapper/goods/BrandDAO.xml`

- Purpose: 追加三个自定义语句（`listBrandPage`/`countBrandPage`/`countActiveByName`）；`selectActiveById` 语句已由生成器提供。
- Symbols: 三个 `<select>`；谓词顺序 `deleted_at IS NULL` → `name LIKE … ESCAPE '\'` → `letter =` → `seq >= / <=`；排序 `ORDER BY seq DESC, id DESC`。
- Repository evidence: 生成 XML 的 `resultMap`/`columns` 片段复用；Spec §11.2.1 访问模式表逐列对应。
- Dependencies and consumers: `BrandDAO` 接口（File 2）；`idx_brand_tenant_seq_id`/`uk_brand_tenant_name_active` 索引（Step 2 DDL）。
- Why now: RED 断言的 GREEN 实现点。
- Contract/signature changes: 追加语句，不改生成语句。
- Input/output and state mapping: `name` 入参已是"转义后前缀"（含 `%` 后缀），`<if test="name != null and name != ''">` 拼接；`seqMin/seqMax` 用 `&gt;=`/`&lt;=`；count 语句无排序无 LIMIT。
- Error and edge behavior: 全部条件可空；LIKE 通配符字面量语义由 ESCAPE + Repository 端转义共同保证（`TEST-008`）。
- Standards impact: `MC-VALID-001`, `MC-TEST-001` — 谓词与转义契约由测试锁定。
- Literal rule enforcement: `Rule 2` — 转义与区间行为即本文件；`Rule 11` — 租户条件交由 MP-SDJ 拦截器、有效行谓词显式（Spec §11.2.1 "不手写于业务层拼接"）。
- Implementation pseudocode:

```xml
<select id="listBrandPage" resultMap="BrandPOResultMap">
    SELECT <include refid="columns"/> FROM brand
    WHERE deleted_at IS NULL
    <if test="name != null and name != ''">AND name LIKE #{name} ESCAPE '\'</if>
    <if test="letter != null and letter != ''">AND letter = #{letter}</if>
    <if test="seqMin != null">AND seq &gt;= #{seqMin}</if>
    <if test="seqMax != null">AND seq &lt;= #{seqMax}</if>
    ORDER BY seq DESC, id DESC
    LIMIT #{limit} OFFSET #{offset}
</select>

<select id="countBrandPage" resultType="long">
    SELECT COUNT(*) FROM brand
    WHERE deleted_at IS NULL
    <if test="name != null and name != ''">AND name LIKE #{name} ESCAPE '\'</if>
    <if test="letter != null and letter != ''">AND letter = #{letter}</if>
    <if test="seqMin != null">AND seq &gt;= #{seqMin}</if>
    <if test="seqMax != null">AND seq &lt;= #{seqMax}</if>
</select>

<select id="countActiveByName" resultType="long">
    SELECT COUNT(*) FROM brand
    WHERE deleted_at IS NULL AND name = #{name}
    <if test="excludeId != null">AND id != #{excludeId}</if>
</select>
```

- Verification contribution: `BrandQuerySqlShapeTest` 全部断言转 GREEN。
- After this file: SQL 形态与 §11.2.1 一致且被测试锁定。

#### File 4 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/repo/BrandRepository.java`

- Purpose: Repository 门面方法 + LIKE 前缀归一（参数级校验）。
- Symbols: `selectActiveById(@NotNull @Positive Long id)`、`countActiveByName(@NotBlank @Size(max=120) String name, @Positive Long excludeId)`、`selectBrandPage(@Valid BrandDomainQuery query, @Min(0) Integer seqMin, @Min(0) Integer seqMax)`、`countBrandPage(同参)`、`static String likePrefix(String raw)`。
- Repository evidence: 生成 Repository 已有 `@Validated`/`@Slf4j`/`@Repository("brandRepository")` + `getBaseMapper()` 模式；参数级约束先例 `SchoolClassRepository`。
- Dependencies and consumers: `BrandDAO`（File 2/3）；Step 6 的域端口与编排。
- Why now: 应用层只经域端口触达持久化，门面必须先于端口实现存在。
- Contract/signature changes: 追加方法与静态归一函数；不改生成方法。
- Input/output and state mapping: `likePrefix`：trim 后空 → null；`raw.replace("\\","\\\\").replace("%","\\%").replace("_","\\_")` 后追加 `%`；`selectBrandPage` 以 `query.getPage().pageSize()/offset()` 计算 LIMIT/OFFSET。
- Error and edge behavior: 参数约束违规抛 `ConstraintViolationException`（`@Validated` 代理），由 Step 8 advice 映射 400 610101；`selectActiveById` 0 行返回 null（上游 404）。
- Standards impact: `MC-VALID-001`, `MC-NAME-001`, `MC-UTIL-001` — 参数级校验 + 仅 JDK 字符串处理（Rule 5）。
- Literal rule enforcement: `Rule 2` — Repository 交接的参数级校验与归一；`Rule 1` — 方法语义命名；`Rule 11` — 唯一持久化入口不变。
- Implementation pseudocode:

```java
public BrandPO selectActiveById(@NotNull @Positive Long id) {
    return getBaseMapper().selectActiveById(id);
}

public long countActiveByName(@NotBlank @Size(max = 120) String name, @Positive Long excludeId) {
    return getBaseMapper().countActiveByName(name, excludeId);
}

public List<BrandPO> selectBrandPage(@Valid BrandDomainQuery query,
        @Min(0) Integer seqMin, @Min(0) Integer seqMax) {
    return getBaseMapper().listBrandPage(likePrefix(query.getName()), query.getLetter(),
            seqMin, seqMax, query.getPage().pageSize(), query.getPage().offset());
}

public long countBrandPage(@Valid BrandDomainQuery query,
        @Min(0) Integer seqMin, @Min(0) Integer seqMax) {
    return getBaseMapper().countBrandPage(likePrefix(query.getName()), query.getLetter(), seqMin, seqMax);
}

static String likePrefix(String raw) {
    if (raw == null || raw.isBlank()) {
        return null;
    }
    return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
}
```

- Verification contribution: `like_prefix_escapes_wildcards` GREEN；Step 6 端口实现的协作者就绪。
- After this file: 持久化层查询能力完整；模块测试通过。

- Validation working directory: `/Users/mario/SelfProject/enjoyshop/enjoyshop-service/enjoyshop-service-goods`
- Verification command: `./mvnw -q -Dtest=BrandQuerySqlShapeTest test`
- Expected result: 4 个用例通过，退出码 0。
- Failure returns to: File 1（断言与 §11 形态不符）→ File 3（SQL 笔误）→ File 4（归一函数错误）。
- Completion criteria: RED→GREEN 闭环；生成的 `selectByQuery` 链未被改动（`git diff` 仅追加）。
- Rollback: revert 四文件（生成文件回到基线哈希，`check` 恢复一致）。
- Commit paths: `enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/goods/repo/BrandQuerySqlShapeTest.java`, `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/dao/BrandDAO.java`, `enjoyshop-service/enjoyshop-service-goods/src/main/resources/mybatis/mapper/goods/BrandDAO.xml`, `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/repo/BrandRepository.java`
- Commit: `feat(goods): 扩展品牌自定义查询语句与仓储门面`

### Step 6 — 定制应用层编排（操作者守卫、名称前置查重、含 total 的 listBrands 与 version 贯通）

- Requirements: `REQ-007`, `REQ-008`, `REQ-009`, `REQ-011`, `REQ-014`, `REQ-018`
- Dependencies: `Step 5`
- Baseline state: 生成的 `BrandManageImpl` 写方法无操作者守卫、无名称查重（仅索引兜底在 advice 层尚不存在）；`BrandResult` 不携带 `version`，导致 `API-004`/`API-005` 的乐观锁回传契约（Spec §9.2.4/§9.2.5）不可实现；列表链路无 total。
- Observable outcome: `BrandManageImplTest` RED→GREEN：空白 MDC `userId` 下三个写方法抛 `CommonException(610105)` 且对域服务零交互；查重命中抛 610103 且不触达 create/update；`listBrands` 返回 records+total+页元数据；`BrandResult` 含 `version` 且 `BrandResultConverter` 完成映射。
- End state: 写路径守卫+查重+生成流程串联；`BrandManage#listBrands(BrandPageQuery, seqMin, seqMax)` 暴露给 adapter；`BrandDetailQuery`/`BrandPageQuery`/`DeleteBrandCommand` 可被控制器直接构造。
- Test-first gate: `Required` — RED 原因：守卫与查重行为不存在（create 在空白操作者下仍会调域服务）、`listBrands` 未实现（接口新增后实现类编译失败），二者均为缺行为而非坏夹具。
- Manual Checks: `MC-VALID-001`, `MC-BEAN-001`, `MC-LOG-001`, `MC-NAME-001`, `MC-REUSE-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`
- Literal Rules: `Rule 1`, `Rule 2`, `Rule 4`, `Rule 9`, `Rule 11`
- Ordered files:

#### File 1 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/BrandDomainService.java`

- Purpose: 域端口增补三个方法（存在性查重、分页行集、分页计数）。
- Symbols: `boolean existsActiveByName(@NotBlank @Size(max=120) String name, @Positive Long excludeId)`；`List<Brand> listPage(@Valid BrandDomainQuery query, @Min(0) Integer seqMin, @Min(0) Integer seqMax)`；`long countPage(同参)`。
- Repository evidence: 生成的 `@Validated` 端口接口 + 参数级约束先例（`delete(Long id, Long expectedVersion)` 的 `@NotNull @Positive`）。
- Dependencies and consumers: `BrandDomainServiceImpl`（File 6）；`BrandManageImpl`（File 7）。
- Why now: 接口是编排与实现的编译前提（`PLAN-CLAR-009` 的端口方向）。
- Contract/signature changes: 追加方法，不改生成方法。
- Input/output and state mapping: `excludeId=null`（create）与自身 id（update）；`query.getPage()` 携带归一后的分页。
- Error and edge behavior: 约束违规抛 `ConstraintViolationException`（端口 `@Validated`）。
- Standards impact: `MC-VALID-001`, `MC-NAME-001` — 端口语义命名 + 校验注解。
- Literal rule enforcement: `Rule 1`/`Rule 2` — 端口命名与交接校验；`Rule 11` — 域端口只暴露业务语义，不暴露 PO 泛型。
- Implementation pseudocode:

```java
boolean existsActiveByName(@NotBlank @Size(max = 120) String name, @Positive Long excludeId);

List<Brand> listPage(@Valid BrandDomainQuery query, @Min(0) Integer seqMin, @Min(0) Integer seqMax);

long countPage(@Valid BrandDomainQuery query, @Min(0) Integer seqMin, @Min(0) Integer seqMax);
```

- Verification contribution: 编译前提；`BrandManageImplTest` 的 mock 契约。
- After this file: 端口契约扩展（实现类暂缺 → 模块编译失败，与 File 6 成对修复）。

#### File 2 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/BrandManage.java`

- Purpose: 编排接口增补 `listBrands`。
- Symbols: `PageResultRecord<BrandResult> listBrands(@Valid BrandPageQuery query, @Min(0) Integer seqMin, @Min(0) Integer seqMax)`。
- Repository evidence: 生成的 `BrandManage`（`@Validated` + `@Valid` 参数）与 `PageResultRecord` 工厂。
- Dependencies and consumers: `BrandManageImpl`（File 7）；Step 9 控制器。
- Why now: 测试（File 3）引用该方法，需先有编译前提（模板规则允许契约先行并声明）。
- Contract/signature changes: 追加方法；`PageResultRecord` 作为载体（`PLAN-CLAR-010`）。
- Input/output and state mapping: 见 File 7。
- Error and edge behavior: 约束违规 400（advice）。
- Standards impact: `MC-NAME-001`, `MC-REUSE-001` — 复用 common-core 信封类为载体。
- Literal rule enforcement: `Rule 1`/`Rule 2` — 命名与 `@Valid` 交接。
- Implementation pseudocode:

```java
/** 新增：分页列表编排载体（records + total + 页元数据，PLAN-CLAR-010）。 */
PageResultRecord<BrandResult> listBrands(@Valid BrandPageQuery query,
        @Min(0) Integer seqMin, @Min(0) Integer seqMax);
```

- Verification contribution: File 3 的编译前提。
- After this file: 接口契约齐备。

#### File 3 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/application/goods/BrandManageImplTest.java`

- Purpose: 守卫、查重与 listBrands 行为的 RED 闸门（Mockito 单测）。
- Symbols: `write_without_operator_is_rejected_with_610105_and_zero_downstream`、`create_with_duplicate_name_is_rejected_with_610103_before_write`、`update_duplicate_check_excludes_self_row`、`list_brands_returns_total_and_page_meta`。
- Repository evidence: application 模块测试依赖 `junit-jupiter`+`mockito-junit-jupiter`（POM 已核对）；`GradeDomainServiceTest` 先例风格。
- Dependencies and consumers: mock `BrandDomainService` 与四个生成转换器（构造注入）。
- Why now: 生成实现无守卫/查重/listBrands → 断言失败或编译失败，均为声明的缺行为。
- Contract/signature changes: 无生产契约变化。
- Input/output and state mapping: `MDC.clear()` 模拟无操作者；`MDC.put("userId","10001")` 模拟有操作者（finally 清理）；`ArgumentCaptor` 断言查重入参 `excludeId=self`。
- Error and edge behavior: 断言 `verifyNoInteractions(brandDomainService)`（守卫先于持久层，`TEST-016` 应用层半部）。
- Standards impact: `MC-TEST-001`, `MC-PATTERN-001` — 直写逻辑的行为锁定。
- Literal rule enforcement: `Rule 2` — 交接负例；`Rule 11` — 测试位于 application 模块。
- Implementation pseudocode:

```java
@Test
void write_without_operator_is_rejected_with_610105_and_zero_downstream() {
    MDC.clear();
    assertThatThrownBy(() -> manage.create(command()))
        .isInstanceOfSatisfying(CommonException.class, ex ->
            assertThat(((GoodsErrorStatus) ex.getErrorStatus()).getCode()).isEqualTo(610105));
    assertThatThrownBy(() -> manage.update(updateCommand())).isInstanceOf(CommonException.class);
    assertThatThrownBy(() -> manage.delete(deleteCommand())).isInstanceOf(CommonException.class);
    verifyNoInteractions(brandDomainService);
}

@Test
void create_with_duplicate_name_is_rejected_with_610103_before_write() {
    MDC.put("userId", "10001");
    when(brandDomainService.existsActiveByName("Apple", null)).thenReturn(true);
    assertThatThrownBy(() -> manage.create(command()))
        .isInstanceOfSatisfying(CommonException.class, ex ->
            assertThat(((GoodsErrorStatus) ex.getErrorStatus()).getCode()).isEqualTo(610103));
    verify(brandDomainService, never()).create(any());
    MDC.clear();
}

@Test
void list_brands_returns_total_and_page_meta() {
    MDC.put("userId", "10001");
    when(brandDomainService.listPage(any(), eq(10), eq(20))).thenReturn(List.of(brand()));
    when(brandDomainService.countPage(any(), eq(10), eq(20))).thenReturn(37L);
    PageResultRecord<BrandResult> page = manage.listBrands(pageQuery("A", 1, 10), 10, 20);
    assertThat(page.page().total()).isEqualTo(37L);
    assertThat(page.records()).hasSize(1);
    MDC.clear();
}
```

- Verification contribution: `TEST-011`/`TEST-016` 的应用层半部 + `TEST-007`/`TEST-009` 的编排半部。
- After this file: RED 成立（实现缺失）。

#### File 4 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/BrandResult.java`

- Purpose: 为 Result 载体增补 `version`（乐观锁回传契约的功能必需，`PLAN-CLAR-011`）。
- Symbols: `private Long version;`（置于 `id` 与业务字段之后）。
- Repository evidence: 生成 `result.ftl` 的 Lombok 组合；域模型 `Brand.version` 存在可直通；`policyFields` 跳过基列故必须定制。
- Dependencies and consumers: `BrandResultConverter`（File 5）；Step 7 `BrandVO`；Step 9 全部写响应。
- Why now: 转换器 `unmappedTargetPolicy=ERROR` 要求字段与映射同批落地。
- Contract/signature changes: 生成文件增补一个字段（接受未来再生成对该文件的 `CONFLICT`，Spec §8.3 已声明该运维模型）。
- Input/output and state mapping: create 时由生成域实现回填 PO 后映射（`version=0`）；update 时为条件更新前值 +1 的库端行为在单测中由 captor 断言。
- Error and edge behavior: null 时 JSON 按 `non_null` 不输出（生成 application.yml 全局包含策略）。
- Standards impact: `MC-MODEL-001`, `MC-NAME-001` — 载体字段语义命名。
- Literal rule enforcement: `Rule 3` — 保持 Lombok 组合不引入 record；`Rule 11` — 定制发生在生成文件的声明边界内（Spec §8.2"GEN 加自定义"）。
- Implementation pseudocode:

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
public class BrandResult {
    private String id;
    private Long version;      // 本 Step 增补：乐观锁回传
    private String name;
    private String image;
    private String letter;
    private Integer seq;
}
```

- Verification contribution: `BrandVO.version` 映射来源；`TEST-004` 断言对象。
- After this file: Result 契约齐备（转换器未映射前编译失败，与 File 5 成对）。

#### File 5 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/converter/BrandResultConverter.java`

- Purpose: 补 `version` 映射。
- Symbols: `@Mapping(target = "version", source = "version")` 加入 `toTarget(Brand source)`。
- Repository evidence: 生成模板已有 `id` 的 expression 映射；`unmappedTargetPolicy=ERROR`。
- Dependencies and consumers: `BrandManageImpl` 全部返回路径。
- Why now: File 4 的编译配套。
- Contract/signature changes: 一条注解。
- Input/output and state mapping: `Brand.version → BrandResult.version` 直通。
- Error and edge behavior: 无。
- Standards impact: `MC-CONVERT-001` — 仍为 MapStruct 映射，无手写拷贝。
- Literal rule enforcement: `Rule 3` — BaseForwardConverter 体系内扩展。
- Implementation pseudocode:

```java
@Override
@Mapping(target = "id", expression = "java(source.getId() == null ? null : Long.toString(source.getId()))")
@Mapping(target = "version", source = "version")
BrandResult toTarget(Brand source);
```

- Verification contribution: 映射编译 + `TEST-004` 的 version 通路。
- After this file: version 全链贯通。

#### File 6 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/service/impl/BrandDomainServiceImpl.java`

- Purpose: 实现三个新端口方法（委托 Repository）。
- Symbols: `existsActiveByName → repository.countActiveByName(name, excludeId) > 0`；`listPage → converter.toTargetList(repository.selectBrandPage(query, seqMin, seqMax))`；`countPage → repository.countBrandPage(...)`。
- Repository evidence: 生成的 `BrandDomainServiceImpl` 已注入 `brandRepository` 与 `brandPersistenceConverterImpl`（`@Qualifier`），且"域实现只调 Repository 方法"是目录契约。
- Dependencies and consumers: `BrandRepository`（Step 5）；`BrandManageImpl`（File 7）。
- Why now: File 1 端口的实现点；编排 GREEN 的前提。
- Contract/signature changes: 追加实现，不改生成方法。
- Input/output and state mapping: 查重计数 0/1；分页行集 PO→Brand 双向转换器复用。
- Error and edge behavior: Repository 约束违规向上抛（`@Validated` 代理链）。
- Standards impact: `MC-REUSE-001`, `MC-BEAN-001` — 复用既有注入与转换器。
- Literal rule enforcement: `Rule 2` — 域→Repository 交接复用 Step 5 参数级校验；`Rule 11` — 域实现不触 DAO（只经 Repository）。
- Implementation pseudocode:

```java
@Override
public boolean existsActiveByName(String name, Long excludeId) {
    return repository.countActiveByName(name, excludeId) > 0;
}

@Override
public List<Brand> listPage(BrandDomainQuery query, Integer seqMin, Integer seqMax) {
    return converter.toTargetList(repository.selectBrandPage(query, seqMin, seqMax));
}

@Override
public long countPage(BrandDomainQuery query, Integer seqMin, Integer seqMax) {
    return repository.countBrandPage(query, seqMin, seqMax);
}
```

- Verification contribution: `BrandManageImplTest` 的 mock 下游真实签名。
- After this file: 端口实现齐备，模块恢复可编译。

#### File 7 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/BrandManageImpl.java`

- Purpose: 写路径前置守卫、名称前置查重、`listBrands` 编排（total 信封载体）。
- Symbols: `private void requireOperator()`（MDC `userId` 空白 → `CommonException(BRAND_OPERATOR_REQUIRED)`）；`create/update` 前置 `existsActiveByName`（update 传 `command.getId()`）；`listBrands` 组合 `listPage+countPage` → `PageResultRecord.success(resultConverter.toTargetList(rows), total, pageNo, pageSize)`。
- Repository evidence: 生成 `BrandManageImpl` 的 `@Transactional`/字段注入/日志风格；`GradeManageImpl` 的异常工厂先例；Spec §7.3.4 行 1 与 §9.2.3/§9.2.4 映射表。
- Dependencies and consumers: File 1/6 端口；Step 9 控制器；`TEST-011`/`TEST-016`。
- Why now: RED 测试的 GREEN 实现点。
- Contract/signature changes: 定制生成方法体（前置插入），新增 `listBrands`；不改接口既有方法语义。
- Input/output and state mapping: 守卫读 `MDC.get("userId")`；查重用 adapter 归一后的 `command.getName()`；`listBrands` 的 `query.getPage().pageNo()/pageSize()` 回填页元数据；全部写方法保持 `@Transactional`。
- Error and edge behavior: 守卫先于查重先于持久层（`TEST-016` 零交互断言）；update 查重排除自身行避免"未改名即 409"。
- Standards impact: `MC-VALID-001`, `MC-BEAN-001`, `MC-LOG-001`, `MC-PATTERN-001` — 直写 Simple 逻辑 + `@Slf4j` 失败日志点 + 复用既有注入。
- Literal rule enforcement: `Rule 2` — Manage→Domain 交接的守卫序；`Rule 4` — Bean 名/Qualifier/`@Slf4j` 保持；`Rule 9` — Simple 直写不加模式；`Rule 11` — 编排不触持久化类型。
- Implementation pseudocode:

```java
private void requireOperator() {
    String operator = MDC.get("userId");
    if (operator == null || operator.isBlank()) {
        throw new CommonException(GoodsErrorStatus.BRAND_OPERATOR_REQUIRED);
    }
}

@Override
@Transactional
public BrandResult create(@Valid CreateBrandCommand command) {
    requireOperator();
    if (domainService.existsActiveByName(command.getName(), null)) {
        throw new CommonException(GoodsErrorStatus.BRAND_NAME_DUPLICATED);
    }
    return resultConverter.toTarget(domainService.create(createConverter.toTarget(command)));
}

@Override
@Transactional
public BrandResult update(@Valid UpdateBrandCommand command) {
    requireOperator();
    if (domainService.existsActiveByName(command.getName(), command.getId())) {
        throw new CommonException(GoodsErrorStatus.BRAND_NAME_DUPLICATED);
    }
    return resultConverter.toTarget(domainService.update(updateConverter.toTarget(command)));
}

@Override
@Transactional
public void delete(@Valid DeleteBrandCommand command) {
    requireOperator();
    domainService.delete(command.getId(), command.getExpectedVersion());
}

@Override
@Transactional(readOnly = true)
public PageResultRecord<BrandResult> listBrands(@Valid BrandPageQuery query,
        Integer seqMin, Integer seqMax) {
    List<BrandResult> records = resultConverter.toTargetList(
            domainService.listPage(query, seqMin, seqMax));
    long total = domainService.countPage(query, seqMin, seqMax);
    return PageResultRecord.success(records, total,
            query.getPage().pageNo(), query.getPage().pageSize());
}
```

- Verification contribution: File 3 全部用例 GREEN；`TEST-004`/`TEST-005`（version 条件与冲突映射）经由生成的域实现 + `GoodsErrorMapper` 在 Step 11 域层测试补充。
- After this file: 应用层行为完整；模块测试通过。

- Validation working directory: `/Users/mario/SelfProject/enjoyshop/enjoyshop-service/enjoyshop-service-goods`
- Verification command: `./mvnw -q -Dtest=BrandManageImplTest test && ./mvnw -q compile`
- Expected result: 4 个用例通过；两模块编译通过；退出码 0。
- Failure returns to: File 3（断言口径）→ File 7（守卫/查重序）→ File 6/1（端口实现）。
- Completion criteria: RED→GREEN 闭环；`git diff` 对生成文件仅显示声明内的增补（`BrandResult` 字段、转换器注解、编排方法体前置与追加）。
- Rollback: revert 七文件（生成文件回基线后 `check` 一致）。
- Commit paths: `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/BrandDomainService.java`, `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/BrandManage.java`, `enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/application/goods/BrandManageImplTest.java`, `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/BrandResult.java`, `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/converter/BrandResultConverter.java`, `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/service/impl/BrandDomainServiceImpl.java`, `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/BrandManageImpl.java`
- Commit: `feat(goods): 品牌写路径守卫与查重、listBrands 分页编排与 version 贯通`

### Step 7 — 落地 adapter 契约载体与 MapStruct 转换器

- Requirements: `REQ-007`, `REQ-008`, `REQ-012`, `REQ-013`, `REQ-018`
- Dependencies: `Step 6`
- Baseline state: adapter 模块仅有生成控制器与样例 pojo；无品牌载体与转换器；`BrandResult` 已含 `version`。
- Observable outcome: `BrandAdapterConverterTest` RED→GREEN：letter 小写入、大写出；名称 trim+连续空白压缩；`seq` 缺省 0；`BrandResult`（String id）→ `BrandVO`（Long id）；`UpdateBrandRequest.version → expectedVersion`。
- End state: 六个 record + 一个转换器就绪；`BrandListRequest` 六参数与 §9.2.1 参数表一致。
- Test-first gate: `Required` — RED 原因：转换器不存在，归一与投影断言无法满足（缺行为；类型本身随同文件引入，测试以编译失败为 RED 表现并在实现后复跑）。
- Manual Checks: `MC-NAME-001`, `MC-MODEL-001`, `MC-CONVERT-001`, `MC-VALID-001`, `MC-UTIL-001`, `MC-JSON-001`, `MC-SCOPE-001`, `MC-TEST-001`
- Literal Rules: `Rule 1`, `Rule 2`, `Rule 3`, `Rule 5`, `Rule 11`
- Ordered files:

#### File 1 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/goods/pojo/convertor/BrandAdapterConverterTest.java`

- Purpose: 归一与投影行为的 RED 闸门。
- Symbols: `letter_is_normalized_to_uppercase`、`name_is_trimmed_and_space_normalized`、`seq_defaults_to_zero_when_absent`、`result_string_id_maps_to_long_vo`、`update_request_version_maps_to_expected_version`。
- Repository evidence: adapter 模块测试依赖 `spring-boot-starter-test`；MapStruct 单测可直接实例化 Impl。
- Dependencies and consumers: 待产出的 `BrandAdapterConverterImpl`。
- Why now: 转换规则是 §9 契约的归一层，先行锁定。
- Contract/signature changes: 无生产契约变化。
- Input/output and state mapping: 输入构造 record；断言输出字段。
- Error and edge behavior: 空白 letter/名称、超长在 Step 9 的切片负例覆盖（约束注解）；此处只测纯映射。
- Standards impact: `MC-TEST-001`。
- Literal rule enforcement: `Rule 3` — 转换行为断言；`Rule 11` — adapter 测试树。
- Implementation pseudocode:

```java
private final BrandAdapterConverter converter = new BrandAdapterConverterImpl();

@Test
void letter_is_normalized_to_uppercase() {
    assertThat(converter.toTarget(request("Apple", "a", 5)).getLetter()).isEqualTo("A");
}

@Test
void name_is_trimmed_and_space_normalized() {
    assertThat(converter.toTarget(request("  Apple   Store ", "A", 5)).getName()).isEqualTo("Apple Store");
}

@Test
void seq_defaults_to_zero_when_absent() {
    assertThat(converter.toTarget(request("Apple", "A", null)).getSeq()).isZero();
}

@Test
void result_string_id_maps_to_long_vo() {
    BrandResult result = new BrandResult().setId("1948672938456580097").setVersion(3L)
        .setName("Apple").setLetter("A").setSeq(5);
    BrandVO vo = converter.toTarget(result);
    assertThat(vo.id()).isEqualTo(1948672938456580097L);
    assertThat(vo.version()).isEqualTo(3L);
}
```

- Verification contribution: `TEST-001`/`TEST-003` 的映射半部。
- After this file: RED 成立。

#### File 2 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/goods/pojo/dto/CreateBrandRequest.java`

- Purpose: `API-003` 请求载体。
- Symbols: record 组件 `name`（`@NotBlank @Size(max=120)`）、`image`（`@Size(max=500)`）、`letter`（`@NotBlank @Pattern(regexp="[A-Za-z]")`）、`seq`（`@Min(0) @Max(999999)` Integer，可空→服务端补 0）。
- Repository evidence: Spec §9.2.3 参数表逐行；record + `@Schema` 先例（common-core 载体风格）。
- Dependencies and consumers: `BrandController#createBrand`；`BrandAdapterConverter`。
- Why now: 契约载体先于控制器（Step 9）与转换器（File 7）。
- Contract/signature changes: 新类型；不含任何派生字段（`id/tenantId/version` 编译期不存在，Spec §9.2.3 禁区）。
- Input/output and state mapping: JSON → record 构造绑定；未知字段由 Boot 默认忽略。
- Error and edge behavior: 约束命中 → `MethodArgumentNotValidException` → 400 610101（Step 8）。
- Standards impact: `MC-VALID-001`, `MC-NAME-001`, `MC-MODEL-001`, `MC-JSON-001` — 输入约束 + record 载体 + 默认 Jackson。
- Literal rule enforcement: `Rule 1`/`Rule 2` — Request 后缀与输入校验；`Rule 3` — 不可变载体 record 豁免；`Rule 6` — 默认 Jackson 无自定义注解（`@Schema` 为文档注解非序列化契约）。
- Implementation pseudocode:

```java
@Schema(description = "新增品牌请求")
public record CreateBrandRequest(
        @Schema(description = "品牌名称，租户内有效行唯一") @NotBlank @Size(max = 120) String name,
        @Schema(description = "品牌图片 URL，可空") @Size(max = 500) String image,
        @Schema(description = "首字母，单字符 A-Z") @NotBlank @Pattern(regexp = "[A-Za-z]") String letter,
        @Schema(description = "排序权重，未传补 0") @Min(0) @Max(999999) Integer seq) {
}
```

- Verification contribution: `TEST-003` 负例的宿主。
- After this file: 请求契约就绪。

#### File 3 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/goods/pojo/dto/UpdateBrandRequest.java`

- Purpose: `API-004` 请求载体（整替换 + 乐观锁）。
- Symbols: `name/image/letter` 同 File 2；`seq` 为 `@NotNull @Min(0) @Max(999999)`（整替换必填）；`version` 为 `@NotNull @PositiveOrZero Long`。
- Repository evidence: Spec §9.2.4 参数表（与创建请求必填集合不同，独立类型）。
- Dependencies and consumers: `BrandController#updateBrand`；`BrandAdapterConverter#toCommand(brandId, request)`。
- Why now: 同 File 2。
- Contract/signature changes: 新类型。
- Input/output and state mapping: `version` 是调用方最近读取值；`image=null` 即显式清空。
- Error and edge behavior: `version` 缺失 400；不匹配由条件更新 → 610104（409）。
- Standards impact: `MC-VALID-001`, `MC-NAME-001`, `MC-MODEL-001`, `MC-JSON-001` — 输入约束（含整替换必填的 `seq`/`version`）+ record 载体 + 默认 Jackson。
- Literal rule enforcement: `Rule 1`/`Rule 2` — Request 后缀与输入校验（`version` 必填不可放宽为可选）；`Rule 3` — 不可变载体 record 豁免；`Rule 6` — 默认 Jackson 无自定义注解（`@Schema` 为文档注解非序列化契约）。
- Implementation pseudocode:

```java
@Schema(description = "整替换品牌请求")
public record UpdateBrandRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 500) String image,
        @NotBlank @Pattern(regexp = "[A-Za-z]") String letter,
        @NotNull @Min(0) @Max(999999) Integer seq,
        @Schema(description = "乐观锁版本，必须来自最近一次读取") @NotNull @PositiveOrZero Long version) {
}
```

- Verification contribution: `TEST-004`/`TEST-005` 请求半部。
- After this file: 更新契约就绪。

#### File 4 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/goods/pojo/dto/BrandListRequest.java`

- Purpose: `API-001` 六个 query 参数的绑定载体（`@ModelAttribute`）。
- Symbols: `name`（`@Size(max=120)`）、`letter`（`@Pattern(regexp="[A-Za-z]")`）、`seqMin`/`seqMax`（`@Min(0)` Integer）、`pageNo`/`pageSize`（`@Min(1)` Integer）——全部可空（缺失即缺省语义）。
- Repository evidence: Spec §9.2.1 参数表与模式规则（全量/分页双模式）。
- Dependencies and consumers: `BrandController#listBrands`；`BrandAdapterConverter#toPageQuery`。
- Why now: 同 File 2。
- Contract/signature changes: 新类型；seqMin/seqMax 非 DDL 列，不进入 `BrandPageQuery`（作为独立参数透传，`PLAN-CLAR-004`）。
- Input/output and state mapping: 空白 `name/letter` 在转换器归一为 null（按缺失处理）；分页模式判定在控制器。
- Error and edge behavior: 区间倒置由控制器 if + `CommonException` 兜底 400（Spec §9.2.1 的跨字段复核）。
- Standards impact: `MC-VALID-001`, `MC-NAME-001` — Query 载体命名与约束。
- Literal rule enforcement: `Rule 1`/`Rule 2` — Query 后缀与参数校验。
- Implementation pseudocode:

```java
@Schema(description = "品牌集合查询参数；pageNo 与 pageSize 均缺失为全量模式（pageSize=500）")
public record BrandListRequest(
        @Schema(description = "名称前缀过滤") @Size(max = 120) String name,
        @Schema(description = "首字母精确过滤") @Pattern(regexp = "[A-Za-z]") String letter,
        @Schema(description = "seq 区间下界") @Min(0) Integer seqMin,
        @Schema(description = "seq 区间上界") @Min(0) Integer seqMax,
        @Schema(description = "页码，从 1 起") @Min(1) Integer pageNo,
        @Schema(description = "页大小，缺省 10、上限 500") @Min(1) Integer pageSize) {
}
```

- Verification contribution: `TEST-007`/`TEST-008`/`TEST-009` 参数半部。
- After this file: 列表契约就绪。

#### File 5 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/goods/pojo/vo/BrandVO.java`

- Purpose: 对外唯一品牌表示（`API-001`/`API-002`/`API-003`/`API-004` 响应元素）。
- Symbols: record `id(Long)`、`name`、`image`、`letter`、`seq`、`version(Long)`。
- Repository evidence: Spec §10.1/§10.3 + `PLAN-CLAR-011`（不含审计时间）；不暴露 `tenantId/deletedAt/createUserId/updateUserId`（`REQ-010` 不泄露）。
- Dependencies and consumers: `BrandAdapterConverter`；四个契约响应。
- Why now: 载体先于控制器。
- Contract/signature changes: 新类型。
- Input/output and state mapping: `id` 由 Result 的 String 转 Long，JSON 输出整数（`RISK-005` 口径：透传标识）。
- Error and edge behavior: 无约束（出站投影）。
- Standards impact: `MC-NAME-001`, `MC-MODEL-001`, `MC-JSON-001` — VO 后缀、record、默认 Jackson。
- Literal rule enforcement: `Rule 1`/`Rule 6` — VO 后缀与 Jackson 默认输出。
- Implementation pseudocode:

```java
@Schema(description = "品牌表示；version 为写操作必须回传的乐观锁身份")
public record BrandVO(
        @Schema(description = "品牌 ID（雪花）") Long id,
        @Schema(description = "品牌名称") String name,
        @Schema(description = "图片 URL，可空") String image,
        @Schema(description = "大写首字母") String letter,
        @Schema(description = "排序权重") Integer seq,
        @Schema(description = "乐观锁版本") Long version) {
}
```

- Verification contribution: 全部响应断言的形状。
- After this file: 出站契约就绪。

#### File 6 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/goods/pojo/vo/FieldViolationVO.java`

- Purpose: 400 载荷的字段级错误元素。
- Symbols: record `field`、`constraint`（命中约束注解简名）、`message`（约束默认消息）。
- Repository evidence: Spec §10.1（`FieldViolationVO`，advice 直接构造）与 §9.2.1 错误 jsonc。
- Dependencies and consumers: `BaseExceptionHandler`（Step 8）。
- Why now: advice 的编译前提。
- Contract/signature changes: 新类型。
- Input/output and state mapping: 由 `BindingResult`/`ConstraintViolation` 逐条构造。
- Error and edge behavior: 列表可空集（永不 null 由 `FieldErrorsVO` 归一）。
- Standards impact: `MC-NAME-001`, `MC-MODEL-001`。
- Literal rule enforcement: `Rule 1` — VO 后缀。
- Implementation pseudocode:

```java
@Schema(description = "字段级校验错误")
public record FieldViolationVO(
        @Schema(description = "出错字段名") String field,
        @Schema(description = "命中的约束注解简名") String constraint,
        @Schema(description = "约束默认消息") String message) {
}
```

- Verification contribution: `TEST-003` 的 `data.fieldErrors` 元素断言。
- After this file: 错误元素就绪。

#### File 7 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/goods/pojo/vo/FieldErrorsVO.java`

- Purpose: 400 载荷的 `data` 包装（`{"fieldErrors":[…]}`，信封字段不变）。
- Symbols: record `fieldErrors(List<FieldViolationVO>)`，紧凑构造器 null → `List.of()`。
- Repository evidence: Spec §9.2.1 错误载荷 `data.fieldErrors` 结构；record 紧凑构造归一先例（`PageSlice`）。
- Dependencies and consumers: `BaseExceptionHandler`。
- Why now: advice 的 `ResultRecord<T>` 载荷需要一个元素类型。
- Contract/signature changes: 新类型（`PLAN-CLAR-006` 增补的载体细节）。
- Input/output and state mapping: 不可变列表包装。
- Error and edge behavior: 永不 null。
- Standards impact: `MC-MODEL-001`, `MC-JSON-001` — 不可变集合语义 + Jackson 默认。
- Literal rule enforcement: `Rule 1`/`Rule 3` — VO 后缀与不可变载体。
- Implementation pseudocode:

```java
@Schema(description = "字段错误集合")
public record FieldErrorsVO(
        @Schema(description = "逐项字段错误，永不 null") List<FieldViolationVO> fieldErrors) {
    public FieldErrorsVO {
        fieldErrors = fieldErrors == null ? List.of() : List.copyOf(fieldErrors);
    }
}
```

- Verification contribution: `TEST-003` 载荷结构断言。
- After this file: 错误载体齐备。

#### File 8 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/goods/pojo/convertor/BrandAdapterConverter.java`

- Purpose: Request→Command/Query 归一映射与 Result→VO 投影（MapStruct + `BaseForwardConverter` 主方向）。
- Symbols: `extends BaseForwardConverter<BrandResult, BrandVO>`；方法 `toTarget(CreateBrandRequest)`、`toCommand(Long brandId, UpdateBrandRequest)`、`toPageQuery(BrandListRequest, PageQuery)`、`toTarget(BrandResult)`、`toVOList`、`@Named normalizeName/normalizeLetter`；Bean `brandAdapterConverterImpl`。
- Repository evidence: Spec §10.4 映射归属；`GradeAdapterConverter` 先例（`@Qualifier("gradeAdapterConverterImpl")`）；单接口不可继承两份同泛型接口（`PLAN-CLAR-004`）。
- Dependencies and consumers: Step 6 的 CQE/Result；Step 9 控制器。
- Why now: File 1 RED 的 GREEN 实现点。
- Contract/signature changes: 新类型；归一规则：名称 `StringUtils.normalizeSpace(trim)`、letter `trim().toUpperCase(Locale.ROOT)`、`seq` null→0、列表请求空白→null。
- Input/output and state mapping: 逐字段映射见各 `@Mapping`；`CreateBrandCommand` 无派生字段可映射（MapStruct 编译期校验）。
- Error and edge behavior: 纯函数，无异常路径；`unmappedTargetPolicy=ERROR` 保证字段漂移编译失败。
- Standards impact: `MC-CONVERT-001`, `MC-UTIL-001`, `MC-MODEL-001` — 强制 BaseConverter 体系 + commons-lang3 + record 目标。
- Literal rule enforcement: `Rule 3` — MapStruct + `BaseForwardConverter`，禁 BeanUtils/反射；`Rule 5` — 仅 commons-lang3 `StringUtils.normalizeSpace`；`Rule 1` — Convertor 命名（Egon 惯例拼写）。
- Implementation pseudocode:

```java
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface BrandAdapterConverter extends BaseForwardConverter<BrandResult, BrandVO> {

    @Mapping(target = "name", source = "name", qualifiedByName = "normalizeName")
    @Mapping(target = "letter", source = "letter", qualifiedByName = "normalizeLetter")
    @Mapping(target = "seq", expression = "java(source.seq() == null ? 0 : source.seq())")
    CreateBrandCommand toTarget(CreateBrandRequest source);

    @Mapping(target = "id", source = "brandId")
    @Mapping(target = "expectedVersion", source = "request.version")
    @Mapping(target = "name", source = "request.name", qualifiedByName = "normalizeName")
    @Mapping(target = "letter", source = "request.letter", qualifiedByName = "normalizeLetter")
    UpdateBrandCommand toCommand(Long brandId, UpdateBrandRequest request);

    @Mapping(target = "name", source = "request.name", qualifiedByName = "nullIfBlank")
    @Mapping(target = "letter", source = "request.letter", qualifiedByName = "nullIfBlankUpper")
    @Mapping(target = "page", source = "page")
    BrandPageQuery toPageQuery(BrandListRequest request, PageQuery page);

    @Override
    @Mapping(target = "id", expression = "java(source.getId() == null ? null : Long.valueOf(source.getId()))")
    BrandVO toTarget(BrandResult source);

    List<BrandVO> toVOList(List<BrandResult> results);

    @Named("normalizeName")
    static String normalizeName(String raw) {
        return raw == null ? null : StringUtils.normalizeSpace(raw.trim());
    }

    @Named("normalizeLetter")
    static String normalizeLetter(String raw) {
        return raw == null ? null : raw.trim().toUpperCase(Locale.ROOT);
    }

    @Named("nullIfBlank")
    static String nullIfBlank(String raw) {
        return raw == null || raw.isBlank() ? null : StringUtils.normalizeSpace(raw.trim());
    }

    @Named("nullIfBlankUpper")
    static String nullIfBlankUpper(String raw) {
        return raw == null || raw.isBlank() ? null : raw.trim().toUpperCase(Locale.ROOT);
    }
}
```

- Verification contribution: File 1 全部用例 GREEN。
- After this file: adapter 归一层完整。

- Validation working directory: `/Users/mario/SelfProject/enjoyshop/enjoyshop-service/enjoyshop-service-goods`
- Verification command: `./mvnw -q -Dtest=BrandAdapterConverterTest test`
- Expected result: 4 个用例通过，退出码 0；MapStruct 生成 Impl 无 unmapped 报错。
- Failure returns to: File 1（断言）→ File 8（映射/归一）→ Files 2-4（约束集与 §9 参数表不符）。
- Completion criteria: 六 record + 转换器编译并通过映射断言；无手写 setter 拷贝。
- Rollback: revert 八文件。
- Commit paths: `enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/goods/pojo/convertor/BrandAdapterConverterTest.java`, `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/goods/pojo/dto/CreateBrandRequest.java`, `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/goods/pojo/dto/UpdateBrandRequest.java`, `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/goods/pojo/dto/BrandListRequest.java`, `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/goods/pojo/vo/BrandVO.java`, `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/goods/pojo/vo/FieldViolationVO.java`, `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/goods/pojo/vo/FieldErrorsVO.java`, `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/goods/pojo/convertor/BrandAdapterConverter.java`
- Commit: `feat(goods): 品牌契约载体与 adapter 转换器`

### Step 8 — 落地统一异常处理 BaseExceptionHandler

- Requirements: `REQ-019`, `REQ-010`
- Dependencies: `Step 3`, `Step 7`
- Baseline state: light 自带的 `GlobalExceptionHandler`／`ResponseWrapperHandler`／`ApiResponse` 已在 Step 1 File 1b 删除（`DEC-115`），因此当前无任何 advice，品牌包异常落到 Boot 默认错误体；`GoodsErrorStatus`/`FieldErrorsVO`/`CommonException` 就绪。
- Observable outcome: `BaseExceptionHandlerTest` RED→GREEN：`CommonException(610104)` → 409 + `ResultRecord`（`code=610104`、`status=BRAND_VERSION_CONFLICT`、`traceId` 字段存在、`data=null`）；字段校验异常 → 400 + `data.fieldErrors`；`DuplicateKeyException` → 409 610103；未知异常 → 500 610199 且响应不含异常类名。
- End state: 品牌包内全部异常有稳定信封映射；样例 advice 行为不受影响（包级限定）。
- Test-first gate: `Required` — RED 原因：advice 不存在，MockMvc 对受控异常得到 Boot 默认白页 JSON 而非 §9 信封（缺行为）。
- Manual Checks: `MC-JSON-001`, `MC-LOG-001`, `MC-BEAN-001`, `MC-NAME-001`, `MC-REUSE-001`, `MC-SCOPE-001`, `MC-TEST-001`
- Literal Rules: `Rule 2`, `Rule 4`, `Rule 6`, `Rule 11`
- Ordered files:

#### File 1 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/handler/BaseExceptionHandlerTest.java`

- Purpose: 异常→状态/载荷映射的 RED 闸门（`@WebMvcTest` 切片 + mock manage 抛异常）。
- Symbols: `common_exception_maps_to_envelope_with_http_status`、`validation_failure_carries_field_errors`、`duplicate_key_maps_to_409_610103`、`unexpected_exception_is_sanitized_to_500`。
- Repository evidence: adapter 测试依赖 `spring-boot-starter-test`；`DdcGlobalExceptionHandler`（天枢 Admin）的 `ResultRecord.failure` 先例。
- Dependencies and consumers: 生成 `BrandController`（触发入口）+ mock `BrandManage`。
- Why now: advice 先于控制器定制，使 Step 9 切片断言能依赖稳定错误外形。
- Contract/signature changes: 无生产契约变化。
- Input/output and state mapping: `when(manage.detail(any())).thenThrow(new CommonException(GoodsErrorStatus.BRAND_VERSION_CONFLICT))` → `GET /api/v1/brands/1` 断言 `$.code==610104`、`$.status`、HTTP 409、`$.data==null`。
- Error and edge behavior: 断言响应不含 `java.lang`、不含 SQL 字样（`REQ-019` 脱敏）。
- Standards impact: `MC-TEST-001`, `MC-JSON-001` — 信封结构与 int 码断言。
- Literal rule enforcement: `Rule 2` — 校验失败负例；`Rule 11` — 切片位于 adapter 测试树。
- Implementation pseudocode:

```java
@WebMvcTest(controllers = BrandController.class)
class BaseExceptionHandlerTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean BrandManage brandManage;

    @Test
    void common_exception_maps_to_envelope_with_http_status() throws Exception {
        when(brandManage.detail(any())).thenThrow(new CommonException(GoodsErrorStatus.BRAND_VERSION_CONFLICT));
        mockMvc.perform(get("/api/v1/brands/1"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value(610104))
            .andExpect(jsonPath("$.status").value("BRAND_VERSION_CONFLICT"))
            .andExpect(jsonPath("$.data").doesNotExist())
            .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    void unexpected_exception_is_sanitized_to_500() throws Exception {
        when(brandManage.detail(any())).thenThrow(new IllegalStateException("jdbc:postgresql://internal-db"));
        mockMvc.perform(get("/api/v1/brands/1"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.code").value(610199))
            .andExpect(content().string(not(containsString("jdbc:postgresql"))))
            .andExpect(content().string(not(containsString("IllegalStateException"))));
    }
}
```

- Verification contribution: `TEST-005`/`TEST-010`/`TEST-016` 的映射半部。
- After this file: RED 成立（Boot 默认错误体）。

#### File 2 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/handler/BaseExceptionHandler.java`

- Purpose: 用户指定的统一异常处理类：§9 全部错误行 → 信封 + 安全 HTTP 状态。
- Symbols: `@RestControllerAdvice(name="baseExceptionHandler", basePackages="top.egon.enjoyshop.goods.adapter.goods")`；handlers：`CommonException`、`MethodArgumentNotValidException`/`BindException`、`ConstraintViolationException`、`MissingServletRequestParameterException`/`MethodArgumentTypeMismatchException`/`HttpMessageNotReadableException`/`IllegalArgumentException`、`DuplicateKeyException`、`Exception`；`httpStatus(int code)`。
- Repository evidence: Spec §9.2 各映射表 + §7.3.4 + `EVD-015` 的 `name` 属性先例；`ResultRecord.failure(Throwable)` 已消费 `CommonException` 码/状态。
- Dependencies and consumers: `GoodsErrorStatus`、`FieldErrorsVO`、`FieldViolationVO`、`CommonException`；全部品牌路由。
- Why now: File 1 RED 的 GREEN 实现点；先于 Step 9。
- Contract/signature changes: 新 Bean（`baseExceptionHandler`）。
- Input/output and state mapping: 610101→400、610102→404、610103/610104→409、610105→401、其余→500；字段错误聚合为 `FieldErrorsVO` 放 `data`；`traceId` 由 `ResultRecord` 从 `TraceContext` 取。
- Error and edge behavior: `Exception` 分支 `log.error` 含 traceId 与异常（服务端日志保留栈），响应体只含信封与 610199；不重排样例 advice（包级限定，`PLAN-CLAR-007`）。
- Standards impact: `MC-JSON-001`, `MC-LOG-001`, `MC-BEAN-001`, `MC-REUSE-001` — Jackson 信封、`@Slf4j`、显式 Bean 名、复用 `ResultRecord.failure`。
- Literal rule enforcement: `Rule 2` — 校验异常聚合；`Rule 4` — `@Slf4j` + Bean 名；`Rule 6` — int 码/name() 状态输出；`Rule 11` — advice 在 adapter 层。
- Implementation pseudocode:

```java
@Slf4j
@RestControllerAdvice(name = "baseExceptionHandler", basePackages = "top.egon.enjoyshop.goods.adapter.goods")
public class BaseExceptionHandler {

    @ExceptionHandler(CommonException.class)
    public ResponseEntity<ResultRecord<Void>> handleCommon(CommonException exception) {
        return ResponseEntity.status(httpStatus(exception.getCode())).body(ResultRecord.failure(exception));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResultRecord<FieldErrorsVO> handleInvalidBody(BindException exception) {
        List<FieldViolationVO> violations = exception.getBindingResult().getFieldErrors().stream()
            .map(error -> new FieldViolationVO(error.getField(),
                error.getConstraint() == null ? null : error.getConstraint().getSimpleName(),
                error.getDefaultMessage()))
            .toList();
        log.warn("brand request invalid: {} field errors", violations.size());
        return ResultRecord.result(GoodsErrorStatus.BRAND_REQUEST_INVALID.getCode(),
            GoodsErrorStatus.BRAND_REQUEST_INVALID.getStatus(),
            GoodsErrorStatus.BRAND_REQUEST_INVALID.getMessage(), false, new FieldErrorsVO(violations));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResultRecord<FieldErrorsVO> handleConstraint(ConstraintViolationException exception) { /* 同形聚合 violations */ }

    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class, IllegalArgumentException.class})
    public ResultRecord<Void> handleBadRequest(Exception exception) {
        log.warn("brand bad request: {}", exception.getClass().getSimpleName());
        return ResultRecord.failure(GoodsErrorStatus.BRAND_REQUEST_INVALID);
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResultRecord<Void> handleDuplicate(DuplicateKeyException exception) {
        log.warn("brand unique conflict on concurrent create");
        return ResultRecord.failure(GoodsErrorStatus.BRAND_NAME_DUPLICATED);
    }

    @ExceptionHandler(Exception.class)
    public ResultRecord<Void> handleUnexpected(Exception exception) {
        log.error("unexpected brand failure", exception);
        return ResultRecord.failure(GoodsErrorStatus.BRAND_INTERNAL_ERROR);
    }

    private static HttpStatusCode httpStatus(int code) {
        return switch (code) {
            case 610101 -> HttpStatus.BAD_REQUEST;
            case 610102 -> HttpStatus.NOT_FOUND;
            case 610103, 610104 -> HttpStatus.CONFLICT;
            case 610105 -> HttpStatus.UNAUTHORIZED;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
```

- Verification contribution: File 1 全部用例 GREEN；Step 9 切片的错误分支依赖。
- After this file: 品牌包异常面统一；模块测试通过。

- Validation working directory: `/Users/mario/SelfProject/enjoyshop/enjoyshop-service/enjoyshop-service-goods`
- Verification command: `./mvnw -q -Dtest=BaseExceptionHandlerTest test`
- Expected result: 4 个用例通过，退出码 0。
- Failure returns to: File 1（断言）→ File 2（映射/脱敏）。
- Completion criteria: `REQ-019` 的映射表逐行有测试或 Step 9 切片覆盖；响应不含内部细节。
- Rollback: revert 两文件。
- Commit paths: `enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/handler/BaseExceptionHandlerTest.java`, `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/handler/BaseExceptionHandler.java`
- Commit: `feat(goods): 统一异常处理 BaseExceptionHandler 与信封映射`

### Step 9 — 定制 BrandController 为 §9 五契约并修正操作者缺省

- Requirements: `REQ-007`, `REQ-008`, `REQ-009`, `REQ-010`, `REQ-011`, `REQ-012`, `REQ-013`, `REQ-014`, `REQ-019`
- Dependencies: `Step 6`, `Step 7`, `Step 8`
- Baseline state: 生成版 `BrandController` 返回裸 `BrandResult`/`PageSlice`，无信封、无 201/Location、无 `@Operation`/`@Tag`、分页无 total；过滤器把缺省操作者写为 `"anonymous"`，写路径守卫（Step 6）不可达。
- Observable outcome: `BrandControllerWebTest` RED→GREEN：`POST` 得 201 + `Location: /api/v1/brands/{id}` + `ResultRecord<BrandVO>`；`GET`（无分页参数）得 `PageResultRecord` 且 `page.pageSize==500`、`records` 非 null；`GET` 越界页空集合；`PUT` 后 `data.version` 为新版本；`DELETE?version=` 得 `data==true`，重复删除 404；`brandId=0` 400。
- End state: §9.2 五契约外形、operationId、错误码声明、`X-Tenant-Id`/操作者引导语义全部就位。
- Test-first gate: `Required` — RED 原因：生成版外形与 §9 逐条不符（断言全失败），而非夹具问题。
- Manual Checks: `MC-VALID-001`, `MC-JSON-001`, `MC-BEAN-001`, `MC-NAME-001`, `MC-REUSE-001`, `MC-TIME-001`, `MC-SCOPE-001`, `MC-TEST-001`
- Literal Rules: `Rule 2`, `Rule 4`, `Rule 6`, `Rule 10`, `Rule 11`
- Ordered files:

#### File 1 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/goods/BrandControllerWebTest.java`

- Purpose: 五契约外形的 RED 闸门（`@WebMvcTest` + mock `BrandManage`）。
- Symbols: `create_returns_201_with_location_and_envelope`、`list_without_page_params_uses_full_mode_500`、`out_of_range_page_returns_empty_records`、`delete_returns_true_and_repeat_maps_404`、`non_positive_brand_id_is_400`、`write_without_actor_is_401_via_advice`。
- Repository evidence: adapter 测试依赖；Spec §9.2 各契约成功/错误 jsonc 与 `TEST-002`/`007`/`009`/`010`/`016` 断言原文。
- Dependencies and consumers: `BrandController`、`BrandManage`（mock）、`BaseExceptionHandler`（切片自动装配 `@RestControllerAdvice`）。
- Why now: 先固定 §9 外形再改控制器，保证定制可审计。
- Contract/signature changes: 无生产契约变化。
- Input/output and state mapping: mock 返回构造的 `BrandResult`/`PageResultRecord`；断言 `$.data.version`、`$.page.pageSize`、`Location` 头、`$.code`。
- Error and edge behavior: 覆盖 400/401/404/409 的代表性分支；空 `records` 非 null。
- Standards impact: `MC-TEST-001`, `MC-VALID-001` — 契约负例。
- Literal rule enforcement: `Rule 2`/`Rule 11` — 切片负例与 adapter 测试树。
- Implementation pseudocode:

```java
@WebMvcTest(controllers = BrandController.class)
class BrandControllerWebTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean BrandManage brandManage;

    @Test
    void create_returns_201_with_location_and_envelope() throws Exception {
        when(brandManage.create(any())).thenReturn(new BrandResult().setId("1948672938456580097")
            .setVersion(0L).setName("Apple").setLetter("A").setSeq(100));
        mockMvc.perform(post("/api/v1/brands").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Apple\",\"letter\":\"a\",\"seq\":100}"))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/v1/brands/1948672938456580097"))
            .andExpect(jsonPath("$.code").value(10000))
            .andExpect(jsonPath("$.data.version").value(0))
            .andExpect(jsonPath("$.data.letter").value("A"));
    }

    @Test
    void list_without_page_params_uses_full_mode_500() throws Exception {
        when(brandManage.listBrands(any(), isNull(), isNull())).thenReturn(
            PageResultRecord.success(List.of(), 0L, 1, 500));
        mockMvc.perform(get("/api/v1/brands"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.pageSize").value(500))
            .andExpect(jsonPath("$.records").isArray())
            .andExpect(jsonPath("$.records").isEmpty());
    }
}
```

- Verification contribution: `TEST-002`/`003`/`006`/`007`/`009`/`010`/`016` 的契约半部。
- After this file: RED 成立（生成版外形不符）。

#### File 2 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/goods/BrandController.java`

- Purpose: 按声明的定制把生成控制器替换为 §9 五契约实现（信封、201/Location、OpenAPI 注解、模式判定、区间复核）。
- Symbols: 五方法 `listBrands`/`getBrand`/`createBrand`/`updateBrand`/`deleteBrand`；`@Tag(name="brand")`、`@Validated`、`@Qualifier("brandManage")`/`@Qualifier("brandAdapterConverterImpl")`；`@Operation(operationId=…)` 五处；`@ApiResponses` 按契约声明错误码（401 仅三条写契约）。
- Repository evidence: Spec §9.2/§9.3 注解计划；`GradeController` 的 201+`ResponseEntity.created` 先例；`EgonOperationCustomizer` 的 operationId 强约束（`EVD-023`）。
- Dependencies and consumers: Step 6 `BrandManage`、Step 7 转换器与载体、Step 8 advice。
- Why now: 契约链全部就绪，定制一次成型。
- Contract/signature changes: 五个方法签名与返回类型替换为 §9 形状；`parseId` 移除（改用 `Long` 路径变量 + `@Positive`，类型不匹配由 `MethodArgumentTypeMismatchException` → 400）。
- Input/output and state mapping: 全量模式 `PageQuery(PageQuery.DEFAULT_PAGE_NO, PageQuery.MAX_PAGE_SIZE)`；分页模式缺项补 `DEFAULT_PAGE_NO/DEFAULT_PAGE_SIZE`；`listBrands` 取 `PageResultRecord` 载体后以 `converter.toVOList` 重建信封（`PLAN-CLAR-010`）；`deleteBrand` 成功返回 `ResultRecord.success(Boolean.TRUE)`（200 而非 204，`DEC-109` 信封不变量）。
- Error and edge behavior: 区间倒置 `if (seqMin!=null && seqMax!=null && seqMax<seqMin) throw CommonException(610101)`；`@Valid`/`@Validated` 负例经 advice 400/401；不触碰 `-facade`。
- Standards impact: `MC-VALID-001`, `MC-BEAN-001`, `MC-NAME-001`, `MC-REUSE-001` — 类级 `@Validated`、显式 Bean 名/Qualifier、复用信封与 `PageQuery` 归一。
- Literal rule enforcement: `Rule 2` — 输入 `@Valid` + 跨字段复核；`Rule 4` — Bean 名/Qualifier/`@Slf4j`；`Rule 6` — 默认 Jackson 输出信封；`Rule 10` — 无新时间类型；`Rule 11` — 控制器只经 Manage 端口。
- Implementation pseudocode:

```java
@RestController("brandController")
@RequestMapping("/api/v1/brands")
@Validated
@Tag(name = "brand", description = "商品品牌字典")
@RequiredArgsConstructor
@Slf4j
public class BrandController {

    @Qualifier("brandManage")
    private final BrandManage manage;
    @Qualifier("brandAdapterConverterImpl")
    private final BrandAdapterConverter converter;

    @GetMapping
    @Operation(operationId = "listBrands", summary = "查询品牌集合")
    @ApiResponses({@ApiResponse(responseCode = "400", ...), @ApiResponse(responseCode = "500", ...)})
    public PageResultRecord<BrandVO> listBrands(@Valid BrandListRequest request) {
        if (request.seqMin() != null && request.seqMax() != null && request.seqMax() < request.seqMin()) {
            throw new CommonException(GoodsErrorStatus.BRAND_REQUEST_INVALID);
        }
        PageQuery page = request.pageNo() == null && request.pageSize() == null
                ? new PageQuery(PageQuery.DEFAULT_PAGE_NO, PageQuery.MAX_PAGE_SIZE)
                : new PageQuery(request.pageNo() == null ? PageQuery.DEFAULT_PAGE_NO : request.pageNo(),
                        request.pageSize() == null ? PageQuery.DEFAULT_PAGE_SIZE : request.pageSize());
        PageResultRecord<BrandResult> payload =
                manage.listBrands(converter.toPageQuery(request, page), request.seqMin(), request.seqMax());
        return PageResultRecord.success(converter.toVOList(payload.records()),
                payload.page().total(), payload.page().pageNo(), payload.page().pageSize());
    }

    @GetMapping("/{brandId}")
    @Operation(operationId = "getBrand", summary = "查询品牌详情")
    public ResultRecord<BrandVO> getBrand(@Positive @PathVariable Long brandId) {
        return ResultRecord.success(converter.toTarget(
                manage.detail(new BrandDetailQuery().setId(brandId))));
    }

    @PostMapping
    @Operation(operationId = "createBrand", summary = "新增品牌")
    public ResponseEntity<ResultRecord<BrandVO>> createBrand(@Valid @RequestBody CreateBrandRequest request) {
        BrandVO created = converter.toTarget(manage.create(converter.toTarget(request)));
        return ResponseEntity.created(URI.create("/api/v1/brands/" + created.id()))
                .body(ResultRecord.success(created));
    }

    @PutMapping("/{brandId}")
    @Operation(operationId = "updateBrand", summary = "更新品牌")
    public ResultRecord<BrandVO> updateBrand(@Positive @PathVariable Long brandId,
            @Valid @RequestBody UpdateBrandRequest request) {
        return ResultRecord.success(converter.toTarget(manage.update(converter.toCommand(brandId, request))));
    }

    @DeleteMapping("/{brandId}")
    @Operation(operationId = "deleteBrand", summary = "下架品牌")
    public ResultRecord<Boolean> deleteBrand(@Positive @PathVariable Long brandId,
            @NotNull @PositiveOrZero @RequestParam Long version) {
        manage.delete(new DeleteBrandCommand().setId(brandId).setExpectedVersion(version));
        return ResultRecord.success(Boolean.TRUE);
    }
}
```

- Verification contribution: File 1 全部用例 GREEN；`TEST-014` 的注解事实源。
- After this file: §9 五契约外形完成。

#### File 3 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/filter/TenantContextFilter.java`

- Purpose: 新增写 MDC 的租户与操作者过滤器，使 MP-SDJ 的租户拦截器与审计填充有 MDC 来源（light 自带 `RequestContextFilter` 用 ThreadLocal，不写 MDC，`DEC-116`／`PLAN-CLAR-014`）。
- Symbols: `@Component("tenantContextFilter")` 的 `OncePerRequestFilter`；常量 `TENANT_HEADER="X-Tenant-Id"`、`OPERATOR_HEADER="X-Actor-Id"`、`DEFAULT_TENANT_ID="1"`；`doFilterInternal` 写 MDC `tenantId` 与 `userId`，`finally` 内 `MDC.remove` 两键。
- Repository evidence: Spec `EVD-027`（light 自带 `RequestContextFilter.java:22-40` 用 `X-Operator-Id` 加 `RequestContextHolder.set(...)`，`TraceIdFilter.java:36-40` 只写 MDC `traceId`）；Spec `EVD-012`（`EgonColaTenantIdProvider` 从 MDC `tenantId` 解析，缺失抛 `TENANT_CONTEXT_MISSING`；审计用户取 MDC `userId`）；Spec §7.3.1 步骤 1。
- Dependencies and consumers: MDC `tenantId`／`userId`（Step 6 守卫与 MP 审计填充、MP-SDJ 租户拦截器）；`TEST-016`；自带 `RequestContextFilter` 与本过滤器共存，职责不重叠（前者提供 ThreadLocal 视图，后者提供 MDC）。
- Why now: 与契约定稿同批（`TEST-016` 的 MockMvc 半部在本 Step 验证 401）；且它必须在任何品牌 SQL 执行前就位。
- Contract/signature changes: 新增一个 filter Bean；不修改自带 `RequestContextFilter`／`TraceIdFilter`。
- Input/output and state mapping: 缺失或空白 `X-Tenant-Id` → MDC `tenantId="1"`（`DEC-107` 选 A 的兜底）；缺失或空白 `X-Actor-Id` → MDC `userId=""`（读路径不受影响，写路径 401 610105）；请求结束在 `finally` 清理两键。
- Error and edge behavior: 过滤器不抛异常（缺省值兜底），因此正常 HTTP 路径不产生 `TENANT_CONTEXT_MISSING`；MDC 清理在 `finally` 中执行，避免线程池串号（`EVD-012` 的 javadoc 已声明该风险）。
- Standards impact: `MC-BEAN-001`, `MC-SCOPE-001` — 显式 Bean 名 `tenantContextFilter`；新增范围仅此一文件，自带过滤器不动。
- Literal rule enforcement: `Rule 4` — Spring 单例显式命名；`Rule 11` — 上下文入口唯一（MDC 写入方只有本类）。
- Implementation pseudocode:

```java
package top.egon.enjoyshop.goods.adapter.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component("tenantContextFilter")
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class TenantContextFilter extends OncePerRequestFilter {

    private static final String TENANT_HEADER = "X-Tenant-Id";
    private static final String OPERATOR_HEADER = "X-Actor-Id";
    private static final String DEFAULT_TENANT_ID = "1";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        MDC.put("tenantId", headerOrDefault(request, TENANT_HEADER, DEFAULT_TENANT_ID));
        MDC.put("userId", headerOrDefault(request, OPERATOR_HEADER, ""));
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("tenantId");
            MDC.remove("userId");
        }
    }

    private static String headerOrDefault(HttpServletRequest request, String header, String fallback) {
        String value = request.getHeader(header);
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
```

- Verification contribution: `BrandControllerWebTest` 的 401 用例（无 `X-Actor-Id` 头时经 advice 得 610105）；`TEST-010` 的租户隔离用例的 MDC 前置。
- After this file: 上下文语义与 §7.3.1 步骤 1 一致，且 MP-SDJ 有 MDC 来源。

- Validation working directory: `/Users/mario/SelfProject/enjoyshop/enjoyshop-service/enjoyshop-service-goods`
- Verification command: `./mvnw -q -Dtest='BrandControllerWebTest,BaseExceptionHandlerTest' test`
- Expected result: 两测试类全部用例通过，退出码 0。
- Failure returns to: File 1（断言）→ File 2（契约外形）→ File 3（MDC 未就位导致守卫不可达）。
- Completion criteria: 五契约的请求/响应/错误分支与 §9.2 一致；`git diff` 显示控制器为整体定制（生成基线已在 Step 4 提交可审计）。
- Rollback: revert 三文件（控制器回到生成基线，MDC 过滤器消失，其余 Step 不受影响）。
- Commit paths: `enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/goods/BrandControllerWebTest.java`, `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/goods/BrandController.java`, `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/filter/TenantContextFilter.java`
- Commit: `feat(goods): BrandController 定制为五契约并新增 MDC 租户过滤器`

### Step 10 — 多环境配置键收口（Rule 7：五份 YAML 同树变更 + 键奇偶测试）

- Requirements: `REQ-004`
- Dependencies: `Step 4`
- Baseline state: 生成工程四份 profile + `egon-mybatis-plus-sharding.yml` 的 sharding `tables` 均无 `brand`；dev/test/prod 的 `TIANSHU_ENABLED`/`TIANSHU_REDIS_ENABLED`/`TIANSHU_REGISTRY_ENABLED`/`TIANSHU_HTTP_REGISTRATION_ENABLED`/`TIANQUAN_SHOUBING_ENABLED` 缺省为 `true`（base `application.yml` 为字面 `false`）；`yuheng.openapi.enabled` 缺省已为 `false`（`OPENAPI_GOVERNANCE_ENABLED:false`）。
- Observable outcome: `ProfileKeyParityTest` RED→GREEN：四份 profile 扁平键集合完全相等；五处 `*_ENABLED` 在空环境变量下解析为 `false`；`brand` 出现在全部承载 `tables` 映射的文件中。
- End state: 缺 Redis/天枢 Admin/守兵凭据时应用可启动（绑定层面）；brand 路由到 `master_data`。
- Test-first gate: `Required` — RED 原因：`brand` 表键与缺省翻转尚未写入，键集合/缺省断言失败。
- Manual Checks: `MC-CONFIG-001`, `MC-SCOPE-001`, `MC-TEST-001`
- Literal Rules: `Rule 7`, `Rule 11`
- Ordered files:

#### File 1 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/resources/egon-mybatis-plus-sharding.yml`

- Purpose: base 导入的分片配置注册 brand 表。
- Symbols: `egon.cola.component.mybatis-plus.sharding.tables.brand: {type: SINGLE, data-source: master_data}`。
- Repository evidence: 该文件已被 base `application.yml` `spring.config.import` 引用；既有表条目形状（`users: {type: SINGLE, data-source: master_data}`）。
- Dependencies and consumers: MP-SDJ STRATEGY 路由；`DEC-114` 选 A 的 SINGLE 落点。
- Why now: brand DDL 已存在（Step 2），路由注册是其运行期对应物。
- Contract/signature changes: 追加一个表键；不改既有键值。
- Input/output and state mapping: `SINGLE` → 无分片键，`tenant_id` 仅为列。
- Error and edge behavior: 键写错会导致启动绑定失败（本迭代仅静态与绑定测试验证）。
- Standards impact: `MC-CONFIG-001` — 同树键集合一致。
- Literal rule enforcement: `Rule 7` — base 文件先改；`Rule 11` — MP-SDJ 受管分片配置。
- Implementation pseudocode:

```yaml
egon:
  cola:
    component:
      mybatis-plus:
        sharding:
          # enabled/mode/config-style/transaction-default-type/data-sources 既有值逐字保留
          tables:
            # ... 既有样例表条目（users/roles/permissions/user_roles/role_permissions/grades 等）逐字保留 ...
            brand:
              type: SINGLE            # DEC-114 选 A：品牌为 SINGLE 主数据表
              data-source: master_data
```

- Verification contribution: `ProfileKeyParityTest` 的 brand 键断言。
- After this file: base 分片注册完成。

#### File 2 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/resources/application.yml`

- Purpose: base profile 同步 brand 表注册；确认天枢/yuheng 键保持缺省关闭。
- Symbols: sharding `tables.brand`（同 File 1）；`egon.cola.component.tianshu.enabled: false` 与 `egon.cola.component.yuheng.openapi.enabled: ${OPENAPI_GOVERNANCE_ENABLED:false}` 保持不变。
- Repository evidence: base 文件含完整 sharding 块（`tables` 映射在第 ~138 行区域）；tianshu 字面 `false` 已核实。
- Dependencies and consumers: 所有 profile 的父层配置。
- Why now: Rule 7 要求 base 与每个环境文件同树修改。
- Contract/signature changes: 仅追加 brand 表键。
- Input/output and state mapping: 同 File 1。
- Error and edge behavior: 不删除任何既有键（键集合一致性）。
- Standards impact: `MC-CONFIG-001`。
- Literal rule enforcement: `Rule 7` — 键结构一致、值可有别。
- Implementation pseudocode:

```yaml
# application.yml（base）：既有 mybatis-plus 块内 sharding.tables 追加 brand，其余键逐字保留
egon:
  cola:
    component:
      mybatis-plus:
        sharding:
          tables:
            brand:
              type: SINGLE
              data-source: master_data
# 核对项：egon.cola.component.tianshu.enabled 保持字面 false（第 185 行区域）
# 核对项：egon.cola.component.yuheng.openapi.enabled 保持 ${OPENAPI_GOVERNANCE_ENABLED:false}
# 核对项：dynamic-table-name 的空 tables 不涉及 brand（该键属另一特性，保持 {} 不动）
```

- Verification contribution: 键奇偶断言的 base 半部。
- After this file: base 配置收口。

#### File 3 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-dev.yml`

- Purpose: dev 环境缺省翻转 + brand 注册。
- Symbols: `TIANSHU_ENABLED:false`、`TIANSHU_REDIS_ENABLED:false`、`TIANSHU_REGISTRY_ENABLED:false`、`TIANSHU_HTTP_REGISTRATION_ENABLED:false`、`TIANQUAN_SHOUBING_ENABLED:false` 五处缺省值翻转；sharding `tables.brand` 追加。
- Repository evidence: dev 文件第 182-199、231 行区域的 `${...:true}` 缺省（已核对）。
- Dependencies and consumers: dev 启动上下文。
- Why now: `DEC-107`/`DEC-111` 选 A——键写全、默认关闭。
- Contract/signature changes: 仅占位符缺省值与 brand 表键；键集合不变。
- Input/output and state mapping: 环境变量仍可显式开启（值可不同、键必须一致）。
- Error and edge behavior: 翻转后缺外部实例不阻断启动。
- Standards impact: `MC-CONFIG-001`。
- Literal rule enforcement: `Rule 7` — 同树修改环境文件。
- Implementation pseudocode:

```yaml
tianshu:
  enabled: ${TIANSHU_ENABLED:false}          # 原 :true
  redis:
    enabled: ${TIANSHU_REDIS_ENABLED:false}  # 原 :true
  registry:
    enabled: ${TIANSHU_REGISTRY_ENABLED:false}
    http:
      enabled: ${TIANSHU_HTTP_REGISTRATION_ENABLED:false}
platform:
  tianquan:
    shoubing:
      enabled: ${TIANQUAN_SHOUBING_ENABLED:false}   # 原 :true
# sharding.tables 追加 brand: {type: SINGLE, data-source: master_data}
```

- Verification contribution: `ProfileKeyParityTest` 的 dev 半部与缺省断言。
- After this file: dev 收口。

#### File 4 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-test.yml`

- Purpose: test 环境与 dev 相同的翻转与注册。
- Symbols: 同 File 3 五处缺省 + brand 表键。
- Repository evidence: test 文件与 dev 同构（已核对行号区域）。
- Dependencies and consumers: test 启动上下文。
- Why now: Rule 7。
- Contract/signature changes: 同 File 3。
- Input/output and state mapping: 同 File 3。
- Error and edge behavior: 同 File 3。
- Standards impact: `MC-CONFIG-001`。
- Literal rule enforcement: `Rule 7`。
- Implementation pseudocode:

```yaml
# application-test.yml —— 五处治理开关缺省翻转（:true → :false）+ sharding.tables 追加 brand
egon:
  cola:
    component:
      tianshu:
        enabled: ${TIANSHU_ENABLED:false}
        redis:
          enabled: ${TIANSHU_REDIS_ENABLED:false}
        registry:
          enabled: ${TIANSHU_REGISTRY_ENABLED:false}
          http:
            enabled: ${TIANSHU_HTTP_REGISTRATION_ENABLED:false}
    platform:
      tianquan:
        shoubing:
          enabled: ${TIANQUAN_SHOUBING_ENABLED:false}
# egon.cola.component.mybatis-plus.sharding.tables 追加：
#   brand: { type: SINGLE, data-source: master_data }
# yuheng.openapi.enabled 保持 ${OPENAPI_GOVERNANCE_ENABLED:false} 不动（缺省已为 false）
```

- Verification contribution: 奇偶断言 test 半部。
- After this file: test 收口。

#### File 5 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-prod.yml`

- Purpose: prod 环境与 dev 相同的翻转与注册。
- Symbols: 同 File 3 五处缺省 + brand 表键。
- Repository evidence: prod 文件第 182-231 行区域（已核对）。
- Dependencies and consumers: prod 启动上下文。
- Why now: Rule 7；`DEC-107` 选 A 期间 prod 同样默认关闭。
- Contract/signature changes: 同 File 3。
- Input/output and state mapping: 同 File 3。
- Error and edge behavior: 同 File 3；真实接入时按新 Spec 显式开启并补发布键。
- Standards impact: `MC-CONFIG-001`。
- Literal rule enforcement: `Rule 7`。
- Implementation pseudocode:

```yaml
# application-prod.yml —— 与 dev/test 相同的五处治理开关缺省翻转 + sharding.tables 追加 brand
egon:
  cola:
    component:
      tianshu:
        enabled: ${TIANSHU_ENABLED:false}          # 原 :true（第 182 行区域）
        redis:
          enabled: ${TIANSHU_REDIS_ENABLED:false}  # 原 :true（第 189 行区域）
        registry:
          enabled: ${TIANSHU_REGISTRY_ENABLED:false}
          http:
            enabled: ${TIANSHU_HTTP_REGISTRATION_ENABLED:false}
    platform:
      tianquan:
        shoubing:
          enabled: ${TIANQUAN_SHOUBING_ENABLED:false}   # 原 :true（第 231 行区域）
# egon.cola.component.mybatis-plus.sharding.tables 追加：
#   brand: { type: SINGLE, data-source: master_data }
# 真实接入天枢/玉衡时按新 Spec 显式开启并补齐发布键，本迭代一律缺省关闭
```

- Verification contribution: 奇偶断言 prod 半部。
- After this file: 四份 profile + base 导入文件全部收口。

#### File 6 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/start/config/ProfileKeyParityTest.java`

- Purpose: `TEST-013` 的执行载体：四份 profile 键集合相等 + 缺省关闭断言（SnakeYAML 纯解析，不启动 Spring 上下文、不连任何实例）。
- Symbols: `four_profiles_have_identical_key_sets`、`governance_switches_default_to_false`、`brand_registered_in_every_tables_map`。
- Repository evidence: starter 模块测试依赖 `spring-boot-starter-test`（含 snakeyaml 传递）；`TEST-013` "收集四份文件的全部键集合"。
- Dependencies and consumers: Files 1-5 的资源文件。
- Why now: 配置修改的 RED→GREEN 闸门与长期防漂移。
- Contract/signature changes: 无生产契约变化。
- Input/output and state mapping: 递归扁平化 YAML 键（`egon.cola.component.tianshu.enabled` 风格）；比较 Set；对含 `${VAR:default}` 的值解析缺省。
- Error and edge behavior: 任何键差异/缺省非 false/brand 缺席 → 断言失败。
- Standards impact: `MC-TEST-001`, `MC-CONFIG-001` — 键奇偶有客观命令。
- Literal rule enforcement: `Rule 7` — 键集合一致性由测试强制。
- Implementation pseudocode:

```java
private static final List<String> PROFILES = List.of(
    "application.yml", "application-dev.yml", "application-test.yml", "application-prod.yml");

@Test
void four_profiles_have_identical_key_sets() throws Exception {
    Set<String> base = flatten(load("application.yml"));
    for (String profile : List.of("application-dev.yml", "application-test.yml", "application-prod.yml")) {
        assertThat(flatten(load(profile))).as(profile).isEqualTo(base);
    }
}

@Test
void governance_switches_default_to_false() {
    for (String profile : PROFILES) {
        Map<String, Object> flat = flatten(load(profile));
        assertThat(defaultValue(flat, "egon.cola.component.tianshu.enabled", profile)).isEqualTo("false");
        assertThat(defaultValue(flat, "egon.cola.component.yuheng.openapi.enabled", profile)).isEqualTo("false");
    }
}

@Test
void brand_registered_in_every_tables_map() throws Exception {
    assertThat(flatten(load("egon-mybatis-plus-sharding.yml")))
        .containsKey("egon.cola.component.mybatis-plus.sharding.tables.brand");
}
```

- Verification contribution: `REQ-004`/`TEST-013` 的静态半部（绑定半部由启动期 `@Pattern` 与部署后验证承担，本迭代不启动应用）。
- After this file: 配置防漂移闸门就位。

- Validation working directory: `/Users/mario/SelfProject/enjoyshop/enjoyshop-service/enjoyshop-service-goods`
- Verification command: `./mvnw -q -Dtest=ProfileKeyParityTest test`
- Expected result: 3 个用例通过，退出码 0。
- Failure returns to: File 1-5（对应 YAML 的键/缺省/brand 条目）。
- Completion criteria: 四份 profile 键集合逐键相等；五处治理开关缺省 false；brand 在全部 `tables` 映射中。
- Rollback: revert 六文件。
- Commit paths: `enjoyshop-service/enjoyshop-service-goods/src/main/resources/egon-mybatis-plus-sharding.yml`, `enjoyshop-service/enjoyshop-service-goods/src/main/resources/application.yml`, `enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-dev.yml`, `enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-test.yml`, `enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-prod.yml`, `enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/start/config/ProfileKeyParityTest.java`
- Commit: `feat(goods): 多环境配置键收口与 brand 分片注册`

### Step 11 — 全量回归与收尾（OpenAPI 结构断言、依赖核查、全模块测试）

- Requirements: `REQ-021`, `REQ-022`
- Dependencies: `Step 1` 至 `Step 10`
- Baseline state: 全部功能 Step 已提交；`BrandController` 注解就位；生成 verifier 与新增测试共存。
- Observable outcome: `BrandOpenApiContractTest` 通过（五个 `operationId`、参数必填性、401 仅写契约）；`./mvnw -q test` 整工程通过（含 `LightPersistenceArchitectureTest`/`ArchetypeContractConvergenceTest`）；`git diff 02cda6b -- '**/pom.xml'` 无新增 `<dependency>`（除生成骨架自带）。
- End state: `REQ-022` 的全部 `TEST-*` 有执行载体或明确边界；仓库可交付评审。
- Test-first gate: `Not applicable` — 本 Step 是验证性收口：OpenAPI 结构在 Step 9 已实现，本测试是其断言载体；全量回归与依赖核查是闸门而非新行为。
- Manual Checks: `MC-ARCH-001`, `MC-DEP-001`, `MC-NAME-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001`
- Literal Rules: `Rule 11`
- Ordered files:

#### File 1 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/goods/BrandOpenApiContractTest.java`

- Purpose: `TEST-014` 的单元级载体：经 MockMvc 读取 springdoc 生成的 OAS（`@WebMvcTest` 切片，不依赖网络与玉衡发布键——`DEC-111` 选 A）。
- Symbols: `five_operation_ids_match_spec`、`required_parameters_match_contract`、`write_operations_declare_401_read_do_not`。
- Repository evidence: springdoc 与 MockMvc 兼容（springdoc 官方支持）；`EgonOperationCustomizer` 空白 operationId 拒绝（`EVD-023`）为启动期反证。
- Dependencies and consumers: 定制后的 `BrandController` 注解。
- Why now: 注解事实源在 Step 9 完成，此处收口文档结构断言。
- Contract/signature changes: 无生产契约变化。
- Input/output and state mapping: `GET /v3/api-docs` JSON：`paths./api/v1/brands.get.operationId==listBrands` 等；`delete` 的 `version` 参数 `required=true`；`post` 含 201/409 与 401 声明而 `get` 不含 401。
- Error and edge behavior: 若切片未装配 springdoc（版本行为差异），回退为直接构造 `SpringDocConfigProperties`+`AbstractOpenApiResource` 的断言——两条路线都在单元层、均不依赖网络；任一路线失败即停止并回查注解。
- Standards impact: `MC-TEST-001` — 文档契约断言；`MC-SCOPE-001` — 不新增 UI 依赖。
- Literal rule enforcement: `Rule 11` — 断言对象是所选形态的 adapter 契约面。
- Implementation pseudocode:

```java
@WebMvcTest(controllers = BrandController.class)
class BrandOpenApiContractTest {
    @Autowired MockMvc mockMvc;

    @Test
    void five_operation_ids_match_spec() throws Exception {
        String docs = mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        assertThat(docs).contains("\"listBrands\"", "\"getBrand\"", "\"createBrand\"",
            "\"updateBrand\"", "\"deleteBrand\"");
    }

    @Test
    void write_operations_declare_401_read_do_not() throws Exception {
        String docs = mockMvc.perform(get("/v3/api-docs")).andReturn().getResponse().getContentAsString();
        JsonNode paths = Json.read(docs).at("/paths");
        assertThat(paths.at("/api/v1/brands/post/responses/401")).isNotNull();
        assertThat(paths.at("/api/v1/brands/delete/responses/401")).isNotNull();
        assertThat(paths.at("/api/v1/brands/get/responses/401")).isMissing();
    }
}
```

- Verification contribution: `TEST-014`；`API-GATE-007` 的单元级证据。
- After this file: 文档契约有机器断言。

- Validation working directory: `/Users/mario/SelfProject/enjoyshop/enjoyshop-service/enjoyshop-service-goods`
- Verification command: `./mvnw -q test && cd /Users/mario/SelfProject/enjoyshop && git diff 02cda6b --stat -- '*.pom.xml' 'pom.xml' 'enjoyshop-service/enjoyshop-service-goods/pom.xml' 'enjoyshop-service/enjoyshop-service-goods/*/pom.xml' | tail -5 && git diff 02cda6b -- 'enjoyshop-service/enjoyshop-service-goods/*/pom.xml' | grep -c "+.*<artifactId>" || true`
- Expected result: 整工程测试通过（含生成 verifier 与 `BrandQuerySqlShapeTest`/`BrandManageImplTest`/`BrandAdapterConverterTest`/`BaseExceptionHandlerTest`/`BrandControllerWebTest`/`ProfileKeyParityTest`/`BrandOpenApiContractTest`/`GoodsErrorStatusTest`）；POM diff 中新增 `<artifactId>` 行数为 0（生成骨架的依赖在 Step 1 提交中，属于 archetype 原文）。
- Failure returns to: 对应功能 Step（测试归属模块）；依赖新增 → `MC-DEP-001` 阻断并回退该改动。
- Completion criteria: `REQ-022` 的 §14 用例全部有载体或边界标注；无 `CONFLICT`（`egon-codegen.sh check` 退出码 0，自定义文件为声明的定制）；`REQ-021` 核查通过。
- Rollback: revert 本测试文件（不影响功能提交）。
- Commit paths: `enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/goods/BrandOpenApiContractTest.java`
- Commit: `test(goods): OpenAPI 结构断言与全量回归收口`

## 8. Test, Validation, and Quality Gates

| Gate/order | Working directory | Command or method | Scope | Expected result | Failure returns to | Requirements/runtime boundary |
| --- | --- | --- | --- | --- | --- | --- |
| Step 1 校验 | enjoyshop 根 / 生成工程 | `mvn -N validate`；`./mvnw -q validate`；`grep` 确认自带信封类零残留；verifier 计数为 9/9/9/6 | reactor + 单模块八包 + 删除与基线更新 | 双退出码 0；parent 链不变；`grep` 零命中；`grep -c` 输出 4 | Step 1 File 1/1b/1c | `REQ-001`/`REQ-003`；静态 |
| Step 2 校验和 | 生成工程根 | `shasum -a 256` + Manifest 断言（§7 Step 2 命令） | DDL + Manifest | 断言通过、脚本数 2 | Step 2 File 1/2 | `REQ-016`；静态 |
| Step 3-9 各 Step RED/GREEN | 生成工程根 | §7 各 Step 的 `./mvnw -q -Dtest=测试类名 test` | 对应模块 | 先声明的 RED 失败 → GREEN 退出码 0 | 各 Step 首文件 | §4.2 表；静态/模块 |
| Step 4 生成闸门 | enjoyshop 根 | `egon-codegen.sh templates/plan/apply/check` | 目录产物 | plan 22 ADD 无 CONFLICT；apply 退出码 0；check 退出码 0 | Step 4 File 1 | `REQ-003`/`REQ-021`；静态 |
| Step 10 配置奇偶 | 生成工程根 | `./mvnw -q -Dtest=ProfileKeyParityTest test` | 四 profile + sharding yml | 3 用例通过 | Step 10 | `REQ-004`；静态 |
| Step 11 全量回归 | 生成工程根 | `./mvnw -q test` | 整工程（含生成 verifier） | 全部通过，退出码 0 | 对应功能 Step | `REQ-022`；模块 |
| 依赖核查 | enjoyshop 根 | `git diff 02cda6b -- '*/pom.xml' 'pom.xml'` 人工审阅 + `cd 生成工程 && ./mvnw -q dependency:tree \| grep -i "enjoyshop\|egon-cola"` | POM | 零新增依赖；无自写 `<version>` | `MC-DEP-001` 阻断 | `REQ-006`/`REQ-021`；静态 |
| 手动/部署后（本迭代不执行） | 部署环境 | 真实 PostgreSQL 上启动（Runner 执行 V 脚本、`ddl_history` 落行）、`EXPLAIN` 验证索引、`/v3/api-docs` 内容、天枢接入 | 运行时 | 部署后清单逐项勾选 | 部署负责人 | `RISK-003`/`RISK-004`/`TEST-012` 动态半部；用户控制 |

无任何闸门需要新建 `scripts/work/` 临时脚本：全部闸门由既有命令（`mvn`/`./mvnw`/`egon-codegen.sh`）与随仓库提交的 JUnit 测试覆盖。

## 9. Migration, Compatibility, Rollout, and Rollback

**迁移**：一次 Schema 变更 = `V20260923_001__initialize_goods_schema.sql`（Step 2）+ Manifest 追加条目（sha256 与字节一致）。不改、不重排、不重哈希 `20260913_001` 及其条目；`db/migration/sharding/` 归档不动。脚本经 `EgonColaPostgreDdlRunner` 在真实库启动时执行（schema advisory lock、脚本与 `ddl_history` 同事务提交）；brand 为 `SINGLE` 主数据表，SHARD 目标对该脚本直通（`PLAN-CLAR-008`）。多物理目标非全局事务：某目标失败不回滚已提交目标，恢复按 CQE 契约"新连接核验 + 新增下一修正脚本"，禁止 auto-DROP 或改校验和。

**兼容性**：全部为新增面。显式保持不变：生成工程其他路由、`ResultRecord`/`PageResultRecord` 字段集合、`EgonModel` 语义、`-facade` 零新增 RPC、样例域代码（除过滤器一行缺省值）。可见的载荷收窄：`BrandVO` 不含 `createTime`/`updateTime`（`PLAN-CLAR-011`）；如需恢复，须新 Spec 修订（侵入域模型与持久化转换器两个生成文件并重新发布契约），本期不实施。

**发布顺序**：Step 1（骨架）→ Step 2（DDL+Manifest）→ Step 3（错误边界）→ Step 4（codegen plan/apply + journal）→ Step 5-9（业务层逐提交）→ Step 10（配置）→ Step 11（回归）。任何一步失败只回退该步提交。

**回滚**：代码层按"一个 Step 一个提交"逐提交 `git revert`；`brand` 表不提供 down DDL——应用回滚到"不引用 brand 的提交"即脱离该表，历史行保留（软删除语义），如需移除表须以新的受管脚本显式决策。配置回滚 = revert Step 10 提交。生成器层：`.egon/` 状态随提交序列自然一致；删除该目录会使后续再生成把同名文件判为 `CONFLICT`（恢复手段是 `recover` 或人工核对），因此 `.gitignore` 保护其只存在于工作区。

**部署后验证清单**（本迭代不执行，移交互）：真实 PostgreSQL 的 DDL 执行与 `ddl_history`；`EXPLAIN` 证明 `listBrandPage` 走 `idx_brand_tenant_seq_id`；真实并发下唯一索引竞争与版本冲突；`/v3/api-docs` 实际输出与 `TEST-014` 一致；天枢注册（若启用）。

## 10. Requirement-to-Step Traceability Matrix

| Requirement | Effective Spec section | Steps | Files | Tests/gates | Completion evidence |
| --- | --- | --- | --- | --- | --- |
| `REQ-001` | §4/§8.2 | Step 1 | 根 `pom.xml` | `mvn -N validate` | 退出码 0 + parent 链核对 |
| `REQ-002` | §4/§8.2/§17 | Step 1 | 两个二级 POM | 目录核对 | §8.2 树一致；四目录不存在 |
| `REQ-003` | §4/§6.1/§8.2 | Step 1, Step 4 | 生成工程 + 22 产物 | `./mvnw validate/compile`、plan JSON、`grep` 确认自带信封类零残留 | 单模块 + 八个包目录 + verifier 通过 |
| `REQ-004` | §4/§7.1/§15 | Step 10 | 五份 YAML + 奇偶测试 | `ProfileKeyParityTest` | 键集合相等、缺省 false |
| `REQ-005` | §4/§7.0 | Step 1 | — | `TEST-015` 树核对 | 无空模块；`GoodsErrorStatus` 在 `-common` |
| `REQ-006` | §4/§6.1/§9.0 | Step 1, Step 11 | — | `dependency:tree` 审阅 | 零自写 `<version>`；`-facade` 未改动 |
| `REQ-007` | §4/§9.2.3/§10/§11 | Step 2, Step 4, Step 5, Step 6, Step 7, Step 9 | DDL、CQE、controller | `BrandControllerWebTest`（201/Location/version0）、`BrandManageImplTest` | 契约断言通过 |
| `REQ-008` | §4/§9.2.4/§10.3 | Step 4, Step 6, Step 7, Step 9 | `UpdateBrandCommand`/`updateBrand` | `BrandManageImplTest` + 切片 | 409 610104 与版本递增断言 |
| `REQ-009` | §4/§9.2.5/§10.6 | Step 4, Step 6, Step 9 | `deleteVersionedById` 链 | `BrandControllerWebTest` | 200 true / 重复 404 |
| `REQ-010` | §4/§9.2.2/§15 | Step 5, Step 6, Step 8, Step 9 | `selectActiveById` 链 + advice | `BaseExceptionHandlerTest`/切片 | 404 同载荷不泄露 |
| `REQ-011` | §4/§9.2.1 | Step 5, Step 6, Step 9 | `listBrandPage`/`listBrands` | `BrandQuerySqlShapeTest`/切片 | `records` 非 null、顺序稳定 |
| `REQ-012` | §4/§9.2.1 | Step 5, Step 7, Step 9 | 三过滤 + 转换器归一 | `BrandQuerySqlShapeTest` | 转义与谓词形态一致 |
| `REQ-013` | §4/§9.2.1/§10.3 | Step 6, Step 7, Step 9 | `PageQuery` 模式判定 | `BrandControllerWebTest` | 500 上限回显、越界空页 |
| `REQ-014` | §4/§9.2.1/§7.3.3 | Step 5, Step 6, Step 9 | `ORDER BY seq DESC, id DESC` + total | `BrandQuerySqlShapeTest` | 排序与 count 形态锁定 |
| `REQ-015` | §4/§10.2/§11.2.1 | Step 2, Step 4 | DDL + `BrandPO` | `TEST-012` 静态半部 + verifier | 逐列一致（含 `PLAN-CLAR-001`） |
| `REQ-016` | §4/§11.2.1/§16 | Step 2 | Manifest | 校验和断言 | sha256 一致、旧条目未变 |
| `REQ-017` | §4/§11.2.1/§7.3.4 | Step 2, Step 5, Step 6 | 部分唯一索引 + 查重 | `BrandManageImplTest`/`BrandQuerySqlShapeTest` | 有效行唯一、软删释放 |
| `REQ-018` | §4/§9.2 参数表/§10.3.1 | Step 5, Step 6, Step 7, Step 8, Step 9 | 各交接文件 | `TEST-003`/`008` 负例 | 逐层负例 400 + `fieldErrors` |
| `REQ-019` | §4/§9.2 映射表/§15 | Step 3, Step 8, Step 9 | 错误边界 + advice + 守卫 | `BaseExceptionHandlerTest` + `BrandManageImplTest`/切片 401 | 全错误行映射、脱敏、traceId |
| `REQ-020` | §4/§9.0 | Step 4 | 生成配置 `events.enabled=false` | 产物清单核对 | 零事件产物与依赖 |
| `REQ-021` | §4/§6.1/§7.0 | Step 1, Step 4, Step 11 | POM 全集 | POM diff + `dependency:tree` | 零新增依赖（FreeMarker 仅工具侧） |
| `REQ-022` | §4/§14 | Step 3, Step 5, Step 6, Step 7, Step 8, Step 9, Step 10, Step 11 | 全部测试文件 | `./mvnw -q test` | §14 的 `TEST-001`-`016` 载体齐备 |

## 11. Risks, Blockers, and User Decisions

| ID | Risk or decision | Impacted Steps/files | Evidence | Owner | Status/action |
| --- | --- | --- | --- | --- | --- |
| `BLOCK-001` | `egon-cola-archetype-web` 强制要求四个 `evaluationFacade*` 对端契约坐标，且它们是生成物的**硬组成部分**（不只是生成参数）：`archetype-metadata.xml:13-16` 将其声明为无默认值的 requiredProperty；`archetype-resources/pom.xml:31-34,74-76` 写入属性与 dependencyManagement；`__rootArtifactId__-infrastructure/pom.xml:37-40` 声明**真实编译期依赖**；`infrastructure/client/evaluation/**` 8 个文件 `import ${evaluationFacadePackage}.*`；`ArchetypeContractConvergenceTest.java:101,129` 与 `OrganizationApplicationTest.java:7,49-50` 两个自带测试断言它们存在。因此"本迭代暂不使用外部 facade、不引入"在 web 形态下不可实现（传空值得 `import .course.CourseFacade` 加空 groupId 依赖；传不存在的坐标则 infrastructure 编译失败），而 create-new-module 技能明文 "Never invent a peer facade or default it to a `source-projects` sample"，禁止我自行采用样例坐标 | Step 1 File 1 的 archetype 选择（**不再是**四个 `-D` 参数） | Spec `EVD-026`（解包 `egon-cola-archetype-web-5.4.1.jar` 逐文件核对）；`archetype-selection.md` 的 "Web has no default for the peer contract. Require all four" 与 "Do not point those properties at `top.egon.internal.archetype.source`… unless the user explicitly names that sample contract"；Spec `EVD-027`（`egon-cola-archetype-light-5.4.1.jar` 的 requiredProperties 只含 `gitignore`） | User（已裁定） | **Closed（2026-09-24）** — 用户明确表示"目前应该还用不到外部 facade，不引入就行了"，本 Plan 据此把 `DEC-101` 由 A 改选 B（light），其四项连带后果由 Spec `DEC-115`（删除自带 `ResponseWrapperHandler`／`GlobalExceptionHandler`／`ApiResponse`）、`DEC-116`（新增 MDC 租户过滤器）、`DEC-117`（verifier 计数基线 8/8/8/5→9/9/9/6）、`DEC-118`（样例域 GraphQL／MQ／Redis／幂等／proto 记 Context-only）承担。本 Plan 随之转 `Ready`，Step 1 File 1 的生成命令不再含任何对端 facade 参数 |
| `RISK-P1` | 生成工程自带样例域（teaching/Organization*）与品牌切片共存，样例 advice/样例数据可能干扰阅读与测试 | Step 8/9（包级限定规避 advice 歧义） | create-new-module"样例替换属后续 Spec" | mario | 已登记（不阻塞）— 本迭代隔离不修改；样例清理另立 Spec |
| `RISK-P2` | `BrandVO` 缺审计时间字段（`PLAN-CLAR-011`），与 Spec §9.2 载荷示例存在两字段差异 | Step 6/7；§9 恢复路径 | 生成器 `policyFields` 跳过基列；archetype `GradeDetailVO` 先例 | mario | 已登记（不阻塞）— 前端若需要时间字段，以新 Spec 修订；本 Plan 保留 `version` 保证功能契约 |
| `RISK-P3` | 定制过的生成文件（`BrandResult`、`BrandResultConverter`、`BrandDAO`、`BrandDAO.xml`、`BrandRepository`、`BrandManage`、`BrandManageImpl`、`BrandDomainService`、`BrandDomainServiceImpl`、`BrandController`、`BrandDomainQuery` 无改、`BrandPersistenceConverter` 无改）在未来再生成时报 `CONFLICT`，须人工合并 | Step 4/5/6/9 | backend-code-generation 冲突模型；Spec §8.3 已声明 | mario | 已登记（不阻塞）— 运维模型：CONFLICT 即停止，按文件比对合并；不再扩大定制面 |
| `RISK-P4` | 全量模式 500 上限使超 500 行的品牌目录静默截断（Spec `RISK-004` 承接） | Step 9 `listBrands`；`TEST-007` | `PageQuery.MAX_PAGE_SIZE=500` | mario | 已登记（不阻塞）— `page.total/hasNext` 已暴露；如需无界分页另立契约决策 |
| `RISK-P5` | 雪花 id JSON 整数在浏览器 53 位精度后失真（Spec `RISK-005` 承接） | Step 7 `BrandVO.id` | `RISK-005` 原文 | mario | 已登记（不阻塞）— 前端按字符串透传比较；全局字符串序列化需后续 Spec |

无其他未决决策：Spec §5.4 十四项已全部由用户选定；`DEC-107`/`DEC-111` 的"默认关闭"口径已由 Step 10 落实。

## 12. Review and Acceptance

### 12.1 Original requirement fidelity

用户 8 条原始需求逐条落位：条目 1/2（父工程与五个二级模块）→ Step 1 的外层 reactor + 两个二级聚合 + 生成工程，未建模块均有 `DEC-102` 至 `DEC-106` 的已选决策且在交付清单显式声明；条目 3（tianshu 注册中心）→ Step 10 的"键写全、默认关闭"（`DEC-107` 选 A 的可交付口径）；条目 4/5（common、service_api）→ `DEC-106`/`DEC-105` 选 A：共享能力在 `-common` 层与 Components BOM、契约载体即单模块内的 `facade/` 包（`DEC-101` 改选 light 后不再是 Maven 模块；本迭代零 RPC 操作）；条目 6（enjoyshop_service_goods）→ Step 1 生成 + `REQ-021` 依赖纪律；条目 7（品牌 8 行为）→ `API-001` 至 `API-005`（`DEC-112` 映射）经 Step 2/4/5/6/7/9 完整实现；条目 8（BaseExceptionHandler）→ Step 8，保留用户命名与 `DEC-109` 信封。无静默削弱：全部偏差以 `PLAN-CLAR-001` 至 `PLAN-CLAR-016` 与已关闭的 `BLOCK-001` 显式登记。

### 12.2 Spec consistency

本 Plan 未改变 Spec 的架构、契约、字段语义、状态规则、Schema 设计意图、错误语义、事务/幂等口径、迁移与回滚边界。执行了两项证据驱动的收窄并全程显式：`PLAN-CLAR-001`（继承列 SQL 类型按 `EgonModel` 与生成器硬校验修正，Spec §11.2.1 表的三处类型笔误不予采用——不改采用即无法通过生成器 `BASE_FIELD_TYPE` 校验）与 `PLAN-CLAR-011`（`BrandVO` 不含审计时间两字段，乐观锁 `version` 完整保留）。§4.5 必要性审计未发现 fetch-then-forward 接口或投机元素；生成的 `selectByQuery`/`page()` 链不删除（目录契约）但不在品牌链路使用，已在 `PLAN-CLAR-002`/`004` 说明。

### 12.3 Repository executability

全部路径、符号、命令已对当前基线核实：`egon-cola-archetype-light:5.4.1`（`requiredProperties` 只含 `gitignore`）与 code-generator 5.4.1 在 `/Users/mario/maven/repository`；另解包 `egon-cola-archetype-light-5.4.1.jar` 与 `egon-cola-archetype-web-5.4.1.jar` 核对了自带信封设施、MDC 过滤器缺失与 verifier 硬编码计数（Spec `EVD-026` 至 `EVD-029`）；生成器模板、`GenerationScopeValidator` 命名/校验规则、`ProjectLayoutStrategy` 路径规则、`EgonModel`/`ResultRecord`/`PageQuery`/`PageSlice`/`CommonException(ErrorStatus)`/`ErrorStatus` API、`SchoolClassRepository`/`GradeController`/过滤器/advice 先例、生成工程四份 profile 与 sharding yml 的键形状，均为本次会话直读源码所得。每个 Step 有唯一语义结果、互不重叠的写作用域、验证工作目录、精确命令、预期退出码与失败回点；每 Step 恰好一个提交且 Commit paths 与写作用域一致；Step 依赖顺序保证中间提交可编译（例外已在 §4.4 声明）。`BLOCK-001` 已关闭，Step 1 可直接执行，故状态为 `Ready`。

### 12.4 Test and release completeness

RED→GREEN 顺序已在 §4.2/§7 固化（6 个 Required 闸门 + 5 个证据-backed Not applicable）；单元/切片/配置/契约/架构 verifier 分层与 Spec §14 一致；迁移安全（不改历史脚本、单版本单条目、SHARD 直通）、回滚（逐提交 revert + 无 down DDL 的 forward-fix 边界）、可观测（advice 日志 + traceId + 部署后清单）与验证边界（本迭代不连库、不启动应用、不接天枢/玉衡）均已声明。验证命令不声明为已执行的证明——它们是执行阶段的未来指令。

### 12.5 Blocking Manual Check

| Check ID | Applicability | Status | Evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- |
| `MC-ARCH-001` | Applicable | PASS | §4.7 形态行 + Step 1（archetype-light 生成 + 删除自带信封类 + verifier 基线更新）+ Step 4（light profile 目录产物）+ 全部文件位于单模块对应包 | 唯一形态为 `egon-cola-archetype-light`（`DEC-101` 选项 B）；无 `biz.*`、无自创层；verifier（`LightPersistenceArchitectureTest`）在 Step 11 回归 | None |
| `MC-REUSE-001` | Applicable | PASS | §4.7 复用账本 8 行（信封/异常/持久化模板/生成器/转换/校验/工具/文档全部复用） | 零自研替代；`CommonException(ErrorStatus)` 复用避免了新异常类型 | None |
| `MC-DEP-001` | Applicable | PASS | §4.7 账本 + §8 依赖核查闸门；FreeMarker 仅生成器工具侧（预批准）；Step 11 POM diff 核查 | 业务工程零新增依赖；light 形态不存在对端 facade 依赖（`BLOCK-001` 已由改选形态消解）；样例域自带的 GraphQL／MQ／Redis／proto 依赖来自生成骨架而非本迭代新增（`DEC-118`、`RISK-015`） | None |
| `MC-NAME-001` | Applicable | PASS | §4.8 Rule 1 行；§7 全部类型的语义后缀清单；Spec §10.1 拒绝清单未被违反（`BrandPageQuery` 等为目录产物名，`PLAN-CLAR-002`） | 全部新类型以 PO/DAO/VO/Request/Query/Command/Result/Repository/Manage/Service/Converter/Filter/Handler/Error/Mapper 结尾；无 `Data/Info/Param/Bean` | None |
| `MC-VALID-001` | Applicable | PASS | §4.8 Rule 2 行 + Step 5/6/7/9 的五个层间交接（Controller `@Valid`+类级 `@Validated`、Manage 接口 `@Valid`、Domain 端口 `@Validated`、Repository 参数级约束、区间跨字段 if+`CommonException`）；无电话语义 | 每个交接有名有注解有负例；`ValidationUtils` 仅通用场景、本设计未承载业务校验 | None |
| `MC-MODEL-001` | Applicable | PASS | §4.8 Rule 3 行；生成载体 class+完整 Lombok 组合（模板实测）；adapter 六 record 为不可变载体且由框架同构理由支撑；`BrandPO` 由模板含 `@EqualsAndHashCode(callSuper=true)`+`@SuperBuilder` | record 仅用于不可变传输/投影载体，普通实体未因简单用 record（`PLAN-CLAR-003`） | None |
| `MC-CONVERT-001` | Applicable | PASS | Step 4 生成的 5 个转换器（`BaseConverter`×1 + `BaseForwardConverter`×4）+ Step 7 `BrandAdapterConverter extends BaseForwardConverter<BrandResult,BrandVO>`；`unmappedTargetPolicy=ERROR` | 全部转换经 MapStruct + Egon 基类；无 BeanUtils/反射/JSON 往返；多方向单接口的 Java 泛型限制已按 `PLAN-CLAR-004` 处理 | None |
| `MC-LOG-001` | Applicable | PASS | 生成模板统一 `@Slf4j`；Step 6/8 的 `log.warn/log.error` 参数化日志点（不落请求体与 SQL 明文） | 业务类日志语义与 Spec §7.3.5 一致 | None |
| `MC-BEAN-001` | Applicable | PASS | 生成 Bean 名实测（`brandController`/`brandManage`/`brandDomainService`/`brandRepository` 等）；自定义 Bean `baseExceptionHandler`；`@RequiredArgsConstructor` + 逐字段 `@Qualifier`；`lombok.config` copyable 已生成 | Bean 命名/注入/传播全部显式（Spec 字面 `brandManageImpl` 的差异按 `PLAN-CLAR-002` 以生成器为准） | None |
| `MC-UTIL-001` | Applicable | PASS | 仅 JDK `String.replace/trim/isBlank` + commons-lang3 `StringUtils.normalizeSpace`（adapter POM 传递依赖，Rule 5 白名单） | 无自研 `*Utils`、无白名单外工具、无 Tika | None |
| `MC-JSON-001` | Applicable | PASS | Boot Jackson 全局（生成 yml `jackson.*`、`non_null`）；`ResultRecord`/`PageResultRecord` 自带注解；`GoodsErrorStatus` int 码 + `name()`；record 载体默认序列化；无 Gson/Fastjson；`@EnumValue`/`@JsonValue` 不适用（无持久化枚举列、VO 无枚举） | JSON 单栈且与 §9 载荷形状一致 | None |
| `MC-TIME-001` | Applicable | PASS | 时间仅存在于 `EgonModel`（`Instant`×2、`LocalDateTime deletedAt`）与 DDL 的 `TIMESTAMP(6) WITH/WITHOUT TIME ZONE`；软删表达式 UTC；VO 无时间字段（`PLAN-CLAR-011`）；无 `java.util.Date/Calendar`（verifier 静态禁止） | 全部时间建模为 `java.time` 且边界语义显式 | None |
| `MC-CONFIG-001` | Applicable | PASS | Step 10 五份 YAML 同树修改 + `ProfileKeyParityTest`（键集合相等 + 五处治理开关缺省 false + brand 全注册）；`@ConfigurationProperties` 校验由 MP-SDJ starter 既有绑定承担 | 键结构一致、值可环境不同；`DEC-107`/`DEC-111` 的默认关闭口径落地 | None |
| `MC-PATTERN-001` | Applicable | PASS | Spec §13 判 Simple（单表、无状态机、无分发）；§4.7 模式行；唯一模式为框架强制的 `EgonColaRepository` 模板扩展 | 复杂分支不存在；未为 Simple 逻辑造仪式模式 | None |
| `MC-SCOPE-001` | Applicable | PASS | §5 变更树完整且与 Spec §8.2 对应；四候选工程不建；样例域仅一行声明内修改；`.egon/` 与 `.DS_Store` 不入库；无任何越界重构 | 触达范围与有效 Spec 一致，例外（`PLAN-CLAR-005`/`011`）均显式 | None |
| `MC-TEST-001` | Applicable | PASS | §8 闸门表 + §7 每个 Step 的 RED/GREEN 与命令；`TEST-001`-`TEST-016` 全部有载体或边界标注（`TEST-012` 动态半部为部署后） | 每个适用标准由具体测试/静态闸门证明；无"以后再补"项 | None |
| `MC-BLOCKER-001` | Applicable | PASS | 其余 16 项全部 PASS/N-A；`BLOCK-001` 已于 2026-09-24 由用户裁定关闭（改选 light），其四项连带后果由 Spec `DEC-115` 至 `DEC-118` 与本 Plan 的 `PLAN-CLAR-013` 至 `016` 承担；Plan 状态 `Ready` | 无未知或静默异常：原阻塞项是用户输入门槛（archetype 硬约束），已由形态改选消除而非绕过；删除自带信封类与更新 verifier 基线两处生成物改动均在 §5 树、Step 1 File 1b/1c 与 Commit paths 中显式声明 | None |

### 12.6 Final verdict

PASS — Ready for user review

本 Plan 在结构、路径、符号、命令与测试闸门上已完整可执行，16 项 Manual Check 全部关闭。`BLOCK-001` 已于 2026-09-24 关闭：web archetype 的四个 `evaluationFacade*` 对端契约坐标不只是生成参数，而是生成物的硬组成部分（infrastructure 真实编译期依赖 加 8 个样例文件 加两个 verifier 断言），因此"暂不使用外部 facade"在该形态下不可实现，而 create-new-module 技能禁止默认采用样例值。用户据此把 `DEC-101` 由 A 改选 B（light，无对端 facade 属性），四项连带后果由 Spec `DEC-115`（删除三个自带信封类）、`DEC-116`（新增 MDC 租户过滤器）、`DEC-117`（verifier 计数基线 8/8/8/5→9/9/9/6）、`DEC-118`（样例域 GraphQL／MQ／Redis／幂等／proto 记 Context-only）承担，本 Plan 以 `PLAN-CLAR-013` 至 `016` 落到 Step 1 与 Step 9／10。`validate_plan.py --strict` 的唯一剩余诊断是 `Status Ready cannot contain unresolved placeholder: <modules>` —— 这是校验器正则的**误报**：该模式命中的是 Step 1 File 2 与 File 3 的 Maven POM XML 片段中的 `<modules>`／`<module>` 元素，而它们是 `packaging=pom` 聚合器的**强制语法**，不是模板占位符。若为通过正则而删除该元素，外层 reactor 将不再聚合二级目录，`REQ-001` 的验收（"modules 清单只列业务工程根"）直接失败。本 Plan 选择保留正确的 POM 语法并显式记录该诊断，而不是把构建清单写错。状态为 `Ready`，可按 §7 顺序执行。本 Plan 不声称任何代码、数据库、服务或运行时验证已完成；未生成任何工程、未写任何业务代码。

## Java / CQE contract review

按 `references/egon-java-cqe-contract.md` 逐项核对并映射既有 MC ID：POJO/record/枚举（`MC-MODEL-001`/`MC-JSON-001`：生成载体为 class+完整注解组合、adapter 不可变 record 载体、`GoodsErrorStatus` 实现 `ErrorStatus` 以 int 码输出无 ordinal）；组件与 MP 复用（`MC-REUSE-001`/`MC-DEP-001`：`EgonModel`/`EgonColaMapper`/`EgonColaRepository`/MP-SDJ starter 全复用，域端口不携带 PO 泛型，`version` 经 `PLAN-CLAR-011` 贯通）；注解驱动校验（`MC-VALID-001`：五个层间交接逐条、`@Valid`/`@Validated`/参数级约束/跨字段复核、无 `ValidationUtils` 业务化）；业务唯一键与 DDL（`MC-MODEL-001`/`MC-TEST-001`：`(tenant_id, name)` + `WHERE deleted_at IS NULL` 部分唯一索引——NULL 语义下有效行唯一的正确形态、重复软删除释放名称由 `TEST-006` 口径用例覆盖、单一 SQL 版本 + Manifest 校验和、`SINGLE` 单目标失败可判定）；CQE 投递（`MC-ARCH-001`/`MC-TEST-001`：写=Command、读=Query、零 Event（`DEC-003`），生成配置 `events.enabled=false`，无 outbox/MQ 依赖即无伪造投递）；DDD 形态来源（`MC-ARCH-001`：全部类型落位于 archetype 生成的 light 单模块包树与端口方向，域实现经 Repository 访问数据、不触 DAO）。验证限制：以上均为静态与源码级证据；真实 PostgreSQL、并发竞争与注册链路属部署后验证。

## Backend generation and dependency evidence

依据 `references/backend-code-generation.md`：生成器为已验证的 `/Users/mario/SelfProject/Egon-COLA/scripts/egon-codegen.sh`（`templates/plan/check/apply/recover`，FreeMarker 2.3.35 工具依赖预批准），profile=`web`，`configVersion=1`，输入为 `input.mode=manifest`（`db/egon-mp/repository-manifest.json`，含既有 `20260913_001` 与新增 `20260923_001`），`logicalTables=["brand"]`，artifacts 为 backend-crud 全链 15 项（web 含 controller）。预期产物 22 个文件（`po` 1、`dao` 1、`mapper-xml` 1、`repo` 1、`domain-model` 1、`domain-query` 1、`domain-service` 1、`domain-impl` 1、`command` 3、`query` 2、`result` 1、`converter` 5、`manage` 1、`manage-impl` 1、`controller` 1），全部标 `GENERATED`，其 Plan 伪代码即生成器配置与 `plan`/`apply` 命令（§7 Step 4），无任何手写类体。自定义/定制文件与生成物的边界：模型写 SQL、Manifest、生成器配置、journal、common 错误边界、adapter 契约层、守卫/查重/列表编排与控制器定制（目录产物未覆盖的业务行为），并接受被定制生成文件未来 `CONFLICT` 的运维模型（§11 `RISK-P3`）。执行前置：`EGON_CODEGEN_CLASSPATH` 按 §6.2 构建，缺失即 `BLOCKED_TOOLING`（不下载依赖、不启动应用）；`plan` 的 JSON 摘要与 22 文件清单为 apply 前的人工复核点，`v1` 计划或指纹漂移即重开 `plan`。apply 成功后向 `docs/egon/codegen/ddl-consumption-log.md` 追加 journal（Step 4 File 8）。依赖证据：业务工程零新增依赖（`REQ-021`）；archetype 生成与生成器运行不改变业务 POM 依赖集。
