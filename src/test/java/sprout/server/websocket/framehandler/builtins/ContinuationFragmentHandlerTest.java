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

class ContinuationFragmentHandlerTest {

    static class CloseTrackingInputStream extends ByteArrayInputStream {
        boolean closed;
        CloseTrackingInputStream(byte[] buf) { super(buf); }
        @Override public void close() throws IOException { closed = true; super.close(); }
    }

    ContinuationFragmentHandler handler = new ContinuationFragmentHandler();

    @Test
    @DisplayName("canHandle: fin=false && opcode=0x0 일 때만 true")
    void canHandle_conditions() {
        FrameProcessingContext ctx = new FrameProcessingContext();

        ctx.setCurrentFrame(new WebSocketFrame(false, (byte) 0x0, new ByteArrayInputStream(new byte[0])));
        assertThat(handler.canHandle(ctx)).isTrue();

        ctx.setCurrentFrame(new WebSocketFrame(true, (byte) 0x0, new ByteArrayInputStream(new byte[0])));
        assertThat(handler.canHandle(ctx)).isFalse();

        ctx.setCurrentFrame(new WebSocketFrame(false, (byte) 0x1, new ByteArrayInputStream(new byte[0])));
        assertThat(handler.canHandle(ctx)).isFalse();
    }

    @Test
    @DisplayName("fragmented=false 상태에서 호출되면 예외")
    void process_throws_whenNotFragmented() {
        FrameProcessingContext ctx = new FrameProcessingContext();
        ctx.setCurrentFrame(new WebSocketFrame(false, (byte) 0x0, new ByteArrayInputStream(new byte[0])));

        assertThatThrownBy(() -> handler.handle(ctx))
                .isInstanceOf(WebSocketProtocolException.class);
    }

    @Test
    @DisplayName("텍스트 continuation이면 버퍼에 추가, false 반환, 스트림 닫힘")
    void process_textContinuation() throws Exception {
        FrameProcessingContext ctx = new FrameProcessingContext();
        ctx.startFragmentedMessage(0x1);
        ctx.getTextBuffer().append("Hel");
        byte[] data = "lo".getBytes(StandardCharsets.UTF_8);
        CloseTrackingInputStream in = new CloseTrackingInputStream(data);
        ctx.setCurrentFrame(new WebSocketFrame(false, (byte) 0x0, in));

        boolean r = handler.handle(ctx);

        assertThat(r).isFalse();
        assertThat(ctx.getTextBuffer().toString()).isEqualTo("Hello");
        assertThat(in.closed).isTrue();
    }

    @Test
    @DisplayName("바이너리 continuation이면 버퍼에 추가, false 반환, 스트림 닫힘")
    void process_binaryContinuation() throws Exception {
        FrameProcessingContext ctx = new FrameProcessingContext();
        ctx.startFragmentedMessage(0x2);
        ctx.getBinaryBuffer().write("AB".getBytes(StandardCharsets.UTF_8));
        byte[] more = "CD".getBytes(StandardCharsets.UTF_8);
        CloseTrackingInputStream in = new CloseTrackingInputStream(more);
        ctx.setCurrentFrame(new WebSocketFrame(false, (byte) 0x0, in));

        boolean r = handler.handle(ctx);

        assertThat(r).isFalse();
        assertThat(ctx.getBinaryBuffer().toByteArray()).isEqualTo("ABCD".getBytes(StandardCharsets.UTF_8));
        assertThat(in.closed).isTrue();
    }
}
