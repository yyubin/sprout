package sprout.server.websocket;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class WebSocketUtilsTest {

    static class CloseTrackingInputStream extends ByteArrayInputStream {
        boolean closed;
        CloseTrackingInputStream(byte[] buf) { super(buf); }
        @Override public void close() throws IOException {
            closed = true;
            super.close();
        }
    }

    @Test
    @DisplayName("readTextToBuffer: 모든 텍스트를 읽고 스트림은 닫지 않는다")
    void readTextToBuffer_doesNotClose() throws Exception {
        CloseTrackingInputStream in = new CloseTrackingInputStream("hello".getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder("pre-");
        WebSocketUtils.readTextToBuffer(in, sb);
        assertThat(sb.toString()).isEqualTo("pre-hello");
        assertThat(in.closed).isFalse();
    }

    @Test
    @DisplayName("readBinaryToBuffer: 모든 바이너리를 읽고 스트림은 닫지 않는다")
    void readBinaryToBuffer_doesNotClose() throws Exception {
        byte[] data = new byte[3000];
        for (int i = 0; i < data.length; i++) data[i] = (byte) (i % 128);
        CloseTrackingInputStream in = new CloseTrackingInputStream(data);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        WebSocketUtils.readBinaryToBuffer(in, out);
        assertThat(out.toByteArray()).isEqualTo(data);
        assertThat(in.closed).isFalse();
    }

    @Test
    @DisplayName("consumeTextFragment: 모든 텍스트를 읽고 스트림을 닫는다")
    void consumeTextFragment_closes() throws Exception {
        CloseTrackingInputStream in = new CloseTrackingInputStream("chunk".getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        WebSocketUtils.consumeTextFragment(in, sb);
        assertThat(sb.toString()).isEqualTo("chunk");
        assertThat(in.closed).isTrue();
    }

    @Test
    @DisplayName("consumeBinaryFragment: 모든 바이너리를 읽고 스트림을 닫는다")
    void consumeBinaryFragment_closes() throws Exception {
        byte[] data = "bin".getBytes(StandardCharsets.UTF_8);
        CloseTrackingInputStream in = new CloseTrackingInputStream(data);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        WebSocketUtils.consumeBinaryFragment(in, out);
        assertThat(out.toByteArray()).isEqualTo(data);
        assertThat(in.closed).isTrue();
    }

    @Test
    @DisplayName("readTextToBuffer: 빈 입력도 정상 처리")
    void readTextToBuffer_empty() throws Exception {
        InputStream in = new ByteArrayInputStream(new byte[0]);
        StringBuilder sb = new StringBuilder();
        WebSocketUtils.readTextToBuffer(in, sb);
        assertThat(sb.toString()).isEmpty();
    }

    @Test
    @DisplayName("readBinaryToBuffer: 빈 입력도 정상 처리")
    void readBinaryToBuffer_empty() throws Exception {
        InputStream in = new ByteArrayInputStream(new byte[0]);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        WebSocketUtils.readBinaryToBuffer(in, out);
        assertThat(out.size()).isZero();
    }
}
