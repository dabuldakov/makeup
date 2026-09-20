package com.example.makeup.service;

import com.example.makeup.dto.VideoItem;
import com.example.makeup.entity.User;
import com.example.makeup.entity.Video;
import com.example.makeup.exception.NotFoundException;
import com.example.makeup.repository.VideoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VideoServiceTest {

    @Mock
    private VideoRepository videoRepository;

    @Mock
    private MinioService minioService;

    @Mock
    private UserService userService;

    @Mock
    private ThumbnailProcessor thumbnailProcessor;

    @InjectMocks
    private VideoService videoService;

    @Test
    void uploadVideo_shouldUploadFileSaveAndScheduleThumbnail() {
        User user = User.builder().id(1L).username("alice").build();
        MockMultipartFile file = new MockMultipartFile("file", "clip.mp4", "video/mp4", new byte[]{1, 2});

        when(userService.getUserByUsername("alice")).thenReturn(user);
        when(minioService.uploadVideo(any(InputStream.class), anyLong(), anyString(), anyString()))
                .thenReturn("cafe0000.mp4");
        when(videoRepository.save(any(Video.class))).thenAnswer(inv -> {
            Video v = inv.getArgument(0);
            v.setId(42L);
            return v;
        });

        Video result = videoService.uploadVideo(file, "Title", "Desc", "alice");

        assertEquals("cafe0000.mp4", result.getFileName());
        verify(thumbnailProcessor).generateAndAttach(anyLong(), any(Path.class));
        verify(videoRepository).save(any(Video.class));
    }

    @Test
    void uploadVideo_shouldNotCallMinioWhenUploadFails() {
        MockMultipartFile file = new MockMultipartFile("file", "clip.mp4", "video/mp4", new byte[]{1});
        when(userService.getUserByUsername("alice")).thenReturn(User.builder().id(1L).build());
        when(minioService.uploadVideo(any(InputStream.class), anyLong(), anyString(), anyString()))
                .thenThrow(new RuntimeException("minio down"));

        assertThrows(RuntimeException.class, () -> videoService.uploadVideo(file, "Title", "Desc", "alice"));

        verify(videoRepository, never()).save(any(Video.class));
        verify(thumbnailProcessor, never()).generateAndAttach(anyLong(), any(Path.class));
    }

    @Test
    void getAllVideos_shouldDelegateFindAllProjected() {
        Pageable pageable = PageRequest.of(0, 20);
        VideoItem item = new VideoItem(1L, "t", null, "f.mp4", null, null, null, null, 0, 0, null, "alice");
        Page<VideoItem> page = new PageImpl<>(List.of(item));
        when(videoRepository.findAllProjected(pageable)).thenReturn(page);

        assertEquals(page, videoService.getAllVideos(pageable));
    }

    @Test
    void getVideoById_shouldThrowWhenAbsent() {
        when(videoRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> videoService.getVideoById(1L));
    }

    @Test
    void incrementViews_shouldUseAtomicQuery() {
        when(videoRepository.incrementViews(1L)).thenReturn(1);

        videoService.incrementViews(1L);

        verify(videoRepository).incrementViews(1L);
    }

    @Test
    void incrementViews_shouldThrowWhenVideoMissing() {
        when(videoRepository.incrementViews(1L)).thenReturn(0);

        assertThrows(NotFoundException.class, () -> videoService.incrementViews(1L));
    }
}