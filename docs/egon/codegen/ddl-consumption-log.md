# Classpath SQL 消费日志（代码生成器输入）

> 按 `backend-code-generation` 的 “Classpath SQL journal” 契约维护：生成器每次 `apply` 成功后追加一条，只追加、不改写或删除更早的条目。
>
> 这里记录的是**生成器作为输入消费的 classpath SQL**（`src/main/resources/db/` 下的受管脚本），
> 不是数据库执行记录，也不对应任何库的 `ddl_history`：写日志过程未连接 PostgreSQL、未执行任何 DDL。
> 真实执行由 `EgonColaPostgreDdlRunner` 在未来应用启动时完成（schema advisory lock、脚本与 `ddl_history` 同事务提交）。

每条记录按契约顺序给出：Asia/Shanghai 时间、profile、仓库相对输出根、本次消费到的最高脚本版本 `throughVersion`，
随后按 Manifest 顺序逐脚本给出：文件名版本令牌、仓库相对路径、文件字节 SHA-256、SQL 全文。

## 2026-09-24 15:40 Asia/Shanghai — profile light → enjoyshop-service/enjoyshop-service-goods

- 生成动作：`egon-codegen.sh plan`（`planId=2242002295b36e88fb4e4ba4dffefb69027f4e605d6f0251637978429d4af7fa`，22 个 ADD、零冲突、零破坏性操作）→ `egon-codegen.sh apply`（`status=APPLIED`，退出码 0）→ `./mvnw -q compile` 通过。
- 生成器配置：`enjoyshop-service/enjoyshop-service-goods/codegen/brand-codegen.json`（`input.mode=manifest`，直读 Manifest 并校验字节摘要，不复制 SQL 副本）。
- 输入 Manifest：`enjoyshop-service/enjoyshop-service-goods/src/main/resources/db/egon-mp/repository-manifest.json`（`family=light`，脚本 2 条）。
- logicalTables：`brand`；artifacts：po/dao/mapper-xml/repo/domain-model/domain-query/command/query/result/converter/domain-service/domain-impl/manage/manage-impl/controller。
- throughVersion: 20260923_001
- 说明：本次消费清单为 Manifest 全量（受管 DDL 历史不可改，生成器按 Manifest 顺序读取全部脚本后取 in-scope 表），故含历史脚本 `20260913_001` 与本轮新增脚本 `20260923_001`；两者均未在任何数据库执行过。

### 1.`20260913_001` — V20260913_001__initialize_repository_schema.sql

- version 令牌：`V20260913_001`／`20260913_001`
- 仓库相对路径：`enjoyshop-service/enjoyshop-service-goods/src/main/resources/db/egon-mp/V20260913_001__initialize_repository_schema.sql`
- SHA-256（文件字节）：`e4936cb2a7943d50cd4b7d7b49252d9712513a2f27ade70f125d2bf8f335ea31`
- Manifest 摘要一致性：OK（`e4936cb2a7943d50cd4b7d7b49252d9712513a2f27ade70f125d2bf8f335ea31`）
- SQL 全文：

