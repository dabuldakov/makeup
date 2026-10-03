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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MinioServiceTest {

    private final MinioClient client = mock(MinioClient.class);
    private final MinioClient presignedClient = mock(MinioClient.class);
    private MinioService service;

    @BeforeEach
    void setUp() {
        service = new MinioService(client, presignedClient);
        ReflectionTestUtils.setField(service, "videoBucketName", "videos");
        ReflectionTestUtils.setField(service, "thumbnailBucketName", "thumbnails");
        ReflectionTestUtils.setField(service, "newsImageBucketName", "news-image");
    }

    @Test
    void extensionFromContentTypeHandlesKnownUnknownAndNull() {
        assertThat(MinioService.getExtensionFromContentType("video/mp4")).isEqualTo(".mp4");
        assertThat(MinioService.getExtensionFromContentType("video/quicktime")).isEqualTo(".mov");
        assertThat(MinioService.getExtensionFromContentType("video/x-custom")).isEqualTo(".x-custom");
        assertThat(MinioService.getExtensionFromContentType(null)).isEqualTo(".mp4");
        assertThat(MinioService.getExtensionFromContentType("application/octet-stream")).isEqualTo(".mp4");
    }

    @Test
    void uploadsVideoStream() throws Exception {
        try (InputStream in = new ByteArrayInputStream(new byte[]{1, 2, 3})) {
            String object = service.uploadVideo(in, 3, "video/mp4", "clip-1");
            assertThat(object).isEqualTo("clip-1.mp4");
        }
        verify(client).putObject(any(PutObjectArgs.class));
    }

    @Test
    void uploadsThumbnailImage() throws Exception {
        BufferedImage image = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);

        assertThat(service.uploadThumbnail(image, "clip-1")).isEqualTo("clip-1.jpeg");
        verify(client).putObject(any(PutObjectArgs.class));
    }

    @Test
    void uploadsClientThumbnailAndNewsImage() throws Exception {
        MockMultipartFile file = new MockMultipartFile("f", "t.jpg", "image/jpeg", new byte[]{1, 2});

        assertThat(service.uploadThumbnail(file, "clip-1")).isEqualTo("clip-1.jpeg");
        assertThat(service.uploadNewsImage(file, "news-1")).isEqualTo("news-1.jpeg");
        verify(client, org.mockito.Mockito.times(2)).putObject(any(PutObjectArgs.class));
    }

    @Test
    void getsVideoFileAndRange() throws Exception {
        when(client.getObject(any(GetObjectArgs.class)))
                .thenReturn(mock(io.minio.GetObjectResponse.class));

        assertThat(service.getVideoFile("clip.mp4")).isNotNull();
        assertThat(service.getVideoFile("clip.mp4", 10, 20)).isNotNull();

        verify(client, org.mockito.Mockito.times(2)).getObject(any(GetObjectArgs.class));
    }

    @Test
    void statsVideo() throws Exception {
        StatObjectResponse response = mock(StatObjectResponse.class);
        when(client.statObject(any(StatObjectArgs.class))).thenReturn(response);

        assertThat(service.statVideo("clip.mp4")).isSameAs(response);
    }

    @Test
    void generatesPresignedUrls() throws Exception {
        when(presignedClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenReturn("https://minio/videos/clip.mp4");

        assertThat(service.getVideoPresignedUrl("clip.mp4")).isEqualTo("https://minio/videos/clip.mp4");
    }

    @Test
    void deletesObjects() throws Exception {
        service.deleteVideo("clip.mp4");
        service.deleteThumbnail("clip.jpeg");
        service.deleteNewsImage("news.jpeg");

        verify(client, org.mockito.Mockito.times(3)).removeObject(any(RemoveObjectArgs.class));
    }

    @Test
    void uploadFailureIsWrapped() throws Exception {
        when(client.putObject(any(PutObjectArgs.class))).thenThrow(new RuntimeException("s3 down"));

        assertThatThrownBy(() -> service.uploadVideo(new ByteArrayInputStream(new byte[]{1}), 1, "video/mp4", "x"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to upload video");
    }
}
