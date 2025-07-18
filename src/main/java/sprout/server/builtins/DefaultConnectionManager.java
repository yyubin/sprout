package sprout.server.builtins;

import sprout.beans.annotation.Component;
import sprout.server.ConnectionManager;
import sprout.server.ProtocolDetector;
import sprout.server.ProtocolHandler;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.List;
import java.util.Objects;

@Component
public class DefaultConnectionManager implements ConnectionManager {

    private final List<ProtocolDetector> detectors;
    private final List<ProtocolHandler> handlers;

    public DefaultConnectionManager(List<ProtocolDetector> detectors, List<ProtocolHandler> handlers) {
        this.detectors = detectors;
        this.handlers = handlers;
    }

    @Override
    public void handleConnection(Socket socket) throws Exception {
        InputStream originalInputStream = socket.getInputStream();
        BufferedInputStream bufferedIn = new BufferedInputStream(originalInputStream);
        String detectedProtocol = "UNKNOWN";

        for (ProtocolDetector detector : detectors) {
            detectedProtocol = detector.detect(bufferedIn);
            try {
                detectedProtocol = detector.detect(bufferedIn);
                if (!"UNKNOWN".equals(detectedProtocol) && detectedProtocol != null) {
                    break;
                }
            } catch (Exception e) {
                System.err.println("Error detecting protocol: " + e.getMessage());
            }
        }

        if ("UNKNOWN".equals(detectedProtocol) || detectedProtocol == null) {
            System.err.println("Unknown protocol detected. Closing socket: " + socket);
            socket.close();
            return;
        }

        boolean handlerFound = false;
        for (ProtocolHandler handler : handlers) {
            if (handler.supports(detectedProtocol)) {
                System.out.println("Handling connection with " + handler.getClass().getSimpleName() + " for protocol " + detectedProtocol);
                handler.handle(socket);
                handlerFound = true;
                break;
            }
        }

        if (!handlerFound) {
            System.err.println("No handler found for protocol: " + detectedProtocol + ". Closing socket: " + socket);
            socket.close();
        }
    }
}
