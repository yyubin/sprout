package sprout.server.websocket.endpoint;

import java.util.List;
import java.util.Map;

public interface EndpointConfig {
    List<Encoder> getEncoders();

    List<Decoder> getDecoders();

    Map<String,Object> getUserProperties();
}
