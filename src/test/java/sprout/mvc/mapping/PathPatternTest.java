package sprout.mvc.mapping;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class PathPatternTest {

    @Test
    @DisplayName("고정된 경로 패턴이 올바르게 매칭되어야 한다.")
    void matches_fixedPath() {
        PathPattern pattern = new PathPattern("/users");
        assertTrue(pattern.matches("/users"));
        assertFalse(pattern.matches("/users/")); // 슬래시 불일치
        assertFalse(pattern.matches("/users/1"));
        assertFalse(pattern.matches("/admin"));
    }

    @Test
    @DisplayName("단일 경로 변수가 있는 패턴이 올바르게 매칭되어야 한다.")
    void matches_singlePathVariable() {
        PathPattern pattern = new PathPattern("/users/{id}");
        assertTrue(pattern.matches("/users/123"));
        assertTrue(pattern.matches("/users/abc"));
        assertFalse(pattern.matches("/users")); // 변수 값 없음
        assertFalse(pattern.matches("/users/123/extra")); // 추가 경로 요소
    }

    @Test
    @DisplayName("여러 경로 변수가 있는 패턴이 올바르게 매칭되어야 한다.")
    void matches_multiplePathVariables() {
        PathPattern pattern = new PathPattern("/categories/{category}/products/{productId}");
        assertTrue(pattern.matches("/categories/electronics/products/P123"));
        assertTrue(pattern.matches("/categories/books/products/B-456"));
        assertFalse(pattern.matches("/categories/electronics/products")); // 변수 값 부족
        assertFalse(pattern.matches("/categories/electronics/products/P123/extra"));
    }

    @Test
    @DisplayName("경로 변수가 없는 경우 빈 맵을 반환해야 한다.")
    void extractPathVariables_noVariables() {
        PathPattern pattern = new PathPattern("/health");
        Map<String, String> variables = pattern.extractPathVariables("/health");
        assertTrue(variables.isEmpty());
        assertEquals(0, pattern.getVariableCount());
    }

    @Test
    @DisplayName("단일 경로 변수를 올바르게 추출해야 한다.")
    void extractPathVariables_singleVariable() {
        PathPattern pattern = new PathPattern("/items/{itemId}");
        Map<String, String> variables = pattern.extractPathVariables("/items/item123");

        assertEquals(1, variables.size());
        assertEquals("item123", variables.get("itemId"));
        assertEquals(1, pattern.getVariableCount());
    }

    @Test
    @DisplayName("여러 경로 변수를 올바르게 추출해야 한다.")
    void extractPathVariables_multipleVariables() {
        PathPattern pattern = new PathPattern("/users/{userId}/orders/{orderId}");
        Map<String, String> variables = pattern.extractPathVariables("/users/userABC/orders/orderXYZ");

        assertEquals(2, variables.size());
        assertEquals("userABC", variables.get("userId"));
        assertEquals("orderXYZ", variables.get("orderId"));
        assertEquals(2, pattern.getVariableCount());
    }

    @Test
    @DisplayName("경로가 패턴과 매칭되지 않으면 빈 맵을 반환해야 한다.")
    void extractPathVariables_noMatchReturnsEmptyMap() {
        PathPattern pattern = new PathPattern("/users/{id}");
        Map<String, String> variables = pattern.extractPathVariables("/admins/123"); // 다른 경로
        assertTrue(variables.isEmpty());

        variables = pattern.extractPathVariables("/users"); // 변수 누락
        assertTrue(variables.isEmpty());
    }

    @Test
    @DisplayName("패턴과 정확히 일치하는지 확인한다 (trailing slash 없음).")
    void matches_exactMatch() {
        PathPattern pattern = new PathPattern("/api/v1/data");
        assertTrue(pattern.matches("/api/v1/data"));
        assertFalse(pattern.matches("/api/v1/data/")); // 불일치
    }

    @Test
    @DisplayName("패턴에 슬래시로만 구성된 변수가 포함된 경우")
    void matches_slashVariablePattern() {
        // 이 경우 PathPattern의 compilePattern 로직에 의해
        // {var}는 [^/]+로 변환되므로, 슬래시가 포함된 값은 매칭되지 않습니다.
        // 현재 로직은 {var}에 슬래시가 들어갈 수 없도록 설계됨.
        PathPattern pattern = new PathPattern("/{path}");
        assertTrue(pattern.matches("/a"));
        assertFalse(pattern.matches("/a/b")); // 이 경우 매칭되지 않음
    }

    @Test
    @DisplayName("패턴에 변수가 여러 개 있지만, 실제 경로에 없는 경우 빈 맵을 반환해야 한다.")
    void extractPathVariables_variablesInPatternButNotInPath() {
        PathPattern pattern = new PathPattern("/a/{b}/c/{d}");
        Map<String, String> variables = pattern.extractPathVariables("/a/valB/c"); // d가 없음

        assertTrue(variables.isEmpty()); // matches()가 false이므로 extractPathVariables도 빈 맵 반환
    }

    @Test
    @DisplayName("패턴이 루트 경로일 때 올바르게 매칭되어야 한다.")
    void matches_rootPath() {
        PathPattern pattern = new PathPattern("/");
        assertTrue(pattern.matches("/"));
        assertFalse(pattern.matches("/a"));
    }

    @Test
    @DisplayName("패턴과 객체 동등성(equals) 및 해시코드(hashCode)를 검증한다.")
    void equalsAndHashCode() {
        PathPattern pattern1 = new PathPattern("/test/{id}");
        PathPattern pattern2 = new PathPattern("/test/{id}");
        PathPattern pattern3 = new PathPattern("/another/{id}");

        // 동등성 검사
        assertTrue(pattern1.equals(pattern2));
        assertFalse(pattern1.equals(pattern3));
        assertFalse(pattern1.equals(null));
        assertFalse(pattern1.equals("string"));

        // 해시코드 검사
        assertEquals(pattern1.hashCode(), pattern2.hashCode());
        assertNotEquals(pattern1.hashCode(), pattern3.hashCode());
    }

    @Test
    @DisplayName("getOriginalPattern이 올바른 원본 패턴을 반환해야 한다.")
    void getOriginalPattern_returnsCorrectPattern() {
        String original = "/api/v2/{name}/data";
        PathPattern pattern = new PathPattern(original);
        assertEquals(original, pattern.getOriginalPattern());
    }

    @Test
    @DisplayName("toString()이 원본 패턴을 반환해야 한다.")
    void toString_returnsOriginalPattern() {
        String original = "/reports/{year}/{month}";
        PathPattern pattern = new PathPattern(original);
        assertEquals(original, pattern.toString());
    }
}