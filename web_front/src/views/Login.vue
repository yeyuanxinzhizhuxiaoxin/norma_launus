<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Right } from '@element-plus/icons-vue'
import { adminLogin } from '@/api/admin' // 确保这里引入了 API

const router = useRouter()
const loading = ref(false)
const rememberMe = ref(true) // 默认勾选记住密码

const form = reactive({
  username: '',
  password: ''
})

// 页面加载时：检查本地存储
onMounted(() => {
  const savedAdmin = localStorage.getItem('admin_credentials')
  if (savedAdmin) {
    try {
      const parsed = JSON.parse(savedAdmin)
      form.username = parsed.username
      form.password = parsed.password // 注意：生产环境建议加密存储或只存Token
      // 可选：如果希望自动登录，可以在这里直接调用 handleLogin()
    } catch (e) {
      localStorage.removeItem('admin_credentials')
    }
  }
})

const handleLogin = async () => {
  if (!form.username || !form.password) {
    return ElMessage.warning('请输入账号和密码')
  }

  loading.value = true
  try {
    // 构造后端需要的 DTO 格式
    const loginData = {
      account: form.username,
      password: form.password
    }

    // 1. 调用真实后端接口
    const res = await adminLogin(loginData)

    if (res.code === 1) {
      ElMessage.success(`欢迎回来，${res.data.name}`)

      // 2. 保存 Token 用于后续请求鉴权
      // 这里的 key 'loginUser' 是为了配合 request.js 中的拦截器
      // 你的 request.js 读取的是: JSON.parse(localStorage.getItem('loginUser')).token
      const tokenObj = {
        token: res.data.token,
        name: res.data.name,
        role: res.data.role
      }
      localStorage.setItem('loginUser', JSON.stringify(tokenObj))

      // 3. 处理"记住密码" (用于下次自动填充)
      if (rememberMe.value) {
        localStorage.setItem('admin_credentials', JSON.stringify({
          username: form.username,
          password: form.password
        }))
      } else {
        localStorage.removeItem('admin_credentials')
      }

      // 4. 跳转
      router.push('/admin/library')
    } else {
      ElMessage.error(res.msg || '登录失败')
    }

  } catch (error) {
    console.error(error)
    ElMessage.error('网络请求异常或服务器未启动')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-container">
    <div class="glass-card login-box">
      <div class="header">
        <h1>Partner Admin</h1>
        <p>校园助手后台管理系统</p>
      </div>

      <el-form :model="form" class="login-form" size="large">
        <el-form-item>
          <el-input
              v-model="form.username"
              placeholder="管理员账号"
              :prefix-icon="User"
              class="glass-input"
          />
        </el-form-item>
        <el-form-item>
          <el-input
              v-model="form.password"
              type="password"
              placeholder="密码"
              show-password
              :prefix-icon="Lock"
              class="glass-input"
              @keyup.enter="handleLogin"
          />
        </el-form-item>

        <div class="remember-box">
          <el-checkbox v-model="rememberMe" label="记住密码 (下次自动填充)"
                       style="color: #fff; --el-checkbox-checked-text-color: #fff;"/>
        </div>

        <el-button
            type="primary"
            class="login-btn"
            :loading="loading"
            @click="handleLogin"
            round
        >
          登录系统
          <el-icon class="el-icon--right">
            <Right/>
          </el-icon>
        </el-button>
      </el-form>
    </div>
  </div>
</template>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
}

.login-box {
  width: 400px;
  padding: 40px;
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.2);
  display: flex;
  flex-direction: column;
  align-items: center;
}

.header {
  text-align: center;
  margin-bottom: 30px;
  color: #fff;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
}

.header h1 {
  margin: 0;
  font-size: 32px;
  font-weight: 700;
  letter-spacing: 2px;
}

.header p {
  margin: 10px 0 0;
  font-size: 14px;
  opacity: 0.8;
}

.login-form {
  width: 100%;
}

.remember-box {
  margin-bottom: 15px;
  margin-left: 5px;
}

/* 输入框样式定制 */
:deep(.el-input__wrapper) {
  background-color: rgba(255, 255, 255, 0.2) !important;
  box-shadow: none !important;
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 25px;
  color: #fff;
}

:deep(.el-input__inner) {
  color: #fff !important;
  height: 45px;
}

:deep(.el-input__inner::placeholder) {
  color: rgba(255, 255, 255, 0.7);
}

.login-btn {
  width: 100%;
  height: 45px;
  font-size: 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  box-shadow: 0 4px 15px rgba(118, 75, 162, 0.4);
  transition: transform 0.2s;
}

.login-btn:hover {
  transform: translateY(-2px);
  background: linear-gradient(135deg, #764ba2 0%, #667eea 100%);
}
</style>