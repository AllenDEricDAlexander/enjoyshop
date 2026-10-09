# 首页 K8s 配置准备（第一阶段）

本目录只提供发布输入契约、离线检查和后续验收交接。没有 K8s 资源清单、渲染器、页面产物或控制器安装步骤；不访问集群、数据库、Redis、镜像仓库，不启动服务或浏览器。

设计依据：[首页架构 Spec](../../docs/egon/spec/2026-10-09-16-39-homepage-kubernetes-gateway-architecture.md)；[第一阶段 Plan](../../docs/egon/plan/2026-10-09-18-55-homepage-gateway-deployment-preparation.md)。用户确认实际环境和部署待后续补齐。

## 离线使用

从仓库根执行。两个 Step 已依次验证，检查入口已归档到本目录；不需要 scripts/work。

```bash
bash deployment/k8s/check.sh selftest
# 示例全部为 null；下面命令应返回 1 / INPUT_REQUIRED。
bash deployment/k8s/check.sh inputs deployment/k8s/release-inputs.example.json
```

实际环境 owner 提供私密输入后，可显式检查该文件：

```bash
HOMEPAGE_RELEASE_INPUTS=/absolute/private/homepage-release-inputs.json
bash deployment/k8s/check.sh inputs "$HOMEPAGE_RELEASE_INPUTS"
```

不提供路径就拒绝；不读取默认 kubeconfig、环境 secret 或示例来补值。输入模式只读取显式文件，不修改它、不输出 YAML 或发布包。退出码：0=输入契约有效或自检通过，1=输入/契约/自检拒绝，2=命令用法或 Python 前置错误。失败只打印固定错误码和安全字段路径，不回显原始值或 JSON 内容。

`INPUT_CONTRACT_VALID` 仅表示已定义的离线约束通过，**不表示可部署**。自检的合成完整正例是临时测试数据，不能作为实际环境证据。填写示例的 39 个字段均为 null，故意不能通过检查；不要填入虚构生产值。

[JSON Schema](release-inputs.schema.json) 是 draft 2020-12 声明；专属检查器不是通用 JSON Schema 引擎。除字段类型/范围/必填外，检查器还执行非blank、跨字段和凭据拒绝规则。复杂环境记录的内部结构留到下一阶段，标准schema解析器验证也不能替代这些检查及真实验收。

## 输入字段、来源与单位

dev/test/prod 共用下列同一组 core 字段，值由对应 owner 提供。不能修改 schema 给某个环境隐藏缺值或新增任意 core 字段。真实输入不放入仓库，不存放 token、password、clientSecret、accessKey、secretKey 或私钥原文；只提供允许的名称/证据引用。

