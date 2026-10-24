import Vue from 'vue'
import VueRouter from 'vue-router'
import { isLoggedIn, getSession } from '../auth/session'

Vue.use(VueRouter)

const router = new VueRouter({
  routes: [
    { path: '/login', name: 'login', component: () => import('../views/Login.vue'), meta: { public: true } },
    { path: '/', redirect: '/dashboard' },
    { path: '/dashboard', name: 'dashboard', component: () => import('../views/Dashboard.vue'), meta: { title: '工作台' } },
    { path: '/transactions', name: 'transactions', component: () => import('../views/TransactionList.vue'), meta: { title: '交易申请查询' } },
    { path: '/transactions/new', name: 'transaction-new', component: () => import('../views/TransactionForm.vue'), meta: { title: '新建交易申请', roles: ['APPLICANT'] } },
    { path: '/transactions/:id/edit', name: 'transaction-edit', component: () => import('../views/TransactionForm.vue'), meta: { title: '驳回重提', roles: ['APPLICANT'] } },
    { path: '/transactions/:id', name: 'transaction-detail', component: () => import('../views/TransactionDetail.vue'), meta: { title: '交易申请详情' } },
    { path: '*', redirect: '/dashboard' }
  ]
})

router.beforeEach((to, from, next) => {
  if (!to.meta.public && !isLoggedIn()) return next({ name: 'login', query: { redirect: to.fullPath } })
  if (to.name === 'login' && isLoggedIn()) return next('/dashboard')
  if (to.meta.roles && !to.meta.roles.includes((getSession() || {}).role)) return next('/transactions')
  next()
})

export default router
