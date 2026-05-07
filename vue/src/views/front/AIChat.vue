<template>
  <div class="ai-chat-wrapper">
    <div class="ai-chat-header">
      AI 路线规划助手
    </div>

    <div class="ai-chat-main">
      <!-- 对话历史区域 -->
      <div ref="chatContainer" class="chat-history-area">
        <div v-if="chatHistory.length === 0" class="empty-state">
          <p class="empty-title">欢迎使用 AI 路线规划助手！</p>
          <p class="empty-subtitle">请告诉我您的旅游需求，例如：</p>
          <p class="empty-example">"我想规划一个北京3日游"</p>
          <p class="empty-example">"推荐一下去成都的旅游路线"</p>
        </div>
        
        <div v-for="(msg, index) in chatHistory" :key="index" class="chat-message">
          <!-- 用户消息 -->
          <div v-if="msg.role === 'user'" class="message-wrapper user-message-wrapper">
            <div class="message-bubble user-message-bubble">
              <div class="message-content">{{ msg.content }}</div>
            </div>
            <div class="message-label user-label">用户</div>
          </div>
          
          <!-- AI消息 -->
          <div v-if="msg.role === 'assistant'" class="message-wrapper assistant-message-wrapper">
            <div class="message-bubble assistant-message-bubble">
              <div class="message-content assistant-content" v-html="formatMessage(msg.content)"></div>
              <div class="message-actions">
                <el-button type="primary" size="small" link @click="openSaveDialog(msg)">
                  <el-icon><DocumentAdd /></el-icon>保存到行程表
                </el-button>
              </div>
            </div>
            <div class="message-label assistant-label">AI 助手</div>
          </div>
        </div>
        
        <!-- 加载状态 -->
        <div v-if="isLoading" class="loading-wrapper">
          <div class="message-bubble assistant-message-bubble">
            <div class="loading-content">
              <el-icon class="is-loading loading-icon"><loading /></el-icon>
              <span>{{ loadingText }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 输入区域 - 固定在底部 -->
      <div class="input-area">
        <div class="input-wrapper">
          <el-input
            v-model="inputMessage"
            type="textarea"
            :rows="3"
            placeholder="请输入您的旅游需求..."
            @keydown.enter.ctrl="sendMessage"
            :disabled="isLoading"
            class="chat-textarea"
            resize="none"
          />
          <div class="button-wrapper">
            <el-button type="primary" @click="sendMessage" :loading="isLoading" class="send-btn">
              {{ isLoading ? '发送中...' : '发送' }}
            </el-button>
            <el-button @click="clearChat" :disabled="isLoading" class="clear-btn">
              清空
            </el-button>
          </div>
        </div>
        <div class="tips-text">
          提示：按 Ctrl+Enter 发送消息 | 如果响应较慢，请耐心等待或稍后重试
        </div>
      </div>
    </div>

    <!-- 保存对话框 -->
    <el-dialog v-model="saveDialogVisible" title="保存到行程表" width="500px" :close-on-click-modal="false">
      <el-form :model="saveForm" :rules="saveRules" ref="saveFormRef" label-width="100px">
        <el-form-item label="目的地" prop="destination">
          <el-input v-model="saveForm.destination" placeholder="请输入目的地，如：北京" />
        </el-form-item>
        <el-form-item label="开始日期" prop="startDate">
          <el-date-picker 
            v-model="saveForm.startDate" 
            type="date" 
            placeholder="选择开始日期" 
            format="YYYY-MM-DD" 
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="结束日期" prop="endDate">
          <el-date-picker 
            v-model="saveForm.endDate" 
            type="date" 
            placeholder="选择结束日期" 
            format="YYYY-MM-DD" 
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="总预算" prop="totalBudget">
          <el-input-number v-model="saveForm.totalBudget" :min="0" :step="100" style="width: 100%" placeholder="可选" />
        </el-form-item>
        <el-form-item label="每日预算" prop="dailyBudget">
          <el-input-number v-model="saveForm.dailyBudget" :min="0" :step="50" style="width: 100%" placeholder="可选" />
        </el-form-item>
        <el-form-item label="路线规划">
          <el-input 
            v-model="saveForm.planData" 
            type="textarea" 
            :rows="6" 
            readonly 
            placeholder="AI生成的路线规划内容"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="saveDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, nextTick, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading, DocumentAdd } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request.js'

const router = useRouter()
const chatContainer = ref(null)
const chatHistory = reactive([])
const inputMessage = ref('')
const isLoading = ref(false)
const loadingText = ref('正在思考中...')
let loadingTimer = null

// 保存对话框相关
const saveDialogVisible = ref(false)
const saveFormRef = ref(null)
const saving = ref(false)
const saveForm = reactive({
  destination: '',
  startDate: '',
  endDate: '',
  totalBudget: null,
  dailyBudget: null,
  planData: ''
})

const saveRules = {
  destination: [
    { required: true, message: '请输入目的地', trigger: 'blur' },
    { min: 1, max: 50, message: '目的地长度在1到50个字符', trigger: 'blur' }
  ],
  startDate: [
    { required: true, message: '请选择开始日期', trigger: 'change' }
  ],
  endDate: [
    { required: true, message: '请选择结束日期', trigger: 'change' }
  ],
  planData: [
    { required: true, message: '路线规划内容不能为空', trigger: 'blur' },
    { min: 10, message: '路线规划内容至少需要10个字符', trigger: 'blur' }
  ]
}

// 格式化消息，简单处理换行
const formatMessage = (content) => {
  return content
    .replace(/\n/g, '<br/>')
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/### (.*?)(?=<br|$)/g, '<h4 style="margin-top: 10px; margin-bottom: 5px;">$1</h4>')
}

// 滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    if (chatContainer.value) {
      chatContainer.value.scrollTop = chatContainer.value.scrollHeight
    }
  })
}

