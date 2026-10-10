# 首页 K8s 网关：部署配置准备与验收计划

| Field | Value |
| --- | --- |
| Document | `2026-10-09-18-55-homepage-gateway-deployment-preparation.md` |
| Template Version | `4` |
| Status | Completed |
| Created | 2026-10-09 18:55 Asia/Shanghai |
| Updated | 2026-10-09 21:35 Asia/Shanghai |
| Owner | mario |
| Repository | enjoyshop |
| Scope | 第一阶段：发布输入契约、参数缺失/非法值离线检查及后续环境/部署验收交接；不创建 K8s 清单或静态工作负载 |
| Source Requirement | 用户“审核通过 开始写plan吧”并指定 egon-coding-writing-plan；已接受主 Spec REQ-001–008；用户选择“先写配置准备与离线验证计划，实际环境和部署待后续补齐” |
| Baseline Revision | main / 74060d5c5e36cec00cfbb814160002394f017cd7；三个已有未跟踪设计文档保留；无 deployment/k8s 或 scripts 目录 |
| Implements Spec | [首页 K8s 网关架构](../spec/2026-10-09-16-39-homepage-kubernetes-gateway-architecture.md) |
| Spec Status | Accepted |
| Spec Revision | 规范设计基线2026-10-09 16:39 Asia/Shanghai；当前审批元数据 Updated=2026-10-09 21:35 Asia/Shanghai、Accepted；技术设计未改，原执行输入hash见acceptance.md |
| Effective Specs | [主 Spec](../spec/2026-10-09-16-39-homepage-kubernetes-gateway-architecture.md) 全文；`2026-09-23-17-16-enjoyshop-scaffold-brand-crud.md` §§5.4、6.1；`2026-09-28-21-50-distributed-file-catalog-lookups.md` §§9、16，依赖文档与局部有效范围的直接链接见 §2.2 |
| Depends On Plans | None |
| Supersedes | None |
| Superseded By | None |
| Related Plans | None |

## 1. Summary

根据用户新答复，计划限定为**配置准备与离线验证**，共有2个顺序Step：先定义发布输入与拒绝行为，再归档离线检查和验收交接。交付release-inputs.schema.json、不可部署的填写示例、README、check.sh及acceptance.md；只规定下一阶段需要提供哪些真实环境资料，不要求现在存在集群或首页镜像。

本Plan当前状态Completed。用户随后明确要求执行，两个Step已按本阶段范围全部实现、验证并交付于d4e392a；完整首页架构的实际环境、资源和运行验收仍属于后续。不存在本阶段待解决的主要设计选择；缺少集群版本/证书/平台地址/静态产物仅阻止下一阶段生成实际清单及部署，不阻塞本阶段离线准备。

各Step独立验证，但按用户AGENTS每逻辑任务最多一次commit：只在最终Step提交一次准备任务，不每Stepcommit。未批准控制器安装、集群访问、真实资源生成、服务/浏览器启动、故障演练或部署。

## 2. Target Spec and Effective Design

### 2.1 Primary target

- Path: [首页架构 Spec](../spec/2026-10-09-16-39-homepage-kubernetes-gateway-architecture.md)。
- Status: 主Spec已同步Accepted审批元数据；用户先批准设计，再指定执行技能并要求“开始工作”。本Plan两个Step已交付，当前Completed；没有扩大为实际部署。
- Revision: 正文对应2026-10-09 16:39 Asia/Shanghai、baseline 74060d5；本轮只加关系链接。
- Approval evidence: 当前用户消息明确批准上一轮交付的具体Spec并请求Plan；不扩大为代码/部署授权。

### 2.2 Effective Spec set

| Role | Spec/link | Status/revision | Effective sections | Why included |
| --- | --- | --- | --- | --- |
| Primary | [首页架构](../spec/2026-10-09-16-39-homepage-kubernetes-gateway-architecture.md) | Accepted（审批元数据已同步）；技术设计未改 | 全文，REQ-001–008；§8未来清单、§14 TEST-001–011、§15/16门禁 | 本阶段准备任务及后续部署的唯一主目标 |
| Dependency | [工程骨架/品牌](../spec/2026-09-23-17-16-enjoyshop-scaffold-brand-crud.md) | Accepted / 2026-09-24 15:20 CST | §§5.4、6.1；仅Light、不自建enjoyshop网关、已有能力边界 | 已完成Java架构约束，不重新执行品牌实现 |
| Dependency | [目录/文件上传](../spec/2026-09-28-21-50-distributed-file-catalog-lookups.md) | Accepted / 2026-09-29 11:11 CST | §§9、16；仅保持上传/图片URL/原响应/迁移兼容 | 已有合同为入口透传回归依据，不重建catalog或SDK |

元数据Effective Specs按完整文件名+局部章节声明；链接在本表，避免将依赖文档的整个历史REQ集合当作本次待实现工作。校验器对链接文件整章REQ提取与局部Depends On不同，手工有效设计审查必须以本表范围为准；不能为了“覆盖历史ID”重新实施无关CRUD。无有效的amendment/supersession新增；商品管理Review Spec仅上下文。

### 2.3 Superseded or excluded content

不替代旧业务设计。主Spec REQ-008中“本轮只Spec、无Plan”是前一阶段交付限制，主Spec§20.6明确允许下一轮请求Plan；本轮用户已请求，阶段进入Plan文档，不把该历史限制误用为禁止规划。广告、页面开发、匿名绕行、Token迁移、平台源码、DB schema、控制器安装全部排除。


本轮用户已明确选择第一阶段离线准备。该阶段是主Spec§8/14/15/16的有限子集，其他清单和运行义务保持有效并在§10逐项标为后续，不重写Spec的规范性目标，也不声称本Plan实施即可实现完整高可用。
## 3. Effective Requirements and Acceptance

| Requirement | Source Spec section | Effective statement | Observable acceptance | Implementation impact |
| --- | --- | --- | --- | --- |
| REQ-001 | 主Spec§4/7.3.1 | 静态与API分流 | 本阶段将准确路径和禁止fallback规则写入交接清单；实际请求证明保留Pending | README/acceptance中的后续清单合同，不生成routes/nginx |
| REQ-002 | §4/8/15.1 | Envoy实现Gateway API入口 | 所有版本/归属字段必填、拒latest；真实CRD验证属于下一阶段 | schema/inputs检查；不猜扩展字段 |
| REQ-003 | §4/7.3.1/16 | 保留身份/TLS/IP信任职责 | 输入仅Secret引用，未知字段/秘密值拒绝；交接保留既有安全验收 | offline checker与原合同说明，不开放新API |
| REQ-004 | §4/13/15.3 | 复用Java两级缓存 | 本阶段无Lua/业务cache文件；文档准确指出TTL/回源局限 | 仅scope/doc检查，原组件不变 |
| REQ-005 | §4/7.3.3/15 | 入口local与Biz共享配额分工 | 配额/策略引用/失败模式字段明确，严格策略只接受DENY | schema/cross-field；全局配额运行验收后续 |
| REQ-006 | §4/15.4/16 | 多副本/退出/依赖失败可验收 | 副本边界、timeout关系有离线规则；运行TEST保持Pending | input checks与验收模板，不声称已有HA |
| REQ-007 | §4/9/15.2/16 | 保留业务/S3和发布回滚兼容 | 原业务路径保持，镜像digest/资源保留输入必填；旧hash回滚记录模板明确 | schema/doc；不构建或拉取镜像 |
| REQ-008 | §4/20.6+最新用户消息 | 阶段隔离与可审核交付 | 本轮只Plan/Spec关系；后续仅离线准备文件；无部署动作 | 文档strict、scope及单次commit门禁 |

REQ-001/003/004/006/007涉及的真实运行结果不由输入有效性替代。配置准备只交付其合同与验收方法，下一阶段必须创建实际清单和完成真实验收；§10明确剩余工作，不使用“全部Spec已实施”。

