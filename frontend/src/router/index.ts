import { createRouter, createWebHistory } from 'vue-router'
import { hasAnyRole } from '../utils/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('../views/auth/Login.vue')
    },
    {
      path: '/',
      redirect: '/applications'
    },
    {
      path: '/applications',
      name: 'ApplicationList',
      component: () => import('../views/application/ApplicationList.vue')
    },
    {
      path: '/suppliers',
      name: 'SupplierList',
      component: () => import('../views/supplier/SupplierList.vue')
    },
    {
      path: '/suppliers/comparison/:id',
      name: 'PriceComparison',
      component: () => import('../views/supplier/PriceComparisonPage.vue')
    },
    {
      path: '/departments',
      name: 'DepartmentList',
      component: () => import('../views/department/DepartmentList.vue')
    },
    {
      path: '/users',
      name: 'UserList',
      component: () => import('../views/user/UserList.vue')
    },
    {
      path: '/roles',
      name: 'RoleList',
      component: () => import('../views/role/RoleList.vue')
    }
  ]
})

router.beforeEach((to) => {
  const token = localStorage.getItem('token')

  if (to.path !== '/login' && !token) {
    return '/login'
  }

  if (to.path === '/login' && token) {
    return '/applications'
  }

  // 供应商管理：采购员/采购负责人/管理员可访问
  if (to.path === '/suppliers' && !hasAnyRole(['BUYER', 'PURCHASE_MANAGER', 'ADMIN'])) {
    return '/applications'
  }

  // 比价分析页面仅采购负责人和管理员可访问
  if (to.path.startsWith('/suppliers/comparison') && !hasAnyRole(['PURCHASE_MANAGER', 'ADMIN'])) {
    return '/applications'
  }
})

export default router
