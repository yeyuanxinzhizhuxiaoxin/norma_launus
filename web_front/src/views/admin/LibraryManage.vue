<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getLibraryUsers, saveLibraryUser, deleteLibraryUser,
  getUserTimeConfigs, addTimeConfig, updateTimeConfig, deleteTimeConfig,
  testBooking
} from '@/api/admin'
import { Plus, Edit, Delete, Timer, VideoPlay, Refresh } from '@element-plus/icons-vue'

// --- 数据状态 ---
const userList = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const timeDialogVisible = ref(false)
const testDialogVisible = ref(false)

// 用户表单
const userForm = reactive({
  studentId: '',
  password: '',
  seatLabel: '',
  sendKey: '',
  autoEnable: true
})
const isEditMode = ref(false)

// 时间配置
const currentTimeList = ref([])
const currentStudentId = ref('')
const timeForm = reactive({
  id: null,
  studentId: '',
  startTime: '',
  endTime: '',
  autoStartTime: '',
  isActive: true
})

// 测试预约
const testForm = reactive({
  studentId: '',
  seatId: null,
  startTime: '08:00',
  endTime: '22:00'
})
const testResult = ref('')

// --- 业务逻辑 ---

// 1. 加载用户列表
const loadData = async () => {
  loading.value = true
  try {
    const res = await getLibraryUsers()
    if(res.code === 1) userList.value = res.data
  } finally {
    loading.value = false
  }
}

// 2. 用户增删改
const handleEditUser = (row) => {
  isEditMode.value = !!row
  if (row) {
    Object.assign(userForm, row)
  } else {
    Object.assign(userForm, { studentId: '', password: '', seatLabel: '', sendKey: '', autoEnable: true })
  }
  dialogVisible.value = true
}

const submitUser = async () => {
  const res = await saveLibraryUser(userForm)
  if(res.code === 1) {
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadData()
  } else {
    ElMessage.error(res.msg)
  }
}

const handleDeleteUser = (studentId) => {
  ElMessageBox.confirm('删除用户将连带删除其所有时间配置，确定继续？', '警告', {
    confirmButtonText: '狠心删除',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    const res = await deleteLibraryUser(studentId)
    if(res.code === 1) {
      ElMessage.success('删除成功')
      loadData()
    }
  })
}

// 3. 时间配置管理
const openTimeDialog = async (row) => {
  currentStudentId.value = row.studentId
  timeForm.studentId = row.studentId
  timeDialogVisible.value = true
  await loadTimeConfigs()
}

const loadTimeConfigs = async () => {
  const res = await getUserTimeConfigs(currentStudentId.value)
  if(res.code === 1) currentTimeList.value = res.data
}

const submitTimeConfig = async () => {
  if(!timeForm.startTime || !timeForm.endTime || !timeForm.autoStartTime) {
    return ElMessage.warning('请填写完整时间信息')
  }
  // 如果没有ID则是新增
  const api = timeForm.id ? updateTimeConfig : addTimeConfig
  const payload = { ...timeForm }
  if(!timeForm.id) delete payload.id

  const res = await api(payload)
  if(res.code === 1) {
    ElMessage.success('操作成功')
    // 重置表单但保留 studentId
    timeForm.id = null
    timeForm.startTime = ''
    timeForm.endTime = ''
    timeForm.autoStartTime = ''
    timeForm.isActive = true
    loadTimeConfigs()
  } else {
    ElMessage.error(res.msg)
  }
}

const handleToggleTime = async (row) => {
  // 切换开关时直接调用更新
  await updateTimeConfig(row)
  ElMessage.success('状态已更新')
}

const handleDelTime = async (id) => {
  await deleteTimeConfig(id)
  loadTimeConfigs()
}

// 4. 测试预约
const openTestDialog = (row) => {
  testForm.studentId = row.studentId
  testForm.seatId = null
  testResult.value = ''
  testDialogVisible.value = true
}

