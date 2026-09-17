<template>
  <div class="cart-page">
    <section class="page-title">
      <h1>购物车</h1>
      <div class="summary">
        <span>已选 {{ selectedCount }} 件</span>
        <strong>¥{{ formatMoney(selectedAmount) }}</strong>
      </div>
    </section>

    <section class="cart-table">
      <el-table :data="data.list" stripe>
        <el-table-column width="54">
          <template v-slot="scope">
            <el-checkbox :model-value="scope.row.selected === 1" @change="checked => changeSelected(scope.row, checked)" />
          </template>
        </el-table-column>
        <el-table-column label="商品" min-width="320">
          <template v-slot="scope">
            <div class="product-cell">
              <el-image class="cover" :src="scope.row.coverImage" fit="cover"></el-image>
              <div>
                <h3>{{ scope.row.productName }}</h3>
                <p>{{ scope.row.productNo }}</p>
                <el-tag size="small" :type="scope.row.status === 'ON_SALE' ? 'success' : 'info'">{{ statusName(scope.row.status) }}</el-tag>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="单价" width="130">
          <template v-slot="scope">¥{{ formatMoney(scope.row.price) }}</template>
        </el-table-column>
        <el-table-column label="数量" width="180">
          <template v-slot="scope">
            <el-input-number v-model="scope.row.quantity" :min="1" :max="availableStock(scope.row)" @change="changeQuantity(scope.row)" />
          </template>
        </el-table-column>
        <el-table-column label="可售库存" width="110">
          <template v-slot="scope">{{ availableStock(scope.row) }}</template>
        </el-table-column>
        <el-table-column label="小计" width="140">
          <template v-slot="scope">
            <strong class="subtotal">¥{{ formatMoney(scope.row.price * scope.row.quantity) }}</strong>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template v-slot="scope">
            <el-button type="danger" link @click="remove(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <section class="cart-footer">
      <el-button @click="router.push('/front/home')">继续购物</el-button>
      <el-button type="danger" plain :disabled="data.ids.length === 0" @click="deleteBatch">删除选中</el-button>
      <div class="total-box">
        <span>合计</span>
        <strong>¥{{ formatMoney(selectedAmount) }}</strong>
      </div>
      <el-button type="primary" :disabled="selectedCount === 0" @click="openOrder">提交订单</el-button>
    </section>

    <el-dialog title="确认订单" v-model="data.orderVisible" width="560px" destroy-on-close>
      <el-form ref="orderFormRef" :model="data.orderForm" :rules="data.orderRules" label-width="92px" style="padding: 12px 10px">
        <el-form-item label="收货人" prop="receiverName">
          <el-input v-model="data.orderForm.receiverName" placeholder="请输入收货人姓名"></el-input>
        </el-form-item>
        <el-form-item label="联系电话" prop="receiverPhone">
          <el-input v-model="data.orderForm.receiverPhone" placeholder="请输入联系电话"></el-input>
        </el-form-item>
        <el-form-item label="收货地址" prop="receiverAddress">
          <el-input v-model="data.orderForm.receiverAddress" type="textarea" :rows="3" placeholder="请输入收货地址"></el-input>
        </el-form-item>
        <div class="pay-info">
          <span>钱包余额：¥{{ formatMoney(data.user.balance) }}</span>
          <strong>应付：¥{{ formatMoney(selectedAmount) }}</strong>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="data.orderVisible = false">取消</el-button>
        <el-button type="primary" @click="createOrder">余额支付</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from "vue";
import request from "@/utils/request.js";
import { ElMessage, ElMessageBox } from "element-plus";
import router from "@/router/index.js";

const orderFormRef = ref()
const data = reactive({
  user: JSON.parse(localStorage.getItem('sys-user') || '{}'),
  list: [],
  ids: [],
  orderVisible: false,
  orderForm: {},
  orderRules: {
    receiverName: [{ required: true, message: '请输入收货人姓名', trigger: 'blur' }],
    receiverPhone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }],
    receiverAddress: [{ required: true, message: '请输入收货地址', trigger: 'blur' }]
  }
})

const formatMoney = (value) => {
  return Number(value || 0).toFixed(2)
}

const availableStock = (row) => {
  return Math.max(Number(row.stockQuantity || 0), 0)
}

const statusName = (status) => {
  return status === 'ON_SALE' ? '销售中' : '已下架'
}

const selectedRows = computed(() => data.list.filter(item => item.selected === 1))
const selectedCount = computed(() => selectedRows.value.reduce((sum, item) => sum + Number(item.quantity || 0), 0))
const selectedAmount = computed(() => selectedRows.value.reduce((sum, item) => sum + Number(item.price || 0) * Number(item.quantity || 0), 0))

