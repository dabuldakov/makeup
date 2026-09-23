package com.example.makeup.news;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

import com.example.makeup.video.VideoResponse;
@Data
@Builder
public class NewsResponse {
    private Long id;
    private String title;
    private String content;
    private String imageUrl;
    private VideoResponse relatedVideo;
    private String author;
    private LocalDateTime publishedAt;
}