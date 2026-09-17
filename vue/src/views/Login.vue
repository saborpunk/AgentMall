<template>
  <div class="auth-page">
    <div class="page-glow glow-one"></div>
    <div class="page-glow glow-two"></div>

    <main class="auth-shell">
      <section class="shop-showcase">
        <div class="brand-row">
          <img class="brand-logo" src="@/assets/imgs/logo.png" alt="AI 智能导购">
          <div>
            <div class="brand-name">智能商城导购平台</div>
            <div class="brand-subtitle">AI Shopping Assistant</div>
          </div>
        </div>

        <div class="showcase-copy">
          <div class="eyebrow"><i></i> 你的专属智能导购</div>
          <h1>不只是搜索商品<br><em>更懂你的每次选择</em></h1>
          <p>说出需求，AI 将结合预算、偏好、库存和真实评价，为你筛选更合适的商品。</p>
        </div>

        <div class="assistant-demo">
          <div class="chat-row customer-chat">
            <span>预算 800 元，想买一款通勤降噪耳机</span>
          </div>
          <div class="chat-row ai-chat">
            <img src="@/assets/imgs/logo.png" alt="AI">
            <div>
              <strong>为你精选 3 款高匹配商品</strong>
              <span>已综合降噪效果、续航和 2,680 条评价</span>
            </div>
          </div>

          <div class="product-strip">
            <div class="product-item">
              <div class="product-thumb thumb-blue">降噪</div>
              <div class="product-info">
                <strong>AirSound Pro</strong>
                <span>深度降噪 · 40h 续航</span>
              </div>
              <div class="product-price"><b>¥699</b><span>96% 匹配</span></div>
            </div>
            <div class="product-item muted-product">
              <div class="product-thumb thumb-violet">轻盈</div>
              <div class="product-info">
                <strong>CloudBuds 3</strong>
                <span>舒适佩戴 · 双设备</span>
              </div>
              <div class="product-price"><b>¥529</b><span>92% 匹配</span></div>
            </div>
          </div>
        </div>

        <div class="feature-row">
          <span><i>✓</i> 个性推荐</span>
          <span><i>✓</i> 智能比价</span>
          <span><i>✓</i> 售前售后问答</span>
        </div>
      </section>

      <section class="login-card">
        <div class="mobile-brand">
          <img src="@/assets/imgs/logo.png" alt="AI 智能导购">
          <strong>智选商城</strong>
        </div>

        <div class="login-badge">WELCOME BACK</div>
        <div class="login-header">
          <h2>欢迎登录系统</h2>
          <p>开启更高效、更懂你的智能购物体验</p>
        </div>

        <el-form ref="formRef" :model="data.form" :rules="data.rules" class="login-form">
          <label class="field-label">账号</label>
          <el-form-item prop="username">
            <el-input v-model="data.form.username" placeholder="请输入商城账号" size="large" :prefix-icon="User"/>
          </el-form-item>
          <label class="field-label">密码</label>
          <el-form-item prop="password">
            <el-input v-model="data.form.password" type="password" placeholder="请输入登录密码" size="large" show-password :prefix-icon="Lock" @keyup.enter="login"/>
          </el-form-item>
          <el-form-item class="submit-item">
            <el-button type="primary" size="large" class="submit-btn" @click="login" :loading="data.loading">
              <span>进入商城</span><span class="arrow">→</span>
            </el-button>
          </el-form-item>
        </el-form>

        <div class="divider"><span>新用户</span></div>
        <div class="login-footer">
          <span>还没有商城账号？</span>
          <a href="/register">立即注册</a>
        </div>
        <p class="security-tip"><span>●</span> 账号信息已加密保护，请放心登录</p>
      </section>
    </main>
  </div>
</template>

<script setup>
import { reactive, ref } from "vue";
import { User, Lock } from "@element-plus/icons-vue";
import request from "@/utils/request.js";
import { ElMessage } from "element-plus";

const data = reactive({
  form: {},
  rules: {
    username: [
      { required: true, message: '请输入账号', trigger: 'blur' }
    ],
    password: [
      { required: true, message: '请输入密码', trigger: 'blur' }
    ]
  },
  loading: false
})

const formRef = ref()

