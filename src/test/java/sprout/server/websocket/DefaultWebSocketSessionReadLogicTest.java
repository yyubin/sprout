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
import sprout.server.websocket.exception.WebSocketProtocolException;
import sprout.server.websocket.message.WebSocketMessageDispatcher;
import sprout.server.websocket.support.Fakes;
import sprout.server.websocket.support.Fakes.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DefaultWebSocketSessionReadLogicTest {

    @Mock SocketChannel channel;
    @Mock Selector selector;
    @Mock SelectionKey key;
    @Mock HttpRequest<?> handshakeRequest;
    @Mock WebSocketEndpointInfo endpointInfo;
    @Mock CloseListener closeListener;

    // 간단히 전부 빈 리스트
    List<WebSocketArgumentResolver> resolvers = List.of();
    List<WebSocketMessageDispatcher> dispatchers;

    DefaultWebSocketSession session;
    QueueParser parser;
    FakeEncoder encoder;

    private final AtomicInteger opsHolder = new AtomicInteger();

    private void stubInterestOpsState() {
        opsHolder.set(OP_READ);
        when(key.interestOps()).thenAnswer(inv -> opsHolder.get());
        when(key.interestOps(anyInt())).thenAnswer(inv -> {
            opsHolder.set(inv.getArgument(0));
            return key;
        });
    }

    private void stubChannelReadReturnsBytes(int times) throws IOException {
        when(channel.read(any(ByteBuffer.class))).thenAnswer(inv -> {
            ByteBuffer buf = inv.getArgument(0, ByteBuffer.class);
            // 최소 1byte 넣어줘야 readBuffer.remaining() > 0
            buf.put((byte) 0x00);
            return 1;
        });
    }

    @BeforeEach
    void setUp() throws Exception {
        when(channel.isOpen()).thenReturn(true);
        when(channel.keyFor(selector)).thenReturn(key);
        when(key.isValid()).thenReturn(true);
        when(key.interestOps()).thenReturn(OP_READ);
        when(handshakeRequest.getPath()).thenReturn("/ws");
        when(handshakeRequest.getQueryParams()).thenReturn(Map.of());
        when(endpointInfo.getOnOpenMethod()).thenReturn(null);
        when(endpointInfo.getOnErrorMethod()).thenReturn(null);
        when(endpointInfo.getOnCloseMethod()).thenReturn(null);

        parser = new QueueParser();
        encoder = new FakeEncoder();
        dispatchers = new ArrayList<>();

        session = new DefaultWebSocketSession(
                "sid", channel, selector, handshakeRequest, endpointInfo,
                parser, encoder, Map.of(),
                resolvers, dispatchers, closeListener
        );

        stubInterestOpsState();
    }

    @Test
    void read_remoteDisconnect_callsCloseAndOnClose() throws Exception {
        // onClose 메서드 세팅
        Method toString = Object.class.getMethod("toString");
        when(endpointInfo.getOnCloseMethod()).thenReturn(toString);
        when(endpointInfo.getHandlerBean()).thenReturn(new Object());

        // close frame encode stub 필요 없음(FakeEncoder 사용)
        when(channel.read(any(ByteBuffer.class))).thenReturn(-1);

        session.read(key);

        assertThat(session.isClosePending()).isTrue();
        verify(closeListener, never()).onSessionClosed(session); // 아직 write drain 전
    }

    @Test
    void pingFrame_sendsPong_and_setsWrite() throws Exception {
        WebSocketFrame ping = new WebSocketFrame(true, (byte) 0x9,
                new ByteArrayInputStream("ping".getBytes(StandardCharsets.UTF_8)));
        parser.add(ping);

        stubChannelReadReturnsBytes(10);

        session.read(key);

        assertThat(opsHolder.get() & OP_WRITE).isEqualTo(OP_WRITE);
        verify(selector).wakeup();
    }


    @Test
    void singleTextFrame_dispatchesMessage() throws Exception {
        String msg = "hello";
        parser.add(new WebSocketFrame(true, (byte) 0x1,
                new ByteArrayInputStream(msg.getBytes(StandardCharsets.UTF_8))));

        CapturingDispatcher dispatcher = new CapturingDispatcher(true, true);
        dispatchers.add(dispatcher);

        stubChannelReadReturnsBytes(1);

        session.read(key);

        assertThat(dispatcher.lastCtx).isNotNull();
        assertThat(dispatcher.lastCtx.getMessagePayload().asText()).isEqualTo(msg);
    }

    @Test
    void fragmentedTextFrames_areConcatenated_andDispatched() throws Exception {
        parser.add(new WebSocketFrame(false, (byte) 0x1, new ByteArrayInputStream("Hel".getBytes())));
        parser.add(new WebSocketFrame(false, (byte) 0x0, new ByteArrayInputStream("lo ".getBytes())));
        parser.add(new WebSocketFrame(true,  (byte) 0x0, new ByteArrayInputStream("World".getBytes())));

        CapturingDispatcher dispatcher = new CapturingDispatcher(true, true);
        dispatchers.add(dispatcher);

        stubChannelReadReturnsBytes(3);

        session.read(key);

        assertThat(dispatcher.lastCtx).isNotNull();
        assertThat(dispatcher.lastCtx.getMessagePayload().asText()).isEqualTo("Hello World");
    }

    @Test
    void unknownOpcode_callsOnError() throws Exception {
        Method toString = Object.class.getMethod("toString");
        when(endpointInfo.getOnErrorMethod()).thenReturn(toString);
        when(endpointInfo.getHandlerBean()).thenReturn(new Object());

        // opcode 0x3 (데이터/컨트롤 모두 아님) 같은 걸 넣으면 processFrame()의 else 분기
        WebSocketFrame unknown = new WebSocketFrame(true, (byte) 0x3,
                new ByteArrayInputStream("x".getBytes(StandardCharsets.UTF_8)));
        parser.add(unknown);

        stubChannelReadReturnsBytes(1);

        session.read(key);

        verify(endpointInfo).getOnErrorMethod();
    }

    @Test
    void continuationWithoutStart_throwsProtocolError() throws Exception {
        parser.add(new WebSocketFrame(false, (byte) 0x0,
                new ByteArrayInputStream("???".getBytes(StandardCharsets.UTF_8))));

        stubChannelReadReturnsBytes(1);

        assertThrows(WebSocketProtocolException.class, () -> session.read(key));
    }


    @Test
    void closeFrame_callsOnClose_notCloseImmediately() throws Exception {
        Method toString = Object.class.getMethod("toString");
        when(endpointInfo.getOnCloseMethod()).thenReturn(toString);
        when(endpointInfo.getHandlerBean()).thenReturn(new Object());

        parser.add(new WebSocketFrame(true, (byte) 0x8, new ByteArrayInputStream(new byte[]{0x03, (byte) 0xE8})));

        stubChannelReadReturnsBytes(1);

        session.read(key);

        assertThat(session.isClosePending()).isFalse();
        verify(endpointInfo).getOnCloseMethod();
    }
}
