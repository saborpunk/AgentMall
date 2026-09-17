<template>
  <div class="guide-page">
    <section class="guide-hero">
      <div class="hero-copy">
        <div class="assistant-mark">
          <img src="@/assets/imgs/logo.png" alt="AI 导购">
          <span>AI SHOPPING GUIDE</span>
        </div>
        <h1>把需求交给我，帮你选得更合适</h1>
        <p>我会结合你的预算、用途、商品库存、价格优惠和历史偏好，生成可以直接购买的推荐清单。</p>
      </div>
      <div class="hero-capabilities">
        <span><i></i> 理解需求</span>
        <span><i></i> 实时查价</span>
        <span><i></i> 库存校验</span>
        <span><i></i> 推荐解释</span>
      </div>
    </section>

    <section class="guide-workspace">
      <div class="demand-card">
        <div class="section-heading">
          <div>
            <span>STEP 01</span>
            <h2>告诉我你想买什么</h2>
          </div>
          <div class="online-dot">AI 在线</div>
        </div>

        <div class="suggestion-list">
          <button v-for="item in suggestions" :key="item" type="button" @click="data.demandText = item">{{ item }}</button>
        </div>

        <el-input
            v-model="data.demandText"
            type="textarea"
            :rows="6"
            maxlength="500"
            show-word-limit
            resize="none"
            placeholder="例如：想买一台适合大学生写代码和做毕设的笔记本，预算 5000 左右，希望轻薄、续航好……">
        </el-input>

        <div class="condition-grid">
          <div class="condition-item">
            <label>预算上限</label>
            <el-input-number v-model="data.budgetAmount" :min="0" :precision="2" :step="500" controls-position="right" />
          </div>
          <div class="condition-item">
            <label>参考商品 <small>可选</small></label>
            <el-select v-model="data.productId" clearable filterable placeholder="选择一件相似商品">
              <el-option v-for="item in data.productOptions" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </div>
        </div>

        <el-button class="generate-btn" type="primary" :loading="data.generating" @click="generate">
          {{ data.generating ? 'AI 正在分析商品与库存…' : '生成智能推荐' }}
        </el-button>
        <p class="generate-tip">推荐过程会读取你的收藏与购物偏好，仅用于本次导购分析</p>
      </div>

      <div class="result-card" ref="resultRef">
        <div class="section-heading result-heading">
          <div>
            <span>STEP 02</span>
            <h2>AI 推荐结果</h2>
          </div>
          <el-tag v-if="data.currentTask" :type="statusType(data.currentTask.status)">{{ statusName(data.currentTask.status) }}</el-tag>
        </div>

        <div v-if="data.generating" class="thinking-state">
          <img src="@/assets/imgs/logo.png" alt="AI 思考中">
          <div class="thinking-lines"><i></i><i></i><i></i></div>
          <strong>正在理解需求并校验候选商品</strong>
          <p>正在执行需求理解、商品召回、用户画像、价格库存校验等步骤</p>
        </div>

        <div v-else-if="!data.currentTask" class="empty-state">
          <img src="@/assets/imgs/logo.png" alt="AI 导购">
          <h3>你的推荐清单将在这里生成</h3>
          <p>描述得越具体，推荐结果会越贴近你的真实需求。</p>
        </div>

        <template v-else>
          <div class="task-summary">
            <div>
              <span>{{ data.currentTask.taskNo }}</span>
              <p>{{ data.currentTask.demandText }}</p>
            </div>
            <time>{{ data.currentTask.executeTime || data.currentTask.createTime }}</time>
          </div>

          <div v-if="data.currentTask.status === 'FAILED'" class="failed-state">
            <strong>本次暂未找到合适商品</strong>
            <p>{{ data.currentTask.executeMessage || '请调整预算或补充更具体的购物需求后重试。' }}</p>
          </div>

          <div v-else-if="data.recommendations.length" class="recommendation-list">
            <article v-for="item in data.recommendations" :key="item.id" class="recommendation-item">
              <div class="rank-badge">TOP {{ item.recommendRank }}</div>
              <!-- 点封面进商品详情页，和首页商品卡片的交互保持一致 -->
              <el-image class="product-image" :src="item.productImage" fit="cover" @click="goDetail(item)">
                <template #error><div class="image-fallback">商品</div></template>
              </el-image>
              <div class="recommend-main">
                <div class="product-title">
                  <!-- 点标题同样进详情页 -->
                  <h3 @click="goDetail(item)">{{ item.productName }}</h3>
                  <span>{{ item.recommendScore }}% 匹配</span>
                </div>
                <p class="reason">{{ item.recommendReason }}</p>
                <p class="evidence">{{ item.evidenceSummary }}</p>
                <div class="recommend-meta">
                  <strong>¥{{ formatMoney(item.priceSnapshot) }}</strong>
                  <del v-if="Number(item.originalPriceSnapshot) > Number(item.priceSnapshot)">¥{{ formatMoney(item.originalPriceSnapshot) }}</del>
                  <span>可售 {{ item.availableQuantity }} 件</span>
                  <el-button type="primary" size="small" @click="addCart(item)">加入购物车</el-button>
                </div>
              </div>
            </article>
          </div>

          <div v-else class="loading-result">推荐明细加载中…</div>
        </template>
      </div>
    </section>

    <section class="history-card">
      <div class="history-heading">
        <div>
          <span>MY GUIDE HISTORY</span>
          <h2>最近导购记录</h2>
        </div>
        <small>点击记录可重新查看推荐</small>
      </div>
      <div v-if="data.history.length" class="history-list">
        <!-- 外层用 div 而不是 button：里面还要放一个删除按钮，button 嵌 button 是非法结构 -->
        <div
            v-for="item in data.history"
            :key="item.id"
            :class="['history-item', { active: data.currentTask?.id === item.id }]">
          <!-- 除删除图标外的整块区域都可以点开这条记录 -->
          <div class="history-main" @click="selectTask(item)">
            <span class="history-status" :class="item.status.toLowerCase()"></span>
            <div class="history-text">
              <strong>{{ item.demandText }}</strong>
              <small>{{ item.taskNo }} · {{ item.executeTime || item.createTime }}</small>
            </div>
            <b>{{ statusName(item.status) }} →</b>
          </div>
          <!-- @click.stop 防止点删除时冒泡到上面的选中事件 -->
          <el-icon class="history-del" title="删除这条记录" @click.stop="delTask(item)">
            <Delete />
          </el-icon>
        </div>
      </div>
      <el-empty v-else :image-size="70" description="还没有导购记录，提交一次需求试试吧" />
    </section>
  </div>
