import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../store/user'
import Layout from '../components/layout/Layout.vue'
import Dashboard from '../views/Dashboard.vue'
import Home from '../views/Home.vue'
import Login from '../views/Login.vue'
import ProductDetail from '../views/ProductDetail.vue'
import Cart from '../views/Cart.vue'
import Pay from '../views/Pay.vue'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: Home
  },
  {
    path: '/product/:id',
    name: 'ProductDetail',
    component: ProductDetail
  },
  {
    path: '/cart',
    name: 'Cart',
    component: Cart
  },
  {
    path: '/pay',
    name: 'Pay',
    component: Pay
  },
  {
    path: '/login',
    name: 'Login',
    component: Login
  },
  {
    path: '/coupons',
    name: 'CouponCenter',
    component: () => import('../views/CouponCenter.vue')
  },
  {
    path: '/admin',
    redirect: '/admin/dashboard',
    component: Layout,
    meta: { requiresAdmin: true },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: Dashboard
      },
      {
        path: 'goods/list',
        name: 'GoodsList',
        component: () => import('../views/GoodsList.vue')
      },
      {
        path: 'goods/category',
        name: 'Category',
        component: () => import('../views/Category.vue')
      },
      {
        path: 'promotion/coupons',
        name: 'CouponManage',
        component: () => import('../views/CouponManage.vue')
      }
    ]
  },
  {
    path: '/admin-login',
    name: 'AdminLogin',
    component: () => import('../views/AdminLogin.vue')
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('../views/NotFound.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, _from, next) => {
  const userStore = useUserStore()

  if (to.meta.requiresAdmin) {
    if (!userStore.isLoggedIn) {
      return next({ name: 'AdminLogin', query: { redirect: to.fullPath } })
    }
    if (!userStore.isAdmin) {
      return next({ name: 'NotFound' })
    }
  }
  next()
})

export default router
