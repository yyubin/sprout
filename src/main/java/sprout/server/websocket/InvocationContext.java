package sprout.server.websocket;

import sprout.mvc.http.HttpRequest;
import sprout.server.websocket.message.MessagePayload;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.util.Map;

public interface InvocationContext {
    LifecyclePhase phase();                // OPEN, MESSAGE, ERROR, CLOSE
    HttpRequest<?> handshakeRequest();     // null 가능
    WebSocketSession session();            // null 가능 (HTTP)
    Map<String,String> pathVars();
    Map<String,String> queryParams();
    boolean isFin();
    @Nullable Throwable error();           // ERROR phase
    @Nullable CloseCode getCloseCode();   // CLOSE phase
    @Nullable
    MessagePayload getMessagePayload();
    @Nullable
    InputStream getInputStream();
    @Nullable
    WebSocketFrame getFrame();
}
