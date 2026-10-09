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
