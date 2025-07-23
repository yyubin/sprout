package sprout.server.websocket.support;

import sprout.server.websocket.*;
import sprout.server.websocket.exception.NotEnoughDataException;
import sprout.server.websocket.message.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Queue;

public final class Fakes {

    // FRAME ENCODER
    public static class FakeEncoder implements WebSocketFrameEncoder {
        @Override public byte[] encodeText(String message) { return ("T:" + message).getBytes(StandardCharsets.UTF_8); }
        @Override public byte[] encodeBinary(byte[] data)   { return ("B:" + data.length).getBytes(StandardCharsets.UTF_8); }
        @Override public byte[] encodeControlFrame(int opcode, byte[] payload) {
            return ("C:" + opcode + ":" + (payload == null ? 0 : payload.length)).getBytes(StandardCharsets.UTF_8);
        }
    }

    // FRAME PARSER : 미리 넣어둔 프레임을 차례로 반환, 없으면 NotEnoughDataException
    public static class QueueParser implements WebSocketFrameParser {
        private final Queue<WebSocketFrame> frames = new ArrayDeque<>();
        public void add(WebSocketFrame f) { frames.add(f); }

        @Override
        public WebSocketFrame parse(InputStream in) throws NotEnoughDataException {
            WebSocketFrame f = frames.poll();
            if (f == null) throw new NotEnoughDataException();
            return f;
        }
    }

    // 메시지 디스패처
    public static class CapturingDispatcher implements WebSocketMessageDispatcher {
        public WebSocketFrame lastFrame;
        public InvocationContext lastCtx;
        private final boolean handled;
        private final boolean closeStream;

        public CapturingDispatcher(boolean handled, boolean closeStream) {
            this.handled = handled;
            this.closeStream = closeStream;
        }

        @Override public boolean supports(WebSocketFrame frame, InvocationContext ctx) { return true; }
        @Override public DispatchResult dispatch(WebSocketFrame frame, InvocationContext ctx) {
            lastFrame = frame;
            lastCtx   = ctx;
            return new DispatchResult(handled, closeStream);
        }
    }

    // InputStream 닫힘 여부 확인용
    public static class CloseTrackingInputStream extends ByteArrayInputStream {
        public boolean closed;
        public CloseTrackingInputStream(byte[] buf) { super(buf); }
        @Override public void close() throws IOException { closed = true; super.close(); }
    }
}
