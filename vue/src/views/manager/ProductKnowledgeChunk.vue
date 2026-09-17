<template>
  <div>
    <div class="page-heading">
      <div>
        <h2>商品知识切片</h2>
        <p>将商品知识库资料切成适合检索的文本片段，为后续 Embedding 生成和 RAG 检索做准备。</p>
      </div>
      <div>
        <el-button plain :disabled="data.generateLoading" @click="data.generateVisible = true">按资料生成</el-button>
        <el-button type="primary" :loading="data.generateLoading" @click="generateAll">生成全部切片</el-button>
      </div>
    </div>

    <div class="card principle-card">
      <div>
        <h3>RAG 切片思路</h3>
        <p>系统先从商品知识库读取已启用资料，再按固定长度和重叠区间切成片段。后续每个片段会生成 Embedding，用户提问时通过相似度检索找到相关片段，再交给大模型组织回答。</p>
      </div>
      <div class="metrics">
        <div>
          <strong>180</strong>
          <span>单片字符</span>
        </div>
        <div>
          <strong>30</strong>
          <span>重叠字符</span>
        </div>
        <div>
          <strong>{{ data.total }}</strong>
          <span>当前片段</span>
        </div>
      </div>
    </div>

    <div class="card search-card">
      <el-input v-model="data.productName" clearable @keyup.enter="load" @clear="reset" prefix-icon="Search" class="search-item" placeholder="请输入商品名称"></el-input>
      <el-input v-model="data.knowledgeTitle" clearable @keyup.enter="load" @clear="reset" prefix-icon="Search" class="search-item" placeholder="请输入资料标题"></el-input>
      <el-input v-model="data.chunkContent" clearable @keyup.enter="load" @clear="reset" prefix-icon="Search" class="search-item" placeholder="请输入片段内容"></el-input>
      <el-select v-model="data.knowledgeType" clearable placeholder="知识类型" class="search-item" @change="load" @clear="load">
        <el-option label="常见问答" value="FAQ"></el-option>
        <el-option label="卖点说明" value="SELLING_POINT"></el-option>
        <el-option label="参数说明" value="PARAMETER"></el-option>
        <el-option label="售后说明" value="AFTER_SALE"></el-option>
        <el-option label="评价摘要" value="REVIEW_SUMMARY"></el-option>
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
        <el-table-column prop="knowledgeType" label="知识类型" width="110">
          <template v-slot="scope">
            <el-tag>{{ knowledgeTypeName(scope.row.knowledgeType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="chunkNo" label="片段序号" width="90" />
        <el-table-column prop="chunkTitle" label="片段标题" min-width="220" show-overflow-tooltip />
        <el-table-column prop="characterCount" label="字符数" width="90" />
        <el-table-column prop="chunkStatus" label="状态" width="90">
          <template v-slot="scope">
            <el-tag type="success">{{ scope.row.chunkStatus === 'READY' ? '可使用' : scope.row.chunkStatus }}</el-tag>
          </template>
        </el-table-column>
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

    <el-dialog title="按资料生成切片" v-model="data.generateVisible" width="620px">
      <el-form label-width="96px" style="padding: 12px">
        <el-form-item label="选择资料">
          <el-select v-model="data.knowledgeId" filterable placeholder="请选择已启用的商品资料" style="width: 100%">
            <el-option v-for="item in data.knowledgeList" :key="item.id" :label="item.productName + ' / ' + item.title" :value="item.id"></el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="data.generateVisible = false">取消</el-button>
        <el-button type="primary" :loading="data.generateLoading" @click="generateOne">生成切片</el-button>
      </template>
    </el-dialog>

    <el-dialog title="切片详情" v-model="data.detailVisible" width="760px">
      <div class="detail" v-if="data.current">
        <h3>{{ data.current.chunkTitle }}</h3>
        <p>商品：{{ data.current.productName }} / {{ data.current.productNo }}</p>
        <p>来源资料：{{ data.current.knowledgeTitle }}</p>
        <p>类型：{{ knowledgeTypeName(data.current.knowledgeType) }}，字符数：{{ data.current.characterCount }}</p>
        <div>{{ data.current.chunkContent }}</div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive } from "vue";
import request from "@/utils/request.js";
import { ElMessage, ElMessageBox } from "element-plus";

const data = reactive({
  productName: null,
  knowledgeTitle: null,
  chunkContent: null,
  knowledgeType: null,
  tableData: [],
  knowledgeList: [],
  pageNum: 1,
  pageSize: 10,
  total: 0,
  ids: [],
  knowledgeId: null,
  generateVisible: false,
  // 生成操作耗时较长，用它控制按钮 loading 并防止重复提交
  generateLoading: false,
  detailVisible: false,
  current: null
})

const knowledgeTypeName = (type) => {
  const map = { FAQ: '常见问答', SELLING_POINT: '卖点说明', PARAMETER: '参数说明', AFTER_SALE: '售后说明', REVIEW_SUMMARY: '评价摘要' }
  return map[type] || type
}

const loadKnowledge = () => {
  request.get('/productKnowledge/selectAll', { params: { isEnabled: 1 } }).then(res => {
    if (res.code === '200') {
      data.knowledgeList = res.data || []
    }
  })
}

const load = () => {
  request.get('/productKnowledgeChunk/selectPage', {
    params: {
      pageNum: data.pageNum,
      pageSize: data.pageSize,
      productName: data.productName,
      knowledgeTitle: data.knowledgeTitle,
      chunkContent: data.chunkContent,
      knowledgeType: data.knowledgeType
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
  data.chunkContent = null
  data.knowledgeType = null
  data.pageNum = 1
  load()
}

const generateOne = () => {
  if (!data.knowledgeId) {
    ElMessage.warning('请选择要生成切片的商品资料')
    return
  }
  data.generateLoading = true
  request.post('/productKnowledgeChunk/generate/' + data.knowledgeId).then(res => {
    if (res.code === '200') {
      ElMessage.success('已生成 ' + res.data + ' 个切片')
      data.generateVisible = false
      load()
    } else {
      ElMessage.error(res.msg)
    }
  }).finally(() => {
    data.generateLoading = false
  })
}

const generateAll = () => {
  ElMessageBox.confirm('确认根据全部已启用商品资料重新生成切片吗？', '生成全部切片', { type: 'warning' }).then(() => {
    // 全量切片要遍历所有启用资料，耗时较长，loading 同时起到防重复点击的作用
    data.generateLoading = true
    request.post('/productKnowledgeChunk/generateAll').then(res => {
      if (res.code === '200') {
        ElMessage.success('已生成 ' + res.data + ' 个切片')
        load()
      } else {
        ElMessage.error(res.msg)
      }
    }).finally(() => {
      data.generateLoading = false
    })
  }).catch(() => {})
}

const deleteById = (id) => {
  ElMessageBox.confirm('确认删除该知识切片吗？', '删除切片', { type: 'warning' }).then(() => {
    request.delete('/productKnowledgeChunk/delete/' + id).then(res => {
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
    ElMessage.warning('请选择要删除的知识切片')
    return
  }
  ElMessageBox.confirm('确认批量删除选中的知识切片吗？', '批量删除', { type: 'warning' }).then(() => {
    request.delete('/productKnowledgeChunk/delete/batch', { data: data.ids }).then(res => {
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

loadKnowledge()
load()
</script>

<style scoped>
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
.principle-card h3 {
  margin: 0;
  color: #172033;
}

.page-heading p,
.principle-card p,
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

.principle-card {
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
}

.principle-card p {
  line-height: 1.7;
}

.metrics {
  display: flex;
  gap: 12px;
}

.metrics div {
  width: 96px;
  padding: 12px;
  border-radius: 8px;
  background: #f8fafc;
  text-align: center;
}

.metrics strong {
  display: block;
  color: #1d4ed8;
  font-size: 22px;
}

.metrics span {
  display: block;
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
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
