<template>
  <div class="users-container">
    <h1>Users</h1>
    <div v-if="loading" class="loading">Loading users...</div>
    <div v-else-if="error" class="error">{{ error }}</div>
    <div v-else class="users-list">
      <div v-for="user in users" :key="user.id" class="user-card">
        <div class="user-header">
          <h3>{{ user.username }}</h3>
          <span class="badge" :class="user.provider.toLowerCase()">{{ user.provider }}</span>
        </div>
        <p class="user-email">{{ user.email }}</p>
        <div class="user-roles">
          <span
            v-for="role in user.roles"
            :key="role"
            class="role-badge"
          >
            {{ role }}
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { userApi, type User } from '@/api/auth'

const users = ref<User[]>([])
const loading = ref(true)
const error = ref('')

onMounted(async () => {
  try {
    const response = await userApi.getAllUsers()
    users.value = response.data
  } catch (err: any) {
    error.value = err.response?.data?.message || 'Failed to load users'
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.users-container {
  padding: 2rem;
}

h1 {
  margin-bottom: 2rem;
  color: #333;
}

.loading,
.error {
  text-align: center;
  padding: 2rem;
  color: #666;
}

.error {
  color: #c33;
}

.users-list {
  display: grid;
  gap: 1rem;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
}

.user-card {
  background: white;
  padding: 1.5rem;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.user-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.5rem;
}

.user-header h3 {
  margin: 0;
  color: #333;
}

.badge {
  padding: 0.25rem 0.75rem;
  border-radius: 12px;
  font-size: 0.75rem;
  font-weight: 600;
  text-transform: uppercase;
}

.badge.local {
  background: #e3f2fd;
  color: #1976d2;
}

.badge.google {
  background: #fff3e0;
  color: #f57c00;
}

.badge.microsoft {
  background: #f3e5f5;
  color: #7b1fa2;
}

.user-email {
  color: #666;
  font-size: 0.9rem;
  margin: 0.5rem 0;
}

.user-roles {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
  margin-top: 1rem;
}

.role-badge {
  background: #f0f0f0;
  color: #666;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  font-size: 0.8rem;
}
</style>

