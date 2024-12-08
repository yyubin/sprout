package http.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import config.annotations.Component;
import config.annotations.Priority;

@Component
@Priority(value = 0)
public class ObjectMapperConfig {
    private final ObjectMapper objectMapper;

    public ObjectMapperConfig() {
        this.objectMapper = new ObjectMapper();
    }

    public String toJson(Object data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
