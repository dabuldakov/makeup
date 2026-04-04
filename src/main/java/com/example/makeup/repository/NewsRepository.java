package com.example.makeup.repository;

import com.example.makeup.entity.NewsItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NewsRepository extends JpaRepository<NewsItem, Long> {
    Page<NewsItem> findByIsPublishedTrueOrderByPublishedAtDesc(Pageable pageable);
}