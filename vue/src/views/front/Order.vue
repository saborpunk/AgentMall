<template>
  <div class="order-page">
    <section class="page-title">
      <h1>我的订单</h1>
      <el-button @click="router.push('/front/home')">继续购物</el-button>
    </section>

    <section class="filter-bar">
      <el-select v-model="data.status" clearable placeholder="全部状态" @change="load" @clear="load">
        <el-option label="待发货" value="PAID"></el-option>
        <el-option label="待收货" value="SHIPPED"></el-option>
        <el-option label="已完成" value="COMPLETED"></el-option>
        <el-option label="已取消" value="CANCELLED"></el-option>
      </el-select>
      <el-button type="primary" @click="load">查询</el-button>
    </section>

    <el-empty v-if="data.list.length === 0" description="还没有订单" />

    <section v-else class="order-list">
      <div class="order-card" v-for="order in data.list" :key="order.id">
        <div class="order-header">
          <div>
            <strong>{{ order.orderNo }}</strong>
            <span>{{ order.createTime }}</span>
          </div>
          <el-tag :type="statusTag(order.status)">{{ statusName(order.status) }}</el-tag>
        </div>
        <div class="item-list">
          <div class="item-row" v-for="item in order.items" :key="item.id">
            <el-image class="cover" :src="item.coverImage" fit="cover"></el-image>
            <div class="item-main">
              <h3>{{ item.productName }}</h3>
              <p>{{ item.productNo }}</p>
            </div>
            <span>¥{{ formatMoney(item.price) }}</span>
            <span>x {{ item.quantity }}</span>
            <strong>¥{{ formatMoney(item.subtotalAmount) }}</strong>
            <el-button v-if="order.status === 'COMPLETED' && item.reviewed !== 1" type="primary" plain @click="openReview(order, item)">评价</el-button>
            <el-tag v-if="order.status === 'COMPLETED' && item.reviewed === 1" type="success">已评价</el-tag>
          </div>
        </div>
        <div class="order-footer">
          <div class="receiver">
            <span>{{ order.receiverName }}</span>
            <span>{{ order.receiverPhone }}</span>
            <span>{{ order.receiverAddress }}</span>
          </div>
          <div class="amount">
            共 {{ order.totalQuantity }} 件，实付 <strong>¥{{ formatMoney(order.totalAmount) }}</strong>
          </div>
          <div class="actions">
            <el-button v-if="order.status === 'PAID'" plain type="danger" @click="cancel(order.id)">取消订单</el-button>
            <el-button v-if="order.status === 'SHIPPED'" type="primary" @click="finish(order.id)">确认收货</el-button>
          </div>
        </div>
      </div>
    </section>

    <el-dialog title="商品评价" v-model="data.reviewVisible" width="520px" destroy-on-close>
      <div class="review-product" v-if="data.currentItem">
        <el-image class="cover" :src="data.currentItem.coverImage" fit="cover"></el-image>
        <div>
          <h3>{{ data.currentItem.productName }}</h3>
          <p>{{ data.currentItem.productNo }}</p>
        </div>
      </div>
      <el-form ref="reviewFormRef" :model="data.reviewForm" :rules="data.reviewRules" label-width="78px">
        <el-form-item label="评分" prop="rating">
          <el-rate v-model="data.reviewForm.rating" :max="5" show-score></el-rate>
        </el-form-item>
        <el-form-item label="评价" prop="content">
          <el-input v-model="data.reviewForm.content" type="textarea" :rows="5" placeholder="请输入商品使用体验、优点或需要改进的地方"></el-input>
        </el-form-item>
        <el-form-item label="图片">
          <el-input v-model="data.reviewForm.images" placeholder="可填写评价图片地址，多个地址用逗号分隔"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="data.reviewVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReview">提交评价</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref } from "vue";
import request from "@/utils/request.js";
import { ElMessage, ElMessageBox } from "element-plus";
import router from "@/router/index.js";

const reviewFormRef = ref()
const data = reactive({
  user: JSON.parse(localStorage.getItem('sys-user') || '{}'),
  status: null,
  list: [],
  reviewVisible: false,
  currentOrder: null,
  currentItem: null,
  reviewForm: {},
  reviewRules: {
    rating: [{ required: true, message: '请选择评分', trigger: 'change' }],
    content: [{ required: true, message: '请输入评价内容', trigger: 'blur' }]
  }
})

