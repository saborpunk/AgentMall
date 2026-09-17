<template>
  <div class="profile-page">
    <section class="profile-hero">
      <div class="user-card">
        <el-upload
            :action="baseUrl + '/files/upload'"
            :on-success="handleFileUpload"
            :show-file-list="false"
            class="avatar-uploader"
        >
          <img v-if="data.user.avatar" :src="data.user.avatar" class="avatar" />
          <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
        </el-upload>
        <div>
          <h2>{{ data.user.name || data.user.username }}</h2>
          <p>{{ roleName(data.user.role) }}</p>
        </div>
      </div>
      <div class="profile-stats">
        <div>
          <strong>{{ data.addressList.length }}</strong>
          <span>收货地址</span>
        </div>
        <div>
          <strong>{{ defaultAddress ? '已设置' : '未设置' }}</strong>
          <span>默认地址</span>
        </div>
      </div>
    </section>

    <section class="content-grid">
      <div class="card profile-card">
        <div class="section-title">
          <h3>个人资料</h3>
          <span>用于订单收货和售后联系</span>
        </div>
        <el-form :model="data.user" label-width="78px">
          <el-form-item prop="username" label="账号">
            <el-input disabled v-model="data.user.username" placeholder="请输入账号"></el-input>
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
          <div style="text-align: right">
            <el-button type="primary" @click="updateUser">保存资料</el-button>
          </div>
        </el-form>
      </div>

      <div class="card address-card">
        <div class="section-title address-title">
          <div>
            <h3>收货地址</h3>
            <span>后续提交订单时会优先使用默认地址</span>
          </div>
          <el-button type="primary" plain @click="handleAddAddress">新增地址</el-button>
        </div>

        <div v-if="data.addressList.length" class="address-list">
          <div class="address-item" v-for="item in data.addressList" :key="item.id">
            <div class="address-main">
              <div class="receiver-row">
                <strong>{{ item.receiverName }}</strong>
                <span>{{ item.receiverPhone }}</span>
                <el-tag v-if="item.isDefault === 1" type="success">默认地址</el-tag>
              </div>
              <p>{{ item.province }} {{ item.city }} {{ item.district }} {{ item.detailAddress }}</p>
              <span v-if="item.postalCode" class="postal-code">邮编：{{ item.postalCode }}</span>
            </div>
            <div class="address-actions">
              <el-button link type="primary" @click="handleEditAddress(item)">编辑</el-button>
              <el-button link type="success" v-if="item.isDefault !== 1" @click="setDefault(item)">设为默认</el-button>
              <el-button link type="danger" @click="deleteAddress(item.id)">删除</el-button>
            </div>
          </div>
        </div>
        <el-empty v-else description="暂无收货地址"></el-empty>
      </div>
    </section>

    <el-dialog title="收货地址" v-model="data.addressVisible" width="520px" destroy-on-close>
      <el-form ref="addressFormRef" :model="data.addressForm" :rules="data.addressRules" label-width="92px">
        <el-form-item prop="receiverName" label="收货人">
          <el-input v-model="data.addressForm.receiverName" placeholder="请输入收货人"></el-input>
        </el-form-item>
        <el-form-item prop="receiverPhone" label="收货电话">
          <el-input v-model="data.addressForm.receiverPhone" placeholder="请输入收货电话"></el-input>
        </el-form-item>
        <el-form-item prop="province" label="省份">
          <el-input v-model="data.addressForm.province" placeholder="请输入省份"></el-input>
        </el-form-item>
        <el-form-item prop="city" label="城市">
          <el-input v-model="data.addressForm.city" placeholder="请输入城市"></el-input>
        </el-form-item>
        <el-form-item prop="district" label="区县">
          <el-input v-model="data.addressForm.district" placeholder="请输入区县"></el-input>
        </el-form-item>
        <el-form-item prop="detailAddress" label="详细地址">
          <el-input v-model="data.addressForm.detailAddress" type="textarea" :rows="3" placeholder="请输入街道、门牌号等详细地址"></el-input>
        </el-form-item>
        <el-form-item prop="postalCode" label="邮政编码">
          <el-input v-model="data.addressForm.postalCode" placeholder="请输入邮政编码"></el-input>
        </el-form-item>
        <el-form-item prop="isDefault" label="默认地址">
          <el-switch v-model="data.addressForm.isDefault" :active-value="1" :inactive-value="0"></el-switch>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="data.addressVisible = false">取消</el-button>
        <el-button type="primary" @click="saveAddress">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from "vue";
import request from "@/utils/request.js";
import {ElMessage, ElMessageBox} from "element-plus";

const baseUrl = import.meta.env.VITE_BASE_URL
const addressFormRef = ref()

