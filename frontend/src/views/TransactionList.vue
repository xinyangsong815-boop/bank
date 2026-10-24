<template>
  <div>
    <el-card class="panel-card">
      <div class="section-heading"><div><h2>交易申请查询</h2><p>后端会根据申请人、审批角色、部门及审计权限自动裁剪数据</p></div><el-button v-if="user.role === 'APPLICANT'" type="primary" icon="el-icon-plus" @click="$router.push('/transactions/new')">新建申请</el-button></div>
      <el-form :inline="true" :model="query" size="small" @submit.native.prevent="search">
        <el-form-item label="关键字"><el-input v-model.trim="query.keyword" clearable placeholder="申请编号 / 用途" style="width:210px" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部状态" style="width:145px">
            <el-option v-for="item in statuses" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="申请日期">
          <el-date-picker v-model="dateRange" type="daterange" value-format="yyyy-MM-dd" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" style="width:250px" />
        </el-form-item>
        <el-form-item><el-button type="primary" icon="el-icon-search" native-type="submit">查询</el-button><el-button @click="reset">重置</el-button></el-form-item>
      </el-form>
    </el-card>

    <el-card class="panel-card">
      <div class="toolbar list-summary"><span>共 <b>{{ total }}</b> 条结果</span><el-button size="small" icon="el-icon-refresh" @click="load">刷新</el-button></div>
      <el-table v-loading="loading" :data="rows" stripe style="width:100%" @row-dblclick="openDetail">
        <el-table-column prop="applicationNo" label="申请编号" min-width="175"><template slot-scope="scope"><span class="mono link" @click="openDetail(scope.row)">{{ scope.row.applicationNo }}</span></template></el-table-column>
        <el-table-column prop="applicantName" label="申请人" width="110" />
        <el-table-column prop="departmentId" label="部门" width="90" />
        <el-table-column label="交易金额" min-width="140"><template slot-scope="scope">{{ formatMoney(scope.row.amount, scope.row.currency) }}</template></el-table-column>
        <el-table-column prop="purpose" label="用途" min-width="150" show-overflow-tooltip />
        <el-table-column label="风险" width="105"><template slot-scope="scope"><span :class="['risk-score', riskClass(scope.row)]">{{ scope.row.riskScore }} / {{ riskText(scope.row.riskLevel) }}</span></template></el-table-column>
        <el-table-column label="状态" width="125"><template slot-scope="scope"><el-tag size="small" :type="statusType(scope.row.status)">{{ statusText(scope.row.status) }}</el-tag></template></el-table-column>
        <el-table-column label="版本" width="68"><template slot-scope="scope"><span class="mono">v{{ scope.row.version }}</span></template></el-table-column>
        <el-table-column label="更新时间" min-width="160"><template slot-scope="scope">{{ formatTime(scope.row.updatedAt) }}</template></el-table-column>
        <el-table-column fixed="right" label="操作" width="82"><template slot-scope="scope"><el-button type="text" @click="openDetail(scope.row)">查看</el-button></template></el-table-column>
      </el-table>
      <el-pagination
        class="pagination" background layout="total, sizes, prev, pager, next"
        :current-page.sync="query.page" :page-size.sync="query.size" :page-sizes="[10,20,50,100]" :total="total"
        @current-change="load" @size-change="sizeChanged" />
    </el-card>
  </div>
</template>

<script>
import { listTransactions } from '../api/transactions'
import { getSession } from '../auth/session'
import { STATUS_META, formatMoney, formatTime, statusText, statusType } from '../utils/presentation'

export default {
  data () {
    return {
      loading: false, rows: [], total: 0, dateRange: [],
      query: { keyword: '', status: '', page: 1, size: 10 },
      statuses: Object.keys(STATUS_META).map(value => ({ value, label: STATUS_META[value].text }))
    }
  },
  computed: { user: () => getSession() || {} },
  created () {
    if (this.$route.query.status) this.query.status = this.$route.query.status
    this.load()
  },
  methods: {
    formatMoney, formatTime, statusText, statusType,
    riskClass (row) { return `risk-${String(row.riskLevel || (row.riskScore >= 70 ? 'high' : row.riskScore >= 40 ? 'medium' : 'low')).toLowerCase()}` },
    riskText (level) { return ({ LOW: '低', MEDIUM: '中', HIGH: '高' })[level] || level || '-' },
    async load () {
      this.loading = true
      try {
        const params = { ...this.query }
        if (this.dateRange && this.dateRange.length === 2) [params.startDate, params.endDate] = this.dateRange
        Object.keys(params).forEach(k => (params[k] === '' || params[k] === null) && delete params[k])
        const result = await listTransactions(params)
        this.rows = (result && (result.records || result.content || result.list)) || []
        this.total = Number((result && result.total) || this.rows.length)
      } catch (error) { /* 错误提示由统一拦截器负责，列表允许再次刷新。 */ }
      finally { this.loading = false }
    },
    search () { this.query.page = 1; this.load() },
    reset () { this.query = { keyword: '', status: '', page: 1, size: 10 }; this.dateRange = []; this.load() },
    sizeChanged () { this.query.page = 1; this.load() },
    openDetail (row) { this.$router.push(`/transactions/${row.id}`) }
  }
}
</script>

