# 接口示例：从申请到四级审批

以下多行命令使用 **Bash / Git Bash / WSL** 的变量和反斜杠续行语法，不能直接粘贴进 PowerShell。Windows 原生验收请在项目根目录运行 `powershell -ExecutionPolicy Bypass -File .\scripts\api-smoke.ps1`。后端默认地址为 `http://localhost:8088`。

响应统一为：

```json
{
  "success": true,
  "code": "OK",
  "message": "success",
  "data": {}
}
```

业务失败时 `success=false`，同时使用合适的 HTTP 状态码。下面用环境变量简化命令：

```bash
export BASE_URL=http://localhost:8088
export JSON='Content-Type: application/json'
```

> **警告：示例请求头是可任意伪造的教学认证，严禁用于生产环境。**

金额建议用字符串提交，如 `"amount":"150000.00"`；响应中的 `amount` 始终为十进制字符串，避免浏览器浮点精度损失。所有示例中的数字 id/version 只是说明，必须替换为实际响应值。所有四级审批员均属于 D001，ALL 不具有通配含义。

| 方法 | 路径（/api 前缀） | 作用 |
|---|---|---|
| GET | /me | 当前演示身份 |
| POST / GET | /transactions | 创建草稿 / 条件分页查询 |
| GET | /transactions/{id} | 脱敏详情 |
| POST | /transactions/{id}/submit | 草稿提交，body 含 version |
| POST | /transactions/{id}/resubmit | 驳回后完整字段修改并重提 |
| POST | /transactions/{id}/approve | 本级通过，body 含 version/comment |
| POST | /transactions/{id}/reject | 本级驳回，body 含 version/reason |
| GET | /transactions/{id}/approvals | 审批历史 |
| GET | /transactions/{id}/audits | 成功操作审计历史 |

## 1. 查看当前演示身份

```bash
curl -s "$BASE_URL/api/me" \
  -H 'X-User-Id: applicant01' \
  -H 'X-User-Name: applicant01' \
  -H 'X-Role: APPLICANT' \
  -H 'X-Dept-Id: D001'
```

## 2. 创建草稿

```bash
curl -s -X POST "$BASE_URL/api/transactions" \
  -H "$JSON" \
  -H 'X-User-Id: applicant01' \
  -H 'X-User-Name: applicant01' \
  -H 'X-Role: APPLICANT' \
  -H 'X-Dept-Id: D001' \
  -d '{
    "mobile": "13800138000",
    "idCard": "110101199001011234",
    "account": "6222020200001234567",
    "counterpartyAccount": "6217000012345678901",
    "amount": 150000.00,
    "currency": "CNY",
    "purpose": "供应商设备采购款"
  }'
```

记下返回的 `data.id`、`data.applicationNo` 和 `data.version`。下文假设 `id=1`、初始 `version=0`。实际调用必须使用接口返回值。

创建后检查数据库时，`mobile_ciphertext`、`id_card_ciphertext`、`account_ciphertext` 和 `counterparty_account_ciphertext` 应为 Base64 密文，而不是上述明文。

## 3. 查询与详情

申请人查询自己的记录：

```bash
curl -s "$BASE_URL/api/transactions?page=1&size=10&status=DRAFT&keyword=" \
  -H 'X-User-Id: applicant01' \
  -H 'X-User-Name: applicant01' \
  -H 'X-Role: APPLICANT' \
  -H 'X-Dept-Id: D001'
```

支持的条件为 `page`、`size`、`keyword`、`status`、`startDate`、`endDate`。日期格式由后端校验；常用示例是 `2026-10-01`。

```bash
curl -s "$BASE_URL/api/transactions/1" \
  -H 'X-User-Id: applicant01' \
  -H 'X-User-Name: applicant01' \
  -H 'X-Role: APPLICANT' \
  -H 'X-Dept-Id: D001'
```

敏感数据响应应为脱敏值，不应原样返回请求体中的手机号、身份证号或账号。

## 4. 提交申请

```bash
curl -i -X POST "$BASE_URL/api/transactions/1/submit" \
  -H "$JSON" \
  -H 'X-User-Id: applicant01' \
  -H 'X-User-Name: applicant01' \
  -H 'X-Role: APPLICANT' \
  -H 'X-Dept-Id: D001' \
  -d '{"version": 0}'
```

成功后状态为 `PENDING_L1`，`currentLevel=1`，版本通常变为 `1`；提交动作会计算风险分和风险级别。

## 5. 四级逐级通过

### 一级审批

```bash
curl -i -X POST "$BASE_URL/api/transactions/1/approve" \
  -H "$JSON" \
  -H 'X-User-Id: approver01' \
  -H 'X-User-Name: approver01' \
  -H 'X-Role: APPROVER_L1' \
  -H 'X-Dept-Id: D001' \
  -d '{"version": 1, "comment": "业务要素已核对"}'
```

成功后：`PENDING_L2`、`currentLevel=2`、`version=2`。

### 二级审批

```bash
curl -i -X POST "$BASE_URL/api/transactions/1/approve" \
  -H "$JSON" \
  -H 'X-User-Id: approver02' \
  -H 'X-User-Name: approver02' \
  -H 'X-Role: APPROVER_L2' \
  -H 'X-Dept-Id: D001' \
  -d '{"version": 2, "comment": "复核通过"}'
```

成功后：`PENDING_L3`、`currentLevel=3`、`version=3`。

### 三级审批

