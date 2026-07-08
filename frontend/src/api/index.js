import http from '../utils/axios'

// Auth
export const login = (data) => http.post('/auth/login', data)
export const register = (data) => http.post('/auth/register', data)
export const refreshToken = (data) => http.post('/auth/refresh', data)
export const changePassword = (data) => http.post('/auth/change-password', data)

// Books
export const getBooks = (params) => http.get('/books', { params })
export const getBook = (id) => http.get(`/books/${id}`)
export const addBook = (data) => http.post('/books', data)
export const updateBook = (id, data) => http.put(`/books/${id}`, data)
export const deleteBook = (id) => http.delete(`/books/${id}`)
export const getInventoryLogs = (id) => http.get(`/books/${id}/inventory-logs`)

// Categories
export const getCategories = () => http.get('/categories')
export const getCategoryTree = () => http.get('/categories/tree')
export const createCategory = (data) => http.post('/categories', data)
export const updateCategory = (id, data) => http.put(`/categories/${id}`, data)
export const deleteCategory = (id) => http.delete(`/categories/${id}`)

// Borrow
export const borrowBook = (data) => http.post('/borrows', data)
export const returnBook = (id) => http.put(`/borrows/${id}/return`)
export const renewBook = (id) => http.put(`/borrows/${id}/renew`)
export const reportLostBook = (id) => http.put(`/borrows/${id}/report-lost`)
export const getAllBorrows = (params) => http.get('/borrows', { params })
export const getMyBorrows = () => http.get('/borrows/my')
export const getActiveBorrowCount = () => http.get('/borrows/my/active-count')

// Reservations
export const reserveBook = (data) => http.post('/reservations', data)
export const cancelReservation = (id) => http.delete(`/reservations/${id}`)
export const getAllReservations = (params) => http.get('/reservations', { params })
export const getMyReservations = () => http.get('/reservations/my')
export const getQueuePosition = (bookId) => http.get('/reservations/queue-position', { params: { bookId } })
export const getReservationCount = (bookId) => http.get(`/reservations/book/${bookId}/count`)

// Fines
export const getMyFines = () => http.get('/fines/my')
export const getUnpaidAmount = () => http.get('/fines/my/unpaid')
export const payFine = (id) => http.put(`/fines/${id}/pay`)
export const waiveFine = (id) => http.put(`/fines/${id}/waive`)
export const getAllFines = (params) => http.get('/fines', { params })
export const getFineStats = () => http.get('/fines/stats')

// Users (admin)
export const getUsers = () => http.get('/users')
export const updateUserRoles = (id, roles) => http.put(`/users/${id}/roles`, { roles })
export const toggleUserStatus = (id) => http.put(`/users/${id}/toggle-status`)

// Statistics
export const getDashboard = () => http.get('/statistics/dashboard')
export const getBorrowTrend = (params) => http.get('/statistics/borrow-trend', { params })
export const getReturnTrend = (start, end) => http.get('/statistics/return-trend', { params: { start, end } })
export const getMonthlyComparison = () => http.get('/statistics/monthly-comparison')
export const getCategoryDistribution = () => http.get('/statistics/category-distribution')
export const getBookStatusDistribution = () => http.get('/statistics/book-status-distribution')
export const getPopularBooks = (limit) => http.get('/statistics/popular-books', { params: { limit } })
export const getTopReaders = (limit) => http.get('/statistics/top-readers', { params: { limit } })
export const getOverdueReport = () => http.get('/statistics/overdue-report')

// Config (admin)
export const getConfigs = () => http.get('/config')
export const updateConfigs = (data) => http.put('/config', data)
