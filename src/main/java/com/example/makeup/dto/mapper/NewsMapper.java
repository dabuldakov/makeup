package com.example.makeup.dto.mapper;

import com.example.makeup.dto.response.NewsResponse;
import com.example.makeup.entity.NewsItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class NewsMapper {

    private final VideoMapper videoMapper;
    private final String publicBaseUrl;

    public NewsMapper(VideoMapper videoMapper,
                      @Value("${app.public-base-url:http://localhost:8080}") String publicBaseUrl) {
        this.videoMapper = videoMapper;
        this.publicBaseUrl = publicBaseUrl;
    }

    public NewsResponse toResponse(NewsItem newsItem) {
        if (newsItem == null) {
            return null;
        }
        return NewsResponse.builder()
                .id(newsItem.getId())
                .title(newsItem.getTitle())
                .content(newsItem.getContent())
                .imageUrl(newsItem.getImageUrl() != null
                        ? publicBaseUrl + "/api/news/image/" + newsItem.getImageUrl()
                        : null)
                .relatedVideo(videoMapper.toResponse(newsItem.getRelatedVideo()))
                .publishedAt(newsItem.getPublishedAt())
                .author(newsItem.getAuthor() != null ? newsItem.getAuthor().getUsername() : null)
                .build();
    }
}