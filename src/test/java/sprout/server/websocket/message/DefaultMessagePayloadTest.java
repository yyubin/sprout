package sprout.server.websocket.message;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class DefaultMessagePayloadTest {

    @Nested
    @DisplayName("isText() & isBinary()")
    class TypeChecks {
        @Test
        @DisplayName("stringPayload가 존재하면 isText()는 true")
        void textPayload_true() {
            DefaultMessagePayload payload =
                    new DefaultMessagePayload("Hello", null);
            assertTrue(payload.isText());
            assertFalse(payload.isBinary());
        }

        @Test
        @DisplayName("binaryPayload가 존재하면 isBinary()는 true")
        void binaryPayload_true() {
            byte[] bin = "bin".getBytes(StandardCharsets.UTF_8);
            DefaultMessagePayload payload =
                    new DefaultMessagePayload(null, bin);
            assertTrue(payload.isBinary());
            assertFalse(payload.isText()); // null stringPayload
        }

        @Test
        @DisplayName("빈 payload는 둘 다 false")
        void emptyPayload_false() {
            DefaultMessagePayload payload =
                    new DefaultMessagePayload("", new byte[0]);
            assertFalse(payload.isText());
            assertFalse(payload.isBinary());
        }
    }

    @Nested
    @DisplayName("asText() & asBinary()")
    class Conversion {
        @Test
        @DisplayName("isText() true 시 asBinary()는 UTF-8 인코딩 결과")
        void text_to_binary() {
            String text = "Hello";
            DefaultMessagePayload payload =
                    new DefaultMessagePayload(text, null);

            byte[] result = payload.asBinary();
            assertArrayEquals(text.getBytes(StandardCharsets.UTF_8), result);
        }

        @Test
        @DisplayName("isBinary() true 시 asText()는 UTF-8 디코딩 결과")
        void binary_to_text() {
            String original = "BinaryText";
            byte[] bin = original.getBytes(StandardCharsets.UTF_8);
            DefaultMessagePayload payload =
                    new DefaultMessagePayload(null, bin);

            String text = payload.asText();
            assertEquals(original, text);
        }

        @Test
        @DisplayName("둘 다 값이 있으면 우선순위 규칙 확인")
        void both_text_and_binary() {
            String text = "Hello";
            byte[] bin = "World".getBytes(StandardCharsets.UTF_8);

            DefaultMessagePayload payload =
                    new DefaultMessagePayload(text, bin);

            assertTrue(payload.isText());
            assertTrue(payload.isBinary());
            assertEquals("World", payload.asText());
            assertArrayEquals("Hello".getBytes(StandardCharsets.UTF_8), payload.asBinary());
        }
    }
}
