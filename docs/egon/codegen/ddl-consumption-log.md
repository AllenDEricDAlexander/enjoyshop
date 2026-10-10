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


## 2026-09-29 23:04 Asia/Shanghai — profile light → enjoyshop-service/enjoyshop-service-goods

- 生成动作：`egon-codegen.sh plan`（1 个 ADD、零冲突、零破坏性操作）→ `apply`（`status=APPLIED`，退出码 0）→ `check`（`status=CLEAN`，退出码 0）。
- 生成器配置：`enjoyshop-service/enjoyshop-service-goods/codegen/catalog-lookup-codegen.json`（`input.mode=manifest`，`logicalTables=[spec]`，`artifacts=[po]`）。
- 输入 Manifest：`enjoyshop-service/enjoyshop-service-goods/src/main/resources/db/egon-mp/repository-manifest.json`（`family=light`，脚本 3 条）。
- 输出根目录：`enjoyshop-service/enjoyshop-service-goods`。
- throughVersion: 20260929_001
- 说明：本次消费清单为 Manifest 全量；日志记录 classpath SQL 输入，不代表数据库执行。

### 1. `20260913_001` — V20260913_001__initialize_repository_schema.sql

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

### 2. `20260923_001` — V20260923_001__initialize_goods_schema.sql

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

### 3. `20260929_001` — V20260929_001__initialize_catalog_lookup_schema.sql

- version 令牌：`V20260929_001`／`20260929_001`
- 仓库相对路径：`enjoyshop-service/enjoyshop-service-goods/src/main/resources/db/egon-mp/V20260929_001__initialize_catalog_lookup_schema.sql`
- SHA-256（文件字节）：`6db577031d1adb40dc83ca5c187345c3ad0cb94d6e446a877163e5755280bd83`
- Manifest 摘要一致性：OK（`6db577031d1adb40dc83ca5c187345c3ad0cb94d6e446a877163e5755280bd83`）
- SQL 全文：

```sql
-- 变更内容：新增商品目录读取所需的分类、模板、规格、参数和分类品牌关联表。
-- 影响范围：仅 master_data 角色；现有 brand 表及其数据保持不变。
-- 兼容性说明：新增表，不回填或写入目录数据；ACTOR-003 负责后续目录写入。
DO $egon$
BEGIN
    IF current_setting('egon_migration.role') = 'MASTER_DATA' THEN
        CREATE TABLE template (
            id BIGINT PRIMARY KEY,
            tenant_id BIGINT NOT NULL,
            create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
            version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
            name VARCHAR(120) NOT NULL,
            spec_num INTEGER NOT NULL DEFAULT 0 CHECK (spec_num >= 0),
            para_num INTEGER NOT NULL DEFAULT 0 CHECK (para_num >= 0)
        );

        CREATE TABLE category (
            id BIGINT PRIMARY KEY,
            tenant_id BIGINT NOT NULL,
            create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
            version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
            name VARCHAR(120) NOT NULL,
            goods_num INTEGER NOT NULL DEFAULT 0 CHECK (goods_num >= 0),
            is_show CHAR(1) NOT NULL DEFAULT '1' CHECK (is_show IN ('0', '1')),
            is_menu CHAR(1) NOT NULL DEFAULT '0' CHECK (is_menu IN ('0', '1')),
            seq INTEGER NOT NULL DEFAULT 0 CHECK (seq >= 0),
            parent_id BIGINT,
            template_id BIGINT,
            CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES category (id) ON DELETE RESTRICT,
            CONSTRAINT fk_category_template FOREIGN KEY (template_id) REFERENCES template (id) ON DELETE RESTRICT
        );

        CREATE TABLE spec (
            id BIGINT PRIMARY KEY,
            tenant_id BIGINT NOT NULL,
            create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
            version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
            name VARCHAR(120) NOT NULL,
            options TEXT NOT NULL,
            seq INTEGER NOT NULL DEFAULT 0 CHECK (seq >= 0),
            template_id BIGINT NOT NULL,
            CONSTRAINT fk_spec_template FOREIGN KEY (template_id) REFERENCES template (id) ON DELETE RESTRICT
        );

        CREATE TABLE para (
            id BIGINT PRIMARY KEY,
            tenant_id BIGINT NOT NULL,
            create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
            version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
            name VARCHAR(120) NOT NULL,
            options TEXT NOT NULL,
            seq INTEGER NOT NULL DEFAULT 0 CHECK (seq >= 0),
            template_id BIGINT NOT NULL,
            CONSTRAINT fk_para_template FOREIGN KEY (template_id) REFERENCES template (id) ON DELETE RESTRICT
        );

        CREATE TABLE category_brand (
            id BIGINT PRIMARY KEY,
            tenant_id BIGINT NOT NULL,
            create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
            version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
            category_id BIGINT NOT NULL,
            brand_id BIGINT NOT NULL,
            CONSTRAINT fk_category_brand_category FOREIGN KEY (category_id) REFERENCES category (id) ON DELETE RESTRICT,
            CONSTRAINT fk_category_brand_brand FOREIGN KEY (brand_id) REFERENCES brand (id) ON DELETE RESTRICT
        );

        CREATE INDEX idx_category_tenant_name_active
            ON category (tenant_id, name, id)
            WHERE deleted_at IS NULL;
        CREATE INDEX idx_spec_tenant_template_seq_id_active
            ON spec (tenant_id, template_id, seq, id)
            WHERE deleted_at IS NULL;
        CREATE UNIQUE INDEX uk_category_brand_lifecycle
            ON category_brand (tenant_id, category_id, brand_id, deleted_at);
        CREATE UNIQUE INDEX uk_category_brand_active
            ON category_brand (tenant_id, category_id, brand_id)
            WHERE deleted_at IS NULL;
    ELSIF current_setting('egon_migration.role') = 'SHARD' THEN
        -- 所有目录表均为 SINGLE/master_data，此分支不建表。
    ELSE
        RAISE EXCEPTION 'Unknown managed DDL role';
    END IF;
END
$egon$;

```

## 2026-10-09 12:29:38 Asia/Shanghai — product-persistence

profile: light

outputRoot: enjoyshop-service/enjoyshop-service-goods

throughVersion: 20261009_001

planId: 0c7257d9943daa3f4db95084dc05f8e079e9dd0f4372f5d316e1e48de6867c65

Outcome: generator apply; SQL input only, no database execution.

### 20260913_001

Path: enjoyshop-service/enjoyshop-service-goods/src/main/resources/db/egon-mp/V20260913_001__initialize_repository_schema.sql

SHA-256: e4936cb2a7943d50cd4b7d7b49252d9712513a2f27ade70f125d2bf8f335ea31

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

### 20260923_001

Path: enjoyshop-service/enjoyshop-service-goods/src/main/resources/db/egon-mp/V20260923_001__initialize_goods_schema.sql

SHA-256: 1904922e16dc9d2c2d56afb8a9cafb3fff2e4291cf20774f47a64a2ab5e44a17

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

### 20260929_001

Path: enjoyshop-service/enjoyshop-service-goods/src/main/resources/db/egon-mp/V20260929_001__initialize_catalog_lookup_schema.sql

SHA-256: 6db577031d1adb40dc83ca5c187345c3ad0cb94d6e446a877163e5755280bd83

```sql
-- 变更内容：新增商品目录读取所需的分类、模板、规格、参数和分类品牌关联表。
-- 影响范围：仅 master_data 角色；现有 brand 表及其数据保持不变。
-- 兼容性说明：新增表，不回填或写入目录数据；ACTOR-003 负责后续目录写入。
DO $egon$
BEGIN
    IF current_setting('egon_migration.role') = 'MASTER_DATA' THEN
        CREATE TABLE template (
            id BIGINT PRIMARY KEY,
            tenant_id BIGINT NOT NULL,
            create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
            version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
            name VARCHAR(120) NOT NULL,
            spec_num INTEGER NOT NULL DEFAULT 0 CHECK (spec_num >= 0),
            para_num INTEGER NOT NULL DEFAULT 0 CHECK (para_num >= 0)
        );

        CREATE TABLE category (
            id BIGINT PRIMARY KEY,
            tenant_id BIGINT NOT NULL,
            create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
            version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
            name VARCHAR(120) NOT NULL,
            goods_num INTEGER NOT NULL DEFAULT 0 CHECK (goods_num >= 0),
            is_show CHAR(1) NOT NULL DEFAULT '1' CHECK (is_show IN ('0', '1')),
            is_menu CHAR(1) NOT NULL DEFAULT '0' CHECK (is_menu IN ('0', '1')),
            seq INTEGER NOT NULL DEFAULT 0 CHECK (seq >= 0),
            parent_id BIGINT,
            template_id BIGINT,
            CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES category (id) ON DELETE RESTRICT,
            CONSTRAINT fk_category_template FOREIGN KEY (template_id) REFERENCES template (id) ON DELETE RESTRICT
        );

        CREATE TABLE spec (
            id BIGINT PRIMARY KEY,
            tenant_id BIGINT NOT NULL,
            create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
            version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
            name VARCHAR(120) NOT NULL,
            options TEXT NOT NULL,
            seq INTEGER NOT NULL DEFAULT 0 CHECK (seq >= 0),
            template_id BIGINT NOT NULL,
            CONSTRAINT fk_spec_template FOREIGN KEY (template_id) REFERENCES template (id) ON DELETE RESTRICT
        );

        CREATE TABLE para (
            id BIGINT PRIMARY KEY,
            tenant_id BIGINT NOT NULL,
            create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
            version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
            name VARCHAR(120) NOT NULL,
            options TEXT NOT NULL,
            seq INTEGER NOT NULL DEFAULT 0 CHECK (seq >= 0),
            template_id BIGINT NOT NULL,
            CONSTRAINT fk_para_template FOREIGN KEY (template_id) REFERENCES template (id) ON DELETE RESTRICT
        );

        CREATE TABLE category_brand (
            id BIGINT PRIMARY KEY,
            tenant_id BIGINT NOT NULL,
            create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
            version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
            category_id BIGINT NOT NULL,
            brand_id BIGINT NOT NULL,
            CONSTRAINT fk_category_brand_category FOREIGN KEY (category_id) REFERENCES category (id) ON DELETE RESTRICT,
            CONSTRAINT fk_category_brand_brand FOREIGN KEY (brand_id) REFERENCES brand (id) ON DELETE RESTRICT
        );

        CREATE INDEX idx_category_tenant_name_active
            ON category (tenant_id, name, id)
            WHERE deleted_at IS NULL;
        CREATE INDEX idx_spec_tenant_template_seq_id_active
            ON spec (tenant_id, template_id, seq, id)
            WHERE deleted_at IS NULL;
        CREATE UNIQUE INDEX uk_category_brand_lifecycle
            ON category_brand (tenant_id, category_id, brand_id, deleted_at);
        CREATE UNIQUE INDEX uk_category_brand_active
            ON category_brand (tenant_id, category_id, brand_id)
            WHERE deleted_at IS NULL;
    ELSIF current_setting('egon_migration.role') = 'SHARD' THEN
        -- 所有目录表均为 SINGLE/master_data，此分支不建表。
    ELSE
        RAISE EXCEPTION 'Unknown managed DDL role';
    END IF;
END
$egon$;
```