## 4. Implementation Strategy and Dependency Order

### 4.1 Ordered strategy

不把未知环境变量填成具体生产值。先写focused输入校验的RED测试与checker契约，再写schema和拒绝型示例，再记录字段owner与后续数据来源。最后把scratch移入deployment/k8s/check.sh并补运行验收清单；同一checker迁移前后结果相同。

### 4.2 Test-first strategy

Step1使用本机已验证可用的python3标准库json/unittest/tempfile/pathlib/urllib.parse，无新依赖。先固定validate_release_inputs(schema, inputs)的输入/错误义务；缺schema是预期RED。schema出现后，缺值/错误类型/越界/secret/跨字段不一致等反例必须持续被拒，合成的结构完整正例仅能证明离线规则、明确不作为部署输入。示例null故意非法，不能修成假的生产值获得GREEN。

JSON Schema是声明性输出；check.sh只验证本Plan明确列出的发布输入约束，不宣称实现完整JSON Schema、验证真实CRD/证书/镜像/身份或生成YAML。checker的输入模式只能输出通过/拒绝与安全字段路径，不能生成发布资源、默认补值或调用kubectl/nginx。

### 4.3 Sequential and parallel boundaries

| Step | Depends on | May run in parallel with | Must not overlap with | Reason |
| --- | --- | --- | --- | --- |
| Step 1 | 已接受Spec及本阶段明确范围 | None | schema/example/初始README/scratch | proof先于输入实现，稳定合同 |
| Step 2 | Step1 focused GREEN | None | scratch迁移、README收尾、acceptance | 单一checker归档，完整交接 |

不要求子agent或并行agent；无集群/前端产物前置。

### 4.4 Commit boundaries

Step1只记录GREEN证据不commit；Step2结束后只提交一次，建议 `chore(deployment): prepare offline homepage release checks`。最终白名单只有deployment/k8s下的本Plan5个文件，不提交scripts/work或现有Spec/Plan/商品管理文档；不git add .、不force-add被忽略scratch。

### 4.5 Spec Simplicity and Implementation-necessity Audit

| Spec element | Spec necessity verdict/section | Current repository evidence | Direct/reuse alternative | Interaction/implementation cost | Plan decision |
| --- | --- | --- | --- | --- | --- |
| 发布输入schema/example/README | 主Spec§8/15已Add | 当前无deployment或输入合同 | 无现有等价输入校验命令 | 3个准备文件，不增加运行请求/状态 | 本阶段Implement |
| Offline checker | 主Spec§14质量职责；技能rule22 | python3已有；无现成部署检查suite | git diff不能检验参数语义；不安装jsonschema/PyYAML | scratch最终移为单个check.sh，无network | PLAN-CLAR-001，Implement |
| 验收交接 | 主Spec§8/14/16 | 当前无集群与静态产物 | 按用户决定保留后续验收，不虚构报告 | 1个acceptance.md | 本阶段Implement |
| gateway/routes/policies/static/Grant/HA/NetworkPolicy | 主Spec§8已Add但有真实输入门禁 | 对应目录不存在、真实资源未证实 | 等版本/schema、镜像和平台合同后另写Plan | 本阶段零resource/renderer/tool安装 | 明确Deferred，不纳入当前Step |
| Java cache/BFF/limiter | 主SpecKeep，§6/7.0/13 | goods POM、平台源码已有 | 直接复用，当前不改Java | 零新增业务依赖/模型/接口 | Keep |

不存在fetch-then-forward、客户端租户预查询、第二网关/cache/Outbox或新Java层。

### 4.6 Change-unit Dependency Matrix

| Change unit | Requirements | Proof/RED point | Compile/runtime prerequisites | Produces | Consumers/unblocks | Owning Step |
| --- | --- | --- | --- | --- | --- | --- |
| 发布输入校验 | REQ-002/003/005/006/007/008 | missing schema、缺值/非法secret/types | 仅python3，无cluster | schema/example/自检/scratch/README | owner准备真实输入、Step2 | Step 1 |
| 检查归档/交接 | REQ-001–008 | 保持已GREEN规则；路径迁移重复验证 | Step1，无环境输入依赖 | check.sh/acceptance/README收尾 | 下一阶段环境准备，不直接部署 | Step 2 |

### 4.7 Java, Spring, and Egon-COLA Implementation Standards

N/A（Java变更）—当前业务是native/non-open Light；POM、Java类、安全链、Spring profiles、DDL manifest全部不变，部署准备目录不是新Java层。

| Concern | Current repository evidence | Effective Spec decision | Planned implementation consequence | Owning Steps/checks |
| --- | --- | --- | --- | --- |
| Architecture profile | goods POM与LightPersistenceArchitectureTest | 主Spec§6.1 | 只deployment准备文件，不建模块 | Every Step；MC-ARCH-001 |
| Reuse/capability | Egon cache、平台Edge/Biz/limiter；python3标准库 | §6/7.0/13 | 零Java依赖；checker仅验证输入 | Every Step；MC-REUSE-001/MC-DEP-001 |
| Model/validation/conversion | §5无Java类型/handoff | §9–11保留 | 不建DTO/Converter/Validator | Every Step；对应MC=N/A |
| Bean/JSON/time/config | 无Bean/Spring/SQL修改 | §15输入独立 | 部署输入core一致；原Java不变 | Every Step；MC-CONFIG-001 |
| Pattern | 本阶段无复杂Java业务 | §13复用现有模式 | 不制造策略类或模板框架 | Every Step；MC-PATTERN-001 |

#### Capability reuse ledger

| Need | Candidates inspected | Exact evidence | Fit/gap | Decision | Added dependency/custom code | Owning Step/check |
| --- | --- | --- | --- | --- | --- | --- |
| JSON读取/断言 | Python json/unittest/urllib.parse | 本会话python3 preflight已执行；未安装jsonschema | 足以验证明确的输入规则，不是完整schema引擎 | 复用标准库 | 一个专属offline checker，无安装 | Step1/2；MC-REUSE-001 |
| YAML/CRD/渲染 | Ruby safe_load可用；kubectl1.34.1；Helm缺失 | 前次只读探测 | 本阶段不处理YAML，不需要这些工具 | 后续确定真实schema/渲染机制 | 当前无依赖/调用 | Deferred |
| 业务基础设施 | common cache starter、Shoubing/Edge/Biz | 主SpecEVD-003–008/当前POM | 保持；无本阶段能力缺口 | 不改代码 | None | Every Step；MC-DEP-001 |

### 4.8 User-mandated Java Rule Implementation Matrix

