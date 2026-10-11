# Elasticsearch 选型冻结与部署后接入阶段计划

| Field | Value |
| --- | --- |
| Document | `2026-10-11-10-10-product-search-offline-backend-implementation.md` |
| Template Version | `4` |
| Status | `Review` |
| Created | `2026-10-11 10:10 Asia/Shanghai` |
| Updated | `2026-10-11 10:45 Asia/Shanghai` |
| Owner | mario |
| Repository | enjoyshop |
| Scope | goods offline search backend API/contracts/validation/conversion/scope/config/tests; real ES and CDC integration deferred |
| Source Requirement | 用户本轮“Plan审核通过，先写代码吧，和es的对接，后续我部署后再进行”；允许本次完整离线步骤修订并执行，不引入客户端依赖 |
| Baseline Revision | `main@4e968affd7e5d8f7543d752cd971da9b7f295ae7`；三份已有未跟踪Spec/Plan保留；无tracked/staged修改 |
| Implements Spec | [商品搜索方案](../spec/2026-10-11-09-34-product-search-engine-proposal.md) |
| Spec Status | `Draft` |
| Spec Revision | 原Updated为2026-10-11 09:47 Asia/Shanghai；本轮只加Related Plans backlink；原文件SHA-256 `27c292edd57ae409abd080d6962a6e379e20c88fd51cac6a4de4eadd8c5d4bc2` |
| Effective Specs | [1: 2026-10-11-09-34-product-search-engine-proposal.md](../spec/2026-10-11-09-34-product-search-engine-proposal.md)；[2: 2026-10-10-14-48-postgresql-cdc-advertisement-synchronization.md](../spec/2026-10-10-14-48-postgresql-cdc-advertisement-synchronization.md)；[3: 2026-10-09-10-55-product-management-lifecycle.md](../spec/2026-10-09-10-55-product-management-lifecycle.md)；[4: 2026-09-23-17-16-enjoyshop-scaffold-brand-crud.md](../spec/2026-09-23-17-16-enjoyshop-scaffold-brand-crud.md)；[5: 2026-09-28-21-50-distributed-file-catalog-lookups.md](../spec/2026-09-28-21-50-distributed-file-catalog-lookups.md)；[6: 2026-10-09-16-39-homepage-kubernetes-gateway-architecture.md](../spec/2026-10-09-16-39-homepage-kubernetes-gateway-architecture.md) |
| Depends On Plans | None |
| Supersedes | [部署前冻结Plan](2026-10-11-10-01-elasticsearch-deferred-integration-plan.md) 仅当前“不可编码”执行时机限制；由用户本轮新授权替代；其ES选择和部署后门禁继续保持 |
| Superseded By | None |
| Related Plans | None |

## 1. Summary

本轮按用户最新授权实现部署前可完成的**离线搜索后端**，1个逻辑Step、1个focused commit。主Spec§8/9/10提供合同，保留纯Domain端口与真实ES适配隔离；不加client/POM依赖、不接ES、不建索引、不装CDC consumer。默认所有profile search.enabled=false；若显式启用但没有真实adapter，返回610403/503，而不是假200或空结果。

旧Plan文档Step已经交付，不能创建空提交模拟执行。本Plan取代它当前只准文档的限制，批准证据是用户本轮“先写代码”，不是把unknown ES version或同步存储当已决。现primary Header Draft不变，执行根据skill允许的本轮显式授权；全部真实ES和同步设计仍部署后处理。

## 2. Target Spec and Effective Design

### 2.1 Primary target

- Path: [商品搜索引擎方案](../spec/2026-10-11-09-34-product-search-engine-proposal.md)。
- Status: 文件Header为Draft；本轮用户明确批准该方案的ES方向和已描述合同，具体接入推迟。
- Revision: Updated=2026-10-11 09:47 Asia/Shanghai，baseline=4e968aff；原SHA-256见Header；只添加本Plan backlink。
- Approval evidence: 用户已经批准搜索方向和旧Plan，并在本轮明确要求“先写代码”，同时保留“和es的对接部署后进行”。本轮授权覆盖下面单Step的离线代码范围；主Spec接口/价格/规格/分页/租户合同不重新设计。

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

先RED HTTP未注册搜索应返回503/非法参数400；然后共享groups/enum/constraints -> Query/domain BO -> Result -> read port -> MapStruct converters -> Manage/DomainServiceImpl -> properties/Filter/Controller -> narrow error/Security/legacy-filter wiring -> four profile keys -> isolated boundary/OAS tests -> scoped regression/architecture verification -> audit -> 一个提交。模型是无SQL的搜索读合同，不是CREATE表CRUD脚手架，因此不调用codegen、不手写任何新PO/DAO/XML。没有相同职责的archetype search样例要删除；教学/user业务保持Unchanged。

### 4.2 Test-first strategy

File1在当前完整MockMvc/H2 fixture里只使用现有类，先确认GET搜索404而预期503产生行为RED；它不是编译失败。建好合同后再增校验/转换/端口负例，GREEN证明有限链路与安全错误。测试的成功数据来自**test-only** ProductSearchRepository，不出现在production，不作为已完成ES检索证据。

### 4.3 Sequential and parallel boundaries

一个Step，顺序处理；不派子agent；不启服务/浏览器/数据库。通用source绑定与安全Filter必须一起验证才能提交。既有four untracked documents不stage或改写。

### 4.4 Commit boundaries

按用户每逻辑任务最多一个commit，本Step验证、diff审查后一个 `feat(search): add offline product search boundary`，只明确文件paths。新Plan和其内execution audit作为本任务产物；旧Spec/Plan保持原样、不纳入提交。不会push/PR/deploy。

### 4.5 Spec Simplicity and Implementation-necessity Audit

| Spec element | Spec necessity verdict/section | Current repository evidence | Direct/reuse alternative | Interaction/implementation cost | Plan decision |
| --- | --- | --- | --- | --- | --- |
| Query/BO/Result/ports/converters | 主Spec§8/10 Add | goods已有Light/MapStruct/Common validation | 不新增层；保持公开与内部tenant分开 | 只进程内调用与有界结构 | Implement offline subset |
| 公共search API+site scope | 主Spec§9/15 Add | 当前管理API不能替代；AdPublicTenantFilter可参照 | 单HTTP返回全部data，scope后端derive | 新exact GET，其他方法拒绝 | Implement |
| 实际ES读/写/client/document mapping | 主Spec§5.4/11/16 Deferred | G/pom无client，版本未定 | Domain Repository端口先存在 | 本轮无网络/依赖/DSL实施 | Defer，不fake实现 |
| receipt/fence/rebuild/consumer | 主Spec§7/16 pending | 现刷新Event与queue足够触发 | 保留现MQ，不安装占位listener | 无新DDL/receipt/state | Defer，部署后Spec闭合 |

### 4.6 Change-unit Dependency Matrix

| Change unit | Requirements | Proof/RED point | Compile/runtime prerequisites | Produces | Consumers/unblocks | Owning Step |
| --- | --- | --- | --- | --- | --- | --- |
| 离线搜索完整边界 | 主REQ-001–012及前驱保护 | HTTP404->503/400；校验及转换反例 | 现JDK21/Maven缓存/MockMvc/H2，无外部系统 | 有界Query、trusted tenant、pure read port、Result、fail-closed API、OAS | 将来真实ES adapter实现原port，不需重改公开合同 | Step1 |

### 4.7 Java, Spring, and Egon-COLA Implementation Standards

唯一profile=现非open Light，bytecode architecture verifier与现POM可核；Domain接口/BO无Spring/MP/ESclient。复用现Jakarta、ValidationUtils、MapStruct/BaseForwardConverter、ResultRecord、Spring ObjectProvider（只用于不存在adapter时安全报不可用）、Security、MDC/host登记；不新增依赖。Common shared validator inner SearchInput参照已有ProductCompositionInput，避免Application/Domain环依赖。Slf4j、命名Bean、RequiredArgsConstructor、final依赖Qualifier与现lombok.config传播全部必须；全部普通carrier class使用Data/NoArgs/AllArgs/Accessors/Builder。两个one-way Converter现在实现；ES DocumentConverter待真实adapter，没有无用途DTO。SortEnum用闭合enum Strategy、CriteriaBO作为参数化Specification，不暴露任意sortField或DSL。

| Concern | Current repository evidence | Effective Spec decision | Planned implementation consequence | Owning Steps/checks |
| --- | --- | --- | --- | --- |
| Architecture profile | G/pom bytecode mappings与LightPersistenceArchitectureTest | 非open Light | Domain纯端口，Application/Infrastructure按当前树 | Step1/MC-ARCH-001 |
| Reuse/capability | 现Jakarta/Common ValidationUtils、MapStruct、ResultRecord、Spring ObjectProvider | 主Spec§6.1/10 | 现设施复用，无新dependency | Step1/MC-REUSE-001 |
| Naming/model/validation/conversion | ProductCompositionValidator共享Input与现Converters | 主Spec§10 | typed class/group/native+custom/BaseForwardConverter | Step1/MC-MODEL-001 |
| Bean/logging/util/JSON/time/config | lombok.config/四profile/Slf4j | 主Spec§8/15 | named final qualified beans、Jackson、Duration、profile parity | Step1/MC-BEAN-001 |
| Business variation/pattern | 主SpecCriteria规范和闭合sort编码 | 主Spec§13 | 参数化Specification与enum Strategy，无多引擎Factory | Step1/MC-PATTERN-001 |

#### Capability reuse ledger

| Need | Candidates inspected | Exact evidence | Fit/gap | Decision | Added dependency/custom code | Owning Step/check |
| --- | --- | --- | --- | --- | --- | --- |
| 输入校验 | Jakarta与Common ValidationUtils | G/pom、现ProductCompositionValidator | 原生leaf/共享跨字段约束已足够 | Reuse | 搜索metadata，0dependency | Step1/MC-REUSE-001 |
| 模型转换 | MapStruct/Common BaseForwardConverter | 当前ProductQueryConverter/AdPublicResultConverter | 单向投影足够 | Reuse | 2Converters，0dependency | Step1/MC-CONVERT-001 |
| 未接引擎 | Spring ObjectProvider | 当前NativeHttpSecurityConfiguration使用同API | absence可显式报503，不需fake adapter | Reuse | 没有真实ESclient/placeholder Bean | Step1/MC-DEP-001 |
| 运行验证 | MockMvc/H2/JUnit及skill validators | 当前PersistenceTestSupport/AdOpenApiContractTest | 有限进程内验证，无真实网络 | Reuse | scoped tests，0dependency | Step1/MC-TEST-001 |

### 4.8 User-mandated Java Rule Implementation Matrix

| Literal rule | Spec source | Repository evidence | Exact files and order | Pseudocode obligations | Validation gate | Steps | Status/blocker |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Rule 1 | 主Spec§6.2/8–15 | 当前POM/邻近商品与广告/Qualifier配置 | 按§7文件序列，typed Query/BO/Result/Service/Validator | 只语义suffix，不Info/Data | focused tests + architecture + 手工diff | Step1 | PASS |
| Rule 2 | 主Spec§6.2/8–15 | 当前POM/邻近商品与广告/Qualifier配置 | 按§7文件序列，groups/native/custom validator -> each handoff | PublicSearch/InternalSearch/Output、@Valid/@Validated与ValidationUtils manual metadata | focused tests + architecture + 手工diff | Step1 | PASS |
| Rule 3 | 主Spec§6.2/8–15 | 当前POM/邻近商品与广告/Qualifier配置 | 按§7文件序列，carrier -> BaseForwardConverter -> consumers | 普通class全Lombok，MapStruct，没有JSONcopy | focused tests + architecture + 手工diff | Step1 | PASS |
| Rule 4 | 主Spec§6.2/8–15 | 当前POM/邻近商品与广告/Qualifier配置 | 按§7文件序列，named Beans/wiring | Slf4j、final qualified RequiredArgsConstructor，propagation核验 | focused tests + architecture + 手工diff | Step1 | PASS |
| Rule 5 | 主Spec§6.2/8–15 | 当前POM/邻近商品与广告/Qualifier配置 | 按§7文件序列，现JDK/Commons/Spring facilities | 无新utility/client依赖 | focused tests + architecture + 手工diff | Step1 | PASS |
| Rule 6 | 主Spec§6.2/8–15 | 当前POM/邻近商品与广告/Qualifier配置 | 按§7文件序列，Result/Jackson/enum | ALWAYS nullable highlight、IDstring、JsonValue；无ordinal | focused tests + architecture + 手工diff | Step1 | PASS |
| Rule 7 | 主Spec§6.2/8–15 | 当前POM/邻近商品与广告/Qualifier配置 | 按§7文件序列，四profile together | enabled false/sites empty/timeout同key | focused tests + architecture + 手工diff | Step1 | PASS |
| Rule 9 | 主Spec§6.2/8–15 | 当前POM/邻近商品与广告/Qualifier配置 | 按§7文件序列，sort enum/criteria组合 | enum Strategy+参数化Specification，无字符串任意字段dispatch | focused tests + architecture + 手工diff | Step1 | PASS |
| Rule 10 | 主Spec§6.2/8–15 | 当前POM/邻近商品与广告/Qualifier配置 | 按§7文件序列，timeout props | Duration，实际ES/日期映射deferred | focused tests + architecture + 手工diff | Step1 | PASS |
| Rule 11 | 主Spec§6.2/8–15 | 当前POM/邻近商品与广告/Qualifier配置 | 按§7文件序列，all current paths | 非open Light/MP/DDL/MQ边界原样，无新持久化 | focused tests + architecture + 手工diff | Step1 | PASS |

## 5. Change File Tree

当前准确write inventory如下，不包括任何旧Spec/Plan/SQL/POM或真实ES adapter。

| Operation | Path | Current evidence/symbol | Final symbols/state | Responsibility | Step | Requirements | Validation owner |
| --- | --- | --- | --- | --- | --- | --- | --- |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/goods/ProductSearchControllerWebTest.java` | 主Spec§8/9/10 + 现同包商品/广告实现；当前不存在 | MockMvc requests for missing route, unknown host, invalid input and absent adapter; assert 503/404/400 instead of 200 empty | HTTP boundary RED and GREEN | Step1 | 搜索REQ-001–012 | §8验证 |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/validation/ProductSearchValidationGroup.java` | 主Spec§8/9/10 + 现同包商品/广告实现；当前不存在 | PublicSearch/InternalSearch extend Default; Output group validates complete engine page | layer groups | Step1 | 搜索REQ-001–012 | §8验证 |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/enums/ProductSearchSortEnum.java` | 主Spec§8/9/10 + 现同包商品/广告实现；当前不存在 | five stable @JsonValue codes; enum Strategy produces immutable SortClauseBO field/direction + skuId tie; no arbitrary field dispatch | sort whitelist | Step1 | 搜索REQ-001–012 | §8验证 |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/validation/ValidProductSearch.java` | 主Spec§8/9/10 + 现同包商品/广告实现；当前不存在 | class-level @Constraint ProductSearchValidator for price/window associations | shared metadata | Step1 | 搜索REQ-001–012 | §8验证 |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/validation/ProductSearchValidator.java` | 主Spec§8/9/10 + 现同包商品/广告实现；当前不存在 | nested SearchInput follows existing ProductCompositionInput; native constraints own leaf fields; cross min/max and long window check produce property paths | cross-field validation | Step1 | 搜索REQ-001–012 | §8验证 |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/search/ProductSearchQuery.java` | 主Spec§8/9/10 + 现同包商品/广告实现；当前不存在 | public keywords/categoryId/brandId/minPrice/maxPrice/pageNo/pageSize/sort/specs; native/container constraints; no tenant field | offline contract and layers | Step1 | 搜索REQ-001–012 | §8验证 |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/search/SearchCriteriaBO.java` | 主Spec§8/9/10 + 现同包商品/广告实现；当前不存在 | internal tenantId/visible plus public criteria, @ValidProductSearch/@AssertTrue, shared metadata; immutable canonical spec values and typed sort plan | offline contract and layers | Step1 | 搜索REQ-001–012 | §8验证 |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/search/ProductSearchItemBO.java` | 主Spec§8/9/10 + 现同包商品/广告实现；当前不存在 | SKU id/spu/name/highlight/price/image/brand/category/specs, all required except highlight | offline contract and layers | Step1 | 搜索REQ-001–012 | §8验证 |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/search/SearchFacetBucketBO.java` | 主Spec§8/9/10 + 现同包商品/广告实现；当前不存在 | key/label/count positive-or-zero, no internal tenant | offline contract and layers | Step1 | 搜索REQ-001–012 | §8验证 |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/search/SearchFacetBO.java` | 主Spec§8/9/10 + 现同包商品/广告实现；当前不存在 | kind brand/category/spec, name/buckets/truncated with bounds | offline contract and layers | Step1 | 搜索REQ-001–012 | §8验证 |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/search/ProductSearchPageBO.java` | 主Spec§8/9/10 + 现同包商品/广告实现；当前不存在 | items/total/pageNo/pageSize/facets/truncation, derived safe totals/window; output constraints | offline contract and layers | Step1 | 搜索REQ-001–012 | §8验证 |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/search/ProductSearchItemResult.java` | 主Spec§8/9/10 + 现同包商品/广告实现；当前不存在 | public projection with string IDs, integer cents, always-present nullable safe highlight | offline contract and layers | Step1 | 搜索REQ-001–012 | §8验证 |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/search/SearchFacetBucketResult.java` | 主Spec§8/9/10 + 现同包商品/广告实现；当前不存在 | public key/label/count | offline contract and layers | Step1 | 搜索REQ-001–012 | §8验证 |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/search/SearchFacetResult.java` | 主Spec§8/9/10 + 现同包商品/广告实现；当前不存在 | public kind/name/buckets/truncated | offline contract and layers | Step1 | 搜索REQ-001–012 | §8验证 |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/search/ProductSearchPageResult.java` | 主Spec§8/9/10 + 现同包商品/广告实现；当前不存在 | rows/count/page metadata/facets; ResultRecord owns outer seven fields | offline contract and layers | Step1 | 搜索REQ-001–012 | §8验证 |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/search/ProductSearchRepository.java` | 主Spec§8/9/10 + 现同包商品/广告实现；当前不存在 | @Valid criteria read port only; no production adapter, fake dataset, SQL fallback, client or MQ listener | offline contract and layers | Step1 | 搜索REQ-001–012 | §8验证 |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/search/ProductSearchDomainService.java` | 主Spec§8/9/10 + 现同包商品/广告实现；当前不存在 | pure domain @Valid/@NotNull internal criteria signature | offline contract and layers | Step1 | 搜索REQ-001–012 | §8验证 |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/search/ProductSearchQueryConverter.java` | 主Spec§8/9/10 + 现同包商品/广告实现；当前不存在 | BaseForwardConverter Query->Criteria; @Context tenant from dedicated trusted MDC scope; canonical spec copies via named mapping | offline contract and layers | Step1 | 搜索REQ-001–012 | §8验证 |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/search/ProductSearchResultConverter.java` | 主Spec§8/9/10 + 现同包商品/广告实现；当前不存在 | BaseForwardConverter PageBO->PageResult nested mappings; safe mark-only highlight; no sensitive fields | offline contract and layers | Step1 | 搜索REQ-001–012 | §8验证 |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/search/ProductSearchManage.java` | 主Spec§8/9/10 + 现同包商品/广告实现；当前不存在 | @Validated PublicSearch input, explicit result signature | offline contract and layers | Step1 | 搜索REQ-001–012 | §8验证 |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/search/ProductSearchManageImpl.java` | 主Spec§8/9/10 + 现同包商品/广告实现；当前不存在 | named service, qualified Lombok injection; common metadata -> converter -> domain -> result converter; no engine access | offline contract and layers | Step1 | 搜索REQ-001–012 | §8验证 |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/config/ProductSearchProperties.java` | 主Spec§8/9/10 + 现同包商品/广告实现；当前不存在 | named validated properties app.search enabled=false/public-sites={}/query-timeout=PT2S; no endpoint/client provisioning | offline contract and layers | Step1 | 搜索REQ-001–012 | §8验证 |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/search/service/impl/ProductSearchDomainServiceImpl.java` | 主Spec§8/9/10 + 现同包商品/广告实现；当前不存在 | validate internal criteria, obtain optional qualified elasticProductSearchRepository via ObjectProvider; absent/disabled ->610403; validate output and tenant scope; no fake search | offline contract and layers | Step1 | 搜索REQ-001–012 | §8验证 |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/goods/ProductSearchController.java` | 主Spec§8/9/10 + 现同包商品/广告实现；当前不存在 | exact GET product-search, strict scalar/spec.* parsing once, validate query, call Manage; explicit OpenAPI operation and responses/security | offline contract and layers | Step1 | 搜索REQ-001–012 | §8验证 |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/filter/ProductSearchTenantFilter.java` | 主Spec§8/9/10 + 现同包商品/广告实现；当前不存在 | exact public GET; enabled/site check, canonical host -> tenant, override trusted MDC keys, restore finally; no caller tenant or forwarded-host derivation | offline contract and layers | Step1 | 搜索REQ-001–012 | §8验证 |
| MODIFY | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/enums/GoodsErrorStatus.java` | 主Spec§8/9/10 + 现同包商品/广告实现；已读当前符号 | append 610401/402/403/499 only; preserve old codes | search protection | Step1 | 搜索REQ-001–012 | §8验证 |
| MODIFY | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/handler/BaseExceptionHandler.java` | 主Spec§8/9/10 + 现同包商品/广告实现；已读当前符号 | search-route error/field mapping without changing brand/product/ad branches | search protection | Step1 | 搜索REQ-001–012 | §8验证 |
| MODIFY | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/filter/TenantContextFilter.java` | 主Spec§8/9/10 + 现同包商品/广告实现；已读当前符号 | exclude search scope from legacy untrusted tenant fallback, preserve all old route rules | search protection | Step1 | 搜索REQ-001–012 | §8验证 |
| MODIFY | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/start/config/NativeHttpSecurityConfiguration.java` | 主Spec§8/9/10 + 现同包商品/广告实现；已读当前符号 | dedicated exact GET search chain; deny other methods/subpaths; existing management chain untouched | search protection | Step1 | 搜索REQ-001–012 | §8验证 |
| MODIFY | `enjoyshop-service/enjoyshop-service-goods/src/main/resources/application.yml` | 主Spec§8/9/10 + 现同包商品/广告实现；已读当前符号 | add same app.search enabled=false/public-sites={}/query-timeout=PT2S; no ES endpoint/secret/config provisioning | profile parity | Step1 | 搜索REQ-001–012 | §8验证 |
| MODIFY | `enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-dev.yml` | 主Spec§8/9/10 + 现同包商品/广告实现；已读当前符号 | add same app.search enabled=false/public-sites={}/query-timeout=PT2S; no ES endpoint/secret/config provisioning | profile parity | Step1 | 搜索REQ-001–012 | §8验证 |
| MODIFY | `enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-test.yml` | 主Spec§8/9/10 + 现同包商品/广告实现；已读当前符号 | add same app.search enabled=false/public-sites={}/query-timeout=PT2S; no ES endpoint/secret/config provisioning | profile parity | Step1 | 搜索REQ-001–012 | §8验证 |
| MODIFY | `enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-prod.yml` | 主Spec§8/9/10 + 现同包商品/广告实现；已读当前符号 | add same app.search enabled=false/public-sites={}/query-timeout=PT2S; no ES endpoint/secret/config provisioning | profile parity | Step1 | 搜索REQ-001–012 | §8验证 |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/application/search/ProductSearchValidationTest.java` | 主Spec§8/9/10 + 现同包商品/广告实现；当前不存在 | Validator + MapStruct input/price/spec/copy/sort/response/security encoding tests | focused proof | Step1 | 搜索REQ-001–012 | §8验证 |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/search/ProductSearchBoundaryTest.java` | 主Spec§8/9/10 + 现同包商品/广告实现；当前不存在 | method validation/group/tenant mismatch/missing provider/output corruption and test-only port result projection | focused proof | Step1 | 搜索REQ-001–012 | §8验证 |
| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/goods/ProductSearchOpenApiContractTest.java` | 主Spec§8/9/10 + 现同包商品/广告实现；当前不存在 | MockMvc real OAS search operation, query/response/errors/security=[] and old management behavior | focused proof | Step1 | 搜索REQ-001–012 | §8验证 |
| MODIFY | `enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/architecture/LightPersistenceArchitectureTest.java` | 主Spec§8/9/10 + 现同包商品/广告实现；已读当前符号 | account for one new domain Repository port and one DomainServiceImpl; assert pure domain and no real ES client/listener/POM dependency | Light preservation | Step1 | 搜索REQ-001–012 | §8验证 |
| CREATE | `docs/egon/plan/2026-10-11-10-10-product-search-offline-backend-implementation.md` | 本轮授权修订 | 单Step计划与audit | 记录时机/范围 | Step1 | REQ-001/012 | validator |

