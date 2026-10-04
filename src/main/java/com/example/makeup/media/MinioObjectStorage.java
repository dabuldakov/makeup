package com.example.makeup.media;

import com.example.makeup.exception.NotFoundException;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;

import java.io.InputStream;

/**
 * Общая механика работы с одним bucket MinIO: загрузка, чтение (в т.ч. Range),
 * метаданные, presigned URL, удаление и ленивое создание bucket.
 *
 * <p>Наследники ({@link VideoStorage}, {@link ThumbnailStorage},
 * {@link NewsImageStorage}) добавляют только доменные методы. Так god-class
 * MinioService разбит по назначению bucket.
 */
@Slf4j
public abstract class MinioObjectStorage {

    /**
     * Регион задаём явно, чтобы клиент не делал сетевой GetBucketLocation при
     * генерации presigned URL (иначе при недоступном внешнем MinIO запрос виснет).
     */
    private static final String PRESIGNED_REGION = "us-east-1";

    protected final MinioClient client;
    protected final MinioClient presignedClient;
    protected final String bucket;

    private volatile boolean bucketVerified;

    protected MinioObjectStorage(MinioClient client, MinioClient presignedClient, String bucket) {
        this.client = client;
        this.presignedClient = presignedClient;
        this.bucket = bucket;
    }

    protected String putObject(InputStream stream, long size, String contentType, String objectName) {
        ensureBucket();
        try {
            client.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .stream(stream, size, -1)
                    .contentType(contentType)
                    .build());
            log.info("Stored object {}/{}", bucket, objectName);
            return objectName;
        } catch (Exception e) {
            log.error("Failed to store object {}/{}: {}", bucket, objectName, e.getMessage(), e);
            throw new IllegalStateException("Failed to store object " + objectName, e);
        }
    }

    protected Resource readResource(String objectName) {
        return new InputStreamResource(stream(objectName, 0, -1));
    }

    protected Resource readResource(String objectName, long offset, long length) {
        return new InputStreamResource(stream(objectName, offset, length));
    }

    protected StatObjectResponse statObject(String objectName) {
        try {
            return client.statObject(StatObjectArgs.builder().bucket(bucket).object(objectName).build());
        } catch (ErrorResponseException e) {
            throw mapNotFound(e, objectName);
        } catch (Exception e) {
            log.error("Failed to stat object {}/{}: {}", bucket, objectName, e.getMessage(), e);
            throw new IllegalStateException("Failed to stat object " + objectName, e);
        }
    }

    protected String presignedObjectUrl(String objectName, int expirySeconds) {
        try {
            return presignedClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucket)
                    .object(objectName)
                    .region(PRESIGNED_REGION)
                    .expiry(expirySeconds)
                    .build());
        } catch (Exception e) {
            log.error("Failed to generate presigned URL for {}/{}: {}", bucket, objectName, e.getMessage(), e);
            throw new IllegalStateException("Failed to generate presigned URL", e);
        }
    }

    protected void removeObject(String objectName) {
        try {
            client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(objectName).build());
            log.info("Deleted object {}/{}", bucket, objectName);
        } catch (Exception e) {
            // Удаление фоновое/очистное: не роняем уже совершённую операцию.
            log.warn("Failed to delete object {}/{}: {}", bucket, objectName, e.getMessage());
        }
    }

    private InputStream stream(String objectName, long offset, long length) {
        GetObjectArgs.Builder builder = GetObjectArgs.builder().bucket(bucket).object(objectName);
        if (offset > 0) {
            builder.offset(offset);
        }
        if (length > 0) {
            builder.length(length);
        }
        try {
            return client.getObject(builder.build());
        } catch (ErrorResponseException e) {
            throw mapNotFound(e, objectName);
        } catch (Exception e) {
            log.error("Failed to read object {}/{}: {}", bucket, objectName, e.getMessage(), e);
            throw new IllegalStateException("Failed to read object " + objectName, e);
        }
    }

    private RuntimeException mapNotFound(ErrorResponseException e, String objectName) {
        if ("NoSuchKey".equals(e.errorResponse().code())) {
            log.warn("Object not found {}/{}", bucket, objectName);
            return new NotFoundException("File not found: " + objectName);
        }
        return new IllegalStateException("Failed to access object " + objectName, e);
    }

    private void ensureBucket() {
        if (bucketVerified) {
            return;
        }
        try {
            if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
                try {
                    client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                    log.info("Created bucket {}", bucket);
                } catch (ErrorResponseException e) {
                    if (!"BucketAlreadyOwnedByYou".equals(e.errorResponse().code())) {
                        throw e;
                    }
                }
            }
            bucketVerified = true;
        } catch (Exception e) {
            log.error("Failed to ensure bucket {}: {}", bucket, e.getMessage(), e);
            throw new IllegalStateException("Failed to ensure bucket " + bucket, e);
        }
    }
}
