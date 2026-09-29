-- 采购申请加审批人字段
ALTER TABLE procurement.procurement_application
    ADD COLUMN approver_id BIGINT;

COMMENT ON COLUMN procurement.procurement_application.approver_id
    IS '指定的审批人 userId';
