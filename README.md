# 银行交易申请与四级审批教学项目

一套前后端分离、可运行的教学工程：Vue **2.7.16** + Element UI **2.15.14** + Axios；Spring Boot **2.7.18** + Java **8** + MyBatis XML + MySQL **8.4**。使用一条完整业务链讲解注解、设计模式、事务、权限、字段加密与并发控制。

> **仅限本地教学。演示登录信任浏览器提交的身份请求头，没有真正的身份校验，任何人都可以伪造角色，绝对不能用于生产。默认数据库口令与 AES 密钥均公开。不要输入真实客户数据。**

## 1. 已实现的功能

- 创建草稿、按申请编号/用途/状态/日期分页查询、查看脱敏详情。
- 提交后依次经过 L1 → L2 → L3 → L4；四级全部通过才进入 APPROVED。
- 任一级驳回，申请人重新录入完整敏感字段、修改业务内容后原子重提至 L1；保留历史审批与审计。
- 风险策略汇总评分；旧风控 SDK 通过适配器纳入同一个策略接口。
- 角色权限、申请人所有权、本部门数据权限、禁止本人审批。
- 每次修改必须携带 `version`；数据库同时检查旧 `status`、`version`，冲突返回 HTTP 409。
- 手机号、身份证、付款/收款账户使用 AES-GCM 加密入库，接口仅返回脱敏值。
- 业务状态、审批事实、审计日志在同一事务中提交；审计失败则回滚。

这里的“交易”是**交易申请与审批**，不含真实转账、账户记账、资金冻结、对账或清算。

## 2. 环境与启动

准备：JDK **8u161+**（推荐较新的 Java 8 更新版）、Node.js **18+** 与 npm、Docker Desktop/Engine。也可以直接安装 MySQL 8.4。请把 `JAVA_HOME` 指向 JDK，而非 JRE；`java -version` 与 Maven 使用的 Java 应一致。

### 2.1 启动 MySQL

在项目根目录执行：

```bash
docker compose up -d mysql
docker compose ps
```

等待数据库显示 `healthy`。首次创建数据卷时自动执行 `database/01-schema.sql` 与 `02-data.sql`。数据库：`bank_approval`，本地端口：`3306`，应用用户：`bank_app`，教学口令：`bank_app_ChangeMe`。端口已占用时调整 compose 的宿主端口，并同步设置后端 `DB_URL`。

如果已有 MySQL，在 MySQL 客户端中执行（项目根目录为当前目录）：

```sql
SOURCE database/01-schema.sql;
SOURCE database/02-data.sql;
CREATE USER IF NOT EXISTS 'bank_app'@'localhost' IDENTIFIED BY 'bank_app_ChangeMe';
GRANT SELECT, INSERT, UPDATE ON bank_approval.* TO 'bank_app'@'localhost';
```

数据库脚本可重复执行，不会删除交易；`sys_user` 只展示演示身份目录，当前 Header 认证**不读取**它。初始化不插入交易密文，首笔数据由应用使用当前密钥加密创建。

### 2.2 启动后端

Windows PowerShell：

```powershell
cd backend
.\mvnw.cmd clean verify
.\mvnw.cmd spring-boot:run
```

macOS / Linux / Git Bash：

```bash
cd backend
sh mvnw clean verify
sh mvnw spring-boot:run
```

包装脚本首次联网下载 Maven 3.9.9 与依赖，无需全局安装 Maven。默认后端地址：`http://localhost:8088`。也可以构建后运行 `java -jar target/bank-approval-backend-1.0.0.jar`。

### 2.3 启动前端（另开一个终端）

```bash
cd frontend
npm ci --ignore-scripts
npm run serve
```

访问 `http://localhost:8081`，选择申请人进入。开发代理把 `/api` 转发到 `http://localhost:8088`。修改地址请编辑 `.env.development` 的 `VUE_APP_API_TARGET`，然后重启前端。前端生产构建：`npm run build`，产物在 `frontend/dist`；静态部署需由网关将 `/api` 转发给后端。

### 2.4 五分钟体验

1. 用 `applicant01` 保存并提交交易。
2. 依次切换 `approver01`、`approver02`、`approver03`、`approver04`，各审批一次。
3. 用 `auditor01` 查看审批记录、审计时间线及脱敏详情。
4. 另建申请，在一级驳回，再切回申请人“修改并重提”。
5. 同时打开两个同级审批详情页，先后点击审批，第二个页面收到 409 并刷新。

