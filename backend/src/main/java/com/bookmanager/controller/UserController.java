package com.bookmanager.controller;

import com.bookmanager.dto.ApiResponse;
import com.bookmanager.entity.User;
import com.bookmanager.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    @GetMapping
    public ApiResponse<List<User>> listUsers() {
        return ApiResponse.success(userService.listAllUsers());
    }

    @PutMapping("/{id}/roles")
    public ApiResponse<User> updateRoles(@PathVariable Long id, @RequestBody Map<String, List<String>> request) {
        return ApiResponse.success(userService.updateUserRoles(id, request.get("roles")));
    }

    @PutMapping("/{id}/toggle-status")
    public ApiResponse<Void> toggleStatus(@PathVariable Long id) {
        userService.toggleUserStatus(id);
        return ApiResponse.success("User status toggled", null);
    }
}
