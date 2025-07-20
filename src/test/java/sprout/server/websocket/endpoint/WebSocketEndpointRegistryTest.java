package sprout.server.websocket.endpoint;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sprout.mvc.mapping.PathPattern;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WebSocketEndpointRegistryTest {
    private WebSocketEndpointRegistry registry;
    private DummyWebSocketHandler handler1;
    private DummyWebSocketHandler handler2;
    private Method onOpenMethod, onCloseMethod, onErrorMethod, onMessageMethod;

    // 테스트에 사용될 더미 핸들러 클래스
    private static class DummyWebSocketHandler {
        public void onOpen() {}
        public void onClose() {}
        public void onError(Throwable t) {}
        public String onMessage(String msg) { return msg; }
    }

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        registry = new WebSocketEndpointRegistry();
        handler1 = new DummyWebSocketHandler();
        handler2 = new DummyWebSocketHandler();

        // 테스트에 사용할 Method 객체들을 미리 준비합니다.
        onOpenMethod = DummyWebSocketHandler.class.getMethod("onOpen");
        onCloseMethod = DummyWebSocketHandler.class.getMethod("onClose");
        onErrorMethod = DummyWebSocketHandler.class.getMethod("onError", Throwable.class);
        onMessageMethod = DummyWebSocketHandler.class.getMethod("onMessage", String.class);
    }

    @Test
    @DisplayName("새로운 엔드포인트를 성공적으로 등록하고 조회할 수 있다.")
    void registerAndGetSimpleEndpoint() {
        // given
        PathPattern pathPattern = new PathPattern("/echo");
        Map<String, Method> messageMappings = Collections.singletonMap("/message", onMessageMethod);

        // when
        registry.registerEndpoint(pathPattern, handler1, onOpenMethod, onCloseMethod, onErrorMethod, messageMappings);
        WebSocketEndpointInfo foundInfo = registry.getEndpointInfo("/echo");

        // then
        assertNotNull(foundInfo, "등록된 엔드포인트를 찾아야 합니다.");
        assertEquals(handler1, foundInfo.getHandlerBean(), "핸들러 인스턴스가 일치해야 합니다.");
        assertEquals(onOpenMethod, foundInfo.getOnOpenMethod(), "OnOpen 메서드가 일치해야 합니다.");
    }

    @Test
    @DisplayName("경로 변수가 포함된 엔드포인트를 등록하고 조회할 수 있다.")
    void registerAndGetEndpointWithPathVariables() {
        // given
        PathPattern pathPattern = new PathPattern("/chat/{roomId}");

        // when
        registry.registerEndpoint(pathPattern, handler1, onOpenMethod, onCloseMethod, onErrorMethod, Collections.emptyMap());
        WebSocketEndpointInfo foundInfo = registry.getEndpointInfo("/chat/general");

        // then
        assertNotNull(foundInfo, "경로 변수를 포함한 엔드포인트를 찾아야 합니다.");
        assertEquals(handler1, foundInfo.getHandlerBean());
    }

    @Test
    @DisplayName("매칭되는 엔드포인트가 없으면 null을 반환한다.")
    void getEndpointInfo_shouldReturnNullForNoMatch() {
        // given
        registry.registerEndpoint(new PathPattern("/echo"), handler1, null, null, null, Collections.emptyMap());

        // when
        WebSocketEndpointInfo foundInfo = registry.getEndpointInfo("/nonexistent/path");

        // then
        assertNull(foundInfo, "매칭되는 경로가 없으면 null을 반환해야 합니다.");
    }

    @Test
    @DisplayName("동일한 경로에 중복 등록 시 마지막에 등록된 핸들러로 덮어쓴다.")
    void registerDuplicateEndpoint_shouldOverwrite() {
        // given
        PathPattern pathPattern = new PathPattern("/updates");

        // when
        registry.registerEndpoint(pathPattern, handler1, null, null, null, Collections.emptyMap());
        registry.registerEndpoint(pathPattern, handler2, null, null, null, Collections.emptyMap()); // handler2로 덮어쓰기
        WebSocketEndpointInfo foundInfo = registry.getEndpointInfo("/updates");

        // then
        assertNotNull(foundInfo);
        assertEquals(handler2, foundInfo.getHandlerBean(), "마지막에 등록된 핸들러가 조회되어야 합니다.");
    }

    @Test
    @DisplayName("필수 인자가 null일 경우 NullPointerException을 던진다.")
    void registerEndpoint_shouldThrowExceptionForNullArguments() {
        // when & then
        assertThrows(NullPointerException.class, () -> {
            registry.registerEndpoint(null, handler1, null, null, null, Collections.emptyMap());
        }, "PathPattern이 null이면 예외가 발생해야 합니다.");

        assertThrows(NullPointerException.class, () -> {
            registry.registerEndpoint(new PathPattern("/test"), null, null, null, null, Collections.emptyMap());
        }, "HandlerBean이 null이면 예외가 발생해야 합니다.");
    }
}