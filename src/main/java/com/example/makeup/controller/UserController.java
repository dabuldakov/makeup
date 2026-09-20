package com.example.makeup.controller;

import com.example.makeup.dto.request.UpdateUserRequest;
import com.example.makeup.dto.response.UserResponse;
import com.example.makeup.entity.User;
import com.example.makeup.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        return ResponseEntity.ok(toResponseDto(userService.getUserByUsername(authentication.getName())));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request,
            Authentication authentication) {
        User current = userService.getUserByUsername(authentication.getName());
        if (!current.getId().equals(id)) {
            throw new AccessDeniedException("You can only update your own profile");
        }
        return ResponseEntity.ok(toResponseDto(userService.updateUser(id, request)));
    }

    private UserResponse toResponseDto(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .userName(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .enabled(user.isEnabled())
                .build();
    }
}