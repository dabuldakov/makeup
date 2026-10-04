package com.example.makeup.media;

import io.minio.MinioClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "minio")
public class MinioConfig {
    private String url;
    private String externalUrl;
    private String accessKey;
    private String secretKey;
    /** Bucket видео (minio.bucket). */
    private String bucket;
    /** Bucket превью (minio.thumbnail-bucket). */
    private String thumbnailBucket;
    /** Bucket изображений новостей (minio.news-image). */
    private String newsImage;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(url)
                .credentials(accessKey, secretKey)
                .build();
    }

    @Bean
    public MinioClient minioClientForPresignedUrls() {
        return MinioClient.builder()
                .endpoint(externalUrl)
                .credentials(accessKey, secretKey)
                .build();
    }
}
