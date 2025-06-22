package sprout.mvc.http.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import sprout.mvc.http.HttpMethod;
import sprout.mvc.http.HttpRequest;
import sprout.mvc.http.parser.RequestLine;
import sprout.mvc.http.parser.RequestLine;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HttpRequestParserTest {

    private HttpRequestParser httpRequestParser;

    @Mock
    private RequestLineParser mockRequestLineParser;
    @Mock
    private QueryStringParser mockQueryStringParser;

    @BeforeEach
    void setUp() {
        // Mockito 목 객체 초기화
        MockitoAnnotations.openMocks(this);
        // 테스트할 HttpRequestParser 인스턴스 생성, Mock 객체 주입
        httpRequestParser = new HttpRequestParser(mockRequestLineParser, mockQueryStringParser);
    }

    @Test
    @DisplayName("GET 요청의 요청 라인, 쿼리 파라미터, 빈 바디를 올바르게 파싱해야 한다")
    void parse_getRequestBody() {
        // given
        String rawHttpRequest = "GET /path/to/resource?param1=value1&param2=value2 HTTP/1.1\r\n" +
                "Host: localhost:8080\r\n" +
                "Connection: keep-alive\r\n" +
                "User-Agent: Mozilla/5.0\r\n" +
                "\r\n"; // 바디 없음

        // RequestLineParser가 반환할 RequestLine 객체 Mocking

        RequestLine mockRequestLine = new RequestLine(HttpMethod.GET, "/path/to/resource?param1=value1&param2=value2", "/path/to/resource");
        when(mockRequestLineParser.parse(anyString())).thenReturn(mockRequestLine);

        // QueryStringParser가 반환할 쿼리 파라미터 맵 Mocking
        Map<String, String> mockQueryParams = new HashMap<>();
        mockQueryParams.put("param1", "value1");
        mockQueryParams.put("param2", "value2");
        when(mockQueryStringParser.parse(mockRequestLine.rawPath())).thenReturn(mockQueryParams);

        // when
        HttpRequest<?> httpRequest = httpRequestParser.parse(rawHttpRequest);

        // then
        assertNotNull(httpRequest);
        assertThat(httpRequest.getMethod()).isEqualTo(HttpMethod.GET);
        assertThat(httpRequest.getPath()).isEqualTo("/path/to/resource"); // cleanPath
        assertThat(httpRequest.getQueryParams()).isEqualTo(mockQueryParams);
        assertThat(httpRequest.getBody()).isEqualTo(""); // GET 요청이라 바디가 비어있음

        // 의존성 메서드 호출 검증 (선택 사항이지만 좋은 습관)
        verify(mockRequestLineParser).parse("GET /path/to/resource?param1=value1&param2=value2 HTTP/1.1");
        verify(mockQueryStringParser).parse("/path/to/resource?param1=value1&param2=value2");
    }

    @Test
    @DisplayName("POST 요청의 요청 라인, 빈 쿼리 파라미터, JSON 바디를 올바르게 파싱해야 한다")
    void parse_postRequestWithJsonBody() {
        // given
        String jsonBody = "{\"name\":\"testuser\", \"age\":30}";
        String rawHttpRequest = "POST /users HTTP/1.1\r\n" +
                "Content-Type: application/json\r\n" +
                "Content-Length: " + jsonBody.length() + "\r\n" +
                "\r\n" +
                jsonBody;

        // RequestLineParser가 반환할 RequestLine 객체 Mocking
        RequestLine mockRequestLine = new RequestLine(HttpMethod.POST, "/users", "/users");
        when(mockRequestLineParser.parse(anyString())).thenReturn(mockRequestLine);

        // QueryStringParser는 빈 맵 반환 (POST 요청에 쿼리 파라미터 없음)
        when(mockQueryStringParser.parse(anyString())).thenReturn(Collections.emptyMap());

        // when
        HttpRequest<?> httpRequest = httpRequestParser.parse(rawHttpRequest);

        // then
        assertNotNull(httpRequest);
        assertThat(httpRequest.getMethod()).isEqualTo(HttpMethod.POST);
        assertThat(httpRequest.getPath()).isEqualTo("/users");
        assertTrue(httpRequest.getQueryParams().isEmpty());
        assertThat(httpRequest.getBody()).isEqualTo(jsonBody); // 바디가 String 그대로 파싱되어야 함

        // 의존성 메서드 호출 검증
        verify(mockRequestLineParser).parse("POST /users HTTP/1.1");
        verify(mockQueryStringParser).parse("/users"); // rawPath에 쿼리스트링이 없어도 호출됨
    }

    @Test
    @DisplayName("헤더와 바디 구분자가 LF만 있는 경우에도 올바르게 파싱해야 한다")
    void parse_lfOnlyDelimiter() {
        // given
        String jsonBody = "{\"data\":\"test\"}";
        String rawHttpRequest = "PUT /items HTTP/1.1\n" + // \n\n (LF만 있는 구분자)
                "Content-Type: application/json\n" +
                "Content-Length: " + jsonBody.length() + "\n" +
                "\n" + // \n\n
                jsonBody;

        // Mocking 설정
        RequestLine mockRequestLine = new RequestLine(HttpMethod.PUT, "/items", "/items");
        when(mockRequestLineParser.parse(anyString())).thenReturn(mockRequestLine);
        when(mockQueryStringParser.parse(anyString())).thenReturn(Collections.emptyMap());

        // when
        HttpRequest<?> httpRequest = httpRequestParser.parse(rawHttpRequest);

        // then
        assertNotNull(httpRequest);
        assertThat(httpRequest.getMethod()).isEqualTo(HttpMethod.PUT);
        assertThat(httpRequest.getPath()).isEqualTo("/items");
        assertThat(httpRequest.getBody()).isEqualTo(jsonBody);
    }

    @Test
    @DisplayName("바디가 없는 요청을 올바르게 파싱해야 한다 (CRLF 없는 경우)")
    void parse_noBodyNoCrLf() {
        // given
        String rawHttpRequest = "DELETE /resource/123 HTTP/1.1\r\n" +
                "Host: example.com\r\n" +
                "Connection: close"; // 바디도 없고, CRLF 구분자도 없음

        // Mocking 설정
        RequestLine mockRequestLine = new RequestLine(HttpMethod.DELETE, "/resource/123", "/resource/123");
        when(mockRequestLineParser.parse(anyString())).thenReturn(mockRequestLine);
        when(mockQueryStringParser.parse(anyString())).thenReturn(Collections.emptyMap());

        // when
        HttpRequest<?> httpRequest = httpRequestParser.parse(rawHttpRequest);

        // then
        assertNotNull(httpRequest);
        assertThat(httpRequest.getMethod()).isEqualTo(HttpMethod.DELETE);
        assertThat(httpRequest.getPath()).isEqualTo("/resource/123");
        assertThat(httpRequest.getBody()).isEqualTo(""); // 바디는 빈 문자열로 처리되어야 함
    }

    @Test
    @DisplayName("복잡한 쿼리 스트링과 바디를 함께 파싱해야 한다")
    void parse_complexRequest() {
        // given
        String jsonBody = "{\"item\":\"laptop\", \"quantity\":1}";
        String rawHttpRequest = "PATCH /orders/123?userId=abc&status=pending HTTP/1.1\r\n" +
                "Content-Type: application/json\r\n" +
                "Authorization: Bearer token123\r\n" +
                "Content-Length: " + jsonBody.length() + "\r\n" +
                "\r\n" +
                jsonBody;

        // Mocking RequestLineParser
        RequestLine mockRequestLine = new RequestLine(HttpMethod.PATCH, "/orders/123?userId=abc&status=pending", "/orders/123");
        when(mockRequestLineParser.parse(anyString())).thenReturn(mockRequestLine);

        // Mocking QueryStringParser
        Map<String, String> mockQueryParams = new HashMap<>();
        mockQueryParams.put("userId", "abc");
        mockQueryParams.put("status", "pending");
        when(mockQueryStringParser.parse("/orders/123?userId=abc&status=pending")).thenReturn(mockQueryParams);

        // when
        HttpRequest<?> httpRequest = httpRequestParser.parse(rawHttpRequest);

        // then
        assertNotNull(httpRequest);
        assertThat(httpRequest.getMethod()).isEqualTo(HttpMethod.PATCH);
        assertThat(httpRequest.getPath()).isEqualTo("/orders/123");
        assertThat(httpRequest.getQueryParams()).isEqualTo(mockQueryParams);
        assertThat(httpRequest.getBody()).isEqualTo(jsonBody);

        verify(mockRequestLineParser).parse("PATCH /orders/123?userId=abc&status=pending HTTP/1.1");
        verify(mockQueryStringParser).parse("/orders/123?userId=abc&status=pending");
    }

    @Test
    @DisplayName("요청 라인만 있고 헤더나 바디가 없는 경우에도 올바르게 파싱해야 한다")
    void parse_requestLineOnly() {
        // given
        String rawHttpRequest = "GET /simple HTTP/1.1"; // 헤더도 바디도 없고, CRLF 구분자도 없음

        // Mocking RequestLineParser
        RequestLine mockRequestLine = new RequestLine(HttpMethod.GET, "/simple", "/simple");
        when(mockRequestLineParser.parse(anyString())).thenReturn(mockRequestLine);

        // Mocking QueryStringParser
        when(mockQueryStringParser.parse(anyString())).thenReturn(Collections.emptyMap());

        // when
        HttpRequest<?> httpRequest = httpRequestParser.parse(rawHttpRequest);

        // then
        assertNotNull(httpRequest);
        assertThat(httpRequest.getMethod()).isEqualTo(HttpMethod.GET);
        assertThat(httpRequest.getPath()).isEqualTo("/simple");
        assertTrue(httpRequest.getQueryParams().isEmpty());
        assertThat(httpRequest.getBody()).isEqualTo(""); // 바디는 빈 문자열로 처리됨
    }
}