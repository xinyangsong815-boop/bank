import Vue from 'vue'

const STORAGE_KEY = 'bank-approval-demo-user'

export const DEMO_USERS = [
  { userId: 'applicant01', userName: '申请人张三', role: 'APPLICANT', departmentId: 'D001' },
  { userId: 'applicant02', userName: 'D002 申请人', role: 'APPLICANT', departmentId: 'D002' },
  { userId: 'approverD002', userName: 'D002 一级审批员', role: 'APPROVER_L1', departmentId: 'D002' },
  { userId: 'approver01', userName: '一级审批李经理', role: 'APPROVER_L1', departmentId: 'D001' },
  { userId: 'approver02', userName: '二级审批王总监', role: 'APPROVER_L2', departmentId: 'D001' },
  { userId: 'approver03', userName: '三级审批赵行长', role: 'APPROVER_L3', departmentId: 'D001' },
  { userId: 'approver04', userName: '四级审批周委员', role: 'APPROVER_L4', departmentId: 'D001' },
  { userId: 'auditor01', userName: '审计员陈审计', role: 'AUDITOR', departmentId: 'AUDIT' },
  { userId: 'admin01', userName: '系统管理员', role: 'ADMIN', departmentId: 'HQ' }
]

function readStoredSession () {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY))
  } catch (e) {
    return null
  }
}

// localStorage 本身不具备响应性；observable 保证切换身份后导航栏立即更新。
const state = Vue.observable({ user: readStoredSession() })

export function getSession () { return state.user }

export function saveSession (user) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(user))
  state.user = user
}

export function clearSession () {
  localStorage.removeItem(STORAGE_KEY)
  state.user = null
}

export function isLoggedIn () {
  const user = getSession()
  return Boolean(user && user.userId && user.role && user.departmentId)
}
