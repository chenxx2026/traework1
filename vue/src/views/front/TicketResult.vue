<template>
  <div style="width: 70%; margin: 20px auto;">
    <div style="margin-bottom: 20px;">
      <el-button type="primary" plain @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
        返回查询
      </el-button>
    </div>

    <el-card style="margin-bottom: 20px;">
      <div style="display: flex; align-items: center; justify-content: space-between; font-size: 18px;">
        <div style="display: flex; align-items: center; gap: 10px;">
          <span style="font-weight: bold; color: #189500;">{{ data.query.fromStation }}</span>
          <el-icon><Right /></el-icon>
          <span style="font-weight: bold; color: #189500;">{{ data.query.toStation }}</span>
          <span style="color: #999; margin-left: 10px;">{{ data.query.date }}</span>
        </div>
        <div style="display: flex; gap: 10px;">
          <el-button-group>
            <el-button :type="data.sortType === 'time' ? 'primary' : ''" @click="sortBy('time')">用时优先</el-button>
            <el-button :type="data.sortType === 'price' ? 'primary' : ''" @click="sortBy('price')">价格优先</el-button>
            <el-button :type="data.sortType === 'start' ? 'primary' : ''" @click="sortBy('start')">最早出发</el-button>
          </el-button-group>
        </div>
      </div>
    </el-card>

    <el-card v-if="data.loading" style="text-align: center; padding: 50px;">
      <el-icon class="is-loading" style="font-size: 40px;"><Loading /></el-icon>
      <div style="margin-top: 20px;">正在查询车票信息...</div>
    </el-card>

    <div v-else>
      <div v-if="data.ticketList.length === 0" style="text-align: center; padding: 50px;">
        <el-icon style="font-size: 60px; color: #999;"><Warning /></el-icon>
        <div style="margin-top: 20px; font-size: 18px; color: #666;">
          未查询到符合条件的车次，建议更换日期或车站试试
        </div>
      </div>

      <div v-else>
        <div style="font-size: 18px; font-weight: bold; margin-bottom: 15px;">
          为你推荐以下车次
        </div>

        <el-card v-for="(ticket, index) in data.ticketList" :key="index" style="margin-bottom: 15px;">
          <div style="display: flex; align-items: center; justify-content: space-between;">
            <div style="flex: 2; display: flex; align-items: center;">
              <div style="flex: 1; text-align: center;">
                <div style="font-size: 24px; font-weight: bold; color: #189500;">{{ ticket.startTime }}</div>
                <div style="color: #666; margin-top: 5px;">{{ ticket.fromStation }}</div>
              </div>
              <div style="flex: 1; text-align: center; position: relative;">
                <div style="color: #999; font-size: 14px;">{{ ticket.duration }}</div>
                <div style="position: absolute; top: 50%; left: 10%; right: 10%; border-top: 2px dashed #ddd;"></div>
                <el-icon style="position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); background: #fff; padding: 0 5px; color: #999;"><Right /></el-icon>
              </div>
              <div style="flex: 1; text-align: center;">
                <div style="font-size: 24px; font-weight: bold; color: #189500;">{{ ticket.endTime }}</div>
                <div style="color: #666; margin-top: 5px;">{{ ticket.toStation }}</div>
              </div>
            </div>
            <div style="flex: 1; display: flex; align-items: center; justify-content: center; gap: 10px;">
              <div style="font-size: 20px; font-weight: bold; color: #e6a23c;">
                ¥{{ ticket.price }}
              </div>
            </div>
            <div style="flex: 1; display: flex; flex-direction: column; align-items: flex-end; gap: 10px;">
              <div style="display: flex; gap: 5px;">
                <el-tag v-if="ticket.tags.includes('最快')" type="danger" size="small">最快</el-tag>
                <el-tag v-if="ticket.tags.includes('最便宜')" type="success" size="small">最便宜</el-tag>
                <el-tag v-if="ticket.tags.includes('最早出发')" type="warning" size="small">最早出发</el-tag>
              </div>
              <el-button type="primary" @click="goTo12306(ticket)">
                去12306购买
              </el-button>
            </div>
          </div>
          <div style="margin-top: 10px; padding-top: 10px; border-top: 1px solid #eee; display: flex; justify-content: space-between; color: #666;">
            <span>{{ ticket.trainNo }}</span>
            <span>{{ ticket.seatType }}</span>
            <span v-if="ticket.remain > 0" style="color: #67c23a;">余票 {{ ticket.remain }} 张</span>
            <span v-else style="color: #f56c6c;">暂无余票</span>
          </div>
        </el-card>

        <div style="margin-top: 30px; padding: 20px; background: #f5f7fa; border-radius: 5px; text-align: center; color: #909399; font-size: 14px;">
          <el-icon style="margin-right: 5px;"><InfoFilled /></el-icon>
          车票信息仅供参考，实际购票及余票情况以铁路12306官方平台为准
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from "vue";
import { useRouter, useRoute } from 'vue-router';
import { ElMessage, ElMessageBox } from "element-plus";
import { ArrowLeft, Right, Loading, Warning, InfoFilled } from '@element-plus/icons-vue';

