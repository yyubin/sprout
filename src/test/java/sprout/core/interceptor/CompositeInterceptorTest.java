package sprout.core.interceptor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import sprout.mvc.http.HttpRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CompositeInterceptorTest {

    HandlerInterceptor i1 = mock(HandlerInterceptor.class);
    HandlerInterceptor i2 = mock(HandlerInterceptor.class);
    HandlerInterceptor i3 = mock(HandlerInterceptor.class);

    HttpRequest<?> request = mock(HttpRequest.class);
    Object result = new Object();

    CompositeInterceptor composite;

    @BeforeEach
    void setUp() {
        composite = new CompositeInterceptor(List.of(i1, i2, i3));
    }

    /* ---------- preHandle() ---------- */

    @Nested @DisplayName("preHandle 동작")
    class PreHandle {

        @Test @DisplayName("모든 인터셉터가 true 를 반환하면 true")
        void allTrue_returnsTrue() {
            when(i1.preHandle(request)).thenReturn(true);
            when(i2.preHandle(request)).thenReturn(true);
            when(i3.preHandle(request)).thenReturn(true);

            boolean ok = composite.preHandle(request);

            assertThat(ok).isTrue();
            InOrder order = inOrder(i1, i2, i3);
            order.verify(i1).preHandle(request);
            order.verify(i2).preHandle(request);
            order.verify(i3).preHandle(request);
        }

        @Test @DisplayName("중간에서 false 를 반환하면 이후 인터셉터는 호출되지 않고 false 반환")
        void shortCircuitOnFalse() {
            when(i1.preHandle(request)).thenReturn(true);
            when(i2.preHandle(request)).thenReturn(false); // 중단점
            // i3.preHandle() 는 호출되지 않아야 함

            boolean ok = composite.preHandle(request);

            assertThat(ok).isFalse();
            verify(i1).preHandle(request);
            verify(i2).preHandle(request);
            verifyNoInteractions(i3);
        }
    }

    /* ---------- postHandle() ---------- */

    @Test
    @DisplayName("postHandle 은 역순으로 실행된다")
    void postHandle_calledInReverseOrder() {
        composite.postHandle(request, result);

        InOrder order = inOrder(i3, i2, i1); // 역순 검증
        order.verify(i3).postHandle(request, result);
        order.verify(i2).postHandle(request, result);
        order.verify(i1).postHandle(request, result);
    }
}
