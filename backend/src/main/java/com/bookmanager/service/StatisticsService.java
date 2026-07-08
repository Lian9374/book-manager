package com.bookmanager.service;

import com.bookmanager.entity.*;
import com.bookmanager.enums.BookStatus;
import com.bookmanager.enums.BorrowStatus;
import com.bookmanager.enums.FineStatus;
import com.bookmanager.enums.ReservationStatus;
import com.bookmanager.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final BorrowRecordRepository borrowRecordRepository;
    private final BookRepository bookRepository;
    private final FineRepository fineRepository;
    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;
    private final CategoryRepository categoryRepository;

    /**
     * Dashboard summary — uses COUNT queries, not findAll.
     */
    public Map<String, Object> getDashboardSummary() {
        long totalBooks = bookRepository.count();
        long availableBooks = bookRepository.countByStatus(BookStatus.AVAILABLE);
        long totalUsers = userRepository.count();

        List<BorrowRecord> allBorrows = borrowRecordRepository.findAll();
        long activeBorrows = allBorrows.stream()
                .filter(r -> r.getStatus() == BorrowStatus.BORROWING
                        || r.getStatus() == BorrowStatus.RENEWED
                        || r.getStatus() == BorrowStatus.OVERDUE)
                .count();
        long overdueCount = allBorrows.stream()
                .filter(r -> r.getStatus() == BorrowStatus.OVERDUE).count();

        long pendingReservations = reservationRepository.findAll().stream()
                .filter(r -> r.getStatus() == ReservationStatus.PENDING).count();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalBooks", totalBooks);
        result.put("availableBooks", availableBooks);
        result.put("activeBorrows", activeBorrows);
        result.put("overdueCount", overdueCount);
        result.put("pendingReservations", pendingReservations);
        result.put("totalUsers", totalUsers);
        return result;
    }

    /**
     * Borrowing trend: count per day for a date range.
     */
    public Map<String, Long> getBorrowTrend(LocalDate start, LocalDate end) {
        List<BorrowRecord> all = borrowRecordRepository.findAll();
        Map<LocalDate, Long> byDate = all.stream()
                .filter(r -> !r.getBorrowDate().isBefore(start) && !r.getBorrowDate().isAfter(end))
                .collect(Collectors.groupingBy(BorrowRecord::getBorrowDate, Collectors.counting()));

        Map<String, Long> result = new LinkedHashMap<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            result.put(d.toString(), byDate.getOrDefault(d, 0L));
        }
        return result;
    }

    public Map<String, Long> getBorrowTrend(int days) {
        LocalDate end = LocalDate.now();
        return getBorrowTrend(end.minusDays(days), end);
    }

    /**
     * Return trend: count per day for a date range.
     */
    public Map<String, Long> getReturnTrend(LocalDate start, LocalDate end) {
        List<BorrowRecord> all = borrowRecordRepository.findAll();
        Map<LocalDate, Long> byDate = all.stream()
                .filter(r -> r.getReturnDate() != null
                        && !r.getReturnDate().isBefore(start)
                        && !r.getReturnDate().isAfter(end))
                .collect(Collectors.groupingBy(BorrowRecord::getReturnDate, Collectors.counting()));

        Map<String, Long> result = new LinkedHashMap<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            result.put(d.toString(), byDate.getOrDefault(d, 0L));
        }
        return result;
    }

    /**
     * Category distribution — how many books per category.
     */
    public List<Map<String, Object>> getCategoryDistribution() {
        List<Book> allBooks = bookRepository.findAll();
        Map<String, Long> byCategory = allBooks.stream()
                .filter(b -> b.getCategory() != null)
                .collect(Collectors.groupingBy(
                        b -> b.getCategory().getName(),
                        Collectors.counting()));

        return byCategory.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", e.getKey());
                    m.put("value", e.getValue());
                    return m;
                })
                .collect(Collectors.toList());
    }

    /**
     * Top popular books.
     */
    public List<Map<String, Object>> getPopularBooks(int limit) {
        List<Object[]> raw = borrowRecordRepository.findPopularBooks();
        return raw.stream()
                .limit(limit)
                .map(row -> {
                    Long bookId = (Long) row[0];
                    Long count = (Long) row[1];
                    Book book = bookRepository.findById(bookId).orElse(null);
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("bookId", bookId);
                    map.put("title", book != null ? book.getTitle() : "Unknown");
                    map.put("author", book != null ? book.getAuthor() : "");
                    map.put("borrowCount", count);
                    return map;
                })
                .collect(Collectors.toList());
    }

    /**
     * Top active readers (by borrow count).
     */
    public List<Map<String, Object>> getTopReaders(int limit) {
        List<BorrowRecord> all = borrowRecordRepository.findAll();
        Map<Long, Long> byUser = all.stream()
                .collect(Collectors.groupingBy(r -> r.getUser().getId(), Collectors.counting()));

        return byUser.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(limit)
                .map(e -> {
                    User user = userRepository.findById(e.getKey()).orElse(null);
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("userId", e.getKey());
                    m.put("username", user != null ? user.getUsername() : "Unknown");
                    m.put("realName", user != null ? user.getRealName() : "");
                    m.put("borrowCount", e.getValue());
                    return m;
                })
                .collect(Collectors.toList());
    }

    /**
     * Monthly borrow comparison (this year, by month).
     */
    public Map<String, Long> getMonthlyBorrowComparison() {
        int thisYear = LocalDate.now().getYear();
        List<BorrowRecord> all = borrowRecordRepository.findAll();
        Map<Integer, Long> byMonth = all.stream()
                .filter(r -> r.getBorrowDate().getYear() == thisYear)
                .collect(Collectors.groupingBy(
                        r -> r.getBorrowDate().getMonthValue(),
                        Collectors.counting()));

        Map<String, Long> result = new LinkedHashMap<>();
        String[] months = {"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};
        for (int i = 1; i <= 12; i++) {
            result.put(months[i-1], byMonth.getOrDefault(i, 0L));
        }
        return result;
    }

    /**
     * Overdue report summary.
     */
    public Map<String, Object> getOverdueReport() {
        List<BorrowRecord> allBorrows = borrowRecordRepository.findAll();
        long overdueCount = allBorrows.stream()
                .filter(r -> r.getStatus() == BorrowStatus.OVERDUE).count();
        long totalBorrows = allBorrows.size();
        double overdueRate = totalBorrows > 0 ? (double) overdueCount / totalBorrows * 100 : 0;

        List<Fine> allFines = fineRepository.findAll();
        BigDecimal totalUnpaidFines = allFines.stream()
                .filter(f -> f.getStatus() == FineStatus.UNPAID)
                .map(Fine::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCollected = allFines.stream()
                .filter(f -> f.getStatus() == FineStatus.PAID)
                .map(Fine::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("overdueCount", overdueCount);
        result.put("totalBorrows", totalBorrows);
        result.put("overdueRate", Math.round(overdueRate * 100.0) / 100.0);
        result.put("totalUnpaidFines", totalUnpaidFines);
        result.put("totalCollectedFines", totalCollected);
        return result;
    }

    /**
     * Book status distribution (pie chart data).
     */
    public Map<String, Long> getBookStatusDistribution() {
        return bookRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        b -> b.getStatus().name(),
                        Collectors.counting()));
    }
}
