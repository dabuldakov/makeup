package com.example.makeup.auth;

import com.example.makeup.exception.ConflictException;
import com.example.makeup.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

import com.example.makeup.media.MinioService;
import com.example.makeup.news.NewsItem;
import com.example.makeup.news.NewsRepository;
import com.example.makeup.security.CustomUserDetailsService;
import com.example.makeup.video.Video;
import com.example.makeup.video.VideoLikeRepository;
import com.example.makeup.video.VideoRepository;
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CustomUserDetailsService userDetailsService;
    private final VideoRepository videoRepository;
    private final NewsRepository newsRepository;
    private final VideoLikeRepository videoLikeRepository;
    private final MinioService minioService;

    public User register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("Username already exists");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already exists");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(Role.USER)
                .isActive(true)
                .build();

        return userRepository.save(user);
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    public User updateUser(Long id, UpdateUserRequest request) {
        User user = getUserById(id);

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new ConflictException("Email already exists");
            }
            user.setEmail(request.getEmail());
        }

        User updated = userRepository.save(user);
        userDetailsService.evict(updated.getUsername());
        return updated;
    }

    /**
     * Удаление аккаунта = отзыв согласия на обработку персональных данных (ст. 14 ФЗ-152):
     * уничтожаем профиль, новости и видео пользователя, снимаем его лайки с чужих видео
     * и файлы из MinIO.
     *
     * <p>Записи удаляются в порядке новости → видео → пользователь: на videos.uploaded_by
     * и news.author_id стоит ON DELETE RESTRICT, поэтому явные flush() обязательны —
     * иначе Hibernate может выполнить DELETE из users раньше дочерних таблиц.
     *
     * <p>Файлы из MinIO удаляются после коммита: если транзакция откатится,
     * видео останется в БД и без файла.
     *
     * <p>Аватар и сообщения чатов здесь не удаляются: они хранятся в чат-бэкенде
     * и должны удаляться на его стороне.
     */
    @Transactional
    public void deleteAccount(Long id, String requesterUsername) {
        User user = getUserById(id);

        // Права проверяем у того, кто запрашивает, а не у удаляемого: иначе любой
        // авторизованный пользователь смог бы удалить аккаунт администратора.
        User requester = userRepository.findByUsername(requesterUsername)
                .orElseThrow(() -> new AccessDeniedException("Unknown requester"));
        boolean isAdmin = Role.ADMIN == requester.getRole();
        if (!isAdmin && !user.getUsername().equals(requesterUsername)) {
            throw new AccessDeniedException("You can only delete your own account");
        }

        List<Video> videos = videoRepository.findAllByUploadedBy(user);
        List<NewsItem> news = newsRepository.findAllByAuthor(user);

        purgeMinioAfterCommit(videos, news);

        // Лайки на чужих видео: снимаем отметки и пересчитываем денормализованные счётчики.
        for (Long videoId : videoLikeRepository.findVideoIdsByUserId(user.getId())) {
            videoRepository.adjustLikesCount(videoId, -1);
        }
        videoLikeRepository.deleteAllByUserId(user.getId());

        newsRepository.deleteAll(news);
        newsRepository.flush();

        // Каскад БД сносит media_jobs и лайки на этих видео.
        videoRepository.deleteAll(videos);
        videoRepository.flush();

        userRepository.delete(user);
        userRepository.flush();

        userDetailsService.evict(user.getUsername());
        // В лог только идентификаторы: он не должен копить персональные данные.
        log.info("Account deleted: id={}, videos={}, news={}", id, videos.size(), news.size());
    }

    /**
     * Собирает ключи объектов и удаляет их из MinIO только после успешного коммита.
     * Ошибки удаления глушит сам MinioService — повреждённые файлы не должны
     * откатывать уже завершённое удаление аккаунта.
     */
    private void purgeMinioAfterCommit(List<Video> videos, List<NewsItem> news) {
        List<String> videoKeys = videos.stream()
                .map(Video::getObjectKey)
                .filter(key -> key != null && !key.isBlank())
                .toList();
        List<String> thumbnailKeys = videos.stream()
                .map(Video::getThumbnailKey)
                .filter(key -> key != null && !key.isBlank())
                .toList();
        List<String> newsImageKeys = news.stream()
                .map(NewsItem::getImageKey)
                .filter(key -> key != null && !key.isBlank())
                .toList();

        if (videoKeys.isEmpty() && thumbnailKeys.isEmpty() && newsImageKeys.isEmpty()) {
            return;
        }

        Runnable purge = () -> {
            videoKeys.forEach(minioService::deleteVideo);
            thumbnailKeys.forEach(minioService::deleteThumbnail);
            newsImageKeys.forEach(minioService::deleteNewsImage);
            log.info("MinIO purge: videos={}, thumbnails={}, news images={}",
                    videoKeys.size(), thumbnailKeys.size(), newsImageKeys.size());
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    purge.run();
                }
            });
        } else {
            purge.run();
        }
    }
}