| 字段 | 类型/范围 | Owner / 真实来源 | 含义与后续证据 |
| --- | --- | --- | --- |
| `clusterVersion` | string | 环境/集群 owner | 目标 Kubernetes 固定版本；不是本地 kubectl 版本。 |
| `gatewayApiVersion` | string | 环境/集群 owner | 环境 owner 批准的 Gateway API 固定版本。 |
| `envoyGatewayVersion` | string | 环境/集群 owner | Envoy Gateway 固定版本；兼容矩阵另行验收。 |
| `envoyProxyImageDigest` | string | 环境/集群 owner | Envoy 数据面完整不可变镜像引用，包含 @sha256:64位小写hex。 |
| `appNamespace` | string | 环境/集群 owner | 应用 namespace，合法 DNS label。 |
| `platformNamespace` | string | 环境/集群 owner | 平台 namespace，合法 DNS label。 |
| `envoyNamespace` | string | 环境/集群 owner | 实际 Envoy 数据面 namespace，合法 DNS label。 |
| `gatewayClassName` | string | 环境/集群 owner | 已安装 GatewayClass 名称；本准备任务不创建控制器。 |
| `publicHostname` | string | 域名/TLS/身份/平台 owner | 实际站点 DNS 主机名，不能携带 scheme/port/path。 |
| `publicOrigin` | string | 域名/TLS/身份/平台 owner | HTTPS 站点 origin；不带 query/fragment/凭据，host必须匹配publicHostname。 |
| `issuerUri` | string | 域名/TLS/身份/平台 owner | 实际 HTTPS issuer URI；可带 issuer path，真实发现/准入另验。 |
| `oidcCallbackUri` | string | 域名/TLS/身份/平台 owner | 实际 HTTPS OIDC callback；origin必须与publicOrigin一致。 |
| `tlsSecretName` | string | 域名/TLS/身份/平台 owner | 站点 TLS Secret 名称引用，不能提供私钥。 |
| `edgeCaReference` | string | 域名/TLS/身份/平台 owner | Edge CA 配置/Secret引用，不能提供CA/私钥原文；真实CA/SNI另验。 |
| `edgeTlsServerName` | string | 域名/TLS/身份/平台 owner | Edge TLS 校验证书的 DNS/SNI 名称，真实匹配另验。 |
| `edgeServiceName` | string | 环境/集群 owner | 实际 Edge ClusterIP Service 名称；不猜容器端口为Service端口。 |
| `edgeServicePort` | integer ≥ 1，≤ 65535 | 环境/集群 owner | 实际 Edge Service TCP 端口，1..65535整数。 |
| `staticServicePort` | integer ≥ 1，≤ 65535 | 环境/集群 owner | 实际静态 Service TCP 端口，1..65535整数。 |
| `trustedProxyChain` | array | 网络 owner | 有序可信代理逐跳记录；本阶段仅要求非空对象数组，不猜hop/CIDR。 |
| `clientIpMode` | string | 网络 owner | 网络 owner 批准的实际源IP模式引用；不是自动采用单跳。 |
| `staticImageDigest` | string | 前端产物/发布 owner | 真实静态镜像完整不可变引用；本工具不拉取或证明镜像存在。 |
| `staticArtifactVersion` | string | 前端产物/发布 owner | 固定静态产物版本；禁止latest；旧hash保留另验。 |
| `assetRetentionPolicy` | object | 前端产物/发布 owner | 旧资源保留/不可覆盖合同的非空记录；结构细节由后续产物owner确定。 |
| `envoyReplicas` | integer ≥ 2 | 容量/平台/数据库 owner | Envoy数据面副本数整数，至少2；不等于HA已证实。 |
| `staticMinReplicas` | integer ≥ 2 | 容量/平台/数据库 owner | 静态最小副本数整数，至少2。 |
| `staticMaxReplicas` | integer ≥ 2 | 容量/平台/数据库 owner | 静态最大副本数整数，至少2且不小于staticMinReplicas。 |
| `resourceRequests` | object | 容量/平台/数据库 owner | 资源requests非空记录；Quantity/容量/CPU指标留到真实清单验证。 |
| `resourceLimits` | object | 容量/平台/数据库 owner | 资源limits非空记录；不能凭非空对象证明容量合理。 |
| `ingressLocalLimit` | integer ≥ 1 | 容量/平台/数据库 owner | 入口实例本地请求额度正整数；不是全局配额。 |
| `burst` | integer ≥ 0 | 容量/平台/数据库 owner | 允许的突发额度非负整数；实际CRD字段/计量方式后续冻结。 |
| `bizPolicyRefs` | array | 容量/平台/数据库 owner | Biz共享配额策略非空引用列表；不新增Envoy全局RLS。 |
| `quotaFailureMode` | string，DENY | 容量/平台/数据库 owner | 严格业务配额失败策略，固定DENY；不以LOCALFALLBACK宣称严格配额。 |
| `readyProbeContract` | object | 环境/集群 owner | 实际角色Ready/liveness/端口合同非空记录；本阶段不定义或验证探针路径。 |
| `drainTimeout` | integer ≥ 1 | 环境/集群 owner | 期望drain预算，正整数秒；不修改现有服务实际timeout。 |
| `terminationGracePeriod` | integer ≥ 1 | 环境/集群 owner | 终止宽限正整数秒；须严格大于drainTimeout+requestTimeouts.inFlightSeconds。 |
| `requestTimeouts` | object | 环境/集群 owner | 请求预算记录；inFlightSeconds为最大在途正整数秒，其他环境细节后续核对。 |
| `redisHaEvidence` | string | Redis owner | Redis用途/HA/恢复证据引用，仅检查非空，不验证真实运行。 |
| `dbCapacityEvidence` | string | 容量/平台/数据库 owner | DB回源容量证据引用，仅检查非空，不访问数据库。 |
| `platformAdmissionEvidence` | string | 域名/TLS/身份/平台 owner | 平台准入/唯一Pod实例/Provider地址/TLS/租约证据引用，不读取token。 |

### 专属离线规则

