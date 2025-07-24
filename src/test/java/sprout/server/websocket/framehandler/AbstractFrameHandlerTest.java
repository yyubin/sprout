package sprout.server.websocket.framehandler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import sprout.server.websocket.WebSocketFrame;
import sprout.server.websocket.exception.WebSocketProtocolException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AbstractFrameHandlerTest {

    @Mock FrameProcessingContext ctx;
    @Mock WebSocketFrame frame;

    @Test
    @DisplayName("canHandle=true면 process만 실행되고 next는 호출되지 않는다")
    void handle_whenSupported_callsProcess_notNext() throws Exception {
        when(ctx.getFrame()).thenReturn(frame);
        TestHandler h = new TestHandler(true, true);
        FrameHandler next = mock(FrameHandler.class);
        h.setNext(next);

        boolean res = h.handle(ctx);

        assertThat(res).isTrue();
        assertThat(h.processCalled).isTrue();
        verify(next, never()).handle(any());
    }

    @Test
    @DisplayName("canHandle=false면 next로 위임한다")
    void handle_whenNotSupported_delegatesToNext() throws Exception {
        when(ctx.getFrame()).thenReturn(frame);
        TestHandler h = new TestHandler(false, false);
        FrameHandler next = mock(FrameHandler.class);
        when(next.handle(ctx)).thenReturn(true);
        h.setNext(next);

        boolean res = h.handle(ctx);

        assertThat(res).isTrue();
        assertThat(h.processCalled).isFalse();
        verify(next, times(1)).handle(ctx);
    }

    @Test
    @DisplayName("canHandle=false이고 next도 없으면 WebSocketProtocolException 발생")
    void handle_whenNotSupported_andNoNext_throws() {
        when(ctx.getFrame()).thenReturn(frame);
        when(frame.getOpcode()).thenReturn((int)1);
        TestHandler h = new TestHandler(false, false);

        assertThatThrownBy(() -> h.handle(ctx))
                .isInstanceOf(WebSocketProtocolException.class);
    }

    static class TestHandler extends AbstractFrameHandler {
        final boolean can;
        final boolean ret;
        boolean processCalled;

        TestHandler(boolean can, boolean ret) {
            this.can = can;
            this.ret = ret;
        }

        public boolean canHandle(FrameProcessingContext state) {
            return can;
        }

        @Override
        protected boolean process(FrameProcessingContext state) {
            processCalled = true;
            return ret;
        }
    }
}
