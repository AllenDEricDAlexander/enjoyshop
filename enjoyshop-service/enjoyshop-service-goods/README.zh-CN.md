# enjoyshop-service-goods

[English](README.md) | [中文](README.zh-CN.md)

`enjoyshop-service-goods` 是由 `egon-cola-archetype-light` 生成的单 Maven 模块项目。它是一个可部署的单体应用，其 Java 包结构约束大型单体轻量领域架构；这些分层不是 Maven 子模块。

## Maven Profiles 与外部启动参数

每次选择 `dev`、`test`、`prod` 中一个环境 profile，未指定时使用 `dev` 默认值。
这些 profile 控制应用启动，Surefire 仍使用原有 `test` 配置。堆内存参数是示例值，部署时按资源调整。

在项目根目录执行，多模块项目先安装兄弟模块依赖：

```bash
mvn install
mvn -Pdev spring-boot:run
mvn -Pprod -Drun.jvm-args="-Xms1g -Xmx2g" \
  -Drun.server-port=8080 \
  -Drun.config-location=file:/etc/myapp/override.yml package
```

Maven 在 `process-resources` 阶段生成 `target/launch.args`，打包时也会生成。
切换 profile 或 `-Drun.*` 参数后，无须 `clean` 即可更新文件。此执行仅使用 `@...@`
过滤 `src/main/launch/launch.args`，现有 YAML 占位符继续在运行时解析，参数文件不进入 JAR。

将可执行 JAR 与 `launch.args` 一起部署，JAR 可重命名为 `app.jar`，然后在部署目录执行：

```bash
java @launch.args -jar app.jar
java @launch.args -Xmx3g -jar app.jar --server.port=9080
```

文件记录 JVM 参数、Spring profile、端口及额外配置位置。这些显式系统属性优先于对应环境变量；
`-Drun.*` 覆盖 Maven 默认值，JVM 覆盖参数放在 `-jar` 前，Spring 命令行覆盖参数放在 JAR 后。
单独 `java -jar` 或 IDE 直接运行 main 不会自动读取该文件。相对配置路径以启动工作目录为准，
并非参数文件所在目录；部署建议使用绝对 `file:` 路径，Windows 路径使用正斜杠。
额外文件补充 `application.yml` 和选中的 `application-{profile}.yml`，不替换默认配置位置；
要求文件必须存在时去掉 `optional:`。密码和密钥继续通过现有环境变量/secrets 注入，不写入 `run.*`。

## 商品图片上传配置与发布检查

上传接口为 `POST /api/v1/files`，使用 Shoubing USER Bearer 认证，接收名为 `file` 的单个
`multipart/form-data` 文件。服务只接受实际内容为 JPEG 或 PNG、大小不超过 10 MiB 的图片；成功时返回
`201 Created`，`Location` 与 `ResultRecord.data` 都是稳定的公开 HTTPS URL。客户端文件名和声明的 MIME
类型不会用于对象键或存储类型。对象存储写入结果未知时不要自动重试，先人工确认远端对象状态。

部署环境应通过环境变量或 Spring 外部配置提供下列值。实际密钥使用 Secret Manager、挂载密钥或
`config/application-secrets.yml`，不要放进仓库：

