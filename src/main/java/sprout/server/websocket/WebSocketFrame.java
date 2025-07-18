package sprout.server.websocket;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class WebSocketFrame {
    private final boolean fin;
    private final int opcode;
    // private final byte[] payload;
    private final InputStream payloadStream;

    public WebSocketFrame(boolean fin, int opcode, InputStream payloadStream) { // <-- 생성자 변경
        this.fin = fin;
        this.opcode = opcode;
        this.payloadStream = payloadStream; // LimitedInputStream 인스턴스가 들어올 것
    }

    public boolean isFin() {
        return fin;
    }

    public int getOpcode() {
        return opcode;
    }

    public InputStream getPayloadStream() { return payloadStream; }

    // 필요하다면, 스트림을 읽어 전체 페이로드를 바이트 배열로 반환하는 헬퍼 메서드
    public byte[] getPayloadBytes() throws IOException {
        if (payloadStream == null) return new byte[0];
        // InputStream을 byte[]로 읽는 유틸리티 사용 (예: IOUtils.toByteArray(payloadStream))
        // 이 메서드는 스트림의 모든 데이터를 메모리에 올리므로, 대용량 메시지에는 적합하지 않음
        // LimitedInputStream의 limit만큼만 읽어옴
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[4096]; // 4KB 버퍼
        while ((nRead = payloadStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        return buffer.toByteArray();
    }

    // 텍스트 페이로드를 가져오는 메서드 (성능 고려하여 신중하게 사용)
    public String getTextPayload() throws IOException {
        return new String(getPayloadBytes(), StandardCharsets.UTF_8);
    }
}
