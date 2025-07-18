package sprout.server.websocket;

public class DispatchResult {
    private final boolean handled;
    private final boolean shouldCloseStream;

    public DispatchResult(boolean handled, boolean shouldCloseStream) {
        this.handled = handled;
        this.shouldCloseStream = shouldCloseStream;
    }

    public boolean isHandled() {
        return handled;
    }

    public boolean shouldCloseStream() {
        return shouldCloseStream;
    }
}