const data = reactive({
  user: JSON.parse(localStorage.getItem('sys-user') || '{}'),
  addressList: [],
  addressVisible: false,
  addressForm: {},
  addressRules: {
    receiverName: [
      { required: true, message: '请输入收货人', trigger: 'blur' }
    ],
    receiverPhone: [
      { required: true, message: '请输入收货电话', trigger: 'blur' }
    ],
    province: [
      { required: true, message: '请输入省份', trigger: 'blur' }
    ],
    city: [
      { required: true, message: '请输入城市', trigger: 'blur' }
    ],
    district: [
      { required: true, message: '请输入区县', trigger: 'blur' }
    ],
    detailAddress: [
      { required: true, message: '请输入详细地址', trigger: 'blur' }
    ]
  }
})

const defaultAddress = computed(() => data.addressList.find(item => item.isDefault === 1))

const roleName = (role) => {
  if (role === 'ADMIN') {
    return '管理员'
  }
  return '普通用户'
}

const handleFileUpload = (res) => {
  data.user.avatar = res.data
}

const emit = defineEmits(['updateUser'])

const updateUser = () => {
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

const loadAddress = () => {
  if (!data.user.id) {
    return
  }
  request.get('/userAddress/selectByUserId/' + data.user.id).then(res => {
    if (res.code === '200') {
      data.addressList = res.data || []
    }
  })
}

const handleAddAddress = () => {
  data.addressForm = {
    userId: data.user.id,
    isDefault: data.addressList.length ? 0 : 1
  }
  data.addressVisible = true
}

const handleEditAddress = (row) => {
  data.addressForm = JSON.parse(JSON.stringify(row))
  data.addressVisible = true
}

const saveAddress = () => {
  addressFormRef.value.validate(valid => {
    if (!valid) {
      return
    }
    const requestApi = data.addressForm.id ? request.put('/userAddress/update', data.addressForm) : request.post('/userAddress/add', data.addressForm)
    requestApi.then(res => {
      if (res.code === '200') {
        ElMessage.success('保存成功')
        data.addressVisible = false
        loadAddress()
      } else {
        ElMessage.error(res.msg)
      }
    })
  })
}

const setDefault = (row) => {
  request.put('/userAddress/setDefault/' + row.id + '/' + data.user.id).then(res => {
    if (res.code === '200') {
      ElMessage.success('设置成功')
      loadAddress()
    } else {
      ElMessage.error(res.msg)
    }
  })
}

const deleteAddress = (id) => {
  ElMessageBox.confirm('删除后无法恢复，确定删除该收货地址吗？', '删除确认', { type: 'warning' }).then(() => {
    request.delete('/userAddress/delete/' + id).then(res => {
      if (res.code === '200') {
        ElMessage.success('删除成功')
        loadAddress()
      } else {
        ElMessage.error(res.msg)
      }
    })
  })
}

loadAddress()
</script>

<style scoped>
.profile-page {
  max-width: 1180px;
  margin: 0 auto;
  padding: 22px 0 36px;
}

.profile-hero {
  min-height: 178px;
  padding: 28px 32px;
  border-radius: 8px;
  background: #edf7f7;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 18px;
}

.user-card {
  display: flex;
  align-items: center;
  gap: 18px;
}

.user-card h2 {
  margin: 0;
  color: #172033;
  font-size: 26px;
}

.user-card p {
  margin: 8px 0 0;
  color: #64748b;
}

.profile-stats {
  display: flex;
  gap: 12px;
}

.profile-stats div {
  width: 130px;
  padding: 18px;
  border-radius: 8px;
  background: #fff;
  border: 1px solid #dbe7e7;
}

.profile-stats strong {
  display: block;
  color: #0f766e;
  font-size: 20px;
}

.profile-stats span {
  display: block;
  margin-top: 8px;
  color: #64748b;
}

.content-grid {
  display: grid;
  grid-template-columns: 420px minmax(0, 1fr);
  gap: 18px;
}

.profile-card,
.address-card {
  padding: 24px;
}

.section-title {
  margin-bottom: 20px;
}

.section-title h3 {
  margin: 0;
  color: #172033;
  font-size: 20px;
}

.section-title span {
  display: block;
  margin-top: 8px;
  color: #64748b;
}

.address-title {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.address-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.address-item {
  padding: 18px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.receiver-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.receiver-row strong {
  color: #172033;
  font-size: 16px;
}

.receiver-row span {
  color: #64748b;
}

.address-main p {
  margin: 10px 0 0;
  color: #475569;
  line-height: 1.7;
}

.postal-code {
  display: block;
  margin-top: 8px;
  color: #94a3b8;
}

.address-actions {
  min-width: 150px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
}

.avatar-uploader {
  width: 116px;
  height: 116px;
}

.avatar-uploader .avatar {
  width: 116px;
  height: 116px;
  border-radius: 50%;
  object-fit: cover;
  display: block;
}

.avatar-uploader :deep(.el-upload) {
  width: 116px;
  height: 116px;
  border: 1px dashed var(--el-border-color);
  border-radius: 50%;
  cursor: pointer;
  overflow: hidden;
  background: #fff;
}

.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 116px;
  height: 116px;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
