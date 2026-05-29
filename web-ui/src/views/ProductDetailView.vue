<template>
  <div class="product-detail" v-loading="loading">
    <template v-if="product.id">
      <div class="product-main">
        <div class="product-gallery">
          <img
            :src="currentImage || '/vite.svg'"
            :alt="product.productName"
            class="main-image"
          />
          <div class="thumb-list" v-if="product.images?.length > 1">
            <img
              v-for="(img, idx) in product.images"
              :key="idx"
              :src="img"
              :class="['thumb', { active: currentImage === img }]"
              @click="currentImage = img"
            />
          </div>
        </div>

        <div class="product-info">
          <h1 class="product-name">{{ product.productName }}</h1>
          <div class="product-price-box">
            <span class="price-label">价格</span>
            <span class="price-value">&yen;{{ formatPrice(selectedSku ? selectedSku.price : product.minPrice) }}</span>
          </div>

          <div class="product-sku" v-if="product.skus?.length > 0">
            <span class="sku-label">规格</span>
            <div class="sku-list">
              <span
                v-for="sku in product.skus"
                :key="sku.id"
                :class="['sku-item', { active: selectedSku?.id === sku.id }]"
                @click="selectSku(sku)"
              >
                {{ sku.skuName }}
              </span>
            </div>
          </div>

          <div class="quantity-wrap">
            <span class="qty-label">数量</span>
            <el-input-number
              v-model="quantity"
              :min="1"
              :max="selectedSku?.stock || 1"
            />
            <span class="stock-tip" v-if="selectedSku">
              库存 {{ selectedSku.stock }} 件
            </span>
          </div>

          <div class="action-buttons">
            <el-button size="large" @click="addToCart">加入购物车</el-button>
            <el-button size="large" type="primary" @click="buyNow">立即购买</el-button>
          </div>

          <div class="shop-info" v-if="product.shopId">
            <span>店铺：</span>
            <router-link :to="`/shop/${product.shopId}`">
              {{ product.shopName || '查看店铺' }}
            </router-link>
            <el-button
              size="small"
              type="primary"
              plain
              style="margin-left: 12px"
              @click="$router.push(`/shop/${product.shopId}`)"
            >
              联系客服
            </el-button>
          </div>
        </div>
      </div>

      <div class="product-description">
        <el-card header="商品详情">
          <div class="desc-content" v-html="product.description || '暂无商品详情'"></div>
        </el-card>
      </div>

      <!-- Reviews & Questions -->
      <div class="review-section" v-if="product.id">
        <el-tabs v-model="reviewTab" @tab-change="onReviewTabChange">
          <el-tab-pane label="商品评价" name="REVIEW">
            <div v-if="reviews.length > 0">
              <div v-for="r in reviews" :key="r.id" class="review-item">
                <div class="review-header">
                  <span class="review-rating" v-if="r.rating">
                    <span v-for="s in 5" :key="s" :class="s <= r.rating ? 'star-on' : 'star-off'">★</span>
                  </span>
                  <span class="review-user">用户{{ r.userId }}</span>
                  <span class="review-time">{{ r.createTime }}</span>
                </div>
                <div class="review-content">{{ r.content }}</div>
                <!-- 追评按钮（未展开时显示） -->
                <el-button
                  v-if="userStore.token && replyingReviewId !== r.id"
                  text
                  size="small"
                  type="primary"
                  @click="startReply(r.id)"
                >
                  追评
                </el-button>
                <!-- 追评回答框（内联在对应评论下方，独立于底部的新建评论框） -->
                <div v-if="replyingReviewId === r.id" class="followup-form">
                  <el-input
                    v-model="followupContent"
                    type="textarea"
                    :rows="2"
                    placeholder="写下你的追评..."
                  />
                  <div style="margin-top: 6px; display: flex; gap: 8px;">
                    <el-button size="small" type="primary" :loading="reviewSubmitting" @click="submitFollowup(r.id)">
                      提交追评
                    </el-button>
                    <el-button size="small" @click="cancelReply">取消</el-button>
                  </div>
                </div>
                <div v-if="r.followups?.length" class="followup-list">
                  <div v-for="f in r.followups" :key="f.id" class="followup-item">
                    <span class="followup-tag">追评</span>
                    {{ f.content }}
                    <span class="review-time">{{ f.createTime }}</span>
                  </div>
                </div>
              </div>
              <el-pagination
                v-if="reviewTotal > reviewSize"
                layout="prev, pager, next"
                :total="reviewTotal"
                :page-size="reviewSize"
                @current-change="(p) => { reviewPage = p; fetchReviews() }"
              />
            </div>
            <el-empty v-else description="暂无评价" :image-size="40" />
            <div v-if="userStore.token" class="review-form">
              <el-rate v-model="newRating" show-text />
              <el-input
                v-model="newReviewContent"
                type="textarea"
                :rows="3"
                placeholder="写下你的评价..."
                style="margin-top: 8px"
              />
              <el-button
                type="primary"
                size="small"
                style="margin-top: 8px"
                :loading="reviewSubmitting"
                @click="submitReview"
              >
                发表评价
              </el-button>
            </div>
          </el-tab-pane>
          <el-tab-pane label="商品问答" name="QUESTION">
            <div v-if="questions.length > 0">
              <div v-for="q in questions" :key="q.id" class="question-item">
                <div class="question-header">
                  <span class="q-label">Q</span>
                  <span>{{ q.content }}</span>
                  <span class="review-time">{{ q.createTime }}</span>
                </div>
                <div v-if="q.answers?.length" class="answer-list">
                  <div v-for="a in q.answers" :key="a.id" class="answer-item">
                    <span :class="a.userId === q.userId ? 'q-label' : 'a-label'">{{ a.userId === q.userId ? 'Q' : 'A' }}</span>
                    <span>{{ a.content }}</span>
                    <span class="review-time">{{ a.createTime }}</span>
                  </div>
                </div>
                <div v-if="answeringQuestionId === q.id" class="answer-form">
                  <el-input
                    v-model="newAnswerContent"
                    type="textarea"
                    :rows="2"
                    placeholder="输入你的回答..."
                  />
                  <div style="margin-top: 6px; display: flex; gap: 8px;">
                    <el-button size="small" type="primary" :loading="questionSubmitting" @click="submitAnswer(q.id)">
                      提交回答
                    </el-button>
                    <el-button size="small" @click="cancelAnswer">取消</el-button>
                  </div>
                </div>
                <el-button
                  v-else-if="userStore.token"
                  text
                  size="small"
                  type="primary"
                  @click="showAnswer(q.id)"
                >
                  回答
                </el-button>
              </div>
              <el-pagination
                v-if="questionTotal > questionSize"
                layout="prev, pager, next"
                :total="questionTotal"
                :page-size="questionSize"
                @current-change="(p) => { questionPage = p; fetchQuestions() }"
              />
            </div>
            <el-empty v-else description="暂无问答" :image-size="40" />
            <div v-if="userStore.token" class="review-form">
              <el-input
                v-model="newQuestionContent"
                type="textarea"
                :rows="2"
                placeholder="提出你的问题..."
                style="margin-top: 8px"
              />
              <el-button
                type="primary"
                size="small"
                style="margin-top: 8px"
                :loading="questionSubmitting"
                @click="submitQuestion"
              >
                提问
              </el-button>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </template>

    <el-empty v-else-if="!loading" description="商品不存在" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getDetail, createReview, getReviews, getQuestions } from '@/api/product'
