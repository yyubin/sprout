package sprout.server.websocket;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import sprout.mvc.http.HttpRequest;
import sprout.server.argument.WebSocketArgumentResolver;
import sprout.server.websocket.endpoint.WebSocketEndpointInfo;
import sprout.server.websocket.framehandler.FrameHandler;
import sprout.server.websocket.framehandler.FrameProcessingContext;
import sprout.server.websocket.message.WebSocketMessageDispatcher;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DefaultWebSocketSessionWriteLogicTest {

    @Mock SocketChannel channel;
    @Mock Selector selector;
    @Mock HttpRequest<?> handshakeRequest;
    @Mock WebSocketEndpointInfo endpointInfo;
    @Mock WebSocketFrameParser frameParser;
    @Mock WebSocketFrameEncoder frameEncoder;
    @Mock List<WebSocketArgumentResolver> argumentResolvers;
    @Mock List<WebSocketMessageDispatcher> dispatchers;
    @Mock FrameHandler handler;
    @Mock CloseListener closeListener;
    @Mock SelectionKey key;

    DefaultWebSocketSession session;

    private final AtomicInteger opsHolder = new AtomicInteger();

    @BeforeEach
    void setUp() throws Exception {
        when(channel.isOpen()).thenReturn(true);
        when(channel.keyFor(selector)).thenReturn(key);
        when(key.isValid()).thenReturn(true);

        lenient().when(frameEncoder.encodeText(anyString()))
                .thenAnswer(inv -> inv.getArgument(0, String.class).getBytes(StandardCharsets.UTF_8));
        lenient().when(frameEncoder.encodeBinary(any(byte[].class)))
                .thenAnswer(inv -> inv.getArgument(0, byte[].class));
        lenient().when(frameEncoder.encodeControlFrame(anyInt(), any(byte[].class)))
                .thenAnswer(inv -> {
                    int opcode = inv.getArgument(0, Integer.class);
                    byte[] payload = inv.getArgument(1, byte[].class);
                    if (payload == null) payload = new byte[0];
                    return ("ctl:" + opcode + ":" + payload.length).getBytes(StandardCharsets.UTF_8);
                });

        // FrameHandler mock 셋업
        lenient().doAnswer(inv -> null).when(handler).setNext(any());
        lenient().when(handler.handle(any(FrameProcessingContext.class))).thenReturn(false);

        stubInterestOpsState(OP_READ);

        session = new DefaultWebSocketSession(
                "test",
                channel,
                selector,
                handshakeRequest,
                endpointInfo,
                frameParser,
                frameEncoder,
                Collections.emptyMap(),
                argumentResolvers,
                dispatchers,
                closeListener,
                Collections.singletonList(handler) // <-- 여기
        );
    }

    @Test
    @DisplayName("sendText -> 큐 enqueue, OP_WRITE 등록, selector.wakeup 호출")
    void sendText_registersWrite() throws IOException {
        session.sendText("hello");
        verify(channel, never()).write(any(ByteBuffer.class));
        assertThat(currentOps()).isEqualTo(OP_READ | OP_WRITE);
        verify(selector).wakeup();
    }

    @Test
    @DisplayName("write - 모두 썼을 때 OP_WRITE 제거")
    void write_drainsQueue_and_unsetWriteFlag() throws Exception {
        session.sendText("hello");
        when(channel.write(any(ByteBuffer.class))).thenAnswer(this::drainBuffer);
        session.write(key);
        assertThat(currentOps()).isEqualTo(OP_READ);
    }

    @Test
    @DisplayName("write - 부분만 썼을 때 OP_WRITE 유지")
    void write_partialWrite_keepsWriteFlag() throws Exception {
        session.sendText("hello");
        when(channel.write(any(ByteBuffer.class))).thenAnswer(inv -> {
            ByteBuffer b = inv.getArgument(0);
            int half = Math.max(1, b.remaining() / 2);
            b.position(b.position() + half);
            return half;
        });
        session.write(key);
        assertThat(currentOps() & OP_WRITE).isEqualTo(OP_WRITE);
        verify(closeListener, never()).onSessionClosed(any());
    }

    @Test
    @DisplayName("close 이후 write가 끝나면 채널 닫고 closeListener 호출")
    void write_afterClosePending_willCloseChannel() throws Exception {
        byte[] closeBytes = "x".getBytes(StandardCharsets.UTF_8);
        when(frameEncoder.encodeControlFrame(eq(0x8), any(byte[].class))).thenReturn(closeBytes);
        session.close();
        assertThat(session.isClosePending()).isTrue();
        when(channel.write(any(ByteBuffer.class))).thenAnswer(this::drainBuffer);
        session.write(key);
        verify(channel).close();
        verify(closeListener).onSessionClosed(session);
        assertThat(session.isOpen()).isFalse();
    }

    @Test
    @DisplayName("이미 OP_WRITE인 상태에서 enqueue 시 wakeup 중복 호출 방지")
    void scheduleWrite_doesNotFlipOpsTwice() throws IOException {
        session.sendText("first");
        clearInvocations(key, selector);
        session.sendText("second");
        verify(selector, atMostOnce()).wakeup();
    }

    @Test
    @DisplayName("sendBinary도 동일하게 큐에 적재되고 write로 비워진다")
    void sendBinary_behavesLikeText() throws Exception {
        session.sendBinary(new byte[]{1,2,3});
        when(channel.write(any(ByteBuffer.class))).thenAnswer(this::drainBuffer);
        session.write(key);
        assertThat(currentOps()).isEqualTo(OP_READ);
    }

    @Test
    @DisplayName("ping은 control frame으로 인코딩되어 전송된다")
    void sendPing_controlFrame() throws Exception {
        session.sendPing(new byte[]{9,9});
        when(channel.write(any(ByteBuffer.class))).thenAnswer(this::drainBuffer);
        session.write(key);
        verify(frameEncoder).encodeControlFrame(eq(0x9), any(byte[].class));
    }

    @Test
    @DisplayName("pong은 control frame으로 인코딩되어 전송된다")
    void sendPong_controlFrame() throws Exception {
        session.sendPong(new byte[]{1});
        when(channel.write(any(ByteBuffer.class))).thenAnswer(this::drainBuffer);
        session.write(key);
        verify(frameEncoder).encodeControlFrame(eq(0xA), any(byte[].class));
    }

    private void stubInterestOpsState(int initialOps) {
        opsHolder.set(initialOps);
        when(key.interestOps()).thenAnswer(inv -> opsHolder.get());
        when(key.interestOps(anyInt())).thenAnswer(inv -> {
            opsHolder.set(inv.getArgument(0));
            return key;
        });
    }

    private int currentOps() {
        return opsHolder.get();
    }

    private int drainBuffer(InvocationOnMock inv) {
        ByteBuffer b = inv.getArgument(0);
        int r = b.remaining();
        b.position(b.limit());
        return r;
    }
}
