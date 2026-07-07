package com.bookmanager.controller;

import com.bookmanager.dto.ApiResponse;
import com.bookmanager.dto.BorrowRequest;
import com.bookmanager.entity.BorrowRecord;
import com.bookmanager.repository.BorrowRecordRepository;
import com.bookmanager.security.UserPrincipal;
import com.bookmanager.service.BorrowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/borrows")
@RequiredArgsConstructor
public class BorrowController {

    private final BorrowService borrowService;
    private final BorrowRecordRepository borrowRecordRepository;

    @PostMapping
    @PreAuthorize("hasRole('READER')")
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
    @PreAuthorize("hasRole('READER')")
    public ApiResponse<BorrowRecord> renewBook(@PathVariable Long id,
                                                @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success("Book renewed successfully", borrowService.renewBook(id, principal));
    }

    @GetMapping
    public ApiResponse<List<BorrowRecord>> listAllBorrows() {
        return ApiResponse.success(borrowRecordRepository.findAll());
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('READER')")
    public ApiResponse<List<BorrowRecord>> myBorrows(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(borrowRecordRepository.findByUserIdOrderByBorrowDateDesc(principal.getUserId()));
    }
}
