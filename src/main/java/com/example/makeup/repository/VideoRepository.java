package com.example.makeup.repository;

import com.example.makeup.dto.VideoItem;
import com.example.makeup.entity.Video;
import com.example.makeup.entity.User;
import com.example.makeup.entity.VideoStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface VideoRepository extends JpaRepository<Video, Long> {

    Page<Video> findByUploadedBy(User user, Pageable pageable);

    /**
     * Лента опубликованных видео без гидрации полных сущностей {@link User}
     * (join только по нужным колонкам) — не тянет password/email загрузивших.
     */
    @Query("""
            SELECT new com.example.makeup.dto.VideoItem(
                v.id, v.title, v.description, v.objectKey, v.contentType, v.fileSize,
                v.durationSeconds, v.thumbnailKey, v.views, v.likesCount, v.createdAt, u.username)
            FROM Video v
            LEFT JOIN v.uploadedBy u
            WHERE v.status = com.example.makeup.entity.VideoStatus.PUBLISHED
            ORDER BY v.createdAt DESC, v.id DESC
            """)
    Page<VideoItem> findAllProjected(Pageable pageable);

    @Query("""
            SELECT v FROM Video v
            WHERE v.status = com.example.makeup.entity.VideoStatus.PUBLISHED
            ORDER BY v.views DESC
            """)
    Page<Video> findMostPopular(Pageable pageable);

    @Query("""
            SELECT v FROM Video v
            WHERE v.status = com.example.makeup.entity.VideoStatus.PUBLISHED
              AND LOWER(v.title) LIKE LOWER(CONCAT('%', :query, '%'))
            """)
    Page<Video> searchVideos(String query, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Video v SET v.views = v.views + 1 WHERE v.id = :id")
    int incrementViews(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Video v SET v.thumbnailKey = :key, v.status = :status WHERE v.id = :id")
    void updateThumbnailAndStatus(@Param("id") Long id,
                                  @Param("key") String key,
                                  @Param("status") VideoStatus status);

    @Modifying
    @Query("UPDATE Video v SET v.status = :status WHERE v.id = :id")
    void updateStatus(@Param("id") Long id, @Param("status") VideoStatus status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Video v SET v.likesCount = v.likesCount + :delta WHERE v.id = :id")
    void adjustLikesCount(@Param("id") Long id, @Param("delta") long delta);
}
