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
        String imageUrl,
        LocalDateTime publishedAt,
        String authorUsername,
        Long videoId,
        String videoTitle,
        String videoDescription,
        String videoFileName,
        String videoContentType,
        Long videoFileSize,
        String videoDuration,
        String videoThumbnailPath,
        Integer videoViews,
        Integer videoLikes,
        LocalDateTime videoUploadedAt,
        String videoUploadedByUsername) {

    public boolean hasRelatedVideo() {
        return videoId != null;
    }
}