package sprout.mvc.mapping;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PathPatternResolverTest {

    private PathPatternResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new PathPatternResolver();
    }

    @Test
    @DisplayName("고정된 경로 문자열을 올바른 PathPattern 객체로 변환해야 한다.")
    void resolve_fixedPathString() {
        // given
        String pathString = "/users";

        // when
        PathPattern pathPattern = resolver.resolve(pathString);

        // then
        assertNotNull(pathPattern);
        assertThat(pathPattern.getOriginalPattern()).isEqualTo(pathString);
        assertThat(pathPattern.getVariableCount()).isZero(); // 변수가 없어야 함
        // 추가적으로, 매칭 테스트를 통해 PathPattern이 제대로 동작하는지 간접 검증
        assertThat(pathPattern.matches("/users")).isTrue();
        assertThat(pathPattern.matches("/users/1")).isFalse();
    }

    @Test
    @DisplayName("경로 변수가 포함된 경로 문자열을 올바른 PathPattern 객체로 변환해야 한다.")
    void resolve_pathVariableString() {
        // given
        String pathString = "/products/{id}";

        // when
        PathPattern pathPattern = resolver.resolve(pathString);

        // then
        assertNotNull(pathPattern);
        assertThat(pathPattern.getOriginalPattern()).isEqualTo(pathString);
        assertThat(pathPattern.getVariableCount()).isEqualTo(1); // 변수가 1개여야 함
        // 추가적으로, 매칭 및 변수 추출 테스트를 통해 PathPattern이 제대로 동작하는지 간접 검증
        assertThat(pathPattern.matches("/products/123")).isTrue();
        assertThat(pathPattern.extractPathVariables("/products/456")).containsEntry("id", "456");
    }

    @Test
    @DisplayName("여러 경로 변수가 포함된 경로 문자열을 올바른 PathPattern 객체로 변환해야 한다.")
    void resolve_multiplePathVariableString() {
        // given
        String pathString = "/categories/{category}/items/{itemId}";

        // when
        PathPattern pathPattern = resolver.resolve(pathString);

        // then
        assertNotNull(pathPattern);
        assertThat(pathPattern.getOriginalPattern()).isEqualTo(pathString);
        assertThat(pathPattern.getVariableCount()).isEqualTo(2); // 변수가 2개여야 함
        // 추가적으로, 매칭 및 변수 추출 테스트를 통해 PathPattern이 제대로 동작하는지 간접 검증
        assertThat(pathPattern.matches("/categories/books/items/novel1")).isTrue();
        assertThat(pathPattern.extractPathVariables("/categories/electronics/items/TV123"))
                .containsEntry("category", "electronics")
                .containsEntry("itemId", "TV123");
    }

    @Test
    @DisplayName("루트 경로 문자열을 올바른 PathPattern 객체로 변환해야 한다.")
    void resolve_rootPathString() {
        // given
        String pathString = "/";

        // when
        PathPattern pathPattern = resolver.resolve(pathString);

        // then
        assertNotNull(pathPattern);
        assertThat(pathPattern.getOriginalPattern()).isEqualTo(pathString);
        assertThat(pathPattern.getVariableCount()).isZero(); // 변수가 없어야 함
        assertThat(pathPattern.matches("/")).isTrue();
        assertThat(pathPattern.matches("/home")).isFalse();
    }

    @Test
    @DisplayName("빈 문자열을 PathPattern 객체로 변환할 때 예상대로 동작해야 한다.")
    void resolve_emptyPathString() {
        // given
        String pathString = ""; // HTTP 경로에서는 흔치 않지만, 테스트 커버리지 목적

        // when
        PathPattern pathPattern = resolver.resolve(pathString);

        // then
        assertNotNull(pathPattern);
        assertThat(pathPattern.getOriginalPattern()).isEqualTo(pathString);
        assertThat(pathPattern.getVariableCount()).isZero();
        // 빈 문자열 패턴은 빈 문자열과만 매칭되어야 함 (실제 URL 경로에서는 거의 없음)
        assertThat(pathPattern.matches("")).isTrue();
        assertThat(pathPattern.matches("/")).isFalse();
    }
}