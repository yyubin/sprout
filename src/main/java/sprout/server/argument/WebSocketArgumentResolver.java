package sprout.server.argument;

import sprout.server.websocket.InvocationContext;

import java.lang.reflect.Parameter;

public interface WebSocketArgumentResolver {
    boolean supports(Parameter parameter, InvocationContext context); // <- context 추가

    Object resolve(Parameter parameter, InvocationContext context) throws Exception; // <- context로 통합
}
