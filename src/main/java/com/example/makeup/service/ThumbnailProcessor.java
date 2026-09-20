package com.example.makeup.service;

import com.example.makeup.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Генерация превью вынесена в отдельный бин, чтобы аннотация {@link Async}
 * работала (self-invocation внутри VideoService её бы проигнорировал).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ThumbnailProcessor {

    private final ThumbnailGeneratorService thumbnailGeneratorService;
    private final MinioService minioService;
    private final VideoRepository videoRepository;

    @Async
    @Transactional
    public void generateAndAttach(Long videoId, Path videoPath) {
        try {
            BufferedImage thumbnail = thumbnailGeneratorService.generateThumbnail(videoPath);
            String objectName = minioService.uploadThumbnail(thumbnail, UUID.randomUUID().toString());

            videoRepository.updateThumbnailPath(videoId, objectName);

            log.info("Thumbnail attached to video {}: {}", videoId, objectName);
        } catch (Exception e) {
            log.warn("Failed to generate thumbnail for video {}: {}", videoId, e.getMessage());
        } finally {
            try {
                Files.deleteIfExists(videoPath);
            } catch (Exception e) {
                log.warn("Failed to delete temp video file: {}", videoPath, e);
            }
        }
    }
}