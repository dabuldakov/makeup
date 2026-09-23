package com.example.makeup.video;

import com.example.makeup.auth.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class VideoMapperTest {

    private final VideoMapper mapper = new VideoMapper("http://localhost:8080");

    @Test
    void toResponse_shouldMapVideoFields() {
        User author = User.builder().id(1L).username("alice").build();
        Video video = Video.builder()
                .id(7L)
                .title("Clip")
                .description("Desc")
                .objectKey("vid0001.mp4")
                .thumbnailKey("thumb0001.jpeg")
                .contentType("video/mp4")
                .fileSize(2048L)
                .durationSeconds(10)
                .views(5L)
                .likesCount(2L)
                .uploadedBy(author)
                .createdAt(LocalDateTime.of(2026, 1, 2, 9, 0))
                .build();

        VideoResponse dto = mapper.toResponse(video, true);

        assertEquals(7L, dto.getId());
        assertEquals("Clip", dto.getTitle());
        assertEquals("/api/videos/stream/vid0001.mp4", dto.getUrl());
        assertEquals("http://localhost:8080/api/videos/thumbnail/thumb0001.jpeg", dto.getThumbnailUrl());
        assertEquals(2048L, dto.getFileSize());
        assertEquals(10, dto.getDurationSeconds());
        assertEquals(5L, dto.getViews());
        assertEquals(2L, dto.getLikes());
        assertEquals(true, dto.getLikedByMe());
        assertEquals("alice", dto.getUploadedBy());
        assertEquals("2026-01-02T09:00", dto.getUploadedAt());
    }

    @Test
    void toResponse_shouldLeaveThumbnailNullWhenAbsent() {
        User author = User.builder().id(1L).username("alice").build();
        Video video = Video.builder().id(7L).objectKey("vid0001.mp4").uploadedBy(author).build();

        assertNull(mapper.toResponse(video).getThumbnailUrl());
    }

    @Test
    void toResponse_shouldReturnNullForNullVideo() {
        assertNull(mapper.toResponse((Video) null));
    }
}