const router = useRouter();
const route = useRoute();

const data = reactive({
  loading: true,
  query: {
    fromStation: '',
    toStation: '',
    date: '',
    trainType: ''
  },
  sortType: '',
  ticketList: []
});

const mockTicketData = [
  {
    trainNo: 'G1',
    fromStation: '北京南',
    toStation: '上海虹桥',
    startTime: '07:00',
    endTime: '11:36',
    duration: '4小时36分',
    price: 553,
    seatType: '二等座',
    remain: 20,
    tags: []
  },
  {
    trainNo: 'G5',
    fromStation: '北京南',
    toStation: '上海虹桥',
    startTime: '08:00',
    endTime: '12:28',
    duration: '4小时28分',
    price: 553,
    seatType: '二等座',
    remain: 15,
    tags: []
  },
  {
    trainNo: 'D313',
    fromStation: '北京南',
    toStation: '上海',
    startTime: '19:35',
    endTime: '07:32',
    duration: '11小时57分',
    price: 309,
    seatType: '二等座',
    remain: 5,
    tags: []
  },
  {
    trainNo: 'G101',
    fromStation: '北京南',
    toStation: '上海虹桥',
    startTime: '06:36',
    endTime: '12:40',
    duration: '6小时4分',
    price: 553,
    seatType: '二等座',
    remain: 10,
    tags: []
  },
  {
    trainNo: 'G3',
    fromStation: '北京南',
    toStation: '上海虹桥',
    startTime: '14:00',
    endTime: '18:36',
    duration: '4小时36分',
    price: 498,
    seatType: '二等座',
    remain: 30,
    tags: []
  }
];

const goBack = () => {
  router.back();
};

const sortBy = (type) => {
  data.sortType = type;
  if (type === 'time') {
    data.ticketList.sort((a, b) => {
      const aDuration = parseDuration(a.duration);
      const bDuration = parseDuration(b.duration);
      return aDuration - bDuration;
    });
  } else if (type === 'price') {
    data.ticketList.sort((a, b) => a.price - b.price);
  } else if (type === 'start') {
    data.ticketList.sort((a, b) => a.startTime.localeCompare(b.startTime));
  }
  addRecommendTags();
};

const parseDuration = (duration) => {
  const match = duration.match(/(\d+)小时(\d+)分/);
  if (match) {
    return parseInt(match[1]) * 60 + parseInt(match[2]);
  }
  return 0;
};

const addRecommendTags = () => {
  data.ticketList.forEach(ticket => {
    ticket.tags = [];
  });

  if (data.ticketList.length > 0) {
    const minDuration = Math.min(...data.ticketList.map(t => parseDuration(t.duration)));
    const minPrice = Math.min(...data.ticketList.map(t => t.price));
    const earliestTime = data.ticketList.reduce((earliest, ticket) => {
      return ticket.startTime < earliest ? ticket.startTime : earliest;
    }, data.ticketList[0].startTime);

    data.ticketList.forEach(ticket => {
      if (parseDuration(ticket.duration) === minDuration) {
        ticket.tags.push('最快');
      }
      if (ticket.price === minPrice) {
        ticket.tags.push('最便宜');
      }
      if (ticket.startTime === earliestTime) {
        ticket.tags.push('最早出发');
      }
    });
  }
};

const goTo12306 = (ticket) => {
  ElMessageBox.confirm(
    '即将前往铁路12306官方平台购票，购票及支付将在12306完成',
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info'
    }
  ).then(() => {
    const url = `https://www.12306.cn/index/`;
    window.open(url, '_blank');
  }).catch(() => {
  });
};

const loadTicketData = () => {
  data.loading = true;
  setTimeout(() => {
    data.query = {
      fromStation: route.query.fromStation || '',
      toStation: route.query.toStation || '',
      date: route.query.date || '',
      trainType: route.query.trainType || ''
    };

    data.ticketList = mockTicketData.map(ticket => ({
      ...ticket,
      fromStation: data.query.fromStation + (ticket.fromStation.includes('南') || ticket.fromStation.includes('东') ? '' : '站'),
      toStation: data.query.toStation + (ticket.toStation.includes('南') || ticket.toStation.includes('东') ? '' : '站')
    }));

    addRecommendTags();
    data.loading = false;
  }, 1000);
};

onMounted(() => {
  loadTicketData();
});
</script>

<style scoped>
</style>