| Literal rule | Spec source | Repository evidence | Exact files and order | Pseudocode obligations | Validation gate | Steps | Status/blocker |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Rule 1 | 主Spec§6.2/8 | §5无Java/POM/profile/SQL目标 | 准备文件Step1→2，不进入Java层 | 无新增适用Java对象；原规范保持，不添加Java包装满足部署准备 | scope/hashes及focused input tests | Every Step | N/A |
| Rule 2 | 主Spec§6.2/8 | §5无Java/POM/profile/SQL目标 | 准备文件Step1→2，不进入Java层 | 无新增适用Java对象；原规范保持，不添加Java包装满足部署准备 | scope/hashes及focused input tests | Every Step | N/A |
| Rule 3 | 主Spec§6.2/8 | §5无Java/POM/profile/SQL目标 | 准备文件Step1→2，不进入Java层 | 无新增适用Java对象；原规范保持，不添加Java包装满足部署准备 | scope/hashes及focused input tests | Every Step | N/A |
| Rule 4 | 主Spec§6.2/8 | §5无Java/POM/profile/SQL目标 | 准备文件Step1→2，不进入Java层 | 无新增适用Java对象；原规范保持，不添加Java包装满足部署准备 | scope/hashes及focused input tests | Every Step | N/A |
| Rule 5 | 主Spec§6.2/8 | §5无Java/POM/profile/SQL目标 | 准备文件Step1→2，不进入Java层 | 无新增适用Java对象；原规范保持，不添加Java包装满足部署准备 | scope/hashes及focused input tests | Every Step | N/A |
| Rule 6 | 主Spec§6.2/8 | §5无Java/POM/profile/SQL目标 | 准备文件Step1→2，不进入Java层 | 无新增适用Java对象；原规范保持，不添加Java包装满足部署准备 | scope/hashes及focused input tests | Every Step | N/A |
| Rule 7 | 主Spec§6.2/8 | §5无Java/POM/profile/SQL目标 | 准备文件Step1→2，不进入Java层 | 无新增适用Java对象；原规范保持，不添加Java包装满足部署准备 | scope/hashes及focused input tests | Every Step | N/A |
| Rule 9 | 主Spec§6.2/8 | §5无Java/POM/profile/SQL目标 | 准备文件Step1→2，不进入Java层 | 无新增适用Java对象；原规范保持，不添加Java包装满足部署准备 | scope/hashes及focused input tests | Every Step | N/A |
| Rule 10 | 主Spec§6.2/8 | §5无Java/POM/profile/SQL目标 | 准备文件Step1→2，不进入Java层 | 无新增适用Java对象；原规范保持，不添加Java包装满足部署准备 | scope/hashes及focused input tests | Every Step | N/A |
| Rule 11 | 主Spec§6.2/8 | §5无Java/POM/profile/SQL目标 | 准备文件Step1→2，不进入Java层 | 保持Light工程和组件边界，不生成模块/DDL | scope/hashes及focused input tests | Every Step | PASS |

部署输入core集合一致另受MC-CONFIG-001强制，不借Java N/A豁免。Java/CQE的模型、验证、MP/唯一键/DDL、Event投递均无新变更；原Light职责维持。

## 5. Change File Tree

```text
deployment/k8s/
├── release-inputs.schema.json  CREATE Step1
├── release-inputs.example.json CREATE Step1，不可部署
├── README.md                   CREATE Step1，MODIFY Step2同一owner
├── check.sh                    RENAME Step2，从scratch归档
└── acceptance.md               CREATE Step2，运行证据Pending
scripts/work/2026-10-09-18-55-homepage-gateway-deployment-preparation-verify.sh
                               CREATE Step1，RENAME Step2，最终不存在
```

| Operation | Path | Current evidence/symbol | Final symbols/state | Responsibility | Step | Requirements | Validation owner |
| --- | --- | --- | --- | --- | --- | --- | --- |
| CREATE→RENAME | scripts/work/2026-10-09-18-55-homepage-gateway-deployment-preparation-verify.sh | 本仓无scripts/deployment检查，技能rule22 | 最终源不存在 | 先RED再GREEN，后归档，不暂存scratch | Step1→2 | REQ-002/003/005/006/007/008 | selftest/lifecycle |
| CREATE | deployment/k8s/release-inputs.schema.json | 主Spec§8/15批准 | JSON Schema声明，core字段类型/必填/范围 | 输入契约，无环境实例值 | Step1 | REQ-002/003/005/006/007 | selftest |
| CREATE | deployment/k8s/release-inputs.example.json | 主Spec§8；无真实环境值 | 全core字段null，故意拒绝 | 待填写示例，不可成为生产默认 | Step1 | REQ-002/007/008 | inputs拒绝 |
| CREATE→MODIFY | deployment/k8s/README.md | 主Spec§8/15/16 | 输入owner、offline命令、阶段边界 | 同文件Step1合同、Step2最终命令/交接 | Step1→2 | REQ-001–008 | 文档/scope |
| RENAME | deployment/k8s/check.sh | 技能rule22及主Spec§14 | selftest/inputs两种模式，无network/render | 持久化本任务离线checker | Step2 | REQ-002/003/005/006/007/008 | 同一suite回归 |
| CREATE | deployment/k8s/acceptance.md | 主Spec§8/14 TEST-001–011 | 每TEST owner/证据/状态分列 | 未来真实环境/清单/runtime交接 | Step2 | REQ-001–008 | 完整性检查 |

只上述准备文件进入未来实施范围；base/、platform-contract/、K8s API对象、前端产物和实际环境输入文件不在当前文件树。

## 6. Prerequisites, Constraints, and Plan Clarifications

### 6.1 Repository and worktree baseline

用户AGENTS适用；main/74060d5，已有商品Spec/Plan与首页Spec保留。当前只改本Plan；主SpecRelated Plans已有正确链接不再改。未来仅本Plan文件白名单，禁止git add .、git clean或恢复整个工作树。现有所有classpath SQL/manifest不可修改。

### 6.2 Build, test, and environment prerequisites

| Concern | Exact command/source | Required state | Validation boundary |
| --- | --- | --- | --- |
| 工具 | python3 --version；本会话资源preflight成功 | Python3标准库可用，无第三方包 | 不下载、不访问网络 |
| 文档检查 | 两技能validate_plan.py/validate_spec.py --strict | 现有技能资源完整 | 当前可执行；不证明代码或部署 |
| 未来checker | bash脚本+embedded Python标准库 | 实施批准后创建 | 离线selftest无需环境数据 |
| 实际输入 | 可选由owner提供私密JSON绝对路径 | selftest不需要；inputs只校验提供文件 | 不读取默认配置/kubeconfig，结果非部署许可 |

### 6.3 Immutable constraints and approved decisions

保持已选Envoy/Yuheng/Nginx/Java cache架构，但本阶段不实例化资源。无controller安装、kubectl调用、Nginx启动、镜像构建/拉取、DB/Redis访问、浏览器或fault injection。业务API/认证/租户/上传/图URL/Java/Spring配置完全不变。

### 6.4 Plan Clarifications

| ID | Small implementation inference | Repository evidence | Why semantics are unchanged | Impact if wrong |
| --- | --- | --- | --- | --- |
| PLAN-CLAR-001 | scratch先做RED/GREEN，最终移deployment/k8s/check.sh | 技能rule22；主Spec§14需要离线gate而仓库无现成检查 | 只验证批准输入，不渲染/部署或更改业务 | 路径可调整，无架构影响 |
| PLAN-CLAR-002 | 示例core字段保留null，明确不可通过inputs校验 | 未有实际参数，主Spec禁止猜填或示例发布 | 只表达待填写状态，真实非null合同不变 | 修改示例呈现，不改生产合同 |
| PLAN-CLAR-003 | 审核值中resourceRequests/limits展开为resourceRequests、resourceLimits | 主Spec§15.1明确同时要求requests与limits | local JSON表示，仍同资源合同 | 字段名协调，无业务API变化 |
| PLAN-CLAR-004 | 输入timeouts采用秒为单位正整数；requestTimeouts对象固定inFlightSeconds最大在途预算 | 主Spec§15.1要求grace>drain+在途余量；§15.4已有terminationGracePeriodSeconds语义 | 仅离线输入数值表示，未来CRD转换须实际版本Plan定义；不改现有服务timeout | 后续接入如字段不适配，显式修订准备合同，不猜写CRD |

用户选择阶段范围是明确指令，不是Plan自行缩减整个Spec；完整部署剩余义务见§10/11。

## 7. Ordered File-by-file Implementation Steps

本阶段冻结core字段名（39项）：`clusterVersion`, `gatewayApiVersion`, `envoyGatewayVersion`, `envoyProxyImageDigest`, `appNamespace`, `platformNamespace`, `envoyNamespace`, `gatewayClassName`, `publicHostname`, `publicOrigin`, `issuerUri`, `oidcCallbackUri`, `tlsSecretName`, `edgeCaReference`, `edgeTlsServerName`, `edgeServiceName`, `edgeServicePort`, `staticServicePort`, `trustedProxyChain`, `clientIpMode`, `staticImageDigest`, `staticArtifactVersion`, `assetRetentionPolicy`, `envoyReplicas`, `staticMinReplicas`, `staticMaxReplicas`, `resourceRequests`, `resourceLimits`, `ingressLocalLimit`, `burst`, `bizPolicyRefs`, `quotaFailureMode`, `readyProbeContract`, `drainTimeout`, `terminationGracePeriod`, `requestTimeouts`, `redisHaEvidence`, `dbCapacityEvidence`, `platformAdmissionEvidence`。所有core字段在真实inputs模式必填非null；dev/test/prod使用同一schema。示例不代表某环境配置。

