package sprout.server.websocket.framehandler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sprout.server.websocket.WebSocketFrame;
import sprout.server.websocket.message.DefaultMessagePayload;
import sprout.server.websocket.message.MessagePayload;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class FrameProcessingContextTest {

    @Test
    @DisplayName("setCurrentFrame/getFrame 동작")
    void currentFrame() {
        FrameProcessingContext ctx = new FrameProcessingContext();
        WebSocketFrame f = new WebSocketFrame(true, (byte) 0x1, new ByteArrayInputStream("x".getBytes()));
        ctx.setCurrentFrame(f);
        assertThat(ctx.getFrame()).isSameAs(f);
    }

    @Test
    @DisplayName("fragmented 플래그/Opcode 설정")
    void fragmentedFlag() {
        FrameProcessingContext ctx = new FrameProcessingContext();
        assertThat(ctx.isFragmented()).isFalse();
        ctx.startFragmentedMessage(0x1);
        assertThat(ctx.isFragmented()).isTrue();
        assertThat(ctx.getFragmentedOpcode()).isEqualTo(0x1);
    }

    @Test
    @DisplayName("createPayload - 텍스트만 있을 때")
    void createPayload_textOnly() throws Exception {
        FrameProcessingContext ctx = new FrameProcessingContext();
        ctx.getTextBuffer().append("hello");
        MessagePayload p = ctx.createPayload();
        assertThat(((DefaultMessagePayload) p).asText()).isEqualTo("hello");
        assertThat(readBinaryField(p)).isNull();
    }

    @Test
    @DisplayName("createPayload - 바이너리만 있을 때")
    void createPayload_binaryOnly() throws Exception {
        FrameProcessingContext ctx = new FrameProcessingContext();
        byte[] bin = "bin".getBytes(StandardCharsets.UTF_8);
        ctx.getBinaryBuffer().write(bin, 0, bin.length);
        MessagePayload p = ctx.createPayload();
        assertThat(((DefaultMessagePayload) p).asText()).isEqualTo("bin");
        assertThat(readBinaryField(p)).isEqualTo(bin);
    }

    @Test
    @DisplayName("createPayload - 둘 다 없으면 둘 다 null")
    void createPayload_empty() throws Exception {
        FrameProcessingContext ctx = new FrameProcessingContext();
        MessagePayload p = ctx.createPayload();
        assertThat(((DefaultMessagePayload) p).asText()).isNull();
        assertThat(readBinaryField(p)).isNull();
    }

    @Test
    @DisplayName("reset은 버퍼/프레임/플래그 초기화")
    void reset() {
        FrameProcessingContext ctx = new FrameProcessingContext();
        ctx.getTextBuffer().append("x");
        ctx.getBinaryBuffer().write(1);
        ctx.startFragmentedMessage(1);
        ctx.setCurrentFrame(new WebSocketFrame(true, (byte)1, new ByteArrayInputStream(new byte[0])));

        ctx.reset();

        assertThat(ctx.getTextBuffer().length()).isZero();
        assertThat(ctx.getBinaryBuffer().size()).isZero();
        assertThat(ctx.isFragmented()).isFalse();
        assertThat(ctx.getFrame()).isNull();
    }

    private byte[] readBinaryField(MessagePayload p) throws Exception {
        Field f = DefaultMessagePayload.class.getDeclaredField("binaryPayload");
        f.setAccessible(true);
        return (byte[]) f.get(p);
    }
}
