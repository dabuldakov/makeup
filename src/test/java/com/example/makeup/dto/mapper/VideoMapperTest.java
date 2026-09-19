package com.example.makeup.dto.mapper;

import com.example.makeup.entity.User;
import com.example.makeup.entity.Video;
import com.example.makeup.dto.response.VideoResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class VideoMapperTest {

    private final VideoMapper mapper = new VideoMapper();

    @Test
    void toResponse_shouldMapVideoFields() {
        User author = User.builder().id(1L).username("alice").build();
        Video video = Video.builder()
                .id(7L)
                .title("Clip")
                .description("Desc")
                .fileName("vid0001")
                .contentType("video/mp4")
                .fileSize(2048L)
                .views(5)
                .likes(2)
                .uploadedBy(author)
                .uploadedAt(LocalDateTime.of(2026, 1, 2, 9, 0))
                .build();

        VideoResponse dto = mapper.toResponse(video);

        assertEquals(7L, dto.getId());
        assertEquals("Clip", dto.getTitle());
        assertEquals("/api/videos/stream/vid0001.mp4", dto.getUrl());
        assertEquals("/api/videos/thumbnail/vid0001.jpeg", dto.getThumbnailUrl());
        assertEquals(2048L, dto.getFileSize());
        assertEquals(5, dto.getViews());
        assertEquals(2, dto.getLikes());
        assertEquals("alice", dto.getUploadedBy());
        assertEquals("2026-01-02T09:00", dto.getUploadedAt());
    }

    @Test
    void toResponse_shouldReturnNullForNullVideo() {
        assertNull(mapper.toResponse(null));
    }
}