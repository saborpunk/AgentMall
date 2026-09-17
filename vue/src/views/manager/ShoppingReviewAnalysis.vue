<template>
  <div>
    <div class="page-heading">
      <div>
        <h2>AI商品评价分析</h2>
        <p>基于已通过的商品评价生成评分、情绪、优点、问题和改进建议。</p>
      </div>
      <el-button type="primary" @click="handleGenerate">生成分析</el-button>
    </div>

    <div class="card search-card">
      <el-input v-model="data.analysisNo" @clear="reset" clearable @keyup.enter="load" prefix-icon="Search" class="search-item" placeholder="请输入分析编号"></el-input>
      <el-input v-model="data.productName" @clear="reset" clearable @keyup.enter="load" prefix-icon="Search" class="search-item" placeholder="请输入商品名称"></el-input>
      <el-select v-model="data.sentimentLevel" clearable placeholder="情绪等级" class="search-item" @change="load">
        <el-option label="正向" value="POSITIVE"></el-option>
        <el-option label="中性" value="NEUTRAL"></el-option>
        <el-option label="负向" value="NEGATIVE"></el-option>
      </el-select>
      <el-button type="primary" plain @click="load">查询</el-button>
      <el-button plain @click="reset">重置</el-button>
      <el-button type="danger" plain @click="delBatch">批量删除</el-button>
    </div>

    <div class="card" style="margin-bottom: 5px">
      <el-table stripe :data="data.tableData" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" />
        <el-table-column prop="analysisNo" label="分析编号" width="190" />
        <el-table-column label="商品" min-width="260">
          <template #default="scope">
            <div class="product-cell">
              <el-image class="cover" :src="scope.row.coverImage" fit="cover"></el-image>
              <div>
                <h3>{{ scope.row.productName }}</h3>
                <p>{{ scope.row.productNo }}</p>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="reviewCount" label="评价数" width="90" />
        <el-table-column prop="averageRating" label="均分" width="90" />
        <el-table-column prop="positiveRate" label="好评率" width="100">
          <template #default="scope">{{ scope.row.positiveRate }}%</template>
        </el-table-column>
        <el-table-column prop="sentimentLevel" label="情绪" width="90">
          <template #default="scope">
            <el-tag :type="sentimentType(scope.row.sentimentLevel)">{{ sentimentName(scope.row.sentimentLevel) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="keywordSummary" label="关键词" min-width="180" show-overflow-tooltip />
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

    <el-dialog title="生成评价分析" v-model="data.generateVisible" width="620px" destroy-on-close>
      <el-form label-width="100px" style="padding: 20px">
        <el-form-item label="选择商品">
          <el-select v-model="data.form.productId" filterable placeholder="请选择需要分析评价的商品" style="width: 100%">
            <!-- 标签带上评价条数，一眼能看出这个商品有多少评价可分析 -->
            <el-option
                v-for="item in data.productOptions"
                :key="item.id"
                :label="item.name + '（' + item.reviewCount + ' 条评价）'"
                :value="item.id"></el-option>
          </el-select>
          <!-- 没有任何商品有已审核评价时，说明原因而不是给一个空下拉 -->
          <div class="option-tip" v-if="!data.productOptions.length">
            当前没有商品拥有已审核通过的评价。请先在【商品评价管理】里审核通过一些评价。
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="data.generateVisible = false">取消</el-button>
          <el-button type="primary" :loading="data.generateLoading" @click="generate">生成分析</el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog title="评价分析详情" v-model="data.detailVisible" width="900px">
      <el-descriptions :column="3" border v-if="data.current">
        <el-descriptions-item label="分析编号">{{ data.current.analysisNo }}</el-descriptions-item>
        <el-descriptions-item label="商品">{{ data.current.productName }}</el-descriptions-item>
        <el-descriptions-item label="情绪">{{ sentimentName(data.current.sentimentLevel) }}</el-descriptions-item>
        <el-descriptions-item label="评价数">{{ data.current.reviewCount }}</el-descriptions-item>
        <el-descriptions-item label="平均评分">{{ data.current.averageRating }}</el-descriptions-item>
        <el-descriptions-item label="好评率">{{ data.current.positiveRate }}%</el-descriptions-item>
        <el-descriptions-item label="好评数">{{ data.current.positiveCount }}</el-descriptions-item>
        <el-descriptions-item label="中评数">{{ data.current.neutralCount }}</el-descriptions-item>
        <el-descriptions-item label="差评数">{{ data.current.negativeCount }}</el-descriptions-item>
      </el-descriptions>
      <!-- 这四项都是大模型生成的，带 Markdown 语法，渲染成 HTML 而不是展示原文 -->
      <h4>优点总结</h4>
      <div class="md-body" v-html="renderMarkdown(data.current?.advantageSummary, '暂无优点总结')"></div>
      <h4>问题总结</h4>
      <div class="md-body" v-html="renderMarkdown(data.current?.problemSummary, '暂无问题总结')"></div>
      <h4>关键词</h4>
      <div class="md-body" v-html="renderMarkdown(data.current?.keywordSummary, '暂无关键词')"></div>
      <h4>改进建议</h4>
      <div class="md-body" v-html="renderMarkdown(data.current?.improvementSuggestion, '暂无改进建议')"></div>
      <h4>评价样本</h4>
      <pre class="detail-text">{{ data.current?.sampleReviews || '暂无评价样本' }}</pre>
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
  // 分析要等大模型返回，耗时较长，用它控制 loading 并防重复提交
  generateLoading: false,
  detailVisible: false,
  current: null,
  form: {
    productId: null
  },
  tableData: [],
  productOptions: [],
  pageNum: 1,
  pageSize: 10,
  total: 0,
  analysisNo: null,
  productName: null,
  sentimentLevel: null,
  ids: []
})

