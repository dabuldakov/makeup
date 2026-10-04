package com.example.makeup.media;

import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VideoStorageTest {

    private final MinioClient client = mock(MinioClient.class);
    private final MinioClient presignedClient = mock(MinioClient.class);
    private VideoStorage storage;

    @BeforeEach
    void setUp() {
        MinioConfig config = new MinioConfig();
        config.setBucket("videos");
        storage = new VideoStorage(client, presignedClient, config);
    }

    @Test
    void extensionFromContentTypeHandlesKnownUnknownAndNull() {
        assertThat(VideoStorage.getExtensionFromContentType("video/mp4")).isEqualTo(".mp4");
        assertThat(VideoStorage.getExtensionFromContentType("video/quicktime")).isEqualTo(".mov");
        assertThat(VideoStorage.getExtensionFromContentType("video/x-custom")).isEqualTo(".x-custom");
        assertThat(VideoStorage.getExtensionFromContentType(null)).isEqualTo(".mp4");
        assertThat(VideoStorage.getExtensionFromContentType("application/octet-stream")).isEqualTo(".mp4");
    }

    @Test
    void uploadsStreamAndCreatesBucketWhenMissing() throws Exception {
        when(client.bucketExists(any())).thenReturn(false);

        try (InputStream in = new ByteArrayInputStream(new byte[]{1, 2, 3})) {
            assertThat(storage.upload(in, 3, "video/mp4", "clip-1")).isEqualTo("clip-1.mp4");
        }

        verify(client).makeBucket(any());
        verify(client).putObject(any(PutObjectArgs.class));
    }

    @Test
    void getAndGetRangeReadObjects() throws Exception {
        when(client.bucketExists(any())).thenReturn(true);
        when(client.getObject(any(GetObjectArgs.class))).thenReturn(mock(io.minio.GetObjectResponse.class));

        assertThat(storage.get("clip.mp4")).isNotNull();
        assertThat(storage.get("clip.mp4", 10, 20)).isNotNull();

        verify(client, times(2)).getObject(any(GetObjectArgs.class));
    }

    @Test
    void statDelegatesToClient() throws Exception {
        StatObjectResponse response = mock(StatObjectResponse.class);
        when(client.statObject(any(StatObjectArgs.class))).thenReturn(response);

        assertThat(storage.stat("clip.mp4")).isSameAs(response);
    }

    @Test
    void presignedUrlUsesPresignedClient() throws Exception {
        when(presignedClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenReturn("https://minio/videos/clip.mp4");

        assertThat(storage.presignedUrl("clip.mp4")).isEqualTo("https://minio/videos/clip.mp4");
    }

    @Test
    void deleteRemovesObject() throws Exception {
        storage.delete("clip.mp4");

        verify(client).removeObject(any(RemoveObjectArgs.class));
    }

    @Test
    void uploadFailureIsWrapped() throws Exception {
        when(client.bucketExists(any())).thenReturn(true);
        when(client.putObject(any(PutObjectArgs.class))).thenThrow(new RuntimeException("s3 down"));

        assertThatThrownBy(() -> storage.upload(new ByteArrayInputStream(new byte[]{1}), 1, "video/mp4", "x"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to store object");
    }
}
