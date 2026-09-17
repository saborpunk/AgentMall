<template>
  <div>
    <div class="page-heading">
      <div>
        <h2>AI运营增长报告</h2>
        <p>聚合订单、导购、推荐、问答和评价分析数据，生成商城运营增长建议。</p>
      </div>
      <el-button type="primary" @click="handleGenerate">生成报告</el-button>
    </div>

    <div class="card search-card">
      <el-input v-model="data.reportNo" @clear="reset" clearable @keyup.enter="load" prefix-icon="Search" class="search-item" placeholder="请输入报告编号"></el-input>
      <el-input v-model="data.reportTitle" @clear="reset" clearable @keyup.enter="load" prefix-icon="Search" class="search-item" placeholder="请输入报告标题"></el-input>
      <el-select v-model="data.reportType" clearable placeholder="报告类型" class="search-item" @change="load">
        <el-option label="综合报告" value="OVERALL"></el-option>
        <el-option label="导购增长" value="GUIDE"></el-option>
        <el-option label="商品运营" value="PRODUCT"></el-option>
      </el-select>
      <el-button type="primary" plain @click="load">查询</el-button>
      <el-button plain @click="reset">重置</el-button>
      <el-button type="danger" plain @click="delBatch">批量删除</el-button>
    </div>

    <div class="card" style="margin-bottom: 5px">
      <el-table stripe :data="data.tableData" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" />
        <el-table-column prop="reportNo" label="报告编号" width="190" />
        <el-table-column prop="reportTitle" label="报告标题" min-width="220" show-overflow-tooltip />
        <el-table-column prop="reportType" label="类型" width="100">
          <template #default="scope">{{ reportTypeName(scope.row.reportType) }}</template>
        </el-table-column>
        <el-table-column prop="orderCount" label="订单数" width="90" />
        <el-table-column prop="salesAmount" label="销售额" width="120" />
        <el-table-column prop="guideTaskCount" label="导购任务" width="100" />
        <el-table-column prop="recommendationCount" label="推荐数" width="90" />
        <el-table-column prop="qaCount" label="问答数" width="90" />
        <el-table-column prop="conversionRate" label="转化率" width="100">
          <template #default="scope">{{ scope.row.conversionRate }}%</template>
        </el-table-column>
        <el-table-column prop="createTime" label="生成时间" width="170" />
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

    <el-dialog title="生成运营增长报告" v-model="data.generateVisible" width="620px" destroy-on-close>
      <el-form label-width="100px" style="padding: 20px">
        <el-form-item label="报告标题">
          <el-input v-model="data.form.reportTitle" placeholder="请输入报告标题"></el-input>
        </el-form-item>
        <el-form-item label="报告类型">
          <el-select v-model="data.form.reportType" placeholder="请选择报告类型" style="width: 100%">
            <el-option label="综合报告" value="OVERALL"></el-option>
            <el-option label="导购增长" value="GUIDE"></el-option>
            <el-option label="商品运营" value="PRODUCT"></el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button :disabled="data.generateLoading" @click="data.generateVisible = false">取消</el-button>
          <el-button type="primary" :loading="data.generateLoading" @click="generate">生成报告</el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog title="运营增长报告详情" v-model="data.detailVisible" width="940px">
      <el-descriptions :column="4" border v-if="data.current">
        <el-descriptions-item label="报告编号">{{ data.current.reportNo }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ reportTypeName(data.current.reportType) }}</el-descriptions-item>
        <el-descriptions-item label="订单数">{{ data.current.orderCount }}</el-descriptions-item>
        <el-descriptions-item label="销售额">{{ data.current.salesAmount }}</el-descriptions-item>
        <el-descriptions-item label="导购任务">{{ data.current.guideTaskCount }}</el-descriptions-item>
        <el-descriptions-item label="完成任务">{{ data.current.guideDoneCount }}</el-descriptions-item>
        <el-descriptions-item label="推荐记录">{{ data.current.recommendationCount }}</el-descriptions-item>
        <el-descriptions-item label="转化率">{{ data.current.conversionRate }}%</el-descriptions-item>
        <el-descriptions-item label="问答记录">{{ data.current.qaCount }}</el-descriptions-item>
        <el-descriptions-item label="评价分析">{{ data.current.reviewAnalysisCount }}</el-descriptions-item>
        <el-descriptions-item label="生成时间" :span="2">{{ data.current.createTime }}</el-descriptions-item>
      </el-descriptions>
      <h4>数据快照</h4>
      <pre class="detail-text">{{ data.current?.dataSnapshot || '暂无数据快照' }}</pre>
      <h4>热门推荐商品</h4>
      <pre class="detail-text">{{ data.current?.topProductSummary || '暂无推荐商品' }}</pre>
      <h4>用户咨询分布</h4>
      <pre class="detail-text">{{ data.current?.qaSummary || '暂无问答数据' }}</pre>
      <h4>评价洞察</h4>
      <pre class="detail-text">{{ data.current?.reviewSummary || '暂无评价分析' }}</pre>
      <h4>增长建议</h4>
      <!-- 增长建议是大模型写的，带 Markdown 语法，渲染成 HTML 而不是展示原文 -->
      <div class="md-body" v-html="renderMarkdown(data.current?.growthSuggestion, '暂无增长建议')"></div>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive } from "vue";
