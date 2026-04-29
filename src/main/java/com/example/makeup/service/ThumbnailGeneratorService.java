package com.example.makeup.service;

import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

@Service
@Slf4j
public class ThumbnailGeneratorService {

    private static final int DEFAULT_WIDTH = 400;
    private static final int DEFAULT_HEIGHT = 300;

    public BufferedImage generateThumbnail(MultipartFile videoFile) {
        return generateThumbnail(videoFile, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    public BufferedImage generateThumbnail(MultipartFile videoFile, int width, int height) {
        File tempFile = null;
        FFmpegFrameGrabber grabber = null;

        try {
            tempFile = File.createTempFile("video", ".mp4");
            videoFile.transferTo(tempFile);

            grabber = new FFmpegFrameGrabber(tempFile);
            grabber.start();

            // Пропускаем первые кадры до 1 секунды
            int targetFrameNumber = (int) grabber.getFrameRate(); // кадр на 1 секунде

            // Перематываем на нужный кадр
            grabber.setFrameNumber(targetFrameNumber);

            // Захватываем кадр
            org.bytedeco.javacv.Frame frame = grabber.grabImage();

            if (frame == null) {
                // Если не удалось получить кадр на 1 секунде, берем первый доступный
                grabber.setFrameNumber(0);
                frame = grabber.grabImage();
            }

            BufferedImage image;
            try (Java2DFrameConverter converter = new Java2DFrameConverter()) {
                image = converter.convert(frame);
            }

            if (image == null) {
                throw new RuntimeException("Failed to extract frame from video");
            }

            return resizeImage(image, width, height);

        } catch (Exception e) {
            log.error("Failed to generate thumbnail: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate thumbnail", e);
        } finally {
            try {
                if (grabber != null) {
                    grabber.stop();
                    grabber.close();
                }
            } catch (Exception e) {
                log.warn("Error closing grabber: {}", e.getMessage());
            }
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    private BufferedImage resizeImage(BufferedImage original, int width, int height) {
        Image scaled = original.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.drawImage(scaled, 0, 0, null);
        g.dispose();
        return resized;
    }
}