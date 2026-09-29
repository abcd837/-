-- 采购申请模块最小数据库设计
-- PostgreSQL 14+

CREATE SCHEMA IF NOT EXISTS procurement;
SET search_path TO procurement, public;

CREATE TABLE IF NOT EXISTS sys_department (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    parent_id BIGINT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    status SMALLINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ NULL,
    CONSTRAINT uk_sys_department_code UNIQUE (code),
    CONSTRAINT fk_sys_department_parent FOREIGN KEY (parent_id)
        REFERENCES sys_department (id)
);

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_no VARCHAR(64) NOT NULL,
    username VARCHAR(64) NOT NULL,
    display_name VARCHAR(128) NOT NULL,
    department_id BIGINT NULL,
    email VARCHAR(128) NULL,
    mobile VARCHAR(32) NULL,
    password_hash VARCHAR(255) NOT NULL,
    status SMALLINT NOT NULL DEFAULT 1,
    last_login_at TIMESTAMPTZ NULL,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ NULL,
    CONSTRAINT uk_sys_user_user_no UNIQUE (user_no),
    CONSTRAINT uk_sys_user_username UNIQUE (username),
    CONSTRAINT fk_sys_user_department FOREIGN KEY (department_id)
        REFERENCES sys_department (id)
);

CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(255) NULL,
    status SMALLINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ NULL,
    CONSTRAINT uk_sys_role_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sys_user_role UNIQUE (user_id, role_id),
    CONSTRAINT fk_sys_user_role_user FOREIGN KEY (user_id)
        REFERENCES sys_user (id),
    CONSTRAINT fk_sys_user_role_role FOREIGN KEY (role_id)
        REFERENCES sys_role (id)
);

