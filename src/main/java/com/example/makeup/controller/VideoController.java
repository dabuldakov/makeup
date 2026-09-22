package com.example.makeup.controller;

import com.example.makeup.dto.mapper.VideoMapper;
import com.example.makeup.dto.response.VideoResponse;
import com.example.makeup.dto.VideoItem;
import com.example.makeup.entity.Video;
import com.example.makeup.service.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;
    private final VideoMapper videoMapper;

    @PostMapping("/upload")
    public ResponseEntity<VideoResponse> uploadVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "thumbnail", required = false) MultipartFile thumbnail,
            Authentication authentication
    ) {
        Video video = videoService.uploadVideo(file, title, description, thumbnail,
                authentication.getName());
        return ResponseEntity.ok(videoMapper.toResponse(video));
    }

    @GetMapping
    public ResponseEntity<List<VideoResponse>> getAllVideos(Pageable pageable) {
        Page<VideoItem> allVideos = videoService.getAllVideos(pageable);
        List<VideoResponse> responses = allVideos.get().map(videoMapper::toResponse).toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<VideoResponse> getVideo(@PathVariable Long id) {
        videoService.incrementViews(id);
        Video video = videoService.getVideoById(id);
        return ResponseEntity.ok(videoMapper.toResponse(video));
    }

    @GetMapping("/stream/{fileName}")
    public ResponseEntity<Resource> streamVideo(@PathVariable String fileName) {
        Resource resource = videoService.getVideoFile(fileName);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @GetMapping("/thumbnail/{fileName}")
    public ResponseEntity<Resource> getVideoThumbnail(@PathVariable String fileName) {
        Resource resource = videoService.getThumbnailFile(fileName);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(resource);
    }

    @GetMapping("/thumbnail/url/{fileName}")
    public ResponseEntity<String> getVideoThumbnailUrl(@PathVariable String fileName) {
        var url = videoService.getThumbnailUrl(fileName);
        return ResponseEntity.ok(url);
    }

    @GetMapping("/url/{fileName}")
    public ResponseEntity<String> getVideoUrl(@PathVariable String fileName) {
        return ResponseEntity.ok(videoService.getVideoUrl(fileName));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVideo(@PathVariable Long id, Authentication authentication) {
        videoService.deleteVideo(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}