<script setup>
import {ref, reactive, onMounted} from 'vue'
import {getClients, addClient, deleteClient} from '@/api/admin'
import {Plus, Delete, Search} from '@element-plus/icons-vue'
import {ElMessage, ElMessageBox} from 'element-plus'

const tableData = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const total = ref(0) // 总条数

// 查询参数
const queryParams = reactive({
  keyword: '',
  page: 1,
  size: 10
})

// 表单数据
const form = reactive({
  account: '',
  password: '',
  name: ''
})

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    // 调用后端接口
    const res = await getClients(queryParams)
    if (res.code === 1) {
      // 后端返回的是 Map: { list: [...], total: 100 }
      tableData.value = res.data.list
      total.value = res.data.total
    }
  } finally {
    loading.value = false
  }
}

// 分页事件处理
const handleSizeChange = (val) => {
  queryParams.size = val
  loadData()
}

const handleCurrentChange = (val) => {
  queryParams.page = val
  loadData()
}

// 提交新增
const handleSubmit = async () => {
  const res = await addClient(form)
  if (res.code === 1) {
    ElMessage.success('添加成功')
    dialogVisible.value = false
    // 重置表单
    form.account = ''
    form.password = ''
    form.name = ''
    loadData()
  } else {
    ElMessage.error(res.msg)
  }
}

// 删除用户
const handleDelete = (id) => {
  ElMessageBox.confirm('确定删除该用户吗？此操作不可恢复。', '提示', {type: 'warning'})
      .then(async () => {
        const res = await deleteClient(id)
        if (res.code === 1) {
          ElMessage.success('删除成功')
          loadData()
        }
      })
}

onMounted(loadData)
</script>

<template>
  <div class="manage-container">
    <div class="filter-bar glass-card">
      <el-input
          v-model="queryParams.keyword"
          placeholder="搜索账号/姓名..."
          style="width: 200px"
          :prefix-icon="Search"
          clearable
          @clear="loadData"
          @keyup.enter="loadData"
      />
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button type="success" :icon="Plus" @click="dialogVisible = true" style="margin-left: auto">新增用户
      </el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" style="width: 100%; flex: 1">
      <el-table-column prop="id" label="ID" width="80"/>
      <el-table-column prop="account" label="账号 (学号)" width="180" sortable/>
      <el-table-column prop="name" label="姓名"/>
      <el-table-column prop="role" label="角色" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.role === 1 ? 'danger' : 'info'">
            {{ scope.row.role === 1 ? '管理员' : '学生' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="scope">
          <el-button type="danger" link :icon="Delete" @click="handleDelete(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-bar glass-card">
      <el-pagination
          v-model:current-page="queryParams.page"
          v-model:page-size="queryParams.size"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
      />
    </div>

    <el-dialog v-model="dialogVisible" title="新增系统用户" width="400px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="账号">
          <el-input v-model="form.account" placeholder="请输入学号"/>
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" show-password placeholder="初始密码"/>
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="form.name" placeholder="用户昵称"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.manage-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.filter-bar {
  padding: 15px;
  display: flex;
  gap: 10px;
  align-items: center;
}

.pagination-bar {
  padding: 10px 20px;
  display: flex;
  justify-content: flex-end;
}

/* 适配毛玻璃背景的表格样式微调已在全局 css 中定义 */
</style>