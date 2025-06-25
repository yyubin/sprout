package sprout.config;

import org.yaml.snakeyaml.Yaml;
import sprout.beans.annotation.Component;

import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

@Component
public class AppConfig {

    private final Map<String, Object> properties;

    public AppConfig() {
        // AppConfig가 생성될 때 application.yml을 로드
        Yaml yaml = new Yaml();
        InputStream inputStream = this.getClass()
                .getClassLoader()
                .getResourceAsStream("application.yml");

        if (inputStream != null) {
            this.properties = yaml.load(inputStream);
            System.out.println("application.yml loaded successfully.");
        } else {
            System.err.println("application.yml not found in classpath. Using empty configuration.");
            this.properties = Collections.emptyMap();
        }
    }

    private Object getProperty(String key) {
        String[] keys = key.split("\\.");
        Map<String, Object> currentMap = this.properties;
        for (int i = 0; i < keys.length - 1; i++) {
            Object next = currentMap.get(keys[i]);
            if (next instanceof Map) {
                currentMap = (Map<String, Object>) next;
            } else {
                return null;
            }
        }
        return currentMap.get(keys[keys.length - 1]);
    }

    public String getStringProperty(String key, String defaultValue) {
        Object value = getProperty(key);
        return value != null ? value.toString() : defaultValue;
    }

    public int getIntProperty(String key, int defaultValue) {
        Object value = getProperty(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return value != null ? Integer.parseInt(value.toString()) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
