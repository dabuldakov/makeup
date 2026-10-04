package com.example.makeup.media;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Хранилище изображений новостей (bucket news-image): загрузка, чтение, удаление.
 */
@Component
public class NewsImageStorage extends MinioObjectStorage {

    public NewsImageStorage(@Qualifier("minioClient") MinioClient client,
                            @Qualifier("minioClientForPresignedUrls") MinioClient presignedClient,
                            MinioConfig config) {
        super(client, presignedClient, config.getNewsImage());
    }

    public String upload(MultipartFile file, String fileId) {
        try {
            String objectName = fileId + ".jpeg";
            return putObject(file.getInputStream(), file.getSize(), file.getContentType(), objectName);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to upload news image", e);
        }
    }

    public Resource get(String objectName) {
        return readResource(objectName);
    }

    public void delete(String objectName) {
        removeObject(objectName);
    }
}
