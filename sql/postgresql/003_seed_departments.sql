-- 部门示例数据，便于本地开发测试
-- PostgreSQL 14+

INSERT INTO procurement.sys_department (
    code,
    name,
    parent_id,
    sort_order,
    status,
    created_at,
    updated_at
)
VALUES
    ('DEPT-GM', '总经办', NULL, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('DEPT-PUR', '采购部', NULL, 2, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('DEPT-FIN', '财务部', NULL, 3, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('DEPT-IT', '技术部', NULL, 4, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('DEPT-ADMIN', '行政部', NULL, 5, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('DEPT-PROD', '生产部', NULL, 6, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;
