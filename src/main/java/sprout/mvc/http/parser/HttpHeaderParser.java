package sprout.mvc.http.parser;

import sprout.beans.annotation.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class HttpHeaderParser {
    private static final Pattern HEADER_PATTERN = Pattern.compile("^([^:]+):\\s*(.*)$");

    public Map<String, String> parse(String rawHeaders) {
        if (rawHeaders == null || rawHeaders.isBlank()) {
            return Collections.emptyMap();
        }

        Map<String, String> headers = new HashMap<>();
        String[] lines = rawHeaders.split("\r?\n"); // CRLF 또는 LF로 라인 분리

        for (String line : lines) {
            if (line.isBlank()) { // 빈 줄은 무시
                continue;
            }
            Matcher matcher = HEADER_PATTERN.matcher(line);
            if (matcher.matches()) {
                String key = matcher.group(1).trim();
                String value = matcher.group(2).trim();
                headers.put(key, value);
            } else {
                // 유효하지 않은 헤더 형식 (예: HTTP/1.1 200 OK 같은 요청 라인)
                // 이 부분은 RequestLineParser가 이미 처리했어야 함
                System.err.println("Warning: Invalid header format detected: " + line);
            }
        }
        return headers;
    }
}
