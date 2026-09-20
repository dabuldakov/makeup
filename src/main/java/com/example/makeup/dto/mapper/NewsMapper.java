package com.example.makeup.dto.mapper;

import com.example.makeup.dto.NewsListItem;
import com.example.makeup.dto.response.NewsResponse;
import com.example.makeup.dto.response.VideoResponse;
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

    public NewsResponse toResponse(NewsListItem item) {
        if (item == null) {
            return null;
        }
        return NewsResponse.builder()
                .id(item.id())
                .title(item.title())
                .content(item.content())
                .imageUrl(item.imageUrl() != null
                        ? publicBaseUrl + "/api/news/image/" + item.imageUrl()
                        : null)
                .relatedVideo(item.hasRelatedVideo() ? toVideoResponse(item) : null)
                .publishedAt(item.publishedAt())
                .author(item.authorUsername())
                .build();
    }

    private VideoResponse toVideoResponse(NewsListItem item) {
        return VideoResponse.builder()
                .id(item.videoId())
                .title(item.videoTitle())
                .description(item.videoDescription())
                .url("/api/videos/stream/" + item.videoFileName())
                .thumbnailUrl(item.videoThumbnailPath() != null
                        ? publicBaseUrl + "/api/videos/thumbnail/" + item.videoThumbnailPath()
                        : null)
                .fileSize(item.videoFileSize())
                .views(item.videoViews())
                .likes(item.videoLikes())
                .uploadedBy(item.videoUploadedByUsername())
                .uploadedAt(item.videoUploadedAt() != null ? item.videoUploadedAt().toString() : null)
                .build();
    }
}