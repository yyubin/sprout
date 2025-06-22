package sprout.mvc.http.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import sprout.mvc.http.HttpMethod;
import app.exception.BadRequestException;
import app.message.ExceptionMessage;
import sprout.mvc.http.ResponseCode;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestLineParserTest {

    private RequestLineParser parser;

    @BeforeEach
    void setUp() {
        parser = new RequestLineParser();
    }

    @Test
    @DisplayName("쿼리 파라미터가 없는 단순 경로를 올바르게 파싱해야 한다")
    void parse_SimplePath_Success() {
        // given
        String requestLineString = "GET /users/1 HTTP/1.1";

        // when
        RequestLine result = parser.parse(requestLineString);

        // then
        assertThat(result).isNotNull();
        assertThat(result.method()).isEqualTo(HttpMethod.GET);
        assertThat(result.rawPath()).isEqualTo("/users/1");
        assertThat(result.cleanPath()).isEqualTo("/users/1");
    }

    @Test
    @DisplayName("쿼리 파라미터가 있는 경로를 올바르게 파싱해야 한다")
    void parse_PathWithQueryString_Success() {
        // given
        String requestLineString = "POST /search?q=test&page=2 HTTP/1.1";

        // when
        RequestLine result = parser.parse(requestLineString);

        // then
        assertThat(result.method()).isEqualTo(HttpMethod.POST);
        assertThat(result.rawPath()).isEqualTo("/search?q=test&page=2");
        assertThat(result.cleanPath()).isEqualTo("/search");
    }

    @Test
    @DisplayName("HTTP 메서드가 소문자여도 대문자로 변환하여 파싱해야 한다")
    void parse_LowercaseMethod_Success() {
        // given
        String requestLineString = "delete /items/5";

        // when
        RequestLine result = parser.parse(requestLineString);

        // then
        assertThat(result.method()).isEqualTo(HttpMethod.DELETE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "GET", ""})
    @DisplayName("구성요소가 2개 미만인 요청 라인은 BadRequestException을 던져야 한다")
    void parse_MalformedLine_ThrowsBadRequestException(String invalidLine) {
        // when & then
        assertThatThrownBy(() -> parser.parse(invalidLine))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("지원하지 않는 HTTP 메서드는 IllegalArgumentException을 던져야 한다")
    void parse_InvalidMethod_ThrowsIllegalArgumentException() {
        // given
        String requestLineString = "JUMP /to/path HTTP/1.1";

        // when & then
        assertThatThrownBy(() -> parser.parse(requestLineString))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("null 입력값은 NullPointerException을 던져야 한다")
    void parse_NullInput_ThrowsNullPointerException() {
        // when & then
        assertThatThrownBy(() -> parser.parse(null))
                .isInstanceOf(NullPointerException.class);
    }
}