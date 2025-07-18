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
            // TODO: CloseCode와 reason을 포함한 Close 프레임 전송
            System.out.println("Closing WebSocket session: " + id);
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
        // TODO: frameEncoder.encodeBinary(data) 구현 후 사용
        throw new UnsupportedOperationException("Binary message encoding not yet implemented.");
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
                        callOnCloseMethod(CloseCodes.getCloseCode(1000)); // @OnClose 호출
                        break; // 루프 종료
                    } else if (WebSocketFrameDecoder.isPingFrame(frame)) {
                        System.out.println("Received Ping frame from client " + id);
                        sendPong(frame.getPayloadBytes());
                    } else if (WebSocketFrameDecoder.isPongFrame(frame)) {
                        System.out.println("Received Pong frame from client " + id);
                        // Pong 수신 처리 (주로 Keep-alive 확인용, 별도 로직 불필요)
                    } else if (WebSocketFrameDecoder.isDataFrame(frame)) {
                        // 텍스트 또는 바이너리 데이터 프레임 처리
                        dispatchMessage(frame);
                    } else {
                        System.err.println("Unknown or unsupported WebSocket opcode: " + frame.getOpcode());
                        // 프로토콜 오류 처리 (연결 종료)
                        callOnErrorMethod(new WebSocketException("Unknown WebSocket opcode: " + frame.getOpcode()));
                        break; // 루프 종료
                    }

                    // TODO: NIO로 전환 시 Thread.sleep(100) 제거
                    Thread.sleep(100); // BIO 환경에서 CPU 과부하 방지 (지속적인 I/O가 없을 때 과도한 CPU 사용 방지)
                }
            } catch (IOException e) {
                if (isOpen()) { // 세션이 아직 열려있는데 IOException 발생 시
                    System.err.println("I/O error in WebSocket session " + id + ": " + e.getMessage());
                    try {
                        callOnErrorMethod(e); // @OnError 호출
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error in WebSocket session " + id + " message loop: " + e.getMessage());
                e.printStackTrace(); // 스택 트레이스 출력 (디버깅용)
                try {
                    callOnErrorMethod(e); // @OnError 호출
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            } finally {
                // 루프 종료 시 (정상 종료든 오류든) 소켓 닫기
                try {
                    // TODO: @OnClose 호출 시 클라이언트로부터 받은 CloseCode/reason 전달 (선택 사항)
                    // 지금은 DefaultWebSocketSession.close()가 인자 없이 클로즈 프레임만 보내므로,
                    // 여기서는 DefaultWebSocketSession.close()를 직접 호출합니다.
                    // onclose 핸들러 호출은 반드시 이전에 이루어져야 합니다.
                    if (isOpen()) { // 아직 열려있는 경우에만 @OnClose 호출
                        callOnCloseMethod(CloseCodes.CLOSED_ABNORMALLY); // 비정상 종료 코드 전달
                    }
                } catch (Exception e) {
                    System.err.println("Error during @OnClose method call or final session close: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    // 최종 소켓 닫기는 WebSocketSession.close()가 책임집니다.
                    // WebSocketSession.close()가 idempotent(멱등)해야 함 (여러 번 호출되어도 문제 없도록).
                    try {
                        close(); // 세션의 내부 상태를 '닫힘'으로 설정하고 소켓 닫기
                    } catch (IOException e) {
                        System.err.println("Error during final socket close for session " + id + ": " + e.getMessage());
                    }
                }
            }
        });
        messageThread.setName("WebSocket-MessageLoop-" + id);
        messageThread.setDaemon(true); // 애플리케이션 종료 시 함께 종료되도록 데몬 스레드로 설정
        messageThread.start(); // 스레드 시작
    }

    public void dispatchMessage(WebSocketFrame frame) throws Exception {
        String textMessageContent = null;
        byte[] binaryMessageContent = null;

        // payloadInputStream은 이제 try-with-resources로 감싸지 않습니다.
        // 이는 frame.getPayloadStream()이 이미 LimitedInputStream이기 때문이며,
        // 이 스트림은 WebSocketSession의 lifecycle에 따라 나중에 닫힙니다.
        // Reader는 필요할 때마다 새롭게 생성하여 사용합니다.

        // 1. FIN 비트 및 프레임 타입에 따른 메시지 재조립
        if (frame.isFin()) {
            if (frame.getOpcode() == 0x0) { // 연속 프레임의 마지막 (FIN=true, Opcode=0x0)
                if (fragmentedTextMessageBuffer.length() > 0) { // 텍스트 연속
                    readAllCharsFromStream(frame.getPayloadStream(), fragmentedTextMessageBuffer); // 스트림에서 직접 읽기
                    textMessageContent = fragmentedTextMessageBuffer.toString();
                    fragmentedTextMessageBuffer = new StringBuilder(); // 버퍼 비우기
                } else if (fragmentedBinaryMessageBuffer.size() > 0) { // 바이너리 연속
                    readAllBytesFromStream(frame.getPayloadStream(), fragmentedBinaryMessageBuffer); // 스트림에서 직접 읽기
                    binaryMessageContent = fragmentedBinaryMessageBuffer.toByteArray();
                    fragmentedBinaryMessageBuffer.reset(); // 버퍼 비우기
                } else {
                    throw new WebSocketProtocolException("Protocol Error: Received continuation frame (0x0) with FIN=true, but no preceding fragmented message.");
                }
            } else if (frame.getOpcode() == 0x1) { // 단일 텍스트 프레임 (FIN=true, Opcode=0x1)
                if (!fragmentedTextMessageBuffer.isEmpty() || fragmentedBinaryMessageBuffer.size() > 0) {
                    throw new WebSocketProtocolException("Protocol Error: FIN=true and Opcode is not 0x0, but fragmented buffer is not empty.");
                }
                textMessageContent = readAllCharsFromStream(frame.getPayloadStream()); // 스트림에서 직접 읽기
            } else if (frame.getOpcode() == 0x2) { // 단일 바이너리 프레임
                if (!fragmentedTextMessageBuffer.isEmpty() || fragmentedBinaryMessageBuffer.size() > 0) {
                    throw new WebSocketProtocolException("Protocol Error: FIN=true and Opcode is not 0x0, but fragmented buffer is not empty.");
                }
                binaryMessageContent = readAllBytesFromStream(frame.getPayloadStream()); // 스트림에서 직접 읽기
            } else { // 알 수 없는 Opcode 또는 컨트롤 프레임의 FIN=true는 이미 상위에서 처리
                System.err.println("Unknown or unsupported WebSocket opcode in final frame: " + frame.getOpcode());
                throw new WebSocketProtocolException("Unknown WebSocket opcode in final frame: " + frame.getOpcode());
            }

            // --- 완전한 메시지가 재조립된 후, 메시지 디스패처 체인에게 위임 ---
            String completeTextPayload = textMessageContent; // 이미 완성됨
            byte[] completeBinaryPayload = binaryMessageContent; // 이미 완성됨

            // 버퍼를 비워 다음 메시지를 준비 (단일 프레임이어도 비워야 함)
            fragmentedTextMessageBuffer = new StringBuilder(); // 새로운 StringBuilder 인스턴스 생성
            fragmentedBinaryMessageBuffer.reset(); // reset만 하면 재사용 가능

            MessagePayload messagePayload = new DefaultMessagePayload(completeTextPayload, completeBinaryPayload);

            // InvocationContext 생성 시 messagePayload와 frame.getPayloadStream() (LimitedInputStream) 전달
            InvocationContext contextWithPayload = new DefaultInvocationContext(
                    this, pathParameters, messagePayload, frame // frame 자체도 전달
            );

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
                if (result == null || result.shouldCloseStream()) {
                    InputStream stream = contextWithPayload.getInputStream();
                    if (stream != null) {
                        try {
                            stream.close();
                        } catch (IOException e) {
                            System.err.println("Failed to close payload input stream: " + e.getMessage());
                        }
                    }
                }
            }

            if (result == null || !result.isHandled()) {
                System.err.println("No suitable WebSocketMessageDispatcher found for frame: " + frame.getOpcode() + " (FIN: " + frame.isFin() + ")");
                // TODO: 처리 못 한 메시지에 대한 에러 응답
            }
            // return; // 메시지 처리 후 반환 (원래 dispatchMessage가 void가 아니었다면...)
            // dispatchMessage는 void 이므로 그냥 return 하면 됩니다.

        } else { // 최종 프레임이 아닌 경우 (부분 메시지)
            // ... (기존 부분 메시지 버퍼링 로직 - readAllCharsFromStream, readAllBytesFromStream 사용) ...
            if (frame.getOpcode() == 0x1) { // 첫 텍스트 조각
                if (!fragmentedTextMessageBuffer.isEmpty() || fragmentedBinaryMessageBuffer.size() > 0) { throw new WebSocketProtocolException("Protocol Error: First fragmented frame must have Opcode 0x1 or 0x2, got " + frame.getOpcode() + " with non-empty buffer."); }
                readAllCharsFromStream(frame.getPayloadStream(), fragmentedTextMessageBuffer);
                System.out.println("Received first fragmented text frame. Buffering...");
                return;
            } else if (frame.getOpcode() == 0x2) { // 첫 바이너리 조각
                if (!fragmentedTextMessageBuffer.isEmpty() || fragmentedBinaryMessageBuffer.size() > 0) { throw new WebSocketProtocolException("Protocol Error: First fragmented frame must have Opcode 0x1 or 0x2, got " + frame.getOpcode() + " with non-empty buffer."); }
                readAllBytesFromStream(frame.getPayloadStream(), fragmentedBinaryMessageBuffer);
                System.out.println("Received first fragmented binary frame. Buffering...");
                return;
            } else if (frame.getOpcode() == 0x0) { // 연속 프레임 (텍스트 또는 바이너리)
                if (fragmentedTextMessageBuffer.length() > 0) { readAllCharsFromStream(frame.getPayloadStream(), fragmentedTextMessageBuffer); System.out.println("Received fragmented text continuation frame. Buffering..."); }
                else if (fragmentedBinaryMessageBuffer.size() > 0) { readAllBytesFromStream(frame.getPayloadStream(), fragmentedBinaryMessageBuffer); System.out.println("Received fragmented binary continuation frame. Buffering..."); }
                else { throw new WebSocketProtocolException("Protocol Error: Received continuation frame (0x0) with empty buffer."); }
                return;
            } else { // 컨트롤 프레임 (FIN=false) 또는 알 수 없는 Opcode
                throw new WebSocketProtocolException("Protocol Error: Control frame (opcode " + frame.getOpcode() + ") received with FIN=false.");
            }
        }

    }

    private String readAllCharsFromStream(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            char[] buf = new char[1024];
            int len;
            while ((len = reader.read(buf)) != -1) {
                sb.append(buf, 0, len);
            }
        } // reader가 여기서 닫히고, in(LimitedInputStream)은 닫히지 않습니다.
        return sb.toString();
    }

    private void readAllCharsFromStream(InputStream in, StringBuilder sb) throws IOException {
        try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            char[] buf = new char[1024];
            int len;
            while ((len = reader.read(buf)) != -1) {
                sb.append(buf, 0, len);
            }
        }
    }

    private void readAllBytesFromStream(InputStream in, ByteArrayOutputStream out) throws IOException {
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) != -1) {
            out.write(buf, 0, len);
        }
    }

    private byte[] readAllBytesFromStream(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
        } finally {
            // out은 여기서 닫지 않습니다. toByteArray()로 이미 내용이 복사됨.
        }
        return out.toByteArray();
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
