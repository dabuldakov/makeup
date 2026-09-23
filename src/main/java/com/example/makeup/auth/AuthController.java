package com.example.makeup.auth;

import com.example.makeup.security.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("User registration attempt: {}", request.getUsername());
        User user = userService.register(request);
        String token = jwtService.generateToken(user);
        log.info("User registered: {}", user.getUsername());
        return ResponseEntity.ok(new AuthResponse(token, user.getUsername(), user.getRole().name()));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("User login attempt: {}", request.getUsername());
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        User user = userService.getUserByUsername(request.getUsername());
        String token = jwtService.generateToken(user);
        log.info("User logged in: {}", user.getUsername());
        return ResponseEntity.ok(new AuthResponse(token, user.getUsername(), user.getRole().name()));
    }
}