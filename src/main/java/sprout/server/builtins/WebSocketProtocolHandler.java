package sprout.server.builtins;

import sprout.beans.annotation.Component;
import sprout.mvc.http.HttpRequest;
import sprout.mvc.http.parser.HttpRequestParser;
import sprout.server.AcceptableProtocolHandler;
import sprout.server.ProtocolHandler;
import sprout.server.ReadableProtocolHandler;
import sprout.server.argument.WebSocketArgumentResolver;
import sprout.server.websocket.*;
import sprout.server.websocket.endpoint.WebSocketEndpointInfo;
import sprout.server.websocket.endpoint.WebSocketEndpointRegistry;
import sprout.server.websocket.framehandler.FrameHandler;
import sprout.server.websocket.handler.WebSocketHandshakeHandler;
import sprout.server.websocket.message.WebSocketMessageDispatcher;
import sprout.server.websocket.message.WebSocketMessageParser;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class WebSocketProtocolHandler implements AcceptableProtocolHandler {

    private final WebSocketHandshakeHandler handshakeHandler;
    private final WebSocketContainer webSocketContainer;
    private final WebSocketEndpointRegistry endpointRegistry;
    private final HttpRequestParser httpRequestParser;
    private final WebSocketFrameParser frameParser;
    private final WebSocketFrameEncoder frameEncoder;
    private final List<WebSocketArgumentResolver> webSocketArgumentResolvers;
    private final List<WebSocketMessageDispatcher> messageDispatchers;
    private final CloseListener closeListener;
    private final List<FrameHandler> frameHandlers;

    public WebSocketProtocolHandler(
            WebSocketHandshakeHandler handshakeHandler,
            WebSocketContainer webSocketContainer,
            WebSocketEndpointRegistry endpointRegistry,
            HttpRequestParser httpRequestParser,
            WebSocketFrameParser frameParser,
            WebSocketFrameEncoder frameEncoder,
            List<WebSocketArgumentResolver> webSocketArgumentResolvers,
            List<WebSocketMessageDispatcher> messageDispatchers,
            CloseListener closeListener,
            List<FrameHandler> frameHandlers
    ) {
        this.handshakeHandler = handshakeHandler;
        this.webSocketContainer = webSocketContainer;
        this.endpointRegistry = endpointRegistry;
        this.httpRequestParser = httpRequestParser;
        this.frameParser = frameParser;
        this.frameEncoder = frameEncoder;
        this.webSocketArgumentResolvers = webSocketArgumentResolvers;
        this.messageDispatchers = messageDispatchers;
        this.closeListener = closeListener;
        this.frameHandlers = frameHandlers;
    }

    @Override
    public boolean supports(String protocol) {
        return "WEBSOCKET".equals(protocol);
    }

    @Override
    public void accept(SocketChannel channel, Selector selector, ByteBuffer byteBuffer) throws Exception {
        // 1. 초기 HTTP 요청 파싱 (웹소켓 핸드셰이크 요청)
        // ByteBuffer를 사용하여 NIO non-blocking 방식으로 읽기
        String rawHttpRequest = readRawHttpRequestContent(channel, byteBuffer);
        HttpRequest<?> request = httpRequestParser.parse(rawHttpRequest);
        if (!request.isValid()) {
            System.out.println("Empty or invalid HTTP request for websocket handshake. Closing socket.");
            channel.close();
            return;
        }

        // 2. 웹소켓 엔드포인트 찾기
        String requestPath = request.getPath();
        System.out.println("WebSocket handshake request received for path: " + requestPath + ". Trying to find matching endpoint.");
        WebSocketEndpointInfo endpointInfo = endpointRegistry.getEndpointInfo(requestPath);

        if (endpointInfo == null) {
            sendHttpResponse(channel, 404, "Not Found", "No WebSocket endpoint found for " + requestPath);
            channel.close();
            return;
        }

        // 3. 핸드셰이크 수행
        boolean handshakeSuccess = handshakeHandler.performHandshake(request, channel);
        if (!handshakeSuccess) {
            System.out.println("WebSocket handshake failed. Closing socket.");
            channel.close();
            return;
        }

        // 4. 핸드셰이크 성공 후 WebSocketSession 초기화 및 등록
        String sessionId = UUID.randomUUID().toString();
        Map<String, String> pathVars = endpointInfo.getPathPattern().extractPathVariables(request.getPath());

        // DefaultWebSocketSession 생성 시 argumentResolvers와 messageParser 전달
        WebSocketSession wsSession = new DefaultWebSocketSession(sessionId, channel, selector, request, endpointInfo, frameParser, frameEncoder, pathVars, webSocketArgumentResolvers, messageDispatchers, closeListener, frameHandlers);
        webSocketContainer.addSession(endpointInfo.getPathPattern().getOriginalPattern(), wsSession);

        SelectionKey key = channel.register(selector, SelectionKey.OP_READ);
        key.attach(wsSession);
        wsSession.callOnOpenMethod();
    }

    private String readRawHttpRequestContent(SocketChannel channel, ByteBuffer buffer) throws IOException {
        StringBuilder sb = new StringBuilder();

        // 1) 이미 읽은 buffer의 데이터를 먼저 추가
        if (buffer != null && buffer.hasRemaining()) {
            byte[] arr = new byte[buffer.remaining()];
            buffer.get(arr);
            sb.append(new String(arr, StandardCharsets.UTF_8));
        }

        // 2) 이미 완전한 HTTP 요청인지 확인
        String current = sb.toString();
        if (current.contains("\r\n\r\n")) {
            return current;
        }

        // 3) 불완전한 경우, 추가로 읽기 (blocking 모드로 전환)
        boolean wasBlocking = channel.isBlocking();
        try {
            channel.configureBlocking(true);

            ByteBuffer readBuffer = ByteBuffer.allocate(8192);

            // HTTP 헤더 끝(\r\n\r\n)까지 읽기
            while (!sb.toString().contains("\r\n\r\n")) {
                readBuffer.clear();
                int bytesRead = channel.read(readBuffer);

                if (bytesRead == -1) {
                    return ""; // 연결 종료
                }

                if (bytesRead == 0) {
                    break;
                }

                readBuffer.flip();
                byte[] bytes = new byte[readBuffer.remaining()];
                readBuffer.get(bytes);
                sb.append(new String(bytes, StandardCharsets.UTF_8));

                // 너무 큰 요청은 거부 (10KB 제한)
                if (sb.length() > 10240) {
                    throw new IOException("HTTP request too large");
                }
            }

            return sb.toString();

        } finally {
            // 원래 blocking 모드로 복원
            if (!wasBlocking) {
                channel.configureBlocking(false);
            }
        }
    }

    /**
     * NIO 방식으로 HTTP 응답 전송
     */
    private void sendHttpResponse(SocketChannel channel, int statusCode, String statusText, String message) throws IOException {
        String response = "HTTP/1.1 " + statusCode + " " + statusText + "\r\n" +
                         "Content-Type: text/plain;charset=UTF-8\r\n" +
                         "Content-Length: " + message.getBytes(StandardCharsets.UTF_8).length + "\r\n" +
                         "\r\n" +
                         message;

        ByteBuffer buffer = ByteBuffer.wrap(response.getBytes(StandardCharsets.UTF_8));
        while (buffer.hasRemaining()) {
            channel.write(buffer);
        }
    }
}
