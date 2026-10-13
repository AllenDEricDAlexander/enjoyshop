# Elasticsearch 选型冻结与部署后接入阶段计划

| Field | Value |
| --- | --- |
| Document | `2026-10-11-10-01-elasticsearch-deferred-integration-plan.md` |
| Template Version | `4` |
| Status | `Draft` |
| Created | `2026-10-11 10:01 Asia/Shanghai` |
| Updated | `2026-10-11 10:01 Asia/Shanghai` |
| Owner | mario |
| Repository | enjoyshop |
| Scope | 当前仅记录已批准的ES搜索方向、冻结接口/数据语义、保留部署后接入门禁；只改Plan与主Spec的Related Plans |
| Source Requirement | 用户2026-10-11：“方案审核通过，就用 Elasticsearch。先按这个方向定下来，具体对接等我部署完 ES 后再做。”并调用writing-plan |
| Baseline Revision | `main@4e968affd7e5d8f7543d752cd971da9b7f295ae7`；三份已有未跟踪Spec/Plan保留；无tracked/staged修改 |
| Implements Spec | [商品搜索方案](../spec/2026-10-11-09-34-product-search-engine-proposal.md) |
| Spec Status | `Draft` |
| Spec Revision | 原Updated为2026-10-11 09:47 Asia/Shanghai；本轮只加Related Plans backlink；原文件SHA-256 `27c292edd57ae409abd080d6962a6e379e20c88fd51cac6a4de4eadd8c5d4bc2` |
| Effective Specs | [1: 2026-10-11-09-34-product-search-engine-proposal.md](../spec/2026-10-11-09-34-product-search-engine-proposal.md)；[2: 2026-10-10-14-48-postgresql-cdc-advertisement-synchronization.md](../spec/2026-10-10-14-48-postgresql-cdc-advertisement-synchronization.md)；[3: 2026-10-09-10-55-product-management-lifecycle.md](../spec/2026-10-09-10-55-product-management-lifecycle.md)；[4: 2026-09-23-17-16-enjoyshop-scaffold-brand-crud.md](../spec/2026-09-23-17-16-enjoyshop-scaffold-brand-crud.md)；[5: 2026-09-28-21-50-distributed-file-catalog-lookups.md](../spec/2026-09-28-21-50-distributed-file-catalog-lookups.md)；[6: 2026-10-09-16-39-homepage-kubernetes-gateway-architecture.md](../spec/2026-10-09-16-39-homepage-kubernetes-gateway-architecture.md) |
| Depends On Plans | None |
| Supersedes | None |
| Superseded By | None |
| Related Plans | None |

## 1. Summary

**Elasticsearch已经由用户批准，选型结束。** 本Plan将“推荐ES”记录为“采用单Elasticsearch”；按主Spec保留PostgreSQL事实源、SKU搜索投影、既有CDC/Rabbit刷新链、Light职责和商品搜索合同。Doris留在历史比较材料，不再是本次实施待选项，不建设双引擎和切换工厂。

当前是**部署前冻结阶段**，只有1个文档Step、2个文件；不产生Java/测试类/ES mapping文件、POM依赖、配置键、DB迁移、消费者或假ES实现。ES部署后再闭合版本/分词/同步存储与fence/重建设计，并据其编写完整接入文件Steps；本稿不能被执行工具解释为马上开始搜索开发。

主Spec文件Header仍Draft，因为它保留了未闭合的同步物理设计；本Plan§2记录本次明确审批，**不再要求用户重复批准ES或既有搜索方向**。Draft只表示完整接入尚未可执行，不表示本次方向审批未通过。本轮不将精确client版本、插件、控制表和真实部署输入伪标已批准；暂不请求用户提供尚未部署的地址/密钥。

完成证据：Plan/Spec严格文档校验通过；主Spec除Related Plans外逐字不变；其他既有文档/SQL不变；tracked代码、POM、配置与Git暂存区无新增改动。运行时测试全部Not run。

## 2. Target Spec and Effective Design

### 2.1 Primary target

- Path: [商品搜索引擎方案](../spec/2026-10-11-09-34-product-search-engine-proposal.md)。
- Status: 文件Header为Draft；本轮用户明确批准该方案的ES方向和已描述合同，具体接入推迟。
- Revision: Updated=2026-10-11 09:47 Asia/Shanghai，baseline=4e968aff；原SHA-256见Header；只添加本Plan backlink。
- Approval evidence: 本轮原话“方案审核通过，就用 Elasticsearch。先按这个方向定下来，具体对接等我部署完 ES 后再做。”这同时关闭引擎选型并约束接入时机，不能只取前半句开始执行。

| 主Spec决策 | 本轮有效状态 | 执行影响 |
| --- | --- | --- |
| DEC-003 推荐ES、DEC-004选型/方案审核 | Approved by user：采用Elasticsearch与已有搜索方案 | 不重新评估Doris，不重复请求相同方向批准 |
| DEC-005 client版本与analyzer | Deferred until ES deployment | 引擎选择不等于未知版本/插件全部获准；不改POM、不下载 |
| DEC-006 同步durable receipt/membership/fence | Deferred design gate | 源协议已定，具体持久化/CAS/清理/ACK实现未定，不造表/consumer |
| DEC-007 环境/容量/安全信任链/上线质量 | Deferred until ES deployment | 不猜集群地址/TLS/副本/容量，不进行连接或上线操作 |

### 2.2 Effective Spec set

以下依赖只对其相应边界生效，不能把其历史“当时只写Spec/不写Plan”当作禁止本轮用户已要求的Plan，也不能把历史实现要求变成重做既有业务。

| Role | Spec/link | Status/revision | Effective sections | Why included |
| --- | --- | --- | --- | --- |
| Primary | [搜索](../spec/2026-10-11-09-34-product-search-engine-proposal.md) | Draft；09:47；本轮用户批准方向 | 全文；§5.4审批状态按§2.1本轮授权解释；§16部署后接入 | 12条搜索需求与候选目标清单 |
| Dependency / accepted amendment | [CDC](../spec/2026-10-10-14-48-postgresql-cdc-advertisement-synchronization.md) | Accepted；2026-10-10 16:33 | §9.2.9 EVENT-003、§11源表元数据、§16恢复/部署 | 当前态重读、membership/fence/receipt/ACK边界；不重新实现广告 |
| Dependency / current contract context | [商品生命周期](../spec/2026-10-09-10-55-product-management-lifecycle.md) | Header Review；2026-10-09 12:08；以当前源码再确认 | §9–11，按CDC amendment补搜索协议，不改商品HTTP/状态 | SKU稳定ID/价格分/可售/软删、LOCAL/master_data；不擅自变更该文档审批 |
| Transitive dependency | [品牌骨架](../spec/2026-09-23-17-16-enjoyshop-scaffold-brand-crud.md) | Accepted；2026-09-24 15:20 CST | §5.4、§6.1、§11与后续已接受amendments | 非open Light、MP与managed DDL；历史STRATEGY被CDC NATIVE范围修订，不能倒退 |
| Transitive dependency / amendment | [目录文件](../spec/2026-09-28-21-50-distributed-file-catalog-lookups.md) | Accepted；2026-09-29 11:11 CST | §9、§11、§16；category_brand writer按商品/现源码生效 | 既有目录、图片URL、上传及CORS不变 |
| Transitive dependency | [首页架构](../spec/2026-10-09-16-39-homepage-kubernetes-gateway-architecture.md) | Accepted；2026-10-09 21:35 | §7、§15、§16，公开广告边界按CDC amendment | Envoy/Yuheng入口、安全/限流/部署归属；不建设新网关 |

