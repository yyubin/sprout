package sprout.server.websocket.framehandler.builtins;

import sprout.beans.annotation.Component;
import sprout.beans.annotation.Order;
import sprout.server.websocket.WebSocketFrame;
import sprout.server.websocket.WebSocketUtils;
import sprout.server.websocket.exception.WebSocketProtocolException;
import sprout.server.websocket.framehandler.AbstractFrameHandler;
import sprout.server.websocket.framehandler.FrameProcessingContext;

import java.io.IOException;

@Component
@Order(10)
public class InitialTextFragmentHandler extends AbstractFrameHandler {
    @Override
    public boolean canHandle(FrameProcessingContext state) throws Exception {
        WebSocketFrame frame = state.getFrame();
        return !frame.isFin() && frame.getOpcode() == 0x1;
    }

    @Override
    protected boolean process(FrameProcessingContext state) throws IOException {
        if (state.isFragmented()) {
            throw new WebSocketProtocolException("Protocol Error: Received a new initial text frame before the previous message was finished.");
        }
        System.out.println("Received first fragmented text frame. Buffering...");
        state.startFragmentedMessage(0x1);
        WebSocketUtils.consumeTextFragment(state.getFrame().getPayloadStream(), state.getTextBuffer());
        return false;
    }
}
