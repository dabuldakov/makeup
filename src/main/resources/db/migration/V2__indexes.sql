-- =====================================================
-- Индексы под реальные hot-пути (раньше часть из них не создавалась).
-- =====================================================

-- Лента видео: findAllProjected ORDER BY created_at DESC, id DESC
create index idx_videos_created_at on videos (created_at desc, id desc);

-- Популярное: ORDER BY views DESC (только опубликованные)
create index idx_videos_views on videos (views desc) where status = 'PUBLISHED';

-- Видео пользователя: findByUploadedBy
create index idx_videos_uploaded_by on videos (uploaded_by);

-- Лента новостей: WHERE status='PUBLISHED' AND deleted_at IS NULL ORDER BY published_at DESC
create index idx_news_published_at on news (published_at desc)
    where status = 'PUBLISHED' and deleted_at is null;

-- Выборки по автору и связанному видео
create index idx_news_author_id on news (author_id);
create index idx_news_video_id on news (video_id);

-- Лайки: выборка по видео (PK покрывает user_id-префикс и пару)
create index idx_video_likes_video on video_likes (video_id);

-- Поиск по названию (LOWER(title) LIKE '%...%')
create index idx_videos_title_trgm on videos using gin (title gin_trgm_ops);
create index idx_news_title_trgm on news using gin (title gin_trgm_ops);

-- Очередь фоновых задач: выборка PENDING
create index idx_media_jobs_pending on media_jobs (status, created_at)
    where status = 'PENDING';
