package sprout.server.websocket;

import sprout.mvc.http.HttpRequest;
import sprout.server.argument.WebSocketArgumentResolver;
import sprout.server.websocket.exception.WebSocketException;
import sprout.server.websocket.endpoint.WebSocketEndpointInfo;
import sprout.server.websocket.exception.WebSocketProtocolException;
import sprout.server.websocket.message.*;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
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
    private final List<WebSocketMessageDispatcher> messageDispatchers;
    private final InputStream in;
    private final OutputStream out;
    private volatile boolean open = true;
    private final Map<String, Object> userProperties = new ConcurrentHashMap<>();
    private StringBuilder fragmentedTextMessageBuffer = new StringBuilder();
    private ByteArrayOutputStream fragmentedBinaryMessageBuffer = new ByteArrayOutputStream();
    private int lastDataFrameOpcode = -1;

    public DefaultWebSocketSession(String id, Socket socket, HttpRequest<?> handshakeRequest, WebSocketEndpointInfo endpointInfo, WebSocketFrameParser frameParser, WebSocketFrameEncoder frameEncoder, Map<String, String> pathParameters, List<WebSocketArgumentResolver> webSocketArgumentResolvers, List<WebSocketMessageDispatcher> messageDispatchers) throws IOException {
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
        this.messageDispatchers = messageDispatchers;
    }

    @Override
    public void close() throws IOException {
        if (open) {
            System.out.println("Closing WebSocket session: " + id);
            out.write(CloseCodes.getCloseCode(lastDataFrameOpcode).getCode());
            out.flush();
            open = false;
            socket.close();
        }
    }

    @Override
    public HttpRequest<?> getHandshakeRequest() {
        return handshakeRequest;
    }

    @Override
    public void sendText(String message) throws IOException {
        byte[] frame = frameEncoder.encodeText(message);
        out.write(frame);
        out.flush();
    }

    @Override
    public void sendBinary(byte[] data) throws IOException {
        byte[] frame = frameEncoder.encodeControlFrame(0x2, data);
        out.write(frame);
        out.flush();
    }

    @Override
    public void sendPing(byte[] data) throws IOException {
        byte[] frame = frameEncoder.encodeControlFrame(0x9, data);
        out.write(frame);
        out.flush();
    }

    @Override
    public void sendPong(byte[] data) throws IOException {
        byte[] frame = frameEncoder.encodeControlFrame(0xA, data);
        out.write(frame);
        out.flush();
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
        Thread messageThread = new Thread(() -> {
            try {
                while (isOpen()) {
                    WebSocketFrame frame = frameParser.parse(in); // 이 부분에서 블로킹 발생 가능
                    if (WebSocketFrameDecoder.isCloseFrame(frame)) {
                        System.out.println("Received Close frame from client " + id);
                        callOnCloseMethod(WebSocketFrameDecoder.getCloseCode(frame.getPayloadBytes())); // getPayloadBytes() 호출
                        break; // 루프 종료
                    } else if (WebSocketFrameDecoder.isPingFrame(frame)) {
                        System.out.println("Received Ping frame from client " + id);
                        sendPong(frame.getPayloadBytes()); // getPayloadBytes() 호출
                    } else if (WebSocketFrameDecoder.isPongFrame(frame)) {
                        System.out.println("Received Pong frame from client " + id);
                        // Pong 수신 처리 (주로 Keep-alive 확인용, 별도 로직 불필요)
                    } else if (WebSocketFrameDecoder.isDataFrame(frame)) {
                        dispatchMessage(frame);
                    } else {
                        System.err.println("Unknown or unsupported WebSocket opcode: " + frame.getOpcode());
                        callOnErrorMethod(new WebSocketException("Unknown WebSocket opcode: " + frame.getOpcode()));
                        break;
                    }
                    Thread.sleep(100); // BIO 환경에서 CPU 과부하 방지 (TODO: NIO 전환 시 제거)
                }
            } catch (IOException e) {
                if (isOpen()) {
                    System.err.println("I/O error in WebSocket session " + id + ": " + e.getMessage());
                    try { callOnErrorMethod(e); } catch (Exception ex) { throw new RuntimeException(ex); }
                }
            } catch (Exception e) {
                System.err.println("Error in WebSocket session " + id + " message loop: " + e.getMessage());
                e.printStackTrace();
                try { callOnErrorMethod(e); } catch (Exception ex) { throw new RuntimeException(ex); }
            } finally {
                try {
                    if (isOpen()) { callOnCloseMethod(CloseCodes.CLOSED_ABNORMALLY); }
                } catch (Exception e) {
                    System.err.println("Error during @OnClose method call or final session close: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    try { close(); } catch (IOException e) { System.err.println("Error during final socket close for session " + id + ": " + e.getMessage()); }
                }
            }
        });
        messageThread.setName("WebSocket-MessageLoop-" + id);
        messageThread.setDaemon(true);
        messageThread.start();
    }

    public void dispatchMessage(WebSocketFrame frame) throws Exception {
        if (frame.isFin()) {
            if (frame.getOpcode() == 0x0) { // 연속 프레임의 마지막
                if (fragmentedTextMessageBuffer.length() > 0) {
                    // 마지막 텍스트 조각을 버퍼에 추가 (스트림은 닫지 않음)
                    readTextToBuffer(frame.getPayloadStream(), fragmentedTextMessageBuffer);
                } else if (fragmentedBinaryMessageBuffer.size() > 0) {
                    // 마지막 바이너리 조각을 버퍼에 추가 (스트림은 닫지 않음)
                    readBinaryToBuffer(frame.getPayloadStream(), fragmentedBinaryMessageBuffer);
                } else {
                    throw new WebSocketProtocolException("Protocol Error: Continuation frame (0x0) with FIN=true, but no preceding fragmented message.");
                }

            } else if (frame.getOpcode() == 0x1) { // 단일 텍스트 프레임
                if (!fragmentedTextMessageBuffer.isEmpty() || fragmentedBinaryMessageBuffer.size() > 0) { throw new WebSocketProtocolException("Protocol Error: FIN=true and Opcode is not 0x0, but fragmented buffer is not empty."); }
                // 전체 텍스트 메시지를 버퍼에 추가 (스트림은 닫지 않음)
                readTextToBuffer(frame.getPayloadStream(), fragmentedTextMessageBuffer);
            } else if (frame.getOpcode() == 0x2) { // 단일 바이너리 프레임
                if (!fragmentedTextMessageBuffer.isEmpty() || fragmentedBinaryMessageBuffer.size() > 0) { throw new WebSocketProtocolException("Protocol Error: FIN=true and Opcode is not 0x0, but fragmented buffer is not empty."); }
                // 전체 바이너리 메시지를 버퍼에 추가 (스트림은 닫지 않음)
                readBinaryToBuffer(frame.getPayloadStream(), fragmentedBinaryMessageBuffer);
            } else {
                throw new WebSocketProtocolException("Unknown WebSocket opcode in final frame: " + frame.getOpcode());
            }

            String completeTextPayload = (fragmentedTextMessageBuffer.length() > 0) ? fragmentedTextMessageBuffer.toString() : null;
            byte[] completeBinaryPayload = (fragmentedBinaryMessageBuffer.size() > 0) ? fragmentedBinaryMessageBuffer.toByteArray() : null;
            MessagePayload messagePayload = new DefaultMessagePayload(completeTextPayload, completeBinaryPayload);
            InvocationContext contextWithPayload = new DefaultInvocationContext(this, pathParameters, messagePayload, frame);
            DispatchResult result = null;
            try {
                for (WebSocketMessageDispatcher dispatcher : messageDispatchers) {
                    if (dispatcher.supports(frame, contextWithPayload)) {
                        result = dispatcher.dispatch(frame, contextWithPayload);
                        if (result.isHandled()) {
                            break;
                        }
                    }
                }
            } finally {
                // InputStream 받아간 경우, 핸들러가 닫아야함. 아닌 경우엔 직접 닫아주기
                if (result == null || result.shouldCloseStream()) {
                    InputStream stream = contextWithPayload.getFrame().getPayloadStream();
                    if (stream != null) {
                        try { stream.close(); } catch (IOException e) { System.err.println("Failed to close payload input stream: " + e.getMessage()); }
                    }
                }
            }
            fragmentedTextMessageBuffer = new StringBuilder();
            fragmentedBinaryMessageBuffer.reset();
            if (result == null || !result.isHandled()) {
                System.err.println("No suitable WebSocketMessageDispatcher found for frame: " + frame.getOpcode() + " (FIN: " + frame.isFin() + ")");
            }

        } else { // 최종 프레임이 아닌 경우 (부분 메시지)
            if (frame.getOpcode() == 0x1) { // 첫 텍스트 조각
                if (!fragmentedTextMessageBuffer.isEmpty() || fragmentedBinaryMessageBuffer.size() > 0) { throw new WebSocketProtocolException("Protocol Error: First fragmented frame must have Opcode 0x1 or 0x2, got " + frame.getOpcode() + " with non-empty buffer."); }
                // 텍스트 조각을 버퍼에 추가하고 해당 프레임 스트림을 소비(닫음)
                consumeTextFragment(frame.getPayloadStream(), fragmentedTextMessageBuffer);
                lastDataFrameOpcode = frame.getOpcode();
                System.out.println("Received first fragmented text frame. Buffering...");
                return;
            } else if (frame.getOpcode() == 0x2) { // 첫 바이너리 조각
                if (!fragmentedTextMessageBuffer.isEmpty() || fragmentedBinaryMessageBuffer.size() > 0) { throw new WebSocketProtocolException("Protocol Error: First fragmented frame must have Opcode 0x1 or 0x2, got " + frame.getOpcode() + " with non-empty buffer."); }
                // 바이너리 조각을 버퍼에 추가하고 해당 프레임 스트림을 소비(닫음)
                consumeBinaryFragment(frame.getPayloadStream(), fragmentedBinaryMessageBuffer);
                lastDataFrameOpcode = frame.getOpcode();
                System.out.println("Received first fragmented binary frame. Buffering...");
                return;
            } else if (frame.getOpcode() == 0x0) { // 연속 프레임
                if (fragmentedTextMessageBuffer.length() > 0) {
                    consumeTextFragment(frame.getPayloadStream(), fragmentedTextMessageBuffer);
                    System.out.println("Received fragmented text continuation frame. Buffering...");
                } else if (fragmentedBinaryMessageBuffer.size() > 0) {
                    consumeBinaryFragment(frame.getPayloadStream(), fragmentedBinaryMessageBuffer);
                    System.out.println("Received fragmented binary continuation frame. Buffering...");
                } else {
                    throw new WebSocketProtocolException("Protocol Error: Received continuation frame (0x0) with empty buffer.");
                }
                return;
            } else {
                throw new WebSocketProtocolException("Protocol Error: Control frame (opcode " + frame.getOpcode() + ") received with FIN=false.");
            }
        }
    }

    /**
     * 스트림의 모든 텍스트 데이터를 읽어 버퍼에 추가합니다.
     * 이 메서드는 스트림을 닫지 않습니다.
     * @param in 입력 스트림
     * @param sb 텍스트를 추가할 StringBuilder
     */
    private void readTextToBuffer(InputStream in, StringBuilder sb) throws IOException {
        InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
        char[] buf = new char[1024];
        int len;
        while ((len = reader.read(buf)) != -1) {
            sb.append(buf, 0, len);
        }
    }

    /**
     * 스트림의 모든 바이트 데이터를 읽어 버퍼에 추가합니다.
     * 이 메서드는 스트림을 닫지 않습니다.
     * @param in 입력 스트림
     * @param out 바이트를 추가할 ByteArrayOutputStream
     */
    private void readBinaryToBuffer(InputStream in, ByteArrayOutputStream out) throws IOException {
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) != -1) {
            out.write(buf, 0, len);
        }
    }

    /**
     * 텍스트 프래그먼트 스트림을 완전히 소비(읽고 닫기)하여 버퍼에 추가합니다.
     * @param in 입력 스트림 (메서드 종료 시 닫힙니다)
     * @param sb 텍스트를 추가할 StringBuilder
     */
    private void consumeTextFragment(InputStream in, StringBuilder sb) throws IOException {
        try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            char[] buf = new char[1024];
            int len;
            while ((len = reader.read(buf)) != -1) {
                sb.append(buf, 0, len);
            }
        }
    }

    /**
     * 바이너리 프래그먼트 스트림을 완전히 소비(읽고 닫기)하여 버퍼에 추가합니다.
     * @param in 입력 스트림 (메서드 종료 시 닫힙니다)
     * @param out 바이트를 추가할 ByteArrayOutputStream
     */
    private void consumeBinaryFragment(InputStream in, ByteArrayOutputStream out) throws IOException {
        try (InputStream fragmentStream = in) {
            byte[] buf = new byte[1024];
            int len;
            while ((len = fragmentStream.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
        }
    }

    @Override
    public WebSocketEndpointInfo getEndpointInfo() {
        return endpointInfo;
    }

    @Override
    public InputStream getInputStream() {
        return in;
    }

    @Override
    public void callOnOpenMethod() throws Exception{
        Method onOpenMethod = endpointInfo.getOnOpenMethod();
        if (onOpenMethod == null) return;

        // InvocationContext 생성
        InvocationContext context = new DefaultInvocationContext(handshakeRequest, this, pathParameters);

        Object[] args = resolveArgs(onOpenMethod, context);
        onOpenMethod.invoke(endpointInfo.getHandlerBean(), args);
    }

    @Override
    public void callOnErrorMethod(Throwable error) throws Exception {
        Method onErrorMethod = endpointInfo.getOnErrorMethod();
        if (onErrorMethod == null) return;

        // InvocationContext 생성
        InvocationContext context = new DefaultInvocationContext(this, pathParameters, error);

        Object[] args = resolveArgs(onErrorMethod, context);
        onErrorMethod.invoke(endpointInfo.getHandlerBean(), args);
    }

    @Override
    public void callOnCloseMethod(CloseCode closeCode) throws Exception {
        Method onCloseMethod = endpointInfo.getOnCloseMethod();
        if (onCloseMethod == null) return;

        // InvocationContext 생성
        InvocationContext context = new DefaultInvocationContext(this, pathParameters, closeCode);

        Object[] args = resolveArgs(onCloseMethod, context);
        onCloseMethod.invoke(endpointInfo.getHandlerBean(), args);
    }

    private Object[] resolveArgs(Method method, InvocationContext context) throws Exception {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            boolean resolved = false;
            for (WebSocketArgumentResolver resolver : argumentResolvers) {
                if (resolver.supports(parameters[i], context)) { // <- InvocationContext 전달
                    args[i] = resolver.resolve(parameters[i], context); // <- InvocationContext 전달
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
        return handshakeRequest.getQueryParams().entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(java.util.stream.Collectors.joining("&"));
    }

    @Override
    public Map<String, String> getPathParameters() {
        return this.pathParameters;
    }

    // MessageHandler 관련 메서드는 JSR-356의 Session 인터페이스에 있으나,
    // 현재 Sprout는 @MessageMapping 기반이므로 직접 구현하지 않아도 될 수 있음
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
