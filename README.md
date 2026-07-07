# Online Book Management System

A full-stack online book management system with book cataloging, borrowing/returning, reservations, overdue reminders, inventory management, and admin statistics.

## Tech Stack

- **Backend**: Java 17 + Spring Boot 3.2 + Spring Security + JPA + Flyway
- **Database**: MySQL 8.x
- **Frontend**: Vue 3 + Element Plus + ECharts + Pinia
- **Auth**: JWT with role-based access (ADMIN / LIBRARIAN / READER)

## Quick Start

### Prerequisites
- JDK 17+
- Maven 3.8+
- MySQL 8.x
- Node.js 18+

### Database Setup
```sql
CREATE DATABASE book_manager CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```
Flyway will auto-create tables and seed data on first run.

### Backend
```bash
cd backend
mvn spring-boot:run
```
The server starts on `http://localhost:8080`.

### Frontend
```bash
cd frontend
npm install
npm run dev
```
The dev server starts on `http://localhost:3000`.

### Default Accounts
| Username    | Password  | Role      |
|-------------|-----------|-----------|
| admin       | admin123  | ADMIN     |
| librarian1  | admin123  | LIBRARIAN |
| reader1     | admin123  | READER    |

## Features

- **User Management**: Registration, role assignment, account status control
- **Book Management**: ISBN-based cataloging, category tree, inventory tracking with audit logs
- **Borrowing**: Concurrent-safe borrowing with row-level locking, return processing, renewal
- **Reservations**: Queue-based reservations, auto-expiry, fulfillment notification
- **Fines**: Automatic overdue fine calculation, payment tracking
- **Statistics**: Borrowing trends, popular books, overdue reports with ECharts dashboards
- **Scheduled Tasks**: Daily overdue detection and reservation expiry
- **Security**: JWT authentication, role-based access control, method-level authorization

## Architecture

```
┌──────────────────────────────────────────────────┐
│                  Vue 3 Frontend                  │
│    Element Plus + Pinia + ECharts + Vue Router   │
└────────────────────┬─────────────────────────────┘
                     │ REST API (JWT)
┌────────────────────▼─────────────────────────────┐
│              Spring Boot Backend                 │
│  Controller → Service → Repository → Entity      │
│  Spring Security + @Transactional + @Scheduled   │
└────────────────────┬─────────────────────────────┘
                     │ JPA / Flyway
┌────────────────────▼─────────────────────────────┐
│                   MySQL 8.x                      │
│  11 tables: users, books, borrow_records, etc.   │
└──────────────────────────────────────────────────┘
```

## Business State Machines

- **Book**: AVAILABLE → BORROWED → RESERVED / DAMAGED / LOST → WITHDRAWN
- **Borrow Record**: BORROWING → OVERDUE / RENEWED / LOST → RETURNED
- **Reservation**: PENDING → FULFILLED / CANCELLED / EXPIRED