const runTest = async () => {
  testResult.value = '正在请求图书馆接口，请稍候...'
  try {
    const res = await testBooking(testForm)
    if(res.code === 1) {
      testResult.value = `✅ 成功:\n${res.data}`
    } else {
      testResult.value = `❌ 失败 (${res.msg}):\n${res.data || ''}` // data里可能放了原文
    }
  } catch (e) {
    testResult.value = '请求异常'
  }
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="admin-container">
    <div class="action-header">
      <div class="title">用户预约监控</div>
      <el-button type="primary" :icon="Plus" @click="handleEditUser(null)" round>添加用户</el-button>
    </div>

    <el-table :data="userList" v-loading="loading" style="width: 100%; height: calc(100vh - 180px);">
      <el-table-column prop="studentId" label="学号" width="140" sortable />
      <el-table-column prop="seatLabel" label="默认座位" width="120">
        <template #default="scope">
          <el-tag effect="light" round>{{ scope.row.seatLabel }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="autoEnable" label="自动预约" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.autoEnable ? 'success' : 'info'" effect="dark">
            {{ scope.row.autoEnable ? '开启' : '关闭' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="updateTime" label="最后更新" width="180">
        <template #default="scope">
          <span style="font-size: 12px; opacity: 0.8">{{ scope.row.updateTime?.replace('T', ' ') }}</span>
        </template>
      </el-table-column>

      <el-table-column label="操作" min-width="280" fixed="right">
        <template #default="scope">
          <el-button-group>
            <el-button type="primary" plain :icon="Timer" size="small" @click="openTimeDialog(scope.row)">时间段</el-button>
            <el-button type="warning" plain :icon="VideoPlay" size="small" @click="openTestDialog(scope.row)">测试</el-button>
            <el-button type="info" plain :icon="Edit" size="small" @click="handleEditUser(scope.row)"></el-button>
            <el-button type="danger" plain :icon="Delete" size="small" @click="handleDeleteUser(scope.row.studentId)"></el-button>
          </el-button-group>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="isEditMode ? '编辑用户' : '添加用户'" width="450px">
      <el-form :model="userForm" label-width="90px">
        <el-form-item label="学号">
          <el-input v-model="userForm.studentId" :disabled="isEditMode" placeholder="请输入学号" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="userForm.password" type="password" show-password placeholder="图书馆密码" />
        </el-form-item>
        <el-form-item label="默认座位">
          <el-input v-model="userForm.seatLabel" placeholder="如 03EN11F (自动解析)" />
        </el-form-item>
        <el-form-item label="Server酱">
          <el-input v-model="userForm.sendKey" placeholder="SendKey (选填)" />
        </el-form-item>
        <el-form-item label="总开关">
          <el-switch v-model="userForm.autoEnable" active-text="开启自动预约" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitUser">保存配置</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="timeDialogVisible" title="预约策略配置" width="700px">
      <div class="time-form-inline glass-card">
        <span class="label">添加新策略:</span>
        <el-time-select v-model="timeForm.startTime" start="06:00" step="00:15" end="22:00" placeholder="开始" style="width: 110px"/>
        <span class="sep">-</span>
        <el-time-select v-model="timeForm.endTime" start="06:00" step="00:15" end="22:00" placeholder="结束" style="width: 110px"/>
        <span class="sep">触发点:</span>
        <el-time-select v-model="timeForm.autoStartTime" start="05:50" step="00:01" end="22:00" placeholder="自动触发" style="width: 110px"/>
        <el-button type="success" :icon="Plus" circle style="margin-left: auto" @click="submitTimeConfig" />
      </div>

      <el-table :data="currentTimeList" style="margin-top: 20px" max-height="300px">
        <el-table-column label="预约时段">
          <template #default="scope">
            <span style="font-weight: bold">{{ scope.row.startTime }}</span> ~ <span>{{ scope.row.endTime }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="autoStartTime" label="触发时间" width="120">
          <template #default="scope">
            <el-tag type="warning" effect="plain">{{ scope.row.autoStartTime }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="启用" width="80">
          <template #default="scope">
            <el-switch v-model="scope.row.isActive" @change="handleToggleTime(scope.row)" size="small" />
          </template>
        </el-table-column>
        <el-table-column width="60">
          <template #default="scope">
            <el-button type="danger" link :icon="Delete" @click="handleDelTime(scope.row.id)"></el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog v-model="testDialogVisible" title="🚀 实时预约测试" width="500px">
      <el-alert title="注意：这将发起真实的预约请求，若成功会占用座位！" type="warning" :closable="false" style="margin-bottom: 15px"/>
      <el-form :model="testForm" label-width="80px">
        <el-form-item label="测试用户">
          <el-input v-model="testForm.studentId" disabled />
        </el-form-item>
        <el-row :gutter="10">
          <el-col :span="12">
            <el-form-item label="开始">
              <el-time-select v-model="testForm.startTime" start="06:00" step="00:30" end="22:00" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束">
              <el-time-select v-model="testForm.endTime" start="06:00" step="00:30" end="22:00" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="临时座位">
          <el-input v-model="testForm.seatId" placeholder="为空则使用默认座位ID" type="number"/>
        </el-form-item>
      </el-form>

      <div class="test-console glass-card" v-if="testResult">
        <pre>{{ testResult }}</pre>
      </div>

      <template #footer>
        <el-button @click="testDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="runTest" :loading="testResult.startsWith('正在')">发起请求</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.admin-container {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.action-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.title {
  font-size: 18px;
  font-weight: 600;
  color: var(--glass-text-color);
}

.time-form-inline {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 15px;
  border: 1px dashed rgba(0,0,0,0.1);
  background: rgba(255,255,255,0.2);
}

.label {
  font-weight: bold;
  font-size: 14px;
}

.sep {
  color: #666;
}

.test-console {
  margin-top: 15px;
  padding: 10px;
  background: rgba(0, 0, 0, 0.8);
  color: #0f0;
  border-radius: 4px;
  max-height: 150px;
  overflow: auto;
  font-family: monospace;
  font-size: 12px;
  white-space: pre-wrap;
}
</style>