</template>

<script setup>
import { nextTick, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'
import request from '@/utils/request.js'
import router from '@/router/index.js'

const suggestions = [
  '5000 元左右的轻薄办公本',
  '适合送父母的健康家电',
  '通勤用的降噪蓝牙耳机'
]

// 推荐结果区的 DOM 引用，点历史记录后滚动到这里
const resultRef = ref()

const data = reactive({
  user: JSON.parse(localStorage.getItem('sys-user') || '{}'),
  demandText: '',
  budgetAmount: 5000,
  productId: null,
  productOptions: [],
  generating: false,
  currentTask: null,
  recommendations: [],
  history: []
})

const checkLogin = () => {
  if (!data.user.id || !data.user.token) {
    ElMessage.warning('请先登录后使用 AI 智能导购')
    router.push('/login')
    return false
  }
  return true
}

const formatMoney = value => Number(value || 0).toFixed(2)

const statusName = status => {
  if (status === 'DONE') return '已完成'
  if (status === 'FAILED') return '未匹配'
  return '分析中'
}

const statusType = status => {
  if (status === 'DONE') return 'success'
  if (status === 'FAILED') return 'danger'
  return 'warning'
}

const loadProducts = () => {
  request.get('/product/selectAll', { params: { status: 'ON_SALE' } }).then(res => {
    if (res.code === '200') data.productOptions = res.data || []
  })
}

const loadRecommendations = taskId => {
  data.recommendations = []
  if (!taskId) return Promise.resolve()
  return request.get('/shoppingRecommendation/selectAll', {
    params: { taskId, userId: data.user.id }
  }).then(res => {
    if (res.code === '200') data.recommendations = res.data || []
  })
}

const loadHistory = (autoSelect = true) => {
  if (!checkLogin()) return
  request.get('/shoppingGuideTask/selectAll', {
    params: { userId: data.user.id }
  }).then(res => {
    if (res.code !== '200') return
    data.history = (res.data || []).slice(0, 8)
    if (autoSelect && !data.currentTask && data.history.length) {
      selectTask(data.history[0])
    }
  })
}

// 删除一条自己的导购记录。删掉的如果正是当前正在看的那条，
// 结果区要一起清空，否则页面上会留着一条已经不存在的记录的推荐结果。
const delTask = item => {
  ElMessageBox.confirm('删除后这条导购记录和它的推荐结果都不再保留，确定删除吗？', '删除导购记录', {
    type: 'warning'
  }).then(() => {
    request.delete('/shoppingGuideTask/delete/' + item.id).then(res => {
      if (res.code !== '200') {
        ElMessage.error(res.msg)
        return
      }
      ElMessage.success('已删除')
      if (data.currentTask?.id === item.id) {
        data.currentTask = null
        data.recommendations = []
      }
      // 重新拉历史列表；当前记录被删掉时允许自动选中剩下的最新一条
      loadHistory(!data.currentTask)
    })
  }).catch(() => {})
}

const selectTask = task => {
  data.currentTask = { ...task }
  if (task.status === 'DONE') {
    loadRecommendations(task.id)
  } else {
    data.recommendations = []
  }
  // 推荐结果区在页面上方，历史记录在下方，
  // 不滚动过去的话点了记录页面没有任何变化，用户会以为没反应
  nextTick(() => {
    resultRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  })
}

const generate = async () => {
  if (!checkLogin()) return
  if (!data.demandText.trim()) {
    ElMessage.warning('请先描述你的购物需求')
    return
  }
  data.generating = true
  data.currentTask = null
  data.recommendations = []
  try {
    const addRes = await request.post('/shoppingGuideTask/add', {
      userId: data.user.id,
      demandText: data.demandText.trim(),
      budgetAmount: data.budgetAmount,
      productId: data.productId
    })
    if (addRes.code !== '200' || !addRes.data?.id) {
      ElMessage.error(addRes.msg || '导购任务创建失败')
      return
    }
    const executeRes = await request.post('/shoppingGuideTask/execute/' + addRes.data.id)
    if (executeRes.code !== '200') {
      ElMessage.error(executeRes.msg || 'AI 导购执行失败')
      return
    }
    data.currentTask = executeRes.data
    await loadRecommendations(executeRes.data.id)
    loadHistory(false)
    ElMessage.success(executeRes.data.status === 'DONE' ? '智能推荐已生成' : '本次暂未匹配到合适商品')
  } catch (error) {
    ElMessage.error(error?.message || 'AI 导购暂时不可用，请稍后重试')
  } finally {
    data.generating = false
  }
}

// 推荐结果里的 productId 就是真实商品 ID，直接跳详情页看完整信息
const goDetail = item => {
  router.push('/front/product/' + item.productId)
}

const addCart = item => {
  request.post('/shoppingCart/add', {
    userId: data.user.id,
    productId: item.productId,
    quantity: 1
  }).then(res => {
    if (res.code === '200') ElMessage.success('已加入购物车')
    else ElMessage.error(res.msg)
  })
}

loadProducts()
loadHistory()
</script>

<style scoped>
.guide-page { max-width: 1180px; margin: 0 auto; padding: 24px 0 36px; color: #172033; }
.guide-hero { min-height: 210px; display: flex; align-items: center; justify-content: space-between; gap: 30px; padding: 34px 40px; overflow: hidden; border-radius: 16px; color: #fff; background: radial-gradient(circle at 82% 0, rgba(84, 214, 228, .28), transparent 35%), linear-gradient(135deg, #081722, #103745); }
.assistant-mark { display: flex; align-items: center; gap: 10px; color: #72e2ed; font-size: 11px; font-weight: 800; letter-spacing: .14em; }
.assistant-mark img { width: 38px; height: 38px; object-fit: cover; border: 2px solid rgba(255,255,255,.8); border-radius: 11px; }
.hero-copy h1 { margin: 15px 0 0; font-size: 31px; letter-spacing: -.03em; }
.hero-copy p { max-width: 620px; margin: 11px 0 0; color: #a7bec8; font-size: 13px; line-height: 1.8; }
.hero-capabilities { width: 210px; display: grid; grid-template-columns: 1fr 1fr; gap: 10px; flex-shrink: 0; }
.hero-capabilities span { padding: 10px; border: 1px solid rgba(255,255,255,.1); border-radius: 9px; color: #c3d4dc; background: rgba(255,255,255,.06); font-size: 11px; }
.hero-capabilities i { display: inline-block; width: 6px; height: 6px; margin-right: 5px; border-radius: 50%; background: #56d8e5; }
.guide-workspace { display: grid; grid-template-columns: 390px 1fr; gap: 16px; margin-top: 16px; }
.demand-card, .result-card, .history-card { border: 1px solid #e3e9ef; border-radius: 14px; background: #fff; box-shadow: 0 8px 25px rgba(29, 50, 75, .045); }
.demand-card, .result-card { min-height: 520px; padding: 22px; }
.section-heading, .history-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 15px; }
.section-heading span, .history-heading span { color: #1595a6; font-size: 9px; font-weight: 800; letter-spacing: .13em; }
.section-heading h2, .history-heading h2 { margin: 5px 0 0; font-size: 19px; }
.online-dot { padding: 5px 8px; border-radius: 20px; color: #188a74; background: #e9faf5; font-size: 10px; }
.suggestion-list { display: flex; flex-wrap: wrap; gap: 6px; margin: 19px 0 12px; }
.suggestion-list button { padding: 6px 8px; border: 1px solid #dce8eb; border-radius: 20px; color: #59717a; background: #f7fbfc; cursor: pointer; font-size: 10px; }
.suggestion-list button:hover { color: #12899a; border-color: #65c8d4; }
.demand-card :deep(.el-textarea__inner) { border-radius: 10px; background: #f8fafc; box-shadow: 0 0 0 1px #e2e8f0 inset; line-height: 1.7; }
.condition-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; margin-top: 15px; }
.condition-item label { display: block; margin-bottom: 7px; color: #526070; font-size: 11px; font-weight: 700; }
.condition-item label small { color: #a2adb9; font-weight: 400; }
.condition-item .el-input-number, .condition-item .el-select { width: 100%; }
.generate-btn { width: 100%; height: 44px; margin-top: 18px; border: 0; border-radius: 9px; background: linear-gradient(100deg, #1298aa, #2879c8); font-weight: 700; }
.generate-tip { margin: 9px 0 0; color: #99a4b1; font-size: 9px; text-align: center; }
.result-heading { padding-bottom: 15px; border-bottom: 1px solid #edf1f4; }
.empty-state, .thinking-state { min-height: 405px; display: flex; flex-direction: column; align-items: center; justify-content: center; text-align: center; }
.empty-state img, .thinking-state img { width: 74px; height: 74px; object-fit: cover; border-radius: 22px; box-shadow: 0 12px 30px rgba(31, 139, 177, .16); }
.empty-state h3, .thinking-state strong { margin: 18px 0 0; font-size: 15px; }
.empty-state p, .thinking-state p { max-width: 390px; margin: 8px 0 0; color: #8b97a5; font-size: 11px; line-height: 1.7; }
.thinking-lines { display: flex; gap: 5px; margin-top: 14px; }
.thinking-lines i { width: 6px; height: 6px; border-radius: 50%; background: #23a8b9; animation: pulse 1s infinite alternate; }
.thinking-lines i:nth-child(2) { animation-delay: .2s; }.thinking-lines i:nth-child(3) { animation-delay: .4s; }
@keyframes pulse { to { opacity: .2; transform: translateY(-3px); } }
.task-summary { display: flex; justify-content: space-between; gap: 15px; padding: 13px 0; }
.task-summary span { color: #84909d; font-size: 9px; }.task-summary p { margin: 4px 0 0; font-size: 11px; }.task-summary time { color: #a1abb7; font-size: 9px; white-space: nowrap; }
.recommendation-list { display: grid; gap: 10px; }
.recommendation-item { position: relative; display: grid; grid-template-columns: 86px 1fr; gap: 13px; padding: 12px; overflow: hidden; border: 1px solid #e5ebf0; border-radius: 11px; background: #fbfcfd; }
.rank-badge { position: absolute; top: 0; left: 0; z-index: 1; padding: 3px 7px; border-radius: 0 0 7px 0; color: #fff; background: #1697a8; font-size: 8px; font-weight: 800; }
/* 封面和标题都可点击进详情页，鼠标样式要跟上 */
.product-image { width: 86px; height: 86px; border-radius: 8px; background: #eef2f5; cursor: pointer; }
.image-fallback { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; color: #9ba7b3; font-size: 11px; }
.product-title { display: flex; align-items: center; justify-content: space-between; gap: 10px; }.product-title h3 { margin: 0; font-size: 14px; cursor: pointer; }.product-title h3:hover { color: #df4338; }.product-title span { padding: 3px 7px; border-radius: 12px; color: #148875; background: #e8f8f4; font-size: 9px; white-space: nowrap; }
.reason, .evidence { margin: 6px 0 0; color: #576879; font-size: 10px; line-height: 1.5; }.evidence { color: #929daa; }
.recommend-meta { display: flex; align-items: center; gap: 8px; margin-top: 8px; }.recommend-meta strong { color: #df4338; font-size: 15px; }.recommend-meta del { color: #a9b1bb; font-size: 9px; }.recommend-meta > span { margin-right: auto; color: #7f8c99; font-size: 9px; }
.failed-state { margin-top: 25px; padding: 22px; border-radius: 10px; color: #8a4b33; background: #fff5ef; }.failed-state p { margin: 7px 0 0; font-size: 11px; }
.loading-result { padding: 50px; color: #8793a0; text-align: center; }
.history-card { margin-top: 16px; padding: 20px 22px; }.history-heading small { color: #9ba5b0; font-size: 10px; }
.history-list { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; margin-top: 15px; }
.history-item { display: grid; grid-template-columns: 1fr auto; align-items: center; gap: 8px; min-width: 0; padding: 11px 12px; border: 1px solid #e7ebef; border-radius: 9px; background: #fff; text-align: left; }
/* 内容区保留原来的三列布局（状态点 / 文字 / 状态文字），只有它可点 */
.history-main { display: grid; grid-template-columns: 8px 1fr auto; align-items: center; gap: 10px; min-width: 0; cursor: pointer; }
/* 删除图标默认淡一点，鼠标移到卡片上才明显，避免列表看起来到处是删除按钮 */
.history-del { color: #c9d0d8; font-size: 13px; cursor: pointer; transition: color .2s; }
.history-item:hover .history-del { color: #9ba4af; }
.history-del:hover { color: #e16a5d; }
.history-item:hover, .history-item.active { border-color: #71c9d3; background: #f5fcfd; }.history-status { width: 7px; height: 7px; border-radius: 50%; background: #e4a83a; }.history-status.done { background: #24ad8e; }.history-status.failed { background: #e16a5d; }
.history-text { min-width: 0; }.history-text strong { display: block; overflow: hidden; color: #354252; font-size: 11px; text-overflow: ellipsis; white-space: nowrap; }.history-text small { display: block; margin-top: 4px; color: #9ba4af; font-size: 8px; }.history-main b { color: #168f9f; font-size: 9px; white-space: nowrap; }
@media (max-width: 900px) { .guide-workspace { grid-template-columns: 1fr; }.demand-card, .result-card { min-height: auto; }.hero-capabilities { display: none; }.history-list { grid-template-columns: 1fr; } }
@media (max-width: 620px) { .guide-page { padding: 12px; }.guide-hero { padding: 25px 22px; }.hero-copy h1 { font-size: 24px; }.condition-grid { grid-template-columns: 1fr; }.recommendation-item { grid-template-columns: 70px 1fr; }.product-image { width: 70px; height: 70px; }.recommend-meta { flex-wrap: wrap; }.history-card { padding: 17px; } }
</style>
