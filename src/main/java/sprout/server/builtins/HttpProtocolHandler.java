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
        detachFromSelector(channel, selector);
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

    private String readRawRequest(ByteBuffer initial, InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();

        // 1) initial buffer
        if (initial != null && initial.hasRemaining()) {
            byte[] arr = new byte[initial.remaining()];
            initial.get(arr);
            sb.append(new String(arr, StandardCharsets.UTF_8));
        }

        // 2) 헤더 끝까지 읽기
        BufferedInputStream bin = new BufferedInputStream(in);
        while (!sb.toString().contains("\r\n\r\n")) {
            int ch = bin.read();
            if (ch == -1) break; // 연결 끊김
            sb.append((char) ch);
        }

        // 파싱해서 Content-Length or chunked 확인
        String headerPart = sb.toString();
        int headerEnd = headerPart.indexOf("\r\n\r\n");
        if (headerEnd < 0) return headerPart; // 잘못된 요청

        String headers = headerPart.substring(0, headerEnd);
        String bodyStart = headerPart.substring(headerEnd + 4);

        int contentLength = parseContentLength(headers); // 없으면 -1
        boolean chunked = isChunked(headers);

        if (chunked) {
            // TODO: chunked 디코딩
            bodyStart += readChunkedBody(bin);
        } else if (contentLength > -1) {
            int alreadyRead = bodyStart.getBytes(StandardCharsets.UTF_8).length;
            int remaining = contentLength - alreadyRead;
            if (remaining > 0) {
                byte[] bodyBytes = bin.readNBytes(remaining);
                bodyStart += new String(bodyBytes, StandardCharsets.UTF_8);
            }
        }

        return headers + "\r\n\r\n" + bodyStart;
    }

    private int parseContentLength(String headers) {
        for (String line : headers.split("\r\n")) {
            if (line.toLowerCase().startsWith("content-length:")) {
                return Integer.parseInt(line.split(":")[1].trim());
            }
        }
        return -1;
    }

    private boolean isChunked(String headers) {
        for (String line : headers.split("\r\n")) {
            if (line.toLowerCase().startsWith("transfer-encoding:")
                    && line.toLowerCase().contains("chunked")) {
                return true;
            }
        }
        return false;
    }

    private String readChunkedBody(InputStream in) throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        StringBuilder body = new StringBuilder();
        while (true) {
            String lenLine = r.readLine();
            if (lenLine == null) break;
            int len = Integer.parseInt(lenLine.trim(), 16);
            if (len == 0) {
                // trailing headers consume up to empty line
                while (!"".equals(r.readLine())) {}
                break;
            }
            char[] buf = new char[len];
            int read = r.read(buf);
            body.append(buf, 0, read);
            r.readLine(); // consume CRLF
        }
        return body.toString();
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

    static void detachFromSelector(SocketChannel ch, Selector sel) {
        SelectionKey k = ch.keyFor(sel);
        if (k != null) {
            k.cancel();
            k.attach(null);
        }
    }

}
