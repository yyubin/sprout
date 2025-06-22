package sprout.mvc.argument;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import sprout.mvc.http.HttpRequest;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CompositeArgumentResolverTest {

    // System Under Test (테스트 대상)
    private CompositeArgumentResolver compositeResolver;

    // Mock 객체들
    @Mock
    private ArgumentResolver resolver1;
    @Mock
    private ArgumentResolver resolver2;
    @Mock
    private HttpRequest<?> mockRequest;

    // 테스트용 더미 컨트롤러
    private static class TestController {
        // 여러 파라미터를 가진 메서드
        public void handleRequest(String param1, Integer param2) {}

        // 지원되지 않는 파라미터를 가진 메서드
        public void handleUnsupported(Double param) {}

        // 파라미터가 없는 메서드
        public void handleNoParams() {}
    }

    @BeforeEach
    void setUp() {
        // Mockito 어노테이션 초기화
        MockitoAnnotations.openMocks(this);
        // 테스트 대상 객체에 Mock Resolver 리스트를 주입하여 생성
        compositeResolver = new CompositeArgumentResolver(List.of(resolver1, resolver2));
    }

    @Test
    @DisplayName("각 파라미터를 지원하는 ArgumentResolver에게 성공적으로 위임해야 한다")
    void resolveArguments_Success() throws Exception {
        // given: 테스트 환경 설정
        Method method = TestController.class.getMethod("handleRequest", String.class, Integer.class);
        Parameter param1 = method.getParameters()[0]; // String
        Parameter param2 = method.getParameters()[1]; // Integer
        Map<String, String> pathVariables = Collections.emptyMap();

        String resolvedValue1 = "resolvedString";
        Integer resolvedValue2 = 123;

        // resolver1은 String 파라미터를 지원하도록 설정
        when(resolver1.supports(param1)).thenReturn(true);
        when(resolver1.supports(param2)).thenReturn(false); // Integer는 지원하지 않음
        when(resolver1.resolve(param1, mockRequest, pathVariables)).thenReturn(resolvedValue1);

        // resolver2는 Integer 파라미터를 지원하도록 설정
        when(resolver2.supports(param1)).thenReturn(false); // String은 지원하지 않음
        when(resolver2.supports(param2)).thenReturn(true);
        when(resolver2.resolve(param2, mockRequest, pathVariables)).thenReturn(resolvedValue2);

        // when: 실제 메서드 호출
        Object[] resolvedArgs = compositeResolver.resolveArguments(method, mockRequest, pathVariables);

        // then: 결과 검증
        assertThat(resolvedArgs).isNotNull();
        assertThat(resolvedArgs.length).isEqualTo(2);
        assertThat(resolvedArgs).containsExactly(resolvedValue1, resolvedValue2);

        // resolver1과 resolver2의 resolve 메서드가 정확히 한 번씩 호출되었는지 검증
        verify(resolver1, times(1)).resolve(param1, mockRequest, pathVariables);
        verify(resolver2, times(1)).resolve(param2, mockRequest, pathVariables);
    }

    @Test
    @DisplayName("파라미터를 지원하는 Resolver가 없으면 IllegalStateException을 던져야 한다")
    void resolveArguments_NoSupportingResolver_ThrowsException() throws NoSuchMethodException {
        // given
        Method method = TestController.class.getMethod("handleUnsupported", Double.class);
        Parameter unsupportedParam = method.getParameters()[0];

        // resolver1과 resolver2 모두 Double 타입을 지원하지 않도록 설정
        when(resolver1.supports(unsupportedParam)).thenReturn(false);
        when(resolver2.supports(unsupportedParam)).thenReturn(false);

        // when & then: 예외 발생 검증
        assertThatThrownBy(() -> compositeResolver.resolveArguments(method, mockRequest, Collections.emptyMap()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No ArgumentResolver for parameter " + unsupportedParam);
    }

    @Test
    @DisplayName("파라미터를 지원하는 첫 번째 Resolver를 사용해야 한다")
    void resolveArguments_UsesFirstSupportingResolver() throws Exception {
        // given
        Method method = TestController.class.getMethod("handleRequest", String.class, Integer.class);
        Parameter stringParam = method.getParameters()[0];
        Parameter intParam = method.getParameters()[1]; // 이 테스트에서는 사용 안 함

        // resolver1과 resolver2 모두 String 타입을 지원한다고 설정
        when(resolver1.supports(stringParam)).thenReturn(true);
        when(resolver2.supports(stringParam)).thenReturn(true);

        // 각 resolver가 다른 값을 반환하도록 설정
        when(resolver1.resolve(any(), any(), any())).thenReturn("from_resolver1");
        when(resolver2.resolve(any(), any(), any())).thenReturn("from_resolver2");
        // intParam은 resolver2만 지원
        when(resolver2.supports(intParam)).thenReturn(true);

        // when
        Object[] resolvedArgs = compositeResolver.resolveArguments(method, mockRequest, Collections.emptyMap());

        // then
        // String 파라미터는 리스트의 첫 번째인 resolver1에 의해 처리되어야 한다.
        assertThat(resolvedArgs[0]).isEqualTo("from_resolver1");

        // resolver1의 resolve가 호출되었는지 확인
        verify(resolver1, times(1)).resolve(stringParam, mockRequest, Collections.emptyMap());
        // resolver2는 String 파라미터에 대해 resolve를 호출하면 안된다 (findFirst()에 의해).
        verify(resolver2, never()).resolve(stringParam, mockRequest, Collections.emptyMap());
        // resolver2는 Integer 파라미터에 대해서만 resolve를 호출해야 한다.
        verify(resolver2, times(1)).resolve(intParam, mockRequest, Collections.emptyMap());
    }

    @Test
    @DisplayName("파라미터가 없는 메서드는 비어있는 객체 배열을 반환해야 한다")
    void resolveArguments_NoParameters_ReturnsEmptyArray() throws Exception {
        // given
        Method method = TestController.class.getMethod("handleNoParams");

        // when
        Object[] resolvedArgs = compositeResolver.resolveArguments(method, mockRequest, Collections.emptyMap());

        // then
        assertThat(resolvedArgs).isNotNull().isEmpty();
    }
}