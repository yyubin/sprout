package sprout.server.websocket.message;

public class ParsedMessage {
    private final String destination;
    private final String payload;

    public ParsedMessage(String destination, String payload) {
        this.destination = destination;
        this.payload = payload;
    }

    public String getDestination() {
        return destination;
    }

    public String getPayload() {
        return payload;
    }
}
