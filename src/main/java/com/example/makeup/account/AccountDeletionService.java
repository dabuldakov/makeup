package com.example.makeup.account;

import com.example.makeup.auth.Role;
import com.example.makeup.auth.User;
import com.example.makeup.auth.UserRepository;
import com.example.makeup.auth.UserService;
import com.example.makeup.news.NewsItem;
import com.example.makeup.news.NewsRepository;
import com.example.makeup.media.NewsImageStorage;
import com.example.makeup.media.ThumbnailStorage;
import com.example.makeup.media.VideoStorage;
import com.example.makeup.auth.CustomUserDetailsService;
import com.example.makeup.video.Video;
import com.example.makeup.video.VideoLikeRepository;
import com.example.makeup.video.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

/**
 * Удаление аккаунта = отзыв согласия на обработку персональных данных (ст. 14 ФЗ-152):
 * уничтожаем профиль, новости и видео пользователя, снимаем его лайки с чужих видео
 * и файлы из MinIO.
 *
 * <p>Вынесено из {@code UserService} в отдельный модуль: это оркестрация данных
 * нескольких bounded context (auth, news, video, media), а не часть профиля.
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
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountDeletionService {

    private final UserService userService;
    private final UserRepository userRepository;
    private final VideoRepository videoRepository;
    private final NewsRepository newsRepository;
    private final VideoLikeRepository videoLikeRepository;
    private final VideoStorage videoStorage;
    private final ThumbnailStorage thumbnailStorage;
    private final NewsImageStorage newsImageStorage;
    private final CustomUserDetailsService userDetailsService;

    @Transactional
    public void deleteAccount(Long id, String requesterUsername) {
        User user = userService.getUserById(id);

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
     * Ошибки удаления глушат сами хранилища — повреждённые файлы не должны
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
            videoKeys.forEach(videoStorage::delete);
            thumbnailKeys.forEach(thumbnailStorage::delete);
            newsImageKeys.forEach(newsImageStorage::delete);
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
