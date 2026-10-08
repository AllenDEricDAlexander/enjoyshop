# 分布式商品目录与文件上传实施计划

| Field | Value |
| --- | --- |
| Document | 2026-09-29-08-52-distributed-catalog-storage-implementation.md |
| Template Version | 4 |
| Status | Review |
| Created | 2026-09-29 08:52 CST |
| Updated | 2026-10-05 02:06 CST |
| Owner | mario |
| Repository | enjoyshop |
| Scope | enjoyshop-service/enjoyshop-service-goods |
| Source Requirement | 用户附件 Pasted text.txt 第2章；用户确认仅支持 Egon-COLA Java 21 + Spring Boot 3.5、文件 API 放入 goods、其他采用推荐方案，并在本轮选择 Shoubing OIDC Bearer |
| Baseline Revision | 60d2fe3 (main；Step 1 开始前 HEAD；当时生产与测试源码干净) |
| Implements Spec | [第2章：跨域、商品目录查询与 MinIO/Ceph 文件存储](../spec/2026-09-28-21-50-distributed-file-catalog-lookups.md) |
| Spec Status | Accepted |
| Spec Revision | 2026-09-29 11:11 CST；本 Plan 实施最终收敛的用户批准设计 |
| Effective Specs | [第2章：跨域、商品目录查询与 MinIO/Ceph 文件存储](../spec/2026-09-28-21-50-distributed-file-catalog-lookups.md); [enjoyshop 工程骨架与商品品牌 CRUD 设计](../spec/2026-09-23-17-16-enjoyshop-scaffold-brand-crud.md) |
| Depends On Plans | [enjoyshop 骨架与品牌 CRUD 实施计划](2026-09-24-12-42-scaffold-brand-crud-implementation.md) |
| Supersedes | None |
| Superseded By | None |
| Related Plans | None |

## 1. Summary

本 Plan 实施已 Accepted 主 Spec 的 REQ-101–108，继续使用 goods 现有 Light 单模块和 Spring Boot 3.5。六个 Step 依次交付本地 catalog schema/Spec PO、API-001 品牌分类过滤、API-007 规格读取、逐 profile CORS/OIDC/S3 配置、图片校验与对象存储 Adapter、最后接通上传 REST 契约。每个 Step 先建立行为测试，再完成代码、验证并提交该 Step 的精确路径。最后用 goods 的 verify/架构门禁收口；Shoubing、外部目录数据和 MinIO/Ceph 的真实读写保留为发布前人工门。

## 2. Target Spec and Effective Design

### 2.1 Primary target

- Path: docs/egon/spec/2026-09-28-21-50-distributed-file-catalog-lookups.md
- Status: Accepted；DEC-103–111 已记录用户选择，本轮“1”明确选择 Shoubing OIDC USER Bearer。
- Revision: Spec metadata Updated 2026-09-29 11:11 CST；代码基线 60d2fe3 (main)。
- Approval evidence: 用户要求“confirmed, start writing plan”，选择 goods 与推荐方案，并选择 Shoubing Bearer 上传认证。

### 2.2 Effective Spec set

| Role | Spec/link | Status/revision | Effective sections | Why included |
| --- | --- | --- | --- | --- |
| Primary | [catalog/file Spec](../spec/2026-09-28-21-50-distributed-file-catalog-lookups.md) | Accepted / 2026-09-29 11:11 CST | 全文；尤其 §§3–5、6.1–6.2、7–11、14–20 | 唯一主需求，定义 CORS、catalog schema、API、Shoubing、S3、文件验证和发布门 |
| Normative dependency | [商品品牌 CRUD Spec](../spec/2026-09-23-17-16-enjoyshop-scaffold-brand-crud.md) | Accepted / 2026-09-24 | §§3.2、5.4、6.1、7.1、9.0–9.4、11.2.1、14、16 | API-001 延续 DEC-112 的集合查询、ResultRecord、BrandPageQuery、Light/MP-SDJ、PostgreSQL 和品牌表不变 |

### 2.3 Superseded or excluded content

旧教程中的 FastDFS、Eureka、独立文件微服务、URL 临时签名、管理 CRUD、删除/下载接口及前端页面均不属于有效设计。主 Spec DEC-103–111 以 MinIO/Ceph RGW、Shoubing USER Bearer、goods 内上传和推荐选项收敛了这些内容。品牌既有 API-001 缺省行为、分页封套及 brand 表不被替换。

## 3. Effective Requirements and Acceptance

| Requirement | Source Spec section | Effective statement | Observable acceptance | Implementation impact |
| --- | --- | --- | --- | --- |
| REQ-101 | §4 / §9 / §14 | CORS 对配置来源开放 GET/POST/OPTIONS 和 Authorization/Content-Type，拒绝未许可来源 | Allowed Origin 预检带精确允许头；非法 Origin 无 ACAO | SecurityFilterChain CORS 配置、四 profile 与 MVC 测试 |
| REQ-102 | §4 / §11 | goods master_data 增加 category/template/spec/para/category_brand 五表 | 一个受管 SQL 版本、一个 Manifest checksum、五个 SINGLE/master_data routing entry；DDL 静态契约与 H2 join 测试通过 | MP-SDJ DDL、Manifest、custom DAO/XML、仅生成 SpecPO |
| REQ-103 | §4 / §9.2.1 / §11 | API-001 接收可选 categoryName，保留原过滤、全量/分页与 PageResultRecord | 同名类目关联合并且 brand id 不重复；tenant/active 过滤与既有顺序、count 一致 | BrandListQuery→BrandPageQuery→BrandDomainQuery、Repository/DAO/XML |
| REQ-104 | §4 / §9.2.2 / §11 | API-007 查询类目模板规格，返回有序 SpecOptionVO.options | exact name、同名 category union、空集合、重复 join、软删除和租户用例通过 | Spec Query 的 Adapter/Application/Domain/Repository/DAO 链及 BaseForwardConverter |
| REQ-105 | §4 / §9.2.3 | POST /api/v1/files 要求 Shoubing USER Bearer；USER_DELEGATION/Jianshen app-entry 是上游 Token 获取前置；S3 PutObject 成功后才返回稳定公开 HTTPS URL | goods filter 拒绝缺失/无效 Bearer 为 401；成功为 201/Location/ResultRecord<String>；失败没有 URL；Starter 5.4.1 不支持 goods per-request USER Resource-grant check | 受批准的 Shoubing starter、文件 CQE、Storage Port/Adapter、BaseExceptionHandler |
| REQ-106 | §4 / §9.2.3 / §14 | 只接收 JPEG/PNG；单文件 10MB，multipart 总请求 11MB | 空文件、伪造 MIME/扩展名、错误签名和超过 10MB 的内容在网络写入前被拒绝；servlet 溢出为 413/610107 | Multipart DTO/Command、ImageIO/JDK 签名检查、四 profile multipart 上限 |
| REQ-107 | §4 / §6.2 / §14 | base/dev/test/prod key 集合相同，值可按环境覆盖 | CORS、object-storage、Shoubing、multipart 与五个表路由键集合逐 profile 相同 | 四 YAML、ProfileKeyParityTest、属性绑定测试 |
| REQ-108 | §4 / §6.1 / §8 / §14 | 唯一应用形态保持现有 goods Light；Java 21 + Spring Boot 3.5；MP-SDJ 管理 PostgreSQL DDL | goods verify、Light 架构 verifier、Codegen plan/check 无漂移 | 不创建模块；窄 Codegen plan/apply；无 Flyway |

## 4. Implementation Strategy and Dependency Order

### 4.1 Ordered strategy

Step 1 先写 DDL 契约测试、单一新版本和 Manifest，再准备 hand-written SpecDAO/XML 作为查询扩展点；Codegen 的 native light profile 只生成 SpecPO，避免编辑 generator-owned DAO/XML。生成器 Plan 使用 Manifest 的实际 checksum，apply 后追加 classpath SQL 消费日志。Step 2 复用 API-001 及其已有分页链；Step 3 在同一 schema 上完成 API-007。Step 4 先装配已获批依赖、配置属性、CORS、只保护 POST 上传的 matcher 和表路由。Step 5 用 ProductImageBO 与 ObjectStoragePort 隔离 S3 SDK。Step 6 才添加 HTTP multipart adapter、错误/文档契约和用户运行说明。

Step 3 与 Step 2 都依赖 Step 1，但计划按顺序提交，以保持每次变更都可构建和审阅。Step 5 依赖 Step 4 的 S3Client/属性 Bean；Step 6 依赖 storage/application 行为及 Step 4 的 security matcher。所有数据库脚本只新增 V20260929_001；不编辑已存在 DDL，不在 Plan 阶段运行迁移。

### 4.2 Test-first strategy

| Behavior | RED test | Minimum GREEN | Permitted refactor/wiring |
| --- | --- | --- | --- |
| 五表与 managed manifest | CatalogDdlContractTest 先读取缺失脚本，应因路径/Manifest 条目不存在而失败 | 一份 SQL + 一项实际 SHA-256 Manifest entry | Generator 只生成 SpecPO；自定义 SpecDAO/XML 保持 Agent-owned |
| Domain method validation remains active without a Spring annotation in the Domain contract | StudentManagementApplicationTest invokes BrandDomainService#delete(null, -1L) through Spring and asserts both Jakarta violations | ConstraintViolationException is raised before repository access; the Domain interface has only Jakarta constraints while its Infrastructure bean remains @Validated | Remove only the redundant Spring @Validated from BrandDomainService; retain interface constraints and signatures |
| API-001 类目过滤 | BrandControllerWebTest、BrandOpenApiContractTest、BrandManageImplTest、BrandCategoryQueryRepositoryTest 和 SQL shape test 先断言新查询行为 | 同一查询增加 categoryName 条件与同名关联 EXISTS 子查询 | 保持 API-001 查询顺序/count/PageResultRecord |
| API-007 规格查询 | SpecControllerWebTest 与 SpecOpenApiContractTest 先看到缺路由/operation；Step 1 已交付 SQL，所以 Step 3 的 SQL-shape/H2 门验证该既有 DAO 合同 | 完成只读 CQE 链与 ordered options projection | 不增加 CRUD 或第二个 Spec API |
| Validation | CategoryNameValidatorTest 先验证可选/必选、trim、blank、120 上限；catalog query group 测试先验证重复校验 | Custom ConstraintValidator + ConvertGroup 边界 | 仅在证据显示重复映射时整理 helper |
| CORS、配置和认证 matcher | NativeHttpSecurityConfigurationTest 与 ProfileKeyParityTest 先缺允许头/配置键/表路由键 | CORS Bean、四 profile 和 POST matcher | OpenAPI 不改变其他路由许可语义 |
| 图片/对象存储 | ProductImageValidatorTest、FileDomainServiceImplTest、S3CompatibleObjectStorageAdapterTest 先缺实现或观察到错误格式/错误 PutObject 映射 | MIME/扩展名由实际签名推导，单次 PutObject，仅 ack 后返回稳定 URL | 只做不改变接口的局部整理 |
| HTTP 上传契约 | FileControllerWebTest 和 FileOpenApiContractTest 先得到无映射或缺操作文档 | multipart POST、401/400/413/503、201+Location+ResultRecord；USER grant denial is upstream | 更新 goods README 的环境与人工发布门 |

若某测试因类型签名尚不存在而不能编译，先加入 Spec 已决定的最小类型/方法签名作为 compile prerequisite，然后运行预期因行为缺失而失败的测试。不能把缺类编译错误冒称为业务 RED。

### 4.3 Sequential and parallel boundaries

| Step | Depends on | May run in parallel with | Must not overlap with | Reason |
| --- | --- | --- | --- | --- |
| Step 1 | None | None | Manifest/DDL/generator output | 一份 managed version 和 PO 生成态必须原子审阅 |
| Step 2 | Step 1 committed | None | Brand query paths | Brand filter 的 RED/GREEN 与 SQL/count 必须一致 |
| Step 3 | Steps 1–2 committed | None | Spec query paths | API-007 使用相同分类 schema 与 validation group |
| Step 4 | Steps 1–3 committed | None | 四个 application profile、POM、安全配置 | 配置 parity 与认证 matcher 同步提交 |
| Step 5 | Step 4 committed | None | FileManage/Domain/Port/Adapter | multipart bytes、格式和 S3 失败语义需端到端一致 |
| Step 6 | Steps 1–5 committed | None | File HTTP/errors/OpenAPI/README | HTTP 契约只在可调用的 Application/Storage 之后接入 |

### 4.4 Commit boundaries

每个 Step 一个路径受限语义提交。Step 1 同时包含 DDL、Manifest、custom DAO/XML、生成 SpecPO 和 SQL 消费日志，因为脚本/模型/生成记录是一个 schema 交付物。其他 Step 不跨分支，也不把未跟踪主 Spec 或其他文档放入源码提交。

### 4.5 Spec Simplicity and Implementation-necessity Audit

| Spec element | Spec necessity verdict/section | Current repository evidence | Direct/reuse alternative | Interaction/implementation cost | Plan decision |
| --- | --- | --- | --- | --- | --- |
| Extend API-001 with categoryName | §5.3 DEC-105; §7.0 | Existing GET /api/v1/brands, BrandController#listBrands, BrandPageQuery, BrandDAO#listBrandPage/countBrandPage | Reuse current collection query; no new brand-category route | One additional filter in existing SQL and filtered total | Implement in Step 2 |
| API-007 Specs query | §5.3 DEC-103; §9.2.2 | No existing Spec query; custom SpecDAO/XML extension fits goods Light | Brand API returns a different resource shape and cannot replace specs | One GET, one read chain, one join | Implement in Step 3 |
| Five catalog tables | §11 / Goal 2; ACTOR-003 is external writer | Current manifest ends at 20260923_001; brand is SINGLE/master_data | Existing brand table has no category/template/spec/para association data | One managed DDL version; table-routing keys and no write CRUD | Implement in Step 1 |
| SpecPO generation | §8.2 / §11.2.3 | Native Light DDL generator; no SpecPO exists | Handwritten PO would bypass DDL-owned scaffolding | Generator plan outputs exactly SpecPO; DAO/XML are omitted from generator artifacts to preserve custom query ownership | Implement generated PO in Step 1 |
| Custom SpecDAO/XML method | §7.3/§9.2.2; generic-mapper learning goal | Existing BrandDAO custom named XML methods | Generator-owned mapper files cannot be hand-edited | One custom mapper method and its result map for API-007 | Implement in Step 1 |
| CORS allowlist | §5.3 DEC-106 | NativeHttpSecurityConfiguration has no CorsConfigurationSource | @CrossOrigin("*") would not express per-environment precise allowlists | One Spring Security CORS source; four profile keys | Implement in Step 4 |
| Shoubing starter | §5.3 DEC-107 | Goods has security chain/config placeholders and RedisConfig#redissonClient; no starter dependency | Caller headers are not authentication | User-approved starter 5.4.1, registered filter, authenticated POST matcher | Implement in Step 4 |
| AWS SDK v2 S3Client | §5.3 DEC-110 | No object storage SDK in goods/Egon BOM; local RAG storage is not distributed object storage | Provider SDKs would split MinIO/Ceph implementations | User-approved BOM and S3 2.55.6, one Light Adapter | Implement in Steps 4–5 |
| File upload API in goods | §5.3 DEC-104/107; §9.2.3 | Current app has no file API; current Light adapter and ResultRecord exist | Separate file app adds runtime, route, POM aggregation without a consumer boundary | One multipart POST and later public URL GET | Implement in Step 6 |
| Stable public URL | §5.3 DEC-106 | Product image URLs are later persisted; there is no URL policy | Presigned URL expires and cannot serve durable product records | Separate endpoint/public base and bucket GET policy | Implement in Steps 4–5; verify in TEST-114 |
| File metadata table/delete endpoint/frontend | §§3.2, 7.0, 9.2.3 | No current upload index or frontend app | S3 object result suffices for accepted consumer | Avoids unrequested DB writes, destructive API and new frontend | Excluded |

### 4.6 Change-unit Dependency Matrix

| Change unit | Requirements | Proof/RED point | Compile/runtime prerequisites | Produces | Consumers/unblocks | Owning Step |
| --- | --- | --- | --- | --- | --- | --- |
| Catalog DDL and Spec PO | REQ-102/108 | CatalogDdlContractTest | MP-SDJ next version; codegen CLI/classpath | five read-model tables and SpecPO | Steps 2–3/external catalog writer | Step 1 |
| Brand category filter | REQ-103 | BrandControllerWebTest + repository H2/XML tests | Step 1 schema, current BrandPageQuery | PageResultRecord filtered by categoryName | Existing product selector | Step 2 |
| Spec query | REQ-104 | SpecControllerWebTest + SQL/OpenAPI tests | Step 1 SpecPO/DAO/XML and Step 2 validation group | typed ordered options | product editor API | Step 3 |
| CORS/OIDC/S3 configuration | REQ-101/107/108 | NativeHttpSecurityConfigurationTest/ProfileKeyParityTest | Shoubing artifact and AWS BOM/S3 2.55.6 | properties, CORS source, protected matcher, S3Client Bean | Steps 5–6 | Step 4 |
| Product image validation/storage | REQ-105/106 | validator/domain/adapter tests | Step 4 S3Client and properties | FileManage/Domain Service/Port/Adapter returning URL | Step 6 | Step 5 |
| Upload HTTP contract | REQ-105–107 | FileControllerWebTest/BaseExceptionHandlerTest/FileOpenApiContractTest | Step 4 security and Step 5 Application | authenticated POST with stable URL/error semantics | browser and release operator | Step 6 |

### 4.7 Java, Spring, and Egon-COLA Implementation Standards

| Concern | Current repository evidence | Effective Spec decision | Planned implementation consequence | Owning Steps/checks |
| --- | --- | --- | --- | --- |
| Architecture profile | enjoyshop-service/enjoyshop-service-goods/pom.xml, Light package tree, goods README verifier | DEC-104; Java 21 + Spring Boot 3.5 only | Existing one-module Light; no new project or biz.* tree | Steps 1–6; MC-ARCH-001 |
| Reuse/capability | Spring MVC/Validation/Security, common-core ResultRecord/PageResultRecord, MP-SDJ, BrandDAO/XML, RedisConfig#redissonClient | Reuse existing Spring/Egon facilities; exact AWS/Shoubing gaps are user-approved | Add only approved AWS SDK BOM/S3 and Shoubing starter; no MinIO SDK, file service, utility package or test-only security library | Steps 4–6; MC-REUSE-001/MC-DEP-001 |
| Naming/model/validation/conversion | BrandPO/BrandBO/BrandPageQuery/BrandResult and BrandPersistenceConverter/BrandAdapterConverter | DDD Light, layered typed queries, custom category-name validation and query Validation Group | PO/BO/DTO/Query/Command/Result classes use Lombok baseline; immutable BrandListQuery/SpecOptionVO are value records; all conversions use MapStructPlus and Egon converters | Steps 1–6; MC-NAME-001/MC-VALID-001/MC-MODEL-001/MC-CONVERT-001 |
| Bean/logging/util/JSON/time/config | lombok.config copies Qualifier; existing @Slf4j/@Qualifier Beans; Spring Jackson; four YAML profiles | Safe logs; common envelopes except Shoubing 401 | Named Beans, @RequiredArgsConstructor, @Qualifier; JDK ImageIO; Jackson; Duration; identical profile keys | Steps 2–6; MC-LOG-001/MC-BEAN-001/MC-UTIL-001/MC-JSON-001/MC-TIME-001/MC-CONFIG-001 |
| Business variation/pattern | MinIO and Ceph RGW use the same S3 protocol; no current storage port | Primary §13 selects Adapter | ObjectStoragePort isolates SDK types in one S3CompatibleObjectStorageAdapter; no provider Strategy/branch tree | Step 5; MC-PATTERN-001 |

#### Capability reuse ledger

| Need | Candidates inspected | Exact evidence | Fit/gap | Decision | Added dependency/custom code | Owning Step/check |
| --- | --- | --- | --- | --- | --- | --- |
| HTTP/CORS/validation | Spring MVC, starter-validation, NativeHttpSecurityConfiguration | goods POM and current Web tests | MVC is present; exact per-environment CORS policy is missing | Reuse framework; add CORS source | None | Step 4; MC-REUSE-001 |
| Security | Spring Security chain and Tianquan-Shoubing starter | NativeHttpSecurityConfiguration, profiles, RedisConfig#redissonClient; sibling starter 5.4.1 | USER token is stateless; the Servlet filter must be installed inside the matching Spring Security chain so its authentication survives context loading | Add approved starter and upload-only SecurityFilterChain; keep Servlet registration disabled; upstream grant state remains external | top.egon:egon-cola-tianquan-shoubing-starter:5.4.1 | Steps 4,6; MC-DEP-001 |
| Database/Mapper | MP-SDJ, EgonColaPostgreDdlRunner, BrandRepository/BrandDAO/XML | existing goods POM, DDL/Manifest, Brand query files | Existing path supports SINGLE master_data and custom typed XML | Reuse; one next SQL/Manifest; generator creates only SpecPO | None | Step 1; MC-REUSE-001 |
| Mapping | MapStructPlus, BaseConverter/BaseForwardConverter | BrandPersistenceConverter and BrandAdapterConverter | One-way query projections and PO↔BO mapping fit existing converters | Reuse; no BeanUtils/reflection | None | Steps 2,3,5; MC-CONVERT-001 |
| Distributed storage | AWS SDK Java v2; no Egon object-storage Component | goods POM/Egon BOM lacks S3 client; official SDK supports custom S3 endpoint/path-style | SDK must be hidden behind Domain port for MinIO/Ceph | Add approved BOM and S3 2.55.6 | software.amazon.awssdk:bom:2.55.6; software.amazon.awssdk:s3 | Steps 4–5; MC-DEP-001 |
| File image identification | JDK ImageIO/file signatures | JDK 21; no MIME utility dependency | 10MB JPEG/PNG identification is local | Use JDK only | None | Step 5; MC-UTIL-001 |
| Security test support | Spring Boot Test/MockMvc; no spring-security-test | goods POM test dependencies and existing MVC slice pattern | authenticated session can be represented without a new dependency; live Shoubing grant stays manual | Reuse current test stack | None | Steps 4,6; MC-DEP-001 |

### 4.8 User-mandated Java Rule Implementation Matrix

| Literal rule | Spec source | Repository evidence | Exact files and order | Pseudocode obligations | Validation gate | Steps | Status/blocker |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Rule 1 | Primary §§6.2/8/10 | BrandListQuery is immutable record; current Brand PO/BO/DAO/Query/Result types | Step 1 SpecPO/SpecDAO; Steps 2–6 semantic DTO/Query/Command/BO/VO/Result types | no Map carrier; no FileUploadResult or suffixless business carrier | static type inventory + compile | 1–6 | PASS |
| Rule 2 | Primary §§6.2/9/10 | starter-validation, Brand Manage/Domain/Repository @Valid patterns | common validation group and Validator before Brand/Spec query files; DTO→Command→BO validation files before upload wiring | @Valid each handoff; custom ValidCategoryName; ConvertGroup on reused BrandDomainQuery/SpecDomainQuery; @Validated Spring boundaries; no DAO business rule duplication | positive/negative/group tests and MockMvc | 2–6 | PASS |
| Rule 3 | Primary §§6.2/10 | BrandPO extends EgonModel; current converters use BaseConverter/BaseForwardConverter | generated SpecPO; mutable PO/BO/DTO/Query/Command/Result classes; immutable BrandListQuery/SpecOptionVO records; MapStruct converters in each layer | Lombok class baseline; SuperBuilder/callSuper for EgonModel PO; BaseForwardConverter for one-way projections | constructor/mapping compile and tests | 1–6 | PASS |
| Rule 4 | Primary §§6.2/7/13 | goods lombok.config propagates Qualifier; current named Beans use RequiredArgsConstructor/Slf4j | Brand/Spec/File Manage/Domain/Repository/Adapter and S3 Adapter | stable Bean names, @Slf4j, final dependencies, @RequiredArgsConstructor, field @Qualifier | compile/context/wiring/safe-log review | 2–6 | PASS |
| Rule 5 | Primary §6.2 | Java 21 ImageIO; no S3 client in goods | ProductImageValidator and S3 Adapter | JDK ImageIO/UUID/URI and approved S3 SDK only | imports/dependency audit | 4–5 | PASS |
| Rule 6 | Primary §§6.2/9 | common-core ResultRecord/PageResultRecord and Jackson | FileUploadDTO, SpecOptionVO, existing wrappers and OAS tests | Jackson field/wire shape; ResultRecord<String>; separate Shoubing 401 body | serialization/generated OAS tests | 3,6 | PASS |
| Rule 7 | Primary §§6.2/5.4 | four application YAML profiles and ProfileKeyParityTest | all profiles and properties class in Step 4 | identical owned key paths incl. five table maps and multipart sizes; values may vary | ProfileKeyParityTest/Binder | 4 | PASS |
| Rule 9 | Primary §§6.2/13 | MinIO and Ceph RGW share S3 protocol | ObjectStoragePort before S3CompatibleObjectStorageAdapter | Adapter isolates approved endpoint/client variation; no provider branch or Strategy | port/adapter tests | 5 | PASS |
| Rule 10 | Primary §§6.2/9/15 | inherited EgonModel time types; SDK timeout properties | ObjectStorageConfigurationProperties and S3Client builder | use java.time.Duration; no java.util date type; existing timestamp unchanged | binding tests/forbidden import search | 4–6 | PASS |
| Rule 11 | Primary §§6.1/6.2/8 | goods module is current Egon-COLA Light; verify-bound architecture check | every target path stays inside goods Light or specified docs/test path | no new app/module, biz.* or reversed layer dependency | goods verify architecture gate | Every Step | PASS |


## 5. Change File Tree

The module and package shape stays within the existing goods Light project. Exact operations and symbols are listed once per path in the inventory below.

    enjoyshop-service/enjoyshop-service-goods/codegen/
    enjoyshop-service/enjoyshop-service-goods/src/main/resources/db/egon-mp/
    enjoyshop-service/enjoyshop-service-goods/src/main/resources/mybatis/mapper/goods/
    enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/validation/
    enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/goods/
    enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/
    enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/
    enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/
    enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/start/config/
    enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/
    docs/egon/codegen/

| Operation | Path | Current evidence/symbol | Final symbols/state | Responsibility | Step | Requirements | Validation owner |
| --- | --- | --- | --- | --- | --- | --- | --- |

