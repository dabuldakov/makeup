package com.example.makeup.video;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RangeHeaderTest {

    private static final long SIZE = 4L;

    @Test
    void nullHeaderMeansFullFile() {
        assertNull(RangeHeader.parse(null, SIZE));
    }

    @Test
    void nonBytesHeaderIsIgnored() {
        assertNull(RangeHeader.parse("items=0-1", SIZE));
    }

    @Test
    void plainByteRange() {
        ByteRange range = RangeHeader.parse("bytes=0-1", SIZE);

        assertEquals(0L, range.start());
        assertEquals(1L, range.end());
        assertEquals(2L, range.length());
        assertEquals("bytes 0-1/4", range.contentRangeHeader(SIZE));
    }

    @Test
    void openEndedRangeGoesToFileEnd() {
        ByteRange range = RangeHeader.parse("bytes=2-", SIZE);

        assertEquals(2L, range.start());
        assertEquals(3L, range.end());
        assertEquals(2L, range.length());
    }

    @Test
    void rangeBeyondFileEndIsClamped() {
        ByteRange range = RangeHeader.parse("bytes=0-100", SIZE);

        assertEquals(0L, range.start());
        assertEquals(3L, range.end());
        assertEquals(4L, range.length());
    }

    @Test
    void suffixRangeReturnsTail() {
        ByteRange range = RangeHeader.parse("bytes=-2", SIZE);

        assertEquals(2L, range.start());
        assertEquals(3L, range.end());
        assertEquals(2L, range.length());
    }

    @Test
    void suffixLargerThanFileReturnsWholeFile() {
        ByteRange range = RangeHeader.parse("bytes=-100", SIZE);

        assertEquals(0L, range.start());
        assertEquals(3L, range.end());
        assertEquals(4L, range.length());
    }

    @Test
    void multipleRangesUseTheFirst() {
        ByteRange range = RangeHeader.parse("bytes=0-1,3-4", SIZE);

        assertEquals(0L, range.start());
        assertEquals(1L, range.end());
    }

    @Test
    void surroundingWhitespaceIsTolerated() {
        ByteRange range = RangeHeader.parse("  bytes=1-2  ", SIZE);

        assertEquals(1L, range.start());
        assertEquals(2L, range.end());
    }

    @Test
    void nonNumericRangeIsRejected() {
        assertThrows(RangeHeader.RangeNotSatisfiableException.class,
                () -> RangeHeader.parse("bytes=abc", SIZE));
    }

    @Test
    void dashOnlyRangeIsRejected() {
        assertThrows(RangeHeader.RangeNotSatisfiableException.class,
                () -> RangeHeader.parse("bytes=-", SIZE));
    }

    @Test
    void startAtOrAfterFileEndIsRejected() {
        assertThrows(RangeHeader.RangeNotSatisfiableException.class,
                () -> RangeHeader.parse("bytes=4-5", SIZE));
        assertThrows(RangeHeader.RangeNotSatisfiableException.class,
                () -> RangeHeader.parse("bytes=100-200", SIZE));
    }

    @Test
    void reversedRangeIsRejected() {
        assertThrows(RangeHeader.RangeNotSatisfiableException.class,
                () -> RangeHeader.parse("bytes=3-1", SIZE));
    }

    @Test
    void zeroSuffixIsRejected() {
        assertThrows(RangeHeader.RangeNotSatisfiableException.class,
                () -> RangeHeader.parse("bytes=-0", SIZE));
    }

    @Test
    void rangeForEmptyFileIsRejected() {
        assertThrows(RangeHeader.RangeNotSatisfiableException.class,
                () -> RangeHeader.parse("bytes=0-0", 0L));
    }

    @Test
    void malformedHeaderWithoutDashIsRejected() {
        assertThrows(RangeHeader.RangeNotSatisfiableException.class,
                () -> RangeHeader.parse("bytes=5", SIZE));
    }
}
