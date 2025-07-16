package sprout.server.websocket;

public interface MessageHandler {

    interface Partial<T> extends MessageHandler {
        void onMessage(T messagePart, boolean last);
    }

    interface Whole<T> extends MessageHandler {
        void onMessage(T message);
    }
}
