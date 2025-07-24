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

class FinalContinuationFrameHandlerTest {

    static class CloseTrackingInputStream extends ByteArrayInputStream {
        boolean closed;
        CloseTrackingInputStream(byte[] buf) { super(buf); }
        @Override public void close() throws IOException { closed = true; super.close(); }
    }

    FinalContinuationFrameHandler handler = new FinalContinuationFrameHandler();

    @Test
    @DisplayName("canHandle: fin=true && opcode=0x0 일 때만 true")
    void canHandle_conditions() {
        FrameProcessingContext ctx = new FrameProcessingContext();

        ctx.setCurrentFrame(new WebSocketFrame(true, (byte) 0x0, new ByteArrayInputStream(new byte[0])));
        assertThat(handler.canHandle(ctx)).isTrue();

        ctx.setCurrentFrame(new WebSocketFrame(false, (byte) 0x0, new ByteArrayInputStream(new byte[0])));
        assertThat(handler.canHandle(ctx)).isFalse();

        ctx.setCurrentFrame(new WebSocketFrame(true, (byte) 0x1, new ByteArrayInputStream(new byte[0])));
        assertThat(handler.canHandle(ctx)).isFalse();
    }

    @Test
    @DisplayName("fragmented=false 상태에서 호출되면 예외")
    void process_throws_whenNotFragmented() {
        FrameProcessingContext ctx = new FrameProcessingContext();
        ctx.setCurrentFrame(new WebSocketFrame(true, (byte) 0x0, new ByteArrayInputStream(new byte[0])));
        assertThatThrownBy(() -> handler.handle(ctx))
                .isInstanceOf(WebSocketProtocolException.class);
    }

    @Test
    @DisplayName("텍스트 조각 최종 프레임이면 텍스트 버퍼에 추가되고 true 반환, 스트림은 닫지 않음")
    void process_textContinuation() throws Exception {
        FrameProcessingContext ctx = new FrameProcessingContext();
        ctx.startFragmentedMessage(0x1);
        ctx.getTextBuffer().append("Hello ");
        byte[] data = "World".getBytes(StandardCharsets.UTF_8);
        CloseTrackingInputStream in = new CloseTrackingInputStream(data);
        ctx.setCurrentFrame(new WebSocketFrame(true, (byte) 0x0, in));

        boolean r = handler.handle(ctx);

        assertThat(r).isTrue();
        assertThat(ctx.getTextBuffer().toString()).isEqualTo("Hello World");
        assertThat(in.closed).isFalse();
    }

    @Test
    @DisplayName("바이너리 조각 최종 프레임이면 바이너리 버퍼에 추가되고 true 반환, 스트림은 닫지 않음")
    void process_binaryContinuation() throws Exception {
        FrameProcessingContext ctx = new FrameProcessingContext();
        ctx.startFragmentedMessage(0x2);
        ctx.getBinaryBuffer().write("ABC".getBytes(StandardCharsets.UTF_8));
        byte[] more = "DEF".getBytes(StandardCharsets.UTF_8);
        CloseTrackingInputStream in = new CloseTrackingInputStream(more);
        ctx.setCurrentFrame(new WebSocketFrame(true, (byte) 0x0, in));

        boolean r = handler.handle(ctx);

        assertThat(r).isTrue();
        assertThat(ctx.getBinaryBuffer().toByteArray()).isEqualTo("ABCDEF".getBytes(StandardCharsets.UTF_8));
        assertThat(in.closed).isFalse();
    }
}
