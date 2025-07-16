package sprout.server.websocket.endpoint;

import sprout.mvc.mapping.PathPattern;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class WebSocketEndpointInfo {
    private final PathPattern pathPattern;
    private final Object handlerBean; // @WebSocketHandler 빈 인스턴스
    private final Method onOpenMethod;
    private final Method onCloseMethod;
    private final Method onErrorMethod;
    private final Map<String, Method> messageMappings;

    public WebSocketEndpointInfo(PathPattern pathPattern, Object handlerBean,
                                 Method onOpenMethod, Method onCloseMethod, Method onErrorMethod,
                                 Map<String, Method> messageMappings) {
        this.pathPattern = pathPattern;
        this.handlerBean = handlerBean;
        this.onOpenMethod = onOpenMethod;
        this.onCloseMethod = onCloseMethod;
        this.onErrorMethod = onErrorMethod;
        this.messageMappings = Collections.unmodifiableMap(new HashMap<>(messageMappings)); // 불변 맵
    }

    public PathPattern getPathPattern() { return pathPattern; }
    public Object getHandlerBean() { return handlerBean; }
    public Method getOnOpenMethod() { return onOpenMethod; }
    public Method getOnCloseMethod() { return onCloseMethod; }
    public Method getOnErrorMethod() { return onErrorMethod; }
    public Map<String, Method> getMessageMappings() { return messageMappings; }

    public Method getMessageMappingMethod(String messagePath) {
        return messageMappings.get(messagePath);
    }

}