### Step 1 — 定义输入契约并验证缺值与非法值拒绝

- Requirements: REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008（本阶段准备责任，运行剩余见§10）。
- Dependencies: None；本阶段范围已由用户明确选择。
- Baseline state: 只有现有Spec/Plan/业务代码，无deployment/proof文件。
- Observable outcome: 准备输入和拒绝规则可离线验证，无默认环境值。
- End state: 本Step4个准备文件可用；未有实际清单/镜像/部署输出。
- Test-first gate: Required — File1测试先于schema；首轮因SCHEMA_MISSING RED，schema后GREEN且非法fixture继续拒绝。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-CONFIG-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001, MC-BLOCKER-001
- Literal Rules: Rule 11 — 保持Light及外部平台职责，不新增Java层；其他Java规则无新适用对象。
- Ordered files:

#### File 1 — `CREATE scripts/work/2026-10-09-18-55-homepage-gateway-deployment-preparation-verify.sh`

- Purpose: 在schema之前固定RED测试和checker的副作用边界。
- Symbols: selftest; inputs; validate_release_inputs; check_scope。
- Repository evidence: 主Spec§8/14/15批准准备合同及验证职责；本仓无对应准备文件；scratch归档受技能rule22/PLAN-CLAR-001约束；README同owner按两个阶段更新。
- Dependencies and consumers: 前序文件产出契约/证明；本阶段checker与README/后续环境owner消费；无Javacaller/callee/生成源；不被集群或业务服务运行加载。
- Why now: 先proof再schema/示例/说明；后续移动稳定checker再写交接；本文件输出为下一文件必需输入。
- Contract/signature changes: 仅上述selftest; inputs; validate_release_inputs; check_scope，具体算法/字段见伪代码；无业务接口/DTO/SQL/Springprofile变动。
- Input/output and state mapping: CLI mode+schema/input JSON→安全字段错误或成功；无YAML/网络/部署输出。
- Error and edge behavior: 工具/文件缺失明确拒绝；未知模式usage非零；null、secret、unknown字段拒绝；正例只标synthetic；格式/路径/类型错误明确非零；不日志secret，不写业务数据；不读隐式运行环境。
- Standards impact: MC-ARCH-001/MC-REUSE-001/MC-CONFIG-001/MC-SCOPE-001/MC-TEST-001；全部core字段的结构/来源/单位一致必须证明；无Java命名/DI/JSON栈/time/Converter改动。
- Literal rule enforcement: Rule 11 — 文件仅位于准备/proof目录，不新建Java层、业务网关或DDL；既有MP/CQE与Light不变。Java其他规则不因本文件产生适用对象，不创建Java包装器。
- Implementation pseudocode:

```text
shell: set -eu; accept only selftest or inputs; discover repo root; no kubectl/nginx/docker/network calls
embedded Python unittest defines test_missing_schema, test_missing_required, test_null, test_unknown_field, test_wrong_type
also test_versions_latest, test_invalid_port, test_replica_bounds, test_bad_digest, test_inline_secret, test_origin_mismatch
also test_strict_fallback, test_grace_not_covering_drain_and_inflight, test_example_cannot_be_release
validate_release_inputs(schema, inputs) checks exactly the named release constraints; do not implement generic JSON Schema engine
first run selftest before schema file exists: expected RED SCHEMA_MISSING only, no third-party import failure
create temporary complete synthetic fixture, then mutate one constraint at a time; log field path only, never value
invalid tests assert rejection with field-specific reason; valid fixture asserts no generated resource or external call
inputs mode opens only explicitly supplied path; missing or unreadable path nonzero; do not fall back to example or env config
```

- Verification contribution: selftest/inputs/example-negative与scope文档检查共同覆盖该文件；schema/README/check一致，runtime Pending不能被改为PASS。
- After this file: 输出可供下个准备文件消费；若focused仍RED，只修当前最小差异，不能通过猜填环境或跳过拒绝测试继续；最后文件后执行完整Step gate。

#### File 2 — `CREATE deployment/k8s/release-inputs.schema.json`

- Purpose: 声明core字段及可离线证明的安全/范围约束。
- Symbols: properties; required; additionalProperties。
- Repository evidence: 主Spec§8/14/15批准准备合同及验证职责；本仓无对应准备文件；scratch归档受技能rule22/PLAN-CLAR-001约束；README同owner按两个阶段更新。
- Dependencies and consumers: 前序文件产出契约/证明；本阶段checker与README/后续环境owner消费；无Javacaller/callee/生成源；不被集群或业务服务运行加载。
- Why now: 先proof再schema/示例/说明；后续移动稳定checker再写交接；本文件输出为下一文件必需输入。
- Contract/signature changes: 仅上述properties; required; additionalProperties，具体算法/字段见伪代码；无业务接口/DTO/SQL/Springprofile变动。
- Input/output and state mapping: core字段从Spec§15.1→schema；真实值由后续owner填写；structured记录保留对象不猜内部CRD字段。
- Error and edge behavior: 非null、unknown core字段、错type拒绝；core对象存在不代表其运行内容验收通过；格式/路径/类型错误明确非零；不日志secret，不写业务数据；不读隐式运行环境。
- Standards impact: MC-ARCH-001/MC-REUSE-001/MC-CONFIG-001/MC-SCOPE-001/MC-TEST-001；全部core字段的结构/来源/单位一致必须证明；无Java命名/DI/JSON栈/time/Converter改动。
- Literal rule enforcement: Rule 11 — 文件仅位于准备/proof目录，不新建Java层、业务网关或DDL；既有MP/CQE与Light不变。Java其他规则不因本文件产生适用对象，不创建Java包装器。
- Implementation pseudocode:

```text
JSON Schema draft 2020-12: top object, required = complete core set, additionalProperties=false
version/namespace/hostname/URI/Secret reference/policy mode fields: nonempty strings; versions reject latest
edgeServicePort/staticServicePort integers1..65535; envoyReplicas/staticMinReplicas>=2; staticMaxReplicas>=2
staticImageDigest/envoyProxyImageDigest: immutable image reference ending @sha256:64hex, never plaintext credentials
ingressLocalLimit positive integer; burst nonnegative integer; bizPolicyRefs array of nonempty policy references
quotaFailureMode constant DENY for this strict quota contract; clientIpMode nonempty string, no guessed hop default
resourceRequests/resourceLimits/readyProbeContract/assetRetentionPolicy nonempty objects: precise environment internals deferred
trustedProxyChain nonempty array; redisHaEvidence/dbCapacityEvidence/platformAdmissionEvidence nonempty string evidence references
drainTimeout/terminationGracePeriod positive integer seconds; requestTimeouts object requires positive inFlightSeconds
checker verifies max>=min, grace>drain+inflight, publicOrigin HTTPS host matches publicHostname, callback uses same origin
walk all JSON keys and reject password/token/privateKey/clientSecret/secretKey/accessKey literal value fields
schema validity and checker agreement are tested; documented environment records remain unverified by this gate
```

- Verification contribution: selftest/inputs/example-negative与scope文档检查共同覆盖该文件；schema/README/check一致，runtime Pending不能被改为PASS。
- After this file: 输出可供下个准备文件消费；若focused仍RED，只修当前最小差异，不能通过猜填环境或跳过拒绝测试继续；最后文件后执行完整Step gate。

#### File 3 — `CREATE deployment/k8s/release-inputs.example.json`

