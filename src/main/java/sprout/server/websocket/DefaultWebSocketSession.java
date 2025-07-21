package sprout.server.websocket;

import sprout.mvc.http.HttpRequest;
import sprout.server.argument.WebSocketArgumentResolver;
import sprout.server.websocket.exception.NotEnoughDataException;
import sprout.server.websocket.exception.WebSocketException;
import sprout.server.websocket.endpoint.WebSocketEndpointInfo;
import sprout.server.websocket.exception.WebSocketProtocolException;
import sprout.server.websocket.message.*;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultWebSocketSession implements WebSocketSession{
    private final String id;
    private final SocketChannel channel;
    private final HttpRequest<?> handshakeRequest;
    private final Map<String, String> pathParameters;
    private final WebSocketEndpointInfo endpointInfo;
    private final WebSocketFrameParser frameParser;
    private final WebSocketFrameEncoder frameEncoder;
    private final List<WebSocketArgumentResolver> argumentResolvers;
    private final List<WebSocketMessageDispatcher> messageDispatchers;
    private final CloseListener closeListener;
    private volatile boolean open = true;
    private final Map<String, Object> userProperties = new ConcurrentHashMap<>();
    private StringBuilder fragmentedTextMessageBuffer = new StringBuilder();
    private ByteArrayOutputStream fragmentedBinaryMessageBuffer = new ByteArrayOutputStream();
    private final ByteBuffer readBuffer = ByteBuffer.allocate(65536);
    private int lastDataFrameOpcode = -1;

    public DefaultWebSocketSession(String id, SocketChannel channel, HttpRequest<?> handshakeRequest, WebSocketEndpointInfo endpointInfo, WebSocketFrameParser frameParser, WebSocketFrameEncoder frameEncoder, Map<String, String> pathParameters, List<WebSocketArgumentResolver> webSocketArgumentResolvers, List<WebSocketMessageDispatcher> messageDispatchers, CloseListener closeListener) throws IOException {
        this.id = id;
        this.channel = channel;
        this.handshakeRequest = handshakeRequest;
        this.endpointInfo = endpointInfo;
        this.frameParser = frameParser;
        this.frameEncoder = frameEncoder;
        this.pathParameters = pathParameters;
        this.argumentResolvers = webSocketArgumentResolvers;
        this.messageDispatchers = messageDispatchers;
        this.closeListener = closeListener;
    }

    @Override
    public void close() throws IOException {
        if (open) {
            System.out.println("Closing WebSocket session: " + id);
            byte[] encoded = frameEncoder.encodeText("Closing WebSocket session: " + id + ", Close code is: " + CloseCodes.getCloseCode(lastDataFrameOpcode).getCode() + ".");
            ByteBuffer buf = ByteBuffer.wrap(encoded);
            while (buf.hasRemaining()) {
                channel.write(buf);
            }
            open = false;
            channel.close();

            if (closeListener != null) {
                closeListener.onSessionClosed(this);
            }
        }
    }

    @Override
    public HttpRequest<?> getHandshakeRequest() {
        return handshakeRequest;
    }

    @Override
    public void sendText(String message) throws IOException {
        byte[] encoded = frameEncoder.encodeText(message);
        ByteBuffer buf = ByteBuffer.wrap(encoded);
        while (buf.hasRemaining()) {
            channel.write(buf); // non-blocking write
        }
    }

    @Override
    public void sendBinary(byte[] data) throws IOException {
        byte[] encoded = frameEncoder.encodeControlFrame(0x2, data);
        ByteBuffer buf = ByteBuffer.wrap(encoded);
        while (buf.hasRemaining()) {
            channel.write(buf);
        }
    }

    @Override
    public void sendPing(byte[] data) throws IOException {
        byte[] encoded = frameEncoder.encodeControlFrame(0x9, data);
        ByteBuffer buf = ByteBuffer.wrap(encoded);
        while (buf.hasRemaining()) {
            channel.write(buf);
        }
    }

    @Override
    public void sendPong(byte[] data) throws IOException {
        byte[] encoded = frameEncoder.encodeControlFrame(0xA, data);
        ByteBuffer buf = ByteBuffer.wrap(encoded);
        while (buf.hasRemaining()) {
            channel.write(buf);
        }
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean isOpen() {
        return open && channel.isOpen();
    }

    @Override
    public Map<String, Object> getUserProperties() {
        return userProperties;
    }

    @Override
    public void read(SelectionKey key) throws Exception {
        int bytesRead = channel.read(readBuffer);
        if (bytesRead == -1) {
            callOnCloseMethod(CloseCodes.NO_STATUS_CODE);
            close();
            return;
        }

        readBuffer.flip();

        while (readBuffer.remaining() > 0) {
            // 파싱 전에 현재 위치를 마크 (파싱 실패 시 복구 위함)
            readBuffer.mark();

            // ByteBuffer를 직접 읽는 InputStream 어댑터 사용
            InputStream frameInputStream = new ByteBufferInputStream(readBuffer);

            try {
                WebSocketFrame frame = frameParser.parse(frameInputStream);
                // 성공적으로 파싱되면, 실제 처리 로직 실행
                processFrame(frame);
            } catch (NotEnoughDataException e) {
                // 버퍼에 아직 완전한 프레임이 없음 -> 다음 read 이벤트를 기다림
                readBuffer.reset(); // 마크한 위치로 복구
                break; // while 루프 종료
            }
        }
        readBuffer.compact();
    }

    private void processFrame(WebSocketFrame frame) throws Exception {
        if (WebSocketFrameDecoder.isCloseFrame(frame)) {
            callOnCloseMethod(WebSocketFrameDecoder.getCloseCode(frame.getPayloadBytes()));
            return;
        } else if (WebSocketFrameDecoder.isPingFrame(frame)) {
            System.out.println("Received Ping frame from client " + id);
            sendPong(frame.getPayloadBytes());
        } else if (WebSocketFrameDecoder.isPongFrame(frame)) {
            System.out.println("Received Pong frame from client " + id);
        } else if (WebSocketFrameDecoder.isDataFrame(frame)) {
            dispatchMessage(frame);
        } else {
            System.err.println("Unknown or unsupported WebSocket opcode: " + frame.getOpcode());
            callOnErrorMethod(new WebSocketException("Unknown WebSocket opcode: " + frame.getOpcode()));
        }
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

}
