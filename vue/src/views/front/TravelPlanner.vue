<template>
  <div>
    <div class="bg"></div>
    <div style="width: 80%; margin: 20px auto">
      <div style="padding-left: 10px; border-left: 5px solid #189500; font-size: 20px; margin-bottom: 20px">智能旅行规划</div>
      
      <el-card v-if="!data.showResult" class="form-card">
        <el-form :model="data.form" label-width="120px">
          <el-form-item label="目的地">
            <el-select v-model="data.form.destination" placeholder="请选择或输入目的地" filterable allow-create style="width: 100%">
              <el-option label="北京" value="北京"></el-option>
              <el-option label="上海" value="上海"></el-option>
              <el-option label="杭州" value="杭州"></el-option>
              <el-option label="成都" value="成都"></el-option>
              <el-option label="西安" value="西安"></el-option>
              <el-option label="广州" value="广州"></el-option>
              <el-option label="深圳" value="深圳"></el-option>
              <el-option label="南京" value="南京"></el-option>
              <el-option label="苏州" value="苏州"></el-option>
              <el-option label="厦门" value="厦门"></el-option>
            </el-select>
          </el-form-item>
          
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="开始日期">
                <el-date-picker v-model="data.form.startDate" type="date" placeholder="选择开始日期" style="width: 100%"></el-date-picker>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="结束日期">
                <el-date-picker v-model="data.form.endDate" type="date" placeholder="选择结束日期" style="width: 100%"></el-date-picker>
              </el-form-item>
            </el-col>
          </el-row>
          
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="总预算(元)">
                <el-input-number v-model="data.form.totalBudget" :min="0" placeholder="可选" style="width: 100%"></el-input-number>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="每日预算(元)">
                <el-input-number v-model="data.form.dailyBudget" :min="0" placeholder="可选" style="width: 100%"></el-input-number>
              </el-form-item>
            </el-col>
          </el-row>
          
          <el-form-item label="交通方式">
            <el-checkbox-group v-model="data.form.transportPreferences">
              <el-checkbox label="飞机">飞机</el-checkbox>
              <el-checkbox label="火车">火车</el-checkbox>
              <el-checkbox label="公交">公交</el-checkbox>
              <el-checkbox label="打车">打车</el-checkbox>
              <el-checkbox label="自驾">自驾</el-checkbox>
            </el-checkbox-group>
          </el-form-item>
          
          <el-form-item label="住宿类型">
            <el-select v-model="data.form.accommodation" placeholder="请选择" style="width: 100%">
              <el-option label="经济型酒店" value="经济型酒店"></el-option>
              <el-option label="舒适型酒店" value="舒适型酒店"></el-option>
              <el-option label="高端酒店" value="高端酒店"></el-option>
              <el-option label="民宿" value="民宿"></el-option>
              <el-option label="青年旅舍" value="青年旅舍"></el-option>
            </el-select>
          </el-form-item>
          
          <el-form-item label="景点类型">
            <el-checkbox-group v-model="data.form.scenicTypes">
              <el-checkbox label="自然风光">自然风光</el-checkbox>
              <el-checkbox label="历史文化">历史文化</el-checkbox>
              <el-checkbox label="主题乐园">主题乐园</el-checkbox>
              <el-checkbox label="美食购物">美食购物</el-checkbox>
              <el-checkbox label="休闲度假">休闲度假</el-checkbox>
            </el-checkbox-group>
          </el-form-item>
          
          <el-form-item label="餐饮风格">
            <el-checkbox-group v-model="data.form.foodStyles">
              <el-checkbox label="当地特色">当地特色</el-checkbox>
              <el-checkbox label="中餐">中餐</el-checkbox>
              <el-checkbox label="西餐">西餐</el-checkbox>
              <el-checkbox label="快餐">快餐</el-checkbox>
              <el-checkbox label="素食">素食</el-checkbox>
            </el-checkbox-group>
          </el-form-item>
          
          <el-form-item label="可调整节点">
            <el-input v-model="data.form.flexibleNotes" type="textarea" :rows="3" placeholder="如有需要特别说明的可灵活调整的行程部分，请在此说明"></el-input>
          </el-form-item>
          
          <div style="text-align: center">
            <el-button type="primary" @click="generatePlan" :loading="data.loading">生成行程方案</el-button>
            <el-button @click="resetForm">重置</el-button>
          </div>
        </el-form>
      </el-card>
      
      <div v-if="data.showResult">
        <div style="display: flex; justify-content: space-between; margin-bottom: 20px">
          <h3>为您生成的行程方案</h3>
          <div>
            <el-button type="primary" @click="savePlan">保存到个人中心</el-button>
            <el-button @click="backToForm">重新规划</el-button>
          </div>
        </div>
        
        <el-card v-for="(day, dayIndex) in data.result.dailyPlans" :key="dayIndex" class="plan-card" style="margin-bottom: 20px">
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center">
              <span style="font-size: 18px; font-weight: bold">第 {{ dayIndex + 1 }} 天 - {{ day.date }}</span>
              <el-tag type="success">当日预算: ¥{{ day.totalCost }}</el-tag>
            </div>
          </template>
          
          <el-timeline>
            <el-timeline-item v-for="(node, nodeIndex) in day.nodes" :key="nodeIndex" :timestamp="node.time" placement="top">
              <el-card class="timeline-card">
                <div style="display: flex; justify-content: space-between; margin-bottom: 10px">
                  <h4 style="margin: 0">{{ node.name }}</h4>
                  <el-tag v-if="node.type === 'scenic'" type="danger">景点</el-tag>
                  <el-tag v-else-if="node.type === 'food'" type="success">餐饮</el-tag>
                  <el-tag v-else-if="node.type === 'transport'" type="info">交通</el-tag>
                  <el-tag v-else-if="node.type === 'hotel'" type="warning">住宿</el-tag>
                  <el-tag v-else-if="node.type === 'shopping'" type="primary">购物</el-tag>
                </div>
                
                <div v-if="node.type === 'scenic'" style="font-size: 14px; color: #666">
                  <p><strong>地址:</strong> {{ node.address }}</p>
                  <p><strong>开放时间:</strong> {{ node.openTime }}</p>
                  <p><strong>票价:</strong> ¥{{ node.ticketPrice }}</p>
                  <p><strong>游玩时长:</strong> {{ node.duration }}</p>
                  <p><strong>游玩建议:</strong> {{ node.suggestion }}</p>
                </div>
                
                <div v-if="node.type === 'food'" style="font-size: 14px; color: #666">
                  <p><strong>地址:</strong> {{ node.address }}</p>
                  <p><strong>特色菜品:</strong> {{ node.specialties }}</p>
                  <p><strong>人均消费:</strong> ¥{{ node.avgCost }}</p>
                </div>
                
                <div v-if="node.transport">
                  <el-divider content-position="left">前往下一站</el-divider>
                  <p><strong>交通方式:</strong> {{ node.transport.type }}</p>
                  <p><strong>路线:</strong> {{ node.transport.route }}</p>
                  <p><strong>预计时间:</strong> {{ node.transport.time }}</p>
                  <p><strong>费用:</strong> ¥{{ node.transport.cost }}</p>
                  <p v-if="node.transport.tips" style="color: #909399"><strong>贴士:</strong> {{ node.transport.tips }}</p>
                </div>
                
                <div v-if="node.cost" style="margin-top: 10px; color: #f56c6c; font-weight: bold">
                  费用: ¥{{ node.cost }}
                </div>
              </el-card>
            </el-timeline-item>
          </el-timeline>
          
          <el-divider></el-divider>
          
          <div style="background: #f5f7fa; padding: 15px; border-radius: 4px">
            <h5 style="margin-top: 0">当日费用明细</h5>
            <el-row :gutter="20">
              <el-col :span="8"><p>景点门票: ¥{{ day.costBreakdown.tickets }}</p></el-col>
              <el-col :span="8"><p>餐饮: ¥{{ day.costBreakdown.food }}</p></el-col>
              <el-col :span="8"><p>交通: ¥{{ day.costBreakdown.transport }}</p></el-col>
            </el-row>
            <el-row :gutter="20">
              <el-col :span="8"><p>住宿: ¥{{ day.costBreakdown.accommodation }}</p></el-col>
              <el-col :span="8"><p>其他: ¥{{ day.costBreakdown.other }}</p></el-col>
              <el-col :span="8"><p style="font-weight: bold; color: #189500">总计: ¥{{ day.totalCost }}</p></el-col>
            </el-row>
          </div>
        </el-card>
        
        <el-card style="margin-top: 20px">
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
        
        <div style="margin-top: 20px; text-align: center; color: #909399">
          <p>行程信息仅供参考，请根据实际情况调整</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive } from "vue"