- 每个 core 字段必填、非 null；顶层未知字段拒绝；integer 不接受 JSON boolean 或数字字符串。对象/数组非空，策略列表项为非blank字符串，代理链项为非空记录。
- namespace、资源名、主机名检查基本 DNS 语法；端口 1–65535；镜像必须是完整 `image@sha256:64位小写hex` 引用。离线不证明资源名被目标版本 admission 接受或镜像存在。
- 固定版本不能含 `latest`。`staticMinReplicas`、`envoyReplicas` ≥2，`staticMaxReplicas` 不小于最小副本数；入口本地额度为正数，burst ≥0；严格业务配额固定 `DENY`。
- `drainTimeout`、`terminationGracePeriod`、`requestTimeouts.inFlightSeconds` 均为正整数秒。宽限须严格大于 drain 与最大在途预算之和；相等也拒绝。这个输入计算不修改现有运行 timeout。
- HTTPS URI 不含用户名、密码、fragment、空白或非法端口。站点 origin 只能根路径、无 query，host 与 publicHostname 一致；callback 与 origin 按 host/有效端口同源，默认443与显式443等价。真实 issuer、callback路径、CA/SNI匹配留到环境验收。
- 在所有嵌套记录检查 `password/token/privateKey/clientSecret/secretKey/accessKey` 键（大小写/下划线等形式统一识别），以及私钥PEM原文；拒绝 JSON重复键、NaN、Infinity和非法JSON。这个检查不是万能凭据扫描器，owner仍须确保记录只含引用而不是其他名称下的秘密。

`resourceRequests`、`resourceLimits`、`assetRetentionPolicy`、`readyProbeContract` 是尚未冻结内部结构的非空记录；`trustedProxyChain` 只是有序记录数组；三类证据字段仅检查非空引用。工具不解释或采信这些记录为 HA、Ready/drain、可信 IP、容量或准入成功，后续须根据实际版本和环境明确内部字段并验证。

## 已确认的后续架构约束

- 静态 HTML/JS/CSS 独立源站，图片保留已有 S3 兼容公开 URL；不经 Java 托管静态资源，不新增 FastDFS。
- K8s 集群入口用 Gateway API + Envoy Gateway。业务仍用 Yuheng Edge/Biz，不新建 enjoyshop_gateway、不把业务数据库访问或广告缓存搬进 Lua。
- `/api`、`/bff`、`/oauth2/authorization`、`/login/oauth2/code`、`/tenants`、`/platform` 按已审核合同进入 Edge；根静态路由不得把 API 或 `/actuator`、`/v3/api-docs`、`/swagger-ui`、`/graphql`、`/mcp` 转成 HTML。首期无新增匿名广告绕行。
- Edge 本地处理 BFF/OIDC，API 路径按原 TokenRelay/Biz/Provider链；Envoy不判业务角色，不重写path/query，不共享缓存动态JSON，不自动重试代理请求（含GET），保留原响应/头及原上传认证。
- 业务缓存复用 Java Guava L1 + Redisson L2，事务后失效和 Pub/Sub 是原组件机制；TTL最终一致不能承诺即时全局下架。Redis故障可能回源，真实DB容量/保护必须后续验证。
- 入口local限制按实例；严格共享配额只由Biz原分布式桶拥有，Redis失败严格策略DENY，不部署额外Envoy全局RLS。多Pod/HPA不自动产生全局额度或Redis/DB高可用。
- 保留文件上传10 MiB文件/11 MiB multipart、201/Location/ResultRecord和原安全失败合同。当前Edge5s与对象存储20s预算需要真实接入验证，不能默改timeout或Token/audience。

## 下一阶段输入与验收

待提供：目标集群/Gateway API/Envoy固定版本及官方CRD schema、现有运维渲染/校验机制；真实静态镜像digest与当前/前版hash资源、非root UID/目录/探针/优雅退出合同；平台Service端口、TLS/CA/SNI、proxy链与selector、OIDC/resource兼容、Provider地址/租约、Redis HA与DB回源容量证据。

后续才规划并创建 Gateway/HTTPRoute/Envoy策略、静态 Deployment/Service/Nginx配置、HA/NetworkPolicy和最小ReferenceGrant。缺真实数据不能创建可发布清单、补假backend或占位页面。逐TEST交接与执行审计见 [acceptance.md](acceptance.md)，运行项保持Pending。

## 源码与提交边界

该任务最终只交付本目录五个准备文件；原Java/POM/Spring profile/SQL/manifest/图片合同不变。scratch自检前后比对原业务源码与配置hash；执行者另外保存本任务开始时的完整保护hash，复核未修改原源码及设计文档。

两个Step顺序验证，按用户AGENTS最多一次最终path-limited commit。scratch不暂存，最后移入本目录；不使用git add .，不带无关商品或Spec/Plan文件。当前没有push/PR/release或实际部署动作。

本次准备自检：32个测试方法通过、null示例被拒绝；schema/README同39字段、原618个保护文件hash保持。全部Manual Check、Literal Rule、Step和完整Spec差距记录在 [验收与审计](acceptance.md)。这不是运行部署验收。
