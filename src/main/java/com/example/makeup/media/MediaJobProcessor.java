package com.example.makeup.media;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Обработка одной задачи медиа в отдельной транзакции. Воркер вызывает этот
 * бин (а не сам себя), чтобы {@link Transactional} применялся и «долгий» ffmpeg
 * не держал транзакцию claim-а.
 *
 * <p>Процессор отвечает только за жизненный цикл задачи (попытки, статусы), а
 * конкретную работу выполняет {@link MediaJobHandler}, найденный по типу задачи.
 * Так медиа-модуль не зависит от доменных (video).
 */
@Component
@Slf4j
public class MediaJobProcessor {

    private static final int MAX_ATTEMPTS = 3;

    private final MediaJobRepository mediaJobRepository;
    private final Map<MediaJobType, MediaJobHandler> handlers;

    public MediaJobProcessor(MediaJobRepository mediaJobRepository, List<MediaJobHandler> handlers) {
        this.mediaJobRepository = mediaJobRepository;
        Map<MediaJobType, MediaJobHandler> byType = new EnumMap<>(MediaJobType.class);
        for (MediaJobHandler handler : handlers) {
            byType.put(handler.type(), handler);
        }
        this.handlers = byType;
    }

    @Transactional
    public void process(Long jobId) {
        MediaJob job = mediaJobRepository.findById(jobId).orElse(null);
        if (job == null) {
            return;
        }

        MediaJobHandler handler = handlers.get(job.getType());
        try {
            if (handler == null) {
                throw new IllegalStateException("No handler for media job type: " + job.getType());
            }
            handler.handle(job);

            job.setStatus(MediaJobStatus.DONE);
            job.setLastError(null);
            log.info("Media job {} done", jobId);
        } catch (Exception e) {
            log.warn("Media job {} failed (attempt {}): {}", jobId, job.getAttempts(), e.getMessage());
            job.setLastError(truncate(e.getMessage()));
            if (job.getAttempts() >= MAX_ATTEMPTS) {
                job.setStatus(MediaJobStatus.FAILED);
                if (handler != null) {
                    handler.onPermanentFailure(job);
                }
            } else {
                job.setStatus(MediaJobStatus.PENDING);
            }
        }

        mediaJobRepository.save(job);
    }

    private String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }
}
