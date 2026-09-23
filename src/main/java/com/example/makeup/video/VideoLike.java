package com.example.makeup.video;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Факт лайка видео пользователем. Денормализованный счётчик —
 * {@link Video#getLikesCount()}.
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode(of = {"userId", "videoId"})
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "video_likes")
@IdClass(VideoLike.VideoLikeId.class)
public class VideoLike {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "video_id")
    private Long videoId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    /** Составной ключ (user_id, video_id). */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class VideoLikeId implements Serializable {
        private Long userId;
        private Long videoId;
    }
}
