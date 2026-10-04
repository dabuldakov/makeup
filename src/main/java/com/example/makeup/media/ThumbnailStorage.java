package com.example.makeup.media;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * Хранилище превью видео (bucket thumbnails): загрузка из сгенерированного
 * изображения или файла клиента, чтение, presigned URL, удаление.
 */
@Component
public class ThumbnailStorage extends MinioObjectStorage {

    private static final int PRESIGNED_EXPIRY_SECONDS = 60 * 60;

    public ThumbnailStorage(MinioClient client,
                            @Qualifier("minioClientForPresignedUrls") MinioClient presignedClient,
                            MinioConfig config) {
        super(client, presignedClient, config.getThumbnailBucket());
    }

    public String upload(BufferedImage image, String fileId) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", output);
            byte[] bytes = output.toByteArray();
            String objectName = fileId + ".jpeg";
            return putObject(new ByteArrayInputStream(bytes), bytes.length, "image/jpeg", objectName);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to upload thumbnail", e);
        }
    }

    public String upload(MultipartFile file, String fileId) {
        try {
            String objectName = fileId + ".jpeg";
            return putObject(file.getInputStream(), file.getSize(), "image/jpeg", objectName);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to upload thumbnail", e);
        }
    }

    public Resource get(String objectName) {
        return readResource(objectName);
    }

    public String presignedUrl(String objectName) {
        return presignedObjectUrl(objectName, PRESIGNED_EXPIRY_SECONDS);
    }

    public void delete(String objectName) {
        removeObject(objectName);
    }
}
