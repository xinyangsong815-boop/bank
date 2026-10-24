<template>
  <router-view v-if="$route.meta.public" />
  <div v-else class="app-shell">
    <el-container>
      <el-aside width="232px" class="sidebar">
        <div class="brand">
          <div class="brand-mark">银</div>
          <div><strong>交易审批</strong><small>教学演示系统</small></div>
        </div>
        <el-menu :default-active="$route.path" router background-color="#10243e" text-color="#c9d6e5" active-text-color="#7dd3fc">
          <el-menu-item index="/dashboard"><i class="el-icon-data-analysis" />工作台</el-menu-item>
          <el-menu-item index="/transactions"><i class="el-icon-search" />交易申请查询</el-menu-item>
          <el-menu-item v-if="user.role === 'APPLICANT'" index="/transactions/new"><i class="el-icon-edit-outline" />发起交易申请</el-menu-item>
        </el-menu>
        <div class="demo-badge"><i class="el-icon-warning-outline" /> 演示认证<br><b>严禁用于生产环境</b></div>
      </el-aside>
      <el-container>
        <el-header class="topbar">
          <div>
            <div class="page-title">{{ $route.meta.title || '银行交易审批' }}</div>
            <div class="page-subtitle">四级逐级审批 · 风险评分 · 全程留痕</div>
          </div>
          <el-dropdown trigger="click" @command="handleUserCommand">
            <div class="user-chip">
              <el-avatar size="small" icon="el-icon-user-solid" />
              <span><b>{{ user.userName }}</b><small>{{ roleLabel }} · {{ user.departmentId }}</small></span>
              <i class="el-icon-arrow-down" />
            </div>
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item command="switch">切换演示身份</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出</el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </el-header>
        <el-main class="main-content"><router-view /></el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script>
import { getSession, clearSession } from './auth/session'
import { ROLE_LABELS } from './utils/presentation'

export default {
  computed: {
    user () { return getSession() || {} },
    roleLabel () { return ROLE_LABELS[this.user.role] || this.user.role }
  },
  methods: {
    handleUserCommand (command) {
      if (command === 'switch' || command === 'logout') {
        clearSession()
        this.$router.push('/login')
      }
    }
  }
}
</script>
