import apiClient from './axios'

export interface LoginRequest {
  email: string
  password: string
}

export interface RegisterRequest {
  email: string
  username: string
  password: string
}

export interface LoginResponse {
  accessToken: string
}

export interface User {
  id: number
  email: string
  username: string
  provider: string
  enabled: boolean
  createdAt: string
  roles: string[]
}

export const authApi = {
  register: (data: RegisterRequest) => apiClient.post('/api/auth/register', data),
  login: (data: LoginRequest) => apiClient.post<LoginResponse>('/api/auth/login', data),
  me: () => apiClient.get<User>('/api/auth/me'),
}

export const userApi = {
  getAllUsers: () => apiClient.get<User[]>('/api/users'),
}

