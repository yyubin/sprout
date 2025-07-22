package sprout.security.web.util.matcher;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import sprout.mvc.http.HttpMethod;
import sprout.mvc.http.HttpRequest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class AntPathRequestMatcherTest {

    private HttpRequest req(String path, HttpMethod method) {
        HttpRequest r = Mockito.mock(HttpRequest.class);
        when(r.getPath()).thenReturn(path);
        when(r.getMethod()).thenReturn(method);
        return r;
    }

    @Test
    @DisplayName("null 패턴이면 NPE")
    void constructor_nullPattern_throws() {
        assertThrows(NullPointerException.class, () -> new AntPathRequestMatcher(null));
    }

    @Test
    @DisplayName("메서드 미지정(null) 시 경로만 맞으면 매치")
    void matches_pathOnly() {
        AntPathRequestMatcher m = new AntPathRequestMatcher("/users/**");
        assertTrue(m.matches(req("/users/1", HttpMethod.GET)));
    }

    @Test
    @DisplayName("HTTP 메서드가 다르면 매치 실패")
    void matches_methodMismatch() {
        AntPathRequestMatcher m = new AntPathRequestMatcher("/users/**", HttpMethod.POST);
        assertFalse(m.matches(req("/users/1", HttpMethod.GET)));
    }

    @Test
    @DisplayName("caseSensitive=false 일 때 대소문자 무시")
    void matches_caseInsensitive() {
        AntPathRequestMatcher m = new AntPathRequestMatcher("/admin/**", HttpMethod.GET, false);
        assertTrue(m.matches(req("/AdMiN/user", HttpMethod.GET)));
    }

    @Test
    @DisplayName("Path 변수 추출 확인")
    void matcher_extractVariables() {
        AntPathRequestMatcher m = new AntPathRequestMatcher("/users/{id}/orders/{orderId}");
        HttpRequest r = req("/users/42/orders/999", HttpMethod.GET);

        RequestMatcher.MatchResult result = m.matcher(r);
        assertTrue(result.isMatch());
        Map<String, String> vars = result.getVariables();
        assertEquals("42", vars.get("id"));
        assertEquals("999", vars.get("orderId"));
    }

    @Test
    @DisplayName("매치 실패 시 MatchResult.notMatch() 반환")
    void matcher_notMatch() {
        AntPathRequestMatcher m = new AntPathRequestMatcher("/users/{id}");
        RequestMatcher.MatchResult result = m.matcher(req("/projects/1", HttpMethod.GET));
        assertFalse(result.isMatch());
        assertTrue(result.getVariables().isEmpty());
    }

    @Nested
    @DisplayName("equals / hashCode")
    class Equality {

        @Test
        @DisplayName("동일 필드면 equals true & hashCode 동일")
        void equalsAndHashCode_same() {
            AntPathRequestMatcher a = new AntPathRequestMatcher("/a/**", HttpMethod.PUT, false);
            AntPathRequestMatcher b = new AntPathRequestMatcher("/a/**", HttpMethod.PUT, false);

            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        @DisplayName("패턴/메서드/대소문자 중 하나라도 다르면 equals false")
        void equals_differs() {
            AntPathRequestMatcher base = new AntPathRequestMatcher("/a/**", HttpMethod.PUT, false);

            assertNotEquals(base, new AntPathRequestMatcher("/b/**", HttpMethod.PUT, false));
            assertNotEquals(base, new AntPathRequestMatcher("/a/**", HttpMethod.GET, false));
            assertNotEquals(base, new AntPathRequestMatcher("/a/**", HttpMethod.PUT, true));
        }
    }

    @Test
    @DisplayName("toString 형식 확인 (간단 체크)")
    void toString_containsFields() {
        AntPathRequestMatcher m = new AntPathRequestMatcher("/x/**", HttpMethod.DELETE, true);
        String s = m.toString();
        assertTrue(s.contains("pattern='/x/**'"));
        assertTrue(s.contains("httpMethod=DELETE"));
        assertTrue(s.contains("caseSensitive=true"));
    }
}
