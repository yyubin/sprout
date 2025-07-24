package sprout.server.websocket;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class MaskingInputStreamTest {

    private static byte[] mask(byte[] data, byte[] key) {
        byte[] m = data.clone();
        for (int i = 0; i < m.length; i++) {
            m[i] ^= key[i % 4];
        }
        return m;
    }

    @Nested
    @DisplayName("생성자 검증")
    class Ctor {

        @Test
        @DisplayName("마스킹 키가 4바이트가 아니면 IllegalArgumentException")
        void key_must_be_4bytes() {
            byte[] bad = {1, 2, 3};
            assertThrows(IllegalArgumentException.class,
                    () -> new MaskingInputStream(new ByteArrayInputStream(new byte[0]), bad));
        }

        @Test
        @DisplayName("null 키면 IllegalArgumentException")
        void key_null() {
            assertThrows(IllegalArgumentException.class,
                    () -> new MaskingInputStream(new ByteArrayInputStream(new byte[0]), null));
        }
    }

    @Nested
    @DisplayName("read() 단일 바이트")
    class SingleByteRead {

        @Test
        @DisplayName("한 바이트씩 읽어도 원본 데이터와 동일해야 한다")
        void read_one_by_one() throws IOException {
            byte[] key   = {(byte) 0x37, (byte) 0xfa, (byte) 0x21, (byte) 0x3d};
            byte[] plain = "HelloWebSocket".getBytes();
            byte[] masked = mask(plain, key);

            try (InputStream in =
                         new MaskingInputStream(new ByteArrayInputStream(masked), key)) {

                for (byte b : plain) {
                    int r = in.read();
                    assertEquals(b & 0xFF, r);
                }
                assertEquals(-1, in.read()); // EOF
            }
        }
    }

    @Nested
    @DisplayName("read(byte[], off, len)")
    class BufferRead {

        @Test
        @DisplayName("버퍼 길이·오프셋 상관없이 언마스킹 결과가 동일")
        void read_into_buffer_with_offset() throws IOException {
            byte[] key   = {0x11, 0x22, 0x33, 0x44};
            byte[] plain = "MaskingStreamOffset!".getBytes();
            byte[] masked = mask(plain, key);

            try (InputStream in =
                         new MaskingInputStream(new ByteArrayInputStream(masked), key)) {

                byte[] buf = new byte[plain.length + 5];          // 5바이트 더 크게
                int n = in.read(buf, 2, plain.length);            // offset 2
                assertEquals(plain.length, n);

                byte[] actual = new byte[plain.length];
                System.arraycopy(buf, 2, actual, 0, plain.length);
                assertArrayEquals(plain, actual);
            }
        }

        @Test
        @DisplayName("버퍼 크기 & 데이터 크기가 1024 경계를 넘더라도 올바르게 읽힘")
        void big_payload() throws IOException {
            byte[] key = {0x01, 0x02, 0x03, 0x04};
            byte[] plain = new byte[3_000];
            new Random(42).nextBytes(plain);
            byte[] masked = mask(plain, key);

            try (InputStream in =
                         new MaskingInputStream(new ByteArrayInputStream(masked), key)) {

                byte[] read = in.readAllBytes();
                assertArrayEquals(plain, read);
            }
        }
    }
}
