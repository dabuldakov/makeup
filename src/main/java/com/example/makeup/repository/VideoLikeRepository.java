package com.example.makeup.repository;

import com.example.makeup.entity.VideoLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VideoLikeRepository extends JpaRepository<VideoLike, VideoLike.VideoLikeId> {

    boolean existsByUserIdAndVideoId(Long userId, Long videoId);

    long deleteByUserIdAndVideoId(Long userId, Long videoId);
}
