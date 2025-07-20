package sprout.server.websocket;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultWebSocketContainerTest {
    private DefaultWebSocketContainer container;

    @Mock
    private WebSocketSession session1;

    @Mock
    private WebSocketSession session2;

    @Mock
    private WebSocketSession session3;

    @BeforeEach
    void setUp() {
        container = new DefaultWebSocketContainer();

        // 각 목(Mock) 세션이 getId() 호출 시 정의된 ID를 반환하도록 설정
        lenient().when(session1.getId()).thenReturn("id-1");
        lenient().when(session2.getId()).thenReturn("id-2");
        lenient().when(session3.getId()).thenReturn("id-3");
    }

    @Test
    @DisplayName("세션을 특정 경로에 추가하고 ID로 조회할 수 있다.")
    void addSession_andGetById() {
        // given
        String path = "/chat";

        // when
        container.addSession(path, session1);
        WebSocketSession foundSession = container.getSession("id-1");

        // then
        assertNotNull(foundSession);
        assertSame(session1, foundSession, "추가한 세션과 조회된 세션이 동일한 인스턴스여야 합니다.");
    }

    @Test
    @DisplayName("세션을 경로와 ID로 정확히 제거할 수 있다.")
    void removeSession_shouldRemoveCorrectSession() {
        // given
        String path = "/chat";
        container.addSession(path, session1);
        container.addSession(path, session2);

        // when
        container.removeSession(path, "id-1");

        // then
        assertNull(container.getSession("id-1"), "제거된 세션은 조회되지 않아야 합니다.");
        assertNotNull(container.getSession("id-2"), "다른 세션은 남아있어야 합니다.");
    }

    @Test
    @DisplayName("존재하지 않는 세션을 제거해도 오류가 발생하지 않는다.")
    void removeSession_forNonExistentSession() {
        // given
        String path = "/chat";
        container.addSession(path, session1);

        // when & then
        assertDoesNotThrow(() -> {
            container.removeSession(path, "non-existent-id"); // 존재하지 않는 ID 제거
            container.removeSession("/other-path", "id-1"); // 존재하지 않는 경로에서 제거
        });
        assertNotNull(container.getSession("id-1"), "기존 세션은 그대로 남아있어야 합니다.");
    }

    @Test
    @DisplayName("특정 경로의 모든 세션을 조회할 수 있다.")
    void getSessions_shouldReturnAllSessionsForPath() {
        // given
        String chatPath = "/chat";
        String notificationPath = "/notifications";
        container.addSession(chatPath, session1);
        container.addSession(chatPath, session2);
        container.addSession(notificationPath, session3);

        // when
        Collection<WebSocketSession> chatSessions = container.getSessions(chatPath);
        Collection<WebSocketSession> notificationSessions = container.getSessions(notificationPath);

        // then
        assertEquals(2, chatSessions.size(), "/chat 경로에는 2개의 세션이 있어야 합니다.");
        assertTrue(chatSessions.contains(session1));
        assertTrue(chatSessions.contains(session2));

        assertEquals(1, notificationSessions.size(), "/notifications 경로에는 1개의 세션이 있어야 합니다.");
        assertTrue(notificationSessions.contains(session3));
    }

    @Test
    @DisplayName("세션이 없는 경로 조회 시 빈 컬렉션을 반환한다.")
    void getSessions_shouldReturnEmptyCollectionForUnknownPath() {
        // when
        Collection<WebSocketSession> sessions = container.getSessions("/unknown-path");

        // then
        assertNotNull(sessions, "반환된 컬렉션은 null이 아니어야 합니다.");
        assertTrue(sessions.isEmpty(), "세션이 없는 경우 빈 컬렉션을 반환해야 합니다.");
    }
}