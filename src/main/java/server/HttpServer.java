package server;

import sprout.beans.annotation.Component;
import sprout.beans.annotation.Requires;
import legacy.http.request.RequestHandler;
import sprout.mvc.http.HttpResponse;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@Requires(dependsOn = {RequestHandler.class})
public class HttpServer {

    private final ExecutorService threadPool;
    private final RequestHandler requestHandler;

    public HttpServer(RequestHandler requestHandler) {
        this.requestHandler = requestHandler;
        this.threadPool = Executors.newFixedThreadPool(10);
    }

    public void serverStart(int port) throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        while (true) {
            Socket clientSocket = serverSocket.accept();
            threadPool.execute(() -> handleClient(clientSocket));
        }
    }

    private void handleClient(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()))) {

            StringBuilder rawRequest = new StringBuilder();
            String line;
            int contentLength = 0;

            while ((line = in.readLine()) != null && !line.isEmpty()) {
                rawRequest.append(line).append("\n");
                if (line.startsWith("Content-Length:")) {
                    contentLength = Integer.parseInt(line.split(":")[1].trim());
                }
            }

            if (rawRequest.length() == 0) {
                return;
            }

            StringBuilder body = new StringBuilder();
            if (contentLength > 0) {
                char[] buffer = new char[contentLength];
                int bytesRead = in.read(buffer, 0, contentLength);
                if (bytesRead > 0) {
                    body.append(buffer, 0, bytesRead);
                }
            }

            rawRequest.append("\r\n").append(body);

            System.out.println("Received Request:\n" + rawRequest);

            HttpResponse<?> result = (HttpResponse<?>) requestHandler.handleRequest(rawRequest.toString());
            if (result != null) {
                String responseBody = result.getResponseCode().getCode() + " " + result.getResponseCode().getMessage() + " " + result.getBody();

                String responseHeaders = "HTTP/1.1 " + result.getResponseCode().getCode() + " " + result.getResponseCode().getMessage() + "\r\n" +
                        "Content-Type: application/json\r\n" +
                        "Content-Length: " + responseBody.getBytes(StandardCharsets.UTF_8).length + "\r\n" +
                        "\r\n";

                out.write(responseHeaders);
                out.write(responseBody);
            } else {
                out.write("HTTP/1.1 500 Internal Server Error\r\n\r\n");
            }
            out.flush();

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
