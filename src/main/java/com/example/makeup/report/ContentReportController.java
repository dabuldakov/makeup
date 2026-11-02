package com.example.makeup.report;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ContentReportController {

    private final ContentReportService reportService;

    @PostMapping
    public ResponseEntity<ContentReportDto> createReport(
            @Valid @RequestBody CreateReportRequestDto request,
            Authentication authentication) {
        ContentReport report = reportService.createReport(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ContentReportDto.fromEntity(report));
    }
}
