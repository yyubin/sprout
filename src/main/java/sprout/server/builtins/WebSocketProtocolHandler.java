package sprout.server.builtins;

import sprout.beans.annotation.Component;
import sprout.mvc.http.HttpRequest;
import sprout.mvc.http.parser.HttpRequestParser;
import sprout.server.ProtocolHandler;
import sprout.server.argument.WebSocketArgumentResolver;
import sprout.server.websocket.*;
import sprout.server.websocket.endpoint.WebSocketEndpointInfo;
import sprout.server.websocket.endpoint.WebSocketEndpointRegistry;
import sprout.server.websocket.handler.WebSocketHandshakeHandler;
import sprout.server.websocket.message.WebSocketMessageParser;

import java.io.*;
import java.lang.reflect.Method;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class WebSocketProtocolHandler implements ProtocolHandler {

    private final WebSocketHandshakeHandler handshakeHandler;
    private final WebSocketContainer webSocketContainer;
    private final WebSocketEndpointRegistry endpointRegistry;
    private final HttpRequestParser httpRequestParser;
    private final WebSocketFrameParser frameParser;
    private final WebSocketFrameEncoder frameEncoder;
    private final List<WebSocketArgumentResolver> webSocketArgumentResolvers;
    private final WebSocketMessageParser webSocketMessageParser;

    public WebSocketProtocolHandler(
            WebSocketHandshakeHandler handshakeHandler,
            WebSocketContainer webSocketContainer,
            WebSocketEndpointRegistry endpointRegistry,
            HttpRequestParser httpRequestParser,
            WebSocketFrameParser frameParser,
            WebSocketFrameEncoder frameEncoder,
            List<WebSocketArgumentResolver> webSocketArgumentResolvers,
            WebSocketMessageParser webSocketMessageParser
    ) {
        this.handshakeHandler = handshakeHandler;
        this.webSocketContainer = webSocketContainer;
        this.endpointRegistry = endpointRegistry;
        this.httpRequestParser = httpRequestParser;
        this.frameParser = frameParser;
        this.frameEncoder = frameEncoder;
        this.webSocketArgumentResolvers = webSocketArgumentResolvers;
        this.webSocketMessageParser = webSocketMessageParser;
    }

    @Override
    public void handle(Socket socket) throws Exception {
        // HTTP Upgrade 요청을 처리하기 위해 BufferedReader/BufferedWriter 사용
        // 웹소켓 메시지 통신을 위해 InputStream/OutputStream을 직접 사용
        try (InputStream in = socket.getInputStream(); // 직접 InputStream 사용
             OutputStream out = socket.getOutputStream();
             BufferedReader httpReader = new BufferedReader(new InputStreamReader(in)); // 초기 HTTP 파싱용
             BufferedWriter httpWriter = new BufferedWriter(new OutputStreamWriter(out))) { // 초기 HTTP 응답용

            // 1. 초기 HTTP 요청 파싱 (웹소켓 핸드셰이크 요청)
            String rawHttpRequest = readRawHttpRequest(httpReader); // httpReader 사용
            if (rawHttpRequest.isBlank()) {
                System.out.println("Empty raw HTTP request for websocket handshake. Closing socket.");
                socket.close();
                return;
            }
            HttpRequest<?> request = httpRequestParser.parse(rawHttpRequest);

            // 2. 웹소켓 엔드포인트 찾기
            String requestPath = request.getPath();
            WebSocketEndpointInfo endpointInfo = endpointRegistry.getEndpointInfo(requestPath);

            if (endpointInfo == null) {
                sendHttpResponse(httpWriter, 404, "Not Found", "No WebSocket endpoint found for " + requestPath);
                socket.close();
                return;
            }

            // 3. 핸드셰이크 수행
            boolean handshakeSuccess = handshakeHandler.performHandshake(request, httpWriter); // httpWriter 사용

            if (!handshakeSuccess) {
                System.out.println("WebSocket handshake failed. Closing socket.");
                socket.close();
                return;
            }

            // 4. 핸드셰이크 성공 후 WebSocketSession 초기화 및 등록
            String sessionId = UUID.randomUUID().toString();
            Map<String, String> pathVars = endpointInfo.getPathPattern().extractPathVariables(request.getPath());
            // DefaultWebSocketSession 구현체에 파서와 인코더도 넘겨주어 메시지 송수신을 담당하게 합니다.
            WebSocketSession wsSession = new DefaultWebSocketSession(sessionId, socket, request, endpointInfo, frameParser, frameEncoder, pathVars, webSocketArgumentResolvers, webSocketMessageParser);

            webSocketContainer.addSession(endpointInfo.getPathPattern().getOriginalPattern(), wsSession);

            // 5. @OnOpen 메서드 호출
            Method onOpenMethod = endpointInfo.getOnOpenMethod();
            if (onOpenMethod != null) {
                try {
                    if (onOpenMethod.getParameterCount() == 1 && onOpenMethod.getParameterTypes()[0].equals(WebSocketSession.class)) {
                        onOpenMethod.invoke(endpointInfo.getHandlerBean(), wsSession);
                    } else if (onOpenMethod.getParameterCount() == 0) {
                        onOpenMethod.invoke(endpointInfo.getHandlerBean());
                    }
                } catch (Exception e) {
                    System.err.println("Error calling @OnOpen method for endpoint " + requestPath + ": " + e.getMessage());
                    e.printStackTrace();
                    wsSession.close(); // 오류 발생 시 세션 닫기
                    webSocketContainer.removeSession(endpointInfo.getPathPattern().getOriginalPattern(), sessionId);
                    return;
                }
            }

            // 6. 웹소켓 메시지 통신 루프 시작
            // 이 루프는 WebSocketSession이 메시지 수신/발신 로직을 처리하도록 위임
            try {
                // WebSocketSession이 메시지 수신 루프를 내부적으로 처리하도록 함
                wsSession.startMessageLoop(); // 이 메서드 내에서 frameParser 사용
            } catch (IOException e) { // 클라이언트 연결 끊김 등
                System.out.println("WebSocket connection for " + sessionId + " closed due to IOException: " + e.getMessage());
                // TODO: @OnError 호출
                Method onErrorMethod = endpointInfo.getOnErrorMethod();
                if (onErrorMethod != null) {
                    try {
                        if (onErrorMethod.getParameterCount() == 2 && onErrorMethod.getParameterTypes()[0].equals(WebSocketSession.class) && onErrorMethod.getParameterTypes()[1].equals(Throwable.class)) {
                            onErrorMethod.invoke(endpointInfo.getHandlerBean(), wsSession, e);
                        } else if (onErrorMethod.getParameterCount() == 1 && onErrorMethod.getParameterTypes()[0].equals(WebSocketSession.class)) {
                            onErrorMethod.invoke(endpointInfo.getHandlerBean(), wsSession);
                        } else if (onErrorMethod.getParameterCount() == 1 && onErrorMethod.getParameterTypes()[0].equals(Throwable.class)) {
                            onErrorMethod.invoke(endpointInfo.getHandlerBean(), e);
                        } else if (onErrorMethod.getParameterCount() == 0) {
                            onErrorMethod.invoke(endpointInfo.getHandlerBean());
                        }
                    } catch (Exception ex) {
                        System.err.println("Error calling @OnError method: " + ex.getMessage());
                        ex.printStackTrace();
                    }
                }
            } catch (Exception e) { // 그 외 예외
                System.err.println("Error in WebSocket session " + sessionId + ": " + e.getMessage());
                e.printStackTrace();
                // TODO: @OnError 호출
                Method onErrorMethod = endpointInfo.getOnErrorMethod();
                if (onErrorMethod != null) {
                    try {
                        if (onErrorMethod.getParameterCount() == 2 && onErrorMethod.getParameterTypes()[0].equals(WebSocketSession.class) && onErrorMethod.getParameterTypes()[1].equals(Throwable.class)) {
                            onErrorMethod.invoke(endpointInfo.getHandlerBean(), wsSession, e);
                        } else if (onErrorMethod.getParameterCount() == 1 && onErrorMethod.getParameterTypes()[0].equals(WebSocketSession.class)) {
                            onErrorMethod.invoke(endpointInfo.getHandlerBean(), wsSession);
                        } else if (onErrorMethod.getParameterCount() == 1 && onErrorMethod.getParameterTypes()[0].equals(Throwable.class)) {
                            onErrorMethod.invoke(endpointInfo.getHandlerBean(), e);
                        } else if (onErrorMethod.getParameterCount() == 0) {
                            onErrorMethod.invoke(endpointInfo.getHandlerBean());
                        }
                    } catch (Exception ex) {
                        System.err.println("Error calling @OnError method: " + ex.getMessage());
                        ex.printStackTrace();
                    }
                }
            } finally {
                // 7. 연결 종료 (@OnClose 호출)
                Method onCloseMethod = endpointInfo.getOnCloseMethod();
                if (onCloseMethod != null) {
                    try {
                        if (onCloseMethod.getParameterCount() == 1 && onCloseMethod.getParameterTypes()[0].equals(WebSocketSession.class)) {
                            onCloseMethod.invoke(endpointInfo.getHandlerBean(), wsSession);
                        } else if (onCloseMethod.getParameterCount() == 0) {
                            onCloseMethod.invoke(endpointInfo.getHandlerBean());
                        }
                    } catch (Exception e) {
                        System.err.println("Error calling @OnClose method for endpoint " + requestPath + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                }
                // 8. 세션 제거
                webSocketContainer.removeSession(endpointInfo.getPathPattern().getOriginalPattern(), sessionId);
                System.out.println("WebSocket session " + sessionId + " removed.");
                if (!socket.isClosed()) { // 이미 닫혔을 수 있으므로 체크
                    socket.close(); // 최종 소켓 닫기
                }
            }

        } catch (Exception e) {
            System.err.println("Error during WebSocket handshake or initial setup: " + e.getMessage());
            e.printStackTrace();
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }

    @Override
    public boolean supports(String protocol) {
        return "WEBSOCKET".equals(protocol);
    }

    private String readRawHttpRequest(BufferedReader in) throws IOException {
        StringBuilder sb = new StringBuilder();
        String line;
        int contentLength = 0;
        boolean firstLine = true;

        // HTTP 요청 라인 + 헤더 읽기
        while ((line = in.readLine()) != null && !line.isEmpty()) {
            if (firstLine) {
                // 첫 줄에서 Content-Length를 미리 파싱하는 것은 부정확할 수 있으므로,
                // RequestParser에게 맡기는 것이 좋습니다. 여기서는 단순히 헤더를 읽습니다.
                firstLine = false;
            }
            sb.append(line).append("\r\n");
            // Content-Length는 헤더 파서에게 맡김. 여기서는 HTTP 바디를 위해 미리 파싱
            if (line.toLowerCase().startsWith("content-length:")) {
                contentLength = Integer.parseInt(line.substring(line.indexOf(':') + 1).trim());
            }
        }
        sb.append("\r\n"); // 헤더와 바디 구분자

        // HTTP 바디 읽기
        if (contentLength > 0) {
            char[] body = new char[contentLength];
            int totalRead = 0;
            int read;
            while (totalRead < contentLength && (read = in.read(body, totalRead, contentLength - totalRead)) != -1) {
                totalRead += read;
            }
            sb.append(body, 0, totalRead); // 읽은 만큼만 추가
        }
        return sb.toString();
    }


    // HTTP 응답을 보내는 헬퍼 메서드 (핸드셰이크 실패 또는 엔드포인트 없을 때)
    private void sendHttpResponse(BufferedWriter out, int statusCode, String statusText, String message) throws IOException {
        out.write("HTTP/1.1 " + statusCode + " " + statusText + "\r\n");
        out.write("Content-Type: text/plain;charset=UTF-8\r\n");
        out.write("Content-Length: " + message.getBytes(StandardCharsets.UTF_8).length + "\r\n");
        out.write("\r\n");
        out.write(message);
        out.flush();
    }
}