### 20261009_001

Path: enjoyshop-service/enjoyshop-service-goods/src/main/resources/db/egon-mp/V20261009_001__initialize_product_schema.sql

SHA-256: db319455730c9d20b96401bbd7cdf0f488b23f8fa4efa15ccc4e1985e167c24c

```sql
-- 商品聚合：spu/sku 与目录关联均由 master_data 承载，同组 LOCAL 事务。
-- 仅新增商品表；目录表、历史迁移及其校验和保持不变。
DO $egon$
BEGIN
    IF current_setting('egon_migration.role') = 'MASTER_DATA' THEN
        CREATE TABLE spu (
            id BIGINT PRIMARY KEY,
            tenant_id BIGINT NOT NULL,
            create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
            version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
            sn VARCHAR(64) NOT NULL,
            name VARCHAR(200) NOT NULL,
            caption VARCHAR(300) NOT NULL DEFAULT '',
            brand_id BIGINT NOT NULL,
            category1_id BIGINT NOT NULL,
            category2_id BIGINT NOT NULL,
            category3_id BIGINT NOT NULL,
            template_id BIGINT NOT NULL,
            freight_id BIGINT NOT NULL DEFAULT 0 CHECK (freight_id >= 0),
            image VARCHAR(500) NOT NULL DEFAULT '',
            images TEXT NOT NULL DEFAULT '[]',
            sale_service TEXT NOT NULL DEFAULT '[]',
            introduction TEXT NOT NULL DEFAULT '',
            spec_items TEXT NOT NULL DEFAULT '{}',
            para_items TEXT NOT NULL DEFAULT '{}',
            sale_num INTEGER NOT NULL DEFAULT 0 CHECK (sale_num >= 0),
            comment_num INTEGER NOT NULL DEFAULT 0 CHECK (comment_num >= 0),
            status CHAR(1) NOT NULL DEFAULT '0' CHECK (status IN ('0', '1')),
            is_marketable BOOLEAN NOT NULL DEFAULT FALSE,
            is_enable_spec BOOLEAN NOT NULL,
            CONSTRAINT fk_spu_brand FOREIGN KEY (brand_id) REFERENCES brand (id) ON DELETE RESTRICT,
            CONSTRAINT fk_spu_category1 FOREIGN KEY (category1_id) REFERENCES category (id) ON DELETE RESTRICT,
            CONSTRAINT fk_spu_category2 FOREIGN KEY (category2_id) REFERENCES category (id) ON DELETE RESTRICT,
            CONSTRAINT fk_spu_category3 FOREIGN KEY (category3_id) REFERENCES category (id) ON DELETE RESTRICT,
            CONSTRAINT fk_spu_template FOREIGN KEY (template_id) REFERENCES template (id) ON DELETE RESTRICT,
            CONSTRAINT ck_spu_publication CHECK (NOT is_marketable OR (status = '1' AND deleted_at IS NULL)),
            CONSTRAINT ck_spu_recycled CHECK (deleted_at IS NULL OR (status = '0' AND NOT is_marketable))
        );

        CREATE TABLE sku (
            id BIGINT PRIMARY KEY,
            tenant_id BIGINT NOT NULL,
            create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
            version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
            spu_id BIGINT NOT NULL,
            sn VARCHAR(64) NOT NULL,
            name VARCHAR(3000) NOT NULL,
            price INTEGER NOT NULL CHECK (price >= 1),
            num INTEGER NOT NULL CHECK (num >= 0),
            alert_num INTEGER NOT NULL CHECK (alert_num >= 0),
            weight INTEGER NOT NULL CHECK (weight >= 0),
            image VARCHAR(500) NOT NULL DEFAULT '',
            images TEXT NOT NULL DEFAULT '[]',
            spec TEXT NOT NULL DEFAULT '{}',
            category_id BIGINT NOT NULL,
            category_name VARCHAR(120) NOT NULL,
            brand_name VARCHAR(120) NOT NULL,
            sale_num INTEGER NOT NULL DEFAULT 0 CHECK (sale_num >= 0),
            comment_num INTEGER NOT NULL DEFAULT 0 CHECK (comment_num >= 0),
            status CHAR(1) NOT NULL DEFAULT '1' CHECK (status IN ('1', '2')),
            deletion_kind CHAR(1),
            CONSTRAINT fk_sku_spu FOREIGN KEY (spu_id) REFERENCES spu (id) ON DELETE RESTRICT,
            CONSTRAINT fk_sku_category FOREIGN KEY (category_id) REFERENCES category (id) ON DELETE RESTRICT,
            CONSTRAINT ck_sku_deletion CHECK (
                (deleted_at IS NULL AND deletion_kind IS NULL)
                OR (deleted_at IS NOT NULL AND deletion_kind IS NOT NULL AND deletion_kind IN ('P', 'E'))
            )
        );

        -- PostgreSQL NULL 不参与普通唯一比较：生命周期键与有效行唯一保护必须成对存在。
        CREATE UNIQUE INDEX uk_spu_lifecycle ON spu (tenant_id, sn, deleted_at);
        CREATE UNIQUE INDEX uk_spu_active ON spu (tenant_id, sn) WHERE deleted_at IS NULL;
        CREATE INDEX idx_spu_active_page ON spu (tenant_id, id DESC) WHERE deleted_at IS NULL;
        CREATE INDEX idx_spu_deleted_page ON spu (tenant_id, id DESC) WHERE deleted_at IS NOT NULL;
        CREATE UNIQUE INDEX uk_sku_lifecycle ON sku (tenant_id, sn, deleted_at);
        CREATE UNIQUE INDEX uk_sku_active ON sku (tenant_id, sn) WHERE deleted_at IS NULL;
        CREATE INDEX idx_sku_parent ON sku (tenant_id, spu_id, id);
    ELSIF current_setting('egon_migration.role') = 'SHARD' THEN
        -- 新商品表为 SINGLE/master_data，分片目标不创建这些表。
    ELSE
        RAISE EXCEPTION 'Unknown managed DDL role';
    END IF;
END
$egon$;
```

## 2026-10-09 12:30:25 Asia/Shanghai — product-spu-model

profile: light

outputRoot: enjoyshop-service/enjoyshop-service-goods

throughVersion: 20261009_001

planId: 0ddc78e1e907f73ed9bd7ba54b1fdbb6ad3919044aab99e851ead6ec01c80bec

Outcome: generator apply; SQL input only, no database execution.

### 20260913_001

Path: enjoyshop-service/enjoyshop-service-goods/src/main/resources/db/egon-mp/V20260913_001__initialize_repository_schema.sql

SHA-256: e4936cb2a7943d50cd4b7d7b49252d9712513a2f27ade70f125d2bf8f335ea31

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

### 20260923_001

Path: enjoyshop-service/enjoyshop-service-goods/src/main/resources/db/egon-mp/V20260923_001__initialize_goods_schema.sql

SHA-256: 1904922e16dc9d2c2d56afb8a9cafb3fff2e4291cf20774f47a64a2ab5e44a17

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

### 20260929_001

Path: enjoyshop-service/enjoyshop-service-goods/src/main/resources/db/egon-mp/V20260929_001__initialize_catalog_lookup_schema.sql

SHA-256: 6db577031d1adb40dc83ca5c187345c3ad0cb94d6e446a877163e5755280bd83

```sql
-- 变更内容：新增商品目录读取所需的分类、模板、规格、参数和分类品牌关联表。
-- 影响范围：仅 master_data 角色；现有 brand 表及其数据保持不变。
-- 兼容性说明：新增表，不回填或写入目录数据；ACTOR-003 负责后续目录写入。
DO $egon$
BEGIN
    IF current_setting('egon_migration.role') = 'MASTER_DATA' THEN
        CREATE TABLE template (
            id BIGINT PRIMARY KEY,
            tenant_id BIGINT NOT NULL,
            create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
            version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
            name VARCHAR(120) NOT NULL,
            spec_num INTEGER NOT NULL DEFAULT 0 CHECK (spec_num >= 0),
            para_num INTEGER NOT NULL DEFAULT 0 CHECK (para_num >= 0)
        );

        CREATE TABLE category (
            id BIGINT PRIMARY KEY,
            tenant_id BIGINT NOT NULL,
            create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
            version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
            name VARCHAR(120) NOT NULL,
            goods_num INTEGER NOT NULL DEFAULT 0 CHECK (goods_num >= 0),
            is_show CHAR(1) NOT NULL DEFAULT '1' CHECK (is_show IN ('0', '1')),
            is_menu CHAR(1) NOT NULL DEFAULT '0' CHECK (is_menu IN ('0', '1')),
            seq INTEGER NOT NULL DEFAULT 0 CHECK (seq >= 0),
            parent_id BIGINT,
            template_id BIGINT,
            CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES category (id) ON DELETE RESTRICT,
            CONSTRAINT fk_category_template FOREIGN KEY (template_id) REFERENCES template (id) ON DELETE RESTRICT
        );

        CREATE TABLE spec (
            id BIGINT PRIMARY KEY,
            tenant_id BIGINT NOT NULL,
            create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
            version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
            name VARCHAR(120) NOT NULL,
            options TEXT NOT NULL,
            seq INTEGER NOT NULL DEFAULT 0 CHECK (seq >= 0),
            template_id BIGINT NOT NULL,
            CONSTRAINT fk_spec_template FOREIGN KEY (template_id) REFERENCES template (id) ON DELETE RESTRICT
        );

        CREATE TABLE para (
            id BIGINT PRIMARY KEY,
            tenant_id BIGINT NOT NULL,
            create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
            version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
            name VARCHAR(120) NOT NULL,
            options TEXT NOT NULL,
            seq INTEGER NOT NULL DEFAULT 0 CHECK (seq >= 0),
            template_id BIGINT NOT NULL,
            CONSTRAINT fk_para_template FOREIGN KEY (template_id) REFERENCES template (id) ON DELETE RESTRICT
        );

        CREATE TABLE category_brand (
            id BIGINT PRIMARY KEY,
            tenant_id BIGINT NOT NULL,
            create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
            version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
            category_id BIGINT NOT NULL,
            brand_id BIGINT NOT NULL,
            CONSTRAINT fk_category_brand_category FOREIGN KEY (category_id) REFERENCES category (id) ON DELETE RESTRICT,
            CONSTRAINT fk_category_brand_brand FOREIGN KEY (brand_id) REFERENCES brand (id) ON DELETE RESTRICT
        );

        CREATE INDEX idx_category_tenant_name_active
            ON category (tenant_id, name, id)
            WHERE deleted_at IS NULL;
        CREATE INDEX idx_spec_tenant_template_seq_id_active
            ON spec (tenant_id, template_id, seq, id)
            WHERE deleted_at IS NULL;
        CREATE UNIQUE INDEX uk_category_brand_lifecycle
            ON category_brand (tenant_id, category_id, brand_id, deleted_at);
        CREATE UNIQUE INDEX uk_category_brand_active
            ON category_brand (tenant_id, category_id, brand_id)
            WHERE deleted_at IS NULL;
    ELSIF current_setting('egon_migration.role') = 'SHARD' THEN
        -- 所有目录表均为 SINGLE/master_data，此分支不建表。
    ELSE
        RAISE EXCEPTION 'Unknown managed DDL role';
    END IF;
END
$egon$;
```

