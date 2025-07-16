package sprout.server.websocket;

import sprout.mvc.http.HttpRequest;
import sprout.server.argument.WebSocketArgumentResolver;
import sprout.server.websocket.endpoint.WebSocketEndpointInfo;
import sprout.server.websocket.message.WebSocketMessageParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.Socket;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultWebSocketSession implements WebSocketSession{
    private final String id;
    private final Socket socket;
    private final HttpRequest<?> handshakeRequest;
    private final Map<String, String> pathParameters;
    private final WebSocketEndpointInfo endpointInfo;
    private final WebSocketFrameParser frameParser;
    private final WebSocketFrameEncoder frameEncoder;
    private final List<WebSocketArgumentResolver> argumentResolvers;
    private final WebSocketMessageParser messageParser;
    private final InputStream in;
    private final OutputStream out;
    private volatile boolean open = true;
    private final Map<String, Object> userProperties = new ConcurrentHashMap<>();

    public DefaultWebSocketSession(String id, Socket socket, HttpRequest<?> handshakeRequest, WebSocketEndpointInfo endpointInfo, WebSocketFrameParser frameParser, WebSocketFrameEncoder frameEncoder, Map<String, String> pathParameters, List<WebSocketArgumentResolver> webSocketArgumentResolvers, WebSocketMessageParser webSocketMessageParser) throws IOException {
        this.id = id;
        this.socket = socket;
        this.handshakeRequest = handshakeRequest;
        this.endpointInfo = endpointInfo;
        this.frameParser = frameParser;
        this.frameEncoder = frameEncoder;
        this.in = socket.getInputStream();
        this.out = socket.getOutputStream();
        this.pathParameters = pathParameters;
        this.argumentResolvers = webSocketArgumentResolvers;
        this.messageParser = webSocketMessageParser;
    }

    @Override
    public void close() throws IOException {
        if (open) {
            // TODO: CloseCode와 reason을 포함한 Close 프레임 전송
            System.out.println("Closing WebSocket session: " + id);
            open = false;
            socket.close();
        }
    }

    @Override
    public void sendText(String message) throws IOException {
        byte[] frame = frameEncoder.encodeText(message);
        out.write(frame);
        out.flush();
    }

    @Override
    public void sendBinary(byte[] data) throws IOException {
        // TODO: frameEncoder.encodeBinary(data) 구현 후 사용
        throw new UnsupportedOperationException("Binary message encoding not yet implemented.");
    }

    @Override
    public void sendPing(byte[] data) throws IOException {
        // TODO: frameEncoder.encodePing(data) 구현 후 사용
        throw new UnsupportedOperationException("Ping frame encoding not yet implemented.");
    }

    @Override
    public void sendPong(byte[] data) throws IOException {
        // TODO: frameEncoder.encodePong(data) 구현 후 사용
        throw new UnsupportedOperationException("Pong frame encoding not yet implemented.");
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean isOpen() {
        return open && !socket.isClosed();
    }

    @Override
    public Map<String, Object> getUserProperties() {
        return userProperties;
    }

    @Override
    public void startMessageLoop() throws Exception {
        while (isOpen()) {
            try {
                WebSocketFrame frame = frameParser.parse(in); // InputStream에서 프레임 파싱

                if (WebSocketFrameDecoder.isCloseFrame(frame)) {
                    System.out.println("Received Close frame from client " + id);
                    // 클라이언트가 보낸 Close 프레임 처리 (Pong으로 응답 후 종료)
                    // TODO: CloseCode 및 reason 파싱 후 @OnClose 호출
                    close(); // 세션 종료
                    break;
                } else if (WebSocketFrameDecoder.isPingFrame(frame)) {
                    System.out.println("Received Ping frame from client " + id);
                    sendPong(frame.getPayload()); // Ping에 Pong으로 응답
                } else if (WebSocketFrameDecoder.isPongFrame(frame)) {
                    System.out.println("Received Pong frame from client " + id);
                    // Pong 수신 처리 (주로 Keep-alive 확인용)
                } else if (WebSocketFrameDecoder.isDataFrame(frame)) {
                    // 텍스트 또는 바이너리 데이터 프레임 처리
                    // TODO: Fragmented messages (FIN=false) 처리 로직 추가 (현재는 단일 FIN=true 프레임만 가정)
                    dispatchMessage(frame);
                } else {
                    System.err.println("Unknown or unsupported WebSocket opcode: " + frame.getOpcode());
                    // TODO: 프로토콜 오류 처리 (연결 종료)
                    close();
                    break;
                }
            } catch (IOException e) {
                if (isOpen()) { // 세션이 아직 열려있는데 IOException 발생 시 (클라이언트 연결 끊김)
                    System.err.println("I/O error in WebSocket session " + id + ": " + e.getMessage());
                    throw e; // 상위로 예외 전파하여 @OnError 호출되도록
                }
                break; // 세션이 이미 닫혔다면 루프 종료
            } catch (Exception e) { // 프레임 파싱 오류 등
                System.err.println("Error parsing WebSocket frame for session " + id + ": " + e.getMessage());
                throw e; // 상위로 예외 전파하여 @OnError 호출되도록
            }
            // TODO: NIO로 전환 시 Thread.sleep(100) 제거
            // Thread.sleep(100); // BIO 환경에서 CPU 과부하 방지
        }
    }

    private void dispatchMessage(WebSocketFrame frame) throws Exception {
        String messagePath = messageParser.extractDestination(frame); // <-- 파서 사용
        String messagePayload = messageParser.extractPayload(frame); // <-- 파서 사용 (payload 추출)

        if (messagePath == null || messagePath.isBlank()) {
            System.err.println("WebSocket message has no destination path. Skipping dispatch.");
            // TODO: 클라이언트에게 오류 응답 전송 고려
            return;
        }

        Method messageMappingMethod = endpointInfo.getMessageMappingMethod(messagePath);

        if (messageMappingMethod != null) {
            Parameter[] parameters = messageMappingMethod.getParameters();
            Object[] args = new Object[parameters.length];

            for (int i = 0; i < parameters.length; i++) {
                boolean resolved = false;
                for (WebSocketArgumentResolver resolver : argumentResolvers) {
                    if (resolver.supports(parameters[i])) {
                        args[i] = resolver.resolve(parameters[i], this, messagePayload); // <-- payload 전달
                        resolved = true;
                        break;
                    }
                }
                if (!resolved) {
                    throw new IllegalArgumentException("No WebSocketArgumentResolver found for parameter: " + parameters[i].getName() + " in method " + messageMappingMethod.getName());
                }
            }

            try {
                messageMappingMethod.invoke(endpointInfo.getHandlerBean(), args);
            } catch (InvocationTargetException e) {
                // @MessageMapping 메서드 내부에서 발생한 실제 예외를 처리
                System.err.println("Exception in @MessageMapping method " + messageMappingMethod.getName() + ": " + e.getTargetException().getMessage());
                e.getTargetException().printStackTrace();
                // TODO: @OnError 호출 또는 적절한 에러 메시지 클라이언트에게 전송
                throw e; // 상위로 전파
            }
        } else {
            System.err.println("No @MessageMapping found for path: " + messagePath);
            // TODO: 매칭되는 핸들러가 없을 경우 에러 메시지 클라이언트에게 전송
            // (예: 클라이언트에게 404 Not Found에 해당하는 웹소켓 에러 프레임 전송)
        }
    }

    @Override
    public String getRequestPath() {
        return handshakeRequest.getPath();
    }

    @Override
    public Map<String, List<String>> getRequestParameterMap() {
        return handshakeRequest.getQueryParams().entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        e -> Collections.singletonList(e.getValue()) // String을 List<String>으로 변환
                ));
    }

    @Override
    public String getQueryString() {
        // HttpRequest에 원본 쿼리 스트링이 있다면 사용
        // 현재 HttpRequest는 Map<String, String>으로 쿼리 파라미터를 저장하므로,
        // 원본 쿼리 스트링을 다시 조합하거나, HttpRequest에 원본 쿼리 스트링 필드를 추가해야 합니다.
        return ""; // TODO: 실제 쿼리 스트링 반환 로직 구현
    }

    @Override
    public Map<String, String> getPathParameters() {
        return this.pathParameters;
    }

    // MessageHandler 관련 메서드는 JSR-356의 Session 인터페이스에 있으나,
    // 현재 Sprout는 @MessageMapping 기반이므로 직접 구현하지 않아도 될 수 있습니다.
    @Override
    public Set<MessageHandler> getMessageHandlers() {
        return null; // TODO: 필요 시 구현
    }

    @Override
    public void addMessageHandler(MessageHandler handler) {
        // TODO: 필요 시 구현
    }

    @Override
    public void removeMessageHandler(MessageHandler handler) {
        // TODO: 필요 시 구현
    }
}