| CREATE | enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/ddl/CatalogDdlContractTest.java | absent; existing goods JUnit suite | five-table DDL/Manifest static contract test | no DB/runtime calls | Step 1 | REQ-102/108 | test |
| MODIFY | enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/architecture/LightPersistenceArchitectureTest.java | exact generated PO/DAO inventory counts are 9 | expect 10 PO and 10 DAO after adding SpecPO/SpecDAO | keep Light architecture guard aligned to the required artifacts | Step 1 | REQ-102/104/108 | verify |
| MODIFY | enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/config/datasource/RepositoryPersistenceContractTest.java | exact Mapper XML inventory count is 9 | expect 10 after adding SpecDAO.xml | preserve the all-mapper lifecycle statement guard | Step 1 | REQ-102/104/108 | verify |
| MODIFY | enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/start/StudentManagementApplicationTest.java | application context smoke test only | assert invalid BrandDomainService method arguments are rejected by Spring method validation | prove the DDD annotation correction preserves executable parameter validation | Step 1 | REQ-001/002/022/108 | focused test/verify |
| MODIFY | enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/BrandDomainService.java | imports Spring @Validated despite Jakarta constraints already declared | remove only the Spring import/type annotation; preserve method signatures and Jakarta constraints | eliminate the verified ARCH-002 domain-to-Spring edge | Step 1 | REQ-001/002/022 | verify |
| CREATE | enjoyshop-service/enjoyshop-service-goods/src/main/resources/db/egon-mp/V20260929_001__initialize_catalog_lookup_schema.sql | latest is V20260923_001 | category/template/spec/para/category_brand DDL and indexes | one next managed version | Step 1 | REQ-102 | CatalogDdlContractTest/runner |
| MODIFY | enjoyshop-service/enjoyshop-service-goods/src/main/resources/db/egon-mp/repository-manifest.json | current two scripts and checksums | append one ordered entry with actual SHA-256 | preserve prefix and family | Step 1 | REQ-102/108 | DDL test/check |
| CREATE | enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/dao/SpecDAO.java | no SpecDAO | typed custom listByCategoryName query extends EgonColaMapper<SpecPO> | handwritten mapper contract, not generator-owned | Step 1 | REQ-104 | compile/mapper test |
| CREATE | enjoyshop-service/enjoyshop-service-goods/src/main/resources/mybatis/mapper/goods/SpecDAO.xml | no Spec mapper | custom joined SELECT/resultMap for API-007 | active/tenant/soft-delete predicates and stable order | Step 1 | REQ-104 | XML shape test |
| CREATE | enjoyshop-service/enjoyshop-service-goods/codegen/catalog-lookup-codegen.json | current brand-codegen.json is configVersion 1/full artifacts | light, manifest input, logicalTables spec, artifacts po, existingTypeMappings.dao SpecDAO | generator output is exactly SpecPO | Step 1 | REQ-102/108 | plan/check |
| GENERATED | enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/po/SpecPO.java | no SpecPO; DDL is source of truth | EgonModel child with spec business columns | generated by codegen plan/apply; no handwritten body | Step 1 | REQ-102/104 | generator check/compile |
| MODIFY | docs/egon/codegen/ddl-consumption-log.md | append-only log already contains 20260923_001 | append one full SQL consumption record after apply | do not edit earlier entries | Step 1 | REQ-102/108 | diff review |
| MODIFY | enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/goods/BrandControllerWebTest.java | existing API-001 MockMvc and BrandManage fixture | optional category binding, absent-filter compatibility, invalid value response | preserve old scenarios | Step 2 | REQ-103 | MockMvc |
| MODIFY | enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/goods/BrandOpenApiContractTest.java | API-001 operation contract exists | optional categoryName schema assertion and unchanged PageResultRecord | no route or operationId change | Step 2 | REQ-103 | generated OpenAPI |
| MODIFY | enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/application/goods/BrandManageImplTest.java | listBrands asserts records and page totals but not category propagation | capture list/count Domain queries and assert both receive the same categoryName | protect the App→Domain query handoff before SQL work | Step 2 | REQ-103 | JUnit/Mockito |
| CREATE | enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/goods/repo/BrandCategoryQueryRepositoryTest.java | no category relation repository test | H2 same-name category union/dedupe/tenant/soft-delete cases | no external DB | Step 2 | REQ-103 | JUnit/H2 |
| MODIFY | enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/goods/repo/BrandQuerySqlShapeTest.java | current list/count SQL assertions | exact category subquery, bind parameter, active links, same-tenant equality | preserve existing filter/order checks | Step 2 | REQ-103 | JUnit XML-shape |
| CREATE | enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/common/validation/ValidCategoryNameValidatorTest.java | no reusable category constraint | optional/required null, whitespace and trimmed length 120/121 | use Jakarta Validator | Step 2 | REQ-103/104 | JUnit |
| CREATE | enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/common/validation/GoodsCatalogQueryValidationTest.java | no catalog query group tests | BrandDomainQuery validation at service/repository boundaries | assert invalid criteria do not invoke DAO | Step 2 | REQ-103 | Jakarta Validator/Mockito |
| CREATE | enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/validation/GoodsCatalogQueryValidationGroup.java | no catalog query group | marker interface for reused domain queries | no runtime behavior | Step 2 | REQ-103/104 | compile/group test |
| CREATE | enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/validation/ValidCategoryName.java | no reusable category constraint | optional/required Bean Validation constraint contract | null semantics depend on required flag | Step 2 | REQ-103/104 | validator test |
| CREATE | enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/validation/ValidCategoryNameValidator.java | no custom ConstraintValidator | trim-check nonblank and length <=120 | no name normalization here | Step 2 | REQ-103/104 | validator test |
| MODIFY | enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/goods/pojo/dto/BrandListQuery.java | existing immutable record with six query fields | append optional categoryName with schema and ValidCategoryName(required=false) | keep record accessors/binding and other query defaults | Step 2 | REQ-103 | MVC/OpenAPI |
| MODIFY | enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/goods/pojo/convertor/BrandAdapterConverter.java | existing MapStructPlus BaseForwardConverter | map categoryName after outer trim into BrandPageQuery | keep internal spaces exact; retain existing filter mappings | Step 2 | REQ-103 | converter test |
| MODIFY | enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/BrandPageQuery.java | current name/letter/PageQuery | add optional categoryName field and query constraint | keep pagination semantics unchanged | Step 2 | REQ-103 | compile/query test |
| MODIFY | enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/BrandManageImpl.java | listBrands maps BrandPageQuery to BrandDomainQuery | carry categoryName into domain query | preserve seq/page/count calls | Step 2 | REQ-103 | Manage test |
| MODIFY | enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/BrandDomainQuery.java | current name/letter/PageQuery | add optional categoryName with group constraint | preserve mutable class/Lombok baseline | Step 2 | REQ-103 | validation test |
| MODIFY | enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/BrandDomainService.java | listPage/countPage use BrandDomainQuery | ConvertGroup to GoodsCatalogQueryValidationGroup on cascaded query | keep method signatures otherwise | Step 2 | REQ-103 | group test |
| MODIFY | enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/repo/BrandRepository.java | list/count forward existing filters | validate the shared query group and pass categoryName to DAO | no direct DAO calls outside Repository | Step 2 | REQ-103 | H2/Mockito |
| MODIFY | enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/dao/BrandDAO.java | listBrandPage/countBrandPage | add named categoryName parameter to list and count | stable typed interface | Step 2 | REQ-103 | XML mapper binding |
| MODIFY | enjoyshop-service/enjoyshop-service-goods/src/main/resources/mybatis/mapper/goods/BrandDAO.xml | current list/count XML | add EXISTS over active same-tenant category/category_brand | no duplicate brands; preserve all filters/order/page | Step 2 | REQ-103 | SQL shape/H2 |
| CREATE | enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/goods/SpecControllerWebTest.java | no Spec route | category path binding, blank/oversize code 610110, empty/populated envelope | Shoubing not applied to reads | Step 3 | REQ-101/104 | MockMvc |
| CREATE | enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/goods/SpecOpenApiContractTest.java | no Spec operation | API-007 route, operationId, path parameter, options array and errors | code-first generated OAS only | Step 3 | REQ-104 | springdoc MockMvc |
| CREATE | enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/goods/pojo/convertor/SpecAdapterConverterTest.java | no Spec converter | DTO→SpecListQuery and SpecResult→immutable VO mappings | options list copy; no Map | Step 3 | REQ-104 | MapStruct/JSON test |
| CREATE | enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/common/validation/SpecCatalogQueryValidationTest.java | no Spec query types | required category constraint and query group propagation | DAO gets zero calls on invalid name | Step 3 | REQ-104 | Jakarta Validator/Mockito |
| CREATE | enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/application/goods/SpecManageImplTest.java | no SpecManage | application/domain converter calls and ordered result | empty result becomes [] | Step 3 | REQ-104 | JUnit/Mockito |
| CREATE | enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/goods/repo/SpecQueryRepositoryTest.java | no Spec query fixture | duplicate categories/templates/spec rows, tenant and deleted rows | distinct ordered SpecPO values | Step 3 | REQ-104 | H2 |
| CREATE | enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/goods/repo/SpecQuerySqlShapeTest.java | custom SpecDAO XML created in Step 1 | bind categoryName, active joins, tenant equality, order seq/id | no string interpolation or write query | Step 3 | REQ-104 | XML-shape test |
| CREATE | enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/goods/pojo/dto/SpecCategoryQueryDTO.java | no API-007 request DTO | mutable path input DTO with required category validation | path value converted once | Step 3 | REQ-104 | MVC/Validator |
| CREATE | enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/goods/SpecController.java | no Spec controller | GET API-007, bind path, call named Manage | no DAO access; 200 empty/populated ResultRecord | Step 3 | REQ-101/104 | MockMvc/OAS |
| CREATE | enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/goods/pojo/vo/SpecOptionVO.java | no Spec view type | immutable record with name and copied options list | Jackson schema is stable | Step 3 | REQ-104 | serialization/OAS |
| CREATE | enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/goods/pojo/convertor/SpecAdapterConverter.java | no Spec adapter mapping | BaseForwardConverter<SpecResult,SpecOptionVO> plus DTO→Query map | explicit fields, no unmapped target | Step 3 | REQ-104 | MapStruct compile |
| CREATE | enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/SpecListQuery.java | no application query | mutable typed categoryName Query | group-independent single handoff | Step 3 | REQ-104 | validation/compile |
| CREATE | enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/SpecResult.java | no application result | mutable typed name/options result | no PO or domain leak | Step 3 | REQ-104 | mapping/JSON |
| CREATE | enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/SpecManage.java | no application facade | named Query method contract | no new protocol or write method | Step 3 | REQ-104 | compile |
| CREATE | enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/SpecManageImpl.java | no application service | map Query→Domain Query, call service, map results | @Service name, @Slf4j, @RequiredArgsConstructor, Qualifier | Step 3 | REQ-104 | JUnit/wiring |
| CREATE | enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/converter/SpecManageConverter.java | no Manage/domain conversion | BaseForwardConverter<SpecListQuery,SpecDomainQuery> | normalize only outer whitespace | Step 3 | REQ-104 | MapStruct compile |
| CREATE | enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/converter/SpecResultConverter.java | no Domain/app conversion | BaseForwardConverter<SpecBO,SpecResult> | map exact result fields and options | Step 3 | REQ-104 | MapStruct test |
| CREATE | enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/SpecBO.java | no domain result | mutable BO with name/options/seq/id | no framework persistence type | Step 3 | REQ-104 | model/converter test |
| CREATE | enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/SpecDomainQuery.java | no domain query | categoryName with required constraint group | used for one tenant-scoped read | Step 3 | REQ-104 | validation group test |
| CREATE | enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/SpecDomainService.java | no Spec query contract | add listByCategoryName with ConvertGroup boundary | named Domain Service query, no persistence type | Step 3 | REQ-104 | compile/group test |
| CREATE | enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/repo/SpecRepository.java | no Spec repository | @Repository named query over SpecDAO, validate shared query group | XML access remains in Repository | Step 3 | REQ-104 | H2/Mockito |
| MODIFY | enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/architecture/LightPersistenceArchitectureTest.java | nine repositories and six DomainServiceImpl classes after Steps 1–2 | expect 10 repositories and seven DomainServiceImpl classes after Step 3 | preserve the Light persistence ownership guard | Step 3 | REQ-104/108 | module verify |
| CREATE | enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/service/impl/SpecPersistenceConverter.java | no Spec persistence mapping | one-way BaseForwardConverter<SpecPO,SpecBO>, split options once | never map inherited columns into BO | Step 3 | REQ-104 | MapStruct test |
| CREATE | enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/service/impl/SpecDomainServiceImpl.java | no Spec domain implementation | @Service named query; repository→BO converter | no DAO use; read-only transaction | Step 3 | REQ-104 | JUnit/wiring |
| MODIFY | enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/enums/GoodsErrorStatus.java | codes 610101–610105 and 610199 | append file codes 610106–610109 and catalog code 610110 | never renumber or reuse existing values | Step 3 | REQ-103–106 | unit tests |
| MODIFY | enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/handler/BaseExceptionHandler.java | current brand-only advice | route-aware 400 codes, 413 multipart and 503 storage mapping | Shoubing raw 401 bypasses MVC advice; grant denials happen upstream | Step 3 | REQ-103–106 | BaseExceptionHandlerTest |
| MODIFY | enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/handler/BaseExceptionHandlerTest.java | existing brand mappings | add catalog/file status table tests | preserve 610101 brand behavior | Step 3 | REQ-103–106 | JUnit/MockMvc |
| CREATE | enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/start/config/NativeHttpSecurityConfigurationTest.java | no precise CORS/upload policy test | anonymous POST denied; OPTIONS allowed/denied Origin headers | use existing MockMvc/session security context, no new test dependency | Step 4 | REQ-101/105 | MVC Security |
| MODIFY | enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/start/config/ProfileKeyParityTest.java | existing governance key parity | add cors/object-storage/multipart and five table-routing keys | no whole-YAML equality assertion | Step 4 | REQ-102/107 | JUnit/YAML |
| MODIFY | enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/start/config/NativeRpcConfigurationTest.java | existing Shoubing binding assertions | assert enabled is false in test and register-filter is false in all profiles | no Shoubing runtime connection | Step 4/6 correction | REQ-105/107 | Binder test |
| CREATE | enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/start/config/ObjectStorageConfigurationPropertiesTest.java | no storage properties | valid binding, required URI/keys, timeout ordering | no S3 request/network | Step 4 | REQ-105/107 | Binder/Validator |
| MODIFY | enjoyshop-service/enjoyshop-service-goods/pom.xml | Java 21/parent 5.4.1; no S3/Shoubing | import AWS BOM 2.55.6; add s3 and Shoubing starter 5.4.1 | no HTTP-client/test utility dependency | Step 4 | REQ-105/108 | dependency tree/compile |
| MODIFY | enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/start/config/NativeHttpSecurityConfiguration.java | anyRequest permitAll; no CORS source | exact CORS source and POST files authenticated matcher; others permitAll | Shoubing filter must register before authorization; no actor header trust | Step 4 | REQ-101/105 | security MVC test |
| CREATE | enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/start/config/ObjectStorageConfigurationProperties.java | no storage configuration object | validated app.object-storage endpoint/region/bucket/secret/public URL/timeouts | fields use URI, String, boolean, Duration | Step 4 | REQ-105/107 | Binder |
| CREATE | enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/start/config/ObjectStorageConfiguration.java | no S3Client Bean | named Spring S3Client with endpointOverride, region, path-style, credentials and 1 attempt | Bean owns SDK lifecycle; no provider branch | Step 4 | REQ-105/107 | property/config test |
| MODIFY | enjoyshop-service/enjoyshop-service-goods/src/main/resources/application.yml | no CORS/storage/multipart/catalog maps | same owned key tree, S3 defaults and five SINGLE/master_data tables | external origins/credentials remain values | Step 4 | REQ-101/102/105/107 | parity test |
| MODIFY | enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-dev.yml | disabled Shoubing and no new keys | matching config tree; environment-controlled auth filter; multipart 10/11MB | fail closed until configured | Step 4 | REQ-101/102/105/107 | parity test |
| MODIFY | enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-test.yml | H2/test profile and disabled Shoubing | matching config tree with non-secret test-only S3 values and no connections | unit tests mock S3 | Step 4 | REQ-101/102/105/107 | parity/binding |
| MODIFY | enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-prod.yml | production placeholders | same key paths and external secret placeholders | no secrets committed | Step 4 | REQ-101/102/105/107 | parity test |
| CREATE | enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/domain/goods/ProductImageValidatorTest.java | no image validator | JPEG/PNG magic and malformed/empty/oversize bytes | no filesystem or network | Step 5 | REQ-105/106 | JUnit |
| CREATE | enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/application/goods/FileManageImplTest.java | no FileManage | defensive Command→Domain conversion and Domain Service delegation | no SDK dependency or retry logic in Application | Step 5 | REQ-105/106 | Mockito |
| CREATE | enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/goods/service/impl/FileDomainServiceImplTest.java | no file domain flow | invalid signature/size fails before Port; valid image calls Port once | no transaction across object store | Step 5 | REQ-105/106 | JUnit/Mockito |
| CREATE | enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/goods/service/impl/S3CompatibleObjectStorageAdapterTest.java | no S3 Adapter | one PutObject, acknowledgement-only URL and failure classification | verify exact request and no retry loop | Step 5 | REQ-105 | Mockito |
| CREATE | enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/FileUploadCommand.java | no upload Command | Lombok class with bounded byte[] input and Validation | never carries MultipartFile/name/MIME | Step 5 | REQ-105/106 | compile/Validator |
| CREATE | enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/converter/FileUploadDomainConverter.java | no app/domain conversion | BaseForwardConverter<FileUploadCommand,ProductImageBO> | explicit defensive byte copy | Step 5 | REQ-105 | MapStruct test |
| CREATE | enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/FileManage.java | no upload use case | typed upload command→String contract | CQE Command, no event | Step 5 | REQ-105 | compile |
| CREATE | enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/FileManageImpl.java | no upload orchestrator | @Service upload Manage; validate/map then call Domain Service | stable Bean name, qualifiers, safe log metadata only | Step 5 | REQ-105/106 | Mockito/wiring |
| CREATE | enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/FileDomainService.java | no file domain port | upload ProductImageBO→stable URL contract | no SDK types | Step 5 | REQ-105 | compile |
| CREATE | enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/ObjectStoragePort.java | no storage port | putObject(bytes, detected format)→URL | app/domain do not depend on AWS SDK | Step 5 | REQ-105 | adapter contract test |
| CREATE | enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/ProductImageBO.java | no product image model | Lombok class with defensive bounded bytes | no caller metadata | Step 5 | REQ-105/106 | Validator/mapping |
| CREATE | enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/ProductImageFormat.java | no format enum | JPEG/PNG EgonEnum codes, MIME and extension values | internal code only; no persisted or JSON enum value | Step 5 | REQ-105/106 | JUnit |
| CREATE | enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/ProductImageValidator.java | no signature check | JDK signature detection returns format or typed request failure | never trust filename or Content-Type | Step 5 | REQ-105/106 | ProductImageValidatorTest |
| CREATE | enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/service/impl/FileDomainServiceImpl.java | no Domain Service implementation | validate ProductImageBO then Port call | @Service name, @Slf4j, Qualifier, RequiredArgsConstructor, no DB transaction | Step 5 | REQ-105 | Domain service test |
| MODIFY | enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/architecture/LightPersistenceArchitectureTest.java | seven DomainServiceImpl classes after Step 4 | expect eight after adding FileDomainServiceImpl | preserve the Light architecture ownership guard | Step 5 | REQ-105/108 | module verify |
| CREATE | enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/service/impl/S3CompatibleObjectStorageAdapter.java | no storage SDK adapter | S3Client PutObject, UUID key, server MIME/ext, stable public URL | adapter maps known failure vs unknown outcome; one attempt | Step 5 | REQ-105 | adapter test |
| CREATE | enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/goods/FileControllerWebTest.java | no file route | 400/401/413/201 scenarios; valid/invalid Shoubing Bearer filter fixture | deterministic JwtDecoder; upstream USER grant remains manual | Step 6 | REQ-105/106 | MockMvc |
| CREATE | enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/goods/FileOpenApiContractTest.java | no upload operation | multipart binary, bearer security, 201 Location, errors | Shoubing error response documented separately | Step 6 | REQ-105/107 | generated OpenAPI |
| CREATE | enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/goods/pojo/convertor/FileUploadAdapterConverterTest.java | no multipart converter | FileUploadDTO bytes map into FileUploadCommand; caller MIME/name not copied | no SDK/network | Step 6 | REQ-105/106 | MapStruct test |
| CREATE | enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/goods/pojo/dto/FileUploadDTO.java | no multipart DTO | mutable Adapter class holding MultipartFile only | @NotNull and OpenAPI binary field; size bounded before conversion | Step 6 | REQ-105/106 | MVC/serialization |
| CREATE | enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/goods/pojo/convertor/FileUploadAdapterConverter.java | no HTTP/app map | BaseForwardConverter<FileUploadDTO,FileUploadCommand> | read bytes; ignore original filename and declared MIME; preserve IO failure context | Step 6 | REQ-105/106 | converter test |
| CREATE | enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/goods/FileController.java | no API-008 route | multipart POST, Shoubing USER auth docs, ResultRecord<String> and 201 Location | no DAO/AWS calls in Controller | Step 6 | REQ-105/106 | MVC/OpenAPI |
| MODIFY | enjoyshop-service/enjoyshop-service-goods/README.zh-CN.md | current goods setup and verify commands | list CORS/Shoubing/S3 inputs and TEST-114 release gate | do not describe local object store container | Step 6 | REQ-101/105/107 | docs review |

## 6. Prerequisites, Constraints, and Plan Clarifications

### 6.1 Repository and worktree baseline

- Step baseline is main at commit 60d2fe3. The primary Spec and Plan remain user-approved untracked design inputs. Step 1 is now in progress with its declared source/test/DDL paths; keep both design documents out of implementation commits.
- Root reactor contains enjoyshop-service and enjoyshop-web; enjoyshop-service-goods is the existing Java 21/Boot 3.5.16 Egon Light module.
- Preserve unrelated sample/user/teaching code, GraphQL samples, RabbitMQ/Redis adapters and current brand behavior. Do not modify root/reactor POMs, brand table DDL, existing Manifest rows, Compose services or any application outside goods.
- Existing BrandControllerWebTest, BrandOpenApiContractTest, BrandQuerySqlShapeTest, BaseExceptionHandlerTest, ProfileKeyParityTest and NativeRpcConfigurationTest are extended where specified. Existing lombok.config already propagates Spring Qualifier; do not change it.
- Codegen owns only generated SpecPO.java. SpecDAO.java and SpecDAO.xml are custom because API-007 needs a named query. Never hand-edit SpecPO. Preserve codegen .egon ownership state and append, never rewrite, the SQL consumption log.

### 6.2 Build, test, and environment prerequisites

| Concern | Exact command/source | Required state | Validation boundary |
| --- | --- | --- | --- |
| Java/Maven | From goods module: java -version; ./mvnw -version | JDK 21 and wrapper available | tooling only |
| Focused Java tests | From goods module: run the exact focused -Dtest selector listed for each Step in §§7–8; for example ./mvnw -B -ntp -Dtest=CatalogDdlContractTest test | test profile uses H2 and disabled remote integrations | module unit/slice tests; no runtime proof |
| Architecture/full module | From enjoyshop-service/enjoyshop-service-goods: ./mvnw -B -ntp verify | local Maven repository can resolve approved SDK/starter artifacts | tests plus verify-bound Light architecture gate |
| AWS SDK resolution | From goods module: ./mvnw -B -ntp -Dincludes=software.amazon.awssdk:bom,software.amazon.awssdk:s3 dependency:tree | approved BOM 2.55.6 resolves | Maven dependency graph only |
| Shoubing artifact | From enjoyshop root after dependency is declared: ./mvnw -B -ntp -Dincludes=top.egon:egon-cola-tianquan-shoubing-starter dependency:tree | starter 5.4.1 is in configured artifact repository; if absent, build/install its existing source from Egon-COLA with -pl egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-starter -am install -DskipTests | dependency resolution only; no platform service starts |
| Native DDL generator | Follow sibling generator README from /Users/mario/SelfProject/Egon-COLA: build/install code-generator with -pl egon-cola-components/egon-cola-component-code-generator -am install -DskipTests; build its runtime dependency classpath to /tmp/egon-codegen.cp; prepend egon-cola-components/egon-cola-component-code-generator/target/classes to EGON_CODEGEN_CLASSPATH | Java and every classpath entry exist; if not, generator returns BLOCKED_TOOLING and apply stops | generator CLI only; no app/database starts |
| Generator invocation root | enjoyshop repository root: /Users/mario/SelfProject/Egon-COLA/scripts/egon-codegen.sh with codegen/catalog-lookup-codegen.json | DDL and Manifest are final before plan; SpecDAO.java exists for its existingTypeMappings declaration | Generator plan/check/apply; no Maven application start |
| Full module acceptance | From goods module: ./mvnw -B -ntp verify | Steps 1–6 committed; no runtime infra required by tests | compile/tests/architecture only; not PostgreSQL, Redis, OIDC issuer or MinIO/Ceph proof |
| Protected external gates | Deployment config and TEST-114 manual procedure in primary Spec §14/§16 | Shoubing Resource/JWK/Redis projections, valid USER grant, CORS origins, external object-store bucket/URL and catalog rows provisioned | release/runtime only; not run by this Plan |

### 6.3 Immutable constraints and approved decisions

- Never edit V20260913_001 or V20260923_001 or rewrite their Manifest prefix. Add exactly V20260929_001 and one matching SHA-256 entry.
- DDL is PostgreSQL and MP-SDJ/EgonColaPostgreDdlRunner managed; all five catalog tables route as SINGLE to master_data in all four profiles.
- Runtime is Java 21 + Spring Boot 3.5 only. The app stays in current goods Light. No separate service, local object-store container, frontend, file metadata table, CRUD management API, or file deletion endpoint.
- AWS SDK BOM/S3 2.55.6 and Shoubing starter 5.4.1 are explicitly accepted in primary DEC-107/110. Do not add URLConnection/Apache/security-test libraries. MinIO/Ceph are external; client calls stay behind ObjectStoragePort.
- Current GET /api/v1/brands retains no-category full/filter/page behavior and PageResultRecord. API-007 is a new read-only path; POST /api/v1/files is the only Shoubing-protected business route.
- CORS allowlist is not authentication. Product object GET is public under external bucket policy; upload is authenticated. No presigned URL.
- One Step/one semantic path-limited implementation commit. A future executor must not stage this Spec, this Plan, or unrelated paths.

### 6.4 Plan Clarifications

| ID | Small implementation inference | Repository evidence | Why semantics are unchanged | Impact if wrong |
| --- | --- | --- | --- | --- |
| PLAN-CLAR-001 | Test-only authenticated servlet requests use a MockHttpSession SecurityContext instead of spring-security-test | Existing goods test dependencies include Spring Boot Test/MockMvc but no spring-security-test | It asserts only the configured authenticated matcher; true OIDC/JWK/Redis Resource behavior remains TEST-114 | If MockHttpSession repository behavior differs, the security slice test must use an existing Spring Security servlet test utility; no runtime contract changes |

## 7. Ordered File-by-file Implementation Steps

### Step 1 — Create the managed catalog schema and Spec persistence contract

- Requirements: REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009, REQ-010, REQ-011, REQ-012, REQ-013, REQ-014, REQ-015, REQ-016, REQ-017, REQ-018, REQ-019, REQ-020, REQ-021, REQ-022 (accepted predecessor baseline invariants, already implemented and preserved), REQ-102, REQ-104, REQ-108
- Dependencies: None
- Baseline state: main manifest has 20260913_001 and 20260923_001; brand is SINGLE/master_data; there is no catalog table, SpecPO, SpecDAO or Spec mapper XML.
- Observable outcome: One V20260929_001 creates the five catalog tables and indexes; Manifest contains one actual checksum; custom SpecDAO/XML query contract exists; codegen owns only generated SpecPO.
- End state: CatalogDdlContractTest and the existing predecessor Brand/scaffold suite pass; architecture and mapper inventory guards expect 10 PO, 10 DAO and 10 mapper XML files; invalid Domain method arguments remain rejected through the Spring-managed Infrastructure proxy; the Egon architecture verifier reports zero violations; generator plan/apply/check have exact SpecPO-only scope; DDL consumption log has one append-only entry; no database was contacted.
- Test-first gate: Required — the new static DDL contract test initially fails because the versioned SQL/Manifest entry does not exist.
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-NAME-001, MC-MODEL-001, MC-CONVERT-001, MC-TIME-001, MC-CONFIG-001, MC-SCOPE-001, MC-TEST-001
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 11
- Approved execution corrections (2026-10-01): update the existing PO/DAO/Mapper XML count guards to 10; remove the pre-existing Spring `@Validated` annotation from the Domain interface while preserving Jakarta constraints and proving method validation through its already-validated Infrastructure implementation.
- Ordered files:

#### File 1 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/ddl/CatalogDdlContractTest.java`

- Purpose: Define the one-version catalog DDL contract before adding the script.
- Symbols: five table names; EgonModel column assertions; named foreign keys/indexes; Manifest script/hash assertion.
- Repository evidence: current migration metadata is enjoyshop-service/enjoyshop-service-goods/src/main/resources/db/egon-mp/repository-manifest.json; brand DDL tests already use JUnit in the goods module.
- Dependencies and consumers: Reads repository-relative SQL and JSON files; future MP-SDJ runner consumes the same Manifest.
- Why now: The test compiles without production types and proves the SQL/Manifest addition rather than depending on an external database.
- Contract/signature changes: None; test only.
- Input/output and state mapping: SQL bytes → parsed table/index names; Manifest path/version/hash → expected single appended version.
- Error and edge behavior: Missing file/entry, modified historical prefix/hash, omitted inherited column, FK/cascade or partial-index drift fail with a named assertion.
- Standards impact: MC-TEST-001, MC-SCOPE-001 — static repository check only; no test fixture connects to PostgreSQL.
- Literal rule enforcement: Rule 11 — target remains current Light master_data; no third schema system.
- Implementation pseudocode:


```java
read V20260929_001 SQL and repository-manifest.json as UTF-8
assert exactly category/template/spec/para/category_brand CREATE TABLE targets are present
assert id, tenant_id, audit, deleted_at and version columns per EgonModel contract
assert non-cascading ID foreign keys and both category_brand lifecycle/active unique indexes
compute SHA-256 from exact SQL bytes and compare only the new Manifest row
assert existing manifest rows/versions remain present and unchanged
```

- Verification contribution: RED before File 2 because the SQL and Manifest row are absent; GREEN after File 3 with exact byte hash.
- After this file: Test source compiles and fails only on absent next DDL/version artifacts.

#### File 2 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/resources/db/egon-mp/V20260929_001__initialize_catalog_lookup_schema.sql`

- Purpose: Add all five new read-model tables in one MP-SDJ-managed version.
- Symbols: category, template, spec, para, category_brand; category hierarchy/template/spec FKs; tenant guard; lifecycle indexes.
- Repository evidence: V20260923_001 is current latest and the Light profile stores managed DDL under src/main/resources/db/egon-mp.
- Dependencies and consumers: PostgreSQL Master Data role; Manifest/runner; generated SpecPO source model; custom joins.
- Why now: Test File 1 freezes exact table/index needs; existing brand is an FK target and is not altered.
- Contract/signature changes: No Java/public contract; create the selected physical schema only.
- Input/output and state mapping: template before category; category self-parent and optional template; spec/para require template; category_brand references category and existing brand.
- Error and edge behavior: One SQL version only; no seeds/backfill; FK actions restrict deletes; ACTOR-003 populates tenant-consistent rows; category name is not unique; active link uniqueness uses both the lifecycle key and PostgreSQL partial index.
- Standards impact: MC-ARCH-001, MC-REUSE-001, MC-SCOPE-001 — SQL stays in the existing Light managed-DDL path; no brand table edit.
- Literal rule enforcement: Rule 11 — PostgreSQL/MP-SDJ only; Rule 2 — DDL constraints mirror persistent shape, not business rules.
- Implementation pseudocode:


```sql
BEGIN ROLE-scoped MASTER_DATA branch
  CREATE template, category, spec, para, category_brand with inherited EgonModel columns
  CREATE non-cascading ID foreign keys after dependency tables exist
  CREATE category exact-name active index and spec tenant/template/seq/id active index
  CREATE category_brand unique (tenant_id, category_id, brand_id, deleted_at)
  CREATE active partial unique (tenant_id, category_id, brand_id) WHERE deleted_at IS NULL
END branch
do not CREATE/ALTER brand and do not INSERT data
```

- Verification contribution: Static DDL contract test plus generator parsing; PostgreSQL syntax/runtime stays a deployment gate.
- After this file: Five local schemas exist in source only; no physical database changed.

#### File 3 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/resources/db/egon-mp/repository-manifest.json`

- Purpose: Register the one new SQL version.
- Symbols: family=light; scripts append 20260929_001 with exact relative path and SHA-256.
- Repository evidence: Current manifest has 20260913_001 and 20260923_001; latest SQL bytes are immutable.
- Dependencies and consumers: MP-SDJ DDL runner and codegen manifest input.
- Why now: The checksum can only be recorded after File 2 final bytes are settled.
- Contract/signature changes: Add one scripts entry; preserve family and both existing entries byte-for-byte.
- Input/output and state mapping: new SQL bytes → SHA-256 → new manifest row; no checksum copy from a prior run.
- Error and edge behavior: Any future DDL edit requires recalculating this row before codegen; a stale or changed prefix is CHECKSUM_DRIFT.
- Standards impact: MC-REUSE-001, MC-SCOPE-001, MC-TEST-001 — use existing MP-SDJ manifest.
- Literal rule enforcement: Rule 11 — distribute DDL only through MP-SDJ runner; Rule 3 — generated PO derives from this DDL.
- Implementation pseudocode:


```json
read exact V20260929_001 file bytes
compute SHA-256
append version/path/hash after 20260923_001
retain existing family and every prior scripts field unchanged
```

- Verification contribution: CatalogDdlContractTest checks new digest and prior prefix.
- After this file: Generator input manifest is consistent and the DDL RED test should pass.

#### File 4 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/dao/SpecDAO.java`

- Purpose: Own one handwritten custom mapper method without taking ownership of generator-generated content.
- Symbols: SpecDAO extends EgonColaMapper<SpecPO>; listByCategoryName(String categoryName).
- Repository evidence: BrandDAO is the existing generic mapper with custom named methods; current custom SQL belongs in matching MyBatis XML.
- Dependencies and consumers: SpecRepository calls this DAO; generated SpecPO is produced later in this Step.
- Why now: Codegen config omits dao and mapper-xml artifacts, so this method remains explicitly custom.
- Contract/signature changes: Add listByCategoryName with @Param("categoryName"); no write API.
- Input/output and state mapping: validated exact categoryName bind → List<SpecPO> with inherited columns mapped by XML.
- Error and edge behavior: No direct result Map; empty rows return empty List; MyBatis failures propagate to the existing handler.
- Standards impact: MC-NAME-001, MC-VALID-001, MC-LOG-001, MC-BEAN-001, MC-SCOPE-001 — @Mapper, @Validated only for shape constraints, no business log/Bean constructor.
- Literal rule enforcement: Rule 1 — DAO suffix; Rule 2 — no duplicate category business validation at DAO; Rule 11 — Mapper remains Infrastructure.
- Implementation pseudocode:


```java
@Mapper interface SpecDAO extends EgonColaMapper<SpecPO> {
    List<SpecPO> listByCategoryName(@Param("categoryName") String categoryName);
}
```

- Verification contribution: Query XML test in Step 3 binds this named method; Step 1 compile occurs after File 7 generates SpecPO.
- After this file: Custom query signature exists but compile waits for the generated PO.

#### File 5 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/resources/mybatis/mapper/goods/SpecDAO.xml`

