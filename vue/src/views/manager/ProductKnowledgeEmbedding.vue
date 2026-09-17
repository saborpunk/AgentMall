<template>
  <div>
    <div class="page-heading">
      <div>
        <h2>Embedding 检索</h2>
        <p>为商品知识切片生成文本向量，并通过余弦相似度检索与用户问题最相关的商品资料。</p>
      </div>
      <div>
        <el-button plain :disabled="data.generateLoading" @click="data.generateVisible = true">按切片生成</el-button>
        <el-button type="primary" :loading="data.generateLoading" @click="generateAll">生成全部向量</el-button>
      </div>
    </div>

    <!-- 全量生成进行中才显示：告诉使用者到哪一步了，而不是干等 -->
    <div class="card progress-card" v-if="data.progress.running">
      <div class="progress-head">
        <strong>正在生成向量</strong>
        <span>{{ data.progress.processed }} / {{ data.progress.total }}</span>
      </div>
      <el-progress
          :percentage="data.progress.total ? Math.round(data.progress.processed * 100 / data.progress.total) : 0"
          :stroke-width="14"
          striped
          striped-flow />
      <p class="progress-tip">
        每个切片都要调用一次向量模型接口，切片多时需要几分钟。
        <span v-if="data.progress.failCount">已失败 {{ data.progress.failCount }} 条。</span>
        这个页面关掉也不影响后台继续执行。
      </p>
    </div>

    <div class="card search-panel">
      <div class="search-left">
        <h3>相似度检索</h3>
        <p>输入用户真实问题，系统会把问题转成向量，并在商品知识向量中找出最相近的片段。</p>
        <div class="query-row">
          <el-input v-model="data.queryText" type="textarea" :rows="3" placeholder="例如：我想买一台适合写 Java 毕设的电脑"></el-input>
          <div class="query-actions">
            <el-select v-model="data.searchProductId" clearable filterable placeholder="限定商品" style="width: 180px">
              <el-option v-for="item in data.products" :key="item.id" :label="item.name" :value="item.id"></el-option>
            </el-select>
            <el-input-number v-model="data.topK" :min="1" :max="20" style="width: 120px"></el-input-number>
            <el-button type="primary" @click="search">检索</el-button>
          </div>
        </div>
      </div>
      <div class="result-list">
        <div v-for="item in data.searchResults" :key="item.id" class="result-item">
          <div class="result-head">
            <strong>{{ item.productName }}</strong>
            <el-tag type="success">相似度 {{ item.similarityScore }}</el-tag>
          </div>
          <p>{{ item.chunkTitle }}</p>
          <div>{{ item.chunkContent }}</div>
        </div>
        <el-empty v-if="data.searched && !data.searchResults.length" description="暂无匹配结果"></el-empty>
      </div>
    </div>

    <div class="card search-card">
      <el-input v-model="data.productName" clearable @keyup.enter="load" @clear="reset" prefix-icon="Search" class="search-item" placeholder="请输入商品名称"></el-input>
      <el-input v-model="data.knowledgeTitle" clearable @keyup.enter="load" @clear="reset" prefix-icon="Search" class="search-item" placeholder="请输入资料标题"></el-input>
      <el-input v-model="data.chunkTitle" clearable @keyup.enter="load" @clear="reset" prefix-icon="Search" class="search-item" placeholder="请输入切片标题"></el-input>
      <el-select v-model="data.embeddingStatus" clearable placeholder="向量状态" class="search-item" @change="load" @clear="load">
        <el-option label="可使用" value="READY"></el-option>
      </el-select>
      <el-button type="primary" plain @click="load">查询</el-button>
      <el-button plain @click="reset">重置</el-button>
      <el-button type="danger" plain @click="deleteBatch">批量删除</el-button>
    </div>

    <div class="card" style="margin-bottom: 12px">
      <el-table stripe :data="data.tableData" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" />
        <el-table-column prop="productName" label="商品" min-width="180" show-overflow-tooltip />
        <el-table-column prop="knowledgeTitle" label="资料标题" min-width="220" show-overflow-tooltip />
        <el-table-column prop="chunkTitle" label="切片标题" min-width="220" show-overflow-tooltip />
        <el-table-column prop="embeddingModel" label="模型" width="180" />
        <el-table-column prop="vectorDimension" label="维度" width="80" />
        <el-table-column prop="embeddingStatus" label="状态" width="90">
          <template v-slot="scope">
            <el-tag type="success">{{ scope.row.embeddingStatus === 'READY' ? '可使用' : scope.row.embeddingStatus }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" width="170" />
        <el-table-column label="操作" width="160" fixed="right">
          <template v-slot="scope">
            <el-button type="primary" link @click="showDetail(scope.row)">详情</el-button>
            <el-button type="danger" plain @click="deleteById(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <div class="card" v-if="data.total">
      <el-pagination @current-change="load" background layout="prev, pager, next" :page-size="data.pageSize" v-model:current-page="data.pageNum" :total="data.total" />
    </div>

    <el-dialog title="按切片生成向量" v-model="data.generateVisible" width="640px">
      <el-form label-width="96px" style="padding: 12px">
        <el-form-item label="选择切片">
          <el-select v-model="data.chunkId" filterable placeholder="请选择商品知识切片" style="width: 100%">
            <el-option v-for="item in data.chunks" :key="item.id" :label="item.productName + ' / ' + item.chunkTitle" :value="item.id"></el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="data.generateVisible = false">取消</el-button>
        <el-button type="primary" :loading="data.generateLoading" @click="generateOne">生成向量</el-button>
      </template>
    </el-dialog>

    <el-dialog title="向量详情" v-model="data.detailVisible" width="780px">
      <div class="detail" v-if="data.current">
        <h3>{{ data.current.chunkTitle }}</h3>
        <p>商品：{{ data.current.productName }} / {{ data.current.productNo }}</p>
        <p>资料：{{ data.current.knowledgeTitle }}</p>
        <p>模型：{{ data.current.embeddingModel }}，维度：{{ data.current.vectorDimension }}</p>
        <h4>切片内容</h4>
        <div>{{ data.current.chunkContent }}</div>
        <h4>向量文本</h4>
        <div class="vector">{{ data.current.vectorText }}</div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { onBeforeUnmount, reactive } from "vue";
import request from "@/utils/request.js";
import { ElMessage, ElMessageBox } from "element-plus";

// 轮询进度的定时器句柄，离开页面时要清掉
let progressTimer = null

const data = reactive({
  productName: null,
  knowledgeTitle: null,
  chunkTitle: null,
  embeddingStatus: null,
  queryText: '',
  topK: 5,
  searchProductId: null,
  searchResults: [],
  searched: false,
  tableData: [],
  chunks: [],
  products: [],
  pageNum: 1,
  pageSize: 10,
  total: 0,
  ids: [],
  chunkId: null,
  generateVisible: false,
  // 生成操作耗时较长，用它控制按钮 loading 并防止重复提交
  generateLoading: false,
  // 全量生成进度，字段和后端 EmbeddingGenerateProgress 一致
  progress: {},
  detailVisible: false,
  current: null
})

const loadChunks = () => {
  request.get('/productKnowledgeChunk/selectAll', { params: { chunkStatus: 'READY' } }).then(res => {
    if (res.code === '200') {
      data.chunks = res.data || []
    }
  })
}

const loadProducts = () => {
  request.get('/product/selectAll').then(res => {
    if (res.code === '200') {
      data.products = res.data || []
    }
  })
}

const load = () => {
  request.get('/productKnowledgeEmbedding/selectPage', {
    params: {
      pageNum: data.pageNum,
      pageSize: data.pageSize,
      productName: data.productName,
      knowledgeTitle: data.knowledgeTitle,
      chunkTitle: data.chunkTitle,
      embeddingStatus: data.embeddingStatus
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
  data.knowledgeTitle = null
  data.chunkTitle = null
  data.embeddingStatus = null
  data.pageNum = 1
  load()
}

const generateOne = () => {
  if (!data.chunkId) {
    ElMessage.warning('请选择要生成向量的知识切片')
    return
  }
  data.generateLoading = true
  request.post('/productKnowledgeEmbedding/generate/' + data.chunkId).then(res => {
    if (res.code === '200') {
      ElMessage.success('已生成 ' + res.data + ' 条向量')
      data.generateVisible = false
      load()
    } else {
      ElMessage.error(res.msg)
    }
  }).finally(() => {
    data.generateLoading = false
  })
}

// 启动全量生成。接口只负责把后台任务开起来并返回切片总数，
// 真正的执行进度靠下面的轮询获取，所以这个请求本身很快返回，不存在超时问题。
const generateAll = () => {
  ElMessageBox.confirm('确认根据全部可用切片重新生成向量吗？', '生成全部向量', { type: 'warning' }).then(() => {
    data.generateLoading = true
    request.post('/productKnowledgeEmbedding/generateAll').then(res => {
      if (res.code === '200') {
        data.progress = { running: true, total: res.data, processed: 0, successCount: 0, failCount: 0 }
        pollProgress()
      } else {
        ElMessage.error(res.msg)
        data.generateLoading = false
      }
    }).catch(() => {
      data.generateLoading = false
    })
  }).catch(() => {})
}

// 每秒查一次进度，直到后端报告任务结束
const pollProgress = () => {
  progressTimer = setTimeout(() => {
    request.get('/productKnowledgeEmbedding/generateProgress').then(res => {
      if (res.code !== '200') {
        return
      }
      data.progress = res.data || {}
      if (data.progress.running) {
        // 还在跑就继续下一轮轮询
        pollProgress()
      } else {
        // 任务结束：停轮询、关 loading、刷新列表，并把结果说明展示出来
        data.generateLoading = false
        ElMessage.success(data.progress.message || '生成完成')
        load()
      }
    }).catch(() => {
      // 单次轮询失败不终止任务，隔一秒再试，避免网络抖动就丢掉进度
      pollProgress()
    })
  }, 1000)
}

// 离开页面时清掉定时器，避免组件销毁后还在发请求
onBeforeUnmount(() => {
  if (progressTimer) {
    clearTimeout(progressTimer)
  }
})

const search = () => {
  if (!data.queryText) {
    ElMessage.warning('请输入检索问题')
    return
  }
  request.post('/productKnowledgeEmbedding/search', {
    queryText: data.queryText,
    topK: data.topK,
    productId: data.searchProductId
  }).then(res => {
    if (res.code === '200') {
      data.searchResults = res.data || []
      data.searched = true
    } else {
      ElMessage.error(res.msg)
    }
  })
}

const deleteById = (id) => {
  ElMessageBox.confirm('确认删除该向量记录吗？', '删除向量', { type: 'warning' }).then(() => {
    request.delete('/productKnowledgeEmbedding/delete/' + id).then(res => {
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
    ElMessage.warning('请选择要删除的向量记录')
    return
  }
  ElMessageBox.confirm('确认批量删除选中的向量记录吗？', '批量删除', { type: 'warning' }).then(() => {
    request.delete('/productKnowledgeEmbedding/delete/batch', { data: data.ids }).then(res => {
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

loadChunks()
loadProducts()
load()
</script>

<style scoped>
.progress-card {
  margin-bottom: 12px;
  padding: 18px 22px;
}

.progress-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.progress-head strong {
  color: #1f2329;
}

.progress-head span {
  color: #8a94a6;
  font-size: 13px;
}

.progress-tip {
  margin: 10px 0 0;
  color: #8a94a6;
  font-size: 13px;
  line-height: 1.8;
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

.page-heading h2,
.search-left h3,
.detail h3,
.detail h4 {
  margin: 0;
  color: #172033;
}

.page-heading p,
.search-left p,
.result-item p,
.detail p {
  margin: 8px 0 0;
  color: #64748b;
}

.card {
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  padding: 14px;
}

.search-panel {
  margin-bottom: 12px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 420px;
  gap: 18px;
}

.query-row {
  margin-top: 12px;
}

.query-actions {
  margin-top: 10px;
  display: flex;
  gap: 10px;
  align-items: center;
}

.result-list {
  max-height: 280px;
  overflow: auto;
}

.result-item {
  margin-bottom: 10px;
  padding: 12px;
  background: #f8fafc;
  border-radius: 8px;
}

.result-head {
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.result-item div:last-child {
  margin-top: 8px;
  color: #334155;
  line-height: 1.7;
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

.detail h4 {
  margin-top: 16px;
}

.detail div {
  margin-top: 10px;
  padding: 14px;
  color: #334155;
  line-height: 1.8;
  white-space: pre-line;
  background: #f8fafc;
  border-radius: 8px;
}

.detail .vector {
  max-height: 180px;
  overflow: auto;
  word-break: break-all;
  font-family: Consolas, Monaco, monospace;
}
</style>
