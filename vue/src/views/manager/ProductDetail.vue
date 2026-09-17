<template>
  <div>
    <div class="page-heading">
      <div>
        <h2>商品详情与参数</h2>
        <p>维护商品详情、包装售后、AI 摘要和结构化参数，为前台详情页和 RAG 商品知识库提供资料。</p>
      </div>
      <el-button type="primary" @click="saveDetail">保存详情</el-button>
    </div>

    <div class="detail-layout">
      <div class="card product-panel">
        <div class="panel-title">选择商品</div>
        <el-input v-model="data.productName" clearable @keyup.enter="loadProducts" @clear="loadProducts" prefix-icon="Search" placeholder="搜索商品名称"></el-input>
        <div class="product-list">
          <div v-for="item in data.products" :key="item.id" class="product-item" :class="{ active: data.currentProduct?.id === item.id }" @click="selectProduct(item)">
            <el-image class="product-thumb" v-if="item.coverImage" :src="item.coverImage"></el-image>
            <div class="product-info">
              <div class="product-name">{{ item.name }}</div>
              <div class="product-meta">{{ item.categoryName }} · {{ item.brandName }}</div>
              <div class="product-price">￥{{ formatMoney(item.price) }}</div>
            </div>
          </div>
        </div>
      </div>

      <div class="detail-main">
        <div class="card empty-card" v-if="!data.currentProduct">
          <el-empty description="请选择一个商品维护详情和参数"></el-empty>
        </div>

        <template v-else>
          <div class="card selected-card">
            <div>
              <div class="selected-title">{{ data.currentProduct.name }}</div>
              <div class="selected-meta">{{ data.currentProduct.productNo }} · {{ data.currentProduct.categoryName }} · {{ data.currentProduct.brandName }}</div>
            </div>
            <el-tag :type="data.currentProduct.status === 'ON_SALE' ? 'success' : 'info'">{{ data.currentProduct.status === 'ON_SALE' ? '上架' : '下架' }}</el-tag>
          </div>

          <div class="card form-card">
            <el-form ref="detailFormRef" :model="data.detailForm" :rules="data.detailRules" label-width="92px">
              <el-form-item prop="detailContent" label="商品详情">
                <el-input v-model="data.detailForm.detailContent" type="textarea" :rows="7" placeholder="请输入商品详情，可写核心卖点、适用人群、使用场景和购买建议"></el-input>
              </el-form-item>
              <el-form-item prop="packageInfo" label="包装清单">
                <el-input v-model="data.detailForm.packageInfo" type="textarea" :rows="3" placeholder="请输入包装清单，例如 主机、电源适配器、说明书"></el-input>
              </el-form-item>
              <el-form-item prop="afterSaleInfo" label="售后说明">
                <el-input v-model="data.detailForm.afterSaleInfo" type="textarea" :rows="3" placeholder="请输入售后说明，例如 7 天无理由、质保范围、退换条件"></el-input>
              </el-form-item>
              <el-form-item prop="aiSummary" label="AI摘要">
                <el-input v-model="data.detailForm.aiSummary" type="textarea" :rows="3" placeholder="请输入面向 AI 检索和推荐的商品摘要"></el-input>
              </el-form-item>
            </el-form>
          </div>

          <div class="card param-card">
            <div class="param-header">
              <div>
                <h3>商品参数</h3>
                <p>把商品规格拆成结构化参数，后续可用于筛选、对比和 Function Calling 查询。</p>
              </div>
              <el-button type="primary" @click="handleParamAdd">新增参数</el-button>
            </div>
            <el-table stripe :data="data.params" @selection-change="handleParamSelectionChange">
              <el-table-column type="selection" width="55" />
              <el-table-column prop="paramGroup" label="参数分组" min-width="120" />
              <el-table-column prop="paramName" label="参数名称" min-width="140" />
              <el-table-column prop="paramValue" label="参数值" min-width="220" show-overflow-tooltip />
              <el-table-column prop="isCore" label="核心参数" width="100">
                <template v-slot="scope">
                  <el-tag :type="scope.row.isCore === 1 ? 'warning' : 'info'">{{ scope.row.isCore === 1 ? '核心' : '普通' }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="sort" label="排序" width="80" />
              <el-table-column label="操作" width="120" fixed="right">
                <template v-slot="scope">
                  <el-button type="primary" circle :icon="Edit" @click="handleParamEdit(scope.row)"></el-button>
                  <el-button type="danger" circle :icon="Delete" @click="delParam(scope.row.id)"></el-button>
                </template>
              </el-table-column>
            </el-table>
            <div class="param-actions">
              <el-button type="danger" plain @click="delParamBatch">批量删除</el-button>
            </div>
          </div>
        </template>
      </div>
    </div>

    <el-dialog title="商品参数" v-model="data.paramVisible" width="560px" destroy-on-close>
      <el-form ref="paramFormRef" :model="data.paramForm" :rules="data.paramRules" label-width="86px" style="padding: 20px">
        <el-form-item prop="paramGroup" label="参数分组">
          <el-input v-model="data.paramForm.paramGroup" placeholder="例如 基础信息、性能参数、包装售后"></el-input>
        </el-form-item>
        <el-form-item prop="paramName" label="参数名称">
          <el-input v-model="data.paramForm.paramName" placeholder="例如 屏幕尺寸、处理器、适用场景"></el-input>
        </el-form-item>
        <el-form-item prop="paramValue" label="参数值">
          <el-input v-model="data.paramForm.paramValue" type="textarea" :rows="3" placeholder="请输入参数值"></el-input>
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item prop="sort" label="排序">
              <el-input-number v-model="data.paramForm.sort" :min="1" :max="999" style="width: 100%"></el-input-number>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item prop="isCore" label="核心参数">
              <el-radio-group v-model="data.paramForm.isCore">
                <el-radio-button :label="1">核心</el-radio-button>
                <el-radio-button :label="0">普通</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="data.paramVisible = false">取消</el-button>
          <el-button type="primary" @click="saveParam">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref } from "vue";
