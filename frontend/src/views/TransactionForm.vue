<template>
  <div>
    <el-alert v-if="editing" title="敏感字段只以脱敏形式返回。为避免浏览器保存明文，驳回重提时请重新输入手机号、身份证号和账户号。" type="info" show-icon :closable="false" class="danger-note" />
    <el-card class="panel-card" v-loading="loading">
      <div class="section-heading">
        <div><h2>{{ editing ? '修改并重新提交' : '填写交易申请' }}</h2><p>{{ editing ? `申请编号：${original.applicationNo || '-'}` : '申请先保存为草稿，可检查后再提交一级审批' }}</p></div>
        <el-tag v-if="editing" type="danger">原状态：已驳回 · v{{ original.version }}</el-tag>
      </div>
      <el-form ref="form" :model="form" :rules="rules" label-width="118px" status-icon class="transaction-form">
        <el-divider content-position="left">客户与付款信息（敏感字段）</el-divider>
        <el-row :gutter="24">
          <el-col :md="12" :sm="24"><el-form-item label="手机号" prop="mobile"><el-input v-model.trim="form.mobile" maxlength="11" autocomplete="off" placeholder="请输入 11 位手机号"><i slot="prefix" class="el-input__icon el-icon-mobile-phone" /></el-input></el-form-item></el-col>
          <el-col :md="12" :sm="24"><el-form-item label="身份证号" prop="idCard"><el-input v-model.trim="form.idCard" maxlength="18" autocomplete="off" placeholder="15 或 18 位身份证号"><i slot="prefix" class="el-input__icon el-icon-postcard" /></el-input></el-form-item></el-col>
          <el-col :md="12" :sm="24"><el-form-item label="付款账户" prop="account"><el-input v-model.trim="form.account" maxlength="32" autocomplete="off" placeholder="8–32 位数字"><i slot="prefix" class="el-input__icon el-icon-bank-card" /></el-input></el-form-item></el-col>
          <el-col :md="12" :sm="24"><el-form-item label="收款账户" prop="counterpartyAccount"><el-input v-model.trim="form.counterpartyAccount" maxlength="32" autocomplete="off" placeholder="8–32 位数字"><i slot="prefix" class="el-input__icon el-icon-wallet" /></el-input></el-form-item></el-col>
        </el-row>
        <el-alert title="以上字段由后端 AES-GCM 应用层加密后入库；前端不保存明文到本地存储。" type="success" :closable="false" show-icon class="field-tip" />

        <el-divider content-position="left">交易信息</el-divider>
        <el-row :gutter="24">
          <el-col :md="12" :sm="24"><el-form-item label="交易金额" prop="amount"><el-input v-model.trim="form.amount" placeholder="0.00"><el-select slot="prepend" v-model="form.currency" style="width:82px"><el-option label="CNY" value="CNY" /><el-option label="USD" value="USD" /><el-option label="EUR" value="EUR" /></el-select></el-input></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="交易用途" prop="purpose"><el-input v-model.trim="form.purpose" type="textarea" :rows="4" maxlength="500" show-word-limit placeholder="说明真实交易背景。高金额、敏感关键词等可能提高风险评分。" /></el-form-item></el-col>
        </el-row>

        <div class="form-actions">
          <el-button @click="$router.back()">取消</el-button>
          <template v-if="editing">
            <el-button type="primary" :loading="saving" @click="save('resubmit')">修改并重新提交</el-button>
          </template>
          <template v-else>
            <el-button :loading="saving" @click="save('draft')">保存草稿</el-button>
            <el-button type="primary" :loading="saving" @click="save('submit')">保存并提交审批</el-button>
          </template>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script>
import { createTransaction, getTransaction, resubmitTransaction, submitTransaction } from '../api/transactions'
import { getSession } from '../auth/session'

export default {
  data () {
    const positiveMoney = (rule, value, callback) => {
      if (!/^\d{1,16}(\.\d{1,2})?$/.test(String(value || '')) || Number(value) <= 0) callback(new Error('请输入大于 0、最多两位小数的金额'))
      else callback()
    }
    return {
      loading: false, saving: false, original: {},
      form: { mobile: '', idCard: '', account: '', counterpartyAccount: '', amount: '', currency: 'CNY', purpose: '' },
      rules: {
        mobile: [{ required: true, message: '请输入手机号', trigger: 'blur' }, { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
        idCard: [{ required: true, message: '请输入身份证号', trigger: 'blur' }, { pattern: /^(\d{15}|\d{17}[0-9Xx])$/, message: '身份证号格式不正确', trigger: 'blur' }],
        account: [{ required: true, message: '请输入付款账户', trigger: 'blur' }, { pattern: /^\d{8,32}$/, message: '账户应为 8–32 位数字', trigger: 'blur' }],
        counterpartyAccount: [{ required: true, message: '请输入收款账户', trigger: 'blur' }, { pattern: /^\d{8,32}$/, message: '账户应为 8–32 位数字', trigger: 'blur' }],
        amount: [{ required: true, validator: positiveMoney, trigger: 'blur' }],
        purpose: [{ required: true, message: '请输入交易用途', trigger: 'blur' }, { max: 500, message: '最多 500 个字符', trigger: 'blur' }]
      }
    }
  },
  computed: { editing () { return Boolean(this.$route.params.id) } },
  created () {
    if ((getSession() || {}).role !== 'APPLICANT') {
      this.$message.error('只有申请人可以填写交易申请')
      this.$router.replace('/transactions')
      return
    }
    if (this.editing) this.loadOriginal()
  },
  methods: {
    async loadOriginal () {
      this.loading = true
      try {
        const tx = await getTransaction(this.$route.params.id)
        if (tx.status !== 'REJECTED') {
          this.$message.warning('只有已驳回的申请可以修改重提')
          this.$router.replace(`/transactions/${tx.id}`)
          return
        }
        this.original = tx
        // 敏感字段故意不回填；详情接口只有脱敏值。
        this.form.amount = String(tx.amount || '')
        this.form.currency = tx.currency || 'CNY'
        this.form.purpose = tx.purpose || ''
      } catch (error) { /* 请求失败由拦截器提示。 */ }
      finally { this.loading = false }
    },
    save (mode) {
      this.$refs.form.validate(async valid => {
        if (!valid) return
        this.saving = true
        try {
          // 金额用十进制字符串传输，交给 Java BigDecimal，避免 JS Number 丢失精度。
          const payload = { ...this.form, amount: String(this.form.amount) }
          let result
          if (mode === 'resubmit') {
            result = await resubmitTransaction(this.original.id, { ...payload, version: this.original.version })
            this.$message.success('申请已修改并重新提交至一级审批')
          } else {
            result = await createTransaction(payload)
            if (mode === 'submit') {
              result = await submitTransaction(result.id, result.version)
              this.$message.success('申请已保存并提交至一级审批')
            } else this.$message.success('草稿已保存')
          }
          this.$router.replace(`/transactions/${result.id}`)
        } catch (error) {
          // 冲突后重新获取当前状态；用户查看后再决定是否重提，不自动重发命令。
          if (error.status === 409 && this.editing) await this.loadOriginal()
        } finally { this.saving = false }
      })
    }
  }
}
</script>