### 2.3 Superseded or excluded content

None for document replacements。用户本轮授权取代主Spec里的“ES尚未选定/待审核”状态；不改其其他技术合同。Doris不是未来本任务选项；PG LIKE不会作为ES故障静默兜底。主Spec§8生产文件、§11 mapping、§14实现测试、§16运行验收是**部署后保留工作**，不是本阶段CREATE/MODIFY范围。

## 3. Effective Requirements and Acceptance

编号按 `来源文档 + REQ-ID` 识别，多个Spec的REQ-001并不是同一要求；所有前驱要求只是保持现边界，不以bare ID相同合并业务。

| Requirement | Source Spec section | Effective statement | Observable acceptance | Implementation impact |
| --- | --- | --- | --- | --- |
| 搜索 REQ-001 | 主Spec§4/17；本轮批准 | 单Elasticsearch选型已定 | 本Plan不再列ES/Doris开放选择，不建双引擎 | 当前仅文档冻结 |
| 搜索 REQ-002 | 主Spec§7/9/11 | name中文/混合词AND，空词浏览 | 未来TEST-001/009/016；本轮只保留合同 | 部署后查询切片 |
| 搜索 REQ-003 | 主Spec§9/11 | category任意层ID、brandId精确filter | 未来TEST-003/008，不以名称替代ID | 部署后查询切片 |
| 搜索 REQ-004 | 主Spec§7/9/11/13 | 同规格OR、异规格AND、nested键值绑定、严格facets | 未来TEST-002/003，完整过滤集和截断标志 | 部署后查询切片 |
| 搜索 REQ-005 | 主Spec§9 | 分为整数、双含区间、单边合法 | 未来TEST-001，小数/负数/反向400 | 部署后输入校验 |
| 搜索 REQ-006 | 主Spec§7/9/10 | 1基/默认20/上限100/窗口10000、exact total | 未来TEST-005/009、空页不null/越窗400 | 部署后分页 |
| 搜索 REQ-007 | 主Spec§7/9 | 5种白名单sort+skuId tie | 未来TEST-004；saleNum累计/createTime创建，不冒充好评率/发布时间 | 部署后查询 |
| 搜索 REQ-008 | 主Spec§7/9/10 | 原name保留、安全mark高亮独立字段可null | 未来TEST-007/016，HTML不注入 | 部署后转换/查询 |
| 搜索 REQ-009 | 主Spec§7/9/11/15 | 可信站点tenant+visible、管理安全链保持 | 未来TEST-006/008/016；本轮不改变访问策略 | 部署后专用filter/查询 |
| 搜索 REQ-010 | 主Spec§7/16；CDC EVENT-003 | 当前态同步、源missing空集合、持久身份/归属/fence后ACK | DEC-006完整设计后TEST-010–013；未闭合不装consumer | 部署后同步接入 |
| 搜索 REQ-011 | 主Spec§11/16；CDC§16 | 新代全量+增量追平+孤儿校验+受控alias | 未来TEST-014/016，断连续性不切换 | 部署后重建 |
| 搜索 REQ-012 | 主Spec§4/16；本轮最新限制 | 现在定方向，ES部署后再对接 | 本轮只Plan+主Spec backlink，无任何代码/环境写 | 当前文档Step；实际对接待后继Plan |

前驱REQ-001–022和目录REQ-101–108的具体保护映射见§10；不宣称重新完成其运行验收。主Spec十二项功能也不因Plan通过而成为已实现。

## 4. Implementation Strategy and Dependency Order

### 4.1 Ordered strategy

当前1个逻辑任务：读现Spec/源码并冻结本轮批准→写本Plan→给主Spec添加Related Plans backlink→跑两个严格validator与改动范围校验→交付。实际代码阶段必须等用户明确ES已部署后再展开，既不启动运行时也不先建立一个没有依赖、接口实现或同步能力的假工程。

部署后工作保留以下**门禁顺序**，不是现在可执行的Implementation Steps：

| Gate | 输入/前置条件 | 要形成的具体产物 | 后续解锁条件 |
| --- | --- | --- | --- |
| GATE-ES-01 | 用户确认ES已部署，提供非secret版本/接入信息 | actual version/image、TLS/角色/容量/站点登记；按版本核验官方client/Jackson兼容与中文语料 | 精确依赖/分词方案获准；方向ES不再询问 |
| GATE-ES-02 | 固定部署输入及既有EVENT-003 | 后继补充Spec：同步控制表/索引/MP DDL、receipt/membership/token/CAS/tombstone GC、bulk部分/未知完成与ACK、snapshot/增量重建协调 | 所有同步设计Manual Checks闭合；不凭单机锁或源version断言安全 |
| GATE-ES-03 | 主Spec+后继Spec已形成完整effective设计 | 更新/另写真正代码Plan，精确展开主Spec§8查询files及后继sync files，测试先行、每层validation/conversion/配置/安全/OAS | Plan审核后才能实施；一项逻辑任务最多一个提交 |
| GATE-ES-04 | 用户批准执行且实现完成 | 受控真实ES/PG/Rabbit验收、全量新generation/增量追平/集合对账/失败恢复证据 | 上线批准后才alias/开关切换；测试不自动启动项目 |

### 4.2 Test-first strategy

当前仅Markdown审批/追溯metadata，N/A for production RED/GREEN；不为文档修改写镜像实现的测试或新scratch harness。现skill validators和逐字差异检查足够。

未来生产行为必须测试先行：主SpecTEST-001/005限定输入/窗口，TEST-002/003/004限定DSL/租户/规格/facet/sort，TEST-007安全转换，TEST-008安全路径，TEST-009真实MockMvc OAS；先让可编译测试因缺行为RED，再最小实现GREEN。TEST-010–014在后继sync精确符号/DDL/控制事务确定后分配，不先编造测试class/fixture。所有外部验收单独列环境，不把mock/H2当真实ES/PG证明。

### 4.3 Sequential and parallel boundaries

| Step | Depends on | May run in parallel with | Must not overlap with | Reason |
| --- | --- | --- | --- | --- |
| Step 1 | 本轮已批准ES方向 | None | 其他未跟踪文档、所有生产/测试/部署文件 | Plan与Spec backlink是一个追溯结果，顺序写入和验证；不用子agent |