// 更新加载提示文本
const updateLoadingText = () => {
  const texts = [
    '正在思考中...',
    '正在规划路线...',
    '正在整理景点信息...',
    '还在努力思考中，请稍候...',
    '快好了，请再等一下...'
  ]
  let index = 0
  loadingTimer = setInterval(() => {
    index = (index + 1) % texts.length
    loadingText.value = texts[index]
  }, 5000)
}

// 发送消息
const sendMessage = async () => {
  const message = inputMessage.value.trim()
  if (!message) {
    ElMessage.warning('请输入消息')
    return
  }
  
  if (isLoading.value) {
    return
  }
  
  // 添加用户消息
  chatHistory.push({
    role: 'user',
    content: message
  })
  inputMessage.value = ''
  scrollToBottom()
  
  isLoading.value = true
  loadingText.value = '正在思考中...'
  updateLoadingText()
  
  try {
    // 发送请求到后端
    const response = await request.post('/duobao/chat', {
      message: message,
      history: chatHistory.slice(0, -1)
    })
    
    if (response.code === '200') {
      // 添加AI回复
      chatHistory.push({
        role: 'assistant',
        content: response.data
      })
    } else {
      ElMessage.error(response.msg || '发送失败')
      // 也可以把错误信息显示在对话中
      chatHistory.push({
        role: 'assistant',
        content: response.msg || '抱歉，发生了一些问题，请稍后重试。'
      })
    }
  } catch (error) {
    console.error('发送消息失败:', error)
    const errorMsg = error.response?.data?.msg || error.message || '网络错误，请稍后重试'
    ElMessage.error(errorMsg)
    // 显示错误信息在对话中
    chatHistory.push({
      role: 'assistant',
      content: '抱歉，网络连接出现问题。请检查网络后重试，或者尝试简化您的问题。'
    })
  } finally {
    isLoading.value = false
    if (loadingTimer) {
      clearInterval(loadingTimer)
      loadingTimer = null
    }
    scrollToBottom()
  }
}

// 清空对话
const clearChat = () => {
  chatHistory.length = 0
  ElMessage.success('对话已清空')
}

// 打开保存对话框
const openSaveDialog = (msg) => {
  // 检查用户登录状态
  const user = JSON.parse(localStorage.getItem('xm-user') || '{}')
  if (!user.id) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  
  // 重置表单
  saveForm.destination = ''
  saveForm.startDate = ''
  saveForm.endDate = ''
  saveForm.totalBudget = null
  saveForm.dailyBudget = null
  saveForm.planData = msg.content
  
  // 尝试从消息内容中提取目的地
  const extractedDestination = extractDestination(msg.content)
  if (extractedDestination) {
    saveForm.destination = extractedDestination
  }
  
  saveDialogVisible.value = true
}

// 尝试从文本中提取目的地
const extractDestination = (content) => {
  const patterns = [
    /(?:去|到|在|规划)([\u4e00-\u9fa5]{2,10})(?:的|游玩|旅游|旅行)/,
    /^([\u4e00-\u9fa5]{2,10})(?:\d+日游|旅游|旅行)/,
    /^([\u4e00-\u9fa5]{2,10})(?:路线|行程)/
  ]
  
  for (const pattern of patterns) {
    const match = content.match(pattern)
    if (match && match[1]) {
      return match[1]
    }
  }
  return ''
}

// 验证日期
const validateDates = () => {
  if (saveForm.startDate && saveForm.endDate) {
    const start = new Date(saveForm.startDate)
    const end = new Date(saveForm.endDate)
    if (end < start) {
      ElMessage.error('结束日期不能早于开始日期')
      return false
    }
  }
  return true
}

