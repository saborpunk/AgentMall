<template>
  <div>
    <div class="page-heading">
      <div>
        <h2>AI商品问答</h2>
        <p>围绕商品知识、价格库存、订单状态和售后规则进行问答，并保存每次回答依据。</p>
      </div>
      <el-button type="primary" @click="handleAsk">发起问答</el-button>
    </div>

    <div class="card search-card">
      <el-input v-model="data.qaNo" @clear="reset" clearable @keyup.enter="load" prefix-icon="Search" class="search-item" placeholder="请输入问答编号"></el-input>
      <el-input v-model="data.questionText" @clear="reset" clearable @keyup.enter="load" prefix-icon="Search" class="search-item" placeholder="请输入问题关键词"></el-input>
      <el-input v-model="data.productName" @clear="reset" clearable @keyup.enter="load" prefix-icon="Search" class="search-item" placeholder="请输入商品名称"></el-input>
      <el-input v-model="data.orderNo" @clear="reset" clearable @keyup.enter="load" prefix-icon="Search" class="search-item" placeholder="请输入订单编号"></el-input>
      <el-select v-model="data.questionType" clearable placeholder="问题类型" class="search-item" @change="load">
        <el-option label="商品知识" value="PRODUCT"></el-option>
        <el-option label="价格库存" value="PRICE_STOCK"></el-option>
        <el-option label="订单状态" value="ORDER"></el-option>
        <el-option label="售后咨询" value="AFTER_SALE"></el-option>
      </el-select>
      <el-button type="primary" plain @click="load">查询</el-button>
      <el-button plain @click="reset">重置</el-button>
      <el-button type="danger" plain @click="delBatch">批量删除</el-button>
    </div>

    <div class="card" style="margin-bottom: 5px">
      <el-table stripe :data="data.tableData" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" />
        <el-table-column prop="qaNo" label="问答编号" width="190" />
        <el-table-column prop="questionType" label="问题类型" width="100">
          <template #default="scope">
            <el-tag>{{ typeName(scope.row.questionType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="questionText" label="问题内容" min-width="260" show-overflow-tooltip />
        <el-table-column prop="productName" label="关联商品" min-width="150" show-overflow-tooltip />
        <el-table-column prop="orderNo" label="关联订单" width="180" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="scope">
            <el-tag type="success">{{ scope.row.status === 'DONE' ? '已回答' : scope.row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="scope">
            <el-button type="info" circle :icon="View" @click="viewDetail(scope.row)"></el-button>
            <el-button type="danger" circle :icon="Delete" @click="del(scope.row.id)"></el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <div class="card" v-if="data.total">
      <el-pagination @current-change="load" background layout="prev, pager, next" :page-size="data.pageSize" v-model:current-page="data.pageNum" :total="data.total" />
    </div>

    <el-dialog title="发起 AI 商品问答" v-model="data.askVisible" width="760px" destroy-on-close>
      <el-form label-width="100px" style="padding: 20px">
        <el-form-item label="问题类型">
          <el-select v-model="data.form.questionType" placeholder="请选择问题类型" style="width: 100%" @change="handleTypeChange">
            <el-option label="商品知识" value="PRODUCT"></el-option>
            <el-option label="价格库存" value="PRICE_STOCK"></el-option>
            <el-option label="订单状态" value="ORDER"></el-option>
            <el-option label="售后咨询" value="AFTER_SALE"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="发起人">
          <!-- 后台问答是管理员自己在测，问答记录固定挂在自己名下 -->
          <el-input :value="data.user.name || data.user.username" disabled></el-input>
        </el-form-item>
        <el-form-item label="关联商品" v-if="data.form.questionType !== 'ORDER'">
          <el-select v-model="data.form.productId" clearable filterable placeholder="商品知识、价格库存、售后咨询可选择商品" style="width: 100%">
            <el-option v-for="item in data.productOptions" :key="item.id" :label="item.name + ' / ￥' + item.price" :value="item.id"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="订单编号" v-if="data.form.questionType === 'ORDER'">
          <el-input v-model="data.form.orderNo" placeholder="请输入订单编号，例如 OD20260626163000100"></el-input>
        </el-form-item>
        <el-form-item label="问题内容">
          <el-input v-model="data.form.questionText" type="textarea" :rows="5" placeholder="请输入用户想问的问题"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="data.askVisible = false">取消</el-button>
          <el-button type="primary" @click="ask">生成回答</el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog title="问答详情" v-model="data.detailVisible" width="900px">
      <el-descriptions :column="2" border v-if="data.current">
        <el-descriptions-item label="问答编号">{{ data.current.qaNo }}</el-descriptions-item>
        <el-descriptions-item label="问题类型">{{ typeName(data.current.questionType) }}</el-descriptions-item>
        <el-descriptions-item label="关联用户">{{ data.current.userName || data.current.userId || '暂无' }}</el-descriptions-item>
        <el-descriptions-item label="关联商品">{{ data.current.productName || '暂无' }}</el-descriptions-item>
        <el-descriptions-item label="关联订单">{{ data.current.orderNo || '暂无' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ data.current.createTime }}</el-descriptions-item>
        <el-descriptions-item label="问题内容" :span="2">{{ data.current.questionText }}</el-descriptions-item>
      </el-descriptions>
      <h4>AI回答</h4>
      <pre class="detail-text">{{ data.current?.answerText || '暂无回答' }}</pre>
      <h4>回答依据</h4>
      <pre class="detail-text">{{ data.current?.evidenceContent || '暂无依据' }}</pre>
      <h4>工具调用轨迹</h4>
      <pre class="detail-text">{{ data.current?.toolTrace || '暂无工具调用' }}</pre>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive } from "vue";
import request from "@/utils/request.js";
import { ElMessage, ElMessageBox } from "element-plus";
import { Delete, View } from "@element-plus/icons-vue";

const data = reactive({
  // 当前登录的管理员，发起问答时作为提问人
  user: JSON.parse(localStorage.getItem('sys-user') || '{}'),
  askVisible: false,
  detailVisible: false,
  current: null,
  form: {
    userId: null,
    questionType: 'PRODUCT',
    productId: null,
    orderNo: null,
    questionText: ''
  },
  tableData: [],
  productOptions: [],
  pageNum: 1,
  pageSize: 10,
  total: 0,
  qaNo: null,
  questionType: null,
  questionText: null,
  productName: null,
  orderNo: null,
  ids: []
})

const typeName = (type) => {
  const map = {
    PRODUCT: '商品知识',
    PRICE_STOCK: '价格库存',
    ORDER: '订单状态',
    AFTER_SALE: '售后咨询'
  }
  return map[type] || type
}

const load = () => {
  request.get('/shoppingQa/selectPage', {
    params: {
      pageNum: data.pageNum,
      pageSize: data.pageSize,
      qaNo: data.qaNo,
      questionType: data.questionType,
      questionText: data.questionText,
      productName: data.productName,
      orderNo: data.orderNo
    }
  }).then(res => {
    if (res.code === '200') {
      data.tableData = res.data?.list || []
      data.total = res.data?.total
    }
  })
}

const loadProductOptions = () => {
  request.get('/product/selectAll', {
    params: {
      status: 'ON_SALE'
    }
  }).then(res => {
    if (res.code === '200') {
      data.productOptions = res.data || []
    }
  })
}

const handleAsk = () => {
  data.form = {
    // 提问人固定为当前登录的管理员
    userId: data.user.id,
    questionType: 'PRODUCT',
    productId: null,
    orderNo: null,
    questionText: ''
  }
  loadProductOptions()
  data.askVisible = true
}

const handleTypeChange = () => {
  data.form.productId = null
  data.form.orderNo = null
}

const ask = () => {
  if (!data.form.questionType) {
    ElMessage.warning('请选择问题类型')
    return
  }
  if (!data.form.questionText) {
    ElMessage.warning('请输入问题内容')
    return
  }
  if ((data.form.questionType === 'PRODUCT' || data.form.questionType === 'PRICE_STOCK') && !data.form.productId) {
    ElMessage.warning('请选择关联商品')
    return
  }
  if (data.form.questionType === 'ORDER' && !data.form.orderNo) {
    ElMessage.warning('请输入订单编号')
    return
  }
  request.post('/shoppingQa/ask', data.form).then(res => {
    if (res.code === '200') {
      ElMessage.success('回答生成成功')
      data.current = res.data
      data.askVisible = false
      data.detailVisible = true
      load()
    } else {
      ElMessage.error(res.msg)
    }
  })
}

const reset = () => {
  data.qaNo = null
  data.questionType = null
  data.questionText = null
  data.productName = null
  data.orderNo = null
  data.pageNum = 1
  load()
}

const viewDetail = (row) => {
  data.current = JSON.parse(JSON.stringify(row))
  data.detailVisible = true
}

const del = (id) => {
  ElMessageBox.confirm('删除后数据无法恢复，确定删除吗？', '删除确认', { type: 'warning' }).then(() => {
    request.delete('/shoppingQa/delete/' + id).then(res => {
      if (res.code === '200') {
        ElMessage.success('删除成功')
        load()
      } else {
        ElMessage.error(res.msg)
      }
    })
  })
}

const delBatch = () => {
  if (!data.ids.length) {
    ElMessage.warning('请选择数据')
    return
  }
  ElMessageBox.confirm('删除后数据无法恢复，确定批量删除吗？', '删除确认', { type: 'warning' }).then(() => {
    request.delete('/shoppingQa/delete/batch', { data: data.ids }).then(res => {
      if (res.code === '200') {
        ElMessage.success('操作成功')
        load()
      } else {
        ElMessage.error(res.msg)
      }
    })
  })
}

const handleSelectionChange = (rows) => {
  data.ids = rows.map(v => v.id)
}

load()
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

.search-card {
  margin-bottom: 5px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.search-item {
  width: 190px;
}

.detail-text {
  margin: 8px 0 16px;
  padding: 14px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  white-space: pre-wrap;
  line-height: 1.7;
  color: #334155;
}
</style>
