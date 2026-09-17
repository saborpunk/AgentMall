<template>
  <div>
    <div class="front-header">
      <div class="front-header-left">
        <img src="@/assets/imgs/logo.png" alt="">
        <div class="title">AI 智能商城导购平台</div>
      </div>
      <div class="front-header-center">
          <el-menu :default-active="router.currentRoute.value.path" router mode="horizontal">
            <el-menu-item index="/front/home">商城首页</el-menu-item>
            <el-menu-item index="/front/guide">AI 智能导购</el-menu-item>
            <el-menu-item index="/front/favorite">我的收藏</el-menu-item>
          <el-menu-item index="/front/cart">购物车</el-menu-item>
          <el-menu-item index="/front/order">我的订单</el-menu-item>
          <el-menu-item index="/front/person">个人中心</el-menu-item>
          <el-menu-item index="/front/wallet">我的钱包</el-menu-item>
        </el-menu>
      </div>
      <div class="front-header-right">
        <div v-if="!data.user.id">
          <el-button @click="router.push('/login')">登录</el-button>
          <el-button @click="router.push('/register')">注册</el-button>
        </div>
        <div v-else>
          <el-dropdown style="cursor: pointer; height: 60px">
            <div style="display: flex; align-items: center">
              <img style="width: 40px; height: 40px; border-radius: 50%;" :src="data.user.avatar" alt="">
              <span style="margin-left: 5px;">{{ data.user.name }}</span><el-icon><arrow-down /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </div>
    <div class="main-body">
      <RouterView @updateUser="updateUser" />
    </div>
  </div>
</template>

<script setup>
import router from "@/router/index.js";
import { reactive } from "vue";

const data = reactive({
  user: JSON.parse(localStorage.getItem('sys-user') || '{}'),
})

const logout = () => {
  localStorage.removeItem('sys-user')
  router.push('/login')
}

const updateUser = () => {
  data.user = JSON.parse(localStorage.getItem('sys-user') || '{}')
}
</script>

<style scoped>
@import "@/assets/css/front.css";
</style>
