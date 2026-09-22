package com.example.makeup.service;

import com.example.makeup.config.BucketType;
import com.example.makeup.dto.VideoItem;
import com.example.makeup.entity.Video;
import com.example.makeup.entity.User;
import com.example.makeup.exception.NotFoundException;
import com.example.makeup.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoService {

    private final VideoRepository videoRepository;
    private final MinioService minioService;
    private final UserService userService;
    private final ThumbnailProcessor thumbnailProcessor;

    public Video uploadVideo(MultipartFile file, String title, String description,
                             MultipartFile thumbnail, String username) {
        try {
            User user = userService.getUserByUsername(username);

            String fileId = UUID.randomUUID().toString();

            Path tempVideo = Files.createTempFile("video_", ".mp4");
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, tempVideo, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }

            String fileName;
            try (InputStream staged = Files.newInputStream(tempVideo)) {
                fileName = minioService.uploadVideo(staged, Files.size(tempVideo),
                        file.getContentType(), fileId);
            }

            Video.VideoBuilder builder = Video.builder()
                    .title(title)
                    .description(description)
                    .fileName(fileName)
                    .filePath("videos/" + fileName)
                    .contentType(file.getContentType())
                    .fileSize(file.getSize())
                    .uploadedBy(user)
                    .views(0)
                    .likes(0);

            // Клиент прислал готовое превью — сохраняем его синхронно
            // и пропускаем фоновую генерацию ffmpeg.
            if (thumbnail != null && !thumbnail.isEmpty()) {
                String thumbnailName = minioService.uploadThumbnail(thumbnail,
                        UUID.randomUUID().toString());
                builder.thumbnailPath(thumbnailName);

                Video savedVideo = videoRepository.save(builder.build());
                Files.deleteIfExists(tempVideo);
                return savedVideo;
            }

            Video savedVideo = videoRepository.save(builder.build());

            // Генерация превью вынесена в отдельный поток; временный файл удалит processor.
            thumbnailProcessor.generateAndAttach(savedVideo.getId(), tempVideo);

            return savedVideo;

        } catch (Exception e) {
            log.error("Failed to upload video: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload video", e);
        }
    }

    public Resource getVideoFile(String fileName) {
        return minioService.getVideoFile(fileName);
    }

    public String getVideoUrl(String fileName) {
        return minioService.getVideoPresignedUrl(fileName);
    }

    public String getThumbnailUrl(String fileName) {
        return minioService.getThumbnailPresignedUrl(fileName);
    }

    public byte[] getThumbnailBytes(String fileName) {
        return minioService.getImageBytes(fileName, BucketType.THUMBNAILS);
    }

    public org.springframework.core.io.Resource getThumbnailFile(String fileName) {
        return minioService.getImageFile(fileName, BucketType.THUMBNAILS);
    }

    public Page<VideoItem> getAllVideos(Pageable pageable) {
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
}