package com.example.makeup.media;

import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.awt.image.BufferedImage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ThumbnailStorageTest {

    private final MinioClient client = mock(MinioClient.class);
    private final MinioClient presignedClient = mock(MinioClient.class);
    private ThumbnailStorage storage;

    @BeforeEach
    void setUp() throws Exception {
        MinioConfig config = new MinioConfig();
        config.setThumbnailBucket("thumbnails");
        storage = new ThumbnailStorage(client, presignedClient, config);
        when(client.bucketExists(any())).thenReturn(true);
    }

    @Test
    void uploadsGeneratedImage() throws Exception {
        BufferedImage image = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);

        assertThat(storage.upload(image, "clip-1")).isEqualTo("clip-1.jpeg");
        verify(client).putObject(any(PutObjectArgs.class));
    }

    @Test
    void uploadsClientMultipartFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile("f", "t.jpg", "image/jpeg", new byte[]{1, 2});

        assertThat(storage.upload(file, "clip-1")).isEqualTo("clip-1.jpeg");
        verify(client).putObject(any(PutObjectArgs.class));
    }

    @Test
    void getReadsObject() throws Exception {
        when(client.getObject(any(GetObjectArgs.class))).thenReturn(mock(io.minio.GetObjectResponse.class));

        assertThat(storage.get("clip-1.jpeg")).isNotNull();
    }

    @Test
    void presignedUrlUsesPresignedClient() throws Exception {
        when(presignedClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenReturn("https://minio/thumbnails/clip-1.jpeg");

        assertThat(storage.presignedUrl("clip-1.jpeg")).isEqualTo("https://minio/thumbnails/clip-1.jpeg");
    }

    @Test
    void deleteRemovesObject() throws Exception {
        storage.delete("clip-1.jpeg");

        verify(client, times(1)).removeObject(any(RemoveObjectArgs.class));
    }
}