import { useCartStore } from '@/stores/cart'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const cartStore = useCartStore()
const userStore = useUserStore()

const product = ref<any>({})
const loading = ref(false)
const selectedSku = ref<any>(null)
const quantity = ref(1)
const currentImage = ref('')

// reviews
const reviewTab = ref('REVIEW')
const reviews = ref<any[]>([])
const reviewPage = ref(1)
const reviewSize = ref(10)
const reviewTotal = ref(0)
const reviewSubmitting = ref(false)
const newReviewContent = ref('')
const newRating = ref(0)

// questions
const questions = ref<any[]>([])
const questionPage = ref(1)
const questionSize = ref(10)
const questionTotal = ref(0)
const questionSubmitting = ref(false)
const newQuestionContent = ref('')
const replyParentId = ref<number>()
// 追评内联表单状态
const replyingReviewId = ref<number | null>(null)
const followupContent = ref('')
const answeringQuestionId = ref<number>()
const newAnswerContent = ref('')

function formatPrice(price: number) {
  if (price == null) return '0.00'
  return Number(price).toFixed(2)
}

function selectSku(sku: any) {
  selectedSku.value = sku
  quantity.value = 1
}

function addToCart() {
  const sku = selectedSku.value || product.value.skus?.[0]
  if (!sku) {
    ElMessage.warning('请选择规格')
    return
  }
  cartStore.addItem({
    skuId: sku.id,
    skuName: sku.skuName || '默认',
    price: sku.price,
    quantity: quantity.value,
    productId: product.value.id,
    productName: product.value.productName,
    image: product.value.images?.[0] || '',
    shopId: product.value.shopId
  })
  ElMessage.success('已加入购物车')
}

