package sprout.server;

import java.net.Socket;

public interface ProtocolHandler {
    void handle(Socket socket) throws Exception;
    boolean supports(String protocol);
}
