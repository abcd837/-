-- 采购申请模块最小数据库设计
-- MySQL 8.0+

CREATE DATABASE IF NOT EXISTS smart_procurement
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE smart_procurement;

CREATE TABLE IF NOT EXISTS sys_department (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    parent_id BIGINT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted_at DATETIME(3) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_department_code (code),
    KEY idx_sys_department_parent_id (parent_id),
    CONSTRAINT fk_sys_department_parent
        FOREIGN KEY (parent_id) REFERENCES sys_department (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_no VARCHAR(64) NOT NULL,
    username VARCHAR(64) NOT NULL,
    display_name VARCHAR(128) NOT NULL,
    department_id BIGINT NULL,
    email VARCHAR(128) NULL,
    mobile VARCHAR(32) NULL,
    password_hash VARCHAR(255) NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    last_login_at DATETIME(3) NULL,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted_at DATETIME(3) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_user_user_no (user_no),
    UNIQUE KEY uk_sys_user_username (username),
    KEY idx_sys_user_department_id (department_id),
    KEY idx_sys_user_status (status),
    CONSTRAINT fk_sys_user_department
        FOREIGN KEY (department_id) REFERENCES sys_department (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(255) NULL,
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted_at DATETIME(3) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_role_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_user_role (user_id, role_id),
    KEY idx_sys_user_role_user_id (user_id),
    KEY idx_sys_user_role_role_id (role_id),
    CONSTRAINT fk_sys_user_role_user
        FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_sys_user_role_role
        FOREIGN KEY (role_id) REFERENCES sys_role (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS procurement_application (
    id BIGINT NOT NULL AUTO_INCREMENT,
    application_no VARCHAR(64) NOT NULL,
    title VARCHAR(255) NOT NULL,
    applicant_id BIGINT NOT NULL,
    department_id BIGINT NOT NULL,
    application_type VARCHAR(32) NOT NULL DEFAULT 'NORMAL',
    purpose TEXT NULL,
    budget_account_code VARCHAR(64) NULL,
    budget_account_name VARCHAR(128) NULL,
    budget_available_amount DECIMAL(18,2) NULL,
    total_amount DECIMAL(18,2) NULL,
    currency CHAR(3) NOT NULL DEFAULT 'CNY',
    required_date DATE NULL,
    risk_level VARCHAR(16) NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    submitted_at DATETIME(3) NULL,
    approved_at DATETIME(3) NULL,
    rejected_at DATETIME(3) NULL,
    withdrawn_at DATETIME(3) NULL,
    version INT NOT NULL DEFAULT 1,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted_at DATETIME(3) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_procurement_application_application_no (application_no),
    KEY idx_procurement_application_applicant_id (applicant_id),
    KEY idx_procurement_application_department_id (department_id),
    KEY idx_procurement_application_status (status),
    KEY idx_procurement_application_created_at (created_at),
    CONSTRAINT ck_procurement_application_status
        CHECK (status IN ('DRAFT', 'SUBMITTED', 'VALIDATING', 'PENDING_APPROVAL', 'APPROVED', 'REJECTED', 'WITHDRAWN')),
    CONSTRAINT ck_procurement_application_risk_level
        CHECK (risk_level IS NULL OR risk_level IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT fk_procurement_application_applicant
        FOREIGN KEY (applicant_id) REFERENCES sys_user (id),
    CONSTRAINT fk_procurement_application_department
        FOREIGN KEY (department_id) REFERENCES sys_department (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS procurement_application_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    application_id BIGINT NOT NULL,
    line_no INT NOT NULL,
    material_code VARCHAR(64) NULL,
    material_name VARCHAR(255) NOT NULL,
    specification VARCHAR(255) NULL,
    unit VARCHAR(32) NULL,
    quantity DECIMAL(18,4) NOT NULL DEFAULT 0,
    estimated_unit_price DECIMAL(18,4) NULL,
    estimated_amount DECIMAL(18,2) NULL,
    required_date DATE NULL,
    suggest_supplier_code VARCHAR(64) NULL,
    suggest_supplier_name VARCHAR(255) NULL,
    remark TEXT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted_at DATETIME(3) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_procurement_application_item_line (application_id, line_no),
    KEY idx_procurement_application_item_material_code (material_code),
    CONSTRAINT fk_procurement_application_item_application
        FOREIGN KEY (application_id) REFERENCES procurement_application (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS procurement_application_validation_result (
    id BIGINT NOT NULL AUTO_INCREMENT,
    application_id BIGINT NOT NULL,
    agent_task_id VARCHAR(64) NULL,
    model_name VARCHAR(128) NULL,
    model_version VARCHAR(64) NULL,
    overall_risk_level VARCHAR(16) NULL,
    overall_result VARCHAR(32) NULL,
    summary TEXT NULL,
    suggestion TEXT NULL,
    confidence DECIMAL(5,4) NULL,
    raw_result JSON NULL,
    executed_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_validation_result_application_id (application_id),
    KEY idx_validation_result_risk_level (overall_risk_level),
    CONSTRAINT ck_validation_result_risk_level
        CHECK (overall_risk_level IS NULL OR overall_risk_level IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT ck_validation_result_overall_result
        CHECK (overall_result IS NULL OR overall_result IN ('PASS', 'NEED_REVIEW', 'REJECT', 'NA')),
    CONSTRAINT fk_validation_result_application
        FOREIGN KEY (application_id) REFERENCES procurement_application (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS procurement_application_validation_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    validation_result_id BIGINT NOT NULL,
    item_code VARCHAR(64) NOT NULL,
    item_name VARCHAR(128) NULL,
    result VARCHAR(32) NULL,
    risk_level VARCHAR(16) NULL,
    description TEXT NULL,
    evidence JSON NULL,
    suggestion TEXT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_validation_item_result_code (validation_result_id, item_code),
    KEY idx_validation_item_result_id (validation_result_id),
    CONSTRAINT ck_validation_item_result
        CHECK (result IS NULL OR result IN ('PASS', 'NEED_REVIEW', 'REJECT', 'NA')),
    CONSTRAINT ck_validation_item_risk_level
        CHECK (risk_level IS NULL OR risk_level IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT fk_validation_item_result
        FOREIGN KEY (validation_result_id) REFERENCES procurement_application_validation_result (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS approval_record (
    id BIGINT NOT NULL AUTO_INCREMENT,
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
    approved_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_approval_record_business (business_type, business_id),
    KEY idx_approval_record_operator_id (operator_id),
    KEY idx_approval_record_created_at (created_at),
    CONSTRAINT fk_approval_record_operator
        FOREIGN KEY (operator_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
