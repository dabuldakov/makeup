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
        String fileName,
        String contentType,
        Long fileSize,
        String duration,
        String thumbnailPath,
        Integer views,
        Integer likes,
        LocalDateTime uploadedAt,
        String uploadedByUsername) {
}