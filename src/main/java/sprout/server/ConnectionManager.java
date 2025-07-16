package sprout.server;

import java.io.IOException;
import java.net.Socket;

public interface ConnectionManager {
    void handleConnection(Socket socket) throws Exception;
}
