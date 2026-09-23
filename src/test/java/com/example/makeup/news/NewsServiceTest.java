package com.example.makeup.news;

import com.example.makeup.auth.User;
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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.makeup.auth.UserService;
import com.example.makeup.media.MinioService;
import com.example.makeup.video.VideoService;
@ExtendWith(MockitoExtension.class)
class NewsServiceTest {

    @Mock
    private NewsRepository newsRepository;

    @Mock
    private VideoService videoService;

    @Mock
    private UserService userService;

    @Mock
    private MinioService minioService;

    @InjectMocks
    private NewsService newsService;

    @Test
    void getAllNews_shouldReturnPublishedNewsDescending() {
        Pageable pageable = PageRequest.of(0, 20);
        NewsListItem item = new NewsListItem(5L, "t", null, null, null, "author",
                null, null, null, null, null, null, null, null, null, null, null, null);
        Page<NewsListItem> page = new PageImpl<>(List.of(item));
        when(newsRepository.findAllPublished(pageable)).thenReturn(page);

        assertEquals(page, newsService.getAllNews(pageable));
    }

    @Test
    void getNewsById_shouldReturnNews() {
        NewsItem news = NewsItem.builder().id(5L).build();
        when(newsRepository.findByIdAndDeletedAtIsNull(5L)).thenReturn(Optional.of(news));

        assertEquals(news, newsService.getNewsById(5L));
    }

    @Test
    void getNewsById_shouldThrowWhenAbsent() {
        when(newsRepository.findByIdAndDeletedAtIsNull(5L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> newsService.getNewsById(5L));
    }

    @Test
    void createNews_shouldSavePublishedNewsWithAuthor() {
        User author = User.builder().id(1L).username("alice").build();
        NewsItem withId = NewsItem.builder().id(10L).build();
        when(userService.getUserByUsername("alice")).thenReturn(author);
        when(newsRepository.save(any(NewsItem.class))).thenReturn(withId);

        Long id = newsService.createNews("Title", "Body", null, "alice", null);

        assertEquals(10L, id);
        verify(newsRepository).save(any(NewsItem.class));
    }

    @Test
    void createNews_shouldUploadAndAttachImage() {
        User author = User.builder().id(1L).username("alice").build();
        NewsItem firstSaved = NewsItem.builder().id(10L).build();
        NewsItem secondSaved = NewsItem.builder().id(10L).imageKey("uuid121212").build();
        MockMultipartFile image = new MockMultipartFile("image", "x.jpg", "image/jpeg", new byte[]{1, 2, 3});

        when(userService.getUserByUsername("alice")).thenReturn(author);
        when(newsRepository.save(any(NewsItem.class))).thenReturn(firstSaved, secondSaved);
        when(minioService.uploadNewsImage(any(), anyString())).thenReturn("uuid121212");

        newsService.createNews("Title", "Body", null, "alice", image);

        verify(minioService).uploadNewsImage(any(), anyString());
        verify(newsRepository, org.mockito.Mockito.times(2)).save(any(NewsItem.class));
    }

    @Test
    void createNews_shouldIgnoreImageUploadFailure() {
        User author = User.builder().id(1L).username("alice").build();
        MockMultipartFile image = new MockMultipartFile("image", "x.jpg", "image/jpeg", new byte[]{1});

        when(userService.getUserByUsername("alice")).thenReturn(author);
        when(newsRepository.save(any(NewsItem.class))).thenReturn(NewsItem.builder().id(10L).build());
        when(minioService.uploadNewsImage(any(), anyString())).thenThrow(new RuntimeException("minio down"));

        Long id = newsService.createNews("Title", "Body", null, "alice", image);

        assertEquals(10L, id);
    }

    @Test
    void deleteNews_shouldSoftDeleteOwnNewsAndRemoveImage() {
        NewsItem news = NewsItem.builder().id(7L).imageKey("img-uuid")
                .author(User.builder().id(1L).username("alice").build()).build();
        when(newsRepository.findByIdAndDeletedAtIsNull(7L)).thenReturn(Optional.of(news));

        newsService.deleteNews(7L, "alice");

        verify(minioService).deleteNewsImage("img-uuid");
        verify(newsRepository).save(news);
        verify(newsRepository, never()).delete(any(NewsItem.class));
        assertNotNull(news.getDeletedAt());
    }

    @Test
    void deleteNews_shouldRejectNonAuthor() {
        NewsItem news = NewsItem.builder().id(7L).imageKey("img-uuid")
                .author(User.builder().id(1L).username("alice").build()).build();
        when(newsRepository.findByIdAndDeletedAtIsNull(7L)).thenReturn(Optional.of(news));

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> newsService.deleteNews(7L, "bob"));

        verify(newsRepository, never()).save(any(NewsItem.class));
        verify(minioService, never()).deleteNewsImage(anyString());
    }

    @Test
    void deleteNews_shouldSkipImageWhenAbsent() {
        NewsItem news = NewsItem.builder().id(7L)
                .author(User.builder().id(1L).username("alice").build()).build();
        when(newsRepository.findByIdAndDeletedAtIsNull(7L)).thenReturn(Optional.of(news));

        newsService.deleteNews(7L, "alice");

        verify(minioService, never()).deleteNewsImage(anyString());
        verify(newsRepository).save(news);
    }

    @Test
    void getImage_shouldDelegateToMinio() {
        when(minioService.getImageBytes("img.jpg", com.example.makeup.media.BucketType.NEWS_IMAGE))
                .thenReturn(new byte[]{1, 2, 3});

        byte[] bytes = newsService.getImage("img.jpg");

        assertEquals(3, bytes.length);
        assertTrue(bytes[0] == 1);
        verify(minioService, never()).getVideoFile(anyString());
    }
}
