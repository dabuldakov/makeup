package com.example.makeup.controller;

import com.example.makeup.dto.response.VideoResponse;
import com.example.makeup.entity.Video;
import com.example.makeup.service.MinioService;
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

    @PostMapping("/upload")
    public ResponseEntity<VideoResponse> uploadVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            Authentication authentication
    ) {
        Video video = videoService.uploadVideo(file, title, description, authentication.getName());
        return ResponseEntity.ok(mapToResponse(video));
    }

    @GetMapping
    public ResponseEntity<List<VideoResponse>> getAllVideos(Pageable pageable) {
        Page<Video> allVideos = videoService.getAllVideos(pageable);
        List<VideoResponse> responses = allVideos.get().map(VideoController::mapToResponse).toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<VideoResponse> getVideo(@PathVariable Long id) {
        Video video = videoService.getVideoById(id);
        videoService.incrementViews(id);
        return ResponseEntity.ok(mapToResponse(video));
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
    public ResponseEntity<byte[]> getVideoThumbnail(@PathVariable String fileName) {
        byte[] thumbnailBytes = videoService.getThumbnailBytes(fileName);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(thumbnailBytes);
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

    public static VideoResponse mapToResponse(Video video) {
        return VideoResponse.builder()
                .id(video.getId())
                .title(video.getTitle())
                .description(video.getDescription())
                .url("/api/videos/stream/" + video.getFileName() + MinioService.getExtensionFromContentType(video.getContentType()))
                .thumbnailUrl("/api/videos/thumbnail/" + video.getFileName() + ".jpeg")
                .fileSize(video.getFileSize())
                .views(video.getViews())
                .likes(video.getLikes())
                .uploadedBy(video.getUploadedBy().getUsername())
                .uploadedAt(video.getUploadedAt().toString())
                .build();
    }
}