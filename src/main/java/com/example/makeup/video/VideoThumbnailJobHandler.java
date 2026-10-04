package com.example.makeup.video;

import com.example.makeup.media.MediaJob;
import com.example.makeup.media.MediaJobHandler;
import com.example.makeup.media.MediaJobType;
import com.example.makeup.media.ThumbnailGeneratorService;
import com.example.makeup.media.ThumbnailStorage;
import com.example.makeup.media.VideoStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Генерация превью видео. Реализует порт {@link MediaJobHandler}, поэтому
 * медиа-очередь не знает о Video, а зависимость остаётся только video → media.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VideoThumbnailJobHandler implements MediaJobHandler {

    private final VideoRepository videoRepository;
    private final VideoStorage videoStorage;
    private final ThumbnailStorage thumbnailStorage;
    private final ThumbnailGeneratorService thumbnailGeneratorService;

    @Override
    public MediaJobType type() {
        return MediaJobType.THUMBNAIL;
    }

    @Override
    public void handle(MediaJob job) throws Exception {
        Video video = videoRepository.findById(job.getTargetId())
                .orElseThrow(() -> new IllegalStateException("Video not found: " + job.getTargetId()));

        Path temp = Files.createTempFile("video_", ".mp4");
        try {
            try (InputStream in = videoStorage.get(video.getObjectKey()).getInputStream()) {
                Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
            }

            BufferedImage thumbnail = thumbnailGeneratorService.generateThumbnail(temp);
            String thumbnailKey = thumbnailStorage.upload(thumbnail, UUID.randomUUID().toString());

            videoRepository.updateThumbnailAndStatus(video.getId(), thumbnailKey, VideoStatus.PUBLISHED);
            log.info("Thumbnail attached to video {}: {}", video.getId(), thumbnailKey);
        } finally {
            try {
                Files.deleteIfExists(temp);
            } catch (Exception e) {
                log.warn("Failed to delete temp video file: {}", temp, e);
            }
        }
    }

    @Override
    public void onPermanentFailure(MediaJob job) {
        videoRepository.updateStatus(job.getTargetId(), VideoStatus.FAILED);
    }
}
