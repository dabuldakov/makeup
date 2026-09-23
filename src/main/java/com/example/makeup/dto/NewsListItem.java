package com.example.makeup.dto;

import java.time.LocalDateTime;

/**
 * Читаемая проекция опубликованной новости для ленты: плоские поля вместе
 * с вложенным связанным видео и usernames (автор + загрузивший видео) — без
 * жадной загрузки полных сущностей {@link com.example.makeup.entity.Video}
 * и {@link com.example.makeup.entity.User} (в т.ч. password).
 */
public record NewsListItem(
        Long id,
        String title,
        String content,
        String imageKey,
        LocalDateTime publishedAt,
        String authorUsername,
        Long videoId,
        String videoTitle,
        String videoDescription,
        String videoObjectKey,
        String videoContentType,
        Long videoFileSize,
        Integer videoDurationSeconds,
        String videoThumbnailKey,
        Long videoViews,
        Long videoLikesCount,
        LocalDateTime videoCreatedAt,
        String videoUploadedByUsername) {

    public boolean hasRelatedVideo() {
        return videoId != null;
    }
}