function buyNow() {
  const sku = selectedSku.value || product.value.skus?.[0]
  if (!sku) {
    ElMessage.warning('请选择规格')
    return
  }
  // 将当前商品存入 sessionStorage，直接进入结算页（绕过购物车）
  const buyNowItem = {
    skuId: sku.id,
    skuName: sku.skuName || '默认',
    price: sku.price,
    quantity: quantity.value,
    productId: product.value.id,
    productName: product.value.productName,
    image: product.value.images?.[0] || '',
    shopId: product.value.shopId
  }
  sessionStorage.setItem('buyNowItems', JSON.stringify([buyNowItem]))
  router.push('/checkout')
}

async function fetchProduct() {
  loading.value = true
  try {
    const id = Number(route.params.id)
    const res: any = await getDetail(id)
    product.value = res.data
    if (product.value.images?.length) {
      currentImage.value = product.value.images[0]
    }
    if (product.value.skus?.length) {
      selectedSku.value = product.value.skus[0]
    }
  } catch {
    product.value = {}
  } finally {
    loading.value = false
  }
}

async function fetchReviews() {
  try {
    const res: any = await getReviews(Number(route.params.id), 'REVIEW', reviewPage.value, reviewSize.value)
    reviews.value = res.data?.records || res.data || []
    reviewTotal.value = res.data?.total || 0
  } catch {
    reviews.value = []
  }
}

async function fetchQuestions() {
  try {
    const res: any = await getQuestions(Number(route.params.id), questionPage.value, questionSize.value)
    questions.value = res.data?.records || res.data || []
    questionTotal.value = res.data?.total || 0
  } catch {
    questions.value = []
  }
}

function onReviewTabChange(tab: string) {
  if (tab === 'REVIEW') fetchReviews()
  else fetchQuestions()
}

async function submitReview() {
  if (!newReviewContent.value.trim()) return
  reviewSubmitting.value = true
  const isFollowup = !!replyParentId.value
  try {
    await createReview({
      productId: Number(route.params.id),
      type: isFollowup ? 'FOLLOWUP' : 'REVIEW',
      rating: isFollowup ? undefined : (newRating.value || 5),
      content: newReviewContent.value,
      parentId: replyParentId.value
    })
    newReviewContent.value = ''
    newRating.value = 0
    replyParentId.value = undefined
    ElMessage.success(isFollowup ? '追评发表成功' : '评价发表成功')
    reviewPage.value = 1
    await fetchReviews()
  } catch {
    // handled by interceptor
  } finally {
    reviewSubmitting.value = false
  }
}

function showReply(id: number) {
  replyParentId.value = id
  newReviewContent.value = ''
}

// 追评内联表单 - 展开
function startReply(reviewId: number) {
  replyingReviewId.value = reviewId
  followupContent.value = ''
}

// 追评内联表单 - 取消
function cancelReply() {
  replyingReviewId.value = null
  followupContent.value = ''
}

// 追评内联表单 - 提交
async function submitFollowup(reviewId: number) {
  if (!followupContent.value.trim()) return
  reviewSubmitting.value = true
  try {
    await createReview({
      productId: Number(route.params.id),
      type: 'FOLLOWUP',
      content: followupContent.value,
      parentId: reviewId
    })
    followupContent.value = ''
    replyingReviewId.value = null
    ElMessage.success('追评发表成功')
    reviewPage.value = 1
    await fetchReviews()
  } catch {
    // handled by interceptor
  } finally {
    reviewSubmitting.value = false
  }
}

async function submitQuestion() {
  if (!newQuestionContent.value.trim()) return
  questionSubmitting.value = true
  try {
    await createReview({
      productId: Number(route.params.id),
      type: 'QUESTION',
      content: newQuestionContent.value
    })
    newQuestionContent.value = ''
    ElMessage.success('问题已提交')
    questionPage.value = 1
    await fetchQuestions()
  } catch {
    // handled by interceptor
  } finally {
    questionSubmitting.value = false
  }
}

function showAnswer(questionId: number) {
  answeringQuestionId.value = questionId
  newAnswerContent.value = ''
}

