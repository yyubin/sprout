package app.benchmark;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sprout.server.websocket.CloseCode;
import sprout.server.websocket.CloseCodes;
import sprout.server.websocket.WebSocketSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketBenchmarkHandlerTest {

    @Mock(lenient = true)
    private WebSocketSession mockSession1;

    @Mock(lenient = true)
    private WebSocketSession mockSession2;

    private WebSocketBenchmarkHandler handler;

    @BeforeEach
    void setUp() {
        handler = new WebSocketBenchmarkHandler();
        WebSocketBenchmarkHandler.resetStats();

        when(mockSession1.getId()).thenReturn("session-1");
        when(mockSession2.getId()).thenReturn("session-2");
        when(mockSession1.isOpen()).thenReturn(true);
        when(mockSession2.isOpen()).thenReturn(true);
        when(mockSession1.getUserProperties()).thenReturn(new HashMap<>());
        when(mockSession2.getUserProperties()).thenReturn(new HashMap<>());
    }

    @Test
    @DisplayName("OnOpen 메서드는 세션을 맵에 추가하고 로그를 출력한다")
    void onOpen_shouldAddSessionToMap() {
        // when
        handler.onOpen(mockSession1);

        // then
        String stats = WebSocketBenchmarkHandler.getStats();
        assertThat(stats).contains("Sessions: 1");
        assertThat(stats).contains("Total Connections: 1");
    }

    @Test
    @DisplayName("OnClose 메서드는 세션을 맵에서 제거한다")
    void onClose_shouldRemoveSessionFromMap() {
        // given
        handler.onOpen(mockSession1);
        handler.onOpen(mockSession2);

        // when
        handler.onClose(mockSession1, CloseCodes.NORMAL_CLOSURE);

        // then
        String stats = WebSocketBenchmarkHandler.getStats();
        assertThat(stats).contains("Sessions: 1");
    }

    @Test
    @DisplayName("Echo 메시지는 그대로 반환된다")
    void handleEcho_shouldEchoMessage() throws IOException {
        // given
        handler.onOpen(mockSession1);
        String testMessage = "Hello Echo";

        // when
        handler.handleEcho(mockSession1, testMessage);

        // then
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockSession1).sendText(captor.capture());

        String sentMessage = captor.getValue();
        assertThat(sentMessage).contains("\"destination\":\"/echo\"");
        assertThat(sentMessage).contains("\"payload\":\"" + testMessage + "\"");

        String stats = WebSocketBenchmarkHandler.getStats();
        assertThat(stats).contains("Received: 1");
        assertThat(stats).contains("Sent: 1");
    }

    @Test
    @DisplayName("Broadcast 메시지는 모든 세션에 전송된다")
    void handleBroadcast_shouldSendToAllSessions() throws IOException {
        // given
        handler.onOpen(mockSession1);
        handler.onOpen(mockSession2);
        String testMessage = "Broadcast Test";

        // when
        handler.handleBroadcast(mockSession1, testMessage);

        // then
        verify(mockSession1).sendText(anyString());
        verify(mockSession2).sendText(anyString());

        String stats = WebSocketBenchmarkHandler.getStats();
        assertThat(stats).contains("Received: 1");
        assertThat(stats).contains("Sent: 2"); // 2개 세션에 전송
    }

    @Test
    @DisplayName("Chat 메시지는 username을 포함하여 전송된다")
    void handleChat_shouldIncludeUsername() throws IOException {
        // given
        handler.onOpen(mockSession1);
        Map<String, Object> userProps = new HashMap<>();
        when(mockSession1.getUserProperties()).thenReturn(userProps);
        String testMessage = "Chat message";

        // when
        handler.handleChat(mockSession1, testMessage);

        // then
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockSession1).sendText(captor.capture());

        String sentMessage = captor.getValue();
        assertThat(sentMessage).contains("\"destination\":\"/chat\"");
        assertThat(sentMessage).contains("User-"); // auto-generated username
        assertThat(sentMessage).contains(testMessage);
    }

    @Test
    @DisplayName("Stats 메시지는 현재 통계를 반환한다")
    void handleStats_shouldReturnStatistics() throws IOException {
        // given
        WebSocketBenchmarkHandler.resetStats(); // 명시적 리셋
        handler.onOpen(mockSession1);
        handler.handleEcho(mockSession1, "test");

        // when
        handler.handleStats(mockSession1, "");

        // then
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockSession1, times(2)).sendText(captor.capture()); // echo + stats

        String statsMessage = captor.getAllValues().get(1);
        assertThat(statsMessage).contains("\"destination\":\"/stats\"");
        assertThat(statsMessage).contains("연결:");
        assertThat(statsMessage).contains("수신:");
        assertThat(statsMessage).contains("송신:");
    }

    @Test
    @DisplayName("닫힌 세션에는 브로드캐스트하지 않는다")
    void handleBroadcast_shouldSkipClosedSessions() throws IOException {
        // given
        handler.onOpen(mockSession1);
        handler.onOpen(mockSession2);
        when(mockSession2.isOpen()).thenReturn(false); // session2는 닫힘

        // when
        handler.handleBroadcast(mockSession1, "test");

        // then
        verify(mockSession1).sendText(anyString());
        verify(mockSession2, never()).sendText(anyString()); // 닫힌 세션에는 전송 안 함
    }

    @Test
    @DisplayName("JSON 특수문자는 올바르게 이스케이프된다")
    void shouldEscapeJsonSpecialCharacters() throws IOException {
        // given
        handler.onOpen(mockSession1);
        String messageWithQuotes = "Test \"quoted\" message";

        // when
        handler.handleEcho(mockSession1, messageWithQuotes);

        // then
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockSession1).sendText(captor.capture());

        String sentMessage = captor.getValue();
        assertThat(sentMessage).contains("\\\"quoted\\\""); // 따옴표가 이스케이프되어야 함
    }
}
