package com.bookmanager.controller;

import com.bookmanager.dto.ApiResponse;
import com.bookmanager.entity.Fine;
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

    @PutMapping("/{id}/pay")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ApiResponse<Fine> payFine(@PathVariable Long id) {
        return ApiResponse.success("Fine paid", fineService.payFine(id));
    }
}
