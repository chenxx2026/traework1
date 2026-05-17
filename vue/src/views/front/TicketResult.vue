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
          <el-tag v-if="typeLabel" size="small" :type="typeTagType">{{ typeLabel }}</el-tag>
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
      <div style="margin-top: 20px;">正在查询{{ loadingLabel }}...</div>
    </el-card>

    <el-card v-else-if="data.loadError" style="text-align: center; padding: 50px;">
      <el-icon style="font-size: 60px; color: #f56c6c;"><Warning /></el-icon>
      <div style="margin-top: 20px; font-size: 18px; color: #f56c6c;">
        {{ data.errorMsg }}
      </div>
      <div style="margin-top: 10px;">
        <el-button type="primary" @click="loadTicketData">重新查询</el-button>
        <el-button @click="goBack">返回修改</el-button>
      </div>
    </el-card>

    <div v-else>
      <div v-if="data.ticketList.length === 0" style="text-align: center; padding: 50px;">
        <el-icon style="font-size: 60px; color: #999;"><Warning /></el-icon>
        <div style="margin-top: 20px; font-size: 18px; color: #666;">
          未查询到符合条件的{{ emptyLabel }}，建议更换日期或车站试试
        </div>
      </div>

      <div v-else>
        <div style="font-size: 18px; font-weight: bold; margin-bottom: 15px;">
          为你推荐以下{{ listTitle }}
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
              <el-button type="primary" @click="goBuyTicket(ticket)">
                {{ buyBtnText }}
              </el-button>
            </div>
          </div>

          <div v-if="isTrain || (isAll && (!ticket.source || ticket.source === 'train'))" style="margin-top: 10px; padding-top: 10px; border-top: 1px solid #eee; display: flex; justify-content: space-between; color: #666; font-size: 13px;">
            <span>{{ ticket.trainNo }}</span>
            <el-tag size="small" :type="ticket.type === 'G' ? 'danger' : ticket.type === 'D' ? 'warning' : ''">{{ ticket.typeName }}</el-tag>
            <span v-if="!isAll">
              二等座 ¥{{ ticket.priceEd }}
              <span v-if="ticket.priceYd" style="margin-left: 8px;">一等座 ¥{{ ticket.priceYd }}</span>
              <span v-if="ticket.priceSw" style="margin-left: 8px;">商务座 ¥{{ ticket.priceSw }}</span>
            </span>
            <span v-else>
              ¥{{ ticket.priceEd }}
            </span>
          </div>

          <div v-if="isPlane || (isAll && ticket.source === 'plane')" style="margin-top: 10px; padding-top: 10px; border-top: 1px solid #eee; display: flex; justify-content: space-between; align-items: center; color: #666; font-size: 13px;">
            <span>{{ ticket.flightNo }}</span>
            <el-tag size="small" type="warning">{{ ticket.airline }}</el-tag>
            <span>机型：{{ ticket.aircraftType }}</span>
            <span>准点率：{{ ticket.onTimeRate }}</span>
          </div>

          <div v-if="isBus || (isAll && ticket.source === 'bus')" style="margin-top: 10px; padding-top: 10px; border-top: 1px solid #eee; display: flex; justify-content: space-between; align-items: center; color: #666; font-size: 13px;">
            <span>{{ ticket.busNo }}</span>
            <el-tag size="small" type="success">{{ ticket.company }}</el-tag>
            <span>车型：{{ ticket.busType }}</span>
            <span>余座：{{ ticket.seatCount }}座</span>
          </div>
        </el-card>

        <div style="margin-top: 30px; padding: 20px; background: #f5f7fa; border-radius: 5px; text-align: center; color: #909399; font-size: 14px;">
          <el-icon style="margin-right: 5px;"><InfoFilled /></el-icon>
          {{ footerNotice }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, onMounted } from "vue";
import { useRouter, useRoute } from 'vue-router';
import { ElMessage, ElMessageBox } from "element-plus";
import { ArrowLeft, Right, Loading, Warning, InfoFilled } from '@element-plus/icons-vue';
import request from "@/utils/request";

const router = useRouter();
const route = useRoute();

const data = reactive({
  loading: true,
  loadError: false,
  errorMsg: '',
  query: {
    fromStation: '',
    toStation: '',
    date: '',
    trainType: ''
  },
  sortType: '',
  ticketList: []
});

const isTrain = computed(() => !data.query.trainType || ['G/D', 'K/T/Z', 'ALL'].includes(data.query.trainType))
const isPlane = computed(() => data.query.trainType === 'PLANE')
const isBus = computed(() => data.query.trainType === 'BUS')
const isAll = computed(() => data.query.trainType === 'ALL')

