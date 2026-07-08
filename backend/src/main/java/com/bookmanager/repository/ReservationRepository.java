package com.bookmanager.repository;

import com.bookmanager.entity.Reservation;
import com.bookmanager.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByUserIdOrderByReserveDateDesc(Long userId);

    Optional<Reservation> findByUserIdAndBookIdAndStatus(Long userId, Long bookId, ReservationStatus status);

    long countByBookIdAndStatus(Long bookId, ReservationStatus status);

    @Query("SELECT r FROM Reservation r WHERE r.expireDate < :date AND r.status = 'PENDING'")
    List<Reservation> findExpiredReservations(@Param("date") LocalDate date);

    @Query("SELECT r FROM Reservation r WHERE r.book.id = :bookId AND r.status = 'PENDING' ORDER BY r.reserveDate ASC")
    List<Reservation> findPendingReservationsByBook(@Param("bookId") Long bookId);

    @Query("SELECT r FROM Reservation r WHERE r.status = :status AND r.fulfillDeadline IS NOT NULL AND r.fulfillDeadline < :date")
    List<Reservation> findByStatusAndFulfillDeadlineBefore(
            @Param("status") ReservationStatus status,
            @Param("date") LocalDate date);
}
