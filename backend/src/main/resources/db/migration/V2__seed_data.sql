-- =============================================
-- V2: Seed Data — Roles, Admin User, Default Configs, Sample Categories
-- =============================================

-- Roles
INSERT INTO roles (role_name, description) VALUES
('ADMIN', 'System administrator — full access'),
('LIBRARIAN', 'Librarian — manages books, returns, views statistics'),
('READER', 'Reader — browses, borrows, reserves books');

-- Default admin user (password: admin123 — BCrypt hashed)
INSERT INTO users (username, password_hash, real_name, email, phone) VALUES
('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'System Admin', 'admin@bookmanager.com', '13800000001'),
('librarian1', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Zhang Librarian', 'zhang@bookmanager.com', '13800000002'),
('reader1', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Li Reader', 'li@bookmanager.com', '13800000003');

-- Assign roles
INSERT INTO user_roles (user_id, role_id) VALUES
(1, 1), -- admin → ADMIN
(2, 2), -- librarian1 → LIBRARIAN
(3, 3); -- reader1 → READER

-- Default system configs
INSERT INTO system_configs (config_key, config_value, description) VALUES
('MAX_BORROW_COUNT', '5', 'Maximum books a reader can borrow simultaneously'),
('BORROW_DURATION_DAYS', '30', 'Default borrow duration in days'),
('MAX_RENEW_COUNT', '2', 'Maximum times a borrow can be renewed'),
('OVERDUE_FINE_PER_DAY', '0.50', 'Fine amount per overdue day (yuan)'),
('RESERVATION_EXPIRE_DAYS', '7', 'Days a reservation stays valid before auto-cancelling'),
('RESERVATION_FULFILL_DAYS', '3', 'Days reader has to borrow after reservation is ready');

-- Sample categories (2-level tree)
INSERT INTO categories (id, name, parent_id, sort_order) VALUES
(1, 'Computer Science', NULL, 1),
(2, 'Literature', NULL, 2),
(3, 'Science', NULL, 3),
(4, 'History', NULL, 4),
(5, 'Programming Languages', 1, 1),
(6, 'Database', 1, 2),
(7, 'Artificial Intelligence', 1, 3),
(8, 'Chinese Literature', 2, 1),
(9, 'Foreign Literature', 2, 2),
(10, 'Physics', 3, 1),
(11, 'Mathematics', 3, 2);

-- Sample books
INSERT INTO books (isbn, title, author, publisher, publish_date, category_id, total_copies, available_copies, location, description) VALUES
('978-7-111-66666-1', 'Spring Boot in Action', 'Craig Walls', 'Manning', '2023-01-15', 5, 3, 3, 'A-01-01', 'A comprehensive guide to Spring Boot development'),
('978-7-111-66666-2', 'Java Core Technology', 'Cay Horstmann', 'Pearson', '2022-06-20', 5, 5, 5, 'A-01-02', 'Java core technology volume I and II'),
('978-7-111-66666-3', 'MySQL Deep Dive', 'Jiang Chengyao', 'Posts & Telecom Press', '2021-09-10', 6, 2, 2, 'A-02-01', 'In-depth exploration of MySQL internals and optimization'),
('978-7-111-66666-4', 'Deep Learning', 'Ian Goodfellow', 'MIT Press', '2020-03-15', 7, 3, 2, 'A-03-01', 'The definitive textbook on deep learning'),
('978-7-111-66666-5', 'Dream of the Red Chamber', 'Cao Xueqin', 'People''s Literature Publishing House', '2019-05-20', 8, 4, 4, 'B-01-01', 'One of the four great classical novels of Chinese literature'),
('978-7-111-66666-6', 'The Three-Body Problem', 'Liu Cixin', 'Chongqing Publishing House', '2021-12-01', 8, 5, 4, 'B-01-02', 'Hugo Award-winning science fiction trilogy'),
('978-7-111-66666-7', 'A Brief History of Time', 'Stephen Hawking', 'Bantam', '2020-08-01', 10, 2, 2, 'C-01-01', 'From the Big Bang to black holes'),
('978-7-111-66666-8', 'Linear Algebra Done Right', 'Sheldon Axler', 'Springer', '2022-02-28', 11, 3, 3, 'C-02-01', 'A rigorous yet accessible approach to linear algebra');
