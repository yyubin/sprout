//package sprout.server.builtins;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import sprout.mvc.http.HttpRequest;
//import sprout.mvc.http.parser.HttpRequestParser;
//import sprout.mvc.mapping.PathPattern;
//import sprout.server.argument.WebSocketArgumentResolver;
//import sprout.server.websocket.*;
//import sprout.server.websocket.endpoint.WebSocketEndpointInfo;
//import sprout.server.websocket.endpoint.WebSocketEndpointRegistry;
//import sprout.server.websocket.handler.WebSocketHandshakeHandler;
//import sprout.server.websocket.message.WebSocketMessageDispatcher;
//import static org.mockito.Mockito.when;
//import java.io.*;
//import java.net.Socket;
//import java.nio.charset.StandardCharsets;
//import java.util.Collections;
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class WebSocketProtocolHandlerTest {
//    @Mock private WebSocketHandshakeHandler mockHandshakeHandler;
//    @Mock private WebSocketContainer mockWebSocketContainer;
//    @Mock private WebSocketEndpointRegistry mockEndpointRegistry;
//    @Mock private HttpRequestParser mockHttpRequestParser;
//    // ... 다른 Mock 객체들 ...
//    @Mock private Socket mockSocket;
//    @Mock private WebSocketEndpointInfo mockEndpointInfo;
//    @Mock private PathPattern mockPathPattern;
//
//    @InjectMocks
//    private WebSocketProtocolHandler protocolHandler;
//
//    private InputStream inputStream;
//    private OutputStream outputStream;
//
//    @BeforeEach
//    void setUp() throws IOException {
//        String httpRequest = "GET /ws HTTP/1.1\r\nUpgrade: websocket\r\n\r\n";
//        inputStream = new ByteArrayInputStream(httpRequest.getBytes(StandardCharsets.UTF_8));
//        outputStream = new ByteArrayOutputStream();
//
//        lenient().when(mockSocket.getInputStream()).thenReturn(inputStream);
//        lenient().when(mockSocket.getOutputStream()).thenReturn(outputStream);
//    }
//
//    @Test
//    @DisplayName("supports 메서드는 'WEBSOCKET' 프로토콜에 대해 true를 반환해야 한다.")
//    void supports_shouldReturnTrueForWebSocketProtocol() {
//        assertTrue(protocolHandler.supports("WEBSOCKET"));
//        assertFalse(protocolHandler.supports("HTTP/1.1"));
//    }
//
//    @Test
//    @DisplayName("엔드포인트를 찾지 못하면 404 응답을 보내고 소켓을 닫아야 한다.")
//    void handle_shouldSend404WhenEndpointNotFound() throws Exception {
//        // given
//        HttpRequest<?> mockRequest = mock(HttpRequest.class);
//        doReturn(mockRequest).when(mockHttpRequestParser).parse(anyString());
//        when(mockRequest.isValid()).thenReturn(true);
//        when(mockRequest.getPath()).thenReturn("/not-found");
//        when(mockEndpointRegistry.getEndpointInfo("/not-found")).thenReturn(null);
//
//        // when
//        protocolHandler.handle(mockSocket);
//
//        // then
//        String response = ((ByteArrayOutputStream) outputStream).toString(StandardCharsets.UTF_8);
//        assertTrue(response.contains("HTTP/1.1 404 Not Found"));
//        verify(mockSocket).close();
//    }
//
//    @Test
//    @DisplayName("핸드셰이크가 실패하면 조용히 소켓을 닫아야 한다.")
//    void handle_shouldCloseSocketOnFailedHandshake() throws Exception {
//        // given
//        HttpRequest<?> mockRequest = mock(HttpRequest.class);
//        doReturn(mockRequest).when(mockHttpRequestParser).parse(anyString());
//
//        when(mockRequest.isValid()).thenReturn(true);
//        when(mockRequest.getPath()).thenReturn("/ws");
//        when(mockEndpointRegistry.getEndpointInfo("/ws")).thenReturn(mockEndpointInfo);
//        when(mockHandshakeHandler.performHandshake(any(), any())).thenReturn(false);
//
//        // when
//        protocolHandler.handle(mockSocket);
//
//        // then
//        verify(mockSocket).close();
//        verify(mockWebSocketContainer, never()).addSession(any(), any());
//    }
//
//    @Test
//    @DisplayName("핸드셰이크 성공 시 로직이 정상적으로 실행되어야 한다.")
//    void handle_shouldFollowFullLifecycleOnSuccess() throws Exception {
//        // given
//        HttpRequest<?> mockRequest = mock(HttpRequest.class);
//        doReturn(mockRequest).when(mockHttpRequestParser).parse(anyString());
//        when(mockRequest.isValid()).thenReturn(true);
//        when(mockRequest.getPath()).thenReturn("/ws");
//        when(mockEndpointRegistry.getEndpointInfo("/ws")).thenReturn(mockEndpointInfo);
//        when(mockEndpointInfo.getPathPattern()).thenReturn(mockPathPattern);
//        when(mockPathPattern.getOriginalPattern()).thenReturn("/ws");
//        when(mockHandshakeHandler.performHandshake(any(), any())).thenReturn(true);
//
//        // when
//        protocolHandler.handle(mockSocket);
//
//        // then
//        // 핸드셰이크 성공 이후 로직으로 진입했는지 확인
//        verify(mockHandshakeHandler).performHandshake(any(), any());
//        verify(mockSocket, atLeastOnce()).close();
//    }
//}