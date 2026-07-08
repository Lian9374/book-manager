package com.bookmanager.controller;

import com.bookmanager.dto.ApiResponse;
import com.bookmanager.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/dashboard")
    public ApiResponse<Map<String, Object>> dashboard() {
        return ApiResponse.success(statisticsService.getDashboardSummary());
    }

    @GetMapping("/borrow-trend")
    public ApiResponse<Map<String, Long>> borrowTrend(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(defaultValue = "30") int days) {
        if (start != null && end != null) {
            return ApiResponse.success(statisticsService.getBorrowTrend(start, end));
        }
        return ApiResponse.success(statisticsService.getBorrowTrend(days));
    }

    @GetMapping("/return-trend")
    public ApiResponse<Map<String, Long>> returnTrend(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ApiResponse.success(statisticsService.getReturnTrend(start, end));
    }

    @GetMapping("/monthly-comparison")
    public ApiResponse<Map<String, Long>> monthlyComparison() {
        return ApiResponse.success(statisticsService.getMonthlyBorrowComparison());
    }

    @GetMapping("/category-distribution")
    public ApiResponse<List<Map<String, Object>>> categoryDistribution() {
        return ApiResponse.success(statisticsService.getCategoryDistribution());
    }

    @GetMapping("/book-status-distribution")
    public ApiResponse<Map<String, Long>> bookStatusDistribution() {
        return ApiResponse.success(statisticsService.getBookStatusDistribution());
    }

    @GetMapping("/popular-books")
    public ApiResponse<List<Map<String, Object>>> popularBooks(@RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.success(statisticsService.getPopularBooks(limit));
    }

    @GetMapping("/top-readers")
    public ApiResponse<List<Map<String, Object>>> topReaders(@RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.success(statisticsService.getTopReaders(limit));
    }

    @GetMapping("/overdue-report")
    public ApiResponse<Map<String, Object>> overdueReport() {
        return ApiResponse.success(statisticsService.getOverdueReport());
    }
}
