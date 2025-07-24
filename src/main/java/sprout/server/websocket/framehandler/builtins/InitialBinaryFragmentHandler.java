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
@Order(20)
public class InitialBinaryFragmentHandler extends AbstractFrameHandler {
    @Override
    public boolean canHandle(FrameProcessingContext state) throws Exception {
        WebSocketFrame frame = state.getFrame();
        return !frame.isFin() && frame.getOpcode() == 0x2;
    }

    @Override
    protected boolean process(FrameProcessingContext state) throws IOException {
        if (state.isFragmented()) {
            throw new WebSocketProtocolException("Protocol Error: Received a new initial binary frame before the previous message was finished.");
        }
        System.out.println("Received first fragmented binary frame. Buffering...");
        state.startFragmentedMessage(0x2);
        WebSocketUtils.consumeBinaryFragment(state.getFrame().getPayloadStream(), state.getBinaryBuffer());
        return false;
    }
}
