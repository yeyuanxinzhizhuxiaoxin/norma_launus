import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/Login.vue'
import GlassLayout from '../layout/GlassLayout.vue'

// 懒加载组件
const LibraryManage = () => import('../views/admin/LibraryManage.vue')
const UserManage = () => import('../views/admin/UserManage.vue')
const ScheduleManage = () => import('../views/admin/ScheduleManage.vue')
const ScoreManage = () => import('../views/admin/ScoreManage.vue')

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/login'
    },
    {
      path: '/login',
      name: 'Login',
      component: LoginView
    },
    {
      path: '/admin',
      component: GlassLayout,
      redirect: '/admin/library',
      children: [
        {
          path: 'library',
          name: 'LibraryManage',
          component: LibraryManage,
          meta: { title: '图书馆预约' }
        },
        {
          path: 'users',
          name: 'UserManage',
          component: UserManage,
          meta: { title: '用户管理' }
        },
        {
          path: 'schedules',
          name: 'ScheduleManage',
          component: ScheduleManage,
          meta: { title: '课表管理' }
        },
        {
          path: 'scores',
          name: 'ScoreManage',
          component: ScoreManage,
          meta: { title: '成绩管理' }
        }
      ]
    }
  ]
})

export default router