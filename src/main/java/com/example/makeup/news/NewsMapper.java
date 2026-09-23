package com.example.makeup.news;

import com.example.makeup.video.VideoResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.makeup.video.VideoMapper;
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
                .imageUrl(newsItem.getImageKey() != null
                        ? publicBaseUrl + "/api/news/image/" + newsItem.getImageKey()
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
                .imageUrl(item.imageKey() != null
                        ? publicBaseUrl + "/api/news/image/" + item.imageKey()
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
                .url("/api/videos/stream/" + item.videoObjectKey())
                .thumbnailUrl(item.videoThumbnailKey() != null
                        ? publicBaseUrl + "/api/videos/thumbnail/" + item.videoThumbnailKey()
                        : null)
                .fileSize(item.videoFileSize())
                .durationSeconds(item.videoDurationSeconds())
                .views(item.videoViews())
                .likes(item.videoLikesCount())
                .uploadedBy(item.videoUploadedByUsername())
                .uploadedAt(item.videoCreatedAt() != null ? item.videoCreatedAt().toString() : null)
                .build();
    }
}