import { ElMessage } from "element-plus"
import request from "@/utils/request.js"
import { useRouter } from "vue-router"

const router = useRouter()

const data = reactive({
  showResult: false,
  loading: false,
  form: {
    destination: '',
    startDate: '',
    endDate: '',
    totalBudget: null,
    dailyBudget: null,
    transportPreferences: [],
    accommodation: '',
    scenicTypes: [],
    foodStyles: [],
    flexibleNotes: ''
  },
  result: {
    dailyPlans: [],
    transportOptions: []
  }
})

const destinationData = {
  北京: {
    scenicSpots: [
      { name: '故宫博物院', address: '北京市东城区景山前街4号', openTime: '08:30 - 17:00（周一闭馆）', ticketPrice: 60, duration: '4小时', suggestion: '建议提前网上预约，避开节假日人流高峰，从午门进神武门出', type: '历史文化' },
      { name: '天安门广场', address: '北京市东城区天安门', openTime: '全天开放', ticketPrice: 0, duration: '1.5小时', suggestion: '升国旗仪式建议提前1小时到场占位', type: '历史文化' },
      { name: '八达岭长城', address: '北京市延庆区八达岭镇八达岭长城', openTime: '06:30 - 19:00', ticketPrice: 40, duration: '5小时', suggestion: '建议穿舒适运动鞋，可乘坐缆车省力，避开周末高峰期', type: '历史文化' },
      { name: '颐和园', address: '北京市海淀区新建宫门路19号', openTime: '06:30 - 18:00', ticketPrice: 30, duration: '3.5小时', suggestion: '建议从东宫门进，游昆明湖登佛香阁，春季最佳', type: '自然风光' },
      { name: '天坛公园', address: '北京市东城区天坛路甲1号', openTime: '06:00 - 22:00', ticketPrice: 15, duration: '2.5小时', suggestion: '清晨可看晨练，上午祈年殿和回音壁人较少', type: '历史文化' },
      { name: '恭王府', address: '北京市西城区前海西街17号', openTime: '08:30 - 17:00（周一闭馆）', ticketPrice: 40, duration: '2小时', suggestion: '可了解清代王府生活，参观福字碑', type: '历史文化' },
      { name: '景山公园', address: '北京市西城区景山西街44号', openTime: '06:30 - 21:00', ticketPrice: 2, duration: '1小时', suggestion: '万春亭是俯瞰故宫全景的最佳位置', type: '自然风光' },
      { name: '南锣鼓巷', address: '北京市东城区南锣鼓巷', openTime: '全天开放', ticketPrice: 0, duration: '2小时', suggestion: '可逛胡同、品小吃，晚上灯光更美', type: '美食购物' }
    ],
    foods: [
      { name: '四季民福烤鸭店（故宫店）', address: '北京市东城区南池子大街11号', specialties: '北京烤鸭、贝勒烤肉、鸭架汤', avgCost: 150 },
      { name: '护国寺小吃', address: '北京市西城区护国寺大街93号', specialties: '豆汁儿、焦圈、艾窝窝、豌豆黄', avgCost: 50 },
      { name: '东来顺饭庄', address: '北京市东城区王府井大街138号', specialties: '涮羊肉、手切鲜羊肉', avgCost: 180 },
      { name: '庆丰包子铺', address: '北京市西城区西单北大街', specialties: '猪肉大葱包子、炒肝', avgCost: 30 }
    ],
    hotels: [
      { name: '北京王府井希尔顿酒店', type: '高端酒店', address: '北京市东城区王府井东街8号' },
      { name: '全季酒店（北京天安门广场店）', type: '舒适型酒店', address: '北京市东城区前门东大街23号' },
      { name: '如家酒店（北京王府井店）', type: '经济型酒店', address: '北京市东城区王府井大街241号' }
    ]
  },
  上海: {
    scenicSpots: [
      { name: '外滩', address: '上海市黄浦区中山东一路', openTime: '全天开放', ticketPrice: 0, duration: '2小时', suggestion: '夜景最美，建议傍晚前往，可同时看陆家嘴灯光', type: '美食购物' },
      { name: '上海迪士尼度假区', address: '上海市浦东新区黄赵路310号', openTime: '09:00 - 21:00', ticketPrice: 475, duration: '10小时', suggestion: '建议提前下载APP预约快速通行证，工作日人流量较少', type: '主题乐园' },
      { name: '东方明珠广播电视塔', address: '上海市浦东新区世纪大道1号', openTime: '08:00 - 21:30', ticketPrice: 220, duration: '2小时', suggestion: '351米太空舱和透明观光层体验最佳', type: '休闲度假' },
      { name: '豫园', address: '上海市黄浦区安仁街132号', openTime: '09:00 - 17:00', ticketPrice: 40, duration: '2小时', suggestion: '可同时逛城隍庙吃小吃，元宵节灯会最美', type: '历史文化' },
      { name: '南京路步行街', address: '上海市黄浦区南京东路', openTime: '全天开放', ticketPrice: 0, duration: '2小时', suggestion: '购物天堂，可乘铛铛车游览', type: '美食购物' },
      { name: '田子坊', address: '上海市黄浦区泰康路210弄', openTime: '全天开放', ticketPrice: 0, duration: '1.5小时', suggestion: '艺术创意街区，适合拍照和淘小店', type: '美食购物' },
      { name: '上海博物馆', address: '上海市黄浦区人民大道201号', openTime: '09:00 - 17:00（周一闭馆）', ticketPrice: 0, duration: '3小时', suggestion: '免费参观，青铜器和陶瓷馆必看', type: '历史文化' }
    ],
    foods: [
      { name: '南翔馒头店（豫园店）', address: '上海市黄浦区豫园路85号', specialties: '南翔小笼包、蟹粉小笼', avgCost: 80 },
      { name: '小杨生煎（南京东路店）', address: '上海市黄浦区南京东路720号', specialties: '鲜肉生煎、牛肉汤', avgCost: 30 },
      { name: '老正兴菜馆', address: '上海市黄浦区福州路556号', specialties: '红烧肉、糖醋排骨、八宝鸭', avgCost: 150 },
      { name: '光明邨大酒家', address: '上海市徐汇区淮海中路588号', specialties: '酱鸭、鲜肉月饼', avgCost: 60 }
    ],
    hotels: [
      { name: '上海外滩华尔道夫酒店', type: '高端酒店', address: '上海市黄浦区中山东一路2号' },
      { name: '全季酒店（上海南京东路店）', type: '舒适型酒店', address: '上海市黄浦区南京东路450号' },
      { name: '如家酒店（上海外滩店）', type: '经济型酒店', address: '上海市黄浦区四川中路633号' }
    ]
  },
  杭州: {
    scenicSpots: [
      { name: '西湖风景名胜区', address: '浙江省杭州市西湖区龙井路1号', openTime: '全天开放', ticketPrice: 0, duration: '5小时', suggestion: '建议租自行车环湖，或乘坐游船，苏堤春晓和断桥残雪必看', type: '自然风光' },
      { name: '灵隐寺', address: '浙江省杭州市西湖区法云弄1号', openTime: '07:00 - 18:15', ticketPrice: 45, duration: '2.5小时', suggestion: '飞来峰造像值得一看，建议早上去人少清静', type: '历史文化' },
      { name: '断桥残雪', address: '浙江省杭州市西湖区北山街', openTime: '全天开放', ticketPrice: 0, duration: '30分钟', suggestion: '西湖十景之一，冬季雪景最美', type: '自然风光' },
      { name: '雷峰塔', address: '浙江省杭州市西湖区南山路15号', openTime: '08:00 - 20:00', ticketPrice: 40, duration: '1.5小时', suggestion: '可登塔俯瞰西湖全景，傍晚看日落最佳', type: '历史文化' },
      { name: '西溪湿地', address: '浙江省杭州市西湖区天目山路518号', openTime: '08:00 - 17:30', ticketPrice: 80, duration: '3.5小时', suggestion: '自然生态湿地，建议乘船游览，秋季芦花最美', type: '自然风光' },
      { name: '宋城', address: '浙江省杭州市西湖区之江路148号', openTime: '09:00 - 21:00', ticketPrice: 310, duration: '5小时', suggestion: '《宋城千古情》演出必看，建议提前选座', type: '主题乐园' },
      { name: '河坊街', address: '浙江省杭州市上城区河坊街', openTime: '全天开放', ticketPrice: 0, duration: '2小时', suggestion: '仿古步行街，可品杭帮菜、买特产', type: '美食购物' }
    ],
    foods: [
      { name: '楼外楼（孤山路店）', address: '浙江省杭州市西湖区孤山路30号', specialties: '西湖醋鱼、东坡肉、龙井虾仁', avgCost: 200 },
      { name: '知味观（湖滨店）', address: '浙江省杭州市上城区仁和路83号', specialties: '片儿川、猫耳朵、小笼包', avgCost: 60 },
      { name: '外婆家（湖滨店）', address: '浙江省杭州市上城区平海路124号', specialties: '茶香鸡、东坡肉、西湖醋鱼', avgCost: 80 },
      { name: '新丰小吃', address: '浙江省杭州市上城区解放路24号', specialties: '虾肉小笼、牛肉粉丝、鸭血汤', avgCost: 30 }
    ],
    hotels: [
      { name: '杭州西湖国宾馆', type: '高端酒店', address: '浙江省杭州市西湖区杨公堤18号' },
      { name: '全季酒店（杭州西湖店）', type: '舒适型酒店', address: '浙江省杭州市西湖区北山街5号' },
      { name: '如家酒店（杭州西湖解放路店）', type: '经济型酒店', address: '浙江省杭州市上城区解放路30号' }
    ]
  },
  成都: {
    scenicSpots: [
      { name: '成都大熊猫繁育研究基地', address: '四川省成都市成华区熊猫大道1375号', openTime: '07:30 - 18:00', ticketPrice: 55, duration: '3.5小时', suggestion: '建议早上9点前到，熊猫最活跃，可看大熊猫产房', type: '自然风光' },
      { name: '武侯祠', address: '四川省成都市武侯区武侯祠大街231号', openTime: '09:00 - 18:00', ticketPrice: 50, duration: '2小时', suggestion: '三国文化圣地，可了解诸葛亮生平', type: '历史文化' },
      { name: '锦里古街', address: '四川省成都市武侯区武侯祠大街231号', openTime: '全天开放', ticketPrice: 0, duration: '2小时', suggestion: '紧邻武侯祠，可吃遍成都小吃，夜景更美', type: '美食购物' },
      { name: '宽窄巷子', address: '四川省成都市青羊区长顺上街127号', openTime: '全天开放', ticketPrice: 0, duration: '2小时', suggestion: '由宽巷子、窄巷子和井巷子组成，适合喝茶泡吧', type: '美食购物' },
      { name: '杜甫草堂', address: '四川省成都市青羊区青华路37号', openTime: '09:00 - 18:00', ticketPrice: 50, duration: '2小时', suggestion: '诗圣杜甫故居，园林清幽雅致', type: '历史文化' },
      { name: '青城山', address: '四川省成都市都江堰市青城山', openTime: '08:00 - 18:00', ticketPrice: 80, duration: '5小时', suggestion: '道教名山，建议前山看人文后山看自然', type: '自然风光' },
      { name: '都江堰', address: '四川省成都市都江堰市公园路', openTime: '08:00 - 18:00', ticketPrice: 80, duration: '3小时', suggestion: '世界文化遗产，可了解古代水利工程', type: '历史文化' }
    ],
    foods: [
      { name: '蜀大侠火锅（春熙路店）', address: '四川省成都市锦江区上东大街6号', specialties: '特色肥牛、鸭肠、毛肚', avgCost: 120 },
      { name: '大龙燚火锅（春熙路店）', address: '四川省成都市锦江区下东大街36号', specialties: '麻辣牛肉、千层肚、虾滑', avgCost: 130 },
      { name: '陈麻婆豆腐（青华路店）', address: '四川省成都市青羊区青华路19号', specialties: '麻婆豆腐、宫保鸡丁', avgCost: 70 },
      { name: '成都吃客（致民路店）', address: '四川省成都市武侯区致民路48号', specialties: '烤五花肉、辣子鸡、担担面', avgCost: 90 },
      { name: '龙抄手（春熙路店）', address: '四川省成都市锦江区春熙路南段6号', specialties: '龙抄手、钟水饺、赖汤圆', avgCost: 40 }
    ],
    hotels: [
      { name: '成都香格里拉大酒店', type: '高端酒店', address: '四川省成都市锦江区滨江东路9号' },
      { name: '全季酒店（成都春熙路店）', type: '舒适型酒店', address: '四川省成都市锦江区春熙路东段1号' },
      { name: '如家酒店（成都宽窄巷子店）', type: '经济型酒店', address: '四川省成都市青羊区长顺上街127号' }
    ]
  },
  西安: {
    scenicSpots: [
      { name: '秦始皇兵马俑博物馆', address: '陕西省西安市临潼区秦陵北路', openTime: '08:30 - 18:00', ticketPrice: 150, duration: '3.5小时', suggestion: '请讲解员能更好了解历史，建议早上去人少', type: '历史文化' },
      { name: '华清宫', address: '陕西省西安市临潼区华清路38号', openTime: '07:30 - 18:00', ticketPrice: 120, duration: '2.5小时', suggestion: '可看《长恨歌》演出（需单独购票），温泉值得体验', type: '历史文化' },
      { name: '西安城墙', address: '陕西省西安市市中心区', openTime: '08:00 - 22:00', ticketPrice: 54, duration: '3小时', suggestion: '建议租自行车环城一周，傍晚日落时分最美', type: '历史文化' },
      { name: '钟鼓楼广场', address: '陕西省西安市莲湖区东西南北四条大街交汇处', openTime: '08:30 - 21:30', ticketPrice: 50, duration: '1.5小时', suggestion: '可登钟楼鼓楼，夜景更佳', type: '历史文化' },
      { name: '回民街', address: '陕西省西安市莲湖区北院门1号', openTime: '全天开放', ticketPrice: 0, duration: '2小时', suggestion: '美食一条街，晚上最热闹', type: '美食购物' },
      { name: '陕西历史博物馆', address: '陕西省西安市雁塔区小寨东路91号', openTime: '09:00 - 17:30（周一闭馆）', ticketPrice: 0, duration: '3.5小时', suggestion: '免费但需预约，何家村珍宝馆和唐代壁画值得看', type: '历史文化' },
      { name: '大雁塔', address: '陕西省西安市雁塔区雁塔路1号', openTime: '08:00 - 18:30', ticketPrice: 40, duration: '2小时', suggestion: '可登塔远眺，北广场音乐喷泉必看', type: '历史文化' },
      { name: '大唐不夜城', address: '陕西省西安市雁塔区大雁塔南广场', openTime: '全天开放', ticketPrice: 0, duration: '2.5小时', suggestion: '夜景最美，可看不倒翁小姐姐表演', type: '美食购物' }
    ],
    foods: [
      { name: '老孙家饭庄', address: '陕西省西安市碑林区东大街364号', specialties: '羊肉泡馍、腊汁肉夹馍', avgCost: 80 },
      { name: '同盛祥', address: '陕西省西安市莲湖区钟鼓楼广场', specialties: '羊肉泡馍、酸梅汤', avgCost: 70 },
      { name: '德发长饺子馆', address: '陕西省西安市莲湖区钟鼓楼广场', specialties: '饺子宴、酸汤水饺', avgCost: 100 },
      { name: '回民街老米家泡馍', address: '陕西省西安市莲湖区北院门127号', specialties: '羊肉泡馍、糖蒜', avgCost: 60 },
      { name: '贾三灌汤包子馆', address: '陕西省西安市莲湖区北院门93号', specialties: '牛肉灌汤包、酸梅汤', avgCost: 50 }
    ],
    hotels: [
      { name: '西安香格里拉大酒店', type: '高端酒店', address: '陕西省西安市雁塔区科技路38号' },
      { name: '全季酒店（西安钟鼓楼店）', type: '舒适型酒店', address: '陕西省西安市莲湖区北大街1号' },
      { name: '如家酒店（西安回民街店）', type: '经济型酒店', address: '陕西省西安市莲湖区西大街86号' }
    ]
  }
}