| 环境变量 | 对应属性 | 用途 |
| --- | --- | --- |
| `GOODS_CORS_ALLOWED_ORIGINS` | `app.web.cors.allowed-origins` | 逗号分隔的前端来源精确白名单 |
| `TIANQUAN_SHOUBING_ENABLED` | `egon.cola.component.platform.tianquan.shoubing.enabled` | 启用 Shoubing 验证 |
| `TIANQUAN_SHOUBING_ISSUER` | `egon.cola.component.platform.tianquan.shoubing.issuer` | Shoubing issuer URI |
| `TIANQUAN_SHOUBING_JWK_SET_URI` | `egon.cola.component.platform.tianquan.shoubing.jwk-set-uri` | issuer 公钥集地址 |
| `TIANQUAN_SHOUBING_RESOURCE_SERVER_ID` | `egon.cola.component.platform.tianquan.shoubing.resource-server-id` | 已登记的资源服务器标识 |
| `TIANQUAN_SHOUBING_RESOURCE_URI` | `egon.cola.component.platform.tianquan.shoubing.resource-uri` | 已激活的商品媒体 Resource URI |
| `TIANQUAN_SHOUBING_SERVICE_APP_ID` | `egon.cola.component.platform.tianquan.shoubing.service-client.app-id` | Shoubing 服务端客户端标识（平台要求时配置） |
| `OBJECT_STORAGE_ENDPOINT` | `app.object-storage.endpoint` | MinIO 或 Ceph RGW 的 S3 API 地址，不是公开下载地址 |
| `OBJECT_STORAGE_REGION` | `app.object-storage.region` | S3 签名 Region，默认 `us-east-1` |
| `OBJECT_STORAGE_BUCKET` | `app.object-storage.bucket` | 预先创建的图片 bucket，默认 `product-media` |
| `OBJECT_STORAGE_PATH_STYLE_ACCESS` | `app.object-storage.path-style-access` | MinIO/Ceph 通常使用 `true` |
| `OBJECT_STORAGE_PUBLIC_URL_BASE` | `app.object-storage.public-url-base` | bucket/object 可公开读取的 HTTPS URL 前缀 |
| `OBJECT_STORAGE_ACCESS_KEY` | `app.object-storage.access-key` | S3 access key，密钥 |
| `OBJECT_STORAGE_SECRET_KEY` | `app.object-storage.secret-key` | S3 secret key，密钥 |
| `APP_OBJECT_STORAGE_API_CALL_ATTEMPT_TIMEOUT` | `app.object-storage.api-call-attempt-timeout` | 单次请求超时，默认 `15s` |
| `APP_OBJECT_STORAGE_API_CALL_TIMEOUT` | `app.object-storage.api-call-timeout` | 总请求超时，默认 `20s`，必须不小于单次超时 |

`egon.cola.platform.tianquan.shoubing.register-filter` 固定为 `false`：goods 将 Shoubing Filter 放进
仅匹配文件上传 POST 的 Spring Security 链，避免 Servlet 外层过滤器设置的认证上下文被 Spring Security 清除。

生产或联调前由发布负责人确认 Shoubing issuer/JWK、共享 Redis、ACTIVE Resource、USER_DELEGATION grant
和 Jianshen application-entry 授权已就绪。Shoubing Starter 的 USER Token 校验是无状态身份校验；这些授权
必须在上游授权/发放 Token 前完成，goods 上传接口本身不重新查询 Resource grant。缺失或无效 Bearer 在
上传接口返回 Shoubing 401；授权流程拒绝发放 Token 时，由上游授权流程返回拒绝结果。

对象存储 bucket 的公开 GET 策略及前端 CORS 来源也必须配置完成。上传后使用返回 URL 下载对象，并比较
下载字节与上传图片。此发布检查不会由 `verify` 自动执行，也不要求应用启动时访问对象存储。

## 领域优先结构

业务领域位于协议或技术细节之前，使 `user` 和 `teaching` 保持内聚，未来可以在不反转包顺序的情况下拆分为独立服务。

```text
src/main/java/top/egon/cola/archetype/source/light
├── start
├── adapter
│   ├── user/{controller,mq,graphql,facade/impl,pojo/{dto,vo,convertor},validators}
│   ├── teaching/{controller,mq,graphql,facade/impl,pojo/{dto,vo,convertor},validators}
│   ├── pojo/{dto,convertor}
│   ├── handler
│   └── filter
├── facade
│   ├── user/{dto,enums,utils}
│   ├── teaching/{dto,enums,utils}
│   └── validation
├── application
│   ├── user/{manage/impl,pojo/{command,query,result,convertor},validators}
│   └── teaching/{manage/impl,pojo/{command,query,result,convertor},validators}
├── domain
│   ├── user/{entities,aggregates,vos,service,validators,enums}
│   └── teaching/{entities,aggregates,vos,service,validators,enums}
├── infrastructure
│   ├── user/{dao,po,converter,repo,service/impl,client/impl,validators}
│   ├── teaching/{dao,po,converter,repo,service/impl,client/impl,validators}
│   ├── aop
│   ├── mq/impl
│   └── config
└── common/{constants,utils,enums,exception}
```

