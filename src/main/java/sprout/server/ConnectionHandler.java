package sprout.server;

import sprout.mvc.dispatcher.RequestDispatcher;
import sprout.mvc.http.HttpRequest;
import sprout.mvc.http.HttpResponse;
import sprout.mvc.http.ResponseEntity;
import sprout.mvc.http.parser.HttpRequestParser;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ConnectionHandler implements Runnable {
    private final Socket socket;
    private final RequestDispatcher dispatcher;
    private final HttpRequestParser parser;

    public ConnectionHandler(Socket socket, RequestDispatcher dispatcher, HttpRequestParser parser) {
        this.socket = socket;
        this.dispatcher = dispatcher;
        this.parser = parser;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {

            String raw = readRawRequest(in);
            if (raw.isBlank()) return;

            HttpRequest<?> req = parser.parse(raw);
            HttpResponse res = new HttpResponse();

            dispatcher.dispatch(req, res);

            writeResponse(out, res.getResponseEntity());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String readRawRequest(BufferedReader in) throws IOException {
        StringBuilder sb = new StringBuilder();
        String line;
        int contentLength = 0;

        while ((line = in.readLine()) != null && !line.isEmpty()) {
            sb.append(line).append("\r\n");
            if (line.toLowerCase().startsWith("content-length:")) {
                contentLength = Integer.parseInt(line.substring(line.indexOf(':') + 1).trim());
            }
        }
        sb.append("\r\n");

        if (contentLength > 0) {
            char[] body = new char[contentLength];
            in.read(body, 0, contentLength);
            sb.append(body);
        }
        return sb.toString();
    }

    private void writeResponse(BufferedWriter out, ResponseEntity<?> res) throws IOException {
        if (res == null) return; // Or handle as an error
        String body = (String) res.getBody();
        out.write("HTTP/1.1 " + res.getStatusCode().getCode() + " " + res.getStatusCode().getMessage() + "\r\n");
        out.write("Content-Type: application/json\r\n");
        out.write("Content-Length: " + body.getBytes(StandardCharsets.UTF_8).length + "\r\n");
        out.write("\r\n");
        out.write(body);
        out.flush();
    }
}
