# 商品管理实施 Plan：聚合、生命周期与操作权限

| Field | Value |
| --- | --- |
| Document | `2026-10-09-11-54-product-management-implementation.md` |
| Template Version | `4` |
| Status | Review |
| Created | 2026-10-09 11:54 Asia/Shanghai |
| Updated | 2026-10-09 12:08 Asia/Shanghai |
| Owner | mario |
| Repository | enjoyshop |
| Scope | enjoyshop-service/enjoyshop-service-goods 商品管理后端；Spec关系元数据 |
| Source Requirement | 用户调用egon-coding-writing-plan，以本轮商品管理Spec编写计划；原附件需求及已批准DEC-201至207 |
| Baseline Revision | main/c3559b0dc0d743e936f712d97037849a8777dd2b；既有未跟踪商品Spec保留，无其他dirty源码 |
| Implements Spec | [商品管理Spec](../spec/2026-10-09-10-55-product-management-lifecycle.md) |
| Spec Status | Review |
| Spec Revision | 主Spec本轮Updated/DEC-208/§10.4；SHA256 `101b758e2a68b2fba0e8c024bdcb299dff727189cea401702f922421467b7820`；main c3559b0，Review |
| Effective Specs | [商品管理Spec](../spec/2026-10-09-10-55-product-management-lifecycle.md) 全文；[品牌基线Spec](../spec/2026-09-23-17-16-enjoyshop-scaffold-brand-crud.md) §6/10/11/16；[目录上传Spec](../spec/2026-09-28-21-50-distributed-file-catalog-lookups.md) §9/11 与category_brand owner修订 |
| Depends On Plans | None |
| Supersedes | None |
| Superseded By | None |
| Related Plans | None |

## 1. Summary

以当前商品管理Spec为唯一主目标，安排8个顺序验证关卡：单版本DDL及生成骨架、准确输入与模型、字段转换/具名SQL、聚合保存与查询、生命周期、操作授权、安全HTTP/OAS、回归与发布说明。先完成真实事务与权限门，再暴露任何新HTTP。无前端、业务Event、新进程或历史SQL改写。

本轮按用户“单表也需要转换”及“修复一下”修正职责清单：单表持久化、共享Save输入、Result输出均保留必要Converter；聚合通过MapStruct uses复用。generic CLI固定五variant的事实保留，但它不能成为排除必要业务映射或强加另一套Command的理由。主Spec DEC-208与§10.4已明确GEN/CUSTOM责任；本Plan为Review，尚未用户批准为Ready。

## 2. Target Spec and Effective Design

### 2.1 Primary target

商品Spec为Review；用户“按照推荐方案进行即可”确认SINGLE/master_data与Jianshen接入方向，随后显式调用本Plan技能，所以允许编写非Ready计划；没有整份Spec Accepted或本Plan Ready批准。本轮不改业务设计、不执行Step或实际生成。

### 2.2 Effective Spec set

| Role | Spec/link | Status/revision | Effective sections | Why included |
| --- | --- | --- | --- | --- |
| Primary | [商品管理Spec](../spec/2026-10-09-10-55-product-management-lifecycle.md) | Review，本轮最新header | 全文，REQ-001–015，API-201–210，INTERNAL-201–221，DEC-201–207 | 未实现的新商品完整合同 |
| Dependency | [品牌Spec](../spec/2026-09-23-17-16-enjoyshop-scaffold-brand-crud.md) | Accepted，2026-09-24 15:20 CST | §6/10/11/16，在其后Light改选与当前源码生效 | exactLight/Common/MP/旧品牌边界，不重做骨架 |
| Dependency/Amended | [目录上传Spec](../spec/2026-09-28-21-50-distributed-file-catalog-lookups.md) | Accepted，读取当前文档 | §9/11，商品Spec只增category_brand内部ensure writer | 已有目录表、查询、上传不改变 |

### 2.3 Superseded or excluded content

品牌早期Web七模块草案由同文件DEC-101/115–118的Light改选排除，不能照旧REQ-003措辞重建七Maven模块。旧DDL已有数据结构不重建。原目录“关联仅外部writer”在商品保存范围按主Spec Amends增加内部ensure，外部其他目录管理仍保留。教学/user样例没有对应Product替代对象，用户禁止无关清理且Spec明确保留，删除路径为None，不把技能一般示例清理要求扩展到样例域。

## 3. Effective Requirements and Acceptance

主文与前序Spec的REQ编号各自有文档作用域；同REQ-001在商品文是ID，在品牌文是reactor，不可混为一条义务。下面分组记录：主文行为新增，前序已完成义务只保护/回归。每项仍使用原始source ID，不新增业务需求。

| Requirement | Source Spec section | Effective statement | Observable acceptance | Implementation impact |
| --- | --- | --- | --- | --- |
| REQ-001 | 商品Spec §4 | 复用 Snowflake 分布式 ID | SPU/SKU 新 ID 为正 BIGINT；JSON 使用十进制字符串，不出现 JS 精度损失或第二 ID 引擎 | §7相应关卡，§10逐项追踪 |
| REQ-002 | 商品Spec §4 | 新增保存一个 SPU | 成功为未审核/下架/未删除；所有客户端状态/审计值不能覆盖服务端初值 | §7相应关卡，§10逐项追踪 |
| REQ-003 | 商品Spec §4 | 同事务保存 SKU 集合 | 一项失败 SPU/SKU/关系全回滚；SKU 名称与快照由后端生成 | §7相应关卡，§10逐项追踪 |
| REQ-004 | 商品Spec §4 | 确保三级分类品牌关联 | 同关系并发保存最多一个有效关联；不删除历史目录关系 | §7相应关卡，§10逐项追踪 |
| REQ-005 | 商品Spec §4 | 查询商品聚合用于回显 | 返回 SPU 与按稳定顺序排列的有效 SKU；不存在/越租户一致 404 | §7相应关卡，§10逐项追踪 |
| REQ-006 | 商品Spec §4 | 修改保留已有 SKU ID 并重新审核 | 仅未删除且下架可编辑；差异增/改/软删；版本冲突无部分写入 | §7相应关卡，§10逐项追踪 |
| REQ-007 | 商品Spec §4 | 审核通过自动上架 | 删除状态拒绝；有效商品一次原子转换为已审核/上架 | §7相应关卡，§10逐项追踪 |
| REQ-008 | 商品Spec §4 | 下架商品 | 未删除才允许；审核状态保持；重复同目标不重复变更 | §7相应关卡，§10逐项追踪 |
| REQ-009 | 商品Spec §4 | 已审核商品才可上架 | 未审核/删除商品拒绝；不存在安全 404 | §7相应关卡，§10逐项追踪 |
| REQ-010 | 商品Spec §4 | 下架商品逻辑删除 | deleted_at 非 NULL、审核清零、仍下架；有效SKU同事务软删为P，父子均不可通过有效商品查询 | §7相应关卡，§10逐项追踪 |
| REQ-011 | 商品Spec §4 | 还原回收站商品 | 恢复未审核/下架，业务键占用时全回滚；不会恢复编辑时移除的 SKU | §7相应关卡，§10逐项追踪 |
| REQ-012 | 商品Spec §4 | 永久删除已逻辑删除商品 | SPU 和全部关联 SKU（含历史软删）同事务清理，无孤儿；不删图片/目录关系 | §7相应关卡，§10逐项追踪 |
| REQ-013 | 商品Spec §4 | 有效列表与回收站查询 | 一个集合接口 deleted=false/true，分页有界且空 records=[]，不返回 SKU 大数组 | §7相应关卡，§10逐项追踪 |
| REQ-014 | 商品Spec §4 | 区分商品操作权限和可信租户 | 无认证 401、无权限 403、越租户 404；编辑身份不能审核/永久删除；授权能力缺失阻塞发布 | §7相应关卡，§10逐项追踪 |
| REQ-015 | 商品Spec §4 | 保持工程、契约、DDL 历史边界 | 无新项目/未授权依赖/旧 SQL 改写，品牌与上传回归一致 | §7相应关卡，§10逐项追踪 |
| REQ-001（品牌基线，已完成保护） | 前序§4，由主文Depends On限定 | 保持：仓库根提供 `packaging=pom` 的外层 reactor，其 modules 清单只列业务工程根，且不作为生成工程的 Maven `<parent> | 既有结构/源码/DDL与focused回归一致；不重新生成已交付工程 | Step8 regression；相关当前结构gate在Step1/6/7 |
| REQ-002（品牌基线，已完成保护） | 前序§4，由主文Depends On限定 | 保持：二级模块按用户清单落位（`gateway`、`service`、`service_api`、`transaction_fescar`、`web`），命名与 POM 职责遵循 §8 且每个未创建模块有已批准决策 | 既有结构/源码/DDL与focused回归一致；不重新生成已交付工程 | Step8 regression；相关当前结构gate在Step1/6/7 |
| REQ-003（品牌基线，已完成保护） | 前序§4，由主文Depends On限定 | 保持：商品业务工程由 `egon-coding-create-new-module` 从唯一非 open archetype 生成，模块集合与所选形态一致 | 既有结构/源码/DDL与focused回归一致；不重新生成已交付工程 | Step8 regression；相关当前结构gate在Step1/6/7 |
| REQ-004（品牌基线，已完成保护） | 前序§4，由主文Depends On限定 | 保持：天枢配置块在 `application.yml` 与 `application-dev.yml`、`application-test.yml`、`application-prod.yml` 中键集合完全一致，默认 `enabled: false | 既有结构/源码/DDL与focused回归一致；不重新生成已交付工程 | Step8 regression；相关当前结构gate在Step1/6/7 |
| REQ-005（品牌基线，已完成保护） | 前序§4，由主文Depends On限定 | 保持：公共能力的复用方式（`DEC-106` 选定：不建 `enjoyshop_common`，用各工程 `-common` 层 加 Components BOM）已按 `DEC-106` 选项 A 确定为"各工程 `-common` 层 加 Components BOM"，且不得出现无归属的空模块 | 既有结构/源码/DDL与focused回归一致；不重新生成已交付工程 | Step8 regression；相关当前结构gate在Step1/6/7 |
| REQ-006（品牌基线，已完成保护） | 前序§4，由主文Depends On限定 | 保持：品牌契约的对外发布载体确定（`-facade` 模块或独立契约工程），并且 `enjoyshop-service-goods` 明确声明它实际使用的每个 Egon 依赖 | 既有结构/源码/DDL与focused回归一致；不重新生成已交付工程 | Step8 regression；相关当前结构gate在Step1/6/7 |
| REQ-007（品牌基线，已完成保护） | 前序§4，由主文Depends On限定 | 保持：POST /api/v1/brands` 在一次事务内创建一条品牌并返回权威结果 | 既有结构/源码/DDL与focused回归一致；不重新生成已交付工程 | Step8 regression；相关当前结构gate在Step1/6/7 |
| REQ-008（品牌基线，已完成保护） | 前序§4，由主文Depends On限定 | 保持：PUT /api/v1/brands/{brandId}` 完整替换该租户下品牌name/image/letter/seq，并以 `version` 防止静默覆盖 | 既有结构/源码/DDL与focused回归一致；不重新生成已交付工程 | Step8 regression；相关当前结构gate在Step1/6/7 |
| REQ-009（品牌基线，已完成保护） | 前序§4，由主文Depends On限定 | 保持：DELETE /api/v1/brands/{brandId}` 执行软删除（写 `deleted_at`）且请求效果幂等 | 既有结构/源码/DDL与focused回归一致；不重新生成已交付工程 | Step8 regression；相关当前结构gate在Step1/6/7 |
| REQ-010（品牌基线，已完成保护） | 前序§4，由主文Depends On限定 | 保持：GET /api/v1/brands/{brandId}` 返回单条品牌；不存在或跨租户返回同一未泄露结果 | 既有结构/源码/DDL与focused回归一致；不重新生成已交付工程 | Step8 regression；相关当前结构gate在Step1/6/7 |
| REQ-011（品牌基线，已完成保护） | 前序§4，由主文Depends On限定 | 保持：GET /api/v1/brands` 在无过滤参数时返回当前租户全部未软删除品牌，顺序稳定 | 既有结构/源码/DDL与focused回归一致；不重新生成已交付工程 | Step8 regression；相关当前结构gate在Step1/6/7 |
| REQ-012（品牌基线，已完成保护） | 前序§4，由主文Depends On限定 | 保持：同一集合查询支持 `name` 前缀、`letter` 精确与 `seq` 闭区间过滤，非法值有确定行为 | 既有结构/源码/DDL与focused回归一致；不重新生成已交付工程 | Step8 regression；相关当前结构gate在Step1/6/7 |
| REQ-013（品牌基线，已完成保护） | 前序§4，由主文Depends On限定 | 保持：同一集合查询支持分页：`pageNo` 从 1 起、`pageSize` 默认 10 且上限 500，超限归一 | 既有结构/源码/DDL与focused回归一致；不重新生成已交付工程 | Step8 regression；相关当前结构gate在Step1/6/7 |
| REQ-014（品牌基线，已完成保护） | 前序§4，由主文Depends On限定 | 保持：分页与条件可同时使用，排序键为 `seq DESC, id DESC` 且分页在并发新增下行为明确 | 既有结构/源码/DDL与focused回归一致；不重新生成已交付工程 | Step8 regression；相关当前结构gate在Step1/6/7 |
| REQ-015（品牌基线，已完成保护） | 前序§4，由主文Depends On限定 | 保持：brand` 表在 PostgreSQL 上包含 `name`、`image`、`letter`、`seq` 四个业务列 加 `EgonModel` 八个继承列，主键为 `BIGINT` 且由既有雪花 ID 生成 | 既有结构/源码/DDL与focused回归一致；不重新生成已交付工程 | Step8 regression；相关当前结构gate在Step1/6/7 |
| REQ-016（品牌基线，已完成保护） | 前序§4，由主文Depends On限定 | 保持：一次数据库变更只新增下一份 `db/egon-mp/V*.sql` 与对应 Manifest 条目，不改已应用脚本 | 既有结构/源码/DDL与focused回归一致；不重新生成已交付工程 | Step8 regression；相关当前结构gate在Step1/6/7 |
| REQ-017（品牌基线，已完成保护） | 前序§4，由主文Depends On限定 | 保持：品牌业务唯一键为 `(tenant_id, name, deleted_at)` 语义，且在有效行上唯一 | 既有结构/源码/DDL与focused回归一致；不重新生成已交付工程 | Step8 regression；相关当前结构gate在Step1/6/7 |
| REQ-018（品牌基线，已完成保护） | 前序§4，由主文Depends On限定 | 保持：每层边界由 Spring Boot Validation 校验：Controller 用 `@Valid`，Repository 方法参数用 `@Validated` 加约束注解，通用校验复用 `ValidationUtils | 既有结构/源码/DDL与focused回归一致；不重新生成已交付工程 | Step8 regression；相关当前结构gate在Step1/6/7 |
| REQ-019（品牌基线，已完成保护） | 前序§4，由主文Depends On限定 | 保持：统一异常处理类名为 `BaseExceptionHandler`，以 `@RestControllerAdvice` 注册，并把 §9 每个错误行映射到选定信封与安全 HTTP 状态 | 既有结构/源码/DDL与focused回归一致；不重新生成已交付工程 | Step8 regression；相关当前结构gate在Step1/6/7 |
| REQ-020（品牌基线，已完成保护） | 前序§4，由主文Depends On限定 | 保持：品牌 8 项行为中的写操作为 Command、读操作为 Query，且本迭代不产生任何 Event | 既有结构/源码/DDL与focused回归一致；不重新生成已交付工程 | Step8 regression；相关当前结构gate在Step1/6/7 |
| REQ-021（品牌基线，已完成保护） | 前序§4，由主文Depends On限定 | 保持：不新增任何未被用户批准的依赖、插件或生成器；`org.freemarker:freemarker` 2.3.35 只属于生成工具 | 既有结构/源码/DDL与focused回归一致；不重新生成已交付工程 | Step8 regression；相关当前结构gate在Step1/6/7 |
| REQ-022（品牌基线，已完成保护） | 前序§4，由主文Depends On限定 | 保持：本切片有可执行的单元与集成测试设计，覆盖校验、唯一性、软删除、分页归一、租户隔离、异常映射与生成契约 | 既有结构/源码/DDL与focused回归一致；不重新生成已交付工程 | Step8 regression；相关当前结构gate在Step1/6/7 |
| REQ-101（目录上传基线，已完成保护） | 前序§4，由主文Depends On限定 | 保持：allowlisted browser origins 可调用本范围 API | 既有结构/源码/DDL与focused回归一致；不重新生成已交付工程 | Step8 regression；相关当前结构gate在Step1/6/7 |
| REQ-102（目录上传基线，已完成保护） | 前序§4，由主文Depends On限定 | 保持：定义规格/参数模板、三级分类和关系结构 | 既有结构/源码/DDL与focused回归一致；不重新生成已交付工程 | Step8 regression；相关当前结构gate在Step1/6/7 |
| REQ-103（目录上传基线，已完成保护） | 前序§4，由主文Depends On限定 | 保持：在 GET /api/v1/brands 按 categoryName 过滤品牌 | 既有结构/源码/DDL与focused回归一致；不重新生成已交付工程 | Step8 regression；相关当前结构gate在Step1/6/7 |
| REQ-104（目录上传基线，已完成保护） | 前序§4，由主文Depends On限定 | 保持：按分类名称读取规格 | 既有结构/源码/DDL与focused回归一致；不重新生成已交付工程 | Step8 regression；相关当前结构gate在Step1/6/7 |
| REQ-105（目录上传基线，已完成保护） | 前序§4，由主文Depends On限定 | 保持：使用 Shoubing USER Bearer 认证上传 JPEG/PNG 产品图片至 MinIO/Ceph 并返回稳定公开 URL | 既有结构/源码/DDL与focused回归一致；不重新生成已交付工程 | Step8 regression；相关当前结构gate在Step1/6/7 |
| REQ-106（目录上传基线，已完成保护） | 前序§4，由主文Depends On限定 | 保持：10MB 文件与请求限制 | 既有结构/源码/DDL与focused回归一致；不重新生成已交付工程 | Step8 regression；相关当前结构gate在Step1/6/7 |
| REQ-107（目录上传基线，已完成保护） | 前序§4，由主文Depends On限定 | 保持：所有 Spring profile 配置 key 结构一致 | 既有结构/源码/DDL与focused回归一致；不重新生成已交付工程 | Step8 regression；相关当前结构gate在Step1/6/7 |
| REQ-108（目录上传基线，已完成保护） | 前序§4，由主文Depends On限定 | 保持：沿用当前 Light/CQE/MP-SDJ 与运行时基线 | 既有结构/源码/DDL与focused回归一致；不重新生成已交付工程 | Step8 regression；相关当前结构gate在Step1/6/7 |

## 4. Implementation Strategy and Dependency Order

### 4.1 Ordered strategy

原子商品多表变更必须先具备DDL/基线PO/mapper和类型，然后字段转换与tenant/lifecycle SQL，再领域事务。先纯native类型建立测试编译前置，不把缺类型编译失败冒充业务RED；每个组合、转换、状态、安全或HTTP行为都有明确失败断言。No public ProductController until Domain全实现且新链requiredBeans已装配，避免中间状态permitAll。所有阶段在同工作树顺序进行，不使用分支并行写配置/mapper。

生成只跑三份最小配置：三表persistence12文件、SpuBO一文件、SkuBO一文件。每次plan/apply/check后追加SQL journal；custom SQL/BO业务扩展在生成初始之后，并声明为agent-owned，不能重跑生成覆写。商品专用转换的生成/业务责任按Spec DEC-208/§10.4；通用generator行为不改。

### 4.2 Test-first strategy

DDL测试缺script RED；组合类型前置后缺class-level constraint RED；SQL资源缺namedstatement RED；聚合事务或稳定identity缺规则RED；生命周期矩阵合法动作不生效RED；认证/权限缺门RED；HTTP已授权请求缺route404、OAS缺operation RED。focused GREEN后再module回归。PG/Redis/平台验收单独由用户授权，不启动服务。测试代码放现有src/test，框架/JUnit/MockMvc不增加基础设施。

### 4.3 Sequential and parallel boundaries

| Step | Depends on | May run in parallel with | Must not overlap with | Reason |
| --- | --- | --- | --- | --- |
| Step 1 | None | None | 其他所有Step写范围 | 共享DTO/SQL/配置/fixture且单任务提交，先通过前置关卡 |
| Step 2 | Step 1 | None | 其他所有Step写范围 | 共享DTO/SQL/配置/fixture且单任务提交，先通过前置关卡 |
| Step 3 | Step 2 | None | 其他所有Step写范围 | 共享DTO/SQL/配置/fixture且单任务提交，先通过前置关卡 |
| Step 4 | Step 3 | None | 其他所有Step写范围 | 共享DTO/SQL/配置/fixture且单任务提交，先通过前置关卡 |
| Step 5 | Step 4 | None | 其他所有Step写范围 | 共享DTO/SQL/配置/fixture且单任务提交，先通过前置关卡 |
| Step 6 | Step 5 | None | 其他所有Step写范围 | 共享DTO/SQL/配置/fixture且单任务提交，先通过前置关卡 |
| Step 7 | Step 6 | None | 其他所有Step写范围 | 共享DTO/SQL/配置/fixture且单任务提交，先通过前置关卡 |
| Step 8 | Step 7 | None | 其他所有Step写范围 | 共享DTO/SQL/配置/fixture且单任务提交，先通过前置关卡 |

### 4.4 Commit boundaries

用户AGENTS优先：本完整商品需求是一个logical task，最多一个最终commit。Step1–7只保留有证据的本地checkpoint，不创建per-Step commit；Step8全通过后仅提案 `feat(goods): implement product management lifecycle`，按最终文件union逐路径stage，不`git add .`。原Spec是用户已有未跟踪文档，当前只改关系元数据，不未经授权把它或无关文件强行纳入代码commit。最终是否commit按执行阶段授权处理；本次不commit。每Step的Commit paths是归属/将来最终commit候选，不表示逐Step提交。

### 4.5 Spec Simplicity and Implementation-necessity Audit

| Spec element | Spec necessity verdict/section | Current repository evidence | Direct/reuse alternative | Interaction/implementation cost | Plan decision |
| --- | --- | --- | --- | --- | --- |
| 聚合API/Manage/Domain | Spec§7.0 Add/§9独立UC | Brand链真实分层；当前无spu/sku | 不加单SKU写接口/参数preflight | 10必要操作、一个既有应用、无新网络模块 | Implement |
| 两新表+关系ensure | Spec§11 Add/Keep | manifest至20260929_001与现有SINGLE目录 | 同master_data LOCAL | 一SQL版本+2表；旧关联schema不改 | Implement |
| PO/DAO/XML/Repo与BO | Spec§8 GEN | 当前catalog支持最小逐artifact范围 | 三组14输出 | 不扩大backend-crud到单表controller | Implement |
| 单表持久/输入/结果与聚合转换 | SpecDEC-208/§10.4全部必要职责 | CLI generic五variant不等于本章共享Save groups/typedJSON/只读投影 | 真实单表Mapper分别复用Common合同，聚合uses委托 | 增加必要本地mapping文件，零额外API/Command/网络状态 | Implement，GEN/CUSTOM按实际职责区分 |
| 权限能力 | SpecDEC207/§15.1 Add/Reuse | localJianshen5.4.1 jar包含AuthorizationService/context/filter | 不能只USER认证或新本地permissionstore | 现有平台snapshot网络cache边界，1s短TTL、失败关闭 | Implement，不重新选型 |
| 样例清理/前端/MQ/cache | Spec§3/8 Unchanged/N/A/Remove | teaching/user现存，web只有pom | 保持边界 | 无无关删除/新工程 | Already exists/不新增 |

### 4.6 Change-unit Dependency Matrix

| Change unit | Requirements | Proof/RED point | Compile/runtime prerequisites | Produces | Consumers/unblocks | Owning Step |
| --- | --- | --- | --- | --- | --- | --- |
| 建立单版本商品表与可复核生成骨架 | REQ-001,REQ-003,REQ-004,REQ-015 | ProductDdlContractTest | 现有Light/本地generator | 两新表DDL/manifest和三表持久化骨架可编译，14个catalog输出可复核 | Step2 | Step 1 |
| 定义商品边界模型与纯字段校验 | REQ-001,REQ-002,REQ-003,REQ-005 | ProductCompositionValidationTest | Step1 contracts | 标准JSON/分组/有界SKU组合可以独立校验，不泄漏持久字段 | Step3 | Step 2 |
| 实现真实字段转换和受控持久化访问 | REQ-003,REQ-004,REQ-005,REQ-006 | ProductConverterTest | Step2 contracts | 具名SQL与BaseConverter字段映射可验证，无通用Wrapper/直接DAO业务调用 | Step4 | Step 3 |
| 完成可查询、可修订的原子商品聚合 | REQ-001,REQ-002,REQ-003,REQ-004 | ProductUpdateTest | Step3 contracts | 保存/修订/详情/列表共享正确事务与稳定SKU身份 | Step5 | Step 4 |
| 完成审核销售状态与完整回收站生命周期 | REQ-007,REQ-008,REQ-009,REQ-010 | ProductLifecycleComponentTest | Step4 contracts | 审核/上下架/软删/恢复/永久删除全端口同规则，P/E历史正确 | Step6 | Step 5 |
| 接入可信身份和四类商品操作授权 | REQ-014,REQ-015,REQ-018,REQ-021 | ProductAuthorizationComponentTest | Step5 contracts | 十个Manage入口与新安全链失败关闭，4权限独立；旧路由策略保留 | Step7 | Step 6 |
| 交付十个商品 REST 与真实生成 OpenAPI 契约 | REQ-002,REQ-005,REQ-006,REQ-007 | ProductControllerWebTest | Step6 contracts | 所有已批准商品API/wire/error/OAS与安全链一致 | Step8 | Step 7 |
| 完成边界回归、发布说明与单任务交付 | REQ-001,REQ-002,REQ-003,REQ-004 | ProductContractConformanceTest | Step7 contracts | 全module验证与旧品牌/上传/目录边界保留，文档和操作前置明确 | Step8 | Step 8 |

### 4.7 Java, Spring, and Egon-COLA Implementation Standards

| Concern | Current repository evidence | Effective Spec decision | Planned implementation consequence | Owning Steps/checks |
| --- | --- | --- | --- | --- |
| Architecture profile | goods实际domain/application/infrastructure/adapter/facade/common/start；Light POM与bytecode plugin | exact非openLight | Controller→Manage→Domain端口→Repo→DAO，禁止biz.*新模块 | 全Step MC-ARCH-001 |
| Reuse/capability | Core/ID/MP/Validation/MapStruct/已有平台starter | 能力ledger、仅Jianshen新增 | 不新增ID/JSON/迁移框架或业务权限引擎 | Step1/3/6 MC-REUSE/DEP |
| Naming/model/validation/conversion | BrandBO/PO/Converter、EgonModel、lombok.config | semantic roles/class/Lombok/BaseConverter | carrier分组nativeConstraint，PO只DBshape，typedJSON边界 | Step2/3/4 MC-NAME/VALID/MODEL/CONVERT |
| Bean/logging/util/JSON/time/config | existingfinalQualifier/Slf4j、Jackson/Instant、四profile | exactJava rules | namedBeans、构造DI、无Date/Fastjson/BeanUtils；allprofile同keys | 每Step对应检查 |
| Business variation/pattern | 多动作状态guards/Spec§13 | Specification | EnumMap+Predicate组合与私有immutableProductTransitionBO，无business状态switch | Step5 MC-PATTERN |

#### Capability reuse ledger

| Need | Candidates inspected | Exact evidence | Fit/gap | Decision | Added dependency/custom code | Owning Step/check |
| --- | --- | --- | --- | --- | --- | --- |
| ID/CRUD/restore | Egon Common ID/MP | source LongIdGenerator/EgonModel/EgonColaMapper/Repository + local5.4.1 | ordinaryguardedCRUD/restore足够；特殊P/E字段需具名SQL | Reuse | 无新framework；customSQL是业务扩展 | Step1/3/5 MC-REUSE |
| JSON/validation/conversion | Jackson/Jakarta/MapStruct/Core | goods POM、BaseConverter、旧ConvertorTest | 业务组合与严格stringIDs没有旧实现 | Reuse + 必要自定义constraint/decoder | 无新lib；商品特有mapping见Spec§10.4 | Step2/3 MC-VALID/CONVERT |
| Auth/permissions | Shoubing/Jianshen | Jianshenstarter jar javap/源码DefaultAuthorizationService/Filter/Properties | 可信permission+resource已有能力，goods尚未依赖 | Reuse | 仅已批top.egon:egon-cola-tianquan-jianshen-starter:5.4.1 | Step6 MC-DEP |
| Schema/generator | MP runner/egon-codegen.sh/FreeMarker | 单版本manifest、CLI templates实测format2/2.3.35 | persistence+BO最小选择可用；converter一次固定5种 | Reuse；template不适配商品业务mapping的部分按Spec DEC-208 CUSTOM | 不改生成器或ftl、不写初始GEN替代 | Step1/3 MC-SCOPE |
| Tests/docs | JUnit/MockMvc/H2/既有fixture/OAS | PersistenceTestSupport + BrandOpenApiContractTest | 必需RBACtestfakes共享helper足够 | Reuse | test-onlyconfiguration，无新production infrastructure | Step4/6/7/8 MC-TEST |

### 4.8 User-mandated Java Rule Implementation Matrix

| Literal rule | Spec source | Repository evidence | Exact files and order | Pseudocode obligations | Validation gate | Steps | Status/blocker |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Rule 1 | 商品Spec§6.2/10/11/13 | 当前Brand/Spec/MP/Light ledger | §7对应精确文件顺序 | 类型语义suffix，避免Info/Data | 字段与类清单 | 全部相关Step；Rule11每Step | PASS |
| Rule 2 | 商品Spec§6.2/10/11/13 | 当前Brand/Spec/MP/Light ledger | §7对应精确文件顺序 | native/constraint/@Valid/@Validated/Create/Update/Restore，逐边界 | Composition/Contract tests | 全部相关Step；Rule11每Step | PASS |
| Rule 3 | 商品Spec§6.2/10/11/13 | 当前Brand/Spec/MP/Light ledger | §7对应精确文件顺序 | class完整Lombok；PO兼容EgonModel SuperBuilder；所有mapper CommonBaseConverter | Converter/architecture tests | 全部相关Step；Rule11每Step | PASS：DEC-208单表与聚合职责已明确 |
| Rule 4 | 商品Spec§6.2/10/11/13 | 当前Brand/Spec/MP/Light ledger | §7对应精确文件顺序 | business Slf4j/namedBean/RequiredArgsConstructor/finalQualifier且lombok传播 | DI/context tests | 全部相关Step；Rule11每Step | PASS |
| Rule 5 | 商品Spec§6.2/10/11/13 | 当前Brand/Spec/MP/Light ledger | §7对应精确文件顺序 | JDK/已用Commons/Guava，不增加工具lib | import/dependency inspection | 全部相关Step；Rule11每Step | PASS |
| Rule 6 | 商品Spec§6.2/10/11/13 | 当前Brand/Spec/MP/Light ledger | §7对应精确文件顺序 | Jackson唯一、stringIDs、EnumValue/JsonValue、不可写审计 | wire/OAS tests | 全部相关Step；Rule11每Step | PASS |
| Rule 7 | 商品Spec§6.2/10/11/13 | 当前Brand/Spec/MP/Light ledger | §7对应精确文件顺序 | 四环境+导入路由同结构 | ProfileKeyParityTest | 全部相关Step；Rule11每Step | PASS |
| Rule 9 | 商品Spec§6.2/10/11/13 | 当前Brand/Spec/MP/Light ledger | §7对应精确文件顺序 | 真实Specification，简单diff集合不增Strategy | LifecycleComponentTest | 全部相关Step；Rule11每Step | PASS |
| Rule 10 | 商品Spec§6.2/10/11/13 | 当前Brand/Spec/MP/Light ledger | §7对应精确文件顺序 | Instant/UTC LocalDateTime微秒，Duration，没有java.util.Date | Converter/SQL tests | 全部相关Step；Rule11每Step | PASS |
| Rule 11 | 商品Spec§6.2/10/11/13 | 当前Brand/Spec/MP/Light ledger | §7对应精确文件顺序 | exactLight/MP/单DDL+业务键deleted_at+activeNULL；C/Q无本章Event | architecture/DDL regressions | 全部相关Step；Rule11每Step | PASS |

```text
1 类名规范，必须以 java的pojo规范命名。以dao po bo vo dto query command event等结尾
2 每层之间必须被 springboot-validation 校验，复用对象使用分组校验；基于原生和自定义约束注解、@Valid、@Validated 及 ConstraintValidator 扩展实现。ValidationUtils 只承担通用手工校验，不承载全部业务校验；电话号码复用 libphonenumber。
3 Java POJO 默认使用 class，注解为 @Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain = true)，按构造目标选择 @Builder 或 @SuperBuilder；父类状态参与相等性时使用 @EqualsAndHashCode(callSuper = true)。只有不可变 value object 可以使用 record，并豁免 class 的 Lombok 规范；普通实体不能因简单而使用 record。使用 MapStruct、MapStructPlus 和 egon-cola-component-common-core 的 BaseConverter（双向）或 BaseForwardConverter（不可逆投影）进行转换。
4业务类必须使用@Slf4j注解注入log对象。如果业务类被spring管理，必须指定名称，如果是单例的情况下，参考@Service("userService")。如果需要依赖注入，必须@RequiredArgsConstructor进行修饰，不要代码中写。且属性必须被@qualify修饰。
5 工具类只允许使用jdk原生、Apache Commons(commons-lang3、commons-collections4、commons-io、commons-text、commons-codec、commons-beanutils)、Guava。针对Tika按需引入。
6 JSON 使用 Spring Boot Jackson；持久化枚举值使用 @EnumValue，向前端输出的枚举值使用 @JsonValue，禁止 ordinal 作为业务编码。
7 springboot 多环境配置文件，必须保持配置一致，但值不一定一致。
9 复杂业务必须引入设计模式，不允许硬编码
10 日期相关的必须使用java.time下的实体类，不允许使用java.util下的
11 只允许现有三层结构或当前 egon-cola-archetypes 的 DDD 结构，三层结构保持现状。必须尽量复用 egon-cola-components；持久化模块必须使用 Egon COLA MP Starter，DDL 统一由 egon-mp-sdj-ext-starter 分布式管理。业务唯一键必须组合业务列与 deletedAt（数据库 deleted_at），并验证 NULL、租户及重复软删除语义。采用 CQE（Command Query Event）；Event 必须经 egon-cola-component-transactional-outbox-starter 或 MQ 中间件投递。
```

## 5. Change File Tree

所有路径以下表为精确write inventory；同路径只列一次，重复Step严格由不同symbol/section先后扩展。操作GENERATED→MODIFY表示先受保护生成，再业务定向扩展，不允许重新生成overwrite。以下是未来执行范围，本轮不创建这些源文件。

| Operation | Path | Symbols/responsibility | Owning Step | Requirements | Validation owner |
| --- | --- | --- | --- | --- | --- |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/ddl/ProductDdlContractTest.java` | ProductDdlContractTest | Step 1 | REQ-001,REQ-003,REQ-004,REQ-015,REQ-016,REQ-021 | 对应Step focused GREEN及最终verify |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/resources/db/egon-mp/V20261009_001__initialize_product_schema.sql` | MASTER_DATA spu/sku DDL | Step 1 | REQ-001,REQ-003,REQ-004,REQ-015,REQ-016,REQ-021 | 对应Step focused GREEN及最终verify |
| MODIFY | `enjoyshop-service/enjoyshop-service-goods/src/main/resources/db/egon-mp/repository-manifest.json` | scripts最后一条20261009_001 | Step 1 | REQ-001,REQ-003,REQ-004,REQ-015,REQ-016,REQ-021 | 对应Step focused GREEN及最终verify |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/codegen/product-persistence-codegen.json` | configVersion1/projectType light/logicalTables | Step 1 | REQ-001,REQ-003,REQ-004,REQ-015,REQ-016,REQ-021 | 对应Step focused GREEN及最终verify |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/codegen/product-spu-model-codegen.json` | configVersion1/projectType light/logicalTables | Step 1 | REQ-001,REQ-003,REQ-004,REQ-015,REQ-016,REQ-021 | 对应Step focused GREEN及最终verify |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/codegen/product-sku-model-codegen.json` | configVersion1/projectType light/logicalTables | Step 1 | REQ-001,REQ-003,REQ-004,REQ-015,REQ-016,REQ-021 | 对应Step focused GREEN及最终verify |
| GENERATED | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/po/SpuPO.java` | SpuPO | Step 1 | REQ-001,REQ-003,REQ-004,REQ-015,REQ-016,REQ-021 | 对应Step focused GREEN及最终verify |
| GENERATED/MODIFY | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/dao/SpuDAO.java` | SpuDAO; selectByLifecycle,selectPage,countPage,lockByLifecycle,loadCatalogReferences,deleteVersionedById,restoreVersionedById,purgeDeletedById | Step 1,Step 3 | REQ-001,REQ-003,REQ-004,REQ-015,REQ-016,REQ-021 | 对应Step focused GREEN及最终verify |
| GENERATED/MODIFY | `enjoyshop-service/enjoyshop-service-goods/src/main/resources/mybatis/mapper/goods/SpuDAO.xml` | SpuDAO; selectByLifecycle,selectPage,countPage,lockByLifecycle,loadCatalogReferences,deleteVersionedById,restoreVersionedById,purgeDeletedById | Step 1,Step 3 | REQ-001,REQ-003,REQ-004,REQ-015,REQ-016,REQ-021 | 对应Step focused GREEN及最终verify |
| GENERATED/MODIFY | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/repo/SpuRepository.java` | SpuRepository; selectByLifecycle,selectPage,countPage,lockByLifecycle,loadCatalogReferences,deleteVersionedById,restoreVersionedById,purgeDeletedById | Step 1,Step 3 | REQ-001,REQ-003,REQ-004,REQ-015,REQ-016,REQ-021 | 对应Step focused GREEN及最终verify |
| GENERATED | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/po/SkuPO.java` | SkuPO | Step 1 | REQ-001,REQ-003,REQ-004,REQ-015,REQ-016,REQ-021 | 对应Step focused GREEN及最终verify |
| GENERATED/MODIFY | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/dao/SkuDAO.java` | SkuDAO; selectBySpuLifecycle,selectAllBySpuForPurge,deleteVersionedById,restoreVersionedById,purgeAllBySpu | Step 1,Step 3 | REQ-001,REQ-003,REQ-004,REQ-015,REQ-016,REQ-021 | 对应Step focused GREEN及最终verify |
| GENERATED/MODIFY | `enjoyshop-service/enjoyshop-service-goods/src/main/resources/mybatis/mapper/goods/SkuDAO.xml` | SkuDAO; selectBySpuLifecycle,selectAllBySpuForPurge,deleteVersionedById,restoreVersionedById,purgeAllBySpu | Step 1,Step 3 | REQ-001,REQ-003,REQ-004,REQ-015,REQ-016,REQ-021 | 对应Step focused GREEN及最终verify |
| GENERATED/MODIFY | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/repo/SkuRepository.java` | SkuRepository; selectBySpuLifecycle,selectAllBySpuForPurge,deleteVersionedById,restoreVersionedById,purgeAllBySpu | Step 1,Step 3 | REQ-001,REQ-003,REQ-004,REQ-015,REQ-016,REQ-021 | 对应Step focused GREEN及最终verify |
| GENERATED | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/po/CategoryBrandPO.java` | CategoryBrandPO | Step 1 | REQ-001,REQ-003,REQ-004,REQ-015,REQ-016,REQ-021 | 对应Step focused GREEN及最终verify |
| GENERATED/MODIFY | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/dao/CategoryBrandDAO.java` | CategoryBrandDAO; selectActiveByEndpoints,ensureActive via repository | Step 1,Step 3 | REQ-001,REQ-003,REQ-004,REQ-015,REQ-016,REQ-021 | 对应Step focused GREEN及最终verify |
| GENERATED/MODIFY | `enjoyshop-service/enjoyshop-service-goods/src/main/resources/mybatis/mapper/goods/CategoryBrandDAO.xml` | CategoryBrandDAO; selectActiveByEndpoints,ensureActive via repository | Step 1,Step 3 | REQ-001,REQ-003,REQ-004,REQ-015,REQ-016,REQ-021 | 对应Step focused GREEN及最终verify |
| GENERATED/MODIFY | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/repo/CategoryBrandRepository.java` | CategoryBrandRepository; selectActiveByEndpoints,ensureActive via repository | Step 1,Step 3 | REQ-001,REQ-003,REQ-004,REQ-015,REQ-016,REQ-021 | 对应Step focused GREEN及最终verify |
| GENERATED/MODIFY | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/SpuBO.java` | SpuBO; SpuBO business/audit field semantics | Step 1,Step 2 | REQ-001,REQ-003,REQ-004,REQ-015,REQ-016,REQ-021 | 对应Step focused GREEN及最终verify |
| GENERATED/MODIFY | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/SkuBO.java` | SkuBO; SkuBO business/audit field semantics | Step 1,Step 2 | REQ-001,REQ-003,REQ-004,REQ-015,REQ-016,REQ-021 | 对应Step focused GREEN及最终verify |
| MODIFY | `docs/egon/codegen/ddl-consumption-log.md` | append three generator consumption entries | Step 1 | REQ-001,REQ-003,REQ-004,REQ-015,REQ-016,REQ-021 | 对应Step focused GREEN及最终verify |
| MODIFY | `enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/architecture/LightPersistenceArchitectureTest.java` | counts + exact persistence file manifest; DomainServiceImpl exact9; final inventories/owned package checks | Step 1,Step 4,Step 8 | REQ-001,REQ-003,REQ-004,REQ-015,REQ-016,REQ-021 | 对应Step focused GREEN及最终verify |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/validation/ProductValidationGroup.java` | Create,Update,HttpStateInput,StateCall,Detail,Page | Step 2 | REQ-001,REQ-002,REQ-003,REQ-005,REQ-006,REQ-013 | 对应Step focused GREEN及最终verify |
| CREATE/MODIFY | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/SpuSaveCommand.java` | sn,name,caption,brandId,category1Id,category2Id,category3Id,templateId,freightId,image,images,saleService,introduction,isEnableSpec,specItems,paraItems; SpuSaveCommand ID binding | Step 2 | REQ-001,REQ-002,REQ-003,REQ-005,REQ-006,REQ-013 | 对应Step focused GREEN及最终verify |
| CREATE/MODIFY | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/SkuSaveCommand.java` | id,sn,num,alertNum,price,weight,spec,image,images,status; SkuSaveCommand ID binding | Step 2 | REQ-001,REQ-002,REQ-003,REQ-005,REQ-006,REQ-013 | 对应Step focused GREEN及最终verify |
| CREATE/MODIFY | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/ProductSaveCommand.java` | SpuSaveCommand spu,List<SkuSaveCommand> skuList,Long version; class-level ValidProductComposition | Step 2 | REQ-001,REQ-002,REQ-003,REQ-005,REQ-006,REQ-013 | 对应Step focused GREEN及最终verify |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/ProductIdQuery.java` | Long spuId,Boolean deleted | Step 2 | REQ-001,REQ-002,REQ-003,REQ-005,REQ-006,REQ-013 | 对应Step focused GREEN及最终verify |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/ProductPageQuery.java` | pageNo,pageSize,deleted,sn,name,status,isMarketable | Step 2 | REQ-001,REQ-002,REQ-003,REQ-005,REQ-006,REQ-013 | 对应Step focused GREEN及最终verify |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/ProductLifecycleCommand.java` | Long spuId,Long version | Step 2 | REQ-001,REQ-002,REQ-003,REQ-005,REQ-006,REQ-013 | 对应Step focused GREEN及最终verify |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/ProductActionEnum.java` | CREATE,EDIT,AUDIT,PULL,PUT,DELETE,RESTORE,PURGE | Step 2 | REQ-001,REQ-002,REQ-003,REQ-005,REQ-006,REQ-013 | 对应Step focused GREEN及最终verify |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/ProductSkuDeletionKindEnum.java` | PRODUCT("P"),EDIT("E") | Step 2 | REQ-001,REQ-002,REQ-003,REQ-005,REQ-006,REQ-013 | 对应Step focused GREEN及最终verify |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/ProductBO.java` | SpuBO spu,List<SkuBO> skuList,Long expectedVersion | Step 2 | REQ-001,REQ-002,REQ-003,REQ-005,REQ-006,REQ-013 | 对应Step focused GREEN及最终verify |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/ProductStateBO.java` | id,status,marketable,deleted,version,hasActiveSku,referencesValid | Step 2 | REQ-001,REQ-002,REQ-003,REQ-005,REQ-006,REQ-013 | 对应Step focused GREEN及最终verify |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/ProductDomainQuery.java` | id,deleted,page,name,sn,status,isMarketable | Step 2 | REQ-001,REQ-002,REQ-003,REQ-005,REQ-006,REQ-013 | 对应Step focused GREEN及最终verify |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/CatalogReferenceBO.java` | brandId,brandName,category1Id,category2Id,category3Id,categoryName,templateId,orderedSpecs,paraOptions | Step 2 | REQ-001,REQ-002,REQ-003,REQ-005,REQ-006,REQ-013 | 对应Step focused GREEN及最终verify |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/ProductResult.java` | SpuResult spu,List<SkuResult> skuList | Step 2 | REQ-001,REQ-002,REQ-003,REQ-005,REQ-006,REQ-013 | 对应Step focused GREEN及最终verify |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/SpuResult.java` | all §9 SPU output fields: id + inputfields + saleNum/commentNum/status/isMarketable/isDelete/version/createTime/updateTime/deletedAt | Step 2 | REQ-001,REQ-002,REQ-003,REQ-005,REQ-006,REQ-013 | 对应Step focused GREEN及最终verify |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/SkuResult.java` | id,spuId,sn,num,alertNum,price,weight,spec,image,images,status,name,categoryId,categoryName,brandName,saleNum,commentNum,createTime,updateTime | Step 2 | REQ-001,REQ-002,REQ-003,REQ-005,REQ-006,REQ-013 | 对应Step focused GREEN及最终verify |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/ProductSummaryResult.java` | id,sn,name,image,status,isMarketable,isDelete,version | Step 2 | REQ-001,REQ-002,REQ-003,REQ-005,REQ-006,REQ-013 | 对应Step focused GREEN及最终verify |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/ProductStateResult.java` | id,status,isMarketable,isDelete,version | Step 2 | REQ-001,REQ-002,REQ-003,REQ-005,REQ-006,REQ-013 | 对应Step focused GREEN及最终verify |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/common/validation/ProductCompositionValidationTest.java` | invalidComposition,unknownFields,groups | Step 2 | REQ-001,REQ-002,REQ-003,REQ-005,REQ-006,REQ-013 | 对应Step focused GREEN及最终verify |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/json/ProductIdDeserializer.java` | deserialize(JsonParser,DeserializationContext) | Step 2 | REQ-001,REQ-002,REQ-003,REQ-005,REQ-006,REQ-013 | 对应Step focused GREEN及最终verify |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/validation/ValidProductComposition.java` | @Constraint(validatedBy=ProductCompositionValidator.class) | Step 2 | REQ-001,REQ-002,REQ-003,REQ-005,REQ-006,REQ-013 | 对应Step focused GREEN及最终verify |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/validation/ProductCompositionValidator.java` | isValid(ProductSaveCommand,ConstraintValidatorContext) | Step 2 | REQ-001,REQ-002,REQ-003,REQ-005,REQ-006,REQ-013 | 对应Step focused GREEN及最终verify |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/application/goods/ProductConverterTest.java` | roundtrip_json_fields,readonly_projection | Step 3 | REQ-003,REQ-004,REQ-005,REQ-006,REQ-010,REQ-011 | 对应Step focused GREEN及最终verify |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/service/impl/ProductSpuPersistenceConverter.java` | BaseConverter<SpuPO,SpuBO> | Step 3 | REQ-003,REQ-004,REQ-005,REQ-006,REQ-010,REQ-011 | 对应Step focused GREEN及最终verify |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/service/impl/ProductSkuPersistenceConverter.java` | BaseConverter<SkuPO,SkuBO> | Step 3 | REQ-003,REQ-004,REQ-005,REQ-006,REQ-010,REQ-011 | 对应Step focused GREEN及最终verify |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/ProductApplicationConverter.java` | BaseForwardConverter<ProductSaveCommand,ProductBO> | Step 3 | REQ-003,REQ-004,REQ-005,REQ-006,REQ-010,REQ-011 | 对应Step focused GREEN及最终verify |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/ProductQueryConverter.java` | BaseForwardConverter<ProductBO,ProductResult> | Step 3 | REQ-003,REQ-004,REQ-005,REQ-006,REQ-010,REQ-011 | 对应Step focused GREEN及最终verify |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/goods/ProductQuerySqlShapeTest.java` | tenantLifecyclePredicates,guardedRowEffects | Step 3 | REQ-003,REQ-004,REQ-005,REQ-006,REQ-010,REQ-011 | 对应Step focused GREEN及最终verify |
| MODIFY | `enjoyshop-service/enjoyshop-service-goods/src/main/resources/application.yml` | MP product tables/lifecycle/custom-root allowlists; product SINGLE maps/restoreStatements/rootStatements/Jianshen keys | Step 3,Step 6 | REQ-003,REQ-004,REQ-005,REQ-006,REQ-010,REQ-011 | 对应Step focused GREEN及最终verify |
| MODIFY | `enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-dev.yml` | MP product tables/lifecycle/custom-root allowlists; product SINGLE maps/restoreStatements/rootStatements/Jianshen keys | Step 3,Step 6 | REQ-003,REQ-004,REQ-005,REQ-006,REQ-010,REQ-011 | 对应Step focused GREEN及最终verify |
| MODIFY | `enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-test.yml` | MP product tables/lifecycle/custom-root allowlists; product SINGLE maps/restoreStatements/rootStatements/Jianshen keys | Step 3,Step 6 | REQ-003,REQ-004,REQ-005,REQ-006,REQ-010,REQ-011 | 对应Step focused GREEN及最终verify |
| MODIFY | `enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-prod.yml` | MP product tables/lifecycle/custom-root allowlists; product SINGLE maps/restoreStatements/rootStatements/Jianshen keys | Step 3,Step 6 | REQ-003,REQ-004,REQ-005,REQ-006,REQ-010,REQ-011 | 对应Step focused GREEN及最终verify |
| MODIFY | `enjoyshop-service/enjoyshop-service-goods/src/main/resources/egon-mybatis-plus-sharding.yml` | MP product tables/lifecycle/custom-root allowlists; product SINGLE maps/restoreStatements/rootStatements/Jianshen keys | Step 3,Step 6 | REQ-003,REQ-004,REQ-005,REQ-006,REQ-010,REQ-011 | 对应Step focused GREEN及最终verify |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/ProductDomainService.java` | create/update/detail/page/audit/pull/put/delete/restore/purge | Step 4 | REQ-001,REQ-002,REQ-003,REQ-004,REQ-005,REQ-006 | 对应Step focused GREEN及最终verify |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/goods/ProductUpdateTest.java` | newIdsStableExistingIds,invalidReferences,rollbackConflicts | Step 4 | REQ-001,REQ-002,REQ-003,REQ-004,REQ-005,REQ-006 | 对应Step focused GREEN及最终verify |
| CREATE/MODIFY | `enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/goods/ProductAggregatePersistenceTest.java` | createRollback,editCas,consistentQueries; P/E delete/restore/purge/adversarialCAS | Step 4,Step 5 | REQ-001,REQ-002,REQ-003,REQ-004,REQ-005,REQ-006 | 对应Step focused GREEN及最终verify |
| MODIFY | `enjoyshop-service/enjoyshop-service-goods/src/test/resources/mybatis/h2-schema.sql` | spu/sku H2-equivalent fixtures | Step 4 | REQ-001,REQ-002,REQ-003,REQ-004,REQ-005,REQ-006 | 对应Step focused GREEN及最终verify |
| CREATE/MODIFY | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/service/impl/ProductDomainServiceImpl.java` | create/update/detail/page + declared lifecycle slots; audit/pull/put/delete/restore/purge | Step 4,Step 5 | REQ-001,REQ-002,REQ-003,REQ-004,REQ-005,REQ-006 | 对应Step focused GREEN及最终verify |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/domain/goods/ProductLifecycleComponentTest.java` | transitionMatrix,invalidCombinations,noOpVersions | Step 5 | REQ-007,REQ-008,REQ-009,REQ-010,REQ-011,REQ-012 | 对应Step focused GREEN及最终verify |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/ProductLifecycleComponent.java` | evaluate(action,current); private ProductTransitionBO | Step 5 | REQ-007,REQ-008,REQ-009,REQ-010,REQ-011,REQ-012 | 对应Step focused GREEN及最终verify |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/goods/ProductUniquenessTest.java` | activeKeys,lifecycleMicros,restoreConflicts | Step 5 | REQ-007,REQ-008,REQ-009,REQ-010,REQ-011,REQ-012 | 对应Step focused GREEN及最终verify |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/application/goods/ProductAuthorizationComponentTest.java` | allowDenyIndeterminate,resourceBoundIdentity | Step 6 | REQ-014,REQ-015,REQ-018,REQ-021,REQ-101,REQ-107 | 对应Step focused GREEN及最终verify |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/start/config/ProductSecurityContractTest.java` | matcherOrder,missingBeans,profileParity | Step 6 | REQ-014,REQ-015,REQ-018,REQ-021,REQ-101,REQ-107 | 对应Step focused GREEN及最终verify |
| MODIFY | `enjoyshop-service/enjoyshop-service-goods/pom.xml` | dependency Jianshen starter5.4.1 | Step 6 | REQ-014,REQ-015,REQ-018,REQ-021,REQ-101,REQ-107 | 对应Step focused GREEN及最终verify |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/ProductAuthorizationComponent.java` | void requirePermission(String permissionCode) | Step 6 | REQ-014,REQ-015,REQ-018,REQ-021,REQ-101,REQ-107 | 对应Step focused GREEN及最终verify |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/ProductManage.java` | all ten operations per INTERNAL201–210 | Step 6 | REQ-014,REQ-015,REQ-018,REQ-021,REQ-101,REQ-107 | 对应Step focused GREEN及最终verify |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/ProductManageImpl.java` | delegation with permission-first gate | Step 6 | REQ-014,REQ-015,REQ-018,REQ-021,REQ-101,REQ-107 | 对应Step focused GREEN及最终verify |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/support/ProductTestSecurityConfiguration.java` | isolated RBAC/IDP/snapshot test beans | Step 6 | REQ-014,REQ-015,REQ-018,REQ-021,REQ-101,REQ-107 | 对应Step focused GREEN及最终verify |
| MODIFY | `enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/support/PersistenceTestSupport.java` | @Import(ProductTestSecurityConfiguration.class) | Step 6 | REQ-014,REQ-015,REQ-018,REQ-021,REQ-101,REQ-107 | 对应Step focused GREEN及最终verify |
| MODIFY | `enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/start/config/ProfileKeyParityTest.java` | assertProductAndJianshenKeys | Step 6 | REQ-014,REQ-015,REQ-018,REQ-021,REQ-101,REQ-107 | 对应Step focused GREEN及最终verify |
| MODIFY | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/start/config/NativeHttpSecurityConfiguration.java` | productSecurityFilterChain | Step 6 | REQ-014,REQ-015,REQ-018,REQ-021,REQ-101,REQ-107 | 对应Step focused GREEN及最终verify |
| MODIFY | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/filter/TenantContextFilter.java` | shouldNotFilter productpath only | Step 6 | REQ-014,REQ-015,REQ-018,REQ-021,REQ-101,REQ-107 | 对应Step focused GREEN及最终verify |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/goods/ProductControllerWebTest.java` | API201–210 contract cases | Step 7 | REQ-002,REQ-005,REQ-006,REQ-007,REQ-008,REQ-009 | 对应Step focused GREEN及最终verify |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/goods/ProductOpenApiContractTest.java` | paths,operationIds,schema/security/erroroneOf | Step 7 | REQ-002,REQ-005,REQ-006,REQ-007,REQ-008,REQ-009 | 对应Step focused GREEN及最终verify |
| MODIFY | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/enums/GoodsErrorStatus.java` | PRODUCT610201–208/299 | Step 7 | REQ-002,REQ-005,REQ-006,REQ-007,REQ-008,REQ-009 | 对应Step focused GREEN及最终verify |
| MODIFY | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/handler/BaseExceptionHandler.java` | product-only error mapper/path selector | Step 7 | REQ-002,REQ-005,REQ-006,REQ-007,REQ-008,REQ-009 | 对应Step focused GREEN及最终verify |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/goods/ProductController.java` | all API201–210 operationId methods | Step 7 | REQ-002,REQ-005,REQ-006,REQ-007,REQ-008,REQ-009 | 对应Step focused GREEN及最终verify |
| MODIFY | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/start/config/OpenApiConfig.java` | bearerAuth securityScheme | Step 7 | REQ-002,REQ-005,REQ-006,REQ-007,REQ-008,REQ-009 | 对应Step focused GREEN及最终verify |
| MODIFY | `enjoyshop-service/enjoyshop-service-goods/README.zh-CN.md` | product API/security/managed-DDL/manualverification | Step 8 | REQ-001,REQ-002,REQ-003,REQ-004,REQ-005,REQ-006 | 对应Step focused GREEN及最终verify |
| MODIFY | `enjoyshop-service/enjoyshop-service-goods/README.md` | product API/security/managed-DDL/manualverification | Step 8 | REQ-001,REQ-002,REQ-003,REQ-004,REQ-005,REQ-006 | 对应Step focused GREEN及最终verify |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/architecture/ProductContractConformanceTest.java` | typed boundaries/bean qualifiers/static forbiddenimports | Step 8 | REQ-001,REQ-002,REQ-003,REQ-004,REQ-005,REQ-006 | 对应Step focused GREEN及最终verify |

无DELETE/RENAME生产文件：教学/user样例、旧SQL、存储/目录接口保持。元数据额外范围只有本Plan及主Spec Related Plans backlink；该backlink不是生产代码Step。

| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/service/impl/ProductJsonFieldConverter.java` | ProductJsonFieldConverter，单表真实职责见Spec§10.4 | Step 3 | REQ-003,REQ-005,REQ-006,REQ-015 | ProductConverterTest与聚合委托测试 |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/ProductSpuSaveConverter.java` | ProductSpuSaveConverter，单表真实职责见Spec§10.4 | Step 3 | REQ-003,REQ-005,REQ-006,REQ-015 | ProductConverterTest与聚合委托测试 |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/ProductSpuResultConverter.java` | ProductSpuResultConverter，单表真实职责见Spec§10.4 | Step 3 | REQ-003,REQ-005,REQ-006,REQ-015 | ProductConverterTest与聚合委托测试 |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/ProductSkuSaveConverter.java` | ProductSkuSaveConverter，单表真实职责见Spec§10.4 | Step 3 | REQ-003,REQ-005,REQ-006,REQ-015 | ProductConverterTest与聚合委托测试 |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/ProductSkuResultConverter.java` | ProductSkuResultConverter，单表真实职责见Spec§10.4 | Step 3 | REQ-003,REQ-005,REQ-006,REQ-015 | ProductConverterTest与聚合委托测试 |
| MODIFY | `enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/ddl/CatalogDdlContractTest.java` | Manifest总数兼容追加，旧prefix/hash不变 | Step 1 | REQ-015,REQ-016 | CatalogDdlContractTest |

## 6. Prerequisites, Constraints, and Plan Clarifications

### 6.1 Repository baseline and working tree

工作根/模块绝对路径为 `/Users/mario/SelfProject/enjoyshop` 和其 `enjoyshop-service/enjoyshop-service-goods`。main HEAD c3559b0；只有用户本轮Spec未跟踪。用户粘贴AGENTS是本任务约束，当前仓库搜索无额外AGENTS。所有Step前重查git status，旧dirty/untracked/staged不能混入，禁止stash/reset/clean强行造干净工作树。本次只新Plan与主Spec backlink，无commit。

### 6.2 Build, test, and environment prerequisites

| Concern | Exact command/source | Required state | Validation boundary |
| --- | --- | --- | --- |
| Java/Maven | 模块java -version；./mvnw -version；现有wrapper和mvn3.9.14已找到 | Java21，已批准artifact可解析 | tooling，不启动服务 |
| generator | Egon /scripts/egon-codegen.sh templates | 本地已核187classpath条目、Freemarker2.3.35；templates exit0 planFormatVersion2 | 只读目录，未plan/apply |
| cachedRBAC API | local5.4.1 jar tf/javap AuthorizationService | 包含requirePermission/Context/Filter | 二进制签名，不是平台授权成功 |
| tests | 每Step ./mvnw -o -B -ntp -Dtest=指定selectors test | main:test配置隔离，DDL/平台/Redis不自动运行 | Java/H2/MockMvc，不是PG/分片/IdP proof |
| artifacts/tool failure | 默认Maven离线build-classpath实测因FreeMarker仓库origin记录失败 | 不修改settings/remote.repositories，不自动下载 | 错误已记录；existingSurefire runtimeClasspath读取可用，templates已验证 |

生成器执行cwd必须仓库根（outputRoot为仓库相对路径）。执行者先取得经过验证的runtimeClasspath：本轮从已有 `Egon-COLA/.../code-generator/target/surefire-reports/TEST-*.xml` 的java.class.path读取，验证所有路径存在，保存 `/tmp/enjoyshop-product-codegen.cp`，未改Maven配置或下载。这是既有本地测试classpath，并非要求提交它。未来若临时文件已不存在，按同样read-only元数据恢复或由用户提供approved完整classpath；缺失则BLOCKED_TOOLING，不能新造CLI/手写GEN。准确调用：

```bash
# cwd: /Users/mario/SelfProject/enjoyshop
export EGON_CODEGEN_CLASSPATH="$(cat /tmp/enjoyshop-product-codegen.cp)"
/Users/mario/SelfProject/Egon-COLA/scripts/egon-codegen.sh templates
/Users/mario/SelfProject/Egon-COLA/scripts/egon-codegen.sh plan --config enjoyshop-service/enjoyshop-service-goods/codegen/product-persistence-codegen.json
# PRODUCT_PLAN_ID必须从上一条最终JSON读取并验证formatVersion2，不invent/hardcode。
/Users/mario/SelfProject/Egon-COLA/scripts/egon-codegen.sh apply --plan "$PRODUCT_PLAN_ID" --config enjoyshop-service/enjoyshop-service-goods/codegen/product-persistence-codegen.json
/Users/mario/SelfProject/Egon-COLA/scripts/egon-codegen.sh check --config enjoyshop-service/enjoyshop-service-goods/codegen/product-persistence-codegen.json
```

BO两config按同样命令各执行一次；check在任何business extension前必须零冲突。后续手工自定义扩展使原generation hash不同是预期停止条件，不自动更改state/hash隐藏差异。`M/.egon/codegen/state.json`保留原ownership hash，本地plan/candidates/generated state不作为源码commit内容；SQL journal append记录真实输入。无new scripts/work harness：完整gate可由既有JUnit/MockMvc/architecture verify与git diff --check覆盖，新cross-file gate安排在ProductContractConformanceTest持久测试中，未来不留下不明临时脚本。

### 6.3 Immutable constraints and approved decisions

旧三条DDL与旧Manifest entries保持原字节/顺序；仅新增20261009_001。用户已批SINGLE/master_data、标准JSON、稳定SKU ID、重新审核、四类权限与Jianshen5.4.1。无业务事件/异步补偿、前端、运费计算、新Maven模块、newstorage部署。scope只有goods必要生产/测试/配置/文档，平台源码不改。

### 6.4 Plan Clarifications

| ID | Small implementation inference | Repository evidence | Why semantics are unchanged | Impact if wrong |
| --- | --- | --- | --- | --- |
| PLAN-CLAR-001 | GEN拆为persistence和两份domain-model配置，sort policy覆盖表business字段，减少artifact输出 | CLI逐artifact支持及fieldPolicy union源码 | publiccontract/架构不变，仅选择已支持范围 | generation inventory不符即停，不扩大文件 |
| PLAN-CLAR-002 | 新权限fake配置被已有PersistenceTestSupport引入 | 当前所有SpringBootTest均继承此fixture | test-only隔离，生产身份/接口不变 | context失败回Step6fixture，不启外部服务 |
| PLAN-CLAR-003 | restore/delete特殊SQL沿用标准mapper lifecycle IDs，XML定向扩P/E及review字段 | EgonColaMapper.restoreVersionedById/Repository.restoreById与groups | 保持既有guard，P/E业务目标不变；不新增接口 | statement/validation不匹配回Step3/5，不disableguard |
| PLAN-CLAR-004 | Step4未暴露的生命周期槽只能temporary拒绝，Step5替换；同任务不提交/发布中间代码 | 领域端口编译需要全签名、HTTP直到Step7才创建 | 只为中间编译前置，不作为已完成能力或成功stub | 全部合法state用例必须Step5 GREEN否则禁止交付 |

DEC-208是本轮用户指示的Spec修订；GEN/CUSTOM以§10.4真实字段语义为准，既不手写通用catalog替代，也不以模板的固定文件数量排除必要单表转换。

## 7. Ordered File-by-file Implementation Steps

### Step 1 — 建立单版本商品表与可复核生成骨架

- Requirements: REQ-001, REQ-003, REQ-004, REQ-015, REQ-016, REQ-021
- Dependencies: None，当前Light基线
- Baseline state: 当前无商品表/PO/SQL/API，旧工作树仅未跟踪Spec
- Observable outcome: 两新表DDL/manifest和三表持久化骨架可编译，14个catalog输出可复核
- End state: 无商品HTTP、无权限依赖启用；旧SQL不改
- Test-first gate: Required — DDL测试先缺文件失败；GEN能力先用templates/v2审查，不以PG启动证明
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-JSON-001, MC-TIME-001, MC-CONFIG-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001, MC-DEP-001, MC-BLOCKER-001；每项适用性按文件以下具体证据检查，不以通用规则代替实际校验。
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；普通对象/业务Bean/约束/SQL/配置分别按文件职责生效，未涉及电话/Event不新增依赖。
- Ordered files:

#### File 1 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/ddl/ProductDdlContractTest.java`

- Purpose: 先证明单版本/继承字段/约束与旧hash不变
- Symbols: ProductDdlContractTest
- Repository evidence: 现有CatalogDdlContractTest采用Files/Pattern/Jackson/SHA256检查资源，不连接数据库；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: ProductDdlContractTest 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第1个文件；先固定observable RED，再补最小implementation；编译契约前置与实际RED执行点已由Test-first gate说明。
- Contract/signature changes: ProductDdlContractTest；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: SQL字节→SHA256/manifest；MASTER_DATA建spu/sku，SHARD空分支，不重建category_brand。
- Error and edge behavior: 缺脚本RED；旧hash变化/版本重复立即失败；不把静态DDL检查当PG执行。
- Standards impact: MC-TEST-001, MC-SCOPE-001, MC-VALID-001 — JUnit/MockMvc/现有fixture；nativegroups/真实结果断言，不建外部runtime；测试不是businessBean，沿用现有test injection风格。
- Literal rule enforcement: Rule 2, Rule 11 — 所选边界正负例/同模块隔离；不把H2或fake当PG/平台证明。
- Implementation pseudocode:

```java
@Test schema_and_manifest_contract() {
  assertThat(Path.of("src/main/resources/db/egon-mp/V20261009_001__initialize_product_schema.sql")).exists();
  parse CREATE definitions; assert exactly spu,sku; assert eight EgonModel columns and all Spec §11 business columns;
  assert lifecycle+active indexes, parent FK RESTRICT, SKU P/E CHECK and manifest sha256;
  compare three predecessor SQL SHA256 against baseline constants; no live datasource;
}
```

- Verification contribution: ProductDdlContractTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 测试已写；明确RED执行点需要的compile prerequisites完成后运行，不以fixture/环境失败凑RED。

#### File 2 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/resources/db/egon-mp/V20261009_001__initialize_product_schema.sql`

- Purpose: 一次逻辑数据库变更，仅追加新版本
- Symbols: MASTER_DATA spu/sku DDL
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: MASTER_DATA spu/sku DDL 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第2个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: MASTER_DATA spu/sku DDL；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: 完整列型/默认/NULL/字段以Spec §11逐项复制，不能仅以伪代码列数猜字段。
- Error and edge behavior: 非空未纳管schema/role/checksum冲突交MP runner失败关闭；不使用CREATE IF NOT EXISTS掩盖漂移。
- Standards impact: MC-ARCH-001, MC-CONFIG-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001 — 精确manifest/typedkeys/UTC/SQL范围、同profile结构；无Java对象注解装饰配置。
- Literal rule enforcement: Rule 6, Rule 7, Rule 10, Rule 11 — Jackson或SQL实际类型、四profile同键、UTC时间、原Light和MP DDL约束，文档N/A项不引代码。
- Implementation pseudocode:

```sql
DO $egon$ BEGIN
  IF current_setting('egon_migration.role')='MASTER_DATA' THEN
    CREATE TABLE spu (Spec §11.2共同八列 + 全部Spec列；FK brand/category1/2/3/template RESTRICT);
    CREATE TABLE sku (共同八列 + 全部Spec列；FK spu/category RESTRICT);
    CREATE unique lifecycle indexes (tenant_id,sn,deleted_at) and active partial indexes (tenant_id,sn) WHERE deleted_at IS NULL;
    CREATE idx_spu_active_page/idx_spu_deleted_page (tenant_id,id DESC), idx_sku_parent (tenant_id,spu_id,id);
    CHECK spu valid state tuples; CHECK sku active=>deletion_kind IS NULL, deleted=>P/E;
  ELSIF role='SHARD' THEN leave branch empty; ELSE RAISE EXCEPTION; END IF;
END $egon$;
```

- Verification contribution: ProductDdlContractTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 3 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/resources/db/egon-mp/repository-manifest.json`

- Purpose: 追加真实新SQL checksum
- Symbols: scripts最后一条20261009_001
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件存在，应局限所列symbol。
- Dependencies and consumers: scripts最后一条20261009_001 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第3个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: scripts最后一条20261009_001；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: 新脚本字节→SHA256；只有一条新增entry。
- Error and edge behavior: 不存在或越租户统一404；状态/旧版本/唯一冲突409并回滚；错误不泄漏token/SQL；品牌/上传行为保留。
- Standards impact: MC-ARCH-001, MC-CONFIG-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001 — 精确manifest/typedkeys/UTC/SQL范围、同profile结构；无Java对象注解装饰配置。
- Literal rule enforcement: Rule 6, Rule 7, Rule 10, Rule 11 — Jackson或SQL实际类型、四profile同键、UTC时间、原Light和MP DDL约束，文档N/A项不引代码。
- Implementation pseudocode:

```text
read existing scripts and preserve prefix entries byte-for-byte;
sha256 = SHA-256(Files.readAllBytes(V20261009_001__initialize_product_schema.sql));
append {version:"20261009_001",path:"db/egon-mp/V20261009_001__initialize_product_schema.sql",sha256};
assert old three entries unchanged, no sorting or rewriting history;
```

- Verification contribution: ProductDdlContractTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 4 — `CREATE enjoyshop-service/enjoyshop-service-goods/codegen/product-persistence-codegen.json`

- Purpose: 明确每次生成最小初始catalog清单
- Symbols: configVersion1/projectType light/logicalTables
- Repository evidence: 现有codegen/brand-codegen.json及catalog-lookup-codegen.json；CLI ConfigValidator/GenerationScopeValidator；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: configVersion1/projectType light/logicalTables 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第4个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: configVersion1/projectType light/logicalTables；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: 3表持久化12文件+spu/sku各1 BO；单独config避免表字段策略互相误用。
- Error and edge behavior: 输出多文件/CONFLICT/MISSING_TYPE/UNSUPPORTED_DDL立即停止；不使用v1或改ftl绕过。
- Standards impact: MC-ARCH-001, MC-CONFIG-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001 — 精确manifest/typedkeys/UTC/SQL范围、同profile结构；无Java对象注解装饰配置。
- Literal rule enforcement: Rule 6, Rule 7, Rule 10, Rule 11 — Jackson或SQL实际类型、四profile同键、UTC时间、原Light和MP DDL约束，文档N/A项不引代码。
- Implementation pseudocode:

```jsonc
{configVersion:1,projectType:"light",basePackage:"top.egon.enjoyshop.goods",domain:"goods",outputRoot:"enjoyshop-service/enjoyshop-service-goods",
 input:{mode:"manifest",resourceRoot:"enjoyshop-service/enjoyshop-service-goods/src/main/resources",manifest:"db/egon-mp/repository-manifest.json"},
 logicalTables:["spu", "sku", "category_brand"],artifacts:["po", "dao", "mapper-xml", "repo"],existingTypeMappings:{} }
For persistence-only config omit fieldPolicies; domain-model has explicit five policy arrays and sort union covering business fields only;
These configs own general table skeleton/BO only, not Product business field/projection Mappers in Step3; do not select backend-crud/command/controller or generic converter bundle; preserve inherited EgonModel fields, Events absent;
```

- Verification contribution: generator v2 inventory + ProductDdlContractTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 5 — `CREATE enjoyshop-service/enjoyshop-service-goods/codegen/product-spu-model-codegen.json`

- Purpose: 明确每次生成最小初始catalog清单
- Symbols: configVersion1/projectType light/logicalTables
- Repository evidence: 现有codegen/brand-codegen.json及catalog-lookup-codegen.json；CLI ConfigValidator/GenerationScopeValidator；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: configVersion1/projectType light/logicalTables 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第5个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: configVersion1/projectType light/logicalTables；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: 3表持久化12文件+spu/sku各1 BO；单独config避免表字段策略互相误用。
- Error and edge behavior: 输出多文件/CONFLICT/MISSING_TYPE/UNSUPPORTED_DDL立即停止；不使用v1或改ftl绕过。
- Standards impact: MC-ARCH-001, MC-CONFIG-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001 — 精确manifest/typedkeys/UTC/SQL范围、同profile结构；无Java对象注解装饰配置。
- Literal rule enforcement: Rule 6, Rule 7, Rule 10, Rule 11 — Jackson或SQL实际类型、四profile同键、UTC时间、原Light和MP DDL约束，文档N/A项不引代码。
- Implementation pseudocode:

```jsonc
{configVersion:1,projectType:"light",basePackage:"top.egon.enjoyshop.goods",domain:"goods",outputRoot:"enjoyshop-service/enjoyshop-service-goods",
 input:{mode:"manifest",resourceRoot:"enjoyshop-service/enjoyshop-service-goods/src/main/resources",manifest:"db/egon-mp/repository-manifest.json"},
 logicalTables:["spu"],artifacts:["domain-model"],existingTypeMappings:{},
 fieldPolicies:{create:[],update:[],result:[],filter:[],sort:["sn", "name", "caption", "brandId", "category1Id", "category2Id", "category3Id", "templateId", "freightId", "image", "images", "saleService", "introduction", "specItems", "paraItems", "saleNum", "commentNum", "status", "isMarketable", "isEnableSpec"]} }
For persistence-only config omit fieldPolicies; domain-model has explicit five policy arrays and sort union covering business fields only;
These configs own general table skeleton/BO only, not Product business field/projection Mappers in Step3; do not select backend-crud/command/controller or generic converter bundle; preserve inherited EgonModel fields, Events absent;
```

- Verification contribution: generator v2 inventory + ProductDdlContractTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 6 — `CREATE enjoyshop-service/enjoyshop-service-goods/codegen/product-sku-model-codegen.json`

- Purpose: 明确每次生成最小初始catalog清单
- Symbols: configVersion1/projectType light/logicalTables
- Repository evidence: 现有codegen/brand-codegen.json及catalog-lookup-codegen.json；CLI ConfigValidator/GenerationScopeValidator；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: configVersion1/projectType light/logicalTables 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第6个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: configVersion1/projectType light/logicalTables；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: 3表持久化12文件+spu/sku各1 BO；单独config避免表字段策略互相误用。
- Error and edge behavior: 输出多文件/CONFLICT/MISSING_TYPE/UNSUPPORTED_DDL立即停止；不使用v1或改ftl绕过。
- Standards impact: MC-ARCH-001, MC-CONFIG-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001 — 精确manifest/typedkeys/UTC/SQL范围、同profile结构；无Java对象注解装饰配置。
- Literal rule enforcement: Rule 6, Rule 7, Rule 10, Rule 11 — Jackson或SQL实际类型、四profile同键、UTC时间、原Light和MP DDL约束，文档N/A项不引代码。
- Implementation pseudocode:

```jsonc
{configVersion:1,projectType:"light",basePackage:"top.egon.enjoyshop.goods",domain:"goods",outputRoot:"enjoyshop-service/enjoyshop-service-goods",
 input:{mode:"manifest",resourceRoot:"enjoyshop-service/enjoyshop-service-goods/src/main/resources",manifest:"db/egon-mp/repository-manifest.json"},
 logicalTables:["sku"],artifacts:["domain-model"],existingTypeMappings:{},
 fieldPolicies:{create:[],update:[],result:[],filter:[],sort:["spuId", "sn", "name", "price", "num", "alertNum", "weight", "image", "images", "spec", "categoryId", "categoryName", "brandName", "saleNum", "commentNum", "status", "deletionKind"]} }
For persistence-only config omit fieldPolicies; domain-model has explicit five policy arrays and sort union covering business fields only;
These configs own general table skeleton/BO only, not Product business field/projection Mappers in Step3; do not select backend-crud/command/controller or generic converter bundle; preserve inherited EgonModel fields, Events absent;
```

- Verification contribution: generator v2 inventory + ProductDdlContractTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 7 — `GENERATED enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/po/SpuPO.java`

- Purpose: 由native Light生成初始表持久化骨架
- Symbols: SpuPO
- Repository evidence: CodegenCommand/ProjectLayoutStrategy catalog与现有BrandPO/DAO/Repository；文件当前不存在；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: SpuPO 的调用/生成消费方为本Step所列focused测试与下一层；源input/manifest/config及CLI artifact；后续真实Repository/Domain消费。
- Why now: 按本Step严格顺序第7个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: SpuPO；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: 继承八字段，PO仅声明本表business列；DAO extends EgonColaMapper，Repo extends EgonColaRepository；SQL列映射含继承列。
- Error and edge behavior: 任何已有无state文件或hash冲突不得覆盖；生成后先check，再允许后续明确的business扩展。
- Standards impact: MC-ARCH-001, MC-REUSE-001, MC-MODEL-001, MC-VALID-001, MC-BEAN-001, MC-CONVERT-001, MC-SCOPE-001 — catalog由CLI生成；检查EgonModel/DBshape/具名Bean，只选artifact不手写。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 11 — suffix、PO组metadata、class完整Lombok/继承、qualifiedRepo注入按template审查；不能改template绕范围。
- Implementation pseudocode:

```bash
Use config enjoyshop-service/enjoyshop-service-goods/codegen/product-persistence-codegen.json; logical table spu; artifact po;
/Users/mario/SelfProject/Egon-COLA/scripts/egon-codegen.sh plan --config enjoyshop-service/enjoyshop-service-goods/codegen/product-persistence-codegen.json
Inspect formatVersion=2, CREATE actions and exact 12 paths; capture returned planId (never guess);
/Users/mario/SelfProject/Egon-COLA/scripts/egon-codegen.sh apply --plan "$PRODUCT_PLAN_ID" --config enjoyshop-service/enjoyshop-service-goods/codegen/product-persistence-codegen.json
Run once for entire cohort; this file entry describes one selected output, NOT a separate apply;
No handwritten catalog Java/XML; no business endpoint generated;
```

- Verification contribution: generator check before custom extension + LightPersistenceArchitectureTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 8 — `GENERATED enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/dao/SpuDAO.java`

- Purpose: 由native Light生成初始表持久化骨架
- Symbols: SpuDAO
- Repository evidence: CodegenCommand/ProjectLayoutStrategy catalog与现有BrandPO/DAO/Repository；文件当前不存在；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: SpuDAO 的调用/生成消费方为本Step所列focused测试与下一层；源input/manifest/config及CLI artifact；后续真实Repository/Domain消费。
- Why now: 按本Step严格顺序第8个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: SpuDAO；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: 继承八字段，PO仅声明本表business列；DAO extends EgonColaMapper，Repo extends EgonColaRepository；SQL列映射含继承列。
- Error and edge behavior: 任何已有无state文件或hash冲突不得覆盖；生成后先check，再允许后续明确的business扩展。
- Standards impact: MC-ARCH-001, MC-REUSE-001, MC-MODEL-001, MC-VALID-001, MC-BEAN-001, MC-CONVERT-001, MC-SCOPE-001 — catalog由CLI生成；检查EgonModel/DBshape/具名Bean，只选artifact不手写。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 11 — suffix、PO组metadata、class完整Lombok/继承、qualifiedRepo注入按template审查；不能改template绕范围。
- Implementation pseudocode:

```bash
Use config enjoyshop-service/enjoyshop-service-goods/codegen/product-persistence-codegen.json; logical table spu; artifact dao;
/Users/mario/SelfProject/Egon-COLA/scripts/egon-codegen.sh plan --config enjoyshop-service/enjoyshop-service-goods/codegen/product-persistence-codegen.json
Inspect formatVersion=2, CREATE actions and exact 12 paths; capture returned planId (never guess);
/Users/mario/SelfProject/Egon-COLA/scripts/egon-codegen.sh apply --plan "$PRODUCT_PLAN_ID" --config enjoyshop-service/enjoyshop-service-goods/codegen/product-persistence-codegen.json
Run once for entire cohort; this file entry describes one selected output, NOT a separate apply;
No handwritten catalog Java/XML; no business endpoint generated;
```

- Verification contribution: generator check before custom extension + LightPersistenceArchitectureTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 9 — `GENERATED enjoyshop-service/enjoyshop-service-goods/src/main/resources/mybatis/mapper/goods/SpuDAO.xml`

- Purpose: 由native Light生成初始表持久化骨架
- Symbols: SpuDAO
- Repository evidence: CodegenCommand/ProjectLayoutStrategy catalog与现有BrandPO/DAO/Repository；文件当前不存在；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: SpuDAO 的调用/生成消费方为本Step所列focused测试与下一层；源input/manifest/config及CLI artifact；后续真实Repository/Domain消费。
- Why now: 按本Step严格顺序第9个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: SpuDAO；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: 继承八字段，PO仅声明本表business列；DAO extends EgonColaMapper，Repo extends EgonColaRepository；SQL列映射含继承列。
- Error and edge behavior: 任何已有无state文件或hash冲突不得覆盖；生成后先check，再允许后续明确的business扩展。
- Standards impact: MC-ARCH-001, MC-CONFIG-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001 — 精确manifest/typedkeys/UTC/SQL范围、同profile结构；无Java对象注解装饰配置。
- Literal rule enforcement: Rule 6, Rule 7, Rule 10, Rule 11 — Jackson或SQL实际类型、四profile同键、UTC时间、原Light和MP DDL约束，文档N/A项不引代码。
- Implementation pseudocode:

```bash
Use config enjoyshop-service/enjoyshop-service-goods/codegen/product-persistence-codegen.json; logical table spu; artifact mapper-xml;
/Users/mario/SelfProject/Egon-COLA/scripts/egon-codegen.sh plan --config enjoyshop-service/enjoyshop-service-goods/codegen/product-persistence-codegen.json
Inspect formatVersion=2, CREATE actions and exact 12 paths; capture returned planId (never guess);
/Users/mario/SelfProject/Egon-COLA/scripts/egon-codegen.sh apply --plan "$PRODUCT_PLAN_ID" --config enjoyshop-service/enjoyshop-service-goods/codegen/product-persistence-codegen.json
Run once for entire cohort; this file entry describes one selected output, NOT a separate apply;
No handwritten catalog Java/XML; no business endpoint generated;
```

- Verification contribution: generator check before custom extension + LightPersistenceArchitectureTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 10 — `GENERATED enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/repo/SpuRepository.java`

- Purpose: 由native Light生成初始表持久化骨架
- Symbols: SpuRepository
- Repository evidence: CodegenCommand/ProjectLayoutStrategy catalog与现有BrandPO/DAO/Repository；文件当前不存在；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: SpuRepository 的调用/生成消费方为本Step所列focused测试与下一层；源input/manifest/config及CLI artifact；后续真实Repository/Domain消费。
- Why now: 按本Step严格顺序第10个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: SpuRepository；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: 继承八字段，PO仅声明本表business列；DAO extends EgonColaMapper，Repo extends EgonColaRepository；SQL列映射含继承列。
- Error and edge behavior: 任何已有无state文件或hash冲突不得覆盖；生成后先check，再允许后续明确的business扩展。
- Standards impact: MC-ARCH-001, MC-REUSE-001, MC-MODEL-001, MC-VALID-001, MC-BEAN-001, MC-CONVERT-001, MC-SCOPE-001 — catalog由CLI生成；检查EgonModel/DBshape/具名Bean，只选artifact不手写。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 11 — suffix、PO组metadata、class完整Lombok/继承、qualifiedRepo注入按template审查；不能改template绕范围。
- Implementation pseudocode:

```bash
Use config enjoyshop-service/enjoyshop-service-goods/codegen/product-persistence-codegen.json; logical table spu; artifact repo;
/Users/mario/SelfProject/Egon-COLA/scripts/egon-codegen.sh plan --config enjoyshop-service/enjoyshop-service-goods/codegen/product-persistence-codegen.json
Inspect formatVersion=2, CREATE actions and exact 12 paths; capture returned planId (never guess);
/Users/mario/SelfProject/Egon-COLA/scripts/egon-codegen.sh apply --plan "$PRODUCT_PLAN_ID" --config enjoyshop-service/enjoyshop-service-goods/codegen/product-persistence-codegen.json
Run once for entire cohort; this file entry describes one selected output, NOT a separate apply;
No handwritten catalog Java/XML; no business endpoint generated;
```

- Verification contribution: generator check before custom extension + LightPersistenceArchitectureTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 11 — `GENERATED enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/po/SkuPO.java`

- Purpose: 由native Light生成初始表持久化骨架
- Symbols: SkuPO
- Repository evidence: CodegenCommand/ProjectLayoutStrategy catalog与现有BrandPO/DAO/Repository；文件当前不存在；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: SkuPO 的调用/生成消费方为本Step所列focused测试与下一层；源input/manifest/config及CLI artifact；后续真实Repository/Domain消费。
- Why now: 按本Step严格顺序第11个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: SkuPO；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: 继承八字段，PO仅声明本表business列；DAO extends EgonColaMapper，Repo extends EgonColaRepository；SQL列映射含继承列。
- Error and edge behavior: 任何已有无state文件或hash冲突不得覆盖；生成后先check，再允许后续明确的business扩展。
- Standards impact: MC-ARCH-001, MC-REUSE-001, MC-MODEL-001, MC-VALID-001, MC-BEAN-001, MC-CONVERT-001, MC-SCOPE-001 — catalog由CLI生成；检查EgonModel/DBshape/具名Bean，只选artifact不手写。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 11 — suffix、PO组metadata、class完整Lombok/继承、qualifiedRepo注入按template审查；不能改template绕范围。
- Implementation pseudocode:

```bash
Use config enjoyshop-service/enjoyshop-service-goods/codegen/product-persistence-codegen.json; logical table sku; artifact po;
/Users/mario/SelfProject/Egon-COLA/scripts/egon-codegen.sh plan --config enjoyshop-service/enjoyshop-service-goods/codegen/product-persistence-codegen.json
Inspect formatVersion=2, CREATE actions and exact 12 paths; capture returned planId (never guess);
/Users/mario/SelfProject/Egon-COLA/scripts/egon-codegen.sh apply --plan "$PRODUCT_PLAN_ID" --config enjoyshop-service/enjoyshop-service-goods/codegen/product-persistence-codegen.json
Run once for entire cohort; this file entry describes one selected output, NOT a separate apply;
No handwritten catalog Java/XML; no business endpoint generated;
```

- Verification contribution: generator check before custom extension + LightPersistenceArchitectureTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 12 — `GENERATED enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/dao/SkuDAO.java`

- Purpose: 由native Light生成初始表持久化骨架
- Symbols: SkuDAO
- Repository evidence: CodegenCommand/ProjectLayoutStrategy catalog与现有BrandPO/DAO/Repository；文件当前不存在；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: SkuDAO 的调用/生成消费方为本Step所列focused测试与下一层；源input/manifest/config及CLI artifact；后续真实Repository/Domain消费。
- Why now: 按本Step严格顺序第12个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: SkuDAO；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: 继承八字段，PO仅声明本表business列；DAO extends EgonColaMapper，Repo extends EgonColaRepository；SQL列映射含继承列。
- Error and edge behavior: 任何已有无state文件或hash冲突不得覆盖；生成后先check，再允许后续明确的business扩展。
- Standards impact: MC-ARCH-001, MC-REUSE-001, MC-MODEL-001, MC-VALID-001, MC-BEAN-001, MC-CONVERT-001, MC-SCOPE-001 — catalog由CLI生成；检查EgonModel/DBshape/具名Bean，只选artifact不手写。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 11 — suffix、PO组metadata、class完整Lombok/继承、qualifiedRepo注入按template审查；不能改template绕范围。
- Implementation pseudocode:

```bash
Use config enjoyshop-service/enjoyshop-service-goods/codegen/product-persistence-codegen.json; logical table sku; artifact dao;
/Users/mario/SelfProject/Egon-COLA/scripts/egon-codegen.sh plan --config enjoyshop-service/enjoyshop-service-goods/codegen/product-persistence-codegen.json
Inspect formatVersion=2, CREATE actions and exact 12 paths; capture returned planId (never guess);
/Users/mario/SelfProject/Egon-COLA/scripts/egon-codegen.sh apply --plan "$PRODUCT_PLAN_ID" --config enjoyshop-service/enjoyshop-service-goods/codegen/product-persistence-codegen.json
Run once for entire cohort; this file entry describes one selected output, NOT a separate apply;
No handwritten catalog Java/XML; no business endpoint generated;
```

- Verification contribution: generator check before custom extension + LightPersistenceArchitectureTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 13 — `GENERATED enjoyshop-service/enjoyshop-service-goods/src/main/resources/mybatis/mapper/goods/SkuDAO.xml`

- Purpose: 由native Light生成初始表持久化骨架
- Symbols: SkuDAO
- Repository evidence: CodegenCommand/ProjectLayoutStrategy catalog与现有BrandPO/DAO/Repository；文件当前不存在；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: SkuDAO 的调用/生成消费方为本Step所列focused测试与下一层；源input/manifest/config及CLI artifact；后续真实Repository/Domain消费。
- Why now: 按本Step严格顺序第13个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: SkuDAO；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: 继承八字段，PO仅声明本表business列；DAO extends EgonColaMapper，Repo extends EgonColaRepository；SQL列映射含继承列。
- Error and edge behavior: 任何已有无state文件或hash冲突不得覆盖；生成后先check，再允许后续明确的business扩展。
- Standards impact: MC-ARCH-001, MC-CONFIG-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001 — 精确manifest/typedkeys/UTC/SQL范围、同profile结构；无Java对象注解装饰配置。
- Literal rule enforcement: Rule 6, Rule 7, Rule 10, Rule 11 — Jackson或SQL实际类型、四profile同键、UTC时间、原Light和MP DDL约束，文档N/A项不引代码。
- Implementation pseudocode:

```bash
Use config enjoyshop-service/enjoyshop-service-goods/codegen/product-persistence-codegen.json; logical table sku; artifact mapper-xml;
/Users/mario/SelfProject/Egon-COLA/scripts/egon-codegen.sh plan --config enjoyshop-service/enjoyshop-service-goods/codegen/product-persistence-codegen.json
Inspect formatVersion=2, CREATE actions and exact 12 paths; capture returned planId (never guess);
/Users/mario/SelfProject/Egon-COLA/scripts/egon-codegen.sh apply --plan "$PRODUCT_PLAN_ID" --config enjoyshop-service/enjoyshop-service-goods/codegen/product-persistence-codegen.json
Run once for entire cohort; this file entry describes one selected output, NOT a separate apply;
No handwritten catalog Java/XML; no business endpoint generated;
```

- Verification contribution: generator check before custom extension + LightPersistenceArchitectureTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 14 — `GENERATED enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/repo/SkuRepository.java`

- Purpose: 由native Light生成初始表持久化骨架
- Symbols: SkuRepository
- Repository evidence: CodegenCommand/ProjectLayoutStrategy catalog与现有BrandPO/DAO/Repository；文件当前不存在；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: SkuRepository 的调用/生成消费方为本Step所列focused测试与下一层；源input/manifest/config及CLI artifact；后续真实Repository/Domain消费。
- Why now: 按本Step严格顺序第14个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: SkuRepository；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: 继承八字段，PO仅声明本表business列；DAO extends EgonColaMapper，Repo extends EgonColaRepository；SQL列映射含继承列。
- Error and edge behavior: 任何已有无state文件或hash冲突不得覆盖；生成后先check，再允许后续明确的business扩展。
- Standards impact: MC-ARCH-001, MC-REUSE-001, MC-MODEL-001, MC-VALID-001, MC-BEAN-001, MC-CONVERT-001, MC-SCOPE-001 — catalog由CLI生成；检查EgonModel/DBshape/具名Bean，只选artifact不手写。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 11 — suffix、PO组metadata、class完整Lombok/继承、qualifiedRepo注入按template审查；不能改template绕范围。
- Implementation pseudocode:

```bash
Use config enjoyshop-service/enjoyshop-service-goods/codegen/product-persistence-codegen.json; logical table sku; artifact repo;
/Users/mario/SelfProject/Egon-COLA/scripts/egon-codegen.sh plan --config enjoyshop-service/enjoyshop-service-goods/codegen/product-persistence-codegen.json
Inspect formatVersion=2, CREATE actions and exact 12 paths; capture returned planId (never guess);
/Users/mario/SelfProject/Egon-COLA/scripts/egon-codegen.sh apply --plan "$PRODUCT_PLAN_ID" --config enjoyshop-service/enjoyshop-service-goods/codegen/product-persistence-codegen.json
Run once for entire cohort; this file entry describes one selected output, NOT a separate apply;
No handwritten catalog Java/XML; no business endpoint generated;
```

- Verification contribution: generator check before custom extension + LightPersistenceArchitectureTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 15 — `GENERATED enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/po/CategoryBrandPO.java`

- Purpose: 由native Light生成初始表持久化骨架
- Symbols: CategoryBrandPO
- Repository evidence: CodegenCommand/ProjectLayoutStrategy catalog与现有BrandPO/DAO/Repository；文件当前不存在；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: CategoryBrandPO 的调用/生成消费方为本Step所列focused测试与下一层；源input/manifest/config及CLI artifact；后续真实Repository/Domain消费。
- Why now: 按本Step严格顺序第15个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: CategoryBrandPO；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: 继承八字段，PO仅声明本表business列；DAO extends EgonColaMapper，Repo extends EgonColaRepository；SQL列映射含继承列。
- Error and edge behavior: 任何已有无state文件或hash冲突不得覆盖；生成后先check，再允许后续明确的business扩展。
- Standards impact: MC-ARCH-001, MC-REUSE-001, MC-MODEL-001, MC-VALID-001, MC-BEAN-001, MC-CONVERT-001, MC-SCOPE-001 — catalog由CLI生成；检查EgonModel/DBshape/具名Bean，只选artifact不手写。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 11 — suffix、PO组metadata、class完整Lombok/继承、qualifiedRepo注入按template审查；不能改template绕范围。
- Implementation pseudocode:

```bash
Use config enjoyshop-service/enjoyshop-service-goods/codegen/product-persistence-codegen.json; logical table category_brand; artifact po;
/Users/mario/SelfProject/Egon-COLA/scripts/egon-codegen.sh plan --config enjoyshop-service/enjoyshop-service-goods/codegen/product-persistence-codegen.json
Inspect formatVersion=2, CREATE actions and exact 12 paths; capture returned planId (never guess);
/Users/mario/SelfProject/Egon-COLA/scripts/egon-codegen.sh apply --plan "$PRODUCT_PLAN_ID" --config enjoyshop-service/enjoyshop-service-goods/codegen/product-persistence-codegen.json
Run once for entire cohort; this file entry describes one selected output, NOT a separate apply;
No handwritten catalog Java/XML; no business endpoint generated;
```

- Verification contribution: generator check before custom extension + LightPersistenceArchitectureTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 16 — `GENERATED enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/dao/CategoryBrandDAO.java`

- Purpose: 由native Light生成初始表持久化骨架
- Symbols: CategoryBrandDAO
- Repository evidence: CodegenCommand/ProjectLayoutStrategy catalog与现有BrandPO/DAO/Repository；文件当前不存在；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: CategoryBrandDAO 的调用/生成消费方为本Step所列focused测试与下一层；源input/manifest/config及CLI artifact；后续真实Repository/Domain消费。
- Why now: 按本Step严格顺序第16个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: CategoryBrandDAO；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: 继承八字段，PO仅声明本表business列；DAO extends EgonColaMapper，Repo extends EgonColaRepository；SQL列映射含继承列。
- Error and edge behavior: 任何已有无state文件或hash冲突不得覆盖；生成后先check，再允许后续明确的business扩展。
- Standards impact: MC-ARCH-001, MC-REUSE-001, MC-MODEL-001, MC-VALID-001, MC-BEAN-001, MC-CONVERT-001, MC-SCOPE-001 — catalog由CLI生成；检查EgonModel/DBshape/具名Bean，只选artifact不手写。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 11 — suffix、PO组metadata、class完整Lombok/继承、qualifiedRepo注入按template审查；不能改template绕范围。
- Implementation pseudocode:

```bash
Use config enjoyshop-service/enjoyshop-service-goods/codegen/product-persistence-codegen.json; logical table category_brand; artifact dao;
/Users/mario/SelfProject/Egon-COLA/scripts/egon-codegen.sh plan --config enjoyshop-service/enjoyshop-service-goods/codegen/product-persistence-codegen.json
Inspect formatVersion=2, CREATE actions and exact 12 paths; capture returned planId (never guess);
/Users/mario/SelfProject/Egon-COLA/scripts/egon-codegen.sh apply --plan "$PRODUCT_PLAN_ID" --config enjoyshop-service/enjoyshop-service-goods/codegen/product-persistence-codegen.json
Run once for entire cohort; this file entry describes one selected output, NOT a separate apply;
No handwritten catalog Java/XML; no business endpoint generated;
```

- Verification contribution: generator check before custom extension + LightPersistenceArchitectureTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 17 — `GENERATED enjoyshop-service/enjoyshop-service-goods/src/main/resources/mybatis/mapper/goods/CategoryBrandDAO.xml`

- Purpose: 由native Light生成初始表持久化骨架
- Symbols: CategoryBrandDAO
- Repository evidence: CodegenCommand/ProjectLayoutStrategy catalog与现有BrandPO/DAO/Repository；文件当前不存在；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: CategoryBrandDAO 的调用/生成消费方为本Step所列focused测试与下一层；源input/manifest/config及CLI artifact；后续真实Repository/Domain消费。
- Why now: 按本Step严格顺序第17个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: CategoryBrandDAO；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: 继承八字段，PO仅声明本表business列；DAO extends EgonColaMapper，Repo extends EgonColaRepository；SQL列映射含继承列。
- Error and edge behavior: 任何已有无state文件或hash冲突不得覆盖；生成后先check，再允许后续明确的business扩展。
- Standards impact: MC-ARCH-001, MC-CONFIG-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001 — 精确manifest/typedkeys/UTC/SQL范围、同profile结构；无Java对象注解装饰配置。
- Literal rule enforcement: Rule 6, Rule 7, Rule 10, Rule 11 — Jackson或SQL实际类型、四profile同键、UTC时间、原Light和MP DDL约束，文档N/A项不引代码。
- Implementation pseudocode:

```bash
Use config enjoyshop-service/enjoyshop-service-goods/codegen/product-persistence-codegen.json; logical table category_brand; artifact mapper-xml;
/Users/mario/SelfProject/Egon-COLA/scripts/egon-codegen.sh plan --config enjoyshop-service/enjoyshop-service-goods/codegen/product-persistence-codegen.json
Inspect formatVersion=2, CREATE actions and exact 12 paths; capture returned planId (never guess);
/Users/mario/SelfProject/Egon-COLA/scripts/egon-codegen.sh apply --plan "$PRODUCT_PLAN_ID" --config enjoyshop-service/enjoyshop-service-goods/codegen/product-persistence-codegen.json
Run once for entire cohort; this file entry describes one selected output, NOT a separate apply;
No handwritten catalog Java/XML; no business endpoint generated;
```

- Verification contribution: generator check before custom extension + LightPersistenceArchitectureTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 18 — `GENERATED enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/repo/CategoryBrandRepository.java`

- Purpose: 由native Light生成初始表持久化骨架
- Symbols: CategoryBrandRepository
- Repository evidence: CodegenCommand/ProjectLayoutStrategy catalog与现有BrandPO/DAO/Repository；文件当前不存在；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: CategoryBrandRepository 的调用/生成消费方为本Step所列focused测试与下一层；源input/manifest/config及CLI artifact；后续真实Repository/Domain消费。
- Why now: 按本Step严格顺序第18个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: CategoryBrandRepository；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: 继承八字段，PO仅声明本表business列；DAO extends EgonColaMapper，Repo extends EgonColaRepository；SQL列映射含继承列。
- Error and edge behavior: 任何已有无state文件或hash冲突不得覆盖；生成后先check，再允许后续明确的business扩展。
- Standards impact: MC-ARCH-001, MC-REUSE-001, MC-MODEL-001, MC-VALID-001, MC-BEAN-001, MC-CONVERT-001, MC-SCOPE-001 — catalog由CLI生成；检查EgonModel/DBshape/具名Bean，只选artifact不手写。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 11 — suffix、PO组metadata、class完整Lombok/继承、qualifiedRepo注入按template审查；不能改template绕范围。
- Implementation pseudocode:

```bash
Use config enjoyshop-service/enjoyshop-service-goods/codegen/product-persistence-codegen.json; logical table category_brand; artifact repo;
/Users/mario/SelfProject/Egon-COLA/scripts/egon-codegen.sh plan --config enjoyshop-service/enjoyshop-service-goods/codegen/product-persistence-codegen.json
Inspect formatVersion=2, CREATE actions and exact 12 paths; capture returned planId (never guess);
/Users/mario/SelfProject/Egon-COLA/scripts/egon-codegen.sh apply --plan "$PRODUCT_PLAN_ID" --config enjoyshop-service/enjoyshop-service-goods/codegen/product-persistence-codegen.json
Run once for entire cohort; this file entry describes one selected output, NOT a separate apply;
No handwritten catalog Java/XML; no business endpoint generated;
```

- Verification contribution: generator check before custom extension + LightPersistenceArchitectureTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 19 — `GENERATED enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/SpuBO.java`

- Purpose: 生成明确表字段与id/version基线，后续业务元数据由已声明扩展补齐
- Symbols: SpuBO
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: SpuBO 的调用/生成消费方为本Step所列focused测试与下一层；源input/manifest/config及CLI artifact；后续真实Repository/Domain消费。
- Why now: 按本Step严格顺序第19个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: SpuBO；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: TEXT字段初始String、审计字段尚不在模板；Step2声明业务扩展typedJSON与审计元数据，不伪称模板已支持这些字段。
- Error and edge behavior: 不存在或越租户统一404；状态/旧版本/唯一冲突409并回滚；错误不泄漏token/SQL；品牌/上传行为保留。
- Standards impact: MC-ARCH-001, MC-REUSE-001, MC-MODEL-001, MC-VALID-001, MC-BEAN-001, MC-CONVERT-001, MC-SCOPE-001 — catalog由CLI生成；检查EgonModel/DBshape/具名Bean，只选artifact不手写。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 11 — suffix、PO组metadata、class完整Lombok/继承、qualifiedRepo注入按template审查；不能改template绕范围。
- Implementation pseudocode:

```text
Use config enjoyshop-service/enjoyshop-service-goods/codegen/product-spu-model-codegen.json, artifact domain-model only;
run scripts/egon-codegen.sh plan --config <this exact config>; inspect one path domain/goods/SpuBO.java and v2 planId;
apply that planId with same config; check before agent-owned business extensions;
Generated class Lombok @Data/@NoArgsConstructor/@AllArgsConstructor/@Accessors(chain=true)/@Builder;
Only business fields from selected policies + id/version; no generated converters/Commands/controllers;
```

- Verification contribution: generator one-file inventory / ProductConverterTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 20 — `GENERATED enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/SkuBO.java`

- Purpose: 生成明确表字段与id/version基线，后续业务元数据由已声明扩展补齐
- Symbols: SkuBO
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: SkuBO 的调用/生成消费方为本Step所列focused测试与下一层；源input/manifest/config及CLI artifact；后续真实Repository/Domain消费。
- Why now: 按本Step严格顺序第20个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: SkuBO；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: TEXT字段初始String、审计字段尚不在模板；Step2声明业务扩展typedJSON与审计元数据，不伪称模板已支持这些字段。
- Error and edge behavior: 不存在或越租户统一404；状态/旧版本/唯一冲突409并回滚；错误不泄漏token/SQL；品牌/上传行为保留。
- Standards impact: MC-ARCH-001, MC-REUSE-001, MC-MODEL-001, MC-VALID-001, MC-BEAN-001, MC-CONVERT-001, MC-SCOPE-001 — catalog由CLI生成；检查EgonModel/DBshape/具名Bean，只选artifact不手写。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 11 — suffix、PO组metadata、class完整Lombok/继承、qualifiedRepo注入按template审查；不能改template绕范围。
- Implementation pseudocode:

```text
Use config enjoyshop-service/enjoyshop-service-goods/codegen/product-sku-model-codegen.json, artifact domain-model only;
run scripts/egon-codegen.sh plan --config <this exact config>; inspect one path domain/goods/SkuBO.java and v2 planId;
apply that planId with same config; check before agent-owned business extensions;
Generated class Lombok @Data/@NoArgsConstructor/@AllArgsConstructor/@Accessors(chain=true)/@Builder;
Only business fields from selected policies + id/version; no generated converters/Commands/controllers;
```

- Verification contribution: generator one-file inventory / ProductConverterTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 21 — `MODIFY docs/egon/codegen/ddl-consumption-log.md`

- Purpose: 保留每次生成SQL输入证据，不能当数据库历史
- Symbols: append three generator consumption entries
- Repository evidence: 现有append-only ddl-consumption-log.md；generator state与manifest source；当前文件存在，应局限所列symbol。
- Dependencies and consumers: append three generator consumption entries 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第21个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: append three generator consumption entries；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: 严格按商品Spec的字段/默认/null/状态/版本定义映射；业务ID十进制字符串，持久BIGINT；审计Instant/UTC deletedAt微秒。
- Error and edge behavior: 不存在或越租户统一404；状态/旧版本/唯一冲突409并回滚；错误不泄漏token/SQL；品牌/上传行为保留。
- Standards impact: MC-ARCH-001, MC-CONFIG-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001 — 精确manifest/typedkeys/UTC/SQL范围、同profile结构；无Java对象注解装饰配置。
- Literal rule enforcement: Rule 6, Rule 7, Rule 10, Rule 11 — Jackson或SQL实际类型、四profile同键、UTC时间、原Light和MP DDL约束，文档N/A项不引代码。
- Implementation pseudocode:

```text
After each successful apply (or plan is last authorized generator action), append Asia/Shanghai timestamp/profile light/outputRoot/throughVersion20261009_001;
for every consumed manifest script record actual version/path/SHA256 and full SQL bytes, in manifest order;
three distinct configs consume same manifest; append separate timed entries, never rewrite earlier entries;
No database connection and no statement that DDL ran;
```

- Verification contribution: ProductDdlContractTest + journal review；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 22 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/architecture/LightPersistenceArchitectureTest.java`

- Purpose: 同步真实新三表的结构计数但保留架构限制
- Symbols: counts + exact persistence file manifest
- Repository evidence: 现有该测试counts均为10/10/10/8，本轮源码计数已核实；当前文件存在，应局限所列symbol。
- Dependencies and consumers: counts + exact persistence file manifest 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第22个文件；先固定observable RED，再补最小implementation；编译契约前置与实际RED执行点已由Test-first gate说明。
- Contract/signature changes: counts + exact persistence file manifest；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: 只增加本Step真实生成文件，不删除样例、不降低依赖规则。
- Error and edge behavior: 不存在或越租户统一404；状态/旧版本/唯一冲突409并回滚；错误不泄漏token/SQL；品牌/上传行为保留。
- Standards impact: MC-TEST-001, MC-SCOPE-001, MC-VALID-001 — JUnit/MockMvc/现有fixture；nativegroups/真实结果断言，不建外部runtime；测试不是businessBean，沿用现有test injection风格。
- Literal rule enforcement: Rule 2, Rule 11 — 所选边界正负例/同模块隔离；不把H2或fake当PG/平台证明。
- Implementation pseudocode:

```java
assert exact PO/DAO/Repository inventories now include Spu,Sku,CategoryBrand;
assertEquals(13, count(PO)); assertEquals(13,count(DAO)); assertEquals(13,count(Repository));
DomainServiceImpl remains 8 until Step4, when exact ProductDomainServiceImpl makes9;
preserve no JPA/AR/direct DAO business service/third layer/bootstrapper invariants;
```

- Verification contribution: LightPersistenceArchitectureTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 测试已写；明确RED执行点需要的compile prerequisites完成后运行，不以fixture/环境失败凑RED。

#### File 23 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/ddl/CatalogDdlContractTest.java`

- Purpose: 兼容追加商品Manifest版本，保留旧三条前缀与SQL hash断言。
- Symbols: shouldAppendOnlyTheNextManifestVersionWithTheExactSqlChecksum。
- Repository evidence: 现有第103行把scripts总数写死3，实际追加后9项回归出现1项失败。
- Dependencies and consumers: Step1单版本Manifest；旧目录DDL契约回归，不改变生产API。
- Why now: 用户本轮“按照推荐方案做”明确批准补入Step1。
- Contract/signature changes: hasSize(3)改hasSizeGreaterThanOrEqualTo(3)，其余prefix/hash断言不变。
- Input/output and state mapping: 旧三条固定prefix与hash保持，后续追加允许；新条目的准确数量/hash由ProductDdlContractTest负责。
- Error and edge behavior: 缺旧条目、重排、旧hash变化仍失败；不改已有SQL或采用新hash。
- Standards impact: MC-SCOPE-001, MC-TEST-001 — 用户明确批准必要回归修复，只改一条总数断言。
- Literal rule enforcement: Rule 11 — 原Light/MP追加迁移约定保持，无DDL历史改写。
- Implementation pseudocode:

```java
assertThat(scripts).hasSizeGreaterThanOrEqualTo(3);
assertManifestEntry(scripts.get(0), originalRepositoryVersion, originalRepositoryHash);
assertManifestEntry(scripts.get(1), originalBrandVersion, originalBrandHash);
assertManifestEntry(scripts.get(2), originalCatalogVersion, sha256(originalCatalogBytes));
```

- Verification contribution: CatalogDdlContractTest与ProductDdlContractTest/LightPersistenceArchitectureTest共同9项回归。
- After this file: 追加版本不再错误触发旧总数断言，旧prefix约束保持。

- Validation working directory: `/Users/mario/SelfProject/enjoyshop/enjoyshop-service/enjoyshop-service-goods`；GEN plan/apply的cwd另为`/Users/mario/SelfProject/enjoyshop`。
- Verification command: `./mvnw -o -B -ntp -Dtest=ProductDdlContractTest,LightPersistenceArchitectureTest test`；`./mvnw -o -B -ntp -DskipTests compile`；generator三cohort check在business extension前，明确14输出，SQL journal逐次append。
- Expected result: exit0，所有selector测试实际执行、0失败/错误；跳过/无test或classpath不解析不算PASS；两新表DDL/manifest和三表持久化骨架可编译，14个catalog输出可复核。
- Failure returns to: 本Step负责该断言的File；DDL/hash/generator冲突回Step1，字段mapping回Step2/3，事务与state回Step4/5，身份/fixture回Step6，wire/OAS回Step7；GEN冲突或实际schema/API不符仍立即停止，不伪造tooling输出。
- Completion criteria: 本Step全部REQ有focused证据，Manual Check逐项有真实结果；最终SINGLE/tenant/版本/字段/旧边界与主Spec一致；无无关diff。
- Rollback: 未执行DDL前仅恢复本Step拥有文件且保留用户preexistingchanges；若用户已运行DDL则保留历史SQL/checksum，只允许后续forward-fix，不能自动DROP/repair。
- Commit paths: enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/ddl/ProductDdlContractTest.java; enjoyshop-service/enjoyshop-service-goods/src/main/resources/db/egon-mp/V20261009_001__initialize_product_schema.sql; enjoyshop-service/enjoyshop-service-goods/src/main/resources/db/egon-mp/repository-manifest.json; enjoyshop-service/enjoyshop-service-goods/codegen/product-persistence-codegen.json; enjoyshop-service/enjoyshop-service-goods/codegen/product-spu-model-codegen.json; enjoyshop-service/enjoyshop-service-goods/codegen/product-sku-model-codegen.json; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/po/SpuPO.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/dao/SpuDAO.java; enjoyshop-service/enjoyshop-service-goods/src/main/resources/mybatis/mapper/goods/SpuDAO.xml; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/repo/SpuRepository.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/po/SkuPO.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/dao/SkuDAO.java; enjoyshop-service/enjoyshop-service-goods/src/main/resources/mybatis/mapper/goods/SkuDAO.xml; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/repo/SkuRepository.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/po/CategoryBrandPO.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/dao/CategoryBrandDAO.java; enjoyshop-service/enjoyshop-service-goods/src/main/resources/mybatis/mapper/goods/CategoryBrandDAO.xml; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/repo/CategoryBrandRepository.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/SpuBO.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/SkuBO.java; docs/egon/codegen/ddl-consumption-log.md; enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/architecture/LightPersistenceArchitectureTest.java; enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/ddl/CatalogDdlContractTest.java
- Commit: Deferred — 用户要求完整logical task最多1次commit；以上路径仅本Step归属，不创建阶段commit。

### Step 2 — 定义商品边界模型与纯字段校验

- Requirements: REQ-001, REQ-002, REQ-003, REQ-005, REQ-006, REQ-013, REQ-015
- Dependencies: Step 1 GREEN checkpoint（按用户约束无逐Step commit）
- Baseline state: 前一Step的明确End state与focused tests已验证；已存在路径只改本Step列出的符号
- Observable outcome: 标准JSON/分组/有界SKU组合可以独立校验，不泄漏持久字段
- End state: 没有商品HTTP/DB行为；native模型前置后composition RED→GREEN
- Test-first gate: Required — 先契约载体保证编译，随后测试invalid composition应返回违规而实际无约束→RED；再实现validator/decoder与annotation
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-JSON-001, MC-TIME-001, MC-CONFIG-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001；每项适用性按文件以下具体证据检查，不以通用规则代替实际校验。
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；普通对象/业务Bean/约束/SQL/配置分别按文件职责生效，未涉及电话/Event不新增依赖。
- Ordered files:

#### File 1 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/validation/ProductValidationGroup.java`

- Purpose: 建立Spec已有的准确语义载体或枚举
- Symbols: Create,Update,HttpStateInput,StateCall,Detail,Page
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: Create,Update,HttpStateInput,StateCall,Detail,Page 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第1个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: Create,Update,HttpStateInput,StateCall,Detail,Page；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: group marker interfaces; Create/Update extend Default for native constraints; HttpStateInput prohibits spuId; StateCall requires positive spuId/version;。
- Error and edge behavior: 这里只定义契约编译前置，无DB/平台/业务默认授权；组合规格行为尚未实现，随后的RED用例必须失败。
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-PATTERN-001, MC-SCOPE-001 — carrier用完整class Lombok；business namedBean+Slf4j+RequiredArgsConstructor/finalQualifier；converter继承Common；不可变record仅私有规则值；annotation/enum/interface无非法@Data。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11 — 语义suffix/具体组、class注解、BaseConverter、Bean命名及lombok传播、JDK/Commons、Jackson、Specification仅复杂lifecycle、java.time、exactLight；按实际类型执行而非机械加注解。
- Implementation pseudocode:

```java
declare ProductValidationGroup in top.egon.enjoyshop.goods.common.validation;
fields/signatures: Create,Update,HttpStateInput,StateCall,Detail,Page;
group marker interfaces; Create/Update extend Default for native constraints; HttpStateInput prohibits spuId; StateCall requires positive spuId/version;
ordinary carriers @Data/@NoArgsConstructor/@AllArgsConstructor/@Accessors(chain=true)/@Builder; enums/marker interfaces do not use illegal data-class annotations;
unknown input rejected by @JsonAnySetter only for Commands; tests in ProductCompositionValidationTest/ProductConverterTest;
```

- Verification contribution: ProductCompositionValidationTest,ProductConverterTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 2 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/SpuSaveCommand.java`

- Purpose: 建立Spec已有的准确语义载体或枚举
- Symbols: sn,name,caption,brandId,category1Id,category2Id,category3Id,templateId,freightId,image,images,saleService,introduction,isEnableSpec,specItems,paraItems
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: sn,name,caption,brandId,category1Id,category2Id,category3Id,templateId,freightId,image,images,saleService,introduction,isEnableSpec,specItems,paraItems 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第2个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: sn,name,caption,brandId,category1Id,category2Id,category3Id,templateId,freightId,image,images,saleService,introduction,isEnableSpec,specItems,paraItems；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: class with full Lombok; @Valid/numeric-ID custom decoder; @NotBlank sn/name, lengths64/200/300/20000; JSON Maps/Lists bounded; defaults empty arrays/objects; no audit/status fields;。
- Error and edge behavior: 这里只定义契约编译前置，无DB/平台/业务默认授权；组合规格行为尚未实现，随后的RED用例必须失败。
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-PATTERN-001, MC-SCOPE-001 — carrier用完整class Lombok；business namedBean+Slf4j+RequiredArgsConstructor/finalQualifier；converter继承Common；不可变record仅私有规则值；annotation/enum/interface无非法@Data。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11 — 语义suffix/具体组、class注解、BaseConverter、Bean命名及lombok传播、JDK/Commons、Jackson、Specification仅复杂lifecycle、java.time、exactLight；按实际类型执行而非机械加注解。
- Implementation pseudocode:

```java
declare SpuSaveCommand in top.egon.enjoyshop.goods.application.goods;
fields/signatures: sn,name,caption,brandId,category1Id,category2Id,category3Id,templateId,freightId,image,images,saleService,introduction,isEnableSpec,specItems,paraItems;
class with full Lombok; @Valid and Long ID compile contract (custom decoder binding is added later in this Step); @NotBlank sn/name, lengths64/200/300/20000; JSON Maps/Lists bounded; defaults empty arrays/objects; no audit/status fields;
ordinary carriers @Data/@NoArgsConstructor/@AllArgsConstructor/@Accessors(chain=true)/@Builder; enums/marker interfaces do not use illegal data-class annotations;
unknown input rejected by @JsonAnySetter only for Commands; tests in ProductCompositionValidationTest/ProductConverterTest;
```

- Verification contribution: ProductCompositionValidationTest,ProductConverterTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 3 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/SkuSaveCommand.java`

- Purpose: 建立Spec已有的准确语义载体或枚举
- Symbols: id,sn,num,alertNum,price,weight,spec,image,images,status
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: id,sn,num,alertNum,price,weight,spec,image,images,status 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第3个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: id,sn,num,alertNum,price,weight,spec,image,images,status；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: class; id @Null(Create)/@Positive(Update) nullable means new; @NotBlank sn64; integer cents price>=1, quantities>=0; status string1/2; @JsonAnySetter rejects unknown;。
- Error and edge behavior: 这里只定义契约编译前置，无DB/平台/业务默认授权；组合规格行为尚未实现，随后的RED用例必须失败。
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-PATTERN-001, MC-SCOPE-001 — carrier用完整class Lombok；business namedBean+Slf4j+RequiredArgsConstructor/finalQualifier；converter继承Common；不可变record仅私有规则值；annotation/enum/interface无非法@Data。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11 — 语义suffix/具体组、class注解、BaseConverter、Bean命名及lombok传播、JDK/Commons、Jackson、Specification仅复杂lifecycle、java.time、exactLight；按实际类型执行而非机械加注解。
- Implementation pseudocode:

```java
declare SkuSaveCommand in top.egon.enjoyshop.goods.application.goods;
fields/signatures: id,sn,num,alertNum,price,weight,spec,image,images,status;
class; id @Null(Create)/@Positive(Update) nullable means new; @NotBlank sn64; integer cents price>=1, quantities>=0; status string1/2; @JsonAnySetter rejects unknown;
ordinary carriers @Data/@NoArgsConstructor/@AllArgsConstructor/@Accessors(chain=true)/@Builder; enums/marker interfaces do not use illegal data-class annotations;
unknown input rejected by @JsonAnySetter only for Commands; tests in ProductCompositionValidationTest/ProductConverterTest;
```

- Verification contribution: ProductCompositionValidationTest,ProductConverterTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 4 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/ProductSaveCommand.java`

- Purpose: 建立Spec已有的准确语义载体或枚举
- Symbols: SpuSaveCommand spu,List<SkuSaveCommand> skuList,Long version
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: SpuSaveCommand spu,List<SkuSaveCommand> skuList,Long version 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第4个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: SpuSaveCommand spu,List<SkuSaveCommand> skuList,Long version；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: class; @NotNull/@Valid spu, list @Size1..100/@Valid elements; version @Null(Create) and @NotNull/Min0(Update); initially only native contract constraints; add composition annotation AFTER RED test;。
- Error and edge behavior: 这里只定义契约编译前置，无DB/平台/业务默认授权；组合规格行为尚未实现，随后的RED用例必须失败。
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-PATTERN-001, MC-SCOPE-001 — carrier用完整class Lombok；business namedBean+Slf4j+RequiredArgsConstructor/finalQualifier；converter继承Common；不可变record仅私有规则值；annotation/enum/interface无非法@Data。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11 — 语义suffix/具体组、class注解、BaseConverter、Bean命名及lombok传播、JDK/Commons、Jackson、Specification仅复杂lifecycle、java.time、exactLight；按实际类型执行而非机械加注解。
- Implementation pseudocode:

```java
declare ProductSaveCommand in top.egon.enjoyshop.goods.application.goods;
fields/signatures: SpuSaveCommand spu,List<SkuSaveCommand> skuList,Long version;
class; @NotNull/@Valid spu, list @Size1..100/@Valid elements; version @Null(Create) and @NotNull/Min0(Update); initially only native contract constraints; add composition annotation AFTER RED test;
ordinary carriers @Data/@NoArgsConstructor/@AllArgsConstructor/@Accessors(chain=true)/@Builder; enums/marker interfaces do not use illegal data-class annotations;
unknown input rejected by @JsonAnySetter only for Commands; tests in ProductCompositionValidationTest/ProductConverterTest;
```

- Verification contribution: ProductCompositionValidationTest,ProductConverterTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 5 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/ProductIdQuery.java`

- Purpose: 建立Spec已有的准确语义载体或枚举
- Symbols: Long spuId,Boolean deleted
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: Long spuId,Boolean deleted 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第5个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: Long spuId,Boolean deleted；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: class; positive ID; deleted default false; @Validated Detail; no body/defaulttenant;。
- Error and edge behavior: 这里只定义契约编译前置，无DB/平台/业务默认授权；组合规格行为尚未实现，随后的RED用例必须失败。
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-PATTERN-001, MC-SCOPE-001 — carrier用完整class Lombok；business namedBean+Slf4j+RequiredArgsConstructor/finalQualifier；converter继承Common；不可变record仅私有规则值；annotation/enum/interface无非法@Data。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11 — 语义suffix/具体组、class注解、BaseConverter、Bean命名及lombok传播、JDK/Commons、Jackson、Specification仅复杂lifecycle、java.time、exactLight；按实际类型执行而非机械加注解。
- Implementation pseudocode:

```java
declare ProductIdQuery in top.egon.enjoyshop.goods.application.goods;
fields/signatures: Long spuId,Boolean deleted;
class; positive ID; deleted default false; @Validated Detail; no body/defaulttenant;
ordinary carriers @Data/@NoArgsConstructor/@AllArgsConstructor/@Accessors(chain=true)/@Builder; enums/marker interfaces do not use illegal data-class annotations;
unknown input rejected by @JsonAnySetter only for Commands; tests in ProductCompositionValidationTest/ProductConverterTest;
```

- Verification contribution: ProductCompositionValidationTest,ProductConverterTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 6 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/ProductPageQuery.java`

- Purpose: 建立Spec已有的准确语义载体或枚举
- Symbols: pageNo,pageSize,deleted,sn,name,status,isMarketable
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: pageNo,pageSize,deleted,sn,name,status,isMarketable 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第6个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: pageNo,pageSize,deleted,sn,name,status,isMarketable；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: class; page1,size10 max100; deleted false; exact sn/containsname; no sort; reject deleted=true+status1/marketabletrue and numeric offset overflow;。
- Error and edge behavior: 这里只定义契约编译前置，无DB/平台/业务默认授权；组合规格行为尚未实现，随后的RED用例必须失败。
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-PATTERN-001, MC-SCOPE-001 — carrier用完整class Lombok；business namedBean+Slf4j+RequiredArgsConstructor/finalQualifier；converter继承Common；不可变record仅私有规则值；annotation/enum/interface无非法@Data。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11 — 语义suffix/具体组、class注解、BaseConverter、Bean命名及lombok传播、JDK/Commons、Jackson、Specification仅复杂lifecycle、java.time、exactLight；按实际类型执行而非机械加注解。
- Implementation pseudocode:

```java
declare ProductPageQuery in top.egon.enjoyshop.goods.application.goods;
fields/signatures: pageNo,pageSize,deleted,sn,name,status,isMarketable;
class; page1,size10 max100; deleted false; exact sn/containsname; no sort; reject deleted=true+status1/marketabletrue and numeric offset overflow;
ordinary carriers @Data/@NoArgsConstructor/@AllArgsConstructor/@Accessors(chain=true)/@Builder; enums/marker interfaces do not use illegal data-class annotations;
unknown input rejected by @JsonAnySetter only for Commands; tests in ProductCompositionValidationTest/ProductConverterTest;
```

- Verification contribution: ProductCompositionValidationTest,ProductConverterTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 7 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/ProductLifecycleCommand.java`

- Purpose: 建立Spec已有的准确语义载体或枚举
- Symbols: Long spuId,Long version
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: Long spuId,Long version 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第7个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: Long spuId,Long version；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: class; spuId @Null(HttpStateInput), Positive/NotNull(StateCall); version Min0/NotNull; Controller fills id after body validation;。
- Error and edge behavior: 这里只定义契约编译前置，无DB/平台/业务默认授权；组合规格行为尚未实现，随后的RED用例必须失败。
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-PATTERN-001, MC-SCOPE-001 — carrier用完整class Lombok；business namedBean+Slf4j+RequiredArgsConstructor/finalQualifier；converter继承Common；不可变record仅私有规则值；annotation/enum/interface无非法@Data。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11 — 语义suffix/具体组、class注解、BaseConverter、Bean命名及lombok传播、JDK/Commons、Jackson、Specification仅复杂lifecycle、java.time、exactLight；按实际类型执行而非机械加注解。
- Implementation pseudocode:

```java
declare ProductLifecycleCommand in top.egon.enjoyshop.goods.application.goods;
fields/signatures: Long spuId,Long version;
class; spuId @Null(HttpStateInput), Positive/NotNull(StateCall); version Min0/NotNull; Controller fills id after body validation;
ordinary carriers @Data/@NoArgsConstructor/@AllArgsConstructor/@Accessors(chain=true)/@Builder; enums/marker interfaces do not use illegal data-class annotations;
unknown input rejected by @JsonAnySetter only for Commands; tests in ProductCompositionValidationTest/ProductConverterTest;
```

- Verification contribution: ProductCompositionValidationTest,ProductConverterTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 8 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/ProductActionEnum.java`

- Purpose: 建立Spec已有的准确语义载体或枚举
- Symbols: CREATE,EDIT,AUDIT,PULL,PUT,DELETE,RESTORE,PURGE
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: CREATE,EDIT,AUDIT,PULL,PUT,DELETE,RESTORE,PURGE 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第8个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: CREATE,EDIT,AUDIT,PULL,PUT,DELETE,RESTORE,PURGE；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: closed action enum; never dispatch from client string; persistent ordinal prohibited;。
- Error and edge behavior: 这里只定义契约编译前置，无DB/平台/业务默认授权；组合规格行为尚未实现，随后的RED用例必须失败。
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-PATTERN-001, MC-SCOPE-001 — carrier用完整class Lombok；business namedBean+Slf4j+RequiredArgsConstructor/finalQualifier；converter继承Common；不可变record仅私有规则值；annotation/enum/interface无非法@Data。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11 — 语义suffix/具体组、class注解、BaseConverter、Bean命名及lombok传播、JDK/Commons、Jackson、Specification仅复杂lifecycle、java.time、exactLight；按实际类型执行而非机械加注解。
- Implementation pseudocode:

```java
declare ProductActionEnum in top.egon.enjoyshop.goods.domain.goods;
fields/signatures: CREATE,EDIT,AUDIT,PULL,PUT,DELETE,RESTORE,PURGE;
closed action enum; never dispatch from client string; persistent ordinal prohibited;
ordinary carriers @Data/@NoArgsConstructor/@AllArgsConstructor/@Accessors(chain=true)/@Builder; enums/marker interfaces do not use illegal data-class annotations;
unknown input rejected by @JsonAnySetter only for Commands; tests in ProductCompositionValidationTest/ProductConverterTest;
```

- Verification contribution: ProductCompositionValidationTest,ProductConverterTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 9 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/ProductSkuDeletionKindEnum.java`

- Purpose: 建立Spec已有的准确语义载体或枚举
- Symbols: PRODUCT("P"),EDIT("E")
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: PRODUCT("P"),EDIT("E") 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第9个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: PRODUCT("P"),EDIT("E")；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: explicit code @EnumValue, frontend projection hides this field; @JsonValue if serialized in diagnostic fixture; unknown codes reject;。
- Error and edge behavior: 这里只定义契约编译前置，无DB/平台/业务默认授权；组合规格行为尚未实现，随后的RED用例必须失败。
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-PATTERN-001, MC-SCOPE-001 — carrier用完整class Lombok；business namedBean+Slf4j+RequiredArgsConstructor/finalQualifier；converter继承Common；不可变record仅私有规则值；annotation/enum/interface无非法@Data。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11 — 语义suffix/具体组、class注解、BaseConverter、Bean命名及lombok传播、JDK/Commons、Jackson、Specification仅复杂lifecycle、java.time、exactLight；按实际类型执行而非机械加注解。
- Implementation pseudocode:

```java
declare ProductSkuDeletionKindEnum in top.egon.enjoyshop.goods.domain.goods;
fields/signatures: PRODUCT("P"),EDIT("E");
explicit code @EnumValue, frontend projection hides this field; @JsonValue if serialized in diagnostic fixture; unknown codes reject;
ordinary carriers @Data/@NoArgsConstructor/@AllArgsConstructor/@Accessors(chain=true)/@Builder; enums/marker interfaces do not use illegal data-class annotations;
unknown input rejected by @JsonAnySetter only for Commands; tests in ProductCompositionValidationTest/ProductConverterTest;
```

- Verification contribution: ProductCompositionValidationTest,ProductConverterTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 10 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/ProductBO.java`

- Purpose: 建立Spec已有的准确语义载体或枚举
- Symbols: SpuBO spu,List<SkuBO> skuList,Long expectedVersion
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: SpuBO spu,List<SkuBO> skuList,Long expectedVersion 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第10个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: SpuBO spu,List<SkuBO> skuList,Long expectedVersion；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: class/@Builder; nested @Valid and Domain groups; BO fields internal, no direct HTTP PO;。
- Error and edge behavior: 这里只定义契约编译前置，无DB/平台/业务默认授权；组合规格行为尚未实现，随后的RED用例必须失败。
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-PATTERN-001, MC-SCOPE-001 — carrier用完整class Lombok；business namedBean+Slf4j+RequiredArgsConstructor/finalQualifier；converter继承Common；不可变record仅私有规则值；annotation/enum/interface无非法@Data。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11 — 语义suffix/具体组、class注解、BaseConverter、Bean命名及lombok传播、JDK/Commons、Jackson、Specification仅复杂lifecycle、java.time、exactLight；按实际类型执行而非机械加注解。
- Implementation pseudocode:

```java
declare ProductBO in top.egon.enjoyshop.goods.domain.goods;
fields/signatures: SpuBO spu,List<SkuBO> skuList,Long expectedVersion;
class/@Builder; nested @Valid and Domain groups; BO fields internal, no direct HTTP PO;
ordinary carriers @Data/@NoArgsConstructor/@AllArgsConstructor/@Accessors(chain=true)/@Builder; enums/marker interfaces do not use illegal data-class annotations;
unknown input rejected by @JsonAnySetter only for Commands; tests in ProductCompositionValidationTest/ProductConverterTest;
```

- Verification contribution: ProductCompositionValidationTest,ProductConverterTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 11 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/ProductStateBO.java`

- Purpose: 建立Spec已有的准确语义载体或枚举
- Symbols: id,status,marketable,deleted,version,hasActiveSku,referencesValid
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: id,status,marketable,deleted,version,hasActiveSku,referencesValid 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第11个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: id,status,marketable,deleted,version,hasActiveSku,referencesValid；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: class; immutable rule registry returns new instance; last two derived transaction facts not DB fields;。
- Error and edge behavior: 这里只定义契约编译前置，无DB/平台/业务默认授权；组合规格行为尚未实现，随后的RED用例必须失败。
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-PATTERN-001, MC-SCOPE-001 — carrier用完整class Lombok；business namedBean+Slf4j+RequiredArgsConstructor/finalQualifier；converter继承Common；不可变record仅私有规则值；annotation/enum/interface无非法@Data。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11 — 语义suffix/具体组、class注解、BaseConverter、Bean命名及lombok传播、JDK/Commons、Jackson、Specification仅复杂lifecycle、java.time、exactLight；按实际类型执行而非机械加注解。
- Implementation pseudocode:

```java
declare ProductStateBO in top.egon.enjoyshop.goods.domain.goods;
fields/signatures: id,status,marketable,deleted,version,hasActiveSku,referencesValid;
class; immutable rule registry returns new instance; last two derived transaction facts not DB fields;
ordinary carriers @Data/@NoArgsConstructor/@AllArgsConstructor/@Accessors(chain=true)/@Builder; enums/marker interfaces do not use illegal data-class annotations;
unknown input rejected by @JsonAnySetter only for Commands; tests in ProductCompositionValidationTest/ProductConverterTest;
```

- Verification contribution: ProductCompositionValidationTest,ProductConverterTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 12 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/ProductDomainQuery.java`

- Purpose: 建立Spec已有的准确语义载体或枚举
- Symbols: id,deleted,page,name,sn,status,isMarketable
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: id,deleted,page,name,sn,status,isMarketable 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第12个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: id,deleted,page,name,sn,status,isMarketable；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: class; Detail/Page group constraints, mandatory query scope; Core PageQuery reuse; no tenant header field;。
- Error and edge behavior: 这里只定义契约编译前置，无DB/平台/业务默认授权；组合规格行为尚未实现，随后的RED用例必须失败。
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-PATTERN-001, MC-SCOPE-001 — carrier用完整class Lombok；business namedBean+Slf4j+RequiredArgsConstructor/finalQualifier；converter继承Common；不可变record仅私有规则值；annotation/enum/interface无非法@Data。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11 — 语义suffix/具体组、class注解、BaseConverter、Bean命名及lombok传播、JDK/Commons、Jackson、Specification仅复杂lifecycle、java.time、exactLight；按实际类型执行而非机械加注解。
- Implementation pseudocode:

```java
declare ProductDomainQuery in top.egon.enjoyshop.goods.domain.goods;
fields/signatures: id,deleted,page,name,sn,status,isMarketable;
class; Detail/Page group constraints, mandatory query scope; Core PageQuery reuse; no tenant header field;
ordinary carriers @Data/@NoArgsConstructor/@AllArgsConstructor/@Accessors(chain=true)/@Builder; enums/marker interfaces do not use illegal data-class annotations;
unknown input rejected by @JsonAnySetter only for Commands; tests in ProductCompositionValidationTest/ProductConverterTest;
```

- Verification contribution: ProductCompositionValidationTest,ProductConverterTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 13 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/CatalogReferenceBO.java`

- Purpose: 建立Spec已有的准确语义载体或枚举
- Symbols: brandId,brandName,category1Id,category2Id,category3Id,categoryName,templateId,orderedSpecs,paraOptions
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: brandId,brandName,category1Id,category2Id,category3Id,categoryName,templateId,orderedSpecs,paraOptions 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第13个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: brandId,brandName,category1Id,category2Id,category3Id,categoryName,templateId,orderedSpecs,paraOptions；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: class; typed projection; orderedSpecs reuse existing SpecBO; paraOptions ordered Map<String,List<String>>; all joins explicit tenant;。
- Error and edge behavior: 这里只定义契约编译前置，无DB/平台/业务默认授权；组合规格行为尚未实现，随后的RED用例必须失败。
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-PATTERN-001, MC-SCOPE-001 — carrier用完整class Lombok；business namedBean+Slf4j+RequiredArgsConstructor/finalQualifier；converter继承Common；不可变record仅私有规则值；annotation/enum/interface无非法@Data。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11 — 语义suffix/具体组、class注解、BaseConverter、Bean命名及lombok传播、JDK/Commons、Jackson、Specification仅复杂lifecycle、java.time、exactLight；按实际类型执行而非机械加注解。
- Implementation pseudocode:

```java
declare CatalogReferenceBO in top.egon.enjoyshop.goods.domain.goods;
fields/signatures: brandId,brandName,category1Id,category2Id,category3Id,categoryName,templateId,orderedSpecs,paraOptions;
class; typed projection; orderedSpecs reuse existing SpecBO; paraOptions ordered Map<String,List<String>>; all joins explicit tenant;
ordinary carriers @Data/@NoArgsConstructor/@AllArgsConstructor/@Accessors(chain=true)/@Builder; enums/marker interfaces do not use illegal data-class annotations;
unknown input rejected by @JsonAnySetter only for Commands; tests in ProductCompositionValidationTest/ProductConverterTest;
```

- Verification contribution: ProductCompositionValidationTest,ProductConverterTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 14 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/ProductResult.java`

- Purpose: 建立Spec已有的准确语义载体或枚举
- Symbols: SpuResult spu,List<SkuResult> skuList
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: SpuResult spu,List<SkuResult> skuList 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第14个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: SpuResult spu,List<SkuResult> skuList；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: class; one aggregate output shared create/update/detail; full json envelope ownedController;。
- Error and edge behavior: 这里只定义契约编译前置，无DB/平台/业务默认授权；组合规格行为尚未实现，随后的RED用例必须失败。
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-PATTERN-001, MC-SCOPE-001 — carrier用完整class Lombok；business namedBean+Slf4j+RequiredArgsConstructor/finalQualifier；converter继承Common；不可变record仅私有规则值；annotation/enum/interface无非法@Data。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11 — 语义suffix/具体组、class注解、BaseConverter、Bean命名及lombok传播、JDK/Commons、Jackson、Specification仅复杂lifecycle、java.time、exactLight；按实际类型执行而非机械加注解。
- Implementation pseudocode:

```java
declare ProductResult in top.egon.enjoyshop.goods.application.goods;
fields/signatures: SpuResult spu,List<SkuResult> skuList;
class; one aggregate output shared create/update/detail; full json envelope ownedController;
ordinary carriers @Data/@NoArgsConstructor/@AllArgsConstructor/@Accessors(chain=true)/@Builder; enums/marker interfaces do not use illegal data-class annotations;
unknown input rejected by @JsonAnySetter only for Commands; tests in ProductCompositionValidationTest/ProductConverterTest;
```

- Verification contribution: ProductCompositionValidationTest,ProductConverterTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 15 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/SpuResult.java`

- Purpose: 建立Spec已有的准确语义载体或枚举
- Symbols: all §9 SPU output fields: id + inputfields + saleNum/commentNum/status/isMarketable/isDelete/version/createTime/updateTime/deletedAt
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: all §9 SPU output fields: id + inputfields + saleNum/commentNum/status/isMarketable/isDelete/version/createTime/updateTime/deletedAt 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第15个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: all §9 SPU output fields: id + inputfields + saleNum/commentNum/status/isMarketable/isDelete/version/createTime/updateTime/deletedAt；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: class; @JsonInclude ALWAYS for documented nullable deletedAt; per-ID ToStringSerializer inclfreight; readonly metadata; Instant ISO UTC;。
- Error and edge behavior: 这里只定义契约编译前置，无DB/平台/业务默认授权；组合规格行为尚未实现，随后的RED用例必须失败。
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-PATTERN-001, MC-SCOPE-001 — carrier用完整class Lombok；business namedBean+Slf4j+RequiredArgsConstructor/finalQualifier；converter继承Common；不可变record仅私有规则值；annotation/enum/interface无非法@Data。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11 — 语义suffix/具体组、class注解、BaseConverter、Bean命名及lombok传播、JDK/Commons、Jackson、Specification仅复杂lifecycle、java.time、exactLight；按实际类型执行而非机械加注解。
- Implementation pseudocode:

```java
declare SpuResult in top.egon.enjoyshop.goods.application.goods;
fields/signatures: all §9 SPU output fields: id + inputfields + saleNum/commentNum/status/isMarketable/isDelete/version/createTime/updateTime/deletedAt;
class; @JsonInclude ALWAYS for documented nullable deletedAt; per-ID ToStringSerializer inclfreight; readonly metadata; Instant ISO UTC;
ordinary carriers @Data/@NoArgsConstructor/@AllArgsConstructor/@Accessors(chain=true)/@Builder; enums/marker interfaces do not use illegal data-class annotations;
unknown input rejected by @JsonAnySetter only for Commands; tests in ProductCompositionValidationTest/ProductConverterTest;
```

- Verification contribution: ProductCompositionValidationTest,ProductConverterTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 16 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/SkuResult.java`

- Purpose: 建立Spec已有的准确语义载体或枚举
- Symbols: id,spuId,sn,num,alertNum,price,weight,spec,image,images,status,name,categoryId,categoryName,brandName,saleNum,commentNum,createTime,updateTime
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: id,spuId,sn,num,alertNum,price,weight,spec,image,images,status,name,categoryId,categoryName,brandName,saleNum,commentNum,createTime,updateTime 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第16个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: id,spuId,sn,num,alertNum,price,weight,spec,image,images,status,name,categoryId,categoryName,brandName,saleNum,commentNum,createTime,updateTime；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: class; exact §9 wire, no tenant/actor/deletionKind/version pollution; ID string;。
- Error and edge behavior: 这里只定义契约编译前置，无DB/平台/业务默认授权；组合规格行为尚未实现，随后的RED用例必须失败。
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-PATTERN-001, MC-SCOPE-001 — carrier用完整class Lombok；business namedBean+Slf4j+RequiredArgsConstructor/finalQualifier；converter继承Common；不可变record仅私有规则值；annotation/enum/interface无非法@Data。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11 — 语义suffix/具体组、class注解、BaseConverter、Bean命名及lombok传播、JDK/Commons、Jackson、Specification仅复杂lifecycle、java.time、exactLight；按实际类型执行而非机械加注解。
- Implementation pseudocode:

```java
declare SkuResult in top.egon.enjoyshop.goods.application.goods;
fields/signatures: id,spuId,sn,num,alertNum,price,weight,spec,image,images,status,name,categoryId,categoryName,brandName,saleNum,commentNum,createTime,updateTime;
class; exact §9 wire, no tenant/actor/deletionKind/version pollution; ID string;
ordinary carriers @Data/@NoArgsConstructor/@AllArgsConstructor/@Accessors(chain=true)/@Builder; enums/marker interfaces do not use illegal data-class annotations;
unknown input rejected by @JsonAnySetter only for Commands; tests in ProductCompositionValidationTest/ProductConverterTest;
```

- Verification contribution: ProductCompositionValidationTest,ProductConverterTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 17 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/ProductSummaryResult.java`

- Purpose: 建立Spec已有的准确语义载体或枚举
- Symbols: id,sn,name,image,status,isMarketable,isDelete,version
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: id,sn,name,image,status,isMarketable,isDelete,version 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第17个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: id,sn,name,image,status,isMarketable,isDelete,version；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: class; bounded list projection excludes SKU/detailsHTML; Long IDs string;。
- Error and edge behavior: 这里只定义契约编译前置，无DB/平台/业务默认授权；组合规格行为尚未实现，随后的RED用例必须失败。
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-PATTERN-001, MC-SCOPE-001 — carrier用完整class Lombok；business namedBean+Slf4j+RequiredArgsConstructor/finalQualifier；converter继承Common；不可变record仅私有规则值；annotation/enum/interface无非法@Data。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11 — 语义suffix/具体组、class注解、BaseConverter、Bean命名及lombok传播、JDK/Commons、Jackson、Specification仅复杂lifecycle、java.time、exactLight；按实际类型执行而非机械加注解。
- Implementation pseudocode:

```java
declare ProductSummaryResult in top.egon.enjoyshop.goods.application.goods;
fields/signatures: id,sn,name,image,status,isMarketable,isDelete,version;
class; bounded list projection excludes SKU/detailsHTML; Long IDs string;
ordinary carriers @Data/@NoArgsConstructor/@AllArgsConstructor/@Accessors(chain=true)/@Builder; enums/marker interfaces do not use illegal data-class annotations;
unknown input rejected by @JsonAnySetter only for Commands; tests in ProductCompositionValidationTest/ProductConverterTest;
```

- Verification contribution: ProductCompositionValidationTest,ProductConverterTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 18 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/ProductStateResult.java`

- Purpose: 建立Spec已有的准确语义载体或枚举
- Symbols: id,status,isMarketable,isDelete,version
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: id,status,isMarketable,isDelete,version 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第18个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: id,status,isMarketable,isDelete,version；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: class; state command output, no ProductStateBO leaks; ID string;。
- Error and edge behavior: 这里只定义契约编译前置，无DB/平台/业务默认授权；组合规格行为尚未实现，随后的RED用例必须失败。
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-PATTERN-001, MC-SCOPE-001 — carrier用完整class Lombok；business namedBean+Slf4j+RequiredArgsConstructor/finalQualifier；converter继承Common；不可变record仅私有规则值；annotation/enum/interface无非法@Data。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11 — 语义suffix/具体组、class注解、BaseConverter、Bean命名及lombok传播、JDK/Commons、Jackson、Specification仅复杂lifecycle、java.time、exactLight；按实际类型执行而非机械加注解。
- Implementation pseudocode:

```java
declare ProductStateResult in top.egon.enjoyshop.goods.application.goods;
fields/signatures: id,status,isMarketable,isDelete,version;
class; state command output, no ProductStateBO leaks; ID string;
ordinary carriers @Data/@NoArgsConstructor/@AllArgsConstructor/@Accessors(chain=true)/@Builder; enums/marker interfaces do not use illegal data-class annotations;
unknown input rejected by @JsonAnySetter only for Commands; tests in ProductCompositionValidationTest/ProductConverterTest;
```

- Verification contribution: ProductCompositionValidationTest,ProductConverterTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 19 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/SpuBO.java`

- Purpose: 在已生成BO基线扩展真实商品业务字段，不重建catalog
- Symbols: SpuBO business/audit field semantics
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: SpuBO business/audit field semantics 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第19个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: SpuBO business/audit field semantics；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: 原有id/version保持；metadata from PO，不接受HTTP；JSON fieldencoding仅存储边界，不做全对象JSON转换。
- Error and edge behavior: 不存在或越租户统一404；状态/旧版本/唯一冲突409并回滚；错误不泄漏token/SQL；品牌/上传行为保留。
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-PATTERN-001, MC-SCOPE-001 — carrier用完整class Lombok；business namedBean+Slf4j+RequiredArgsConstructor/finalQualifier；converter继承Common；不可变record仅私有规则值；annotation/enum/interface无非法@Data。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11 — 语义suffix/具体组、class注解、BaseConverter、Bean命名及lombok传播、JDK/Commons、Jackson、Specification仅复杂lifecycle、java.time、exactLight；按实际类型执行而非机械加注解。
- Implementation pseudocode:

```java
Start from GENERATED SpuBO baseline, preserve id/version identity and Lombok class construction;
replace JSON TEXT business String fields with exact typed Maps/Lists from Spec §10, via persistence converter field codec;
add tenantId/createUserId/createTime/updateUserId/updateTime/deletedAt internal metadata required for read/edit/restore;
Instant for audit, UTC LocalDateTime for deletedAt; not an EgonModel/PO subtype and never directly returned to HTTP;
record agent-extension ownership; later generator check may report content conflict and MUST NOT overwrite;
```

- Verification contribution: ProductConverterTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 20 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/SkuBO.java`

- Purpose: 在已生成BO基线扩展真实商品业务字段，不重建catalog
- Symbols: SkuBO business/audit field semantics
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: SkuBO business/audit field semantics 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第20个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: SkuBO business/audit field semantics；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: 原有id/version保持；metadata from PO，不接受HTTP；JSON fieldencoding仅存储边界，不做全对象JSON转换。
- Error and edge behavior: 不存在或越租户统一404；状态/旧版本/唯一冲突409并回滚；错误不泄漏token/SQL；品牌/上传行为保留。
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-PATTERN-001, MC-SCOPE-001 — carrier用完整class Lombok；business namedBean+Slf4j+RequiredArgsConstructor/finalQualifier；converter继承Common；不可变record仅私有规则值；annotation/enum/interface无非法@Data。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11 — 语义suffix/具体组、class注解、BaseConverter、Bean命名及lombok传播、JDK/Commons、Jackson、Specification仅复杂lifecycle、java.time、exactLight；按实际类型执行而非机械加注解。
- Implementation pseudocode:

```java
Start from GENERATED SkuBO baseline, preserve id/version identity and Lombok class construction;
replace JSON TEXT business String fields with exact typed Maps/Lists from Spec §10, via persistence converter field codec;
add tenantId/createUserId/createTime/updateUserId/updateTime/deletedAt internal metadata required for read/edit/restore;
Instant for audit, UTC LocalDateTime for deletedAt; not an EgonModel/PO subtype and never directly returned to HTTP;
record agent-extension ownership; later generator check may report content conflict and MUST NOT overwrite;
```

- Verification contribution: ProductConverterTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 21 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/common/validation/ProductCompositionValidationTest.java`

- Purpose: 在组合约束/decoder实现前运行行为RED
- Symbols: invalidComposition,unknownFields,groups
- Repository evidence: GoodsCatalogQueryValidationTest/SpecCatalogQueryValidationTest使用JakartaValidator，模型已在本Step前置使测试可编译；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: invalidComposition,unknownFields,groups 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第21个文件；先固定observable RED，再补最小implementation；编译契约前置与实际RED执行点已由Test-first gate说明。
- Contract/signature changes: invalidComposition,unknownFields,groups；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: 非法输入→字段path+400；无需目录DB校验，composition测试只管纯字段。
- Error and edge behavior: 不存在或越租户统一404；状态/旧版本/唯一冲突409并回滚；错误不泄漏token/SQL；品牌/上传行为保留。
- Standards impact: MC-TEST-001, MC-SCOPE-001, MC-VALID-001 — JUnit/MockMvc/现有fixture；nativegroups/真实结果断言，不建外部runtime；测试不是businessBean，沿用现有test injection风格。
- Literal rule enforcement: Rule 2, Rule 11 — 所选边界正负例/同模块隔离；不把H2或fake当PG/平台证明。
- Implementation pseudocode:

```java
@Test invalid_compositions_fail() {
  construct native-valid ProductSaveCommand with two SKU when isEnableSpec=false;
  assert validator.validate(command,Create.class) has composition violation; // baseline empty violations => RED
  vary empty/101/null SKU, duplicate id/sn/canonicalSpec, unknown spec values, enabled map mismatch;
  assert each group null/id/version rule and nested field path matches Spec;
}
@Test malformed_id_wire_rejected() { bind known Command with numeric/scientific/out-of-range IDs; expect binding failure; }
```

- Verification contribution: ProductCompositionValidationTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 测试已写；明确RED执行点需要的compile prerequisites完成后运行，不以fixture/环境失败凑RED。

#### File 22 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/json/ProductIdDeserializer.java`

- Purpose: 只为新商品Long ID实现严格数字字符串读取
- Symbols: deserialize(JsonParser,DeserializationContext)
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: deserialize(JsonParser,DeserializationContext) 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第22个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: deserialize(JsonParser,DeserializationContext)；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: ID string→Long；服务端snowflake输出仍ToStringSerializer。
- Error and edge behavior: 新typed decoder自身不认证、不打印token；旧Brand Long binding不改变。
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-PATTERN-001, MC-SCOPE-001 — carrier用完整class Lombok；business namedBean+Slf4j+RequiredArgsConstructor/finalQualifier；converter继承Common；不可变record仅私有规则值；annotation/enum/interface无非法@Data。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11 — 语义suffix/具体组、class注解、BaseConverter、Bean命名及lombok传播、JDK/Commons、Jackson、Specification仅复杂lifecycle、java.time、exactLight；按实际类型执行而非机械加注解。
- Implementation pseudocode:

```java
extend Jackson StdDeserializer<Long>; no custom JSON library or Spring Bean;
require token VALUE_STRING; accept canonical digits 0 or [1-9][0-9]* within Long range;
nonfreight IDs additionally @Positive group reject0, freight permits0; reject numeric token, leading0, exponent, negative and blank;
raise JsonMappingException/invalid-format; assign @JsonDeserialize only new Command ID fields, not global ObjectMapper;
```

- Verification contribution: ProductCompositionValidationTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 23 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/validation/ValidProductComposition.java`

- Purpose: 注册复用的跨字段约束
- Symbols: @Constraint(validatedBy=ProductCompositionValidator.class)
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: @Constraint(validatedBy=ProductCompositionValidator.class) 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第23个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: @Constraint(validatedBy=ProductCompositionValidator.class)；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: constraint注册→nested spu/skuList相对fieldpath。
- Error and edge behavior: 不存在或越租户统一404；状态/旧版本/唯一冲突409并回滚；错误不泄漏token/SQL；品牌/上传行为保留。
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-PATTERN-001, MC-SCOPE-001 — carrier用完整class Lombok；business namedBean+Slf4j+RequiredArgsConstructor/finalQualifier；converter继承Common；不可变record仅私有规则值；annotation/enum/interface无非法@Data。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11 — 语义suffix/具体组、class注解、BaseConverter、Bean命名及lombok传播、JDK/Commons、Jackson、Specification仅复杂lifecycle、java.time、exactLight；按实际类型执行而非机械加注解。
- Implementation pseudocode:

```java
@Target(TYPE) @Retention(RUNTIME) @Constraint(validatedBy=ProductCompositionValidator.class)
@interface ValidProductComposition { message/groups/payload per Jakarta contract; }
apply to ProductSaveCommand with Create/Update groups, cascade remains @Valid;
validator has no Repository/AuthorizationService dependency; state checks stay Domain transaction;
```

- Verification contribution: ProductCompositionValidationTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 24 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/validation/ProductCompositionValidator.java`

- Purpose: 让纯组合规则RED转GREEN
- Symbols: isValid(ProductSaveCommand,ConstraintValidatorContext)
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: isValid(ProductSaveCommand,ConstraintValidatorContext) 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第24个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: isValid(ProductSaveCommand,ConstraintValidatorContext)；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: 模板名存在性及库存销售状态不在此validator，留给真实引用/Domain。
- Error and edge behavior: 不存在或越租户统一404；状态/旧版本/唯一冲突409并回滚；错误不泄漏token/SQL；品牌/上传行为保留。
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-PATTERN-001, MC-SCOPE-001 — carrier用完整class Lombok；business namedBean+Slf4j+RequiredArgsConstructor/finalQualifier；converter继承Common；不可变record仅私有规则值；annotation/enum/interface无非法@Data。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11 — 语义suffix/具体组、class注解、BaseConverter、Bean命名及lombok传播、JDK/Commons、Jackson、Specification仅复杂lifecycle、java.time、exactLight；按实际类型执行而非机械加注解。
- Implementation pseudocode:

```java
@Component("productCompositionValidator") @Slf4j
isValid(command,context): leave missing/null native violations to native metadata;
if isEnableSpec=false require exactly1 SKU, empty specItems and empty each spec; else every SKU chosen key exists and value belongs to advertised values;
canonicalize spec key/value pairs by stable key ordering for uniqueness; reject duplicate SKU IDs/sn/spec combination and null/empty members;
add exact skuList[index].spec/path violations; no DB reads, clocks, status transitions or utility rule registry;
```

- Verification contribution: ProductCompositionValidationTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 25 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/ProductSaveCommand.java`

- Purpose: 关联已实现组合约束与明确组
- Symbols: class-level ValidProductComposition
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: class-level ValidProductComposition 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第25个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: class-level ValidProductComposition；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: 已定义输入→纯组合约束生效，无业务写入。
- Error and edge behavior: 不存在或越租户统一404；状态/旧版本/唯一冲突409并回滚；错误不泄漏token/SQL；品牌/上传行为保留。
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-PATTERN-001, MC-SCOPE-001 — carrier用完整class Lombok；business namedBean+Slf4j+RequiredArgsConstructor/finalQualifier；converter继承Common；不可变record仅私有规则值；annotation/enum/interface无非法@Data。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11 — 语义suffix/具体组、class注解、BaseConverter、Bean命名及lombok传播、JDK/Commons、Jackson、Specification仅复杂lifecycle、java.time、exactLight；按实际类型执行而非机械加注解。
- Implementation pseudocode:

```java
retain native fields/cascade and Lombok shape;
@ValidProductComposition(groups={Create.class,Update.class}) on ProductSaveCommand;
Create version remains null, Update version nonnull>=0; do not reuse weak Default for both behaviors;
run focused Validator test: correct nested violations for every invalid composition, valid singleton passes;
```

- Verification contribution: ProductCompositionValidationTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 26 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/SpuSaveCommand.java`

- Purpose: decoder存在后仅绑定新商品字段，关闭严格ID RED
- Symbols: SpuSaveCommand ID binding
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: SpuSaveCommand ID binding 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第26个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: SpuSaveCommand ID binding；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: 只为已有Long字段绑定已实现codec，没有新外部参数。
- Error and edge behavior: 不存在或越租户统一404；状态/旧版本/唯一冲突409并回滚；错误不泄漏token/SQL；品牌/上传行为保留。
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-PATTERN-001, MC-SCOPE-001 — carrier用完整class Lombok；business namedBean+Slf4j+RequiredArgsConstructor/finalQualifier；converter继承Common；不可变record仅私有规则值；annotation/enum/interface无非法@Data。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11 — 语义suffix/具体组、class注解、BaseConverter、Bean命名及lombok传播、JDK/Commons、Jackson、Specification仅复杂lifecycle、java.time、exactLight；按实际类型执行而非机械加注解。
- Implementation pseudocode:

```java
On SpuSaveCommand exact ID fields add @JsonDeserialize(using=ProductIdDeserializer.class);
keep per-field @Positive or freight @Min0 and Create/Update group semantics;
@JsonAnySetter reject unknown fields with IllegalArgumentException mapped400;
no global ObjectMapper/customizer change, retain existing brand behavior;
```

- Verification contribution: ProductCompositionValidationTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 27 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/SkuSaveCommand.java`

- Purpose: decoder存在后仅绑定新商品字段，关闭严格ID RED
- Symbols: SkuSaveCommand ID binding
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: SkuSaveCommand ID binding 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第27个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: SkuSaveCommand ID binding；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: 只为已有Long字段绑定已实现codec，没有新外部参数。
- Error and edge behavior: 不存在或越租户统一404；状态/旧版本/唯一冲突409并回滚；错误不泄漏token/SQL；品牌/上传行为保留。
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-PATTERN-001, MC-SCOPE-001 — carrier用完整class Lombok；business namedBean+Slf4j+RequiredArgsConstructor/finalQualifier；converter继承Common；不可变record仅私有规则值；annotation/enum/interface无非法@Data。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11 — 语义suffix/具体组、class注解、BaseConverter、Bean命名及lombok传播、JDK/Commons、Jackson、Specification仅复杂lifecycle、java.time、exactLight；按实际类型执行而非机械加注解。
- Implementation pseudocode:

```java
On SkuSaveCommand exact ID fields add @JsonDeserialize(using=ProductIdDeserializer.class);
keep per-field @Positive or freight @Min0 and Create/Update group semantics;
@JsonAnySetter reject unknown fields with IllegalArgumentException mapped400;
no global ObjectMapper/customizer change, retain existing brand behavior;
```

- Verification contribution: ProductCompositionValidationTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

- Validation working directory: `/Users/mario/SelfProject/enjoyshop/enjoyshop-service/enjoyshop-service-goods`；GEN plan/apply的cwd另为`/Users/mario/SelfProject/enjoyshop`。
- Verification command: `./mvnw -o -B -ntp -Dtest=ProductCompositionValidationTest test`；`./mvnw -o -B -ntp -DskipTests compile`；exact checks见本StepManual Checks与§8，同步review该Step diff。
- Expected result: exit0，所有selector测试实际执行、0失败/错误；跳过/无test或classpath不解析不算PASS；标准JSON/分组/有界SKU组合可以独立校验，不泄漏持久字段。
- Failure returns to: 本Step负责该断言的File；DDL/hash/generator冲突回Step1，字段mapping回Step2/3，事务与state回Step4/5，身份/fixture回Step6，wire/OAS回Step7；GEN冲突或实际schema/API不符仍立即停止，不伪造tooling输出。
- Completion criteria: 本Step全部REQ有focused证据，Manual Check逐项有真实结果；最终SINGLE/tenant/版本/字段/旧边界与主Spec一致；无无关diff。
- Rollback: 未执行DDL前仅恢复本Step拥有文件且保留用户preexistingchanges；若用户已运行DDL则保留历史SQL/checksum，只允许后续forward-fix，不能自动DROP/repair。
- Commit paths: enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/validation/ProductValidationGroup.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/SpuSaveCommand.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/SkuSaveCommand.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/ProductSaveCommand.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/ProductIdQuery.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/ProductPageQuery.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/ProductLifecycleCommand.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/ProductActionEnum.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/ProductSkuDeletionKindEnum.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/ProductBO.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/ProductStateBO.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/ProductDomainQuery.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/CatalogReferenceBO.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/ProductResult.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/SpuResult.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/SkuResult.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/ProductSummaryResult.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/ProductStateResult.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/SpuBO.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/SkuBO.java; enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/common/validation/ProductCompositionValidationTest.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/json/ProductIdDeserializer.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/validation/ValidProductComposition.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/validation/ProductCompositionValidator.java
- Commit: Deferred — 用户要求完整logical task最多1次commit；以上路径仅本Step归属，不创建阶段commit。

### Step 3 — 实现真实字段转换和受控持久化访问

- Requirements: REQ-003, REQ-004, REQ-005, REQ-006, REQ-010, REQ-011, REQ-012, REQ-013, REQ-015
- Dependencies: Step 2 GREEN checkpoint（按用户约束无逐Step commit）
- Baseline state: 前一Step的明确End state与focused tests已验证；已存在路径只改本Step列出的符号
- Observable outcome: 具名SQL与BaseConverter字段映射可验证，无通用Wrapper/直接DAO业务调用
- End state: 业务事务服务/HTTP未接入；后续写使用已定义mapperID/guard
- Test-first gate: Required — converter编译签名先就绪后执行丢失JSON/审计mapping RED；SQL形态先缺statement RED，再扩展GEN基础表文件；Product Mapper是Spec明确业务转换
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-JSON-001, MC-TIME-001, MC-CONFIG-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001, MC-DEP-001, MC-BLOCKER-001；每项适用性按文件以下具体证据检查，不以通用规则代替实际校验。
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；普通对象/业务Bean/约束/SQL/配置分别按文件职责生效，未涉及电话/Event不新增依赖。
- Ordered files:

#### File 1 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/application/goods/ProductConverterTest.java`

- Purpose: 先定义真正字段转换和敏感信息排除
- Symbols: roundtrip_json_fields,readonly_projection
- Repository evidence: 现有BrandAdapterConverterTest/SpecAdapterConverterTest测试真实MapStruct生成Bean；本Step先建接口编译签名再RUN RED字段断言；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: roundtrip_json_fields,readonly_projection 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第1个文件；先固定observable RED，再补最小implementation；编译契约前置与实际RED执行点已由Test-first gate说明。
- Contract/signature changes: roundtrip_json_fields,readonly_projection；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: PO⇄BO真实存储边界；BO→Result仅展示投影，错误JSON不吞成空对象。
- Error and edge behavior: 不存在或越租户统一404；状态/旧版本/唯一冲突409并回滚；错误不泄漏token/SQL；品牌/上传行为保留。
- Standards impact: MC-TEST-001, MC-SCOPE-001, MC-VALID-001 — JUnit/MockMvc/现有fixture；nativegroups/真实结果断言，不建外部runtime；测试不是businessBean，沿用现有test injection风格。
- Literal rule enforcement: Rule 2, Rule 11 — 所选边界正负例/同模块隔离；不把H2或fake当PG/平台证明。
- Implementation pseudocode:

```java
@Test typed_json_codec_roundtrip() {
  arrange SpuPO JSON TEXT/images/specItems/paraItems with inherited audit and tombstone;
  convert PO->SpuBO->PO through BaseConverter and assert typed collections, UTC times, stable IDs;
  project ProductBO through ProductQueryConverter; assert tenant/user/deletionKind absent, isDelete derived, all ID fields strings;
  assert mutable collections are not accidentally shared, null/default semantics match Spec;
  independently assert ProductSpu/SkuSaveConverter and ProductSpu/SkuResultConverter read/write masks; verify aggregate Mapper uses child Mappers, with no duplicate metadata conversion;
}
```

- Verification contribution: ProductConverterTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 测试已写；明确RED执行点需要的compile prerequisites完成后运行，不以fixture/环境失败凑RED。

#### File 2 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/service/impl/ProductJsonFieldConverter.java`

- Purpose: 只编码/解码单个JSON TEXT字段，不序列化整PO/BO做对象复制
- Symbols: BaseForwardConverter<String,JsonNode> + typed named JSON field methods
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: ProductJsonFieldConverter 由§10.4对应的聚合或持久Mapper通过uses消费；只依赖已定义Command/BO/Result与Common BaseConverter合同。
- Why now: 单表field codec/输入/结果先于依赖它们的聚合Mapper；focused test已定义每个单表边界，随后进行必要组合。
- Contract/signature changes: BaseForwardConverter<String,JsonNode> + typed named JSON field methods；不新增HTTP、单表Command或持久状态。
- Input/output and state mapping: 只编码/解码单个JSON TEXT字段，不序列化整PO/BO做对象复制；字段/默认/null/元数据保护以Spec§10.4为准。
- Error and edge behavior: Jackson字段解码失败抛明确safeInternal error，不吞错误变{}；没有wholePojoJSONroundtrip。
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-PATTERN-001, MC-SCOPE-001 — carrier用完整class Lombok；business namedBean+Slf4j+RequiredArgsConstructor/finalQualifier；converter继承Common；不可变record仅私有规则值；annotation/enum/interface无非法@Data。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11 — 语义suffix/具体组、class注解、BaseConverter、Bean命名及lombok传播、JDK/Commons、Jackson、Specification仅复杂lifecycle、java.time、exactLight；按实际类型执行而非机械加注解。
- Implementation pseudocode:

```java
@Component("productJsonFieldConverter") @Slf4j @RequiredArgsConstructor
class ProductJsonFieldConverter implements BaseForwardConverter<String,JsonNode>;
final @Qualifier("jacksonObjectMapper") ObjectMapper mapper; toTarget parses one field text, malformed stored JSON -> safe Common PRODUCT_INTERNAL_ERROR;
@Named methods decodeImages(List<String>), decodeSpecItems(Map<String,List<String>>), decodeParaItems/decodeSpec(Map<String,String>), encode lists/maps with explicit TypeReference;
no Gson/Fastjson/manual whole-object copy; callers are ProductSpu/ProductSkuPersistenceConverter qualifiedByName mappings;
```

- Verification contribution: ProductConverterTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 3 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/ProductSpuSaveConverter.java`

- Purpose: Spu单表保存输入→领域业务字段，Create/Update复用同输入组但不允许客户端覆盖权威metadata
- Symbols: BaseForwardConverter<SpuSaveCommand,SpuBO>
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: ProductSpuSaveConverter 由§10.4对应的聚合或持久Mapper通过uses消费；只依赖已定义Command/BO/Result与Common BaseConverter合同。
- Why now: 单表field codec/输入/结果先于依赖它们的聚合Mapper；focused test已定义每个单表边界，随后进行必要组合。
- Contract/signature changes: BaseForwardConverter<SpuSaveCommand,SpuBO>；不新增HTTP、单表Command或持久状态。
- Input/output and state mapping: Spu单表保存输入→领域业务字段，Create/Update复用同输入组但不允许客户端覆盖权威metadata；字段/默认/null/元数据保护以Spec§10.4为准。
- Error and edge behavior: Jackson字段解码失败抛明确safeInternal error，不吞错误变{}；没有wholePojoJSONroundtrip。
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-PATTERN-001, MC-SCOPE-001 — carrier用完整class Lombok；business namedBean+Slf4j+RequiredArgsConstructor/finalQualifier；converter继承Common；不可变record仅私有规则值；annotation/enum/interface无非法@Data。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11 — 语义suffix/具体组、class注解、BaseConverter、Bean命名及lombok传播、JDK/Commons、Jackson、Specification仅复杂lifecycle、java.time、exactLight；按实际类型执行而非机械加注解。
- Implementation pseudocode:

```java
@Mapper(componentModel="spring",unmappedTargetPolicy=ERROR)
interface ProductSpuSaveConverter extends BaseForwardConverter<SpuSaveCommand,SpuBO>;
map exact user-writable fields from Spec§10; ignore tenant/audit/counters and backend-derived state/names/references;
Spu id/version supplied by aggregate trusted path/version mapping; Sku new/existing id obeys Create/Update input validation and Domain ownership check;
JSON remains typed collections here; no persistence encoding, DAO calls or another Create/UpdateSpuCommand; aggregate ProductApplicationConverter uses this Mapper;
```

- Verification contribution: ProductConverterTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 4 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/ProductSpuResultConverter.java`

- Purpose: Spu单表领域结果→实际只读展示投影，排除tenant/actor和内部删除原因
- Symbols: BaseForwardConverter<SpuBO,SpuResult>
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: ProductSpuResultConverter 由§10.4对应的聚合或持久Mapper通过uses消费；只依赖已定义Command/BO/Result与Common BaseConverter合同。
- Why now: 单表field codec/输入/结果先于依赖它们的聚合Mapper；focused test已定义每个单表边界，随后进行必要组合。
- Contract/signature changes: BaseForwardConverter<SpuBO,SpuResult>；不新增HTTP、单表Command或持久状态。
- Input/output and state mapping: Spu单表领域结果→实际只读展示投影，排除tenant/actor和内部删除原因；字段/默认/null/元数据保护以Spec§10.4为准。
- Error and edge behavior: Jackson字段解码失败抛明确safeInternal error，不吞错误变{}；没有wholePojoJSONroundtrip。
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-PATTERN-001, MC-SCOPE-001 — carrier用完整class Lombok；business namedBean+Slf4j+RequiredArgsConstructor/finalQualifier；converter继承Common；不可变record仅私有规则值；annotation/enum/interface无非法@Data。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11 — 语义suffix/具体组、class注解、BaseConverter、Bean命名及lombok传播、JDK/Commons、Jackson、Specification仅复杂lifecycle、java.time、exactLight；按实际类型执行而非机械加注解。
- Implementation pseudocode:

```java
@Mapper(componentModel="spring",unmappedTargetPolicy=ERROR)
interface ProductSpuResultConverter extends BaseForwardConverter<SpuBO,SpuResult>;
map exact Spec§9 output fields; Spu isDelete derives deletedAt, toSummary(SpuBO) reuses same field semantics; Sku hides deletionKind and internal row version;
Result Jackson owns string IDs and ISO java.time values; no PO/Security token leaks or duplicate envelope;
aggregate ProductQueryConverter uses this Mapper; test each single-table mapping independently before aggregate composition;
```

- Verification contribution: ProductConverterTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 5 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/ProductSkuSaveConverter.java`

- Purpose: Sku单表保存输入→领域业务字段，Create/Update复用同输入组但不允许客户端覆盖权威metadata
- Symbols: BaseForwardConverter<SkuSaveCommand,SkuBO>
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: ProductSkuSaveConverter 由§10.4对应的聚合或持久Mapper通过uses消费；只依赖已定义Command/BO/Result与Common BaseConverter合同。
- Why now: 单表field codec/输入/结果先于依赖它们的聚合Mapper；focused test已定义每个单表边界，随后进行必要组合。
- Contract/signature changes: BaseForwardConverter<SkuSaveCommand,SkuBO>；不新增HTTP、单表Command或持久状态。
- Input/output and state mapping: Sku单表保存输入→领域业务字段，Create/Update复用同输入组但不允许客户端覆盖权威metadata；字段/默认/null/元数据保护以Spec§10.4为准。
- Error and edge behavior: Jackson字段解码失败抛明确safeInternal error，不吞错误变{}；没有wholePojoJSONroundtrip。
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-PATTERN-001, MC-SCOPE-001 — carrier用完整class Lombok；business namedBean+Slf4j+RequiredArgsConstructor/finalQualifier；converter继承Common；不可变record仅私有规则值；annotation/enum/interface无非法@Data。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11 — 语义suffix/具体组、class注解、BaseConverter、Bean命名及lombok传播、JDK/Commons、Jackson、Specification仅复杂lifecycle、java.time、exactLight；按实际类型执行而非机械加注解。
- Implementation pseudocode:

```java
@Mapper(componentModel="spring",unmappedTargetPolicy=ERROR)
interface ProductSkuSaveConverter extends BaseForwardConverter<SkuSaveCommand,SkuBO>;
map exact user-writable fields from Spec§10; ignore tenant/audit/counters and backend-derived state/names/references;
Spu id/version supplied by aggregate trusted path/version mapping; Sku new/existing id obeys Create/Update input validation and Domain ownership check;
JSON remains typed collections here; no persistence encoding, DAO calls or another Create/UpdateSkuCommand; aggregate ProductApplicationConverter uses this Mapper;
```

- Verification contribution: ProductConverterTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 6 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/ProductSkuResultConverter.java`

- Purpose: Sku单表领域结果→实际只读展示投影，排除tenant/actor和内部删除原因
- Symbols: BaseForwardConverter<SkuBO,SkuResult>
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: ProductSkuResultConverter 由§10.4对应的聚合或持久Mapper通过uses消费；只依赖已定义Command/BO/Result与Common BaseConverter合同。
- Why now: 单表field codec/输入/结果先于依赖它们的聚合Mapper；focused test已定义每个单表边界，随后进行必要组合。
- Contract/signature changes: BaseForwardConverter<SkuBO,SkuResult>；不新增HTTP、单表Command或持久状态。
- Input/output and state mapping: Sku单表领域结果→实际只读展示投影，排除tenant/actor和内部删除原因；字段/默认/null/元数据保护以Spec§10.4为准。
- Error and edge behavior: Jackson字段解码失败抛明确safeInternal error，不吞错误变{}；没有wholePojoJSONroundtrip。
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-PATTERN-001, MC-SCOPE-001 — carrier用完整class Lombok；business namedBean+Slf4j+RequiredArgsConstructor/finalQualifier；converter继承Common；不可变record仅私有规则值；annotation/enum/interface无非法@Data。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11 — 语义suffix/具体组、class注解、BaseConverter、Bean命名及lombok传播、JDK/Commons、Jackson、Specification仅复杂lifecycle、java.time、exactLight；按实际类型执行而非机械加注解。
- Implementation pseudocode:

```java
@Mapper(componentModel="spring",unmappedTargetPolicy=ERROR)
interface ProductSkuResultConverter extends BaseForwardConverter<SkuBO,SkuResult>;
map exact Spec§9 output fields; Spu isDelete derives deletedAt, toSummary(SpuBO) reuses same field semantics; Sku hides deletionKind and internal row version;
Result Jackson owns string IDs and ISO java.time values; no PO/Security token leaks or duplicate envelope;
aggregate ProductQueryConverter uses this Mapper; test each single-table mapping independently before aggregate composition;
```

- Verification contribution: ProductConverterTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 7 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/service/impl/ProductSpuPersistenceConverter.java`

- Purpose: 商品专用持久化mapping处理真实JSON/审计保护语义
- Symbols: BaseConverter<SpuPO,SpuBO>
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: BaseConverter<SpuPO,SpuBO> 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第2个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: BaseConverter<SpuPO,SpuBO>；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: exact generics双向；商品typedJSON/metadata与P-E的CUSTOM业务mapping按已修复Spec§10.4，不复写通用catalog或改生成器。
- Error and edge behavior: Jackson字段解码失败抛明确safeInternal error，不吞错误变{}；没有wholePojoJSONroundtrip。
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-PATTERN-001, MC-SCOPE-001 — carrier用完整class Lombok；business namedBean+Slf4j+RequiredArgsConstructor/finalQualifier；converter继承Common；不可变record仅私有规则值；annotation/enum/interface无非法@Data。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11 — 语义suffix/具体组、class注解、BaseConverter、Bean命名及lombok传播、JDK/Commons、Jackson、Specification仅复杂lifecycle、java.time、exactLight；按实际类型执行而非机械加注解。
- Implementation pseudocode:

```java
@Mapper(componentModel="spring",uses=ProductJsonFieldConverter.class,unmappedTargetPolicy=ERROR,builder=@org.mapstruct.Builder(disableBuilder=true))
interface ProductSpuPersistenceConverter extends BaseConverter<SpuPO,SpuBO>;
Map inherited id/tenant/audit/deletedAt/version field-by-field; JSON TEXT fields use named Jackson encode/decode methods through qualified methods in ProductJsonFieldConverter, not whole-object conversion;
No duplicate generic Create/Update/DeleteSpuCommand; ProductSpuSaveConverter and ProductSpuResultConverter provide necessary actual single-table input/output mappings; mutable collections copy at boundary;
Map enum deletionKind code P/E explicitly for Sku; ordinary defaults established Domain, converter never accepts HTTP-supplied audit;
```

- Verification contribution: ProductConverterTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 8 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/service/impl/ProductSkuPersistenceConverter.java`

- Purpose: 商品专用持久化mapping处理真实JSON/审计保护语义
- Symbols: BaseConverter<SkuPO,SkuBO>
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: BaseConverter<SkuPO,SkuBO> 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第3个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: BaseConverter<SkuPO,SkuBO>；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: exact generics双向；商品typedJSON/metadata与P-E的CUSTOM业务mapping按已修复Spec§10.4，不复写通用catalog或改生成器。
- Error and edge behavior: Jackson字段解码失败抛明确safeInternal error，不吞错误变{}；没有wholePojoJSONroundtrip。
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-PATTERN-001, MC-SCOPE-001 — carrier用完整class Lombok；business namedBean+Slf4j+RequiredArgsConstructor/finalQualifier；converter继承Common；不可变record仅私有规则值；annotation/enum/interface无非法@Data。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11 — 语义suffix/具体组、class注解、BaseConverter、Bean命名及lombok传播、JDK/Commons、Jackson、Specification仅复杂lifecycle、java.time、exactLight；按实际类型执行而非机械加注解。
- Implementation pseudocode:

```java
@Mapper(componentModel="spring",uses=ProductJsonFieldConverter.class,unmappedTargetPolicy=ERROR,builder=@org.mapstruct.Builder(disableBuilder=true))
interface ProductSkuPersistenceConverter extends BaseConverter<SkuPO,SkuBO>;
Map inherited id/tenant/audit/deletedAt/version field-by-field; JSON TEXT fields use named Jackson encode/decode methods through qualified methods in ProductJsonFieldConverter, not whole-object conversion;
No duplicate generic Create/Update/DeleteSkuCommand; ProductSkuSaveConverter and ProductSkuResultConverter provide necessary actual single-table input/output mappings; mutable collections copy at boundary;
Map enum deletionKind code P/E explicitly for Sku; ordinary defaults established Domain, converter never accepts HTTP-supplied audit;
```

- Verification contribution: ProductConverterTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 9 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/ProductApplicationConverter.java`

- Purpose: 复用Common投影合同避免传输/领域/持久对象泄漏
- Symbols: BaseForwardConverter<ProductSaveCommand,ProductBO>
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: BaseForwardConverter<ProductSaveCommand,ProductBO> 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第4个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: BaseForwardConverter<ProductSaveCommand,ProductBO>；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: Delegate child conversion to ProductSpuSaveConverter/ProductSkuSaveConverter via uses; only aggregate spu/skuList/version are mapped here; from update path fills existing id with explicit MapStruct extra parameter; version→expectedVersion; ignore tenant/audit/counters/status and derived SKU names; map ProductId/PageQuery→DomainQuery。
- Error and edge behavior: 不存在或越租户统一404；状态/旧版本/唯一冲突409并回滚；错误不泄漏token/SQL；品牌/上传行为保留。
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-PATTERN-001, MC-SCOPE-001 — carrier用完整class Lombok；business namedBean+Slf4j+RequiredArgsConstructor/finalQualifier；converter继承Common；不可变record仅私有规则值；annotation/enum/interface无非法@Data。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11 — 语义suffix/具体组、class注解、BaseConverter、Bean命名及lombok传播、JDK/Commons、Jackson、Specification仅复杂lifecycle、java.time、exactLight；按实际类型执行而非机械加注解。
- Implementation pseudocode:

```java
@Mapper(componentModel="spring",uses={ProductSpuSaveConverter.class,ProductSkuSaveConverter.class},unmappedTargetPolicy=ERROR)
interface ProductApplicationConverter extends BaseForwardConverter<ProductSaveCommand,ProductBO>;
Delegate child conversion to ProductSpuSaveConverter/ProductSkuSaveConverter via uses; only aggregate spu/skuList/version are mapped here; from update path fills existing id with explicit MapStruct extra parameter; version→expectedVersion; ignore tenant/audit/counters/status and derived SKU names; map ProductId/PageQuery→DomainQuery;
use qualified MapStruct mappings/defaults, generated bean productApplicationConverterImpl; no manual bean-copy/get-set chains;
Jackson field codec only appears at persistence JSON fields; this mapper never creates new ID or dispatches actions;
```

- Verification contribution: ProductConverterTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 10 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/ProductQueryConverter.java`

- Purpose: 复用Common投影合同避免传输/领域/持久对象泄漏
- Symbols: BaseForwardConverter<ProductBO,ProductResult>
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: BaseForwardConverter<ProductBO,ProductResult> 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第5个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: BaseForwardConverter<ProductBO,ProductResult>；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: Delegate SpuBO→SpuResult and SkuBO→SkuResult to ProductSpuResultConverter/ProductSkuResultConverter via uses; do not repeat child mappings; ProductStateBO→ProductStateResult; SpuBO→ProductSummaryResult; preserve declared nullableTime; excludes technical fields。
- Error and edge behavior: 不存在或越租户统一404；状态/旧版本/唯一冲突409并回滚；错误不泄漏token/SQL；品牌/上传行为保留。
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-PATTERN-001, MC-SCOPE-001 — carrier用完整class Lombok；business namedBean+Slf4j+RequiredArgsConstructor/finalQualifier；converter继承Common；不可变record仅私有规则值；annotation/enum/interface无非法@Data。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11 — 语义suffix/具体组、class注解、BaseConverter、Bean命名及lombok传播、JDK/Commons、Jackson、Specification仅复杂lifecycle、java.time、exactLight；按实际类型执行而非机械加注解。
- Implementation pseudocode:

```java
@Mapper(componentModel="spring",uses={ProductSpuResultConverter.class,ProductSkuResultConverter.class},unmappedTargetPolicy=ERROR)
interface ProductQueryConverter extends BaseForwardConverter<ProductBO,ProductResult>;
Delegate SpuBO→SpuResult and SkuBO→SkuResult to ProductSpuResultConverter/ProductSkuResultConverter via uses; do not repeat child mappings; ProductStateBO→ProductStateResult; SpuBO→ProductSummaryResult; preserve declared nullableTime; excludes technical fields;
use qualified MapStruct mappings/defaults, generated bean productQueryConverterImpl; no manual bean-copy/get-set chains;
Jackson field codec only appears at persistence JSON fields; this mapper never creates new ID or dispatches actions;
```

- Verification contribution: ProductConverterTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 11 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/goods/ProductQuerySqlShapeTest.java`

- Purpose: 先用资源SQL形态断言缺少具名查询/生命周期语句
- Symbols: tenantLifecyclePredicates,guardedRowEffects
- Repository evidence: 现有BrandQuerySqlShapeTest/SpecQuerySqlShapeTest以XML文本/结构验证SQL，没有DB依赖；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: tenantLifecyclePredicates,guardedRowEffects 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第6个文件；先固定observable RED，再补最小implementation；编译契约前置与实际RED执行点已由Test-first gate说明。
- Contract/signature changes: tenantLifecyclePredicates,guardedRowEffects；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: 严格按商品Spec的字段/默认/null/状态/版本定义映射；业务ID十进制字符串，持久BIGINT；审计Instant/UTC deletedAt微秒。
- Error and edge behavior: 不存在或越租户统一404；状态/旧版本/唯一冲突409并回滚；错误不泄漏token/SQL；品牌/上传行为保留。
- Standards impact: MC-TEST-001, MC-SCOPE-001, MC-VALID-001 — JUnit/MockMvc/现有fixture；nativegroups/真实结果断言，不建外部runtime；测试不是businessBean，沿用现有test injection风格。
- Literal rule enforcement: Rule 2, Rule 11 — 所选边界正负例/同模块隔离；不把H2或fake当PG/平台证明。
- Implementation pseudocode:

```java
@Test named_sql_has_required_guards() {
  read SpuDAO/SkuDAO/CategoryBrandDAO XML; assert named IDs, explicit tenant and expectedVersion binding;
  assert active and recycled=P query differ, historicalE is never restored; purge has NO active-only predicate;
  assert list/count same filters, ORDER BY id DESC, LIKE wildcard escaping, bounded LIMIT/OFFSET;
  assert deletion computes monotone UTC micros per tenant/sn, no unsafe physical cascade/globalWrapper;
}
```

- Verification contribution: ProductQuerySqlShapeTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 测试已写；明确RED执行点需要的compile prerequisites完成后运行，不以fixture/环境失败凑RED。

#### File 12 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/dao/SpuDAO.java`

- Purpose: 在生成DAO增加真实具名读写签名与shape约束
- Symbols: selectByLifecycle,selectPage,countPage,lockByLifecycle,loadCatalogReferences,deleteVersionedById,restoreVersionedById,purgeDeletedById
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: selectByLifecycle,selectPage,countPage,lockByLifecycle,loadCatalogReferences,deleteVersionedById,restoreVersionedById,purgeDeletedById 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第7个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: selectByLifecycle,selectPage,countPage,lockByLifecycle,loadCatalogReferences,deleteVersionedById,restoreVersionedById,purgeDeletedById；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: typedPO/DomainQuery→Mapper绑定，所有tenant来自可信context而非callerHeaders。
- Error and edge behavior: 不存在或越租户统一404；状态/旧版本/唯一冲突409并回滚；错误不泄漏token/SQL；品牌/上传行为保留。
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-PATTERN-001, MC-SCOPE-001 — carrier用完整class Lombok；business namedBean+Slf4j+RequiredArgsConstructor/finalQualifier；converter继承Common；不可变record仅私有规则值；annotation/enum/interface无非法@Data。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11 — 语义suffix/具体组、class注解、BaseConverter、Bean命名及lombok传播、JDK/Commons、Jackson、Specification仅复杂lifecycle、java.time、exactLight；按实际类型执行而非机械加注解。
- Implementation pseudocode:

```java
retain extends EgonColaMapper<SpuPO>;
add named query methods with @Param, @Validated(Query.class), @Valid typed DomainQuery/PO and scalar @Positive/@Min;
for custom physical delete use minimal id/version command shape, no tombstone passed to group requiring @Null deletedAt;
mapper loaded rows use Persisted, not business state validation; SQL declarations below implement exact selectByLifecycle,selectPage,countPage,lockByLifecycle,loadCatalogReferences,deleteVersionedById,restoreVersionedById,purgeDeletedById;
```

- Verification contribution: ProductQuerySqlShapeTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 13 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/resources/mybatis/mapper/goods/SpuDAO.xml`

- Purpose: 扩展显式XML查询与受控生命周期更新
- Symbols: selectByLifecycle,selectPage,countPage,lockByLifecycle,loadCatalogReferences,deleteVersionedById,restoreVersionedById,purgeDeletedById
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: selectByLifecycle,selectPage,countPage,lockByLifecycle,loadCatalogReferences,deleteVersionedById,restoreVersionedById,purgeDeletedById 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第8个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: selectByLifecycle,selectPage,countPage,lockByLifecycle,loadCatalogReferences,deleteVersionedById,restoreVersionedById,purgeDeletedById；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: 每个写tenant/id/version/状态谓词，affected rows核验；不全局disableLogicDelete/guard。
- Error and edge behavior: unique异常立即abortPG事务，不catch后续跑；所有恢复仅指定mapper statement白名单。
- Standards impact: MC-ARCH-001, MC-CONFIG-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001 — 精确manifest/typedkeys/UTC/SQL范围、同profile结构；无Java对象注解装饰配置。
- Literal rule enforcement: Rule 6, Rule 7, Rule 10, Rule 11 — Jackson或SQL实际类型、四profile同键、UTC时间、原Light和MP DDL约束，文档N/A项不引代码。
- Implementation pseudocode:

```xml
Query WHERE tenant_id=:trusted AND id=:id AND deleted scope; includeDeleted read is explicit;
list/count WHERE tenant+deleted+(optional bound sn/name/status/marketable); ORDER BY id DESC, LIMIT/OFFSET;
loadCatalogReferences joins brand/category1/2/3/template with same-tenant active and correct parent links; select specs/para once order seq,id;
FOR UPDATE category IDs ascending then brand/template; parent lockByLifecycle after directory locks for SAVE;
override deleteVersionedById atomic SET status='0',marketable=false,deleted_at=GREATEST(UTC now,max historical tenant/sn +1micro),version+1,audit WHERE tenant/id/version AND active AND down;
restoreVersionedById WHERE tombstone+expectedVersion; existing Egon restoreById handles transaction/allowlist;
purgeDeletedById uses minimal Delete-group PO(id/version,deletedAt=null) only as input, SQL guards real deleted IS NOT NULL; expect1 or safe absent;
Keep generated resultMap inherited fields; annotate SQL by same project comment style; custom work after initial generate only.
```

- Verification contribution: ProductQuerySqlShapeTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 14 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/repo/SpuRepository.java`

- Purpose: 将具名DAO封装在现有Repo层，Domain永不直接DAO
- Symbols: selectByLifecycle,selectPage,countPage,lockByLifecycle,loadCatalogReferences,deleteVersionedById,restoreVersionedById,purgeDeletedById
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: selectByLifecycle,selectPage,countPage,lockByLifecycle,loadCatalogReferences,deleteVersionedById,restoreVersionedById,purgeDeletedById 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第9个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: selectByLifecycle,selectPage,countPage,lockByLifecycle,loadCatalogReferences,deleteVersionedById,restoreVersionedById,purgeDeletedById；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: 仅基础设施getBaseMapper；父service只组合Repository，读写均单组事务。
- Error and edge behavior: 不存在或越租户统一404；状态/旧版本/唯一冲突409并回滚；错误不泄漏token/SQL；品牌/上传行为保留。
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-PATTERN-001, MC-SCOPE-001 — carrier用完整class Lombok；business namedBean+Slf4j+RequiredArgsConstructor/finalQualifier；converter继承Common；不可变record仅私有规则值；annotation/enum/interface无非法@Data。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11 — 语义suffix/具体组、class注解、BaseConverter、Bean命名及lombok传播、JDK/Commons、Jackson、Specification仅复杂lifecycle、java.time、exactLight；按实际类型执行而非机械加注解。
- Implementation pseudocode:

```java
preserve EgonColaRepository and named bean spuRepository; @Validated/@Slf4j/@RequiredArgsConstructor and final qualified DAO/properties;
expose only named business persistence methods from selectByLifecycle,selectPage,countPage,lockByLifecycle,loadCatalogReferences,deleteVersionedById,restoreVersionedById,purgeDeletedById; incoming PO shapes choose Insert/Update/Delete/Restore groups;
ordinary save/update/remove/restore inherited final methods not copied; custom DAO methods participate existing MyBatis interceptors/local-write-guard;
return typed loaded rows; for every effect==0 classify absent/version/state under existing trusted scope, not global retries;
```

- Verification contribution: ProductQuerySqlShapeTest,ProductAggregatePersistenceTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 15 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/dao/SkuDAO.java`

- Purpose: 在生成DAO增加真实具名读写签名与shape约束
- Symbols: selectBySpuLifecycle,selectAllBySpuForPurge,deleteVersionedById,restoreVersionedById,purgeAllBySpu
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: selectBySpuLifecycle,selectAllBySpuForPurge,deleteVersionedById,restoreVersionedById,purgeAllBySpu 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第10个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: selectBySpuLifecycle,selectAllBySpuForPurge,deleteVersionedById,restoreVersionedById,purgeAllBySpu；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: typedPO/DomainQuery→Mapper绑定，所有tenant来自可信context而非callerHeaders。
- Error and edge behavior: 不存在或越租户统一404；状态/旧版本/唯一冲突409并回滚；错误不泄漏token/SQL；品牌/上传行为保留。
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-PATTERN-001, MC-SCOPE-001 — carrier用完整class Lombok；business namedBean+Slf4j+RequiredArgsConstructor/finalQualifier；converter继承Common；不可变record仅私有规则值；annotation/enum/interface无非法@Data。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11 — 语义suffix/具体组、class注解、BaseConverter、Bean命名及lombok传播、JDK/Commons、Jackson、Specification仅复杂lifecycle、java.time、exactLight；按实际类型执行而非机械加注解。
- Implementation pseudocode:

```java
retain extends EgonColaMapper<SkuPO>;
add named query methods with @Param, @Validated(Query.class), @Valid typed DomainQuery/PO and scalar @Positive/@Min;
for custom physical delete use minimal id/version command shape, no tombstone passed to group requiring @Null deletedAt;
mapper loaded rows use Persisted, not business state validation; SQL declarations below implement exact selectBySpuLifecycle,selectAllBySpuForPurge,deleteVersionedById,restoreVersionedById,purgeAllBySpu;
```

- Verification contribution: ProductQuerySqlShapeTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 16 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/resources/mybatis/mapper/goods/SkuDAO.xml`

- Purpose: 扩展显式XML查询与受控生命周期更新
- Symbols: selectBySpuLifecycle,selectAllBySpuForPurge,deleteVersionedById,restoreVersionedById,purgeAllBySpu
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: selectBySpuLifecycle,selectAllBySpuForPurge,deleteVersionedById,restoreVersionedById,purgeAllBySpu 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第11个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: selectBySpuLifecycle,selectAllBySpuForPurge,deleteVersionedById,restoreVersionedById,purgeAllBySpu；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: 每个写tenant/id/version/状态谓词，affected rows核验；不全局disableLogicDelete/guard。
- Error and edge behavior: unique异常立即abortPG事务，不catch后续跑；所有恢复仅指定mapper statement白名单。
- Standards impact: MC-ARCH-001, MC-CONFIG-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001 — 精确manifest/typedkeys/UTC/SQL范围、同profile结构；无Java对象注解装饰配置。
- Literal rule enforcement: Rule 6, Rule 7, Rule 10, Rule 11 — Jackson或SQL实际类型、四profile同键、UTC时间、原Light和MP DDL约束，文档N/A项不引代码。
- Implementation pseudocode:

```xml
selectBySpuLifecycle tenant/spu and either active rows or deleted kind='P', ORDER BY id ASC; exclude E;
selectAllBySpuForPurge explicitly no deleted filter; no singleSKU HTTP or cross-parent write;
override deleteVersionedById: SET deletion_kind=#{et.deletionKind},deleted_at=monotoneUTC per tenant/sn,version+1,audit WHERE tenant/id/spu/version AND active;
restoreVersionedById atomic SET deleted_at=NULL,deletion_kind=NULL,version+1,audit WHERE tombstone+kind='P'+tenant/id/version;
purgeAllBySpu physical DELETE tenant+spu only, within locked deleted-parent transaction; assert affected rows equal known history count;
ordinary insert/update inherited guarded methods, every child failure rolls wholeparent transaction back;
Keep generated resultMap inherited fields; annotate SQL by same project comment style; custom work after initial generate only.
```

- Verification contribution: ProductQuerySqlShapeTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 17 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/repo/SkuRepository.java`

- Purpose: 将具名DAO封装在现有Repo层，Domain永不直接DAO
- Symbols: selectBySpuLifecycle,selectAllBySpuForPurge,deleteVersionedById,restoreVersionedById,purgeAllBySpu
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: selectBySpuLifecycle,selectAllBySpuForPurge,deleteVersionedById,restoreVersionedById,purgeAllBySpu 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第12个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: selectBySpuLifecycle,selectAllBySpuForPurge,deleteVersionedById,restoreVersionedById,purgeAllBySpu；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: 仅基础设施getBaseMapper；父service只组合Repository，读写均单组事务。
- Error and edge behavior: 不存在或越租户统一404；状态/旧版本/唯一冲突409并回滚；错误不泄漏token/SQL；品牌/上传行为保留。
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-PATTERN-001, MC-SCOPE-001 — carrier用完整class Lombok；business namedBean+Slf4j+RequiredArgsConstructor/finalQualifier；converter继承Common；不可变record仅私有规则值；annotation/enum/interface无非法@Data。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11 — 语义suffix/具体组、class注解、BaseConverter、Bean命名及lombok传播、JDK/Commons、Jackson、Specification仅复杂lifecycle、java.time、exactLight；按实际类型执行而非机械加注解。
- Implementation pseudocode:

```java
preserve EgonColaRepository and named bean skuRepository; @Validated/@Slf4j/@RequiredArgsConstructor and final qualified DAO/properties;
expose only named business persistence methods from selectBySpuLifecycle,selectAllBySpuForPurge,deleteVersionedById,restoreVersionedById,purgeAllBySpu; incoming PO shapes choose Insert/Update/Delete/Restore groups;
ordinary save/update/remove/restore inherited final methods not copied; custom DAO methods participate existing MyBatis interceptors/local-write-guard;
return typed loaded rows; for every effect==0 classify absent/version/state under existing trusted scope, not global retries;
```

- Verification contribution: ProductQuerySqlShapeTest,ProductAggregatePersistenceTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 18 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/dao/CategoryBrandDAO.java`

- Purpose: 在生成DAO增加真实具名读写签名与shape约束
- Symbols: selectActiveByEndpoints,ensureActive via repository
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: selectActiveByEndpoints,ensureActive via repository 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第13个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: selectActiveByEndpoints,ensureActive via repository；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: typedPO/DomainQuery→Mapper绑定，所有tenant来自可信context而非callerHeaders。
- Error and edge behavior: 不存在或越租户统一404；状态/旧版本/唯一冲突409并回滚；错误不泄漏token/SQL；品牌/上传行为保留。
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-PATTERN-001, MC-SCOPE-001 — carrier用完整class Lombok；business namedBean+Slf4j+RequiredArgsConstructor/finalQualifier；converter继承Common；不可变record仅私有规则值；annotation/enum/interface无非法@Data。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11 — 语义suffix/具体组、class注解、BaseConverter、Bean命名及lombok传播、JDK/Commons、Jackson、Specification仅复杂lifecycle、java.time、exactLight；按实际类型执行而非机械加注解。
- Implementation pseudocode:

```java
retain extends EgonColaMapper<CategoryBrandPO>;
add named query methods with @Param, @Validated(Query.class), @Valid typed DomainQuery/PO and scalar @Positive/@Min;
for custom physical delete use minimal id/version command shape, no tombstone passed to group requiring @Null deletedAt;
mapper loaded rows use Persisted, not business state validation; SQL declarations below implement exact selectActiveByEndpoints,ensureActive via repository;
```

- Verification contribution: ProductQuerySqlShapeTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 19 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/resources/mybatis/mapper/goods/CategoryBrandDAO.xml`

- Purpose: 扩展显式XML查询与受控生命周期更新
- Symbols: selectActiveByEndpoints,ensureActive via repository
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: selectActiveByEndpoints,ensureActive via repository 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第14个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: selectActiveByEndpoints,ensureActive via repository；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: 每个写tenant/id/version/状态谓词，affected rows核验；不全局disableLogicDelete/guard。
- Error and edge behavior: unique异常立即abortPG事务，不catch后续跑；所有恢复仅指定mapper statement白名单。
- Standards impact: MC-ARCH-001, MC-CONFIG-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001 — 精确manifest/typedkeys/UTC/SQL范围、同profile结构；无Java对象注解装饰配置。
- Literal rule enforcement: Rule 6, Rule 7, Rule 10, Rule 11 — Jackson或SQL实际类型、四profile同键、UTC时间、原Light和MP DDL约束，文档N/A项不引代码。
- Implementation pseudocode:

```xml
SELECT id, tenant_id, category_id, brand_id, deleted_at, version FROM category_brand
WHERE tenant_id = #{tenantId} AND category_id = #{categoryId} AND brand_id = #{brandId} AND deleted_at IS NULL;
Use existing uk_category_brand_active, return zero or one typed CategoryBrandPO; no UPDATE/DROP/DELETE relationship;
An absent relation is saved through guarded Repository in the parent aggregate transaction, never this read query;
Keep generated resultMap inherited fields; annotate SQL by same project comment style; custom work after initial generate only.
```

- Verification contribution: ProductQuerySqlShapeTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 20 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/repo/CategoryBrandRepository.java`

- Purpose: 将具名DAO封装在现有Repo层，Domain永不直接DAO
- Symbols: selectActiveByEndpoints,ensureActive via repository
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: selectActiveByEndpoints,ensureActive via repository 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第15个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: selectActiveByEndpoints,ensureActive via repository；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: 仅基础设施getBaseMapper；父service只组合Repository，读写均单组事务。
- Error and edge behavior: 不存在或越租户统一404；状态/旧版本/唯一冲突409并回滚；错误不泄漏token/SQL；品牌/上传行为保留。
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-PATTERN-001, MC-SCOPE-001 — carrier用完整class Lombok；business namedBean+Slf4j+RequiredArgsConstructor/finalQualifier；converter继承Common；不可变record仅私有规则值；annotation/enum/interface无非法@Data。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11 — 语义suffix/具体组、class注解、BaseConverter、Bean命名及lombok传播、JDK/Commons、Jackson、Specification仅复杂lifecycle、java.time、exactLight；按实际类型执行而非机械加注解。
- Implementation pseudocode:

```java
preserve EgonColaRepository and named bean categoryBrandRepository; @Validated/@Slf4j/@RequiredArgsConstructor and final qualified DAO/properties;
expose only named business persistence methods from selectActiveByEndpoints,ensureActive via repository; incoming PO shapes choose Insert/Update/Delete/Restore groups;
ordinary save/update/remove/restore inherited final methods not copied; custom DAO methods participate existing MyBatis interceptors/local-write-guard;
return typed loaded rows; for every effect==0 classify absent/version/state under existing trusted scope, not global retries;
```

- Verification contribution: ProductQuerySqlShapeTest,ProductAggregatePersistenceTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 21 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/resources/application.yml`

- Purpose: 先于Step4/5真实mapper测试配置同组与受限生命周期
- Symbols: MP product tables/lifecycle/custom-root allowlists
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件存在，应局限所列symbol。
- Dependencies and consumers: MP product tables/lifecycle/custom-root allowlists 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第16个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: MP product tables/lifecycle/custom-root allowlists；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: 此Step先更新MP键；Step6仅新增授权键，重复文件明确按不同section负责。
- Error and edge behavior: 不存在或越租户统一404；状态/旧版本/唯一冲突409并回滚；错误不泄漏token/SQL；品牌/上传行为保留。
- Standards impact: MC-ARCH-001, MC-CONFIG-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001 — 精确manifest/typedkeys/UTC/SQL范围、同profile结构；无Java对象注解装饰配置。
- Literal rule enforcement: Rule 6, Rule 7, Rule 10, Rule 11 — Jackson或SQL实际类型、四profile同键、UTC时间、原Light和MP DDL约束，文档N/A项不引代码。
- Implementation pseudocode:

```yaml
Register spu/sku SINGLE/master_data under MP sharding.tables in application.yml; preserve alloldtable keys;
set mybatis-plus.lifecycle.restore-statements exact SpuDAO.restoreVersionedById and SkuDAO.restoreVersionedById;
local-write-guard.allowed-root-statements mapping only SpuDAO.purgeDeletedById=>spu and SkuDAO.purgeAllBySpu=>sku plus required named state updates;
all fully qualified namespace IDs start top.egon.enjoyshop.goods.infrastructure.goods.dao; keep LOCAL/guards enabled;
no Jianshen keys yet (Step6 owns permission configuration of same files); verify structure parity;
```

- Verification contribution: ProfileKeyParityTest,ProductAggregatePersistenceTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 22 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-dev.yml`

- Purpose: 先于Step4/5真实mapper测试配置同组与受限生命周期
- Symbols: MP product tables/lifecycle/custom-root allowlists
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件存在，应局限所列symbol。
- Dependencies and consumers: MP product tables/lifecycle/custom-root allowlists 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第17个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: MP product tables/lifecycle/custom-root allowlists；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: 此Step先更新MP键；Step6仅新增授权键，重复文件明确按不同section负责。
- Error and edge behavior: 不存在或越租户统一404；状态/旧版本/唯一冲突409并回滚；错误不泄漏token/SQL；品牌/上传行为保留。
- Standards impact: MC-ARCH-001, MC-CONFIG-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001 — 精确manifest/typedkeys/UTC/SQL范围、同profile结构；无Java对象注解装饰配置。
- Literal rule enforcement: Rule 6, Rule 7, Rule 10, Rule 11 — Jackson或SQL实际类型、四profile同键、UTC时间、原Light和MP DDL约束，文档N/A项不引代码。
- Implementation pseudocode:

```yaml
Register spu/sku SINGLE/master_data under MP sharding.tables in application-dev.yml; preserve alloldtable keys;
set mybatis-plus.lifecycle.restore-statements exact SpuDAO.restoreVersionedById and SkuDAO.restoreVersionedById;
local-write-guard.allowed-root-statements mapping only SpuDAO.purgeDeletedById=>spu and SkuDAO.purgeAllBySpu=>sku plus required named state updates;
all fully qualified namespace IDs start top.egon.enjoyshop.goods.infrastructure.goods.dao; keep LOCAL/guards enabled;
no Jianshen keys yet (Step6 owns permission configuration of same files); verify structure parity;
```

- Verification contribution: ProfileKeyParityTest,ProductAggregatePersistenceTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 23 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-test.yml`

- Purpose: 先于Step4/5真实mapper测试配置同组与受限生命周期
- Symbols: MP product tables/lifecycle/custom-root allowlists
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件存在，应局限所列symbol。
- Dependencies and consumers: MP product tables/lifecycle/custom-root allowlists 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第18个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: MP product tables/lifecycle/custom-root allowlists；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: 此Step先更新MP键；Step6仅新增授权键，重复文件明确按不同section负责。
- Error and edge behavior: 不存在或越租户统一404；状态/旧版本/唯一冲突409并回滚；错误不泄漏token/SQL；品牌/上传行为保留。
- Standards impact: MC-ARCH-001, MC-CONFIG-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001 — 精确manifest/typedkeys/UTC/SQL范围、同profile结构；无Java对象注解装饰配置。
- Literal rule enforcement: Rule 6, Rule 7, Rule 10, Rule 11 — Jackson或SQL实际类型、四profile同键、UTC时间、原Light和MP DDL约束，文档N/A项不引代码。
- Implementation pseudocode:

```yaml
Register spu/sku SINGLE/master_data under MP sharding.tables in application-test.yml; preserve alloldtable keys;
set mybatis-plus.lifecycle.restore-statements exact SpuDAO.restoreVersionedById and SkuDAO.restoreVersionedById;
local-write-guard.allowed-root-statements mapping only SpuDAO.purgeDeletedById=>spu and SkuDAO.purgeAllBySpu=>sku plus required named state updates;
all fully qualified namespace IDs start top.egon.enjoyshop.goods.infrastructure.goods.dao; keep LOCAL/guards enabled;
no Jianshen keys yet (Step6 owns permission configuration of same files); verify structure parity;
```

- Verification contribution: ProfileKeyParityTest,ProductAggregatePersistenceTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 24 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-prod.yml`

- Purpose: 先于Step4/5真实mapper测试配置同组与受限生命周期
- Symbols: MP product tables/lifecycle/custom-root allowlists
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件存在，应局限所列symbol。
- Dependencies and consumers: MP product tables/lifecycle/custom-root allowlists 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第19个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: MP product tables/lifecycle/custom-root allowlists；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: 此Step先更新MP键；Step6仅新增授权键，重复文件明确按不同section负责。
- Error and edge behavior: 不存在或越租户统一404；状态/旧版本/唯一冲突409并回滚；错误不泄漏token/SQL；品牌/上传行为保留。
- Standards impact: MC-ARCH-001, MC-CONFIG-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001 — 精确manifest/typedkeys/UTC/SQL范围、同profile结构；无Java对象注解装饰配置。
- Literal rule enforcement: Rule 6, Rule 7, Rule 10, Rule 11 — Jackson或SQL实际类型、四profile同键、UTC时间、原Light和MP DDL约束，文档N/A项不引代码。
- Implementation pseudocode:

```yaml
Register spu/sku SINGLE/master_data under MP sharding.tables in application-prod.yml; preserve alloldtable keys;
set mybatis-plus.lifecycle.restore-statements exact SpuDAO.restoreVersionedById and SkuDAO.restoreVersionedById;
local-write-guard.allowed-root-statements mapping only SpuDAO.purgeDeletedById=>spu and SkuDAO.purgeAllBySpu=>sku plus required named state updates;
all fully qualified namespace IDs start top.egon.enjoyshop.goods.infrastructure.goods.dao; keep LOCAL/guards enabled;
no Jianshen keys yet (Step6 owns permission configuration of same files); verify structure parity;
```

- Verification contribution: ProfileKeyParityTest,ProductAggregatePersistenceTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 25 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/resources/egon-mybatis-plus-sharding.yml`

- Purpose: 先于Step4/5真实mapper测试配置同组与受限生命周期
- Symbols: MP product tables/lifecycle/custom-root allowlists
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件存在，应局限所列symbol。
- Dependencies and consumers: MP product tables/lifecycle/custom-root allowlists 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第20个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: MP product tables/lifecycle/custom-root allowlists；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: 此Step先更新MP键；Step6仅新增授权键，重复文件明确按不同section负责。
- Error and edge behavior: 不存在或越租户统一404；状态/旧版本/唯一冲突409并回滚；错误不泄漏token/SQL；品牌/上传行为保留。
- Standards impact: MC-ARCH-001, MC-CONFIG-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001 — 精确manifest/typedkeys/UTC/SQL范围、同profile结构；无Java对象注解装饰配置。
- Literal rule enforcement: Rule 6, Rule 7, Rule 10, Rule 11 — Jackson或SQL实际类型、四profile同键、UTC时间、原Light和MP DDL约束，文档N/A项不引代码。
- Implementation pseudocode:

```yaml
Register spu/sku SINGLE/master_data under MP sharding.tables in egon-mybatis-plus-sharding.yml; preserve alloldtable keys;
set mybatis-plus.lifecycle.restore-statements exact SpuDAO.restoreVersionedById and SkuDAO.restoreVersionedById;
local-write-guard.allowed-root-statements mapping only SpuDAO.purgeDeletedById=>spu and SkuDAO.purgeAllBySpu=>sku plus required named state updates;
all fully qualified namespace IDs start top.egon.enjoyshop.goods.infrastructure.goods.dao; keep LOCAL/guards enabled;
no Jianshen keys yet (Step6 owns permission configuration of same files); verify structure parity;
```

- Verification contribution: ProfileKeyParityTest,ProductAggregatePersistenceTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

- Validation working directory: `/Users/mario/SelfProject/enjoyshop/enjoyshop-service/enjoyshop-service-goods`；GEN plan/apply的cwd另为`/Users/mario/SelfProject/enjoyshop`。
- Verification command: `./mvnw -o -B -ntp -Dtest=ProductConverterTest,ProductQuerySqlShapeTest test`；`./mvnw -o -B -ntp -DskipTests compile`；exact checks见本StepManual Checks与§8，同步review该Step diff。
- Expected result: exit0，所有selector测试实际执行、0失败/错误；跳过/无test或classpath不解析不算PASS；具名SQL与BaseConverter字段映射可验证，无通用Wrapper/直接DAO业务调用。
- Failure returns to: 本Step负责该断言的File；DDL/hash/generator冲突回Step1，字段mapping回Step2/3，事务与state回Step4/5，身份/fixture回Step6，wire/OAS回Step7；GEN冲突或实际schema/API不符仍立即停止，不伪造tooling输出。
- Completion criteria: 本Step全部REQ有focused证据，Manual Check逐项有真实结果；最终SINGLE/tenant/版本/字段/旧边界与主Spec一致；无无关diff。
- Rollback: 未执行DDL前仅恢复本Step拥有文件且保留用户preexistingchanges；若用户已运行DDL则保留历史SQL/checksum，只允许后续forward-fix，不能自动DROP/repair。
- Commit paths: enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/application/goods/ProductConverterTest.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/service/impl/ProductSpuPersistenceConverter.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/service/impl/ProductSkuPersistenceConverter.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/ProductApplicationConverter.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/ProductQueryConverter.java; enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/goods/ProductQuerySqlShapeTest.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/dao/SpuDAO.java; enjoyshop-service/enjoyshop-service-goods/src/main/resources/mybatis/mapper/goods/SpuDAO.xml; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/repo/SpuRepository.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/dao/SkuDAO.java; enjoyshop-service/enjoyshop-service-goods/src/main/resources/mybatis/mapper/goods/SkuDAO.xml; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/repo/SkuRepository.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/dao/CategoryBrandDAO.java; enjoyshop-service/enjoyshop-service-goods/src/main/resources/mybatis/mapper/goods/CategoryBrandDAO.xml; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/repo/CategoryBrandRepository.java; enjoyshop-service/enjoyshop-service-goods/src/main/resources/application.yml; enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-dev.yml; enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-test.yml; enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-prod.yml; enjoyshop-service/enjoyshop-service-goods/src/main/resources/egon-mybatis-plus-sharding.yml; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/service/impl/ProductJsonFieldConverter.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/ProductSpuSaveConverter.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/ProductSpuResultConverter.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/ProductSkuSaveConverter.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/ProductSkuResultConverter.java
- Commit: Deferred — 用户要求完整logical task最多1次commit；以上路径仅本Step归属，不创建阶段commit。

### Step 4 — 完成可查询、可修订的原子商品聚合

- Requirements: REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-013, REQ-015
- Dependencies: Step 3 GREEN checkpoint（按用户约束无逐Step commit）
- Baseline state: 前一Step的明确End state与focused tests已验证；已存在路径只改本Step列出的符号
- Observable outcome: 保存/修订/详情/列表共享正确事务与稳定SKU身份
- End state: 仅内部领域能力已GREEN，没有商品HTTP暴露；状态命令下一Step闭合
- Test-first gate: Required — 接口契约先编译，Unit与H2事务失败测试先RED；不以缺基础设施作为RED
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-JSON-001, MC-TIME-001, MC-CONFIG-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001；每项适用性按文件以下具体证据检查，不以通用规则代替实际校验。
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；普通对象/业务Bean/约束/SQL/配置分别按文件职责生效，未涉及电话/Event不新增依赖。
- Ordered files:

#### File 1 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/ProductDomainService.java`

- Purpose: 先建立Spec规定的真实领域端口签名编译前置
- Symbols: create/update/detail/page/audit/pull/put/delete/restore/purge
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: create/update/detail/page/audit/pull/put/delete/restore/purge 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第1个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: create/update/detail/page/audit/pull/put/delete/restore/purge；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: 纯BO/Query/标量版本，shared core PageResult；尚无可调用HTTP实现。
- Error and edge behavior: 不存在或越租户统一404；状态/旧版本/唯一冲突409并回滚；错误不泄漏token/SQL；品牌/上传行为保留。
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-PATTERN-001, MC-SCOPE-001 — carrier用完整class Lombok；business namedBean+Slf4j+RequiredArgsConstructor/finalQualifier；converter继承Common；不可变record仅私有规则值；annotation/enum/interface无非法@Data。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11 — 语义suffix/具体组、class注解、BaseConverter、Bean命名及lombok传播、JDK/Commons、Jackson、Specification仅复杂lifecycle、java.time、exactLight；按实际类型执行而非机械加注解。
- Implementation pseudocode:

```java
@Validated interface ProductDomainService {
 ProductBO create(@Valid ProductBO value); ProductBO update(@Valid ProductBO value);
 ProductBO detail(@Valid ProductDomainQuery query); PageResultRecord<SpuBO> page(@Valid ProductDomainQuery query);
 ProductStateBO audit/pull/put/restore(@Positive Long spuId,@Min(0) Long expectedVersion);
 boolean delete/purge(@Positive Long spuId,@Min(0) Long expectedVersion);
}
Only declared Spec signatures; no PO/Security/HTTP/DAO leaked;
```

- Verification contribution: ProductUpdateTest,ProductAggregatePersistenceTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 2 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/goods/ProductUpdateTest.java`

- Purpose: 保存规则Unit RED先于Domain实现
- Symbols: newIdsStableExistingIds,invalidReferences,rollbackConflicts
- Repository evidence: 现有BrandManageImplTest/BrandDomainService调用模型/Repo mocks可复用；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: newIdsStableExistingIds,invalidReferences,rollbackConflicts 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第2个文件；先固定observable RED，再补最小implementation；编译契约前置与实际RED执行点已由Test-first gate说明。
- Contract/signature changes: newIdsStableExistingIds,invalidReferences,rollbackConflicts；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: 完整替换聚合，SPU sn与旧SKU sn不可变，修改后status0/down、父version+1。
- Error and edge behavior: 不存在或越租户统一404；状态/旧版本/唯一冲突409并回滚；错误不泄漏token/SQL；品牌/上传行为保留。
- Standards impact: MC-TEST-001, MC-SCOPE-001, MC-VALID-001 — JUnit/MockMvc/现有fixture；nativegroups/真实结果断言，不建外部runtime；测试不是businessBean，沿用现有test injection风格。
- Literal rule enforcement: Rule 2, Rule 11 — 所选边界正负例/同模块隔离；不把H2或fake当PG/平台证明。
- Implementation pseudocode:

```java
@Test retains_existing_sku_identity() {
  arrange active/down SPU version3 and two SKU IDs, valid same-tenant catalog;
  command contains one existing ID and one newSKU; expect same existing id/createTime/sn/counters, new ID from common Snowflake, missingold marked E;
  invalid parent/id/tenant/upperCategory/template/spec rejects before writes;
  mock child CAS0 -> assert exception and no success result;
}
```

- Verification contribution: ProductUpdateTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 测试已写；明确RED执行点需要的compile prerequisites完成后运行，不以fixture/环境失败凑RED。

#### File 3 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/goods/ProductAggregatePersistenceTest.java`

- Purpose: 先定义多表原子性与真实mapper验收
- Symbols: createRollback,editCas,consistentQueries
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: createRollback,editCas,consistentQueries 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第3个文件；先固定observable RED，再补最小implementation；编译契约前置与实际RED执行点已由Test-first gate说明。
- Contract/signature changes: createRollback,editCas,consistentQueries；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: H2仅业务事务/SQL绑定；PG unique/locking/DDL另外手工门，不自动启动。
- Error and edge behavior: 不存在或越租户统一404；状态/旧版本/唯一冲突409并回滚；错误不泄漏token/SQL；品牌/上传行为保留。
- Standards impact: MC-TEST-001, MC-SCOPE-001, MC-VALID-001 — JUnit/MockMvc/现有fixture；nativegroups/真实结果断言，不建外部runtime；测试不是businessBean，沿用现有test injection风格。
- Literal rule enforcement: Rule 2, Rule 11 — 所选边界正负例/同模块隔离；不把H2或fake当PG/平台证明。
- Implementation pseudocode:

```java
use Product-specific H2 test context with datasource overrides from PersistenceTestSupport and DDL disabled;
insert catalog fixtures tenant1/2; call public ProductDomainService proxy;
assert failing nthSKU insert leaves no SPU/SKU/newcategoryBrand, and parent rollback on child CAS failure;
assert aggregate edit version3->4 once, activeSKU≤100, ID order stable; detail/recycled read share snapshot;
assert list/count same filters and ≤100, page no SKU query N+1; realPG gates kept separate/manual;
```

- Verification contribution: ProductAggregatePersistenceTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 测试已写；明确RED执行点需要的compile prerequisites完成后运行，不以fixture/环境失败凑RED。

#### File 4 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/test/resources/mybatis/h2-schema.sql`

- Purpose: 为已定义真实mapper测试提供隔离逻辑表
- Symbols: spu/sku H2-equivalent fixtures
- Repository evidence: 现有PersistenceTestSupport每测试创建隔离mem数据库并SHUTDOWN；当前文件存在，应局限所列symbol。
- Dependencies and consumers: spu/sku H2-equivalent fixtures 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第4个文件；先固定observable RED，再补最小implementation；编译契约前置与实际RED执行点已由Test-first gate说明。
- Contract/signature changes: spu/sku H2-equivalent fixtures；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: 严格按商品Spec的字段/默认/null/状态/版本定义映射；业务ID十进制字符串，持久BIGINT；审计Instant/UTC deletedAt微秒。
- Error and edge behavior: 不存在或越租户统一404；状态/旧版本/唯一冲突409并回滚；错误不泄漏token/SQL；品牌/上传行为保留。
- Standards impact: MC-TEST-001, MC-SCOPE-001, MC-VALID-001 — JUnit/MockMvc/现有fixture；nativegroups/真实结果断言，不建外部runtime；测试不是businessBean，沿用现有test injection风格。
- Literal rule enforcement: Rule 2, Rule 11 — 所选边界正负例/同模块隔离；不把H2或fake当PG/平台证明。
- Implementation pseudocode:

```sql
append H2-compatible spu/sku definitions matching business columns/base audit and simple checks;
retain existing brand/catalog/example tables exactly; no test table rows seeded globally;
partial-unique PostgreSQL syntax not supported by H2 must not be claimed proven; dedicated PG acceptance covers it;
no database containers or network autostart;
```

- Verification contribution: ProductAggregatePersistenceTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 测试已写；明确RED执行点需要的compile prerequisites完成后运行，不以fixture/环境失败凑RED。

#### File 5 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/service/impl/ProductDomainServiceImpl.java`

- Purpose: 让聚合新增/编辑与只读查询按Spec原子执行
- Symbols: create/update/detail/page + declared lifecycle slots
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: create/update/detail/page + declared lifecycle slots 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第5个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: create/update/detail/page + declared lifecycle slots；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: 禁止客户端tenant/counters/audit，数据库fill权威；未知commit不自动重发，GET核对。
- Error and edge behavior: 章节5未完的端口不得用空成功实现；本Step实现可编译的拒绝型guard占位只为已声明未暴露能力，Step5立即替换并完成所有端口。
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-PATTERN-001, MC-SCOPE-001 — carrier用完整class Lombok；business namedBean+Slf4j+RequiredArgsConstructor/finalQualifier；converter继承Common；不可变record仅私有规则值；annotation/enum/interface无非法@Data。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11 — 语义suffix/具体组、class注解、BaseConverter、Bean命名及lombok传播、JDK/Commons、Jackson、Specification仅复杂lifecycle、java.time、exactLight；按实际类型执行而非机械加注解。
- Implementation pseudocode:

```java
@Service("productDomainService") @Validated @Slf4j @RequiredArgsConstructor
final @Qualifier spuRepository,skuRepository,categoryBrandRepository,productSpuPersistenceConverterImpl,productSkuPersistenceConverterImpl;
@Transactional create(value): validate directory locks/same tenant/parent chain/template/spec/para; ensure active categoryBrand; assign common Snowflake via guarded save; name SKU by ordered template(seq,id), derive brand/category fields; initialize counters0/status0/down; save all; any failure rollback;
@Transactional update(value): directory locks then parent FOR UPDATE; expectedVersion and active/down/sn invariant; classify SKU existing/new/omitted; oldIDs belong parent+tenant; preserveid/createTime/counters; omittedE via guardeddelete; parent CAS increments once, childCAS checked;
@Transactional(readOnly=true,isolation=REPEATABLE_READ) detail/page: named repositories only, shared snapshot, no events/writes;
Do not wire public lifecycle endpoints until Step5 complete; domain lifecycle declared methods remain unreachable feature slice until Step5, no false success stubs;
Return typed BO through MapStruct persistence converters; log success only committed outcome;
```

- Verification contribution: ProductUpdateTest,ProductAggregatePersistenceTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 6 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/architecture/LightPersistenceArchitectureTest.java`

- Purpose: 随真实新增Product实现提高准确计数
- Symbols: DomainServiceImpl exact9
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件存在，应局限所列symbol。
- Dependencies and consumers: DomainServiceImpl exact9 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第6个文件；先固定observable RED，再补最小implementation；编译契约前置与实际RED执行点已由Test-first gate说明。
- Contract/signature changes: DomainServiceImpl exact9；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: 只改准确增量8→9，不放宽架构检查。
- Error and edge behavior: 不存在或越租户统一404；状态/旧版本/唯一冲突409并回滚；错误不泄漏token/SQL；品牌/上传行为保留。
- Standards impact: MC-TEST-001, MC-SCOPE-001, MC-VALID-001 — JUnit/MockMvc/现有fixture；nativegroups/真实结果断言，不建外部runtime；测试不是businessBean，沿用现有test injection风格。
- Literal rule enforcement: Rule 2, Rule 11 — 所选边界正负例/同模块隔离；不把H2或fake当PG/平台证明。
- Implementation pseudocode:

```java
retain exact 13 PO/DAO/Repository inventories;
assertEquals(9, count(DomainServiceImpl)); include ProductDomainServiceImpl in no DAO/directAR check;
verify domain contracts import no infrastructure PO/Security/HTTP and Product service composesRepositories;
preserve every earlier invariant;
```

- Verification contribution: LightPersistenceArchitectureTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 测试已写；明确RED执行点需要的compile prerequisites完成后运行，不以fixture/环境失败凑RED。

- Validation working directory: `/Users/mario/SelfProject/enjoyshop/enjoyshop-service/enjoyshop-service-goods`；GEN plan/apply的cwd另为`/Users/mario/SelfProject/enjoyshop`。
- Verification command: `./mvnw -o -B -ntp -Dtest=ProductUpdateTest,ProductAggregatePersistenceTest,ProductQuerySqlShapeTest,LightPersistenceArchitectureTest test`；`./mvnw -o -B -ntp -DskipTests compile`；exact checks见本StepManual Checks与§8，同步review该Step diff。
- Expected result: exit0，所有selector测试实际执行、0失败/错误；跳过/无test或classpath不解析不算PASS；保存/修订/详情/列表共享正确事务与稳定SKU身份。
- Failure returns to: 本Step负责该断言的File；DDL/hash/generator冲突回Step1，字段mapping回Step2/3，事务与state回Step4/5，身份/fixture回Step6，wire/OAS回Step7；GEN冲突或实际schema/API不符仍立即停止，不伪造tooling输出。
- Completion criteria: 本Step全部REQ有focused证据，Manual Check逐项有真实结果；最终SINGLE/tenant/版本/字段/旧边界与主Spec一致；无无关diff。
- Rollback: 未执行DDL前仅恢复本Step拥有文件且保留用户preexistingchanges；若用户已运行DDL则保留历史SQL/checksum，只允许后续forward-fix，不能自动DROP/repair。
- Commit paths: enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/ProductDomainService.java; enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/goods/ProductUpdateTest.java; enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/goods/ProductAggregatePersistenceTest.java; enjoyshop-service/enjoyshop-service-goods/src/test/resources/mybatis/h2-schema.sql; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/service/impl/ProductDomainServiceImpl.java; enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/architecture/LightPersistenceArchitectureTest.java
- Commit: Deferred — 用户要求完整logical task最多1次commit；以上路径仅本Step归属，不创建阶段commit。

### Step 5 — 完成审核销售状态与完整回收站生命周期

- Requirements: REQ-007, REQ-008, REQ-009, REQ-010, REQ-011, REQ-012, REQ-015
- Dependencies: Step 4 GREEN checkpoint（按用户约束无逐Step commit）
- Baseline state: 前一Step的明确End state与focused tests已验证；已存在路径只改本Step列出的符号
- Observable outcome: 审核/上下架/软删/恢复/永久删除全端口同规则，P/E历史正确
- End state: 所有Domain端口实现；尚无公共HTTP，平台权限下一Step
- Test-first gate: Required — 纯rule矩阵与H2 P/E状态效果先RED；再实现Specification和Domain状态方法
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-JSON-001, MC-TIME-001, MC-CONFIG-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001；每项适用性按文件以下具体证据检查，不以通用规则代替实际校验。
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；普通对象/业务Bean/约束/SQL/配置分别按文件职责生效，未涉及电话/Event不新增依赖。
- Ordered files:

#### File 1 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/domain/goods/ProductLifecycleComponentTest.java`

- Purpose: 先固定真实Specification状态规则
- Symbols: transitionMatrix,invalidCombinations,noOpVersions
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: transitionMatrix,invalidCombinations,noOpVersions 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第1个文件；先固定observable RED，再补最小implementation；编译契约前置与实际RED执行点已由Test-first gate说明。
- Contract/signature changes: transitionMatrix,invalidCombinations,noOpVersions；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: 只测组合guard/目标状态，不调用DB、平台或消息。
- Error and edge behavior: 不存在或越租户统一404；状态/旧版本/唯一冲突409并回滚；错误不泄漏token/SQL；品牌/上传行为保留。
- Standards impact: MC-TEST-001, MC-SCOPE-001, MC-VALID-001 — JUnit/MockMvc/现有fixture；nativegroups/真实结果断言，不建外部runtime；测试不是businessBean，沿用现有test injection风格。
- Literal rule enforcement: Rule 2, Rule 11 — 所选边界正负例/同模块隔离；不把H2或fake当PG/平台证明。
- Implementation pseudocode:

```java
enumerate each ProductActionEnum × valid/invalid current state × version × hasActiveSku × referencesValid;
assert audit=>approved/up; pull keepsapproved; put requiresapproved+normalSKU; edit resetsreview onlydown;
assert delete requiresdown, restore onlydeleted, purge onlydeleted; oldversion always409 even targetalreadytrue;
assert registry coversallactions and rejectsmissingrule; no client string action dispatch;
```

- Verification contribution: ProductLifecycleComponentTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 测试已写；明确RED执行点需要的compile prerequisites完成后运行，不以fixture/环境失败凑RED。

#### File 2 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/ProductLifecycleComponent.java`

- Purpose: 实现Spec已选Specification规则组合
- Symbols: evaluate(action,current); private ProductTransitionBO
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: evaluate(action,current); private ProductTransitionBO 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第2个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: evaluate(action,current); private ProductTransitionBO；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: rule configuration immutable/noEntity state；Domain仍持有事务，不创建Spring Statemachine依赖。
- Error and edge behavior: 不存在或越租户统一404；状态/旧版本/唯一冲突409并回滚；错误不泄漏token/SQL；品牌/上传行为保留。
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-PATTERN-001, MC-SCOPE-001 — carrier用完整class Lombok；business namedBean+Slf4j+RequiredArgsConstructor/finalQualifier；converter继承Common；不可变record仅私有规则值；annotation/enum/interface无非法@Data。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11 — 语义suffix/具体组、class注解、BaseConverter、Bean命名及lombok传播、JDK/Commons、Jackson、Specification仅复杂lifecycle、java.time、exactLight；按实际类型执行而非机械加注解。
- Implementation pseudocode:

```java
@Component("productLifecycleComponent") @Slf4j
EnumMap<ProductActionEnum,ProductTransitionBO> registry; ProductTransitionBO immutable nestedrecord(Predicate,Function,errorCode,sideEffectCategory);
compose notDeleted/down/approved/currentVersion/referencesValid/activeSKU predicates; register allactions once and fail missing registration;
evaluate(action,current) checks rule and returns new ProductStateBO; do not switch/if dispatch by client status;
keep ID/version metadata and guard result explicit; terminal errors CommonException withproductstate/versioncode;
```

- Verification contribution: ProductLifecycleComponentTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 3 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/goods/ProductAggregatePersistenceTest.java`

- Purpose: 先为状态写与历史清理加入真实事务RED
- Symbols: P/E delete/restore/purge/adversarialCAS
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: P/E delete/restore/purge/adversarialCAS 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第3个文件；先固定observable RED，再补最小implementation；编译契约前置与实际RED执行点已由Test-first gate说明。
- Contract/signature changes: P/E delete/restore/purge/adversarialCAS；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: 全部父/子状态一次commit；历史E不借restore复活。
- Error and edge behavior: 不存在或越租户统一404；状态/旧版本/唯一冲突409并回滚；错误不泄漏token/SQL；品牌/上传行为保留。
- Standards impact: MC-TEST-001, MC-SCOPE-001, MC-VALID-001 — JUnit/MockMvc/现有fixture；nativegroups/真实结果断言，不建外部runtime；测试不是businessBean，沿用现有test injection风格。
- Literal rule enforcement: Rule 2, Rule 11 — 所选边界正负例/同模块隔离；不把H2或fake当PG/平台证明。
- Implementation pseudocode:

```java
createapproved/up product then assert delete rejects; pull then delete marks parentreview0/down and onlycurrentSKU P;
keep earlierremoved E; restore returns original P ids and never E;
recreate sn/barcode while recycled then restoreconflict rolls parent+allP back;
purge active rejects; recycledpurge removes allP/E history, midwaySQLException restores entireparent; repeatedabsentpurge true;
race edit/audit sameversion: at most one valid aggregatecommit; no child partialwrite;
```

- Verification contribution: ProductAggregatePersistenceTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 测试已写；明确RED执行点需要的compile prerequisites完成后运行，不以fixture/环境失败凑RED。

#### File 4 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/goods/ProductUniquenessTest.java`

- Purpose: 设计有界数据库唯一与重复软删除验收
- Symbols: activeKeys,lifecycleMicros,restoreConflicts
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: activeKeys,lifecycleMicros,restoreConflicts 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第4个文件；先固定observable RED，再补最小implementation；编译契约前置与实际RED执行点已由Test-first gate说明。
- Contract/signature changes: activeKeys,lifecycleMicros,restoreConflicts；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: 真实PG并发与索引EXECUTE/EXPLAIN留明确manualgate，不能用H2成功冒充。
- Error and edge behavior: 不存在或越租户统一404；状态/旧版本/唯一冲突409并回滚；错误不泄漏token/SQL；品牌/上传行为保留。
- Standards impact: MC-TEST-001, MC-SCOPE-001, MC-VALID-001 — JUnit/MockMvc/现有fixture；nativegroups/真实结果断言，不建外部runtime；测试不是businessBean，沿用现有test injection风格。
- Literal rule enforcement: Rule 2, Rule 11 — 所选边界正负例/同模块隔离；不把H2或fake当PG/平台证明。
- Implementation pseudocode:

```java
H2 test checks portable duplicate/mapping behavior only, tag source expectations separately;
verify SQL shape of unique lifecycle+active pairs and monotoneUTC, cross-tenant keys independent;
manual PostgreSQL cases require existing user-authorized database, no Testcontainers: two concurrent creates/restores, same micro timestamp repeateddelete;
assert zeroorphans/invalidP-E/duplicateactive keys and complete rollback on uniqueness violation;
```

- Verification contribution: ProductUniquenessTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 测试已写；明确RED执行点需要的compile prerequisites完成后运行，不以fixture/环境失败凑RED。

#### File 5 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/service/impl/ProductDomainServiceImpl.java`

- Purpose: 让所有领域端口遵守同一规则与历史生命周期
- Symbols: audit/pull/put/delete/restore/purge
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: audit/pull/put/delete/restore/purge 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第5个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: audit/pull/put/delete/restore/purge；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: 审计填充/父+子版本递增/状态guard同一源，restore不修改已清0的review。
- Error and edge behavior: PG事务duplicateKey直接abort不吞；永删必须deleted且权限由Manage层后续兜住；组件只内部可见。
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-PATTERN-001, MC-SCOPE-001 — carrier用完整class Lombok；business namedBean+Slf4j+RequiredArgsConstructor/finalQualifier；converter继承Common；不可变record仅私有规则值；annotation/enum/interface无非法@Data。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11 — 语义suffix/具体组、class注解、BaseConverter、Bean命名及lombok传播、JDK/Commons、Jackson、Specification仅复杂lifecycle、java.time、exactLight；按实际类型执行而非机械加注解。
- Implementation pseudocode:

```java
inject final @Qualifier("productLifecycleComponent") ProductLifecycleComponent;
@Transactional lifecycle: load trusted parent(include tombstone for delete/no-op/restore/purge), lock, verifyexpectedVersion, evaluate selectedRule;
Audit/Put revalidate active references+≥1 status1SKU; CAS parent once; duplicate samecurrentversion target mayno-op;
Delete: custom inherited mapper deleteVersionedById resetsstatus0/down and monotoneUTC, eachactiveSKU deleteVersionedById carries kindP; Ehistory untouched;
Restore: allowed SpuDAO.restoreVersionedById and SkuDAO.restoreVersionedById clear onlyP marker+deletedAt same tx; anysn/barcode conflict abort all;
Purge: lockdeletedparent, count allhistory, deleteallSKU then deletedSPU(id/tenant/version); noactivefilter, noimages/catalogdelete; missingparenttrue;
rollback any0-effect/unique/child failure; no autoCASretry or businessEvent;
```

- Verification contribution: ProductLifecycleComponentTest,ProductAggregatePersistenceTest,ProductUniquenessTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

- Validation working directory: `/Users/mario/SelfProject/enjoyshop/enjoyshop-service/enjoyshop-service-goods`；GEN plan/apply的cwd另为`/Users/mario/SelfProject/enjoyshop`。
- Verification command: `./mvnw -o -B -ntp -Dtest=ProductLifecycleComponentTest,ProductAggregatePersistenceTest,ProductUniquenessTest test`；`./mvnw -o -B -ntp -DskipTests compile`；exact checks见本StepManual Checks与§8，同步review该Step diff。
- Expected result: exit0，所有selector测试实际执行、0失败/错误；跳过/无test或classpath不解析不算PASS；审核/上下架/软删/恢复/永久删除全端口同规则，P/E历史正确。
- Failure returns to: 本Step负责该断言的File；DDL/hash/generator冲突回Step1，字段mapping回Step2/3，事务与state回Step4/5，身份/fixture回Step6，wire/OAS回Step7；GEN冲突或实际schema/API不符仍立即停止，不伪造tooling输出。
- Completion criteria: 本Step全部REQ有focused证据，Manual Check逐项有真实结果；最终SINGLE/tenant/版本/字段/旧边界与主Spec一致；无无关diff。
- Rollback: 未执行DDL前仅恢复本Step拥有文件且保留用户preexistingchanges；若用户已运行DDL则保留历史SQL/checksum，只允许后续forward-fix，不能自动DROP/repair。
- Commit paths: enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/domain/goods/ProductLifecycleComponentTest.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/ProductLifecycleComponent.java; enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/goods/ProductAggregatePersistenceTest.java; enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/goods/ProductUniquenessTest.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/service/impl/ProductDomainServiceImpl.java
- Commit: Deferred — 用户要求完整logical task最多1次commit；以上路径仅本Step归属，不创建阶段commit。

### Step 6 — 接入可信身份和四类商品操作授权

- Requirements: REQ-014, REQ-015, REQ-018, REQ-021, REQ-101, REQ-107, REQ-108
- Dependencies: Step 5 GREEN checkpoint（按用户约束无逐Step commit）
- Baseline state: 前一Step的明确End state与focused tests已验证；已存在路径只改本Step列出的符号
- Observable outcome: 十个Manage入口与新安全链失败关闭，4权限独立；旧路由策略保留
- End state: 未创建ProductController之前已具备安全门；需用户平台部署配置和live验收
- Test-first gate: Required — 权限三态Unit及未带token的HTTP必须401先RED；再starter/Component/配置链GREEN
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-JSON-001, MC-TIME-001, MC-CONFIG-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001, MC-DEP-001, MC-BLOCKER-001；每项适用性按文件以下具体证据检查，不以通用规则代替实际校验。
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；普通对象/业务Bean/约束/SQL/配置分别按文件职责生效，未涉及电话/Event不新增依赖。
- Ordered files:

#### File 1 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/application/goods/ProductAuthorizationComponentTest.java`

- Purpose: 先定义可信权限三态与4操作独立授权
- Symbols: allowDenyIndeterminate,resourceBoundIdentity
- Repository evidence: Jianshen5.4.1本地jar AuthorizationService签名与Rbac3ContextAuthentication已核实，现有Mockito测试工具可用；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: allowDenyIndeterminate,resourceBoundIdentity 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第1个文件；先固定observable RED，再补最小implementation；编译契约前置与实际RED执行点已由Test-first gate说明。
- Contract/signature changes: allowDenyIndeterminate,resourceBoundIdentity；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: 严格按商品Spec的字段/默认/null/状态/版本定义映射；业务ID十进制字符串，持久BIGINT；审计Instant/UTC deletedAt微秒。
- Error and edge behavior: 不存在或越租户统一404；状态/旧版本/唯一冲突409并回滚；错误不泄漏token/SQL；品牌/上传行为保留。
- Standards impact: MC-TEST-001, MC-SCOPE-001, MC-VALID-001 — JUnit/MockMvc/现有fixture；nativegroups/真实结果断言，不建外部runtime；测试不是businessBean，沿用现有test injection风格。
- Literal rule enforcement: Rule 2, Rule 11 — 所选边界正负例/同模块隔离；不把H2或fake当PG/平台证明。
- Implementation pseudocode:

```java
mock existing AuthorizationService; arrange verified Rbac3ContextAuthentication(identity,snapshot system enjoyshop-goods);
for read/write/audit/purge PermissionRequest.api(code,"enjoyshop.product") assert requiredpermission exact and noinheritwrite->audit/purge;
ALLOW sameidentity passes; DENY=>610206/403; INDETERMINATE/null/missingRBAC=>610208/503; unauthenticated=>Common401000;
assert spoofedheaders/tenant snapshot mismatch never reach Domain; no local users/roles lookup or newpermissionscache;
```

- Verification contribution: ProductAuthorizationComponentTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 测试已写；明确RED执行点需要的compile prerequisites完成后运行，不以fixture/环境失败凑RED。

#### File 2 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/start/config/ProductSecurityContractTest.java`

- Purpose: 在安全链改动前固定失败关闭与旧边界
- Symbols: matcherOrder,missingBeans,profileParity
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: matcherOrder,missingBeans,profileParity 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第2个文件；先固定observable RED，再补最小implementation；编译契约前置与实际RED执行点已由Test-first gate说明。
- Contract/signature changes: matcherOrder,missingBeans,profileParity；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: 无实际Token端点/Redis调用，fakes同时覆盖403/503filter双字段与trustedIdentityscope。
- Error and edge behavior: 不存在或越租户统一404；状态/旧版本/唯一冲突409并回滚；错误不泄漏token/SQL；品牌/上传行为保留。
- Standards impact: MC-TEST-001, MC-SCOPE-001, MC-VALID-001 — JUnit/MockMvc/现有fixture；nativegroups/真实结果断言，不建外部runtime；测试不是businessBean，沿用现有test injection风格。
- Literal rule enforcement: Rule 2, Rule 11 — 所选边界正负例/同模块隔离；不把H2或fake当PG/平台证明。
- Implementation pseudocode:

```java
with isolated existing testcontext and fake Shoubing/Jianshen beans, request /api/v1/products withouttoken: expect401 (currentfallback404=>RED);
assert IDP→RBAC→Anonymous order; SERVICEnotaccepted; trustedMDC override clears afterrequest;
verify requiredbeans missing makes context startupfail, both filterregistrationsfalse, no silentObjectProvider omission;
oldbrand/Spec noBearer allowed and fileUPLOAD onlyoldidentitychain; CORS PUT/DELETE allowlistedOrigin only;
```

- Verification contribution: ProductSecurityContractTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 测试已写；明确RED执行点需要的compile prerequisites完成后运行，不以fixture/环境失败凑RED。

#### File 3 — `MODIFY enjoyshop-service/enjoyshop-service-goods/pom.xml`

- Purpose: 最小增加已获批准的真实操作授权starter
- Symbols: dependency Jianshen starter5.4.1
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件存在，应局限所列symbol。
- Dependencies and consumers: dependency Jianshen starter5.4.1 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第3个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: dependency Jianshen starter5.4.1；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: 原goods缺RBACOperationdecoder能力；starter已有快照/permission/resource三态，授权原文已由DEC207批准。
- Error and edge behavior: 不存在或越租户统一404；状态/旧版本/唯一冲突409并回滚；错误不泄漏token/SQL；品牌/上传行为保留。
- Standards impact: MC-ARCH-001, MC-CONFIG-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001 — 精确manifest/typedkeys/UTC/SQL范围、同profile结构；无Java对象注解装饰配置。
- Literal rule enforcement: Rule 6, Rule 7, Rule 10, Rule 11 — Jackson或SQL实际类型、四profile同键、UTC时间、原Light和MP DDL约束，文档N/A项不引代码。
- Implementation pseudocode:

```xml
add top.egon:egon-cola-tianquan-jianshen-starter:5.4.1 only; preserve Egonparent,ID/MP/version and existingShoubing;
inspect dependency:tree for Jianshen contract/core,Security/AOP,Common data-desensitize,redisson-starter; no duplicate version pins;
retain existing RedissonAutoConfigurationV2 exclusion in configs; no FreeMarker in business POM;
offline classpath/API inspection verifies exact runtime method signatures, no service startup/download without approved dependency resolution scope;
```

- Verification contribution: ProductAuthorizationComponentTest,dependency:tree；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 4 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/ProductAuthorizationComponent.java`

- Purpose: 在每个真实Manage入口复用既有权限决策
- Symbols: void requirePermission(String permissionCode)
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: void requirePermission(String permissionCode) 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第4个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: void requirePermission(String permissionCode)；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: 现有JianshenCtx→permission+resource→CommonException；identity/tenant不来自Headers。
- Error and edge behavior: 不存在或越租户统一404；状态/旧版本/唯一冲突409并回滚；错误不泄漏token/SQL；品牌/上传行为保留。
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-PATTERN-001, MC-SCOPE-001 — carrier用完整class Lombok；business namedBean+Slf4j+RequiredArgsConstructor/finalQualifier；converter继承Common；不可变record仅私有规则值；annotation/enum/interface无非法@Data。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11 — 语义suffix/具体组、class注解、BaseConverter、Bean命名及lombok传播、JDK/Commons、Jackson、Specification仅复杂lifecycle、java.time、exactLight；按实际类型执行而非机械加注解。
- Implementation pseudocode:

```java
@Component("productAuthorizationComponent") @Validated @Slf4j @RequiredArgsConstructor
final @Qualifier("authorizationService") AuthorizationService authorization;
requirePermission(@NotBlank code): trusted USER+RBACcontext only; validate system/tenant/subject binding and MDC matches positiveLongtenant;
decision=authorization.requirePermission(PermissionRequest.api(code,"enjoyshop.product"));
ALLOW only passes; DENY Common610206; null/INDETERMINATE/brokenRBACContext Common610208; missingUSER Common401000;
no cache/store/fetch client here; record trace/action/error withouttoken;
```

- Verification contribution: ProductAuthorizationComponentTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 5 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/ProductManage.java`

- Purpose: 应用端口共享领域能力与输出合同
- Symbols: all ten operations per INTERNAL201–210
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: all ten operations per INTERNAL201–210 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第5个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: all ten operations per INTERNAL201–210；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: 所有public方法均有权限门；不省略非HTTP入口校验。
- Error and edge behavior: 不存在或越租户统一404；状态/旧版本/唯一冲突409并回滚；错误不泄漏token/SQL；品牌/上传行为保留。
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-PATTERN-001, MC-SCOPE-001 — carrier用完整class Lombok；business namedBean+Slf4j+RequiredArgsConstructor/finalQualifier；converter继承Common；不可变record仅私有规则值；annotation/enum/interface无非法@Data。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11 — 语义suffix/具体组、class注解、BaseConverter、Bean命名及lombok传播、JDK/Commons、Jackson、Specification仅复杂lifecycle、java.time、exactLight；按实际类型执行而非机械加注解。
- Implementation pseudocode:

```java
@Validated interface ProductManage with exact Spec §9.5 signatures;
create/update accept @Valid sharedCommand using Create/Update groups; update LongspuId positive;
detail/page @Valid respectiveQuery; lifecycle accepts @Valid StateCall ProductLifecycleCommand;
return ProductResult/PageResultRecord<ProductSummaryResult>/ProductStateResult/boolean as Spec; no newRPC/Facade module;
```

- Verification contribution: ProductAuthorizationComponentTest,ProductConverterTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 6 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/ProductManageImpl.java`

- Purpose: 将四类权限和真实聚合领域端口连接
- Symbols: delegation with permission-first gate
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: delegation with permission-first gate 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第6个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: delegation with permission-first gate；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: nativeCommands分组→MapStruct BO→Domain→MapStruct Result，C/Q不引入业务Event。
- Error and edge behavior: 不存在或越租户统一404；状态/旧版本/唯一冲突409并回滚；错误不泄漏token/SQL；品牌/上传行为保留。
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-PATTERN-001, MC-SCOPE-001 — carrier用完整class Lombok；business namedBean+Slf4j+RequiredArgsConstructor/finalQualifier；converter继承Common；不可变record仅私有规则值；annotation/enum/interface无非法@Data。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11 — 语义suffix/具体组、class注解、BaseConverter、Bean命名及lombok传播、JDK/Commons、Jackson、Specification仅复杂lifecycle、java.time、exactLight；按实际类型执行而非机械加注解。
- Implementation pseudocode:

```java
@Service("productManage") @Validated @Slf4j @RequiredArgsConstructor
final qualified ProductAuthorizationComponent,ProductDomainService,ProductApplicationConverter,ProductQueryConverter;
create/update require "enjoyshop:product:write" then validatedCommand→BO (pathid wins, expectedversion); callDomain, project Result;
detail/page require read then typedDomainQuery; page convert SpuBO list to ProductSummaryResult once;
audit requireaudit; purge requirepurge; pull/put/delete/restore requirewrite; forward id/version only, actionnotfromclient;
no Repository/PO/DAO import, no copying audit/serverstate from request, no method self-invocation bypass validation;
```

- Verification contribution: ProductAuthorizationComponentTest,ProductConverterTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 7 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/resources/application.yml`

- Purpose: 同Step覆盖每个环境，保持结构一致
- Symbols: product SINGLE maps/restoreStatements/rootStatements/Jianshen keys
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件存在，应局限所列symbol。
- Dependencies and consumers: product SINGLE maps/restoreStatements/rootStatements/Jianshen keys 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第7个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: product SINGLE maps/restoreStatements/rootStatements/Jianshen keys；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: 五份路由结构保持；四profile权限同keys不同values；仅配置已存在properties字段。
- Error and edge behavior: restoreallowlist/guard exactID mismatches.fail；missingruntimebeans启动失败，测试不自动连Redis。
- Standards impact: MC-ARCH-001, MC-CONFIG-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001 — 精确manifest/typedkeys/UTC/SQL范围、同profile结构；无Java对象注解装饰配置。
- Literal rule enforcement: Rule 6, Rule 7, Rule 10, Rule 11 — Jackson或SQL实际类型、四profile同键、UTC时间、原Light和MP DDL约束，文档N/A项不引代码。
- Implementation pseudocode:

```yaml
In application.yml, register spu and sku under existing MP sharding.tables: type SINGLE,data-source master_data;
MP lifecycle.restore-statements = SpuDAO.restoreVersionedById and SkuDAO.restoreVersionedById exact fully-qualified mapper IDs;
local-write-guard.allowed-root-statements map only customwrite ID→spu or sku; keep guards enabled and default LOCAL;
base/dev/test/prod Jianshen prefix exact Spec §15.1.3 keys: enabled/register-filter=false/system-code enjoyshop-goods, runtime redis*, authorization endpoint/cacheTTL1s/nearTTL0/jitter0/fetchTimeout1s/service-token resource-uri+scopes;
test uses runtimeRedis=false/serviceToken=false with explicit snapshot/clientfakes; base/dev/prod secrets/URIs deployment placeholders, never inline passwords;
imported sharding YAML owns only routing keys, never duplicate Jianshen business keys at wrongpath; preserve old objectstorage/Shoubing/Tianshu keys;
```

- Verification contribution: ProfileKeyParityTest,ProductSecurityContractTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 8 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-dev.yml`

- Purpose: 同Step覆盖每个环境，保持结构一致
- Symbols: product SINGLE maps/restoreStatements/rootStatements/Jianshen keys
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件存在，应局限所列symbol。
- Dependencies and consumers: product SINGLE maps/restoreStatements/rootStatements/Jianshen keys 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第8个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: product SINGLE maps/restoreStatements/rootStatements/Jianshen keys；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: 五份路由结构保持；四profile权限同keys不同values；仅配置已存在properties字段。
- Error and edge behavior: restoreallowlist/guard exactID mismatches.fail；missingruntimebeans启动失败，测试不自动连Redis。
- Standards impact: MC-ARCH-001, MC-CONFIG-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001 — 精确manifest/typedkeys/UTC/SQL范围、同profile结构；无Java对象注解装饰配置。
- Literal rule enforcement: Rule 6, Rule 7, Rule 10, Rule 11 — Jackson或SQL实际类型、四profile同键、UTC时间、原Light和MP DDL约束，文档N/A项不引代码。
- Implementation pseudocode:

```yaml
In application-dev.yml, register spu and sku under existing MP sharding.tables: type SINGLE,data-source master_data;
MP lifecycle.restore-statements = SpuDAO.restoreVersionedById and SkuDAO.restoreVersionedById exact fully-qualified mapper IDs;
local-write-guard.allowed-root-statements map only customwrite ID→spu or sku; keep guards enabled and default LOCAL;
base/dev/test/prod Jianshen prefix exact Spec §15.1.3 keys: enabled/register-filter=false/system-code enjoyshop-goods, runtime redis*, authorization endpoint/cacheTTL1s/nearTTL0/jitter0/fetchTimeout1s/service-token resource-uri+scopes;
test uses runtimeRedis=false/serviceToken=false with explicit snapshot/clientfakes; base/dev/prod secrets/URIs deployment placeholders, never inline passwords;
imported sharding YAML owns only routing keys, never duplicate Jianshen business keys at wrongpath; preserve old objectstorage/Shoubing/Tianshu keys;
```

- Verification contribution: ProfileKeyParityTest,ProductSecurityContractTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 9 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-test.yml`

- Purpose: 同Step覆盖每个环境，保持结构一致
- Symbols: product SINGLE maps/restoreStatements/rootStatements/Jianshen keys
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件存在，应局限所列symbol。
- Dependencies and consumers: product SINGLE maps/restoreStatements/rootStatements/Jianshen keys 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第9个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: product SINGLE maps/restoreStatements/rootStatements/Jianshen keys；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: 五份路由结构保持；四profile权限同keys不同values；仅配置已存在properties字段。
- Error and edge behavior: restoreallowlist/guard exactID mismatches.fail；missingruntimebeans启动失败，测试不自动连Redis。
- Standards impact: MC-ARCH-001, MC-CONFIG-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001 — 精确manifest/typedkeys/UTC/SQL范围、同profile结构；无Java对象注解装饰配置。
- Literal rule enforcement: Rule 6, Rule 7, Rule 10, Rule 11 — Jackson或SQL实际类型、四profile同键、UTC时间、原Light和MP DDL约束，文档N/A项不引代码。
- Implementation pseudocode:

```yaml
In application-test.yml, register spu and sku under existing MP sharding.tables: type SINGLE,data-source master_data;
MP lifecycle.restore-statements = SpuDAO.restoreVersionedById and SkuDAO.restoreVersionedById exact fully-qualified mapper IDs;
local-write-guard.allowed-root-statements map only customwrite ID→spu or sku; keep guards enabled and default LOCAL;
base/dev/test/prod Jianshen prefix exact Spec §15.1.3 keys: enabled/register-filter=false/system-code enjoyshop-goods, runtime redis*, authorization endpoint/cacheTTL1s/nearTTL0/jitter0/fetchTimeout1s/service-token resource-uri+scopes;
test uses runtimeRedis=false/serviceToken=false with explicit snapshot/clientfakes; base/dev/prod secrets/URIs deployment placeholders, never inline passwords;
imported sharding YAML owns only routing keys, never duplicate Jianshen business keys at wrongpath; preserve old objectstorage/Shoubing/Tianshu keys;
```

- Verification contribution: ProfileKeyParityTest,ProductSecurityContractTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 10 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-prod.yml`

- Purpose: 同Step覆盖每个环境，保持结构一致
- Symbols: product SINGLE maps/restoreStatements/rootStatements/Jianshen keys
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件存在，应局限所列symbol。
- Dependencies and consumers: product SINGLE maps/restoreStatements/rootStatements/Jianshen keys 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第10个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: product SINGLE maps/restoreStatements/rootStatements/Jianshen keys；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: 五份路由结构保持；四profile权限同keys不同values；仅配置已存在properties字段。
- Error and edge behavior: restoreallowlist/guard exactID mismatches.fail；missingruntimebeans启动失败，测试不自动连Redis。
- Standards impact: MC-ARCH-001, MC-CONFIG-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001 — 精确manifest/typedkeys/UTC/SQL范围、同profile结构；无Java对象注解装饰配置。
- Literal rule enforcement: Rule 6, Rule 7, Rule 10, Rule 11 — Jackson或SQL实际类型、四profile同键、UTC时间、原Light和MP DDL约束，文档N/A项不引代码。
- Implementation pseudocode:

```yaml
In application-prod.yml, register spu and sku under existing MP sharding.tables: type SINGLE,data-source master_data;
MP lifecycle.restore-statements = SpuDAO.restoreVersionedById and SkuDAO.restoreVersionedById exact fully-qualified mapper IDs;
local-write-guard.allowed-root-statements map only customwrite ID→spu or sku; keep guards enabled and default LOCAL;
base/dev/test/prod Jianshen prefix exact Spec §15.1.3 keys: enabled/register-filter=false/system-code enjoyshop-goods, runtime redis*, authorization endpoint/cacheTTL1s/nearTTL0/jitter0/fetchTimeout1s/service-token resource-uri+scopes;
test uses runtimeRedis=false/serviceToken=false with explicit snapshot/clientfakes; base/dev/prod secrets/URIs deployment placeholders, never inline passwords;
imported sharding YAML owns only routing keys, never duplicate Jianshen business keys at wrongpath; preserve old objectstorage/Shoubing/Tianshu keys;
```

- Verification contribution: ProfileKeyParityTest,ProductSecurityContractTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 11 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/resources/egon-mybatis-plus-sharding.yml`

- Purpose: 同Step覆盖每个环境，保持结构一致
- Symbols: product SINGLE maps/restoreStatements/rootStatements/Jianshen keys
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件存在，应局限所列symbol。
- Dependencies and consumers: product SINGLE maps/restoreStatements/rootStatements/Jianshen keys 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第11个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: product SINGLE maps/restoreStatements/rootStatements/Jianshen keys；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: 五份路由结构保持；四profile权限同keys不同values；仅配置已存在properties字段。
- Error and edge behavior: restoreallowlist/guard exactID mismatches.fail；missingruntimebeans启动失败，测试不自动连Redis。
- Standards impact: MC-ARCH-001, MC-CONFIG-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001 — 精确manifest/typedkeys/UTC/SQL范围、同profile结构；无Java对象注解装饰配置。
- Literal rule enforcement: Rule 6, Rule 7, Rule 10, Rule 11 — Jackson或SQL实际类型、四profile同键、UTC时间、原Light和MP DDL约束，文档N/A项不引代码。
- Implementation pseudocode:

```yaml
In egon-mybatis-plus-sharding.yml, register spu and sku under existing MP sharding.tables: type SINGLE,data-source master_data;
MP lifecycle.restore-statements = SpuDAO.restoreVersionedById and SkuDAO.restoreVersionedById exact fully-qualified mapper IDs;
local-write-guard.allowed-root-statements map only customwrite ID→spu or sku; keep guards enabled and default LOCAL;
base/dev/test/prod Jianshen prefix exact Spec §15.1.3 keys: enabled/register-filter=false/system-code enjoyshop-goods, runtime redis*, authorization endpoint/cacheTTL1s/nearTTL0/jitter0/fetchTimeout1s/service-token resource-uri+scopes;
test uses runtimeRedis=false/serviceToken=false with explicit snapshot/clientfakes; base/dev/prod secrets/URIs deployment placeholders, never inline passwords;
imported sharding YAML owns only routing keys, never duplicate Jianshen business keys at wrongpath; preserve old objectstorage/Shoubing/Tianshu keys;
```

- Verification contribution: ProfileKeyParityTest,ProductSecurityContractTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 12 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/support/ProductTestSecurityConfiguration.java`

- Purpose: 隔离新增RBAC运行时依赖并保持旧fixture契约
- Symbols: isolated RBAC/IDP/snapshot test beans
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: isolated RBAC/IDP/snapshot test beans 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第12个文件；先固定observable RED，再补最小implementation；编译契约前置与实际RED执行点已由Test-first gate说明。
- Contract/signature changes: isolated RBAC/IDP/snapshot test beans；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: JUnit/SpringBootTest-only fixture，共享PS支持旧接口新requiredBean，但绝不改变生产权限。
- Error and edge behavior: 不存在或越租户统一404；状态/旧版本/唯一冲突409并回滚；错误不泄漏token/SQL；品牌/上传行为保留。
- Standards impact: MC-TEST-001, MC-SCOPE-001, MC-VALID-001 — JUnit/MockMvc/现有fixture；nativegroups/真实结果断言，不建外部runtime；测试不是businessBean，沿用现有test injection风格。
- Literal rule enforcement: Rule 2, Rule 11 — 所选边界正负例/同模块隔离；不把H2或fake当PG/平台证明。
- Implementation pseudocode:

```java
@TestConfiguration("productTestSecurityConfiguration") creates explicitly named test-only Shoubing/Jianshen beans;
no live IdP/Redis/SERVICE token client; fake IDP verifier maps only fixed synthetictokens to trusted IdentityPrincipal, missingtoken401;
existing AuthorizationSnapshotCache in-memory SnapshotStore testdouble, Clock-controlled snapshot/permission+resource/system/tenant binding;
real Rbac3BearerAuthenticationFilter wraps verified context; fake AuthorizationService defaultdeny unless fixture explicitly grants;
shared PersistenceTestSupport imports configuration so all existing @SpringBootTest contexts still start; existing Mockito/TestBean overrides remain supported;
never define these fake beans in main sources or permissive productionprofile;
```

- Verification contribution: ProductSecurityContractTest,ProductAggregatePersistenceTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 测试已写；明确RED执行点需要的compile prerequisites完成后运行，不以fixture/环境失败凑RED。

#### File 13 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/support/PersistenceTestSupport.java`

- Purpose: 在真实依赖变更同Step补齐全套既有Boot测试隔离
- Symbols: @Import(ProductTestSecurityConfiguration.class)
- Repository evidence: 全部当前@SpringBootTest源码继承此helper，本轮检索无例外；当前文件存在，应局限所列symbol。
- Dependencies and consumers: @Import(ProductTestSecurityConfiguration.class) 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第13个文件；先固定observable RED，再补最小implementation；编译契约前置与实际RED执行点已由Test-first gate说明。
- Contract/signature changes: @Import(ProductTestSecurityConfiguration.class)；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: 只补充必要的测试runtime上下文，不改单元测试生产规则。
- Error and edge behavior: 不存在或越租户统一404；状态/旧版本/唯一冲突409并回滚；错误不泄漏token/SQL；品牌/上传行为保留。
- Standards impact: MC-TEST-001, MC-SCOPE-001, MC-VALID-001 — JUnit/MockMvc/现有fixture；nativegroups/真实结果断言，不建外部runtime；测试不是businessBean，沿用现有test injection风格。
- Literal rule enforcement: Rule 2, Rule 11 — 所选边界正负例/同模块隔离；不把H2或fake当PG/平台证明。
- Implementation pseudocode:

```java
retain isolatedDataSource/plainResolver/routingProfiles/routeFingerprint and H2shutdown;
@Import(ProductTestSecurityConfiguration.class) supplies only test securityruntime beans, no prodmapper/status change;
all existing @SpringBootTest classes currently extend this base (source inventory verified);
add test properties disable liveJianshenRedis/serviceToken but keep key shapes, use controlledfakes;
ensure old NativeHttp/Brand/File/Spec tests still own their mock overrides and never need externalplatform;
```

- Verification contribution: ProductSecurityContractTest,BrandControllerWebTest,FileControllerWebTest,SpecControllerWebTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 测试已写；明确RED执行点需要的compile prerequisites完成后运行，不以fixture/环境失败凑RED。

#### File 14 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/start/config/ProfileKeyParityTest.java`

- Purpose: 在配置完整后证明准确键结构和值类型
- Symbols: assertProductAndJianshenKeys
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件存在，应局限所列symbol。
- Dependencies and consumers: assertProductAndJianshenKeys 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第14个文件；先固定observable RED，再补最小implementation；编译契约前置与实际RED执行点已由Test-first gate说明。
- Contract/signature changes: assertProductAndJianshenKeys；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: 不得因为test值不同就删key，releasevalue由owner输入。
- Error and edge behavior: 不存在或越租户统一404；状态/旧版本/唯一冲突409并回滚；错误不泄漏token/SQL；品牌/上传行为保留。
- Standards impact: MC-TEST-001, MC-SCOPE-001, MC-VALID-001 — JUnit/MockMvc/现有fixture；nativegroups/真实结果断言，不建外部runtime；测试不是businessBean，沿用现有test injection风格。
- Literal rule enforcement: Rule 2, Rule 11 — 所选边界正负例/同模块隔离；不把H2或fake当PG/平台证明。
- Implementation pseudocode:

```java
compare recursive key sets for base/dev/test/prod product/Jianshen config, not unrelatedenvironmentvalues;
assert spu/sku SINGLE/master_data in allroutesincl importedYAML, bothregister-filter false;
assert cacheTTL1s,near0,jitter0,fetchTimeout1s, no accidentalprofile missing restore/rootIDs;
assert test no externalruntime enabled and explicitfakes replace requiredbeans;
```

- Verification contribution: ProfileKeyParityTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 测试已写；明确RED执行点需要的compile prerequisites完成后运行，不以fixture/环境失败凑RED。

#### File 15 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/start/config/NativeHttpSecurityConfiguration.java`

- Purpose: 只保护新商品路由，旧行为不扩大
- Symbols: productSecurityFilterChain
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件存在，应局限所列symbol。
- Dependencies and consumers: productSecurityFilterChain 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第15个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: productSecurityFilterChain；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: 保护pattern上线时即使HTTP还没创建也无匿名路由暴露。
- Error and edge behavior: 不存在或越租户统一404；状态/旧版本/唯一冲突409并回滚；错误不泄漏token/SQL；品牌/上传行为保留。
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-PATTERN-001, MC-SCOPE-001 — carrier用完整class Lombok；business namedBean+Slf4j+RequiredArgsConstructor/finalQualifier；converter继承Common；不可变record仅私有规则值；annotation/enum/interface无非法@Data。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11 — 语义suffix/具体组、class注解、BaseConverter、Bean命名及lombok传播、JDK/Commons、Jackson、Specification仅复杂lifecycle、java.time、exactLight；按实际类型执行而非机械加注解。
- Implementation pseudocode:

```java
@Bean("productSecurityFilterChain") @Order(higher than file and fallback)
securityMatcher("/api/v1/products","/api/v1/products/**"); require finalnamed IDP and RBAC filters;
add IDP before Anonymous, RBAC after IDP but before Anonymous; global servlet registration false;
newproduct .authenticated(), USER policy; do not insertRBAC into oldfile/brand chains; commonCORS addsPUT/DELETE only;
no ObjectProvider silentlymissing RBAC, no permitAll fallback on productconfigurationfailure;
```

- Verification contribution: ProductSecurityContractTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 16 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/filter/TenantContextFilter.java`

- Purpose: 防止调用方tenant/actor头覆盖商品trustedMDC
- Symbols: shouldNotFilter productpath only
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件存在，应局限所列symbol。
- Dependencies and consumers: shouldNotFilter productpath only 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第16个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: shouldNotFilter productpath only；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: 不是全局删除tenantFilter，旧默认tenant1只在旧边界保持。
- Error and edge behavior: 不存在或越租户统一404；状态/旧版本/唯一冲突409并回滚；错误不泄漏token/SQL；品牌/上传行为保留。
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-PATTERN-001, MC-SCOPE-001 — carrier用完整class Lombok；business namedBean+Slf4j+RequiredArgsConstructor/finalQualifier；converter继承Common；不可变record仅私有规则值；annotation/enum/interface无非法@Data。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11 — 语义suffix/具体组、class注解、BaseConverter、Bean命名及lombok传播、JDK/Commons、Jackson、Specification仅复杂lifecycle、java.time、exactLight；按实际类型执行而非机械加注解。
- Implementation pseudocode:

```java
skip untrusted headerMDC writing only exactproducts base and slash-child path (respectrequest contextpath);
for old brand/Spec/file preserve oldbehavior unchanged;
verifiedTenantContextFilter from Shoubing remains only producttenant/subject writer and wraps downstream RBAC chain;
finally cleanup and threadreuse tests prove no crossrequesttenant leak;
```

- Verification contribution: ProductSecurityContractTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

- Validation working directory: `/Users/mario/SelfProject/enjoyshop/enjoyshop-service/enjoyshop-service-goods`；GEN plan/apply的cwd另为`/Users/mario/SelfProject/enjoyshop`。
- Verification command: `./mvnw -o -B -ntp -Dtest=ProductAuthorizationComponentTest,ProductSecurityContractTest,ProfileKeyParityTest test`；`./mvnw -o -B -ntp -DskipTests compile`；exact checks见本StepManual Checks与§8，同步review该Step diff。
- Expected result: exit0，所有selector测试实际执行、0失败/错误；跳过/无test或classpath不解析不算PASS；十个Manage入口与新安全链失败关闭，4权限独立；旧路由策略保留。
- Failure returns to: 本Step负责该断言的File；DDL/hash/generator冲突回Step1，字段mapping回Step2/3，事务与state回Step4/5，身份/fixture回Step6，wire/OAS回Step7；GEN冲突或实际schema/API不符仍立即停止，不伪造tooling输出。
- Completion criteria: 本Step全部REQ有focused证据，Manual Check逐项有真实结果；最终SINGLE/tenant/版本/字段/旧边界与主Spec一致；无无关diff。
- Rollback: 未执行DDL前仅恢复本Step拥有文件且保留用户preexistingchanges；若用户已运行DDL则保留历史SQL/checksum，只允许后续forward-fix，不能自动DROP/repair。
- Commit paths: enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/application/goods/ProductAuthorizationComponentTest.java; enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/start/config/ProductSecurityContractTest.java; enjoyshop-service/enjoyshop-service-goods/pom.xml; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/ProductAuthorizationComponent.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/ProductManage.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/ProductManageImpl.java; enjoyshop-service/enjoyshop-service-goods/src/main/resources/application.yml; enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-dev.yml; enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-test.yml; enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-prod.yml; enjoyshop-service/enjoyshop-service-goods/src/main/resources/egon-mybatis-plus-sharding.yml; enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/support/ProductTestSecurityConfiguration.java; enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/support/PersistenceTestSupport.java; enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/start/config/ProfileKeyParityTest.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/start/config/NativeHttpSecurityConfiguration.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/filter/TenantContextFilter.java
- Commit: Deferred — 用户要求完整logical task最多1次commit；以上路径仅本Step归属，不创建阶段commit。

### Step 7 — 交付十个商品 REST 与真实生成 OpenAPI 契约

- Requirements: REQ-002, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010, REQ-011, REQ-012, REQ-013, REQ-014, REQ-015, REQ-019, REQ-020
- Dependencies: Step 6 GREEN checkpoint（按用户约束无逐Step commit）
- Baseline state: 前一Step的明确End state与focused tests已验证；已存在路径只改本Step列出的符号
- Observable outcome: 所有已批准商品API/wire/error/OAS与安全链一致
- End state: 完整调用链可验收，无前端工程；仅运行isolatedtests，live由用户
- Test-first gate: Required — Controller缺失时已授权请求404、OAS无operation先RED，随后真实Controller/advice/annotations GREEN
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-JSON-001, MC-TIME-001, MC-CONFIG-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001；每项适用性按文件以下具体证据检查，不以通用规则代替实际校验。
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；普通对象/业务Bean/约束/SQL/配置分别按文件职责生效，未涉及电话/Event不新增依赖。
- Ordered files:

#### File 1 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/goods/ProductControllerWebTest.java`

- Purpose: ProductController存在前先证明路由/响应/错误缺失
- Symbols: API201–210 contract cases
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: API201–210 contract cases 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第1个文件；先固定observable RED，再补最小implementation；编译契约前置与实际RED执行点已由Test-first gate说明。
- Contract/signature changes: API201–210 contract cases；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: 真实10个接口，沿用common信封；状态完全来自已验证Domain，不controller写SQL。
- Error and edge behavior: 不存在或越租户统一404；状态/旧版本/唯一冲突409并回滚；错误不泄漏token/SQL；品牌/上传行为保留。
- Standards impact: MC-TEST-001, MC-SCOPE-001, MC-VALID-001 — JUnit/MockMvc/现有fixture；nativegroups/真实结果断言，不建外部runtime；测试不是businessBean，沿用现有test injection风格。
- Literal rule enforcement: Rule 2, Rule 11 — 所选边界正负例/同模块隔离；不把H2或fake当PG/平台证明。
- Implementation pseudocode:

```java
with Product-specific fakes and validtoken+permissions, request ten Spec methods and paths;
assert create201 Location+fullaggregate, query/list200 boundedrecords, update version3->4, state200smallprojection, delete/purge200true;
assert malformedJSON/unknownFields/type/paths400, contextfilter401/403/503doublefield, Managegate403/503envelope, tenant404, stale409;
CurrentController missing=>404 for validcalls RED (auth alreadyworks Step6), no test fallback fake route in main;
verify idstrings, nullabledeletedAt, integercents, SKUorder, no no-opPOleak;
```

- Verification contribution: ProductControllerWebTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 测试已写；明确RED执行点需要的compile prerequisites完成后运行，不以fixture/环境失败凑RED。

#### File 2 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/goods/ProductOpenApiContractTest.java`

- Purpose: 文档漂移Gate先RED再注解修正
- Symbols: paths,operationIds,schema/security/erroroneOf
- Repository evidence: 现有BrandOpenApiContractTest通过MockMvc读取真实OAS不启动browser/server；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: paths,operationIds,schema/security/erroroneOf 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第2个文件；先固定observable RED，再补最小implementation；编译契约前置与实际RED执行点已由Test-first gate说明。
- Contract/signature changes: paths,operationIds,schema/security/erroroneOf；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: 严格按商品Spec的字段/默认/null/状态/版本定义映射；业务ID十进制字符串，持久BIGINT；审计Instant/UTC deletedAt微秒。
- Error and edge behavior: 不存在或越租户统一404；状态/旧版本/唯一冲突409并回滚；错误不泄漏token/SQL；品牌/上传行为保留。
- Standards impact: MC-TEST-001, MC-SCOPE-001, MC-VALID-001 — JUnit/MockMvc/现有fixture；nativegroups/真实结果断言，不建外部runtime；测试不是businessBean，沿用现有test injection风格。
- Literal rule enforcement: Rule 2, Rule 11 — 所选边界正负例/同模块隔离；不把H2或fake当PG/平台证明。
- Implementation pseudocode:

```java
fetch /v3/api-docs using MockMvc in isolatedH2context, parse Jackson JsonNode;
assert exactly new10 method+paths and explicit unique operationIds listProducts/getProduct/createProduct/updateProduct/approveProduct/withdrawProduct/publishProduct/deleteProduct/restoreProduct/purgeProduct;
assert GET/DELETE no body, IDstring constraints, page1/size≤100, enum0/1 vsSKU1/2, Location201;
verify each reachableHTTPerror/securitybearerAuth and filtererror∨ResultRecord oneOf403/503, concretegenericwrapperdata/records/page;
retain oldBrand/File/Spec operationIds & exposure; no network orremotepublication;
```

- Verification contribution: ProductOpenApiContractTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 测试已写；明确RED执行点需要的compile prerequisites完成后运行，不以fixture/环境失败凑RED。

#### File 3 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/enums/GoodsErrorStatus.java`

- Purpose: 只追加商品稳定业务码
- Symbols: PRODUCT610201–208/299
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件存在，应局限所列symbol。
- Dependencies and consumers: PRODUCT610201–208/299 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第3个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: PRODUCT610201–208/299；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: enumErrorStatus而非存储ordinal，对旧业务码零重编号。
- Error and edge behavior: 不存在或越租户统一404；状态/旧版本/唯一冲突409并回滚；错误不泄漏token/SQL；品牌/上传行为保留。
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-PATTERN-001, MC-SCOPE-001 — carrier用完整class Lombok；business namedBean+Slf4j+RequiredArgsConstructor/finalQualifier；converter继承Common；不可变record仅私有规则值；annotation/enum/interface无非法@Data。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11 — 语义suffix/具体组、class注解、BaseConverter、Bean命名及lombok传播、JDK/Commons、Jackson、Specification仅复杂lifecycle、java.time、exactLight；按实际类型执行而非机械加注解。
- Implementation pseudocode:

```java
append PRODUCT_REQUEST_INVALID610201, NOT_FOUND610202, VERSION_CONFLICT610203, STATE_CONFLICT610204,
 BUSINESS_KEY_DUPLICATED610205, FORBIDDEN610206, ID_UNAVAILABLE610207, AUTHORIZATION_UNAVAILABLE610208, INTERNAL_ERROR610299;
preserve all BRAND/FILE/CATALOG existingnumbers/messages; getStatus remains stable declaredname, noordinal;
Common401000 reused for missingUSER, no duplicate inventedAuthcode;
```

- Verification contribution: ProductControllerWebTest,GoodsErrorStatusTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 4 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/handler/BaseExceptionHandler.java`

- Purpose: 精确映射新错误且不误变成品牌错误
- Symbols: product-only error mapper/path selector
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件存在，应局限所列symbol。
- Dependencies and consumers: product-only error mapper/path selector 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第4个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: product-only error mapper/path selector；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: 新productbranch限定，共享advice旧行为回归，错误状态与OAS一致。
- Error and edge behavior: 不存在或越租户统一404；状态/旧版本/唯一冲突409并回滚；错误不泄漏token/SQL；品牌/上传行为保留。
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-PATTERN-001, MC-SCOPE-001 — carrier用完整class Lombok；business namedBean+Slf4j+RequiredArgsConstructor/finalQualifier；converter继承Common；不可变record仅私有规则值；annotation/enum/interface无非法@Data。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11 — 语义suffix/具体组、class注解、BaseConverter、Bean命名及lombok传播、JDK/Commons、Jackson、Specification仅复杂lifecycle、java.time、exactLight；按实际类型执行而非机械加注解。
- Implementation pseudocode:

```java
identify productbase/slashchild respectingcontextpath; productCommon codes map400/404/409/403/503/500;401000=>401;
newproduct fieldviolations preserve skuList[index].field path; reuse FieldErrorsVO, no rejectedvalue/token/SQL;
translate unique constraint identities toproduct610205 BEFORE generic brandDuplicateKey rule; no catchingException then falseSuccess;
handle AccessDenied/AuthorizationDenied403 and RuntimeUnavailable503 withinproduct scope, otherpathsnormallypreserved;
Shoubing/Jianshen filtererror alreadywritten: do not doublewrap/overwrite; ErrorEnvelope fields exact commoncore;
```

- Verification contribution: ProductControllerWebTest,BaseExceptionHandlerTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 5 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/goods/ProductController.java`

- Purpose: 最后暴露已受保护的完整REST商品能力
- Symbols: all API201–210 operationId methods
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: all API201–210 operationId methods 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第5个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: all API201–210 operationId methods；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: controller负责HTTP/validation/OAS；不实现业务state或别层对象手工copy。
- Error and edge behavior: 不存在或越租户统一404；状态/旧版本/唯一冲突409并回滚；错误不泄漏token/SQL；品牌/上传行为保留。
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-PATTERN-001, MC-SCOPE-001 — carrier用完整class Lombok；business namedBean+Slf4j+RequiredArgsConstructor/finalQualifier；converter继承Common；不可变record仅私有规则值；annotation/enum/interface无非法@Data。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11 — 语义suffix/具体组、class注解、BaseConverter、Bean命名及lombok传播、JDK/Commons、Jackson、Specification仅复杂lifecycle、java.time、exactLight；按实际类型执行而非机械加注解。
- Implementation pseudocode:

```java
@RestController("productController") @RequestMapping("/api/v1/products") @Validated @Tag("product") @Slf4j @RequiredArgsConstructor
final @Qualifier("productManage") ProductManage; noRepository/DAO injection;
GETcollection ProductPageQuery; GET/{spuId} optionaldeleted; POSTsharedSave/Create ->201 Location; PUT/{spuId}Save/Update ->200;
PUTapproval/withdrawal/publication/restoration validate HttpStateInput then fillpathID and delegate correspondingmethod; DELETEbase/purge queryversion typedCommand;
strict path canonical decimal/Long-range, Querygroups, ContentTypeJSON, no-store headers, error/status annotations exact Spec;
@Operation explicit unique IDs, per-operation @SecurityRequirement bearerAuth and @ApiResponses complete reachablestatuses;
CommonResultRecord/PageResultRecord envelopes, no domainStateBO or doublewrapper;
```

- Verification contribution: ProductControllerWebTest,ProductOpenApiContractTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 6 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/start/config/OpenApiConfig.java`

- Purpose: 复用已有OpenAPI Bean只补真实机制
- Symbols: bearerAuth securityScheme
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件存在，应局限所列symbol。
- Dependencies and consumers: bearerAuth securityScheme 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第6个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: bearerAuth securityScheme；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: 4permissioncode不是OAuthscopes，不把文档当授权机制。
- Error and edge behavior: 不存在或越租户统一404；状态/旧版本/唯一冲突409并回滚；错误不泄漏token/SQL；品牌/上传行为保留。
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-JSON-001, MC-TIME-001, MC-PATTERN-001, MC-SCOPE-001 — carrier用完整class Lombok；business namedBean+Slf4j+RequiredArgsConstructor/finalQualifier；converter继承Common；不可变record仅私有规则值；annotation/enum/interface无非法@Data。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11 — 语义suffix/具体组、class注解、BaseConverter、Bean命名及lombok传播、JDK/Commons、Jackson、Specification仅复杂lifecycle、java.time、exactLight；按实际类型执行而非机械加注解。
- Implementation pseudocode:

```java
retain existing studentManagementOpenApi Bean/info/exposure and oldpublicoperations;
components.securitySchemes["bearerAuth"] type HTTP scheme bearer formatJWT;
Product operations reference scheme explicitly, not globalsecurity rule on allcontrollers;
model filterdouble-field errors vs ResultRecord oneOf with existing io.swagger.v3 annotations; noSpringfox/extraUI/plugin/customizer unless provenfailure returnsSpec;
```

- Verification contribution: ProductOpenApiContractTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

- Validation working directory: `/Users/mario/SelfProject/enjoyshop/enjoyshop-service/enjoyshop-service-goods`；GEN plan/apply的cwd另为`/Users/mario/SelfProject/enjoyshop`。
- Verification command: `./mvnw -o -B -ntp -Dtest=ProductControllerWebTest,ProductOpenApiContractTest,BaseExceptionHandlerTest,GoodsErrorStatusTest test`；`./mvnw -o -B -ntp -DskipTests compile`；exact checks见本StepManual Checks与§8，同步review该Step diff。
- Expected result: exit0，所有selector测试实际执行、0失败/错误；跳过/无test或classpath不解析不算PASS；所有已批准商品API/wire/error/OAS与安全链一致。
- Failure returns to: 本Step负责该断言的File；DDL/hash/generator冲突回Step1，字段mapping回Step2/3，事务与state回Step4/5，身份/fixture回Step6，wire/OAS回Step7；GEN冲突或实际schema/API不符仍立即停止，不伪造tooling输出。
- Completion criteria: 本Step全部REQ有focused证据，Manual Check逐项有真实结果；最终SINGLE/tenant/版本/字段/旧边界与主Spec一致；无无关diff。
- Rollback: 未执行DDL前仅恢复本Step拥有文件且保留用户preexistingchanges；若用户已运行DDL则保留历史SQL/checksum，只允许后续forward-fix，不能自动DROP/repair。
- Commit paths: enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/goods/ProductControllerWebTest.java; enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/goods/ProductOpenApiContractTest.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/enums/GoodsErrorStatus.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/handler/BaseExceptionHandler.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/goods/ProductController.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/start/config/OpenApiConfig.java
- Commit: Deferred — 用户要求完整logical task最多1次commit；以上路径仅本Step归属，不创建阶段commit。

### Step 8 — 完成边界回归、发布说明与单任务交付

- Requirements: REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010, REQ-011, REQ-012, REQ-013, REQ-014, REQ-015, REQ-016, REQ-017, REQ-018, REQ-019, REQ-020, REQ-021, REQ-022, REQ-101, REQ-102, REQ-103, REQ-104, REQ-105, REQ-106, REQ-107, REQ-108
- Dependencies: Step 7 GREEN checkpoint（按用户约束无逐Step commit）
- Baseline state: 前一Step的明确End state与focused tests已验证；已存在路径只改本Step列出的符号
- Observable outcome: 全module验证与旧品牌/上传/目录边界保留，文档和操作前置明确
- End state: 所有设计REQ覆盖；仅一个最终path-limited commit提案；runtime验收仍独立
- Test-first gate: Required — 新增Conformance静态规则若缺DI/guard/字段隔离应RED；旧已有测试是Regression非新RED
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-JSON-001, MC-TIME-001, MC-CONFIG-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001；每项适用性按文件以下具体证据检查，不以通用规则代替实际校验。
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；普通对象/业务Bean/约束/SQL/配置分别按文件职责生效，未涉及电话/Event不新增依赖。
- Ordered files:

#### File 1 — `MODIFY enjoyshop-service/enjoyshop-service-goods/README.zh-CN.md`

- Purpose: 记录准确使用与发布回滚边界，双语保持语义一致
- Symbols: product API/security/managed-DDL/manualverification
- Repository evidence: 现有M/README.md和README.zh-CN.md包含Light运行/验证约定，此Step仅加真实商品能力；当前文件存在，应局限所列symbol。
- Dependencies and consumers: product API/security/managed-DDL/manualverification 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第1个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: product API/security/managed-DDL/manualverification；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: 严格按商品Spec的字段/默认/null/状态/版本定义映射；业务ID十进制字符串，持久BIGINT；审计Instant/UTC deletedAt微秒。
- Error and edge behavior: 不存在或越租户统一404；状态/旧版本/唯一冲突409并回滚；错误不泄漏token/SQL；品牌/上传行为保留。
- Standards impact: MC-ARCH-001, MC-CONFIG-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001 — 精确manifest/typedkeys/UTC/SQL范围、同profile结构；无Java对象注解装饰配置。
- Literal rule enforcement: Rule 6, Rule 7, Rule 10, Rule 11 — Jackson或SQL实际类型、四profile同键、UTC时间、原Light和MP DDL约束，文档N/A项不引代码。
- Implementation pseudocode:

```text
document10paths, stringIDs/cents/groups/version/CQE and P/E restore semantics;
record platform permissions/resource/system registration and trustedUSER chain, config keys/cache1s boundedstaleness/503, no plaintextsecrets;
explain nextDDL+manifest immutableprefix, generationinitialownership/customexception, LOCALsamegroup, per-targetresume and forwardfix;
manualPG/IdP/Redis acceptance requires explicituserauthorization; never startupapplication as completion;
rollbackoldbinary keepsnewtables/data; purge irreversible and never deletesstoredimages/catalog;
```

- Verification contribution: documentation review + primarySpec traceability；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 2 — `MODIFY enjoyshop-service/enjoyshop-service-goods/README.md`

- Purpose: 记录准确使用与发布回滚边界，双语保持语义一致
- Symbols: product API/security/managed-DDL/manualverification
- Repository evidence: 现有M/README.md和README.zh-CN.md包含Light运行/验证约定，此Step仅加真实商品能力；当前文件存在，应局限所列symbol。
- Dependencies and consumers: product API/security/managed-DDL/manualverification 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第2个文件；其直接类型/配置/DDL前置已在上游关卡或本Step前序文件建立，后续文件才接线/暴露。
- Contract/signature changes: product API/security/managed-DDL/manualverification；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: 严格按商品Spec的字段/默认/null/状态/版本定义映射；业务ID十进制字符串，持久BIGINT；审计Instant/UTC deletedAt微秒。
- Error and edge behavior: 不存在或越租户统一404；状态/旧版本/唯一冲突409并回滚；错误不泄漏token/SQL；品牌/上传行为保留。
- Standards impact: MC-ARCH-001, MC-CONFIG-001, MC-JSON-001, MC-TIME-001, MC-SCOPE-001 — 精确manifest/typedkeys/UTC/SQL范围、同profile结构；无Java对象注解装饰配置。
- Literal rule enforcement: Rule 6, Rule 7, Rule 10, Rule 11 — Jackson或SQL实际类型、四profile同键、UTC时间、原Light和MP DDL约束，文档N/A项不引代码。
- Implementation pseudocode:

```text
document10paths, stringIDs/cents/groups/version/CQE and P/E restore semantics;
record platform permissions/resource/system registration and trustedUSER chain, config keys/cache1s boundedstaleness/503, no plaintextsecrets;
explain nextDDL+manifest immutableprefix, generationinitialownership/customexception, LOCALsamegroup, per-targetresume and forwardfix;
manualPG/IdP/Redis acceptance requires explicituserauthorization; never startupapplication as completion;
rollbackoldbinary keepsnewtables/data; purge irreversible and never deletesstoredimages/catalog;
```

- Verification contribution: documentation review + primarySpec traceability；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 仅本文件职责增量就绪；后序wiring/验证尚未完成，不声称整个Step或Spec已交付。

#### File 3 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/architecture/LightPersistenceArchitectureTest.java`

- Purpose: 关闭结构规则且不做样例清理
- Symbols: final inventories/owned package checks
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件存在，应局限所列symbol。
- Dependencies and consumers: final inventories/owned package checks 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第3个文件；先固定observable RED，再补最小implementation；编译契约前置与实际RED执行点已由Test-first gate说明。
- Contract/signature changes: final inventories/owned package checks；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: 样例保留来自primarySpec §3/8，不以技能模板要求扩到无关删除。
- Error and edge behavior: 不存在或越租户统一404；状态/旧版本/唯一冲突409并回滚；错误不泄漏token/SQL；品牌/上传行为保留。
- Standards impact: MC-TEST-001, MC-SCOPE-001, MC-VALID-001 — JUnit/MockMvc/现有fixture；nativegroups/真实结果断言，不建外部runtime；测试不是businessBean，沿用现有test injection风格。
- Literal rule enforcement: Rule 2, Rule 11 — 所选边界正负例/同模块隔离；不把H2或fake当PG/平台证明。
- Implementation pseudocode:

```java
assert 13 PO/DAO/Repository and9DomainServiceImpl, named exactadditioninventory;
assert ProductDomainServiceImpl has noDAO/getBaseMapper/AR, domain has noSpringSecurity/http/persistence dependencies;
newconverters implementCommonBaseConverter/Forward, noDate/Fastjson/Hutool/manualBeanCopy;
preserve teaching/user examples, oldGLOBALadvice deletions alreadydone do notrepeat;
```

- Verification contribution: LightPersistenceArchitectureTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 测试已写；明确RED执行点需要的compile prerequisites完成后运行，不以fixture/环境失败凑RED。

#### File 4 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/architecture/ProductContractConformanceTest.java`

- Purpose: 把无现成单命令的跨文件规则纳入已有JUnit机制
- Symbols: typed boundaries/bean qualifiers/static forbiddenimports
- Repository evidence: 商品Spec §8/§10/§11；现有BrandController/BrandManage/Repository/PO/Mapper XML分层与相邻命名；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: typed boundaries/bean qualifiers/static forbiddenimports 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第4个文件；先固定observable RED，再补最小implementation；编译契约前置与实际RED执行点已由Test-first gate说明。
- Contract/signature changes: typed boundaries/bean qualifiers/static forbiddenimports；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: 增加repo源码级contracttest不镜像业务实现，保护身份/层/字段暴露真正风险。
- Error and edge behavior: 不存在或越租户统一404；状态/旧版本/唯一冲突409并回滚；错误不泄漏token/SQL；品牌/上传行为保留。
- Standards impact: MC-TEST-001, MC-SCOPE-001, MC-VALID-001 — JUnit/MockMvc/现有fixture；nativegroups/真实结果断言，不建外部runtime；测试不是businessBean，沿用现有test injection风格。
- Literal rule enforcement: Rule 2, Rule 11 — 所选边界正负例/同模块隔离；不把H2或fake当PG/平台证明。
- Implementation pseudocode:

```java
read exactdeclared Product sourceinventory and verify suffixes/classLombok/POEgonModel inheritance;
verify allManage methods have permission gate and validatedgroups, namedBeans/RequiredArgsConstructor/finalQualifier with existinglombok.config;
assert noForbiddenutilities/Date/Fastjson/JSONwholeObjectMapping, readonlyfields absentfromCommands, noadditionalmodules;
verifyproduct-onlySecuritymatching and no newXMLglobalunscopeddelete; retainexistingarchitectureplugin finalverify;
this durableJUnit test owns gate, no temporary scripts/work harness needed;
```

- Verification contribution: ProductContractConformanceTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 测试已写；明确RED执行点需要的compile prerequisites完成后运行，不以fixture/环境失败凑RED。

- Validation working directory: `/Users/mario/SelfProject/enjoyshop/enjoyshop-service/enjoyshop-service-goods`；GEN plan/apply的cwd另为`/Users/mario/SelfProject/enjoyshop`。
- Verification command: `./mvnw -o -B -ntp -Dtest=ProductContractConformanceTest,LightPersistenceArchitectureTest,BrandControllerWebTest,BrandOpenApiContractTest,FileControllerWebTest,FileOpenApiContractTest,SpecControllerWebTest,SpecOpenApiContractTest,ProfileKeyParityTest test`；`./mvnw -o -B -ntp -DskipTests compile`；exact checks见本StepManual Checks与§8，同步review该Step diff。
- Expected result: exit0，所有selector测试实际执行、0失败/错误；跳过/无test或classpath不解析不算PASS；全module验证与旧品牌/上传/目录边界保留，文档和操作前置明确。
- Failure returns to: 本Step负责该断言的File；DDL/hash/generator冲突回Step1，字段mapping回Step2/3，事务与state回Step4/5，身份/fixture回Step6，wire/OAS回Step7；GEN冲突或实际schema/API不符仍立即停止，不伪造tooling输出。
- Completion criteria: 本Step全部REQ有focused证据，Manual Check逐项有真实结果；最终SINGLE/tenant/版本/字段/旧边界与主Spec一致；无无关diff。
- Rollback: 未执行DDL前仅恢复本Step拥有文件且保留用户preexistingchanges；若用户已运行DDL则保留历史SQL/checksum，只允许后续forward-fix，不能自动DROP/repair。
- Commit paths: enjoyshop-service/enjoyshop-service-goods/README.zh-CN.md; enjoyshop-service/enjoyshop-service-goods/README.md; enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/architecture/LightPersistenceArchitectureTest.java; enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/architecture/ProductContractConformanceTest.java
- Commit: feat(goods): implement product management lifecycle — 仅全部8关卡与验收门满足后，最终stage §5完整源码union，最多一次，不含临时classpath/生成候选/密钥/未授权用户文件。

## 8. Test, Validation, and Quality Gates

| Gate/order | Working directory | Command or method | Scope | Expected result | Failure returns to | Requirements/runtime boundary |
| --- | --- | --- | --- | --- | --- | --- |
| Step1 RED/GREEN | enjoyshop-service/enjoyshop-service-goods | `./mvnw -o -B -ntp -Dtest=ProductDdlContractTest,LightPersistenceArchitectureTest test` | 本Step精确selector | RED为声明缺行为；实现后GREEN exit0无skip | Step1 owningFile | REQ-001,REQ-003,REQ-004,REQ-015；isolatedJava/H2/MockMvc |
| Step2 RED/GREEN | enjoyshop-service/enjoyshop-service-goods | `./mvnw -o -B -ntp -Dtest=ProductCompositionValidationTest test` | 本Step精确selector | RED为声明缺行为；实现后GREEN exit0无skip | Step2 owningFile | REQ-001,REQ-002,REQ-003,REQ-005；isolatedJava/H2/MockMvc |
| Step3 RED/GREEN | enjoyshop-service/enjoyshop-service-goods | `./mvnw -o -B -ntp -Dtest=ProductConverterTest,ProductQuerySqlShapeTest test` | 本Step精确selector | RED为声明缺行为；实现后GREEN exit0无skip | Step3 owningFile | REQ-003,REQ-004,REQ-005,REQ-006；isolatedJava/H2/MockMvc |
| Step4 RED/GREEN | enjoyshop-service/enjoyshop-service-goods | `./mvnw -o -B -ntp -Dtest=ProductUpdateTest,ProductAggregatePersistenceTest,ProductQuerySqlShapeTest,LightPersistenceArchitectureTest test` | 本Step精确selector | RED为声明缺行为；实现后GREEN exit0无skip | Step4 owningFile | REQ-001,REQ-002,REQ-003,REQ-004；isolatedJava/H2/MockMvc |
| Step5 RED/GREEN | enjoyshop-service/enjoyshop-service-goods | `./mvnw -o -B -ntp -Dtest=ProductLifecycleComponentTest,ProductAggregatePersistenceTest,ProductUniquenessTest test` | 本Step精确selector | RED为声明缺行为；实现后GREEN exit0无skip | Step5 owningFile | REQ-007,REQ-008,REQ-009,REQ-010；isolatedJava/H2/MockMvc |
| Step6 RED/GREEN | enjoyshop-service/enjoyshop-service-goods | `./mvnw -o -B -ntp -Dtest=ProductAuthorizationComponentTest,ProductSecurityContractTest,ProfileKeyParityTest test` | 本Step精确selector | RED为声明缺行为；实现后GREEN exit0无skip | Step6 owningFile | REQ-014,REQ-015,REQ-018,REQ-021；isolatedJava/H2/MockMvc |
| Step7 RED/GREEN | enjoyshop-service/enjoyshop-service-goods | `./mvnw -o -B -ntp -Dtest=ProductControllerWebTest,ProductOpenApiContractTest,BaseExceptionHandlerTest,GoodsErrorStatusTest test` | 本Step精确selector | RED为声明缺行为；实现后GREEN exit0无skip | Step7 owningFile | REQ-002,REQ-005,REQ-006,REQ-007；isolatedJava/H2/MockMvc |
| Step8 RED/GREEN | enjoyshop-service/enjoyshop-service-goods | `./mvnw -o -B -ntp -Dtest=ProductContractConformanceTest,LightPersistenceArchitectureTest,BrandControllerWebTest,BrandOpenApiContractTest,FileControllerWebTest,FileOpenApiContractTest,SpecControllerWebTest,SpecOpenApiContractTest,ProfileKeyParityTest test` | 本Step精确selector | RED为声明缺行为；实现后GREEN exit0无skip | Step8 owningFile | REQ-001,REQ-002,REQ-003,REQ-004；isolatedJava/H2/MockMvc |
| Generator | enjoyshop root | §6精确templates/plan/apply/check命令 | 仅选择14输出 | format2/0冲突/实际journal，不连DB | Step1/已关闭DEC-208 | generated static |
| Module final | enjoyshop-service/enjoyshop-service-goods | `./mvnw -o -B -ntp verify` | 全测试+bytecode规则 | 所有tests执行，architecture报告0violations | 对应Step | 全需求，非livePG/Redis/platform |
| Approved dependency graph | enjoyshop-service/enjoyshop-service-goods | `./mvnw -o -B -ntp -Dincludes=top.egon:egon-cola-tianquan-jianshen-starter,top.egon:egon-cola-tianquan-shoubing-starter,org.redisson dependency:tree` | exact5.4.1收敛/旧Redisson排除 | 0不批准的版本/依赖 | Step6 | 无平台连接 |
| Diff/immutable scope | enjoyshop root | `git diff --check` + ProductDdlContractTest/ProductContractConformanceTest | actualtracked和新增文件共同审查 | 无空白问题/旧SQLhash变化/无无关文件 | ownershipStep | source/static |
| Manual PostgreSQL | 已有授权数据库 | §9 pre/post查询；真实unique/rowlock/EXPLAIN | 只在用户显式授权后执行 | 0duplicateactive/orphans/非法状态，rollback/恢复正确 | Step1/3/4/5 | 未授权不连接，不用H2冒充 |
| Manual security | 已有授权平台/Redis | syntheticread/write/audit/purge角色分离、tenant伪造、过期/撤销/cachefailure测试 | §15.1绑定和缓存边界 | 401/403/404/503与allow仅真实登记角色；无即时撤销假保证 | Step6/7 | 用户-controlled，不自动启动 |

本轮真实已执行：资源preflight、当前源路径/数量检索、本地Jianshen javap、generator templates；未执行上述商品tests/build/DDL或调用任何platform。Maven离线classpath构建失败与read-only恢复已在§6记录，不能报告Maven验证通过。

## 9. Migration, Compatibility, Rollout, and Rollback

Step1只写新V20261009_001与sha256manifest；不是schema已更新。MASTER_DATA建spu后sku/索引/FK，SHARD空分支仍分别记history，runner每target SQL+history原子，跨target不承诺全局事务。checksum/routeFingerprint/未管理schema冲突停止；commit未知先核history，不blind-replay/repair。category_brand仅新增writer无DDL；旧三versions、Flywayarchive所有B/V不改。无历史商品backfill或seed默认权限。

部署先提供平台system享shop-goods、resource enjoyshop.product及四permission登记/角色分配、Shoubing服务client凭据、JianshenHTTPS/Redis输入；具体systemCode准确值为`enjoyshop-goods`，不使用中文显示名作配置。后由用户执行DDL并核全部target完成，再发布通过验证的新应用。商品requiredbeans缺失启动失败；旧品牌/上传/目录策略由回归证明。尚未accepted的Spec/Plan不意味着授权实现/部署。

Post checks：active sn/条码重复0行；sku join spu无孤儿0行；deletedparent+activechild0行；deleted_at/kind非法组合0行；还原冲突后父与所有P仍原样；永久删除所有P/E；EXPLAIN listactive/deleted和parent查询索引可用。主副本延迟/分片均匀/平台撤销只有实际运行证明，当前SINGLE新表不增加二级shard。未提供capacity不虚构SLA。

回滚代码保留新增tables/data与已经执行的immutableversion；不得自动DROP或修checksum。“永久删除”属于业务不可恢复命令，受purgepermission与客户端确认保护；部署rollback不能复活已删数据，也不删文件对象或目录关系。必要数据修复另加nextversion，不能修改本已应用SQL。

## 10. Requirement-to-Step Traceability Matrix

| Requirement | Effective Spec section | Steps | Files | Tests/gates | Completion evidence |
| --- | --- | --- | --- | --- | --- |
| REQ-001（主文1–15；前序重复ID按文档作用域保护） | 主Spec§4或品牌基线§4/6/10/11/16 | Step1,Step2,Step4,Step8 | §5精确归属，Step8保留旧baseline | 对应Step focused和旧Brand regression | §3可观察标准，验证实际执行日志，不是本轮代码证明 |
| REQ-002（主文1–15；前序重复ID按文档作用域保护） | 主Spec§4或品牌基线§4/6/10/11/16 | Step2,Step4,Step7,Step8 | §5精确归属，Step8保留旧baseline | 对应Step focused和旧Brand regression | §3可观察标准，验证实际执行日志，不是本轮代码证明 |
| REQ-003（主文1–15；前序重复ID按文档作用域保护） | 主Spec§4或品牌基线§4/6/10/11/16 | Step1,Step2,Step3,Step4,Step8 | §5精确归属，Step8保留旧baseline | 对应Step focused和旧Brand regression | §3可观察标准，验证实际执行日志，不是本轮代码证明 |
| REQ-004（主文1–15；前序重复ID按文档作用域保护） | 主Spec§4或品牌基线§4/6/10/11/16 | Step1,Step3,Step4,Step8 | §5精确归属，Step8保留旧baseline | 对应Step focused和旧Brand regression | §3可观察标准，验证实际执行日志，不是本轮代码证明 |
| REQ-005（主文1–15；前序重复ID按文档作用域保护） | 主Spec§4或品牌基线§4/6/10/11/16 | Step2,Step3,Step4,Step7,Step8 | §5精确归属，Step8保留旧baseline | 对应Step focused和旧Brand regression | §3可观察标准，验证实际执行日志，不是本轮代码证明 |
| REQ-006（主文1–15；前序重复ID按文档作用域保护） | 主Spec§4或品牌基线§4/6/10/11/16 | Step2,Step3,Step4,Step7,Step8 | §5精确归属，Step8保留旧baseline | 对应Step focused和旧Brand regression | §3可观察标准，验证实际执行日志，不是本轮代码证明 |
| REQ-007（主文1–15；前序重复ID按文档作用域保护） | 主Spec§4或品牌基线§4/6/10/11/16 | Step5,Step7,Step8 | §5精确归属，Step8保留旧baseline | 对应Step focused和旧Brand regression | §3可观察标准，验证实际执行日志，不是本轮代码证明 |
| REQ-008（主文1–15；前序重复ID按文档作用域保护） | 主Spec§4或品牌基线§4/6/10/11/16 | Step5,Step7,Step8 | §5精确归属，Step8保留旧baseline | 对应Step focused和旧Brand regression | §3可观察标准，验证实际执行日志，不是本轮代码证明 |
| REQ-009（主文1–15；前序重复ID按文档作用域保护） | 主Spec§4或品牌基线§4/6/10/11/16 | Step5,Step7,Step8 | §5精确归属，Step8保留旧baseline | 对应Step focused和旧Brand regression | §3可观察标准，验证实际执行日志，不是本轮代码证明 |
| REQ-010（主文1–15；前序重复ID按文档作用域保护） | 主Spec§4或品牌基线§4/6/10/11/16 | Step3,Step5,Step7,Step8 | §5精确归属，Step8保留旧baseline | 对应Step focused和旧Brand regression | §3可观察标准，验证实际执行日志，不是本轮代码证明 |
| REQ-011（主文1–15；前序重复ID按文档作用域保护） | 主Spec§4或品牌基线§4/6/10/11/16 | Step3,Step5,Step7,Step8 | §5精确归属，Step8保留旧baseline | 对应Step focused和旧Brand regression | §3可观察标准，验证实际执行日志，不是本轮代码证明 |
| REQ-012（主文1–15；前序重复ID按文档作用域保护） | 主Spec§4或品牌基线§4/6/10/11/16 | Step3,Step5,Step7,Step8 | §5精确归属，Step8保留旧baseline | 对应Step focused和旧Brand regression | §3可观察标准，验证实际执行日志，不是本轮代码证明 |
| REQ-013（主文1–15；前序重复ID按文档作用域保护） | 主Spec§4或品牌基线§4/6/10/11/16 | Step2,Step3,Step4,Step7,Step8 | §5精确归属，Step8保留旧baseline | 对应Step focused和旧Brand regression | §3可观察标准，验证实际执行日志，不是本轮代码证明 |
| REQ-014（主文1–15；前序重复ID按文档作用域保护） | 主Spec§4或品牌基线§4/6/10/11/16 | Step6,Step7,Step8 | §5精确归属，Step8保留旧baseline | 对应Step focused和旧Brand regression | §3可观察标准，验证实际执行日志，不是本轮代码证明 |
| REQ-015（主文1–15；前序重复ID按文档作用域保护） | 主Spec§4或品牌基线§4/6/10/11/16 | Step1,Step2,Step3,Step4,Step5,Step6,Step7,Step8 | §5精确归属，Step8保留旧baseline | 对应Step focused和旧Brand regression | §3可观察标准，验证实际执行日志，不是本轮代码证明 |
| REQ-016（主文1–15；前序重复ID按文档作用域保护） | 主Spec§4或品牌基线§4/6/10/11/16 | Step1,Step8 | §5精确归属，Step8保留旧baseline | 对应Step focused和旧Brand regression | §3可观察标准，验证实际执行日志，不是本轮代码证明 |
| REQ-017（主文1–15；前序重复ID按文档作用域保护） | 主Spec§4或品牌基线§4/6/10/11/16 | Step8 | §5精确归属，Step8保留旧baseline | 对应Step focused和旧Brand regression | §3可观察标准，验证实际执行日志，不是本轮代码证明 |
| REQ-018（主文1–15；前序重复ID按文档作用域保护） | 主Spec§4或品牌基线§4/6/10/11/16 | Step6,Step8 | §5精确归属，Step8保留旧baseline | 对应Step focused和旧Brand regression | §3可观察标准，验证实际执行日志，不是本轮代码证明 |
| REQ-019（主文1–15；前序重复ID按文档作用域保护） | 主Spec§4或品牌基线§4/6/10/11/16 | Step7,Step8 | §5精确归属，Step8保留旧baseline | 对应Step focused和旧Brand regression | §3可观察标准，验证实际执行日志，不是本轮代码证明 |
| REQ-020（主文1–15；前序重复ID按文档作用域保护） | 主Spec§4或品牌基线§4/6/10/11/16 | Step7,Step8 | §5精确归属，Step8保留旧baseline | 对应Step focused和旧Brand regression | §3可观察标准，验证实际执行日志，不是本轮代码证明 |
| REQ-021（主文1–15；前序重复ID按文档作用域保护） | 主Spec§4或品牌基线§4/6/10/11/16 | Step1,Step6,Step8 | §5精确归属，Step8保留旧baseline | 对应Step focused和旧Brand regression | §3可观察标准，验证实际执行日志，不是本轮代码证明 |
| REQ-022（主文1–15；前序重复ID按文档作用域保护） | 主Spec§4或品牌基线§4/6/10/11/16 | Step8 | §5精确归属，Step8保留旧baseline | 对应Step focused和旧Brand regression | §3可观察标准，验证实际执行日志，不是本轮代码证明 |
| REQ-101（目录上传） | 目录上传Spec§4/9/11 | Step6/8 | 旧File/Spec/Brand未改契约；配置/fixture必要更新 | File/Spec/Brand/CORS/profilefocused回归 | 表与目录/文件结果保持，live上传不由本Plan声称 |
| REQ-102（目录上传） | 目录上传Spec§4/9/11 | Step6/8 | 旧File/Spec/Brand未改契约；配置/fixture必要更新 | File/Spec/Brand/CORS/profilefocused回归 | 表与目录/文件结果保持，live上传不由本Plan声称 |
| REQ-103（目录上传） | 目录上传Spec§4/9/11 | Step6/8 | 旧File/Spec/Brand未改契约；配置/fixture必要更新 | File/Spec/Brand/CORS/profilefocused回归 | 表与目录/文件结果保持，live上传不由本Plan声称 |
| REQ-104（目录上传） | 目录上传Spec§4/9/11 | Step6/8 | 旧File/Spec/Brand未改契约；配置/fixture必要更新 | File/Spec/Brand/CORS/profilefocused回归 | 表与目录/文件结果保持，live上传不由本Plan声称 |
| REQ-105（目录上传） | 目录上传Spec§4/9/11 | Step6/8 | 旧File/Spec/Brand未改契约；配置/fixture必要更新 | File/Spec/Brand/CORS/profilefocused回归 | 表与目录/文件结果保持，live上传不由本Plan声称 |
| REQ-106（目录上传） | 目录上传Spec§4/9/11 | Step6/8 | 旧File/Spec/Brand未改契约；配置/fixture必要更新 | File/Spec/Brand/CORS/profilefocused回归 | 表与目录/文件结果保持，live上传不由本Plan声称 |
| REQ-107（目录上传） | 目录上传Spec§4/9/11 | Step6/8 | 旧File/Spec/Brand未改契约；配置/fixture必要更新 | File/Spec/Brand/CORS/profilefocused回归 | 表与目录/文件结果保持，live上传不由本Plan声称 |
| REQ-108（目录上传） | 目录上传Spec§4/9/11 | Step6/8 | 旧File/Spec/Brand未改契约；配置/fixture必要更新 | File/Spec/Brand/CORS/profilefocused回归 | 表与目录/文件结果保持，live上传不由本Plan声称 |

## 11. Risks, Blockers, and User Decisions

| ID | Risk or decision | Impacted Steps/files | Evidence | Owner | Status/action |
| --- | --- | --- | --- | --- | --- |
| DEC-208（Closed） | 必要单表转换与generic固定五variant责任已修复 | Step3全部Converter、Spec§8/10.4 | 用户要求修复；真实字段mapping与Common合同，不新增重复单表Command | User | Closed；单表必要职责完整，聚合uses复用，tooling事实仍实测审查 |
| RISK-002 | 字节级GEN ownership与后续业务扩展冲突 | Step1/2/3 | v2 plan拒绝手改原hash | 执行者 | 明确GEN先check、自定义扩展后不可rerun覆盖，state不手修 |
| RISK-003 | 权限运行时输入未部署或classpath漂移 | Step6/7 | 本地jar签名有能力，但未验runtime身份、Redis/角色 | 平台部署owner | 执行前dependency-tree/环境输入/live授权验收；不是再设计权限方案 |
| RISK-004 | 当前默认Mavenoffline仓库origin不匹配 | tooling/所有futuremvn | FreeMarker jar有central origin但active repos不包含它，实测offline命令失败 | 执行者/环境owner | 不改用户settings；generator已通过existingclasspath运行templates；tests若环境不解析如实停，不下载未批准依赖 |
| RISK-005 | PG部分唯一/时间精度/并发与H2不同 | Step1/3/5 | PostgreSQL partialNULL唯一语义不能H2模拟 | 数据库验收owner | manual gate未授权不运行，不降低schema约束 |
| RISK-006 | Primary Spec未Accepted、本Plan未批准 | 全Step | 当前Spec Review，用户只请求写Plan | mario | 不标Ready，不开始实现；方案答复不自动等同所有文档Accepted |

## 12. Review and Acceptance

### 12.1 Original requirement fidelity

原附件全部商品目标有主文REQ和对应Step，不扩前端/运费管理/订单库存/事件。旧Spec保护项按文档作用域记录，不重做已完成骨架。待确认converter范围不会被手写或额外Command悄悄绕过。

### 12.2 Spec consistency

SINGLE/LOCAL、标准JSON、SKU稳定ID、重新审核、review自动上架、P/E回收、永久清全部history、四类RBAC权限保持。本轮Spec DEC-208修正单表必要职责和真实业务mapping责任，Plan与主Spec同步；不以最小清单排除单表转换，不以rename伪装generic模板输出。仅局部测试fixture/生命周期statement复用属于记录的可逆澄清。

### 12.3 Repository executability

已核target不存在/旧路径、13/9计数目标、现有全SpringBootTest继承fixture、source层边界、模板5variants、generatorCLI v2实际目录、本地Jianshen签名。每File有精确operation/path/symbol/evidence/mapping/error/pseudocode/gate；每Step有cwd/selector/expected/failure/rollback和归属commitpaths。用户每logicaltask最多1commit覆盖技能默认per-Step commit。

### 12.4 Test and release completeness

RED/GREEN、H2事务、OAS/wire/security、oldboundary、sourcecontract、realPG/manual平台分别定义。未执行商品Java测试，不把plannedgate或templates成功当runtime证明。所有依赖/源码/配置修改属于未来批准执行，本轮仅文档与metadata。

### 12.5 Blocking Manual Check

| Check ID | Applicability | Status | Evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- |
| MC-ARCH-001 | Applicable | PASS | §4.7/Step1,8/currentLight plugin | exactLight，无第三结构 | None；此PASS仅计划设计证据，执行再验 |
| MC-REUSE-001 | Applicable | PASS | §4.7 ledger/实际POM/Codegen/Jianshen源与jar | Core/ID/MP/Validation/Jackson/springdoc/权限能力已核 | None；此PASS仅计划设计证据，执行再验 |
| MC-DEP-001 | Applicable | PASS | SpecDEC-208/§10.4、本PlanStep3与源码CLI事实 | 必要单表映射与聚合委托完整，通用GEN/商品CUSTOM责任已闭合；没有未批准依赖或被隐瞒的工具变体 | None；执行时仍验证实际output/compile/tests |
| MC-NAME-001 | Applicable | PASS | §5/Step2 exacttype | 所有语义载体/访问/行为suffix，无Data/Info | None；此PASS仅计划设计证据，执行再验 |
| MC-VALID-001 | Applicable | PASS | Step2/3/4/6/7 groups/MC边界 | native/组合/每层/DBshape分开，电话不适用 | None；此PASS仅计划设计证据，执行再验 |
| MC-MODEL-001 | Applicable | PASS | SpecDEC-208/§10.4、本PlanStep3与源码CLI事实 | 必要单表映射与聚合委托完整，通用GEN/商品CUSTOM责任已闭合；没有未批准依赖或被隐瞒的工具变体 | None；执行时仍验证实际output/compile/tests |
| MC-CONVERT-001 | Applicable | PASS | SpecDEC-208/§10.4、本PlanStep3与源码CLI事实 | 必要单表映射与聚合委托完整，通用GEN/商品CUSTOM责任已闭合；没有未批准依赖或被隐瞒的工具变体 | None；执行时仍验证实际output/compile/tests |
| MC-LOG-001 | Applicable | PASS | Step4/5/6/7 businessBean伪代码 | Slf4j安全标识与commit outcome，不日志token/HTML | None；此PASS仅计划设计证据，执行再验 |
| MC-BEAN-001 | Applicable | PASS | M/lombok.config、Step6 required bean/fake隔离 | named/final/RequiredArgsConstructor/Qualifier复制已核 | None；此PASS仅计划设计证据，执行再验 |
| MC-UTIL-001 | Applicable | PASS | Step2 decoder/validator、Step8 static gate | JDK/Jackson/Jakarta/现有Commons，无新工具 | None；此PASS仅计划设计证据，执行再验 |
| MC-JSON-001 | Applicable | PASS | Step2 typedwire/Step3/Step7 OAS | stringIDs/unknownreject/Jackson-only/enum stable codes | None；此PASS仅计划设计证据，执行再验 |
| MC-TIME-001 | Applicable | PASS | Step3/5 monotoneUTC/Step2 Instant | java.time、µs、Duration、noDate | None；此PASS仅计划设计证据，执行再验 |
| MC-CONFIG-001 | Applicable | PASS | Step3 MP键先配置、Step6全部profile授权键 | 同结构不同值，旧profile/input保持 | None；此PASS仅计划设计证据，执行再验 |
| MC-PATTERN-001 | Applicable | PASS | Step5 actualSpecification registry | 复杂生命周期规则组合；简单diff不加虚假Strategy | None；此PASS仅计划设计证据，执行再验 |
| MC-SCOPE-001 | Applicable | PASS | §5/Spec§3/AGENTS/stepcommitboundary | 仅必要goods，不删样例、不改历史SQL/平台源码 | None；此PASS仅计划设计证据，执行再验 |
| MC-TEST-001 | Applicable | PASS | §7/8所有focusedselectors/oldregression/runtime gates | 未来命令有cwd/expected/failure，未伪报已执行 | None；此PASS仅计划设计证据，执行再验 |
| MC-BLOCKER-001 | Applicable | PASS | SpecDEC-208/§10.4、本PlanStep3与源码CLI事实 | 必要单表映射与聚合委托完整，通用GEN/商品CUSTOM责任已闭合；没有未批准依赖或被隐瞒的工具变体 | None；执行时仍验证实际output/compile/tests |

### 12.6 Final verdict

PASS — Ready for user review

用户要求的转换职责修复已写入Spec与Plan，8关卡完整，可以供用户审核；当前Review而非Ready。未执行任何商品源代码Step/DDL/Java tests，不将文档校验当实现验收。后续tooling冲突/缺classpath按执行前置如实停止，不虚构converterKind参数或默认扩展Command。

## Java / CQE contract review

POJO/Enum→Step2与MC-MODEL/JSON；组件/MP→Step1/3/6与MC-REUSE/DEP；每层注解校验→Step2/3/4/6/7；业务deleted_at双唯一+NULL+微秒→Step1/3/5与MC-MODEL/TEST；C/Q共享库，无本章Event，所以不加Outbox或本地fakeEvent；exactLight→全StepMC-ARCH。源码和fake/H2不证明真实PG/Redis/身份投递，边界如§8。

## Backend generation and dependency evidence

当前CLI templates实际exit0：FreeMarker2.3.35、planFormatVersion2、native light/web/service，templateSetDigest 684468699d51ecfdfac9eb6bf9f0d2675872095315a2cc1ecaf4fc613908f0b9。仅本轮读取目录，未plan/apply、不追加journal。未来GEN选三份配置/14文件，后续定向business SQL/BO记录agentowned；无手写初始PO/DAO/XML/Repo。generic converter固定5variant的事实保留；Product专用typedJSON/共享Save分组/受控Result业务映射见DEC-208，单表Save/Result Mapper必要且由聚合复用。不写新的converterKind开关、不复制ftl、不生成无消费者的另一套Command/HTTP。工具classpath恢复在/tmp不commit，必要artifact下载未执行。每成功apply或最后授权plan必须追加SQLjournal，准确throughVersion/字节hash/fullSQL；非PG执行记录。#### File 1 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/ddl/ProductDdlContractTest.java`

- Purpose: 先证明单版本/继承字段/约束与旧hash不变
- Symbols: ProductDdlContractTest
- Repository evidence: 现有CatalogDdlContractTest采用Files/Pattern/Jackson/SHA256检查资源，不连接数据库；当前文件不存在，位置由主Spec目标清单或既有测试同包职责定位。
- Dependencies and consumers: ProductDdlContractTest 的调用/生成消费方为本Step所列focused测试与下一层；遵循§4依赖方向与主Spec§7/9.5精确接口，不产生反向import。
- Why now: 按本Step严格顺序第1个文件；先固定observable RED，再补最小implementation；编译契约前置与实际RED执行点已由Test-first gate说明。
- Contract/signature changes: ProductDdlContractTest；仅以上列出的新声明或已有符号增量，外部原有契约保持。
- Input/output and state mapping: SQL字节→SHA256/manifest；MASTER_DATA建spu/sku，SHARD空分支，不重建category_brand。
- Error and edge behavior: 缺脚本RED；旧hash变化/版本重复立即失败；不把静态DDL检查当PG执行。
- Standards impact: MC-TEST-001, MC-SCOPE-001, MC-VALID-001 — JUnit/MockMvc/现有fixture；nativegroups/真实结果断言，不建外部runtime；测试不是businessBean，沿用现有test injection风格。
- Literal rule enforcement: Rule 2, Rule 11 — 所选边界正负例/同模块隔离；不把H2或fake当PG/平台证明。
- Implementation pseudocode:

```java
@Test schema_and_manifest_contract() {
  assertThat(Path.of("src/main/resources/db/egon-mp/V20261009_001__initialize_product_schema.sql")).exists();
  parse CREATE definitions; assert exactly spu,sku; assert eight EgonModel columns and all Spec §11 business columns;
  assert lifecycle+active indexes, parent FK RESTRICT, SKU P/E CHECK and manifest sha256;
  compare three predecessor SQL SHA256 against baseline constants; no live datasource;
}
```

- Verification contribution: ProductDdlContractTest；cwd为模块（generator仅仓库根），本Step结束的exact selector观察该文件贡献。
- After this file: 测试已写；明确RED执行点需要的compile prerequisites完成后运行，不以fixture/环境失败凑RED。


## 12. 已批准的前置修正 Step C — MP 生命周期策略（2026-10-09）

批准依据：集中提案后用户“批准，开始改动吧”。此Step插入商品Step5继续验证前；Step1–4 checkpoint保留，原Step6–8顺序不变。有效Spec增加§21/DEC-211。

组件根路径：/Users/mario/SelfProject/Egon-COLA/egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter。
精确写清单：
1. MODIFY src/main/java/top/egon/cola/component/common/mybatis/autoconfigure/EgonColaMybatisPlusProperties.java：LifecyclePolicyBO及ScopeEnum，精确statement映射，原allowlist兼容。
2. CREATE src/test/java/top/egon/cola/component/common/mybatis/interceptor/EgonColaLifecyclePolicyTest.java：原保护负例与新增策略正反例，SQL AST单元验证；不冒充PG。
3. MODIFY src/main/java/top/egon/cola/component/common/mybatis/autoconfigure/EgonColaMybatisPlusContractValidator.java：配置标识/策略/SQLcommand/存在性校验。
4. MODIFY src/main/java/top/egon/cola/component/common/mybatis/interceptor/EgonColaTenantIdGuardInnerInterceptor.java：不可变策略快照，Specification条件与scope策略分派；全部权威tenant/状态/版本/原tombstone验证。
5. MODIFY src/main/java/top/egon/cola/component/common/mybatis/interceptor/EgonColaLocalWriteGuardInnerInterceptor.java：purge必须实际LOCAL transaction、ConnectionHolder、同组写目标。
6. MODIFY src/test/java/top/egon/cola/component/common/mybatis/contract/EgonColaLifecycleContractTest.java：默认失败关闭/非法配置/运行期配置变更不可启用；必要H2生命周期验证。

RED：新guard策略测试先声明typed配置（仅编译前置），旧guard拒绝合法声明语句；负例原拒绝保持。GREEN实现后执行 `mvn -o -B -ntp -Dtest=EgonColaLifecyclePolicyTest,EgonColaLifecycleContractTest,EgonColaLocalWriteGuardTest,EgonColaRepositoryTest test`，再组件verify；默认runtime/Testcontainers不启动。验证/Manual Checks通过后该独立组件任务一次path-limited commit，再install本地产物。

goods必要接线：已声明SpuDAO/SkuDAO、SpuRepository/SkuRepository、Mapper XML、ProductDomainServiceImpl、4profiles/imported配置及当前product测试。purge改用有校验的scalar id/version/deletedAt，子项删除附同版本回收父项EXISTS，restore原tombstone精确参数；去掉错误表名write-root配置。每步修正检查diff、重复Step1–4聚焦回归，再继续Step5正常顺序。
无新迁移/依赖版本/数据库运行/其他业务改动。最终Step8仍只有商品task一次commit，Spec/Plan已有未跟踪状态不强行纳入代码提交。

Step C机械实现补充：MODIFY src/main/java/top/egon/cola/component/common/mybatis/interceptor/EgonColaOriginalSqlGuardInterceptor.java 的nested visitor，JSqlParser5.2将子查询分派到visit(Select)而非visit(ParenthesedSelect)，必须收集该scope才能验证EXISTS父项。由已有Step C策略测试覆盖，保持租户保护，非新增业务边界。

Step C必要测试更新：MODIFY src/test/java/top/egon/cola/component/common/mybatis/contract/EgonColaContractGovernanceTest.java，精确登记新增ScopeEnum与源码已有RESTORE；MODIFY同包EgonColaRepositoryArchitectureTest.java，保留upstream57方法完整匹配，额外精确匹配已有restoreById一个方法。全组件verify暴露旧assertions尚未登记恢复能力；不修改生产接口或放松数量/任意方法检查。

Step6 artifact校准：现有Shoubing5.4.1缓存缺少Spec已声明的VerifiedTenantContextFilter。只在既有shoubing-starter目录离线mvn -DskipTests install刷新原版本产物，未改源代码/未新增依赖/未触及admin脏文件，避免goods自造MDC身份绑定。后续旧上传链回归验证实际保持。日志/tmp/enjoyshop-shoubing-artifact-refresh.log。

Step7必要fixture/输入校准：7个既有@WebMvcTest slice只增加ProductTestSecurityConfiguration导入（BaseExceptionHandlerTest、BrandControllerWebTest、PermissionControllerTest、RoleControllerTest、UserControllerTest、SchoolClassControllerTest、CourseControllerTest），不改旧断言。新增required security beans不能以放松生产chain解决。输入6个Command/Query的AllArgsConstructor仅对Jackson显式DISABLED creator，保留完整Lombok/Builder，解决现有ConstructorProperties使省略字段丢失默认值；Price/token/version/unknown字段HTTP正反例覆盖。原ProductIdDeserializer新增局部Long整型codec，Rootversion不接受String/float。单表SaveMapper和Query.sn复用已有Mapper承担规范化trim，不影响ID/状态合同。Jianshen真实jar属性是runtime.database/password-file及String scopes，4profile纠正字段路径与值类型；runtime.timeout保持Spec PT2S。上述均为原合同实现所需修复，不增加endpoint/DTO/依赖/业务范围。

Step8必要架构回归校准：RepositoryPersistenceContractTest仅将精确Mapper XML数量10→13，全部读/删/版本保护断言保留。ArchetypeContractConvergenceTest按有效Java/CQE合同明确区分原生ConstraintValidator与通用BaseValidator、具名JSON字段codec与实体MapStruct mapping、P/E持久化scalar enum与仅服务器内部动作enum；不豁免Common converter、稳定编码、Jackson或未注册新类型。新增ProductContractConformanceTest及已有Mapper/Composition tests提供职责隔离正负证据。无生产接口变化。

Step8字节码架构修复（无网络合同变化）：RENAME domain/goods/ProductSkuDeletionKindEnum.java→common/enums/ProductSkuDeletionKindEnum.java，保留EnumValue/JsonValue稳定P/E，更新全部已知SkuBO/converter调用者；Domain ProductDomainService仅保留Jakarta约束，Validated/operation groups移到现有Infrastructure实现；Domain生命周期纯Specification不带Spring注解，CREATE infrastructure/config/ProductDomainConfiguration.java具名Bean装配。CREATE adapter/goods/ProductOpenApiConfiguration.java使用既有OpenApiCustomizer，业务DTO的Schema投影从start/OpenApiConfig迁出；启动层只保留通用API标题/securityScheme。其余职责/原Light结构不变，不以baseline/ignore消除架构错误。接受测试追加原Spec已列race、noop、purge failure与安全故障。fake timeout按HttpClient既有typed unavailable合同模拟，不把不遵守client timeout的sleep当成平台证明。
