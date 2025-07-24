package sprout.server.websocket.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.*;

class DefaultWebSocketMessageParserTest {

    private final ObjectMapper om = new ObjectMapper();
    private final DefaultWebSocketMessageParser parser =
            new DefaultWebSocketMessageParser(om);

    @Test
    @DisplayName("올바른 메시지 → destination·payload 추출")
    void parse_validMessage() throws Exception {
        String json = """
            {
              "destination": "/topic/chat",
              "payload": { "text": "hello" }
            }
            """;

        ParsedMessage msg = parser.parse(json);

        assertThat(msg.getDestination()).isEqualTo("/topic/chat");
        assertThat(msg.getPayload()).isEqualTo("{\"text\":\"hello\"}");
    }

    @Test
    @DisplayName("payload 가 배열이어도 그대로 문자열 형태로 보존한다")
    void parse_arrayPayload() throws Exception {
        String json = """
            {
              "destination": "/numbers",
              "payload": [1,2,3]
            }
            """;

        ParsedMessage msg = parser.parse(json);

        assertThat(msg.getDestination()).isEqualTo("/numbers");
        assertThat(msg.getPayload()).isEqualTo("[1,2,3]");
    }

    @Nested
    @DisplayName("예외 케이스")
    class InvalidCases {

        @Test
        @DisplayName("destination 필드가 없으면 IOException")
        void missingDestination() {
            String json = """
                { "payload": { "x":1 } }
                """;

            assertThatThrownBy(() -> parser.parse(json))
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining("Invalid WebSocket message");
        }

        @Test
        @DisplayName("payload 필드가 없으면 IOException")
        void missingPayload() {
            String json = """
                { "destination": "/foo" }
                """;

            assertThatThrownBy(() -> parser.parse(json))
                    .isInstanceOf(IOException.class);
        }

        @Test
        @DisplayName("destination 이 text가 아니면(IOException)")
        void destinationNotText() {
            String json = """
                { "destination": 123, "payload": {} }
                """;

            assertThatThrownBy(() -> parser.parse(json))
                    .isInstanceOf(IOException.class);
        }

        @Test
        @DisplayName("JSON 자체가 invalid 면 JsonProcessingException → IOException 상위포착")
        void invalidJson() {
            String json = "{ this is not json }";

            assertThatThrownBy(() -> parser.parse(json))
                    .isInstanceOf(IOException.class); // Jackson이 JsonProcessingException 던짐
        }
    }
}
