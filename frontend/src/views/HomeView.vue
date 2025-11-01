<template>
  <div class="home-container">
    <div v-if="authStore.isAuthenticated && authStore.user" class="welcome-card">
      <h1>Welcome, {{ authStore.user.username }}!</h1>
      <div class="user-info">
        <p><strong>Email:</strong> {{ authStore.user.email }}</p>
        <p><strong>Provider:</strong> {{ authStore.user.provider }}</p>
        <p><strong>Roles:</strong> {{ authStore.user.roles.join(', ') }}</p>
      </div>
    </div>
    <div v-else class="welcome-card">
      <h1>Welcome to SSO App</h1>
      <p>Please <router-link to="/login">login</router-link> to continue.</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
</script>

<style scoped>
.home-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: calc(100vh - 200px);
  padding: 2rem;
}

.welcome-card {
  background: white;
  padding: 2rem;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  width: 100%;
  max-width: 600px;
}

h1 {
  margin-bottom: 1.5rem;
  color: #333;
}

.user-info {
  margin-top: 1.5rem;
}

.user-info p {
  margin: 0.75rem 0;
  color: #666;
}

.user-info strong {
  color: #333;
}

a {
  color: #007bff;
  text-decoration: none;
}

a:hover {
  text-decoration: underline;
}
</style>