未来Gate必须顺序经过版本→完整设计→代码Plan→实施→真实验收，不把独立阅读并行等同可并行修改。

### 4.4 Commit boundaries

当前不自动commit。若后续要求提交，本阶段两个文档属于一个逻辑任务，只允许一个路径限定提交：`docs(search): freeze Elasticsearch direction and deferred integration gates`。不stage已有CDC Spec/Plan或其他历史文件，不按Gate或每文件分别commit。未来代码Plan也必须遵守用户“每逻辑任务最多一次提交”，不能直接照技能默认每Step提交造成多提交。

### 4.5 Spec Simplicity and Implementation-necessity Audit

| Spec element | Spec necessity verdict/section | Current repository evidence | Direct/reuse alternative | Interaction/implementation cost | Plan decision |
| --- | --- | --- | --- | --- | --- |
| 单ES读引擎 | 主Spec§7.0/17 Add；本轮批准 | goods尚无client/engine实现 | 当前管理PG页不满足全文与高亮 | 新集群/客户端/同步state，全部待部署 | Freeze direction；不造双实现 |
| 单search API+facets | 主Spec§7.0/9 Add，直接显示商品 | ProductController管理API需认证，现无商城搜索 | 一次search返回页/候选，tenant后端derive | 1 HTTP+1 ES，无参数预取 | Preserve contract，Deferred implementation |
| Query/BO/DTO/3Converters、Light端口 | 主Spec§8/10真实语义边界 | G/pom MapStruct/Validation/common-core；领域纯端口 | 不泄露PO，不每层再新增一套对象 | 进程内职责，不加模块/微服务 | 后继代码Plan精确展开，不本期CREATE |
| 同步receipt/membership/fence | 主Spec§7.3/11/16 Add pending | 现ProductRefreshRequestedEvent/CdcRabbitConfiguration有生产链、无consumer | 仅锁/队列顺序不阻止已发出的迟到remote write | 未决物理state/CAS/DDL/恢复 | Return to Spec after deployment；不在Plan改设计 |
| 新缓存/tenant预取/双引擎Factory | 主Spec§7.0 Remove | 现公开站点可derive，暂无实测search缓存需求 | 直接search和单ES实现 | 额外call/state无当前收益 | Excluded |
| 当前Plan与主Spec backlink | 技能scope允许追溯metadata；主Spec§16后继接入 | 两文档已存在/可创建，当前授权明确 | 口头“定了”无法在仓库追溯暂停接入条件 | 只文档，无runtime/state | Implement Step 1 |

未发现需要本轮扩大业务范围的证据；既有主Spec中的精确client、同步state开放项必须继续保留，不能靠Plan“顺便设计”填平。

### 4.6 Change-unit Dependency Matrix

| Change unit | Requirements | Proof/RED point | Compile/runtime prerequisites | Produces | Consumers/unblocks | Owning Step |
| --- | --- | --- | --- | --- | --- | --- |
| 文档方向冻结+关系metadata | 搜索REQ-001/012与§10全部合同保护ID | doc validator+逐字范围检查，生产RED N/A | 已读主Spec、本轮明确批准、现Python/Git | ES固定方向、部署后门禁、双向链接 | 用户复核和部署后恢复任务；不解锁代码 | Step 1 |

### 4.7 Java, Spring, and Egon-COLA Implementation Standards

本阶段无Java文件变更，下表记录之后不得弱化的既有设计约束；不是声明代码已符合所有标准。

| Concern | Current repository evidence | Effective Spec decision | Planned implementation consequence | Owning Steps/checks |
| --- | --- | --- | --- | --- |
| Architecture profile | G/pom实际bytecode-architecture plugin、domain/application/infrastructure/adapter mappings；LightPersistenceArchitectureTest | 主Spec§6.1/8单非open Light | 现在不改目录；未来保持Controller→Manage→DomainService→Repository，无biz混层 | Step1边界记录；MC-ARCH-001 |
| Reuse/capability | 当前Validation/AMQP/MP/Common ID/common-core/MapStruct/Yuheng OpenAPI | 主Spec§6.1复用；ES client缺口待实际版本 | 不加依赖或重复broker/迁移框架 | Step1边界；MC-REUSE-001/DEP |
| Naming/model/validation/conversion | G/lombok.config；ProductPageQuery/Converters/ResultRecord | 主Spec§10普通class+Lombok/每层group/BaseForwardConverter | 未来代码Plan逐文件执行，不本期创建DTO/Converter | Step1保持；Java实现N/A |
| Bean/logging/util/JSON/time/config | 实际Qualifier传播、Slf4j Bean、四profile及Jackson | 主Spec§8/10/15严格Bean/工具/Instant/Duration/key parity | 现在不改profile；后续不能只配置一个环境 | Step1保持；Java实现N/A |
| Business variation/pattern | 商品现策略/生命周期；主SpecSpecification | 主Spec§13组合条件Specification、Repository适配 | 不新增多引擎工厂，参数Map不能直进DSL | Step1冻结方向；实现待后继 |

#### Capability reuse ledger

| Need | Candidates inspected | Exact evidence | Fit/gap | Decision | Added dependency/custom code | Owning Step/check |
| --- | --- | --- | --- | --- | --- | --- |
| 文档完整性 | 现skill Python validators、Git diff、Python hashlib | 两skill scripts/validate_*路径已存在 | 可验证本阶段artifact与变化范围 | Reuse | None，不建脚本 | Step1/MC-TEST-001 |
| 搜索client | 现Spring/Jackson，E/components与goods源码查询 | G/pom无ES；主Spec§6.1已发现正式client缺口 | 版本/传递兼容未定 | Deferred until deployed version | 本轮None；未来co.elastic.clients:elasticsearch-java具体版本待锁定 | GATE-ES-01/MC-DEP-001 |
| 消息协议/PG读取 | Spring AMQP、MP-SDJ、当前Repository | ProductRefreshRequestedEvent v1、CdcRabbitConfiguration队列和MANUAL工厂、SkuDAO.XML全根路径 | 生产端可复用；真实search consumer/控制存储未实现 | Keep current contracts | None | Step1保持；GATE-ES-02 |
| 公共对象/转换/安全 | common-core/MapStruct、ResultRecord、AdPublicTenantFilter、现安全链 | G/pom/lombok.config/当前源码 | 足够保持架构与候选搜索合同 | Reuse after Plan approved | None | Step1记录 |

### 4.8 User-mandated Java Rule Implementation Matrix

本阶段是文档Plan，不影响Java对象/运行时；保留全部十条原编号，未来实施仍按主Spec规范。这些N/A不能被未来代码Step继承为豁免。

