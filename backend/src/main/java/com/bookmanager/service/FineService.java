package com.bookmanager.service;

import com.bookmanager.entity.Fine;
import com.bookmanager.enums.FineStatus;
import com.bookmanager.exception.BusinessException;
import com.bookmanager.repository.FineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FineService {

    private final FineRepository fineRepository;

    public List<Fine> getMyFines(Long userId) {
        return fineRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public BigDecimal getUnpaidAmount(Long userId) {
        return fineRepository.sumUnpaidFinesByUserId(userId);
    }

    @Transactional
    public Fine payFine(Long fineId) {
        Fine fine = fineRepository.findById(fineId)
                .orElseThrow(() -> new BusinessException("Fine not found"));

        if (fine.getStatus() != FineStatus.UNPAID) {
            throw new BusinessException("This fine is already " + fine.getStatus());
        }

        fine.setStatus(FineStatus.PAID);
        fine.setPaidAt(LocalDateTime.now());
        return fineRepository.save(fine);
    }
}
