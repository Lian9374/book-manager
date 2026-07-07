package com.bookmanager.repository;

import com.bookmanager.entity.BorrowRecord;
import com.bookmanager.enums.BorrowStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BorrowRecordRepository extends JpaRepository<BorrowRecord, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT br FROM BorrowRecord br WHERE br.id = :id")
    Optional<BorrowRecord> findByIdForUpdate(@Param("id") Long id);

    long countByUserIdAndStatusIn(Long userId, List<BorrowStatus> statuses);

    List<BorrowRecord> findByUserIdOrderByBorrowDateDesc(Long userId);

    @Query("SELECT br FROM BorrowRecord br WHERE br.dueDate < :date AND br.status = 'BORROWING'")
    List<BorrowRecord> findOverdueRecords(@Param("date") LocalDate date);

    @Query("SELECT br FROM BorrowRecord br WHERE br.status = 'BORROWING' AND br.dueDate < :date")
    List<BorrowRecord> findBorrowingAndDueBefore(@Param("date") LocalDate date);

    @Query("SELECT br.book.id, COUNT(br) FROM BorrowRecord br GROUP BY br.book.id ORDER BY COUNT(br) DESC")
    List<Object[]> findPopularBooks();

    @Query("SELECT COUNT(br) FROM BorrowRecord br WHERE br.borrowDate BETWEEN :start AND :end")
    long countByBorrowDateBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);
}
