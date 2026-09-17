<template>
  <div>
    <div class="page-heading">
      <div>
        <h2>AI智能导购任务</h2>
        <p>维护用户购物需求，执行导购任务后生成结构化推荐结果。</p>
      </div>
      <el-button type="primary" @click="handleAdd">新增任务</el-button>
    </div>

    <div class="card search-card">
      <el-input v-model="data.taskNo" @clear="reset" clearable @keyup.enter="load" prefix-icon="Search" class="search-item" placeholder="请输入任务编号"></el-input>
      <el-input v-model="data.demandText" @clear="reset" clearable @keyup.enter="load" prefix-icon="Search" class="search-item" placeholder="请输入购物需求"></el-input>
      <el-select v-model="data.status" clearable placeholder="任务状态" class="search-item" @change="load" @clear="reset">
        <el-option label="待执行" value="WAITING"></el-option>
        <el-option label="已完成" value="DONE"></el-option>
        <el-option label="执行失败" value="FAILED"></el-option>
      </el-select>
      <el-button type="primary" plain @click="load">查询</el-button>
      <el-button plain @click="reset">重置</el-button>
      <el-button type="danger" plain @click="delBatch">批量删除</el-button>
    </div>

    <div class="card" style="margin-bottom: 5px">
      <el-table stripe :data="data.tableData" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" />
        <el-table-column prop="taskNo" label="任务编号" width="190" />
        <el-table-column prop="userName" label="用户" width="120" />
        <el-table-column prop="demandText" label="购物需求" min-width="260" show-overflow-tooltip />
        <el-table-column prop="budgetAmount" label="预算" width="110" />
        <el-table-column prop="productName" label="基准商品" min-width="180" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="statusType(scope.row.status)">{{ statusName(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="executeTime" label="执行时间" width="170" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="scope">
            <el-button type="primary" circle :icon="Edit" @click="handleEdit(scope.row)"></el-button>
            <el-button type="success" circle :icon="VideoPlay" @click="execute(scope.row.id)"></el-button>
            <el-button type="info" circle :icon="View" @click="viewResult(scope.row)"></el-button>
            <el-button type="danger" circle :icon="Delete" @click="del(scope.row.id)"></el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <div class="card" v-if="data.total">
      <el-pagination @current-change="load" background layout="prev, pager, next" :page-size="data.pageSize" v-model:current-page="data.pageNum" :total="data.total" />
    </div>

    <el-dialog title="导购任务" v-model="data.formVisible" width="760px" destroy-on-close>
      <el-form ref="formRef" :model="data.form" :rules="data.rules" label-width="92px" style="padding: 20px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="任务编号">
              <el-input v-model="data.form.taskNo" placeholder="不填则自动生成"></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="发起人">
              <!-- 后台的导购任务是管理员自己在测，固定挂在自己名下，不让记录出现在别的用户那里 -->
              <el-input :value="data.form.userName || data.user.name || data.user.username" disabled></el-input>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="预算">
              <el-input-number v-model="data.form.budgetAmount" :min="0" :precision="2" :step="100" style="width: 100%"></el-input-number>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="基准商品">
              <el-select v-model="data.form.productId" clearable filterable placeholder="可选，用于相似商品召回" style="width: 100%">
                <el-option v-for="item in data.productOptions" :key="item.id" :label="item.name" :value="item.id"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item prop="demandText" label="购物需求">
          <el-input v-model="data.form.demandText" type="textarea" :rows="5" placeholder="例如：想买一台适合大学生写代码和做毕设的笔记本，预算5000左右"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="data.formVisible = false">取消</el-button>
          <el-button type="primary" @click="save">确定</el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog title="导购结果" v-model="data.resultVisible" width="820px">
      <el-descriptions :column="2" border v-if="data.current">
        <el-descriptions-item label="任务编号">{{ data.current.taskNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ statusName(data.current.status) }}</el-descriptions-item>
        <el-descriptions-item label="匹配商品ID">{{ data.current.matchedProductIds }}</el-descriptions-item>
        <el-descriptions-item label="执行时间">{{ data.current.executeTime }}</el-descriptions-item>
        <el-descriptions-item label="执行消息" :span="2">{{ data.current.executeMessage }}</el-descriptions-item>
      </el-descriptions>
      <pre class="result-text">{{ data.current?.recommendationResult || '暂无推荐结果，请先执行导购任务。' }}</pre>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref } from "vue";
import request from "@/utils/request.js";
import { ElMessage, ElMessageBox } from "element-plus";
import { Delete, Edit, VideoPlay, View } from "@element-plus/icons-vue";

const formRef = ref()

const data = reactive({
  // 当前登录的管理员，新建任务时作为发起人
  user: JSON.parse(localStorage.getItem('sys-user') || '{}'),
  formVisible: false,
  resultVisible: false,
  form: {},
  current: null,
  tableData: [],
  productOptions: [],
  pageNum: 1,
  pageSize: 10,
  total: 0,
  taskNo: null,
  demandText: null,
  status: null,
  ids: [],
  rules: {
    demandText: [{ required: true, message: '请输入购物需求', trigger: 'blur' }]
  }
})

const statusName = (status) => {
  if (status === 'DONE') {
    return '已完成'
  }
  if (status === 'FAILED') {
    return '执行失败'
  }
  return '待执行'
}

const statusType = (status) => {
  if (status === 'DONE') {
    return 'success'
  }
  if (status === 'FAILED') {
    return 'danger'
  }
  return 'warning'
}

const loadOptions = () => {
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

const load = () => {
  request.get('/shoppingGuideTask/selectPage', {
    params: {
      pageNum: data.pageNum,
      pageSize: data.pageSize,
      taskNo: data.taskNo,
      demandText: data.demandText,
      status: data.status
    }
  }).then(res => {
    if (res.code === '200') {
      data.tableData = res.data?.list || []
      data.total = res.data?.total
    }
  })
}

const handleAdd = () => {
  data.form = {
    // 发起人固定为当前登录的管理员，用户画像工具也读这个 ID
    userId: data.user.id,
    budgetAmount: 5000,
    demandText: ''
  }
  data.formVisible = true
}

const handleEdit = (row) => {
  data.form = JSON.parse(JSON.stringify(row))
  data.formVisible = true
}

const add = () => {
  request.post('/shoppingGuideTask/add', data.form).then(res => {
    if (res.code === '200') {
      ElMessage.success('操作成功')
      data.formVisible = false
      load()
    } else {
      ElMessage.error(res.msg)
    }
  })
}

const update = () => {
  request.put('/shoppingGuideTask/update', data.form).then(res => {
    if (res.code === '200') {
      ElMessage.success('操作成功')
      data.formVisible = false
      load()
    } else {
      ElMessage.error(res.msg)
    }
  })
}

const save = () => {
  formRef.value.validate(valid => {
    if (valid) {
      data.form.id ? update() : add()
    }
  })
}

const execute = (id) => {
  request.post('/shoppingGuideTask/execute/' + id).then(res => {
    if (res.code === '200') {
      ElMessage.success('导购任务执行完成')
      data.current = res.data
      data.resultVisible = true
      load()
    } else {
      ElMessage.error(res.msg)
    }
  })
}

const viewResult = (row) => {
  data.current = JSON.parse(JSON.stringify(row))
  data.resultVisible = true
}

const del = (id) => {
  ElMessageBox.confirm('删除后数据无法恢复，确定删除吗？', '删除确认', { type: 'warning' }).then(() => {
    request.delete('/shoppingGuideTask/delete/' + id).then(res => {
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
    request.delete('/shoppingGuideTask/delete/batch', { data: data.ids }).then(res => {
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

const reset = () => {
  data.taskNo = null
  data.demandText = null
  data.status = null
  data.pageNum = 1
  load()
}

loadOptions()
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

.result-text {
  margin-top: 14px;
  padding: 14px;
  color: #334155;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
  background: #f8fafc;
  border-radius: 8px;
}
</style>
