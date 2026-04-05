package com.example.makeup.dto.response;

import com.example.makeup.entity.Role;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponse {
    private String userName;
    private String email;
    private Role role;
}
