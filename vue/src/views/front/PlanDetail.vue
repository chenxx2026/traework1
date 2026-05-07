<template>
  <div>
    <div class="bg"></div>
    <div style="width: 80%; margin: 20px auto">
      <!-- 加载状态 -->
      <div v-if="data.loading" style="text-align: center; padding: 100px 0">
        <el-icon :size="50" class="is-loading"><Loading /></el-icon>
        <p style="margin-top: 20px; color: #909399">正在加载行程...</p>
      </div>
      
      <!-- 错误状态 -->
      <div v-else-if="data.error" style="text-align: center; padding: 100px 0">
        <el-icon :size="50" style="color: #f56c6c"><Warning /></el-icon>
        <p style="margin-top: 20px; color: #909399">{{ data.errorMessage }}</p>
        <div style="margin-top: 20px">
          <el-button type="primary" @click="loadPlan">重试</el-button>
          <el-button @click="goBack">返回</el-button>
        </div>
      </div>
      
      <!-- 正常显示 -->
      <div v-else>
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px">
          <div style="padding-left: 10px; border-left: 5px solid #189500; font-size: 20px">
            {{ data.plan?.destination || '行程详情' }}
          </div>
          <div>
            <el-button type="primary" @click="goBack">返回</el-button>
            <el-button @click="editPlan">编辑</el-button>
          </div>
        </div>

        <!-- 基本信息卡片 -->
        <el-card style="margin-bottom: 20px">
          <template #header>
            <span style="font-size: 18px; font-weight: bold">行程基本信息</span>
          </template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="目的地">{{ data.plan?.destination }}</el-descriptions-item>
            <el-descriptions-item label="出行时间">{{ data.plan?.startDate }} 至 {{ data.plan?.endDate }}</el-descriptions-item>
            <el-descriptions-item label="总预算">{{ data.plan?.totalBudget ? '¥' + data.plan.totalBudget : '未设置' }}</el-descriptions-item>
            <el-descriptions-item label="每日预算">{{ data.plan?.dailyBudget ? '¥' + data.plan.dailyBudget : '未设置' }}</el-descriptions-item>
            <el-descriptions-item label="创建时间" :span="2">{{ formatDate(data.plan?.createTime) }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
        
        <!-- 根据数据类型显示不同内容 -->
        <div v-if="data.isJsonFormat">
          <!-- JSON格式的详细行程 -->
          <el-card v-for="(day, dayIndex) in data.result.dailyPlans" :key="dayIndex" class="plan-card" style="margin-bottom: 20px">
            <template #header>
              <div style="display: flex; justify-content: space-between; align-items: center">
                <span style="font-size: 18px; font-weight: bold">第 {{ dayIndex + 1 }} 天 - {{ day.date }}</span>
                <el-tag type="success" v-if="day.totalCost">当日预算: ¥{{ day.totalCost }}</el-tag>
              </div>
            </template>
            
            <el-timeline v-if="day.nodes && day.nodes.length">
              <el-timeline-item v-for="(node, nodeIndex) in day.nodes" :key="nodeIndex" :timestamp="node.time" placement="top">
                <el-card class="timeline-card">
                  <div style="display: flex; justify-content: space-between; margin-bottom: 10px">
                    <h4 style="margin: 0">{{ node.name }}</h4>
                    <el-tag v-if="node.type === 'scenic'" type="danger">景点</el-tag>
                    <el-tag v-else-if="node.type === 'food'" type="success">餐饮</el-tag>
                    <el-tag v-else-if="node.type === 'transport'" type="info">交通</el-tag>
                    <el-tag v-else-if="node.type === 'hotel'" type="warning">住宿</el-tag>
                  </div>
                  
                  <div v-if="node.type === 'scenic'" style="font-size: 14px; color: #666">
                    <p><strong>地址:</strong> {{ node.address }}</p>
                    <p><strong>开放时间:</strong> {{ node.openTime }}</p>
                    <p><strong>票价:</strong> ¥{{ node.ticketPrice }}</p>
                    <p><strong>游玩时长:</strong> {{ node.duration }}</p>
                    <p><strong>游玩建议:</strong> {{ node.suggestion }}</p>
                  </div>
                  
                  <div v-if="node.transport">
                    <el-divider content-position="left">前往下一站</el-divider>
                    <p><strong>交通方式:</strong> {{ node.transport.type }}</p>
                    <p><strong>路线:</strong> {{ node.transport.route }}</p>
                    <p><strong>预计时间:</strong> {{ node.transport.time }}</p>
                    <p><strong>费用:</strong> ¥{{ node.transport.cost }}</p>
                  </div>
                  
                  <div v-if="node.cost" style="margin-top: 10px; color: #f56c6c; font-weight: bold">
                    费用: ¥{{ node.cost }}
                  </div>
                </el-card>
              </el-timeline-item>
            </el-timeline>
            
            <div v-if="day.costBreakdown" style="background: #f5f7fa; padding: 15px; border-radius: 4px; margin-top: 15px">
              <h5 style="margin-top: 0">当日费用明细</h5>
              <el-row :gutter="20">
                <el-col :span="8"><p>景点门票: ¥{{ day.costBreakdown.tickets || 0 }}</p></el-col>
                <el-col :span="8"><p>餐饮: ¥{{ day.costBreakdown.food || 0 }}</p></el-col>
                <el-col :span="8"><p>交通: ¥{{ day.costBreakdown.transport || 0 }}</p></el-col>
              </el-row>
              <el-row :gutter="20" v-if="day.costBreakdown.accommodation || day.costBreakdown.other">
                <el-col :span="8"><p>住宿: ¥{{ day.costBreakdown.accommodation || 0 }}</p></el-col>
                <el-col :span="8"><p>其他: ¥{{ day.costBreakdown.other || 0 }}</p></el-col>
                <el-col :span="8"><p v-if="day.totalCost" style="font-weight: bold; color: #189500">总计: ¥{{ day.totalCost }}</p></el-col>
              </el-row>
            </div>
          </el-card>
          
          <el-card v-if="data.result.transportOptions && data.result.transportOptions.length" style="margin-top: 20px">
            <template #header>
              <span style="font-size: 18px; font-weight: bold">交通优化方案对比</span>
            </template>
            <el-table :data="data.result.transportOptions" style="width: 100%">
              <el-table-column prop="name" label="方案名称"></el-table-column>
              <el-table-column prop="cost" label="总费用"></el-table-column>
              <el-table-column prop="time" label="总耗时"></el-table-column>
              <el-table-column prop="comfort" label="舒适度"></el-table-column>
              <el-table-column label="优缺点">
                <template #default="scope">
                  <div>
                    <p style="color: #67c23a"><strong>优点:</strong> {{ scope.row.pros }}</p>
                    <p style="color: #f56c6c"><strong>缺点:</strong> {{ scope.row.cons }}</p>
                  </div>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </div>

        <!-- 纯文本格式的路线规划 -->
        <div v-else>
          <el-card>
            <template #header>
              <span style="font-size: 18px; font-weight: bold">AI路线规划</span>
            </template>
            <div class="plan-content" v-html="formatContent(data.plan?.planData)"></div>
          </el-card>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, onMounted } from "vue"