| CREATE | `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/goods/ProductSearchOpenApiConfiguration.java` | 当前@Schema(implementation=ResultRecord.class)擦除validation payload；现AdOpenApiConfiguration有真实generic组件登记 | SearchRequestError/SearchBusinessError typed generic components | 修正真实错误schema，不造endpoint/wrapper subtype | Step1 | REQ-009/012 | OAS RED/GREEN |

## 6. Prerequisites, Constraints, and Plan Clarifications

### 6.1 Repository and worktree baseline

main@4e968aff；当前tracked/staged干净；4个既有untracked文档（CDC Spec/Plan、搜索Spec/旧阶段Plan）保持hash原样、不能stage。用户AGENTS每逻辑任务最多1commit优先于技能per-step默认；本Plan本来就单Step。

### 6.2 Build, test, and environment prerequisites

现JDK21/Maven3.9；测试使用当前resolved byte-buddy-agent1.17.8预加载，规避sandbox禁止Mockito动态attach，不下载或改POM；使用`mvn -o`仅已缓存现依赖，baseline `-DskipTests test` 编译成功但未运行tests；已有Lombok @Data warning和旧接口deprecated warning记录为baseline，不借任务修无关代码。不联网下载安装。finite MockMvc/H2 tests允许，不启动web port/PG/Redis/Rabbit/ES。

### 6.3 Immutable constraints and approved decisions

本轮授权离线coding、此前已选择ES，不再问选型；不引入未定client/插件、真实查询DSL/索引/receipt/consumer。未接入时503，不虚造商品/空索引结果。当前公开tenant从注册host取得，不从默认租户/header取得；legacy TenantContextFilter需要精确排除搜索路由，这是安全链直接依赖。

### 6.4 Plan Clarifications

当前延期范围从“全部编码等部署”变为“本轮离线后端编码、真实ES接入等部署”，依据是**用户明确改变执行时机**，不是agent小推断。ObjectProvider沿Spring已存在能力在DomainServiceImpl中解析future `elasticProductSearchRepository`：当前没有production Bean就610403；不是新增占位adapter。其余名称/路径按主Spec。未引入任何新的业务语义。

## 7. Ordered File-by-file Implementation Steps

### Step 1 — 实现搜索离线合同及fail-closed后端链路

- Requirements: `REQ-001`, `REQ-002`, `REQ-003`, `REQ-004`, `REQ-005`, `REQ-006`, `REQ-007`, `REQ-008`, `REQ-009`, `REQ-010`, `REQ-011`, `REQ-012`, `REQ-013`, `REQ-014`, `REQ-015`, `REQ-016`, `REQ-017`, `REQ-018`, `REQ-019`, `REQ-020`, `REQ-021`, `REQ-022`, `REQ-101`, `REQ-102`, `REQ-103`, `REQ-104`, `REQ-105`, `REQ-106`, `REQ-107`, `REQ-108` — 主搜索REQ-001–012记录采用/保存未来功能；其他来源ID仅保护Unchanged合同，并不重新实施。
- Dependencies: 用户当前“先写代码”明确授权；ES实时/同步storage不作为此Step依赖。
- Baseline state: main@4e968aff，当前无search路由/client，现Java compile成功；既有dirty docs不能改写。
- Observable outcome: 精确GET搜索输入校验及trusted scope进入真实Manage/Domain端口；未部署/无adapter明确503；测试adapter的完整页可安全投影，production不造结果。
- End state: 离线API/validation/types/converters/端口/Filter/配置/OAS与focused tests通过；真实ES/DSL/index/CDC/rebuild仍缺失且显式deferred。
- Test-first gate: Required — File1在现有MockMvc上下文期待503/400，现无route404；测试引用已有fixture可编译。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-JSON-001, MC-TIME-001, MC-CONFIG-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001, MC-BLOCKER-001。
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11。
- Ordered files:

#### File 1 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/goods/ProductSearchControllerWebTest.java`

- Purpose: HTTP boundary RED and GREEN；MockMvc requests for missing route, unknown host, invalid input and absent adapter; assert 503/404/400 instead of 200 empty。
- Symbols: `ProductSearchControllerWebTest`及上列字段/方法；按主Spec§8/9/10和本轮offline边界实现。
- Repository evidence: 现同包Product/Ad Query/BO/Result/Controller、ProductCompositionValidator、AdPublicTenantFilter、G/lombok.config及有限MockMvc fixture；此文件operation已在§5核对。
- Dependencies and consumers: public HTTP -> Manage -> domain port -> infrastructure service -> optional repository；carrier与Mapper先于其消费者；File1只现有类，不引用未创建搜索类型。
- Why now: 按上文编译/RED/生产/接线/回归顺序；这是缺少route的RED证明。
- Contract/signature changes: MockMvc requests for missing route, unknown host, invalid input and absent adapter; assert 503/404/400 instead of 200 empty；涉及的公开边界是主Spec已批准API-001，只有精确search GET变化。
- Input/output and state mapping: §9字符串ID/整数分、1基窗口、spec同键OR异键AND、rows/facets不null；MDC绑定tenant/visible不可由client发送；所有source业务/DDL/MQ unchanged。
- Error and edge behavior: 必须覆盖invalid/missing tenant、duplicate params/单次decode、unknown sort、无adapter503、坏output不伪200和thread scope finally restoration；不自动retry或启动系统。
- Standards impact: MC-ARCH-001, MC-REUSE-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-SCOPE-001, MC-TEST-001 — 适用字段按§4.8落实；测试/YAML无注解要求，生产carrier与业务Bean必须遵守。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 本Java/配置涉及者按§4.8；测试仅其边界证明，不给不涉及的持久化/日期造实现。
- Implementation pseudocode:

```java
ProductSearchControllerWebTest: MockMvc requests for missing route, unknown host, invalid input and absent adapter; assert 503/404/400 instead of 200 empty
input -> exact Spec §9 fields / trusted context -> existing typed boundary; preserve null/default/precision semantics
failure -> SEARCH_REQUEST_INVALID(610401) / SITE(610402) / UNAVAILABLE(610403); no hidden SQL/MQ/network mutation
proof -> HTTP boundary RED and GREEN; inspect generated MapStruct/Lombok contracts and focused positive/negative tests
```

- Verification contribution: HTTP boundary RED and GREEN，§8列精确command；不把尚未写ES adapter当检索完成。
- After this file: route-negative test可以编译并因现404而RED；不把compile failure当RED。

#### File 2 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/validation/ProductSearchValidationGroup.java`

- Purpose: layer groups；PublicSearch/InternalSearch extend Default; Output group validates complete engine page。
- Symbols: `ProductSearchValidationGroup`及上列字段/方法；按主Spec§8/9/10和本轮offline边界实现。
- Repository evidence: 现同包Product/Ad Query/BO/Result/Controller、ProductCompositionValidator、AdPublicTenantFilter、G/lombok.config及有限MockMvc fixture；此文件operation已在§5核对。
- Dependencies and consumers: public HTTP -> Manage -> domain port -> infrastructure service -> optional repository；carrier与Mapper先于其消费者；复用当前Spring/Jakarta/Common/MapStruct，不引入client或新模块。
- Why now: 按上文编译/RED/生产/接线/回归顺序；前置metadata/types/port建立后本文件形成该职责的最小实现或验证。
- Contract/signature changes: PublicSearch/InternalSearch extend Default; Output group validates complete engine page；涉及的公开边界是主Spec已批准API-001，只有精确search GET变化。
- Input/output and state mapping: §9字符串ID/整数分、1基窗口、spec同键OR异键AND、rows/facets不null；MDC绑定tenant/visible不可由client发送；所有source业务/DDL/MQ unchanged。
- Error and edge behavior: 必须覆盖invalid/missing tenant、duplicate params/单次decode、unknown sort、无adapter503、坏output不伪200和thread scope finally restoration；不自动retry或启动系统。
- Standards impact: MC-ARCH-001, MC-REUSE-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-SCOPE-001, MC-TEST-001 — 适用字段按§4.8落实；测试/YAML无注解要求，生产carrier与业务Bean必须遵守。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 本Java/配置涉及者按§4.8；测试仅其边界证明，不给不涉及的持久化/日期造实现。
- Implementation pseudocode:

```java
ProductSearchValidationGroup: PublicSearch/InternalSearch extend Default; Output group validates complete engine page
input -> exact Spec §9 fields / trusted context -> existing typed boundary; preserve null/default/precision semantics
failure -> SEARCH_REQUEST_INVALID(610401) / SITE(610402) / UNAVAILABLE(610403); no hidden SQL/MQ/network mutation
proof -> layer groups; inspect generated MapStruct/Lombok contracts and focused positive/negative tests
```

- Verification contribution: layer groups，§8列精确command；不把尚未写ES adapter当检索完成。
- After this file: 该职责可编译/可验证，后续consumer接线或最终focused GREEN仍在同Step内。

#### File 3 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/enums/ProductSearchSortEnum.java`

- Purpose: sort whitelist；five stable @JsonValue codes; enum Strategy produces immutable SortClauseBO field/direction + skuId tie; no arbitrary field dispatch。
- Symbols: `ProductSearchSortEnum`及上列字段/方法；按主Spec§8/9/10和本轮offline边界实现。
- Repository evidence: 现同包Product/Ad Query/BO/Result/Controller、ProductCompositionValidator、AdPublicTenantFilter、G/lombok.config及有限MockMvc fixture；此文件operation已在§5核对。
- Dependencies and consumers: public HTTP -> Manage -> domain port -> infrastructure service -> optional repository；carrier与Mapper先于其消费者；复用当前Spring/Jakarta/Common/MapStruct，不引入client或新模块。
- Why now: 按上文编译/RED/生产/接线/回归顺序；前置metadata/types/port建立后本文件形成该职责的最小实现或验证。
- Contract/signature changes: five stable @JsonValue codes; enum Strategy produces immutable SortClauseBO field/direction + skuId tie; no arbitrary field dispatch；涉及的公开边界是主Spec已批准API-001，只有精确search GET变化。
- Input/output and state mapping: §9字符串ID/整数分、1基窗口、spec同键OR异键AND、rows/facets不null；MDC绑定tenant/visible不可由client发送；所有source业务/DDL/MQ unchanged。
- Error and edge behavior: 必须覆盖invalid/missing tenant、duplicate params/单次decode、unknown sort、无adapter503、坏output不伪200和thread scope finally restoration；不自动retry或启动系统。
- Standards impact: MC-ARCH-001, MC-REUSE-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-SCOPE-001, MC-TEST-001 — 适用字段按§4.8落实；测试/YAML无注解要求，生产carrier与业务Bean必须遵守。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 本Java/配置涉及者按§4.8；测试仅其边界证明，不给不涉及的持久化/日期造实现。
- Implementation pseudocode:

```java
ProductSearchSortEnum: five stable @JsonValue codes; enum Strategy produces immutable SortClauseBO field/direction + skuId tie; no arbitrary field dispatch
input -> exact Spec §9 fields / trusted context -> existing typed boundary; preserve null/default/precision semantics
failure -> SEARCH_REQUEST_INVALID(610401) / SITE(610402) / UNAVAILABLE(610403); no hidden SQL/MQ/network mutation
proof -> sort whitelist; inspect generated MapStruct/Lombok contracts and focused positive/negative tests
```

- Verification contribution: sort whitelist，§8列精确command；不把尚未写ES adapter当检索完成。
- After this file: 该职责可编译/可验证，后续consumer接线或最终focused GREEN仍在同Step内。

#### File 4 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/validation/ValidProductSearch.java`

- Purpose: shared metadata；class-level @Constraint ProductSearchValidator for price/window associations。
- Symbols: `ValidProductSearch`及上列字段/方法；按主Spec§8/9/10和本轮offline边界实现。
- Repository evidence: 现同包Product/Ad Query/BO/Result/Controller、ProductCompositionValidator、AdPublicTenantFilter、G/lombok.config及有限MockMvc fixture；此文件operation已在§5核对。
- Dependencies and consumers: public HTTP -> Manage -> domain port -> infrastructure service -> optional repository；carrier与Mapper先于其消费者；复用当前Spring/Jakarta/Common/MapStruct，不引入client或新模块。
- Why now: 按上文编译/RED/生产/接线/回归顺序；前置metadata/types/port建立后本文件形成该职责的最小实现或验证。
- Contract/signature changes: class-level @Constraint ProductSearchValidator for price/window associations；涉及的公开边界是主Spec已批准API-001，只有精确search GET变化。
- Input/output and state mapping: §9字符串ID/整数分、1基窗口、spec同键OR异键AND、rows/facets不null；MDC绑定tenant/visible不可由client发送；所有source业务/DDL/MQ unchanged。
- Error and edge behavior: 必须覆盖invalid/missing tenant、duplicate params/单次decode、unknown sort、无adapter503、坏output不伪200和thread scope finally restoration；不自动retry或启动系统。
- Standards impact: MC-ARCH-001, MC-REUSE-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-SCOPE-001, MC-TEST-001 — 适用字段按§4.8落实；测试/YAML无注解要求，生产carrier与业务Bean必须遵守。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 本Java/配置涉及者按§4.8；测试仅其边界证明，不给不涉及的持久化/日期造实现。
- Implementation pseudocode:

```java
ValidProductSearch: class-level @Constraint ProductSearchValidator for price/window associations
input -> exact Spec §9 fields / trusted context -> existing typed boundary; preserve null/default/precision semantics
failure -> SEARCH_REQUEST_INVALID(610401) / SITE(610402) / UNAVAILABLE(610403); no hidden SQL/MQ/network mutation
proof -> shared metadata; inspect generated MapStruct/Lombok contracts and focused positive/negative tests
```