- Purpose: Implement API-007 join and result mapping outside the generator-owned file scope.
- Symbols: SpecPO resultMap; listByCategoryName mapped statement.
- Repository evidence: BrandDAO.xml is current same-module resultMap/typed SQL convention.
- Dependencies and consumers: SpecDAO namespace from File 4; tables from File 2; SpecPO from File 7.
- Why now: Exact SQL is needed for the generic Mapper custom method requested by the source.
- Contract/signature changes: One custom read-only SELECT; the XML also supplies the three standard lifecycle statements required by EgonColaMapper (`selectActiveById`, `selectActiveByIds`, `deleteVersionedById`), with no custom business write method.
- Input/output and state mapping: query.categoryName exact match across active same-name categories; join category.template/spec on IDs; enforce tenant equality and active rows; SELECT DISTINCT SpecPO fields; ORDER BY seq,id.
- Error and edge behavior: no matching template/spec gives empty list; no duplicate SpecPO if same template is reached from multiple categories; use bind parameters only.
- Standards impact: MC-VALID-001, MC-SCOPE-001, MC-TEST-001 — SQL-shape/H2 tests prove predicates and order.
- Literal rule enforcement: Rule 2 — DAO only receives database query object and binds values; Rule 11 — keep custom XML in goods Infrastructure.
- Implementation pseudocode:


```xml
SELECT DISTINCT spec columns
  FROM category c
  JOIN template t ON t.id=c.template_id AND t.tenant_id=c.tenant_id
  JOIN spec s ON s.template_id=t.id AND s.tenant_id=t.tenant_id
 WHERE c.name = #{categoryName}
   AND c.deleted_at IS NULL AND t.deleted_at IS NULL AND s.deleted_at IS NULL
   AND c.tenant_id=s.tenant_id
 ORDER BY s.seq, s.id
```

- Verification contribution: SpecQuerySqlShapeTest later asserts binding, tenant/soft-delete joins, DISTINCT/order; no untrusted string interpolation.
- After this file: Custom query SQL exists but no Domain Service/Repository wiring yet.

#### File 6 — `CREATE enjoyshop-service/enjoyshop-service-goods/codegen/catalog-lookup-codegen.json`

- Purpose: Select the narrow native Light generated catalog artifact.
- Symbols: configVersion 1, projectType light, outputRoot goods module, manifest input, logicalTables=["spec"], artifacts=["po"], existingTypeMappings.dao = SpecDAO FQCN.
- Repository evidence: brand-codegen.json is current format/config convention; sibling generator README requires explicit logicalTables and v2 generated plan state.
- Dependencies and consumers: egon-codegen.sh plan/apply/check from enjoyshop repository root; reads File 2/3.
- Why now: SpecDAO/XML already exist as custom files, so the config records the complete generated/non-generated boundary.
- Contract/signature changes: Generator owns SpecPO only; do not select mapper-xml/dao artifacts.
- Input/output and state mapping: Manifest through 20260929_001 → spec schema → SpecPO.java.
- Error and edge behavior: stale checksum or missing SpecDAO mapping blocks plan/apply; CONFLICT is resolved by returning to config/path review, never by hand patching generator state.
- Standards impact: MC-DEP-001, MC-REUSE-001, MC-SCOPE-001 — use the verified sibling CLI and no new application dependency.
- Literal rule enforcement: Rule 3 — PO follows EgonModel generated contract; Rule 11 — projectType remains light.
- Implementation pseudocode:


```json
configVersion = 1
projectType = "light"
input.mode = "manifest"
logicalTables = ["spec"]
artifacts = ["po"]
existingTypeMappings.dao = "top.egon.enjoyshop.goods.infrastructure.goods.dao.SpecDAO"
```

- Verification contribution: Codegen plan output is reviewed for only SpecPO; check after apply must show no drift.
- After this file: Generator plan has a bounded, reviewed input.

#### File 7 — `GENERATED enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/po/SpecPO.java`

- Purpose: Generate the persistence shape for spec.
- Symbols: SpecPO extends EgonModel<SpecPO>; only business fields from spec columns.
- Repository evidence: BrandPO extends EgonModel and declares business columns; generator README defines generated native Light PO.
- Dependencies and consumers: SpecDAO/XML, SpecPersistenceConverter, MP-SDJ.
- Why now: Codegen requires the final SQL and manifest from Files 2–3 and reviewed config from File 6.
- Contract/signature changes: Generator-created entity only; inherited id/tenant/audit/deletedAt/version are not redeclared.
- Input/output and state mapping: DDL name/options/seq/template_id → camelCase fields and generated null/size constraints.
- Error and edge behavior: do not manually edit generated body; a changed output with no matching state is a CONFLICT.
- Standards impact: MC-NAME-001, MC-MODEL-001, MC-VALID-001, MC-SCOPE-001 — class/Lombok/EgonModel requirements come from DDL/template.
- Literal rule enforcement: Rule 1 — PO suffix; Rule 2 — generated column shape validation; Rule 3 — class/EgonModel conventions; Rule 11 — current Infrastructure path.
- Implementation pseudocode:


```java
run codegen plan; verify only SpecPO is ADD and no existing file is changed
apply the emitted plan id with the same config/DDL fingerprints
never hand-write or patch SpecPO
```

- Verification contribution: codegen check plus module compile.
- After this file: SpecDAO/XML type reference compiles and later query converters can be built.

#### File 8 — `MODIFY docs/egon/codegen/ddl-consumption-log.md`

- Purpose: Append the required record after successful codegen apply.
- Symbols: one Asia/Shanghai entry; profile light; output root; throughVersion 20260929_001; Manifest-order SQL content and SHA-256.
- Repository evidence: Existing file explicitly says append-only classpath SQL log and has the prior goods generation entry.
- Dependencies and consumers: Codegen apply completed; future maintainers review exact source SQL.
- Why now: The generator skill requires journaling after apply.
- Contract/signature changes: Append one complete entry only; do not rewrite prior log.
- Input/output and state mapping: Manifest-consumed scripts through new version → journal text.
- Error and edge behavior: if apply fails, do not append; if only plan is authorized in a future run, record only when required by generator workflow.
- Standards impact: MC-SCOPE-001, MC-TEST-001 — documentation only; no claim DDL executed.
- Literal rule enforcement: Rule 11 — documents managed DDL ownership and generator-produced SpecPO.
- Implementation pseudocode:


```text
if apply status is APPLIED:
    append timestamp/profile/root/throughVersion
    append each consumed Manifest version, relative path, SHA-256 and full SQL
preserve all earlier records
```

- Verification contribution: git diff review confirms append-only record; CatalogDdlContractTest still checks SQL/Manifest only.
- After this file: The generator input journal is complete; the two architecture inventory guards below are aligned with the newly required persistence artifacts before Step 1 validation.

#### File 9 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/architecture/LightPersistenceArchitectureTest.java`

- Purpose: Keep the Light architecture inventory test accurate after adding the planned Spec persistence type and mapper.
- Symbols: expected `PO.java` and `DAO.java` counts change from 9 to 10; repository and Domain Service counts remain unchanged.
- Repository evidence: The full goods verify reported `expected: <9> but was: <10>` at the PO count; source discovery also found 10 DAO files after Step 1.
- Dependencies and consumers: Verifies the source tree against this existing generated-project architecture contract.
- Why now: SpecPO and SpecDAO are explicit Step 1 artifacts; preserving the old fixed counts would make the required full-module verification fail despite a compliant tree.
- Contract/signature changes: Test expectation only; no product behavior or Java API changes.
- Input/output and state mapping: goods `src/main/java` type inventory → exact expected counts of 10 PO and 10 DAO files.
- Error and edge behavior: Any missing or extra inventory artifact continues to fail the architecture guard.
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001 — retains the exact Light structure while updating its expected catalog inventory.
- Literal rule enforcement: Rule 11 — asserts the selected Light persistence structure remains in place.
- Implementation pseudocode:


```java
javaFiles = walk(Path.of("src/main/java")) filtered to Java sources
assert exactly 10 files end with PO.java and exactly 10 end with DAO.java
keep all other architecture assertions unchanged
```

- Verification contribution: `./mvnw -B -ntp verify` must pass `LightPersistenceArchitectureTest`.
- After this file: The static Light architecture inventory accepts the required SpecPO/SpecDAO additions.

#### File 10 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/config/datasource/RepositoryPersistenceContractTest.java`

- Purpose: Keep the existing all-mapper lifecycle contract test accurate after adding the custom Spec mapper XML.
- Symbols: XML inventory expectation changes from 9 to 10; all lifecycle statement and safety assertions remain unchanged.
- Repository evidence: The full goods verify reported `Expected size: 9 but was: 10` for the mapper XML list, including the new SpecDAO.xml.
- Dependencies and consumers: Reads all `DAO.xml` files under `src/main/resources/mybatis/mapper` and verifies standard active-read/versioned-delete contracts.
- Why now: The new SpecDAO.xml intentionally implements all three EgonColaMapper-required lifecycle statements plus its custom read query.
- Contract/signature changes: Test expectation only; no repository or Mapper behavior changes.
- Input/output and state mapping: mapper XML file inventory → exact expected size 10, then retain the existing per-file statement checks.
- Error and edge behavior: Missing lifecycle statements, unsafe substitutions, or an unexpected mapper count still fail the test.
- Standards impact: MC-REUSE-001, MC-SCOPE-001, MC-TEST-001 — confirms EgonColaMapper reuse and its XML contract across the module.
- Literal rule enforcement: Rule 11 — keeps persistence ownership on the existing Light/MyBatis path.
- Implementation pseudocode:


```java
mappingRoot = Path.of("src/main/resources/mybatis/mapper")
mappings = all files under mappingRoot whose name ends in DAO.xml
assert mappings.size() == 10; retain each existing lifecycle and tenant-safety assertion
```

- Verification contribution: `./mvnw -B -ntp verify` must pass `RepositoryPersistenceContractTest`.
- After this file: The per-mapper lifecycle contract covers the newly registered Spec mapper XML.

#### File 11 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/start/StudentManagementApplicationTest.java`

- Purpose: Prove Spring method validation still rejects invalid Domain Service arguments after removing a framework annotation from the Domain interface.
- Symbols: inject the existing named `brandDomainService` bean; call `delete(null, -1L)` and assert two parameter violations.
- Repository evidence: `BrandDomainService` declares Jakarta `@NotNull`, `@Positive` and `@Min`; `BrandDomainServiceImpl` is the Spring Bean and already carries `@Validated`.
- Dependencies and consumers: Reuses the existing `@SpringBootTest` and H2 test profile; validation must stop the call before repository access.
- Why now: The bytecode architecture gate identified the interface-level Spring annotation; this test protects the existing runtime behavior while that technical dependency is removed.
- Contract/signature changes: None; test only.
- Input/output and state mapping: invalid `delete` arguments → `ConstraintViolationException`; repository is not invoked.
- Error and edge behavior: Both null id and negative expected version are rejected by Jakarta constraints inherited from the interface method.
- Standards impact: MC-VALID-001, MC-ARCH-001, MC-TEST-001 — verifies executable validation and the Domain/Infrastructure dependency boundary.
- Literal rule enforcement: Rule 2 — Jakarta constraints remain active at the Spring-managed boundary; Rule 11 — no new DDD layer is introduced.
- Implementation pseudocode:


```java
inject BrandDomainService with @Qualifier("brandDomainService") from the test application context
invoke delete(null, -1L) through the Spring proxy and assert ConstraintViolationException
assert its violation set has two entries, before any repository access can occur
```

- Verification contribution: Run the focused test before and after File 12; include it in goods `verify`.
- After this file: The method-validation preservation contract is fixed before the Domain annotation is removed.

#### File 12 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/BrandDomainService.java`

- Purpose: Remove the existing Spring technical dependency from the Domain contract so the selected DDD architecture gate passes.
- Symbols: remove the `org.springframework.validation.annotation.Validated` import and interface annotation only.
- Repository evidence: The initial commit contains this annotation and verify reported `ARCH-002`; the Spring-managed `BrandDomainServiceImpl` already has `@Validated`.
- Dependencies and consumers: Existing Application/Infrastructure callers keep the same interface; no dependency or public method signature changes.
- Why now: Full verify requires zero Egon-COLA architecture violations; preserving method validation is covered by File 11.
- Contract/signature changes: None; retain every Jakarta parameter/cascade constraint and every method signature.
- Input/output and state mapping: Spring proxy continues to derive method constraints from the interface while activation remains on the named Infrastructure implementation.
- Error and edge behavior: Invalid arguments still raise `ConstraintViolationException` before repository work; the Domain package no longer imports Spring validation types.
- Standards impact: MC-ARCH-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001 — removes the proven technical edge and verifies its validation boundary.
- Literal rule enforcement: Rule 2 — keep Jakarta validation constraints active; Rule 11 — keep the current Light DDD package structure.
- Implementation pseudocode:


```java
remove only import org.springframework.validation.annotation.Validated
remove only @Validated from the BrandDomainService interface
preserve every jakarta.validation constraint/signature and keep @Validated on BrandDomainServiceImpl
```

- Verification contribution: Focused `StudentManagementApplicationTest` must pass and `./mvnw -B -ntp verify` must report zero architecture violations.
- After this file: The DDD contract is Jakarta-only and Spring method validation stays enabled at its managed Infrastructure boundary.

- Validation working directory: enjoyshop repository root for generator commands; goods module root for JUnit/compile.
- Verification command: From enjoyshop root with EGON_CODEGEN_CLASSPATH present, run /Users/mario/SelfProject/Egon-COLA/scripts/egon-codegen.sh plan --config enjoyshop-service/enjoyshop-service-goods/codegen/catalog-lookup-codegen.json; inspect its final JSON line and set CODEGEN_PLAN_ID to the emitted planId; run the same Bash script with apply --plan "$CODEGEN_PLAN_ID" --config enjoyshop-service/enjoyshop-service-goods/codegen/catalog-lookup-codegen.json and then check --config. From the goods module, run ./mvnw -B -ntp -Dtest=CatalogDdlContractTest,StudentManagementApplicationTest test, ./mvnw -B -ntp -DskipTests compile, and ./mvnw -B -ntp verify.
- Expected result: initial DDL test fails only while its script/Manifest input is absent; then codegen plan lists one SpecPO ADD and zero conflict/destructive actions; apply reports APPLIED; check reports no drift; focused tests, compile and verify exit 0; architecture inventory counts are 10 PO/DAO/XML; invalid Domain Service parameters still fail before repository access; Egon architecture verify reports zero violations; the verify-bound architecture test preserves predecessor REQ-001–022.
- Failure returns to: DDL/table contract and manifest checksum (CHECKSUM_DRIFT); custom paths/config if CONFLICT; generator tool/classpath prerequisite if BLOCKED_TOOLING.
- Completion criteria: one next SQL version, one Manifest row, exact SpecPO generation, custom DAO/XML intact, journal appended, architecture inventory guards aligned to 10 PO/DAO/XML, Domain contract has no Spring dependency while proxy validation remains tested, previous scaffold/brand requirements still pass, focused test/compile/verify green.
- Rollback: revert only this Step's uncommitted/path-scoped files before commit. After commit or database apply, keep SQL/manifest immutable and use a later accepted forward migration; never drop tables or edit an old version.
- Commit paths: enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/ddl/CatalogDdlContractTest.java; enjoyshop-service/enjoyshop-service-goods/src/main/resources/db/egon-mp/V20260929_001__initialize_catalog_lookup_schema.sql; enjoyshop-service/enjoyshop-service-goods/src/main/resources/db/egon-mp/repository-manifest.json; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/dao/SpecDAO.java; enjoyshop-service/enjoyshop-service-goods/src/main/resources/mybatis/mapper/goods/SpecDAO.xml; enjoyshop-service/enjoyshop-service-goods/codegen/catalog-lookup-codegen.json; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/po/SpecPO.java; docs/egon/codegen/ddl-consumption-log.md; enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/architecture/LightPersistenceArchitectureTest.java; enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/config/datasource/RepositoryPersistenceContractTest.java; enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/start/StudentManagementApplicationTest.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/BrandDomainService.java
- Commit: feat(goods): add managed catalog schema



### Step 2 — Extend API-001 with tenant-safe category filtering

- Requirements: REQ-101, REQ-103, REQ-108
- Dependencies: Step 1 committed
- Baseline state: API-001 uses BrandListQuery→BrandPageQuery→BrandDomainQuery→BrandRepository→BrandDAO; list/count contain name/letter/seq predicates but no category relation.
- Observable outcome: GET /api/v1/brands?categoryName=phones returns the existing PageResultRecord with exact category filter; absent categoryName is behavior-compatible.
- End state: Same-name active categories are unioned with EXISTS; brand rows are not duplicated; current tenant and active-row rules remain; invalid names fail before DAO.
- Test-first gate: Required — after compile-only type signatures and the Validator/MapStruct stubs, MVC binding/mapping, Domain/Repository groups, Manage propagation, H2 results and SQL-shape assertions must show the missing behavior before their implementation files change.
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-JSON-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 11
- Approved execution correction (2026-10-02): reorder Step 2 so compile-only contracts and RED tests precede behavior implementation; keep a temporary MapStruct ignore solely to satisfy the existing unmapped-target compile gate; add the existing BrandManageImplTest to prove App→Domain propagation; conform the custom ConstraintValidator to the repository's BaseValidator architecture guard. No route, response, persistence scope or business semantics change.
- Ordered files:

#### File 1 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/goods/pojo/dto/BrandListQuery.java`

- Purpose: Add the optional categoryName component to the existing public API-001 query carrier.
- Symbols: immutable `BrandListQuery.categoryName` with optional Jakarta constraint and OpenAPI metadata.
- Repository evidence: BrandListQuery is the existing immutable record bound by BrandController; BrandPageQuery is a mutable Lombok Query already carrying name/letter/PageQuery.
- Dependencies and consumers: BrandAdapterConverter maps request to BrandPageQuery; BrandManageImpl maps to BrandDomainQuery.
- Why now: Tests must compile against the exact fields before proving the missing propagation.
- Contract/signature changes: append one nullable record component; preserve the six existing components and their order.
- Input/output and state mapping: absent stays null; present value is constrained and later outer-trimmed.
- Error and edge behavior: only supplied blank/trimmed length >120 fails; absent remains valid.
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-JSON-001 — BrandListQuery remains an immutable scalar value record; BrandPageQuery remains a mutable class.
- Literal rule enforcement: Rule 1 — Query semantic suffix; Rule 2 — optional constraint contract; Rule 3 — record only for immutable value object, class/Lombok for app Query; Rule 6 — OpenAPI schema field; Rule 11 — preserve existing layers.
- Implementation pseudocode:


```java
record BrandListQuery(existing six components,
    @Schema(description = "精确匹配的商品分类名称", maxLength = 120)
    @ValidCategoryName(required = false) String categoryName)
```

- Verification contribution: Makes MVC/captor tests compile; does not make request mapping GREEN.
- After this file: new query fields exist, converter still leaves app value null.
#### File 2 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/BrandPageQuery.java`

- Purpose: Carry the validated optional categoryName through the existing mutable Application Query.
- Symbols: `BrandPageQuery.categoryName` with the existing Lombok class baseline.
- Repository evidence: current `BrandPageQuery` holds name, letter and PageQuery; Manage receives it through `@Valid`.
- Dependencies and consumers: `BrandAdapterConverter` creates it; `BrandManageImpl` carries it into `BrandDomainQuery`.
- Why now: Establish the destination field before the converter/Manage RED assertions.
- Contract/signature changes: add one nullable String field; preserve page defaults and existing fields.
- Input/output and state mapping: absent remains null; supplied value is validated at the Application method boundary.
- Error and edge behavior: blank and trimmed length >120 fail; null leaves category filtering inactive.
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-SCOPE-001 — keep the mutable class and pagination behavior.
- Literal rule enforcement: Rule 1 — retain Query suffix; Rule 2 — apply Jakarta constraint at the existing validated boundary; Rule 3 — mutable class/Lombok; Rule 11 — preserve architecture.
- Implementation pseudocode:

```java
@Data @NoArgsConstructor @AllArgsConstructor @Accessors(chain = true) @Builder
private String categoryName annotated @ValidCategoryName(required = false)
keep PageQuery default and every existing filter field unchanged
```

- Verification contribution: Makes MVC, Manage and Domain-query RED tests compile.
- After this file: the Application Query has the new nullable field; no converter behavior has changed.
#### File 3 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/validation/GoodsCatalogQueryValidationGroup.java`

- Purpose: Define the reusable group/constraint metadata used by later boundary files.
- Symbols: marker interface `GoodsCatalogQueryValidationGroup` only.
- Repository evidence: no existing catalog constraint; goods already has starter-validation/Jakarta Validation.
- Dependencies and consumers: BrandDomainQuery and SpecDomainQuery use the group; DTO/app query fields use optional/required constraint modes.
- Why now: query types and failing tests need an exact annotation/group contract.
- Contract/signature changes: add one marker group; the constraint type is declared by the next file.
- Input/output and state mapping: later catalog Domain constraints opt into this group; Application/API DTO constraints remain in Default.
- Error and edge behavior: internal whitespace preserved; no mutation in validator.
- Standards impact: MC-VALID-001, MC-NAME-001, MC-UTIL-001 — constraint uses only Jakarta/JDK.
- Literal rule enforcement: Rule 1 — named ValidationGroup/constraint; Rule 2 — custom constraint and group; Rule 5 — no utility dependency; Rule 11 — common package remains inside Light.
- Implementation pseudocode:


```java
public interface GoodsCatalogQueryValidationGroup {}
use this group for the optional categoryName constraint on reused BrandDomainQuery
convert Default into this group only at Domain Service and Repository @Valid boundaries
```

- Verification contribution: compile prerequisite for the constraint annotation and grouped Domain query.
- After this file: the explicit catalog validation group exists without runtime behavior.
#### File 4 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/validation/ValidCategoryName.java`

- Purpose: Define one Jakarta constraint shared by optional API-001 and required catalog queries.
- Symbols: `@ValidCategoryName(required)` and standard `message`, `groups`, `payload` members.
- Repository evidence: no reusable category constraint.
- Dependencies and consumers: the adjacent current-layer caller and consumer in Step 2.
- Why now: follows its compile prerequisite and precedes the next consumer.
- Contract/signature changes: create the constraint annotation with a required flag; null and blank behavior stays in its ConstraintValidator.
- Input/output and state mapping: `required=false` permits null; `required=true` rejects null; non-null strings are checked using a trimmed view.
- Error and edge behavior: internal whitespace is preserved; this annotation never mutates input.
- Standards impact: MC-VALID-001, MC-NAME-001, MC-UTIL-001 — Jakarta metadata plus one stateless validator.
- Literal rule enforcement: Rule 1 — constraint annotation has a semantic name; Rule 2 — use Jakarta validation; Rule 5 — JDK String only; Rule 11 — common package stays inside Light.
- Implementation pseudocode:

```java
@Constraint(validatedBy = ValidCategoryNameValidator.class)
@Target({ FIELD, PARAMETER, RECORD_COMPONENT, ANNOTATION_TYPE }) @Retention(RUNTIME) @Documented
declare required() plus Jakarta message, groups and payload members; do not normalize the input
```

- Verification contribution: validator and MVC tests consume the same optional/required annotation.
- After this file: query DTOs compile against a single shared constraint contract.
#### File 5 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/validation/ValidCategoryNameValidator.java`

- Purpose: Provide the ConstraintValidator compile contract and satisfy the existing common Validator hierarchy before behavior tests run.
- Symbols: `extends BaseValidator`, `implements ConstraintValidator<ValidCategoryName,String>`, `getValidationUtils`, `initialize`, `isValid`.
- Repository evidence: `ArchetypeContractConvergenceTest` requires every handwritten `*Validator.java` to extend `BaseValidator` and expose `getValidationUtils`; starter-validation supplies Jakarta Validator.
- Dependencies and consumers: annotation from File 4 and the DTO/Query fields; common `ValidationUtils` is used only by the inherited contract accessor.
- Why now: Test-first requires a runnable true stub rather than a missing type or a verifier failure.
- Contract/signature changes: implement required state and return true temporarily; File 15 adds the category predicate.
- Input/output and state mapping: nullable/raw category name→boolean; `required` is stored from the annotation.
- Error and edge behavior: the true stub deliberately accepts blank and long input so the RED assertions fail on behavior.
- Standards impact: MC-ARCH-001, MC-VALID-001, MC-UTIL-001 — retain the shared BaseValidator contract; the custom ConstraintValidator performs its one rule directly with JDK String operations.
- Literal rule enforcement: Rule 2 — use Jakarta ConstraintValidator; Rule 5 — no new utility library; Rule 11 — keep common validation inside the selected Light module.
- Implementation pseudocode:


```text
initialize(annotation): store annotation.required()
getValidationUtils(): return the existing lazy common ValidationUtils facade required by BaseValidator
isValid(value): return true // compile-only stub; File 15 replaces this with the category predicate
```

- Verification contribution: enables CategoryNameValidatorTest to assert the exact failing cases.
- After this file: test compiles but blank/overlong negative cases fail as expected.
#### File 6 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/BrandDomainQuery.java`

- Purpose: Add the category field and its catalog-group constraint to the reused Domain query.
- Symbols: `BrandDomainQuery.categoryName`; the Default→catalog ConvertGroup boundaries are added by File 16 and File 20.
- Repository evidence: current BrandDomainQuery is mutable with nested @Valid PageQuery; DomainService has listPage/countPage.
- Dependencies and consumers: BrandManageImpl, DomainServiceImpl and BrandRepository.
- Why now: the group test must compile and observe missing proxy propagation before service/repository conversions are added.
- Contract/signature changes: add optional field with `@ValidCategoryName(required=false, groups=GoodsCatalogQueryValidationGroup.class)`; preserve PageQuery.
- Input/output and state mapping: normalized BrandPageQuery→DomainQuery; query passes through Domain Service to Repository.
- Error and edge behavior: same object is revalidated under the same named group before service and repository SQL.
- Standards impact: MC-VALID-001, MC-MODEL-001, MC-NAME-001 — existing mutable class/Lombok and service signatures stay stable.
- Literal rule enforcement: Rule 2 — ConvertGroup for reused object; Rule 3 — mutable class unchanged; Rule 11 — preserve Domain location.
- Implementation pseudocode:


```text
add categoryName to the existing mutable BrandDomainQuery
apply ValidCategoryName(required=false, groups=GoodsCatalogQueryValidationGroup.class)
leave service/repository ConvertGroup absent until the group RED test observes the gap
```

- Verification contribution: GoodsCatalogQueryValidationTest compiles and remains RED until method boundaries convert Default to the catalog group.
- After this file: the query carries a grouped category constraint; the routing boundary behavior is not yet GREEN.
#### File 7 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/goods/pojo/convertor/BrandAdapterConverter.java`

- Purpose: Keep MapStruct compilation green while the new BrandPageQuery target field is in place but behavior tests have not yet passed.
- Symbols: `BrandAdapterConverter#toPageQuery`; temporary `@Mapping(target = "categoryName", ignore = true)`.
- Repository evidence: The mapper declares `unmappedTargetPolicy = ReportingPolicy.ERROR`, so the new target field requires an explicit compile-safe mapping before the RED tests can run.
- Dependencies and consumers: BrandControllerWebTest captures the BrandPageQuery passed to the mocked BrandManage.
- Why now: This is a compile prerequisite only; preserving null makes the MVC captor test fail on the missing category propagation.
- Contract/signature changes: No public type or HTTP contract change; the ignore mapping is replaced by the final named normalization in File 17.
- Input/output and state mapping: supplied categoryName deliberately remains null at this checkpoint; absent categoryName remains unchanged.
- Error and edge behavior: No validation or normalization behavior is added here; the explicit validator stub keeps the request path observable.
- Standards impact: MC-CONVERT-001, MC-SCOPE-001, MC-TEST-001 — preserve the existing MapStruct BaseForwardConverter and avoid an unmapped-target compile failure.
- Literal rule enforcement: Rule 3 — retain MapStructPlus/BaseForwardConverter; Rule 11 — keep the Adapter mapping in its current layer.
- Implementation pseudocode:


```java
add @Mapping(target = "categoryName", ignore = true) to toPageQuery
compile with unmappedTargetPolicy = ReportingPolicy.ERROR while downstream categoryName fields exist
run the MVC captor RED test, then replace the ignore with the named trim mapping in File 17
```

- Verification contribution: Allows the focused tests to compile; the MVC captor still observes null until File 17.
- After this file: the compile-only map preserves prior behavior and makes the missing propagation testable.
#### File 8 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/common/validation/ValidCategoryNameValidatorTest.java`

- Purpose: Prove optional/required and trimming behavior before GREEN implementation.
- Symbols: acceptsNullWhenOptional; rejectsNullWhenRequired; rejectsBlank; countsTrimmedLength; accepts120.
- Repository evidence: current tests use JUnit 5 and Spring/Jakarta validation.
- Dependencies and consumers: ValidCategoryName annotation and stub validator.
- Why now: Fixes the shared public validation rule before converter/SQL changes.
- Contract/signature changes: test only.
- Input/output and state mapping: raw string→ConstraintViolation set.
- Error and edge behavior: outer spaces are ignored for length; internal spaces remain valid.
- Standards impact: MC-VALID-001, MC-TEST-001 — positive, negative and required-mode coverage.
- Literal rule enforcement: Rule 2 — custom annotation exercised through Validator; Rule 11 — test remains under goods.
- Implementation pseudocode:


```java
Validator.validate("  phone  ", optional) is valid
Validator.validate(" ", optional) is invalid
Validator.validate(null, required) is invalid
Validator.validate("x".repeat(121), optional) is invalid
Validator.validate("x".repeat(121), optional) uses exact trimmed length
```

- Verification contribution: RED against the true stub; GREEN after File 5.
- After this file: failure is expected only for the absent name predicate.
#### File 9 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/common/validation/GoodsCatalogQueryValidationTest.java`

- Purpose: Prove both reused-query method-validation boundaries before adding their group conversions.
- Symbols: invalid category rejected at DomainService and direct Repository entry; optional null remains allowed.
- Repository evidence: BrandDomainQuery carries the catalog-group constraint, but its Domain Service/Repository parameters do not convert groups yet.
- Dependencies and consumers: group marker, constraint, Validator stub and BrandDomainQuery field from Files 3–6.
- Why now: DomainQuery is reused across two validation boundaries and each must stop an invalid value before mapper access.
- Contract/signature changes: test only.
- Input/output and state mapping: grouped query→Validator/proxy method outcome.
- Error and edge behavior: overlong category must fail before the Domain method/repository; direct Repository invocation must also fail before BrandDAO; null optional category remains valid.
- Standards impact: MC-VALID-001, MC-TEST-001 — assert zero DAO/repository calls on invalid query.
- Literal rule enforcement: Rule 2 — reused query uses an explicit group; Rule 11 — current domain/repository layers.
- Implementation pseudocode:


```java
inject the Spring-managed Domain Service and Repository; replace brandDAO with a Mockito test bean
invoke listPage and selectBrandPage with a BrandDomainQuery whose categoryName has 121 characters
assert both calls throw ConstraintViolationException and verify brandDAO has no interactions
```

- Verification contribution: RED before Domain Service and Repository ConvertGroup wiring; no database is required.
- After this file: group propagation failures are observable at both boundaries.
#### File 10 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/goods/BrandControllerWebTest.java`

