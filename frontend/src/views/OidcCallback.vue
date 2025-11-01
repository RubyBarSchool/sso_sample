<template>
  <div class="callback-container">
    <div class="callback-message">Signing you in...</div>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

onMounted(async () => {
  // console.log('OIDC Callback mounted, processing token...')
  // const hash = window.location.hash || route.hash
  // console.log('Current URL hash:', hash)
  // const params = new URLSearchParams(hash.replace(/^#/, ''))
  // console.log('Extracted params from hash:', params.toString())
  // const token = params.get('token')
  const token = route.query.token as string
  const provider = route.query.provider as string

  if (token) {
    try {
      await authStore.setTokenFromCallback(token)
      router.replace('/')
    } catch (error) {
      console.error('Failed to set token:', error)
      router.replace('/login?error=missing_token')
    }
  } else {
    router.replace('/login?error=missing_token')
  }
})
</script>

<style scoped>
.callback-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: calc(100vh - 200px);
  padding: 2rem;
}

.callback-message {
  font-size: 1.2rem;
  color: #666;
}
</style>