### 20261009_001

Path: enjoyshop-service/enjoyshop-service-goods/src/main/resources/db/egon-mp/V20261009_001__initialize_product_schema.sql

SHA-256: db319455730c9d20b96401bbd7cdf0f488b23f8fa4efa15ccc4e1985e167c24c

```sql
-- 商品聚合：spu/sku 与目录关联均由 master_data 承载，同组 LOCAL 事务。
-- 仅新增商品表；目录表、历史迁移及其校验和保持不变。
DO $egon$
BEGIN
    IF current_setting('egon_migration.role') = 'MASTER_DATA' THEN
        CREATE TABLE spu (
            id BIGINT PRIMARY KEY,
            tenant_id BIGINT NOT NULL,
            create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
            version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
            sn VARCHAR(64) NOT NULL,
            name VARCHAR(200) NOT NULL,
            caption VARCHAR(300) NOT NULL DEFAULT '',
            brand_id BIGINT NOT NULL,
            category1_id BIGINT NOT NULL,
            category2_id BIGINT NOT NULL,
            category3_id BIGINT NOT NULL,
            template_id BIGINT NOT NULL,
            freight_id BIGINT NOT NULL DEFAULT 0 CHECK (freight_id >= 0),
            image VARCHAR(500) NOT NULL DEFAULT '',
            images TEXT NOT NULL DEFAULT '[]',
            sale_service TEXT NOT NULL DEFAULT '[]',
            introduction TEXT NOT NULL DEFAULT '',
            spec_items TEXT NOT NULL DEFAULT '{}',
            para_items TEXT NOT NULL DEFAULT '{}',
            sale_num INTEGER NOT NULL DEFAULT 0 CHECK (sale_num >= 0),
            comment_num INTEGER NOT NULL DEFAULT 0 CHECK (comment_num >= 0),
            status CHAR(1) NOT NULL DEFAULT '0' CHECK (status IN ('0', '1')),
            is_marketable BOOLEAN NOT NULL DEFAULT FALSE,
            is_enable_spec BOOLEAN NOT NULL,
            CONSTRAINT fk_spu_brand FOREIGN KEY (brand_id) REFERENCES brand (id) ON DELETE RESTRICT,
            CONSTRAINT fk_spu_category1 FOREIGN KEY (category1_id) REFERENCES category (id) ON DELETE RESTRICT,
            CONSTRAINT fk_spu_category2 FOREIGN KEY (category2_id) REFERENCES category (id) ON DELETE RESTRICT,
            CONSTRAINT fk_spu_category3 FOREIGN KEY (category3_id) REFERENCES category (id) ON DELETE RESTRICT,
            CONSTRAINT fk_spu_template FOREIGN KEY (template_id) REFERENCES template (id) ON DELETE RESTRICT,
            CONSTRAINT ck_spu_publication CHECK (NOT is_marketable OR (status = '1' AND deleted_at IS NULL)),
            CONSTRAINT ck_spu_recycled CHECK (deleted_at IS NULL OR (status = '0' AND NOT is_marketable))
        );

        CREATE TABLE sku (
            id BIGINT PRIMARY KEY,
            tenant_id BIGINT NOT NULL,
            create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
            version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
            spu_id BIGINT NOT NULL,
            sn VARCHAR(64) NOT NULL,
            name VARCHAR(3000) NOT NULL,
            price INTEGER NOT NULL CHECK (price >= 1),
            num INTEGER NOT NULL CHECK (num >= 0),
            alert_num INTEGER NOT NULL CHECK (alert_num >= 0),
            weight INTEGER NOT NULL CHECK (weight >= 0),
            image VARCHAR(500) NOT NULL DEFAULT '',
            images TEXT NOT NULL DEFAULT '[]',
            spec TEXT NOT NULL DEFAULT '{}',
            category_id BIGINT NOT NULL,
            category_name VARCHAR(120) NOT NULL,
            brand_name VARCHAR(120) NOT NULL,
            sale_num INTEGER NOT NULL DEFAULT 0 CHECK (sale_num >= 0),
            comment_num INTEGER NOT NULL DEFAULT 0 CHECK (comment_num >= 0),
            status CHAR(1) NOT NULL DEFAULT '1' CHECK (status IN ('1', '2')),
            deletion_kind CHAR(1),
            CONSTRAINT fk_sku_spu FOREIGN KEY (spu_id) REFERENCES spu (id) ON DELETE RESTRICT,
            CONSTRAINT fk_sku_category FOREIGN KEY (category_id) REFERENCES category (id) ON DELETE RESTRICT,
            CONSTRAINT ck_sku_deletion CHECK (
                (deleted_at IS NULL AND deletion_kind IS NULL)
                OR (deleted_at IS NOT NULL AND deletion_kind IS NOT NULL AND deletion_kind IN ('P', 'E'))
            )
        );

        -- PostgreSQL NULL 不参与普通唯一比较：生命周期键与有效行唯一保护必须成对存在。
        CREATE UNIQUE INDEX uk_spu_lifecycle ON spu (tenant_id, sn, deleted_at);
        CREATE UNIQUE INDEX uk_spu_active ON spu (tenant_id, sn) WHERE deleted_at IS NULL;
        CREATE INDEX idx_spu_active_page ON spu (tenant_id, id DESC) WHERE deleted_at IS NULL;
        CREATE INDEX idx_spu_deleted_page ON spu (tenant_id, id DESC) WHERE deleted_at IS NOT NULL;
        CREATE UNIQUE INDEX uk_sku_lifecycle ON sku (tenant_id, sn, deleted_at);
        CREATE UNIQUE INDEX uk_sku_active ON sku (tenant_id, sn) WHERE deleted_at IS NULL;
        CREATE INDEX idx_sku_parent ON sku (tenant_id, spu_id, id);
    ELSIF current_setting('egon_migration.role') = 'SHARD' THEN
        -- 新商品表为 SINGLE/master_data，分片目标不创建这些表。
    ELSE
        RAISE EXCEPTION 'Unknown managed DDL role';
    END IF;
END
$egon$;
```

## 2026-10-09 12:30:27 Asia/Shanghai — product-sku-model

profile: light

outputRoot: enjoyshop-service/enjoyshop-service-goods

throughVersion: 20261009_001

planId: 73e590bc674e43c35af8df32df4d493237f39578609b0840ab37c8aa9157cb2f

Outcome: generator apply; SQL input only, no database execution.

### 20260913_001

Path: enjoyshop-service/enjoyshop-service-goods/src/main/resources/db/egon-mp/V20260913_001__initialize_repository_schema.sql

SHA-256: e4936cb2a7943d50cd4b7d7b49252d9712513a2f27ade70f125d2bf8f335ea31

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

### 20260923_001

Path: enjoyshop-service/enjoyshop-service-goods/src/main/resources/db/egon-mp/V20260923_001__initialize_goods_schema.sql

SHA-256: 1904922e16dc9d2c2d56afb8a9cafb3fff2e4291cf20774f47a64a2ab5e44a17

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

### 20260929_001

Path: enjoyshop-service/enjoyshop-service-goods/src/main/resources/db/egon-mp/V20260929_001__initialize_catalog_lookup_schema.sql

SHA-256: 6db577031d1adb40dc83ca5c187345c3ad0cb94d6e446a877163e5755280bd83

```sql
-- 变更内容：新增商品目录读取所需的分类、模板、规格、参数和分类品牌关联表。
-- 影响范围：仅 master_data 角色；现有 brand 表及其数据保持不变。
-- 兼容性说明：新增表，不回填或写入目录数据；ACTOR-003 负责后续目录写入。
DO $egon$
BEGIN
    IF current_setting('egon_migration.role') = 'MASTER_DATA' THEN
        CREATE TABLE template (
            id BIGINT PRIMARY KEY,
            tenant_id BIGINT NOT NULL,
            create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
            version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
            name VARCHAR(120) NOT NULL,
            spec_num INTEGER NOT NULL DEFAULT 0 CHECK (spec_num >= 0),
            para_num INTEGER NOT NULL DEFAULT 0 CHECK (para_num >= 0)
        );

        CREATE TABLE category (
            id BIGINT PRIMARY KEY,
            tenant_id BIGINT NOT NULL,
            create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
            version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
            name VARCHAR(120) NOT NULL,
            goods_num INTEGER NOT NULL DEFAULT 0 CHECK (goods_num >= 0),
            is_show CHAR(1) NOT NULL DEFAULT '1' CHECK (is_show IN ('0', '1')),
            is_menu CHAR(1) NOT NULL DEFAULT '0' CHECK (is_menu IN ('0', '1')),
            seq INTEGER NOT NULL DEFAULT 0 CHECK (seq >= 0),
            parent_id BIGINT,
            template_id BIGINT,
            CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES category (id) ON DELETE RESTRICT,
            CONSTRAINT fk_category_template FOREIGN KEY (template_id) REFERENCES template (id) ON DELETE RESTRICT
        );

        CREATE TABLE spec (
            id BIGINT PRIMARY KEY,
            tenant_id BIGINT NOT NULL,
            create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
            version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
            name VARCHAR(120) NOT NULL,
            options TEXT NOT NULL,
            seq INTEGER NOT NULL DEFAULT 0 CHECK (seq >= 0),
            template_id BIGINT NOT NULL,
            CONSTRAINT fk_spec_template FOREIGN KEY (template_id) REFERENCES template (id) ON DELETE RESTRICT
        );

        CREATE TABLE para (
            id BIGINT PRIMARY KEY,
            tenant_id BIGINT NOT NULL,
            create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
            version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
            name VARCHAR(120) NOT NULL,
            options TEXT NOT NULL,
            seq INTEGER NOT NULL DEFAULT 0 CHECK (seq >= 0),
            template_id BIGINT NOT NULL,
            CONSTRAINT fk_para_template FOREIGN KEY (template_id) REFERENCES template (id) ON DELETE RESTRICT
        );

        CREATE TABLE category_brand (
            id BIGINT PRIMARY KEY,
            tenant_id BIGINT NOT NULL,
            create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
            version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
            category_id BIGINT NOT NULL,
            brand_id BIGINT NOT NULL,
            CONSTRAINT fk_category_brand_category FOREIGN KEY (category_id) REFERENCES category (id) ON DELETE RESTRICT,
            CONSTRAINT fk_category_brand_brand FOREIGN KEY (brand_id) REFERENCES brand (id) ON DELETE RESTRICT
        );

        CREATE INDEX idx_category_tenant_name_active
            ON category (tenant_id, name, id)
            WHERE deleted_at IS NULL;
        CREATE INDEX idx_spec_tenant_template_seq_id_active
            ON spec (tenant_id, template_id, seq, id)
            WHERE deleted_at IS NULL;
        CREATE UNIQUE INDEX uk_category_brand_lifecycle
            ON category_brand (tenant_id, category_id, brand_id, deleted_at);
        CREATE UNIQUE INDEX uk_category_brand_active
            ON category_brand (tenant_id, category_id, brand_id)
            WHERE deleted_at IS NULL;
    ELSIF current_setting('egon_migration.role') = 'SHARD' THEN
        -- 所有目录表均为 SINGLE/master_data，此分支不建表。
    ELSE
        RAISE EXCEPTION 'Unknown managed DDL role';
    END IF;
END
$egon$;
```

