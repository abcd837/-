-- 修复角色编码为英文（和代码注解保持一致）
-- 在 pgAdmin Query Tool 中执行即可

-- 1. 先看一下当前数据库里到底有哪些角色
SELECT id, code, name, description FROM procurement.sys_role ORDER BY id;

-- 2. 清掉用户-角色关联（因为后面要删 role）
DELETE FROM procurement.sys_user_role;

-- 3. 清掉旧角色数据
DELETE FROM procurement.sys_role;

-- 4. 重新插入英文编码的角色种子
INSERT INTO procurement.sys_role (
    code, name, description, status, created_at, updated_at
) VALUES
    ('APPLICANT',       '申请人',     '发起采购申请',                       1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('APPROVER',        '审批人',     '审批采购申请',                       1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('BUYER',           '采购员',     '负责寻源和报价录入',                 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('PURCHASE_MANAGER','采购负责人', '负责采购订单二次审批',               1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('FINANCE',         '财务人员',   '负责账单核对和付款处理',             1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('ADMIN',           '管理员',     '业务管理员，负责寻源和验收决策',     1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('INSPECTOR',       '验收员',     '负责采购验收',                       1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 5. 把 admin 用户重新关联角色（admin 一般 user_no 是 admin 或 id=1）
-- 先查一下 admin 用户的 id
SELECT id, username, user_no, display_name FROM procurement.sys_user WHERE deleted_at IS NULL;
-- 然后根据上面查到的 user id 执行下面的 INSERT（把 1 改成实际的 admin user id）
INSERT INTO procurement.sys_user_role (user_id, role_id)
SELECT u.id, r.id
FROM procurement.sys_user u, procurement.sys_role r
WHERE u.username = 'admin'
  AND r.code IN ('APPROVER', 'ADMIN');

-- 6. 可以酌情给其他用户分配角色
-- 比如给另一个测试用户：
-- INSERT INTO procurement.sys_user_role (user_id, role_id)
-- SELECT u.id, r.id FROM procurement.sys_user u, procurement.sys_role r
-- WHERE u.username = 'testuser' AND r.code = 'APPLICANT';

-- 验证：重新查一下现在的角色
SELECT r.id, r.code, r.name FROM procurement.sys_role r ORDER BY r.id;
