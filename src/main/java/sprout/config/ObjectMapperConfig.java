package sprout.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import sprout.beans.annotation.Bean;
import sprout.beans.annotation.Configuration;

@Configuration
public class ObjectMapperConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

}
