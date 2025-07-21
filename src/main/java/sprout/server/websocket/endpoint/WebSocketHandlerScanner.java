package sprout.server.websocket.endpoint;

import sprout.aop.annotation.Around;
import sprout.beans.annotation.Component;
import sprout.context.BeanFactory;
import sprout.context.Container;
import sprout.mvc.mapping.PathPattern;
import sprout.mvc.mapping.PathPatternResolver;
import sprout.server.websocket.CloseCode;
import sprout.server.websocket.WebSocketSession;
import sprout.server.websocket.annotation.*;

import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Component
public class WebSocketHandlerScanner {
    private final WebSocketEndpointRegistry endpointRegistry;
    private final PathPatternResolver pathPatternResolver;

    public WebSocketHandlerScanner(WebSocketEndpointRegistry endpointRegistry, PathPatternResolver pathPatternResolver) {
        this.endpointRegistry = endpointRegistry;
        this.pathPatternResolver = pathPatternResolver;
    }

    public void scanWebSocketHandlers(BeanFactory context) {
        Collection<Object> beans = context.getAllBeans();
        System.out.println(beans.size() + " beans found for WebSocket scan.");

        for (Object bean : beans) {
            Class<?> beanClass = bean.getClass();
            if (beanClass.isAnnotationPresent(WebSocketHandler.class)) {
                System.out.println("Found @WebSocketHandler: " + beanClass.getName());

                WebSocketHandler webSocketHandlerAnn = beanClass.getAnnotation(WebSocketHandler.class);
                String classLevelPath = webSocketHandlerAnn.value(); // @WebSocketHandler의 value()는 엔드포인트 경로

                PathPattern pathPattern = pathPatternResolver.resolve(classLevelPath);

                Method onOpenMethod = null;
                Method onCloseMethod = null;
                Method onErrorMethod = null;
                Map<String, Method> messageMappings = new HashMap<>();

                for (Method method : beanClass.getMethods()) { // public 메서드 스캔
                    method.setAccessible(true); // 리플렉션 호출을 위해

                    if (method.isAnnotationPresent(OnOpen.class)) {
                        onOpenMethod = method;
                    } else if (method.isAnnotationPresent(OnClose.class)) {
                        onCloseMethod = method;
                    } else if (method.isAnnotationPresent(OnError.class)) {
                        onErrorMethod = method;
                    } else if (method.isAnnotationPresent(MessageMapping.class)) {
                        MessageMapping messageMappingAnn = method.getAnnotation(MessageMapping.class);
                        String messagePath = messageMappingAnn.value();
                        messageMappings.put(messagePath, method);
                    }
                    // TODO: 메서드 시그니처 (파라미터 타입) 검증 로직 추가 (WebSocketSession, String message 등)
                }

                endpointRegistry.registerEndpoint(pathPattern, bean, onOpenMethod, onCloseMethod, onErrorMethod, messageMappings);
            }
        }
    }
}
