package sprout.server.argument.builtins;

import sprout.beans.annotation.Component;
import sprout.server.argument.annotation.Payload;
import sprout.server.websocket.InvocationContext;
import sprout.server.argument.WebSocketArgumentResolver;
import sprout.server.websocket.LifecyclePhase;

import java.io.InputStream;
import java.lang.reflect.Parameter;

@Component
public class InputStreamPayloadArgumentResolver implements WebSocketArgumentResolver {
    @Override
    public boolean supports(Parameter parameter, InvocationContext context) {
        return  parameter.isAnnotationPresent(Payload.class) &&
                InputStream.class.isAssignableFrom(parameter.getType()) &&
                context.phase() == LifecyclePhase.MESSAGE &&
                context.getFrame() != null && // InvocationContext에 프레임이 있어야 함
                context.getMessagePayload() == null && // 메시지 페이로드가 아직 재조립되지 않았어야 함 (스트리밍 의도)
                context.isFin();  // 최종 프레임이어야 함 (부분 메시지 스트림은 더 복잡)
    }

    @Override
    public Object resolve(Parameter parameter, InvocationContext context) throws Exception {
        return context.getFrame().getPayloadStream();
    }
}
