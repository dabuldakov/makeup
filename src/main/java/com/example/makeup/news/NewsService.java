package com.example.makeup.news;

import com.example.makeup.media.BucketType;
import com.example.makeup.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import com.example.makeup.auth.UserService;
import com.example.makeup.media.MinioService;
import com.example.makeup.video.VideoService;
@Service
@RequiredArgsConstructor
@Slf4j
public class NewsService {

    private final NewsRepository newsRepository;
    private final VideoService videoService;
    private final UserService userService;
    private final MinioService minioService;

    public Page<NewsListItem> getAllNews(Pageable pageable) {
        log.debug("Fetching news list (page: {}, size: {})", pageable.getPageNumber(), pageable.getPageSize());
        return newsRepository.findAllPublished(pageable);
    }

    public NewsItem getNewsById(Long id) {
        return newsRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new NotFoundException("News not found"));
    }

    @Transactional
    public Long createNews(String title, String content, Long videoId, String username, MultipartFile image) {
        NewsItem news = NewsItem.builder()
                .title(title)
                .content(content)
                .author(userService.getUserByUsername(username))
                .relatedVideo(videoId != null ? videoService.getVideoById(videoId) : null)
                .status(NewsStatus.PUBLISHED)
                .publishedAt(LocalDateTime.now(ZoneOffset.UTC))
                .build();

        NewsItem savedNews = newsRepository.save(news);

        uploadImage(image, savedNews);

        log.info("News created: id={}, title={}, by user={}", savedNews.getId(), title, username);
        return savedNews.getId();
    }

    /**
     * Мягкое удаление: помечаем deleted_at (запись остаётся для аудита),
     * изображение удаляем из хранилища.
     */
    @Transactional
    public void deleteNews(Long id, String username) {
        NewsItem news = getNewsById(id);
        if (news.getAuthor() == null || !username.equals(news.getAuthor().getUsername())) {
            throw new AccessDeniedException("Only the author can delete this news");
        }

        if (news.getImageKey() != null) {
            minioService.deleteNewsImage(news.getImageKey());
        }

        news.setDeletedAt(LocalDateTime.now(ZoneOffset.UTC));
        news.setStatus(NewsStatus.ARCHIVED);
        newsRepository.save(news);
        log.info("News soft-deleted: id={}, by user={}", id, username);
    }

    public byte[] getImage(String fileName) {
        return minioService.getImageBytes(fileName, BucketType.NEWS_IMAGE);
    }

    public org.springframework.core.io.Resource getImageFile(String fileName) {
        return minioService.getImageFile(fileName, BucketType.NEWS_IMAGE);
    }

    private void uploadImage(MultipartFile image, NewsItem savedNews) {
        if (image != null && !image.isEmpty()) {
            try {
                String fileId = UUID.randomUUID().toString();
                String imageKey = minioService.uploadNewsImage(image, fileId);
                savedNews.setImageKey(imageKey);
                newsRepository.save(savedNews);
            } catch (Exception e) {
                log.error("Failed to upload news image: {}", e.getMessage(), e);
            }
        }
    }
}
