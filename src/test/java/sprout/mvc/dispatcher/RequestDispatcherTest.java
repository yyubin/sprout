package sprout.mvc.dispatcher;

import org.junit.jupiter.api.*;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import sprout.core.filter.FilterChain;
import sprout.core.interceptor.Interceptor;
import sprout.core.interceptor.InterceptorChain;
import sprout.mvc.advice.ResponseAdvice;
import sprout.mvc.exception.ExceptionResolver;
import sprout.mvc.http.*;
import sprout.mvc.invoke.HandlerMethod;
import sprout.mvc.invoke.HandlerMethodInvoker;
import sprout.mvc.mapping.HandlerMapping;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RequestDispatcherTest {

    @Mock HandlerMapping mapping;
    @Mock HandlerMethodInvoker invoker;
    @Mock ResponseResolver responseResolver;
    @Mock ExceptionResolver exceptionResolver;
    @Mock Interceptor interceptor;
    @Mock DispatchHook hook;
    @Mock HttpRequest<?> req;
    @Mock HttpResponse    res;

    HandlerMethod hm = mock(HandlerMethod.class);

    RequestDispatcher dispatcher;

    AutoCloseable mocks;

    @BeforeEach
    void init() {
        mocks = MockitoAnnotations.openMocks(this);

        // 기본값: 응답이 아직 커밋되지 않음
        when(res.isCommitted()).thenReturn(false);
        // ResponseResolver 기본 supports false → 상황별로 stub
        when(responseResolver.supports(any())).thenReturn(false);
        when(req.getPath()).thenReturn("/test");      // ★ 필수
        when(req.getMethod()).thenReturn(HttpMethod.GET);   // ★ 필수
        when(res.isCommitted()).thenReturn(false);

        dispatcher = new RequestDispatcher(
                mapping, invoker,
                List.of(responseResolver),
                List.of(),               // ResponseAdvice 없음
                List.of(),               // Filter 없음 (필요 시 개별 테스트서 mock)
                List.of(interceptor),    // 인터셉터 1개
                List.of(exceptionResolver),
                List.of(hook)            // Hook 1개
        );
    }

    @AfterEach
    void close() throws Exception { mocks.close(); }

    /* ---------- 1. 핸들러 없음 → 404 ---------- */

    @Test
    @DisplayName("요청 경로에 매핑이 없으면 404 응답")
    void noHandler_returns404() throws IOException {
        when(mapping.findHandler(anyString(), any())).thenReturn(null);

        dispatcher.dispatch(req, res);

        verify(res).setResponseEntity(argThat(e ->
                e.getStatusCode() == ResponseCode.NOT_FOUND &&
                        "Not Found".equals(e.getBody())
        ));
        // invoker·interceptor는 호출되지 않음
        verifyNoInteractions(invoker, interceptor);
    }

    /* ---------- 2. 정상 처리 ---------- */

    @Test
    @DisplayName("필터∙인터셉터 통과 후 핸들러 호출, 응답 설정")
    void normalFlow() throws Exception {
        when(mapping.findHandler(anyString(), any())).thenReturn(hm);
        when(interceptor.preHandle(req,res,hm)).thenReturn(true);
        when(invoker.invoke(any(), eq(req))).thenReturn("Hello");
        when(responseResolver.supports("Hello")).thenReturn(true);
        ResponseEntity<String> ok = new ResponseEntity<>("OK", new HashMap<>(), ResponseCode.SUCCESS);
        doReturn(ok).when(responseResolver).resolve(any(), any());

        dispatcher.dispatch(req, res);

        InOrder order = inOrder(interceptor, invoker, responseResolver, res);
        order.verify(interceptor).preHandle(req,res,hm);
        order.verify(invoker).invoke(any(), eq(req));
        order.verify(interceptor).postHandle(req,res,hm,"Hello");
        order.verify(responseResolver).resolve("Hello", req);
        order.verify(res).setResponseEntity(ok);
        order.verify(interceptor).afterCompletion(req,res,hm,null);
    }

    /* ---------- 3. 인터셉터가 preHandle false ---------- */

    @Test
    @DisplayName("preHandle 이 false 면 이후 단계가 중단된다")
    void interceptorStopsFlow() throws Exception {
        when(mapping.findHandler(anyString(), any())).thenReturn(hm);
        when(interceptor.preHandle(req,res,hm)).thenReturn(false);

        dispatcher.dispatch(req, res);

        verify(interceptor).preHandle(req,res,hm);
        verify(interceptor).afterCompletion(req,res,hm,null);
        verifyNoInteractions(invoker, responseResolver);
        verify(res, never()).setResponseEntity(any());
    }

    /* ---------- 4. 핸들러에서 예외 발생 + ExceptionResolver ---------- */

    @Test
    @DisplayName("핸들러 예외를 ExceptionResolver 가 처리")
    void exceptionHandledByResolver() throws Exception {
        when(mapping.findHandler(anyString(), any())).thenReturn(hm);
        when(interceptor.preHandle(req,res,hm)).thenReturn(true);
        when(invoker.invoke(any(), eq(req))).thenThrow(new IllegalStateException("boom"));

        when(exceptionResolver.resolveException(eq(req), eq(res), eq(hm), any()))
                .thenAnswer(inv -> {         // Resolver 가 String 반환
                    return "ERR";
                });

        when(responseResolver.supports("ERR")).thenReturn(true);
        ResponseEntity<String> errEnt = new ResponseEntity<>("E", new HashMap<>(), ResponseCode.INTERNAL_SERVER_ERROR);
        doReturn(errEnt).when(responseResolver).resolve(any(), any());

        dispatcher.dispatch(req, res);

        verify(exceptionResolver).resolveException(eq(req), eq(res), eq(hm), any());
        verify(responseResolver).resolve("ERR", req);
        verify(res).setResponseEntity(errEnt);

        // afterCompletion 에는 예외 객체가 전달
        verify(interceptor).afterCompletion(eq(req), eq(res), eq(hm), any(IllegalStateException.class));
    }
}