### 20261009_001

Path: enjoyshop-service/enjoyshop-service-goods/src/main/resources/db/egon-mp/V20261009_001__initialize_product_schema.sql

SHA-256: db319455730c9d20b96401bbd7cdf0f488b23f8fa4efa15ccc4e1985e167c24c

```sql
-- 商品聚合：spu/sku 与目录关联均由 master_data 承载，同组 LOCAL 事务。
-- 仅新增商品表；目录表、历史迁移及其校验和保持不变。
DO $egon$
BEGIN
    IF current_setting('egon_migration.role') = 'MASTER_DATA' THEN
        CREATE TABLE spu (
            id BIGINT PRIMARY KEY,
            tenant_id BIGINT NOT NULL,
            create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
            version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
            sn VARCHAR(64) NOT NULL,
            name VARCHAR(200) NOT NULL,
            caption VARCHAR(300) NOT NULL DEFAULT '',
            brand_id BIGINT NOT NULL,
            category1_id BIGINT NOT NULL,
            category2_id BIGINT NOT NULL,
            category3_id BIGINT NOT NULL,
            template_id BIGINT NOT NULL,
            freight_id BIGINT NOT NULL DEFAULT 0 CHECK (freight_id >= 0),
            image VARCHAR(500) NOT NULL DEFAULT '',
            images TEXT NOT NULL DEFAULT '[]',
            sale_service TEXT NOT NULL DEFAULT '[]',
            introduction TEXT NOT NULL DEFAULT '',
            spec_items TEXT NOT NULL DEFAULT '{}',
            para_items TEXT NOT NULL DEFAULT '{}',
            sale_num INTEGER NOT NULL DEFAULT 0 CHECK (sale_num >= 0),
            comment_num INTEGER NOT NULL DEFAULT 0 CHECK (comment_num >= 0),
            status CHAR(1) NOT NULL DEFAULT '0' CHECK (status IN ('0', '1')),
            is_marketable BOOLEAN NOT NULL DEFAULT FALSE,
            is_enable_spec BOOLEAN NOT NULL,
            CONSTRAINT fk_spu_brand FOREIGN KEY (brand_id) REFERENCES brand (id) ON DELETE RESTRICT,
            CONSTRAINT fk_spu_category1 FOREIGN KEY (category1_id) REFERENCES category (id) ON DELETE RESTRICT,
            CONSTRAINT fk_spu_category2 FOREIGN KEY (category2_id) REFERENCES category (id) ON DELETE RESTRICT,
            CONSTRAINT fk_spu_category3 FOREIGN KEY (category3_id) REFERENCES category (id) ON DELETE RESTRICT,
            CONSTRAINT fk_spu_template FOREIGN KEY (template_id) REFERENCES template (id) ON DELETE RESTRICT,
            CONSTRAINT ck_spu_publication CHECK (NOT is_marketable OR (status = '1' AND deleted_at IS NULL)),
            CONSTRAINT ck_spu_recycled CHECK (deleted_at IS NULL OR (status = '0' AND NOT is_marketable))
        );

        CREATE TABLE sku (
            id BIGINT PRIMARY KEY,
            tenant_id BIGINT NOT NULL,
            create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
            version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
            spu_id BIGINT NOT NULL,
            sn VARCHAR(64) NOT NULL,
            name VARCHAR(3000) NOT NULL,
            price INTEGER NOT NULL CHECK (price >= 1),
            num INTEGER NOT NULL CHECK (num >= 0),
            alert_num INTEGER NOT NULL CHECK (alert_num >= 0),
            weight INTEGER NOT NULL CHECK (weight >= 0),
            image VARCHAR(500) NOT NULL DEFAULT '',
            images TEXT NOT NULL DEFAULT '[]',
            spec TEXT NOT NULL DEFAULT '{}',
            category_id BIGINT NOT NULL,
            category_name VARCHAR(120) NOT NULL,
            brand_name VARCHAR(120) NOT NULL,
            sale_num INTEGER NOT NULL DEFAULT 0 CHECK (sale_num >= 0),
            comment_num INTEGER NOT NULL DEFAULT 0 CHECK (comment_num >= 0),
            status CHAR(1) NOT NULL DEFAULT '1' CHECK (status IN ('1', '2')),
            deletion_kind CHAR(1),
            CONSTRAINT fk_sku_spu FOREIGN KEY (spu_id) REFERENCES spu (id) ON DELETE RESTRICT,
            CONSTRAINT fk_sku_category FOREIGN KEY (category_id) REFERENCES category (id) ON DELETE RESTRICT,
            CONSTRAINT ck_sku_deletion CHECK (
                (deleted_at IS NULL AND deletion_kind IS NULL)
                OR (deleted_at IS NOT NULL AND deletion_kind IS NOT NULL AND deletion_kind IN ('P', 'E'))
            )
        );

        -- PostgreSQL NULL 不参与普通唯一比较：生命周期键与有效行唯一保护必须成对存在。
        CREATE UNIQUE INDEX uk_spu_lifecycle ON spu (tenant_id, sn, deleted_at);
        CREATE UNIQUE INDEX uk_spu_active ON spu (tenant_id, sn) WHERE deleted_at IS NULL;
        CREATE INDEX idx_spu_active_page ON spu (tenant_id, id DESC) WHERE deleted_at IS NULL;
        CREATE INDEX idx_spu_deleted_page ON spu (tenant_id, id DESC) WHERE deleted_at IS NOT NULL;
        CREATE UNIQUE INDEX uk_sku_lifecycle ON sku (tenant_id, sn, deleted_at);
        CREATE UNIQUE INDEX uk_sku_active ON sku (tenant_id, sn) WHERE deleted_at IS NULL;
        CREATE INDEX idx_sku_parent ON sku (tenant_id, spu_id, id);
    ELSIF current_setting('egon_migration.role') = 'SHARD' THEN
        -- 新商品表为 SINGLE/master_data，分片目标不创建这些表。
    ELSE
        RAISE EXCEPTION 'Unknown managed DDL role';
    END IF;
END
$egon$;
```

## 2026-10-09 12:55:26 Asia/Shanghai — product-spu-model column-policy correction

profile: light

outputRoot: enjoyshop-service/enjoyshop-service-goods

throughVersion: 20261009_001

planId: d024b5ea68cefe92bf2a5b2f37c6404dd2b08cf02f851b25f53b7e217fc44a36

Outcome: generator apply; SQL column-name policy corrected, no database execution.

### 20260913_001

Path: enjoyshop-service/enjoyshop-service-goods/src/main/resources/db/egon-mp/V20260913_001__initialize_repository_schema.sql

SHA-256: e4936cb2a7943d50cd4b7d7b49252d9712513a2f27ade70f125d2bf8f335ea31

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

### 20260923_001

Path: enjoyshop-service/enjoyshop-service-goods/src/main/resources/db/egon-mp/V20260923_001__initialize_goods_schema.sql

SHA-256: 1904922e16dc9d2c2d56afb8a9cafb3fff2e4291cf20774f47a64a2ab5e44a17

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

### 20260929_001

Path: enjoyshop-service/enjoyshop-service-goods/src/main/resources/db/egon-mp/V20260929_001__initialize_catalog_lookup_schema.sql

SHA-256: 6db577031d1adb40dc83ca5c187345c3ad0cb94d6e446a877163e5755280bd83

```sql
-- 变更内容：新增商品目录读取所需的分类、模板、规格、参数和分类品牌关联表。
-- 影响范围：仅 master_data 角色；现有 brand 表及其数据保持不变。
-- 兼容性说明：新增表，不回填或写入目录数据；ACTOR-003 负责后续目录写入。
DO $egon$
BEGIN
    IF current_setting('egon_migration.role') = 'MASTER_DATA' THEN
        CREATE TABLE template (
            id BIGINT PRIMARY KEY,
            tenant_id BIGINT NOT NULL,
            create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
            version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
            name VARCHAR(120) NOT NULL,
            spec_num INTEGER NOT NULL DEFAULT 0 CHECK (spec_num >= 0),
            para_num INTEGER NOT NULL DEFAULT 0 CHECK (para_num >= 0)
        );

        CREATE TABLE category (
            id BIGINT PRIMARY KEY,
            tenant_id BIGINT NOT NULL,
            create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
            version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
            name VARCHAR(120) NOT NULL,
            goods_num INTEGER NOT NULL DEFAULT 0 CHECK (goods_num >= 0),
            is_show CHAR(1) NOT NULL DEFAULT '1' CHECK (is_show IN ('0', '1')),
            is_menu CHAR(1) NOT NULL DEFAULT '0' CHECK (is_menu IN ('0', '1')),
            seq INTEGER NOT NULL DEFAULT 0 CHECK (seq >= 0),
            parent_id BIGINT,
            template_id BIGINT,
            CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES category (id) ON DELETE RESTRICT,
            CONSTRAINT fk_category_template FOREIGN KEY (template_id) REFERENCES template (id) ON DELETE RESTRICT
        );

        CREATE TABLE spec (
            id BIGINT PRIMARY KEY,
            tenant_id BIGINT NOT NULL,
            create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
            version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
            name VARCHAR(120) NOT NULL,
            options TEXT NOT NULL,
            seq INTEGER NOT NULL DEFAULT 0 CHECK (seq >= 0),
            template_id BIGINT NOT NULL,
            CONSTRAINT fk_spec_template FOREIGN KEY (template_id) REFERENCES template (id) ON DELETE RESTRICT
        );

        CREATE TABLE para (
            id BIGINT PRIMARY KEY,
            tenant_id BIGINT NOT NULL,
            create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
            version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
            name VARCHAR(120) NOT NULL,
            options TEXT NOT NULL,
            seq INTEGER NOT NULL DEFAULT 0 CHECK (seq >= 0),
            template_id BIGINT NOT NULL,
            CONSTRAINT fk_para_template FOREIGN KEY (template_id) REFERENCES template (id) ON DELETE RESTRICT
        );

        CREATE TABLE category_brand (
            id BIGINT PRIMARY KEY,
            tenant_id BIGINT NOT NULL,
            create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
            version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
            category_id BIGINT NOT NULL,
            brand_id BIGINT NOT NULL,
            CONSTRAINT fk_category_brand_category FOREIGN KEY (category_id) REFERENCES category (id) ON DELETE RESTRICT,
            CONSTRAINT fk_category_brand_brand FOREIGN KEY (brand_id) REFERENCES brand (id) ON DELETE RESTRICT
        );

        CREATE INDEX idx_category_tenant_name_active
            ON category (tenant_id, name, id)
            WHERE deleted_at IS NULL;
        CREATE INDEX idx_spec_tenant_template_seq_id_active
            ON spec (tenant_id, template_id, seq, id)
            WHERE deleted_at IS NULL;
        CREATE UNIQUE INDEX uk_category_brand_lifecycle
            ON category_brand (tenant_id, category_id, brand_id, deleted_at);
        CREATE UNIQUE INDEX uk_category_brand_active
            ON category_brand (tenant_id, category_id, brand_id)
            WHERE deleted_at IS NULL;
    ELSIF current_setting('egon_migration.role') = 'SHARD' THEN
        -- 所有目录表均为 SINGLE/master_data，此分支不建表。
    ELSE
        RAISE EXCEPTION 'Unknown managed DDL role';
    END IF;
END
$egon$;
```