`adapter` 负责 HTTP、GraphQL、RabbitMQ consumer，以及原生 RPC provider——`@EgonRpcProvider` 直接标注在 `facade/impl` 的契约实现类上，不再有独立的 `rpc` 包装包。`facade` 负责稳定的外部契约、契约载体、契约枚举、契约断言与共享校验分组，并保持自包含以便单独发布。`application` 编排用例和事务。`domain` 负责业务状态、规则以及查询/事件/幂等领域服务契约。`infrastructure` 提供 MyBatis-Plus DAO、`EgonModel` 持久化对象、`EgonColaRepository` 子类和这些领域契约与出站客户端的实现。`common` 存放与业务无关的基础类型，以及各层共享的异常根类型。`start` 负责组装和运行时配置。

## 依赖图

内部层之间只允许以下依赖：

```text
start          -> adapter, infrastructure
adapter        -> application, facade
application    -> domain
domain         -> common.exception
infrastructure -> domain, facade
facade         -> common.exception
common         -> no business layer
```

只有 `common.exception` 里的异常根类型可以跨层出现，其他 `common` 类型不得离开本层。Domain Service 接口位于 `domain.<business>.service`，实现位于 `infrastructure.<business>.service.impl`；Application service 只依赖这些契约，绝不依赖 infrastructure 类型。

依赖图由 `egon-cola-component-bytecode-architecture-maven-plugin` 强制检查。它绑定在 `verify` 阶段，把每个 `top.egon.enjoyshop.goods.<layer>` 包映射到对应分层，并以 `unknownLayerPolicy=FAIL` 运行；因此落在映射之外的类会让构建失败，而不是只留下一条警告。`adapter.controller`、`infrastructure.repo` 这类技术优先的根包因为无法解析到任何分层而被拒绝。该检查只在 `verify` 阶段执行，单独运行 `./mvnw test` 不会触发它。

## 公共合同

各层复用 common-core 合同，不再各自造轮子：

- 异常：`common/exception` 的根类型继承组件的 `BusinessException` / `CommonException` 链，对外稳定错误码是 `getStatus()` 字符串，数字型 `getCode()` 不参与传输。
- 枚举：手写业务枚举实现 `EgonEnum`，序列化取声明的 `code` 而不是 `ordinal()`；`DeletedStatus` 与 `*FacadeStatus` 示范 `UNKNOWN` 兜底。
- 校验：现有 BaseValidator/egonColaValidationUtils 入口按源码识别；新增或修改边界以原生/自定义约束注解、@Valid、@Validated、分组及扩展定义规则，工具仅负责通用手工触发。保持现有协议间的分组差异。
- 转换：DTO/Command/Domain/PO 映射由 MapStruct（`@Mapper`）生成并继承 `BaseConverter` 或 `BaseForwardConverter`；只做快照投影的转换器不补反向方法。

## 主要业务流程

1. 请求校验并完成外部身份查询后创建用户。
2. 为有效用户分配有效角色。
3. 为未归档角色授予有效权限。
4. 创建学校班级和课程。
5. 在校验班级、课程、学期和时间冲突规则后安排课程。

相同的 Application 用例服务于 HTTP、GraphQL、COLA RPC 和 RabbitMQ 入口。用户、权限、学校班级和课程查询都通过 Application 边界实现，而不是在各协议 adapter 中重复实现。

## 持久化与集成

持久化使用 Common MP Repository 与显式 Mapper XML；Domain Service 不依赖技术 CRUD 类型。

`dev` 是本地工作站开发和 `feature/*` 分支验证的默认 profile，使用由环境变量提供的 PostgreSQL、Redis、RabbitMQ、COLA RPC 和外部 HTTP 集成。

Maven 测试会自动选择 `test`，`dev`、`release/*` 和 `hotfix/*` 分支的测试流水线也使用该 profile。它使用 H2、内存 adapter 和确定性 stub，并关闭 RabbitMQ、Redis、COLA RPC registry 和外部 HTTP 调用。

