<template>
  <div class="detail-page" v-loading="data.loading">
    <div class="detail-back">
      <el-button link @click="router.push('/front/home')">← 返回商品列表</el-button>
    </div>

    <template v-if="data.product.id">
      <!-- 上半部分：封面 + 基础信息 + 购买操作 -->
      <section class="detail-top">
        <div class="detail-gallery">
          <el-image class="detail-cover" :src="data.currentImage" fit="cover" :preview-src-list="data.images" preview-teleported></el-image>
          <div class="detail-thumbs" v-if="data.images.length > 1">
            <!-- 点缩略图切换主图，album_images 用逗号分隔存在商品表里 -->
            <el-image
                v-for="(img, index) in data.images"
                :key="index"
                :class="['detail-thumb', img === data.currentImage ? 'active' : '']"
                :src="img"
                fit="cover"
                @click="data.currentImage = img"
            ></el-image>
          </div>
        </div>

        <div class="detail-info">
          <div class="detail-tags">
            <el-tag size="small" type="success" v-if="data.product.isRecommend === 1">推荐</el-tag>
            <el-tag size="small" type="info">{{ data.product.categoryName }}</el-tag>
            <el-tag size="small">{{ data.product.brandName }}</el-tag>
          </div>
          <h1>{{ data.product.name }}</h1>
          <p class="detail-selling">{{ data.product.sellingPoint }}</p>

          <div class="detail-price">
            <strong>¥{{ formatMoney(data.product.price) }}</strong>
            <span v-if="data.product.originalPrice">¥{{ formatMoney(data.product.originalPrice) }}</span>
          </div>

          <div class="detail-meta">
            <span>商品编号：{{ data.product.productNo }}</span>
            <!-- 库存就是商品自己的 stock_quantity 字段，为 0 时下面的按钮会禁用 -->
            <span>
              库存：
              <em :class="data.product.stockQuantity > 0 ? 'in-stock' : 'no-stock'">
                {{ data.product.stockQuantity > 0 ? data.product.stockQuantity + ' 件' : '暂时缺货' }}
              </em>
            </span>
          </div>

          <div class="detail-actions">
            <el-input-number v-model="data.quantity" :min="1" :max="Math.max(data.product.stockQuantity || 1, 1)" />
            <el-button plain @click="addFavorite">收藏</el-button>
            <el-button type="primary" :disabled="!data.product.stockQuantity" @click="addCart">加入购物车</el-button>
          </div>
        </div>
      </section>

      <!-- 下半部分：详情描述 / 规格参数 / 用户评价 -->
      <section class="detail-tabs">
        <el-tabs v-model="data.activeTab">
          <el-tab-pane label="商品详情" name="detail">
            <div v-if="data.detail.id" class="detail-block">
              <h3>商品介绍</h3>
              <pre>{{ data.detail.detailContent }}</pre>
              <template v-if="data.detail.packageInfo">
                <h3>包装清单</h3>
                <pre>{{ data.detail.packageInfo }}</pre>
              </template>
              <template v-if="data.detail.afterSaleInfo">
                <h3>售后说明</h3>
                <pre>{{ data.detail.afterSaleInfo }}</pre>
              </template>
            </div>
            <el-empty v-else description="商家还没有填写商品详情" :image-size="90" />
          </el-tab-pane>

          <el-tab-pane :label="'规格参数(' + data.params.length + ')'" name="param">
            <div v-if="data.params.length" class="param-groups">
              <!-- 参数按 param_group 分组展示，核心参数标一个"核心"标签 -->
              <div v-for="group in paramGroups" :key="group.name" class="param-group">
                <h3>{{ group.name }}</h3>
                <div class="param-row" v-for="item in group.items" :key="item.id">
                  <span class="param-name">{{ item.paramName }}</span>
                  <span class="param-value">
                    {{ item.paramValue }}
                    <el-tag v-if="item.isCore === 1" size="small" type="warning">核心</el-tag>
                  </span>
                </div>
              </div>
            </div>
            <el-empty v-else description="商家还没有维护规格参数" :image-size="90" />
          </el-tab-pane>

          <el-tab-pane :label="'用户评价(' + data.reviews.length + ')'" name="review">
            <div v-if="data.reviews.length" class="review-list">
              <div class="review-item" v-for="item in data.reviews" :key="item.id">
                <div class="review-head">
                  <strong>{{ item.userName }}</strong>
                  <el-rate :model-value="item.rating" disabled size="small" />
                  <span>{{ item.createTime }}</span>
                </div>
                <p>{{ item.content }}</p>
              </div>
            </div>
            <el-empty v-else description="这个商品还没有评价" :image-size="90" />
          </el-tab-pane>

          <el-tab-pane label="AI问答" name="qa">
            <div class="qa-block">
              <p class="qa-tip">关于这个商品想问什么？AI 会先到商品知识库里检索资料，再依据检索到的资料回答，资料里没有的内容它会直接说明。</p>

              <!-- 快捷问题只是把问题填进输入框，方便不知道问什么的用户 -->
              <div class="qa-quick">
                <el-tag
                    v-for="item in quickQuestions"
                    :key="item"
                    class="qa-quick-item"
                    effect="plain"
                    @click="data.qaQuestion = item"
                >{{ item }}</el-tag>
              </div>

              <div class="qa-input">
                <el-input
                    v-model="data.qaQuestion"
                    type="textarea"
                    :rows="3"
                    maxlength="200"
                    show-word-limit
                    placeholder="例如：这个商品的尺寸是多少？适合什么场景使用？"
                ></el-input>
                <el-button type="primary" :loading="data.qaLoading" @click="askQuestion">提问</el-button>
              </div>

              <!-- 回答区：只有拿到结果才显示 -->
              <div class="qa-answer" v-if="data.qaResult.id">
                <div class="qa-answer-head">
                  <strong>AI 回答</strong>
                  <!-- toolTrace 记录这次回答走了哪条链路，方便理解 RAG 过程 -->
                  <el-tag size="small" type="info">{{ data.qaResult.toolTrace }}</el-tag>
                </div>
                <pre class="qa-answer-text">{{ data.qaResult.answerText }}</pre>

                <!-- evidenceContent 是本次召回的知识切片原文，展示出来才能看出回答有没有依据 -->
                <el-collapse v-if="data.qaResult.evidenceContent">
                  <el-collapse-item title="查看 AI 引用的商品资料">
                    <pre class="qa-evidence">{{ data.qaResult.evidenceContent }}</pre>
                  </el-collapse-item>
                </el-collapse>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </section>
    </template>

    <el-empty v-else-if="!data.loading" description="商品不存在或已下架" />
  </div>
