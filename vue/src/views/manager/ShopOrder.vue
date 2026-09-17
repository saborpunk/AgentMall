<template>
  <div>
    <div class="page-heading">
      <div>
        <h2>订单管理</h2>
        <p>查看用户订单、收货信息、商品明细和支付状态，并处理发货与取消订单。</p>
      </div>
    </div>

    <div class="card search-card">
      <el-input v-model="data.orderNo" clearable @keyup.enter="load" @clear="reset" prefix-icon="Search" class="search-item" placeholder="请输入订单编号"></el-input>
      <el-input v-model="data.receiverPhone" clearable @keyup.enter="load" @clear="reset" prefix-icon="Search" class="search-item" placeholder="请输入收货手机号"></el-input>
      <el-select v-model="data.status" clearable placeholder="订单状态" class="search-item" @change="load" @clear="load">
        <el-option label="待发货" value="PAID"></el-option>
        <el-option label="待收货" value="SHIPPED"></el-option>
        <el-option label="已完成" value="COMPLETED"></el-option>
        <el-option label="已取消" value="CANCELLED"></el-option>
      </el-select>
      <el-button type="primary" plain @click="load">查询</el-button>
      <el-button plain @click="reset">重置</el-button>
    </div>

    <div class="card" style="margin-bottom: 12px">
      <el-table stripe :data="data.tableData">
        <el-table-column prop="orderNo" label="订单编号" min-width="190" />
        <el-table-column prop="userName" label="下单用户" width="120" />
        <el-table-column prop="totalAmount" label="订单金额" width="120">
          <template v-slot="scope">¥{{ formatMoney(scope.row.totalAmount) }}</template>
        </el-table-column>
        <el-table-column prop="totalQuantity" label="件数" width="80" />
        <el-table-column prop="status" label="状态" width="100">
          <template v-slot="scope">
            <el-tag :type="statusTag(scope.row.status)">{{ statusName(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="receiverName" label="收货人" width="120" />
        <el-table-column prop="receiverPhone" label="联系电话" width="140" />
        <el-table-column prop="receiverAddress" label="收货地址" min-width="220" show-overflow-tooltip />
        <el-table-column prop="createTime" label="下单时间" width="170" />
        <el-table-column label="操作" width="210" fixed="right">
          <template v-slot="scope">
            <el-button type="primary" plain @click="showDetail(scope.row)">明细</el-button>
            <el-button v-if="scope.row.status === 'PAID'" type="success" plain @click="ship(scope.row.id)">发货</el-button>
            <el-button v-if="scope.row.status === 'PAID'" type="danger" plain @click="cancel(scope.row.id)">取消</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <div class="card" v-if="data.total">
      <el-pagination @current-change="load" background layout="prev, pager, next" :page-size="data.pageSize" v-model:current-page="data.pageNum" :total="data.total" />
    </div>

    <el-dialog title="订单明细" v-model="data.detailVisible" width="760px">
      <div class="detail-summary" v-if="data.current">
        <span>订单编号：{{ data.current.orderNo }}</span>
        <span>实付金额：¥{{ formatMoney(data.current.totalAmount) }}</span>
        <span>订单状态：{{ statusName(data.current.status) }}</span>
      </div>
      <el-table :data="data.current.items || []" stripe>
        <el-table-column label="商品" min-width="280">
          <template v-slot="scope">
            <div class="product-cell">
              <el-image class="cover" :src="scope.row.coverImage" fit="cover"></el-image>
              <div>
                <h3>{{ scope.row.productName }}</h3>
                <p>{{ scope.row.productNo }}</p>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="单价" width="120">
          <template v-slot="scope">¥{{ formatMoney(scope.row.price) }}</template>
        </el-table-column>
        <el-table-column prop="quantity" label="数量" width="90" />
        <el-table-column label="小计" width="130">
          <template v-slot="scope">¥{{ formatMoney(scope.row.subtotalAmount) }}</template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive } from "vue";
import request from "@/utils/request.js";
import { ElMessage, ElMessageBox } from "element-plus";

const data = reactive({
  orderNo: null,
  receiverPhone: null,
  status: null,
  tableData: [],
  pageNum: 1,
  pageSize: 10,
  total: 0,
  detailVisible: false,
  current: {}
})

const formatMoney = (value) => {
  return Number(value || 0).toFixed(2)
}

const statusName = (status) => {
  const map = { PAID: '待发货', SHIPPED: '待收货', COMPLETED: '已完成', CANCELLED: '已取消' }
  return map[status] || status
}

const statusTag = (status) => {
  const map = { PAID: 'warning', SHIPPED: 'primary', COMPLETED: 'success', CANCELLED: 'info' }
  return map[status] || 'info'
}

const load = () => {
  request.get('/shopOrder/selectPage', {
    params: {
      pageNum: data.pageNum,
      pageSize: data.pageSize,
      orderNo: data.orderNo,
      receiverPhone: data.receiverPhone,
      status: data.status
    }
  }).then(res => {
    if (res.code === '200') {
      data.tableData = res.data?.list || []
      data.total = res.data?.total || 0
    }
  })
}

const reset = () => {
  data.orderNo = null
  data.receiverPhone = null
  data.status = null
  data.pageNum = 1
  load()
}

const showDetail = (row) => {
  data.current = row
  data.detailVisible = true
}

const ship = (id) => {
  ElMessageBox.confirm('确认将该订单标记为已发货吗？', '订单发货', { type: 'warning' }).then(() => {
    request.put('/shopOrder/ship/' + id).then(res => {
      if (res.code === '200') {
        ElMessage.success('发货成功')
        load()
      } else {
        ElMessage.error(res.msg)
      }
    })
  }).catch(() => {})
}

const cancel = (id) => {
  ElMessageBox.confirm('确认取消该订单并退回用户余额吗？', '取消订单', { type: 'warning' }).then(() => {
    request.put('/shopOrder/cancel/' + id).then(res => {
      if (res.code === '200') {
        ElMessage.success('订单已取消')
        load()
      } else {
        ElMessage.error(res.msg)
      }
    })
  }).catch(() => {})
}

load()
</script>

<style scoped>
.page-heading {
  margin-bottom: 12px;
  padding: 20px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.page-heading h2 {
  margin: 0;
  color: #172033;
}

.page-heading p {
  margin: 8px 0 0;
  color: #64748b;
}

.card {
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  padding: 14px;
}

.search-card {
  margin-bottom: 12px;
  display: flex;
  gap: 10px;
  align-items: center;
}

.search-item {
  width: 210px;
}

.detail-summary {
  margin-bottom: 12px;
  padding: 12px;
  display: flex;
  gap: 18px;
  background: #f8fafc;
  border-radius: 8px;
}

.product-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.cover {
  width: 58px;
  height: 58px;
  border-radius: 6px;
}

.product-cell h3 {
  margin: 0;
  font-size: 15px;
  color: #172033;
}

.product-cell p {
  margin: 6px 0 0;
  color: #94a3b8;
  font-size: 12px;
}
</style>
