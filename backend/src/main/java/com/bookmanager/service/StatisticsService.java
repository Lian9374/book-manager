package com.bookmanager.service;

import com.bookmanager.entity.Book;
import com.bookmanager.entity.BorrowRecord;
import com.bookmanager.entity.Fine;
import com.bookmanager.enums.BorrowStatus;
import com.bookmanager.enums.BookStatus;
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
    private final com.bookmanager.repository.UserRepository userRepository;

    /**
     * Borrowing trend: count per day for the last N days
     */
    public Map<String, Long> getBorrowTrend(int days) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days);
        List<BorrowRecord> allRecords = borrowRecordRepository.findAll();
        Map<LocalDate, Long> byDate = allRecords.stream()
                .filter(r -> !r.getBorrowDate().isBefore(start) && !r.getBorrowDate().isAfter(end))
                .collect(Collectors.groupingBy(BorrowRecord::getBorrowDate, Collectors.counting()));

        Map<String, Long> result = new LinkedHashMap<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            result.put(d.toString(), byDate.getOrDefault(d, 0L));
        }
        return result;
    }

    /**
     * Popular books ranking
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
     * Overdue report summary
     */
    public Map<String, Object> getOverdueReport() {
        long overdueCount = borrowRecordRepository.findAll().stream()
                .filter(r -> r.getStatus() == BorrowStatus.OVERDUE)
                .count();
        long totalBorrows = borrowRecordRepository.count();
        double overdueRate = totalBorrows > 0 ? (double) overdueCount / totalBorrows * 100 : 0;

        BigDecimal totalUnpaidFines = fineRepository.findAll().stream()
                .filter(f -> f.getStatus() == com.bookmanager.enums.FineStatus.UNPAID)
                .map(Fine::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("overdueCount", overdueCount);
        result.put("totalBorrows", totalBorrows);
        result.put("overdueRate", Math.round(overdueRate * 100.0) / 100.0);
        result.put("totalUnpaidFines", totalUnpaidFines);
        return result;
    }

    /**
     * Dashboard summary stats
     */
    public Map<String, Object> getDashboardSummary() {
        long totalBooks = bookRepository.count();
        long availableBooks = bookRepository.countByStatus(BookStatus.AVAILABLE);
        long activeBorrows = borrowRecordRepository.findAll().stream()
                .filter(r -> r.getStatus() == BorrowStatus.BORROWING
                        || r.getStatus() == BorrowStatus.RENEWED
                        || r.getStatus() == BorrowStatus.OVERDUE)
                .count();
        long totalUsers = userRepository.count();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalBooks", totalBooks);
        result.put("availableBooks", availableBooks);
        result.put("activeBorrows", activeBorrows);
        result.put("totalUsers", totalUsers);
        return result;
    }
}