`prod` 仅用于 `main` 分支的运行时构建和部署。`dev` 与 `prod` 通过 `RABBITMQ_ENABLED=true`、`REDIS_ENABLED=true`、`EXTERNAL_HTTP_ENABLED=true`、`TIANSHU_RPC_TARGET=host:19090`、`TIANSHU_ENABLED=true`、`TIANSHU_REGISTRY_ENABLED=true` 和 `RPC_ENABLED=true` 等环境变量配置真实 adapter。消息代理凭据使用 Spring 自身的变量名 `SPRING_RABBITMQ_HOST`、`SPRING_RABBITMQ_PORT`、`SPRING_RABBITMQ_USERNAME`、`SPRING_RABBITMQ_PASSWORD`，而 `RABBITMQ_ENABLED` 与 `RABBITMQ_LISTENER_AUTO_STARTUP` 是应用自身的开关。

## 命令

运行全部测试和架构检查：

```bash
./mvnw -B -ntp verify
```

必须使用 `verify`。架构治理插件绑定在该阶段，`./mvnw test` 只会跑测试，不做任何分层检查。

打包应用：

```bash
./mvnw -B -ntp -DskipTests package
```

配置好 `dev` 集成后在本地运行：

```bash
./mvnw spring-boot:run
```

## 容器交付

生成的项目使用一个从源码构建的 `deploy/container/Dockerfile`：

```bash
docker build --build-arg CONTAINER_ENGINE=docker -f deploy/container/Dockerfile -t enjoyshop-service-goods:local .
podman build --build-arg CONTAINER_ENGINE=podman -f deploy/container/Dockerfile -t enjoyshop-service-goods:local .
nerdctl build --build-arg CONTAINER_ENGINE=nerdctl -f deploy/container/Dockerfile -t enjoyshop-service-goods:local .
```

使用以下命令启动完整的 Docker 开发栈：

```bash
docker compose --env-file deploy/env/.env.example -f deploy/compose/compose.docker.yaml up -d --build
```

模板自带的 Compose 栈默认使用 `APP_DATASOURCE_MODE=SHARDING`，创建
`postgres-master-data`、`postgres-shard-0`、`postgres-shard-1` 三个 PostgreSQL
primary。模板不创建 replica；`SHARDING_READWRITE` 是为具备对应 primary/replica
端点的环境提供的代码与配置能力。

Podman 和 nerdctl 分别使用 `compose.podman.yaml` 和 `compose.nerdctl.yaml`。生产环境使用匹配的 `.prod.yaml` 文件和由运维方持有的 `.env.prod`。关于 rootless 前置条件、持久化、生产边界和数据删除警告，请参见 `deploy/container/README.md`。

根目录 `Jenkinsfile` 会运行测试，也可以发布不可变镜像。设置 `PUBLISH_IMAGE=true` 并提供 registry 参数即可发布；它不会执行部署。

使用通过 `EGON_CONFIG_DECRYPT_KEY` 或 `EGON_CONFIG_DECRYPT_KEY_FILE` 提供的 32 字节密钥加密配置值：

```bash
printf '%s' 'plain-text' | EGON_CONFIG_DECRYPT_KEY='replace-with-32-byte-secret-key' \
  ./mvnw -q -DskipTests compile exec:java \
  -Dexec.mainClass=top.egon.enjoyshop.goods.start.config.encryption.ConfigCipherCli
```

`ConfigCipherCli` 不接受任何参数，明文从标准输入读取。`exec:java` 在 Maven 自身的 JVM 中运行，
上面的管道才能送达；`spring-boot:run` 这类会派生子进程的运行方式无法给该 CLI 提供可用的标准输入。

将输出的 `ENC(v1:...)` 值写入配置。请通过环境变量、挂载文件、`config/application-secrets.yml` 或 `configtree:/run/secrets/` 提供真实密钥；不要提交凭据或解密密钥。

## 原生 RPC 与 Tianshu 配置

10 个 unary 操作定义于 `src/main/proto/teaching_user_facade.proto`，由四个具名 RPC Provider 实现。platform OpenAPI MVC starter 保留 `/v3/api-docs` 及既有业务 HTTP 访问。文档治理默认关闭；启用 `egon.cola.component.yuheng.openapi.enabled` 时，需要按平台合同补齐文档身份、发布分组和 JWT decoder，并使用 `yuheng.openapi.read` scope。每个发布 handler 还必须显式声明 `@Operation(operationId = "...")`；现有示例业务 Controller 需完成该编目后才能开启治理。