const loadingLabel = computed(() => {
  if (isPlane.value) return '航班信息'
  if (isBus.value) return '汽车票信息'
  if (isAll.value) return '票务信息'
  return '车票信息'
})

const emptyLabel = computed(() => {
  if (isPlane.value) return '航班'
  if (isBus.value) return '汽车班次'
  return '车次'
})

const listTitle = computed(() => {
  if (isPlane.value) return '航班'
  if (isBus.value) return '汽车班次'
  return '车次'
})

const typeLabel = computed(() => {
  if (isPlane.value) return '✈️ 飞机'
  if (isBus.value) return '🚌 汽车'
  if (isAll.value) return '全部'
  return ''
})

const typeTagType = computed(() => {
  if (isPlane.value) return 'warning'
  if (isBus.value) return 'success'
  return ''
})

const buyBtnText = computed(() => {
  if (isPlane.value) return '前往购票'
  if (isBus.value) return '客运购票'
  if (isAll.value) return '前往购票'
  return '去12306购买'
})

const footerNotice = computed(() => {
  if (isPlane.value) return '航班信息为AI生成参考数据，实际班次、票价及余票以航空公司官方平台为准'
  if (isBus.value) return '汽车票信息为AI生成参考数据，实际班次、票价及余票以客运站官方平台为准'
  if (isAll.value) return '火车票为铁路12306实时数据，航班及汽车票为AI生成参考数据，请以各官方渠道为准'
  return '车票信息仅供参考，实际购票及余票情况以铁路12306官方平台为准'
})

const goBack = () => {
  router.back();
};

const goBuyTicket = (ticket) => {
  if (isAll.value) {
    const source = ticket.source || 'train'
    if (source === 'train') {
      ElMessageBox.confirm('即将前往铁路12306官方平台购票，购票及支付将在12306完成', '提示', {
        confirmButtonText: '确定', cancelButtonText: '取消', type: 'info'
      }).then(() => { window.open('https://www.12306.cn/index/', '_blank'); }).catch(() => {});
    } else if (source === 'plane') {
      ElMessage.info('请前往航空公司官网或携程、飞猪等OTA平台购票')
    } else {
      ElMessage.info('请前往当地客运站或客运联网售票平台购票')
    }
  } else if (isTrain.value) {
    ElMessageBox.confirm(
      '即将前往铁路12306官方平台购票，购票及支付将在12306完成',
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      }
    ).then(() => {
      window.open('https://www.12306.cn/index/', '_blank');
    }).catch(() => {});
  } else if (isPlane.value) {
    ElMessage.info('请前往航空公司官网或携程、飞猪等OTA平台购票')
  } else {
    ElMessage.info('请前往当地客运站或客运联网售票平台购票')
  }
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
    data.ticketList.sort((a, b) => (a.price || 0) - (b.price || 0));
  } else if (type === 'start') {
    data.ticketList.sort((a, b) => (a.startTime || '').localeCompare(b.startTime || ''));
  }
  addRecommendTags();
};

const parseDuration = (duration) => {
  if (!duration) return 0;
  const match = String(duration).match(/(\d+)小时(\d+)分/);
  if (match) {
    return parseInt(match[1]) * 60 + parseInt(match[2]);
  }
  const hourMatch = String(duration).match(/(\d+):(\d+)/);
  if (hourMatch) {
    return parseInt(hourMatch[1]) * 60 + parseInt(hourMatch[2]);
  }
  return 0;
};

const addRecommendTags = () => {
  data.ticketList.forEach(ticket => {
    ticket.tags = [];
  });

  if (data.ticketList.length > 0) {
    const durations = data.ticketList.map(t => parseDuration(t.duration)).filter(d => d > 0);
    const prices = data.ticketList.map(t => t.price || 0).filter(p => p > 0);
    const startTimes = data.ticketList.map(t => t.startTime || '').filter(s => s);

    if (durations.length > 0) {
      const minDuration = Math.min(...durations);
      data.ticketList.forEach(ticket => {
        if (parseDuration(ticket.duration) === minDuration) {
          ticket.tags.push('最快');
        }
      });
    }

    if (prices.length > 0) {
      const minPrice = Math.min(...prices);
      data.ticketList.forEach(ticket => {
        if ((ticket.price || 0) === minPrice) {
          ticket.tags.push('最便宜');
        }
      });
    }

    if (startTimes.length > 0) {
      const earliestTime = startTimes.reduce((earliest, t) => t < earliest ? t : earliest, startTimes[0]);
      data.ticketList.forEach(ticket => {
        if (ticket.startTime === earliestTime) {
          ticket.tags.push('最早出发');
        }
      });
    }
  }
};