- Purpose: 提供待填写且不可部署的输入表。
- Symbols: core field skeleton。
- Repository evidence: 主Spec§8/14/15批准准备合同及验证职责；本仓无对应准备文件；scratch归档受技能rule22/PLAN-CLAR-001约束；README同owner按两个阶段更新。
- Dependencies and consumers: 前序文件产出契约/证明；本阶段checker与README/后续环境owner消费；无Javacaller/callee/生成源；不被集群或业务服务运行加载。
- Why now: 先proof再schema/示例/说明；后续移动稳定checker再写交接；本文件输出为下一文件必需输入。
- Contract/signature changes: 仅上述core field skeleton，具体算法/字段见伪代码；无业务接口/DTO/SQL/Springprofile变动。
- Input/output and state mapping: 39个core键均null；不增加owner/explanation schema外字段，说明放README。
- Error and edge behavior: inputs(example)必须拒绝；未提供实际值不得回填example域名/证书/IP/镜像；格式/路径/类型错误明确非零；不日志secret，不写业务数据；不读隐式运行环境。
- Standards impact: MC-ARCH-001/MC-REUSE-001/MC-CONFIG-001/MC-SCOPE-001/MC-TEST-001；全部core字段的结构/来源/单位一致必须证明；无Java命名/DI/JSON栈/time/Converter改动。
- Literal rule enforcement: Rule 11 — 文件仅位于准备/proof目录，不新建Java层、业务网关或DDL；既有MP/CQE与Light不变。Java其他规则不因本文件产生适用对象，不创建Java包装器。
- Implementation pseudocode:

```text
read schema.properties names and produce identical top-level core key set in deterministic order
assign null to every unknown field; no public.example value pretends to be actual environment
write README explanation that this file is intentionally rejected, not valid dev/test/prod configuration
selftest asserts example keys equal core schema keys and example fails required/non-null validation
inputs mode given example must exit nonzero and create no rendered files, publish bundle or network state
```

- Verification contribution: selftest/inputs/example-negative与scope文档检查共同覆盖该文件；schema/README/check一致，runtime Pending不能被改为PASS。
- After this file: 输出可供下个准备文件消费；若focused仍RED，只修当前最小差异，不能通过猜填环境或跳过拒绝测试继续；最后文件后执行完整Step gate。

#### File 4 — `CREATE deployment/k8s/README.md`

- Purpose: 文档先解释离线checker何时可以/不可以证明结果。
- Symbols: phase1 boundary; input owner ledger。
- Repository evidence: 主Spec§8/14/15批准准备合同及验证职责；本仓无对应准备文件；scratch归档受技能rule22/PLAN-CLAR-001约束；README同owner按两个阶段更新。
- Dependencies and consumers: 前序文件产出契约/证明；本阶段checker与README/后续环境owner消费；无Javacaller/callee/生成源；不被集群或业务服务运行加载。
- Why now: 先proof再schema/示例/说明；后续移动稳定checker再写交接；本文件输出为下一文件必需输入。
- Contract/signature changes: 仅上述phase1 boundary; input owner ledger，具体算法/字段见伪代码；无业务接口/DTO/SQL/Springprofile变动。
- Input/output and state mapping: 输入field→owner/格式/来源/后续真实证据；当前工具只是准备。
- Error and edge behavior: 不能把合成fixture或inputs成功称上线批准；无自动profile填充或部署操作；格式/路径/类型错误明确非零；不日志secret，不写业务数据；不读隐式运行环境。
- Standards impact: MC-ARCH-001/MC-REUSE-001/MC-CONFIG-001/MC-SCOPE-001/MC-TEST-001；全部core字段的结构/来源/单位一致必须证明；无Java命名/DI/JSON栈/time/Converter改动。
- Literal rule enforcement: Rule 11 — 文件仅位于准备/proof目录，不新建Java层、业务网关或DDL；既有MP/CQE与Light不变。Java其他规则不因本文件产生适用对象，不创建Java包装器。
- Implementation pseudocode:

```text
state phase1 only schema/example/checker/acceptance, with actual K8s resources deferred
map every core field to owner, declared type/unit and required future evidence; no secrets or cluster addresses filled
explain object records are structurally checked only, not official CRD/Ready/drain/HA validation
document scratch selftest/inputs command until Step2 promotes checker; synthetic fixtures never accepted as release evidence
preserve static/API split, BFF/OIDC paths, Java cache TTL and local/global quota boundaries from accepted Spec
list service/image/version/TLS/IP/UID/old hash retention and official schema/render choices as next-phase inputs
```

- Verification contribution: selftest/inputs/example-negative与scope文档检查共同覆盖该文件；schema/README/check一致，runtime Pending不能被改为PASS。
- After this file: 输出可供下个准备文件消费；若focused仍RED，只修当前最小差异，不能通过猜填环境或跳过拒绝测试继续；最后文件后执行完整Step gate。

- Validation working directory: /Users/mario/SelfProject/enjoyshop；自检tmp fixture自清理，不读取用户secret或集群配置。
- Verification command: `bash scripts/work/2026-10-09-18-55-homepage-gateway-deployment-preparation-verify.sh selftest`（期待0）；`bash scripts/work/2026-10-09-18-55-homepage-gateway-deployment-preparation-verify.sh inputs deployment/k8s/release-inputs.example.json`（期待非零且错误为null/missing，随后确认没有输出资源）；`git diff --check`；所有CREATE文件分别 `git diff --no-index --check /dev/null 文件绝对路径`。
- Expected result: selftest所有命名positive/negative case通过；example明确拒绝；schema/example/README core键一致；原业务文件hash不变；scratch只存在本任务工作位置。
- Failure returns to: fixture/helper错误回File1；schema/README不同回对应文件；未知业务/CRD要求不能在本阶段加实现，记录到下一阶段设计；缺真实环境不是本阶段failure。
- Completion criteria: 本Step可观察准备结果与全部离线断言有真实命令证据；副作用范围为准备文件；运行验收结果保持未执行。
- Rollback: 只撤销本Step本任务新创建/修改且未被别人改动的准备文件；不git clean/restore整仓、不改原文档/数据库。未提交scratch仅清自己临时文件。
- Commit paths: `scripts/work/2026-10-09-18-55-homepage-gateway-deployment-preparation-verify.sh`, `deployment/k8s/release-inputs.schema.json`, `deployment/k8s/release-inputs.example.json`, `deployment/k8s/README.md`；scratch路径仅工作清单，禁止暂存；最终唯一提交白名单为全部5个deployment/k8s准备文件。
- Commit: 不提交，记录Step1 GREEN后进入Step2（用户AGENTS每逻辑任务最多1个commit）。

### Step 2 — 归档离线检查并交接后续部署验收

- Requirements: REQ-001, REQ-002, REQ-003, REQ-004, REQ-005, REQ-006, REQ-007, REQ-008（本阶段准备责任，运行剩余见§10）。
- Dependencies: Step1 selftest GREEN及example拒绝已证明；不依赖真实集群/镜像。
- Baseline state: schema/example/初始README和scratch已通过输入suite，runtime尚无证据。
- Observable outcome: 单一持久化checker与逐项验收交接，scratch生命周期结束。
- End state: 仅最终5个准备文件；scratch源不存在；未来真实部署义务保持Pending。
- Test-first gate: Not applicable — 只移动已GREEN checker并整理验收文档，不引入新的配置行为；重复suite防路径回归。
- Manual Checks: MC-ARCH-001, MC-REUSE-001, MC-DEP-001, MC-CONFIG-001, MC-PATTERN-001, MC-SCOPE-001, MC-TEST-001, MC-BLOCKER-001
- Literal Rules: Rule 11 — 保持Light及外部平台职责，不新增Java层；其他Java规则无新适用对象。
- Ordered files:

#### File 1 — `RENAME deployment/k8s/check.sh`

