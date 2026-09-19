package com.example.makeup.dto.mapper;

import com.example.makeup.entity.Video;
import com.example.makeup.dto.response.VideoResponse;
import com.example.makeup.service.MinioService;
import org.springframework.stereotype.Component;

@Component
public class VideoMapper {

    public VideoResponse toResponse(Video video) {
        if (video == null) {
            return null;
        }
        return VideoResponse.builder()
                .id(video.getId())
                .title(video.getTitle())
                .description(video.getDescription())
                .url("/api/videos/stream/" + video.getFileName() + MinioService.getExtensionFromContentType(video.getContentType()))
                .thumbnailUrl("/api/videos/thumbnail/" + video.getFileName() + ".jpeg")
                .fileSize(video.getFileSize())
                .views(video.getViews())
                .likes(video.getLikes())
                .uploadedBy(video.getUploadedBy().getUsername())
                .uploadedAt(video.getUploadedAt().toString())
                .build();
    }
}