package sprout.server.websocket;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import sprout.mvc.http.HttpRequest;
import sprout.server.websocket.framehandler.FrameHandler;
import sprout.server.websocket.framehandler.FrameProcessingContext;
import sprout.server.websocket.message.MessagePayload;
import sprout.server.websocket.message.WebSocketMessageDispatcher;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WebSocketFrameDispatcherTest {

    @Mock FrameHandler handler;
    @Mock FrameProcessingContext state;
    @Mock WebSocketSession session;
    @Mock MessagePayload payload;
    @Mock WebSocketFrame frame;
    @Mock WebSocketMessageDispatcher d1;
    @Mock WebSocketMessageDispatcher d2;
    @Mock DispatchResult result;
    @Mock HttpRequest<?> request;

    @Test
    @DisplayName("빈 핸들러 리스트면 예외")
    void emptyHandlersThrows() {
        assertThatThrownBy(() -> new WebSocketFrameDispatcher(Collections.emptyList(), List.of()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("messageCompleted=false면 dispatcher 호출 안 함")
    void notCompleted_noDispatch() throws Exception {
        when(handler.handle(state)).thenReturn(false);
        WebSocketFrameDispatcher dispatcher = new WebSocketFrameDispatcher(List.of(handler), List.of(d1));
        dispatcher.dispatch(state, session, Map.of());
        verify(d1, never()).supports(any(), any());
        verify(state, never()).reset();
    }

    @Test
    @DisplayName("완성된 메시지면 supports/dispatch 한 번, reset 호출, stream 닫힘")
    void completed_dispatch_once_and_reset_and_close() throws Exception {
        CloseTrackingInputStream in = new CloseTrackingInputStream("x".getBytes());
        when(handler.handle(state)).thenReturn(true);
        when(state.createPayload()).thenReturn(payload);
        when(state.getFrame()).thenReturn(frame);
        when(frame.getPayloadStream()).thenReturn(in);
        doReturn(request).when(session).getHandshakeRequest();
        when(request.getQueryParams()).thenReturn(Map.of());
        when(request.getPath()).thenReturn("/ws");

        when(d1.supports(any(), any())).thenReturn(true);
        when(d1.dispatch(any(), any())).thenReturn(result);
        when(result.isHandled()).thenReturn(true);
        when(result.shouldCloseStream()).thenReturn(true);

        WebSocketFrameDispatcher dispatcher = new WebSocketFrameDispatcher(List.of(handler), List.of(d1, d2));
        dispatcher.dispatch(state, session, Map.of());

        verify(d1, times(1)).dispatch(any(), any());
        verify(d2, never()).dispatch(any(), any());
        verify(state).reset();
        assertThat(in.closed).isTrue();
    }

    @Test
    @DisplayName("handled=false이거나 dispatcher 없으면 stream 닫히고 reset")
    void notHandled_closesStream() throws Exception {
        CloseTrackingInputStream in = new CloseTrackingInputStream("y".getBytes());
        when(handler.handle(state)).thenReturn(true);
        when(state.createPayload()).thenReturn(payload);
        when(state.getFrame()).thenReturn(frame);
        when(frame.getPayloadStream()).thenReturn(in);
        doReturn(request).when(session).getHandshakeRequest();
        when(request.getQueryParams()).thenReturn(Map.of());
        when(request.getPath()).thenReturn("/ws");
        when(d1.supports(any(), any())).thenReturn(false);

        WebSocketFrameDispatcher dispatcher = new WebSocketFrameDispatcher(List.of(handler), List.of(d1));
        dispatcher.dispatch(state, session, Map.of());

        verify(d1, times(1)).supports(any(), any());
        verify(d1, never()).dispatch(any(), any());
        verify(state).reset();
        assertThat(in.closed).isTrue();
    }

    @Test
    @DisplayName("shouldCloseStream=false면 stream 유지")
    void whenShouldNotClose_streamNotClosed() throws Exception {
        CloseTrackingInputStream in = new CloseTrackingInputStream("z".getBytes());
        when(handler.handle(state)).thenReturn(true);
        when(state.createPayload()).thenReturn(payload);
        when(state.getFrame()).thenReturn(frame);
        when(frame.getPayloadStream()).thenReturn(in);
        doReturn(request).when(session).getHandshakeRequest();
        when(request.getQueryParams()).thenReturn(Map.of());
        when(request.getPath()).thenReturn("/ws");

        when(d1.supports(any(), any())).thenReturn(true);
        when(d1.dispatch(any(), any())).thenReturn(result);
        when(result.isHandled()).thenReturn(true);
        when(result.shouldCloseStream()).thenReturn(false);

        WebSocketFrameDispatcher dispatcher = new WebSocketFrameDispatcher(List.of(handler), List.of(d1));
        dispatcher.dispatch(state, session, Map.of());

        assertThat(in.closed).isFalse();
        verify(state).reset();
    }

    static class CloseTrackingInputStream extends ByteArrayInputStream {
        boolean closed;
        CloseTrackingInputStream(byte[] buf) { super(buf); }
        @Override public void close() throws IOException { closed = true; super.close(); }
    }
}