CREATE TABLE IF NOT EXISTS procurement_application (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    application_no VARCHAR(64) NOT NULL,
    title VARCHAR(255) NOT NULL,
    applicant_id BIGINT NOT NULL,
    department_id BIGINT NOT NULL,
    application_type VARCHAR(32) NOT NULL DEFAULT 'NORMAL',
    purpose TEXT NULL,
    budget_account_code VARCHAR(64) NULL,
    budget_account_name VARCHAR(128) NULL,
    budget_available_amount NUMERIC(18,2) NULL,
    total_amount NUMERIC(18,2) NULL,
    currency CHAR(3) NOT NULL DEFAULT 'CNY',
    required_date DATE NULL,
    risk_level VARCHAR(16) NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    submitted_at TIMESTAMPTZ NULL,
    approved_at TIMESTAMPTZ NULL,
    rejected_at TIMESTAMPTZ NULL,
    withdrawn_at TIMESTAMPTZ NULL,
    version INTEGER NOT NULL DEFAULT 1,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ NULL,
    CONSTRAINT uk_procurement_application_application_no UNIQUE (application_no),
    CONSTRAINT ck_procurement_application_status
        CHECK (status IN ('DRAFT', 'SUBMITTED', 'VALIDATING', 'PENDING_APPROVAL', 'APPROVED', 'REJECTED', 'WITHDRAWN')),
    CONSTRAINT ck_procurement_application_risk_level
        CHECK (risk_level IS NULL OR risk_level IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT fk_procurement_application_applicant FOREIGN KEY (applicant_id)
        REFERENCES sys_user (id),
    CONSTRAINT fk_procurement_application_department FOREIGN KEY (department_id)
        REFERENCES sys_department (id)
);

CREATE TABLE IF NOT EXISTS procurement_application_item (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    application_id BIGINT NOT NULL,
    line_no INTEGER NOT NULL,
    material_code VARCHAR(64) NULL,
    material_name VARCHAR(255) NOT NULL,
    specification VARCHAR(255) NULL,
    unit VARCHAR(32) NULL,
    quantity NUMERIC(18,4) NOT NULL DEFAULT 0,
    estimated_unit_price NUMERIC(18,4) NULL,
    estimated_amount NUMERIC(18,2) NULL,
    required_date DATE NULL,
    suggest_supplier_code VARCHAR(64) NULL,
    suggest_supplier_name VARCHAR(255) NULL,
    remark TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ NULL,
    CONSTRAINT uk_procurement_application_item_line UNIQUE (application_id, line_no),
    CONSTRAINT fk_procurement_application_item_application FOREIGN KEY (application_id)
        REFERENCES procurement_application (id)
);

CREATE TABLE IF NOT EXISTS procurement_application_validation_result (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    application_id BIGINT NOT NULL,
    agent_task_id VARCHAR(64) NULL,
    model_name VARCHAR(128) NULL,
    model_version VARCHAR(64) NULL,
    overall_risk_level VARCHAR(16) NULL,
    overall_result VARCHAR(32) NULL,
    summary TEXT NULL,
    suggestion TEXT NULL,
    confidence NUMERIC(5,4) NULL,
    raw_result JSONB NULL,
    executed_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_validation_result_risk_level
        CHECK (overall_risk_level IS NULL OR overall_risk_level IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT ck_validation_result_overall_result
        CHECK (overall_result IS NULL OR overall_result IN ('PASS', 'NEED_REVIEW', 'REJECT', 'NA')),
    CONSTRAINT fk_validation_result_application FOREIGN KEY (application_id)
        REFERENCES procurement_application (id)
);

CREATE TABLE IF NOT EXISTS procurement_application_validation_item (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    validation_result_id BIGINT NOT NULL,
    item_code VARCHAR(64) NOT NULL,
    item_name VARCHAR(128) NULL,
    result VARCHAR(32) NULL,
    risk_level VARCHAR(16) NULL,
    description TEXT NULL,
    evidence JSONB NULL,
    suggestion TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_validation_item_result_code UNIQUE (validation_result_id, item_code),
    CONSTRAINT ck_validation_item_result
        CHECK (result IS NULL OR result IN ('PASS', 'NEED_REVIEW', 'REJECT', 'NA')),
    CONSTRAINT ck_validation_item_risk_level
        CHECK (risk_level IS NULL OR risk_level IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT fk_validation_item_result FOREIGN KEY (validation_result_id)
        REFERENCES procurement_application_validation_result (id)
);

CREATE TABLE IF NOT EXISTS approval_record (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    business_type VARCHAR(64) NOT NULL,
    business_id BIGINT NOT NULL,
    node_code VARCHAR(64) NULL,
    node_name VARCHAR(128) NULL,
    operator_id BIGINT NOT NULL,
    operator_name VARCHAR(128) NULL,
    role_code VARCHAR(64) NULL,
    action VARCHAR(32) NOT NULL,
    comment TEXT NULL,
    from_status VARCHAR(32) NULL,
    to_status VARCHAR(32) NULL,
    approved_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_approval_record_operator FOREIGN KEY (operator_id)
        REFERENCES sys_user (id)
);

CREATE INDEX IF NOT EXISTS idx_sys_user_department_id ON sys_user (department_id);
CREATE INDEX IF NOT EXISTS idx_sys_user_status ON sys_user (status);
CREATE INDEX IF NOT EXISTS idx_sys_user_role_user_id ON sys_user_role (user_id);
CREATE INDEX IF NOT EXISTS idx_sys_user_role_role_id ON sys_user_role (role_id);

CREATE INDEX IF NOT EXISTS idx_procurement_application_applicant_id
    ON procurement_application (applicant_id);
CREATE INDEX IF NOT EXISTS idx_procurement_application_department_id
    ON procurement_application (department_id);
CREATE INDEX IF NOT EXISTS idx_procurement_application_status
    ON procurement_application (status);
CREATE INDEX IF NOT EXISTS idx_procurement_application_created_at
    ON procurement_application (created_at);

CREATE INDEX IF NOT EXISTS idx_procurement_application_item_material_code
    ON procurement_application_item (material_code);

CREATE INDEX IF NOT EXISTS idx_validation_result_application_id
    ON procurement_application_validation_result (application_id);
CREATE INDEX IF NOT EXISTS idx_validation_result_risk_level
    ON procurement_application_validation_result (overall_risk_level);

CREATE INDEX IF NOT EXISTS idx_validation_item_result_id
    ON procurement_application_validation_item (validation_result_id);

CREATE INDEX IF NOT EXISTS idx_approval_record_business
    ON approval_record (business_type, business_id);
CREATE INDEX IF NOT EXISTS idx_approval_record_operator_id
    ON approval_record (operator_id);
CREATE INDEX IF NOT EXISTS idx_approval_record_created_at
    ON approval_record (created_at);
