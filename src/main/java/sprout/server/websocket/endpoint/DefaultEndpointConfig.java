package sprout.server.websocket.endpoint;

import sprout.beans.annotation.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DefaultEndpointConfig implements EndpointConfig {

    private final List<Encoder> encoders;
    private final List<Decoder> decoders;
    private final Map<String,Object> userProperties = new ConcurrentHashMap<>();

    public DefaultEndpointConfig(List<Encoder> encoders, List<Decoder> decoders) {
        this.encoders = encoders;
        this.decoders = decoders;
    }

    @Override
    public List<Encoder> getEncoders() {
        return encoders;
    }

    @Override
    public List<Decoder> getDecoders() {
        return decoders;
    }

    @Override
    public Map<String, Object> getUserProperties() {
        return userProperties;
    }
}
