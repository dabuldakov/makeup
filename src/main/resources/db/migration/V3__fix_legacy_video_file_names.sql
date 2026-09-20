-- =====================================================
-- Починка legacy-данных: восстановление расширений у видео.
--
-- Баг cef8773/cf28aea: объект в MinIO кладётся под ключом с расширением,
-- а в БД сохраняются ключи БЕЗ расширения (иногда с лишним префиксом).
-- Реальные объекты в бакетах (проверено по makeup_minio_data):
--   videos бакет     -> <uuid>.mp4 и т.д.  (файла без расширения нет)
--   thumbnails бакет -> <uuid>.jpeg        (файла без расширения нет)
--
-- Целевые форматы (как их пишет текущий код 074fa74):
--   videos.file_name       = <uuid>.<ext>
--   videos.file_path       = videos/<uuid>.<ext>
--   videos.thumbnail_path  = <uuid>.jpeg     (БЕЗ префикса thumbnails/,
--                                             бакет определяется кодом)
--
-- Миграция идемпотентна: каждый regex триггерится ТОЛЬКО на legacy-строках.
-- =====================================================

-- 1. file_name: голый uuid -> <uuid><ext>. Расширение восстанавливаем
--    по videos.content_type тем же правилом, что MinioService.getExtensionFromContentType().
update videos
set file_name = file_name || case lower(content_type)
        when 'video/mp4'        then '.mp4'
        when 'video/mpeg'       then '.mpeg'
        when 'video/quicktime'  then '.mov'
        when 'video/x-msvideo'  then '.avi'
        when 'video/webm'       then '.webm'
        when 'video/x-matroska' then '.mkv'
        when 'video/ogg'        then '.ogv'
        when 'video/3gpp'       then '.3gp'
        when 'video/x-flv'      then '.flv'
        else '.mp4'
    end
where file_name ~ '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$';

-- 2. file_path: голый uuid или videos/<uuid> -> videos/<uuid><ext>
--    (префикс videos/ сохраняется, если уже есть).
update videos
set file_path = file_path || case lower(content_type)
        when 'video/mp4'        then '.mp4'
        when 'video/mpeg'       then '.mpeg'
        when 'video/quicktime'  then '.mov'
        when 'video/x-msvideo'  then '.avi'
        when 'video/webm'       then '.webm'
        when 'video/x-matroska' then '.mkv'
        when 'video/ogg'        then '.ogv'
        when 'video/3gpp'       then '.3gp'
        when 'video/x-flv'      then '.flv'
        else '.mp4'
    end
where file_path ~ '^(videos/)?[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$';

-- 3. thumbnail_path: голый uuid ИЛИ thumbnails/<uuid> -> <uuid>.jpeg
--    (лишний префикс thumbnails/ срезается — код сам выбирает бакет).
update videos
set thumbnail_path = regexp_replace(thumbnail_path, '^thumbnails/', '') || '.jpeg'
where thumbnail_path ~ '^(thumbnails/)?[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$';