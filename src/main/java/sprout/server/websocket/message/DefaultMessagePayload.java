package sprout.server.websocket.message;

public class DefaultMessagePayload implements MessagePayload{
    private final String stringPayload;
    private final byte[] binaryPayload;

    public DefaultMessagePayload(String stringPayload, byte[] binaryPayload) {
        this.stringPayload = stringPayload;
        this.binaryPayload = binaryPayload;
    }

    @Override
    public boolean isText() {
        return !stringPayload.isEmpty();
    }

    @Override
    public boolean isBinary() {
        return binaryPayload.length > 0;
    }

    @Override
    public String asText() {
        if (isBinary()) {
            return new String(binaryPayload);
        }
        return stringPayload;
    }

    @Override
    public byte[] asBinary() {
        if (isText()) {
            return asText().getBytes();
        }
        return binaryPayload;
    }
}
