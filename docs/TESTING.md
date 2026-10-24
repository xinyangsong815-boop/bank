# 测试与验收说明

## 1. 自动化测试层次

已实现 15 项 JUnit 5 自动化测试，覆盖以下层次：

1. 纯单元测试：状态转换、风险策略/责任链、脱敏函数、AES-GCM 往返与篡改检测。
2. Service 集成测试：真实 Repository，验证四级审批、驳回重提；另用审计 Repository 故障桩验证真实事务回滚。
3. MyBatis 测试：用 H2 MySQL 兼容模式验证 XML、条件更新；两个线程真实竞争时恰好一次成功。
4. Controller 集成测试：MockMvc 验证校验错误、统一响应、角色保护和 HTTP 409。
5. 前端提供生产构建验收；浏览器业务验收与真实 HTTP 脚本见下文（未提供前端单元测试套件）。

H2 能提高本地速度，但不能完全代表 MySQL 8.4 的 JSON、排序规则、锁和日期行为；发布前至少运行一次真实 MySQL 集成测试。

本次交付还用 **MySQL 8.4.0 临时实例 + Java 8 可执行 JAR** 完成了 `scripts/api-smoke.ps1` 全流程；具体证据与环境见 `VERIFICATION.md`。Docker Compose 配置已通过静态校验，Docker 容器自身未实测启动。

## 2. 后端测试

在 `backend` 目录执行：

```bash
mvn clean test
mvn clean package
```

若仓库带 Maven Wrapper，优先使用：

```bash
sh mvnw clean verify
```

关键测试矩阵：

| 场景 | 期望 |
|---|---|
| 正数金额、合法字段创建 | 201/200，状态 DRAFT，密文落库 |
| 金额为 0、身份证/手机号非法 | 400，返回字段级错误 |
| 申请人查询他人记录 | 403/404，不泄露详情 |
| D001 一级审批员处理 D002 | 403/404，无状态变化 |
| L2 处理 PENDING_L1 | 业务冲突/禁止，无审批记录 |
| 同版本并发审批 | 恰好一次成功，另一次 409 |
| 审批写审计失败 | 整个事务回滚，主表不变化 |
| 任一级驳回 | REJECTED，原因保留，审批+审计存在 |
| 非原申请人重提 | 403/404 |
| AES-GCM 密文被改 1 bit | 解密失败，不能返回伪造明文 |
| 相同明文加密两次 | 密文不同，但均可解密 |
| 列表/审计响应 | 无完整敏感字段 |

## 3. 前端构建

在 `frontend` 目录执行：

```bash
npm ci --ignore-scripts
npm run build
```

如项目提供 lint/test 脚本，再执行：

```bash
npm run lint
npm run test:unit
```

验收点：

- 切换身份后请求头随之变化，并持续显示“仅演示、禁止生产”警告。
- 申请人只看到自己的创建、提交、重提操作；审批员只看到当前级别操作。
- 详情展示四级进度、风险分、风险等级、审批记录和审计时间线。
- 409 弹窗明确提示数据已被他人处理，并刷新详情而不是覆盖数据。
- API 错误不会让按钮永久 loading；表单错误可定位到字段。
- 构建产物不包含真实密钥、生产口令或开发代理地址。

## 4. MySQL 8.4 烟雾测试

在项目根目录执行：

```bash
docker compose up -d mysql
docker compose ps
```

等待 health 状态为 `healthy`，检查初始化：

```bash
docker compose exec mysql mysql \
  -ubank_app -pbank_app_ChangeMe bank_approval \
  -e "SHOW TABLES; SELECT id, role_code, department_id FROM sys_user ORDER BY id;"
```

启动后端并按 `API_EXAMPLES.md` 完成一条四级审批。随后检查：

```bash
docker compose exec mysql mysql \
  -ubank_app -pbank_app_ChangeMe bank_approval \
  -e "SELECT id,application_no,status,current_level,version FROM transaction_application; SELECT transaction_id,level,action,status_after FROM approval_record ORDER BY id;"
```

不要在共享终端或 CI 日志中查询/打印密文字段。即使是密文，也可能成为离线分析和流量关联材料。

## 5. 手工验收剧本

### 正向流程

1. 申请人 applicant01 创建并提交一笔交易。
2. approver01、approver02、approver03、approver04 顺序通过（均为 D001）。
3. 确认最终 `APPROVED`，四条审批记录按时间有序，审计包含创建、提交和每次审批。

### 驳回流程

1. 新建并提交另一笔交易。
2. 任一级填写原因并驳回。
3. 申请人修改用途等字段后使用 `resubmit` 重提。
4. 确认重新进入 `PENDING_L1`、风险重新计算、历史审批/审计没有被覆盖。

### 并发流程

1. 两个浏览器标签同时打开同一待办，均记录旧 `version`。
2. 第一个标签审批成功。
3. 第二个标签审批，必须收到 409 并引导刷新。
4. 数据库只有一条对应级别的成功审批事实。

### 权限流程

1. D002 申请人不能读取 D001 申请。
2. D002 一级审批员不能处理 D001 的 L1 待办。
3. L1 不能执行 L2 操作；申请人不能调用 approve/reject。
4. AUDITOR 只能读取，不能创建、提交或审批。

## 6. 目录完整性检查

交付前至少确认存在：

```text
bank-approval-demo/
├─ backend/pom.xml
├─ backend/src/main/java/
├─ backend/src/main/resources/
├─ backend/src/test/
├─ frontend/package.json
├─ frontend/src/
├─ database/01-schema.sql
├─ database/02-data.sql
├─ docs/ARCHITECTURE.md
├─ docs/API_EXAMPLES.md
├─ docs/SECURITY.md
├─ docs/TESTING.md
├─ docker-compose.yml
└─ README.md
```

打包前删除或排除 `backend/target`、`frontend/node_modules`、`frontend/dist`、IDE 缓存、日志和本地密钥文件。最终 ZIP 应在全新目录解压验证一次，确保 README 中所有相对路径有效。