const normalizeFlightItem = (item) => {
  return {
    flightNo: item.flightNo || '',
    airline: item.airline || '',
    fromStation: item.fromAirport || '',
    toStation: item.toAirport || '',
    startTime: item.departureTime || '',
    endTime: item.arrivalTime || '',
    duration: item.duration || '',
    price: item.price || 0,
    aircraftType: item.aircraftType || '',
    onTimeRate: item.onTimeRate || '',
    tags: []
  };
};

const normalizeBusItem = (item) => {
  return {
    busNo: item.busNo || '',
    company: item.company || '',
    fromStation: item.fromStation || '',
    toStation: item.toStation || '',
    startTime: item.departureTime || '',
    endTime: item.arrivalTime || '',
    duration: item.duration || '',
    price: item.price || 0,
    busType: item.busType || '',
    seatCount: item.seatCount || 0,
    tags: []
  };
};

const normalizeTrainItem = (item) => {
  return {
    trainNo: item.trainno || item.train_no || '',
    typeName: item.typename || '',
    type: item.type || '',
    fromStation: item.station || item.from_station || '',
    toStation: item.endstation || item.to_station || '',
    startTime: item.departuretime || item.starttime || '',
    endTime: item.arrivaltime || item.endtime || '',
    duration: item.costtime || item.duration || '',
    price: item.priceed || item.price || 0,
    priceEd: item.priceed || 0,
    priceYd: item.priceyd || 0,
    priceSw: item.pricesw || 0,
    priceWz: item.pricewz || 0,
    priceYxyd: item.priceyxyd || 0,
    seatType: item.typename || '',
    remain: 0,
    isend: item.isend || 0,
    source: item.source || 'train',
    tags: []
  };
};

const normalizeAllItem = (item) => {
  const source = item.source || 'train'
  if (source === 'plane') {
    const plane = normalizeFlightItem(item)
    plane.source = 'plane'
    return plane
  }
  if (source === 'bus') {
    const bus = normalizeBusItem(item)
    bus.source = 'bus'
    return bus
  }
  const train = normalizeTrainItem(item)
  train.source = 'train'
  return train
};

const loadTicketData = async () => {
  data.loading = true;
  data.loadError = false;
  data.errorMsg = '';

  data.query = {
    fromStation: route.query.fromStation || '',
    toStation: route.query.toStation || '',
    date: route.query.date || '',
    trainType: route.query.trainType || ''
  };

  try {
    const res = await request.get('/ticket/query', {
      params: {
        fromStation: data.query.fromStation,
        toStation: data.query.toStation,
        date: data.query.date,
        trainType: data.query.trainType || ''
      }
    });

    if (res.code === '200' && res.data) {
      let ticketList = [];

      if (Array.isArray(res.data)) {
        ticketList = res.data;
      } else if (res.data.list && Array.isArray(res.data.list)) {
        ticketList = res.data.list;
      } else if (res.data.result && Array.isArray(res.data.result)) {
        ticketList = res.data.result;
      } else if (res.data.trainList && Array.isArray(res.data.trainList)) {
        ticketList = res.data.trainList;
      }

      if (ticketList.length > 0) {
        if (isAll.value) {
          data.ticketList = ticketList.map(normalizeAllItem);
        } else if (isPlane.value) {
          data.ticketList = ticketList.map(normalizeFlightItem);
        } else if (isBus.value) {
          data.ticketList = ticketList.map(normalizeBusItem);
        } else {
          data.ticketList = ticketList.map(normalizeTrainItem);
        }
        addRecommendTags();
        data.loading = false;
        return;
      }
    }

    if (res.code !== '200') {
      data.loadError = true;
      data.errorMsg = res.msg || '查询失败，请稍后再试';
    } else {
      data.ticketList = [];
    }
  } catch (e) {
    console.error('查询失败:', e);
    data.loadError = true;
    if (e.response && e.response.status === 500) {
      data.errorMsg = '系统异常，请稍后再试';
    } else if (e.code === 'ECONNABORTED' || e.message.includes('timeout')) {
      data.errorMsg = '查询超时，请稍后再试';
    } else {
      data.errorMsg = '网络连接失败，请检查网络';
    }
  } finally {
    data.loading = false;
  }
};

onMounted(() => {
  loadTicketData();
});
</script>

<style scoped>
</style>
