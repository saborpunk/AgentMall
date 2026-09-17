<template>
  <div class="favorite-page">
    <section class="page-title">
      <h1>我的收藏</h1>
      <el-button @click="router.push('/front/home')">继续逛商城</el-button>
    </section>

    <section class="toolbar">
      <el-input v-model="data.productName" clearable prefix-icon="Search" placeholder="搜索收藏商品" @keyup.enter="load" @clear="load"></el-input>
      <el-button type="primary" @click="load">查询</el-button>
    </section>

    <el-empty v-if="data.list.length === 0" description="还没有收藏商品" />

    <section v-else class="favorite-grid">
      <div class="favorite-card" v-for="item in data.list" :key="item.id">
        <el-image class="cover" :src="item.coverImage" fit="cover"></el-image>
        <div class="info">
          <div class="tags">
            <el-tag size="small" type="info">{{ item.categoryName }}</el-tag>
            <el-tag size="small">{{ item.brandName }}</el-tag>
          </div>
          <h3>{{ item.productName }}</h3>
          <p>{{ item.sellingPoint }}</p>
          <div class="price-row">
            <strong>¥{{ formatMoney(item.price) }}</strong>
            <span>{{ item.productNo }}</span>
          </div>
          <div class="actions">
            <el-button type="primary" @click="addCart(item)">加入购物车</el-button>
            <el-button plain @click="remove(item.id)">取消收藏</el-button>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { reactive } from "vue";
import request from "@/utils/request.js";
import { ElMessage, ElMessageBox } from "element-plus";
import router from "@/router/index.js";

const data = reactive({
  user: JSON.parse(localStorage.getItem('sys-user') || '{}'),
  productName: null,
  list: []
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

const load = () => {
  if (!checkLogin()) {
    return
  }
  request.get('/productFavorite/selectAll', {
    params: {
      userId: data.user.id,
      productName: data.productName
    }
  }).then(res => {
    if (res.code === '200') {
      data.list = res.data || []
    }
  })
}

const addCart = (item) => {
  request.post('/shoppingCart/add', {
    userId: data.user.id,
    productId: item.productId,
    quantity: 1
  }).then(res => {
    if (res.code === '200') {
      ElMessage.success('已加入购物车')
    } else {
      ElMessage.error(res.msg)
    }
  })
}

const remove = (id) => {
  ElMessageBox.confirm('确认取消收藏该商品吗？', '取消收藏', { type: 'warning' }).then(() => {
    request.delete('/productFavorite/delete/' + id).then(res => {
      if (res.code === '200') {
        ElMessage.success('已取消收藏')
        load()
      } else {
        ElMessage.error(res.msg)
      }
    })
  }).catch(() => {})
}

load()
</script>

<style scoped>
.favorite-page {
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

.toolbar {
  margin: 18px 0;
  display: flex;
  gap: 12px;
  padding: 16px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.favorite-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.favorite-card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow: hidden;
}

.cover {
  width: 100%;
  height: 210px;
  display: block;
}

.info {
  padding: 14px;
}

.tags {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
}

.info h3 {
  height: 44px;
  margin: 0;
  color: #172033;
  line-height: 22px;
  font-size: 16px;
  overflow: hidden;
}

.info p {
  height: 42px;
  margin: 8px 0 0;
  color: #64748b;
  line-height: 21px;
  overflow: hidden;
}

.price-row {
  margin-top: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.price-row strong {
  color: #dc2626;
  font-size: 20px;
}

.price-row span {
  color: #94a3b8;
  font-size: 12px;
}

.actions {
  margin-top: 14px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}
</style>
