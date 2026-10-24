import axios from 'axios'
import { Message, MessageBox } from 'element-ui'
import { getSession, clearSession } from '../auth/session'

const http = axios.create({
  baseURL: process.env.VUE_APP_API_BASE_URL || '/api',
  timeout: 15000
})

// 教学重点：请求拦截器只是在演示环境模拟认证。生产系统必须使用统一身份平台、
// 短期令牌或服务端会话，绝不能信任浏览器可随意伪造的身份请求头。
http.interceptors.request.use(config => {
  const user = getSession()
  if (user) {
    config.headers['X-User-Id'] = user.userId
    // 浏览器请求头只安全承载 Latin-1；这里使用 ASCII 用户号，中文显示名只留在界面。
    config.headers['X-User-Name'] = user.userId
    config.headers['X-Role'] = user.role
    config.headers['X-Dept-Id'] = user.departmentId
  }
  return config
})

http.interceptors.response.use(response => {
  const body = response.data
  // 同时兼容统一响应与直接返回业务数据，便于讲解 Axios 适配器思想。
  if (body && Object.prototype.hasOwnProperty.call(body, 'success')) {
    if (body.success === false) return Promise.reject(new Error(body.message || '请求失败'))
    return body.data
  }
  if (body && Object.prototype.hasOwnProperty.call(body, 'code') && Object.prototype.hasOwnProperty.call(body, 'data')) {
    if (String(body.code) !== '0' && String(body.code) !== '200') return Promise.reject(new Error(body.message || '请求失败'))
    return body.data
  }
  return body
}, error => {
  const status = error.response && error.response.status
  const data = error.response && error.response.data
  const message = (data && (data.message || data.error)) || error.message || '网络请求失败'

  if (status === 409) {
    MessageBox.alert('该申请已被其他用户修改，请刷新页面后重试。', '并发冲突', { type: 'warning' })
  } else if (status === 401) {
    clearSession()
    Message.error('演示身份已失效，请重新选择身份')
    if (window.location.hash !== '#/login') window.location.hash = '#/login'
  } else if (status === 403) {
    Message.error(message || '当前角色没有此操作权限')
  } else {
    Message.error(message)
  }
  const normalized = new Error(message)
  normalized.status = status
  normalized.payload = data
  return Promise.reject(normalized)
})

export default http
