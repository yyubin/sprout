package sprout.server.websocket;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class LimitedInputStreamTest {

    private static InputStream make(String data, long limit) {
        return new LimitedInputStream(
                new ByteArrayInputStream(data.getBytes()), limit);
    }

    @Nested
    @DisplayName("read() 단일 바이트")
    class SingleByteRead {

        @Test
        @DisplayName("limit 만큼만 읽히고 그 뒤에는 -1")
        void read_up_to_limit_then_eof() throws IOException {
            try (InputStream in = make("abcdef", 3)) {
                assertEquals('a', in.read());
                assertEquals('b', in.read());
                assertEquals('c', in.read());
                assertEquals(-1, in.read());         // limit 초과 시 EOF
            }
        }
    }

    @Nested
    @DisplayName("read(byte[], off, len)")
    class BulkRead {

        @Test
        @DisplayName("len > remaining 이면 remaining 만 반환")
        void read_byte_array_respects_remaining() throws IOException {
            byte[] buf = new byte[10];

            try (InputStream in = make("abcdefghij", 4)) {
                int n = in.read(buf, 0, buf.length);
                assertEquals(4, n);
                assertArrayEquals("abcd".getBytes(),               // 4 byte 만 복사
                        java.util.Arrays.copyOf(buf, 4));
                assertEquals(-1, in.read());                       // EOF
            }
        }
    }

    @Nested
    @DisplayName("skip()")
    class Skip {

        @Test
        @DisplayName("skip 도 limit 을 넘기지 못한다")
        void skip_respects_limit() throws IOException {
            try (InputStream in = make("0123456789", 5)) {
                long skipped = in.skip(10);     // 10 요청해도 5만
                assertEquals(5, skipped);
                assertEquals(-1, in.read());    // 모두 소비됨
            }
        }
    }

    @Nested
    @DisplayName("available()")
    class Available {

        @Test
        @DisplayName("available 은 underlying 과 limit 중 작은 값")
        void available_is_min_of_underlying_and_remaining() throws IOException {
            try (ByteArrayInputStream base =
                         new ByteArrayInputStream("abcdef".getBytes());
                 InputStream in = new LimitedInputStream(base, 3)) {

                assertEquals(3, in.available());   // limit=3 < base.available=6

                in.read();                         // 1 소비
                assertEquals(2, in.available());   // 2 남음
            }
        }
    }

    @Test
    @DisplayName("limit 이 0 이면 항상 EOF")
    void zero_limit() throws IOException {
        try (InputStream in = make("data", 0)) {
            assertEquals(-1, in.read());
        }
    }
}
