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
    private String duration;
    private Integer views;
    private Integer likes;
    private String uploadedBy;
    private String uploadedAt;
}