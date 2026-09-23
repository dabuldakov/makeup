package com.example.makeup.service;

import com.example.makeup.dto.VideoItem;
import com.example.makeup.entity.User;
import com.example.makeup.entity.Video;
import com.example.makeup.entity.VideoStatus;
import com.example.makeup.exception.NotFoundException;
import com.example.makeup.repository.NewsRepository;
import com.example.makeup.repository.VideoLikeRepository;
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
    private VideoLikeRepository videoLikeRepository;

    @Mock
    private NewsRepository newsRepository;

    @Mock
    private MinioService minioService;

    @Mock
    private UserService userService;

    @Mock
    private MediaJobService mediaJobService;

    @InjectMocks
    private VideoService videoService;

    @Test
    void uploadVideo_shouldUploadFileSaveAndEnqueueThumbnail() {
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

        Video result = videoService.uploadVideo(file, "Title", "Desc", null, "alice");

        assertEquals("cafe0000.mp4", result.getObjectKey());
        assertEquals(VideoStatus.PUBLISHED, result.getStatus());
        verify(mediaJobService).enqueue(42L);
        verify(videoRepository).save(any(Video.class));
    }

    @Test
    void uploadVideo_withClientThumbnail_PublishesImmediatelyAndSkipsQueue() {
        User user = User.builder().id(1L).username("alice").build();
        MockMultipartFile file = new MockMultipartFile("file", "clip.mp4", "video/mp4", new byte[]{1, 2});
        MockMultipartFile thumbnail =
                new MockMultipartFile("thumbnail", "thumb.jpeg", "image/jpeg", new byte[]{3, 4, 5});

        when(userService.getUserByUsername("alice")).thenReturn(user);
        when(minioService.uploadVideo(any(InputStream.class), anyLong(), anyString(), anyString()))
                .thenReturn("cafe0000.mp4");
        when(minioService.uploadThumbnail(any(MockMultipartFile.class), anyString()))
                .thenReturn("thumb-uuid.jpeg");
        when(videoRepository.save(any(Video.class))).thenAnswer(inv -> {
            Video v = inv.getArgument(0);
            v.setId(42L);
            return v;
        });

        Video result = videoService.uploadVideo(file, "Title", "Desc", thumbnail, "alice");

        assertEquals("thumb-uuid.jpeg", result.getThumbnailKey());
        assertEquals(VideoStatus.PUBLISHED, result.getStatus());
        verify(minioService).uploadThumbnail(any(MockMultipartFile.class), anyString());
        verify(mediaJobService, never()).enqueue(anyLong());
        verify(videoRepository).save(any(Video.class));
    }

    @Test
    void uploadVideo_shouldNotSaveWhenUploadFails() {
        MockMultipartFile file = new MockMultipartFile("file", "clip.mp4", "video/mp4", new byte[]{1});
        when(userService.getUserByUsername("alice")).thenReturn(User.builder().id(1L).build());
        when(minioService.uploadVideo(any(InputStream.class), anyLong(), anyString(), anyString()))
                .thenThrow(new RuntimeException("minio down"));

        assertThrows(RuntimeException.class, () -> videoService.uploadVideo(file, "Title", "Desc", null, "alice"));

        verify(videoRepository, never()).save(any(Video.class));
        verify(mediaJobService, never()).enqueue(anyLong());
    }

    @Test
    void getAllVideos_shouldDelegateFindAllProjected() {
        Pageable pageable = PageRequest.of(0, 20);
        VideoItem item = new VideoItem(1L, "t", null, "f.mp4", null, null, null, null, 0L, 0L, null, "alice");
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

    @Test
    void toggleLike_shouldAddLikeAndIncrementCounter() {
        User user = User.builder().id(1L).username("alice").build();
        when(userService.getUserByUsername("alice")).thenReturn(user);
        when(videoRepository.findById(7L)).thenReturn(Optional.of(Video.builder().id(7L).build()));
        when(videoLikeRepository.existsByUserIdAndVideoId(1L, 7L)).thenReturn(false);
        when(videoRepository.findById(7L)).thenReturn(Optional.of(Video.builder().id(7L).likesCount(1L).build()));

        long likes = videoService.toggleLike(7L, "alice");

        assertEquals(1L, likes);
        verify(videoLikeRepository).save(any());
        verify(videoRepository).adjustLikesCount(7L, 1);
    }

    @Test
    void toggleLike_shouldRemoveExistingLikeAndDecrementCounter() {
        User user = User.builder().id(1L).username("alice").build();
        when(userService.getUserByUsername("alice")).thenReturn(user);
        when(videoLikeRepository.existsByUserIdAndVideoId(1L, 7L)).thenReturn(true);
        when(videoRepository.findById(7L)).thenReturn(Optional.of(Video.builder().id(7L).likesCount(0L).build()));

        long likes = videoService.toggleLike(7L, "alice");

        assertEquals(0L, likes);
        verify(videoLikeRepository).deleteByUserIdAndVideoId(1L, 7L);
        verify(videoRepository).adjustLikesCount(7L, -1);
    }
}
