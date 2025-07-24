package sprout.server.websocket.framehandler;

public interface FrameHandler {
    void setNext(FrameHandler next);
    boolean canHandle(FrameProcessingContext state) throws Exception;
    boolean handle(FrameProcessingContext state) throws Exception;
}