</template>

<script setup>
import { computed, reactive } from "vue";
import { useRoute } from "vue-router";
import request from "@/utils/request.js";
import { ElMessage } from "element-plus";
import router from "@/router/index.js";

const route = useRoute()

const data = reactive({
  // 当前登录用户，收藏和加购都要带 userId
  user: JSON.parse(localStorage.getItem('sys-user') || '{}'),
  loading: true,
  // 商品主信息，来自 /product/selectAll 按 id 过滤
  product: {},
  // 商品详情描述，来自 /productDetail/selectByProductId/{id}
  detail: {},
  // 规格参数列表，来自 /productParam/selectAll?productId=
  params: [],
  // 已通过审核的评价列表
  reviews: [],
  // 主图和相册图片
  images: [],
  currentImage: '',
  quantity: 1,
  activeTab: 'detail',
  // AI 问答输入的问题
  qaQuestion: '',
  // 提问按钮的 loading，AI 回答要等几秒
  qaLoading: false,
  // 本次问答结果，字段来自后端 shopping_qa 表
  qaResult: {}
})

// 快捷问题，点一下填进输入框，降低"不知道问什么"的门槛
const quickQuestions = [
  '这个商品的规格参数是怎样的？',
  '它适合什么场景使用？',
  '有哪些配件或包装清单？',
  '售后和保修是怎么规定的？'
]

