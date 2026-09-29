-- 供应商管理模块
-- 1. supplier：供应商主数据档案（采购员维护的合格供应商库）
-- 2. procurement_application_supplier：采购申请单的候选供应商（采购员手动录入报价信息，供负责人遴选）
-- PostgreSQL 14+

SET search_path TO procurement, public;

-- ── 供应商主数据 ─────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS supplier (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    supplier_no VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    short_name VARCHAR(64) NULL,
    contact_person VARCHAR(64) NULL,
    contact_phone VARCHAR(32) NULL,
    email VARCHAR(128) NULL,
    address VARCHAR(255) NULL,
    remark VARCHAR(500) NULL,
    application_id BIGINT NULL,
    status SMALLINT NOT NULL DEFAULT 1,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ NULL,
    CONSTRAINT uk_supplier_no UNIQUE (supplier_no),
    CONSTRAINT fk_supplier_application FOREIGN KEY (application_id)
        REFERENCES procurement_application (id)
);

COMMENT ON TABLE supplier IS '供应商主数据档案';
COMMENT ON COLUMN supplier.supplier_no IS '供应商编码（唯一）';
COMMENT ON COLUMN supplier.application_id IS '关联的采购申请单（procurement_application.id）';
COMMENT ON COLUMN supplier.status IS '状态：1启用 0停用';

CREATE INDEX IF NOT EXISTS idx_supplier_application
    ON supplier (application_id)
    WHERE deleted_at IS NULL;

-- ── 采购申请候选供应商（询价名单） ────────────────────────────────
CREATE TABLE IF NOT EXISTS procurement_application_supplier (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    application_id BIGINT NOT NULL,
    supplier_name VARCHAR(128) NOT NULL,
    contact_person VARCHAR(64) NULL,
    contact_phone VARCHAR(32) NULL,
    quoted_amount NUMERIC(18, 2) NULL,
    delivery_days INTEGER NULL,
    is_selected SMALLINT NOT NULL DEFAULT 0,
    remark VARCHAR(500) NULL,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ NULL,
    CONSTRAINT fk_app_supplier_application FOREIGN KEY (application_id)
        REFERENCES procurement_application (id)
);

CREATE INDEX IF NOT EXISTS idx_app_supplier_application
    ON procurement_application_supplier (application_id)
    WHERE deleted_at IS NULL;

COMMENT ON TABLE procurement_application_supplier IS '采购申请候选供应商/询价名单，由采购员手动录入';
COMMENT ON COLUMN procurement_application_supplier.supplier_name IS '供应商名称（当前阶段手动输入，不强制关联供应商档案）';
COMMENT ON COLUMN procurement_application_supplier.quoted_amount IS '该供应商报价总金额';
COMMENT ON COLUMN procurement_application_supplier.delivery_days IS '承诺交货周期（天）';
COMMENT ON COLUMN procurement_application_supplier.is_selected IS '是否被负责人选定：1是 0否';
