package com.bookmanager.controller;

import com.bookmanager.dto.ApiResponse;
import com.bookmanager.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/dashboard")
    public ApiResponse<Map<String, Object>> dashboard() {
        return ApiResponse.success(statisticsService.getDashboardSummary());
    }

    @GetMapping("/borrow-trend")
    public ApiResponse<Map<String, Long>> borrowTrend(@RequestParam(defaultValue = "30") int days) {
        return ApiResponse.success(statisticsService.getBorrowTrend(days));
    }

    @GetMapping("/popular-books")
    public ApiResponse<List<Map<String, Object>>> popularBooks(@RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.success(statisticsService.getPopularBooks(limit));
    }

    @GetMapping("/overdue-report")
    public ApiResponse<Map<String, Object>> overdueReport() {
        return ApiResponse.success(statisticsService.getOverdueReport());
    }
}
