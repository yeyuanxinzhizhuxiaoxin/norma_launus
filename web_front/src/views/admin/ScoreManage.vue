<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getScores, deleteScore } from '@/api/admin'
import { Search, Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const tableData = ref([])
const loading = ref(false)

const queryParams = reactive({
  studentId: '',
  year: '',
  semester: ''
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await getScores(queryParams)
    if(res.code === 1) tableData.value = res.data
  } finally {
    loading.value = false
  }
}

const handleDelete = (id) => {
  ElMessageBox.confirm('确定删除这条成绩记录吗？', '警告', { type: 'warning' })
      .then(async () => {
        const res = await deleteScore(id)
        if(res.code === 1) {
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
      <el-input v-model="queryParams.studentId" placeholder="学号 (模糊)" style="width: 150px" clearable />
      <el-input v-model="queryParams.year" placeholder="学年 (如2024-2025)" style="width: 160px" />
      <el-input v-model="queryParams.semester" placeholder="学期 (如1)" style="width: 100px" />

      <el-button type="primary" :icon="Search" @click="loadData">查询</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" style="width: 100%; flex: 1" height="0">
      <el-table-column prop="studentId" label="学号" width="130" />
      <el-table-column prop="courseName" label="课程名称" min-width="150" />
      <el-table-column prop="score" label="成绩" width="80" align="center">
        <template #default="scope">
          <span :style="{ color: parseFloat(scope.row.score) < 60 ? 'red' : 'inherit', fontWeight: 'bold' }">
            {{ scope.row.score }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="gradePoint" label="绩点" width="80" align="center" />
      <el-table-column prop="credit" label="学分" width="80" align="center" />
      <el-table-column prop="examType" label="性质" width="100" />
      <el-table-column prop="semester" label="学期" width="140" />

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
  gap: 10px;
  align-items: center;
}
</style>