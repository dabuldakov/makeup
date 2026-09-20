package com.example.makeup.dto.request;

import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class UpdateUserRequest {
    private String fullName;

    @Email
    private String email;

    private String avatarUrl;

    private Boolean enabled;
}