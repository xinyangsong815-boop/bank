<template>
  <div v-loading="loading">
    <el-card class="panel-card">
      <div class="section-heading">
        <div><h2>{{ tx.applicationNo || '交易申请详情' }}</h2><p>申请人 {{ tx.applicantName || '-' }} · 部门 {{ tx.departmentId || '-' }} · 当前版本 v{{ tx.version == null ? '-' : tx.version }}</p></div>
        <el-tag :type="statusType(tx.status)" effect="dark">{{ statusText(tx.status) }}</el-tag>
      </div>

      <el-alert v-if="tx.status === 'REJECTED'" :title="`驳回原因：${tx.rejectionReason || '未填写'}`" type="error" show-icon :closable="false" class="danger-note" />
      <div class="progress-wrap">
        <el-steps :active="activeStep" align-center finish-status="success" :process-status="tx.status === 'REJECTED' ? 'error' : 'process'">
          <el-step title="提交申请" description="风险评分" />
          <el-step title="一级审批" description="经办复核" />
          <el-step title="二级审批" description="部门复核" />
          <el-step title="三级审批" description="管理复核" />
          <el-step title="四级审批" description="最终决策" />
          <el-step title="完成" />
        </el-steps>
      </div>
      <div class="action-bar">
        <el-button icon="el-icon-back" @click="$router.push('/transactions')">返回列表</el-button>
        <el-button v-if="canEdit" type="warning" icon="el-icon-edit" @click="$router.push(`/transactions/${tx.id}/edit`)">修改并重提</el-button>
        <el-button v-if="canSubmit" type="primary" :loading="acting" @click="submitDraft">提交一级审批</el-button>
        <template v-if="canApprove">
          <el-button type="danger" plain @click="openAction('reject')">驳回</el-button>
          <el-button type="success" @click="openAction('approve')">审批通过</el-button>
        </template>
      </div>
    </el-card>

    <el-row :gutter="18" style="margin-top:18px">
      <el-col :lg="17" :md="24">
        <el-card class="panel-card info-card">
          <div class="section-heading"><div><h2>申请信息</h2><p>敏感字段仅以脱敏形式展示</p></div></div>
          <div class="detail-grid">
            <div class="detail-item"><label>交易金额</label><span class="amount">{{ formatMoney(tx.amount, tx.currency) }}</span></div>
            <div class="detail-item"><label>风险评分</label><span :class="['risk-score', riskClass]">{{ tx.riskScore == null ? '-' : tx.riskScore }} / {{ riskText }}</span></div>
            <div class="detail-item"><label>当前审批级别</label><span>{{ tx.currentLevel ? `${tx.currentLevel} 级` : '-' }}</span></div>
            <div class="detail-item"><label>手机号</label><span class="mono">{{ tx.mobileMasked || '-' }}</span></div>
            <div class="detail-item"><label>身份证号</label><span class="mono">{{ tx.idCardMasked || '-' }}</span></div>
            <div class="detail-item"><label>付款账户</label><span class="mono">{{ tx.accountMasked || '-' }}</span></div>
            <div class="detail-item"><label>收款账户</label><span class="mono">{{ tx.counterpartyAccountMasked || '-' }}</span></div>
            <div class="detail-item"><label>申请时间</label><span>{{ formatTime(tx.createdAt) }}</span></div>
            <div class="detail-item"><label>最后更新</label><span>{{ formatTime(tx.updatedAt) }}</span></div>
            <div class="detail-item purpose"><label>交易用途</label><span>{{ tx.purpose || '-' }}</span></div>
          </div>
        </el-card>
      </el-col>
      <el-col :lg="7" :md="24">
        <el-card class="panel-card lock-card">
          <div class="section-heading"><div><h2>并发保护</h2><p>Conditional Update / Version</p></div></div>
          <div class="version-orbit"><span>v{{ tx.version == null ? '-' : tx.version }}</span></div>
          <p>审批与提交请求都携带当前版本号。后端执行 <code>WHERE id=? AND version=? AND status=?</code> 条件更新；受影响行数为 0 时返回 409。</p>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="panel-card">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="审批记录" name="approvals">
          <el-table :data="approvals" empty-text="暂无审批记录">
            <el-table-column prop="level" label="级别" width="72"><template slot-scope="scope">L{{ scope.row.level }}</template></el-table-column>
            <el-table-column prop="approverName" label="审批人" width="120" />
            <el-table-column prop="action" label="动作" width="90"><template slot-scope="scope"><el-tag size="mini" :type="scope.row.action === 'APPROVE' ? 'success' : 'danger'">{{ scope.row.action === 'APPROVE' ? '通过' : '驳回' }}</el-tag></template></el-table-column>
            <el-table-column label="状态变化" min-width="220"><template slot-scope="scope"><span class="mono">{{ scope.row.statusBefore }} → {{ scope.row.statusAfter }}</span></template></el-table-column>
            <el-table-column prop="comment" label="意见" min-width="180" />
            <el-table-column label="时间" width="170"><template slot-scope="scope">{{ formatTime(scope.row.createdAt) }}</template></el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="审计时间线" name="audits">
          <el-timeline v-if="audits.length">
            <el-timeline-item v-for="item in audits" :key="item.id" :timestamp="formatTime(item.createdAt)" placement="top" color="#0ea5b7">
              <div class="timeline-title"><b>{{ operationText(item.operation) }}</b><el-tag size="mini" type="info">{{ item.operatorName || item.operatorId }}</el-tag><span class="muted">IP {{ item.ipAddress || '-' }}</span></div>
              <div class="timeline-detail">{{ item.detail || '-' }}</div>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无审计记录" :image-size="70" />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog :title="actionMode === 'approve' ? '确认审批通过' : '确认驳回申请'" :visible.sync="actionVisible" width="460px">
      <el-alert :title="`本次操作基于版本 v${tx.version}；如版本已变化，后端将拒绝更新。`" type="info" :closable="false" show-icon />
      <el-form style="margin-top:18px" label-position="top">
        <el-form-item :label="actionMode === 'approve' ? '审批意见（选填）' : '驳回原因（必填）'">
          <el-input v-model.trim="actionComment" type="textarea" :rows="4" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <span slot="footer"><el-button @click="actionVisible=false">取消</el-button><el-button :type="actionMode === 'approve' ? 'success' : 'danger'" :loading="acting" @click="confirmAction">确认{{ actionMode === 'approve' ? '通过' : '驳回' }}</el-button></span>
    </el-dialog>
  </div>
