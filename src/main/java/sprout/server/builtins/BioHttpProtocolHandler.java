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

    private void detachFromSelector(SocketChannel ch, Selector sel) {
        SelectionKey k = ch.keyFor(sel);
        if (k != null) {
            k.cancel();
            k.attach(null);
        }
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
