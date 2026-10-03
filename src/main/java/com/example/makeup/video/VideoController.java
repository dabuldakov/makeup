package com.example.makeup.video;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

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
    public ResponseEntity<VideoResponse> getVideo(@PathVariable Long id, Authentication authentication) {
        videoService.incrementViews(id);
        Video video = videoService.getVideoById(id);
        Boolean likedByMe = authentication != null
                ? videoService.isLikedBy(id, authentication.getName())
                : null;
        return ResponseEntity.ok(videoMapper.toResponse(video, likedByMe));
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<Map<String, Object>> likeVideo(@PathVariable Long id, Authentication authentication) {
        long likes = videoService.toggleLike(id, authentication.getName());
        return ResponseEntity.ok(Map.of("liked", true, "likes", likes));
    }

    @DeleteMapping("/{id}/like")
    public ResponseEntity<Map<String, Object>> unlikeVideo(@PathVariable Long id, Authentication authentication) {
        long likes = videoService.toggleLike(id, authentication.getName());
        return ResponseEntity.ok(Map.of("liked", false, "likes", likes));
    }

    /**
     * Отдача видеофайла с поддержкой HTTP Range.
     *
     * Плееру (ExoPlayer) нужна докачка по диапазонам: без неё для MP4 с
     * moov-атомом в конце приходится качать весь файл, и воспроизведение
     * «висит». Поэтому при заголовке {@code Range} отдаём 206 с Content-Range,
     * а в обычном ответе сообщаем Accept-Ranges: bytes.
     */
    @GetMapping("/stream/{fileName}")
    public ResponseEntity<Resource> streamVideo(
            @PathVariable String fileName,
            @RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeader
    ) {
        VideoFileInfo info = videoService.getVideoFileInfo(fileName);
        long fileSize = info.size();
        MediaType contentType = MediaType.parseMediaType(
                info.contentType() != null && !info.contentType().isBlank()
                        ? info.contentType()
                        : MediaType.APPLICATION_OCTET_STREAM_VALUE
        );

        ByteRange range;
        try {
            range = RangeHeader.parse(rangeHeader, fileSize);
        } catch (RangeHeader.RangeNotSatisfiableException e) {
            return rangeNotSatisfiable(fileSize);
        }

        if (range == null) {
            Resource resource = videoService.getVideoFile(fileName, 0, -1);
            return ResponseEntity.ok()
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                    .contentLength(fileSize)
                    .contentType(contentType)
                    .body(resource);
        }

        Resource resource = videoService.getVideoFile(fileName, range.start(), range.length());
        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .header(HttpHeaders.CONTENT_RANGE, range.contentRangeHeader(fileSize))
                .contentLength(range.length())
                .contentType(contentType)
                .body(resource);
    }

    private ResponseEntity<Resource> rangeNotSatisfiable(long fileSize) {
        return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header(HttpHeaders.CONTENT_RANGE, "bytes */" + fileSize)
                .build();
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