| Literal rule | Spec source | Repository evidence | Exact files and order | Pseudocode obligations | Validation gate | Steps | Status/blocker |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Rule 1 | 主Spec§6.2/8/10 | 当前无Search Java，新carrier仍在候选tree | Plan→Spec backlink，不改Java | 后继语义suffix，现阶段无新类型 | 文档范围检查 | Step1 | N/A |
| Rule 2 | 主Spec§10.3.1 | 当前Validation/分组与manual metadata；本期无handoff变化 | 仅两个Markdown | 后继每层@Valid/@Validated/group，自调用同metadata；phone不涉及 | 无Java差异 | Step1 | N/A |
| Rule 3 | 主Spec§10.4 | 现MapStruct/common BaseForwardConverter/Lombok | 仅两个Markdown | 后继普通class全注解、3Converter，禁止手工/JSONcopy | 无Java差异 | Step1 | N/A |
| Rule 4 | 主Spec§8 | 现lombok.config复制Qualifier | 仅两个Markdown | 后继@Slf4j、命名Bean、Lombok final qualified injection | 无Bean修改 | Step1 | N/A |
| Rule 5 | 主Spec§6.2 | 当前JDK/Commons/Guava | 无依赖或Utils文件 | 后继闭合allowlist，无当前utility引入 | git diff无POM/Java变更 | Step1 | N/A |
| Rule 6 | 主Spec§9/10 | 当前Jackson/ResultRecord，事件v1 | 无新JSON/wire | 后继enum @JsonValue、7字段响应、无ordinal/Gson/Fastjson | 无wire变化 | Step1 | N/A |
| Rule 7 | 主Spec§15 | R/application.yml/dev/test/prod | 无YAML变化 | 后继新增key四profile同结构、default false | git diff无profile修改 | Step1 | N/A |
| Rule 9 | 主Spec§13 | 现事件策略；Specification候选 | 无业务logic changes | 后继使用已选组合规范，不造双引擎Factory | 无业务变化 | Step1 | N/A |
| Rule 10 | 主Spec§10/15 | 现Event Instant及预算Duration | 仅文档Asia/Shanghai时间 | 后继Java只java.time，UTC/epoch milli原样 | 日期header/文件名一致 | Step1 | N/A |
| Rule 11 | 主Spec§6.1/11 | Light bytecode mappings、MP/managed DDL、Rabbit v1 | Plan→Spec backlink | 不混层、不新模块/DDL/Event；将现架构设为后继不变量 | 无production差异+§2依赖保护 | Step1 | PASS |

## 5. Change File Tree

```text
docs/egon/
  plan/2026-10-11-10-01-elasticsearch-deferred-integration-plan.md                                      CREATE Step1 File1
  spec/2026-10-11-09-34-product-search-engine-proposal.md  MODIFY Step1 File2：仅Related Plans
```

| Operation | Path | Current evidence/symbol | Final symbols/state | Responsibility | Step | Requirements | Validation owner |
| --- | --- | --- | --- | --- | --- | --- | --- |
| CREATE | docs/egon/plan/2026-10-11-10-01-elasticsearch-deferred-integration-plan.md | 此路径本轮前不存在；主Spec§16与本轮writing-plan | 本Plan12章、ES已批准/实际接入待部署、Draft状态 | 记录本轮决定及阶段门禁，不设计新业务 | Step1 File1 | 主搜索REQ-001/012、§10冻结全部REQ | Plan validator/范围检查 |
| MODIFY | docs/egon/spec/2026-10-11-09-34-product-search-engine-proposal.md | 原Related Plans=None，Header Draft | Related Plans链接本Plan；其余逐字原样 | 建立规范→Plan追溯 | Step1 File2 | 主搜索REQ-001/012、前驱合同保护 | Spec validator/hash检查 |

主Spec§8未来Java/测试/mapping清单仍保留在主Spec；因实际版本及同步Spec未闭合，本Plan不把其列成当前可执行write范围。本轮不删教学/user样例，已有广告/商品样例生产化工作不重复清理。

## 6. Prerequisites, Constraints, and Plan Clarifications

### 6.1 Repository and worktree baseline

- 根目录 `/Users/mario/SelfProject/enjoyshop`，main/4e968aff；执行前核对git status与本轮相同或逐项解释变化。
- 用户提供的AGENTS有效：不自动启动项目/服务器/浏览器/GUI；每逻辑任务最多一个commit；不动无关文件/旧迁移。仓库及父路径未发现额外磁盘版AGENTS。
- 本轮前未跟踪：CDC Spec、CDC Plan、主搜索Spec。只主搜索Spec的Related Plans行获准修改，CDC两文件、其他Spec/Plan保持字节一致。
- 不stage/reset/clean任何用户工作；不读取kubeconfig/密钥；不写Egon-COLA外部工作区。

### 6.2 Build, test, and environment prerequisites

| Concern | Exact command/source | Required state | Validation boundary |
| --- | --- | --- | --- |
| 文档Python工具 | 两skill `scripts/validate_skill_resources.py`、`validate_spec.py`、`validate_plan.py` | Python3及resource preflight PASS | 仅文档结构/metadata/link/coverage，不证明ES |
| Git | `git status --short`、`git diff --name-only`、`git diff --cached --name-only` | 已有dirty边界已记录，当前tracked/staged空 | 仅write范围 |
| 将来Java | G/pom java.version=21；现test profile | 部署后检查JDK/Maven及resolved starter/client/Jackson | 本轮不解析/下载/编译 |
| 将来runtime | 用户部署ES后；主Spec§16输入 | 版本/TLS/访问角色/站点/PG源/Rabbit容量明确 | 当前Not run，不用默认localhost/latest替代 |

`G`为 `enjoyshop-service/enjoyshop-service-goods`；后续命令cwd显式选root/module，不依赖当前shell隐式目录。

### 6.3 Immutable constraints and approved decisions

ES方向与现主Spec描述的SKU搜索合同已批准，后续不重复询问。client精确版本、中文分词插件、同步控制存储/DDL/CAS及实际部署输入仍推迟，不据批准方向直接下载依赖或申请现在不存在的secret。

PG为唯一商品事实源；源码及SQL保持；旧Flyway/MP SQL与checksum不可改；EVENT-003 body/header/schema/sourceEpoch与现Rabbit拓扑原样；不安装fake consumer丢弃pending搜索消息。输入租户不得caller指定；管理bearer/权限不因未来匿名search扩大。主Spectitle/Header仍历史Draft，本Plan明确批准证据优先，不以旧“待审核”字样重新开选型。

### 6.4 Plan Clarifications

| ID | Small implementation inference | Repository evidence | Why semantics are unchanged | Impact if wrong |
| --- | --- | --- | --- | --- |
| PLAN-CLAR-001 | 当前任务的可执行write仅阶段Plan+Related Plans，而完整代码Steps后补 | 用户“先按方向定下来/部署后再对接”；主Spec§5.4/16仍开放物理细节 | 这是执行时机范围，保留全部业务要求与未来目标，不减少功能 | 若用户另行授权部署前独立编码，再据已定Spec追加完整代码Plan；当前不自行扩展 |

