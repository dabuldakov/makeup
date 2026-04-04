package com.example.makeup.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NewsResponse {
    private Long id;
    private String title;
    private String content;
    private String imageUrl;
    private VideoResponse relatedVideo;
    private String author;
    private String publishedAt;
}