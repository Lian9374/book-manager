import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const http = axios.create({
  baseURL: '/api',
  timeout: 15000
})

// Track whether we are currently refreshing the token
let isRefreshing = false
let failedQueue = []

function processQueue(error, token = null) {
  failedQueue.forEach(prom => {
    if (error) {
      prom.reject(error)
    } else {
      prom.resolve(token)
    }
  })
  failedQueue = []
}

// Request interceptor — attach JWT token
http.interceptors.request.use(
  config => {
    const token = localStorage.getItem('accessToken')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error)
)

// Response interceptor — handle errors and auto-refresh
http.interceptors.response.use(
  response => {
    const data = response.data
    if (data.code !== 200 && data.code !== undefined) {
      ElMessage.error(data.message || 'Request failed')
      return Promise.reject(new Error(data.message))
    }
    return data
  },
  async error => {
    const originalRequest = error.config

    if (error.response) {
      const status = error.response.status

      // Auto-refresh token on 401 (skip auth endpoints)
      const isAuthEndpoint = originalRequest.url.includes('/auth/login')
                          || originalRequest.url.includes('/auth/refresh')
                          || originalRequest.url.includes('/auth/register')

      if (status === 401 && !originalRequest._retry && !isAuthEndpoint) {
        const refreshToken = localStorage.getItem('refreshToken')
        if (refreshToken) {
          originalRequest._retry = true

          if (isRefreshing) {
            // Queue this request until refresh completes
            return new Promise((resolve, reject) => {
              failedQueue.push({ resolve, reject })
            }).then(token => {
              originalRequest.headers.Authorization = `Bearer ${token}`
              return http(originalRequest)
            })
          }

          isRefreshing = true

          try {
            const res = await axios.post('/api/auth/refresh', { refreshToken })
            const data = res.data.data
            const newToken = data.accessToken

            localStorage.setItem('accessToken', newToken)
            localStorage.setItem('refreshToken', data.refreshToken)
            localStorage.setItem('userInfo', JSON.stringify({
              userId: data.userId,
              username: data.username,
              realName: data.realName,
              roles: data.roles
            }))

            processQueue(null, newToken)
            originalRequest.headers.Authorization = `Bearer ${newToken}`
            return http(originalRequest)
          } catch (refreshError) {
            processQueue(refreshError, null)
            localStorage.removeItem('accessToken')
            localStorage.removeItem('refreshToken')
            localStorage.removeItem('userInfo')
            router.push('/login')
            ElMessage.error('Session expired. Please login again.')
            return Promise.reject(refreshError)
          } finally {
            isRefreshing = false
          }
        }

        // No refresh token available
        localStorage.removeItem('accessToken')
        localStorage.removeItem('refreshToken')
        localStorage.removeItem('userInfo')
        router.push('/login')
        ElMessage.error('Session expired. Please login again.')
      } else if (status === 403) {
        ElMessage.error(error.response.data?.message || 'Access denied')
      } else {
        ElMessage.error(error.response.data?.message || 'Server error')
      }
    } else {
      ElMessage.error('Network error')
    }
    return Promise.reject(error)
  }
)

export default http
