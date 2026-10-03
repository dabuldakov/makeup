package com.example.makeup.video;

/**
 * Метаданные видеофайла в хранилище: нужны для поддержки HTTP Range
 * (размер объекта и его content-type).
 */
public record VideoFileInfo(long size, String contentType) {
}
