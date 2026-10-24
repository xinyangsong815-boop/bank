export const STATUS_META = {
  DRAFT: { text: '草稿', type: 'info', step: 0 },
  PENDING_L1: { text: '待一级审批', type: 'warning', step: 1 },
  PENDING_L2: { text: '待二级审批', type: 'warning', step: 2 },
  PENDING_L3: { text: '待三级审批', type: 'warning', step: 3 },
  PENDING_L4: { text: '待四级审批', type: 'warning', step: 4 },
  APPROVED: { text: '审批通过', type: 'success', step: 5 },
  REJECTED: { text: '已驳回', type: 'danger', step: 0 }
}

export const ROLE_LABELS = {
  APPLICANT: '申请人', APPROVER_L1: '一级审批人', APPROVER_L2: '二级审批人',
  APPROVER_L3: '三级审批人', APPROVER_L4: '四级审批人', AUDITOR: '审计员', ADMIN: '管理员'
}

export function statusText (status) {
  return (STATUS_META[status] && STATUS_META[status].text) || status || '-'
}

export function statusType (status) {
  return (STATUS_META[status] && STATUS_META[status].type) || 'info'
}

export function formatMoney (amount, currency) {
  if (amount === null || amount === undefined || amount === '') return '-'
  const [integer, fraction = ''] = String(amount).split('.')
  return `${currency || 'CNY'} ${integer.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}.${fraction.padEnd(2, '0').slice(0, 2)}`
}

export function formatTime (value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').replace(/\.\d+$/, '')
}
