package com.example.makeup.video;

/**
 * Разобранный HTTP-диапазон байт (заголовок {@code Range}) для файла
 * известного размера. Границы включительные, как в RFC 7233.
 */
public record ByteRange(long start, long end) {

    public long length() {
        return end - start + 1;
    }

    public String contentRangeHeader(long fileSize) {
        return "bytes " + start + "-" + end + "/" + fileSize;
    }
}
