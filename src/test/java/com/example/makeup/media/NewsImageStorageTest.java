package com.example.makeup.media;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NewsImageStorageTest {

    private final MinioClient client = mock(MinioClient.class);
    private final MinioClient presignedClient = mock(MinioClient.class);
    private NewsImageStorage storage;

    @BeforeEach
    void setUp() throws Exception {
        MinioConfig config = new MinioConfig();
        config.setNewsImage("news-image");
        storage = new NewsImageStorage(client, presignedClient, config);
        when(client.bucketExists(any())).thenReturn(true);
    }

    @Test
    void uploadsImage() throws Exception {
        MockMultipartFile file = new MockMultipartFile("image", "n.jpg", "image/jpeg", new byte[]{1, 2});

        assertThat(storage.upload(file, "news-1")).isEqualTo("news-1.jpeg");
        verify(client).putObject(any(PutObjectArgs.class));
    }

    @Test
    void getReadsObject() throws Exception {
        when(client.getObject(any(GetObjectArgs.class))).thenReturn(mock(io.minio.GetObjectResponse.class));

        assertThat(storage.get("news-1.jpeg")).isNotNull();
    }

    @Test
    void deleteRemovesObject() throws Exception {
        storage.delete("news-1.jpeg");

        verify(client).removeObject(any(RemoveObjectArgs.class));
    }
}
