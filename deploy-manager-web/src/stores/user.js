import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { login as loginApi, logout as logoutApi, getMe } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  const router = useRouter()

  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(null)

  const role = computed(() => userInfo.value?.role || '')
  const displayName = computed(() => userInfo.value?.displayName || userInfo.value?.username || '')

  const isLoggedIn = computed(() => !!token.value)

  async function login(username, password) {
    const res = await loginApi({ username, password })
    const newToken = res?.token || res?.accessToken
    if (newToken) {
      token.value = newToken
      localStorage.setItem('token', newToken)
    }
    await fetchUserInfo()
  }

  async function logout() {
    try {
      await logoutApi()
    } catch (e) {
      // ignore
    } finally {
      token.value = ''
      userInfo.value = null
      localStorage.removeItem('token')
      router.push('/login')
    }
  }

  async function fetchUserInfo() {
    const info = await getMe()
    userInfo.value = info
  }

  return {
    token,
    userInfo,
    role,
    displayName,
    isLoggedIn,
    login,
    logout,
    fetchUserInfo
  }
})
