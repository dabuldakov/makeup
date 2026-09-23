package com.example.makeup.media;

import com.example.makeup.video.Video;
import com.example.makeup.video.VideoStatus;
import com.example.makeup.video.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Обработка одной задачи медиа в отдельной транзакции. Воркер вызывает этот
 * бин (а не сам себя), чтобы {@link Transactional} применялся и «долгий» ffmpeg
 * не держал транзакцию claim-а.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MediaJobProcessor {

    private static final int MAX_ATTEMPTS = 3;

    private final MediaJobRepository mediaJobRepository;
    private final VideoRepository videoRepository;
    private final MinioService minioService;
    private final ThumbnailGeneratorService thumbnailGeneratorService;

    @Transactional
    public void process(Long jobId) {
        MediaJob job = mediaJobRepository.findById(jobId).orElse(null);
        if (job == null) {
            return;
        }

        Path temp = null;
        try {
            Video video = videoRepository.findById(job.getVideoId())
                    .orElseThrow(() -> new IllegalStateException("Video not found: " + job.getVideoId()));

            temp = Files.createTempFile("video_", ".mp4");
            try (InputStream in = minioService.getVideoFile(video.getObjectKey()).getInputStream()) {
                Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
            }

            BufferedImage thumbnail = thumbnailGeneratorService.generateThumbnail(temp);
            String thumbnailKey = minioService.uploadThumbnail(thumbnail, UUID.randomUUID().toString());

            videoRepository.updateThumbnailAndStatus(video.getId(), thumbnailKey, VideoStatus.PUBLISHED);

            job.setStatus(MediaJobStatus.DONE);
            job.setLastError(null);
            log.info("Thumbnail attached to video {}: {}", video.getId(), thumbnailKey);
        } catch (Exception e) {
            log.warn("Media job {} failed (attempt {}): {}", jobId, job.getAttempts(), e.getMessage());
            job.setLastError(truncate(e.getMessage()));
            if (job.getAttempts() >= MAX_ATTEMPTS) {
                job.setStatus(MediaJobStatus.FAILED);
                videoRepository.updateStatus(job.getVideoId(), VideoStatus.FAILED);
            } else {
                job.setStatus(MediaJobStatus.PENDING);
            }
        } finally {
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (Exception e) {
                    log.warn("Failed to delete temp video file: {}", temp, e);
                }
            }
        }

        mediaJobRepository.save(job);
    }

    private String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }
}
