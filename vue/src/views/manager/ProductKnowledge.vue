<template>
  <div>
    <div class="page-heading">
      <div>
        <h2>商品知识库</h2>
        <p>维护商品 FAQ、卖点、参数、售后和评价摘要，为后续 RAG 切片与 AI 导购问答提供可信资料。</p>
      </div>
      <div>
        <!-- 商品详情和规格参数本来只在前台展示，导入后才能被切片、向量化和检索到 -->
        <el-button plain @click="handleImport">从商品详情导入</el-button>
        <el-button type="primary" @click="handleAdd">新增资料</el-button>
      </div>
    </div>

    <el-dialog title="从商品详情导入资料" v-model="data.importVisible" width="560px" destroy-on-close>
      <div v-loading="data.importLoading" element-loading-text="正在导入，请稍候…" style="padding: 10px 20px">
        <p class="import-tip">
          系统会把商品的<strong>详情介绍、包装清单、售后说明</strong>各生成一条资料，
          并把<strong>规格参数按分组</strong>各生成一条资料。生成后还需要依次做「生成切片」和「生成向量」，AI 才能检索到这些内容。
        </p>

        <!-- 两种导入范围：单个商品用于补录，全部商品用于初始化整个知识库 -->
        <el-radio-group v-model="data.importScope" style="margin-bottom: 12px">
          <el-radio label="ONE">导入指定商品</el-radio>
          <el-radio label="ALL">一键导入全部商品</el-radio>
        </el-radio-group>

        <el-select
            v-if="data.importScope === 'ONE'"
            v-model="data.importProductId"
            filterable
            placeholder="请选择商品"
            style="width: 100%">
          <el-option v-for="item in data.products" :key="item.id" :label="item.name" :value="item.id"></el-option>
        </el-select>
        <p v-else class="import-tip">
          将对全部 {{ data.products.length }} 个商品逐个导入。没有维护详情和参数的商品会自动跳过。
        </p>

        <p class="import-tip warn">
          重复导入会<strong>覆盖</strong>上次导入的资料（连同切片和向量），你手工新增的资料不受影响。
        </p>
      </div>
      <template #footer>
        <el-button :disabled="data.importLoading" @click="data.importVisible = false">取 消</el-button>
        <el-button type="primary" :loading="data.importLoading" @click="confirmImport">确 定 导 入</el-button>
      </template>
    </el-dialog>

    <div class="card search-card">
      <el-input v-model="data.productName" clearable @keyup.enter="load" @clear="reset" prefix-icon="Search" class="search-item" placeholder="请输入商品名称"></el-input>
      <el-input v-model="data.title" clearable @keyup.enter="load" @clear="reset" prefix-icon="Search" class="search-item" placeholder="请输入资料标题"></el-input>
      <el-select v-model="data.knowledgeType" clearable placeholder="知识类型" class="search-item" @change="load" @clear="load">
        <el-option label="常见问答" value="FAQ"></el-option>
        <el-option label="卖点说明" value="SELLING_POINT"></el-option>
        <el-option label="参数说明" value="PARAMETER"></el-option>
        <el-option label="售后说明" value="AFTER_SALE"></el-option>
        <el-option label="评价摘要" value="REVIEW_SUMMARY"></el-option>
      </el-select>
      <el-select v-model="data.sourceType" clearable placeholder="来源类型" class="search-item" @change="load" @clear="load">
        <el-option label="人工维护" value="MANUAL"></el-option>
        <el-option label="商品详情" value="PRODUCT_DETAIL"></el-option>
        <el-option label="售后规则" value="AFTER_SALE_RULE"></el-option>
        <el-option label="商品评价" value="REVIEW"></el-option>
      </el-select>
      <el-select v-model="data.isEnabled" clearable placeholder="启用状态" class="search-item" @change="load" @clear="load">
        <el-option label="启用" :value="1"></el-option>
        <el-option label="停用" :value="0"></el-option>
      </el-select>
      <el-button type="primary" plain @click="load">查询</el-button>
      <el-button plain @click="reset">重置</el-button>
      <el-button type="danger" plain @click="deleteBatch">批量删除</el-button>
    </div>

    <div class="card" style="margin-bottom: 12px">
      <el-table stripe :data="data.tableData" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" />
        <el-table-column label="商品" min-width="260">
          <template v-slot="scope">
            <div class="product-cell">
              <el-image class="cover" :src="scope.row.coverImage" fit="cover"></el-image>
              <div>
                <h3>{{ scope.row.productName }}</h3>
                <p>{{ scope.row.productNo }}</p>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="knowledgeType" label="知识类型" width="110">
          <template v-slot="scope">
            <el-tag>{{ knowledgeTypeName(scope.row.knowledgeType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="资料标题" min-width="220" show-overflow-tooltip />
        <el-table-column prop="sourceType" label="来源" width="110">
          <template v-slot="scope">{{ sourceTypeName(scope.row.sourceType) }}</template>
        </el-table-column>
        <el-table-column prop="isEnabled" label="状态" width="90">
          <template v-slot="scope">
            <el-tag :type="scope.row.isEnabled === 1 ? 'success' : 'info'">{{ scope.row.isEnabled === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column prop="updateTime" label="更新时间" width="170" />
        <el-table-column label="操作" width="210" fixed="right">
          <template v-slot="scope">
            <el-button type="primary" plain @click="handleEdit(scope.row)">编辑</el-button>
            <el-button type="primary" link @click="showDetail(scope.row)">详情</el-button>
            <el-button type="danger" plain @click="deleteById(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <div class="card" v-if="data.total">
      <el-pagination @current-change="load" background layout="prev, pager, next" :page-size="data.pageSize" v-model:current-page="data.pageNum" :total="data.total" />
    </div>

    <el-dialog :title="data.form.id ? '编辑商品资料' : '新增商品资料'" v-model="data.formVisible" width="780px" destroy-on-close>
      <el-form ref="formRef" :model="data.form" :rules="data.rules" label-width="92px" style="padding: 12px">
        <el-form-item label="关联商品" prop="productId">
          <el-select v-model="data.form.productId" filterable placeholder="请选择商品" style="width: 100%">
            <el-option v-for="item in data.products" :key="item.id" :label="item.name + ' / ' + item.productNo" :value="item.id"></el-option>
          </el-select>
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="知识类型" prop="knowledgeType">
              <el-select v-model="data.form.knowledgeType" placeholder="请选择知识类型" style="width: 100%">
                <el-option label="常见问答" value="FAQ"></el-option>
                <el-option label="卖点说明" value="SELLING_POINT"></el-option>
                <el-option label="参数说明" value="PARAMETER"></el-option>
                <el-option label="售后说明" value="AFTER_SALE"></el-option>
                <el-option label="评价摘要" value="REVIEW_SUMMARY"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="来源类型">
              <el-select v-model="data.form.sourceType" placeholder="请选择来源类型" style="width: 100%">
                <el-option label="人工维护" value="MANUAL"></el-option>
                <el-option label="商品详情" value="PRODUCT_DETAIL"></el-option>
                <el-option label="售后规则" value="AFTER_SALE_RULE"></el-option>
                <el-option label="商品评价" value="REVIEW"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="资料标题" prop="title">
          <el-input v-model="data.form.title" placeholder="请输入资料标题"></el-input>
        </el-form-item>
        <el-form-item label="资料内容" prop="content">
          <el-input v-model="data.form.content" type="textarea" :rows="7" placeholder="请输入可被 AI 导购检索和引用的商品资料"></el-input>
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="启用状态">
              <el-switch v-model="data.form.isEnabled" :active-value="1" :inactive-value="0"></el-switch>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序">
              <el-input-number v-model="data.form.sort" :min="1" style="width: 100%"></el-input-number>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="data.formVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog title="资料详情" v-model="data.detailVisible" width="720px">
      <div class="detail" v-if="data.current">
        <h3>{{ data.current.title }}</h3>
        <p>商品：{{ data.current.productName }} / {{ data.current.productNo }}</p>
        <p>类型：{{ knowledgeTypeName(data.current.knowledgeType) }}，来源：{{ sourceTypeName(data.current.sourceType) }}</p>
        <div>{{ data.current.content }}</div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref } from "vue";
import request from "@/utils/request.js";
import { ElMessage, ElMessageBox } from "element-plus";

const formRef = ref()
const data = reactive({
  productName: null,
  title: null,
  knowledgeType: null,
  sourceType: null,
  isEnabled: null,
  tableData: [],
  products: [],
  pageNum: 1,
  pageSize: 10,
  total: 0,
  ids: [],
  formVisible: false,
  detailVisible: false,
  // 导入弹窗的显示状态、导入范围、选中的商品和按钮 loading
  importVisible: false,
  importScope: 'ONE',
  importProductId: null,
  importLoading: false,
  form: {},
  current: null,
  rules: {
    productId: [{ required: true, message: '请选择关联商品', trigger: 'change' }],
    knowledgeType: [{ required: true, message: '请选择知识类型', trigger: 'change' }],
    title: [{ required: true, message: '请输入资料标题', trigger: 'blur' }],
    content: [{ required: true, message: '请输入资料内容', trigger: 'blur' }]
  }
})

const knowledgeTypeName = (type) => {
  const map = { FAQ: '常见问答', SELLING_POINT: '卖点说明', PARAMETER: '参数说明', AFTER_SALE: '售后说明', REVIEW_SUMMARY: '评价摘要' }
  return map[type] || type
}

const sourceTypeName = (type) => {
  const map = { MANUAL: '人工维护', PRODUCT_DETAIL: '商品详情', AFTER_SALE_RULE: '售后规则', REVIEW: '商品评价' }
  return map[type] || type
}

const loadProducts = () => {
  request.get('/product/selectAll').then(res => {
    if (res.code === '200') {
      data.products = res.data || []
    }
  })
}

const load = () => {
  request.get('/productKnowledge/selectPage', {
    params: {
      pageNum: data.pageNum,
      pageSize: data.pageSize,
      productName: data.productName,
      title: data.title,
      knowledgeType: data.knowledgeType,
      sourceType: data.sourceType,
      isEnabled: data.isEnabled
    }
  }).then(res => {
    if (res.code === '200') {
      data.tableData = res.data?.list || []
      data.total = res.data?.total || 0
    }
  })
}

const reset = () => {
  data.productName = null
  data.title = null
  data.knowledgeType = null
  data.sourceType = null
  data.isEnabled = null
  data.pageNum = 1
  load()
}

// 打开导入弹窗，每次打开都回到默认范围并清空上次选的商品
const handleImport = () => {
  data.importScope = 'ONE'
  data.importProductId = null
  data.importVisible = true
}

// 调导入接口，后端返回本次生成的资料条数。
// 全量导入要逐个商品处理，耗时较长，所以全程有 loading 挡住重复点击。
const confirmImport = () => {
  if (data.importScope === 'ONE' && !data.importProductId) {
    ElMessage.warning('请先选择要导入的商品')
    return
  }
  const url = data.importScope === 'ALL'
      ? '/productKnowledge/importAll'
      : '/productKnowledge/importFromProduct/' + data.importProductId
  data.importLoading = true
  request.post(url).then(res => {
    if (res.code === '200') {
      ElMessage.success('已导入 ' + res.data + ' 条资料，请接着去【商品知识切片】生成切片')
      data.importVisible = false
      load()
    } else {
      ElMessage.error(res.msg)
    }
  }).finally(() => {
    data.importLoading = false
  })
}

const handleAdd = () => {
  data.form = { knowledgeType: 'FAQ', sourceType: 'MANUAL', isEnabled: 1, sort: 1 }
  data.formVisible = true
}

const handleEdit = (row) => {
  data.form = JSON.parse(JSON.stringify(row))
  data.formVisible = true
}

const save = () => {
  formRef.value.validate(valid => {
    if (!valid) {
      return
    }
    const requestApi = data.form.id ? request.put('/productKnowledge/update', data.form) : request.post('/productKnowledge/add', data.form)
    requestApi.then(res => {
      if (res.code === '200') {
        ElMessage.success('保存成功')
        data.formVisible = false
        load()
      } else {
        ElMessage.error(res.msg)
      }
    })
  })
}

const deleteById = (id) => {
  ElMessageBox.confirm('确认删除该商品资料吗？', '删除资料', { type: 'warning' }).then(() => {
    request.delete('/productKnowledge/delete/' + id).then(res => {
      if (res.code === '200') {
        ElMessage.success('删除成功')
        load()
      } else {
        ElMessage.error(res.msg)
      }
    })
  }).catch(() => {})
}

const handleSelectionChange = (rows) => {
  data.ids = rows.map(item => item.id)
}

const deleteBatch = () => {
  if (data.ids.length === 0) {
    ElMessage.warning('请选择要删除的商品资料')
    return
  }
  ElMessageBox.confirm('确认批量删除选中的商品资料吗？', '批量删除', { type: 'warning' }).then(() => {
    request.delete('/productKnowledge/delete/batch', { data: data.ids }).then(res => {
      if (res.code === '200') {
        ElMessage.success('删除成功')
        load()
      } else {
        ElMessage.error(res.msg)
      }
    })
  }).catch(() => {})
}

const showDetail = (row) => {
  data.current = row
  data.detailVisible = true
}

loadProducts()
load()
</script>

<style scoped>
.import-tip {
  margin: 0 0 12px;
  color: #8a94a6;
  font-size: 13px;
  line-height: 1.9;
}

.import-tip.warn {
  margin: 12px 0 0;
  color: #e6a23c;
}

.page-heading {
  margin-bottom: 12px;
  padding: 20px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.page-heading h2 {
  margin: 0;
  color: #172033;
}

.page-heading p {
  margin: 8px 0 0;
  color: #64748b;
}

.card {
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  padding: 14px;
}

.search-card {
  margin-bottom: 12px;
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.search-item {
  width: 190px;
}

.product-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.cover {
  width: 58px;
  height: 58px;
  border-radius: 6px;
}

.product-cell h3 {
  margin: 0;
  font-size: 15px;
  color: #172033;
}

.product-cell p,
.detail p {
  margin: 6px 0 0;
  color: #94a3b8;
  font-size: 12px;
}

.detail h3 {
  margin: 0 0 12px;
  color: #172033;
}

.detail div {
  margin-top: 14px;
  padding: 14px;
  color: #334155;
  line-height: 1.8;
  white-space: pre-line;
  background: #f8fafc;
  border-radius: 8px;
}
</style>
