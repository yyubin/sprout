package sprout.core.interceptor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import sprout.mvc.http.HttpRequest;
import sprout.mvc.http.HttpResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class InterceptorChainTest {

    Interceptor i1 = mock(Interceptor.class);
    Interceptor i2 = mock(Interceptor.class);
    Interceptor i3 = mock(Interceptor.class);

    HttpRequest  req  = mock(HttpRequest.class);
    HttpResponse res  = mock(HttpResponse.class);
    Object       handler = new Object();
    Object       result  = "OK";
    Exception    ex      = new RuntimeException("boom");

    InterceptorChain chain;

    @BeforeEach
    void setUp() {
        chain = new InterceptorChain(List.of(i1, i2, i3));
    }

    /* ---------- preHandle() ---------- */

    @Nested @DisplayName("applyPreHandle()")
    class PreHandle {

        @Test @DisplayName("모든 인터셉터가 true 를 반환하면 true")
        void allTrue_returnsTrue() {
            when(i1.preHandle(req,res,handler)).thenReturn(true);
            when(i2.preHandle(req,res,handler)).thenReturn(true);
            when(i3.preHandle(req,res,handler)).thenReturn(true);

            boolean ok = chain.applyPreHandle(req, res, handler);

            assertThat(ok).isTrue();
            InOrder order = inOrder(i1, i2, i3);
            order.verify(i1).preHandle(req,res,handler);
            order.verify(i2).preHandle(req,res,handler);
            order.verify(i3).preHandle(req,res,handler);
        }

        @Test @DisplayName("중간에서 false 면 이후 호출 없이 false 반환")
        void shortCircuit() {
            when(i1.preHandle(req,res,handler)).thenReturn(true);
            when(i2.preHandle(req,res,handler)).thenReturn(false);

            boolean ok = chain.applyPreHandle(req, res, handler);

            assertThat(ok).isFalse();
            verify(i1).preHandle(req,res,handler);
            verify(i2).preHandle(req,res,handler);
            verifyNoInteractions(i3);              // 이후 인터셉터는 호출되지 않음
        }
    }

    /* ---------- postHandle() ---------- */

    @Test @DisplayName("applyPostHandle() 은 역순으로 호출")
    void postHandle_reverseOrder() {
        chain.applyPostHandle(req, res, handler, result);

        InOrder order = inOrder(i3, i2, i1);
        order.verify(i3).postHandle(req,res,handler,result);
        order.verify(i2).postHandle(req,res,handler,result);
        order.verify(i1).postHandle(req,res,handler,result);
    }

    /* ---------- afterCompletion() ---------- */

    @Test @DisplayName("applyAfterCompletion() 도 역순 호출")
    void afterCompletion_reverseOrder() {
        chain.applyAfterCompletion(req, res, handler, ex);

        InOrder order = inOrder(i3, i2, i1);
        order.verify(i3).afterCompletion(req,res,handler,ex);
        order.verify(i2).afterCompletion(req,res,handler,ex);
        order.verify(i1).afterCompletion(req,res,handler,ex);
    }
}
