package com.example.makeup.repository;

import com.example.makeup.entity.Video;
import com.example.makeup.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VideoRepository extends JpaRepository<Video, Long> {
    Page<Video> findByUploadedBy(User user, Pageable pageable);

    @Query("SELECT v FROM Video v ORDER BY v.views DESC")
    Page<Video> findMostPopular(Pageable pageable);

    @Query("SELECT v FROM Video v WHERE LOWER(v.title) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Video> searchVideos(String query, Pageable pageable);
}