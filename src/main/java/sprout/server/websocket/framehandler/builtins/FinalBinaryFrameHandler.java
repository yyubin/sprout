package sprout.server.websocket.framehandler.builtins;

import sprout.beans.annotation.Component;
import sprout.beans.annotation.Order;
import sprout.server.websocket.WebSocketFrame;
import sprout.server.websocket.WebSocketUtils;
import sprout.server.websocket.exception.WebSocketProtocolException;
import sprout.server.websocket.framehandler.AbstractFrameHandler;
import sprout.server.websocket.framehandler.FrameProcessingContext;

@Component
@Order(60)
public class FinalBinaryFrameHandler extends AbstractFrameHandler {
    @Override
    protected boolean process(FrameProcessingContext state) throws Exception {
        if (state.isFragmented()) {
            throw new WebSocketProtocolException("Protocol Error: Received a single binary frame while in a fragmented message sequence.");
        }
        WebSocketUtils.readBinaryToBuffer(state.getFrame().getPayloadStream(), state.getBinaryBuffer());
        return true;
    }

    @Override
    public boolean canHandle(FrameProcessingContext state) {
        WebSocketFrame frame = state.getFrame();
        return frame.isFin() && frame.getOpcode() == 0x2;
    }
}