`dev` 和 `prod` 使用部署方已有的 Tianshu RPC 服务（`TIANSHU_RPC_TARGET`）和 Tianshu Redis 服务，还需提供注册 resource URI 及独立的 runtime/registry HMAC 凭据。RPC/Tianshu 配置前缀分别为 `egon.cola.component.rpc`、`egon.cola.component.tianshu`；应用身份由 `APP_NAME`、`APP_ENV`、`TIANSHU_BIZ_CODE`、`TIANSHU_APP_CODE`、`INSTANCE_ID` 指定。advertised host 必须能被消费者访问。本工程不附带 Tianshu 容器。`test` 关闭 RPC provider/consumer、Tianshu config/registry/Redis、HTTP 注册及文档发布。

生产环境启用 RPC 和 Tianshu mTLS：通过对应的 `RPC_*`、`TIANSHU_RPC_*` 变量传入证书链、私钥和信任证书路径，并将证书文件挂载到容器中的相同路径。开发环境显式允许明文。部署前填写 `deploy/env/.env.prod.example` 中的空白项，真实凭据通过环境变量或挂载文件提供。Compose 保留 PostgreSQL、Redis、RabbitMQ 及原有数据卷。静态和模块测试不能证明真实 Tianshu 注册、TLS 互通或容器就绪。

Tianshu Provider 和 HTTP 注册还需获取带 `tianshu:registration:write` 的 Tianquan-Shoubing SERVICE Token。填写 `.env` 样例中的 `TIANQUAN_SHOUBING_*` 字段，并配置 Spring OAuth2 Client 的 `tianshuregistration` registration/provider，使用 `client_credentials`、`client_secret_basic`、client ID/secret 和 token URI。Compose 已映射标准 Spring 环境变量；直接 Java 启动时，请通过外部配置提供 `spring.security.oauth2.client.registration.tianshuregistration` 及 `spring.security.oauth2.client.provider.tianshuregistration.token-uri`。app ID、resource server ID/URI、registration resource URI 必须与部署方的 Tianquan-Shoubing/Tianshu 保持一致。Tianquan-Shoubing 的 Servlet filter 自动注册关闭，以保留业务 HTTP 行为；显式启用后，平台文档安全链仍负责文档权限。

排除默认 Redisson 自动配置，由 Tianshu 管理显式配置的 Redis client；业务 Redis 保持 Spring 原有 connection factory。关闭 Tianshu 的测试环境不会因此自动建立 Redis 连接。

## Repository、CQE 与 PostgreSQL

本脚手架使用 MyBatis-Plus 3.5.16：Domain Service 保留业务语义，具体 Repository 继承 `EgonColaRepository`，Mapper 继承 `EgonColaMapper`；查询全部使用显式 XML。PO 继承 `EgonModel` 的 id、tenantId、创建/更新用户与时间、`LocalDateTime deletedAt`、`Long version`。活动行是 NULL，软删写 UTC 时间戳并递增版本。AR/QueryChain 不启用，技术元数据强制填充；枚举与字段 handler 遵循 Common 合同。

`APP_DATASOURCE_MODE` 支持 `SHARDING` 与 `SHARDING_READWRITE`，事务类型为 LOCAL。单表使用明确的 `!SINGLE group.schema.table`；广播表只读。默认 legacy tenant 路由保持原地址。可选两级模板见 `src/test/resources/sharding/two-level-readwrite.yml`（多模块项目在 infrastructure 中）：先按 tenant_id 散列到 tenant slot，再按业务根 ID 散列到 bucket。订单与明细分别使用 id/order_id 共享同一根语义；同租户固定在一个物理组。

算法固定为 mix64-v1，T/B 是不超过 1024 的二次幂，乘积不超过 4096。库间均衡还依赖均衡 slot map 和租户负载；没有自动重分布。Query 缺次级键/范围查询受 fanout 上限约束，Command 必须有精确键。修改分布配置需配套新建表/迁移设计，不能直接套用测试模板到已有业务库。

初始化由 组件统一管理数据源/拓扑与 `EgonColaPostgreDdlRunner` 受管 DDL，仅对物理 PRIMARY 执行 `db/egon-mp/V20260913_001__initialize_repository_schema.sql` 与 `repository-manifest.json`。空库首次初始化；受管库验证 checksum/前缀/路由指纹；非空未受管库报 REBUILD_REQUIRED。旧 B/V/manual SQL 原样保留作档案，不再作为本脚手架运行入口。

