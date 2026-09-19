package com.example.makeup.integration;

import com.example.makeup.entity.NewsItem;
import com.example.makeup.entity.User;
import com.example.makeup.entity.Video;
import com.example.makeup.repository.NewsRepository;
import com.example.makeup.repository.VideoRepository;
import com.example.makeup.service.NewsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Интеграционные тесты новостей: создание (с видео и изображением),
 * публикация/скрытие, пагинация. MinIO замокан.
 */
class NewsServiceIT extends AbstractIntegrationTest {

    @Autowired
    private NewsService newsService;

    @Autowired
    private NewsRepository newsRepository;

    @Autowired
    private VideoRepository videoRepository;

    private Video createVideo(User author) {
        return videoRepository.save(Video.builder()
                .title("Video title")
                .description("desc")
                .fileName("video-file.mp4")
                .filePath("videos/video-file.mp4")
                .contentType("video/mp4")
                .fileSize(1024L)
                .uploadedBy(author)
                .views(0)
                .build());
    }

    @Test
    void createNewsWithoutImagePersistsPublishedNews() {
        User author = createUser("author1");
        String username = author.getUsername();

        Long id = newsService.createNews("News title", "News content", null, username, null);

        NewsItem news = newsService.getNewsById(id);
        assertThat(news.getTitle()).isEqualTo("News title");
        assertThat(news.getAuthor().getUsername()).isEqualTo(username);
        assertThat(news.isPublished()).isTrue();
        assertThat(news.getImageUrl()).isNull();
    }

    @Test
    void createNewsWithImageSetsImageUrl() {
        User author = createUser("author2");

        Long id = newsService.createNews("With image", "content", null, author.getUsername(), imageFile());

        NewsItem news = newsService.getNewsById(id);
        assertThat(news.getImageUrl()).isNotBlank().endsWith(".jpeg");
    }

    @Test
    void createNewsWithRelatedVideoLinksVideo() {
        User author = createUser("author3");
        Video video = createVideo(author);

        Long id = newsService.createNews("With video", "content", video.getId(), author.getUsername(), null);

        NewsItem news = newsService.getNewsById(id);
        assertThat(news.getRelatedVideo()).isNotNull();
        assertThat(news.getRelatedVideo().getId()).isEqualTo(video.getId());
    }

    @Test
    void getAllNewsReturnsOnlyPublishedNews() {
        User author = createUser("author4");
        newsService.createNews("Published 1", "content", null, author.getUsername(), null);
        newsService.createNews("Published 2", "content", null, author.getUsername(), null);

        NewsItem hidden = NewsItem.builder()
                .title("Hidden")
                .content("content")
                .author(author)
                .isPublished(false)
                .build();
        newsRepository.save(hidden);

        Page<NewsItem> page = newsService.getAllNews(PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2L);
        List<String> titles = page.getContent().stream().map(NewsItem::getTitle).toList();
        assertThat(titles).containsExactlyInAnyOrder("Published 1", "Published 2");
    }

    @Test
    void getAllNewsPaginates() {
        User author = createUser("author5");
        newsService.createNews("News 1", "content", null, author.getUsername(), null);
        newsService.createNews("News 2", "content", null, author.getUsername(), null);

        Page<NewsItem> page = newsService.getAllNews(PageRequest.of(0, 1));

        assertThat(page.getTotalElements()).isEqualTo(2L);
        assertThat(page.getContent()).hasSize(1);
    }

    @Test
    void getNewsByIdThrowsForUnknownId() {
        assertThatThrownBy(() -> newsService.getNewsById(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("News not found");
    }
}