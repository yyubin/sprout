package sprout.server.websocket;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class DefaultWebSocketFrameEncoderTest {

    private final DefaultWebSocketFrameEncoder encoder = new DefaultWebSocketFrameEncoder();

    @Test
    @DisplayName("컨트롤 프레임(PING) 인코딩 테스트")
    void encodeControlFrame_Ping() {
        byte[] payload = "Hello".getBytes(StandardCharsets.UTF_8);
        byte[] frame = encoder.encodeControlFrame(0x9, payload); // Opcode 9 for Ping

        assertEquals(2 + payload.length, frame.length);
        assertEquals((byte) (0x80 | 0x9), frame[0]); // FIN + Opcode
        assertEquals((byte) payload.length, frame[1]); // Payload length
        assertArrayEquals(payload, Arrays.copyOfRange(frame, 2, frame.length));
    }

    @Test
    @DisplayName("컨트롤 프레임 페이로드 125바이트 초과 시 예외 발생 테스트")
    void encodeControlFrame_PayloadTooBig() {
        byte[] payload = new byte[126]; // 125 초과

        assertThrows(IllegalArgumentException.class, () -> {
            encoder.encodeControlFrame(0x8, payload); // Opcode 8 for Close
        });
    }

    @Test
    @DisplayName("짧은 텍스트 메시지(< 126 바이트) 인코딩 테스트")
    void encodeText_ShortMessage() {
        String message = "Hello, WebSocket!";
        byte[] payload = message.getBytes(StandardCharsets.UTF_8);
        byte[] frame = encoder.encodeText(message);

        assertEquals(2 + payload.length, frame.length);
        assertEquals((byte) 0x81, frame[0]); // FIN + Text Opcode
        assertEquals((byte) payload.length, frame[1]);
        assertArrayEquals(payload, Arrays.copyOfRange(frame, 2, frame.length));
    }

    @Test
    @DisplayName("중간 길이 텍스트 메시지(126 <= length <= 65535) 인코딩 테스트")
    void encodeText_MediumMessage() {
        String message = "A".repeat(300);
        byte[] payload = message.getBytes(StandardCharsets.UTF_8);
        byte[] frame = encoder.encodeText(message);

        assertEquals(4 + payload.length, frame.length);
        assertEquals((byte) 0x81, frame[0]);
        assertEquals((byte) 126, frame[1]);
        assertEquals((byte) ((payload.length >> 8) & 0xFF), frame[2]); // Length MSB
        assertEquals((byte) (payload.length & 0xFF), frame[3]); // Length LSB
        assertArrayEquals(payload, Arrays.copyOfRange(frame, 4, frame.length));
    }

    @Test
    @DisplayName("긴 텍스트 메시지(> 65535 바이트) 인코딩 테스트")
    void encodeText_LongMessage() {
        // 65536 bytes
        String message = "L".repeat(65536);
        byte[] payload = message.getBytes(StandardCharsets.UTF_8);
        long payloadLen = payload.length;
        byte[] frame = encoder.encodeText(message);

        assertEquals(10 + payload.length, frame.length);
        assertEquals((byte) 0x81, frame[0]);
        assertEquals((byte) 127, frame[1]);

        // 8-byte length check
        for (int i = 0; i < 8; i++) {
            assertEquals((byte) ((payloadLen >> (8 * (7 - i))) & 0xFF), frame[2 + i]);
        }
        assertArrayEquals(payload, Arrays.copyOfRange(frame, 10, frame.length));
    }

}