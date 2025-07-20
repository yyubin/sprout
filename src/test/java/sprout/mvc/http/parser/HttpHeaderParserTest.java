package sprout.mvc.http.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HttpHeaderParserTest {

    private HttpHeaderParser parser;

    @BeforeEach
    void setUp() {
        parser = new HttpHeaderParser();
    }

    @Test
    @DisplayName("표준적인 HTTP 헤더를 정확히 파싱해야 한다.")
    void parse_shouldParseStandardHeaders() {
        // given
        String rawHeaders = """
                Host: example.com
                Content-Type: application/json
                Accept: */*
                User-Agent: my-test-client/1.0""";

        // when
        Map<String, String> headers = parser.parse(rawHeaders);

        // then
        assertEquals(4, headers.size());
        assertEquals("example.com", headers.get("Host"));
        assertEquals("application/json", headers.get("Content-Type"));
        assertEquals("*/*", headers.get("Accept"));
        assertEquals("my-test-client/1.0", headers.get("User-Agent"));
    }

    @Test
    @DisplayName("CRLF(\\r\\n) 줄바꿈을 사용하는 헤더를 파싱해야 한다.")
    void parse_shouldHandleCRLFLineEndings() {
        // given
        String rawHeaders = "Host: example.com\r\nConnection: keep-alive\r\n";

        // when
        Map<String, String> headers = parser.parse(rawHeaders);

        // then
        assertEquals(2, headers.size());
        assertEquals("example.com", headers.get("Host"));
        assertEquals("keep-alive", headers.get("Connection"));
    }

    @Test
    @DisplayName("헤더 키와 값 주변의 불필요한 공백을 제거해야 한다.")
    void parse_shouldTrimWhitespace() {
        // given
        String rawHeaders = "  Host  :   example.com   \nContent-Length:123";

        // when
        Map<String, String> headers = parser.parse(rawHeaders);

        // then
        assertEquals(2, headers.size());
        assertEquals("example.com", headers.get("Host"));
        assertEquals("123", headers.get("Content-Length"));
    }

    @Test
    @DisplayName("null, 비어있거나 공백만 있는 문자열 입력 시 빈 맵을 반환해야 한다.")
    void parse_shouldReturnEmptyMapForNullOrBlankInput() {
        // when
        Map<String, String> forNull = parser.parse(null);
        Map<String, String> forEmpty = parser.parse("");
        Map<String, String> forBlank = parser.parse("   \r\n   ");

        // then
        assertTrue(forNull.isEmpty());
        assertTrue(forEmpty.isEmpty());
        assertTrue(forBlank.isEmpty());
    }

    @Test
    @DisplayName("잘못된 형식의 라인이 포함된 경우 해당 라인을 무시하고 정상적인 헤더만 파싱해야 한다.")
    void parse_shouldIgnoreInvalidLines() {
        // given
        // HTTP 요청의 첫 라인(Request Line)과 빈 줄이 포함된 경우
        String rawHeaders = """
                GET /test HTTP/1.1
                Host: valid.host

                Accept: text/html""";

        // when
        Map<String, String> headers = parser.parse(rawHeaders);

        // then
        assertEquals(2, headers.size(), "유효한 헤더만 파싱되어야 합니다.");
        assertEquals("valid.host", headers.get("Host"));
        assertEquals("text/html", headers.get("Accept"));
        assertNull(headers.get("GET /test HTTP/1.1"));
    }

    @Test
    @DisplayName("값이 비어있는 헤더도 정상적으로 파싱해야 한다.")
    void parse_shouldHandleEmptyHeaderValue() {
        // given
        String rawHeaders = "X-Custom-Header:\r\nHost: example.com";

        // when
        Map<String, String> headers = parser.parse(rawHeaders);

        // then
        assertEquals(2, headers.size());
        assertTrue(headers.containsKey("X-Custom-Header"));
        assertEquals("", headers.get("X-Custom-Header"), "헤더 값은 빈 문자열이어야 합니다.");
        assertEquals("example.com", headers.get("Host"));
    }
}