package sprout.mvc.http.parser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class QueryStringParserTest {

    private final QueryStringParser parser = new QueryStringParser();

    @Test
    @DisplayName("쿼리 스트링이 없는 경우 빈 맵을 반환해야 한다.")
    void parse_noQueryString() {
        String rawPath = "/path/to/resource";
        Map<String, String> result = parser.parse(rawPath);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("빈 쿼리 스트링을 파싱할 때 빈 맵을 반환해야 한다.")
    void parse_emptyQueryString() {
        String rawPath = "/path/to/resource?";
        Map<String, String> result = parser.parse(rawPath);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("단일 쿼리 파라미터를 올바르게 파싱해야 한다.")
    void parse_singleParameter() {
        String rawPath = "/path?name=value";
        Map<String, String> result = parser.parse(rawPath);
        assertEquals(1, result.size());
        assertEquals("value", result.get("name"));
    }

    @Test
    @DisplayName("여러 쿼리 파라미터를 올바르게 파싱해야 한다.")
    void parse_multipleParameters() {
        String rawPath = "/path?param1=value1&param2=value2";
        Map<String, String> result = parser.parse(rawPath);
        System.out.println(result);
        assertEquals(2, result.size());
        assertEquals("value1", result.get("param1"));
        assertEquals("value2", result.get("param2"));
    }

    @Test
    @DisplayName("URL 인코딩된 값을 올바르게 디코딩해야 한다.")
    void parse_urlEncodedValues() {
        String encodedValue = URLEncoder.encode("한글 값", StandardCharsets.UTF_8);
        String rawPath = "/path?data=" + encodedValue;
        Map<String, String> result = parser.parse(rawPath);
        assertEquals(1, result.size());
        assertEquals("한글 값", result.get("data"));
    }

    @Test
    @DisplayName("키에만 인코딩된 값이 있는 경우를 올바르게 디코딩해야 한다.")
    void parse_urlEncodedKey() {
        String encodedKey = URLEncoder.encode("한글 키", StandardCharsets.UTF_8);
        String rawPath = "/path?" + encodedKey + "=value";
        Map<String, String> result = parser.parse(rawPath);
        assertEquals(1, result.size());
        assertEquals("value", result.get("한글 키"));
    }

    @Test
    @DisplayName("키와 값 모두 인코딩된 경우를 올바르게 디코딩해야 한다.")
    void parse_urlEncodedKeyAndValue() {
        String encodedKey = URLEncoder.encode("키1", StandardCharsets.UTF_8);
        String encodedValue = URLEncoder.encode("값1", StandardCharsets.UTF_8);
        String rawPath = "/path?" + encodedKey + "=" + encodedValue;
        Map<String, String> result = parser.parse(rawPath);
        assertEquals(1, result.size());
        assertEquals("값1", result.get("키1"));
    }

    @Test
    @DisplayName("값이 없는 파라미터를 올바르게 처리해야 한다.")
    void parse_parameterWithoutValue() {
        String rawPath = "/path?param1=&param2=value2";
        Map<String, String> result = parser.parse(rawPath);
        assertEquals(2, result.size());
        assertEquals("", result.get("param1"));
        assertEquals("value2", result.get("param2"));
    }

    @Test
    @DisplayName("동일한 키가 여러 번 나타나는 경우 마지막 값을 사용해야 한다.")
    void parse_duplicateKeys() {
        String rawPath = "/path?name=value1&name=value2";
        Map<String, String> result = parser.parse(rawPath);
        assertEquals(1, result.size());
        assertEquals("value2", result.get("name")); // Map의 특성상 마지막 값이 덮어씌워짐
    }

    @Test
    @DisplayName("값이 없는 파라미터를 빈 문자열 값으로 파싱해야 한다.")
    void parse_parameterWithoutExplicitValue() {
        String rawPath = "/path?param1=value1&malformed&param2=value2";
        Map<String, String> result = parser.parse(rawPath);
        System.out.println(result); // 예상 출력: {param1=value1, malformed=, param2=value2}
        assertEquals(3, result.size()); // 파라미터가 3개이므로 3으로 변경
        assertEquals("value1", result.get("param1"));
        assertEquals("", result.get("malformed")); // null이 아니라 빈 문자열로 검증
        assertEquals("value2", result.get("param2"));
    }

    @Test
    @DisplayName("쿼리 스트링에 특수 문자가 포함된 경우를 올바르게 처리해야 한다.")
    void parse_specialCharactersInQuery() {
        // 테스트할 원본 특수 문자열
        String originalSpecialChars = "!@#$%^&*()_+-=[]{};':\"|,.<>/?";

        // 원본 특수 문자열을 UTF-8로 URL 인코딩
        String encodedSpecialChars = URLEncoder.encode(originalSpecialChars, StandardCharsets.UTF_8);

        // 인코딩된 문자열을 포함하는 rawPath 생성
        String rawPath = "/path?data=" + encodedSpecialChars;

        Map<String, String> result = parser.parse(rawPath);

        assertEquals(1, result.size());
        // 결과는 디코딩된 원본 문자열이어야 함
        assertEquals(originalSpecialChars, result.get("data"));
    }

    @Test
    @DisplayName("공백이 포함된 쿼리 파라미터를 올바르게 디코딩해야 한다.")
    void parse_whitespace() {
        String encodedValue = URLEncoder.encode("value with space", StandardCharsets.UTF_8);
        String rawPath = "/path?param=" + encodedValue;
        Map<String, String> result = parser.parse(rawPath);
        assertEquals(1, result.size());
        assertEquals("value with space", result.get("param"));
    }
}