- Verification contribution: shared metadata，§8列精确command；不把尚未写ES adapter当检索完成。
- After this file: 该职责可编译/可验证，后续consumer接线或最终focused GREEN仍在同Step内。

#### File 5 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/validation/ProductSearchValidator.java`

- Purpose: cross-field validation；nested SearchInput follows existing ProductCompositionInput; native constraints own leaf fields; cross min/max and long window check produce property paths。
- Symbols: `ProductSearchValidator`及上列字段/方法；按主Spec§8/9/10和本轮offline边界实现。
- Repository evidence: 现同包Product/Ad Query/BO/Result/Controller、ProductCompositionValidator、AdPublicTenantFilter、G/lombok.config及有限MockMvc fixture；此文件operation已在§5核对。
- Dependencies and consumers: public HTTP -> Manage -> domain port -> infrastructure service -> optional repository；carrier与Mapper先于其消费者；复用当前Spring/Jakarta/Common/MapStruct，不引入client或新模块。
- Why now: 按上文编译/RED/生产/接线/回归顺序；前置metadata/types/port建立后本文件形成该职责的最小实现或验证。
- Contract/signature changes: nested SearchInput follows existing ProductCompositionInput; native constraints own leaf fields; cross min/max and long window check produce property paths；涉及的公开边界是主Spec已批准API-001，只有精确search GET变化。
- Input/output and state mapping: §9字符串ID/整数分、1基窗口、spec同键OR异键AND、rows/facets不null；MDC绑定tenant/visible不可由client发送；所有source业务/DDL/MQ unchanged。
- Error and edge behavior: 必须覆盖invalid/missing tenant、duplicate params/单次decode、unknown sort、无adapter503、坏output不伪200和thread scope finally restoration；不自动retry或启动系统。
- Standards impact: MC-ARCH-001, MC-REUSE-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-SCOPE-001, MC-TEST-001 — 适用字段按§4.8落实；测试/YAML无注解要求，生产carrier与业务Bean必须遵守。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 本Java/配置涉及者按§4.8；测试仅其边界证明，不给不涉及的持久化/日期造实现。
- Implementation pseudocode:

```java
ProductSearchValidator: nested SearchInput follows existing ProductCompositionInput; native constraints own leaf fields; cross min/max and long window check produce property paths
input -> exact Spec §9 fields / trusted context -> existing typed boundary; preserve null/default/precision semantics
failure -> SEARCH_REQUEST_INVALID(610401) / SITE(610402) / UNAVAILABLE(610403); no hidden SQL/MQ/network mutation
proof -> cross-field validation; inspect generated MapStruct/Lombok contracts and focused positive/negative tests
```

- Verification contribution: cross-field validation，§8列精确command；不把尚未写ES adapter当检索完成。
- After this file: 该职责可编译/可验证，后续consumer接线或最终focused GREEN仍在同Step内。

#### File 6 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/search/ProductSearchQuery.java`

- Purpose: offline contract and layers；public keywords/categoryId/brandId/minPrice/maxPrice/pageNo/pageSize/sort/specs; native/container constraints; no tenant field。
- Symbols: `ProductSearchQuery`及上列字段/方法；按主Spec§8/9/10和本轮offline边界实现。
- Repository evidence: 现同包Product/Ad Query/BO/Result/Controller、ProductCompositionValidator、AdPublicTenantFilter、G/lombok.config及有限MockMvc fixture；此文件operation已在§5核对。
- Dependencies and consumers: public HTTP -> Manage -> domain port -> infrastructure service -> optional repository；carrier与Mapper先于其消费者；复用当前Spring/Jakarta/Common/MapStruct，不引入client或新模块。
- Why now: 按上文编译/RED/生产/接线/回归顺序；前置metadata/types/port建立后本文件形成该职责的最小实现或验证。
- Contract/signature changes: public keywords/categoryId/brandId/minPrice/maxPrice/pageNo/pageSize/sort/specs; native/container constraints; no tenant field；涉及的公开边界是主Spec已批准API-001，只有精确search GET变化。
- Input/output and state mapping: §9字符串ID/整数分、1基窗口、spec同键OR异键AND、rows/facets不null；MDC绑定tenant/visible不可由client发送；所有source业务/DDL/MQ unchanged。
- Error and edge behavior: 必须覆盖invalid/missing tenant、duplicate params/单次decode、unknown sort、无adapter503、坏output不伪200和thread scope finally restoration；不自动retry或启动系统。
- Standards impact: MC-ARCH-001, MC-REUSE-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-SCOPE-001, MC-TEST-001 — 适用字段按§4.8落实；测试/YAML无注解要求，生产carrier与业务Bean必须遵守。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 本Java/配置涉及者按§4.8；测试仅其边界证明，不给不涉及的持久化/日期造实现。
- Implementation pseudocode:

```java
ProductSearchQuery: public keywords/categoryId/brandId/minPrice/maxPrice/pageNo/pageSize/sort/specs; native/container constraints; no tenant field
input -> exact Spec §9 fields / trusted context -> existing typed boundary; preserve null/default/precision semantics
failure -> SEARCH_REQUEST_INVALID(610401) / SITE(610402) / UNAVAILABLE(610403); no hidden SQL/MQ/network mutation
proof -> offline contract and layers; inspect generated MapStruct/Lombok contracts and focused positive/negative tests
```

- Verification contribution: offline contract and layers，§8列精确command；不把尚未写ES adapter当检索完成。
- After this file: 该职责可编译/可验证，后续consumer接线或最终focused GREEN仍在同Step内。

#### File 7 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/search/SearchCriteriaBO.java`

- Purpose: offline contract and layers；internal tenantId/visible plus public criteria, @ValidProductSearch/@AssertTrue, shared metadata; immutable canonical spec values and typed sort plan。
- Symbols: `SearchCriteriaBO`及上列字段/方法；按主Spec§8/9/10和本轮offline边界实现。
- Repository evidence: 现同包Product/Ad Query/BO/Result/Controller、ProductCompositionValidator、AdPublicTenantFilter、G/lombok.config及有限MockMvc fixture；此文件operation已在§5核对。
- Dependencies and consumers: public HTTP -> Manage -> domain port -> infrastructure service -> optional repository；carrier与Mapper先于其消费者；复用当前Spring/Jakarta/Common/MapStruct，不引入client或新模块。
- Why now: 按上文编译/RED/生产/接线/回归顺序；前置metadata/types/port建立后本文件形成该职责的最小实现或验证。
- Contract/signature changes: internal tenantId/visible plus public criteria, @ValidProductSearch/@AssertTrue, shared metadata; immutable canonical spec values and typed sort plan；涉及的公开边界是主Spec已批准API-001，只有精确search GET变化。
- Input/output and state mapping: §9字符串ID/整数分、1基窗口、spec同键OR异键AND、rows/facets不null；MDC绑定tenant/visible不可由client发送；所有source业务/DDL/MQ unchanged。
- Error and edge behavior: 必须覆盖invalid/missing tenant、duplicate params/单次decode、unknown sort、无adapter503、坏output不伪200和thread scope finally restoration；不自动retry或启动系统。
- Standards impact: MC-ARCH-001, MC-REUSE-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-SCOPE-001, MC-TEST-001 — 适用字段按§4.8落实；测试/YAML无注解要求，生产carrier与业务Bean必须遵守。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 本Java/配置涉及者按§4.8；测试仅其边界证明，不给不涉及的持久化/日期造实现。
- Implementation pseudocode:

```java
SearchCriteriaBO: internal tenantId/visible plus public criteria, @ValidProductSearch/@AssertTrue, shared metadata; immutable canonical spec values and typed sort plan
input -> exact Spec §9 fields / trusted context -> existing typed boundary; preserve null/default/precision semantics
failure -> SEARCH_REQUEST_INVALID(610401) / SITE(610402) / UNAVAILABLE(610403); no hidden SQL/MQ/network mutation
proof -> offline contract and layers; inspect generated MapStruct/Lombok contracts and focused positive/negative tests
```

- Verification contribution: offline contract and layers，§8列精确command；不把尚未写ES adapter当检索完成。
- After this file: 该职责可编译/可验证，后续consumer接线或最终focused GREEN仍在同Step内。

#### File 8 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/search/ProductSearchItemBO.java`

- Purpose: offline contract and layers；SKU id/spu/name/highlight/price/image/brand/category/specs, all required except highlight。
- Symbols: `ProductSearchItemBO`及上列字段/方法；按主Spec§8/9/10和本轮offline边界实现。
- Repository evidence: 现同包Product/Ad Query/BO/Result/Controller、ProductCompositionValidator、AdPublicTenantFilter、G/lombok.config及有限MockMvc fixture；此文件operation已在§5核对。
- Dependencies and consumers: public HTTP -> Manage -> domain port -> infrastructure service -> optional repository；carrier与Mapper先于其消费者；复用当前Spring/Jakarta/Common/MapStruct，不引入client或新模块。
- Why now: 按上文编译/RED/生产/接线/回归顺序；前置metadata/types/port建立后本文件形成该职责的最小实现或验证。
- Contract/signature changes: SKU id/spu/name/highlight/price/image/brand/category/specs, all required except highlight；涉及的公开边界是主Spec已批准API-001，只有精确search GET变化。
- Input/output and state mapping: §9字符串ID/整数分、1基窗口、spec同键OR异键AND、rows/facets不null；MDC绑定tenant/visible不可由client发送；所有source业务/DDL/MQ unchanged。
- Error and edge behavior: 必须覆盖invalid/missing tenant、duplicate params/单次decode、unknown sort、无adapter503、坏output不伪200和thread scope finally restoration；不自动retry或启动系统。
- Standards impact: MC-ARCH-001, MC-REUSE-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-SCOPE-001, MC-TEST-001 — 适用字段按§4.8落实；测试/YAML无注解要求，生产carrier与业务Bean必须遵守。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 本Java/配置涉及者按§4.8；测试仅其边界证明，不给不涉及的持久化/日期造实现。
- Implementation pseudocode:

```java
ProductSearchItemBO: SKU id/spu/name/highlight/price/image/brand/category/specs, all required except highlight
input -> exact Spec §9 fields / trusted context -> existing typed boundary; preserve null/default/precision semantics
failure -> SEARCH_REQUEST_INVALID(610401) / SITE(610402) / UNAVAILABLE(610403); no hidden SQL/MQ/network mutation
proof -> offline contract and layers; inspect generated MapStruct/Lombok contracts and focused positive/negative tests
```

- Verification contribution: offline contract and layers，§8列精确command；不把尚未写ES adapter当检索完成。
- After this file: 该职责可编译/可验证，后续consumer接线或最终focused GREEN仍在同Step内。

#### File 9 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/search/SearchFacetBucketBO.java`

- Purpose: offline contract and layers；key/label/count positive-or-zero, no internal tenant。
- Symbols: `SearchFacetBucketBO`及上列字段/方法；按主Spec§8/9/10和本轮offline边界实现。
- Repository evidence: 现同包Product/Ad Query/BO/Result/Controller、ProductCompositionValidator、AdPublicTenantFilter、G/lombok.config及有限MockMvc fixture；此文件operation已在§5核对。
- Dependencies and consumers: public HTTP -> Manage -> domain port -> infrastructure service -> optional repository；carrier与Mapper先于其消费者；复用当前Spring/Jakarta/Common/MapStruct，不引入client或新模块。
- Why now: 按上文编译/RED/生产/接线/回归顺序；前置metadata/types/port建立后本文件形成该职责的最小实现或验证。
- Contract/signature changes: key/label/count positive-or-zero, no internal tenant；涉及的公开边界是主Spec已批准API-001，只有精确search GET变化。
- Input/output and state mapping: §9字符串ID/整数分、1基窗口、spec同键OR异键AND、rows/facets不null；MDC绑定tenant/visible不可由client发送；所有source业务/DDL/MQ unchanged。
- Error and edge behavior: 必须覆盖invalid/missing tenant、duplicate params/单次decode、unknown sort、无adapter503、坏output不伪200和thread scope finally restoration；不自动retry或启动系统。
- Standards impact: MC-ARCH-001, MC-REUSE-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-SCOPE-001, MC-TEST-001 — 适用字段按§4.8落实；测试/YAML无注解要求，生产carrier与业务Bean必须遵守。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 本Java/配置涉及者按§4.8；测试仅其边界证明，不给不涉及的持久化/日期造实现。
- Implementation pseudocode:

```java
SearchFacetBucketBO: key/label/count positive-or-zero, no internal tenant
input -> exact Spec §9 fields / trusted context -> existing typed boundary; preserve null/default/precision semantics
failure -> SEARCH_REQUEST_INVALID(610401) / SITE(610402) / UNAVAILABLE(610403); no hidden SQL/MQ/network mutation
proof -> offline contract and layers; inspect generated MapStruct/Lombok contracts and focused positive/negative tests
```

- Verification contribution: offline contract and layers，§8列精确command；不把尚未写ES adapter当检索完成。
- After this file: 该职责可编译/可验证，后续consumer接线或最终focused GREEN仍在同Step内。

#### File 10 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/search/SearchFacetBO.java`

- Purpose: offline contract and layers；kind brand/category/spec, name/buckets/truncated with bounds。
- Symbols: `SearchFacetBO`及上列字段/方法；按主Spec§8/9/10和本轮offline边界实现。
- Repository evidence: 现同包Product/Ad Query/BO/Result/Controller、ProductCompositionValidator、AdPublicTenantFilter、G/lombok.config及有限MockMvc fixture；此文件operation已在§5核对。
- Dependencies and consumers: public HTTP -> Manage -> domain port -> infrastructure service -> optional repository；carrier与Mapper先于其消费者；复用当前Spring/Jakarta/Common/MapStruct，不引入client或新模块。
- Why now: 按上文编译/RED/生产/接线/回归顺序；前置metadata/types/port建立后本文件形成该职责的最小实现或验证。
- Contract/signature changes: kind brand/category/spec, name/buckets/truncated with bounds；涉及的公开边界是主Spec已批准API-001，只有精确search GET变化。
- Input/output and state mapping: §9字符串ID/整数分、1基窗口、spec同键OR异键AND、rows/facets不null；MDC绑定tenant/visible不可由client发送；所有source业务/DDL/MQ unchanged。
- Error and edge behavior: 必须覆盖invalid/missing tenant、duplicate params/单次decode、unknown sort、无adapter503、坏output不伪200和thread scope finally restoration；不自动retry或启动系统。
- Standards impact: MC-ARCH-001, MC-REUSE-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-SCOPE-001, MC-TEST-001 — 适用字段按§4.8落实；测试/YAML无注解要求，生产carrier与业务Bean必须遵守。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 本Java/配置涉及者按§4.8；测试仅其边界证明，不给不涉及的持久化/日期造实现。
- Implementation pseudocode:

```java
SearchFacetBO: kind brand/category/spec, name/buckets/truncated with bounds
input -> exact Spec §9 fields / trusted context -> existing typed boundary; preserve null/default/precision semantics
failure -> SEARCH_REQUEST_INVALID(610401) / SITE(610402) / UNAVAILABLE(610403); no hidden SQL/MQ/network mutation
proof -> offline contract and layers; inspect generated MapStruct/Lombok contracts and focused positive/negative tests
```

- Verification contribution: offline contract and layers，§8列精确command；不把尚未写ES adapter当检索完成。
- After this file: 该职责可编译/可验证，后续consumer接线或最终focused GREEN仍在同Step内。

#### File 11 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/search/ProductSearchPageBO.java`

- Purpose: offline contract and layers；items/total/pageNo/pageSize/facets/truncation, derived safe totals/window; output constraints。
- Symbols: `ProductSearchPageBO`及上列字段/方法；按主Spec§8/9/10和本轮offline边界实现。
- Repository evidence: 现同包Product/Ad Query/BO/Result/Controller、ProductCompositionValidator、AdPublicTenantFilter、G/lombok.config及有限MockMvc fixture；此文件operation已在§5核对。
- Dependencies and consumers: public HTTP -> Manage -> domain port -> infrastructure service -> optional repository；carrier与Mapper先于其消费者；复用当前Spring/Jakarta/Common/MapStruct，不引入client或新模块。
- Why now: 按上文编译/RED/生产/接线/回归顺序；前置metadata/types/port建立后本文件形成该职责的最小实现或验证。
- Contract/signature changes: items/total/pageNo/pageSize/facets/truncation, derived safe totals/window; output constraints；涉及的公开边界是主Spec已批准API-001，只有精确search GET变化。
- Input/output and state mapping: §9字符串ID/整数分、1基窗口、spec同键OR异键AND、rows/facets不null；MDC绑定tenant/visible不可由client发送；所有source业务/DDL/MQ unchanged。
- Error and edge behavior: 必须覆盖invalid/missing tenant、duplicate params/单次decode、unknown sort、无adapter503、坏output不伪200和thread scope finally restoration；不自动retry或启动系统。
- Standards impact: MC-ARCH-001, MC-REUSE-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-SCOPE-001, MC-TEST-001 — 适用字段按§4.8落实；测试/YAML无注解要求，生产carrier与业务Bean必须遵守。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 本Java/配置涉及者按§4.8；测试仅其边界证明，不给不涉及的持久化/日期造实现。
- Implementation pseudocode:

```java
ProductSearchPageBO: items/total/pageNo/pageSize/facets/truncation, derived safe totals/window; output constraints
input -> exact Spec §9 fields / trusted context -> existing typed boundary; preserve null/default/precision semantics
failure -> SEARCH_REQUEST_INVALID(610401) / SITE(610402) / UNAVAILABLE(610403); no hidden SQL/MQ/network mutation
proof -> offline contract and layers; inspect generated MapStruct/Lombok contracts and focused positive/negative tests
```

- Verification contribution: offline contract and layers，§8列精确command；不把尚未写ES adapter当检索完成。
- After this file: 该职责可编译/可验证，后续consumer接线或最终focused GREEN仍在同Step内。

#### File 12 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/search/ProductSearchItemResult.java`

- Purpose: offline contract and layers；public projection with string IDs, integer cents, always-present nullable safe highlight。
- Symbols: `ProductSearchItemResult`及上列字段/方法；按主Spec§8/9/10和本轮offline边界实现。
- Repository evidence: 现同包Product/Ad Query/BO/Result/Controller、ProductCompositionValidator、AdPublicTenantFilter、G/lombok.config及有限MockMvc fixture；此文件operation已在§5核对。
- Dependencies and consumers: public HTTP -> Manage -> domain port -> infrastructure service -> optional repository；carrier与Mapper先于其消费者；复用当前Spring/Jakarta/Common/MapStruct，不引入client或新模块。
- Why now: 按上文编译/RED/生产/接线/回归顺序；前置metadata/types/port建立后本文件形成该职责的最小实现或验证。
- Contract/signature changes: public projection with string IDs, integer cents, always-present nullable safe highlight；涉及的公开边界是主Spec已批准API-001，只有精确search GET变化。
- Input/output and state mapping: §9字符串ID/整数分、1基窗口、spec同键OR异键AND、rows/facets不null；MDC绑定tenant/visible不可由client发送；所有source业务/DDL/MQ unchanged。
- Error and edge behavior: 必须覆盖invalid/missing tenant、duplicate params/单次decode、unknown sort、无adapter503、坏output不伪200和thread scope finally restoration；不自动retry或启动系统。
- Standards impact: MC-ARCH-001, MC-REUSE-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-SCOPE-001, MC-TEST-001 — 适用字段按§4.8落实；测试/YAML无注解要求，生产carrier与业务Bean必须遵守。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 本Java/配置涉及者按§4.8；测试仅其边界证明，不给不涉及的持久化/日期造实现。
- Implementation pseudocode:

