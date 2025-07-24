package sprout.server.websocket.framehandler.builtins;

import sprout.beans.annotation.Component;
import sprout.beans.annotation.Order;
import sprout.server.websocket.WebSocketFrame;
import sprout.server.websocket.WebSocketUtils;
import sprout.server.websocket.exception.WebSocketProtocolException;
import sprout.server.websocket.framehandler.AbstractFrameHandler;
import sprout.server.websocket.framehandler.FrameProcessingContext;

@Component
@Order(30)
public class ContinuationFragmentHandler extends AbstractFrameHandler {

    @Override
    protected boolean process(FrameProcessingContext state) throws Exception {
        if (!state.isFragmented()) {
            throw new WebSocketProtocolException("Protocol Error: Received continuation frame with no preceding fragmented message.");
        }
        if (state.getFragmentedOpcode() == 0x1) {
            System.out.println("Received fragmented text continuation frame. Buffering...");
            WebSocketUtils.consumeTextFragment(state.getFrame().getPayloadStream(), state.getTextBuffer());
        } else {
            System.out.println("Received fragmented binary continuation frame. Buffering...");
            WebSocketUtils.consumeBinaryFragment(state.getFrame().getPayloadStream(), state.getBinaryBuffer());
        }
        return false;
    }

    @Override
    public boolean canHandle(FrameProcessingContext state) {
        WebSocketFrame frame = state.getFrame();
        return !frame.isFin() && frame.getOpcode() == 0x0;
    }
}
