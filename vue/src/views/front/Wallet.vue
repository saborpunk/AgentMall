<template>
  <div class="wallet-page">
    <section class="wallet-hero">
      <div>
        <span>账户余额</span>
        <h1>￥{{ formatMoney(data.user.balance) }}</h1>
      </div>
      <el-button type="primary" size="large" @click="openRecharge">充值</el-button>
    </section>

    <section class="wallet-grid">
      <div class="card record-card">
        <div class="section-title">
          <h3>钱包流水</h3>
          <span>充值、支付和退款的资金记录</span>
        </div>
        <el-table :data="data.records" stripe>
          <el-table-column prop="businessNo" label="业务编号" min-width="170" />
          <el-table-column prop="type" label="类型" width="100">
            <template v-slot="scope">
              <el-tag :type="recordTagType(scope.row.type)">{{ recordTypeName(scope.row.type) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="amount" label="金额" width="120">
            <template v-slot="scope">
              <span :class="scope.row.amount >= 0 ? 'income' : 'expense'">￥{{ formatMoney(scope.row.amount) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="balanceAfter" label="变动后余额" width="130">
            <template v-slot="scope">￥{{ formatMoney(scope.row.balanceAfter) }}</template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" min-width="180" />
          <el-table-column prop="createTime" label="时间" width="170" />
        </el-table>
      </div>
    </section>

    <el-dialog title="钱包充值" v-model="data.rechargeVisible" width="460px" destroy-on-close>
      <el-form ref="formRef" :model="data.form" :rules="data.rules" label-width="96px">
        <el-form-item prop="amount" label="充值金额">
          <el-input-number v-model="data.form.amount" :min="1" :precision="2" :step="50" style="width: 100%"></el-input-number>
          <!-- 常用金额：点一下只把数字填进上面的输入框，仍需点确认充值才提交 -->
          <div class="amount-grid">
            <button
                v-for="item in data.quickAmounts"
                :key="item"
                :class="data.form.amount === item ? 'active' : ''"
                @click.prevent="data.form.amount = item"
            >￥{{ item }}</button>
          </div>
        </el-form-item>
        <el-form-item prop="remark" label="备注">
          <el-input v-model="data.form.remark" placeholder="请输入备注"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="data.rechargeVisible = false">取消</el-button>
        <el-button type="primary" @click="recharge">确认充值</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref } from "vue";
import request from "@/utils/request.js";
import {ElMessage} from "element-plus";

const formRef = ref()

const data = reactive({
  user: JSON.parse(localStorage.getItem('sys-user') || '{}'),
  records: [],
  quickAmounts: [100, 300, 500, 1000, 2000, 5000],
  rechargeVisible: false,
  form: {},
  rules: {
    amount: [
      { required: true, message: '请输入充值金额', trigger: 'blur' }
    ]
  }
})

const formatMoney = (value) => {
  return Number(value || 0).toFixed(2)
}

const recordTypeName = (type) => {
  const map = {
    RECHARGE: '充值',
    PAY: '支付',
    REFUND: '退款',
    INCOME: '收入'
  }
  return map[type] || type
}

const recordTagType = (type) => {
  if (type === 'PAY') {
    return 'warning'
  }
  if (type === 'INCOME') {
    return 'success'
  }
  if (type === 'REFUND') {
    return 'info'
  }
  return 'primary'
}

const loadRecords = () => {
  if (!data.user.id) {
    return
  }
  request.get('/wallet/records/' + data.user.id).then(res => {
    if (res.code === '200') {
      data.records = res.data || []
    }
  })
}

const openRecharge = () => {
  data.form = {
    userId: data.user.id,
    amount: 100,
    remark: '用户钱包充值'
  }
  data.rechargeVisible = true
}

const recharge = () => {
  if (!data.form.amount || data.form.amount <= 0) {
    ElMessage.warning('请输入正确的充值金额')
    return
  }
  request.post('/wallet/recharge', data.form).then(res => {
    if (res.code === '200') {
      data.user = { ...data.user, ...res.data, token: data.user.token }
      localStorage.setItem('sys-user', JSON.stringify(data.user))
      ElMessage.success('充值成功')
      data.rechargeVisible = false
      loadRecords()
    } else {
      ElMessage.error(res.msg)
    }
  })
}

loadRecords()
</script>

<style scoped>
.wallet-page {
  max-width: 1180px;
  margin: 0 auto;
  padding: 22px 0 36px;
}

/* 钱包页保留余额大字展示，但压缩成一条卡片而不是整屏方块 */
.wallet-hero {
  padding: 22px 24px;
  border-radius: 8px;
  background: #fff;
  border: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 14px;
}

.wallet-hero span {
  color: #64748b;
  font-size: 14px;
}

.wallet-hero h1 {
  margin: 6px 0 0;
  color: #f5533d;
  font-size: 32px;
}

.wallet-grid {
  display: grid;
  gap: 18px;
}

.record-card {
  padding: 24px;
}

.section-title {
  margin-bottom: 18px;
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
  line-height: 1.6;
}

.amount-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin-top: 10px;
}

.amount-grid button {
  height: 34px;
  border: 1px solid #dbe7e7;
  border-radius: 8px;
  background: #fff;
  color: #0f766e;
  font-weight: 700;
  cursor: pointer;
}

.amount-grid button:hover,
.amount-grid button.active {
  border-color: #0f766e;
  background: #f0fdfa;
}

.income {
  color: #0f766e;
  font-weight: 700;
}

.expense {
  color: #dc2626;
  font-weight: 700;
}
</style>
