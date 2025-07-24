package sprout.server.websocket;

import sprout.mvc.http.HttpRequest;
import sprout.server.WritableHandler;
import sprout.server.argument.WebSocketArgumentResolver;
import sprout.server.websocket.exception.NotEnoughDataException;
import sprout.server.websocket.exception.WebSocketException;
import sprout.server.websocket.endpoint.WebSocketEndpointInfo;
import sprout.server.websocket.framehandler.FrameHandler;
import sprout.server.websocket.framehandler.FrameProcessingContext;
import sprout.server.websocket.message.*;

import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import static java.nio.channels.SelectionKey.OP_WRITE;

public class DefaultWebSocketSession implements WebSocketSession, WritableHandler {
    private final String id;
    private final SocketChannel channel;
    private final Selector selector;
    private final HttpRequest<?> handshakeRequest;
    private final Map<String, String> pathParameters;
    private final WebSocketEndpointInfo endpointInfo;
    private final WebSocketFrameParser frameParser;
    private final WebSocketFrameEncoder frameEncoder;
    private final List<WebSocketArgumentResolver> argumentResolvers;
    private final List<WebSocketMessageDispatcher> messageDispatchers;
    private final CloseListener closeListener;

    private final FrameProcessingContext processingContext;
    private volatile boolean open = true;
    private volatile boolean isClosePending = false;
    private final Map<String, Object> userProperties = new ConcurrentHashMap<>();
    private final ByteBuffer readBuffer = ByteBuffer.allocate(65536);
    private final Queue<ByteBuffer> pendingWrites = new ConcurrentLinkedQueue<>();

    private final WebSocketFrameDispatcher frameDispatcher;

    public DefaultWebSocketSession(String id, SocketChannel channel, Selector selector, HttpRequest<?> handshakeRequest, WebSocketEndpointInfo endpointInfo, WebSocketFrameParser frameParser, WebSocketFrameEncoder frameEncoder, Map<String, String> pathParameters, List<WebSocketArgumentResolver> webSocketArgumentResolvers, List<WebSocketMessageDispatcher> messageDispatchers, CloseListener closeListener, List<FrameHandler> frameHandlers) throws IOException {
        this.id = id;
        this.channel = channel;
        this.selector = selector;
        this.handshakeRequest = handshakeRequest;
        this.endpointInfo = endpointInfo;
        this.frameParser = frameParser;
        this.frameEncoder = frameEncoder;
        this.pathParameters = pathParameters;
        this.argumentResolvers = webSocketArgumentResolvers;
        this.messageDispatchers = messageDispatchers;
        this.closeListener = closeListener;
        this.frameDispatcher = new WebSocketFrameDispatcher(frameHandlers, messageDispatchers);
        this.processingContext = new FrameProcessingContext();
    }

    @Override
    public void close() throws IOException {
        if (open && !isClosePending) {
            System.out.println("Scheduling close for WebSocket session: " + id);
            isClosePending = true; // 종료 요청 표시
            // 종료 프레임 생성 (opcode 0x8, 정상 종료 코드 1000)
            String closeReason = "Closing WebSocket session: " + id + ", Close code is: " + CloseCodes.NORMAL_CLOSURE.getCode() + ".";
            byte[] closePayload = closeReason.getBytes(StandardCharsets.UTF_8);
            byte[] encoded = frameEncoder.encodeControlFrame(0x8, closePayload);
            scheduleWrite(ByteBuffer.wrap(encoded));
        }
    }

    @Override
    public HttpRequest<?> getHandshakeRequest() {
        return handshakeRequest;
    }

    @Override
    public void sendText(String message) throws IOException {
        scheduleWrite(ByteBuffer.wrap(frameEncoder.encodeText(message)));
    }

    @Override
    public void write(SelectionKey key) throws Exception {
        ByteBuffer buf;
        while ((buf = pendingWrites.peek()) != null) {
            channel.write(buf);
            if (buf.hasRemaining()) return;
            pendingWrites.poll();
        }

        if (pendingWrites.isEmpty()) {
            key.interestOps(key.interestOps() & ~OP_WRITE);
            // 큐가 비었고 종료 요청이 있었다면 채널 닫기
            if (isClosePending && open) {
                System.out.println("All pending writes completed, closing channel for session: " + id);
                open = false;
                channel.close();
                if (closeListener != null) {
                    closeListener.onSessionClosed(this);
                }
            }
        }
    }

    @Override
    public void sendBinary(byte[] data) throws IOException {
        scheduleWrite(ByteBuffer.wrap(frameEncoder.encodeBinary(data)));
    }

    @Override
    public void sendPing(byte[] data) throws IOException {
        scheduleWrite(ByteBuffer.wrap(frameEncoder.encodeControlFrame(0x9, data)));
    }

    @Override
    public void sendPong(byte[] data) throws IOException {
        scheduleWrite(ByteBuffer.wrap(frameEncoder.encodeControlFrame(0xA, data)));
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

    private void scheduleWrite(ByteBuffer buf) {
        pendingWrites.add(buf);
        SelectionKey key = channel.keyFor(selector);
        if (key != null && key.isValid() && (key.interestOps() & OP_WRITE) == 0) {
            // | 연산자로 OP_WRITE 플래그를 추가
            key.interestOps(key.interestOps() | OP_WRITE);
            selector.wakeup(); // Selector가 select()에서 대기 중일 수 있으므로 깨워주기
        }
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
        this.processingContext.setCurrentFrame(frame);
        try {
            frameDispatcher.dispatch(this.processingContext, this, pathParameters);
        } catch (Exception e) {
            System.err.println("Error dispatching frame: " + e.getMessage());
            callOnErrorMethod(e); // 에러 핸들러 호출
            close(); // 치명적 오류 시 연결 종료
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

    public boolean isClosePending() {
        return isClosePending;
    }
}
