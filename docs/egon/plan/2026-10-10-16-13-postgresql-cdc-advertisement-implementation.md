# PostgreSQL 主从、广告缓存与 RabbitMQ CDC 实施 Plan

| Field | Value |
| --- | --- |
| Document | `2026-10-10-16-13-postgresql-cdc-advertisement-implementation.md` |
| Template Version | `4` |
| Status | Completed |
| Created | 2026-10-10 16:13 Asia/Shanghai |
| Updated | 2026-10-10 20:33 Asia/Shanghai |
| Owner | mario |
| Repository | enjoyshop |
| Scope | goods native Light：NATIVE主从配置、广告管理/匿名读取/缓存、CDC规范化/投递/失效；独立Server配置准备；搜索仅协议 |
| Source Requirement | 用户“confirmed spec doc, writing plan”；后续明确批准Domain Jakarta约束/NATIVE SQL目标修正、Step 1补列ProfileKeyParityTest/dataSourceGroups YAML结构，以及Step 2补入 `--schema` 校验脚本；不执行真实数据库/服务部署 |
| Baseline Revision | main / 7e6a5065cae81836cfa6744f51fba686f8f8df92；primary Spec尚未跟踪，之前4份docs已由mario提交；其他工作区Egon-COLA只读 |
| Implements Spec | [2026-10-10-14-48-postgresql-cdc-advertisement-synchronization.md](../spec/2026-10-10-14-48-postgresql-cdc-advertisement-synchronization.md) |
| Spec Status | Accepted |
| Spec Revision | 2026-10-10 16:33 Asia/Shanghai；本会话用户接受全文并批准§10.3.1约束修正；原基线7e6a506 |
| Effective Specs | [2026-10-10-14-48-postgresql-cdc-advertisement-synchronization.md](../spec/2026-10-10-14-48-postgresql-cdc-advertisement-synchronization.md)；[品牌骨架](../spec/2026-09-23-17-16-enjoyshop-scaffold-brand-crud.md)；[目录与文件](../spec/2026-09-28-21-50-distributed-file-catalog-lookups.md)；[首页架构](../spec/2026-10-09-16-39-homepage-kubernetes-gateway-architecture.md) |
| Depends On Plans | None |
| Supersedes | None |
| Superseded By | None |
| Related Plans | [首页部署准备](2026-10-09-18-55-homepage-gateway-deployment-preparation.md)（Completed，只作背景，不重开） |

## 1. Summary

本Plan将已接受Spec拆为9个可独立验证的逻辑任务：先Native配置和单一DDL/生成，再完整广告管理/HTTP安全/缓存，再CDC协议与Rabbit链，最后独立Server部署准备和回归收尾。每Step包含严格文件顺序、RED/GREEN、唯一语义提交与回退点；只在该Step检查通过后提交，记录HEAD后才能开始后继。9个Step是9个逻辑任务，每任务最多一个提交，不拆成多个补丁commit，也不合并无关dirty工作。

本Plan已按用户批准的执行请求完成9个顺序Step并分别提交。代码和离线配置门禁通过不代表真实PG/Rabbit/Redis/CDC、搜索投影或K8s已经运行；这些TEST-019–022门禁仍为Not run并阻止部署。本轮没有启动服务或执行数据库DDL。

路径简称：M=`enjoyshop-service/enjoyshop-service-goods`，J=`M/src/main/java/top/egon/enjoyshop/goods`，R=`M/src/main/resources`，T=`M/src/test/java/top/egon/enjoyshop/goods`，E=`/Users/mario/SelfProject/Egon-COLA`。§5/7仍列精确仓库相对路径，简称只用于解释。

## 2. Target Spec and Effective Design

### 2.1 Primary target

Primary=[2026-10-10-14-48-postgresql-cdc-advertisement-synchronization.md](../spec/2026-10-10-14-48-postgresql-cdc-advertisement-synchronization.md)，全章节与REQ-001–012有效；用户“confirmed spec doc”构成接受证据，新增修正于本轮独立确认。Spec Header记录Accepted/最新Updated和本Plan关联。仓库源实际未有ad/CDClistener/读写replicas，因此不是对已完成工作的重复Plan。

### 2.2 Effective Spec set

| Role | Spec/link | Status/revision | Effective sections | Why included |
| --- | --- | --- | --- | --- |
| Primary | [2026-10-10-14-48-postgresql-cdc-advertisement-synchronization.md](../spec/2026-10-10-14-48-postgresql-cdc-advertisement-synchronization.md) | Accepted；本轮Updated 2026-10-10 16:13 | 全文，特别§4/8–11/14–16/20 | 本次12需求、6API、3事件、16ad列和运行边界 |
| Dependency | [品牌骨架](../spec/2026-09-23-17-16-enjoyshop-scaffold-brand-crud.md) | Accepted；2026-09-24 15:20 CST | §5.4/6.1 | 精确native Light/MP/不新增工程或网关 |
| Dependency | [目录与文件](../spec/2026-09-28-21-50-distributed-file-catalog-lookups.md) | Accepted；2026-09-29 11:11 CST | §9/16 | S3公开URL/上传认证与错误信封不变 |
| Dependency | [首页架构](../spec/2026-10-09-16-39-homepage-kubernetes-gateway-architecture.md) | Accepted；2026-10-09 21:35 Asia/Shanghai | §7/15/16，广告边界由primary补充 | Envoy/Yuheng/K8s、JavaCache与发布输入 |

### 2.3 Superseded or excluded content

没有全篇替代。Primary只补首页匿名精确GET与广告功能；旧商品Spec Header为Review、作为已实现源码背景，不升级其审批状态或引入新生命周期需求。此前Completed首页准备Plan和5部署文件不重新实施。无广告UI、搜索引擎/索引表、Kafka、Canal、Outbox/SSM、组件源码修改；旧教学/用户/MQ样例原样保留，不因新增业务删除无关示例。新ad catalog暂态文件只在引入任务内收敛/删除，非最后扫尾。

## 3. Effective Requirements and Acceptance

| Requirement | Source Spec section | Effective statement | Observable acceptance | Implementation impact |
| --- | --- | --- | --- | --- |
| REQ-001 | primary §4/19 | 分清物理复制、路由、CDC | PRIMARY→standby不走MQ；逻辑slot只接PRIMARY；ordinary read与事务读有不同路由 | Steps 1,8,9；用户pgsql主从要求 |
| REQ-002 | primary §4/19 | 应用使用NATIVE且不改组件 | Native+无STRATEGY tables；保留全部业务表与SQL定义的2n目标/LOCAL；不声称旧STRATEGY已正确实现2n；配置各环境同键、真实路由和profile回归 | Steps 1,2,9；已确认选择 |
| REQ-003 | primary §4/19 | Debezium Server直连Rabbit捕获提交DML | 无Kafka服务；publication只ad/spu/sku；事务rollback不产生业务刷新；confirmed transport后才checkpoint | Steps 6,7,8,9；已确认替代Canal |
| REQ-004 | primary §4/19 | 实现广告管理后端合同 | 5个管理操作闭合，USER/RBAC、租户/version/软删除/字段约束完整 | Steps 2,3,4,5,9；用户广告管理本期 |
| REQ-005 | primary §4/19 | 匿名读仅按可信站点映射租户 | 公开GET只读；X-Tenant/Actor不能覆盖；未知Host404；默认tenant1不可用于此路径 | Steps 4,7,8,9；用户确认 |
| REQ-006 | primary §4/19 | 广告两级缓存覆盖计划时间 | 缓存候选、逐请求一次UTC时钟筛start≤now<end；PRIMARY回源；不包住缓存命中的事务；明确TTL/失效局限 | Steps 4,5,9；第6章+首页已接受方向 |
| REQ-007 | primary §4/19 | CDC失效覆盖旧/新广告位及删除 | update移动同时失效old/new，delete取before；重复/乱序只evict，跨租户before/after分组不串数据 | Steps 2,3,5,6,7,9；教程缓存更新目标 |
| REQ-008 | primary §4/19 | 商品搜索仅刷新事件/消费合同 | 覆盖spu/sku c/u/d/r、两次父归属变化；不根据旧UP/DOWN直接执行；future消费者PRIMARY重读+fence/tombstone | Steps 6,7,8,9；用户搜索分期 |
| REQ-009 | primary §4/19 | 稳定身份、confirm/ACK/retry/DLQ | 身份不取处理时间或broker deliveryTag；部分confirm/ACK丢失可重复安全重试；毒消息隔离；不宣称exactly once | Steps 6,7,8,9；同步正确性隐含需求 |
| REQ-010 | primary §4/19 | 主从/CDC恢复可操作 | 一slot一owner；WAL/offset/epoch/源角色/副本延迟监控；有promote/slot丢失/全量重建门禁；不RESET/repair历史 | Steps 1,7,8,9；主从/部署要求 |
| REQ-011 | primary §4/19 | 数据/依赖/历史与接口兼容 | 一个新SQL+manifest；旧SQL不变；无新应用依赖；原API/wire/S3/persistence框架保持 | Steps 1,2,3,4,5,6,7,8,9；AGENTS与技能 |
| REQ-012 | primary §4/19 | 文档可审查且不执行实现 | Spec strict/关系/范围复核通过；当前writing-plan任务只改本文和授权Spec，未改生产/test/DDL文件或启动服务；用户现在授权Plan-only，仍不授权开始实现 | Steps 9；用户writing-spec |

前置文档同名REQ按Spec路径限定；超出primary编号的原需求在Step9仅作兼容保护/已完成范围排除，§10逐项标记，不重做原feature。

6HTTP的method/path、operationId、body/参数/错误/权限完全以primary §9.2.1–6为准；3Event和sourceEpoch/LSN/old-new target以§9.2.7–9为准。窗口UTC/微秒、Long string IDs/整数version、Delete终态true、public4字段/no-store、schema1/稳定EID、不以旧UP命令写索引，均进入相应Step的断言。source.ts在snapshot不等价原commit时刻。

## 4. Implementation Strategy and Dependency Order

### 4.1 Ordered strategy

Step1按用户新批准的不可变SQL目标替换sharding规则；当前STRATEGY与SQL不一致先明确记录，不声称路由变更自动兼容历史数据，不迁移数据；Step2一个新managed版本加CREATE-only初始catalog，不发布未受保护HTTP。Step3完成内部管理和公开查询语义，Step4才同时发布6API与安全链。Step5接缓存只改变加载/失效实现，保持HTTP结果。Step6先形成pureFacts/Strategy/Event，Step7再启用受flag控制的实际Rabbit producer/listeners。Step8是正式环境配置模板与运行门禁，Step9完整回归/归位临时脚本；所有期间均不启动服务或部署不完整Step。

### 4.2 Test-first strategy

每Step第一组File为focused RED：static reflection/resource assertions可在类型未创建时证明缺失；需要编译前提时用已有接口/资源反射建立RED，生成基线后再补直接类型断言。缺类作为approved compile prerequisite须显式记录，不能把DB连接失败称RED。普通业务从合法fixture/negative约束/CAS开始，Cache/confirm/ACK用可控组件而非真实依赖。命名测试GREEN后才做最小affected-regression，未执行真实验收不补假证据。

### 4.3 Sequential and parallel boundaries

| Step | Depends on | May run in parallel with | Must not overlap with | Reason |
| --- | --- | --- | --- | --- |
| Step 1 | None | None | 所有同工作树Step；重复文件按本Step具名symbols限定 | 全原表/2n/LOCAL及三组逻辑身份保留，六物理池与事务主读规则可离线验证；不连接PG。 |
| Step 2 | Step 1 | None | 所有同工作树Step；重复文件按本Step具名symbols限定 | 一个新SQL/manifest、16列ad、三表RI FULL；离线生成初始6类/Mapper，未发布HTTP。 |
| Step 3 | Step 2 | None | 所有同工作树Step；重复文件按本Step具名symbols限定 | 管理的create/replace/page/detail/delete在Light内部可调用并受verified权限保护；无HTTP暴露，所有字段/version/软删除语义闭合。 |
| Step 4 | Step 3 | None | 所有同工作树Step；重复文件按本Step具名symbols限定 | 全部6HTTP操作/OAS与原信封一致；匿名tenant只由登记Host，管理verified USER/RBAC；原API不漂移。 |
| Step 5 | Step 4 | None | 所有同工作树Step；重复文件按本Step具名symbols限定 | 公开读命中L1/L2，miss只在PRIMARY事务回源；窗口每请求检查，管理写提交后失效old/new；TTL与SPI吞失败限制有证据。 |
| Step 6 | Step 5 | None | 所有同工作树Step；重复文件按本Step具名symbols限定 | 已提交raw facts可转为两个tenant隔离语义事件；身份稳定、old/new union完整；没有索引引擎。 |
| Step 7 | Step 6 | None | 所有同工作树Step；重复文件按本Step具名symbols限定 | raw只在全部semantic confirm且无return后ACK；重投身份稳定、poison隔离；Ad消费验证scope后尝试SPI失效，search无假消费。 |
| Step 8 | Step 7 | None | 所有同工作树Step；重复文件按本Step具名symbols限定 | 部署文件覆盖pgoutput/slot/offset/正式版本输入/Rabbit拓扑与K8s单owner；离线检查可区分配置通过与发布阻塞。 |
| Step 9 | Step 8 | None | 所有同工作树Step；重复文件按本Step具名symbols限定 | 源代码/离线配置/协议回归和原合同保护证据完成；scratch清零；运行门禁单独未运行标注，未启动服务。 |

本Plan不要求多agent。审阅可并行，写入和Step提交必须顺序，profiles/架构测试/生成config的多次修改在§5列所有owner与§7分段符号；不把后继未完成修改提前混入提交。唯一当前任务的Plan文档不自动commit。

### 4.4 Migration and compatibility constraints

现latest20261009_001；执行前复查若并发新migration占version就停止并按新latest选一个次版本，不动旧SQL/history。managed runner每PRIMARY/schema/version持有advisory transaction lock，SQL+ddl_history同连接提交；SHARD本版本no-op但仍按runner记录，后目标失败不回滚已提交目标；unknown commit新连接核对后续跑，无repair/adopt/drop。最小应用依赖全部现有；没有POM变更。只在准备/离线完成并满足真实发布输入后启CDC。

### 4.5 Spec simplicity and implementation-necessity audit

| Spec element | Spec necessity verdict/section | Current repository evidence | Direct/reuse alternative | Plan decision |
| --- | --- | --- | --- | --- |
| 6ads REST | primary§7.0 Add/§9；操作goal清楚 | 无ad路由；已有Product/Brand同Light链 | 复用标准wrapper/原身份，不造参数预查询 | Implement Steps3/4 |
| ad单表/纵切面 | §7/8/10/11 Add | ad absent，PO/DAO/repo13 | 现goods内新增，不拆模块/框架 | Implement Step2/3 |
| NATIVE规则 | §7 Keep能力/应用新配置 | loader已有，strategy不组RW | 完整native读写group而非改组件 | Implement Step1 |
| AdCandidates区域/Specification | §7/13 Add真实时间变化点 | CacheSPI/regionalTTL已有 | 候选+逐读Clock，不定时任务/Lua缓存 | Implement Step5 |
| 2CDC Strategy/Event/identity | §7/13 Add表归属差异 | 当前无CDC；原MQafterCommit不足 | narrowStrategy registry+Common mapper；不新store | Implement Step6/7 |
| Narrow SyncEvent metadata | §8已有Strategy/Event，§9发布需稳定身份 | 两个Event字段相同metadata，publisher无需知道target数组类型 | Strategy内组合identity+converter，不做instanceof/reflection分支 | Plan Clarification，nested SPI无新wire/file |
| 独立Server与运维合同 | §7/15/16批准新增runtime | 商品源SINGLE/master_data，Rabbit已有 | 无Kafka/Outbox，Rabbit不复制数据库 | Implement Step8 |
| search consumer/receipt/index储存 | §3.2/8/18明确Not applicable | 当前无搜索模块 | 只refresh协议/futurefence，不假消费/ACK | Keep out of scope |
| Source/Host/tenant preflight接口 | §7.0拒绝多调用 | verifiedUSER/配置source/site已有来源 | 从trustedcontext/registry派生，不让caller fetch-forward | None added |
| 临时harness/CREATE-only视图 | primary§8/11要求extract/hash；技能rule22 | 现无跨SQL/Native/props全面静态命令 | 现JUnit+stdlib少量检查，最终归属到codegen/deployment | Implement Steps1/2/9，仅本子系统 |

### 4.6 Change-unit dependency matrix

| Change unit | Requirements | Proof/RED point | Compile/runtime prerequisites | Produces | Consumers/unblocks | Owning Step |
| --- | --- | --- | --- | --- | --- | --- |
| 按不可变SQL确定 NATIVE 主从路由配置 | REQ-001,REQ-002,REQ-010,REQ-011 | NativeReadwriteContractTest,ProfileKeyParityTest,LightPersistenceArchitectureTest | 当前源/POM | 全原表/2n/LOCAL及三组逻辑身份保留，六物理池与事务主读规则可离线验证；不连接PG。 | 后继Step/最终门禁 | Step 1 |
| 新增单一广告 DDL 并生成持久化与领域模型基线 | REQ-002,REQ-004,REQ-007,REQ-011 | AdPersistenceTest,NativeReadwriteContractTest,ProfileKeyParityTest,LightPersistenceArchitectureTest | Step 1已提交 | 一个新SQL/manifest、16列ad、三表RI FULL；离线生成初始6类/Mapper，未发布HTTP。 | 后继Step/最终门禁 | Step 2 |
| 实现租户隔离和版本保护的广告管理业务 | REQ-004,REQ-007,REQ-011 | AdManageTest,AdPersistenceTest,LightPersistenceArchitectureTest | Step 2已提交 | 管理的create/replace/page/detail/delete在Light内部可调用并受verified权限保护；无HTTP暴露，所有字段/version/软删除语义闭合。 | 后继Step/最终门禁 | Step 3 |
| 发布6个广告API并闭合匿名站点和管理权限边界 | REQ-004,REQ-005,REQ-006,REQ-011 | AdControllerTest,AdOpenApiContractTest,AdSecurityContractTest,AdManageTest | Step 3已提交 | 全部6HTTP操作/OAS与原信封一致；匿名tenant只由登记Host，管理verified USER/RBAC；原API不漂移。 | 后继Step/最终门禁 | Step 4 |
| 启用候选广告两级缓存并覆盖提交后失效 | REQ-004,REQ-006,REQ-007,REQ-011 | AdCacheContractTest,AdManageTest,AdControllerTest,NativeReadwriteContractTest | Step 4已提交 | 公开读命中L1/L2，miss只在PRIMARY事务回源；窗口每请求检查，管理写提交后失效old/new；TTL与SPI吞失败限制有证据。 | 后继Step/最终门禁 | Step 5 |
| 定义PG事实规范化、稳定事件身份和搜索消费协议 | REQ-003,REQ-007,REQ-008,REQ-009,REQ-011 | CdcContractTest,AdCacheContractTest | Step 5已提交 | 已提交raw facts可转为两个tenant隔离语义事件；身份稳定、old/new union完整；没有索引引擎。 | 后继Step/最终门禁 | Step 6 |
| 接入Rabbit确认投递、有限重试和广告失效消费 | REQ-003,REQ-005,REQ-007,REQ-008,REQ-009,REQ-010,REQ-011 | CdcContractTest,AdCacheContractTest,AdSecurityContractTest,NativeReadwriteContractTest | Step 6已提交 | raw只在全部semantic confirm且无return后ACK；重投身份稳定、poison隔离；Ad消费验证scope后尝试SPI失效，search无假消费。 | 后继Step/最终门禁 | Step 7 |
| 准备独立CDC运行配置和主从恢复验收合同 | REQ-001,REQ-003,REQ-005,REQ-008,REQ-009,REQ-010,REQ-011 | CdcContractTest,NativeReadwriteContractTest | Step 7已提交 | 部署文件覆盖pgoutput/slot/offset/正式版本输入/Rabbit拓扑与K8s单owner；离线检查可区分配置通过与发布阻塞。 | 后继Step/最终门禁 | Step 8 |
| 完成范围审查、回归和验证脚本归属收尾 | REQ-001,REQ-002,REQ-003,REQ-004,REQ-005,REQ-006,REQ-007,REQ-008,REQ-009,REQ-010,REQ-011,REQ-012 | AdControllerTest,AdOpenApiContractTest,AdSecurityContractTest,AdManageTest,AdPersistenceTest,AdCacheContractTest,CdcContractTest,NativeReadwriteContractTest,ProfileKeyParityTest,LightPersistenceArchitectureTest,ProductContractConformanceTest | Step 8已提交 | 源代码/离线配置/协议回归和原合同保护证据完成；scratch清零；运行门禁单独未运行标注，未启动服务。 | 后继Step/最终门禁 | Step 9 |

### 4.7 Java, Spring, and Egon-COLA Implementation Standards

| Concern | Current repository evidence | Effective Spec decision | Planned implementation consequence | Owning Steps/checks |
| --- | --- | --- | --- | --- |
| Architecture profile | M/POM nativeLight parent5.4.1；E/egon-cola-archetypes/source-projects/egon-cola-source-light；J现adapter/application/domain/infra/start/common；LightPersistenceArchitectureTest | §6/8只现Light | Controller→Manage→DomainService→Repository→DAO/XML；无新module/端口副本 | All; MC-ARCH-001 |
| Reuse/capability | MP-SDJ/Cache/AMQP/Validation/MapStruct/原身份/OAS已声明 | 不增加应用deps、不改组件 | 原TypedProperties/事务manager/CachePort/RabbitConnectionFactory | All; MC-REUSE-001/MC-DEP-001 |
| Naming/model/validation/conversion | SpuBO/Jakarta、ProductValidationGroup/Manage、EgonModel、BaseConverter | 用户本轮批准DomainJakarta纠正，其余§10 | 普通class完整Lombok；groups+代理/手动同约束；MapStruct/BaseConverter | 2–7; MC-NAME-001/MC-VALID-001/MC-MODEL-001/MC-CONVERT-001 |
| Bean/log/JSON/time/config | lombok.config复制Qualifier/Value；ProductManageImpl具名构造；原ResultRecord/Instant；4profiles/import | §10/15 | final qualified构造，Slf4j，Jackson/UTC微秒/Duration；同键模板 | 1–9; MC-BEAN-001/MC-LOG-001/MC-JSON-001/MC-TIME-001/MC-CONFIG-001 |
| Pattern | ProductLifecycleComponent组合；CDC ad与商品根有不同变化点 | §13 Strategy+Specification | 2Strategy注册/编排及纯资格predicate；简单CRUD直接事务 | 3/5/6/7; MC-PATTERN-001 |

#### Capability reuse ledger

| Need | Candidates inspected | Exact evidence | Fit/gap | Decision | Added dependency/custom code | Owning Step/check |
| --- | --- | --- | --- | --- | --- | --- |
| Persistence/routing | Spring事务/Bootpool/MP-SDJ/NATIVE | EgonColaShardingYamlLoader.loadNative、TopologyValidator、Mapper/Repository、GoodsPOM | Native能力有，RW配置缺；Common CRUD已有 | Fullrules+namedXML，Common versioneddelete(et) | 无deps；应用配置/ad SQL | 1–3; MC-REUSE-001 |
| Cache | SpringCache/EgonColaCachePort/GuavaRedisson | EgonColaCacheProperties.regions、TwoLevelCache.get、Port.registerEvictionAfterCommit | 区域TTL/afterCommit有；失效失败可warn | 复用Port+PRIMARYTransactionTemplate | 无组件修改；业务区域 | 5/7; MC-REUSE-001 |
| Identity/RBAC | Shoubing/Jianshen/原严格guard | ProductAuthorizationComponent、NativeHttpSecurityConfiguration | USER绑定可用，productregex不能接adpermission | AdGuard composeAuthorizationService+publicHostscope | 无deps；newadguard/filter必要 | 3/4; MC-REUSE-001/MC-VALID-001 |
| Mapping/validation | Jakarta/MapStruct/Common | ProductApplicationConverter/BaseForwardConverter、ValidationUtils、PO groups | 全能力已有；BO可Jakarta约束 | Strict scalar Jackson+native/custom constraints | 无utils/JSON新库 | 3–7; MC-CONVERT-001/MC-DEP-001 |
| API documentation | springdoc MVC/Yuheng OAS | ProductOpenApiConfiguration/已有bearerAuth/原wrapper | group模型已有扩展模式 | 管理/匿名6方法同source生成 | 无Springfox/OpenAPI新依赖 | 4; MC-DEP-001 |
| MQ delivery | SpringAMQP/Boot配置+Common样例 | RabbitMqConfig、RabbitMqMessageServiceImpl、GoodsPOM | brokerAPI已声明，sample缺confirmedCDC链 | 独立Template/Factory、有限retry、manualACK | 无deps；新桥/消费者 | 7; MC-TEST-001 |
| CDC | PG logical decoding vsappOutbox | 外部Server已由用户批准；Outbox guard single PRIMARY而宿主3 | 需要捕获directSQL，应用无nativeCDC | 外部pgoutputServer，WAL持久offset | 独立runtime已批准；无Kafka应用deps | 8; MC-DEP-001 |
| Generation | Egon offline FreeMarker CLI | CodegenCommand.emit、LayoutStrategy、CREATE-only parser、launcher要求CP | supportedLight；时间字段初始模板需业务定制 | 三段选择新artifact，v2review/apply，不覆盖已改类 | 现tool2.3.35；无tool/framework更新 | 2/3/4; MC-REUSE-001/MC-SCOPE-001 |

### 4.8 User-mandated Java Rule Implementation Matrix

| Literal rule | Spec source | Repository evidence | Exact files and order | Pseudocode obligations | Validation gate | Steps | Status/blocker |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Rule 1 | primary §6.2/10/13/15 | 复用ledger具体源码与当前lombok.config/POM | §7每file必填enforcement；tests先于owning实现 | 角色后缀；无Data/Info/Param镜像 | §8focused+final offline/architecture gate | 1–9（按每file角色适用） | PASS |
| Rule 2 | primary §6.2/10/13/15 | 复用ledger具体源码与当前lombok.config/POM | §7每file必填enforcement；tests先于owning实现 | 每层Jakarta/groups/@Valid/@Validated，无法代理时同约束ValidationUtils；无telephone场景 | §8focused+final offline/architecture gate | 1–9（按每file角色适用） | PASS |
| Rule 3 | primary §6.2/10/13/15 | 复用ledger具体源码与当前lombok.config/POM | §7每file必填enforcement；tests先于owning实现 | 普通class Data/NoArgs/AllArgs/Accessors+Builder/SuperBuilder，继承callSuper；MapStruct+BaseConverter/Forward；不滥用record | §8focused+final offline/architecture gate | 1–9（按每file角色适用） | PASS |
| Rule 4 | primary §6.2/10/13/15 | 复用ledger具体源码与当前lombok.config/POM | §7每file必填enforcement；tests先于owning实现 | 业务Slf4j/具名Bean/final Qualifier/RequiredArgsConstructor，lombok复制构造参数 | §8focused+final offline/architecture gate | 1–9（按每file角色适用） | PASS |
| Rule 5 | primary §6.2/10/13/15 | 复用ledger具体源码与当前lombok.config/POM | §7每file必填enforcement；tests先于owning实现 | 只JDK/现ApacheCommons/Guava；无新Utils、Tika/BeanUtils映射 | §8focused+final offline/architecture gate | 1–9（按每file角色适用） | PASS |
| Rule 6 | primary §6.2/10/13/15 | 复用ledger具体源码与当前lombok.config/POM | §7每file必填enforcement；tests先于owning实现 | BootJackson，EnumValue/JsonValue，严格unknown/scalar/ID/wire | §8focused+final offline/architecture gate | 1–9（按每file角色适用） | PASS |
| Rule 7 | primary §6.2/10/13/15 | 复用ledger具体源码与当前lombok.config/POM | §7每file必填enforcement；tests先于owning实现 | 所有base/dev/test/prod/import同corekeys、值可不同 | §8focused+final offline/architecture gate | 1–9（按每file角色适用） | PASS |
| Rule 9 | primary §6.2/10/13/15 | 复用ledger具体源码与当前lombok.config/POM | §7每file必填enforcement；tests先于owning实现 | Complex真实Strategy/Specification注册、分支失败测试，不反射/type switch | §8focused+final offline/architecture gate | 1–9（按每file角色适用） | PASS |
| Rule 10 | primary §6.2/10/13/15 | 复用ledger具体源码与当前lombok.config/POM | §7每file必填enforcement；tests先于owning实现 | Instant/UTC微秒、deletedAt旧LocalDateTime、Clock/Duration，无util.Date | §8focused+final offline/architecture gate | 1–9（按每file角色适用） | PASS |
| Rule 11 | primary §6.2/10/13/15 | 复用ledger具体源码与当前lombok.config/POM | §7每file必填enforcement；tests先于owning实现 | 精确nativeLight/MP受管DDL/单版本manifest/业务UK无虚构；Event实际Rabbit传播 | §8focused+final offline/architecture gate | Every Step | PASS |

无放松原始十条规则：本Plan各file说明实际适用项，约束注解/接口不伪加Lombok或Spring stereotype；零fieldinterface与rootcallSuper遵守技能的编译例外。非Java运维文件Rule11保护scope，Rule7/10只在该文件涉及配置/时间时应用；没有新增电话处理。

## 5. Change File Tree

下表是完整树，每个精确path只列一次；同path所有owner都列出。GENERATED包括唯一初始catalog apply，后续业务定制由同/后继Step明确指出；`RENAME`目标与暂态源各有一行，暂态项最终删除而不提交额外types。执行工具的`.egon/codegen`属于既有本地所有权记录，不提交build/state/lock/temp outputs；绝不删除其历史绕冲突。

| Operation | Path | Current evidence/symbol | Final symbols/state | Responsibility | Step | Requirements | Validation owner |
| --- | --- | --- | --- | --- | --- | --- | --- |
| CREATE/MODIFY | `enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/config/NativeReadwriteContractTest.java` | 当前ProductContractConformanceTest及lombok.copyableAnnotations | allApprovedFilesPresent; qualifiedConstructorBeans; noForbiddenImports; noTemporaryTypes | 最终架构/工具/注入/生成定制/配置测试 | Step 1,Step 2,Step 5,Step 7,Step 9 | REQ-001,REQ-002,REQ-003,REQ-004,REQ-005,REQ-006,REQ-007,REQ-008,REQ-009,REQ-010,REQ-011,REQ-012 | owningStepfocused+§8 |
| CREATE/MODIFY | `scripts/work/2026-10-10-16-13-postgresql-cdc-advertisement-implementation-verify.sh` | Step1scratch验证入口；Step2 `--schema` 未列owner | 暂态生成/工作路径，当期收敛或Step9归位后删除 | 完成拓扑、迁移/manifest/CREATE-only输入和部署静态门禁及缺环境拒发布自测 | Step 1,Step 2,Step 8 | REQ-001,REQ-002,REQ-003,REQ-005,REQ-008,REQ-009,REQ-010,REQ-011 | owningStepfocused+§8 |
| CREATE/MODIFY | `enjoyshop-service/enjoyshop-service-goods/src/main/resources/egon-ss-readwrite.yml` | enjoyshop-service/enjoyshop-service-goods/src/main/resources/egon-ss-readwrite.yml由Step1形成 | master_data.public.ad | 新增唯一SINGLE ad规则 | Step 1,Step 2 | REQ-001,REQ-002,REQ-004,REQ-007,REQ-010,REQ-011 | owningStepfocused+§8 |
| MODIFY | `enjoyshop-service/enjoyshop-service-goods/src/main/resources/egon-mybatis-plus-sharding.yml` | enjoyshop-service/enjoyshop-service-goods/src/main/resources/egon-mybatis-plus-sharding.yml原Spu/Sku named政策 | lifecycle.statement-policies AdDAO names | 追加ad生命周期语句策略并保留旧条目 | Step 1,Step 3 | REQ-001,REQ-002,REQ-004,REQ-007,REQ-010,REQ-011 | owningStepfocused+§8 |
| MODIFY | `enjoyshop-service/enjoyshop-service-goods/src/main/resources/application.yml` | 当前integrationrabbitmqdisabled及Boot AMQP配置；Spec15 | app.cdc.*; spring.rabbitmq.publisher-confirm-type/publisher-returns | 同键启用CDC配置面但默认关闭外部监听 | Step 1,Step 3,Step 4,Step 5,Step 7 | REQ-001,REQ-002,REQ-003,REQ-004,REQ-005,REQ-006,REQ-007,REQ-008,REQ-009,REQ-010,REQ-011 | owningStepfocused+§8 |
| MODIFY | `enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-dev.yml` | 当前integrationrabbitmqdisabled及Boot AMQP配置；Spec15 | app.cdc.*; spring.rabbitmq.publisher-confirm-type/publisher-returns | 同键启用CDC配置面但默认关闭外部监听 | Step 1,Step 3,Step 4,Step 5,Step 7 | REQ-001,REQ-002,REQ-003,REQ-004,REQ-005,REQ-006,REQ-007,REQ-008,REQ-009,REQ-010,REQ-011 | owningStepfocused+§8 |
| MODIFY | `enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-test.yml` | 当前integrationrabbitmqdisabled及Boot AMQP配置；Spec15 | app.cdc.*; spring.rabbitmq.publisher-confirm-type/publisher-returns | 同键启用CDC配置面但默认关闭外部监听 | Step 1,Step 3,Step 4,Step 5,Step 7 | REQ-001,REQ-002,REQ-003,REQ-004,REQ-005,REQ-006,REQ-007,REQ-008,REQ-009,REQ-010,REQ-011 | owningStepfocused+§8 |
| MODIFY | `enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-prod.yml` | 当前integrationrabbitmqdisabled及Boot AMQP配置；Spec15 | app.cdc.*; spring.rabbitmq.publisher-confirm-type/publisher-returns | 同键启用CDC配置面但默认关闭外部监听 | Step 1,Step 3,Step 4,Step 5,Step 7 | REQ-001,REQ-002,REQ-003,REQ-004,REQ-005,REQ-006,REQ-007,REQ-008,REQ-009,REQ-010,REQ-011 | owningStepfocused+§8 |
| MODIFY | `enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/start/config/ProfileKeyParityTest.java` | Existing GOVERNANCE_KEYS and catalog_tables_are_single_master_data_in_every_profile assertions still require STRATEGY tables.brand/category/template/spec/para/category_brand | Native profile keys and reference to classpath:egon-ss-readwrite.yml; existing identity/CORS/S3/Jianshen assertions preserved | Retarget old profile routing checks to approved native contract | Step 1 | REQ-002,REQ-011 | NativeReadwriteContractTest/ProfileKeyParityTest |
| MODIFY | `enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/architecture/LightPersistenceArchitectureTest.java` | enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/architecture/LightPersistenceArchitectureTest.java Step2 | DomainServiceImpl count9->10 | 确认新增Service实现仅组合Repository | Step 1,Step 2,Step 3 | REQ-001,REQ-002,REQ-004,REQ-007,REQ-010,REQ-011 | owningStepfocused+§8 |
| CREATE/MODIFY | `enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/ad/AdPersistenceTest.java` | enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/ad/AdPersistenceTest.java Step2 | tenantPredicates; concurrentVersionContract; terminalDeleteContract | 为命名SQL/CAS/tenant隔离添加用例 | Step 2,Step 3 | REQ-002,REQ-004,REQ-007,REQ-011 | owningStepfocused+§8 |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/resources/db/egon-mp/V20261010_001__initialize_advertisement_cdc.sql` | 现latest V20261009_001与role guard；Spec §11.2 | CREATE TABLE public.ad; 3 indexes; ALTER REPLICA IDENTITY | 一个新受管版本创建ad并补CDC旧行信息 | Step 2 | REQ-002,REQ-004,REQ-007,REQ-011 | owningStepfocused+§8 |
| MODIFY | `enjoyshop-service/enjoyshop-service-goods/src/main/resources/db/egon-mp/repository-manifest.json` | enjoyshop-service/enjoyshop-service-goods/src/main/resources/db/egon-mp/repository-manifest.json四条已应用prefix | scripts[last].version/path/sha256 | 追加新脚本SHA256条目，不改旧记录 | Step 2 | REQ-002,REQ-004,REQ-007,REQ-011 | owningStepfocused+§8 |
| CREATE | `scripts/work/2026-10-10-16-13-postgresql-cdc-advertisement-implementation-prepare-ad.sh` | E/scripts/egon-codegen.sh及PostgreDdlAdapter拒绝ALTER；Spec §8/11 | 暂态生成/工作路径，当期收敛或Step9归位后删除 | 生成CREATE-only输入及核对v2 generator范围 | Step 2 | REQ-002,REQ-004,REQ-007,REQ-011 | owningStepfocused+§8 |
| CREATE/MODIFY | `enjoyshop-service/enjoyshop-service-goods/codegen/ad-codegen.json` | enjoyshop-service/enjoyshop-service-goods/codegen/ad-codegen.json Step3与LightLayout adapter.domain默认包 | artifacts:[controller] | 只生成尚不存在的Controller，不重放application artifact | Step 2,Step 3,Step 4 | REQ-002,REQ-004,REQ-005,REQ-006,REQ-007,REQ-011 | owningStepfocused+§8 |
| GENERATED | `enjoyshop-service/enjoyshop-service-goods/codegen/ad-create.sql` | 新20261010_001；Spec明确CREATE-only视图 | ad CREATE and indexes; extraction provenance | CREATE-only视图保留与源迁移相同ad定义 | Step 2 | REQ-002,REQ-004,REQ-007,REQ-011 | owningStepfocused+§8 |
| GENERATED/MODIFY | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/ad/po/AdPO.java` | AdPO Step2与EgonModel/MP validation组 | status AdStatusEnum; Instant startTime/endTime; 8 business fields | 把PO业务码接入enum且保留DB形状约束 | Step 2,Step 3 | REQ-002,REQ-004,REQ-007,REQ-011 | owningStepfocused+§8 |
| GENERATED/MODIFY | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/ad/dao/AdDAO.java` | enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/dao/SpuDAO.java与EgonColaMapper | selectByLifecycle; lockByLifecycle; selectPage; countPage; selectCandidates; deleteVersionedById | 只提供明确tenant/lifecycle查询与版本删除 | Step 2,Step 3 | REQ-002,REQ-004,REQ-007,REQ-011 | owningStepfocused+§8 |
| GENERATED/MODIFY | `enjoyshop-service/enjoyshop-service-goods/src/main/resources/mybatis/mapper/ad/AdDAO.xml` | enjoyshop-service/enjoyshop-service-goods/src/main/resources/mybatis/mapper/goods/SpuDAO.xml及Step2 generatedXML；Common契约要求三个标准statement | same namespace=AdDAO; complete resultMap; selectActiveById/selectActiveByIds; explicit lifecycle/page/candidate/CAS SQL | 约束查询、标准Common active reads与CAS删除SQL | Step 2,Step 3,Step 4 | REQ-002,REQ-004,REQ-005,REQ-007,REQ-011 | owningStepfocused+§8 |
| GENERATED/MODIFY | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/ad/repo/AdRepository.java` | Step3 Repository；existing EgonColaCachePort & Common tenantKeyGuard；test profile cache.enabled=false | cachedCandidates; loadCandidates; registerAdvertisementEviction; evictCandidates; optional ObjectProvider<EgonColaCachePort> | Repository拥有cache及PRIMARY miss loader，不循环注入Domain；组件缺席/缓存读取异常时使用有界PRIMARY loader | Step 2,Step 3,Step 5 | REQ-002,REQ-004,REQ-006,REQ-007,REQ-011 | owningStepfocused+§8 |
| GENERATED/MODIFY | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/ad/AdBO.java` | enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/SpuBO.java及用户批准Domain Jakarta修正 | AdBO id/version/8business/tenant/audit/deletedAt; ValidAdWindow Create/Update/PublicRead | 补足业务约束、受信上下文字段和缓存候选窗口校验 | Step 2,Step 3,Step 5 | REQ-002,REQ-004,REQ-006,REQ-007,REQ-011 | owningStepfocused+§8 |
| GENERATED/MODIFY | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/ad/AdDomainQuery.java` | enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/ProductDomainQuery.java及生成Step2 | AdDomainQuery id/tenantId/position/status/page/limit; PublicRead position required | 领域查询条件避免Spring分页依赖 | Step 2,Step 3,Step 5 | REQ-002,REQ-004,REQ-005,REQ-006,REQ-007,REQ-011 | owningStepfocused+§8 |
| MODIFY | `docs/egon/codegen/ddl-consumption-log.md` | 前两个apply日志 append-only | append stage3 Controller generator record | 追加Controller catalog apply与移包journal | Step 2,Step 3,Step 4 | REQ-002,REQ-004,REQ-005,REQ-006,REQ-007,REQ-011 | owningStepfocused+§8 |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/application/ad/AdManageTest.java` | enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/application/goods原Product tests及PersistenceTestSupport | rejectsUnknownOrInvalidWrite; permissionsFailClosed; versionConflict; deleteTerminalIsTrue | 管理用例和每层Validation的RED | Step 3 | REQ-004,REQ-007,REQ-011 | owningStepfocused+§8 |
| MODIFY | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/enums/GoodsErrorStatus.java` | enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/enums/GoodsErrorStatus.java原6101xx/6102xx | AD_REQUEST_INVALID610301; AD_NOT_FOUND302; AD_VERSION_CONFLICT303; AD_DEPENDENCY_UNAVAILABLE304; AD_FORBIDDEN305; AD_SITE_NOT_CONFIGURED306; AD_DATA_LIMIT307; AD_AUTHORIZATION_UNAVAILABLE308; AD_ID_UNAVAILABLE309; AD_DATA_INVALID310; AD_INTERNAL_ERROR399 | 追加稳定广告错误码，不改原枚举 | Step 3 | REQ-004,REQ-007,REQ-011 | owningStepfocused+§8 |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/validation/AdValidationGroup.java` | enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/validation/ProductValidationGroup.java及已批准Spec修正 | Write extends Default; Create/Update extends Write; Delete/Detail/Page/PublicRead | 为管理/查询/消息共用对象定义分组 | Step 3 | REQ-004,REQ-007,REQ-011 | owningStepfocused+§8 |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/ad/AdStatusEnum.java` | enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods现enum及Spec §10 | DISABLED("0"); ENABLED("1"); fromValue | 统一启用码与JSON/数据库枚举 | Step 3 | REQ-004,REQ-007,REQ-011 | owningStepfocused+§8 |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/validation/ValidAdWindow.java` | enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/validation/Product约束模式 | ValidAdWindow; validatedBy AdWindowValidator | 跨字段UTC窗口/微秒约束声明 | Step 3 | REQ-004,REQ-007,REQ-011 | owningStepfocused+§8 |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/validation/AdWindowValidator.java` | enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/validation/ProductImageValidator.java模式、原Jakarta/S3配置 | AdWindowValidator implements ConstraintValidator | 跨字段UTC窗口/微秒约束实现 | Step 3 | REQ-004,REQ-007,REQ-011 | owningStepfocused+§8 |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/validation/ValidAdPosition.java` | enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/validation/Product约束模式 | ValidAdPosition; validatedBy AdPositionValidator | 广告位语法和启动登记值校验声明 | Step 3 | REQ-004,REQ-007,REQ-011 | owningStepfocused+§8 |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/validation/AdPositionValidator.java` | enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/validation/ProductImageValidator.java模式、原Jakarta/S3配置 | AdPositionValidator implements ConstraintValidator | 广告位语法和启动登记值校验实现 | Step 3 | REQ-004,REQ-007,REQ-011 | owningStepfocused+§8 |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/validation/ValidAdUrl.java` | enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/validation/Product约束模式 | ValidAdUrl; validatedBy AdUrlValidator | 复用S3公开base约束及点击URL边界声明 | Step 3 | REQ-004,REQ-007,REQ-011 | owningStepfocused+§8 |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/validation/AdUrlValidator.java` | enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/validation/ProductImageValidator.java模式、原Jakarta/S3配置 | AdUrlValidator implements ConstraintValidator | 复用S3公开base约束及点击URL边界实现 | Step 3 | REQ-004,REQ-007,REQ-011 | owningStepfocused+§8 |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/config/AdProperties.java` | enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/config现Properties模式；Spec15.1 | app.ad; publicEnabled; publicSites; allowedPositions; candidateLimit; publicLimit; loadTimeout | 可校验的广告参数容器供约束/后续public读取使用 | Step 3 | REQ-004,REQ-007,REQ-011 | owningStepfocused+§8 |
| CREATE/MODIFY | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/config/AdConfiguration.java` | Step3 config及Common primary transactional routing | adPrimaryReadTransactionTemplate | 在既有事务管理器上创建主读Template | Step 3,Step 5 | REQ-004,REQ-006,REQ-007,REQ-011 | owningStepfocused+§8 |
| GENERATED | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/ad/CreateAdCommand.java` | CodegenCommand.emit与GenerationScopeValidator固定Create/Update/Delete/Detail及converter包 | 暂态生成/工作路径，当期收敛或Step9归位后删除 | 先由catalog生成暂态基线，再收敛到已批准对象，不保留额外业务类型 | Step 3 | REQ-004,REQ-007,REQ-011 | owningStepfocused+§8 |
| GENERATED | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/ad/UpdateAdCommand.java` | CodegenCommand.emit与GenerationScopeValidator固定Create/Update/Delete/Detail及converter包 | 暂态生成/工作路径，当期收敛或Step9归位后删除 | 先由catalog生成暂态基线，再收敛到已批准对象，不保留额外业务类型 | Step 3 | REQ-004,REQ-007,REQ-011 | owningStepfocused+§8 |
| GENERATED | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/ad/DeleteAdCommand.java` | CodegenCommand.emit与GenerationScopeValidator固定Create/Update/Delete/Detail及converter包 | 暂态生成/工作路径，当期收敛或Step9归位后删除 | 先由catalog生成暂态基线，再收敛到已批准对象，不保留额外业务类型 | Step 3 | REQ-004,REQ-007,REQ-011 | owningStepfocused+§8 |
| GENERATED | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/ad/AdDetailQuery.java` | CodegenCommand.emit与GenerationScopeValidator固定Create/Update/Delete/Detail及converter包 | 暂态生成/工作路径，当期收敛或Step9归位后删除 | 先由catalog生成暂态基线，再收敛到已批准对象，不保留额外业务类型 | Step 3 | REQ-004,REQ-007,REQ-011 | owningStepfocused+§8 |
| GENERATED | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/ad/converter/CreateAdCommandConverter.java` | CodegenCommand.emit与GenerationScopeValidator固定Create/Update/Delete/Detail及converter包 | 暂态生成/工作路径，当期收敛或Step9归位后删除 | 先由catalog生成暂态基线，再收敛到已批准对象，不保留额外业务类型 | Step 3 | REQ-004,REQ-007,REQ-011 | owningStepfocused+§8 |
| GENERATED | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/ad/converter/UpdateAdCommandConverter.java` | CodegenCommand.emit与GenerationScopeValidator固定Create/Update/Delete/Detail及converter包 | 暂态生成/工作路径，当期收敛或Step9归位后删除 | 先由catalog生成暂态基线，再收敛到已批准对象，不保留额外业务类型 | Step 3 | REQ-004,REQ-007,REQ-011 | owningStepfocused+§8 |
| GENERATED | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/ad/converter/DeleteAdCommandConverter.java` | CodegenCommand.emit与GenerationScopeValidator固定Create/Update/Delete/Detail及converter包 | 暂态生成/工作路径，当期收敛或Step9归位后删除 | 先由catalog生成暂态基线，再收敛到已批准对象，不保留额外业务类型 | Step 3 | REQ-004,REQ-007,REQ-011 | owningStepfocused+§8 |
| GENERATED | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/ad/converter/AdResultConverter.java` | CodegenCommand.emit与GenerationScopeValidator固定Create/Update/Delete/Detail及converter包 | 暂态生成/工作路径，当期收敛或Step9归位后删除 | 先由catalog生成暂态基线，再收敛到已批准对象，不保留额外业务类型 | Step 3 | REQ-004,REQ-007,REQ-011 | owningStepfocused+§8 |
| RENAME/MODIFY | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/ad/AdSaveCommand.java` | stage2 catalog输出与Spec §9/10固定字段 | AdSaveCommand; Step 4 JSON setter trims name before Create/Update validation | 收敛AdSaveCommand到批准管理合同并在HTTP绑定期规范名称 | Step 3,Step 4 | REQ-004,REQ-005,REQ-006,REQ-007,REQ-011 | owningStepfocused+§8 |
| RENAME | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/ad/AdLifecycleCommand.java` | stage2 catalog输出与Spec §9/10固定字段 | AdLifecycleCommand | 收敛AdLifecycleCommand到批准管理合同 | Step 3 | REQ-004,REQ-007,REQ-011 | owningStepfocused+§8 |
| RENAME | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/ad/AdReadQuery.java` | stage2 catalog输出与Spec §9/10固定字段 | AdReadQuery | 收敛AdReadQuery到批准管理合同 | Step 3 | REQ-004,REQ-007,REQ-011 | owningStepfocused+§8 |
| GENERATED | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/ad/AdPageQuery.java` | stage2 catalog输出与Spec §9/10固定字段 | AdPageQuery | 收敛AdPageQuery到批准管理合同 | Step 3 | REQ-004,REQ-007,REQ-011 | owningStepfocused+§8 |
| GENERATED | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/ad/AdResult.java` | stage2 catalog输出与Spec §9/10固定字段 | AdResult | 收敛AdResult到批准管理合同 | Step 3 | REQ-004,REQ-007,REQ-011 | owningStepfocused+§8 |
| RENAME | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/ad/AdApplicationConverter.java` | stage2 catalog输出与Spec §9/10固定字段 | AdApplicationConverter extends BaseForwardConverter<AdSaveCommand,AdBO> | 收敛AdApplicationConverter extends BaseForwardConverter<AdSaveCommand,AdBO>到批准管理合同 | Step 3 | REQ-004,REQ-007,REQ-011 | owningStepfocused+§8 |
| RENAME | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/ad/AdResultConverter.java` | stage2 catalog输出与Spec §9/10固定字段 | AdResultConverter extends BaseForwardConverter<AdBO,AdResult> | 收敛AdResultConverter extends BaseForwardConverter<AdBO,AdResult>到批准管理合同 | Step 3 | REQ-004,REQ-007,REQ-011 | owningStepfocused+§8 |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/ad/AdPublicResult.java` | Spec §9 API006及§10投影；原Product Result建模 | AdPublicResult id/name/imageUrl/targetUrl | 公开投影作为Manage签名依赖，HTTP在Step4才出现 | Step 3 | REQ-004,REQ-007,REQ-011 | owningStepfocused+§8 |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/ad/AdPublicResultConverter.java` | 现ProductSpuResultConverter/BaseForwardConverter与Spec10.4 | BaseForwardConverter<AdBO,AdPublicResult> | 可逆信息丢失明确的公开MapStruct投影 | Step 3 | REQ-004,REQ-007,REQ-011 | owningStepfocused+§8 |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/ad/AdEligibilitySpecification.java` | enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/ProductLifecycleComponent.java及Spec13.1 | boolean isSatisfiedBy(AdBO ad, Instant now) | 按传入同一次时钟筛资格 | Step 3 | REQ-004,REQ-007,REQ-011 | owningStepfocused+§8 |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/ad/AdAuthorizationComponent.java` | enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/ProductAuthorizationComponent.java既有严格context/version校验 | requirePermission(String permission) | 单独验证广告USER/RBAC上下文，不借商品permission regex | Step 3 | REQ-004,REQ-007,REQ-011 | owningStepfocused+§8 |
| GENERATED | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/ad/AdDomainService.java` | stage2 catalog AdDomainService及当前Product Light链 | AdDomainService | 由catalog初始生成并实现批准管理语义 | Step 3 | REQ-004,REQ-007,REQ-011 | owningStepfocused+§8 |
| GENERATED | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/ad/converter/AdPersistenceConverter.java` | 本轮只读 CLI plan 73e3cdf98173df5d2c525e98a67761e16b7b783a40641814d2645f05d245dd16 实际输出 infrastructure/ad/converter/ | AdPersistenceConverter extends BaseConverter<AdPO,AdBO> | 复用生成双向映射并补明确时间/枚举/继承审计 | Step 3 | REQ-004,REQ-007,REQ-011 | owningStepfocused+§8 |
| GENERATED/MODIFY | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/ad/service/impl/AdDomainServiceImpl.java` | Step3 domainimpl及现SPI afterCommit contract | publishedCandidates; create; replace; delete | 移除public cache外层事务，并把write失效登记放同业务事务 | Step 3,Step 5 | REQ-004,REQ-006,REQ-007,REQ-011 | owningStepfocused+§8 |
| GENERATED | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/ad/AdManage.java` | stage2 catalog AdManage及当前Product Light链 | AdManage | 由catalog初始生成并实现批准管理语义 | Step 3 | REQ-004,REQ-007,REQ-011 | owningStepfocused+§8 |
| GENERATED/MODIFY | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/ad/AdManageImpl.java` | Step3published implementeddirectPRIMARYread | published(AdReadQuery) | 保持一次Clock和public投影，防止外层事务绕cache | Step 3,Step 5 | REQ-004,REQ-006,REQ-007,REQ-011 | owningStepfocused+§8 |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/goods/AdControllerTest.java` | enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/goods/ProductControllerTest.java、ProductOpenApiContractTest及ProductTestSecurityConfiguration | AdControllerTest | 测试先锁HTTP/安全/文档行为 | Step 4 | REQ-004,REQ-005,REQ-006,REQ-011 | owningStepfocused+§8 |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/start/config/AdSecurityContractTest.java` | enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/goods/ProductControllerTest.java、ProductOpenApiContractTest及ProductTestSecurityConfiguration | AdSecurityContractTest | 测试先锁HTTP/安全/文档行为 | Step 4 | REQ-004,REQ-005,REQ-006,REQ-011 | owningStepfocused+§8 |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/goods/AdOpenApiContractTest.java` | enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/goods/ProductControllerTest.java、ProductOpenApiContractTest及ProductTestSecurityConfiguration | AdOpenApiContractTest | 测试先锁HTTP/安全/文档行为 | Step 4 | REQ-004,REQ-005,REQ-006,REQ-011 | owningStepfocused+§8 |
| GENERATED | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/ad/AdController.java` | LightLayout.packageName controller=base.adapter.ad，而BaseExceptionHandler仅adapter.goods | 暂态生成/工作路径，当期收敛或Step9归位后删除 | Controller初始模板来源，不留下未受ad advice覆盖的endpoint | Step 4 | REQ-004,REQ-005,REQ-006,REQ-011 | owningStepfocused+§8 |
| RENAME | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/goods/AdController.java` | Spec9固定ID；原ProductController/BrandController与范围advice | listAdvertisements,getAdvertisement,createAdvertisement,replaceAdvertisement,deleteAdvertisement,listPublishedAdvertisements | 精确6HTTP映射和typedgroups/结果 | Step 4 | REQ-004,REQ-005,REQ-006,REQ-011 | owningStepfocused+§8 |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/filter/AdPublicTenantFilter.java` | TenantContextFilter范围过滤+原MDC/MP tenant provider | shouldNotFilter; doFilterInternal | 只公开GET建立受控站点租户scope | Step 4 | REQ-004,REQ-005,REQ-006,REQ-011 | owningStepfocused+§8 |
| MODIFY | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/filter/TenantContextFilter.java` | enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/filter/TenantContextFilter.java现products skip | shouldNotFilter(HttpServletRequest) | 新ad路径完全跳过旧Header默认tenant | Step 4 | REQ-004,REQ-005,REQ-006,REQ-011 | owningStepfocused+§8 |
| MODIFY | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/start/StudentManagementApplication.java` | 当前显式@MapperScan只覆盖user/teaching/goods DAO package | infrastructure.ad.dao | 追加广告DAO包以注册AdDAO | Step 4 | REQ-004,REQ-005,REQ-006,REQ-011 | AdOpenApiContractTest/owningStepfocused+§8 |
| MODIFY | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/start/config/NativeHttpSecurityConfiguration.java` | existing product/file/application chains以及IDP filter顺序 | adSecurityFilterChain; publicAdSecurityFilterChain | 独立管理链和精确公开GET拒绝其他method | Step 4 | REQ-004,REQ-005,REQ-006,REQ-011 | owningStepfocused+§8 |
| MODIFY | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/handler/BaseExceptionHandler.java` | 现Product/Brand/File scopedadvice及FieldViolationVO(field,constraint,message) | adPath; requestErrorStatus; mapBusinessStatus | adPath专属错误映射而保持原wire | Step 4 | REQ-004,REQ-005,REQ-006,REQ-011 | owningStepfocused+§8 |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/goods/AdOpenApiConfiguration.java` | 同包ProductOpenApiConfiguration处理groups源schema与现springdocstarter | ads group; existing bearerAuth reuse; operation customizer | group和Create/Update请求schema投影 | Step 4 | REQ-004,REQ-005,REQ-006,REQ-011 | owningStepfocused+§8 |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/ad/AdCacheContractTest.java` | EgonColaTwoLevelCache.get/evict及EgonColaTwoLevelCachePort registerEvictionAfterCommit源码 | cacheHitBypassesLoader; primaryMiss; utcWindow; invalidatesOldAndNew; swallowedEvictionFailure | 锁定候选缓存而非最终时间列表，以及真实Cache SPI局限 | Step 5 | REQ-004,REQ-006,REQ-007,REQ-011 | owningStepfocused+§8 |
| CREATE/MODIFY | `enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/sync/CdcContractTest.java` | SpecTEST019–022与原deployment/k8soffline经验 | deploymentConfigPreservesBeforeAfter; inputGatesAreExplicit | 先锁独立Server配置/运行验收文档必需项 | Step 6,Step 7,Step 8 | REQ-001,REQ-003,REQ-005,REQ-007,REQ-008,REQ-009,REQ-010,REQ-011 | owningStepfocused+§8 |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/sync/CdcOperationEnum.java` | SpecEVENT001与Jakarta/enum规则 | CREATE c; UPDATE u; DELETE d; SNAPSHOT r | 明确合法数据操作和控制消息分流 | Step 6 | REQ-003,REQ-007,REQ-008,REQ-009,REQ-011 | owningStepfocused+§8 |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/sync/CdcSourceCursorBO.java` | Spec §7 stablecanonicalidentity/§10BO；sourcemetadatajsonc | sourceAlias/sourceEpoch/schema/table/op/rowPk/lsn/txId/recordOrder/sourceTsMs | 保留精确来源游标而不以时间排序 | Step 6 | REQ-003,REQ-007,REQ-008,REQ-009,REQ-011 | owningStepfocused+§8 |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/sync/CdcRecordBO.java` | Spec10 factsBO边界与before/after union要求 | cursor; before/after row identity; factFingerprint | 只保存经验证的row归属事实，领域不持JsonNode | Step 6 | REQ-003,REQ-007,REQ-008,REQ-009,REQ-011 | owningStepfocused+§8 |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/sync/CdcChangeStrategy.java` | Spec §13.1 两种确定策略 | supports source table mapping; normalize(@Valid CdcRecordBO) | 具名Strategy合同针对真实两种目标计算 | Step 6 | REQ-003,REQ-007,REQ-008,REQ-009,REQ-011 | owningStepfocused+§8 |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/sync/AdInvalidationBO.java` | Spec10.1/AdInvalidationBO | AdInvalidationBO tenantId/cursor/positions | 本租户目标集合承载而非索引/缓存快照 | Step 6 | REQ-003,REQ-007,REQ-008,REQ-009,REQ-011 | owningStepfocused+§8 |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/sync/ProductRefreshBO.java` | Spec10.1/ProductRefreshBO | ProductRefreshBO tenantId/cursor/spuIds | 本租户目标集合承载而非索引/缓存快照 | Step 6 | REQ-003,REQ-007,REQ-008,REQ-009,REQ-011 | owningStepfocused+§8 |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/sync/AdCacheInvalidationEvent.java` | Spec9 EVENT002/003 fixedjsonc及10投影 | AdCacheInvalidationEvent eventId/schemaVersion/tenantId/sourceAlias/sourceEpoch/sourceLsn/sourceTxId/sourceRecordOrder/occurredAt/sourceTable/operation/positions | 外部v1 wire与领域BO分离 | Step 6 | REQ-003,REQ-007,REQ-008,REQ-009,REQ-011 | owningStepfocused+§8 |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/sync/ProductRefreshRequestedEvent.java` | Spec9 EVENT002/003 fixedjsonc及10投影 | ProductRefreshRequestedEvent eventId/schemaVersion/tenantId/sourceAlias/sourceEpoch/sourceLsn/sourceTxId/sourceRecordOrder/occurredAt/sourceTable/operation/spuIds | 外部v1 wire与领域BO分离 | Step 6 | REQ-003,REQ-007,REQ-008,REQ-009,REQ-011 | owningStepfocused+§8 |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/config/CdcSourceProperties.java` | Spec15.1 closedconfig；existing typedproperties | app.cdc enabled/sourceRegistry/maxPayloadBytes/confirmTimeout | 启动校验源registry和监听/投递预算 | Step 6 | REQ-003,REQ-007,REQ-008,REQ-009,REQ-011 | owningStepfocused+§8 |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/sync/AdCdcChangeStrategy.java` | Spec7/13与当前Component纯计算组合 | AdCdcChangeStrategy normalize(CdcRecordBO) | 实现确定目标算法，不查询DB | Step 6 | REQ-003,REQ-007,REQ-008,REQ-009,REQ-011 | owningStepfocused+§8 |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/sync/ProductCdcChangeStrategy.java` | Spec7/13与当前Component纯计算组合 | ProductCdcChangeStrategy normalize(CdcRecordBO) | 实现确定目标算法，不查询DB | Step 6 | REQ-003,REQ-007,REQ-008,REQ-009,REQ-011 | owningStepfocused+§8 |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/sync/CdcEventIdentityComponent.java` | Spec7.3及JDKMessageDigest/canonicalJSON项目Jackson | eventId(CdcRecordBO,Long targetTenant,String routingKey) | 稳定SHA256身份与内容指纹，不用接收时间 | Step 6 | REQ-003,REQ-007,REQ-008,REQ-009,REQ-011 | owningStepfocused+§8 |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/sync/AdInvalidationEventConverter.java` | Spec10.4 MapStruct与Product converters | AdInvalidationEventConverter extends BaseForwardConverter<AdInvalidationBO,AdCacheInvalidationEvent> | 事件稳定wire投影经Common Converter | Step 6 | REQ-003,REQ-007,REQ-008,REQ-009,REQ-011 | owningStepfocused+§8 |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/sync/ProductRefreshEventConverter.java` | Spec10.4 MapStruct与Product converters | ProductRefreshEventConverter extends BaseForwardConverter<ProductRefreshBO,ProductRefreshRequestedEvent> | 事件稳定wire投影经Common Converter | Step 6 | REQ-003,REQ-007,REQ-008,REQ-009,REQ-011 | owningStepfocused+§8 |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/config/CdcRabbitConfiguration.java` | 当前RabbitMqConfig已有factory，不改sampleconverter/routes；现CommonMPPOM AMQP | cdcRabbitTemplate; cdcRabbitListenerContainerFactory; cdcChangeStrategies; queue/exchange/binding beans | 独立具名Template/Factory/显式Strategy注册和关闭默认 | Step 7 | REQ-003,REQ-005,REQ-007,REQ-008,REQ-009,REQ-010,REQ-011 | owningStepfocused+§8 |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/sync/CdcEventPublisher.java` | Spec7/9 confirmations与既有RabbitTemplate dependency | publishConfirmed(List<? extends SyncEvent>,String routingKey) | transport确认仅在全部目标成功后返回 | Step 7 | REQ-003,REQ-005,REQ-007,REQ-008,REQ-009,REQ-010,REQ-011 | owningStepfocused+§8 |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/sync/CdcRawConsumer.java` | Spec rawEVENT001；BootJackson/RabbitListener/API scope currentadapter | onMessage(Message,Channel); rawschema/control validation | source strict解析→Strategy→confirmed publish→manualACK | Step 7 | REQ-003,REQ-005,REQ-007,REQ-008,REQ-009,REQ-010,REQ-011 | owningStepfocused+§8 |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/sync/AdCacheInvalidationConsumer.java` | SpecEVENT002；CachePort swallowing afterCommitfailures已核对 | onMessage(Message,Channel); validateInvalidateEvent | 唯一消息消费效果是租户失效尝试 | Step 7 | REQ-003,REQ-005,REQ-007,REQ-008,REQ-009,REQ-010,REQ-011 | owningStepfocused+§8 |
| CREATE | `deployment/cdc/debezium/application.properties` | Spec15.3/15.4引用官方Server3.2文档；实际正式版待releaseinputs | debezium.source.*; debezium.sink.rabbitmq.*; format.value.* | 独立Server配置模板只连PRIMARY与Rabbit | Step 8 | REQ-001,REQ-003,REQ-005,REQ-008,REQ-009,REQ-010,REQ-011 | owningStepfocused+§8 |
| CREATE | `deployment/cdc/postgres/replication-contract.md` | Spec11.4/15.2/16，当前3logicalpools | cluster->database->logicalGroup map; RPO; slots/RI/role/fencing | 物理复制/逻辑CDC/Native分工及恢复DBA合同 | Step 8 | REQ-001,REQ-003,REQ-005,REQ-008,REQ-009,REQ-010,REQ-011 | owningStepfocused+§8 |
| CREATE | `deployment/cdc/rabbit/topology-contract.md` | Spec15.4与CdcRabbitConfiguration selectednames | raw/sync exchanges; raw/ad/search queues; AE/DLX policies | 提前建持久拓扑/ACL/AE/DLQ和容量门禁 | Step 8 | REQ-001,REQ-003,REQ-005,REQ-008,REQ-009,REQ-010,REQ-011 | owningStepfocused+§8 |
| CREATE | `deployment/cdc/acceptance.md` | Spec14/16及deployment/k8s/acceptance.md既有证据分级 | TEST019–022 records; release matrix; rollback/failover proof | 将未运行的真实验收设计为用户启动后可记录证据的门禁 | Step 8 | REQ-001,REQ-003,REQ-005,REQ-008,REQ-009,REQ-010,REQ-011 | owningStepfocused+§8 |
| CREATE/MODIFY | `deployment/cdc/README.md` | Step8README | verificationcommands; phasecompletion | 更新归位验证命令和最终证据边界 | Step 8,Step 9 | REQ-001,REQ-002,REQ-003,REQ-004,REQ-005,REQ-006,REQ-007,REQ-008,REQ-009,REQ-010,REQ-011,REQ-012 | owningStepfocused+§8 |
| RENAME | `deployment/cdc/check.sh` | scripts/work/2026-10-10-16-13-postgresql-cdc-advertisement-implementation-verify.sh全Step累计，技能rule22归属要求 | offline --self-test/--topology/--schema/--standards/--deployment/--final | 将守护本子系统的临时校验入口归位 | Step 9 | REQ-001,REQ-002,REQ-003,REQ-004,REQ-005,REQ-006,REQ-007,REQ-008,REQ-009,REQ-010,REQ-011,REQ-012 | owningStepfocused+§8 |
| RENAME | `enjoyshop-service/enjoyshop-service-goods/codegen/prepare-ad.sh` | scripts/work/2026-10-10-16-13-postgresql-cdc-advertisement-implementation-prepare-ad.sh已实现 | extract-create; verify-generator-scope | 将CREATE-only可复核提取器归到所属业务codegen目录 | Step 9 | REQ-001,REQ-002,REQ-003,REQ-004,REQ-005,REQ-006,REQ-007,REQ-008,REQ-009,REQ-010,REQ-011,REQ-012 | owningStepfocused+§8 |

没有DELETE当前用户/教学/GraphQL/MQ样例；没有修改原Flyway/MP旧SQL。文件总量包含有依赖的多次业务编辑和生成器暂态对象，不代表新增同等数量的运行类型。数据库只有ad一个新业务表和一个新version。

## 6. Prerequisites, Constraints, and Plan Clarifications

### 6.1 Repository and worktree baseline

遵守用户提供AGENTS全文：最小安全改动、每逻辑任务最多一个commit、旧migration不可改、不得自动启动服务或GUI。当前repo未发现额外适用AGENTS；外部E只读源码/POM/template证据，不修改其component/platform/archetype。当前HEAD7e6a506，primary Spec未跟踪；本轮Plan和授权Specmetadata不作为未授权代码提交。

执行每Step前`git status --short`、`git diff --cached --name-only`、`git rev-parse HEAD`；若别人已改目标path，保留并比较，不能reset/stash/checkout/clean他人数据。stage只该Step§7精确paths；历史SQL、无关docs、`.egon`、target、env、secret、IDE文件均不纳入。提交后记录新HEAD作为下Step基线；不空提交。

### 6.2 Build, test, and environment prerequisites

| Concern | Exact command/source | Required state | Validation boundary |
| --- | --- | --- | --- |
| Maven/JDK | root pom.xml二级aggregator；M/POM Java21/Bootparent5.4.1、MapStruct1.6.3/Lombok1.18.46 | JDK21+既有mvn；默认tests无需externalservices | 本轮未执行Maven |
| Focused RED/GREEN | `mvn -pl enjoyshop-service/enjoyshop-service-goods -am -Dtest=SELECTORS -Dsurefire.failIfNoSpecifiedTests=false test`；每Step实际SELECTORS见§7 | 用existingJUnit/MockMvc/harness；missingtypes用compile prerequisites或资源反射RED | 不启动Spring应用/PG/Redis/Rabbit/Testcontainers |
| Generator | E/scripts/egon-codegen.sh +CodegenCommand +catalogFreeMarker2.3.35 | 当前E/target与实际Maven localRepository=/Users/mario/maven/repository已有generator5.4.1 jar；执行者提供EGON_CODEGEN_CLASSPATH含该tool与既有runtimeclasspath；launcher先查路径；不下载/安装依赖 | 只offlineplan/apply，另行Plan审核后才执行 |
| Generator missing tooling | `test -x /Users/mario/SelfProject/Egon-COLA/scripts/egon-codegen.sh`、检查已给classpath每entry | 没Java/CP/jar返回BLOCKED_TOOLING exit7；停止对应生成，不手写替代 | 实施前置，不声称已probe完整runtimeCP |
| Profile check | NativeReadwriteContractTest +harness --topology/--self-test | 只资源/objects解析，不创建physicalconnection | static不是主从真实路由证明 |
| Runtime acceptance | deployment/cdc/acceptance.md TEST019–022 | 用户准备/启动正式env、提供version/digest/roles/sites/TLS/PVC/capacity | 当前未运行；不是默认Maven前置 |

### 6.3 Immutable constraints and approved decisions

primary§3/5/11/16全约束有效：只NATIVE不改组件、physicalreplication单独owner、PRIMARYCDC与强读、无Kafka/Outbox/searchengine、可信Host公开tenant、管理USER/RBAC、源及schema严格、有界重试/DLQ、缓存失效尝试/TTL非强一致。ad多个同名/position多条无naturalUK；spu/sku原business+tenant+deletedAt和activeNULL唯一键原样保持。默认public/CDC关闭且离线templates不会自动安装/上线。

### 6.4 Plan Clarifications

| ID | Small implementation inference | Repository evidence | Why semantics are unchanged | Impact if wrong |
| --- | --- | --- | --- | --- |
| PLAN-CLAR-001 | 初始generator固定Create/Update/Delete/Detail与converter子包，分段apply后重命名/合并到Spec AdSave/AdLifecycle/AdRead/flatconverter；unused新暂态当期删 | CodegenCommand.emit/LayoutStrategy/GenerationScopeValidator | public6API/role/fields/Validation/指定最终path不变；不改组件/模板 | outputinventory有差异先停生成，不覆盖目标 |
| PLAN-CLAR-002 | Controller初始adapter.ad移至approvedadapter.goods | LightLayout固定domain；BaseExceptionHandler仅adapter.goods范围 | 相同Controller/wire/依赖层级，维持Spec既定advice覆盖 | handler/OAS tests不通过则回此file |
| PLAN-CLAR-003 | CREATE-only view放M/codegen/ad-create.sql；新SQL原文/hash作为provenance，journal actual throughVersion:none | parser拒ALTER；技能journal按实际classpath输入 | 一份managedmigration不变，view非第二migration；工具记录真实输入 | 抽取/16字段hash不一致停止apply |
| PLAN-CLAR-004 | 实际NATIVE property为native-rules-resource、regions.AdCandidates是现配置键 | EgonColaShardingProperties/EgonColaCacheProperties | 实现Spec已经指定能力，无自造属性 | binding/keyparity失败不默认fallback |
| PLAN-CLAR-005 | maxAttempts3包含首次，总两次间隔1s/2s；策略上限10s，指数下一项4s不在终态之后执行 | SpringRetry maxAttempts API；SpecTEST015要求3次有界 | 保留3次总投递和有限重试，不变DLQ/identity/ACK | fakeClock+calls断言失败回Factory |
| PLAN-CLAR-006 | 用Common inherited deleteVersionedById(et)/Repository.removeById(PO)，XML bind et与MP_OPTLOCK_VERSION_ORIGINAL；双向converter遵生成<PO,BO>方向 | EgonColaMapper.java/Repository.java、SpuDAO.xml/原Converters | 相同CAS/tenant/softdelete/audit合同，不造标量重载绕Common验证 | mapper/填充/version契约不成立停止上线 |
| PLAN-CLAR-007 | Strategy normalize返回nested SyncEvent元数据合同，2策略内部计算BO→identity→MapStruct Event | 两Event同metadata；Spec所选Strategy接口尚无完整内部签名 | 没新wire字段/file/层，无instanceof分派，实际目标集合保持 | contract/producer测试不通过回Strategy |
| PLAN-CLAR-008 | 当前pageQuery是record，AdPageQuery保持普通class字段并converter构造原PageQuery，不继承record | common PageQuery及ProductPageQuery | 原pageNo/pageSize/body/OAS行为保持，普通class标准符合 | 生成signature校验失败回AdPageQuery |
| PLAN-CLAR-009 | 将status的`CHECK IN ('0','1')`写为命名表级约束；现有生成器只从表级CheckConstraint提取枚举值并生成PO的`@Pattern`，不处理列内CHECK | `PostgreDdlAdapter.table`收集`CheckConstraint`；`columnConstraints`仅收集PRIMARY/UNIQUE；`GenerationScopeValidator.enumPattern`消费表约束 | PostgreSQL约束语义相同，兼容生成器的既有支持边界；没有组件修改或行为变化 | `AdPersistenceTest`和生成PO必须确认约束仍存在且Pattern为^(?:0|1)$，失败则停止该生成阶段 |

Additional source-grounded clarification：common AdWindowValidator采用现ProductCompositionValidator的nested input接口，两载体由已有getters实现；Position/URL校验只注入AdConfiguration提供的JDK List/URI，避免Common→Application/Infrastructure的反向依赖。没有新业务字段/wire/文件/公共调用，Step3/9编译和bytecode检查验证。

Domain允许Jakarta不是Plan inference：已通过本轮问题明确获得mario批准并落primary§10.3.1。没有未获批准的架构/业务/依赖更改。NATIVE布局修正也是本轮显式用户决策，不归入局部Plan Clarification。

## 7. Ordered File-by-file Implementation Steps

每Step验证working directory均为`/Users/mario/SelfProject/enjoyshop`；除已批准offlinegenerator外，所有命令只检查/构建，不启动应用/基础设施。GENERATED一段apply同时原子写该阶段已审全清单，后续file块记录检查及approvedbusiness定制顺序；不把这些块误作多次重放同阶段apply。

### Step 1 — 按不可变SQL确定 NATIVE 主从路由配置

- Requirements: REQ-001, REQ-002, REQ-010, REQ-011
- Dependencies: None
- Baseline state: HEAD7e6a506；当前配置/缺失源码和STRATEGY与SQL基线冲突已在§6/11锁定。
- Observable outcome: 全原表/2n/LOCAL及三组逻辑身份保留，六物理池与事务主读规则可离线验证；不连接PG。
- End state: 全原表/2n/LOCAL及三组逻辑身份保留，六物理池与事务主读规则可离线验证；不连接PG。后继内容只按后继Step发生；未部署本Step。
- Test-first gate: Required — 新增断言尚无NATIVE读写组/replica配置；现有ProfileKeyParityTest仍要求四个profile包含STRATEGY表路径，改配置前先为新合同更新该测试，RED应明确指向旧配置结构。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-JSON-001, MC-TIME-001, MC-CONFIG-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001；本Step文件角色对不适用注解写明理由，scope/test永远适用。
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；按每file角色落实，不让非Java运维文本伪承担Java类型规则。
- Ordered files:

#### File 1 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/config/NativeReadwriteContractTest.java`

- Purpose: 验证生产native规则能由ShardingSphere 5.5.3 parser读取、Common拓扑校验可materialize完整SQL布局，并证明读写组/物理角色映射正确。
- Symbols: `readWriteGroupsBindSixPhysicalSources`; `nativeProfilesAreParseableWithoutConnections`
- Repository evidence: `EgonColaShardingTopologyValidator.validate(EgonColaShardingProperties,byte[])` uses `YamlEngine.unmarshal` without constructing JDBC pools；same starter's `two-level-readwrite.yml` provides ShardingSphere `dataSourceGroups` syntax; `ToplogyBO` exposes profiles/schemas.
- Dependencies and consumers: `EgonColaShardingProperties` builder/physical-source records; `EgonColaShardingTopologyValidator`; Jakarta Validator; `YamlEngine` and the target NATIVE YAML; `ProfileKeyParityTest` independently checks per-profile property shape.
- Why now: 当前resource absent and typed properties still STRATEGY/3 PRIMARY; this RED observes exact route groups/schema without a Spring context or database.
- Contract/signature changes: test-only; route contract source is the approved SQL's `_0/_1` targets and `dataSourceGroups` with PRIMARY transactions.
- Input/output and state mapping: a test-only typed config names master_data/shard_0/shard_1 primary+replica logical pairs; dummy jdbc:postgresql URLs are syntax fixtures and never opened; output is ShardingSphere YAML configuration plus topology profiles.
- Error and edge behavior: missing resource fails a named assertion; unsupported/invalid YAML fails parser or Common topology validation; wrong role/logical-name/transaction strategy/target-order fails field assertions; no active TCP/PG/Redis/server.
- Standards impact: `MC-REUSE-001`, `MC-DEP-001`, `MC-SCOPE-001`, `MC-TEST-001`; test reuses existing ShardingSphere/Common Validator APIs and adds no test or runtime dependency.
- Literal rule enforcement: Rule 1, Rule 7, Rule 11; the test class has semantic `Test` suffix, verifies four profile properties in its companion test and remains under the established Light test tree; no production POJO/converter/time API is added here.
- Implementation pseudocode:

```java
@Test void nativeProfilesAreParseableWithoutConnections() throws Exception {
    byte[] yaml = readRequiredClasspath("egon-ss-readwrite.yml");
    EgonColaShardingProperties properties = properties(
        SHARDING_READWRITE, NATIVE, LOCAL, primaryReplicaPairsWithNonroutableExampleJDBCURLs());
    try (ValidatorFactory vf = Validation.buildDefaultValidatorFactory()) {
        var validator = new EgonColaShardingTopologyValidator(new ValidationUtils(vf.getValidator()));
        var topology = validator.validate(properties, yaml);
        assertThat(topology.profiles().keySet()).containsExactlyInAnyOrder(
            "spu","sku","users","roles","permissions","user_roles","role_permissions",
            "courses","brand","category","template","spec","para","category_brand",
            "school_classes","class_course_schedules");
        assertNodes(topology,"school_classes",Set.of(
            "shard_0.public.school_classes_0","shard_0.public.school_classes_1",
            "shard_1.public.school_classes_0","shard_1.public.school_classes_1"));
        assertNodes(topology,"class_course_schedules",matchingSqlNodes());
        assertThat(topology.schemas().keySet()).containsExactlyInAnyOrder("master_data","shard_0","shard_1");
    }
    YamlJDBCConfiguration jdbc = YamlEngine.unmarshal(utf8(yaml), YamlJDBCConfiguration.class);
    assertThat(jdbc.getDataSources()).isEmpty(); // Common datasource pool creation stays sole owner.
    var rw = onlyRule(jdbc, YamlReadwriteSplittingRuleConfiguration.class);
    assertThat(rw.getDataSourceGroups()).containsOnlyKeys("master_data","shard_0","shard_1");
    for (String group : groups) {
        assertThat(rw.getDataSourceGroups().get(group).getWriteDataSourceName()).isEqualTo(group+"_primary");
        assertThat(rw.getDataSourceGroups().get(group).getReadDataSourceNames()).containsExactly(group+"_replica_0");
        assertThat(rw.getDataSourceGroups().get(group).getTransactionalReadQueryStrategy()).isEqualTo("PRIMARY");
        assertThat(rw.getDataSourceGroups().get(group).getLoadBalancerName()).isEqualTo("round_robin");
    }
    assertThat(jdbc.getTransaction().getDefaultType()).isEqualTo("LOCAL");
    assertThat(jdbc.getProps()).containsEntry("sql-show",false).containsEntry("check-table-metadata-enabled",true);
}

@Test void readWriteGroupsBindSixPhysicalSources() {
    // Bind each environment profile with dummy URL/credentials as values only; do not open a connection.
    for (String profile : PROFILES) {
        var p = bindShardingProperties(profile, examplePgPlaceholders());
        assertThat(p.getMode()).isEqualTo(SHARDING_READWRITE);
        assertThat(p.getConfigStyle()).isEqualTo(NATIVE);
        assertThat(p.getNativeRulesResource()).isEqualTo("classpath:egon-ss-readwrite.yml");
        assertThat(p.getDataSources()).hasSize(6);
        assertThat(roleMap(p)).containsExactlyInAnyOrderEntriesOf(Map.of(
            "master_data_primary","master_data/PRIMARY","master_data_replica_0","master_data/REPLICA",
            "shard_0_primary","shard_0/PRIMARY","shard_0_replica_0","shard_0/REPLICA",
            "shard_1_primary","shard_1/PRIMARY","shard_1_replica_0","shard_1/REPLICA"));
        assertThat(p.getTables()).isEmpty();
        assertThat(p.getTransactionDefaultType()).isEqualTo("LOCAL");
    }
}
```

- Verification contribution: Step 1 test first produces RED because the required NATIVE resource/rules and six pools are absent; GREEN parses through the shipped 5.5.3 YAML engine and materializes the Common typed topology without any driver connection.
- After this file: The contract compiles against current starter APIs and fails only because the approved NATIVE resource/pools have not yet been added.


#### File 2 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/start/config/ProfileKeyParityTest.java`

- Purpose: 移除本次变更必然废弃的四环境 STRATEGY 表断言，并对四profile切换NATIVE的核心键树/规则资源引用施加回归断言。
- Symbols: `GOVERNANCE_KEYS`; `catalog_tables_are_single_master_data_in_every_profile` → `catalogs_use_native_rule_resource_in_every_profile`; new `native_sharding_core_keys_have_strict_parity`。
- Repository evidence: `ProfileKeyParityTest.GOVERNANCE_KEYS` current entries at lines 69–79 require `sharding.tables.brand/category/template/spec/para/category_brand`; method `catalog_tables_are_single_master_data_in_every_profile` lines 187–212 reads the same obsolete shape. The rest of the class checks unrelated identity/CORS/S3/tenant/cache contracts and stays intact.
- Dependencies and consumers: Existing YAML-loading helpers/profiles; Step 1's classpath `egon-ss-readwrite.yml`; `NativeReadwriteContractTest` performs actual custom-tag parsing and routing checks.
- Why now: No Spring context or topology connection; this RED makes the current STRATEGY-only profile assertions fail by design before the profile keys and NATIVE resource are changed.
- Contract/signature changes: Remove only obsolete `sharding.tables.*` governance paths; add `egon.cola.component.mybatis-plus.sharding.config-style`, `.native-rules-resource`, `.mode`, `.transaction-default-type`. The renamed catalog test asserts the four profiles use the same NATIVE resource pointer; deep `!SINGLE` content is asserted by NativeReadwriteContractTest, not SnakeYAML.
- Input/output and state mapping: each `application*.yml` config map -> normalized NATIVE core key set; profile URLs/passwords may differ; the rules resource is shared classpath input.
- Error and edge behavior: Missing/STRATEGY-only profile, wrong resource path or non-LOCAL type fails this focused test; existing unrelated Governance switches and Jianshen parity stay covered.
- Standards impact: `MC-CONFIG-001`, `MC-SCOPE-001`, `MC-TEST-001` — profile key sets are checked at their owning config boundary; no project dependencies added.
- Literal rule enforcement: Rule 7, Rule 11 — compare all four profile trees and keep test in its current Light `start/config` package; no new application model or framework layer.
- Implementation pseudocode:

```java
@Test void native_sharding_core_keys_have_strict_parity() {
    Set<String> expected = normalizedCoreNativeKeys(load("application.yml"));
    for (String profile : PROFILES) {
        Map<String,Object> root = load(profile);
        assertThat(normalizedCoreNativeKeys(root)).isEqualTo(expected);
        assertThat(valueAt(root, PREFIX+"config-style")).isEqualTo("NATIVE");
        assertThat(valueAt(root, PREFIX+"native-rules-resource")).isEqualTo("classpath:egon-ss-readwrite.yml");
        assertThat(valueAt(root, PREFIX+"mode")).isEqualTo("SHARDING_READWRITE");
        assertThat(valueAt(root, PREFIX+"transaction-default-type")).isEqualTo("LOCAL");
    }
    assertThat(classpathResource("egon-ss-readwrite.yml")).exists();
}
```

- Verification contribution: With current profiles the focused test fails specifically on NATIVE core values and the missing NATIVE resource; after config changes it proves equal core key shape/default policy while preserving existing unrelated assertions.
- After this file: The four-profile test no longer requires the old STRATEGY tables; NATIVE route semantics remain independently covered by NativeReadwriteContractTest.


#### File 3 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/architecture/LightPersistenceArchitectureTest.java`

- Purpose: 把STRATEGY文本断言换成等价native结构断言
- Symbols: generatedShardingConfigurationUsesTenantRouting
- Repository evidence: enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/architecture/LightPersistenceArchitectureTest.java当前STANDARD_TENANT_ID/type SINGLE断言
- Dependencies and consumers: 本file调用/观察Step 1列明的实际subject（NativeReadwriteContractTest,ProfileKeyParityTest,LightPersistenceArchitectureTest）；复用JUnit/MockMvc/原testsupport，负面fixture不依赖外部服务。
- Why now: 本位置承接Step1前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: generatedShardingConfigurationUsesTenantRouting；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 把STRATEGY文本断言换成等价native结构断言；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-VALID-001/MC-JSON-001/MC-BEAN-001/MC-CONFIG-001/MC-SCOPE-001/MC-TEST-001：测试pseudocode所列正负fixture/模型与wiring，不启外部服务。
- Literal rule enforcement: Rule 1, Rule 2, Rule 6, Rule 10, Rule 11：同模块testrole，逐约束/组/JSON/时序断言；测试类不是业务Bean，不伪加POJO构造注解。
- Implementation pseudocode:

```java
@Test generatedShardingConfigurationUsesTenantRouting(): parse native rules and typed topology;
assert NATIVE + LOCAL + schema-qualified SINGLE rules + existing tenant_id sharding;
assert no local DataSource bootstrapper and same Common Starter ownership;
keep counters 13 PO/DAO/Repository and 9 DomainServiceImpl in this Step. // Step 2/3 own count changes
```

- Verification contribution: Step 1的 `NativeReadwriteContractTest,ProfileKeyParityTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: generatedShardingConfigurationUsesTenantRouting的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。


#### File 4 — `CREATE scripts/work/2026-10-10-16-13-postgresql-cdc-advertisement-implementation-verify.sh`

- Purpose: 为profile/schema/源依赖/最终范围建立离线验证入口
- Symbols: self-test; topology; schema; standards; deployment; final
- Repository evidence: deployment/k8s/check.sh的离线/自测试约定；技能要求scratch位于scripts/work
- Dependencies and consumers: 本Step伪代码列明的原组件/生成输入和具名Bean，§5对应后继owner消费；commit scope仅本file和明确依赖。
- Why now: 本位置承接Step1前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: self-test; topology; schema; standards; deployment; final；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 为profile/schema/源依赖/最终范围建立离线验证入口；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-SCOPE-001/MC-TEST-001；MC-CONFIG-001/MC-TIME-001：配置键/预算沿明确模板；不含secret/无自动运行。
- Literal rule enforcement: Rule 11：保留Light/本任务scope；Rule 7：此配置与同profile文件成套；Rule 10：预算/记录UTC单位明确。
- Implementation pseudocode:

```bash
#!/usr/bin/env bash
set -euo pipefail; parse only --self-test/--topology/--schema/--standards/--deployment/--final;
use Python stdlib fixtures in mktemp, trap cleanup; no Docker/curl/kubectl/SQL/service start;
--self-test: valid fixture passes; missing replica/wrong transaction role/changed old hash fails;
--topology: inspect exact NativeReadwriteContractTest inventory and all 5 YAML inputs;
--schema/--standards/--deployment initially report gate not available until owning Step adds assertions;
--final invokes implemented offline gates; fail on missing/stale source, no ignored assertions;
Step 9 moves this file to deployment/cdc/check.sh, updates invocation paths and removes scratch copy.
```

- Verification contribution: Step 1的 `NativeReadwriteContractTest,ProfileKeyParityTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: self-test; topology; schema; standards; deployment; final的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。


#### File 5 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/resources/egon-ss-readwrite.yml`

- Purpose: 完整native规则提供原表路由及三组readwrite
- Symbols: rules; dataSourceGroups; loadBalancers; props
- Repository evidence: E/.../EgonColaShardingYamlLoader.java assembleStrategy/loadNative；原有效profile tables
- Dependencies and consumers: 对应typedProperties→Commonphysicalpool/Native loader/Cachemanager及Ad/CDCconfiguration；同profile资源和NativeReadwriteContractTest消费键树；无新factory。
- Why now: 本位置承接Step1前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: rules; dataSourceGroups; loadBalancers; props；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 完整native规则提供原表路由及三组readwrite；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-SCOPE-001/MC-TEST-001；MC-CONFIG-001/MC-TIME-001：配置键/预算沿明确模板；不含secret/无自动运行。
- Literal rule enforcement: Rule 11：保留Light/本任务scope；Rule 7：此配置与同profile文件成套；Rule 10：预算/记录UTC单位明确。
- Implementation pseudocode:

```yaml
databaseName: egon
rules:
  - !READWRITE_SPLITTING
    dataSourceGroups:
      master_data:
        writeDataSourceName: master_data_primary
        readDataSourceNames: [master_data_replica_0]
        transactionalReadQueryStrategy: PRIMARY
        loadBalancerName: round_robin
      shard_0:
        writeDataSourceName: shard_0_primary
        readDataSourceNames: [shard_0_replica_0]
        transactionalReadQueryStrategy: PRIMARY
        loadBalancerName: round_robin
      shard_1:
        writeDataSourceName: shard_1_primary
        readDataSourceNames: [shard_1_replica_0]
        transactionalReadQueryStrategy: PRIMARY
        loadBalancerName: round_robin
    loadBalancers:
      round_robin:
        type: ROUND_ROBIN
  - !SINGLE
    tables:
      - master_data.public.spu
      - master_data.public.sku
      - master_data.public.users
      - master_data.public.roles
      - master_data.public.permissions
      - master_data.public.user_roles
      - master_data.public.role_permissions
      - master_data.public.courses
      - master_data.public.brand
      - master_data.public.category
      - master_data.public.template
      - master_data.public.spec
      - master_data.public.para
      - master_data.public.category_brand
  - !SHARDING
    tables:
      school_classes:
        actualDataNodes: shard_0.public.school_classes_0,shard_0.public.school_classes_1,shard_1.public.school_classes_0,shard_1.public.school_classes_1
        databaseStrategy:
          standard: { shardingColumn: tenant_id, shardingAlgorithmName: school_classes_database }
        tableStrategy:
          standard: { shardingColumn: tenant_id, shardingAlgorithmName: school_classes_table }
      class_course_schedules:
        actualDataNodes: shard_0.public.class_course_schedules_0,shard_0.public.class_course_schedules_1,shard_1.public.class_course_schedules_0,shard_1.public.class_course_schedules_1
        databaseStrategy:
          standard: { shardingColumn: tenant_id, shardingAlgorithmName: class_course_schedules_database }
        tableStrategy:
          standard: { shardingColumn: tenant_id, shardingAlgorithmName: class_course_schedules_table }
    bindingTables: ["school_classes,class_course_schedules"]
    shardingAlgorithms:
      school_classes_database:
        type: CLASS_BASED
        props: { strategy: STANDARD, algorithmClassName: top.egon.cola.component.common.mybatis.sharding.algorithm.EgonColaStandardTenantIdShardingAlgorithm, target-order: "shard_0,shard_1" }
      school_classes_table:
        type: CLASS_BASED
        props: { strategy: STANDARD, algorithmClassName: top.egon.cola.component.common.mybatis.sharding.algorithm.EgonColaStandardTenantIdShardingAlgorithm, target-order: "school_classes_0,school_classes_1" }
      class_course_schedules_database:
        type: CLASS_BASED
        props: { strategy: STANDARD, algorithmClassName: top.egon.cola.component.common.mybatis.sharding.algorithm.EgonColaStandardTenantIdShardingAlgorithm, target-order: "shard_0,shard_1" }
      class_course_schedules_table:
        type: CLASS_BASED
        props: { strategy: STANDARD, algorithmClassName: top.egon.cola.component.common.mybatis.sharding.algorithm.EgonColaStandardTenantIdShardingAlgorithm, target-order: "class_course_schedules_0,class_course_schedules_1" }
transaction: { defaultType: LOCAL }
props: { sql-show: false, check-table-metadata-enabled: true }
```

Reuse the existing read/write YAML shape in `T/src/test/resources/sharding/two-level-readwrite.yml`; do not put physical `dataSources` here because the Common loader owns them. The two STANDARD algorithms call the existing class with explicit target order, so DB and table indexes derive from the same unsigned tenant ID modulo their respective target count; the physical table targets match the immutable `_0/_1` DDL. The existing `ProfileKeyParityTest` keeps identity/CORS/S3/Jianshen assertions and changes only its obsolete STRATEGY table assertions.

- Verification contribution: Step 1的 `NativeReadwriteContractTest,ProfileKeyParityTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: rules; dataSourceGroups; loadBalancers; props的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。


#### File 6 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/resources/egon-mybatis-plus-sharding.yml`

- Purpose: 迁移本文件有效sharding配置且保持原生命周期/其他配置
- Symbols: egon.cola.component.mybatis-plus.sharding.*
- Repository evidence: enjoyshop-service/enjoyshop-service-goods/src/main/resources/egon-mybatis-plus-sharding.yml目前STRATEGY tables及PRIMARY pools
- Dependencies and consumers: 对应typedProperties→Commonphysicalpool/Native loader/Cachemanager及Ad/CDCconfiguration；同profile资源和NativeReadwriteContractTest消费键树；无新factory。
- Why now: 本位置承接Step1前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: egon.cola.component.mybatis-plus.sharding.*；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 迁移本文件有效sharding配置且保持原生命周期/其他配置；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-SCOPE-001/MC-TEST-001；MC-CONFIG-001/MC-TIME-001：配置键/预算沿明确模板；不含secret/无自动运行。
- Literal rule enforcement: Rule 11：保留Light/本任务scope；Rule 7：此配置与同profile文件成套；Rule 10：预算/记录UTC单位明确。
- Implementation pseudocode:

```yaml
sharding.config-style = NATIVE;
sharding.native-rules-resource = classpath:egon-ss-readwrite.yml;
sharding.mode = SHARDING_READWRITE; transaction-default-type = LOCAL;
replace physical names with master_data_primary/shard_0_primary/shard_1_primary,
append each matching replica_0 with same logical-name and REPLICA role;
keep primary URL environment variables; add distinct replica URL/credentials env inputs, no real secrets;
remove only sharding.tables from this input; preserve mybatis-plus.tables, lifecycle and write-guard unrelated keys;
retain disabled Rabbit/other integration defaults; compare effective profile key tree with NativeReadwriteContractTest.
```

- Verification contribution: Step 1的 `NativeReadwriteContractTest,ProfileKeyParityTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: egon.cola.component.mybatis-plus.sharding.*的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。


#### File 7 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/resources/application.yml`

- Purpose: 迁移本文件有效sharding配置且保持原生命周期/其他配置
- Symbols: egon.cola.component.mybatis-plus.sharding.*
- Repository evidence: enjoyshop-service/enjoyshop-service-goods/src/main/resources/application.yml目前STRATEGY tables及PRIMARY pools
- Dependencies and consumers: 对应typedProperties→Commonphysicalpool/Native loader/Cachemanager及Ad/CDCconfiguration；同profile资源和NativeReadwriteContractTest消费键树；无新factory。
- Why now: 本位置承接Step1前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: egon.cola.component.mybatis-plus.sharding.*；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 迁移本文件有效sharding配置且保持原生命周期/其他配置；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-SCOPE-001/MC-TEST-001；MC-CONFIG-001/MC-TIME-001：配置键/预算沿明确模板；不含secret/无自动运行。
- Literal rule enforcement: Rule 11：保留Light/本任务scope；Rule 7：此配置与同profile文件成套；Rule 10：预算/记录UTC单位明确。
- Implementation pseudocode:

```yaml
sharding.config-style = NATIVE;
sharding.native-rules-resource = classpath:egon-ss-readwrite.yml;
sharding.mode = SHARDING_READWRITE; transaction-default-type = LOCAL;
replace physical names with master_data_primary/shard_0_primary/shard_1_primary,
append each matching replica_0 with same logical-name and REPLICA role;
keep primary URL environment variables; add distinct replica URL/credentials env inputs, no real secrets;
remove only sharding.tables from this input; preserve mybatis-plus.tables, lifecycle and write-guard unrelated keys;
retain disabled Rabbit/other integration defaults; compare effective profile key tree with NativeReadwriteContractTest.
```

- Verification contribution: Step 1的 `NativeReadwriteContractTest,ProfileKeyParityTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: egon.cola.component.mybatis-plus.sharding.*的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。


#### File 8 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-dev.yml`

- Purpose: 迁移本文件有效sharding配置且保持原生命周期/其他配置
- Symbols: egon.cola.component.mybatis-plus.sharding.*
- Repository evidence: enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-dev.yml目前STRATEGY tables及PRIMARY pools
- Dependencies and consumers: 对应typedProperties→Commonphysicalpool/Native loader/Cachemanager及Ad/CDCconfiguration；同profile资源和NativeReadwriteContractTest消费键树；无新factory。
- Why now: 本位置承接Step1前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: egon.cola.component.mybatis-plus.sharding.*；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 迁移本文件有效sharding配置且保持原生命周期/其他配置；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-SCOPE-001/MC-TEST-001；MC-CONFIG-001/MC-TIME-001：配置键/预算沿明确模板；不含secret/无自动运行。
- Literal rule enforcement: Rule 11：保留Light/本任务scope；Rule 7：此配置与同profile文件成套；Rule 10：预算/记录UTC单位明确。
- Implementation pseudocode:

```yaml
sharding.config-style = NATIVE;
sharding.native-rules-resource = classpath:egon-ss-readwrite.yml;
sharding.mode = SHARDING_READWRITE; transaction-default-type = LOCAL;
replace physical names with master_data_primary/shard_0_primary/shard_1_primary,
append each matching replica_0 with same logical-name and REPLICA role;
keep primary URL environment variables; add distinct replica URL/credentials env inputs, no real secrets;
remove only sharding.tables from this input; preserve mybatis-plus.tables, lifecycle and write-guard unrelated keys;
retain disabled Rabbit/other integration defaults; compare effective profile key tree with NativeReadwriteContractTest.
```

- Verification contribution: Step 1的 `NativeReadwriteContractTest,ProfileKeyParityTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: egon.cola.component.mybatis-plus.sharding.*的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。


#### File 9 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-test.yml`

- Purpose: 迁移本文件有效sharding配置且保持原生命周期/其他配置
- Symbols: egon.cola.component.mybatis-plus.sharding.*
- Repository evidence: enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-test.yml目前STRATEGY tables及PRIMARY pools
- Dependencies and consumers: 对应typedProperties→Commonphysicalpool/Native loader/Cachemanager及Ad/CDCconfiguration；同profile资源和NativeReadwriteContractTest消费键树；无新factory。
- Why now: 本位置承接Step1前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: egon.cola.component.mybatis-plus.sharding.*；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 迁移本文件有效sharding配置且保持原生命周期/其他配置；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-SCOPE-001/MC-TEST-001；MC-CONFIG-001/MC-TIME-001：配置键/预算沿明确模板；不含secret/无自动运行。
- Literal rule enforcement: Rule 11：保留Light/本任务scope；Rule 7：此配置与同profile文件成套；Rule 10：预算/记录UTC单位明确。
- Implementation pseudocode:

```yaml
sharding.config-style = NATIVE;
sharding.native-rules-resource = classpath:egon-ss-readwrite.yml;
sharding.mode = SHARDING_READWRITE; transaction-default-type = LOCAL;
replace physical names with master_data_primary/shard_0_primary/shard_1_primary,
append each matching replica_0 with same logical-name and REPLICA role;
keep primary URL environment variables; add distinct replica URL/credentials env inputs, no real secrets;
remove only sharding.tables from this input; preserve mybatis-plus.tables, lifecycle and write-guard unrelated keys;
retain disabled Rabbit/other integration defaults; compare effective profile key tree with NativeReadwriteContractTest.
```

- Verification contribution: Step 1的 `NativeReadwriteContractTest,ProfileKeyParityTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: egon.cola.component.mybatis-plus.sharding.*的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。


#### File 10 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-prod.yml`

- Purpose: 迁移本文件有效sharding配置且保持原生命周期/其他配置
- Symbols: egon.cola.component.mybatis-plus.sharding.*
- Repository evidence: enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-prod.yml目前STRATEGY tables及PRIMARY pools
- Dependencies and consumers: 对应typedProperties→Commonphysicalpool/Native loader/Cachemanager及Ad/CDCconfiguration；同profile资源和NativeReadwriteContractTest消费键树；无新factory。
- Why now: 本位置承接Step1前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: egon.cola.component.mybatis-plus.sharding.*；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 迁移本文件有效sharding配置且保持原生命周期/其他配置；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-SCOPE-001/MC-TEST-001；MC-CONFIG-001/MC-TIME-001：配置键/预算沿明确模板；不含secret/无自动运行。
- Literal rule enforcement: Rule 11：保留Light/本任务scope；Rule 7：此配置与同profile文件成套；Rule 10：预算/记录UTC单位明确。
- Implementation pseudocode:

```yaml
sharding.config-style = NATIVE;
sharding.native-rules-resource = classpath:egon-ss-readwrite.yml;
sharding.mode = SHARDING_READWRITE; transaction-default-type = LOCAL;
replace physical names with master_data_primary/shard_0_primary/shard_1_primary,
append each matching replica_0 with same logical-name and REPLICA role;
keep primary URL environment variables; add distinct replica URL/credentials env inputs, no real secrets;
remove only sharding.tables from this input; preserve mybatis-plus.tables, lifecycle and write-guard unrelated keys;
retain disabled Rabbit/other integration defaults; compare effective profile key tree with NativeReadwriteContractTest.
```

- Verification contribution: Step 1的 `NativeReadwriteContractTest,ProfileKeyParityTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: egon.cola.component.mybatis-plus.sharding.*的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

- Validation working directory: `/Users/mario/SelfProject/enjoyshop`
- Verification command: `mvn -pl enjoyshop-service/enjoyshop-service-goods -am -Dtest=NativeReadwriteContractTest,ProfileKeyParityTest,LightPersistenceArchitectureTest -Dsurefire.failIfNoSpecifiedTests=false test`（先RED记录原因，最低实现后同命令GREEN）；另执行 `bash scripts/work/2026-10-10-16-13-postgresql-cdc-advertisement-implementation-verify.sh --self-test` 与对应 `--topology`。
- Expected result: RED仅因上述缺失行为；GREEN exit0，named tests全部执行且无误跳过；离线检查对应断言通过。TEST019–022运行项仍明确Not run。
- Failure returns to: 本Step声明的测试/最低production/wiring file；若历史SQL/架构/工具范围冲突则回相应前置Step及§11，不绕过修复。
- Completion criteria: 本Step所有file/规则/MC证据闭合，测试报告匹配selectors、diff只本scope；不包含临时无用catalog类、秘密或组件源码。
- Rollback: 未运行SQL时仅撤销本Step自己写的path改动，保留他人内容；已部署SQL按§9追加forward-fix，不改旧脚本/history；下游持久消息/offset不丢。
- Commit paths: `enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/config/NativeReadwriteContractTest.java`；`enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/start/config/ProfileKeyParityTest.java`；`scripts/work/2026-10-10-16-13-postgresql-cdc-advertisement-implementation-verify.sh`；`enjoyshop-service/enjoyshop-service-goods/src/main/resources/egon-ss-readwrite.yml`；`enjoyshop-service/enjoyshop-service-goods/src/main/resources/egon-mybatis-plus-sharding.yml`；`enjoyshop-service/enjoyshop-service-goods/src/main/resources/application.yml`；`enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-dev.yml`；`enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-test.yml`；`enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-prod.yml`；`enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/architecture/LightPersistenceArchitectureTest.java`

- Commit: `feat(goods): prepare native postgres readwrite topology`；每逻辑任务至多一个path-limitedcommit；暂态净删除file不为其制造空commit，记录新HEAD。



### Step 2 — 新增单一广告 DDL 并生成持久化与领域模型基线

- Requirements: REQ-002, REQ-004, REQ-007, REQ-011
- Dependencies: Step 1 committed
- Baseline state: Step 1已GREEN/提交并记录HEAD；前序已提供本file依赖。
- Observable outcome: 一个新SQL/manifest、16列ad、三表RI FULL；离线生成初始6类/Mapper，未发布HTTP。
- End state: 一个新SQL/manifest、16列ad、三表RI FULL；离线生成初始6类/Mapper，未发布HTTP。 后继内容只按后继Step发生；未部署本Step。
- Test-first gate: Required — Ad表/新version不存在；count应新增1，旧3表仍default replica identity。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-JSON-001, MC-TIME-001, MC-CONFIG-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001；本Step文件角色对不适用注解写明理由，scope/test永远适用。
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；按每file角色落实，不让非Java运维文本伪承担Java类型规则。
- Ordered files:

#### File 1 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/ad/AdPersistenceTest.java`

- Purpose: 先定义DDL和生成的PO/DAO/Repository继承及UTC字段RED
- Symbols: singleNewMigration; inheritedFields; noOldSqlDrift; noUnscopedCandidates
- Repository evidence: enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/support/PersistenceTestSupport.java及原manifest/架构测试
- Dependencies and consumers: 本file调用/观察Step 2列明的实际subject（AdPersistenceTest,NativeReadwriteContractTest,ProfileKeyParityTest,LightPersistenceArchitectureTest）；复用JUnit/MockMvc/原testsupport，负面fixture不依赖外部服务。
- Why now: 先确立缺失行为的RED，后续最小实现须满足它。
- Contract/signature changes: singleNewMigration; inheritedFields; noOldSqlDrift; noUnscopedCandidates；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 先定义DDL和生成的PO/DAO/Repository继承及UTC字段RED；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-VALID-001/MC-JSON-001/MC-BEAN-001/MC-CONFIG-001/MC-SCOPE-001/MC-TEST-001：测试pseudocode所列正负fixture/模型与wiring，不启外部服务。
- Literal rule enforcement: Rule 1, Rule 2, Rule 6, Rule 10, Rule 11：同模块testrole，逐约束/组/JSON/时序断言；测试类不是业务Bean，不伪加POJO构造注解。
- Implementation pseudocode:

```java
@Test singleNewMigration(): assert latest+1=20261010_001, manifest checksum exact bytes;
assert guarded MASTER_DATA creates 16-column ad + 3 indexes and alters RI FULL on ad/spu/sku;
assert SHARD no-op and unknown role exception; assert old scripts unchanged;
@Test inheritedFields(): reflect AdPO extends EgonModel, no shared field redeclaration;
assert business startTime/endTime Instant, inherited deletedAt LocalDateTime;
assert Repository extends Common base, Domain has no ORM classes. // RED: new resources/types missing
add persistence-behavior tests only in Step 3 after types compile; no external DB autostart.
```

- Verification contribution: Step 2的 `AdPersistenceTest,NativeReadwriteContractTest,ProfileKeyParityTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: singleNewMigration; inheritedFields; noOldSqlDrift; noUnscopedCandidates的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 2 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/config/NativeReadwriteContractTest.java`

- Purpose: 随着新ad加入调整唯一允许新表清单
- Symbols: retainsAllLogicalTables +ad
- Repository evidence: Step1 test与新增single表约束
- Dependencies and consumers: 本file调用/观察Step 2列明的实际subject（AdPersistenceTest,NativeReadwriteContractTest,ProfileKeyParityTest,LightPersistenceArchitectureTest）；复用JUnit/MockMvc/原testsupport，负面fixture不依赖外部服务。
- Why now: 先确立缺失行为的RED，后续最小实现须满足它。
- Contract/signature changes: retainsAllLogicalTables +ad；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 随着新ad加入调整唯一允许新表清单；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-VALID-001/MC-JSON-001/MC-BEAN-001/MC-CONFIG-001/MC-SCOPE-001/MC-TEST-001：测试pseudocode所列正负fixture/模型与wiring，不启外部服务。
- Literal rule enforcement: Rule 1, Rule 2, Rule 6, Rule 10, Rule 11：同模块testrole，逐约束/组/JSON/时序断言；测试类不是业务Bean，不伪加POJO构造注解。
- Implementation pseudocode:

```java
expected original14SINGLE + ad; assert no other table changes;
assert Ad native profile=SINGLE/master_data and DDL PRIMARY target identity same;
fingerprintcomparison reportsdelta for correctedSQLtargetlayout andnewad, verifiesoriginalSINGLEownership unchanged; never autoaccept persistedhistorydelta, document expected additive ad profile delta.
```

- Verification contribution: Step 2的 `AdPersistenceTest,NativeReadwriteContractTest,ProfileKeyParityTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: retainsAllLogicalTables +ad的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 3 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/resources/db/egon-mp/V20261010_001__initialize_advertisement_cdc.sql`

- Purpose: 一个新受管版本创建ad并补CDC旧行信息
- Symbols: CREATE TABLE public.ad; 3 indexes; ALTER REPLICA IDENTITY
- Repository evidence: 现latest V20261009_001与role guard；Spec §11.2
- Dependencies and consumers: 本Step伪代码列明的原组件/生成输入和具名Bean，§5对应后继owner消费；commit scope仅本file和明确依赖。
- Why now: 本位置承接Step2前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: CREATE TABLE public.ad; 3 indexes; ALTER REPLICA IDENTITY；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 一个新受管版本创建ad并补CDC旧行信息；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-SCOPE-001/MC-TEST-001；MC-CONFIG-001/MC-TIME-001：配置键/预算沿明确模板；不含secret/无自动运行。
- Literal rule enforcement: Rule 11：保留Light/本任务scope；Rule 7：此配置与同profile文件成套；Rule 10：预算/记录UTC单位明确。
- Implementation pseudocode:

```sql
DO role guard:
 IF current_setting('egon_migration.role')='MASTER_DATA' THEN
  CREATE TABLE ad (id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL CHECK>0,
   create_user_id/update_user_id VARCHAR(128) DEFAULT 'migration' NOT NULL,
   create_time/update_time TIMESTAMPTZ(6) DEFAULT CURRENT_TIMESTAMP NOT NULL,
   deleted_at TIMESTAMP(6) NULL, version BIGINT DEFAULT 0 CHECK>=0 NOT NULL,
   name VARCHAR(120), position VARCHAR(64), image_url VARCHAR(512), target_url VARCHAR(1024),
            status CHAR(1) DEFAULT '0', start_time/end_time TIMESTAMPTZ(6), sort_order INTEGER DEFAULT0,
            CONSTRAINT ck_ad_status CHECK (status IN ('0', '1')));
  enforce all 8 business fields NOT NULL, nonblank/position regex/status0,1/window/sort CHECK per Spec;
  CREATE idx_ad_active_slot(tenant_id,position,sort_order,id) WHERE deleted_at IS NULL AND status='1';
  CREATE idx_ad_admin_tenant_id(tenant_id,id DESC) WHERE deleted_at IS NULL;
  ALTER TABLE public.ad/public.spu/public.sku REPLICA IDENTITY FULL;
 ELSIF role='SHARD' THEN no-op; ELSE RAISE EXCEPTION; END IF;
no FK/adCode/UK/new migration framework/backfill; keep old SQL byte-identical.
```

- Verification contribution: Step 2的 `AdPersistenceTest,NativeReadwriteContractTest,ProfileKeyParityTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: CREATE TABLE public.ad; 3 indexes; ALTER REPLICA IDENTITY的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 4 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/resources/db/egon-mp/repository-manifest.json`

- Purpose: 追加新脚本SHA256条目，不改旧记录
- Symbols: scripts[last].version/path/sha256
- Repository evidence: enjoyshop-service/enjoyshop-service-goods/src/main/resources/db/egon-mp/repository-manifest.json四条已应用prefix
- Dependencies and consumers: 本Step伪代码列明的原组件/生成输入和具名Bean，§5对应后继owner消费；commit scope仅本file和明确依赖。
- Why now: 本位置承接Step2前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: scripts[last].version/path/sha256；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 追加新脚本SHA256条目，不改旧记录；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-SCOPE-001/MC-TEST-001；MC-CONFIG-001/MC-TIME-001：配置键/预算沿明确模板；不含secret/无自动运行。
- Literal rule enforcement: Rule 11：保留Light/本任务scope；Rule 7：此配置与同profile文件成套；Rule 10：预算/记录UTC单位明确。
- Implementation pseudocode:

```json
sha256 = hash exact new SQL bytes after final edit;
append {version:'20261010_001', path:'db/egon-mp/V20261010_001__initialize_advertisement_cdc.sql', sha256};
retain family/light and original entry order/checksums; validate no duplicate version/path;
rollback never deletes applied manifest/history; later correction uses another separately approved logical task.
```

- Verification contribution: Step 2的 `AdPersistenceTest,NativeReadwriteContractTest,ProfileKeyParityTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: scripts[last].version/path/sha256的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 5 — `CREATE scripts/work/2026-10-10-16-13-postgresql-cdc-advertisement-implementation-prepare-ad.sh`

- Purpose: 生成CREATE-only输入及核对v2 generator范围
- Symbols: extract-create; generator-stage; verify-generated-scope
- Repository evidence: E/scripts/egon-codegen.sh及PostgreDdlAdapter拒绝ALTER；Spec §8/11
- Dependencies and consumers: 本Step伪代码列明的原组件/生成输入和具名Bean，§5对应后继owner消费；commit scope仅本file和明确依赖。
- Why now: 本位置承接Step2前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: extract-create; generator-stage; verify-generated-scope；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 生成CREATE-only输入及核对v2 generator范围；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-SCOPE-001/MC-TEST-001；MC-CONFIG-001/MC-TIME-001：配置键/预算沿明确模板；不含secret/无自动运行。
- Literal rule enforcement: Rule 11：保留Light/本任务scope；Rule 7：此配置与同profile文件成套；Rule 10：预算/记录UTC单位明确。
- Implementation pseudocode:

```bash
parse balanced CREATE TABLE ad from approved new SQL using Python stdlib (no SQL dependency);
fail on missing/multiple CREATE/mismatched 16 cols; copy CREATE+standalone indexes to module codegen/ad-create.sql;
record source SHA and view SHA; reject ALTER/DROP/COMMENT in derived view;
accept stage persistence/application/controller; render exact approved artifacts into ad-codegen.json;
run verified launcher plan --config absoluteConfig using existing explicit EGON_CODEGEN_CLASSPATH;
parse last JSON, assert formatVersion2, exact output inventory, all new paths ADD/no conflicts;
run apply --plan recordedId --config sameConfig only after review; do not build/download tooling;
do not run a later stage including previously hand-customized artifacts;
Step 9 moves to module/codegen/prepare-ad.sh and keeps deterministic extraction/tests.
```

- Verification contribution: Step 2的 `AdPersistenceTest,NativeReadwriteContractTest,ProfileKeyParityTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: extract-create; generator-stage; verify-generated-scope的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 6 — `CREATE enjoyshop-service/enjoyshop-service-goods/codegen/ad-codegen.json`

- Purpose: 锁profile/input/table/artifact范围，不传混合SQL给工具
- Symbols: configVersion; projectType; logicalTables; artifacts; fieldPolicies
- Repository evidence: 同目录brand-codegen.json/product-persistence-codegen.json与CodegenConfigBO
- Dependencies and consumers: 本Step伪代码列明的原组件/生成输入和具名Bean，§5对应后继owner消费；commit scope仅本file和明确依赖。
- Why now: 本位置承接Step2前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: configVersion; projectType; logicalTables; artifacts; fieldPolicies；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 锁profile/input/table/artifact范围，不传混合SQL给工具；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-SCOPE-001/MC-TEST-001；MC-CONFIG-001/MC-TIME-001：配置键/预算沿明确模板；不含secret/无自动运行。
- Literal rule enforcement: Rule 11：保留Light/本任务scope；Rule 7：此配置与同profile文件成套；Rule 10：预算/记录UTC单位明确。
- Implementation pseudocode:

```json
configVersion:1; projectType:light; basePackage:top.egon.enjoyshop.goods; domain:ad;
outputRoot:enjoyshop-service/enjoyshop-service-goods;
input:{mode:schema,resourceRoot:enjoyshop-service/enjoyshop-service-goods/codegen,schemaFiles:[ad-create.sql]};
logicalTables:[ad]; existingTypeMappings:{};
artifacts stage1:[po,dao,mapper-xml,repo,domain-model,domain-query];
fieldPolicies create/update/result name,position,image_url,target_url,status,start_time,end_time,sort_order;
filter position,status; sort id,sort_order;
apiContract existingErrorMapper=top.egon.enjoyshop.goods.common.error.GoodsErrorMapper,
contextSymbol=top.egon.enjoyshop.goods.adapter.filter.RequestContext,basePath=/api/v1/ads;
events.enabled:false; stage2/3 change only artifacts after scope review, same CREATE input hashes.
```

- Verification contribution: Step 2的 `AdPersistenceTest,NativeReadwriteContractTest,ProfileKeyParityTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: configVersion; projectType; logicalTables; artifacts; fieldPolicies的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 7 — `GENERATED enjoyshop-service/enjoyshop-service-goods/codegen/ad-create.sql`

- Purpose: CREATE-only视图保留与源迁移相同ad定义
- Symbols: ad CREATE and indexes; extraction provenance
- Repository evidence: 新20261010_001；Spec明确CREATE-only视图
- Dependencies and consumers: 本Step伪代码列明的原组件/生成输入和具名Bean，§5对应后继owner消费；commit scope仅本file和明确依赖。
- Why now: 本位置承接Step2前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: ad CREATE and indexes; extraction provenance；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: CREATE-only视图保留与源迁移相同ad定义；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-SCOPE-001/MC-TEST-001；MC-CONFIG-001/MC-TIME-001：配置键/预算沿明确模板；不含secret/无自动运行。
- Literal rule enforcement: Rule 11：保留Light/本任务scope；Rule 7：此配置与同profile文件成套；Rule 10：预算/记录UTC单位明确。
- Implementation pseudocode:

```bash
初始来源只允许本Step已核对的 ad-codegen.json + E/scripts/egon-codegen.sh plan/apply；下列为命令、生成diff核对和允许业务定制目标，不允许手写初始类替代工具。
bash scripts/work/2026-10-10-16-13-postgresql-cdc-advertisement-implementation-prepare-ad.sh --extract-create;
assert view = exact new migration CREATE ad slice + declared indexes;
assert source and view hash recorded; only this schemaFile reaches generator;
view is not another migration version and is never in managed manifest;
SQL journal records throughVersion:none for outside-classpath input and explicit original-script provenance/full SQL separately.
```

- Verification contribution: Step 2的 `AdPersistenceTest,NativeReadwriteContractTest,ProfileKeyParityTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: ad CREATE and indexes; extraction provenance的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 8 — `GENERATED enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/ad/po/AdPO.java`

- Purpose: 用初始CREATE生成AdPO；限定后续业务编辑
- Symbols: AdPO
- Repository evidence: E generator catalog po及Light layout固定路径
- Dependencies and consumers: AdManage→AdDomainService/Impl→AdRepository→AdDAO/XML；MP-SDJ/CommonModel、namedConverters、CachePort/primaryTemplate通过明确Bean接入，Domain port不引用DAO。
- Why now: 本位置承接Step2前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: AdPO；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 用初始CREATE生成AdPO；限定后续业务编辑；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-NAME-001/MC-VALID-001/MC-MODEL-001/MC-JSON-001/MC-TIME-001：普通class完整Lombok，root@Builder/继承@SuperBuilder+callSuper；Jakarta分组，UTC/wire/敏感字段按本file。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 6, Rule 10, Rule 11：@Data/@NoArgsConstructor/@AllArgsConstructor/@Accessors(chain=true)，匹配构造Builder；继承字段不重复；禁止record身份对象。
- Implementation pseudocode:

```bash
初始来源只允许本Step已核对的 ad-codegen.json + E/scripts/egon-codegen.sh plan/apply；下列为命令、生成diff核对和允许业务定制目标，不允许手写初始类替代工具。
bash /Users/mario/SelfProject/Egon-COLA/scripts/egon-codegen.sh plan --config /Users/mario/SelfProject/enjoyshop/enjoyshop-service/enjoyshop-service-goods/codegen/ad-codegen.json;
review v2 plan: this exact po output ADD, no overwrite/no sample deletion;
bash /Users/mario/SelfProject/Egon-COLA/scripts/egon-codegen.sh apply --plan REVIEWED_ID --config sameConfig;
inspect generated AdPO diff; no handwritten initial class body;
agent customize only approved time fields startTime/endTime to Instant in AdPO and startAt/endAt in AdBO,
keep inherited common fields and add explicit MapStruct mappings later; XML column map uses original PG names;
verify DAO/Repository use Common Mapper/Repository and query access remains inside Repository;
no Controller generated in this stage; no ad HTTP reachable.
```

- Verification contribution: Step 2的 `AdPersistenceTest,NativeReadwriteContractTest,ProfileKeyParityTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: AdPO的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 9 — `GENERATED enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/ad/dao/AdDAO.java`

- Purpose: 用初始CREATE生成AdDAO；限定后续业务编辑
- Symbols: AdDAO
- Repository evidence: E generator catalog dao及Light layout固定路径
- Dependencies and consumers: AdManage→AdDomainService/Impl→AdRepository→AdDAO/XML；MP-SDJ/CommonModel、namedConverters、CachePort/primaryTemplate通过明确Bean接入，Domain port不引用DAO。
- Why now: 本位置承接Step2前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: AdDAO；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 用初始CREATE生成AdDAO；限定后续业务编辑；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-ARCH-001/MC-REUSE-001/MC-VALID-001/MC-LOG-001/MC-BEAN-001/MC-UTIL-001/MC-PATTERN-001/MC-SCOPE-001：保持所选层，实际注解/Bean/边界见pseudocode。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：业务@Slf4j、显式Bean名，@RequiredArgsConstructor和每final@Qualifier；JDK/现库，Jackson/Instant/Duration；selectedpattern不换反射分派。配置factory用现@Bean参数Qualifier，接口不伪构造器。
- Implementation pseudocode:

```bash
初始来源只允许本Step已核对的 ad-codegen.json + E/scripts/egon-codegen.sh plan/apply；下列为命令、生成diff核对和允许业务定制目标，不允许手写初始类替代工具。
bash /Users/mario/SelfProject/Egon-COLA/scripts/egon-codegen.sh plan --config /Users/mario/SelfProject/enjoyshop/enjoyshop-service/enjoyshop-service-goods/codegen/ad-codegen.json;
review v2 plan: this exact dao output ADD, no overwrite/no sample deletion;
bash /Users/mario/SelfProject/Egon-COLA/scripts/egon-codegen.sh apply --plan REVIEWED_ID --config sameConfig;
inspect generated AdDAO diff; no handwritten initial class body;
agent customize only approved time fields startTime/endTime to Instant in AdPO and startAt/endAt in AdBO,
keep inherited common fields and add explicit MapStruct mappings later; XML column map uses original PG names;
verify DAO/Repository use Common Mapper/Repository and query access remains inside Repository;
no Controller generated in this stage; no ad HTTP reachable.
```

- Verification contribution: Step 2的 `AdPersistenceTest,NativeReadwriteContractTest,ProfileKeyParityTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: AdDAO的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 10 — `GENERATED enjoyshop-service/enjoyshop-service-goods/src/main/resources/mybatis/mapper/ad/AdDAO.xml`

- Purpose: 用初始CREATE生成AdDAO.xml；限定后续业务编辑
- Symbols: AdDAO.xml
- Repository evidence: E generator catalog mapper-xml及Light layout固定路径
- Dependencies and consumers: 本Step伪代码列明的原组件/生成输入和具名Bean，§5对应后继owner消费；commit scope仅本file和明确依赖。
- Why now: 本位置承接Step2前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: AdDAO.xml；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 用初始CREATE生成AdDAO.xml；限定后续业务编辑；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-SCOPE-001/MC-TEST-001；MC-SCOPE-001：仅追加/精确文档归属，标运行未验证。
- Literal rule enforcement: Rule 11：保留Light/本任务scope；规则中Java注解不适用此非Java文件；不引入运行/Schema新语义。
- Implementation pseudocode:

```bash
初始来源只允许本Step已核对的 ad-codegen.json + E/scripts/egon-codegen.sh plan/apply；下列为命令、生成diff核对和允许业务定制目标，不允许手写初始类替代工具。
bash /Users/mario/SelfProject/Egon-COLA/scripts/egon-codegen.sh plan --config /Users/mario/SelfProject/enjoyshop/enjoyshop-service/enjoyshop-service-goods/codegen/ad-codegen.json;
review v2 plan: this exact mapper-xml output ADD, no overwrite/no sample deletion;
bash /Users/mario/SelfProject/Egon-COLA/scripts/egon-codegen.sh apply --plan REVIEWED_ID --config sameConfig;
inspect generated AdDAO.xml diff; no handwritten initial class body;
agent customize only approved time fields startTime/endTime to Instant in AdPO and startAt/endAt in AdBO,
keep inherited common fields and add explicit MapStruct mappings later; XML column map uses original PG names;
verify DAO/Repository use Common Mapper/Repository and query access remains inside Repository;
no Controller generated in this stage; no ad HTTP reachable.
```

- Verification contribution: Step 2的 `AdPersistenceTest,NativeReadwriteContractTest,ProfileKeyParityTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: AdDAO.xml的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 11 — `GENERATED enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/ad/repo/AdRepository.java`

- Purpose: 用初始CREATE生成AdRepository；限定后续业务编辑
- Symbols: AdRepository
- Repository evidence: E generator catalog repo及Light layout固定路径
- Dependencies and consumers: AdManage→AdDomainService/Impl→AdRepository→AdDAO/XML；MP-SDJ/CommonModel、namedConverters、CachePort/primaryTemplate通过明确Bean接入，Domain port不引用DAO。
- Why now: 本位置承接Step2前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: AdRepository；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 用初始CREATE生成AdRepository；限定后续业务编辑；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-ARCH-001/MC-REUSE-001/MC-VALID-001/MC-LOG-001/MC-BEAN-001/MC-UTIL-001/MC-PATTERN-001/MC-SCOPE-001：保持所选层，实际注解/Bean/边界见pseudocode。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：业务@Slf4j、显式Bean名，@RequiredArgsConstructor和每final@Qualifier；JDK/现库，Jackson/Instant/Duration；selectedpattern不换反射分派。配置factory用现@Bean参数Qualifier，接口不伪构造器。
- Implementation pseudocode:

```bash
初始来源只允许本Step已核对的 ad-codegen.json + E/scripts/egon-codegen.sh plan/apply；下列为命令、生成diff核对和允许业务定制目标，不允许手写初始类替代工具。
bash /Users/mario/SelfProject/Egon-COLA/scripts/egon-codegen.sh plan --config /Users/mario/SelfProject/enjoyshop/enjoyshop-service/enjoyshop-service-goods/codegen/ad-codegen.json;
review v2 plan: this exact repo output ADD, no overwrite/no sample deletion;
bash /Users/mario/SelfProject/Egon-COLA/scripts/egon-codegen.sh apply --plan REVIEWED_ID --config sameConfig;
inspect generated AdRepository diff; no handwritten initial class body;
agent customize only approved time fields startTime/endTime to Instant in AdPO and startAt/endAt in AdBO,
keep inherited common fields and add explicit MapStruct mappings later; XML column map uses original PG names;
verify DAO/Repository use Common Mapper/Repository and query access remains inside Repository;
no Controller generated in this stage; no ad HTTP reachable.
```

- Verification contribution: Step 2的 `AdPersistenceTest,NativeReadwriteContractTest,ProfileKeyParityTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: AdRepository的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 12 — `GENERATED enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/ad/AdBO.java`

- Purpose: 用初始CREATE生成AdBO；限定后续业务编辑
- Symbols: AdBO
- Repository evidence: E generator catalog domain-model及Light layout固定路径
- Dependencies and consumers: AdManage→AdDomainService/Impl→AdRepository→AdDAO/XML；MP-SDJ/CommonModel、namedConverters、CachePort/primaryTemplate通过明确Bean接入，Domain port不引用DAO。
- Why now: 本位置承接Step2前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: AdBO；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 用初始CREATE生成AdBO；限定后续业务编辑；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-NAME-001/MC-VALID-001/MC-MODEL-001/MC-JSON-001/MC-TIME-001：普通class完整Lombok，root@Builder/继承@SuperBuilder+callSuper；Jakarta分组，UTC/wire/敏感字段按本file。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 6, Rule 10, Rule 11：@Data/@NoArgsConstructor/@AllArgsConstructor/@Accessors(chain=true)，匹配构造Builder；继承字段不重复；禁止record身份对象。
- Implementation pseudocode:

```bash
初始来源只允许本Step已核对的 ad-codegen.json + E/scripts/egon-codegen.sh plan/apply；下列为命令、生成diff核对和允许业务定制目标，不允许手写初始类替代工具。
bash /Users/mario/SelfProject/Egon-COLA/scripts/egon-codegen.sh plan --config /Users/mario/SelfProject/enjoyshop/enjoyshop-service/enjoyshop-service-goods/codegen/ad-codegen.json;
review v2 plan: this exact domain-model output ADD, no overwrite/no sample deletion;
bash /Users/mario/SelfProject/Egon-COLA/scripts/egon-codegen.sh apply --plan REVIEWED_ID --config sameConfig;
inspect generated AdBO diff; no handwritten initial class body;
agent customize only approved time fields startTime/endTime to Instant in AdPO and startAt/endAt in AdBO,
keep inherited common fields and add explicit MapStruct mappings later; XML column map uses original PG names;
verify DAO/Repository use Common Mapper/Repository and query access remains inside Repository;
no Controller generated in this stage; no ad HTTP reachable.
```

- Verification contribution: Step 2的 `AdPersistenceTest,NativeReadwriteContractTest,ProfileKeyParityTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: AdBO的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 13 — `GENERATED enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/ad/AdDomainQuery.java`

- Purpose: 用初始CREATE生成AdDomainQuery；限定后续业务编辑
- Symbols: AdDomainQuery
- Repository evidence: E generator catalog domain-query及Light layout固定路径
- Dependencies and consumers: AdManage→AdDomainService/Impl→AdRepository→AdDAO/XML；MP-SDJ/CommonModel、namedConverters、CachePort/primaryTemplate通过明确Bean接入，Domain port不引用DAO。
- Why now: 本位置承接Step2前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: AdDomainQuery；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 用初始CREATE生成AdDomainQuery；限定后续业务编辑；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-NAME-001/MC-VALID-001/MC-MODEL-001/MC-JSON-001/MC-TIME-001：普通class完整Lombok，root@Builder/继承@SuperBuilder+callSuper；Jakarta分组，UTC/wire/敏感字段按本file。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 6, Rule 10, Rule 11：@Data/@NoArgsConstructor/@AllArgsConstructor/@Accessors(chain=true)，匹配构造Builder；继承字段不重复；禁止record身份对象。
- Implementation pseudocode:

```bash
初始来源只允许本Step已核对的 ad-codegen.json + E/scripts/egon-codegen.sh plan/apply；下列为命令、生成diff核对和允许业务定制目标，不允许手写初始类替代工具。
bash /Users/mario/SelfProject/Egon-COLA/scripts/egon-codegen.sh plan --config /Users/mario/SelfProject/enjoyshop/enjoyshop-service/enjoyshop-service-goods/codegen/ad-codegen.json;
review v2 plan: this exact domain-query output ADD, no overwrite/no sample deletion;
bash /Users/mario/SelfProject/Egon-COLA/scripts/egon-codegen.sh apply --plan REVIEWED_ID --config sameConfig;
inspect generated AdDomainQuery diff; no handwritten initial class body;
agent customize only approved time fields startTime/endTime to Instant in AdPO and startAt/endAt in AdBO,
keep inherited common fields and add explicit MapStruct mappings later; XML column map uses original PG names;
verify DAO/Repository use Common Mapper/Repository and query access remains inside Repository;
no Controller generated in this stage; no ad HTTP reachable.
```

- Verification contribution: Step 2的 `AdPersistenceTest,NativeReadwriteContractTest,ProfileKeyParityTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: AdDomainQuery的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 14 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/resources/egon-ss-readwrite.yml`

- Purpose: 新增唯一SINGLE ad规则
- Symbols: master_data.public.ad
- Repository evidence: enjoyshop-service/enjoyshop-service-goods/src/main/resources/egon-ss-readwrite.yml由Step1形成
- Dependencies and consumers: 对应typedProperties→Commonphysicalpool/Native loader/Cachemanager及Ad/CDCconfiguration；同profile资源和NativeReadwriteContractTest消费键树；无新factory。
- Why now: 本位置承接Step2前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: master_data.public.ad；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 新增唯一SINGLE ad规则；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-SCOPE-001/MC-TEST-001；MC-CONFIG-001/MC-TIME-001：配置键/预算沿明确模板；不含secret/无自动运行。
- Literal rule enforcement: Rule 11：保留Light/本任务scope；Rule 7：此配置与同profile文件成套；Rule 10：预算/记录UTC单位明确。
- Implementation pseudocode:

```yaml
append master_data.public.ad to SINGLE list; preserve all other native rules/pool names/profile fingerprints;
assert exactly one ad SINGLE rule, no shard ad table and no PRIMARY renamed again.
assert ad is the only new profile and original historical migration bytes are unchanged; do not reset stored topology fingerprint.
```

- Verification contribution: Step 2的 `AdPersistenceTest,NativeReadwriteContractTest,ProfileKeyParityTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: master_data.public.ad的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 15 — `MODIFY docs/egon/codegen/ddl-consumption-log.md`

- Purpose: 追加工具运行记录与源DDL溯源，非DB执行日志
- Symbols: append-only timed journal
- Repository evidence: 技能backend-code-generation classpath SQL journal；仓库现文件存在时改为MODIFY append-only
- Dependencies and consumers: 本Step伪代码列明的原组件/生成输入和具名Bean，§5对应后继owner消费；commit scope仅本file和明确依赖。
- Why now: 本位置承接Step2前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: append-only timed journal；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 追加工具运行记录与源DDL溯源，非DB执行日志；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-SCOPE-001/MC-TEST-001；MC-SCOPE-001：仅追加/精确文档归属，标运行未验证。
- Literal rule enforcement: Rule 11：保留Light/本任务scope；规则中Java注解不适用此非Java文件；不引入运行/Schema新语义。
- Implementation pseudocode:

```markdown
append Asia/Shanghai time/profile light/outputRoot/generator plan+apply identity;
throughVersion:none because actual schema input is module/codegen/ad-create.sql outside classpath:db;
record derived view path/hash/full text; separately record provenance original20261010_001 path/SHA/full SQL;
state original mixed SQL not sent to generator and no PG SQL executed;
append once per actual successful apply (Step3/4 repeat); never rewrite earlier entries.
```

- Verification contribution: Step 2的 `AdPersistenceTest,NativeReadwriteContractTest,ProfileKeyParityTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: append-only timed journal的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 16 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/architecture/LightPersistenceArchitectureTest.java`

- Purpose: 只随真实生成更新持久化计数
- Symbols: generatedProjectUsesCommonPersistenceOwnership
- Repository evidence: enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/architecture/LightPersistenceArchitectureTest.java Step1
- Dependencies and consumers: 本file调用/观察Step 2列明的实际subject（AdPersistenceTest,NativeReadwriteContractTest,ProfileKeyParityTest,LightPersistenceArchitectureTest）；复用JUnit/MockMvc/原testsupport，负面fixture不依赖外部服务。
- Why now: 本位置承接Step2前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: generatedProjectUsesCommonPersistenceOwnership；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 只随真实生成更新持久化计数；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-VALID-001/MC-JSON-001/MC-BEAN-001/MC-CONFIG-001/MC-SCOPE-001/MC-TEST-001：测试pseudocode所列正负fixture/模型与wiring，不启外部服务。
- Literal rule enforcement: Rule 1, Rule 2, Rule 6, Rule 10, Rule 11：同模块testrole，逐约束/组/JSON/时序断言；测试类不是业务Bean，不伪加POJO构造注解。
- Implementation pseudocode:

```java
set PO/DAO/Repository count 13 ->14; retain DomainServiceImpl count9 until Step3;
assert AdPO/AdDAO/AdRepository exist and no additional shared-field declarations;
assert no ORM/local bootstrap/no generic Service infrastructure extension.
```

- Verification contribution: Step 2的 `AdPersistenceTest,NativeReadwriteContractTest,ProfileKeyParityTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: generatedProjectUsesCommonPersistenceOwnership的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 17 — `MODIFY scripts/work/2026-10-10-16-13-postgresql-cdc-advertisement-implementation-verify.sh`

- Purpose: 实现本Step已经调用的 `--schema` 离线门禁，闭合迁移、manifest 与 generator 输入视图的一致性检查。
- Symbols: `--schema`; migration/manifest SHA-256; CREATE-only extraction parity.
- Repository evidence: Step 1脚本现对`--schema`返回`CHECK_PENDING`；本Step验证命令已经要求该模式通过；新migration及`codegen/ad-create.sql`现已在本Step产生。
- Dependencies and consumers: 本Step `AdPersistenceTest`、`prepare-ad.sh --extract-create`与managed migration；仅Python/JDK stdlib和Git，不连接PostgreSQL。
- Why now: 当前Plan验证调用尚无对应owner；用户已批准将此脚本修改纳入Step2，并规定核对migration/manifest/CREATE-only视图。
- Contract/signature changes: `--schema`由exit77待实现转为只读校验；其他self-test/topology/pending模式行为保持。
- Input/output and state mapping: 读取manifest唯一新版本、源DDL字节、CREATE TABLE列、两条索引、三表Replica Identity与CREATE-only视图；成功输出PASS，不修改任何文件。
- Error and edge behavior: 错路径/版本、manifest SHA漂移、列/index/Replica Identity缺失、视图与源提取不等、混入ALTER/DROP/COMMENT均非0退出；不自动repair/重写view或manifest。
- Standards impact: `MC-CONFIG-001`, `MC-SCOPE-001`, `MC-TEST-001`；仅本Plan已列离线检查，不更改运行服务。
- Literal rule enforcement: Rule 7, Rule 11 — shell/YAML/SQL文件不是Java类，不引入依赖、不连接真实数据库。
- Implementation pseudocode:

```bash
case --schema -> Python stdlib only;
check exactly one manifest entry 20261010_001 and exact migration path/SHA;
extract exactly one balanced CREATE TABLE ad; assert exact approved 16 columns and two named non-unique indexes;
assert MASTER_DATA/SHARD/unknown-role branches and REPLICA IDENTITY FULL for ad/spu/sku;
rebuild CREATE-only view text in memory and require byte equality/hash with codegen/ad-create.sql;
retain old manifest prefix/history hash checks; never write, connect, or invoke the DDL runner.
```

- Verification contribution: `bash scripts/work/2026-10-10-16-13-postgresql-cdc-advertisement-implementation-verify.sh --schema` must pass after Step2 files are complete; `--self-test` remains green and rejects mutated fixtures.
- After this file: Step2's declared `--schema` gate has an owning implementation and cannot silently pass as pending.

- Validation working directory: `/Users/mario/SelfProject/enjoyshop`
- Verification command: `mvn -pl enjoyshop-service/enjoyshop-service-goods -am -Dtest=AdPersistenceTest,NativeReadwriteContractTest,ProfileKeyParityTest,LightPersistenceArchitectureTest -Dsurefire.failIfNoSpecifiedTests=false test`（先RED记录原因，最低实现后同命令GREEN）；另执行 `bash scripts/work/2026-10-10-16-13-postgresql-cdc-advertisement-implementation-verify.sh --self-test` 与对应 `--schema`。
- Expected result: RED仅因上述缺失行为；GREEN exit0，named tests全部执行且无误跳过；离线检查对应断言通过。TEST019–022运行项仍明确Not run。
- Failure returns to: 本Step声明的测试/最低production/wiring file；若历史SQL/架构/工具范围冲突则回相应前置Step及§11，不绕过修复。
- Completion criteria: 本Step所有file/规则/MC证据闭合，测试报告匹配selectors、diff只本scope；不包含临时无用catalog类、秘密或组件源码。
- Rollback: 未运行SQL时仅撤销本Step自己写的path改动，保留他人内容；已部署SQL按§9追加forward-fix，不改旧脚本/history；下游持久消息/offset不丢。
- Commit paths: `enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/ad/AdPersistenceTest.java`；`enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/config/NativeReadwriteContractTest.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/resources/db/egon-mp/V20261010_001__initialize_advertisement_cdc.sql`；`enjoyshop-service/enjoyshop-service-goods/src/main/resources/db/egon-mp/repository-manifest.json`；`scripts/work/2026-10-10-16-13-postgresql-cdc-advertisement-implementation-prepare-ad.sh`；`scripts/work/2026-10-10-16-13-postgresql-cdc-advertisement-implementation-verify.sh`；`enjoyshop-service/enjoyshop-service-goods/codegen/ad-codegen.json`；`enjoyshop-service/enjoyshop-service-goods/codegen/ad-create.sql`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/ad/po/AdPO.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/ad/dao/AdDAO.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/resources/mybatis/mapper/ad/AdDAO.xml`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/ad/repo/AdRepository.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/ad/AdBO.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/ad/AdDomainQuery.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/resources/egon-ss-readwrite.yml`；`docs/egon/codegen/ddl-consumption-log.md`；`enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/architecture/LightPersistenceArchitectureTest.java`

- Commit: `feat(goods): add advertisement schema and generated persistence`；每逻辑任务至多一个path-limitedcommit；暂态净删除file不为其制造空commit，记录新HEAD。

### Step 3 — 实现租户隔离和版本保护的广告管理业务

- Requirements: REQ-004, REQ-007, REQ-011
- Dependencies: Step 2 committed
- Baseline state: Step 1已提交HEAD `8c8f08cd98157b8281041245dae48243b8d0d9e3`（10个commit paths见Step 1提交）；Step 2已提交HEAD `5fd424c62f781985698b28d68b0cb4258ed3068c`（17个commit paths见Step 2的Commit paths/git show）；代码工作树无剩余Step1/2差异，Spec与Plan文档仍未跟踪且未暂存。
- Observable outcome: 管理的create/replace/page/detail/delete在Light内部可调用并受verified权限保护；无HTTP暴露，所有字段/version/软删除语义闭合。
- End state: 管理的create/replace/page/detail/delete在Light内部可调用并受verified权限保护；无HTTP暴露，所有字段/version/软删除语义闭合。 后继内容只按后继Step发生；未部署本Step。
- Test-first gate: Required — 没有广告Command/groups/domain/manage；缺权限、CAS与终态delete行为。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-JSON-001, MC-TIME-001, MC-CONFIG-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001；本Step文件角色对不适用注解写明理由，scope/test永远适用。
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；按每file角色落实，不让非Java运维文本伪承担Java类型规则。
- Ordered files:

#### File 1 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/application/ad/AdManageTest.java`

- Purpose: 管理用例和每层Validation的RED
- Symbols: rejectsUnknownOrInvalidWrite; permissionsFailClosed; versionConflict; deleteTerminalIsTrue
- Repository evidence: enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/application/goods原Product tests及PersistenceTestSupport
- Dependencies and consumers: 本file调用/观察Step 3列明的实际subject（AdManageTest,AdPersistenceTest,LightPersistenceArchitectureTest）；复用JUnit/MockMvc/原testsupport，负面fixture不依赖外部服务。
- Why now: 先确立缺失行为的RED，后续最小实现须满足它。
- Contract/signature changes: rejectsUnknownOrInvalidWrite; permissionsFailClosed; versionConflict; deleteTerminalIsTrue；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 管理用例和每层Validation的RED；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-VALID-001/MC-JSON-001/MC-BEAN-001/MC-CONFIG-001/MC-SCOPE-001/MC-TEST-001：测试pseudocode所列正负fixture/模型与wiring，不启外部服务。
- Literal rule enforcement: Rule 1, Rule 2, Rule 6, Rule 10, Rule 11：同模块testrole，逐约束/组/JSON/时序断言；测试类不是业务Bean，不伪加POJO构造注解。
- Implementation pseudocode:

```java
@Test create_requires_8_fields_and_verified_user(): valid USER permits exactly one Repository.save;
invalid window/URL/position/version/group -> constraint error before repository; unknown Json property ->400;
@Test stale_replace_conflicts(): tenant41/ad1/version2 vsstored3 ->610303, no update;
@Test delete_terminal(): missing/deleted/crossTenant ->true; active wrongversion->610303;
@Test group_handoffs(): direct Manage/DomainImpl/Repository invalid nested BO/query fails, not only Controller;
assert no business Rabbit publish; rollback no cache eviction (implemented Step5);
reflect on renamed generated types until they exist; report missing approved behavior, not DB environment failure.
```

- Verification contribution: Step 3的 `AdManageTest,AdPersistenceTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: rejectsUnknownOrInvalidWrite; permissionsFailClosed; versionConflict; deleteTerminalIsTrue的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 2 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/ad/AdPersistenceTest.java`

- Purpose: 为命名SQL/CAS/tenant隔离添加用例
- Symbols: tenantPredicates; concurrentVersionContract; terminalDeleteContract
- Repository evidence: enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/ad/AdPersistenceTest.java Step2
- Dependencies and consumers: 本file调用/观察Step 3列明的实际subject（AdManageTest,AdPersistenceTest,LightPersistenceArchitectureTest）；复用JUnit/MockMvc/原testsupport，负面fixture不依赖外部服务。
- Why now: 先确立缺失行为的RED，后续最小实现须满足它。
- Contract/signature changes: tenantPredicates; concurrentVersionContract; terminalDeleteContract；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 为命名SQL/CAS/tenant隔离添加用例；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-VALID-001/MC-JSON-001/MC-BEAN-001/MC-CONFIG-001/MC-SCOPE-001/MC-TEST-001：测试pseudocode所列正负fixture/模型与wiring，不启外部服务。
- Literal rule enforcement: Rule 1, Rule 2, Rule 6, Rule 10, Rule 11：同模块testrole，逐约束/组/JSON/时序断言；测试类不是业务Bean，不伪加POJO构造注解。
- Implementation pseudocode:

```java
stub Common mapper boundary only for deterministic orchestration; test named Repository methods;
SQL static assertions enforce tenant/id/active/version predicates and UTC deletion expression;
fake two concurrent expectedVersion writes -> one affected1, other0; zero-row maps610303;
assert input audit/tenant from verified context, not command body; missing other-tenant detail404;
explicitly label PG row-lock/isolation/concurrency unproven until manual TEST-019.
```

- Verification contribution: Step 3的 `AdManageTest,AdPersistenceTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: tenantPredicates; concurrentVersionContract; terminalDeleteContract的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 3 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/enums/GoodsErrorStatus.java`

- Purpose: 追加稳定广告错误码，不改原枚举
- Symbols: AD_REQUEST_INVALID610301; AD_NOT_FOUND302; AD_VERSION_CONFLICT303; AD_DEPENDENCY_UNAVAILABLE304; AD_FORBIDDEN305; AD_SITE_NOT_CONFIGURED306; AD_DATA_LIMIT307; AD_AUTHORIZATION_UNAVAILABLE308; AD_ID_UNAVAILABLE309; AD_DATA_INVALID310; AD_INTERNAL_ERROR399
- Repository evidence: enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/enums/GoodsErrorStatus.java原6101xx/6102xx
- Dependencies and consumers: 本Step伪代码列明的原组件/生成输入和具名Bean，§5对应后继owner消费；commit scope仅本file和明确依赖。
- Why now: 本位置承接Step3前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: AD_REQUEST_INVALID610301; AD_NOT_FOUND302; AD_VERSION_CONFLICT303; AD_DEPENDENCY_UNAVAILABLE304; AD_FORBIDDEN305; AD_SITE_NOT_CONFIGURED306; AD_DATA_LIMIT307; AD_AUTHORIZATION_UNAVAILABLE308; AD_ID_UNAVAILABLE309; AD_DATA_INVALID310; AD_INTERNAL_ERROR399；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 原错误码全保持，新增6103xx仅广告范围；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-NAME-001/MC-VALID-001/MC-JSON-001/MC-SCOPE-001：精确接口/enum/约束角色；无普通POJO注解误加，Common/Jakarta允许依赖。
- Literal rule enforcement: Rule 1, Rule 2, Rule 6, Rule 11：约束/enum/接口不是可变BO；按本file合法值/组注解；枚举@JsonValue/@EnumValue仅实际持久边界；不给接口生成构造器。
- Implementation pseudocode:

```java
append 11 Ad ErrorStatus constants with exact code/status names per Spec;
messages safe, no rejected secret/DBdetail; getStatus=name() and original codes unchanged;
map dependency/ID/authorization unavailable503, stale409, notFound404 in Handler Step4.
```

- Verification contribution: Step 3的 `AdManageTest,AdPersistenceTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: AD_REQUEST_INVALID610301; AD_NOT_FOUND302; AD_VERSION_CONFLICT303; AD_DEPENDENCY_UNAVAILABLE304; AD_FORBIDDEN305; AD_SITE_NOT_CONFIGURED306; AD_DATA_LIMIT307; AD_AUTHORIZATION_UNAVAILABLE308; AD_ID_UNAVAILABLE309; AD_DATA_INVALID310; AD_INTERNAL_ERROR399的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 4 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/validation/AdValidationGroup.java`

- Purpose: 为管理/查询/消息共用对象定义分组
- Symbols: Write extends Default; Create/Update extends Write; Delete/Detail/Page/PublicRead
- Repository evidence: enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/validation/ProductValidationGroup.java及已批准Spec修正
- Dependencies and consumers: AdSaveCommand/AdBO/AdPageQuery的Jakarta约束、Manage/DomainImpl的组调用消费；JDK类型参数由AdConfiguration供给，Common不反向导入业务实现。
- Why now: 本位置承接Step3前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: Write extends Default; Create/Update extends Write; Delete/Detail/Page/PublicRead；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 为管理/查询/消息共用对象定义分组；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-NAME-001/MC-VALID-001/MC-JSON-001/MC-SCOPE-001：精确接口/enum/约束角色；无普通POJO注解误加，Common/Jakarta允许依赖。
- Literal rule enforcement: Rule 1, Rule 2, Rule 6, Rule 11：约束/enum/接口不是可变BO；按本file合法值/组注解；枚举@JsonValue/@EnumValue仅实际持久边界；不给接口生成构造器。
- Implementation pseudocode:

```java
interface Write extends Default; Create/Update extends Write;
Delete/Detail/Page extend Default; PublicRead extends Write; domain BO can import this common group;
Create id/version null before fill, Update id/version required, Delete id/version required;
PO CRUD uses existing EgonColaModelValidationGroups, not these application groups;
no state-machine or generic validator registry.
```

- Verification contribution: Step 3的 `AdManageTest,AdPersistenceTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: Write extends Default; Create/Update extends Write; Delete/Detail/Page/PublicRead的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 5 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/ad/AdStatusEnum.java`

- Purpose: 统一启用码与JSON/数据库枚举
- Symbols: DISABLED("0"); ENABLED("1"); fromValue
- Repository evidence: enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods现enum及Spec §10
- Dependencies and consumers: AdManage→AdDomainService/Impl→AdRepository→AdDAO/XML；MP-SDJ/CommonModel、namedConverters、CachePort/primaryTemplate通过明确Bean接入，Domain port不引用DAO。
- Why now: 本位置承接Step3前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: DISABLED("0"); ENABLED("1"); fromValue；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 统一启用码与JSON/数据库枚举；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-NAME-001/MC-VALID-001/MC-JSON-001/MC-SCOPE-001：精确接口/enum/约束角色；无普通POJO注解误加，Common/Jakarta允许依赖。
- Literal rule enforcement: Rule 1, Rule 2, Rule 6, Rule 11：约束/enum/接口不是可变BO；按本file合法值/组注解；枚举@JsonValue/@EnumValue仅实际持久边界；不给接口生成构造器。
- Implementation pseudocode:

```java
enum AdStatusEnum: @EnumValue code; @JsonValue getCode(); @JsonCreator fromValue(JsonNode);
require isTextual; accept only "0"/"1" exact scalar, null/unknown invalid; never ordinal/name automatic coercion;
Simple scalar code rule; default state is provided Command mandatory, DB default0 only compatibility.
```

- Verification contribution: Step 3的 `AdManageTest,AdPersistenceTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: DISABLED("0"); ENABLED("1"); fromValue的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 6 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/validation/ValidAdWindow.java`

- Purpose: 跨字段UTC窗口/微秒约束声明
- Symbols: ValidAdWindow; validatedBy AdWindowValidator
- Repository evidence: enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/validation/Product约束模式
- Dependencies and consumers: AdSaveCommand/AdBO/AdPageQuery的Jakarta约束、Manage/DomainImpl的组调用消费；JDK类型参数由AdConfiguration供给，Common不反向导入业务实现。
- Why now: 本位置承接Step3前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: ValidAdWindow; validatedBy AdWindowValidator；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 跨字段UTC窗口/微秒约束声明；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-NAME-001/MC-VALID-001/MC-JSON-001/MC-SCOPE-001：精确接口/enum/约束角色；无普通POJO注解误加，Common/Jakarta允许依赖。
- Literal rule enforcement: Rule 1, Rule 2, Rule 6, Rule 11：约束/enum/接口不是可变BO；按本file合法值/组注解；枚举@JsonValue/@EnumValue仅实际持久边界；不给接口生成构造器。
- Implementation pseudocode:

```java
@Target(TYPE,ANNOTATION_TYPE) @Retention(RUNTIME)
@Constraint(validatedBy=AdWindowValidator.class) interface ValidAdWindow:
message/groups/payload + explicit variants only needed by purpose; no broad global validation switches;
use application create/update/write groups; keep generic nullability on @NotNull.
```

- Verification contribution: Step 3的 `AdManageTest,AdPersistenceTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: ValidAdWindow; validatedBy AdWindowValidator的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 7 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/validation/AdWindowValidator.java`

- Purpose: 跨字段UTC窗口/微秒约束实现
- Symbols: AdWindowValidator implements ConstraintValidator
- Repository evidence: enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/validation/ProductImageValidator.java模式、原Jakarta/S3配置
- Dependencies and consumers: AdSaveCommand/AdBO/AdPageQuery的Jakarta约束、Manage/DomainImpl的组调用消费；JDK类型参数由AdConfiguration供给，Common不反向导入业务实现。
- Why now: 本位置承接Step3前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: AdWindowValidator implements ConstraintValidator；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 跨字段UTC窗口/微秒约束实现；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-ARCH-001/MC-REUSE-001/MC-VALID-001/MC-LOG-001/MC-BEAN-001/MC-UTIL-001/MC-PATTERN-001/MC-SCOPE-001：保持所选层，实际注解/Bean/边界见pseudocode。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：业务@Slf4j、显式Bean名，@RequiredArgsConstructor和每final@Qualifier；JDK/现库，Jackson/Instant/Duration；selectedpattern不换反射分派。配置factory用现@Bean参数Qualifier，接口不伪构造器。
- Implementation pseudocode:

```java
AdWindowValidator implements ConstraintValidator<ValidAdWindow,AdWindowValidator.AdWindowInput>;
nested common interface AdWindowInput { Instant getStartAt(); Instant getEndAt(); }
AdSaveCommand and AdBO implement this interface; generated Lombok getters already fulfill it;
common does not import application/domain classes; mirror existing ProductCompositionValidator.ProductCompositionInput;
isValid(AdWindowInput value, context): null handled by @NotNull;
require startAt/endAt nonnull; nano%1000==0; endAt.isAfter(startAt);
produce endAt/startAt field-level violation via ConstraintValidatorContext; no clock/DB access.
```

- Verification contribution: Step 3的 `AdManageTest,AdPersistenceTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: AdWindowValidator implements ConstraintValidator的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 8 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/validation/ValidAdPosition.java`

- Purpose: 广告位语法和启动登记值校验声明
- Symbols: ValidAdPosition; validatedBy AdPositionValidator
- Repository evidence: enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/validation/Product约束模式
- Dependencies and consumers: AdSaveCommand/AdBO/AdPageQuery的Jakarta约束、Manage/DomainImpl的组调用消费；JDK类型参数由AdConfiguration供给，Common不反向导入业务实现。
- Why now: 本位置承接Step3前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: ValidAdPosition; validatedBy AdPositionValidator；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 广告位语法和启动登记值校验声明；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-NAME-001/MC-VALID-001/MC-JSON-001/MC-SCOPE-001：精确接口/enum/约束角色；无普通POJO注解误加，Common/Jakarta允许依赖。
- Literal rule enforcement: Rule 1, Rule 2, Rule 6, Rule 11：约束/enum/接口不是可变BO；按本file合法值/组注解；枚举@JsonValue/@EnumValue仅实际持久边界；不给接口生成构造器。
- Implementation pseudocode:

```java
@Target(FIELD,PARAMETER,ANNOTATION_TYPE) @Retention(RUNTIME)
@Constraint(validatedBy=AdPositionValidator.class) interface ValidAdPosition:
message/groups/payload + explicit variants only needed by purpose; no broad global validation switches;
use application create/update/write groups; keep generic nullability on @NotNull.
```

- Verification contribution: Step 3的 `AdManageTest,AdPersistenceTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: ValidAdPosition; validatedBy AdPositionValidator的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 9 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/validation/AdPositionValidator.java`

- Purpose: 广告位语法和启动登记值校验实现
- Symbols: AdPositionValidator implements ConstraintValidator
- Repository evidence: enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/validation/ProductImageValidator.java模式、原Jakarta/S3配置
- Dependencies and consumers: AdSaveCommand/AdBO/AdPageQuery的Jakarta约束、Manage/DomainImpl的组调用消费；JDK类型参数由AdConfiguration供给，Common不反向导入业务实现。
- Why now: 本位置承接Step3前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: AdPositionValidator implements ConstraintValidator；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 广告位语法和启动登记值校验实现；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-ARCH-001/MC-REUSE-001/MC-VALID-001/MC-LOG-001/MC-BEAN-001/MC-UTIL-001/MC-PATTERN-001/MC-SCOPE-001：保持所选层，实际注解/Bean/边界见pseudocode。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：业务@Slf4j、显式Bean名，@RequiredArgsConstructor和每final@Qualifier；JDK/现库，Jackson/Instant/Duration；selectedpattern不换反射分派。配置factory用现@Bean参数Qualifier，接口不伪构造器。
- Implementation pseudocode:

```java
isValid(String position): reject blank or nonmatch [a-z][a-z0-9_]{0,63};
@Component("adPositionValidator") @Slf4j @RequiredArgsConstructor;
@Qualifier("adAllowedPositions") final List<String> allowedPositions; require allowedPositions.contains(position); do not lower-case input; no unbounded cache keys;
CDC old positions only syntax, not current membership, to invalidate revoked positions.
```

- Verification contribution: Step 3的 `AdManageTest,AdPersistenceTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: AdPositionValidator implements ConstraintValidator的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 10 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/validation/ValidAdUrl.java`

- Purpose: 复用S3公开base约束及点击URL边界声明
- Symbols: ValidAdUrl; validatedBy AdUrlValidator
- Repository evidence: enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/validation/Product约束模式
- Dependencies and consumers: AdSaveCommand/AdBO/AdPageQuery的Jakarta约束、Manage/DomainImpl的组调用消费；JDK类型参数由AdConfiguration供给，Common不反向导入业务实现。
- Why now: 本位置承接Step3前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: ValidAdUrl; validatedBy AdUrlValidator；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 复用S3公开base约束及点击URL边界声明；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-NAME-001/MC-VALID-001/MC-JSON-001/MC-SCOPE-001：精确接口/enum/约束角色；无普通POJO注解误加，Common/Jakarta允许依赖。
- Literal rule enforcement: Rule 1, Rule 2, Rule 6, Rule 11：约束/enum/接口不是可变BO；按本file合法值/组注解；枚举@JsonValue/@EnumValue仅实际持久边界；不给接口生成构造器。
- Implementation pseudocode:

```java
@Target(FIELD,PARAMETER,ANNOTATION_TYPE) @Retention(RUNTIME)
@Constraint(validatedBy=AdUrlValidator.class) interface ValidAdUrl:
message/groups/payload + explicit variants only needed by purpose; no broad global validation switches;
use application create/update/write groups; keep generic nullability on @NotNull.
```

- Verification contribution: Step 3的 `AdManageTest,AdPersistenceTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: ValidAdUrl; validatedBy AdUrlValidator的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 11 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/validation/AdUrlValidator.java`

- Purpose: 复用S3公开base约束及点击URL边界实现
- Symbols: AdUrlValidator implements ConstraintValidator
- Repository evidence: enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/validation/ProductImageValidator.java模式、原Jakarta/S3配置
- Dependencies and consumers: AdSaveCommand/AdBO/AdPageQuery的Jakarta约束、Manage/DomainImpl的组调用消费；JDK类型参数由AdConfiguration供给，Common不反向导入业务实现。
- Why now: 本位置承接Step3前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: AdUrlValidator implements ConstraintValidator；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 复用S3公开base约束及点击URL边界实现；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-ARCH-001/MC-REUSE-001/MC-VALID-001/MC-LOG-001/MC-BEAN-001/MC-UTIL-001/MC-PATTERN-001/MC-SCOPE-001：保持所选层，实际注解/Bean/边界见pseudocode。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：业务@Slf4j、显式Bean名，@RequiredArgsConstructor和每final@Qualifier；JDK/现库，Jackson/Instant/Duration；selectedpattern不换反射分派。配置factory用现@Bean参数Qualifier，接口不伪构造器。
- Implementation pseudocode:

```java
@Component("adUrlValidator") @Slf4j @RequiredArgsConstructor;
@Qualifier("adImagePublicBase") final URI imagePublicBase (Bean inAdConfiguration, same existing objectStorage publicbase);
isValid(String uri): parse via JDK URI; reject userinfo/control/blank/overflow;
image variant: https + existing base scheme/host/effectiveport equality;
JDK URI decodedpath normalized dotsegments must stay withinbase path-segment boundary;
reject encoded escape/userinfo/control or credentials-bearing query keys, not naiveString.startsWith;
baseempty/unconfigured fails validation400, no inventedpublichost;
target variant: https absolute or single slash relative; reject //, javascript/data; no remote HTTP fetch;
reject decode/boundary tricks; use existing configured S3 public base, no SDK/dependency added.
```

- Verification contribution: Step 3的 `AdManageTest,AdPersistenceTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: AdUrlValidator implements ConstraintValidator的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 12 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/config/AdProperties.java`

- Purpose: 可校验的广告参数容器供约束/后续public读取使用
- Symbols: app.ad; publicEnabled; publicSites; allowedPositions; candidateLimit; publicLimit; loadTimeout
- Repository evidence: enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/config现Properties模式；Spec15.1
- Dependencies and consumers: AdConfiguration注册binder；AdPublicTenantFilter、AdRepository、Position/URL约束与NativeReadwriteContractTest消费配置。
- Why now: 本位置承接Step3前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: app.ad; publicEnabled; publicSites; allowedPositions; candidateLimit; publicLimit; loadTimeout；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 可校验的广告参数容器供约束/后续public读取使用；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-NAME-001/MC-VALID-001/MC-MODEL-001/MC-JSON-001/MC-TIME-001：普通class完整Lombok，root@Builder/继承@SuperBuilder+callSuper；Jakarta分组，UTC/wire/敏感字段按本file。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 6, Rule 10, Rule 11：@Data/@NoArgsConstructor/@AllArgsConstructor/@Accessors(chain=true)，匹配构造Builder；继承字段不重复；禁止record身份对象。
- Implementation pseudocode:

```java
@ConfigurationProperties("app.ad") @Validated class AdProperties;
class Lombok baseline; publicEnabled=false; publicSites Map hostname -> positive Long;
allowedPositions bounded explicit list; candidateLimit200/publicLimit20/loadTimeout Duration.ofSeconds(2);
canonical host collision rejects enable; hostname exact lower-case ASCII FQDN, no request tenant default;
publicEnable requires nonempty sites/positions; unset real inputs leave false, not autoenable;
mark binder unknown key rejection; no real DSN/secrets in object logs.
```

- Verification contribution: Step 3的 `AdManageTest,AdPersistenceTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: app.ad; publicEnabled; publicSites; allowedPositions; candidateLimit; publicLimit; loadTimeout的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 13 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/config/AdConfiguration.java`

- Purpose: 注册广告typed properties与纯domain资格/UTC Clock
- Symbols: adClock; adEligibilitySpecification; AdProperties binder
- Repository evidence: 现具名@Configuration与lombok.config；Spec8/10
- Dependencies and consumers: 本Step伪代码列明的原组件/生成输入和具名Bean，§5对应后继owner消费；commit scope仅本file和明确依赖。
- Why now: 本位置承接Step3前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: adClock; adEligibilitySpecification; AdProperties binder；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 注册广告typed properties与纯domain资格/UTC Clock；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-ARCH-001/MC-REUSE-001/MC-VALID-001/MC-LOG-001/MC-BEAN-001/MC-UTIL-001/MC-PATTERN-001/MC-SCOPE-001：保持所选层，实际注解/Bean/边界见pseudocode。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：业务@Slf4j、显式Bean名，@RequiredArgsConstructor和每final@Qualifier；JDK/现库，Jackson/Instant/Duration；selectedpattern不换反射分派。配置factory用现@Bean参数Qualifier，接口不伪构造器。
- Implementation pseudocode:

```java
@Configuration("adConfiguration",proxyBeanMethods=false) @EnableConfigurationProperties(AdProperties.class);
@Bean("adClock") Clock.systemUTC(); @Bean("adEligibilitySpecification") new AdEligibilitySpecification();
Spring配置不把framework注解放到pureDomain;
AdProperties binderBean qualifier use actual generated app.ad-top.egon.enjoyshop.goods.infrastructure.config.AdProperties;
@Bean("adAllowedPositions") List.copyOf(AdProperties.allowedPositions);
@Bean("adImagePublicBase") URI from existing app.object-storage.public-url-base using @Value onfactoryparameter;
if baseblank whileexistingstorageinputsnotconfigured, inject URI.create("") inertvalue ratherthanrequireexternalservice;
AdUrlValidator marks imageinvalid untilsameexistingS3base is configuredHTTPS; no fallbackhost/credentials;
publicenable neverneedsstaticnewRuntimeAddress auto-fill;
only JDK List/URI injected into commonvalidators, noCommon dependencyoninfra/start;
no datasource factory/no external serviceclient in config; primaryTransactionTemplate supplied Step5.
```

- Verification contribution: Step 3的 `AdManageTest,AdPersistenceTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: adClock; adEligibilitySpecification; AdProperties binder的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 14 — `MODIFY enjoyshop-service/enjoyshop-service-goods/codegen/ad-codegen.json`

- Purpose: 选择尚未生成的完整应用链catalog，不重生成已改文件
- Symbols: artifacts stage2; existingTypeMappings po/dao
- Repository evidence: enjoyshop-service/enjoyshop-service-goods/codegen/ad-codegen.json Step2与CLI converter扩展源码
- Dependencies and consumers: 本Step伪代码列明的原组件/生成输入和具名Bean，§5对应后继owner消费；commit scope仅本file和明确依赖。
- Why now: 本位置承接Step3前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: artifacts stage2; existingTypeMappings po/dao；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 选择尚未生成的完整应用链catalog，不重生成已改文件；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-SCOPE-001/MC-TEST-001；MC-CONFIG-001/MC-TIME-001：配置键/预算沿明确模板；不含secret/无自动运行。
- Literal rule enforcement: Rule 11：保留Light/本任务scope；Rule 7：此配置与同profile文件成套；Rule 10：预算/记录UTC单位明确。
- Implementation pseudocode:

```json
artifacts=[command,query,result,converter,domain-service,domain-impl,manage,manage-impl];
existingTypeMappings po=top.egon.enjoyshop.goods.infrastructure.ad.po.AdPO,
dao=top.egon.enjoyshop.goods.infrastructure.ad.dao.AdDAO (GenerationScopeValidator.qualified uses po/dao keys);
same schema source/hash; review plan excludes Step2 six customized paths;
apply only new catalog outputs; no controller yet; journal append actual successful apply.
```

- Verification contribution: Step 3的 `AdManageTest,AdPersistenceTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: artifacts stage2; existingTypeMappings po/dao的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 15 — `GENERATED enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/ad/CreateAdCommand.java`

- Purpose: 先由catalog生成暂态基线，再收敛到已批准对象，不保留额外业务类型
- Symbols: CreateAdCommand.java
- Repository evidence: CodegenCommand.emit与GenerationScopeValidator固定Create/Update/Delete/Detail及converter包
- Dependencies and consumers: AdController→AdManage/Impl→AdDomainService，MapStruct输入BO/结果投影与AdAuthorizationComponent；相关typedgroups/Common wrappers，不直接引用Repository/DAO。
- Why now: 本位置承接Step3前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: CreateAdCommand.java；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 先由catalog生成暂态基线，再收敛到已批准对象，不保留额外业务类型；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-NAME-001/MC-VALID-001/MC-MODEL-001/MC-JSON-001/MC-TIME-001：普通class完整Lombok，root@Builder/继承@SuperBuilder+callSuper；Jakarta分组，UTC/wire/敏感字段按本file。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 6, Rule 10, Rule 11：@Data/@NoArgsConstructor/@AllArgsConstructor/@Accessors(chain=true)，匹配构造Builder；继承字段不重复；禁止record身份对象。
- Implementation pseudocode:

```bash
初始来源只允许本Step已核对的 ad-codegen.json + E/scripts/egon-codegen.sh plan/apply；下列为命令、生成diff核对和允许业务定制目标，不允许手写初始类替代工具。
run stage2 verified plan/apply once (not handwritten body); inventory this exact transient path;
move to enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/ad/AdSaveCommand.java; update references in Manage/Domain/Converters before GREEN;
rename/merge only brand-new generated files, no unrelated source;
for Update/Delete converter merge named toUpdate/toDelete mapping into approved AdApplicationConverter;
remove unused temp class paths and validate rg finds no CreateAdCommand/UpdateAdCommand/DeleteAdCommand/AdDetailQuery leftovers;
retain .egon ownership/history unchanged: later check detects manual divergence, never rerun overwrite;
final path implementation described below; do not commit transient unused source.
```

- Verification contribution: Step 3的 `AdManageTest,AdPersistenceTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: CreateAdCommand.java的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。暂态源最终不保留，原消费者已由该Step替换。

#### File 16 — `GENERATED enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/ad/UpdateAdCommand.java`

- Purpose: 先由catalog生成暂态基线，再收敛到已批准对象，不保留额外业务类型
- Symbols: UpdateAdCommand.java
- Repository evidence: CodegenCommand.emit与GenerationScopeValidator固定Create/Update/Delete/Detail及converter包
- Dependencies and consumers: AdController→AdManage/Impl→AdDomainService，MapStruct输入BO/结果投影与AdAuthorizationComponent；相关typedgroups/Common wrappers，不直接引用Repository/DAO。
- Why now: 本位置承接Step3前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: UpdateAdCommand.java；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 先由catalog生成暂态基线，再收敛到已批准对象，不保留额外业务类型；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-NAME-001/MC-VALID-001/MC-MODEL-001/MC-JSON-001/MC-TIME-001：普通class完整Lombok，root@Builder/继承@SuperBuilder+callSuper；Jakarta分组，UTC/wire/敏感字段按本file。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 6, Rule 10, Rule 11：@Data/@NoArgsConstructor/@AllArgsConstructor/@Accessors(chain=true)，匹配构造Builder；继承字段不重复；禁止record身份对象。
- Implementation pseudocode:

```bash
初始来源只允许本Step已核对的 ad-codegen.json + E/scripts/egon-codegen.sh plan/apply；下列为命令、生成diff核对和允许业务定制目标，不允许手写初始类替代工具。
run stage2 verified plan/apply once (not handwritten body); inventory this exact transient path;
delete after all generated references replaced; update references in Manage/Domain/Converters before GREEN;
rename/merge only brand-new generated files, no unrelated source;
for Update/Delete converter merge named toUpdate/toDelete mapping into approved AdApplicationConverter;
remove unused temp class paths and validate rg finds no CreateAdCommand/UpdateAdCommand/DeleteAdCommand/AdDetailQuery leftovers;
retain .egon ownership/history unchanged: later check detects manual divergence, never rerun overwrite;
final path implementation described below; do not commit transient unused source.
```

- Verification contribution: Step 3的 `AdManageTest,AdPersistenceTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: UpdateAdCommand.java的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。暂态源最终不保留，原消费者已由该Step替换。

#### File 17 — `GENERATED enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/ad/DeleteAdCommand.java`

- Purpose: 先由catalog生成暂态基线，再收敛到已批准对象，不保留额外业务类型
- Symbols: DeleteAdCommand.java
- Repository evidence: CodegenCommand.emit与GenerationScopeValidator固定Create/Update/Delete/Detail及converter包
- Dependencies and consumers: AdController→AdManage/Impl→AdDomainService，MapStruct输入BO/结果投影与AdAuthorizationComponent；相关typedgroups/Common wrappers，不直接引用Repository/DAO。
- Why now: 本位置承接Step3前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: DeleteAdCommand.java；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 先由catalog生成暂态基线，再收敛到已批准对象，不保留额外业务类型；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-NAME-001/MC-VALID-001/MC-MODEL-001/MC-JSON-001/MC-TIME-001：普通class完整Lombok，root@Builder/继承@SuperBuilder+callSuper；Jakarta分组，UTC/wire/敏感字段按本file。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 6, Rule 10, Rule 11：@Data/@NoArgsConstructor/@AllArgsConstructor/@Accessors(chain=true)，匹配构造Builder；继承字段不重复；禁止record身份对象。
- Implementation pseudocode:

```bash
初始来源只允许本Step已核对的 ad-codegen.json + E/scripts/egon-codegen.sh plan/apply；下列为命令、生成diff核对和允许业务定制目标，不允许手写初始类替代工具。
run stage2 verified plan/apply once (not handwritten body); inventory this exact transient path;
move to enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/ad/AdLifecycleCommand.java; update references in Manage/Domain/Converters before GREEN;
rename/merge only brand-new generated files, no unrelated source;
for Update/Delete converter merge named toUpdate/toDelete mapping into approved AdApplicationConverter;
remove unused temp class paths and validate rg finds no CreateAdCommand/UpdateAdCommand/DeleteAdCommand/AdDetailQuery leftovers;
retain .egon ownership/history unchanged: later check detects manual divergence, never rerun overwrite;
final path implementation described below; do not commit transient unused source.
```

- Verification contribution: Step 3的 `AdManageTest,AdPersistenceTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: DeleteAdCommand.java的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。暂态源最终不保留，原消费者已由该Step替换。

#### File 18 — `GENERATED enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/ad/AdDetailQuery.java`

- Purpose: 先由catalog生成暂态基线，再收敛到已批准对象，不保留额外业务类型
- Symbols: AdDetailQuery.java
- Repository evidence: CodegenCommand.emit与GenerationScopeValidator固定Create/Update/Delete/Detail及converter包
- Dependencies and consumers: AdController→AdManage/Impl→AdDomainService，MapStruct输入BO/结果投影与AdAuthorizationComponent；相关typedgroups/Common wrappers，不直接引用Repository/DAO。
- Why now: 本位置承接Step3前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: AdDetailQuery.java；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 先由catalog生成暂态基线，再收敛到已批准对象，不保留额外业务类型；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-NAME-001/MC-VALID-001/MC-MODEL-001/MC-JSON-001/MC-TIME-001：普通class完整Lombok，root@Builder/继承@SuperBuilder+callSuper；Jakarta分组，UTC/wire/敏感字段按本file。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 6, Rule 10, Rule 11：@Data/@NoArgsConstructor/@AllArgsConstructor/@Accessors(chain=true)，匹配构造Builder；继承字段不重复；禁止record身份对象。
- Implementation pseudocode:

```bash
初始来源只允许本Step已核对的 ad-codegen.json + E/scripts/egon-codegen.sh plan/apply；下列为命令、生成diff核对和允许业务定制目标，不允许手写初始类替代工具。
run stage2 verified plan/apply once (not handwritten body); inventory this exact transient path;
move to enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/ad/AdReadQuery.java; update references in Manage/Domain/Converters before GREEN;
rename/merge only brand-new generated files, no unrelated source;
for Update/Delete converter merge named toUpdate/toDelete mapping into approved AdApplicationConverter;
remove unused temp class paths and validate rg finds no CreateAdCommand/UpdateAdCommand/DeleteAdCommand/AdDetailQuery leftovers;
retain .egon ownership/history unchanged: later check detects manual divergence, never rerun overwrite;
final path implementation described below; do not commit transient unused source.
```

- Verification contribution: Step 3的 `AdManageTest,AdPersistenceTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: AdDetailQuery.java的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。暂态源最终不保留，原消费者已由该Step替换。

#### File 19 — `GENERATED enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/ad/converter/CreateAdCommandConverter.java`

- Purpose: 先由catalog生成暂态基线，再收敛到已批准对象，不保留额外业务类型
- Symbols: CreateAdCommandConverter.java
- Repository evidence: CodegenCommand.emit与GenerationScopeValidator固定Create/Update/Delete/Detail及converter包
- Dependencies and consumers: AdController→AdManage/Impl→AdDomainService，MapStruct输入BO/结果投影与AdAuthorizationComponent；相关typedgroups/Common wrappers，不直接引用Repository/DAO。
- Why now: 本位置承接Step3前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: CreateAdCommandConverter.java；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 先由catalog生成暂态基线，再收敛到已批准对象，不保留额外业务类型；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-CONVERT-001/MC-BEAN-001/MC-JSON-001/MC-TIME-001：MapStruct/Common基类与准确generics/mapping、实现Bean名、无反射/BeanUtils搬字段。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 10, Rule 11：保留BaseConverter/BaseForwardConverter；@Mapper spring ERROR；消费者final@Qualifier实际生成Impl名；默认转换不得吞null/unknown。
- Implementation pseudocode:

```bash
初始来源只允许本Step已核对的 ad-codegen.json + E/scripts/egon-codegen.sh plan/apply；下列为命令、生成diff核对和允许业务定制目标，不允许手写初始类替代工具。
run stage2 verified plan/apply once (not handwritten body); inventory this exact transient path;
move to enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/ad/AdApplicationConverter.java; update references in Manage/Domain/Converters before GREEN;
rename/merge only brand-new generated files, no unrelated source;
for Update/Delete converter merge named toUpdate/toDelete mapping into approved AdApplicationConverter;
remove unused temp class paths and validate rg finds no CreateAdCommand/UpdateAdCommand/DeleteAdCommand/AdDetailQuery leftovers;
retain .egon ownership/history unchanged: later check detects manual divergence, never rerun overwrite;
final path implementation described below; do not commit transient unused source.
```

- Verification contribution: Step 3的 `AdManageTest,AdPersistenceTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: CreateAdCommandConverter.java的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。暂态源最终不保留，原消费者已由该Step替换。

#### File 20 — `GENERATED enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/ad/converter/UpdateAdCommandConverter.java`

- Purpose: 先由catalog生成暂态基线，再收敛到已批准对象，不保留额外业务类型
- Symbols: UpdateAdCommandConverter.java
- Repository evidence: CodegenCommand.emit与GenerationScopeValidator固定Create/Update/Delete/Detail及converter包
- Dependencies and consumers: AdController→AdManage/Impl→AdDomainService，MapStruct输入BO/结果投影与AdAuthorizationComponent；相关typedgroups/Common wrappers，不直接引用Repository/DAO。
- Why now: 本位置承接Step3前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: UpdateAdCommandConverter.java；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 先由catalog生成暂态基线，再收敛到已批准对象，不保留额外业务类型；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-CONVERT-001/MC-BEAN-001/MC-JSON-001/MC-TIME-001：MapStruct/Common基类与准确generics/mapping、实现Bean名、无反射/BeanUtils搬字段。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 10, Rule 11：保留BaseConverter/BaseForwardConverter；@Mapper spring ERROR；消费者final@Qualifier实际生成Impl名；默认转换不得吞null/unknown。
- Implementation pseudocode:

```bash
初始来源只允许本Step已核对的 ad-codegen.json + E/scripts/egon-codegen.sh plan/apply；下列为命令、生成diff核对和允许业务定制目标，不允许手写初始类替代工具。
run stage2 verified plan/apply once (not handwritten body); inventory this exact transient path;
delete after all generated references replaced; update references in Manage/Domain/Converters before GREEN;
rename/merge only brand-new generated files, no unrelated source;
for Update/Delete converter merge named toUpdate/toDelete mapping into approved AdApplicationConverter;
remove unused temp class paths and validate rg finds no CreateAdCommand/UpdateAdCommand/DeleteAdCommand/AdDetailQuery leftovers;
retain .egon ownership/history unchanged: later check detects manual divergence, never rerun overwrite;
final path implementation described below; do not commit transient unused source.
```

- Verification contribution: Step 3的 `AdManageTest,AdPersistenceTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: UpdateAdCommandConverter.java的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。暂态源最终不保留，原消费者已由该Step替换。

#### File 21 — `GENERATED enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/ad/converter/DeleteAdCommandConverter.java`

- Purpose: 先由catalog生成暂态基线，再收敛到已批准对象，不保留额外业务类型
- Symbols: DeleteAdCommandConverter.java
- Repository evidence: CodegenCommand.emit与GenerationScopeValidator固定Create/Update/Delete/Detail及converter包
- Dependencies and consumers: AdController→AdManage/Impl→AdDomainService，MapStruct输入BO/结果投影与AdAuthorizationComponent；相关typedgroups/Common wrappers，不直接引用Repository/DAO。
- Why now: 本位置承接Step3前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: DeleteAdCommandConverter.java；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 先由catalog生成暂态基线，再收敛到已批准对象，不保留额外业务类型；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-CONVERT-001/MC-BEAN-001/MC-JSON-001/MC-TIME-001：MapStruct/Common基类与准确generics/mapping、实现Bean名、无反射/BeanUtils搬字段。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 10, Rule 11：保留BaseConverter/BaseForwardConverter；@Mapper spring ERROR；消费者final@Qualifier实际生成Impl名；默认转换不得吞null/unknown。
- Implementation pseudocode:

```bash
初始来源只允许本Step已核对的 ad-codegen.json + E/scripts/egon-codegen.sh plan/apply；下列为命令、生成diff核对和允许业务定制目标，不允许手写初始类替代工具。
run stage2 verified plan/apply once (not handwritten body); inventory this exact transient path;
delete after all generated references replaced; update references in Manage/Domain/Converters before GREEN;
rename/merge only brand-new generated files, no unrelated source;
for Update/Delete converter merge named toUpdate/toDelete mapping into approved AdApplicationConverter;
remove unused temp class paths and validate rg finds no CreateAdCommand/UpdateAdCommand/DeleteAdCommand/AdDetailQuery leftovers;
retain .egon ownership/history unchanged: later check detects manual divergence, never rerun overwrite;
final path implementation described below; do not commit transient unused source.
```

- Verification contribution: Step 3的 `AdManageTest,AdPersistenceTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: DeleteAdCommandConverter.java的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。暂态源最终不保留，原消费者已由该Step替换。

#### File 22 — `GENERATED enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/ad/converter/AdResultConverter.java`

- Purpose: 先由catalog生成暂态基线，再收敛到已批准对象，不保留额外业务类型
- Symbols: AdResultConverter.java
- Repository evidence: CodegenCommand.emit与GenerationScopeValidator固定Create/Update/Delete/Detail及converter包
- Dependencies and consumers: AdController→AdManage/Impl→AdDomainService，MapStruct输入BO/结果投影与AdAuthorizationComponent；相关typedgroups/Common wrappers，不直接引用Repository/DAO。
- Why now: 本位置承接Step3前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: AdResultConverter.java；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 先由catalog生成暂态基线，再收敛到已批准对象，不保留额外业务类型；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-CONVERT-001/MC-BEAN-001/MC-JSON-001/MC-TIME-001：MapStruct/Common基类与准确generics/mapping、实现Bean名、无反射/BeanUtils搬字段。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 10, Rule 11：保留BaseConverter/BaseForwardConverter；@Mapper spring ERROR；消费者final@Qualifier实际生成Impl名；默认转换不得吞null/unknown。
- Implementation pseudocode:

```bash
初始来源只允许本Step已核对的 ad-codegen.json + E/scripts/egon-codegen.sh plan/apply；下列为命令、生成diff核对和允许业务定制目标，不允许手写初始类替代工具。
run stage2 verified plan/apply once (not handwritten body); inventory this exact transient path;
move to enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/ad/AdResultConverter.java; update references in Manage/Domain/Converters before GREEN;
rename/merge only brand-new generated files, no unrelated source;
for Update/Delete converter merge named toUpdate/toDelete mapping into approved AdApplicationConverter;
remove unused temp class paths and validate rg finds no CreateAdCommand/UpdateAdCommand/DeleteAdCommand/AdDetailQuery leftovers;
retain .egon ownership/history unchanged: later check detects manual divergence, never rerun overwrite;
final path implementation described below; do not commit transient unused source.
```

- Verification contribution: Step 3的 `AdManageTest,AdPersistenceTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: AdResultConverter.java的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。暂态源最终不保留，原消费者已由该Step替换。

#### File 23 — `RENAME enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/ad/AdSaveCommand.java`

- Purpose: 收敛AdSaveCommand到批准管理合同
- Symbols: AdSaveCommand
- Repository evidence: stage2 catalog输出与Spec §9/10固定字段
- Dependencies and consumers: AdController→AdManage/Impl→AdDomainService，MapStruct输入BO/结果投影与AdAuthorizationComponent；相关typedgroups/Common wrappers，不直接引用Repository/DAO。
- Why now: 本位置承接Step3前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: AdSaveCommand；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 收敛AdSaveCommand到批准管理合同；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-NAME-001/MC-VALID-001/MC-MODEL-001/MC-JSON-001/MC-TIME-001：普通class完整Lombok，root@Builder/继承@SuperBuilder+callSuper；Jakarta分组，UTC/wire/敏感字段按本file。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 6, Rule 10, Rule 11：@Data/@NoArgsConstructor/@AllArgsConstructor/@Accessors(chain=true)，匹配构造Builder；继承字段不重复；禁止record身份对象。
- Implementation pseudocode:

```java
customize generated CreateAdCommand -> AdSaveCommand; retain complete class Lombok;
fields name,position,imageUrl,targetUrl,status AdStatusEnum,startAt/endAt Instant,sortOrder,version Long;
@Null(Create) version; @NotNull(Update) @Min(0) version; 8 fields @NotNull(Write), native length/bounds/custom constraints;
@JsonAnySetter unknown(String,Object) rejects; disable ambiguous AllArgs JsonCreator along ProductSaveCommand;
Create/Update share this class; no id/tenant/actor/deletedAt writable body.
Nested StrictLongDeserializer/StrictIntegerDeserializer use Jackson StdDeserializer: only null when group allows or integral tokens within Long/Integer bounds; reject float/string/boolean before BeanValidation. Apply @JsonDeserialize to version/sortOrder;
nested StrictInstantDeserializer for startAt/endAt accepts textual ISO offset/Z only, explicitfractiondigits<=6,
then Instant parse UTC; rejects9-digit trailingzero strings even when nano%1000==0; no new file/dependency;
retain full Lombok input class and group-based presence/null constraints.
```

- Verification contribution: Step 3的 `AdManageTest,AdPersistenceTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: AdSaveCommand的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 24 — `RENAME enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/ad/AdLifecycleCommand.java`

- Purpose: 收敛AdLifecycleCommand到批准管理合同
- Symbols: AdLifecycleCommand
- Repository evidence: stage2 catalog输出与Spec §9/10固定字段
- Dependencies and consumers: AdController→AdManage/Impl→AdDomainService，MapStruct输入BO/结果投影与AdAuthorizationComponent；相关typedgroups/Common wrappers，不直接引用Repository/DAO。
- Why now: 本位置承接Step3前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: AdLifecycleCommand；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 收敛AdLifecycleCommand到批准管理合同；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-NAME-001/MC-VALID-001/MC-MODEL-001/MC-JSON-001/MC-TIME-001：普通class完整Lombok，root@Builder/继承@SuperBuilder+callSuper；Jakarta分组，UTC/wire/敏感字段按本file。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 6, Rule 10, Rule 11：@Data/@NoArgsConstructor/@AllArgsConstructor/@Accessors(chain=true)，匹配构造Builder；继承字段不重复；禁止record身份对象。
- Implementation pseudocode:

```java
customize generated DeleteAdCommand -> AdLifecycleCommand; id field adId @Positive/@NotNull(Delete),
version @NotNull/@Min(0)(Delete); no HTTP body; adapter binds path/query; only delete task;
class baseline + @Builder; rejects fractional/nonnumeric values at HTTP binding.
```

- Verification contribution: Step 3的 `AdManageTest,AdPersistenceTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: AdLifecycleCommand的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 25 — `RENAME enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/ad/AdReadQuery.java`

- Purpose: 收敛AdReadQuery到批准管理合同
- Symbols: AdReadQuery
- Repository evidence: stage2 catalog输出与Spec §9/10固定字段
- Dependencies and consumers: AdController→AdManage/Impl→AdDomainService，MapStruct输入BO/结果投影与AdAuthorizationComponent；相关typedgroups/Common wrappers，不直接引用Repository/DAO。
- Why now: 本位置承接Step3前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: AdReadQuery；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 收敛AdReadQuery到批准管理合同；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-NAME-001/MC-VALID-001/MC-MODEL-001/MC-JSON-001/MC-TIME-001：普通class完整Lombok，root@Builder/继承@SuperBuilder+callSuper；Jakarta分组，UTC/wire/敏感字段按本file。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 6, Rule 10, Rule 11：@Data/@NoArgsConstructor/@AllArgsConstructor/@Accessors(chain=true)，匹配构造Builder；继承字段不重复；禁止record身份对象。
- Implementation pseudocode:

```java
customize generated AdDetailQuery -> AdReadQuery; public input position+limit default20;
position @ValidAdPosition, limit1..20; trusted tenant is assigned context by filter, not query;
no adId in this class; admin detail still verified Long path argument.
```

- Verification contribution: Step 3的 `AdManageTest,AdPersistenceTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: AdReadQuery的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 26 — `GENERATED enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/ad/AdPageQuery.java`

- Purpose: 收敛AdPageQuery到批准管理合同
- Symbols: AdPageQuery
- Repository evidence: stage2 catalog输出与Spec §9/10固定字段
- Dependencies and consumers: AdController→AdManage/Impl→AdDomainService，MapStruct输入BO/结果投影与AdAuthorizationComponent；相关typedgroups/Common wrappers，不直接引用Repository/DAO。
- Why now: 本位置承接Step3前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: AdPageQuery；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 收敛AdPageQuery到批准管理合同；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-NAME-001/MC-VALID-001/MC-MODEL-001/MC-JSON-001/MC-TIME-001：普通class完整Lombok，root@Builder/继承@SuperBuilder+callSuper；Jakarta分组，UTC/wire/敏感字段按本file。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 6, Rule 10, Rule 11：@Data/@NoArgsConstructor/@AllArgsConstructor/@Accessors(chain=true)，匹配构造Builder；继承字段不重复；禁止record身份对象。
- Implementation pseudocode:

```java
初始来源只允许本Step已核对的 ad-codegen.json + E/scripts/egon-codegen.sh plan/apply；下列为命令、生成diff核对和允许业务定制目标，不允许手写初始类替代工具。
generated query catalog baseline then customize class with pageNo1/default1,max1000000;
pageSize1..100/default20; optional position/status nonblank/known value if supplied;
do not extend record PageQuery: keep primitive input fields and converter creates existing PageQuery for domain;
no dynamic sort/name filters, unknown/repeated query rejected by Controller Step4.
```

- Verification contribution: Step 3的 `AdManageTest,AdPersistenceTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: AdPageQuery的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 27 — `GENERATED enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/ad/AdResult.java`

- Purpose: 收敛AdResult到批准管理合同
- Symbols: AdResult
- Repository evidence: stage2 catalog输出与Spec §9/10固定字段
- Dependencies and consumers: AdController→AdManage/Impl→AdDomainService，MapStruct输入BO/结果投影与AdAuthorizationComponent；相关typedgroups/Common wrappers，不直接引用Repository/DAO。
- Why now: 本位置承接Step3前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: AdResult；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 收敛AdResult到批准管理合同；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-NAME-001/MC-VALID-001/MC-MODEL-001/MC-JSON-001/MC-TIME-001：普通class完整Lombok，root@Builder/继承@SuperBuilder+callSuper；Jakarta分组，UTC/wire/敏感字段按本file。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 6, Rule 10, Rule 11：@Data/@NoArgsConstructor/@AllArgsConstructor/@Accessors(chain=true)，匹配构造Builder；继承字段不重复；禁止record身份对象。
- Implementation pseudocode:

```java
初始来源只允许本Step已核对的 ad-codegen.json + E/scripts/egon-codegen.sh plan/apply；下列为命令、生成diff核对和允许业务定制目标，不允许手写初始类替代工具。
generated result then customize: id string; name,position,imageUrl,targetUrl,status,startAt,endAt,sortOrder,version;
no tenant/actor/audit/deletedAt; Instant microseconds UTC serialization; version numeric int64;
full class Lombok, @Schema field descriptions; use original ResultRecord/PageResultRecord separately.
```

- Verification contribution: Step 3的 `AdManageTest,AdPersistenceTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: AdResult的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 28 — `RENAME enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/ad/AdApplicationConverter.java`

- Purpose: 收敛AdApplicationConverter extends BaseForwardConverter<AdSaveCommand,AdBO>到批准管理合同
- Symbols: AdApplicationConverter extends BaseForwardConverter<AdSaveCommand,AdBO>
- Repository evidence: stage2 catalog输出与Spec §9/10固定字段
- Dependencies and consumers: AdController→AdManage/Impl→AdDomainService，MapStruct输入BO/结果投影与AdAuthorizationComponent；相关typedgroups/Common wrappers，不直接引用Repository/DAO。
- Why now: 本位置承接Step3前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: AdApplicationConverter extends BaseForwardConverter<AdSaveCommand,AdBO>；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 收敛AdApplicationConverter extends BaseForwardConverter<AdSaveCommand,AdBO>到批准管理合同；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-CONVERT-001/MC-BEAN-001/MC-JSON-001/MC-TIME-001：MapStruct/Common基类与准确generics/mapping、实现Bean名、无反射/BeanUtils搬字段。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 10, Rule 11：保留BaseConverter/BaseForwardConverter；@Mapper spring ERROR；消费者final@Qualifier实际生成Impl名；默认转换不得吞null/unknown。
- Implementation pseudocode:

```java
move/merge generated command converters to @Mapper(componentModel="spring",unmappedTargetPolicy=ERROR,builder disableBuilder=true);
toTarget(AdSaveCommand) ->AdBO eight fields/version; verified tenant/actor not sourced from body;
toUpdateTarget(Long adId,AdSaveCommand) ->AdBO.id/version; toDeleteTarget(AdLifecycleCommand) ->id/version;
toDomainQuery(AdPageQuery) ->new common PageQuery(pageNo,pageSize),position/status;
query scalar overload for detail id; no hand-written property copies; explicit ignored fields for context fill;
Bean adApplicationConverterImpl, qualifier on Manage; no Query mutation to infer identity.
```

- Verification contribution: Step 3的 `AdManageTest,AdPersistenceTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: AdApplicationConverter extends BaseForwardConverter<AdSaveCommand,AdBO>的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 29 — `RENAME enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/ad/AdResultConverter.java`

- Purpose: 收敛AdResultConverter extends BaseForwardConverter<AdBO,AdResult>到批准管理合同
- Symbols: AdResultConverter extends BaseForwardConverter<AdBO,AdResult>
- Repository evidence: stage2 catalog输出与Spec §9/10固定字段
- Dependencies and consumers: AdController→AdManage/Impl→AdDomainService，MapStruct输入BO/结果投影与AdAuthorizationComponent；相关typedgroups/Common wrappers，不直接引用Repository/DAO。
- Why now: 本位置承接Step3前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: AdResultConverter extends BaseForwardConverter<AdBO,AdResult>；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 收敛AdResultConverter extends BaseForwardConverter<AdBO,AdResult>到批准管理合同；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-CONVERT-001/MC-BEAN-001/MC-JSON-001/MC-TIME-001：MapStruct/Common基类与准确generics/mapping、实现Bean名、无反射/BeanUtils搬字段。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 10, Rule 11：保留BaseConverter/BaseForwardConverter；@Mapper spring ERROR；消费者final@Qualifier实际生成Impl名；默认转换不得吞null/unknown。
- Implementation pseudocode:

```java
move generated result converter to approved flat application/ad package;
MapStruct explicit id Long->decimal string via existing serializer/conversion expression;
startAt/endAt alreadyInstant, status @JsonValue; ignore all internal tenant/audit/deleted fields;
Bean adResultConverterImpl; preserve PageResultRecord records/page wrapper from Manage.
```

- Verification contribution: Step 3的 `AdManageTest,AdPersistenceTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: AdResultConverter extends BaseForwardConverter<AdBO,AdResult>的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 30 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/ad/AdBO.java`

- Purpose: 补足业务约束和受信上下文字段
- Symbols: AdBO id/version/8business/tenant/audit/deletedAt
- Repository evidence: enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/SpuBO.java及用户批准Domain Jakarta修正
- Dependencies and consumers: AdManage→AdDomainService/Impl→AdRepository→AdDAO/XML；MP-SDJ/CommonModel、namedConverters、CachePort/primaryTemplate通过明确Bean接入，Domain port不引用DAO。
- Why now: 本位置承接Step3前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: AdBO id/version/8business/tenant/audit/deletedAt；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 补足业务约束和受信上下文字段；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-NAME-001/MC-VALID-001/MC-MODEL-001/MC-JSON-001/MC-TIME-001：普通class完整Lombok，root@Builder/继承@SuperBuilder+callSuper；Jakarta分组，UTC/wire/敏感字段按本file。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 6, Rule 10, Rule 11：@Data/@NoArgsConstructor/@AllArgsConstructor/@Accessors(chain=true)，匹配构造Builder；继承字段不重复；禁止record身份对象。
- Implementation pseudocode:

```java
class baseline; id @Null(Create)/@NotNull(Update/Delete), @Positive; version @Null(Create)/@Min(0)/requiredUpdateDelete;
8business fields native+custom Write constraints; @ValidAdWindow on Write;
tenantId internal positive; create/updateUserId and auditInstant/deletedAt UTC LocalDateTime optional read context;
status enum; startAt/endAt Instant; no Spring/MP/Security/Rabbit imports;
Create validation before MP fill allows nulltechnicalid/version; Persisted PO validation after fill remains Common owned.
```

- Verification contribution: Step 3的 `AdManageTest,AdPersistenceTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: AdBO id/version/8business/tenant/audit/deletedAt的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 31 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/ad/AdDomainQuery.java`

- Purpose: 领域查询条件避免Spring分页依赖
- Symbols: AdDomainQuery id/position/status/page
- Repository evidence: enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/ProductDomainQuery.java及生成Step2
- Dependencies and consumers: AdManage→AdDomainService/Impl→AdRepository→AdDAO/XML；MP-SDJ/CommonModel、namedConverters、CachePort/primaryTemplate通过明确Bean接入，Domain port不引用DAO。
- Why now: 本位置承接Step3前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: AdDomainQuery id/position/status/page；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 领域查询条件避免Spring分页依赖；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-NAME-001/MC-VALID-001/MC-MODEL-001/MC-JSON-001/MC-TIME-001：普通class完整Lombok，root@Builder/继承@SuperBuilder+callSuper；Jakarta分组，UTC/wire/敏感字段按本file。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 6, Rule 10, Rule 11：@Data/@NoArgsConstructor/@AllArgsConstructor/@Accessors(chain=true)，匹配构造Builder；继承字段不重复；禁止record身份对象。
- Implementation pseudocode:

```java
class baseline; id positive @NotNull Detail; page common PageQuery @Valid @NotNull Page;
position optional @ValidAdPosition for Page, required PublicRead; status optional enum;
input tenant derived from context not untrusted query; group Detail/Page/PublicRead;
read-only queries no writes/Event publish; no query wrapper bypass.
```

- Verification contribution: Step 3的 `AdManageTest,AdPersistenceTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: AdDomainQuery id/position/status/page的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 32 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/ad/po/AdPO.java`

- Purpose: 把PO业务码接入enum且保留DB形状约束
- Symbols: status AdStatusEnum; Instant startTime/endTime; 8 business fields
- Repository evidence: AdPO Step2与EgonModel/MP validation组
- Dependencies and consumers: AdManage→AdDomainService/Impl→AdRepository→AdDAO/XML；MP-SDJ/CommonModel、namedConverters、CachePort/primaryTemplate通过明确Bean接入，Domain port不引用DAO。
- Why now: 本位置承接Step3前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: status AdStatusEnum; Instant startTime/endTime; 8 business fields；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 把PO业务码接入enum且保留DB形状约束；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-NAME-001/MC-VALID-001/MC-MODEL-001/MC-JSON-001/MC-TIME-001：普通class完整Lombok，root@Builder/继承@SuperBuilder+callSuper；Jakarta分组，UTC/wire/敏感字段按本file。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 6, Rule 10, Rule 11：@Data/@NoArgsConstructor/@AllArgsConstructor/@Accessors(chain=true)，匹配构造Builder；继承字段不重复；禁止record身份对象。
- Implementation pseudocode:

```java
keep class extends EgonModel<AdPO>, @SuperBuilder/callSuper true;
status AdStatusEnum with @EnumValue code; remove generated String-only @Pattern on this enum field, enum factory and DB CHECK define its closed values; fields @NotNull(Insert/Persisted), @Size only String DB lengths;
numeric sort0..1000000; time CHECK at owning service + DB;
8common inherited only; update omitted-field semantics use Common groups; no generic business DTO validation in DAO.
```

- Verification contribution: Step 3的 `AdManageTest,AdPersistenceTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: status AdStatusEnum; Instant startTime/endTime; 8 business fields的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 33 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/ad/AdPublicResult.java`

- Purpose: 公开投影作为Manage签名依赖，HTTP在Step4才出现
- Symbols: AdPublicResult id/name/imageUrl/targetUrl
- Repository evidence: Spec §9 API006及§10投影；原Product Result建模
- Dependencies and consumers: AdController→AdManage/Impl→AdDomainService，MapStruct输入BO/结果投影与AdAuthorizationComponent；相关typedgroups/Common wrappers，不直接引用Repository/DAO。
- Why now: 本位置承接Step3前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: AdPublicResult id/name/imageUrl/targetUrl；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 公开投影作为Manage签名依赖，HTTP在Step4才出现；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-NAME-001/MC-VALID-001/MC-MODEL-001/MC-JSON-001/MC-TIME-001：普通class完整Lombok，root@Builder/继承@SuperBuilder+callSuper；Jakarta分组，UTC/wire/敏感字段按本file。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 6, Rule 10, Rule 11：@Data/@NoArgsConstructor/@AllArgsConstructor/@Accessors(chain=true)，匹配构造Builder；继承字段不重复；禁止record身份对象。
- Implementation pseudocode:

```java
class full Lombok baseline @Builder; String id/name/imageUrl/targetUrl only;
no tenant/status/version/window/audit/deleted fields; SpringJackson defaults match exact4-key object;
@Schema documents decimalStringID and allowedlink constraints; no fakewrapper subtype.
```

- Verification contribution: Step 3的 `AdManageTest,AdPersistenceTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: AdPublicResult id/name/imageUrl/targetUrl的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 34 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/ad/AdPublicResultConverter.java`

- Purpose: 可逆信息丢失明确的公开MapStruct投影
- Symbols: BaseForwardConverter<AdBO,AdPublicResult>
- Repository evidence: 现ProductSpuResultConverter/BaseForwardConverter与Spec10.4
- Dependencies and consumers: AdController→AdManage/Impl→AdDomainService，MapStruct输入BO/结果投影与AdAuthorizationComponent；相关typedgroups/Common wrappers，不直接引用Repository/DAO。
- Why now: 本位置承接Step3前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: BaseForwardConverter<AdBO,AdPublicResult>；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 可逆信息丢失明确的公开MapStruct投影；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-CONVERT-001/MC-BEAN-001/MC-JSON-001/MC-TIME-001：MapStruct/Common基类与准确generics/mapping、实现Bean名、无反射/BeanUtils搬字段。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 10, Rule 11：保留BaseConverter/BaseForwardConverter；@Mapper spring ERROR；消费者final@Qualifier实际生成Impl名；默认转换不得吞null/unknown。
- Implementation pseudocode:

```java
@Mapper spring ERROR disableBuilder; Long id->decimal string; name/imageUrl/targetUrl same semantics;
ignore all internal fields by destination white-list; generatedBean adPublicResultConverterImpl;
no copying PO directly to HTTP, no extra roundtrip or queryperfield.
```

- Verification contribution: Step 3的 `AdManageTest,AdPersistenceTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: BaseForwardConverter<AdBO,AdPublicResult>的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 35 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/ad/AdEligibilitySpecification.java`

- Purpose: 按传入同一次时钟筛资格
- Symbols: boolean isSatisfiedBy(AdBO ad, Instant now)
- Repository evidence: enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/ProductLifecycleComponent.java及Spec13.1
- Dependencies and consumers: AdManage→AdDomainService/Impl→AdRepository→AdDAO/XML；MP-SDJ/CommonModel、namedConverters、CachePort/primaryTemplate通过明确Bean接入，Domain port不引用DAO。
- Why now: 本位置承接Step3前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: boolean isSatisfiedBy(AdBO ad, Instant now)；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 按传入同一次时钟筛资格；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-PATTERN-001/MC-LOG-001/MC-BEAN-001/MC-TIME-001：pure predicate @Slf4j，无Spring import；AdConfiguration @Bean明确名；时间从参数传入。
- Literal rule enforcement: Rule 1, Rule 4, Rule 9, Rule 10, Rule 11：纯领域资格Specification，Clock/Cache不进domain，实例由具名@Bean提供；无依赖无需构造注入。
- Implementation pseudocode:

```java
pureDomain class @Slf4j; noSpringClock/Cache/MQ;
isSatisfiedBy(ad,now): ad.deletedAt==null && status==ENABLED && startAt<=now && now<endAt;
null/invalidpersistedfield ->610310 at owning service before predicate, not quietlyskip;
only predicate shared, no polling/job/genericruleengine; Bean supplied by AdConfiguration.
```

- Verification contribution: Step 3的 `AdManageTest,AdPersistenceTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: boolean isSatisfiedBy(AdBO ad, Instant now)的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 36 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/ad/dao/AdDAO.java`

- Purpose: 只提供明确tenant/lifecycle查询与版本删除
- Symbols: selectByLifecycle; lockByLifecycle; selectPage; countPage; selectCandidates; deleteVersionedById
- Repository evidence: enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/dao/SpuDAO.java与EgonColaMapper
- Dependencies and consumers: AdManage→AdDomainService/Impl→AdRepository→AdDAO/XML；MP-SDJ/CommonModel、namedConverters、CachePort/primaryTemplate通过明确Bean接入，Domain port不引用DAO。
- Why now: 本位置承接Step3前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: selectByLifecycle; lockByLifecycle; selectPage; countPage; selectCandidates; deleteVersionedById；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 只提供明确tenant/lifecycle查询与版本删除；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-NAME-001/MC-VALID-001/MC-JSON-001/MC-SCOPE-001：精确接口/enum/约束角色；无普通POJO注解误加，Common/Jakarta允许依赖。
- Literal rule enforcement: Rule 1, Rule 2, Rule 6, Rule 11：约束/enum/接口不是可变BO；按本file合法值/组注解；枚举@JsonValue/@EnumValue仅实际持久边界；不给接口生成构造器。
- Implementation pseudocode:

```java
@Mapper @Validated interface AdDAO extends EgonColaMapper<AdPO>;
query inputs @Valid @NotNull AdDomainQuery; tenant Long @Positive/@NotNull;
List<AdPO> selectPage(query,tenant,limit,offset); long countPage(query,tenant);
AdPO selectByLifecycle(id,tenant); AdPO lockByLifecycle(id,tenant);
List<AdPO> selectCandidates(tenant,position,limit=201);
inherit int deleteVersionedById(@Param("et") @Valid AdPO entity) from EgonColaMapper; no overload/shadow with3scalarargs; validate PO at Delete DB-shape boundary;
inherited Common CRUD validation after fill, no native ActiveRecord/QueryWrapper.
```

- Verification contribution: Step 3的 `AdManageTest,AdPersistenceTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: selectByLifecycle; lockByLifecycle; selectPage; countPage; selectCandidates; deleteVersionedById的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 37 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/resources/mybatis/mapper/ad/AdDAO.xml`

- Purpose: 约束查询和CAS删除SQL
- Symbols: same namespace=AdDAO; explicit16-column resultMap
- Repository evidence: enjoyshop-service/enjoyshop-service-goods/src/main/resources/mybatis/mapper/goods/SpuDAO.xml及Step2 generatedXML
- Dependencies and consumers: 本Step伪代码列明的原组件/生成输入和具名Bean，§5对应后继owner消费；commit scope仅本file和明确依赖。
- Why now: 本位置承接Step3前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: same namespace=AdDAO; explicit16-column resultMap；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 约束查询和CAS删除SQL；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-SCOPE-001/MC-TEST-001；MC-CONFIG-001/MC-TIME-001：配置键/预算沿明确模板；不含secret/无自动运行。
- Literal rule enforcement: Rule 11：保留Light/本任务scope；Rule 7：此配置与同profile文件成套；Rule 10：预算/记录UTC单位明确。
- Implementation pseudocode:

```xml
selectByLifecycle WHERE id=:id AND tenant_id=:tenant AND deleted_at IS NULL;
lockByLifecycle same FOR UPDATE; page/count active/currenttenant optional position/status; ORDER id DESC;
selectCandidates active enabled same tenant/position ORDER sort_order,id LIMIT201;
deleteVersionedById UPDATE ad SET deleted_at=CURRENT_TIMESTAMP AT TIME ZONE 'UTC',update_user_id=#{et.updateUserId},update_time=#{et.updateTime},version=version+1
WHERE id=#{et.id} AND tenant_id=#{et.tenantId} AND deleted_at IS NULL AND version=#{MP_OPTLOCK_VERSION_ORIGINAL};
retain MP audit/tenant/version interceptor compatibility and named lifecycle policies;
no raw dynamic SQL/table substitutions, no widening deleted rows public.
```

- Verification contribution: Step 3的 `AdManageTest,AdPersistenceTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: same namespace=AdDAO; explicit16-column resultMap的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 38 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/ad/repo/AdRepository.java`

- Purpose: Repository拥有所有DAO与写入结果边界
- Symbols: selectByLifecycle; lockByLifecycle; selectPage; countPage; deleteVersionedById; selectCandidates
- Repository evidence: enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/repo/SpuRepository.java具名baseMapper/getProperties模式
- Dependencies and consumers: AdManage→AdDomainService/Impl→AdRepository→AdDAO/XML；MP-SDJ/CommonModel、namedConverters、CachePort/primaryTemplate通过明确Bean接入，Domain port不引用DAO。
- Why now: 本位置承接Step3前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: selectByLifecycle; lockByLifecycle; selectPage; countPage; deleteVersionedById; selectCandidates；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: Repository拥有所有DAO与写入结果边界；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-ARCH-001/MC-REUSE-001/MC-VALID-001/MC-LOG-001/MC-BEAN-001/MC-UTIL-001/MC-PATTERN-001/MC-SCOPE-001：保持所选层，实际注解/Bean/边界见pseudocode。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：业务@Slf4j、显式Bean名，@RequiredArgsConstructor和每final@Qualifier；JDK/现库，Jackson/Instant/Duration；selectedpattern不换反射分派。配置factory用现@Bean参数Qualifier，接口不伪构造器。
- Implementation pseudocode:

```java
@Repository("adRepository") @Validated @Slf4j @RequiredArgsConstructor;
final @Getter @Qualifier("adDAO") AdDAO baseMapper; protected properties getter from Common;
forward validated named queries with EgonColaTenantIdProvider.currentTenantId(); inputcaller cannot supplyothertenant;
Common save/updateById remain inherited; reuse final inherited removeById(locked AdPO) ->deleteVersionedById(et) with Common fill/guard and bool result; no redundant forwardingdelete method;
DAO inputs DB-shape-only groups; no overridden generic CRUD/standalone transaction manager;
candidate method initially direct and PRIMARY-owned, cache wrapped in Step5.
```

- Verification contribution: Step 3的 `AdManageTest,AdPersistenceTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: selectByLifecycle; lockByLifecycle; selectPage; countPage; deleteVersionedById; selectCandidates的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 39 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/ad/AdAuthorizationComponent.java`

- Purpose: 单独验证广告USER/RBAC上下文，不借商品permission regex
- Symbols: requirePermission(String permission)
- Repository evidence: enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/ProductAuthorizationComponent.java既有严格context/version校验
- Dependencies and consumers: AdController→AdManage/Impl→AdDomainService，MapStruct输入BO/结果投影与AdAuthorizationComponent；相关typedgroups/Common wrappers，不直接引用Repository/DAO。
- Why now: 本位置承接Step3前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: requirePermission(String permission)；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 单独验证广告USER/RBAC上下文，不借商品permission regex；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-ARCH-001/MC-REUSE-001/MC-VALID-001/MC-LOG-001/MC-BEAN-001/MC-UTIL-001/MC-PATTERN-001/MC-SCOPE-001：保持所选层，实际注解/Bean/边界见pseudocode。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：业务@Slf4j、显式Bean名，@RequiredArgsConstructor和每final@Qualifier；JDK/现库，Jackson/Instant/Duration；selectedpattern不换反射分派。配置factory用现@Bean参数Qualifier，接口不伪构造器。
- Implementation pseudocode:

```java
@Component("adAuthorizationComponent") @Validated @Slf4j @RequiredArgsConstructor;
@Qualifier("authorizationService") final AuthorizationService authorization;
requirePermission @NotBlank @Pattern(enjoyshop:ad:(read|write)):
reject anonymous401; require Rbac3ContextAuthentication/USER/nonfenced;
verify positiveLongtenant/subject/systemCode enjoyshop-goods/MDC identity/snapshot consistency;
call PermissionRequest.api(permission,"enjoyshop.ad"); INDETERMINATE/unavailable610308;
DENY orreturnedtenant/user/authVersion/policyVersion mismatch610305;
no product code change/no trustX-Tenant/no path-based bypass; safe denial logs.
```

- Verification contribution: Step 3的 `AdManageTest,AdPersistenceTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: requirePermission(String permission)的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 40 — `GENERATED enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/ad/AdDomainService.java`

- Purpose: 由catalog初始生成并实现批准管理语义
- Symbols: AdDomainService
- Repository evidence: stage2 catalog AdDomainService及当前Product Light链
- Dependencies and consumers: AdManage→AdDomainService/Impl→AdRepository→AdDAO/XML；MP-SDJ/CommonModel、namedConverters、CachePort/primaryTemplate通过明确Bean接入，Domain port不引用DAO。
- Why now: 本位置承接Step3前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: AdDomainService；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 由catalog初始生成并实现批准管理语义；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-ARCH-001/MC-REUSE-001/MC-VALID-001/MC-LOG-001/MC-BEAN-001/MC-UTIL-001/MC-PATTERN-001/MC-SCOPE-001：保持所选层，实际注解/Bean/边界见pseudocode。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：业务@Slf4j、显式Bean名，@RequiredArgsConstructor和每final@Qualifier；JDK/现库，Jackson/Instant/Duration；selectedpattern不换反射分派。配置factory用现@Bean参数Qualifier，接口不伪构造器。
- Implementation pseudocode:

```bash
初始来源只允许本Step已核对的 ad-codegen.json + E/scripts/egon-codegen.sh plan/apply；下列为命令、生成diff核对和允许业务定制目标，不允许手写初始类替代工具。
generate stage2 businessport, remove template @Validated Spring import from pureDomain port; onlyJakarta constraints remain; customize signatures AdBO create(@Valid AdBO), replace(@Valid AdBO),
AdBO detail(@Valid AdDomainQuery), PageResultRecord<AdBO> page(@Valid AdDomainQuery),boolean delete(@Valid AdBO);
List<AdBO> publishedCandidates(@Valid AdDomainQuery) for publicread contract; no MPPO/Rabbit/SpringSecuritytypes;
Jakarta constraints/groups allowed by approved correction; actualSpringvalidation in infraimpl.
```

- Verification contribution: Step 3的 `AdManageTest,AdPersistenceTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: AdDomainService的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 41 — `GENERATED enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/ad/converter/AdPersistenceConverter.java`

- Purpose: 复用生成双向映射并补明确时间/枚举/继承审计
- Symbols: AdPersistenceConverter extends BaseConverter<AdPO,AdBO>
- Repository evidence: 本轮只读 CLI plan 73e3cdf98173df5d2c525e98a67761e16b7b783a40641814d2645f05d245dd16 实际输出 infrastructure/ad/converter/
- Dependencies and consumers: AdManage→AdDomainService/Impl→AdRepository→AdDAO/XML；MP-SDJ/CommonModel、namedConverters、CachePort/primaryTemplate通过明确Bean接入，Domain port不引用DAO。
- Why now: 本位置承接Step3前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: AdPersistenceConverter extends BaseConverter<AdPO,AdBO>；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 复用生成双向映射并补明确时间/枚举/继承审计；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-CONVERT-001/MC-BEAN-001/MC-JSON-001/MC-TIME-001：MapStruct/Common基类与准确generics/mapping、实现Bean名、无反射/BeanUtils搬字段。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 10, Rule 11：保留BaseConverter/BaseForwardConverter；@Mapper spring ERROR；消费者final@Qualifier实际生成Impl名；默认转换不得吞null/unknown。
- Implementation pseudocode:

```bash
初始来源只允许本Step已核对的 ad-codegen.json + E/scripts/egon-codegen.sh plan/apply；下列为命令、生成diff核对和允许业务定制目标，不允许手写初始类替代工具。
generate via stage2 converter; follow generated BaseConverter<AdPO,AdBO> direction;
toTarget(AdPO): startTime->startAt,endTime->endAt, common identity/audit fields to BO;
toSource(AdBO): startAt->startTime,endAt->endTime, inheritedcommon copied where semantically provided,
write tenant/actor trusted context remains MP authoritative; audit fill never from HTTP;
@Mapper spring ERROR disableBuilder; enum PO stored AdStatusEnum via EnumValue;
customize only explicit @Mappings required by approved time renames; Bean adPersistenceConverterImpl.
```

- Verification contribution: Step 3的 `AdManageTest,AdPersistenceTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: AdPersistenceConverter extends BaseConverter<AdPO,AdBO>的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 42 — `GENERATED enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/ad/service/impl/AdDomainServiceImpl.java`

- Purpose: 由catalog初始生成并实现批准管理语义
- Symbols: AdDomainServiceImpl
- Repository evidence: stage2 catalog AdDomainServiceImpl及当前Product Light链
- Dependencies and consumers: AdManage→AdDomainService/Impl→AdRepository→AdDAO/XML；MP-SDJ/CommonModel、namedConverters、CachePort/primaryTemplate通过明确Bean接入，Domain port不引用DAO。
- Why now: 本位置承接Step3前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: AdDomainServiceImpl；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 由catalog初始生成并实现批准管理语义；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-ARCH-001/MC-REUSE-001/MC-VALID-001/MC-LOG-001/MC-BEAN-001/MC-UTIL-001/MC-PATTERN-001/MC-SCOPE-001：保持所选层，实际注解/Bean/边界见pseudocode。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：业务@Slf4j、显式Bean名，@RequiredArgsConstructor和每final@Qualifier；JDK/现库，Jackson/Instant/Duration；selectedpattern不换反射分派。配置factory用现@Bean参数Qualifier，接口不伪构造器。
- Implementation pseudocode:

```bash
初始来源只允许本Step已核对的 ad-codegen.json + E/scripts/egon-codegen.sh plan/apply；下列为命令、生成diff核对和允许业务定制目标，不允许手写初始类替代工具。
generate stage2 then @Service("adDomainService") @Validated @Slf4j @RequiredArgsConstructor;
qualified adRepository/adPersistenceConverterImpl/egonColaValidationUtils;
create @Transactional LOCAL: Write/Create validate ->toSource ->Repository.save, Common ID/audit fill ->toTarget;
replace @Transactional: validate Update ->lock tenant active row; missing610302; expectedversion mismatch610303;
copy approved8fields via mapper ->Common.updateById ->false610303; return currentbusinessBO;
delete @Transactional: missing/deleted/crosstenant true; activewrongversion610303; Repository.removeById(lockedAdPO) true else409;
page/detail @Transactional(readOnly=true,isolation=REPEATABLE_READ) ->PRIMARY consistent query/count;
publishedCandidates primaryRR read bounded201, disabled/deleted excluded; >200610307;
Clock eligibility/publicmapping added Step4; write invalidation added Step5 afterCommit via existing SPI only.
```

- Verification contribution: Step 3的 `AdManageTest,AdPersistenceTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: AdDomainServiceImpl的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 43 — `GENERATED enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/ad/AdManage.java`

- Purpose: 由catalog初始生成并实现批准管理语义
- Symbols: AdManage
- Repository evidence: stage2 catalog AdManage及当前Product Light链
- Dependencies and consumers: AdController→AdManage/Impl→AdDomainService，MapStruct输入BO/结果投影与AdAuthorizationComponent；相关typedgroups/Common wrappers，不直接引用Repository/DAO。
- Why now: 本位置承接Step3前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: AdManage；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 由catalog初始生成并实现批准管理语义；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-ARCH-001/MC-REUSE-001/MC-VALID-001/MC-LOG-001/MC-BEAN-001/MC-UTIL-001/MC-PATTERN-001/MC-SCOPE-001：保持所选层，实际注解/Bean/边界见pseudocode。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：业务@Slf4j、显式Bean名，@RequiredArgsConstructor和每final@Qualifier；JDK/现库，Jackson/Instant/Duration；selectedpattern不换反射分派。配置factory用现@Bean参数Qualifier，接口不伪构造器。
- Implementation pseudocode:

```bash
初始来源只允许本Step已核对的 ad-codegen.json + E/scripts/egon-codegen.sh plan/apply；下列为命令、生成diff核对和允许业务定制目标，不允许手写初始类替代工具。
generate stage2 then approved contract create(AdSaveCommand),replace(Long,AdSaveCommand),
get(Long),page(AdPageQuery),delete(AdLifecycleCommand),published(AdReadQuery);
return AdResult/PageResultRecord<AdResult>/boolean/List<AdPublicResult> respectively;
no extraall/name-filter/querypreflight publiccalls; @Valid groups at invocation signatures.
```

- Verification contribution: Step 3的 `AdManageTest,AdPersistenceTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: AdManage的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 44 — `GENERATED enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/ad/AdManageImpl.java`

- Purpose: 由catalog初始生成并实现批准管理语义
- Symbols: AdManageImpl
- Repository evidence: stage2 catalog AdManageImpl及当前Product Light链
- Dependencies and consumers: AdController→AdManage/Impl→AdDomainService，MapStruct输入BO/结果投影与AdAuthorizationComponent；相关typedgroups/Common wrappers，不直接引用Repository/DAO。
- Why now: 本位置承接Step3前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: AdManageImpl；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 由catalog初始生成并实现批准管理语义；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-ARCH-001/MC-REUSE-001/MC-VALID-001/MC-LOG-001/MC-BEAN-001/MC-UTIL-001/MC-PATTERN-001/MC-SCOPE-001：保持所选层，实际注解/Bean/边界见pseudocode。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：业务@Slf4j、显式Bean名，@RequiredArgsConstructor和每final@Qualifier；JDK/现库，Jackson/Instant/Duration；selectedpattern不换反射分派。配置factory用现@Bean参数Qualifier，接口不伪构造器。
- Implementation pseudocode:

```bash
初始来源只允许本Step已核对的 ad-codegen.json + E/scripts/egon-codegen.sh plan/apply；下列为命令、生成diff核对和允许业务定制目标，不允许手写初始类替代工具。
generate stage2 then @Service("adManage") @Validated @Slf4j @RequiredArgsConstructor;
qualified adAuthorizationComponent/adDomainService/adApplicationConverterImpl/adResultConverterImpl/egonColaValidationUtils;
admin read/write require exact permission before dataaccess; validate command/group (manualhelper only as actualproxy gap);
MapStruct toBusiness ->domain ->Result; addcontext from verified identity only; never explicit untrustedtenant/actor;
page reconstruct original PageResultRecord using mapped records +existing PageMeta fields;
published(AdReadQuery) validates PublicRead, no management RBAC shortcut;
qualified adClock/adEligibilitySpecification/adPublicResultConverterImpl;
now=clock.instant once; domain.publishedCandidates typedquery (PRIMARY RR) ->filter specification ->take limit ->MapStruct publicResult;
publictrusted tenant scope established adapter Step4; internal tests establish controlled scope;
admin operations always RBAC; no unsafeanonymousManagementshortcut;
Common save generatesID; propagate IDunavailable610309 via stable admapping, no UUIDfallback.
```

- Verification contribution: Step 3的 `AdManageTest,AdPersistenceTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: AdManageImpl的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 45 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/resources/application.yml`

- Purpose: 追加ad生命周期语句策略并保留旧条目
- Symbols: lifecycle.statement-policies AdDAO names
- Repository evidence: enjoyshop-service/enjoyshop-service-goods/src/main/resources/application.yml原Spu/Sku named政策
- Dependencies and consumers: 对应typedProperties→Commonphysicalpool/Native loader/Cachemanager及Ad/CDCconfiguration；同profile资源和NativeReadwriteContractTest消费键树；无新factory。
- Why now: 本位置承接Step3前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: lifecycle.statement-policies AdDAO names；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 追加ad生命周期语句策略并保留旧条目；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-SCOPE-001/MC-TEST-001；MC-CONFIG-001/MC-TIME-001：配置键/预算沿明确模板；不含secret/无自动运行。
- Literal rule enforcement: Rule 11：保留Light/本任务scope；Rule 7：此配置与同profile文件成套；Rule 10：预算/记录UTC单位明确。
- Implementation pseudocode:

```yaml
append selectByLifecycle/lockByLifecycle scopeID, selectPage PAGE,countPage COUNT,
deleteVersionedById MONOTONE_DELETE tablead;
selectCandidates normal tenant/active enabled path, no custom exemption;
retain all Spu/Sku restore/purge/local-write-guard keys and no generic ignoredTables;
config parity compare effective layers; this Step does not addpublicenabled or MQflags.
```

- Verification contribution: Step 3的 `AdManageTest,AdPersistenceTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: lifecycle.statement-policies AdDAO names的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 46 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-dev.yml`

- Purpose: 追加ad生命周期语句策略并保留旧条目
- Symbols: lifecycle.statement-policies AdDAO names
- Repository evidence: enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-dev.yml原Spu/Sku named政策
- Dependencies and consumers: 对应typedProperties→Commonphysicalpool/Native loader/Cachemanager及Ad/CDCconfiguration；同profile资源和NativeReadwriteContractTest消费键树；无新factory。
- Why now: 本位置承接Step3前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: lifecycle.statement-policies AdDAO names；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 追加ad生命周期语句策略并保留旧条目；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-SCOPE-001/MC-TEST-001；MC-CONFIG-001/MC-TIME-001：配置键/预算沿明确模板；不含secret/无自动运行。
- Literal rule enforcement: Rule 11：保留Light/本任务scope；Rule 7：此配置与同profile文件成套；Rule 10：预算/记录UTC单位明确。
- Implementation pseudocode:

```yaml
append selectByLifecycle/lockByLifecycle scopeID, selectPage PAGE,countPage COUNT,
deleteVersionedById MONOTONE_DELETE tablead;
selectCandidates normal tenant/active enabled path, no custom exemption;
retain all Spu/Sku restore/purge/local-write-guard keys and no generic ignoredTables;
config parity compare effective layers; this Step does not addpublicenabled or MQflags.
```

- Verification contribution: Step 3的 `AdManageTest,AdPersistenceTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: lifecycle.statement-policies AdDAO names的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 47 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-test.yml`

- Purpose: 追加ad生命周期语句策略并保留旧条目
- Symbols: lifecycle.statement-policies AdDAO names
- Repository evidence: enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-test.yml原Spu/Sku named政策
- Dependencies and consumers: 对应typedProperties→Commonphysicalpool/Native loader/Cachemanager及Ad/CDCconfiguration；同profile资源和NativeReadwriteContractTest消费键树；无新factory。
- Why now: 本位置承接Step3前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: lifecycle.statement-policies AdDAO names；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 追加ad生命周期语句策略并保留旧条目；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-SCOPE-001/MC-TEST-001；MC-CONFIG-001/MC-TIME-001：配置键/预算沿明确模板；不含secret/无自动运行。
- Literal rule enforcement: Rule 11：保留Light/本任务scope；Rule 7：此配置与同profile文件成套；Rule 10：预算/记录UTC单位明确。
- Implementation pseudocode:

```yaml
append selectByLifecycle/lockByLifecycle scopeID, selectPage PAGE,countPage COUNT,
deleteVersionedById MONOTONE_DELETE tablead;
selectCandidates normal tenant/active enabled path, no custom exemption;
retain all Spu/Sku restore/purge/local-write-guard keys and no generic ignoredTables;
config parity compare effective layers; this Step does not addpublicenabled or MQflags.
```

- Verification contribution: Step 3的 `AdManageTest,AdPersistenceTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: lifecycle.statement-policies AdDAO names的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 48 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-prod.yml`

- Purpose: 追加ad生命周期语句策略并保留旧条目
- Symbols: lifecycle.statement-policies AdDAO names
- Repository evidence: enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-prod.yml原Spu/Sku named政策
- Dependencies and consumers: 对应typedProperties→Commonphysicalpool/Native loader/Cachemanager及Ad/CDCconfiguration；同profile资源和NativeReadwriteContractTest消费键树；无新factory。
- Why now: 本位置承接Step3前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: lifecycle.statement-policies AdDAO names；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 追加ad生命周期语句策略并保留旧条目；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-SCOPE-001/MC-TEST-001；MC-CONFIG-001/MC-TIME-001：配置键/预算沿明确模板；不含secret/无自动运行。
- Literal rule enforcement: Rule 11：保留Light/本任务scope；Rule 7：此配置与同profile文件成套；Rule 10：预算/记录UTC单位明确。
- Implementation pseudocode:

```yaml
append selectByLifecycle/lockByLifecycle scopeID, selectPage PAGE,countPage COUNT,
deleteVersionedById MONOTONE_DELETE tablead;
selectCandidates normal tenant/active enabled path, no custom exemption;
retain all Spu/Sku restore/purge/local-write-guard keys and no generic ignoredTables;
config parity compare effective layers; this Step does not addpublicenabled or MQflags.
```

- Verification contribution: Step 3的 `AdManageTest,AdPersistenceTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: lifecycle.statement-policies AdDAO names的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 49 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/resources/egon-mybatis-plus-sharding.yml`

- Purpose: 追加ad生命周期语句策略并保留旧条目
- Symbols: lifecycle.statement-policies AdDAO names
- Repository evidence: enjoyshop-service/enjoyshop-service-goods/src/main/resources/egon-mybatis-plus-sharding.yml原Spu/Sku named政策
- Dependencies and consumers: 对应typedProperties→Commonphysicalpool/Native loader/Cachemanager及Ad/CDCconfiguration；同profile资源和NativeReadwriteContractTest消费键树；无新factory。
- Why now: 本位置承接Step3前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: lifecycle.statement-policies AdDAO names；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 追加ad生命周期语句策略并保留旧条目；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-SCOPE-001/MC-TEST-001；MC-CONFIG-001/MC-TIME-001：配置键/预算沿明确模板；不含secret/无自动运行。
- Literal rule enforcement: Rule 11：保留Light/本任务scope；Rule 7：此配置与同profile文件成套；Rule 10：预算/记录UTC单位明确。
- Implementation pseudocode:

```yaml
append selectByLifecycle/lockByLifecycle scopeID, selectPage PAGE,countPage COUNT,
deleteVersionedById MONOTONE_DELETE tablead;
selectCandidates normal tenant/active enabled path, no custom exemption;
retain all Spu/Sku restore/purge/local-write-guard keys and no generic ignoredTables;
config parity compare effective layers; this Step does not addpublicenabled or MQflags.
```

- Verification contribution: Step 3的 `AdManageTest,AdPersistenceTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: lifecycle.statement-policies AdDAO names的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 50 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/architecture/LightPersistenceArchitectureTest.java`

- Purpose: 确认新增Service实现仅组合Repository
- Symbols: DomainServiceImpl count9->10
- Repository evidence: enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/architecture/LightPersistenceArchitectureTest.java Step2
- Dependencies and consumers: 本file调用/观察Step 3列明的实际subject（AdManageTest,AdPersistenceTest,LightPersistenceArchitectureTest）；复用JUnit/MockMvc/原testsupport，负面fixture不依赖外部服务。
- Why now: 本位置承接Step3前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: DomainServiceImpl count9->10；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 确认新增Service实现仅组合Repository；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-VALID-001/MC-JSON-001/MC-BEAN-001/MC-CONFIG-001/MC-SCOPE-001/MC-TEST-001：测试pseudocode所列正负fixture/模型与wiring，不启外部服务。
- Literal rule enforcement: Rule 1, Rule 2, Rule 6, Rule 10, Rule 11：同模块testrole，逐约束/组/JSON/时序断言；测试类不是业务Bean，不伪加POJO构造注解。
- Implementation pseudocode:

```java
assert count DomainServiceImpl=10; PO/DAO/repo stay14;
assert AdDomainServiceImpl no directDAO/getBaseMapper access;
assert DomainBO mayJakarta but noMP/ORM/SpringSecurity/Rabbit;
assert no temporary generated Create/Update/DeleteAdCommand/AdDetailQuery leftovers.
```

- Verification contribution: Step 3的 `AdManageTest,AdPersistenceTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: DomainServiceImpl count9->10的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 51 — `MODIFY docs/egon/codegen/ddl-consumption-log.md`

- Purpose: 记录第二次catalog apply及手工定制范围
- Symbols: append stage2 generation journal
- Repository evidence: Step2 append-only SQL journal
- Dependencies and consumers: 本Step伪代码列明的原组件/生成输入和具名Bean，§5对应后继owner消费；commit scope仅本file和明确依赖。
- Why now: 本位置承接Step3前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: append stage2 generation journal；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 记录第二次catalog apply及手工定制范围；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-SCOPE-001/MC-TEST-001；MC-SCOPE-001：仅追加/精确文档归属，标运行未验证。
- Literal rule enforcement: Rule 11：保留Light/本任务scope；规则中Java注解不适用此非Java文件；不引入运行/Schema新语义。
- Implementation pseudocode:

```markdown
append actual stage2 plan/apply id/config/CREATEview bytesSHA;
throughVersion:none and original20261010_001 provenance/fulltext recorded truthfully;
list transient command/converter renames/removals and sameSpec contract;
state no managedDDL executed/no replaced old sources; no rewriting prior entry.
```

- Verification contribution: Step 3的 `AdManageTest,AdPersistenceTest,LightPersistenceArchitectureTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: append stage2 generation journal的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

- Validation working directory: `/Users/mario/SelfProject/enjoyshop`
- Verification command: `mvn -pl enjoyshop-service/enjoyshop-service-goods -am -Dtest=AdManageTest,AdPersistenceTest,LightPersistenceArchitectureTest -Dsurefire.failIfNoSpecifiedTests=false test`（先RED记录原因，最低实现后同命令GREEN）；
- Expected result: RED仅因上述缺失行为；GREEN exit0，named tests全部执行且无误跳过；离线检查对应断言通过。TEST019–022运行项仍明确Not run。
- Failure returns to: 本Step声明的测试/最低production/wiring file；若历史SQL/架构/工具范围冲突则回相应前置Step及§11，不绕过修复。
- Completion criteria: 本Step所有file/规则/MC证据闭合，测试报告匹配selectors、diff只本scope；不包含临时无用catalog类、秘密或组件源码。
- Rollback: 未运行SQL时仅撤销本Step自己写的path改动，保留他人内容；已部署SQL按§9追加forward-fix，不改旧脚本/history；下游持久消息/offset不丢。
- Commit paths: `enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/application/ad/AdManageTest.java`；`enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/ad/AdPersistenceTest.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/enums/GoodsErrorStatus.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/validation/AdValidationGroup.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/ad/AdStatusEnum.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/validation/ValidAdWindow.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/validation/AdWindowValidator.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/validation/ValidAdPosition.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/validation/AdPositionValidator.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/validation/ValidAdUrl.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/validation/AdUrlValidator.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/config/AdProperties.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/config/AdConfiguration.java`；`enjoyshop-service/enjoyshop-service-goods/codegen/ad-codegen.json`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/ad/CreateAdCommand.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/ad/UpdateAdCommand.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/ad/DeleteAdCommand.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/ad/AdDetailQuery.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/ad/converter/CreateAdCommandConverter.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/ad/converter/UpdateAdCommandConverter.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/ad/converter/DeleteAdCommandConverter.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/ad/converter/AdResultConverter.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/ad/AdSaveCommand.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/ad/AdLifecycleCommand.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/ad/AdReadQuery.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/ad/AdPageQuery.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/ad/AdResult.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/ad/AdApplicationConverter.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/ad/AdResultConverter.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/ad/AdBO.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/ad/AdDomainQuery.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/ad/po/AdPO.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/ad/AdPublicResult.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/ad/AdPublicResultConverter.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/ad/AdEligibilitySpecification.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/ad/dao/AdDAO.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/resources/mybatis/mapper/ad/AdDAO.xml`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/ad/repo/AdRepository.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/ad/AdAuthorizationComponent.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/ad/AdDomainService.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/ad/converter/AdPersistenceConverter.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/ad/service/impl/AdDomainServiceImpl.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/ad/AdManage.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/ad/AdManageImpl.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/resources/application.yml`；`enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-dev.yml`；`enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-test.yml`；`enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-prod.yml`；`enjoyshop-service/enjoyshop-service-goods/src/main/resources/egon-mybatis-plus-sharding.yml`；`enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/architecture/LightPersistenceArchitectureTest.java`；`docs/egon/codegen/ddl-consumption-log.md`

- Commit: `feat(goods): implement guarded advertisement management`；HEAD `fb296e0480c3712d05a2ec25e40772dedcbdfcd0`，43个精确Step 3 paths。26项定向管理/持久化/架构/NATIVE/profile测试通过，Plan strict及diff check通过；暂态净删除file不生成额外commit。

### Step 4 — 发布6个广告API并闭合匿名站点和管理权限边界

- Requirements: REQ-004, REQ-005, REQ-006, REQ-011
- Dependencies: Step 3 committed
- Baseline state: Step 3已提交HEAD `fb296e0480c3712d05a2ec25e40772dedcbdfcd0`；`feat(goods): implement guarded advertisement management` 的43个paths及Step 3 RED/GREEN见提交与`AdManageTest,AdPersistenceTest,LightPersistenceArchitectureTest,NativeReadwriteContractTest,ProfileKeyParityTest`报告。
- Observable outcome: 全部6HTTP操作/OAS与原信封一致；匿名tenant只由登记Host，管理verified USER/RBAC；原API不漂移。
- End state: 全部6HTTP操作/OAS与原信封一致；匿名tenant只由登记Host，管理verified USER/RBAC；原API不漂移。 后继内容只按后继Step发生；未部署本Step。
- Test-first gate: Required — 无ads/ad-slots mapping、旧filter信任header和默认tenant1，OAS操作缺失。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-JSON-001, MC-TIME-001, MC-CONFIG-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001；本Step文件角色对不适用注解写明理由，scope/test永远适用。
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；按每file角色落实，不让非Java运维文本伪承担Java类型规则。
- Ordered files:

#### File 1 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/goods/AdControllerTest.java`

- Purpose: 测试先锁HTTP/安全/文档行为
- Symbols: AdControllerTest
- Repository evidence: enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/goods/ProductControllerTest.java、ProductOpenApiContractTest及ProductTestSecurityConfiguration
- Dependencies and consumers: 本file调用/观察Step 4列明的实际subject（AdControllerTest,AdOpenApiContractTest,AdSecurityContractTest,AdManageTest）；复用JUnit/MockMvc/原testsupport，负面fixture不依赖外部服务。
- Why now: 先确立缺失行为的RED，后续最小实现须满足它。
- Contract/signature changes: AdControllerTest；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 测试先锁HTTP/安全/文档行为；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-VALID-001/MC-JSON-001/MC-BEAN-001/MC-CONFIG-001/MC-SCOPE-001/MC-TEST-001：测试pseudocode所列正负fixture/模型与wiring，不启外部服务。
- Literal rule enforcement: Rule 1, Rule 2, Rule 6, Rule 10, Rule 11：同模块testrole，逐约束/组/JSON/时序断言；测试类不是业务Bean，不伪加POJO构造注解。
- Implementation pseudocode:

```java
isolated MockMvc with real Jackson/Validator/handler and fakeManage; noexternalServices;
assert GET /api/v1/ads page.records/page.pages; GET id typedstring; POST201 Location; PUT200fullreplace; DELETE?version200true;
public GET /api/v1/ad-slots/{position}/ads returns4fieldarray/no-store;
reject unknown/duplicatequery, overflowid/version, versionfloat/string, blank/defaultmisuse, nonJSON415;
assert integeradcode6103xx/fieldErrors field,constraint,message; originalwrapper7 orpage8keys alwayspresent;
public empty[] vs503dependency distinct, never returninternalfields.
```

- Verification contribution: Step 4的 `AdControllerTest,AdOpenApiContractTest,AdSecurityContractTest,AdManageTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: AdControllerTest的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 2 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/start/config/AdSecurityContractTest.java`

- Purpose: 测试先锁HTTP/安全/文档行为
- Symbols: AdSecurityContractTest
- Repository evidence: enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/goods/ProductControllerTest.java、ProductOpenApiContractTest及ProductTestSecurityConfiguration
- Dependencies and consumers: 本file调用/观察Step 4列明的实际subject（AdControllerTest,AdOpenApiContractTest,AdSecurityContractTest,AdManageTest）；复用JUnit/MockMvc/原testsupport，负面fixture不依赖外部服务。
- Why now: 先确立缺失行为的RED，后续最小实现须满足它。
- Contract/signature changes: AdSecurityContractTest；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 测试先锁HTTP/安全/文档行为；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-VALID-001/MC-JSON-001/MC-BEAN-001/MC-CONFIG-001/MC-SCOPE-001/MC-TEST-001：测试pseudocode所列正负fixture/模型与wiring，不启外部服务。
- Literal rule enforcement: Rule 1, Rule 2, Rule 6, Rule 10, Rule 11：同模块testrole，逐约束/组/JSON/时序断言；测试类不是业务Bean，不伪加POJO构造注解。
- Implementation pseudocode:

```java
context test filters with verified USER/tenant41/MDC and fakeRBAC only;
admin anonymous401/SERVICE403/DENY403/INDETERMINATE503; tenantHeader cannot alter verifiedscope;
public knownHostmaps41, unknownHost404, forgedXForwardedHost/X-Tenant ignored;
nonGET publicroute rejected; finally restores/clears tenant/user scope;
product/file chains same precedence/identity behavior; baselegacy samples unchanged.
```

- Verification contribution: Step 4的 `AdControllerTest,AdOpenApiContractTest,AdSecurityContractTest,AdManageTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: AdSecurityContractTest的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 3 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/goods/AdOpenApiContractTest.java`

- Purpose: 测试先锁HTTP/安全/文档行为
- Symbols: AdOpenApiContractTest
- Repository evidence: enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/goods/ProductControllerTest.java、ProductOpenApiContractTest及ProductTestSecurityConfiguration
- Dependencies and consumers: 本file调用/观察Step 4列明的实际subject（AdControllerTest,AdOpenApiContractTest,AdSecurityContractTest,AdManageTest）；复用JUnit/MockMvc/原testsupport，负面fixture不依赖外部服务。
- Why now: 本位置承接Step4前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: AdOpenApiContractTest；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 测试先锁HTTP/安全/文档行为；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-VALID-001/MC-JSON-001/MC-BEAN-001/MC-CONFIG-001/MC-SCOPE-001/MC-TEST-001：测试pseudocode所列正负fixture/模型与wiring，不启外部服务。
- Literal rule enforcement: Rule 1, Rule 2, Rule 6, Rule 10, Rule 11：同模块testrole，逐约束/组/JSON/时序断言；测试类不是业务Bean，不伪加POJO构造注解。
- Implementation pseudocode:

```java
isolated springdoc MockMvc /v3/api-docs and ads group;
assert exactly6paths+methods+operationId names §9, management bearerAuth and publicsecurity=[];
Createversionnotwritable vsUpdate required; ISOInstant and decimalstringID; response201Location/200array/boolean/page.pages;
assert publicschema lacks tenant/version/window and genericwrapperactualtypes correct;
assert originalBrand/Product/File schemas/routes unchanged; noSpringfox/newwrapper/noexternal runtime.
```

- Verification contribution: Step 4的 `AdControllerTest,AdOpenApiContractTest,AdSecurityContractTest,AdManageTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: AdOpenApiContractTest的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 4 — `MODIFY enjoyshop-service/enjoyshop-service-goods/codegen/ad-codegen.json`

- Purpose: 只生成尚不存在的Controller，不重放application artifact
- Symbols: artifacts:[controller]
- Repository evidence: enjoyshop-service/enjoyshop-service-goods/codegen/ad-codegen.json Step3与LightLayout adapter.domain默认包
- Dependencies and consumers: 本Step伪代码列明的原组件/生成输入和具名Bean，§5对应后继owner消费；commit scope仅本file和明确依赖。
- Why now: 本位置承接Step4前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: artifacts:[controller]；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 只生成尚不存在的Controller，不重放application artifact；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-SCOPE-001/MC-TEST-001；MC-CONFIG-001/MC-TIME-001：配置键/预算沿明确模板；不含secret/无自动运行。
- Literal rule enforcement: Rule 11：保留Light/本任务scope；Rule 7：此配置与同profile文件成套；Rule 10：预算/记录UTC单位明确。
- Implementation pseudocode:

```json
artifacts=[controller]; keep po/dao existing mappings and sameCREATEview;
run approvedplan/apply expect single adapter/ad/AdController.java ADD only;
no existing customizedfile inactions; unknownscopeconflict stops beforeapply; append journal.
```

- Verification contribution: Step 4的 `AdControllerTest,AdOpenApiContractTest,AdSecurityContractTest,AdManageTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: artifacts:[controller]的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 5 — `GENERATED enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/ad/AdController.java`

- Purpose: Controller初始模板来源，不留下未受ad advice覆盖的endpoint
- Symbols: AdController generation and semantic-preserving relocation
- Repository evidence: LightLayout.packageName controller=base.adapter.ad，而BaseExceptionHandler仅adapter.goods
- Dependencies and consumers: 本Step伪代码列明的原组件/生成输入和具名Bean，§5对应后继owner消费；commit scope仅本file和明确依赖。
- Why now: 本位置承接Step4前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: AdController generation and semantic-preserving relocation；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: Controller初始模板来源，不留下未受ad advice覆盖的endpoint；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-ARCH-001/MC-REUSE-001/MC-VALID-001/MC-LOG-001/MC-BEAN-001/MC-UTIL-001/MC-PATTERN-001/MC-SCOPE-001：保持所选层，实际注解/Bean/边界见pseudocode。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：业务@Slf4j、显式Bean名，@RequiredArgsConstructor和每final@Qualifier；JDK/现库，Jackson/Instant/Duration；selectedpattern不换反射分派。配置factory用现@Bean参数Qualifier，接口不伪构造器。
- Implementation pseudocode:

```bash
初始来源只允许本Step已核对的 ad-codegen.json + E/scripts/egon-codegen.sh plan/apply；下列为命令、生成diff核对和允许业务定制目标，不允许手写初始类替代工具。
verified stage3 plan/apply creates this exact controller once;
move immediately to approved adapter/goods/AdController.java; package/importreferences update;
replace only newcontroller mappings per nextfile; do not commit default template exposed endpoints;
no custom generator/templatecomponent edits; preserve generatorownership journal, no later overwrites.
```

- Verification contribution: Step 4的 `AdControllerTest,AdOpenApiContractTest,AdSecurityContractTest,AdManageTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: AdController generation and semantic-preserving relocation的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。暂态源最终不保留，原消费者已由该Step替换。

#### File 6 — `RENAME enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/goods/AdController.java`

- Purpose: 精确6HTTP映射和typedgroups/结果
- Symbols: listAdvertisements,getAdvertisement,createAdvertisement,replaceAdvertisement,deleteAdvertisement,listPublishedAdvertisements
- Repository evidence: Spec9固定ID；原ProductController/BrandController与范围advice
- Dependencies and consumers: 本Step伪代码列明的原组件/生成输入和具名Bean，§5对应后继owner消费；commit scope仅本file和明确依赖。
- Why now: 本位置承接Step4前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: listAdvertisements,getAdvertisement,createAdvertisement,replaceAdvertisement,deleteAdvertisement,listPublishedAdvertisements；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 精确6HTTP映射和typedgroups/结果；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-ARCH-001/MC-REUSE-001/MC-VALID-001/MC-LOG-001/MC-BEAN-001/MC-UTIL-001/MC-PATTERN-001/MC-SCOPE-001：保持所选层，实际注解/Bean/边界见pseudocode。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：业务@Slf4j、显式Bean名，@RequiredArgsConstructor和每final@Qualifier；JDK/现库，Jackson/Instant/Duration；selectedpattern不换反射分派。配置factory用现@Bean参数Qualifier，接口不伪构造器。
- Implementation pseudocode:

```java
move generated source from adapter/ad, then @RestController("adController") @Validated @Slf4j @RequiredArgsConstructor;
normalize name at Jackson @JsonSetter boundary (trim before @Size/Write validation), do not trim positions or opaqueURL silently; nonHTTP callers use the same declared normalized input contract;
qualified adManage; replace generated classbase with @RequestMapping("/api/v1"), methodsrelative /ads and /ad-slots so no duplicateprefix; mappings exactly5 /api/v1/ads +1 GET /api/v1/ad-slots/{position}/ads;
POST @Validated(Create)/@Valid AdSaveCommand ->201 ResultRecord<AdResult>,Location newID;
PUT pathID + @Validated(Update)fullbody ->200; DELETE versionquery ->AdLifecycleCommand;
GETlist optionalposition/status,page1/20 ->existingPageResultRecord; detail positiveLongID;
public @Validated(PublicRead) Query ->ResultRecord<List<AdPublicResult>>,no-store;
validate rawparameter names/multiplicity once before typedbinding; strictdecimal lexicalquery/path, no arbitrarytenant;
@Operation fixed IDs/@Tag(ad)/@ApiResponses/@Schema; publicsecurity=[]; no ad_update orotherroute.
```

- Verification contribution: Step 4的 `AdControllerTest,AdOpenApiContractTest,AdSecurityContractTest,AdManageTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: listAdvertisements,getAdvertisement,createAdvertisement,replaceAdvertisement,deleteAdvertisement,listPublishedAdvertisements的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 7 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/ad/AdSaveCommand.java`

- Purpose: trim the advertisement name during JSON binding before Write-group validation
- Symbols: `@JsonSetter("name")`; preserve null and trim only name
- Repository evidence: Step 3 AdSaveCommand currently receives Bean Validation before AdManageImpl normalization; Spec §10.3 requires trim before enforcing the 120-character bound
- Dependencies and consumers: AdController request-body Create/Update validation; AdManage retains the same normalization for non-HTTP callers
- Why now: MVC validates `@RequestBody` before entering Manage, so normalization must happen at the binder boundary for the HTTP contract
- Contract/signature changes: the existing fluent `setName(String)` becomes the Jackson setter and returns the normalized value; all other fields and unknown-field rejection stay unchanged
- Input/output and state mapping: null remains null; non-null name is trimmed once before `@NotBlank`/`@Size`; position and URLs are never silently rewritten
- Error and edge behavior: whitespace-only becomes empty and receives the existing field validation error; no tenant, identity, or business values are added
- Standards impact: MC-VALID-001/MC-JSON-001/MC-SCOPE-001; reuse the existing command type and Jackson boundary
- Literal rule enforcement: Rule 3, Rule 6, Rule 11; preserve Lombok fluent setter semantics and reject unknown body fields
- Implementation pseudocode:

```java
@JsonSetter("name")
public AdSaveCommand setName(String value) {
    this.name = value == null ? null : value.trim();
    return this;
}
// Do not normalize position, imageUrl, targetUrl, status, or timestamps.
```

- Verification contribution: `AdControllerTest` proves HTTP name normalization precedes field validation; `AdManageTest` retains the same validation contract for direct calls
- After this file: AdSaveCommand JSON name binding is canonical before the next MVC validation step

#### File 8 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/filter/AdPublicTenantFilter.java`

- Purpose: 只公开GET建立受控站点租户scope
- Symbols: shouldNotFilter; doFilterInternal
- Repository evidence: TenantContextFilter范围过滤+原MDC/MP tenant provider
- Dependencies and consumers: 本Step伪代码列明的原组件/生成输入和具名Bean，§5对应后继owner消费；commit scope仅本file和明确依赖。
- Why now: 本位置承接Step4前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: shouldNotFilter; doFilterInternal；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 只公开GET建立受控站点租户scope；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-ARCH-001/MC-REUSE-001/MC-VALID-001/MC-LOG-001/MC-BEAN-001/MC-UTIL-001/MC-PATTERN-001/MC-SCOPE-001：保持所选层，实际注解/Bean/边界见pseudocode。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：业务@Slf4j、显式Bean名，@RequiredArgsConstructor和每final@Qualifier；JDK/现库，Jackson/Instant/Duration；selectedpattern不换反射分派。配置factory用现@Bean参数Qualifier，接口不伪构造器。
- Implementation pseudocode:

```java
@Component("adPublicTenantFilter") @Slf4j @RequiredArgsConstructor;
qualified AdProperties/ObjectMapper; filter only exact GET ad-slots/{position}/ads, no body tenant;
canonical request.serverName lowerASCII; rejectunknown/invalid404610306; no default1;
resolve positiveLongtenant from startupvalidated map; bindMDC tenantId only, no calleractor identity;
wrap chain in finallyrestore/clear originalscope; X-Tenant/X-Actor/XForwarded ignored unlessapprovedtrustedserverconfig;
JSONfiltererrors use originalResultRecord; route publicEnabled=false rejects404 without DBcall.
```

- Verification contribution: Step 4的 `AdControllerTest,AdOpenApiContractTest,AdSecurityContractTest,AdManageTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: shouldNotFilter; doFilterInternal的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 9 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/filter/TenantContextFilter.java`

- Purpose: 新ad路径完全跳过旧Header默认tenant
- Symbols: shouldNotFilter(HttpServletRequest)
- Repository evidence: enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/filter/TenantContextFilter.java现products skip
- Dependencies and consumers: 本Step伪代码列明的原组件/生成输入和具名Bean，§5对应后继owner消费；commit scope仅本file和明确依赖。
- Why now: 本位置承接Step4前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: shouldNotFilter(HttpServletRequest)；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 新ad路径完全跳过旧Header默认tenant；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-ARCH-001/MC-REUSE-001/MC-VALID-001/MC-LOG-001/MC-BEAN-001/MC-UTIL-001/MC-PATTERN-001/MC-SCOPE-001：保持所选层，实际注解/Bean/边界见pseudocode。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：业务@Slf4j、显式Bean名，@RequiredArgsConstructor和每final@Qualifier；JDK/现库，Jackson/Instant/Duration；selectedpattern不换反射分派。配置factory用现@Bean参数Qualifier，接口不伪构造器。
- Implementation pseudocode:

```java
keep originalproductsskip; add exact ads +ads/** and ad-slots/** skips;
legacy otherpaths unchanged; no broad /api bypass/no defaulttenant fornewpaths;
security/admin scopes owned by verifiedShoubing; publicscope owned newfilter.
```

- Verification contribution: Step 4的 `AdControllerTest,AdOpenApiContractTest,AdSecurityContractTest,AdManageTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: shouldNotFilter(HttpServletRequest)的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 10 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/start/StudentManagementApplication.java`

- Purpose: register the new ad DAO package in the existing MyBatis mapper scan
- Symbols: `@MapperScan(basePackages=...)`
- Repository evidence: the current application lists only user, teaching and goods DAO packages; `AdDAO` is in `infrastructure.ad.dao`
- Dependencies and consumers: generated `AdDAO` and `AdRepository` bean wiring; no new scanner or configuration layer
- Why now: the full application context showed that the new mapper package is outside the explicit scan, so the repository cannot be constructed until this existing list includes it
- Contract/signature changes: append `top.egon.enjoyshop.goods.infrastructure.ad.dao` to the existing mapper package list; retain every prior package
- Input/output and state mapping: MyBatis discovers `AdDAO` under its existing `@MapperScan`; runtime routes remain configured through the existing NATIVE and statement-policy keys
- Error and edge behavior: no datasource startup, SQL execution, or broad package-scan change is introduced
- Standards impact: MC-ARCH-001/MC-SCOPE-001/MC-TEST-001; extend the established package list only
- Literal rule enforcement: Rule 11; reuse the existing Light startup and mapper scanning contract
- Implementation pseudocode:

```java
@MapperScan(basePackages = {
    existing user.dao, teaching.dao, goods.dao,
    "top.egon.enjoyshop.goods.infrastructure.ad.dao"
})
```

- Verification contribution: `AdOpenApiContractTest` loads the isolated application context and proves adRepository's DAO dependency can be constructed without a real database
- After this file: the already implemented AdDAO is registered by the owning application bootstrap

#### File 11 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/resources/mybatis/mapper/ad/AdDAO.xml`

- Purpose: preserve the Common mapper lifecycle statements required for each EgonColaMapper
- Symbols: `selectActiveById`; `selectActiveByIds`; retain advertisement lifecycle/page/candidate/CAS statements
- Repository evidence: EgonColaMybatisPlusContractValidator requires XML-backed `selectActiveById`, `selectActiveByIds` and `deleteVersionedById` for every scanned EgonColaMapper; the Step 3 ad XML currently lacks the two standard active reads
- Dependencies and consumers: StudentManagementApplication scans `infrastructure.ad.dao`; EgonColaMapper and the existing tenant interceptor consume these statement names
- Why now: the isolated full application context exposes `MAPPER_XML_CONTRACT_MISSING` until the standard Common statements are restored
- Contract/signature changes: add both active selects with the complete resultMap/columns; the IN query returns no rows for null/empty IDs; keep explicit tenant/lifecycle predicates on custom ad reads and keep the monotone versioned delete
- Input/output and state mapping: active reads return AdPO only; tenant scope remains owned by Common tenant interception and existing named policies
- Error and edge behavior: no hard delete, cross-tenant read, temporary table or database execution
- Standards impact: MC-REUSE-001/MC-ARCH-001/MC-TEST-001; reuse EgonColaMapper's established XML contract
- Literal rule enforcement: Rule 11; maintain the existing Light persistence owner and standard mapper shape
- Implementation pseudocode:

```xml
<select id="selectActiveById" resultMap="AdPOResultMap"> ... WHERE id=#{id} AND deleted_at IS NULL ... </select>
<select id="selectActiveByIds" resultMap="AdPOResultMap"> ... AND deleted_at IS NULL; empty/null ids => AND 1=0 ... </select>
<!-- retain selectByLifecycle/lockByLifecycle/selectPage/countPage/selectCandidates/deleteVersionedById -->
```

- Verification contribution: `AdOpenApiContractTest` boots the isolated application context and the Common mapper contract validator; focused persistence assertions continue to guard tenant/CAS SQL
- After this file: AdDAO satisfies the same Common XML lifecycle contract as existing goods mappers

#### File 12 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/start/config/NativeHttpSecurityConfiguration.java`

- Purpose: 独立管理链和精确公开GET拒绝其他method
- Symbols: adSecurityFilterChain; publicAdSecurityFilterChain
- Repository evidence: existing product/file/application chains以及IDP filter顺序
- Dependencies and consumers: 本Step伪代码列明的原组件/生成输入和具名Bean，§5对应后继owner消费；commit scope仅本file和明确依赖。
- Why now: 本位置承接Step4前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: adSecurityFilterChain; publicAdSecurityFilterChain；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 独立管理链和精确公开GET拒绝其他method；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-ARCH-001/MC-REUSE-001/MC-VALID-001/MC-LOG-001/MC-BEAN-001/MC-UTIL-001/MC-PATTERN-001/MC-SCOPE-001：保持所选层，实际注解/Bean/边界见pseudocode。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：业务@Slf4j、显式Bean名，@RequiredArgsConstructor和每final@Qualifier；JDK/现库，Jackson/Instant/Duration；selectedpattern不换反射分派。配置factory用现@Bean参数Qualifier，接口不伪构造器。
- Implementation pseudocode:

```java
@Bean("adSecurityFilterChain") @Order(LOWEST_PRECEDENCE-3): matcher ads/ads/**;
reuse configureCommonSecurity; identity before Rbac3 before Anonymous; filterregistrationfalse guard;
anyrequest authenticated, businesspermission AdAuthorizationComponent;
@Bean("publicAdSecurityFilterChain") @Order(LOWEST_PRECEDENCE-4): matcher ad-slots/**;
permit only exact GET supportedroute when publicEnabled; denyAll othermethods/paths; no JWT dependency forpublic;
keep product order-2/file-1/baseLOWEST and old CORS/upload behavior;
qualified factoryparams as existing Springconfig pattern; no blanket /api permit fornewadmin.
```

- Verification contribution: Step 4的 `AdControllerTest,AdOpenApiContractTest,AdSecurityContractTest,AdManageTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: adSecurityFilterChain; publicAdSecurityFilterChain的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 13 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/handler/BaseExceptionHandler.java`

- Purpose: adPath专属错误映射而保持原wire
- Symbols: adPath; requestErrorStatus; mapBusinessStatus
- Repository evidence: 现Product/Brand/File scopedadvice及FieldViolationVO(field,constraint,message)
- Dependencies and consumers: 本Step伪代码列明的原组件/生成输入和具名Bean，§5对应后继owner消费；commit scope仅本file和明确依赖。
- Why now: 本位置承接Step4前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: adPath; requestErrorStatus; mapBusinessStatus；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: adPath专属错误映射而保持原wire；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-ARCH-001/MC-REUSE-001/MC-VALID-001/MC-LOG-001/MC-BEAN-001/MC-UTIL-001/MC-PATTERN-001/MC-SCOPE-001：保持所选层，实际注解/Bean/边界见pseudocode。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：业务@Slf4j、显式Bean名，@RequiredArgsConstructor和每final@Qualifier；JDK/现库，Jackson/Instant/Duration；selectedpattern不换反射分派。配置factory用现@Bean参数Qualifier，接口不伪构造器。
- Implementation pseudocode:

```java
add adPath covering only ads/ads/** and publicexactad-slots route;
validation400->610301; notfound302/site306->404; conflict303->409; forbidden305->403;
auth/ID/dependency/data-limit/data-invalid308/309/304/307/310->503; internal399->500;
fieldErrors use existingFieldErrorsVO and3fields; wrapperALWAYS, securityfilter2fieldexceptions retained;
maskunexpectedexceptions, preserveexistingoriginalrouting/multipart/filelimits/errors unchanged;
catch boundary exception only under existingadvice style, no broad business catchswallow.
```

- Verification contribution: Step 4的 `AdControllerTest,AdOpenApiContractTest,AdSecurityContractTest,AdManageTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: adPath; requestErrorStatus; mapBusinessStatus的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 14 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/goods/AdOpenApiConfiguration.java`

- Purpose: group和Create/Update请求schema投影
- Symbols: ads group; existing bearerAuth reuse; operation customizer
- Repository evidence: 同包ProductOpenApiConfiguration处理groups源schema与现springdocstarter
- Dependencies and consumers: 本Step伪代码列明的原组件/生成输入和具名Bean，§5对应后继owner消费；commit scope仅本file和明确依赖。
- Why now: 本位置承接Step4前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: ads group; existing bearerAuth reuse; operation customizer；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: group和Create/Update请求schema投影；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-ARCH-001/MC-REUSE-001/MC-VALID-001/MC-LOG-001/MC-BEAN-001/MC-UTIL-001/MC-PATTERN-001/MC-SCOPE-001：保持所选层，实际注解/Bean/边界见pseudocode。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：业务@Slf4j、显式Bean名，@RequiredArgsConstructor和每final@Qualifier；JDK/现库，Jackson/Instant/Duration；selectedpattern不换反射分派。配置factory用现@Bean参数Qualifier，接口不伪构造器。
- Implementation pseudocode:

```java
@Configuration("adOpenApiConfiguration",proxyBeanMethods=false);
@Bean namedadsGroupedOpenApi paths only sixoperations;
reuse bearerAuthdefinition and originalwrappergeneric; group customizer makesCreateversionnullonly,Updaterequired;
source validation/controllerfieldname sameauthority, no hand-maintainedfakewrapper;
publicsecurity=[] overrides bearerrequirement; prod docs remainfalse; test generatedschema snapshots.
```

- Verification contribution: Step 4的 `AdControllerTest,AdOpenApiContractTest,AdSecurityContractTest,AdManageTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: ads group; existing bearerAuth reuse; operation customizer的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 15 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/resources/application.yml`

- Purpose: 按同键注册ad站点和运行参数，真实启用后续发布输入
- Symbols: app.ad.public-enabled/public-sites/allowed-positions/candidate-limit/public-limit/load-timeout
- Repository evidence: enjoyshop-service/enjoyshop-service-goods/src/main/resources/application.yml既有profile相同结构；AdProperties binder
- Dependencies and consumers: 对应typedProperties→Commonphysicalpool/Native loader/Cachemanager及Ad/CDCconfiguration；同profile资源和NativeReadwriteContractTest消费键树；无新factory。
- Why now: 本位置承接Step4前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: app.ad.public-enabled/public-sites/allowed-positions/candidate-limit/public-limit/load-timeout；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 按同键注册ad站点和运行参数，真实启用后续发布输入；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-SCOPE-001/MC-TEST-001；MC-CONFIG-001/MC-TIME-001：配置键/预算沿明确模板；不含secret/无自动运行。
- Literal rule enforcement: Rule 11：保留Light/本任务scope；Rule 7：此配置与同profile文件成套；Rule 10：预算/记录UTC单位明确。
- Implementation pseudocode:

```yaml
add sameapp.ad tree inbase/dev/test/prod; public-enabled defaultfalse;
public-sites=controlled canonicalhost->positiveTenant exampleonly, not production guessedhost;
allowed-positions explicit initialweb_index_lb; candidate-limit200/public-limit20/load-timeoutPT2S;
retainspringdocprodfalse,identityregistrationfalse and oldCORS/securitykeys;
testisolatedfixtures enablepublic expressly; configurationenabledinvalidregistry/site failsstartup;
no actual gateway/platform route write, only releasecontract in Step8.
```

- Verification contribution: Step 4的 `AdControllerTest,AdOpenApiContractTest,AdSecurityContractTest,AdManageTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: app.ad.public-enabled/public-sites/allowed-positions/candidate-limit/public-limit/load-timeout的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 16 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-dev.yml`

- Purpose: 按同键注册ad站点和运行参数，真实启用后续发布输入
- Symbols: app.ad.public-enabled/public-sites/allowed-positions/candidate-limit/public-limit/load-timeout
- Repository evidence: enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-dev.yml既有profile相同结构；AdProperties binder
- Dependencies and consumers: 对应typedProperties→Commonphysicalpool/Native loader/Cachemanager及Ad/CDCconfiguration；同profile资源和NativeReadwriteContractTest消费键树；无新factory。
- Why now: 本位置承接Step4前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: app.ad.public-enabled/public-sites/allowed-positions/candidate-limit/public-limit/load-timeout；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 按同键注册ad站点和运行参数，真实启用后续发布输入；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-SCOPE-001/MC-TEST-001；MC-CONFIG-001/MC-TIME-001：配置键/预算沿明确模板；不含secret/无自动运行。
- Literal rule enforcement: Rule 11：保留Light/本任务scope；Rule 7：此配置与同profile文件成套；Rule 10：预算/记录UTC单位明确。
- Implementation pseudocode:

```yaml
add sameapp.ad tree inbase/dev/test/prod; public-enabled defaultfalse;
public-sites=controlled canonicalhost->positiveTenant exampleonly, not production guessedhost;
allowed-positions explicit initialweb_index_lb; candidate-limit200/public-limit20/load-timeoutPT2S;
retainspringdocprodfalse,identityregistrationfalse and oldCORS/securitykeys;
testisolatedfixtures enablepublic expressly; configurationenabledinvalidregistry/site failsstartup;
no actual gateway/platform route write, only releasecontract in Step8.
```

- Verification contribution: Step 4的 `AdControllerTest,AdOpenApiContractTest,AdSecurityContractTest,AdManageTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: app.ad.public-enabled/public-sites/allowed-positions/candidate-limit/public-limit/load-timeout的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 17 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-test.yml`

- Purpose: 按同键注册ad站点和运行参数，真实启用后续发布输入
- Symbols: app.ad.public-enabled/public-sites/allowed-positions/candidate-limit/public-limit/load-timeout
- Repository evidence: enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-test.yml既有profile相同结构；AdProperties binder
- Dependencies and consumers: 对应typedProperties→Commonphysicalpool/Native loader/Cachemanager及Ad/CDCconfiguration；同profile资源和NativeReadwriteContractTest消费键树；无新factory。
- Why now: 本位置承接Step4前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: app.ad.public-enabled/public-sites/allowed-positions/candidate-limit/public-limit/load-timeout；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 按同键注册ad站点和运行参数，真实启用后续发布输入；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-SCOPE-001/MC-TEST-001；MC-CONFIG-001/MC-TIME-001：配置键/预算沿明确模板；不含secret/无自动运行。
- Literal rule enforcement: Rule 11：保留Light/本任务scope；Rule 7：此配置与同profile文件成套；Rule 10：预算/记录UTC单位明确。
- Implementation pseudocode:

```yaml
add sameapp.ad tree inbase/dev/test/prod; public-enabled defaultfalse;
public-sites=controlled canonicalhost->positiveTenant exampleonly, not production guessedhost;
allowed-positions explicit initialweb_index_lb; candidate-limit200/public-limit20/load-timeoutPT2S;
retainspringdocprodfalse,identityregistrationfalse and oldCORS/securitykeys;
testisolatedfixtures enablepublic expressly; configurationenabledinvalidregistry/site failsstartup;
no actual gateway/platform route write, only releasecontract in Step8.
```

- Verification contribution: Step 4的 `AdControllerTest,AdOpenApiContractTest,AdSecurityContractTest,AdManageTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: app.ad.public-enabled/public-sites/allowed-positions/candidate-limit/public-limit/load-timeout的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 18 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-prod.yml`

- Purpose: 按同键注册ad站点和运行参数，真实启用后续发布输入
- Symbols: app.ad.public-enabled/public-sites/allowed-positions/candidate-limit/public-limit/load-timeout
- Repository evidence: enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-prod.yml既有profile相同结构；AdProperties binder
- Dependencies and consumers: 对应typedProperties→Commonphysicalpool/Native loader/Cachemanager及Ad/CDCconfiguration；同profile资源和NativeReadwriteContractTest消费键树；无新factory。
- Why now: 本位置承接Step4前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: app.ad.public-enabled/public-sites/allowed-positions/candidate-limit/public-limit/load-timeout；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 按同键注册ad站点和运行参数，真实启用后续发布输入；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-SCOPE-001/MC-TEST-001；MC-CONFIG-001/MC-TIME-001：配置键/预算沿明确模板；不含secret/无自动运行。
- Literal rule enforcement: Rule 11：保留Light/本任务scope；Rule 7：此配置与同profile文件成套；Rule 10：预算/记录UTC单位明确。
- Implementation pseudocode:

```yaml
add sameapp.ad tree inbase/dev/test/prod; public-enabled defaultfalse;
public-sites=controlled canonicalhost->positiveTenant exampleonly, not production guessedhost;
allowed-positions explicit initialweb_index_lb; candidate-limit200/public-limit20/load-timeoutPT2S;
retainspringdocprodfalse,identityregistrationfalse and oldCORS/securitykeys;
testisolatedfixtures enablepublic expressly; configurationenabledinvalidregistry/site failsstartup;
no actual gateway/platform route write, only releasecontract in Step8.
```

- Verification contribution: Step 4的 `AdControllerTest,AdOpenApiContractTest,AdSecurityContractTest,AdManageTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: app.ad.public-enabled/public-sites/allowed-positions/candidate-limit/public-limit/load-timeout的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 19 — `MODIFY docs/egon/codegen/ddl-consumption-log.md`

- Purpose: 追加Controller catalog apply与移包journal
- Symbols: append stage3 Controller generator record
- Repository evidence: 前两个apply日志 append-only
- Dependencies and consumers: 本Step伪代码列明的原组件/生成输入和具名Bean，§5对应后继owner消费；commit scope仅本file和明确依赖。
- Why now: 本位置承接Step4前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: append stage3 Controller generator record；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 追加Controller catalog apply与移包journal；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-SCOPE-001/MC-TEST-001；MC-SCOPE-001：仅追加/精确文档归属，标运行未验证。
- Literal rule enforcement: Rule 11：保留Light/本任务scope；规则中Java注解不适用此非Java文件；不引入运行/Schema新语义。
- Implementation pseudocode:

```markdown
append generatorconfig/inputSHA/actualplanid andexactnewcontrollerpath;
record semantic-preserving move adapter/ad->adapter/goods preserving SpecAPI/adviceboundary;
throughVersion:none forderivednonclasspathview, originalSQLprovenance unchanged;
noDBexecution/noeditedhistory/noextraAPI.
```

- Verification contribution: Step 4的 `AdControllerTest,AdOpenApiContractTest,AdSecurityContractTest,AdManageTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: append stage3 Controller generator record的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

- Validation working directory: `/Users/mario/SelfProject/enjoyshop`
- Verification command: `mvn -pl enjoyshop-service/enjoyshop-service-goods -am -Dtest=AdControllerTest,AdOpenApiContractTest,AdSecurityContractTest,AdManageTest -Dsurefire.failIfNoSpecifiedTests=false test`（先RED记录原因，最低实现后同命令GREEN）；
- Expected result: RED仅因上述缺失行为；GREEN exit0，named tests全部执行且无误跳过；离线检查对应断言通过。TEST019–022运行项仍明确Not run。
- Failure returns to: 本Step声明的测试/最低production/wiring file；若历史SQL/架构/工具范围冲突则回相应前置Step及§11，不绕过修复。
- Completion criteria: 本Step所有file/规则/MC证据闭合，测试报告匹配selectors、diff只本scope；不包含临时无用catalog类、秘密或组件源码。
- Rollback: 未运行SQL时仅撤销本Step自己写的path改动，保留他人内容；已部署SQL按§9追加forward-fix，不改旧脚本/history；下游持久消息/offset不丢。
- Commit paths: `enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/goods/AdControllerTest.java`；`enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/start/config/AdSecurityContractTest.java`；`enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/goods/AdOpenApiContractTest.java`；`enjoyshop-service/enjoyshop-service-goods/codegen/ad-codegen.json`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/ad/AdController.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/goods/AdController.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/ad/AdSaveCommand.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/filter/AdPublicTenantFilter.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/filter/TenantContextFilter.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/start/StudentManagementApplication.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/resources/mybatis/mapper/ad/AdDAO.xml`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/start/config/NativeHttpSecurityConfiguration.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/handler/BaseExceptionHandler.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/goods/AdOpenApiConfiguration.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/resources/application.yml`；`enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-dev.yml`；`enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-test.yml`；`enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-prod.yml`；`docs/egon/codegen/ddl-consumption-log.md`

- Commit: `feat(goods): expose secured advertisement api contracts`；HEAD `f3da9ee3696d956145a262f51f32a68651448bd5`，18个精确Step 4 paths。31项Controller/OpenAPI/security/Manage/NATIVE/profile测试通过，Plan strict与diff check通过；暂态净删除file不生成额外commit。

### Step 5 — 启用候选广告两级缓存并覆盖提交后失效

- Requirements: REQ-004, REQ-006, REQ-007, REQ-011
- Dependencies: Step 4 committed
- Baseline state: Step 4已提交HEAD `f3da9ee3696d956145a262f51f32a68651448bd5`；`feat(goods): expose secured advertisement api contracts` 的18个精确paths及Step 4定向测试报告见Step 4 Commit/测试记录。
- Observable outcome: 公开读命中L1/L2，miss只在PRIMARY事务回源；窗口每请求检查，管理写提交后失效old/new；TTL与SPI吞失败限制有证据。
- End state: 公开读命中L1/L2，miss只在PRIMARY事务回源；窗口每请求检查，管理写提交后失效old/new；TTL与SPI吞失败限制有证据。 后继内容只按后继Step发生；未部署本Step。
- Test-first gate: Required — 当前公开读直接PG，不命中AdCandidates；事务包裹cache会绕cache，写后无广告失效。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-JSON-001, MC-TIME-001, MC-CONFIG-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001；本Step文件角色对不适用注解写明理由，scope/test永远适用。
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；按每file角色落实，不让非Java运维文本伪承担Java类型规则。
- Ordered files:

#### File 1 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/ad/AdCacheContractTest.java`

- Purpose: 锁定候选缓存而非最终时间列表，以及真实Cache SPI局限
- Symbols: cacheHitBypassesLoader; primaryMiss; cachePortMissingLoadsPrimary; cachePayloadUsesWhitelistedTypes; utcWindow; invalidatesOldAndNew; swallowedEvictionFailure; publicReadValidatesStoredWindows
- Repository evidence: EgonColaTwoLevelCache.get/evict及EgonColaTwoLevelCachePort registerEvictionAfterCommit源码
- Dependencies and consumers: 本file调用/观察Step 5列明的实际subject（AdCacheContractTest,AdManageTest,AdControllerTest,NativeReadwriteContractTest）；复用JUnit/MockMvc/原testsupport，负面fixture不依赖外部服务。
- Why now: 先确立缺失行为的RED，后续最小实现须满足它。
- Contract/signature changes: cacheHitBypassesLoader; primaryMiss; cachePortMissingLoadsPrimary; cachePayloadUsesWhitelistedTypes; utcWindow; invalidatesOldAndNew; swallowedEvictionFailure; publicReadValidatesStoredWindows；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 锁定候选缓存而非最终时间列表，以及真实Cache SPI局限；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-VALID-001/MC-JSON-001/MC-BEAN-001/MC-CONFIG-001/MC-SCOPE-001/MC-TEST-001：测试pseudocode所列正负fixture/模型与wiring，不启外部服务。
- Literal rule enforcement: Rule 1, Rule 2, Rule 6, Rule 10, Rule 11：同模块testrole，逐约束/组/JSON/时序断言；测试类不是业务Bean，不伪加POJO构造注解。
- Implementation pseudocode:

```java
stub/instantiate existingCacheSPI with controlled L1/L2 and transaction manager, noRedis;
assert caller outerTransaction inactive; miss loader REQUIRES_NEW readOnly RR then cachefill aftertransaction;
hit calls noDAO; cachePort absent still loads once from bounded PRIMARY; row futurestart becomesvisible afterfixedClockadvance, endexclusive; clock onecall/request;
201enabledcandidates throws610307, no truncatedcacheddata; limit20 stable sort,id;
write moves positionA->B: register both keys inside transaction; rollback publishesnothing;
rawCacheevict mayreturn afterRediserrorwarn: listenerACK is attempt, notmultiPod/L2deletedassert;
inflightloader canfinish afterevict; TTL fromfill12/35s, notcommitdeadline; dependencyfailure503 not[].
```

- Verification contribution: Step 5的 `AdCacheContractTest,AdManageTest,AdControllerTest,NativeReadwriteContractTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: cacheHitBypassesLoader; primaryMiss; utcWindow; invalidatesOldAndNew; swallowedEvictionFailure的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 2 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/config/NativeReadwriteContractTest.java`

- Purpose: 校验区域配置和新读取事务预算
- Symbols: adRegionHasBoundedIndependentTtls
- Repository evidence: Step1profiletest及CacheProperties RegionTtl
- Dependencies and consumers: 本file调用/观察Step 5列明的实际subject（AdCacheContractTest,AdManageTest,AdControllerTest,NativeReadwriteContractTest）；复用JUnit/MockMvc/原testsupport，负面fixture不依赖外部服务。
- Why now: 先确立缺失行为的RED，后续最小实现须满足它。
- Contract/signature changes: adRegionHasBoundedIndependentTtls；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 校验区域配置和新读取事务预算；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-VALID-001/MC-JSON-001/MC-BEAN-001/MC-CONFIG-001/MC-SCOPE-001/MC-TEST-001：测试pseudocode所列正负fixture/模型与wiring，不启外部服务。
- Literal rule enforcement: Rule 1, Rule 2, Rule 6, Rule 10, Rule 11：同模块testrole，逐约束/组/JSON/时序断言；测试类不是业务Bean，不伪加POJO构造注解。
- Implementation pseudocode:

```java
assert AdCandidates exactregionkeycase matchesRepositorySPI;
assert 10+2s/30+5s bounds, sameprofiletree, no mutationglobalTTL;
assert primaryTemplate propagation/readOnly/RR/2s and public outermethodnotTransactional;
no defaultoutsideprocessinfrastructure startup; disabledCDCcontextasserts noPublisher/listener/factorydependencyfailure, typedconfigstillbindsbudgets.
```

- Verification contribution: Step 5的 `AdCacheContractTest,AdManageTest,AdControllerTest,NativeReadwriteContractTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: adRegionHasBoundedIndependentTtls的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 3 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/config/AdConfiguration.java`

- Purpose: 在既有事务管理器上创建主读Template
- Symbols: adPrimaryReadTransactionTemplate
- Repository evidence: Step3 config及Common primary transactional routing
- Dependencies and consumers: 本Step伪代码列明的原组件/生成输入和具名Bean，§5对应后继owner消费；commit scope仅本file和明确依赖。
- Why now: 本位置承接Step5前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: adPrimaryReadTransactionTemplate；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 在既有事务管理器上创建主读Template；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-ARCH-001/MC-REUSE-001/MC-VALID-001/MC-LOG-001/MC-BEAN-001/MC-UTIL-001/MC-PATTERN-001/MC-SCOPE-001：保持所选层，实际注解/Bean/边界见pseudocode。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：业务@Slf4j、显式Bean名，@RequiredArgsConstructor和每final@Qualifier；JDK/现库，Jackson/Instant/Duration；selectedpattern不换反射分派。配置factory用现@Bean参数Qualifier，接口不伪构造器。
- Implementation pseudocode:

```java
@Bean("adPrimaryReadTransactionTemplate") TransactionTemplate(@Qualifier("transactionManager") existing PlatformTransactionManager);
set PROPAGATION_REQUIRES_NEW/readOnly=true/ISOLATION_REPEATABLE_READ/timeout2s;
no newmanager/DataSource and no explicitreplica connection;
clock/spec/properties Beans unchanged; tests observe active boundary and NATIVE transaction PRIMARY rule.
```

- Verification contribution: Step 5的 `AdCacheContractTest,AdManageTest,AdControllerTest,NativeReadwriteContractTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: adPrimaryReadTransactionTemplate的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 4 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/ad/repo/AdRepository.java`

- Purpose: Repository拥有cache及PRIMARY miss loader，不循环注入Domain
- Symbols: cachedCandidates; loadCandidates; registerAdvertisementEviction; evictCandidates
- Repository evidence: Step3 Repository；existing EgonColaCachePort/Common tenantKeyGuard/restricted value codec；L2 codec allows java.util/java.time/java.lang/top.egon.cola only；application-test disables the Cache starter
- Dependencies and consumers: AdManage→AdDomainService/Impl→AdRepository→AdDAO/XML；MP-SDJ/CommonModel、namedConverters、CachePort/primaryTemplate通过明确Bean接入，Domain port不引用DAO。
- Why now: 本位置承接Step5前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: cachedCandidates; loadCandidates; registerAdvertisementEviction; evictCandidates；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: Repository拥有cache及PRIMARY miss loader，不循环注入Domain；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-ARCH-001/MC-REUSE-001/MC-VALID-001/MC-LOG-001/MC-BEAN-001/MC-UTIL-001/MC-PATTERN-001/MC-SCOPE-001：保持所选层，实际注解/Bean/边界见pseudocode。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：业务@Slf4j、显式Bean名，@RequiredArgsConstructor和每final@Qualifier；JDK/现库，Jackson/Instant/Duration；selectedpattern不换反射分派。配置factory用现@Bean参数Qualifier，接口不伪构造器。
- Implementation pseudocode:

```java
qualified final @Qualifier("egonColaCachePort") ObjectProvider<EgonColaCachePort> cacheProvider; cache bean absent when starter disabled -> bounded PRIMARY load;
qualified adPrimaryReadTransactionTemplate and AdProperties; @Validated query methods;
cachedCandidates(query) outside transaction ->cacheProvider.getIfAvailable(); absent => loadCandidates directly; present => cache.get("AdCandidates",tenant+":"+position,supplier);
supplier primaryTemplate.execute ->baseMapper.selectCandidates(currentTenant,position,201); validate persisted row shape/size<=200; serialize immutable snapshot via existing jacksonObjectMapper to JSON String before cache fill;
cache hit must be a String payload; deserialize with TypeReference<List<AdPO>>, validate and return a defensive list; invalid payload ->610310; the restricted L2 codec never stores top.egon.enjoyshop PO types;
no querycurrentwindow inSQL/cache; no serviceinjection intoRepository; disabled cache is a direct PRIMARY fallback, never an app-start failure;
registerAdvertisementEviction(old/new position): distinct allowedsyntax keys samecurrenttenant;
cache.registerEvictionAfterCommit("AdCandidates",exactKeys,List.of()) insidebusiness transaction;
CDCevictCandidates uses cache.registerEvictionAfterCommit outsidebusiness transaction per actualCachePort immediate scheduling behavior; guardMDC positive tenant; no directRedissonkeys;
cache failures that actuallythrow may controlledPRIMARY fallback; no swallowedfailure strongclaim.
```

- Verification contribution: Step 5的 `AdCacheContractTest,AdManageTest,AdControllerTest,NativeReadwriteContractTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: cachedCandidates; loadCandidates; registerAdvertisementEviction; evictCandidates的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 5 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/ad/service/impl/AdDomainServiceImpl.java`

- Purpose: 移除public cache外层事务，并把write失效登记放同业务事务
- Symbols: publishedCandidates; create; replace; delete
- Repository evidence: Step3 domainimpl及现SPI afterCommit contract
- Dependencies and consumers: AdManage→AdDomainService/Impl→AdRepository→AdDAO/XML；MP-SDJ/CommonModel、namedConverters、CachePort/primaryTemplate通过明确Bean接入，Domain port不引用DAO。
- Why now: 本位置承接Step5前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: publishedCandidates; create; replace; delete；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 移除public cache外层事务，并把write失效登记放同业务事务；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-ARCH-001/MC-REUSE-001/MC-VALID-001/MC-LOG-001/MC-BEAN-001/MC-UTIL-001/MC-PATTERN-001/MC-SCOPE-001：保持所选层，实际注解/Bean/边界见pseudocode。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：业务@Slf4j、显式Bean名，@RequiredArgsConstructor和每final@Qualifier；JDK/现库，Jackson/Instant/Duration；selectedpattern不换反射分派。配置factory用现@Bean参数Qualifier，接口不伪构造器。
- Implementation pseudocode:

```java
publishedCandidates has no @Transactional around cache call ->Repository.cachedCandidates; map to new BO then validate PublicRead including window/URL/position; map stored-row violations to AD_DATA_INVALID/503;
converter creates fresh BO perresult to avoid exposed mutablecachedPO; validatePublicRead input;
validate each loadedBO with PublicRead(extendsWrite) for URL/window/required businessfields before publicprojection;
known ConstraintViolationException for storeddata becomes610310 AD_DATA_INVALID/503, notclient400 orsilentfilter;
this covers directSQL rows whose URL is nonblank but violates the approved publicS3/target scheme contract;
create: after successfulsave register currentposition exactkey in same transaction;
replace: capture locked oldposition beforeMapStruct/update, aftersuccess register old/newunion;
delete: terminaltrue no unintendedversionwrite; afteractive CASsuccess register oldposition;
rollback neverexecutes eviction; return CRUDcommit beforeCDC; no applicationMQpublish.
```

- Verification contribution: Step 5的 `AdCacheContractTest,AdManageTest,AdControllerTest,NativeReadwriteContractTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: publishedCandidates; create; replace; delete的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 6 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/ad/AdBO.java`

- Purpose: validate cached source windows when a candidate is projected for public reads
- Symbols: `@ValidAdWindow(groups=Create,Update,PublicRead)`
- Repository evidence: current AdBO applies the cross-field/precision window validator only to Create and Update; Step 5 reads rows written by direct SQL and cache hits
- Dependencies and consumers: AdDomainServiceImpl validates candidates with PublicRead before AdManage eligibility and public projection
- Why now: public reads must reject invalid persisted/cache candidates as AD_DATA_INVALID instead of letting an inverted or sub-microsecond interval bypass the command boundary
- Contract/signature changes: include `AdValidationGroup.PublicRead` in the existing class-level ValidAdWindow groups; no field or wire shape changes
- Input/output and state mapping: PublicRead inherits Write constraints and now validates required Instant precision plus end-after-start
- Error and edge behavior: invalid source data fails closed as 503/610310; it is not silently filtered or exposed
- Standards impact: MC-VALID-001/MC-TIME-001/MC-SCOPE-001; reuse the existing pure Jakarta validator
- Literal rule enforcement: Rule 2 and Rule 10; preserve Domain's Jakarta-only boundary and UTC microsecond rules
- Implementation pseudocode:

```java
@ValidAdWindow(groups = {Create.class, Update.class, PublicRead.class})
// PublicRead inherits Write: retain all required fields, position and URL constraints.
validation.validate(candidate, AdValidationGroup.PublicRead.class);
// Map stored-row violations to AD_DATA_INVALID/503 before producing the public projection.
```

- Verification contribution: `AdCacheContractTest` submits a persisted candidate with an invalid window and proves PublicRead reports the stored-data violation
- After this file: the public candidate validation group includes the established window contract

#### File 7 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/ad/AdDomainQuery.java`

- Purpose: keep public position mandatory at the Domain boundary while leaving admin position filters optional
- Symbols: `position @NotBlank(groups=PublicRead.class)`
- Repository evidence: AdReadQuery already requires position; AdDomainQuery currently uses a nullable-friendly `@ValidAdPosition` for both page and public groups
- Dependencies and consumers: AdDomainServiceImpl and AdRepository validate PublicRead independently of the HTTP adapter
- Why now: direct internal callers must not turn a missing public slot into a successful empty list or cache key `tenant:null`
- Contract/signature changes: add NotBlank only to PublicRead; existing optional admin page filtering and allowed-position validation remain unchanged
- Input/output and state mapping: public query requires a canonical configured position and bounded limit; no tenant field is accepted from HTTP
- Error and edge behavior: null/blank public position fails 400/610301 before cache or DAO; admin list without position continues to mean no position filter
- Standards impact: MC-VALID-001/MC-SCOPE-001; group-local Jakarta validation only
- Literal rule enforcement: Rule 2 and Rule 11; preserve Domain's Spring-free boundary and existing query shape
- Implementation pseudocode:

```java
@NotBlank(groups = AdValidationGroup.PublicRead.class)
@ValidAdPosition(groups = {AdValidationGroup.Page.class, AdValidationGroup.PublicRead.class})
private String position;
```

- Verification contribution: `AdCacheContractTest` proves a direct PublicRead Domain call rejects missing position before repository access; existing admin page tests preserve optional filtering
- After this file: PublicRead domain queries cannot reach repository/cache with tenant:null-position keys

#### File 8 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/ad/AdManageImpl.java`

- Purpose: 保持一次Clock和public投影，防止外层事务绕cache
- Symbols: published(AdReadQuery)
- Repository evidence: Step3published implementeddirectPRIMARYread
- Dependencies and consumers: AdController→AdManage/Impl→AdDomainService，MapStruct输入BO/结果投影与AdAuthorizationComponent；相关typedgroups/Common wrappers，不直接引用Repository/DAO。
- Why now: 本位置承接Step5前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: published(AdReadQuery)；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 保持一次Clock和public投影，防止外层事务绕cache；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-ARCH-001/MC-REUSE-001/MC-VALID-001/MC-LOG-001/MC-BEAN-001/MC-UTIL-001/MC-PATTERN-001/MC-SCOPE-001：保持所选层，实际注解/Bean/边界见pseudocode。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：业务@Slf4j、显式Bean名，@RequiredArgsConstructor和每final@Qualifier；JDK/现库，Jackson/Instant/Duration；selectedpattern不换反射分派。配置factory用现@Bean参数Qualifier，接口不伪构造器。
- Implementation pseudocode:

```java
keep validation/tenant scope and oneclock.instant(); domainpublishedCandidates nowcached;
never annotate Manage.published transactional; filterviaAdEligibilitySpecification thenlimit and publicconverter;
adminpage/detail still RR PRIMARY, no regression in authorization or wrapper;
null/invalidcachecandidate/data overflow ->503610310/307 notemptylist.
```

- Verification contribution: Step 5的 `AdCacheContractTest,AdManageTest,AdControllerTest,NativeReadwriteContractTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: published(AdReadQuery)的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 9 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/resources/application.yml`

- Purpose: 只为AdCandidates覆盖TTL，其余区域不变
- Symbols: egon.cola.component.cache.regions.AdCandidates.*
- Repository evidence: enjoyshop-service/enjoyshop-service-goods/src/main/resources/application.ymlglobal TTL与现EgonColaCacheProperties.regions支持
- Dependencies and consumers: 对应typedProperties→Commonphysicalpool/Native loader/Cachemanager及Ad/CDCconfiguration；同profile资源和NativeReadwriteContractTest消费键树；无新factory。
- Why now: 本位置承接Step5前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: egon.cola.component.cache.regions.AdCandidates.*；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 只为AdCandidates覆盖TTL，其余区域不变；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-SCOPE-001/MC-TEST-001；MC-CONFIG-001/MC-TIME-001：配置键/预算沿明确模板；不含secret/无自动运行。
- Literal rule enforcement: Rule 11：保留Light/本任务scope；Rule 7：此配置与同profile文件成套；Rule 10：预算/记录UTC单位明确。
- Implementation pseudocode:

```yaml
regions.AdCandidates:
 l1-expire: PT10S; l1-jitter: PT2S; l2-expire: PT30S; l2-jitter: PT5S;
 do not add region null-expire: loader returns non-null List including emptyList, global nullTTL stays unchanged; regionL2 uses exact35smax independently;
retain alloldglobalttl/null/lock/second-evict-delay keys;
corekeys normalize dynamicmapentries as values (public-site hostname/source epoch), identical acrossprofiles, trace evict attempt vs successmetrics separately.
```

- Verification contribution: Step 5的 `AdCacheContractTest,AdManageTest,AdControllerTest,NativeReadwriteContractTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: egon.cola.component.cache.regions.AdCandidates.*的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 10 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-dev.yml`

- Purpose: 只为AdCandidates覆盖TTL，其余区域不变
- Symbols: egon.cola.component.cache.regions.AdCandidates.*
- Repository evidence: enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-dev.ymlglobal TTL与现EgonColaCacheProperties.regions支持
- Dependencies and consumers: 对应typedProperties→Commonphysicalpool/Native loader/Cachemanager及Ad/CDCconfiguration；同profile资源和NativeReadwriteContractTest消费键树；无新factory。
- Why now: 本位置承接Step5前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: egon.cola.component.cache.regions.AdCandidates.*；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 只为AdCandidates覆盖TTL，其余区域不变；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-SCOPE-001/MC-TEST-001；MC-CONFIG-001/MC-TIME-001：配置键/预算沿明确模板；不含secret/无自动运行。
- Literal rule enforcement: Rule 11：保留Light/本任务scope；Rule 7：此配置与同profile文件成套；Rule 10：预算/记录UTC单位明确。
- Implementation pseudocode:

```yaml
regions.AdCandidates:
 l1-expire: PT10S; l1-jitter: PT2S; l2-expire: PT30S; l2-jitter: PT5S;
 do not add region null-expire: loader returns non-null List including emptyList, global nullTTL stays unchanged; regionL2 uses exact35smax independently;
retain alloldglobalttl/null/lock/second-evict-delay keys;
corekeys normalize dynamicmapentries as values (public-site hostname/source epoch), identical acrossprofiles, trace evict attempt vs successmetrics separately.
```

- Verification contribution: Step 5的 `AdCacheContractTest,AdManageTest,AdControllerTest,NativeReadwriteContractTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: egon.cola.component.cache.regions.AdCandidates.*的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 11 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-test.yml`

- Purpose: 只为AdCandidates覆盖TTL，其余区域不变
- Symbols: egon.cola.component.cache.regions.AdCandidates.*
- Repository evidence: enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-test.ymlglobal TTL与现EgonColaCacheProperties.regions支持
- Dependencies and consumers: 对应typedProperties→Commonphysicalpool/Native loader/Cachemanager及Ad/CDCconfiguration；同profile资源和NativeReadwriteContractTest消费键树；无新factory。
- Why now: 本位置承接Step5前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: egon.cola.component.cache.regions.AdCandidates.*；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 只为AdCandidates覆盖TTL，其余区域不变；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-SCOPE-001/MC-TEST-001；MC-CONFIG-001/MC-TIME-001：配置键/预算沿明确模板；不含secret/无自动运行。
- Literal rule enforcement: Rule 11：保留Light/本任务scope；Rule 7：此配置与同profile文件成套；Rule 10：预算/记录UTC单位明确。
- Implementation pseudocode:

```yaml
regions.AdCandidates:
 l1-expire: PT10S; l1-jitter: PT2S; l2-expire: PT30S; l2-jitter: PT5S;
 do not add region null-expire: loader returns non-null List including emptyList, global nullTTL stays unchanged; regionL2 uses exact35smax independently;
retain alloldglobalttl/null/lock/second-evict-delay keys;
corekeys normalize dynamicmapentries as values (public-site hostname/source epoch), identical acrossprofiles, trace evict attempt vs successmetrics separately.
```

- Verification contribution: Step 5的 `AdCacheContractTest,AdManageTest,AdControllerTest,NativeReadwriteContractTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: egon.cola.component.cache.regions.AdCandidates.*的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 12 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-prod.yml`

- Purpose: 只为AdCandidates覆盖TTL，其余区域不变
- Symbols: egon.cola.component.cache.regions.AdCandidates.*
- Repository evidence: enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-prod.ymlglobal TTL与现EgonColaCacheProperties.regions支持
- Dependencies and consumers: 对应typedProperties→Commonphysicalpool/Native loader/Cachemanager及Ad/CDCconfiguration；同profile资源和NativeReadwriteContractTest消费键树；无新factory。
- Why now: 本位置承接Step5前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: egon.cola.component.cache.regions.AdCandidates.*；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 只为AdCandidates覆盖TTL，其余区域不变；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-SCOPE-001/MC-TEST-001；MC-CONFIG-001/MC-TIME-001：配置键/预算沿明确模板；不含secret/无自动运行。
- Literal rule enforcement: Rule 11：保留Light/本任务scope；Rule 7：此配置与同profile文件成套；Rule 10：预算/记录UTC单位明确。
- Implementation pseudocode:

```yaml
regions.AdCandidates:
 l1-expire: PT10S; l1-jitter: PT2S; l2-expire: PT30S; l2-jitter: PT5S;
 do not add region null-expire: loader returns non-null List including emptyList, global nullTTL stays unchanged; regionL2 uses exact35smax independently;
retain alloldglobalttl/null/lock/second-evict-delay keys;
corekeys normalize dynamicmapentries as values (public-site hostname/source epoch), identical acrossprofiles, trace evict attempt vs successmetrics separately.
```

- Verification contribution: Step 5的 `AdCacheContractTest,AdManageTest,AdControllerTest,NativeReadwriteContractTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: egon.cola.component.cache.regions.AdCandidates.*的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

- Validation working directory: `/Users/mario/SelfProject/enjoyshop`
- Verification command: `mvn -pl enjoyshop-service/enjoyshop-service-goods -am -Dtest=AdCacheContractTest,AdManageTest,AdControllerTest,NativeReadwriteContractTest -Dsurefire.failIfNoSpecifiedTests=false test`（先RED记录原因，最低实现后同命令GREEN）；
- Expected result: RED仅因上述缺失行为；GREEN exit0，named tests全部执行且无误跳过；离线检查对应断言通过。TEST019–022运行项仍明确Not run。
- Failure returns to: 本Step声明的测试/最低production/wiring file；若历史SQL/架构/工具范围冲突则回相应前置Step及§11，不绕过修复。
- Completion criteria: 本Step所有file/规则/MC证据闭合，测试报告匹配selectors、diff只本scope；不包含临时无用catalog类、秘密或组件源码。
- Rollback: 未运行SQL时仅撤销本Step自己写的path改动，保留他人内容；已部署SQL按§9追加forward-fix，不改旧脚本/history；下游持久消息/offset不丢。
- Commit paths: `enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/ad/AdCacheContractTest.java`；`enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/config/NativeReadwriteContractTest.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/config/AdConfiguration.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/ad/repo/AdRepository.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/ad/AdBO.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/ad/AdDomainQuery.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/ad/service/impl/AdDomainServiceImpl.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/ad/AdManageImpl.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/resources/application.yml`；`enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-dev.yml`；`enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-test.yml`；`enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-prod.yml`

- Commit: `feat(goods): cache advertisement candidates with primary refill`；HEAD `a9d385704b9f093860a9b36abc8eab5ceb9597af`，12个精确Step 5 paths。42项定向测试（AdCacheContract 10、AdManage 9、AdController 8、AdOpenAPI 1、AdSecurity 5、NativeReadwrite 3、ProfileKeyParity 6）全部通过，Plan strict与diff check通过；无真实Redis/PG运行验证。每逻辑任务至多一个path-limitedcommit；暂态净删除file不为其制造空commit，记录新HEAD。

### Step 6 — 定义PG事实规范化、稳定事件身份和搜索消费协议

- Requirements: REQ-003, REQ-007, REQ-008, REQ-009, REQ-011
- Dependencies: Step 5 committed
- Baseline state: Step 5已GREEN/提交，HEAD `a9d385704b9f093860a9b36abc8eab5ceb9597af`；前序已提供本file依赖。
- Observable outcome: 已提交raw facts可转为两个tenant隔离语义事件；身份稳定、old/new union完整；没有索引引擎。
- End state: 已提交raw facts可转为两个tenant隔离语义事件；身份稳定、old/new union完整；没有索引引擎。 后继内容只按后继Step发生；未部署本Step。
- Test-first gate: Required — 无事件/策略/identity，oldposition、deletebefore、SKU父迁移和重复投递未处理。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-JSON-001, MC-TIME-001, MC-CONFIG-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001；本Step文件角色对不适用注解写明理由，scope/test永远适用。
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；按每file角色落实，不让非Java运维文本伪承担Java类型规则。
- Ordered files:

#### File 1 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/sync/CdcContractTest.java`

- Purpose: 先定义vendor/消息/目标计算/身份和未来搜索合同
- Symbols: normalizesAdOldNew; normalizesSkuRoots; stableEventIdentity; rejectsPoison; refreshProtocolIsCurrentState
- Repository evidence: 既有RabbitMqMessageServiceImplTest；Spec TEST011–017 fixtures
- Dependencies and consumers: 本file调用/观察Step 6列明的实际subject（CdcContractTest,AdCacheContractTest）；复用JUnit/MockMvc/原testsupport，负面fixture不依赖外部服务。
- Why now: 先确立缺失行为的RED，后续最小实现须满足它。
- Contract/signature changes: normalizesAdOldNew; normalizesSkuRoots; stableEventIdentity; rejectsPoison; refreshProtocolIsCurrentState；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 先定义vendor/消息/目标计算/身份和未来搜索合同；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-VALID-001/MC-JSON-001/MC-BEAN-001/MC-CONFIG-001/MC-SCOPE-001/MC-TEST-001：测试pseudocode所列正负fixture/模型与wiring，不启外部服务。
- Literal rule enforcement: Rule 1, Rule 2, Rule 6, Rule 10, Rule 11：同模块testrole，逐约束/组/JSON/时序断言；测试类不是业务Bean，不伪加POJO构造注解。
- Implementation pseudocode:

```java
create fixedschemasDisabledrawad/spu/skuc/u/d/r fixtures from citedknownvendor shape, label releaseversionnotyetliveverified;
assert adtenant/position old/newunion; skuold/newspu roots; crossTenant max2separatemessages;
deleteusesbefore; missingbefore/PK/tenant/unavailableTOAST/unknownop/schema/source ->poison;
repeatcanonicalraw changesJSONkeyorder/deliveryTag/attempt ->same EID; PK/cursor/targettenant/routechanges ->differentEID;
assert source.ts notprocessingClock; numericbigint exact noDouble;
futurefakeprojection: delayedup triggersPRIMARYcurrentread, missing/down root ->emptymembershipreplacement;
assert receipt/fence/tombstone obligations and partial/unknown target preventsACK; noactualESimports/client/table.
```

- Verification contribution: Step 6的 `CdcContractTest,AdCacheContractTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: normalizesAdOldNew; normalizesSkuRoots; stableEventIdentity; rejectsPoison; refreshProtocolIsCurrentState的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 2 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/sync/CdcOperationEnum.java`

- Purpose: 明确合法数据操作和控制消息分流
- Symbols: CREATE c; UPDATE u; DELETE d; SNAPSHOT r
- Repository evidence: SpecEVENT001与Jakarta/enum规则
- Dependencies and consumers: CdcRawConsumer→CdcChangeStrategy接口→Ad/Product策略、identity与MapStruct→CdcEventPublisher；类型只由domain/common/JDK定义，事件field集合供协议validator。
- Why now: 本位置承接Step6前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: CREATE c; UPDATE u; DELETE d; SNAPSHOT r；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 明确合法数据操作和控制消息分流；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-NAME-001/MC-VALID-001/MC-JSON-001/MC-SCOPE-001：精确接口/enum/约束角色；无普通POJO注解误加，Common/Jakarta允许依赖。
- Literal rule enforcement: Rule 1, Rule 2, Rule 6, Rule 11：约束/enum/接口不是可变BO；按本file合法值/组注解；枚举@JsonValue/@EnumValue仅实际持久边界；不给接口生成构造器。
- Implementation pseudocode:

```java
enum stablecodes c/u/d/r; JsonValue forsemanticoperation; noordinal;
fromCode rejectsunknown ratherthandefault; t/DDL/control notvariant usedforbusinessrouting;
rawadapter recognizes onlyexactvalidatedBEGIN/END control separately; heartbeatdisabled.
```

- Verification contribution: Step 6的 `CdcContractTest,AdCacheContractTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: CREATE c; UPDATE u; DELETE d; SNAPSHOT r的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 3 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/sync/CdcSourceCursorBO.java`

- Purpose: 保留精确来源游标而不以时间排序
- Symbols: sourceAlias/sourceEpoch/schema/table/op/rowPk/lsn/txId/recordOrder/sourceTsMs
- Repository evidence: Spec §7 stablecanonicalidentity/§10BO；sourcemetadatajsonc
- Dependencies and consumers: CdcRawConsumer→CdcChangeStrategy接口→Ad/Product策略、identity与MapStruct→CdcEventPublisher；类型只由domain/common/JDK定义，事件field集合供协议validator。
- Why now: 本位置承接Step6前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: sourceAlias/sourceEpoch/schema/table/op/rowPk/lsn/txId/recordOrder/sourceTsMs；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 保留精确来源游标而不以时间排序；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-NAME-001/MC-VALID-001/MC-MODEL-001/MC-JSON-001/MC-TIME-001：普通class完整Lombok，root@Builder/继承@SuperBuilder+callSuper；Jakarta分组，UTC/wire/敏感字段按本file。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 6, Rule 10, Rule 11：@Data/@NoArgsConstructor/@AllArgsConstructor/@Accessors(chain=true)，匹配构造Builder；继承字段不重复；禁止record身份对象。
- Implementation pseudocode:

```java
ordinaryclass fullLombok@Builder; sourcealias/epoch requirednonblankregistered;
rowPK positiveLong; sourceLSN unsigned64 ->canonical highHex/lowHex exactBigInteger, snapshotnullable;
sourceTxIddecimalstring, recordOrderpositive whenmetadata; sourceTsMs Longmillis required;
Instant occurredAt fromsourceTsMs, snapshotmay采集时刻; nobridgeClock/currentdeliverymetadata;
Jakarta fieldconstraints alloweddomain, novendor/Rabbit/Springtypes.
```

- Verification contribution: Step 6的 `CdcContractTest,AdCacheContractTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: sourceAlias/sourceEpoch/schema/table/op/rowPk/lsn/txId/recordOrder/sourceTsMs的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 4 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/sync/CdcRecordBO.java`

- Purpose: 只保存经验证的row归属事实，领域不持JsonNode
- Symbols: cursor; before/after row identity; factFingerprint
- Repository evidence: Spec10 factsBO边界与before/after union要求
- Dependencies and consumers: CdcRawConsumer→CdcChangeStrategy接口→Ad/Product策略、identity与MapStruct→CdcEventPublisher；类型只由domain/common/JDK定义，事件field集合供协议validator。
- Why now: 本位置承接Step6前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: cursor; before/after row identity; factFingerprint；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 只保存经验证的row归属事实，领域不持JsonNode；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-NAME-001/MC-VALID-001/MC-MODEL-001/MC-JSON-001/MC-TIME-001：普通class完整Lombok，root@Builder/继承@SuperBuilder+callSuper；Jakarta分组，UTC/wire/敏感字段按本file。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 6, Rule 10, Rule 11：@Data/@NoArgsConstructor/@AllArgsConstructor/@Accessors(chain=true)，匹配构造Builder；继承字段不重复；禁止record身份对象。
- Implementation pseudocode:

```java
class fullLombok@Builder @Valid cursor @NotNull;
fields before/afterRowId/TenantId/SpuId/Position; onlyop-requiredsidesnullable;
rawadapterextractfacts/fromvendor types once; keepcanonicalbeforeAfterFactHash forEID;
CdcRecordBO domainrejects missingoldidentity onupdate/delete; positiveLongtenant/PK;
noStringObjectmap/Jacksonnode inDomain; samefacts allUPDATEstillevent evenchosencolumnsunchanged.
```

- Verification contribution: Step 6的 `CdcContractTest,AdCacheContractTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: cursor; before/after row identity; factFingerprint的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 5 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/sync/CdcChangeStrategy.java`

- Purpose: 具名Strategy合同针对真实两种目标计算
- Symbols: supports source table mapping; normalize(@Valid CdcRecordBO)
- Repository evidence: Spec §13.1 两种确定策略
- Dependencies and consumers: CdcRawConsumer→CdcChangeStrategy接口→Ad/Product策略、identity与MapStruct→CdcEventPublisher；类型只由domain/common/JDK定义，事件field集合供协议validator。
- Why now: 本位置承接Step6前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: supports source table mapping; normalize(@Valid CdcRecordBO)；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 具名Strategy合同针对真实两种目标计算；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-NAME-001/MC-VALID-001/MC-JSON-001/MC-SCOPE-001：精确接口/enum/约束角色；无普通POJO注解误加，Common/Jakarta允许依赖。
- Literal rule enforcement: Rule 1, Rule 2, Rule 6, Rule 11：约束/enum/接口不是可变BO；按本file合法值/组注解；枚举@JsonValue/@EnumValue仅实际持久边界；不给接口生成构造器。
- Implementation pseudocode:

```java
pureDomain interface CdcChangeStrategy:
 String routingKey(); List<? extends SyncEvent> normalize(@Valid @NotNull CdcRecordBO record);
 nested SyncEvent getters getEventId/getSchemaVersion/getTenantId/getSourceAlias/getSourceEpoch,
 the metadata genuinely required by publisher and validator; no extra wire field or new file;
 both approved Event classes implement SyncEvent; getters supplied Lombok;
 Ad/Product Strategy calculates own targetBO, uses qualified identity+MapStructConverter, returns own validatedEvents;
 registry Map<table,CdcChangeStrategy> explicit ad and spu/sku mappings, no instanceof/reflection switch;
 Domain SPI no Spring/Rabbit/Jacksonnode/PO implementation types.
```

- Verification contribution: Step 6的 `CdcContractTest,AdCacheContractTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: supports source table mapping; normalize(@Valid CdcRecordBO)的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 6 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/sync/AdInvalidationBO.java`

- Purpose: 本租户目标集合承载而非索引/缓存快照
- Symbols: AdInvalidationBO tenantId/cursor/positions
- Repository evidence: Spec10.1/AdInvalidationBO
- Dependencies and consumers: CdcRawConsumer→CdcChangeStrategy接口→Ad/Product策略、identity与MapStruct→CdcEventPublisher；类型只由domain/common/JDK定义，事件field集合供协议validator。
- Why now: 本位置承接Step6前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: AdInvalidationBO tenantId/cursor/positions；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 本租户目标集合承载而非索引/缓存快照；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-NAME-001/MC-VALID-001/MC-MODEL-001/MC-JSON-001/MC-TIME-001：普通class完整Lombok，root@Builder/继承@SuperBuilder+callSuper；Jakarta分组，UTC/wire/敏感字段按本file。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 6, Rule 10, Rule 11：@Data/@NoArgsConstructor/@AllArgsConstructor/@Accessors(chain=true)，匹配构造Builder；继承字段不重复；禁止record身份对象。
- Implementation pseudocode:

```java
class fullLombok@Builder; @Positive/@NotNull tenantId; @Valid/@NotNull cursor;
List<String> positions: @NotEmpty @Size(max=2), elementconstraints; List.copyOf defensive setter/builder result;
calculation = old/new (tenantId,position) union, groupedbytenant, sortedunique;
no fullrow/price/URLsnapshot/credentials; targetfacts only.
```

- Verification contribution: Step 6的 `CdcContractTest,AdCacheContractTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: AdInvalidationBO tenantId/cursor/positions的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 7 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/sync/ProductRefreshBO.java`

- Purpose: 本租户目标集合承载而非索引/缓存快照
- Symbols: ProductRefreshBO tenantId/cursor/spuIds
- Repository evidence: Spec10.1/ProductRefreshBO
- Dependencies and consumers: CdcRawConsumer→CdcChangeStrategy接口→Ad/Product策略、identity与MapStruct→CdcEventPublisher；类型只由domain/common/JDK定义，事件field集合供协议validator。
- Why now: 本位置承接Step6前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: ProductRefreshBO tenantId/cursor/spuIds；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 本租户目标集合承载而非索引/缓存快照；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-NAME-001/MC-VALID-001/MC-MODEL-001/MC-JSON-001/MC-TIME-001：普通class完整Lombok，root@Builder/继承@SuperBuilder+callSuper；Jakarta分组，UTC/wire/敏感字段按本file。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 6, Rule 10, Rule 11：@Data/@NoArgsConstructor/@AllArgsConstructor/@Accessors(chain=true)，匹配构造Builder；继承字段不重复；禁止record身份对象。
- Implementation pseudocode:

```java
class fullLombok@Builder; @Positive/@NotNull tenantId; @Valid/@NotNull cursor;
List<Long> spuIds: @NotEmpty @Size(max=2), elementconstraints; List.copyOf defensive setter/builder result;
calculation = spu rowid or sku before/after spuId union, groupedbytenant, sortedunique;
no fullrow/price/URLsnapshot/credentials; targetfacts only.
```

- Verification contribution: Step 6的 `CdcContractTest,AdCacheContractTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: ProductRefreshBO tenantId/cursor/spuIds的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 8 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/sync/AdCacheInvalidationEvent.java`

- Purpose: 外部v1 wire与领域BO分离
- Symbols: AdCacheInvalidationEvent eventId/schemaVersion/tenantId/sourceAlias/sourceEpoch/sourceLsn/sourceTxId/sourceRecordOrder/occurredAt/sourceTable/operation/positions
- Repository evidence: Spec9 EVENT002/003 fixedjsonc及10投影
- Dependencies and consumers: CdcRawConsumer→CdcChangeStrategy接口→Ad/Product策略、identity与MapStruct→CdcEventPublisher；类型只由domain/common/JDK定义，事件field集合供协议validator。
- Why now: 本位置承接Step6前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: AdCacheInvalidationEvent eventId/schemaVersion/tenantId/sourceAlias/sourceEpoch/sourceLsn/sourceTxId/sourceRecordOrder/occurredAt/sourceTable/operation/positions；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 外部v1 wire与领域BO分离；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-NAME-001/MC-VALID-001/MC-MODEL-001/MC-JSON-001/MC-TIME-001：普通class完整Lombok，root@Builder/继承@SuperBuilder+callSuper；Jakarta分组，UTC/wire/敏感字段按本file。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 6, Rule 10, Rule 11：@Data/@NoArgsConstructor/@AllArgsConstructor/@Accessors(chain=true)，匹配构造Builder；继承字段不重复；禁止record身份对象。
- Implementation pseudocode:

```java
ordinaryclass fullLombok@Builder, schemaVersioninteger1; eventId64lowerhex;
tenantIdLong andspuIdsLong serialize decimalstrings; positions1..2strings; collectionsdefensivecopies;
flatten sourcecursorfields exactlySpecjsonc, no extra nestedcursor/PK/rawfacts payload;
strict unknown top-levelreject via@JsonAnySetter, nullcursorfields explicit; InstantISOZ;
semanticconsumer before binding checks schemaVersion integral1, tenantId/spuIds textualdecimal strings,
source/event strings andtargets array exacttypes, rejecting Jackson's numeric/string coercions;
Jakarta recursiveconstraints onpositiveidentity/targets; noauthenticationtoken/HTTPwrapper;
futureconsumerconflictsameEIDdifferentcanonicalpayload quarantined, no search engine API.
implements CdcChangeStrategy.SyncEvent; existing fields/getters fulfill narrow common metadata contract, no inherited mutable envelope/new wire keys.
```

- Verification contribution: Step 6的 `CdcContractTest,AdCacheContractTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: AdCacheInvalidationEvent eventId/schemaVersion/tenantId/sourceAlias/sourceEpoch/sourceLsn/sourceTxId/sourceRecordOrder/occurredAt/sourceTable/operation/positions的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 9 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/sync/ProductRefreshRequestedEvent.java`

- Purpose: 外部v1 wire与领域BO分离
- Symbols: ProductRefreshRequestedEvent eventId/schemaVersion/tenantId/sourceAlias/sourceEpoch/sourceLsn/sourceTxId/sourceRecordOrder/occurredAt/sourceTable/operation/spuIds
- Repository evidence: Spec9 EVENT002/003 fixedjsonc及10投影
- Dependencies and consumers: CdcRawConsumer→CdcChangeStrategy接口→Ad/Product策略、identity与MapStruct→CdcEventPublisher；类型只由domain/common/JDK定义，事件field集合供协议validator。
- Why now: 本位置承接Step6前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: ProductRefreshRequestedEvent eventId/schemaVersion/tenantId/sourceAlias/sourceEpoch/sourceLsn/sourceTxId/sourceRecordOrder/occurredAt/sourceTable/operation/spuIds；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 外部v1 wire与领域BO分离；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-NAME-001/MC-VALID-001/MC-MODEL-001/MC-JSON-001/MC-TIME-001：普通class完整Lombok，root@Builder/继承@SuperBuilder+callSuper；Jakarta分组，UTC/wire/敏感字段按本file。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 6, Rule 10, Rule 11：@Data/@NoArgsConstructor/@AllArgsConstructor/@Accessors(chain=true)，匹配构造Builder；继承字段不重复；禁止record身份对象。
- Implementation pseudocode:

```java
ordinaryclass fullLombok@Builder, schemaVersioninteger1; eventId64lowerhex;
tenantIdLong andspuIdsLong serialize decimalstrings; positions1..2strings; collectionsdefensivecopies;
flatten sourcecursorfields exactlySpecjsonc, no extra nestedcursor/PK/rawfacts payload;
strict unknown top-levelreject via@JsonAnySetter, nullcursorfields explicit; InstantISOZ;
semanticconsumer before binding checks schemaVersion integral1, tenantId/spuIds textualdecimal strings,
source/event strings andtargets array exacttypes, rejecting Jackson's numeric/string coercions;
Jakarta recursiveconstraints onpositiveidentity/targets; noauthenticationtoken/HTTPwrapper;
futureconsumerconflictsameEIDdifferentcanonicalpayload quarantined, no search engine API.
implements CdcChangeStrategy.SyncEvent; existing fields/getters fulfill narrow common metadata contract, no inherited mutable envelope/new wire keys.
```

- Verification contribution: Step 6的 `CdcContractTest,AdCacheContractTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: ProductRefreshRequestedEvent eventId/schemaVersion/tenantId/sourceAlias/sourceEpoch/sourceLsn/sourceTxId/sourceRecordOrder/occurredAt/sourceTable/operation/spuIds的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 10 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/config/CdcSourceProperties.java`

- Purpose: 启动校验源registry和监听/投递预算
- Symbols: app.cdc enabled/sourceRegistry/maxPayloadBytes/confirmTimeout
- Repository evidence: Spec15.1 closedconfig；existing typedproperties
- Dependencies and consumers: CdcRabbitConfiguration绑定并校验；CdcRawConsumer/AdCacheInvalidationConsumer/Publisher读取registry与timeouts。
- Why now: 本位置承接Step6前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: app.cdc enabled/sourceRegistry/maxPayloadBytes/confirmTimeout；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 启动校验源registry和监听/投递预算；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-NAME-001/MC-VALID-001/MC-MODEL-001/MC-JSON-001/MC-TIME-001：普通class完整Lombok，root@Builder/继承@SuperBuilder+callSuper；Jakarta分组，UTC/wire/敏感字段按本file。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 6, Rule 10, Rule 11：@Data/@NoArgsConstructor/@AllArgsConstructor/@Accessors(chain=true)，匹配构造Builder；继承字段不重复；禁止record身份对象。
- Implementation pseudocode:

```java
class fullLombok@ConfigurationProperties("app.cdc",ignoreUnknownFields=false) @Validated;
enableddefaultfalse; maxPayloadBytes1048576; confirmTimeoutDuration5s;
sourceRegistry explicit master_data currentepoch/source.name/topic.prefix/db/public/(ad,spu,sku) mapping;
require unique sourceNames/currentepoch andexactroute association whenenabled;
oldEpoch orunknownsource rejects, noepochordercomparison/defaulttenant;
unknown/nonvalidconfig failclosed; no sourcecredentials printed in config logs.
```

- Verification contribution: Step 6的 `CdcContractTest,AdCacheContractTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: app.cdc enabled/sourceRegistry/maxPayloadBytes/confirmTimeout的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 11 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/sync/AdCdcChangeStrategy.java`

- Purpose: 实现确定目标算法，不查询DB
- Symbols: AdCdcChangeStrategy normalize(CdcRecordBO)
- Repository evidence: Spec7/13与当前Component纯计算组合
- Dependencies and consumers: 原BootObjectMapper/RabbitConnectionFactory与具名Strategy/MapStruct/identity/Publisher；rawQueue→semanticQueue→AdRepository cacheSPI，search无consumer。
- Why now: 本位置承接Step6前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: AdCdcChangeStrategy normalize(CdcRecordBO)；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 实现确定目标算法，不查询DB；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-ARCH-001/MC-REUSE-001/MC-VALID-001/MC-LOG-001/MC-BEAN-001/MC-UTIL-001/MC-PATTERN-001/MC-SCOPE-001：保持所选层，实际注解/Bean/边界见pseudocode。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：业务@Slf4j、显式Bean名，@RequiredArgsConstructor和每final@Qualifier；JDK/现库，Jackson/Instant/Duration；selectedpattern不换反射分派。配置factory用现@Bean参数Qualifier，接口不伪构造器。
- Implementation pseudocode:

```java
@Component("adCdcChangeStrategy") @Validated @Slf4j @RequiredArgsConstructor;
normalize(@Valid CdcRecordBO): sourceguard alreadyvalidated ad; collect before/after tenant+position; syntaxvalid oldposition can be revoked; union/group;
c/r onlyafter,d onlybefore,u bothrequired; missingidentity throwsSYNC_IDENTITY_INVALID;
positiveTenant/root and1..2targets pergroup; deterministic sort; mapAd/ProductBO;
no DB/cache/indexwrite; registryconstructorqualified, no reflection/scanning/fallbacktable;
safe logs sourceAlias/op/targetCount, no rawrowpayload.
qualified cdcEventIdentityComponent and adInvalidationEventConverterImpl; targetBO->event aftercanonicalEID; return List<? extends SyncEvent>, routingKey fixed ad.invalidate.v1/product.refresh.v1 respectively.
```

- Verification contribution: Step 6的 `CdcContractTest,AdCacheContractTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: AdCdcChangeStrategy normalize(CdcRecordBO)的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 12 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/sync/ProductCdcChangeStrategy.java`

- Purpose: 实现确定目标算法，不查询DB
- Symbols: ProductCdcChangeStrategy normalize(CdcRecordBO)
- Repository evidence: Spec7/13与当前Component纯计算组合
- Dependencies and consumers: 原BootObjectMapper/RabbitConnectionFactory与具名Strategy/MapStruct/identity/Publisher；rawQueue→semanticQueue→AdRepository cacheSPI，search无consumer。
- Why now: 本位置承接Step6前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: ProductCdcChangeStrategy normalize(CdcRecordBO)；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 实现确定目标算法，不查询DB；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-ARCH-001/MC-REUSE-001/MC-VALID-001/MC-LOG-001/MC-BEAN-001/MC-UTIL-001/MC-PATTERN-001/MC-SCOPE-001：保持所选层，实际注解/Bean/边界见pseudocode。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：业务@Slf4j、显式Bean名，@RequiredArgsConstructor和每final@Qualifier；JDK/现库，Jackson/Instant/Duration；selectedpattern不换反射分派。配置factory用现@Bean参数Qualifier，接口不伪构造器。
- Implementation pseudocode:

```java
@Component("productCdcChangeStrategy") @Validated @Slf4j @RequiredArgsConstructor;
normalize(@Valid CdcRecordBO): sourceguard alreadyvalidated spu,sku; for spu use rowid, sku use before/after spuId; collect old/new tenant+root; groupunion;
c/r onlyafter,d onlybefore,u bothrequired; missingidentity throwsSYNC_IDENTITY_INVALID;
positiveTenant/root and1..2targets pergroup; deterministic sort; mapAd/ProductBO;
no DB/cache/indexwrite; registryconstructorqualified, no reflection/scanning/fallbacktable;
safe logs sourceAlias/op/targetCount, no rawrowpayload.
qualified cdcEventIdentityComponent and productRefreshEventConverterImpl; targetBO->event aftercanonicalEID; return List<? extends SyncEvent>, routingKey fixed ad.invalidate.v1/product.refresh.v1 respectively.
```

- Verification contribution: Step 6的 `CdcContractTest,AdCacheContractTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: ProductCdcChangeStrategy normalize(CdcRecordBO)的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 13 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/sync/CdcEventIdentityComponent.java`

- Purpose: 稳定SHA256身份与内容指纹，不用接收时间
- Symbols: eventId(CdcRecordBO,Long targetTenant,String routingKey)
- Repository evidence: Spec7.3及JDKMessageDigest/canonicalJSON项目Jackson
- Dependencies and consumers: 原BootObjectMapper/RabbitConnectionFactory与具名Strategy/MapStruct/identity/Publisher；rawQueue→semanticQueue→AdRepository cacheSPI，search无consumer。
- Why now: 本位置承接Step6前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: eventId(CdcRecordBO,Long targetTenant,String routingKey)；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 稳定SHA256身份与内容指纹，不用接收时间；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-ARCH-001/MC-REUSE-001/MC-VALID-001/MC-LOG-001/MC-BEAN-001/MC-UTIL-001/MC-PATTERN-001/MC-SCOPE-001：保持所选层，实际注解/Bean/边界见pseudocode。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：业务@Slf4j、显式Bean名，@RequiredArgsConstructor和每final@Qualifier；JDK/现库，Jackson/Instant/Duration；selectedpattern不换反射分派。配置factory用现@Bean参数Qualifier，接口不伪构造器。
- Implementation pseudocode:

```java
@Component("cdcEventIdentityComponent") @Slf4j @RequiredArgsConstructor;
qualified existingjacksonObjectMapper; canonical sortedfieldUTF8 facts (sourcealias/epoch/schema/table/op/PK/lsn/tx/order/sourceTs/beforeAfterFactHash/targetTenant/route);
SHA256 lowercasehex; no noncanonicaldouble/date, no attemptedeliveryTag/bridgeClock;
nullsnapshotcursorhasexplicitnulltag, do notcollapseabsent withzerovalue;
fixturepairs prove repeatedsourcefact sameEID anddifferenttenantroute differentEID;
semanticcanonicalcontent fingerprint separatefutureduplicateguard; no newidentityStore/table.
```

- Verification contribution: Step 6的 `CdcContractTest,AdCacheContractTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: eventId(CdcRecordBO,Long targetTenant,String routingKey)的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 14 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/sync/AdInvalidationEventConverter.java`

- Purpose: 事件稳定wire投影经Common Converter
- Symbols: AdInvalidationEventConverter extends BaseForwardConverter<AdInvalidationBO,AdCacheInvalidationEvent>
- Repository evidence: Spec10.4 MapStruct与Product converters
- Dependencies and consumers: 原BootObjectMapper/RabbitConnectionFactory与具名Strategy/MapStruct/identity/Publisher；rawQueue→semanticQueue→AdRepository cacheSPI，search无consumer。
- Why now: 本位置承接Step6前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: AdInvalidationEventConverter extends BaseForwardConverter<AdInvalidationBO,AdCacheInvalidationEvent>；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 事件稳定wire投影经Common Converter；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-CONVERT-001/MC-BEAN-001/MC-JSON-001/MC-TIME-001：MapStruct/Common基类与准确generics/mapping、实现Bean名、无反射/BeanUtils搬字段。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 10, Rule 11：保留BaseConverter/BaseForwardConverter；@Mapper spring ERROR；消费者final@Qualifier实际生成Impl名；默认转换不得吞null/unknown。
- Implementation pseudocode:

```java
@Mapper(componentModel="spring",unmappedTargetPolicy=ERROR,builderdisableBuilder=true)
interface AdInvalidationEventConverter extends BaseForwardConverter<AdInvalidationBO,AdCacheInvalidationEvent>;
explicit MapStruct cursor->flattenedsourcefields, targettenant+collection->samequalifiedwire,
schemaVersion=1, eventId provided fromidentityComponent beforeconversion;
instantfromsourceTime, no currentclock; no network/readrepo in mapper;
generatedBean adInvalidationEventConverterImpl qualifiers onpublisher/consumer.
```

- Verification contribution: Step 6的 `CdcContractTest,AdCacheContractTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: AdInvalidationEventConverter extends BaseForwardConverter<AdInvalidationBO,AdCacheInvalidationEvent>的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 15 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/sync/ProductRefreshEventConverter.java`

- Purpose: 事件稳定wire投影经Common Converter
- Symbols: ProductRefreshEventConverter extends BaseForwardConverter<ProductRefreshBO,ProductRefreshRequestedEvent>
- Repository evidence: Spec10.4 MapStruct与Product converters
- Dependencies and consumers: 原BootObjectMapper/RabbitConnectionFactory与具名Strategy/MapStruct/identity/Publisher；rawQueue→semanticQueue→AdRepository cacheSPI，search无consumer。
- Why now: 本位置承接Step6前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: ProductRefreshEventConverter extends BaseForwardConverter<ProductRefreshBO,ProductRefreshRequestedEvent>；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 事件稳定wire投影经Common Converter；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-CONVERT-001/MC-BEAN-001/MC-JSON-001/MC-TIME-001：MapStruct/Common基类与准确generics/mapping、实现Bean名、无反射/BeanUtils搬字段。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 10, Rule 11：保留BaseConverter/BaseForwardConverter；@Mapper spring ERROR；消费者final@Qualifier实际生成Impl名；默认转换不得吞null/unknown。
- Implementation pseudocode:

```java
@Mapper(componentModel="spring",unmappedTargetPolicy=ERROR,builderdisableBuilder=true)
interface ProductRefreshEventConverter extends BaseForwardConverter<ProductRefreshBO,ProductRefreshRequestedEvent>;
explicit MapStruct cursor->flattenedsourcefields, targettenant+collection->samequalifiedwire,
schemaVersion=1, eventId provided fromidentityComponent beforeconversion;
instantfromsourceTime, no currentclock; no network/readrepo in mapper;
generatedBean productRefreshEventConverterImpl qualifiers onpublisher/consumer.
```

- Verification contribution: Step 6的 `CdcContractTest,AdCacheContractTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: ProductRefreshEventConverter extends BaseForwardConverter<ProductRefreshBO,ProductRefreshRequestedEvent>的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

- Validation working directory: `/Users/mario/SelfProject/enjoyshop`
- Verification command: `mvn -pl enjoyshop-service/enjoyshop-service-goods -am -Dtest=CdcContractTest,AdCacheContractTest -Dsurefire.failIfNoSpecifiedTests=false test`（先RED记录原因，最低实现后同命令GREEN）；
- Expected result: RED仅因上述缺失行为；GREEN exit0，named tests全部执行且无误跳过；离线检查对应断言通过。TEST019–022运行项仍明确Not run。
- Failure returns to: 本Step声明的测试/最低production/wiring file；若历史SQL/架构/工具范围冲突则回相应前置Step及§11，不绕过修复。
- Completion criteria: 本Step所有file/规则/MC证据闭合，测试报告匹配selectors、diff只本scope；不包含临时无用catalog类、秘密或组件源码。
- Rollback: 未运行SQL时仅撤销本Step自己写的path改动，保留他人内容；已部署SQL按§9追加forward-fix，不改旧脚本/history；下游持久消息/offset不丢。
- Commit paths: `enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/sync/CdcContractTest.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/sync/CdcOperationEnum.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/sync/CdcSourceCursorBO.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/sync/CdcRecordBO.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/sync/CdcChangeStrategy.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/sync/AdInvalidationBO.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/sync/ProductRefreshBO.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/sync/AdCacheInvalidationEvent.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/sync/ProductRefreshRequestedEvent.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/config/CdcSourceProperties.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/sync/AdCdcChangeStrategy.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/sync/ProductCdcChangeStrategy.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/sync/CdcEventIdentityComponent.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/sync/AdInvalidationEventConverter.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/sync/ProductRefreshEventConverter.java`

- Commit: `feat(goods): normalize postgres cdc synchronization events`；HEAD `956b1e990c5e513bf18bfbe98e2382867881c67c`，15个精确Step 6 paths。52项CDC/缓存/管理/HTTP/OpenAPI/安全/profile/架构定向测试全部通过，Plan strict与diff check通过；未运行Debezium/Rabbit/PG或搜索引擎。每逻辑任务至多一个path-limitedcommit；暂态净删除file不为其制造空commit，记录新HEAD。

### Step 7 — 接入Rabbit确认投递、有限重试和广告失效消费

- Requirements: REQ-003, REQ-005, REQ-007, REQ-008, REQ-009, REQ-010, REQ-011
- Dependencies: Step 6 committed
- Baseline state: Step 6已GREEN/提交，HEAD `956b1e990c5e513bf18bfbe98e2382867881c67c`；前序已提供本file依赖。
- Observable outcome: raw只在全部semantic confirm且无return后ACK；重投身份稳定、poison隔离；Ad消费验证scope后尝试SPI失效，search无假消费。
- End state: raw只在全部semantic confirm且无return后ACK；重投身份稳定、poison隔离；Ad消费验证scope后尝试SPI失效，search无假消费。 后继内容只按后继Step发生；未部署本Step。
- Test-first gate: Required — 无CdcRabbitTemplate/监听器；当前sample afterCommit发送不提供此链的确认/持久ACK语义。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-JSON-001, MC-TIME-001, MC-CONFIG-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001；本Step文件角色对不适用注解写明理由，scope/test永远适用。
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；按每file角色落实，不让非Java运维文本伪承担Java类型规则。
- Ordered files:

#### File 1 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/sync/CdcContractTest.java`

- Purpose: 覆盖确认/return/重试/ACK和异常scope清理
- Symbols: partialConfirmDoesNotAck; redeliveryStableIdentity; invalidSourceRejects; manualRejectAfterFiniteRetries
- Repository evidence: Step6 factsfixtures；当前RabbitMQ sample tests
- Dependencies and consumers: 本file调用/观察Step 7列明的实际subject（CdcContractTest,AdCacheContractTest,AdSecurityContractTest,NativeReadwriteContractTest）；复用JUnit/MockMvc/原testsupport，负面fixture不依赖外部服务。
- Why now: 先确立缺失行为的RED，后续最小实现须满足它。
- Contract/signature changes: partialConfirmDoesNotAck; redeliveryStableIdentity; invalidSourceRejects; manualRejectAfterFiniteRetries；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 覆盖确认/return/重试/ACK和异常scope清理；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-VALID-001/MC-JSON-001/MC-BEAN-001/MC-CONFIG-001/MC-SCOPE-001/MC-TEST-001：测试pseudocode所列正负fixture/模型与wiring，不启外部服务。
- Literal rule enforcement: Rule 1, Rule 2, Rule 6, Rule 10, Rule 11：同模块testrole，逐约束/组/JSON/时序断言；测试类不是业务Bean，不伪加POJO构造注解。
- Implementation pseudocode:

```java
fake CorrelationData futures/channel and source registry; nobroker;
publish2tenantgroups: firstack secondtimeout/return ->rawnoACK; repeat ->samefirstEID;
allconfirmed+noreturn ->basicAck once; rawACKloss/redelivery ->duplicate safe;
retry maxAttempts3 inclinitial, waits1s/2s, nofourthattempt orpostterminalsleep;
permanentvalidation rejectsfalse immediately; exhaustedtransient rejectsmanualtrue to durableDLQcontract;
legitimate BEGIN/ENDshape acceptedonlyontrustedsource fixedroute; missingrowop/sourcepoisonnotcontrol;
Adconsumer validatesheader/body/epoch/tenant, evicts1..2keys, finallyrestoreMDC;
cacheSPI swallowedwarn ->ACKattempt only; no fakeassertmultiPodL2success;
assert search queue declared but no listener/consumer/projectionStore.
```

- Verification contribution: Step 7的 `CdcContractTest,AdCacheContractTest,AdSecurityContractTest,NativeReadwriteContractTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: partialConfirmDoesNotAck; redeliveryStableIdentity; invalidSourceRejects; manualRejectAfterFiniteRetries的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 2 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/config/NativeReadwriteContractTest.java`

- Purpose: 将CDC flags/源权限预算纳入同键静态验证
- Symbols: cdcDisabledByDefault; originalSampleConfigurationUnchanged
- Repository evidence: Step1 profilesbaseline及SpecTEST023
- Dependencies and consumers: 本file调用/观察Step 7列明的实际subject（CdcContractTest,AdCacheContractTest,AdSecurityContractTest,NativeReadwriteContractTest）；复用JUnit/MockMvc/原testsupport，负面fixture不依赖外部服务。
- Why now: 先确立缺失行为的RED，后续最小实现须满足它。
- Contract/signature changes: cdcDisabledByDefault; originalSampleConfigurationUnchanged；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 将CDC flags/源权限预算纳入同键静态验证；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-VALID-001/MC-JSON-001/MC-BEAN-001/MC-CONFIG-001/MC-SCOPE-001/MC-TEST-001：测试pseudocode所列正负fixture/模型与wiring，不启外部服务。
- Literal rule enforcement: Rule 1, Rule 2, Rule 6, Rule 10, Rule 11：同模块testrole，逐约束/组/JSON/时序断言；测试类不是业务Bean，不伪加POJO构造注解。
- Implementation pseudocode:

```java
bindallprofiles withoutserver; assert cdc=false andvalidtypedbudgets;
assert rawlistener exclusivefactoryMANUAL/prefetch32/threeattempts/confirmreturnenabled;
assert originalRabbitMqConfig/MqRouteEnum contenthash unchanged;
assert no Kafka/Debezium/search/outbox dependency inGoodsPOM andno newcomponent-sourcewrites.
```

- Verification contribution: Step 7的 `CdcContractTest,AdCacheContractTest,AdSecurityContractTest,NativeReadwriteContractTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: cdcDisabledByDefault; originalSampleConfigurationUnchanged的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 3 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/config/CdcRabbitConfiguration.java`

- Purpose: 独立具名Template/Factory/显式Strategy注册和关闭默认
- Symbols: cdcRabbitTemplate; cdcRabbitListenerContainerFactory; cdcChangeStrategies; queue/exchange/binding beans
- Repository evidence: 当前RabbitMqConfig已有factory，不改sampleconverter/routes；现CommonMPPOM AMQP
- Dependencies and consumers: 本Step伪代码列明的原组件/生成输入和具名Bean，§5对应后继owner消费；commit scope仅本file和明确依赖。
- Why now: 本位置承接Step7前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: cdcRabbitTemplate; cdcRabbitListenerContainerFactory; cdcChangeStrategies; queue/exchange/binding beans；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 独立具名Template/Factory/显式Strategy注册和关闭默认；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-ARCH-001/MC-REUSE-001/MC-VALID-001/MC-LOG-001/MC-BEAN-001/MC-UTIL-001/MC-PATTERN-001/MC-SCOPE-001：保持所选层，实际注解/Bean/边界见pseudocode。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：业务@Slf4j、显式Bean名，@RequiredArgsConstructor和每final@Qualifier；JDK/现库，Jackson/Instant/Duration；selectedpattern不换反射分派。配置factory用现@Bean参数Qualifier，接口不伪构造器。
- Implementation pseudocode:

```java
@Configuration("cdcRabbitConfiguration",proxyBeanMethods=false) @EnableRabbit always registers validatedCdcSourceProperties;
individual Rabbit-specific @Bean methods guarded @ConditionalOnProperty(app.cdc.enabled=true);
explicit Bean names cdcRawExchange/cdcSyncExchange/cdcRawQueue/cdcAdQueue/cdcSearchQueue,
cdcRawBinding/cdcAdBinding/cdcSearchBinding/cdcDeadLetterExchange/cdcUnroutedExchange andspecificquarantine/deadqueues;
all DI finalfields use exactqualifiers rabbitConnectionFactory/jacksonObjectMapper/cdcRabbitTemplate/
app.cdc-top.egon.enjoyshop.goods.infrastructure.config.CdcSourceProperties;
AdProperties qualifier is app.ad-top.egon.enjoyshop.goods.infrastructure.config.AdProperties;
@EnableConfigurationProperties(CdcSourceProperties.class); require originalRabbitconnectionFactoryconfirm/returns enabled;
@Bean cdcRabbitTemplate(originalConnectionFactory): mandatory=true, correlatedconfirm, persistentJSONbytes;
@Bean cdcRabbitListenerContainerFactory via existingBootConfigurer: MANUAL,prefetch32,
 retryAdvice maxAttempts3, exponentialbackoff1000*2 max10000, no defaultrequeue;
 MessageRecoverer throws AmqpRejectAndDontRequeueException(safeReason,true,cause) for exhausted transient,
 soMANUAL mode rejects; 已对本机3.2.12 JAR javap核对(String,boolean,Throwable) constructor；执行前仍校验实际resolved版本，无手写customretryrunner;
 declare durabletopics raw=enjoyshop.cdc.raw.v1 andsync=enjoyshop.sync.v1;
 rawquorum=enjoyshop.cdc.normalize.v1 fixedbindingmaster-data +x-single-active-consumer;
 adquorum=enjoyshop.ad.invalidate.v1 routead.invalidate.v1; searchquorum=enjoyshop.search.refresh.v1 routeproduct.refresh.v1;
 declare durable quarantine/DLXqueues as Step8operatorcontract; brokerpoliciesatleastoncedeadletter separate;
 @Bean cdcChangeStrategies qualifiedMap ad->adCdcChangeStrategy,spu/sku->productCdcChangeStrategy;
 failduplicate/unknownaliasregistry beforeconsume; no scan/reflection/commondefaultstrategy;
 no modificationRabbitMqMessageServiceImpl/MqRouteEnum/shareddefaultconverter.
```

- Verification contribution: Step 7的 `CdcContractTest,AdCacheContractTest,AdSecurityContractTest,NativeReadwriteContractTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: cdcRabbitTemplate; cdcRabbitListenerContainerFactory; cdcChangeStrategies; queue/exchange/binding beans的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 4 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/sync/CdcEventPublisher.java`

- Purpose: transport确认仅在全部目标成功后返回
- Symbols: publishConfirmed(List<? extends SyncEvent>,String routingKey)
- Repository evidence: Spec7/9 confirmations与既有RabbitTemplate dependency
- Dependencies and consumers: 原BootObjectMapper/RabbitConnectionFactory与具名Strategy/MapStruct/identity/Publisher；rawQueue→semanticQueue→AdRepository cacheSPI，search无consumer。
- Why now: 本位置承接Step7前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: publishConfirmed(List<? extends SyncEvent>,String routingKey)；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: transport确认仅在全部目标成功后返回；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-ARCH-001/MC-REUSE-001/MC-VALID-001/MC-LOG-001/MC-BEAN-001/MC-UTIL-001/MC-PATTERN-001/MC-SCOPE-001：保持所选层，实际注解/Bean/边界见pseudocode。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：业务@Slf4j、显式Bean名，@RequiredArgsConstructor和每final@Qualifier；JDK/现库，Jackson/Instant/Duration；selectedpattern不换反射分派。配置factory用现@Bean参数Qualifier，接口不伪构造器。
- Implementation pseudocode:

```java
@ConditionalOnProperty(name="app.cdc.enabled",havingValue="true") @Component("cdcEventPublisher") @Validated @Slf4j @RequiredArgsConstructor;
qualified cdcRabbitTemplate/jacksonObjectMapper/CdcSourceProperties/egonColaValidationUtils;
for validatedevent: serialize actual typedEvent via Jackson toUTF8bytes;
Message properties persistent,application/json,messageId=eventId,AMQP headers.schemaVersion=1 integer; no userJWT;
CorrelationData transport ID is unique per send attempt and includes eventId plus random suffix; stable eventId remains body/messageId and survives redelivery;
await allrequired futures bounded5s each within configuredconsumerbudget;
ack=false/exception/timeout/getReturned!=null throws transientdeliveryerror; no optimisticvoidsuccess;
only return after allack+noReturn; templateversionreturn-beforeconfirm ordering verified in contract/live;
partialacceptedlist not rolledback, samefactsretries canrepeat; safeids/resultslogs, neverbody.
```

- Verification contribution: Step 7的 `CdcContractTest,AdCacheContractTest,AdSecurityContractTest,NativeReadwriteContractTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: publishConfirmed(List<? extends SyncEvent>,String routingKey)的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 5 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/sync/CdcRawConsumer.java`

- Purpose: source strict解析→Strategy→confirmed publish→manualACK
- Symbols: onMessage(Message,Channel); rawschema/control validation
- Repository evidence: Spec rawEVENT001；BootJackson/RabbitListener/API scope currentadapter
- Dependencies and consumers: 原BootObjectMapper/RabbitConnectionFactory与具名Strategy/MapStruct/identity/Publisher；rawQueue→semanticQueue→AdRepository cacheSPI，search无consumer。
- Why now: 本位置承接Step7前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: onMessage(Message,Channel); rawschema/control validation；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: source strict解析→Strategy→confirmed publish→manualACK；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-ARCH-001/MC-REUSE-001/MC-VALID-001/MC-LOG-001/MC-BEAN-001/MC-UTIL-001/MC-PATTERN-001/MC-SCOPE-001：保持所选层，实际注解/Bean/边界见pseudocode。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：业务@Slf4j、显式Bean名，@RequiredArgsConstructor和每final@Qualifier；JDK/现库，Jackson/Instant/Duration；selectedpattern不换反射分派。配置factory用现@Bean参数Qualifier，接口不伪构造器。
- Implementation pseudocode:

```java
@ConditionalOnProperty(name="app.cdc.enabled",havingValue="true") @Component("cdcRawConsumer") @Slf4j @Validated @RequiredArgsConstructor;
@RabbitListener(queues="enjoyshop.cdc.normalize.v1",containerFactory="cdcRabbitListenerContainerFactory");
qualified jacksonObjectMapper/CdcSourceProperties/cdcChangeStrategies/cdcEventPublisher/egonColaValidationUtils;
maxpayload1MiB check beforeparse; exactsource.name/db/public/table/op/epoch whitelist; bigint exacttree integral notdouble;
rowrequiredc/r after,d before,u both; sourcecursor normalization andfacthash fillCdcRecordBO;
validate BO recursively beforeStrategy; lookupMap exacttable thennormalize ->publishConfirmed;
alloutputs success ->basicAck(deliveryTag,false); no DB/Redis read/no businesssourcewrites;
trustedfixedroute BEGIN/ENDcontrol strictfixture only ->safeACK; heartbeatdisabled;
known permanentformat/schema/source/identity constraint errors ->basicReject(tag,false), no retry or new event;
transport failures propagate to finiteRetryAdvice; no manualearlyACK or catch-and-success;
unknown outcome sourceoriginalbody retainedDLQ; no mutation/fallbacktenant; lowcardinalitylags/poisonlogs.
```

- Verification contribution: Step 7的 `CdcContractTest,AdCacheContractTest,AdSecurityContractTest,NativeReadwriteContractTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: onMessage(Message,Channel); rawschema/control validation的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 6 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/sync/AdCacheInvalidationConsumer.java`

- Purpose: 唯一消息消费效果是租户失效尝试
- Symbols: onMessage(Message,Channel); validateInvalidateEvent
- Repository evidence: SpecEVENT002；CachePort swallowing afterCommitfailures已核对
- Dependencies and consumers: 原BootObjectMapper/RabbitConnectionFactory与具名Strategy/MapStruct/identity/Publisher；rawQueue→semanticQueue→AdRepository cacheSPI，search无consumer。
- Why now: 本位置承接Step7前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: onMessage(Message,Channel); validateInvalidateEvent；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 唯一消息消费效果是租户失效尝试；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-ARCH-001/MC-REUSE-001/MC-VALID-001/MC-LOG-001/MC-BEAN-001/MC-UTIL-001/MC-PATTERN-001/MC-SCOPE-001：保持所选层，实际注解/Bean/边界见pseudocode。
- Literal rule enforcement: Rule 1, Rule 2, Rule 4, Rule 5, Rule 6, Rule 9, Rule 10, Rule 11：业务@Slf4j、显式Bean名，@RequiredArgsConstructor和每final@Qualifier；JDK/现库，Jackson/Instant/Duration；selectedpattern不换反射分派。配置factory用现@Bean参数Qualifier，接口不伪构造器。
- Implementation pseudocode:

```java
@ConditionalOnProperty(name="app.cdc.enabled",havingValue="true") @Component("adCacheInvalidationConsumer") @Validated @Slf4j @RequiredArgsConstructor;
@RabbitListener(ad.invalidate queue,cdcRabbitListenerContainerFactory);
qualified originalObjectMapper/CdcSourceProperties/adRepository/egonColaValidationUtils;
preparse JSON tree: tenantId must be textual positive decimal Long, schemaVersion integral1, positions textual array1..2, source/event strings exact; then typed Jackson + recursive Validation;
strictschema1/unknownfield/headerbodymessageId andheaders.schemaVersion/tenant/sourceEpoch/tablead/route validation;
no cryptographicproof claim frompayloadagreement; trustedproducer publishACL mandatory;
finally scope: bindpositiveTenant ->adRepository.evictCandidates(old/newuniquePositions) outsidebusiness transaction;
validate syntax even revokedposition allowed; no ROW payload refilling/no HTTP ad_update;
SPIattemptreturned ->basicAck; swallowedRediswarn not retried byimaginedexception;
actualinvalidpermanentreject; actualthrow transientretry, alwaysrestoreMDC/trace;
no indexlistener implemented, no fake searchACK.
```

- Verification contribution: Step 7的 `CdcContractTest,AdCacheContractTest,AdSecurityContractTest,NativeReadwriteContractTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: onMessage(Message,Channel); validateInvalidateEvent的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 7 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/resources/application.yml`

- Purpose: 同键启用CDC配置面但默认关闭外部监听
- Symbols: app.cdc.*; spring.rabbitmq.publisher-confirm-type/publisher-returns
- Repository evidence: 当前integrationrabbitmqdisabled及Boot AMQP配置；Spec15
- Dependencies and consumers: 对应typedProperties→Commonphysicalpool/Native loader/Cachemanager及Ad/CDCconfiguration；同profile资源和NativeReadwriteContractTest消费键树；无新factory。
- Why now: 本位置承接Step7前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: app.cdc.*; spring.rabbitmq.publisher-confirm-type/publisher-returns；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 同键启用CDC配置面但默认关闭外部监听；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-SCOPE-001/MC-TEST-001；MC-CONFIG-001/MC-TIME-001：配置键/预算沿明确模板；不含secret/无自动运行。
- Literal rule enforcement: Rule 11：保留Light/本任务scope；Rule 7：此配置与同profile文件成套；Rule 10：预算/记录UTC单位明确。
- Implementation pseudocode:

```yaml
app.cdc.enabled=false; source-registry precisefixturealias epochmetadata placeholdersclosed;
max-payload-bytes1048576/confirm-timeoutPT5S;
spring.rabbitmq.publisher-confirm-type=correlated; publisher-returns=true;
leave actualhost/user/pass/TLS/vhost environmentinputs, no plaintextsecret;
when cdc.enabled=true originalRabbitconnectionFactory exists/typedregistryvalid andtopologyverified;
allprofilecorekeys same; do not activate originalsample integration merelytoactivateCDC;
no perfeaturefakeConnectionFactory/no new dependency.
```

- Verification contribution: Step 7的 `CdcContractTest,AdCacheContractTest,AdSecurityContractTest,NativeReadwriteContractTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: app.cdc.*; spring.rabbitmq.publisher-confirm-type/publisher-returns的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 8 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-dev.yml`

- Purpose: 同键启用CDC配置面但默认关闭外部监听
- Symbols: app.cdc.*; spring.rabbitmq.publisher-confirm-type/publisher-returns
- Repository evidence: 当前integrationrabbitmqdisabled及Boot AMQP配置；Spec15
- Dependencies and consumers: 对应typedProperties→Commonphysicalpool/Native loader/Cachemanager及Ad/CDCconfiguration；同profile资源和NativeReadwriteContractTest消费键树；无新factory。
- Why now: 本位置承接Step7前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: app.cdc.*; spring.rabbitmq.publisher-confirm-type/publisher-returns；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 同键启用CDC配置面但默认关闭外部监听；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-SCOPE-001/MC-TEST-001；MC-CONFIG-001/MC-TIME-001：配置键/预算沿明确模板；不含secret/无自动运行。
- Literal rule enforcement: Rule 11：保留Light/本任务scope；Rule 7：此配置与同profile文件成套；Rule 10：预算/记录UTC单位明确。
- Implementation pseudocode:

```yaml
app.cdc.enabled=false; source-registry precisefixturealias epochmetadata placeholdersclosed;
max-payload-bytes1048576/confirm-timeoutPT5S;
spring.rabbitmq.publisher-confirm-type=correlated; publisher-returns=true;
leave actualhost/user/pass/TLS/vhost environmentinputs, no plaintextsecret;
when cdc.enabled=true originalRabbitconnectionFactory exists/typedregistryvalid andtopologyverified;
allprofilecorekeys same; do not activate originalsample integration merelytoactivateCDC;
no perfeaturefakeConnectionFactory/no new dependency.
```

- Verification contribution: Step 7的 `CdcContractTest,AdCacheContractTest,AdSecurityContractTest,NativeReadwriteContractTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: app.cdc.*; spring.rabbitmq.publisher-confirm-type/publisher-returns的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 9 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-test.yml`

- Purpose: 同键启用CDC配置面但默认关闭外部监听
- Symbols: app.cdc.*; spring.rabbitmq.publisher-confirm-type/publisher-returns
- Repository evidence: 当前integrationrabbitmqdisabled及Boot AMQP配置；Spec15
- Dependencies and consumers: 对应typedProperties→Commonphysicalpool/Native loader/Cachemanager及Ad/CDCconfiguration；同profile资源和NativeReadwriteContractTest消费键树；无新factory。
- Why now: 本位置承接Step7前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: app.cdc.*; spring.rabbitmq.publisher-confirm-type/publisher-returns；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 同键启用CDC配置面但默认关闭外部监听；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-SCOPE-001/MC-TEST-001；MC-CONFIG-001/MC-TIME-001：配置键/预算沿明确模板；不含secret/无自动运行。
- Literal rule enforcement: Rule 11：保留Light/本任务scope；Rule 7：此配置与同profile文件成套；Rule 10：预算/记录UTC单位明确。
- Implementation pseudocode:

```yaml
app.cdc.enabled=false; source-registry precisefixturealias epochmetadata placeholdersclosed;
max-payload-bytes1048576/confirm-timeoutPT5S;
spring.rabbitmq.publisher-confirm-type=correlated; publisher-returns=true;
leave actualhost/user/pass/TLS/vhost environmentinputs, no plaintextsecret;
when cdc.enabled=true originalRabbitconnectionFactory exists/typedregistryvalid andtopologyverified;
allprofilecorekeys same; do not activate originalsample integration merelytoactivateCDC;
no perfeaturefakeConnectionFactory/no new dependency.
```

- Verification contribution: Step 7的 `CdcContractTest,AdCacheContractTest,AdSecurityContractTest,NativeReadwriteContractTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: app.cdc.*; spring.rabbitmq.publisher-confirm-type/publisher-returns的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 10 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-prod.yml`

- Purpose: 同键启用CDC配置面但默认关闭外部监听
- Symbols: app.cdc.*; spring.rabbitmq.publisher-confirm-type/publisher-returns
- Repository evidence: 当前integrationrabbitmqdisabled及Boot AMQP配置；Spec15
- Dependencies and consumers: 对应typedProperties→Commonphysicalpool/Native loader/Cachemanager及Ad/CDCconfiguration；同profile资源和NativeReadwriteContractTest消费键树；无新factory。
- Why now: 本位置承接Step7前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: app.cdc.*; spring.rabbitmq.publisher-confirm-type/publisher-returns；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 同键启用CDC配置面但默认关闭外部监听；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-SCOPE-001/MC-TEST-001；MC-CONFIG-001/MC-TIME-001：配置键/预算沿明确模板；不含secret/无自动运行。
- Literal rule enforcement: Rule 11：保留Light/本任务scope；Rule 7：此配置与同profile文件成套；Rule 10：预算/记录UTC单位明确。
- Implementation pseudocode:

```yaml
app.cdc.enabled=false; source-registry precisefixturealias epochmetadata placeholdersclosed;
max-payload-bytes1048576/confirm-timeoutPT5S;
spring.rabbitmq.publisher-confirm-type=correlated; publisher-returns=true;
leave actualhost/user/pass/TLS/vhost environmentinputs, no plaintextsecret;
when cdc.enabled=true originalRabbitconnectionFactory exists/typedregistryvalid andtopologyverified;
allprofilecorekeys same; do not activate originalsample integration merelytoactivateCDC;
no perfeaturefakeConnectionFactory/no new dependency.
```

- Verification contribution: Step 7的 `CdcContractTest,AdCacheContractTest,AdSecurityContractTest,NativeReadwriteContractTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: app.cdc.*; spring.rabbitmq.publisher-confirm-type/publisher-returns的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

- Validation working directory: `/Users/mario/SelfProject/enjoyshop`
- Verification command: `mvn -pl enjoyshop-service/enjoyshop-service-goods -am -Dtest=CdcContractTest,AdCacheContractTest,AdSecurityContractTest,NativeReadwriteContractTest -Dsurefire.failIfNoSpecifiedTests=false test`（先RED记录原因，最低实现后同命令GREEN）；
- Expected result: RED仅因上述缺失行为；GREEN exit0，named tests全部执行且无误跳过；离线检查对应断言通过。TEST019–022运行项仍明确Not run。
- Failure returns to: 本Step声明的测试/最低production/wiring file；若历史SQL/架构/工具范围冲突则回相应前置Step及§11，不绕过修复。
- Completion criteria: 本Step所有file/规则/MC证据闭合，测试报告匹配selectors、diff只本scope；不包含临时无用catalog类、秘密或组件源码。
- Rollback: 未运行SQL时仅撤销本Step自己写的path改动，保留他人内容；已部署SQL按§9追加forward-fix，不改旧脚本/history；下游持久消息/offset不丢。
- Commit paths: `enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/sync/CdcContractTest.java`；`enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/config/NativeReadwriteContractTest.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/config/CdcRabbitConfiguration.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/sync/CdcEventPublisher.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/sync/CdcRawConsumer.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/sync/AdCacheInvalidationConsumer.java`；`enjoyshop-service/enjoyshop-service-goods/src/main/resources/application.yml`；`enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-dev.yml`；`enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-test.yml`；`enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-prod.yml`

- Commit: `feat(goods): deliver cdc refresh events through rabbitmq`；HEAD `90ba87153d356d0a5723fc3c5366344511976f34`，10个精确Step 7 paths。63项CDC确认/重投/控制消息/租户失效/应用上下文/HTTP/缓存/架构回归全部通过，Plan strict与diff check通过；未连接Rabbit或执行真实CDC。每逻辑任务至多一个path-limitedcommit；暂态净删除file不为其制造空commit，记录新HEAD。

### Step 8 — 准备独立CDC运行配置和主从恢复验收合同

- Requirements: REQ-001, REQ-003, REQ-005, REQ-008, REQ-009, REQ-010, REQ-011
- Dependencies: Step 7 committed
- Baseline state: Step 7已GREEN/提交，HEAD `90ba87153d356d0a5723fc3c5366344511976f34`；前序已提供本file依赖。
- Observable outcome: 部署文件覆盖pgoutput/slot/offset/正式版本输入/Rabbit拓扑与K8s单owner；离线检查可区分配置通过与发布阻塞。
- End state: 部署文件覆盖pgoutput/slot/offset/正式版本输入/Rabbit拓扑与K8s单owner；离线检查可区分配置通过与发布阻塞。 后继内容只按后继Step发生；未部署本Step。
- Test-first gate: Required — 无deployment/cdc文件，source/sink可能自动创建不兼容queue，binding/slot/epoch门禁未落文档。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-JSON-001, MC-TIME-001, MC-CONFIG-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001；本Step文件角色对不适用注解写明理由，scope/test永远适用。
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；按每file角色落实，不让非Java运维文本伪承担Java类型规则。
- Ordered files:

#### File 1 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/sync/CdcContractTest.java`

- Purpose: 先锁独立Server配置/运行验收文档必需项
- Symbols: deploymentConfigPreservesBeforeAfter; inputGatesAreExplicit
- Repository evidence: SpecTEST019–022与原deployment/k8soffline经验
- Dependencies and consumers: 本file调用/观察Step 8列明的实际subject（CdcContractTest,NativeReadwriteContractTest）；复用JUnit/MockMvc/原testsupport，负面fixture不依赖外部服务。
- Why now: 先确立缺失行为的RED，后续最小实现须满足它。
- Contract/signature changes: deploymentConfigPreservesBeforeAfter; inputGatesAreExplicit；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 先锁独立Server配置/运行验收文档必需项；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-VALID-001/MC-JSON-001/MC-BEAN-001/MC-CONFIG-001/MC-SCOPE-001/MC-TEST-001：测试pseudocode所列正负fixture/模型与wiring，不启外部服务。
- Literal rule enforcement: Rule 1, Rule 2, Rule 6, Rule 10, Rule 11：同模块testrole，逐约束/组/JSON/时序断言；测试类不是业务Bean，不伪加POJO构造注解。
- Implementation pseudocode:

```java
readdeploymentfiles static; fail if noPGconnector/pgoutput/publicationdisabled/initialsnapshot;
assertbefore/after preserved(no unwrap), jsonschemafalse/tombstonesfalse/transactionmetadata;
assertinclude-listcoversad16/spuidentity/skuoldroot andskip.messages.without.change=false;
assertautoCreateRoutingKey=false/deliveryMode2/exchange/rawrouting/ackTimeout;
assertoffsetpersistentpath +singleowner/fencing/realversionmatrix andsearchqueueunconsumed warning;
fixtures prove emptyrealinputs blocksrelease but validstaticplaceholdertemplate passesconfigurationreview;
neverinstantiateDebeziumclient orstartprocess.
```

- Verification contribution: Step 8的 `CdcContractTest,NativeReadwriteContractTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: deploymentConfigPreservesBeforeAfter; inputGatesAreExplicit的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 2 — `CREATE deployment/cdc/debezium/application.properties`

- Purpose: 独立Server配置模板只连PRIMARY与Rabbit
- Symbols: debezium.source.*; debezium.sink.rabbitmq.*; format.value.*
- Repository evidence: Spec15.3/15.4引用官方Server3.2文档；实际正式版待releaseinputs
- Dependencies and consumers: CdcRabbitConfiguration/Server source/sink配置与DBA/MQ/K8s owner消费；acceptance/check scripts验证固定names和releaseinputs，不安装环境。
- Why now: 本位置承接Step8前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: debezium.source.*; debezium.sink.rabbitmq.*; format.value.*；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 独立Server配置模板只连PRIMARY与Rabbit；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-SCOPE-001/MC-TEST-001；MC-CONFIG-001/MC-TIME-001：配置键/预算沿明确模板；不含secret/无自动运行。
- Literal rule enforcement: Rule 11：保留Light/本任务scope；Rule 7：此配置与同profile文件成套；Rule 10：预算/记录UTC单位明确。
- Implementation pseudocode:

```properties
debezium.source.connector.class=io.debezium.connector.postgresql.PostgresConnector;
plugin.name=pgoutput; database.hostname/port/dbname/user/password = externalSecret/environment inputs;
table.include.list=public.ad,public.spu,public.sku; publication.autocreate.mode=disabled;
slot.name/ publication.name/ topic.prefix=registeredepoch sourcecontrolledinputs;
snapshot.mode=initial; tombstones.on.delete=false; provide.transaction.metadata=true;
heartbeat.interval.ms=0; skipped.operations=none; column.include.list explicit approvedcolumns;
skip.messages.without.change=false; time.precision.mode=isostring only supportedselectedformalversion;
offset.storage=org.apache.kafka.connect.storage.FileOffsetBackingStore;
offset.storage.file.filename persistentvolume path; no emptyDir/schemahistory guesses;
debezium.sink.type=rabbitmq; sink.rabbitmq exchange=enjoyshop.cdc.raw.v1/routingKey=master-data;
autoCreateRoutingKey=false/deliveryMode=2/ackTimeout=30000 plusversiondocumentedhost/vhost/TLSinputs;
format.key/value=json; schemas.enable=false; noSMTunwrap/dropbefore;
formalversion/digest not latest/SNAPSHOT; no actualhost/secret or automatic installationcommand.
```

- Verification contribution: Step 8的 `CdcContractTest,NativeReadwriteContractTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: debezium.source.*; debezium.sink.rabbitmq.*; format.value.*的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 3 — `CREATE deployment/cdc/postgres/replication-contract.md`

- Purpose: 物理复制/逻辑CDC/Native分工及恢复DBA合同
- Symbols: cluster->database->logicalGroup map; RPO; slots/RI/role/fencing
- Repository evidence: Spec11.4/15.2/16，当前3logicalpools
- Dependencies and consumers: CdcRabbitConfiguration/Server source/sink配置与DBA/MQ/K8s owner消费；acceptance/check scripts验证固定names和releaseinputs，不安装环境。
- Why now: 本位置承接Step8前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: cluster->database->logicalGroup map; RPO; slots/RI/role/fencing；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 物理复制/逻辑CDC/Native分工及恢复DBA合同；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-SCOPE-001/MC-TEST-001；MC-SCOPE-001：仅追加/精确文档归属，标运行未验证。
- Literal rule enforcement: Rule 11：保留Light/本任务scope；规则中Java注解不适用此非Java文件；不引入运行/Schema新语义。
- Implementation pseudocode:

```markdown
document percluster physicalprimary/standby, notoneclusterperJDBCpool;
inputs PGmajor/RPO/addresses/TLS/replicationrole/lag/PITR/permissions;
read-only checks currentrole +3tableRI=f/publicationonlyadspusku/slotstatus/restartLSN/confirmedLSN;
WALcapacity peakRate*stopWindow+margin andretentioncap failure response;
failover: fenceoldprimary ->provephysical+slot+offsetcontinuity ->switchsource/DSN;
PG17+slotfailover conditionalallofficialprerequisites; otherwisestop/newEpoch/fullrebuild;
no executedSQL/install/promote scripts/no automaticallydropslot/repairddl_history.
```

- Verification contribution: Step 8的 `CdcContractTest,NativeReadwriteContractTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: cluster->database->logicalGroup map; RPO; slots/RI/role/fencing的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 4 — `CREATE deployment/cdc/rabbit/topology-contract.md`

- Purpose: 提前建持久拓扑/ACL/AE/DLQ和容量门禁
- Symbols: raw/sync exchanges; raw/ad/search queues; AE/DLX policies
- Repository evidence: Spec15.4与CdcRabbitConfiguration selectednames
- Dependencies and consumers: CdcRabbitConfiguration/Server source/sink配置与DBA/MQ/K8s owner消费；acceptance/check scripts验证固定names和releaseinputs，不安装环境。
- Why now: 本位置承接Step8前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: raw/sync exchanges; raw/ad/search queues; AE/DLX policies；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 提前建持久拓扑/ACL/AE/DLQ和容量门禁；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-SCOPE-001/MC-TEST-001；MC-SCOPE-001：仅追加/精确文档归属，标运行未验证。
- Literal rule enforcement: Rule 11：保留Light/本任务scope；规则中Java注解不适用此非Java文件；不引入运行/Schema新语义。
- Implementation pseudocode:

```markdown
defineexactfixedexchanges/routes/queues matchingJava andServer;
rawSAC/prefetch32/quorum; ad/searchquorumdurable,persistentmessages;
operator AE policy raw->enjoyshop.cdc.unrouted.v1 exchange+quarantinequeue;
DLX enjoyshop.cdc.dead.v1 +perraw/ad/search deadqueue, sourcebody/identity retained;
versionverified quorum at-least-once-dead-lettering +overflow reject-publish; no drop-head/TTL silentfactsdrop;
producerACL Debezium onlyraw, bridge onlysync, consumer no arbitrarypublishrights;
bindingabsence AE/confirm distinction, retryexhaustion manualreject, partialconfirmedreplay;
searchqueue noconsumerthisphase;capacity/age/retention explicitbeforeCDCproductrouteenable;
no implicationbrokerconfirmmeansprocessed/allPodcachedeleted.
```

- Verification contribution: Step 8的 `CdcContractTest,NativeReadwriteContractTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: raw/sync exchanges; raw/ad/search queues; AE/DLX policies的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 5 — `CREATE deployment/cdc/acceptance.md`

- Purpose: 将未运行的真实验收设计为用户启动后可记录证据的门禁
- Symbols: TEST019–022 records; release matrix; rollback/failover proof
- Repository evidence: Spec14/16及deployment/k8s/acceptance.md既有证据分级
- Dependencies and consumers: CdcRabbitConfiguration/Server source/sink配置与DBA/MQ/K8s owner消费；acceptance/check scripts验证固定names和releaseinputs，不安装环境。
- Why now: 本位置承接Step8前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: TEST019–022 records; release matrix; rollback/failover proof；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 将未运行的真实验收设计为用户启动后可记录证据的门禁；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-SCOPE-001/MC-TEST-001；MC-SCOPE-001：仅追加/精确文档归属，标运行未验证。
- Literal rule enforcement: Rule 11：保留Light/本任务scope；规则中Java注解不适用此非Java文件；不引入运行/Schema新语义。
- Implementation pseudocode:

```markdown
list mandatoryformalPG/Debezium/Rabbit/SSversions+imageDigests+roles/hosts/TLS/namespace/volume/sizes;
separate offlineConfigurationPASS and realReleaseBLOCKED untilownerprovidedinputs;
manual routedread evidence primary/recoveryflag, replicalag/cachedmissPRIMARY, nativeprofile/historyreadonlyproof;
controlledDML c/u/d/r+rollback/nochangeincludedcolumns/oldnewtenant/deleteTOAST/restartreplay;
Rabbitreturns/AEunrouted/quorumDLQ/consumerstop/searchnolistener andoffset/WALbacklog;
Redis2Pods/warnswallowed/inflightfillTTL window, notfakeglobaldeleteassertion;
fencedpromotion+continuousslot vsnewEpochrebuild, sourceRPOcannotMQrepair;
fill observedtimestamp/command/identity/sourceLSN/result; defaultstatus Not run, no fabricatedPASS;
no commands automaticallystart/installservice/migrate/promote orbrowser.
```

- Verification contribution: Step 8的 `CdcContractTest,NativeReadwriteContractTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: TEST019–022 records; release matrix; rollback/failover proof的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 6 — `CREATE deployment/cdc/README.md`

- Purpose: 解释责任、配置准备、secret及K8s单owner边界
- Symbols: README inputs/profiles/productiongates/operatingowners
- Repository evidence: Spec15.7/16及现部署准备方案
- Dependencies and consumers: CdcRabbitConfiguration/Server source/sink配置与DBA/MQ/K8s owner消费；acceptance/check scripts验证固定names和releaseinputs，不安装环境。
- Why now: 本位置承接Step8前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: README inputs/profiles/productiongates/operatingowners；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 解释责任、配置准备、secret及K8s单owner边界；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-SCOPE-001/MC-TEST-001；MC-SCOPE-001：仅追加/精确文档归属，标运行未验证。
- Literal rule enforcement: Rule 11：保留Light/本任务scope；规则中Java注解不适用此非Java文件；不引入运行/Schema新语义。
- Implementation pseudocode:

```markdown
linkdebeziumprops/postgres/rabbit/acceptance andeffectiveSpec;
showPGphysical->Native->pgoutputServer->raw->semantic->Adcache/searchprotocol responsibilities;
externalServer1active/Recreate/PVCsinglewriter/resourcebudgets/terminationcheckpoint;
Secretreferences andNetworkPolicy goods->PGRedisRabbit/Server->PRIMARYRabbit;
versionspecificsupportedhealthendpoints only, readinessnotfencing;
noKafka/server/ES/Outbox, noEgoncomponentchanges, noactualK8s manifests/envcredentials;
preciseYuheng public GET AuthenticationMode.NONE registration requiresapprovedsiteowner;
existingdeployment/k8s5files andGateway/bff/uploadlimits unchanged; phase defaultsdisabled.
```

- Verification contribution: Step 8的 `CdcContractTest,NativeReadwriteContractTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: README inputs/profiles/productiongates/operatingowners的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 7 — `MODIFY scripts/work/2026-10-10-16-13-postgresql-cdc-advertisement-implementation-verify.sh`

- Purpose: 完成部署静态门禁及缺环境拒发布自测
- Symbols: --deployment; --self-test valid/invalid source/sink/releasefixtures
- Repository evidence: Step1scratch验证入口
- Dependencies and consumers: 本Step伪代码列明的原组件/生成输入和具名Bean，§5对应后继owner消费；commit scope仅本file和明确依赖。
- Why now: 本位置承接Step8前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: --deployment; --self-test valid/invalid source/sink/releasefixtures；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 完成部署静态门禁及缺环境拒发布自测；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-SCOPE-001/MC-TEST-001；MC-CONFIG-001/MC-TIME-001：配置键/预算沿明确模板；不含secret/无自动运行。
- Literal rule enforcement: Rule 11：保留Light/本任务scope；Rule 7：此配置与同profile文件成套；Rule 10：预算/记录UTC单位明确。
- Implementation pseudocode:

```bash
addproperties parsing Pythonstdlib, assert allfixedconnector/sink/JSON/offset keys withdocumentedformalversionfamily;
verifydocs exactqueues/ACL/capacity/epoch/fence/manualnotrun labels;
--self-test mutate bindings/autoCreateRoutingKey/source/table/skipchange/offsetpath/latestdigest ->nonzero;
--deployment offline checks completepreparationonly; require allrealreleaseinputs toclaimruntimegate, neverrunservices;
updateREADME futurecommands butkeeppathscratchuntilStep9moves it.
```

- Verification contribution: Step 8的 `CdcContractTest,NativeReadwriteContractTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: --deployment; --self-test valid/invalid source/sink/releasefixtures的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

- Validation working directory: `/Users/mario/SelfProject/enjoyshop`
- Verification command: `mvn -pl enjoyshop-service/enjoyshop-service-goods -am -Dtest=CdcContractTest,NativeReadwriteContractTest -Dsurefire.failIfNoSpecifiedTests=false test`（先RED记录原因，最低实现后同命令GREEN）；另执行 `bash scripts/work/2026-10-10-16-13-postgresql-cdc-advertisement-implementation-verify.sh --self-test` 与对应 `--deployment`。
- Expected result: RED仅因上述缺失行为；GREEN exit0，named tests全部执行且无误跳过；离线检查对应断言通过。TEST019–022运行项仍明确Not run。
- Failure returns to: 本Step声明的测试/最低production/wiring file；若历史SQL/架构/工具范围冲突则回相应前置Step及§11，不绕过修复。
- Completion criteria: 本Step所有file/规则/MC证据闭合，测试报告匹配selectors、diff只本scope；不包含临时无用catalog类、秘密或组件源码。
- Rollback: 未运行SQL时仅撤销本Step自己写的path改动，保留他人内容；已部署SQL按§9追加forward-fix，不改旧脚本/history；下游持久消息/offset不丢。
- Commit paths: `enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/sync/CdcContractTest.java`；`deployment/cdc/debezium/application.properties`；`deployment/cdc/postgres/replication-contract.md`；`deployment/cdc/rabbit/topology-contract.md`；`deployment/cdc/acceptance.md`；`deployment/cdc/README.md`；`scripts/work/2026-10-10-16-13-postgresql-cdc-advertisement-implementation-verify.sh`

- Commit: `docs(cdc): prepare postgres rabbitmq deployment contracts`；HEAD `8c8678bf690384b977c29abf89a1571bd374eb50`，7个精确Step 8 paths。21项CDC/Native契约测试、`--self-test`与`--deployment`静态门禁全部通过，Plan strict与diff check通过；部署门禁仍因正式版本digest、环境输入和TEST-019–022未提供而阻塞，未启动服务/执行DDL。每逻辑任务至多一个path-limitedcommit；暂态净删除file不为其制造空commit，记录新HEAD。

### Step 9 — 完成范围审查、回归和验证脚本归属收尾

- Requirements: REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010, REQ-011, REQ-012；Dependency preservation guards（源文档qualified，非primary扩范围）：REQ-013, REQ-014, REQ-015, REQ-016, REQ-017, REQ-018, REQ-019, REQ-020, REQ-021, REQ-022, REQ-101, REQ-102, REQ-103, REQ-104, REQ-105, REQ-106, REQ-107, REQ-108
- Dependencies: Step 8 committed
- Baseline state: Step 8已GREEN/提交，HEAD `8c8678bf690384b977c29abf89a1571bd374eb50`；前序已提供本file依赖。
- Observable outcome: 源代码/离线配置/协议回归和原合同保护证据完成；scratch清零；运行门禁单独未运行标注，未启动服务。
- End state: 源代码/离线配置/协议回归和原合同保护证据完成；scratch清零；运行门禁单独未运行标注，未启动服务。 后继内容只按后继Step发生；未部署本Step。
- Test-first gate: Required — 无finalallgate，scratch尚在work，样例/组件/immutableSQL/tempgenerated类可能遗漏。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-JSON-001, MC-TIME-001, MC-CONFIG-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001；本Step文件角色对不适用注解写明理由，scope/test永远适用。
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11；按每file角色落实，不让非Java运维文本伪承担Java类型规则。
- Ordered files:

#### File 1 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/config/NativeReadwriteContractTest.java`

- Purpose: 最终架构/工具/注入/生成定制/配置测试
- Symbols: allApprovedFilesPresent; qualifiedConstructorBeans; noForbiddenImports; noTemporaryTypes
- Repository evidence: 当前ProductContractConformanceTest及lombok.copyableAnnotations
- Dependencies and consumers: 本file调用/观察Step 9列明的实际subject（AdControllerTest,AdOpenApiContractTest,AdSecurityContractTest,AdManageTest,AdPersistenceTest,AdCacheContractTest,CdcContractTest,NativeReadwriteContractTest,ProfileKeyParityTest,LightPersistenceArchitectureTest,ProductContractConformanceTest）；复用JUnit/MockMvc/原testsupport，负面fixture不依赖外部服务。
- Why now: 先确立缺失行为的RED，后续最小实现须满足它。
- Contract/signature changes: allApprovedFilesPresent; qualifiedConstructorBeans; noForbiddenImports; noTemporaryTypes；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 最终架构/工具/注入/生成定制/配置测试；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-VALID-001/MC-JSON-001/MC-BEAN-001/MC-CONFIG-001/MC-SCOPE-001/MC-TEST-001：测试pseudocode所列正负fixture/模型与wiring，不启外部服务。
- Literal rule enforcement: Rule 1, Rule 2, Rule 6, Rule 10, Rule 11：同模块testrole，逐约束/组/JSON/时序断言；测试类不是业务Bean，不伪加POJO构造注解。
- Implementation pseudocode:

```java
assertallfinal§5files andexactclassroles, original13->14PO/DAO/repo and9->10DomainImpl;
reflection finaldeps/Qualifier->Lombokconstructorparameter propagation, namedbusinessBeans;
DomainJakartaallowed; noSpringSecurity/MP/Rabbit/DAOimplementation references;
Converters BaseConverter/BaseForwardConverter, MapStructgeneratedImpl beans; noDate/Fastjson/BeanUtils/utilitynewdeps;
allprofileAd/CDC/nativecorekeys equal; sourcefields/defaults strictwire;
asserttemporarycommands/converters/controllerpackage removed, scratchnoneafterdisposition;
keepexistinguser/teaching/sampleMQ andimmutableSQL snapshots unchanged.
```

- Verification contribution: Step 9的 `AdControllerTest,AdOpenApiContractTest,AdSecurityContractTest,AdManageTest,AdPersistenceTest,AdCacheContractTest,CdcContractTest,NativeReadwriteContractTest,ProfileKeyParityTest,LightPersistenceArchitectureTest,ProductContractConformanceTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: allApprovedFilesPresent; qualifiedConstructorBeans; noForbiddenImports; noTemporaryTypes的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 2 — `RENAME deployment/cdc/check.sh`

- Purpose: 将守护本子系统的临时校验入口归位
- Symbols: offline --self-test/--topology/--schema/--standards/--deployment/--final
- Repository evidence: scripts/work/2026-10-10-16-13-postgresql-cdc-advertisement-implementation-verify.sh全Step累计，技能rule22归属要求
- Dependencies and consumers: CdcRabbitConfiguration/Server source/sink配置与DBA/MQ/K8s owner消费；acceptance/check scripts验证固定names和releaseinputs，不安装环境。
- Why now: 本位置承接Step9前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: offline --self-test/--topology/--schema/--standards/--deployment/--final；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 将守护本子系统的临时校验入口归位；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-SCOPE-001/MC-TEST-001；MC-CONFIG-001/MC-TIME-001：配置键/预算沿明确模板；不含secret/无自动运行。
- Literal rule enforcement: Rule 11：保留Light/本任务scope；Rule 7：此配置与同profile文件成套；Rule 10：预算/记录UTC单位明确。
- Implementation pseudocode:

```bash
move exactscratch file todeployment/cdc/check.sh; update relative ROOT resolution fromnewdirectory;
no duplicatedcopy leftscripts/work; stillstdliboffline/no start/no networkSQL;
runallselftests +offlinegates +targetedMaven as declaredseparatecommand;
--final checks touchedpathscope against§5, historicalSQLprefix, no new POMdependencies/componentwrites;
nonzero onmissinggate/runtimeNotRun notmisreportedPassed; chmod executable.
```

- Verification contribution: Step 9的 `AdControllerTest,AdOpenApiContractTest,AdSecurityContractTest,AdManageTest,AdPersistenceTest,AdCacheContractTest,CdcContractTest,NativeReadwriteContractTest,ProfileKeyParityTest,LightPersistenceArchitectureTest,ProductContractConformanceTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: offline --self-test/--topology/--schema/--standards/--deployment/--final的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 3 — `RENAME enjoyshop-service/enjoyshop-service-goods/codegen/prepare-ad.sh`

- Purpose: 将CREATE-only可复核提取器归到所属业务codegen目录
- Symbols: extract-create; verify-generator-scope
- Repository evidence: scripts/work/2026-10-10-16-13-postgresql-cdc-advertisement-implementation-prepare-ad.sh已实现
- Dependencies and consumers: 本Step伪代码列明的原组件/生成输入和具名Bean，§5对应后继owner消费；commit scope仅本file和明确依赖。
- Why now: 本位置承接Step9前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: extract-create; verify-generator-scope；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 将CREATE-only可复核提取器归到所属业务codegen目录；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-SCOPE-001/MC-TEST-001；MC-CONFIG-001/MC-TIME-001：配置键/预算沿明确模板；不含secret/无自动运行。
- Literal rule enforcement: Rule 11：保留Light/本任务scope；Rule 7：此配置与同profile文件成套；Rule 10：预算/记录UTC单位明确。
- Implementation pseudocode:

```bash
move scratchpreparer to module/codegen/prepare-ad.sh; preserve explicitEGON_CODEGEN_CLASSPATH/notdownload;
default --extract-create and --verify-generated-scope as separate invocations; aftercustomization neverautomaticallyregeneratefullcatalog;
explicitfuturestage apply requires reviewedADDactions andnewnonexistingtargets;
verify deterministicview+SQLorigin hashes; no retyping/classes or deletinggeneratorstate;
removeoldworkpath, updateallcurrentdocinvocations, no newgeneratorEgonfiles.
```

- Verification contribution: Step 9的 `AdControllerTest,AdOpenApiContractTest,AdSecurityContractTest,AdManageTest,AdPersistenceTest,AdCacheContractTest,CdcContractTest,NativeReadwriteContractTest,ProfileKeyParityTest,LightPersistenceArchitectureTest,ProductContractConformanceTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: extract-create; verify-generator-scope的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

#### File 4 — `MODIFY deployment/cdc/README.md`

- Purpose: 更新归位验证命令和最终证据边界
- Symbols: verificationcommands; phasecompletion
- Repository evidence: Step8README
- Dependencies and consumers: CdcRabbitConfiguration/Server source/sink配置与DBA/MQ/K8s owner消费；acceptance/check scripts验证固定names和releaseinputs，不安装环境。
- Why now: 本位置承接Step9前面定义的类型/资源/生成输入，供后续调用或最终GREEN；依赖未形成时保持明确部分编译状态。
- Contract/signature changes: verificationcommands; phasecompletion；before为当前evidence所述状态/新文件不存在，after仅下列pseudocode与primary对应合同。
- Input/output and state mapping: 更新归位验证命令和最终证据边界；具体字段/null/default/enum/UTC/副作用以本file pseudocode和primary§9–11一致，禁止从caller Header派生可信身份。
- Error and edge behavior: 沿主Spec对应错误/范围：不默认tenant/source，不吞依赖错误、不写无关数据；此文件失败退回本Step，未满足GREEN不提交。
- Standards impact: MC-SCOPE-001/MC-TEST-001；MC-SCOPE-001：仅追加/精确文档归属，标运行未验证。
- Literal rule enforcement: Rule 11：保留Light/本任务scope；规则中Java注解不适用此非Java文件；不引入运行/Schema新语义。
- Implementation pseudocode:

```markdown
document bash deployment/cdc/check.sh --self-test/--final fromrepositoryroot;
codegen scopecheck runs module/codegen/prepare-ad.sh --verify-generated-scope; run --extract-create separately only when intentionally refreshing its deterministic view;
recordoffline test counts/results onlyfromactualexecution; runtime TEST019–022 Not run untilownerexecutes;
finalcompletionmeansapprovedpreparation/code/protocol, not deployedCDC/indexeddata;
no actualsource/credentials orimplicitstart command.
```

- Verification contribution: Step 9的 `AdControllerTest,AdOpenApiContractTest,AdSecurityContractTest,AdManageTest,AdPersistenceTest,AdCacheContractTest,CdcContractTest,NativeReadwriteContractTest,ProfileKeyParityTest,LightPersistenceArchitectureTest,ProductContractConformanceTest` 和对应§8 gates观测此file明确分支；缺失/错误时不得用无关suite的PASS替代。
- After this file: verificationcommands; phasecompletion的定义/资源/改动已存在且可由后续file消费；本Step尚未完成的类型/注册可令编译或focused test暂时RED，只有下面整Step GREEN才提交。

- Validation working directory: `/Users/mario/SelfProject/enjoyshop`
- Verification command: `mvn -pl enjoyshop-service/enjoyshop-service-goods -am -Dtest=AdControllerTest,AdOpenApiContractTest,AdSecurityContractTest,AdManageTest,AdPersistenceTest,AdCacheContractTest,CdcContractTest,NativeReadwriteContractTest,ProfileKeyParityTest,LightPersistenceArchitectureTest,ProductContractConformanceTest -Dsurefire.failIfNoSpecifiedTests=false test`（先RED记录原因，最低实现后同命令GREEN）；另执行 `bash deployment/cdc/check.sh --self-test`、`bash deployment/cdc/check.sh --final`、`git diff --check`；affected regression/verify见§8。
- Expected result: RED仅因上述缺失行为；GREEN exit0，named tests全部执行且无误跳过；离线检查对应断言通过。TEST019–022运行项仍明确Not run。
- Failure returns to: 本Step声明的测试/最低production/wiring file；若历史SQL/架构/工具范围冲突则回相应前置Step及§11，不绕过修复。
- Completion criteria: 本Step所有file/规则/MC证据闭合，测试报告匹配selectors、diff只本scope；不包含临时无用catalog类、秘密或组件源码。
- Rollback: 未运行SQL时仅撤销本Step自己写的path改动，保留他人内容；已部署SQL按§9追加forward-fix，不改旧脚本/history；下游持久消息/offset不丢。
- Commit paths: `enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/config/NativeReadwriteContractTest.java`；`deployment/cdc/check.sh`；`enjoyshop-service/enjoyshop-service-goods/codegen/prepare-ad.sh`；`deployment/cdc/README.md`；`scripts/work/2026-10-10-16-13-postgresql-cdc-advertisement-implementation-verify.sh`（归位删除）；`scripts/work/2026-10-10-16-13-postgresql-cdc-advertisement-implementation-prepare-ad.sh`（归位删除）

- Commit: `test(goods): audit advertisement cdc conformance`；HEAD `4e968affd7e5d8f7543d752cd971da9b7f295ae7`，4个精确Step 9 paths及两次脚本归位。76项最终定向测试、NATIVE/SQL/Scope/standards静态最终审计、CDC verifier的self-test/deployment/topology/schema/standards/final、codegen只读scope检查全部通过；Plan strict与diff check通过。PostgreSQL、RabbitMQ、Redis、Debezium和Kubernetes实际运行验收仍Not run且阻止部署。每逻辑任务至多一个path-limitedcommit；暂态净删除file不为其制造空commit，记录新HEAD。

## 8. Test, Validation, and Quality Gates

### 8.1 Validation ladder and interpretation

本章所有Maven/generator/runtime命令是**后续执行指令**，没有在writing-plan任务中运行。

| Order | Working directory | Exact command/method | Scope | Expected result | Failure returns to | Runtime boundary |
| --- | --- | --- | --- | --- | --- | --- |
| 1 RED | enjoyshop根 | 每Step §7 Verification command（新test first） | 本Step named test | 明确approved缺失行为失败；不能是无环境/网络连接失败 | 本Step firsttest/compile prerequisite | isolated tests/static |
| 2 GREEN | enjoyshop根 | 同Step命令，test报告核对每类非0且无误skip | 最低实现+wiring | exit0，所有对应断言通过，无新增wrongscope | 本Step对应file | 不启动externalservice |
| 3 generated | enjoyshop根 | `bash /Users/mario/SelfProject/Egon-COLA/scripts/egon-codegen.sh plan --config ABS_CONFIG`，reviewv2后`apply --plan ACTUAL_ID --config ABS_CONFIG` | 精确新artifact；3次分段 | ADD列表仅选目标；同config/SQL/template/tool/state指纹；无CONFLICT/staleinput | Step2/3/4 config/inputscope | 只offline，缺CP返回7停止 |
| 4 subsystem | enjoyshop根 | `bash deployment/cdc/check.sh --self-test`和`--final`；Step9前用scratch路径 | 原SQL/hash/Native/profiles/wire/source/sink/scope | mutationbadcases均失败、validfixtures均成功；所有规则实际有断言 | 对应Step配置/模型/source | static不是live证明 |
| 5 affected regression | enjoyshop根 | `mvn -pl enjoyshop-service/enjoyshop-service-goods -am -Dtest=AdControllerTest,AdOpenApiContractTest,AdSecurityContractTest,AdManageTest,AdPersistenceTest,AdCacheContractTest,CdcContractTest,NativeReadwriteContractTest,ProfileKeyParityTest,LightPersistenceArchitectureTest,ArchetypeContractConvergenceTest,ProductContractConformanceTest,ProductControllerTest -Dsurefire.failIfNoSpecifiedTests=false test` | 新链+原架构/商品HTTP | exit0、旧合同不漂移；原Brand/File具体tests见下一gate正常发现 | owningfile/previousscope | namedunits/isolatedcontext |
| 6 module/bytecode | enjoyshop根 | `mvn -pl enjoyshop-service/enjoyshop-service-goods -am '-Dtest=*Test' -Dsurefire.failIfNoSpecifiedTests=false verify` | Common组件使用、bytecodecheck、原Brand/Product/File/all defaultunit contracts | exit0，报告实际counts；默认外部测试disabled/skip列清，不冒称运行PASS | owningStep；baselinefailure另记录 | 执行前核对默认profile无external autostart；发现异常先止损调整选择 |
| 7 scope/format | enjoyshop根 | `git diff --check`；`git diff --name-only`；`git diff --cached --name-only`；`git status --short` | 本Step/final pathscope | 无空白错误；exactallowedpaths；history/POM/Egon/oldk8s不变 | 本Step修改者 | 不运行服务 |
| 8 release | 用户准备好的PG/Rabbit/Redis/CDC/K8s环境 | 按deployment/cdc/acceptance.md逐TEST019–022采证 | Native实际主从/RI/WAL/slot/quorum/AE/confirm/Redis2Pods/failover | 各证据完整且满足标准才release | DBA/MQ/CDC/平台owner与对应codeStep | Not run；需用户自行启动与补inputs |

### 8.2 Test coverage and RED/GREEN ownership

| Spec tests | Owning Step(s) | Concrete proof | Limits |
| --- | --- | --- | --- |
| TEST001–004 | 2/3/4 | DDL16cols/inherited8/audit/version/tenant/CRUD/error/unknown | Fake/H2不证明PG锁/类型 |
| TEST005/006/007 | 3/4/7 | OAS6操作、7/8wrapper字段、groups/scalars/身份/Host/sourceScope | 不是浏览器/Yuheng真实链 |
| TEST008–010 | 3/4/5 | 一次Clock/候选窗口/201cap、outer无tx/missPRIMARY、失效吞异常/旧fill | 非multiPod/Redis强一致 |
| TEST011–017 | 6/7/8 | vendor/control、old/new/parent/tenant、EID、部分confirm/finiteACK、futurefence合同 | 无broker/engine本轮运行证明 |
| TEST018/023 | 1/2/5/7/9 | SQLtargetrules/NATIVEprofiles/作用域/bytecode/bean/工具hash | 不替代PG实际route |
| TEST019–022 | 8/9准备acceptance | 保存可执行manualcases/versionmatrix/evidence模板 | 用户后续提供环境并执行；默认Not run |

### 8.3 Every-layer Validation and standards proof

| Handoff | Input/group and normalization | Invocation | Negative proof | Owner |
| --- | --- | --- | --- | --- |
| HTTP→Controller | AdSaveCommand Create/Update；AdLifecycleCommand Delete；Page/PublicRead query；strict scalar/unknown/repeated | @Valid/@Validated及Jacksonfielddeserializer、binding | 400fieldErrors，不触Manage/DAO | Step4 AdControllerTest |
| Controller→Manage | 同typedgroups，trusted scope已经filter建立 | Spring @Validated Manage/impl；无法保证proxy处用qualifiedValidationUtils同metadata | 非HTTP直接invalid调用也失败；admin401/403/503 | Step3/4 AdManageTest |
| Manage→DomainServiceImpl | AdBO Create/Update/Delete与AdDomainQuery Detail/Page/PublicRead | BO Jakarta约束+@Valid接口、impl @Validated组；显式helper覆盖实际非代理入口 | invalidnestedfields不调用Repository；tenant不来自body | Step3/5 AdManageTest |
| Domain→Repository | 业务guard后typedquery/PO；查询有tenant/active、delete有expectedVersion | @Validated Repository methods、@Valid，Common CRUD validation groups | 错scope/形状/CAS0 fail，no unexpectedwrites | Step3 AdPersistenceTest |
| Repository→DAO | EgonModel CRUD Insert(Update/Delete/Query/Persisted)，只DB-shape | DAO @Valid/@Validated；Common after-fill Validator | inheritedID/audit/version字段/长度/enum/SQL约束，不搬domaininvariants | Step2/3/9 architecture/persistence |
| raw/semantic→Strategy/evict | strictvendor/source/epoch/schema/header/tenant；CdcRecordBO/Event recursiveconstraints | adapteroriginalJackson、qualifiedValidationUtils后调用@Validated Strategy/Repository | unknown/missingbefore/invalidtenant poison，scopefinally清，noACKbeforeconfirm | Step6/7 CdcContractTest |

最终静态门禁禁止新增Date/Calendar/SimpleDateFormat/Gson/Fastjson/BeanUtilscopy/依赖；参数日志不含body/token/secret。规范校验按新增/本任务改动符号，不能借gate去改无关legacy样例。时间、JSON及构造注入用编译/生成Impl和反射/fixture断言，不只搜索注解字符串。

## 9. Migration, Compatibility, Rollout, and Rollback

### 9.1 Schema and managed runner

一个新20261010_001+manifest最终SHA；3个PRIMARY主target role guard：master_data创建ad并3表RI FULL，shards no-op；每target脚本和history同tx提交。其他实例advisorylock等待budget，失败处续跑不是跨库rollback。锁/statement/topology-ready超时沿Common已有配置不重建runner；unknowncommit新连接检查history版本/校验和/目标指纹，确定没有提交才重试。不能自动repair/checksumreset/adopt unmanagedschema/drop。

新增ad不迁旧MySQL数据/不backfill原SPU/SKU；自然业务UK不新增，旧active/lifecycleUK字节保持。CREATE-only codegen view非managedmigration、不能把ALTER入parser或把生成源码当成SQL已执行记录。正式DBA确认RI=f/publication/最小权限/WAL预算后才启Server。

### 9.2 Compatibility and deployment order

所有Step完成及离线gates通过后：补正式releaseinputs→部署queue/AE/DLX/ACL与容量→受控应用/DDL+NATIVE验收→单Server initialsnapshot→CDC/sourceepoch追踪→广告当前态失效/PRIMARY refill验收。publicsites与精确GET的Yuheng NONE配置由平台owner管理；管理scope enjoyshop.ad+read/write权限登记不能靠登录授权代替。原商品/品牌/文件/CORS/BFF/上传限额/旧samplequeue不变。

Server正式版/digest/PGmajor/Rabbit特性/SS解析版本未填阻止上线，不阻止离线Plan实现。本期搜索queue没有consumer；容量/持久保留/告警必须在启productroute前批准，不用TTL/drophead丢事实。下一期索引全量重建+增量协调/旧membership清理/receipt/fence/tombstone另写Spec，不在本任务实现。

### 9.3 Recovery and rollback

native回退须保留原主pool/group/profile与已追加DDL，且明确会暂失去读写分流。旧SQL/history不可回写；RI FULL若后续要改DEFAULT另新增批准migration。关闭newpublic/CDC listeners或Server保留slot/offset/DLQ/source事实，不以删slot/skipoffset释放容量。

failover先fence旧primary防splitbrain；PG17+slot同步只有全部官方条件与offset/WAL连续性采证后保持epoch。连续性不能证明、旧PG、slot/WAL/offset丢失→停发布/新epoch/initial+fullreconcile；广告controlledcacheevict、未来search新generation全量对账。异步复制RPO丢掉的源写不是MQ自动恢复；sourceAPI结果unknown不自动重发create。

## 10. Requirement-to-Step Traceability Matrix

| Requirement | Effective Spec section | Steps | Files | Tests/gates | Completion evidence |
| --- | --- | --- | --- | --- | --- |
| REQ-001 | primary§4/19 | Step 1,Step 8,Step 9 | §5/7相应ownedpaths | NativeReadwriteContractTest, LightPersistenceArchitectureTest, CdcContractTest, AdControllerTest, AdOpenApiContractTest, AdSecurityContractTest, AdManageTest, AdPersistenceTest, AdCacheContractTest, ProductContractConformanceTest；§8 | PRIMARY→standby不走MQ；逻辑slot只接PRIMARY；ordinary read与事务读有不同路由 |
| REQ-002 | primary§4/19 | Step 1,Step 2,Step 9 | §5/7相应ownedpaths | NativeReadwriteContractTest, LightPersistenceArchitectureTest, AdPersistenceTest, AdControllerTest, AdOpenApiContractTest, AdSecurityContractTest, AdManageTest, AdCacheContractTest, CdcContractTest, ProductContractConformanceTest；§8 | Native+无STRATEGY tables；保留全部业务表与SQL定义的2n目标/LOCAL；不声称旧STRATEGY已正确实现2n；配置各环境同键、真实路由和profile回归 |
| REQ-003 | primary§4/19 | Step 6,Step 7,Step 8,Step 9 | §5/7相应ownedpaths | CdcContractTest, AdCacheContractTest, AdSecurityContractTest, NativeReadwriteContractTest, AdControllerTest, AdOpenApiContractTest, AdManageTest, AdPersistenceTest, LightPersistenceArchitectureTest, ProductContractConformanceTest；§8 | 无Kafka服务；publication只ad/spu/sku；事务rollback不产生业务刷新；confirmed transport后才checkpoint |
| REQ-004 | primary§4/19 | Step 2,Step 3,Step 4,Step 5,Step 9 | §5/7相应ownedpaths | AdPersistenceTest, NativeReadwriteContractTest, LightPersistenceArchitectureTest, AdManageTest, AdControllerTest, AdOpenApiContractTest, AdSecurityContractTest, AdCacheContractTest, CdcContractTest, ProductContractConformanceTest；§8 | 5个管理操作闭合，USER/RBAC、租户/version/软删除/字段约束完整 |
| REQ-005 | primary§4/19 | Step 4,Step 7,Step 8,Step 9 | §5/7相应ownedpaths | AdControllerTest, AdOpenApiContractTest, AdSecurityContractTest, AdManageTest, CdcContractTest, AdCacheContractTest, NativeReadwriteContractTest, AdPersistenceTest, LightPersistenceArchitectureTest, ProductContractConformanceTest；§8 | 公开GET只读；X-Tenant/Actor不能覆盖；未知Host404；默认tenant1不可用于此路径 |
| REQ-006 | primary§4/19 | Step 4,Step 5,Step 9 | §5/7相应ownedpaths | AdControllerTest, AdOpenApiContractTest, AdSecurityContractTest, AdManageTest, AdCacheContractTest, NativeReadwriteContractTest, AdPersistenceTest, CdcContractTest, LightPersistenceArchitectureTest, ProductContractConformanceTest；§8 | 缓存候选、逐请求一次UTC时钟筛start≤now<end；PRIMARY回源；不包住缓存命中的事务；明确TTL/失效局限 |
| REQ-007 | primary§4/19 | Step 2,Step 3,Step 5,Step 6,Step 7,Step 9 | §5/7相应ownedpaths | AdPersistenceTest, NativeReadwriteContractTest, LightPersistenceArchitectureTest, AdManageTest, AdCacheContractTest, AdControllerTest, CdcContractTest, AdSecurityContractTest, AdOpenApiContractTest, ProductContractConformanceTest；§8 | update移动同时失效old/new，delete取before；重复/乱序只evict，跨租户before/after分组不串数据 |
| REQ-008 | primary§4/19 | Step 6,Step 7,Step 8,Step 9 | §5/7相应ownedpaths | CdcContractTest, AdCacheContractTest, AdSecurityContractTest, NativeReadwriteContractTest, AdControllerTest, AdOpenApiContractTest, AdManageTest, AdPersistenceTest, LightPersistenceArchitectureTest, ProductContractConformanceTest；§8 | 覆盖spu/sku c/u/d/r、两次父归属变化；不根据旧UP/DOWN直接执行；future消费者PRIMARY重读+fence/tombstone |
| REQ-009 | primary§4/19 | Step 6,Step 7,Step 8,Step 9 | §5/7相应ownedpaths | CdcContractTest, AdCacheContractTest, AdSecurityContractTest, NativeReadwriteContractTest, AdControllerTest, AdOpenApiContractTest, AdManageTest, AdPersistenceTest, LightPersistenceArchitectureTest, ProductContractConformanceTest；§8 | 身份不取处理时间或broker deliveryTag；部分confirm/ACK丢失可重复安全重试；毒消息隔离；不宣称exactly once |
| REQ-010 | primary§4/19 | Step 1,Step 7,Step 8,Step 9 | §5/7相应ownedpaths | NativeReadwriteContractTest, LightPersistenceArchitectureTest, CdcContractTest, AdCacheContractTest, AdSecurityContractTest, AdControllerTest, AdOpenApiContractTest, AdManageTest, AdPersistenceTest, ProductContractConformanceTest；§8 | 一slot一owner；WAL/offset/epoch/源角色/副本延迟监控；有promote/slot丢失/全量重建门禁；不RESET/repair历史 |
| REQ-011 | primary§4/19 | Step 1,Step 2,Step 3,Step 4,Step 5,Step 6,Step 7,Step 8,Step 9 | §5/7相应ownedpaths | NativeReadwriteContractTest, LightPersistenceArchitectureTest, AdPersistenceTest, AdManageTest, AdControllerTest, AdOpenApiContractTest, AdSecurityContractTest, AdCacheContractTest, CdcContractTest, ProductContractConformanceTest；§8 | 一个新SQL+manifest；旧SQL不变；无新应用依赖；原API/wire/S3/persistence框架保持 |
| REQ-012 | primary§4/19 | Step 9 | §5/7相应ownedpaths | AdControllerTest, AdOpenApiContractTest, AdSecurityContractTest, AdManageTest, AdPersistenceTest, AdCacheContractTest, CdcContractTest, NativeReadwriteContractTest, LightPersistenceArchitectureTest, ProductContractConformanceTest；§8 | Spec strict/关系/范围复核通过；当前writing-plan任务只改本文和授权Spec，未改生产/test/DDL文件或启动服务 |
| REQ-013 | predecessor qualified guard：2026-09-23-17-16-enjoyshop-scaffold-brand-crud.md，仅§2列effectivesections；不是primary新增需求 | Step 9（保护/原regression，不重做旧迭代） | 原Brand/File/Product/identity及Gateway规范保持；本任务§5禁止范围 | module *Test/architecture +gitdiffscope | 已有功能合同不漂移；非适用的旧构建工作excluded，禁止扩大scope |
| REQ-014 | predecessor qualified guard：2026-09-23-17-16-enjoyshop-scaffold-brand-crud.md，仅§2列effectivesections；不是primary新增需求 | Step 9（保护/原regression，不重做旧迭代） | 原Brand/File/Product/identity及Gateway规范保持；本任务§5禁止范围 | module *Test/architecture +gitdiffscope | 已有功能合同不漂移；非适用的旧构建工作excluded，禁止扩大scope |
| REQ-015 | predecessor qualified guard：2026-09-23-17-16-enjoyshop-scaffold-brand-crud.md，仅§2列effectivesections；不是primary新增需求 | Step 9（保护/原regression，不重做旧迭代） | 原Brand/File/Product/identity及Gateway规范保持；本任务§5禁止范围 | module *Test/architecture +gitdiffscope | 已有功能合同不漂移；非适用的旧构建工作excluded，禁止扩大scope |
| REQ-016 | predecessor qualified guard：2026-09-23-17-16-enjoyshop-scaffold-brand-crud.md，仅§2列effectivesections；不是primary新增需求 | Step 9（保护/原regression，不重做旧迭代） | 原Brand/File/Product/identity及Gateway规范保持；本任务§5禁止范围 | module *Test/architecture +gitdiffscope | 已有功能合同不漂移；非适用的旧构建工作excluded，禁止扩大scope |
| REQ-017 | predecessor qualified guard：2026-09-23-17-16-enjoyshop-scaffold-brand-crud.md，仅§2列effectivesections；不是primary新增需求 | Step 9（保护/原regression，不重做旧迭代） | 原Brand/File/Product/identity及Gateway规范保持；本任务§5禁止范围 | module *Test/architecture +gitdiffscope | 已有功能合同不漂移；非适用的旧构建工作excluded，禁止扩大scope |
| REQ-018 | predecessor qualified guard：2026-09-23-17-16-enjoyshop-scaffold-brand-crud.md，仅§2列effectivesections；不是primary新增需求 | Step 9（保护/原regression，不重做旧迭代） | 原Brand/File/Product/identity及Gateway规范保持；本任务§5禁止范围 | module *Test/architecture +gitdiffscope | 已有功能合同不漂移；非适用的旧构建工作excluded，禁止扩大scope |
| REQ-019 | predecessor qualified guard：2026-09-23-17-16-enjoyshop-scaffold-brand-crud.md，仅§2列effectivesections；不是primary新增需求 | Step 9（保护/原regression，不重做旧迭代） | 原Brand/File/Product/identity及Gateway规范保持；本任务§5禁止范围 | module *Test/architecture +gitdiffscope | 已有功能合同不漂移；非适用的旧构建工作excluded，禁止扩大scope |
| REQ-020 | predecessor qualified guard：2026-09-23-17-16-enjoyshop-scaffold-brand-crud.md，仅§2列effectivesections；不是primary新增需求 | Step 9（保护/原regression，不重做旧迭代） | 原Brand/File/Product/identity及Gateway规范保持；本任务§5禁止范围 | module *Test/architecture +gitdiffscope | 已有功能合同不漂移；非适用的旧构建工作excluded，禁止扩大scope |
| REQ-021 | predecessor qualified guard：2026-09-23-17-16-enjoyshop-scaffold-brand-crud.md，仅§2列effectivesections；不是primary新增需求 | Step 9（保护/原regression，不重做旧迭代） | 原Brand/File/Product/identity及Gateway规范保持；本任务§5禁止范围 | module *Test/architecture +gitdiffscope | 已有功能合同不漂移；非适用的旧构建工作excluded，禁止扩大scope |
| REQ-022 | predecessor qualified guard：2026-09-23-17-16-enjoyshop-scaffold-brand-crud.md，仅§2列effectivesections；不是primary新增需求 | Step 9（保护/原regression，不重做旧迭代） | 原Brand/File/Product/identity及Gateway规范保持；本任务§5禁止范围 | module *Test/architecture +gitdiffscope | 已有功能合同不漂移；非适用的旧构建工作excluded，禁止扩大scope |
| REQ-101 | predecessor qualified guard：2026-09-28-21-50-distributed-file-catalog-lookups.md，仅§2列effectivesections；不是primary新增需求 | Step 9（保护/原regression，不重做旧迭代） | 原Brand/File/Product/identity及Gateway规范保持；本任务§5禁止范围 | module *Test/architecture +gitdiffscope | 已有功能合同不漂移；非适用的旧构建工作excluded，禁止扩大scope |
| REQ-102 | predecessor qualified guard：2026-09-28-21-50-distributed-file-catalog-lookups.md，仅§2列effectivesections；不是primary新增需求 | Step 9（保护/原regression，不重做旧迭代） | 原Brand/File/Product/identity及Gateway规范保持；本任务§5禁止范围 | module *Test/architecture +gitdiffscope | 已有功能合同不漂移；非适用的旧构建工作excluded，禁止扩大scope |
| REQ-103 | predecessor qualified guard：2026-09-28-21-50-distributed-file-catalog-lookups.md，仅§2列effectivesections；不是primary新增需求 | Step 9（保护/原regression，不重做旧迭代） | 原Brand/File/Product/identity及Gateway规范保持；本任务§5禁止范围 | module *Test/architecture +gitdiffscope | 已有功能合同不漂移；非适用的旧构建工作excluded，禁止扩大scope |
| REQ-104 | predecessor qualified guard：2026-09-28-21-50-distributed-file-catalog-lookups.md，仅§2列effectivesections；不是primary新增需求 | Step 9（保护/原regression，不重做旧迭代） | 原Brand/File/Product/identity及Gateway规范保持；本任务§5禁止范围 | module *Test/architecture +gitdiffscope | 已有功能合同不漂移；非适用的旧构建工作excluded，禁止扩大scope |
| REQ-105 | predecessor qualified guard：2026-09-28-21-50-distributed-file-catalog-lookups.md，仅§2列effectivesections；不是primary新增需求 | Step 9（保护/原regression，不重做旧迭代） | 原Brand/File/Product/identity及Gateway规范保持；本任务§5禁止范围 | module *Test/architecture +gitdiffscope | 已有功能合同不漂移；非适用的旧构建工作excluded，禁止扩大scope |
| REQ-106 | predecessor qualified guard：2026-09-28-21-50-distributed-file-catalog-lookups.md，仅§2列effectivesections；不是primary新增需求 | Step 9（保护/原regression，不重做旧迭代） | 原Brand/File/Product/identity及Gateway规范保持；本任务§5禁止范围 | module *Test/architecture +gitdiffscope | 已有功能合同不漂移；非适用的旧构建工作excluded，禁止扩大scope |
| REQ-107 | predecessor qualified guard：2026-09-28-21-50-distributed-file-catalog-lookups.md，仅§2列effectivesections；不是primary新增需求 | Step 9（保护/原regression，不重做旧迭代） | 原Brand/File/Product/identity及Gateway规范保持；本任务§5禁止范围 | module *Test/architecture +gitdiffscope | 已有功能合同不漂移；非适用的旧构建工作excluded，禁止扩大scope |
| REQ-108 | predecessor qualified guard：2026-09-28-21-50-distributed-file-catalog-lookups.md，仅§2列effectivesections；不是primary新增需求 | Step 9（保护/原regression，不重做旧迭代） | 原Brand/File/Product/identity及Gateway规范保持；本任务§5禁止范围 | module *Test/architecture +gitdiffscope | 已有功能合同不漂移；非适用的旧构建工作excluded，禁止扩大scope |


## 11. Risks, Blockers, and User Decisions

| ID | Risk or decision | Impacted Steps/files | Evidence | Owner | Status/action |
| --- | --- | --- | --- | --- | --- |
| DEC-PLAN-001 | DomainJakarta约束与Spec原句冲突 | primary10.3.1、BO/query/Event/impl | 用户已明确“批准推荐修正”；原SpuBO/CompositionValidator同pattern | mario | Closed：正文修正，Domain不依赖Spring/MP/Security/Rabbit |
| DEC-PLAN-002 | native+Server+Rabbit/广告全部/搜索协议 | All | 本会话已有逐项确认和Spec全文确认 | mario | Closed，不重问技术选型 |
| DEC-PLAN-003 | 旧STANDARD_TENANT_ID（三PRIMARY/_t*）与SQL不一致；采用SQL两库×两表NATIVE、不复制旧组装 | Step1/2/9、primary§2/5/11 | 实际5.4.1 JAR/source及V20260913_001对照；用户明确批准 | mario | Closed：exact目标/已有算法明确；旧数据/历史指纹需要发布前只读证明，失败停发布、不repair |
| DEC-PLAN-004 | Step2验证命令要求scratch `--schema`，但原Plan只把脚本归入Step1/8 | Step2验证命令与§5脚本行 | 用户明确批准补列Step2路径，实现migration/manifest/CREATE-only视图只读校验 | mario | Closed：Step2新增脚本File17和精确commit path |
| TOOL-PLAN-001 | generatorruntimeclasspath组装/unknownfile冲突 | 2/3/4 | launcher explicitCP/exit7、v2 ownershipfingerprints；实际settings localRepository只读核对 | 执行者 | 实施前条件：用现有tool/依赖；缺失停止该action，不写替代/下载 |
| REL-PLAN-001 | 无实际PG/Rabbit/Debezium正式matrix/地址/TLS/PVC | 8/9 | primary15/16/18 | DBA/CDC/MQ/平台owner | 发布输入；offline可完成，部署BLOCKED直到实测，不虚称liveReady |
| REL-PLAN-002 | 初始Native SQL目标/history不兼容或副本连主/旧sharding块残留 | 1/2/9 | NativeProperty/validator和三组role | 执行者/DBA | 离线compare+PG路由证明；不默回主/repairhistory |
| REL-PLAN-003 | Cache吞失效失败/旧loader晚fill | 5/7 | 两级Cache/Port源码 | 应用/Redisowner | Accepted limitation：ACK=attempt，TTL自fill；真实2Pods另测 |
| REL-PLAN-004 | search无人消费、WAL/queue容量耗尽 | 7/8 | 此期无engine；slotretention/queuebacklog合同 | MQ/CDC/搜索owner | capacity门禁、无静默消息丢弃；下期fullrebuild/fence |
| REL-PLAN-005 | async复制RPO/旧主未fence/slotoffset不连续 | 8/9 | PG failover条件/primary16 | DBA | 连续性采证或新epoch重建；禁止自动跳位点 |

没有未决架构/公共合同/新增依赖批准项。工具/发布输入是已定实施门禁，不能据此自行绕过或宣称完成live任务。后续若发现重大源码能力差异，暂停对应action并给Spec/user具体证据，独立工作可继续。

## 12. Review and Acceptance

### 12.1 Original requirement fidelity

已覆盖PG物理复制/NATIVE/独立Debezium→Rabbit、不改组件、完整广告后端/匿名站点tenant/cache、搜索仅事件协议。没有增加页面/engine/Kafka/Outbox，也没有把当前任务变成部署/代码执行。

### 12.2 Spec consistency

primary唯一，accepted依赖按selectedsections纳入；9Step覆12primaryREQ与前置compatibilityguards。9项局部clarification只解析路径/工具输出/现API签名/组装，不改变6HTTP/3Event/16列/状态与一致性；Domain约束和SQL目标NATIVE纠正分别有单独用户授权。需效益的每新增元素已在§4.5审查，无fetch-forward、多余层/store/时间任务。

### 12.3 Repository executability

§5完整唯一path inventory，§7每file都有evidence/输入输出/错误/伪代码/中间态/规则。重复配置/test/生成config按Step所列symbols顺序所有，暂态catalog当期收敛。全新代码需approvedPlan执行，当前只有文档修改。GENERATOR拥有初始模板来源，后续不同wire/业务字段用explicitcustomization而非再apply覆盖；一步缺工具/输入停止，无implicitdownload。

### 12.4 Test and release completeness

每行为test-first；focused→module→bytecode/scope→manualrelease。RuntimeTEST019–022未运行，harness不模拟live成功。一个新DDL/manifest、历史/role/unknowncommit/rollback与K8s单owner/fence/epoch/capacity全部有归属。scratch两个脚本Step9分别归到deployment/cdc和module/codegen，scripts/work零遗留，未定工具处置无残项。

### 12.5 Blocking Manual Check

以下PASS表示**Plan审查闭合**，对应命令/代码/运行验收还没有执行。每row都有当前源码和具体未来证明；releaseinputs不是尚未选择架构。

| Check ID | Applicability | Status | Evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- |
| MC-ARCH-001 | Applicable | PASS | M/POM、现nativeLight目录/原verifier；§4.7/Step1/9 | 只选nativeLight；依赖保持Controller→Manage→Domain→Repo→DAO | None |
| MC-REUSE-001 | Applicable | PASS | §4.7 ledger；CommonMapper/CachePort/AMQP/identity/MapStruct/source | 已有能力优先，无平行router/cache/store | None |
| MC-DEP-001 | Applicable | PASS | GoodsPOM、E generator已存在、用户批准外部Server；§6 | 应用无新deps/POM编辑；正式环境输入不猜填 | None |
| MC-NAME-001 | Applicable | PASS | §5精确class角色和Step3新暂态收敛/Step6事件 | PO/BO/Command/Query/Result/Event及行为suffix，无Info/Data类型 | None |
| MC-VALID-001 | Applicable | PASS | 批准primary§10修正；§8.3六handoffs；Step3/4/6/7 | Jakarta/groups/proxy/nonHTTP入口全覆盖，不只Controller | None |
| MC-MODEL-001 | Applicable | PASS | Step2生成器/EgonModel、Step3/6classannotations、§7file | 普通class完整Lombok、PO继承、集合防御，不滥用record；ad无naturalUK | None |
| MC-CONVERT-001 | Applicable | PASS | 实际BaseConverter/Forward、MapStruct与stageconverter扩展；Step3/6 | 精确generics/mapping/Impl qualifier，不用BeanUtils/reflection复制 | None |
| MC-LOG-001 | Applicable | PASS | Step3/4/6/7businesspseudocode；primary15.6 | 业务Slf4j+safeIDs/duration，禁rawbody/token/secret | None |
| MC-BEAN-001 | Applicable | PASS | lombok.config复制Qualifier；Step3/5/6/7命名；Step9 reflection | 具名Bean/finalqualified@RequiredArgsConstructor，配置factory沿现Bean参数模式 | None |
| MC-UTIL-001 | Applicable | PASS | JDKMessageDigest/URI、已有Jackson/Jakarta；§8静态gate | 仅批准工具，无新utility依赖/Utils | None |
| MC-JSON-001 | Applicable | PASS | 6HTTP+3Event/Step3严格deserializer/Step4OAS/Step6wiretests | BootJackson、EnumValue/JsonValue、IDs/string/versionint/unknown/body一致 | None |
| MC-TIME-001 | Applicable | PASS | Step2TIMESTAMPTZ6、Step3/5Clock/Instant、Step6sourcecursor | 新time java.time，原deletedAt LocalDateTime UTC不变；snapshot不造committime | None |
| MC-CONFIG-001 | Applicable | PASS | Step1/3/4/5/7四profiles+import/Properties；NativeReadwriteContractTest | 同corekeytree，NATIVE旧块清理，source/public默认false、prod文档false | None |
| MC-PATTERN-001 | Applicable | PASS | primary13；Step3eligibility/Step6两Strategy注册，§4.5 | 真正表归属/窗口变化点，简单CRUD不新增ceremonialState | None |
| MC-SCOPE-001 | Applicable | PASS | §5完整树、Step9diffchecks、旧sample/Egon/k8s/history保持 | 只本任务code/config/docs，暂态和scratch明确当期处置 | None |
| MC-TEST-001 | Applicable | PASS | 9Step RED/GREEN、SpecTEST001–023与§8ladder | isolated/配置/契约/架构可证明；真实门禁明确未运行 | None |
| MC-BLOCKER-001 | Applicable | PASS | §11用户关闭Domain修正、SQL目标NATIVE纠正与技术范围、上述16row | 无设计/审批未决项；实施工具/发布条件明确禁止绕过 | None |

### 12.6 Final verdict

PASS — Ready for user review

Execution verdict: 9个Step已按批准范围实施并完成离线最终审计，本Plan Status=Completed。真实TEST-019–022均Not run，Debezium正式发行版/image digest、PG/Rabbit/K8s输入未提供，因此部署门禁仍Blocked；不得把离线PASS解释为生产就绪。

Execution status: 用户已批准并要求Plan全部做完。Step 1–9分别提交为`8c8f08cd98157b8281041245dae48243b8d0d9e3`、`5fd424c62f781985698b28d68b0cb4258ed3068c`、`fb296e0480c3712d05a2ec25e40772dedcbdfcd0`、`f3da9ee3696d956145a262f51f32a68651448bd5`、`a9d385704b9f093860a9b36abc8eab5ceb9597af`、`956b1e990c5e513bf18bfbe98e2382867881c67c`、`90ba87153d356d0a5723fc3c5366344511976f34`、`8c8678bf690384b977c29abf89a1571bd374eb50`、`4e968affd7e5d8f7543d752cd971da9b7f295ae7`。Step 4通过31项定向Maven测试，Step 5通过42项缓存/管理/HTTP/OpenAPI/安全/profile测试，Step 6通过52项CDC和架构回归，Step 7通过63项CDC/Rabbit/HTTP/架构回归，Step 8通过21项配置/Native测试及self-test/deployment静态门禁，Step 9通过76项最终回归及最终scope/标准/SQL审计。Plan strict与diff check通过；临时脚本已归位，Rabbit样例路由、Egon组件、既有SQL及manifest历史未改。真实PG/Rabbit/Redis/Debezium/K8s和搜索运行验收仍Not run；正式版本/digest和环境输入缺失，发布被明确阻止。本Plan未启动服务或执行数据库DDL。

## Java / CQE contract review

C=广告管理intent与主库事务/verified权限，Q=只读管理/public候选，E=PG committed事实经实际Rabbit producer/确认/消费。Domain可Jakarta但无技术实现引用；Common组件/MP受管DDL/PO继承/枚举/Jackson/time/构造注入/转换已逐file落实。无Outbox单主强塞到三主宿主，无SSM业务状态机，无fake MQ/target receipt完成声明。

## Backend generation and dependency evidence

实际CLI configVersion1/planformatVersion2、profilelight、tablead、三阶段artifact清单/sourceview/outputRoot/目标路径在Step2–4。当前工具的固定Create/Update/Delete/Detail及converter/Controller路径经源码核对，初始只能plan/apply，approvedbusiness customization后不重放覆盖。原SQL新版本仅一条；view与manifest提取hash门禁防漂移。journal append每次实际成功apply输入/throughVersion真实标记，无SQL已执行暗示。没有实际下载、添加或升级依赖；缺工具返回精确BLOCKED_TOOLING，不伪造generator可用或手写模板替代。
