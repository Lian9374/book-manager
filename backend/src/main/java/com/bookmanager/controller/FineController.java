package com.bookmanager.controller;

import com.bookmanager.dto.ApiResponse;
import com.bookmanager.entity.Fine;
import com.bookmanager.enums.FineStatus;
import com.bookmanager.security.UserPrincipal;
import com.bookmanager.service.FineService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/fines")
@RequiredArgsConstructor
public class FineController {

    private final FineService fineService;

    // ── Reader endpoints ──

    @GetMapping("/my")
    @PreAuthorize("hasRole('READER')")
    public ApiResponse<List<Fine>> myFines(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(fineService.getMyFines(principal.getUserId()));
    }

    @GetMapping("/my/unpaid")
    @PreAuthorize("hasRole('READER')")
    public ApiResponse<Map<String, BigDecimal>> myUnpaidAmount(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(Map.of("unpaidAmount", fineService.getUnpaidAmount(principal.getUserId())));
    }

    // ── Admin/Librarian endpoints ──

    @GetMapping
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ApiResponse<List<Fine>> listAllFines(
            @RequestParam(required = false) FineStatus status,
            @RequestParam(required = false) Long userId) {
        return ApiResponse.success(fineService.getAllFines(status, userId));
    }

    @PutMapping("/{id}/pay")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ApiResponse<Fine> payFine(@PathVariable Long id) {
        return ApiResponse.success("Fine paid", fineService.payFine(id));
    }

    @PutMapping("/{id}/waive")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Fine> waiveFine(@PathVariable Long id) {
        return ApiResponse.success("Fine waived", fineService.waiveFine(id));
    }

    @PostMapping("/pay-all")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ApiResponse<Void> payAllFines(@RequestBody Map<String, Long> body) {
        fineService.payAllFines(body.get("userId"));
        return ApiResponse.success("All fines paid", null);
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ApiResponse<Map<String, Object>> fineStats() {
        return ApiResponse.success(fineService.getFineStats());
    }
}
