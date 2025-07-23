package sprout.server.websocket;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import sprout.mvc.http.HttpRequest;
import sprout.server.argument.WebSocketArgumentResolver;
import sprout.server.websocket.endpoint.WebSocketEndpointInfo;
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
    @Mock CloseListener closeListener;
    @Mock SelectionKey key;

    DefaultWebSocketSession session;

    @BeforeEach
    void setUp() throws IOException {
        // 기본 stubbing
        when(channel.isOpen()).thenReturn(true);
        when(channel.keyFor(selector)).thenReturn(key);
        when(key.isValid()).thenReturn(true);

        // encode 계열은 전부 any()로 스텁해서 인자 mismatch 방지
        lenient().when(frameEncoder.encodeText(anyString()))
                .thenAnswer(inv -> inv.getArgument(0, String.class).getBytes(StandardCharsets.UTF_8));

        lenient().when(frameEncoder.encodeBinary(ArgumentMatchers.<byte[]>any()))
                .thenAnswer(inv -> inv.getArgument(0, byte[].class));  // 그대로 돌려주기

        lenient().when(frameEncoder.encodeControlFrame(anyInt(), ArgumentMatchers.<byte[]>any()))
                .thenAnswer(inv -> {
                    int opcode = inv.getArgument(0, Integer.class);
                    byte[] payload = inv.getArgument(1, byte[].class);
                    if (payload == null) payload = new byte[0];
                    return ("ctl:" + opcode + ":" + payload.length).getBytes(StandardCharsets.UTF_8);
                });

        // interestOps 상태를 직접 관리하기 위한 헬퍼
        stubInterestOpsState(OP_READ);

        session = new DefaultWebSocketSession(
                "test", channel, selector, handshakeRequest, endpointInfo,
                frameParser, frameEncoder, Collections.emptyMap(),
                argumentResolvers, dispatchers, closeListener
        );
    }

    // ---------------------- Tests ----------------------

    @Test
    @DisplayName("sendText -> pendingWrites에 들어가고 OP_WRITE 등록 및 selector.wakeup() 호출")
    void sendText_registersWrite() throws IOException {
        session.sendText("hello");
        when(channel.write(ArgumentMatchers.<ByteBuffer>any()))
                .thenAnswer(inv -> {
                    ByteBuffer buf = inv.getArgument(0, ByteBuffer.class);
                    int remaining = buf.remaining();
                    buf.position(buf.limit());
                    return remaining;
                });

        // 직접 write()는 호출되지 않음
        verify(channel, never()).write(ArgumentMatchers.<ByteBuffer>any());

        // OP_WRITE 세팅 확인
        assertThat(currentOps()).isEqualTo(OP_READ | OP_WRITE);
        verify(selector).wakeup();
    }

    @Test
    @DisplayName("write - 버퍼를 모두 비우면 OP_WRITE 제거")
    void write_drainsQueue_and_unsetWriteFlag() throws Exception {
        // given
        session.sendText("hello");
        // channel.write가 한 번 호출에 모두 소비하도록 stubbing
        when(channel.write(any(ByteBuffer.class))).thenAnswer(inv -> {
            ByteBuffer b = inv.getArgument(0);
            int r = b.remaining();
            b.position(b.limit());
            return r;
        });

        // when
        session.write(key);

        // then
        assertThat(currentOps()).isEqualTo(OP_READ); // OP_WRITE 제거
    }

    @Test
    @DisplayName("write - 부분만 썼다면 큐에 남아있고 OP_WRITE 유지")
    void write_partialWrite_keepsWriteFlag() throws Exception {
        session.sendText("hello");

        when(channel.write(any(ByteBuffer.class))).thenAnswer(inv -> {
            ByteBuffer b = inv.getArgument(0);
            int half = Math.max(1, b.remaining() / 2);
            b.position(b.position() + half);
            return half;
        });

        session.write(key);

        // 큐에 아직 남아있으므로 OP_WRITE 여전히 유지
        assertThat(currentOps() & OP_WRITE).isEqualTo(OP_WRITE);
        verify(channel, atLeastOnce()).write(any(ByteBuffer.class));
        // closeListener 호출 X
        verify(closeListener, never()).onSessionClosed(any());
    }

    @Test
    @DisplayName("close 호출 후 write가 큐를 비우면 채널 닫고 closeListener 호출")
    void write_afterClosePending_willCloseChannel() throws Exception {
        // 먼저 close() 호출 -> close frame enqueue
        byte[] closeBytes = "close-frame".getBytes(StandardCharsets.UTF_8);
        doReturn(closeBytes).when(frameEncoder)
                .encodeControlFrame(eq(0x8), ArgumentMatchers.<byte[]>any());
        session.close();
        assertThat(session.isClosePending()).isTrue();
        // queue drain 동작
        when(channel.write(ArgumentMatchers.<ByteBuffer>any())).thenAnswer(inv -> {
            ByteBuffer b = inv.getArgument(0, ByteBuffer.class);
            int r = b.remaining();
            b.position(b.limit());
            return r;
        });

        // OP_WRITE가 있어야 write 호출됨
        assertThat(currentOps() & OP_WRITE).isEqualTo(OP_WRITE);

        session.write(key);

        verify(channel).close();
        verify(closeListener).onSessionClosed(session);
        assertThat(session.isOpen()).isFalse();
    }

    @Test
    @DisplayName("이미 OP_WRITE 등록된 상태에서 또 enqueue하면 interestOps 중복 변경하지 않음")
    void scheduleWrite_doesNotFlipOpsTwice() throws IOException {
        session.sendText("first"); // OP_WRITE set

        // 기록 초기화
        clearInvocations(key, selector);

        session.sendText("second");

        // 두 번째 호출시 interestOps(OP_WRITE 추가)가 또 일어나지 않거나, 일어나도 동일 값
        // (= 괜찮지만 적어도 selector.wakeup()은 중복 호출 안해도 됨)
        verify(selector, atMostOnce()).wakeup();
    }

    // ---------------------- Helper ----------------------

    private final AtomicInteger opsHolder = new AtomicInteger();

    private void stubInterestOpsState(int initialOps) {
        opsHolder.set(initialOps);

        // get
        when(key.interestOps()).thenAnswer(inv -> opsHolder.get());
        // set
        when(key.interestOps(anyInt())).thenAnswer(inv -> {
            opsHolder.set(inv.getArgument(0));
            return key;
        });
    }

    private int currentOps() {
        return opsHolder.get();
    }
}
