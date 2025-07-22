package sprout.server.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sprout.mvc.http.HttpRequest;
import sprout.server.argument.WebSocketArgumentResolver;
import sprout.server.websocket.endpoint.WebSocketEndpointInfo;
import sprout.server.websocket.exception.NotEnoughDataException;
import sprout.server.websocket.message.WebSocketMessageDispatcher;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DefaultWebSocketSession 테스트")
class DefaultWebSocketSessionTest {

    @Mock private SocketChannel mockChannel;
    @Mock private HttpRequest<?> mockHandshakeRequest;
    @Mock private WebSocketEndpointInfo mockEndpointInfo;
    @Mock private WebSocketFrameParser mockFrameParser;
    @Mock private WebSocketFrameEncoder mockFrameEncoder;
    @Mock private List<WebSocketArgumentResolver> mockArgumentResolvers;
    @Mock private List<WebSocketMessageDispatcher> mockMessageDispatchers;
    @Mock private CloseListener mockCloseListener;
    @Mock private SelectionKey mockSelectionKey;

    private DefaultWebSocketSession session;

    @BeforeEach
    void setUp() throws IOException {
        session = new DefaultWebSocketSession(
                "test-session-id", mockChannel, mockHandshakeRequest, mockEndpointInfo,
                mockFrameParser, mockFrameEncoder, Collections.emptyMap(),
                mockArgumentResolvers, mockMessageDispatchers, mockCloseListener
        );
        lenient().when(mockChannel.isOpen()).thenReturn(true);
    }

    @Test
    @DisplayName("close 호출 시 Close 프레임을 보내고 채널을 닫고 리스너를 호출해야 한다")
    void close_should_send_frame_and_close_channel_and_notify_listener() throws Exception {
        // ① encodeText 는 최소 1바이트 이상 반환
        byte[] closeMsg = "bye".getBytes(StandardCharsets.UTF_8);
        when(mockFrameEncoder.encodeText(anyString())).thenReturn(closeMsg);

        // ② write() 가 호출될 때마다 버퍼를 모두 소비했다고 가정
        when(mockChannel.write(any(ByteBuffer.class))).thenAnswer(inv -> {
            ByteBuffer buf = inv.getArgument(0);
            int bytes = buf.remaining();   // 남은 바이트 수
            buf.position(buf.limit());     // position 을 limit 로 이동 → hasRemaining() == false
            return bytes;                  // 실제로 쓴 바이트 수 반환
        });

        // when
        session.close();

        // then
        verify(mockChannel).write(any(ByteBuffer.class));
        verify(mockChannel).close();
        verify(mockCloseListener).onSessionClosed(session);
        assertFalse(session.isOpen());
    }


    @Nested
    @DisplayName("데이터 읽기 및 프레임 처리 (Read) 로직")
    class ReadLogicTests {
        @Test
        @DisplayName("원격 연결 종료(-1) 시 onClose 메서드와 close를 호출해야 한다")
        void read_should_close_on_remote_disconnect() throws Exception {
            // given
            when(mockChannel.read(any(ByteBuffer.class))).thenReturn(-1);
            // FIX: onClose 메서드와 핸들러 빈이 null이 아니도록 설정
            when(mockEndpointInfo.getOnCloseMethod()).thenReturn(Object.class.getMethod("toString"));
            when(mockEndpointInfo.getHandlerBean()).thenReturn(new Object()); // 실제 빈 객체
            // FIX: close() 내부에서 encodeText가 호출되므로 Mocking 필요
            when(mockFrameEncoder.encodeText(anyString())).thenReturn(new byte[0]);

            // when
            session.read(mockSelectionKey);

            // then
            verify(mockEndpointInfo).getOnCloseMethod();
            verify(mockChannel).close();
        }

        @Test
        @DisplayName("Ping 프레임 수신 시 Pong 프레임을 전송해야 한다")
        void read_should_send_pong_on_ping() throws Exception {
            // given
            byte[] pingPayload = {'p','i','n','g'};
            WebSocketFrame pingFrame = new WebSocketFrame(true, 0x9,
                    new ByteArrayInputStream(pingPayload));
            byte[] raw = { (byte)0x89, 0x04,'p','i','n','g'};

            when(mockChannel.read(any(ByteBuffer.class))).thenAnswer(inv -> {
                ByteBuffer buf = inv.getArgument(0);
                buf.put(raw);
                return raw.length;
            });

            when(mockFrameParser.parse(any()))
                    .thenReturn(pingFrame)
                    .thenThrow(new NotEnoughDataException());

            byte[] pong = {0x00};
            when(mockFrameEncoder.encodeControlFrame(eq(0xA), any()))
                    .thenReturn(pong);

            // FIX: write가 호출되면 버퍼를 소비하는 동작을 시뮬레이션
            when(mockChannel.write(any(ByteBuffer.class))).thenAnswer(invocation -> {
                ByteBuffer buffer = invocation.getArgument(0);
                int bytesToWrite = buffer.remaining();
                buffer.position(buffer.limit()); // 버퍼를 모두 소비한 것처럼 position을 끝으로 이동
                return bytesToWrite; // 쓴 바이트 수 반환
            });

            // when
            session.read(mockSelectionKey);

            // then
            verify(mockFrameEncoder).encodeControlFrame(0xA, pingPayload);
            verify(mockChannel).write(any(ByteBuffer.class));
        }

        @Test
        @DisplayName("단일 텍스트 프레임 수신 시 메시지를 디스패치해야 한다")
        void read_should_dispatch_single_text_frame() throws Exception {
            // given
            String message = "A single frame";
            byte[] rawTextFrameBytes = message.getBytes(StandardCharsets.UTF_8);
            WebSocketFrame textFrame = new WebSocketFrame(true, 0x1, new ByteArrayInputStream(rawTextFrameBytes));
            WebSocketMessageDispatcher mockDispatcher = mock(WebSocketMessageDispatcher.class);

            when(mockChannel.read(any(ByteBuffer.class))).thenAnswer(invocation -> {
                ByteBuffer buffer = invocation.getArgument(0);
                buffer.put(rawTextFrameBytes);
                return rawTextFrameBytes.length;
            });
            when(mockFrameParser.parse(any()))
                    .thenReturn(textFrame)
                    .thenThrow(new NotEnoughDataException());
            when(mockMessageDispatchers.iterator())
                    .thenAnswer(inv -> List.of(mockDispatcher).iterator());
            when(mockDispatcher.supports(any(), any())).thenReturn(true);
            DispatchResult result = new DispatchResult(true, false);
            when(mockDispatcher.dispatch(any(), any())).thenReturn(result);

            // when
            session.read(mockSelectionKey);

            // then
            ArgumentCaptor<InvocationContext> captor = ArgumentCaptor.forClass(InvocationContext.class);
            verify(mockDispatcher).dispatch(any(), captor.capture());
            assertEquals(message, captor.getValue().getMessagePayload().asText());
        }
    }
}