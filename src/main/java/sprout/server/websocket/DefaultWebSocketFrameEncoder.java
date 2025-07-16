package sprout.server.websocket;

import sprout.beans.annotation.Component;

import java.nio.charset.StandardCharsets;

@Component
public class DefaultWebSocketFrameEncoder implements WebSocketFrameEncoder{

    @Override
    public byte[] encodeText(String message) {
        byte[] payload = message.getBytes(StandardCharsets.UTF_8);
        int payloadLen = payload.length;

        int headerSize = 2; // 기본 헤더 크기
        if (payloadLen >= 126 && payloadLen <= 65535) {
            headerSize += 2;
        } else if (payloadLen > 65535) {
            headerSize += 8;
        }

        byte[] frame = new byte[headerSize + payloadLen];

        // 첫 번째 바이트: FIN + opcode (텍스트 = 0x1)
        frame[0] = (byte) 0x81;

        // 두 번째 바이트: MASK 비트 없음 (0), 서버는 마스킹 안 함
        if (payloadLen < 126) {
            frame[1] = (byte) payloadLen;
        } else if (payloadLen <= 65535) {
            frame[1] = 126;
            frame[2] = (byte) ((payloadLen >> 8) & 0xFF);
            frame[3] = (byte) (payloadLen & 0xFF);
        } else {
            frame[1] = 127;
            for (int i = 0; i < 8; i++) {
                frame[2 + i] = (byte) ((payloadLen >> (8 * (7 - i))) & 0xFF);
            }
        }

        // payload 복사
        System.arraycopy(payload, 0, frame, headerSize, payloadLen);

        return frame;
    }
}
