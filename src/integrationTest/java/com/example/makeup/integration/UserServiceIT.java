package com.example.makeup.integration;

import com.example.makeup.auth.UpdateUserRequest;
import com.example.makeup.auth.User;
import com.example.makeup.auth.UserService;
import com.example.makeup.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Интеграционные тесты работы с пользователями: чтение профиля из БД,
 * обновление профиля (fullName/avatar) и кеш UserDetails.
 * Собственного логина/пароля больше нет — вход только через единый IdP.
 * Email здесь не хранится: его владелец — auth-service.
 */
class UserServiceIT extends AbstractIntegrationTest {

    @Autowired
    private UserService userService;

    @Test
    void getUserByUsernameReturnsPersistedUser() {
        createUser("alice");

        User user = userService.getUserByUsername("alice");

        assertThat(user.getUsername()).isEqualTo("alice");
    }

    @Test
    void getUserByUsernameThrowsForUnknownUser() {
        assertThatThrownBy(() -> userService.getUserByUsername("missing"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void getUserByIdReturnsPersistedUser() {
        User saved = createUser("bob");

        assertThat(userService.getUserById(saved.getId()).getUsername()).isEqualTo("bob");
    }

    @Test
    void getUserByIdThrowsForUnknownUser() {
        assertThatThrownBy(() -> userService.getUserById(999L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateUserChangesFullNameAndAvatar() {
        User saved = createUser("carol");

        UpdateUserRequest updates = new UpdateUserRequest();
        updates.setFullName("Carol Updated");
        updates.setAvatarUrl("http://avatar/carol.png");
        User updated = userService.updateUser(saved.getId(), updates);

        assertThat(updated.getFullName()).isEqualTo("Carol Updated");
        assertThat(updated.getAvatarUrl()).isEqualTo("http://avatar/carol.png");
    }

    @Test
    void loadUserReturnsPersistedUserDetails() {
        createUser("erin");

        assertThat(loadUser("erin").getUsername()).isEqualTo("erin");
    }
}
