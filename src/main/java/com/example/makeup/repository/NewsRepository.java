package com.example.makeup.repository;

import com.example.makeup.dto.NewsListItem;
import com.example.makeup.entity.NewsItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NewsRepository extends JpaRepository<NewsItem, Long> {

    Optional<NewsItem> findByIdAndDeletedAtIsNull(Long id);

    /**
     * Лента опубликованных новостей без гидрации полных сущностей Video/User
     * (join только по нужным колонкам) — не тянет password авторов и загрузивших.
     */
    @Query("""
            SELECT new com.example.makeup.dto.NewsListItem(
                n.id, n.title, n.content, n.imageKey, n.publishedAt, a.username,
                v.id, v.title, v.description, v.objectKey, v.contentType, v.fileSize,
                v.durationSeconds, v.thumbnailKey, v.views, v.likesCount, v.createdAt, u.username)
            FROM NewsItem n
            LEFT JOIN n.author a
            LEFT JOIN n.relatedVideo v
            LEFT JOIN v.uploadedBy u
            WHERE n.status = com.example.makeup.entity.NewsStatus.PUBLISHED
              AND n.deletedAt IS NULL
            ORDER BY n.publishedAt DESC
            """)
    Page<NewsListItem> findAllPublished(Pageable pageable);

    /**
     * Отвязывает удаляемое видео от новостей, чтобы новости не остались
     * со ссылкой на несуществующую запись.
     */
    @Modifying
    @Query("UPDATE NewsItem n SET n.relatedVideo = NULL WHERE n.relatedVideo.id = :videoId")
    void detachVideo(@Param("videoId") Long videoId);
}
