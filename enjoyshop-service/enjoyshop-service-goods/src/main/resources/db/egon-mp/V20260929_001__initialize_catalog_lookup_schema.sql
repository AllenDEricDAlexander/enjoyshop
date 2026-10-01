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
