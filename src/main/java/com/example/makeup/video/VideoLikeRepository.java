package com.example.makeup.video;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VideoLikeRepository extends JpaRepository<VideoLike, VideoLike.VideoLikeId> {

    boolean existsByUserIdAndVideoId(Long userId, Long videoId);

    long deleteByUserIdAndVideoId(Long userId, Long videoId);

    /** Видео, которые лайкнул пользователь, — нужны, чтобы пересчитать счётчики при удалении аккаунта. */
    @Query("SELECT vl.videoId FROM VideoLike vl WHERE vl.userId = :userId")
    List<Long> findVideoIdsByUserId(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM VideoLike vl WHERE vl.userId = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);
}
