<template>
  <div class="login-page">
    <section class="login-hero">
      <div class="hero-content">
        <div class="eyebrow">BANK WORKFLOW LAB</div>
        <h1>银行交易申请<br>四级审批教学系统</h1>
        <p>从交易申请、风险评分、逐级审批到审计追踪，完整演示前后端分离项目的典型工程实践。</p>
        <div class="feature-list">
          <span><i class="el-icon-check" /> 乐观锁并发保护</span>
          <span><i class="el-icon-check" /> 角色与数据权限</span>
          <span><i class="el-icon-check" /> 敏感数据应用层加密</span>
        </div>
      </div>
    </section>
    <section class="login-panel">
      <div class="login-card">
        <div class="login-title"><span>演示身份登录</span><small>选择一个预置角色体验相应业务流程</small></div>
        <el-alert
          title="安全警告：请求头模拟认证仅供本地教学，任何生产环境都不得使用。"
          type="error" :closable="false" show-icon class="login-alert" />
        <el-form label-position="top" @submit.native.prevent="login">
          <el-form-item label="演示用户">
            <el-select v-model="selectedId" filterable style="width:100%" size="large">
              <el-option v-for="item in users" :key="item.userId" :value="item.userId" :label="`${item.userName}（${roleLabel(item.role)}）`">
                <span>{{ item.userName }}</span><span class="option-meta">{{ roleLabel(item.role) }} / {{ item.departmentId }}</span>
              </el-option>
            </el-select>
          </el-form-item>
          <el-button type="primary" native-type="submit" size="large" class="login-button">进入教学系统</el-button>
        </el-form>
        <p class="login-note">系统不会验证密码；身份请求头可被篡改。生产方案应接入 OIDC/OAuth 2.1、MFA、服务端鉴权及密钥托管。</p>
      </div>
    </section>
  </div>
</template>

<script>
import { DEMO_USERS, saveSession } from '../auth/session'
import { ROLE_LABELS } from '../utils/presentation'

export default {
  data: () => ({ users: DEMO_USERS, selectedId: 'applicant01' }),
  methods: {
    roleLabel: role => ROLE_LABELS[role] || role,
    login () {
      const user = this.users.find(item => item.userId === this.selectedId)
      if (!user) return
      saveSession(user)
      this.$message.success(`已切换为：${user.userName}`)
      this.$router.replace(this.$route.query.redirect || '/dashboard')
    }
  }
}
</script>
