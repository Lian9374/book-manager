package com.bookmanager.controller;

import com.bookmanager.dto.ApiResponse;
import com.bookmanager.dto.ReservationRequest;
import com.bookmanager.entity.Reservation;
import com.bookmanager.repository.ReservationRepository;
import com.bookmanager.security.UserPrincipal;
import com.bookmanager.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ApiResponse<List<Reservation>> listAllReservations() {
        return ApiResponse.success(reservationRepository.findAll());
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('READER')")
    public ApiResponse<List<Reservation>> myReservations(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(reservationService.getMyReservations(principal.getUserId()));
    }
}