const login = () => {
  formRef.value.validate(valid => {
    if (valid) {
      data.loading = true
      request.post('/user/login', data.form).then(res => {
        if (res.code === '200') {
          ElMessage.success('登录成功')
          localStorage.setItem('sys-user', JSON.stringify(res.data))
          setTimeout(() => {
            if (res.data.role === 'USER') {
              location.href = '/front/home'
            } else {
              location.href = '/manager/home'
            }
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
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  padding: 24px;
  color: #162033;
  background: #eef3f8;
}

.page-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(4px);
  pointer-events: none;
}

.glow-one {
  top: -260px;
  left: -160px;
  width: 560px;
  height: 560px;
  background: rgba(48, 205, 225, 0.16);
}

.glow-two {
  right: -180px;
  bottom: -280px;
  width: 600px;
  height: 600px;
  background: rgba(82, 112, 255, 0.12);
}

.auth-shell {
  width: min(1080px, 100%);
  min-height: 620px;
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.8);
  border-radius: 26px;
  background: #fff;
  box-shadow: 0 28px 80px rgba(25, 43, 72, 0.16);
}

.shop-showcase {
  position: relative;
  overflow: hidden;
  padding: 36px 42px 30px;
  color: #fff;
  background:
      radial-gradient(circle at 88% 18%, rgba(48, 205, 225, 0.22), transparent 28%),
      radial-gradient(circle at 12% 85%, rgba(82, 112, 255, 0.22), transparent 32%),
      linear-gradient(145deg, #081421 0%, #0c2232 58%, #0c2d3b 100%);
  display: flex;
  flex-direction: column;
}

.shop-showcase::after {
  content: '';
  position: absolute;
  top: -70px;
  right: -90px;
  width: 260px;
  height: 260px;
  border: 1px solid rgba(114, 225, 240, 0.16);
  border-radius: 50%;
  box-shadow: 0 0 0 42px rgba(114, 225, 240, 0.035), 0 0 0 84px rgba(114, 225, 240, 0.025);
}

.brand-row {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 11px;
}

.brand-logo {
  width: 42px;
  height: 42px;
  object-fit: cover;
  border: 2px solid rgba(255, 255, 255, 0.82);
  border-radius: 13px;
}

.brand-name {
  font-size: 17px;
  font-weight: 800;
  letter-spacing: 0.04em;
}

.brand-subtitle {
  margin-top: 2px;
  color: #8fa9b8;
  font-size: 10px;
  letter-spacing: 0.13em;
  text-transform: uppercase;
}

.showcase-copy {
  position: relative;
  z-index: 1;
  margin-top: 34px;
}

.eyebrow {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #68dbe8;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.eyebrow i {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #48d7e8;
  box-shadow: 0 0 0 5px rgba(72, 215, 232, 0.12);
}

.showcase-copy h1 {
  margin: 13px 0 0;
  font-size: clamp(30px, 3.2vw, 42px);
  line-height: 1.22;
  letter-spacing: -0.035em;
}

.showcase-copy h1 em {
  color: #72e2ed;
  font-style: normal;
}

.showcase-copy p {
  max-width: 455px;
  margin: 13px 0 0;
  color: #a9bdc8;
  font-size: 13px;
  line-height: 1.75;
}

.assistant-demo {
  position: relative;
  z-index: 1;
  margin-top: 24px;
  padding: 16px;
  border: 1px solid rgba(139, 210, 221, 0.16);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.065);
  box-shadow: 0 18px 38px rgba(0, 0, 0, 0.16);
  backdrop-filter: blur(12px);
}

.chat-row {
  display: flex;
  align-items: center;
}

.customer-chat {
  justify-content: flex-end;
}

.customer-chat span {
  padding: 9px 12px;
  border-radius: 12px 12px 3px 12px;
  color: #d9fbff;
  background: rgba(47, 177, 194, 0.22);
  font-size: 12px;
}

.ai-chat {
  gap: 9px;
  margin-top: 12px;
}

.ai-chat img {
  width: 32px;
  height: 32px;
  border-radius: 9px;
  object-fit: cover;
}

.ai-chat div {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.ai-chat strong {
  font-size: 12px;
}

.ai-chat span {
  color: #7f9aa9;
  font-size: 10px;
}

.product-strip {
  margin-top: 12px;
  overflow: hidden;
  border-radius: 12px;
  background: rgba(2, 12, 20, 0.48);
}

.product-item {
  display: grid;
  grid-template-columns: 42px 1fr auto;
  align-items: center;
  gap: 10px;
  padding: 10px 11px;
}

.product-item + .product-item {
  border-top: 1px solid rgba(255, 255, 255, 0.07);
}

.product-thumb {
  width: 42px;
  height: 42px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  font-size: 10px;
  font-weight: 800;
}

.thumb-blue {
  color: #c9f9ff;
  background: linear-gradient(145deg, #215066, #173347);
}

.thumb-violet {
  color: #e9ddff;
  background: linear-gradient(145deg, #514172, #2f2e52);
}

.product-info,
.product-price {
  display: flex;
  flex-direction: column;
}

.product-info {
  gap: 4px;
}

.product-info strong {
  font-size: 12px;
}

.product-info span {
  color: #76909f;
  font-size: 10px;
}

.product-price {
  align-items: flex-end;
  gap: 3px;
}

.product-price b {
  color: #fff;
  font-size: 13px;
}

.product-price span {
  color: #5edbe8;
  font-size: 9px;
}

.muted-product {
  opacity: 0.72;
}

.feature-row {
  position: relative;
  z-index: 1;
  display: flex;
  gap: 20px;
  margin-top: auto;
  padding-top: 24px;
  color: #91aab7;
  font-size: 11px;
}

.feature-row span {
  display: flex;
  align-items: center;
  gap: 5px;
}

.feature-row i {
  color: #54d6e4;
  font-style: normal;
}

.login-card {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 44px 48px;
  background: #fff;
}

.mobile-brand {
  display: none;
}

.login-badge {
  margin-bottom: 12px;
  color: #258b9b;
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 0.18em;
}

.login-header {
  margin-bottom: 28px;
}

.login-header h2 {
  margin: 0;
  color: #111c2e;
  font-size: 28px;
  font-weight: 800;
  letter-spacing: -0.025em;
}

.login-header p {
  margin: 8px 0 0;
  color: #7c899b;
  font-size: 13px;
}

.field-label {
  display: block;
  margin: 0 0 8px 2px;
  color: #3a4658;
  font-size: 12px;
  font-weight: 700;
}

.login-form :deep(.el-input__wrapper) {
  min-height: 48px;
  padding: 0 15px;
  border-radius: 11px;
  background: #f7f9fc;
  box-shadow: 0 0 0 1px #e1e7ef inset;
  transition: all 0.2s ease;
}

.login-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #b9cad6 inset;
}

.login-form :deep(.el-input__wrapper.is-focus) {
  background: #fff;
  box-shadow: 0 0 0 1px #25a7b8 inset, 0 0 0 4px rgba(37, 167, 184, 0.1);
}

.login-form :deep(.el-input__inner) {
  color: #1b2738;
  font-size: 13px;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 19px;
}

.login-form :deep(.submit-item) {
  margin: 7px 0 0;
}

.submit-btn {
  width: 100%;
  height: 49px;
  display: flex;
  justify-content: space-between;
  padding: 0 18px 0 22px;
  border-color: #1298aa;
  border-radius: 11px;
  background: linear-gradient(100deg, #149aad, #2479c9);
  font-weight: 700;
  box-shadow: 0 12px 24px rgba(25, 137, 164, 0.2);
}

.submit-btn:hover {
  border-color: #0d889a;
  background: linear-gradient(100deg, #108c9e, #216fb9);
  transform: translateY(-1px);
}

.submit-btn .arrow {
  font-size: 18px;
  font-weight: 400;
}

.divider {
  position: relative;
  margin: 27px 0 18px;
  border-top: 1px solid #edf0f4;
  text-align: center;
}

.divider span {
  position: relative;
  top: -9px;
  padding: 0 11px;
  color: #a0a9b6;
  background: #fff;
  font-size: 10px;
}

.login-footer {
  text-align: center;
  color: #778395;
  font-size: 13px;
}

.login-footer a {
  margin-left: 8px;
  color: #168fa0;
  font-weight: 700;
  text-decoration: none;
}

.login-footer a:hover {
  color: #156fba;
}

.security-tip {
  margin: 24px 0 0;
  color: #a1aab6;
  font-size: 10px;
  text-align: center;
}

.security-tip span {
  margin-right: 5px;
  color: #36b99d;
  font-size: 8px;
}

@media (max-width: 860px) {
  .auth-shell {
    grid-template-columns: 1fr;
    width: min(520px, 100%);
    min-height: auto;
  }

  .shop-showcase {
    padding: 25px 28px 28px;
  }

  .brand-row,
  .assistant-demo,
  .feature-row {
    display: none;
  }

  .showcase-copy {
    margin-top: 0;
  }

  .showcase-copy h1 {
    margin-top: 10px;
    font-size: 27px;
  }

  .showcase-copy h1 br {
    display: none;
  }

  .showcase-copy p {
    margin-top: 8px;
  }

  .login-card {
    padding: 32px 34px 30px;
  }

  .mobile-brand {
    display: flex;
    align-items: center;
    gap: 9px;
    margin-bottom: 27px;
    color: #172033;
  }

  .mobile-brand img {
    width: 34px;
    height: 34px;
    object-fit: cover;
    border-radius: 10px;
  }
}

@media (max-width: 520px) {
  .auth-page {
    align-items: flex-start;
    padding: 12px;
  }

  .auth-shell {
    border-radius: 20px;
  }

  .shop-showcase {
    padding: 22px;
  }

  .showcase-copy p {
    font-size: 12px;
  }

  .login-card {
    padding: 27px 22px 25px;
  }

  .login-header {
    margin-bottom: 24px;
  }

  .login-header h2 {
    font-size: 24px;
  }
}
</style>
