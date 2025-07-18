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
        int b = super.read();
        if (b != -1) {
            b = b ^ maskingKey[(int) (bytesRead % 4)];
            bytesRead++;
        }
        return b;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int n = super.read(b, off, len);
        if (n != -1) {
            for (int i = 0; i < n; i++) {
                b[off + i] = (byte) (b[off + i] ^ maskingKey[(int) ((bytesRead + i) % 4)]);
            }
            bytesRead += n;
        }
        return n;
    }
}
