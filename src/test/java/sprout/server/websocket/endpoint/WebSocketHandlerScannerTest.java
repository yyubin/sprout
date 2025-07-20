package sprout.server.websocket.endpoint;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sprout.context.Container;
import sprout.mvc.mapping.PathPattern;
import sprout.mvc.mapping.PathPatternResolver;
import sprout.server.websocket.annotation.*;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketHandlerScannerTest {
    @Mock
    private Container container;

    @Mock
    private WebSocketEndpointRegistry endpointRegistry;

    @Mock
    private PathPatternResolver pathPatternResolver;

    @InjectMocks
    private WebSocketHandlerScanner scanner;

    private MyTestWebSocketHandler handlerBean;

    @WebSocketHandler("/chat")
    class MyTestWebSocketHandler {
        @OnOpen
        public void handleOpen() {}

        @OnClose
        public void handleClose() {}

        @OnError
        public void handleError() {}

        @MessageMapping("/join")
        public void onJoinMessage() {}

        @MessageMapping("/send")
        public void onSendMessage() {}
    }

    class NotAHandlerBean {}

    @BeforeEach
    void setUp() {
        handlerBean = new MyTestWebSocketHandler();
    }

    @Test
    @DisplayName("@WebSocketHandler가 붙은 빈을 스캔하여 엔드포인트를 정확히 등록해야 한다.")
    void scanWebSocketHandlers_shouldRegisterCorrectEndpoints() throws NoSuchMethodException {
        // given (준비)
        // 1. 컨테이너가 스캔할 빈 목록을 반환하도록 설정
        when(container.beans()).thenReturn(List.of(handlerBean, new NotAHandlerBean()));

        // 2. 경로 문자열이 들어오면 PathPattern 객체를 반환하도록 설정
        PathPattern mockPathPattern = new PathPattern("/chat");
        when(pathPatternResolver.resolve("/chat")).thenReturn(mockPathPattern);

        // when (실행)
        scanner.scanWebSocketHandlers();

        // then (검증)
        // 1. endpointRegistry.registerEndpoint 메서드가 1번만 호출되었는지 확인
        // (NotAHandlerBean은 무시되어야 함)
        verify(endpointRegistry, times(1)).registerEndpoint(
                any(PathPattern.class),
                any(Object.class),
                any(Method.class),
                any(Method.class),
                any(Method.class),
                any(Map.class)
        );

        // 2. registerEndpoint에 전달된 인자들을 캡처하여 상세히 검증
        ArgumentCaptor<PathPattern> pathPatternCaptor = ArgumentCaptor.forClass(PathPattern.class);
        ArgumentCaptor<Object> handlerCaptor = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<Method> onOpenCaptor = ArgumentCaptor.forClass(Method.class);
        ArgumentCaptor<Map<String, Method>> messageMappingsCaptor = ArgumentCaptor.forClass(Map.class);

        verify(endpointRegistry).registerEndpoint(
                pathPatternCaptor.capture(),
                handlerCaptor.capture(),
                onOpenCaptor.capture(),
                any(Method.class), // onClose
                any(Method.class), // onError
                messageMappingsCaptor.capture()
        );

        // 3. 캡처된 값들이 기대와 일치하는지 확인
        assertEquals("/chat", pathPatternCaptor.getValue().getOriginalPattern());
        assertSame(handlerBean, handlerCaptor.getValue());
        assertEquals("handleOpen", onOpenCaptor.getValue().getName());

        Map<String, Method> capturedMappings = messageMappingsCaptor.getValue();
        assertEquals(2, capturedMappings.size());
        assertTrue(capturedMappings.containsKey("/join"));
        assertEquals("onJoinMessage", capturedMappings.get("/join").getName());
        assertTrue(capturedMappings.containsKey("/send"));
        assertEquals("onSendMessage", capturedMappings.get("/send").getName());
    }

}