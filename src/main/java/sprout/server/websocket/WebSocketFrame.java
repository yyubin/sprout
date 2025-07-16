package sprout.server.websocket;

public class WebSocketFrame {
    private final boolean fin;
    private final int opcode;
    private final byte[] payload;

    public WebSocketFrame(boolean fin, int opcode, byte[] payload) {
        this.fin = fin;
        this.opcode = opcode;
        this.payload = payload;
    }

    public boolean isFin() {
        return fin;
    }

    public int getOpcode() {
        return opcode;
    }

    public byte[] getPayload() {
        return payload;
    }

    public String getTextPayload() {
        return new String(payload); // 기본 charset UTF-8 assumption
    }
}
