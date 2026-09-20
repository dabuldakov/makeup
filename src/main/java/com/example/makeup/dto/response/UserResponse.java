package com.example.makeup.dto.response;

import com.example.makeup.entity.Role;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserResponse {
    private Long id;
    private String userName;
    private String email;
    private String fullName;
    private String avatarUrl;
    private Role role;
    private LocalDateTime createdAt;
    private boolean enabled;
}