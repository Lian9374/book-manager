package com.bookmanager.controller;

import com.bookmanager.dto.*;
import com.bookmanager.entity.User;
import com.bookmanager.security.JwtUtils;
import com.bookmanager.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = userService.findByUsername(request.getUsername());

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            return ApiResponse.error(401, "Invalid username or password");
        }

        var roles = user.getRoles().stream()
                .map(role -> role.getRoleName())
                .collect(Collectors.toList());

        String accessToken = jwtUtils.generateAccessToken(user.getId(), user.getUsername(), roles);
        String refreshToken = jwtUtils.generateRefreshToken(user.getId(), user.getUsername());

        AuthResponse response = AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .roles(roles)
                .build();

        return ApiResponse.success(response);
    }

    @PostMapping("/register")
    public ApiResponse<User> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.register(request);
        return ApiResponse.success("Registration successful", user);
    }
}
