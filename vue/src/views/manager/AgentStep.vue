<template>
  <div>
    <div class="page-heading">
      <div>
        <h2>Agent Step执行步骤</h2>
        <p>查看一次智能导购运行中每个步骤的输入、输出、工具编码、状态和耗时。</p>
      </div>
    </div>

    <div class="card search-card">
      <el-input v-model="data.runNo" @clear="reset" clearable @keyup.enter="load" prefix-icon="Search" class="search-item" placeholder="请输入运行编号"></el-input>
      <el-input v-model="data.taskNo" @clear="reset" clearable @keyup.enter="load" prefix-icon="Search" class="search-item" placeholder="请输入任务编号"></el-input>
      <el-select v-model="data.stepCode" clearable placeholder="步骤类型" class="search-item" @change="load" @clear="reset">
        <el-option label="工具调用" value="TOOL_CALL"></el-option>
        <el-option label="生成推荐结果" value="RECOMMENDATION_GENERATE"></el-option>
      </el-select>
      <el-select v-model="data.status" clearable placeholder="步骤状态" class="search-item" @change="load" @clear="reset">
        <el-option label="运行中" value="RUNNING"></el-option>
        <el-option label="已完成" value="DONE"></el-option>
        <el-option label="执行失败" value="FAILED"></el-option>
      </el-select>
      <el-button type="primary" plain @click="load">查询</el-button>
      <el-button plain @click="reset">重置</el-button>
      <el-button type="danger" plain @click="delBatch">批量删除</el-button>
    </div>

    <div class="card" style="margin-bottom: 5px">
      <el-table stripe :data="data.tableData" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" />
        <el-table-column prop="runNo" label="运行编号" width="190" />
        <el-table-column prop="taskNo" label="任务编号" width="190" />
        <el-table-column prop="stepOrder" label="顺序" width="70" />
        <el-table-column prop="stepName" label="步骤名称" min-width="150" />
        <el-table-column prop="stepCode" label="步骤编码" width="190" />
        <el-table-column prop="toolCode" label="工具编码" min-width="220" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="statusType(scope.row.status)">{{ statusName(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="durationMs" label="耗时(ms)" width="110" />
        <el-table-column prop="startTime" label="开始时间" width="170" />
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="scope">
            <el-button type="info" circle :icon="View" @click="viewDetail(scope.row)"></el-button>
            <el-button type="danger" circle :icon="Delete" @click="del(scope.row.id)"></el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <div class="card" v-if="data.total">
      <el-pagination @current-change="load" background layout="prev, pager, next" :page-size="data.pageSize" v-model:current-page="data.pageNum" :total="data.total" />
    </div>

    <el-dialog title="步骤详情" v-model="data.detailVisible" width="880px">
      <el-descriptions :column="2" border v-if="data.current">
        <el-descriptions-item label="运行编号">{{ data.current.runNo }}</el-descriptions-item>
        <el-descriptions-item label="任务编号">{{ data.current.taskNo }}</el-descriptions-item>
        <el-descriptions-item label="步骤顺序">{{ data.current.stepOrder }}</el-descriptions-item>
        <el-descriptions-item label="步骤名称">{{ data.current.stepName }}</el-descriptions-item>
        <el-descriptions-item label="步骤编码">{{ data.current.stepCode }}</el-descriptions-item>
        <el-descriptions-item label="工具编码">{{ data.current.toolCode }}</el-descriptions-item>
        <el-descriptions-item label="步骤状态">{{ statusName(data.current.status) }}</el-descriptions-item>
        <el-descriptions-item label="耗时">{{ data.current.durationMs }} ms</el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ data.current.startTime }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{ data.current.endTime }}</el-descriptions-item>
        <el-descriptions-item label="错误信息" :span="2">{{ data.current.errorMessage }}</el-descriptions-item>
      </el-descriptions>
      <h4>步骤输入</h4>
      <pre class="detail-text">{{ data.current?.inputContent || '暂无输入' }}</pre>
      <h4>步骤输出</h4>
      <pre class="detail-text">{{ data.current?.outputContent || '暂无输出' }}</pre>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive } from "vue";
import request from "@/utils/request.js";
import { ElMessage, ElMessageBox } from "element-plus";
import { Delete, View } from "@element-plus/icons-vue";

const data = reactive({
  detailVisible: false,
  current: null,
  tableData: [],
  pageNum: 1,
  pageSize: 10,
  total: 0,
  runNo: null,
  taskNo: null,
  stepCode: null,
  status: null,
  ids: []
})

const statusName = (status) => {
  if (status === 'DONE') {
    return '已完成'
  }
  if (status === 'FAILED') {
    return '执行失败'
  }
  return '运行中'
}

const statusType = (status) => {
  if (status === 'DONE') {
    return 'success'
  }
  if (status === 'FAILED') {
    return 'danger'
  }
  return 'warning'
}

const load = () => {
  request.get('/agentStep/selectPage', {
    params: {
      pageNum: data.pageNum,
      pageSize: data.pageSize,
      runNo: data.runNo,
      taskNo: data.taskNo,
      stepCode: data.stepCode,
      status: data.status
    }
  }).then(res => {
    if (res.code === '200') {
      data.tableData = res.data?.list || []
      data.total = res.data?.total
    }
  })
}

const reset = () => {
  data.runNo = null
  data.taskNo = null
  data.stepCode = null
  data.status = null
  data.pageNum = 1
  load()
}

const viewDetail = (row) => {
  data.current = JSON.parse(JSON.stringify(row))
  data.detailVisible = true
}

const del = (id) => {
  ElMessageBox.confirm('删除后数据无法恢复，确定删除吗？', '删除确认', { type: 'warning' }).then(() => {
    request.delete('/agentStep/delete/' + id).then(res => {
      if (res.code === '200') {
        ElMessage.success('删除成功')
        load()
      } else {
        ElMessage.error(res.msg)
      }
    })
  })
}

const delBatch = () => {
  if (!data.ids.length) {
    ElMessage.warning('请选择数据')
    return
  }
  ElMessageBox.confirm('删除后数据无法恢复，确定批量删除吗？', '删除确认', { type: 'warning' }).then(() => {
    request.delete('/agentStep/delete/batch', { data: data.ids }).then(res => {
      if (res.code === '200') {
        ElMessage.success('操作成功')
        load()
      } else {
        ElMessage.error(res.msg)
      }
    })
  })
}

const handleSelectionChange = (rows) => {
  data.ids = rows.map(v => v.id)
}

load()
</script>

<style scoped>
.page-heading {
  margin-bottom: 12px;
  padding: 20px 22px;
  border-radius: 8px;
  background: #fff;
  border: 1px solid #e5e7eb;
}

.page-heading h2 {
  margin: 0;
  font-size: 22px;
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
  padding: 16px;
}

.search-card {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 5px;
}

.search-item {
  width: 220px;
}

h4 {
  margin: 18px 0 8px;
  color: #172033;
}

.detail-text {
  margin: 0;
  padding: 14px;
  color: #334155;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
  background: #f8fafc;
  border-radius: 8px;
}
</style>