```java
ProductSearchItemResult: public projection with string IDs, integer cents, always-present nullable safe highlight
input -> exact Spec §9 fields / trusted context -> existing typed boundary; preserve null/default/precision semantics
failure -> SEARCH_REQUEST_INVALID(610401) / SITE(610402) / UNAVAILABLE(610403); no hidden SQL/MQ/network mutation
proof -> offline contract and layers; inspect generated MapStruct/Lombok contracts and focused positive/negative tests
```

- Verification contribution: offline contract and layers，§8列精确command；不把尚未写ES adapter当检索完成。
- After this file: 该职责可编译/可验证，后续consumer接线或最终focused GREEN仍在同Step内。

#### File 13 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/search/SearchFacetBucketResult.java`

- Purpose: offline contract and layers；public key/label/count。
- Symbols: `SearchFacetBucketResult`及上列字段/方法；按主Spec§8/9/10和本轮offline边界实现。
- Repository evidence: 现同包Product/Ad Query/BO/Result/Controller、ProductCompositionValidator、AdPublicTenantFilter、G/lombok.config及有限MockMvc fixture；此文件operation已在§5核对。
- Dependencies and consumers: public HTTP -> Manage -> domain port -> infrastructure service -> optional repository；carrier与Mapper先于其消费者；复用当前Spring/Jakarta/Common/MapStruct，不引入client或新模块。
- Why now: 按上文编译/RED/生产/接线/回归顺序；前置metadata/types/port建立后本文件形成该职责的最小实现或验证。
- Contract/signature changes: public key/label/count；涉及的公开边界是主Spec已批准API-001，只有精确search GET变化。
- Input/output and state mapping: §9字符串ID/整数分、1基窗口、spec同键OR异键AND、rows/facets不null；MDC绑定tenant/visible不可由client发送；所有source业务/DDL/MQ unchanged。
- Error and edge behavior: 必须覆盖invalid/missing tenant、duplicate params/单次decode、unknown sort、无adapter503、坏output不伪200和thread scope finally restoration；不自动retry或启动系统。
- Standards impact: MC-ARCH-001, MC-REUSE-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-SCOPE-001, MC-TEST-001 — 适用字段按§4.8落实；测试/YAML无注解要求，生产carrier与业务Bean必须遵守。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 本Java/配置涉及者按§4.8；测试仅其边界证明，不给不涉及的持久化/日期造实现。
- Implementation pseudocode:

```java
SearchFacetBucketResult: public key/label/count
input -> exact Spec §9 fields / trusted context -> existing typed boundary; preserve null/default/precision semantics
failure -> SEARCH_REQUEST_INVALID(610401) / SITE(610402) / UNAVAILABLE(610403); no hidden SQL/MQ/network mutation
proof -> offline contract and layers; inspect generated MapStruct/Lombok contracts and focused positive/negative tests
```

- Verification contribution: offline contract and layers，§8列精确command；不把尚未写ES adapter当检索完成。
- After this file: 该职责可编译/可验证，后续consumer接线或最终focused GREEN仍在同Step内。

#### File 14 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/search/SearchFacetResult.java`

- Purpose: offline contract and layers；public kind/name/buckets/truncated。
- Symbols: `SearchFacetResult`及上列字段/方法；按主Spec§8/9/10和本轮offline边界实现。
- Repository evidence: 现同包Product/Ad Query/BO/Result/Controller、ProductCompositionValidator、AdPublicTenantFilter、G/lombok.config及有限MockMvc fixture；此文件operation已在§5核对。
- Dependencies and consumers: public HTTP -> Manage -> domain port -> infrastructure service -> optional repository；carrier与Mapper先于其消费者；复用当前Spring/Jakarta/Common/MapStruct，不引入client或新模块。
- Why now: 按上文编译/RED/生产/接线/回归顺序；前置metadata/types/port建立后本文件形成该职责的最小实现或验证。
- Contract/signature changes: public kind/name/buckets/truncated；涉及的公开边界是主Spec已批准API-001，只有精确search GET变化。
- Input/output and state mapping: §9字符串ID/整数分、1基窗口、spec同键OR异键AND、rows/facets不null；MDC绑定tenant/visible不可由client发送；所有source业务/DDL/MQ unchanged。
- Error and edge behavior: 必须覆盖invalid/missing tenant、duplicate params/单次decode、unknown sort、无adapter503、坏output不伪200和thread scope finally restoration；不自动retry或启动系统。
- Standards impact: MC-ARCH-001, MC-REUSE-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-SCOPE-001, MC-TEST-001 — 适用字段按§4.8落实；测试/YAML无注解要求，生产carrier与业务Bean必须遵守。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 本Java/配置涉及者按§4.8；测试仅其边界证明，不给不涉及的持久化/日期造实现。
- Implementation pseudocode:

```java
SearchFacetResult: public kind/name/buckets/truncated
input -> exact Spec §9 fields / trusted context -> existing typed boundary; preserve null/default/precision semantics
failure -> SEARCH_REQUEST_INVALID(610401) / SITE(610402) / UNAVAILABLE(610403); no hidden SQL/MQ/network mutation
proof -> offline contract and layers; inspect generated MapStruct/Lombok contracts and focused positive/negative tests
```

- Verification contribution: offline contract and layers，§8列精确command；不把尚未写ES adapter当检索完成。
- After this file: 该职责可编译/可验证，后续consumer接线或最终focused GREEN仍在同Step内。

#### File 15 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/search/ProductSearchPageResult.java`

- Purpose: offline contract and layers；rows/count/page metadata/facets; ResultRecord owns outer seven fields。
- Symbols: `ProductSearchPageResult`及上列字段/方法；按主Spec§8/9/10和本轮offline边界实现。
- Repository evidence: 现同包Product/Ad Query/BO/Result/Controller、ProductCompositionValidator、AdPublicTenantFilter、G/lombok.config及有限MockMvc fixture；此文件operation已在§5核对。
- Dependencies and consumers: public HTTP -> Manage -> domain port -> infrastructure service -> optional repository；carrier与Mapper先于其消费者；复用当前Spring/Jakarta/Common/MapStruct，不引入client或新模块。
- Why now: 按上文编译/RED/生产/接线/回归顺序；前置metadata/types/port建立后本文件形成该职责的最小实现或验证。
- Contract/signature changes: rows/count/page metadata/facets; ResultRecord owns outer seven fields；涉及的公开边界是主Spec已批准API-001，只有精确search GET变化。
- Input/output and state mapping: §9字符串ID/整数分、1基窗口、spec同键OR异键AND、rows/facets不null；MDC绑定tenant/visible不可由client发送；所有source业务/DDL/MQ unchanged。
- Error and edge behavior: 必须覆盖invalid/missing tenant、duplicate params/单次decode、unknown sort、无adapter503、坏output不伪200和thread scope finally restoration；不自动retry或启动系统。
- Standards impact: MC-ARCH-001, MC-REUSE-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-SCOPE-001, MC-TEST-001 — 适用字段按§4.8落实；测试/YAML无注解要求，生产carrier与业务Bean必须遵守。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 本Java/配置涉及者按§4.8；测试仅其边界证明，不给不涉及的持久化/日期造实现。
- Implementation pseudocode:

```java
ProductSearchPageResult: rows/count/page metadata/facets; ResultRecord owns outer seven fields
input -> exact Spec §9 fields / trusted context -> existing typed boundary; preserve null/default/precision semantics
failure -> SEARCH_REQUEST_INVALID(610401) / SITE(610402) / UNAVAILABLE(610403); no hidden SQL/MQ/network mutation
proof -> offline contract and layers; inspect generated MapStruct/Lombok contracts and focused positive/negative tests
```

- Verification contribution: offline contract and layers，§8列精确command；不把尚未写ES adapter当检索完成。
- After this file: 该职责可编译/可验证，后续consumer接线或最终focused GREEN仍在同Step内。

#### File 16 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/search/ProductSearchRepository.java`

- Purpose: offline contract and layers；@Valid criteria read port only; no production adapter, fake dataset, SQL fallback, client or MQ listener。
- Symbols: `ProductSearchRepository`及上列字段/方法；按主Spec§8/9/10和本轮offline边界实现。
- Repository evidence: 现同包Product/Ad Query/BO/Result/Controller、ProductCompositionValidator、AdPublicTenantFilter、G/lombok.config及有限MockMvc fixture；此文件operation已在§5核对。
- Dependencies and consumers: public HTTP -> Manage -> domain port -> infrastructure service -> optional repository；carrier与Mapper先于其消费者；复用当前Spring/Jakarta/Common/MapStruct，不引入client或新模块。
- Why now: 按上文编译/RED/生产/接线/回归顺序；前置metadata/types/port建立后本文件形成该职责的最小实现或验证。
- Contract/signature changes: @Valid criteria read port only; no production adapter, fake dataset, SQL fallback, client or MQ listener；涉及的公开边界是主Spec已批准API-001，只有精确search GET变化。
- Input/output and state mapping: §9字符串ID/整数分、1基窗口、spec同键OR异键AND、rows/facets不null；MDC绑定tenant/visible不可由client发送；所有source业务/DDL/MQ unchanged。
- Error and edge behavior: 必须覆盖invalid/missing tenant、duplicate params/单次decode、unknown sort、无adapter503、坏output不伪200和thread scope finally restoration；不自动retry或启动系统。
- Standards impact: MC-ARCH-001, MC-REUSE-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-SCOPE-001, MC-TEST-001 — 适用字段按§4.8落实；测试/YAML无注解要求，生产carrier与业务Bean必须遵守。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 本Java/配置涉及者按§4.8；测试仅其边界证明，不给不涉及的持久化/日期造实现。
- Implementation pseudocode:

```java
ProductSearchRepository: @Valid criteria read port only; no production adapter, fake dataset, SQL fallback, client or MQ listener
input -> exact Spec §9 fields / trusted context -> existing typed boundary; preserve null/default/precision semantics
failure -> SEARCH_REQUEST_INVALID(610401) / SITE(610402) / UNAVAILABLE(610403); no hidden SQL/MQ/network mutation
proof -> offline contract and layers; inspect generated MapStruct/Lombok contracts and focused positive/negative tests
```

- Verification contribution: offline contract and layers，§8列精确command；不把尚未写ES adapter当检索完成。
- After this file: 该职责可编译/可验证，后续consumer接线或最终focused GREEN仍在同Step内。

#### File 17 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/search/ProductSearchDomainService.java`

- Purpose: offline contract and layers；pure domain @Valid/@NotNull internal criteria signature。
- Symbols: `ProductSearchDomainService`及上列字段/方法；按主Spec§8/9/10和本轮offline边界实现。
- Repository evidence: 现同包Product/Ad Query/BO/Result/Controller、ProductCompositionValidator、AdPublicTenantFilter、G/lombok.config及有限MockMvc fixture；此文件operation已在§5核对。
- Dependencies and consumers: public HTTP -> Manage -> domain port -> infrastructure service -> optional repository；carrier与Mapper先于其消费者；复用当前Spring/Jakarta/Common/MapStruct，不引入client或新模块。
- Why now: 按上文编译/RED/生产/接线/回归顺序；前置metadata/types/port建立后本文件形成该职责的最小实现或验证。
- Contract/signature changes: pure domain @Valid/@NotNull internal criteria signature；涉及的公开边界是主Spec已批准API-001，只有精确search GET变化。
- Input/output and state mapping: §9字符串ID/整数分、1基窗口、spec同键OR异键AND、rows/facets不null；MDC绑定tenant/visible不可由client发送；所有source业务/DDL/MQ unchanged。
- Error and edge behavior: 必须覆盖invalid/missing tenant、duplicate params/单次decode、unknown sort、无adapter503、坏output不伪200和thread scope finally restoration；不自动retry或启动系统。
- Standards impact: MC-ARCH-001, MC-REUSE-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-SCOPE-001, MC-TEST-001 — 适用字段按§4.8落实；测试/YAML无注解要求，生产carrier与业务Bean必须遵守。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 本Java/配置涉及者按§4.8；测试仅其边界证明，不给不涉及的持久化/日期造实现。
- Implementation pseudocode:

```java
ProductSearchDomainService: pure domain @Valid/@NotNull internal criteria signature
input -> exact Spec §9 fields / trusted context -> existing typed boundary; preserve null/default/precision semantics
failure -> SEARCH_REQUEST_INVALID(610401) / SITE(610402) / UNAVAILABLE(610403); no hidden SQL/MQ/network mutation
proof -> offline contract and layers; inspect generated MapStruct/Lombok contracts and focused positive/negative tests
```

- Verification contribution: offline contract and layers，§8列精确command；不把尚未写ES adapter当检索完成。
- After this file: 该职责可编译/可验证，后续consumer接线或最终focused GREEN仍在同Step内。

#### File 18 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/search/ProductSearchQueryConverter.java`

- Purpose: offline contract and layers；BaseForwardConverter Query->Criteria; @Context tenant from dedicated trusted MDC scope; canonical spec copies via named mapping。
- Symbols: `ProductSearchQueryConverter`及上列字段/方法；按主Spec§8/9/10和本轮offline边界实现。
- Repository evidence: 现同包Product/Ad Query/BO/Result/Controller、ProductCompositionValidator、AdPublicTenantFilter、G/lombok.config及有限MockMvc fixture；此文件operation已在§5核对。
- Dependencies and consumers: public HTTP -> Manage -> domain port -> infrastructure service -> optional repository；carrier与Mapper先于其消费者；复用当前Spring/Jakarta/Common/MapStruct，不引入client或新模块。
- Why now: 按上文编译/RED/生产/接线/回归顺序；前置metadata/types/port建立后本文件形成该职责的最小实现或验证。
- Contract/signature changes: BaseForwardConverter Query->Criteria; @Context tenant from dedicated trusted MDC scope; canonical spec copies via named mapping；涉及的公开边界是主Spec已批准API-001，只有精确search GET变化。
- Input/output and state mapping: §9字符串ID/整数分、1基窗口、spec同键OR异键AND、rows/facets不null；MDC绑定tenant/visible不可由client发送；所有source业务/DDL/MQ unchanged。
- Error and edge behavior: 必须覆盖invalid/missing tenant、duplicate params/单次decode、unknown sort、无adapter503、坏output不伪200和thread scope finally restoration；不自动retry或启动系统。
- Standards impact: MC-ARCH-001, MC-REUSE-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-SCOPE-001, MC-TEST-001 — 适用字段按§4.8落实；测试/YAML无注解要求，生产carrier与业务Bean必须遵守。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 本Java/配置涉及者按§4.8；测试仅其边界证明，不给不涉及的持久化/日期造实现。
- Implementation pseudocode:

```java
ProductSearchQueryConverter: BaseForwardConverter Query->Criteria; @Context tenant from dedicated trusted MDC scope; canonical spec copies via named mapping
input -> exact Spec §9 fields / trusted context -> existing typed boundary; preserve null/default/precision semantics
failure -> SEARCH_REQUEST_INVALID(610401) / SITE(610402) / UNAVAILABLE(610403); no hidden SQL/MQ/network mutation
proof -> offline contract and layers; inspect generated MapStruct/Lombok contracts and focused positive/negative tests
```

- Verification contribution: offline contract and layers，§8列精确command；不把尚未写ES adapter当检索完成。
- After this file: 该职责可编译/可验证，后续consumer接线或最终focused GREEN仍在同Step内。

#### File 19 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/search/ProductSearchResultConverter.java`

- Purpose: offline contract and layers；BaseForwardConverter PageBO->PageResult nested mappings; safe mark-only highlight; no sensitive fields。
- Symbols: `ProductSearchResultConverter`及上列字段/方法；按主Spec§8/9/10和本轮offline边界实现。
- Repository evidence: 现同包Product/Ad Query/BO/Result/Controller、ProductCompositionValidator、AdPublicTenantFilter、G/lombok.config及有限MockMvc fixture；此文件operation已在§5核对。
- Dependencies and consumers: public HTTP -> Manage -> domain port -> infrastructure service -> optional repository；carrier与Mapper先于其消费者；复用当前Spring/Jakarta/Common/MapStruct，不引入client或新模块。
- Why now: 按上文编译/RED/生产/接线/回归顺序；前置metadata/types/port建立后本文件形成该职责的最小实现或验证。
- Contract/signature changes: BaseForwardConverter PageBO->PageResult nested mappings; safe mark-only highlight; no sensitive fields；涉及的公开边界是主Spec已批准API-001，只有精确search GET变化。
- Input/output and state mapping: §9字符串ID/整数分、1基窗口、spec同键OR异键AND、rows/facets不null；MDC绑定tenant/visible不可由client发送；所有source业务/DDL/MQ unchanged。
- Error and edge behavior: 必须覆盖invalid/missing tenant、duplicate params/单次decode、unknown sort、无adapter503、坏output不伪200和thread scope finally restoration；不自动retry或启动系统。
- Standards impact: MC-ARCH-001, MC-REUSE-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-SCOPE-001, MC-TEST-001 — 适用字段按§4.8落实；测试/YAML无注解要求，生产carrier与业务Bean必须遵守。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 本Java/配置涉及者按§4.8；测试仅其边界证明，不给不涉及的持久化/日期造实现。
- Implementation pseudocode:

```java
ProductSearchResultConverter: BaseForwardConverter PageBO->PageResult nested mappings; safe mark-only highlight; no sensitive fields
input -> exact Spec §9 fields / trusted context -> existing typed boundary; preserve null/default/precision semantics
failure -> SEARCH_REQUEST_INVALID(610401) / SITE(610402) / UNAVAILABLE(610403); no hidden SQL/MQ/network mutation
proof -> offline contract and layers; inspect generated MapStruct/Lombok contracts and focused positive/negative tests
```

- Verification contribution: offline contract and layers，§8列精确command；不把尚未写ES adapter当检索完成。
- After this file: 该职责可编译/可验证，后续consumer接线或最终focused GREEN仍在同Step内。

#### File 20 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/search/ProductSearchManage.java`

- Purpose: offline contract and layers；@Validated PublicSearch input, explicit result signature。
- Symbols: `ProductSearchManage`及上列字段/方法；按主Spec§8/9/10和本轮offline边界实现。
- Repository evidence: 现同包Product/Ad Query/BO/Result/Controller、ProductCompositionValidator、AdPublicTenantFilter、G/lombok.config及有限MockMvc fixture；此文件operation已在§5核对。
- Dependencies and consumers: public HTTP -> Manage -> domain port -> infrastructure service -> optional repository；carrier与Mapper先于其消费者；复用当前Spring/Jakarta/Common/MapStruct，不引入client或新模块。
- Why now: 按上文编译/RED/生产/接线/回归顺序；前置metadata/types/port建立后本文件形成该职责的最小实现或验证。
- Contract/signature changes: @Validated PublicSearch input, explicit result signature；涉及的公开边界是主Spec已批准API-001，只有精确search GET变化。
- Input/output and state mapping: §9字符串ID/整数分、1基窗口、spec同键OR异键AND、rows/facets不null；MDC绑定tenant/visible不可由client发送；所有source业务/DDL/MQ unchanged。
- Error and edge behavior: 必须覆盖invalid/missing tenant、duplicate params/单次decode、unknown sort、无adapter503、坏output不伪200和thread scope finally restoration；不自动retry或启动系统。
- Standards impact: MC-ARCH-001, MC-REUSE-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-SCOPE-001, MC-TEST-001 — 适用字段按§4.8落实；测试/YAML无注解要求，生产carrier与业务Bean必须遵守。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 本Java/配置涉及者按§4.8；测试仅其边界证明，不给不涉及的持久化/日期造实现。
- Implementation pseudocode:

