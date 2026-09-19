package com.example.makeup.dto.mapper;

import com.example.makeup.dto.response.NewsResponse;
import com.example.makeup.entity.NewsItem;
import org.springframework.stereotype.Component;

@Component
public class NewsMapper {

    private final VideoMapper videoMapper;

    public NewsMapper(VideoMapper videoMapper) {
        this.videoMapper = videoMapper;
    }

    public NewsResponse toResponse(NewsItem newsItem) {
        return NewsResponse.builder()
                .id(newsItem.getId())
                .title(newsItem.getTitle())
                .content(newsItem.getContent())
                .imageUrl(newsItem.getImageUrl() != null ? "/api/news/image/" + newsItem.getImageUrl() : null)
                .relatedVideo(videoMapper.toResponse(newsItem.getRelatedVideo()))
                .publishedAt(newsItem.getPublishedAt())
                .author(newsItem.getAuthor().getUsername())
                .build();
    }
}