// 参数按 param_group 分组，页面按组展示
const paramGroups = computed(() => {
  const groups = []
  data.params.forEach(item => {
    const name = item.paramGroup || '基础参数'
    let group = groups.find(g => g.name === name)
    if (!group) {
      group = { name, items: [] }
      groups.push(group)
    }
    group.items.push(item)
  })
  return groups
})

const formatMoney = (value) => {
  return Number(value || 0).toFixed(2)
}

const checkLogin = () => {
  if (!data.user.id) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return false
  }
  return true
}

// 商品主信息：复用商品列表接口，按 id 精确取一条
const loadProduct = (productId) => {
  request.get('/product/selectAll', { params: { id: productId } }).then(res => {
    if (res.code === '200') {
      data.product = (res.data || [])[0] || {}
      // 封面加相册拼成预览图列表，相册用逗号分隔
      const album = (data.product.albumImages || '').split(',').filter(item => item)
      data.images = [data.product.coverImage, ...album].filter(item => item)
      data.currentImage = data.images[0] || ''
    }
  }).finally(() => {
    data.loading = false
  })
}

// 商品详情描述：一个商品最多一条，没维护时返回 null
const loadDetail = (productId) => {
  request.get('/productDetail/selectByProductId/' + productId).then(res => {
    if (res.code === '200') {
      data.detail = res.data || {}
    }
  })
}

// 规格参数：按 sort 排好序返回，页面再按分组归类
const loadParams = (productId) => {
  request.get('/productParam/selectAll', { params: { productId } }).then(res => {
    if (res.code === '200') {
      data.params = res.data || []
    }
  })
}

// 用户评价：只展示审核通过的，避免未审核内容直接对外
const loadReviews = (productId) => {
  request.get('/productReview/selectPage', {
    params: {
      pageNum: 1,
      pageSize: 20,
      productId,
      auditStatus: 'APPROVED'
    }
  }).then(res => {
    if (res.code === '200') {
      data.reviews = res.data?.list || []
    }
  })
}

const addFavorite = () => {
  if (!checkLogin()) {
    return
  }
  request.post('/productFavorite/add', {
    userId: data.user.id,
    productId: data.product.id
  }).then(res => {
    if (res.code === '200') {
      ElMessage.success('收藏成功')
    } else {
      ElMessage.error(res.msg)
    }
  })
}

const addCart = () => {
  if (!checkLogin()) {
    return
  }
  request.post('/shoppingCart/add', {
    userId: data.user.id,
    productId: data.product.id,
    // 详情页可以选购买数量，购物车里同一商品会累加
    quantity: data.quantity
  }).then(res => {
    if (res.code === '200') {
      ElMessage.success('已加入购物车')
    } else {
      ElMessage.error(res.msg)
    }
  })
}

// AI 问答：走 RAG 链路，后端先向量检索商品知识切片，再让大模型依据切片作答
const askQuestion = () => {
  if (!checkLogin()) {
    return
  }
  if (!data.qaQuestion.trim()) {
    ElMessage.warning('请先输入你的问题')
    return
  }
  data.qaLoading = true
  request.post('/shoppingQa/ask', {
    userId: data.user.id,
    // PRODUCT 类型走商品知识库检索，后端据此选择 RAG 链路
    questionType: 'PRODUCT',
    questionText: data.qaQuestion,
    productId: data.product.id,
    productName: data.product.name
  }).then(res => {
    if (res.code === '200') {
      data.qaResult = res.data || {}
    } else {
      ElMessage.error(res.msg)
    }
  }).finally(() => {
    data.qaLoading = false
  })
}