### 20261009_001

Path: enjoyshop-service/enjoyshop-service-goods/src/main/resources/db/egon-mp/V20261009_001__initialize_product_schema.sql

SHA-256: db319455730c9d20b96401bbd7cdf0f488b23f8fa4efa15ccc4e1985e167c24c

```sql
-- 商品聚合：spu/sku 与目录关联均由 master_data 承载，同组 LOCAL 事务。
-- 仅新增商品表；目录表、历史迁移及其校验和保持不变。
DO $egon$
BEGIN
    IF current_setting('egon_migration.role') = 'MASTER_DATA' THEN
        CREATE TABLE spu (
            id BIGINT PRIMARY KEY,
            tenant_id BIGINT NOT NULL,
            create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
            version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
            sn VARCHAR(64) NOT NULL,
            name VARCHAR(200) NOT NULL,
            caption VARCHAR(300) NOT NULL DEFAULT '',
            brand_id BIGINT NOT NULL,
            category1_id BIGINT NOT NULL,
            category2_id BIGINT NOT NULL,
            category3_id BIGINT NOT NULL,
            template_id BIGINT NOT NULL,
            freight_id BIGINT NOT NULL DEFAULT 0 CHECK (freight_id >= 0),
            image VARCHAR(500) NOT NULL DEFAULT '',
            images TEXT NOT NULL DEFAULT '[]',
            sale_service TEXT NOT NULL DEFAULT '[]',
            introduction TEXT NOT NULL DEFAULT '',
            spec_items TEXT NOT NULL DEFAULT '{}',
            para_items TEXT NOT NULL DEFAULT '{}',
            sale_num INTEGER NOT NULL DEFAULT 0 CHECK (sale_num >= 0),
            comment_num INTEGER NOT NULL DEFAULT 0 CHECK (comment_num >= 0),
            status CHAR(1) NOT NULL DEFAULT '0' CHECK (status IN ('0', '1')),
            is_marketable BOOLEAN NOT NULL DEFAULT FALSE,
            is_enable_spec BOOLEAN NOT NULL,
            CONSTRAINT fk_spu_brand FOREIGN KEY (brand_id) REFERENCES brand (id) ON DELETE RESTRICT,
            CONSTRAINT fk_spu_category1 FOREIGN KEY (category1_id) REFERENCES category (id) ON DELETE RESTRICT,
            CONSTRAINT fk_spu_category2 FOREIGN KEY (category2_id) REFERENCES category (id) ON DELETE RESTRICT,
            CONSTRAINT fk_spu_category3 FOREIGN KEY (category3_id) REFERENCES category (id) ON DELETE RESTRICT,
            CONSTRAINT fk_spu_template FOREIGN KEY (template_id) REFERENCES template (id) ON DELETE RESTRICT,
            CONSTRAINT ck_spu_publication CHECK (NOT is_marketable OR (status = '1' AND deleted_at IS NULL)),
            CONSTRAINT ck_spu_recycled CHECK (deleted_at IS NULL OR (status = '0' AND NOT is_marketable))
        );

        CREATE TABLE sku (
            id BIGINT PRIMARY KEY,
            tenant_id BIGINT NOT NULL,
            create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
            version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
            spu_id BIGINT NOT NULL,
            sn VARCHAR(64) NOT NULL,
            name VARCHAR(3000) NOT NULL,
            price INTEGER NOT NULL CHECK (price >= 1),
            num INTEGER NOT NULL CHECK (num >= 0),
            alert_num INTEGER NOT NULL CHECK (alert_num >= 0),
            weight INTEGER NOT NULL CHECK (weight >= 0),
            image VARCHAR(500) NOT NULL DEFAULT '',
            images TEXT NOT NULL DEFAULT '[]',
            spec TEXT NOT NULL DEFAULT '{}',
            category_id BIGINT NOT NULL,
            category_name VARCHAR(120) NOT NULL,
            brand_name VARCHAR(120) NOT NULL,
            sale_num INTEGER NOT NULL DEFAULT 0 CHECK (sale_num >= 0),
            comment_num INTEGER NOT NULL DEFAULT 0 CHECK (comment_num >= 0),
            status CHAR(1) NOT NULL DEFAULT '1' CHECK (status IN ('1', '2')),
            deletion_kind CHAR(1),
            CONSTRAINT fk_sku_spu FOREIGN KEY (spu_id) REFERENCES spu (id) ON DELETE RESTRICT,
            CONSTRAINT fk_sku_category FOREIGN KEY (category_id) REFERENCES category (id) ON DELETE RESTRICT,
            CONSTRAINT ck_sku_deletion CHECK (
                (deleted_at IS NULL AND deletion_kind IS NULL)
                OR (deleted_at IS NOT NULL AND deletion_kind IS NOT NULL AND deletion_kind IN ('P', 'E'))
            )
        );

        -- PostgreSQL NULL 不参与普通唯一比较：生命周期键与有效行唯一保护必须成对存在。
        CREATE UNIQUE INDEX uk_spu_lifecycle ON spu (tenant_id, sn, deleted_at);
        CREATE UNIQUE INDEX uk_spu_active ON spu (tenant_id, sn) WHERE deleted_at IS NULL;
        CREATE INDEX idx_spu_active_page ON spu (tenant_id, id DESC) WHERE deleted_at IS NULL;
        CREATE INDEX idx_spu_deleted_page ON spu (tenant_id, id DESC) WHERE deleted_at IS NOT NULL;
        CREATE UNIQUE INDEX uk_sku_lifecycle ON sku (tenant_id, sn, deleted_at);
        CREATE UNIQUE INDEX uk_sku_active ON sku (tenant_id, sn) WHERE deleted_at IS NULL;
        CREATE INDEX idx_sku_parent ON sku (tenant_id, spu_id, id);
    ELSIF current_setting('egon_migration.role') = 'SHARD' THEN
        -- 新商品表为 SINGLE/master_data，分片目标不创建这些表。
    ELSE
        RAISE EXCEPTION 'Unknown managed DDL role';
    END IF;
END
$egon$;
```

## 2026-10-09 12:55:28 Asia/Shanghai — product-sku-model column-policy correction

profile: light

outputRoot: enjoyshop-service/enjoyshop-service-goods

throughVersion: 20261009_001

planId: ff702ecb589bb32833dc6969357235bb8a5bd94f7ce960241032f2c7d25ed3d8

Outcome: generator apply; SQL column-name policy corrected, no database execution.

### 20260913_001

Path: enjoyshop-service/enjoyshop-service-goods/src/main/resources/db/egon-mp/V20260913_001__initialize_repository_schema.sql

SHA-256: e4936cb2a7943d50cd4b7d7b49252d9712513a2f27ade70f125d2bf8f335ea31

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

### 20260923_001

Path: enjoyshop-service/enjoyshop-service-goods/src/main/resources/db/egon-mp/V20260923_001__initialize_goods_schema.sql

SHA-256: 1904922e16dc9d2c2d56afb8a9cafb3fff2e4291cf20774f47a64a2ab5e44a17

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

### 20260929_001

Path: enjoyshop-service/enjoyshop-service-goods/src/main/resources/db/egon-mp/V20260929_001__initialize_catalog_lookup_schema.sql

SHA-256: 6db577031d1adb40dc83ca5c187345c3ad0cb94d6e446a877163e5755280bd83

```sql
-- 变更内容：新增商品目录读取所需的分类、模板、规格、参数和分类品牌关联表。
-- 影响范围：仅 master_data 角色；现有 brand 表及其数据保持不变。
-- 兼容性说明：新增表，不回填或写入目录数据；ACTOR-003 负责后续目录写入。
DO $egon$
BEGIN
    IF current_setting('egon_migration.role') = 'MASTER_DATA' THEN
        CREATE TABLE template (
            id BIGINT PRIMARY KEY,
            tenant_id BIGINT NOT NULL,
            create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
            version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
            name VARCHAR(120) NOT NULL,
            spec_num INTEGER NOT NULL DEFAULT 0 CHECK (spec_num >= 0),
            para_num INTEGER NOT NULL DEFAULT 0 CHECK (para_num >= 0)
        );

        CREATE TABLE category (
            id BIGINT PRIMARY KEY,
            tenant_id BIGINT NOT NULL,
            create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
            version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
            name VARCHAR(120) NOT NULL,
            goods_num INTEGER NOT NULL DEFAULT 0 CHECK (goods_num >= 0),
            is_show CHAR(1) NOT NULL DEFAULT '1' CHECK (is_show IN ('0', '1')),
            is_menu CHAR(1) NOT NULL DEFAULT '0' CHECK (is_menu IN ('0', '1')),
            seq INTEGER NOT NULL DEFAULT 0 CHECK (seq >= 0),
            parent_id BIGINT,
            template_id BIGINT,
            CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES category (id) ON DELETE RESTRICT,
            CONSTRAINT fk_category_template FOREIGN KEY (template_id) REFERENCES template (id) ON DELETE RESTRICT
        );

        CREATE TABLE spec (
            id BIGINT PRIMARY KEY,
            tenant_id BIGINT NOT NULL,
            create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
            version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
            name VARCHAR(120) NOT NULL,
            options TEXT NOT NULL,
            seq INTEGER NOT NULL DEFAULT 0 CHECK (seq >= 0),
            template_id BIGINT NOT NULL,
            CONSTRAINT fk_spec_template FOREIGN KEY (template_id) REFERENCES template (id) ON DELETE RESTRICT
        );

        CREATE TABLE para (
            id BIGINT PRIMARY KEY,
            tenant_id BIGINT NOT NULL,
            create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
            version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
            name VARCHAR(120) NOT NULL,
            options TEXT NOT NULL,
            seq INTEGER NOT NULL DEFAULT 0 CHECK (seq >= 0),
            template_id BIGINT NOT NULL,
            CONSTRAINT fk_para_template FOREIGN KEY (template_id) REFERENCES template (id) ON DELETE RESTRICT
        );

        CREATE TABLE category_brand (
            id BIGINT PRIMARY KEY,
            tenant_id BIGINT NOT NULL,
            create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
            version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
            category_id BIGINT NOT NULL,
            brand_id BIGINT NOT NULL,
            CONSTRAINT fk_category_brand_category FOREIGN KEY (category_id) REFERENCES category (id) ON DELETE RESTRICT,
            CONSTRAINT fk_category_brand_brand FOREIGN KEY (brand_id) REFERENCES brand (id) ON DELETE RESTRICT
        );

        CREATE INDEX idx_category_tenant_name_active
            ON category (tenant_id, name, id)
            WHERE deleted_at IS NULL;
        CREATE INDEX idx_spec_tenant_template_seq_id_active
            ON spec (tenant_id, template_id, seq, id)
            WHERE deleted_at IS NULL;
        CREATE UNIQUE INDEX uk_category_brand_lifecycle
            ON category_brand (tenant_id, category_id, brand_id, deleted_at);
        CREATE UNIQUE INDEX uk_category_brand_active
            ON category_brand (tenant_id, category_id, brand_id)
            WHERE deleted_at IS NULL;
    ELSIF current_setting('egon_migration.role') = 'SHARD' THEN
        -- 所有目录表均为 SINGLE/master_data，此分支不建表。
    ELSE
        RAISE EXCEPTION 'Unknown managed DDL role';
    END IF;
END
$egon$;
```

