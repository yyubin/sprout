package sprout.server.websocket.framehandler;

import sprout.server.websocket.exception.WebSocketProtocolException;

public abstract class AbstractFrameHandler implements FrameHandler{
    protected FrameHandler next;

    @Override
    public void setNext(FrameHandler next) {
        this.next = next;
    }

    @Override
    public boolean handle(FrameProcessingContext state) throws Exception {
        if (canHandle(state)) {
            return process(state);
        } else if (next != null) {
            return next.handle(state);
        }
        throw new WebSocketProtocolException("Unknown or invalid frame sequence for opcode: " + state.getFrame().getOpcode());
    }

    protected abstract boolean process(FrameProcessingContext state) throws Exception;
}
