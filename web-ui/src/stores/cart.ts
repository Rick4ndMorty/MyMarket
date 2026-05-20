import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

interface CartItem {
  skuId: number
  skuName: string
  price: number
  quantity: number
  productId: number
  productName: string
  image: string
  shopId: number
}

function loadCart(): CartItem[] {
  try {
    const raw = localStorage.getItem('cart')
    return raw ? JSON.parse(raw) : []
  } catch {
    return []
  }
}

function saveCart(items: CartItem[]) {
  localStorage.setItem('cart', JSON.stringify(items))
}

export const useCartStore = defineStore('cart', () => {
  const items = ref<CartItem[]>(loadCart())

  const totalPrice = computed(() => {
    return items.value.reduce((sum, item) => sum + item.price * item.quantity, 0)
  })

  const totalCount = computed(() => {
    return items.value.reduce((sum, item) => sum + item.quantity, 0)
  })

  function addItem(item: CartItem) {
    const existing = items.value.find(i => i.skuId === item.skuId)
    if (existing) {
      existing.quantity += item.quantity
    } else {
      items.value.push({ ...item })
    }
    saveCart(items.value)
  }

  function removeItem(skuId: number) {
    items.value = items.value.filter(i => i.skuId !== skuId)
    saveCart(items.value)
  }

  function updateQuantity(skuId: number, quantity: number) {
    const item = items.value.find(i => i.skuId === skuId)
    if (item) {
      item.quantity = Math.max(1, quantity)
    }
    saveCart(items.value)
  }

  function clear() {
    items.value = []
    saveCart(items.value)
  }

  return { items, totalPrice, totalCount, addItem, removeItem, updateQuantity, clear }
})
