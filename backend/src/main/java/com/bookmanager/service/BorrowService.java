package com.bookmanager.service;

import com.bookmanager.dto.BorrowRequest;
import com.bookmanager.entity.*;
import com.bookmanager.enums.BookStatus;
import com.bookmanager.enums.BorrowStatus;
import com.bookmanager.enums.InventoryOperation;
import com.bookmanager.enums.ReservationStatus;
import com.bookmanager.exception.BusinessException;
import com.bookmanager.repository.*;
import com.bookmanager.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BorrowService {

    private final BookRepository bookRepository;
    private final BorrowRecordRepository borrowRecordRepository;
    private final ReservationRepository reservationRepository;
    private final FineRepository fineRepository;
    private final InventoryLogRepository inventoryLogRepository;
    private final NotificationRepository notificationRepository;
    private final SystemConfigService configService;

    public int getMaxBorrowCount() {
        return configService.getMaxBorrowCount();
    }

    @Transactional
    public BorrowRecord borrowBook(BorrowRequest request, UserPrincipal principal) {
        Long userId = principal.getUserId();
        int maxBorrowCount = configService.getMaxBorrowCount();
        int borrowDurationDays = configService.getBorrowDurationDays();

        // Check max borrow limit
        long currentBorrows = borrowRecordRepository.countByUserIdAndStatusIn(
                userId, List.of(BorrowStatus.BORROWING, BorrowStatus.OVERDUE, BorrowStatus.RENEWED));
        if (currentBorrows >= maxBorrowCount) {
            throw new BusinessException("You have reached the maximum borrowing limit (" + maxBorrowCount + ")");
        }

        // Check unpaid fines
        BigDecimal unpaidFines = fineRepository.sumUnpaidFinesByUserId(userId);
        if (unpaidFines.compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessException("You have unpaid fines of ¥" + unpaidFines + ". Please pay before borrowing.");
        }

        // Lock the book row for update (prevents concurrent over-borrowing)
        Book book = bookRepository.findByIdForUpdate(request.getBookId())
                .orElseThrow(() -> new BusinessException("Book not found"));

        if (book.getAvailableCopies() <= 0) {
            throw new BusinessException("No available copies of this book");
        }
        if (book.getStatus() != BookStatus.AVAILABLE && book.getStatus() != BookStatus.RESERVED) {
            throw new BusinessException("Book is not available for borrowing (status: " + book.getStatus() + ")");
        }

        // Update book inventory
        int beforeAvailable = book.getAvailableCopies();
        book.setAvailableCopies(beforeAvailable - 1);
        if (book.getAvailableCopies() == 0) {
            book.setStatus(BookStatus.BORROWED);
        }
        bookRepository.save(book);

        // Create borrow record
        LocalDate now = LocalDate.now();
        BorrowRecord record = BorrowRecord.builder()
                .user(new User() {{ setId(userId); }})
                .book(book)
                .borrowDate(now)
                .dueDate(now.plusDays(borrowDurationDays))
                .status(BorrowStatus.BORROWING)
                .renewedCount(0)
                .build();
        record = borrowRecordRepository.save(record);

        // Log inventory
        inventoryLogRepository.save(InventoryLog.builder()
                .book(book)
                .operation(InventoryOperation.OUTBOUND)
                .quantityChange(-1)
                .totalBefore(beforeAvailable)
                .totalAfter(book.getAvailableCopies())
                .remark("Borrowed by user #" + userId)
                .build());

        // Fulfill reservation if this user had reserved this book
        reservationRepository.findByUserIdAndBookIdAndStatus(userId, book.getId(), ReservationStatus.PENDING)
                .ifPresent(reservation -> {
                    reservation.setStatus(ReservationStatus.FULFILLED);
                    reservationRepository.save(reservation);
                });

        return record;
    }

    @Transactional
    public BorrowRecord returnBook(Long borrowRecordId, UserPrincipal principal) {
        // Lock the borrow record for update
        BorrowRecord record = borrowRecordRepository.findByIdForUpdate(borrowRecordId)
                .orElseThrow(() -> new BusinessException("Borrow record not found"));

        if (record.getStatus() == BorrowStatus.RETURNED || record.getStatus() == BorrowStatus.LOST) {
            throw new BusinessException("This record is already " + record.getStatus());
        }

        LocalDate now = LocalDate.now();

        // Update borrow record
        record.setReturnDate(now);
        record.setStatus(BorrowStatus.RETURNED);
        borrowRecordRepository.save(record);

        // Update book inventory
        Book book = record.getBook();
        int beforeAvailable = book.getAvailableCopies();
        book.setAvailableCopies(beforeAvailable + 1);
        book.setStatus(BookStatus.AVAILABLE);
        bookRepository.save(book);

        // Log inventory
        inventoryLogRepository.save(InventoryLog.builder()
                .book(book)
                .operation(InventoryOperation.INBOUND)
                .quantityChange(1)
                .totalBefore(beforeAvailable)
                .totalAfter(book.getAvailableCopies())
                .remark("Returned by user #" + record.getUser().getId())
                .build());

        // Calculate fine if overdue
        if (now.isAfter(record.getDueDate())) {
            long overdueDays = ChronoUnit.DAYS.between(record.getDueDate(), now);
            BigDecimal finePerDay = configService.getOverdueFinePerDay();
            BigDecimal amount = finePerDay.multiply(BigDecimal.valueOf(overdueDays));

            fineRepository.save(Fine.builder()
                    .user(record.getUser())
                    .borrowRecord(record)
                    .amount(amount)
                    .reason("Overdue " + overdueDays + " day(s)")
                    .build());

            notificationRepository.save(Notification.builder()
                    .user(record.getUser())
                    .title("Overdue Fine")
                    .content("You have been charged ¥" + amount + " for overdue return of '" + book.getTitle() + "'")
                    .type("FINE")
                    .build());
        }

        // Notify next user in reservation queue
        List<Reservation> pendingReservations = reservationRepository
                .findPendingReservationsByBook(book.getId());
        if (!pendingReservations.isEmpty()) {
            Reservation nextReservation = pendingReservations.get(0);
            int fulfillDays = configService.getReservationFulfillDays();
            nextReservation.setFulfillDeadline(now.plusDays(fulfillDays));
            reservationRepository.save(nextReservation);

            notificationRepository.save(Notification.builder()
                    .user(nextReservation.getUser())
                    .title("Reservation Ready")
                    .content("'" + book.getTitle() + "' is now available. Please borrow it by " + nextReservation.getFulfillDeadline())
                    .type("RESERVATION_READY")
                    .build());
        }

        return record;
    }

    @Transactional
    public BorrowRecord renewBook(Long borrowRecordId, UserPrincipal principal) {
        BorrowRecord record = borrowRecordRepository.findById(borrowRecordId)
                .orElseThrow(() -> new BusinessException("Borrow record not found"));

        if (!record.getUser().getId().equals(principal.getUserId())) {
            throw new BusinessException("You can only renew your own borrows");
        }
        if (record.getStatus() != BorrowStatus.BORROWING && record.getStatus() != BorrowStatus.RENEWED) {
            throw new BusinessException("Only active borrows can be renewed");
        }

        int maxRenew = configService.getMaxRenewCount();
        if (record.getRenewedCount() >= maxRenew) {
            throw new BusinessException("Maximum renewal count reached (" + maxRenew + ")");
        }

        // Check if there are pending reservations for this book
        long pendingCount = reservationRepository.countByBookIdAndStatus(record.getBook().getId(), ReservationStatus.PENDING);
        if (pendingCount > 0) {
            throw new BusinessException("Cannot renew — there are pending reservations for this book");
        }

        int borrowDurationDays = configService.getBorrowDurationDays();
        record.setDueDate(LocalDate.now().plusDays(borrowDurationDays));
        record.setRenewedCount(record.getRenewedCount() + 1);
        record.setStatus(BorrowStatus.RENEWED);
        return borrowRecordRepository.save(record);
    }
}