// 商品 id 从路由参数拿：/front/product/12
const productId = route.params.id
loadProduct(productId)
loadDetail(productId)
loadParams(productId)
loadReviews(productId)
</script>

<style scoped>
.detail-page {
  /* 电商详情页不铺满整屏，限宽居中，两侧留白 */
  max-width: 1180px;
  margin: 0 auto;
  padding: 16px 16px 48px;
  min-height: 60vh;
}

.detail-back {
  margin-bottom: 10px;
}

.detail-top {
  display: grid;
  grid-template-columns: 400px minmax(0, 1fr);
  gap: 28px;
  padding: 24px;
  border-radius: 10px;
  background: #fff;
  border: 1px solid #eceff5;
}

.detail-cover {
  width: 100%;
  height: 340px;
  border-radius: 8px;
}

.detail-thumbs {
  display: flex;
  gap: 8px;
  margin-top: 10px;
}

.detail-thumb {
  width: 62px;
  height: 62px;
  border-radius: 6px;
  cursor: pointer;
  border: 2px solid transparent;
}

.detail-thumb.active {
  border-color: #f56c6c;
}

.detail-tags {
  display: flex;
  gap: 8px;
}

.detail-info h1 {
  margin: 14px 0 8px;
  font-size: 24px;
  color: #1f2329;
}

.detail-selling {
  margin: 0;
  color: #8a94a6;
  line-height: 1.8;
}

.detail-price {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin: 18px 0;
  padding: 14px 18px;
  border-radius: 8px;
  background: #fff5f5;
}

.detail-price strong {
  font-size: 30px;
  color: #f56c6c;
}

.detail-price span {
  color: #b6bdc9;
  text-decoration: line-through;
}

.detail-meta {
  display: flex;
  gap: 24px;
  color: #8a94a6;
  font-size: 14px;
}

.detail-meta em {
  font-style: normal;
  font-weight: 700;
}

.in-stock {
  color: #67c23a;
}

.no-stock {
  color: #f56c6c;
}

.detail-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 24px;
}

.detail-tabs {
  margin-top: 14px;
  padding: 8px 24px 24px;
  border-radius: 10px;
  background: #fff;
  border: 1px solid #eceff5;
}

.detail-block h3,
.param-group h3 {
  margin: 18px 0 10px;
  font-size: 16px;
  color: #1f2329;
}

.detail-block pre {
  margin: 0;
  color: #4b5563;
  font-family: inherit;
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.9;
}

.param-row {
  display: flex;
  padding: 10px 0;
  border-bottom: 1px dashed #eceff5;
}

.param-name {
  flex: 0 0 180px;
  color: #8a94a6;
}

.param-value {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #1f2329;
}

.review-item {
  padding: 16px 0;
  border-bottom: 1px dashed #eceff5;
}

.review-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.review-head span {
  color: #b6bdc9;
  font-size: 13px;
}

.review-item p {
  margin: 0;
  color: #4b5563;
  line-height: 1.9;
}

.qa-block {
  padding: 8px 0;
}

.qa-tip {
  margin: 0 0 14px;
  color: #8a94a6;
  line-height: 1.9;
}

.qa-quick {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.qa-quick-item {
  cursor: pointer;
}

.qa-input {
  display: flex;
  align-items: flex-end;
  gap: 12px;
}

.qa-answer {
  margin-top: 18px;
  padding: 16px 18px;
  border-radius: 8px;
  background: #f7f9fc;
  border: 1px solid #eceff5;
}

.qa-answer-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.qa-answer-text {
  margin: 0;
  color: #1f2329;
  font-family: inherit;
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.9;
}

.qa-evidence {
  margin: 0;
  color: #8a94a6;
  font-family: inherit;
  font-size: 13px;
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.8;
}

@media (max-width: 900px) {
  .detail-top {
    grid-template-columns: 1fr;
  }
}
</style>