不把版本/表/字段/锁/配置值当Plan Clarification；那些是后继Spec的material decisions。

## 7. Ordered File-by-file Implementation Steps

### Step 1 — 冻结ES已批准方向并建立延后接入追溯

- Requirements: `REQ-001`, `REQ-002`, `REQ-003`, `REQ-004`, `REQ-005`, `REQ-006`, `REQ-007`, `REQ-008`, `REQ-009`, `REQ-010`, `REQ-011`, `REQ-012`, `REQ-013`, `REQ-014`, `REQ-015`, `REQ-016`, `REQ-017`, `REQ-018`, `REQ-019`, `REQ-020`, `REQ-021`, `REQ-022`, `REQ-101`, `REQ-102`, `REQ-103`, `REQ-104`, `REQ-105`, `REQ-106`, `REQ-107`, `REQ-108` — 主搜索REQ-001–012记录采用/保存未来功能；其他来源ID仅保护Unchanged合同，并不重新实施。
- Dependencies: 当前用户批准；主Spec原字节/commit/dirty范围已记录；完整Java接入依赖仍未满足，因此本Step不做coding。
- Baseline state: main@4e968aff；主Spec存在且Related Plans=None；没有本阶段Plan；当前无ES client/search consumer，已有PG/MQ业务保持。
- Observable outcome: 仓库明确“已选Elasticsearch、部署后具体对接”，主Spec可导航到有审批证据和后继门禁的Plan。
- End state: 本Plan Draft完整保存12章与已选方向，主Spec只新增backlink；其余代码/测试/POM/YAML/SQL/既有文档原样；绝不把索引搜索标已实现。
- Test-first gate: Not applicable — 本Step只Markdown/metadata，不修改Java行为，不写镜像文档测试；使用现严格validators与逐字范围检查。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-SCOPE-001, MC-TEST-001, MC-BLOCKER-001 — 前两项保护架构/工具复用；DEP/BLOCKER揭示后继门禁，不阻止本Step文档产物。
- Literal Rules: Rule 11 — 保持Light/MP/MQ既有边界；Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10本Step不适用Java变化，理由见§4.8。
- Ordered files:

#### File 1 — `CREATE docs/egon/plan/2026-10-11-10-01-elasticsearch-deferred-integration-plan.md`

- Purpose: 保存唯一ES方向、用户审批原话、部署后门禁与保留需求，阻止阶段误执行。
- Symbols: Header Implements Spec/Effective Specs/Spec Status；§2审批覆盖；GATE-ES-01–04；§3/10来源限定REQ；§11/12阻断状态。
- Repository evidence: 主Spec§5.4/8/14/16留version/sync/运行时边界；既有docs/egon/plan使用时间戳Markdown；本轮明确invoke writing-plan。
- Dependencies and consumers: 输入主Spec、已列effective文档及当前源码；消费者用户/后继writing-spec、writing-plan、executing-plan；无Javaimport/build consumer。
- Why now: 先有Plan完整内容，才能添加不会指向不存在文件的Spec backlink。
- Contract/signature changes: 没有API/Java/Event/schema改动；文件Status=Draft、Spec Status=Draft匹配历史header，审批证据明确方向已Accepted by user。
- Input/output and state mapping: 用户批准→ES固定选择；实际对接延期→门禁；主REQ→未来TEST/阶段状态；不把输入未知值转换为伪deployment配置。
- Error and edge behavior: 发现Spec major缺口保留blocked integration，不造client或控制表；重复调用查找并更新同Plan不制造第二选型任务；Spec旧待审核词由§2解释为历史，方向不再次问用户。
- Standards impact: MC-ARCH-001/MC-REUSE-001/MC-DEP-001/MC-SCOPE-001/MC-TEST-001/MC-BLOCKER-001 — 保留实际Light与现Python工具，精确列代码门禁；无新Java可执行行为。
- Literal rule enforcement: Rule 11 — 记当前Light依赖方向/MP-managed-DDL/Event-MQ且不改实现；其他Rule N不涉及当前Markdown代码变更，不写绕过标准的未来伪类。
- Implementation pseudocode:

```markdown
Header.Status = Draft; Header.Implements Spec = ../spec/2026-10-11-09-34-product-search-engine-proposal.md
§2.Approval = 用户“方案审核通过，就用 Elasticsearch；具体对接等我部署完 ES 后再做”
§2.Decisions = ES选型关闭；DEC-005/006/007保留部署后门禁；没有Doris实施分支
§3/§10 = source-qualified requirements -> 保留现合同或未来验收；不标implemented
§7.writeScope = 本Plan CREATE + 主Spec Related Plans MODIFY，零backend/deployment文件
§12.verdict = BLOCKED — Spec or user decision required，仅指完整接入计划不能执行
```

- Verification contribution: Plan strict validator覆盖Header/links/Rules/REQ/Step/file/Manual Checks；人工核验审批原话和“延期接入”没有丢。
- After this file: 完整Draft Plan存在，代码仍原样；Spec尚未反向链接，需要File2收尾。

#### File 2 — `MODIFY docs/egon/spec/2026-10-11-09-34-product-search-engine-proposal.md`

- Purpose: 只添加规范到本阶段Plan的关系链接，不重写已审批设计或开放同步选择。
- Symbols: Header `Related Plans`单行；Status/Updated/§5.3/§5.4/§20以及全部normative正文保持。
- Repository evidence: 当前主Spec这一字段为None；skill scope允许更新Related Plans；其他已批准前驱不需backlink。
- Dependencies and consumers: 依赖File1已存在；读取者从Spec进入Plan看到审批证据，validator要求相对link存在。
- Why now: File1完整且路由唯一后，再连接Spec；避免悬空或指向其他Plan。
- Contract/signature changes: 从 `Related Plans=None` 改为指向 `../plan/2026-10-11-10-01-elasticsearch-deferred-integration-plan.md`；无生产contract变化，不擅自将整份未闭合Spec升级为可执行Accepted。
- Input/output and state mapping: 原Spec字节→仅关系单行替换→移除新关系再还原后SHA-256应等于原27c292edd57ae409abd080d6962a6e379e20c88fd51cac6a4de4eadd8c5d4bc2；sourceREQ与技术字段不变。
- Error and edge behavior: 字段缺失/有他人新增Plan时先读取并合并确切关系，不能全文覆盖；hash超出预期停止追溯修改并保留并发工作；不revert其他dirty文档。
- Standards impact: MC-SCOPE-001/MC-TEST-001/MC-ARCH-001 — metadata-only证明保留全部规则和架构；无Java/DDL/Event代码。
- Literal rule enforcement: Rule 11 — 不修改架构/MP/DDL或MQ；其余Java规则通过保留正文不弱化，不能因为metadata改动去清理业务文件。
- Implementation pseudocode:

