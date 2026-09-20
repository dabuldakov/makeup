-- =====================================================
-- Дополнительные индексы для hot-путей.
--   videos.uploaded_by    — findByUploadedBy
--   videos.views          — findMostPopular (ORDER BY views DESC)
--   news.author_id        — FK, выборки по автору
--   news.video_id         — FK, выборки по связанному видео
-- =====================================================

create index if not exists idx_videos_uploaded_by on videos (uploaded_by);
create index if not exists idx_videos_views on videos (views desc);
create index if not exists idx_news_author_id on news (author_id);
create index if not exists idx_news_video_id on news (video_id);