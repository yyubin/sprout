package sprout.server.websocket;

import sprout.mvc.http.HttpRequest;
import sprout.server.websocket.message.MessagePayload;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.util.Map;

public class DefaultInvocationContext implements InvocationContext{
    private final LifecyclePhase phase;
    private final HttpRequest<?> handshakeRequest;
    private final WebSocketSession session;
    private final Map<String, String> pathVars;
    private final Map<String, String> queryParams;
    private final MessagePayload messagePayload;
    private final InputStream inputStream;
    private final boolean isFin;
    private final Throwable error;
    private final CloseCode closeCode;
    private final WebSocketFrame frame;


    public DefaultInvocationContext(LifecyclePhase phase, HttpRequest<?> handshakeRequest, WebSocketSession session, Map<String, String> pathVars, Map<String, String> queryParams, MessagePayload messagePayload, InputStream inputStream, boolean isFin, Throwable error, CloseCode closeCode, WebSocketFrame frame) {
        this.phase = phase;
        this.handshakeRequest = handshakeRequest;
        this.session = session;
        this.pathVars = pathVars;
        this.queryParams = queryParams;
        this.messagePayload = messagePayload;
        this.inputStream = inputStream;
        this.isFin = isFin;
        this.error = error;
        this.closeCode = closeCode;
        this.frame = frame;
    }

    // @OnOpen용 생성자
    public DefaultInvocationContext(HttpRequest<?> handshakeRequest, WebSocketSession session, Map<String, String> pathVars) {
        this(LifecyclePhase.OPEN, handshakeRequest, session, pathVars,
                handshakeRequest.getQueryParams(), null, null, false, null, null, null);
    }

    // @MessageMapping용 생성자
    public DefaultInvocationContext(WebSocketSession session, Map<String, String> pathVars, MessagePayload payload, WebSocketFrame webSocketFrame) {
        this(LifecyclePhase.MESSAGE, session.getHandshakeRequest(), session, pathVars,
                session.getHandshakeRequest().getQueryParams(), payload, webSocketFrame.getPayloadStream(), webSocketFrame.isFin(), null, null, webSocketFrame);
    }

    // @OnError용 생성자
    public DefaultInvocationContext(WebSocketSession session, Map<String, String> pathVars, Throwable error) {
        this(LifecyclePhase.ERROR, session.getHandshakeRequest(), session, pathVars,
                session.getHandshakeRequest().getQueryParams(), null, null, false, error, null, null);
    }

    // @OnClose용 생성자
    public DefaultInvocationContext(WebSocketSession session, Map<String, String> pathVars, CloseCode closeCode) {
        this(LifecyclePhase.CLOSE, session.getHandshakeRequest(), session, pathVars,
                session.getHandshakeRequest().getQueryParams(), null, null, false, null, closeCode, null);
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
    public boolean isFin() {
        return isFin;
    }

    @Override
    public @Nullable Throwable error() { return error; }
    @Override
    public @Nullable CloseCode getCloseCode() { return closeCode; }
    @Override
    public @Nullable MessagePayload getMessagePayload() { return messagePayload; }
    @Override
    public @Nullable InputStream getInputStream() { return inputStream; }

    @Override
    public @Nullable WebSocketFrame getFrame() { return frame; }

}
