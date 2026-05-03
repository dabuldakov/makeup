package com.example.makeup.config;

public enum BucketType {
    VIDEOS("videos", "Video bucket"),
    THUMBNAILS("thumbnails", "Thumbnail bucket"),
    NEWS_IMAGE("news-image", "News image bucket");

    private final String bucketName;
    private final String description;

    BucketType(String bucketName, String description) {
        this.bucketName = bucketName;
        this.description = description;
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getDescription() {
        return description;
    }
}