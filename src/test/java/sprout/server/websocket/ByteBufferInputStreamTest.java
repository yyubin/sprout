package sprout.server.websocket;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

class ByteBufferInputStreamTest {

    private static ByteBufferInputStream make(String data) {
        return new ByteBufferInputStream(ByteBuffer.wrap(data.getBytes()));
    }

    @Nested
    @DisplayName("read() 단일 바이트")
    class SingleByteRead {

        @Test
        @DisplayName("끝까지 읽으면 -1 반환")
        void read_all_then_eof() throws IOException {
            ByteBufferInputStream in = make("XYZ");

            assertEquals('X', in.read());
            assertEquals('Y', in.read());
            assertEquals('Z', in.read());
            assertEquals(-1, in.read());   // EOF
        }
    }

    @Nested
    @DisplayName("read(byte[], off, len)")
    class BulkRead {

        @Test
        @DisplayName("len > 남은 byte 수 → 실제 남은 만큼만 읽고 반환값은 읽은 수")
        void read_array() throws IOException {
            ByteBufferInputStream in = make("HELLO");
            byte[] buf = new byte[10];

            int n = in.read(buf, 2, buf.length - 2);  // offset=2
            assertEquals(5, n);                       // “HELLO” 5 byte
            assertArrayEquals(
                    new byte[]{0, 0, 'H', 'E', 'L', 'L', 'O', 0, 0, 0},
                    buf
            );
            assertEquals(-1, in.read());              // EOF
        }
    }

    @Nested
    @DisplayName("available()")
    class Available {

        @Test
        @DisplayName("읽을 때마다 감소한다")
        void available_decreases() throws IOException {
            ByteBufferInputStream in = make("ABCD");

            assertEquals(4, in.available());
            in.read();                    // 1 byte
            assertEquals(3, in.available());
            in.read(new byte[2], 0, 2);   // 2 byte
            assertEquals(1, in.available());
        }
    }

    @Test
    @DisplayName("빈 버퍼는 바로 EOF 와 available=0")
    void empty_buffer() throws IOException {
        ByteBufferInputStream in = new ByteBufferInputStream(ByteBuffer.allocate(0));
        assertEquals(0, in.available());
        assertEquals(-1, in.read());
    }
}
