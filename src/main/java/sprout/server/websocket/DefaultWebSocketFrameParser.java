package sprout.server.websocket;

import sprout.beans.annotation.Component;

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
            // 다음 2바이트 = 실제 길이
            actualPayloadLen = ((in.read() & 0xFF) << 8) | (in.read() & 0xFF);
        } else if (payloadLen == 127) {
            // 다음 8바이트 = 실제 길이
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
                throw new RuntimeException("Could not read full masking key.");
            }
        }

        byte[] payloadData = new byte[(int) actualPayloadLen];
        int totalRead = 0;
        while (totalRead < actualPayloadLen) {
            int read = in.read(payloadData, totalRead, (int) (actualPayloadLen - totalRead));
            if (read == -1) break;
            totalRead += read;
        }

        InputStream payloadInputStream = new LimitedInputStream(in, actualPayloadLen);

        // 마스킹된 경우, 마스킹 해제 로직이 적용된 InputStream을 제공해야 함
        // 이는 MaskingInputStream 같은 별도의 래퍼 클래스를 만들 수 있음
        if (masked) {
            // 이 MaskingInputStream은 LimitedInputStream에서 읽은 바이트를 실시간으로 언마스킹
            payloadInputStream = new MaskingInputStream(payloadInputStream, maskingKey);
        }

        return new WebSocketFrame(fin, opcode, payloadInputStream);
    }
}
