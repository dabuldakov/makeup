package com.example.makeup.report;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateReportRequestDto {

    @NotNull(message = "contentType is required")
    private ContentReport.ContentType contentType;

    @NotNull(message = "contentId is required")
    private Long contentId;

    @NotNull(message = "reason is required")
    private ContentReport.Reason reason;

    @Size(max = 1000, message = "comment must be at most 1000 characters")
    private String comment;
}
