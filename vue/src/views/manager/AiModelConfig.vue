<template>
  <div>
    <div class="page-heading">
      <div>
        <h2>AI 模型配置</h2>
        <p>维护商城导购、商品问答、评价分析和运营报告使用的大模型连接参数。</p>
      </div>
      <el-button type="primary" @click="handleAdd">新增模型</el-button>
    </div>

    <div class="card search-card">
      <el-input v-model="data.provider" @clear="reset" clearable @keyup.enter="load" prefix-icon="Search" class="search-item" placeholder="请输入供应商"></el-input>
      <el-input v-model="data.modelName" @clear="reset" clearable @keyup.enter="load" prefix-icon="Search" class="search-item" placeholder="请输入模型名称"></el-input>
      <el-select v-model="data.isEnabled" clearable placeholder="启用状态" class="search-item" @change="load" @clear="reset">
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
        <el-table-column prop="provider" label="供应商" min-width="120" />
        <el-table-column prop="modelName" label="模型名称" min-width="170" />
        <el-table-column prop="modelType" label="类型" width="90">
          <template v-slot="scope">
            <el-tag :type="scope.row.modelType === 'EMBEDDING' ? 'warning' : 'primary'">
              {{ scope.row.modelType === 'EMBEDDING' ? '向量' : '对话' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="baseUrl" label="接口地址" min-width="260" show-overflow-tooltip />
        <el-table-column prop="temperature" label="温度" width="90" />
        <el-table-column prop="maxTokens" label="最大输出" width="110" />
        <el-table-column prop="isEnabled" label="状态" width="90">
          <template v-slot="scope">
            <el-tag :type="scope.row.isEnabled === 1 ? 'success' : 'info'">
              {{ scope.row.isEnabled === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="用途说明" min-width="180" show-overflow-tooltip />
        <el-table-column prop="updateTime" label="更新时间" min-width="160" />
        <el-table-column label="操作" width="120" fixed="right">
          <template v-slot="scope">
            <el-button type="primary" circle :icon="Edit" @click="handleEdit(scope.row)"></el-button>
            <el-button type="danger" circle :icon="Delete" @click="del(scope.row.id)"></el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <div class="card" v-if="data.total">
      <el-pagination @current-change="load" background layout="prev, pager, next" :page-size="data.pageSize" v-model:current-page="data.pageNum" :total="data.total" />
    </div>

    <el-dialog title="AI 模型配置" v-model="data.formVisible" width="680px" destroy-on-close>
      <el-form ref="formRef" :model="data.form" :rules="data.rules" label-width="96px" style="padding: 20px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item prop="provider" label="供应商">
              <el-input v-model="data.form.provider" placeholder="例如 DeepSeek"></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item prop="modelName" label="模型名称">
              <el-input v-model="data.form.modelName" placeholder="例如 deepseek-chat"></el-input>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item prop="modelType" label="模型类型">
          <el-radio-group v-model="data.form.modelType">
            <el-radio-button label="CHAT">对话模型</el-radio-button>
            <el-radio-button label="EMBEDDING">向量模型</el-radio-button>
          </el-radio-group>
          <span style="margin-left: 12px; color: #94a3b8; font-size: 12px">对话模型用于问答/推荐/分析；向量模型用于知识库检索（Embedding）</span>
        </el-form-item>
        <el-form-item prop="baseUrl" label="接口地址">
          <el-input v-model="data.form.baseUrl" placeholder="对话填 .../v1/chat/completions，向量填 .../v1/embeddings"></el-input>
        </el-form-item>
        <el-form-item prop="apiKey" label="API Key">
          <el-input v-model="data.form.apiKey" placeholder="请输入接口密钥" show-password></el-input>
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item prop="temperature" label="温度">
              <el-input-number v-model="data.form.temperature" :min="0" :max="2" :step="0.1" style="width: 100%"></el-input-number>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item prop="maxTokens" label="最大输出">
              <el-input-number v-model="data.form.maxTokens" :min="256" :max="8192" :step="256" style="width: 100%"></el-input-number>
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
        <el-form-item prop="remark" label="用途说明">
          <el-input v-model="data.form.remark" type="textarea" :rows="3" placeholder="说明该模型适合哪些 AI 业务"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="data.formVisible = false">取消</el-button>
          <el-button type="primary" @click="save">确定</el-button>
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

const formRef = ref()

const data = reactive({
  formVisible: false,
  form: {},
  tableData: [],
  pageNum: 1,
  pageSize: 10,
  total: 0,
  provider: null,
  modelName: null,
  isEnabled: null,
  ids: [],
  rules: {
    provider: [{ required: true, message: '请输入供应商', trigger: 'blur' }],
    modelName: [{ required: true, message: '请输入模型名称', trigger: 'blur' }],
    baseUrl: [{ required: true, message: '请输入接口地址', trigger: 'blur' }],
    apiKey: [{ required: true, message: '请输入 API Key', trigger: 'blur' }]
  }
})

const load = () => {
  request.get('/aiModelConfig/selectPage', {
    params: {
      pageNum: data.pageNum,
      pageSize: data.pageSize,
      provider: data.provider,
      modelName: data.modelName,
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
    modelType: 'CHAT',
    temperature: 0.7,
    maxTokens: 2048,
    isEnabled: 1
  }
  data.formVisible = true
}

const handleEdit = (row) => {
  data.form = JSON.parse(JSON.stringify(row))
  data.formVisible = true
}

const add = () => {
  request.post('/aiModelConfig/add', data.form).then(res => {
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
  request.put('/aiModelConfig/update', data.form).then(res => {
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
    request.delete('/aiModelConfig/delete/' + id).then(res => {
      if (res.code === '200') {
        ElMessage.success("删除成功")
        load()
      } else {
        ElMessage.error(res.msg)
      }
    })
  })
}

const delBatch = () => {
  if (!data.ids.length) {
    ElMessage.warning("请选择数据")
    return
  }
  ElMessageBox.confirm('删除后数据无法恢复，您确定删除吗？', '删除确认', { type: 'warning' }).then(() => {
    request.delete("/aiModelConfig/delete/batch", {data: data.ids}).then(res => {
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
  data.provider = null
  data.modelName = null
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
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 5px;
}

.search-item {
  width: 220px;
}
</style>
