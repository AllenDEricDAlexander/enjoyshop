# EnjoyShop 前端工作台

独立静态前端，覆盖当前 goods 工程 11 个 REST Controller 的 34 个业务操作。采用原生 ES Modules、Hash 路由和共享请求/表单组件，无第三方运行依赖，不由 Java 服务托管，不加入 Maven reactor。管理工作台提供商品、内容运营、用户权限与教学页面；商城搜索和广告展示分别展示真实公开接口的结果。

## 检查与构建

需要 Node.js 20 或更新版本。没有依赖安装步骤。

```bash
cd enjoyshop-web
npm run check
npm test
npm run build
```

产物位于 `dist/`：HTML、可编辑的 `config.js` 和带内容 hash 的 JS/CSS。构建不启动服务、不打开浏览器。源码也可由已有静态服务器直接提供；不能通过 `file://` 加载 ES Modules。Hash 路由无需 SPA 路径回退。

## 接入环境

修改源码 `config.js` 后重新构建，或修改产物 `dist/config.js`：

- `apiBase`：默认空字符串，使用同源 `/api` 和 `/bff`。如需直连开发接口，填写真实 HTTP(S) 地址；明文 HTTP 仅允许 localhost/loopback。
- `authMode`：默认 `bff`。通过平台会话与 TokenRelay 访问业务接口；写操作先读取 `/bff/csrf`，按返回的 headerName/token 提交。
- `loginRegistrationId`：填写实际 Edge 的 OIDC registration ID，登录按钮进入 `/oauth2/authorization/{registrationId}`。默认空，不虚构身份配置。
- `adPosition`：默认使用当前后端配置的 `web_index_lb`，页面可输入其他真实配置的广告位。

“连接与登录”支持临时切换连接和开发 Bearer 凭据。凭据仅保留在当前文档内存中，不写 localStorage/sessionStorage、URL 或日志，刷新后清除。开发模式发送 Authorization，不发送 Cookie；BFF 模式使用 Cookie，不在浏览器保存访问 token。服务端仍负责每次操作的权限判定。

旧品牌/规格/用户/教学接口使用已有上下文头。开发 Bearer 模式可填写旧接口的租户 ID 和操作人 ID，分别发送 `X-Tenant-Id`、写操作的 `X-Actor-Id` 与 `X-Operator-Id`；这些值不会发送到商品、广告、搜索或公开广告接口。BFF 模式须由实际网关提供旧接口所需的可信上下文；前端不会用会话 subject 猜测数值操作人 ID。当前 goods CORS 仅允许 Authorization/Content-Type，旧上下文头的开发调试需同源代理或另行配置经过审核的 CORS，前端未修改后端 CORS。

部署时静态站点与同源 API 路由由已有 Gateway/Edge/Biz 拥有。独立静态源站只提供本目录产物，不把 `/api`、`/bff` 或 OIDC 路径回退成 HTML，不改写 API path/query，也不自动重试。`index.html` 和 `config.js` 推荐 no-cache；带 hash 的资源可设 immutable，并保留上一版本资源以供回滚。若配置 CSP，脚本/样式需允许本站点，connect-src 和 img-src 需允许实际接口/图片源；没有外部字体或 CDN 脚本依赖。

## 页面与接口覆盖

