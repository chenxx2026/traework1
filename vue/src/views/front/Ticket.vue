<template>
  <div style="width: 60%; margin: 20px auto;">
    <div style="font-size: 28px; text-align: center; margin: 30px 0; font-weight: bold; color: #189500;">
      🚄 车票查询
    </div>

    <el-card style="margin-bottom: 20px;">
      <el-form ref="formRef" :model="data.form" label-width="100px">
        <div style="display: flex; align-items: center; justify-content: center; gap: 20px;">
          <div style="flex: 1;">
            <el-form-item label="出发地">
              <el-input v-model="data.form.fromStation" placeholder="请选择出发地" clearable></el-input>
            </el-form-item>
          </div>

          <div style="margin-top: -20px;">
            <el-button type="primary" circle @click="swapStation">
              <el-icon><Switch /></el-icon>
            </el-button>
          </div>

          <div style="flex: 1;">
            <el-form-item label="目的地">
              <el-input v-model="data.form.toStation" placeholder="请选择目的地" clearable></el-input>
            </el-form-item>
          </div>
        </div>

        <div style="display: flex; gap: 20px; margin-top: 20px;">
          <div style="flex: 1;">
            <el-form-item label="出发日期">
              <el-date-picker
                v-model="data.form.date"
                type="date"
                placeholder="请选择出发日期"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                :disabled-date="disabledDate"
                style="width: 100%;"
              />
            </el-form-item>
          </div>
          <div style="flex: 1;">
            <el-form-item label="车次类型">
              <el-select v-model="data.form.trainType" placeholder="请选择车次类型" style="width: 100%;">
                <el-option label="全部" value=""></el-option>
                <el-option label="高铁/动车" value="G/D"></el-option>
                <el-option label="普快" value="K/T/Z"></el-option>
              </el-select>
            </el-form-item>
          </div>
        </div>

        <div style="text-align: center; margin-top: 20px;">
          <el-button type="primary" size="large" style="padding: 15px 50px;" @click="searchTickets">
            <el-icon><Search /></el-icon>
            查询车票
          </el-button>
        </div>
      </el-form>
    </el-card>

    <el-card v-if="data.history.length > 0" style="margin-bottom: 20px;">
      <template #header>
        <div class="card-header">
          <span>📋 历史查询</span>
        </div>
      </template>
      <div v-for="(item, index) in data.history" :key="index" style="padding: 10px; border-bottom: 1px solid #eee; cursor: pointer;" @click="useHistory(item)">
        <span style="font-size: 16px;">{{ item.fromStation }} → {{ item.toStation }}</span>
        <span style="color: #999; margin-left: 20px;">{{ item.date }}</span>
      </div>
    </el-card>

    <el-card>
      <template #header>
        <div class="card-header">
          <span>🔥 热门路线</span>
        </div>
      </template>
      <div style="display: flex; flex-wrap: wrap; gap: 10px;">
        <el-tag v-for="(route, index) in data.hotRoutes" :key="index" size="large" style="padding: 10px 20px; cursor: pointer;" @click="selectHotRoute(route)">
          {{ route.from }} → {{ route.to }}
        </el-tag>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from "vue";
import { useRouter } from 'vue-router';
import { ElMessage } from "element-plus";
import { Search, Switch } from '@element-plus/icons-vue';

const router = useRouter();
const formRef = ref();

const data = reactive({
  form: {
    fromStation: '',
    toStation: '',
    date: '',
    trainType: ''
  },
  history: [],
  hotRoutes: [
    { from: '北京', to: '上海' },
    { from: '上海', to: '杭州' },
    { from: '广州', to: '深圳' },
    { from: '成都', to: '重庆' },
    { from: '武汉', to: '长沙' },
    { from: '南京', to: '苏州' },
    { from: '西安', to: '郑州' },
    { from: '天津', to: '北京' }
  ]
});

const disabledDate = (time) => {
  return time.getTime() < Date.now() - 8.64e7;
};

const swapStation = () => {
  const temp = data.form.fromStation;
  data.form.fromStation = data.form.toStation;
  data.form.toStation = temp;
};

const selectHotRoute = (route) => {
  data.form.fromStation = route.from;
  data.form.toStation = route.to;
  if (!data.form.date) {
    const today = new Date();
    data.form.date = today.toISOString().split('T')[0];
  }
};

const useHistory = (item) => {
  data.form.fromStation = item.fromStation;
  data.form.toStation = item.toStation;
  data.form.date = item.date;
  data.form.trainType = item.trainType || '';
};

const searchTickets = () => {
  if (!data.form.fromStation) {
    ElMessage.warning('请输入出发地');
    return;
  }
  if (!data.form.toStation) {
    ElMessage.warning('请输入目的地');
    return;
  }
  if (!data.form.date) {
    ElMessage.warning('请选择出发日期');
    return;
  }
  if (data.form.fromStation === data.form.toStation) {
    ElMessage.warning('出发地和目的地不能相同');
    return;
  }

  const historyItem = {
    fromStation: data.form.fromStation,
    toStation: data.form.toStation,
    date: data.form.date,
    trainType: data.form.trainType
  };
  data.history.unshift(historyItem);
  if (data.history.length > 5) {
    data.history.pop();
  }

  router.push({
    path: '/front/ticketResult',
    query: {
      fromStation: data.form.fromStation,
      toStation: data.form.toStation,
      date: data.form.date,
      trainType: data.form.trainType
    }
  });
};

const initData = () => {
  const today = new Date();
  data.form.date = today.toISOString().split('T')[0];
};

initData();
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 16px;
  font-weight: bold;
}
</style>
