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
