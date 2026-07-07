package com.bookmanager.controller;

import com.bookmanager.dto.ApiResponse;
import com.bookmanager.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ConfigController {

    private final SystemConfigService configService;

    @GetMapping
    public ApiResponse<Map<String, String>> getAllConfigs() {
        return ApiResponse.success(configService.getAllConfigs());
    }

    @PutMapping
    public ApiResponse<Map<String, String>> updateConfigs(@RequestBody Map<String, String> updates) {
        return ApiResponse.success("Config updated", configService.updateConfigs(updates));
    }
}
