-- =============================================
-- V3: Sample Borrow/Reservation/Fine Data for Demo
-- =============================================

-- ── Borrow Records ──

-- Book 1 (3 copies) → reader1 borrows 1, overdue 5 days
INSERT INTO borrow_records (user_id, book_id, borrow_date, due_date, return_date, status, renewed_count) VALUES
(3, 1, DATE_SUB(CURDATE(), INTERVAL 35 DAY), DATE_SUB(CURDATE(), INTERVAL 5 DAY), NULL, 'OVERDUE', 0);

-- Book 4 (3 copies) → reader1 borrows 1, currently borrowing
INSERT INTO borrow_records (user_id, book_id, borrow_date, due_date, return_date, status, renewed_count) VALUES
(3, 4, DATE_SUB(CURDATE(), INTERVAL 10 DAY), DATE_ADD(CURDATE(), INTERVAL 20 DAY), NULL, 'BORROWING', 0);

-- Book 7 (2 copies) → reader1 borrows 1, returned with 1-day overdue
INSERT INTO borrow_records (user_id, book_id, borrow_date, due_date, return_date, status, renewed_count) VALUES
(3, 7, DATE_SUB(CURDATE(), INTERVAL 15 DAY), DATE_SUB(CURDATE(), INTERVAL 1 DAY), CURDATE(), 'RETURNED', 1);

-- Book 3 (2 copies) → admin + librarian borrow both copies, fully borrowed → triggers reservation
INSERT INTO borrow_records (user_id, book_id, borrow_date, due_date, status, renewed_count) VALUES
(1, 3, DATE_SUB(CURDATE(), INTERVAL 5 DAY), DATE_ADD(CURDATE(), INTERVAL 25 DAY), 'BORROWING', 0),
(2, 3, DATE_SUB(CURDATE(), INTERVAL 3 DAY), DATE_ADD(CURDATE(), INTERVAL 27 DAY), 'BORROWING', 0);

-- Book 6 (5 copies, 4 available) → borrow remaining copies to make it unreservable
INSERT INTO borrow_records (user_id, book_id, borrow_date, due_date, status, renewed_count) VALUES
(1, 6, DATE_SUB(CURDATE(), INTERVAL 20 DAY), DATE_SUB(CURDATE(), INTERVAL 5 DAY), 'OVERDUE', 0),
(2, 6, DATE_SUB(CURDATE(), INTERVAL 18 DAY), DATE_SUB(CURDATE(), INTERVAL 3 DAY), 'OVERDUE', 0),
(2, 6, DATE_SUB(CURDATE(), INTERVAL 15 DAY), DATE_ADD(CURDATE(), INTERVAL 15 DAY), 'BORROWING', 0),
(1, 6, DATE_SUB(CURDATE(), INTERVAL 10 DAY), DATE_ADD(CURDATE(), INTERVAL 20 DAY), 'BORROWING', 0);

-- ── Update Book Inventory ──

-- Book 1: 3 total → 2 available (1 borrowed)
UPDATE books SET available_copies = 2, status = 'BORROWED' WHERE id = 1 AND available_copies >= 1;
-- Book 3: 2 total → 0 available (2 borrowed)
UPDATE books SET available_copies = 0, status = 'BORROWED' WHERE id = 3;
-- Book 4: 3 total → 2 available (1 borrowed)
UPDATE books SET available_copies = 2, status = 'BORROWED' WHERE id = 4 AND available_copies >= 1;
-- Book 6: 5 total → 0 available (5 borrowed)
UPDATE books SET available_copies = 0, status = 'RESERVED' WHERE id = 6;
-- Book 7: 2 total → 2 available (returned, so back to full)
UPDATE books SET available_copies = 2, status = 'AVAILABLE' WHERE id = 7;

-- ── Reservations ──

-- reader1 reserves book 3 (all copies borrowed)
INSERT INTO reservations (user_id, book_id, reserve_date, expire_date, fulfill_deadline, status) VALUES
(3, 3, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 7 DAY), NULL, 'PENDING');

-- reader1 also reserves book 6 (all copies borrowed)
INSERT INTO reservations (user_id, book_id, reserve_date, expire_date, status) VALUES
(3, 6, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 7 DAY), 'PENDING');

-- ── Fine ──

-- Overdue fine for book 1 (5 days × ¥0.50)
INSERT INTO fines (user_id, borrow_record_id, amount, reason, status) VALUES
(3, 1, 2.50, 'Overdue 5 day(s)', 'UNPAID');

-- Overdue fines for book 6 borrowers
INSERT INTO fines (user_id, borrow_record_id, amount, reason, status) VALUES
(1, 4, 2.50, 'Overdue 5 day(s)', 'UNPAID'),
(2, 5, 1.50, 'Overdue 3 day(s)', 'UNPAID');

-- ── Notification ──

INSERT INTO notifications (user_id, title, content, is_read, type) VALUES
(3, 'Book Overdue', '''Spring Boot in Action'' is overdue by 5 day(s). Fine: ¥2.50', 0, 'OVERDUE'),
(3, 'Reservation Created', 'Your reservation for ''MySQL Deep Dive'' is pending. You are #1 in queue.', 1, 'RESERVATION_READY');
