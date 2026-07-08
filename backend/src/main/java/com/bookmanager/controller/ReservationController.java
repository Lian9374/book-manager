package com.bookmanager.controller;

import com.bookmanager.dto.ApiResponse;
import com.bookmanager.dto.ReservationRequest;
import com.bookmanager.entity.Reservation;
import com.bookmanager.enums.ReservationStatus;
import com.bookmanager.repository.ReservationRepository;
import com.bookmanager.security.UserPrincipal;
import com.bookmanager.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;
    private final ReservationRepository reservationRepository;

    @PostMapping
    @PreAuthorize("hasRole('READER')")
    public ApiResponse<Reservation> reserveBook(@Valid @RequestBody ReservationRequest request,
                                                  @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success("Reservation created", reservationService.reserveBook(request, principal));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('READER')")
    public ApiResponse<Void> cancelReservation(@PathVariable Long id,
                                                @AuthenticationPrincipal UserPrincipal principal) {
        reservationService.cancelReservation(id, principal);
        return ApiResponse.success("Reservation cancelled", null);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ApiResponse<List<Reservation>> listAllReservations(
            @RequestParam(required = false) ReservationStatus status) {
        if (status != null) {
            return ApiResponse.success(reservationRepository.findAll().stream()
                    .filter(r -> r.getStatus() == status).toList());
        }
        return ApiResponse.success(reservationRepository.findAll());
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('READER')")
    public ApiResponse<List<Reservation>> myReservations(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(reservationService.getMyReservations(principal.getUserId()));
    }

    @GetMapping("/queue-position")
    @PreAuthorize("hasRole('READER')")
    public ApiResponse<Map<String, Object>> queuePosition(@RequestParam Long bookId,
                                                           @AuthenticationPrincipal UserPrincipal principal) {
        long position = reservationService.getQueuePosition(bookId, principal.getUserId());
        long totalInQueue = reservationService.getReservationCount(bookId);
        return ApiResponse.success(Map.of(
                "position", position,
                "totalInQueue", totalInQueue,
                "inQueue", position > 0
        ));
    }

    @GetMapping("/book/{bookId}/count")
    public ApiResponse<Map<String, Long>> reservationCount(@PathVariable Long bookId) {
        return ApiResponse.success(Map.of(
                "pendingCount", reservationService.getReservationCount(bookId)
        ));
    }
}
