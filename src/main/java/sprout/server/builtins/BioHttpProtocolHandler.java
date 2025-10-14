package sprout.server.builtins;

import sprout.mvc.dispatcher.RequestDispatcher;
import sprout.mvc.http.HttpRequest;
import sprout.mvc.http.HttpResponse;
import sprout.mvc.http.ResponseEntity;
import sprout.mvc.http.parser.HttpRequestParser;
import sprout.server.AcceptableProtocolHandler;
import sprout.server.RequestExecutorService;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static sprout.server.HttpUtils.readRawRequest;


public class BioHttpProtocolHandler implements AcceptableProtocolHandler {
    private final RequestDispatcher dispatcher;
    private final HttpRequestParser parser;
    private final RequestExecutorService requestExecutorService;

    public BioHttpProtocolHandler(RequestDispatcher dispatcher, HttpRequestParser parser, RequestExecutorService requestExecutorService) {
        this.dispatcher = dispatcher;
        this.parser = parser;
        this.requestExecutorService = requestExecutorService;
    }

    @Override
    public void accept(SocketChannel channel, Selector selector,  ByteBuffer initialBuffer) throws Exception {
        detachFromSelector(channel, selector);
        channel.configureBlocking(true);
        Socket socket = channel.socket();

        requestExecutorService.execute(() -> {
            try (InputStream in = socket.getInputStream();
                 OutputStream out = socket.getOutputStream()) {

                ByteBuffer currentBuffer = initialBuffer;

                // HTTP/1.1 keep-alive 처리: 같은 연결에서 여러 요청을 순차 처리
                while (!socket.isClosed()) {
                    String raw = readRawRequest(currentBuffer, in);

                    // 요청이 없거나 연결이 끊긴 경우
                    if (raw.isBlank()) break;

                    HttpRequest<?> req = parser.parse(raw);
                    HttpResponse res = new HttpResponse();

                    dispatcher.dispatch(req, res);

                    // Connection 헤더 확인
                    String connectionHeader = req.getHeaders().getOrDefault("Connection", "keep-alive");
                    boolean shouldClose = "close".equalsIgnoreCase(connectionHeader);

                    writeResponse(out, res.getResponseEntity(), shouldClose);

                    // Content-Length가 없거나 Connection: close 요청이면 종료
                    if (shouldClose) {
                        break;
                    }

                    // 다음 요청을 위해 버퍼 초기화
                    currentBuffer = null;
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException ignored) {}
            }
        });
    }

    @Override
    public boolean supports(String protocol) {
        return "HTTP/1.1".equals(protocol);
    }

    private void detachFromSelector(SocketChannel ch, Selector sel) {
        SelectionKey k = ch.keyFor(sel);
        if (k != null) {
            k.cancel();
            k.attach(null);
        }
    }

    private void writeResponse(OutputStream out, ResponseEntity<?> res, boolean shouldClose) throws IOException {
        if (res == null) return;

        // Body를 바이트로 변환 (UTF-8)
        byte[] bodyBytes = res.getBody() != null
            ? res.getBody().toString().getBytes(StandardCharsets.UTF_8)
            : new byte[0];

        // HTTP 헤더 작성
        StringBuilder header = new StringBuilder();
        header.append("HTTP/1.1 ")
              .append(res.getStatusCode().getCode())
              .append(" ")
              .append(res.getStatusCode().getMessage())
              .append("\r\n");

        // Content-Type
        header.append("Content-Type: ")
              .append(res.getContentType())
              .append("\r\n");

        // Content-Length (바이트 단위로 정확히)
        header.append("Content-Length: ")
              .append(bodyBytes.length)
              .append("\r\n");

        // Connection 헤더: keep-alive 활성화 (HTTP/1.1 기본)
        if (shouldClose) {
            header.append("Connection: close\r\n");
        } else {
            header.append("Connection: keep-alive\r\n");
            header.append("Keep-Alive: timeout=5, max=1000\r\n");
        }

        // Custom headers
        if (res.getHeaders() != null) {
            for (Map.Entry<String, String> entry : res.getHeaders().entrySet()) {
                header.append(entry.getKey())
                      .append(": ")
                      .append(entry.getValue())
                      .append("\r\n");
            }
        }

        // 헤더 끝
        header.append("\r\n");

        // 헤더 + 바디를 바이트 단위로 전송
        out.write(header.toString().getBytes(StandardCharsets.UTF_8));
        out.write(bodyBytes);
        out.flush();
    }


}