```java
ProductSearchManage: @Validated PublicSearch input, explicit result signature
input -> exact Spec §9 fields / trusted context -> existing typed boundary; preserve null/default/precision semantics
failure -> SEARCH_REQUEST_INVALID(610401) / SITE(610402) / UNAVAILABLE(610403); no hidden SQL/MQ/network mutation
proof -> offline contract and layers; inspect generated MapStruct/Lombok contracts and focused positive/negative tests
```

- Verification contribution: offline contract and layers，§8列精确command；不把尚未写ES adapter当检索完成。
- After this file: 该职责可编译/可验证，后续consumer接线或最终focused GREEN仍在同Step内。

#### File 21 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/search/ProductSearchManageImpl.java`

- Purpose: offline contract and layers；named service, qualified Lombok injection; common metadata -> converter -> domain -> result converter; no engine access。
- Symbols: `ProductSearchManageImpl`及上列字段/方法；按主Spec§8/9/10和本轮offline边界实现。
- Repository evidence: 现同包Product/Ad Query/BO/Result/Controller、ProductCompositionValidator、AdPublicTenantFilter、G/lombok.config及有限MockMvc fixture；此文件operation已在§5核对。
- Dependencies and consumers: public HTTP -> Manage -> domain port -> infrastructure service -> optional repository；carrier与Mapper先于其消费者；复用当前Spring/Jakarta/Common/MapStruct，不引入client或新模块。
- Why now: 按上文编译/RED/生产/接线/回归顺序；前置metadata/types/port建立后本文件形成该职责的最小实现或验证。
- Contract/signature changes: named service, qualified Lombok injection; common metadata -> converter -> domain -> result converter; no engine access；涉及的公开边界是主Spec已批准API-001，只有精确search GET变化。
- Input/output and state mapping: §9字符串ID/整数分、1基窗口、spec同键OR异键AND、rows/facets不null；MDC绑定tenant/visible不可由client发送；所有source业务/DDL/MQ unchanged。
- Error and edge behavior: 必须覆盖invalid/missing tenant、duplicate params/单次decode、unknown sort、无adapter503、坏output不伪200和thread scope finally restoration；不自动retry或启动系统。
- Standards impact: MC-ARCH-001, MC-REUSE-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-SCOPE-001, MC-TEST-001 — 适用字段按§4.8落实；测试/YAML无注解要求，生产carrier与业务Bean必须遵守。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 本Java/配置涉及者按§4.8；测试仅其边界证明，不给不涉及的持久化/日期造实现。
- Implementation pseudocode:

```java
ProductSearchManageImpl: named service, qualified Lombok injection; common metadata -> converter -> domain -> result converter; no engine access
input -> exact Spec §9 fields / trusted context -> existing typed boundary; preserve null/default/precision semantics
failure -> SEARCH_REQUEST_INVALID(610401) / SITE(610402) / UNAVAILABLE(610403); no hidden SQL/MQ/network mutation
proof -> offline contract and layers; inspect generated MapStruct/Lombok contracts and focused positive/negative tests
```

- Verification contribution: offline contract and layers，§8列精确command；不把尚未写ES adapter当检索完成。
- After this file: 该职责可编译/可验证，后续consumer接线或最终focused GREEN仍在同Step内。

#### File 22 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/config/ProductSearchProperties.java`

- Purpose: offline contract and layers；named validated properties app.search enabled=false/public-sites={}/query-timeout=PT2S; no endpoint/client provisioning。
- Symbols: `ProductSearchProperties`及上列字段/方法；按主Spec§8/9/10和本轮offline边界实现。
- Repository evidence: 现同包Product/Ad Query/BO/Result/Controller、ProductCompositionValidator、AdPublicTenantFilter、G/lombok.config及有限MockMvc fixture；此文件operation已在§5核对。
- Dependencies and consumers: public HTTP -> Manage -> domain port -> infrastructure service -> optional repository；carrier与Mapper先于其消费者；复用当前Spring/Jakarta/Common/MapStruct，不引入client或新模块。
- Why now: 按上文编译/RED/生产/接线/回归顺序；前置metadata/types/port建立后本文件形成该职责的最小实现或验证。
- Contract/signature changes: named validated properties app.search enabled=false/public-sites={}/query-timeout=PT2S; no endpoint/client provisioning；涉及的公开边界是主Spec已批准API-001，只有精确search GET变化。
- Input/output and state mapping: §9字符串ID/整数分、1基窗口、spec同键OR异键AND、rows/facets不null；MDC绑定tenant/visible不可由client发送；所有source业务/DDL/MQ unchanged。
- Error and edge behavior: 必须覆盖invalid/missing tenant、duplicate params/单次decode、unknown sort、无adapter503、坏output不伪200和thread scope finally restoration；不自动retry或启动系统。
- Standards impact: MC-ARCH-001, MC-REUSE-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-SCOPE-001, MC-TEST-001 — 适用字段按§4.8落实；测试/YAML无注解要求，生产carrier与业务Bean必须遵守。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 本Java/配置涉及者按§4.8；测试仅其边界证明，不给不涉及的持久化/日期造实现。
- Implementation pseudocode:

```java
ProductSearchProperties: named validated properties app.search enabled=false/public-sites={}/query-timeout=PT2S; no endpoint/client provisioning
input -> exact Spec §9 fields / trusted context -> existing typed boundary; preserve null/default/precision semantics
failure -> SEARCH_REQUEST_INVALID(610401) / SITE(610402) / UNAVAILABLE(610403); no hidden SQL/MQ/network mutation
proof -> offline contract and layers; inspect generated MapStruct/Lombok contracts and focused positive/negative tests
```

- Verification contribution: offline contract and layers，§8列精确command；不把尚未写ES adapter当检索完成。
- After this file: 该职责可编译/可验证，后续consumer接线或最终focused GREEN仍在同Step内。

#### File 23 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/search/service/impl/ProductSearchDomainServiceImpl.java`

- Purpose: offline contract and layers；validate internal criteria, obtain optional qualified elasticProductSearchRepository via ObjectProvider; absent/disabled ->610403; validate output and tenant scope; no fake search。
- Symbols: `ProductSearchDomainServiceImpl`及上列字段/方法；按主Spec§8/9/10和本轮offline边界实现。
- Repository evidence: 现同包Product/Ad Query/BO/Result/Controller、ProductCompositionValidator、AdPublicTenantFilter、G/lombok.config及有限MockMvc fixture；此文件operation已在§5核对。
- Dependencies and consumers: public HTTP -> Manage -> domain port -> infrastructure service -> optional repository；carrier与Mapper先于其消费者；复用当前Spring/Jakarta/Common/MapStruct，不引入client或新模块。
- Why now: 按上文编译/RED/生产/接线/回归顺序；前置metadata/types/port建立后本文件形成该职责的最小实现或验证。
- Contract/signature changes: validate internal criteria, obtain optional qualified elasticProductSearchRepository via ObjectProvider; absent/disabled ->610403; validate output and tenant scope; no fake search；涉及的公开边界是主Spec已批准API-001，只有精确search GET变化。
- Input/output and state mapping: §9字符串ID/整数分、1基窗口、spec同键OR异键AND、rows/facets不null；MDC绑定tenant/visible不可由client发送；所有source业务/DDL/MQ unchanged。
- Error and edge behavior: 必须覆盖invalid/missing tenant、duplicate params/单次decode、unknown sort、无adapter503、坏output不伪200和thread scope finally restoration；不自动retry或启动系统。
- Standards impact: MC-ARCH-001, MC-REUSE-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-SCOPE-001, MC-TEST-001 — 适用字段按§4.8落实；测试/YAML无注解要求，生产carrier与业务Bean必须遵守。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 本Java/配置涉及者按§4.8；测试仅其边界证明，不给不涉及的持久化/日期造实现。
- Implementation pseudocode:

```java
ProductSearchDomainServiceImpl: validate internal criteria, obtain optional qualified elasticProductSearchRepository via ObjectProvider; absent/disabled ->610403; validate output and tenant scope; no fake search
input -> exact Spec §9 fields / trusted context -> existing typed boundary; preserve null/default/precision semantics
failure -> SEARCH_REQUEST_INVALID(610401) / SITE(610402) / UNAVAILABLE(610403); no hidden SQL/MQ/network mutation
proof -> offline contract and layers; inspect generated MapStruct/Lombok contracts and focused positive/negative tests
```

- Verification contribution: offline contract and layers，§8列精确command；不把尚未写ES adapter当检索完成。
- After this file: 该职责可编译/可验证，后续consumer接线或最终focused GREEN仍在同Step内。

#### File 24 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/goods/ProductSearchController.java`

- Purpose: offline contract and layers；exact GET product-search, strict scalar/spec.* parsing once, validate query, call Manage; explicit OpenAPI operation and responses/security。
- Symbols: `ProductSearchController`及上列字段/方法；按主Spec§8/9/10和本轮offline边界实现。
- Repository evidence: 现同包Product/Ad Query/BO/Result/Controller、ProductCompositionValidator、AdPublicTenantFilter、G/lombok.config及有限MockMvc fixture；此文件operation已在§5核对。
- Dependencies and consumers: public HTTP -> Manage -> domain port -> infrastructure service -> optional repository；carrier与Mapper先于其消费者；复用当前Spring/Jakarta/Common/MapStruct，不引入client或新模块。
- Why now: 按上文编译/RED/生产/接线/回归顺序；前置metadata/types/port建立后本文件形成该职责的最小实现或验证。
- Contract/signature changes: exact GET product-search, strict scalar/spec.* parsing once, validate query, call Manage; explicit OpenAPI operation and responses/security；涉及的公开边界是主Spec已批准API-001，只有精确search GET变化。
- Input/output and state mapping: §9字符串ID/整数分、1基窗口、spec同键OR异键AND、rows/facets不null；MDC绑定tenant/visible不可由client发送；所有source业务/DDL/MQ unchanged。
- Error and edge behavior: 必须覆盖invalid/missing tenant、duplicate params/单次decode、unknown sort、无adapter503、坏output不伪200和thread scope finally restoration；不自动retry或启动系统。
- Standards impact: MC-ARCH-001, MC-REUSE-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-SCOPE-001, MC-TEST-001 — 适用字段按§4.8落实；测试/YAML无注解要求，生产carrier与业务Bean必须遵守。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 本Java/配置涉及者按§4.8；测试仅其边界证明，不给不涉及的持久化/日期造实现。
- Implementation pseudocode:

```java
ProductSearchController: exact GET product-search, strict scalar/spec.* parsing once, validate query, call Manage; explicit OpenAPI operation and responses/security
input -> exact Spec §9 fields / trusted context -> existing typed boundary; preserve null/default/precision semantics
failure -> SEARCH_REQUEST_INVALID(610401) / SITE(610402) / UNAVAILABLE(610403); no hidden SQL/MQ/network mutation
proof -> offline contract and layers; inspect generated MapStruct/Lombok contracts and focused positive/negative tests
```

- Verification contribution: offline contract and layers，§8列精确command；不把尚未写ES adapter当检索完成。
- After this file: 该职责可编译/可验证，后续consumer接线或最终focused GREEN仍在同Step内。

#### File 25 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/filter/ProductSearchTenantFilter.java`

- Purpose: offline contract and layers；exact public GET; enabled/site check, canonical host -> tenant, override trusted MDC keys, restore finally; no caller tenant or forwarded-host derivation。
- Symbols: `ProductSearchTenantFilter`及上列字段/方法；按主Spec§8/9/10和本轮offline边界实现。
- Repository evidence: 现同包Product/Ad Query/BO/Result/Controller、ProductCompositionValidator、AdPublicTenantFilter、G/lombok.config及有限MockMvc fixture；此文件operation已在§5核对。
- Dependencies and consumers: public HTTP -> Manage -> domain port -> infrastructure service -> optional repository；carrier与Mapper先于其消费者；复用当前Spring/Jakarta/Common/MapStruct，不引入client或新模块。
- Why now: 按上文编译/RED/生产/接线/回归顺序；前置metadata/types/port建立后本文件形成该职责的最小实现或验证。
- Contract/signature changes: exact public GET; enabled/site check, canonical host -> tenant, override trusted MDC keys, restore finally; no caller tenant or forwarded-host derivation；涉及的公开边界是主Spec已批准API-001，只有精确search GET变化。
- Input/output and state mapping: §9字符串ID/整数分、1基窗口、spec同键OR异键AND、rows/facets不null；MDC绑定tenant/visible不可由client发送；所有source业务/DDL/MQ unchanged。
- Error and edge behavior: 必须覆盖invalid/missing tenant、duplicate params/单次decode、unknown sort、无adapter503、坏output不伪200和thread scope finally restoration；不自动retry或启动系统。
- Standards impact: MC-ARCH-001, MC-REUSE-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-SCOPE-001, MC-TEST-001 — 适用字段按§4.8落实；测试/YAML无注解要求，生产carrier与业务Bean必须遵守。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 本Java/配置涉及者按§4.8；测试仅其边界证明，不给不涉及的持久化/日期造实现。
- Implementation pseudocode:

```java
ProductSearchTenantFilter: exact public GET; enabled/site check, canonical host -> tenant, override trusted MDC keys, restore finally; no caller tenant or forwarded-host derivation
input -> exact Spec §9 fields / trusted context -> existing typed boundary; preserve null/default/precision semantics
failure -> SEARCH_REQUEST_INVALID(610401) / SITE(610402) / UNAVAILABLE(610403); no hidden SQL/MQ/network mutation
proof -> offline contract and layers; inspect generated MapStruct/Lombok contracts and focused positive/negative tests
```

- Verification contribution: offline contract and layers，§8列精确command；不把尚未写ES adapter当检索完成。
- After this file: 该职责可编译/可验证，后续consumer接线或最终focused GREEN仍在同Step内。

#### File 26 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/enums/GoodsErrorStatus.java`

- Purpose: search protection；append 610401/402/403/499 only; preserve old codes。
- Symbols: `GoodsErrorStatus`及上列字段/方法；按主Spec§8/9/10和本轮offline边界实现。
- Repository evidence: 现同包Product/Ad Query/BO/Result/Controller、ProductCompositionValidator、AdPublicTenantFilter、G/lombok.config及有限MockMvc fixture；此文件operation已在§5核对。
- Dependencies and consumers: public HTTP -> Manage -> domain port -> infrastructure service -> optional repository；carrier与Mapper先于其消费者；复用当前Spring/Jakarta/Common/MapStruct，不引入client或新模块。
- Why now: 按上文编译/RED/生产/接线/回归顺序；前置metadata/types/port建立后本文件形成该职责的最小实现或验证。
- Contract/signature changes: append 610401/402/403/499 only; preserve old codes；涉及的公开边界是主Spec已批准API-001，只有精确search GET变化。
- Input/output and state mapping: §9字符串ID/整数分、1基窗口、spec同键OR异键AND、rows/facets不null；MDC绑定tenant/visible不可由client发送；所有source业务/DDL/MQ unchanged。
- Error and edge behavior: 必须覆盖invalid/missing tenant、duplicate params/单次decode、unknown sort、无adapter503、坏output不伪200和thread scope finally restoration；不自动retry或启动系统。
- Standards impact: MC-ARCH-001, MC-REUSE-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-SCOPE-001, MC-TEST-001 — 适用字段按§4.8落实；测试/YAML无注解要求，生产carrier与业务Bean必须遵守。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 本Java/配置涉及者按§4.8；测试仅其边界证明，不给不涉及的持久化/日期造实现。
- Implementation pseudocode:

```java
GoodsErrorStatus: append 610401/402/403/499 only; preserve old codes
input -> exact Spec §9 fields / trusted context -> existing typed boundary; preserve null/default/precision semantics
failure -> SEARCH_REQUEST_INVALID(610401) / SITE(610402) / UNAVAILABLE(610403); no hidden SQL/MQ/network mutation
proof -> search protection; inspect generated MapStruct/Lombok contracts and focused positive/negative tests
```

- Verification contribution: search protection，§8列精确command；不把尚未写ES adapter当检索完成。
- After this file: 该职责可编译/可验证，后续consumer接线或最终focused GREEN仍在同Step内。

#### File 27 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/handler/BaseExceptionHandler.java`

- Purpose: search protection；search-route error/field mapping without changing brand/product/ad branches。
- Symbols: `BaseExceptionHandler`及上列字段/方法；按主Spec§8/9/10和本轮offline边界实现。
- Repository evidence: 现同包Product/Ad Query/BO/Result/Controller、ProductCompositionValidator、AdPublicTenantFilter、G/lombok.config及有限MockMvc fixture；此文件operation已在§5核对。
- Dependencies and consumers: public HTTP -> Manage -> domain port -> infrastructure service -> optional repository；carrier与Mapper先于其消费者；复用当前Spring/Jakarta/Common/MapStruct，不引入client或新模块。
- Why now: 按上文编译/RED/生产/接线/回归顺序；前置metadata/types/port建立后本文件形成该职责的最小实现或验证。
- Contract/signature changes: search-route error/field mapping without changing brand/product/ad branches；涉及的公开边界是主Spec已批准API-001，只有精确search GET变化。
- Input/output and state mapping: §9字符串ID/整数分、1基窗口、spec同键OR异键AND、rows/facets不null；MDC绑定tenant/visible不可由client发送；所有source业务/DDL/MQ unchanged。
- Error and edge behavior: 必须覆盖invalid/missing tenant、duplicate params/单次decode、unknown sort、无adapter503、坏output不伪200和thread scope finally restoration；不自动retry或启动系统。
- Standards impact: MC-ARCH-001, MC-REUSE-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-SCOPE-001, MC-TEST-001 — 适用字段按§4.8落实；测试/YAML无注解要求，生产carrier与业务Bean必须遵守。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 本Java/配置涉及者按§4.8；测试仅其边界证明，不给不涉及的持久化/日期造实现。
- Implementation pseudocode:

```java
BaseExceptionHandler: search-route error/field mapping without changing brand/product/ad branches
input -> exact Spec §9 fields / trusted context -> existing typed boundary; preserve null/default/precision semantics
failure -> SEARCH_REQUEST_INVALID(610401) / SITE(610402) / UNAVAILABLE(610403); no hidden SQL/MQ/network mutation
proof -> search protection; inspect generated MapStruct/Lombok contracts and focused positive/negative tests
```

- Verification contribution: search protection，§8列精确command；不把尚未写ES adapter当检索完成。
- After this file: 该职责可编译/可验证，后续consumer接线或最终focused GREEN仍在同Step内。

#### File 28 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/filter/TenantContextFilter.java`

