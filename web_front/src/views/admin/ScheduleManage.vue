<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getSchedules, deleteSchedule } from '@/api/admin'
import { Search, Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const tableData = ref([])
const loading = ref(false)

const queryParams = reactive({
  studentId: '',
  year: '2024',
  semester: '',
  week: ''
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await getSchedules(queryParams)
    if(res.code === 1) tableData.value = res.data.list
  } finally {
    loading.value = false
  }
}

const handleDelete = (id) => {
  ElMessageBox.confirm('确定删除这条课程记录吗？', '警告', { type: 'warning' })
      .then(async () => {
        const res = await deleteSchedule(id)
        if(res.code === 1) {
          ElMessage.success('删除成功')
          loadData() // 刷新
        }
      })
}

onMounted(loadData)
</script>

<template>
  <div class="manage-container">
    <div class="filter-bar glass-card">
      <el-input v-model="queryParams.studentId" placeholder="学号 (模糊)" style="width: 140px" clearable />
      <el-input v-model="queryParams.year" placeholder="学年" style="width: 100px" />
      <el-select v-model="queryParams.semester" placeholder="学期" style="width: 100px" clearable>
        <el-option label="秋季 (3)" value="3" />
        <el-option label="春季 (12)" value="12" />
      </el-select>
      <el-input v-model="queryParams.week" placeholder="周次" style="width: 80px" type="number" />

      <el-button type="primary" :icon="Search" @click="loadData">查询</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" style="width: 100%; flex: 1" height="0">
      <el-table-column prop="studentId" label="学号" width="120" />
      <el-table-column prop="courseName" label="课程名称" min-width="150" />
      <el-table-column prop="teacher" label="教师" width="100" />
      <el-table-column prop="location" label="教室" width="120" />
      <el-table-column label="时间" width="120">
        <template #default="scope">
          周{{ scope.row.dayOfWeek }} ({{ scope.row.startNode }}-{{ scope.row.endNode }}节)
        </template>
      </el-table-column>
      <el-table-column prop="rawZcd" label="周次描述" width="120" />

      <el-table-column label="操作" width="80" fixed="right">
        <template #default="scope">
          <el-button type="danger" link :icon="Delete" @click="handleDelete(scope.row.id)"></el-button>
        </template>
      </el-table-column>
    </el-table>
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
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}
</style>