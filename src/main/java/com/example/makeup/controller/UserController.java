package com.example.makeup.controller;

import com.example.makeup.dto.response.UserResponse;
import com.example.makeup.entity.User;
import com.example.makeup.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<Object> updateUser(@PathVariable Long id, @RequestBody User user) {
        userService.updateUser(id, user);
        return ResponseEntity.ok().build();
    }

    private UserResponse toResponseDto(User user) {
        return UserResponse.builder()
                .userName(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}