- Purpose: search protection；exclude search scope from legacy untrusted tenant fallback, preserve all old route rules。
- Symbols: `TenantContextFilter`及上列字段/方法；按主Spec§8/9/10和本轮offline边界实现。
- Repository evidence: 现同包Product/Ad Query/BO/Result/Controller、ProductCompositionValidator、AdPublicTenantFilter、G/lombok.config及有限MockMvc fixture；此文件operation已在§5核对。
- Dependencies and consumers: public HTTP -> Manage -> domain port -> infrastructure service -> optional repository；carrier与Mapper先于其消费者；复用当前Spring/Jakarta/Common/MapStruct，不引入client或新模块。
- Why now: 按上文编译/RED/生产/接线/回归顺序；前置metadata/types/port建立后本文件形成该职责的最小实现或验证。
- Contract/signature changes: exclude search scope from legacy untrusted tenant fallback, preserve all old route rules；涉及的公开边界是主Spec已批准API-001，只有精确search GET变化。
- Input/output and state mapping: §9字符串ID/整数分、1基窗口、spec同键OR异键AND、rows/facets不null；MDC绑定tenant/visible不可由client发送；所有source业务/DDL/MQ unchanged。
- Error and edge behavior: 必须覆盖invalid/missing tenant、duplicate params/单次decode、unknown sort、无adapter503、坏output不伪200和thread scope finally restoration；不自动retry或启动系统。
- Standards impact: MC-ARCH-001, MC-REUSE-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-SCOPE-001, MC-TEST-001 — 适用字段按§4.8落实；测试/YAML无注解要求，生产carrier与业务Bean必须遵守。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 本Java/配置涉及者按§4.8；测试仅其边界证明，不给不涉及的持久化/日期造实现。
- Implementation pseudocode:

```java
TenantContextFilter: exclude search scope from legacy untrusted tenant fallback, preserve all old route rules
input -> exact Spec §9 fields / trusted context -> existing typed boundary; preserve null/default/precision semantics
failure -> SEARCH_REQUEST_INVALID(610401) / SITE(610402) / UNAVAILABLE(610403); no hidden SQL/MQ/network mutation
proof -> search protection; inspect generated MapStruct/Lombok contracts and focused positive/negative tests
```

- Verification contribution: search protection，§8列精确command；不把尚未写ES adapter当检索完成。
- After this file: 该职责可编译/可验证，后续consumer接线或最终focused GREEN仍在同Step内。

#### File 29 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/start/config/NativeHttpSecurityConfiguration.java`

- Purpose: search protection；dedicated exact GET search chain; deny other methods/subpaths; existing management chain untouched。
- Symbols: `NativeHttpSecurityConfiguration`及上列字段/方法；按主Spec§8/9/10和本轮offline边界实现。
- Repository evidence: 现同包Product/Ad Query/BO/Result/Controller、ProductCompositionValidator、AdPublicTenantFilter、G/lombok.config及有限MockMvc fixture；此文件operation已在§5核对。
- Dependencies and consumers: public HTTP -> Manage -> domain port -> infrastructure service -> optional repository；carrier与Mapper先于其消费者；复用当前Spring/Jakarta/Common/MapStruct，不引入client或新模块。
- Why now: 按上文编译/RED/生产/接线/回归顺序；前置metadata/types/port建立后本文件形成该职责的最小实现或验证。
- Contract/signature changes: dedicated exact GET search chain; deny other methods/subpaths; existing management chain untouched；涉及的公开边界是主Spec已批准API-001，只有精确search GET变化。
- Input/output and state mapping: §9字符串ID/整数分、1基窗口、spec同键OR异键AND、rows/facets不null；MDC绑定tenant/visible不可由client发送；所有source业务/DDL/MQ unchanged。
- Error and edge behavior: 必须覆盖invalid/missing tenant、duplicate params/单次decode、unknown sort、无adapter503、坏output不伪200和thread scope finally restoration；不自动retry或启动系统。
- Standards impact: MC-ARCH-001, MC-REUSE-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-SCOPE-001, MC-TEST-001 — 适用字段按§4.8落实；测试/YAML无注解要求，生产carrier与业务Bean必须遵守。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 本Java/配置涉及者按§4.8；测试仅其边界证明，不给不涉及的持久化/日期造实现。
- Implementation pseudocode:

```java
NativeHttpSecurityConfiguration: dedicated exact GET search chain; deny other methods/subpaths; existing management chain untouched
input -> exact Spec §9 fields / trusted context -> existing typed boundary; preserve null/default/precision semantics
failure -> SEARCH_REQUEST_INVALID(610401) / SITE(610402) / UNAVAILABLE(610403); no hidden SQL/MQ/network mutation
proof -> search protection; inspect generated MapStruct/Lombok contracts and focused positive/negative tests
```

- Verification contribution: search protection，§8列精确command；不把尚未写ES adapter当检索完成。
- After this file: 该职责可编译/可验证，后续consumer接线或最终focused GREEN仍在同Step内。

#### File 30 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/resources/application.yml`

- Purpose: profile parity；add same app.search enabled=false/public-sites={}/query-timeout=PT2S; no ES endpoint/secret/config provisioning。
- Symbols: `application`及上列字段/方法；按主Spec§8/9/10和本轮offline边界实现。
- Repository evidence: 现同包Product/Ad Query/BO/Result/Controller、ProductCompositionValidator、AdPublicTenantFilter、G/lombok.config及有限MockMvc fixture；此文件operation已在§5核对。
- Dependencies and consumers: public HTTP -> Manage -> domain port -> infrastructure service -> optional repository；carrier与Mapper先于其消费者；复用当前Spring/Jakarta/Common/MapStruct，不引入client或新模块。
- Why now: 按上文编译/RED/生产/接线/回归顺序；前置metadata/types/port建立后本文件形成该职责的最小实现或验证。
- Contract/signature changes: add same app.search enabled=false/public-sites={}/query-timeout=PT2S; no ES endpoint/secret/config provisioning；涉及的公开边界是主Spec已批准API-001，只有精确search GET变化。
- Input/output and state mapping: §9字符串ID/整数分、1基窗口、spec同键OR异键AND、rows/facets不null；MDC绑定tenant/visible不可由client发送；所有source业务/DDL/MQ unchanged。
- Error and edge behavior: 必须覆盖invalid/missing tenant、duplicate params/单次decode、unknown sort、无adapter503、坏output不伪200和thread scope finally restoration；不自动retry或启动系统。
- Standards impact: MC-ARCH-001, MC-REUSE-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-SCOPE-001, MC-TEST-001 — 适用字段按§4.8落实；测试/YAML无注解要求，生产carrier与业务Bean必须遵守。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 本Java/配置涉及者按§4.8；测试仅其边界证明，不给不涉及的持久化/日期造实现。
- Implementation pseudocode:

```yaml
app.search.enabled = false
app.search.public-sites = {}
app.search.query-timeout = PT2S
Keep all existing profile keys/values; do not add endpoints, secrets or client provisioning
```

- Verification contribution: profile parity，§8列精确command；不把尚未写ES adapter当检索完成。
- After this file: 该职责可编译/可验证，后续consumer接线或最终focused GREEN仍在同Step内。

#### File 31 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-dev.yml`

- Purpose: profile parity；add same app.search enabled=false/public-sites={}/query-timeout=PT2S; no ES endpoint/secret/config provisioning。
- Symbols: `application-dev`及上列字段/方法；按主Spec§8/9/10和本轮offline边界实现。
- Repository evidence: 现同包Product/Ad Query/BO/Result/Controller、ProductCompositionValidator、AdPublicTenantFilter、G/lombok.config及有限MockMvc fixture；此文件operation已在§5核对。
- Dependencies and consumers: public HTTP -> Manage -> domain port -> infrastructure service -> optional repository；carrier与Mapper先于其消费者；复用当前Spring/Jakarta/Common/MapStruct，不引入client或新模块。
- Why now: 按上文编译/RED/生产/接线/回归顺序；前置metadata/types/port建立后本文件形成该职责的最小实现或验证。
- Contract/signature changes: add same app.search enabled=false/public-sites={}/query-timeout=PT2S; no ES endpoint/secret/config provisioning；涉及的公开边界是主Spec已批准API-001，只有精确search GET变化。
- Input/output and state mapping: §9字符串ID/整数分、1基窗口、spec同键OR异键AND、rows/facets不null；MDC绑定tenant/visible不可由client发送；所有source业务/DDL/MQ unchanged。
- Error and edge behavior: 必须覆盖invalid/missing tenant、duplicate params/单次decode、unknown sort、无adapter503、坏output不伪200和thread scope finally restoration；不自动retry或启动系统。
- Standards impact: MC-ARCH-001, MC-REUSE-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-SCOPE-001, MC-TEST-001 — 适用字段按§4.8落实；测试/YAML无注解要求，生产carrier与业务Bean必须遵守。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 本Java/配置涉及者按§4.8；测试仅其边界证明，不给不涉及的持久化/日期造实现。
- Implementation pseudocode:

```yaml
app.search.enabled = false
app.search.public-sites = {}
app.search.query-timeout = PT2S
Keep all existing profile keys/values; do not add endpoints, secrets or client provisioning
```

- Verification contribution: profile parity，§8列精确command；不把尚未写ES adapter当检索完成。
- After this file: 该职责可编译/可验证，后续consumer接线或最终focused GREEN仍在同Step内。

#### File 32 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-test.yml`

- Purpose: profile parity；add same app.search enabled=false/public-sites={}/query-timeout=PT2S; no ES endpoint/secret/config provisioning。
- Symbols: `application-test`及上列字段/方法；按主Spec§8/9/10和本轮offline边界实现。
- Repository evidence: 现同包Product/Ad Query/BO/Result/Controller、ProductCompositionValidator、AdPublicTenantFilter、G/lombok.config及有限MockMvc fixture；此文件operation已在§5核对。
- Dependencies and consumers: public HTTP -> Manage -> domain port -> infrastructure service -> optional repository；carrier与Mapper先于其消费者；复用当前Spring/Jakarta/Common/MapStruct，不引入client或新模块。
- Why now: 按上文编译/RED/生产/接线/回归顺序；前置metadata/types/port建立后本文件形成该职责的最小实现或验证。
- Contract/signature changes: add same app.search enabled=false/public-sites={}/query-timeout=PT2S; no ES endpoint/secret/config provisioning；涉及的公开边界是主Spec已批准API-001，只有精确search GET变化。
- Input/output and state mapping: §9字符串ID/整数分、1基窗口、spec同键OR异键AND、rows/facets不null；MDC绑定tenant/visible不可由client发送；所有source业务/DDL/MQ unchanged。
- Error and edge behavior: 必须覆盖invalid/missing tenant、duplicate params/单次decode、unknown sort、无adapter503、坏output不伪200和thread scope finally restoration；不自动retry或启动系统。
- Standards impact: MC-ARCH-001, MC-REUSE-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-SCOPE-001, MC-TEST-001 — 适用字段按§4.8落实；测试/YAML无注解要求，生产carrier与业务Bean必须遵守。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 本Java/配置涉及者按§4.8；测试仅其边界证明，不给不涉及的持久化/日期造实现。
- Implementation pseudocode:

```yaml
app.search.enabled = false
app.search.public-sites = {}
app.search.query-timeout = PT2S
Keep all existing profile keys/values; do not add endpoints, secrets or client provisioning
```

- Verification contribution: profile parity，§8列精确command；不把尚未写ES adapter当检索完成。
- After this file: 该职责可编译/可验证，后续consumer接线或最终focused GREEN仍在同Step内。

#### File 33 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-prod.yml`

- Purpose: profile parity；add same app.search enabled=false/public-sites={}/query-timeout=PT2S; no ES endpoint/secret/config provisioning。
- Symbols: `application-prod`及上列字段/方法；按主Spec§8/9/10和本轮offline边界实现。
- Repository evidence: 现同包Product/Ad Query/BO/Result/Controller、ProductCompositionValidator、AdPublicTenantFilter、G/lombok.config及有限MockMvc fixture；此文件operation已在§5核对。
- Dependencies and consumers: public HTTP -> Manage -> domain port -> infrastructure service -> optional repository；carrier与Mapper先于其消费者；复用当前Spring/Jakarta/Common/MapStruct，不引入client或新模块。
- Why now: 按上文编译/RED/生产/接线/回归顺序；前置metadata/types/port建立后本文件形成该职责的最小实现或验证。
- Contract/signature changes: add same app.search enabled=false/public-sites={}/query-timeout=PT2S; no ES endpoint/secret/config provisioning；涉及的公开边界是主Spec已批准API-001，只有精确search GET变化。
- Input/output and state mapping: §9字符串ID/整数分、1基窗口、spec同键OR异键AND、rows/facets不null；MDC绑定tenant/visible不可由client发送；所有source业务/DDL/MQ unchanged。
- Error and edge behavior: 必须覆盖invalid/missing tenant、duplicate params/单次decode、unknown sort、无adapter503、坏output不伪200和thread scope finally restoration；不自动retry或启动系统。
- Standards impact: MC-ARCH-001, MC-REUSE-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-SCOPE-001, MC-TEST-001 — 适用字段按§4.8落实；测试/YAML无注解要求，生产carrier与业务Bean必须遵守。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 本Java/配置涉及者按§4.8；测试仅其边界证明，不给不涉及的持久化/日期造实现。
- Implementation pseudocode:

```yaml
app.search.enabled = false
app.search.public-sites = {}
app.search.query-timeout = PT2S
Keep all existing profile keys/values; do not add endpoints, secrets or client provisioning
```

- Verification contribution: profile parity，§8列精确command；不把尚未写ES adapter当检索完成。
- After this file: 该职责可编译/可验证，后续consumer接线或最终focused GREEN仍在同Step内。

#### File 34 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/application/search/ProductSearchValidationTest.java`

- Purpose: focused proof；Validator + MapStruct input/price/spec/copy/sort/response/security encoding tests。
- Symbols: `ProductSearchValidationTest`及上列字段/方法；按主Spec§8/9/10和本轮offline边界实现。
- Repository evidence: 现同包Product/Ad Query/BO/Result/Controller、ProductCompositionValidator、AdPublicTenantFilter、G/lombok.config及有限MockMvc fixture；此文件operation已在§5核对。
- Dependencies and consumers: public HTTP -> Manage -> domain port -> infrastructure service -> optional repository；carrier与Mapper先于其消费者；复用当前Spring/Jakarta/Common/MapStruct，不引入client或新模块。
- Why now: 按上文编译/RED/生产/接线/回归顺序；前置metadata/types/port建立后本文件形成该职责的最小实现或验证。
- Contract/signature changes: Validator + MapStruct input/price/spec/copy/sort/response/security encoding tests；涉及的公开边界是主Spec已批准API-001，只有精确search GET变化。
- Input/output and state mapping: §9字符串ID/整数分、1基窗口、spec同键OR异键AND、rows/facets不null；MDC绑定tenant/visible不可由client发送；所有source业务/DDL/MQ unchanged。
- Error and edge behavior: 必须覆盖invalid/missing tenant、duplicate params/单次decode、unknown sort、无adapter503、坏output不伪200和thread scope finally restoration；不自动retry或启动系统。
- Standards impact: MC-ARCH-001, MC-REUSE-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-SCOPE-001, MC-TEST-001 — 适用字段按§4.8落实；测试/YAML无注解要求，生产carrier与业务Bean必须遵守。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 本Java/配置涉及者按§4.8；测试仅其边界证明，不给不涉及的持久化/日期造实现。
- Implementation pseudocode:

```java
ProductSearchValidationTest: Validator + MapStruct input/price/spec/copy/sort/response/security encoding tests
input -> exact Spec §9 fields / trusted context -> existing typed boundary; preserve null/default/precision semantics
failure -> SEARCH_REQUEST_INVALID(610401) / SITE(610402) / UNAVAILABLE(610403); no hidden SQL/MQ/network mutation
proof -> focused proof; inspect generated MapStruct/Lombok contracts and focused positive/negative tests
```

- Verification contribution: focused proof，§8列精确command；不把尚未写ES adapter当检索完成。
- After this file: 该职责可编译/可验证，后续consumer接线或最终focused GREEN仍在同Step内。

#### File 35 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/search/ProductSearchBoundaryTest.java`

- Purpose: focused proof；method validation/group/tenant mismatch/missing provider/output corruption and test-only port result projection。
- Symbols: `ProductSearchBoundaryTest`及上列字段/方法；按主Spec§8/9/10和本轮offline边界实现。
- Repository evidence: 现同包Product/Ad Query/BO/Result/Controller、ProductCompositionValidator、AdPublicTenantFilter、G/lombok.config及有限MockMvc fixture；此文件operation已在§5核对。
- Dependencies and consumers: public HTTP -> Manage -> domain port -> infrastructure service -> optional repository；carrier与Mapper先于其消费者；复用当前Spring/Jakarta/Common/MapStruct，不引入client或新模块。
- Why now: 按上文编译/RED/生产/接线/回归顺序；前置metadata/types/port建立后本文件形成该职责的最小实现或验证。
- Contract/signature changes: method validation/group/tenant mismatch/missing provider/output corruption and test-only port result projection；涉及的公开边界是主Spec已批准API-001，只有精确search GET变化。
- Input/output and state mapping: §9字符串ID/整数分、1基窗口、spec同键OR异键AND、rows/facets不null；MDC绑定tenant/visible不可由client发送；所有source业务/DDL/MQ unchanged。
- Error and edge behavior: 必须覆盖invalid/missing tenant、duplicate params/单次decode、unknown sort、无adapter503、坏output不伪200和thread scope finally restoration；不自动retry或启动系统。
- Standards impact: MC-ARCH-001, MC-REUSE-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-SCOPE-001, MC-TEST-001 — 适用字段按§4.8落实；测试/YAML无注解要求，生产carrier与业务Bean必须遵守。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 本Java/配置涉及者按§4.8；测试仅其边界证明，不给不涉及的持久化/日期造实现。
- Implementation pseudocode:

```java
ProductSearchBoundaryTest: method validation/group/tenant mismatch/missing provider/output corruption and test-only port result projection
input -> exact Spec §9 fields / trusted context -> existing typed boundary; preserve null/default/precision semantics
failure -> SEARCH_REQUEST_INVALID(610401) / SITE(610402) / UNAVAILABLE(610403); no hidden SQL/MQ/network mutation
proof -> focused proof; inspect generated MapStruct/Lombok contracts and focused positive/negative tests
```

- Verification contribution: focused proof，§8列精确command；不把尚未写ES adapter当检索完成。
- After this file: 该职责可编译/可验证，后续consumer接线或最终focused GREEN仍在同Step内。

#### File 36 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/goods/ProductSearchOpenApiContractTest.java`

- Purpose: focused proof；MockMvc real OAS search operation, query/response/errors/security=[] and old management behavior。
- Symbols: `ProductSearchOpenApiContractTest`及上列字段/方法；按主Spec§8/9/10和本轮offline边界实现。
- Repository evidence: 现同包Product/Ad Query/BO/Result/Controller、ProductCompositionValidator、AdPublicTenantFilter、G/lombok.config及有限MockMvc fixture；此文件operation已在§5核对。
- Dependencies and consumers: public HTTP -> Manage -> domain port -> infrastructure service -> optional repository；carrier与Mapper先于其消费者；复用当前Spring/Jakarta/Common/MapStruct，不引入client或新模块。
- Why now: 按上文编译/RED/生产/接线/回归顺序；前置metadata/types/port建立后本文件形成该职责的最小实现或验证。
- Contract/signature changes: MockMvc real OAS search operation, query/response/errors/security=[] and old management behavior；涉及的公开边界是主Spec已批准API-001，只有精确search GET变化。
- Input/output and state mapping: §9字符串ID/整数分、1基窗口、spec同键OR异键AND、rows/facets不null；MDC绑定tenant/visible不可由client发送；所有source业务/DDL/MQ unchanged。
- Error and edge behavior: 必须覆盖invalid/missing tenant、duplicate params/单次decode、unknown sort、无adapter503、坏output不伪200和thread scope finally restoration；不自动retry或启动系统。
- Standards impact: MC-ARCH-001, MC-REUSE-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-SCOPE-001, MC-TEST-001 — 适用字段按§4.8落实；测试/YAML无注解要求，生产carrier与业务Bean必须遵守。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 本Java/配置涉及者按§4.8；测试仅其边界证明，不给不涉及的持久化/日期造实现。
- Implementation pseudocode:

```java
ProductSearchOpenApiContractTest: MockMvc real OAS search operation, query/response/errors/security=[] and old management behavior
input -> exact Spec §9 fields / trusted context -> existing typed boundary; preserve null/default/precision semantics
failure -> SEARCH_REQUEST_INVALID(610401) / SITE(610402) / UNAVAILABLE(610403); no hidden SQL/MQ/network mutation
proof -> focused proof; inspect generated MapStruct/Lombok contracts and focused positive/negative tests
```

- Verification contribution: focused proof，§8列精确command；不把尚未写ES adapter当检索完成。
- After this file: 该职责可编译/可验证，后续consumer接线或最终focused GREEN仍在同Step内。

#### File 37 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/architecture/LightPersistenceArchitectureTest.java`

- Purpose: Light preservation；account for one new domain Repository port and one DomainServiceImpl; assert pure domain and no real ES client/listener/POM dependency。
- Symbols: `LightPersistenceArchitectureTest`及上列字段/方法；按主Spec§8/9/10和本轮offline边界实现。
- Repository evidence: 现同包Product/Ad Query/BO/Result/Controller、ProductCompositionValidator、AdPublicTenantFilter、G/lombok.config及有限MockMvc fixture；此文件operation已在§5核对。
- Dependencies and consumers: public HTTP -> Manage -> domain port -> infrastructure service -> optional repository；carrier与Mapper先于其消费者；复用当前Spring/Jakarta/Common/MapStruct，不引入client或新模块。
- Why now: 按上文编译/RED/生产/接线/回归顺序；前置metadata/types/port建立后本文件形成该职责的最小实现或验证。
- Contract/signature changes: account for one new domain Repository port and one DomainServiceImpl; assert pure domain and no real ES client/listener/POM dependency；涉及的公开边界是主Spec已批准API-001，只有精确search GET变化。
- Input/output and state mapping: §9字符串ID/整数分、1基窗口、spec同键OR异键AND、rows/facets不null；MDC绑定tenant/visible不可由client发送；所有source业务/DDL/MQ unchanged。
- Error and edge behavior: 必须覆盖invalid/missing tenant、duplicate params/单次decode、unknown sort、无adapter503、坏output不伪200和thread scope finally restoration；不自动retry或启动系统。
- Standards impact: MC-ARCH-001, MC-REUSE-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-BEAN-001, MC-SCOPE-001, MC-TEST-001 — 适用字段按§4.8落实；测试/YAML无注解要求，生产carrier与业务Bean必须遵守。
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11 — 本Java/配置涉及者按§4.8；测试仅其边界证明，不给不涉及的持久化/日期造实现。
- Implementation pseudocode:

```java
LightPersistenceArchitectureTest: account for one new domain Repository port and one DomainServiceImpl; assert pure domain and no real ES client/listener/POM dependency
input -> exact Spec §9 fields / trusted context -> existing typed boundary; preserve null/default/precision semantics
failure -> SEARCH_REQUEST_INVALID(610401) / SITE(610402) / UNAVAILABLE(610403); no hidden SQL/MQ/network mutation
proof -> Light preservation; inspect generated MapStruct/Lombok contracts and focused positive/negative tests
```

- Verification contribution: Light preservation，§8列精确command；不把尚未写ES adapter当检索完成。
- After this file: 该职责可编译/可验证，后续consumer接线或最终focused GREEN仍在同Step内。

#### File 38 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/goods/ProductSearchOpenApiConfiguration.java`

- Purpose: 补全真实泛型validation/business错误schema，修正Raw ResultRecord丢失FieldErrorsVO的工具表达缺口。
- Symbols: productSearchErrorSchemas OpenApiCustomizer；SearchRequestError/SearchBusinessError，Controller error refs。
- Repository evidence: 现AdOpenApiConfiguration通过ModelConverters+TypeReference解析实际ResultRecord<FieldErrorsVO>与ResultRecord<Void>；当前原Raw ResultRecord schema不能给出fieldErrors。
- Dependencies and consumers: 只现Swagger/springdoc/Jackson/common-core；只存在search operation的OAS audience登记components，不暴露其它内部模型。
- Why now: OAS test先RED证明擦除，最后修metadata+Controller refs，不新增任何API/业务行为。
- Contract/signature changes: 400 schema为实际typed wrapper、其他错误为Void wrapper；runtime不变。
- Input/output and state mapping: ResultRecord字段与FieldErrorsVO/FieldViolationVO ->真实components；引用生成的generic schema，无fake wrapper subtype。
- Error and edge behavior: 非search audience不登记；未有paths时跳过；不更改管理bearer或其它operation。
- Standards impact: MC-REUSE-001, MC-JSON-001, MC-SCOPE-001, MC-TEST-001 — 复用当前ModelConverters和现注册风格，用fresh生成OAS验证。
- Literal rule enforcement: Rule 1, Rule 4, Rule 6, Rule 11 — Configuration行为suffix、显式Bean/Slf4j、Jackson TypeReference与实际框架、保持Adapter位置。
- Implementation pseudocode:

```java
@Configuration("productSearchOpenApiConfiguration") @Slf4j -> @Bean("productSearchErrorSchemas")
OpenApiCustomizer only when api.paths contains /api/v1/product-search
ModelConverters.resolve(TypeReference<ResultRecord<FieldErrorsVO>>) -> components.SearchRequestError
ModelConverters.resolve(TypeReference<ResultRecord<Void>>) -> components.SearchBusinessError
Controller.ApiResponses use these exact component refs; no response subclasses or extra endpoint
```

- Verification contribution: ProductSearchOpenApiContractTest reads actual /v3/api-docs and asserts refs/payload/wrapper/operation security。
- After this file: OAS语义完整且同runtime，最终focused/regression GREEN后单提交。

- Validation working directory: `/Users/mario/SelfProject/enjoyshop`，Maven `-f enjoyshop-service/enjoyshop-service-goods/pom.xml`；tests读资源时cwd用goods模块。
- Verification command: `mvn -o -DargLine=-javaagent:/Users/mario/maven/repository/net/bytebuddy/byte-buddy-agent/1.17.8/byte-buddy-agent-1.17.8.jar -f enjoyshop-service/enjoyshop-service-goods/pom.xml -Dtest=ProductSearchControllerWebTest,ProductSearchValidationTest,ProductSearchBoundaryTest,ProductSearchOpenApiContractTest,LightPersistenceArchitectureTest,BaseExceptionHandlerTest,ProductControllerWebTest,ProductOpenApiContractTest,AdOpenApiContractTest test`；另最终architecture plugin verify与docs validator见§8。
- Expected result: RED是route404不满足契约；GREEN指定tests无fail/error、当前source compile和新搜索scope architecture findings=0；完整bytecode verify如原HEAD存在违规，须fresh baseline report比较并记录，不宣称整体PASS；既有警告只记录不扩大修复范围；既有SQL/POM/dirty docs字节不变。
- Failure returns to: File1 fixture异常先修fixture；GREEN错误回对应scope/validation/converter/route；涉及未知client/DDL一律仍deferred，不能下载或造表。
- Completion criteria: offline全部边界有负例与正确wire证明，MC/Rule表完整；不宣称真实搜索/同步已完成。
- Rollback: 只撤此Step新files与search-specific error/security/config细小修改；不动PG/MQ/history/其他工作。
- Commit paths: `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/goods/ProductSearchOpenApiConfiguration.java`, `enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/goods/ProductSearchControllerWebTest.java`, `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/validation/ProductSearchValidationGroup.java`, `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/enums/ProductSearchSortEnum.java`, `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/validation/ValidProductSearch.java`, `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/validation/ProductSearchValidator.java`, `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/search/ProductSearchQuery.java`, `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/search/SearchCriteriaBO.java`, `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/search/ProductSearchItemBO.java`, `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/search/SearchFacetBucketBO.java`, `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/search/SearchFacetBO.java`, `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/search/ProductSearchPageBO.java`, `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/search/ProductSearchItemResult.java`, `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/search/SearchFacetBucketResult.java`, `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/search/SearchFacetResult.java`, `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/search/ProductSearchPageResult.java`, `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/search/ProductSearchRepository.java`, `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/search/ProductSearchDomainService.java`, `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/search/ProductSearchQueryConverter.java`, `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/search/ProductSearchResultConverter.java`, `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/search/ProductSearchManage.java`, `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/search/ProductSearchManageImpl.java`, `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/config/ProductSearchProperties.java`, `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/search/service/impl/ProductSearchDomainServiceImpl.java`, `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/goods/ProductSearchController.java`, `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/filter/ProductSearchTenantFilter.java`, `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/enums/GoodsErrorStatus.java`, `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/handler/BaseExceptionHandler.java`, `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/filter/TenantContextFilter.java`, `enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/start/config/NativeHttpSecurityConfiguration.java`, `enjoyshop-service/enjoyshop-service-goods/src/main/resources/application.yml`, `enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-dev.yml`, `enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-test.yml`, `enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-prod.yml`, `enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/application/search/ProductSearchValidationTest.java`, `enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/search/ProductSearchBoundaryTest.java`, `enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/goods/ProductSearchOpenApiContractTest.java`, `enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/architecture/LightPersistenceArchitectureTest.java`, `docs/egon/plan/2026-10-11-10-10-product-search-offline-backend-implementation.md`。
- Commit: `feat(search): add offline product search boundary` — 一个逻辑任务一个path-limited commit，验证后立即commit。

## 8. Test, Validation, and Quality Gates

当前命令都明确finite/offline，不启动应用server/外部系统。RED先 `mvn -o -DargLine=-javaagent:/Users/mario/maven/repository/net/bytebuddy/byte-buddy-agent/1.17.8/byte-buddy-agent-1.17.8.jar -f enjoyshop-service/enjoyshop-service-goods/pom.xml -Dtest=ProductSearchControllerWebTest test`。GREEN按§7精确selectors；最终可用现verify架构插件，resolved classpath取现有target缓存，不加dependency。

两个docs validators分别严格验证本Plan和主Spec；源码import/Bean/四profilekey及dirty-hash检查用Python/rg，不创建scratch script；所有MC/Rule逐行audit放本Plan§12。预检成功不代替代码tests；future realES/PG/Rabbit/中文召回全部Not run。

## 9. Migration, Compatibility, Rollout, and Rollback

无本轮DB schema/migration/codegen/新client配置。默认enabled=false，当前允许HTTP契约建立但不存在adapter时503。现SPU/SKU事实、管理权限、目录/文件/广告、MQ v1、sourceEpoch与pendingqueue完全保持；不设置search listener、不改slot/offset/DLQ。将来部署ES后按主Spec§16闭合client/analyzer/receipt/fence/tombstone/full-rebuild并正式接线，不以此次offline端口替代完整流程。

## 10. Requirement-to-Step Traceability Matrix

以下Step1的“完成”只针对方向冻结/延期接入记录与既有边界保护。搜索行为验收仍Deferred，不因为覆盖检查通过就标implemented。

| Requirement | Effective Spec section | Steps | Files | Tests/gates | Completion evidence |
| --- | --- | --- | --- | --- | --- |
| 搜索 REQ-001 | [主Spec](../spec/2026-10-11-09-34-product-search-engine-proposal.md) §4/17 | Step1：offline合同实现/保留用户批准ES选型合同/状态 | 当前§5 typed-contract/边界files与本Plan | 已批准记录，未来TEST-016不在本轮 | offline边界验证，engine行为deferred |
| 搜索 REQ-002 | [主Spec](../spec/2026-10-11-09-34-product-search-engine-proposal.md) §7/9/11 | Step1：offline合同实现/保留name关键词/中文合同/状态 | 当前§5 typed-contract/边界files与本Plan | Deferred TEST-001/009/016 | offline边界验证，engine行为deferred |
| 搜索 REQ-003 | [主Spec](../spec/2026-10-11-09-34-product-search-engine-proposal.md) §9/11 | Step1：offline合同实现/保留分类/品牌filter合同/状态 | 当前§5 typed-contract/边界files与本Plan | Deferred TEST-003/008 | offline边界验证，engine行为deferred |
| 搜索 REQ-004 | [主Spec](../spec/2026-10-11-09-34-product-search-engine-proposal.md) §7/9/11/13 | Step1：offline合同实现/保留规格/严格facets合同/状态 | 当前§5 typed-contract/边界files与本Plan | Deferred TEST-002/003 | offline边界验证，engine行为deferred |
| 搜索 REQ-005 | [主Spec](../spec/2026-10-11-09-34-product-search-engine-proposal.md) §9 | Step1：offline合同实现/保留价格整数分区间合同/状态 | 当前§5 typed-contract/边界files与本Plan | Deferred TEST-001 | offline边界验证，engine行为deferred |
| 搜索 REQ-006 | [主Spec](../spec/2026-10-11-09-34-product-search-engine-proposal.md) §7/9/10 | Step1：offline合同实现/保留有界分页与exact total合同/状态 | 当前§5 typed-contract/边界files与本Plan | Deferred TEST-005/009 | offline边界验证，engine行为deferred |
| 搜索 REQ-007 | [主Spec](../spec/2026-10-11-09-34-product-search-engine-proposal.md) §7/9 | Step1：offline合同实现/保留排序白名单/稳定tie合同/状态 | 当前§5 typed-contract/边界files与本Plan | Deferred TEST-004 | offline边界验证，engine行为deferred |
| 搜索 REQ-008 | [主Spec](../spec/2026-10-11-09-34-product-search-engine-proposal.md) §7/9/10 | Step1：offline合同实现/保留独立安全高亮合同/状态 | 当前§5 typed-contract/边界files与本Plan | Deferred TEST-007/016 | offline边界验证，engine行为deferred |
| 搜索 REQ-009 | [主Spec](../spec/2026-10-11-09-34-product-search-engine-proposal.md) §7/9/11/15 | Step1：offline合同实现/保留tenant/可售/管理安全不变合同/状态 | 当前§5 typed-contract/边界files与本Plan | Deferred TEST-006/008/016 | offline边界验证，engine行为deferred |
| 搜索 REQ-010 | [主Spec](../spec/2026-10-11-09-34-product-search-engine-proposal.md) §7/11/16 | Step1：offline合同实现/保留当前态与receipt/fence/ACK合同/状态 | 当前§5 typed-contract/边界files与本Plan | Deferred GATE-ES-02；TEST-010–013 | offline边界验证，engine行为deferred |
| 搜索 REQ-011 | [主Spec](../spec/2026-10-11-09-34-product-search-engine-proposal.md) §11/16 | Step1：offline合同实现/保留全量/增量/重建切换合同/状态 | 当前§5 typed-contract/边界files与本Plan | Deferred GATE-ES-02/04；TEST-014/016 | offline边界验证，engine行为deferred |
| 搜索 REQ-012 | [主Spec](../spec/2026-10-11-09-34-product-search-engine-proposal.md) §4/16 | Step1：offline合同实现/保留部署后再具体接入合同/状态 | 当前§5 typed-contract/边界files与本Plan | 本轮两validator/metadata-only/无代码 | offline边界验证，engine行为deferred |
| 前驱 2026-10-10-14-48-postgresql-cdc-advertisement-synchronization.md：REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010, REQ-011, REQ-012 | [边界依赖](../spec/2026-10-10-14-48-postgresql-cdc-advertisement-synchronization.md)，有效章节见§2.2 | Step1：只保护现合同，不重复实现历史功能 | 本Plan§2/9，前驱文件只读 | 前驱hash原样，生产差异仅search范围；历史runtime gate未重跑 | Unchanged；与同名搜索ID不混淆 |
| 前驱 2026-10-09-10-55-product-management-lifecycle.md：REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010, REQ-011, REQ-012, REQ-013, REQ-014, REQ-015 | [边界依赖](../spec/2026-10-09-10-55-product-management-lifecycle.md)，有效章节见§2.2 | Step1：只保护现合同，不重复实现历史功能 | 本Plan§2/9，前驱文件只读 | 前驱hash原样，生产差异仅search范围；历史runtime gate未重跑 | Unchanged；与同名搜索ID不混淆 |
| 前驱 2026-09-23-17-16-enjoyshop-scaffold-brand-crud.md：REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010, REQ-011, REQ-012, REQ-013, REQ-014, REQ-015, REQ-016, REQ-017, REQ-018, REQ-019, REQ-020, REQ-021, REQ-022 | [边界依赖](../spec/2026-09-23-17-16-enjoyshop-scaffold-brand-crud.md)，有效章节见§2.2 | Step1：只保护现合同，不重复实现历史功能 | 本Plan§2/9，前驱文件只读 | 前驱hash原样，生产差异仅search范围；历史runtime gate未重跑 | Unchanged；与同名搜索ID不混淆 |
| 前驱 2026-09-28-21-50-distributed-file-catalog-lookups.md：REQ-101, REQ-102, REQ-103, REQ-104, REQ-105, REQ-106, REQ-107, REQ-108 | [边界依赖](../spec/2026-09-28-21-50-distributed-file-catalog-lookups.md)，有效章节见§2.2 | Step1：只保护现合同，不重复实现历史功能 | 本Plan§2/9，前驱文件只读 | 前驱hash原样，生产差异仅search范围；历史runtime gate未重跑 | Unchanged；与同名搜索ID不混淆 |
| 前驱 2026-10-09-16-39-homepage-kubernetes-gateway-architecture.md：REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008 | [边界依赖](../spec/2026-10-09-16-39-homepage-kubernetes-gateway-architecture.md)，有效章节见§2.2 | Step1：只保护现合同，不重复实现历史功能 | 本Plan§2/9，前驱文件只读 | 前驱hash原样，生产差异仅search范围；历史runtime gate未重跑 | Unchanged；与同名搜索ID不混淆 |

前驱bare-ID重合时，以本章来源限定为准；未展开实施的历史品牌CRUD/目录/商品/广告要求只是Unchanged保护，不为它们增加新的测试/功能Steps。

## 11. Risks, Blockers, and User Decisions

ES方向已批准；用户本轮明确改为先offline coding，故旧Plan只准文档限制关闭。当前Step不需要真实ES版本或同步storage，均保留到部署后；不据此silent waiver。

运行时Host/代理信任链、中文分词/相关性/facet/count正确性、partial shards、bulk/fence/receipt/tombstone/rebuild/容量在本轮**没有实现或验收**；缺adapter时必须503，不把未实现报为Runtime unverified或假200。旧Spec/Plan untracked文件不纳入本Step提交。

## 12. Review and Acceptance

### 12.1 Original requirement fidelity

