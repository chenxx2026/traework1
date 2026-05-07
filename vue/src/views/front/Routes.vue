<template>
  <div>
    <div class="bg"></div>
    <div style="width: 60%; margin: 20px auto">
      <div style="padding-left: 10px; border-left: 5px solid #189500; font-size: 20px; margin-bottom: 20px">推荐旅游路线</div>
      <el-row :gutter="20">
       <el-col :span="8" v-for="item in data.routesList" :key="item.id" style="margin-bottom: 20px">
         <a :href="'/front/routesDetail?id=' + item.id">
           <div>
            <img :src="item.img" alt="" style="width: 100%; height: 400px">
            <div style="height: 50px; text-align: justify" class="line2 title">{{ item.name }}</div>
          </div>
         </a>
       </el-col>
      </el-row>
      <div style="margin: 10px 0" v-if="data.total">
        <el-pagination size="small" @current-change="load" background layout="total, prev, pager, next" :page-size="data.pageSize" v-model:current-page="data.pageNum" :total="data.total" />
      </div>
      
      <el-divider style="margin: 40px 0"></el-divider>
      
      <div style="padding-left: 10px; border-left: 5px solid #189500; font-size: 20px; margin-bottom: 20px">智能旅行规划</div>
      <el-card class="planner-card" @click="goToPlanner">
        <div style="text-align: center; padding: 30px">
          <el-icon :size="60" style="color: #189500"><MapLocation /></el-icon>
          <h3 style="margin: 20px 0 10px 0">AI 智能规划</h3>
          <p style="color: #909399; margin-bottom: 20px">输入您的需求，自动生成个性化行程方案</p>
          <el-button type="primary" size="large">开始规划</el-button>
        </div>
      </el-card>
    </div>
  </div>

</template>

<script setup>
import { reactive} from "vue";
import request from "@/utils/request.js";
import { MapLocation } from '@element-plus/icons-vue';
import { useRouter } from 'vue-router';

const router = useRouter();

const data = reactive({
  routesList: [],
  pageNum: 1,
  pageSize: 6,
  total: 0,
})

const load = () => {
  request.get('/routes/selectPage', {
    params: {
      pageNum: data.pageNum,
      pageSize: data.pageSize,
      name: data.name
    }
  }).then(res => {
    if (res.code === '200') {
      data.routesList = res.data?.list || []
      data.total = res.data?.total
    }
  })
}

const goToPlanner = () => {
  router.push('/front/travelPlanner')
}

load()
</script>

<style scoped>
.bg {
  height: 400px;
  background-image: url("@/assets/imgs/lxbg.jpg");
  background-size: 100% 110%;
}

.planner-card {
  cursor: pointer;
  transition: all 0.3s ease;
}

.planner-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 12px 24px 0 rgba(0, 0, 0, 0.15);
}
</style>