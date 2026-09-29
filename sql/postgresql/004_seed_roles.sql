-- 角色示例数据，便于本地开发测试
-- PostgreSQL 14+

INSERT INTO procurement.sys_role (
    code,
    name,
    description,
    status,
    created_at,
    updated_at
)
VALUES
    ('APPLICANT', '申请人', '发起采购申请', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('APPROVER', '审批人', '审批采购申请', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('BUYER', '采购员', '负责寻源和报价录入', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('PURCHASE_MANAGER', '采购负责人', '负责采购订单二次审批', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('FINANCE', '财务人员', '负责账单核对和付款处理', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('ADMIN', '管理员', '业务管理员，负责寻源和验收决策', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('INSPECTOR', '验收员', '负责采购验收', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;
