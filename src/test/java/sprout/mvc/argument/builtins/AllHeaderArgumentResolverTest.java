package sprout.mvc.argument.builtins;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import sprout.mvc.annotation.Header;
import sprout.mvc.http.HttpRequest;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AllHeaderArgumentResolverTest {

    AllHeaderArgumentResolver resolver;

    @Mock HttpRequest<?> request;

    // 테스트용 메서드 (파라미터 메타데이터 확보용)
    static class Dummy {
        void all(@Header Map<String, String> all) {}
        void specific(@Header("X-ID") Map<String, String> map) {}
        void noHeader(Map<String, String> map) {}
    }

    Parameter pAll;        // @Header ("") Map
    Parameter pSpecific;   // @Header("X-ID") Map
    Parameter pNoAnnot;    // Map (no annotation)

    @BeforeEach
    void init() throws NoSuchMethodException {
        MockitoAnnotations.openMocks(this);
        resolver = new AllHeaderArgumentResolver();

        Method m = Dummy.class.getDeclaredMethod("all", Map.class);
        pAll      = m.getParameters()[0];
        pSpecific = Dummy.class.getDeclaredMethod("specific", Map.class).getParameters()[0];
        pNoAnnot  = Dummy.class.getDeclaredMethod("noHeader", Map.class).getParameters()[0];
    }

    @Nested
    @DisplayName("supports() 동작")
    class Supports {
        @Test @DisplayName("@Header 빈값 Map 은 지원")
        void supports_all() {
            assertThat(resolver.supports(pAll)).isTrue();
        }
        @Test @DisplayName("특정 헤더·미주석 파라미터는 지원 안함")
        void supports_false() {
            assertThat(resolver.supports(pSpecific)).isFalse();
            assertThat(resolver.supports(pNoAnnot)).isFalse();
        }
    }

    @Nested
    @DisplayName("resolve() 동작")
    class Resolve {
        @Test @DisplayName("전체 헤더 Map 을 그대로 반환")
        void resolve_allHeaders() throws Exception {
            Map<String,String> headers = Map.of("A","1","B","2");
            when(request.getHeaders()).thenReturn(headers);

            Object result = resolver.resolve(pAll, request, Map.of());

            assertThat(result).isSameAs(headers);
        }

        @Test @DisplayName("특정 헤더를 Map 으로 요청하면 예외")
        void resolve_specificHeaderMap_throws() {
            assertThatThrownBy(() ->
                    resolver.resolve(pSpecific, request, Map.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cannot bind specific header");
        }

        @Test @DisplayName("지원되지 않는 파라미터 타입은 null 반환")
        void resolve_unsupportedType_returnsNull() throws Exception {
            Object r = resolver.resolve(pNoAnnot, request, Map.of());
            assertThat(r).isNull();
        }
    }
}
