package com.example.makeup.service;

import com.example.makeup.entity.User;
import com.example.makeup.entity.Video;
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

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
    private ThumbnailGeneratorService thumbnailGeneratorService;

    @InjectMocks
    private VideoService videoService;

    @Test
    void uploadVideo_shouldUploadFileSaveAndGenerateThumbnail() {
        User user = User.builder().id(1L).username("alice").build();
        MockMultipartFile file = new MockMultipartFile("file", "clip.mp4", "video/mp4", new byte[]{1, 2});

        when(userService.getUserByUsername("alice")).thenReturn(user);
        when(minioService.uploadVideo(any(), anyString())).thenReturn("cafe0000");
        when(videoRepository.save(any(Video.class))).thenAnswer(inv -> inv.getArgument(0));
        when(thumbnailGeneratorService.generateThumbnail(any()))
                .thenReturn(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB));
        when(minioService.uploadThumbnail(any(), anyString())).thenReturn("cafe0000.jpeg");

        Video result = videoService.uploadVideo(file, "Title", "Desc", "alice");

        assertEquals("thumbnails/cafe0000.jpeg", result.getThumbnailPath());
        verify(minioService).uploadThumbnail(any(), anyString());
    }

    @Test
    void uploadVideo_shouldStillSaveWhenThumbnailFails() {
        User user = User.builder().id(1L).username("alice").build();
        Video saved = Video.builder().id(1L).build();
        MockMultipartFile file = new MockMultipartFile("file", "clip.mp4", "video/mp4", new byte[]{1});

        when(userService.getUserByUsername("alice")).thenReturn(user);
        when(minioService.uploadVideo(any(), anyString())).thenReturn("cafe0000");
        when(videoRepository.save(any(Video.class))).thenReturn(saved);
        when(thumbnailGeneratorService.generateThumbnail(any()))
                .thenThrow(new RuntimeException("thumbnail gen failed"));

        Video result = videoService.uploadVideo(file, "Title", "Desc", "alice");

        assertEquals(saved, result);
        verify(minioService, never()).uploadThumbnail(any(), anyString());
    }

    @Test
    void getAllVideos_shouldDelegateFindAll() {
        Pageable pageable = PageRequest.of(0, 20);
        Video video = Video.builder().id(1L).build();
        Page<Video> page = new PageImpl<>(List.of(video));
        when(videoRepository.findAll(pageable)).thenReturn(page);

        assertEquals(page, videoService.getAllVideos(pageable));
    }

    @Test
    void getVideoById_shouldThrowWhenAbsent() {
        when(videoRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> videoService.getVideoById(1L));
    }

    @Test
    void incrementViews_shouldIncrementAndSave() {
        Video video = Video.builder().id(1L).views(10).build();
        when(videoRepository.findById(1L)).thenReturn(Optional.of(video));
        when(videoRepository.save(any(Video.class))).thenAnswer(inv -> inv.getArgument(0));

        videoService.incrementViews(1L);

        assertEquals(11, video.getViews());
        verify(videoRepository).save(video);
    }
}