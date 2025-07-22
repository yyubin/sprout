package sprout.mvc.advice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import sprout.mvc.advice.annotation.ExceptionHandler;
import sprout.mvc.http.*;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ControllerAdviceExceptionResolverTest {

    // Mocks
    ControllerAdviceRegistry registry = mock(ControllerAdviceRegistry.class);
    ResponseResolver resolver         = mock(ResponseResolver.class);
    HttpRequest<?>   httpRequest      = mock(HttpRequest.class);
    HttpResponse     httpResponse     = mock(HttpResponse.class);

    // System under test
    ControllerAdviceExceptionResolver sut;

    @BeforeEach
    void init() {
        sut = new ControllerAdviceExceptionResolver(registry,
                List.of(resolver),
                null); // ObjectMapper 사용 안 함
    }

    /* ---------- 테스트용 Advice 빈 ---------- */

    static class CustomException extends RuntimeException { }

    static class AdviceBean {
        @ExceptionHandler({CustomException.class})
        public String handleCustom(CustomException ex) {
            return "handled:" + ex.getClass().getSimpleName();
        }
    }

    /* ---------- 시나리오 1 : 핸들러 존재 ---------- */

    @Test @DisplayName("@ExceptionHandler 가 존재하면 호출 후 ResponseResolver 로 처리")
    void resolveException_withHandler() throws Exception {
        // given
        AdviceBean bean = spy(new AdviceBean());
        Method m = AdviceBean.class.getMethod("handleCustom", CustomException.class);

        // ExceptionHandlerObject mock
        ExceptionHandlerObject ehObj = mock(ExceptionHandlerObject.class);
        when(ehObj.getBean()).thenReturn(bean);
        when(ehObj.getMethod()).thenReturn(m);

        when(registry.getExceptionHandler(CustomException.class))
                .thenReturn(Optional.of(ehObj));

        // ResponseResolver supports & resolve
        when(resolver.supports(any())).thenReturn(true);

        ResponseEntity<?> responseEntity = new ResponseEntity<>("OK", new HashMap<>(), ResponseCode.SUCCESS);
        doReturn(responseEntity).when(resolver).resolve(any(), any());

        // when
        Object result = sut.resolveException(httpRequest, httpResponse,
                null, new CustomException());

        // then : 1) 비‑null 반환
        assertThat(result).isNotNull();

        // 2) Advice 메서드 호출 확인
        verify(bean).handleCustom(any(CustomException.class));

        // 3) ResponseResolver 사용 & HttpResponse 세팅
        verify(resolver).resolve(any(), eq(httpRequest));
        verify(httpResponse).setResponseEntity(responseEntity);
    }

    /* ---------- 시나리오 2 : 핸들러 없음 ---------- */

    @Test @DisplayName("해당 예외를 처리할 핸들러가 없으면 null 반환")
    void resolveException_noHandler() {
        when(registry.getExceptionHandler(IllegalStateException.class))
                .thenReturn(Optional.empty());

        Object result = sut.resolveException(httpRequest, httpResponse,
                null, new IllegalStateException());

        assertThat(result).isNull();
        // HttpResponse 는 변경되지 않아야 함
        verifyNoInteractions(resolver);
        verify(httpResponse, never()).setResponseEntity(any());
    }
}
