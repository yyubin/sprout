package sprout.server.websocket;

import sprout.server.websocket.endpoint.Decoder;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

public class WebSocketFrameDecoder implements Decoder {

    public static String decodeText(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            char[] buf = new char[1024];
            int len;
            while ((len = reader.read(buf)) != -1) {
                sb.append(buf, 0, len);
            }
        }
        return sb.toString();
    }

//    public static String decodeText(WebSocketFrame frame) {
//        if (frame.getOpcode() != 0x1 && frame.getOpcode() != 0x0) {
//            throw new IllegalArgumentException("Not a text frame: opcode=" + frame.getOpcode());
//        }
//        return new String(frame.getPayload(), StandardCharsets.UTF_8);
//    }

//    public static byte[] decodeBinary(WebSocketFrame frame) {
//        if (frame.getOpcode() != 0x2 && frame.getOpcode() != 0x0) {
//            throw new IllegalArgumentException("Not a binary frame: opcode=" + frame.getOpcode());
//        }
//        return frame.getPayload();
//    }

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
        return frame.getOpcode() == 0x0 || frame.getOpcode() == 0x1 || frame.getOpcode() == 0x2;
    }

    public static CloseCode getCloseCode(byte[] payload) {
        if (payload == null || payload.length < 2) {
            // 페이로드가 없거나 2바이트 미만이면 기본 코드 반환 또는 예외
            return CloseCodes.NO_STATUS_CODE; // 또는 PROTOCOL_ERROR
        }
        // 첫 2바이트를 Big-Endian으로 읽어 CloseCode 반환
        int code = ((payload[0] & 0xFF) << 8) | (payload[1] & 0xFF);
        return CloseCodes.getCloseCode(code);
    }
}