function cancelAnswer() {
  answeringQuestionId.value = undefined
  newAnswerContent.value = ''
}

async function submitAnswer(questionId: number) {
  if (!newAnswerContent.value.trim()) return
  questionSubmitting.value = true
  try {
    await createReview({
      productId: Number(route.params.id),
      type: 'ANSWER',
      content: newAnswerContent.value,
      parentId: questionId
    })
    newAnswerContent.value = ''
    answeringQuestionId.value = undefined
    ElMessage.success('回答已提交')
    questionPage.value = 1
    await fetchQuestions()
  } catch {
    // handled by interceptor
  } finally {
    questionSubmitting.value = false
  }
}

onMounted(async () => {
  await fetchProduct()
  if (product.value.id) {
    await fetchReviews()
  }
})
</script>

<style scoped>
.product-detail {
  min-height: 70vh;
}

.product-main {
  display: flex;
  gap: 40px;
  margin-bottom: 30px;
  background: #fff;
  padding: 30px;
  border-radius: 8px;
}

.product-gallery {
  width: 400px;
  flex-shrink: 0;
}

.main-image {
  width: 400px;
  height: 400px;
  object-fit: cover;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
}

.thumb-list {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}

.thumb {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border: 2px solid transparent;
  border-radius: 4px;
  cursor: pointer;
}

.thumb.active {
  border-color: #409eff;
}

.product-info {
  flex: 1;
}

.product-name {
  font-size: 22px;
  color: #303133;
  margin: 0 0 16px;
}

.product-price-box {
  background: #fef0f0;
  padding: 15px;
  border-radius: 4px;
  margin-bottom: 20px;
}

.price-label {
  color: #909399;
  font-size: 14px;
  margin-right: 10px;
}

.price-value {
  color: #f56c6c;
  font-size: 28px;
  font-weight: 700;
}

.product-sku {
  margin-bottom: 20px;
}

.sku-label {
  color: #606266;
  margin-right: 10px;
}

.sku-list {
  display: inline-flex;
  gap: 10px;
  flex-wrap: wrap;
}

.sku-item {
  padding: 6px 16px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.2s;
}

.sku-item:hover {
  border-color: #409eff;
  color: #409eff;
}

.sku-item.active {
  border-color: #409eff;
  background: #ecf5ff;
  color: #409eff;
}

.quantity-wrap {
  margin-bottom: 25px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.qty-label {
  color: #606266;
}

.stock-tip {
  color: #909399;
  font-size: 13px;
}

.action-buttons {
  display: flex;
  gap: 12px;
  margin-bottom: 25px;
}

.shop-info {
  font-size: 14px;
  color: #606266;
}

.shop-info a {
  color: #409eff;
  text-decoration: none;
}

.product-description {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
}

.desc-content {
  min-height: 200px;
  line-height: 1.8;
}

.review-section {
  margin-top: 24px;
  background: #fff;
  border-radius: 8px;
  padding: 20px;
}

.review-item, .question-item {
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.review-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}

.star-on { color: #f7ba2a; }
.star-off { color: #c0c4cc; }

.review-user { color: #909399; font-size: 13px; }
.review-time { color: #c0c4cc; font-size: 12px; margin-left: auto; }

.review-content { font-size: 14px; color: #303133; margin-bottom: 4px; line-height: 1.6; }

.followup-list { margin-top: 8px; padding-left: 16px; border-left: 2px solid #409eff; }
.followup-item { font-size: 13px; color: #606266; margin-bottom: 4px; }
.followup-tag { color: #409eff; font-size: 12px; margin-right: 4px; }

.question-header { display: flex; align-items: flex-start; gap: 8px; font-size: 14px; }
.q-label { color: #e6a23c; font-weight: 700; font-size: 16px; flex-shrink: 0; }

.answer-list { padding-left: 24px; margin: 8px 0; }
.answer-item { display: flex; align-items: flex-start; gap: 8px; font-size: 13px; color: #606266; margin-bottom: 4px; }
.a-label { color: #67c23a; font-weight: 700; font-size: 16px; flex-shrink: 0; }

.review-form { margin-top: 16px; }

.answer-form { margin-top: 8px; padding-left: 24px; }

.shop-link { color: #409eff; text-decoration: none; }

@media (max-width: 768px) {
  .product-main {
    flex-direction: column;
    padding: 16px;
  }
  .product-gallery {
    width: 100%;
  }
  .main-image {
    width: 100%;
    height: auto;
    aspect-ratio: 1;
  }
}
</style>
