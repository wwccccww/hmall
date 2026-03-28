import { createRouter, createWebHistory } from 'vue-router'
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
    path: '/admin',
    component: Layout,
    children: [
      {
        path: '',
        redirect: '/admin/dashboard'
      },
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
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
