package sprout.server.websocket.message.builtins;

import com.fasterxml.jackson.databind.ObjectMapper;
import sprout.beans.annotation.Component;
import sprout.server.websocket.DefaultInvocationContext;
import sprout.server.websocket.InvocationContext;
import sprout.server.argument.WebSocketArgumentResolver;
import sprout.server.websocket.WebSocketFrame;
import sprout.server.websocket.WebSocketFrameDecoder;
import sprout.server.websocket.WebSocketSession;
import sprout.server.websocket.endpoint.WebSocketEndpointInfo;
import sprout.server.websocket.message.ParsedMessage;
import sprout.server.websocket.message.WebSocketMessageDispatcher;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;

@Component
public class JsonWebSocketMessageDispatcher implements WebSocketMessageDispatcher {
    private final ObjectMapper objectMapper;
    private final List<WebSocketArgumentResolver> argumentResolvers; // 인자 리졸버도 주입
    private final WebSocketEndpointInfo endpointInfo;

    public JsonWebSocketMessageDispatcher(ObjectMapper objectMapper, List<WebSocketArgumentResolver> argumentResolvers, WebSocketEndpointInfo endpointInfo) {
        this.objectMapper = objectMapper;
        this.argumentResolvers = argumentResolvers;
        this.endpointInfo = endpointInfo;
    }


    @Override
    public boolean supports(WebSocketFrame frame, InvocationContext context) {
        if (!WebSocketFrameDecoder.isDataFrame(frame) || (frame.getOpcode() != 0x1 && frame.getOpcode() != 0x0)) {
            return false;
        }
        // TODO: (선택적) 페이로드 시작 부분이 '{' 인지 확인하는 등 JSON 여부 힌트 추가
        return true;
    }

    @Override
    public boolean dispatch(WebSocketFrame frame, InvocationContext context) throws Exception {
        // InvocationContext에서 최종 조립된 메시지 페이로드를 가져옴
        String messageContent = context.getMessagePayload().asText(); // InvocationContext.payload()는 이미 최종 조립된 String

        // JSON 파싱
        ParsedMessage parsedMessage = objectMapper.readValue(messageContent, ParsedMessage.class); // ParsedMessage를 바로 파싱하도록

        String destination = parsedMessage.getDestination();
        String payload = parsedMessage.getPayload();

        if (destination == null || destination.isBlank()) {
            System.err.println("WebSocket message has no destination path. Skipping dispatch.");
            return false; // 처리 못 함 (다음 디스패처에게 기회)
        }

        // EndpointInfo는 InvocationContext나 Session에서 가져와야 함
        WebSocketEndpointInfo currentEndpointInfo = (context.session() instanceof WebSocketSession) ?
                ((WebSocketSession) context.session()).getEndpointInfo() : // 가상 메서드 getEndpointInfo()
                this.endpointInfo; // 생성자 주입된 경우

        if (currentEndpointInfo == null) {
            System.err.println("EndpointInfo not available in context. Cannot dispatch message.");
            return false;
        }

        Method messageMappingMethod = currentEndpointInfo.getMessageMappingMethod(destination);
        if (messageMappingMethod == null) {
            System.err.println("No @MessageMapping found for path: " + destination);
            return false; // 처리 못 함
        }

        // InvocationContext를 업데이트하거나 새롭게 생성 (페이로드는 이미 파싱된 JSON 본문)
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
                if (resolver.supports(parameters[i], context)) {
                    args[i] = resolver.resolve(parameters[i], context);
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
