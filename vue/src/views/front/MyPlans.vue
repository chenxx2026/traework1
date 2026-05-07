<template>
  <div style="width: 80%; margin: 20px auto">
    <div style="padding-left: 10px; border-left: 5px solid #189500; font-size: 20px; margin-bottom: 20px">我的行程表</div>
    
    <!-- 加载状态 -->
    <div v-if="data.loading" style="text-align: center; padding: 100px 0">
      <el-icon :size="50" class="is-loading"><Loading /></el-icon>
      <p style="margin-top: 20px; color: #909399">正在加载...</p>
    </div>
    
    <!-- 错误状态 -->
    <div v-else-if="data.error" style="text-align: center; padding: 100px 0">
      <el-icon :size="50" style="color: #f56c6c"><Warning /></el-icon>
      <p style="margin-top: 20px; color: #909399">{{ data.errorMessage }}</p>
      <el-button type="primary" @click="loadPlans" style="margin-top: 20px">重试</el-button>
    </div>
    
    <!-- 空数据 -->
    <div v-else-if="data.plans.length === 0" style="text-align: center; padding: 50px; color: #909399">
      <el-empty description="暂无行程表"></el-empty>
      <el-button type="primary" @click="router.push('/front/travelPlanner')">去规划行程</el-button>
    </div>
    
    <!-- 正常列表 -->
    <el-row :gutter="20" v-else>
      <el-col :span="8" v-for="item in data.plans" :key="item.id" style="margin-bottom: 20px">
        <el-card class="plan-card" shadow="hover">
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center">
              <span style="font-weight: bold">{{ item.destination }}</span>
            </div>
          </template>
          
          <div style="min-height: 100px">
            <p><strong>出行时间:</strong> {{ item.startDate }} 至 {{ item.endDate }}</p>
            <p><strong>创建时间:</strong> {{ formatDate(item.createTime) }}</p>
          </div>
          
          <el-divider></el-divider>
          
          <div style="display: flex; gap: 10px; justify-content: center">
            <el-button size="small" type="primary" @click="viewDetail(item)">查看</el-button>
            <el-button size="small" @click="sharePlan(item)">分享</el-button>
            <el-button size="small" type="danger" @click="deletePlan(item)">删除</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { reactive, onMounted } from "vue"
import { ElMessage, ElMessageBox } from "element-plus"
import { useRouter } from "vue-router"
import request from "@/utils/request.js"
import { Loading, Warning } from "@element-plus/icons-vue"

const router = useRouter()

const data = reactive({
  loading: true,
  error: false,
  errorMessage: '',
  plans: []
})

const loadPlans = async () => {
  data.loading = true
  data.error = false
  data.errorMessage = ''
  
  try {
    const user = JSON.parse(localStorage.getItem('xm-user') || '{}')
    if (!user.id) {
      ElMessage.warning('请先登录')
      router.push('/login')
      return
    }
    
    const res = await request.get('/travelPlan/selectAll')
    if (res.code === '200') {
      data.plans = res.data || []
    } else {
      data.error = true
      data.errorMessage = res.msg || '加载失败'
    }
  } catch (error) {
    console.error('加载行程失败:', error)
    data.error = true
    data.errorMessage = '网络异常，请检查网络连接后重试'
  } finally {
    data.loading = false
  }
}

const formatDate = (dateStr) => {
  return new Date(dateStr).toLocaleString('zh-CN')
}

const viewDetail = (item) => {
  router.push(`/front/planDetail?id=${item.id}`)
}

const sharePlan = (item) => {
  const shareText = `我规划了去${item.destination}的行程！时间：${item.startDate} - ${item.endDate}`
  if (navigator.clipboard) {
    navigator.clipboard.writeText(shareText)
    ElMessage.success('分享内容已复制到剪贴板')
  } else {
    ElMessage.success(shareText)
  }
}

const deletePlan = (item) => {
  ElMessageBox.confirm('确定要删除这个行程表吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    request.delete(`/travelPlan/delete/${item.id}`).then(res => {
      if (res.code === '200') {
        ElMessage.success('删除成功')
        loadPlans()
      } else {
        ElMessage.error(res.msg || '删除失败')
      }
    })
  }).catch(() => {})
}

onMounted(() => {
  loadPlans()
})
</script>

<style scoped>
.plan-card:hover {
  transform: translateY(-5px);
  transition: all 0.3s ease;
}
</style>