const checkLogin = () => {
  if (!data.user.id) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return false
  }
  return true
}

const loadUser = () => {
  request.get('/user/selectById/' + data.user.id).then(res => {
    if (res.code === '200') {
      const token = data.user.token
      Object.assign(data.user, res.data, { token })
      localStorage.setItem('sys-user', JSON.stringify(data.user))
    }
  })
}

const load = () => {
  if (!checkLogin()) {
    return
  }
  request.get('/shoppingCart/selectAll', {
    params: { userId: data.user.id }
  }).then(res => {
    if (res.code === '200') {
      data.list = res.data || []
      data.ids = data.list.filter(item => item.selected === 1).map(item => item.id)
    }
  })
}

const updateCart = (row) => {
  request.put('/shoppingCart/update', {
    id: row.id,
    quantity: row.quantity,
    selected: row.selected
  }).then(res => {
    if (res.code !== '200') {
      ElMessage.error(res.msg)
    }
  })
}

const changeQuantity = (row) => {
  if (availableStock(row) <= 0) {
    row.quantity = 1
    ElMessage.warning('该商品暂无可售库存')
    return
  }
  if (row.quantity > availableStock(row)) {
    row.quantity = availableStock(row)
  }
  updateCart(row)
}

const changeSelected = (row, checked) => {
  row.selected = checked ? 1 : 0
  data.ids = data.list.filter(item => item.selected === 1).map(item => item.id)
  updateCart(row)
}

const remove = (id) => {
  ElMessageBox.confirm('确认从购物车删除该商品吗？', '删除商品', { type: 'warning' }).then(() => {
    request.delete('/shoppingCart/delete/' + id).then(res => {
      if (res.code === '200') {
        ElMessage.success('删除成功')
        load()
      } else {
        ElMessage.error(res.msg)
      }
    })
  }).catch(() => {})
}

const deleteBatch = () => {
  ElMessageBox.confirm('确认删除选中的购物车商品吗？', '批量删除', { type: 'warning' }).then(() => {
    request.delete('/shoppingCart/delete/batch', { data: data.ids }).then(res => {
      if (res.code === '200') {
        ElMessage.success('删除成功')
        load()
      } else {
        ElMessage.error(res.msg)
      }
    })
  }).catch(() => {})
}

const openOrder = () => {
  if (selectedAmount.value > Number(data.user.balance || 0)) {
    ElMessage.warning('钱包余额不足，请先充值')
    router.push('/front/wallet')
    return
  }
  data.orderForm = {
    receiverName: data.user.name,
    receiverPhone: data.user.phone,
    receiverAddress: ''
  }
  data.orderVisible = true
}

const createOrder = () => {
  orderFormRef.value.validate(valid => {
    if (!valid) {
      return
    }
    request.post('/shopOrder/create', {
      userId: data.user.id,
      receiverName: data.orderForm.receiverName,
      receiverPhone: data.orderForm.receiverPhone,
      receiverAddress: data.orderForm.receiverAddress
    }).then(res => {
      if (res.code === '200') {
        ElMessage.success('订单提交成功')
        data.orderVisible = false
        load()
        loadUser()
        router.push('/front/order')
      } else {
        ElMessage.error(res.msg)
      }
    })
  })
}

loadUser()
load()
</script>

<style scoped>
.cart-page {
  max-width: 1180px;
  margin: 0 auto;
  padding: 24px 0;
}

/* 页面标题栏：一行标题加右侧操作，不再占用整屏高度 */
.page-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 16px 20px;
  border-radius: 8px;
  background: #fff;
  border: 1px solid #e5e7eb;
}

.page-title h1 {
  margin: 0;
  font-size: 20px;
  color: #172033;
}

/* 合计信息跟标题排在同一行 */
.summary {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.summary span {
  color: #64748b;
}

.summary strong {
  color: #f5533d;
  font-size: 22px;
}

.cart-table {
  margin-top: 18px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow: hidden;
}

.product-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.cover {
  width: 72px;
  height: 72px;
  border-radius: 6px;
}

.product-cell h3 {
  margin: 0;
  font-size: 15px;
  color: #172033;
}

.product-cell p {
  margin: 6px 0;
  color: #94a3b8;
  font-size: 12px;
}

.subtotal {
  color: #dc2626;
}

.cart-footer {
  margin-top: 16px;
  padding: 16px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.total-box {
  min-width: 180px;
  text-align: right;
}

.total-box span {
  color: #64748b;
  margin-right: 10px;
}

.total-box strong {
  color: #dc2626;
  font-size: 22px;
}

.pay-info {
  padding: 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #f8fafc;
  border-radius: 8px;
}

.pay-info span {
  color: #64748b;
}

.pay-info strong {
  color: #dc2626;
  font-size: 18px;
}
</style>
