package sprout.server.websocket;

import sprout.beans.annotation.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class DefaultWebSocketFrameEncoder implements WebSocketFrameEncoder{

    @Override
    public byte[] encodeControlFrame(int opcode, byte[] payload) {
        if (payload.length > 125) {
            throw new IllegalArgumentException("Control frame payload too big (must be <= 125)");
        }
        byte[] frame = new byte[2 + payload.length];
        frame[0] = (byte) (0x80 | opcode); // FIN + opcode
        frame[1] = (byte) (payload.length); // No mask, just payload length
        System.arraycopy(payload, 0, frame, 2, payload.length);
        return frame;
    }

    @Override
    public byte[] encodeBinary(byte[] message) {
        int payloadLen = message.length;
        ByteArrayOutputStream frameStream = new ByteArrayOutputStream();

        // 첫 번째 바이트: FIN + opcode (바이너리 0x2)
        frameStream.write(0x82);

        // 두 번째 바이트 및 확 길이 필드
        if (payloadLen < 126) {
            frameStream.write((byte) payloadLen);
        } else if (payloadLen <= 65535) {
            frameStream.write(126);
            frameStream.write((payloadLen >> 8) & 0xFF);
            frameStream.write(payloadLen & 0xFF);
        } else {
            frameStream.write(127);
            for (int i = 0; i < 8; i++) {
                frameStream.write((byte) ((long)payloadLen >> (8 * (7 - i)) & 0xFF));
            }
        }

        // 페이로드 복사
        try {
            frameStream.write(message);
        } catch (IOException e) {
            // ByteArrayOutputStream은 IOException을 던지지 않음
            throw new RuntimeException(e);
        }

        return frameStream.toByteArray();
    }

    @Override
    public byte[] encodeText(String message) {
        byte[] payload = message.getBytes(StandardCharsets.UTF_8);
        int payloadLen = payload.length;

        ByteArrayOutputStream frameStream = new ByteArrayOutputStream();

        // 첫 번째 바이트: FIN + opcode (텍스트 = 0x1)
        frameStream.write(0x81);

        // 두 번째 바이트 및 확장 길이 필드
        if (payloadLen < 126) {
            frameStream.write((byte) payloadLen);
        } else if (payloadLen <= 65535) {
            frameStream.write(126);
            frameStream.write((payloadLen >> 8) & 0xFF);
            frameStream.write(payloadLen & 0xFF);
        } else {
            frameStream.write(127);
            for (int i = 0; i < 8; i++) {
                frameStream.write((byte) ((long)payloadLen >> (8 * (7 - i)) & 0xFF));
            }
        }

        // 페이로드 복사
        try {
            frameStream.write(payload);
        } catch (IOException e) {
            // ByteArrayOutputStream은 IOException을 던지지 않음
            throw new RuntimeException(e);
        }

        return frameStream.toByteArray();
    }
}
