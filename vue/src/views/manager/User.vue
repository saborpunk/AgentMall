<template>
  <div>
    <div class="page-heading">
      <div>
        <h2>用户管理</h2>
        <p>管理商城管理员和普通用户账号，后续订单、导购任务和评价数据都会关联到用户。</p>
      </div>
      <el-button type="primary" @click="handleAdd">新增用户</el-button>
    </div>

    <div class="card search-card">
      <el-input v-model="data.username" @clear="reset" clearable @keyup.enter="load" prefix-icon="Search" class="search-item" placeholder="请输入账号"></el-input>
      <el-input v-model="data.name" @clear="reset" clearable @keyup.enter="load" prefix-icon="Search" class="search-item" placeholder="请输入姓名"></el-input>
      <el-select v-model="data.role" clearable placeholder="请选择角色" class="search-item" @change="load" @clear="reset">
        <el-option label="管理员" value="ADMIN"></el-option>
        <el-option label="普通用户" value="USER"></el-option>
      </el-select>
      <el-button type="primary" plain @click="load">查询</el-button>
      <el-button plain @click="reset">重置</el-button>
      <el-button type="danger" plain @click="delBatch">批量删除</el-button>
    </div>

    <div class="card" style="margin-bottom: 5px">
      <el-table stripe :data="data.tableData" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" />
        <el-table-column prop="username" label="账号" min-width="120" />
        <el-table-column prop="avatar" label="头像" width="90">
          <template v-slot="scope">
            <el-image style="width: 40px; height: 40px; border-radius: 50%; display: block" v-if="scope.row.avatar"
                      :src="scope.row.avatar" :preview-src-list="[scope.row.avatar]" preview-teleported></el-image>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="姓名" min-width="120" />
        <el-table-column prop="role" label="角色" width="120">
          <template v-slot="scope">
            <el-tag :type="roleTagType(scope.row.role)">{{ roleName(scope.row.role) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="电话" min-width="130" />
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column prop="balance" label="余额" width="120">
          <template v-slot="scope">
            <span class="money-text">￥{{ formatMoney(scope.row.balance) }}</span>
          </template>
        </el-table-column>
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

    <el-dialog title="用户信息" v-model="data.formVisible" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="data.form" :rules="data.rules" label-width="82px" style="padding: 20px">
        <el-form-item prop="username" label="账号">
          <el-input v-model="data.form.username" placeholder="请输入账号"></el-input>
        </el-form-item>
        <el-form-item prop="password" label="密码">
          <el-input v-model="data.form.password" placeholder="请输入密码" show-password></el-input>
        </el-form-item>
        <el-form-item prop="role" label="角色">
          <el-select v-model="data.form.role" placeholder="请选择角色" style="width: 100%">
            <el-option label="管理员" value="ADMIN"></el-option>
            <el-option label="普通用户" value="USER"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item prop="avatar" label="头像">
          <el-upload :action="baseUrl + '/files/upload'" :on-success="handleFileUpload" list-type="picture">
            <el-button type="primary">上传头像</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item prop="name" label="姓名">
          <el-input v-model="data.form.name" placeholder="请输入姓名"></el-input>
        </el-form-item>
        <el-form-item prop="phone" label="电话">
          <el-input v-model="data.form.phone" placeholder="请输入电话"></el-input>
        </el-form-item>
        <el-form-item prop="email" label="邮箱">
          <el-input v-model="data.form.email" placeholder="请输入邮箱"></el-input>
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
  username: null,
  name: null,
  role: null,
  ids: [],
  rules: {
    username: [
      { required: true, message: '请输入账号', trigger: 'blur' }
    ],
    role: [
      { required: true, message: '请选择角色', trigger: 'change' }
    ]
  }
})

const roleName = (role) => {
  if (role === 'ADMIN') {
    return '管理员'
  }
  return '普通用户'
}

const roleTagType = (role) => {
  if (role === 'ADMIN') {
    return 'danger'
  }
  return 'success'
}

const formatMoney = (value) => {
  return Number(value || 0).toFixed(2)
}

const load = () => {
  request.get('/user/selectPage', {
    params: {
      pageNum: data.pageNum,
      pageSize: data.pageSize,
      username: data.username,
      name: data.name,
      role: data.role
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
    role: 'USER'
  }
  data.formVisible = true
}

const handleEdit = (row) => {
  data.form = JSON.parse(JSON.stringify(row))
  data.formVisible = true
}

const add = () => {
  request.post('/user/add', data.form).then(res => {
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
  request.put('/user/update', data.form).then(res => {
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
    request.delete('/user/delete/' + id).then(res => {
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
    request.delete("/user/delete/batch", {data: data.ids}).then(res => {
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

const handleFileUpload = (res) => {
  data.form.avatar = res.data
}

const reset = () => {
  data.username = null
  data.name = null
  data.role = null
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

.money-text {
  color: #0f766e;
  font-weight: 700;
}
</style>