PowerShell 可自动创建并验证完整流程（会生成两笔教学交易）：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\api-smoke.ps1
```

## 3. 角色与数据范围

| 用户 | 角色 | 部门 | 能力 |
|---|---|---|---|
| applicant01 | APPLICANT | D001 | 创建、提交、查看本人申请、本人驳回重提 |
| applicant02 | APPLICANT | D002 | 用于演示申请人数据隔离 |
| approver01…04 | APPROVER_L1…L4 | D001 | 只处理本部门、对应级别、非本人申请 |
| approverD002 | APPROVER_L1 | D002 | 用于演示跨部门审批被拒绝 |
| auditor01 | AUDITOR | AUDIT | 跨部门只读，无创建和审批能力 |
| admin01 | ADMIN | HQ | 跨部门读取，可创建/提交/重提；不能执行审批 |

审批员可查询本部门各状态的记录；只有当前级别才有处理权限。`ALL` 不是部门通配符。管理员不能代替审批员完成终审。前端隐藏按钮只是交互提示，后端始终重新鉴权。

## 4. 教学导航

后端包根目录：`backend/src/main/java/com/example/bankapproval`。

| 学习点 | 入口 |
|---|---|
| `@RestController`、`@Validated`、`@Valid`、`@ModelAttribute` | `api/TransactionController.java`；查询 DTO 绑定多个 URL 参数，请求体 DTO 绑定 JSON |
| `@RestControllerAdvice` | `common/GlobalExceptionHandler.java`；区分请求体、查询绑定、路径校验、权限与并发错误 |
| `@ConfigurationProperties` | `config/BankCryptoProperties.java`、`BankSecurityProperties.java` |
| `@Profile` | `config/ProfileTeachingConfig.java`、`ProductionSafetyGuard.java` |
| `@ConditionalOnProperty` | `infrastructure/crypto/AesGcmCryptoService.java` 与 `PlaintextCryptoService.java` |
| `@Transactional`、`@PreAuthorize` | `application/TransactionService.java`；从 Spring 代理调用公开方法才生效 |
| 状态模式 | `domain/state/TransactionStateMachine.java` |
| 策略模式 | `domain/risk/RiskEngine.java`、`RiskStrategy.java` |
| 责任链 | `domain/approval/ApprovalRuleChain.java`；状态、级别、部门、职责分离依次校验 |
| 适配器 | `infrastructure/risk/LegacyRiskAdapter.java`；包装接口不兼容的旧 SDK |
| Repository + MyBatis XML | `domain/repository/`、`infrastructure/persistence/`、`resources/mapper/` |
| MapStruct | `application/TransactionDtoMapper.java`；构建时生成实现，不手工编写/提交生成类 |
| Axios 拦截器 | `frontend/src/api/http.js`；模拟身份、统一错误、409 提示 |
| 金额精度 | Java `BigDecimal` + MySQL `DECIMAL(18,2)`；JSON 金额以十进制字符串传输 |

建议阅读顺序：Controller → Service → 状态机/风险策略/审批链 → Repository → XML → 测试。关键类附中文教学注释。

## 5. 配置与安全边界

| 环境变量 | 默认/用途 |
|---|---|
| SERVER_PORT | 8088 |
| DB_URL | 本机 MySQL `bank_approval`，详见 application.yml |
| DB_USERNAME / DB_PASSWORD | bank_app / bank_app_ChangeMe，仅教学 |
| BANK_CRYPTO_KEY | Base64 AES 密钥；默认 32 字节公开教学密钥 |
| SPRING_PROFILES_ACTIVE | dev；测试使用 test，生产安全闸使用 prod |
| DEMO_AUTH_ENABLED | dev 默认 true，显式身份头仍必填 |

开发默认密钥仅为开箱运行；如果改变密钥，旧密文将无法解密。本例未实现密钥版本/轮换，不能直接替换已有数据的密钥。`prod` 禁止演示认证和关闭加密，并要求外部密钥；它尚未接入真正身份提供方，关闭演示认证后所有业务请求都不能通过认证，**切换 prod 不等于生产可用**。

AES-GCM 保护数据库/备份中的指定字段，不能防御能读取应用进程与密钥的主机管理员。浏览器到后端的本机 HTTP 仅为教学便利，真实部署必须使用 HTTPS/TLS。用途和审批意见是自由文本，当前不会自动识别其中的个人信息，请勿填写敏感明文。审计仅记录成功业务操作，不包含登录、失败鉴权及每次读取审计。

本项目依照指定旧技术栈教学。[Vue 2 已结束官方支持](https://v2.vuejs.org/lts/)；[Spring Boot 2.7.18 文档](https://docs.spring.io/spring-boot/docs/2.7.18/reference/htmlsingle/)说明其 Java 8 兼容范围。生产选型应另行评估受支持版本与依赖修复，不应把本教学工程直接上线。

## 6. 目录与进一步说明

```text
bank-approval-demo/
├── backend/              Java 源码、MyBatis XML、测试、Maven Wrapper
├── frontend/             Vue 2 源码、package-lock.json、代理配置
├── database/             MySQL 唯一规范建表及身份初始化 SQL
├── scripts/api-smoke.ps1 自动调用真实 HTTP 接口的验收脚本
├── docs/ARCHITECTURE.md  架构、状态、注解/模式、加密威胁边界
├── docs/API_EXAMPLES.md  curl 接口示例与错误码
├── docs/SECURITY.md      演示认证及生产替换边界
├── docs/TESTING.md       测试方法和结果边界
├── docs/VERIFICATION.md  本次交付的实际验收结果
└── docker-compose.yml    MySQL 8.4 本地容器
```

常见问题：

- 后端连接失败：确认 MySQL healthy、3306 未被占用、数据库/账号已初始化；已有 Docker 数据卷不会重新执行初始化脚本。
- 前端 502：确认后端 8088 已启动；修改开发代理后需重启前端。
- 401：必须显式选择演示身份，curl 必须发送 X-User-Id、X-Role、X-Dept-Id。
- 403：检查本人/部门/角色；ADMIN 不具备审批角色。
- 409：刷新取得最新 version，不要自动覆盖或循环重试写请求。
- 解密失败：检查使用的 BANK_CRYPTO_KEY 是否与写入数据时一致；不能通过关闭加密“修复”既有密文。
- 测试不需要 MySQL：自动化使用 H2 MySQL 兼容模式，但它不能替代实际 MySQL 8.4 的验收。

本项目采用 MIT 许可证，第三方组件适用各自许可证。
