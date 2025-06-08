package sprout.boot;


import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public final class YamlConfigLoader {
    public static List<String> loadPackages(String yaml) {
        Yaml y = new Yaml();
        try (InputStream in = YamlConfigLoader.class.getClassLoader().getResourceAsStream(yaml)) {
            Map<String, List<String>> data = y.load(in);
            return data.getOrDefault("packages", List.of("app")); // fallback
        } catch (Exception e) {
            throw new IllegalStateException("Unable to read config.yml", e);
        }
    }
}