// 保存行程
const handleSave = async () => {
  if (!saveFormRef.value) return
  
  try {
    // 表单验证
    await saveFormRef.value.validate()
    
    // 日期验证
    if (!validateDates()) {
      return
    }
    
    saving.value = true
    
    // 发送请求
    const res = await request.post('/travelPlan/add', {
      destination: saveForm.destination,
      startDate: saveForm.startDate,
      endDate: saveForm.endDate,
      totalBudget: saveForm.totalBudget,
      dailyBudget: saveForm.dailyBudget,
      planData: saveForm.planData
    })
    
    if (res.code === '200') {
      ElMessage.success('保存成功！')
      saveDialogVisible.value = false
      
      // 询问是否跳转到我的行程表
      ElMessageBox.confirm('已成功保存到行程表，是否前往查看？', '提示', {
        confirmButtonText: '去查看',
        cancelButtonText: '继续对话',
        type: 'success'
      }).then(() => {
        router.push('/front/myPlans')
      }).catch(() => {})
    } else {
      ElMessage.error(res.msg || '保存失败')
    }
  } catch (error) {
    if (error !== false) {
      console.error('保存失败:', error)
      ElMessage.error('保存失败，请稍后重试')
    }
  } finally {
    saving.value = false
  }
}

// 清理定时器
onUnmounted(() => {
  if (loadingTimer) {
    clearInterval(loadingTimer)
  }
})
</script>

<style scoped>
.ai-chat-wrapper {
  width: 80%;
  margin: 20px auto;
  min-width: 600px;
  height: calc(100vh - 100px);
  display: flex;
  flex-direction: column;
}

.ai-chat-header {
  padding-left: 10px;
  border-left: 5px solid #189500;
  font-size: 20px;
  margin-bottom: 20px;
  flex-shrink: 0;
}

.ai-chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.chat-history-area {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: #f5f7fa;
  min-height: 0;
}

/* 对话历史容器滚动条 */
.chat-history-area::-webkit-scrollbar {
  width: 10px;
}

.chat-history-area::-webkit-scrollbar-track {
  background: #e8e8e8;
  border-radius: 5px;
}

.chat-history-area::-webkit-scrollbar-thumb {
  background: #b0b0b0;
  border-radius: 5px;
  transition: background 0.3s;
}

.chat-history-area::-webkit-scrollbar-thumb:hover {
  background: #888;
}

.empty-state {
  text-align: center;
  color: #909399;
  padding: 50px;
}

.empty-title {
  font-size: 18px;
  margin: 0;
}

.empty-subtitle {
  margin: 10px 0 0 0;
}

.empty-example {
  margin: 5px 0 0 0;
}

.chat-message {
  margin-bottom: 20px;
}

.message-wrapper {
  display: flex;
  flex-direction: column;
}

.user-message-wrapper {
  align-items: flex-end;
}

.assistant-message-wrapper {
  align-items: flex-start;
}

.message-bubble {
  max-width: 70%;
  padding: 12px 16px;
  border-radius: 8px;
}

.user-message-bubble {
  background: #409eff;
  color: white;
}

.assistant-message-bubble {
  background: white;
  border: 1px solid #dcdfe6;
}

.message-content {
  word-wrap: break-word;
  line-height: 1.6;
}

/* AI消息内容区域滚动条 */
.assistant-content {
  max-height: 400px;
  overflow-y: auto;
  padding-right: 8px;
}

/* AI消息内容区域的自定义滚动条 */
.assistant-content::-webkit-scrollbar {
  width: 6px;
}

.assistant-content::-webkit-scrollbar-track {
  background: #f5f5f5;
  border-radius: 3px;
}

.assistant-content::-webkit-scrollbar-thumb {
  background: #c0c4cc;
  border-radius: 3px;
  transition: background 0.2s;
}

.assistant-content::-webkit-scrollbar-thumb:hover {
  background: #909399;
}

.message-label {
  font-size: 12px;
  margin-top: 5px;
  color: #909399;
}

.user-label {
  text-align: right;
}

.assistant-label {
  text-align: left;
}

.message-actions {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid #ebeef5;
  text-align: right;
}

.loading-wrapper {
  display: flex;
  margin-bottom: 20px;
}

.loading-content {
  display: flex;
  align-items: center;
}

.loading-icon {
  margin-right: 8px;
}

/* 输入区域 - 固定高度和位置 */
.input-area {
  flex-shrink: 0;
  background: white;
  border-top: 1px solid #ebeef5;
  padding: 15px 20px;
}

.input-wrapper {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.chat-textarea {
  flex: 1;
}

.button-wrapper {
  display: flex;
  flex-direction: column;
  gap: 10px;
  flex-shrink: 0;
}

.send-btn {
  height: 80px;
  white-space: nowrap;
}

.clear-btn {
  height: 40px;
}

.tips-text {
  text-align: center;
  color: #909399;
  font-size: 12px;
  margin-top: 10px;
}

/* 响应式适配 */
@media (max-width: 1200px) {
  .ai-chat-wrapper {
    width: 90%;
    min-width: auto;
  }
  
  .message-bubble {
    max-width: 80%;
  }
}

@media (max-width: 768px) {
  .ai-chat-wrapper {
    width: 95%;
    height: calc(100vh - 60px);
  }
  
  .message-bubble {
    max-width: 85%;
  }
  
  .assistant-content {
    max-height: 300px;
  }
  
  .input-wrapper {
    flex-direction: column;
  }
  
  .button-wrapper {
    flex-direction: row;
    width: 100%;
  }
  
  .send-btn,
  .clear-btn {
    height: auto;
    flex: 1;
  }
}
</style>
