-- 供应商档案改为关联采购申请单（撤销上一版 purchaser_id，改为 application_id）
-- 每张供应商档案归属一张采购申请单，由采购员录入时选择对应的申请单

SET search_path TO procurement, public;

ALTER TABLE supplier DROP COLUMN IF EXISTS purchaser_id;

ALTER TABLE supplier
    ADD COLUMN IF NOT EXISTS application_id BIGINT NULL;

COMMENT ON COLUMN supplier.application_id IS '关联的采购申请单（procurement_application.id）';

ALTER TABLE supplier
    ADD CONSTRAINT fk_supplier_application FOREIGN KEY (application_id)
        REFERENCES procurement_application (id);

CREATE INDEX IF NOT EXISTS idx_supplier_application ON supplier (application_id) WHERE deleted_at IS NULL;
