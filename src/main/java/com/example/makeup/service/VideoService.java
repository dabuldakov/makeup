package com.example.makeup.service;

import com.example.makeup.config.BucketType;
import com.example.makeup.entity.Video;
import com.example.makeup.entity.User;
import com.example.makeup.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoService {

    private final VideoRepository videoRepository;
    private final MinioService minioService;
    private final UserService userService;
    private final ThumbnailGeneratorService thumbnailGeneratorService;

    public Video uploadVideo(MultipartFile file, String title, String description, String username) {
        try {
            User user = userService.getUserByUsername(username);

            // Загружаем видео в MinIO
            String fileId = UUID.randomUUID().toString();

            String fileName = minioService.uploadVideo(file, fileId);

            // Сохраняем метаданные
            Video video = Video.builder()
                    .title(title)
                    .description(description)
                    .fileName(fileName)
                    .filePath("videos" + "/" + fileName)
                    .contentType(file.getContentType())
                    .fileSize(file.getSize())
                    .uploadedBy(user)
                    .views(0)
                    .build();

            Video savedVideo = videoRepository.save(video);

            // Генерируем и загружаем превью
            try {
                BufferedImage thumbnail = thumbnailGeneratorService.generateThumbnail(file);
                String thumbnailPAth = minioService.uploadThumbnail(thumbnail, fileId);
                savedVideo.setThumbnailPath("thumbnails/" + thumbnailPAth);
                return videoRepository.save(savedVideo);
            } catch (Exception e) {
                log.warn("Failed to generate thumbnail for video {}: {}", savedVideo.getId(), e.getMessage());
                return savedVideo;
            }

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

    public Page<Video> getAllVideos(Pageable pageable) {
        Page<Video> videos = videoRepository.findAll(pageable);
        videos.forEach(video -> {
            video.setFilePath(video.getFilePath());
            video.setThumbnailPath(video.getThumbnailPath());
        });
        return videos;
    }

    public Video getVideoById(Long id) {
        return videoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video not found"));
    }

    public void incrementViews(Long id) {
        Video video = getVideoById(id);
        video.setViews(video.getViews() + 1);
        videoRepository.save(video);
    }
}