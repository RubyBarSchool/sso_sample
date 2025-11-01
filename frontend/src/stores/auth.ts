import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi, type User } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem('accessToken'))
  const user = ref<User | null>(null)

  const isAuthenticated = computed(() => !!token.value && !!user.value)

  const setToken = (newToken: string) => {
    token.value = newToken
    localStorage.setItem('accessToken', newToken)
  }

  const login = async (email: string, password: string) => {
    const response = await authApi.login({ email, password })
    const accessToken = response.data.accessToken
    setToken(accessToken)
    await fetchMe()
  }

  const fetchMe = async () => {
    try {
      const response = await authApi.me()
      user.value = response.data
    } catch (error) {
      console.error('Failed to fetch user:', error)
      logout()
    }
  }

  const setTokenFromCallback = async (newToken: string) => {
    setToken(newToken)
    await fetchMe()
  }

  const logout = () => {
    token.value = null
    user.value = null
    localStorage.removeItem('accessToken')
  }

  // Load user on init if token exists
  if (token.value) {
    fetchMe()
  }

  return {
    token,
    user,
    isAuthenticated,
    login,
    fetchMe,
    setTokenFromCallback,
    logout,
  }
})

