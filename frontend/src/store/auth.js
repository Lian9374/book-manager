import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi } from '../api'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('accessToken') || '')
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

  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('userInfo')
  }

  return { token, userInfo, isLoggedIn, roles, isAdmin, isLibrarian, isReader, login, logout }
})