const generatePlan = () => {
  if (!data.form.destination) {
    ElMessage.warning('请输入目的地')
    return
  }
  if (!data.form.startDate || !data.form.endDate) {
    ElMessage.warning('请选择出行日期')
    return
  }
  if (new Date(data.form.startDate) > new Date(data.form.endDate)) {
    ElMessage.warning('结束日期不能早于开始日期')
    return
  }
  
  data.loading = true
  
  setTimeout(() => {
    data.result = generateMockPlan()
    data.showResult = true
    data.loading = false
    ElMessage.success('行程方案生成成功！')
  }, 1500)
}

const generateMockPlan = () => {
  const days = Math.ceil((new Date(data.form.endDate) - new Date(data.form.startDate)) / (1000 * 60 * 60 * 24)) + 1
  const destData = destinationData[data.form.destination] || destinationData.北京
  const dailyPlans = []
  
  for (let i = 0; i < days; i++) {
    const date = new Date(data.form.startDate)
    date.setDate(date.getDate() + i)
    const dateStr = date.toLocaleDateString('zh-CN')
    
    const dayNodes = []
    const spotsPerDay = Math.min(3, destData.scenicSpots.length)
    const startIdx = i % (destData.scenicSpots.length - spotsPerDay + 1)
    
    if (destData.foods.length > 0) {
      const breakfast = destData.foods[i % destData.foods.length]
      dayNodes.push({
        name: breakfast.name,
        type: 'food',
        time: '08:00 - 09:00',
        address: breakfast.address,
        specialties: breakfast.specialties,
        avgCost: breakfast.avgCost,
        cost: breakfast.avgCost,
        transport: null
      })
    }
    
    for (let j = 0; j < spotsPerDay; j++) {
      const spot = destData.scenicSpots[startIdx + j]
      const prevNode = dayNodes[dayNodes.length - 1]
      
      dayNodes.push({
        name: spot.name,
        type: 'scenic',
        time: `${9 + j * 3}:00 - ${11 + j * 3}:00`,
        address: spot.address,
        openTime: spot.openTime,
        ticketPrice: spot.ticketPrice,
        duration: spot.duration,
        suggestion: spot.suggestion,
        cost: spot.ticketPrice,
        transport: prevNode ? {
          type: '打车',
          route: `从${prevNode.name}出发，经主要街道`,
          time: '20-30分钟',
          cost: 25,
          tips: '建议避开早晚高峰（7:30-9:00, 17:30-19:30）'
        } : null
      })
      
      if (j < spotsPerDay && destData.foods.length > 0) {
        const lunch = destData.foods[(i + j + 1) % destData.foods.length]
        dayNodes.push({
          name: lunch.name,
          type: 'food',
          time: `${11 + j * 3}:00 - ${13 + j * 3}:00`,
          address: lunch.address,
          specialties: lunch.specialties,
          avgCost: lunch.avgCost,
          cost: lunch.avgCost,
          transport: {
            type: '步行',
            route: '从景点步行约5-10分钟',
            time: '10分钟',
            cost: 0
          }
        })
      }
    }
    
    if (destData.foods.length > 0) {
      const dinner = destData.foods[(i + spotsPerDay) % destData.foods.length]
      dayNodes.push({
        name: dinner.name,
        type: 'food',
        time: '18:00 - 19:30',
        address: dinner.address,
        specialties: dinner.specialties,
        avgCost: dinner.avgCost,
        cost: dinner.avgCost,
        transport: {
          type: '公交',
          route: '乘坐市区公交线路',
          time: '25分钟',
          cost: 2
        }
      })
    }
    
    if (destData.hotels.length > 0) {
      const hotel = destData.hotels[0]
      dayNodes.push({
        name: hotel.name,
        type: 'hotel',
        time: '20:00',
        address: hotel.address,
        cost: 0
      })
    }
    
    dailyPlans.push({
      date: dateStr,
      totalCost: Math.floor(Math.random() * 500) + 600,
      nodes: dayNodes,
      costBreakdown: {
        tickets: dayNodes.filter(n => n.type === 'scenic').reduce((sum, n) => sum + (n.ticketPrice || 0), 0),
        food: dayNodes.filter(n => n.type === 'food').reduce((sum, n) => sum + (n.avgCost || 0), 0),
        transport: 50 + i * 10,
        accommodation: 200 + i * 50,
        other: 50,
        total: 0
      }
    })
    
    dailyPlans[i].costBreakdown.total = 
      dailyPlans[i].costBreakdown.tickets +
      dailyPlans[i].costBreakdown.food +
      dailyPlans[i].costBreakdown.transport +
      dailyPlans[i].costBreakdown.accommodation +
      dailyPlans[i].costBreakdown.other
    dailyPlans[i].totalCost = dailyPlans[i].costBreakdown.total
  }
  
  return {
    dailyPlans,
    transportOptions: [
      {
        name: '经济型方案',
        cost: '¥' + (days * 100),
        time: '适中',
        comfort: '一般',
        pros: '费用最低，适合预算有限的游客，可体验当地公共交通',
        cons: '耗时较长，需要多次换乘，携带行李不方便'
      },
      {
        name: '舒适型方案',
        cost: '¥' + (days * 200),
        time: '较短',
        comfort: '良好',
        pros: '时间和费用平衡，舒适度较好，景点间转场高效',
        cons: '费用相对较高，高峰时段可能堵车'
      },
      {
        name: '快捷型方案',
        cost: '¥' + (days * 350),
        time: '最短',
        comfort: '高',
        pros: '最节省时间，舒适度最高，可灵活调整行程',
        cons: '费用最高，需要提前规划好路线'
      }
    ]
  }
}

const resetForm = () => {
  data.form = {
    destination: '',
    startDate: '',
    endDate: '',
    totalBudget: null,
    dailyBudget: null,
    transportPreferences: [],
    accommodation: '',
    scenicTypes: [],
    foodStyles: [],
    flexibleNotes: ''
  }
  data.showResult = false
}

const backToForm = () => {
  data.showResult = false
}

const savePlan = () => {
  const user = JSON.parse(localStorage.getItem('xm-user') || '{}')
  if (!user.id) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  
  const planData = {
    destination: data.form.destination,
    startDate: data.form.startDate,
    endDate: data.form.endDate,
    planData: JSON.stringify(data.result),
    totalBudget: data.form.totalBudget,
    dailyBudget: data.form.dailyBudget
  }
  
  request.post('/travelPlan/add', planData).then(res => {
    if (res.code === '200') {
      ElMessage.success('行程保存成功！')
    } else {
      ElMessage.error(res.msg || '保存失败')
    }
  })
}
</script>

<style scoped>
.bg {
  height: 300px;
  background-image: url("@/assets/imgs/lxbg.jpg");
  background-size: 100% 110%;
}

.form-card {
  padding: 20px;
}

.plan-card {
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.timeline-card {
  margin-bottom: 10px;
}
</style>
