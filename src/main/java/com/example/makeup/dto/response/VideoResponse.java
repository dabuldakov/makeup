package com.example.makeup.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VideoResponse {
    private Long id;
    private String title;
    private String description;
    private String url;
    private String thumbnailUrl;
    private Long fileSize;
    private Integer durationSeconds;
    private Long views;
    private Long likes;
    private Boolean likedByMe;
    private String uploadedBy;
    private String uploadedAt;
}
