package com.example.makeup.media;

/**
 * Порт обработки задач очереди. Живёт в модуле {@code media}, а реализации —
 * в доменных модулях (например, генерация превью видео — в {@code video}).
 *
 * <p>Благодаря порту {@code media} ничего не знает о {@code Video} и
 * {@code VideoRepository}: зависимость идёт только {@code video -> media}.
 */
public interface MediaJobHandler {

    /** Тип задач, которые умеет обрабатывать реализация. */
    MediaJobType type();

    /**
     * Выполняет работу по задаче. Исключение означает неуспех: очередь сама
     * решит, повторить (PENDING) или окончательно провалить (FAILED) задачу.
     */
    void handle(MediaJob job) throws Exception;

    /**
     * Вызывается один раз, когда задача окончательно провалена (исчерпаны
     * попытки) — доменный модуль может пометить свою сущность как FAILED.
     */
    default void onPermanentFailure(MediaJob job) {
    }
}
