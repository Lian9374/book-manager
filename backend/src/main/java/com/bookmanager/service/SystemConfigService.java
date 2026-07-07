package com.bookmanager.service;

import com.bookmanager.entity.SystemConfig;
import com.bookmanager.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SystemConfigService {

    private final SystemConfigRepository configRepository;

    public String getConfigValue(String key, String defaultValue) {
        return configRepository.findByConfigKey(key)
                .map(SystemConfig::getConfigValue)
                .orElse(defaultValue);
    }

    public int getIntConfig(String key, int defaultValue) {
        try {
            return Integer.parseInt(getConfigValue(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public Map<String, String> getAllConfigs() {
        return configRepository.findAll().stream()
                .collect(Collectors.toMap(SystemConfig::getConfigKey, SystemConfig::getConfigValue));
    }

    @Transactional
    public Map<String, String> updateConfigs(Map<String, String> updates) {
        for (Map.Entry<String, String> entry : updates.entrySet()) {
            SystemConfig config = configRepository.findByConfigKey(entry.getKey())
                    .orElseThrow(() -> new com.bookmanager.exception.BusinessException(
                            "Config key not found: " + entry.getKey()));
            config.setConfigValue(entry.getValue());
            configRepository.save(config);
        }
        return getAllConfigs();
    }

    public int getMaxBorrowCount() {
        return getIntConfig("MAX_BORROW_COUNT", 5);
    }

    public int getBorrowDurationDays() {
        return getIntConfig("BORROW_DURATION_DAYS", 30);
    }

    public int getMaxRenewCount() {
        return getIntConfig("MAX_RENEW_COUNT", 2);
    }

    public java.math.BigDecimal getOverdueFinePerDay() {
        String val = getConfigValue("OVERDUE_FINE_PER_DAY", "0.50");
        return new java.math.BigDecimal(val);
    }

    public int getReservationExpireDays() {
        return getIntConfig("RESERVATION_EXPIRE_DAYS", 7);
    }

    public int getReservationFulfillDays() {
        return getIntConfig("RESERVATION_FULFILL_DAYS", 3);
    }
}
