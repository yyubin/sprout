package sprout.server.websocket.framehandler.builtins;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sprout.server.websocket.WebSocketFrame;
import sprout.server.websocket.exception.WebSocketProtocolException;
import sprout.server.websocket.framehandler.FrameProcessingContext;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FinalTextFrameHandlerTest {

    static class CloseTrackingInputStream extends ByteArrayInputStream {
        boolean closed;
        CloseTrackingInputStream(byte[] buf) { super(buf); }
        @Override public void close() throws IOException { closed = true; super.close(); }
    }

    FinalTextFrameHandler handler = new FinalTextFrameHandler();

    @Test
    @DisplayName("canHandle: fin=true && opcode=0x1 일 때만 true")
    void canHandle_conditions() {
        FrameProcessingContext ctx = new FrameProcessingContext();

        ctx.setCurrentFrame(new WebSocketFrame(true, (byte) 0x1, new ByteArrayInputStream(new byte[0])));
        assertThat(handler.canHandle(ctx)).isTrue();

        ctx.setCurrentFrame(new WebSocketFrame(false, (byte) 0x1, new ByteArrayInputStream(new byte[0])));
        assertThat(handler.canHandle(ctx)).isFalse();

        ctx.setCurrentFrame(new WebSocketFrame(true, (byte) 0x2, new ByteArrayInputStream(new byte[0])));
        assertThat(handler.canHandle(ctx)).isFalse();
    }

    @Test
    @DisplayName("process: fragmented 상태면 예외")
    void process_throws_whenFragmented() {
        FrameProcessingContext ctx = new FrameProcessingContext();
        ctx.startFragmentedMessage(0x1);
        ctx.setCurrentFrame(new WebSocketFrame(true, (byte) 0x1, new ByteArrayInputStream("x".getBytes(StandardCharsets.UTF_8))));

        assertThatThrownBy(() -> handler.handle(ctx))
                .isInstanceOf(WebSocketProtocolException.class);
    }

    @Test
    @DisplayName("process: 텍스트를 버퍼에 넣고 true 반환, 스트림은 닫지 않음")
    void process_readsText_returnsTrue_streamNotClosed() throws Exception {
        FrameProcessingContext ctx = new FrameProcessingContext();
        byte[] data = "Hello".getBytes(StandardCharsets.UTF_8);
        CloseTrackingInputStream in = new CloseTrackingInputStream(data);
        ctx.setCurrentFrame(new WebSocketFrame(true, (byte) 0x1, in));

        boolean r = handler.handle(ctx);

        assertThat(r).isTrue();
        assertThat(ctx.getTextBuffer().toString()).isEqualTo("Hello");
        assertThat(in.closed).isFalse();
    }
}
