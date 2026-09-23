package com.example.makeup.service;

import com.example.makeup.entity.MediaJob;
import com.example.makeup.entity.MediaJobType;
import com.example.makeup.repository.MediaJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/**
 * Очередь фоновой обработки медиа. {@code enqueue} вызывается в транзакции
 * загрузки видео; {@code processPendingBatch} — планировщиком/тестом.
 *
 * Claim выполняется отдельной короткой транзакцией (SKIP LOCKED), а сама
 * обработка каждой задачи — в своей транзакции ({@link MediaJobProcessor}).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MediaJobService {

    private final MediaJobRepository mediaJobRepository;
    private final MediaJobProcessor mediaJobProcessor;
    private final PlatformTransactionManager transactionManager;

    public void enqueue(Long videoId) {
        mediaJobRepository.save(MediaJob.builder()
                .videoId(videoId)
                .type(MediaJobType.THUMBNAIL)
                .build());
        log.info("Enqueued THUMBNAIL media job for video {}", videoId);
    }

    public int processPendingBatch(int limit) {
        List<Long> claimed = new TransactionTemplate(transactionManager).execute(status -> {
            List<Long> ids = mediaJobRepository.findPendingIdsForUpdate(limit);
            for (Long id : ids) {
                mediaJobRepository.markRunning(id);
            }
            return ids;
        });
        if (claimed == null || claimed.isEmpty()) {
            return 0;
        }
        for (Long jobId : claimed) {
            mediaJobProcessor.process(jobId);
        }
        return claimed.size();
    }
}
