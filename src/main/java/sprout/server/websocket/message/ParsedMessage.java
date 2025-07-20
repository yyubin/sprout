package sprout.server.websocket.message;

public class ParsedMessage {
    private String destination;
    private String payload;

    public ParsedMessage() {
    }

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
