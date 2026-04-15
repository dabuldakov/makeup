package com.example.makeup.service;

import com.example.makeup.entity.Video;
import com.example.makeup.entity.User;
import com.example.makeup.repository.VideoRepository;
import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoService {

    private final VideoRepository videoRepository;
    private final MinioClient minioClient;
    private final UserService userService;

    @Value("${minio.bucket}")
    private String bucketName;

    public Video uploadVideo(MultipartFile file, String title, String description, String username) {
        try {
            User user = userService.getUserByUsername(username);

            // Generate unique filename
            String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();

            // Upload to MinIO
            boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!bucketExists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            // Save metadata
            Video video = Video.builder()
                    .title(title)
                    .description(description)
                    .fileName(fileName)
                    .filePath(bucketName + "/" + fileName)
                    .contentType(file.getContentType())
                    .fileSize(file.getSize())
                    .uploadedBy(user)
                    .views(0)
                    .build();

            return videoRepository.save(video);

        } catch (Exception e) {
            log.error("Failed to upload video: {}", e.getMessage());
            throw new RuntimeException("Failed to upload video", e);
        }
    }

    public Resource getVideoFile(String fileName) {
        try {
            InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .build()
            );
            return new InputStreamResource(stream);
        } catch (Exception e) {
            log.error("Failed to get video file: {}", e.getMessage());
            throw new RuntimeException("Video not found", e);
        }
    }

    public String getVideoUrl(String fileName) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(io.minio.http.Method.GET)
                            .bucket(bucketName)
                            .object(fileName)
                            .expiry(15 * 60) // 15 minutes
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to generate presigned URL: {}", e.getMessage());
            throw new RuntimeException("Failed to generate video URL", e);
        }
    }

    public Page<Video> getAllVideos(Pageable pageable) {
        return videoRepository.findAll(pageable);
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