package com.example.makeup.media;

import com.example.makeup.exception.NotFoundException;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Map;
import javax.imageio.ImageIO;

import com.example.makeup.video.Video;
@Service
@RequiredArgsConstructor
@Slf4j
public class MinioService {

    private final MinioClient minioClient;
    private final MinioClient minioClientForPresignedUrls;

    /**
     * Регион задаётся явно, чтобы клиент не делал сетевой GetBucketLocation
     * (иначе при недоступном внешнем MinIO генерация URL подвисает).
     */
    private static final String PRESIGNED_REGION = "us-east-1";

    @Value("${minio.bucket}")
    private String videoBucketName;

    @Value("${minio.thumbnail-bucket}")
    private String thumbnailBucketName;

    @Value("${minio.news-image}")
    private String newsImageBucketName;

    @PostConstruct
    public void init() {
        for (BucketType bucketType : BucketType.values()) {
            createBucket(bucketType);
        }
    }

    private void createBucket(BucketType bucketType) {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketType.getBucketName()).build()
            );
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(bucketType.getBucketName()).build()
                );
                log.info("{} created: {}", bucketType.getDescription(), bucketType.getBucketName());
            }
        } catch (Exception e) {
            log.error("Failed to create {}: {}", bucketType.getDescription().toLowerCase(), e.getMessage(), e);
        }
    }

    /**
     * Загрузка видео файла в MinIO
     */
    public String uploadVideo(MultipartFile file, String fileId) {
        try {
            return uploadVideo(file.getInputStream(), file.getSize(), file.getContentType(), fileId);
        } catch (Exception e) {
            log.error("Failed to upload video: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload video", e);
        }
    }

    public String uploadVideo(InputStream stream, long size, String contentType, String fileId) {
        try {
            String objectName = fileId + getExtensionFromContentType(contentType);

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(videoBucketName)
                            .object(objectName)
                            .stream(stream, size, -1)
                            .contentType(contentType)
                            .build()
            );

            log.info("Video uploaded: {}", objectName);
            return objectName;
        } catch (Exception e) {
            log.error("Failed to upload video: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload video", e);
        }
    }

    public String uploadThumbnail(BufferedImage image, String fileId) {
        return uploadImage(image, fileId, BucketType.THUMBNAILS);
    }

    /**
     * Загрузка превью в MinIO из файла, переданного клиентом вместе с видео.
     * Позволяет сохранить превью сразу при загрузке, не дожидаясь async ffmpeg.
     */
    public String uploadThumbnail(MultipartFile file, String fileId) {
        try {
            String objectName = fileId + ".jpeg";

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(thumbnailBucketName)
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType("image/jpeg")
                            .build()
            );

            log.info("Thumbnail uploaded: {}", objectName);
            return objectName;
        } catch (Exception e) {
            log.error("Failed to upload thumbnail: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload thumbnail", e);
        }
    }

    /**
     * Загрузка изображения для новости в MinIO
     */
    public String uploadNewsImage(MultipartFile file, String fileId) {
        try {
            String fileName = fileId + ".jpeg";

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(newsImageBucketName)
                            .object(fileName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            log.info("News image uploaded: {}", fileName);
            return fileName;
        } catch (Exception e) {
            log.error("Failed to upload news image: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload news image", e);
        }
    }

    /**
     * Загрузка превью видео в MinIO
     */
    private String uploadImage(BufferedImage image, String fileId, BucketType bucketType) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", baos);
            byte[] thumbnailBytes = baos.toByteArray();

            String objectName = fileId + ".jpeg";

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketType.getBucketName())
                            .object(objectName)
                            .stream(new ByteArrayInputStream(thumbnailBytes), thumbnailBytes.length, -1)
                            .contentType("image/jpeg")
                            .build()
            );

            log.info("Image uploaded: {}", objectName);
            return objectName;
        } catch (Exception e) {
            log.error("Failed to upload image: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload image", e);
        }
    }

    /**
     * Получение видео файла из MinIO
     */
    public Resource getVideoFile(String fileName) {
        try {
            InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(videoBucketName)
                            .object(fileName)
                            .build()
            );
            return new InputStreamResource(stream);
        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code())) {
                log.warn("Video file not found in bucket '{}': {}", videoBucketName, fileName);
                throw new NotFoundException("Video file not found: " + fileName);
            }
            log.error("Failed to get video file: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get video file", e);
        } catch (Exception e) {
            log.error("Failed to get video file: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get video file", e);
        }
    }

    /**
     * Получение превью из MinIO
     */
    public Resource getThumbnailFile(String thumbnailName) {
        try {
            InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(thumbnailBucketName)
                            .object(thumbnailName)
                            .build()
            );
            return new InputStreamResource(stream);
        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code())) {
                log.warn("Thumbnail not found in bucket '{}': {}", thumbnailBucketName, thumbnailName);
                throw new NotFoundException("Thumbnail not found: " + thumbnailName);
            }
            log.error("Failed to get thumbnail: {}", e.getMessage(), e);
            throw new RuntimeException("Thumbnail not found", e);
        } catch (Exception e) {
            log.error("Failed to get thumbnail: {}", e.getMessage(), e);
            throw new RuntimeException("Thumbnail not found", e);
        }
    }

    /**
     * Получение превью/картинки как байтовый массив
     */
    public byte[] getImageBytes(String imageName, BucketType bucketType) {
        try {
            InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketType.getBucketName())
                            .object(imageName)
                            .build()
            );
            return stream.readAllBytes();
        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code())) {
                throw new NotFoundException("Image not found: " + imageName);
            }
            log.error("Failed to get image: {}", e.getMessage(), e);
            throw new RuntimeException("Image not found", e);
        } catch (Exception e) {
            log.error("Failed to get image: {}", e.getMessage(), e);
            throw new RuntimeException("Image not found", e);
        }
    }

    /**
     * Получение превью/картинки потоком (не грузит объект в heap целиком).
     */
    public Resource getImageFile(String imageName, BucketType bucketType) {
        try {
            InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketType.getBucketName())
                            .object(imageName)
                            .build()
            );
            return new InputStreamResource(stream);
        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code())) {
                log.warn("Image not found in bucket '{}': {}", bucketType.getBucketName(), imageName);
                throw new NotFoundException("Image not found: " + imageName);
            }
            log.error("Failed to get image: {}", e.getMessage(), e);
            throw new RuntimeException("Image not found", e);
        } catch (Exception e) {
            log.error("Failed to get image: {}", e.getMessage(), e);
            throw new RuntimeException("Image not found", e);
        }
    }

    /**
     * Генерация presigned URL для видео
     */
    public String getVideoPresignedUrl(String fileName) {
        try {
            return minioClientForPresignedUrls.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(videoBucketName)
                            .object(fileName)
                            .region(PRESIGNED_REGION)
                            .expiry(15 * 60) // 15 minutes
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to generate presigned URL: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate video URL", e);
        }
    }

    /**
     * Генерация presigned URL для превью
     */
    public String getThumbnailPresignedUrl(String thumbnailName) {
        try {
            return minioClientForPresignedUrls.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(thumbnailBucketName)
                            .object(thumbnailName)
                            .region(PRESIGNED_REGION)
                            .expiry(60 * 60) // 1 hour for thumbnails
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to generate thumbnail presigned URL: {}", e.getMessage(), e);
            return "";
        }
    }

    /**
     * Удаление видео из MinIO
     */
    public void deleteVideo(String fileName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(videoBucketName)
                            .object(fileName)
                            .build()
            );
            log.info("Video deleted: {}", fileName);
        } catch (Exception e) {
            log.error("Failed to delete video: {}", e.getMessage(), e);
        }
    }

    /**
     * Удаление превью из MinIO
     */
    public void deleteThumbnail(String thumbnailName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(thumbnailBucketName)
                            .object(thumbnailName)
                            .build()
            );
            log.info("Thumbnail deleted: {}", thumbnailName);
        } catch (Exception e) {
            log.error("Failed to delete thumbnail: {}", e.getMessage(), e);
        }
    }

    /**
     * Удаление изображения новости из MinIO
     */
    public void deleteNewsImage(String fileName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(newsImageBucketName)
                            .object(fileName)
                            .build()
            );
            log.info("News image deleted: {}", fileName);
        } catch (Exception e) {
            log.error("Failed to delete news image: {}", e.getMessage(), e);
        }
    }

    private static final Map<String, String> MIME_TO_EXTENSION = Map.of(
            "video/mp4", ".mp4",
            "video/mpeg", ".mpeg",
            "video/quicktime", ".mov",
            "video/x-msvideo", ".avi",
            "video/webm", ".webm",
            "video/x-matroska", ".mkv",
            "video/ogg", ".ogv",
            "video/3gpp", ".3gp",
            "video/x-flv", ".flv"
    );

    public static String getExtensionFromContentType(String contentType) {
        if (contentType == null) {
            return ".mp4";
        }
        return MIME_TO_EXTENSION.getOrDefault(contentType.toLowerCase(),
                contentType.startsWith("video/") ? "." + contentType.split("/")[1] : ".mp4");
    }
}