```sql
-- Complete PostgreSQL initialization for the light archetype; only an empty managed target is eligible.
-- Tables retain existing business keys and foreign keys. IDs are provided by the application.
DO $egon$
BEGIN
    IF current_setting('egon_migration.role') = 'MASTER_DATA' THEN
        CREATE TABLE users (
            id BIGINT PRIMARY KEY,
            external_id VARCHAR(64) NOT NULL,
            name VARCHAR(120) NOT NULL,
            email VARCHAR(160) NOT NULL,
            status VARCHAR(32) NOT NULL,
            tenant_id BIGINT NOT NULL,
            create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
            version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
            CONSTRAINT uk_users_external_id UNIQUE (external_id),
            CONSTRAINT uk_users_email UNIQUE (email)
        );

        CREATE UNIQUE INDEX pk_users_id ON users (id);

        CREATE UNIQUE INDEX uk_users_tenant_external_active ON users (tenant_id, external_id) WHERE deleted_at IS NULL;

        CREATE UNIQUE INDEX uk_users_tenant_email_active ON users (tenant_id, email) WHERE deleted_at IS NULL;

        CREATE TABLE roles (
            code VARCHAR(64) PRIMARY KEY,
            name VARCHAR(120) NOT NULL,
            status VARCHAR(32) NOT NULL,
            id BIGINT NOT NULL,
            tenant_id BIGINT NOT NULL,
            create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
            version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0)
        );

        CREATE UNIQUE INDEX pk_roles_id ON roles (id);

        CREATE UNIQUE INDEX uk_roles_tenant_code_active ON roles (tenant_id, code) WHERE deleted_at IS NULL;

        CREATE TABLE permissions (
            code VARCHAR(128) PRIMARY KEY,
            name VARCHAR(120) NOT NULL,
            status VARCHAR(32) NOT NULL,
            id BIGINT NOT NULL,
            tenant_id BIGINT NOT NULL,
            create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
            version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0)
        );

        CREATE UNIQUE INDEX pk_permissions_id ON permissions (id);

        CREATE UNIQUE INDEX uk_permissions_tenant_code_active ON permissions (tenant_id, code) WHERE deleted_at IS NULL;

        CREATE TABLE user_roles (
            user_id BIGINT NOT NULL,
            role_code VARCHAR(64) NOT NULL,
            id BIGINT NOT NULL,
            tenant_id BIGINT NOT NULL,
            create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
            version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
            PRIMARY KEY (user_id, role_code),
            CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id),
            CONSTRAINT fk_user_roles_role FOREIGN KEY (role_code) REFERENCES roles (code)
        );

        CREATE UNIQUE INDEX pk_user_roles_id ON user_roles (id);

        CREATE UNIQUE INDEX uk_user_roles_tenant_member_active ON user_roles (tenant_id, user_id, role_code) WHERE deleted_at IS NULL;

        CREATE TABLE role_permissions (
            role_code VARCHAR(64) NOT NULL,
            permission_code VARCHAR(128) NOT NULL,
            id BIGINT NOT NULL,
            tenant_id BIGINT NOT NULL,
            create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
            version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
            PRIMARY KEY (role_code, permission_code),
            CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_code) REFERENCES roles (code),
            CONSTRAINT fk_role_permissions_permission
                FOREIGN KEY (permission_code) REFERENCES permissions (code)
        );

        CREATE UNIQUE INDEX pk_role_permissions_id ON role_permissions (id);

        CREATE UNIQUE INDEX uk_role_permissions_tenant_grant_active ON role_permissions (tenant_id, role_code, permission_code) WHERE deleted_at IS NULL;

        CREATE TABLE courses (
            id BIGINT PRIMARY KEY,
            course_code VARCHAR(64) NOT NULL,
            name VARCHAR(120) NOT NULL,
            status VARCHAR(32) NOT NULL,
            tenant_id BIGINT NOT NULL,
            create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
            version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
            CONSTRAINT uk_courses_code UNIQUE (course_code)
        );

        CREATE UNIQUE INDEX pk_courses_id ON courses (id);

        CREATE UNIQUE INDEX uk_courses_tenant_code_active ON courses (tenant_id, course_code) WHERE deleted_at IS NULL;

        INSERT INTO roles (code,name,status,id,tenant_id) VALUES ('STUDENT','Student','ACTIVE',1001,1);

        INSERT INTO permissions (code,name,status,id,tenant_id) VALUES ('CLASS_READ','Read class','ACTIVE',2001,1);
    ELSIF current_setting('egon_migration.role') = 'SHARD' THEN
        CREATE TABLE school_classes_0 (
            id BIGINT PRIMARY KEY,
            name VARCHAR(120) NOT NULL,
            semester VARCHAR(32) NOT NULL,
            status VARCHAR(32) NOT NULL,
            tenant_id BIGINT NOT NULL,
            create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
            version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0)
        );

        CREATE INDEX idx_school_classes_tenant_active_0 ON school_classes_0 (tenant_id, id);

        CREATE TABLE school_classes_1 (
            id BIGINT PRIMARY KEY,
            name VARCHAR(120) NOT NULL,
            semester VARCHAR(32) NOT NULL,
            status VARCHAR(32) NOT NULL,
            tenant_id BIGINT NOT NULL,
            create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
            version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0)
        );

        CREATE INDEX idx_school_classes_tenant_active_1 ON school_classes_1 (tenant_id, id);

        CREATE TABLE class_course_schedules_0 (
            id BIGINT PRIMARY KEY,
            school_class_id BIGINT NOT NULL,
            course_id BIGINT NOT NULL,
            starts_at TIMESTAMP NOT NULL,
            ends_at TIMESTAMP NOT NULL,
            tenant_id BIGINT NOT NULL,
            create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
            version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
            CONSTRAINT uk_class_course_start_0
                UNIQUE (school_class_id, course_id, starts_at),
            CONSTRAINT fk_schedule_class_0
                FOREIGN KEY (school_class_id) REFERENCES school_classes_0 (id)
        );

        CREATE UNIQUE INDEX uk_class_course_start_tenant_active_0 ON class_course_schedules_0 (tenant_id, school_class_id, course_id, starts_at) WHERE deleted_at IS NULL;

        CREATE TABLE class_course_schedules_1 (
            id BIGINT PRIMARY KEY,
            school_class_id BIGINT NOT NULL,
            course_id BIGINT NOT NULL,
            starts_at TIMESTAMP NOT NULL,
            ends_at TIMESTAMP NOT NULL,
            tenant_id BIGINT NOT NULL,
            create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
            version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
            CONSTRAINT uk_class_course_start_1
                UNIQUE (school_class_id, course_id, starts_at),
            CONSTRAINT fk_schedule_class_1
                FOREIGN KEY (school_class_id) REFERENCES school_classes_1 (id)
        );

        CREATE UNIQUE INDEX uk_class_course_start_tenant_active_1 ON class_course_schedules_1 (tenant_id, school_class_id, course_id, starts_at) WHERE deleted_at IS NULL;
    ELSE
        RAISE EXCEPTION 'Unknown managed DDL role';
    END IF;
END
$egon$;

CREATE TABLE ddl_history (
    tenant_id bigint NOT NULL DEFAULT 0 CHECK (tenant_id = 0),
    script varchar(500) NOT NULL,
    type varchar(30) NOT NULL DEFAULT 'SQL' CHECK (type = 'SQL'),
    version varchar(30) NOT NULL,
    checksum char(64) NOT NULL CHECK (checksum ~ '^[0-9a-f]{64}$'),
    installed_on timestamptz(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    execution_ms bigint NOT NULL CHECK (execution_ms >= 0),
    route_fingerprint char(64) NOT NULL CHECK (route_fingerprint ~ '^[0-9a-f]{64}$'),
    CONSTRAINT pk_ddl_history PRIMARY KEY (script, type),
    CONSTRAINT uk_ddl_history_type_version UNIQUE (type, version)
);
```

