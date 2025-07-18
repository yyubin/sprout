package sprout.server.websocket;

import sprout.server.websocket.endpoint.Encoder;

public interface WebSocketFrameEncoder extends Encoder {
    byte[] encodeControlFrame(int opcode, byte[] payload);

    byte[] encodeText(String message);
}
