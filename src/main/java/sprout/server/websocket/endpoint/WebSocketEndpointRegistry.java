package sprout.server.websocket.endpoint;

import sprout.beans.annotation.Component;
import sprout.mvc.mapping.PathPattern;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketEndpointRegistry {

    private final Map<PathPattern, WebSocketEndpointInfo> endpointMappings = new ConcurrentHashMap<>();

    public WebSocketEndpointInfo getEndpointInfo(String path) {
        for (Map.Entry<PathPattern, WebSocketEndpointInfo> entry : endpointMappings.entrySet()) {
            if (entry.getKey().matches(path)) {
                // 필요하다면 여기서 PathPattern.extractPathVariables() 호출하여
                // WebSocketEndpointInfo에 변수를 전달하거나, 나중에 WebSocketSession에서 관리
                return entry.getValue();
            }
        }
        return null;
    }

    public void registerEndpoint(PathPattern pathPattern, Object handlerBean,
                                 Method onOpenMethod, Method onCloseMethod, Method onErrorMethod,
                                 Map<String, Method> messageMappings) { // Map<MessagePath, Method>
        Objects.requireNonNull(pathPattern, "WebSocket endpoint pathPattern cannot be null.");
        Objects.requireNonNull(handlerBean, "WebSocket handler bean cannot be null.");

        if (endpointMappings.containsKey(pathPattern)) {
            System.err.println("Warning: Duplicate WebSocket endpoint path registered: " + pathPattern.getOriginalPattern());
        }

        WebSocketEndpointInfo info = new WebSocketEndpointInfo(pathPattern, handlerBean, onOpenMethod, onCloseMethod, onErrorMethod, messageMappings);
        endpointMappings.put(pathPattern, info);
        System.out.println("Registered WebSocket endpoint: " + pathPattern.getOriginalPattern());
    }


}