读写分离使用 PostgreSQL 自身复制，普通读走 ROUND_ROBIN 副本，事务读/锁定读/强制主库读走 PRIMARY；不自动创建副本或故障选主。单表、广播表及业务物理表均携带 tenant_id。跨物理组写入会拒绝并标记回滚。

每个实例配置唯一 `EGON_ID_MACHINE_ID`，生产使用现有 Common Snowflake。各 profile 保持同一 MP 配置键；dev 才开启诊断，原始 recorder logger 为 OFF。动态表名默认关闭，只接受明确映射；MybatisBatch 在调用方事务中执行。

默认测试使用隔离 H2 和受控依赖。真实路由测试需 `-Degon.pg.routing=true` 与 `EGON_TEST_PG_URL`；主从测试需 `-Degon.pg.readwrite=true` 与 `EGON_TEST_PG_PRIMARY_URL`、`EGON_TEST_PG_REPLICA_URL`，并提供专用 `EGON_TEST_PG_USER/PASSWORD`。它们只创建/清理自己的 UUID schema，不启动数据库。PG/SS 运行、迁移和性能 EXPLAIN 由使用者手动验收，跳过不表示通过。

## 二级缓存

`base`、`dev`、`prod` 均以 `egon.cola.component.cache.enabled=true` 交付；`test` 保留同样的键但设为 `enabled: false`，让单元与模块测试不依赖 Redis。`infrastructure/config/RedisConfig.java` 已带 `@EnableCaching`，配置好 Redis 连接即可直接使用二级缓存。mp-sd-ext 基类已移除缓存端口耦合，具体
Repository 通过 `@CacheConfig`、`@Cacheable`、`@CacheEvict` 等注解声明策略；已有 Repository 示例使用 `findCachedById` /
`updateCachedById`（Agent 按业务自行声明）。普通 CRUD
不再隐式失效缓存，其他写入和删除入口也须声明失效。所有 profile 都声明同一组五个 TTL 键（`l1-expire`、`l1-jitter`、`l2-expire`、`l2-jitter`、`null-expire`）以及共享的 `key-prefix`、`tenant-mdc-key` 与批量/锁预算，只有取值随环境不同。Key、条件、组合操作、事务与同步加载限制详见 [cache starter README](../../../egon-cola-components/egon-cola-component-common/egon-cola-component-common-cache-spring-boot-starter/README.md)。

## 2026-09-22 Java / CQE 维护规范

普通实体使用 class，仅不可变值对象可用 record。普通构造目标使用 @Builder，包含父类字段的构建使用兼容继承链的 @SuperBuilder。持久化枚举使用 @EnumValue，前端 JSON 使用 @JsonValue。必须复用 Components 和 Common MP Repository。校验基于原生/自定义约束、@Valid、@Validated 与分组，ValidationUtils 仅作通用手工触发。软删除业务唯一键必须组合业务列与 deleted_at，并验证有效行 NULL 语义。Event 经 Egon 事务 Outbox 或实际 MQ 投递，明确事务、失败和消费幂等。现有代码及旧 SQL 应按新规范逐项复核；本文档更新不代表已经迁移。


## 商品管理生命周期与部署验收

商品后端提供10个接口，使用已有Common Snowflake、MP Repository、具名Mapper与LOCAL事务；不包含前端、运费计算或图片删除。`spu`/`sku`/`category_brand`均为SINGLE/master_data，目录必须先存在同租户的品牌、三级分类与模板定义。

| 请求 | 权限 | 结果 |
| --- | --- | --- |
| GET /api/v1/products，GET /{spuId} | enjoyshop:product:read | 有效/回收站列表、聚合详情 |
| POST /api/v1/products，PUT /{spuId} | enjoyshop:product:write | 未审核/下架；编辑保留已有SKU ID，遗漏项记E |
| PUT /{spuId}/approval | enjoyshop:product:audit | 已审核并上架 |
| PUT /{spuId}/withdrawal、publication | enjoyshop:product:write | 下架、上架（后者必须已审核） |
| DELETE /{spuId}?version=... | enjoyshop:product:write | 下架商品及当前SKU软删为P |
| PUT /{spuId}/restoration | enjoyshop:product:write | 只还原P，保持未审核/下架；唯一键冲突全回滚 |
| DELETE /{spuId}/purge?version=... | enjoyshop:product:purge | 同事务清理父项与全部P/E历史；缺失父项成功 |