- Purpose: 将已GREEN专属checker归档到子系统。
- Symbols: same selftest/inputs suite; repo path resolution。
- Repository evidence: 主Spec§8/14/15批准准备合同及验证职责；本仓无对应准备文件；scratch归档受技能rule22/PLAN-CLAR-001约束；README同owner按两个阶段更新。
- Dependencies and consumers: 前序文件产出契约/证明；本阶段checker与README/后续环境owner消费；无Javacaller/callee/生成源；RENAME源=scripts/work/2026-10-09-18-55-homepage-gateway-deployment-preparation-verify.sh。
- Why now: 先proof再schema/示例/说明；后续移动稳定checker再写交接；本文件输出为下一文件必需输入。
- Contract/signature changes: 仅上述same selftest/inputs suite; repo path resolution，具体算法/字段见伪代码；无业务接口/DTO/SQL/Springprofile变动。
- Input/output and state mapping: 源scratch→子系统入口；保持field错误和无副作用输出；路径相对repo而非scratch目录。
- Error and edge behavior: 迁移后缺schema是路径回归错误；无重复残留工具；输入文件不可读仍拒绝；格式/路径/类型错误明确非零；不日志secret，不写业务数据；不读隐式运行环境。
- Standards impact: MC-ARCH-001/MC-REUSE-001/MC-CONFIG-001/MC-SCOPE-001/MC-TEST-001；全部core字段的结构/来源/单位一致必须证明；无Java命名/DI/JSON栈/time/Converter改动。
- Literal rule enforcement: Rule 11 — 文件仅位于准备/proof目录，不新建Java层、业务网关或DDL；既有MP/CQE与Light不变。Java其他规则不因本文件产生适用对象，不创建Java包装器。
- Implementation pseudocode:

```text
move scripts/work plan-id-verify.sh to deployment/k8s/check.sh, not scripts top level
update repository root/path lookup relative to deployment/k8s or explicit git-root discovery
run same selftest positive/negative suite after move and compare mutation coverage and exit outcomes
selftest never needs live release data, third-party imports, YAML files or network access
inputs explicit absolute JSON path checks structural/cross-field constraints only; success text says INPUT_CONTRACT_VALID not DEPLOY_READY
temporary fixtures removed on success/failure; no sensitive values printed; script executable only for offline modes
assert original scratch source no longer exists; reject references to a missing scripts/work command
```

- Verification contribution: selftest/inputs/example-negative与scope文档检查共同覆盖该文件；schema/README/check一致，runtime Pending不能被改为PASS。
- After this file: 输出可供下个准备文件消费；若focused仍RED，只修当前最小差异，不能通过猜填环境或跳过拒绝测试继续；最后文件后执行完整Step gate。

#### File 2 — `CREATE deployment/k8s/acceptance.md`

- Purpose: 交接后续真实清单与运行验证，分开准备证据。
- Symbols: TEST-001..011 ownership ledger; deferred phase gate。
- Repository evidence: 主Spec§8/14/15批准准备合同及验证职责；本仓无对应准备文件；scratch归档受技能rule22/PLAN-CLAR-001约束；README同owner按两个阶段更新。
- Dependencies and consumers: 前序文件产出契约/证明；本阶段checker与README/后续环境owner消费；无Javacaller/callee/生成源；不被集群或业务服务运行加载。
- Why now: 先proof再schema/示例/说明；后续移动稳定checker再写交接；本文件输出为下一文件必需输入。
- Contract/signature changes: 仅上述TEST-001..011 ownership ledger; deferred phase gate，具体算法/字段见伪代码；无业务接口/DTO/SQL/Springprofile变动。
- Input/output and state mapping: 每Spec TEST→owner/prerequisite/static assertion/runtime action/evidence/status；只有准备层证据归本阶段。
- Error and edge behavior: 实际URL/集群/镜像未知记录AwaitingEnvironment；各runtime Pending，不写猜测命令或成功；格式/路径/类型错误明确非零；不日志secret，不写业务数据；不读隐式运行环境。
- Standards impact: MC-ARCH-001/MC-REUSE-001/MC-CONFIG-001/MC-SCOPE-001/MC-TEST-001；全部core字段的结构/来源/单位一致必须证明；无Java命名/DI/JSON栈/time/Converter改动。
- Literal rule enforcement: Rule 11 — 文件仅位于准备/proof目录，不新建Java层、业务网关或DDL；既有MP/CQE与Light不变。Java其他规则不因本文件产生适用对象，不创建Java包装器。
- Implementation pseudocode:

```text
create all TEST-001..011 entries exactly preserving accepted Spec scenario and expected assertion
columns: owner, actual environment required, phase1 proof, next phase config proof, runtime evidence, result
TEST-011 records phase1 input/diff/scope proof; explicitly supersedes its prior Spec-only stage wording for current Plan instruction
TEST-001/002/003/004/005/006/007/008/009/010 runtime remains Pending, not implied by selftest PASS
name future files gateway/routes/policies/static/Grant/HA/NetworkPolicy and selected schema prerequisites, no resource body
record failed origin/issuer/token/audience/CA/upload20s vs Edge5s gate as future compatibility prerequisite
write old route/TLS/image/hash rollback obligations and prohibition on shared CRD deletion, auth bypass and DB repair
require explicit user authorization before any apply/server-check/browser/fault/service activity; do not embed executable disruptive commands
```

- Verification contribution: selftest/inputs/example-negative与scope文档检查共同覆盖该文件；schema/README/check一致，runtime Pending不能被改为PASS。
- After this file: 输出可供下个准备文件消费；若focused仍RED，只修当前最小差异，不能通过猜填环境或跳过拒绝测试继续；最后文件后执行完整Step gate。

#### File 3 — `MODIFY deployment/k8s/README.md`

- Purpose: 固定最终使用方式与本阶段完成标准。
- Symbols: final check commands; phase handoff; commit whitelist。
- Repository evidence: 主Spec§8/14/15批准准备合同及验证职责；本仓无对应准备文件；scratch归档受技能rule22/PLAN-CLAR-001约束；README同owner按两个阶段更新。
- Dependencies and consumers: 前序文件产出契约/证明；本阶段checker与README/后续环境owner消费；无Javacaller/callee/生成源；不被集群或业务服务运行加载。
- Why now: 先proof再schema/示例/说明；后续移动稳定checker再写交接；本文件输出为下一文件必需输入。
- Contract/signature changes: 仅上述final check commands; phase handoff; commit whitelist，具体算法/字段见伪代码；无业务接口/DTO/SQL/Springprofile变动。
- Input/output and state mapping: scratch路径替换为check.sh；examples/owner合同不变；链接acceptance和Spec。
- Error and edge behavior: 不存在scripts/work残留命令；未获得真实数据不能claim已高可用/已部署；格式/路径/类型错误明确非零；不日志secret，不写业务数据；不读隐式运行环境。
- Standards impact: MC-ARCH-001/MC-REUSE-001/MC-CONFIG-001/MC-SCOPE-001/MC-TEST-001；全部core字段的结构/来源/单位一致必须证明；无Java命名/DI/JSON栈/time/Converter改动。
- Literal rule enforcement: Rule 11 — 文件仅位于准备/proof目录，不新建Java层、业务网关或DDL；既有MP/CQE与Light不变。Java其他规则不因本文件产生适用对象，不创建Java包装器。
- Implementation pseudocode:

```text
replace scratch invocations with bash deployment/k8s/check.sh selftest or inputs "$HOMEPAGE_RELEASE_INPUTS"
HOMEPAGE_RELEASE_INPUTS must be explicitly supplied absolute file path; no default kubeconfig or secret loading
state selftest exit0 plus example input rejection establishes phase1 proof only
link acceptance.md and the primary accepted Spec; list remaining environment/config/runtime obligations
list single final commit paths: schema.json, example.json, README.md, check.sh, acceptance.md
record local Python tool availability separately from future Kubernetes/Envoy versions; no renderer/CRD claim
assert README links exist, core fields/units match schema and no old scratch command survives
```

