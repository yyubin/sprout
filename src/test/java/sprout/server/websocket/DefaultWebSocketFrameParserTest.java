package sprout.server.websocket;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class DefaultWebSocketFrameParserTest {
    private final DefaultWebSocketFrameParser parser = new DefaultWebSocketFrameParser();

    @Test
    @DisplayName("마스킹되지 않은 짧은 텍스트 프레임 파싱 테스트")
    void parse_UnmaskedShortFrame() throws Exception {
        byte[] payload = "Hello".getBytes(StandardCharsets.UTF_8);
        byte[] frameBytes = new byte[2 + payload.length];
        frameBytes[0] = (byte) 0x81; // FIN + TEXT
        frameBytes[1] = (byte) payload.length;
        System.arraycopy(payload, 0, frameBytes, 2, payload.length);

        ByteArrayInputStream in = new ByteArrayInputStream(frameBytes);
        WebSocketFrame frame = parser.parse(in);

        assertTrue(frame.isFin());
        assertEquals(0x1, frame.getOpcode());
        assertArrayEquals(payload, frame.getPayloadStream().readAllBytes());
    }

    @Test
    @DisplayName("마스킹된 짧은 텍스트 프레임 파싱 테스트")
    void parse_MaskedShortFrame() throws Exception {
        byte[] originalPayload = "WebSocket".getBytes(StandardCharsets.UTF_8);
        byte[] maskingKey = new byte[]{(byte) 0x1a, (byte) 0x2b, (byte) 0x3c, (byte) 0x4d};
        byte[] maskedPayload = new byte[originalPayload.length];
        for (int i = 0; i < originalPayload.length; i++) {
            maskedPayload[i] = (byte) (originalPayload[i] ^ maskingKey[i % 4]);
        }

        ByteArrayOutputStream frameStream = new ByteArrayOutputStream();
        frameStream.write((byte) 0x81); // FIN + TEXT
        frameStream.write((byte) (0x80 | originalPayload.length)); // MASKED + length
        frameStream.write(maskingKey);
        frameStream.write(maskedPayload);

        ByteArrayInputStream in = new ByteArrayInputStream(frameStream.toByteArray());
        WebSocketFrame frame = parser.parse(in);

        assertTrue(frame.isFin());
        assertEquals(0x1, frame.getOpcode());
        assertArrayEquals(originalPayload, frame.getPayloadStream().readAllBytes());
    }

    @Test
    @DisplayName("마스킹되지 않은 중간 길이 프레임 파싱 테스트")
    void parse_UnmaskedMediumFrame() throws Exception {
        byte[] payload = "M".repeat(500).getBytes(StandardCharsets.UTF_8);
        int len = payload.length;

        ByteArrayOutputStream frameStream = new ByteArrayOutputStream();
        frameStream.write((byte) 0x81); // FIN + TEXT
        frameStream.write((byte) 126); // length indicator
        frameStream.write((byte) (len >> 8));
        frameStream.write((byte) len);
        frameStream.write(payload);

        ByteArrayInputStream in = new ByteArrayInputStream(frameStream.toByteArray());
        WebSocketFrame frame = parser.parse(in);

        assertTrue(frame.isFin());
        assertEquals(0x1, frame.getOpcode());
        assertArrayEquals(payload, frame.getPayloadStream().readAllBytes());
    }

    @Test
    @DisplayName("스트림이 예기치 않게 종료될 경우 예외 발생 테스트")
    void parse_UnexpectedEndOfStream() {
        byte[] incompleteFrame = new byte[]{(byte) 0x81}; // 헤더가 불완전함
        ByteArrayInputStream in = new ByteArrayInputStream(incompleteFrame);

        assertThrows(RuntimeException.class, () -> {
            parser.parse(in);
        }, "Unexpected end of stream while reading frame header.");
    }

}