### 20261009_001

Path: enjoyshop-service/enjoyshop-service-goods/src/main/resources/db/egon-mp/V20261009_001__initialize_product_schema.sql

SHA-256: db319455730c9d20b96401bbd7cdf0f488b23f8fa4efa15ccc4e1985e167c24c

```sql
-- 商品聚合：spu/sku 与目录关联均由 master_data 承载，同组 LOCAL 事务。
-- 仅新增商品表；目录表、历史迁移及其校验和保持不变。
DO $egon$
BEGIN
    IF current_setting('egon_migration.role') = 'MASTER_DATA' THEN
        CREATE TABLE spu (
            id BIGINT PRIMARY KEY,
            tenant_id BIGINT NOT NULL,
            create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
            version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
            sn VARCHAR(64) NOT NULL,
            name VARCHAR(200) NOT NULL,
            caption VARCHAR(300) NOT NULL DEFAULT '',
            brand_id BIGINT NOT NULL,
            category1_id BIGINT NOT NULL,
            category2_id BIGINT NOT NULL,
            category3_id BIGINT NOT NULL,
            template_id BIGINT NOT NULL,
            freight_id BIGINT NOT NULL DEFAULT 0 CHECK (freight_id >= 0),
            image VARCHAR(500) NOT NULL DEFAULT '',
            images TEXT NOT NULL DEFAULT '[]',
            sale_service TEXT NOT NULL DEFAULT '[]',
            introduction TEXT NOT NULL DEFAULT '',
            spec_items TEXT NOT NULL DEFAULT '{}',
            para_items TEXT NOT NULL DEFAULT '{}',
            sale_num INTEGER NOT NULL DEFAULT 0 CHECK (sale_num >= 0),
            comment_num INTEGER NOT NULL DEFAULT 0 CHECK (comment_num >= 0),
            status CHAR(1) NOT NULL DEFAULT '0' CHECK (status IN ('0', '1')),
            is_marketable BOOLEAN NOT NULL DEFAULT FALSE,
            is_enable_spec BOOLEAN NOT NULL,
            CONSTRAINT fk_spu_brand FOREIGN KEY (brand_id) REFERENCES brand (id) ON DELETE RESTRICT,
            CONSTRAINT fk_spu_category1 FOREIGN KEY (category1_id) REFERENCES category (id) ON DELETE RESTRICT,
            CONSTRAINT fk_spu_category2 FOREIGN KEY (category2_id) REFERENCES category (id) ON DELETE RESTRICT,
            CONSTRAINT fk_spu_category3 FOREIGN KEY (category3_id) REFERENCES category (id) ON DELETE RESTRICT,
            CONSTRAINT fk_spu_template FOREIGN KEY (template_id) REFERENCES template (id) ON DELETE RESTRICT,
            CONSTRAINT ck_spu_publication CHECK (NOT is_marketable OR (status = '1' AND deleted_at IS NULL)),
            CONSTRAINT ck_spu_recycled CHECK (deleted_at IS NULL OR (status = '0' AND NOT is_marketable))
        );

        CREATE TABLE sku (
            id BIGINT PRIMARY KEY,
            tenant_id BIGINT NOT NULL,
            create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
            version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
            spu_id BIGINT NOT NULL,
            sn VARCHAR(64) NOT NULL,
            name VARCHAR(3000) NOT NULL,
            price INTEGER NOT NULL CHECK (price >= 1),
            num INTEGER NOT NULL CHECK (num >= 0),
            alert_num INTEGER NOT NULL CHECK (alert_num >= 0),
            weight INTEGER NOT NULL CHECK (weight >= 0),
            image VARCHAR(500) NOT NULL DEFAULT '',
            images TEXT NOT NULL DEFAULT '[]',
            spec TEXT NOT NULL DEFAULT '{}',
            category_id BIGINT NOT NULL,
            category_name VARCHAR(120) NOT NULL,
            brand_name VARCHAR(120) NOT NULL,
            sale_num INTEGER NOT NULL DEFAULT 0 CHECK (sale_num >= 0),
            comment_num INTEGER NOT NULL DEFAULT 0 CHECK (comment_num >= 0),
            status CHAR(1) NOT NULL DEFAULT '1' CHECK (status IN ('1', '2')),
            deletion_kind CHAR(1),
            CONSTRAINT fk_sku_spu FOREIGN KEY (spu_id) REFERENCES spu (id) ON DELETE RESTRICT,
            CONSTRAINT fk_sku_category FOREIGN KEY (category_id) REFERENCES category (id) ON DELETE RESTRICT,
            CONSTRAINT ck_sku_deletion CHECK (
                (deleted_at IS NULL AND deletion_kind IS NULL)
                OR (deleted_at IS NOT NULL AND deletion_kind IS NOT NULL AND deletion_kind IN ('P', 'E'))
            )
        );

        -- PostgreSQL NULL 不参与普通唯一比较：生命周期键与有效行唯一保护必须成对存在。
        CREATE UNIQUE INDEX uk_spu_lifecycle ON spu (tenant_id, sn, deleted_at);
        CREATE UNIQUE INDEX uk_spu_active ON spu (tenant_id, sn) WHERE deleted_at IS NULL;
        CREATE INDEX idx_spu_active_page ON spu (tenant_id, id DESC) WHERE deleted_at IS NULL;
        CREATE INDEX idx_spu_deleted_page ON spu (tenant_id, id DESC) WHERE deleted_at IS NOT NULL;
        CREATE UNIQUE INDEX uk_sku_lifecycle ON sku (tenant_id, sn, deleted_at);
        CREATE UNIQUE INDEX uk_sku_active ON sku (tenant_id, sn) WHERE deleted_at IS NULL;
        CREATE INDEX idx_sku_parent ON sku (tenant_id, spu_id, id);
    ELSIF current_setting('egon_migration.role') = 'SHARD' THEN
        -- 新商品表为 SINGLE/master_data，分片目标不创建这些表。
    ELSE
        RAISE EXCEPTION 'Unknown managed DDL role';
    END IF;
END
$egon$;
```


## 2026-10-10 17:54 Asia/Shanghai — profile light → enjoyshop-service/enjoyshop-service-goods

- 生成动作：`egon-codegen.sh plan`（`planId=793d9d1e247ab79cc7492da5ee31b5d32517b0f5f14a9e483bd6c7a97e1c1496`，6 个 ADD、零冲突、零待处理）→ `egon-codegen.sh apply`（`status=APPLIED`，退出码 0）。
- 生成器配置：`enjoyshop-service/enjoyshop-service-goods/codegen/ad-codegen.json`（`input.mode=schema`，logicalTables=`ad`，artifacts=`po,dao,mapper-xml,repo,domain-model,domain-query`）。
- throughVersion: none — 输入是模块 `codegen/ad-create.sql`，不是 `src/main/resources/db/` 下的 classpath 脚本。
- 派生输入视图：`enjoyshop-service/enjoyshop-service-goods/codegen/ad-create.sql`；SHA-256（文件字节）：`6ddc271429997ae3935805bff3f3d79383cfc60e2a1abced925b9a71fa64e907`。
- 源脚本来源：`enjoyshop-service/enjoyshop-service-goods/src/main/resources/db/egon-mp/V20261010_001__initialize_advertisement_cdc.sql`；SHA-256（文件字节）：`f571b36ef7a87a8dfc7214113cf5a3232b65397050864960259401ddcad7d364`。
- 记录边界：生成过程只读取DDL并写入代码生成目标；未连接 PostgreSQL、未执行 SQL。

### Generator input view — `enjoyshop-service/enjoyshop-service-goods/codegen/ad-create.sql`

```sql
CREATE TABLE ad (
            id BIGINT PRIMARY KEY,
            tenant_id BIGINT NOT NULL,
            create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
            version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
            name VARCHAR(120) NOT NULL,
            position VARCHAR(64) NOT NULL,
            image_url VARCHAR(512) NOT NULL,
            target_url VARCHAR(1024) NOT NULL,
            status CHAR(1) NOT NULL DEFAULT '0' CHECK (status IN ('0', '1')),
            start_time TIMESTAMP(6) WITH TIME ZONE NOT NULL,
            end_time TIMESTAMP(6) WITH TIME ZONE NOT NULL,
            sort_order INTEGER NOT NULL DEFAULT 0,
            CONSTRAINT ck_ad_tenant CHECK (tenant_id > 0),
            CONSTRAINT ck_ad_name CHECK (char_length(name) BETWEEN 1 AND 120 AND btrim(name) <> ''),
            CONSTRAINT ck_ad_position CHECK (position ~ '^[a-z][a-z0-9_]{0,63}$'),
            CONSTRAINT ck_ad_image_url CHECK (btrim(image_url) <> ''),
            CONSTRAINT ck_ad_target_url CHECK (btrim(target_url) <> ''),
            CONSTRAINT ck_ad_window CHECK (end_time > start_time),
            CONSTRAINT ck_ad_sort_order CHECK (sort_order BETWEEN 0 AND 1000000)
        );

CREATE INDEX idx_ad_active_slot ON ad (tenant_id, position, sort_order, id) WHERE deleted_at IS NULL AND status = '1';
CREATE INDEX idx_ad_admin_tenant_id ON ad (tenant_id, id DESC) WHERE deleted_at IS NULL;
```

