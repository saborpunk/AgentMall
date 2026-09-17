<template>
  <div>
    <div class="page-heading">
      <div>
        <h2>商品管理</h2>
        <p>维护商城商品基础信息、封面图片、相册图片、价格、分类和品牌。</p>
      </div>
      <el-button type="primary" @click="handleAdd">新增商品</el-button>
    </div>

    <div class="card search-card">
      <el-input v-model="data.name" @clear="reset" clearable @keyup.enter="load" prefix-icon="Search" class="search-item" placeholder="请输入商品名称"></el-input>
      <el-input v-model="data.productNo" @clear="reset" clearable @keyup.enter="load" prefix-icon="Search" class="search-item" placeholder="请输入商品编号"></el-input>
      <el-select v-model="data.categoryId" clearable filterable placeholder="商品分类" class="search-item" @change="load" @clear="reset">
        <el-option v-for="item in data.categoryOptions" :key="item.id" :label="item.name" :value="item.id"></el-option>
      </el-select>
      <el-select v-model="data.brandId" clearable filterable placeholder="商品品牌" class="search-item" @change="load" @clear="reset">
        <el-option v-for="item in data.brandOptions" :key="item.id" :label="item.name" :value="item.id"></el-option>
      </el-select>
      <el-select v-model="data.status" clearable placeholder="商品状态" class="search-item" @change="load" @clear="reset">
        <el-option label="上架" value="ON_SALE"></el-option>
        <el-option label="下架" value="OFF_SALE"></el-option>
      </el-select>
      <el-button type="primary" plain @click="load">查询</el-button>
      <el-button plain @click="reset">重置</el-button>
      <el-button type="danger" plain @click="delBatch">批量删除</el-button>
    </div>

    <div class="card" style="margin-bottom: 5px">
      <el-table stripe :data="data.tableData" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" />
        <el-table-column prop="coverImage" label="封面" width="92">
          <template v-slot="scope">
            <el-image v-if="scope.row.coverImage" class="product-cover" :src="scope.row.coverImage" :preview-src-list="[scope.row.coverImage]" preview-teleported></el-image>
          </template>
        </el-table-column>
        <el-table-column prop="productNo" label="商品编号" min-width="150" />
        <el-table-column prop="name" label="商品名称" min-width="220" show-overflow-tooltip />
        <el-table-column prop="categoryName" label="分类" min-width="120" />
        <el-table-column prop="brandName" label="品牌" min-width="120" />
        <el-table-column prop="price" label="售价" width="110">
          <template v-slot="scope">
            <span class="money-text">￥{{ formatMoney(scope.row.price) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="originalPrice" label="原价" width="110">
          <template v-slot="scope">￥{{ formatMoney(scope.row.originalPrice) }}</template>
        </el-table-column>
        <el-table-column prop="stockQuantity" label="库存" width="100">
          <template v-slot="scope">
            <!-- 库存为 0 时标红，提醒管理员及时补货；下单和 AI 导购都看这个值 -->
            <el-tag :type="scope.row.stockQuantity > 0 ? 'success' : 'danger'">{{ scope.row.stockQuantity }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="isRecommend" label="推荐" width="90">
          <template v-slot="scope">
            <el-tag :type="scope.row.isRecommend === 1 ? 'warning' : 'info'">{{ scope.row.isRecommend === 1 ? '推荐' : '普通' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template v-slot="scope">
            <el-tag :type="scope.row.status === 'ON_SALE' ? 'success' : 'info'">{{ statusName(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column label="操作" width="120" fixed="right">
          <template v-slot="scope">
            <el-button type="primary" circle :icon="Edit" @click="handleEdit(scope.row)"></el-button>
            <el-button type="danger" circle :icon="Delete" @click="del(scope.row.id)"></el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <div class="card" v-if="data.total">
      <el-pagination @current-change="load" background layout="prev, pager, next" :page-size="data.pageSize" v-model:current-page="data.pageNum" :total="data.total" />
    </div>

    <el-dialog title="商品信息" v-model="data.formVisible" width="860px" destroy-on-close>
      <el-form ref="formRef" :model="data.form" :rules="data.rules" label-width="92px" style="padding: 20px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item prop="name" label="商品名称">
              <el-input v-model="data.form.name" placeholder="请输入商品名称"></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item prop="productNo" label="商品编号">
              <el-input v-model="data.form.productNo" placeholder="不填写则自动生成"></el-input>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item prop="categoryId" label="商品分类">
              <el-select v-model="data.form.categoryId" filterable placeholder="请选择商品分类" style="width: 100%">
                <el-option v-for="item in data.categoryOptions" :key="item.id" :label="item.name" :value="item.id"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item prop="brandId" label="商品品牌">
              <el-select v-model="data.form.brandId" filterable placeholder="请选择商品品牌" style="width: 100%">
                <el-option v-for="item in data.brandOptions" :key="item.id" :label="item.name" :value="item.id"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item prop="price" label="售价">
              <el-input-number v-model="data.form.price" :min="0.01" :precision="2" :step="10" style="width: 100%"></el-input-number>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item prop="originalPrice" label="原价">
              <el-input-number v-model="data.form.originalPrice" :min="0.01" :precision="2" :step="10" style="width: 100%"></el-input-number>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item prop="stockQuantity" label="库存">
              <!-- 库存直接存在 product.stock_quantity，管理员在这里改完就生效 -->
              <el-input-number v-model="data.form.stockQuantity" :min="0" :step="10" style="width: 100%"></el-input-number>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item prop="sort" label="排序">
              <el-input-number v-model="data.form.sort" :min="1" :max="999" style="width: 100%"></el-input-number>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item prop="status" label="商品状态">
              <el-radio-group v-model="data.form.status">
                <el-radio-button label="ON_SALE">上架</el-radio-button>
                <el-radio-button label="OFF_SALE">下架</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item prop="isRecommend" label="首页推荐">
              <el-radio-group v-model="data.form.isRecommend">
                <el-radio-button :label="1">推荐</el-radio-button>
                <el-radio-button :label="0">普通</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item prop="coverImage" label="商品封面">
          <div class="upload-box">
            <!-- 回显当前封面：编辑时 data.form.coverImage 来自表格行，新增时上传成功后写入 -->
            <el-image v-if="data.form.coverImage" class="upload-preview" :src="data.form.coverImage" fit="cover"
                      :preview-src-list="[data.form.coverImage]" preview-teleported></el-image>
            <!-- show-file-list 关掉：图片以 data.form 里的地址为准，不用组件内部的临时列表 -->
            <el-upload :action="baseUrl + '/files/upload'" :on-success="handleCoverUpload" :show-file-list="false">
              <el-button type="primary">{{ data.form.coverImage ? '重新上传' : '上传封面' }}</el-button>
            </el-upload>
          </div>
        </el-form-item>
        <el-form-item prop="albumImages" label="商品相册">
          <div class="upload-box">
            <!-- albumImages 用逗号分隔存多张图，逐张回显并支持删除 -->
            <div class="album-item" v-for="(item, index) in albumList" :key="item">
              <el-image class="upload-preview" :src="item" fit="cover" :preview-src-list="albumList" preview-teleported></el-image>
              <el-button class="album-remove" link type="danger" @click="removeAlbum(index)">删除</el-button>
            </div>
            <el-upload :action="baseUrl + '/files/upload'" :on-success="handleAlbumUpload" :show-file-list="false" multiple>
              <el-button type="primary">上传相册图片</el-button>
            </el-upload>
          </div>
        </el-form-item>
        <el-form-item prop="sellingPoint" label="商品卖点">
          <el-input v-model="data.form.sellingPoint" type="textarea" :rows="3" placeholder="请输入商品核心卖点"></el-input>
        </el-form-item>
        <el-form-item prop="tags" label="商品标签">
          <el-input v-model="data.form.tags" placeholder="多个标签用中文逗号分隔，例如 轻薄本，学生党，高性价比"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="data.formVisible = false">取消</el-button>
          <el-button type="primary" @click="save">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from "vue";
import request from "@/utils/request.js";
import {ElMessage, ElMessageBox} from "element-plus";
import {Delete, Edit} from "@element-plus/icons-vue";

const baseUrl = import.meta.env.VITE_BASE_URL
const formRef = ref()

const data = reactive({
  formVisible: false,
  form: {},
  tableData: [],
  categoryOptions: [],
  brandOptions: [],
  pageNum: 1,
  pageSize: 10,
  total: 0,
  productNo: null,
  name: null,
  categoryId: null,
  brandId: null,
  status: null,
  ids: [],
  rules: {
    name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
    categoryId: [{ required: true, message: '请选择商品分类', trigger: 'change' }],
    brandId: [{ required: true, message: '请选择商品品牌', trigger: 'change' }],
    price: [{ required: true, message: '请输入商品售价', trigger: 'blur' }]
  }
})

const albumList = computed(() => {
  return data.form.albumImages ? data.form.albumImages.split(',').filter(Boolean) : []
})

const formatMoney = (value) => {
  return Number(value || 0).toFixed(2)
}

const statusName = (status) => {
  return status === 'ON_SALE' ? '上架' : '下架'
}

const loadOptions = () => {
  request.get('/productCategory/selectAll', {
    params: {
      isEnabled: 1
    }
  }).then(res => {
    if (res.code === '200') {
      data.categoryOptions = res.data || []
    }
  })
  request.get('/productBrand/selectAll', {
    params: {
      isEnabled: 1
    }
  }).then(res => {
    if (res.code === '200') {
      data.brandOptions = res.data || []
    }
  })
}

const load = () => {
  request.get('/product/selectPage', {
    params: {
      pageNum: data.pageNum,
      pageSize: data.pageSize,
      productNo: data.productNo,
      name: data.name,
      categoryId: data.categoryId,
      brandId: data.brandId,
      status: data.status
    }
  }).then(res => {
    if (res.code === '200') {
      data.tableData = res.data?.list || []
      data.total = res.data?.total
    }
  })
}

const handleAdd = () => {
  data.form = {
    price: 99,
    originalPrice: 129,
    stockQuantity: 0,
    sort: 1,
    status: 'ON_SALE',
    isRecommend: 0
  }
  data.formVisible = true
}

const handleEdit = (row) => {
  data.form = JSON.parse(JSON.stringify(row))
  data.formVisible = true
}

const add = () => {
  request.post('/product/add', data.form).then(res => {
    if (res.code === '200') {
      ElMessage.success('操作成功')
      data.formVisible = false
      load()
    } else {
      ElMessage.error(res.msg)
    }
  })
}

const update = () => {
  request.put('/product/update', data.form).then(res => {
    if (res.code === '200') {
      ElMessage.success('操作成功')
      data.formVisible = false
      load()
    } else {
      ElMessage.error(res.msg)
    }
  })
}

const save = () => {
  formRef.value.validate(valid => {
    if (valid) {
      data.form.id ? update() : add()
    }
  })
}

const del = (id) => {
  ElMessageBox.confirm('删除后数据无法恢复，您确定删除吗？', '删除确认', { type: 'warning' }).then(() => {
    request.delete('/product/delete/' + id).then(res => {
      if (res.code === '200') {
        ElMessage.success("删除成功")
        load()
      } else {
        ElMessage.error(res.msg)
      }
    })
  })
}

const delBatch = () => {
  if (!data.ids.length) {
    ElMessage.warning("请选择数据")
    return
  }
  ElMessageBox.confirm('删除后数据无法恢复，您确定删除吗？', '删除确认', { type: 'warning' }).then(() => {
    request.delete("/product/delete/batch", {data: data.ids}).then(res => {
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

const handleCoverUpload = (res) => {
  data.form.coverImage = res.data
}

const handleAlbumUpload = (res) => {
  const list = albumList.value.slice()
  list.push(res.data)
  data.form.albumImages = list.join(',')
}

const removeAlbum = (index) => {
  const list = albumList.value.slice()
  list.splice(index, 1)
  data.form.albumImages = list.join(',')
}

const reset = () => {
  data.productNo = null
  data.name = null
  data.categoryId = null
  data.brandId = null
  data.status = null
  data.pageNum = 1
  load()
}

loadOptions()
load()
</script>

<style scoped>
.page-heading {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20px;
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

.upload-box {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.upload-preview {
  width: 72px;
  height: 72px;
  border-radius: 6px;
  border: 1px solid #eceff5;
}

.album-item {
  text-align: center;
}

.album-remove {
  display: block;
  margin: 0 auto;
}

.product-cover {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  display: block;
  object-fit: cover;
}

.money-text {
  color: #0f766e;
  font-weight: 700;
}
</style>
