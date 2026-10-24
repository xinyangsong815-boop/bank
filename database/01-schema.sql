-- 银行交易申请与四级审批教学项目（MySQL 8.4）
-- 本脚本不删除既有表或数据。生产环境应交由 Flyway/Liquibase 管理变更。
SET NAMES utf8mb4;
CREATE DATABASE IF NOT EXISTS bank_approval
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE bank_approval;

CREATE TABLE IF NOT EXISTS sys_user (
  id            VARCHAR(32)  NOT NULL COMMENT '演示用户ID，同时作为 X-User-Id',
  username      VARCHAR(64)  NOT NULL COMMENT '登录名（本项目不使用密码登录）',
  display_name  VARCHAR(64)  NOT NULL COMMENT '显示名称',
  role_code     VARCHAR(32)  NOT NULL COMMENT 'APPLICANT/APPROVER_L1..L4/AUDITOR/ADMIN',
  department_id VARCHAR(32)  NOT NULL COMMENT '数据权限所属部门',
  enabled       TINYINT(1)   NOT NULL DEFAULT 1,
  created_at    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_user_username (username),
  KEY idx_sys_user_role_dept (role_code, department_id),
  CONSTRAINT ck_sys_user_role CHECK (
    role_code IN ('APPLICANT', 'APPROVER_L1', 'APPROVER_L2', 'APPROVER_L3',
                  'APPROVER_L4', 'AUDITOR', 'ADMIN')
  )
) ENGINE=InnoDB COMMENT='演示身份目录；当前过滤器不读取本表，不能替代身份系统';

CREATE TABLE IF NOT EXISTS transaction_application (
  id                              BIGINT       NOT NULL AUTO_INCREMENT,
  application_no                  VARCHAR(64)  NOT NULL COMMENT '业务申请编号',
  applicant_id                    VARCHAR(32)  NOT NULL,
  applicant_name                  VARCHAR(64)  NOT NULL,
  department_id                   VARCHAR(32)  NOT NULL COMMENT '申请人部门，供数据权限过滤',
  mobile_ciphertext               TEXT         NOT NULL COMMENT 'AES-GCM Base64(iv+ciphertext+tag)',
  id_card_ciphertext              TEXT         NOT NULL COMMENT 'AES-GCM Base64(iv+ciphertext+tag)',
  account_ciphertext              TEXT         NOT NULL COMMENT '付款账号密文',
  counterparty_account_ciphertext TEXT         NOT NULL COMMENT '收款账号密文',
  amount                          DECIMAL(18,2) NOT NULL,
  currency                        VARCHAR(3)   NOT NULL DEFAULT 'CNY',
  purpose                         VARCHAR(500) NOT NULL,
  risk_score                      INT          NOT NULL DEFAULT 0,
  risk_level                      VARCHAR(16)  NOT NULL DEFAULT 'LOW',
  status                          VARCHAR(32)  NOT NULL DEFAULT 'DRAFT',
  current_level                   INT          NOT NULL DEFAULT 0 COMMENT '0=草稿/终态，1..4=当前待审批级别',
  rejection_reason                VARCHAR(500) NULL,
  version                         BIGINT       NOT NULL DEFAULT 0 COMMENT '乐观锁版本；更新条件必须含 id+version',
  created_at                      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at                      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_transaction_application_no (application_no),
  KEY idx_transaction_applicant (applicant_id, created_at),
  KEY idx_transaction_dept_status (department_id, status, created_at),
  KEY idx_transaction_status_level (status, current_level, created_at),
  -- 不对 applicant_id 建 sys_user 外键：演示身份来自 Header，生产身份通常来自独立 IAM，
  -- 用户生命周期不应阻塞历史交易留存。数据权限仍由服务端查询条件强制执行。
  CONSTRAINT ck_transaction_amount CHECK (amount > 0),
  CONSTRAINT ck_transaction_currency CHECK (CHAR_LENGTH(currency) = 3),
  CONSTRAINT ck_transaction_risk_score CHECK (risk_score BETWEEN 0 AND 100),
  CONSTRAINT ck_transaction_risk_level CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH')),
  CONSTRAINT ck_transaction_status CHECK (
    status IN ('DRAFT', 'PENDING_L1', 'PENDING_L2', 'PENDING_L3', 'PENDING_L4', 'APPROVED', 'REJECTED')
  ),
  CONSTRAINT ck_transaction_level CHECK (current_level BETWEEN 0 AND 4)
) ENGINE=InnoDB COMMENT='交易申请主表；敏感字段仅保存认证加密后的密文';

CREATE TABLE IF NOT EXISTS approval_record (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  transaction_id  BIGINT       NOT NULL,
  level           INT          NOT NULL COMMENT '本次操作对应审批级别 1..4',
  approver_id     VARCHAR(32)  NOT NULL,
  approver_name   VARCHAR(64)  NOT NULL,
  action          VARCHAR(16)  NOT NULL COMMENT 'APPROVE/REJECT',
  status_before   VARCHAR(32)  NOT NULL,
  status_after    VARCHAR(32)  NOT NULL,
  comment         VARCHAR(500) NULL,
  created_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_approval_transaction_time (transaction_id, created_at),
  KEY idx_approval_actor_time (approver_id, created_at),
  CONSTRAINT fk_approval_transaction FOREIGN KEY (transaction_id)
    REFERENCES transaction_application (id),
  CONSTRAINT ck_approval_level CHECK (level BETWEEN 1 AND 4),
  CONSTRAINT ck_approval_action CHECK (action IN ('APPROVE', 'REJECT'))
) ENGINE=InnoDB COMMENT='审批事实记录，只追加不修改';

CREATE TABLE IF NOT EXISTS audit_log (
  id              BIGINT        NOT NULL AUTO_INCREMENT,
  transaction_id  BIGINT        NULL COMMENT '非交易类审计允许为空',
  operator_id     VARCHAR(32)   NOT NULL,
  operator_name   VARCHAR(64)   NOT NULL,
  operation       VARCHAR(64)   NOT NULL,
  detail          VARCHAR(2000) NOT NULL COMMENT '脱敏后的审计详情；禁止写入敏感明文/密钥',
  ip_address      VARCHAR(64)   NULL,
  created_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_audit_transaction_time (transaction_id, created_at),
  KEY idx_audit_actor_time (operator_id, created_at),
  KEY idx_audit_operation_time (operation, created_at),
  CONSTRAINT fk_audit_transaction FOREIGN KEY (transaction_id)
    REFERENCES transaction_application (id)
) ENGINE=InnoDB COMMENT='不可记录敏感明文的审计日志';

