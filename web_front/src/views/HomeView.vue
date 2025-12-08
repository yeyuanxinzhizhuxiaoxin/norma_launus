<script setup>
import { ref } from 'vue'
import {loginApi, queryScoreApi} from '@/api/client'

const form = ref({
  account: '',
  password: ''
})
const form_score = ref({
  xnmmc: '',
  xqmmc: '',
  JSESSIONID:'',
  route:''
})

const loading = ref(false)
const result = ref('')
const error = ref('')
const scoreList = ref([])

const year = ref('')
const semester = ref('')
let id= ''
let total=''
const handleLogin = async () => {
  if (!form.value.account || !form.value.password) {
    alert('请填写账号和密码')
    return
  }

  loading.value = true
  result.value = ''
  error.value = ''

  try {
    // 调用后端登录接口
    const res = await loginApi(form.value)
    console.log('登录成功，返回内容:', res)
    result.value = res.data
    // 存储到 localStorage
    localStorage.setItem('JSESSIONID', res.data.JSESSIONID)
    localStorage.setItem('account',form.value.account)
    localStorage.setItem('route',res.data.route)
    error.value = ''
  } catch (err) {
    console.error('登录失败:', err)
    error.value = err.message || '请求失败'
    result.value = ''
  } finally {
    loading.value = false
  }
}

const queryScore = async () => {
  //1.将cookies传入
  form_score.value.JSESSIONID= localStorage.getItem('JSESSIONID')
  form_score.value.route = localStorage.getItem('route')

  try {
    // 调用后端登录接口
    const res = await queryScoreApi(form_score.value)
    console.log('登录成功，返回内容:', res)
    scoreList.value = res.data

    const data = res.data || []
    total = data.length
    if (data.length > 0) {
      year.value = data[0].year
      // 将数字学期转为中文描述（可选）
      const semMap = { '1': '第1学期', '2': '第2学期', '3': '第3学期' }
      semester.value = semMap[data[0].semester] || `第${data[0].semester}学期`
    } else {
      year.value = ''
      semester.value = ''
    }
    id = localStorage.getItem('account')
    error.value = ''
  } catch (err) {
    console.error('查询错误:', err)
    error.value = err.message || '请求失败'
    result.value = ''
  } finally {
    loading.value = false
  }

}
</script>

<template>
  <div>
    <h2>客户登录测试（模拟教务系统登录）</h2>

    <el-form :model="form" label-width="80px" @submit.prevent="handleLogin">
      <el-form-item label="账号">
        <el-input v-model="form.account" placeholder="请输入学号/工号" />
      </el-form-item>
      <el-form-item label="密码">
        <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            show-password
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" native-type="submit" :loading="loading">
          登录并进入教务系统
        </el-button>
      </el-form-item>
    </el-form>

    <!-- 显示结果 -->
    <div style="margin-top: 20px;">
      <h3>教务系统返回内容（片段）：</h3>
      <pre>{{ result.substring(0, 1000) }}...</pre>
    </div>

    <el-form :model="form_score" label-width="80px" @submit.prevent="queryScore">
      <el-form-item label="学年">
        <el-input v-model="form_score.xnmmc" placeholder="请输入学年" />
      </el-form-item>
      <el-form-item label="学期">
        <el-input v-model="form_score.xqmmc"  placeholder="请输入学年" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" native-type="submit">
          查询成绩
        </el-button>
      </el-form-item>
    </el-form>

    <div style="margin-top: 20px;">
      <h3>{{id}} {{year}}{{semester}}总门数{{total}}</h3>
      <el-table :data="scoreList" style="width: 100%">
        <el-table-column prop="courseName" label="课程名称" />
        <el-table-column prop="credit" label="课程学分" width="100" align="center"/>
        <el-table-column prop="point" label="绩点" width="80" align="center"/>
        <el-table-column prop="grade" label="成绩" width="80" align="center"/>
        <el-table-column prop="gpa" label="学分绩点" width="100" align="center"/>
      </el-table>
    </div>



    <div v-if="error" style="margin-top: 20px; color: red;">
      <h3>错误信息：</h3>
      <p>{{ error }}</p>
    </div>
  </div>
</template>
<style scoped>
</style>