- Verification contribution: selftest/inputs/example-negative与scope文档检查共同覆盖该文件；schema/README/check一致，runtime Pending不能被改为PASS。
- After this file: 输出可供下个准备文件消费；若focused仍RED，只修当前最小差异，不能通过猜填环境或跳过拒绝测试继续；最后文件后执行完整Step gate。

- Validation working directory: /Users/mario/SelfProject/enjoyshop；自检tmp fixture自清理，不读取用户secret或集群配置。
- Verification command: `bash deployment/k8s/check.sh selftest`（期待0）；`bash deployment/k8s/check.sh inputs deployment/k8s/release-inputs.example.json`（期待非零且错误为null/missing，随后确认没有输出资源）；`git diff --check`；所有CREATE文件分别 `git diff --no-index --check /dev/null 文件绝对路径`。
- Expected result: selftest所有命名positive/negative case通过；example明确拒绝；schema/example/README core键一致；原业务文件hash不变；test ! -e scripts/work/2026-10-09-18-55-homepage-gateway-deployment-preparation-verify.sh成功，最终check.sh重跑同suite通过。
- Failure returns to: fixture/helper错误回File1；schema/README不同回对应文件；未知业务/CRD要求不能在本阶段加实现，记录到下一阶段设计；缺真实环境不是本阶段failure。
- Completion criteria: 本Step可观察准备结果与全部离线断言有真实命令证据；副作用范围为准备文件；运行验收结果保持未执行。
- Rollback: 只撤销本Step本任务新创建/修改且未被别人改动的准备文件；不git clean/restore整仓、不改原文档/数据库。未提交scratch仅清自己临时文件。
- Commit paths: `deployment/k8s/check.sh`, `deployment/k8s/acceptance.md`, `deployment/k8s/README.md`；scratch路径仅工作清单，禁止暂存；最终唯一提交白名单为全部5个deployment/k8s准备文件。
- Commit: `chore(deployment): prepare offline homepage release checks`；最多一次，仅5个准备文件，不带原Spec/Plan/商品dirty文档。

## 8. Test, Validation, and Quality Gates

| Gate/order | Working directory | Command or method | Scope | Expected result | Failure returns to | Requirements/runtime boundary |
| --- | --- | --- | --- | --- | --- | --- |
| 当前Plan资源 | enjoyshop | python3 /Users/mario/SelfProject/Egon-COLA/.agents/skills/egon-coding-writing-plan/scripts/validate_skill_resources.py | 技能 | PASS（16Markdown、21资源） | 技能完整性修复 | REQ-008；静态 |
| 当前Plan strict | enjoyshop | python3 /Users/mario/SelfProject/Egon-COLA/.agents/skills/egon-coding-writing-plan/scripts/validate_plan.py docs/egon/plan/2026-10-09-18-55-homepage-gateway-deployment-preparation.md --strict | 本Plan | metadata/Step/coverage有效 | 本Plan相应章节 | 文档，不是实现证明 |
| Step1 RED | enjoyshop | bash scripts/work/2026-10-09-18-55-homepage-gateway-deployment-preparation-verify.sh selftest | 输入契约 | schema创建前SCHEMA_MISSING导致RED，非import/env错误 | Step1/File1 | offline，不需集群 |
| Step1 GREEN | enjoyshop | 同scratch selftest；inputs release-inputs.example.json反例 | 明确命名输入用例 | suite exit0，example非零拒绝，无render/network/output | Step1 schema/checker | REQ-002/003/005/006/007 |
| Step2迁移回归 | enjoyshop | bash deployment/k8s/check.sh selftest；test ! -e scripts/work/2026-10-09-18-55-homepage-gateway-deployment-preparation-verify.sh | 同一suite/路径 | 全GREEN，无scratch源/README残留旧命令 | Step2/File1/3 | 无runtime证明 |
| 私密真实input检查（有文件时） | enjoyshop | bash deployment/k8s/check.sh inputs "$HOMEPAGE_RELEASE_INPUTS" | 明确传入JSON | 仅INPUT_CONTRACT_VALID或safe字段错误；不作部署许可 | 输入owner | 可选，不是本阶段完成前置 |
| scope/格式 | enjoyshop | git diff --check；CREATE路径逐个no-index --check；用checker check_scope比对捕获的原Java/POM/SQL/profile hash | 白名单 | 仅5个准备文件，原数据/代码/config不变 | owning Step | REQ-004/007/008 |
| 未来CRD/manifest/runtime | 用户实际环境 | 下一阶段Plan确定其schema/render/client/server验证命令；acceptance逐TEST记录 | 当前不执行也不生成 | 本阶段全部标Pending | 下一阶段owner | 不自动下载/访问集群/启动/部署 |

新check_scope只监控本任务已确定的原源码/config/SQL路径，不输出敏感文件内容。当前没有部署命令可复用，因此使用技能要求scratch，最终移入子系统；不创建通用JSON Schema引擎、渲染器、脚本顶层工具、运行服务或安装库。所有命令在未来实施时执行，本轮只做文档校验。

## 9. Migration, Compatibility, Rollout, and Rollback

数据库/生成器迁移N/A：主Spec§11，无SQL/manifest/DAO/新工程目标；不运行codegen、不写SQLjournal、不改示例业务。运行rollout当前Deferred：不创建Gateway/HTTPRoute/EnvoyProxy/Nginx Deployment/Grant、不调整DNS/LB，不选择cluster/CRD版本或改OIDC/Token合同。

本阶段回滚只涉及新准备文件；输入合同变化按后续Spec/Plan审阅，不能自动覆盖用户真实输入。后续实际部署仍必须完成版本兼容、TLS/IP/identity、原API透传、上传时限/限制、静态hash保留、共享配额、Ready/drain、Redis/DB/控制面故障验收；记录到acceptance.md而非假装本阶段已证明。真实用户环境值只存私密输入，Secret内容不入仓库。

## 10. Requirement-to-Step Traceability Matrix

| Requirement | Effective Spec section | Steps | Files | Tests/gates | Completion evidence |
| --- | --- | --- | --- | --- | --- |
| REQ-001 | §4/7.3.1 | Step1/2（合同/交接） | README/acceptance | 路径/禁止fallback合同完整；实际TEST-001/002后续 | 准备合同完成；真实分流Deferred |
| REQ-002 | §4/8/15.1 | Step1/2 | schema/example/check/README | versions required/latest拒绝 | 离线输入完成；actual CRD/Gateway条件Deferred |
| REQ-003 | §4/7.3.1/16 | Step1/2 | schema/check/acceptance | origin/secret/reference拒绝 | 原安全未改；TLS/IP/Token真实TEST-003/009 Deferred |
| REQ-004 | §4/13/15.3 | Step1/2 | README/check_scope/acceptance | scope原cache/hash不变 | 无新Lua/cache；回源运行TEST-005 Deferred |
| REQ-005 | §4/7.3.3/15 | Step1/2 | schema/check/acceptance | DENY、配额边界、policyRefs | 输入责任完成；共享配额TEST-004/005 Deferred |
| REQ-006 | §4/15.4/16 | Step1/2 | schema/check/acceptance | 副本/timeout crosschecks | 可离线证明数值；HA/Pod/drain/依赖TEST-006/007 Deferred |
| REQ-007 | §4/9/15.2/16 | Step1/2 | schema/example/README/acceptance | digest/资源保留/原业务hash | 准备合同和原代码不变；实际旧hash/API TEST-003/008 Deferred |
| REQ-008 | §4/20.6+阶段选择 | Step1/2 | 本Plan及5个准备文件 | strict/scope/临时脚本处置 | 当前只Plan；实施时只offline准备，无集群/业务运行 |

完整Spec验收未由本阶段替代。下一阶段应沿主Spec§8覆盖base/gateway.yaml、routes.yaml、static-deployment.yaml、static-service.yaml、static-nginx.conf、availability.yaml、network-policy.yaml、envoy-policies.yaml及platform-contract/edge-reference-grant.yaml。必须有真实环境证据后再编写其实施步骤，此Plan不授权创建它们。