import request from "@/utils/request.js";
import {ElMessage, ElMessageBox} from "element-plus";
import {Delete, Edit} from "@element-plus/icons-vue";

const detailFormRef = ref()
const paramFormRef = ref()

const data = reactive({
  productName: null,
  products: [],
  currentProduct: null,
  detailForm: {},
  params: [],
  paramVisible: false,
  paramForm: {},
  paramIds: [],
  detailRules: {
    detailContent: [{ required: true, message: '请输入商品详情', trigger: 'blur' }]
  },
  paramRules: {
    paramGroup: [{ required: true, message: '请输入参数分组', trigger: 'blur' }],
    paramName: [{ required: true, message: '请输入参数名称', trigger: 'blur' }],
    paramValue: [{ required: true, message: '请输入参数值', trigger: 'blur' }]
  }
})

const formatMoney = (value) => {
  return Number(value || 0).toFixed(2)
}

const loadProducts = () => {
  request.get('/product/selectAll', {
    params: {
      name: data.productName,
      status: 'ON_SALE'
    }
  }).then(res => {
    if (res.code === '200') {
      data.products = res.data || []
      if (!data.currentProduct && data.products.length) {
        selectProduct(data.products[0])
      }
    }
  })
}

const selectProduct = (product) => {
  data.currentProduct = product
  loadDetail()
  loadParams()
}

const loadDetail = () => {
  request.get('/productDetail/selectByProductId/' + data.currentProduct.id).then(res => {
    if (res.code === '200' && res.data) {
      data.detailForm = res.data
    } else {
      data.detailForm = {
        productId: data.currentProduct.id,
        detailContent: '',
        packageInfo: '',
        afterSaleInfo: '',
        aiSummary: ''
      }
    }
  })
}

const saveDetail = () => {
  if (!data.currentProduct) {
    ElMessage.warning('请先选择商品')
    return
  }
  detailFormRef.value.validate(valid => {
    if (valid) {
      data.detailForm.productId = data.currentProduct.id
      request.post('/productDetail/save', data.detailForm).then(res => {
        if (res.code === '200') {
          ElMessage.success('保存成功')
          loadDetail()
        } else {
          ElMessage.error(res.msg)
        }
      })
    }
  })
}

