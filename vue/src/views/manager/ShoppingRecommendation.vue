<template>
  <div>
    <div class="page-heading">
      <div>
        <h2>AI推荐商品</h2>
        <p>查看智能导购任务生成的结构化推荐商品、推荐排名、评分、价格库存快照和推荐理由。</p>
      </div>
    </div>

    <div class="card search-card">
      <el-input v-model="data.taskNo" @clear="reset" clearable @keyup.enter="load" prefix-icon="Search" class="search-item" placeholder="请输入任务编号"></el-input>
      <el-input v-model="data.runNo" @clear="reset" clearable @keyup.enter="load" prefix-icon="Search" class="search-item" placeholder="请输入运行编号"></el-input>
      <el-input v-model="data.productName" @clear="reset" clearable @keyup.enter="load" prefix-icon="Search" class="search-item" placeholder="请输入商品名称"></el-input>
      <el-select v-model="data.status" clearable placeholder="状态" class="search-item" @change="load" @clear="reset">
        <el-option label="有效" value="VALID"></el-option>
      </el-select>
      <el-button type="primary" plain @click="load">查询</el-button>
      <el-button plain @click="reset">重置</el-button>
      <el-button type="danger" plain @click="delBatch">批量删除</el-button>
    </div>

    <div class="card" style="margin-bottom: 5px">
      <el-table stripe :data="data.tableData" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" />
        <el-table-column prop="productImage" label="商品图" width="90">
          <template #default="scope">
            <el-image v-if="scope.row.productImage" class="product-image" :src="scope.row.productImage" :preview-src-list="[scope.row.productImage]" preview-teleported></el-image>
          </template>
        </el-table-column>
        <el-table-column prop="taskNo" label="任务编号" width="190" />
        <el-table-column prop="runNo" label="运行编号" width="190" />
        <el-table-column prop="productName" label="推荐商品" min-width="180" show-overflow-tooltip />
        <el-table-column prop="recommendRank" label="排名" width="80" />
        <el-table-column prop="recommendScore" label="评分" width="80" />
        <el-table-column prop="priceSnapshot" label="推荐价" width="100" />
        <el-table-column prop="availableQuantity" label="库存" width="90" />
        <el-table-column prop="discountAmount" label="优惠" width="90" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="scope">
            <el-tag type="success">{{ scope.row.status === 'VALID' ? '有效' : scope.row.status }}</el-tag>
          </template>
        </el-table-column>
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

    <el-dialog title="推荐详情" v-model="data.detailVisible" width="880px">
      <el-descriptions :column="2" border v-if="data.current">
        <el-descriptions-item label="任务编号">{{ data.current.taskNo }}</el-descriptions-item>
        <el-descriptions-item label="运行编号">{{ data.current.runNo }}</el-descriptions-item>
        <el-descriptions-item label="用户">{{ data.current.userName }}</el-descriptions-item>
        <el-descriptions-item label="商品编号">{{ data.current.productNo }}</el-descriptions-item>
        <el-descriptions-item label="商品名称">{{ data.current.productName }}</el-descriptions-item>
        <el-descriptions-item label="推荐排名">{{ data.current.recommendRank }}</el-descriptions-item>
        <el-descriptions-item label="推荐评分">{{ data.current.recommendScore }}</el-descriptions-item>
        <el-descriptions-item label="推荐价">{{ data.current.priceSnapshot }}</el-descriptions-item>
        <el-descriptions-item label="原价">{{ data.current.originalPriceSnapshot }}</el-descriptions-item>
        <el-descriptions-item label="推荐时库存">{{ data.current.availableQuantity }}</el-descriptions-item>
        <el-descriptions-item label="优惠金额">{{ data.current.discountAmount }}</el-descriptions-item>
        <el-descriptions-item label="折扣率">{{ data.current.discountRate }}</el-descriptions-item>
      </el-descriptions>
      <h4>推荐理由</h4>
      <pre class="detail-text">{{ data.current?.recommendReason || '暂无推荐理由' }}</pre>
      <h4>证据摘要</h4>
      <pre class="detail-text">{{ data.current?.evidenceSummary || '暂无证据摘要' }}</pre>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive } from "vue";
import request from "@/utils/request.js";
import { ElMessage, ElMessageBox } from "element-plus";
import { Delete, View } from "@element-plus/icons-vue";

const data = reactive({
  detailVisible: false,
  current: null,
  tableData: [],
  pageNum: 1,
  pageSize: 10,
  total: 0,
  taskNo: null,
  runNo: null,
  productName: null,
  status: null,
  ids: []
})

const load = () => {
  request.get('/shoppingRecommendation/selectPage', {
    params: {
      pageNum: data.pageNum,
      pageSize: data.pageSize,
      taskNo: data.taskNo,
      runNo: data.runNo,
      productName: data.productName,
      status: data.status
    }
  }).then(res => {
    if (res.code === '200') {
      data.tableData = res.data?.list || []
      data.total = res.data?.total
    }
  })
}

const reset = () => {
  data.taskNo = null
  data.runNo = null
  data.productName = null
  data.status = null
  data.pageNum = 1
  load()
}

const viewDetail = (row) => {
  data.current = JSON.parse(JSON.stringify(row))
  data.detailVisible = true
}

const del = (id) => {
  ElMessageBox.confirm('删除后数据无法恢复，确定删除吗？', '删除确认', { type: 'warning' }).then(() => {
    request.delete('/shoppingRecommendation/delete/' + id).then(res => {
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
    request.delete('/shoppingRecommendation/delete/batch', { data: data.ids }).then(res => {
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

.card {
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  padding: 16px;
}

.search-card {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 5px;
}

.search-item {
  width: 220px;
}

.product-image {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  display: block;
  object-fit: cover;
}

h4 {
  margin: 18px 0 8px;
  color: #172033;
}

.detail-text {
  margin: 0;
  padding: 14px;
  color: #334155;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
  background: #f8fafc;
  border-radius: 8px;
}
</style>
