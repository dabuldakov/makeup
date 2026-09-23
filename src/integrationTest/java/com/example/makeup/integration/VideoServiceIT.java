package com.example.makeup.integration;

import com.example.makeup.dto.VideoItem;
import com.example.makeup.entity.User;
import com.example.makeup.entity.Video;
import com.example.makeup.entity.VideoStatus;
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
import static org.mockito.Mockito.verify;
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

        Video video = videoService.uploadVideo(videoFile(), "My Video", "My description", null, author.getUsername());

        Video saved = videoRepository.findById(video.getId()).orElseThrow();
        assertThat(saved.getTitle()).isEqualTo("My Video");
        assertThat(saved.getDescription()).isEqualTo("My description");
        assertThat(saved.getObjectKey()).isNotBlank();
        assertThat(saved.getContentType()).isEqualTo("video/mp4");
        assertThat(saved.getFileSize()).isEqualTo(4L);
        assertThat(saved.getViews()).isZero();
        assertThat(saved.getStatus()).isEqualTo(VideoStatus.PUBLISHED);
        assertThat(saved.getUploadedBy().getUsername()).isEqualTo(author.getUsername());
    }

    @Test
    void toggleLikeAddsAndRemovesLikeAndUpdatesCounter() {
        User author = createUser("like-author");
        Video video = videoService.uploadVideo(videoFile(), "Like me", "d", null, author.getUsername());

        assertThat(videoService.toggleLike(video.getId(), author.getUsername())).isEqualTo(1L);
        assertThat(videoService.isLikedBy(video.getId(), author.getUsername())).isTrue();
        assertThat(videoRepository.findById(video.getId()).orElseThrow().getLikesCount()).isEqualTo(1L);

        assertThat(videoService.toggleLike(video.getId(), author.getUsername())).isZero();
        assertThat(videoService.isLikedBy(video.getId(), author.getUsername())).isFalse();
    }

    @Test
    void uploadVideoWithUnknownUserFails() {
        assertThatThrownBy(() -> videoService.uploadVideo(videoFile(), "T", "d", null, "missing-user"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to upload video");
    }

    @Test
    void getAllVideosPaginates() {
        User author = createUser("author2");
        videoService.uploadVideo(videoFile(), "Video 1", "d", null, author.getUsername());
        videoService.uploadVideo(videoFile(), "Video 2", "d", null, author.getUsername());

        Page<VideoItem> page = videoService.getAllVideos(PageRequest.of(0, 1));

        assertThat(page.getTotalElements()).isEqualTo(2L);
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).uploadedByUsername()).isEqualTo(author.getUsername());
    }

    @Test
    void getVideoByIdReturnsUploadedVideo() {
        User author = createUser("author3");
        Video video = videoService.uploadVideo(videoFile(), "Video title", "d", null, author.getUsername());

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
        Video video = videoService.uploadVideo(videoFile(), "Video title", "d", null, author.getUsername());

        videoService.incrementViews(video.getId());
        videoService.incrementViews(video.getId());

        Video reloaded = videoRepository.findById(video.getId()).orElseThrow();
        assertThat(reloaded.getViews()).isEqualTo(2);
    }

    @Test
    void getAllVideosSortsByCreationTimeDescending() {
        User author = createUser("sort-author");
        Video first = videoService.uploadVideo(videoFile(), "Older video", "d", null, author.getUsername());
        Video second = videoService.uploadVideo(videoFile(), "Newer video", "d", null, author.getUsername());

        Page<VideoItem> page = videoService.getAllVideos(PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent().get(0).id()).isEqualTo(second.getId());
        assertThat(page.getContent().get(1).id()).isEqualTo(first.getId());
    }

    @Test
    void deleteVideoRemovesVideoAndFiles() {
        User author = createUser("delete-author");
        Video video = videoService.uploadVideo(
                videoFile(), "To delete", "d",
                new MockMultipartFile("thumbnail", "p.jpeg", "image/jpeg", new byte[]{1, 2, 3}),
                author.getUsername());

        Video saved = videoRepository.findById(video.getId()).orElseThrow();
        videoService.deleteVideo(video.getId(), author.getUsername());

        assertThat(videoRepository.findById(video.getId())).isEmpty();
        verify(minioService).deleteVideo(saved.getObjectKey());
        verify(minioService).deleteThumbnail(saved.getThumbnailKey());
    }

    @Test
    void deleteVideoByNonOwnerFails() {
        User author = createUser("delete-owner");
        User other = createUser("delete-other");
        Video video = videoService.uploadVideo(videoFile(), "Mine", "d", null, author.getUsername());

        assertThatThrownBy(() -> videoService.deleteVideo(video.getId(), other.getUsername()))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThat(videoRepository.findById(video.getId())).isPresent();
    }

    @Test
    void deleteVideoByUnknownIdFails() {
        User author = createUser("delete-unknown");
        assertThatThrownBy(() -> videoService.deleteVideo(999L, author.getUsername()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Video not found");
    }
}