const sentimentName = (level) => {
  const map = {
    POSITIVE: '正向',
    NEUTRAL: '中性',
    NEGATIVE: '负向'
  }
  return map[level] || level
}

const sentimentType = (level) => {
  if (level === 'POSITIVE') {
    return 'success'
  }
  if (level === 'NEGATIVE') {
    return 'danger'
  }
  return 'warning'
}

const load = () => {
  request.get('/shoppingReviewAnalysis/selectPage', {
    params: {
      pageNum: data.pageNum,
      pageSize: data.pageSize,
      analysisNo: data.analysisNo,
      productName: data.productName,
      sentimentLevel: data.sentimentLevel
    }
  }).then(res => {
    if (res.code === '200') {
      data.tableData = res.data?.list || []
      data.total = res.data?.total
    }
  })
}

// 评价分析必须有已审核评价才能生成，所以下拉只放有评价的商品，并带上评价条数
const loadProductOptions = () => {
  request.get('/productReview/selectAll', {
    params: {
      auditStatus: 'APPROVED'
    }
  }).then(res => {
    if (res.code !== '200') {
      return
    }
    // 按商品聚合评价条数，得到"哪些商品可分析"
    const countMap = {}
    ;(res.data || []).forEach(item => {
      countMap[item.productId] = (countMap[item.productId] || 0) + 1
    })
    request.get('/product/selectAll').then(productRes => {
      if (productRes.code === '200') {
        data.productOptions = (productRes.data || [])
            .filter(item => countMap[item.id])
            .map(item => ({ ...item, reviewCount: countMap[item.id] }))
      }
    })
  })
}

const handleGenerate = () => {
  data.form = {
    productId: null
  }
  loadProductOptions()
  data.generateVisible = true
}

const generate = () => {
  if (!data.form.productId) {
    ElMessage.warning('请选择商品')
    return
  }
  // 分析要把评价汇总后交给大模型写四项结论，耗时较长，全程给 loading
  data.generateLoading = true
  request.post('/shoppingReviewAnalysis/generate', data.form).then(res => {
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
  data.analysisNo = null
  data.productName = null
  data.sentimentLevel = null
  data.pageNum = 1
  load()
}

const viewDetail = (row) => {
  data.current = JSON.parse(JSON.stringify(row))
  data.detailVisible = true
}

const del = (id) => {
  ElMessageBox.confirm('删除后数据无法恢复，确定删除吗？', '删除确认', { type: 'warning' }).then(() => {
    request.delete('/shoppingReviewAnalysis/delete/' + id).then(res => {
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
    request.delete('/shoppingReviewAnalysis/delete/batch', { data: data.ids }).then(res => {
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

.option-tip {
  margin-top: 8px;
  color: #e6a23c;
  font-size: 13px;
  line-height: 1.7;
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

.product-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.cover {
  width: 54px;
  height: 54px;
  border-radius: 6px;
}

.product-cell h3 {
  margin: 0;
  font-size: 15px;
  color: #172033;
}

.product-cell p {
  margin: 6px 0 0;
  color: #94a3b8;
  font-size: 12px;
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
