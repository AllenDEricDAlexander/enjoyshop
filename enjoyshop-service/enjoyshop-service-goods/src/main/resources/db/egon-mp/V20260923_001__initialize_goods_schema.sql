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
        CREATE UNIQUE INDEX uk_brand_tenant_name_active ON brand (tenant_id, name) WHERE deleted_at IS NULL;
        CREATE INDEX idx_brand_tenant_seq_id ON brand (tenant_id, seq DESC, id DESC);
    ELSIF current_setting('egon_migration.role') = 'SHARD' THEN
        NULL;  -- brand 为 SINGLE 主数据表，分片目标不承载该表
    ELSE
        RAISE EXCEPTION 'Unknown managed DDL role';
    END IF;
END
$egon$;
