package com.bookmanager.repository;

import com.bookmanager.entity.Fine;
import com.bookmanager.enums.FineStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;

@Repository
public interface FineRepository extends JpaRepository<Fine, Long> {

    List<Fine> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Fine> findByUserIdAndStatus(Long userId, FineStatus status);

    long countByUserIdAndStatus(Long userId, FineStatus status);

    boolean existsByBorrowRecordId(Long borrowRecordId);

    @Query("SELECT COALESCE(SUM(f.amount), 0) FROM Fine f WHERE f.status = 'UNPAID' AND f.user.id = :userId")
    BigDecimal sumUnpaidFinesByUserId(@Param("userId") Long userId);

    @Query("SELECT f FROM Fine f WHERE " +
           "(:status IS NULL OR f.status = :status) AND " +
           "(:userId IS NULL OR f.user.id = :userId) " +
           "ORDER BY f.createdAt DESC")
    List<Fine> searchFines(@Param("status") FineStatus status,
                           @Param("userId") Long userId);
}