const formatMoney = (value) => {
  return Number(value || 0).toFixed(2)
}

const statusName = (status) => {
  const map = { PAID: '待发货', SHIPPED: '待收货', COMPLETED: '已完成', CANCELLED: '已取消' }
  return map[status] || status
}

const statusTag = (status) => {
  const map = { PAID: 'warning', SHIPPED: 'primary', COMPLETED: 'success', CANCELLED: 'info' }
  return map[status] || 'info'
}

const checkLogin = () => {
  if (!data.user.id) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return false
  }
  return true
}

const load = () => {
  if (!checkLogin()) {
    return
  }
  request.get('/shopOrder/selectAll', {
    params: {
      userId: data.user.id,
      status: data.status
    }
  }).then(res => {
    if (res.code === '200') {
      data.list = res.data || []
    }
  })
}

const cancel = (id) => {
  ElMessageBox.confirm('确认取消该订单并退回余额吗？', '取消订单', { type: 'warning' }).then(() => {
    request.put('/shopOrder/cancel/' + id).then(res => {
      if (res.code === '200') {
        ElMessage.success('订单已取消')
        load()
      } else {
        ElMessage.error(res.msg)
      }
    })
  }).catch(() => {})
}

const finish = (id) => {
  request.put('/shopOrder/finish/' + id).then(res => {
    if (res.code === '200') {
      ElMessage.success('确认收货成功')
      load()
    } else {
      ElMessage.error(res.msg)
    }
  })
}

const openReview = (order, item) => {
  data.currentOrder = order
  data.currentItem = item
  data.reviewForm = {
    rating: 5,
    content: '',
    images: ''
  }
  data.reviewVisible = true
}

const submitReview = () => {
  reviewFormRef.value.validate(valid => {
    if (!valid) {
      return
    }
    request.post('/productReview/add', {
      userId: data.user.id,
      orderId: data.currentOrder.id,
      orderItemId: data.currentItem.id,
      productId: data.currentItem.productId,
      rating: data.reviewForm.rating,
      content: data.reviewForm.content,
      images: data.reviewForm.images
    }).then(res => {
      if (res.code === '200') {
        ElMessage.success('评价提交成功')
        data.reviewVisible = false
        load()
      } else {
        ElMessage.error(res.msg)
      }
    })
  })
}

load()
</script>

<style scoped>
.order-page {
  max-width: 1180px;
  margin: 0 auto;
  padding: 24px 0;
}

/* 页面标题栏：一行标题加右侧操作，不再占用整屏高度 */
.page-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 16px 20px;
  border-radius: 8px;
  background: #fff;
  border: 1px solid #e5e7eb;
}

.page-title h1 {
  margin: 0;
  font-size: 20px;
  color: #172033;
}

.filter-bar {
  margin: 18px 0;
  padding: 16px;
  display: flex;
  gap: 12px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.order-list {
  display: grid;
  gap: 14px;
}

.order-card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow: hidden;
}

.order-header,
.order-footer {
  padding: 14px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  background: #f8fafc;
}

.order-header div {
  display: flex;
  align-items: center;
  gap: 12px;
}

.order-header span,
.receiver span {
  color: #64748b;
}

.item-list {
  padding: 4px 16px;
}

.item-row {
  padding: 12px 0;
  display: grid;
  grid-template-columns: 72px 1fr 110px 70px 120px 90px;
  align-items: center;
  gap: 12px;
  border-bottom: 1px solid #f1f5f9;
}

.cover {
  width: 64px;
  height: 64px;
  border-radius: 6px;
}

.item-main h3,
.review-product h3 {
  margin: 0;
  color: #172033;
  font-size: 15px;
}

.item-main p,
.review-product p {
  margin: 6px 0 0;
  color: #94a3b8;
  font-size: 12px;
}

.item-row strong,
.amount strong {
  color: #dc2626;
}

.receiver {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  flex: 1;
}

.amount {
  min-width: 190px;
  text-align: right;
}

.actions {
  min-width: 110px;
  text-align: right;
}

.review-product {
  margin-bottom: 14px;
  padding: 12px;
  display: flex;
  align-items: center;
  gap: 12px;
  background: #f8fafc;
  border-radius: 8px;
}
</style>
