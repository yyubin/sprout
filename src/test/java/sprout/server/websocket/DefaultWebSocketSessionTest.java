//package sprout.server.websocket;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.Spy;
//import org.mockito.junit.jupiter.MockitoExtension;
//import sprout.mvc.http.HttpRequest;
//import sprout.server.argument.WebSocketArgumentResolver;
//import sprout.server.websocket.DispatchResult;
//import sprout.server.websocket.endpoint.WebSocketEndpointInfo;
//import sprout.server.websocket.message.MessagePayload;
//import sprout.server.websocket.message.WebSocketMessageDispatcher;
//
//import java.io.*;
//import java.lang.reflect.Method;
//import java.net.Socket;
//import java.util.Collections;
//import java.util.List;
//import java.util.Map;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class DefaultWebSocketSessionTest {
//
//    // --- Mocks for Dependencies ---
//    @Mock private Socket mockSocket;
//    @Mock private HttpRequest<?> mockHandshakeRequest;
//    @Mock private WebSocketEndpointInfo mockEndpointInfo;
//    @Mock private WebSocketFrameParser mockFrameParser;
//    @Mock private WebSocketFrameEncoder mockFrameEncoder;
//    @Mock private List<WebSocketArgumentResolver> mockArgumentResolvers;
//    @Mock private List<WebSocketMessageDispatcher> mockMessageDispatchers;
//    @Mock private Map<String, String> mockPathParameters;
//
//    // --- Spies for I/O Streams ---
//    // 실제 스트림 동작을 확인하기 위해 Spy를 사용
//    @Spy private ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[0]);
//    @Spy private ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//
//    // --- Class Under Test ---
//    // @InjectMocks는 생성자 주입을 시도하지만, I/O 스트림은 수동으로 설정해야 함
//    private DefaultWebSocketSession session;
//
//    @BeforeEach
//    void setUp() throws IOException {
//        // Socket이 Spy 스트림을 반환하도록 설정
//        when(mockSocket.getInputStream()).thenReturn(inputStream);
//        when(mockSocket.getOutputStream()).thenReturn(outputStream);
//
//        // 테스트 대상 클래스 수동 생성
//        session = new DefaultWebSocketSession(
//                "test-session-id",
//                mockSocket,
//                mockHandshakeRequest,
//                mockEndpointInfo,
//                mockFrameParser,
//                mockFrameEncoder,
//                mockPathParameters,
//                mockArgumentResolvers,
//                mockMessageDispatchers
//        );
//    }
//
//    @Nested
//    @DisplayName("Send Methods Test")
//    class SendMethodsTest {
//        @Test
//        @DisplayName("sendText는 frameEncoder를 호출하고 결과를 OutputStream에 써야 한다.")
//        void sendText_shouldWriteEncodedFrameToOutput() throws IOException {
//            // given
//            String message = "Hello";
//            byte[] encodedFrame = new byte[]{1, 2, 3};
//            when(mockFrameEncoder.encodeText(message)).thenReturn(encodedFrame);
//
//            // when
//            session.sendText(message);
//
//            // then
//            verify(mockFrameEncoder).encodeText(message);
//            assertArrayEquals(encodedFrame, outputStream.toByteArray());
//        }
//
//        @Test
//        @DisplayName("sendPing은 control frame을 인코딩하여 OutputStream에 써야 한다.")
//        void sendPing_shouldWriteEncodedControlFrameToOutput() throws IOException {
//            // given
//            byte[] payload = new byte[]{4, 5};
//            byte[] encodedFrame = new byte[]{6, 7, 8};
//            when(mockFrameEncoder.encodeControlFrame(0x9, payload)).thenReturn(encodedFrame);
//
//            // when
//            session.sendPing(payload);
//
//            // then
//            verify(mockFrameEncoder).encodeControlFrame(0x9, payload);
//            assertArrayEquals(encodedFrame, outputStream.toByteArray());
//        }
//    }
//
//    @Nested
//    @DisplayName("Lifecycle Callback Test")
//    class LifecycleCallbackTest {
//        @Test
//        @DisplayName("callOnOpenMethod는 EndpointInfo에서 메서드를 찾아 실행해야 한다.")
//        void callOnOpenMethod_shouldInvokeCorrectMethod() throws Exception {
//            // given
//            // 실제 메서드를 모킹하기 위해 더미 핸들러 클래스 사용
//            class DummyHandler { public void onOpen(WebSocketSession s) {} }
//            Method onOpenMethod = DummyHandler.class.getMethod("onOpen", WebSocketSession.class);
//            Object handlerBean = new DummyHandler();
//
//            when(mockEndpointInfo.getOnOpenMethod()).thenReturn(onOpenMethod);
//            when(mockEndpointInfo.getHandlerBean()).thenReturn(handlerBean);
//
//            // ArgumentResolver 설정
//            WebSocketArgumentResolver mockResolver = mock(WebSocketArgumentResolver.class);
//            when(mockArgumentResolvers.iterator()).thenReturn(List.of(mockResolver).iterator());
//            when(mockResolver.supports(any(), any())).thenReturn(true);
//            when(mockResolver.resolve(any(), any())).thenReturn(session);
//
//            // when
//            session.callOnOpenMethod();
//
//            // then
//            // 실제 invoke가 일어나는지 검증하기는 복잡하므로,
//            // resolveArgs가 올바르게 호출되는지 간접적으로 확인
//            verify(mockResolver, times(1)).resolve(any(), any());
//        }
//    }
//
//    @Nested
//    @DisplayName("Message Dispatch Test")
//    class MessageDispatchTest {
//        @Mock private WebSocketFrame mockFrame;
//        @Mock private WebSocketMessageDispatcher mockDispatcher;
//        @Mock private MessagePayload mockPayload;
//
//        @Test
//        @DisplayName("단일 텍스트 프레임 수신 시 dispatcher를 호출해야 한다.")
//        void dispatchMessage_shouldCallDispatcherForSingleTextFrame() throws Exception {
//            // given
//            String content = "Hello";
//            InputStream payloadStream = new ByteArrayInputStream(content.getBytes());
//            when(mockFrame.isFin()).thenReturn(true);
//            when(mockFrame.getOpcode()).thenReturn(0x1); // TEXT
//            when(mockFrame.getPayloadStream()).thenReturn(payloadStream);
//
//            when(mockMessageDispatchers.iterator()).thenReturn(List.of(mockDispatcher).iterator());
//            when(mockDispatcher.supports(any(), any())).thenReturn(true);
//            when(mockDispatcher.dispatch(any(), any())).thenReturn(new DispatchResult(true, true));
//
//            // when
//            session.dispatchMessage(mockFrame);
//
//            // then
//            verify(mockDispatcher).dispatch(eq(mockFrame), any(InvocationContext.class));
//        }
//
//        @Test
//        @DisplayName("분할된 텍스트 프레임 수신 시 버퍼링 후 마지막에 dispatcher를 호출해야 한다.")
//        void dispatchMessage_shouldBufferAndDispatchFragmentedFrames() throws Exception {
//            // --- Part 1: First fragment ---
//            String part1 = "Hello ";
//            InputStream stream1 = new ByteArrayInputStream(part1.getBytes());
//            when(mockFrame.isFin()).thenReturn(false);
//            when(mockFrame.getOpcode()).thenReturn(0x1); // TEXT
//            when(mockFrame.getPayloadStream()).thenReturn(stream1);
//
//            session.dispatchMessage(mockFrame);
//
//            // 첫 프레임 후에는 dispatcher가 호출되면 안 됨
//            verify(mockDispatcher, never()).dispatch(any(), any());
//
//            // --- Part 2: Final fragment ---
//            String part2 = "World";
//            InputStream stream2 = new ByteArrayInputStream(part2.getBytes());
//            when(mockFrame.isFin()).thenReturn(true);
//            when(mockFrame.getOpcode()).thenReturn(0x0); // CONTINUATION
//            when(mockFrame.getPayloadStream()).thenReturn(stream2);
//
//            when(mockMessageDispatchers.iterator()).thenReturn(List.of(mockDispatcher).iterator());
//            when(mockDispatcher.supports(any(), any())).thenReturn(true);
//            when(mockDispatcher.dispatch(any(), any())).thenReturn(new DispatchResult(true, true));
//
//            session.dispatchMessage(mockFrame);
//
//            // 마지막 프레임 후에 dispatcher가 호출되어야 함
//            verify(mockDispatcher, times(1)).dispatch(eq(mockFrame), any(InvocationContext.class));
//        }
//    }
//}