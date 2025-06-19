package sprout.server;

import sprout.mvc.dispatcher.RequestDispatcher;
import sprout.mvc.http.HttpResponse;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

class ConnectionHandler implements Runnable {
    private final Socket socket;
    private final RequestDispatcher dispatcher;

    ConnectionHandler(Socket socket, RequestDispatcher dispatcher) {
        this.socket = socket; this.dispatcher = dispatcher;
    }

    @Override public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {

            String raw = readRawRequest(in);
            if (raw.isBlank()) return;

            HttpResponse<?> resp = dispatcher.dispatch(raw);
            writeResponse(out, resp);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String readRawRequest(BufferedReader in) throws IOException {
        StringBuilder sb = new StringBuilder();
        String line; int contentLength = 0;

        while ((line = in.readLine()) != null && !line.isEmpty()) {
            sb.append(line).append("\r\n");
            if (line.startsWith("Content-Length:"))
                contentLength = Integer.parseInt(line.split(":")[1].trim());
        }
        if (contentLength > 0) {
            char[] buf = new char[contentLength];
            in.read(buf, 0, contentLength);
            sb.append("\r\n").append(buf);
        }
        return sb.toString();
    }

    private void writeResponse(BufferedWriter out, HttpResponse<?> res) throws IOException {
        String body = res.getBody();
        out.write("HTTP/1.1 " + res.getResponseCode().getCode() + " " + res.getResponseCode().getMessage() + "\r\n");
        out.write("Content-Type: application/json\r\n");
        out.write("Content-Length: " + body.getBytes(StandardCharsets.UTF_8).length + "\r\n");
        out.write("\r\n");
        out.write(body);
        out.flush();
    }
}
