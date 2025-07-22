package sprout.server.websocket.endpoint;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sprout.context.BeanFactory;
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

    @Mock WebSocketEndpointRegistry endpointRegistry;
    @Mock PathPatternResolver pathPatternResolver;
    @Mock BeanFactory beanFactory; // ← 변경된 시그니처

    WebSocketHandlerScanner scanner;

    private MyTestWebSocketHandler handlerBean;

    @BeforeEach
    void setUp() {
        scanner = new WebSocketHandlerScanner(endpointRegistry, pathPatternResolver);
        handlerBean = new MyTestWebSocketHandler();
    }

    @WebSocketHandler("/chat")
    static class MyTestWebSocketHandler {
        @OnOpen    public void handleOpen() {}
        @OnClose   public void handleClose() {}
        @OnError   public void handleError() {}
        @MessageMapping("/join") public void onJoinMessage() {}
        @MessageMapping("/send") public void onSendMessage() {}
    }
    static class NotAHandlerBean {}

    @Test
    @DisplayName("Scanner는 @WebSocketHandler 빈만 찾아 endpointRegistry에 등록한다")
    void scan_registers_expected_endpoint() {
        // given
        PathPattern mockPathPattern = new PathPattern("/chat");
        when(pathPatternResolver.resolve("/chat")).thenReturn(mockPathPattern);

        when(beanFactory.getAllBeans())
                .thenReturn(List.of(handlerBean, new NotAHandlerBean()));

        // when
        scanner.scanWebSocketHandlers(beanFactory);

        // then
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Method>> mappingsCap = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<PathPattern> pathCap = ArgumentCaptor.forClass(PathPattern.class);
        ArgumentCaptor<Object> handlerCap = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<Method> onOpenCap  = ArgumentCaptor.forClass(Method.class);
        ArgumentCaptor<Method> onCloseCap = ArgumentCaptor.forClass(Method.class);
        ArgumentCaptor<Method> onErrorCap = ArgumentCaptor.forClass(Method.class);

        verify(endpointRegistry, times(1)).registerEndpoint(
                pathCap.capture(),
                handlerCap.capture(),
                onOpenCap.capture(),
                onCloseCap.capture(),
                onErrorCap.capture(),
                mappingsCap.capture()
        );

        assertEquals("/chat", pathCap.getValue().getOriginalPattern());
        assertSame(handlerBean, handlerCap.getValue());
        assertEquals("handleOpen",  onOpenCap.getValue().getName());
        assertEquals("handleClose", onCloseCap.getValue().getName());
        assertEquals("handleError", onErrorCap.getValue().getName());

        Map<String, Method> mappings = mappingsCap.getValue();
        assertEquals(2, mappings.size());
        assertTrue(mappings.containsKey("/join"));
        assertEquals("onJoinMessage", mappings.get("/join").getName());
        assertTrue(mappings.containsKey("/send"));
        assertEquals("onSendMessage", mappings.get("/send").getName());
    }
}
