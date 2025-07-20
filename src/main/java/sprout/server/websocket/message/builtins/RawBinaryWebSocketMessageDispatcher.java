package sprout.server.websocket.message.builtins;

import sprout.beans.annotation.Component;
import sprout.server.argument.WebSocketArgumentResolver;
import sprout.server.websocket.InvocationContext;
import sprout.server.websocket.WebSocketFrame;
import sprout.server.websocket.message.AbstractWebSocketMessageDispatcher;

import java.util.List;

@Component
public class RawBinaryWebSocketMessageDispatcher extends AbstractWebSocketMessageDispatcher {

    public RawBinaryWebSocketMessageDispatcher(List<WebSocketArgumentResolver> argumentResolvers) {
        super(argumentResolvers);
    }

    @Override
    public boolean supports(WebSocketFrame frame, InvocationContext context) {
        // 바이너리 프레임(opcode 0x2) 또는 바이너리 연속 프레임(opcode 0x0)만 지원
        return (frame.getOpcode() == 0x2 || frame.getOpcode() == 0x0) && context.getMessagePayload().isBinary();
    }

    @Override
    protected DispatchInfo prepareDispatchInfo(InvocationContext context) {
        // 모든 바이너리 메시지는 고정된 "/binary" 목적지로 라우팅
        String destination = "/binary";
        byte[] payload = context.getMessagePayload().asBinary();

        return new DispatchInfo(destination, payload);
    }
}