- Purpose: Prove the new optional query binding and validation without weakening any existing API-001 scenario.
- Symbols: capture `BrandPageQuery.categoryName`; reject blank and trimmed length 121; keep absent category optional.
- Repository evidence: existing API-001 MockMvc and BrandManage fixture.
- Dependencies and consumers: `BrandController`, `BrandAdapterConverterImpl`, mocked BrandManage and BrandPageQuery.
- Why now: all query types and the validator stub compile; tests must observe null mapping and accepted invalid values before behavior implementation.
- Contract/signature changes: test only; preserve existing route, response and page modes.
- Input/output and state mapping: HTTP categoryName → captured Application Query; omitted input → null.
- Error and edge behavior: blank/overlong values must produce 400/610101; no category continues full-mode behavior.
- Standards impact: MC-VALID-001, MC-JSON-001, MC-TEST-001, MC-SCOPE-001 — checks boundary validation and the existing envelope.
- Literal rule enforcement: Rule 2 — validate the external handoff; Rule 6 — preserve Jackson response shape; Rule 11 — keep the same Light Adapter.
- Implementation pseudocode:

```java
perform GET /api/v1/brands?categoryName=phones and capture BrandPageQuery
assert the temporary ignore leaves categoryName null, producing the planned RED
perform blank and 121-character requests; the Validator stub must expose the missing rejection
```

- Verification contribution: focused MockMvc RED before validator and converter GREEN.
- After this file: binding, normalization and invalid-name behavior are frozen by the HTTP test.
#### File 11 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/goods/BrandOpenApiContractTest.java`

- Purpose: Freeze the additive OpenAPI query parameter while preserving the existing operation and response.
- Symbols: `GET /api/v1/brands` remains `listBrands`; `categoryName` is an optional string with maxLength 120.
- Repository evidence: API-001 operation contract exists.
- Dependencies and consumers: the adjacent current-layer caller and consumer in Step 2.
- Why now: follows its compile prerequisite and precedes the next consumer.
- Contract/signature changes: implement exactly the final state in the inventory.
- Input/output and state mapping: satisfy REQ-103 with the named typed fields.
- Error and edge behavior: preserve invalid/empty/dependency/no-side-effect cases in this Step.
- Standards impact: MC-TEST-001, MC-SCOPE-001 — verified by the owning Step.
- Literal rule enforcement: Rule 2, Rule 6, Rule 11 — apply the file-specific obligation from Chapter 4.8.
- Implementation pseudocode:

```java
locate the existing listBrands operation and assert its route and response schemas are unchanged
assert parameters still include name, letter, seqMin, seqMax, pageNo and pageSize
assert categoryName is an optional string parameter with the declared max length
```

- Verification contribution: generated OpenAPI document.
- After this file: optional categoryName schema assertion and unchanged PageResultRecord.
#### File 12 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/application/goods/BrandManageImplTest.java`

- Purpose: Prove Application Manage carries categoryName into the same Domain query used for list and count.
- Symbols: Add a focused listBrands test with ArgumentCaptors for `BrandDomainService#listPage` and `countPage`.
- Repository evidence: Existing test verifies records and page metadata but does not inspect the Domain query fields.
- Dependencies and consumers: Uses the existing Mockito Domain Service and result converter; no Spring context or database.
- Why now: The new BrandPageQuery and BrandDomainQuery fields compile, while the current Manage implementation omits categoryName.
- Contract/signature changes: Test only; no production behavior changes in this file.
- Input/output and state mapping: `BrandPageQuery.categoryName = "phones"` → both captured BrandDomainQuery objects carry `"phones"`.
- Error and edge behavior: Existing pagination, seqMin/seqMax and total calculations remain unchanged.
- Standards impact: MC-VALID-001, MC-CONVERT-001, MC-TEST-001 — asserts the typed App→Domain handoff without reflection or a Map carrier.
- Literal rule enforcement: Rule 1 — retain semantic Query types; Rule 2 — preserve the validated handoff; Rule 3 — no manual cross-layer field mapping is added to a Converter.
- Implementation pseudocode:


```java
build a BrandPageQuery with categoryName "phones", existing page and seq bounds
invoke BrandManageImpl.listBrands with list/count Domain Service mocks and capture both queries
assert listPage and countPage receive the same categoryName and retain existing page/filter values
```

- Verification contribution: RED before File 18 and GREEN after Manage propagation.
- After this file: the Application→Domain categoryName contract is observable.
#### File 13 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/goods/repo/BrandCategoryQueryRepositoryTest.java`

- Purpose: Prove category filtering against an isolated H2 fixture before adding the SQL predicate.
- Symbols: exact-name category union, active links, same tenant, active brands, ID deduplication, seq/id tie order, name/seq composition, absent-category compatibility and filtered count.
- Repository evidence: no category relation repository test.
- Dependencies and consumers: existing BrandRepository plus H2 tables created by this focused test; no PostgreSQL or fixture-wide DDL change.
- Why now: the new Domain query field compiles, while the current repository ignores it and should return extra rows as RED evidence.
- Contract/signature changes: test only; create/drop isolated brand/category/category_brand fixture tables inside the H2 test context.
- Input/output and state mapping: query exact categoryName "phones" for tenant 41; same-name categories union and map to distinct brands.
- Error and edge behavior: exclude other tenants, deleted categories, deleted links and deleted brands; preserve name/sequence/page filters; page total counts only filtered distinct rows; no match is empty/zero; absent category returns the prior tenant-scoped active collection.
- Standards impact: MC-TEST-001, MC-SCOPE-001 — verified by the owning Step.
- Literal rule enforcement: Rule 2, Rule 6, Rule 11 — apply the file-specific obligation from Chapter 4.8.
- Implementation pseudocode:

```java
create isolated H2 brand/category/category_brand tables and insert duplicate same-name category links
query BrandRepository.list/count with the current tenant and categoryName "phones"
assert distinct active IDs including the id tie-breaker, combined name/seq filters and filtered total
assert no-match returns empty/zero and null categoryName retains existing active collection behavior
```

- Verification contribution: JUnit/H2 RED before Repository/DAO/XML filtering; GREEN afterward.
- After this file: H2 same-name category union/dedupe/tenant/soft-delete cases.
#### File 14 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/goods/repo/BrandQuerySqlShapeTest.java`

- Purpose: Preserve existing filter/order checks and freeze the bounded category EXISTS query.
- Symbols: `listBrandPage`/`countBrandPage` bind categoryName, require active category/link rows and equal tenant IDs.
- Repository evidence: current list/count SQL assertions.
- Dependencies and consumers: the adjacent current-layer caller and consumer in Step 2.
- Why now: follows its compile prerequisite and precedes the next consumer.
- Contract/signature changes: implement exactly the final state in the inventory.
- Input/output and state mapping: satisfy REQ-103 with the named typed fields.
- Error and edge behavior: preserve invalid/empty/dependency/no-side-effect cases in this Step.
- Standards impact: MC-TEST-001, MC-SCOPE-001 — verified by the owning Step.
- Literal rule enforcement: Rule 2, Rule 6, Rule 11 — apply the file-specific obligation from Chapter 4.8.
- Implementation pseudocode:

```java
extract listBrandPage and countBrandPage XML bodies
assert both have the same bound categoryName EXISTS, active predicates and tenant equality
assert the outer query keeps existing predicates/order/page and avoids category joins that duplicate rows
```

- Verification contribution: JUnit XML-shape.
- After this file: exact category subquery, bind parameter, active links, same-tenant equality.
#### File 15 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/validation/ValidCategoryNameValidator.java`

- Purpose: Implement the shared optional/required name rule while keeping the repository's common Validator base contract.
- Symbols: `BaseValidator#getValidationUtils`, `ConstraintValidator#initialize`, and `isValid`.
- Repository evidence: `ArchetypeContractConvergenceTest` requires `*Validator.java` types to extend `BaseValidator` and expose `getValidationUtils`; RED tests from Files 8–10 prove the missing predicate.
- Dependencies and consumers: API-001 and API-007 query fields.
- Why now: File 8 has shown the stub accepts invalid optional/required values.
- Contract/signature changes: replace unconditional true with required/null, trimmed blank and trimmed length checks.
- Input/output and state mapping: no mutation; converter performs normalization separately.
- Error and edge behavior: null passes only when optional; blank fails; trimmed length 1–120 passes; >120 fails.
- Standards impact: MC-ARCH-001, MC-VALID-001, MC-UTIL-001 — one shared constraint; existing ValidationUtils contract and JDK String only.
- Literal rule enforcement: Rule 2 — native/custom Jakarta validation; Rule 5 — no extra string utility; Rule 11 — preserve the repository's Light validator contract.
- Implementation pseudocode:


```java
extend BaseValidator and keep getValidationUtils() on the common ValidationUtils facade
if value == null: return !required
String candidate = value.trim()
return !candidate.isBlank() && candidate.length() <= 120; leave value unchanged and preserve interior whitespace
```

- Verification contribution: CategoryNameValidatorTest turns GREEN.
- After this file: common constraint is ready for both endpoints.
#### File 16 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/BrandDomainService.java`

- Purpose: keep method signatures otherwise.
- Symbols: ConvertGroup to GoodsCatalogQueryValidationGroup on cascaded query.
- Repository evidence: listPage/countPage use BrandDomainQuery.
- Dependencies and consumers: the adjacent current-layer caller and consumer in Step 2.
- Why now: follows its compile prerequisite and precedes the next consumer.
- Contract/signature changes: implement exactly the final state in the inventory.
- Domain boundary note: do not restore Spring `@Validated` on this interface; keep its Jakarta constraints, while the Infrastructure implementation remains the validation proxy boundary.
- Input/output and state mapping: satisfy REQ-103 with the named typed fields.
- Error and edge behavior: preserve invalid/empty/dependency/no-side-effect cases in this Step.
- Standards impact: MC-NAME-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001 — verified by the owning Step.
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 11 — apply the file-specific obligation from Chapter 4.8.
- Implementation pseudocode:

```java
add @ConvertGroup(from = Default.class, to = GoodsCatalogQueryValidationGroup.class)
to the existing @Valid BrandDomainQuery parameters on listPage and countPage
preserve Jakarta-only Domain dependencies and existing method signatures
```

- Verification contribution: group test.
- After this file: ConvertGroup to GoodsCatalogQueryValidationGroup on cascaded query.
#### File 17 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/goods/pojo/convertor/BrandAdapterConverter.java`

- Purpose: Map the optional Adapter query into the Application Query.
- Symbols: BrandAdapterConverter#toPageQuery; trimCategoryName.
- Repository evidence: current mapper already converts request fields and PageQuery with named methods.
- Dependencies and consumers: BrandController then BrandManage.
- Why now: tests now assert exact value flow.
- Contract/signature changes: add explicit categoryName mapping only.
- Input/output and state mapping: null→null; otherwise raw outer trim; preserve internal characters.
- Error and edge behavior: validation occurs before conversion; no conversion to “missing” for non-null valid data.
- Standards impact: MC-CONVERT-001, MC-UTIL-001 — MapStructPlus BaseForwardConverter and JDK String only.
- Literal rule enforcement: Rule 2 — normalize only after validation; Rule 3 — all cross-layer mapping stays MapStructPlus/BaseForwardConverter.
- Implementation pseudocode:


```java
@Mapping(target = "categoryName", source = "request.categoryName", qualifiedByName = "trimCategoryName")
BrandPageQuery toPageQuery(BrandListQuery request, PageQuery page)
trimCategoryName(value) returns null when absent, otherwise value.trim() and preserves interior whitespace
```

- Verification contribution: MVC captor observes normalized categoryName.
- After this file: Adapter→Application propagation is GREEN.
#### File 18 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/BrandManageImpl.java`

- Purpose: Carry categoryName into the existing Domain query for both list and count.
- Symbols: listBrands(BrandPageQuery query, Integer seqMin, Integer seqMax).
- Repository evidence: current implementation constructs BrandDomainQuery with name, letter and page.
- Dependencies and consumers: BrandDomainService#listPage and #countPage.
- Why now: adapter query is already mapped.
- Contract/signature changes: set categoryName on the same domainQuery used for rows and total.
- Input/output and state mapping: BrandPageQuery.categoryName→BrandDomainQuery.categoryName.
- Error and edge behavior: no extra GET, DAO call or separate count-only predicate.
- Standards impact: MC-VALID-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001 — keep current named service, qualifiers and safe logging.
- Literal rule enforcement: Rule 2 — preserve typed validated query; Rule 3 — no ad-hoc map; Rule 4 — preserve bean/log pattern.
- Implementation pseudocode:


```java
domainQuery = build(name, letter, categoryName, page)
list = domainService.listPage(domainQuery, seqMin, seqMax)
total = domainService.countPage(domainQuery, seqMin, seqMax)
use the same query instance for both calls and preserve page/total metadata
```

- Verification contribution: same normalized category value reaches list and count.
- After this file: App→Domain propagation is GREEN.
#### File 19 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/dao/BrandDAO.java`

- Purpose: Add a named optional query parameter to both existing page/count Mapper methods.
- Symbols: `listBrandPage`/`countBrandPage` add `@Param("categoryName") String categoryName`.
- Repository evidence: existing DAO defines named custom Mapper methods; XML uses bound filters and stable order.
- Dependencies and consumers: BrandRepository and Step 1 catalog tables.
- Why now: all callers now pass categoryName.
- Contract/signature changes: add the same bound String parameter to both methods; no write contract.
- Input/output and state mapping: validated exact category string is passed from Repository to a named SQL bind.
- Error and edge behavior: null leaves the predicate inactive; SQL implementation follows after the signatures compile.
- Standards impact: MC-VALID-001, MC-SCOPE-001, MC-TEST-001 — no business validation in DAO; bound SQL.
- Literal rule enforcement: Rule 2 — DAO only maps validated criteria; Rule 11 — retain MyBatis custom method/XML in selected Light.
- Implementation pseudocode:


```xml
listBrandPage(..., @Param("categoryName") String categoryName, ...)
countBrandPage(@Param("categoryName") String categoryName, ...)
keep parameter names stable for the matching XML; do not interpolate values into SQL
```

- Verification contribution: SQL shape, H2 duplicates/tenant/deleted cases, full existing Brand test suite.
- After this file: all API-001 category behavior is GREEN.
#### File 20 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/repo/BrandRepository.java`

- Purpose: Bind the Domain query field to the existing DAO list and count operations.
- Symbols: selectBrandPage/countBrandPage.
- Repository evidence: BrandRepository owns MyBatis calls and PageQuery offsets.
- Dependencies and consumers: BrandDomainServiceImpl and BrandDAO.
- Why now: next boundary after Domain Service.
- Contract/signature changes: ConvertGroup the validated query and pass query.categoryName to both DAO methods.
- Input/output and state mapping: validated query → named String SQL parameter.
- Error and edge behavior: group validation before DAO; no category preflight request.
- Standards impact: MC-VALID-001, MC-LOG-001, MC-BEAN-001.
- Literal rule enforcement: Rule 2 — validate reused query before mapper; Rule 4 — @Repository name/Qualifier/constructor; Rule 11 — DAO stays behind Repository.
- Implementation pseudocode:


```java
@Valid @ConvertGroup(from = Default.class, to = GoodsCatalogQueryValidationGroup.class) BrandDomainQuery query
selectBrandPage and countBrandPage pass query.categoryName to the corresponding DAO binds
preserve existing escaped name prefix, letter, seq bounds, page size and offset
```

- Verification contribution: H2 test observes filtering and matching total.
- After this file: Repository forwards categoryName; custom SQL is next.
#### File 21 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/resources/mybatis/mapper/goods/BrandDAO.xml`

- Purpose: no duplicate brands; preserve all filters/order/page.
- Symbols: add EXISTS over active same-tenant category/category_brand.
- Repository evidence: current list/count XML.
- Dependencies and consumers: the adjacent current-layer caller and consumer in Step 2.
- Why now: follows its compile prerequisite and precedes the next consumer.
- Contract/signature changes: implement exactly the final state in the inventory.
- Input/output and state mapping: satisfy REQ-103 with the named typed fields.
- Error and edge behavior: preserve invalid/empty/dependency/no-side-effect cases in this Step.
- Standards impact: MC-VALID-001, MC-SCOPE-001, MC-TEST-001 — verified by the owning Step.
- Literal rule enforcement: Rule 2, Rule 11 — apply the file-specific obligation from Chapter 4.8.
- Implementation pseudocode:

```xml
<if test="categoryName != null and categoryName != ''">
  AND EXISTS (SELECT 1 FROM category c JOIN category_brand cb ON cb.category_id=c.id AND cb.tenant_id=c.tenant_id
    WHERE c.name=#{categoryName} AND cb.brand_id=brand.id AND c.tenant_id=brand.tenant_id
      AND c.deleted_at IS NULL AND cb.deleted_at IS NULL)
</if>
copy the same EXISTS block into countBrandPage; preserve all current bound filters, order, limit and offset
```

- Verification contribution: SQL shape/H2.
- After this file: add EXISTS over active same-tenant category/category_brand.

- Validation working directory: /Users/mario/SelfProject/enjoyshop/enjoyshop-service/enjoyshop-service-goods.
- Verification command: ./mvnw -B -ntp -Dtest=BrandControllerWebTest,BrandOpenApiContractTest,BrandManageImplTest,BrandCategoryQueryRepositoryTest,BrandQuerySqlShapeTest,GoodsCatalogQueryValidationTest,ValidCategoryNameValidatorTest test
- Expected result: the initial failing gates cover blank/overlong names, missing ConvertGroup, absent Adapter→Application mapping, absent App→Domain propagation, unfiltered H2 results and absent EXISTS; after GREEN, every case passes with stable pagination/count.
- Failure returns to: common ConstraintValidator for invalid input; converter/Manage mapping for captured values; DAO/XML for counts, tenant and deduplication.
- Completion criteria: API-001 remains one route and unchanged when categoryName is absent; filtered and paged results are unique and tenant scoped.
- Rollback: revert only Step 2 paths; retain Step 1 committed schema and SpecPO.
- Commit paths: enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/goods/BrandControllerWebTest.java; enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/goods/BrandOpenApiContractTest.java; enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/application/goods/BrandManageImplTest.java; enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/goods/repo/BrandCategoryQueryRepositoryTest.java; enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/goods/repo/BrandQuerySqlShapeTest.java; enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/common/validation/ValidCategoryNameValidatorTest.java; enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/common/validation/GoodsCatalogQueryValidationTest.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/validation/GoodsCatalogQueryValidationGroup.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/validation/ValidCategoryName.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/validation/ValidCategoryNameValidator.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/goods/pojo/dto/BrandListQuery.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/goods/pojo/convertor/BrandAdapterConverter.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/BrandPageQuery.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/BrandManageImpl.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/BrandDomainQuery.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/BrandDomainService.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/repo/BrandRepository.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/dao/BrandDAO.java; enjoyshop-service/enjoyshop-service-goods/src/main/resources/mybatis/mapper/goods/BrandDAO.xml
- Commit: feat(goods): filter brands by category

### Step 3 — Add the read-only API-007 specification query

- Requirements: REQ-101, REQ-104, REQ-108
- Dependencies: Steps 1–2 committed
- Baseline state: catalog schema and custom Spec mapper exist; API-001 and reusable category validation work; no Spec API/Application/Domain chain exists.
- Observable outcome: GET /api/v1/specs/category/{categoryName} returns ResultRecord<List<SpecOptionVO>>, sorted by seq/id, with parsed options and [] when absent.
- End state: Spec Adapter/Application/Domain/Repository uses typed models and the custom Step 1 DAO/XML; invalid names return 400/610110 before SQL.
- Test-first gate: Required. Define `SpecListQuery`, `SpecResult`, and the `SpecManage` interface first so the HTTP test can compile. Then run `./mvnw -B -ntp -Dtest=SpecControllerWebTest,SpecOpenApiContractTest test` before creating `SpecController`; the request must return 404 and OpenAPI must omit the path. Step 1 already owns the custom SpecDAO/XML query, so SQL-shape/H2 checks are regression tests, not RED tests for absent SQL. Add remaining typed contracts before group, mapping, and repository tests.
- Approved execution correction (2026-10-04): order compile-safe query/result/Manage contracts first, then HTTP/OpenAPI RED tests, then Step 1 SQL regression checks, remaining typed contracts/converters, Domain/Repository implementation, the Light architecture inventory check, validation/H2 tests, and finally Controller wiring. The API test uses the established full-context MockMvc pattern with a mocked `SpecManage`, so it compiles and observes 404 before the Controller type exists. Full verification revealed that adding SpecRepository and SpecDomainServiceImpl also requires updating their inventory counts in LightPersistenceArchitectureTest; no route, response, or data scope changes.
- Approved execution correction (2026-10-02): account for Step 1's completed SpecDAO/XML behavior, run API/OAS RED tests before Controller wiring, and use compile-safe type contracts before group/mapping tests; no API route or behavior scope changes.
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-JSON-001, MC-TIME-001, MC-CONFIG-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001, MC-BLOCKER-001
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11
- Ordered files:

#### File 1 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/SpecListQuery.java`

- Purpose: group-independent single handoff.
- Symbols: mutable typed categoryName Query.
- Repository evidence: no application query.
- Dependencies and consumers: the adjacent current-layer caller and consumer in Step 3.
- Why now: follows its compile prerequisite and precedes the next consumer.
- Contract/signature changes: implement exactly the final state in the inventory.
- Input/output and state mapping: query rows map to SpecBO with ordered options arrays; the persisted option string is split once.
- Error and edge behavior: no matching data returns []; empty/null stored text maps to []; direct split preserves whitespace and internal empty tokens.
- Standards impact: MC-NAME-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001 — verified by the owning Step.
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 11 — apply the file-specific obligation from Chapter 4.8.
- Implementation pseudocode:

```java
    declare the exact typed symbol and annotations in the Spec
    validate/map approved input fields into the target contract
    preserve edge/error behavior and assert it through the owning test
```

- Verification contribution: validation/compile.
- After this file: mutable typed categoryName Query.
#### File 2 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/SpecResult.java`

- Purpose: no PO or domain leak.
- Symbols: mutable typed name/options result.
- Repository evidence: no application result.
- Dependencies and consumers: the adjacent current-layer caller and consumer in Step 3.
- Why now: follows its compile prerequisite and precedes the next consumer.
- Contract/signature changes: implement exactly the final state in the inventory.
- Input/output and state mapping: satisfy REQ-104 with the named typed fields.
- Error and edge behavior: preserve invalid/empty/dependency/no-side-effect cases in this Step.
- Standards impact: MC-NAME-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001 — verified by the owning Step.
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 11 — apply the file-specific obligation from Chapter 4.8.
- Implementation pseudocode:

```java
    declare the exact typed symbol and annotations in the Spec
    validate/map approved input fields into the target contract
    preserve edge/error behavior and assert it through the owning test
```

- Verification contribution: mapping/JSON.
- After this file: mutable typed name/options result.
#### File 3 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/SpecManage.java`

- Purpose: Publish typed read-only Application/Domain contracts and mapping interfaces.
- Symbols: SpecManage#listSpecsByCategoryName; SpecDomainService#listByCategoryName; SpecManageConverter; SpecResultConverter; SpecAdapterConverter.
- Repository evidence: BrandManage and BrandDomainService are existing interfaces; current converters are MapStructPlus Spring mappers with common-core bases.
- Dependencies and consumers: SpecController, corresponding Impl classes, typed queries/results.
- Why now: implementation can proceed without layer inversion.
- Contract/signature changes: manage receives SpecListQuery; Domain Service receives SpecDomainQuery; no events/writes.
- Input/output and state mapping: Query→Domain Query; BO→application Result→REST VO.
- Error and edge behavior: all contracts return typed List; empty never null.
- Standards impact: MC-NAME-001, MC-VALID-001, MC-CONVERT-001, MC-BEAN-001.
- Literal rule enforcement: Rule 1 — Query/Result/service symbols; Rule 2 — @Valid/@ConvertGroup; Rule 3 — BaseForwardConverter; Rule 4 — named Spring implementation injections; Rule 11 — current DDD layers.
- Implementation pseudocode:


```text
@Validated SpecManage.listSpecsByCategoryName(@Valid SpecListQuery)
@Validated SpecDomainService.listByCategoryName(@Valid + ConvertGroup SpecDomainQuery)
MapStructPlus converters extend BaseForwardConverter with exact source/target generics
```

- Verification contribution: compile prerequisites for test implementations.
- After this file: interfaces and converter signatures compile; no behavior is wired.

- Validation working directory: /Users/mario/SelfProject/enjoyshop/enjoyshop-service/enjoyshop-service-goods.
- Verification command: ./mvnw -B -ntp -Dtest=SpecControllerWebTest,SpecOpenApiContractTest,SpecAdapterConverterTest,SpecManageImplTest,SpecQueryRepositoryTest,SpecQuerySqlShapeTest,SpecCatalogQueryValidationTest,LightPersistenceArchitectureTest test
- Expected result: API RED before route wiring; GREEN after Step 3 with stable JSON schema, validated groups, ordered distinct rows, 10 Repositories and seven DomainServiceImpl classes in the Light architecture inventory.
- Failure returns to: Step 1 custom mapper/XML for SQL errors; converter/type definitions for MapStruct compile; Controller/OpenAPI for contract mismatch.
- Completion criteria: API-007 query path, validation, tenant/active joins, options projection, empty response and error status match Spec.
- Rollback: revert Step 3 created/modified paths only; preserve prior schema/brand commits.
- Commit paths: enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/goods/SpecControllerWebTest.java; enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/goods/SpecOpenApiContractTest.java; enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/goods/pojo/convertor/SpecAdapterConverterTest.java; enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/common/validation/SpecCatalogQueryValidationTest.java; enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/application/goods/SpecManageImplTest.java; enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/goods/repo/SpecQueryRepositoryTest.java; enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/goods/repo/SpecQuerySqlShapeTest.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/goods/pojo/dto/SpecCategoryQueryDTO.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/goods/SpecController.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/goods/pojo/vo/SpecOptionVO.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/goods/pojo/convertor/SpecAdapterConverter.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/SpecListQuery.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/SpecResult.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/SpecManage.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/SpecManageImpl.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/converter/SpecManageConverter.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/converter/SpecResultConverter.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/SpecBO.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/SpecDomainQuery.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/SpecDomainService.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/repo/SpecRepository.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/service/impl/SpecPersistenceConverter.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/service/impl/SpecDomainServiceImpl.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/enums/GoodsErrorStatus.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/handler/BaseExceptionHandler.java; enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/handler/BaseExceptionHandlerTest.java; enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/architecture/LightPersistenceArchitectureTest.java
- Commit: feat(goods): add category specification query

#### File 4 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/goods/SpecControllerWebTest.java`

- Purpose: Define API-007 route, validation and response before Controller wiring.
- Symbols: populated, empty, invalid-name tests.
- Repository evidence: `BrandOpenApiContractTest` and `NativeHttpCompatibilityTest` use the established full-context `@SpringBootTest` + `@AutoConfigureMockMvc` pattern.
- Dependencies and consumers: path literal, the preceding `SpecManage` interface mocked as a Spring bean, and the common envelope.
- Why now: public behavior must produce a genuine 404 RED while `SpecController` is not yet present; after wiring, the mock supplies the response without database access.
- Contract/signature changes: no production signature in test.
- Input/output and state mapping: encoded category path→ordered options array or [].
- Error and edge behavior: invalid category 400/610110; reads retain prior public policy.
- Standards impact: MC-VALID-001, MC-JSON-001, MC-TEST-001 — positive/negative MVC and Jackson assertions.
- Literal rule enforcement: Rule 2 — validation contract; Rule 6 — exact response JSON; Rule 11 — Adapter route.
- Implementation pseudocode:


```java
GET /api/v1/specs/category/phones
assert 200, ResultRecord.data[0].name and options are JSON array
GET with invalid categoryName; assert 400 and CATALOG_REQUEST_INVALID
```

- Verification contribution: run the focused test before Controller creation; route requests must return 404. After wiring, populated/empty/invalid cases use mocked `SpecManage` and prove the exact envelope without database access.
- After this file: route/envelope expectation is fixed.
#### File 5 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/goods/SpecOpenApiContractTest.java`

- Purpose: code-first generated OAS only.
- Symbols: API-007 route, operationId, path parameter, options array and errors.
- Repository evidence: `BrandOpenApiContractTest` reads `/v3/api-docs` from the full application context; API-007 is absent before Controller wiring.
- Dependencies and consumers: the adjacent current-layer caller and consumer in Step 3.
- Why now: follows its compile prerequisite and precedes the next consumer.
- Contract/signature changes: implement exactly the final state in the inventory.
- Input/output and state mapping: satisfy REQ-104 with the named typed fields.
- Error and edge behavior: preserve invalid/empty/dependency/no-side-effect cases in this Step.
- Standards impact: MC-TEST-001, MC-SCOPE-001 — verified by the owning Step.
- Literal rule enforcement: Rule 2, Rule 6, Rule 11 — apply the file-specific obligation from Chapter 4.8.
- Implementation pseudocode:

```java
    arrange exact Spec fixtures
    invoke the named public method, mapper or route
    assert result/error and required side-effect/no-call behavior
```

- Verification contribution: springdoc MockMvc.
- After this file: API-007 route, operationId, path parameter, options array and errors.
#### File 6 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/goods/repo/SpecQuerySqlShapeTest.java`

- Purpose: Define query SQL and data semantics before read-chain implementation.
- Symbols: exact category bind; active tenant joins; duplicate same-name category/template; stable seq/id.
- Repository evidence: BrandQuerySqlShapeTest uses static mapper checks; goods repository tests use H2.
- Dependencies and consumers: Step 1 SpecDAO/XML and H2 fixture.
- Why now: data behavior must fail before Service/Repository mapping is added.
- Contract/signature changes: test only.
- Input/output and state mapping: same-name categories→distinct SpecPO list; deleted/other tenant decoys omitted.
- Error and edge behavior: no matching category/template/spec returns empty list; options order preserved.
- Standards impact: MC-VALID-001, MC-TEST-001 — H2 only, no PostgreSQL assumption.
- Literal rule enforcement: Rule 2 — invalid query avoids DAO; Rule 11 — test current MP-SDJ/MyBatis path.
- Implementation pseudocode:


```java
assert XML binds categoryName, joins active category/template/spec,
   proves same tenant and DISTINCT ORDER BY seq,id
seed overlapping category/template/spec rows and assert each spec id once
```

- Verification contribution: inspect the Step 1 custom XML binding/join/order contract and exercise its H2 behavior as a regression gate; these assertions are expected to pass at the Step 3 baseline.
- After this file: mapper semantics are executable before the business read chain.
#### File 7 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/goods/pojo/dto/SpecCategoryQueryDTO.java`

- Purpose: Declare distinct Adapter, Application and Domain query contracts.
- Symbols: categoryName and required validation on each layer’s own type.
- Repository evidence: BrandListQuery→BrandPageQuery→BrandDomainQuery is current split-layer convention.
- Dependencies and consumers: SpecAdapterConverter, SpecManage, SpecManageConverter, SpecDomainService and SpecRepository.
- Why now: typed contract prerequisites allow tests/converters to compile without sharing a layer type.
- Contract/signature changes: SpecCategoryQueryDTO has required custom constraint; SpecListQuery is Application Query; SpecDomainQuery uses the catalog validation group.
- Input/output and state mapping: path→DTO→App Query→Domain Query; normalize outer whitespace only.
- Error and edge behavior: no tenant or actor field is accepted from URL.
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001 — DTO/Query are mutable Lombok classes with exact semantic suffixes.
- Literal rule enforcement: Rule 1 — DTO/Query suffixes; Rule 2 — distinct validation boundaries; Rule 3 — class/Lombok baseline; Rule 11 — layer ownership.
- Implementation pseudocode:


