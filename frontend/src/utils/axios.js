import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const http = axios.create({
  baseURL: '/api',
  timeout: 15000
})

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

// Response interceptor — handle errors
http.interceptors.response.use(
  response => {
    const data = response.data
    if (data.code !== 200 && data.code !== undefined) {
      ElMessage.error(data.message || 'Request failed')
      return Promise.reject(new Error(data.message))
    }
    return data
  },
  error => {
    if (error.response) {
      const status = error.response.status
      if (status === 401) {
        localStorage.removeItem('accessToken')
        localStorage.removeItem('refreshToken')
        localStorage.removeItem('userInfo')
        router.push('/login')
        ElMessage.error('Session expired. Please login again.')
      } else if (status === 403) {
        ElMessage.error('Access denied')
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
