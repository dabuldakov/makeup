-- =====================================================
-- Починка legacy-данных: восстановление расширений у видео.
--
-- В версии cef8773/cf28aea MinioService.uploadVideo() загружал объект в MinIO
-- под ключом fileId + getExtensionFromContentType(contentType), но ВОЗВРАЩАЛ
-- голый fileId (uuid без расширения). VideoService сохранял возвращённое
-- значение в videos.file_name / videos.file_path. Аналогичный баг был у
-- uploadThumbnail(): объект = file_id + '.jpeg', в БД попадал голый uuid.
--
-- Итог: у legacy-строк в БД file_name/thumbnail_path = голый uuid, а реальные
-- объекты в MinIO лежат под ключами <uuid>.<ext> / <uuid>.jpeg. Клиент запрашивал
-- /api/videos/stream/<uuid> и получал NoSuchKey (404 / 400).
--
-- Миграция восстанавливает суффикс расширения, не трогая корректные строки
-- (reg-экспрессия матчит только «голый uuid»). Идемпотентна.
-- =====================================================

-- 1. Расширение видеофайла — по videos.content_type, тем же правилом,
--    что и MinioService.getExtensionFromContentType().
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
    end,
    file_path = file_path || case lower(content_type)
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

-- 2. Legacy-превью: объект в бакете thumbnails = <uuid>.jpeg.
update videos
set thumbnail_path = thumbnail_path || '.jpeg'
where thumbnail_path ~ '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$';