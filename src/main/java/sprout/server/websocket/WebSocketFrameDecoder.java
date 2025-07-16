package sprout.server.websocket;

import java.nio.charset.StandardCharsets;

public class WebSocketFrameDecoder {
    public static String decodeText(WebSocketFrame frame) {
        if (frame.getOpcode() != 0x1) {
            throw new IllegalArgumentException("Not a text frame: opcode=" + frame.getOpcode());
        }
        return new String(frame.getPayload(), StandardCharsets.UTF_8);
    }

    public static byte[] decodeBinary(WebSocketFrame frame) {
        if (frame.getOpcode() != 0x2) {
            throw new IllegalArgumentException("Not a binary frame: opcode=" + frame.getOpcode());
        }
        return frame.getPayload();
    }

    public static boolean isCloseFrame(WebSocketFrame frame) {
        return frame.getOpcode() == 0x8;
    }

    public static boolean isPingFrame(WebSocketFrame frame) {
        return frame.getOpcode() == 0x9;
    }

    public static boolean isPongFrame(WebSocketFrame frame) {
        return frame.getOpcode() == 0xA;
    }

    public static boolean isDataFrame(WebSocketFrame frame) {
        return frame.getOpcode() == 0x1 || frame.getOpcode() == 0x2;
    }

}
