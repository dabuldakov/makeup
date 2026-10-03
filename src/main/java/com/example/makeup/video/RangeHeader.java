package com.example.makeup.video;

/**
 * Разбор заголовка {@code Range} (RFC 7233) в {@link ByteRange}.
 *
 * Вынесено из контроллера, чтобы логику границ можно было покрыть юнит-тестами
 * без поднятия Spring-контекста.
 */
public final class RangeHeader {

    private static final String BYTES_PREFIX = "bytes=";

    private RangeHeader() {
    }

    /** Диапазон некорректен или выходит за пределы файла → ответ 416. */
    public static class RangeNotSatisfiableException extends RuntimeException {
        public RangeNotSatisfiableException() {
            super("Range not satisfiable");
        }
    }

    /**
     * @param header   значение заголовка {@code Range} или {@code null}
     * @param fileSize размер файла в байтах
     * @return разобранный диапазон, либо {@code null}, если заголовка нет или
     * он не в формате {@code bytes=} — тогда отдаётся весь файл
     * @throws RangeNotSatisfiableException если диапазон некорректен/вне границ
     */
    public static ByteRange parse(String header, long fileSize) {
        if (header == null) {
            return null;
        }
        String trimmed = header.trim();
        if (!trimmed.startsWith(BYTES_PREFIX)) {
            return null;
        }
        String value = trimmed.substring(BYTES_PREFIX.length()).trim();
        // Поддерживаем только первый диапазон из списка.
        int comma = value.indexOf(',');
        if (comma >= 0) {
            value = value.substring(0, comma).trim();
        }

        String[] parts = value.split("-", -1);
        if (parts.length != 2) {
            throw new RangeNotSatisfiableException();
        }

        long start;
        long end;
        try {
            if (parts[0].isEmpty()) {
                // суффикс: bytes=-N — последние N байт
                if (parts[1].isEmpty()) {
                    throw new RangeNotSatisfiableException();
                }
                long suffix = Long.parseLong(parts[1]);
                if (suffix <= 0) {
                    throw new RangeNotSatisfiableException();
                }
                start = Math.max(0, fileSize - suffix);
                end = fileSize - 1;
            } else {
                start = Long.parseLong(parts[0]);
                end = parts[1].isEmpty() ? fileSize - 1 : Long.parseLong(parts[1]);
            }
        } catch (NumberFormatException e) {
            throw new RangeNotSatisfiableException();
        }

        if (fileSize <= 0 || start < 0 || start > end || start >= fileSize) {
            throw new RangeNotSatisfiableException();
        }
        end = Math.min(end, fileSize - 1);
        return new ByteRange(start, end);
    }
}
