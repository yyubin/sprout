package sprout.server.builtins;

import sprout.beans.annotation.Component;
import sprout.mvc.dispatcher.RequestDispatcher;
import sprout.mvc.http.HttpRequest;
import sprout.mvc.http.HttpResponse;
import sprout.mvc.http.ResponseEntity;
import sprout.mvc.http.parser.HttpRequestParser;
import sprout.server.AcceptableProtocolHandler;
import sprout.server.ProtocolHandler;
import sprout.server.ThreadService;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Map;
@Component
public class HttpProtocolHandler implements AcceptableProtocolHandler {
    private final ThreadService threadService;
    private final RequestDispatcher dispatcher;
    private final HttpRequestParser parser;

    public HttpProtocolHandler(RequestDispatcher dispatcher, HttpRequestParser parser, ThreadService threadService) {
        this.dispatcher = dispatcher;
        this.parser = parser;
        this.threadService = threadService;
    }

    @Override
    public void accept(SocketChannel channel, Selector selector,  ByteBuffer initialBuffer) throws Exception {
        channel.configureBlocking(true);
        Socket socket = channel.socket();

        threadService.execute(() -> {
            System.out.println("Worker Thread for http allocated!");
            try (InputStream in = socket.getInputStream();
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {

                String raw = readRawRequest(initialBuffer, in);
                System.out.println("Received request: " + raw);

                if (raw.isBlank()) return;

                HttpRequest<?> req = parser.parse(raw);
                HttpResponse res = new HttpResponse();

                dispatcher.dispatch(req, res);

                writeResponse(out, res.getResponseEntity());

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public boolean supports(String protocol) {
        return "HTTP/1.1".equals(protocol);
    }

    private String readRawRequest(ByteBuffer initialBuffer, InputStream in) throws IOException {
        // 1. 이미 읽은 초기 데이터를 먼저 가져옴
        byte[] initialBytes = new byte[initialBuffer.remaining()];
        initialBuffer.get(initialBytes);
        String initialData = new String(initialBytes, StandardCharsets.UTF_8);

        // 2. 나머지 데이터를 스트림에서 읽어옴
        // BufferedReader를 여기서 생성하여 나머지 스트림을 안전하게 읽음
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder remainingData = new StringBuilder();

        // HTTP 요청은 보통 한 번에 다 읽히므로, non-blocking 상태에서 read-ready가 되면
        // 데이터가 있을 확률이 높다. 간단하게 처리.
        // 더 견고하게 만들려면 헤더 끝(빈 줄)을 만날 때까지 읽는 로직이 필요.
        while (reader.ready()) { // 데이터가 있을 때만 읽도록 시도
            remainingData.append((char)reader.read());
        }

        return initialData + remainingData.toString();
    }

    private void writeResponse(BufferedWriter out, ResponseEntity<?> res) throws IOException {
        if (res == null) return;

        String body = res.getBody() != null ? res.getBody().toString() : "";
        out.write("HTTP/1.1 " + res.getStatusCode().getCode() + " " + res.getStatusCode().getMessage() + "\r\n");

        // Content-Type
        out.write("Content-Type: " + res.getContentType() + "\r\n");

        // Content-Length
        out.write("Content-Length: " + body.getBytes(StandardCharsets.UTF_8).length + "\r\n");

        // Custom headers
        if (res.getHeaders() != null) {
            for (Map.Entry<String, String> header : res.getHeaders().entrySet()) {
                out.write(header.getKey() + ": " + header.getValue() + "\r\n");
            }
        }

        out.write("\r\n");
        out.write(body);
        out.flush();
    }
}
