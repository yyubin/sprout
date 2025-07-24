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
import sprout.server.websocket.framehandler.FrameHandler;
import sprout.server.websocket.framehandler.FrameProcessingContext;
import sprout.server.websocket.message.DefaultMessagePayload;
import sprout.server.websocket.message.WebSocketMessageDispatcher;
import sprout.server.websocket.support.Fakes.*;
import sprout.server.websocket.support.Fakes;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
    @Mock FrameHandler handler;

    QueueParser parser;
    FakeEncoder encoder;
    List<WebSocketArgumentResolver> resolvers = List.of();
    List<WebSocketMessageDispatcher> dispatchers;
    DefaultWebSocketSession session;

    private final AtomicInteger opsHolder = new AtomicInteger();

    @BeforeEach
    void setUp() throws Exception {
        when(channel.isOpen()).thenReturn(true);
        when(channel.keyFor(selector)).thenReturn(key);
        when(key.isValid()).thenReturn(true);
        when(handshakeRequest.getPath()).thenReturn("/ws");
        when(handshakeRequest.getQueryParams()).thenReturn(Map.of());
        when(endpointInfo.getOnOpenMethod()).thenReturn(null);
        when(endpointInfo.getOnErrorMethod()).thenReturn(null);
        when(endpointInfo.getOnCloseMethod()).thenReturn(null);

        parser = new QueueParser();
        encoder = new FakeEncoder();
        dispatchers = new ArrayList<>();

        lenient().doAnswer(inv -> null).when(handler).setNext(any());
        lenient().when(handler.handle(any(FrameProcessingContext.class))).thenAnswer(inv -> {
            FrameProcessingContext ctx = inv.getArgument(0);
            return ctx.getFrame().isFin();
        });

        stubInterestOpsState(OP_READ);

        session = new DefaultWebSocketSession(
                "sid", channel, selector, handshakeRequest, endpointInfo,
                parser, encoder, Map.of(),
                resolvers, dispatchers, closeListener,
                Collections.singletonList(handler)
        );
    }

    @Test
    void read_remoteDisconnect_callsCloseAndOnClose() throws Exception {
        Method toString = Object.class.getMethod("toString");
        when(endpointInfo.getOnCloseMethod()).thenReturn(toString);
        when(endpointInfo.getHandlerBean()).thenReturn(new Object());
        when(channel.read(any(ByteBuffer.class))).thenReturn(-1);
        session.read(key);
        assertThat(session.isClosePending()).isTrue();
        verify(closeListener, never()).onSessionClosed(session);
    }

    @Test
    void pingFrame_sendsPong_and_setsWrite() throws Exception {
        parser.add(new WebSocketFrame(true, (byte) 0x9,
                new ByteArrayInputStream("ping".getBytes(StandardCharsets.UTF_8))));
        stubChannelReadReturnsBytes();
        session.read(key);
        assertThat(currentOps() & OP_WRITE).isEqualTo(OP_WRITE);
        verify(selector).wakeup();
    }

    @Test
    void unknownOpcode_callsOnError() throws Exception {
        Method toString = Object.class.getMethod("toString");
        when(endpointInfo.getOnErrorMethod()).thenReturn(toString);
        when(endpointInfo.getHandlerBean()).thenReturn(new Object());
        parser.add(new WebSocketFrame(true, (byte) 0x3,
                new ByteArrayInputStream("x".getBytes(StandardCharsets.UTF_8))));
        stubChannelReadReturnsBytes();
        session.read(key);
        verify(endpointInfo).getOnErrorMethod();
    }

    @Test
    void closeFrame_callsOnClose_notCloseImmediately() throws Exception {
        Method toString = Object.class.getMethod("toString");
        when(endpointInfo.getOnCloseMethod()).thenReturn(toString);
        when(endpointInfo.getHandlerBean()).thenReturn(new Object());
        parser.add(new WebSocketFrame(true, (byte) 0x8, new ByteArrayInputStream(new byte[]{0x03, (byte) 0xE8})));
        stubChannelReadReturnsBytes();
        session.read(key);
        assertThat(session.isClosePending()).isFalse();
        verify(endpointInfo).getOnCloseMethod();
    }

    @Test
    @DisplayName("메시지가 완성될 때 단 한 번만 dispatch 호출된다")
    void dispatch_called_only_when_message_completed() throws Exception {
        WebSocketMessageDispatcher md = mock(WebSocketMessageDispatcher.class);
        DispatchResult dr = mock(DispatchResult.class);
        when(md.supports(any(), any())).thenReturn(true);
        when(md.dispatch(any(), any())).thenReturn(dr);
        when(dr.isHandled()).thenReturn(true);
        when(dr.shouldCloseStream()).thenReturn(true);
        dispatchers.add(md);

        parser.add(new WebSocketFrame(false, (byte) 0x1, new ByteArrayInputStream("Hel".getBytes())));
        parser.add(new WebSocketFrame(false, (byte) 0x0, new ByteArrayInputStream("lo ".getBytes())));
        parser.add(new WebSocketFrame(true,  (byte) 0x0, new ByteArrayInputStream("World".getBytes())));

        stubChannelReadReturnsBytes();
        session.read(key);

        verify(md, times(1)).dispatch(any(), any());
    }


    private void stubInterestOpsState(int init) {
        opsHolder.set(init);
        when(key.interestOps()).thenAnswer(inv -> opsHolder.get());
        when(key.interestOps(anyInt())).thenAnswer(inv -> {
            opsHolder.set(inv.getArgument(0));
            return key;
        });
    }

    private int currentOps() {
        return opsHolder.get();
    }

    private void stubChannelReadReturnsBytes() throws IOException {
        when(channel.read(any(ByteBuffer.class))).thenAnswer(inv -> {
            ByteBuffer b = inv.getArgument(0);
            b.put((byte) 0x0);
            return 1;
        });
    }
}
