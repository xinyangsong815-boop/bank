-- 可重复执行的本地演示数据。
-- 演示认证直接信任请求头，本表不参与认证/授权；仅对应前端的身份目录，严禁生产使用。
SET NAMES utf8mb4;
USE bank_approval;

INSERT INTO sys_user (id, username, display_name, role_code, department_id, enabled)
VALUES
  ('applicant01', 'applicant01', '申请人张三', 'APPLICANT', 'D001', 1),
  ('applicant02', 'applicant02', 'D002申请人', 'APPLICANT', 'D002', 1),
  ('approver01', 'approver01', '一级审批李经理', 'APPROVER_L1', 'D001', 1),
  ('approverD002', 'approverD002', 'D002一级审批员', 'APPROVER_L1', 'D002', 1),
  ('approver02', 'approver02', '二级审批王总监', 'APPROVER_L2', 'D001', 1),
  ('approver03', 'approver03', '三级审批赵行长', 'APPROVER_L3', 'D001', 1),
  ('approver04', 'approver04', '四级审批周委员', 'APPROVER_L4', 'D001', 1),
  ('auditor01', 'auditor01', '审计员陈审计', 'AUDITOR', 'AUDIT', 1),
  ('admin01', 'admin01', '系统管理员', 'ADMIN', 'HQ', 1)
ON DUPLICATE KEY UPDATE
  username = VALUES(username),
  display_name = VALUES(display_name),
  role_code = VALUES(role_code),
  department_id = VALUES(department_id),
  enabled = VALUES(enabled);

-- 不直接插入交易演示数据：AES-GCM 每次使用随机 IV，只有应用经配置密钥加密后
-- 生成的密文才能可靠解密。请按 docs/API_EXAMPLES.md 通过 API 创建首笔申请。

