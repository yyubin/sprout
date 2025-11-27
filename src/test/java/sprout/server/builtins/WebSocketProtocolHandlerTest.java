package sprout.server.builtins;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sprout.mvc.http.HttpMethod;
import sprout.mvc.http.HttpRequest;
import sprout.mvc.http.parser.HttpRequestParser;
import sprout.mvc.mapping.PathPattern;
import sprout.server.argument.WebSocketArgumentResolver;
import sprout.server.websocket.*;
import sprout.server.websocket.endpoint.WebSocketEndpointInfo;
import sprout.server.websocket.endpoint.WebSocketEndpointRegistry;
import sprout.server.websocket.handler.WebSocketHandshakeHandler;
import sprout.server.websocket.message.WebSocketMessageDispatcher;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketProtocolHandlerTest {

    // 수많은 의존성을 모두 Mock으로 처리
    @Mock private WebSocketHandshakeHandler mockHandshakeHandler;
    @Mock private WebSocketContainer mockWebSocketContainer;
    @Mock private WebSocketEndpointRegistry mockEndpointRegistry;
    @Mock private HttpRequestParser mockHttpRequestParser;
    @Mock private WebSocketFrameParser mockFrameParser;
    @Mock private WebSocketFrameEncoder mockFrameEncoder;
    @Mock private List<WebSocketArgumentResolver> mockArgumentResolvers;
    @Mock private List<WebSocketMessageDispatcher> mockMessageDispatchers;
    @Mock private CloseListener mockCloseListener;

    @Mock private SocketChannel mockChannel;
    @Mock private Selector mockSelector;
    @Mock private Socket mockSocket;
    @Mock private SelectionKey mockSelectionKey;

    @InjectMocks
    private WebSocketProtocolHandler webSocketProtocolHandler;

    private final String fakeHandshakeRequest = "GET /chat HTTP/1.1\r\n" +
            "Host: example.com\r\n" +
            "Upgrade: websocket\r\n" +
            "Connection: Upgrade\r\n" +
            "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==\r\n" +
            "Sec-WebSocket-Version: 13\r\n\r\n";

    @BeforeEach
    void setUp() throws Exception {
        // 테스트 전반에 걸쳐 필요한 기본 Mock 동작 설정
        lenient().when(mockChannel.socket()).thenReturn(mockSocket);
        lenient().when(mockChannel.register(any(Selector.class), anyInt())).thenReturn(mockSelectionKey);
    }

    @Test
    @DisplayName("supports 메서드는 'WEBSOCKET' 프로토콜을 지원해야 한다")
    void supports_should_return_true_for_websocket_protocol() {
        assertTrue(webSocketProtocolHandler.supports("WEBSOCKET"));
        assertFalse(webSocketProtocolHandler.supports("HTTP/1.1"));
    }

    @Test
    @DisplayName("핸드셰이크 성공 시 WebSocket 세션을 생성하고 등록해야 한다")
    void accept_should_establish_session_on_successful_handshake() throws Exception {
        // given: I/O 스트림 및 요청 객체 준비
        InputStream inputStream = new ByteArrayInputStream(fakeHandshakeRequest.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        lenient().when(mockSocket.getInputStream()).thenReturn(inputStream);
        lenient().when(mockSocket.getOutputStream()).thenReturn(outputStream);

        HttpRequest<?> mockRequest = new HttpRequest<>(HttpMethod.GET, "/chat", null, new HashMap<>(), new HashMap<>());
        WebSocketEndpointInfo mockEndpointInfo = new WebSocketEndpointInfo(new PathPattern("/chat"), new Object(), null, null, null, new HashMap<>());

        // given: 의존성 Mock 동작 설정 (성공 경로)
        doReturn(mockRequest).when(mockHttpRequestParser).parse(anyString());
        when(mockEndpointRegistry.getEndpointInfo("/chat")).thenReturn(mockEndpointInfo);
        when(mockHandshakeHandler.performHandshake(any(), any())).thenReturn(true);

        // when: 핸들러 실행
        webSocketProtocolHandler.accept(mockChannel, mockSelector, ByteBuffer.allocate(0));

        // then: 주요 메서드 호출 검증
        verify(mockHttpRequestParser).parse(anyString());
        verify(mockEndpointRegistry).getEndpointInfo("/chat");
        verify(mockHandshakeHandler).performHandshake(any(), any());

        // then: 세션 생성 및 등록 검증
        ArgumentCaptor<WebSocketSession> sessionCaptor = ArgumentCaptor.forClass(WebSocketSession.class);
        verify(mockWebSocketContainer).addSession(eq("/chat"), sessionCaptor.capture());
        assertNotNull(sessionCaptor.getValue()); // 세션이 실제로 생성되었는지 확인

        // then: 채널 등록 및 Key 첨부 검증
        verify(mockChannel).register(mockSelector, SelectionKey.OP_READ);
        verify(mockSelectionKey).attach(sessionCaptor.getValue());
    }

    @DisplayName("엔드포인트를 찾지 못하면 404 응답을 보내고 연결을 닫아야 한다")
    void accept_should_send_404_when_endpoint_not_found() throws Exception {
        // given
        InputStream inputStream = new ByteArrayInputStream(fakeHandshakeRequest.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        when(mockSocket.getInputStream()).thenReturn(inputStream);
        when(mockSocket.getOutputStream()).thenReturn(outputStream);

        HttpRequest<?> mockRequest = new HttpRequest<>(HttpMethod.GET, "/not-found", null, new HashMap<>(), new HashMap<>());

        // given: 엔드포인트 레지스트리가 null을 반환하도록 설정
        doReturn(mockRequest).when(mockHttpRequestParser).parse(anyString());
        when(mockEndpointRegistry.getEndpointInfo("/not-found")).thenReturn(null);

        // when
        webSocketProtocolHandler.accept(mockChannel, mockSelector, ByteBuffer.allocate(0));

        // then: 404 응답 검증
        String response = outputStream.toString(StandardCharsets.UTF_8);
        assertTrue(response.startsWith("HTTP/1.1 404 Not Found"));
        assertTrue(response.contains("No WebSocket endpoint found for /not-found"));

        // then: 연결이 닫혔는지, 후속 작업이 없는지 검증
        verify(mockSocket).close();
        verifyNoInteractions(mockHandshakeHandler, mockWebSocketContainer);
    }

    @Test
    @DisplayName("핸드셰이크에 실패하면 연결을 닫아야 한다")
    void accept_should_close_channel_when_handshake_fails() throws Exception {
        // given
        InputStream inputStream = new ByteArrayInputStream(fakeHandshakeRequest.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        lenient().when(mockSocket.getInputStream()).thenReturn(inputStream);
        lenient().when(mockSocket.getOutputStream()).thenReturn(outputStream);

        HttpRequest<?> mockRequest = new HttpRequest<>(HttpMethod.GET, "/chat", null, new HashMap<>(), new HashMap<>());
        WebSocketEndpointInfo mockEndpointInfo = new WebSocketEndpointInfo(new PathPattern("/chat"), new Object(), null, null, null, new HashMap<>());

        // given: 핸드셰이크 핸들러가 false를 반환하도록 설정
        doReturn(mockRequest).when(mockHttpRequestParser).parse(anyString());
        when(mockEndpointRegistry.getEndpointInfo("/chat")).thenReturn(mockEndpointInfo);
        when(mockHandshakeHandler.performHandshake(any(), any())).thenReturn(false);

        // when
        webSocketProtocolHandler.accept(mockChannel, mockSelector, ByteBuffer.allocate(0));

        // then: 채널이 닫혔는지, 후속 작업이 없는지 검증
        verify(mockChannel).close();
        verifyNoInteractions(mockWebSocketContainer);
        verify(mockSelectionKey, never()).attach(any());
    }
}