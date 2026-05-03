package com.example.makeup.service;

import com.example.makeup.config.BucketType;
import com.example.makeup.entity.NewsItem;
import com.example.makeup.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsService {

    private final NewsRepository newsRepository;
    private final VideoService videoService;
    private final UserService userService;
    private final MinioService minioService;

    public Page<NewsItem> getAllNews(Pageable pageable) {
        return newsRepository.findByIsPublishedTrueOrderByPublishedAtDesc(pageable);
    }

    public NewsItem getNewsById(Long id) {
        return newsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("News not found"));
    }

    public Long createNews(String title, String content, Long videoId, String username, MultipartFile image) {
        NewsItem news = NewsItem.builder()
                .title(title)
                .content(content)
                .author(userService.getUserByUsername(username))
                .relatedVideo(videoId != null ? videoService.getVideoById(videoId) : null)
                .isPublished(true)
                .build();

        NewsItem savedNews = newsRepository.save(news);

        // Загружаем изображение если оно предоставлено
        uploadImage(image, savedNews);

        return savedNews.getId();
    }

    public byte[] getImage(String fileName) {
        return minioService.getImageBytes(fileName, BucketType.NEWS_IMAGE);
    }

    private void uploadImage(MultipartFile image, NewsItem savedNews) {
        if (image != null && !image.isEmpty()) {
            try {
                String fileId = UUID.randomUUID().toString();
                String imageName = minioService.uploadNewsImage(image, fileId);
                savedNews.setImageUrl(imageName);
                newsRepository.save(savedNews);
            } catch (Exception e) {
                log.error("Failed to upload news image: {}", e.getMessage(), e);
            }
        }
    }
}