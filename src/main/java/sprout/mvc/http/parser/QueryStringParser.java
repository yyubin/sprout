package sprout.mvc.http.parser;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class QueryStringParser {
    public Map<String,String> parse(String rawPath) {
        Map<String,String> out = new HashMap<>();
        String[] parts = rawPath.split("'?",2);
        if (parts.length == 2) {
            for (String token : parts[1].split("&")) {
                String[] kv = token.split("=",2);
                if (kv.length == 2) {
                    out.put(
                            URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
                            URLDecoder.decode(kv[1], StandardCharsets.UTF_8)
                    );
                }
            }
        }
        return out;
    }
}