<script setup>
import { RouterView, useRouter } from 'vue-router'
import { SwitchButton, Reading, Setting, UserFilled } from '@element-plus/icons-vue'

const router = useRouter()
const handleLogout = () => {
  localStorage.removeItem('loginUser')
  localStorage.removeItem('JSESSIONID')
  router.push('/login')
}
</script>

<template>
  <div class="layout-container">
    <aside class="sidebar glass-card">
      <div class="logo">
        <h2>Partner Admin</h2>
      </div>
      <el-menu
          :default-active="$route.path"
          class="glass-menu"
          router
          text-color="#2c3e50"
          active-text-color="#409EFF"
      >
        <el-menu-item index="/admin/library">
          <el-icon><Reading /></el-icon>
          <span>图书馆预约</span>
        </el-menu-item>

        <el-menu-item index="/admin/users">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>

        <el-menu-item index="/admin/schedules">
          <el-icon><Calendar /></el-icon>
          <span>课表管理</span>
        </el-menu-item>

        <el-menu-item index="/admin/scores">
          <el-icon><TrendCharts /></el-icon>
          <span>成绩管理</span>
        </el-menu-item>
      </el-menu>
      <div class="user-info">
        <el-avatar :icon="UserFilled" size="small" style="background: rgba(255,255,255,0.5); color:#333"/>
        <span style="margin-left: 10px; font-size: 14px;">管理员</span>
      </div>
    </aside>

    <main class="main-content">
      <header class="top-bar glass-card">
        <div class="breadcrumb">后台管理 / 图书馆自动预约监控</div>
        <el-button type="danger" :icon="SwitchButton" circle plain @click="handleLogout" title="退出登录"/>
      </header>

      <div class="content-view glass-card">
        <RouterView />
      </div>
    </main>
  </div>
</template>

<style scoped>
.layout-container {
  display: flex;
  height: 100vh;
  padding: 20px;
  gap: 20px;
  box-sizing: border-box;
}

.sidebar {
  width: 240px;
  display: flex;
  flex-direction: column;
  padding: 20px 0;
}

.logo h2 {
  text-align: center;
  margin: 0 0 30px 0;
  font-size: 20px;
  color: #2c3e50;
  letter-spacing: 1px;
}

.glass-menu {
  background: transparent !important;
  border-right: none !important;
  flex: 1;
}

.user-info {
  padding: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-top: 1px solid rgba(255,255,255,0.3);
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 20px;
  overflow: hidden;
}

.top-bar {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 25px;
  font-weight: 500;
}

.content-view {
  flex: 1;
  padding: 20px;
  overflow: hidden; /* 内部滚动由子组件处理 */
  position: relative;
}

/* 菜单项悬停效果优化 */
:deep(.el-menu-item) {
  border-radius: 0 50px 50px 0;
  margin-right: 10px;
  height: 50px;
}
:deep(.el-menu-item:hover) {
  background-color: rgba(255, 255, 255, 0.4) !important;
}
:deep(.el-menu-item.is-active) {
  background-color: rgba(255, 255, 255, 0.6) !important;
  font-weight: bold;
  box-shadow: 2px 2px 10px rgba(0,0,0,0.05);
}
</style>