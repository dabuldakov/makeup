package com.example.makeup.entity;

/**
 * Жизненный цикл видео: загрузка/обработка/публикация.
 */
public enum VideoStatus {
    UPLOADING,
    PROCESSING,
    PUBLISHED,
    FAILED,
    ARCHIVED
}