```java
SpecCategoryQueryDTO.categoryName: @ValidCategoryName(required=true)
SpecListQuery.categoryName: @NotBlank @Size(max=120)
SpecDomainQuery.categoryName: @ValidCategoryName(required=true, groups=GoodsCatalogQueryValidationGroup)
```

- Verification contribution: controller/group tests compile against exact types.
- After this file: only model/converter/Service implementation remains absent.
#### File 8 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/SpecDomainQuery.java`

- Purpose: used for one tenant-scoped read.
- Symbols: categoryName with required constraint group.
- Repository evidence: no domain query.
- Dependencies and consumers: the adjacent current-layer caller and consumer in Step 3.
- Why now: follows its compile prerequisite and precedes the next consumer.
- Contract/signature changes: implement exactly the final state in the inventory.
- Input/output and state mapping: satisfy REQ-104 with the named typed fields.
- Error and edge behavior: preserve invalid/empty/dependency/no-side-effect cases in this Step.
- Standards impact: MC-NAME-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001 — verified by the owning Step.
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 11 — apply the file-specific obligation from Chapter 4.8.
- Implementation pseudocode:

```java
    declare the exact typed symbol and annotations in the Spec
    validate/map approved input fields into the target contract
    preserve edge/error behavior and assert it through the owning test

    categoryName is mandatory after path normalization
    apply GoodsCatalogQueryValidationGroup at both reused boundaries
    no tenant or actor field is added to the query
```

- Verification contribution: validation group test.
- After this file: categoryName with required constraint group.
#### File 9 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/SpecBO.java`

- Purpose: Declare the internal domain result and immutable HTTP item.
- Symbols: SpecBO(id,name,options,seq); SpecOptionVO(name,List<String> options).
- Repository evidence: current BrandBO is mutable class; public BrandVO is an immutable value record.
- Dependencies and consumers: SpecPersistenceConverter/SpecResultConverter/SpecAdapterConverter.
- Why now: exact typed result shapes prevent PO and tenant/audit metadata leaking.
- Contract/signature changes: no persistence or public fields beyond Spec §9.2.2.
- Input/output and state mapping: parsed options are ordered and non-null; VO copies its list immutably.
- Error and edge behavior: empty stored options become []; unknown columns fail MapStruct compile.
- Standards impact: MC-NAME-001, MC-MODEL-001, MC-CONVERT-001, MC-JSON-001.
- Literal rule enforcement: Rule 1 — BO/VO suffix; Rule 3 — mutable BO class, immutable VO record; Rule 6 — exact Jackson schema.
- Implementation pseudocode:


```java
SpecBO uses class/Lombok fields id,name,List<String> options,seq
SpecOptionVO record constructor applies List.copyOf(options)
SpecPersistenceConverter drops tenant/audit/version fields and never returns SpecPO
SpecResultConverter maps only name/options and keeps an empty option list non-null
```

- Verification contribution: converter, JSON and OpenAPI tests.
- After this file: target model contracts compile.
#### File 10 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/goods/pojo/vo/SpecOptionVO.java`

- Purpose: Jackson schema is stable.
- Symbols: immutable record with name and copied options list.
- Repository evidence: no Spec view type.
- Dependencies and consumers: the adjacent current-layer caller and consumer in Step 3.
- Why now: follows its compile prerequisite and precedes the next consumer.
- Contract/signature changes: implement exactly the final state in the inventory.
- Input/output and state mapping: satisfy REQ-104 with the named typed fields.
- Error and edge behavior: preserve invalid/empty/dependency/no-side-effect cases in this Step.
- Standards impact: MC-NAME-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001 — verified by the owning Step.
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 11 — apply the file-specific obligation from Chapter 4.8.
- Implementation pseudocode:

```java
    declare the exact typed symbol and annotations in the Spec
    validate/map approved input fields into the target contract
    preserve edge/error behavior and assert it through the owning test
```

- Verification contribution: serialization/OAS.
- After this file: immutable record with name and copied options list.
#### File 11 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/SpecDomainService.java`

- Purpose: named Domain Service query, no persistence type.
- Symbols: add listByCategoryName with ConvertGroup boundary.
- Repository evidence: no Spec query contract.
- Dependencies and consumers: the adjacent current-layer caller and consumer in Step 3.
- Why now: follows its compile prerequisite and precedes the next consumer.
- Contract/signature changes: implement exactly the final state in the inventory.
- Input/output and state mapping: satisfy REQ-104 with the named typed fields.
- Error and edge behavior: preserve invalid/empty/dependency/no-side-effect cases in this Step.
- Standards impact: MC-NAME-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001 — verified by the owning Step.
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 11 — apply the file-specific obligation from Chapter 4.8.
- Implementation pseudocode:

```java
    declare the exact typed symbol and annotations in the Spec
    validate/map approved input fields into the target contract
    preserve edge/error behavior and assert it through the owning test
```

- Verification contribution: compile/group test.
- After this file: add listByCategoryName with ConvertGroup boundary.
#### File 12 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/converter/SpecManageConverter.java`

- Purpose: normalize only outer whitespace.
- Symbols: BaseForwardConverter<SpecListQuery,SpecDomainQuery>.
- Repository evidence: no Manage/domain conversion.
- Dependencies and consumers: the adjacent current-layer caller and consumer in Step 3.
- Why now: follows its compile prerequisite and precedes the next consumer.
- Contract/signature changes: implement exactly the final state in the inventory.
- Input/output and state mapping: satisfy REQ-104 with the named typed fields.
- Error and edge behavior: preserve invalid/empty/dependency/no-side-effect cases in this Step.
- Standards impact: MC-NAME-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001, MC-CONVERT-001 — verified by the owning Step.
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 11 — apply the file-specific obligation from Chapter 4.8.
- Implementation pseudocode:

```java
    declare the exact typed symbol and annotations in the Spec
    validate/map approved input fields into the target contract
    preserve edge/error behavior and assert it through the owning test
```

- Verification contribution: MapStruct compile.
- After this file: BaseForwardConverter<SpecListQuery,SpecDomainQuery>.
#### File 13 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/converter/SpecResultConverter.java`

- Purpose: map exact result fields and options.
- Symbols: BaseForwardConverter<SpecBO,SpecResult>.
- Repository evidence: no Domain/app conversion.
- Dependencies and consumers: the adjacent current-layer caller and consumer in Step 3.
- Why now: follows its compile prerequisite and precedes the next consumer.
- Contract/signature changes: implement exactly the final state in the inventory.
- Input/output and state mapping: satisfy REQ-104 with the named typed fields.
- Error and edge behavior: preserve invalid/empty/dependency/no-side-effect cases in this Step.
- Standards impact: MC-NAME-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001, MC-CONVERT-001 — verified by the owning Step.
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 11 — apply the file-specific obligation from Chapter 4.8.
- Implementation pseudocode:

```java
    declare the exact typed symbol and annotations in the Spec
    validate/map approved input fields into the target contract
    preserve edge/error behavior and assert it through the owning test
```

- Verification contribution: MapStruct test.
- After this file: BaseForwardConverter<SpecBO,SpecResult>.
#### File 14 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/goods/pojo/convertor/SpecAdapterConverter.java`

- Purpose: explicit fields, no unmapped target.
- Symbols: BaseForwardConverter<SpecResult,SpecOptionVO> plus DTO→Query map.
- Repository evidence: no Spec adapter mapping.
- Dependencies and consumers: the adjacent current-layer caller and consumer in Step 3.
- Why now: follows its compile prerequisite and precedes the next consumer.
- Contract/signature changes: implement exactly the final state in the inventory.
- Input/output and state mapping: satisfy REQ-104 with the named typed fields.
- Error and edge behavior: preserve invalid/empty/dependency/no-side-effect cases in this Step.
- Standards impact: MC-NAME-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001, MC-CONVERT-001 — verified by the owning Step.
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 11 — apply the file-specific obligation from Chapter 4.8.
- Implementation pseudocode:

```java
    declare the exact typed symbol and annotations in the Spec
    validate/map approved input fields into the target contract
    preserve edge/error behavior and assert it through the owning test
```

- Verification contribution: MapStruct compile.
- After this file: BaseForwardConverter<SpecResult,SpecOptionVO> plus DTO→Query map.
#### File 15 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/service/impl/SpecPersistenceConverter.java`

- Purpose: Implement only approved cross-layer projections.
- Symbols: SpecPO→SpecBO; SpecListQuery→SpecDomainQuery; SpecBO→SpecResult; SpecResult→SpecOptionVO and DTO→SpecListQuery.
- Repository evidence: BrandPersistenceConverter, BrandResultConverter and BrandAdapterConverter define current MapStruct style.
- Dependencies and consumers: SpecRepository/Domain Service/Application Manage/Controller.
- Why now: typed query and result contracts, Domain Service contract, and converters are defined before repository wiring.
- Contract/signature changes: one-way projections; Spec persistence converter has no reverse write operation.
- Input/output and state mapping: split options once in PO→BO, copy immutable list at VO boundary; null list→[].
- Error and edge behavior: MapStruct unmappedTargetPolicy=ERROR; category converters trim outer whitespace only.
- Standards impact: MC-CONVERT-001, MC-UTIL-001, MC-BEAN-001.
- Literal rule enforcement: Rule 3 — all conversions use Egon BaseConverter/BaseForwardConverter and MapStructPlus; Rule 5 — JDK string split only; Rule 4 — named mapper Beans.
- Implementation pseudocode:


```java
@Mapper(componentModel="spring", unmappedTargetPolicy=ERROR)
SpecPersistenceConverter extends BaseForwardConverter<SpecPO,SpecBO>
SpecManageConverter extends BaseForwardConverter<SpecListQuery,SpecDomainQuery>
SpecResultConverter extends BaseForwardConverter<SpecBO,SpecResult>
SpecAdapterConverter extends BaseForwardConverter<SpecResult,SpecOptionVO>
```

- Verification contribution: converter tests expose exact field mappings and option order.
- After this file: every interlayer projection compiles; SQL path is still not invoked.
#### File 16 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/repo/SpecRepository.java`

- Purpose: Wire Application→Domain→Repository→custom DAO and return typed results.
- Symbols: listSpecsByCategoryName and listByCategoryName methods.
- Repository evidence: BrandRepository/BrandDomainServiceImpl/BrandManageImpl provide named DI and read-only transaction convention.
- Dependencies and consumers: custom SpecDAO/XML/PO from Step 1 plus files 4–7.
- Why now: all lower compile dependencies now exist; the RED repository/MVC tests drive the minimal behavior.
- Contract/signature changes: new read-only chain only.
- Input/output and state mapping: Query converters, repository returns BO rows, Manage returns Result rows; preserve seq/id ordering.
- Error and edge behavior: grouped category validation before DAO; empty list preserved; no write/event/transaction spanning object storage.
- Standards impact: MC-VALID-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-PATTERN-001.
- Literal rule enforcement: Rule 2 — service/repository validation; Rule 3 — typed mapped objects; Rule 4 — @Slf4j, Bean names, @RequiredArgsConstructor and @Qualifier on each dependency; Rule 9 — simple Query stays direct; Rule 11 — exact Light layer placement.
- Implementation pseudocode:


```java
SpecRepository.listByCategoryName(query):
    validate with Catalog group
    rows = specDAO.listByCategoryName(query.getCategoryName())
    return persistenceConverter.toTargetList(rows)
SpecDomainServiceImpl.listByCategoryName(query):
    return repository.listByCategoryName(query)
SpecManageImpl.listSpecsByCategoryName(query):
    domain = manageConverter.toTarget(query)
    return resultConverter.toTargetList(domainService.listByCategoryName(domain))
```

- Verification contribution: repository/domain/manage unit tests reach GREEN.
- After this file: Application Query is callable in-process; adapter route remains RED.
#### File 17 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/service/impl/SpecDomainServiceImpl.java`

- Purpose: no DAO use; read-only transaction.
- Symbols: @Service named query; repository→BO converter.
- Repository evidence: no Spec domain implementation.
- Dependencies and consumers: the adjacent current-layer caller and consumer in Step 3.
- Why now: follows its compile prerequisite and precedes the next consumer.
- Contract/signature changes: implement exactly the final state in the inventory.
- Input/output and state mapping: satisfy REQ-104 with the named typed fields.
- Error and edge behavior: preserve invalid/empty/dependency/no-side-effect cases in this Step.
- Standards impact: MC-NAME-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001, MC-LOG-001, MC-BEAN-001 — verified by the owning Step.
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 11, Rule 4 — apply the file-specific obligation from Chapter 4.8.
- Implementation pseudocode:

```java
    declare the exact typed symbol and annotations in the Spec
    validate/map approved input fields into the target contract
    preserve edge/error behavior and assert it through the owning test
```

- Verification contribution: JUnit/wiring.
- After this file: @Service named query; repository→BO converter.
#### File 18 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/architecture/LightPersistenceArchitectureTest.java`

- Purpose: Keep the selected Light persistence architecture inventory aligned with the new Spec Repository.
- Symbols: `assertEquals(10, count(javaFiles, "Repository.java"))`; `assertEquals(7, count(javaFiles, "DomainServiceImpl.java"))`.
- Repository evidence: Step 1 added SpecPO/SpecDAO and updated PO/DAO counts; this Step adds the tenth Repository and seventh DomainServiceImpl.
- Dependencies and consumers: `SpecRepository` from File 16, `SpecDomainServiceImpl` from File 17, and the existing Light repositories.
- Why now: the architecture contract must track both Step 3 implementation counts before validation gates.
- Contract/signature changes: test expectation only; no production contract changes.
- Input/output and state mapping: source inventory recognizes ten Repository and seven DomainServiceImpl classes while retaining the established Light structure.
- Error and edge behavior: no production behavior; an unexpected repository count fails the architecture gate.
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001.
- Literal rule enforcement: Rule 11 — preserve the existing Light profile and Egon-COLA persistence ownership.
- Implementation pseudocode:

```java
var javaFiles = walk(Path.of("src/main/java")).filter(nameEndsWithJava).toList()
assertEquals(10, count(javaFiles, "Repository.java"))
assertEquals(7, count(javaFiles, "DomainServiceImpl.java"))
assertTrue(source.contains("extends EgonColaRepository<"))
assertFalse(source.contains("jakarta.persistence"))
```

- Verification contribution: focused architecture test and full module verify.
- After this file: the Light architecture inventory accounts for SpecRepository and SpecDomainServiceImpl.
#### File 19 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/common/validation/SpecCatalogQueryValidationTest.java`

- Purpose: Verify required names at the Application Manage boundary and both reused DomainQuery group boundaries.
- Symbols: SpecListQuery null/blank/overlong invalid; SpecDomainQuery fails under the converted group at Domain Service and Repository; valid name passes.
- Repository evidence: GoodsCatalogQueryValidationGroup/ValidCategoryName from Step 2.
- Dependencies and consumers: SpecManage, SpecDomainQuery, Domain Service/Repository methods.
- Why now: API-007 uses a required path value that crosses all three validated service boundaries.
- Contract/signature changes: test only.
- Input/output and state mapping: required input→Manage Query→grouped Domain Query before DAO.
- Error and edge behavior: invalid input gives zero DAO interactions at every boundary.
- Standards impact: MC-VALID-001, MC-TEST-001.
- Literal rule enforcement: Rule 2 — ConvertGroup and ConstraintValidator at each boundary.
- Implementation pseudocode:


```java
validate SpecDomainQuery under GoodsCatalogQueryValidationGroup
invoke service/repository proxy with invalid value; assert violation and no mapper call

    validate SpecDomainQuery with GoodsCatalogQueryValidationGroup
    invoke the Service and Repository method-validation boundaries
    assert violation and zero DAO calls
```

- Verification contribution: Spring method-validation proxies prove Application, Domain Service, and Repository rejection; H2/mapper remains untouched on invalid inputs.
- After this file: invalid Spec queries have a fixed acceptance point at each service boundary.
#### File 20 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/goods/repo/SpecQueryRepositoryTest.java`

- Purpose: verify distinct ordered SpecPO values and the persistence-to-domain options projection.
- Symbols: duplicate categories/templates/spec rows, tenant/deleted rows, and comma-split options.
- Repository evidence: no Spec query fixture.
- Dependencies and consumers: the adjacent current-layer caller and consumer in Step 3.
- Why now: follows its compile prerequisite and precedes the next consumer.
- Contract/signature changes: implement exactly the final state in the inventory.
- Input/output and state mapping: satisfy REQ-104 with the named typed fields.
- Error and edge behavior: preserve invalid/empty/dependency/no-side-effect cases in this Step.
- Standards impact: MC-TEST-001, MC-SCOPE-001 — verified by the owning Step.
- Literal rule enforcement: Rule 2, Rule 6, Rule 11 — apply the file-specific obligation from Chapter 4.8.
- Implementation pseudocode:

```java
    arrange exact Spec fixtures
    invoke the named public method, mapper or route
    assert result/error and required side-effect/no-call behavior
```

- Verification contribution: H2 query plus MapStruct projection assertions; unit assertions cover null/empty/consecutive/trailing delimiter and whitespace behavior.
- After this file: duplicate categories/templates/spec rows are deduplicated, tenant/soft-delete decoys are excluded, and options are projected as ordered non-null arrays.
#### File 21 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/SpecManageImpl.java`

- Purpose: @Service name, @Slf4j, @RequiredArgsConstructor, Qualifier.
- Symbols: map Query→Domain Query, call service, map results.
- Repository evidence: no application service.
- Dependencies and consumers: the adjacent current-layer caller and consumer in Step 3.
- Why now: follows its compile prerequisite and precedes the next consumer.
- Contract/signature changes: implement exactly the final state in the inventory.
- Input/output and state mapping: satisfy REQ-104 with the named typed fields.
- Error and edge behavior: preserve invalid/empty/dependency/no-side-effect cases in this Step.
- Standards impact: MC-NAME-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001, MC-LOG-001, MC-BEAN-001 — verified by the owning Step.
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 11, Rule 4 — apply the file-specific obligation from Chapter 4.8.
- Implementation pseudocode:

```java
    declare the exact typed symbol and annotations in the Spec
    validate/map approved input fields into the target contract
    preserve edge/error behavior and assert it through the owning test
```

- Verification contribution: JUnit/wiring.
- After this file: map Query→Domain Query, call service, map results.
#### File 22 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/application/goods/SpecManageImplTest.java`

- Purpose: empty result becomes [].
- Symbols: application/domain converter calls and ordered result.
- Repository evidence: no SpecManage.
- Dependencies and consumers: the adjacent current-layer caller and consumer in Step 3.
- Why now: follows its compile prerequisite and precedes the next consumer.
- Contract/signature changes: implement exactly the final state in the inventory.
- Input/output and state mapping: satisfy REQ-104 with the named typed fields.
- Error and edge behavior: preserve invalid/empty/dependency/no-side-effect cases in this Step.
- Standards impact: MC-TEST-001, MC-SCOPE-001 — verified by the owning Step.
- Literal rule enforcement: Rule 2, Rule 6, Rule 11 — apply the file-specific obligation from Chapter 4.8.
- Implementation pseudocode:

```java
    arrange exact Spec fixtures
    invoke the named public method, mapper or route
    assert result/error and required side-effect/no-call behavior
```

- Verification contribution: JUnit/Mockito.
- After this file: application/domain converter calls and ordered result.
#### File 23 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/goods/pojo/convertor/SpecAdapterConverterTest.java`

- Purpose: options list copy; no Map.
- Symbols: DTO→SpecListQuery and SpecResult→immutable VO mappings.
- Repository evidence: no Spec converter.
- Dependencies and consumers: the adjacent current-layer caller and consumer in Step 3.
- Why now: follows its compile prerequisite and precedes the next consumer.
- Contract/signature changes: implement exactly the final state in the inventory.
- Input/output and state mapping: satisfy REQ-104 with the named typed fields.
- Error and edge behavior: preserve invalid/empty/dependency/no-side-effect cases in this Step.
- Standards impact: MC-TEST-001, MC-SCOPE-001 — verified by the owning Step.
- Literal rule enforcement: Rule 2, Rule 6, Rule 11 — apply the file-specific obligation from Chapter 4.8.
- Implementation pseudocode:

```java
    arrange exact Spec fixtures
    invoke the named public method, mapper or route
    assert result/error and required side-effect/no-call behavior
```

- Verification contribution: MapStruct/JSON test.
- After this file: DTO→SpecListQuery and SpecResult→immutable VO mappings.
#### File 24 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/common/enums/GoodsErrorStatus.java`

- Purpose: Add stable catalog and file/storage errors.
- Symbols: FILE_REQUEST_INVALID 610106; FILE_TOO_LARGE 610107; FILE_STORAGE_UNAVAILABLE 610108; FILE_STORAGE_OUTCOME_UNKNOWN 610109; CATALOG_REQUEST_INVALID 610110.
- Repository evidence: existing GoodsErrorStatus values 610101–610105 and 610199 are stable.
- Dependencies and consumers: BaseExceptionHandler and CommonException.
- Why now: API-007 tests require the route-specific error code.
- Contract/signature changes: append codes only; preserve current numbers/names.
- Input/output and state mapping: CommonException status becomes ResultRecord; Shoubing auth remains external.
- Error and edge behavior: storage unavailable/outcome unknown both map to 503 but retain distinct codes.
- Standards impact: MC-NAME-001, MC-JSON-001, MC-SCOPE-001.
- Literal rule enforcement: Rule 1 semantic enum names; Rule 6 no ordinal; Rule 11 existing common package.
- Implementation pseudocode:


```java
append the five new constants after existing brand codes
retain ErrorStatus#getStatus as enum name
    do not renumber, reuse, or persist enum ordinal values\n    HTTP status mapping remains in BaseExceptionHandler\n
    append stable names and codes without changing existing values
    610106 through 610110 stay one-to-one with the accepted error contract
    no ordinal or persisted enum behavior is introduced
```

- Verification contribution: compile and route-code unit assertions.
- After this file: error codes exist; handlers still return old mappings.
#### File 25 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/handler/BaseExceptionHandlerTest.java`

- Purpose: Prove new error codes/statuses without changing brand behavior.
- Symbols: 610101 brand; 610110 catalog; 610106 file; 610107 413; 610108/610109 503.
- Repository evidence: existing test asserts stable brand codes and envelope.
- Dependencies and consumers: the already declared `GoodsErrorStatus` values and existing goods advice; the following handler test verifies the mapping before handler implementation.
- Why now: handler change needs a GREEN test before API-007 wiring.
- Contract/signature changes: add tests only.
- Input/output and state mapping: request URI plus exception → HTTP status/ResultRecord code/data.
- Error and edge behavior: assert Shoubing 401 is not handled here; Resource-grant denial is an upstream authorization result; preserve brand 610101.
- Standards impact: MC-JSON-001, MC-TEST-001.
- Literal rule enforcement: Rule 6 response shape; Rule 11 existing adapter handler package.
- Implementation pseudocode:


```java
assert invalid /api/v1/brands query -> 400/610101
assert invalid /api/v1/specs query -> 400/610110
assert file overflow -> 413/610107; storage exceptions -> 503/610108 or 610109
```

- Verification contribution: focused unit/MockMvc error contract test.
- After this file: API-001/API-007 error mappings are GREEN before SpecController is added.
#### File 26 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/handler/BaseExceptionHandler.java`

