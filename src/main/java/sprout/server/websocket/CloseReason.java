package sprout.server.websocket;

public class CloseReason {
    private final CloseCode closeCode;
    private final String reasonPhrase;

    public CloseReason(CloseCode closeCode, String reasonPhrase) {
        this.closeCode = closeCode;
        this.reasonPhrase = reasonPhrase;
    }

    public CloseCode getCloseCode() {
        return closeCode;
    }

    public String getReasonPhrase() {
        return reasonPhrase;
    }

    @Override
    public String toString() {
        return "CloseReason: code [" + closeCode.getCode() + "], reason [" + reasonPhrase + "]";
    }

}
