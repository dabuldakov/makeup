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
    public ResponseEntity<Page<NewsItem>> getAllNews(Pageable pageable) {
        return ResponseEntity.ok(newsService.getAllNews(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<NewsItem> getNewsById(@PathVariable Long id) {
        return ResponseEntity.ok(newsService.getNewsById(id));
    }

    @PostMapping
    public ResponseEntity<NewsItem> createNews(
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam(required = false) Long videoId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(newsService.createNews(title, content, videoId, authentication.getName()));
    }
}