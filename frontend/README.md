# 前端说明

技术栈：Vue 2.7、Element UI 2、Vue Router 3、Axios。

> **安全警告**：本前端把所选演示用户写入 `localStorage`，并通过 `X-User-Id`、`X-Role`、`X-Dept-Id` 请求头发送给后端。请求头可被任意伪造，这种方式只用于本地教学，严禁用于生产环境。

## 启动

```bash
npm ci --ignore-scripts
npm run serve
```

默认地址为 <http://localhost:8081>，`/api` 会代理到 <http://localhost:8088>。需要修改后端地址时设置 `VUE_APP_API_TARGET`。

生产构建：

```bash
npm run build
```

## 页面与教学点

- `/login`：切换申请人、四级审批人、审计员和管理员等演示身份。
- `/dashboard`：显示当前角色可见范围内的概况及最近申请。
- `/transactions`：条件查询、分页、数据权限结果展示。
- `/transactions/new`：创建草稿或创建后立即提交；表单校验与后端 Bean Validation 对应。
- `/transactions/:id`：四级进度、风险分数、审批/驳回、审批记录、审计时间线、`version` 并发控制说明。
- `/transactions/:id/edit`：驳回后重新录入敏感字段并重提。详情接口只返回脱敏值，因此不会把密文或明文回填到浏览器。

生产系统还应使用 OIDC/OAuth 2.1、MFA、短期令牌、安全 Cookie 或标准 Bearer Token、同域 TLS 网关、CSP/CSRF 防护和服务端细粒度鉴权。