### 2.`20260923_001` — V20260923_001__initialize_goods_schema.sql

- version 令牌：`V20260923_001`／`20260923_001`
- 仓库相对路径：`enjoyshop-service/enjoyshop-service-goods/src/main/resources/db/egon-mp/V20260923_001__initialize_goods_schema.sql`
- SHA-256（文件字节）：`1904922e16dc9d2c2d56afb8a9cafb3fff2e4291cf20774f47a64a2ab5e44a17`
- Manifest 摘要一致性：OK（`1904922e16dc9d2c2d56afb8a9cafb3fff2e4291cf20774f47a64a2ab5e44a17`）
- SQL 全文：

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
        -- 业务唯一键组合业务列与 deleted_at（Spec Rule 11／REQ-017）：生命周期键 + 有效行部分唯一键成对出现，
        -- 与生成器先例 ddl/schema.sql 的 orders_lifecycle 加 orders_active 同形。deleted_at 为 NULL 时
        -- PostgreSQL 唯一索引视 NULL 互异，故本键不约束有效行，删除后同名重建仍可行。
        CREATE UNIQUE INDEX uk_brand_tenant_name_lifecycle ON brand (tenant_id, name, deleted_at);
        CREATE UNIQUE INDEX uk_brand_tenant_name_active ON brand (tenant_id, name) WHERE deleted_at IS NULL;
        CREATE INDEX idx_brand_tenant_seq_id ON brand (tenant_id, seq DESC, id DESC);
    ELSIF current_setting('egon_migration.role') = 'SHARD' THEN
        -- brand 为 SINGLE 主数据表，分片目标不承载该表，故本分支为空语句表：PL/pgSQL 的 proc_sect 允许为空，
        -- 其运行期行为与原先的 NULL 占位语句完全一致（角色命中即直通，不落入 ELSE 的 RAISE EXCEPTION）。
        -- 不再写 NULL 是因为代码生成器按分号切分脚本后把每段交 SQL 解析器，NULL 不是语句会被判为 UNSUPPORTED_DDL。
    ELSE
        RAISE EXCEPTION 'Unknown managed DDL role';
    END IF;
END
$egon$;
```

