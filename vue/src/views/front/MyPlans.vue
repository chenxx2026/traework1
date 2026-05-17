<template>
  <div style="width: 80%; margin: 20px auto">
    <div style="padding-left: 10px; border-left: 5px solid #189500; font-size: 20px; margin-bottom: 20px">我的行程表</div>
    
    <div v-if="data.loading" style="text-align: center; padding: 100px 0">
      <el-icon :size="50" class="is-loading"><Loading /></el-icon>
      <p style="margin-top: 20px; color: #909399">正在加载...</p>
    </div>
    
    <div v-else-if="data.error" style="text-align: center; padding: 100px 0">
      <el-icon :size="50" style="color: #f56c6c"><Warning /></el-icon>
      <p style="margin-top: 20px; color: #909399">{{ data.errorMessage }}</p>
      <el-button type="primary" @click="loadPlans" style="margin-top: 20px">重试</el-button>
    </div>
    
    <div v-else-if="data.plans.length === 0" style="text-align: center; padding: 50px; color: #909399">
      <el-empty description="暂无行程表"></el-empty>
      <el-button type="primary" @click="router.push('/front/aiChat')">去规划行程</el-button>
    </div>
    
    <el-row :gutter="20" v-else>
      <el-col :span="8" v-for="item in data.plans" :key="item.id" style="margin-bottom: 20px">
        <el-card class="plan-card" shadow="hover">
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center">
              <span style="font-weight: bold">{{ item.destination }}</span>
              <el-tag v-if="weatherLoading[item.id]" type="info" size="small">天气加载中</el-tag>
            </div>
          </template>
          
          <div style="min-height: 100px">
            <p><strong>出行时间:</strong> {{ item.startDate }} 至 {{ item.endDate }}</p>
            <p><strong>创建时间:</strong> {{ formatDate(item.createTime) }}</p>
            
            <div v-if="weatherData[item.id] && weatherData[item.id].length > 0" class="weather-section">
              <el-divider content-position="left">
                <span style="font-size: 13px; color: #189500; font-weight: bold">
                  <i class="weather-title-icon">☀</i> 出行天气
                </span>
              </el-divider>
              <div class="weather-days">
                <div v-for="(w, wi) in weatherData[item.id]" :key="wi" class="weather-day-item">
                  <div class="weather-date">{{ formatShortDate(w.date) }}</div>
                  <div class="weather-icon">{{ getWeatherEmoji(w.dayWeather) }}</div>
                  <div class="weather-desc">{{ w.dayWeather || '--' }}</div>
                  <div class="weather-temp">
                    <span class="temp-high">{{ w.highTemp }}</span>
                    <span class="temp-sep">/</span>
                    <span class="temp-low">{{ w.lowTemp }}</span>
                  </div>
                  <div v-if="w.windSpeed" class="weather-wind">🌬 {{ w.windSpeed }}</div>
                </div>
              </div>
            </div>
            
            <div v-else-if="weatherData[item.id] && weatherData[item.id].length === 0 && !weatherLoading[item.id]" class="weather-section">
              <el-divider content-position="left">
                <span style="font-size: 13px; color: #909399">出行天气</span>
              </el-divider>
              <p style="font-size: 12px; color: #909399; text-align: center">暂无该目的地天气数据</p>
            </div>
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

const weatherData = reactive({})
const weatherLoading = reactive({})

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
      fetchWeatherForPlans()
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

const fetchWeatherForPlans = () => {
  data.plans.forEach(plan => {
    if (plan.destination && plan.startDate && plan.endDate) {
      fetchWeather(plan)
    }
  })
}

const fetchWeather = async (plan) => {
  weatherLoading[plan.id] = true
  try {
    const res = await request.get('/weather/query', {
      params: {
        city: plan.destination,
        startDate: plan.startDate,
        endDate: plan.endDate
      }
    })
    if (res.code === '200') {
      weatherData[plan.id] = res.data || []
    } else {
      weatherData[plan.id] = []
    }
  } catch (error) {
    console.error('获取天气失败:', plan.destination, error)
    weatherData[plan.id] = []
  } finally {
    weatherLoading[plan.id] = false
  }
}

const getWeatherEmoji = (weather) => {
  if (!weather) return '🌤'
  const w = weather.toLowerCase()
  if (w.includes('晴') || w.includes('clear') || w.includes('sunny')) return '☀️'
  if (w.includes('多云') || w.includes('partly') || w.includes('cloud')) return '⛅'
  if (w.includes('阴') || w.includes('overcast')) return '☁️'
  if (w.includes('雪') || w.includes('snow') || w.includes('sleet')) return '❄️'
  if (w.includes('雨') || w.includes('rain') || w.includes('drizzle') || w.includes('shower')) return '🌧️'
  if (w.includes('雷') || w.includes('thunder')) return '⛈️'
  if (w.includes('雾') || w.includes('fog') || w.includes('mist') || w.includes('haze')) return '🌫️'
  if (w.includes('风') || w.includes('wind')) return '💨'
  return '🌤'
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('zh-CN')
}

const formatShortDate = (dateStr) => {
  if (!dateStr) return ''
  if (dateStr.length === 10) {
    return dateStr.substring(5)
  }
  return dateStr
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

.weather-section {
  margin-top: 5px;
}

.weather-title-icon {
  font-style: normal;
  margin-right: 4px;
}

.weather-days {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
}

.weather-day-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  background: #f5f7fa;
  border-radius: 8px;
  padding: 8px 6px;
  min-width: 70px;
  transition: background 0.2s ease;
}

.weather-day-item:hover {
  background: #e8f5e9;
}

.weather-date {
  font-size: 11px;
  color: #909399;
  margin-bottom: 4px;
}

.weather-icon {
  font-size: 24px;
  margin-bottom: 2px;
  line-height: 1;
}

.weather-desc {
  font-size: 11px;
  color: #606266;
  text-align: center;
  white-space: nowrap;
}

.weather-temp {
  font-size: 11px;
  margin-top: 2px;
}

.temp-high {
  color: #e6a23c;
  font-weight: bold;
}

.temp-sep {
  color: #c0c4cc;
  margin: 0 2px;
}

.temp-low {
  color: #67c23a;
}

.weather-wind {
  font-size: 10px;
  color: #909399;
  margin-top: 2px;
}
</style>
