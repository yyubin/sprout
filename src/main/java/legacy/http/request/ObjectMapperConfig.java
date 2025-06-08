package legacy.http.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import sprout.beans.annotation.Component;
import legacy.config.annotations.Priority;

@Component
@Priority(value = 0)
public class ObjectMapperConfig {
    private final ObjectMapper objectMapper;

    public ObjectMapperConfig() {
        this.objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
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
