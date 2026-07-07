package com.bookmanager.repository;

import com.bookmanager.entity.Fine;
import com.bookmanager.enums.FineStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;

@Repository
public interface FineRepository extends JpaRepository<Fine, Long> {

    List<Fine> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndStatus(Long userId, FineStatus status);

    boolean existsByBorrowRecordId(Long borrowRecordId);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(f.amount), 0) FROM Fine f WHERE f.status = 'UNPAID' AND f.user.id = :userId")
    BigDecimal sumUnpaidFinesByUserId(@org.springframework.data.repository.query.Param("userId") Long userId);
}
