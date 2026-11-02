package com.example.makeup.report;

import com.example.makeup.auth.User;
import com.example.makeup.auth.UserRepository;
import com.example.makeup.exception.NotFoundException;
import com.example.makeup.news.NewsItem;
import com.example.makeup.news.NewsRepository;
import com.example.makeup.video.Video;
import com.example.makeup.video.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Приём жалоб на новости и видео. Повторная жалоба идемпотентна. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentReportService {

    private static final int PREVIEW_LIMIT = 500;

    private final ContentReportRepository reportRepository;
    private final NewsRepository newsRepository;
    private final VideoRepository videoRepository;
    private final UserRepository userRepository;

    @Transactional
    public ContentReport createReport(String username, CreateReportRequestDto request) {
        User reporter = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found: " + username));

        var existing = reportRepository.findByReporterIdAndContentTypeAndContentId(
                reporter.getId(), request.getContentType(), request.getContentId());
        if (existing.isPresent()) {
            return existing.get();
        }

        Long targetUserId;
        String preview;

        switch (request.getContentType()) {
            case NEWS -> {
                NewsItem news = newsRepository.findById(request.getContentId())
                        .orElseThrow(() -> new NotFoundException("News not found"));
                if (news.getAuthor().getId().equals(reporter.getId())) {
                    throw new IllegalArgumentException("Cannot report your own news");
                }
                targetUserId = news.getAuthor().getId();
                preview = truncate(news.getTitle());
            }
            case VIDEO -> {
                Video video = videoRepository.findById(request.getContentId())
                        .orElseThrow(() -> new NotFoundException("Video not found"));
                if (video.getUploadedBy().getId().equals(reporter.getId())) {
                    throw new IllegalArgumentException("Cannot report your own video");
                }
                targetUserId = video.getUploadedBy().getId();
                preview = truncate(video.getTitle());
            }
            default -> throw new IllegalArgumentException("Unsupported content type");
        }

        ContentReport report = ContentReport.builder()
                .reporterId(reporter.getId())
                .contentType(request.getContentType())
                .contentId(request.getContentId())
                .targetUserId(targetUserId)
                .reason(request.getReason())
                .comment(trimToNull(request.getComment()))
                .contentPreview(preview)
                .status(ContentReport.Status.NEW)
                .build();

        try {
            ContentReport saved = reportRepository.save(report);
            log.warn("Content report created: type={}, contentId={}, reason={}, reporter={}, target={}",
                    saved.getContentType(), saved.getContentId(), saved.getReason(),
                    reporter.getId(), targetUserId);
            return saved;
        } catch (DataIntegrityViolationException e) {
            return reportRepository.findByReporterIdAndContentTypeAndContentId(
                            reporter.getId(), request.getContentType(), request.getContentId())
                    .orElseThrow(() -> e);
        }
    }

    private static String truncate(String text) {
        if (text == null) return null;
        return text.length() <= PREVIEW_LIMIT ? text : text.substring(0, PREVIEW_LIMIT);
    }

    private static String trimToNull(String text) {
        if (text == null) return null;
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
