package com.example.makeup.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ThumbnailGeneratorService {

    private static final int DEFAULT_WIDTH = 400;
    private static final int DEFAULT_HEIGHT = 300;

    public BufferedImage generateThumbnail(MultipartFile videoFile) {
        return generateThumbnail(videoFile, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    public BufferedImage generateThumbnail(MultipartFile videoFile, int width, int height) {
        File tempVideoFile = null;
        File tempThumbnailFile = null;

        try {
            // Создаем временные файлы
            tempVideoFile = File.createTempFile("video_", ".mp4");
            tempThumbnailFile = File.createTempFile("thumbnail_", ".jpg");

            // Сохраняем загруженное видео
            videoFile.transferTo(tempVideoFile);
            log.info("Video saved to temp file: {}", tempVideoFile.getAbsolutePath());

            // Генерируем превью через FFmpeg
            generateThumbnailWithFFmpeg(tempVideoFile.getAbsolutePath(), tempThumbnailFile.getAbsolutePath());

            // Загружаем сгенерированное превью
            BufferedImage thumbnail = ImageIO.read(tempThumbnailFile);

            if (thumbnail == null) {
                throw new RuntimeException("Failed to read generated thumbnail");
            }

            // Изменяем размер
            return resizeImage(thumbnail, width, height);

        } catch (Exception e) {
            log.error("Failed to generate thumbnail: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate thumbnail", e);
        } finally {
            // Очищаем временные файлы
            cleanupFiles(tempVideoFile, tempThumbnailFile);
        }
    }

    private void generateThumbnailWithFFmpeg(String videoPath, String thumbnailPath) {
        try {
            // Проверяем существование FFmpeg
            if (!isFFmpegAvailable()) {
                throw new RuntimeException("FFmpeg is not installed or not available in PATH");
            }

            // Команда для извлечения кадра на 1 секунде
            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg",
                    "-i", videoPath,           // входной файл
                    "-ss", "00:00:01",         // ищем кадр на 1 секунде
                    "-vframes", "1",           // берем только один кадр
                    "-vf", "scale=400:-1",     // масштабируем до ширины 400 пикселей
                    "-y",                      // перезаписываем файл
                    thumbnailPath              // выходной файл
            );

            pb.redirectErrorStream(true);

            log.info("Executing FFmpeg command: {}", String.join(" ", pb.command()));

            Process process = pb.start();

            // Читаем вывод для отладки
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            // Ждем завершения с таймаутом
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

            // Проверяем, что файл был создан
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
        try {
            ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-version");
            Process process = pb.start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);

            if (finished && process.exitValue() == 0) {
                // Читаем версию для лога
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String version = reader.readLine();
                    log.info("FFmpeg is available: {}", version);
                }
                return true;
            }
        } catch (Exception e) {
            log.warn("FFmpeg not available: {}", e.getMessage());
        }
        return false;
    }

    private BufferedImage resizeImage(BufferedImage original, int width, int height) {
        if (original == null) {
            throw new RuntimeException("Original image is null");
        }

        // Создаем новое изображение с нужным размером
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();

        // Включаем антиалиасинг для лучшего качества
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Рисуем масштабированное изображение
        g.drawImage(original, 0, 0, width, height, null);
        g.dispose();

        return resized;
    }

    private void cleanupFiles(File... files) {
        for (File file : files) {
            if (file != null && file.exists()) {
                try {
                    Files.deleteIfExists(file.toPath());
                    log.debug("Deleted temp file: {}", file.getAbsolutePath());
                } catch (Exception e) {
                    log.warn("Failed to delete temp file: {}", file.getAbsolutePath(), e);
                }
            }
        }
    }
}