package com.bookmanager.service;

import com.bookmanager.dto.RegisterRequest;
import com.bookmanager.entity.Role;
import com.bookmanager.entity.User;
import com.bookmanager.enums.UserStatus;
import com.bookmanager.exception.BusinessException;
import com.bookmanager.repository.RoleRepository;
import com.bookmanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("Username already taken");
        }

        Role readerRole = roleRepository.findByRoleName("READER")
                .orElseThrow(() -> new BusinessException("Default READER role not found"));

        User user = User.builder()
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .realName(request.getRealName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .status(UserStatus.ACTIVE)
                .roles(new java.util.HashSet<>(Collections.singletonList(readerRole)))
                .build();

        return userRepository.save(user);
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("User not found"));
    }

    /**
     * Returns Optional to avoid user-enumeration leaks during login.
     */
    public java.util.Optional<User> findByUsernameOptional(String username) {
        return userRepository.findByUsername(username);
    }

    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));

        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new BusinessException("Current password is incorrect");
        }
        if (newPassword.length() < 6) {
            throw new BusinessException("New password must be at least 6 characters");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public List<User> listAllUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public User updateUserRoles(Long userId, List<String> roleNames) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));

        List<Role> roles = roleRepository.findAll().stream()
                .filter(r -> roleNames.contains(r.getRoleName()))
                .toList();

        if (roles.isEmpty()) {
            throw new BusinessException("No valid roles specified");
        }

        user.setRoles(new java.util.HashSet<>(roles));
        return userRepository.save(user);
    }

    @Transactional
    public void toggleUserStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));

        user.setStatus(user.getStatus() == UserStatus.ACTIVE ? UserStatus.DISABLED : UserStatus.ACTIVE);
        userRepository.save(user);
    }
}
