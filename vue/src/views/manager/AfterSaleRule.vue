<template>
  <div>
    <div class="page-heading">
      <div>
        <h2>售后规则管理</h2>
        <p>维护退换货、质保、退款、物流等售后规则，为客服问答和后续 AI 售后咨询提供规则依据。</p>
      </div>
      <el-button type="primary" @click="handleAdd">新增规则</el-button>
    </div>

    <div class="card search-card">
      <el-input v-model="data.ruleName" clearable @keyup.enter="load" @clear="reset" prefix-icon="Search" class="search-item" placeholder="请输入规则名称"></el-input>
      <el-select v-model="data.ruleType" clearable placeholder="规则类型" class="search-item" @change="load" @clear="load">
        <el-option label="退货" value="RETURN"></el-option>
        <el-option label="换货" value="EXCHANGE"></el-option>
        <el-option label="质保" value="WARRANTY"></el-option>
        <el-option label="退款" value="REFUND"></el-option>
        <el-option label="物流" value="LOGISTICS"></el-option>
      </el-select>
      <el-select v-model="data.categoryId" clearable filterable placeholder="商品分类" class="search-item" @change="load" @clear="load">
        <el-option v-for="item in data.categories" :key="item.id" :label="item.name" :value="item.id"></el-option>
      </el-select>
      <el-select v-model="data.isEnabled" clearable placeholder="启用状态" class="search-item" @change="load" @clear="load">
        <el-option label="启用" :value="1"></el-option>
        <el-option label="停用" :value="0"></el-option>
      </el-select>
      <el-button type="primary" plain @click="load">查询</el-button>
      <el-button plain @click="reset">重置</el-button>
      <el-button type="danger" plain @click="deleteBatch">批量删除</el-button>
    </div>

    <div class="card" style="margin-bottom: 12px">
      <el-table stripe :data="data.tableData" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" />
        <el-table-column prop="ruleName" label="规则名称" min-width="180" />
        <el-table-column prop="ruleType" label="类型" width="100">
          <template v-slot="scope">
            <el-tag>{{ ruleTypeName(scope.row.ruleType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="categoryName" label="适用分类" width="120">
          <template v-slot="scope">{{ scope.row.categoryName || '全部分类' }}</template>
        </el-table-column>
        <el-table-column prop="applyScene" label="适用场景" min-width="180" show-overflow-tooltip />
        <el-table-column prop="timeLimit" label="处理时效" width="130" />
        <el-table-column prop="isEnabled" label="状态" width="90">
          <template v-slot="scope">
            <el-tag :type="scope.row.isEnabled === 1 ? 'success' : 'info'">{{ scope.row.isEnabled === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column label="操作" width="210" fixed="right">
          <template v-slot="scope">
            <el-button type="primary" plain @click="handleEdit(scope.row)">编辑</el-button>
            <el-button type="primary" link @click="showDetail(scope.row)">详情</el-button>
            <el-button type="danger" plain @click="deleteById(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <div class="card" v-if="data.total">
      <el-pagination @current-change="load" background layout="prev, pager, next" :page-size="data.pageSize" v-model:current-page="data.pageNum" :total="data.total" />
    </div>

    <el-dialog :title="data.form.id ? '编辑售后规则' : '新增售后规则'" v-model="data.formVisible" width="760px" destroy-on-close>
      <el-form ref="formRef" :model="data.form" :rules="data.rules" label-width="92px" style="padding: 12px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="规则名称" prop="ruleName">
              <el-input v-model="data.form.ruleName" placeholder="请输入规则名称"></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="规则类型" prop="ruleType">
              <el-select v-model="data.form.ruleType" placeholder="请选择规则类型" style="width: 100%">
                <el-option label="退货" value="RETURN"></el-option>
                <el-option label="换货" value="EXCHANGE"></el-option>
                <el-option label="质保" value="WARRANTY"></el-option>
                <el-option label="退款" value="REFUND"></el-option>
                <el-option label="物流" value="LOGISTICS"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="商品分类">
              <el-select v-model="data.form.categoryId" clearable filterable placeholder="不选则适用全部分类" style="width: 100%">
                <el-option v-for="item in data.categories" :key="item.id" :label="item.name" :value="item.id"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="处理时效">
              <el-input v-model="data.form.timeLimit" placeholder="例如：1-3 个工作日"></el-input>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="适用场景" prop="applyScene">
          <el-input v-model="data.form.applyScene" placeholder="请输入适用场景"></el-input>
        </el-form-item>
        <el-form-item label="适用条件" prop="conditionText">
          <el-input v-model="data.form.conditionText" type="textarea" :rows="4" placeholder="请输入售后规则适用条件"></el-input>
        </el-form-item>
        <el-form-item label="处理流程" prop="processText">
          <el-input v-model="data.form.processText" type="textarea" :rows="4" placeholder="请输入用户申请后如何处理"></el-input>
        </el-form-item>
        <el-form-item label="联系渠道">
          <el-input v-model="data.form.contactChannel" placeholder="例如：在线客服、客服电话、订单售后入口"></el-input>
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="启用状态">
              <el-switch v-model="data.form.isEnabled" :active-value="1" :inactive-value="0"></el-switch>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序">
              <el-input-number v-model="data.form.sort" :min="1" style="width: 100%"></el-input-number>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="data.formVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog title="规则详情" v-model="data.detailVisible" width="720px">
      <div class="detail" v-if="data.current">
        <h3>{{ data.current.ruleName }}</h3>
        <p>规则类型：{{ ruleTypeName(data.current.ruleType) }}</p>
        <p>适用分类：{{ data.current.categoryName || '全部分类' }}</p>
        <p>适用场景：{{ data.current.applyScene }}</p>
        <h4>适用条件</h4>
        <div>{{ data.current.conditionText }}</div>
        <h4>处理流程</h4>
        <div>{{ data.current.processText }}</div>
        <p>处理时效：{{ data.current.timeLimit }}</p>
        <p>联系渠道：{{ data.current.contactChannel }}</p>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref } from "vue";
import request from "@/utils/request.js";
import { ElMessage, ElMessageBox } from "element-plus";

const formRef = ref()
const data = reactive({
  ruleName: null,
  ruleType: null,
  categoryId: null,
  isEnabled: null,
  tableData: [],
  categories: [],
  pageNum: 1,
  pageSize: 10,
  total: 0,
  ids: [],
  formVisible: false,
  detailVisible: false,
  form: {},
  current: null,
  rules: {
    ruleName: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
    ruleType: [{ required: true, message: '请选择规则类型', trigger: 'change' }],
    applyScene: [{ required: true, message: '请输入适用场景', trigger: 'blur' }],
    conditionText: [{ required: true, message: '请输入适用条件', trigger: 'blur' }],
    processText: [{ required: true, message: '请输入处理流程', trigger: 'blur' }]
  }
})

const ruleTypeName = (type) => {
  const map = { RETURN: '退货', EXCHANGE: '换货', WARRANTY: '质保', REFUND: '退款', LOGISTICS: '物流' }
  return map[type] || type
}

const loadCategories = () => {
  request.get('/productCategory/selectAll', { params: { isEnabled: 1 } }).then(res => {
    if (res.code === '200') {
      data.categories = res.data || []
    }
  })
}

const load = () => {
  request.get('/afterSaleRule/selectPage', {
    params: {
      pageNum: data.pageNum,
      pageSize: data.pageSize,
      ruleName: data.ruleName,
      ruleType: data.ruleType,
      categoryId: data.categoryId,
      isEnabled: data.isEnabled
    }
  }).then(res => {
    if (res.code === '200') {
      data.tableData = res.data?.list || []
      data.total = res.data?.total || 0
    }
  })
}

const reset = () => {
  data.ruleName = null
  data.ruleType = null
  data.categoryId = null
  data.isEnabled = null
  data.pageNum = 1
  load()
}

const handleAdd = () => {
  data.form = { isEnabled: 1, sort: 1 }
  data.formVisible = true
}

const handleEdit = (row) => {
  data.form = JSON.parse(JSON.stringify(row))
  data.formVisible = true
}

const save = () => {
  formRef.value.validate(valid => {
    if (!valid) {
      return
    }
    const requestApi = data.form.id ? request.put('/afterSaleRule/update', data.form) : request.post('/afterSaleRule/add', data.form)
    requestApi.then(res => {
      if (res.code === '200') {
        ElMessage.success('保存成功')
        data.formVisible = false
        load()
      } else {
        ElMessage.error(res.msg)
      }
    })
  })
}

const deleteById = (id) => {
  ElMessageBox.confirm('确认删除该售后规则吗？', '删除规则', { type: 'warning' }).then(() => {
    request.delete('/afterSaleRule/delete/' + id).then(res => {
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
    ElMessage.warning('请选择要删除的规则')
    return
  }
  ElMessageBox.confirm('确认批量删除选中的售后规则吗？', '批量删除', { type: 'warning' }).then(() => {
    request.delete('/afterSaleRule/delete/batch', { data: data.ids }).then(res => {
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

loadCategories()
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

.page-heading h2 {
  margin: 0;
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
  padding: 14px;
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

.detail h4 {
  margin: 18px 0 8px;
  color: #172033;
}

.detail p {
  margin: 8px 0;
  color: #64748b;
}

.detail div {
  padding: 12px;
  color: #334155;
  line-height: 1.8;
  background: #f8fafc;
  border-radius: 8px;
}
</style>
