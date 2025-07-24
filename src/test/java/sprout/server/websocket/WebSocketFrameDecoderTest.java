package sprout.server.websocket;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WebSocketFrameDecoderTest {

    @Nested
    @DisplayName("decodeText(InputStream)")
    class DecodeText {

        @Test
        @DisplayName("UTF-8 문자열을 끝까지 읽는다(버퍼 크기 초과 포함)")
        void read_all_utf8() throws Exception {
            // 2KB 이상으로 만들어 1024 버퍼를 여러 번 돌도록
            String base = "한글😊abcDEF123!";
            String text = base.repeat(200);
            InputStream in = new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));

            String decoded = WebSocketFrameDecoder.decodeText(in);

            assertEquals(text, decoded);
        }

        @Test
        @DisplayName("빈 스트림이면 빈 문자열 반환")
        void empty_stream() throws Exception {
            InputStream in = new ByteArrayInputStream(new byte[0]);
            assertEquals("", WebSocketFrameDecoder.decodeText(in));
        }
    }

    @Nested
    @DisplayName("frame opcode helpers")
    class OpcodeHelpers {

        @ParameterizedTest(name = "opcode {0} -> isCloseFrame={1}, isPingFrame={2}, isPongFrame={3}, isDataFrame={4}")
        @CsvSource({
                "8,true,false,false,false",   // CLOSE
                "9,false,true,false,false",   // PING
                "10,false,false,true,false",  // PONG
                "0,false,false,false,true",   // CONTINUATION
                "1,false,false,false,true",   // TEXT
                "2,false,false,false,true",   // BINARY
                "3,false,false,false,false"   // 기타
        })
        void helpers(int opcode, boolean close, boolean ping, boolean pong, boolean data) {
            WebSocketFrame f = mock(WebSocketFrame.class);
            when(f.getOpcode()).thenReturn(opcode);

            assertEquals(close, WebSocketFrameDecoder.isCloseFrame(f));
            assertEquals(ping,  WebSocketFrameDecoder.isPingFrame(f));
            assertEquals(pong,  WebSocketFrameDecoder.isPongFrame(f));
            assertEquals(data,  WebSocketFrameDecoder.isDataFrame(f));
        }
    }

    @Nested
    @DisplayName("getCloseCode(byte[])")
    class CloseCodeParsing {

        @Test
        @DisplayName("payload null 또는 2바이트 미만이면 NO_STATUS_CODE")
        void short_payload() {
            assertEquals(CloseCodes.NO_STATUS_CODE, WebSocketFrameDecoder.getCloseCode(null));
            assertEquals(CloseCodes.NO_STATUS_CODE, WebSocketFrameDecoder.getCloseCode(new byte[0]));
            assertEquals(CloseCodes.NO_STATUS_CODE, WebSocketFrameDecoder.getCloseCode(new byte[]{0x03}));
        }

        @Test
        @DisplayName("Big-Endian으로 정상 파싱 (1000 = NORMAL_CLOSURE)")
        void parse_normal_closure() {
            byte[] payload = {(byte) 0x03, (byte) 0xE8}; // 1000
            assertEquals(CloseCodes.NORMAL_CLOSURE, WebSocketFrameDecoder.getCloseCode(payload));
        }

    }
}
