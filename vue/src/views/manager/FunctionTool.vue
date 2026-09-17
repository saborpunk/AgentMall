<template>
  <div>
    <div class="page-heading">
      <div>
        <h2>Function Tool 工具中心</h2>
        <p>维护 AI Agent 可识别的商城业务工具，统一管理工具编码、调用方式、入参 Schema 和出参 Schema。</p>
      </div>
      <el-button type="primary" @click="handleAdd">新增工具</el-button>
    </div>

    <div class="card search-card">
      <el-input v-model="data.toolCode" @clear="reset" clearable @keyup.enter="load" prefix-icon="Search" class="search-item" placeholder="请输入工具编码"></el-input>
      <el-input v-model="data.toolName" @clear="reset" clearable @keyup.enter="load" prefix-icon="Search" class="search-item" placeholder="请输入工具名称"></el-input>
      <el-select v-model="data.toolType" clearable placeholder="工具类型" class="search-item" @change="load" @clear="reset">
        <el-option label="商品" value="PRODUCT"></el-option>
        <el-option label="库存" value="STOCK"></el-option>
        <el-option label="订单" value="ORDER"></el-option>
        <el-option label="用户" value="USER"></el-option>
        <el-option label="售后" value="AFTER_SALE"></el-option>
      </el-select>
      <el-select v-model="data.isEnabled" clearable placeholder="状态" class="search-item" @change="load" @clear="reset">
        <el-option label="启用" :value="1"></el-option>
        <el-option label="停用" :value="0"></el-option>
      </el-select>
      <el-button type="primary" plain @click="load">查询</el-button>
      <el-button plain @click="reset">重置</el-button>
      <el-button type="danger" plain @click="delBatch">批量删除</el-button>
    </div>

    <div class="card" style="margin-bottom: 5px">
      <el-table stripe :data="data.tableData" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" />
        <el-table-column prop="toolCode" label="工具编码" min-width="210" show-overflow-tooltip />
        <el-table-column prop="toolName" label="工具名称" min-width="180" />
        <el-table-column prop="toolType" label="类型" width="100">
          <template v-slot="scope">
            <el-tag>{{ toolTypeName(scope.row.toolType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="invokeType" label="调用方式" width="110" />
        <el-table-column prop="serviceBean" label="Service Bean" min-width="160" show-overflow-tooltip />
        <el-table-column prop="serviceMethod" label="方法名" min-width="150" show-overflow-tooltip />
        <el-table-column prop="isEnabled" label="状态" width="90">
          <template v-slot="scope">
            <el-tag :type="scope.row.isEnabled === 1 ? 'success' : 'info'">
              {{ scope.row.isEnabled === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column prop="updateTime" label="更新时间" min-width="160" />
        <el-table-column label="操作" width="170" fixed="right">
          <template v-slot="scope">
            <el-button type="primary" link @click="showDetail(scope.row)">详情</el-button>
            <el-button type="primary" circle :icon="Edit" @click="handleEdit(scope.row)"></el-button>
            <el-button type="danger" circle :icon="Delete" @click="del(scope.row.id)"></el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <div class="card" v-if="data.total">
      <el-pagination @current-change="load" background layout="prev, pager, next" :page-size="data.pageSize" v-model:current-page="data.pageNum" :total="data.total" />
    </div>

    <el-dialog title="Function Tool" v-model="data.formVisible" width="920px" destroy-on-close>
      <el-form ref="formRef" :model="data.form" :rules="data.rules" label-width="108px" style="padding: 20px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item prop="toolCode" label="工具编码">
              <el-input v-model="data.form.toolCode" placeholder="例如 PRODUCT_SELECT_BY_ID"></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item prop="toolName" label="工具名称">
              <el-input v-model="data.form.toolName" placeholder="请输入工具名称"></el-input>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item prop="toolType" label="工具类型">
              <el-select v-model="data.form.toolType" placeholder="请选择工具类型" style="width: 100%">
                <el-option label="商品" value="PRODUCT"></el-option>
                <el-option label="库存" value="STOCK"></el-option>
                <el-option label="订单" value="ORDER"></el-option>
                <el-option label="用户" value="USER"></el-option>
                <el-option label="售后" value="AFTER_SALE"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item prop="invokeType" label="调用方式">
              <el-select v-model="data.form.invokeType" placeholder="请选择调用方式" style="width: 100%">
                <el-option label="Spring Service" value="SERVICE"></el-option>
                <el-option label="HTTP 接口" value="HTTP"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item prop="isEnabled" label="状态">
              <el-radio-group v-model="data.form.isEnabled">
                <el-radio-button :label="1">启用</el-radio-button>
                <el-radio-button :label="0">停用</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="10">
            <el-form-item prop="serviceBean" label="Service Bean">
              <el-input v-model="data.form.serviceBean" placeholder="例如 productService"></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="10">
            <el-form-item prop="serviceMethod" label="Service方法">
              <el-input v-model="data.form.serviceMethod" placeholder="例如 selectById"></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="4">
            <el-form-item prop="sort" label="排序">
              <el-input-number v-model="data.form.sort" :min="1" :max="999" style="width: 100%"></el-input-number>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item prop="inputSchema" label="入参Schema">
          <el-input v-model="data.form.inputSchema" type="textarea" :rows="6" placeholder="请输入 JSON Schema"></el-input>
        </el-form-item>
        <el-form-item prop="outputSchema" label="出参Schema">
          <el-input v-model="data.form.outputSchema" type="textarea" :rows="5" placeholder="请输入 JSON Schema"></el-input>
        </el-form-item>
        <el-form-item prop="remark" label="工具说明">
          <el-input v-model="data.form.remark" type="textarea" :rows="2" placeholder="说明该工具适合哪些 Agent 场景"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="data.formVisible = false">取消</el-button>
          <el-button type="primary" @click="save">确定</el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog title="工具详情" v-model="data.detailVisible" width="860px">
      <div class="detail" v-if="data.current">
        <h3>{{ data.current.toolName }}</h3>
        <p>编码：{{ data.current.toolCode }}</p>
        <p>类型：{{ toolTypeName(data.current.toolType) }} / 调用方式：{{ data.current.invokeType }}</p>
        <p>目标：{{ data.current.serviceBean }}.{{ data.current.serviceMethod }}</p>
        <h4>入参 Schema</h4>
        <pre>{{ data.current.inputSchema }}</pre>
        <h4>出参 Schema</h4>
        <pre>{{ data.current.outputSchema }}</pre>
        <h4>说明</h4>
        <div>{{ data.current.remark || '暂无说明' }}</div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref } from "vue";
import request from "@/utils/request.js";
import { ElMessage, ElMessageBox } from "element-plus";
import { Delete, Edit } from "@element-plus/icons-vue";

const formRef = ref()

const data = reactive({
  formVisible: false,
  detailVisible: false,
  form: {},
  current: null,
  tableData: [],
  pageNum: 1,
  pageSize: 10,
  total: 0,
  toolCode: null,
  toolName: null,
  toolType: null,
  isEnabled: null,
  ids: [],
  rules: {
    toolCode: [{ required: true, message: '请输入工具编码', trigger: 'blur' }],
    toolName: [{ required: true, message: '请输入工具名称', trigger: 'blur' }],
    toolType: [{ required: true, message: '请选择工具类型', trigger: 'change' }],
    invokeType: [{ required: true, message: '请选择调用方式', trigger: 'change' }],
    serviceBean: [{ required: true, message: '请输入 Service Bean', trigger: 'blur' }],
    serviceMethod: [{ required: true, message: '请输入 Service 方法', trigger: 'blur' }],
    inputSchema: [{ required: true, message: '请输入入参 Schema', trigger: 'blur' }],
    outputSchema: [{ required: true, message: '请输入出参 Schema', trigger: 'blur' }]
  }
})

const toolTypeName = (type) => {
  const map = {
    PRODUCT: '商品',
    STOCK: '库存',
    ORDER: '订单',
    USER: '用户',
    AFTER_SALE: '售后'
  }
  return map[type] || type
}

const load = () => {
  request.get('/functionTool/selectPage', {
    params: {
      pageNum: data.pageNum,
      pageSize: data.pageSize,
      toolCode: data.toolCode,
      toolName: data.toolName,
      toolType: data.toolType,
      isEnabled: data.isEnabled
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
    toolType: 'PRODUCT',
    invokeType: 'SERVICE',
    isEnabled: 1,
    sort: 1
  }
  data.formVisible = true
}

const handleEdit = (row) => {
  data.form = JSON.parse(JSON.stringify(row))
  data.formVisible = true
}

const showDetail = (row) => {
  data.current = row
  data.detailVisible = true
}

const add = () => {
  request.post('/functionTool/add', data.form).then(res => {
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
  request.put('/functionTool/update', data.form).then(res => {
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

const del = (id) => {
  ElMessageBox.confirm('删除后数据无法恢复，您确定删除吗？', '删除确认', { type: 'warning' }).then(() => {
    request.delete('/functionTool/delete/' + id).then(res => {
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
  ElMessageBox.confirm('删除后数据无法恢复，您确定删除吗？', '删除确认', { type: 'warning' }).then(() => {
    request.delete('/functionTool/delete/batch', { data: data.ids }).then(res => {
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
  data.toolCode = null
  data.toolName = null
  data.toolType = null
  data.isEnabled = null
  data.pageNum = 1
  load()
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

.page-heading h2,
.detail h3,
.detail h4 {
  margin: 0;
  color: #172033;
}

.page-heading p,
.detail p {
  margin: 8px 0 0;
  color: #64748b;
}

.search-card {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 5px;
}

.search-item {
  width: 210px;
}

.detail h4 {
  margin-top: 16px;
}

.detail pre,
.detail div {
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

