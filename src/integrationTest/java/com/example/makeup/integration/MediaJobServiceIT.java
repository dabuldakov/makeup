package com.example.makeup.integration;

import com.example.makeup.entity.MediaJobStatus;
import com.example.makeup.entity.User;
import com.example.makeup.entity.Video;
import com.example.makeup.entity.VideoStatus;
import com.example.makeup.repository.MediaJobRepository;
import com.example.makeup.repository.VideoRepository;
import com.example.makeup.service.MediaJobService;
import com.example.makeup.service.VideoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.awt.image.BufferedImage;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Пайплайн фоновой обработки: загрузка без превью ставит задачу в очередь,
 * воркер генерирует превью и публикует видео.
 */
class MediaJobServiceIT extends AbstractIntegrationTest {

    @Autowired
    private VideoService videoService;
    @Autowired
    private MediaJobService mediaJobService;
    @Autowired
    private MediaJobRepository mediaJobRepository;
    @Autowired
    private VideoRepository videoRepository;

    @Test
    void uploadWithoutThumbnailQueuesJobAndWorkerAttachesThumbnail() {
        when(thumbnailGeneratorService.generateThumbnail(any(Path.class)))
                .thenReturn(new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB));

        User author = createUser("media-author");
        Video video = videoService.uploadVideo(videoFile(), "Queued", "d", null, author.getUsername());

        assertThat(mediaJobRepository.countByStatus(MediaJobStatus.PENDING)).isEqualTo(1);

        int processed = mediaJobService.processPendingBatch(5);

        assertThat(processed).isEqualTo(1);
        Video reloaded = videoRepository.findById(video.getId()).orElseThrow();
        assertThat(reloaded.getThumbnailKey()).isNotBlank();
        assertThat(reloaded.getStatus()).isEqualTo(VideoStatus.PUBLISHED);
        assertThat(mediaJobRepository.countByStatus(MediaJobStatus.PENDING)).isZero();
        assertThat(mediaJobRepository.countByStatus(MediaJobStatus.DONE)).isEqualTo(1);
    }
}
