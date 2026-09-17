<template>
  <div>
    <div class="page-heading">
      <div>
        <h2>商品评价管理</h2>
        <p>查看用户对已完成订单商品的评价内容、评分和审核状态，为后续 AI 评价分析提供数据基础。</p>
      </div>
    </div>

    <div class="card search-card">
      <el-input v-model="data.productName" clearable @keyup.enter="load" @clear="reset" prefix-icon="Search" class="search-item" placeholder="请输入商品名称"></el-input>
      <el-input v-model="data.orderNo" clearable @keyup.enter="load" @clear="reset" prefix-icon="Search" class="search-item" placeholder="请输入订单编号"></el-input>
      <el-select v-model="data.rating" clearable placeholder="评分" class="search-item" @change="load" @clear="load">
        <el-option v-for="item in [5,4,3,2,1]" :key="item" :label="item + ' 星'" :value="item"></el-option>
      </el-select>
      <el-select v-model="data.auditStatus" clearable placeholder="审核状态" class="search-item" @change="load" @clear="load">
        <el-option label="已通过" value="APPROVED"></el-option>
        <el-option label="已隐藏" value="HIDDEN"></el-option>
      </el-select>
      <el-button type="primary" plain @click="load">查询</el-button>
      <el-button plain @click="reset">重置</el-button>
    </div>

    <div class="card" style="margin-bottom: 12px">
      <el-table stripe :data="data.tableData">
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
        <el-table-column prop="orderNo" label="订单编号" min-width="180" />
        <el-table-column prop="userName" label="用户" width="110" />
        <el-table-column prop="rating" label="评分" width="150">
          <template v-slot="scope">
            <el-rate v-model="scope.row.rating" disabled size="small"></el-rate>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="评价内容" min-width="260" show-overflow-tooltip />
        <el-table-column prop="auditStatus" label="状态" width="100">
          <template v-slot="scope">
            <el-tag :type="scope.row.auditStatus === 'APPROVED' ? 'success' : 'info'">{{ auditStatusName(scope.row.auditStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="评价时间" width="170" />
        <el-table-column label="操作" width="170" fixed="right">
          <template v-slot="scope">
            <el-button type="primary" link @click="showDetail(scope.row)">详情</el-button>
            <el-button v-if="scope.row.auditStatus === 'APPROVED'" type="warning" link @click="audit(scope.row.id, 'HIDDEN')">隐藏</el-button>
            <el-button v-else type="success" link @click="audit(scope.row.id, 'APPROVED')">通过</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <div class="card" v-if="data.total">
      <el-pagination @current-change="load" background layout="prev, pager, next" :page-size="data.pageSize" v-model:current-page="data.pageNum" :total="data.total" />
    </div>

    <el-dialog title="评价详情" v-model="data.detailVisible" width="620px">
      <div class="detail" v-if="data.current">
        <h3>{{ data.current.productName }}</h3>
        <p>订单编号：{{ data.current.orderNo }}</p>
        <p>评价用户：{{ data.current.userName }}</p>
        <el-rate v-model="data.current.rating" disabled></el-rate>
        <div class="content">{{ data.current.content }}</div>
        <p v-if="data.current.images">评价图片：{{ data.current.images }}</p>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive } from "vue";
import request from "@/utils/request.js";
import { ElMessage } from "element-plus";

const data = reactive({
  productName: null,
  orderNo: null,
  rating: null,
  auditStatus: null,
  tableData: [],
  pageNum: 1,
  pageSize: 10,
  total: 0,
  detailVisible: false,
  current: null
})

const auditStatusName = (status) => {
  return status === 'APPROVED' ? '已通过' : '已隐藏'
}

const load = () => {
  request.get('/productReview/selectPage', {
    params: {
      pageNum: data.pageNum,
      pageSize: data.pageSize,
      productName: data.productName,
      orderNo: data.orderNo,
      rating: data.rating,
      auditStatus: data.auditStatus
    }
  }).then(res => {
    if (res.code === '200') {
      data.tableData = res.data?.list || []
      data.total = res.data?.total || 0
    }
  })
}

const reset = () => {
  data.productName = null
  data.orderNo = null
  data.rating = null
  data.auditStatus = null
  data.pageNum = 1
  load()
}

const showDetail = (row) => {
  data.current = row
  data.detailVisible = true
}

const audit = (id, auditStatus) => {
  request.put('/productReview/audit', { id, auditStatus }).then(res => {
    if (res.code === '200') {
      ElMessage.success('操作成功')
      load()
    } else {
      ElMessage.error(res.msg)
    }
  })
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
  width: 200px;
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

.product-cell p,
.detail p {
  margin: 6px 0 0;
  color: #94a3b8;
  font-size: 12px;
}

.content {
  margin-top: 14px;
  padding: 14px;
  line-height: 1.8;
  color: #334155;
  background: #f8fafc;
  border-radius: 8px;
}
</style>
