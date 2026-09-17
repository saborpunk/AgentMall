<template>
  <div class="auth-page">
    <section class="register-card">
      <div class="login-header">
        <h2>创建商城账号</h2>
        <p>注册后可使用智能导购、商品收藏、购物车和订单服务</p>
      </div>

      <el-form ref="formRef" :model="data.form" :rules="data.rules" class="login-form">
        <el-form-item prop="username">
          <el-input v-model="data.form.username" placeholder="请输入账号" size="large" :prefix-icon="User"/>
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="data.form.password" type="password" placeholder="请输入密码" size="large" show-password :prefix-icon="Lock"/>
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input v-model="data.form.confirmPassword" type="password" placeholder="请确认密码" size="large" show-password :prefix-icon="Lock" @keyup.enter="register"/>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" class="submit-btn" @click="register" :loading="data.loading">注册账号</el-button>
        </el-form-item>
      </el-form>

      <div class="login-footer">
        <span>已有账号？</span>
        <a href="/login">返回登录</a>
      </div>
    </section>

    <section class="benefit-panel">
      <div class="panel-title">AI 商城能力</div>
      <div class="benefit-item">
        <strong>智能导购</strong>
        <span>根据预算、用途和偏好推荐商品</span>
      </div>
      <div class="benefit-item">
        <strong>商品问答</strong>
        <span>围绕参数、评价和售后规则进行咨询</span>
      </div>
      <div class="benefit-item">
        <strong>推荐解释</strong>
        <span>清晰说明每个推荐商品适合的原因</span>
      </div>
    </section>
  </div>
</template>

<script setup>
import { reactive, ref } from "vue";
import { User, Lock } from "@element-plus/icons-vue";
import request from "@/utils/request.js";
import { ElMessage } from "element-plus";

const validatePass = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请确认密码'))
  } else {
    if (value !== data.form.password) {
      callback(new Error("两次输入的密码不一致"))
    }
    callback()
  }
}

const data = reactive({
  form: {},
  rules: {
    username: [
      { required: true, message: '请输入账号', trigger: 'blur' }
    ],
    password: [
      { required: true, message: '请输入密码', trigger: 'blur' }
    ],
    confirmPassword: [
      { validator: validatePass, trigger: 'blur' }
    ]
  },
  loading: false
})

const formRef = ref()

const register = () => {
  formRef.value.validate(valid => {
    if (valid) {
      data.loading = true
      request.post('/user/register', data.form).then(res => {
        if (res.code === '200') {
          ElMessage.success('注册成功')
          setTimeout(() => {
            location.href = '/login'
          }, 500)
        } else {
          ElMessage.error(res.msg)
        }
      }).finally(() => {
        data.loading = false
      })
    }
  })
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 480px minmax(420px, 1fr);
  background: #f6f8fb;
}

.register-card {
  background: #fff;
  padding: 0 54px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  box-shadow: 18px 0 42px rgba(15, 23, 42, 0.06);
  z-index: 1;
}

.login-header {
  margin-bottom: 34px;
}

.login-header h2 {
  margin: 0;
  font-size: 30px;
  font-weight: 800;
  color: #172033;
}

.login-header p {
  margin: 10px 0 0;
  color: #64748b;
  line-height: 1.7;
}

.login-form :deep(.el-input__wrapper) {
  min-height: 46px;
  border-radius: 8px;
  box-shadow: 0 0 0 1px #dbe3ee inset;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 22px;
}

.submit-btn {
  width: 100%;
  height: 46px;
  border-radius: 8px;
  font-weight: 700;
  background: #2563eb;
  border-color: #2563eb;
}

.submit-btn:hover {
  background: #1d4ed8;
  border-color: #1d4ed8;
}

.login-footer {
  text-align: center;
  color: #64748b;
  font-size: 14px;
}

.login-footer a {
  color: #2563eb;
  text-decoration: none;
  margin-left: 8px;
  font-weight: 700;
}

.benefit-panel {
  padding: 72px;
  background:
      linear-gradient(180deg, rgba(37, 99, 235, 0.1), rgba(255, 255, 255, 0)),
      #edf4ff;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.panel-title {
  font-size: 38px;
  font-weight: 800;
  margin-bottom: 30px;
  color: #172033;
}

.benefit-item {
  max-width: 560px;
  padding: 22px 24px;
  margin-bottom: 16px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid #dbe7ff;
}

.benefit-item strong {
  display: block;
  color: #1d4ed8;
  font-size: 18px;
  margin-bottom: 8px;
}

.benefit-item span {
  color: #475569;
}

@media (max-width: 900px) {
  .auth-page {
    grid-template-columns: 1fr;
  }

  .benefit-panel {
    display: none;
  }

  .register-card {
    min-height: 100vh;
    padding: 0 28px;
  }
}
</style>
