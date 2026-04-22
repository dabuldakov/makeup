package com.example.makeup.controller;

import com.example.makeup.dto.response.NewsResponse;
import com.example.makeup.entity.NewsItem;
import com.example.makeup.service.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsService newsService;

    @GetMapping
    public ResponseEntity<Page<NewsResponse>> getAllNews(Pageable pageable) {
        return ResponseEntity.ok(newsService.getAllNews(pageable).map(this::toDto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<NewsResponse> getNewsById(@PathVariable Long id) {
        return ResponseEntity.ok(toDto(newsService.getNewsById(id)));
    }

    @PostMapping
    public ResponseEntity<NewsResponse> createNews(
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam(required = false) Long videoId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(toDto(newsService.createNews(title, content, videoId, authentication.getName())));
    }

    public NewsResponse toDto(NewsItem newsItem) {
        return NewsResponse.builder()
                .id(newsItem.getId())
                .title(newsItem.getTitle())
                .content(newsItem.getContent())
                .imageUrl(newsItem.getImageUrl())
                .relatedVideo(VideoController.mapToResponse(newsItem.getRelatedVideo()))
                .publishedAt(newsItem.getPublishedAt())
                .author(newsItem.getAuthor().getUsername())
                .build();
    }
}