package sprout.server.websocket;

import java.io.IOException;
import java.io.InputStream;

public class LimitedInputStream extends InputStream {
    private final InputStream in;
    private long remaining;

    public LimitedInputStream(InputStream in, long limit) {
        this.in = in;
        this.remaining = limit;
    }

    @Override
    public int read() throws IOException {
        if (remaining <= 0) return -1;
        int b = in.read();
        if (b != -1) remaining--;
        return b;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (remaining <= 0) return -1;
        len = (int) Math.min(len, remaining);
        int n = in.read(b, off, len);
        if (n != -1) remaining -= n;
        return n;
    }

    @Override
    public long skip(long n) throws IOException {
        long toSkip = Math.min(n, remaining);
        long skipped = in.skip(toSkip);
        remaining -= skipped;
        return skipped;
    }

    @Override
    public int available() throws IOException {
        return (int) Math.min(in.available(), remaining);
    }


}
