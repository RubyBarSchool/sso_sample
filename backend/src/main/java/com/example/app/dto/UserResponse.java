package com.example.app.dto;

import com.example.app.entity.Provider;

import java.time.LocalDateTime;
import java.util.Set;

public record UserResponse(
    Long id,
    String email,
    String username,
    Provider provider,
    Boolean enabled,
    LocalDateTime createdAt,
    Set<String> roles
) {}

