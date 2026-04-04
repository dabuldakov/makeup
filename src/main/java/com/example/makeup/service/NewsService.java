package com.example.makeup.service;

import com.example.makeup.entity.NewsItem;
import com.example.makeup.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NewsService {

    private final NewsRepository newsRepository;
    private final VideoService videoService;
    private final UserService userService;

    public Page<NewsItem> getAllNews(Pageable pageable) {
        return newsRepository.findByIsPublishedTrueOrderByPublishedAtDesc(pageable);
    }

    public NewsItem getNewsById(Long id) {
        return newsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("News not found"));
    }

    public NewsItem createNews(String title, String content, Long videoId, String username) {
        NewsItem news = NewsItem.builder()
                .title(title)
                .content(content)
                .author(userService.getUserByUsername(username))
                .relatedVideo(videoId != null ? videoService.getVideoById(videoId) : null)
                .isPublished(true)
                .build();

        return newsRepository.save(news);
    }
}