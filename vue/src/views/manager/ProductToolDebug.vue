<template>
  <div>
    <div class="page-heading">
      <div>
        <h2>商品工具调试</h2>
        <p>调试商品价格、库存和优惠查询工具，验证 Function Calling 调用前后的结构化入参和出参。</p>
      </div>
    </div>

    <div class="card form-card">
      <el-form label-width="92px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="商品">
              <!-- 按商品名称选，选中后绑的是商品 ID，使用者不需要知道 ID -->
              <el-select v-model="data.form.productId" filterable clearable placeholder="请选择商品" style="width: 100%">
                <el-option v-for="item in data.productOptions" :key="item.id" :label="item.name" :value="item.id"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="购买数量">
              <el-input-number v-model="data.form.quantity" :min="1" :max="999" controls-position="right" style="width: 100%"></el-input-number>
            </el-form-item>
          </el-col>
        </el-row>
        <div class="actions">
          <el-button type="primary" @click="callTool('price')">查询价格</el-button>
          <el-button type="success" @click="callTool('stock')">查询库存</el-button>
          <el-button type="warning" @click="callTool('promotion')">查询优惠</el-button>
          <el-button plain @click="reset">重置</el-button>
        </div>
      </el-form>
    </div>

    <div class="card result-card" v-if="data.result">
      <div class="result-head">
        <div>
          <h3>{{ data.result.productName }}</h3>
          <p>商品编号：{{ data.result.productNo }}</p>
          <!-- 本次调用命中的工具：结果里的 toolCode 对应 function_tool 表里登记的工具 -->
          <div class="tool-badge">
            <el-tag type="primary">调用工具：{{ toolName(data.result.toolCode) }}</el-tag>
            <code>{{ data.result.toolCode }}</code>
          </div>
        </div>
        <el-tag :type="data.result.canBuy === 1 ? 'success' : 'danger'">
          {{ data.result.canBuy === 1 ? '可购买' : '不可购买' }}
        </el-tag>
      </div>
      <el-descriptions :column="3" border>
        <el-descriptions-item label="现价">{{ data.result.price }}</el-descriptions-item>
        <el-descriptions-item label="原价">{{ data.result.originalPrice }}</el-descriptions-item>
        <el-descriptions-item label="购买数量">{{ data.result.quantity }}</el-descriptions-item>
        <el-descriptions-item label="总金额">{{ data.result.totalAmount }}</el-descriptions-item>
        <el-descriptions-item label="优惠金额">{{ data.result.discountAmount }}</el-descriptions-item>
        <el-descriptions-item label="折扣率">{{ data.result.discountRate }}</el-descriptions-item>
        <el-descriptions-item label="库存数量">{{ data.result.stockQuantity }}</el-descriptions-item>
        <el-descriptions-item label="商品状态">{{ data.result.status }}</el-descriptions-item>
        <el-descriptions-item label="消息">{{ data.result.message }}</el-descriptions-item>
      </el-descriptions>
      <h4>原始 JSON</h4>
      <pre>{{ JSON.stringify(data.result, null, 2) }}</pre>
    </div>

    <el-empty v-else description="请选择一种商品工具进行调试"></el-empty>
  </div>
</template>

<script setup>
import { reactive } from "vue";
import request from "@/utils/request.js";
import { ElMessage } from "element-plus";

const data = reactive({
  form: {
    // 只保留商品 ID（由下拉选出来）和购买数量两个入参
    productId: null,
    quantity: 1
  },
  // 商品下拉选项，按名称展示
  productOptions: [],
  // 工具登记表，用来把 toolCode 翻译成中文工具名
  toolOptions: [],
  result: null
})

// 拉上架商品填下拉，使用者按名称选而不是手填 ID
const loadProducts = () => {
  request.get('/product/selectAll', { params: { status: 'ON_SALE' } }).then(res => {
    if (res.code === '200') data.productOptions = res.data || []
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

const callTool = (type) => {
  if (!data.form.productId) {
    ElMessage.warning('请先选择要调试的商品')
    return
  }
  request.post('/productTool/' + type, data.form).then(res => {
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
    productId: null,
    quantity: 1
  }
  data.result = null
}

loadProducts()
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
