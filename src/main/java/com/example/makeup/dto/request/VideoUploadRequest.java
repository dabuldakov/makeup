package com.example.makeup.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class VideoUploadRequest {
    @NotBlank
    private String title;

    private String description;

    private MultipartFile file;
}