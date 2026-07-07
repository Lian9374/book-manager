package com.bookmanager.service;

import com.bookmanager.dto.ReservationRequest;
import com.bookmanager.entity.*;
import com.bookmanager.enums.BookStatus;
import com.bookmanager.enums.ReservationStatus;
import com.bookmanager.exception.BusinessException;
import com.bookmanager.repository.*;
import com.bookmanager.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final BookRepository bookRepository;
    private final NotificationRepository notificationRepository;
    private final SystemConfigService configService;

    @Transactional
    public Reservation reserveBook(ReservationRequest request, UserPrincipal principal) {
        Long userId = principal.getUserId();
        int expireDays = configService.getReservationExpireDays();

        Book book = bookRepository.findById(request.getBookId())
                .orElseThrow(() -> new BusinessException("Book not found"));

        // Can reserve if book has no available copies (all borrowed or reserved)
        if (book.getAvailableCopies() > 0) {
            throw new BusinessException("This book has available copies — borrow directly instead of reserving");
        }

        // Check duplicate reservation
        if (reservationRepository.findByUserIdAndBookIdAndStatus(userId, book.getId(), ReservationStatus.PENDING).isPresent()) {
            throw new BusinessException("You already have a pending reservation for this book");
        }

        LocalDate now = LocalDate.now();
        Reservation reservation = Reservation.builder()
                .user(new User() {{ setId(userId); }})
                .book(book)
                .reserveDate(now)
                .expireDate(now.plusDays(expireDays))
                .status(ReservationStatus.PENDING)
                .build();

        reservation = reservationRepository.save(reservation);

        // Update book status
        if (book.getStatus() == BookStatus.BORROWED) {
            book.setStatus(BookStatus.RESERVED);
            bookRepository.save(book);
        }

        return reservation;
    }

    @Transactional
    public void cancelReservation(Long reservationId, UserPrincipal principal) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new BusinessException("Reservation not found"));

        if (!reservation.getUser().getId().equals(principal.getUserId())) {
            throw new BusinessException("You can only cancel your own reservations");
        }
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new BusinessException("Only pending reservations can be cancelled");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);
    }

    public List<Reservation> getMyReservations(Long userId) {
        return reservationRepository.findByUserIdOrderByReserveDateDesc(userId);
    }
}
