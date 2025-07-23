package sprout.server.websocket;

import sprout.beans.annotation.Component;

import java.io.IOException;
import java.io.InputStream;

@Component
public class DefaultWebSocketFrameParser implements WebSocketFrameParser {

    @Override
    public WebSocketFrame parse(InputStream in) throws Exception {
        int b1 = in.read();
        int b2 = in.read();

        if (b1 == -1 || b2 == -1) {
            throw new RuntimeException("Unexpected end of stream while reading frame header.");
        }

        boolean fin = (b1 & 0x80) != 0;
        int opcode = b1 & 0x0F;

        boolean masked = (b2 & 0x80) != 0;
        int payloadLen = b2 & 0x7F;

        long actualPayloadLen;

        if (payloadLen == 126) {
            actualPayloadLen = ((in.read() & 0xFF) << 8) | (in.read() & 0xFF);
        } else if (payloadLen == 127) {
            actualPayloadLen = 0;
            for (int i = 0; i < 8; i++) {
                actualPayloadLen = (actualPayloadLen << 8) | (in.read() & 0xFF);
            }
        } else {
            actualPayloadLen = payloadLen;
        }

        byte[] maskingKey = new byte[4];
        if (masked) {
            if (in.read(maskingKey) != 4) {
                throw new IOException("Failed to read full 4-byte masking key from client stream.");
            }
        }

        // --- BUG FIX ---
        // [삭제] 불필요하게 페이로드를 미리 읽는 로직을 제거합니다.
        /*
        byte[] payloadData = new byte[(int) actualPayloadLen];
        int totalRead = 0;
        while (totalRead < actualPayloadLen) {
            int read = in.read(payloadData, totalRead, (int) (actualPayloadLen - totalRead));
            if (read == -1) break;
            totalRead += read;
        }
        */

        // 헤더와 마스킹 키 까지만 읽은 스트림을 LimitedInputStream으로 감쌉니다.
        InputStream payloadInputStream = new LimitedInputStream(in, actualPayloadLen);

        if (masked) {
            // 이 MaskingInputStream은 LimitedInputStream에서 읽은 바이트를 실시간으로 언마스킹합니다.
            payloadInputStream = new MaskingInputStream(payloadInputStream, maskingKey);
        }

        return new WebSocketFrame(fin, opcode, payloadInputStream);
    }
}
