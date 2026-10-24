<template>
  <div v-loading="loading">
    <el-alert
      title="当前使用可伪造的请求头模拟身份，仅用于教学演示，严禁部署到生产环境。"
      type="warning" show-icon :closable="false" class="danger-note" />
    <div class="metric-grid">
      <div class="metric-card" style="--metric:#2563eb"><div class="metric-label">可见申请</div><div class="metric-value">{{ summary.total }}</div><div class="metric-foot">受角色与部门数据权限约束</div></div>
      <div class="metric-card" style="--metric:#f59e0b"><div class="metric-label">最近 100 笔待审批</div><div class="metric-value">{{ summary.pending }}</div><div class="metric-foot">按当前可见范围的最近 100 笔统计</div></div>
      <div class="metric-card" style="--metric:#10b981"><div class="metric-label">最近 100 笔已通过</div><div class="metric-value">{{ summary.approved }}</div><div class="metric-foot">完成全部四级审批</div></div>
      <div class="metric-card" style="--metric:#ef4444"><div class="metric-label">最近 100 笔高风险</div><div class="metric-value">{{ summary.highRisk }}</div><div class="metric-foot">风险评分需要重点关注</div></div>
    </div>

    <el-row :gutter="18">
      <el-col :lg="16" :md="24">
        <el-card class="panel-card">
          <div class="section-heading">
            <div><h2>{{ isApprover ? '我的审批队列' : '最近交易申请' }}</h2><p>只显示当前演示身份有权查看的数据</p></div>
            <el-button type="text" @click="$router.push('/transactions')">查看全部 <i class="el-icon-arrow-right" /></el-button>
          </div>
          <el-table :data="recent" style="width:100%" empty-text="暂无可见申请">
            <el-table-column prop="applicationNo" label="申请编号" min-width="170">
              <template slot-scope="scope"><span class="mono">{{ scope.row.applicationNo }}</span></template>
            </el-table-column>
            <el-table-column prop="applicantName" label="申请人" width="110" />
            <el-table-column label="金额" min-width="135">
              <template slot-scope="scope">{{ formatMoney(scope.row.amount, scope.row.currency) }}</template>
            </el-table-column>
            <el-table-column label="风险" width="90">
              <template slot-scope="scope"><span :class="['risk-score', riskClass(scope.row.riskLevel)]">{{ scope.row.riskScore }}</span></template>
            </el-table-column>
            <el-table-column label="状态" width="125">
              <template slot-scope="scope"><el-tag size="small" :type="statusType(scope.row.status)">{{ statusText(scope.row.status) }}</el-tag></template>
            </el-table-column>
            <el-table-column width="70"><template slot-scope="scope"><el-button type="text" @click="$router.push(`/transactions/${scope.row.id}`)">详情</el-button></template></el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :lg="8" :md="24">
        <el-card class="panel-card teaching-card">
          <div class="section-heading"><div><h2>本项目的教学切面</h2><p>从界面追踪后端设计</p></div></div>
          <div class="pattern-item"><b>状态模式</b><span>状态决定提交、审批、驳回、重提是否合法</span></div>
          <div class="pattern-item"><b>策略模式</b><span>多项风险规则组合形成评分与等级</span></div>
          <div class="pattern-item"><b>责任链</b><span>角色、部门、职责分离规则依次校验</span></div>
          <div class="pattern-item"><b>乐观锁</b><span>每次写操作携带 version，冲突返回 409</span></div>
          <el-button v-if="user.role === 'APPLICANT'" type="primary" style="width:100%;margin-top:16px" @click="$router.push('/transactions/new')">发起交易申请</el-button>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { listTransactions } from '../api/transactions'
import { getSession } from '../auth/session'
import { formatMoney, statusText, statusType } from '../utils/presentation'

export default {
  data: () => ({ loading: false, records: [], total: 0 }),
  computed: {
    user: () => getSession() || {},
    isApprover () { return /^APPROVER_L[1-4]$/.test(this.user.role || '') },
    recent () {
      const records = this.isApprover ? this.records.filter(row => row.status === `PENDING_L${this.user.role.slice(-1)}`) : this.records
      return records.slice(0, 8)
    },
    summary () {
      return {
        total: this.total,
        pending: this.records.filter(row => /^PENDING_L/.test(row.status)).length,
        approved: this.records.filter(row => row.status === 'APPROVED').length,
        highRisk: this.records.filter(row => row.riskLevel === 'HIGH' || Number(row.riskScore) >= 70).length
      }
    }
  },
  created () { this.load() },
  methods: {
    formatMoney, statusText, statusType,
    riskClass (level) { return `risk-${String(level || 'low').toLowerCase()}` },
    async load () {
      this.loading = true
      try {
        const result = await listTransactions({ page: 1, size: 100 })
        this.records = (result && (result.records || result.content || result.list)) || []
        this.total = Number((result && result.total) || this.records.length)
      } catch (error) { /* Axios 拦截器已提示；保留页面，允许用户重试。 */ }
      finally { this.loading = false }
    }
  }
}
</script>

