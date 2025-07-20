package sprout.server.websocket.message;

import sprout.server.argument.WebSocketArgumentResolver;
import sprout.server.websocket.DispatchResult;
import sprout.server.websocket.InvocationContext;
import sprout.server.websocket.WebSocketFrame;
import sprout.server.websocket.WebSocketSession;
import sprout.server.websocket.endpoint.WebSocketEndpointInfo;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Objects;

public abstract class AbstractWebSocketMessageDispatcher implements WebSocketMessageDispatcher {

    private final List<WebSocketArgumentResolver> argumentResolvers;

    public AbstractWebSocketMessageDispatcher(List<WebSocketArgumentResolver> argumentResolvers) {
        this.argumentResolvers = Objects.requireNonNull(argumentResolvers);
    }

    protected abstract DispatchInfo prepareDispatchInfo(InvocationContext context) throws Exception;

    @Override
    public final DispatchResult dispatch(WebSocketFrame frame, InvocationContext context) throws Exception {
        // 1. 하위 클래스에서 메시지 파싱 및 정보 준비
        DispatchInfo dispatchInfo = prepareDispatchInfo(context);
        if (dispatchInfo == null || dispatchInfo.destination() == null) {
            System.err.println("WebSocket message has no destination path. Skipping dispatch.");
            return new DispatchResult(false, true); // 스트림은 이미 소비되었을 수 있음
        }

        // 2. 엔드포인트 정보 가져오기
        WebSocketEndpointInfo endpointInfo = context.session().getEndpointInfo();
        if (endpointInfo == null) {
            System.err.println("EndpointInfo not available in context. Cannot dispatch message.");
            return new DispatchResult(false, true);
        }

        // 3. 목적지에 맞는 핸들러 메서드 찾기
        Method messageMappingMethod = endpointInfo.getMessageMappingMethod(dispatchInfo.destination());
        if (messageMappingMethod == null) {
            System.err.println("No @MessageMapping found for path: " + dispatchInfo.destination());
            return new DispatchResult(false, true);
        }

        // 4. 메서드 인자 해석 및 호출
        Object[] args = resolveArgs(messageMappingMethod, context);
        messageMappingMethod.invoke(endpointInfo.getHandlerBean(), args);

        // 5. 결과 반환
        return new DispatchResult(true, !wasStreamPassedToHandler(messageMappingMethod));
    }

    private boolean wasStreamPassedToHandler(Method handlerMethod) {
        if (handlerMethod == null) return false;
        for (Parameter param : handlerMethod.getParameters()) {
            if (InputStream.class.isAssignableFrom(param.getType())) return true;
        }
        return false;
    }

    private Object[] resolveArgs(Method method, InvocationContext context) throws Exception {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            boolean resolved = false;
            for (WebSocketArgumentResolver resolver : argumentResolvers) {
                if (resolver.supports(parameters[i], context)) {
                    args[i] = resolver.resolve(parameters[i], context);
                    resolved = true;
                    break;
                }
            }
            if (!resolved) {
                throw new IllegalArgumentException("No WebSocketArgumentResolver found for parameter: " + parameters[i].getName());
            }
        }
        return args;
    }

    public record DispatchInfo(String destination, Object payload) {}
}