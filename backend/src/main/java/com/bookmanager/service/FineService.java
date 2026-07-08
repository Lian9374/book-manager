package com.bookmanager.service;

import com.bookmanager.entity.Fine;
import com.bookmanager.enums.FineStatus;
import com.bookmanager.exception.BusinessException;
import com.bookmanager.repository.FineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    public List<Fine> getAllFines(FineStatus status, Long userId) {
        return fineRepository.searchFines(status, userId);
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

    @Transactional
    public Fine waiveFine(Long fineId) {
        Fine fine = fineRepository.findById(fineId)
                .orElseThrow(() -> new BusinessException("Fine not found"));

        if (fine.getStatus() != FineStatus.UNPAID) {
            throw new BusinessException("This fine is already " + fine.getStatus());
        }

        fine.setStatus(FineStatus.WAIVED);
        return fineRepository.save(fine);
    }

    @Transactional
    public void payAllFines(Long userId) {
        List<Fine> unpaidFines = fineRepository.findByUserIdAndStatus(userId, FineStatus.UNPAID);
        for (Fine fine : unpaidFines) {
            fine.setStatus(FineStatus.PAID);
            fine.setPaidAt(LocalDateTime.now());
        }
        fineRepository.saveAll(unpaidFines);
    }

    /**
     * Summary statistics for fines
     */
    public Map<String, Object> getFineStats() {
        List<Fine> all = fineRepository.findAll();

        long unpaidCount = all.stream().filter(f -> f.getStatus() == FineStatus.UNPAID).count();
        long paidCount = all.stream().filter(f -> f.getStatus() == FineStatus.PAID).count();
        long waivedCount = all.stream().filter(f -> f.getStatus() == FineStatus.WAIVED).count();

        BigDecimal totalUnpaid = all.stream()
                .filter(f -> f.getStatus() == FineStatus.UNPAID)
                .map(Fine::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCollected = all.stream()
                .filter(f -> f.getStatus() == FineStatus.PAID)
                .map(Fine::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("unpaidCount", unpaidCount);
        stats.put("paidCount", paidCount);
        stats.put("waivedCount", waivedCount);
        stats.put("totalUnpaid", totalUnpaid);
        stats.put("totalCollected", totalCollected);
        return stats;
    }
}
