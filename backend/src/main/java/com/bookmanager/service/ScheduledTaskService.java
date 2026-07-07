package com.bookmanager.service;

import com.bookmanager.entity.*;
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

            // Check if fine already exists for this record (to avoid duplicates)
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
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void checkExpiredReservations() {
        log.info("Running reservation expiration check...");
        LocalDate today = LocalDate.now();
        List<Reservation> expiredReservations = reservationRepository
                .findExpiredReservations(today);

        for (Reservation reservation : expiredReservations) {
            reservation.setStatus(ReservationStatus.EXPIRED);
            reservationRepository.save(reservation);

            notificationRepository.save(Notification.builder()
                    .user(reservation.getUser())
                    .title("Reservation Expired")
                    .content("Your reservation for '" + reservation.getBook().getTitle() + "' has expired.")
                    .type("SYSTEM")
                    .build());
        }
        log.info("Reservation expiration check complete. {} expired.", expiredReservations.size());
    }
}