```markdown
Read primary.Header.Related Plans = None; check File1 plan path exists
Replace only Header.Related Plans with [Elasticsearch选型冻结与部署后接入阶段Plan](../plan/2026-10-11-10-01-elasticsearch-deferred-integration-plan.md)
Preserve Header.Status=Draft and all original requirements/interfaces/models/decisions/Manual Checks
Normalize updated Related Plans line back to None; compute SHA-256 == 27c292edd57ae409abd080d6962a6e379e20c88fd51cac6a4de4eadd8c5d4bc2
Run strict Spec validator and strict Plan validator; inspect git write scope; keep existing dirty CDC artifacts untouched
```

- Verification contribution: Spec strict校验与normalized SHA-256证明本轮没有重写Spec；来源qualified追溯与角色门禁仍完整。
- After this file: 双向关系完成，当前文档Step可交付；完整代码接入依旧等待部署后后继Spec/Plan，不继续执行。

- Validation working directory: `/Users/mario/SelfProject/enjoyshop`。
- Verification command: `python3 /Users/mario/SelfProject/Egon-COLA/.agents/skills/egon-coding-writing-plan/scripts/validate_plan.py docs/egon/plan/2026-10-11-10-01-elasticsearch-deferred-integration-plan.md --strict`；随后 `python3 /Users/mario/SelfProject/Egon-COLA/.agents/skills/egon-coding-writing-spec/scripts/validate_spec.py docs/egon/spec/2026-10-11-09-34-product-search-engine-proposal.md --strict`；范围/hash检查按§8方法。
- Expected result: 两validator exit0；metadata链接正确；primary归一化hash等于baseline；其他既有文档和SQL hash原样；tracked/staged生产差异为空。
- Failure returns to: 文档结构/链路失败→File1或File2；发现major设计缺口→§11保留门禁不编造；环境缺失不触发启动/下载安装。
- Completion criteria: 当前ES方向冻结/延期接入可追溯且无代码变更；全部主REQ保留状态明确；没有运行或功能验收伪claim。完整feature完成不在本Step结论内。
- Rollback: 如用户明确撤回本阶段文档，只处理本Plan和Related Plans那一行；不改变其他Spec/dirty文件、PG、MQ、offset或ES。
- Commit paths: `docs/egon/plan/2026-10-11-10-01-elasticsearch-deferred-integration-plan.md`, `docs/egon/spec/2026-10-11-09-34-product-search-engine-proposal.md`。
- Commit: `docs(search): freeze Elasticsearch direction and deferred integration gates` — 仅在用户另行要求提交时执行，当前不commit。

## 8. Test, Validation, and Quality Gates

| Gate/order | Working directory | Command or method | Scope | Expected result | Failure returns to | Requirements/runtime boundary |
| --- | --- | --- | --- | --- | --- | --- |
| Skill资源预检 | repository root | `python3 /Users/mario/SelfProject/Egon-COLA/.agents/skills/egon-coding-writing-plan/scripts/validate_skill_resources.py` | 当前skill资源路径 | exit0/PASS | skill资源；不部分起草 | 当前文档static |
| Plan结构 | repository root | §7准确validate_plan.py命令 | 本Plan | exit0/PASS，不要求Draft变Ready | File1 | 全部REQ追溯；纯文档 |
| Spec结构 | repository root | §7准确validate_spec.py命令 | metadata-updated主Spec | exit0/PASS；开门禁不受结构通过影响 | File2 | 主Spec正文保持；纯文档 |
| 原字节保护 | repository root | Python hashlib：primary把Related Plans恢复None后hash；其余既有docs/SQL逐文件hash与baseline比较 | 原始文件集合 | 仅新增Plan和primary一行 | File1/2，保留并发工作 | 所有前驱约束，无runtime |
| Git范围 | repository root | `git diff --name-only`、`git diff --cached --name-only`、`git status --short` | tracked/staged/untracked | 无代码差异/暂存，dirty CDC原样，新增仅本Plan | 当前Step | 主REQ-012 |
| 后继源码/测试 | goods module | 主Spec§14列出的focused Maven selectors，实际文件存在后再运行 | TEST-001–009/015 | 真实RED/GREEN/OAS；当前Not run | GATE-ES-03 | 未执行代码验证 |
| 后继外部验收 | 用户部署隔离环境 | 主SpecTEST-010–014/016和后继完整Spec命令 | ES/PG/Rabbit/入口 | 源当前态/迟到写/GC/重建/安全/中文/负载真实证据 | GATE-ES-01/02/04 | 当前Not run；不自动启动项目 |

本阶段使用现validators和stdlib检查，覆盖所有新增文档门禁；无需新scratch harness。后继代码门禁如果没有现命令覆盖，代码Plan才分配 `scripts/work/<当期plan-id>-<verb>.sh` 且最后Step明确promotion/delete；现在不创建或留未定临时脚本。现`deployment/cdc/check.sh`只验证CDC离线合同，不能拿其PASS证明ES查询/同步完成。

## 9. Migration, Compatibility, Rollout, and Rollback

本阶段migration/runtime rollout N/A：只两个Markdown，源SQL、manifest、实际DDL/history与生产config无改动；没有codegen plan/apply，故无SQL journal新增。所有旧Flyway/MP脚本始终immutable，不挑当前日期迁移version占位。

未来顺序依主Spec§16：实际ES版本及权限/中文能力→后继同步完整设计→精确代码Plan/审核→实现/离线验证→用户控制真实验收→PG全量新代+增量追平+无孤儿证明→获准后alias/公开开关。源current-state、delete/membership/fence/tombstone、partial bulk/未知完成ACK规则必须先于上线。未ready时不安装假consumer吞积压，不盲清offset/slot/DLQ。

兼容范围：现products十个REST operation、管理认证/授权、品牌/目录/图片/上传、GraphQL与ad API、匿名站点、安全链、MQ schema1/源epoch及NATIVE PRIMARY/LOCAL规则原样；后继不得因引入search退回旧STRATEGY或删除旧DDL。若后续新控制关系表获准，一逻辑变更一个新MP版本/manifest、CREATE catalog用生成器、agent自定义条件写与恢复证明齐全；本稿不发明表名/版本/SQL。

当前回滚只文档；未来切回旧ES generation前需证明旧代继续增量或已追平，否则明确不可用。保留商品事实、队列、receipt/fence/tombstone、slot/offset和安全证据，不自动drop或repair。

## 10. Requirement-to-Step Traceability Matrix

以下Step1的“完成”只针对方向冻结/延期接入记录与既有边界保护。搜索行为验收仍Deferred，不因为覆盖检查通过就标implemented。

