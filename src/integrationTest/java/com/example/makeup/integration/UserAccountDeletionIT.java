package com.example.makeup.integration;

import com.example.makeup.auth.User;
import com.example.makeup.account.AccountDeletionService;
import com.example.makeup.news.NewsRepository;
import com.example.makeup.news.NewsService;
import com.example.makeup.video.VideoLikeRepository;
import com.example.makeup.video.VideoRepository;
import com.example.makeup.video.VideoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Удаление аккаунта: профиль, новости, видео, лайки и файлы MinIO.
 * MinIO замокан (см. {@link AbstractIntegrationTest}), поэтому проверяем,
 * что сервис заказывает удаление объектов, а не сам ходит в S3.
 */
class UserAccountDeletionIT extends AbstractIntegrationTest {

    @Autowired
    private AccountDeletionService deletionService;

    @Autowired
    private VideoService videoService;

    @Autowired
    private NewsService newsService;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private NewsRepository newsRepository;

    @Autowired
    private VideoLikeRepository videoLikeRepository;

    private MockMultipartFile image() {
        return new MockMultipartFile("image", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3});
    }

    private User createAdmin(String username) {
        return userRepository.save(User.builder()
                .username(username)
                .email(username + "@example.com")
                .password(passwordEncoder.encode("password123"))
                .fullName("Test " + username)
                .role(com.example.makeup.auth.Role.ADMIN)
                .isActive(true)
                .build());
    }

    @Test
    void deleteAccountRemovesProfileNewsAndVideos() {
        User user = createUser("leaver");
        videoService.uploadVideo(videoFile(), "My video", "d", image(), user.getUsername());
        videoService.uploadVideo(videoFile(), "Another video", "d", null, user.getUsername());
        newsService.createNews("News title", "News body", null, user.getUsername(), image());
        newsService.createNews("Second news", "body", null, user.getUsername(), null);

        deletionService.deleteAccount(user.getId(), user.getUsername());

        assertThat(userRepository.findById(user.getId())).isEmpty();
        assertThat(videoRepository.findAll()).isEmpty();
        assertThat(newsRepository.findAll()).isEmpty();
    }

    @Test
    void deleteAccountPurgesObjectsFromMinio() {
        User user = createUser("storage");
        var video = videoService.uploadVideo(videoFile(), "With thumb", "d", image(), user.getUsername());
        newsService.createNews("With image", "body", null, user.getUsername(), image());

        String newsImageKey = newsRepository.findAll().get(0).getImageKey();;

        deletionService.deleteAccount(user.getId(), user.getUsername());

        verify(minioService).deleteVideo(video.getObjectKey());
        verify(minioService).deleteThumbnail(video.getThumbnailKey());
        verify(minioService).deleteNewsImage(newsImageKey);
    }

    @Test
    void deleteAccountRemovesLikesFromOtherUsersVideos() {
        User owner = createUser("owner");
        User leaver = createUser("leaver2");
        var video = videoService.uploadVideo(videoFile(), "Popular", "d", null, owner.getUsername());

        videoService.toggleLike(video.getId(), leaver.getUsername());
        assertThat(videoRepository.findById(video.getId()).orElseThrow().getLikesCount()).isEqualTo(1L);

        deletionService.deleteAccount(leaver.getId(), leaver.getUsername());

        assertThat(videoLikeRepository.findVideoIdsByUserId(leaver.getId())).isEmpty();
        assertThat(videoRepository.findById(video.getId()).orElseThrow().getLikesCount()).isZero();
    }

    @Test
    void deleteAccountKeepsOtherUsersContent() {
        User owner = createUser("stay");
        User leaver = createUser("leave");
        videoService.uploadVideo(videoFile(), "Stays", "d", null, owner.getUsername());
        newsService.createNews("Stays too", "body", null, owner.getUsername(), null);

        deletionService.deleteAccount(leaver.getId(), leaver.getUsername());

        assertThat(videoRepository.findAll()).hasSize(1);
        assertThat(newsRepository.findAll()).hasSize(1);
        assertThat(userRepository.findByUsername("stay")).isPresent();
    }

    @Test
    void deleteAccountRejectsForeignAccount() {
        User victim = createUser("victim");
        User attacker = createUser("attacker");

        assertThatThrownBy(() -> deletionService.deleteAccount(victim.getId(), attacker.getUsername()))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);

        assertThat(userRepository.findById(victim.getId())).isPresent();
    }

    /**
     * Роль проверяется у того, кто запрашивает, а не у удаляемого. Иначе любой
     * пользователь смог бы удалить аккаунт администратора: он «админ», значит
     * проверка на владельца не сработала бы.
     */
    @Test
    void deleteAccountRejectsForeignAdminAccount() {
        User admin = createAdmin("root");
        User attacker = createUser("attacker3");

        assertThatThrownBy(() -> deletionService.deleteAccount(admin.getId(), attacker.getUsername()))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);

        assertThat(userRepository.findById(admin.getId())).isPresent();
    }

    @Test
    void deleteAccountAllowsAdminToDeleteAnotherUser() {
        User admin = createAdmin("admin-del");
        User victim = createUser("victim3");

        deletionService.deleteAccount(victim.getId(), admin.getUsername());

        assertThat(userRepository.findById(victim.getId())).isEmpty();
        assertThat(userRepository.findById(admin.getId())).isPresent();
    }

    @Test
    void deleteAccountEvictsCachedUserDetails() {
        User user = createUser("cached");
        assertThat(loadUser("cached")).isNotNull();

        deletionService.deleteAccount(user.getId(), user.getUsername());

        assertThatThrownBy(() -> loadUser("cached"))
                .isInstanceOf(org.springframework.security.core.userdetails.UsernameNotFoundException.class);
    }

    @Test
    void deleteAccountEndpointReturnsNoContent() throws Exception {
        String token = registerUser("viahttp");
        Long id = userRepository.findByUsername("viahttp").orElseThrow().getId();

        mockMvc.perform(delete("/api/users/{id}", id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        assertThat(userRepository.findByUsername("viahttp")).isEmpty();
    }

    @Test
    void deleteAccountEndpointRejectsAnotherUsersAccount() throws Exception {
        User victim = createUser("victim2");
        String token = registerUser("attacker2");

        mockMvc.perform(delete("/api/users/{id}", victim.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        assertThat(userRepository.findById(victim.getId())).isPresent();
    }

    /**
     * Без токена запрос отклоняется фильтром безопасности. В этом проекте для
     * анонимных обращений настроен не 401, а 403 (точка входа по умолчанию).
     */
    @Test
    void deleteAccountEndpointRejectsAnonymousRequest() throws Exception {
        User user = createUser("anon");

        mockMvc.perform(delete("/api/users/{id}", user.getId()))
                .andExpect(status().isForbidden());

        assertThat(userRepository.findById(user.getId())).isPresent();
        verify(minioService, never()).deleteVideo(org.mockito.ArgumentMatchers.anyString());
    }
}