import { ElMessage } from "element-plus"
import { useRouter, useRoute } from "vue-router"
import request from "@/utils/request.js"
import { Loading, Warning } from "@element-plus/icons-vue"

const router = useRouter()
const route = useRoute()

const data = reactive({
  loading: true,
  error: false,
  errorMessage: '',
  plan: null,
  isJsonFormat: false,
  result: {
    dailyPlans: [],
    transportOptions: []
  }
})

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('zh-CN')
}

const formatContent = (content) => {
  if (!content) return ''
  return content
    .replace(/\n/g, '<br/>')
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/### (.*?)(?=<br|$)/g, '<h4 style="margin-top: 15px; margin-bottom: 10px; color: #189500">$1</h4>')
    .replace(/## (.*?)(?=<br|$)/g, '<h3 style="margin-top: 20px; margin-bottom: 10px; color: #303133">$1</h3>')
    .replace(/- (.*?)(?=<br|$)/g, '<li style="margin-left: 20px; margin-bottom: 5px">$1</li>')
}

const loadPlan = async () => {
  data.loading = true
  data.error = false
  data.errorMessage = ''
  
  try {
    const planId = parseInt(route.query.id)
    if (!planId || isNaN(planId)) {
      data.error = true
      data.errorMessage = '无效的行程ID'
      return
    }
    
    const res = await request.get(`/travelPlan/selectById/${planId}`)
    if (res.code === '200' && res.data) {
      data.plan = res.data
      if (res.data.planData) {
        try {
          data.result = JSON.parse(res.data.planData)
          data.isJsonFormat = true
        } catch (parseError) {
          console.log('检测到纯文本格式的行程数据')
          data.isJsonFormat = false
          data.result = { dailyPlans: [], transportOptions: [] }
        }
      }
    } else {
      data.error = true
      data.errorMessage = res.msg || '行程表不存在'
    }
  } catch (error) {
    console.error('加载行程失败:', error)
    data.error = true
    data.errorMessage = '网络异常，请检查网络连接后重试'
  } finally {
    data.loading = false
  }
}

const goBack = () => {
  router.back()
}

const editPlan = () => {
  ElMessage.info('编辑功能开发中...')
}

onMounted(() => {
  loadPlan()
})
</script>

<style scoped>
.bg {
  height: 300px;
  background-image: url("@/assets/imgs/lxbg.jpg");
  background-size: 100% 110%;
}

.plan-card {
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.timeline-card {
  margin-bottom: 10px;
}

.plan-content {
  line-height: 1.8;
  color: #606266;
  font-size: 15px;
}

.plan-content h3,
.plan-content h4 {
  margin-top: 15px;
  margin-bottom: 10px;
}

.plan-content strong {
  color: #303133;
}

.plan-content li {
  margin-left: 20px;
}
</style>
