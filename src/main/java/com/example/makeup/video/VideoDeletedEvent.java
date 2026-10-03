package com.example.makeup.video;

/**
 * Факт удаления видео. Публикуется до удаления строки, чтобы модули, ссылающиеся
 * на видео (например, новости), успели снять ссылку — иначе FK ON DELETE RESTRICT
 * не даст удалить видео.
 *
 * <p>Событие разрывает зависимость {@code video -> news}: video больше не знает
 * о новостях, подписчик живёт на стороне news.
 */
public record VideoDeletedEvent(Long videoId) {
}
