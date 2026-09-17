<template>
  <div>
    <div class="page-heading">
      <div>
        <h2>Prompt 模板</h2>
        <p>维护商城导购、商品问答、评价分析、运营报告等 AI 场景的提示词模板。</p>
      </div>
      <el-button type="primary" @click="handleAdd">新增模板</el-button>
    </div>

    <div class="card search-card">
      <el-input v-model="data.templateCode" @clear="reset" clearable @keyup.enter="load" prefix-icon="Search" class="search-item" placeholder="请输入模板编码"></el-input>
      <el-input v-model="data.templateName" @clear="reset" clearable @keyup.enter="load" prefix-icon="Search" class="search-item" placeholder="请输入模板名称"></el-input>
      <el-select v-model="data.businessType" clearable placeholder="业务类型" class="search-item" @change="load" @clear="reset">
        <el-option label="导购" value="GUIDE"></el-option>
        <el-option label="问答" value="QA"></el-option>
        <el-option label="评价" value="REVIEW"></el-option>
        <el-option label="运营" value="OPERATION"></el-option>
        <el-option label="售后" value="AFTER_SALE"></el-option>
      </el-select>
      <el-button type="primary" plain @click="load">查询</el-button>
      <el-button plain @click="reset">重置</el-button>
      <el-button type="danger" plain @click="delBatch">批量删除</el-button>
    </div>

    <div class="card" style="margin-bottom: 5px">
      <el-table stripe :data="data.tableData" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" />
        <el-table-column prop="templateCode" label="模板编码" min-width="190" />
        <el-table-column prop="templateName" label="模板名称" min-width="160" />
        <el-table-column prop="businessType" label="业务类型" width="110">
          <template v-slot="scope">
            <el-tag>{{ businessTypeName(scope.row.businessType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="systemPrompt" label="系统提示词" min-width="240" show-overflow-tooltip />
        <el-table-column prop="userPrompt" label="用户提示词" min-width="240" show-overflow-tooltip />
        <el-table-column prop="isEnabled" label="状态" width="90">
          <template v-slot="scope">
            <el-tag :type="scope.row.isEnabled === 1 ? 'success' : 'info'">
              {{ scope.row.isEnabled === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
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

    <el-dialog title="Prompt 模板" v-model="data.formVisible" width="860px" destroy-on-close>
      <el-form ref="formRef" :model="data.form" :rules="data.rules" label-width="96px" style="padding: 20px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item prop="templateCode" label="模板编码">
              <el-input v-model="data.form.templateCode" placeholder="例如 PRODUCT_QA_RAG"></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item prop="templateName" label="模板名称">
              <el-input v-model="data.form.templateName" placeholder="请输入模板名称"></el-input>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item prop="businessType" label="业务类型">
              <el-select v-model="data.form.businessType" placeholder="请选择业务类型" style="width: 100%">
                <el-option label="导购" value="GUIDE"></el-option>
                <el-option label="问答" value="QA"></el-option>
                <el-option label="评价" value="REVIEW"></el-option>
                <el-option label="运营" value="OPERATION"></el-option>
                <el-option label="售后" value="AFTER_SALE"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item prop="isEnabled" label="状态">
              <el-radio-group v-model="data.form.isEnabled">
                <el-radio-button :label="1">启用</el-radio-button>
                <el-radio-button :label="0">停用</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item prop="systemPrompt" label="系统提示词">
          <el-input v-model="data.form.systemPrompt" type="textarea" :rows="4" placeholder="定义 AI 的角色、边界和回答原则"></el-input>
        </el-form-item>
        <el-form-item prop="userPrompt" label="用户提示词">
          <el-input v-model="data.form.userPrompt" type="textarea" :rows="5" placeholder="定义业务输入变量和任务要求"></el-input>
        </el-form-item>
        <el-form-item prop="outputFormat" label="输出格式">
          <el-input v-model="data.form.outputFormat" type="textarea" :rows="4" placeholder="定义 JSON 字段或固定文本结构"></el-input>
        </el-form-item>
        <el-form-item prop="remark" label="用途说明">
          <el-input v-model="data.form.remark" type="textarea" :rows="2" placeholder="说明该模板适合的业务场景"></el-input>
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
  templateCode: null,
  templateName: null,
  businessType: null,
  ids: [],
  rules: {
    templateCode: [{ required: true, message: '请输入模板编码', trigger: 'blur' }],
    templateName: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
    businessType: [{ required: true, message: '请选择业务类型', trigger: 'change' }],
    systemPrompt: [{ required: true, message: '请输入系统提示词', trigger: 'blur' }],
    userPrompt: [{ required: true, message: '请输入用户提示词', trigger: 'blur' }]
  }
})

const businessTypeName = (type) => {
  const map = {
    GUIDE: '导购',
    QA: '问答',
    REVIEW: '评价',
    OPERATION: '运营',
    AFTER_SALE: '售后'
  }
  return map[type] || type
}

const load = () => {
  request.get('/promptTemplate/selectPage', {
    params: {
      pageNum: data.pageNum,
      pageSize: data.pageSize,
      templateCode: data.templateCode,
      templateName: data.templateName,
      businessType: data.businessType
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
    businessType: 'GUIDE',
    isEnabled: 1
  }
  data.formVisible = true
}

const handleEdit = (row) => {
  data.form = JSON.parse(JSON.stringify(row))
  data.formVisible = true
}

const add = () => {
  request.post('/promptTemplate/add', data.form).then(res => {
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
  request.put('/promptTemplate/update', data.form).then(res => {
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
    request.delete('/promptTemplate/delete/' + id).then(res => {
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
    request.delete("/promptTemplate/delete/batch", {data: data.ids}).then(res => {
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
  data.templateCode = null
  data.templateName = null
  data.businessType = null
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
