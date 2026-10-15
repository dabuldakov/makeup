package com.example.makeup.auth;

import com.example.makeup.auth.JwtService;
import jakarta.servlet.http.HttpServletRequest;
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
    private final LoginRateLimiter loginRateLimiter;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {
        log.info("User registration attempt: {}", request.getUsername());
        loginRateLimiter.checkAndRecord("register:" + clientIp(httpRequest));

        User user = userService.register(request);
        String token = jwtService.generateToken(user);
        log.info("User registered: {}", user.getUsername());
        return ResponseEntity.ok(new AuthResponse(token, user.getUsername(), user.getRole().name()));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        log.info("User login attempt: {}", request.getUsername());

        // Перебор пароля конкретного аккаунта: считаем неудачные попытки,
        // успешный вход сбрасывает счётчик.
        String rateKey = LoginRateLimiter.loginKey(request.getUsername());
        loginRateLimiter.check(rateKey);
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
        } catch (org.springframework.security.core.AuthenticationException e) {
            loginRateLimiter.record(rateKey);
            throw e;
        }
        loginRateLimiter.reset(rateKey);

        User user = userService.getUserByUsername(request.getUsername());
        String token = jwtService.generateToken(user);
        log.info("User logged in: {}", user.getUsername());
        return ResponseEntity.ok(new AuthResponse(token, user.getUsername(), user.getRole().name()));
    }

    /**
     * IP клиента для лимита регистрации. За nginx берём первый адрес
     * X-Forwarded-For, иначе адрес соединения.
     */
    private static String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            int comma = forwardedFor.indexOf(',');
            return (comma > 0 ? forwardedFor.substring(0, comma) : forwardedFor).trim();
        }
        return request.getRemoteAddr();
    }
}