package com.example.makeup.media;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Периодически забирает и обрабатывает задачи медиа. Отключается свойством
 * {@code media.jobs.scheduler.enabled=false} (например, в интеграционных тестах,
 * где обработку вызывают напрямую).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "media.jobs.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class MediaJobScheduler {

    private static final int BATCH_SIZE = 5;

    private final MediaJobService mediaJobService;

    @Scheduled(fixedDelayString = "${media.jobs.poll-interval-ms:5000}")
    public void poll() {
        try {
            int processed = mediaJobService.processPendingBatch(BATCH_SIZE);
            if (processed > 0) {
                log.info("Processed {} media jobs", processed);
            }
        } catch (Exception e) {
            log.error("Media job poll failed", e);
        }
    }
}
