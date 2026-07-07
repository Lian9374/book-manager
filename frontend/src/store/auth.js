import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, refreshToken as refreshTokenApi } from '../api'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('accessToken') || '')
  const refreshTokenVal = ref(localStorage.getItem('refreshToken') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || 'null'))

  const isLoggedIn = computed(() => !!token.value)
  const roles = computed(() => userInfo.value?.roles || [])
  const isAdmin = computed(() => roles.value.includes('ADMIN'))
  const isLibrarian = computed(() => roles.value.includes('LIBRARIAN'))
  const isReader = computed(() => roles.value.includes('READER'))

  async function login(username, password) {
    const res = await loginApi({ username, password })
    const data = res.data
    token.value = data.accessToken
    refreshTokenVal.value = data.refreshToken
    userInfo.value = {
      userId: data.userId,
      username: data.username,
      realName: data.realName,
      roles: data.roles
    }
    localStorage.setItem('accessToken', data.accessToken)
    localStorage.setItem('refreshToken', data.refreshToken)
    localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
    return data
  }

  async function refreshAccessToken() {
    if (!refreshTokenVal.value) {
      throw new Error('No refresh token available')
    }
    try {
      const res = await refreshTokenApi({ refreshToken: refreshTokenVal.value })
      const data = res.data
      token.value = data.accessToken
      refreshTokenVal.value = data.refreshToken
      userInfo.value = {
        userId: data.userId,
        username: data.username,
        realName: data.realName,
        roles: data.roles
      }
      localStorage.setItem('accessToken', data.accessToken)
      localStorage.setItem('refreshToken', data.refreshToken)
      localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
      return data.accessToken
    } catch (e) {
      // Refresh failed — force logout
      logout()
      throw e
    }
  }

  function logout() {
    token.value = ''
    refreshTokenVal.value = ''
    userInfo.value = null
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('userInfo')
  }

  return {
    token, refreshTokenVal, userInfo,
    isLoggedIn, roles, isAdmin, isLibrarian, isReader,
    login, refreshAccessToken, logout
  }
})