- Purpose: Map invalid requests according to the owning goods route and map multipart/storage statuses.
- Symbols: bind/constraint/structural handlers; request-path selector; httpStatus table; MaxUploadSizeExceededException handler.
- Repository evidence: current advice is scoped to the goods adapter package and owns brand ResultRecord errors.
- Dependencies and consumers: API-001/007/008; Shoubing filter precedes MVC advice and keeps its own 401 body.
- Why now: the Spec query needs 610110 while existing brand requests must keep 610101.
- Contract/signature changes: accept HttpServletRequest in request-failure handlers; add 413 and 503 mappings.
- Input/output and state mapping: /brands → 610101; /specs/** → 610110; /files/** → 610106; oversize → 413/610107; storage CommonException 610108/610109 → 503.
- Error and edge behavior: no SDK detail reaches client; Shoubing response is not double wrapped; data remains null unless existing field-validation contract applies.
- Standards impact: MC-VALID-001, MC-LOG-001, MC-JSON-001, MC-SCOPE-001.
- Literal rule enforcement: Rule 2 route boundary; Rule 4 safe logs; Rule 6 Jackson envelope; Rule 11 preserve existing handler.
- Implementation pseudocode:


```java
CommonException: status = httpStatus(code); return ResultRecord.failure(exception)
request validation: select stable code from request URI, preserve FieldErrorsVO
MaxUploadSizeExceededException: FILE_TOO_LARGE / HTTP 413
storage error codes: HTTP 503
```

- Verification contribution: route-specific BaseExceptionHandlerTest cases.
- After this file: API-007 610110 contract can pass; upload error routes are ready for Step 6.
#### File 27 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/goods/SpecController.java`

- Purpose: Publish API-007 through current goods adapter.
- Symbols: GET /api/v1/specs/category/{categoryName}, operationId listSpecsByCategoryName.
- Repository evidence: BrandController uses Spring MVC, @Validated, @Qualifier and ResultRecord.
- Dependencies and consumers: SpecManage and SpecAdapterConverter; external product editor.
- Why now: domain/application behavior is green before HTTP wiring.
- Contract/signature changes: one GET read-only route, no tenant query field.
- Input/output and state mapping: required path → DTO → SpecListQuery → List<SpecOptionVO> → ResultRecord.
- Error and edge behavior: 400/610110; no data 200/[]; DB errors use current masked 500/610199; no Shoubing auth change.
- Standards impact: MC-VALID-001, MC-JSON-001, MC-LOG-001, MC-BEAN-001.
- Literal rule enforcement: Rule 1 — Controller semantic suffix; Rule 2 — validated path boundary; Rule 4 — stable Bean/Qualifier injection; Rule 6 — exact JSON envelope; Rule 11 — Adapter package.
- Implementation pseudocode:


```java
@GetMapping("/category/{categoryName}")
validate the required path String; dto = new SpecCategoryQueryDTO().setCategoryName(categoryName)
    query = converter.toListQuery(dto); rows = manage.listSpecsByCategoryName(query)
    return ResultRecord.success(converter.toVOList(rows))
query = adapterConverter.toQuery(dto)
rows = manage.listSpecsByCategoryName(query)
return ResultRecord.success(adapterConverter.toVOList(rows))
```

- Verification contribution: MVC/OpenAPI tests become GREEN.
- After this file: API-007 is complete without CRUD/frontend expansion.
### Step 4 — Configure CORS, approved OIDC/S3 dependencies, client properties and profile routing

- Requirements: REQ-101, REQ-102, REQ-105, REQ-107, REQ-108
- Dependencies: Steps 1–3 committed
- Baseline state: NativeHttpSecurityConfiguration permits every request; four profiles have Shoubing placeholders but no CORS/storage keys and do not map catalog tables.
- Observable outcome: precise CORS works; anonymous POST /api/v1/files fails closed; all profiles have parity; typed properties build one named S3Client without network access.
- End state: only the future file POST requires authenticated identity; other paths stay permitAll; table maps route all five catalog tables to master_data; Shoubing can validate when activated.
- Test-first gate: Required — preflight has no allowed headers, anonymous upload path is currently permitAll, and profile parity omits all new keys.
- Approved repository clarification (2026-10-05): Shoubing `IdpBearerAuthenticationFilter` continues when Authorization is absent; the native security chain must produce a 401 JSON response using its established `AUTHORIZATION_HEADER_INVALID` reason code. The filter continues to own malformed/invalid Bearer responses. This closes the accepted missing-Bearer contract without changing the route or authorization scope.
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-JSON-001, MC-TIME-001, MC-CONFIG-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001, MC-BLOCKER-001
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11
- Ordered files:

#### File 1 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/start/config/NativeHttpSecurityConfigurationTest.java`

- Purpose: Define CORS and upload authorization behavior before changing the chain.
- Symbols: allowed_origin_preflight, rejected_origin_has_no_acao, anonymous_upload_is_401_with_shoubing_body, existing_brand_get_remains_public, named goodsS3Client Bean exists.
- Repository evidence: current SecurityChain anyRequest().permitAll(); current MVC tests use Spring MockMvc.
- Dependencies and consumers: NativeHttpSecurityConfiguration; test-only session SecurityContext for authenticated requests.
- Why now: this is the RED proof for the new security/CORS behavior.
- Contract/signature changes: tests only; no spring-security-test dependency.
- Input/output and state mapping: Origin/method/header or auth session → Access-Control headers/security result.
- Error and edge behavior: disallowed Origin gets no ACAO; anonymous POST is denied before an absent route can return 404 with 401 and the Shoubing `AUTHORIZATION_HEADER_INVALID` JSON shape; OPTIONS preflight remains available.
- Standards impact: MC-CONFIG-001, MC-SCOPE-001, MC-TEST-001.
- Literal rule enforcement: Rule 7 — config is externalized; Rule 11 — existing servlet chain is the boundary.
- Implementation pseudocode:


```java
OPTIONS /api/v1/files with allowed Origin and requested Authorization/Content-Type
assert allow-origin/method/header and expose Location
repeat with denied Origin and assert no allow-origin
anonymous POST -> 401 with code AUTHORIZATION_HEADER_INVALID and message; authenticated session reaches absent MVC route; GET /api/v1/brands without auth -> existing public policy
```

- Verification contribution: fails before the CORS source and authenticated matcher exist.
- After this file: expected security tests compile and are RED.

#### File 2 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/start/config/ProfileKeyParityTest.java`

- Purpose: Expand strict owned-key parity to this feature.
- Symbols: four profile files; app.web.cors; app.object-storage; multipart sizes; Shoubing keys; five sharding table paths.
- Repository evidence: existing test compares narrowed governance key families and uses SnakeYAML.
- Dependencies and consumers: the four resource YAML files in Step 4.
- Why now: it proves profile changes stay synchronized rather than copying values manually.
- Contract/signature changes: include every new owned path, not whole-document key equality.
- Input/output and state mapping: four YAML maps→identical key/path set; values may differ.
- Error and edge behavior: missing or extra owned path reports the exact profile/path.
- Standards impact: MC-CONFIG-001, MC-TEST-001.
- Literal rule enforcement: Rule 7 — all environment files checked; Rule 11 — config remains in current Light.
- Implementation pseudocode:


```java
load base/dev/test/prod YAML
flatten owned prefixes
assert same CORS/S3/multipart/Shoubing and five table routing paths
assert each table routes SINGLE/master_data
```

- Verification contribution: RED while new profile paths are absent.
- After this file: profile drift is observable.

#### File 3 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/start/config/NativeRpcConfigurationTest.java`

- Purpose: Preserve existing Shoubing property binding coverage with environment-controlled fail-closed defaults.
- Symbols: IdpStarterProperties enabled/registerFilter defaults; existing service-client registration id.
- Repository evidence: current test binds the existing starter prefix and asserts disabled test defaults.
- Dependencies and consumers: YAML and Shoubing starter dependency.
- Why now: prod/dev registration placeholders change from fixed false to an environment override with false default.
- Contract/signature changes: update only assertions for the new placeholder/default; retain unrelated RPC assertions.
- Input/output and state mapping: test profile disables Shoubing; deployment enables the Starter while `register-filter` remains false so the application SecurityFilterChain installs its filter.
- Error and edge behavior: tests do not connect to Redis or Shoubing.
- Standards impact: MC-DEP-001, MC-CONFIG-001, MC-TEST-001.
- Literal rule enforcement: Rule 7 — values may differ while key paths remain identical; Rule 11 — preserve existing config tests.
- Implementation pseudocode:


```java
bind egon.cola.platform.tianquan.shoubing in test property set
assert enabled=false and registerFilter=false without environment override
retain registration-id and unrelated native property assertions
```

- Verification contribution: property binding regression test.
- After this file: Shoubing config remains test-disabled and fail-closed.

#### File 4 — `MODIFY enjoyshop-service/enjoyshop-service-goods/pom.xml`

- Purpose: Add the two exact user-approved capability dependencies.
- Symbols: AWS BOM import/version 2.55.6; software.amazon.awssdk:s3; top.egon:egon-cola-tianquan-shoubing-starter:5.4.1.
- Repository evidence: current module parent manages Spring Boot/Egon components; neither dependency is present in the goods POM.
- Dependencies and consumers: S3 configuration/adapter and Shoubing autoconfiguration.
- Why now: compile SDK/config types and activate the approved identity filter.
- Contract/signature changes: add dependency-management import and two dependencies only.
- Input/output and state mapping: BOM pins AWS SDK group; Shoubing uses exact 5.4.1.
- Error and edge behavior: do not add MinIO SDK, explicit HTTP client, spring-security-test or utility library.
- Standards impact: MC-DEP-001, MC-REUSE-001, MC-SCOPE-001.
- Literal rule enforcement: Rule 5 — SDK is used only for storage protocol; Rule 11 — same app/module.
- Implementation pseudocode:


```xml
import software.amazon.awssdk:bom:2.55.6
add software.amazon.awssdk:s3 without an independent version
add Shoubing starter 5.4.1; do not upgrade unrelated dependencies
```

- Verification contribution: dependency:tree shows only approved additions and one AWS SDK version.
- After this file: types resolve; no remote client call occurs.

#### File 5 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/start/config/ObjectStorageConfigurationProperties.java`

- Purpose: Define a typed property contract for the external storage endpoint.
- Symbols: URI endpoint/publicUrlBase; region/bucket/accessKey/secretKey; pathStyleAccess; apiCallAttemptTimeout/apiCallTimeout.
- Repository evidence: current application uses Spring environment properties; primary Spec names app.object-storage.*.
- Dependencies and consumers: ObjectStorageConfiguration and all profiles.
- Why now: config tests need the class contract before its bean constraints are added.
- Contract/signature changes: @ConfigurationProperties prefix app.object-storage; timeout fields are java.time.Duration.
- Input/output and state mapping: YAML/environment values→typed properties.
- Error and edge behavior: Lombok-generated toString must exclude accessKey and secretKey; no secret logs.
- Standards impact: MC-VALID-001, MC-MODEL-001, MC-TIME-001, MC-CONFIG-001.
- Literal rule enforcement: Rule 3 — mutable property class/Lombok; Rule 4 — config type is not a business processor; Rule 6 — not serialized to API; Rule 10 — Duration.
- Implementation pseudocode:


```text
@ConfigurationProperties("app.object-storage")
mutable Lombok class for URI/String/boolean/Duration fields
exclude credential fields from toString
```

- Verification contribution: compile prerequisite for File 6.
- After this file: properties bind by name but required-field validation is not yet enforced.

#### File 6 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/start/config/ObjectStorageConfigurationPropertiesTest.java`

- Purpose: Define required, safe S3 property binding and endpoint URL validation before adding constraints.
- Symbols: valid_config_binds; required_endpoint_credentials_public_base; endpoint_absolute_http; public_base_absolute_https; total_timeout_not_shorter_than_attempt; toString_redacts_credentials.
- Repository evidence: NativeRpcConfigurationTest uses Spring Boot Binder without starting the app.
- Dependencies and consumers: properties type from File 5.
- Why now: ensures missing fields/defaults and secret exposure are tested.
- Contract/signature changes: test only.
- Input/output and state mapping: map properties→bound object.
- Error and edge behavior: Binder tests do not connect; missing fields, relative endpoints, non-HTTPS public URLs, and attempt timeout longer than total fail validation.
- Standards impact: MC-VALID-001, MC-TIME-001, MC-CONFIG-001, MC-TEST-001.
- Literal rule enforcement: Rule 2 — property constraints; Rule 4 — credentials are not logged; Rule 10 — java.time.Duration.
- Implementation pseudocode:


```java
Binder.bind valid map and assert endpoint/region/bucket/pathStyle/timeouts
Jakarta Validator rejects missing credentials, relative endpoint, non-HTTPS public URL and total timeout < attempt timeout
assert toString omits access and secret values
```

- Verification contribution: RED while required constraints are absent.
- After this file: properties readiness contract is fixed.

#### File 7 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/start/config/ObjectStorageConfigurationProperties.java`

- Purpose: Complete validated Spring property binding.
- Symbols: @Validated; @NotNull/@NotBlank constraints; duration relation validation.
- Repository evidence: existing configuration classes use Spring binding and Jakarta validation.
- Dependencies and consumers: test File 6 and S3Client configuration.
- Why now: RED test establishes required fields and secret-safe representation.
- Contract/signature changes: validate endpoint URI, region, bucket, public HTTPS base, access/secret key, positive attempt/total timeouts.
- Input/output and state mapping: every active profile binds the same fields; values are environment-specific.
- Error and edge behavior: startup property failure is fail-closed; total timeout must be >= attempt timeout; no fallback to local object-store host.
- Standards impact: MC-VALID-001, MC-TIME-001, MC-CONFIG-001.
- Literal rule enforcement: Rule 2 — native/custom validation; Rule 4 — secrets excluded; Rule 10 — Duration comparisons.
- Implementation pseudocode:


```java
validate endpoint/publicUrlBase are absolute URIs
require nonblank bucket, region, accessKey, secretKey
require attemptTimeout > 0 and apiCallTimeout >= attemptTimeout
```

- Verification contribution: ObjectStorageConfigurationPropertiesTest becomes GREEN.
- After this file: storage properties fail at startup instead of producing a false/unstable URL.

#### File 8 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/resources/application.yml`

- Purpose: Declare identical CORS, object-storage, multipart, Shoubing and table-routing paths.
- Symbols: app.web.cors.allowed-origins; app.object-storage.*; multipart max-file-size=10MB/max-request-size=11MB; existing Shoubing prefix; five SINGLE/master_data table entries.
- Repository evidence: current four profiles already hold identical Shoubing key structure and ProfileKeyParityTest checks selected paths.
- Dependencies and consumers: CORS source, properties binding, MP-SDJ, Shoubing starter.
- Why now: every consumer bean needs its environment values.
- Contract/signature changes: add the same owned keys in all profiles; test values use non-secret fake endpoint/credentials; dev/prod values are environment-injected.
- Input/output and state mapping: all five logical tables route to master_data; CORS origin list and S3 endpoint/secret/base map to typed values.
- Error and edge behavior: Shoubing stays false unless configured, so upload stays denied; no credentials in tracked files; request cap leaves multipart framing headroom.
- Standards impact: MC-CONFIG-001, MC-VALID-001, MC-SCOPE-001.
- Literal rule enforcement: Rule 7 — four-profile parity in one change; Rule 11 — existing application configuration only.
- Implementation pseudocode:


```yaml
in each profile:
    add CORS allowed-origins list
    add app.object-storage fields and 15s/20s duration defaults
    set max-file-size 10MB and max-request-size 11MB
    add category/template/spec/para/category_brand SINGLE/master_data
    keep Shoubing key paths and environment-controlled false defaults
```

- Verification contribution: ProfileKeyParityTest and properties binder test.
- After this file: every environment has the same config shape.

#### File 9 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-dev.yml`

- Purpose: fail closed until configured.
- Symbols: matching config tree; environment-controlled auth filter; multipart 10/11MB.
- Repository evidence: disabled Shoubing and no new keys.
- Dependencies and consumers: the adjacent current-layer caller and consumer in Step 4.
- Why now: follows its compile prerequisite and precedes the next consumer.
- Contract/signature changes: implement exactly the final state in the inventory.
- Input/output and state mapping: satisfy REQ-101/102/105/107 with the named typed fields.
- Error and edge behavior: preserve invalid/empty/dependency/no-side-effect cases in this Step.
- Standards impact: MC-CONFIG-001, MC-SCOPE-001, MC-TEST-001 — verified by the owning Step.
- Literal rule enforcement: Rule 7, Rule 11 — apply the file-specific obligation from Chapter 4.8.
- Implementation pseudocode:

```yaml
    preserve current module/profile/file ownership
    add only the approved property/schema/generator state
    preserve old keys/checksums and verify the named consumer
```

- Verification contribution: parity test.
- After this file: matching config tree; environment-controlled auth filter; multipart 10/11MB.

#### File 10 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-test.yml`

- Purpose: unit tests mock S3.
- Symbols: matching config tree with non-secret test-only S3 values and no connections.
- Repository evidence: H2/test profile and disabled Shoubing.
- Dependencies and consumers: the adjacent current-layer caller and consumer in Step 4.
- Why now: follows its compile prerequisite and precedes the next consumer.
- Contract/signature changes: implement exactly the final state in the inventory.
- Input/output and state mapping: satisfy REQ-101/102/105/107 with the named typed fields.
- Error and edge behavior: preserve invalid/empty/dependency/no-side-effect cases in this Step.
- Standards impact: MC-CONFIG-001, MC-SCOPE-001, MC-TEST-001 — verified by the owning Step.
- Literal rule enforcement: Rule 7, Rule 11 — apply the file-specific obligation from Chapter 4.8.
- Implementation pseudocode:

```yaml
    preserve current module/profile/file ownership
    add only the approved property/schema/generator state
    preserve old keys/checksums and verify the named consumer
```

- Verification contribution: parity/binding.
- After this file: matching config tree with non-secret test-only S3 values and no connections.

#### File 11 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-prod.yml`

- Purpose: no secrets committed.
- Symbols: same key paths and external secret placeholders.
- Repository evidence: production placeholders.
- Dependencies and consumers: the adjacent current-layer caller and consumer in Step 4.
- Why now: follows its compile prerequisite and precedes the next consumer.
- Contract/signature changes: implement exactly the final state in the inventory.
- Input/output and state mapping: satisfy REQ-101/102/105/107 with the named typed fields.
- Error and edge behavior: preserve invalid/empty/dependency/no-side-effect cases in this Step.
- Standards impact: MC-CONFIG-001, MC-SCOPE-001, MC-TEST-001 — verified by the owning Step.
- Literal rule enforcement: Rule 7, Rule 11 — apply the file-specific obligation from Chapter 4.8.
- Implementation pseudocode:

```yaml
    preserve current module/profile/file ownership
    add only the approved property/schema/generator state
    preserve old keys/checksums and verify the named consumer
```

- Verification contribution: parity test.
- After this file: same key paths and external secret placeholders.

#### File 12 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/start/config/NativeHttpSecurityConfiguration.java`

- Purpose: Install CORS and require Authentication only for the future file upload POST.
- Symbols: applicationSecurityFilterChain; CorsConfigurationSource Bean; POST /api/v1/files matcher.
- Repository evidence: current chain disables CSRF/form/basic/logout then permits all.
- Dependencies and consumers: Spring Security; Shoubing filter from File 4; future FileController.
- Why now: CORS and authorization are one servlet security configuration.
- Contract/signature changes: CORS uses exact configured origins, GET/POST/OPTIONS, Authorization/Content-Type, exposes Location, credentials=false; POST files authenticated with a Shoubing-shaped 401 AuthenticationEntryPoint for missing Bearer; all other paths permitAll.
- Input/output and state mapping: Shoubing filter writes Authentication before authorization; absent token fails protected POST.
- Error and edge behavior: OPTIONS preflight is accepted; denied Origin gets no ACAO; absent Authorization gets 401 JSON with the stable Shoubing reason; CORS is not identity.
- Standards impact: MC-REUSE-001, MC-BEAN-001, MC-CONFIG-001, MC-TEST-001.
- Literal rule enforcement: Rule 4 — named filter-chain Bean and injection; Rule 7 — profile-based origin list; Rule 11 — existing security chain.
- Implementation pseudocode:


```java
CorsConfigurationSource(allowedOrigins):
    allow GET, POST, OPTIONS; allow Authorization and Content-Type
    expose Location; allowCredentials=false
authorize POST /api/v1/files as authenticated; anyRequest remains permitAll
AuthenticationEntryPoint for missing authentication writes 401 JSON:
    code=AUTHORIZATION_HEADER_INVALID; message=Tianquan-Shoubing authentication failed
leave invalid Bearer responses to IdpBearerAuthenticationFilter
```

- Verification contribution: NativeHttpSecurityConfigurationTest becomes GREEN.
- After this file: the upload path fails closed before Controller exists.

#### File 13 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/start/config/ObjectStorageConfiguration.java`

- Purpose: Construct the Spring-managed synchronous S3Client.
- Symbols: Bean goodsS3Client.
- Repository evidence: current module uses named @Bean configuration; AWS SDK supports endpointOverride, signing region and path-style S3Configuration.
- Dependencies and consumers: validated properties from Files 5–7; Adapter in Step 5.
- Why now: dependency and property contract are present.
- Contract/signature changes: one singleton S3Client; no provider strategy.
- Input/output and state mapping: endpoint/region/credentials/path-style and timeouts → client.
- Error and edge behavior: API attempt timeout 15s, total 20s, retry max attempts 1; bean lifecycle closes client; no network during construction.
- Standards impact: MC-DEP-001, MC-BEAN-001, MC-TIME-001, MC-PATTERN-001.
- Literal rule enforcement: Rule 4 — named client Bean; Rule 9 — adapter handles endpoint variance; Rule 10 — Duration.
- Implementation pseudocode:


```java
S3Client.builder()
  .endpointOverride(endpoint)
  .region(Region.of(region))
  .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey())))
  .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(pathStyle).build())
  .overrideConfiguration(b -> b.apiCallAttemptTimeout(attempt)
      .apiCallTimeout(total).retryStrategy(retry -> retry.maxAttempts(1)))
  .build()
```

- Verification contribution: NativeHttpSecurityConfigurationTest asserts the named S3Client bean is available; test-profile values are fake and the SDK client is constructed without a request.
- After this file: one S3Client is ready for the Adapter.

- Validation working directory: goods module root; dependency tree from module root.
- Verification command: ./mvnw -B -ntp -Dtest=NativeHttpSecurityConfigurationTest,ProfileKeyParityTest,NativeRpcConfigurationTest,ObjectStorageConfigurationPropertiesTest test; ./mvnw -B -ntp -Dincludes=software.amazon.awssdk:bom,software.amazon.awssdk:s3,top.egon:egon-cola-tianquan-shoubing-starter dependency:tree
- Expected result: allowed preflight and profile paths pass; anonymous file POST is 401; property binding/redaction passes; dependency tree contains only approved AWS/Shoubing versions.
- Failure returns to: profile paths/values; property constraints; or Security/CORS wiring. Do not solve by wildcard CORS or permitting upload.
- Completion criteria: all four profiles bind, five table routing entries exist, upload matcher fails closed, S3Client builds without network.
- Rollback: revert only Step 4 paths; retain catalog/query changes.
- Commit paths: enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/start/config/NativeHttpSecurityConfigurationTest.java; enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/start/config/ProfileKeyParityTest.java; enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/start/config/NativeRpcConfigurationTest.java; enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/start/config/ObjectStorageConfigurationPropertiesTest.java; enjoyshop-service/enjoyshop-service-goods/pom.xml; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/start/config/NativeHttpSecurityConfiguration.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/start/config/ObjectStorageConfigurationProperties.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/start/config/ObjectStorageConfiguration.java; enjoyshop-service/enjoyshop-service-goods/src/main/resources/application.yml; enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-dev.yml; enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-test.yml; enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-prod.yml
- Commit: feat(goods): configure cors oidc and s3



### Step 5 — Implement JPEG/PNG validation and the S3-compatible storage Adapter

- Requirements: REQ-105, REQ-106, REQ-108
- Dependencies: Steps 1–4 committed
- Baseline state: named S3Client Bean and validated properties exist; no image model, FileManage, Domain storage port or Adapter exists.
- Observable outcome: valid bounded JPEG/PNG bytes cause one PutObject with detected MIME/extension and stable URL; invalid bytes or SDK failure never return success.
- End state: FileManage/Domain Service/Port/Infrastructure Adapter operate in process; HTTP upload remains absent for Step 6.
- Test-first gate: Required — define model/port signatures first, then validator/domain/adapter tests RED against missing or contract-stub behavior. Application tests assert Domain delegation only; Domain and S3 Adapter tests own invalid-media and remote-call failure behavior.
- Approved repository clarification (2026-10-05): ArchetypeContractConvergenceTest requires each handwritten enum to implement EgonEnum. ProductImageFormat therefore has stable internal codes while remaining unpersisted and absent from JSON; no ordinal contract is introduced.
- Plan clarification (2026-10-05): keep the Application test free of SDK types and call-count assertions; `FileDomainServiceImplTest` owns the Domain-to-Port boundary and `S3CompatibleObjectStorageAdapterTest` owns the exact AWS request/acknowledgement contract, preserving Light layer ownership.
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-JSON-001, MC-TIME-001, MC-CONFIG-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001, MC-BLOCKER-001
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11
- Ordered files:

#### File 1 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/ProductImageFormat.java`

- Purpose: Establish the typed Domain storage model needed by behavior tests.
- Symbols: `ProductImageFormat implements EgonEnum` with JPEG/PNG internal code, MIME and extension; ProductImageBO.content.
- Repository evidence: current Domain models are mutable Lombok classes; no image type exists.
- Dependencies and consumers: ProductImageValidator, FileDomainService and ObjectStoragePort.
- Why now: test source must compile against real domain input and format names.
- Contract/signature changes: bytes-only BO; format code stays internal, not persisted or serialized; ordinal is never used.
- Input/output and state mapping: bounded raw bytes → ProductImageBO → detected format.
- Error and edge behavior: constructor/accessor clone the byte array; caller filename/type are never retained.
- Standards impact: MC-NAME-001, MC-MODEL-001, MC-JSON-001.
- Literal rule enforcement: Rule 1 — ProductImageBO suffix; Rule 3 — mutable BO class/Lombok; Rule 6 — no enum JSON contract and no ordinal business value; Rule 11 — existing EgonEnum contract.
- Implementation pseudocode:


```text
ProductImageBO.builder().content(content).build() stores a clone; accessor and setter return/store clones
ProductImageFormat.JPEG = code 1, image/jpeg, jpg
ProductImageFormat.PNG = code 2, image/png, png
format codes are internal and never persisted or serialized
```

- Verification contribution: fixtures use typed models without production behavior.
- After this file: types compile; validation and S3 behavior are absent.

#### File 2 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/ProductImageBO.java`

- Purpose: no caller metadata.
- Symbols: complete mutable Lombok BO with `byte[] content`, clone-on-builder/setter/getter, and bounded validation.
- Repository evidence: no product image model.
- Dependencies and consumers: the adjacent current-layer caller and consumer in Step 5.
- Why now: follows its compile prerequisite and precedes the next consumer.
- Contract/signature changes: implement exactly the final state in the inventory.
- Input/output and state mapping: bytes→ProductImageFormat→ObjectStoragePort.
- Error and edge behavior: invalid/empty maps 610106, oversize maps 610107; neither invokes the Port.
- Standards impact: MC-NAME-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001 — verified by the owning Step.
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 11 — apply the file-specific obligation from Chapter 4.8.
- Implementation pseudocode:

```java
    declare the exact typed symbol and annotations in the Spec
    validate/map approved input fields into the target contract
    preserve edge/error behavior and assert it through the owning test
```

- Verification contribution: Validator/mapping.
- After this file: Lombok class with defensive bounded bytes.

#### File 3 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/ProductImageValidator.java`

- Purpose: Establish a compile-safe common-validator contract before its RED tests.
- Symbols: static detectFormat(byte[]) returning ProductImageFormat; extends BaseValidator and exposes common ValidationUtils.
- Repository evidence: primary Spec chooses JDK ImageIO/signature validation with no MIME dependency.
- Dependencies and consumers: FileDomainServiceImpl and ProductImageValidatorTest.
- Why now: the test must run and fail on assertions, not missing Java symbols.
- Contract/signature changes: stateless helper signature, @Slf4j and BaseValidator facade only; return/format body deliberately incomplete.
- Input/output and state mapping: bytes→format or typed invalid-file failure.
- Error and edge behavior: temporary unsupported result lets invalid/valid cases fail visibly; no storage side effect.
- Standards impact: MC-VALID-001, MC-UTIL-001.
- Literal rule enforcement: Rule 2 — common ValidationUtils facade plus BO constraints; Rule 4 — @Slf4j; Rule 5 — JDK ImageIO only; Rule 11 — Domain-owned validation.
- Implementation pseudocode:


```text
ProductImageValidator extends BaseValidator and overrides getValidationUtils()
detectFormat(bytes): return null // contract stub for RED test only
    inspect actual JPEG/PNG signature only after enforcing the byte bound\n    throw FILE_REQUEST_INVALID for unknown/corrupt signatures\n    no S3 call or URL is produced by this validator\n
    return JPEG or PNG only after checking actual bytes
    check byte length before signature inspection
    invalid format throws FILE_REQUEST_INVALID before Port use
```

- Verification contribution: makes ProductImageValidatorTest compile and fail for valid JPEG/PNG.
- After this file: the intended RED reason is executable.

#### File 4 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/domain/goods/ProductImageValidatorTest.java`

- Purpose: Specify byte-based validation before implementation.
- Symbols: acceptsJpegSignature; acceptsPngSignature; rejectsEmptyUnknownTruncatedAndOversize.
- Repository evidence: current tests use JUnit 5 and Java byte fixtures.
- Dependencies and consumers: ProductImageFormat/BO and File 2 validator signature.
- Why now: failure proves the missing detection rule after the contract exists.
- Contract/signature changes: tests only.
- Input/output and state mapping: raw bytes→enum.
- Error and edge behavior: rejects null/empty/unknown/truncated signatures with 610106 and content over 10MiB with 610107; filename and declared MIME are not inputs.
- Standards impact: MC-VALID-001, MC-UTIL-001, MC-TEST-001.
- Literal rule enforcement: Rule 2 — positive/negative validation; Rule 5 — JDK only; Rule 11 — test the selected Domain layer.
- Implementation pseudocode:


```java
assert detectFormat(valid minimal JPEG bytes) == JPEG
assert detectFormat(valid minimal PNG bytes) == PNG
assert null/empty/unknown/truncated bytes throw FILE_REQUEST_INVALID (610106)
assert over-limit bytes throw FILE_TOO_LARGE (610107)
```

- Verification contribution: RED against null-returning validator stub.
- After this file: exact size/signature cases are locked.

#### File 5 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/ProductImageValidator.java`

- Purpose: Implement Jakarta size validation and actual ImageIO format detection.
- Symbols: detectFormat(byte[]).
- Repository evidence: JDK 21 ImageIO, BaseValidator contract test and primary Spec DEC-108.
- Dependencies and consumers: FileDomainServiceImpl uses returned MIME/extension.
- Why now: RED test sets the required behavior.
- Contract/signature changes: use ProductImageBO.content @NotNull/@Size through common ValidationUtils; map null/empty/unknown/truncated to FILE_REQUEST_INVALID, over 10MiB to FILE_TOO_LARGE, and return the actual JPEG/PNG format.
- Input/output and state mapping: validate ProductImageBO.content bounds through common ValidationUtils before ImageIO reader inspection; JPEG/PNG reader format maps to ProductImageFormat.
- Error and edge behavior: check the 10MiB bound before image-reader inspection; 610107 maps to HTTP 413; invalid content maps to 610106/400; no network, filename or declared-MIME trust.
- Standards impact: MC-VALID-001, MC-UTIL-001, MC-SCOPE-001.
- Literal rule enforcement: Rule 2 — actual untrusted data validator; Rule 5 — JDK ImageIO only; Rule 11 — Domain helper.
- Implementation pseudocode:


```java
validateValue(ProductImageBO.class, "content", bytes)
map size violation above 10MiB to FILE_TOO_LARGE; map null/empty to FILE_REQUEST_INVALID
use MemoryCacheImageInputStream and ImageIO reader to detect JPEG/PNG
reader/format I/O failure -> FILE_REQUEST_INVALID; never decode or persist the file
```

- Verification contribution: validator unit tests turn GREEN.
- After this file: format check is proven without an object-store call.

#### File 6 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/ObjectStoragePort.java`

- Purpose: Define the Domain storage variation point and service API.
- Symbols: ObjectStoragePort.putObject(byte[],ProductImageFormat); FileDomainService.upload(ProductImageBO).
- Repository evidence: Egon Light packages existing Manage/DomainService/Repository separately.
- Dependencies and consumers: Domain Service implementation and S3 Adapter.
- Why now: tests need vendor-neutral signatures before implementations.
- Contract/signature changes: domain returns String URL only after port acknowledgement.
- Input/output and state mapping: valid BO→byte[]/server format→stable public URL.
- Error and edge behavior: port has no endpoint, bucket, SDK or caller metadata parameter.
- Standards impact: MC-NAME-001, MC-VALID-001, MC-PATTERN-001.
- Literal rule enforcement: Rule 1 — Port/Service names are semantic; Rule 2 — @Valid Domain boundary; Rule 9 — Adapter pattern port; Rule 11 — Domain layer.
- Implementation pseudocode:


```text
interface ObjectStoragePort { String putObject(@NotNull byte[] content, @NotNull ProductImageFormat format); }
interface FileDomainService { String upload(@Valid ProductImageBO image); }

    Domain Port accepts only bytes and server-detected format
    Infrastructure supplies bucket, key and MIME
    Application/Domain do not import AWS request/response types
```

- Verification contribution: unit tests can mock the Port independently of AWS SDK.
- After this file: Domain boundary is defined; behavior is absent.

#### File 7 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/FileDomainService.java`

- Purpose: no SDK types.
- Symbols: upload ProductImageBO→stable URL contract.
- Repository evidence: no file domain port.
- Dependencies and consumers: the adjacent current-layer caller and consumer in Step 5.
- Why now: follows its compile prerequisite and precedes the next consumer.
- Contract/signature changes: implement exactly the final state in the inventory.
- Input/output and state mapping: bounded bytes/format→one configured bucket/key/MIME/body PutObject→URL only after response.
- Error and edge behavior: S3Exception maps 610108; SdkClientException or missing acknowledgement maps 610109; no URL on failure and no retry loop.
- Standards impact: MC-NAME-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001 — verified by the owning Step.
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 11 — apply the file-specific obligation from Chapter 4.8.
- Implementation pseudocode:

```java
    declare the exact typed symbol and annotations in the Spec
    validate/map approved input fields into the target contract
    preserve edge/error behavior and assert it through the owning test
```

- Verification contribution: compile.
- After this file: upload ProductImageBO→stable URL contract.

#### File 8 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/FileUploadCommand.java`

- Purpose: Define the Application Command and orchestration signatures.
- Symbols: FileUploadCommand.content; FileManage.upload; FileManageImpl#upload; MapStruct command-to-domain converter.
- Repository evidence: BrandManage/BrandManageImpl and CreateBrandCommand use current layer conventions.
- Dependencies and consumers: Adapter later maps MultipartFile; Application maps command to ProductImageBO and calls FileDomainService.
- Why now: the Application test needs stable types and a mockable domain dependency.
- Contract/signature changes: one synchronous upload Command returning String.
- Input/output and state mapping: byte[] only; MultipartFile/name/MIME do not cross Adapter.
- Error and edge behavior: @Size(min=1,max=10485760), null rejected; servlet limit reports 413 before the Application boundary; no storage call in a contract stub.
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-BEAN-001.
- Literal rule enforcement: Rule 1 — Command/Manage names; Rule 2 — validate Command at Manage; Rule 3 — mutable Lombok class; Rule 4 — named Service and qualifiers; Rule 11 — Application layer.
- Implementation pseudocode:


```text
FileUploadCommand class with byte[] content
@Validated FileManage.upload(@Valid FileUploadCommand) returns String
FileManageImpl declaration with stable service name and qualified dependencies
```

- Verification contribution: application test compiles before behavior is added.
- After this file: command/manage contract exists; converter and orchestration remain incomplete.

#### File 9 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/FileManage.java`

- Purpose: CQE Command, no event.
- Symbols: typed upload command→String contract.
- Repository evidence: no upload use case.
- Dependencies and consumers: the adjacent current-layer caller and consumer in Step 5.
- Why now: follows its compile prerequisite and precedes the next consumer.
- Contract/signature changes: implement exactly the final state in the inventory.
- Input/output and state mapping: satisfy REQ-105 with the named typed fields.
- Error and edge behavior: preserve invalid/empty/dependency/no-side-effect cases in this Step.
- Standards impact: MC-NAME-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001 — verified by the owning Step.
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 11 — apply the file-specific obligation from Chapter 4.8.
- Implementation pseudocode:

```java
    declare the exact typed symbol and annotations in the Spec
    validate/map approved input fields into the target contract
    preserve edge/error behavior and assert it through the owning test
```

- Verification contribution: compile.
- After this file: typed upload command→String contract.

#### File 10 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/FileManageImpl.java`

- Purpose: Establish a compile-safe named Application Manage before the MapStruct converter is declared.
- Symbols: @Service upload Manage stub and FileManage.upload signature.
- Repository evidence: no upload orchestrator; current Manage classes use named Service Beans.
- Dependencies and consumers: FileDomainService is injected now; FileUploadDomainConverter is created in File 11 and injected when this file is modified in File 18.
- Why now: the test/declaration sequence must compile without referring to a converter that is not declared yet.
- Contract/signature changes: create the stable Bean and typed upload method; the stub fails until File 18.
- Input/output and state mapping: FileUploadCommand→String contract; behavior is added in File 18.
- Error and edge behavior: no successful URL or storage call from the stub.
- Standards impact: MC-NAME-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001, MC-LOG-001, MC-BEAN-001 — verified by the owning Step.
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 11, Rule 4 — apply the file-specific obligation from Chapter 4.8.
- Implementation pseudocode:

```java
@Service("fileManage") @Validated @Slf4j @RequiredArgsConstructor
@Qualifier("fileDomainService") final FileDomainService domainService
String upload(FileUploadCommand command) { throw new UnsupportedOperationException(); }
```

- Verification contribution: compile the Manage contract before the converter exists.
- After this file: named Application Bean/signature exists; tests remain RED until File 18.

#### File 11 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/converter/FileUploadDomainConverter.java`

- Purpose: Map the application Command to the Domain BO.
- Symbols: BaseForwardConverter<FileUploadCommand,ProductImageBO>.
- Repository evidence: current MapStructPlus application converters use common-core BaseForwardConverter.
- Dependencies and consumers: FileManageImpl.
- Why now: distinct layer types keep Application Command out of Domain.
- Contract/signature changes: one-way defensive content copy.
- Input/output and state mapping: command.content bytes→BO.content bytes.
- Error and edge behavior: no caller filename, media type, actor or tenant copied.
- Standards impact: MC-CONVERT-001, MC-MODEL-001, MC-UTIL-001.
- Literal rule enforcement: Rule 3 — generated MapStruct BaseForwardConverter; Rule 5 — array clone uses JDK; Rule 11 — Application converter.
- Implementation pseudocode:


```java
@Mapper(componentModel="spring", unmappedTargetPolicy=ERROR)
ProductImageBO toTarget(FileUploadCommand command)
copy content defensively
```

- Verification contribution: FileManageImplTest asserts defensive copy.
- After this file: Command→Domain mapping compiles.

#### File 12 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/service/impl/FileDomainServiceImpl.java`

- Purpose: Establish Spring implementation constructors so behavior tests can compile.
- Symbols: fileDomainService.upload; adapter.putObject; named Beans/qualified dependencies.
- Repository evidence: BrandDomainServiceImpl, BrandRepository and managed adapter patterns.
- Dependencies and consumers: Domain interfaces, ProductImageValidator and named goodsS3Client from Step 4.
- Why now: S3/domain tests need concrete types to instantiate/mock.
- Contract/signature changes: named Service/Component, @RequiredArgsConstructor, final qualified dependencies; methods temporarily throw unsupported operation for RED.
- Input/output and state mapping: ProductImageBO→validator→Port; Port bytes/format→S3 request.
- Error and edge behavior: no retry; no return URL from stubs.
- Standards impact: MC-LOG-001, MC-BEAN-001, MC-PATTERN-001.
- Literal rule enforcement: Rule 4 — @Slf4j/name/Qualifier/constructor; Rule 9 — one Adapter; Rule 11 — Infrastructure implementation.
- Implementation pseudocode:


```text
@Service("fileDomainService") method upload(ProductImageBO)
@Component("s3CompatibleObjectStorageAdapter") method putObject(bytes, format)

    detect ProductImageFormat before the storage call
    invoke ObjectStoragePort exactly once for a valid image
    propagate unavailable versus outcome-unknown error code
```

- Verification contribution: allows FileDomainServiceImplTest and S3 adapter test to fail on behavior.
- After this file: complete compile surface exists; no successful storage yet.

#### File 13 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/architecture/LightPersistenceArchitectureTest.java`

- Purpose: Keep the Light architecture inventory aligned with the new Domain Service implementation.
- Symbols: `assertEquals(8, count(javaFiles, "DomainServiceImpl.java"))`.
- Repository evidence: Step 4 has seven DomainServiceImpl classes; this Step adds FileDomainServiceImpl.
- Dependencies and consumers: FileDomainServiceImpl from File 12 and the existing Light Domain Services.
- Why now: update the static architecture expectation after the implementation type is present.
- Contract/signature changes: test expectation only; no production API changes.
- Input/output and state mapping: source inventory recognizes eight DomainServiceImpl classes in the existing Light profile.
- Error and edge behavior: unexpected architecture inventory fails module verification.
- Standards impact: MC-ARCH-001, MC-SCOPE-001, MC-TEST-001.
- Literal rule enforcement: Rule 11 — preserve the current Light structure and infrastructure ownership.
- Implementation pseudocode:

```java
var javaFiles = walk(Path.of("src/main/java")).filter(nameEndsWithJava).toList()
assertEquals(8, count(javaFiles, "DomainServiceImpl.java"))
assertTrue(source.contains("extends EgonColaRepository<"))
assertFalse(source.contains("jakarta.persistence"))
```

- Verification contribution: focused architecture test and full module verify.
- After this file: architecture inventory accounts for FileDomainServiceImpl.
#### File 14 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/service/impl/S3CompatibleObjectStorageAdapter.java`

- Purpose: adapter maps known failure vs unknown outcome; one attempt.
- Symbols: S3Client PutObject, UUID key, server MIME/ext, stable public URL.
- Repository evidence: no storage SDK adapter.
- Dependencies and consumers: the adjacent current-layer caller and consumer in Step 5.
- Why now: follows its compile prerequisite and precedes the next consumer.
- Contract/signature changes: implement exactly the final state in the inventory.
- Input/output and state mapping: satisfy REQ-105 with the named typed fields.
- Error and edge behavior: preserve invalid/empty/dependency/no-side-effect cases in this Step.
- Standards impact: MC-NAME-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001, MC-LOG-001, MC-BEAN-001 — verified by the owning Step.
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 11, Rule 4 — apply the file-specific obligation from Chapter 4.8.
- Implementation pseudocode:

```java
    declare the exact typed symbol and annotations in the Spec
    validate/map approved input fields into the target contract
    preserve edge/error behavior and assert it through the owning test
```

- Verification contribution: adapter test.
- After this file: S3Client PutObject, UUID key, server MIME/ext, stable public URL.

#### File 15 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/application/goods/FileManageImplTest.java`

- Purpose: Prove Application Command mapping and Domain Service delegation without leaking SDK concerns upward.
- Symbols: defensive_command_copy; invalid_command_rejected_by_validated_manage; domain_failure_propagates; URL returned only after Domain Service acknowledgement.
- Repository evidence: goods JUnit/Mockito pattern; BrandManageImplTest mocks the Domain Service.
- Dependencies and consumers: FileUploadCommand, FileManage, converter declaration and FileDomainService; no AWS SDK type.
- Why now: pins Application orchestration while the HTTP Controller is not present.
- Contract/signature changes: tests only.
- Input/output and state mapping: byte[] Command→defensive ProductImageBO→FileDomainService String URL.
- Error and edge behavior: Domain failures propagate; Application returns no URL unless the Domain Service acknowledges success; no SDK call occurs in this layer.
- Standards impact: MC-VALID-001, MC-CONVERT-001, MC-PATTERN-001, MC-TEST-001.
- Literal rule enforcement: Rule 2 — validated Command boundary; Rule 3 — BaseForwardConverter mapping; Rule 9 — Adapter stays in Infrastructure; Rule 11 — preserve layer ownership.
- Implementation pseudocode:


```java
FileUploadCommand.builder().content(inputBytes).build() copies the source bytes
FileUploadDomainConverter.toTarget(command) creates a defensive ProductImageBO
FileManageImpl.upload(command) calls FileDomainService once and returns its acknowledged URL
MethodValidationInterceptor rejects an empty Command before converter or Domain invocation
```

- Verification contribution: stubbed Manage fails the delegation/URL assertions; converter copy is tested independently.
- After this file: Application mapping/delegation behavior is defined; Domain and Adapter tests own storage behavior.

#### File 16 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/goods/service/impl/FileDomainServiceImplTest.java`

- Purpose: no transaction across object store.
- Symbols: invalid signature/size fails before Port; valid image calls Port once.
- Repository evidence: no file domain flow.
- Dependencies and consumers: the adjacent current-layer caller and consumer in Step 5.
- Why now: follows its compile prerequisite and precedes the next consumer.
- Contract/signature changes: implement exactly the final state in the inventory.
- Input/output and state mapping: ProductImageBO bytes→detected ProductImageFormat→ObjectStoragePort call.
- Error and edge behavior: null/empty/unknown image maps 610106; oversize maps 610107; invalid inputs make zero Port calls.
- Standards impact: MC-TEST-001, MC-SCOPE-001 — verified by the owning Step.
- Literal rule enforcement: Rule 2, Rule 6, Rule 11 — apply the file-specific obligation from Chapter 4.8.
- Implementation pseudocode:

```java
var port = mock(ObjectStoragePort.class)
var service = new FileDomainServiceImpl(port)
assert null/unknown image maps to 610106 and oversize image maps to 610107
verifyNoInteractions(port) for both invalid cases
when(port.putObject(validBytes, JPEG)).thenReturn(stableUrl)
assert service.upload(validImage) equals stableUrl; verify port called once
```

- Verification contribution: JUnit/Mockito.
- After this file: invalid signature/size fails before Port; valid image calls Port once.

#### File 17 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/goods/service/impl/S3CompatibleObjectStorageAdapterTest.java`

- Purpose: verify the S3 request, acknowledgement and failure classification without network access.
- Symbols: request bucket/key/MIME/body, stable URL, one PutObject call, S3 rejection and uncertain client failure mappings.
- Repository evidence: no S3 Adapter.
- Dependencies and consumers: the adjacent current-layer caller and consumer in Step 5.
- Why now: follows its compile prerequisite and precedes the next consumer.
- Contract/signature changes: implement exactly the final state in the inventory.
- Input/output and state mapping: one S3 PutObject request→stable public URL only after a non-null acknowledgement.
- Error and edge behavior: S3Exception maps 610108; SdkClientException or missing acknowledgement maps 610109; failures never return a URL.
- Standards impact: MC-TEST-001, MC-SCOPE-001 — verified by the owning Step.
- Literal rule enforcement: Rule 2, Rule 6, Rule 11 — apply the file-specific obligation from Chapter 4.8.
- Implementation pseudocode:

```java
var adapter = new S3CompatibleObjectStorageAdapter(mockS3Client, validProperties)
when(putObject).thenReturn(PutObjectResponse.builder().build())
assert URL uses publicUrlBase and opaque UUID extension; capture bucket/key/contentType/body bytes
verify putObject called once
assert S3Exception -> 610108; SdkClientException/null acknowledgement -> 610109; each once
```

- Verification contribution: Mockito captures the exact request, call count, acknowledgement and failure classification.
- After this file: request bucket/key/MIME/body, stable URL, error/outcome map, one call.

#### File 18 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/FileManageImpl.java`

- Purpose: Implement typed Application and Domain orchestration after RED.
- Symbols: upload(Command), upload(ProductImageBO), command-to-BO mapping.
- Repository evidence: current BrandManageImpl delegates to Domain Service through converters.
- Dependencies and consumers: FileController later; ObjectStoragePort and image validator.
- Why now: FileManageImplTest defines exact Application behavior.
- Contract/signature changes: fill method implementations only.
- Input/output and state mapping: Command bytes defensively copied to BO; Domain detects MIME/extension then calls Port once.
- Error and edge behavior: no database transaction/outbox; no auto retry; invalid bytes stop before SDK.
- Standards impact: MC-VALID-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001.
- Literal rule enforcement: Rule 2 — @Valid at boundary; Rule 3 — converter; Rule 4 — named Beans/qualifiers/safe logs; Rule 11 — layer-specific orchestration.
- Implementation pseudocode:


```java
FileManageImpl.upload(command):
    image = converter.toTarget(command)
    return fileDomainService.upload(image)
FileDomainServiceImpl.upload(image):
    format = validator.detectFormat(image.content)
    return storagePort.putObject(image.content, format)
```

- Verification contribution: FileManage and Domain tests turn GREEN.
- After this file: in-process use case is complete; S3 Adapter tests remain RED.

#### File 19 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/service/impl/FileDomainServiceImpl.java`

- Purpose: @Service name, @Slf4j, Qualifier, RequiredArgsConstructor, no DB transaction.
- Symbols: validate ProductImageBO then Port call.
- Repository evidence: no Domain Service implementation.
- Dependencies and consumers: the adjacent current-layer caller and consumer in Step 5.
- Why now: follows its compile prerequisite and precedes the next consumer.
- Contract/signature changes: implement exactly the final state in the inventory.
- Input/output and state mapping: satisfy REQ-105 with the named typed fields.
- Error and edge behavior: preserve invalid/empty/dependency/no-side-effect cases in this Step.
- Standards impact: MC-NAME-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001, MC-LOG-001, MC-BEAN-001 — verified by the owning Step.
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 11, Rule 4 — apply the file-specific obligation from Chapter 4.8.
- Implementation pseudocode:

```java
    declare the exact typed symbol and annotations in the Spec
    validate/map approved input fields into the target contract
    preserve edge/error behavior and assert it through the owning test
```

- Verification contribution: Domain service test.
- After this file: validate ProductImageBO then Port call.

#### File 20 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/converter/FileUploadDomainConverter.java`

- Purpose: explicit defensive byte copy.
- Symbols: BaseForwardConverter<FileUploadCommand,ProductImageBO>.
- Repository evidence: no app/domain conversion.
- Dependencies and consumers: the adjacent current-layer caller and consumer in Step 5.
- Why now: follows its compile prerequisite and precedes the next consumer.
- Contract/signature changes: implement exactly the final state in the inventory.
- Input/output and state mapping: satisfy REQ-105 with the named typed fields.
- Error and edge behavior: preserve invalid/empty/dependency/no-side-effect cases in this Step.
- Standards impact: MC-NAME-001, MC-VALID-001, MC-SCOPE-001, MC-TEST-001, MC-CONVERT-001 — verified by the owning Step.
- Literal rule enforcement: Rule 1, Rule 2, Rule 3, Rule 11 — apply the file-specific obligation from Chapter 4.8.
- Implementation pseudocode:

```java
    declare the exact typed symbol and annotations in the Spec
    validate/map approved input fields into the target contract
    preserve edge/error behavior and assert it through the owning test
```

- Verification contribution: MapStruct test.
- After this file: BaseForwardConverter<FileUploadCommand,ProductImageBO>.

#### File 21 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/service/impl/S3CompatibleObjectStorageAdapter.java`

- Purpose: Implement the single S3-compatible AWS SDK Adapter.
- Symbols: named Component; ObjectStoragePort.putObject.
- Repository evidence: ObjectStorageConfiguration provides goodsS3Client; user-approved AWS SDK supports endpointOverride/region/path-style.
- Dependencies and consumers: FileDomainServiceImpl.
- Why now: S3 test from File 9 fixes required request/return behavior.
- Contract/signature changes: one PutObject, return URL only after successful acknowledgement.
- Input/output and state mapping: byte[]/detected format→configured bucket/UUID.ext/MIME/RequestBody.fromBytes; base URI+encoded key→stable HTTPS URL.
- Error and edge behavior: max attempts one, 15s attempt/20s total; S3 rejection 610108, uncertain timeout 610109; no SDK exception detail, endpoint DNS or key in logs; no presign/retry.
- Standards impact: MC-DEP-001, MC-UTIL-001, MC-LOG-001, MC-BEAN-001, MC-PATTERN-001.
- Literal rule enforcement: Rule 4 — named Bean, Qualifier, constructor, @Slf4j; Rule 5 — approved SDK/JDK only; Rule 9 — Adapter; Rule 11 — vendor code in Infrastructure.
- Implementation pseudocode:


```java
key = UUID.randomUUID() + "." + format.extension
s3Client.putObject(bucket,key,format.contentType,RequestBody.fromBytes(content))
return stable public base + encoded key
catch definitive S3 failure -> CommonException(FILE_STORAGE_UNAVAILABLE)
catch indeterminate timeout -> CommonException(FILE_STORAGE_OUTCOME_UNKNOWN)
```

- Verification contribution: success/error/one-call/no-false-URL tests turn GREEN.
- After this file: internal storage path completes without contacting MinIO/Ceph.

- Validation working directory: goods module root.
- Verification command: ./mvnw -B -ntp -Dtest=ProductImageValidatorTest,FileManageImplTest,FileDomainServiceImplTest,S3CompatibleObjectStorageAdapterTest,LightPersistenceArchitectureTest test
- Expected result: contract stubs first show invalid bytes/no URL/wrong or absent PutObject behavior; after implementation all focused tests pass without network, and the Light inventory reports eight DomainServiceImpl classes.
- Failure returns to: image signature/size validator; command converter for leakage; Adapter request mapping/error classification.
- Completion criteria: only actual JPEG/PNG <=10MB calls PutObject once; stable URL only after ACK; no automatic retry.
- Rollback: revert Step 5 files; retain client/config from Step 4.
- Commit paths: enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/domain/goods/ProductImageValidatorTest.java; enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/application/goods/FileManageImplTest.java; enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/goods/service/impl/FileDomainServiceImplTest.java; enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/infrastructure/goods/service/impl/S3CompatibleObjectStorageAdapterTest.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/FileUploadCommand.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/converter/FileUploadDomainConverter.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/FileManage.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/application/goods/FileManageImpl.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/FileDomainService.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/ObjectStoragePort.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/ProductImageBO.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/ProductImageFormat.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/domain/goods/ProductImageValidator.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/service/impl/FileDomainServiceImpl.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/infrastructure/goods/service/impl/S3CompatibleObjectStorageAdapter.java; enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/architecture/LightPersistenceArchitectureTest.java
- Commit: feat(goods): add s3 product image storage

### Step 6 — Expose the Shoubing-authenticated multipart upload API

- Requirements: REQ-101, REQ-105, REQ-106, REQ-107
- Dependencies: Steps 1–5 committed
- Baseline state: file Application/Domain/S3 flow works with mocks; Step 4 filter/matcher already requires Authentication on POST /api/v1/files.
- Observable outcome: POST /api/v1/files accepts one validated multipart part under Shoubing USER authentication and returns 201, Location and ResultRecord<String>.
- End state: OpenAPI and stable 400/401/413/503 errors match the verified Starter surface; the active USER_DELEGATION/Jianshen entry decision remains an upstream token-issuance prerequisite and is not represented as a goods 403.
- Test-first gate: Required — before Controller, authenticated test session reaches a missing route and OpenAPI has no API-008 operation.
- Approved execution correction (2026-10-05): the committed Step 4 `NativeHttpSecurityConfigurationTest` used an authenticated bare POST to assert that the then-missing route returned 404. After API-008 exists, that request resolves to the multipart-only Controller and returns 415. Update the regression test to send an authenticated empty multipart request and assert 400/610106, proving the same session passes the unchanged security matcher and reaches MVC validation.
- Approved source-alignment correction (2026-10-05): Starter 5.4.1's `IdpJwtVerifier.verifyUser` verifies the USER JWT statelessly and does not expose Resource/client grant state. Its `IdpBearerAuthenticationFilter` is registered outside Spring Security by default; the valid USER authentication is cleared when the chain loads its context. Add a filter-contract fixture with a deterministic JwtDecoder, install the filter inside the POST upload SecurityFilterChain before `AnonymousAuthenticationFilter`, and keep `register-filter=false` in all profiles to prevent duplicate outer registration. A grant-specific 403 is an upstream token-issuance/application-entry response, not an API-008 response; per-request grant/revocation enforcement remains an external platform contract gap.
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-CONVERT-001, MC-LOG-001, MC-BEAN-001, MC-UTIL-001, MC-JSON-001, MC-TIME-001, MC-CONFIG-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001, MC-BLOCKER-001
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 10, Rule 11
- Ordered files:

#### File 1 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/goods/FileControllerWebTest.java`

- Purpose: Define multipart/security/error/OpenAPI contract before adapter mapping.
- Symbols: no_bearer_is_401; invalid_bearer_is_401; verified_USER_bearer_uploads; public_brand_route_remains_open; missing_file_400; over_10mb_413; Location/201; operationId uploadFile.
- Repository evidence: existing BrandControllerWebTest/BrandOpenApiContractTest use MockMvc; Starter 5.4.1 exposes IdpBearerAuthenticationFilter and supports a deterministic JwtDecoder fixture.
- Dependencies and consumers: Step 4 matcher, Step 5 FileManage, ResultRecord and Shoubing error boundary.
- Why now: tests first observe missing Controller route/schema.
- Contract/signature changes: no production changes.
- Input/output and state mapping: one verified USER Bearer plus one file form part→FileUploadCommand→stable URL record.
- Error and edge behavior: missing/invalid Bearer returns a separate Shoubing 401 body; upstream Resource-grant denial occurs before API-008; MVC errors use stable goods codes; invalid content reaches no Port.
- Standards impact: MC-VALID-001, MC-JSON-001, MC-CONFIG-001, MC-TEST-001.
- Literal rule enforcement: Rule 2 — validation/error cases; Rule 6 — JSON/OpenAPI exactness; Rule 7 — profile-dependent auth; Rule 11 — Adapter test.
- Implementation pseudocode:


```java
construct IdpBearerAuthenticationFilter with a deterministic JwtDecoder: one valid USER JWT and all other test tokens invalid
POST /api/v1/files without Authorization -> 401 from the native entry point
POST /api/v1/files with invalid Bearer -> 401 from the Shoubing filter; with valid USER Bearer and valid MultipartFile -> 201/Location
inspect generated OpenAPI multipart binary, bearer, response codes and ResultRecord<String>
```

- Verification contribution: before Controller tests see 404 for authenticated path and no operation schema.
- After this file: public acceptance and RED reason are fixed.

#### File 2 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/goods/FileOpenApiContractTest.java`

- Purpose: Shoubing error response documented separately.
- Symbols: multipart binary, bearer security, 201 Location, errors.
- Repository evidence: no upload operation.
- Dependencies and consumers: the adjacent current-layer caller and consumer in Step 6.
- Why now: follows its compile prerequisite and precedes the next consumer.
- Contract/signature changes: implement exactly the final state in the inventory.
- Input/output and state mapping: satisfy REQ-105/107 with the named typed fields.
- Error and edge behavior: preserve invalid/empty/dependency/no-side-effect cases in this Step.
- Standards impact: MC-TEST-001, MC-SCOPE-001 — verified by the owning Step.
- Literal rule enforcement: Rule 2, Rule 6, Rule 11 — apply the file-specific obligation from Chapter 4.8.
- Implementation pseudocode:

```java
    arrange exact Spec fixtures
    invoke the named public method, mapper or route
    assert result/error and required side-effect/no-call behavior
```

- Verification contribution: generated OpenAPI.
- After this file: multipart binary, bearer security, 201 Location, errors.

#### File 3 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/goods/pojo/dto/FileUploadDTO.java`

- Purpose: Keep MultipartFile restricted to the HTTP Adapter boundary.
- Symbols: file field, @NotNull, @Schema(type=binary).
- Repository evidence: DTO package and Lombok class convention.
- Dependencies and consumers: FileController and FileUploadAdapterConverter.
- Why now: typed request model before MapStruct implementation.
- Contract/signature changes: one multipart part, no filename/MIME/tenant/actor fields.
- Input/output and state mapping: MultipartFile.size/content→file and then byte[] Command.
- Error and edge behavior: empty file and servlet request limit rejected before object store.
- Standards impact: MC-NAME-001, MC-VALID-001, MC-MODEL-001, MC-JSON-001.
- Literal rule enforcement: Rule 1 — DTO suffix; Rule 2 — @NotNull/file size; Rule 3 — class/Lombok; Rule 6 — multipart schema; Rule 11 — Adapter layer.
- Implementation pseudocode:


```java
class FileUploadDTO {
    @NotNull @Schema(type=BINARY)
    MultipartFile file;
}
    exactly one multipart part named file is accepted\n    original filename and declared MIME are not copied to Command\n    size is bounded before the application storage flow\n
    bind exactly one required multipart part named file
    multipart request limit is 11MB and file limit is 10MB
    do not carry caller filename or MIME into Application
```

- Verification contribution: MVC/form binding test.
- After this file: MultipartFile exists only at the Controller boundary.

#### File 4 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/goods/pojo/convertor/FileUploadAdapterConverter.java`

- Purpose: Convert the HTTP DTO to the Application Command.
- Symbols: BaseForwardConverter<FileUploadDTO,FileUploadCommand>; toTarget may propagate IOException.
- Repository evidence: BrandAdapterConverter maps Adapter DTOs through MapStructPlus.
- Dependencies and consumers: FileController and FileManage.
- Why now: no MultipartFile type crosses Application.
- Contract/signature changes: map only bytes; explicitly discard filename and declared MIME.
- Input/output and state mapping: FileUploadDTO.file.getBytes()→FileUploadCommand.content.
- Error and edge behavior: propagate read failure to safe request-invalid handling; no partial command.
- Standards impact: MC-CONVERT-001, MC-VALID-001, MC-UTIL-001.
- Literal rule enforcement: Rule 2 — HTTP boundary; Rule 3 — BaseForwardConverter/MapStruct; Rule 5 — JDK byte[] only.
- Implementation pseudocode:


```java
map FileUploadDTO.file.getBytes() to command.content
do not map MultipartFile.originalFilename or contentType
    propagate unreadable multipart bytes as a safe file request failure\n    preserve byte content exactly and exclude caller metadata\n
    map FileUploadDTO.file bytes into FileUploadCommand.content
    propagate read failure as a safe invalid-request error
    never map filename or declared content type
```

- Verification contribution: converter test proves input metadata does not influence result.
- After this file: Adapter conversion is available.

#### File 5 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/goods/FileController.java`

- Purpose: Publish API-008 and keep HTTP orchestration in the Adapter.
- Symbols: @RestController("fileController"); POST /api/v1/files; operationId uploadFile.
- Repository evidence: BrandController uses named MVC Bean, @Validated, @RequiredArgsConstructor, @Qualifier, @Slf4j and ResultRecord.
- Dependencies and consumers: external browser; FileManage; FileUploadAdapterConverter.
- Why now: Step 4 protects this route and Step 5 has a working upload use case.
- Contract/signature changes: multipart/form-data file part; 201 Created; Location equals response.data stable URL.
- Input/output and state mapping: bind DTO→convert Command→Manage→ResultRecord<String>; no DAO/S3 imports.
- Error and edge behavior: request failures before network; storage failure has no URL; unauthorized requests never reach Manage; no original filename logged.
- Standards impact: MC-VALID-001, MC-JSON-001, MC-LOG-001, MC-BEAN-001, MC-CONVERT-001.
- Literal rule enforcement: Rule 1 — Controller/DTO/Command semantic types; Rule 2 — @Valid boundary; Rule 3 — converter; Rule 4 — Bean/log/Qualifier; Rule 6 — Jackson wrapper; Rule 11 — Adapter only.
- Implementation pseudocode:


```java
@PostMapping(consumes=MULTIPART_FORM_DATA)
ResponseEntity<ResultRecord<String>> uploadFile(@Valid @ModelAttribute FileUploadDTO request):
    command = converter.toTarget(request)
    url = manage.upload(command)
    return created(URI.create(url)).body(ResultRecord.success(url))
```

- Verification contribution: FileControllerWebTest and OpenAPI contract become GREEN.
- After this file: only docs/final module gates remain.

#### File 6 — `MODIFY enjoyshop-service/enjoyshop-service-goods/README.zh-CN.md`

- Purpose: Document exact launch inputs and release manual gates for the external services.
- Symbols: environment names for CORS, storage endpoint/bucket/region/public URL/secrets/timeouts and Shoubing issuer/JWK/resource; document `register-filter=false` and upstream USER_DELEGATION/app-entry authorization; one 10MB file limit.
- Repository evidence: existing README documents dev/test/prod profiles and verify command.
- Dependencies and consumers: service operator/release owner.
- Why now: all required property names exist in POM/profile/source and must not drift.
- Contract/signature changes: documentation only; no local object-store container command.
- Input/output and state mapping: secret values stay in secret manager/env; test profile values are examples only.
- Error and edge behavior: readiness gate requires ACTOR-003 rows, Shoubing/Jianshen authorization for token acquisition, required shared Redis, supported endpoint, bucket public GET policy and CORS origins; API-008 bearer failures use 401, not a grant-specific 403.
- Standards impact: MC-CONFIG-001, MC-SCOPE-001, MC-TEST-001.
- Literal rule enforcement: Rule 7 — document all profile keys; Rule 11 — describe existing goods app.
- Implementation pseudocode:


```text
list required property names and whether secret/non-secret
state test and verify commands
describe TEST-114 manual authenticated upload then public GET
```

- Verification contribution: compare README keys against ProfileKeyParityTest/ConfigurationProperties.
- After this file: an operator can configure deployment without guessing names.

#### File 7 — `CREATE enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/goods/pojo/convertor/FileUploadAdapterConverterTest.java`

- Purpose: no SDK/network.
- Symbols: FileUploadDTO bytes map into FileUploadCommand; caller MIME/name not copied.
- Repository evidence: no multipart converter.
- Dependencies and consumers: the adjacent current-layer caller and consumer in Step 6.
- Why now: follows its compile prerequisite and precedes the next consumer.
- Contract/signature changes: implement exactly the final state in the inventory.
- Input/output and state mapping: satisfy REQ-105/106 with the named typed fields.
- Error and edge behavior: preserve invalid/empty/dependency/no-side-effect cases in this Step.
- Standards impact: MC-TEST-001, MC-SCOPE-001 — verified by the owning Step.
- Literal rule enforcement: Rule 2, Rule 6, Rule 11 — apply the file-specific obligation from Chapter 4.8.
- Implementation pseudocode:

```java
    arrange exact Spec fixtures
    invoke the named public method, mapper or route
    assert result/error and required side-effect/no-call behavior
```

- Verification contribution: MapStruct test.
- After this file: FileUploadDTO bytes map into FileUploadCommand; caller MIME/name not copied.

#### File 8 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/start/config/NativeHttpSecurityConfigurationTest.java`

- Purpose: Keep the Step 4 security regression valid after API-008 is added.
- Symbols: anonymous POST remains Shoubing-shaped 401; authenticated empty multipart request reaches MVC and returns 400/610106.
- Repository evidence: the current test asserts 404 only because its authenticated POST had no content type while the route did not yet exist.
- Dependencies and consumers: Step 4 matcher and the new FileController multipart route.
- Why now: adding API-008 changes the prior “missing route” response without changing the security chain.
- Contract/signature changes: test expectation only; no production security or API scope change.
- Input/output and state mapping: authenticated session + empty multipart form → FileUploadDTO validation → FILE_REQUEST_INVALID.
- Error and edge behavior: anonymous request still fails before MVC; authenticated missing-file request returns the standard 400 envelope.
- Standards impact: MC-TEST-001, MC-SCOPE-001 — verified by the owning Step.
- Literal rule enforcement: Rule 2 — missing-file validation; Rule 6 — ResultRecord JSON; Rule 11 — adapter/security boundary.
- Implementation pseudocode:

```java
arrange the existing MockMvc context and authenticated-session fixture
POST /api/v1/files without SecurityContext -> assert 401 and the Shoubing body
POST multipart /api/v1/files with SecurityContext and no file -> assert 400/610106
verify both rejected requests do not invoke FileManage
```

- Verification contribution: proves only authentication—not the missing route—continues to block anonymous upload.
- After this file: the pre-API-008 assertion no longer fails the full module regression gate.

#### File 9 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/start/config/NativeHttpSecurityConfiguration.java`

- Purpose: Install Shoubing USER verification inside the SecurityContext lifecycle and retain the public fallback chain.
- Symbols: POST-only `fileUploadSecurityFilterChain`; `applicationSecurityFilterChain` remains the public fallback; insert `IdpBearerAuthenticationFilter` before `AnonymousAuthenticationFilter`.
- Repository evidence: Starter 5.4.1 registers its filter at Servlet order -102; the new valid-USER Bearer test returned 401 because Spring Security replaced that outer filter context.
- Dependencies and consumers: `IdpBearerAuthenticationFilter`, CORS source, `ObjectMapper`, `HttpSecurity`.
- Why now: current MockMvc RED proves valid Bearer is lost before authorization; use Spring Security's established chain extension point.
- Contract/signature changes: only POST `/api/v1/files` selects the authenticated chain; every other request selects the existing permit-all policy.
- Input/output and state mapping: validated USER Bearer → IdpAuthenticationToken inside the upload chain → authenticated MVC request.
- Error and edge behavior: missing Bearer continues to the native 401 entry point; invalid Bearer is rejected by Shoubing; `register-filter=true` fails fast to prevent duplicate Servlet registration.
- Standards impact: MC-ARCH-001, MC-REUSE-001, MC-VALID-001, MC-BEAN-001, MC-SCOPE-001, MC-TEST-001.
- Literal rule enforcement: Rule 2 — verified USER boundary; Rule 4 — named chain Bean; Rule 11 — security remains in Start/Adapter composition.
- Implementation pseudocode:

```java
configure the POST /api/v1/files matcher before the permit-all fallback chain
require the Shoubing Servlet registration property to remain false and resolve its optional filter Bean
insert that filter before AnonymousAuthenticationFilter so SecurityContextHolderFilter does not overwrite its identity
retain shared CORS and the JSON 401 entry point in both chains; leave other paths permitAll
```

- Verification contribution: the valid USER Bearer integration test must reach the mocked FileManage; invalid Bearer must stop at Shoubing.
- After this file: filter authentication is inside the same Spring Security chain that authorizes the upload request.

#### File 10 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/resources/application.yml`

- Purpose: Disable Starter-owned outer Servlet registration for the upload filter.
- Symbols: `egon.cola.platform.tianquan.shoubing.register-filter=false`.
- Repository evidence: base profile currently permits an environment override to `true`, which would create a second filter path.
- Dependencies and consumers: Step 4 Shoubing Starter properties and `NativeHttpSecurityConfiguration`.
- Why now: the application now installs the filter directly in its upload SecurityFilterChain.
- Contract/signature changes: preserve the same key path; make the registration value fixed `false`.
- Input/output and state mapping: Boot configuration → Starter registration disabled; chain injection stays active when Shoubing is enabled.
- Error and edge behavior: duplicate outer registration is rejected by NativeHttpSecurityConfiguration.
- Standards impact: MC-CONFIG-001, MC-SCOPE-001, MC-TEST-001.
- Literal rule enforcement: Rule 7 — parity across all profiles; Rule 11 — existing Light configuration.
- Implementation pseudocode:

```yaml
keep the Shoubing register-filter property path in the base profile
set its value to false so the Starter will not register an outer Servlet filter
preserve issuer, JWK, Resource Server and Resource URI configuration values
```

- Verification contribution: ProfileKeyParityTest and NativeRpcConfigurationTest.
- After this file: the base profile cannot double-register the Shoubing filter.

#### File 11 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-dev.yml`

- Purpose: Match the fixed Shoubing filter registration value in dev.
- Symbols: `egon.cola.platform.tianquan.shoubing.register-filter=false`.
- Repository evidence: dev profile currently exposes the filter-registration environment variable.
- Dependencies and consumers: Starter auto-configuration and the upload SecurityFilterChain.
- Why now: profile structures remain aligned after the security-chain correction.
- Contract/signature changes: property value only; preserve all Shoubing keys.
- Input/output and state mapping: dev property binding → disabled Servlet registration.
- Error and edge behavior: no duplicate filter invocation or authentication context reset.
- Standards impact: MC-CONFIG-001, MC-SCOPE-001, MC-TEST-001.
- Literal rule enforcement: Rule 7 — profile parity; Rule 11 — current Light profile.
- Implementation pseudocode:

```yaml
retain all existing dev Shoubing property paths
set register-filter to false instead of reading TIANQUAN_SHOUBING_REGISTER_FILTER
leave issuer, JWK, Resource URI and other dev values unchanged
```

- Verification contribution: profile value assertion and full goods verify.
- After this file: dev uses the in-chain filter path.

#### File 12 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-prod.yml`

- Purpose: Match the fixed Shoubing filter registration value in prod.
- Symbols: `egon.cola.platform.tianquan.shoubing.register-filter=false`.
- Repository evidence: prod profile currently exposes the filter-registration environment variable.
- Dependencies and consumers: Starter auto-configuration and the upload SecurityFilterChain.
- Why now: production must use the verified in-chain registration boundary.
- Contract/signature changes: property value only; preserve all Shoubing keys.
- Input/output and state mapping: prod property binding → disabled Servlet registration.
- Error and edge behavior: config guard fails startup if external overrides request duplicate Servlet registration.
- Standards impact: MC-CONFIG-001, MC-SCOPE-001, MC-TEST-001.
- Literal rule enforcement: Rule 7 — profile parity; Rule 11 — current Light profile.
- Implementation pseudocode:

```yaml
retain all existing prod Shoubing property paths
set register-filter to false and do not document a runtime enable override
preserve issuer, JWK, Resource URI and secret-source configuration
```

- Verification contribution: profile value assertion and full goods verify.
- After this file: prod uses the verified in-chain filter path.

#### File 13 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/start/config/NativeRpcConfigurationTest.java`

- Purpose: Guard the fixed Shoubing filter-registration value across every profile.
- Symbols: base/dev/test/prod `register-filter=false` assertions.
- Repository evidence: existing test already inspects Shoubing bindings and external lifecycle defaults.
- Dependencies and consumers: four profile YAML files and Starter properties.
- Why now: changing filter ownership requires a value-level profile assertion.
- Contract/signature changes: validation only; no production interface changes.
- Input/output and state mapping: profile YAML values → expected disabled outer registration.
- Error and edge behavior: any profile with `register-filter=true` fails the test before deployment.
- Standards impact: MC-CONFIG-001, MC-TEST-001, MC-SCOPE-001.
- Literal rule enforcement: Rule 7 — all profile values are checked; Rule 11 — current architecture.
- Implementation pseudocode:

```java
load application.yml, application-dev.yml, application-test.yml and application-prod.yml
for each profile assert egon.cola.platform.tianquan.shoubing.register-filter equals false
retain existing assertions that test disables external identity and RPC lifecycles
```

- Verification contribution: profile configuration test.
- After this file: the duplicate registration mode cannot regress silently.

#### File 14 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/goods/FileController.java`

- Purpose: Publish only the API-008 response codes actually produced by the goods route and Shoubing filter.
- Symbols: operationId `uploadFile`; responses 201/400/401/413/503; no grant-specific 403 response.
- Repository evidence: IdpBearerAuthenticationFilter returns 401 for invalid USER Bearer; goods has no per-request grant verifier or AccessDenied branch.
- Dependencies and consumers: generated OpenAPI and product-media API caller.
- Why now: a 403 annotation would promise behavior absent from the selected Shoubing 5.4.1 USER token contract.
- Contract/signature changes: OAS response documentation only; grant denial remains an upstream token/application-entry response.
- Input/output and state mapping: Bearer/filter failure → Shoubing 401; application/storage failures → ResultRecord.
- Error and edge behavior: no HTTP 403 claim is made for this Controller.
- Standards impact: MC-JSON-001, MC-SCOPE-001, MC-TEST-001.
- Literal rule enforcement: Rule 6 — exact Jackson/OAS contract; Rule 11 — adapter boundary.
- Implementation pseudocode:

```java
keep 201 with Location and the ResultRecord<String> schema
keep 400, 401, 413 and 503 responses with their owning JSON envelopes
remove the 403 API response whose grant enforcement is owned upstream
```

- Verification contribution: FileOpenApiContractTest exact response-code set.
- After this file: `/v3/api-docs` describes the actual API-008 security behavior.

#### File 15 — `MODIFY enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/goods/FileOpenApiContractTest.java`

- Purpose: Assert the corrected Shoubing and goods response boundary.
- Symbols: 201/400/401/413/503; no 403 in API-008 responses.
- Repository evidence: FileController produces goods errors and Shoubing invalid-token 401 only.
- Dependencies and consumers: springdoc-generated `/v3/api-docs` document.
- Why now: prevent reintroducing an unsupported grant-specific API response.
- Contract/signature changes: test expectation only.
- Input/output and state mapping: generated document → exact upload response-code set.
- Error and edge behavior: USER grant denial is upstream and not represented as a goods response.
- Standards impact: MC-TEST-001, MC-SCOPE-001.
- Literal rule enforcement: Rule 6 — Jackson/OpenAPI response shape; Rule 11 — Light Adapter contract.
- Implementation pseudocode:

```java
fetch /v3/api-docs through MockMvc
assert API-008 documents 201, 400, 401, 413 and 503
assert binary multipart, bearer security and Location remain present
```

- Verification contribution: generated OAS regression test.
- After this file: route docs match actual Shoubing filter and goods error producers.

- Validation working directory: goods module root.
- Verification command: ./mvnw -B -ntp -Dtest=FileControllerWebTest,FileOpenApiContractTest,FileUploadAdapterConverterTest,BaseExceptionHandlerTest,NativeHttpSecurityConfigurationTest,NativeRpcConfigurationTest,ProfileKeyParityTest test; ./mvnw -B -ntp verify
- Expected result: API/OpenAPI/error tests pass; final verify runs the Light architecture gate; no runtime app, DB, Redis, Shoubing or object store is started.
- Failure returns to: Controller/Converter for multipart binding; security matcher only if anonymous request passes; BaseExceptionHandler for status/body mismatch; README/profile only for config drift.
- Completion criteria: actual Shoubing filter fixture validates invalid/valid USER Bearer, only POST files requires authentication, OAS matches produced 401/400/413/503 responses, and full goods verify succeeds. Per-request USER Resource-grant enforcement remains an explicit upstream contract gap from EVD-019.
- Rollback: revert only Step 6 files; do not delete external object bytes or Step 1 schema/Manifest.
- Commit paths: enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/goods/FileControllerWebTest.java; enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/goods/FileOpenApiContractTest.java; enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/adapter/goods/pojo/convertor/FileUploadAdapterConverterTest.java; enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/start/config/NativeHttpSecurityConfigurationTest.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/start/config/NativeHttpSecurityConfiguration.java; enjoyshop-service/enjoyshop-service-goods/src/main/resources/application.yml; enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-dev.yml; enjoyshop-service/enjoyshop-service-goods/src/main/resources/application-prod.yml; enjoyshop-service/enjoyshop-service-goods/src/test/java/top/egon/enjoyshop/goods/start/config/NativeRpcConfigurationTest.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/goods/pojo/dto/FileUploadDTO.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/goods/pojo/convertor/FileUploadAdapterConverter.java; enjoyshop-service/enjoyshop-service-goods/src/main/java/top/egon/enjoyshop/goods/adapter/goods/FileController.java; enjoyshop-service/enjoyshop-service-goods/README.zh-CN.md
- Commit: feat(goods): expose authenticated image upload
- Corrective commit for this Step: fix(goods): integrate Shoubing filter with upload chain



## 8. Test, Validation, and Quality Gates

Commands in this chapter are future implementation gates; none has been run during Plan writing.

| Gate/Test | Level and exact command | Expected result | Requirements/Steps | Failure returns to |
| --- | --- | --- | --- | --- |
| TEST-115 CatalogDdlContractTest | goods module: ./mvnw -B -ntp -Dtest=CatalogDdlContractTest test | five tables, inherited columns/FKs/indexes, one new Manifest hash and unchanged old prefix pass; no DB | REQ-102/108 / Step 1 | DDL or Manifest |
| TEST-118 Domain proxy validation | goods module: ./mvnw -B -ntp -Dtest=StudentManagementApplicationTest test | Spring-managed BrandDomainService proxy rejects null id and negative expectedVersion after the Domain interface loses its Spring annotation | REQ-001/002/022 / Step 1 | Domain contract or proxy validation |
| Native generator | enjoyshop root with EGON_CODEGEN_CLASSPATH: scripts/egon-codegen.sh plan, review final JSON; apply emitted planId; then check | exactly one SpecPO ADD, no conflict/destructive action; APPLIED and clean check | REQ-102/108 / Step 1 | config/DDL/tooling; stop on CONFLICT/BLOCKED_TOOLING |
| TEST-103/104/105 Brand contract/query | goods module: ./mvnw -B -ntp -Dtest=BrandControllerWebTest,BrandOpenApiContractTest,BrandManageImplTest,BrandCategoryQueryRepositoryTest,BrandQuerySqlShapeTest test | binding/OAS, App→Domain propagation, same-name union, no duplicates, tenant/deleted filtering, filtered total and stable sort pass | REQ-103 / Step 2 | Brand mapper/query mapping |
| Category validator/group | goods module: ./mvnw -B -ntp -Dtest=ValidCategoryNameValidatorTest,GoodsCatalogQueryValidationTest test | optional/required, whitespace/120/121 and shared group constraints pass | REQ-103 / Step 2 | Common constraint/group |
| TEST-106–108/117 Spec query | goods module: ./mvnw -B -ntp -Dtest=SpecControllerWebTest,SpecOpenApiContractTest,SpecAdapterConverterTest,SpecManageImplTest,SpecQueryRepositoryTest,SpecQuerySqlShapeTest,SpecCatalogQueryValidationTest test | API route/schema, active tenant joins, dedupe, seq/id, options array, empty and group failure all pass | REQ-104 / Step 3 | Spec SQL/layers/validation |
| TEST-116 error contract | goods module: ./mvnw -B -ntp -Dtest=BaseExceptionHandlerTest test | brand 610101 preserved; catalog/file 610110/610106, multipart 413/610107, storage 503/610108–610109 | REQ-103–106 / Step 3 | error status/route selection |
| TEST-101/102/112 security/config | goods module: ./mvnw -B -ntp -Dtest=NativeHttpSecurityConfigurationTest,ProfileKeyParityTest,NativeRpcConfigurationTest,ObjectStorageConfigurationPropertiesTest test | allowed/rejected CORS, anonymous upload denial, public brand read, identical key paths, valid/redacted properties pass | REQ-101/102/105/107 / Step 4 | security/profile/property |
| TEST-109/110 image/storage | goods module: ./mvnw -B -ntp -Dtest=ProductImageValidatorTest,FileManageImplTest,FileDomainServiceImplTest,S3CompatibleObjectStorageAdapterTest,LightPersistenceArchitectureTest test | only real bounded JPEG/PNG reaches one PutObject; ACK-only stable URL; known/unknown failures distinct; Light inventory reports eight DomainServiceImpl classes | REQ-105/106/108 / Step 5 | validator/converter/Port/Adapter |
| TEST-111/113 File HTTP/docs | goods module: ./mvnw -B -ntp -Dtest=FileControllerWebTest,FileOpenApiContractTest,FileUploadAdapterConverterTest test | Shoubing filter fixture rejects invalid Bearer and authenticates USER; multipart maps bytes only; status/Location/envelope/docs match | REQ-105–107 / Step 6 | controller/filter-chain/converter/OAS |
| TEST-114 manual runtime | after deployment only | authenticate with Shoubing USER Resource grant, PutObject to external MinIO/Ceph, GET stable public URL, compare response bytes | REQ-105/106 / release gate | storage/security/deployment owner |
| full module and architecture | goods module: ./mvnw -B -ntp verify | all tests plus verify-bound Light architecture verifier pass | REQ-101–108 / final Step | return to owning Step |
| path/diff audit | enjoyshop root: git diff --check; git status --short; git diff --stat; git diff -- <Step-owned paths> | no whitespace errors, old migration unchanged, only current Step paths staged/changed | every commit | current Step |

No gate starts a persistent application/server or requires Docker/PostgreSQL/Redis/issuer/Ceph. TEST-114 is the only genuine platform/object-store integration proof and remains a named release-owner action.

## 9. Migration, Compatibility, Rollout, and Rollback

- Schema: Step 1 creates exactly V20260929_001__initialize_catalog_lookup_schema.sql and one matching Manifest entry. V20260913_001 and V20260923_001 remain immutable.
- DDL runner: deployment applies the new script through existing EgonColaPostgreDdlRunner/MP-SDJ startup governance. This Plan does not execute SQL or claim a physical database migrated.
- Generator: its v2 plan is produced with configVersion 1, logicalTables spec and artifacts po only. SpecDAO/XML are custom and never overwritten by codegen. After successful apply, append the complete consumed-script entry to docs/egon/codegen/ddl-consumption-log.md.
- API compatibility: API-001 gains one optional filter; no categoryName preserves all existing full/filter/page semantics, records order and PageResultRecord fields. API-007 and API-008 are additive.
- Security rollout: configure exact browser origins; activate Shoubing Resource Server, issuer/JWK, USER_DELEGATION/Jianshen app-entry authorization and Redis readers; enable the Starter while keeping `register-filter=false` so the upload chain owns filter placement. Missing/invalid upload Bearer remains fail closed.
- Storage rollout: provide supported external MinIO/Ceph endpoint, product-media bucket, path-style support, secret credentials, separate stable public HTTPS base and public GET policy. No local object-store container is added.
- Data rollout: ACTOR-003 publishes tenant-consistent catalog data after the runner creates tables. Until then read APIs return empty collections.
- Manual acceptance: release owner runs TEST-114 with a USER token obtained through the provisioned USER_DELEGATION/Jianshen app-entry authorization, then checks upload/read-back, oversize input (413) and unknown storage outcome (503, no URL). Grant denial must occur in the upstream authorization/token-issuance flow; goods does not claim a per-request 403 branch.
- Rollback before deployment: revert only that uncommitted Step’s paths. After DDL is applied, retain table/history; roll back the application artifact without dropping schema.
- Rollback after image uploads: do not delete remote objects and do not change the public base/bucket while product records refer to those URLs. Any future cleanup/delete behavior requires a separate accepted Spec.

## 10. Requirement-to-Step Traceability Matrix

| Requirement | Spec source | Owning Step | Exact paths/output | Validation gate | Acceptance evidence |
| --- | --- | --- | --- | --- | --- |
| REQ-001 | Accepted predecessor Spec §§4–20 | Step 1 (baseline invariant; unchanged) | existing scaffold/Brand contract/source remains outside the file tree | final goods verify + diff audit | baseline at 60d2fe3 and completed predecessor Plan; no requirement is reimplemented |
| REQ-002 | Accepted predecessor Spec §§4–20 | Step 1 (baseline invariant; unchanged) | existing scaffold/Brand contract/source remains outside the file tree | final goods verify + diff audit | baseline at 60d2fe3 and completed predecessor Plan; no requirement is reimplemented |
| REQ-003 | Accepted predecessor Spec §§4–20 | Step 1 (baseline invariant; unchanged) | existing scaffold/Brand contract/source remains outside the file tree | final goods verify + diff audit | baseline at 60d2fe3 and completed predecessor Plan; no requirement is reimplemented |
| REQ-004 | Accepted predecessor Spec §§4–20 | Step 1 (baseline invariant; unchanged) | existing scaffold/Brand contract/source remains outside the file tree | final goods verify + diff audit | baseline at 60d2fe3 and completed predecessor Plan; no requirement is reimplemented |
| REQ-005 | Accepted predecessor Spec §§4–20 | Step 1 (baseline invariant; unchanged) | existing scaffold/Brand contract/source remains outside the file tree | final goods verify + diff audit | baseline at 60d2fe3 and completed predecessor Plan; no requirement is reimplemented |
| REQ-006 | Accepted predecessor Spec §§4–20 | Step 1 (baseline invariant; unchanged) | existing scaffold/Brand contract/source remains outside the file tree | final goods verify + diff audit | baseline at 60d2fe3 and completed predecessor Plan; no requirement is reimplemented |
| REQ-007 | Accepted predecessor Spec §§4–20 | Step 1 (baseline invariant; unchanged) | existing scaffold/Brand contract/source remains outside the file tree | final goods verify + diff audit | baseline at 60d2fe3 and completed predecessor Plan; no requirement is reimplemented |
| REQ-008 | Accepted predecessor Spec §§4–20 | Step 1 (baseline invariant; unchanged) | existing scaffold/Brand contract/source remains outside the file tree | final goods verify + diff audit | baseline at 60d2fe3 and completed predecessor Plan; no requirement is reimplemented |
| REQ-009 | Accepted predecessor Spec §§4–20 | Step 1 (baseline invariant; unchanged) | existing scaffold/Brand contract/source remains outside the file tree | final goods verify + diff audit | baseline at 60d2fe3 and completed predecessor Plan; no requirement is reimplemented |
| REQ-010 | Accepted predecessor Spec §§4–20 | Step 1 (baseline invariant; unchanged) | existing scaffold/Brand contract/source remains outside the file tree | final goods verify + diff audit | baseline at 60d2fe3 and completed predecessor Plan; no requirement is reimplemented |
| REQ-011 | Accepted predecessor Spec §§4–20 | Step 1 (baseline invariant; unchanged) | existing scaffold/Brand contract/source remains outside the file tree | final goods verify + diff audit | baseline at 60d2fe3 and completed predecessor Plan; no requirement is reimplemented |
| REQ-012 | Accepted predecessor Spec §§4–20 | Step 1 (baseline invariant; unchanged) | existing scaffold/Brand contract/source remains outside the file tree | final goods verify + diff audit | baseline at 60d2fe3 and completed predecessor Plan; no requirement is reimplemented |
| REQ-013 | Accepted predecessor Spec §§4–20 | Step 1 (baseline invariant; unchanged) | existing scaffold/Brand contract/source remains outside the file tree | final goods verify + diff audit | baseline at 60d2fe3 and completed predecessor Plan; no requirement is reimplemented |
| REQ-014 | Accepted predecessor Spec §§4–20 | Step 1 (baseline invariant; unchanged) | existing scaffold/Brand contract/source remains outside the file tree | final goods verify + diff audit | baseline at 60d2fe3 and completed predecessor Plan; no requirement is reimplemented |
| REQ-015 | Accepted predecessor Spec §§4–20 | Step 1 (baseline invariant; unchanged) | existing scaffold/Brand contract/source remains outside the file tree | final goods verify + diff audit | baseline at 60d2fe3 and completed predecessor Plan; no requirement is reimplemented |
| REQ-016 | Accepted predecessor Spec §§4–20 | Step 1 (baseline invariant; unchanged) | existing scaffold/Brand contract/source remains outside the file tree | final goods verify + diff audit | baseline at 60d2fe3 and completed predecessor Plan; no requirement is reimplemented |
| REQ-017 | Accepted predecessor Spec §§4–20 | Step 1 (baseline invariant; unchanged) | existing scaffold/Brand contract/source remains outside the file tree | final goods verify + diff audit | baseline at 60d2fe3 and completed predecessor Plan; no requirement is reimplemented |
| REQ-018 | Accepted predecessor Spec §§4–20 | Step 1 (baseline invariant; unchanged) | existing scaffold/Brand contract/source remains outside the file tree | final goods verify + diff audit | baseline at 60d2fe3 and completed predecessor Plan; no requirement is reimplemented |
| REQ-019 | Accepted predecessor Spec §§4–20 | Step 1 (baseline invariant; unchanged) | existing scaffold/Brand contract/source remains outside the file tree | final goods verify + diff audit | baseline at 60d2fe3 and completed predecessor Plan; no requirement is reimplemented |
| REQ-020 | Accepted predecessor Spec §§4–20 | Step 1 (baseline invariant; unchanged) | existing scaffold/Brand contract/source remains outside the file tree | final goods verify + diff audit | baseline at 60d2fe3 and completed predecessor Plan; no requirement is reimplemented |
| REQ-021 | Accepted predecessor Spec §§4–20 | Step 1 (baseline invariant; unchanged) | existing scaffold/Brand contract/source remains outside the file tree | final goods verify + diff audit | baseline at 60d2fe3 and completed predecessor Plan; no requirement is reimplemented |
| REQ-022 | Accepted predecessor Spec §§4–20 | Step 1 (baseline invariant; unchanged) | existing scaffold/Brand contract/source remains outside the file tree | final goods verify + diff audit | baseline at 60d2fe3 and completed predecessor Plan; no requirement is reimplemented |
| REQ-101 | Primary §4 | 4, 6 | CORS source/profile keys and upload CORS contract | NativeHttpSecurityConfigurationTest | allowed Origin gets exact headers; denied Origin has no ACAO |
| REQ-102 | Primary §§4/11 | 1, 4 | five table DDL, Manifest, SpecPO, SINGLE/master_data routing | CatalogDdlContractTest/ProfileKeyParityTest | one new version/hash and five routing entries |
| REQ-103 | Primary §§4/9.2.1 | 2 | BrandListQuery→BrandDAO/XML/PageResultRecord | MVC/H2/XML/OAS tests | same-name union, distinct active tenant brands; no-category response unchanged |
| REQ-104 | Primary §§4/9.2.2 | 1, 3 | custom SpecDAO/XML and typed API-007 Query chain | Spec MVC/H2/XML/OAS/group tests | ordered options array or [] |
| REQ-105 | Primary §§4/9.2.3 | 4–6 | Shoubing matcher, ObjectStoragePort/S3 adapter, upload API | internal adapter tests, File MVC test, TEST-114 | URL only after acknowledged S3 put |
| REQ-106 | Primary §§4/9.2.3 | 4–6 | multipart budget, DTO/Command, ProductImageValidator, 413 mapping | validator/MVC/error tests | only JPEG/PNG <=10MB calls storage |
| REQ-107 | Primary §§4/6.2 | 4,6 | identical config key paths and operator README | parity/Binder tests | equal key structures; secrets are external |
| REQ-108 | Primary §§4/6/8 | 1–6 | existing goods Light, MP-SDJ and generator scope | generator check and final goods verify | one module and Light architecture verifier pass |

## 11. Risks, Blockers, and User Decisions

| ID | Risk or decision | Impacted Steps/files | Evidence | Owner | Status/action |
| --- | --- | --- | --- | --- | --- |
| RISK-101 | ACTOR-003 has not provisioned catalog rows | 1–3, TEST-114 | Primary §5.4/§18 | external catalog owner | External gate; lookups return [] until provisioned |
| RISK-102 | Shoubing starter, Resource, USER grant or shared Redis projections are inactive | 4,6, TEST-114 | starter EVD-019; current RedisConfig#redissonClient | Shoubing/release owner | External gate; upload remains denied until active |
| RISK-103 | S3 API DNS differs from public URL DNS or bucket GET policy | 4–6, README | primary DEC-106/§5.4 | storage operator | External gate; TEST-114 verifies public GET |
| RISK-104 | Deployed MinIO/Ceph release differs in endpoint/signing support | 4–5 | SDK/server compatibility is release-specific | storage operator | Verify endpoint/path-style/signing in TEST-114 |
| RISK-105 | PutObject commits remotely but response times out | 5–6 | object store is outside PostgreSQL transaction | release/operator | one attempt; 610109 unknown; inspect before resubmission |
| DEC-103–111 | Schema/owner, goods placement, API-001 extension, public URL, Shoubing, media format, external server/SDK and no local container | 1–6 | Accepted primary Spec and user answers “goods/other recommendations” and “1” | user/decision owner | Closed; no further user decision required |

There is no design blocker. External services/data remain release preconditions with named owners and do not change the approved implementation contract.

## 12. Review and Acceptance

### 12.1 Original requirement fidelity

The four learning goals map to CORS (REQ-101), catalog structure (REQ-102), generic Mapper custom methods (REQ-103/104), and distributed upload (REQ-105/106). The Plan implements them in the existing goods Light and excludes management CRUD, frontend code, FastDFS, separate file service, local object-store container, metadata persistence and delete/download endpoints.

### 12.2 Spec consistency

This Plan implements one accepted primary Spec plus its accepted brand predecessor. It adds no route, table, state, dependency or operational service beyond the effective design. The necessity audit found API-001 reuse, an independent Spec read resource, the required upload command and one SDK Adapter. There is no fetch-then-forward endpoint.

### 12.3 Repository executability

All production/test/config paths are verified against the current main checkout. Codegen owns only SpecPO; custom SpecDAO/XML remain agent-owned. DDL has one new version and no runtime database operation is included in Plan writing. Test/build commands run from the repository roots stated in Steps.

### 12.4 Test and release completeness

Each behavior has a focused RED/GREEN gate; final goods verify includes the architecture verifier. DDL/Manifest and profile compatibility have static tests. PostgreSQL runner execution, Shoubing/JWK/Redis state, ACTOR-003 data, and external object PUT/public GET remain TEST-114 release proof; this Plan does not claim them complete.

### 12.5 Blocking Manual Check

| Check ID | Applicability | Status | Evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- |
| MC-ARCH-001 | Applicable | PASS | current goods POM/tree, EVD-003 and primary DEC-104 | one existing Egon-COLA Light profile remains the only application | None |
| MC-REUSE-001 | Applicable | PASS | §4.7 ledger: MVC, Validation, MP-SDJ, converters, RedisConfig, ResultRecord | existing Spring/Egon facilities are reused; no duplicate generic layer | None |
| MC-DEP-001 | Applicable | PASS | primary DEC-107/110 and EVD-019 | AWS BOM/S3 2.55.6 and Shoubing starter 5.4.1 were explicitly approved; no test-only dependency added | None |
| MC-NAME-001 | Applicable | PASS | §5 inventory and Step Symbols | new POJO types use semantic suffixes; no Map carrier or URL wrapper | None |
| MC-VALID-001 | Applicable | PASS | Steps 2–6 and tests | all changed handoffs validate; reused Domain Queries use ConvertGroup; storage bytes/signature validate before Port | None |
| MC-MODEL-001 | Applicable | PASS | Step 1 generated SpecPO and Steps 2–6 typed models | mutable classes follow Lombok; BrandListQuery/SpecOptionVO are immutable value records | None |
| MC-CONVERT-001 | Applicable | PASS | Steps 2/3/5/6 BaseConverter file blocks | MapStructPlus and Egon BaseConverter/BaseForwardConverter own each layer mapping | None |
| MC-LOG-001 | Applicable | PASS | Brand/Spec/File business file blocks | logs omit content, token, original filename and full object key | None |
| MC-BEAN-001 | Applicable | PASS | goods lombok.config and Service/Repository/Adapter blocks | managed business Beans use stable name, RequiredArgsConstructor and field Qualifier | None |
| MC-UTIL-001 | Applicable | PASS | ProductImageValidator/S3 Adapter blocks | JDK ImageIO/UUID/URI plus approved SDK only | None |
| MC-JSON-001 | Applicable | PASS | common ResultRecord contracts, File DTO/VO/OAS tests | Jackson envelope retained; Shoubing 401 remains separate; grant denial is upstream | None |
| MC-TIME-001 | Applicable | PASS | ObjectStorageConfigurationProperties uses Duration | no java.util date type is added; existing timestamp contract unchanged | None |
| MC-CONFIG-001 | Applicable | PASS | four YAML profiles, ProfileKeyParityTest and Binder test | owned key paths/table maps match across profiles | None |
| MC-PATTERN-001 | Applicable | PASS | primary §13 and Step 5 ObjectStoragePort/S3 Adapter | Adapter isolates S3 variation; no provider Strategy | None |
| MC-SCOPE-001 | Applicable | PASS | §5 inventory | only goods, required tests/docs, managed DDL and generator journal change | None |
| MC-TEST-001 | Applicable | PASS | §8 matrix and per-Step exact commands | RED/GREEN, module verify and TEST-114 boundary are specified | None |
| MC-BLOCKER-001 | Applicable | PASS | §11 user decisions closed and external gates assigned | no unresolved Spec/user decision; runtime owners are release preconditions | None |

### 12.6 Final verdict

PASS — Ready for user review

This is a complete implementation Plan awaiting user review, not authorization to execute it. No production/test code, DDL or runtime service was changed or executed while writing the Plan.
