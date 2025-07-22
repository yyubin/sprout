package sprout.server.websocket;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class WebSocketFrameTest {

    @Test
    @DisplayName("생성자로 전달된 속성들을 올바르게 반환해야 한다")
    void should_return_correct_properties() {
        // given
        boolean fin = true;
        int opcode = 0x1; // TEXT frame
        InputStream payloadStream = new ByteArrayInputStream(new byte[0]);

        // when
        WebSocketFrame frame = new WebSocketFrame(fin, opcode, payloadStream);

        // then
        assertEquals(fin, frame.isFin());
        assertEquals(opcode, frame.getOpcode());
        // 동일한 스트림 인스턴스인지 확인
        assertSame(payloadStream, frame.getPayloadStream());
    }

    @Test
    @DisplayName("getPayloadBytes는 InputStream의 모든 데이터를 byte 배열로 반환해야 한다")
    void getPayloadBytes_should_return_all_bytes_from_stream() throws IOException {
        // given
        byte[] originalPayload = "Hello, Stream!".getBytes(StandardCharsets.UTF_8);
        InputStream payloadStream = new ByteArrayInputStream(originalPayload);
        WebSocketFrame frame = new WebSocketFrame(true, 0x2, payloadStream); // BINARY frame

        // when
        byte[] resultPayload = frame.getPayloadBytes();

        // then
        assertArrayEquals(originalPayload, resultPayload);
    }

    @Test
    @DisplayName("getTextPayload는 InputStream의 데이터를 UTF-8 문자열로 반환해야 한다")
    void getTextPayload_should_return_utf8_string_from_stream() throws IOException {
        // given
        String originalText = "안녕하세요, 웹소켓!";
        byte[] originalPayload = originalText.getBytes(StandardCharsets.UTF_8);
        InputStream payloadStream = new ByteArrayInputStream(originalPayload);
        WebSocketFrame frame = new WebSocketFrame(true, 0x1, payloadStream); // TEXT frame

        // when
        String resultText = frame.getTextPayload();

        // then
        assertEquals(originalText, resultText);
    }

    @Test
    @DisplayName("getPayloadBytes를 두 번 호출하면 두 번째는 빈 배열을 반환해야 한다 (스트림 소모)")
    void getPayloadBytes_should_consume_the_stream() throws IOException {
        // given
        byte[] originalPayload = "some data".getBytes();
        InputStream payloadStream = new ByteArrayInputStream(originalPayload);
        WebSocketFrame frame = new WebSocketFrame(true, 0x2, payloadStream);

        // when: 첫 번째 호출
        byte[] firstResult = frame.getPayloadBytes();

        // when: 두 번째 호출
        byte[] secondResult = frame.getPayloadBytes();

        // then
        assertArrayEquals(originalPayload, firstResult, "첫 번째 호출은 전체 페이로드를 반환해야 합니다.");
        assertEquals(0, secondResult.length, "두 번째 호출은 스트림이 소모되어 빈 배열을 반환해야 합니다.");
    }

    @Test
    @DisplayName("빈 스트림이 주어지면 getPayloadBytes는 빈 배열을 반환해야 한다")
    void getPayloadBytes_should_return_empty_array_for_empty_stream() throws IOException {
        // given
        InputStream payloadStream = new ByteArrayInputStream(new byte[0]);
        WebSocketFrame frame = new WebSocketFrame(true, 0x2, payloadStream);

        // when
        byte[] resultPayload = frame.getPayloadBytes();

        // then
        assertEquals(0, resultPayload.length);
    }
}