package com.example.makeup.dto;

import java.time.LocalDateTime;

/**
 * Читаемая проекция строки видео для лент/списков. Позволяет отдать только
 * нужные колонки (включая username загрузившего) без жадной загрузки полного
 * объекта {@link com.example.makeup.entity.User} (в т.ч. password) на каждое видео.
 */
public record VideoItem(
        Long id,
        String title,
        String description,
        String objectKey,
        String contentType,
        Long fileSize,
        Integer durationSeconds,
        String thumbnailKey,
        Long views,
        Long likesCount,
        LocalDateTime createdAt,
        String uploadedByUsername) {
}
