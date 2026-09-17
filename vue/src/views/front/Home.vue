<template>
  <div class="shop-home">
    <section class="banner">
      <div class="banner-text">
        <h1>好物精选，正品保障</h1>
        <p>全场包邮 · 七天无理由退换 · 余额支付立减</p>
      </div>
      <!-- 挑不出来的时候可以让 AI 帮忙推荐，作为一个普通入口放在 banner 里 -->
      <el-button class="banner-btn" @click="router.push('/front/guide')">挑花眼了？让 AI 帮我选 →</el-button>
    </section>

    <!-- 分类直接平铺成一行可点击的标签，点谁就筛谁，选中项高亮 -->
    <section class="category-nav" ref="productSection">
      <span
          :class="['category-item', data.categoryId === null ? 'active' : '']"
          @click="selectCategory(null)"
      >全部商品</span>
      <span
          v-for="item in data.categories"
          :key="item.id"
          :class="['category-item', data.categoryId === item.id ? 'active' : '']"
          @click="selectCategory(item.id)"
      >{{ item.name }}</span>
    </section>

    <section class="toolbar">
      <el-input v-model="data.name" clearable @keyup.enter="loadProducts" @clear="loadProducts" prefix-icon="Search" placeholder="搜索商品名称、卖点或标签"></el-input>
      <el-select v-model="data.brandId" clearable filterable placeholder="全部品牌" @change="loadProducts" @clear="loadProducts">
        <el-option v-for="item in data.brands" :key="item.id" :label="item.name" :value="item.id"></el-option>
      </el-select>
      <el-button type="primary" @click="loadProducts">搜索</el-button>
    </section>

    <el-empty v-if="!data.products.length" description="没有找到符合条件的商品" />

    <section class="product-grid" v-else>
      <div class="product-card" v-for="item in data.products" :key="item.id">
        <!-- 点封面进商品详情页 -->
        <el-image class="product-image" :src="item.coverImage" fit="cover" @click="goDetail(item)"></el-image>
        <div class="product-content">
          <div class="product-tags">
            <el-tag size="small" type="success" v-if="item.isRecommend === 1">推荐</el-tag>
            <el-tag size="small" type="info">{{ item.categoryName }}</el-tag>
          </div>
          <!-- 点标题同样进详情页 -->
          <h3 class="product-name" @click="goDetail(item)">{{ item.name }}</h3>
          <p>{{ item.sellingPoint }}</p>
          <div class="product-meta">
            <span>{{ item.brandName }}</span>
            <strong>¥{{ formatMoney(item.price) }}</strong>
          </div>
          <div class="product-actions">
            <el-button plain @click="addFavorite(item)">收藏</el-button>
            <el-button type="primary" @click="addCart(item)">加入购物车</el-button>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { reactive, ref } from "vue";
import request from "@/utils/request.js";
import { ElMessage } from "element-plus";
import router from "@/router/index.js";

const productSection = ref()
const data = reactive({
  user: JSON.parse(localStorage.getItem('sys-user') || '{}'),
  products: [],
  categories: [],
  brands: [],
  name: null,
  categoryId: null,
  brandId: null
})

const formatMoney = (value) => {
  return Number(value || 0).toFixed(2)
}

// 进入商品详情页，商品 id 放在路径上
const goDetail = (product) => {
  router.push('/front/product/' + product.id)
}

// 点分类标签直接切换筛选条件并重新查商品，不需要再点一次搜索
const selectCategory = (categoryId) => {
  data.categoryId = categoryId
  loadProducts()
}

const checkLogin = () => {
  if (!data.user.id) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return false
  }
  return true
}

const loadOptions = () => {
  request.get('/productCategory/selectAll', {
    params: { isEnabled: 1 }
  }).then(res => {
    if (res.code === '200') {
      data.categories = res.data || []
    }
  })
  request.get('/productBrand/selectAll', {
    params: { isEnabled: 1 }
  }).then(res => {
    if (res.code === '200') {
      data.brands = res.data || []
    }
  })
}

const loadProducts = () => {
  request.get('/product/selectAll', {
    params: {
      name: data.name,
      categoryId: data.categoryId,
      brandId: data.brandId,
      status: 'ON_SALE'
    }
  }).then(res => {
    if (res.code === '200') {
      data.products = res.data || []
    }
  })
}

const addFavorite = (product) => {
  if (!checkLogin()) {
    return
  }
  request.post('/productFavorite/add', {
    userId: data.user.id,
    productId: product.id
  }).then(res => {
    if (res.code === '200') {
      ElMessage.success('收藏成功')
    } else {
      ElMessage.error(res.msg)
    }
  })
}

const addCart = (product) => {
  if (!checkLogin()) {
    return
  }
  request.post('/shoppingCart/add', {
    userId: data.user.id,
    productId: product.id,
    quantity: 1
  }).then(res => {
    if (res.code === '200') {
      ElMessage.success('已加入购物车')
    } else {
      ElMessage.error(res.msg)
    }
  })
}

loadOptions()
loadProducts()
</script>

<style scoped>
.shop-home {
  max-width: 1180px;
  margin: 0 auto;
  padding: 24px 0;
}

.banner {
  padding: 30px 36px;
  border-radius: 8px;
  /* 电商促销条常用的暖色渐变，比大片留白更有购物氛围 */
  background: linear-gradient(120deg, #ff6a5e 0%, #ff9068 100%);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
}

.banner-text h1 {
  margin: 0;
  font-size: 28px;
  color: #fff;
  letter-spacing: 2px;
}

.banner-text p {
  margin: 10px 0 0;
  color: rgba(255, 255, 255, 0.9);
  font-size: 14px;
}

.banner-btn {
  flex-shrink: 0;
  border: none;
  color: #f5533d;
  font-weight: 600;
}

.category-nav {
  margin-top: 16px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 12px 16px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.category-item {
  padding: 6px 16px;
  border-radius: 16px;
  color: #4b5563;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.category-item:hover {
  color: #f5533d;
  background: #fff5f4;
}

/* 当前选中的分类用实心底色标出来，一眼知道正在看哪一类 */
.category-item.active {
  color: #fff;
  background: #f5533d;
}

.toolbar {
  margin-top: 12px;
  display: flex;
  gap: 12px;
  padding: 16px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.toolbar .el-input {
  flex: 1;
}

.toolbar .el-select {
  width: 180px;
}

.product-grid {
  margin-top: 18px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.product-card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow: hidden;
}

.product-image {
  cursor: pointer;
  width: 100%;
  height: 190px;
  display: block;
}

.product-content {
  padding: 14px;
}

.product-tags {
  display: flex;
  gap: 6px;
  margin-bottom: 8px;
}

.product-name {
  cursor: pointer;
}

.product-name:hover {
  color: #f56c6c;
}

.product-content h3 {
  margin: 0;
  height: 44px;
  line-height: 22px;
  font-size: 16px;
  color: #172033;
  overflow: hidden;
}

.product-content p {
  height: 40px;
  margin: 8px 0 0;
  color: #64748b;
  line-height: 20px;
  overflow: hidden;
}

.product-meta {
  margin-top: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.product-meta span {
  color: #64748b;
}

.product-meta strong {
  color: #f5533d;
  font-size: 18px;
}

.product-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin-top: 12px;
}
</style>
