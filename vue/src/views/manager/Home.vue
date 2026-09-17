<template>
  <div>
    <div class="dashboard">
      <div class="welcome-card">
        <div>
          <div class="welcome-title">您好，{{ data.user?.name }}</div>
          <div class="welcome-desc">欢迎进入 AI 智能商城导购与运营增长系统</div>
        </div>
      </div>
      <div class="metric-grid">
        <div class="metric-card">
          <span>导购能力</span>
          <strong>需求识别</strong>
        </div>
        <div class="metric-card">
          <span>知识能力</span>
          <strong>商品 RAG</strong>
        </div>
        <div class="metric-card">
          <span>工具能力</span>
          <strong>价格库存查询</strong>
        </div>
        <div class="metric-card">
          <span>运营能力</span>
          <strong>评价洞察</strong>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>

import {reactive} from "vue";
const data = reactive({
  user: JSON.parse(localStorage.getItem('sys-user') || '{}'),
})

</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.welcome-card {
  padding: 24px;
  border-radius: 8px;
  background: #fff;
  border: 1px solid #e5e7eb;
}

.welcome-title {
  font-size: 22px;
  font-weight: 700;
  color: #172033;
}

.welcome-desc {
  margin-top: 8px;
  color: #64748b;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.metric-card {
  padding: 20px;
  border-radius: 8px;
  background: #fff;
  border: 1px solid #e5e7eb;
}

.metric-card span {
  display: block;
  color: #64748b;
  margin-bottom: 10px;
}

.metric-card strong {
  color: #0f766e;
  font-size: 18px;
}
</style>
