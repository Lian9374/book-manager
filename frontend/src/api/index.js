import http from '../utils/axios'

// Auth
export const login = (data) => http.post('/auth/login', data)
export const register = (data) => http.post('/auth/register', data)

// Books
export const getBooks = (params) => http.get('/books', { params })
export const getBook = (id) => http.get(`/books/${id}`)
export const addBook = (data) => http.post('/books', data)
export const updateBook = (id, data) => http.put(`/books/${id}`, data)
export const getInventoryLogs = (id) => http.get(`/books/${id}/inventory-logs`)

// Categories
export const getCategories = () => http.get('/categories')
export const createCategory = (data) => http.post('/categories', data)

// Borrow
export const borrowBook = (data) => http.post('/borrows', data)
export const returnBook = (id) => http.put(`/borrows/${id}/return`)
export const renewBook = (id) => http.put(`/borrows/${id}/renew`)
export const getAllBorrows = () => http.get('/borrows')
export const getMyBorrows = () => http.get('/borrows/my')

// Reservations
export const reserveBook = (data) => http.post('/reservations', data)
export const cancelReservation = (id) => http.delete(`/reservations/${id}`)
export const getAllReservations = () => http.get('/reservations')
export const getMyReservations = () => http.get('/reservations/my')

// Fines
export const getMyFines = () => http.get('/fines/my')
export const getUnpaidAmount = () => http.get('/fines/my/unpaid')
export const payFine = (id) => http.put(`/fines/${id}/pay`)

// Users (admin)
export const getUsers = () => http.get('/users')
export const updateUserRoles = (id, roles) => http.put(`/users/${id}/roles`, { roles })
export const toggleUserStatus = (id) => http.put(`/users/${id}/toggle-status`)

// Statistics
export const getDashboard = () => http.get('/statistics/dashboard')
export const getBorrowTrend = (days) => http.get('/statistics/borrow-trend', { params: { days } })
export const getPopularBooks = (limit) => http.get('/statistics/popular-books', { params: { limit } })
export const getOverdueReport = () => http.get('/statistics/overdue-report')

// Config (admin)
export const getConfigs = () => http.get('/config')
export const updateConfigs = (data) => http.put('/config', data)
