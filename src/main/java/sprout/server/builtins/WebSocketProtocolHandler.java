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
import sprout.server.websocket.message.WebSocketMessageDispatcher;
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
    private final List<WebSocketMessageDispatcher> messageDispatchers;

    public WebSocketProtocolHandler(
            WebSocketHandshakeHandler handshakeHandler,
            WebSocketContainer webSocketContainer,
            WebSocketEndpointRegistry endpointRegistry,
            HttpRequestParser httpRequestParser,
            WebSocketFrameParser frameParser,
            WebSocketFrameEncoder frameEncoder,
            List<WebSocketArgumentResolver> webSocketArgumentResolvers,
            List<WebSocketMessageDispatcher> messageDispatchers
    ) {
        this.handshakeHandler = handshakeHandler;
        this.webSocketContainer = webSocketContainer;
        this.endpointRegistry = endpointRegistry;
        this.httpRequestParser = httpRequestParser;
        this.frameParser = frameParser;
        this.frameEncoder = frameEncoder;
        this.webSocketArgumentResolvers = webSocketArgumentResolvers;
        this.messageDispatchers = messageDispatchers;
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
            String rawHttpRequest = readRawHttpRequestContent(httpReader);
            HttpRequest<?> request = httpRequestParser.parse(rawHttpRequest);

            if (!request.isValid()) {
                System.out.println("Empty or invalid HTTP request for websocket handshake. Closing socket.");
                socket.close();
                return;
            }

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

            // DefaultWebSocketSession 생성 시 argumentResolvers와 messageParser 전달
            WebSocketSession wsSession = new DefaultWebSocketSession(sessionId, socket, request, endpointInfo, frameParser, frameEncoder, pathVars, webSocketArgumentResolvers, messageDispatchers);

            webSocketContainer.addSession(endpointInfo.getPathPattern().getOriginalPattern(), wsSession);


            // 5. @OnOpen 메서드 호출
            try {
                wsSession.callOnOpenMethod();
            } catch (Exception e) {
                System.err.println("Error calling @OnOpen method for endpoint " + requestPath + ": " + e.getMessage());
                e.printStackTrace();
                wsSession.close();
                webSocketContainer.removeSession(endpointInfo.getPathPattern().getOriginalPattern(), sessionId);
                return;
            }


            // 6. 웹소켓 메시지 통신 루프 시작
            // 이 루프는 WebSocketSession이 메시지 수신/발신 로직을 처리하도록 위임
            try {
                wsSession.startMessageLoop(); // 이 메서드 내에서 frameParser 사용
            } catch (IOException e) { // 클라이언트 연결 끊김 등
                System.out.println("WebSocket connection for " + sessionId + " closed due to IOException: " + e.getMessage());
                wsSession.callOnErrorMethod(e);
            } catch (Exception e) {
                System.err.println("Error in WebSocket session " + sessionId + ": " + e.getMessage());
                e.printStackTrace();
                wsSession.callOnErrorMethod(e);
            } finally {
                // 7. 연결 종료 (@OnClose 호출)
                wsSession.callOnCloseMethod(CloseCodes.getCloseCode(1000));
                webSocketContainer.removeSession(endpointInfo.getPathPattern().getOriginalPattern(), sessionId);
                System.out.println("WebSocket session " + sessionId + " removed.");
                if (!socket.isClosed()) {
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

    private String readRawHttpRequestContent(BufferedReader in) throws IOException {
        StringBuilder sb = new StringBuilder();
        String line;
        int contentLength = 0;
        // 헤더 끝을 나타내는 플래그
        boolean headersDone = false;

        // HTTP 요청 라인 + 헤더 읽기
        // readLine()이 null을 반환하면 클라이언트가 연결을 끊은 것
        // line.isEmpty()는 헤더 끝의 빈 줄을 의미
        while ((line = in.readLine()) != null) {
            if (line.isEmpty()) { // 빈 줄은 헤더의 끝을 의미 (CRLFCRLF 또는 LF LF)
                headersDone = true;
                break;
            }
            sb.append(line).append("\r\n"); // HTTP 규격에 맞게 CRLF 추가
            if (line.toLowerCase().startsWith("content-length:")) {
                try {
                    contentLength = Integer.parseInt(line.substring(line.indexOf(':') + 1).trim());
                } catch (NumberFormatException e) {
                    System.err.println("Warning: Invalid Content-Length header: " + line);
                    contentLength = 0; // 파싱 실패 시 0으로 설정
                }
            }
        }
        sb.append("\r\n"); // 헤더와 바디 구분자 (readLine()이 빈 줄을 이미 제거했을 수도 있지만, 안전을 위해 추가)

        // HTTP 바디 읽기 (Content-Length가 있고, 헤더가 끝났을 경우에만)
        if (contentLength > 0 && headersDone) {
            char[] body = new char[contentLength];
            int totalRead = 0;
            int read;
            // Content-Length만큼 정확히 읽으려고 시도
            // read()는 모든 바이트를 한 번에 읽지 않을 수 있으므로 루프 필요
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
