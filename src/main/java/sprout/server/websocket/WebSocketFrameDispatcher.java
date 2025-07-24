package sprout.server.websocket;

import sprout.server.websocket.framehandler.FrameHandler;
import sprout.server.websocket.framehandler.FrameProcessingContext;
import sprout.server.websocket.message.MessagePayload;
import sprout.server.websocket.message.WebSocketMessageDispatcher;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class WebSocketFrameDispatcher {
    private final FrameHandler handlerChain;
    private final List<WebSocketMessageDispatcher> messageDispatchers;

    public WebSocketFrameDispatcher(List<FrameHandler> handlers, List<WebSocketMessageDispatcher> messageDispatchers) {
        this.messageDispatchers = messageDispatchers;
        this.handlerChain = buildHandlerChain(handlers);
    }

    private FrameHandler buildHandlerChain(List<FrameHandler> handlers) {
        if (handlers == null || handlers.isEmpty()) {
            throw new IllegalStateException("No FrameHandler beans found. Cannot build handler chain.");
        }
        // 리스트의 순서대로 next 핸들러를 설정
        for (int i = 0; i < handlers.size() - 1; i++) {
            handlers.get(i).setNext(handlers.get(i + 1));
        }
        return handlers.get(0); // 체인의 시작점 반환
    }

    public void dispatch(FrameProcessingContext state, WebSocketSession webSocketSession, Map<String, String> pathParameters) throws Exception {
        // 핸들러 체인에 프레임 처리 위임
        boolean messageCompleted = handlerChain.handle(state);

        if (messageCompleted) {
            // 메시지가 완성되면, 실제 비즈니스 로직을 처리할 디스패처에게 위임
            MessagePayload payload = state.createPayload();
            InvocationContext contextWithPayload = new DefaultInvocationContext(webSocketSession, pathParameters, payload, state.getFrame());

            DispatchResult result = null;
            try {
                for (WebSocketMessageDispatcher dispatcher : messageDispatchers) {
                    if (dispatcher.supports(state.getFrame(), contextWithPayload)) {
                        result = dispatcher.dispatch(state.getFrame(), contextWithPayload);
                        if (result.isHandled()) {
                            break;
                        }
                    }
                }
            } finally {
                // 스트림 닫기 및 상태 초기화
                if (result == null || result.shouldCloseStream()) {
                    InputStream stream = contextWithPayload.getFrame().getPayloadStream();
                    if (stream != null) {
                        try { stream.close(); } catch (IOException e) { System.err.println("Failed to close payload input stream: " + e.getMessage()); }
                    }
                }
                state.reset(); // 버퍼 및 분할 메시지 상태 초기화
            }

            if (result == null || !result.isHandled()) {
                System.err.println("No suitable WebSocketMessageDispatcher found for frame: " + state.getFrame().getOpcode() + " (FIN: " + state.getFrame().isFin() + ")");
            }
        }
    }
}
