package com.example.makeup.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ThumbnailGeneratorService {

    private static final int DEFAULT_WIDTH = 400;
    private static final int DEFAULT_HEIGHT = 300;

    private volatile Boolean ffmpegAvailable;

    public BufferedImage generateThumbnail(Path videoPath) {
        return generateThumbnail(videoPath, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    public BufferedImage generateThumbnail(Path videoPath, int width, int height) {
        File tempThumbnailFile = null;
        try {
            tempThumbnailFile = File.createTempFile("thumbnail_", ".jpg");

            generateThumbnailWithFFmpeg(videoPath.toAbsolutePath().toString(),
                    tempThumbnailFile.getAbsolutePath());

            BufferedImage thumbnail = ImageIO.read(tempThumbnailFile);
            if (thumbnail == null) {
                throw new RuntimeException("Failed to read generated thumbnail");
            }

            return resizeImage(thumbnail, width, height);
        } catch (Exception e) {
            log.error("Failed to generate thumbnail: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate thumbnail", e);
        } finally {
            deleteQuietly(tempThumbnailFile);
        }
    }

    private void generateThumbnailWithFFmpeg(String videoPath, String thumbnailPath) {
        try {
            if (!isFFmpegAvailable()) {
                throw new RuntimeException("FFmpeg is not installed or not available in PATH");
            }

            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg",
                    "-i", videoPath,
                    "-ss", "00:00:01",
                    "-vframes", "1",
                    "-vf", "scale=400:-1",
                    "-y",
                    thumbnailPath
            );

            pb.redirectErrorStream(true);
            log.info("Executing FFmpeg command: {}", String.join(" ", pb.command()));

            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("FFmpeg process timed out after 10 seconds");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                log.error("FFmpeg failed with exit code: {}", exitCode);
                log.error("FFmpeg output: {}", output);
                throw new RuntimeException("FFmpeg failed with exit code: " + exitCode);
            }

            File thumbnailFile = new File(thumbnailPath);
            if (!thumbnailFile.exists() || thumbnailFile.length() == 0) {
                throw new RuntimeException("Thumbnail file was not created or is empty");
            }

            log.info("Thumbnail generated successfully: {} (size: {} bytes)",
                    thumbnailPath, thumbnailFile.length());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("FFmpeg process was interrupted", e);
        } catch (Exception e) {
            log.error("Error generating thumbnail with FFmpeg", e);
            throw new RuntimeException("Failed to generate thumbnail with FFmpeg: " + e.getMessage(), e);
        }
    }

    private boolean isFFmpegAvailable() {
        Boolean cached = ffmpegAvailable;
        if (cached != null) {
            return cached;
        }
        try {
            ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-version");
            Process process = pb.start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);

            if (finished && process.exitValue() == 0) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String version = reader.readLine();
                    log.info("FFmpeg is available: {}", version);
                }
                ffmpegAvailable = true;
                return true;
            }
        } catch (Exception e) {
            log.warn("FFmpeg not available: {}", e.getMessage());
        }
        ffmpegAvailable = false;
        return false;
    }

    private BufferedImage resizeImage(BufferedImage original, int width, int height) {
        if (original == null) {
            throw new RuntimeException("Original image is null");
        }

        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(original, 0, 0, width, height, null);
        g.dispose();

        return resized;
    }

    private void deleteQuietly(File file) {
        if (file != null && file.exists()) {
            try {
                java.nio.file.Files.deleteIfExists(file.toPath());
            } catch (Exception e) {
                log.warn("Failed to delete temp file: {}", file.getAbsolutePath(), e);
            }
        }
    }
}