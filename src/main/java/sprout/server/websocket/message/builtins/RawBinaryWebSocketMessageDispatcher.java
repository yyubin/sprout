package sprout.server.websocket.message.builtins;

import sprout.beans.annotation.Component;
import sprout.server.websocket.DefaultInvocationContext;
import sprout.server.websocket.InvocationContext;
import sprout.server.argument.WebSocketArgumentResolver;
import sprout.server.websocket.WebSocketFrame;
import sprout.server.websocket.WebSocketSession;
import sprout.server.websocket.endpoint.WebSocketEndpointInfo;
import sprout.server.websocket.message.WebSocketMessageDispatcher;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;

@Component
public class RawBinaryWebSocketMessageDispatcher implements WebSocketMessageDispatcher {
    private final List<WebSocketArgumentResolver> argumentResolvers;
    private final WebSocketEndpointInfo endpointInfo; // EndpointInfo 접근용

    public RawBinaryWebSocketMessageDispatcher(List<WebSocketArgumentResolver> argumentResolvers, WebSocketEndpointInfo endpointInfo) {
        this.argumentResolvers = argumentResolvers;
        this.endpointInfo = endpointInfo;
    }

    @Override
    public boolean supports(WebSocketFrame frame, InvocationContext context) {
        return (frame.getOpcode() == 0x2 || frame.getOpcode() == 0x0) && context.getMessagePayload().isBinary();
    }

    @Override
    public boolean dispatch(WebSocketFrame frame, InvocationContext context) throws Exception {
        byte[] rawBinaryPayload = context.getMessagePayload().asBinary();
        String destination = "/binary"; // 임시 목적지, 또는 메시지 내용에서 추출
        WebSocketEndpointInfo currentEndpointInfo = (context.session() instanceof WebSocketSession) ?
                ((WebSocketSession) context.session()).getEndpointInfo() :
                this.endpointInfo;

        if (currentEndpointInfo == null) {
            System.err.println("EndpointInfo not available in context for binary dispatch. Cannot dispatch message.");
            return false;
        }
        Method messageMappingMethod = currentEndpointInfo.getMessageMappingMethod(destination);
        if (messageMappingMethod == null) {
            System.err.println("No @MessageMapping found for binary path: " + destination);
            return false;
        }

        InvocationContext updatedContext = new DefaultInvocationContext(context.session(), context.pathVars(), context.getMessagePayload());

        Object[] args = resolveArgs(messageMappingMethod, updatedContext); // ArgumentResolver 사용
        messageMappingMethod.invoke(currentEndpointInfo.getHandlerBean(), args);

        return true;
    }

    private Object[] resolveArgs(Method method, InvocationContext context) throws Exception {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            boolean resolved = false;
            for (WebSocketArgumentResolver resolver : argumentResolvers) {
                if (resolver.supports(parameters[i], context)) { // <- InvocationContext 전달
                    args[i] = resolver.resolve(parameters[i], context); // <- InvocationContext 전달
                    resolved = true;
                    break;
                }
            }
            if (!resolved) {
                throw new IllegalArgumentException("No WebSocketArgumentResolver found for parameter: " + parameters[i].getName() + " in method " + method.getName() + " for phase " + context.phase());
            }
        }
        return args;
    }
}