根据用户当前明确coding授权，offline类实现与ES部署后接入分开；前驱保持，无frontend/DDL/client/MQ新增。

### 12.2 Spec consistency

主SpecQuery/BO/Result/纯领域端口复用，真实adapter不创建。Missing provider行为按主Spec503合同，由已存在Spring ObjectProvider实现，不引入占位Bean。架构核验发现Adapter直接引用Infrastructure属性违反ARCH-008；新Properties移到Common是无业务变化/无外部caller的路径澄清，Bean名和app.search配置不变。两个MapStruct接口使用现1.6.3 AnnotateWith生成显式命名Component，不手改generated output。当前新增3个search核心配置同四profile；ES-specific参数推迟，不能提前验证未定endpoint。

### 12.3 Repository executability

当前Step文件范围具体，单逻辑commit；新测试RED不依赖新类型；后续规范符号与通用validate/converter技术已可编译，实际client不影响当前合同。

### 12.4 Test and release completeness

finite MockMvc/H2/tests，0真实环境/服务启动；之后仍按部署后Spec/Plan/验收流程对接，不claim整体search feature完成。

### 12.5 Blocking Manual Check

| Check ID | Applicability | Status | Evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- |
| MC-ARCH-001 | Applicable | PASS | §4.8/5/7/8相应文件与focused验证设计 | 当前离线scope定义可独立验证；DEP无新增，TIME仅Duration；未来engine不属于该Step | None；执行后audit记录真实结果 |
| MC-REUSE-001 | Applicable | PASS | §4.8/5/7/8相应文件与focused验证设计 | 当前离线scope定义可独立验证；DEP无新增，TIME仅Duration；未来engine不属于该Step | None；执行后audit记录真实结果 |
| MC-DEP-001 | Applicable | PASS | §4.8/5/7/8相应文件与focused验证设计 | 当前离线scope定义可独立验证；DEP无新增，TIME仅Duration；未来engine不属于该Step | None；执行后audit记录真实结果 |
| MC-NAME-001 | Applicable | PASS | §4.8/5/7/8相应文件与focused验证设计 | 当前离线scope定义可独立验证；DEP无新增，TIME仅Duration；未来engine不属于该Step | None；执行后audit记录真实结果 |
| MC-VALID-001 | Applicable | PASS | §4.8/5/7/8相应文件与focused验证设计 | 当前离线scope定义可独立验证；DEP无新增，TIME仅Duration；未来engine不属于该Step | None；执行后audit记录真实结果 |
| MC-MODEL-001 | Applicable | PASS | §4.8/5/7/8相应文件与focused验证设计 | 当前离线scope定义可独立验证；DEP无新增，TIME仅Duration；未来engine不属于该Step | None；执行后audit记录真实结果 |
| MC-CONVERT-001 | Applicable | PASS | §4.8/5/7/8相应文件与focused验证设计 | 当前离线scope定义可独立验证；DEP无新增，TIME仅Duration；未来engine不属于该Step | None；执行后audit记录真实结果 |
| MC-LOG-001 | Applicable | PASS | §4.8/5/7/8相应文件与focused验证设计 | 当前离线scope定义可独立验证；DEP无新增，TIME仅Duration；未来engine不属于该Step | None；执行后audit记录真实结果 |
| MC-BEAN-001 | Applicable | PASS | §4.8/5/7/8相应文件与focused验证设计 | 当前离线scope定义可独立验证；DEP无新增，TIME仅Duration；未来engine不属于该Step | None；执行后audit记录真实结果 |
| MC-UTIL-001 | Applicable | PASS | §4.8/5/7/8相应文件与focused验证设计 | 当前离线scope定义可独立验证；DEP无新增，TIME仅Duration；未来engine不属于该Step | None；执行后audit记录真实结果 |
| MC-JSON-001 | Applicable | PASS | §4.8/5/7/8相应文件与focused验证设计 | 当前离线scope定义可独立验证；DEP无新增，TIME仅Duration；未来engine不属于该Step | None；执行后audit记录真实结果 |
| MC-TIME-001 | Applicable | PASS | §4.8/5/7/8相应文件与focused验证设计 | 当前离线scope定义可独立验证；DEP无新增，TIME仅Duration；未来engine不属于该Step | None；执行后audit记录真实结果 |
| MC-CONFIG-001 | Applicable | PASS | §4.8/5/7/8相应文件与focused验证设计 | 当前离线scope定义可独立验证；DEP无新增，TIME仅Duration；未来engine不属于该Step | None；执行后audit记录真实结果 |
| MC-PATTERN-001 | Applicable | PASS | §4.8/5/7/8相应文件与focused验证设计 | 当前离线scope定义可独立验证；DEP无新增，TIME仅Duration；未来engine不属于该Step | None；执行后audit记录真实结果 |
| MC-SCOPE-001 | Applicable | PASS | §4.8/5/7/8相应文件与focused验证设计 | 当前离线scope定义可独立验证；DEP无新增，TIME仅Duration；未来engine不属于该Step | None；执行后audit记录真实结果 |
| MC-TEST-001 | Applicable | PASS | §4.8/5/7/8相应文件与focused验证设计 | 当前离线scope定义可独立验证；DEP无新增，TIME仅Duration；未来engine不属于该Step | None；执行后audit记录真实结果 |
| MC-BLOCKER-001 | Applicable | PASS | §4.8/5/7/8相应文件与focused验证设计 | 当前离线scope定义可独立验证；DEP无新增，TIME仅Duration；未来engine不属于该Step | None；执行后audit记录真实结果 |

### 12.6 Final verdict

**PASS — Ready for user review**

本轮用户已明确要求按批准方向先编码，执行授权来自本轮原话；上表是offline代码Plan的设计门禁，不是执行结果。下面在同Step完成后追加fresh execution audit与真实测试/commit/partial矩阵。

### 12.7 Execution audit — offline Step and independent Spec conformance

**Baseline**：main@4e968affd7e5d8f7543d752cd971da9b7f295ae7。当前用户明确授权先编码/真实ES接入后置；采用本轮单Step修订，不执行已交付旧文档Step或空commit。Primary及effective版本见Header/§2，旧四个untracked文档与全部SQL/manifest/POM均保持原hash。

| Step | Current verified state | Commit ownership | Delivery scope | Fresh evidence |
| --- | --- | --- | --- | --- |
| Step1 offline backend | Verified；在最终回复记录Committed及实际hash | 本文件与§7的source/test/config同一个限定commit；可用git log --format=%H -- 本文件路径定位，避免在文件里伪造其自身hash | 39个明确路径，不包含旧untracked文档、target、tmp或外部工程 | 最新9类35 tests、0 failures/errors/skips；所有当前scope Manual/Literal Checks逐行闭合 |

执行验证证据（实际完成）：

- baseline `mvn -o ... -DskipTests test` BUILD SUCCESS，仅编译不称tests passed。
- 初始RED：ProductSearchControllerWebTest 1 failure，expected503/actual404，0 errors；先前Mockito动态attach环境error没有计为RED，改用当前classpath byte-buddy-agent1.17.8预加载后取得真正行为RED，不改POM/下载。
- OAS RED：Raw ResultRecord擦除FieldErrorsVO，expected SearchRequestError/actual ResultRecord；用现ModelConverters/TypeReference登记真实泛型，不添加fake endpoint/response subtype。
- 最后负例RED：字面null被接受、positive total+empty rows伪200；同metadata约束与返回集合完整性校验修正后GREEN。
- 最新精确§7 Maven selector + resolved javaagent参数 BUILD SUCCESS：BaseExceptionHandlerTest9、AdOpenApiContractTest1、ProductSearchControllerWebTest5、ProductSearchOpenApiContractTest1、ProductControllerWebTest3、ProductOpenApiContractTest1、ProductSearchValidationTest8、ProductSearchBoundaryTest5、LightPersistenceArchitectureTest2，共35，失败/错误/跳过均0。全部finite MockMvc/H2或纯Java，无监听server/外部引擎。
- 完整 `mvn -o ... -DskipTests -DskipITs=true -Dmaven.javadoc.skip=true verify` **BUILD FAILURE**：原HEAD的git archive临时副本fresh同命令也报告72项（ARCH-002:1、ARCH-008:70、ARCH-009:1）。修复本轮新Filter->Infra属性依赖后，最终fresh bytecode `...:5.4.1:check`仍72；按rule/severity/layer/class/member/descriptor/dependencyKind/locationKind/message进行保留重数的semantic比较：added=0、removed=0，search来源finding=0。没有加baseline-ignore文件、跳过规则或改插件阈值。完整verify不能称PASS；旧问题留独立范围处理。
- Properties移动到Common是新内部类型的语义保持路径澄清：配置键/Bean名/行为不变，修正实际ARCH-008；没有修改旧Ad/CDC属性包。两个Mapper通过现MapStruct AnnotateWith生成显式Component bean名，编译/容器测试验证；generated source不手改、不提交。
- 四profile app.search均enabled=false/public-sites={}/query-timeout=PT2S；纯Domain无Spring/MP/client；git diff --check通过；任务path inventory全部存在，未声明路径不纳入提交。

**完整搜索Spec状态（不能用offline Step通过替代）**：

| Requirement ID | Effective Spec source | Expected behavior | Commit evidence | Code/symbol evidence | Test/validation evidence | Status | Gap/impact/next action |
| --- | --- | --- | --- | --- | --- | --- | --- |
| REQ-001 | 主Spec§4/17与用户批准 | 单ES方向 | 本次唯一commit/本Plan | 不增Doris工厂/client | 源/POM保护 | Satisfied | 方向已经定，不重复询问 |
| REQ-002 | 主Spec§7/9/11 | 关键词实际检索 | 同Step | Query/Criteria只定义name/关键词语义 | input/copy边界tests | Partial | 真实中文召回/BM25/查询adapter尚未实现，部署后接入 |
| REQ-003 | 主Spec§9/11 | 品牌/类目实际过滤 | 同Step | typed IDs + trusted tenant | HTTP/Scope tests | Partial | 引擎filter翻译后再证明结果正确 |
| REQ-004 | 主Spec§7/9/11 | 规格AND/OR、真实facet | 同Step | spec canonical map、typed facet/truncation合同 | bounds/copy/projection tests | Partial | 真nested query/aggregations未实现，不用test-only port作证明 |
| REQ-005 | 主Spec§9 | 分为整数、范围过滤 | 同Step | int parser/native/cross-field validator | min/max/null/overflow负例 | Partial | 输入已实现，实际range检索后置 |
| REQ-006 | 主Spec§7/9/10 | 有界页/实际total/空页 | 同Step | long window、safe count计算、strict returned page | HTTP/window/empty/incomplete page tests | Partial | 实际ES精确total/分页还未实现 |
| REQ-007 | 主Spec§7/9 | 白名单/稳定排序结果 | 同Step | enum策略产生固定字段与numeric sku tie | 5种ordering tests | Partial | 排序定义已实现，真实ES执行后置 |
| REQ-008 | 主Spec§7/9/10 | 原生高亮与安全呈现 | 同Step | 独立mark-only安全转换、原name/nullable字段 | XSS/serialization tests | Partial | native ES highlight生成未实现，不能声称召回高亮已验证 |
| REQ-009 | 主Spec§7/9/11/15 | tenant与当前可售来源 | 同Step | host allowlist/独立MDC/finally/visible固定/窄安全链 | scope/security tests、管理回归 | Partial | HTTP隔离已证实；源可售投影/真实引擎过滤需后继 |
| REQ-010 | 主Spec§7/16 + CDC EVENT-003 | 真同步receipt/membership/fence/ACK | 无该范围代码 | 现producer/事件/queue不变，0新consumer | 历史文件/POM/SQL hash保护 | Not satisfied | 用户明确延期，storage/fence/bulk/ACK完整设计及实现部署后再做 |
| REQ-011 | 主Spec§11/16 | 全量/增量重建与切换 | 无该范围代码 | 0新index/mapping/generation/alias操作 | No external action | Not satisfied | 用户延期，不能把此状态改为Runtime unverified来掩盖尚无实现 |
| REQ-012 | 用户本轮覆盖主Spec时机 | 代码先完成离线，ES对接后置 | 同Step | disabled默认/无adapter503，无ES依赖、DDL、MQ操作 | 35 tests+hash/diff | Satisfied | 未启动项目/浏览器/DB/ES、未部署/push/PR/release |

前驱requirements按§10来源限定保持原样：既有品牌REQ-001–022、目录REQ-101–108、商品REQ-001–015、首页REQ-001–008、CDC REQ-001–012都不是本轮重新交付功能；其原文/旧数据/SQL与本轮边界的兼容性通过hash/商品与广告回归证明，历史真实PG/Redis/Rabbit/入口验收未重跑，仍不补造live evidence。

**Fresh final Manual Check（当前offline scope，整体框架及真实引擎局限另外明确列出）**：

| Check ID | Applicability | Status | Evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- |
| MC-ARCH-001 | Applicable | PASS | Common Properties/纯domain/实际bytecode report与fresh HEAD差分0，LightPersistenceArchitectureTest2 | 当前search符合既有Light方向；全模块verify仍原72违规，未伪称整体PASS | 旧72另期处理，当前不改无关源码 |
| MC-REUSE-001 | Applicable | PASS | Spring ObjectProvider、Common ValidationUtils/ResultRecord/BaseForwardConverter、现MapStruct/Security/OAS | 先复用实际设施，无重复框架 | None |
| MC-DEP-001 | Applicable | PASS | G/pom hash未变，所有Maven -o，agent为已resolved版本 | 0新client/utility/plugin依赖 | ES具体version/client部署后单独确定 |
| MC-NAME-001 | Applicable | PASS | 当前§5全部Java typed names | Query/BO/Result/ports/Validator/Configuration/Properties职责明确，无模糊carrier | None |
| MC-VALID-001 | Applicable | PASS | Query/Criteria shared metadata、interface Valid/NotNull、Public/Internal groups、手工同metadata、output校验 | 不是Controller-only；direct/proxy边界都有负例 | None |
| MC-MODEL-001 | Applicable | PASS | 普通carrier Data/NoArgs/AllArgs/Accessors/Builder编译；SortClauseBO immutable record | 没有普通Entity改record，无无意义父类；0持久化新模型 | None |
| MC-CONVERT-001 | Applicable | PASS | 2个BaseForwardConverter、generated映射、ID/spec/highlight tests | MapStruct单向投影，无JSON/reflection/BeanUtils object copying | ES document converter尚不在当前scope |
| MC-LOG-001 | Applicable | PASS | Manage/Domain/Filter/Controller/Validator/OAS配置Slf4j，safe debug/result/error日志 | 不记keyword/token/credentials，不加业务日志库 | None |
| MC-BEAN-001 | Applicable | PASS | 显式Component/Service/Bean名、qualified final Lombok注入；Mapper AnnotateWith生成命名Component；context tests | 真实名字和generated constructor qualifier传播匹配 | None |
| MC-UTIL-001 | Applicable | PASS | 当前新增imports为JDK/现Framework，POM不变 | 无Utils复制/新工具库/BeanUtils copy | None |
| MC-JSON-001 | Applicable | PASS | JsonValue sort、ALWAYS nullable highlight、string IDs、实际generic errors、generated OAS test | 仅Spring Jackson，7字段wrapper与字段约束一致 | None |
| MC-TIME-001 | Applicable | PASS | Properties queryTimeout Duration、Common ResultRecord Instant/epoch ms | 没有java.util日期；真实deadline执行属client后继，不谎称已实现 | None |
| MC-CONFIG-001 | Applicable | PASS | 四profile相同3-key block、validated named properties、default false | 0真实endpoint/secret配置，Cdc/ad旧值未改 | None |
| MC-PATTERN-001 | Applicable | PASS | sort enum Strategy及参数化Criteria Specification、闭合编码/不可变choices tests | 当前变化轴已明确，Engine算法翻译延期，未新增可切换引擎Factory | None |
| MC-SCOPE-001 | Applicable | PASS | 39路径 inventory；保护旧四文档/SQL/POM hash；只exact新增search与依赖接线 | 没有前端/商品写业务/广告/框架/迁移扩大修改 | None |
| MC-TEST-001 | Applicable | PASS | 最新35 tests全部GREEN；HTTP/OAS/output真实RED记录；scoped static/hash | 当前scope验证完整；full verify baseline failure明确，不把mock当引擎 | None |
| MC-BLOCKER-001 | Applicable | PASS | 当前Step所有17条scope gates闭合；零新增架构违规；post-deploy功能已显式排除 | offline Step没有隐藏blocker；完整Spec仍Partial且基线72导致全模块verify失败 | 真实ES/sync/rebuild及原框架问题后继，当前不宣称全Spec PASS |

**Fresh final Literal Rule Gate**：

| Literal rule | Applicability | Status | Final diff/path/symbol evidence | Final test/static evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- | --- |
| Rule 1 | Applicable | PASS | §5 typed inventory与Configuration/Properties/Group角色 | compile+手工类型检查 | 语义名称，未知领域模型不占位 | None |
| Rule 2 | Applicable | PASS | native/custom shared SearchInput、Valid/Validated/groups、manual ValidationUtils、output contract | query/internal/direct/cascade负例 | 每层校验；phone无字段N/A | None |
| Rule 3 | Applicable | PASS | 全普通class Lombok组合；immutable SortClause record；2个BaseForwardConverter | source+generated compile与转换tests | root无callSuper，不发明父类，无手动跨层拷贝 | None |
| Rule 4 | Applicable | PASS | Slf4j/name/final Qualifier/RequiredArgs，AnnotateWith mapper bean名 | 容器测试+generated Component/constructor检查 | 无field Autowired/业务手写注入构造 | None |
| Rule 5 | Applicable | PASS | JDK/已有Framework imports，POM原hash | imports review | 未引入额外utility/Utils | None |
| Rule 6 | Applicable | PASS | JsonValue/Jackson/nullable/string IDs、typed error schema | serializer与真OAS | 0Gson/Fastjson/ordinal持久编码；DB enum无本轮修改 | None |
| Rule 7 | Applicable | PASS | 同四profile search block | 精确block parity | 值均default false，没有单环境漏key | None |
| Rule 9 | Applicable | PASS | enum Strategy+Criteria参数化Specification | ordering/criteria/spec-copy tests | 真实引擎算法未写，不用硬编码dispatch冒充它 | None |
| Rule 10 | Applicable | PASS | Duration timeout/Common Instant response | negative Duration测试、禁用日期imports检查 | only java.time，真实client deadline未接 | None |
| Rule 11 | Applicable | PASS | 非open Light/Common配置、Domain ports、旧MP/DDL/Event保持 | 实际layer报告相对fresh HEAD新增0 | 全模块旧72违规另列，没有改规则阈值或忽略文件 | None |

**Scope and delivery discipline**：单逻辑Step最多一个commit；真实hash在最终响应/工具结果记录。旧4个untracked文档不stage、不commit；sql/POM/history数据不变。不push/PR/merge/release，不运行main/server/browser/DB/ES。没有repository scripts/work harness，故没有未定disposition；临时baseline/archive/log和source准备脚本只在/private/tmp，不纳入交付。

**Execution verdict: PARTIAL — Spec requirements are unmet or unverified**。offline Step达成并经过fresh校验；完整ES检索、native高亮/聚合、CDC持久化/fence和重建仍按用户要求延期，不将这些缺实现称作已经运行可用。
