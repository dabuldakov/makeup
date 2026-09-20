package com.example.makeup.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString(exclude = {"relatedVideo", "author"})
@EqualsAndHashCode(of = "id")
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

    private LocalDateTime publishedAt;

    private boolean isPublished = true;

    @PrePersist
    protected void onCreate() {
        publishedAt = LocalDateTime.now();
    }
}