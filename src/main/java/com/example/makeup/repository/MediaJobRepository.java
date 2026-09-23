package com.example.makeup.repository;

import com.example.makeup.entity.MediaJob;
import com.example.makeup.entity.MediaJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MediaJobRepository extends JpaRepository<MediaJob, Long> {

    long countByStatus(MediaJobStatus status);

    /**
     * Забирает до {@code limit} PENDING-задач под блокировку (SKIP LOCKED —
     * несколько воркеров не подерутся за одну задачу). Вызывается в транзакции.
     */
    @Query(value = """
            SELECT id FROM media_jobs
             WHERE status = 'PENDING'
             ORDER BY created_at
             LIMIT :limit
             FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<Long> findPendingIdsForUpdate(@Param("limit") int limit);

    @Modifying
    @Query(value = """
            UPDATE media_jobs
               SET status = 'RUNNING', attempts = attempts + 1, updated_at = now()
             WHERE id = :id
            """, nativeQuery = true)
    int markRunning(@Param("id") Long id);
}