| Requirement | Effective Spec section | Steps | Files | Tests/gates | Completion evidence |
| --- | --- | --- | --- | --- | --- |
| 搜索 REQ-001 | [主Spec](../spec/2026-10-11-09-34-product-search-engine-proposal.md) §4/17 | Step1：保存用户批准ES选型合同/状态 | 本Plan、主SpecRelated Plans | 已批准记录，未来TEST-016不在本轮 | 方向/保留范围可审；运行行为未实现 |
| 搜索 REQ-002 | [主Spec](../spec/2026-10-11-09-34-product-search-engine-proposal.md) §7/9/11 | Step1：保存name关键词/中文合同/状态 | 本Plan、主SpecRelated Plans | Deferred TEST-001/009/016 | 方向/保留范围可审；运行行为未实现 |
| 搜索 REQ-003 | [主Spec](../spec/2026-10-11-09-34-product-search-engine-proposal.md) §9/11 | Step1：保存分类/品牌filter合同/状态 | 本Plan、主SpecRelated Plans | Deferred TEST-003/008 | 方向/保留范围可审；运行行为未实现 |
| 搜索 REQ-004 | [主Spec](../spec/2026-10-11-09-34-product-search-engine-proposal.md) §7/9/11/13 | Step1：保存规格/严格facets合同/状态 | 本Plan、主SpecRelated Plans | Deferred TEST-002/003 | 方向/保留范围可审；运行行为未实现 |
| 搜索 REQ-005 | [主Spec](../spec/2026-10-11-09-34-product-search-engine-proposal.md) §9 | Step1：保存价格整数分区间合同/状态 | 本Plan、主SpecRelated Plans | Deferred TEST-001 | 方向/保留范围可审；运行行为未实现 |
| 搜索 REQ-006 | [主Spec](../spec/2026-10-11-09-34-product-search-engine-proposal.md) §7/9/10 | Step1：保存有界分页与exact total合同/状态 | 本Plan、主SpecRelated Plans | Deferred TEST-005/009 | 方向/保留范围可审；运行行为未实现 |
| 搜索 REQ-007 | [主Spec](../spec/2026-10-11-09-34-product-search-engine-proposal.md) §7/9 | Step1：保存排序白名单/稳定tie合同/状态 | 本Plan、主SpecRelated Plans | Deferred TEST-004 | 方向/保留范围可审；运行行为未实现 |
| 搜索 REQ-008 | [主Spec](../spec/2026-10-11-09-34-product-search-engine-proposal.md) §7/9/10 | Step1：保存独立安全高亮合同/状态 | 本Plan、主SpecRelated Plans | Deferred TEST-007/016 | 方向/保留范围可审；运行行为未实现 |
| 搜索 REQ-009 | [主Spec](../spec/2026-10-11-09-34-product-search-engine-proposal.md) §7/9/11/15 | Step1：保存tenant/可售/管理安全不变合同/状态 | 本Plan、主SpecRelated Plans | Deferred TEST-006/008/016 | 方向/保留范围可审；运行行为未实现 |
| 搜索 REQ-010 | [主Spec](../spec/2026-10-11-09-34-product-search-engine-proposal.md) §7/11/16 | Step1：保存当前态与receipt/fence/ACK合同/状态 | 本Plan、主SpecRelated Plans | Deferred GATE-ES-02；TEST-010–013 | 方向/保留范围可审；运行行为未实现 |
| 搜索 REQ-011 | [主Spec](../spec/2026-10-11-09-34-product-search-engine-proposal.md) §11/16 | Step1：保存全量/增量/重建切换合同/状态 | 本Plan、主SpecRelated Plans | Deferred GATE-ES-02/04；TEST-014/016 | 方向/保留范围可审；运行行为未实现 |
| 搜索 REQ-012 | [主Spec](../spec/2026-10-11-09-34-product-search-engine-proposal.md) §4/16 | Step1：保存部署后再具体接入合同/状态 | 本Plan、主SpecRelated Plans | 本轮两validator/metadata-only/无代码 | 方向/保留范围可审；运行行为未实现 |
| 前驱 2026-10-10-14-48-postgresql-cdc-advertisement-synchronization.md：REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010, REQ-011, REQ-012 | [边界依赖](../spec/2026-10-10-14-48-postgresql-cdc-advertisement-synchronization.md)，有效章节见§2.2 | Step1：只保护现合同，不重复实现历史功能 | 本Plan§2/9，前驱文件只读 | hash原样+生产diff空；历史runtime gate未重跑 | Unchanged；与同名搜索ID不混淆 |
| 前驱 2026-10-09-10-55-product-management-lifecycle.md：REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010, REQ-011, REQ-012, REQ-013, REQ-014, REQ-015 | [边界依赖](../spec/2026-10-09-10-55-product-management-lifecycle.md)，有效章节见§2.2 | Step1：只保护现合同，不重复实现历史功能 | 本Plan§2/9，前驱文件只读 | hash原样+生产diff空；历史runtime gate未重跑 | Unchanged；与同名搜索ID不混淆 |
| 前驱 2026-09-23-17-16-enjoyshop-scaffold-brand-crud.md：REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010, REQ-011, REQ-012, REQ-013, REQ-014, REQ-015, REQ-016, REQ-017, REQ-018, REQ-019, REQ-020, REQ-021, REQ-022 | [边界依赖](../spec/2026-09-23-17-16-enjoyshop-scaffold-brand-crud.md)，有效章节见§2.2 | Step1：只保护现合同，不重复实现历史功能 | 本Plan§2/9，前驱文件只读 | hash原样+生产diff空；历史runtime gate未重跑 | Unchanged；与同名搜索ID不混淆 |
| 前驱 2026-09-28-21-50-distributed-file-catalog-lookups.md：REQ-101, REQ-102, REQ-103, REQ-104, REQ-105, REQ-106, REQ-107, REQ-108 | [边界依赖](../spec/2026-09-28-21-50-distributed-file-catalog-lookups.md)，有效章节见§2.2 | Step1：只保护现合同，不重复实现历史功能 | 本Plan§2/9，前驱文件只读 | hash原样+生产diff空；历史runtime gate未重跑 | Unchanged；与同名搜索ID不混淆 |
| 前驱 2026-10-09-16-39-homepage-kubernetes-gateway-architecture.md：REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008 | [边界依赖](../spec/2026-10-09-16-39-homepage-kubernetes-gateway-architecture.md)，有效章节见§2.2 | Step1：只保护现合同，不重复实现历史功能 | 本Plan§2/9，前驱文件只读 | hash原样+生产diff空；历史runtime gate未重跑 | Unchanged；与同名搜索ID不混淆 |

前驱bare-ID重合时，以本章来源限定为准；未展开实施的历史品牌CRUD/目录/商品/广告要求只是Unchanged保护，不为它们增加新的测试/功能Steps。

## 11. Risks, Blockers, and User Decisions

