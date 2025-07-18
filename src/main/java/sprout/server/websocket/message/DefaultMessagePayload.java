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
        return stringPayload != null;
    }

    @Override
    public boolean isBinary() {
        return binaryPayload != null;
    }

    @Override
    public String asText() {
        if (stringPayload == null) {
            return new String(binaryPayload);
        }
        return stringPayload;
    }

    @Override
    public byte[] asBinary() {
        if (binaryPayload == null) {
            return asText().getBytes();
        }
        return binaryPayload;
    }
}
