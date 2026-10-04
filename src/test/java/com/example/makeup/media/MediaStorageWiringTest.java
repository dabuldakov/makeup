package com.example.makeup.media;

import io.minio.MinioClient;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Проверяет, что хранилища корректно собираются Spring'ом при двух бинах
 * {@link MinioClient} (обычный и presigned). Регрессия: без {@code @Qualifier}
 * контекст падал с «expected single matching bean but found 2». Интеграционные
 * тесты этого не ловят, т.к. хранилища там замоканы.
 */
class MediaStorageWiringTest {

    @Test
    void storagesWireWithTwoMinioClients() {
        MinioConfig config = new MinioConfig();
        config.setBucket("videos");
        config.setThumbnailBucket("thumbnails");
        config.setNewsImage("news-image");

        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext()) {
            ctx.registerBean("minioClient", MinioClient.class, () -> mock(MinioClient.class));
            ctx.registerBean("minioClientForPresignedUrls", MinioClient.class, () -> mock(MinioClient.class));
            ctx.registerBean(MinioConfig.class, () -> config);
            ctx.register(VideoStorage.class, ThumbnailStorage.class, NewsImageStorage.class);
            ctx.refresh();

            assertThat(ctx.getBean(VideoStorage.class)).isNotNull();
            assertThat(ctx.getBean(ThumbnailStorage.class)).isNotNull();
            assertThat(ctx.getBean(NewsImageStorage.class)).isNotNull();
        }
    }
}
