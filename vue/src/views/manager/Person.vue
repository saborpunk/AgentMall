<template>
  <div class="person-page">
    <div class="page-heading">
      <h2>个人资料</h2>
      <p>维护后台账号资料，商品管理、订单处理和 AI 导购任务会使用当前账号信息。</p>
    </div>
  <div class="card person-card">
    <el-form ref="user" :model="data.user" label-width="70px" style="padding: 20px">
      <el-form-item prop="avatar" label="头像">
        <el-upload
            :action="baseUrl + '/files/upload'"
            :on-success="handleFileUpload"
            :show-file-list="false"
            class="avatar-uploader"
        >
          <img v-if="data.user.avatar" :src="data.user.avatar" class="avatar" />
          <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
        </el-upload>
      </el-form-item>
      <el-form-item prop="username" label="用户名">
        <el-input disabled v-model="data.user.username" placeholder="请输入用户名"></el-input>
      </el-form-item>
      <el-form-item prop="name" label="姓名">
        <el-input v-model="data.user.name" placeholder="请输入姓名"></el-input>
      </el-form-item>
      <el-form-item prop="phone" label="电话">
        <el-input v-model="data.user.phone" placeholder="请输入电话"></el-input>
      </el-form-item>
      <el-form-item prop="email" label="邮箱">
        <el-input v-model="data.user.email" placeholder="请输入邮箱"></el-input>
      </el-form-item>
      <div style="text-align: center">
        <el-button type="primary" @click="update">保 存</el-button>
      </div>
    </el-form>
  </div>
  </div>
</template>

<script setup>
import { reactive } from "vue";
import request from "@/utils/request.js";
import {ElMessage} from "element-plus";

const baseUrl = import.meta.env.VITE_BASE_URL

const data = reactive({
  user: JSON.parse(localStorage.getItem('sys-user') || '{}')
})

const handleFileUpload = (res) => {
  data.user.avatar = res.data
}

const emit = defineEmits(['updateUser'])
const update = () => {
  const userForm = {
    id: data.user.id,
    username: data.user.username,
    name: data.user.name,
    avatar: data.user.avatar,
    phone: data.user.phone,
    email: data.user.email
  }
  request.put('/user/update', userForm).then(res => {
    if (res.code === '200') {
      ElMessage.success('保存成功')
      data.user = { ...data.user, ...userForm }
      localStorage.setItem('sys-user', JSON.stringify(data.user))
      emit('updateUser')
    } else {
      ElMessage.error(res.msg)
    }
  })
}
</script>

<style scoped>
.person-page {
  max-width: 760px;
}

.page-heading {
  margin-bottom: 12px;
  padding: 20px 22px;
  border-radius: 8px;
  background: #fff;
  border: 1px solid #e5e7eb;
}

.page-heading h2 {
  margin: 0;
  color: #172033;
  font-size: 22px;
}

.page-heading p {
  margin: 8px 0 0;
  color: #64748b;
}

.person-card {
  width: 100%;
}

.avatar-uploader {
  height: 120px;
}
.avatar-uploader .avatar {
  width: 120px;
  height: 120px;
  display: block;
}
.avatar-uploader .el-upload {
  border: 1px dashed var(--el-border-color);
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: var(--el-transition-duration-fast);
}

.avatar-uploader .el-upload:hover {
  border-color: var(--el-color-primary);
}

.el-icon.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 120px;
  height: 120px;
  text-align: center;
}
</style>
