package com.example.makeup.report;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContentReportRepository extends JpaRepository<ContentReport, Long> {

    Optional<ContentReport> findByReportUuid(UUID reportUuid);

    Optional<ContentReport> findByReporterIdAndContentTypeAndContentId(
            Long reporterId,
            ContentReport.ContentType contentType,
            Long contentId
    );
}
