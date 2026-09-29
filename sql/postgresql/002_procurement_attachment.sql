-- 采购申请附件表
-- PostgreSQL 14+

CREATE TABLE IF NOT EXISTS procurement.procurement_attachment (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    business_type VARCHAR(64) NOT NULL,
    business_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    stored_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL DEFAULT 0,
    content_type VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_procurement_attachment_business
    ON procurement.procurement_attachment (business_type, business_id);
