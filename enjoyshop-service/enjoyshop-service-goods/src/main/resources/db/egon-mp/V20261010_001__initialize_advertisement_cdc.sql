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
