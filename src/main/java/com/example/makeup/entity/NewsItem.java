package com.example.makeup.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "news")
public class NewsItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 5000)
    private String content;

    private String imageUrl;

    @ManyToOne
    @JoinColumn(name = "video_id")
    private Video relatedVideo;

    @ManyToOne
    @JoinColumn(name = "author_id")
    private User author;

    @CreatedDate
    private LocalDateTime publishedAt;

    private boolean isPublished = true;

    @PrePersist
    protected void onCreate() {
        publishedAt = LocalDateTime.now();
    }
}