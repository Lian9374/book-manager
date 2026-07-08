package com.bookmanager.service;

import com.bookmanager.entity.*;
import com.bookmanager.enums.BookStatus;
import com.bookmanager.enums.BorrowStatus;
import com.bookmanager.enums.ReservationStatus;
import com.bookmanager.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledTaskService {

    private final BorrowRecordRepository borrowRecordRepository;
    private final ReservationRepository reservationRepository;
    private final FineRepository fineRepository;
    private final NotificationRepository notificationRepository;
    private final BookRepository bookRepository;
    private final SystemConfigService configService;

    /**
     * Daily check for overdue borrows: runs at 2:00 AM
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void checkOverdueBorrows() {
        log.info("Running overdue borrow check...");
        LocalDate today = LocalDate.now();
        List<BorrowRecord> overdueRecords = borrowRecordRepository
                .findBorrowingAndDueBefore(today);

        for (BorrowRecord record : overdueRecords) {
            record.setStatus(BorrowStatus.OVERDUE);
            borrowRecordRepository.save(record);

            long overdueDays = ChronoUnit.DAYS.between(record.getDueDate(), today);
            BigDecimal finePerDay = configService.getOverdueFinePerDay();
            BigDecimal amount = finePerDay.multiply(BigDecimal.valueOf(overdueDays));

            if (!fineRepository.existsByBorrowRecordId(record.getId())) {
                fineRepository.save(Fine.builder()
                        .user(record.getUser())
                        .borrowRecord(record)
                        .amount(amount)
                        .reason("Overdue " + overdueDays + " day(s)")
                        .build());

                notificationRepository.save(Notification.builder()
                        .user(record.getUser())
                        .title("Book Overdue")
                        .content("'" + record.getBook().getTitle() + "' is overdue by " + overdueDays
                                + " day(s). Fine: ¥" + amount)
                        .type("OVERDUE")
                        .build());
            }
        }
        log.info("Overdue check complete. {} records updated.", overdueRecords.size());
    }

    /**
     * Daily check for expired reservations: runs at 3:00 AM
     * Handles both wait-expiration and fulfillment-deadline expiration.
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void checkExpiredReservations() {
        log.info("Running reservation expiration check...");
        LocalDate today = LocalDate.now();

        // 1. Expire reservations past their wait-expiry date
        List<Reservation> expiredByDate = reservationRepository.findExpiredReservations(today);
        for (Reservation reservation : expiredByDate) {
            expireReservation(reservation, "Your reservation for '" + reservation.getBook().getTitle() + "' has expired.");
        }

        // 2. Expire reservations past their fulfillment deadline (book was available but user didn't borrow)
        List<Reservation> expiredFulfillment = reservationRepository
                .findByStatusAndFulfillDeadlineBefore(ReservationStatus.PENDING, today);
        for (Reservation reservation : expiredFulfillment) {
            if (reservation.getFulfillDeadline() != null) {
                expireReservation(reservation, "You did not borrow '" + reservation.getBook().getTitle()
                        + "' by the deadline. Reservation released.");
            }
        }

        log.info("Reservation expiration complete. {} expired by date, {} by fulfillment deadline.",
                expiredByDate.size(), expiredFulfillment.size());
    }

    private void expireReservation(Reservation reservation, String message) {
        reservation.setStatus(ReservationStatus.EXPIRED);
        reservationRepository.save(reservation);

        notificationRepository.save(Notification.builder()
                .user(reservation.getUser())
                .title("Reservation Expired")
                .content(message)
                .type("SYSTEM")
                .build());

        // Revert book status if no more pending reservations
        long pendingCount = reservationRepository.countByBookIdAndStatus(
                reservation.getBook().getId(), ReservationStatus.PENDING);
        if (pendingCount == 0) {
            Book book = reservation.getBook();
            if (book.getStatus() == BookStatus.RESERVED) {
                book.setStatus(BookStatus.BORROWED);
                bookRepository.save(book);
            }
        }
    }
}