const loadParams = () => {
  request.get('/productParam/selectAll', {
    params: {
      productId: data.currentProduct.id
    }
  }).then(res => {
    if (res.code === '200') {
      data.params = res.data || []
    }
  })
}

const handleParamAdd = () => {
  data.paramForm = {
    productId: data.currentProduct.id,
    paramGroup: '基础信息',
    sort: 1,
    isCore: 1
  }
  data.paramVisible = true
}

const handleParamEdit = (row) => {
  data.paramForm = JSON.parse(JSON.stringify(row))
  data.paramVisible = true
}

const saveParam = () => {
  paramFormRef.value.validate(valid => {
    if (valid) {
      data.paramForm.productId = data.currentProduct.id
      const requestTask = data.paramForm.id ? request.put('/productParam/update', data.paramForm) : request.post('/productParam/add', data.paramForm)
      requestTask.then(res => {
        if (res.code === '200') {
          ElMessage.success('操作成功')
          data.paramVisible = false
          loadParams()
        } else {
          ElMessage.error(res.msg)
        }
      })
    }
  })
}

const delParam = (id) => {
  ElMessageBox.confirm('删除后数据无法恢复，您确定删除吗？', '删除确认', { type: 'warning' }).then(() => {
    request.delete('/productParam/delete/' + id).then(res => {
      if (res.code === '200') {
        ElMessage.success('删除成功')
        loadParams()
      } else {
        ElMessage.error(res.msg)
      }
    })
  })
}

const delParamBatch = () => {
  if (!data.paramIds.length) {
    ElMessage.warning('请选择数据')
    return
  }
  ElMessageBox.confirm('删除后数据无法恢复，您确定删除吗？', '删除确认', { type: 'warning' }).then(() => {
    request.delete('/productParam/delete/batch', {data: data.paramIds}).then(res => {
      if (res.code === '200') {
        ElMessage.success('操作成功')
        loadParams()
      } else {
        ElMessage.error(res.msg)
      }
    })
  })
}

const handleParamSelectionChange = (rows) => {
  data.paramIds = rows.map(v => v.id)
}

loadProducts()
</script>

<style scoped>
.page-heading {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20px;
  margin-bottom: 12px;
  padding: 20px 22px;
  border-radius: 8px;
  background: #fff;
  border: 1px solid #e5e7eb;
}

.page-heading h2 {
  margin: 0;
  font-size: 22px;
  color: #172033;
}

.page-heading p {
  margin: 8px 0 0;
  color: #64748b;
}

.detail-layout {
  display: grid;
  grid-template-columns: 330px 1fr;
  gap: 12px;
  align-items: start;
}

.product-panel {
  min-height: 680px;
}

.panel-title {
  font-weight: 700;
  color: #172033;
  margin-bottom: 12px;
}

.product-list {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 610px;
  overflow-y: auto;
}

.product-item {
  display: flex;
  gap: 10px;
  padding: 10px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  cursor: pointer;
  background: #fff;
}

.product-item.active {
  border-color: #2563eb;
  background: #eff6ff;
}

.product-thumb {
  width: 58px;
  height: 58px;
  border-radius: 8px;
  flex: 0 0 auto;
}

.product-info {
  min-width: 0;
}

.product-name {
  font-weight: 700;
  color: #172033;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-meta {
  margin-top: 5px;
  color: #64748b;
  font-size: 13px;
}

.product-price {
  margin-top: 5px;
  color: #0f766e;
  font-weight: 700;
}

.detail-main {
  min-width: 0;
}

.empty-card {
  min-height: 420px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.selected-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.selected-title {
  font-size: 18px;
  color: #172033;
  font-weight: 700;
}

.selected-meta {
  margin-top: 6px;
  color: #64748b;
}

.form-card {
  margin-bottom: 12px;
}

.param-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20px;
  margin-bottom: 12px;
}

.param-header h3 {
  margin: 0;
  color: #172033;
}

.param-header p {
  margin: 6px 0 0;
  color: #64748b;
}

.param-actions {
  margin-top: 12px;
}
</style>