| 方法与路径 | 页面及交互 |
| --- | --- |
| GET `/api/v1/products` | 商品管理、回收站：筛选与分页 |
| GET `/api/v1/products/{spuId}` | 商品详情及编辑前读取，支持 deleted |
| POST `/api/v1/products` | 新建 SPU/SKU 聚合 |
| PUT `/api/v1/products/{spuId}` | 编辑下架商品、保留 SKU ID，提交读取版本 |
| PUT `/api/v1/products/{spuId}/approval` | 审核通过并上架 |
| PUT `/api/v1/products/{spuId}/withdrawal` | 下架 |
| PUT `/api/v1/products/{spuId}/publication` | 上架已审核商品 |
| PUT `/api/v1/products/{spuId}/restoration` | 回收站还原 |
| DELETE `/api/v1/products/{spuId}` | 移入回收站 |
| DELETE `/api/v1/products/{spuId}/purge` | 永久删除，操作前确认 |
| GET `/api/v1/brands` | 品牌筛选及分页：名称、首字母、分类、排序范围 |
| GET `/api/v1/brands/{brandId}` | 品牌详情、编辑和删除前读取 |
| POST `/api/v1/brands` | 新建品牌 |
| PUT `/api/v1/brands/{brandId}` | 完整替换可编辑字段，回传 version |
| DELETE `/api/v1/brands/{brandId}` | 删除品牌，回传 version |
| GET `/api/v1/specs/category/{categoryName}` | 分类规格查询 |
| POST `/api/v1/files` | 图片上传页及商品/SKU/品牌/广告表单上传 |
| GET `/api/v1/ads` | 广告列表，按广告位/状态筛选与分页 |
| GET `/api/v1/ads/{adId}` | 广告详情、编辑和删除前读取 |
| POST `/api/v1/ads` | 新建广告 |
| PUT `/api/v1/ads/{adId}` | 编辑广告，回传 version |
| DELETE `/api/v1/ads/{adId}` | 删除广告，回传 version |
| GET `/api/v1/ad-slots/{position}/ads` | 广告展示，选择广告位和条数 |
| GET `/api/v1/product-search` | 商城 SKU 搜索：价格、排序、品牌/分类及重复 spec 筛选 |
| POST `/api/users` | 创建用户 |
| GET `/api/users/{userId}` | 用户 ID 查询 |
| POST `/api/users/{userId}/roles` | 分配已有角色编码 |
| POST `/api/roles/{roleCode}/permissions` | 向已有角色授予权限编码 |
| GET `/api/users/{userId}/permissions` | 权限树查询 |
| POST `/api/courses` | 创建课程 |
| GET `/api/courses/{courseId}` | 课程 ID 查询 |
| POST `/api/school-classes` | 创建班级 |
| GET `/api/school-classes/{schoolClassId}` | 班级 ID 查询 |
| POST `/api/school-classes/{schoolClassId}/courses/{courseId}/schedule` | 排课，保留 LocalDateTime |

另外提供工作概览、连接配置，以及已有平台 `/bff/session`、`/bff/csrf`、`/bff/logout` 的交互。用户、课程、班级接口没有集合查询/更新/删除能力，页面按实际能力提供创建和 ID 查询；角色与权限使用已有编码，不提供虚构的角色/权限创建接口。后端没有分类树或模板查询接口，商品表单保留明确的关联 ID 输入；分类规格页面提供现有查询能力。CDC、RPC、缓存等后台机制由服务端负责。

## 关键约束与验收

- 商品只提交后端允许编辑的字段。编辑回传当前版本和现有 SKU ID；未启用规格时必须为一个 SKU，启用后检查选项、唯一条码/规格组合。价格为整数分，重量为克。编辑后由后端重新审核。
- 所有列表和详情内容按文本转义，商品介绍作为文本展示；搜索用普通商品名称，不注入 nameHighlight。图片只加载 HTTPS，链接只允许 HTTPS 或站内绝对路径。
- 品牌的 JSON Long ID 在解析前保护精度；版本以整数原样提交。401/403/409/413/503 和字段错误保留服务端信息，不把失败显示为空成功。
- 图片检查 JPEG/PNG 签名及 10 MiB 限制，用 multipart 的 `file` 字段提交。写入期间禁用重复提交；网络结果未知时提示先查询确认，不自动重试。
- 搜索页处理候选截断及 maxAccessiblePageNo，重复 `spec.<name>` 仅编码一次。默认搜索引擎尚未接入会返回 503；公开广告默认关闭会返回拒绝。需按现有后端配置接入后才能运行验收。
- 静态检查、Node 测试和构建可离线完成。浏览器布局/交互、真实 OIDC/BFF/权限、旧接口网关上下文、对象存储上传及数据库联调仍需用户启动实际环境后验收；本任务未启动应用或浏览器。

建议运行验收：配置连接 → 创建/编辑品牌 → 查询分类规格 → 上传图片 → 创建商品与多个 SKU → 审核上架 → 搜索（引擎接入后）→ 下架编辑/重新审核 → 回收/还原/永久删除 → 广告时间窗口与公开展示（启用后）→ 用户/角色/权限 → 课程/班级/排课。另用两个会话验证版本冲突、无权限身份、失效会话、超限文件和网络中断。
