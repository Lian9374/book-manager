import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { guest: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue'),
    meta: { guest: true }
  },
  {
    path: '/',
    name: 'Dashboard',
    component: () => import('../views/Dashboard.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/books',
    name: 'Books',
    component: () => import('../views/books/BookList.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/books/add',
    name: 'BookAdd',
    component: () => import('../views/books/BookForm.vue'),
    meta: { requiresAuth: true, roles: ['LIBRARIAN', 'ADMIN'] }
  },
  {
    path: '/books/:id',
    name: 'BookDetail',
    component: () => import('../views/books/BookDetail.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/categories',
    name: 'Categories',
    component: () => import('../views/books/CategoryManagement.vue'),
    meta: { requiresAuth: true, roles: ['ADMIN'] }
  },
  {
    path: '/borrows',
    name: 'Borrows',
    component: () => import('../views/borrow/BorrowList.vue'),
    meta: { requiresAuth: true, roles: ['LIBRARIAN', 'ADMIN'] }
  },
  {
    path: '/my-borrows',
    name: 'MyBorrows',
    component: () => import('../views/borrow/MyBorrows.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/reservations',
    name: 'Reservations',
    component: () => import('../views/reservation/ReservationList.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/users',
    name: 'Users',
    component: () => import('../views/user/UserList.vue'),
    meta: { requiresAuth: true, roles: ['ADMIN'] }
  },
  {
    path: '/statistics',
    name: 'Statistics',
    component: () => import('../views/statistics/StatisticsView.vue'),
    meta: { requiresAuth: true, roles: ['LIBRARIAN', 'ADMIN'] }
  },
  {
    path: '/fines',
    name: 'Fines',
    component: () => import('../views/fine/FineList.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/config',
    name: 'Config',
    component: () => import('../views/config/ConfigView.vue'),
    meta: { requiresAuth: true, roles: ['ADMIN'] }
  },
  {
    path: '/notifications',
    name: 'Notifications',
    component: () => import('../views/NotificationList.vue'),
    meta: { requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('accessToken')
  const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
  const roles = userInfo.roles || []

  if (to.meta.requiresAuth && !token) {
    return next('/login')
  }

  if (to.meta.guest && token) {
    return next('/')
  }

  if (to.meta.roles && !to.meta.roles.some(r => roles.includes(r))) {
    return next('/')
  }

  next()
})

export default router