## 11. Risks, Blockers, and User Decisions

| ID | Risk or decision | Impacted Steps/files | Evidence | Owner | Status/action |
| --- | --- | --- | --- | --- | --- |
| DEC-PHASE-001 | 当前仅配置准备+offline，真实环境/部署后续补齐 | 当前Step1/2与剩余清单 | 用户明确选择推荐前提 | mario | Closed；范围记录完成，不再等待当前集群资料 |
| DEFER-001 | 集群/Envoy/Gateway API schema与运维渲染机制 | 下一阶段资源配置，不影响当前Steps | 主Spec§15/18 | 环境owner | Deferred；待后续实际部署Plan前提供；当前不猜字段或安装工具 |
| DEFER-002 | 静态镜像/前版hash资源/UID/probe/drain证据 | 下一阶段静态工作负载 | enjoyshop-web只有POM、Spec§16 | 前端owner | Deferred；当前不生成页面或静态Deployment |
| DEFER-003 | 平台Service/TLS/IP/身份与上传预算/容量资料 | 下一阶段集群接入/runtime | 主Spec§15/16 | 平台/网络/身份owner | Deferred；不改Token/timeout，不宣称连通 |
| RISK-001 | 合成fixture/输入check成功被误认部署许可 | check/README/acceptance | 输入有效不代表证书/镜像/schema可用 | 执行者 | Closed；输出明确INPUT_CONTRACT_VALID，运行Pending |
| RISK-002 | 多Step或无关dirty混入commit | 最终提交 | 用户AGENTS | 执行者 | Closed；最多1次，显式5文件白名单，scratch/设计文档除外 |

本阶段无Open主要决策或阻塞；上述Deferred为完整Spec的剩余工作，不计入准备阶段完成声明。未经用户进一步请求不开始下一阶段部署计划或实施。

## 12. Review and Acceptance

### 12.1 Original requirement fidelity

采用用户最新选择，当前只规划准备与离线验证。保留全部主Spec责任及剩余清单/runtime验收映射，未擅自重定义技术路线或把阶段PASS写成整Spec完成。

### 12.2 Spec consistency

schema/example/README/acceptance属于原Spec§8目标；checker/scratch仅支撑§14并按技能rule22归档。实际清单由真实输入触发下一阶段设计，不删除Spec义务。局部JSON表示/单位推断在§6.4列明；不新增业务接口/表/平台源码/缓存模式/渲染框架。

### 12.3 Repository executability

所有当前步骤只需要已探测可用的python3/bash/标准库；selftest无需cluster/镜像/真实值。明确RED reason、per-file顺序与错误/无副作用、同suite迁移和最终一次commit；无默认环境数据源。未执行代码或后续checker，文档验证与实现证据分开。

### 12.4 Test and release completeness

focused input套件先RED后schema GREEN，反例持续拒绝；移脚本后重复suite。原Java/Spring/SQL/API只做scope/hash保持，实际业务运行不在当前完成边界。acceptance保留全部TEST-001–011及owner/证据/状态；运行项Pending，只有真实用户验收能关闭。

### 12.5 Blocking Manual Check

| Check ID | Applicability | Status | Evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- |
| MC-ARCH-001 | Applicable | PASS | §4.7/5；主Spec§6.1 | Light及外部平台职责保持，当前无Java/资源实现 | None |
| MC-REUSE-001 | Applicable | PASS | §4.5/4.7、本机python3资源验证 | 标准库足以做明确参数check，不复制运行基础设施 | None |
| MC-DEP-001 | Applicable | PASS | §5/6，无POM/CRD/render库目标 | 当前不装依赖/工具，外部版本归下阶段 | None |
| MC-NAME-001 | Not applicable | N/A | §5只有JSON/sh/doc | 无Java名称变更 | None |
| MC-VALID-001 | Not applicable | N/A | 原业务handoff不变，§5/7 | Java校验不改；准备输入按独立suite验证 | None |
| MC-MODEL-001 | Not applicable | N/A | 无POJO/SQL/enum目标 | 无Java建模/唯一键/DDL变化 | None |
| MC-CONVERT-001 | Not applicable | N/A | 无Converter/MapStruct目标 | 原跨层转换保持 | None |
| MC-LOG-001 | Not applicable | N/A | 无业务Java类 | 无新Slf4j责任，offline输出safe字段路径 | None |
| MC-BEAN-001 | Not applicable | N/A | 无Bean/lombok.config目标 | 原DI保持 | None |
| MC-UTIL-001 | Not applicable | N/A | 无Javautility/import变化 | 标准库checker不新增Java工具依赖 | None |
| MC-JSON-001 | Not applicable | N/A | 业务JSON wire不变，输入属于部署准备 | Boot Jackson不变，无新增serializer/转换 | None |
| MC-TIME-001 | Not applicable | N/A | 无Java日期/时间变更 | 秒单位是准备输入表示，不改java.time/运行deadline | None |
| MC-CONFIG-001 | Applicable | PASS | §6.4/7：core键/单位/schema/example一致 | 同schema用于各环境，unknown/null拒绝；Spring profiles保持 | None |
| MC-PATTERN-001 | Applicable | PASS | §4.5/4.7；原Spec§13 | 保留现有Cache-Aside/流水线；无新复杂业务分派 | None |
| MC-SCOPE-001 | Applicable | PASS | §5/commit/用户阶段选择 | 当前只offline准备，余下manifest/runtime明确Deferred | None |
| MC-TEST-001 | Applicable | PASS | §7/8精确命名input suite和归档回归 | 本阶段proof无需真实环境，不能冒充CRD/live验收 | None |
| MC-BLOCKER-001 | Applicable | PASS | §11/用户明确阶段选择/以上MC | 当前所有适用设计检查闭合，外部资料只影响下阶段 | None |

### 12.6 Final verdict

PASS — Ready for user review

本节保留Plan设计审查时的结论；当前实际执行收尾记录见§12.7，Header为Completed。该状态仅覆盖这份明确限定为第一阶段的两Step Plan，不表示完整架构已部署或全部运行验收通过。

本轮已执行：技能资源preflight PASS（16 Markdown、21资源）；修订Plan严格验证PASS；主Spec严格验证PASS；修订Plan no-index whitespace检查通过。没有创建checker/schema/部署清单或执行任何Step；所有业务/环境验证仍未运行。

### 12.7 执行完工与状态收尾（2026-10-09 21:35 Asia/Shanghai）

用户已确认先做配置准备/离线验证，实际环境/部署后续补齐，并明确请求执行及完成全部Plan。该范围内没有未实现Step、未定scratch或依赖真实环境的阻塞。不能把完整Spec的Deferred自动扩展为本Plan新Step。

| Step | 状态 | 交付提交 | 文件/证据 |
| --- | --- | --- | --- |
| 1 定义输入契约并验证拒绝 | Completed | d4e392a303f3af0bca4654d73b0602087154f0f2（共享最终提交，按用户单一commit规则） | 39字段schema、全null示例、README、归档前scratch；先SCHEMA_MISSING RED后32自检GREEN |
| 2 归档检查与验收交接 | Completed | 同一d4e392a提交 | check.sh、acceptance.md、README；同32测试通过、无发布输出、scratch源不存在 |

实现交付只包含deployment/k8s下5个准备文件；完整Step、17项Manual Check、10项Literal Rule和8项REQ审计见 [acceptance.md](../../../deployment/k8s/acceptance.md)。acceptance.md里的Plan/Spec hash是执行当时的输入快照，收尾元数据变更不追溯改写该历史证明。

当前只补两个相关设计文档的状态/记录，不新增代码、依赖、K8s清单、镜像或运行动作，不再提交第二个实现commit，不stage原商品管理文档。主Spec保持Accepted而非Implemented。完整架构实际资源及TEST-001–010运行验收仍按用户决定Deferred/Pending。