### Source migration provenance — `enjoyshop-service/enjoyshop-service-goods/src/main/resources/db/egon-mp/V20261010_001__initialize_advertisement_cdc.sql`

```sql
-- Advertisement source data is owned by master_data; shard targets remain unchanged.
DO $egon$
BEGIN
    IF current_setting('egon_migration.role') = 'MASTER_DATA' THEN
        CREATE TABLE ad (
            id BIGINT PRIMARY KEY,
            tenant_id BIGINT NOT NULL,
            create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
            version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
            name VARCHAR(120) NOT NULL,
            position VARCHAR(64) NOT NULL,
            image_url VARCHAR(512) NOT NULL,
            target_url VARCHAR(1024) NOT NULL,
            status CHAR(1) NOT NULL DEFAULT '0' CHECK (status IN ('0', '1')),
            start_time TIMESTAMP(6) WITH TIME ZONE NOT NULL,
            end_time TIMESTAMP(6) WITH TIME ZONE NOT NULL,
            sort_order INTEGER NOT NULL DEFAULT 0,
            CONSTRAINT ck_ad_tenant CHECK (tenant_id > 0),
            CONSTRAINT ck_ad_name CHECK (char_length(name) BETWEEN 1 AND 120 AND btrim(name) <> ''),
            CONSTRAINT ck_ad_position CHECK (position ~ '^[a-z][a-z0-9_]{0,63}$'),
            CONSTRAINT ck_ad_image_url CHECK (btrim(image_url) <> ''),
            CONSTRAINT ck_ad_target_url CHECK (btrim(target_url) <> ''),
            CONSTRAINT ck_ad_window CHECK (end_time > start_time),
            CONSTRAINT ck_ad_sort_order CHECK (sort_order BETWEEN 0 AND 1000000)
        );

        CREATE INDEX idx_ad_active_slot ON ad (tenant_id, position, sort_order, id) WHERE deleted_at IS NULL AND status = '1';
        CREATE INDEX idx_ad_admin_tenant_id ON ad (tenant_id, id DESC) WHERE deleted_at IS NULL;

        ALTER TABLE public.ad REPLICA IDENTITY FULL;
        ALTER TABLE public.spu REPLICA IDENTITY FULL;
        ALTER TABLE public.sku REPLICA IDENTITY FULL;
    ELSIF current_setting('egon_migration.role') = 'SHARD' THEN
        -- SHARD has no advertisement tables in this release.
    ELSE
        RAISE EXCEPTION 'Unknown managed DDL role';
    END IF;
END
$egon$;
```


## 2026-10-10 17:55 Asia/Shanghai — profile light → enjoyshop-service/enjoyshop-service-goods

- 生成动作：源DDL的status枚举约束改为等价表级CHECK后重新`plan`（`planId=2f1d6ec7629a381584bc5a19c90913b0df0c8cd307414bf19473fb68a03b381e`，AdPO 1个UPDATE，其余5个NO_CHANGE、零冲突）→ `apply`（`status=APPLIED`，退出码 0）。本次只更新仍由生成器拥有且哈希匹配的AdPO，已生成源码未手工覆盖。
- 生成器配置：`enjoyshop-service/enjoyshop-service-goods/codegen/ad-codegen.json`（`input.mode=schema`，logicalTables=`ad`，artifacts=`po,dao,mapper-xml,repo,domain-model,domain-query`）。
- throughVersion: none — 输入是模块 `codegen/ad-create.sql`，不是 `src/main/resources/db/` 下的 classpath 脚本。
- 派生输入视图：`enjoyshop-service/enjoyshop-service-goods/codegen/ad-create.sql`；SHA-256（文件字节）：`2a7f67c09c523d14d930af8785b297a32aac063909c55dc6aacda7cb5f19f61c`。
- 源脚本来源：`enjoyshop-service/enjoyshop-service-goods/src/main/resources/db/egon-mp/V20261010_001__initialize_advertisement_cdc.sql`；SHA-256（文件字节）：`f66d690bc2587fed033d7e7dd94121b18266e1f6a795acc477f38c6ba8392efa`。
- 记录边界：生成过程只读取DDL并写入AdPO；未连接 PostgreSQL、未执行 SQL。

### Generator input view — `enjoyshop-service/enjoyshop-service-goods/codegen/ad-create.sql`

```sql
CREATE TABLE ad (
            id BIGINT PRIMARY KEY,
            tenant_id BIGINT NOT NULL,
            create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
            version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
            name VARCHAR(120) NOT NULL,
            position VARCHAR(64) NOT NULL,
            image_url VARCHAR(512) NOT NULL,
            target_url VARCHAR(1024) NOT NULL,
            status CHAR(1) NOT NULL DEFAULT '0',
            start_time TIMESTAMP(6) WITH TIME ZONE NOT NULL,
            end_time TIMESTAMP(6) WITH TIME ZONE NOT NULL,
            sort_order INTEGER NOT NULL DEFAULT 0,
            CONSTRAINT ck_ad_tenant CHECK (tenant_id > 0),
            CONSTRAINT ck_ad_name CHECK (char_length(name) BETWEEN 1 AND 120 AND btrim(name) <> ''),
            CONSTRAINT ck_ad_position CHECK (position ~ '^[a-z][a-z0-9_]{0,63}$'),
            CONSTRAINT ck_ad_image_url CHECK (btrim(image_url) <> ''),
            CONSTRAINT ck_ad_target_url CHECK (btrim(target_url) <> ''),
            CONSTRAINT ck_ad_status CHECK (status IN ('0', '1')),
            CONSTRAINT ck_ad_window CHECK (end_time > start_time),
            CONSTRAINT ck_ad_sort_order CHECK (sort_order BETWEEN 0 AND 1000000)
        );

CREATE INDEX idx_ad_active_slot ON ad (tenant_id, position, sort_order, id) WHERE deleted_at IS NULL AND status = '1';
CREATE INDEX idx_ad_admin_tenant_id ON ad (tenant_id, id DESC) WHERE deleted_at IS NULL;
```

### Source migration provenance — `enjoyshop-service/enjoyshop-service-goods/src/main/resources/db/egon-mp/V20261010_001__initialize_advertisement_cdc.sql`

```sql
-- Advertisement source data is owned by master_data; shard targets remain unchanged.
DO $egon$
BEGIN
    IF current_setting('egon_migration.role') = 'MASTER_DATA' THEN
        CREATE TABLE ad (
            id BIGINT PRIMARY KEY,
            tenant_id BIGINT NOT NULL,
            create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
            version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
            name VARCHAR(120) NOT NULL,
            position VARCHAR(64) NOT NULL,
            image_url VARCHAR(512) NOT NULL,
            target_url VARCHAR(1024) NOT NULL,
            status CHAR(1) NOT NULL DEFAULT '0',
            start_time TIMESTAMP(6) WITH TIME ZONE NOT NULL,
            end_time TIMESTAMP(6) WITH TIME ZONE NOT NULL,
            sort_order INTEGER NOT NULL DEFAULT 0,
            CONSTRAINT ck_ad_tenant CHECK (tenant_id > 0),
            CONSTRAINT ck_ad_name CHECK (char_length(name) BETWEEN 1 AND 120 AND btrim(name) <> ''),
            CONSTRAINT ck_ad_position CHECK (position ~ '^[a-z][a-z0-9_]{0,63}$'),
            CONSTRAINT ck_ad_image_url CHECK (btrim(image_url) <> ''),
            CONSTRAINT ck_ad_target_url CHECK (btrim(target_url) <> ''),
            CONSTRAINT ck_ad_status CHECK (status IN ('0', '1')),
            CONSTRAINT ck_ad_window CHECK (end_time > start_time),
            CONSTRAINT ck_ad_sort_order CHECK (sort_order BETWEEN 0 AND 1000000)
        );

        CREATE INDEX idx_ad_active_slot ON ad (tenant_id, position, sort_order, id) WHERE deleted_at IS NULL AND status = '1';
        CREATE INDEX idx_ad_admin_tenant_id ON ad (tenant_id, id DESC) WHERE deleted_at IS NULL;

        ALTER TABLE public.ad REPLICA IDENTITY FULL;
        ALTER TABLE public.spu REPLICA IDENTITY FULL;
        ALTER TABLE public.sku REPLICA IDENTITY FULL;
    ELSIF current_setting('egon_migration.role') = 'SHARD' THEN
        -- SHARD has no advertisement tables in this release.
    ELSE
        RAISE EXCEPTION 'Unknown managed DDL role';
    END IF;
END
$egon$;
```

## 2026-10-10 18:11:32 Asia/Shanghai — profile light → enjoyshop-service/enjoyshop-service-goods

- 生成动作：`egon-codegen.sh plan`（`planId=73e3cdf98173df5d2c525e98a67761e16b7b783a40641814d2645f05d245dd16`，15 个 ADD、0 pending、0 冲突）→ `egon-codegen.sh apply`（`status=APPLIED`，退出码 0，应用 15 个文件）。
- 生成器配置：`enjoyshop-service/enjoyshop-service-goods/codegen/ad-codegen.json`（`input.mode=schema`，logicalTables=`ad`，artifacts=`command,query,result,converter,domain-service,domain-impl,manage,manage-impl`，existingTypeMappings 指向已存在的 AdPO/AdDAO）。
- throughVersion: none — 输入是 `enjoyshop-service/enjoyshop-service-goods/codegen/ad-create.sql`，不是 `src/main/resources/db/` 下的 classpath 脚本；该 apply 没有读取 classpath SQL。
- 派生输入视图：`enjoyshop-service/enjoyshop-service-goods/codegen/ad-create.sql`；SHA-256（文件字节）：`2a7f67c09c523d14d930af8785b297a32aac063909c55dc6aacda7cb5f19f61c`。
- 源脚本来源：`enjoyshop-service/enjoyshop-service-goods/src/main/resources/db/egon-mp/V20261010_001__initialize_advertisement_cdc.sql`；SHA-256（文件字节）：`f66d690bc2587fed033d7e7dd94121b18266e1f6a795acc477f38c6ba8392efa`。
- 同 Spec 定制：将模板生成的 `CreateAdCommand`/`UpdateAdCommand` 收敛为完整替换共用的 `AdSaveCommand`，将 `DeleteAdCommand` 收敛为 `AdLifecycleCommand`，以 `AdReadQuery` 承载匿名广告位读取；移除模板产生的逐命令Converter，改由 `AdApplicationConverter` 映射经验证输入和租户内查询，并保留 `AdResultConverter`/`AdPublicResultConverter` 的白名单结果投影。`AdPersistenceConverter` 实际文件路径按本次只读计划修正至 `infrastructure/ad/converter/`，Java package 与源路径一致。最终字段、身份/租户来源、数据库版本CAS、公开投影均按本 Spec；没有修改 Spec 规定之外的业务语义。
- 记录边界：生成器仅读取 schema 视图并写代码；未连接 PostgreSQL、未执行 SQL；没有改写既有 SQL、manifest 或本日志中先前记录。

