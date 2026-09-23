package com.example.makeup.video;

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
