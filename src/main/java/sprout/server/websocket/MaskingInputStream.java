package sprout.server.websocket;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class MaskingInputStream extends FilterInputStream {
    private final byte[] maskingKey;
    private long bytesRead; // 마스킹 키 인덱스 계산용

    public MaskingInputStream(InputStream in, byte[] maskingKey) {
        super(in);
        if (maskingKey == null || maskingKey.length != 4) {
            throw new IllegalArgumentException("Masking key must be 4 bytes long.");
        }
        this.maskingKey = maskingKey;
        this.bytesRead = 0;
    }

    @Override
    public int read() throws IOException {
        int r = super.read();
        if (r != -1) {
            // FIX : maskingKey 가 byte라서 int로 승격될 때 음수로 확장되고 ^ 연산 결과가 0255 범위를 벗어나 음수가 됨
            // 255 또는 -1만 반환해야 하니까 결과를 & 0xFF로 정리
            int k = maskingKey[(int) (bytesRead & 3)] & 0xFF; // 키도 0~255로
            r = (r ^ k) & 0xFF;                               // 결과도 0~255로
            bytesRead++;
        }
        return r;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int n = super.read(b, off, len);
        if (n != -1) {
            for (int i = 0; i < n; i++) {
                // FIX: 일관성 위해 키 마스킹 추가
                int k = maskingKey[(int) ((bytesRead + i) & 3)] & 0xFF;
                b[off + i] = (byte) ((b[off + i] ^ k) & 0xFF);
            }
            bytesRead += n;
        }
        return n;
    }
}
