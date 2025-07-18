package sprout.server.websocket;

import sprout.beans.annotation.Component;
import sprout.mvc.http.HttpRequest;
import sprout.server.websocket.message.MessagePayload;

import javax.annotation.Nullable;
import java.util.Map;

@Component
public class DefaultInvocationContext implements InvocationContext{
    private final LifecyclePhase phase;
    private final HttpRequest<?> handshakeRequest;
    private final WebSocketSession session;
    private final Map<String, String> pathVars;
    private final Map<String, String> queryParams;
    private final MessagePayload messagePayload;
    private final Throwable error;
    private final CloseCode closeCode;


    public DefaultInvocationContext(LifecyclePhase phase, HttpRequest<?> handshakeRequest, WebSocketSession session, Map<String, String> pathVars, Map<String, String> queryParams, MessagePayload messagePayload, Throwable error, CloseCode closeCode) {
        this.phase = phase;
        this.handshakeRequest = handshakeRequest;
        this.session = session;
        this.pathVars = pathVars;
        this.queryParams = queryParams;
        this.messagePayload = messagePayload;
        this.error = error;
        this.closeCode = closeCode;
    }

    // @OnOpen용 생성자
    public DefaultInvocationContext(HttpRequest<?> handshakeRequest, WebSocketSession session, Map<String, String> pathVars) {
        this(LifecyclePhase.OPEN, handshakeRequest, session, pathVars,
                handshakeRequest.getQueryParams(), null, null, null);
    }

    // @MessageMapping용 생성자
    public DefaultInvocationContext(WebSocketSession session, Map<String, String> pathVars, MessagePayload payload) {
        this(LifecyclePhase.MESSAGE, session.getHandshakeRequest(), session, pathVars,
                session.getHandshakeRequest().getQueryParams(), payload, null, null);
    }

    // @OnError용 생성자
    public DefaultInvocationContext(WebSocketSession session, Map<String, String> pathVars, Throwable error) {
        this(LifecyclePhase.ERROR, session.getHandshakeRequest(), session, pathVars,
                session.getHandshakeRequest().getQueryParams(), null, error, null);
    }

    // @OnClose용 생성자
    public DefaultInvocationContext(WebSocketSession session, Map<String, String> pathVars, CloseCode closeCode) {
        this(LifecyclePhase.CLOSE, session.getHandshakeRequest(), session, pathVars,
                session.getHandshakeRequest().getQueryParams(), null, null, closeCode);
    }

    @Override
    public LifecyclePhase phase() { return phase; }
    @Override
    public HttpRequest<?> handshakeRequest() { return handshakeRequest; }
    @Override
    public WebSocketSession session() { return session; }
    @Override
    public Map<String, String> pathVars() { return pathVars; }
    @Override
    public Map<String, String> queryParams() { return queryParams; }
    @Override
    public @Nullable Throwable error() { return error; }
    @Override
    public @Nullable CloseCode getCloseCode() { return closeCode; }
    @Override
    public @Nullable MessagePayload getMessagePayload() { return messagePayload; }

}