</template>

<script>
import { getTransaction, submitTransaction, approveTransaction, rejectTransaction, listApprovals, listAudits } from '../api/transactions'
import { getSession } from '../auth/session'
import { STATUS_META, formatMoney, formatTime, statusText, statusType } from '../utils/presentation'

export default {
  data: () => ({ loading: false, acting: false, tx: {}, approvals: [], audits: [], activeTab: 'approvals', actionVisible: false, actionMode: 'approve', actionComment: '' }),
  computed: {
    user: () => getSession() || {},
    activeStep () {
      if (this.tx.status === 'APPROVED') return 6
      if (this.tx.status === 'REJECTED') {
        const rejection = this.approvals.filter(item => item.action === 'REJECT').slice(-1)[0]
        return rejection ? Number(rejection.level) : 1
      }
      return (STATUS_META[this.tx.status] && STATUS_META[this.tx.status].step) || 0
    },
    canSubmit () { return this.user.role === 'APPLICANT' && this.tx.status === 'DRAFT' },
    canEdit () { return this.user.role === 'APPLICANT' && this.tx.status === 'REJECTED' },
    canApprove () {
      const match = /^PENDING_L([1-4])$/.exec(this.tx.status || '')
      return Boolean(match && this.user.role === `APPROVER_L${match[1]}` &&
        this.user.departmentId === this.tx.departmentId && this.user.userId !== this.tx.applicantId)
    },
    riskClass () { return `risk-${String(this.tx.riskLevel || (this.tx.riskScore >= 70 ? 'HIGH' : this.tx.riskScore >= 40 ? 'MEDIUM' : 'LOW')).toLowerCase()}` },
    riskText () { return ({ LOW: '低风险', MEDIUM: '中风险', HIGH: '高风险' })[this.tx.riskLevel] || this.tx.riskLevel || '-' }
  },
  created () { this.load() },
  methods: {
    formatMoney, formatTime, statusText, statusType,
    operationText (value) { return ({ CREATE: '创建草稿', SUBMIT: '提交审批', APPROVE: '审批通过', REJECT: '驳回申请', RESUBMIT: '修改重提' })[value] || value },
    async load () {
      this.loading = true
      try {
        const id = this.$route.params.id
        const [tx, approvals, audits] = await Promise.all([getTransaction(id), listApprovals(id), listAudits(id)])
        this.tx = tx || {}
        this.approvals = approvals || []
        this.audits = audits || []
      } catch (error) { /* 网络或权限错误已提示，不产生未处理的 Promise。 */ }
      finally { this.loading = false }
    },
    async submitDraft () {
      try { await this.$confirm('确认将草稿提交至一级审批？提交后不能直接编辑。', '提交确认', { type: 'warning' }) } catch (e) { return }
      this.acting = true
      try { this.tx = await submitTransaction(this.tx.id, this.tx.version); this.$message.success('已提交一级审批'); await this.load() }
      catch (e) { if (e.status === 409) await this.load() }
      finally { this.acting = false }
    },
    openAction (mode) { this.actionMode = mode; this.actionComment = ''; this.actionVisible = true },
    async confirmAction () {
      if (this.actionMode === 'reject' && !this.actionComment) return this.$message.warning('请填写驳回原因')
      this.acting = true
      try {
        if (this.actionMode === 'approve') await approveTransaction(this.tx.id, this.tx.version, this.actionComment)
        else await rejectTransaction(this.tx.id, this.tx.version, this.actionComment)
        this.$message.success(this.actionMode === 'approve' ? '审批已通过' : '申请已驳回')
        this.actionVisible = false
        await this.load()
      } catch (e) {
        if (e.status === 409) { this.actionVisible = false; await this.load() }
      } finally { this.acting = false }
    }
  }
}
</script>

