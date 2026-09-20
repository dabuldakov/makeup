package com.example.makeup.dto.mapper;

import com.example.makeup.entity.Video;
import com.example.makeup.dto.VideoItem;
import com.example.makeup.dto.response.VideoResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class VideoMapper {

    private final String publicBaseUrl;

    public VideoMapper(@Value("${app.public-base-url:http://localhost:8080}") String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }

    public VideoResponse toResponse(Video video) {
        if (video == null) {
            return null;
        }
        return VideoResponse.builder()
                .id(video.getId())
                .title(video.getTitle())
                .description(video.getDescription())
                .url("/api/videos/stream/" + video.getFileName())
                .thumbnailUrl(video.getThumbnailPath() != null
                        ? publicBaseUrl + "/api/videos/thumbnail/" + video.getThumbnailPath()
                        : null)
                .fileSize(video.getFileSize())
                .views(video.getViews())
                .likes(video.getLikes())
                .uploadedBy(video.getUploadedBy().getUsername())
                .uploadedAt(video.getUploadedAt() != null ? video.getUploadedAt().toString() : null)
                .build();
    }

    public VideoResponse toResponse(VideoItem v) {
        if (v == null) {
            return null;
        }
        return VideoResponse.builder()
                .id(v.id())
                .title(v.title())
                .description(v.description())
                .url("/api/videos/stream/" + v.fileName())
                .thumbnailUrl(v.thumbnailPath() != null
                        ? publicBaseUrl + "/api/videos/thumbnail/" + v.thumbnailPath()
                        : null)
                .fileSize(v.fileSize())
                .views(v.views())
                .likes(v.likes())
                .uploadedBy(v.uploadedByUsername())
                .uploadedAt(v.uploadedAt() != null ? v.uploadedAt().toString() : null)
                .build();
    }
}