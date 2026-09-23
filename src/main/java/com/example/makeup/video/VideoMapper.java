package com.example.makeup.video;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class VideoMapper {

    private final String publicBaseUrl;

    public VideoMapper(@Value("${app.public-base-url:http://localhost:8080}") String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }

    public VideoResponse toResponse(Video video) {
        return toResponse(video, null);
    }

    public VideoResponse toResponse(Video video, Boolean likedByMe) {
        if (video == null) {
            return null;
        }
        return VideoResponse.builder()
                .id(video.getId())
                .title(video.getTitle())
                .description(video.getDescription())
                .url("/api/videos/stream/" + video.getObjectKey())
                .thumbnailUrl(video.getThumbnailKey() != null
                        ? publicBaseUrl + "/api/videos/thumbnail/" + video.getThumbnailKey()
                        : null)
                .fileSize(video.getFileSize())
                .durationSeconds(video.getDurationSeconds())
                .views(video.getViews())
                .likes(video.getLikesCount())
                .likedByMe(likedByMe)
                .uploadedBy(video.getUploadedBy() != null ? video.getUploadedBy().getUsername() : null)
                .uploadedAt(video.getCreatedAt() != null ? video.getCreatedAt().toString() : null)
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
                .url("/api/videos/stream/" + v.objectKey())
                .thumbnailUrl(v.thumbnailKey() != null
                        ? publicBaseUrl + "/api/videos/thumbnail/" + v.thumbnailKey()
                        : null)
                .fileSize(v.fileSize())
                .durationSeconds(v.durationSeconds())
                .views(v.views())
                .likes(v.likesCount())
                .uploadedBy(v.uploadedByUsername())
                .uploadedAt(v.createdAt() != null ? v.createdAt().toString() : null)
                .build();
    }
}
