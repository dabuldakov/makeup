package com.example.makeup.controller;

import com.example.makeup.dto.mapper.NewsMapper;
import com.example.makeup.dto.response.NewsResponse;
import com.example.makeup.service.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsService newsService;
    private final NewsMapper newsMapper;

    @GetMapping
    public ResponseEntity<Page<NewsResponse>> getAllNews(Pageable pageable) {
        Page<NewsResponse> response = newsService.getAllNews(pageable).map(newsMapper::toResponse);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<NewsResponse> getNewsById(@PathVariable Long id) {
        return ResponseEntity.ok(newsMapper.toResponse(newsService.getNewsById(id)));
    }

    @PostMapping
    public ResponseEntity<NewsResponse> createNews(
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam(required = false) Long videoId,
            @RequestParam(required = false) MultipartFile image,
            Authentication authentication
    ) {
        Long id = newsService.createNews(title, content, videoId, authentication.getName(), image);
        return ResponseEntity.ok(newsMapper.toResponse(newsService.getNewsById(id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNews(@PathVariable Long id, Authentication authentication) {
        newsService.deleteNews(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/image/{fileName}")
    public ResponseEntity<byte[]> getImage(@PathVariable String fileName) {
        byte[] bytes = newsService.getImage(fileName);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(bytes);
    }
}