import request from "@/utils/request.js";
import { renderMarkdown } from "@/utils/markdown.js";
import { ElMessage, ElMessageBox } from "element-plus";
import { Delete, View } from "@element-plus/icons-vue";

const data = reactive({
  generateVisible: false,
  // 报告要等大模型写完增长建议，耗时较长，用它控制 loading 并防重复提交
  generateLoading: false,
  detailVisible: false,
  current: null,
  form: {
    reportTitle: '',
    reportType: 'OVERALL'
  },
  tableData: [],
  pageNum: 1,
  pageSize: 10,
  total: 0,
  reportNo: null,
  reportTitle: null,
  reportType: null,
  ids: []
})

const reportTypeName = (type) => {
  const map = {
    OVERALL: '综合报告',
    GUIDE: '导购增长',
    PRODUCT: '商品运营'
  }
  return map[type] || type
}

const load = () => {
  request.get('/shoppingGrowthReport/selectPage', {
    params: {
      pageNum: data.pageNum,
      pageSize: data.pageSize,
      reportNo: data.reportNo,
      reportTitle: data.reportTitle,
      reportType: data.reportType
    }
  }).then(res => {
    if (res.code === '200') {
      data.tableData = res.data?.list || []
      data.total = res.data?.total
    }
  })
}

const handleGenerate = () => {
  data.form = {
    reportTitle: 'AI商城运营增长报告',
    reportType: 'OVERALL'
  }
  data.generateVisible = true
}

const generate = () => {
  if (!data.form.reportTitle) {
    ElMessage.warning('请输入报告标题')
    return
  }
  if (!data.form.reportType) {
    ElMessage.warning('请选择报告类型')
    return
  }
  // 报告要先统计各项指标，再等大模型写完增长建议，耗时较长，全程给 loading
  data.generateLoading = true
  request.post('/shoppingGrowthReport/generate', data.form).then(res => {
    if (res.code === '200') {
      ElMessage.success('生成成功')
      data.current = res.data
      data.generateVisible = false
      data.detailVisible = true
      load()
    } else {
      ElMessage.error(res.msg)
    }
  }).finally(() => {
    data.generateLoading = false
  })
}

const reset = () => {
  data.reportNo = null
  data.reportTitle = null
  data.reportType = null
  data.pageNum = 1
  load()
}

const viewDetail = (row) => {
  data.current = JSON.parse(JSON.stringify(row))
  data.detailVisible = true
}

const del = (id) => {
  ElMessageBox.confirm('删除后数据无法恢复，确定删除吗？', '删除确认', { type: 'warning' }).then(() => {
    request.delete('/shoppingGrowthReport/delete/' + id).then(res => {
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
    request.delete('/shoppingGrowthReport/delete/batch', { data: data.ids }).then(res => {
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
/* 大模型返回的 Markdown 渲染后的样式。
   用 :deep() 是因为 v-html 插入的节点不带 scoped 作用域的属性标记，
   不穿透的话这些样式对它们完全不生效。 */
.md-body {
  padding: 14px 16px;
  color: #334155;
  line-height: 1.9;
  background: #f8fafc;
  border-radius: 8px;
}

.md-body :deep(h1),
.md-body :deep(h2),
.md-body :deep(h3),
.md-body :deep(h4) {
  margin: 14px 0 8px;
  color: #1f2329;
  font-size: 15px;
}

.md-body :deep(p) {
  margin: 8px 0;
}

.md-body :deep(ul),
.md-body :deep(ol) {
  margin: 8px 0;
  padding-left: 22px;
}

.md-body :deep(li) {
  margin: 4px 0;
}

.md-body :deep(strong) {
  color: #1f2329;
}

.md-body :deep(table) {
  width: 100%;
  margin: 10px 0;
  border-collapse: collapse;
}

.md-body :deep(th),
.md-body :deep(td) {
  padding: 8px 10px;
  border: 1px solid #e2e8f0;
  text-align: left;
}

.md-body :deep(th) {
  background: #eef2f7;
}

.md-body :deep(code) {
  padding: 2px 5px;
  border-radius: 4px;
  background: #eef2f7;
  font-size: 13px;
}

.md-body :deep(.md-empty) {
  margin: 0;
  color: #8a94a6;
}

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