```bash
curl -i -X POST "$BASE_URL/api/transactions/1/approve" \
  -H "$JSON" \
  -H 'X-User-Id: approver03' \
  -H 'X-User-Name: approver03' \
  -H 'X-Role: APPROVER_L3' \
  -H 'X-Dept-Id: D001' \
  -d '{"version": 3, "comment": "风险复核通过"}'
```

成功后：`PENDING_L4`、`currentLevel=4`、`version=4`。

### 四级审批

```bash
curl -i -X POST "$BASE_URL/api/transactions/1/approve" \
  -H "$JSON" \
  -H 'X-User-Id: approver04' \
  -H 'X-User-Name: approver04' \
  -H 'X-Role: APPROVER_L4' \
  -H 'X-Dept-Id: D001' \
  -d '{"version": 4, "comment": "终审通过"}'
```

成功后：`APPROVED`、`currentLevel=0`、`version=5`。

## 6. 驳回与重提分支

另建一笔申请并提交，假设它的 `id=2`、当前 `PENDING_L1`、`version=1`。

```bash
curl -i -X POST "$BASE_URL/api/transactions/2/reject" \
  -H "$JSON" \
  -H 'X-User-Id: approver01' \
  -H 'X-User-Name: approver01' \
  -H 'X-Role: APPROVER_L1' \
  -H 'X-Dept-Id: D001' \
  -d '{"version": 1, "reason": "用途说明不充分，请补充合同背景"}'
```

申请人修改完整业务字段并重提。`resubmit` 是一个原子业务命令，不需要先发送 PUT；它只允许原申请人处理 `REJECTED` 记录，并重新评分：

```bash
curl -i -X POST "$BASE_URL/api/transactions/2/resubmit" \
  -H "$JSON" \
  -H 'X-User-Id: applicant01' \
  -H 'X-User-Name: applicant01' \
  -H 'X-Role: APPLICANT' \
  -H 'X-Dept-Id: D001' \
  -d '{
    "version": 2,
    "mobile": "13800138000",
    "idCard": "110101199001011234",
    "account": "6222020200001234567",
    "counterpartyAccount": "6217000012345678901",
    "amount": 150000.00,
    "currency": "CNY",
    "purpose": "供应商设备采购款，合同编号 HT-2026-001"
  }'
```

成功后重新进入 `PENDING_L1`，`rejectionReason` 被清理，并使用新的 `version` 继续审批。

## 7. 审批记录与审计记录

```bash
curl -s "$BASE_URL/api/transactions/1/approvals" \
  -H 'X-User-Id: auditor01' \
  -H 'X-User-Name: auditor01' \
  -H 'X-Role: AUDITOR' \
  -H 'X-Dept-Id: D001'

curl -s "$BASE_URL/api/transactions/1/audits" \
  -H 'X-User-Id: auditor01' \
  -H 'X-User-Name: auditor01' \
  -H 'X-Role: AUDITOR' \
  -H 'X-Dept-Id: D001'
```

检查响应：审计详情不能包含完整手机号、身份证号、账号、密钥或原始请求体。

## 8. 演示 `409 Conflict`

两个审批员页面都读取到同一笔 `PENDING_L1`，版本均为 `1`。第一个请求成功：

```bash
curl -i -X POST "$BASE_URL/api/transactions/3/approve" \
  -H "$JSON" \
  -H 'X-User-Id: approver01' -H 'X-User-Name: approver01' \
  -H 'X-Role: APPROVER_L1' -H 'X-Dept-Id: D001' \
  -d '{"version": 1, "comment": "第一次处理"}'
```

第二个请求仍携带旧版本 `1`：

```bash
curl -i -X POST "$BASE_URL/api/transactions/3/approve" \
  -H "$JSON" \
  -H 'X-User-Id: approver01' -H 'X-User-Name: approver01' \
  -H 'X-Role: APPROVER_L1' -H 'X-Dept-Id: D001' \
  -d '{"version": 1, "comment": "旧页面重复处理"}'
```

预期第二次返回 HTTP `409`。刷新详情后可看到它已经进入 `PENDING_L2`，一级审批不能再次执行。

## 9. 权限负向示例

用 D002 的一级审批员尝试处理 D001 申请：

```bash
curl -i -X POST "$BASE_URL/api/transactions/3/approve" \
  -H "$JSON" \
  -H 'X-User-Id: approverD002' -H 'X-User-Name: approverD002' \
  -H 'X-Role: APPROVER_L1' -H 'X-Dept-Id: D002' \
  -d '{"version": 1, "comment": "跨部门尝试"}'
```

预期为 `403 Forbidden`，且数据库不产生审批记录。

## 10. 错误约定

| HTTP | code 示例 | 含义 |
|---|---|---|
| 400 | VALIDATION_ERROR / INVALID_ROLE / ILLEGAL_STATE | 参数非法、未知演示角色、非法状态迁移 |
| 401 | UNAUTHENTICATED | 缺少显式演示身份头，或关闭演示认证后尚未接入身份提供方 |
| 403 | FORBIDDEN / DATA_SCOPE_DENIED / SEPARATION_OF_DUTIES | 角色、本人/部门或职责分离不满足 |
| 404 | NOT_FOUND | 申请不存在 |
| 409 | CONCURRENT_MODIFICATION | version 过期或条件更新失败，需刷新读取最新数据 |
| 500 | INTERNAL_ERROR | 未预期故障，前端不显示内部异常详情 |

错误结构为 `{"success":false,"code":"...","message":"..."}`；字段校验失败还会携带 `data` 字段错误映射。不要因收到 409 就自动使用新版本重发审批，必须让用户重新查看后确认。

