package com.example.makeup.repository;

import com.example.makeup.dto.VideoItem;
import com.example.makeup.entity.Video;
import com.example.makeup.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VideoRepository extends JpaRepository<Video, Long> {
    Page<Video> findByUploadedBy(User user, Pageable pageable);

    /**
     * Лента видео без гидрации полных сущностей {@link User} (join только по
     * нужным колонкам) — не тянет password/email загрузивших.
     */
    @Query("""
            SELECT new com.example.makeup.dto.VideoItem(
                v.id, v.title, v.description, v.fileName, v.contentType, v.fileSize,
                v.duration, v.thumbnailPath, v.views, v.likes, v.uploadedAt, u.username)
            FROM Video v
            LEFT JOIN v.uploadedBy u
            ORDER BY v.uploadedAt DESC, v.id DESC
            """)
    Page<VideoItem> findAllProjected(Pageable pageable);

    @Query("SELECT v FROM Video v ORDER BY v.views DESC")
    Page<Video> findMostPopular(Pageable pageable);

    @Query("SELECT v FROM Video v WHERE LOWER(v.title) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Video> searchVideos(String query, Pageable pageable);

    @Modifying
    @Query("UPDATE Video v SET v.views = v.views + 1 WHERE v.id = :id")
    int incrementViews(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Video v SET v.thumbnailPath = :path WHERE v.id = :id")
    void updateThumbnailPath(@Param("id") Long id, @Param("path") String path);
}