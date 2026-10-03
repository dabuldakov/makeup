package com.example.makeup.account;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Удаление аккаунта вынесено из {@code UserController}: это оркестрация очистки
 * данных нескольких модулей (auth, news, video, media), а не часть профиля.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class AccountController {

    private final AccountDeletionService accountDeletionService;

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccount(
            @PathVariable Long id,
            Authentication authentication) {
        accountDeletionService.deleteAccount(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
