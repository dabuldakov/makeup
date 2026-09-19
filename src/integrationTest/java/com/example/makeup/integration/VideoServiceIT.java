package com.example.makeup.integration;

import com.example.makeup.entity.User;
import com.example.makeup.entity.Video;
import com.example.makeup.repository.VideoRepository;
import com.example.makeup.service.VideoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Интеграционные тесты видео: загрузка (метаданные сохраняются), пагинация,
 * просмотры, ошибки. MinIO и генерация превью замоканы.
 */
class VideoServiceIT extends AbstractIntegrationTest {

    @Autowired
    private VideoService videoService;

    @Autowired
    private VideoRepository videoRepository;

    @Test
    void uploadVideoPersistsMetadata() {
        User author = createUser("author1");

        Video video = videoService.uploadVideo(videoFile(), "My Video", "My description", author.getUsername());

        Video saved = videoRepository.findById(video.getId()).orElseThrow();
        assertThat(saved.getTitle()).isEqualTo("My Video");
        assertThat(saved.getDescription()).isEqualTo("My description");
        assertThat(saved.getFileName()).isNotBlank();
        assertThat(saved.getFilePath()).startsWith("videos/");
        assertThat(saved.getContentType()).isEqualTo("video/mp4");
        assertThat(saved.getFileSize()).isEqualTo(4L);
        assertThat(saved.getViews()).isZero();
        assertThat(saved.getThumbnailPath()).startsWith("thumbnails/");
        assertThat(saved.getUploadedBy().getUsername()).isEqualTo(author.getUsername());
    }

    @Test
    void uploadVideoWithUnknownUserFails() {
        assertThatThrownBy(() -> videoService.uploadVideo(videoFile(), "T", "d", "missing-user"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to upload video");
    }

    @Test
    void getAllVideosPaginates() {
        User author = createUser("author2");
        videoService.uploadVideo(videoFile(), "Video 1", "d", author.getUsername());
        videoService.uploadVideo(videoFile(), "Video 2", "d", author.getUsername());

        Page<Video> page = videoService.getAllVideos(PageRequest.of(0, 1));

        assertThat(page.getTotalElements()).isEqualTo(2L);
        assertThat(page.getContent()).hasSize(1);
    }

    @Test
    void getVideoByIdReturnsUploadedVideo() {
        User author = createUser("author3");
        Video video = videoService.uploadVideo(videoFile(), "Video title", "d", author.getUsername());

        Video found = videoService.getVideoById(video.getId());

        assertThat(found.getTitle()).isEqualTo("Video title");
    }

    @Test
    void getVideoByIdThrowsForUnknownId() {
        assertThatThrownBy(() -> videoService.getVideoById(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Video not found");
    }

    @Test
    void incrementViewsIncrementsStoredCounter() {
        User author = createUser("author4");
        Video video = videoService.uploadVideo(videoFile(), "Video title", "d", author.getUsername());

        videoService.incrementViews(video.getId());
        videoService.incrementViews(video.getId());

        Video reloaded = videoRepository.findById(video.getId()).orElseThrow();
        assertThat(reloaded.getViews()).isEqualTo(2);
    }

    @Test
    void uploadVideoSurvivesThumbnailGenerationFailure() {
        User author = createUser("author5");
        when(thumbnailGeneratorService.generateThumbnail(any()))
                .thenThrow(new RuntimeException("ffmpeg unavailable"));

        Video video = videoService.uploadVideo(videoFile(), "Video title", "d", author.getUsername());

        Video reloaded = videoRepository.findById(video.getId()).orElseThrow();
        assertThat(reloaded.getThumbnailPath()).isNull();
        assertThat(reloaded.getTitle()).isEqualTo("Video title");
    }
}