### Generator input view — `enjoyshop-service/enjoyshop-service-goods/codegen/ad-create.sql`

```sql
CREATE TABLE ad (
            id BIGINT PRIMARY KEY,
            tenant_id BIGINT NOT NULL,
            create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
            version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
            name VARCHAR(120) NOT NULL,
            position VARCHAR(64) NOT NULL,
            image_url VARCHAR(512) NOT NULL,
            target_url VARCHAR(1024) NOT NULL,
            status CHAR(1) NOT NULL DEFAULT '0',
            start_time TIMESTAMP(6) WITH TIME ZONE NOT NULL,
            end_time TIMESTAMP(6) WITH TIME ZONE NOT NULL,
            sort_order INTEGER NOT NULL DEFAULT 0,
            CONSTRAINT ck_ad_tenant CHECK (tenant_id > 0),
            CONSTRAINT ck_ad_name CHECK (char_length(name) BETWEEN 1 AND 120 AND btrim(name) <> ''),
            CONSTRAINT ck_ad_position CHECK (position ~ '^[a-z][a-z0-9_]{0,63}$'),
            CONSTRAINT ck_ad_image_url CHECK (btrim(image_url) <> ''),
            CONSTRAINT ck_ad_target_url CHECK (btrim(target_url) <> ''),
            CONSTRAINT ck_ad_status CHECK (status IN ('0', '1')),
            CONSTRAINT ck_ad_window CHECK (end_time > start_time),
            CONSTRAINT ck_ad_sort_order CHECK (sort_order BETWEEN 0 AND 1000000)
        );

CREATE INDEX idx_ad_active_slot ON ad (tenant_id, position, sort_order, id) WHERE deleted_at IS NULL AND status = '1';
CREATE INDEX idx_ad_admin_tenant_id ON ad (tenant_id, id DESC) WHERE deleted_at IS NULL;
```

### Source migration provenance — `enjoyshop-service/enjoyshop-service-goods/src/main/resources/db/egon-mp/V20261010_001__initialize_advertisement_cdc.sql`

```sql
-- Advertisement source data is owned by master_data; shard targets remain unchanged.
DO $egon$
BEGIN
    IF current_setting('egon_migration.role') = 'MASTER_DATA' THEN
        CREATE TABLE ad (
            id BIGINT PRIMARY KEY,
            tenant_id BIGINT NOT NULL,
            create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
            version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
            name VARCHAR(120) NOT NULL,
            position VARCHAR(64) NOT NULL,
            image_url VARCHAR(512) NOT NULL,
            target_url VARCHAR(1024) NOT NULL,
            status CHAR(1) NOT NULL DEFAULT '0',
            start_time TIMESTAMP(6) WITH TIME ZONE NOT NULL,
            end_time TIMESTAMP(6) WITH TIME ZONE NOT NULL,
            sort_order INTEGER NOT NULL DEFAULT 0,
            CONSTRAINT ck_ad_tenant CHECK (tenant_id > 0),
            CONSTRAINT ck_ad_name CHECK (char_length(name) BETWEEN 1 AND 120 AND btrim(name) <> ''),
            CONSTRAINT ck_ad_position CHECK (position ~ '^[a-z][a-z0-9_]{0,63}$'),
            CONSTRAINT ck_ad_image_url CHECK (btrim(image_url) <> ''),
            CONSTRAINT ck_ad_target_url CHECK (btrim(target_url) <> ''),
            CONSTRAINT ck_ad_status CHECK (status IN ('0', '1')),
            CONSTRAINT ck_ad_window CHECK (end_time > start_time),
            CONSTRAINT ck_ad_sort_order CHECK (sort_order BETWEEN 0 AND 1000000)
        );

        CREATE INDEX idx_ad_active_slot ON ad (tenant_id, position, sort_order, id) WHERE deleted_at IS NULL AND status = '1';
        CREATE INDEX idx_ad_admin_tenant_id ON ad (tenant_id, id DESC) WHERE deleted_at IS NULL;

        ALTER TABLE public.ad REPLICA IDENTITY FULL;
        ALTER TABLE public.spu REPLICA IDENTITY FULL;
        ALTER TABLE public.sku REPLICA IDENTITY FULL;
    ELSIF current_setting('egon_migration.role') = 'SHARD' THEN
        -- SHARD has no advertisement tables in this release.
    ELSE
        RAISE EXCEPTION 'Unknown managed DDL role';
    END IF;
END
$egon$;
```

## 2026-10-10 18:27:19 Asia/Shanghai — profile light → enjoyshop-service/enjoyshop-service-goods

- 生成动作：`egon-codegen.sh plan`（`planId=7627e99c62bbe9d7c612d7549842bb5fdd165947a7585b5424d8f4c029362795`，1 个 ADD、0 pending、0 冲突）→ `egon-codegen.sh apply`（`status=APPLIED`，退出码 0，应用 1 个文件）。
- 生成器配置：`enjoyshop-service/enjoyshop-service-goods/codegen/ad-codegen.json`（`input.mode=schema`，artifacts=`controller`，logicalTables=`ad`；保留已存在 AdPO/AdDAO type mappings）。配置文件SHA-256：`fa3d59c8cf10a33901051f607286e78d27390322ae389c28ddc3ae4278ae409c`。
- throughVersion: none — 输入仍是 CREATE-only 模块schema视图 `enjoyshop-service/enjoyshop-service-goods/codegen/ad-create.sql`，不是 classpath SQL。该 apply 的唯一暂态输出是 `src/main/java/top/egon/enjoyshop/goods/adapter/ad/AdController.java`，随后按既有 advice 包边界移动到 `src/main/java/top/egon/enjoyshop/goods/adapter/goods/AdController.java` 并按Spec完成六个映射。
- 派生输入视图SHA-256（文件字节）：`2a7f67c09c523d14d930af8785b297a32aac063909c55dc6aacda7cb5f19f61c`。
- 原脚本来源：`enjoyshop-service/enjoyshop-service-goods/src/main/resources/db/egon-mp/V20261010_001__initialize_advertisement_cdc.sql`；SHA-256（文件字节）：`f66d690bc2587fed033d7e7dd94121b18266e1f6a795acc477f38c6ba8392efa`，与 Step 3 journal 完全一致。旧 classpath 脚本、Manifest 和 SQL 文本均未改变。
- 记录边界：生成器只读取schema视图并生成Controller源码；随后只移动该临时源码至批准包路径；未连接PostgreSQL、未执行SQL；不包含新增应用依赖或安全链/服务启动。

### Generator input view — `enjoyshop-service/enjoyshop-service-goods/codegen/ad-create.sql`

```sql
CREATE TABLE ad (
            id BIGINT PRIMARY KEY,
            tenant_id BIGINT NOT NULL,
            create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
            version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
            name VARCHAR(120) NOT NULL,
            position VARCHAR(64) NOT NULL,
            image_url VARCHAR(512) NOT NULL,
            target_url VARCHAR(1024) NOT NULL,
            status CHAR(1) NOT NULL DEFAULT '0',
            start_time TIMESTAMP(6) WITH TIME ZONE NOT NULL,
            end_time TIMESTAMP(6) WITH TIME ZONE NOT NULL,
            sort_order INTEGER NOT NULL DEFAULT 0,
            CONSTRAINT ck_ad_tenant CHECK (tenant_id > 0),
            CONSTRAINT ck_ad_name CHECK (char_length(name) BETWEEN 1 AND 120 AND btrim(name) <> ''),
            CONSTRAINT ck_ad_position CHECK (position ~ '^[a-z][a-z0-9_]{0,63}$'),
            CONSTRAINT ck_ad_image_url CHECK (btrim(image_url) <> ''),
            CONSTRAINT ck_ad_target_url CHECK (btrim(target_url) <> ''),
            CONSTRAINT ck_ad_status CHECK (status IN ('0', '1')),
            CONSTRAINT ck_ad_window CHECK (end_time > start_time),
            CONSTRAINT ck_ad_sort_order CHECK (sort_order BETWEEN 0 AND 1000000)
        );

CREATE INDEX idx_ad_active_slot ON ad (tenant_id, position, sort_order, id) WHERE deleted_at IS NULL AND status = '1';
CREATE INDEX idx_ad_admin_tenant_id ON ad (tenant_id, id DESC) WHERE deleted_at IS NULL;
```

### Source migration provenance — `enjoyshop-service/enjoyshop-service-goods/src/main/resources/db/egon-mp/V20261010_001__initialize_advertisement_cdc.sql`

```sql
-- Advertisement source data is owned by master_data; shard targets remain unchanged.
DO $egon$
BEGIN
    IF current_setting('egon_migration.role') = 'MASTER_DATA' THEN
        CREATE TABLE ad (
            id BIGINT PRIMARY KEY,
            tenant_id BIGINT NOT NULL,
            create_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            create_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            update_user_id VARCHAR(128) NOT NULL DEFAULT 'migration',
            update_time TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            deleted_at TIMESTAMP(6) WITHOUT TIME ZONE,
            version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
            name VARCHAR(120) NOT NULL,
            position VARCHAR(64) NOT NULL,
            image_url VARCHAR(512) NOT NULL,
            target_url VARCHAR(1024) NOT NULL,
            status CHAR(1) NOT NULL DEFAULT '0',
            start_time TIMESTAMP(6) WITH TIME ZONE NOT NULL,
            end_time TIMESTAMP(6) WITH TIME ZONE NOT NULL,
            sort_order INTEGER NOT NULL DEFAULT 0,
            CONSTRAINT ck_ad_tenant CHECK (tenant_id > 0),
            CONSTRAINT ck_ad_name CHECK (char_length(name) BETWEEN 1 AND 120 AND btrim(name) <> ''),
            CONSTRAINT ck_ad_position CHECK (position ~ '^[a-z][a-z0-9_]{0,63}$'),
            CONSTRAINT ck_ad_image_url CHECK (btrim(image_url) <> ''),
            CONSTRAINT ck_ad_target_url CHECK (btrim(target_url) <> ''),
            CONSTRAINT ck_ad_status CHECK (status IN ('0', '1')),
            CONSTRAINT ck_ad_window CHECK (end_time > start_time),
            CONSTRAINT ck_ad_sort_order CHECK (sort_order BETWEEN 0 AND 1000000)
        );

        CREATE INDEX idx_ad_active_slot ON ad (tenant_id, position, sort_order, id) WHERE deleted_at IS NULL AND status = '1';
        CREATE INDEX idx_ad_admin_tenant_id ON ad (tenant_id, id DESC) WHERE deleted_at IS NULL;

        ALTER TABLE public.ad REPLICA IDENTITY FULL;
        ALTER TABLE public.spu REPLICA IDENTITY FULL;
        ALTER TABLE public.sku REPLICA IDENTITY FULL;
    ELSIF current_setting('egon_migration.role') = 'SHARD' THEN
        -- SHARD has no advertisement tables in this release.
    ELSE
        RAISE EXCEPTION 'Unknown managed DDL role';
    END IF;
END
$egon$;
```
