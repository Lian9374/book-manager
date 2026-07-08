package com.bookmanager.controller;

import com.bookmanager.dto.ApiResponse;
import com.bookmanager.dto.BorrowRequest;
import com.bookmanager.entity.BorrowRecord;
import com.bookmanager.enums.BorrowStatus;
import com.bookmanager.exception.BusinessException;
import com.bookmanager.repository.BorrowRecordRepository;
import com.bookmanager.security.UserPrincipal;
import com.bookmanager.service.BorrowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/borrows")
@RequiredArgsConstructor
public class BorrowController {

    private final BorrowService borrowService;
    private final BorrowRecordRepository borrowRecordRepository;

    @PostMapping
    @PreAuthorize("hasAnyRole('READER', 'LIBRARIAN', 'ADMIN')")
    public ApiResponse<BorrowRecord> borrowBook(@Valid @RequestBody BorrowRequest request,
                                                  @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success("Book borrowed successfully", borrowService.borrowBook(request, principal));
    }

    @PutMapping("/{id}/return")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ApiResponse<BorrowRecord> returnBook(@PathVariable Long id,
                                                  @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success("Book returned successfully", borrowService.returnBook(id, principal));
    }

    @PutMapping("/{id}/renew")
    @PreAuthorize("hasAnyRole('READER', 'LIBRARIAN', 'ADMIN')")
    public ApiResponse<BorrowRecord> renewBook(@PathVariable Long id,
                                                @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success("Book renewed successfully", borrowService.renewBook(id, principal));
    }

    @PutMapping("/{id}/report-lost")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ApiResponse<BorrowRecord> reportLost(@PathVariable Long id) {
        BorrowRecord record = borrowRecordRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Borrow record not found"));
        if (record.getStatus() == BorrowStatus.RETURNED || record.getStatus() == BorrowStatus.LOST) {
            throw new BusinessException("Record is already " + record.getStatus());
        }
        record.setStatus(BorrowStatus.LOST);
        return ApiResponse.success("Marked as lost", borrowRecordRepository.save(record));
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ApiResponse<List<BorrowRecord>> listAllBorrows(
            @RequestParam(required = false) BorrowStatus status,
            @RequestParam(required = false) Long userId) {
        return ApiResponse.success(borrowRecordRepository.searchBorrows(status, userId));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('READER', 'LIBRARIAN', 'ADMIN')")
    @Transactional(readOnly = true)
    public ApiResponse<List<BorrowRecord>> myBorrows(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(borrowRecordRepository.findByUserIdOrderByBorrowDateDesc(principal.getUserId()));
    }

    @GetMapping("/my/active-count")
    @PreAuthorize("hasAnyRole('READER', 'LIBRARIAN', 'ADMIN')")
    public ApiResponse<Map<String, Long>> myActiveBorrowCount(@AuthenticationPrincipal UserPrincipal principal) {
        long count = borrowRecordRepository.countByUserIdAndStatusIn(
                principal.getUserId(),
                List.of(BorrowStatus.BORROWING, BorrowStatus.RENEWED, BorrowStatus.OVERDUE));
        return ApiResponse.success(Map.of("activeCount", count, "maxAllowed",
                (long) borrowService.getMaxBorrowCount()));
    }
}
