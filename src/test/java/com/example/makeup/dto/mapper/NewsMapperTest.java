package com.example.makeup.dto.mapper;

import com.example.makeup.entity.NewsItem;
import com.example.makeup.entity.User;
import com.example.makeup.entity.Video;
import com.example.makeup.dto.response.NewsResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class NewsMapperTest {

    private final NewsMapper mapper = new NewsMapper(new VideoMapper());

    private NewsItem newsItem(Video video, String imageUrl) {
        User author = User.builder().username("alice").build();
        return NewsItem.builder()
                .id(10L)
                .title("Title")
                .content("Body")
                .imageUrl(imageUrl)
                .relatedVideo(video)
                .author(author)
                .publishedAt(LocalDateTime.of(2026, 1, 1, 12, 0))
                .build();
    }

    @Test
    void toResponse_shouldMapBaseFieldsAndImageUrl() {
        NewsResponse dto = mapper.toResponse(newsItem(null, "img001.jpg"));

        assertEquals(10L, dto.getId());
        assertEquals("Title", dto.getTitle());
        assertEquals("Body", dto.getContent());
        assertEquals("/api/news/image/img001.jpg", dto.getImageUrl());
        assertEquals("alice", dto.getAuthor());
        assertEquals("2026-01-01T12:00", dto.getPublishedAt().toString());
        assertNull(dto.getRelatedVideo());
    }

    @Test
    void toResponse_shouldLeaveImageUrlNullWhenAbsent() {
        NewsResponse dto = mapper.toResponse(newsItem(null, null));

        assertNull(dto.getImageUrl());
    }

    @Test
    void toResponse_shouldMapRelatedVideo() {
        User author = User.builder().username("bob").build();
        Video video = Video.builder()
                .id(7L)
                .title("Clip")
                .fileName("vid0001")
                .contentType("video/mp4")
                .fileSize(1024L)
                .views(3)
                .likes(1)
                .uploadedBy(author)
                .uploadedAt(LocalDateTime.of(2026, 1, 2, 9, 0))
                .build();

        NewsResponse dto = mapper.toResponse(newsItem(video, null));

        assertEquals(7L, dto.getRelatedVideo().getId());
        assertEquals("/api/videos/stream/vid0001.mp4", dto.getRelatedVideo().getUrl());
        assertEquals("/api/videos/thumbnail/vid0001.jpeg", dto.getRelatedVideo().getThumbnailUrl());
    }
}