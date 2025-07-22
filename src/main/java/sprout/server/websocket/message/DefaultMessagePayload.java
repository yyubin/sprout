package sprout.server.websocket.message;

import java.nio.charset.StandardCharsets;

public class DefaultMessagePayload implements MessagePayload{
    private final String stringPayload;
    private final byte[] binaryPayload;

    public DefaultMessagePayload(String stringPayload, byte[] binaryPayload) {
        this.stringPayload = stringPayload;
        this.binaryPayload = binaryPayload;
    }

    @Override
    public boolean isText() {
        return stringPayload != null && !stringPayload.isEmpty();
    }

    @Override
    public boolean isBinary() {
        return binaryPayload != null && binaryPayload.length > 0;
    }

    @Override
    public String asText() {
        if (isBinary()) {
            return new String(binaryPayload, StandardCharsets.UTF_8);
        }
        return stringPayload;
    }

    @Override
    public byte[] asBinary() {
        if (isText()) {
            return stringPayload.getBytes(StandardCharsets.UTF_8);
        }
        return binaryPayload;
    }
}
