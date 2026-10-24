import http from './http'

export const getCurrentUser = () => http.get('/me')
export const createTransaction = data => http.post('/transactions', data)
export const listTransactions = params => http.get('/transactions', { params })
export const getTransaction = id => http.get(`/transactions/${id}`)
export const submitTransaction = (id, version) => http.post(`/transactions/${id}/submit`, { version })
export const resubmitTransaction = (id, data) => http.post(`/transactions/${id}/resubmit`, data)
export const approveTransaction = (id, version, comment) => http.post(`/transactions/${id}/approve`, { version, comment })
export const rejectTransaction = (id, version, reason) => http.post(`/transactions/${id}/reject`, { version, reason })
export const listApprovals = id => http.get(`/transactions/${id}/approvals`)
export const listAudits = id => http.get(`/transactions/${id}/audits`)