| ID | Risk or decision | Impacted Steps/files | Evidence | Owner | Status/action |
| --- | --- | --- | --- | --- | --- |
| BLOCK-001 | 引擎是否ES | Step1文档 | 本轮用户明确“就用Elasticsearch” | User | Closed — 不再重复选型审核 |
| BLOCK-002 | ES尚未部署，actual version/TLS/权限/容量不明 | 未来GATE-ES-01/04，不阻止Step1 | 用户“部署完ES后再做”、主Spec§16 | User/运维 | Deferred — 等用户部署后再开始具体对接，不轮询/自动创建提醒 |
| BLOCK-003 | client精确坐标/版本/传递兼容及analyzer插件 | 未来GATE-ES-01/03；不改G/pom | 主SpecDEC-005；当前POM无ESclient | 后继设计与User | Deferred — 版本确定再提交精确引入方案，不重问已批准ES方向 |
| BLOCK-004 | receipt/membership/fence/GC/重建协调物理设计未完整 | 未来GATE-ES-02/03 | 主SpecDEC-006/§7.3/11/16、CDC EVENT-003 | 后继Spec owner/User | Deferred — 完整设计审核后才生成代码Plan；不现在造表/假consumer |
| BLOCK-005 | 主Spec历史Header Draft与本轮方向批准不同层次 | §2记录及主Specbacklink | §2.1原话，历史Spec开放细节 | Main agent | Closed for choice — 审批证据在Plan；完整接入状态仍非Ready |
| RISK-001 | 误把Plan严格PASS当代码/运行时完成 | Step1与后继任务 | §8/12区分artifact结构与Manual Checks | Main agent | 本稿明确未实现/Not run；完整接入verdict仍BLOCKED |

本轮无需用户再次确认已批准方案，亦无需现在发送secret或真实地址。计划仅在用户之后说明ES部署完成并要求继续时恢复具体接入；不创建automation、不自己等待或启动进程。

## 12. Review and Acceptance

### 12.1 Original requirement fidelity

“审核通过/就用ES”已经记录为Closed；“具体对接部署后再做”同样保留为实际执行门禁。主Spec的搜索REQ-002–011不删不改，尚未实现；前驱合同只保护，不重做。当前授权文档范围已经完成，完整接入Plan未伪标最终版。

### 12.2 Spec consistency

没有新增API/DTO/表/插件/缓存/状态设计，主Spec只改Related Plans。PLAN-CLAR-001只确定当前执行时机范围，不修改价格、facet、分页、可售、匿名、同步合同。ES选型由用户明确授权关闭，不是agent Plan内擅自重新设计。

### 12.3 Repository executability

当前Step的两个路径、准确validators、规范化hash与Git范围检查均可执行；没有runtime前置。后继client/同步符号、DDL与部署输入缺失时，不写依赖它们的编译Steps或伪commit。保留main/4e968aff、现Light verifier/Qualifier复制，以及三个既有未跟踪文档的范围。每逻辑任务最多一个commit，当前不自动提交。

### 12.4 Test and release completeness

本阶段RED/GREEN N/A，范围与文档校验是实际验证；没有执行Maven或启动服务/外部依赖。生产测试时序已保留在GATE-ES-03及主Spec§14，完整接入Manual Check仍不能PASS；alias/consumer/公开开关均不触发。scratch harness没有产生，故不存在未定disposition。

### 12.5 Blocking Manual Check

文档Step闭合不等于全部未来接入闭合。Applicable/PASS只表示当前阶段证据；Not applicable/N/A只对应本阶段没有Java变化，后继代码Plan必须逐文件重评，不能继承豁免。

| Check ID | Applicability | Status | Evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- |
| MC-ARCH-001 | Applicable | PASS | G/pom实际bytecode mappings、主Spec§6.1、本Plan§4.7 | 非open Light边界保留，没有新层/模块 | None |
| MC-REUSE-001 | Applicable | PASS | §4.7 ledger、现Python/Git/common/MP/MQ | 现工具与能力先检索，当前不复制设施 | None |
| MC-DEP-001 | Applicable | BLOCKED | BLOCK-003、主SpecDEC-005、G/pom缺ESclient | 具体版本/插件及传递兼容尚未形成可执行引入方案 | 部署后GATE-ES-01，User/后继设计 |
| MC-NAME-001 | Not applicable | N/A | §5当前tree只有Markdown | 当前无Java类型新增/改名 | None；未来§8生产tree重评 |
| MC-VALID-001 | Not applicable | N/A | §5/7无Controller/Service/消息代码变化 | 当前无新层边界，已保留主Specvalidation规则 | None；未来代码Plan逐handoff落实 |
| MC-MODEL-001 | Applicable | BLOCKED | BLOCK-004、主Spec§11同步控制状态待闭合 | 完整接入对象/控制持久化尚无可执行设计；当前不创建 | 部署后GATE-ES-02，补Spec |
| MC-CONVERT-001 | Not applicable | N/A | §5无Java/MapStruct输出 | 三个BaseForwardConverter仅在主Spec保留，当前不造假实现 | None |
| MC-LOG-001 | Not applicable | N/A | §5无业务类修改 | 当前不增加/改变日志点 | None |
| MC-BEAN-001 | Not applicable | N/A | G/lombok.config现Qualifier传播，§5无Bean修改 | 当前无新的注入/构造/注册 | None |
| MC-UTIL-001 | Not applicable | N/A | §5只Markdown；artifact校验用现工具 | 无utility依赖/Utils生产文件 | None |
| MC-JSON-001 | Not applicable | N/A | §7无DTO/Event/wire修改 | 当前不改变Jackson/7wrapper/schema | None |
| MC-TIME-001 | Not applicable | N/A | §5无Java时间字段；Header仅Asia/Shanghai文档日期 | 无新的日期/timeout实现 | None |
| MC-CONFIG-001 | Not applicable | N/A | §5无YAML/properties修改 | 四profile核心key与值原样 | None |
| MC-PATTERN-001 | Not applicable | N/A | §5无新业务flow实现；主Spec§13保留 | 已选择Specification/Repository，当前不扩展虚构工厂 | None |
| MC-SCOPE-001 | Applicable | PASS | §5/7、baseline hash、Git范围 | 两文档write scope，其他业务/SQL/dirty文档原样 | None |
| MC-TEST-001 | Applicable | BLOCKED | §8与主SpecTEST-010–016、BLOCK-004 | 本阶段文档校验可执行；完整sync runtime测试具体步骤需后继完整设计 | 部署后GATE-ES-02/03/04补完整代码验证 |
| MC-BLOCKER-001 | Applicable | BLOCKED | BLOCK-002/003/004、DEP/MODEL/TEST | 选型已闭合，完整接入环境/设计门禁仍保留 | 用户部署完成后再继续；不现在要求补输入 |

### 12.6 Final verdict

**BLOCKED — Spec or user decision required**

此结论只约束“完整接入实施”，不重新打开已经批准的Elasticsearch选择，也不阻止当前文档冻结Step交付。当前任务输出为已定方向的阶段Plan；后继必须在用户ES部署完成后补完整技术设计与代码Steps，不能由strict validator通过推断ready。当前不编码、不加依赖、不启动、不提交。
