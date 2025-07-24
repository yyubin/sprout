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

class InitialBinaryFragmentHandlerTest {

    static class CloseTrackingInputStream extends ByteArrayInputStream {
        boolean closed;
        CloseTrackingInputStream(byte[] buf) { super(buf); }
        @Override public void close() throws IOException { closed = true; super.close(); }
    }

    InitialBinaryFragmentHandler handler = new InitialBinaryFragmentHandler();

    @Test
    @DisplayName("canHandle: fin=false && opcode=0x2 일 때만 true")
    void canHandle_conditions() throws Exception {
        FrameProcessingContext ctx = new FrameProcessingContext();
        ctx.setCurrentFrame(new WebSocketFrame(false, (byte) 0x2, new ByteArrayInputStream(new byte[0])));
        assertThat(handler.canHandle(ctx)).isTrue();

        ctx.setCurrentFrame(new WebSocketFrame(true, (byte) 0x2, new ByteArrayInputStream(new byte[0])));
        assertThat(handler.canHandle(ctx)).isFalse();

        ctx.setCurrentFrame(new WebSocketFrame(false, (byte) 0x1, new ByteArrayInputStream(new byte[0])));
        assertThat(handler.canHandle(ctx)).isFalse();
    }

    @Test
    @DisplayName("process: 바이너리 버퍼에 누적, fragmented 시작, 스트림 닫힘, false 반환")
    void process_buffersAndMarksFragmented() throws Exception {
        FrameProcessingContext ctx = new FrameProcessingContext();
        byte[] data = "BIN".getBytes(StandardCharsets.UTF_8);
        CloseTrackingInputStream in = new CloseTrackingInputStream(data);
        ctx.setCurrentFrame(new WebSocketFrame(false, (byte) 0x2, in));

        boolean r = handler.handle(ctx);

        assertThat(r).isFalse();
        assertThat(ctx.isFragmented()).isTrue();
        assertThat(ctx.getFragmentedOpcode()).isEqualTo(0x2);
        assertThat(ctx.getBinaryBuffer().toByteArray()).isEqualTo(data);
        assertThat(in.closed).isTrue();
    }

    @Test
    @DisplayName("이미 fragmented 상태에서 또 초기 바이너리 프레임이면 예외")
    void process_throws_whenAlreadyFragmented() {
        FrameProcessingContext ctx = new FrameProcessingContext();
        ctx.startFragmentedMessage(0x2);
        ctx.setCurrentFrame(new WebSocketFrame(false, (byte) 0x2, new ByteArrayInputStream("X".getBytes(StandardCharsets.UTF_8))));

        assertThatThrownBy(() -> handler.handle(ctx))
                .isInstanceOf(WebSocketProtocolException.class);
    }
}
