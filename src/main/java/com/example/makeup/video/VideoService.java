package com.example.makeup.video;

import com.example.makeup.media.BucketType;
import com.example.makeup.auth.User;
import com.example.makeup.exception.NotFoundException;
import com.example.makeup.news.NewsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import com.example.makeup.auth.UserService;
import com.example.makeup.media.MediaJobService;
import com.example.makeup.media.MinioService;
@Service
@RequiredArgsConstructor
@Slf4j
public class VideoService {

    private final VideoRepository videoRepository;
    private final VideoLikeRepository videoLikeRepository;
    private final NewsRepository newsRepository;
    private final MinioService minioService;
    private final UserService userService;
    private final MediaJobService mediaJobService;

    @Transactional
    public Video uploadVideo(MultipartFile file, String title, String description,
                             MultipartFile thumbnail, String username) {
        try {
            User user = userService.getUserByUsername(username);

            String fileId = UUID.randomUUID().toString();
            Path tempVideo = Files.createTempFile("video_", ".mp4");
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, tempVideo, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }

            String objectKey;
            try (InputStream staged = Files.newInputStream(tempVideo)) {
                objectKey = minioService.uploadVideo(staged, Files.size(tempVideo),
                        file.getContentType(), fileId);
            }
            Files.deleteIfExists(tempVideo);

            Video.VideoBuilder builder = Video.builder()
                    .title(title)
                    .description(description)
                    .objectKey(objectKey)
                    .contentType(file.getContentType())
                    .fileSize(file.getSize())
                    .uploadedBy(user)
                    .views(0L)
                    .likesCount(0L);

            // Клиент прислал готовое превью — публикуем сразу.
            if (thumbnail != null && !thumbnail.isEmpty()) {
                String thumbnailKey = minioService.uploadThumbnail(thumbnail, UUID.randomUUID().toString());
                builder.thumbnailKey(thumbnailKey).status(VideoStatus.PUBLISHED);

                Video savedVideo = videoRepository.save(builder.build());
                log.info("Video uploaded: id={}, title={}, by user={}", savedVideo.getId(), title, username);
                return savedVideo;
            }

            // Видео публикуется сразу, превью сгенерируем в фоне через media_jobs.
            builder.status(VideoStatus.PUBLISHED);
            Video savedVideo = videoRepository.save(builder.build());
            mediaJobService.enqueue(savedVideo.getId());

            log.info("Video uploaded: id={}, title={}, by user={} (thumbnail queued)",
                    savedVideo.getId(), title, username);
            return savedVideo;

        } catch (Exception e) {
            log.error("Failed to upload video: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload video", e);
        }
    }

    public Resource getVideoFile(String objectKey) {
        return minioService.getVideoFile(objectKey);
    }

    public String getVideoUrl(String objectKey) {
        return minioService.getVideoPresignedUrl(objectKey);
    }

    public String getThumbnailUrl(String thumbnailKey) {
        return minioService.getThumbnailPresignedUrl(thumbnailKey);
    }

    public byte[] getThumbnailBytes(String thumbnailKey) {
        return minioService.getImageBytes(thumbnailKey, BucketType.THUMBNAILS);
    }

    public Resource getThumbnailFile(String thumbnailKey) {
        return minioService.getImageFile(thumbnailKey, BucketType.THUMBNAILS);
    }

    public Page<VideoItem> getAllVideos(Pageable pageable) {
        log.debug("Fetching videos list (page: {}, size: {})", pageable.getPageNumber(), pageable.getPageSize());
        return videoRepository.findAllProjected(pageable);
    }

    public Video getVideoById(Long id) {
        return videoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Video not found"));
    }

    /**
     * Атомарно увеличивает счётчик просмотров. Возвращает 0, если видео не найдено,
     * и флаг существования приходит из самой UPDATE-операции (без отдельного SELECT).
     */
    @Transactional
    public int incrementViews(Long id) {
        int updated = videoRepository.incrementViews(id);
        if (updated == 0) {
            throw new NotFoundException("Video not found");
        }
        return updated;
    }

    /**
     * Переключает лайк пользователя и синхронно правит денормализованный счётчик.
     * Возвращает актуальное число лайков.
     */
    @Transactional
    public long toggleLike(Long videoId, String username) {
        User user = userService.getUserByUsername(username);
        getVideoById(videoId);

        if (videoLikeRepository.existsByUserIdAndVideoId(user.getId(), videoId)) {
            videoLikeRepository.deleteByUserIdAndVideoId(user.getId(), videoId);
            videoRepository.adjustLikesCount(videoId, -1);
        } else {
            videoLikeRepository.save(VideoLike.builder()
                    .userId(user.getId())
                    .videoId(videoId)
                    .build());
            videoRepository.adjustLikesCount(videoId, 1);
        }

        return videoRepository.findById(videoId)
                .map(Video::getLikesCount)
                .orElse(0L);
    }

    public boolean isLikedBy(Long videoId, String username) {
        User user = userService.getUserByUsername(username);
        return videoLikeRepository.existsByUserIdAndVideoId(user.getId(), videoId);
    }

    /**
     * Удаление видео: доступно только загрузившему его пользователю.
     * Отвязывает видео от новостей и удаляет файл + превью из MinIO.
     */
    @Transactional
    public void deleteVideo(Long id, String username) {
        Video video = getVideoById(id);

        if (video.getUploadedBy() == null || !username.equals(video.getUploadedBy().getUsername())) {
            throw new AccessDeniedException("Only the uploader can delete this video");
        }

        newsRepository.detachVideo(id);

        if (video.getThumbnailKey() != null && !video.getThumbnailKey().isBlank()) {
            minioService.deleteThumbnail(video.getThumbnailKey());
        }
        if (video.getObjectKey() != null && !video.getObjectKey().isBlank()) {
            minioService.deleteVideo(video.getObjectKey());
        }

        videoRepository.delete(video);
        log.info("Video deleted: id={}, by user={}", id, username);
    }
}
