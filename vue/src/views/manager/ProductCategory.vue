<template>
  <div>
    <div class="page-heading">
      <div>
        <h2>商品分类</h2>
        <p>管理商城前台导航和商品归类，AI 导购会结合分类缩小商品推荐范围。</p>
      </div>
      <el-button type="primary" @click="handleAdd">新增分类</el-button>
    </div>

    <div class="card search-card">
      <el-input v-model="data.name" @clear="reset" clearable @keyup.enter="load" prefix-icon="Search" class="search-item" placeholder="请输入分类名称"></el-input>
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
        <el-table-column prop="icon" label="图标" width="90">
          <template v-slot="scope">
            <el-image v-if="scope.row.icon" class="category-icon" :src="scope.row.icon" :preview-src-list="[scope.row.icon]" preview-teleported></el-image>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="分类名称" min-width="150" />
        <el-table-column prop="sort" label="排序" width="90" />
        <el-table-column prop="isEnabled" label="状态" width="90">
          <template v-slot="scope">
            <el-tag :type="scope.row.isEnabled === 1 ? 'success' : 'info'">
              {{ scope.row.isEnabled === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="分类说明" min-width="200" show-overflow-tooltip />
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

    <el-dialog title="商品分类" v-model="data.formVisible" width="620px" destroy-on-close>
      <el-form ref="formRef" :model="data.form" :rules="data.rules" label-width="86px" style="padding: 20px">
        <el-row :gutter="12">
          <el-col :span="24">
            <el-form-item prop="name" label="分类名称">
              <el-input v-model="data.form.name" placeholder="请输入分类名称"></el-input>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item prop="sort" label="排序">
              <el-input-number v-model="data.form.sort" :min="1" :max="999" style="width: 100%"></el-input-number>
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
        <el-form-item prop="icon" label="分类图标">
          <el-upload :action="baseUrl + '/files/upload'" :on-success="handleIconUpload" list-type="picture">
            <el-button type="primary">上传图标</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item prop="remark" label="分类说明">
          <el-input v-model="data.form.remark" type="textarea" :rows="3" placeholder="请输入分类说明"></el-input>
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

const baseUrl = import.meta.env.VITE_BASE_URL
const formRef = ref()

const data = reactive({
  formVisible: false,
  form: {},
  tableData: [],
  pageNum: 1,
  pageSize: 10,
  total: 0,
  name: null,
  isEnabled: null,
  ids: [],
  rules: {
    name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }]
  }
})

const load = () => {
  request.get('/productCategory/selectPage', {
    params: {
      pageNum: data.pageNum,
      pageSize: data.pageSize,
      name: data.name,
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
    sort: 1,
    isEnabled: 1
  }
  data.formVisible = true
}

const handleEdit = (row) => {
  data.form = JSON.parse(JSON.stringify(row))
  data.formVisible = true
}

const add = () => {
  request.post('/productCategory/add', data.form).then(res => {
    if (res.code === '200') {
      ElMessage.success('操作成功')
      data.formVisible = false
      loadParentOptions()
      load()
    } else {
      ElMessage.error(res.msg)
    }
  })
}

const update = () => {
  request.put('/productCategory/update', data.form).then(res => {
    if (res.code === '200') {
      ElMessage.success('操作成功')
      data.formVisible = false
      loadParentOptions()
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
  ElMessageBox.confirm('删除分类前请确认该分类下没有子分类，您确定删除吗？', '删除确认', { type: 'warning' }).then(() => {
    request.delete('/productCategory/delete/' + id).then(res => {
      if (res.code === '200') {
        ElMessage.success("删除成功")
        loadParentOptions()
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
  ElMessageBox.confirm('删除分类前请确认所选分类下没有子分类，您确定删除吗？', '删除确认', { type: 'warning' }).then(() => {
    request.delete("/productCategory/delete/batch", {data: data.ids}).then(res => {
      if (res.code === '200') {
        ElMessage.success('操作成功')
        loadParentOptions()
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

const handleIconUpload = (res) => {
  data.form.icon = res.data
}

const reset = () => {
  data.name = null
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

.category-icon {
  width: 42px;
  height: 42px;
  border-radius: 8px;
  display: block;
  object-fit: cover;
}
</style>
