package sprout.mvc.argument.builtins;

import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import sprout.mvc.annotation.Header;
import sprout.mvc.argument.TypeConverter;
import sprout.mvc.http.HttpRequest;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class HeaderArgumentResolverTest {

    HeaderArgumentResolver resolver = new HeaderArgumentResolver();

    HttpRequest<?> request = mock(HttpRequest.class);

    // 테스트용 메서드들 ─ 파라미터 메타데이터 추출용
    static class Dummy {
        void handle(@Header("X-REQ") String reqId,
                    @Header                 String implicitName,
                    String noHeader) {}
    }

    Parameter pExplicit;     // @Header("X-REQ")
    Parameter pImplicit;     // @Header("")   (빈 값)
    Parameter pNoHeader;     // 어노테이션 없음

    @BeforeEach
    void prepare() throws NoSuchMethodException {
        Method m = Dummy.class.getDeclaredMethod("handle", String.class, String.class, String.class);
        Parameter[] ps = m.getParameters();
        pExplicit  = ps[0];
        pImplicit  = ps[1];
        pNoHeader  = ps[2];
    }

    /* ------------ supports() ------------- */

    @Test @DisplayName("명시적 헤더 이름이 있으면 supports = true")
    void supports_explicitHeader() {
        assertThat(resolver.supports(pExplicit)).isTrue();
    }

    @Test @DisplayName("빈 값 @Header 또는 어노테이션 없음은 supports = false")
    void supports_falseCases() {
        assertThat(resolver.supports(pImplicit)).isFalse();
        assertThat(resolver.supports(pNoHeader)).isFalse();
    }

    /* ------------ resolve() ------------- */

    @Test @DisplayName("명시적 헤더 이름으로 값을 찾아 TypeConverter 로 변환")
    void resolve_explicitHeader() throws Exception {
        // given: 요청 헤더 & TypeConverter 스텁
        when(request.getHeaders()).thenReturn(Map.of("X-REQ", "123"));

        try (MockedStatic<TypeConverter> tc = mockStatic(TypeConverter.class)) {
            tc.when(() -> TypeConverter.convert("123", String.class)).thenReturn("123");

            // when
            Object result = resolver.resolve(pExplicit, request, Map.of());

            // then
            assertThat(result).isEqualTo("123");
            tc.verify(() -> TypeConverter.convert("123", String.class));
        }
    }

    @Test @DisplayName("헤더가 존재하지 않으면 null 을 반환")
    void resolve_missingHeader_returnsNull() throws Exception {
        when(request.getHeaders()).thenReturn(Map.of());

        try (MockedStatic<TypeConverter> tc = mockStatic(TypeConverter.class)) {
            tc.when(() -> TypeConverter.convert(null, String.class)).thenReturn(null);

            Object result = resolver.resolve(pExplicit, request, Map.of());
            assertThat(result).isNull();
        }
    }
}
