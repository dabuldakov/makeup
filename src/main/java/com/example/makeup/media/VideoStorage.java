package com.example.makeup.media;

import io.minio.MinioClient;
import io.minio.StatObjectResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Map;

/**
 * Хранилище видеофайлов (bucket videos): загрузка, стриминг (в т.ч. Range),
 * метаданные, presigned URL, удаление.
 */
@Component
public class VideoStorage extends MinioObjectStorage {

    private static final int PRESIGNED_EXPIRY_SECONDS = 15 * 60;

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

    public VideoStorage(@Qualifier("minioClient") MinioClient client,
                        @Qualifier("minioClientForPresignedUrls") MinioClient presignedClient,
                        MinioConfig config) {
        super(client, presignedClient, config.getBucket());
    }

    public String upload(InputStream stream, long size, String contentType, String fileId) {
        String objectName = fileId + getExtensionFromContentType(contentType);
        return putObject(stream, size, contentType, objectName);
    }

    public Resource get(String objectName) {
        return readResource(objectName);
    }

    public Resource get(String objectName, long offset, long length) {
        return readResource(objectName, offset, length);
    }

    public StatObjectResponse stat(String objectName) {
        return statObject(objectName);
    }

    public String presignedUrl(String objectName) {
        return presignedObjectUrl(objectName, PRESIGNED_EXPIRY_SECONDS);
    }

    public void delete(String objectName) {
        removeObject(objectName);
    }

    public static String getExtensionFromContentType(String contentType) {
        if (contentType == null) {
            return ".mp4";
        }
        return MIME_TO_EXTENSION.getOrDefault(contentType.toLowerCase(),
                contentType.startsWith("video/") ? "." + contentType.split("/")[1] : ".mp4");
    }
}
