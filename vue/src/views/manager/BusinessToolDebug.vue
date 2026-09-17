<template>
  <div>
    <div class="page-heading">
      <div>
        <h2>业务工具调试</h2>
        <p>调试用户画像、相似商品和订单状态工具，为 AI Agent 导购决策提供结构化业务数据。</p>
      </div>
    </div>

    <div class="card form-card">
      <el-form label-width="96px">
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="用户">
              <!-- 按用户昵称选，选中后绑的是用户 ID，使用者不需要知道 ID -->
              <el-select v-model="data.form.userId" filterable clearable placeholder="请选择用户" style="width: 100%">
                <el-option v-for="item in data.userOptions" :key="item.id" :label="item.name" :value="item.id"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="商品">
              <el-select v-model="data.form.productId" filterable clearable placeholder="请选择商品" style="width: 100%">
                <el-option v-for="item in data.productOptions" :key="item.id" :label="item.name" :value="item.id"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="订单">
              <!-- 按订单编号选，后面拼上下单人和金额方便分辨 -->
              <el-select v-model="data.form.orderId" filterable clearable placeholder="请选择订单" style="width: 100%">
                <el-option
                    v-for="item in data.orderOptions"
                    :key="item.id"
                    :label="item.orderNo + '（' + (item.userName || '') + ' ￥' + item.totalAmount + '）'"
                    :value="item.id"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="返回数量">
              <el-input-number v-model="data.form.limit" :min="1" :max="20" controls-position="right" style="width: 100%"></el-input-number>
            </el-form-item>
          </el-col>
        </el-row>
        <div class="actions">
          <el-button type="primary" @click="callTool('userProfile')">查询用户画像</el-button>
          <el-button type="success" @click="callTool('similarProducts')">查询相似商品</el-button>
          <el-button type="warning" @click="callTool('orderStatus')">查询订单状态</el-button>
          <el-button plain @click="reset">重置</el-button>
        </div>
      </el-form>
    </div>

    <div class="card result-card" v-if="data.result">
      <div class="result-head">
        <div>
          <h3>{{ titleText }}</h3>
          <!-- 本次调用命中的工具：结果里的 toolCode 对应 function_tool 表里登记的工具 -->
          <div class="tool-badge">
            <el-tag type="primary">调用工具：{{ toolName(data.result.toolCode) }}</el-tag>
            <code>{{ data.result.toolCode }}</code>
          </div>
        </div>
        <el-tag>{{ data.result.message }}</el-tag>
      </div>

      <el-descriptions v-if="data.result.toolCode === 'USER_PROFILE_QUERY'" :column="3" border>
        <el-descriptions-item label="用户ID">{{ data.result.userId }}</el-descriptions-item>
        <el-descriptions-item label="账号">{{ data.result.username }}</el-descriptions-item>
        <el-descriptions-item label="姓名">{{ data.result.userName }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ data.result.phone }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ data.result.email }}</el-descriptions-item>
        <el-descriptions-item label="余额">{{ data.result.balance }}</el-descriptions-item>
        <el-descriptions-item label="订单数量">{{ data.result.orderCount }}</el-descriptions-item>
        <el-descriptions-item label="订单金额">{{ data.result.orderAmount }}</el-descriptions-item>
        <el-descriptions-item label="最近订单">{{ data.result.latestOrderNo }}</el-descriptions-item>
        <el-descriptions-item label="最近状态">{{ data.result.latestOrderStatus }}</el-descriptions-item>
      </el-descriptions>

      <div v-if="data.result.toolCode === 'SIMILAR_PRODUCT_QUERY'">
        <el-descriptions :column="3" border>
          <el-descriptions-item label="基准商品ID">{{ data.result.productId }}</el-descriptions-item>
          <el-descriptions-item label="商品编号">{{ data.result.productNo }}</el-descriptions-item>
          <el-descriptions-item label="商品名称">{{ data.result.productName }}</el-descriptions-item>
          <el-descriptions-item label="分类ID">{{ data.result.categoryId }}</el-descriptions-item>
          <el-descriptions-item label="品牌ID">{{ data.result.brandId }}</el-descriptions-item>
        </el-descriptions>
        <el-table :data="data.result.similarProducts || []" border style="width: 100%; margin-top: 14px">
          <el-table-column prop="id" label="ID" width="70"></el-table-column>
          <el-table-column prop="productNo" label="商品编号" width="150"></el-table-column>
          <el-table-column prop="name" label="商品名称"></el-table-column>
          <el-table-column prop="price" label="价格" width="110"></el-table-column>
          <el-table-column prop="status" label="状态" width="110"></el-table-column>
          <el-table-column prop="isRecommend" label="推荐" width="90">
            <template #default="scope">
              <el-tag :type="scope.row.isRecommend === 1 ? 'success' : 'info'">
                {{ scope.row.isRecommend === 1 ? '是' : '否' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <el-descriptions v-if="data.result.toolCode === 'ORDER_STATUS_QUERY'" :column="3" border>
        <el-descriptions-item label="订单ID">{{ data.result.orderId }}</el-descriptions-item>
        <el-descriptions-item label="订单编号">{{ data.result.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="订单状态">{{ data.result.orderStatus }}</el-descriptions-item>
        <el-descriptions-item label="用户ID">{{ data.result.userId }}</el-descriptions-item>
        <el-descriptions-item label="用户姓名">{{ data.result.userName }}</el-descriptions-item>
        <el-descriptions-item label="订单金额">{{ data.result.totalAmount }}</el-descriptions-item>
        <el-descriptions-item label="商品数量">{{ data.result.totalQuantity }}</el-descriptions-item>
        <el-descriptions-item label="收货人">{{ data.result.receiverName }}</el-descriptions-item>
        <el-descriptions-item label="收货电话">{{ data.result.receiverPhone }}</el-descriptions-item>
        <el-descriptions-item label="收货地址">{{ data.result.receiverAddress }}</el-descriptions-item>
        <el-descriptions-item label="支付时间">{{ data.result.payTime }}</el-descriptions-item>
        <el-descriptions-item label="发货时间">{{ data.result.shipTime }}</el-descriptions-item>
        <el-descriptions-item label="完成时间">{{ data.result.finishTime }}</el-descriptions-item>
        <el-descriptions-item label="取消时间">{{ data.result.cancelTime }}</el-descriptions-item>
      </el-descriptions>

      <h4>原始 JSON</h4>
      <pre>{{ JSON.stringify(data.result, null, 2) }}</pre>
    </div>

    <el-empty v-else description="请选择一种业务工具进行调试"></el-empty>
  </div>
</template>

<script setup>
import { computed, reactive } from "vue";
import request from "@/utils/request.js";
import { ElMessage } from "element-plus";

const data = reactive({
  form: {
    // 三个 ID 都由下拉选出来，页面上只出现昵称、商品名和订单编号
    userId: null,
    productId: null,
    orderId: null,
    limit: 5
  },
  // 三个下拉的数据源
  userOptions: [],
  productOptions: [],
  orderOptions: [],
  // 工具登记表，用来把 toolCode 翻译成中文工具名
  toolOptions: [],
  result: null
})

// 用户下拉：展示姓名（昵称），值是用户 ID
const loadUsers = () => {
  request.get('/user/selectAll').then(res => {
    if (res.code === '200') data.userOptions = res.data || []
  })
}

// 商品下拉：只取上架商品
const loadProducts = () => {
  request.get('/product/selectAll', { params: { status: 'ON_SALE' } }).then(res => {
    if (res.code === '200') data.productOptions = res.data || []
  })
}

// 订单下拉：展示订单编号，值是订单 ID
const loadOrders = () => {
  request.get('/shopOrder/selectAll').then(res => {
    if (res.code === '200') data.orderOptions = res.data || []
  })
}

// 拉工具中心登记的工具，供结果区展示"这次调用的是哪个 Function Tool"
const loadTools = () => {
  request.get('/functionTool/selectPage', { params: { pageNum: 1, pageSize: 100 } }).then(res => {
    if (res.code === '200') data.toolOptions = res.data?.list || []
  })
}

// toolCode 翻译成中文工具名，登记表里没有就原样显示编码
const toolName = (toolCode) => {
  const tool = data.toolOptions.find(item => item.toolCode === toolCode)
  return tool ? tool.toolName : toolCode
}

const titleText = computed(() => {
  if (!data.result) {
    return '业务工具结果'
  }
  if (data.result.toolCode === 'USER_PROFILE_QUERY') {
    return data.result.userName || data.result.username || '用户画像'
  }
  if (data.result.toolCode === 'SIMILAR_PRODUCT_QUERY') {
    return data.result.productName || '相似商品'
  }
  if (data.result.toolCode === 'ORDER_STATUS_QUERY') {
    return data.result.orderNo || '订单状态'
  }
  return '业务工具结果'
})

const callTool = (type) => {
  if (type === 'userProfile' && !data.form.userId) {
    ElMessage.warning('请先选择用户')
    return
  }
  if (type === 'similarProducts' && !data.form.productId) {
    ElMessage.warning('请先选择商品')
    return
  }
  if (type === 'orderStatus' && !data.form.orderId) {
    ElMessage.warning('请先选择订单')
    return
  }
  request.post('/businessTool/' + type, data.form).then(res => {
    if (res.code === '200') {
      data.result = res.data
      ElMessage.success('工具调用成功')
    } else {
      ElMessage.error(res.msg)
    }
  })
}

const reset = () => {
  data.form = {
    userId: null,
    productId: null,
    orderId: null,
    limit: 5
  }
  data.result = null
}

loadUsers()
loadProducts()
loadOrders()
loadTools()
</script>

<style scoped>
.tool-badge {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
}

.tool-badge code {
  padding: 2px 8px;
  border-radius: 4px;
  background: #f1f5f9;
  color: #64748b;
  font-size: 12px;
}

.page-heading {
  margin-bottom: 12px;
  padding: 20px 22px;
  border-radius: 8px;
  background: #fff;
  border: 1px solid #e5e7eb;
}

.page-heading h2,
.result-head h3,
h4 {
  margin: 0;
  color: #172033;
}

.page-heading p,
.result-head p {
  margin: 8px 0 0;
  color: #64748b;
}

.card {
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  padding: 16px;
}

.form-card {
  margin-bottom: 12px;
}

.actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}

.result-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 14px;
}

h4 {
  margin-top: 18px;
}

pre {
  margin-top: 10px;
  padding: 14px;
  color: #334155;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
  background: #f8fafc;
  border-radius: 8px;
}
</style>
