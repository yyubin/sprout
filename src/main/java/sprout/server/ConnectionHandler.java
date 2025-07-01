package sprout.server;

import sprout.mvc.dispatcher.RequestDispatcher;
import sprout.mvc.http.ResponseEntity;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ConnectionHandler implements Runnable {
    private final Socket socket;
    private final RequestDispatcher dispatcher;

    public ConnectionHandler(Socket socket, RequestDispatcher dispatcher) {
        this.socket = socket; this.dispatcher = dispatcher;
    }

    @Override public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {

            String raw = readRawRequest(in);
            if (raw.isBlank()) return;

            ResponseEntity<?> resp = dispatcher.dispatch(raw);
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

    private void writeResponse(BufferedWriter out, ResponseEntity<?> res) throws IOException {
        String body = (String) res.getBody();

        out.write("HTTP/1.1 " + res.getStatusCode().getCode() + " " + res.getStatusCode().getMessage() + "\r\n");
        out.write("Content-Type: application/json\r\n");
        out.write("Content-Length: " + body.getBytes(StandardCharsets.UTF_8).length + "\r\n");
        out.write("\r\n");
        out.write(body);
        out.flush();
    }
}