所有写操作除新增外提交父项当前`version`；状态PUT正文仅`{"version":0}`。旧版本409，包括重复目标；相同当前版本且状态已达目标不新增写。业务ID（含SKU、品牌、分类、模板）使用十进制字符串；价格为整数分，数量/重量为整数；禁止字符串数字/浮点价格或版本。禁用规格时恰好一个空规格SKU，启用时键值必须匹配模板定义。省略的可选集合默认为[]/{}，显式null仍按合同校验。货号trim且区分大小写，已有SPU/SKU货号不可修改。

列表默认pageNo=1/pageSize=10，最大100；deleted=true查回收站。名称为转义后的字面包含查询，货号精确查询；详情SKU稳定ID升序；列表不加载SKU。未知/重复query、未知body字段、非canonical ID均400/610201。401为身份失败；403/610206为业务权限拒绝；404统一不存在或越租户；409为版本/状态/业务键冲突；503为ID/授权不可用。过滤器403/503使用原双字段code/message，业务错误使用现有ResultRecord；OpenAPI声明两类形状，data.fieldErrors保留完整嵌套路径。

商品链为Shoubing→Jianshen→Anonymous，两个register-filter必须false；新链需要实际身份/授权beans，缺失启动失败。旧品牌/目录公开策略及上传仅身份策略保留。商品tenantId/userId来自Shoubing可信上下文和Jianshen绑定快照，忽略X-Tenant-Id/X-Actor-Id；不能把业务教学用户表或SCOPE权限当作商品审核/永删授权。

部署必须启用并配置现有Shoubing USER验证及其SERVICE OAuth2 client，填写issuer/JWK、服务client注册与密钥文件；平台注册系统`enjoyshop-goods`、资源`enjoyshop.product`及4个独立权限。JIANSHEN_AUTHORIZATION_ENDPOINT配置平台基地址；JIANSHEN_REDIS_ADDRESS/DATABASE/PASSWORD_FILE配置授权专用Redis；JIANSHEN_RESOURCE_URI与service-token.scopes必须匹配平台snapshot SERVICE授权。配置实际字段是runtime.database/password-file、String scopes；不能用未消费的token-endpoint/client-id字段代替Shoubing凭据。

授权cache-ttl=PT1S、near-cache/jitter=PT0S、fetch-timeout=PT1S；不承诺即时撤销或全链路1秒SLA。测试profile使用显式测试fakes，不能用于生产。API仅返回已提交事务，禁止失败后自动重试未知写结果。

运行环境需包含支持lifecycle.statement-policies的MP Starter5.4.1产物（组件修正7084c44ce）和具备VerifiedTenantContextFilter的Shoubing5.4.1产物；版本号相同不代表旧缓存包含能力。精确statement策略默认失败关闭：回收查询须ID/父键/分页约束，restore绑定原tombstone并仅清声明字段，purge需回收父项/version/FOR UPDATE/同组事务；不得关闭guard或忽略商品表。

新增DDL为V20261009_001__initialize_product_schema.sql，manifest追加校验和。旧版本不可变；只在MASTER_DATA建表，SHARD分支无商品表。默认应用测试不执行部署DDL；上线前由操作者核对目录数据/无冲突业务键、schema权限与分布式DDL记录，执行发布前备份和版本状态核查。新版本失败依据既有MP分布式状态重试，不能改旧SQL/checksum或使用repair。首次部署前回退代码与配置；执行DDL后保持新增表/历史，回退需前向兼容方案，不自动DROP。

执行验证使用模块目录 `./mvnw -o -B -ntp verify`，新测试为隔离H2/MockMvc与静态合同，未证明真实PostgreSQL/ShardingSphere或平台联调。发布前还需用户控制的PG并发新增/审核CAS/重复微秒软删/恢复键冲突/事务回滚/索引EXPLAIN、跨租户与平台4权限/资源绑定、Redis异常/超时/撤销验收，以及已有图片上传TEST-114。不会自动启动应用、数据库或容器。
