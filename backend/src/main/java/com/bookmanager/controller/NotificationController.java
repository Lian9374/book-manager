package com.bookmanager.controller;

import com.bookmanager.dto.ApiResponse;
import com.bookmanager.entity.Notification;
import com.bookmanager.repository.NotificationRepository;
import com.bookmanager.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;

    @GetMapping
    public ApiResponse<List<Notification>> myNotifications(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(
                notificationRepository.findByUserIdOrderByCreatedAtDesc(principal.getUserId()));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Long>> unreadCount(@AuthenticationPrincipal UserPrincipal principal) {
        long count = notificationRepository.countByUserIdAndIsReadFalse(principal.getUserId());
        return ApiResponse.success(Map.of("unreadCount", count));
    }

    @PutMapping("/{id}/read")
    @Transactional
    public ApiResponse<Void> markAsRead(@PathVariable Long id) {
        notificationRepository.findById(id).ifPresent(n -> {
            n.setIsRead(true);
            notificationRepository.save(n);
        });
        return ApiResponse.success(null);
    }

    @PutMapping("/read-all")
    @Transactional
    public ApiResponse<Void> markAllAsRead(@AuthenticationPrincipal UserPrincipal principal) {
        notificationRepository.markAllAsRead(principal.getUserId());
        return ApiResponse.success(null);
    }
}
