package com.example.makeup.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserRequest {
    private String fullName;

    @Email
    private String email;

    @Size(max = 500)
    private String avatarUrl;
}